pip install -U pip setuptools wheel
pip install -U spacy waitress hug_middleware_cors hug
python -m spacy download en_core_web_lg
python -m spacy download pt_core_news_lg
waitress-serve --listen=0.0.0.0:2800 app_hug:__hug_wsgi__
