package xd.harm.modules.impl.misc;

import com.google.common.eventbus.Subscribe;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SwordItem;
import net.minecraft.network.play.client.CUseEntityPacket;
import net.minecraft.network.play.server.SEntityStatusPacket;
import net.minecraft.network.play.server.SExplosionPacket;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.server.ServerWorld;
import xd.harm.events.network.EventPacket;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;

import java.util.UUID;

@ModuleRegister(name = "TestPlayer", category = Category.Misc, desc = "Создание фейковых игроков для тестирования")
public class TestPlayer extends Module {
    public static AbstractClientPlayerEntity fakePlayer = null;
    UUID uuid = PlayerEntity.getOfflineUUID("Harmony");

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (mc.world == null || mc.player == null || fakePlayer == null) {
            return;
        }
        if (mc.world != null && fakePlayer != null) {
            AbstractClientPlayerEntity e = fakePlayer;
            float[] rotate = new float[]{0, 0, 0, 0};
            e.rotationYaw = rotate[0];
            e.rotationYawHead = rotate[0];
            e.rotationPitch = rotate[1];
            e.rotationPitchHead = rotate[1];
            e.setPrimaryHand(mc.player.getPrimaryHand());
            e.openContainer = mc.player.openContainer;
            if (mc.player.getActiveHand() != null && mc.player.isHandActive()) {
                e.setActiveHand(mc.player.getActiveHand());
            } else {
                e.resetActiveHand();
            }
            e.activeItemStack = mc.player.activeItemStack;
        }
    }

    public static boolean moveKeysPressed() {
        return mc.gameSettings.keyBindForward.isKeyDown() || mc.gameSettings.keyBindBack.isKeyDown() || mc.gameSettings.keyBindLeft.isKeyDown() || mc.gameSettings.keyBindRight.isKeyDown();
    }

    @Subscribe
    public void onPacket(EventPacket event) {
        if (mc.world != null && fakePlayer != null) {
            Entity targetEntity = fakePlayer;
            if (event.getPacket() instanceof CUseEntityPacket) {
                CUseEntityPacket p = (CUseEntityPacket) event.getPacket();
                if ((p.getAction() == CUseEntityPacket.Action.ATTACK) && targetEntity.getDistance(mc.player) < 6) {
                    if (((AbstractClientPlayerEntity) targetEntity).hurtTime > 0) {
                        mc.player.world.playSound(mc.player, targetEntity.getPosX(), targetEntity.getPosY(), targetEntity.getPosZ(), SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE, mc.player.getSoundCategory(), 1.0f, 1.0f);
                        return;
                    }
                    ((AbstractClientPlayerEntity) targetEntity).hurtTime = 9;
                    boolean cooled = (double) mc.player.getCooledAttackStrength(1.0f) >= 0.9;

                    boolean canCrit = cooled && (!mc.player.isServerSprintState() || !mc.player.isSprinting() || !moveKeysPressed() && !mc.player.isOnGround()) && (mc.player.fallDistance != 0.0f && !mc.player.isOnGround());
                    boolean canKnock = cooled && !canCrit && (mc.player.isServerSprintState() || mc.player.isSprinting());
                    boolean canSweep = cooled && (!mc.player.isServerSprintState() || !mc.player.isSprinting()) && mc.player.isOnGround() && mc.player.collidedVertically && mc.player.getHeldItemMainhand().getItem() instanceof SwordItem;
                    boolean canStrong = !canCrit && !canKnock && !canSweep && mc.player.fallDistance == 0.0f && cooled;
                    boolean canWeak = !canStrong;
                    float f = (float) mc.player.getAttribute(Attributes.ATTACK_DAMAGE).getValue();
                    float f1 = EnchantmentHelper.getModifierForCreature(mc.player.getHeldItemMainhand(), mc.player.getCreatureAttribute());
                    float f2 = mc.player.getCooledAttackStrength(0.5f);
                    f *= 0.2f + f2 * f2 * 0.8f;
                    float damage = (f1 *= f2) / 2.0f + 1.6f;
                    if (canCrit || canKnock || canSweep || canStrong || canWeak) {
                        mc.player.world.playSound(mc.player, targetEntity.getPosX(), targetEntity.getPosY(), targetEntity.getPosZ(), SoundEvents.ENTITY_PLAYER_HURT, mc.player.getSoundCategory(), 0.5f, 1.0f);
                    }
                    if (canCrit) {
                        damage = (float) ((double) damage * 1.5);
                        mc.player.world.playSound(mc.player, targetEntity.getPosX(), targetEntity.getPosY(), targetEntity.getPosZ(), SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, mc.player.getSoundCategory(), 1.0f, 1.0f);
                    } else if (canKnock) {
                        mc.player.world.playSound(mc.player, targetEntity.getPosX(), targetEntity.getPosY(), targetEntity.getPosZ(), SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, mc.player.getSoundCategory(), 1.0f, 1.0f);
                    } else if (canSweep) {
                        mc.player.world.playSound(mc.player, targetEntity.getPosX(), targetEntity.getPosY(), targetEntity.getPosZ(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, mc.player.getSoundCategory(), 1.0f, 1.0f);
                    } else if (canStrong) {
                        mc.player.world.playSound(mc.player, targetEntity.getPosX(), targetEntity.getPosY(), targetEntity.getPosZ(), SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, mc.player.getSoundCategory(), 1.0f, 1.0f);
                    } else if (canWeak) {
                        mc.player.world.playSound(mc.player, targetEntity.getPosX(), targetEntity.getPosY(), targetEntity.getPosZ(), SoundEvents.ENTITY_PLAYER_ATTACK_WEAK, mc.player.getSoundCategory(), 1.0f, 1.0f);
                    }
                    if (((PlayerEntity) targetEntity).getAbsorptionAmount() != 0.0f) {
                        float addToDamageHP = ((PlayerEntity) targetEntity).getAbsorptionAmount() - damage / 2.0f + ((PlayerEntity) targetEntity).getAbsorptionAmount();
                        ((PlayerEntity) targetEntity).setAbsorptionAmount(MathHelper.clamp(((PlayerEntity) targetEntity).getAbsorptionAmount() - damage, 0.0f, 1000.0f));
                        ((LivingEntity) targetEntity).setHealth(MathHelper.clamp(((LivingEntity) targetEntity).getHealth() - addToDamageHP, 0.99f, 20.0f));
                    } else {
                        ((LivingEntity) targetEntity).setHealth(MathHelper.clamp(((LivingEntity) targetEntity).getHealth() - damage + ((PlayerEntity) targetEntity).getAbsorptionAmount(), 0.99f, 20.0f));
                    }
                    ((AbstractClientPlayerEntity) targetEntity).limbSwingAmount = (float) ((AbstractClientPlayerEntity) targetEntity).hurtTime / 10.0f;
                    if (((LivingEntity) targetEntity).getHealth() < 1.0f && ((PlayerEntity) targetEntity).getAbsorptionAmount() == 0.0f) {
                        ((LivingEntity) targetEntity).clearActivePotions();
                        mc.particles.emitParticleAtEntity(targetEntity, ParticleTypes.TOTEM_OF_UNDYING, 30);
                        mc.player.world.playSound(mc.player, targetEntity.getPosX(), targetEntity.getPosY(), targetEntity.getPosZ(), SoundEvents.ITEM_TOTEM_USE, mc.player.getSoundCategory(), 1.0f, 1.0f);

                        mc.world.playSound(targetEntity.getPosX(), targetEntity.getPosY(), targetEntity.getPosZ(), SoundEvents.ENTITY_PLAYER_HURT, targetEntity.getSoundCategory(), 1.0f, 1.0f, false);
                        ((LivingEntity) targetEntity).addPotionEffect(new EffectInstance(Effects.ABSORPTION, 100, 0));
                        ((LivingEntity) targetEntity).addPotionEffect(new EffectInstance(Effects.REGENERATION, 880, 0));
                        ((PlayerEntity) targetEntity).setAbsorptionAmount(8.0f);
                        ((LivingEntity) targetEntity).setHealth(1.0f);
                        ((ServerWorld) mc.player.world).getChunkProvider().sendToAllTracking(mc.player, new SEntityStatusPacket(mc.player, (byte) 55));

                    }
                }
            }
            if (event.getPacket() instanceof SExplosionPacket) {
                SExplosionPacket explosion = (SExplosionPacket) event.getPacket();
                boolean canDamage;
                if (fakePlayer.hurtTime > 1) {
                    return;
                }
                Vector3d posExplosion = new Vector3d(explosion.getX(), explosion.getY(), explosion.getZ());
                boolean bl = canDamage = getDistanceAtVecToVec(fakePlayer.getPositionVec(), posExplosion) < 11.0;
                if (!canDamage) {
                    return;
                }
                float damage = (float) ((9.0 - getDistanceAtVecToVec(fakePlayer.getPositionVec(), posExplosion)) / 9.0) * 10.0f;
                float hp = (float) MathHelper.clamp(fakePlayer.getHealth() - damage + fakePlayer.getAbsorptionAmount(), 0.001, 20.0);
                if (fakePlayer.getAbsorptionAmount() != 0.0f) {
                    fakePlayer.setAbsorptionAmount(MathHelper.clamp(fakePlayer.getAbsorptionAmount() - damage, 0.0f, 8.0f));
                }
                if (fakePlayer.getAbsorptionAmount() == 0.0f || damage > fakePlayer.getAbsorptionAmount()) {
                    fakePlayer.setHealth((int) MathHelper.clamp(hp + 1.0f, 1.0f, 20.0f));
                }
                mc.player.world.playSound(mc.player, fakePlayer.getPosX(), fakePlayer.getPosY(), fakePlayer.getPosZ(), SoundEvents.ENTITY_PLAYER_DEATH, mc.player.getSoundCategory(), 0.5f, 1.0f);
                if (hp < 1.0f && fakePlayer.getAbsorptionAmount() < 4.0f && fakePlayer.getHeldItemOffhand().getItem() == Items.TOTEM_OF_UNDYING) {
                    fakePlayer.clearActivePotions();
                    mc.particles.emitParticleAtEntity(fakePlayer, ParticleTypes.TOTEM_OF_UNDYING, 30);
                    mc.world.playSound(fakePlayer.getPosX(), fakePlayer.getPosY(), fakePlayer.getPosZ(), SoundEvents.ENTITY_PLAYER_HURT, fakePlayer.getSoundCategory(), 1.0f, 1.0f, false);
                    fakePlayer.addPotionEffect(new EffectInstance(Effects.ABSORPTION, 100, 1));
                    fakePlayer.addPotionEffect(new EffectInstance(Effects.REGENERATION, 880, 0));
                    fakePlayer.setHeldItem(Hand.OFF_HAND, new ItemStack(Item.getItemById(449), fakePlayer.getHeldItemOffhand().getMaxStackSize() - 1));
                    fakePlayer.setHealth(1.0f);
                }
            }
        }
    }


    public void onToggled(boolean actived) {
        if (!actived) {
            mc.world.removeEntityFromWorld(462462998);
            fakePlayer = null;
        } else if (actived) {
            fakePlayer = new AbstractClientPlayerEntity(mc.world, new GameProfile(uuid,  "Harmony"));
            mc.world.addEntity(462462998, fakePlayer);
            fakePlayer.copyLocationAndAnglesFrom(mc.player);
            fakePlayer.setPosition(mc.player.getPosX(), mc.player.getPosY(), mc.player.getPosZ());
            fakePlayer.setHeldItem(Hand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING, 64));
            fakePlayer.setHeldItem(Hand.MAIN_HAND, new ItemStack(Items.NETHERITE_SWORD, 1));
            fakePlayer.setHealth(20.0f);
        }
    }

    public static final double getDistanceAtVecToVec(Vector3d first, Vector3d second) {
        return Math.sqrt((first.x - second.x) * (first.x - second.x) + (first.y - second.y) * (first.y - second.y) + (first.z - second.z) * (first.z - second.z));
    }


    @Override
    public boolean onDisable() {
        if (mc.player != null) {
            onToggled(false);
        }
        super.onDisable();
        return false;
    }

    @Override
    public boolean onEnable() {
        if (mc.player != null) {
            onToggled(true);
        }
        super.onEnable();
        return false;
    }
}
