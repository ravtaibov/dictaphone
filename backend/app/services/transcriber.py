import os
from faster_whisper import WhisperModel
from pathlib import Path

# Настройки модели из ENV
MODEL_SIZE = os.getenv("WHISPER_MODEL", "small")
DEVICE = os.getenv("WHISPER_DEVICE", "cpu")
COMPUTE_TYPE = os.getenv("WHISPER_COMPUTE_TYPE", "int8")

class TranscriberService:
    def __init__(self):
        print(f"Loading Whisper model: {MODEL_SIZE} on {DEVICE} ({COMPUTE_TYPE})...")
        # Загружаем модель один раз при старте сервиса
        self.model = WhisperModel(MODEL_SIZE, device=DEVICE, compute_type=COMPUTE_TYPE)
        print("Whisper model loaded successfully.")

    def transcribe(self, audio_path: Path, lang: str = "ru", return_segments: bool = False):
        """
        Выполняет транскрипцию аудиофайла.
        audio_path должен указывать на файл WAV 16kHz (желательно).
        """
        # language=None позволяет автоопределить язык, но если передан конкретный - используем его
        language_arg = lang if lang and lang != "auto" else None

        segments_generator, info = self.model.transcribe(
            str(audio_path),
            beam_size=5,
            language=language_arg
        )

        segments_list = []
        full_text_parts = []

        for segment in segments_generator:
            text = segment.text.strip()
            full_text_parts.append(text)
            
            if return_segments:
                segments_list.append({
                    "start": segment.start,
                    "end": segment.end,
                    "text": text
                })

        full_text = " ".join(full_text_parts)

        return {
            "text": full_text,
            "language": info.language,
            "duration_sec": info.duration,
            "segments": segments_list if return_segments else []
        }

# Глобальный инстанс сервиса (Singleton)
# В FastAPI это нормально, если мы хотим держать модель в памяти.
_service_instance = None

def get_transcriber_service():
    global _service_instance
    if _service_instance is None:
        _service_instance = TranscriberService()
    return _service_instance
