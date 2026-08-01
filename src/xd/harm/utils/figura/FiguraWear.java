package xd.harm.utils.figura;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.PointOfView;
import net.minecraft.item.AxeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.TridentItem;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3f;
import xd.harm.Harmony;
import xd.harm.events.render.EventRender3D;
import xd.harm.modules.impl.render.FiguraCosmetic;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Движок FiguraLite: одевает на игрока ровно один пак из figura/avatars.
 *
 * v4:
 *   Аватары     — заменяют игрока целиком.
 *   Петы       — у ног или на голове (FiguraPackSettings.PetPlacement), скин виден.
 *   Аксессуары / Крылья — поверх тела 1:1, скин виден.
 *   Оружие      — в руке, на спине или Авто (на спине, а когда в руках
 *                  меч/топор/трезубец — перепрыгивает в руку), скин виден.
 */
public final class FiguraWear {

    private static final FiguraWear INSTANCE = new FiguraWear();
    private static final Object LOCK = new Object();

    /** Как рисовать надетый пак. */
    public enum Mode {
        AVATAR,
        PET,
        BODY,
        WEAPON
    }

    // Опорные точки ванильной модели (в блоках, отсчёт от ног).
    private static final float SHOULDER_X = -5f / 16f;
    private static final float SHOULDER_Y = 22f / 16f;
    private static final float ARM_LENGTH = 10f / 16f;
    private static final float NECK_Y = 24f / 16f;
    private static final float HEAD_TOP = 31f / 16f;

    private static volatile String current;
    private static volatile FiguraAvatarLibrary.Entry currentEntry;
    private static volatile List<BbModelRenderer> renderers = new ArrayList<BbModelRenderer>();
    private static final List<BbModelRenderer> DISPOSE_QUEUE = new ArrayList<BbModelRenderer>();

    private static volatile boolean loading;
    private static volatile String status = "";
    private static volatile boolean registered;
    private static volatile boolean restored;

    private static volatile Mode mode = Mode.AVATAR;
    private static volatile float fitScale = 1f;
    private static volatile float groundOffset = 0f;
    private static volatile float petScale = 1f;
    private static volatile float headPetScale = 1f;
    /** true — надета копия пета из «Аксессуаров»: сидит на голове. */
    private static volatile boolean petOnHead = false;
    /** true — пак уже слеплен в координатах игрока (сидит на голове сам). */
    private static volatile boolean headPetAuthored = false;

    private static volatile boolean handHeld;
    private static volatile float handScale = 1f;
    private static volatile float modelCenterX = 0f;
    private static volatile float modelCenterY = 0f;
    private static volatile float modelCenterZ = 0f;
    private static volatile float modelMinY = 0f;
    private static volatile boolean weaponLying;

    // Хват для оружия: длинная ось модели и точка рукояти.
    private static volatile int longAxis = 1;      // 0 = X, 1 = Y, 2 = Z
    /** Копия оружия нарисована сразу в координатах игрока (её не надо никуда двигать). */
    private static volatile boolean weaponOnBody;
    /** Какая копия оружия сейчас выбрана: true = для руки. */
    private static volatile Boolean weaponHandSelection;
    private static volatile float gripScale = 1f;
    private static volatile float gripX = 0f;
    private static volatile float gripY = 0f;
    private static volatile float gripZ = 0f;

    private FiguraWear() {
    }

    // ------------------------------------------------------------------ API

    public static void bootstrap() {
        tryRegister();
        CosmeticFeatures.bootstrap();
        if (!restored) {
            restored = true;
            loadSavedSelection();
        }
    }

    public static boolean tryRegister() {
        if (registered) {
            return true;
        }
        try {
            Harmony harmony = Harmony.getInstance();
            if (harmony == null || harmony.getEventBus() == null) {
                return false;
            }
            harmony.getEventBus().register(INSTANCE);
            registered = true;
        } catch (Throwable ignored) {
        }
        return registered;
    }

    /**
     * Имя папки надетого пака, НО только если этот пак заменяет игрока целиком.
     * Этот метод через рефлексию дёргает PlayerRenderer, чтобы понять, прятать ли скин.
     */
    public static String getCurrent() {
        return isPlayerReplaced() ? current : null;
    }

    /** Имя папки надетого пака любой секции или null. Для GUI. */
    public static String getWornFolder() {
        return current;
    }

    public static boolean isPlayerReplaced() {
        return current != null && mode == Mode.AVATAR;
    }

    public static Mode getMode() {
        return mode;
    }

    public static FiguraAvatarLibrary.Entry getCurrentEntry() {
        return currentEntry;
    }

    public static boolean isLoading() {
        return loading;
    }

    public static String getStatus() {
        return status;
    }

    public static boolean isWorn(String folder) {
        String cur = current;
        return cur != null && folder != null && cur.equalsIgnoreCase(folder);
    }

    /** Надет ли хоть один вид этой группы. */
    public static boolean isGroupWorn(FiguraAvatarLibrary.Entry head) {
        if (head == null || current == null) {
            return false;
        }
        if (isWorn(head.folder)) {
            return true;
        }
        for (int i = 0; i < head.variants.size(); i++) {
            if (isWorn(head.variants.get(i).folder)) {
                return true;
            }
        }
        return false;
    }

    public static float getFitScale() {
        return fitScale;
    }

    /** Перенадеть тот же пак заново (после смены вида в настройках). */
    public static void rewear(String folder) {
        if (folder == null) {
            return;
        }
        if (isWorn(folder)) {
            return;
        }
        wear(folder);
    }

    /** Надеть пак. Предыдущий снимается автоматически. */
    public static void wear(String folder) {
        if (folder == null || folder.isEmpty()) {
            takeOff();
            return;
        }
        if (isWorn(folder) && !renderers.isEmpty()) {
            return;
        }
        final FiguraAvatarLibrary.Entry entry = FiguraAvatarLibrary.byFolder(folder);
        if (entry == null) {
            status = "Не найден: " + folder;
            return;
        }
        if (entry.models.isEmpty()) {
            status = "В паке нет .bbmodel: " + entry.name;
            return;
        }

        tryRegister();
        loading = true;
        status = "Загрузка: " + entry.name;

        Thread thread = new Thread(new Runnable() {
            public void run() {
                List<BbModelRenderer> built = new ArrayList<BbModelRenderer>();
                try {
                    FiguraPackSettings.beginBuild(entry.folder);
                    for (Path model : entry.models) {
                        if (!entry.isModelVisible(model)) {
                            continue;
                        }
                        try {
                            BbModel parsed = BbModel.parse(model.toFile());
                            BbModelRenderer renderer = new BbModelRenderer(parsed, entry.modelId(model), entry);
                            if (renderer.hasGeometry()) {
                                built.add(renderer);
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                } catch (Throwable ignored) {
                } finally {
                    FiguraPackSettings.endBuild();
                }

                if (built.isEmpty()) {
                    loading = false;
                    status = "Не удалось загрузить: " + entry.name;
                    return;
                }

                synchronized (LOCK) {
                    DISPOSE_QUEUE.addAll(renderers);
                    renderers = built;
                    weaponHandSelection = null;
                    current = entry.folder;
                    currentEntry = entry;
                }
                recomputeFit(built, entry);
                save(entry.folder);
                loading = false;
                status = "Надет: " + entry.name;
            }
        }, "Harmony-FiguraLite-Loader");
        thread.setDaemon(true);
        thread.start();
    }

    /** Снять текущий пак и вернуть обычный скин. */
    public static void takeOff() {
        synchronized (LOCK) {
            DISPOSE_QUEUE.addAll(renderers);
            renderers = new ArrayList<BbModelRenderer>();
            current = null;
            currentEntry = null;
        }
        mode = Mode.AVATAR;
        weaponHandSelection = null;
        weaponOnBody = false;
        fitScale = 1f;
        groundOffset = 0f;
        petScale = 1f;
        headPetScale = 1f;
        handHeld = false;
        handScale = 1f;
        modelCenterX = 0f;
        modelCenterY = 0f;
        modelCenterZ = 0f;
        modelMinY = 0f;
        weaponLying = false;
        status = "Аватар снят";
        save(null);
    }

    public static boolean isValidAvatar(String folder) {
        FiguraAvatarLibrary.Entry entry = FiguraAvatarLibrary.byFolder(folder);
        return entry != null && !entry.models.isEmpty();
    }

    // ------------------------------------------------------- Габариты и режим

    private static Mode modeOf(FiguraAvatarLibrary.Entry entry) {
        if (entry == null || entry.section == null) {
            return Mode.AVATAR;
        }
        if (entry.headMount) {
            // Копия пета из «Аксессуаров» — тот же рендер, но строго на голове.
            return Mode.PET;
        }
        switch (entry.section) {
            case PETS:
                return Mode.PET;
            case ACCESSORIES:
            case WINGS:
                return Mode.BODY;
            case WEAPONS:
                return Mode.WEAPON;
            default:
                return Mode.AVATAR;
        }
    }

    private static void recomputeFit(List<BbModelRenderer> list, FiguraAvatarLibrary.Entry entry) {
        Mode detected = modeOf(entry);
        petOnHead = entry != null && entry.headMount;

        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        float minX = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE;
        float maxZ = -Float.MAX_VALUE;

        for (int i = 0; i < list.size(); i++) {
            BbModelRenderer renderer = list.get(i);
            if (!renderer.hasGeometry()) {
                continue;
            }
            minY = Math.min(minY, renderer.getMinY());
            maxY = Math.max(maxY, renderer.getMaxY());
            minX = Math.min(minX, renderer.getMinX());
            maxX = Math.max(maxX, renderer.getMaxX());
            minZ = Math.min(minZ, renderer.getMinZ());
            maxZ = Math.max(maxZ, renderer.getMaxZ());
        }

        mode = detected;

        if (minY == Float.MAX_VALUE || maxY <= minY) {
            fitScale = 1f;
            groundOffset = 0f;
            petScale = 1f;
            headPetScale = 1f;
            headPetAuthored = true;
            handHeld = false;
            handScale = 1f;
            modelCenterX = 0f;
            modelCenterY = 0f;
            modelCenterZ = 0f;
            modelMinY = 0f;
            weaponLying = false;
            longAxis = 1;
            gripScale = 1f;
            gripX = 0f;
            gripY = 0f;
            gripZ = 0f;
            return;
        }

        float height = maxY - minY;
        float depth = maxZ - minZ;
        float width = maxX - minX;

        groundOffset = -minY;
        modelMinY = minY;
        modelCenterX = (minX + maxX) / 2f;
        modelCenterY = (minY + maxY) / 2f;
        modelCenterZ = (minZ + maxZ) / 2f;

        float scale = 1.8f / height;
        if (scale < 0.15f) scale = 0.15f;
        if (scale > 6f) scale = 6f;
        fitScale = scale;

        // Пет не должен быть выше игрока.
        petScale = height > 1.0f ? 0.9f / height : 1f;

        // Пет на голове — подгоняем под шапку, по самому крупному габариту.
        float biggest = Math.max(height, Math.max(width, depth));
        float headFit = 1.35f / Math.max(biggest, 0.05f);
        if (headFit > 4f) headFit = 4f;
        if (headFit < 0.05f) headFit = 0.05f;
        headPetScale = headFit;

        // Если автор пака уже посадил модель на голову (то есть геометрия
        // находится на уровне головы игрока, а не вокруг нуля), ничего не
        // центруем и не масштабируем — рисуем строго в координатах игрока.
        // Подгоняем под макушку только модели, слепленные вокруг нуля.
        headPetAuthored = maxY >= 1.2f;

        // Маленькую модель берём в кисть; большая уже смоделирована в координатах игрока.
        handHeld = detected == Mode.WEAPON && maxY < 1.2f && minY > -1.2f;
        float handFit = 1.1f / Math.max(height, 0.05f);
        if (handFit > 1.6f) handFit = 1.6f;
        if (handFit < 0.2f) handFit = 0.2f;
        handScale = handFit;

        // Модель лежит вдоль Z (как коса, вытянутая вперёд) — на спине её надо поставить.
        weaponLying = depth > height * 1.3f;

        // Копия, нарисованная в координатах игрока (вся выше нуля и достаёт до корпуса).
        weaponOnBody = detected == Mode.WEAPON && minY > 0.15f && maxY > 1.2f;

        // Самая длинная ось — это древко/клинок. По ней выравниваем оружие.
        int axis = 1;
        float longest = height;
        if (width > longest) {
            axis = 0;
            longest = width;
        }
        if (depth > longest) {
            axis = 2;
            longest = depth;
        }
        longAxis = axis;

        // Длинное оружие приводим к адекватным «мечевым» размерам (~1.7 блока).
        float grip = 1.7f / Math.max(longest, 0.05f);
        if (grip > 3f) grip = 3f;
        if (grip < 0.1f) grip = 0.1f;
        gripScale = grip;

        // Точка хвата — 22% от нижнего конца длинной оси (рукоять), по остальным осям — центр.
        float t = 0.22f;
        gripX = modelCenterX;
        gripY = modelCenterY;
        gripZ = modelCenterZ;
        if (axis == 0) {
            gripX = minX + width * t;
        } else if (axis == 1) {
            gripY = minY + height * t;
        } else {
            gripZ = minZ + depth * t;
        }

        // Если модель нарисована вокруг нуля — автор уже поставил точку хвата в ноль.
        if (detected == Mode.WEAPON && minY < -0.05f && maxY > 0.05f) {
            gripX = 0f;
            gripY = 0f;
            gripZ = 0f;
        }
    }

    /**
     * Выбирает нужную копию оружия внутри модели: в паках вроде Cool scythe
     * лежит и версия для спины, и версия для руки — иначе видны две косы сразу.
     */
    private static void applyWeaponSelection(boolean hand) {
        Boolean currentSelection = weaponHandSelection;
        if (currentSelection != null && currentSelection.booleanValue() == hand) {
            return;
        }
        List<BbModelRenderer> list = renderers;
        if (list == null || list.isEmpty()) {
            return;
        }
        for (int i = 0; i < list.size(); i++) {
            BbModelRenderer renderer = list.get(i);
            try {
                String bone = renderer.pickWeaponBone(hand);
                if (bone != null) {
                    renderer.selectBone(bone);
                } else {
                    renderer.selectAll();
                }
            } catch (Throwable ignored) {
            }
        }
        weaponHandSelection = Boolean.valueOf(hand);

        FiguraAvatarLibrary.Entry entry = current == null ? null : FiguraAvatarLibrary.byFolder(current);
        if (entry != null) {
            recomputeFit(list, entry);
        }
    }

    // -------------------------------------------------------- Сохранение

    private static File gameDir() {
        Minecraft mc = Minecraft.getInstance();
        return mc != null && mc.gameDir != null ? mc.gameDir : new File(".");
    }

    private static File saveFile() {
        return new File(new File(gameDir(), "figura"), "harmony_avatar.txt");
    }

    private static void save(String folder) {
        try {
            File file = saveFile();
            File parent = file.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            Files.write(file.toPath(), (folder == null ? "" : folder).getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
    }

    public static void loadSavedSelection() {
        try {
            File file = saveFile();
            if (!file.isFile()) {
                return;
            }
            byte[] data = Files.readAllBytes(file.toPath());
            String folder = new String(data, StandardCharsets.UTF_8).trim();
            if (!folder.isEmpty() && isValidAvatar(folder)) {
                wear(folder);
            }
        } catch (Exception ignored) {
        }
    }

    // ----------------------------------------------------------- Рендер

    @Subscribe
    public void onRender(EventRender3D event) {
        disposePending();

        // Питомцы из вкладки «Петы» рисуются всегда, даже если аватар не надет.
        try {
            FiguraPetController.get().render(event);
        } catch (Throwable ignored) {
        }

        List<BbModelRenderer> list = renderers;
        Minecraft mc = Minecraft.getInstance();
        if (list == null || list.isEmpty() || mc.player == null || mc.world == null) {
            return;
        }
        if (mc.gameSettings.getPointOfView() == PointOfView.FIRST_PERSON) {
            return;
        }

        // Все эти настройки теперь свои у каждого пака (ПКМ по карточке).
        String packFolder = current;
        float userScale = FiguraPackSettings.getScale(packFolder) / 100f;
        float extraRotate = FiguraPackSettings.getRotate(packFolder);
        boolean autoFit = FiguraPackSettings.getHeightMode(packFolder) == FiguraPackSettings.HeightMode.AUTO;
        float offsetY = autoFit ? 0f : FiguraPackSettings.getHeight(packFolder) / 100f;

        float pt = event.getPartialTicks();
        double x = MathHelper.lerp(pt, mc.player.lastTickPosX, mc.player.getPosX()) - mc.getRenderManager().renderPosX();
        double y = MathHelper.lerp(pt, mc.player.lastTickPosY, mc.player.getPosY()) - mc.getRenderManager().renderPosY();
        double z = MathHelper.lerp(pt, mc.player.lastTickPosZ, mc.player.getPosZ()) - mc.getRenderManager().renderPosZ();
        float yaw = MathHelper.lerp(pt, mc.player.prevRenderYawOffset, mc.player.renderYawOffset);

        BbModelRenderer.Pose pose = buildPose(mc, pt);
        Mode currentMode = mode;
        String folder = current;
        double crouch = pose.crouching ? -0.125D : 0.0D;

        MatrixStack ms = event.getStack();
        ms.push();
        ms.translate(x, y + offsetY, z);
        ms.rotate(Vector3f.YP.rotationDegrees(180f - yaw + extraRotate));

        if (currentMode == Mode.PET) {
            if (petOnHead) {
                // Сидит на макушке и крутится вместе с головой.
                // Привязка ровно как у аксессуаров: точка шеи -> наклон корпуса -> повороты головы.
                ms.translate(0.0D, crouch, 0.0D);
                ms.translate(0.0D, NECK_Y, 0.0D);
                if (pose.crouching) {
                    ms.rotate(Vector3f.XP.rotationDegrees(28.6f));
                }
                ms.rotate(Vector3f.YP.rotationDegrees(-pose.headYaw));
                ms.rotate(Vector3f.XP.rotationDegrees(-pose.headPitch));

                if (headPetAuthored) {
                    // Автор пака уже посадил модель на голову и в нужном размере —
                    // рисуем его в координатах игрока, без подгонки и центрования.
                    ms.scale(userScale, userScale, userScale);
                    ms.translate(0.0D, -NECK_Y, 0.0D);
                } else {
                    float hs = userScale * headPetScale;
                    ms.translate(0.0D, HEAD_TOP - NECK_Y, 0.0D);
                    ms.scale(hs, hs, hs);
                    // Ставим модель дном ровно на макушку и по центру головы.
                    ms.translate(-modelCenterX, -modelMinY, -modelCenterZ);
                }
            } else {
                // Стоит справа-сзади от игрока, на земле.
                float ps = userScale * petScale;
                ms.translate(0.7D, crouch + groundOffset * ps, -0.45D);
                ms.scale(ps, ps, ps);
            }
        } else if (currentMode == Mode.BODY) {
            ms.translate(0.0D, crouch, 0.0D);
            ms.scale(userScale, userScale, userScale);
        } else if (currentMode == Mode.WEAPON) {
            FiguraPackSettings.WeaponPlacement place = FiguraPackSettings.getWeapon(folder);
            if (place == FiguraPackSettings.WeaponPlacement.AUTO) {
                place = holdingWeapon(mc)
                        ? FiguraPackSettings.WeaponPlacement.HAND
                        : FiguraPackSettings.WeaponPlacement.BACK;
            }

            applyWeaponSelection(place == FiguraPackSettings.WeaponPlacement.HAND);

            if (place == FiguraPackSettings.WeaponPlacement.BACK) {
                renderOnBack(ms, userScale, crouch);
            } else {
                renderInHand(ms, mc, pose, pt, userScale, crouch);
            }
        } else {
            float scale = userScale * (autoFit ? fitScale : 1f);
            float ground = autoFit ? groundOffset * scale : 0f;
            ms.translate(0.0D, ground + crouch, 0.0D);
            ms.scale(scale, scale, scale);
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableAlphaTest();
        RenderSystem.defaultAlphaFunc();
        RenderSystem.enableTexture();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.color4f(1f, 1f, 1f, 1f);

        BbModelRenderer.Pose renderPose = currentMode == Mode.AVATAR ? pose : bodyPose(pose, currentMode);

        for (int i = 0; i < list.size(); i++) {
            try {
                list.get(i).render(ms, renderPose);
            } catch (Throwable ignored) {
            }
        }

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.disableAlphaTest();
        RenderSystem.color4f(1f, 1f, 1f, 1f);
        ms.pop();
    }

    /**
     * Оружие в правой руке: берётся за рукоять, ходит вместе с махом руки и с ударом.
     * Раньше большие модели (Cool scythe) рисовались как есть — и коса торчала из живота.
     */
    private static void renderInHand(MatrixStack ms, Minecraft mc, BbModelRenderer.Pose pose,
                                     float pt, float userScale, double crouch) {
        float swing = 0f;
        try {
            swing = -60f * MathHelper.sin(mc.player.getSwingProgress(pt) * (float) Math.PI);
        } catch (Throwable ignored) {
        }
        float armAngle = pose.rightArm + swing;

        ms.translate(0.0D, crouch, 0.0D);
        ms.translate(SHOULDER_X, SHOULDER_Y, 0f);
        ms.rotate(Vector3f.XP.rotationDegrees(armAngle));
        ms.translate(0f, -ARM_LENGTH, 0f);          // кисть

        // Наклон как у обычного меча в руке.
        ms.rotate(Vector3f.XP.rotationDegrees(-100f));
        ms.rotate(Vector3f.ZP.rotationDegrees(10f));

        float ws = userScale * gripScale;
        ms.scale(ws, ws, ws);
        alignLongAxis(ms);
        ms.translate(-gripX, -gripY, -gripZ);       // рукоять в кисть
    }

    /** Оружие за спиной, по диагонали. */
    private static void renderOnBack(MatrixStack ms, float userScale, double crouch) {
        if (weaponOnBody) {
            // Автор уже нарисовал эту копию прямо на спине игрока — ничего не двигаем.
            ms.translate(0.0D, crouch, 0.0D);
            ms.scale(userScale, userScale, userScale);
            return;
        }
        ms.translate(0.0D, crouch, 0.0D);
        // точка между лопаток
        ms.translate(0.0D, 1.20D, 0.20D);
        ms.rotate(Vector3f.ZP.rotationDegrees(28f));
        ms.rotate(Vector3f.XP.rotationDegrees(-12f));

        float ws = userScale * gripScale;
        ms.scale(ws, ws, ws);
        alignLongAxis(ms);
        ms.translate(-modelCenterX, -modelCenterY, -modelCenterZ);
    }

    /** Поворачивает модель так, чтобы её самая длинная ось смотрела вверх. */
    private static void alignLongAxis(MatrixStack ms) {
        if (longAxis == 0) {
            ms.rotate(Vector3f.ZP.rotationDegrees(90f));
        } else if (longAxis == 2) {
            ms.rotate(Vector3f.XP.rotationDegrees(-90f));
        }
    }

    /** Держит ли игрок оружие в основной руке — для режима «Авто». */
    private static boolean holdingWeapon(Minecraft mc) {
        try {
            ItemStack stack = mc.player.getHeldItemMainhand();
            if (stack == null || stack.isEmpty()) {
                return false;
            }
            Item item = stack.getItem();
            return item instanceof SwordItem || item instanceof AxeItem || item instanceof TridentItem;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static BbModelRenderer.Pose bodyPose(BbModelRenderer.Pose source, Mode currentMode) {
        BbModelRenderer.Pose pose = new BbModelRenderer.Pose();
        if (currentMode == Mode.BODY) {
            pose.headYaw = source.headYaw;
            pose.headPitch = source.headPitch;
        }
        pose.crouching = false;
        return pose;
    }

    /** Поза игрока: голова + мах конечностями как у ванильной модели. */
    public static BbModelRenderer.Pose buildPose(Minecraft mc, float pt) {
        BbModelRenderer.Pose pose = new BbModelRenderer.Pose();
        if (mc.player == null) {
            return pose;
        }
        float yaw = MathHelper.lerp(pt, mc.player.prevRenderYawOffset, mc.player.renderYawOffset);
        pose.headYaw = MathHelper.lerp(pt, mc.player.prevRotationYawHead, mc.player.rotationYawHead) - yaw;
        pose.headPitch = MathHelper.lerp(pt, mc.player.prevRotationPitch, mc.player.rotationPitch);

        float limbSwingAmount = MathHelper.lerp(pt, mc.player.prevLimbSwingAmount, mc.player.limbSwingAmount);
        if (limbSwingAmount > 1.0f) {
            limbSwingAmount = 1.0f;
        }
        float limbSwing = mc.player.limbSwing - mc.player.limbSwingAmount * (1.0f - pt);

        pose.rightArm = (float) Math.toDegrees(MathHelper.cos(limbSwing * 0.6662f + (float) Math.PI) * 1.0f * limbSwingAmount);
        pose.leftArm = (float) Math.toDegrees(MathHelper.cos(limbSwing * 0.6662f) * 1.0f * limbSwingAmount);
        pose.rightLeg = (float) Math.toDegrees(MathHelper.cos(limbSwing * 0.6662f) * 1.4f * limbSwingAmount);
        pose.leftLeg = (float) Math.toDegrees(MathHelper.cos(limbSwing * 0.6662f + (float) Math.PI) * 1.4f * limbSwingAmount);
        pose.crouching = mc.player.isSneaking();
        return pose;
    }

    private static void disposePending() {
        List<BbModelRenderer> dead;
        synchronized (LOCK) {
            if (DISPOSE_QUEUE.isEmpty()) {
                return;
            }
            dead = new ArrayList<BbModelRenderer>(DISPOSE_QUEUE);
            DISPOSE_QUEUE.clear();
        }
        for (int i = 0; i < dead.size(); i++) {
            try {
                dead.get(i).close();
            } catch (Throwable ignored) {
            }
        }
    }
}
