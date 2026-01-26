from fastapi import APIRouter, UploadFile, File, Form, HTTPException, BackgroundTasks, Depends
from app.utils.validation import validate_audio_file
from app.utils.tempfiles import save_upload_file, cleanup_file
from app.services.audio_convert import convert_to_wav_16k
from app.services.transcriber import get_transcriber_service, TranscriberService
import shutil
import os

router = APIRouter()

@router.post("/transcribe", tags=["Transcription"])
async def transcribe_audio(
    background_tasks: BackgroundTasks,
    file: UploadFile = File(...),
    lang: str = Form("ru"),
    return_segments: bool = Form(False),
    transcriber: TranscriberService = Depends(get_transcriber_service)
):
    """
    Принимает аудиофайл, конвертирует его в WAV 16kHz и транскрибирует через Whisper.
    """
    # 1. Валидация
    await validate_audio_file(file)
    
    # 2. Сохранение исходного файла
    original_path = await save_upload_file(file)
    converted_path = None
    
    try:
        # 3. Конвертация в WAV 16kHz (требование Whisper для корректной работы)
        converted_path = convert_to_wav_16k(original_path)
        
        # 4. Транскрипция (синхронный вызов, так как faster-whisper блокирующий, 
        # но в рамках одного воркера это ок для MVP. Для high-load нужно выносить в Celery/RQ)
        result = transcriber.transcribe(converted_path, lang=lang, return_segments=return_segments)
        
        # 5. Очистка (ставим в фон, чтобы не задерживать ответ)
        # Удаляем и оригинал, и сконвертированный wav
        background_tasks.add_task(cleanup_file, original_path)
        background_tasks.add_task(cleanup_file, converted_path)
        
        return result

    except Exception as e:
        # Если ошибка — чистим сразу
        cleanup_file(original_path)
        if converted_path:
            cleanup_file(converted_path)
        
        print(f"Transcription error: {e}")
        raise HTTPException(status_code=500, detail=str(e))
