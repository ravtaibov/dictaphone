FROM python:3.11-slim

# Устанавливаем системные зависимости (ffmpeg понадобится для обработки аудио)
RUN apt-get update && apt-get install -y \
    ffmpeg \
    pkg-config \
    build-essential \
    libavformat-dev \
    libavcodec-dev \
    libavdevice-dev \
    libavutil-dev \
    libswresample-dev \
    libswscale-dev \
    libavfilter-dev \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Копируем requirements и устанавливаем зависимости
COPY requirements.txt .
RUN pip install --no-cache-dir --upgrade pip && \
    pip install --no-cache-dir -r requirements.txt

# Копируем код приложения
COPY app/ ./app/

# Открываем порт
EXPOSE 8000

# Запускаем приложение (в режиме reload для разработки)
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000", "--reload"]
