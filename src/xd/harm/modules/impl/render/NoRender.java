package xd.harm.modules.impl.render;

import com.google.common.eventbus.Subscribe;

import xd.harm.events.render.EventCancelOverlay;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.ModeListSetting;
import net.minecraft.potion.Effects;

import java.util.Map;
import java.util.TreeMap;

@ModuleRegister(name = "NoRender", category = Category.Render, desc = "Скрывает элементы на экране")
public class NoRender extends Module {

    private final BooleanSetting fireOverlay = new BooleanSetting("Огонь на экране", true);
    private final BooleanSetting bossLine = new BooleanSetting("Линия босса", false);
    private final BooleanSetting totemAnimation = new BooleanSetting("Анимация тотема", true);
    private final BooleanSetting titles = new BooleanSetting("Тайтлы", false);
    private final BooleanSetting scoreboard = new BooleanSetting("Таблица", false);
    private final BooleanSetting fog = new BooleanSetting("Туман", true);
    private final BooleanSetting hurtCamera = new BooleanSetting("Тряску камеры", true);
    private final BooleanSetting badEffects = new BooleanSetting("Плохие эффекты", true);
    private final BooleanSetting cameraClip = new BooleanSetting("Камера клип", true);
    private final BooleanSetting armor = new BooleanSetting("Броня", false);
    private final BooleanSetting cape = new BooleanSetting("Плащ", false);
    private final BooleanSetting grass = new BooleanSetting("Трава", true);
    private final BooleanSetting glowingEffect = new BooleanSetting("Эффект свечения", true);
    private final BooleanSetting waterEffect = new BooleanSetting("Эффект воды", true);
    private final BooleanSetting players = new BooleanSetting("Игроки", false);

    public ModeListSetting element = new CachedModeListSetting("Удалять",
            fireOverlay,
            bossLine,
            totemAnimation,
            titles,
            scoreboard,
            fog,
            hurtCamera,
            badEffects,
            cameraClip,
            armor,
            cape,
            grass,
            glowingEffect,
            waterEffect,
            players
    );

    public NoRender() {
        addSettings(element);
    }

    @Subscribe
    private void onUpdate(EventUpdate e) {
        handleEventUpdate(e);
    }

    @Subscribe
    private void onEventCancelOverlay(EventCancelOverlay e) {
        handleEventOverlaysRender(e);
    }

    private void handleEventOverlaysRender(EventCancelOverlay event) {
        boolean cancelOverlay = switch (event.overlayType) {
            case FIRE_OVERLAY -> fireOverlay.get();
            case BOSS_LINE -> bossLine.get();
            case SCOREBOARD -> scoreboard.get();
            case TITLES -> titles.get();
            case TOTEM -> totemAnimation.get();
            case FOG -> fog.get();
            case HURT -> hurtCamera.get();
            case UNDER_WATER -> waterEffect.get();
            case CAMERA_CLIP -> cameraClip.get();
            case TRAVA -> grass.get();
            case ARMOR -> armor.get();
        };

        if (cancelOverlay) {
            event.cancel();
        }
    }

    private void handleEventUpdate(EventUpdate event) {
        if (badEffects.get() && (mc.player.isPotionActive(Effects.BLINDNESS)
                || mc.player.isPotionActive(Effects.NAUSEA))) {
            mc.player.removePotionEffect(Effects.NAUSEA);
            mc.player.removePotionEffect(Effects.BLINDNESS);
        }
    }

    private static final class CachedModeListSetting extends ModeListSetting {
        private final Map<String, BooleanSetting> settingsByName = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        private CachedModeListSetting(String name, BooleanSetting... settings) {
            super(name, settings);
            for (BooleanSetting setting : settings) {
                settingsByName.put(setting.getName(), setting);
            }
        }

        @Override
        public BooleanSetting getValueByName(String settingName) {
            if (settingName != null) {
                BooleanSetting setting = settingsByName.get(settingName);
                if (setting != null) {
                    return setting;
                }
            }

            return super.getValueByName(settingName);
        }
    }
}
