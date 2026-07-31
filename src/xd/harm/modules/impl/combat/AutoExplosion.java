package xd.harm.modules.impl.combat;

import com.google.common.eventbus.Subscribe;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EnderCrystalEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import xd.harm.config.FriendStorage;
import xd.harm.events.combat.EventClickBlockRight;
import xd.harm.events.combat.PlaceObsidianEvent;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.ModeListSetting;
import xd.harm.modules.settings.impl.ModeSetting;
import xd.harm.utils.player.InventoryUtil;
import xd.harm.utils.rotation.AuraUtil;
import xd.harm.utils.rotation.FreeLookHandler;
import xd.harm.utils.rotation.Rotation;
import xd.harm.utils.rotation.RotationHandler;

@ModuleRegister(name = "AutoExplosion", category = Category.Combat, desc = "Автоматически взрывает кристаллы")
public class AutoExplosion extends Module {

    private final ModeSetting mode = new ModeSetting("Режим", "Легит", "Легит", "Рейдж");
    private final BooleanSetting keepCrystal = new BooleanSetting("Оставлять кристалл в руке", false);
    private final ModeListSetting protection = new ModeListSetting("Не взрывать",
            new BooleanSetting("Себя", true),
            new BooleanSetting("Друзей", true),
            new BooleanSetting("Предметы", true));

    private BlockPos targetPos;
    private int targetSlot = -1;
    private int oldSlot = -1;
    private boolean needSync;
    private AxisAlignedBB crystalArea;
    private boolean blocked;
    private long lastPlaceTime;

    private static final long PLACE_DELAY = 1L;

    public AutoExplosion() {
        addSettings(mode, keepCrystal, protection);
    }

    @Subscribe
    public void onPlaceObsidian(PlaceObsidianEvent event) {
        if (event == null || mc.player == null || mc.world == null) {
            return;
        }

        if (event.getBlock() == Blocks.OBSIDIAN) {
            if (mode.is("Легит") && mc.player.getCooldownTracker().hasCooldown(Items.END_CRYSTAL)) {
                return;
            }

            int slotInHotBar = InventoryUtil.getHotBarSlot(Items.END_CRYSTAL);
            if (slotInHotBar == -1) {
                return;
            }

            this.targetPos = event.getPos();
            this.targetSlot = slotInHotBar;
        }

        if (mode.is("Легит")) {
            this.blocked = true;
        }
    }

    @Subscribe
    public void onUpdate(EventUpdate e) {
        if (e == null || mc.player == null || mc.world == null || mc.playerController == null) {
            return;
        }

        if (needSync) {
            needSync = false;
            if (!keepCrystal.get() && oldSlot >= 0 && oldSlot < 9) {
                mc.player.inventory.currentItem = oldSlot;
                mc.playerController.syncCurrentPlayItem();
            }
            oldSlot = -1;
        } else if (targetPos != null) {
            if (mc.world.getBlockState(targetPos).isAir()) {
                targetPos = null;
                targetSlot = -1;
            } else if (blocked) {
                blocked = false;
            } else if (targetSlot >= 0 && targetSlot < 9) {
                Vector3d eyeVec = mc.player.getEyePosition(1.0F);
                Vector3d hitVec = AuraUtil.getClosestVec(eyeVec, new AxisAlignedBB(targetPos));
                Vector3d offset = hitVec.subtract(eyeVec);

                float targetYaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(offset.z, offset.x)) - 90.0D);
                float targetPitch = (float) (-Math.toDegrees(Math.atan2(offset.y, Math.hypot(offset.x, offset.z))));
                if (mode.is("Рейдж")) {
                    RotationHandler.update(new Rotation(targetYaw, targetPitch), 360.0F, 1, 1);
                } else {
                    RotationHandler.update(new Rotation(targetYaw, targetPitch), 180.0F, 1, 7);
                }

                oldSlot = mc.player.inventory.currentItem;
                mc.player.inventory.currentItem = targetSlot;
                mc.playerController.syncCurrentPlayItem();

                Vector3d reverse = offset.inverse();
                BlockRayTraceResult rayTraceResult = new BlockRayTraceResult(
                        hitVec,
                        Direction.getFacingFromVector(reverse.x, reverse.y, reverse.z),
                        targetPos,
                        false
                );

                ActionResultType result = mc.playerController.processRightClickBlock(
                        mc.player,
                        mc.world,
                        Hand.MAIN_HAND,
                        rayTraceResult
                );
                if (result.isSuccessOrConsume()) {
                    mc.player.swingArm(Hand.MAIN_HAND);
                }

                needSync = true;
                lastPlaceTime = System.currentTimeMillis();
                targetPos = null;
                targetSlot = -1;
            } else {
                targetPos = null;
            }
        }

        if (crystalArea != null) {
            if (mode.is("Легит") && System.currentTimeMillis() - lastPlaceTime < PLACE_DELAY) {
                return;
            }

            for (Entity entity : mc.world.getAllEntities()) {
                if (!(entity instanceof EnderCrystalEntity) || !crystalArea.contains(entity.getPositionVec())) {
                    continue;
                }

                EnderCrystalEntity crystalEntity = (EnderCrystalEntity) entity;
                if (hasProtectionBlock(crystalEntity)) {
                    crystalArea = null;
                    return;
                }

                if (mode.is("Легит") && !entity.getBoundingBox().contains(mc.player.getEyePosition(1.0F))) {
                    Vector3d direction = AuraUtil.getClosestVec(entity);
                    float targetYaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0D);
                    float targetPitch = (float) (-Math.toDegrees(Math.atan2(direction.y, Math.hypot(direction.x, direction.z))));
                    RotationHandler.update(new Rotation(targetYaw, targetPitch), 180.0F, 1, 7);
                }

                mc.playerController.attackEntity(mc.player, entity);
                mc.player.swingArm(Hand.MAIN_HAND);
                crystalArea = null;
                return;
            }
        }
    }

    @Subscribe
    public void onClickBlockRight(EventClickBlockRight e) {
        if (e == null || mc.player == null || mc.world == null || e.getResult() == null || e.getWorld() == null) {
            return;
        }

        if (mode.is("Легит") && mc.player.getCooldownTracker().hasCooldown(Items.END_CRYSTAL)) {
            return;
        }

        Block block = e.getWorld().getBlockState(e.getResult().getPos()).getBlock();
        if (block == Blocks.OBSIDIAN || block == Blocks.BEDROCK) {
            crystalArea = new AxisAlignedBB(e.getResult().getPos().up()).grow(0.1D);
        }
    }

    private boolean shouldNotExplodeBecauseOfSelf(EnderCrystalEntity crystalEntity) {
        if (crystalEntity == null || mc.player == null) {
            return false;
        }
        return protection.getValueByName("Себя").get() && crystalEntity.getPosY() <= mc.player.getPosY() + 0.1D;
    }

    private boolean shouldNotExplodeBecauseOfFriend(EnderCrystalEntity crystalEntity) {
        if (crystalEntity == null || mc.player == null || mc.world == null) {
            return false;
        }
        if (!protection.getValueByName("Друзей").get()) {
            return false;
        }

        return mc.world.getEntitiesWithinAABB(PlayerEntity.class, crystalEntity.getBoundingBox().grow(6.0D))
                .stream()
                .anyMatch(entity -> entity != mc.player
                        && entity.isAlive()
                        && entity.getName() != null
                        && FriendStorage.isFriend(entity.getName().getString()));
    }

    private boolean shouldNotExplodeBecauseOfItems(EnderCrystalEntity crystalEntity) {
        if (crystalEntity == null || mc.world == null) {
            return false;
        }
        if (!protection.getValueByName("Предметы").get()) {
            return false;
        }

        return mc.world.getEntitiesWithinAABB(ItemEntity.class, crystalEntity.getBoundingBox().grow(6.0D))
                .stream()
                .map(e -> e.getItem().getItem())
                .anyMatch(item -> item == Items.TOTEM_OF_UNDYING
                        || item == Items.END_CRYSTAL
                        || item == Items.ENCHANTED_GOLDEN_APPLE
                        || item == Items.NETHERITE_HELMET
                        || item == Items.NETHERITE_CHESTPLATE
                        || item == Items.NETHERITE_LEGGINGS
                        || item == Items.NETHERITE_BOOTS
                        || item == Items.NETHERITE_SWORD
                        || item == Items.DIAMOND_SWORD
                        || item == Items.ELYTRA
                        || item == Items.TRIDENT);
    }

    private boolean hasProtectionBlock(EnderCrystalEntity crystalEntity) {
        return shouldNotExplodeBecauseOfSelf(crystalEntity)
                || shouldNotExplodeBecauseOfFriend(crystalEntity)
                || shouldNotExplodeBecauseOfItems(crystalEntity);
    }

    @Override
    public boolean onEnable() {
        FreeLookHandler.setActive(false);
        return super.onEnable();
    }

    @Override
    public boolean onDisable() {
        FreeLookHandler.setActive(false);
        targetPos = null;
        targetSlot = -1;
        oldSlot = -1;
        needSync = false;
        crystalArea = null;
        blocked = false;
        lastPlaceTime = 0L;
        return super.onDisable();
    }
}
