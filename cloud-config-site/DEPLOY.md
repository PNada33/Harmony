# Deploy на Linux сервер

Ниже универсальный деплой без привязки к конкретному IP/паролю.

## 1. Подготовка сервера

```bash
sudo apt update
sudo apt install -y python3
```

## 2. Создание директории проекта

```bash
sudo mkdir -p /opt/harmony-cloud
sudo chown "$USER":"$USER" /opt/harmony-cloud
cd /opt/harmony-cloud
```

## 3. Копирование файлов на сервер

С локальной машины (пример):

```bash
scp -r ./cloud-config-site/* user@YOUR_SERVER_IP:/opt/harmony-cloud/
```

Или любым удобным способом (git/rsync/sftp).

## 4. Настройка окружения

На сервере:

```bash
cd /opt/harmony-cloud
cp .env.example .env
chmod +x run-local-cloud.sh run-remote-cloud.sh
```

Отредактируй `.env`:

```env
HARMONY_CLOUD_HOST=0.0.0.0
HARMONY_CLOUD_PORT=54321
HARMONY_CLOUD_REQUIRE_AUTH=1
HARMONY_CLOUD_API_KEY=PUT_LONG_RANDOM_SECRET_HERE
HARMONY_CLOUD_DATA_DIR=/opt/harmony-cloud/local-server-data
PYTHON_BIN=python3
```

## 5. Ручной запуск (проверка)

```bash
cd /opt/harmony-cloud
./run-remote-cloud.sh
```

Проверка:

```bash
curl http://127.0.0.1:54321/health
```

Ожидаемый ответ:

```json
{"ok": true, "service": "local-cloud"}
```

## 6. Запуск через systemd

### 6.1 Создай пользователя сервиса (рекомендуется)

```bash
sudo useradd --system --no-create-home --shell /usr/sbin/nologin harmony || true
sudo chown -R harmony:harmony /opt/harmony-cloud
```

### 6.2 Установи unit-файл

```bash
sudo cp /opt/harmony-cloud/harmony-cloud.service.example /etc/systemd/system/harmony-cloud.service
```

Если нужно, поправь `User`, `Group`, `WorkingDirectory` и `EnvironmentFile` в `/etc/systemd/system/harmony-cloud.service`.

### 6.3 Включи и запусти сервис

```bash
sudo systemctl daemon-reload
sudo systemctl enable harmony-cloud
sudo systemctl restart harmony-cloud
sudo systemctl status harmony-cloud
```

Логи:

```bash
sudo journalctl -u harmony-cloud -f
```

## 7. Открой порт в firewall (если требуется)

```bash
sudo ufw allow 54321/tcp
```

## 8. Подключение клиента

В `cloud-config.json` клиента укажи:

```json
{
  "projectUrl": "http://YOUR_SERVER_IP:54321",
  "anonKey": "YOUR_STRONG_API_KEY",
  "table": "configs",
  "timeoutMs": 4000
}
```

## 9. Публикация runtime

```bash
python3 publish_hit_aura_runtime.py \
  --server http://YOUR_SERVER_IP:54321 \
  --anon-key YOUR_STRONG_API_KEY \
  --jar /path/to/runtime-provider.jar \
  --entry-class your.package.YourRotationProvider \
  --rotations-json ./hit-aura-rotations.payload.json
```
