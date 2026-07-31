#!/bin/bash
# Script for starting the cloud server on a remote Linux host.
# Usage: ./run-remote-cloud.sh

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

if [ -f .env ]; then
  set -a
  . ./.env
  set +a
fi

PYTHON_BIN="${PYTHON_BIN:-python3}"
HARMONY_CLOUD_HOST="${HARMONY_CLOUD_HOST:-0.0.0.0}"
HARMONY_CLOUD_PORT="${HARMONY_CLOUD_PORT:-54321}"

exec "$PYTHON_BIN" local_cloud_server.py --host "$HARMONY_CLOUD_HOST" --port "$HARMONY_CLOUD_PORT"
