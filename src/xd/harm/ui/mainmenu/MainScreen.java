package xd.harm.ui.mainmenu;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MainWindow;
import net.minecraft.client.gui.AnimatedBackgroundRenderer;
import net.minecraft.client.gui.screen.MultiplayerScreen;
import net.minecraft.client.gui.screen.MultiplayerWarningScreen;
import net.minecraft.client.gui.screen.OptionsScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.WorldSelectionScreen;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.text.ITextComponent;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import xd.harm.Harmony;
import xd.harm.modules.impl.render.Theme;
import xd.harm.utils.client.DiscordRPC;
import xd.harm.utils.client.IMinecraft;
import xd.harm.utils.math.MathUtil;
import xd.harm.utils.render.color.ColorUtils;
import xd.harm.utils.render.font.Fonts;
import xd.harm.utils.render.rect.RenderUtility;
import xd.harm.utils.text.font.ClientFonts;

import java.util.ArrayList;
import java.util.List;

public class MainScreen extends Screen implements IMinecraft {
    private static final ResourceLocation SETKA_IMAGE = new ResourceLocation("harmony/images/mainscreen/setka.png");
    private static final float SETKA_WIDTH = 1082f;
    private static final float SETKA_HEIGHT = 1922f;
    private static final float SETKA_SCALE = 1f;
    private static final float LOGO_X_OFFSET = -2.5f;
    private static final float LOGO_FLOAT_AMPLITUDE = 3.0f;
    private static final float LOGO_SHINE_WIDTH = 10.0f;
    private static final long TRANSITION_DURATION = 260L;
    private static final int DARK_BLUE_ACCENT = ColorUtils.rgba(28, 72, 190, 255);
    private static final int CYAN_ACCENT = ColorUtils.rgba(58, 198, 255, 255);
    private static boolean rpcStarted;
    private final List<MenuButton> buttons = new ArrayList<>();
    private final List<MenuButton> smallButtons = new ArrayList<>();
    private long openedAt;
    private boolean opening;
    private long openStartedAt;
    private boolean transitioning;
    private long transitionStartedAt;
    private Screen transitionTarget;
    private float transitionProgress;
    private float contentAlpha = 1f;
    private float contentOffsetY;
    private float setkaImageWidth;
    private float setkaImageHeight;
    private float logoWidth = -1f;
    private float logoHeight;

    public MainScreen() {
        super(ITextComponent.getTextComponentOrEmpty(""));
        if (!rpcStarted) {
            rpcStarted = true;
            try {
                DiscordRPC.startRPC();
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void init(Minecraft minecraft, int width, int height) {
        super.init(minecraft, width, height);
        openedAt = System.currentTimeMillis();
        opening = true;
        openStartedAt = openedAt;
        transitioning = false;
        transitionTarget = null;
        transitionProgress = 0f;
        contentAlpha = 0f;
        contentOffsetY = 0f;

        buttons.clear();
        smallButtons.clear();

        float centerX = this.width / 2f;
        float setkaScale = Math.max(width / SETKA_HEIGHT, height / SETKA_WIDTH) * SETKA_SCALE;
        setkaImageWidth = SETKA_WIDTH * setkaScale;
        setkaImageHeight = SETKA_HEIGHT * setkaScale;

        float buttonW = MathUtil.clamp(this.width * 0.124f, 108f, 216f);
        float buttonH = buttonW * 0.18f;
        float gap = buttonH * 0.32f;
        float x = centerX - buttonW / 2f;
        float y = this.height / 2f - buttonH * 2.52f;

        buttons.add(new MenuButton(x, y, buttonW, buttonH, "Singleplayer", "E", ClientFonts.icons_nur[17],
                () -> startTransition(new WorldSelectionScreen(this)), 2f, 0f));
        y += buttonH + gap;

        buttons.add(new MenuButton(x, y, buttonW, buttonH, "Multiplayer", "N", ClientFonts.icons_nur[16],
                () -> startTransition(this.minecraft.gameSettings.skipMultiplayerWarning
                        ? new MultiplayerScreen(this)
                        : new MultiplayerWarningScreen(this)), 2f, 0f));
        y += buttonH + gap;

        buttons.add(new MenuButton(x, y, buttonW, buttonH, "Account Manager", "B", ClientFonts.icons_wex[22],
                () -> startTransition(resolveAltScreen()), 2f, 0f));

        float small = buttonH;
        float smallY = y + buttonH + buttonH * 0.36f;
        float iconGap = small * 0.28f;
        float centerIconX = x + buttonW / 2f - small - iconGap / 2f;
        smallButtons.add(new MenuButton(x, smallY, small, small, "", "H", ClientFonts.icons_wex[22],
                () -> startTransition(new OptionsScreen(this, this.minecraft.gameSettings)), -0.5f, 1f));
        smallButtons.add(new MenuButton(centerIconX, smallY, small, small, "", "D", ClientFonts.icon[40],
                () -> openExternalLink("https://dsc.gg/harmonyclient"), DARK_BLUE_ACCENT, - 1f, 3f));
        smallButtons.add(new MenuButton(centerIconX + small + iconGap, smallY, small, small, "", "C", ClientFonts.icon[40],
                () -> openExternalLink("https://t.me/harmonyclientnews"), CYAN_ACCENT, - 1f, 3f));
        smallButtons.add(new MenuButton(x + buttonW - small, smallY, small, small, "", "k", ClientFonts.icons_client[17],
                () -> startTransition(new ExitScreen()), true, 0f, 0f));
    }

    private void openExternalLink(String url) {
        Util.getOSType().openURI(url);
    }

    private Screen resolveAltScreen() {
        Harmony harmony = Harmony.getInstance();
        if (harmony != null && harmony.getAltScreen() != null) {
            return harmony.getAltScreen();
        }
        return new AltScreen();
    }

    @Override
    public void render(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        updateTransition();

        GL11.glClearColor(0.006f, 0.006f, 0.008f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

        float time = (System.currentTimeMillis() - openedAt) / 1000f;
        AnimatedBackgroundRenderer.draw(mc, width, height, 1.0f, time,
                AnimatedBackgroundRenderer.MAIN_MENU_PROGRESS * contentAlpha,
                AnimatedBackgroundRenderer.MAIN_MENU_EXIT_PROGRESS + transitionProgress * (1.0f - AnimatedBackgroundRenderer.MAIN_MENU_EXIT_PROGRESS));
        drawDarkOverlay(stack);
        drawSetkaOverlay(stack);
        drawMonoLogo(stack, width / 2f, height / 2f - 108f);

        for (MenuButton button : buttons) {
            button.render(stack, mouseX, mouseY, partialTicks);
        }
        for (MenuButton button : smallButtons) {
            button.render(stack, mouseX, mouseY, partialTicks);
        }

        drawVersionAndTags(stack);
        mc.gameRenderer.setupOverlayRendering();
    }

    private void startTransition(Screen target) {
        if (opening || transitioning) {
            return;
        }
        transitioning = true;
        transitionStartedAt = System.currentTimeMillis();
        transitionTarget = target;
        transitionProgress = 0f;
    }

    private void updateTransition() {
        if (opening) {
            long elapsed = System.currentTimeMillis() - openStartedAt;
            float progress = Math.min(1f, (float) elapsed / TRANSITION_DURATION);
            transitionProgress = 0f;
            contentAlpha = easeOutCubic(progress);
            contentOffsetY = 0f;

            if (progress >= 1f) {
                opening = false;
                contentAlpha = 1f;
            }
            return;
        }

        if (!transitioning) {
            transitionProgress = 0f;
            contentAlpha = 1f;
            contentOffsetY = 0f;
            return;
        }

        long elapsed = System.currentTimeMillis() - transitionStartedAt;
        float progress = Math.min(1f, (float) elapsed / TRANSITION_DURATION);
        transitionProgress = easeInCubic(progress);
        contentAlpha = 1f - transitionProgress;
        contentOffsetY = 0f;

        if (progress >= 1f) {
            Screen target = transitionTarget;
            transitioning = false;
            transitionTarget = null;
            transitionProgress = 0f;
            minecraft.displayGuiScreen(target);
        }
    }

    private void drawDarkOverlay(MatrixStack stack) {
        RenderUtility.drawRect(stack, 0, 0, width, height, ColorUtils.rgba(4, 4, 5, 176));
    }

    private void drawMonoLogo(MatrixStack stack, float centerX, float y) {
        float time = (System.currentTimeMillis() - openedAt) / 1000f;
        float floatY = (float) Math.sin(time * 1.85f) * LOGO_FLOAT_AMPLITUDE;
        ensureLogoMetrics();
        float logoX = centerX - logoWidth / 2.5f + LOGO_X_OFFSET;
        float logoY = y + floatY + contentOffsetY;

        int themeColor = Theme.MainColor((int) (time * 120f));
        int themeLight = ColorUtils.brighter(themeColor, 0.42f);
        float shineProgress = (time * 0.55f) % 1.0f;
        float shineX = logoX - LOGO_SHINE_WIDTH + shineProgress * (logoWidth + LOGO_SHINE_WIDTH * 2.0f);

        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        ClientFonts.watermark[56].drawString(stack, "a", logoX - 1.6f, logoY + 1.4f,
                alphaColor(ColorUtils.rgba(0, 0, 0, 72)));
        ClientFonts.watermark[56].drawString(stack, "a", logoX, logoY,
                alphaColor(ColorUtils.setAlpha(themeColor, 118)));
        ClientFonts.watermark[56].drawString(stack, "a", logoX + 0.8f, logoY - 0.8f,
                alphaColor(ColorUtils.setAlpha(themeLight, 44)));

        enableGuiScissor(shineX, logoY - 2.0f, LOGO_SHINE_WIDTH, logoHeight + 4.0f);
        ClientFonts.watermark[56].drawString(stack, "a", logoX + 0.3f, logoY - 0.3f,
                alphaColor(ColorUtils.rgba(255, 255, 255, 150)));
        ClientFonts.watermark[56].drawString(stack, "a", logoX + 1.0f, logoY - 0.8f,
                alphaColor(ColorUtils.setAlpha(themeLight, 95)));
        RenderSystem.disableScissor();

        GlStateManager.disableBlend();
    }

    private void ensureLogoMetrics() {
        if (logoWidth < 0f) {
            logoWidth = ClientFonts.watermark[56].getWidth("a");
            logoHeight = ClientFonts.watermark[56].getFontHeight();
        }
    }

    private void enableGuiScissor(float x, float y, float width, float height) {
        MainWindow window = mc.getMainWindow();
        double scale = window.getGuiScaleFactor();
        int scissorX = (int) Math.floor(x * scale);
        int scissorY = (int) Math.floor((this.height - y - height) * scale);
        int scissorW = (int) Math.ceil(width * scale);
        int scissorH = (int) Math.ceil(height * scale);
        RenderSystem.enableScissor(scissorX, scissorY, scissorW, scissorH);
    }

    private void drawSetkaOverlay(MatrixStack stack) {
        float time = (System.currentTimeMillis() - openedAt) / 1000f;
        int setkaColor = ColorUtils.setAlpha(Theme.MainColor((int) (time * 120f)), 255);

        stack.push();
        stack.translate(width / 2f, height / 2f, 0);
        stack.rotate(Vector3f.ZP.rotationDegrees(90f));
        RenderUtility.drawImageAlphaSmooth(stack, SETKA_IMAGE, -setkaImageWidth / 2f, -setkaImageHeight / 2f, setkaImageWidth, setkaImageHeight,
                setkaColor);
        stack.pop();
    }

    private void drawVersionAndTags(MatrixStack stack) {
        float centerX = width / 2f;
        ClientFonts.interMedium[12].drawCenteredString(stack, "V 1.00", centerX, height / 2f + 142f + contentOffsetY,
                alphaColor(ColorUtils.rgba(80, 78, 84, 52)));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (opening || transitioning) {
            return true;
        }
        if (button == 0) {
            for (MenuButton menuButton : buttons) {
                if (menuButton.click(mouseX, mouseY)) {
                    return true;
                }
            }
            for (MenuButton menuButton : smallButtons) {
                if (menuButton.click(mouseX, mouseY)) {
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return keyCode == GLFW.GLFW_KEY_ESCAPE || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private boolean isInside(double mouseX, double mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private float easeInCubic(float x) {
        return x * x * x;
    }

    private float easeOutCubic(float x) {
        float inverted = 1f - x;
        return 1f - inverted * inverted * inverted;
    }

    private int alphaColor(int color) {
        return ColorUtils.setAlpha(color, clampAlpha((int) (ColorUtils.getAlpha(color) * contentAlpha)));
    }

    private int clampAlpha(int alpha) {
        return Math.max(0, Math.min(255, alpha));
    }

    private final class MenuButton {
        private final float x;
        private final float y;
        private final float width;
        private final float height;
        private final String text;
        private final String icon;
        private final Object iconFont;
        private final Runnable action;
        private final boolean exit;
        private final int accentColor;
        private final float iconOffsetX;
        private final float iconOffsetY;
        private final float iconWidth;
        private final float iconHeight;
        private final float textSize;
        private float textWidth;
        private final float spacing;
        private float hover;

        private MenuButton(float x, float y, float width, float height, String text, String icon, Object iconFont, Runnable action) {
            this(x, y, width, height, text, icon, iconFont, action, false, 0f, 0f);
        }

        private MenuButton(float x, float y, float width, float height, String text, String icon, Object iconFont, Runnable action, boolean exit) {
            this(x, y, width, height, text, icon, iconFont, action, exit, 0f, 0f);
        }

        private MenuButton(float x, float y, float width, float height, String text, String icon, Object iconFont,
                           Runnable action, float iconOffsetX, float iconOffsetY) {
            this(x, y, width, height, text, icon, iconFont, action, false, iconOffsetX, iconOffsetY);
        }

        private MenuButton(float x, float y, float width, float height, String text, String icon, Object iconFont,
                           Runnable action, int accentColor, float iconOffsetX, float iconOffsetY) {
            this(x, y, width, height, text, icon, iconFont, action, false, accentColor, iconOffsetX, iconOffsetY);
        }

        private MenuButton(float x, float y, float width, float height, String text, String icon, Object iconFont,
                           Runnable action, boolean exit, float iconOffsetX, float iconOffsetY) {
            this(x, y, width, height, text, icon, iconFont, action, exit, 0, iconOffsetX, iconOffsetY);
        }

        private MenuButton(float x, float y, float width, float height, String text, String icon, Object iconFont,
                           Runnable action, boolean exit, int accentColor, float iconOffsetX, float iconOffsetY) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.text = text;
            this.icon = icon;
            this.iconFont = iconFont;
            this.action = action;
            this.exit = exit;
            this.accentColor = accentColor;
            this.iconOffsetX = iconOffsetX;
            this.iconOffsetY = iconOffsetY;
            this.iconWidth = measureIconWidth(icon);
            this.iconHeight = measureIconHeight();
            this.textSize = text.isEmpty() ? 0f : MathUtil.clamp(height * 0.38f, 7.2f, 15.0f);
            this.textWidth = text.isEmpty() ? 0f : -1f;
            this.spacing = text.isEmpty() ? 0f : MathUtil.clamp(height * 0.14f, 4.5f, 6.0f);
        }

        private void render(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
            float renderY = y + contentOffsetY;
            boolean hovered = !opening && !transitioning && isInside(mouseX, mouseY, x, y, width, height);
            float delta = MathUtil.clamp(0.10f + partialTicks * 0.075f, 0.10f, 0.22f);
            hover += ((hovered ? 1f : 0f) - hover) * delta;

            int themeCol = ColorUtils.getColor(0);
            int tr = ColorUtils.getRed(themeCol);
            int tg = ColorUtils.getGreen(themeCol);
            int tb = ColorUtils.getBlue(themeCol);
            boolean accented = accentColor != 0;
            int ar = accented ? ColorUtils.getRed(accentColor) : tr;
            int ag = accented ? ColorUtils.getGreen(accentColor) : tg;
            int ab = accented ? ColorUtils.getBlue(accentColor) : tb;

            int bg = accented
                    ? ColorUtils.rgba(8 + (int) (hover * (ar / 10 + 10)), 10 + (int) (hover * (ag / 12 + 6)), 18 + (int) (hover * (ab / 12 + 8)), 126 + (int) (hover * 58))
                    : exit
                    ? ColorUtils.rgba(14 + (int) (hover * 20), 13 + (int) (hover * 6), 16 + (int) (hover * 7), 106 + (int) (hover * 50))
                    : ColorUtils.rgba(14 + (int) (hover * (tr / 12 + 6)), 14 + (int) (hover * (tg / 12 + 4)), 17 + (int) (hover * (tb / 12 + 4)), 106 + (int) (hover * 52));
            int border = accented
                    ? ColorUtils.rgba(ar, ag, ab, 82 + (int) (hover * 132))
                    : exit
                    ? ColorUtils.rgba(155, 70, 84, 45 + (int) (hover * 96))
                    : ColorUtils.rgba(
                        (int) (80 + hover * (tr - 80)),
                        (int) (80 + hover * (tg - 80)),
                        (int) (85 + hover * (tb - 85)),
                        50 + (int) (hover * 102));
            int textColor = ColorUtils.rgba(
                    Math.min(255, (int) (178 + hover * (tr / 2.5f))),
                    Math.min(255, (int) (178 + hover * (tg / 2.5f))),
                    Math.min(255, (int) (186 + hover * (tb / 2.5f))),
                    228);
            int iconColor = accented
                    ? ColorUtils.rgba(Math.min(255, ar + (int) (hover * 54)), Math.min(255, ag + (int) (hover * 54)), Math.min(255, ab + (int) (hover * 34)), 238)
                    : exit
                    ? ColorUtils.rgba(196 + (int) (hover * 42), 84, 100, 228)
                    : ColorUtils.rgba(
                        Math.min(255, (int) (170 + hover * (tr / 2f))),
                        Math.min(255, (int) (170 + hover * (tg / 2f))),
                        Math.min(255, (int) (180 + hover * (tb / 2f))),
                        228);
            if (text.isEmpty()) {
                float iconX = x + width / 2f - iconWidth / 2.3f + iconOffsetX;
                float iconY = renderY + height / 1.7f - iconHeight / 2f + 1f + iconOffsetY;
                float smoothGlow = accented ? 0.14f + hover * 0.18f : hover * hover * 0.16f;

                RenderUtility.drawMenuButton(x, renderY, width, height, 5f,
                        alphaColor(bg),
                        alphaColor(ColorUtils.rgba(16 + (int) (hover * (ar / 14)), 16 + (int) (hover * (ag / 14)), 18 + (int) (hover * (ab / 14)), 116 + (int) (hover * 46))),
                        alphaColor(ColorUtils.rgba(9, 9, 11, 106 + (int) (hover * 38))),
                        alphaColor(ColorUtils.rgba(14 + (int) (hover * (ar / 16)), 14 + (int) (hover * (ag / 16)), 16 + (int) (hover * (ab / 16)), 112 + (int) (hover * 42))),
                        alphaColor(border), 0.8f + hover * 0.32f, smoothGlow * contentAlpha, 18f);

                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                if (accented) {
                    int glowColor = alphaColor(ColorUtils.rgba(ar, ag, ab, 48 + (int) (hover * 86)));
                    drawIcon(stack, icon, iconX - 0.8f, iconY, glowColor);
                    drawIcon(stack, icon, iconX + 0.8f, iconY, glowColor);
                    drawIcon(stack, icon, iconX, iconY - 0.8f, glowColor);
                    drawIcon(stack, icon, iconX, iconY + 0.8f, glowColor);
                    drawIcon(stack, icon, iconX, iconY, alphaColor(iconColor));
                    GlStateManager.disableBlend();
                    return;
                }

                drawIcon(stack, icon, iconX, iconY, alphaColor(iconColor));
                GlStateManager.disableBlend();
                return;
            }

            float smoothGlow = hover * hover * 0.16f;
            RenderUtility.drawMenuButton(x, renderY, width, height, 5f,
                    alphaColor(bg),
                    alphaColor(ColorUtils.rgba(16 + (int) (hover * (ar / 14)), 16 + (int) (hover * (ag / 14)), 18 + (int) (hover * (ab / 14)), 116 + (int) (hover * 46))),
                    alphaColor(ColorUtils.rgba(9, 9, 11, 106 + (int) (hover * 38))),
                    alphaColor(ColorUtils.rgba(14 + (int) (hover * (ar / 16)), 14 + (int) (hover * (ag / 16)), 16 + (int) (hover * (ab / 16)), 112 + (int) (hover * 42))),
                    alphaColor(border), 0.8f + hover * 0.32f, smoothGlow * contentAlpha, 18f);

            if (Fonts.sfuy == null) {
                float iconX = x + width / 2f - iconWidth / 2f + iconOffsetX;
                float iconY = renderY + height / 1.7f - iconHeight / 2f + MathUtil.clamp(height * 0.05f, 0.8f, 1.6f) + iconOffsetY;

                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                drawIcon(stack, icon, iconX, iconY, alphaColor(iconColor));
                GlStateManager.disableBlend();
                return;
            }

            float currentTextWidth = getTextWidth();
            float totalWidth = iconWidth + spacing + currentTextWidth;
            float startX = x + width / 2f - totalWidth / 2f;
            float iconY = renderY + height / 1.7f - iconHeight / 2f + MathUtil.clamp(height * 0.05f, 0.8f, 1.6f);
            float textY = renderY + height / 2f - Fonts.sfuy.getHeight(textSize) / 2f + 0.4f;

            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            drawIcon(stack, icon, startX + iconOffsetX, iconY + iconOffsetY, alphaColor(iconColor));
            GlStateManager.disableBlend();
            Fonts.sfuy.drawText(stack, text, startX + iconWidth + spacing, textY, alphaColor(textColor), textSize);
        }

        private float getTextWidth() {
            if (textWidth < 0f) {
                textWidth = Fonts.sfuy.getWidth(text, textSize);
            }
            return textWidth;
        }

        private boolean click(double mouseX, double mouseY) {
            if (!isInside(mouseX, mouseY, x, y, width, height)) {
                return false;
            }
            action.run();
            return true;
        }

        private float measureIconWidth(String value) {
            if (iconFont instanceof xd.harm.utils.text.font.styled.StyledFont) {
                return ((xd.harm.utils.text.font.styled.StyledFont) iconFont).getWidth(value);
            }
            if (ClientFonts.icons_nur[17] != null) {
                return ClientFonts.icons_nur[17].getWidth(value);
            }
            return value.length() * 8f;
        }

        private float measureIconHeight() {
            if (iconFont instanceof xd.harm.utils.text.font.styled.StyledFont) {
                return ((xd.harm.utils.text.font.styled.StyledFont) iconFont).getFontHeight();
            }
            if (ClientFonts.icons_nur[17] != null) {
                return ClientFonts.icons_nur[17].getFontHeight();
            }
            return 16f;
        }

        private void drawIcon(MatrixStack stack, String value, float x, float y, int color) {
            if (iconFont instanceof xd.harm.utils.text.font.styled.StyledFont) {
                ((xd.harm.utils.text.font.styled.StyledFont) iconFont).drawString(stack, value, x, y, color);
            } else if (ClientFonts.icons_nur[17] != null) {
                ClientFonts.icons_nur[17].drawString(stack, value, x, y, color);
            }
        }
    }
}
