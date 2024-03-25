from celery_config import celery

@celery.task(bind=True, name='tasks.add')
def id(self):
    return