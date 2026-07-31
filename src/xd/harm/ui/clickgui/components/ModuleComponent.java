package xd.harm.ui.clickgui.components;

import xd.harm.modules.api.Module;
import xd.harm.modules.impl.render.Theme;
import xd.harm.modules.settings.Setting;
import xd.harm.modules.settings.impl.*;
import xd.harm.ui.clickgui.Panel;
import xd.harm.ui.clickgui.components.builder.Component;
import xd.harm.ui.clickgui.components.settings.*;
import xd.harm.modules.settings.impl.BindSetting;
import xd.harm.utils.SoundUtil;
import xd.harm.utils.client.KeyStorage;
import xd.harm.utils.math.MathUtil;
import xd.harm.utils.render.Cursors;
import xd.harm.utils.render.color.ColorUtils;
import xd.harm.utils.render.font.Fonts;
import xd.harm.utils.render.gl.Scissor;
import xd.harm.utils.render.rect.RenderUtility;
import xd.harm.utils.text.BetterText;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.vector.Vector4f;
import org.lwjgl.glfw.GLFW;
import ru.hogoshi.Animation;
import ru.hogoshi.util.Easings;
import xd.harm.utils.text.font.ClientFonts;

import java.util.List;

@Getter
public class ModuleComponent extends Component {
    private static final Vector4f DETAILS_ROUNDING = new Vector4f(4, 4, 4, 4);
    private final Module module;
    private final String moduleName;
    private final float moduleNameWidth;

    @Setter
    protected Panel panel;
    public Animation expandAnim = new Animation();
    private Animation hoverAnim = new Animation();
    private Animation glowAnim = new Animation();
    private Animation heightAnim = new Animation();
    private Animation fadeAnim = new Animation();
    private Animation rotateAnim = new Animation();
    private Animation bindAnim = new Animation();
    private Animation arrowFadeAnim = new Animation();
    private final FloatArrayList componentAnimValues = new FloatArrayList();
    private long openTime = 0;
    private boolean open;
    private boolean bind;
    private BindSetting bindSetting;
    private double openAnimValue = 0.8, noOpenAnimValue = 0.6;
    private final ObjectArrayList<Component> components = new ObjectArrayList<>();
    private final BetterText bindingDots = new BetterText(List.of("..."), 50);
    private boolean hovered = false;
    private static final float ANIMATION_DURATION = 600f;
    private static final float STAGGER_DELAY = 40f;

    public ModuleComponent(Module module) {
        this.module = module;
        this.moduleName = module.getName();
        this.moduleNameWidth = Fonts.sfuy.getWidth(moduleName, 8);

        for (Setting<?> setting : module.getSettings()) {
            if (setting instanceof BooleanSetting bool) {
                components.add(new BooleanComponent(bool));
            }
            if (setting instanceof SliderSetting slider) {
                components.add(new SliderComponent(slider));
            }
            if (setting instanceof BindSetting bindSetting) {
                this.bindSetting = bindSetting;
            }
            if (setting instanceof ModeSetting mode) {
                components.add(new ModeComponent(mode));
            }
            if (setting instanceof ModeListSetting mode) {
                components.add(new MultiBoxComponent(mode));
            }
            if (setting instanceof StringSetting string) {
                components.add(new StringComponent(string));
            }
            if (setting instanceof ColorSetting color) {
                components.add(new ColorComponent(color));
            }
            if (setting instanceof CategorySetting category) {
                components.add(new CategoryComponent(category));
            }

            componentAnimValues.add(0f);
        }

        expandAnim = expandAnim.animate(open ? 1 : 0, open ? openAnimValue : noOpenAnimValue, Easings.EXPO_OUT);
        hoverAnim = hoverAnim.animate(0, 0.2f, Easings.EXPO_OUT);
        glowAnim = glowAnim.animate(module.isState() ? 1 : 0, 0.3f, Easings.EXPO_OUT);
        heightAnim = heightAnim.animate(0, 0.7f, Easings.EXPO_OUT);
        fadeAnim = fadeAnim.animate(0, 0.5f, Easings.SINE_OUT);
        rotateAnim = rotateAnim.animate(0, 0.4f, Easings.BACK_OUT);
        bindAnim = bindAnim.animate(0, 0.3f, Easings.EXPO_OUT);
        arrowFadeAnim = arrowFadeAnim.animate(1, 0.3f, Easings.EXPO_OUT);
    }

    public void setOpen(boolean open) {
        this.open = open;
        this.expandAnim = expandAnim.animate(open ? 1 : 0, open ? openAnimValue : noOpenAnimValue, Easings.EXPO_OUT);
        this.heightAnim = heightAnim.animate(open ? 1 : 0, 0.7f, Easings.EXPO_OUT);
        this.fadeAnim = fadeAnim.animate(open ? 1 : 0, 0.5f, Easings.SINE_OUT);
        this.rotateAnim = rotateAnim.animate(open ? 1 : 0, 0.4f, Easings.BACK_OUT);

        // Прячем BindComponent из тела настроек когда модуль открыт
        for (Component comp : components) {
            if (comp instanceof BindComponent bc) {
                bc.setHidden(open);
            }
        }

        this.openTime = System.currentTimeMillis();
    }

    public void setBind(boolean bind) {
        if (this.bind != bind) {
            this.bind = bind;
            if (bind) {
                SoundUtil.playSound("binding.wav");
                bindingDots.setTextIndex(0);
                bindingDots.setCharIndex(0);
                bindingDots.getOutput().setLength(0);
            }
            bindAnim = bindAnim.animate(bind ? 1 : 0, 0.3f, Easings.EXPO_OUT);
            arrowFadeAnim = arrowFadeAnim.animate(bind ? 0 : 1, 0.3f, Easings.EXPO_OUT);
        }
    }

    /**
     * Проверяет, скрыт ли компонент (по realIndex в списке components)
     * из-за того, что лежит под свёрнутой категорией.
     * Сам CategoryComponent (заголовок) никогда не скрывается.
     */
    public boolean isHiddenByCollapsedCategory(int realIndex) {
        Component current = components.get(realIndex);
        if (current instanceof CategoryComponent) return false;
        for (int i = realIndex - 1; i >= 0; i--) {
            Component c = components.get(i);
            if (c instanceof CategoryComponent cat) {
                return cat.isCollapsed();
            }
        }
        return false;
    }

    public void onClose() {
        open = false;
        bind = false;
        expandAnim.animate(0, noOpenAnimValue, Easings.EXPO_OUT);
        hoverAnim.animate(0, 0.2f, Easings.EXPO_OUT);
        glowAnim.animate(0, 0.3f, Easings.EXPO_OUT);
        heightAnim.animate(0, 0.5f, Easings.EXPO_OUT);
        fadeAnim.animate(0, 0.3f, Easings.SINE_OUT);
        rotateAnim.animate(0, 0.3f, Easings.BACK_IN);
        bindAnim.animate(0, 0.2f, Easings.EXPO_OUT);
        arrowFadeAnim.animate(1, 0.2f, Easings.EXPO_OUT);

        for (int i = 0; i < componentAnimValues.size(); i++) {
            componentAnimValues.set(i, 0f);
        }

        for (Component component : components) {
            component.setFocused(false);
        }

        this.openTime = System.currentTimeMillis();
    }

    @Override
    public void mouseRelease(float mouseX, float mouseY, int mouse) {
        for (int i = 0; i < components.size(); i++) {
            Component component = components.get(i);
            if (!isHiddenByCollapsedCategory(i)) {
                component.mouseRelease(mouseX, mouseY, mouse);
            }
        }
        super.mouseRelease(mouseX, mouseY, mouse);
    }

    private float easeBackOut(float t) {
        if (t <= 0) return 0;
        if (t >= 1) return 1;
        float c1 = 1.70158f;
        float c3 = c1 + 1;
        return 1 + c3 * (float)Math.pow(t - 1, 3) + c1 * (float)Math.pow(t - 1, 2);
    }

    private float easeBackIn(float t) {
        if (t <= 0) return 0;
        if (t >= 1) return 1;
        float c1 = 1.70158f;
        float c3 = c1 + 1;
        return c3 * t * t * t - c1 * t * t;
    }

    @Override
    public void render(MatrixStack stack, float mouseX, float mouseY) {
        super.render(stack, mouseX, mouseY);

        module.getAnimation().update();
        if (bind) {
            bindingDots.update();
        }
        expandAnim.update();
        hoverAnim.update();
        glowAnim.update();
        heightAnim.update();
        fadeAnim.update();
        rotateAnim.update();
        bindAnim.update();
        arrowFadeAnim.update();

        long currentTime = System.currentTimeMillis();
        long timeSinceOpen = currentTime - openTime;

        for (int i = 0; i < componentAnimValues.size(); i++) {
            float delay = i * STAGGER_DELAY;
            long adjustedTime = timeSinceOpen - (long)delay;

            if (adjustedTime < 0) {
                componentAnimValues.set(i, open ? 0f : 1f);
                continue;
            }

            float progress = Math.min(1.0f, adjustedTime / ANIMATION_DURATION);

            if (open) {
                float eased = easeBackOut(progress);
                componentAnimValues.set(i, eased);
            } else {
                float eased = easeBackIn(progress);
                componentAnimValues.set(i, 1f - eased);
            }
        }

        boolean hover = RenderUtility.isInRegion(mouseX, mouseY, getX(), getY(), getWidth(), 18);
        hoverAnim = hoverAnim.animate(hover ? 1 : 0, 0.2f, Easings.EXPO_OUT);
        glowAnim = glowAnim.animate(module.isState() ? 1 : 0, 0.3f, Easings.EXPO_OUT);

        float hoverValue = (float) hoverAnim.getValue();
        float glowValue = (float) glowAnim.getValue();
        float bindValue = (float) bindAnim.getValue();
        float arrowFadeValue = (float) arrowFadeAnim.getValue();

        if (hover && !bind) {
            GLFW.glfwSetCursor(Minecraft.getInstance().getMainWindow().getHandle(), Cursors.HAND);
            if (!this.hovered) {
                this.hovered = true;
                SoundUtil.playSound("Hovered.wav");
            }
        } else if (this.hovered && !bind) {
            GLFW.glfwSetCursor(Minecraft.getInstance().getMainWindow().getHandle(), Cursors.ARROW);
            this.hovered = false;
        }

        int themeColor = Theme.MainColor(0);

        String displayText;
        if (bind) {
            displayText = "Binding" + bindingDots.getOutput().toString();
        } else {
            displayText = moduleName;
        }

        float textWidth = bind ? Fonts.sfuy.getWidth(displayText, 8) : moduleNameWidth;
        float textHeight = 8;

        float textX = getX() + (getWidth() - 2) / 2f - textWidth / 2f;
        float textY = getY() + 18 / 2f - textHeight / 2f;

        int textColor;
        if (bind) {
            float pulse = (float)(Math.sin(currentTime * 0.003) * 0.3 + 0.7);
            int brightness = (int)(140 + 115 * pulse);
            textColor = ColorUtils.rgb(brightness, brightness, brightness);
        } else if (module.isState()) {
            textColor = ColorUtils.rgb(255, 255, 255);
        } else {
            textColor = ColorUtils.rgb(140, 140, 140);
        }

        if (glowValue > 0.01f && !bind) {
            int glowColor = ColorUtils.setAlpha(themeColor, (int)(50 * glowValue));

            Fonts.sfuy.drawText(stack, displayText, textX - 0.5f, textY, glowColor, 8);
            Fonts.sfuy.drawText(stack, displayText, textX + 0.5f, textY, glowColor, 8);
            Fonts.sfuy.drawText(stack, displayText, textX, textY - 0.5f, glowColor, 8);
            Fonts.sfuy.drawText(stack, displayText, textX, textY + 0.5f, glowColor, 8);
        }

        Fonts.sfuy.drawText(
                stack,
                displayText,
                textX,
                textY,
                textColor,
                8
        );

        // Рисуем модуль по образцу Quantum: "Key: ..." в хедере при bind-режиме
        if (bind) {
            String keyName = module.getBind() == 0 ? "..." : KeyStorage.getKey(module.getBind());
            if (keyName == null) keyName = GLFW.glfwGetKeyName(module.getBind(), 0);
            if (keyName == null) keyName = "...";
            String bindText = "Key: " + keyName.toUpperCase();
            float bindTextWidth = Fonts.sfuy.getWidth(bindText, 6f);
            float bindTextX = getX() + getWidth() - bindTextWidth - 6;
            float bindTextY = getY() + (18 - Fonts.sfuy.getHeight(6f)) / 2f;
            Fonts.sfuy.drawText(stack, bindText, Math.round(bindTextX), Math.round(bindTextY), textColor, 6f);
        }

        if (!components.isEmpty() && arrowFadeValue > 0.01f) {
            float arrowX = getX() + getWidth() - 12;
            float arrowY = getY() + 18 / 2f;

            int arrowColor = module.isState() ?
                    ColorUtils.setAlpha(ColorUtils.rgb(180, 180, 180), (int)(255 * arrowFadeValue)) :
                    ColorUtils.setAlpha(ColorUtils.rgb(100, 100, 100), (int)(255 * arrowFadeValue));

            if (glowValue > 0.01f && arrowFadeValue > 0.01f) {
                int arrowGlow = ColorUtils.setAlpha(themeColor, (int)(40 * glowValue * arrowFadeValue));

                stack.push();
                stack.translate(arrowX, arrowY, 0);

                float rotation = (float) (rotateAnim.getValue() * 270);
                stack.rotate(new net.minecraft.util.math.vector.Quaternion(0, 0, rotation, true));


                float scaleValue = arrowFadeValue;
                stack.scale(scaleValue, scaleValue, 1.0f);

                ClientFonts.icons_client[16].drawString(stack, "p", -1, -1, arrowGlow);

                stack.pop();
            }

            stack.push();
            stack.translate(arrowX, arrowY, 0);

            float rotation = (float) (rotateAnim.getValue() * 270);
            stack.rotate(new net.minecraft.util.math.vector.Quaternion(0, 0, rotation, true));

            float pulseScale = (1.0f + hoverValue * 0.1f) * arrowFadeValue;
            stack.scale(pulseScale, pulseScale, 1.0f);

            ClientFonts.icons_client[16].drawString(stack, "p", -1, -1, arrowColor);

            stack.pop();
        }


        if (heightAnim.getValue() > 0.01f && open && panel != null) {
            float heightValue = (float) heightAnim.getValue();
            float fadeValue = (float) fadeAnim.getValue();

            Scissor.push();
            Scissor.setFromComponentCoordinates(
                    panel.getX() + 3,
                    panel.getY() + 18,
                    panel.getWidth() - 6,
                    panel.getHeight() - 26
            );

            float totalHeight = 0;
            for (int i = 0; i < components.size(); i++) {
                Component component = components.get(i);
                if (component.isVisible() && !isHiddenByCollapsedCategory(i)) {
                    totalHeight += component.getHeight();
                }
            }

            float animatedHeight = totalHeight * heightValue;

            stack.push();

            float slideY = (1 - heightValue) * 10;
            stack.translate(0, slideY, 0);

            RenderUtility.drawRoundedRect(
                    getX(),
                    getY() + 18,
                    getWidth(),
                    animatedHeight + 4,
                    DETAILS_ROUNDING,
                    ColorUtils.rgba(20, 20, 20, (int)(60 * fadeValue))
            );

            stack.pop();

            float y = getY() + 20;

            for (int i = 0; i < components.size(); i++) {
                Component component = components.get(i);
                boolean hiddenByCat = isHiddenByCollapsedCategory(i);
                if (component.isVisible() && !hiddenByCat && i < componentAnimValues.size()) {
                    float componentAnimValue = componentAnimValues.getFloat(i);

                    if (componentAnimValue > 0.001f) {
                        stack.push();

                        float slideX = (1 - componentAnimValue) * 20;

                        stack.translate(slideX, 0, 0);

                        float scaleX = 0.95f + componentAnimValue * 0.05f;
                        stack.scale(scaleX, 1.0f, 1.0f);

                        boolean fading = componentAnimValue < 0.999f;
                        if (fading) {
                            RenderSystem.enableBlend();
                            RenderSystem.defaultBlendFunc();
                            RenderSystem.color4f(1.0f, 1.0f, 1.0f, componentAnimValue);
                        }

                        component.setX(getX());
                        component.setY(y);
                        component.setWidth(getWidth());
                        component.render(stack, mouseX, mouseY);

                        if (fading) {
                            RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
                            RenderSystem.disableBlend();
                        }

                        stack.pop();

                        y += component.getHeight() * componentAnimValue;
                    }
                }
            }

            Scissor.pop();
        }
    }

    @Override
    public void mouseClick(float mouseX, float mouseY, int button) {
        if (MathUtil.isHovered(mouseX, mouseY, getX(), getY(), getWidth(), 18)) {
            if (button == 0 && !bind) {
                module.toggle();
                SoundUtil.playSound("click.wav");
            } else if (button == 1 && !bind && !components.isEmpty()) {
                setOpen(!open);
                SoundUtil.playSound(open ? "openmodescreen.wav" : "closemodescreen.wav");
            } else if (button == 2) {
                setBind(!bind);
            }
        }

        if (open && isHovered(mouseX, mouseY)) {
            for (int i = 0; i < components.size(); i++) {
                Component component = components.get(i);
                if (component.isVisible() && !isHiddenByCollapsedCategory(i)) {
                    component.mouseClick(mouseX, mouseY, button);
                }
            }
        }
        super.mouseClick(mouseX, mouseY, button);
    }

    @Override
    public void charTyped(char codePoint, int modifiers) {
        for (int i = 0; i < components.size(); i++) {
            Component component = components.get(i);
            if (component.isVisible() && !isHiddenByCollapsedCategory(i)) {
                component.charTyped(codePoint, modifiers);
            }
        }
        super.charTyped(codePoint, modifiers);
    }

    @Override
    public void keyPressed(int key, int scanCode, int modifiers) {
        for (int i = 0; i < components.size(); i++) {
            Component component = components.get(i);
            if (component.isVisible() && !isHiddenByCollapsedCategory(i)) {
                component.keyPressed(key, scanCode, modifiers);
            }
        }
        if (bind) {
            if (key == GLFW.GLFW_KEY_DELETE || key == GLFW.GLFW_KEY_ESCAPE) {
                module.setBind(0);
                SoundUtil.playSound("unbind.wav");
            } else {
                module.setBind(key);
                SoundUtil.playSound("bind.wav");
            }
            setBind(false);
        }
        super.keyPressed(key, scanCode, modifiers);
    }
}
