from fastapi import FastAPI
from app.api import health, transcribe
from dotenv import load_dotenv
import os

# Загружаем переменные окружения
load_dotenv()

app = FastAPI(
    title="DictaTranscribe API",
    version="0.1.0",
    description="Backend for DictaTranscribe Android App"
)

# Подключаем роутеры
app.include_router(health.router)
app.include_router(transcribe.router)

@app.on_event("startup")
async def startup_event():
    print(f"Starting up in {os.getenv('APP_ENV', 'unknown')} mode...")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
