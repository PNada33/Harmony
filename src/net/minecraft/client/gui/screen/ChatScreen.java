package net.minecraft.client.gui.screen;

import xd.harm.utils.animations.Animation;
import xd.harm.utils.animations.Direction;
import xd.harm.utils.animations.impl.DecelerateAnimation;
import xd.harm.utils.drag.DragManager;
import xd.harm.utils.drag.Dragging;
import xd.harm.utils.math.MathUtil;
import xd.harm.utils.render.Cursors;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.CommandSuggestionHelper;
import net.minecraft.client.gui.NewChatGui;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TranslationTextComponent;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.atomic.AtomicBoolean;

public class ChatScreen extends Screen {
    private static final int INPUT_BASE_X = 4;
    private static final int SYSTEM_PREFIX_LEFT = 6;
    private static final int SYSTEM_PREFIX_GAP = 5;
    private static final int SYSTEM_PREFIX_WIDTH = 24;

    private String historyBuffer = "";
    private int sentHistoryCursor = -1;
    protected TextFieldWidget inputField;
    private String defaultInputFieldText = "";
    private CommandSuggestionHelper commandSuggestionHelper;
    public static Animation hotbarAnimation;
    public static Animation buttonAnimation;
    public static boolean hide;
    public static boolean correctPosition = true;
    private boolean closing = false;
    private float systemPrefixAnimation = 0.0F;

    public ChatScreen(String defaultText) {
        super(NarratorChatListener.EMPTY);
        this.defaultInputFieldText = defaultText;
    }

    public static float getHotbarAnimationOutput() {
        return hotbarAnimation != null ? (float) hotbarAnimation.getOutput() : 0.0f;
    }

    public static float getButtonAnimationOutput() {
        return buttonAnimation != null ? (float) buttonAnimation.getOutput() : 0.0f;
    }

    @Override
    protected void init() {

        hotbarAnimation = new DecelerateAnimation(150, 1, Direction.FORWARDS);
        buttonAnimation = new DecelerateAnimation(150, 1, Direction.FORWARDS);
        closing = false;



        this.minecraft.keyboardListener.enableRepeatEvents(true);
        this.sentHistoryCursor = this.minecraft.ingameGUI.getChatGUI().getSentMessages().size();
        this.inputField = new TextFieldWidget(this.font, 4, this.height - 12, this.width - 4, 12, new TranslationTextComponent("chat.editBox")) {
            @Override
            protected IFormattableTextComponent getNarrationMessage() {
                return super.getNarrationMessage().appendString(ChatScreen.this.commandSuggestionHelper.getSuggestionMessage());
            }
        };
        this.inputField.setMaxStringLength(256);
        this.inputField.setEnableBackgroundDrawing(false);
        this.inputField.setText(this.defaultInputFieldText);
        this.inputField.setResponder(this::func_212997_a);
        this.children.add(this.inputField);
        this.commandSuggestionHelper = new CommandSuggestionHelper(this.minecraft, this, this.inputField, this.font, false, false, 1, 10, true, -805306368);
        this.commandSuggestionHelper.init();
        this.setFocusedDefault(this.inputField);
    }

    @Override
    public void resize(Minecraft minecraft, int width, int height) {
        String s = this.inputField.getText();
        this.init(minecraft, width, height);
        this.setChatLine(s);
        this.commandSuggestionHelper.init();
    }

    @Override
    public void onClose() {
        startClosing();
        this.minecraft.keyboardListener.enableRepeatEvents(false);
        this.minecraft.ingameGUI.getChatGUI().resetScroll();
        for (Dragging dragging : DragManager.draggables.values()) {
            dragging.onRelease(0);
        }
        DragManager.save();
    }

    private void startClosing() {
        if (!closing && hotbarAnimation != null) {
            closing = true;
            hotbarAnimation.setDirection(Direction.BACKWARDS);
            buttonAnimation.setDirection(Direction.BACKWARDS);
        }
    }

    @Override
    public void tick() {
        this.inputField.tick();


        if (hotbarAnimation != null) {
            hotbarAnimation.isDone();
        }

        if (buttonAnimation != null) {
            buttonAnimation.isDone();
        }


        if (closing && hotbarAnimation != null && hotbarAnimation.finished(Direction.BACKWARDS)) {
            this.minecraft.displayGuiScreen(null);
        }
    }

    private void func_212997_a(String p_212997_1_) {
        String s = this.inputField.getText();
        this.commandSuggestionHelper.shouldAutoSuggest(!s.equals(this.defaultInputFieldText));
        this.commandSuggestionHelper.init();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.commandSuggestionHelper.onKeyPressed(keyCode, scanCode, modifiers)) {
            return true;
        } else if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        } else if (keyCode == 256) {
            startClosing();
            return true;
        } else if (keyCode == 257 || keyCode == 335) {
            String s = this.inputField.getText().trim();
            if (!s.isEmpty()) {
                this.sendMessage(s);
            }
            startClosing();
            return true;
        } else if (keyCode == 265) {
            this.getSentHistory(-1);
            return true;
        } else if (keyCode == 264) {
            this.getSentHistory(1);
            return true;
        } else if (keyCode == 266) {
            this.minecraft.ingameGUI.getChatGUI().addScrollPos((double) (this.minecraft.ingameGUI.getChatGUI().getLineCount() - 1));
            return true;
        } else if (keyCode == 267) {
            this.minecraft.ingameGUI.getChatGUI().addScrollPos((double) (-this.minecraft.ingameGUI.getChatGUI().getLineCount() + 1));
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta > 1.0D) {
            delta = 1.0D;
        }
        if (delta < -1.0D) {
            delta = -1.0D;
        }
        if (this.commandSuggestionHelper.onScroll(delta)) {
            return true;
        } else {
            if (!hasShiftDown()) {
                delta *= 7.0D;
            }
            this.minecraft.ingameGUI.getChatGUI().addScrollPos(delta);
            return true;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.commandSuggestionHelper.onClick((double) ((int) mouseX), (double) ((int) mouseY), button)) {
            return true;
        } else {
            if (button == 0) {
                int buttonWidth = 12;
                int buttonHeight = 12;
                int buttonSpacing = 2;
                float buttonAnimationOutput = getButtonAnimationOutput();
                int buttonY = (int) (this.height - 14 * buttonAnimationOutput);
                
                int pButtonX = this.width - buttonWidth - 4;
                int hButtonX = pButtonX - buttonWidth - buttonSpacing;
                
                if (mouseX >= pButtonX && mouseX <= pButtonX + buttonWidth && mouseY >= buttonY && mouseY <= buttonY + buttonHeight) {
                    Dragging.alignmentEnabled = !Dragging.alignmentEnabled;
                    DragManager.save();
                    return true;
                }
                
                if (mouseX >= hButtonX && mouseX <= hButtonX + buttonWidth && mouseY >= buttonY && mouseY <= buttonY + buttonHeight) {
                    hide = !hide;
                    return true;
                }
                
                NewChatGui newChatGui = this.minecraft.ingameGUI.getChatGUI();

                float animationOffset = getHotbarAnimationOutput();
                double correctedMouseY = correctPosition ? mouseY + (15 * (1 - animationOffset) + 15) : mouseY;

                if (newChatGui.func_238491_a_(mouseX, correctedMouseY)) {
                    return true;
                }

                Style style = newChatGui.func_238494_b_(mouseX, correctedMouseY);
                if (style != null && this.handleComponentClicked(style)) {
                    return true;
                }
            }

            for (Dragging dragging : DragManager.draggables.values()) {
                if (dragging.getModule() != null && dragging.getModule().isState()) {
                    if (dragging.onClick(mouseX, mouseY, button)) {
                        break;
                    }
                }
            }

            return this.inputField.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean wasDragging = false;
        for (Dragging dragging : DragManager.draggables.values()) {
            if (dragging.isDragging()) {
                wasDragging = true;
            }
            dragging.onRelease(button);
        }
        if (button == 0 && wasDragging) {
            DragManager.save();
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected void insertText(String text, boolean overwrite) {
        if (overwrite) {
            this.inputField.setText(text);
        } else {
            this.inputField.writeText(text);
        }
    }

    public void getSentHistory(int msgPos) {
        int i = this.sentHistoryCursor + msgPos;
        int j = this.minecraft.ingameGUI.getChatGUI().getSentMessages().size();
        i = MathHelper.clamp(i, 0, j);
        if (i != this.sentHistoryCursor) {
            if (i == j) {
                this.sentHistoryCursor = j;
                this.inputField.setText(this.historyBuffer);
            } else {
                if (this.sentHistoryCursor == j) {
                    this.historyBuffer = this.inputField.getText();
                }
                this.inputField.setText(this.minecraft.ingameGUI.getChatGUI().getSentMessages().get(i));
                this.commandSuggestionHelper.shouldAutoSuggest(false);
                this.sentHistoryCursor = i;
            }
        }
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        restoreGuiRenderState();
        this.setListener(this.inputField);
        this.inputField.setFocused2(true);

        float animationOutput = hotbarAnimation != null ? (float) hotbarAnimation.getOutput() : 1.0f;

        int chatY = (int) (this.height - 14 * animationOutput);
        int buttonTotalWidth = 12 + 2 + 12 + 4;
        fill(matrixStack, 2, chatY, this.width - 2 - buttonTotalWidth, chatY + 12, this.minecraft.gameSettings.getChatBackgroundColor(Integer.MIN_VALUE));

        float targetPrefixAnimation = isSystemCommandInput() ? 1.0F : 0.0F;
        float prefixStep = MathUtil.clamp(0.20F + partialTicks * 0.08F, 0.20F, 0.32F);
        systemPrefixAnimation += (targetPrefixAnimation - systemPrefixAnimation) * prefixStep;
        if (targetPrefixAnimation == 0.0F && systemPrefixAnimation < 0.01F) {
            systemPrefixAnimation = 0.0F;
        }

        float prefixProgress = easeOutCubic(systemPrefixAnimation);
        int prefixWidth = (int) ((SYSTEM_PREFIX_WIDTH + SYSTEM_PREFIX_GAP) * prefixProgress);
        if (systemPrefixAnimation > 0.01F) {
            renderSystemCommandPrefix(matrixStack, chatY, prefixProgress);
        }

        this.inputField.y = (int) (this.height - 12 * animationOutput);
        this.inputField.setX(INPUT_BASE_X + prefixWidth);
        this.inputField.setWidth(this.width - 8 - buttonTotalWidth - prefixWidth);
        this.inputField.render(matrixStack, mouseX, mouseY, partialTicks);

        this.commandSuggestionHelper.drawSuggestionList(matrixStack, mouseX, mouseY);

        float animationOffset = animationOutput;
        double correctedMouseY = correctPosition ? mouseY + (15 * (1 - animationOffset)) + 15 : mouseY;

        Style style = this.minecraft.ingameGUI.getChatGUI().func_238494_b_(mouseX, correctedMouseY);
        if (style != null && style.getHoverEvent() != null) {
            this.renderComponentHoverEffect(matrixStack, style, mouseX, mouseY);
        }

        AtomicBoolean anyHovered = new AtomicBoolean(false);
        DragManager.draggables.values().forEach(dragging -> {
            if (dragging.getModule() != null && dragging.getModule().isState()) {
                if (MathUtil.isHovered(mouseX, mouseY, dragging.getX(), dragging.getY(), dragging.getWidth(), dragging.getHeight())) {
                    anyHovered.set(true);
                }
                dragging.onDraw(mouseX, mouseY, Minecraft.getInstance().getMainWindow());
            }
        });

        if (anyHovered.get()) {
            GLFW.glfwSetCursor(Minecraft.getInstance().getMainWindow().getHandle(), Cursors.HAND);
        } else {
            GLFW.glfwSetCursor(Minecraft.getInstance().getMainWindow().getHandle(), Cursors.ARROW);
        }

        int buttonBgColor = this.minecraft.gameSettings.getChatBackgroundColor(Integer.MIN_VALUE);
        int buttonTextColor = 0xE0E0E0;
        
        int buttonWidth = 12;
        int buttonHeight = 12;
        int buttonSpacing = 2;
        float buttonAnimationOutput = getButtonAnimationOutput();
        int buttonY = (int) (this.height - 14 * buttonAnimationOutput);
        
        int pButtonX = this.width - buttonWidth - 4;
        int hButtonX = pButtonX - buttonWidth - buttonSpacing;
        
        boolean pHovered = mouseX >= pButtonX && mouseX <= pButtonX + buttonWidth && mouseY >= buttonY && mouseY <= buttonY + buttonHeight;
        boolean hHovered = mouseX >= hButtonX && mouseX <= hButtonX + buttonWidth && mouseY >= buttonY && mouseY <= buttonY + buttonHeight;
        
        int pBgColor = pHovered ? this.minecraft.gameSettings.getChatBackgroundColor(0x80FFFFFF) : buttonBgColor;
        int hBgColor = hHovered ? this.minecraft.gameSettings.getChatBackgroundColor(0x80FFFFFF) : buttonBgColor;
        int dTextColor = Dragging.alignmentEnabled ? 0x9AFF9A : buttonTextColor;
        int hTextColor = hide ? 0x9AB5FF : buttonTextColor;
        
        fill(matrixStack, pButtonX, buttonY, pButtonX + buttonWidth, buttonY + buttonHeight, pBgColor);
        fill(matrixStack, hButtonX, buttonY, hButtonX + buttonWidth, buttonY + buttonHeight, hBgColor);
        
        String pText = "D";
        String hText = "H";
        
        drawCenteredString(matrixStack, this.font, pText, pButtonX + buttonWidth / 2, buttonY + 2, dTextColor);
        drawCenteredString(matrixStack, this.font, hText, hButtonX + buttonWidth / 2, buttonY + 2, hTextColor);

        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    private boolean isSystemCommandInput() {
        String text = this.inputField != null ? this.inputField.getText() : "";
        return text != null && text.startsWith(".");
    }

    private void renderSystemCommandPrefix(MatrixStack matrixStack, int chatY, float progress) {
        long now = System.currentTimeMillis();
        float pulse = 0.5F + 0.5F * (float) Math.sin(now / 155.0D);
        float x = SYSTEM_PREFIX_LEFT - (1.0F - progress) * 8.0F;
        float y = chatY + 2.0F;
        int promptAlpha = (int) (232.0F * progress);
        int glowAlpha = (int) (82.0F * progress);
        int caretAlpha = (int) ((104.0F + pulse * 126.0F) * progress);

        drawPromptLine(x + 1.5F, y + 1.2F, x + 7.2F, y + 4.8F, 3.2F, withAlpha(0xFF000000, glowAlpha));
        drawPromptLine(x + 1.5F, y + 8.4F, x + 7.2F, y + 4.8F, 3.2F, withAlpha(0xFF000000, glowAlpha));
        drawPromptLine(x + 1.5F, y + 1.2F, x + 7.2F, y + 4.8F, 1.55F, withAlpha(0xFF8BFFB0, promptAlpha));
        drawPromptLine(x + 1.5F, y + 8.4F, x + 7.2F, y + 4.8F, 1.55F, withAlpha(0xFF8BFFB0, promptAlpha));

        float caretY = y + 8.2F + (pulse - 0.5F) * 0.55F;
        drawPromptLine(x + 12.3F, caretY, x + 18.5F, caretY, 3.5F, withAlpha(0xFF000000, glowAlpha));
        drawPromptLine(x + 12.3F, caretY, x + 18.5F, caretY, 1.8F, withAlpha(0xFF6AA6FF, caretAlpha));
    }

    private float easeOutCubic(float value) {
        float clamped = MathUtil.clamp(value, 0.0F, 1.0F);
        float inverse = 1.0F - clamped;
        return 1.0F - inverse * inverse * inverse;
    }

    private void drawPromptLine(float x1, float y1, float x2, float y2, float width, int color) {
        float alpha = (float) (color >> 24 & 255) / 255.0F;
        float red = (float) (color >> 16 & 255) / 255.0F;
        float green = (float) (color >> 8 & 255) / 255.0F;
        float blue = (float) (color & 255) / 255.0F;

        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.disableTexture();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_LINE_SMOOTH);
        org.lwjgl.opengl.GL11.glHint(org.lwjgl.opengl.GL11.GL_LINE_SMOOTH_HINT, org.lwjgl.opengl.GL11.GL_NICEST);
        org.lwjgl.opengl.GL11.glLineWidth(width);
        org.lwjgl.opengl.GL11.glColor4f(red, green, blue, alpha);
        org.lwjgl.opengl.GL11.glBegin(org.lwjgl.opengl.GL11.GL_LINES);
        org.lwjgl.opengl.GL11.glVertex2f(x1, y1);
        org.lwjgl.opengl.GL11.glVertex2f(x2, y2);
        org.lwjgl.opengl.GL11.glEnd();
        org.lwjgl.opengl.GL11.glLineWidth(1.0F);
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_LINE_SMOOTH);
        org.lwjgl.opengl.GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        com.mojang.blaze3d.systems.RenderSystem.enableTexture();
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }

    private int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (MathHelper.clamp(alpha, 0, 255) << 24);
    }

    private void restoreGuiRenderState() {
        org.lwjgl.opengl.GL20.glUseProgram(0);
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
        org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_STENCIL_TEST);
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_TEXTURE_2D);
        org.lwjgl.opengl.GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        com.mojang.blaze3d.systems.RenderSystem.enableTexture();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        com.mojang.blaze3d.systems.RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void setChatLine(String p_208604_1_) {
        this.inputField.setText(p_208604_1_);
    }
}
