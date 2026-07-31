package xd.harm.modules.impl.player;

import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.EntityType;
import net.minecraft.network.play.server.*;
import xd.harm.events.network.EventPacket;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.ModeSetting;
import xd.harm.modules.settings.impl.SliderSetting;
import xd.harm.modules.settings.impl.StringSetting;
import xd.harm.utils.math.TimerHelper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ModuleRegister(name = "AutoSolverLLM", category = Category.Player, desc = "3x3 captcha solver via LLM (OpenRouter)")
public class AutoSolverLLM extends Module {

    private final SliderSetting gridX = new SliderSetting("Grid X", 555, 0, 4000, 1);
    private final SliderSetting gridY = new SliderSetting("Grid Y", 142, 0, 4000, 1);
    private final SliderSetting tileSize = new SliderSetting("Tile size", 120, 20, 400, 1);
    private final SliderSetting gap = new SliderSetting("Gap", 1, 0, 20, 1);
    private final StringSetting endpoint = new StringSetting("Endpoint", "https://openrouter.ai/api/v1/chat/completions", "LLM API endpoint");
    private final StringSetting model = new StringSetting("Model", "nvidia/nemotron-nano-12b-v2-vl:free", "Model ID");
    private final StringSetting apiKey = new StringSetting("API Key", "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", "OpenRouter API key");
    private final ModeSetting start = new ModeSetting("Start", "Off", "Off", "Solve");
    private final ModeSetting mode = new ModeSetting("Mode", "Screenshot", "Screenshot", "Packet");
    private final BooleanSetting debug = new BooleanSetting("Debug", true);
    private final SliderSetting timeoutMs = new SliderSetting("Timeout", 60, 5, 120, 1);

    private final TimerHelper timer = new TimerHelper();

    private final Map<Integer, FrameInfo> frames = new ConcurrentHashMap<>();
    private final Map<Integer, byte[]> mapData = new ConcurrentHashMap<>();
    private boolean captchaActive = false;
    private int captchaAttempts = 0;

    private volatile String pendingBase64 = null;
    private volatile int[] pendingClicks = null;
    private volatile boolean llmWorking = false;

    private static final int[][] MAP_BASE = {
            {0,0,0},{127,178,56},{247,233,163},{199,199,199},{167,167,167},{255,0,0},
            {160,160,255},{167,167,167},{0,124,0},{255,252,245},{136,136,136},{139,69,19},
            {255,252,0},{255,252,0},{167,167,167},{127,127,127},{54,54,54},{0,0,0},
            {197,197,197},{136,136,136},{0,0,0},{197,197,197},{136,136,136},{139,69,19},
            {139,69,19},{0,124,0},{0,124,0},{0,124,0},{255,252,245},{216,127,51},
            {178,76,216},{102,153,216},{229,229,51},{127,204,25},{242,127,165},{76,127,153},
            {153,153,153},{76,76,76},{0,112,160},{247,247,247},{217,178,127},{247,233,163},
            {153,159,183},{127,178,56},{102,153,216},{127,178,56},{217,178,127},{153,159,183},
            {102,153,216},{178,76,216},{76,127,153},{76,76,76},{127,127,127},{153,153,153},
            {76,76,76},{0,0,0},{160,160,255},{167,167,167},{0,124,0},{54,54,54},
            {136,136,136},{54,54,54},{139,69,19},{178,76,216},{76,127,153},{127,204,25},
            {127,178,56},{197,197,197},{153,153,153},{136,136,136},{76,76,76},{54,54,54},
            {136,136,136},{247,247,247},{217,178,127},{247,233,163},{167,167,167},
            {197,197,197},{76,76,76},{136,136,136},{0,0,0},{0,0,0},{0,0,0},
            {54,54,54},{216,127,51},{216,127,51},{216,127,51},{216,127,51}
    };
    private static final int[] SHADE_MUL = {180, 220, 255, 135};
    private static final int[][] MAP_PALETTE = new int[256][4];
    static {
        for (int b = 0; b < 256; b++) {
            int ci = Math.min(b >> 2, MAP_BASE.length - 1);
            int sh = b & 3;
            int[] base = MAP_BASE[ci];
            float m = SHADE_MUL[sh] / 255f;
            MAP_PALETTE[b][0] = Math.round(base[0] * m);
            MAP_PALETTE[b][1] = Math.round(base[1] * m);
            MAP_PALETTE[b][2] = Math.round(base[2] * m);
            MAP_PALETTE[b][3] = ci == 0 ? 0 : 255;
        }
    }

    public AutoSolverLLM() {
        addSettings(gridX, gridY, tileSize, gap, endpoint, model, apiKey, start, mode, debug, timeoutMs);
    }

    private int lastDataLogTick = 0;

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (!start.is("Solve")) return;

        if (pendingClicks != null) {
            applyClicks(pendingClicks);
            pendingClicks = null;
            start.set("Off");
            timer.reset();
            return;
        }

        if (llmWorking || pendingBase64 != null) return;
        if (!timer.hasReached(3000)) return;

        try {
            String b64;
            if (mode.is("Screenshot")) {
                b64 = prepareScreenshot();
                if (b64 == null) {
                    start.set("Off");
                    timer.reset();
                    return;
                }
            } else {
                if (frames.size() < 9 || mapData.size() < 9) {
                    lastDataLogTick++;
                    if (lastDataLogTick % 30 == 0) {
                        print("Waiting: frames=" + frames.size() + "/9 maps=" + mapData.size() + "/9");
                    }
                    timer.reset();
                    return;
                }
                lastDataLogTick = 0;
                b64 = preparePacket();
                if (b64 == null) {
                    start.set("Off");
                    timer.reset();
                    return;
                }
            }

            pendingBase64 = b64;
            llmWorking = true;

            final String finalB64 = b64;
            new Thread(() -> {
                try {
                    print("LLM request sent...");
                    int[] result = callLLM(finalB64);
                    if (result != null) {
                        pendingClicks = result;
                        print("LLM: " + Arrays.toString(result));
                    } else {
                        print("LLM returned null");
                    }
                } catch (Exception e) {
                    print("LLM error: " + e.getMessage());
                } finally {
                    llmWorking = false;
                    pendingBase64 = null;
                }
            }, "AutoSolverLLM-HTTP").start();

        } catch (Exception e) {
            e.printStackTrace();
            print("Error: " + e.getMessage());
            start.set("Off");
            timer.reset();
        }
    }

    @Subscribe
    public void onPacket(EventPacket e) {
        if (!e.isReceive()) return;

        try {
            Object pkt = e.getPacket();

            if (pkt instanceof SSpawnObjectPacket) {
                SSpawnObjectPacket p = (SSpawnObjectPacket) pkt;
                EntityType<?> t = p.getType();
                if (debug.get() && (t == EntityType.ITEM_FRAME || t == EntityType.PAINTING || t == EntityType.LEASH_KNOT)) {
                    print("SpawnObj type=" + t + " id=" + p.getEntityID());
                }
                if (t == EntityType.ITEM_FRAME) {
                    frames.put(p.getEntityID(), new FrameInfo(p.getEntityID(), p.getX(), p.getY(), p.getZ(), p.getYaw()));
                    print("Frame #" + p.getEntityID() + " pos=" + (int)p.getX() + "," + (int)p.getY() + "," + (int)p.getZ());
                }
            }

            if (pkt instanceof SEntityMetadataPacket) {
                SEntityMetadataPacket p = (SEntityMetadataPacket) pkt;
                FrameInfo fi = frames.get(p.getEntityId());
                if (fi != null) {
                    try {
                        p.getDataManagerEntries().forEach(entry -> {
                            try {
                                if (entry.getKey().getId() == 7 && entry.getValue() != null) {
                                    String valStr = entry.getValue().toString();
                                    int idx = valStr.indexOf("map:");
                                    if (idx >= 0) {
                                        StringBuilder num = new StringBuilder();
                                        for (int i = idx + 4; i < valStr.length(); i++) {
                                            char ch = valStr.charAt(i);
                                            if (ch >= '0' && ch <= '9') num.append(ch);
                                            else break;
                                        }
                                        if (num.length() > 0) {
                                            fi.mapId = Integer.parseInt(num.toString());
                                            print("Frame #" + fi.entityId + " -> map " + fi.mapId);
                                        }
                                    }
                                }
                            } catch (Exception ex) {
                                print("Meta entry err: " + ex.getMessage());
                            }
                        });
                    } catch (Exception ex) {
                        print("Metadata err: " + ex.getMessage());
                    }
                }
            }

            if (pkt instanceof SMapDataPacket) {
                SMapDataPacket p = (SMapDataPacket) pkt;
                if (p.getMapId() >= 1 && p.getMapId() <= 9) {
                    byte[] raw = p.getMapDataBytes();
                    if (raw != null && p.getColumns() > 0 && p.getRows() > 0) {
                        mapData.put(p.getMapId(), raw.clone());
                        print("Map " + p.getMapId() + " (" + p.getColumns() + "x" + p.getRows() + ") [" + mapData.size() + "/9]");
                    }
                }
            }

            if (pkt instanceof SChatPacket) {
                SChatPacket p = (SChatPacket) pkt;
                String msg = p.getChatComponent().getString().toLowerCase();
                if (debug.get() && msg.length() > 0) {
                    print("Chat: " + p.getChatComponent().getString().substring(0, Math.min(60, p.getChatComponent().getString().length())));
                }
                String[] triggers = {"решите пазл", "поворачивая", "проверк", "капч"};
                for (String t : triggers) {
                    if (msg.contains(t) && !captchaActive) {
                        captchaActive = true;
                        captchaAttempts = 0;
                        frames.clear();
                        mapData.clear();
                        print("CAPTCHA detected!");
                        break;
                    }
                }
                String[] done = {"успешно прошли проверку", "вы вошли в игру", "проверка пройдена"};
                for (String d : done) {
                    if (msg.contains(d)) {
                        captchaActive = false;
                        print("Solved!");
                        break;
                    }
                }
            }

            if (pkt instanceof SDestroyEntitiesPacket) {
                SDestroyEntitiesPacket p = (SDestroyEntitiesPacket) pkt;
                for (int id : p.getEntityIDs()) frames.remove(id);
            }
        } catch (Exception ex) {
            print("Packet err: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    private String prepareScreenshot() throws Exception {
        int gx = gridX.getInt();
        int gy = gridY.getInt();
        int size = tileSize.getInt();
        int g = gap.getInt();
        int step = size + g;

        BufferedImage screenshot = captureScreenshot();
        if (screenshot == null) { print("Screenshot failed"); return null; }

        int fbW = screenshot.getWidth();
        int fbH = screenshot.getHeight();
        int totalW = step * 3;
        int totalH = step * 3;
        if (gx < 0 || gy < 0 || gx + totalW > fbW || gy + totalH > fbH) {
            print("Grid OOB"); return null;
        }

        BufferedImage[] tiles = new BufferedImage[9];
        int idx = 0;
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 3; c++)
                tiles[idx++] = screenshot.getSubimage(gx + c * step, gy + r * step, size, size);

        BufferedImage composite = new BufferedImage(384, 384, BufferedImage.TYPE_INT_RGB);
        int[] rgb = new int[size * size];
        for (int i = 0; i < 9; i++) {
            tiles[i].getRGB(0, 0, size, size, rgb, 0, size);
            BufferedImage tile128 = new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB);
            tile128.setRGB(0, 0, 128, 128, scaleRGB(rgb, size, 128), 0, 128);
            int row = i / 3, col = i % 3;
            composite.getGraphics().drawImage(tile128, col * 128, row * 128, null);
        }

        if (debug.get()) {
            try { ImageIO.write(composite, "png", new File("autosolver_screenshot.png")); } catch (Exception ignored) {}
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(composite, "png", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    private String preparePacket() throws Exception {
        if (frames.size() < 9 || mapData.size() < 9) {
            print("Waiting data: frames=" + frames.size() + " maps=" + mapData.size());
            return null;
        }

        List<FrameInfo> list = new ArrayList<>(frames.values());
        list.removeIf(f -> f.mapId == null || f.mapId < 1 || f.mapId > 9);
        if (list.size() < 9) { print("Missing map IDs"); return null; }

        Set<Integer> xs = new HashSet<>();
        Set<Integer> zs = new HashSet<>();
        for (FrameInfo f : list) { xs.add((int)Math.round(f.x)); zs.add((int)Math.round(f.z)); }
        boolean useZ = zs.size() >= xs.size();
        list.sort((a, b) -> {
            int by = Double.compare(b.y, a.y);
            if (by != 0) return by;
            return Double.compare(useZ ? b.z : b.x, useZ ? a.z : a.x);
        });

        int TILE = 128;
        BufferedImage[] tileImages = new BufferedImage[10];
        for (Map.Entry<Integer, byte[]> me : mapData.entrySet()) {
            int mid = me.getKey();
            byte[] raw = me.getValue();
            BufferedImage img = new BufferedImage(TILE, TILE, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < TILE; y++)
                for (int x = 0; x < TILE; x++) {
                    int i2 = y * TILE + x;
                    int bv = i2 < raw.length ? (raw[i2] & 0xFF) : 0;
                    int[] c = MAP_PALETTE[bv];
                    img.setRGB(x, y, (c[0] << 16) | (c[1] << 8) | c[2]);
                }
            tileImages[mid] = img;
        }

        BufferedImage composite = new BufferedImage(TILE * 3, TILE * 3, BufferedImage.TYPE_INT_RGB);
        for (int i = 0; i < 9 && i < list.size(); i++) {
            BufferedImage tile = tileImages[list.get(i).mapId];
            if (tile != null)
                composite.getGraphics().drawImage(tile, (i % 3) * TILE, (i / 3) * TILE, null);
        }

        if (debug.get()) {
            try { ImageIO.write(composite, "png", new File("autosolver_packet.png")); } catch (Exception ignored) {}
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(composite, "png", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    private void applyClicks(int[] clicks) {
        int gx = gridX.getInt();
        int gy = gridY.getInt();
        int size = tileSize.getInt();
        int g = gap.getInt();
        int step = size + g;
        double guiScale = mc.getMainWindow().getGuiScaleFactor();

        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                int n = clicks[r * 3 + c];
                if (n <= 0) continue;
                int cx = (int) ((gx + c * step + size / 2) / guiScale);
                int cy = (int) ((gy + r * step + size / 2) / guiScale);
                if (mc.currentScreen != null) {
                    for (int k = 0; k < n; k++) {
                        mc.currentScreen.mouseClicked(cx, cy, 0);
                        if (mc.currentScreen == null) break;
                    }
                }
            }
        }

        if (mode.is("Packet")) captchaActive = false;
        print("Clicks applied");
    }

    private int[] callLLM(String b64Image) {
        String prompt =
            "TASK: Solve a 3x3 rotation puzzle from a Minecraft captcha.\n\n" +
            "The image shows a 3x3 grid of 9 square tiles. All 9 tiles are pieces of ONE single picture " +
            "(for example: a face, an animal, a landscape, a building, food, etc.). " +
            "Each tile has been randomly rotated by 0, 90, 180, or 270 degrees clockwise.\n\n" +
            "YOUR GOAL: Figure out how many degrees CLOCKWISE to rotate each tile so the full picture looks correct.\n\n" +
            "HOW TO SOLVE:\n" +
            "1. Look at the overall image. What is the picture? (face, animal, object, etc.)\n" +
            "2. For each tile, check: are edges/lines continuous with neighbors? Is text upright? Is a face oriented correctly?\n" +
            "3. For each tile, decide: 0 = already correct, 90 = rotate 90 CW, 180 = rotate 180, 270 = rotate 270 CW (= 90 CCW)\n\n" +
            "Grid layout (row-major order):\n" +
            "  [1][2][3]\n" +
            "  [4][5][6]\n" +
            "  [7][8][9]\n\n" +
            "OUTPUT FORMAT: Reply with ONLY a JSON array of 9 integers, one for each tile.\n" +
            "Each integer is 0, 90, 180, or 270 (degrees clockwise).\n" +
            "Example: [0, 90, 180, 270, 0, 90, 0, 180, 0]\n\n" +
            "Do NOT explain. Only output the array.";

        String body = "{\"model\":\"" + esc(model.get()) + "\",\"temperature\":0.0,\"max_tokens\":100," +
                "\"messages\":[{\"role\":\"user\",\"content\":[" +
                "{\"type\":\"text\",\"text\":\"" + esc(prompt) + "\"}," +
                "{\"type\":\"image_url\",\"image_url\":{\"url\":\"data:image/png;base64," + b64Image + "\"}}" +
                "]}]}";

        try {
            HttpURLConnection c = (HttpURLConnection) new URL(endpoint.get()).openConnection();
            c.setConnectTimeout(15000);
            c.setReadTimeout(timeoutMs.getInt() * 1000);
            c.setRequestMethod("POST");
            c.setRequestProperty("Content-Type", "application/json");
            c.setRequestProperty("Authorization", "Bearer " + apiKey.get());
            c.setRequestProperty("HTTP-Referer", "https://github.com");
            c.setDoOutput(true);
            try (OutputStream os = c.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }

            int status = c.getResponseCode();
            if (status != 200) {
                String err = readStream(status >= 400 ? c.getErrorStream() : c.getInputStream());
                print("HTTP " + status + ": " + err.substring(0, Math.min(200, err.length())));
                return null;
            }

            String resp = readStream(c.getInputStream());
            c.disconnect();

            String content = extractContent(resp);
            if (content == null || content.isEmpty()) { print("Empty response"); return null; }
            if (debug.get()) print("LLM: " + content);

            return parseRotations(content);
        } catch (Exception e) {
            print("Error: " + e.getMessage());
            return null;
        }
    }

    private int[] parseRotations(String content) {
        int start = content.indexOf('[');
        int end = content.lastIndexOf(']');
        if (start < 0 || end < 0) return null;
        String arrStr = content.substring(start, end + 1);
        String[] parts = arrStr.replace("[", "").replace("]", "").split(",");
        List<Integer> vals = new ArrayList<>();
        for (String p : parts) {
            try { vals.add(Integer.parseInt(p.trim())); } catch (Exception ignored) {}
        }
        if (vals.size() != 9) return null;

        int[] clicks = new int[9];
        for (int i = 0; i < 9; i++) {
            int d = vals.get(i);
            clicks[i] = (Math.round(d / 90f) % 4 + 4) % 4;
        }
        return clicks;
    }

    private BufferedImage captureScreenshot() {
        net.minecraft.client.renderer.texture.NativeImage ni = null;
        try {
            net.minecraft.client.shader.Framebuffer fb = mc.getFramebuffer();
            ni = net.minecraft.util.ScreenShotHelper.createScreenshot(fb.framebufferWidth, fb.framebufferHeight, fb);
            int w = ni.getWidth();
            int h = ni.getHeight();
            BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < h; y++)
                for (int x = 0; x < w; x++) {
                    int abgr = ni.getPixelRGBA(x, y);
                    bi.setRGB(x, y, ((abgr & 0xFF) << 16) | (((abgr >> 8) & 0xFF) << 8) | ((abgr >> 16) & 0xFF));
                }
            return bi;
        } catch (Exception e) {
            return null;
        } finally {
            if (ni != null) ni.close();
        }
    }

    private int[] scaleRGB(int[] src, int srcSize, int dstSize) {
        int[] dst = new int[dstSize * dstSize];
        float ratio = (float) srcSize / dstSize;
        for (int y = 0; y < dstSize; y++)
            for (int x = 0; x < dstSize; x++)
                dst[y * dstSize + x] = src[Math.min((int)(y * ratio), srcSize - 1) * srcSize + Math.min((int)(x * ratio), srcSize - 1)];
        return dst;
    }

    private String readStream(InputStream is) throws IOException {
        if (is == null) return "";
        try (BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            return sb.toString();
        }
    }

    private String extractContent(String resp) {
        int i = resp.indexOf("\"content\":\"");
        if (i < 0) return null;
        StringBuilder out = new StringBuilder();
        for (int j = i + 11; j < resp.length(); j++) {
            char ch = resp.charAt(j);
            if (ch == '\\') { j++; if (j < resp.length()) out.append(resp.charAt(j)); continue; }
            if (ch == '"') break;
            out.append(ch);
        }
        return out.toString();
    }

    private String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    private static class FrameInfo {
        int entityId;
        double x, y, z;
        int yaw;
        Integer mapId;

        FrameInfo(int entityId, double x, double y, double z, int yaw) {
            this.entityId = entityId;
            this.x = x; this.y = y; this.z = z; this.yaw = yaw;
        }
    }

    @Override
    public boolean onEnable() {
        timer.reset();
        pendingBase64 = null;
        pendingClicks = null;
        llmWorking = false;
        return super.onEnable();
    }
}
