from fastapi import FastAPI
from pydantic import BaseModel
from transformers import pipeline


app = FastAPI(title="cloudscanner-bert-classifier")
classifier = pipeline(
    "zero-shot-classification",
    model="facebook/bart-large-mnli",
)
LABELS = [
    "confidential document",
    "payment information",
    "internal data",
    "public content",
]


class ClassificationRequest(BaseModel):
    text: str


@app.post("/classify")
def classify_text(request: ClassificationRequest) -> dict:
    raw_prediction = classifier(request.text, LABELS, multi_label=True)

    prediction = []
    for label, score in zip(raw_prediction["labels"], raw_prediction["scores"]):
        prediction.append({"label": label.upper().replace(" ", "_"), "score": round(float(score), 4)})

    return {"prediction": prediction}
