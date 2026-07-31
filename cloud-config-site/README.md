# Harmony Cloud Config Site

Минималистичный cloud-config сайт + backend для конфигов и HitAura runtime.

## Что есть в проекте

- `local_cloud_server.py` - REST backend + раздача статических файлов.
- `index.html`, `app.js`, `cloud-client.js`, `styles.css` - UI.
- `run-local-cloud.sh` - быстрый запуск на Linux локально.
- `run-remote-cloud.sh` - запуск на Linux сервере (production-friendly).
- `harmony-cloud.service.example` - шаблон systemd unit.
- `.env.example` - переменные окружения для Linux запуска.
- `publish_hit_aura_runtime.py` - публикация runtime/rotations в backend.

## Быстрый старт на Linux

1. Перейди в папку проекта:

```bash
cd /opt/harmony-cloud
```

2. Дай права на выполнение скриптов:

```bash
chmod +x run-local-cloud.sh run-remote-cloud.sh
```

3. Запусти локально:

```bash
./run-local-cloud.sh
```

4. Открой:

- UI: `http://127.0.0.1:54321/`
- Health: `http://127.0.0.1:54321/health`

## Запуск на удаленном Linux сервере

```bash
./run-remote-cloud.sh
```

По умолчанию сервер слушает `0.0.0.0:54321`.
Если рядом со скриптом есть `.env`, переменные подхватываются автоматически.

Переменные окружения:

- `HARMONY_CLOUD_HOST` (default `0.0.0.0` для remote, `127.0.0.1` для local)
- `HARMONY_CLOUD_PORT` (default `54321`)
- `HARMONY_CLOUD_REQUIRE_AUTH` (default `1`, включает проверку API ключа)
- `HARMONY_CLOUD_API_KEY` (обязательный секретный ключ при включенной auth)
- `HARMONY_CLOUD_DATA_DIR` (default `./local-server-data`)
- `PYTHON_BIN` (default `python3`)

## Конфиг чита (`cloud-config.json`)

Файл:
`<gameDir>/harmony/files/other/cloud-config.json`

Пример:

```json
{
  "projectUrl": "http://YOUR_SERVER_IP:54321",
  "anonKey": "YOUR_STRONG_API_KEY",
  "table": "configs",
  "timeoutMs": 4000
}
```

## API

- `GET/POST/DELETE /rest/v1/configs`
- `GET /health`
- `GET/POST/DELETE /backend/runtime/hit-aura`
- `GET/POST/DELETE /backend/runtime/hit-aura-rotations`

`hit-aura-runtime` и `hit-aura-rotations` скрыты из общего списка `/rest/v1/configs`.

## Публикация HitAura runtime

```bash
python3 publish_hit_aura_runtime.py \
  --server http://YOUR_SERVER_IP:54321 \
  --anon-key YOUR_STRONG_API_KEY \
  --jar /path/to/runtime-provider.jar \
  --entry-class your.package.YourRotationProvider \
  --rotations-json ./hit-aura-rotations.payload.json
```

## systemd (Linux)

Подробная инструкция: `DEPLOY.md`.
