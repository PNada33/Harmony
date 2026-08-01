package xd.harm.utils.figura;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.vector.Matrix3f;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import xd.harm.events.input.EventMouseButtonPress;
import xd.harm.events.render.EventRender3D;
import xd.harm.events.world.EventUpdate;
import xd.harm.utils.SoundUtil;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Функция «Погладить» (бывший модуль PatPatPat) внутри Figura Cosmetic.
 *
 * Логика и анимация перенесены без изменений: правая кнопка мыши по мобу или
 * питомцу проигрывает анимацию ладошки и слегка приминает цель.
 * Включение теперь идёт через {@link CosmeticFeatures}.
 */
public final class CosmeticPat {

    private static final ResourceLocation TEXTURE = new ResourceLocation("minecraft", "harmony/images/patpat/patpat.png");
    private static final RenderType RENDER_TYPE = RenderType.getEntityTranslucent(TEXTURE);
    private static final String[] SOUNDS = {"pat.wav", "pat1.wav", "pat2.wav"};
    private static final int FRAME_COUNT = 5;
    private static final float FRAME_WIDTH = 1.0F / FRAME_COUNT;
    private static final Quaternion ROTATE_Y_180 = Vector3f.YP.rotationDegrees(180.0F);
    private static final long DURATION_MS = 240L;
    private static final int COOLDOWN_TICKS = 4;
    private static final float SCALE = 0.85F;
    private static final float PAT_WEIGHT = 0.425F;
    private static final double PET_PAT_RANGE = 6.0D;

    private static final CosmeticPat INSTANCE = new CosmeticPat();

    private final Map<Integer, EntityPatAnimation> entityAnimations = new HashMap<Integer, EntityPatAnimation>();
    private PetPatAnimation petAnimation;
    private FiguraPetController cachedPetModule;
    private int cooldownTicks;

    private CosmeticPat() {
    }

    public static CosmeticPat get() {
        return INSTANCE;
    }

    private static Minecraft mc() {
        return Minecraft.getInstance();
    }

    private boolean enabled() {
        return CosmeticFeatures.isEnabled(CosmeticFeatures.PAT);
    }

    /** Сбрасывает анимации — вызывается при выключении функции. */
    public void reset() {
        entityAnimations.clear();
        petAnimation = null;
        cooldownTicks = 0;
    }

    // --------------------------------------------------------------- События

    @Subscribe
    public void onMouseButton(EventMouseButtonPress event) {
        if (!enabled() || event.getButton() != 1) {
            return;
        }
        tryPat();
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (!enabled()) {
            if (!entityAnimations.isEmpty() || petAnimation != null) {
                reset();
            }
            return;
        }
        Minecraft mc = mc();
        if (mc.player == null || mc.world == null) {
            reset();
            return;
        }
        if (cooldownTicks > 0) {
            cooldownTicks--;
        }
        if (!entityAnimations.isEmpty() || petAnimation != null) {
            long timeMillis = System.currentTimeMillis();
            Iterator<EntityPatAnimation> iterator = entityAnimations.values().iterator();
            while (iterator.hasNext()) {
                if (shouldRemove(iterator.next(), timeMillis)) {
                    iterator.remove();
                }
            }
            if (shouldRemove(petAnimation, timeMillis)) {
                petAnimation = null;
            }
        }
        if (mc.gameSettings.keyBindUseItem.isKeyDown()) {
            tryPat();
        }
    }

    @Subscribe
    public void onRender3D(EventRender3D event) {
        Minecraft mc = mc();
        if (!enabled() || (entityAnimations.isEmpty() && petAnimation == null)
                || mc.player == null || mc.world == null) {
            return;
        }
        MatrixStack matrixStack = event.getStack();
        IRenderTypeBuffer buffer = event.getVertex();
        long timeMillis = System.currentTimeMillis();
        FiguraPetController pet = petAnimation != null ? getPetModule() : null;
        EntityRendererManager renderManager = mc.getRenderManager();
        double renderX = renderManager.renderPosX();
        double renderY = renderManager.renderPosY();
        double renderZ = renderManager.renderPosZ();
        Quaternion cameraOrientation = renderManager.getCameraOrientation();
        for (EntityPatAnimation animation : entityAnimations.values()) {
            if (!shouldRemove(animation, timeMillis)) {
                renderEntityAnimation(animation, event.getPartialTicks(), renderX, renderY, renderZ,
                        cameraOrientation, matrixStack, buffer, timeMillis);
            }
        }
        if (!shouldRemove(petAnimation, timeMillis, pet)) {
            renderPetAnimation(petAnimation, pet, renderX, renderY, renderZ, cameraOrientation,
                    matrixStack, buffer, timeMillis);
        }
    }

    // ---------------------------------------------------------------- Рендер

    private void renderEntityAnimation(EntityPatAnimation animation, float partialTicks, double renderX,
                                       double renderY, double renderZ, Quaternion cameraOrientation,
                                       MatrixStack matrixStack, IRenderTypeBuffer buffer, long timeMillis) {
        LivingEntity target = animation.target;
        double x = target.lastTickPosX + (target.getPosX() - target.lastTickPosX) * partialTicks - renderX;
        double y = target.lastTickPosY + (target.getPosY() - target.lastTickPosY) * partialTicks - renderY
                + target.getHeight() * animation.getTargetScaleY(timeMillis) + 0.11D;
        double z = target.lastTickPosZ + (target.getPosZ() - target.lastTickPosZ) * partialTicks - renderZ;
        int light = WorldRenderer.getCombinedLight(mc().world,
                new BlockPos(target.getPosX(), target.getPosY() + target.getHeight(), target.getPosZ()));
        renderPatQuad(x, y, z, light, animation.getFrame(timeMillis), cameraOrientation, matrixStack, buffer);
    }

    private void renderPetAnimation(PetPatAnimation animation, FiguraPetController pet, double renderX,
                                    double renderY, double renderZ, Quaternion cameraOrientation,
                                    MatrixStack matrixStack, IRenderTypeBuffer buffer, long timeMillis) {
        if (pet == null || !pet.isState()) {
            return;
        }
        Vector3d petPos = pet.getPatPosition();
        if (petPos == null) {
            return;
        }
        float petHeight = pet.getPatHeight();
        double x = petPos.x - renderX;
        double y = petPos.y - renderY + petHeight * animation.getTargetScaleY(petHeight, timeMillis) + 0.11D;
        double z = petPos.z - renderZ;
        int light = WorldRenderer.getCombinedLight(mc().world,
                new BlockPos(petPos.x, petPos.y + petHeight, petPos.z));
        renderPatQuad(x, y, z, light, animation.getFrame(timeMillis), cameraOrientation, matrixStack, buffer);
    }

    private void renderPatQuad(double x, double y, double z, int light, int frame,
                               Quaternion cameraOrientation, MatrixStack matrixStack, IRenderTypeBuffer buffer) {
        float u1 = frame * FRAME_WIDTH;
        float u2 = u1 + FRAME_WIDTH;

        matrixStack.push();
        matrixStack.translate(x, y, z);
        matrixStack.rotate(cameraOrientation);
        matrixStack.rotate(ROTATE_Y_180);
        matrixStack.scale(SCALE, SCALE, SCALE);

        MatrixStack.Entry entry = matrixStack.getLast();
        Matrix4f matrix = entry.getMatrix();
        Matrix3f normal = entry.getNormal();
        IVertexBuilder builder = buffer.getBuffer(RENDER_TYPE);

        vertex(builder, matrix, normal, light, -0.5F, -0.5F, u1, 1.0F);
        vertex(builder, matrix, normal, light, -0.5F, 0.5F, u1, 0.0F);
        vertex(builder, matrix, normal, light, 0.5F, 0.5F, u2, 0.0F);
        vertex(builder, matrix, normal, light, 0.5F, -0.5F, u2, 1.0F);

        matrixStack.pop();
    }

    private void vertex(IVertexBuilder builder, Matrix4f matrix, Matrix3f normal, int light,
                        float x, float y, float u, float v) {
        builder.pos(matrix, x, y, 0.0F).color(255, 255, 255, 255).tex(u, v)
                .overlay(OverlayTexture.NO_OVERLAY).lightmap(light)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
    }

    // ----------------------------------------------------------- Логика пата

    private void tryPat() {
        if (!canPat()) {
            return;
        }
        LivingEntity entityTarget = getEntityTarget();
        if (entityTarget != null) {
            entityAnimations.put(Integer.valueOf(entityTarget.getEntityId()), new EntityPatAnimation(entityTarget));
            playPat();
            return;
        }

        FiguraPetController petTarget = getPetTarget();
        if (petTarget == null) {
            return;
        }
        petAnimation = new PetPatAnimation(petTarget.getPetTypeIndex());
        playPat();
    }

    private void playPat() {
        cooldownTicks = COOLDOWN_TICKS;
        mc().player.swingArm(Hand.MAIN_HAND);
        SoundUtil.playSound(SOUNDS[ThreadLocalRandom.current().nextInt(SOUNDS.length)], 0.25D, false);
    }

    private LivingEntity getEntityTarget() {
        Minecraft mc = mc();
        if (!(mc.objectMouseOver instanceof EntityRayTraceResult)) {
            return null;
        }
        Entity entity = ((EntityRayTraceResult) mc.objectMouseOver).getEntity();
        if (!(entity instanceof LivingEntity)) {
            return null;
        }
        LivingEntity target = (LivingEntity) entity;
        return isValidTarget(target) ? target : null;
    }

    private FiguraPetController getPetTarget() {
        FiguraPetController pet = getPetModule();
        if (pet == null || !pet.isState()) {
            return null;
        }
        Minecraft mc = mc();
        Entity viewEntity = mc.getRenderViewEntity();
        if (viewEntity == null) {
            viewEntity = mc.player;
        }
        Vector3d start = viewEntity.getEyePosition(1.0F);
        Vector3d end = start.add(viewEntity.getLook(1.0F).scale(PET_PAT_RANGE));
        return pet.canPat(start, end) ? pet : null;
    }

    private boolean canPat() {
        Minecraft mc = mc();
        return mc.player != null && mc.world != null && mc.currentScreen == null
                && cooldownTicks <= 0 && mc.player.getHeldItemMainhand().isEmpty();
    }

    private boolean isValidTarget(LivingEntity target) {
        Minecraft mc = mc();
        return target.isAlive() && !target.isInvisible() && target.world == mc.world
                && mc.world.getEntityByID(target.getEntityId()) == target;
    }

    private boolean shouldRemove(EntityPatAnimation animation, long timeMillis) {
        return animation == null || animation.isExpired(timeMillis) || !isValidTarget(animation.target);
    }

    private boolean shouldRemove(PetPatAnimation animation, long timeMillis) {
        return shouldRemove(animation, timeMillis, getPetModule());
    }

    private boolean shouldRemove(PetPatAnimation animation, long timeMillis, FiguraPetController pet) {
        if (animation == null || animation.isExpired(timeMillis)) {
            return true;
        }
        return pet == null || !pet.isState() || pet.getPetTypeIndex() != animation.petTypeIndex
                || pet.getPatPosition() == null;
    }

    private FiguraPetController getPetModule() {
        if (cachedPetModule == null) {
            cachedPetModule = FiguraPetController.get();
        }
        return cachedPetModule;
    }

    // ------------------------------------------------------- Масштаб цели

    /** Насколько приминается моб, которого гладят. Зовётся из LivingRenderer. */
    public float getEntityScale(LivingEntity target) {
        if (!enabled()) {
            return 1.0F;
        }
        EntityPatAnimation animation = entityAnimations.get(Integer.valueOf(target.getEntityId()));
        long timeMillis = System.currentTimeMillis();
        if (shouldRemove(animation, timeMillis)) {
            return 1.0F;
        }
        return animation.getTargetScaleY(timeMillis);
    }

    /** Насколько приминается питомец, которого гладят. */
    public float getPetScale(int petTypeIndex, float petHeight) {
        if (!enabled()) {
            return 1.0F;
        }
        long timeMillis = System.currentTimeMillis();
        if (shouldRemove(petAnimation, timeMillis) || petAnimation.petTypeIndex != petTypeIndex) {
            return 1.0F;
        }
        return petAnimation.getTargetScaleY(petHeight, timeMillis);
    }

    // -------------------------------------------------------------- Анимации

    private static class EntityPatAnimation extends TimedAnimation {
        private final LivingEntity target;

        private EntityPatAnimation(LivingEntity target) {
            this.target = target;
        }

        private float getTargetScaleY(long timeMillis) {
            return getPatScale(target.getHeight(), timeMillis);
        }
    }

    private static class PetPatAnimation extends TimedAnimation {
        private final int petTypeIndex;

        private PetPatAnimation(int petTypeIndex) {
            this.petTypeIndex = petTypeIndex;
        }

        private float getTargetScaleY(float petHeight, long timeMillis) {
            return getPatScale(petHeight, timeMillis);
        }
    }

    private static class TimedAnimation {
        private final long startTime = System.currentTimeMillis();

        protected final boolean isExpired(long timeMillis) {
            return timeMillis - startTime >= DURATION_MS;
        }

        protected final float getLinearProgress(long timeMillis) {
            return Math.min(1.0F, (timeMillis - startTime) / (float) DURATION_MS);
        }

        protected final float getAnimationProgress(long timeMillis) {
            float progress = getLinearProgress(timeMillis);
            float inv = 1.0F - progress;
            return 1.0F - inv * inv;
        }

        protected final float getPatScale(float height, long timeMillis) {
            float range = Math.min(0.95F, PAT_WEIGHT / Math.max(height, 0.001F));
            return (1.0F - range) + range * (1.0F - (float) Math.sin(getAnimationProgress(timeMillis) * Math.PI));
        }

        protected final int getFrame(long timeMillis) {
            return Math.min(FRAME_COUNT - 1, (int) Math.floor(FRAME_COUNT * getAnimationProgress(timeMillis)));
        }
    }
}
