package xd.harm.modules.impl.player;

import com.google.common.eventbus.Subscribe;
import net.minecraft.network.play.server.SChatPacket;
import xd.harm.events.input.EventKey;
import xd.harm.events.network.EventPacket;
import xd.harm.events.render.EventRender;
import xd.harm.events.world.EventUpdate;
import xd.harm.Harmony;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.impl.player.autobuy.AutoBuyManager;
import xd.harm.modules.impl.player.autobuy.AutoBuySystem;
import xd.harm.modules.settings.impl.BindSetting;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.SliderSetting;
import xd.harm.ui.clickgui.MenuPanel;

@ModuleRegister(name = "AutoBuy", category = Category.Player, desc = "\u0410\u0432\u0442\u043e\u043c\u0430\u0442\u0438\u0447\u0435\u0441\u043a\u0430\u044f \u043f\u043e\u043a\u0443\u043f\u043a\u0430 \u043d\u0430 \u0430\u0443\u043a\u0446\u0438\u043e\u043d\u0435")
public class AutoBuy extends Module {
    private final AutoBuyManager manager = new AutoBuyManager();
    private final AutoBuySystem system = new AutoBuySystem(this, manager);
    private boolean pendingParserStart = false;

    public AutoBuyManager getManager() { return manager; }
    public AutoBuySystem getSystem() { return system; }
    public boolean isPendingParserStart() { return pendingParserStart; }
    public BindSetting getOpenGuiBind() { return openGuiBind; }
    public BooleanSetting getParser() { return parser; }
    public SliderSetting getParserDiscount() { return parserDiscount; }
    public SliderSetting getUpdateDelay() { return updateDelay; }
    public SliderSetting getBuyDelay() { return buyDelay; }
    public BooleanSetting getHighlightCheapest() { return highlightCheapest; }
    public BooleanSetting getAutoSell() { return autoSell; }
    public SliderSetting getAutoSellPercent() { return autoSellPercent; }

    private final BindSetting openGuiBind = new BindSetting("\u041e\u0442\u043a\u0440\u044b\u0442\u044c GUI", 344);
    private final BooleanSetting parser = new BooleanSetting("\u0410\u0432\u0442\u043e\u043f\u0430\u0440\u0441\u0435\u0440", false);
    private final SliderSetting parserDiscount = new SliderSetting("\u0423\u043c\u0435\u043d\u044c\u0448\u0430\u0442\u044c \u0446\u0435\u043d\u044b \u043d\u0430", 20.0f, 1.0f, 99.0f, 0.5f)
            .setVisible(parser::get);

    private final SliderSetting updateDelay = new SliderSetting("\u0417\u0430\u0434\u0435\u0440\u0436\u043a\u0430 \u043e\u0431\u043d\u043e\u0432\u043b\u0435\u043d\u0438\u044f (\u043c\u0441)", 0, 0, 1000, 50f);
    private final SliderSetting buyDelay = new SliderSetting("\u0417\u0430\u0434\u0435\u0440\u0436\u043a\u0430 \u043f\u043e\u043a\u0443\u043f\u043a\u0438 (\u043c\u0441)", 700, 0, 1000, 50f);
    private final BooleanSetting highlightCheapest = new BooleanSetting("\u041f\u043e\u0434\u0441\u0432\u0435\u0442\u043a\u0430 \u0434\u0435\u0448\u0435\u0432\u043e\u0433\u043e", false);
    private final BooleanSetting autoSell = new BooleanSetting("\u0410\u0432\u0442\u043e-\u043f\u0440\u043e\u0434\u0430\u0436\u0430", false);
    private final SliderSetting autoSellPercent = new SliderSetting("\u0423\u0432\u0435\u043b\u0438\u0447\u0438\u0442\u044c \u0446\u0435\u043d\u0443 \u043d\u0430 %", 20, 1, 100, 1f)
            .setVisible(autoSell::get);

    public AutoBuy() {
        addSettings(openGuiBind, parser, parserDiscount, updateDelay, buyDelay, highlightCheapest, autoSell, autoSellPercent);
    }

    @Override
    public boolean isVisibleInClickGui() {
        return false;
    }

    @Override
    public boolean isVisibleInKeyBindHud() {
        return false;
    }

    @Override
    public boolean isVisibleInArrayList() {
        return false;
    }

    public void openGui() {
        if (mc.player == null) {
            return;
        }
        MenuPanel panel;
        if (mc.currentScreen instanceof MenuPanel) {
            panel = (MenuPanel) mc.currentScreen;
        } else if (Harmony.getInstance() != null && Harmony.getInstance().getMenuPanel() != null) {
            panel = Harmony.getInstance().getMenuPanel();
        } else {
            panel = new MenuPanel();
        }
        panel.openAutoBuyTab();
        mc.displayGuiScreen(panel);
    }

    public void requestParserStart() {
        pendingParserStart = true;
    }

    @Override
    public boolean onEnable() {
        system.enable();
        system.resetState();
        return super.onEnable();
    }

    @Override
    public boolean onDisable() {
        system.disable();
        system.resetState();
        system.stopPriceParsing();
        parser.set(false);
        return super.onDisable();
    }

    @Subscribe
    public void onKey(EventKey eventKey) {
        if (eventKey.getKey() == openGuiBind.get()) {
            openGui();
        }
    }

    @Subscribe
    public void onUpdate(EventUpdate eventUpdate) {
        if (!isState() || mc.player == null || mc.world == null) {
            return;
        }

        system.onUpdate();
        system.processBuy();
        if (pendingParserStart) {
            pendingParserStart = false;
            system.startPriceParsing();
        }
        system.up();

        if (highlightCheapest.get()) {
            if (!system.cheapestHighlighter.isEnabled()) {
                system.cheapestHighlighter.enable();
            }
            system.cheapestHighlighter.update();
        } else {
            if (system.cheapestHighlighter.isEnabled()) {
                system.cheapestHighlighter.disable();
            }
        }
    }

    @Subscribe
    public void onPacket(EventPacket eventPacket) {
        if (!isState() || !eventPacket.isReceive()) {
            return;
        }

        if (eventPacket.getPacket() instanceof SChatPacket chatPacket) {
            boolean cancel = system.onChatMessage(chatPacket.getChatComponent().getString());
            if (cancel) {
                eventPacket.setCancel(true);
            }
        }
    }

    @Subscribe
    public void onRender(EventRender eventRender) {
        if (!isState()) {
            return;
        }
        if (eventRender.isRender2D() && highlightCheapest.get()) {
            system.cheapestHighlighter.render(eventRender.matrixStack);
        }
    }
}
