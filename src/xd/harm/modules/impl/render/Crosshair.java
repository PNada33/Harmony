package xd.harm.modules.impl.render;

import xd.harm.utils.render.color.ColorUtils;
import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import xd.harm.events.render.EventDisplay;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.ColorSetting;
import xd.harm.modules.settings.impl.SliderSetting;
import xd.harm.modules.settings.impl.ModeSetting;
import xd.harm.utils.math.MathUtil;
import xd.harm.utils.render.rect.RenderUtility;
import lombok.Getter;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.settings.PointOfView;
import net.minecraft.util.math.MathHelper;
import com.mojang.blaze3d.matrix.MatrixStack;
import org.lwjgl.opengl.GL11;

import java.awt.*;

@ModuleRegister(name = "Crosshair", category = Category.Render, desc = "Прицел чо ещё сказать")
public class Crosshair extends Module {
    private static final String MODE_CLASSIC = "Класический";
    private static final String MODE_SYMBOL = "Символ";
    private static final String MODE_CUSTOM = "Кастомный";

    public final ModeSetting mode = new ModeSetting("Вид", MODE_SYMBOL, MODE_CLASSIC, MODE_SYMBOL, MODE_CUSTOM);
    public final SliderSetting lSpeed = new SliderSetting("Скорость", 2.0f, 0.1f, 10.0f, 0.1f).setVisible(() -> mode.is(MODE_SYMBOL));
    public final SliderSetting lSize = new SliderSetting("Размер", 3.5f, 2.1f, 5.1f, 0.1f).setVisible(() -> mode.is(MODE_SYMBOL));

    public final BooleanSetting animate = new BooleanSetting("Анимация удара", true);
    public final BooleanSetting dot = new BooleanSetting("Точка", true).setVisible(() -> mode.is("Кастомный"));
    public final BooleanSetting tCrosshair = new BooleanSetting("T-образный", false).setVisible(() -> mode.is("Кастомный"));
    public final SliderSetting width1 = new SliderSetting("Ширина", 5, 0, 10, 1).setVisible(() -> mode.is("Кастомный"));
    public final SliderSetting height1 = new SliderSetting("Высота", 5, 0, 10, 1).setVisible(() -> mode.is("Кастомный"));
    public final SliderSetting gap = new SliderSetting("Промежуток", 3, 0, 10, 1).setVisible(() -> mode.is("Кастомный"));
    public final SliderSetting thickness = new SliderSetting("Толщина", 1, 1, 10, 1).setVisible(() -> mode.is("Кастомный"));

    public final ModeSetting colorMode = new ModeSetting("Цвет", "Белый", "Белый", "Клиент", "Свой");
    public final ColorSetting crosshairColor = new ColorSetting("Цвет прицела", ColorUtils.rgb(255, 255, 255)).setVisible(() -> colorMode.is("Свой"));

    @Getter
    public float animatedYaw, x;
    @Getter
    public float animatedPitch, y;
    private float animationSize;
    private float lRotation = 0;
    private long lastTime = System.currentTimeMillis();
    private final int outlineColor = Color.BLACK.getRGB();
    private final float[] symbolSegments = new float[32];

    private float animationValue = 0;
    private float animationSpeed = 0.05f;

    public Crosshair() {
        addSettings(mode, colorMode, crosshairColor, lSpeed, lSize, animate, dot, tCrosshair, width1, height1, gap, thickness);
    }

    private int getCrosshairColor() {
        if (colorMode.is("Тема")) {
            return Theme.MainColor(0);
        } else if (colorMode.is("Свой")) {
            return crosshairColor.get();
        } else {
            return Color.WHITE.getRGB();
        }
    }

    @Subscribe
    public void onDisplay(EventDisplay e) {
        if (mc.player == null || mc.world == null || e.getType() != EventDisplay.Type.POST) {
            return;
        }
        normalizeMode();

        x = mc.getMainWindow().getScaledWidth() / 2f;
        y = mc.getMainWindow().getScaledHeight() / 2f;

        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastTime) / 16.67f;
        lastTime = currentTime;

        boolean firstPerson = mc.gameSettings.getPointOfView() == PointOfView.FIRST_PERSON;
        if (animate.get()) {
            float swingProgress = mc.player.swingProgress;
            float sin = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
            float target = sin;
            animationValue = animationValue + (target - animationValue) * animationSpeed;
        }

        switch (mode.getIndex()) {
            case 0 -> {
                if (!firstPerson) return;

                animationSize = MathUtil.fast(animationSize, (1 - mc.player.getCooledAttackStrength(0)), 10);
                float thickness = 1;
                float length = 3;
                float gap = 2 + 8 * animationSize;

                int color = getCrosshairColor();

                drawOutlined(x - thickness / 2, y - gap - length, thickness, length, color);
                drawOutlined(x - thickness / 2, y + gap, thickness, length, color);

                drawOutlined(x - gap - length, y - thickness / 2, length, thickness, color);
                drawOutlined(x + gap, y - thickness / 2, length, thickness, color);
            }


            case 1 -> {
                if (!firstPerson) return;

                lRotation += lSpeed.get() * deltaTime;
                while (lRotation >= 360) {
                    lRotation -= 360;
                }
                while (lRotation < 0) {
                    lRotation += 360;
                }

                animationSize = MathUtil.fast(animationSize, (1 - mc.player.getCooledAttackStrength(0)) * 0.3f, 10);
                float baseSize = lSize.get() + animationSize;
                int color = getCrosshairColor();

                drawRotatingLs(x, y, baseSize, lRotation, color);
            }

            case 2 -> {
                if (!firstPerson) return;
                double centerX = x;
                double centerY = y;

                double cwidth = this.width1.get().doubleValue();
                double cheight = this.height1.get().doubleValue();

                int color = getCrosshairColor();

                float thickness = this.thickness.get().floatValue();

                float gap = (float) (this.gap.get().floatValue() + (animate.get() ? (animationValue * 7) : 0));
                if (dot.get()) {
                    RenderUtility.drawRect((float) (centerX - 0.5), (float) (centerY - 0.5), (float) (centerX + 0.5), (float) (centerY + 0.5), color);
                }

                if (!tCrosshair.get()) {
                    RenderUtility.drawRect(
                            (float) (centerX - (thickness / 2F) - 0.5f),
                            (float) (centerY - gap - cheight - 0.5f),
                            (float) (centerX + (thickness / 2F) + 0.5f),
                            (float) (centerY - gap + 0.5f),
                            outlineColor);
                    RenderUtility.drawRect(
                            (float) (centerX - (thickness / 2F)),
                            (float) (centerY - gap - cheight),
                            (float) (centerX + (thickness / 2F)),
                            (float) (centerY - gap),
                            color);
                }

                RenderUtility.drawRect(
                        (float) (centerX - (thickness / 2F) - 0.5f),
                        (float) (centerY + gap - 0.5f),
                        (float) (centerX + (thickness / 2F) + 0.5f),
                        (float) (centerY + gap + cheight + 0.5f),
                        outlineColor);
                RenderUtility.drawRect(
                        (float) (centerX - (thickness / 2F)),
                        (float) (centerY + gap),
                        (float) (centerX + (thickness / 2F)),
                        (float) (centerY + gap + cheight),
                        color);

                RenderUtility.drawRect(
                        (float) (centerX - gap - cwidth - 0.5f),
                        (float) (centerY - (thickness / 2F) - 0.5f),
                        (float) (centerX - gap + 0.5f),
                        (float) (centerY + (thickness / 2F) + 0.5f),
                        outlineColor);
                RenderUtility.drawRect(
                        (float) (centerX - gap - cwidth),
                        (float) (centerY - (thickness / 2F)),
                        (float) (centerX - gap),
                        (float) (centerY + (thickness / 2F)),
                        color);

                RenderUtility.drawRect(
                        (float) (centerX + gap - 0.5f),
                        (float) (centerY - (thickness / 2F) - 0.5f),
                        (float) (centerX + gap + cwidth + 0.5f),
                        (float) (centerY + (thickness / 2F) + 0.5f),
                        outlineColor);
                RenderUtility.drawRect(
                        (float) (centerX + gap),
                        (float) (centerY - (thickness / 2F)),
                        (float) (centerX + gap + cwidth),
                        (float) (centerY + (thickness / 2F)),
                        color);
            }
        }
    }

    private void normalizeMode() {
        for (String allowedMode : mode.strings) {
            if (allowedMode.equalsIgnoreCase(mode.get())) {
                return;
            }
        }
        mode.set(MODE_SYMBOL);
    }

    private void drawRotatingLs(float centerX, float centerY, float size, float rotation, int color) {
        centerX = (float) Math.floor(centerX) + 0.5f;
        centerY = (float) Math.floor(centerY) + 0.5f;

        double rotationRad = Math.toRadians(rotation);
        float cos = (float) Math.cos(rotationRad);
        float sin = (float) Math.sin(rotationRad);
        float arm = Math.max(2.4f, size + 0.45f);
        float hook = arm * 0.72f;
        float centerGap = 0f;

        int segmentIndex = 0;
        for (int i = 0; i < 4; i++) {
            float dirX;
            float dirY;
            switch (i) {
                case 1:
                    dirX = -sin;
                    dirY = cos;
                    break;
                case 2:
                    dirX = -cos;
                    dirY = -sin;
                    break;
                case 3:
                    dirX = sin;
                    dirY = -cos;
                    break;
                default:
                    dirX = cos;
                    dirY = sin;
                    break;
            }

            float perpX = -dirY;
            float perpY = dirX;
            float startX = centerX + dirX * centerGap;
            float startY = centerY + dirY * centerGap;
            float cornerX = centerX + dirX * arm;
            float cornerY = centerY + dirY * arm;
            float hookX = cornerX + perpX * hook;
            float hookY = cornerY + perpY * hook;

            segmentIndex = putSegment(segmentIndex, startX, startY, cornerX, cornerY);
            segmentIndex = putSegment(segmentIndex, cornerX, cornerY, hookX, hookY);
        }

        int segmentCount = segmentIndex / 4;
        drawLineSegments(symbolSegments, segmentCount, 3.1f, ColorUtils.rgba(0, 0, 0, 118));
        drawLineSegments(symbolSegments, segmentCount, 1.55f, ColorUtils.brighter(color, 0.08f));
        drawLineSegments(symbolSegments, segmentCount, 0.9f, ColorUtils.swapAlpha(ColorUtils.brighter(color, 0.42f), 178));
    }

    private int putSegment(int index, float x1, float y1, float x2, float y2) {
        symbolSegments[index++] = x1;
        symbolSegments[index++] = y1;
        symbolSegments[index++] = x2;
        symbolSegments[index++] = y2;
        return index;
    }

    private void drawLineSegments(float[] segments, int segmentCount, float width, int color) {
        if (segmentCount <= 0) {
            return;
        }

        float red = (float) (color >> 16 & 255) / 255.0F;
        float green = (float) (color >> 8 & 255) / 255.0F;
        float blue = (float) (color & 255) / 255.0F;
        float alpha = (float) (color >> 24 & 255) / 255.0F;

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.defaultBlendFunc();
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glLineWidth(width);

        buffer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        int limit = segmentCount * 4;
        for (int i = 0; i < limit; i += 4) {
            buffer.pos(segments[i], segments[i + 1], 0.0F).color(red, green, blue, alpha).endVertex();
            buffer.pos(segments[i + 2], segments[i + 3], 0.0F).color(red, green, blue, alpha).endVertex();
        }
        buffer.finishDrawing();
        WorldVertexBufferUploader.draw(buffer);

        GL11.glLineWidth(1.0F);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    private void drawOutlined(
            final float x,
            final float y,
            final float w,
            final float h,
            final int hex
    ) {
        RenderUtility.drawRectW(x - 0.5, y - 0.5, w + 1, h + 1, outlineColor);
        RenderUtility.drawRectW(x, y, w, h, hex);
    }
}
