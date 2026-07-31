package xd.harm.modules.impl.player;

import com.google.common.eventbus.Subscribe;
import net.minecraft.network.play.server.SChatPacket;
import xd.harm.events.network.EventPacket;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.ModeSetting;
import xd.harm.modules.settings.impl.StringSetting;
import xd.harm.utils.math.TimerHelper;

import java.security.SecureRandom;
import java.util.Locale;

@ModuleRegister(name = "AutoRegister", category = Category.Player, desc = "Автоматически логинится и регистрируется на серверах")
public class AutoRegister extends Module {

    private final StringSetting password = new StringSetting("Password", "", "Пароль для /login и /register");
    private final BooleanSetting login = new BooleanSetting("Login", false);
    private final ModeSetting loginCommand = new ModeSetting("Login cmd", "/l", "/l", "/login")
            .setVisible(() -> login.get());
    private final ModeSetting registerCommand = new ModeSetting("Register cmd", "/reg", "/reg", "/register");
    private final BooleanSetting repeatRegister = new BooleanSetting("Repeat password", true);
    private final BooleanSetting randomize = new BooleanSetting("Randomize", false);

    private final TimerHelper timer = new TimerHelper();
    private final SecureRandom random = new SecureRandom();

    public AutoRegister() {
        addSettings(password, login, loginCommand, registerCommand, repeatRegister, randomize);
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (!randomize.get()) {
            return;
        }

        password.set(generatePassword());
        randomize.set(false);
        print("AutoRegister: сгенерирован новый пароль.");
    }

    @Subscribe
    public void onPacket(EventPacket event) {
        if (!event.isReceive() || mc.player == null || !timer.hasReached(1500)) {
            return;
        }

        if (!(event.getPacket() instanceof SChatPacket chatPacket)) {
            return;
        }

        String text = normalize(chatPacket.getChatComponent().getString());
        if (text.isEmpty()) {
            return;
        }

        if (isRegisterPrompt(text)) {
            sendRegister();
        } else if (login.get() && isLoginPrompt(text)) {
            sendLogin();
        }
    }

    private void sendRegister() {
        String pass = password.get().trim();
        if (pass.isEmpty()) {
            print("AutoRegister: пароль не указан.");
            timer.reset();
            return;
        }

        String command = registerCommand.get() + " " + pass;
        if (repeatRegister.get()) {
            command += " " + pass;
        }

        mc.player.sendChatMessage(command);
        timer.reset();
    }

    private void sendLogin() {
        String pass = password.get().trim();
        if (pass.isEmpty()) {
            print("AutoRegister: пароль не указан.");
            timer.reset();
            return;
        }

        mc.player.sendChatMessage(loginCommand.get() + " " + pass);
        timer.reset();
    }

    private boolean isRegisterPrompt(String text) {
        if (containsAny(text, "already registered", "уже зарегистр", "already logged")) {
            return false;
        }

        return containsAny(text,
                "/register", "/reg ", " register", "please register", "you need to register",
                "зарегистр", "регистрац", "придумайте пароль", "повторите пароль",
                "repeat password", "create password");
    }

    private boolean isLoginPrompt(String text) {
        if (isRegisterPrompt(text)) {
            return false;
        }

        return containsAny(text,
                "/login", "/l ", " login", "log in", "please login", "enter password",
                "авториз", "войдите", "войти", "залогин", "введите пароль");
    }

    private boolean containsAny(String text, String... words) {
        for (String word : words) {
            if (text.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String generatePassword() {
        String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        int length = 10 + random.nextInt(5);
        StringBuilder builder = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            builder.append(chars.charAt(random.nextInt(chars.length())));
        }

        return builder.toString();
    }
}
