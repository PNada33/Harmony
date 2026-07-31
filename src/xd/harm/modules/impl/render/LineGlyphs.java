package xd.harm.modules.impl.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.culling.ClippingHelper;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import org.lwjgl.opengl.GL11;
import xd.harm.events.render.EventRender3D;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.ColorSetting;
import xd.harm.modules.settings.impl.ModeSetting;
import xd.harm.modules.settings.impl.SliderSetting;
import xd.harm.utils.render.AnimationUtils;
import xd.harm.utils.render.color.ColorUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@ModuleRegister(name = "LineGlyphs", category = Category.Render, desc = "Линии в Мире")
public class LineGlyphs extends Module {
    public static LineGlyphs get;

    public final SliderSetting glyphsCount = new SliderSetting("Количество Линий", 70.0F, 10.0F, 200.0F, 1.0F);
    public final BooleanSetting slowSpeed = new BooleanSetting("Медленная Скорость", false);
    public final BooleanSetting applyStippleLines = new BooleanSetting("Пунктирные Линии", false);
    public final SliderSetting stippleStepPixels = new SliderSetting("Разделение", 3.0F, 0.5F, 20.0F, 0.5F).setVisible(() -> applyStippleLines.get());
    public final BooleanSetting linesGlowing = new BooleanSetting("Свечение", false);
    public final ModeSetting colorMode = new ModeSetting("Цвет", "Клиент", "Радужный", "Клиент", "Свой", "Свой 2 цвета");
    public final ColorSetting pickColor1 = new ColorSetting("Цвет 1", ColorUtils.rgb(100, 255, 100)).setVisible(() -> colorMode.get().contains("Свой"));
    public final ColorSetting pickColor2 = new ColorSetting("Цвет 2", ColorUtils.rgb(60, 60, 255)).setVisible(() -> colorMode.get().equalsIgnoreCase("Свой 2 цвета"));

    private final Random rand = new Random(93882L);
    private final List<Vec3d> temp3dVecs = new ArrayList<>();
    private static final Tessellator tessellator = Tessellator.getInstance();
    private final List<GliphsVecGen> glyphs = new ArrayList<>();
    private final List<GliphsVecGen> filteredGlyphs = new ArrayList<>();
    private static final int[] LINE_MOVE_STEPS = {0, 3};
    private static final int[] LINE_STEPS_AMOUNT = {7, 12};
    private static final int[] SPAWN_RANGES = {6, 24, 0, 12};

    public LineGlyphs() {
        addSettings(glyphsCount, slowSpeed, applyStippleLines, stippleStepPixels, linesGlowing, colorMode, pickColor1, pickColor2);
        get = this; 
    }

    @Override
    public boolean onDisable() {
        glyphs.clear();
        return super.onDisable();
    }

    @Subscribe
    public void onUpdate(EventUpdate e) {
        if (mc.player == null || mc.world == null) {
            glyphs.clear();
            return;
        }
        gliphsUpdate();
        addAllGliphs(maxObjCount(), getFrustum());
    }

    @Subscribe
    public void onRender(EventRender3D e) {
        if (mc.player == null || mc.world == null || mc.getRenderManager().info == null) {
            return;
        }
        try {
            getAnimation().update();
            float alphaPC = (float) MathHelper.clamp(getAnimation().getValue(), 0.0, 1.0);
            if (!isState()) {
                if (alphaPC < 0.03F) {
                    glyphs.clear();
                    return;
                }
            }
            ClippingHelper frustum = getFrustum();
            gliphsRemoveAuto(alphaPC, frustum);
            if (e.getStack() == null) {
                return;
            }
            MatrixStack stack = e.getStack();
            Vector3d cam = mc.getRenderManager().info.getProjectedView();
            stack.push();
            stack.translate(-cam.x, -cam.y, -cam.z);
            drawAllGliphs(alphaPC, e.getPartialTicks(), frustum, stack);
            stack.pop();
        } finally {
            restoreRenderState();
        }
    }

    private static void restoreRenderState() {
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.enableTexture();
        RenderSystem.enableAlphaTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.shadeModel(GL11.GL_FLAT);
        RenderSystem.alphaFunc(GL11.GL_GREATER, 0.1F);
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glLineWidth(1.0F);
        GL11.glPointSize(1.0F);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glDisable(GL11.GL_POINT_SMOOTH);
    }

    private void applyStippleModeToLines(Runnable linesBeginsDraw, boolean set, float stippleStep) {
        int stepInt = (int) (stippleStep + 0.5F);
        if (stepInt <= 0) {
            set = false;
        }
        if (set) {
            GL11.glEnable(GL11.GL_LINE_STIPPLE);
            GL11.glLineStipple(stepInt, (short) -21846);
            linesBeginsDraw.run();
            GL11.glDisable(GL11.GL_LINE_STIPPLE);
        } else {
            linesBeginsDraw.run();
        }
    }

    private int stateColor(int index, float alphaPC) {
        int color = -1;
        String mode = colorMode.get();
        if ("Радужный".equalsIgnoreCase(mode)) {
            color = ColorUtils.rainbow(8, index, 0.85f, 1.0f, 1.0f);
        } else if ("Клиент".equalsIgnoreCase(mode)) {
            color = Theme.MainColor(index);
        } else if ("Свой".equalsIgnoreCase(mode)) {
            color = pickColor1.get();
        } else if ("Свой 2 цвета".equalsIgnoreCase(mode)) {
            float t = (float) ((Math.sin((System.currentTimeMillis() / 300.0) + index * 0.05) + 1.0) * 0.5);
            color = ColorUtils.interpolateColor(pickColor1.get(), pickColor2.get(), t);
        }
        int white = ColorUtils.setAlpha(0xFFFFFFFF, ColorUtils.getAlphaFromColor(color));
        color = ColorUtils.interpolateColor(color, white, 0.1f);
        return ColorUtils.swapAlpha(color, (float) ColorUtils.getAlphaFromColor(color) * alphaPC);
    }

    private int[] lineMoveSteps() {
        return LINE_MOVE_STEPS;
    }

    private int[] lineStepsAmount() {
        return LINE_STEPS_AMOUNT;
    }

    private int[] spawnRanges() {
        return SPAWN_RANGES;
    }

    private int maxObjCount() {
        return Math.max(1, (int) glyphsCount.get().floatValue());
    }

    private int minGliphsJointDst() {
        return 8;
    }

    private int getR360X() {
        return rand.nextInt(4) * 90;
    }

    private int getR360Y() {
        return (rand.nextInt(4) - 2) * 90;
    }

    private void setR360XY(int[] result) {
        result[0] = rand.nextInt(4) * 90;
        result[1] = (rand.nextInt(2) - 1) * 90;
    }

    private void setA90R(int[] outdated, int[] result) {
        int a = outdated[0];
        int ao = a;
        int b = outdated[1];
        int bo = b;
        for (int maxAttempt = 150; maxAttempt > 0 && Math.abs(b - bo) != 90; --maxAttempt) {
            b = getR360Y();
        }
        for (int maxAttempt = 5; maxAttempt > 0 && (Math.abs(a - ao) != 90 || Math.abs(a - ao) != 270); --maxAttempt) {
            a = getR360X();
        }
        result[0] = a;
        result[1] = b;
    }

    private Vec3i offsetFromRXYR(Vec3i vec3i, int[] rxy, int r) {
        float yawR = (float) Math.toRadians(rxy[0]);
        float pitchR = (float) Math.toRadians(rxy[1]);
        float r1 = (float) r;
        int ry = (int) (MathHelper.sin(pitchR) * r1);
        if (pitchR != 0.0F) {
            r1 = 0.0F;
        }
        int rx = (int) (-(MathHelper.sin(yawR) * r1));
        int rz = (int) (MathHelper.cos(yawR) * r1);
        return new Vec3i(vec3i.getX() + rx, vec3i.getY() + ry, vec3i.getZ() + rz);
    }

    private float moveAdvanceFromTicks(int ticksSet, int ticksExpiring, float pTicks) {
        return Math.min(Math.max(1.0F - ((float) ticksExpiring - pTicks) / (float) ticksSet, 0.0F), 1.0F);
    }

    private List<Vec3d> getSmoothTickedFromList(List<Vec3i> vec3is, float moveAdvance) {
        temp3dVecs.clear();
        for (int i = 0; i < vec3is.size(); i++) {
            Vec3i vec3i = vec3is.get(i);
            double x = vec3i.getX();
            double y = vec3i.getY();
            double z = vec3i.getZ();
            if (vec3is.size() >= 2 && i == vec3is.size() - 1) {
                Vec3i prev = vec3is.get(vec3is.size() - 2);
                x = lerp(prev.getX(), x, moveAdvance);
                y = lerp(prev.getY(), y, moveAdvance);
                z = lerp(prev.getZ(), z, moveAdvance);
            }
            temp3dVecs.add(new Vec3d(x, y, z));
        }
        return temp3dVecs;
    }

    private Vec3i randGliphSpawnPos() {
        int[] spawn = spawnRanges();
        Vector3d cam = mc.getRenderManager().info != null ? mc.getRenderManager().info.getProjectedView() : mc.player.getPositionVec();
        for (int attempt = 16; attempt > 0; --attempt) {
            double dst = (double) randInt(spawn[0], spawn[1]);
            double fov = mc.gameSettings.fov;
            float yaw = mc.player == null ? 0.0F : mc.player.rotationYaw;
            int yawMin = (int) (yaw - fov * 0.75F);
            int yawMax = (int) (yaw + fov * 0.75F);
            float radYaw = (float) Math.toRadians(randInt(yawMin, yawMax));
            int randXOff = (int) (-(MathHelper.sin(radYaw) * dst));
            int randYOff = randInt(-spawn[2], spawn[3]);
            int randZOff = (int) (MathHelper.cos(radYaw) * dst);
            Vec3i pos = new Vec3i(cam.x + randXOff, cam.y + randYOff, cam.z + randZOff);
            if (isSpawnPosFree(pos)) {
                return pos;
            }
        }
        return new Vec3i(cam.x, cam.y, cam.z);
    }

    private boolean isSpawnPosFree(Vec3i pos) {
        if (mc.world == null) {
            return true;
        }
        BlockPos bp = new BlockPos(pos.getX(), pos.getY(), pos.getZ());
        BlockState state = mc.world.getBlockState(bp);
        if (state.isAir()) {
            return true;
        }
        if (!state.getFluidState().isEmpty()) {
            return false;
        }
        return state.getCollisionShape(mc.world, bp).isEmpty();
    }

    private Vec3i genWhiteGliphPos(List<GliphsVecGen> gliphVecGens, ClippingHelper frustum) {
        Vec3i temp = randGliphSpawnPos();
        if (gliphVecGens.size() >= 2) {
            for (int maxAttempt = 8; maxAttempt > 0; --maxAttempt) {
                boolean tooClose = false;
                for (GliphsVecGen gen : gliphVecGens) {
                    if (gen.vecGens.size() >= 1) {
                        Vec3i v = gen.vecGens.get(0);
                        if (v.distanceSq(temp) <= (double) (minGliphsJointDst() * minGliphsJointDst())) {
                            tooClose = true;
                            break;
                        }
                    }
                }
                if (!tooClose && frustum != null) {
                    if (!frustum.isBoundingBoxInFrustum(aabbFromVec3d(new Vec3d((double) temp.getX(), (double) temp.getY(), (double) temp.getZ())))) {
                        tooClose = true;
                    }
                }
                if (!tooClose) {
                    break;
                }
                temp = randGliphSpawnPos();
            }
        }
        return temp;
    }

    private void addAllGliphs(int countCap, ClippingHelper frustum) {
        for (int maxAttempt = 8; maxAttempt > 0 && activeGlyphCount() < countCap; --maxAttempt) {
            int[] stepsAmount = lineStepsAmount();
            while (glyphs.size() < countCap) {
                Vec3i pos = randGliphSpawnPos();
                glyphs.add(new GliphsVecGen(pos, randInt(stepsAmount[0], stepsAmount[1])));
            }
        }
    }

    private int activeGlyphCount() {
        int count = 0;
        for (GliphsVecGen glyph : glyphs) {
            if (glyph.alphaPC.to != 0.0F) {
                count++;
            }
        }
        return count;
    }

    private void gliphsRemoveAuto(float moduleAlphaPC, ClippingHelper frustum) {
        for (int i = glyphs.size() - 1; i >= 0; --i) {
            if (glyphs.get(i).isToRemove(moduleAlphaPC, frustum)) {
                glyphs.remove(i);
            }
        }
    }

    private void gliphsUpdate() {
        for (GliphsVecGen gen : glyphs) {
            gen.update();
        }
    }

    private static AxisAlignedBB aabbFromVec3d(Vec3d pos) {
        return new AxisAlignedBB(pos.xCoord, pos.yCoord, pos.zCoord, pos.xCoord, pos.yCoord, pos.zCoord).grow(0.1);
    }

    private void drawAllGliphs(float alphaPC, float pTicks, ClippingHelper frustum, MatrixStack stack) {
        if (glyphs.isEmpty()) {
            return;
        }
        filteredGlyphs.clear();
        for (GliphsVecGen gen : glyphs) {
            if (alphaPC * gen.getAlphaPC() * 255.0F >= 1.0F) {
                filteredGlyphs.add(gen);
            }
        }
        if (filteredGlyphs.isEmpty()) {
            return;
        }
        float step = (float) (Math.round(stippleStepPixels.get() * 2.0f) / 2.0f);
        float stipple = applyStippleLines.get() ? step : 0.0F;
        float glowingPC = linesGlowing.get() ? 1.0F : 0.0F;
        boolean glowing = glowingPC * 255.0F > 1.0F;
        GliphVecRenderer.set3DRendering(glowing, () -> {
            applyStippleModeToLines(() -> {
                int colorIndex = 0;
                for (GliphsVecGen gen : filteredGlyphs) {
                    colorIndex++;
                    GliphVecRenderer.clientColoredBegin(this, gen, colorIndex, 180, alphaPC * gen.alphaPC.anim, pTicks, frustum, 1.0F, 0.0F, stack);
                }
            }, stipple > 0.25F, stipple);
            if (glowing) {
                int colorIndex = 0;
                for (GliphsVecGen gen : filteredGlyphs) {
                    colorIndex++;
                    GliphVecRenderer.clientColoredBegin(this, gen, colorIndex, 180, alphaPC * gen.alphaPC.anim * glowingPC * 0.1F, pTicks, frustum, 1.0F + glowingPC * 0.5F, glowingPC * 4.0F, stack);
                }
                colorIndex = 0;
                for (GliphsVecGen gen : filteredGlyphs) {
                    colorIndex++;
                    GliphVecRenderer.clientColoredBegin(this, gen, colorIndex, 180, alphaPC * gen.alphaPC.anim * glowingPC * 0.04F, pTicks, frustum, 1.0F + glowingPC * 0.9F, glowingPC * 9.0F, stack);
                }
            }
        });
    }

    private ClippingHelper getFrustum() {
        ClippingHelper frustum = WorldRenderer.frustum;
        if (frustum != null && mc.getRenderManager().info != null) {
            Vector3d cam = mc.getRenderManager().info.getProjectedView();
            frustum.setCameraPosition(cam.x, cam.y, cam.z);
        }
        return frustum;
    }

    private int randInt(int min, int max) {
        if (max <= min) {
            return min;
        }
        return rand.nextInt(max - min) + min;
    }

    private double lerp(double start, double end, double delta) {
        return start + (end - start) * delta;
    }

    private class GliphsVecGen {
        private final List<Vec3i> vecGens = new ArrayList<>();
        private int currentStepTicks;
        private int lastStepSet;
        private int stepsAmount;
        private final int[] lastYawPitch = new int[2];
        private final int[] nextYawPitch = new int[2];
        private final AnimationUtils alphaPC = new AnimationUtils(0.1F, 1.0F, 0.075F);

        GliphsVecGen(Vec3i spawnPos, int maxStepsAmount) {
            vecGens.add(spawnPos);
            setR360XY(lastYawPitch);
            stepsAmount = maxStepsAmount;
        }

        private void update() {
            if (stepsAmount == 0) {
                alphaPC.to = 0.0F;
            }
            if (currentStepTicks > 0) {
                currentStepTicks -= slowSpeed.get() ? 1 : 2;
                if (currentStepTicks < 0) {
                    currentStepTicks = 0;
                }
            } else {
                Vec3i last = vecGens.get(vecGens.size() - 1);
                boolean added = false;
                for (int attempt = 6; attempt > 0; --attempt) {
                    setA90R(lastYawPitch, nextYawPitch);
                    int step = randInt(lineMoveSteps()[0], lineMoveSteps()[1]);
                    Vec3i next = offsetFromRXYR(last, nextYawPitch, step);
                    if (isSpawnPosFree(next)) {
                        lastYawPitch[0] = nextYawPitch[0];
                        lastYawPitch[1] = nextYawPitch[1];
                        lastStepSet = currentStepTicks = step;
                        vecGens.add(next);
                        --stepsAmount;
                        added = true;
                        break;
                    }
                }
                if (!added) {
                    setWantToRemove();
                }
            }
        }

        public List<Vec3d> getPosVectors(float pTicks) {
            return getSmoothTickedFromList(vecGens, moveAdvanceFromTicks(lastStepSet, currentStepTicks, pTicks));
        }

        public float getAlphaPC() {
            return MathHelper.clamp(alphaPC.getAnim(), 0.0F, 1.0F);
        }

        public void setWantToRemove() {
            stepsAmount = 0;
        }

        public boolean isToRemove(float moduleAlphaPC, ClippingHelper frustum) {
            return moduleAlphaPC * (alphaPC.to == 0.0F ? getAlphaPC() : 1.0F) * 255.0F < 1.0F;
        }
    }

    private static class GliphVecRenderer {
        private static void set3DRendering(boolean bloom, Runnable render) {
            GL11.glPushMatrix();
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA,
                    bloom ? GlStateManager.DestFactor.ONE : GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                    GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            RenderSystem.disableTexture();
            RenderSystem.disableCull();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.shadeModel(GL11.GL_SMOOTH);
            RenderSystem.alphaFunc(GL11.GL_GREATER, 0.003921569F);
            GL11.glLineWidth(1.0F);
            GL11.glPointSize(1.0F);
            GL11.glEnable(GL11.GL_POINT_SMOOTH);
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
            Minecraft mc = Minecraft.getInstance();
            if (mc != null) {
                mc.gameRenderer.getLightTexture().disableLightmap();
            }
            render.run();
            GL11.glLineWidth(1.0F);
            GL11.glPointSize(1.0F);
            GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_DONT_CARE);
            GL11.glDisable(GL11.GL_LINE_SMOOTH);
            GL11.glDisable(GL11.GL_POINT_SMOOTH);
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.alphaFunc(GL11.GL_GREATER, 0.1F);
            RenderSystem.shadeModel(GL11.GL_FLAT);
            RenderSystem.enableTexture();
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            if (mc != null) {
                mc.gameRenderer.getLightTexture().enableLightmap();
            }
            if (bloom) {
                RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                        GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
            }
            RenderSystem.disableBlend();
            GL11.glPopMatrix();
        }

        private static float calcLineWidth(GliphsVecGen gen) {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) {
                return 1.0F;
            }
            double cameraX = mc.getRenderManager().renderPosX();
            double cameraY = mc.getRenderManager().renderPosY();
            double cameraZ = mc.getRenderManager().renderPosZ();
            Vec3i pos = null;
            double maxDistanceSq = Double.NEGATIVE_INFINITY;
            for (Vec3i vec3i : gen.vecGens) {
                double dx = vec3i.getX() - cameraX;
                double dy = vec3i.getY() - cameraY;
                double dz = vec3i.getZ() - cameraZ;
                double distanceSq = dx * dx + dy * dy + dz * dz;
                if (distanceSq > maxDistanceSq) {
                    maxDistanceSq = distanceSq;
                    pos = vec3i;
                }
            }
            if (pos == null) {
                return 1.0E-4F;
            }
            double eyeY = cameraY + (mc.player == null ? 0.0F : mc.player.getEyeHeight());
            double dx = cameraX - pos.getX();
            double dy = eyeY - pos.getY();
            double dz = cameraZ - pos.getZ();
            double dst = MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
            return 1.0E-4F + 3.0F * (float) MathHelper.clamp(1.0F - dst / 20.0F, 0.0F, 1.0F);
        }

        private static void clientColoredBegin(LineGlyphs owner, GliphsVecGen gen, int objIndex, int colorIndexStep, float alphaPC, float pTicks,
                                               ClippingHelper frustum, float lineWidthMul, float lineWidthAddPerm, MatrixStack stack) {
            if (alphaPC * 255.0F < 1.0F || gen.vecGens.size() < 2) {
                return;
            }
            float lineWidth = calcLineWidth(gen);
            GL11.glLineWidth(Math.min(lineWidth * lineWidthMul + lineWidthAddPerm, 40.0F));
            BufferBuilder buffer = tessellator.getBuffer();
            Matrix4f mat = stack.getLast().getMatrix();
            buffer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
            int colorIndex = objIndex;
            int index = 0;
            for (Vec3d vec3d : gen.getPosVectors(pTicks)) {
                float aPC = alphaPC * (0.25F + (float) index / (float) gen.vecGens.size() / 1.75F);
                buffer.pos(mat, (float) vec3d.xCoord, (float) vec3d.yCoord, (float) vec3d.zCoord).color(owner.stateColor(colorIndex, aPC)).endVertex();
                colorIndex += colorIndexStep;
                ++index;
            }
            tessellator.draw();
            GL11.glPointSize(Math.min(lineWidth * 3.0F * lineWidthMul + lineWidthAddPerm, 40.0F));
            buffer.begin(GL11.GL_POINTS, DefaultVertexFormats.POSITION_COLOR);
            colorIndex = objIndex;
            index = 0;
            for (Vec3d vec3d : gen.getPosVectors(pTicks)) {
                float aPC = alphaPC * (0.25F + (float) index / (float) gen.vecGens.size() / 1.75F);
                buffer.pos(mat, (float) vec3d.xCoord, (float) vec3d.yCoord, (float) vec3d.zCoord).color(owner.stateColor(colorIndex, aPC)).endVertex();
                colorIndex += colorIndexStep;
                ++index;
            }
            tessellator.draw();
        }
    }
}
