package xd.harm.ui.clickgui.components.settings;

import xd.harm.modules.settings.impl.StringSetting;
import xd.harm.ui.clickgui.components.builder.Component;
import xd.harm.utils.math.MathUtil;
import xd.harm.utils.render.Cursors;
import xd.harm.utils.render.color.ColorUtils;
import xd.harm.utils.render.font.Fonts;
import xd.harm.utils.render.rect.RenderUtility;
import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.SharedConstants;
import org.lwjgl.glfw.GLFW;
import ru.hogoshi.Animation;
import ru.hogoshi.util.Easings;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class StringComponent extends Component {

    static final int MAX_TEXT_LENGTH = 60;
    final StringSetting setting;
    boolean typing;
    String text = "";
    boolean hovered = false;
    Animation focusAnimation = new Animation();

    public StringComponent(StringSetting setting) {
        this.setting = setting;
        this.setHeight(28);
        focusAnimation = focusAnimation.animate(0, 0.3f, Easings.EXPO_OUT);
    }

    @Override
    public void render(MatrixStack stack, float mouseX, float mouseY) {
        super.render(stack, mouseX, mouseY);
        focusAnimation.update();

        String settingText = setting.get();
        if (setting.isOnlyNumber()) {
            text = filterDigits(settingText);
            if (!text.equals(settingText)) {
                setting.set(text);
            }
        } else {
            text = settingText;
        }

        float fieldX = getX() + 4;
        float fieldY = getY() + 14;
        float fieldWidth = getWidth() - 8;
        float fieldHeight = 12;

        Fonts.sfuy.drawText(stack, setting.getName(), getX() + 4, getY() + 3, ColorUtils.rgba(200, 200, 200, 255), 6f);

        float focusValue = (float) focusAnimation.getValue();
        int borderColor = ColorUtils.interpolateColor(
                ColorUtils.rgba(50, 52, 58, 255),
                ColorUtils.rgba(80, 85, 95, 255),
                focusValue
        );

        RenderUtility.drawRoundedRect(fieldX - 0.5f, fieldY - 0.5f, fieldWidth + 1, fieldHeight + 1, 4.5f, borderColor);
        RenderUtility.drawRoundedRect(fieldX, fieldY, fieldWidth, fieldHeight, 4, ColorUtils.rgba(20, 22, 26, 255));

        String displayText = text;
        if (text.isEmpty() && !typing) {
            displayText = setting.getDescription();
        }

        int textColor = text.isEmpty() && !typing ?
                ColorUtils.rgba(100, 100, 100, 255) :
                ColorUtils.rgba(220, 220, 220, 255);

        float textX = fieldX + 4;
        float textY = fieldY + 3;
        Fonts.sfuy.drawText(stack, displayText, textX, textY, textColor, 6f);
        if (typing && System.currentTimeMillis() % 1000 > 500) {
            Fonts.sfuy.drawText(stack, "_", textX + Fonts.sfuy.getWidth(displayText, 6f), textY, textColor, 6f);
        }

        boolean fieldHovered = MathUtil.isHovered(mouseX, mouseY, fieldX, fieldY, fieldWidth, fieldHeight);
        if (fieldHovered) {
            if (!hovered) {
                GLFW.glfwSetCursor(Minecraft.getInstance().getMainWindow().getHandle(), Cursors.IBEAM);
                hovered = true;
            }
        } else if (hovered) {
            GLFW.glfwSetCursor(Minecraft.getInstance().getMainWindow().getHandle(), Cursors.ARROW);
            hovered = false;
        }
    }

    @Override
    public void charTyped(char codePoint, int modifiers) {
        if (!typing) {
            super.charTyped(codePoint, modifiers);
            return;
        }

        appendText(String.valueOf(codePoint));
        super.charTyped(codePoint, modifiers);
    }

    @Override
    public void keyPressed(int key, int scanCode, int modifiers) {
        if (typing) {
            if (Screen.isCopy(key)) {
                Minecraft.getInstance().keyboardListener.setClipboardString(text);
            }

            if (Screen.isCut(key)) {
                Minecraft.getInstance().keyboardListener.setClipboardString(text);
                setText("");
            }

            if (Screen.isPaste(key)) {
                try {
                    appendText(Minecraft.getInstance().keyboardListener.getClipboardString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (key == GLFW.GLFW_KEY_BACKSPACE && !text.isEmpty()) {
                setText(text.substring(0, text.length() - 1));
            }

            if (key == GLFW.GLFW_KEY_ENTER) {
                typing = false;
                focusAnimation.animate(0, 0.2f, Easings.EXPO_OUT);
            }
        }
        super.keyPressed(key, scanCode, modifiers);
    }

    private void appendText(String value) {
        String sanitized = sanitize(value);
        if (sanitized.isEmpty()) {
            return;
        }

        int available = MAX_TEXT_LENGTH - text.length();
        if (available <= 0) {
            return;
        }

        if (sanitized.length() > available) {
            sanitized = sanitized.substring(0, available);
        }

        setText(text + sanitized);
    }

    private void setText(String value) {
        text = sanitize(value);
        setting.set(text);
    }

    private String sanitize(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        String sanitized = SharedConstants.filterAllowedCharacters(value);
        if (setting.isOnlyNumber()) {
            sanitized = filterDigits(sanitized);
        }

        if (sanitized.length() > MAX_TEXT_LENGTH) {
            sanitized = sanitized.substring(0, MAX_TEXT_LENGTH);
        }

        return sanitized;
    }

    private String filterDigits(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        int length = value.length();
        int firstInvalid = -1;
        for (int i = 0; i < length; i++) {
            char ch = value.charAt(i);
            if (ch < '0' || ch > '9') {
                firstInvalid = i;
                break;
            }
        }

        if (firstInvalid == -1) {
            return value;
        }

        StringBuilder builder = new StringBuilder(length);
        builder.append(value, 0, firstInvalid);
        for (int i = firstInvalid + 1; i < length; i++) {
            char ch = value.charAt(i);
            if (ch >= '0' && ch <= '9') {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    @Override
    public void mouseClick(float mouseX, float mouseY, int mouse) {
        float fieldX = getX() + 4;
        float fieldY = getY() + 14;
        float fieldWidth = getWidth() - 8;
        float fieldHeight = 12;

        if (MathUtil.isHovered(mouseX, mouseY, fieldX, fieldY, fieldWidth, fieldHeight)) {
            typing = !typing;
            focusAnimation.animate(typing ? 1 : 0, 0.3f, Easings.EXPO_OUT);
        } else if (typing) {
            typing = false;
            focusAnimation.animate(0, 0.2f, Easings.EXPO_OUT);
        }
        super.mouseClick(mouseX, mouseY, mouse);
    }

    @Override
    public boolean isVisible() {
        return setting.visible.get();
    }
}
