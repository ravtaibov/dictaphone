from fastapi import APIRouter, UploadFile, File, Form, HTTPException, BackgroundTasks
from app.utils.validation import validate_audio_file
from app.utils.tempfiles import save_upload_file, cleanup_file
import shutil

router = APIRouter()

@router.post("/transcribe", tags=["Transcription"])
async def transcribe_audio(
    background_tasks: BackgroundTasks,
    file: UploadFile = File(...),
    lang: str = Form("ru"),
    return_segments: bool = Form(False)
):
    """
    Принимает аудиофайл, валидирует его и (в будущем) запускает транскрипцию.
    """
    # 1. Валидация
    await validate_audio_file(file)
    
    # 2. Сохранение во временный файл
    temp_path = await save_upload_file(file)
    
    # Добавляем задачу на удаление файла после ответа (или после обработки)
    # В реальной асинхронной задаче удаление будет внутри воркера, 
    # но пока для MVP удалим через background_tasks, если обработка синхронная,
    # или оставим (если будем передавать путь дальше).
    # Пока что просто логируем путь.
    
    try:
        # TODO: Здесь будет вызов сервиса транскрипции
        # result = await transcriber_service.process(temp_path, lang)
        
        # MOCK RESPONSE (заглушка для проверки API)
        mock_text = f"Это заглушка транскрипции для файла {file.filename}. Язык: {lang}."
        
        # Удаляем файл сразу после 'обработки' (так как это заглушка)
        background_tasks.add_task(cleanup_file, temp_path)
        
        response = {
            "text": mock_text,
            "language": lang,
            "duration_sec": 0, # Пока не считаем
        }
        
        if return_segments:
            response["segments"] = []
            
        return response

    except Exception as e:
        # Если ошибка, всё равно чистим за собой
        cleanup_file(temp_path)
        raise HTTPException(status_code=500, detail=str(e))
