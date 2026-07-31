package xd.harm.ui.clickgui.components;

import xd.harm.Harmony;
import xd.harm.modules.api.Module;
import xd.harm.modules.impl.render.FiguraCosmetic;
import xd.harm.modules.impl.render.Theme;
import xd.harm.utils.SoundUtil;
import xd.harm.utils.client.ClientUtility;
import xd.harm.utils.math.MathUtil;
import xd.harm.utils.render.GaussianBlur;
import xd.harm.utils.render.color.ColorUtils;
import xd.harm.utils.render.font.Fonts;
import xd.harm.utils.render.rect.RenderUtility;
import xd.harm.utils.text.font.ClientFonts;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import ru.hogoshi.Animation;
import ru.hogoshi.util.Easings;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class SearchField {
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public String getText() { return text; }
    public boolean isFocused() { return isFocused; }
    public boolean isTyping() { return typing; }
    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }
    public void setWidth(int width) { this.width = width; }
    public void setHeight(int height) { this.height = height; }
    public void setFocused(boolean focused) { this.isFocused = focused; }
    public void setTyping(boolean typing) { this.typing = typing; }
    private int x, y, width, height;
    private String text = "";
    private boolean isFocused;
    private boolean typing;
    private final String placeholder;
    private final Animation focusAnimation = new Animation();
    private final Animation suggestionsAnimation = new Animation();

    private static final int MAX_TEXT_LENGTH = 17;
    private static final int MAX_SUGGESTIONS = 5;

    private boolean textSelected = false;
    private int cursorPosition = 0;
    private int selectionPosition = 0;
    private boolean selectionDragActive = false;
    private int selectionDragAnchor = 0;

    private final List<String> suggestions = new ArrayList<>(MAX_SUGGESTIONS);
    private int selectedSuggestionIndex = -1;
    private final List<Animation> suggestionHoverAnims = new ArrayList<>(MAX_SUGGESTIONS);
    private final List<Animation> suggestionSelectAnims = new ArrayList<>(MAX_SUGGESTIONS);
    private String lastSuggestionText;
    private boolean lastSuggestionFocused;

    private static class AnimatedChar {
        char character;
        String characterText;
        Animation scaleAnim;
        Animation alphaAnim;
        Animation offsetYAnim;
        Animation widthAnim;
        boolean removing;
        float fullWidth;

        AnimatedChar(char c, boolean instant) {
            this.character = c;
            this.characterText = String.valueOf(c);
            this.scaleAnim = new Animation();
            this.alphaAnim = new Animation();
            this.offsetYAnim = new Animation();
            this.widthAnim = new Animation();
            this.removing = false;
            this.fullWidth = Fonts.sfuy.getWidth(characterText, 8);

            if (instant) {
                this.scaleAnim.setValue(1);
                this.scaleAnim.setToValue(1);
                this.alphaAnim.setValue(1);
                this.alphaAnim.setToValue(1);
                this.offsetYAnim.setValue(0);
                this.offsetYAnim.setToValue(0);
                this.widthAnim.setValue(fullWidth);
                this.widthAnim.setToValue(fullWidth);
            } else {
                this.scaleAnim.setValue(0.3);
                this.alphaAnim.setValue(0);
                this.offsetYAnim.setValue(4);
                this.widthAnim.setValue(fullWidth);
                this.widthAnim.setToValue(fullWidth);

                this.scaleAnim.animate(1, 0.25, Easings.EXPO_OUT);
                this.alphaAnim.animate(1, 0.2, Easings.EXPO_OUT);
                this.offsetYAnim.animate(0, 0.25, Easings.EXPO_OUT);
            }
        }

        void startRemove() {
            if (removing) return;
            removing = true;
            scaleAnim.animate(0.3, 0.15, Easings.EXPO_IN);
            alphaAnim.animate(0, 0.12, Easings.EXPO_IN);
            offsetYAnim.animate(-4, 0.15, Easings.EXPO_IN);
            widthAnim.animate(0, 0.15, Easings.EXPO_IN);
        }

        void update() {
            scaleAnim.update();
            alphaAnim.update();
            offsetYAnim.update();
            widthAnim.update();
        }

        boolean isFinishedRemoving() {
            return removing && alphaAnim.isDone() && widthAnim.isDone();
        }
    }

    private final List<AnimatedChar> animatedChars = new ArrayList<>();

    public SearchField(int x, int y, int width, int height, String placeholder) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.placeholder = placeholder;
        this.focusAnimation.setValue(0);
        this.suggestionsAnimation.setValue(0);

        for (int i = 0; i < MAX_SUGGESTIONS; i++) {
            suggestionHoverAnims.add(new Animation().animate(0, 0.2f, Easings.EXPO_OUT));
            suggestionSelectAnims.add(new Animation().animate(0, 0.2f, Easings.EXPO_OUT));
        }
    }

    private void removeLastAnimatedChar() {
        for (int i = animatedChars.size() - 1; i >= 0; i--) {
            if (!animatedChars.get(i).removing) {
                animatedChars.get(i).startRemove();
                return;
            }
        }
    }

    private void removeAllAnimatedChars() {
        for (AnimatedChar ac : animatedChars) {
            if (!ac.removing) {
                ac.startRemove();
            }
        }
    }

    private void setAnimatedCharsInstant(String newText) {
        animatedChars.clear();
        for (char c : newText.toCharArray()) {
            animatedChars.add(new AnimatedChar(c, true));
        }
    }

    private int clampCursor(int position) {
        return Math.max(0, Math.min(position, text.length()));
    }

    private boolean hasSelection() {
        return cursorPosition != selectionPosition;
    }

    private int getSelectionStart() {
        return Math.min(cursorPosition, selectionPosition);
    }

    private int getSelectionEnd() {
        return Math.max(cursorPosition, selectionPosition);
    }

    private void syncTextSelectionFlag() {
        textSelected = hasSelection();
    }

    private void setCursorPosition(int position, boolean keepSelection) {
        cursorPosition = clampCursor(position);
        if (!keepSelection) {
            selectionPosition = cursorPosition;
        }
        syncTextSelectionFlag();
    }

    private void syncAfterTextMutation(boolean resetSuggestionIndex) {
        setAnimatedCharsInstant(text);
        if (resetSuggestionIndex) {
            selectedSuggestionIndex = -1;
        }
        cursorPosition = clampCursor(cursorPosition);
        selectionPosition = clampCursor(selectionPosition);
        syncTextSelectionFlag();
    }

    private void deleteSelectedText() {
        if (!hasSelection()) {
            return;
        }
        int start = getSelectionStart();
        int end = getSelectionEnd();
        text = text.substring(0, start) + text.substring(end);
        cursorPosition = start;
        selectionPosition = start;
        syncAfterTextMutation(true);
    }

    private void insertTextAtCursor(String insert) {
        if (insert == null || insert.isEmpty()) {
            return;
        }
        if (hasSelection()) {
            deleteSelectedText();
        }
        int available = MAX_TEXT_LENGTH - text.length();
        if (available <= 0) {
            return;
        }
        String toInsert = insert.substring(0, Math.min(insert.length(), available));
        text = text.substring(0, cursorPosition) + toInsert + text.substring(cursorPosition);
        cursorPosition += toInsert.length();
        selectionPosition = cursorPosition;
        syncAfterTextMutation(true);
    }

    private void deletePreviousCharacter() {
        if (hasSelection()) {
            deleteSelectedText();
            return;
        }
        if (cursorPosition <= 0) {
            return;
        }
        text = text.substring(0, cursorPosition - 1) + text.substring(cursorPosition);
        cursorPosition--;
        selectionPosition = cursorPosition;
        syncAfterTextMutation(true);
    }

    private void deleteNextCharacter() {
        if (hasSelection()) {
            deleteSelectedText();
            return;
        }
        if (cursorPosition >= text.length()) {
            return;
        }
        text = text.substring(0, cursorPosition) + text.substring(cursorPosition + 1);
        selectionPosition = cursorPosition;
        syncAfterTextMutation(true);
    }

    private boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private int getPreviousWordBoundary(int fromPos) {
        int pos = clampCursor(fromPos);
        if (pos <= 0) {
            return 0;
        }
        pos--;
        while (pos > 0 && !isWordChar(text.charAt(pos))) {
            pos--;
        }
        while (pos > 0 && isWordChar(text.charAt(pos - 1))) {
            pos--;
        }
        return pos;
    }

    private int getNextWordBoundary(int fromPos) {
        int len = text.length();
        int pos = clampCursor(fromPos);
        if (pos >= len) {
            return len;
        }
        while (pos < len && !isWordChar(text.charAt(pos))) {
            pos++;
        }
        while (pos < len && isWordChar(text.charAt(pos))) {
            pos++;
        }
        return pos;
    }

    private void deletePreviousWord() {
        if (hasSelection()) {
            deleteSelectedText();
            return;
        }
        int start = getPreviousWordBoundary(cursorPosition);
        if (start == cursorPosition) {
            return;
        }
        text = text.substring(0, start) + text.substring(cursorPosition);
        cursorPosition = start;
        selectionPosition = cursorPosition;
        syncAfterTextMutation(true);
    }

    private void deleteNextWord() {
        if (hasSelection()) {
            deleteSelectedText();
            return;
        }
        int end = getNextWordBoundary(cursorPosition);
        if (end == cursorPosition) {
            return;
        }
        text = text.substring(0, cursorPosition) + text.substring(end);
        selectionPosition = cursorPosition;
        syncAfterTextMutation(true);
    }

    private int getCursorFromMouseX(float mouseX) {
        float startX = x + 19f;
        float localX = mouseX - startX;
        if (localX <= 0f) {
            return 0;
        }
        float prevWidth = 0f;
        for (int i = 1; i <= text.length(); i++) {
            float currentWidth = Fonts.sfuy.getWidth(text.substring(0, i), 8);
            float midpoint = prevWidth + (currentWidth - prevWidth) * 0.5f;
            if (localX < midpoint) {
                return i - 1;
            }
            prevWidth = currentWidth;
        }
        return text.length();
    }

    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks, float guiAnimation) {
        focusAnimation.update();
        suggestionsAnimation.update();
        for (int i = 0; i < suggestionHoverAnims.size(); i++) {
            suggestionHoverAnims.get(i).update();
            suggestionSelectAnims.get(i).update();
        }

        for (AnimatedChar ac : animatedChars) {
            ac.update();
        }

        for (int i = animatedChars.size() - 1; i >= 0; i--) {
            if (animatedChars.get(i).isFinishedRemoving()) {
                animatedChars.remove(i);
            }
        }

        focusAnimation.animate(isFocused ? 1 : 0, 0.3, Easings.EXPO_OUT);
        updateSuggestions();

        boolean hasSuggestions = !suggestions.isEmpty() && isFocused && !text.isEmpty();
        suggestionsAnimation.animate(hasSuggestions ? 1 : 0, 0.3, Easings.EXPO_OUT);

        int themeColor = Theme.MainColor(0);
        float alpha = guiAnimation;

        if (alpha < 0.01f) return;

        GlStateManager.pushMatrix();
        GlStateManager.translatef(0, (1 - alpha) * -50, 0);

        GaussianBlur.startBlur();
        RenderUtility.drawRoundedRect(x, y, width, height, 8, -1);
        GaussianBlur.endBlur(5, 2);

        float focusValue = (float) focusAnimation.getValue();
        if (focusValue > 0.01f) {
            RenderUtility.drawRoundedRect(x - 0.5f, y - 0.5f, width + 1, height + 1, 8.5f,
                    ColorUtils.setAlpha(themeColor, (int)(40 * focusValue * alpha)));
        }

        RenderUtility.drawRoundedRect(x, y, width, height, 8, ColorUtils.rgba(20, 20, 20, (int)(185 * alpha)));

        ClientFonts.icons_client[20].drawCenteredString(matrixStack, "a", x + 10, y + (height - 8f) / 2 + 2,
                ColorUtils.rgba(255, 255, 255, (int)(180 * alpha)));

        if (text.isEmpty() && !isFocused && animatedChars.isEmpty()) {
            Fonts.sfuy.drawText(matrixStack, placeholder, x + 19, y + (height - 8f) / 2,
                    ColorUtils.rgba(255, 255, 255, (int)(140 * alpha)), 8);
        } else {
            if (hasSelection() && !text.isEmpty() && isFocused) {
                int selStart = getSelectionStart();
                int selEnd = getSelectionEnd();
                float selX = x + 19 + Fonts.sfuy.getWidth(text.substring(0, selStart), 8);
                float selWidth = Fonts.sfuy.getWidth(text.substring(selStart, selEnd), 8);
                if (selWidth > 0.01f) {
                    RenderUtility.drawRoundedRect(selX, y + (height - 10f) / 2, selWidth, 10, 2,
                            ColorUtils.setAlpha(themeColor, (int)(100 * alpha)));
                }
            }

            float charX = x + 19;
            float baseY = y + (height - 8f) / 2;

            for (AnimatedChar ac : animatedChars) {
                float charAlpha = (float) ac.alphaAnim.getValue();
                float charScale = (float) ac.scaleAnim.getValue();
                float charOffsetY = (float) ac.offsetYAnim.getValue();
                float animWidth = (float) ac.widthAnim.getValue();

                if (charAlpha > 0.01f) {
                    int charColor = ColorUtils.rgba(255, 255, 255, (int)(255 * charAlpha * alpha));
                    GlStateManager.pushMatrix();

                    float centerX = charX + animWidth / 2f;
                    float centerY = baseY + 4f;

                    GlStateManager.translatef(centerX, centerY, 0);
                    GlStateManager.scalef(charScale, charScale, 1);
                    GlStateManager.translatef(-centerX, -centerY, 0);

                    Fonts.sfuy.drawText(matrixStack, ac.characterText, charX, baseY + charOffsetY, charColor, 8);

                    GlStateManager.popMatrix();
                }

                charX += animWidth;
            }

            if (isFocused && System.currentTimeMillis() % 1000 > 500) {
                float caretX = x + 19 + Fonts.sfuy.getWidth(text.substring(0, clampCursor(cursorPosition)), 8);
                Fonts.sfuy.drawText(matrixStack, "_", caretX, baseY,
                        ColorUtils.rgba(255, 255, 255, (int)(255 * alpha)), 8);
            }
        }

        if (suggestionsAnimation.getValue() > 0.01f && alpha > 0.01f) {
            renderSuggestions(matrixStack, mouseX, mouseY, alpha, themeColor);
        }

        GlStateManager.popMatrix();
    }

    private void renderSuggestions(MatrixStack matrixStack, int mouseX, int mouseY, float alpha, int themeColor) {
        float suggestionAlpha = (float) suggestionsAnimation.getValue() * alpha;
        float dropdownY = y + height + 6;
        float dropdownHeight = Math.min(suggestions.size(), MAX_SUGGESTIONS) * 18 + 4;
        float animatedHeight = dropdownHeight * (float) suggestionsAnimation.getValue();

        GaussianBlur.startBlur();
        RenderUtility.drawRoundedRect(x, dropdownY, width, animatedHeight, 6, -1);
        GaussianBlur.endBlur(5, 2);

        RenderUtility.drawRoundedRect(x - 0.5f, dropdownY - 0.5f, width + 1, animatedHeight + 1, 6.5f,
                ColorUtils.setAlpha(themeColor, (int)(20 * suggestionAlpha)));
        RenderUtility.drawRoundedRect(x, dropdownY, width, animatedHeight, 6,
                ColorUtils.rgba(15, 15, 15, (int)(180 * suggestionAlpha)));

        for (int i = 0; i < Math.min(suggestions.size(), MAX_SUGGESTIONS); i++) {
            String suggestion = suggestions.get(i);
            float itemY = dropdownY + 2 + i * 18;
            float itemHeight = 16;

            float itemDelay = i * 0.05f;
            float itemAlpha = (float) Math.max(0, Math.min(1, (suggestionsAnimation.getValue() - itemDelay) / (1 - itemDelay)));
            itemAlpha *= alpha;

            if (itemAlpha < 0.01f) continue;

            boolean hovered = MathUtil.isHovered(mouseX, mouseY, x + 2, itemY, width - 4, itemHeight);
            boolean selected = (i == selectedSuggestionIndex);

            suggestionHoverAnims.get(i).animate(hovered ? 1 : 0, 0.2f, Easings.EXPO_OUT);
            suggestionSelectAnims.get(i).animate(selected ? 1 : 0, 0.2f, Easings.EXPO_OUT);

            float combinedValue = Math.max((float) suggestionHoverAnims.get(i).getValue(),
                    (float) suggestionSelectAnims.get(i).getValue());

            if (combinedValue > 0.01f) {
                RenderUtility.drawRoundedRect(x + 2, itemY, width - 4, itemHeight, 4,
                        ColorUtils.setAlpha(themeColor, (int)(25 * combinedValue * itemAlpha)));
            }

            int matchLength = Math.min(text.length(), suggestion.length());
            String matchedPart = suggestion.substring(0, matchLength);
            String remainingPart = suggestion.substring(matchLength);

            float textX = x + 8;
            float textY = itemY + itemHeight / 2f - 3;

            Fonts.sfuy.drawText(matrixStack, matchedPart, textX, textY,
                    ColorUtils.setAlpha(themeColor, (int)(200 * itemAlpha)), 7.5f);
            Fonts.sfuy.drawText(matrixStack, remainingPart, textX + Fonts.sfuy.getWidth(matchedPart, 7.5f), textY,
                    ColorUtils.rgba(255, 255, 255, (int)(160 * itemAlpha)), 7.5f);

            if (selected && itemAlpha > 0.5f) {
                String enterHint = "ENTER";
                float enterX = x + width - Fonts.sfuy.getWidth(enterHint, 6f) - 8;
                Fonts.sfuy.drawText(matrixStack, enterHint, enterX, textY + 0.5f,
                        ColorUtils.rgba(255, 255, 255, (int)(100 * itemAlpha)), 6f);
            }
        }
    }

    private void updateSuggestions() {
        if (text.equals(lastSuggestionText) && isFocused == lastSuggestionFocused) {
            return;
        }

        lastSuggestionText = text;
        lastSuggestionFocused = isFocused;
        suggestions.clear();

        if (text.isEmpty() || !isFocused) {
            selectedSuggestionIndex = -1;
            return;
        }

        String query = text;

        for (Module module : Harmony.getInstance().getModuleManager().getModules()) {
            if (!module.isVisibleInClickGui()) {
                continue;
            }

            String name = module.getName();
            if (name.length() >= query.length() && name.regionMatches(true, 0, query, 0, query.length())) {
                suggestions.add(name);
            }
        }

        suggestions.sort((a, b) -> {
            boolean aExact = a.equalsIgnoreCase(query);
            boolean bExact = b.equalsIgnoreCase(query);
            if (aExact != bExact) return aExact ? -1 : 1;
            return a.compareToIgnoreCase(b);
        });

        while (suggestions.size() > MAX_SUGGESTIONS) {
            suggestions.remove(suggestions.size() - 1);
        }

        if (selectedSuggestionIndex >= suggestions.size()) {
            selectedSuggestionIndex = suggestions.isEmpty() ? -1 : suggestions.size() - 1;
        }
    }

    private void copyToClipboard(String str) {
        Minecraft.getInstance().keyboardListener.setClipboardString(str);
    }

    private void playTypingSound() {
        SoundUtil.playSound("searchtyping.wav");
    }

    private void resetSuggestions() {
        suggestions.clear();
        selectedSuggestionIndex = -1;
        lastSuggestionText = null;
    }

    private void unfocus() {
        isFocused = false;
        typing = false;
        cursorPosition = clampCursor(cursorPosition);
        selectionPosition = cursorPosition;
        selectionDragActive = false;
        syncTextSelectionFlag();
        resetSuggestions();
    }

    public boolean charTyped(char codePoint, int modifiers) {
        if (!isFocused) return false;

        if (codePoint >= 32) {
            insertTextAtCursor(String.valueOf(codePoint));
            playTypingSound();
            return true;
        }
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isFocused) {
            if (ClientUtility.ctrlIsDown() && keyCode == GLFW.GLFW_KEY_F) {
                isFocused = true;
                typing = true;
                SoundUtil.playSound("searchfield.wav");
                return true;
            }
            return false;
        }

        boolean ctrlDown = ClientUtility.ctrlIsDown();
        boolean shiftDown = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0
                || GLFW.glfwGetKey(Minecraft.getInstance().getMainWindow().getHandle(), GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(Minecraft.getInstance().getMainWindow().getHandle(), GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;

        if (ctrlDown) {
            switch (keyCode) {
                case GLFW.GLFW_KEY_A:
                    selectionPosition = 0;
                    cursorPosition = text.length();
                    syncTextSelectionFlag();
                    playTypingSound();
                    return true;

                case GLFW.GLFW_KEY_C:
                    if (!text.isEmpty()) {
                        if (hasSelection()) {
                            copyToClipboard(text.substring(getSelectionStart(), getSelectionEnd()));
                        } else {
                            copyToClipboard(text);
                        }
                        playTypingSound();
                    }
                    return true;

                case GLFW.GLFW_KEY_X:
                    if (!text.isEmpty()) {
                        if (hasSelection()) {
                            copyToClipboard(text.substring(getSelectionStart(), getSelectionEnd()));
                            deleteSelectedText();
                        } else {
                            copyToClipboard(text);
                            text = "";
                            cursorPosition = 0;
                            selectionPosition = 0;
                            syncAfterTextMutation(true);
                        }
                        playTypingSound();
                    }
                    return true;

                case GLFW.GLFW_KEY_V:
                    try {
                        String pastedText = ClientUtility.pasteString();
                        if (pastedText != null && !pastedText.isEmpty()) {
                            insertTextAtCursor(pastedText);
                            playTypingSound();
                        }
                    } catch (Exception ignored) {}
                    return true;

                case GLFW.GLFW_KEY_BACKSPACE:
                    deletePreviousWord();
                    playTypingSound();
                    return true;

                case GLFW.GLFW_KEY_DELETE:
                    deleteNextWord();
                    playTypingSound();
                    return true;

                case GLFW.GLFW_KEY_LEFT:
                    setCursorPosition(getPreviousWordBoundary(cursorPosition), shiftDown);
                    return true;

                case GLFW.GLFW_KEY_RIGHT:
                    setCursorPosition(getNextWordBoundary(cursorPosition), shiftDown);
                    return true;
            }
        }

        if (!suggestions.isEmpty()) {
            if (keyCode == GLFW.GLFW_KEY_DOWN) {
                selectedSuggestionIndex = (selectedSuggestionIndex + 1) % Math.min(suggestions.size(), MAX_SUGGESTIONS);
                selectionPosition = cursorPosition;
                syncTextSelectionFlag();
                playTypingSound();
                return true;
            }

            if (keyCode == GLFW.GLFW_KEY_UP) {
                selectedSuggestionIndex--;
                if (selectedSuggestionIndex < 0) {
                    selectedSuggestionIndex = Math.min(suggestions.size(), MAX_SUGGESTIONS) - 1;
                }
                selectionPosition = cursorPosition;
                syncTextSelectionFlag();
                playTypingSound();
                return true;
            }
        }

        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            setCursorPosition(cursorPosition - 1, shiftDown);
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            setCursorPosition(cursorPosition + 1, shiftDown);
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_HOME) {
            setCursorPosition(0, shiftDown);
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_END) {
            setCursorPosition(text.length(), shiftDown);
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ENTER) {
            if (selectedSuggestionIndex >= 0 && selectedSuggestionIndex < suggestions.size()) {
                String selected = suggestions.get(selectedSuggestionIndex);
                setAnimatedCharsInstant(selected);
                text = selected;
                cursorPosition = text.length();
                selectionPosition = cursorPosition;
                syncTextSelectionFlag();
                resetSuggestions();
                SoundUtil.playSound("click.wav");
            } else {
                unfocus();
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            unfocus();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            int before = text.length();
            deletePreviousCharacter();
            if (text.length() != before) {
                playTypingSound();
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            int before = text.length();
            deleteNextCharacter();
            if (text.length() != before) {
                playTypingSound();
            }
            return true;
        }

        return false;
    }

    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (suggestionsAnimation.getValue() > 0.5f && !suggestions.isEmpty()) {
            float dropdownY = y + height + 6;

            for (int i = 0; i < Math.min(suggestions.size(), MAX_SUGGESTIONS); i++) {
                float itemY = dropdownY + 2 + i * 18;

                if (MathUtil.isHovered((float) mouseX, (float) mouseY, x + 2, itemY, width - 4, 16)) {
                    String selected = suggestions.get(i);
                    setAnimatedCharsInstant(selected);
                    text = selected;
                    cursorPosition = text.length();
                    selectionPosition = cursorPosition;
                    syncTextSelectionFlag();
                    resetSuggestions();
                    SoundUtil.playSound("click.wav");
                    return true;
                }
            }
        }

        boolean wasAlreadyFocused = isFocused;
        isFocused = MathUtil.isHovered((float) mouseX, (float) mouseY, x, y, width, height);
        typing = isFocused;
        selectionDragActive = false;

        if (!isFocused) {
            selectionPosition = cursorPosition;
            syncTextSelectionFlag();
            resetSuggestions();
        } else {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                int clickedCursor = getCursorFromMouseX((float) mouseX);
                setCursorPosition(clickedCursor, false);
                selectionDragAnchor = clickedCursor;
                selectionDragActive = true;
            }
            if (!wasAlreadyFocused) {
                SoundUtil.playSound("searchfield.wav");
            }
        }

        return isFocused;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (!isFocused || button != GLFW.GLFW_MOUSE_BUTTON_LEFT || !selectionDragActive) {
            return false;
        }
        int dragCursor = getCursorFromMouseX((float) mouseX);
        cursorPosition = clampCursor(dragCursor);
        selectionPosition = clampCursor(selectionDragAnchor);
        syncTextSelectionFlag();
        return true;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            selectionDragActive = false;
        }
        return false;
    }

    public void setText(String text) {
        this.text = text == null ? "" : text;
        if (this.text.length() > MAX_TEXT_LENGTH) {
            this.text = this.text.substring(0, MAX_TEXT_LENGTH);
        }
        cursorPosition = this.text.length();
        selectionPosition = cursorPosition;
        syncAfterTextMutation(false);
    }

    public boolean isEmpty() {
        return text.isEmpty();
    }
}
