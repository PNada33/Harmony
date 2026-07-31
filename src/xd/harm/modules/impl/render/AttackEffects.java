package xd.harm.modules.impl.render;

import com.google.common.eventbus.Subscribe;

import net.minecraft.entity.Entity;
import net.minecraft.network.play.client.CUseEntityPacket;
import net.minecraft.network.play.server.SPlaySoundEffectPacket;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;

import xd.harm.events.network.EventPacket;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;

@ModuleRegister(name = "AttackEffects", category = Category.Render, desc = "Добавляет эффекты и частицы при атаке сущности")
public class AttackEffects extends Module {

    // --- Particles ---
    private final BooleanSetting criticalParticles = new BooleanSetting("Critical Particles", false);
    private final BooleanSetting sharpnessParticles = new BooleanSetting("Sharpness Particles", true);

    // --- Sounds ---
    private final BooleanSetting critSound = new BooleanSetting("Critical Sound", false);
    private final BooleanSetting knockbackSound = new BooleanSetting("Knockback Sound", false);
    private final BooleanSetting strongSound = new BooleanSetting("Strong Sound", false);
    private final BooleanSetting sweepSound = new BooleanSetting("Sweep Sound", false);
    private final BooleanSetting weakSound = new BooleanSetting("Weak Sound", false);
    private final BooleanSetting noDamageSound = new BooleanSetting("No Damage Sound", false);

    public AttackEffects() {
        addSettings(
                criticalParticles,
                sharpnessParticles,
                critSound,
                knockbackSound,
                strongSound,
                sweepSound,
                weakSound,
                noDamageSound
        );
    }

    @Subscribe
    public void onPacket(EventPacket event) {
        if (mc.player == null || mc.world == null) return;

        // --- Фильтрация звуков атаки (входящие пакеты) ---
        if (event.isReceive() && event.getPacket() instanceof SPlaySoundEffectPacket) {
            SPlaySoundEffectPacket soundPacket = (SPlaySoundEffectPacket) event.getPacket();
            SoundEvent sound = soundPacket.getSound();

            if (sound == SoundEvents.ENTITY_PLAYER_ATTACK_CRIT && !critSound.get()) {
                event.cancel();
            } else if (sound == SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK && !knockbackSound.get()) {
                event.cancel();
            } else if (sound == SoundEvents.ENTITY_PLAYER_ATTACK_STRONG && !strongSound.get()) {
                event.cancel();
            } else if (sound == SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP && !sweepSound.get()) {
                event.cancel();
            } else if (sound == SoundEvents.ENTITY_PLAYER_ATTACK_WEAK && !weakSound.get()) {
                event.cancel();
            } else if (sound == SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE && !noDamageSound.get()) {
                event.cancel();
            }
        }

        // --- Спавн частиц при отправке пакета атаки ---
        if (event.isSend() && event.getPacket() instanceof CUseEntityPacket) {
            CUseEntityPacket packet = (CUseEntityPacket) event.getPacket();

            if (packet.getAction() == CUseEntityPacket.Action.ATTACK) {
                Entity target = packet.getEntityFromWorld(mc.world);
                if (target != null) {
                    if (criticalParticles.get()) {
                        mc.player.onCriticalHit(target);
                    }
                    if (sharpnessParticles.get()) {
                        mc.player.onEnchantmentCritical(target);
                    }
                }
            }
        }
    }
}
