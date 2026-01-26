from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from app.services.summarizer import summarize_text

router = APIRouter()

class SummarizeRequest(BaseModel):
    text: str
    max_sentences: int = 5

class SummarizeResponse(BaseModel):
    summary: str
    bullets: list[str]

@router.post("/summarize", response_model=SummarizeResponse, tags=["Summarization"])
async def summarize(request: SummarizeRequest):
    """
    Генерирует краткую выжимку (summary) из текста.
    """
    if not request.text or len(request.text.strip()) == 0:
         raise HTTPException(status_code=400, detail="Text is empty")

    try:
        result = summarize_text(request.text, num_sentences=request.max_sentences)
        return SummarizeResponse(
            summary=result["summary"],
            bullets=result["bullets"]
        )
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
