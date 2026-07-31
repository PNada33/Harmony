package xd.harm.ui.clickgui.components.settings;

import xd.harm.modules.impl.render.Theme;
import xd.harm.modules.settings.impl.ModeSetting;
import xd.harm.ui.clickgui.components.builder.Component;
import xd.harm.utils.SoundUtil;
import xd.harm.utils.math.MathUtil;
import xd.harm.utils.render.Cursors;
import xd.harm.utils.render.color.ColorUtils;
import xd.harm.utils.render.font.Fonts;
import xd.harm.utils.render.gl.Scissor;
import xd.harm.utils.render.rect.RenderUtility;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import ru.hogoshi.Animation;
import ru.hogoshi.util.Easings;

import java.util.ArrayList;
import java.util.List;

public class ModeComponent extends Component {

    final ModeSetting setting;
    private final List<ModeOption> options = new ArrayList<>();
    private final Animation revealAnimation = new Animation();
    private int hoveredIndex = -1;
    private boolean hovered = false;
    private long lastRenderTime = 0L;
    private float scrollOffset = 0;
    private float maxScroll = 0;

    private static final int HEADER_HEIGHT = 14;
    private static final int OPTION_HEIGHT = 12;
    private static final int VISIBLE_OPTIONS = 8;
    private static final long REVEAL_RESET_DELAY_MS = 140L;
    private static final float SCROLL_SPEED = 12f;
    private final int optionGap;

    public ModeComponent(ModeSetting setting) {
        this.setting = setting;
        this.optionGap = setting.optionGap;
        revealAnimation.setValue(0);
        revealAnimation.setToValue(0);

        for (String mode : setting.strings) {
            ModeOption option = new ModeOption(mode);
            if (mode.equals(setting.get())) {
                option.selectAnimation.animate(1, 0.4f, Easings.EXPO_OUT);
            }
            options.add(option);
        }

        calculateHeight();
        updateMaxScroll();
    }

    private void calculateHeight() {
        int visibleCount = Math.min(options.size(), VISIBLE_OPTIONS);
        setHeight(HEADER_HEIGHT + visibleCount * (OPTION_HEIGHT + optionGap) + 6);
    }

    private void updateMaxScroll() {
        float totalHeight = options.size() * (OPTION_HEIGHT + optionGap);
        float visibleHeight = VISIBLE_OPTIONS * (OPTION_HEIGHT + optionGap);
        maxScroll = Math.max(0, totalHeight - visibleHeight);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
    }

    private int getVisibleStartIndex() {
        if (maxScroll <= 0 || options.size() <= VISIBLE_OPTIONS) return 0;
        float progress = scrollOffset / maxScroll;
        int maxStart = options.size() - VISIBLE_OPTIONS;
        return Math.min(maxStart, Math.round(progress * maxStart));
    }

    @Override
    public void render(MatrixStack stack, float mouseX, float mouseY) {
        super.render(stack, mouseX, mouseY);

        handleScrollWheel(mouseY);

        long now = System.currentTimeMillis();
        if (now - lastRenderTime > REVEAL_RESET_DELAY_MS) {
            revealAnimation.setValue(0);
            revealAnimation.setToValue(0);
        }
        lastRenderTime = now;

        revealAnimation.update();
        revealAnimation.animate(1, 0.35f, Easings.EXPO_OUT);
        float revealValue = (float) revealAnimation.getValue();

        float containerX = getX() + 0f;
        float containerY = getY() - 1f;
        float containerW = getWidth() + 0f;
        float containerH = getHeight() * revealValue;
        RenderUtility.drawRoundedRect(
                containerX,
                containerY,
                containerW,
                containerH,
                3f,
                ColorUtils.rgba(8, 8, 8, (int) (120 * revealValue))
        );

        Scissor.push();
        Scissor.setFromComponentCoordinates(
                containerX, containerY,
                containerW, containerH
        );

        Fonts.sfuy.drawText(
                stack,
                setting.getName(),
                getX() + 4,
                getY() + 4,
                ColorUtils.rgba(180, 180, 180, (int) (255 * revealValue)),
                6f
        );

        float startY = getY() + HEADER_HEIGHT;
        int newHoveredIndex = -1;
        int visibleStart = getVisibleStartIndex();
        int visibleEnd = Math.min(visibleStart + VISIBLE_OPTIONS + 1, options.size());

        for (int i = visibleStart; i < visibleEnd; i++) {
            ModeOption option = options.get(i);
            float optionDelay = options.size() > 1
                    ? (i * 0.5f / (options.size() - 1))
                    : 0f;
            float optionReveal = revealValue <= optionDelay
                    ? 0f
                    : Math.min(1f, (revealValue - optionDelay) / Math.max(0.001f, 1f - optionDelay));
            float optionBaseX = getX() + 6f;
            float optionY = startY + (i - visibleStart) * (OPTION_HEIGHT + optionGap);
            float optionW = getWidth() - 12f;
            float optionH = OPTION_HEIGHT * optionReveal;
            float optionX = optionBaseX + (optionW - optionW * optionReveal) / 2f;
            float animatedOptionW = optionW * optionReveal;

            if (optionReveal < 0.01f) {
                continue;
            }

            boolean isSelected = setting.get().equals(option.name);
            boolean isHovered = optionReveal > 0.35f && MathUtil.isHovered(mouseX, mouseY, optionX, optionY, animatedOptionW, optionH);

            if (isHovered) {
                newHoveredIndex = i;
            }

            option.selectAnimation.update();
            option.hoverAnimation.update();

            option.selectAnimation.animate(isSelected ? 1 : 0, 0.4f, Easings.EXPO_OUT);
            option.hoverAnimation.animate(isHovered ? 1 : 0, 0.2f, Easings.EXPO_OUT);

            float selectValue = (float) option.selectAnimation.getValue();
            float hoverValue = (float) option.hoverAnimation.getValue();

            int bgColor = ColorUtils.interpolateColor(
                    ColorUtils.rgba(16, 16, 16, 175),
                    ColorUtils.setAlpha(Theme.MainColor(0), 95),
                    selectValue
            );
            if (hoverValue > 0.01f) {
                bgColor = ColorUtils.interpolateColor(bgColor, ColorUtils.rgba(38, 38, 38, 185), hoverValue * 0.35f);
            }
            bgColor = ColorUtils.setAlpha(bgColor, (int) (ColorUtils.getAlpha(bgColor) * optionReveal));

            RenderUtility.drawRoundedRect(optionX, optionY, animatedOptionW, optionH, 3f, bgColor);

            int outlineColor = ColorUtils.interpolateColor(
                    ColorUtils.rgba(75, 75, 75, 130),
                    Theme.MainColor(0),
                    Math.max(selectValue, hoverValue * 0.75f)
            );
            outlineColor = ColorUtils.setAlpha(outlineColor, (int) (ColorUtils.getAlpha(outlineColor) * optionReveal));
            RenderUtility.drawRoundedRectOutline(optionX, optionY, animatedOptionW, optionH, 3f, 0.6f, outlineColor);

            int textColor = ColorUtils.interpolateColor(
                    ColorUtils.rgba(140, 140, 140, 255),
                    ColorUtils.rgba(255, 255, 255, 255),
                    Math.min(1f, selectValue * 0.8f + hoverValue * 0.35f)
            );
            textColor = ColorUtils.setAlpha(textColor, (int) (ColorUtils.getAlpha(textColor) * optionReveal));

            Fonts.sfuy.drawCenteredText(
                    stack,
                    option.name,
                    optionX + animatedOptionW / 2f,
                    optionY + optionH / 2f - 3.2f,
                    textColor,
                    6.3f,
                    0.04f
            );
        }

        Scissor.pop();

        hoveredIndex = newHoveredIndex;

        if (hoveredIndex != -1) {
            GLFW.glfwSetCursor(Minecraft.getInstance().getMainWindow().getHandle(), Cursors.HAND);
            if (!this.hovered) {
                this.hovered = true;
                SoundUtil.playSound("Hovered.wav");
            }
        } else if (this.hovered) {
            GLFW.glfwSetCursor(Minecraft.getInstance().getMainWindow().getHandle(), Cursors.ARROW);
            this.hovered = false;
        }
    }

    @Override
    public void mouseClick(float mouseX, float mouseY, int mouse) {
        if (mouse == 0) {
            float startY = getY() + HEADER_HEIGHT;
            int visibleStart = getVisibleStartIndex();
            int visibleEnd = Math.min(visibleStart + VISIBLE_OPTIONS + 1, options.size());

            for (int i = visibleStart; i < visibleEnd; i++) {
                ModeOption option = options.get(i);
                float optionX = getX() + 6f;
                float optionY = startY + (i - visibleStart) * (OPTION_HEIGHT + optionGap);
                float optionW = getWidth() - 12f;

                if (MathUtil.isHovered(mouseX, mouseY, optionX, optionY, optionW, OPTION_HEIGHT)) {
                    if (!setting.get().equals(option.name)) {
                        setting.set(option.name);
                        SoundUtil.playSound("click.wav");

                        for (ModeOption opt : options) {
                            if (opt.name.equals(option.name)) {
                                opt.selectAnimation.animate(1, 0.4f, Easings.EXPO_OUT);
                            } else {
                                opt.selectAnimation.animate(0, 0.3f, Easings.EXPO_OUT);
                            }
                        }
                    }
                    break;
                }
            }
        }
        super.mouseClick(mouseX, mouseY, mouse);
    }

    @Override
    public boolean isVisible() {
        return setting.visible.get();
    }

    private void handleScrollWheel(float mouseY) {
        if (options.size() <= VISIBLE_OPTIONS) return;
        double wheel = Minecraft.getInstance().mouseHelper.getWheel();
        if (wheel == 0) return;

        float startY = getY() + HEADER_HEIGHT;
        float visibleH = VISIBLE_OPTIONS * (OPTION_HEIGHT + optionGap);
        if (mouseY >= startY && mouseY <= startY + visibleH) {
            scrollOffset -= (float) (wheel * SCROLL_SPEED);
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        }
        Minecraft.getInstance().mouseHelper.setWheel(0);
    }

    private static class ModeOption {
        public Animation selectAnimation = new Animation();
        public Animation hoverAnimation = new Animation();
        public String name;

        public ModeOption(String name) {
            this.name = name;
            this.selectAnimation = selectAnimation.animate(0, 0.4f, Easings.EXPO_OUT);
            this.hoverAnimation = hoverAnimation.animate(0, 0.2f, Easings.EXPO_OUT);
        }
    }
}
