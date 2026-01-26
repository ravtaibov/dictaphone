from fastapi import APIRouter

router = APIRouter()

@router.get("/health", tags=["Health"])
async def check_health():
    """
    Проверка работоспособности сервиса.
    """
    return {"status": "ok"}
