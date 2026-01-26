import subprocess
import os
from pathlib import Path

def convert_to_wav_16k(input_path: Path) -> Path:
    """
    Конвертирует входной аудиофайл в WAV 16kHz mono, используя системный ffmpeg.
    Возвращает путь к новому файлу.
    """
    output_path = input_path.with_suffix(".wav")
    
    # Команда ffmpeg:
    # -i input
    # -ar 16000 (sample rate 16k)
    # -ac 1 (mono channel)
    # -c:a pcm_s16le (codec wav pcm 16 bit)
    # -y (overwrite output)
    command = [
        "ffmpeg",
        "-i", str(input_path),
        "-ar", "16000",
        "-ac", "1",
        "-c:a", "pcm_s16le",
        "-y",
        str(output_path)
    ]
    
    try:
        # Запускаем конвертацию. capture_output=True скроет мусор в логах, если все ок.
        subprocess.run(command, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)
        return output_path
    except subprocess.CalledProcessError as e:
        # Если ffmpeg упал, выводим ошибку из stderr
        error_msg = e.stderr.decode() if e.stderr else "Unknown error"
        raise RuntimeError(f"FFmpeg conversion failed: {error_msg}")

def get_audio_duration(file_path: Path) -> float:
    """
    Возвращает длительность аудио в секундах через ffprobe (идет вместе с ffmpeg).
    """
    command = [
        "ffprobe",
        "-v", "error",
        "-show_entries", "format=duration",
        "-of", "default=noprint_wrappers=1:nokey=1",
        str(file_path)
    ]
    try:
        result = subprocess.run(command, check=True, capture_output=True, text=True)
        return float(result.stdout.strip())
    except (subprocess.CalledProcessError, ValueError):
        return 0.0
