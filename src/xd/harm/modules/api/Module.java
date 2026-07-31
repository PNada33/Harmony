package xd.harm.modules.api;

import xd.harm.modules.impl.combat.HitAura;
import xd.harm.modules.impl.misc.ClientTune;
import xd.harm.modules.impl.misc.Notifications;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import net.minecraft.util.text.TextFormatting;
import ru.hogoshi.Animation;
import ru.hogoshi.util.Easings;

import java.util.List;

import xd.harm.Harmony;
import xd.harm.events.Event;
import xd.harm.modules.settings.Setting;
import xd.harm.utils.client.ClientUtil;
import xd.harm.utils.client.IMinecraft;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
public abstract class Module implements IMinecraft, Comparable<Module> {

    final String name;
    final String desc;
    final Category category;
    boolean state;
    @Setter
    int bind;
    final List<Setting<?>> settings = new ObjectArrayList<>();

    final Animation animation = new Animation();

    public Module() {
        this.name = getClass().getAnnotation(ModuleRegister.class).name();
        this.desc = getClass().getAnnotation(ModuleRegister.class).desc();
        this.category = getClass().getAnnotation(ModuleRegister.class).category();
        this.bind = getClass().getAnnotation(ModuleRegister.class).key();
    }

    public boolean isState() {
        return state;
    }

    public Module(String name, String desc, Category category) {
        this.name = name;
        this.category = category;
        this.desc = desc;
    }

    public void addSettings(Setting<?>... settings) {
        this.settings.addAll(List.of(settings));
    }

    public void onEvent(Event event) {
    }

    public boolean isVisibleInClickGui() {
        return true;
    }

    public boolean isVisibleInKeyBindHud() {
        return true;
    }

    public boolean isVisibleInArrayList() {
        return true;
    }

    public List<Setting<?>> getAllValues() {
        java.util.ArrayList<Setting<?>> allValues = new java.util.ArrayList<>();

        settings.forEach(value -> {
            allValues.add((Setting<?>) settings);
        });

        return allValues;
    }

    @Override
    public int compareTo(Module other) {
        return this.name.compareToIgnoreCase(other.name);
    }

    public boolean onEnable() {
        animation.animate(1, 0f, Easings.CIRC_OUT);
        Harmony.getInstance().getEventBus().register(this);
        return false;
    }

    ModuleManager moduleManager = Harmony.getInstance().getModuleManager();
    HitAura hitAura = moduleManager.getHitAura();

    public boolean onDisable() {
        animation.animate(0, 0f, Easings.CIRC_OUT);
        Harmony.getInstance().getEventBus().unregister(this);
        return false;
    }

    public void toggle() {
        setState(!state, false);
    }

    public final void setState(boolean newState, boolean config) {
        if (state == newState) {
            return;
        }

        state = newState;

        try {
            if (state) {
                onEnable();
                if (shouldShowModuleToggleNotification() && Notify.NOTIFICATION_MANAGER != null) {
                    Notify.NOTIFICATION_MANAGER.add(this.name + " включен.", "", 2);
                }
            } else {
                onDisable();
                if (shouldShowModuleToggleNotification() && Notify.NOTIFICATION_MANAGER != null) {
                    Notify.NOTIFICATION_MANAGER.add(this.name + " выключен.", "", 2);
                }
            }

            if (!config) {
                ModuleManager moduleManager = Harmony.getInstance().getModuleManager();
                ClientTune clientTune = moduleManager.getClientTune();
                if (clientTune != null && clientTune.isState()) {
                    String fileName = clientTune.getFileName(state);
                    float volume = clientTune.volume.get();
                    ClientUtil.playSound(fileName, volume, false);
                }
            }
        } catch (Exception e) {
            handleException(state ? "onEnable" : "onDisable", e);
        }
    }

    private void handleException(String methodName, Exception e) {
        if (mc.player != null) {
            print("[" + name + "] Произошла ошибка в методе " + TextFormatting.RED + methodName + TextFormatting.WHITE
                    + "() Предоставьте это сообщение разработчику: " + TextFormatting.GRAY + e.getMessage());
            e.printStackTrace();
        } else {
            System.out.println("[" + name + " Error" + methodName + "() Message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean shouldShowModuleToggleNotification() {
        try {
            if (moduleManager == null) {
                return true;
            }
            Notifications Notifications = moduleManager.getNotifications();
            if (Notifications == null) {
                return true;
            }
            if (!Notifications.isState()) {
                return false;
            }
            return Notifications.isModuleToggleNotifyEnabled();
        } catch (Exception ignored) {
            return true;
        }
    }
}
