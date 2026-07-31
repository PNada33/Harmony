package xd.harm.modules.impl.misc;

import com.google.common.eventbus.Subscribe;
import xd.harm.events.combat.AttackEvent;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.ModeSetting;
import xd.harm.modules.settings.impl.SliderSetting;
import xd.harm.utils.math.MathUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.io.BufferedInputStream;
import java.io.InputStream;

import static java.lang.Math.*;
import static java.lang.Math.signum;
import static net.minecraft.util.math.MathHelper.wrapDegrees;

@ModuleRegister(name = "ClientTune", category = Category.Misc, desc = "Воспроизводит звуковые эффекты")
public class ClientTune extends Module {

    public ModeSetting mode = new ModeSetting("Тип", "Обычный",  "Обычный","Поп","Прикольные","Лол","Хефи", "Виндовс", "Дроблет", "Пузырьки");
    public SliderSetting volume = new SliderSetting("Громкость", 60.0f, 0.0f, 100.0f, 1.0f);
    public BooleanSetting other = new BooleanSetting("Гуи", true);
    public BooleanSetting hitSound = new BooleanSetting("Звук удара", false);
    private final ModeSetting sound = new ModeSetting("Звук", "bell", "bell", "metallic", "bubble", "crime", "uwu", "moan", "perdej").setVisible(() -> hitSound.get());
    SliderSetting volumehitSound = new SliderSetting("Громкость HitSound", 35.0f, 5.0f, 100.0f, 5.0f).setVisible(() -> hitSound.get());

    public ClientTune() {
        addSettings(mode, volume, other, hitSound, sound, volumehitSound);
    }


    public String getFileName(boolean state) {
        switch (mode.get()) {
            case "ы" -> {
                return state ? "enable" : "disable";
            }
            case "Пузырьки" -> {
                return state ? "enableBubbles" : "disableBubbles";
            }
            case "Лол" -> {
                return state ? "enable2" : "disable2";
            }
            case "Поп" -> {
                return state ? "popenable" : "popdisable";
            }
            case "Хефи" -> {
                return state ? "heavyenable" : "heavydisable";
            }
            case "Виндовс" -> {
                return state ? "winenable" : "windisable";
            }
            case "Дроблет" -> {
                return state ? "dropletenable" : "dropletdisable";
            }
            case "Прикольные" -> {
                return state ? "enablevl" : "disablevl";
            }
            case "Обычный" -> {
                return state ? "slideenable" : "slidedisable";
            }
        }
        return "";
    }

    @Subscribe
    public void onPacket(AttackEvent e) {
        if (mc.player == null || mc.world == null) return;
        if (hitSound.get()) playSound(e.entity);
    }

    public void playSound(Entity e) {
        try {
            Clip clip = AudioSystem.getClip();
            String resourceSound = "";

            if (sound.is("moan")) {
                int i = MathUtil.randomInt(1, 4);
                resourceSound = "harmony/sounds/moan" + i + ".wav";
            } else {
                resourceSound = "harmony/sounds/" + sound.get() + ".wav";
            }

            InputStream is = mc.getResourceManager().getResource(new ResourceLocation(resourceSound)).getInputStream();
            BufferedInputStream bis = new BufferedInputStream(is);
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(bis);
            if (audioInputStream == null) {
                System.out.println("Sound not found!");
                return;
            }
            clip.open(audioInputStream);
            clip.start();

            FloatControl floatControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            if (e != null) {
                FloatControl balance = (FloatControl) clip.getControl(FloatControl.Type.BALANCE);
                Vector3d vec = e.getPositionVec().subtract(Minecraft.getInstance().player.getPositionVec());


                double yaw = wrapDegrees(toDegrees(atan2(vec.z, vec.x)) - 90);
                double delta = wrapDegrees(yaw - mc.player.rotationYaw);

                if (abs(delta) > 180) delta -= signum(delta) * 360;
                try {
                    balance.setValue((float) delta / 180);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            floatControl.setValue(-(mc.player.getDistance(e) * 5) - (volume.max / volume.get()));
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}

