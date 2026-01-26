import os
from fastapi import UploadFile, HTTPException

# Получаем настройки из ENV (с дефолтными значениями, если вдруг не подтянулись)
MAX_UPLOAD_MB = int(os.getenv("MAX_UPLOAD_MB", 200))
ALLOWED_EXTENSIONS = {".m4a", ".aac", ".mp3", ".wav", ".ogg", ".webm"}

async def validate_audio_file(file: UploadFile):
    """
    Проверяет файл на допустимый размер и расширение.
    Выбрасывает HTTPException, если проверка не пройдена.
    """
    # 1. Проверка расширения (по имени файла)
    filename = file.filename or ""
    _, ext = os.path.splitext(filename)
    if ext.lower() not in ALLOWED_EXTENSIONS:
        raise HTTPException(
            status_code=400,
            detail=f"Unsupported file extension: {ext}. Allowed: {ALLOWED_EXTENSIONS}"
        )

    # 2. Проверка размера файла (читаем чанками, чтобы не грузить память, или используем content-length)
    # content-length может быть подделан, но для MVP доверяем Nginx/Uvicorn ограничению + проверка тут.
    # Но так как UploadFile - это SpooledTemporaryFile, мы можем проверить размер после сохранения
    # или попробовать оценить file.size (если доступен).
    
    # Для надежности в FastAPI лучше проверять размер при чтении/сохранении, 
    # но пока сделаем простую проверку, если Uvicorn передал Content-Length.
    file_size = file.size
    if file_size and file_size > MAX_UPLOAD_MB * 1024 * 1024:
         raise HTTPException(
            status_code=413,
            detail=f"File too large. Limit is {MAX_UPLOAD_MB}MB"
        )
    
    return True
