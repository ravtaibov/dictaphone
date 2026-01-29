# DictaTranscribe — ТЗ + Project Bible (единый документ) 
 
 
 
 **Правила (анти-сбои):** 
 1) Любые изменения — маленькими итерациями. Не переписывать проект целиком. 
 2) Безопасность: фронтенду не доверяем. Проверки/ограничения — на backend. Секреты/токены — только в `.env`, `.env` в `.gitignore`. 
 3) Если ошибка — сначала запросить **логи** (терминал Android Studio / Logcat / docker logs). Не гадать. 
 4) Проект по умолчанию контейнеризируется: backend поднимается `docker compose up --build`. 
 5) Перед “деплоем” — локально всё поднимается одной командой и есть короткий чек “как проверить”. 
 6) Этот файл — **источник правды**. Все важные изменения архитектуры должны отражаться здесь. 
 
 --- 
 
 ## 1) Идея продукта (что делаем) 
 **Android приложение (MVP):** 
 - качественный диктофон (запись речи, простая и стабильная), 
 - отправка выбранной записи на backend, 
 - backend делает транскрипцию (Whisper / faster-whisper), 
 - приложение показывает текст + сохраняет историю локально, 
 - экспорт/шаринг текста (TXT) и (минимум) шаринг аудио файла, 
 - (опционально) выжимка/summary из текста без платных ключей. 
 
 **Важно про “шёпот”:** 
 мы не можем “магически” сделать любой микрофон слышащим шёпот. Улучшаем шанс: 
 - корректный источник записи (VOICE_RECOGNITION/MIC), 
 - нормальный формат аудио, 
 - на backend делаем нормализацию/усиление (ffmpeg) перед Whisper. 
 
 --- 
 
 ## 2) Целевая аудитория и ценность 
 - Сейчас: **1 пользователь (ты)**. 
 - Ценность: “записал → получил текст → поделился/сохранил”. 
 
 --- 
 
 ## 3) MVP-скоуп (фиксируем) 
 **В MVP входит:** 
 1) Android: запись аудио (m4a/aac), сохранение файла локально. 
 2) Android: список записей (история), экран деталей. 
 3) Android → Backend: отправка файла на транскрипцию, получение текста. 
 4) Android: сохранение транскрипции в локальную БД (Room). 
 5) Android: экспорт/шаринг текста (TXT) + шаринг аудио файла. 
 6) Backend: `/health`, `/transcribe`, `/summarize` (summary — бесплатный, без ключей). 
 
 **В MVP НЕ входит (Post-MVP):** 
 - офлайн-транскрипция на телефоне (whisper.cpp), 
 - диаризация (разделение по спикерам), 
 - аккаунты/оплаты/облако, 
 - сложное шумоподавление, 
 - MP3 экспорт “прямо в приложении” (можно позже: либо ffmpeg-kit, либо конвертация на backend). 
 
 --- 
 
 ## 4) Архитектура (выбранная: Вариант B — Hybrid) 
 
 ### 4.1 Компоненты 
 **Android (Kotlin + Jetpack Compose):** 
 - запись аудио, 
 - локальные файлы, 
 - Room DB, 
 - HTTP клиент (Retrofit/OkHttp) для запросов на backend, 
 - UI: 3 экрана (Список / Запись / Детали). 
 
 **Backend (Python/FastAPI в Docker):** 
 - принимает аудио, 
 - ffmpeg конвертирует в WAV 16kHz mono (+ опц. нормализация), 
 - faster-whisper транскрибирует, 
 - summarizer делает extractive summary без платных API, 
 - возвращает JSON. 
 
 ### 4.2 Поток данных 
 1) Android записывает `*.m4a` → сохраняет в app storage. 
 2) Пользователь нажимает “Транскрибировать”. 
 3) Android отправляет файл на `POST /transcribe`. 
 4) Backend: проверка размера/типа → ffmpeg → whisper → JSON response. 
 5) Android сохраняет transcript в Room и показывает на экране деталей. 
 6) (опц.) Android отправляет текст на `POST /summarize` → сохраняет summary. 
 
 --- 
 
 ## 5) Техстек (MVP) 
 ### Android 
 - Kotlin 
 - Jetpack Compose 
 - Room (SQLite) 
 - Retrofit + OkHttp 
 - Coroutines + Flow 
 - MediaRecorder (запись в m4a/aac) 
 
 ### Backend 
 - Python 3.11 
 - FastAPI + Uvicorn 
 - faster-whisper (CPU) + модель по умолчанию `small` 
 - ffmpeg (в контейнере) 
 - summarizer без ключей (extractive): 
   - вариант: TF-IDF + top-k предложений (scikit-learn) **или** TextRank (легкая реализация) 
 - Docker + docker-compose 
 
 --- 
 
 ## 6) Безопасность и приватность 
 ### 6.1 Приватность 
 - Записи хранятся **локально на устройстве**. 
 - На сервер отправляется только выбранный файл. 
 - Backend по умолчанию **stateless** (не хранит аудио/тексты после ответа), кроме временных файлов обработки (удаляются). 
 
 ### 6.2 Backend: проверки (не доверяем клиенту) 
 Обязательные ограничения: 
 - `MAX_UPLOAD_MB` (например 200) 
 - разрешённые mime/расширения: `m4a`, `aac`, `mp3`, `wav`, `ogg`, `webm` 
 - ограничение длительности (опц.): `MAX_AUDIO_SECONDS` (например 2 часа) 
 - таймаут/ограничение ресурсов (на уровне Nginx в проде) 
 
 ### 6.3 Секреты 
 - любые ключи/токены (если появятся) — только в `.env` на backend. 
 - `.env` в `.gitignore`. 
 
 ### 6.4 Android 
 - В debug можно разрешить http (cleartext) для локалки. 
 - В production — только https endpoint. 
 
 --- 
 
 ## 7) API (черновик, MVP) 
 
 ### 7.1 Health 
 `GET /health` 
 Response: 
 ```json 
 { "status": "ok" } 
 ```
 
 ### 7.2 Transcribe 
 `POST /transcribe` (multipart/form-data) 
 Fields: 
 - `file`: audio file (required) 
 - `lang`: string (optional, default "ru") 
 - `return_segments`: boolean (optional, default false) 
 
 Response: 
 ```json 
 { 
   "text": "полный текст…", 
   "language": "ru", 
   "duration_sec": 123, 
   "segments": [ 
     { "start": 0.0, "end": 3.2, "text": "..." } 
   ] 
 } 
 ```
 *Примечания:* 
 В MVP можно возвращать segments только если return_segments=true. 
 
 ### 7.3 Summarize 
 `POST /summarize` (application/json) 
 Body: 
 ```json 
 { 
   "text": "длинный текст", 
   "max_sentences": 5 
 } 
 ```
 Response: 
 ```json 
 { 
   "summary": "короткая выжимка 3–7 предложений", 
   "bullets": ["пункт 1", "пункт 2", "пункт 3"] 
 } 
 ```
 
 ## 8) Модель данных (Android Room, MVP) 
 **Entity: RecordingEntity (таблица recordings)** 
 - `id`: String (UUID) PK 
 - `title`: String 
 - `filePath`: String 
 - `createdAt`: Long (epoch ms) 
 - `durationSec`: Int? 
 - `status`: String (RECORDED | TRANSCRIBING | DONE | ERROR) 
 - `language`: String (default "ru") 
 - `transcript`: String? 
 - `summary`: String? 
 - `errorMessage`: String? 
 
 ## 9) UI/UX (экраны) 
 ### 9.1 Home / список 
 - список карточек: title, дата, статус 
 - кнопка/FAB: “Запись” 
 - тап по карточке → детали 
 
 ### 9.2 Экран записи 
 - таймер 
 - кнопка Start/Stop 
 - индикатор записи 
 - после Stop → сохранить запись и вернуть на список 
 
 ### 9.3 Детали записи 
 - проигрывание (минимум кнопка “Открыть/Поделиться аудио” в MVP) 
 - кнопки: 
   - “Транскрибировать” 
   - “Сделать выжимку” (опц.) 
   - “Копировать текст” 
   - “Поделиться TXT” 
   - “Поделиться аудио” 
 - поле транскрипции (скролл) 
 - поле summary (если есть) 
 - статусы: Processing/Error 
 
 **UX принципы:** 
 - важные действия заметнее (Transcribe), 
 - всегда понятный статус, 
 - понятные ошибки (и кнопка “повторить”). 
 
 ## 10) Репозиторий / структура папок (монорепо) 
 ```text
 / 
 - documentation.md (этот файл) 
 - .gitignore 
 - docker-compose.yml 
 - backend/ 
   - Dockerfile 
   - requirements.txt 
   - .env.example 
   - app/ 
     - main.py 
     - api/ 
       - health.py 
       - transcribe.py 
       - summarize.py 
     - services/ 
       - audio_convert.py 
       - transcriber.py 
       - summarizer.py 
     - utils/ 
       - validation.py 
       - tempfiles.py 
 - android/ 
   - (Android Studio проект) 
 ```
 
 ## 11) ENV переменные (backend) 
 Файл: `backend/.env` (не коммитим), пример: `backend/.env.example` 
 ```bash
 APP_ENV=dev 
 HOST=0.0.0.0 
 PORT=8000 
 DEFAULT_LANG=ru 
 MAX_UPLOAD_MB=200 
 MAX_AUDIO_SECONDS=7200 # (опционально) 
 WHISPER_MODEL=small 
 WHISPER_DEVICE=cpu 
 WHISPER_COMPUTE_TYPE=int8 
 WHISPER_THREADS=4 # (опционально) 
 ```
 
 ## 12) Локальный запуск (точные команды) 
 ### 12.1 Backend (Docker) 
 В корне репо: 
 `docker compose up --build` 
 
 Проверка: 
 `curl http://localhost:8000/health` 
 
 ### 12.2 Android (Android Studio) 
 Открыть папку `android/` как проект в Android Studio. 
 Запустить на эмуляторе/устройстве. 
 
 **Важно про адрес backend:** 
 - Эмулятор Android → `http://10.0.2.2:8000`  (доступ к localhost на хосте). 
 - Физический телефон → `http://<LAN_IP_ПК>:8000` (backend должен слушать 0.0.0.0). 
 
 ## 13) Контейнеризация backend (Docker/Compose) 
 `docker-compose.yml` (в корне): 
 - сервис backend 
 - проброс портов 8000:8000 
 - env_file backend/.env 
 - установка ffmpeg в Dockerfile 
 
 ## 14) Деплой backend (опционально, когда MVP локально готов) 
 Цель: `https://api.yourdomain.com`  → проксирует на контейнер backend:8000. 
 
 **VPS Ubuntu (черновик команд)** 
 Установка Docker: 
 ```bash
 sudo apt update 
 sudo apt install -y ca-certificates curl gnupg 
 # дальше стандартная установка docker + compose plugin 
 ```
 Клонировать репо, создать `backend/.env` 
 
 Поднять: 
 `docker compose up -d --build` 
 
 Nginx reverse proxy → localhost:8000 
 
 SSL: 
 certbot для Nginx (Let's Encrypt) 
 
 ## 15) Чек-лист готовности MVP 
 - [ ] backend поднимается `docker compose up --build` 
 - [ ] `/health` отдаёт ok 
 - [ ] `/transcribe` принимает m4a и возвращает непустой text 
 - [ ] Android пишет аудио и сохраняет файл 
 - [ ] Android показывает историю записей 
 - [ ] Android отправляет запись на backend и получает транскрипцию 
 - [ ] Транскрипция сохраняется локально и отображается 
 - [ ] Share TXT работает 
 - [ ] Ошибки читаемые + есть retry 
 
 ## 16) План реализации (для ориентира Trae, НЕ как обязаловка “в одном махе”) 
 1. backend: skeleton + compose + health 
 2. backend: upload endpoint + validation 
 3. backend: ffmpeg convert + loudnorm (опц.) 
 4. backend: faster-whisper transcribe 
 5. backend: summarize extractive 
 6. android: проект + 3 экрана + навигация 
 7. android: запись и сохранение файла 
 8. android: Room + история 
 9. android: загрузка на backend + отображение текста 
 10. android: share TXT + обработка ошибок 
 
 **Конечный результат (MVP)** 
 Android диктофон, который: 
 - записывает, 
 - хранит историю, 
 - отправляет выбранную запись на backend, 
 - получает текст, 
 - даёт export/share текста.
