from fastapi import FastAPI
from pydantic import BaseModel
import re
import spacy


app = FastAPI(title="cloudscanner-nlp-service")
nlp = spacy.load("en_core_web_sm")
EMAIL_PATTERN = re.compile(r"\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}\b")
LOCATION_LABELS = {"GPE", "LOC", "FAC"}


class DetectionRequest(BaseModel):
    text: str


@app.post("/detect")
def detect_entities(request: DetectionRequest) -> dict:
    doc = nlp(request.text)
    entities = []

    for ent in doc.ents:
        label = ent.label_
        if label in {"PERSON", "ORG"}:
            entities.append({"text": ent.text, "label": label})
        elif label in LOCATION_LABELS:
            entities.append({"text": ent.text, "label": "LOCATION"})

    for match in EMAIL_PATTERN.finditer(request.text):
        entities.append({"text": match.group(0), "label": "EMAIL"})

    return {"entities": entities}
