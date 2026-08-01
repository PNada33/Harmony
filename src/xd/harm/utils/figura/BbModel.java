package xd.harm.utils.figura;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Минимальный парсер Blockbench .bbmodel для FiguraLite.
 *
 * ИСПРАВЛЕНО (v3):
 *  1) MESH-элементы (type = "mesh") больше не выбрасываются. Из-за этого раньше
 *     целиком не грузились паки вроде "Cool scythe" (38 из 38 элементов — mesh),
 *     а также пропадали плащ Cool coat, зонт leaf, щиты и эффекты Blue Archive.
 *  2) UV теперь считаются от размера КОНКРЕТНОЙ текстуры (uv_width/uv_height),
 *     а не от общего resolution модели. Именно поэтому модели были
 *     розово-фиолетовыми и с разъехавшейся развёрткой: например у Mari
 *     main.bbmodel resolution 64x197, а skin.png — 64x64.
 *  3) Текстура может лежать отдельным PNG рядом с моделью (path / relative_path),
 *     а не только base64 внутри .bbmodel.
 *  4) Сохранено из v2: face.rotation (0/90/180/270), видимость как boolean и как число,
 *     пропуск вырожденных граней с нулевой площадью UV.
 *
 * Анимации не поддерживаются.
 */
public final class BbModel {
    /** Размер UV-полотна по умолчанию (resolution модели). */
    public float texW = 64f;
    public float texH = 64f;

    /** PNG-данные текстур; элемент может быть null, если текстуру не нашли. */
    public final List<byte[]> textures = new ArrayList<byte[]>();
    /** Размер UV-полотна каждой текстуры {width, height}; индексы как в textures. */
    public final List<float[]> textureUv = new ArrayList<float[]>();

    public final List<Bone> roots = new ArrayList<Bone>();

    public static final class Face {
        public float u1, v1, u2, v2;
        public int texture;
        /** Поворот развёртки в градусах: 0 / 90 / 180 / 270. */
        public int rotation;
    }

    public static final class Cube {
        public float[] from;
        public float[] to;
        public float[] origin;
        public float[] rotation;
        public float inflate;
        public final Map<String, Face> faces = new LinkedHashMap<String, Face>();
    }

    /** Грань меша: 3 или 4 вершины, координаты относительно origin меша. */
    public static final class MeshFace {
        public int texture;
        public float[][] pos;
        public float[][] uv;
    }

    public static final class Mesh {
        public float[] origin = {0f, 0f, 0f};
        public float[] rotation;
        public final List<MeshFace> faces = new ArrayList<MeshFace>();
    }

    public static final class Bone {
        public String name = "";
        public float[] origin = {0f, 0f, 0f};
        public float[] rotation;
        public boolean visible = true;
        public final List<Cube> cubes = new ArrayList<Cube>();
        public final List<Mesh> meshes = new ArrayList<Mesh>();
        public final List<Bone> children = new ArrayList<Bone>();
    }

    /** Ширина UV-полотна текстуры с указанным индексом. */
    public float uvWidth(int index) {
        if (index >= 0 && index < textureUv.size()) {
            float[] uv = textureUv.get(index);
            if (uv != null && uv[0] > 0f) return uv[0];
        }
        return texW;
    }

    /** Высота UV-полотна текстуры с указанным индексом. */
    public float uvHeight(int index) {
        if (index >= 0 && index < textureUv.size()) {
            float[] uv = textureUv.get(index);
            if (uv != null && uv[1] > 0f) return uv[1];
        }
        return texH;
    }

    public static BbModel parse(File file) throws Exception {
        JsonObject root;
        try (InputStreamReader reader = new InputStreamReader(
                Files.newInputStream(file.toPath()), StandardCharsets.UTF_8)) {
            root = new JsonParser().parse(reader).getAsJsonObject();
        }
        BbModel model = new BbModel();

        if (root.has("resolution") && root.get("resolution").isJsonObject()) {
            JsonObject res = root.getAsJsonObject("resolution");
            if (res.has("width")) model.texW = Math.max(1f, res.get("width").getAsFloat());
            if (res.has("height")) model.texH = Math.max(1f, res.get("height").getAsFloat());
        }

        File dir = file.getParentFile();
        if (root.has("textures") && root.get("textures").isJsonArray()) {
            for (JsonElement el : root.getAsJsonArray("textures")) {
                byte[] data = null;
                float uvW = model.texW;
                float uvH = model.texH;
                if (el.isJsonObject()) {
                    JsonObject tex = el.getAsJsonObject();
                    data = readTextureData(tex, dir);
                    uvW = readSize(tex, "uv_width", "width", uvW);
                    uvH = readSize(tex, "uv_height", "height", uvH);
                }
                model.textures.add(data);
                model.textureUv.add(new float[]{uvW, uvH});
            }
        }

        Map<String, Cube> cubesByUuid = new HashMap<String, Cube>();
        Map<String, Mesh> meshesByUuid = new HashMap<String, Mesh>();
        if (root.has("elements") && root.get("elements").isJsonArray()) {
            for (JsonElement el : root.getAsJsonArray("elements")) {
                if (!el.isJsonObject()) continue;
                JsonObject obj = el.getAsJsonObject();
                if (obj.has("export") && !asBool(obj.get("export"), true)) continue;
                if (obj.has("visibility") && !asBool(obj.get("visibility"), true)) continue;
                String type = obj.has("type") && obj.get("type").isJsonPrimitive()
                        ? obj.get("type").getAsString() : "cube";
                String uuid = obj.has("uuid") ? obj.get("uuid").getAsString() : null;
                if ("mesh".equals(type)) {
                    Mesh mesh = parseMesh(obj);
                    if (mesh != null && uuid != null) meshesByUuid.put(uuid, mesh);
                    continue;
                }
                if (!"cube".equals(type)) continue;
                Cube cube = parseCube(obj);
                if (cube != null && uuid != null) cubesByUuid.put(uuid, cube);
            }
        }

        Bone implicitRoot = new Bone();
        if (root.has("outliner") && root.get("outliner").isJsonArray()) {
            for (JsonElement el : root.getAsJsonArray("outliner")) {
                if (el.isJsonPrimitive()) {
                    attach(implicitRoot, el.getAsString(), cubesByUuid, meshesByUuid);
                } else if (el.isJsonObject()) {
                    Bone bone = parseBone(el.getAsJsonObject(), cubesByUuid, meshesByUuid);
                    if (bone != null) model.roots.add(bone);
                }
            }
        }
        // Элементы без группы или без outliner — в неявный корень.
        implicitRoot.cubes.addAll(cubesByUuid.values());
        implicitRoot.meshes.addAll(meshesByUuid.values());
        if (!implicitRoot.cubes.isEmpty() || !implicitRoot.meshes.isEmpty()) {
            model.roots.add(implicitRoot);
        }
        return model;
    }

    // ------------------------------------------------------------- Текстуры

    private static byte[] readTextureData(JsonObject tex, File dir) {
        if (tex.has("source") && tex.get("source").isJsonPrimitive()) {
            String source = tex.get("source").getAsString();
            int comma = source.indexOf(',');
            if (source.startsWith("data:") && comma > 0) {
                try {
                    return Base64.getDecoder().decode(
                            source.substring(comma + 1).replaceAll("\\s", ""));
                } catch (Exception ignored) {
                }
            }
        }
        String[] keys = {"relative_path", "path", "name"};
        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];
            if (!tex.has(key) || !tex.get(key).isJsonPrimitive()) continue;
            String raw = tex.get(key).getAsString();
            if (raw == null || raw.isEmpty()) continue;
            byte[] data = tryRead(dir, raw);
            if (data != null) return data;
            if (!raw.toLowerCase(Locale.ROOT).endsWith(".png")) {
                data = tryRead(dir, raw + ".png");
                if (data != null) return data;
            }
        }
        return null;
    }

    private static byte[] tryRead(File dir, String raw) {
        if (dir == null || raw == null) return null;
        String cleaned = raw.replace('\\', '/');
        String[] candidates = {
                cleaned,
                "textures/" + cleaned,
                "../textures/" + cleaned,
                "../" + cleaned
        };
        for (int i = 0; i < candidates.length; i++) {
            try {
                File f = new File(dir, candidates[i]);
                if (!f.isFile()) {
                    f = new File(candidates[i]);
                }
                if (f.isFile() && f.length() > 0L && f.length() < 32L * 1024L * 1024L) {
                    return Files.readAllBytes(f.toPath());
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private static float readSize(JsonObject tex, String primary, String fallback, float def) {
        float value = readFloat(tex, primary, -1f);
        if (value > 0f) return value;
        value = readFloat(tex, fallback, -1f);
        if (value > 0f) return value;
        return def;
    }

    private static float readFloat(JsonObject obj, String key, float def) {
        if (obj.has(key) && obj.get(key).isJsonPrimitive()) {
            try {
                return obj.get(key).getAsFloat();
            } catch (Exception ignored) {
            }
        }
        return def;
    }

    // -------------------------------------------------------------- Элементы

    private static Cube parseCube(JsonObject obj) {
        Cube cube = new Cube();
        cube.from = vec3(obj.get("from"));
        cube.to = vec3(obj.get("to"));
        if (cube.from == null || cube.to == null) return null;
        cube.origin = vec3(obj.get("origin"));
        cube.rotation = vec3(obj.get("rotation"));
        cube.inflate = readFloat(obj, "inflate", 0f);
        if (obj.has("faces") && obj.get("faces").isJsonObject()) {
            for (Map.Entry<String, JsonElement> fe : obj.getAsJsonObject("faces").entrySet()) {
                if (!fe.getValue().isJsonObject()) continue;
                JsonObject fo = fe.getValue().getAsJsonObject();
                if (!fo.has("uv") || !fo.get("uv").isJsonArray()) continue;
                JsonArray uv = fo.getAsJsonArray("uv");
                if (uv.size() < 4) continue;
                if (fo.has("texture") && fo.get("texture").isJsonNull()) continue;
                Face face = new Face();
                face.u1 = uv.get(0).getAsFloat();
                face.v1 = uv.get(1).getAsFloat();
                face.u2 = uv.get(2).getAsFloat();
                face.v2 = uv.get(3).getAsFloat();
                if (face.u1 == face.u2 || face.v1 == face.v2) continue;
                face.texture = textureIndex(fo);
                if (fo.has("rotation") && fo.get("rotation").isJsonPrimitive()) {
                    try {
                        face.rotation = fo.get("rotation").getAsInt();
                    } catch (Exception ignored) {
                    }
                }
                cube.faces.put(fe.getKey(), face);
            }
        }
        return cube;
    }

    private static Mesh parseMesh(JsonObject obj) {
        if (!obj.has("vertices") || !obj.get("vertices").isJsonObject()) return null;
        Mesh mesh = new Mesh();
        float[] origin = vec3(obj.get("origin"));
        if (origin != null) mesh.origin = origin;
        mesh.rotation = vec3(obj.get("rotation"));

        Map<String, float[]> vertices = new HashMap<String, float[]>();
        for (Map.Entry<String, JsonElement> ve : obj.getAsJsonObject("vertices").entrySet()) {
            float[] v = vec3(ve.getValue());
            if (v != null) vertices.put(ve.getKey(), v);
        }
        if (vertices.isEmpty()) return null;

        if (obj.has("faces") && obj.get("faces").isJsonObject()) {
            for (Map.Entry<String, JsonElement> fe : obj.getAsJsonObject("faces").entrySet()) {
                if (!fe.getValue().isJsonObject()) continue;
                JsonObject fo = fe.getValue().getAsJsonObject();
                if (!fo.has("vertices") || !fo.get("vertices").isJsonArray()) continue;
                if (fo.has("texture") && fo.get("texture").isJsonNull()) continue;
                JsonArray keys = fo.getAsJsonArray("vertices");
                int count = keys.size();
                if (count < 3) continue;
                if (count > 4) count = 4;
                JsonObject uvObj = fo.has("uv") && fo.get("uv").isJsonObject()
                        ? fo.getAsJsonObject("uv") : null;

                float[][] pos = new float[count][];
                float[][] uv = new float[count][];
                boolean ok = true;
                for (int i = 0; i < count; i++) {
                    String key = keys.get(i).getAsString();
                    float[] v = vertices.get(key);
                    if (v == null) {
                        ok = false;
                        break;
                    }
                    pos[i] = v;
                    float[] coord = {0f, 0f};
                    if (uvObj != null && uvObj.has(key) && uvObj.get(key).isJsonArray()) {
                        JsonArray arr = uvObj.getAsJsonArray(key);
                        if (arr.size() >= 2) {
                            coord[0] = arr.get(0).getAsFloat();
                            coord[1] = arr.get(1).getAsFloat();
                        }
                    }
                    uv[i] = coord;
                }
                if (!ok) continue;

                MeshFace face = new MeshFace();
                face.texture = textureIndex(fo);
                face.pos = pos;
                face.uv = uv;
                mesh.faces.add(face);
            }
        }
        return mesh.faces.isEmpty() ? null : mesh;
    }

    private static int textureIndex(JsonObject face) {
        if (!face.has("texture") || !face.get("texture").isJsonPrimitive()) return 0;
        try {
            return face.get("texture").getAsInt();
        } catch (Exception e) {
            return 0;
        }
    }

    private static void attach(Bone bone, String uuid,
                               Map<String, Cube> cubesByUuid, Map<String, Mesh> meshesByUuid) {
        Cube cube = cubesByUuid.remove(uuid);
        if (cube != null) {
            bone.cubes.add(cube);
            return;
        }
        Mesh mesh = meshesByUuid.remove(uuid);
        if (mesh != null) {
            bone.meshes.add(mesh);
        }
    }

    private static Bone parseBone(JsonObject obj,
                                  Map<String, Cube> cubesByUuid, Map<String, Mesh> meshesByUuid) {
        Bone bone = new Bone();
        if (obj.has("name")) bone.name = obj.get("name").getAsString();
        float[] origin = vec3(obj.get("origin"));
        if (origin != null) bone.origin = origin;
        bone.rotation = vec3(obj.get("rotation"));
        if (obj.has("visibility")) {
            bone.visible = asBool(obj.get("visibility"), true);
        }
        if (obj.has("export")) {
            bone.visible = bone.visible && asBool(obj.get("export"), true);
        }
        if (obj.has("children") && obj.get("children").isJsonArray()) {
            for (JsonElement child : obj.getAsJsonArray("children")) {
                if (child.isJsonPrimitive()) {
                    attach(bone, child.getAsString(), cubesByUuid, meshesByUuid);
                } else if (child.isJsonObject()) {
                    Bone childBone = parseBone(child.getAsJsonObject(), cubesByUuid, meshesByUuid);
                    if (childBone != null) bone.children.add(childBone);
                }
            }
        }
        return bone;
    }

    private static boolean asBool(JsonElement el, boolean def) {
        if (el == null || el.isJsonNull() || !el.isJsonPrimitive()) return def;
        try {
            return el.getAsBoolean();
        } catch (Exception e) {
            try {
                return el.getAsInt() != 0;
            } catch (Exception ignored) {
                return def;
            }
        }
    }

    private static float[] vec3(JsonElement el) {
        if (el == null || !el.isJsonArray()) return null;
        JsonArray arr = el.getAsJsonArray();
        if (arr.size() < 3) return null;
        return new float[]{arr.get(0).getAsFloat(), arr.get(1).getAsFloat(), arr.get(2).getAsFloat()};
    }
}
