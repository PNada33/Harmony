package xd.harm.ui.basefinder;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector4f;
import xd.harm.utils.render.rect.RenderUtility;
import xd.harm.utils.render.gl.Stencil;
import xd.harm.utils.render.color.ColorUtils;
import xd.harm.utils.text.font.ClientFonts;
import xd.harm.modules.impl.player.BaseFinder;
import xd.harm.modules.impl.render.Theme;
import xd.harm.utils.math.Vector4i;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.List;

public class BaseFinderUI extends Screen {

    private double progress = 0;
    private String status = "Инициализация";
    private int foundPlayers = 0;
    private long startTime = System.currentTimeMillis();
    private String playerName = "Player";
    private int totalFoundPlayers = 0;
    private String currentPoint = "Точка: 0/0";
    private BaseFinder baseFinder;

    private List<FoundPlayer> foundPlayersList = new ArrayList<>();
    private FoundPlayer selectedPlayer = null;
    private List<String> consoleMessages = new ArrayList<>();
    private float consoleScrollOffset = 0;
    private float playersScrollOffset = 0;
    private boolean showPlayerInfo = false;
    private FoundPlayer infoPlayer = null;

    public BaseFinderUI(BaseFinder baseFinder) {
        super(new StringTextComponent("BaseFinder"));
        this.baseFinder = baseFinder;
        if (Minecraft.getInstance().player != null) {
            playerName = Minecraft.getInstance().player.getName().getString();
        }
        this.consoleMessages = baseFinder.getConsoleMessages();
    }

    public void updateProgress(double progress, String status, int foundPlayers) {
        this.progress = progress;
        this.status = status;
        this.foundPlayers = foundPlayers;
    }

    public void updateCurrentPoint(String currentPoint) {
        this.currentPoint = currentPoint;
    }

    public void updateConsole(List<String> messages) {
        this.consoleMessages = new ArrayList<>(messages);
    }

    public void addFoundPlayer(PlayerEntity player) {
        List<ItemStack> armorList = new ArrayList<>();
        for (ItemStack armor : player.getArmorInventoryList()) {
            armorList.add(armor);
        }

        FoundPlayer foundPlayer = new FoundPlayer(
                player.getName().getString(),
                player.getHealth(),
                player.getMaxHealth(),
                System.currentTimeMillis(),
                (int)player.getPosX(),
                (int)player.getPosY(),
                (int)player.getPosZ(),
                player instanceof AbstractClientPlayerEntity ? ((AbstractClientPlayerEntity) player).getLocationSkin() : null,
                armorList,
                player.getHeldItemMainhand(),
                player.getHeldItemOffhand()
        );
        foundPlayersList.add(foundPlayer);
        totalFoundPlayers++;
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);

        float rectWidth = 220;
        float rectHeight = 300;
        float rectX = (this.width - rectWidth) / 2;
        float rectY = (this.height - rectHeight) / 2;

        Vector4f bgRadius = new Vector4f(2, 2, 2, 2);
        RenderUtility.drawRoundedRect(rectX, rectY, rectWidth, rectHeight, bgRadius,
                new Vector4i(ColorUtils.rgba(0, 0, 0, 120), ColorUtils.rgba(0, 0, 0, 120),
                        ColorUtils.rgba(0, 0, 0, 120), ColorUtils.rgba(0, 0, 0, 120)));

        drawCenteredString(matrixStack, this.font, "BaseFinder", (int)(rectX + rectWidth / 2), (int)(rectY + 12), 0xFFFFFFFF);

        float currentY = rectY + 28;
        float padding = 12;

        drawStatSection(matrixStack, rectX + padding, currentY, rectWidth - padding * 2);
        currentY += 65;

        drawPauseButton(matrixStack, rectX + padding, currentY, rectWidth - padding * 2, mouseX, mouseY);
        currentY += 30;

        drawPlayersList(matrixStack, rectX + padding, currentY, rectWidth - padding * 2, rectHeight - (currentY - rectY) - padding, mouseX, mouseY);

        if (showPlayerInfo && infoPlayer != null) {
            drawPlayerInfoPanel(matrixStack, rectX + rectWidth + 10, rectY, mouseX, mouseY);
        }

        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    private void drawStatSection(MatrixStack matrixStack, float x, float y, float width) {
        Vector4f statRadius = new Vector4f(2, 2, 2, 2);
        RenderUtility.drawRoundedRect(x, y, width, 60, statRadius,
                new Vector4i(ColorUtils.rgba(0, 0, 0, 80), ColorUtils.rgba(0, 0, 0, 80),
                        ColorUtils.rgba(0, 0, 0, 80), ColorUtils.rgba(0, 0, 0, 80)));

        float textY = y + 6;

        long workTime = (System.currentTimeMillis() - startTime) / 1000;
        String timeStr = String.format("%02d:%02d", (workTime % 3600) / 60, workTime % 60);
        drawString(matrixStack, this.font, "Время: " + timeStr, (int)(x + 6), (int)textY, 0xFFAAAAAA);
        textY += 10;

        drawString(matrixStack, this.font, String.format("%.1f%% | Найдено: %d", progress, totalFoundPlayers), (int)(x + 6), (int)textY, 0xFF00FF00);
        textY += 10;

        String shortStatus = status.length() > 22 ? status.substring(0, 19) + "..." : status;
        drawString(matrixStack, this.font, shortStatus, (int)(x + 6), (int)textY, 0xFFFFFF00);
        textY += 10;

        String shortPoint = currentPoint.length() > 22 ? currentPoint.substring(0, 19) + "..." : currentPoint;
        drawString(matrixStack, this.font, shortPoint, (int)(x + 6), (int)textY, 0xFFCCCCCC);
    }

    private void drawPauseButton(MatrixStack matrixStack, float x, float y, float width, int mouseX, int mouseY) {
        boolean isPaused = baseFinder.isPaused();
        boolean isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 22;

        int buttonAlpha;
        if (isPaused) {
            buttonAlpha = 150;
        } else if (isHovered) {
            buttonAlpha = 100;
        } else {
            buttonAlpha = 60;
        }

        String buttonText = isPaused ? "Продолжить" : "Пауза";

        Vector4f btnRadius = new Vector4f(2, 2, 2, 2);
        RenderUtility.drawRoundedRect(x, y, width, 22, btnRadius,
                new Vector4i(ColorUtils.rgba(0, 0, 0, buttonAlpha), ColorUtils.rgba(0, 0, 0, buttonAlpha),
                        ColorUtils.rgba(0, 0, 0, buttonAlpha), ColorUtils.rgba(0, 0, 0, buttonAlpha)));

        drawCenteredString(matrixStack, this.font, buttonText, (int)(x + width / 2), (int)(y + 7), 0xFFFFFFFF);
    }

    private void drawPlayersList(MatrixStack matrixStack, float x, float y, float width, float height, int mouseX, int mouseY) {
        drawString(matrixStack, this.font, "Игроки: " + foundPlayersList.size(), (int)x, (int)y, 0xFFFFFFFF);

        float listY = y + 12;
        float listHeight = height - 12;

        Vector4f listRadius = new Vector4f(2, 2, 2, 2);
        RenderUtility.drawRoundedRect(x, listY, width, listHeight, listRadius,
                new Vector4i(ColorUtils.rgba(0, 0, 0, 140), ColorUtils.rgba(0, 0, 0, 140),
                        ColorUtils.rgba(0, 0, 0, 140), ColorUtils.rgba(0, 0, 0, 140)));

        if (foundPlayersList.isEmpty()) {
            drawCenteredString(matrixStack, this.font, "Пусто", (int)(x + width / 2), (int)(listY + listHeight / 2 - 4), 0xFF666666);
            return;
        }

        float itemHeight = 28;
        float currentY = listY + 5 + playersScrollOffset;

        for (int i = 0; i < foundPlayersList.size(); i++) {
            FoundPlayer player = foundPlayersList.get(i);
            float playerY = currentY + (i * itemHeight);

            if (playerY + itemHeight < listY + 8 || playerY > listY + listHeight - 28) {
                continue;
            }

            boolean isSelected = selectedPlayer == player;
            boolean isHovered = mouseX >= x + 3 && mouseX <= x + width - 3 &&
                    mouseY >= playerY && mouseY <= playerY + itemHeight - 2 &&
                    mouseY >= listY + 3 && mouseY <= listY + listHeight - 3;

            if (isSelected || isHovered) {
                int itemAlpha = isSelected ? 100 : 60;
                Vector4f itemRadius = new Vector4f(3, 3, 3, 3);
                RenderUtility.drawRoundedRect(x + 3, playerY, width - 6, itemHeight - 2, itemRadius,
                        new Vector4i(ColorUtils.rgba(0, 0, 0, itemAlpha), ColorUtils.rgba(0, 0, 0, itemAlpha),
                                ColorUtils.rgba(0, 0, 0, itemAlpha), ColorUtils.rgba(0, 0, 0, itemAlpha)));
            }

            float headSize = 25;
            float headX = x + 3 + (20 - headSize) / 2;
            float headY = playerY + (itemHeight - headSize) - 5;
            drawPlayerHead(matrixStack, player.skin, headX, headY, headSize);

            String playerName = player.name.length() > 12 ? player.name.substring(0, 9) + "..." : player.name;
            drawString(matrixStack, this.font, playerName, (int)(x + 30), (int)(playerY + 2), 0xFFFFFFFF);

            float healthPercent = player.health / player.maxHealth;
            int healthColor = ColorUtils.interpolateColor(0xFFFF0000, 0xFF00FF00, healthPercent);
            long timeAgo = (System.currentTimeMillis() - player.foundTime) / 1000;
            String timeAgoStr = timeAgo < 60 ? timeAgo + "с" : (timeAgo / 60) + "м";

            String healthText = String.format("%.0f HP", player.health);
            drawString(matrixStack, this.font, healthText, (int)(x + 30), (int)(playerY + 12), healthColor);

            drawString(matrixStack, this.font, timeAgoStr, (int)(x + width - 20), (int)(playerY + 2), 0xFF888888);
        }

        int visibleItems = (int)(listHeight / itemHeight);
        if (foundPlayersList.size() > visibleItems) {
            drawMiniScrollbar(matrixStack, x + width + 1, listY, 4, listHeight,
                    foundPlayersList.size(), visibleItems, playersScrollOffset, itemHeight);
        }
    }

    private void drawPlayerInfoPanel(MatrixStack matrixStack, float x, float y, int mouseX, int mouseY) {
        float panelWidth = 200;
        float panelHeight = 280;

        Vector4f panelRadius = new Vector4f(5, 5, 5, 5);
        RenderUtility.drawRoundedRect(x, y, panelWidth, panelHeight, panelRadius,
                new Vector4i(ColorUtils.rgba(0, 0, 0, 150), ColorUtils.rgba(0, 0, 0, 150),
                        ColorUtils.rgba(0, 0, 0, 150), ColorUtils.rgba(0, 0, 0, 150)));

        float currentY = y + 10;

        float headSize = 32;
        float headX = x + (panelWidth - headSize) / 2;
        drawPlayerHead(matrixStack, infoPlayer.skin, headX, currentY, headSize);
        currentY += headSize + 8;

        drawCenteredString(matrixStack, this.font, infoPlayer.name, (int)(x + panelWidth / 2), (int)currentY, 0xFFFFFFFF);
        currentY += 15;

        drawString(matrixStack, this.font, "Координаты: " + infoPlayer.x + " " + infoPlayer.y + " " + infoPlayer.z, (int)(x + 10), (int)currentY, 0xFF00FFFF);
        currentY += 15;

        drawString(matrixStack, this.font, "Броня:", (int)(x + 10), (int)currentY, 0xFFCCCCCC);
        currentY += 12;

        String[] armorSlots = {"Шлем", "Нагрудник", "Поножи", "Ботинки"};
        for (int i = 0; i < 4; i++) {
            ItemStack armorPiece = i < infoPlayer.armor.size() ? infoPlayer.armor.get(3 - i) : ItemStack.EMPTY;

            float itemX = x + 15;
            float itemY = currentY;

            if (!armorPiece.isEmpty()) {
                this.itemRenderer.renderItemAndEffectIntoGUI(armorPiece, (int)itemX, (int)itemY);

                if (mouseX >= itemX && mouseX <= itemX + 16 && mouseY >= itemY && mouseY <= itemY + 16) {
                    this.renderTooltip(matrixStack, armorPiece, (int)mouseX, (int)mouseY);
                }
            } else {
                Vector4f emptyRadius = new Vector4f(2, 2, 2, 2);
                RenderUtility.drawRoundedRect(itemX, itemY, 16, 16, emptyRadius,
                        new Vector4i(ColorUtils.rgba(50, 50, 50, 100), ColorUtils.rgba(50, 50, 50, 100),
                                ColorUtils.rgba(50, 50, 50, 100), ColorUtils.rgba(50, 50, 50, 100)));
            }

            drawString(matrixStack, this.font, armorSlots[i], (int)(itemX + 20), (int)(itemY + 4), 0xFFFFFFFF);
            currentY += 20;
        }

        currentY += 5;
        drawString(matrixStack, this.font, "В руках:", (int)(x + 10), (int)currentY, 0xFFCCCCCC);
        currentY += 12;

        float mainHandX = x + 15;
        float mainHandY = currentY;
        if (!infoPlayer.mainHand.isEmpty()) {
            this.itemRenderer.renderItemAndEffectIntoGUI(infoPlayer.mainHand, (int)mainHandX, (int)mainHandY);

            if (mouseX >= mainHandX && mouseX <= mainHandX + 16 && mouseY >= mainHandY && mouseY <= mainHandY + 16) {
                this.renderTooltip(matrixStack, infoPlayer.mainHand, (int)mouseX, (int)mouseY);
            }
        } else {
            Vector4f emptyRadius = new Vector4f(2, 2, 2, 2);
            RenderUtility.drawRoundedRect(mainHandX, mainHandY, 16, 16, emptyRadius,
                    new Vector4i(ColorUtils.rgba(50, 50, 50, 100), ColorUtils.rgba(50, 50, 50, 100),
                            ColorUtils.rgba(50, 50, 50, 100), ColorUtils.rgba(50, 50, 50, 100)));
        }

        drawString(matrixStack, this.font, "Основная", (int)(mainHandX + 20), (int)(mainHandY + 4), 0xFFFFFFFF);
        currentY += 20;

        float offHandX = x + 15;
        float offHandY = currentY;
        if (!infoPlayer.offHand.isEmpty()) {
            this.itemRenderer.renderItemAndEffectIntoGUI(infoPlayer.offHand, (int)offHandX, (int)offHandY);

            if (mouseX >= offHandX && mouseX <= offHandX + 16 && mouseY >= offHandY && mouseY <= offHandY + 16) {
                this.renderTooltip(matrixStack, infoPlayer.offHand, (int)mouseX, (int)mouseY);
            }
        } else {
            Vector4f emptyRadius = new Vector4f(2, 2, 2, 2);
            RenderUtility.drawRoundedRect(offHandX, offHandY, 16, 16, emptyRadius,
                    new Vector4i(ColorUtils.rgba(50, 50, 50, 100), ColorUtils.rgba(50, 50, 50, 100),
                            ColorUtils.rgba(50, 50, 50, 100), ColorUtils.rgba(50, 50, 50, 100)));
        }

        drawString(matrixStack, this.font, "Левая", (int)(offHandX + 20), (int)(offHandY + 4), 0xFFFFFFFF);
    }

    private void drawMiniScrollbar(MatrixStack matrixStack, float x, float y, float width, float height,
                                   int totalItems, int visibleItems, float scrollOffset, float itemHeight) {
        Vector4f scrollBgRadius = new Vector4f(2, 2, 2, 2);
        RenderUtility.drawRoundedRect(x, y, width, height, scrollBgRadius,
                new Vector4i(ColorUtils.rgba(0, 0, 0, 80), ColorUtils.rgba(0, 0, 0, 80),
                        ColorUtils.rgba(0, 0, 0, 80), ColorUtils.rgba(0, 0, 0, 80)));

        float scrollbarHeight = (float)visibleItems / totalItems * height;
        float scrollbarY = y + Math.abs(scrollOffset) / itemHeight / totalItems * height;

        Vector4f scrollRadius = new Vector4f(1, 1, 1, 1);
        RenderUtility.drawRoundedRect(x + 0.5f, scrollbarY, width - 1, scrollbarHeight, scrollRadius,
                new Vector4i(ColorUtils.rgba(255, 255, 255, 120), ColorUtils.rgba(255, 255, 255, 120),
                        ColorUtils.rgba(255, 255, 255, 120), ColorUtils.rgba(255, 255, 255, 120)));
    }

    private void drawPlayerHead(MatrixStack matrixStack, ResourceLocation skin, float x, float y, float size) {
        if (skin != null) {
            Vector4f headRadius = new Vector4f(2, 2, 2, 2);

            Stencil.initStencilToWrite();
            RenderUtility.drawRoundedRect(x, y, size, size, headRadius,
                    new Vector4i(0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF));
            Stencil.readStencilBuffer(1);
            RenderUtility.drawHead(skin, x + 1, y + 1, size - 2, size - 2, 1, 1.0f, 0.0f);
            Stencil.uninitStencilBuffer();
        } else {
            Vector4f unknownRadius = new Vector4f(2, 2, 2, 2);
            RenderUtility.drawRoundedRect(x, y, size, size, unknownRadius,
                    new Vector4i(ColorUtils.rgba(0, 0, 0, 100), ColorUtils.rgba(0, 0, 0, 100),
                            ColorUtils.rgba(0, 0, 0, 100), ColorUtils.rgba(0, 0, 0, 100)));

            drawCenteredString(matrixStack, this.font, "?", (int)(x + size / 2), (int)(y + size / 2 - 4), 0xFFFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float rectWidth = 220;
        float rectHeight = 300;
        float rectX = (this.width - rectWidth) / 2;
        float rectY = (this.height - rectHeight) / 2;
        float padding = 12;

        float pauseButtonY = rectY + 28 + 65;
        if (mouseX >= rectX + padding && mouseX <= rectX + rectWidth - padding &&
                mouseY >= pauseButtonY && mouseY <= pauseButtonY + 22) {
            if (baseFinder.isPaused()) {
                baseFinder.resumeSearch();
            } else {
                baseFinder.pauseSearch();
            }
            return true;
        }

        float listStartY = pauseButtonY + 30 + 12;
        float listHeight = rectHeight - (listStartY - rectY) - padding;
        float itemHeight = 28;

        for (int i = 0; i < foundPlayersList.size(); i++) {
            FoundPlayer player = foundPlayersList.get(i);
            float playerY = listStartY + 3 + playersScrollOffset + (i * itemHeight);

            if (playerY + itemHeight < listStartY || playerY > listStartY + listHeight) {
                continue;
            }

            if (mouseX >= rectX + padding + 3 && mouseX <= rectX + rectWidth - padding - 3 &&
                    mouseY >= playerY && mouseY <= playerY + itemHeight - 2) {

                if (button == 0) {
                    if (infoPlayer == player && showPlayerInfo) {
                        showPlayerInfo = false;
                        infoPlayer = null;
                    } else {
                        infoPlayer = player;
                        showPlayerInfo = true;
                    }
                    return true;
                }

                return true;
            }
        }

        if (showPlayerInfo) {
            float panelX = rectX + rectWidth + 10;
            float panelY = rectY;
            float panelWidth = 200;
            float panelHeight = 280;

            if (!(mouseX >= panelX && mouseX <= panelX + panelWidth &&
                    mouseY >= panelY && mouseY <= panelY + panelHeight)) {
                showPlayerInfo = false;
                infoPlayer = null;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        float rectWidth = 220;
        float rectHeight = 300;
        float rectX = (this.width - rectWidth) / 2;
        float rectY = (this.height - rectHeight) / 2;
        float padding = 12;

        float listStartY = rectY + 28 + 65 + 30 + 12;
        float listHeight = rectHeight - (listStartY - rectY) - padding;

        if (mouseX >= rectX + padding && mouseX <= rectX + rectWidth - padding &&
                mouseY >= listStartY && mouseY <= listStartY + listHeight) {

            float itemHeight = 28;
            int visibleItems = (int)(listHeight / itemHeight);

            if (foundPlayersList.size() > visibleItems) {
                playersScrollOffset += delta * 2 * itemHeight;

                float maxScroll = 0;
                float minScroll = -(foundPlayersList.size() - visibleItems) * itemHeight;
                playersScrollOffset = Math.max(minScroll, Math.min(maxScroll, playersScrollOffset));
            }
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static class FoundPlayer {
        public final String name;
        public final float health;
        public final float maxHealth;
        public final long foundTime;
        public final int x, y, z;
        public final ResourceLocation skin;
        public final List<ItemStack> armor;
        public final ItemStack mainHand;
        public final ItemStack offHand;

        public FoundPlayer(String name, float health, float maxHealth, long foundTime,
                           int x, int y, int z, ResourceLocation skin,
                           List<ItemStack> armor, ItemStack mainHand, ItemStack offHand) {
            this.name = name;
            this.health = health;
            this.maxHealth = maxHealth;
            this.foundTime = foundTime;
            this.x = x;
            this.y = y;
            this.z = z;
            this.skin = skin;
            this.armor = armor;
            this.mainHand = mainHand;
            this.offHand = offHand;
        }
    }
}