# API3

Spring 서버와 연결되는 AI를 위한 Python 서버

# LOCAL환경 API3 구동

1. 필요 라이브러리 설치
   프로젝트 폴더에 이동하여 아래 명령어 실행

```
pip install -r requirements.txt
```

2. Flask Server 구동

```
python flask_api.py
```

3. Celery Worker 구동

```
celery -A celery_worker worker --loglevel=info --pool=eventlet
```
