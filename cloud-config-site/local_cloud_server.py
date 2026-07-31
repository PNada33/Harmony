#!/usr/bin/env python3
import argparse
import base64
import hashlib
import hmac
import json
import mimetypes
import os
import struct
import threading
import zlib
from copy import deepcopy
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import parse_qs, urlparse


ROOT_DIR = Path(__file__).resolve().parent
DEFAULT_DATA_DIR = ROOT_DIR / "local-server-data"
DATA_DIR = DEFAULT_DATA_DIR
DB_FILE = DATA_DIR / "configs.json"
ADMIN_API_KEY = ""
CLIENT_API_KEY = ""
AUTH_REQUIRED = True
UI_BASIC_USER = ""
UI_BASIC_PASS = ""
DB_LOCK = threading.Lock()
SYSTEM_CONFIG_NAMES = {"hit-aura-runtime", "hit-aura-rotations"}
SYSTEM_CONFIG_IDS = {f"cfg:{name}" for name in SYSTEM_CONFIG_NAMES}
RUNTIME_ROUTE_TO_KEY = {
    "/backend/runtime/hit-aura": "hit-aura-runtime",
    "/backend/runtime/hit-aura-rotations": "hit-aura-rotations",
}
STATIC_ALLOWED_PATHS = {
    "/",
    "/index.html",
    "/app.js",
    "/styles.css",
    "/cloud-client.js",
    "/config.js",
}
STATIC_ALLOWED_PREFIXES = ("/public/",)
WS_ROUTE = "/ws/cloud"
WS_PROTOCOL_LABEL = b"HarmonyCloudWSv1"
ROTATIONS_OVERRIDE_FILE = ROOT_DIR / "hit-aura-rotations.payload.json"
RUNTIME_OVERRIDE_FILE = ROOT_DIR / "hit-aura-runtime.payload.json"


def utc_now_iso():
    return datetime.now(timezone.utc).isoformat()


def parse_int_or_default(value, default):
    try:
        parsed = int(str(value).strip())
        return parsed if parsed > 0 else default
    except Exception:
        return default


def parse_bool_or_default(value, default):
    if value is None:
        return default
    normalized = str(value).strip().lower()
    if normalized in {"1", "true", "yes", "y", "on"}:
        return True
    if normalized in {"0", "false", "no", "n", "off"}:
        return False
    return default


def configure_storage(data_dir):
    global DATA_DIR, DB_FILE
    path_value = (data_dir or "").strip()
    target_dir = Path(path_value).expanduser() if path_value else DEFAULT_DATA_DIR
    DATA_DIR = target_dir.resolve()
    DB_FILE = DATA_DIR / "configs.json"


def configure_auth(admin_api_key, client_api_key, require_auth, ui_basic_user, ui_basic_pass):
    global ADMIN_API_KEY, CLIENT_API_KEY, AUTH_REQUIRED, UI_BASIC_USER, UI_BASIC_PASS
    ADMIN_API_KEY = (admin_api_key or "").strip()
    CLIENT_API_KEY = (client_api_key or "").strip()
    AUTH_REQUIRED = bool(require_auth)
    UI_BASIC_USER = (ui_basic_user or "").strip()
    UI_BASIC_PASS = (ui_basic_pass or "").strip()


def ensure_db():
    DATA_DIR.mkdir(parents=True, exist_ok=True)
    if not DB_FILE.exists():
        DB_FILE.write_text(json.dumps({"configs": {}, "runtime": {}}, ensure_ascii=False, indent=2), encoding="utf-8")


def load_db():
    ensure_db()
    with DB_FILE.open("r", encoding="utf-8") as fp:
        data = json.load(fp)
    if not isinstance(data, dict):
        data = {}
    configs = data.get("configs")
    if not isinstance(configs, dict):
        data["configs"] = {}
    runtime = data.get("runtime")
    if not isinstance(runtime, dict):
        data["runtime"] = {}
    return data


def save_db(data):
    ensure_db()
    temp_file = DB_FILE.with_suffix(".tmp")
    temp_file.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
    temp_file.replace(DB_FILE)


def canonical_id(record_id, name):
    if isinstance(record_id, str) and record_id.strip():
        rid = record_id.strip()
        if rid.lower().startswith("cfg:"):
            return rid
    safe_name = (name or "unnamed").strip().lower()
    if not safe_name:
        safe_name = "unnamed"
    return f"cfg:{safe_name}"


def normalize_name(name, record_id):
    if isinstance(name, str) and name.strip():
        return name.strip()
    if isinstance(record_id, str) and record_id.lower().startswith("cfg:"):
        suffix = record_id[4:].strip()
        if suffix:
            return suffix
    return "unnamed"


def ensure_object(value):
    return value if isinstance(value, dict) else {}


def ensure_list(value):
    return value if isinstance(value, list) else []


def is_system_config_name(name):
    if not isinstance(name, str):
        return False
    return name.strip().lower() in SYSTEM_CONFIG_NAMES


def is_system_config_id(record_id):
    if not isinstance(record_id, str):
        return False
    return record_id.strip().lower() in SYSTEM_CONFIG_IDS


def is_system_record(record):
    if not isinstance(record, dict):
        return False
    if is_system_config_name(record.get("name")):
        return True
    return is_system_config_id(record.get("id"))


def pick_payload(raw):
    if not isinstance(raw, dict):
        return {}
    if isinstance(raw.get("payload"), dict):
        return raw["payload"]
    if isinstance(raw.get("config"), dict):
        return raw["config"]
    if isinstance(raw.get("data"), dict):
        data = raw["data"]
        if isinstance(data.get("payload"), dict):
            return data["payload"]
        if isinstance(data.get("config"), dict):
            return data["config"]
        return data
    return raw


def pick_fields(record, select_param):
    if not select_param:
        return deepcopy(record)
    columns = [part.strip() for part in select_param.split(",") if part.strip()]
    if not columns or columns == ["*"]:
        return deepcopy(record)
    return {key: deepcopy(record[key]) for key in columns if key in record}


def extract_eq(query_values):
    if not query_values:
        return None
    raw = query_values[0]
    if isinstance(raw, str) and raw.startswith("eq."):
        return raw[3:]
    return None


def clone_json(value):
    return json.loads(json.dumps(value, ensure_ascii=False))


def try_load_json_file(path):
    target = Path(path)
    if not target.is_file():
        return None
    try:
        with target.open("r", encoding="utf-8") as fp:
            loaded = json.load(fp)
        if isinstance(loaded, dict):
            if isinstance(loaded.get("payload"), dict):
                return clone_json(loaded["payload"])
            return clone_json(loaded)
    except Exception:
        return None
    return None


def hmac_sha256(key_bytes, data_bytes):
    return hmac.new(key_bytes, data_bytes, hashlib.sha256).digest()


def derive_session_key(shared_secret, client_nonce, server_nonce):
    secret_bytes = (shared_secret or "").encode("utf-8")
    return hmac_sha256(secret_bytes, WS_PROTOCOL_LABEL + client_nonce + server_nonce)


def build_proof(session_key, action, client_nonce_b64, server_nonce_b64):
    value = f"{action}|{client_nonce_b64}|{server_nonce_b64}".encode("utf-8")
    return base64.b64encode(hmac_sha256(session_key, value)).decode("ascii")


def derive_stream_key(session_key, label):
    return hmac_sha256(session_key, label)


def xor_keystream(enc_key, iv, payload):
    output = bytearray(payload)
    counter = 0
    offset = 0
    while offset < len(output):
        block = hmac_sha256(enc_key, iv + counter.to_bytes(4, "big", signed=False))
        for byte in block:
            if offset >= len(output):
                break
            output[offset] ^= byte
            offset += 1
        counter += 1
    return bytes(output)


def encrypt_payload(session_key, payload_obj):
    plain = json.dumps(payload_obj, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
    compressed = zlib.compress(plain, 9)
    iv = os.urandom(16)
    enc_key = derive_stream_key(session_key, b"enc")
    mac_key = derive_stream_key(session_key, b"mac")
    cipher = xor_keystream(enc_key, iv, compressed)
    mac = hmac_sha256(mac_key, iv + cipher)
    return {
        "iv": base64.b64encode(iv).decode("ascii"),
        "ciphertext": base64.b64encode(cipher).decode("ascii"),
        "mac": base64.b64encode(mac).decode("ascii"),
        "compression": "deflate",
    }


def decrypt_payload(session_key, envelope):
    if not isinstance(envelope, dict):
        return None

    try:
        iv = base64.b64decode(str(envelope.get("iv", "")).encode("ascii"))
        cipher = base64.b64decode(str(envelope.get("ciphertext", "")).encode("ascii"))
        received_mac = base64.b64decode(str(envelope.get("mac", "")).encode("ascii"))
    except Exception:
        return None

    enc_key = derive_stream_key(session_key, b"enc")
    mac_key = derive_stream_key(session_key, b"mac")
    expected_mac = hmac_sha256(mac_key, iv + cipher)
    if not hmac.compare_digest(received_mac, expected_mac):
        return None

    try:
        compressed = xor_keystream(enc_key, iv, cipher)
        plain = zlib.decompress(compressed)
        loaded = json.loads(plain.decode("utf-8"))
        return loaded if isinstance(loaded, dict) else None
    except Exception:
        return None


def websocket_accept_value(sec_websocket_key):
    magic = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"
    digest = hashlib.sha1((sec_websocket_key + magic).encode("ascii")).digest()
    return base64.b64encode(digest).decode("ascii")


def read_exact(sock_file, size):
    data = sock_file.read(size)
    if data is None or len(data) != size:
        raise EOFError("unexpected websocket eof")
    return data


def ws_read_frame(sock_file):
    first = read_exact(sock_file, 1)[0]
    second = read_exact(sock_file, 1)[0]
    opcode = first & 0x0F
    masked = (second & 0x80) != 0
    payload_len = second & 0x7F

    if payload_len == 126:
        payload_len = struct.unpack(">H", read_exact(sock_file, 2))[0]
    elif payload_len == 127:
        payload_len = struct.unpack(">Q", read_exact(sock_file, 8))[0]

    mask = read_exact(sock_file, 4) if masked else b""
    payload = read_exact(sock_file, payload_len) if payload_len else b""

    if masked:
        payload = bytes(payload[i] ^ mask[i % 4] for i in range(len(payload)))
    return opcode, payload


def ws_send_frame(sock, opcode, payload):
    payload = payload or b""
    header = bytearray()
    header.append(0x80 | (opcode & 0x0F))
    payload_len = len(payload)
    if payload_len <= 125:
        header.append(payload_len)
    elif payload_len <= 0xFFFF:
        header.append(126)
        header.extend(struct.pack(">H", payload_len))
    else:
        header.append(127)
        header.extend(struct.pack(">Q", payload_len))
    sock.sendall(bytes(header) + payload)


def ws_send_json(sock, payload_obj):
    ws_send_frame(sock, 0x1, json.dumps(payload_obj, ensure_ascii=False, separators=(",", ":")).encode("utf-8"))


def ws_close(sock):
    try:
        ws_send_frame(sock, 0x8, b"")
    except Exception:
        pass


class LocalCloudHandler(BaseHTTPRequestHandler):
    server_version = "HarmonyLocalCloud/2.0"

    def log_message(self, fmt, *args):
        return

    def _set_headers(self, status_code=200, content_type="application/json", extra_headers=None):
        self.send_response(status_code)
        self.send_header("Content-Type", content_type)
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Access-Control-Allow-Headers", "Content-Type, Authorization, apikey, Prefer")
        self.send_header("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS")
        if isinstance(extra_headers, dict):
            for key, value in extra_headers.items():
                self.send_header(str(key), str(value))
        self.end_headers()

    def _write_json(self, payload, status_code=200):
        self._set_headers(status_code=status_code)
        self.wfile.write(json.dumps(payload, ensure_ascii=False).encode("utf-8"))

    def _read_json_body(self):
        length = int(self.headers.get("Content-Length", "0") or "0")
        if length <= 0:
            return None
        body = self.rfile.read(length).decode("utf-8")
        if not body.strip():
            return None
        return json.loads(body)

    def _extract_bearer_token(self, header_value):
        if not isinstance(header_value, str):
            return ""
        raw = header_value.strip()
        if not raw.lower().startswith("bearer "):
            return ""
        return raw[7:].strip()

    def _request_api_key(self):
        header_api_key = str(self.headers.get("apikey", "") or "").strip()
        if header_api_key:
            return header_api_key
        return self._extract_bearer_token(self.headers.get("Authorization"))

    def _matches_api_key(self, expected_key):
        if not expected_key:
            return False
        provided_key = self._request_api_key()
        if not provided_key:
            return False
        return hmac.compare_digest(provided_key, expected_key)

    def _is_admin_authorized(self):
        if not AUTH_REQUIRED:
            return True
        if not ADMIN_API_KEY:
            return False
        return self._matches_api_key(ADMIN_API_KEY)

    def _is_client_authorized(self):
        if not AUTH_REQUIRED:
            return True
        if self._matches_api_key(CLIENT_API_KEY):
            return True
        return self._is_admin_authorized()

    def _require_admin_auth(self):
        if self._is_admin_authorized():
            return True
        self._write_json({"error": "unauthorized"}, status_code=401)
        return False

    def _require_client_auth(self):
        if self._is_client_authorized():
            return True
        self._write_json({"error": "unauthorized"}, status_code=401)
        return False

    def _is_static_path_allowed(self, request_path):
        if request_path in STATIC_ALLOWED_PATHS:
            return True
        return any(request_path.startswith(prefix) for prefix in STATIC_ALLOWED_PREFIXES)

    def _has_valid_ui_basic_auth(self):
        if not UI_BASIC_USER or not UI_BASIC_PASS:
            return True

        header_value = str(self.headers.get("Authorization", "") or "").strip()
        if not header_value.lower().startswith("basic "):
            return False

        try:
            decoded = base64.b64decode(header_value[6:].strip()).decode("utf-8")
        except Exception:
            return False

        if ":" not in decoded:
            return False

        username, password = decoded.split(":", 1)
        return hmac.compare_digest(username, UI_BASIC_USER) and hmac.compare_digest(password, UI_BASIC_PASS)

    def _require_ui_basic_auth(self):
        if self._has_valid_ui_basic_auth():
            return True
        self._set_headers(
            status_code=401,
            content_type="text/plain",
            extra_headers={"WWW-Authenticate": 'Basic realm="Harmony Cloud Admin"'},
        )
        self.wfile.write(b"Authentication required")
        return False

    def _serve_static(self, request_path):
        path = request_path.strip() or "/"
        if path == "/":
            path = "/index.html"

        if not self._is_static_path_allowed(path):
            return False
        if not self._require_ui_basic_auth():
            return True

        relative = path.lstrip("/")
        if not relative:
            return False

        parts = Path(relative).parts
        if any(part.startswith(".") for part in parts):
            self._write_json({"error": "not found"}, status_code=404)
            return True

        file_path = (ROOT_DIR / relative).resolve()
        root_path = ROOT_DIR.resolve()
        if root_path not in file_path.parents and file_path != root_path:
            self._write_json({"error": "not found"}, status_code=404)
            return True
        if not file_path.exists() or not file_path.is_file():
            return False

        content_type, _ = mimetypes.guess_type(str(file_path))
        if not content_type:
            content_type = "application/octet-stream"

        payload = file_path.read_bytes()
        self._set_headers(
            status_code=200,
            content_type=content_type,
            extra_headers={
                "Cache-Control": "no-store, max-age=0",
                "Pragma": "no-cache",
                "Expires": "0",
            },
        )
        self.wfile.write(payload)
        return True

    def _read_runtime_payload(self, runtime_key):
        override_file = ROTATIONS_OVERRIDE_FILE if runtime_key == "hit-aura-rotations" else RUNTIME_OVERRIDE_FILE
        override_payload = try_load_json_file(override_file)
        if override_payload is not None:
            return override_payload

        with DB_LOCK:
            data = load_db()
            runtime_map = ensure_object(data.get("runtime"))
            payload = runtime_map.get(runtime_key)
            if isinstance(payload, dict):
                return deepcopy(payload)

            legacy_id = canonical_id(runtime_key, runtime_key)
            configs = ensure_object(data.get("configs"))
            legacy_row = ensure_object(configs.get(legacy_id))
            legacy_payload = legacy_row.get("payload")
            if isinstance(legacy_payload, dict):
                runtime_map[runtime_key] = ensure_object(legacy_payload)
                if legacy_id in configs:
                    del configs[legacy_id]
                save_db(data)
                return deepcopy(legacy_payload)
            return {}

    def _write_runtime_payload(self, runtime_key, payload):
        with DB_LOCK:
            data = load_db()
            runtime_map = data.setdefault("runtime", {})
            runtime_map[runtime_key] = ensure_object(payload)
            save_db(data)

    def _ws_shared_secret(self):
        if CLIENT_API_KEY:
            return CLIENT_API_KEY
        if ADMIN_API_KEY:
            return ADMIN_API_KEY
        return ""

    def _is_websocket_upgrade(self):
        upgrade = str(self.headers.get("Upgrade", "") or "").strip().lower()
        connection = str(self.headers.get("Connection", "") or "").strip().lower()
        return upgrade == "websocket" and "upgrade" in connection

    def _handle_websocket_upgrade(self):
        shared_secret = self._ws_shared_secret()
        if AUTH_REQUIRED and not shared_secret:
            self.send_error(503, "WebSocket secret is not configured")
            return True

        sec_websocket_key = str(self.headers.get("Sec-WebSocket-Key", "") or "").strip()
        if not sec_websocket_key:
            self.send_error(400, "Missing Sec-WebSocket-Key")
            return True

        accept_value = websocket_accept_value(sec_websocket_key)
        self.send_response(101, "Switching Protocols")
        self.send_header("Upgrade", "websocket")
        self.send_header("Connection", "Upgrade")
        self.send_header("Sec-WebSocket-Accept", accept_value)
        self.end_headers()

        try:
            self._serve_websocket_session(shared_secret)
        except Exception:
            ws_close(self.connection)
        return True

    def _serve_websocket_session(self, shared_secret):
        hello_opcode, hello_payload = ws_read_frame(self.rfile)
        if hello_opcode == 0x8:
            return
        if hello_opcode != 0x1:
            ws_close(self.connection)
            return

        try:
            hello = json.loads(hello_payload.decode("utf-8"))
        except Exception:
            ws_send_json(self.connection, {"op": "response", "payload": encrypt_payload(shared_secret.encode("utf-8"), {"ok": False, "error": "invalid hello"})})
            ws_close(self.connection)
            return

        action = str(hello.get("action", "")).strip()
        client_nonce_b64 = str(hello.get("clientNonce", "")).strip()
        if str(hello.get("op", "")).strip() != "hello" or not action or not client_nonce_b64:
            ws_close(self.connection)
            return

        try:
            client_nonce = base64.b64decode(client_nonce_b64.encode("ascii"))
        except Exception:
            ws_close(self.connection)
            return

        server_nonce = os.urandom(16)
        server_nonce_b64 = base64.b64encode(server_nonce).decode("ascii")
        ws_send_json(self.connection, {"op": "challenge", "serverNonce": server_nonce_b64})

        request_opcode, request_payload = ws_read_frame(self.rfile)
        if request_opcode == 0x8:
            return
        if request_opcode != 0x1:
            ws_close(self.connection)
            return

        try:
            request = json.loads(request_payload.decode("utf-8"))
        except Exception:
            ws_close(self.connection)
            return

        session_key = derive_session_key(shared_secret, client_nonce, server_nonce)
        expected_proof = build_proof(session_key, action, client_nonce_b64, server_nonce_b64)
        received_proof = str(request.get("proof", "")).strip()
        if str(request.get("op", "")).strip() != "request" or not hmac.compare_digest(received_proof, expected_proof):
            ws_send_json(self.connection, {"op": "response", "payload": encrypt_payload(session_key, {"ok": False, "error": "unauthorized"})})
            ws_close(self.connection)
            return

        payload = decrypt_payload(session_key, ensure_object(request.get("payload")))
        if payload is None:
            ws_send_json(self.connection, {"op": "response", "payload": encrypt_payload(session_key, {"ok": False, "error": "invalid payload"})})
            ws_close(self.connection)
            return

        response = self._dispatch_websocket_action(action, payload)
        ws_send_json(self.connection, {"op": "response", "payload": encrypt_payload(session_key, response)})
        ws_close(self.connection)

    def _dispatch_websocket_action(self, action, payload):
        normalized_action = (action or "").strip().lower()
        if normalized_action == "bootstrap":
            return self._build_bootstrap_response()
        if normalized_action == "save_config":
            return self._save_config_via_websocket(payload)
        if normalized_action == "delete_config":
            return self._delete_config_via_websocket(payload)
        return {"ok": False, "error": "unknown action"}

    def _build_bootstrap_response(self):
        with DB_LOCK:
            data = load_db()
            configs = ensure_object(data.get("configs"))
            records = []
            for row in configs.values():
                if is_system_record(row):
                    continue
                records.append(
                    {
                        "id": str(row.get("id", "")),
                        "name": str(row.get("name", "")),
                        "payload": ensure_object(row.get("payload")),
                        "updated_at": str(row.get("updated_at", "")),
                    }
                )

        records.sort(key=lambda item: (item.get("updated_at", ""), item.get("name", "")), reverse=True)
        return {
            "ok": True,
            "configs": records,
            "rotations": self._read_runtime_payload("hit-aura-rotations"),
            "runtime": self._read_runtime_payload("hit-aura-runtime"),
        }

    def _save_config_via_websocket(self, payload):
        raw_name = str(payload.get("name", "")).strip()
        row_id = canonical_id(payload.get("id"), raw_name)
        row_name = normalize_name(raw_name, row_id)
        if is_system_config_id(row_id) or is_system_config_name(row_name):
            return {"ok": False, "error": "system configs are backend-only"}

        now = utc_now_iso()
        row_payload = ensure_object(payload.get("payload"))
        with DB_LOCK:
            data = load_db()
            configs = data.setdefault("configs", {})
            existing = ensure_object(configs.get(row_id))
            normalized = {
                "id": row_id,
                "name": row_name,
                "payload": row_payload,
                "created_at": str(existing.get("created_at", now)),
                "updated_at": now,
            }
            configs[row_id] = normalized
            save_db(data)
        return {"ok": True, "id": row_id, "name": row_name}

    def _delete_config_via_websocket(self, payload):
        raw_name = str(payload.get("name", "")).strip()
        row_id = canonical_id(payload.get("id"), raw_name)
        deleted = 0

        with DB_LOCK:
            data = load_db()
            configs = data.setdefault("configs", {})
            keys_to_remove = []
            for key, row in configs.items():
                if is_system_record(row):
                    continue
                current_id = str(row.get("id", ""))
                current_name = str(row.get("name", ""))
                if current_id == row_id or (raw_name and current_name.lower() == raw_name.lower()):
                    keys_to_remove.append(key)

            for key in keys_to_remove:
                if key in configs:
                    del configs[key]
                    deleted += 1

            save_db(data)

        return {"ok": deleted > 0, "deleted": deleted, "error": "" if deleted > 0 else "config not found"}

    def do_OPTIONS(self):
        self._set_headers(status_code=204, content_type="text/plain")

    def do_GET(self):
        parsed = urlparse(self.path)
        if parsed.path == WS_ROUTE and self._is_websocket_upgrade():
            self._handle_websocket_upgrade()
            return

        if parsed.path == "/health":
            self._write_json({"ok": True, "service": "local-cloud"})
            return

        runtime_key = RUNTIME_ROUTE_TO_KEY.get(parsed.path)
        if runtime_key is not None:
            if not self._require_admin_auth():
                return
            payload = self._read_runtime_payload(runtime_key)
            self._write_json(payload, status_code=200)
            return

        if parsed.path != "/rest/v1/configs":
            if self._serve_static(parsed.path):
                return
            self._write_json({"error": "not found"}, status_code=404)
            return
        if not self._require_admin_auth():
            return

        query = parse_qs(parsed.query)
        id_eq = extract_eq(query.get("id"))
        name_eq = extract_eq(query.get("name"))
        limit = None
        if "limit" in query:
            try:
                limit = max(0, int(query["limit"][0]))
            except Exception:
                limit = None
        order = query.get("order", [None])[0]
        select_param = query.get("select", [None])[0]

        with DB_LOCK:
            data = load_db()
            records = list(data.get("configs", {}).values())

        if id_eq is not None:
            records = [row for row in records if str(row.get("id", "")) == id_eq]
        if name_eq is not None:
            records = [row for row in records if str(row.get("name", "")) == name_eq]
        records = [row for row in records if not is_system_record(row)]

        if isinstance(order, str) and "." in order:
            field, direction = order.split(".", 1)
            reverse = direction.lower() == "desc"
            records.sort(key=lambda x: str(x.get(field, "")), reverse=reverse)

        if limit is not None:
            records = records[:limit]

        output = [pick_fields(item, select_param) for item in records]
        self._write_json(output)

    def do_POST(self):
        parsed = urlparse(self.path)
        runtime_key = RUNTIME_ROUTE_TO_KEY.get(parsed.path)
        if runtime_key is not None:
            if not self._require_admin_auth():
                return
            try:
                payload = self._read_json_body()
            except Exception as exc:
                self._write_json({"error": f"invalid json: {exc}"}, status_code=400)
                return

            if not isinstance(payload, dict):
                self._write_json({"error": "body must be JSON object"}, status_code=400)
                return

            self._write_runtime_payload(runtime_key, payload)
            self._write_json({"ok": True, "key": runtime_key}, status_code=200)
            return

        if parsed.path != "/rest/v1/configs":
            self._write_json({"error": "not found"}, status_code=404)
            return
        if not self._require_admin_auth():
            return

        try:
            payload = self._read_json_body()
        except Exception as exc:
            self._write_json({"error": f"invalid json: {exc}"}, status_code=400)
            return

        if isinstance(payload, dict):
            rows = [payload]
        elif isinstance(payload, list):
            rows = payload
        else:
            self._write_json({"error": "body must be JSON object or array"}, status_code=400)
            return

        for raw in rows:
            if not isinstance(raw, dict):
                continue
            candidate_id = canonical_id(raw.get("id"), raw.get("name"))
            candidate_name = normalize_name(raw.get("name"), candidate_id)
            if is_system_config_id(candidate_id) or is_system_config_name(candidate_name):
                self._write_json({"error": "system configs are backend-only"}, status_code=403)
                return

        updated_rows = []
        now = utc_now_iso()
        with DB_LOCK:
            data = load_db()
            configs = data.setdefault("configs", {})

            for raw in rows:
                if not isinstance(raw, dict):
                    continue
                row_id = canonical_id(raw.get("id"), raw.get("name"))
                existing = ensure_object(configs.get(row_id))
                created_at = str(existing.get("created_at", now))
                normalized = {
                    "id": row_id,
                    "name": normalize_name(raw.get("name"), row_id),
                    "payload": ensure_object(pick_payload(raw)),
                    "created_at": created_at,
                    "updated_at": str(raw.get("updated_at", now)),
                }
                configs[row_id] = normalized
                updated_rows.append(deepcopy(normalized))

            save_db(data)

        prefer = self.headers.get("Prefer", "")
        if "return=minimal" in prefer:
            self._write_json([], status_code=200)
            return

        self._write_json(updated_rows, status_code=200)

    def do_DELETE(self):
        parsed = urlparse(self.path)
        runtime_key = RUNTIME_ROUTE_TO_KEY.get(parsed.path)
        if runtime_key is not None:
            if not self._require_admin_auth():
                return
            deleted = 0
            with DB_LOCK:
                data = load_db()
                runtime_map = data.setdefault("runtime", {})
                if runtime_key in runtime_map:
                    del runtime_map[runtime_key]
                    deleted = 1
                save_db(data)
            self._write_json({"deleted": deleted}, status_code=200)
            return

        if parsed.path != "/rest/v1/configs":
            self._write_json({"error": "not found"}, status_code=404)
            return
        if not self._require_admin_auth():
            return

        query = parse_qs(parsed.query)
        id_eq = extract_eq(query.get("id"))
        name_eq = extract_eq(query.get("name"))

        deleted = 0
        with DB_LOCK:
            data = load_db()
            configs = data.setdefault("configs", {})
            keys_to_remove = []

            for key, row in configs.items():
                if is_system_record(row):
                    continue
                row_id = str(row.get("id", ""))
                row_name = str(row.get("name", ""))
                if id_eq is not None and row_id == id_eq:
                    keys_to_remove.append(key)
                    continue
                if name_eq is not None and row_name == name_eq:
                    keys_to_remove.append(key)

            for key in keys_to_remove:
                if key in configs:
                    del configs[key]
                    deleted += 1

            save_db(data)

        self._write_json({"deleted": deleted}, status_code=200)


def main():
    parser = argparse.ArgumentParser(description="Harmony local cloud server")
    parser.add_argument("--host", default=os.getenv("HARMONY_CLOUD_HOST", "127.0.0.1"))
    parser.add_argument(
        "--port",
        type=int,
        default=parse_int_or_default(os.getenv("HARMONY_CLOUD_PORT", "54321"), 54321),
    )
    parser.add_argument(
        "--data-dir",
        default=os.getenv("HARMONY_CLOUD_DATA_DIR", ""),
        help="Path to directory where configs.json will be stored",
    )
    parser.add_argument(
        "--api-key",
        default=os.getenv("HARMONY_CLOUD_API_KEY", ""),
        help="Admin API key for config CRUD and runtime publishing",
    )
    parser.add_argument(
        "--client-api-key",
        default=os.getenv("HARMONY_CLOUD_CLIENT_API_KEY", ""),
        help="Shared client secret used by the one-shot WebSocket bootstrap",
    )
    parser.add_argument(
        "--require-auth",
        default=os.getenv("HARMONY_CLOUD_REQUIRE_AUTH", "1"),
        help="Require secrets for REST/backend endpoints (1/0, true/false)",
    )
    parser.add_argument(
        "--ui-basic-user",
        default=os.getenv("HARMONY_CLOUD_UI_BASIC_USER", ""),
        help="Optional HTTP Basic auth username for admin UI/static assets",
    )
    parser.add_argument(
        "--ui-basic-pass",
        default=os.getenv("HARMONY_CLOUD_UI_BASIC_PASS", ""),
        help="Optional HTTP Basic auth password for admin UI/static assets",
    )
    args = parser.parse_args()

    configure_storage(args.data_dir)
    require_auth = parse_bool_or_default(args.require_auth, True)
    configure_auth(args.api_key, args.client_api_key, require_auth, args.ui_basic_user, args.ui_basic_pass)
    if AUTH_REQUIRED and not ADMIN_API_KEY and not CLIENT_API_KEY:
        raise SystemExit("HARMONY_CLOUD_API_KEY or HARMONY_CLOUD_CLIENT_API_KEY is required when auth is enabled")

    ensure_db()
    server = ThreadingHTTPServer((args.host, args.port), LocalCloudHandler)
    print(f"[local-cloud] running at http://{args.host}:{args.port}")
    print(f"[local-cloud] ui: http://{args.host}:{args.port}/")
    print(f"[local-cloud] api: http://{args.host}:{args.port}/rest/v1/configs")
    print(f"[local-cloud] ws : ws://{args.host}:{args.port}{WS_ROUTE}")
    print(f"[local-cloud] db file: {DB_FILE}")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()


if __name__ == "__main__":
    main()
