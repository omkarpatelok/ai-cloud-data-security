AI services in this folder should be run with Python 3.13 on this machine.

Python 3.14 is not currently compatible with the spaCy stack used by `nlp_service.py`.

Setup:

```powershell
cd C:\Projects\ai-cloud-data-security\ai-services
py -3.13 -m venv .venv
.venv\Scripts\Activate.ps1
python -m pip install --upgrade pip
pip install -r requirements.txt
python -m spacy download en_core_web_sm
```

Run NLP service:

```powershell
uvicorn nlp_service:app --host 0.0.0.0 --port 9000
```

Run BERT service:

```powershell
uvicorn bert_classifier:app --host 0.0.0.0 --port 9100
```

If you already created the virtual environment with Python 3.14, delete `.venv` and recreate it with `py -3.13 -m venv .venv`.
