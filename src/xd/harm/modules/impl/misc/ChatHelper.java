package xd.harm.modules.impl.misc;

import xd.harm.modules.settings.impl.*;
import com.google.common.eventbus.Subscribe;
import xd.harm.events.input.EventKey;
import xd.harm.events.network.EventPacket;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;

import xd.harm.utils.math.StopWatch;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.server.SChatPacket;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@ModuleRegister(name = "ChatHelper", category = Category.Misc, desc = "Р”РѕРїРѕР»РЅРµРЅРёРµ РґР»СЏ С‡Р°С‚Р°")
public class ChatHelper extends Module {

    public final BooleanSetting autoAuth = new BooleanSetting("РђРІС‚Рѕ Р»РѕРіРёРЅ", true);
    public final BooleanSetting autoRegister = new BooleanSetting("\u0410\u0432\u0442\u043e \u0440\u0435\u0433\u0438\u0441\u0442\u0440", true);
    public final StringSetting passwordField = new StringSetting("РџР°СЂРѕР»СЊ", "", "Р’РІРµРґРёС‚Рµ РїР°СЂРѕР»СЊ").setVisible(() -> autoAuth.get() || autoRegister.get());

    public final BooleanSetting autoText = new BooleanSetting("РђРІС‚Рѕ С‚РµРєСЃС‚", false);
    public final BindSetting textBind = new BindSetting("РљРЅРѕРїРєР°", -1).setVisible(() -> autoText.get());
    public final ModeSetting textMode = new ModeSetting("РўРµРєСЃС‚", "РљРѕСЂРґС‹", "РљРѕСЂРґС‹", "РљР°СЃС‚РѕРј").setVisible(() -> autoText.get());
    public final StringSetting customText = new StringSetting("РљР°СЃС‚РѕРјРЅС‹Р№ С‚РµРєСЃС‚", "Р¶РґРё СЃРІР°С‚", "РЈРєР°Р¶РёС‚Рµ С‚РµРєСЃС‚").setVisible(() -> autoText.get() && textMode.is("РљР°СЃС‚РѕРј"));
    public final BooleanSetting globalChat = new BooleanSetting("РџРёСЃР°С‚СЊ РІ РіР»РѕР±Р°Р»", true).setVisible(() -> autoText.get());

    public final BooleanSetting spammer = new BooleanSetting("CРїР°РјРјРµСЂ", false);
    public final SliderSetting spamDelay = new SliderSetting("Р—Р°РґРµСЂР¶РєР° РІ СЃРµРєСѓРЅРґР°С…", 5, 1, 30, 0.5f).setVisible(() -> spammer.get());
    public final StringSetting spammerText = new StringSetting("РљР°СЃС‚РѕРјРЅС‹Р№ С‚РµРєСЃС‚", "Р¶РґРё СЃРІР°С‚", "РЈРєР°Р¶РёС‚Рµ С‚РµРєСЃС‚").setVisible(() -> spammer.get());
    public List<String> lastMessages = new ArrayList<>();
    public String password;
    public StopWatch stopWatch = new StopWatch();

    public ChatHelper() {
        addSettings(autoAuth, autoRegister, passwordField, autoText, textBind, textMode, customText, globalChat, spammer, spamDelay, spammerText);
    }

    @Subscribe
    public void onUpdate(EventUpdate e) {
        if (!autoAuth.get() && !autoRegister.get() && !spammer.get()) {
            toggle();
        }

        password = passwordField.get();

        if (spammer.get()) {
            if (stopWatch.isReached(spamDelay.get().longValue() * 1000))
                mc.player.sendChatMessage(spammerText.get());
        }
    }

    @Subscribe
    public void onPacket(EventPacket e) {
        if (e.isReceive()) {
            if (mc.player == null) return;
            if (e.getPacket() instanceof SChatPacket wrapper) {
                if (lastMessages.size() > 10) lastMessages.remove(0);
                lastMessages.add(wrapper.getChatComponent().getString());

                List<String> registerWords = List.of(
                        "/reg", "/register", "reg ", "register ",
                        "\u0437\u0430\u0440\u0435\u0433\u0438\u0441\u0442\u0440\u0438\u0440\u0443", "\u0440\u0435\u0433\u0438\u0441\u0442\u0440\u0430\u0446\u0438",
                        "\u043f\u0440\u0438\u0434\u0443\u043c\u0430\u0439\u0442\u0435 \u043f\u0430\u0440\u043e\u043b\u044c", "\u043f\u043e\u0432\u0442\u043e\u0440\u0438\u0442\u0435 \u043f\u0430\u0440\u043e\u043b\u044c",
                        "please register", "repeat password",
                        "Р—Р°СЂРµРіРёСЃС‚СЂРёСЂСѓР№С‚РµСЃСЊ", "/reg РџР°СЂРѕР»СЊ РџРѕРІС‚РѕСЂРёС‚РµРџР°СЂРѕР»СЊ"
                );
                List<String> loginWords = List.of(
                        "/login", "/l ", "login ", "log in",
                        "\u0430\u0432\u0442\u043e\u0440\u0438\u0437", "\u0432\u043e\u0439\u0434\u0438\u0442\u0435", "\u0432\u043e\u0439\u0434\u0438",
                        "please login",
                        "РђРІС‚РѕСЂРёР·СѓР№С‚РµСЃСЊ", "/login РџР°СЂРѕР»СЊ", "/login РџР°СЂРѕР»СЊ РџР°СЂРѕР»СЊ",
                        "/login <РџР°СЂРѕР»СЊ>", "/login <password>", "/login password"
                );
                List<String> alreadyRegisteredWords = List.of(
                        "already registered", "\u0443\u0436\u0435 \u0437\u0430\u0440\u0435\u0433\u0438\u0441\u0442\u0440"
                );

                boolean alreadyRegistered = containsAny(lastMessages, alreadyRegisteredWords);
                boolean containsRegister = autoRegister.get() && !alreadyRegistered && containsAny(lastMessages, registerWords);
                boolean containsLogin = autoAuth.get() && containsAny(lastMessages, loginWords);

                String emptyField = "Р’С‹ РЅРµ СѓРєР°Р·Р°Р»Рё РїР°СЂРѕР»СЊ";
                String success = "Р’Р°С€ Р°РєРєР°СѓРЅС‚ Р±С‹Р» СѓСЃРїРµС€РЅРѕ Р·Р°СЂРµРіРёСЃС‚СЂРёСЂРѕРІР°РЅ";
                String successLogin = "РђРІС‚РѕСЂРёР·Р°С†РёСЏ СѓСЃРїРµС€РЅРѕ РїСЂРѕР№РґРµРЅР°";

                if (containsLogin || containsRegister) {
                    if (password == null || password.equals("")) {
                        assert mc.player != null;
                        if (containsRegister) {
                            if (!lastMessages.contains(emptyField) && !lastMessages.contains(success)) {
                                print(emptyField);
                            }
                        }
                        if (containsLogin) {
                            if (!lastMessages.contains(emptyField) && !lastMessages.contains(successLogin)) {
                                print(getName() + ": РЇ РЅРµ Р·РЅР°СЋ РІР°С€ РїР°СЂРѕР»СЊ!");
                            }
                        }
                        lastMessages.clear();
                    } else {
                        assert mc.player != null;
                        if (containsRegister) {
                            if (!lastMessages.contains(emptyField) && !lastMessages.contains(success)) {
                                mc.player.sendChatMessage("/register " + password + " " + password);
                            }
                        } else if (containsLogin) {
                            if (!lastMessages.contains(emptyField) && !lastMessages.contains(successLogin)) {
                                mc.player.sendChatMessage("/login " + password);
                            }
                        }
                        lastMessages.clear();
                        GLFW.glfwSetClipboardString(Minecraft.getInstance().getMainWindow().getHandle(), password);
                        print("РџР°СЂРѕР»СЊ СЃРєРѕРїРёСЂРѕРІР°РЅ РІ Р±СѓС„РµСЂ РѕР±РјРµРЅР°!");
                    }
                }
            }
        }
    }

    private boolean containsAny(List<String> messages, List<String> words) {
        for (String message : messages) {
            String normalizedMessage = normalize(message);
            for (String word : words) {
                if (normalizedMessage.contains(normalize(word))) {
                    return true;
                }
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    @Subscribe
    public void onKeyPress(EventKey e) {
        if (e.getKey() == textBind.get() && autoText.get()) {
            String text = globalChat.get() ? "!" : "";
            String pos = (int) (mc.player.getPosX()) + ", " + (int) (mc.player.getPosY()) + ", " + (int) (mc.player.getPosZ());
            if (textMode.is("РљРѕСЂРґС‹")) {
                mc.player.sendChatMessage(text + pos);
            }
            if (textMode.is("РљР°СЃС‚РѕРј")) {
                mc.player.sendChatMessage(text + customText.get());
            }
        }
    }
}
