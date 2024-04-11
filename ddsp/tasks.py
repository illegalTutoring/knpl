# Common
import io
import soundfile as sf
import librosa
import base64
import torch
import numpy as np
import logging

# Preprocessing
from sep_wav import demucs
from tqdm import tqdm
from glob import glob
from pydub import AudioSegment
from logger.utils import traverse_dir
from logger import utils
import tempfile
import os

# Mixing
import ffmpeg

# Inference
from ddsp.vocoder import load_model, F0_Extractor, Volume_Extractor, Units_Encoder
from ddsp.core import upsample
from enhancer import Enhancer

# REST
from celery_worker import celery
import requests
from requests.exceptions import RequestException
from requests_toolbelt.multipart.encoder import MultipartEncoder

springboot_domain = "192.168.31.183:5000"
logging.basicConfig(level = logging.DEBUG, format = "%(asctime)s - %(levelname)s - %(message)s")
class SvcDDSP:
    def __init__(self, model_path, vocoder_based_enhancer, enhancer_adaptive_key, input_pitch_extractor,
                 f0_min, f0_max, threhold, spk_id, spk_mix_dict, enable_spk_id_cover):
        self.model_path = model_path
        self.vocoder_based_enhancer = vocoder_based_enhancer
        self.enhancer_adaptive_key = enhancer_adaptive_key
        self.input_pitch_extractor = input_pitch_extractor
        self.f0_min = f0_min
        self.f0_max = f0_max
        self.threhold = threhold
        self.device = 'cuda' if torch.cuda.is_available() else 'cpu'
        self.spk_id = spk_id
        self.spk_mix_dict = spk_mix_dict
        self.enable_spk_id_cover = enable_spk_id_cover
        
        # load ddsp model
        self.model, self.args = load_model(self.model_path, device=self.device)
        
        # load units encoder
        if self.args.data.encoder == 'cnhubertsoftfish':
            cnhubertsoft_gate = self.args.data.cnhubertsoft_gate
        else:
            cnhubertsoft_gate = 10
        self.units_encoder = Units_Encoder(
            self.args.data.encoder,
            self.args.data.encoder_ckpt,
            self.args.data.encoder_sample_rate,
            self.args.data.encoder_hop_size,
            cnhubertsoft_gate=cnhubertsoft_gate,
            device=self.device)
        
        # load enhancer
        if self.vocoder_based_enhancer:
            self.enhancer = Enhancer(self.args.enhancer.type, self.args.enhancer.ckpt, device=self.device)

    def infer(self, input_wav, pitch_adjust, speaker_id, safe_prefix_pad_length):
        logging.info("Inference Start")
        # load input
        audio, sample_rate = librosa.load(input_wav, sr=None, mono=True)
        if len(audio.shape) > 1:
            audio = librosa.to_mono(audio)
        hop_size = self.args.data.block_size * sample_rate / self.args.data.sampling_rate
        
        # safe front silence
        if safe_prefix_pad_length > 0.03:
            silence_front = safe_prefix_pad_length - 0.03
        else:
            silence_front = 0
            
        # extract f0
        pitch_extractor = F0_Extractor(
            self.input_pitch_extractor,
            sample_rate,
            hop_size,
            float(self.f0_min),
            float(self.f0_max))
        f0 = pitch_extractor.extract(audio, uv_interp=True, device=self.device, silence_front=silence_front)
        f0 = torch.from_numpy(f0).float().to(self.device).unsqueeze(-1).unsqueeze(0)
        f0 = f0 * 2 ** (float(pitch_adjust) / 12)
        
        # extract volume
        volume_extractor = Volume_Extractor(hop_size)
        volume = volume_extractor.extract(audio)
        mask = (volume > 10 ** (float(self.threhold) / 20)).astype('float')
        mask = np.pad(mask, (4, 4), constant_values=(mask[0], mask[-1]))
        mask = np.array([np.max(mask[n : n + 9]) for n in range(len(mask) - 8)])
        mask = torch.from_numpy(mask).float().to(self.device).unsqueeze(-1).unsqueeze(0)
        mask = upsample(mask, self.args.data.block_size).squeeze(-1)
        volume = torch.from_numpy(volume).float().to(self.device).unsqueeze(-1).unsqueeze(0)

        # extract units
        audio_t = torch.from_numpy(audio).float().unsqueeze(0).to(self.device)
        units = self.units_encoder.encode(audio_t, sample_rate, hop_size)
        
        if self.enable_spk_id_cover:
            spk_id = self.spk_id
        else:
            spk_id = speaker_id
        spk_id = torch.LongTensor(np.array([[spk_id]])).to(self.device)
        
        # forward and return the output
        with torch.no_grad():
            output, _, (s_h, s_n) = self.model(units, f0, volume, spk_id = spk_id, spk_mix_dict = self.spk_mix_dict)
            output *= mask
            if self.vocoder_based_enhancer:
                output, output_sample_rate = self.enhancer.enhance(
                                                                output, 
                                                                self.args.data.sampling_rate, 
                                                                f0, 
                                                                self.args.data.block_size,
                                                                adaptive_key = self.enhancer_adaptive_key,
                                                                silence_front = silence_front)
            else:
                output_sample_rate = self.args.data.sampling_rate

            output = output.squeeze().cpu().numpy()
            return output, output_sample_rate

@celery.task(bind=True, name='tasks.svc')
def svc(self, svc_code, voice_model, input_file_io_base64):
    checkpoint_path = f"exp/sins-test/{voice_model}.pt"
    use_vocoder_based_enhancer = True
    enhancer_adaptive_key = 0
    select_pitch_extractor = 'crepe'
    limit_f0_min = 50
    limit_f0_max = 700
    threhold = -60
    spk_id = 1
    enable_spk_id_cover = True
    spk_mix_dict = None 
    svc_model = SvcDDSP(checkpoint_path, use_vocoder_based_enhancer, enhancer_adaptive_key, select_pitch_extractor,
                        limit_f0_min, limit_f0_max, threhold, spk_id, spk_mix_dict, enable_spk_id_cover)

    input_file_io = io.BytesIO(base64.b64decode(input_file_io_base64))
    input_file_io.seek(0)

    # 조바꿈 사용 시 사용 
    # audio, model_sample_rate = svc_model.infer(input_file_io, f_pitch_change, 1, 0)
    audio, model_sample_rate = svc_model.infer(input_file_io, 0, 1, 0)
    target_audio = librosa.resample(y=audio, orig_sr=model_sample_rate, target_sr=44100)
    
    output_file_io = io.BytesIO()
    sf.write(output_file_io, target_audio, 44100, format="wav")
    output_file_io.seek(0)

    multipart_encoder = MultipartEncoder(
        fields = {
            'file': (f"{svc_code}.wav", output_file_io.read(), 'audio/wav')
        }
    )
    headers = {'Content-Type': multipart_encoder.content_type}

    try:
        response = requests.post(f"http://{springboot_domain}/api/svc/{svc_code}/completion", data = multipart_encoder, headers = headers)
        logging.debug(f"응답 내용: {response.text}")
        return "Complete"
    except RequestException as e:
        if hasattr(e, 'response') and e.response is not None:
            if e.response.status_code == 404:
                logging.info("404 NOT FOUND: 요청한 리소스를 찾을 수 없습니다.")
            elif e.response.status_code == 500:
                logging.info("500 INTERNAL SERVER ERROR: 서버 내부 오류가 발생했습니다.")
            else:
                logging.info(f"Error: HTTP 응답 코드 {e.response.status_code}")
        else:
            logging.info(f"서버에 연결할 수 없습니다.: {e}")

        return "Failed"

@celery.task(bind=True, name='tasks.separate')
def separate(self, separation_code, input_extension, output_extension, input_file_io_base64):
    device = 'cuda' if torch.cuda.is_available() else 'cpu'
    args = utils.load_config('./configs/sins.yaml')

    MP4_DATA_PATH   = 'preprocess/mp4'
    ORIGINAL_PATH   = 'preprocess/original/'
    DEMUCS_PATH     = 'preprocess/demucs/'

    input_file_io = io.BytesIO(base64.b64decode(input_file_io_base64))
    input_file_io.seek(0)

    # wav 확장자로 변환한 후 ORIGINAL_PATH에 저장한다.
    with tempfile.NamedTemporaryFile(delete = False, suffix = f".{input_extension}") as temp_file:
        temp_file.write(base64.b64decode(input_file_io_base64))
        track = AudioSegment.from_file(temp_file.name, format = input_extension)
        track = track.set_frame_rate(44100)
        track.export(os.path.join(ORIGINAL_PATH, f"{separation_code}.wav"), format = "wav")
    os.remove(temp_file.name)

    demucs(ORIGINAL_PATH, f"{separation_code}.wav", DEMUCS_PATH)

    multipart_encoder = MultipartEncoder(
        fields = {
            "vocals": (f"{separation_code}_vocals.wav", open(f"{DEMUCS_PATH}/{separation_code}_vocals.wav", "rb"), 'audio/wav'),
            "drums": (f"{separation_code}_drums.wav", open(f"{DEMUCS_PATH}/{separation_code}_drums.wav", "rb"), 'audio/wav'),
            "bass": (f"{separation_code}_bass.wav", open(f"{DEMUCS_PATH}/{separation_code}_bass.wav", "rb"), 'audio/wav'),
            "other": (f"{separation_code}_other.wav", open(f"{DEMUCS_PATH}/{separation_code}_other.wav", "rb"), 'audio/wav')
        }
    )
    headers = {'Content-Type': multipart_encoder.content_type}

    try:
        response = requests.post(f"http://{springboot_domain}/api/separation/{separation_code}/completion", data = multipart_encoder, headers = headers)
        logging.debug(f"응답 내용: {response.text}")
        return "Complete"
    except RequestException as e:
        if hasattr(e, 'response') and e.response is not None:
            if e.response.status_code == 404:
                logging.info("404 NOT FOUND: 요청한 리소스를 찾을 수 없습니다.")
            elif e.response.status_code == 500:
                logging.info("500 INTERNAL SERVER ERROR: 서버 내부 오류가 발생했습니다.")
            else:
                logging.info(f"Error: HTTP 응답 코드 {e.response.status_code}")
        else:
            logging.info(f"서버에 연결할 수 없습니다.: {e}")

        return "Failed"

# TODO: checked 변수들 배열형태로 받아오게 수정할 것. 현재 파라미터가 너무 많다.
@celery.task(bind=True, name='tasks.mix')
def mix(self, separation_code, checked_vocals, checked_drums, checked_bass, checked_other):
    DEMUCS_PATH     = 'preprocess/demucs/'
    logging.info(f"인자 값(V, D, B, O): {checked_vocals}, {checked_drums}, {checked_bass}, {checked_other}")
    separated_files = [
        f"{DEMUCS_PATH}{separation_code}_vocals.wav" if checked_vocals else None,
        f"{DEMUCS_PATH}{separation_code}_drums.wav" if checked_drums else None,
        f"{DEMUCS_PATH}{separation_code}_bass.wav" if checked_bass else None,
        f"{DEMUCS_PATH}{separation_code}_other.wav" if checked_other else None
    ]

    ffmpeg_input_files = [file for file in separated_files if file is not None]
    ffmpeg_inputs = [ffmpeg.input(filename) for filename in ffmpeg_input_files]

    mixed = ffmpeg.filter(ffmpeg_inputs, 'amix', inputs = len(ffmpeg_input_files), duration = 'longest')

    # 믹싱 된 파일 저장
    try:
        ffmpeg.output(mixed, f"{DEMUCS_PATH}{separation_code}.wav").run()
        logging.info(f"Mixing 완료. 파일 저장: {DEMUCS_PATH}{separation_code}.wav")
    except Exception as e:
        logging.info(f"ffmpeg 파일 저장 중 오류 발생: {e}")

    # BE로 결과 송신
    multipart_encoder = MultipartEncoder(
        fields = {
            "file": (f"{separation_code}.wav", open(f"{DEMUCS_PATH}{separation_code}.wav", "rb"), 'audio/wav'),
        }
    )
    headers = {'Content-Type': multipart_encoder.content_type}

    try:
        response = requests.post(f"http://{springboot_domain}/api/separation/{separation_code}/mixing/completion", data = multipart_encoder, headers = headers)
        logging.debug(f"응답 내용: {response.text}")
        return "Complete"
    except RequestException as e:
        if hasattr(e, 'response') and e.response is not None:
            if e.response.status_code == 404:
                logging.info("404 NOT FOUND: 요청한 리소스를 찾을 수 없습니다.")
            elif e.response.status_code == 500:
                logging.info("500 INTERNAL SERVER ERROR: 서버 내부 오류가 발생했습니다.")
            else:
                logging.info(f"Error: HTTP 응답 코드 {e.response.status_code}")
        else:
            logging.info(f"서버에 연결할 수 없습니다.: {e}")

        return "Failed"

@celery.task(bind=True, name='tasks.svc_separation')
def svc_separation(self, svc_code, checked_infos, model_infos):
    DEMUCS_PATH     = 'preprocess/demucs/'

    use_vocoder_based_enhancer = True
    enhancer_adaptive_key = 0
    select_pitch_extractor = 'crepe'
    limit_f0_min = 50
    limit_f0_max = 700
    threhold = -60
    spk_id = 1
    enable_spk_id_cover = True
    spk_mix_dict = None 

    logging.info(checked_infos)
    logging.info(model_infos)

    # 4개의 음원을 추론.
    with open(f"{DEMUCS_PATH}{svc_code}_vocals.wav", "rb") as vocals_file:
        vocals = vocals_file.read()
        vocals_io = io.BytesIO(vocals)
        vocals_io.seek(0)
    if checked_infos[0]:
            checkpoint_path = f"exp/sins-test/{model_infos[0]}.pt"
            svc_model = SvcDDSP(checkpoint_path, use_vocoder_based_enhancer, enhancer_adaptive_key, select_pitch_extractor,
                                limit_f0_min, limit_f0_max, threhold, spk_id, spk_mix_dict, enable_spk_id_cover)
            vocals_audio, vocals_model_sample_rate = svc_model.infer(vocals_io, 0, 1, 0)
            vocals_target_audio = librosa.resample(y=vocals_audio, orig_sr=vocals_model_sample_rate, target_sr=44100)
            
            vocals_output_file_io = io.BytesIO()
            sf.write(vocals_output_file_io, vocals_target_audio, 44100, format="wav")
    else:
        vocals_output_file_io = vocals_io

    with open(f"{DEMUCS_PATH}/{svc_code}_svc_vocals.wav", "wb") as svc_file:
        vocals_output_file_io.seek(0)
        svc_file.write(vocals_output_file_io.read())
    
    vocals_output_file_io.seek(0)


    with open(f"{DEMUCS_PATH}{svc_code}_drums.wav", "rb") as drums_file:
        drums = drums_file.read()
        drums_io = io.BytesIO(drums)
        drums_io.seek(0)
    if checked_infos[1]:
            checkpoint_path = f"exp/sins-test/{model_infos[1]}.pt"
            svc_model = SvcDDSP(checkpoint_path, use_vocoder_based_enhancer, enhancer_adaptive_key, select_pitch_extractor,
                                limit_f0_min, limit_f0_max, threhold, spk_id, spk_mix_dict, enable_spk_id_cover)
            drums_audio, drums_model_sample_rate = svc_model.infer(drums_io, 0, 1, 0)
            drums_target_audio = librosa.resample(y=drums_audio, orig_sr=drums_model_sample_rate, target_sr=44100)
            
            drums_output_file_io = io.BytesIO()
            sf.write(drums_output_file_io, drums_target_audio, 44100, format="wav")
    else:
        drums_output_file_io = drums_io
    
    with open(f"{DEMUCS_PATH}/{svc_code}_svc_drums.wav", "wb") as svc_file:
        drums_output_file_io.seek(0)
        svc_file.write(drums_output_file_io.read())
    
    drums_output_file_io.seek(0)

    with open(f"{DEMUCS_PATH}{svc_code}_bass.wav", "rb") as bass_file:
        bass = bass_file.read()
        bass_io = io.BytesIO(bass)
        bass_io.seek(0)
    if checked_infos[2]:
            checkpoint_path = f"exp/sins-test/{model_infos[2]}.pt"
            svc_model = SvcDDSP(checkpoint_path, use_vocoder_based_enhancer, enhancer_adaptive_key, select_pitch_extractor,
                                limit_f0_min, limit_f0_max, threhold, spk_id, spk_mix_dict, enable_spk_id_cover)
            bass_audio, bass_model_sample_rate = svc_model.infer(bass_io, 0, 1, 0)
            bass_target_audio = librosa.resample(y=bass_audio, orig_sr=bass_model_sample_rate, target_sr=44100)
            
            bass_output_file_io = io.BytesIO()
            sf.write(bass_output_file_io, bass_target_audio, 44100, format="wav")
    else:
        bass_output_file_io = bass_io

    with open(f"{DEMUCS_PATH}/{svc_code}_svc_bass.wav", "wb") as svc_file:
        bass_output_file_io.seek(0)
        svc_file.write(bass_output_file_io.read())
    
    bass_output_file_io.seek(0)

    with open(f"{DEMUCS_PATH}{svc_code}_other.wav", "rb") as other_file:
        other = other_file.read()
        other_io = io.BytesIO(other)
        other_io.seek(0)
    if checked_infos[3]:
            checkpoint_path = f"exp/sins-test/{model_infos[3]}.pt"
            svc_model = SvcDDSP(checkpoint_path, use_vocoder_based_enhancer, enhancer_adaptive_key, select_pitch_extractor,
                                limit_f0_min, limit_f0_max, threhold, spk_id, spk_mix_dict, enable_spk_id_cover)
            other_audio, other_model_sample_rate = svc_model.infer(other_io, 0, 1, 0)
            other_target_audio = librosa.resample(y=other_audio, orig_sr=other_model_sample_rate, target_sr=44100)
            
            other_output_file_io = io.BytesIO()
            sf.write(other_output_file_io, other_target_audio, 44100, format="wav")
    else:
        other_output_file_io = other_io
    
    with open(f"{DEMUCS_PATH}{svc_code}_svc_other.wav", "wb") as svc_file:
        other_output_file_io.seek(0)
        svc_file.write(other_output_file_io.read())
    
    other_output_file_io.seek(0)

    multipart_encoder = MultipartEncoder(
        fields = {
            "vocals": (f"{svc_code}_svc_vocals.wav", vocals_output_file_io.read(), 'audio/wav'),
            "drums": (f"{svc_code}_svc_drums.wav", drums_output_file_io.read(), 'audio/wav'),
            "bass": (f"{svc_code}_svc_bass.wav", bass_output_file_io.read(), 'audio/wav'),
            "other": (f"{svc_code}_svc_other.wav", other_output_file_io.read(), 'audio/wav'),
        }
    )
    headers = {'Content-Type': multipart_encoder.content_type}

    try:
        response = requests.post(f"http://{springboot_domain}/api/svc/separation/{svc_code}/completion", data = multipart_encoder, headers = headers)
        logging.debug(f"응답 내용: {response.text}")
        return "Complete"
    except RequestException as e:
        if hasattr(e, 'response') and e.response is not None:
            if e.response.status_code == 404:
                logging.info("404 NOT FOUND: 요청한 리소스를 찾을 수 없습니다.")
            elif e.response.status_code == 500:
                logging.info("500 INTERNAL SERVER ERROR: 서버 내부 오류가 발생했습니다.")
            else:
                logging.info(f"Error: HTTP 응답 코드 {e.response.status_code}")
        else:
            logging.info(f"서버에 연결할 수 없습니다.: {e}")

        return "Failed"

@celery.task(bind=True, name='tasks.svc_separation_mix')
def svc_separation_mix(self, svc_code, checked_infos):
    DEMUCS_PATH = 'preprocess/demucs/'
    logging.info(svc_code)
    logging.info(f"{checked_infos}")
    separated_files = [
        f"{DEMUCS_PATH}{svc_code}_svc_vocals.wav" if checked_infos[0] else None,
        f"{DEMUCS_PATH}{svc_code}_svc_drums.wav" if checked_infos[1] else None,
        f"{DEMUCS_PATH}{svc_code}_svc_bass.wav" if checked_infos[2] else None,
        f"{DEMUCS_PATH}{svc_code}_svc_other.wav" if checked_infos[3] else None
    ]

    ffmpeg_input_files = [file for file in separated_files if file is not None]
    ffmpeg_inputs = [ffmpeg.input(filename) for filename in ffmpeg_input_files]
    logging.info(ffmpeg_input_files)
    logging.info(ffmpeg_inputs)
    mixed = ffmpeg.filter(ffmpeg_inputs, 'amix', inputs = len(ffmpeg_input_files), duration = 'longest')

    # 믹싱 된 파일 저장
    try:
        ffmpeg.output(mixed, f"{DEMUCS_PATH}{svc_code}_svc.wav").run()
        logging.info(f"Mixing 완료. 파일 저장: {DEMUCS_PATH}{svc_code}.wav")
    except Exception as e:
        logging.info(f"ffmpeg 파일 저장 중 오류 발생: {e}")

    # BE로 결과 송신
    multipart_encoder = MultipartEncoder(
        fields = {
            "file": (f"{svc_code}_svc.wav", open(f"{DEMUCS_PATH}{svc_code}_svc.wav", "rb"), 'audio/wav'),
        }
    )
    headers = {'Content-Type': multipart_encoder.content_type}

    try:
        response = requests.post(f"http://{springboot_domain}/api/svc/separation/{svc_code}/mixing/completion", data = multipart_encoder, headers = headers)
        logging.info(f"응답 내용: {response.text}")
        return "Complete"
    except RequestException as e:
        if hasattr(e, 'response') and e.response is not None:
            if e.response.status_code == 404:
                logging.info("404 NOT FOUND: 요청한 리소스를 찾을 수 없습니다.")
            elif e.response.status_code == 500:
                logging.info("500 INTERNAL SERVER ERROR: 서버 내부 오류가 발생했습니다.")
            else:
                logging.info(f"Error: HTTP 응답 코드 {e.response.status_code}")
        else:
            logging.info(f"서버에 연결할 수 없습니다.: {e}")

        return "Failed"