package xd.harm.ui.clickgui.minidrop;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import xd.harm.Harmony;
import xd.harm.modules.impl.render.ClickGui;
import xd.harm.modules.impl.render.Theme;
import xd.harm.ui.clickgui.minidrop.components.ModuleComponent;
import xd.harm.utils.SoundUtil;
import xd.harm.utils.client.IMinecraft;
import xd.harm.utils.math.AnimationMath;
import xd.harm.ui.clickgui.minidrop.utils.MathUtil;
import xd.harm.ui.clickgui.minidrop.utils.ColorUtils;
import xd.harm.ui.clickgui.minidrop.utils.RenderHelper;
import xd.harm.utils.render.KawaseBlur;
import xd.harm.utils.render.font.Fonts;
import xd.harm.utils.render.rect.RenderUtility;
import xd.harm.utils.text.font.ClientFonts;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ClickGuiScreen extends Screen implements IMinecraft {

    private static final int TAB_COUNT = 6;
    private static final String[] TAB_ICONS = {"A", "B", "C", "D", "E", "F"};
    private static final String[] TAB_NAMES = {"Main", "Configs", "AutoBuy", "Theme", "BotConfigs", "Figura"};
    private static final float TAB_SPACING = 22.0f;
    private static final float TAB_PILL_WIDTH = 15.0f;
    private static final float TAB_PILL_HEIGHT = 14.0f;
    private static final float TAB_BAR_HEIGHT = 18.0f;
    private static final float TAB_BAR_RADIUS = 5.0f;
    private static final int TAB_ICON_FONT_SIZE = 25; // 'F' for Figura
    private static final float[] TAB_PILL_OFFSETS_X = {0.0f, 0.1f, -0.5f, -0.5f, -0.5f, -0.5f};

    public static float scale = 1F;
    private static ClickGuiScreen instance;
    private final List<Panel> panels = new ArrayList<>();
    private ModuleComponent expandedModule = null;
    private float updownPanel = 40;
    private float movePanel = 0;
    private boolean exit = false, open = false;
    private float globalAlpha = 0;
    private float scaleAnim = 1.5f;

    private float tabPillX = 0;
    private boolean tabPillInit = false;
    private final float[] tabHoverAnims = new float[TAB_COUNT];

    public static ClickGuiScreen getInstance() { return instance; }
    public ModuleComponent getExpandedModule() { return expandedModule; }
    public void setExpandedModule(ModuleComponent module) { this.expandedModule = module; }

    public ClickGuiScreen(ITextComponent titleIn) {
        super(titleIn);
        instance = this;
        for (xd.harm.modules.api.Category category : xd.harm.modules.api.Category.values()) {
            panels.add(new Panel(category));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        boolean animationsEnabled = ClickGui.mdAnimations.get();
        globalAlpha = animationsEnabled ? 0 : 1;
        scaleAnim = animationsEnabled ? 1.5f : 1f;
        exit = false;
        open = true;
        tabPillInit = false;
        super.init();
    }

    @Override
    public void closeScreen() {
        super.closeScreen();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (isCtrlDown()) {
            movePanel += (float) (delta * 5);
        } else {
            updownPanel -= (float) (delta * 20);
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private boolean isCtrlDown() {
        return GLFW.glfwGetKey(mc.getMainWindow().getHandle(), GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS ||
                GLFW.glfwGetKey(mc.getMainWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        boolean animationsEnabled = ClickGui.mdAnimations.get();

        if (animationsEnabled) {
            globalAlpha = exit ? MathUtil.fast(globalAlpha, 0, 10) : MathUtil.fast(globalAlpha, 1, 10);
            scaleAnim = exit ? MathUtil.fast(scaleAnim, 1.5f, 10) : MathUtil.fast(scaleAnim, 1f, 10);
        } else {
            globalAlpha = exit ? 0 : 1;
            scaleAnim = 1f;
        }

        if (globalAlpha <= 0.1f && exit) {
            closeScreen();
            return;
        }

        float off = 10.0F;
        float width = (float) panels.size() * 115;
        updateScaleBasedOnScreenWidth();
        int windowWidth = mc.getMainWindow().getScaledWidth();
        int windowHeight = mc.getMainWindow().getScaledHeight();

        if (ClickGui.mdBlur.get()) {
            KawaseBlur.blur.render(() -> {
                RenderHelper.drawRectW(0, 0, windowWidth, windowHeight, ColorUtils.rgba(0, 0, 0, (int) (128 * globalAlpha)));
            });
        }

        if (ClickGui.mdBackground.get()) {
            RenderHelper.drawRectHorizontalW(0, 0, windowWidth, windowHeight,
                    ColorUtils.getClickGuiColor(0, (int) (50 * globalAlpha)),
                    ColorUtils.rgba(0, 0, 0, 0));
        }

        RenderHelper.scaleStart(windowWidth / 2F, windowHeight / 2f, scaleAnim);

        for (Panel panel : panels) {
            float targetY = windowHeight / 2.0F - 110.0F - updownPanel;
            panel.setY(MathUtil.fast(panel.getY(), targetY, 10));
            panel.setX(((windowWidth / 2f) - (width / 2f) + panel.getCategory().ordinal() * (115 + off / 2) - off / 1.5f) - movePanel);
            panel.render(matrixStack, (float) mouseX, (float) mouseY);
        }

        RenderHelper.scaleEnd();

        renderTabBar(matrixStack, mouseX, mouseY, windowWidth);
    }

    private void renderTabBar(MatrixStack stack, int mouseX, int mouseY, int windowWidth) {
        int themeColor = Theme.MainColor(0);
        float barW = Math.min(windowWidth * 0.5f, 114.0f);
        float barX = (windowWidth - barW) / 2.0f;
        float barY = 4.0f;
        float barH = TAB_BAR_HEIGHT;
        float totalWidth = (TAB_COUNT - 1) * TAB_SPACING;
        float startX = barX + (barW - totalWidth) / 2.0f;
        float iconY = barY + barH / 2.0f;

        float selectedIconX = startX;
        float targetPillX = selectedIconX - TAB_PILL_WIDTH / 2.0f + TAB_PILL_OFFSETS_X[0];
        if (!tabPillInit) {
            tabPillX = targetPillX;
            tabPillInit = true;
        }
        tabPillX = AnimationMath.fast(tabPillX, targetPillX, 12);

        float pillYPos = barY + (barH - TAB_PILL_HEIGHT) / 2.0f;

        RenderUtility.drawRoundedRect(barX, barY, barW, barH, TAB_BAR_RADIUS,
                ColorUtils.rgba(10, 10, 10, (int) (170 * globalAlpha)));
        RenderUtility.drawRoundedRectOutline(barX, barY, barW, barH, TAB_BAR_RADIUS, 0.5f,
                ColorUtils.rgba(255, 255, 255, (int) (20 * globalAlpha)));

        RenderUtility.drawRoundedRect(tabPillX, pillYPos, TAB_PILL_WIDTH, TAB_PILL_HEIGHT, 4.0f,
                ColorUtils.setAlpha(themeColor, (int) (40 * globalAlpha)));

        for (int i = 0; i < TAB_COUNT; i++) {
            float iconX = startX + i * TAB_SPACING;
            boolean hovered = RenderUtility.isInRegion(mouseX, mouseY, iconX - TAB_PILL_WIDTH / 2f, pillYPos, TAB_PILL_WIDTH, TAB_PILL_HEIGHT);

            tabHoverAnims[i] = AnimationMath.fast(tabHoverAnims[i], hovered ? 1.0f : 0.0f, 10);

            int iconColor;
            if (i == 0) {
                iconColor = ColorUtils.setAlpha(themeColor, (int) (255 * globalAlpha));
            } else {
                iconColor = ColorUtils.rgba(255, 255, 255, (int) ((80 + 60 * tabHoverAnims[i]) * globalAlpha));
            }

            if (i == 0) {
                float glowSize = 6.0f;
                RenderUtility.drawShadow(iconX - glowSize, iconY - glowSize - 2, glowSize * 2, glowSize * 2, 8,
                        ColorUtils.setAlpha(themeColor, (int) (50 * globalAlpha)));
            }

            float iconW = ClientFonts.upico[TAB_ICON_FONT_SIZE].getWidth(TAB_ICONS[i]);
            float iconH = ClientFonts.upico[TAB_ICON_FONT_SIZE].getFontHeight();
            ClientFonts.upico[TAB_ICON_FONT_SIZE].drawString(stack, TAB_ICONS[i],
                    iconX - iconW / 2f, iconY - iconH / 2f + 5f, iconColor);

            if (tabHoverAnims[i] > 0.05f) {
                float labelAlpha = tabHoverAnims[i] * globalAlpha;
                Fonts.sfuy.drawCenteredText(stack, TAB_NAMES[i], iconX, barY + barH + 2,
                        ColorUtils.rgba(255, 255, 255, (int) (200 * labelAlpha)), 5.0f);
            }
        }
    }

    private boolean handleTabBarClick(double mouseX, double mouseY) {
        int windowWidth = mc.getMainWindow().getScaledWidth();
        float barW = Math.min(windowWidth * 0.5f, 114.0f);
        float barX = (windowWidth - barW) / 2.0f;
        float barY = 4.0f;
        float barH = TAB_BAR_HEIGHT;
        float totalWidth = (TAB_COUNT - 1) * TAB_SPACING;
        float startX = barX + (barW - totalWidth) / 2.0f;
        float pillYPos = barY + (barH - TAB_PILL_HEIGHT) / 2.0f;

        for (int i = 0; i < TAB_COUNT; i++) {
            float iconX = startX + i * TAB_SPACING;
            if (RenderUtility.isInRegion(mouseX, mouseY, iconX - TAB_PILL_WIDTH / 2f, pillYPos, TAB_PILL_WIDTH, TAB_PILL_HEIGHT)) {
                if (i == 5) {
                    mc.displayGuiScreen(new xd.harm.ui.clickgui.figura.FiguraCosmeticScreen(this));
                    return true;
                }
                if (i != 0) {
                    SoundUtil.playSound("switchcategory");
                    exit = true;
                    Harmony.getInstance().openMenuPanelOnTab(i);
                    return true;
                }
                return true;
            }
        }
        return false;
    }

    private void updateScaleBasedOnScreenWidth() {
        float totalPanelWidth = (float) panels.size() * 115;
        float screenWidth = (float) mc.getMainWindow().getScaledWidth();
        if (totalPanelWidth >= screenWidth) {
            scale = screenWidth / totalPanelWidth;
            scale = MathHelper.clamp(scale, 0.5F, 1.0F);
        } else {
            scale = 1.0F;
        }
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean ruleBind = false;

        for (Panel panel : panels) {
            panel.keyPressed(keyCode, scanCode, modifiers);
            if (panel.isBinding()) {
                ruleBind = true;
            }
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE && !exit && !ruleBind) {
            exit = true;
            open = false;
            return false;
        } else {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) return false;
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && handleTabBarClick(mouseX, mouseY)) {
            return true;
        }

        for (Panel panel : panels) {
            panel.mouseClick((float) mouseX, (float) mouseY, button);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        for (Panel panel : panels) {
            panel.mouseRelease((float) mouseX, (float) mouseY, button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return super.charTyped(codePoint, modifiers);
    }
}
