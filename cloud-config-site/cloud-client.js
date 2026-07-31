(function () {
  "use strict";

  const cfg = window.CLOUD_CONFIG || {};
  const CRYPTO_KEY = "HarmonyCloudMaskV1";
  const PAYLOAD_KEY = "enc_v1";
  const SHARE_PREFIX = "HCFG1:";

  function nowIso() {
    return new Date().toISOString();
  }

  function isPlainObject(value) {
    return value && typeof value === "object" && !Array.isArray(value);
  }

  function normalizeName(name) {
    const value = String(name || "").trim();
    return value || "untitled";
  }

  function buildIdFromName(name) {
    const normalized = normalizeName(name).toLowerCase();
    return `cfg:${normalized}`;
  }

  function canonicalId(id, name) {
    if (typeof id === "string" && id.trim().toLowerCase().startsWith("cfg:")) {
      return id.trim();
    }
    return buildIdFromName(name);
  }

  function xorBytes(inputBytes, keyBytes) {
    const out = new Uint8Array(inputBytes.length);
    for (let i = 0; i < inputBytes.length; i += 1) {
      out[i] = inputBytes[i] ^ keyBytes[i % keyBytes.length];
    }
    return out;
  }

  function bytesToBase64(bytes) {
    let binary = "";
    const chunk = 0x8000;
    for (let i = 0; i < bytes.length; i += chunk) {
      binary += String.fromCharCode.apply(null, bytes.subarray(i, i + chunk));
    }
    return btoa(binary);
  }

  function base64ToBytes(base64) {
    const binary = atob(base64);
    const bytes = new Uint8Array(binary.length);
    for (let i = 0; i < binary.length; i += 1) {
      bytes[i] = binary.charCodeAt(i);
    }
    return bytes;
  }

  function encryptText(text) {
    const encoder = new TextEncoder();
    const data = encoder.encode(String(text || ""));
    const key = encoder.encode(CRYPTO_KEY);
    const encrypted = xorBytes(data, key);
    return bytesToBase64(encrypted);
  }

  function decryptText(cipherText) {
    const decoder = new TextDecoder();
    const encoder = new TextEncoder();
    const key = encoder.encode(CRYPTO_KEY);
    const encrypted = base64ToBytes(String(cipherText || ""));
    const data = xorBytes(encrypted, key);
    return decoder.decode(data);
  }

  function encodePayloadObject(payload) {
    const value = isPlainObject(payload) ? payload : {};
    return {
      [PAYLOAD_KEY]: encryptText(JSON.stringify(value))
    };
  }

  function decodePayloadObject(payload) {
    if (!isPlainObject(payload)) {
      return {};
    }

    const encrypted = payload[PAYLOAD_KEY];
    if (typeof encrypted === "string" && encrypted.trim()) {
      try {
        const decoded = JSON.parse(decryptText(encrypted));
        return isPlainObject(decoded) ? decoded : {};
      } catch {
        return {};
      }
    }

    return payload;
  }

  function encodeShare(name, payload) {
    const object = {
      name: normalizeName(name),
      payload: isPlainObject(payload) ? payload : {}
    };
    return SHARE_PREFIX + encryptText(JSON.stringify(object));
  }

  function decodeShare(code) {
    if (typeof code !== "string") {
      return null;
    }

    const value = code.trim();
    if (!value.startsWith(SHARE_PREFIX)) {
      return null;
    }

    try {
      const decoded = JSON.parse(decryptText(value.slice(SHARE_PREFIX.length)));
      return isPlainObject(decoded) ? decoded : null;
    } catch {
      return null;
    }
  }

  function normalizeRecord(raw) {
    const normalizedName = normalizeName(raw.name);
    return {
      id: String(raw.id || buildIdFromName(normalizedName)),
      name: normalizedName,
      payload: decodePayloadObject(raw.payload),
      createdAt: raw.created_at || raw.createdAt || nowIso(),
      updatedAt: raw.updated_at || raw.updatedAt || nowIso()
    };
  }

  function sortByUpdated(items) {
    return [...items].sort((a, b) => {
      const aTime = new Date(a.updatedAt || 0).getTime();
      const bTime = new Date(b.updatedAt || 0).getTime();
      return bTime - aTime;
    });
  }

  function ensureConfigured() {
    if (typeof cfg.anonKey !== "string" || !cfg.anonKey.trim()) {
      throw new Error("Cloud config missing: anonKey");
    }
  }

  function resolveProjectBase(config) {
    const configured = String(config.projectUrl || "").trim();
    const currentOrigin = typeof window !== "undefined" && window.location ? String(window.location.origin || "").trim() : "";

    if (currentOrigin) {
      return currentOrigin.replace(/\/$/, "");
    }

    return configured.replace(/\/$/, "");
  }

  function makeSupabaseAdapter(config) {
    const base = `${resolveProjectBase(config)}/rest/v1`;
    const table = config.table || "configs";

    async function request(path, options = {}) {
      ensureConfigured();

      const headers = {
        apikey: config.anonKey,
        Authorization: `Bearer ${config.anonKey}`,
        "Content-Type": "application/json",
        ...options.headers
      };

      const res = await fetch(`${base}${path}`, {
        ...options,
        headers
      });

      if (!res.ok) {
        const text = await res.text();
        throw new Error(`HTTP ${res.status}: ${text || "request failed"}`);
      }

      if (res.status === 204) {
        return null;
      }

      return res.json();
    }

    return {
      mode: "Cloud",
      async listConfigs() {
        const rows = await request(`/${table}?select=id,name,payload,created_at,updated_at&order=updated_at.desc`);
        return Array.isArray(rows) ? sortByUpdated(rows.map(normalizeRecord)) : [];
      },
      async upsertConfig(input) {
        const stableId = canonicalId(input.id, input.name);
        const previousId = typeof input.id === "string" ? input.id.trim() : "";

        if (previousId && previousId !== stableId && !previousId.toLowerCase().startsWith("cfg:")) {
          await request(`/${table}?id=eq.${encodeURIComponent(previousId)}`, {
            method: "DELETE"
          });
        }

        const row = {
          id: stableId,
          name: normalizeName(input.name),
          payload: encodePayloadObject(input.payload),
          updated_at: nowIso()
        };

        const data = await request(`/${table}?on_conflict=id`, {
          method: "POST",
          headers: {
            Prefer: "resolution=merge-duplicates,return=representation"
          },
          body: JSON.stringify([row])
        });

        const first = Array.isArray(data) ? data[0] : row;
        return normalizeRecord(first);
      },
      async deleteConfig(id) {
        const safeId = encodeURIComponent(String(id));
        await request(`/${table}?id=eq.${safeId}`, {
          method: "DELETE"
        });
      },
      async bulkImport(items) {
        const rows = items.map((item) => ({
          id: canonicalId(item.id, item.name),
          name: normalizeName(item.name),
          payload: encodePayloadObject(item.payload),
          updated_at: nowIso()
        }));

        await request(`/${table}?on_conflict=id`, {
          method: "POST",
          headers: {
            Prefer: "resolution=merge-duplicates,return=representation"
          },
          body: JSON.stringify(rows)
        });

        return this.listConfigs();
      }
    };
  }

  window.CloudCrypto = {
    payloadKey: PAYLOAD_KEY,
    sharePrefix: SHARE_PREFIX,
    encodePayloadObject,
    decodePayloadObject,
    encodeShare,
    decodeShare,
    encryptText,
    decryptText
  };

  window.CloudStore = makeSupabaseAdapter(cfg);
})();
