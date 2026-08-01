package xd.harm.utils.figura;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3f;
import org.lwjgl.opengl.GL11;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * FiguraLite-рендер .bbmodel для Harmony 1.16.5.
 *
 * Что нового в v3:
 *  - рисуются MESH-элементы (треугольники/четырёхугольники). Без этого коса,
 *    плащи, зонты и щиты вообще не имели геометрии — отсюда было
 *    «Не удалось загрузить»;
 *  - UV берутся от размера конкретной текстуры (BbModel#uvWidth/uvHeight),
 *    а не от общего resolution — исчезает розово-фиолетовая каша;
 *  - mesh-грани рисуются двусторонне, чтобы ткань не пропадала с изнанки.
 *
 * Осталось из v2: customizations из avatar.json (visible=false), габариты для
 * автомасштаба и постановки на землю, автовыпрямление T-позы.
 *
 * Анимации не поддерживаются.
 */
public final class BbModelRenderer {

    private final BbModel model;
    private final String modelId;
    /** Фолдер пака — нужен, чтобы читать его собственную настройку «Исправлять T-позу». */
    private final String packFolder;

    private final Map<BbModel.Bone, Boolean> hidden = new IdentityHashMap<BbModel.Bone, Boolean>();
    private final Map<BbModel.Bone, Float> armCorrection = new IdentityHashMap<BbModel.Bone, Float>();
    /** Кубы-заглушки ванильного скина — их не надо рисовать поверх игрока. */
    private final Map<BbModel.Cube, Boolean> skippedCubes = new IdentityHashMap<BbModel.Cube, Boolean>();
    /** Сдвиг кости в пикселях модели (чтобы вставить слой в корпус). */
    private final Map<BbModel.Bone, float[]> boneOffset = new IdentityHashMap<BbModel.Bone, float[]>();
    private final List<BbModel.Bone> altStateBones = new ArrayList<BbModel.Bone>();
    private boolean hideSkinCubes;

    /** Группы-варианты одного и того же элемента (состояния экрана и т.п.). */
    private static final String[] ALT_STATE_BONES = new String[]{
            "static", "static1", "static2", "static3", "background", "blank", "glitch", "noise",
            "bsod", "error", "screen2", "screen3", "screensaver", "offscreen"};

    /** Коробки ванильного скина: голова, тело, руки, ноги. */
    private static final float[][] VANILLA_BOXES = new float[][]{
            {-4f, 24f, -4f, 4f, 32f, 4f},
            {-4f, 12f, -2f, 4f, 24f, 2f},
            {-8f, 12f, -2f, -4f, 24f, 2f},
            {4f, 12f, -2f, 8f, 24f, 2f},
            {-4f, 0f, -2f, 0f, 12f, 2f},
            {0f, 0f, -2f, 4f, 12f, 2f},
            {-2f, 0f, -2f, 2f, 12f, 2f}};

    private final List<ResourceLocation> textureLocations = new ArrayList<ResourceLocation>();
    private boolean uploaded;
    private boolean closed;

    /** Если задано — рисуется только эта ветка (вариант модели). */
    private volatile BbModel.Bone activeBone;
    private float[] fullBounds;

    private float minX = Float.MAX_VALUE;
    private float minY = Float.MAX_VALUE;
    private float minZ = Float.MAX_VALUE;
    private float maxX = -Float.MAX_VALUE;
    private float maxY = -Float.MAX_VALUE;
    private float maxZ = -Float.MAX_VALUE;
    private boolean hasGeometry;

    public BbModelRenderer(BbModel model) {
        this(model, "main", null);
    }

    public BbModelRenderer(BbModel model, String modelId, FiguraAvatarLibrary.Entry entry) {
        this.model = model;
        this.modelId = modelId == null ? "main" : modelId;
        this.packFolder = entry == null ? null : entry.folder;
        prepare(entry);
        this.fullBounds = new float[]{minX, minY, minZ, maxX, maxY, maxZ};
    }

    /** Углы в градусах. */
    public static final class Pose {
        public float headPitch;
        public float headYaw;
        public float rightArm;
        public float leftArm;
        public float rightLeg;
        public float leftLeg;
        public boolean crouching;
    }

    // ------------------------------------------------------------- Подготовка

    private void prepare(FiguraAvatarLibrary.Entry entry) {
        // Аксессуары / петы / оружие / крылья не должны дублировать скин — он и так виден.
        this.hideSkinCubes = entry != null && entry.section != FiguraAvatarLibrary.Section.AVATARS;
        for (int i = 0; i < model.roots.size(); i++) {
            BbModel.Bone root = model.roots.get(i);
            walk(root, root.name == null ? "" : root.name, entry, false);
        }
        resolveAltStates();
    }

    /**
     * В моделях вроде Computer Head лежит несколько вариантов картинки на экране
     * (Static1, Static2, Background, Blank), и автор отнёс их далеко вперёд — скрипт пака
     * сам выбирал нужный и ставил его на место. Мы делаем то же: оставляем один слой
     * и плотно вставляем его в корпус, остальные прячем.
     */
    private void resolveAltStates() {
        if (altStateBones.isEmpty() || !hasGeometry) {
            return;
        }
        BbModel.Bone keeper = null;
        for (int i = 0; i < altStateBones.size(); i++) {
            String simple = simplify(altStateBones.get(i).name);
            if (simple.equals("background")) {
                keeper = altStateBones.get(i);
                break;
            }
        }
        if (keeper == null) {
            keeper = altStateBones.get(0);
        }
        unhideSubtree(keeper);

        float[] bb = subtreeBounds(keeper);
        if (bb == null) {
            return;
        }
        // Границы корпуса без этих слоёв уже посчитаны в minX..maxZ.
        float dx = 0f;
        float dy = 0f;
        float dz = 0f;
        if (bb[2] < minZ - 0.5f || bb[5] > maxZ + 0.5f) {
            dz = (minZ + 0.05f) - bb[2];   // лицевой стороной в переднюю стенку корпуса
        }
        if (bb[0] < minX - 0.5f || bb[3] > maxX + 0.5f) {
            dx = ((minX + maxX) / 2f) - ((bb[0] + bb[3]) / 2f);
        }
        if (bb[1] < minY - 0.5f || bb[4] > maxY + 0.5f) {
            dy = ((minY + maxY) / 2f) - ((bb[1] + bb[4]) / 2f);
        }
        if (dx != 0f || dy != 0f || dz != 0f) {
            boneOffset.put(keeper, new float[]{dx, dy, dz});
        }
    }

    private void unhideSubtree(BbModel.Bone bone) {
        if (bone == null) {
            return;
        }
        if (bone.visible) {
            hidden.put(bone, Boolean.FALSE);
        }
        for (int i = 0; i < bone.children.size(); i++) {
            unhideSubtree(bone.children.get(i));
        }
    }

    private void walk(BbModel.Bone bone, String path, FiguraAvatarLibrary.Entry entry, boolean parentHidden) {
        boolean isHidden = parentHidden || !bone.visible;
        if (!isHidden && entry != null && path != null && !path.isEmpty()) {
            isHidden = entry.isBoneHidden(modelId, path);
        }
        if (!isHidden && isAlternateState(bone.name)) {
            // вариант одной и той же детали — разберёмся с ним в resolveAltStates()
            altStateBones.add(bone);
            isHidden = true;
        }
        hidden.put(bone, Boolean.valueOf(isHidden));

        if (!isHidden) {
            for (int i = 0; i < bone.cubes.size(); i++) {
                BbModel.Cube cube = bone.cubes.get(i);
                if (hideSkinCubes && isVanillaSkinCube(cube)) {
                    skippedCubes.put(cube, Boolean.TRUE);
                    continue;
                }
                expand(cube);
            }
            for (int i = 0; i < bone.meshes.size(); i++) {
                expandMesh(bone.meshes.get(i));
            }
            Float correction = detectArmCorrection(bone);
            if (correction != null) {
                armCorrection.put(bone, correction);
            }
        }

        for (int i = 0; i < bone.children.size(); i++) {
            BbModel.Bone child = bone.children.get(i);
            String childName = child.name == null ? "" : child.name;
            String childPath = path == null || path.isEmpty() ? childName : path + "." + childName;
            walk(child, childPath, entry, isHidden);
        }
    }

    private void expand(BbModel.Cube cube) {
        if (cube.from == null || cube.to == null) {
            return;
        }
        float inf = cube.inflate;
        expandPoint(Math.min(cube.from[0], cube.to[0]) - inf,
                Math.min(cube.from[1], cube.to[1]) - inf,
                Math.min(cube.from[2], cube.to[2]) - inf);
        expandPoint(Math.max(cube.from[0], cube.to[0]) + inf,
                Math.max(cube.from[1], cube.to[1]) + inf,
                Math.max(cube.from[2], cube.to[2]) + inf);
    }

    private void expandMesh(BbModel.Mesh mesh) {
        if (mesh == null) return;
        float ox = mesh.origin == null ? 0f : mesh.origin[0];
        float oy = mesh.origin == null ? 0f : mesh.origin[1];
        float oz = mesh.origin == null ? 0f : mesh.origin[2];
        for (int i = 0; i < mesh.faces.size(); i++) {
            BbModel.MeshFace face = mesh.faces.get(i);
            if (face.pos == null) continue;
            for (int v = 0; v < face.pos.length; v++) {
                float[] p = face.pos[v];
                if (p == null) continue;
                expandPoint(ox + p[0], oy + p[1], oz + p[2]);
            }
        }
    }

    private void expandPoint(float x, float y, float z) {
        if (x < minX) minX = x;
        if (y < minY) minY = y;
        if (z < minZ) minZ = z;
        if (x > maxX) maxX = x;
        if (y > maxY) maxY = y;
        if (z > maxZ) maxZ = z;
        hasGeometry = true;
    }

    /**
     * Определяет, насколько надо довернуть руку вокруг Z, чтобы она смотрела вниз.
     * Большинство Figura-аватаров смоделированы в T-позе и выпрямляются скриптом.
     */
    private Float detectArmCorrection(BbModel.Bone bone) {
        String name = simplify(bone.name);
        boolean isArm = name.equals("rightarm") || name.equals("leftarm")
                || name.equals("armright") || name.equals("armleft")
                || name.equals("rarm") || name.equals("larm");
        if (!isArm) {
            return null;
        }
        float[] bb = subtreeBounds(bone);
        if (bb == null) {
            return null;
        }
        float cx = (bb[0] + bb[3]) / 2f - bone.origin[0];
        float cy = (bb[1] + bb[4]) / 2f - bone.origin[1];
        if (Math.abs(cx) < 0.05f && Math.abs(cy) < 0.05f) {
            return null;
        }
        double angle = Math.toDegrees(Math.atan2(cy, cx));
        double theta = -90.0 - angle;
        while (theta <= -180.0) theta += 360.0;
        while (theta > 180.0) theta -= 360.0;
        if (Math.abs(theta) < 5.0 || Math.abs(theta) > 120.0) {
            return null;
        }
        return Float.valueOf((float) theta);
    }

    private float[] subtreeBounds(BbModel.Bone bone) {
        float[] bb = new float[]{Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE,
                -Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
        boolean any = collectBounds(bone, bb);
        return any ? bb : null;
    }

    private boolean collectBounds(BbModel.Bone bone, float[] bb) {
        boolean any = false;
        for (int i = 0; i < bone.cubes.size(); i++) {
            BbModel.Cube cube = bone.cubes.get(i);
            if (cube.from == null || cube.to == null) continue;
            for (int axis = 0; axis < 3; axis++) {
                float lo = Math.min(cube.from[axis], cube.to[axis]);
                float hi = Math.max(cube.from[axis], cube.to[axis]);
                if (lo < bb[axis]) bb[axis] = lo;
                if (hi > bb[axis + 3]) bb[axis + 3] = hi;
            }
            any = true;
        }
        for (int i = 0; i < bone.meshes.size(); i++) {
            BbModel.Mesh mesh = bone.meshes.get(i);
            float[] origin = mesh.origin == null ? new float[]{0f, 0f, 0f} : mesh.origin;
            for (int f = 0; f < mesh.faces.size(); f++) {
                float[][] pos = mesh.faces.get(f).pos;
                if (pos == null) continue;
                for (int v = 0; v < pos.length; v++) {
                    float[] p = pos[v];
                    if (p == null) continue;
                    for (int axis = 0; axis < 3; axis++) {
                        float value = origin[axis] + p[axis];
                        if (value < bb[axis]) bb[axis] = value;
                        if (value > bb[axis + 3]) bb[axis + 3] = value;
                    }
                    any = true;
                }
            }
        }
        for (int i = 0; i < bone.children.size(); i++) {
            if (collectBounds(bone.children.get(i), bb)) {
                any = true;
            }
        }
        return any;
    }

    private static String simplify(String raw) {
        if (raw == null) return "";
        return raw.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "").replace(" ", "");
    }

    // ------------------------------------------------- Варианты внутри модели

    private void collectBones(BbModel.Bone bone, List<BbModel.Bone> out) {
        out.add(bone);
        for (int i = 0; i < bone.children.size(); i++) {
            collectBones(bone.children.get(i), out);
        }
    }

    private List<BbModel.Bone> allBones() {
        List<BbModel.Bone> out = new ArrayList<BbModel.Bone>();
        for (int i = 0; i < model.roots.size(); i++) {
            collectBones(model.roots.get(i), out);
        }
        return out;
    }

    private boolean isDescendant(BbModel.Bone parent, BbModel.Bone child) {
        for (int i = 0; i < parent.children.size(); i++) {
            BbModel.Bone kid = parent.children.get(i);
            if (kid == child || isDescendant(kid, child)) {
                return true;
            }
        }
        return false;
    }

    private static boolean looksLikeWeaponGroup(String name) {
        return name.startsWith("weapon") || name.startsWith("item") || name.startsWith("held")
                || name.startsWith("scythe") || name.startsWith("sword") || name.startsWith("blade")
                || name.startsWith("katana") || name.startsWith("axe") || name.startsWith("back")
                || name.startsWith("hand");
    }

    /**
     * В паках типа Cool scythe лежит СРАЗУ ДВЕ копии оружия: одна нарисована
     * в координатах игрока (висит на спине), вторая — вокруг точки хвата (для руки).
     * Оригинальный Figura-скрипт прячет лишнюю; без скрипта видны обе.
     *
     * @return имя нужной ветки или null, если копия всего одна.
     */
    public String pickWeaponBone(boolean hand) {
        List<BbModel.Bone> bones = allBones();
        List<BbModel.Bone> candidates = new ArrayList<BbModel.Bone>();
        for (int i = 0; i < bones.size(); i++) {
            BbModel.Bone bone = bones.get(i);
            Boolean isHidden = hidden.get(bone);
            if (isHidden != null && isHidden.booleanValue()) {
                continue;
            }
            String name = simplify(bone.name);
            if (name.isEmpty() || !looksLikeWeaponGroup(name)) {
                continue;
            }
            if (subtreeBounds(bone) == null) {
                continue;
            }
            candidates.add(bone);
        }

        // убираем вложенные друг в друга
        List<BbModel.Bone> tops = new ArrayList<BbModel.Bone>();
        for (int i = 0; i < candidates.size(); i++) {
            BbModel.Bone bone = candidates.get(i);
            boolean nested = false;
            for (int j = 0; j < candidates.size(); j++) {
                if (i != j && isDescendant(candidates.get(j), bone)) {
                    nested = true;
                    break;
                }
            }
            if (!nested) {
                tops.add(bone);
            }
        }
        if (tops.size() < 2) {
            return null;
        }

        // Копия для руки нарисована вокруг нуля (есть отрицательный Y), копия для спины — высоко.
        BbModel.Bone best = null;
        float bestValue = 0f;
        for (int i = 0; i < tops.size(); i++) {
            float[] bb = subtreeBounds(tops.get(i));
            if (bb == null) {
                continue;
            }
            float value = bb[1];
            if (best == null || (hand ? value < bestValue : value > bestValue)) {
                best = tops.get(i);
                bestValue = value;
            }
        }
        return best == null ? null : best.name;
    }

    /** Рисовать только эту ветку (габариты тоже пересчитываются по ней). */
    public boolean selectBone(String name) {
        if (name == null) {
            selectAll();
            return false;
        }
        String wanted = simplify(name);
        List<BbModel.Bone> bones = allBones();
        for (int i = 0; i < bones.size(); i++) {
            BbModel.Bone bone = bones.get(i);
            if (!simplify(bone.name).equals(wanted)) {
                continue;
            }
            float[] bb = subtreeBounds(bone);
            if (bb == null) {
                continue;
            }
            activeBone = bone;
            minX = bb[0];
            minY = bb[1];
            minZ = bb[2];
            maxX = bb[3];
            maxY = bb[4];
            maxZ = bb[5];
            return true;
        }
        return false;
    }

    /** Вернуть отрисовку всей модели. */
    public void selectAll() {
        activeBone = null;
        if (fullBounds != null) {
            minX = fullBounds[0];
            minY = fullBounds[1];
            minZ = fullBounds[2];
            maxX = fullBounds[3];
            maxY = fullBounds[4];
            maxZ = fullBounds[5];
        }
    }

    // -------------------------------------------------------------- Габариты

    public boolean hasGeometry() {
        return hasGeometry;
    }

    /** Нижняя точка модели в блоках. */
    public float getMinY() {
        return hasGeometry ? minY / 16f : 0f;
    }

    /** Верхняя точка модели в блоках. */
    public float getMaxY() {
        return hasGeometry ? maxY / 16f : 0f;
    }

    public float getHeight() {
        return hasGeometry ? (maxY - minY) / 16f : 0f;
    }

    public float getWidth() {
        return hasGeometry ? (maxX - minX) / 16f : 0f;
    }

    public float getDepth() {
        return hasGeometry ? (maxZ - minZ) / 16f : 0f;
    }

    public float getMinX() {
        return hasGeometry ? minX / 16f : 0f;
    }

    public float getMaxX() {
        return hasGeometry ? maxX / 16f : 0f;
    }

    public float getMinZ() {
        return hasGeometry ? minZ / 16f : 0f;
    }

    public float getMaxZ() {
        return hasGeometry ? maxZ / 16f : 0f;
    }

    // --------------------------------------------------------------- Рендер

    /** Загрузка текстур в GPU — только из render-потока, поэтому лениво в render(). */
    private void ensureUploaded() {
        if (uploaded) return;
        uploaded = true;
        for (int t = 0; t < model.textures.size(); t++) {
            byte[] png = model.textures.get(t);
            ResourceLocation loc = null;
            if (png != null) {
                try {
                    NativeImage image = NativeImage.read(new ByteArrayInputStream(png));
                    DynamicTexture texture = new DynamicTexture(image);
                    loc = Minecraft.getInstance().getTextureManager()
                            .getDynamicTextureLocation("figura_lite_" + System.identityHashCode(this) + "_" + t, texture);
                } catch (Exception ignored) {
                }
            }
            textureLocations.add(loc);
        }
    }

    public void render(MatrixStack ms, Pose pose) {
        if (closed) return;
        ensureUploaded();
        for (int tex = 0; tex < textureLocations.size(); tex++) {
            ResourceLocation loc = textureLocations.get(tex);
            if (loc == null) continue;
            Minecraft.getInstance().getTextureManager().bindTexture(loc);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);
            Tessellator tess = Tessellator.getInstance();
            BufferBuilder buf = tess.getBuffer();
            buf.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            BbModel.Bone only = activeBone;
            if (only != null) {
                renderBone(ms, buf, only, pose, tex);
            } else {
                for (int i = 0; i < model.roots.size(); i++) {
                    renderBone(ms, buf, model.roots.get(i), pose, tex);
                }
            }
            tess.draw();
        }
    }

    private void renderBone(MatrixStack ms, BufferBuilder buf, BbModel.Bone bone, Pose pose, int tex) {
        Boolean isHidden = hidden.get(bone);
        if (isHidden != null && isHidden.booleanValue()) {
            return;
        }
        ms.push();
        float[] off = boneOffset.get(bone);
        if (off != null) {
            ms.translate(off[0] / 16f, off[1] / 16f, off[2] / 16f);
        }
        float ox = bone.origin[0] / 16f;
        float oy = bone.origin[1] / 16f;
        float oz = bone.origin[2] / 16f;
        ms.translate(ox, oy, oz);
        if (bone.rotation != null) {
            if (bone.rotation[2] != 0f) ms.rotate(Vector3f.ZP.rotationDegrees(bone.rotation[2]));
            if (bone.rotation[1] != 0f) ms.rotate(Vector3f.YP.rotationDegrees(bone.rotation[1]));
            if (bone.rotation[0] != 0f) ms.rotate(Vector3f.XP.rotationDegrees(bone.rotation[0]));
        }
        Float correction = armCorrection.get(bone);
        if (correction != null && fixTPose()) {
            ms.rotate(Vector3f.ZP.rotationDegrees(correction.floatValue()));
        }
        applyPose(ms, bone.name, pose);
        ms.translate(-ox, -oy, -oz);
        for (int i = 0; i < bone.cubes.size(); i++) {
            BbModel.Cube cube = bone.cubes.get(i);
            if (skippedCubes.containsKey(cube)) {
                continue;
            }
            renderCube(ms, buf, cube, tex);
        }
        for (int i = 0; i < bone.meshes.size(); i++) {
            renderMesh(ms, buf, bone.meshes.get(i), tex);
        }
        for (int i = 0; i < bone.children.size(); i++) {
            renderBone(ms, buf, bone.children.get(i), pose, tex);
        }
        ms.pop();
    }

    /** Настройка «Исправлять T-позу» берётся у конкретного пака (ПКМ по карточке). */
    private boolean fixTPose() {
        try {
            if (packFolder != null) {
                return FiguraPackSettings.getFixTPose(packFolder);
            }
            return FiguraPackSettings.isFixTPoseActive();
        } catch (Throwable ignored) {
            return true;
        }
    }

    /** Кости вроде Static1 / Background / Blank — это альтернативные состояния, а не детали. */
    private static boolean isAlternateState(String rawName) {
        String name = simplify(rawName);
        if (name.isEmpty()) {
            return false;
        }
        for (int i = 0; i < ALT_STATE_BONES.length; i++) {
            if (name.equals(ALT_STATE_BONES[i])) {
                return true;
            }
        }
        return false;
    }

    /** Куб точно повторяет коробку ванильного скина (голова / тело / руки / ноги). */
    private static boolean isVanillaSkinCube(BbModel.Cube cube) {
        if (cube == null || cube.from == null || cube.to == null
                || cube.from.length < 3 || cube.to.length < 3) {
            return false;
        }
        float x0 = Math.min(cube.from[0], cube.to[0]);
        float y0 = Math.min(cube.from[1], cube.to[1]);
        float z0 = Math.min(cube.from[2], cube.to[2]);
        float x1 = Math.max(cube.from[0], cube.to[0]);
        float y1 = Math.max(cube.from[1], cube.to[1]);
        float z1 = Math.max(cube.from[2], cube.to[2]);
        for (int i = 0; i < VANILLA_BOXES.length; i++) {
            float[] box = VANILLA_BOXES[i];
            if (near(x0, box[0]) && near(y0, box[1]) && near(z0, box[2])
                    && near(x1, box[3]) && near(y1, box[4]) && near(z1, box[5])) {
                return true;
            }
        }
        return false;
    }

    private static boolean near(float a, float b) {
        return Math.abs(a - b) < 0.05f;
    }

    private void applyPose(MatrixStack ms, String rawName, Pose pose) {
        if (rawName == null || pose == null) return;
        String name = simplify(rawName);
        if (name.isEmpty()) return;
        if (name.equals("head") || name.equals("heads")) {
            ms.rotate(Vector3f.YP.rotationDegrees(-pose.headYaw));
            ms.rotate(Vector3f.XP.rotationDegrees(-pose.headPitch));
        } else if (name.equals("rightarm") || name.equals("armright") || name.equals("rarm")) {
            ms.rotate(Vector3f.XP.rotationDegrees(pose.rightArm));
        } else if (name.equals("leftarm") || name.equals("armleft") || name.equals("larm")) {
            ms.rotate(Vector3f.XP.rotationDegrees(pose.leftArm));
        } else if (name.equals("rightleg") || name.equals("legright") || name.equals("rleg")) {
            ms.rotate(Vector3f.XP.rotationDegrees(pose.rightLeg));
        } else if (name.equals("leftleg") || name.equals("legleft") || name.equals("lleg")) {
            ms.rotate(Vector3f.XP.rotationDegrees(pose.leftLeg));
        } else if (pose.crouching && (name.equals("upperbody") || name.equals("body"))) {
            ms.rotate(Vector3f.XP.rotationDegrees(28.6f));
        }
    }

    private void renderCube(MatrixStack ms, BufferBuilder buf, BbModel.Cube cube, int tex) {
        if (cube.from == null || cube.to == null) return;
        boolean rotated = cube.rotation != null && cube.origin != null
                && (cube.rotation[0] != 0f || cube.rotation[1] != 0f || cube.rotation[2] != 0f);
        if (rotated) {
            ms.push();
            float ox = cube.origin[0] / 16f;
            float oy = cube.origin[1] / 16f;
            float oz = cube.origin[2] / 16f;
            ms.translate(ox, oy, oz);
            if (cube.rotation[2] != 0f) ms.rotate(Vector3f.ZP.rotationDegrees(cube.rotation[2]));
            if (cube.rotation[1] != 0f) ms.rotate(Vector3f.YP.rotationDegrees(cube.rotation[1]));
            if (cube.rotation[0] != 0f) ms.rotate(Vector3f.XP.rotationDegrees(cube.rotation[0]));
            ms.translate(-ox, -oy, -oz);
        }

        float inf = cube.inflate / 16f;
        float x0 = Math.min(cube.from[0], cube.to[0]) / 16f - inf;
        float y0 = Math.min(cube.from[1], cube.to[1]) / 16f - inf;
        float z0 = Math.min(cube.from[2], cube.to[2]) / 16f - inf;
        float x1 = Math.max(cube.from[0], cube.to[0]) / 16f + inf;
        float y1 = Math.max(cube.from[1], cube.to[1]) / 16f + inf;
        float z1 = Math.max(cube.from[2], cube.to[2]) / 16f + inf;
        Matrix4f m = ms.getLast().getMatrix();

        float uvW = model.uvWidth(tex);
        float uvH = model.uvHeight(tex);

        for (Map.Entry<String, BbModel.Face> entry : cube.faces.entrySet()) {
            BbModel.Face f = entry.getValue();
            if (f.texture != tex) continue;
            float u1 = f.u1 / uvW;
            float v1 = f.v1 / uvH;
            float u2 = f.u2 / uvW;
            float v2 = f.v2 / uvH;
            String side = entry.getKey();
            if ("north".equals(side)) {
                v(buf, m, x1, y1, z0, u1, v1); v(buf, m, x0, y1, z0, u2, v1);
                v(buf, m, x0, y0, z0, u2, v2); v(buf, m, x1, y0, z0, u1, v2);
            } else if ("south".equals(side)) {
                v(buf, m, x0, y1, z1, u1, v1); v(buf, m, x1, y1, z1, u2, v1);
                v(buf, m, x1, y0, z1, u2, v2); v(buf, m, x0, y0, z1, u1, v2);
            } else if ("west".equals(side)) {
                v(buf, m, x0, y1, z0, u1, v1); v(buf, m, x0, y1, z1, u2, v1);
                v(buf, m, x0, y0, z1, u2, v2); v(buf, m, x0, y0, z0, u1, v2);
            } else if ("east".equals(side)) {
                v(buf, m, x1, y1, z1, u1, v1); v(buf, m, x1, y1, z0, u2, v1);
                v(buf, m, x1, y0, z0, u2, v2); v(buf, m, x1, y0, z1, u1, v2);
            } else if ("up".equals(side)) {
                v(buf, m, x0, y1, z0, u1, v1); v(buf, m, x1, y1, z0, u2, v1);
                v(buf, m, x1, y1, z1, u2, v2); v(buf, m, x0, y1, z1, u1, v2);
            } else if ("down".equals(side)) {
                v(buf, m, x0, y0, z1, u1, v1); v(buf, m, x1, y0, z1, u2, v1);
                v(buf, m, x1, y0, z0, u2, v2); v(buf, m, x0, y0, z0, u1, v2);
            }
        }

        if (rotated) {
            ms.pop();
        }
    }

    /**
     * Рисует mesh-элемент. Вершины хранятся относительно origin элемента,
     * поэтому сначала переезжаем в origin и там же применяем поворот.
     * Каждая грань рисуется с двух сторон — у ткани и лезвий нормали обычно
     * разнонаправленные, иначе половина модели пропадает.
     */
    private void renderMesh(MatrixStack ms, BufferBuilder buf, BbModel.Mesh mesh, int tex) {
        if (mesh == null || mesh.faces.isEmpty()) return;

        ms.push();
        float ox = (mesh.origin == null ? 0f : mesh.origin[0]) / 16f;
        float oy = (mesh.origin == null ? 0f : mesh.origin[1]) / 16f;
        float oz = (mesh.origin == null ? 0f : mesh.origin[2]) / 16f;
        ms.translate(ox, oy, oz);
        if (mesh.rotation != null) {
            if (mesh.rotation[2] != 0f) ms.rotate(Vector3f.ZP.rotationDegrees(mesh.rotation[2]));
            if (mesh.rotation[1] != 0f) ms.rotate(Vector3f.YP.rotationDegrees(mesh.rotation[1]));
            if (mesh.rotation[0] != 0f) ms.rotate(Vector3f.XP.rotationDegrees(mesh.rotation[0]));
        }

        Matrix4f m = ms.getLast().getMatrix();
        float uvW = model.uvWidth(tex);
        float uvH = model.uvHeight(tex);

        for (int i = 0; i < mesh.faces.size(); i++) {
            BbModel.MeshFace face = mesh.faces.get(i);
            if (face.texture != tex || face.pos == null || face.uv == null) continue;
            int count = Math.min(face.pos.length, face.uv.length);
            if (count < 3) continue;

            float[] p0 = face.pos[0];
            float[] p1 = face.pos[1];
            float[] p2 = face.pos[2];
            float[] p3 = count >= 4 ? face.pos[3] : p2;
            float[] t0 = face.uv[0];
            float[] t1 = face.uv[1];
            float[] t2 = face.uv[2];
            float[] t3 = count >= 4 ? face.uv[3] : t2;
            if (p0 == null || p1 == null || p2 == null || p3 == null) continue;

            // лицевая сторона
            mv(buf, m, p0, t0, uvW, uvH);
            mv(buf, m, p1, t1, uvW, uvH);
            mv(buf, m, p2, t2, uvW, uvH);
            mv(buf, m, p3, t3, uvW, uvH);
            // изнанка
            mv(buf, m, p3, t3, uvW, uvH);
            mv(buf, m, p2, t2, uvW, uvH);
            mv(buf, m, p1, t1, uvW, uvH);
            mv(buf, m, p0, t0, uvW, uvH);
        }

        ms.pop();
    }

    private void mv(BufferBuilder buf, Matrix4f m, float[] pos, float[] uv, float uvW, float uvH) {
        buf.pos(m, pos[0] / 16f, pos[1] / 16f, pos[2] / 16f)
                .tex(uv[0] / uvW, uv[1] / uvH)
                .endVertex();
    }

    private void v(BufferBuilder buf, Matrix4f m, float x, float y, float z, float u, float v) {
        buf.pos(m, x, y, z).tex(u, v).endVertex();
    }

    /** Удаляет загруженные текстуры. Вызывать только из render-потока. */
    public void close() {
        if (closed) return;
        closed = true;
        for (int i = 0; i < textureLocations.size(); i++) {
            ResourceLocation loc = textureLocations.get(i);
            if (loc == null) continue;
            try {
                Minecraft.getInstance().getTextureManager().deleteTexture(loc);
            } catch (Exception ignored) {
            }
        }
        textureLocations.clear();
    }
}
