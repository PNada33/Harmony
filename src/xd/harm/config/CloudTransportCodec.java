package xd.harm.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

final class CloudTransportCodec {

    private static final JsonParser PARSER = new JsonParser();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final byte[] PROTOCOL_LABEL = "HarmonyCloudWSv1".getBytes(StandardCharsets.UTF_8);
    private static final byte[] ENC_LABEL = "enc".getBytes(StandardCharsets.UTF_8);
    private static final byte[] MAC_LABEL = "mac".getBytes(StandardCharsets.UTF_8);

    private CloudTransportCodec() {
    }

    static byte[] randomBytes(int size) {
        byte[] value = new byte[size];
        RANDOM.nextBytes(value);
        return value;
    }

    static byte[] deriveSessionKey(String sharedSecret, byte[] clientNonce, byte[] serverNonce) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(PROTOCOL_LABEL);
        output.write(clientNonce);
        output.write(serverNonce);
        return hmac(sharedSecret.getBytes(StandardCharsets.UTF_8), output.toByteArray());
    }

    static String createProof(byte[] sessionKey, String action, String clientNonceBase64, String serverNonceBase64) throws Exception {
        String value = action + "|" + clientNonceBase64 + "|" + serverNonceBase64;
        return Base64.getEncoder().encodeToString(hmac(sessionKey, value.getBytes(StandardCharsets.UTF_8)));
    }

    static JsonObject encryptRequest(byte[] sessionKey, JsonObject payload) throws Exception {
        byte[] plain = payload.toString().getBytes(StandardCharsets.UTF_8);
        byte[] compressed = deflate(plain);
        byte[] iv = randomBytes(16);
        byte[] encKey = hmac(sessionKey, ENC_LABEL);
        byte[] macKey = hmac(sessionKey, MAC_LABEL);
        byte[] cipher = xorKeystream(encKey, iv, compressed);
        byte[] mac = computeMac(macKey, iv, cipher);

        JsonObject envelope = new JsonObject();
        envelope.addProperty("iv", Base64.getEncoder().encodeToString(iv));
        envelope.addProperty("ciphertext", Base64.getEncoder().encodeToString(cipher));
        envelope.addProperty("mac", Base64.getEncoder().encodeToString(mac));
        envelope.addProperty("compression", "deflate");
        return envelope;
    }

    static JsonObject decryptRequest(byte[] sessionKey, JsonObject envelope) throws Exception {
        byte[] iv = decodeField(envelope, "iv");
        byte[] cipher = decodeField(envelope, "ciphertext");
        byte[] mac = decodeField(envelope, "mac");

        byte[] encKey = hmac(sessionKey, ENC_LABEL);
        byte[] macKey = hmac(sessionKey, MAC_LABEL);
        byte[] expectedMac = computeMac(macKey, iv, cipher);
        if (!MessageDigest.isEqual(mac, expectedMac)) {
            return null;
        }

        byte[] compressed = xorKeystream(encKey, iv, cipher);
        byte[] plain = inflate(compressed);
        JsonElement parsed = PARSER.parse(new String(plain, StandardCharsets.UTF_8));
        return parsed != null && parsed.isJsonObject() ? parsed.getAsJsonObject() : null;
    }

    private static byte[] decodeField(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return new byte[0];
        }

        try {
            return Base64.getDecoder().decode(element.getAsString());
        } catch (Exception ignored) {
            return new byte[0];
        }
    }

    private static byte[] computeMac(byte[] macKey, byte[] iv, byte[] cipher) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(iv);
        output.write(cipher);
        return hmac(macKey, output.toByteArray());
    }

    private static byte[] xorKeystream(byte[] encKey, byte[] iv, byte[] input) throws Exception {
        byte[] output = Arrays.copyOf(input, input.length);
        int offset = 0;
        int counter = 0;

        while (offset < output.length) {
            ByteArrayOutputStream blockData = new ByteArrayOutputStream();
            blockData.write(iv);
            blockData.write(ByteBuffer.allocate(4).putInt(counter++).array());
            byte[] block = hmac(encKey, blockData.toByteArray());

            for (int i = 0; i < block.length && offset < output.length; i++) {
                output[offset] = (byte) (output[offset] ^ block[i]);
                offset++;
            }
        }

        return output;
    }

    private static byte[] hmac(byte[] key, byte[] data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data);
    }

    private static byte[] deflate(byte[] input) {
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        deflater.setInput(input);
        deflater.finish();

        byte[] buffer = new byte[4096];
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        while (!deflater.finished()) {
            int written = deflater.deflate(buffer);
            if (written <= 0) {
                break;
            }
            output.write(buffer, 0, written);
        }
        deflater.end();
        return output.toByteArray();
    }

    private static byte[] inflate(byte[] input) throws Exception {
        Inflater inflater = new Inflater();
        inflater.setInput(input);

        byte[] buffer = new byte[4096];
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        while (!inflater.finished()) {
            int read = inflater.inflate(buffer);
            if (read <= 0) {
                if (inflater.needsInput()) {
                    break;
                }
                throw new IllegalStateException("Failed to inflate cloud payload");
            }
            output.write(buffer, 0, read);
        }
        inflater.end();
        return output.toByteArray();
    }
}
