package xd.harm.modules.impl.combat;

import com.google.common.eventbus.Subscribe;

import xd.harm.events.movement.EventMotion;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.SliderSetting;
import xd.harm.utils.math.MathUtil;
import xd.harm.utils.math.StopWatch;
import xd.harm.utils.player.InventoryUtil;
import xd.harm.utils.player.MoveUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.network.play.client.CHeldItemChangePacket;
import net.minecraft.network.play.client.CPlayerTryUseItemPacket;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.Hand;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

@ModuleRegister(name = "AutoPotion", category = Category.Combat, desc = "Автоматически бросает зелья")
public class PotionThrower extends Module {
    private float previousPitch;
    private final StopWatch stopWatch = new StopWatch();

    private final BooleanSetting heal = new BooleanSetting("Зелье исцеления", false);
    private final SliderSetting healthThreshold = new SliderSetting("Здоровье", 10.0f, 5.0f, 20.0f, 0.5f) .setVisible(() -> heal.get());

    public PotionThrower() {
        this.addSettings(heal, healthThreshold);
    }

    @Subscribe
    public void onMotion(EventMotion e) {
        if (!canThrowPotion()) {
            return;
        }

        if (this.isActive()) {
            Vector3d posPoint = this.findNearestCollision();
            Vector2f rotationVector = posPoint == null ? new Vector2f(mc.player.rotationYaw, 90.0F) : MathUtil.rotationToVec(posPoint);
            this.previousPitch = rotationVector.y;
            e.setYaw(rotationVector.x);
            e.setPitch(this.previousPitch);
            mc.player.rotationPitchHead = this.previousPitch;
        }

        e.setPostMotion(() -> {
            boolean pitchIsValid = this.previousPitch == e.getPitch();
            int oldCurrentItem = mc.player.inventory.currentItem;
            Potions[] potions = Potions.values();
            int i = potions.length;

            for(int i1 = 0; i1 < i; ++i1) {
                Potions potion = potions[i1];
                potion.state = true;

                if (potion == Potions.HEALING && !this.hasPotionInInventory(potion)) {
                    continue;
                }

                if (!this.shouldUsePotion(potion) && potion.state && pitchIsValid) {
                    this.sendPotion(potion);
                    mc.player.connection.sendPacket(new CHeldItemChangePacket(oldCurrentItem));
                    mc.playerController.syncCurrentPlayItem();
                }
            }
        });
    }

    public boolean isActive() {
        for (Potions potionType : Potions.values()) {
            if (potionType == Potions.HEALING) {
                if (this.heal.get() && !this.shouldUsePotion(potionType) && potionType.isState() && this.hasPotionInInventory(potionType)) {
                    return true;
                }
            } else {
                if (!this.shouldUsePotion(potionType) && potionType.isState()) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean canThrowPotion() {
        boolean isOnGround = !MoveUtils.isBlockUnder(0.5F) || mc.player.isOnGround();
        boolean timeIsReached = stopWatch.isReached(700);
        boolean ticksExisted = mc.player.ticksExisted > 100;

        return isOnGround && timeIsReached && ticksExisted;
    }

    private boolean shouldUsePotion(Potions potions) {
        if (potions == Potions.HEALING) {
            if (this.heal.get() && mc.player.getHealth() < this.healthThreshold.get() && this.hasPotionInInventory(potions)) {
                potions.state = true;
                return false;
            } else {
                potions.state = false;
                return false;
            }
        }

        if (mc.player.isPotionActive(potions.getPotion())) {
            potions.state = false;
            return true;
        }

        int potionId = potions.getId();
        if (this.findPotionSlot(potionId, true) == -1 && this.findPotionSlot(potionId, false) == -1) {
            potions.state = false;
            return true;
        }
        return false;
    }

    private boolean hasPotionInInventory(Potions potion) {
        int potionId = potion.getId();
        return this.findPotionSlot(potionId, true) != -1 || this.findPotionSlot(potionId, false) != -1;
    }

    private void sendPotion(Potions potions) {
        int potionId = potions.getId();
        int hotBarSlot = this.findPotionSlot(potionId, true);
        int inventorySlot = this.findPotionSlot(potionId, false);

        if (mc.player.isPotionActive(potions.getPotion())) {
            potions.state = false;
        }

        if (hotBarSlot != -1) {
            this.sendUsePacket(hotBarSlot, Hand.MAIN_HAND);
        } else if (inventorySlot != -1) {
            int bestSlotInHotBar = InventoryUtil.getInstance().findBestSlotInHotBar();
            InventoryUtil.moveItem(inventorySlot, bestSlotInHotBar + 36, mc.player.inventory.getStackInSlot(bestSlotInHotBar).getItem() != Items.AIR);
            this.sendUsePacket(bestSlotInHotBar, Hand.MAIN_HAND);
        }
    }

    private void sendUsePacket(int slot, Hand hand) {
        mc.player.connection.sendPacket(new CHeldItemChangePacket(slot));
        mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(hand));
        mc.player.swingArm(Hand.MAIN_HAND);
        this.previousPitch = 0.0F;
        this.stopWatch.reset();
    }

    private int findPotionSlot(int id, boolean inHotBar) {
        int start = inHotBar ? 0 : 9;
        int end = inHotBar ? 9 : 36;

        for(int i = start; i < end; ++i) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof SplashPotionItem) {
                List<EffectInstance> potionEffects = PotionUtils.getEffectsFromStack(stack);
                Iterator iterator = potionEffects.iterator();

                while(iterator.hasNext()) {
                    EffectInstance effectInstance = (EffectInstance)iterator.next();
                    if (effectInstance.getPotion() == Effect.get(id)) {
                        return i;
                    }
                }
            }
        }

        return -1;
    }

    private Vector3d findNearestCollision() {
        return (Vector3d)mc.world.getCollisionShapes(mc.player, mc.player.getBoundingBox().grow(0.0, 0.5, 0.0)).toList().stream().min(Comparator.comparingDouble((box) -> {
            return box.getBoundingBox().getCenter().squareDistanceTo(mc.player.getPositionVec());
        })).map((box) -> {
            return box.getBoundingBox().getCenter();
        }).orElse((Vector3d) null);
    }

    public static enum Potions {
        STRENGTH(Effects.STRENGTH, 5),
        SPEED(Effects.SPEED, 1),
        FIRE_RESIST(Effects.FIRE_RESISTANCE, 12),
        HEALING(Effects.INSTANT_HEALTH, 6);

        private final Effect potion;
        private final int id;
        private boolean state;

        private Potions(Effect potion, int potionId) {
            this.potion = potion;
            this.id = potionId;
        }

        public Effect getPotion() {
            return this.potion;
        }

        public int getId() {
            return this.id;
        }

        public boolean isState() {
            return this.state;
        }
    }
}

