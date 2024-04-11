import io
import logging
from flask import Flask, request, send_file, jsonify
from flask_cors import CORS

import asyncio
import aiohttp
import base64

from celery_worker import celery
from tasks import svc
from tasks import separate
from tasks import mix
from tasks import svc_separation
from tasks import svc_separation_mix

app = Flask(__name__)
CORS(app)
logging.getLogger("numba").setLevel(logging.DEBUG)

@app.route("/api2/svc/<svccode>", methods=["POST"])
async def request_svc(svccode):
    request_form = request.form
    svc_code = svccode
    voice_model = request_form.get("voice")
    input_file = request.files.get("file", None)
    message_queue_id = "None"

    if  not svc_code or not voice_model or input_file is None:
        message = "Missing parameters"
        code = 400
    else:
        input_file_base64 = base64.b64encode(input_file.read()).decode('utf-8')
        task = svc.delay(svc_code, voice_model, input_file_base64)
        message_queue_id = task.id

        message = "Request received"
        code = 202

    return jsonify({"message": message, "message_queue_id": message_queue_id}), code

@app.route("/api2/separation/<separationcode>", methods=["POST"])
async def request_separation(separationcode):
    # TODO: Stereo Type에서 Mono Type으로 변경할 때 생기는 문제 해결할 것.

    request_form = request.form
    separation_code = separationcode
    input_extension = request_form.get("inputExtension")
    output_extension = request_form.get("outputExtension")
    input_file = request.files.get("file", None)
    message_queue_id = "None"

    if not separation_code or not output_extension or input_file is None:
        message = "Missing parameters"
        code = 400
    else:
        input_file_base64 = base64.b64encode(input_file.read()).decode('utf-8')
        task = separate.delay(separation_code, input_extension, output_extension, input_file_base64)
        message_queue_id = task.id

        message = "Request received"
        code = 202

    return jsonify({"message": message, "message_queue_id": message_queue_id}), code

@app.route("/api2/separation/<separationcode>/mixing", methods=["POST"])
async def request_separation_mixing(separationcode):
    request_form = request.get_json()
    separation_code = separationcode
    checked_vocals = request_form.get("vocals")
    checked_drums = request_form.get("drums")
    checked_bass = request_form.get("bass")
    checked_other = request_form.get("other")
    message_queue_id = "None"

    if not separation_code:
        message = "Missing Parameters"
        code = 400
    else:
        task = mix.delay(separation_code, checked_vocals, checked_drums, checked_bass, checked_other)
        message_queue_id = task.id
        message = "Request received"
        code = 202

    return jsonify({"message": message, "message_queue_id": message_queue_id}), code

@app.route("/api2/svc/separation/<svccode>", methods = ["POST"])
async def request_svc_separation(svccode):
    request_form = request.get_json()
    svc_code = svccode
    checked_vocals = request_form.get("vocals")
    checked_drums = request_form.get("drums")
    checked_bass = request_form.get("bass")
    checked_other = request_form.get("other")
    vocals_model = request_form.get("vocals_model")
    drums_model = request_form.get("drums_model")
    bass_model = request_form.get("bass_model")
    other_model = request_form.get("other_model")
    message_queue_id = "None"

    checked_infos = [checked_vocals, checked_drums, checked_bass, checked_other]
    model_infos = [vocals_model, drums_model, bass_model, other_model]

    if not svc_code:
        message = "Missing Parameters"
        code = 400
    else:
        task = svc_separation.delay(svc_code, checked_infos, model_infos)
        message_queue_id = task.id
        message = "Request received"
        code = 202

    return jsonify({"message": message, "message_queue_id": message_queue_id}), code

@app.route("/api2/svc/separation/<svccode>/mixing", methods=["POST"])
async def request_svc_separation_mixing(svccode):
    request_form = request.get_json()
    svc_code = svccode
    checked_vocals = request_form.get("vocals")
    checked_drums = request_form.get("drums")
    checked_bass = request_form.get("bass")
    checked_other = request_form.get("other")
    message_queue_id = "None"

    checked_infos = [checked_vocals, checked_drums, checked_bass, checked_other]

    if not svc_code:
        message = "Missing Parameters"
        code = 400
    else:
        task = svc_separation_mix.delay(svc_code, checked_infos)
        message_queue_id = task.id
        message = "Request received"
        code = 202

    return jsonify({"message": message, "message_queue_id": message_queue_id}), code

@app.route('/api2/status/<task_id>')
def task_status(task_id):
    task = celery.AsyncResult(task_id)
    if task.state == 'PENDING':
        response = {
            'state': task.state,
            'status': 'Pending...'
        }
    elif task.state != 'FAILURE':
        response = {
            'state': task.state,
            'status': task.info.get('status', '')
        }
        if 'result' in task.info:
            response['result'] = task.info['result']
    else:
        # 작업 실패
        response = {
            'state': task.state,
            'status': str(task.info),  # 예외 정보
        }
    return jsonify(response)




if __name__ == "__main__":
    app.run(port=6000, host="0.0.0.0", debug=False, threaded=False)
