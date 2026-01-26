import os
import uuid
import shutil
from pathlib import Path
from fastapi import UploadFile

TEMP_DIR = Path("/tmp/dictatranscribe_uploads") if os.name != 'nt' else Path("temp_uploads")

def get_temp_file_path(extension: str) -> Path:
    """Генерирует уникальный путь для временного файла."""
    TEMP_DIR.mkdir(parents=True, exist_ok=True)
    filename = f"{uuid.uuid4()}{extension}"
    return TEMP_DIR / filename

async def save_upload_file(upload_file: UploadFile) -> Path:
    """Сохраняет загруженный файл во временную директорию."""
    filename = upload_file.filename or "audio.tmp"
    _, ext = os.path.splitext(filename)
    if not ext:
        ext = ".tmp"
    
    temp_path = get_temp_file_path(ext)
    
    with open(temp_path, "wb") as buffer:
        # Копируем содержимое из UploadFile (spooled) в реальный файл
        shutil.copyfileobj(upload_file.file, buffer)
        
    return temp_path

def cleanup_file(file_path: Path):
    """Удаляет временный файл, если он существует."""
    try:
        if file_path.exists():
            os.remove(file_path)
    except Exception as e:
        print(f"Error deleting temp file {file_path}: {e}")
