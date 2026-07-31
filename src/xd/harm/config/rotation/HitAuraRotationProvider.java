package xd.harm.config.rotation;

import net.minecraft.entity.LivingEntity;
import xd.harm.modules.impl.combat.HitAura;

public interface HitAuraRotationProvider {
    void onEnable(HitAura hitAura);

    void onDisable(HitAura hitAura);

    boolean rotate(HitAura hitAura, LivingEntity target, boolean isAttacking);

    default void onAttack(HitAura hitAura, LivingEntity target) {
    }
}
