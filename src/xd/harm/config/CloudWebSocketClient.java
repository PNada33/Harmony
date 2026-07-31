package xd.harm.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

final class CloudWebSocketClient {

    private static final JsonParser PARSER = new JsonParser();
    private static final String HELLO_OP = "hello";
    private static final String CHALLENGE_OP = "challenge";
    private static final String REQUEST_OP = "request";
    private static final String RESPONSE_OP = "response";

    private final CloudSettingsLoader.CloudSettings settings;

    CloudWebSocketClient(CloudSettingsLoader.CloudSettings settings) {
        this.settings = settings;
    }

    JsonObject request(String action, JsonObject payload) throws IOException {
        ensureConfigured();
        URI uri = normalizeUri(settings.websocketUrl);

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(uri.getHost(), resolvePort(uri)), settings.timeoutMs);
            socket.setSoTimeout(settings.timeoutMs);

            BufferedInputStream input = new BufferedInputStream(socket.getInputStream());
            BufferedOutputStream output = new BufferedOutputStream(socket.getOutputStream());

            performHandshake(uri, input, output);

            byte[] clientNonce = CloudTransportCodec.randomBytes(16);
            String clientNonceBase64 = Base64.getEncoder().encodeToString(clientNonce);

            JsonObject hello = new JsonObject();
            hello.addProperty("op", HELLO_OP);
            hello.addProperty("action", action);
            hello.addProperty("clientNonce", clientNonceBase64);
            sendTextFrame(output, hello.toString());

            JsonObject challenge = parseFrame(readTextFrame(input, output));
            if (!CHALLENGE_OP.equals(getString(challenge, "op"))) {
                throw new IOException("Unexpected WebSocket challenge payload");
            }

            String serverNonceBase64 = getString(challenge, "serverNonce");
            if (serverNonceBase64.isEmpty()) {
                throw new IOException("Cloud challenge is missing server nonce");
            }

            byte[] serverNonce = Base64.getDecoder().decode(serverNonceBase64);
            byte[] sessionKey = CloudTransportCodec.deriveSessionKey(settings.anonKey, clientNonce, serverNonce);

            JsonObject request = new JsonObject();
            request.addProperty("op", REQUEST_OP);
            request.addProperty("proof", CloudTransportCodec.createProof(sessionKey, action, clientNonceBase64, serverNonceBase64));
            request.add("payload", CloudTransportCodec.encryptRequest(sessionKey, payload == null ? new JsonObject() : payload));
            sendTextFrame(output, request.toString());

            JsonObject responseEnvelope = parseFrame(readTextFrame(input, output));
            if (!RESPONSE_OP.equals(getString(responseEnvelope, "op"))) {
                throw new IOException("Unexpected WebSocket response payload");
            }

            JsonObject encryptedResponse = getObject(responseEnvelope, "payload");
            JsonObject response = CloudTransportCodec.decryptRequest(sessionKey, encryptedResponse);
            if (response == null) {
                throw new IOException("Cloud response decryption failed");
            }

            if (!getBoolean(response, "ok", false)) {
                throw new IOException(getString(response, "error"));
            }

            try {
                sendCloseFrame(output);
            } catch (Exception ignored) {
            }

            return response;
        } catch (IOException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IOException("Cloud WebSocket request failed", exception);
        }
    }

    private void ensureConfigured() throws IOException {
        if (!settings.isWebSocketConfigured()) {
            throw new IOException("Cloud WebSocket settings are invalid: set websocketUrl and anonKey");
        }
    }

    private URI normalizeUri(String rawValue) throws IOException {
        try {
            String value = rawValue == null ? "" : rawValue.trim();
            if (value.startsWith("ws://localhost")) {
                value = "ws://127.0.0.1" + value.substring("ws://localhost".length());
            }
            URI uri = URI.create(value);
            if (!"ws".equalsIgnoreCase(uri.getScheme())) {
                throw new IOException("Only ws:// endpoints are supported");
            }
            if (uri.getHost() == null || uri.getHost().trim().isEmpty()) {
                throw new IOException("Cloud WebSocket host is missing");
            }
            return uri;
        } catch (IllegalArgumentException exception) {
            throw new IOException("Invalid cloud WebSocket URL", exception);
        }
    }

    private int resolvePort(URI uri) {
        return uri.getPort() > 0 ? uri.getPort() : 80;
    }

    private void performHandshake(URI uri, InputStream input, OutputStream output) throws Exception {
        byte[] rawKey = CloudTransportCodec.randomBytes(16);
        String webSocketKey = Base64.getEncoder().encodeToString(rawKey);
        String path = buildPath(uri);
        String hostHeader = buildHostHeader(uri);

        StringBuilder request = new StringBuilder();
        request.append("GET ").append(path).append(" HTTP/1.1\r\n");
        request.append("Host: ").append(hostHeader).append("\r\n");
        request.append("Upgrade: websocket\r\n");
        request.append("Connection: Upgrade\r\n");
        request.append("Sec-WebSocket-Version: 13\r\n");
        request.append("Sec-WebSocket-Key: ").append(webSocketKey).append("\r\n");
        request.append("\r\n");

        output.write(request.toString().getBytes(StandardCharsets.US_ASCII));
        output.flush();

        String statusLine = readHttpLine(input);
        if (statusLine == null || !statusLine.contains("101")) {
            throw new IOException("Cloud WebSocket upgrade failed: " + statusLine);
        }

        Map<String, String> headers = new LinkedHashMap<>();
        while (true) {
            String line = readHttpLine(input);
            if (line == null || line.isEmpty()) {
                break;
            }

            int separatorIndex = line.indexOf(':');
            if (separatorIndex <= 0) {
                continue;
            }

            String key = line.substring(0, separatorIndex).trim().toLowerCase(Locale.ROOT);
            String value = line.substring(separatorIndex + 1).trim();
            headers.put(key, value);
        }

        String accept = headers.get("sec-websocket-accept");
        String expectedAccept = expectedAccept(webSocketKey);
        if (accept == null || !accept.trim().equals(expectedAccept)) {
            throw new IOException("Invalid cloud WebSocket accept header");
        }
    }

    private String buildPath(URI uri) {
        String path = uri.getRawPath();
        if (path == null || path.isEmpty()) {
            path = "/";
        }
        String query = uri.getRawQuery();
        return query == null || query.isEmpty() ? path : path + "?" + query;
    }

    private String buildHostHeader(URI uri) {
        int port = resolvePort(uri);
        return port == 80 ? uri.getHost() : uri.getHost() + ":" + port;
    }

    private String expectedAccept(String webSocketKey) throws Exception {
        String magic = webSocketKey + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
        byte[] hash = MessageDigest.getInstance("SHA-1").digest(magic.getBytes(StandardCharsets.US_ASCII));
        return Base64.getEncoder().encodeToString(hash);
    }

    private String readHttpLine(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        int previous = -1;

        while (true) {
            int current = input.read();
            if (current == -1) {
                if (output.size() == 0) {
                    return null;
                }
                break;
            }

            if (previous == '\r' && current == '\n') {
                break;
            }

            if (previous != -1) {
                output.write(previous);
            }
            previous = current;
        }

        return new String(output.toByteArray(), StandardCharsets.US_ASCII);
    }

    private void sendTextFrame(OutputStream output, String text) throws IOException {
        sendMaskedFrame(output, 0x1, text.getBytes(StandardCharsets.UTF_8));
    }

    private void sendCloseFrame(OutputStream output) throws IOException {
        sendMaskedFrame(output, 0x8, new byte[0]);
    }

    private void sendPongFrame(OutputStream output, byte[] payload) throws IOException {
        sendMaskedFrame(output, 0xA, payload);
    }

    private void sendMaskedFrame(OutputStream output, int opcode, byte[] payload) throws IOException {
        int firstByte = 0x80 | (opcode & 0x0F);
        output.write(firstByte);

        int length = payload.length;
        if (length <= 125) {
            output.write(0x80 | length);
        } else if (length <= 0xFFFF) {
            output.write(0x80 | 126);
            output.write((length >>> 8) & 0xFF);
            output.write(length & 0xFF);
        } else {
            output.write(0x80 | 127);
            long longLength = length;
            for (int i = 7; i >= 0; i--) {
                output.write((int) ((longLength >>> (i * 8)) & 0xFF));
            }
        }

        byte[] mask = CloudTransportCodec.randomBytes(4);
        output.write(mask);
        for (int i = 0; i < payload.length; i++) {
            output.write(payload[i] ^ mask[i % 4]);
        }
        output.flush();
    }

    private String readTextFrame(InputStream input, OutputStream output) throws IOException {
        while (true) {
            int first = input.read();
            if (first == -1) {
                throw new EOFException("Unexpected end of WebSocket stream");
            }

            int second = input.read();
            if (second == -1) {
                throw new EOFException("Unexpected end of WebSocket stream");
            }

            int opcode = first & 0x0F;
            boolean masked = (second & 0x80) != 0;
            long length = second & 0x7F;

            if (length == 126) {
                length = ((long) readByte(input) << 8) | readByte(input);
            } else if (length == 127) {
                length = 0;
                for (int i = 0; i < 8; i++) {
                    length = (length << 8) | readByte(input);
                }
            }

            byte[] mask = masked ? readFully(input, 4) : null;
            byte[] payload = readFully(input, (int) length);

            if (masked && mask != null) {
                for (int i = 0; i < payload.length; i++) {
                    payload[i] = (byte) (payload[i] ^ mask[i % 4]);
                }
            }

            if (opcode == 0x8) {
                return "";
            }
            if (opcode == 0x9) {
                sendPongFrame(output, payload);
                continue;
            }
            if (opcode == 0xA) {
                continue;
            }
            if (opcode != 0x1 && opcode != 0x2) {
                continue;
            }

            return new String(payload, StandardCharsets.UTF_8);
        }
    }

    private int readByte(InputStream input) throws IOException {
        int value = input.read();
        if (value == -1) {
            throw new EOFException("Unexpected end of WebSocket stream");
        }
        return value;
    }

    private byte[] readFully(InputStream input, int length) throws IOException {
        byte[] buffer = new byte[length];
        int offset = 0;
        while (offset < length) {
            int read = input.read(buffer, offset, length - offset);
            if (read < 0) {
                throw new EOFException("Unexpected end of WebSocket stream");
            }
            offset += read;
        }
        return buffer;
    }

    private JsonObject parseFrame(String raw) throws IOException {
        try {
            JsonElement parsed = PARSER.parse(raw);
            if (parsed != null && parsed.isJsonObject()) {
                return parsed.getAsJsonObject();
            }
        } catch (Exception exception) {
            throw new IOException("Failed to parse cloud WebSocket JSON frame", exception);
        }
        throw new IOException("Cloud WebSocket frame is not a JSON object");
    }

    private String getString(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return "";
        }

        try {
            String value = element.getAsString();
            return value == null ? "" : value.trim();
        } catch (Exception ignored) {
            return "";
        }
    }

    private boolean getBoolean(JsonObject object, String key, boolean defaultValue) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return defaultValue;
        }

        try {
            return element.getAsBoolean();
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private JsonObject getObject(JsonObject object, String key) {
        JsonElement element = object.get(key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
    }
}
