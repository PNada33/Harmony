package net.minecraft.client.gui.screen;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.advancements.AdvancementsScreen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.realms.RealmsBridgeScreen;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IngameMenuScreen extends Screen {
    private final boolean isFullMenu;
    private static final Pattern ANARCHY_PATTERN = Pattern.compile("[\\u0410\\u0430]\\u043d\\u0430\\u0440\\u0445\\u0438\\u044f[\\s\\-]*?(\\d+)");

    public IngameMenuScreen(boolean isFullMenu) {
        super(isFullMenu ? new TranslationTextComponent("menu.game") : new TranslationTextComponent("menu.paused"));
        this.isFullMenu = isFullMenu;
    }

    @Override
    protected void init() {
        if (this.isFullMenu) {
            this.addButtons();
        }
    }

    private String getAnarchyNumber() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.ingameGUI == null || mc.ingameGUI.getTabList() == null || mc.ingameGUI.getTabList().header == null) {
            return "";
        }

        String serverHeader = TextFormatting.getTextWithoutFormattingCodes(mc.ingameGUI.getTabList().header.getString());
        if (serverHeader == null) {
            return "";
        }

        Matcher matcher = ANARCHY_PATTERN.matcher(serverHeader);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private void addButtons() {
        int offset = 24;

        this.addButton(new Button(this.width / 2 - 102, this.height / 4 + 8 + offset, 204, 20, new TranslationTextComponent("menu.returnToGame"), (button2) ->
        {
            this.minecraft.displayGuiScreen((Screen) null);
            this.minecraft.mouseHelper.grabMouse();
        }));
        this.addButton(new Button(this.width / 2 - 102, this.height / 4 + 32 + offset, 98, 20, new TranslationTextComponent("gui.advancements"), (button2) ->
        {
            this.minecraft.displayGuiScreen(new AdvancementsScreen(this.minecraft.player.connection.getAdvancementManager()));
        }));
        this.addButton(new Button(this.width / 2 + 4, this.height / 4 + 32 + offset, 98, 20, new TranslationTextComponent("gui.stats"), (button2) ->
        {
            this.minecraft.displayGuiScreen(new StatsScreen(this, this.minecraft.player.getStats()));
        }));
        this.addButton(new Button(this.width / 2 - 102, this.height / 4 + 56 + offset, 98, 20, new TranslationTextComponent("menu.options"), (button2) ->
        {
            this.minecraft.displayGuiScreen(new OptionsScreen(this, this.minecraft.gameSettings));
        }));
        Button button = this.addButton(new Button(this.width / 2 + 4, this.height / 4 + 56 + offset, 98, 20, new TranslationTextComponent("menu.shareToLan"), (button2) ->
        {
            this.minecraft.displayGuiScreen(new ShareToLanScreen(this));
        }));
        button.active = this.minecraft.isSingleplayer() && !this.minecraft.getIntegratedServer().getPublic();
        Button button1 = this.addButton(new Button(this.width / 2 - 102, this.height / 4 + 80 + offset, 204, 20, new TranslationTextComponent("menu.returnToMenu"), (button2) ->
        {
            disconnect(button2);
        }));

        if (!this.minecraft.isSingleplayer()) {
            this.addButton(new Button(this.width / 2 - 102, this.height / 4 + 104 + offset, 204, 20, new TranslationTextComponent("\u041f\u0435\u0440\u0435\u043f\u043e\u0434\u043a\u043b\u044e\u0447\u0438\u0442\u044c\u0441\u044f"), (button2) ->
            {
                disconnect(button2);
                this.minecraft.displayGuiScreen(new ConnectingScreen(new xd.harm.ui.mainmenu.MainScreen(), this.minecraft, ConnectingScreen.IP, ConnectingScreen.PORT));
            }));
        }

        if (!this.minecraft.isIntegratedServerRunning()) {
            button1.setMessage(new TranslationTextComponent("menu.disconnect"));
        }
    }

    private void disconnect(Button button) {
        boolean flag = this.minecraft.isIntegratedServerRunning();
        boolean flag1 = this.minecraft.isConnectedToRealms();
        button.active = false;
        this.minecraft.world.sendQuittingDisconnectingPacket();

        if (flag) {
            this.minecraft.unloadWorld(new DirtMessageScreen(new TranslationTextComponent("menu.savingLevel")));
        } else {
            this.minecraft.unloadWorld();
        }

        if (flag) {
            this.minecraft.displayGuiScreen(new xd.harm.ui.mainmenu.MainScreen());
        } else if (flag1) {
            RealmsBridgeScreen realmsbridgescreen = new RealmsBridgeScreen();
            realmsbridgescreen.func_231394_a_(new xd.harm.ui.mainmenu.MainScreen());
        } else {
            this.minecraft.displayGuiScreen(new MultiplayerScreen(new xd.harm.ui.mainmenu.MainScreen()));
        }
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        if (this.isFullMenu) {
            this.renderBackground(matrixStack);
            drawCenteredString(matrixStack, this.font, this.title, this.width / 2, 40, 16777215);
        } else {
            drawCenteredString(matrixStack, this.font, this.title, this.width / 2, 10, 16777215);
        }

        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }
}
