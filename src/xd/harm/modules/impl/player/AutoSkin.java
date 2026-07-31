package xd.harm.modules.impl.player;

import com.google.common.eventbus.Subscribe;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.network.play.client.CChatMessagePacket;
import net.minecraft.util.ResourceLocation;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.ModeSetting;
import xd.harm.modules.settings.impl.StringSetting;
import xd.harm.utils.math.TimerHelper;

@ModuleRegister(name = "AutoSkin", category = Category.Player, desc = "Автоматически меняет скин")
public class AutoSkin extends Module {

    private final ModeSetting mode = new ModeSetting("Mode", "Visuals", "Visuals", "Default");
    private final StringSetting skinName = new StringSetting("Skin name", "", "Имя скина");
    private final BooleanSetting onlyDefault = new BooleanSetting("При обычном скине", true)
            .setVisible(() -> mode.is("Default"));
    private final BooleanSetting sendPacket = new BooleanSetting("Send packet", false)
            .setVisible(() -> mode.is("Visuals"));

    private final TimerHelper joinTimer = new TimerHelper();
    private boolean sentCommand = false;
    private ResourceLocation customSkinLocation = null;
    private String lastSkinName = "";

    public AutoSkin() {
        addSettings(mode, skinName, onlyDefault, sendPacket);
    }

    public boolean isVisuals() {
        return isState() && mode.is("Visuals");
    }

    public ResourceLocation getCustomSkinLocation() {
        return customSkinLocation;
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (mc.player == null) return;

        if (mode.is("Visuals")) {
            handleVisuals();
        } else if (mode.is("Default")) {
            handleDefault();
        }
    }

    private void handleVisuals() {
        String name = skinName.get().trim();
        if (name.isEmpty()) {
            customSkinLocation = null;
            return;
        }

        if (!name.equals(lastSkinName) || customSkinLocation == null) {
            loadCustomSkin(name);
            lastSkinName = name;
        }

        if (sendPacket.get()) {
            mc.player.connection.sendPacket(new CChatMessagePacket("/skin " + name));
            sendPacket.set(false);
        }
    }

    private void loadCustomSkin(String name) {
        ResourceLocation location = AbstractClientPlayerEntity.getLocationSkin(name);
        AbstractClientPlayerEntity.getDownloadImageSkin(location, name);
        customSkinLocation = location;
    }

    private void handleDefault() {
        if (!joinTimer.hasReached(5000)) return;
        if (sentCommand) return;

        String name = skinName.get().trim();
        if (name.isEmpty()) return;

        if (onlyDefault.get() && !isDefaultSkin()) return;

        mc.player.sendChatMessage("/skin " + name);
        sentCommand = true;
    }

    private boolean isDefaultSkin() {
        if (mc.player == null) return true;
        String path = mc.player.getLocationSkin().getPath();
        return path.equals("textures/entity/steve.png") || path.equals("textures/entity/alex.png");
    }

    @Override
    public boolean onEnable() {
        super.onEnable();
        joinTimer.reset();
        sentCommand = false;
        return false;
    }

    @Override
    public boolean onDisable() {
        super.onDisable();
        customSkinLocation = null;
        lastSkinName = "";
        return false;
    }
}
