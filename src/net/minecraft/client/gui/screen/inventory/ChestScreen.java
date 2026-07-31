package net.minecraft.client.gui.screen.inventory;

import xd.harm.Harmony;
import xd.harm.modules.api.ModuleManager;
import xd.harm.modules.impl.player.AutoBuy;
import xd.harm.utils.client.IMinecraft;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.client.gui.IHasContainer;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ChestContainer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import java.util.Locale;

public class ChestScreen extends ContainerScreen<ChestContainer> implements IHasContainer<ChestContainer>, IMinecraft {
    private static final ResourceLocation CHEST_GUI_TEXTURE = new ResourceLocation("textures/gui/container/generic_54.png");
    private final int inventoryRows;
    private final ITextComponent title;
    private boolean isProcessing = false;
    private int currentIndex = 0;
    private long lastActionTime = 0;
    private static final long ACTION_DELAY = 1;
    private String currentAction = "";
    private Button autoBuyOpenButton;
    private Button autoBuyToggleButton;
    private Button autoBuyParserButton;
    private Button autoBuyDiscountButton;

    public ChestScreen(ChestContainer container, PlayerInventory playerInventory, ITextComponent title) {
        super(container, playerInventory, title);
        this.title = title;
        this.passEvents = false;
        this.inventoryRows = container.getNumRows();
        this.ySize = 114 + this.inventoryRows * 18;
        this.playerInventoryTitleY = this.ySize - 94;
    }

    @Override
    protected void init() {
        super.init();

        this.addButton(new Button(width / 2 + 90, height / 2 - 84, 90, 20,
                new StringTextComponent("Сложить всё"), (button) -> {
            if (mc.player != null && mc.playerController != null) {
                startAction("drop");
            }
        }));
        this.addButton(new Button(width / 2 + 90, height / 2 - 62, 90, 20,
                new StringTextComponent("Забрать всё"), (button) -> {
            if (mc.player != null && mc.playerController != null) {
                startAction("take");
            }
        }));
        this.addButton(new Button(width / 2 + 90, height / 2 - 40, 90, 20,
                new StringTextComponent("Выбросить всё"), (button) -> {
            if (mc.player != null && mc.playerController != null) {
                startAction("throw");
            }
        }));

        if (isAuctionChest()) {
            int chestX = (this.width - this.xSize) / 2;
            int chestY = (this.height - this.ySize) / 2;
            int topWideY = chestY - 46;
            int topButtonsY = chestY - 24;
            int gap = 4;
            int halfWidth = (this.xSize - gap) / 2;

            this.autoBuyParserButton = this.addButton(new Button(chestX, topWideY, halfWidth, 20,
                    getAutoBuyParserText(), (button) -> {
                AutoBuy autoBuy = getAutoBuyModule();
                if (autoBuy != null) {
                    boolean isParsing = autoBuy.getSystem().isParsingPrices();
                    if (isParsing) {
                        autoBuy.getSystem().stopPriceParsing();
                        autoBuy.getParser().set(false);
                    } else {
                        if (!autoBuy.isState()) {
                            autoBuy.setState(true, false);
                        }
                        autoBuy.getParser().set(true);
                        autoBuy.getSystem().startPriceParsing();
                    }
                    updateAutoBuyButtons();
                }
            }));

            this.autoBuyDiscountButton = this.addButton(new Button(chestX + halfWidth + gap, topWideY, halfWidth, 20,
                    getAutoBuyDiscountText(), (button) -> {
                AutoBuy autoBuy = getAutoBuyModule();
                if (autoBuy != null) {
                    adjustParserDiscount(autoBuy, 0.5f);
                    updateAutoBuyButtons();
                }
            }));

            this.autoBuyOpenButton = this.addButton(new Button(chestX, topButtonsY, halfWidth, 20,
                    new StringTextComponent("AutoBuy GUI"), (button) -> {
                AutoBuy autoBuy = getAutoBuyModule();
                if (autoBuy != null) {
                    autoBuy.openGui();
                }
            }));

            this.autoBuyToggleButton = this.addButton(new Button(chestX + halfWidth + gap, topButtonsY, halfWidth, 20,
                    getAutoBuyToggleText(), (button) -> {
                AutoBuy autoBuy = getAutoBuyModule();
                if (autoBuy != null) {
                    autoBuy.setState(!autoBuy.isState(), false);
                    updateAutoBuyButtons();
                }
            }));

            updateAutoBuyButtons();
        } else {
            this.autoBuyOpenButton = null;
            this.autoBuyToggleButton = null;
            this.autoBuyParserButton = null;
            this.autoBuyDiscountButton = null;
        }
    }

    @Override
    public void tick() {
        super.tick();
        updateAutoBuyButtons();

        if (isProcessing && mc.currentScreen == this) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastActionTime >= ACTION_DELAY) {
                int chestSlots = this.inventoryRows * 9;
                int playerInventoryStart = chestSlots;

                if (currentAction.equals("drop")) {
                    while (currentIndex < 36) {
                        int slotIndex = playerInventoryStart + currentIndex;
                        if (slotIndex < this.container.getInventory().size() && !this.container.getSlot(slotIndex).getStack().isEmpty()) {
                            mc.playerController.windowClick(this.container.windowId, slotIndex, 0, ClickType.QUICK_MOVE, mc.player);
                            lastActionTime = currentTime;
                            currentIndex++;
                            return;
                        }
                        currentIndex++;
                    }
                } else {
                    int maxIndex = currentAction.equals("take") || currentAction.equals("throw") ? chestSlots : this.container.getInventory().size();
                    while (currentIndex < maxIndex) {
                        if (!this.container.getSlot(currentIndex).getStack().isEmpty()) {
                            if (currentAction.equals("take")) {
                                mc.playerController.windowClick(this.container.windowId, currentIndex, 0, ClickType.QUICK_MOVE, mc.player);
                            } else if (currentAction.equals("throw")) {
                                mc.playerController.windowClick(this.container.windowId, currentIndex, 1, ClickType.THROW, mc.player);
                            }
                            lastActionTime = currentTime;
                            currentIndex++;
                            return;
                        }
                        currentIndex++;
                    }
                }

                isProcessing = false;
                currentIndex = 0;
                currentAction = "";
            }
        }
    }

    private void startAction(String action) {
        isProcessing = true;
        currentIndex = 0;
        lastActionTime = System.currentTimeMillis();
        currentAction = action;
    }

    private AutoBuy getAutoBuyModule() {
        if (Harmony.getInstance() == null) {
            return null;
        }
        ModuleManager moduleManager = Harmony.getInstance().getModuleManager();
        if (moduleManager == null) {
            return null;
        }
        return moduleManager.getAutoBuy();
    }

    private boolean isAuctionChest() {
        if (title == null) {
            return false;
        }
        String currentTitle = title.getString();
        if (currentTitle == null || currentTitle.isEmpty()) {
            return false;
        }
        String lower = currentTitle.toLowerCase(Locale.ROOT);
        return lower.contains("auction")
                || lower.contains("search")
                || lower.contains("\u0430\u0443\u043a\u0446")
                || lower.contains("\u043f\u043e\u0438\u0441\u043a");
    }

    private ITextComponent getAutoBuyToggleText() {
        AutoBuy autoBuy = getAutoBuyModule();
        boolean enabled = autoBuy != null && autoBuy.isState();
        return new StringTextComponent(enabled ? "AutoBuy: ON" : "AutoBuy: OFF");
    }

    private ITextComponent getAutoBuyParserText() {
        AutoBuy autoBuy = getAutoBuyModule();
        boolean parsing = autoBuy != null && autoBuy.getSystem().isParsingPrices();
        return new StringTextComponent(parsing ? "\u041e\u0441\u0442\u0430\u043d\u043e\u0432\u0438\u0442\u044c \u043f\u0430\u0440\u0441\u0435\u0440" : "\u0421\u043f\u0430\u0440\u0441\u0438\u0442\u044c \u0446\u0435\u043d\u044b");
    }

    private ITextComponent getAutoBuyDiscountText() {
        AutoBuy autoBuy = getAutoBuyModule();
        float discount = autoBuy != null ? autoBuy.getParserDiscount().get() : 20.0f;
        return new StringTextComponent(String.format(Locale.ROOT, "\u0421\u043a\u0438\u0434\u043a\u0430: %.1f%%", discount));
    }

    private void adjustParserDiscount(AutoBuy autoBuy, float delta) {
        float current = autoBuy.getParserDiscount().get();
        float next = Math.round((current + delta) * 2.0f) / 2.0f;
        if (next < 1.0f) {
            next = 99.0f;
        } else if (next > 99.0f) {
            next = 1.0f;
        }
        autoBuy.getParserDiscount().set(next);
    }

    private void updateAutoBuyButtons() {
        if (!isAuctionChest()) {
            return;
        }

        AutoBuy autoBuy = getAutoBuyModule();
        boolean available = autoBuy != null;

        if (autoBuyOpenButton != null) {
            autoBuyOpenButton.active = available;
        }
        if (autoBuyToggleButton != null) {
            autoBuyToggleButton.active = available;
            autoBuyToggleButton.setMessage(getAutoBuyToggleText());
        }
        if (autoBuyParserButton != null) {
            autoBuyParserButton.active = available;
            autoBuyParserButton.setMessage(getAutoBuyParserText());
        }
        if (autoBuyDiscountButton != null) {
            autoBuyDiscountButton.active = available;
            autoBuyDiscountButton.setMessage(getAutoBuyDiscountText());
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1 && isAuctionChest() && autoBuyDiscountButton != null && autoBuyDiscountButton.isMouseOver(mouseX, mouseY)) {
            AutoBuy autoBuy = getAutoBuyModule();
            if (autoBuy != null) {
                adjustParserDiscount(autoBuy, -0.5f);
                updateAutoBuyButtons();
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void closeScreen() {
        super.closeScreen();
        isProcessing = false;
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(matrixStack);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        
        if (isAuctionChest()) {
            renderPurchaseLogs(matrixStack);
        }
        
        this.renderHoveredTooltip(matrixStack, mouseX, mouseY);
    }
    
    private void renderPurchaseLogs(MatrixStack stack) {
        AutoBuy autoBuy = getAutoBuyModule();
        if (autoBuy == null) return;
        
        java.util.List<xd.harm.modules.impl.player.autobuy.TransactionLog> logs = autoBuy.getManager().getTransactionLogs();
        if (logs.isEmpty()) return;
        
        int chestX = (this.width - this.xSize) / 2;
        int chestY = (this.height - this.ySize) / 2;
        
        float logX = chestX - 160;
        float logY = chestY;
        float logW = 150;
        float logH = 24;
        float gap = 4;
        
        int maxLogs = Math.min(5, logs.size());
        
        for (int i = 0; i < maxLogs; i++) {
            xd.harm.modules.impl.player.autobuy.TransactionLog log = logs.get(logs.size() - 1 - i);
            float y = logY + i * (logH + gap);
            
            boolean isBuy = log.type == xd.harm.modules.impl.player.autobuy.TransactionLog.Type.BUY;
            int bgColor = isBuy ? 0x80FF6B6B : 0x806BFF6B;
            int borderColor = isBuy ? 0xFFFF4444 : 0xFF44FF44;
            
            xd.harm.utils.render.rect.RenderUtility.drawRoundedRect(logX, y, logW, logH, 6, bgColor);
            xd.harm.utils.render.rect.RenderUtility.drawRoundedRectOutline(logX, y, logW, logH, 6, 1f, borderColor);
            
            if (log.stack != null && !log.stack.isEmpty()) {
                minecraft.getItemRenderer().renderItemIntoGUI(log.stack, (int)(logX + 4), (int)(y + 4));
            }
            
            String itemName = log.itemName;
            if (itemName.length() > 12) itemName = itemName.substring(0, 10) + "..";
            xd.harm.utils.render.font.Fonts.sfuy.drawText(stack, itemName, logX + 24, y + 4, -1, 7f);
            
            String priceText = String.format("%,d$", log.price);
            xd.harm.utils.render.font.Fonts.sfuy.drawText(stack, priceText, logX + 24, y + 14, 0xFFFFDD44, 6.5f);
        }
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(MatrixStack matrixStack, float partialTicks, int x, int y) {
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        this.minecraft.getTextureManager().bindTexture(CHEST_GUI_TEXTURE);
        int i = (this.width - this.xSize) / 2;
        int j = (this.height - this.ySize) / 2;
        this.blit(matrixStack, i, j, 0, 0, this.xSize, this.inventoryRows * 18 + 17);
        this.blit(matrixStack, i, j + this.inventoryRows * 18 + 17, 0, 126, this.xSize, 96);
    }
}
