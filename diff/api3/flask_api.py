import logging
import os
import redis
import time

from redis_config import redis_instance as r
from functools import wraps
from werkzeug.utils import secure_filename
from flask import Flask, request, send_file, jsonify
from flask_cors import CORS

from tasks import id

app = Flask(__name__)

CORS(app)

logging.getLogger('numba').setLevel(logging.WARNING)

# 허용되는 파일 확장자 설정
ALLOWED_EXTENSIONS = {'wav', 'mp3'}

# 파일이 허용되는 확장자를 가지고 있는지 확인
def allowed_file(filename):
    return '.' in filename and \
           filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS

# 파일 검사 및 저장 함수
def check_and_save_file(file, folder):
    if not os.path.exists(folder):
        os.makedirs(folder)

    # 파일이 선택되었는지 확인
    if file.filename == '':
        return {'error': 'No selected file'}, 400

    # 허용되는 파일 형식인지 확인
    if file and allowed_file(file.filename):
        filename = secure_filename(file.filename)
        file_path = os.path.join(folder, filename)
        file.save(file_path)
        return {'success': True}, 200
    else:
        return {'error': 'File type not allowed'}, 400


LIMIT_TIME = 5

def limit_requests(f):
    @wraps(f)
    def decorated_function(*args, **kwargs):
        client_ip = request.remote_addr
        current_time = time.time()
        last_request_time = r.get(client_ip)

        # 요청 제한 체크
        if last_request_time is not None:
            last_request_time = float(last_request_time)
            if current_time - last_request_time < LIMIT_TIME:
                return jsonify({"error": "Request too frequently"}), 429
        
        r.setex(client_ip, LIMIT_TIME, current_time)
        return f(*args, **kwargs)
    return decorated_function

@app.route("/api3/svc", methods=["POST"])
@limit_requests
def request_svc():
    task = id.delay()
    # 보컬 파일 파라메타 처리
    vocalFile = request.files.get('vocalFile')
    if vocalFile is None:
        return jsonify({'error': 'No vocalFile part'}), 400

    vocal_save_dir = f'input/{task.id}/vocal'
    result, status = check_and_save_file(vocalFile, vocal_save_dir)
    if status != 200:
        return jsonify(result), status

    # 음악 파일 처리
    musicFile = request.files.get('musicFile')
    if musicFile is None:
        return jsonify({'error': 'No musicFile part'}), 400

    music_save_dir = f'input/{task.id}/music'
    result, status = check_and_save_file(musicFile, music_save_dir)
    if status != 200:
        return jsonify(result), status


    message = "Request received"

    # 결과 폴더 생성
    output = f'output/{task.id}'
    if not os.path.exists(output):
        os.makedirs(output)
    
    return jsonify({"message": message, "message_queue_id": task.id}), 200


@app.route('/api3/status/<task_id>')
def task_status(task_id):
    base_path = 'output'
    task_folder = os.path.join(base_path, task_id)
    result_file_path = os.path.join(task_folder, 'result.wav')

    if not os.path.exists(task_folder):
        return jsonify({'error': 'Invalid task ID'}), 400

    if os.path.isfile(result_file_path):
        return send_file(result_file_path, as_attachment=True)
    else:
        return jsonify({'message': 'Processing is in progress'}), 202


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=3000, debug=True, threaded=False)
