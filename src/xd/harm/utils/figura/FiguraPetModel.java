package xd.harm.utils.figura;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.util.math.vector.Vector3f;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Питомец из Figura-пака (например Axolotl! или Turtle Pet) для модуля Pet.
 *
 * Модель грузится один раз в фоне, а рисуется на той позиции, которую даёт
 * «мозг» питомца (DogBrain — ходит по земле, DragonBrain — летает рядом).
 */
public final class FiguraPetModel {

    /** Имя папки пака в figura/avatars. */
    private final String folder;
    /** Желаемая высота питомца в блоках. */
    private final float targetHeight;

    private volatile List<BbModelRenderer> renderers = new ArrayList<BbModelRenderer>();
    private volatile boolean loading = false;
    private volatile boolean failed = false;

    private volatile float fit = 1f;
    private volatile float centerX = 0f;
    private volatile float centerZ = 0f;
    private volatile float minY = 0f;

    public FiguraPetModel(String folder, float targetHeight) {
        this.folder = folder;
        this.targetHeight = targetHeight;
    }

    public String getFolder() {
        return folder;
    }

    public float getTargetHeight() {
        return targetHeight;
    }

    public boolean isReady() {
        return !renderers.isEmpty();
    }

    /** Пак мог появиться позже — даём возможность попробовать снова. */
    public void reset() {
        failed = false;
    }

    private void ensureLoading() {
        if (loading || failed || !renderers.isEmpty()) {
            return;
        }
        final FiguraAvatarLibrary.Entry entry = FiguraAvatarLibrary.byFolder(folder);
        if (entry == null || entry.models.isEmpty()) {
            failed = true;
            return;
        }
        loading = true;
        Thread thread = new Thread(new Runnable() {
            public void run() {
                List<BbModelRenderer> built = new ArrayList<BbModelRenderer>();
                try {
                    FiguraPackSettings.beginBuild(entry.folder);
                    for (int i = 0; i < entry.models.size(); i++) {
                        Path model = entry.models.get(i);
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
                    failed = true;
                    loading = false;
                    return;
                }

                measure(built);
                renderers = built;
                loading = false;
            }
        }, "Harmony-FiguraPet-Loader");
        thread.setDaemon(true);
        thread.start();
    }

    /** Габариты пака: масштаб под нужную высоту и посадка на землю по центру. */
    private void measure(List<BbModelRenderer> list) {
        float lowY = Float.MAX_VALUE;
        float highY = -Float.MAX_VALUE;
        float lowX = Float.MAX_VALUE;
        float highX = -Float.MAX_VALUE;
        float lowZ = Float.MAX_VALUE;
        float highZ = -Float.MAX_VALUE;

        for (int i = 0; i < list.size(); i++) {
            BbModelRenderer renderer = list.get(i);
            if (!renderer.hasGeometry()) {
                continue;
            }
            lowY = Math.min(lowY, renderer.getMinY());
            highY = Math.max(highY, renderer.getMaxY());
            lowX = Math.min(lowX, renderer.getMinX());
            highX = Math.max(highX, renderer.getMaxX());
            lowZ = Math.min(lowZ, renderer.getMinZ());
            highZ = Math.max(highZ, renderer.getMaxZ());
        }

        if (lowY == Float.MAX_VALUE || highY <= lowY) {
            fit = 1f;
            centerX = 0f;
            centerZ = 0f;
            minY = 0f;
            return;
        }

        float height = highY - lowY;
        float scale = targetHeight / height;
        if (scale < 0.05f) scale = 0.05f;
        if (scale > 8f) scale = 8f;

        fit = scale;
        minY = lowY;
        centerX = (lowX + highX) / 2f;
        centerZ = (lowZ + highZ) / 2f;
    }

    /**
     * Рисует питомца. Стек уже смещён в позицию питомца.
     *
     * @param bodyYaw         поворот корпуса из «мозга»
     * @param userScale       множитель размера (1.0 — как есть)
     * @param limbSwing       фаза шага
     * @param limbSwingAmount сила шага (0 — стоит)
     */
    public void render(MatrixStack ms, float bodyYaw, float userScale, float limbSwing, float limbSwingAmount) {
        ensureLoading();
        List<BbModelRenderer> list = renderers;
        if (list.isEmpty()) {
            return;
        }

        BbModelRenderer.Pose pose = new BbModelRenderer.Pose();
        float swing = (float) Math.toDegrees(Math.cos(limbSwing * 0.6662f) * 1.2f * limbSwingAmount);
        pose.rightLeg = swing;
        pose.leftLeg = -swing;
        pose.rightArm = -swing;
        pose.leftArm = swing;
        pose.crouching = false;

        float scale = fit * userScale;
        if (scale <= 0f) {
            return;
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

        ms.push();
        ms.rotate(Vector3f.YP.rotationDegrees(180f - bodyYaw));
        ms.scale(scale, scale, scale);
        ms.translate(-centerX, -minY, -centerZ);
        for (int i = 0; i < list.size(); i++) {
            try {
                list.get(i).render(ms, pose);
            } catch (Throwable ignored) {
            }
        }
        ms.pop();

        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.disableAlphaTest();
        RenderSystem.color4f(1f, 1f, 1f, 1f);
    }
}
