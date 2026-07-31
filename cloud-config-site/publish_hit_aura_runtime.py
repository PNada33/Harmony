#!/usr/bin/env python3
import argparse
import base64
import hashlib
import io
import json
from pathlib import Path
from urllib import request
import zipfile


def post_json(url, api_key, payload):
    body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
    req = request.Request(url, data=body, method="POST")
    req.add_header("Content-Type", "application/json")
    req.add_header("apikey", api_key)
    req.add_header("Authorization", f"Bearer {api_key}")
    with request.urlopen(req, timeout=10) as resp:
        return resp.read().decode("utf-8")


def normalize_jar_entry_names(jar_bytes):
    source = io.BytesIO(jar_bytes)
    output = io.BytesIO()
    with zipfile.ZipFile(source, "r") as src, zipfile.ZipFile(output, "w", zipfile.ZIP_DEFLATED) as dst:
        for info in src.infolist():
            data = src.read(info.filename)
            normalized = info.filename.replace("\\", "/")
            new_info = zipfile.ZipInfo(normalized, info.date_time)
            new_info.external_attr = info.external_attr
            new_info.comment = info.comment
            dst.writestr(new_info, data)
    return output.getvalue()


def main():
    parser = argparse.ArgumentParser(description="Publish HitAura runtime to backend endpoint")
    parser.add_argument("--server", required=True, help="Example: http://localhost:54321")
    parser.add_argument("--anon-key", required=True, help="Cloud anon key")
    parser.add_argument("--jar", required=True, help="Path to runtime jar")
    parser.add_argument("--entry-class", required=True, help="Provider class implementing HitAuraRotationProvider")
    parser.add_argument("--rotations-json", help="Optional JSON file for /backend/runtime/hit-aura-rotations")
    args = parser.parse_args()

    jar_path = Path(args.jar).resolve()
    jar_bytes = normalize_jar_entry_names(jar_path.read_bytes())
    jar_b64 = base64.b64encode(jar_bytes).decode("ascii")
    sha256 = hashlib.sha256(jar_bytes).hexdigest()

    runtime_payload = {
        "runtime": {
            "entryClass": args.entry_class,
            "jarBase64": jar_b64,
            "sha256": sha256,
        }
    }

    base = args.server.rstrip("/")
    runtime_url = f"{base}/backend/runtime/hit-aura"
    print(f"Publishing runtime -> {runtime_url}")
    print(post_json(runtime_url, args.anon_key, runtime_payload))

    if args.rotations_json:
        rotations_payload = json.loads(Path(args.rotations_json).read_text(encoding="utf-8"))
        rotations_url = f"{base}/backend/runtime/hit-aura-rotations"
        print(f"Publishing rotations -> {rotations_url}")
        print(post_json(rotations_url, args.anon_key, rotations_payload))

    print("Done")


if __name__ == "__main__":
    main()
