package xd.harm.modules.impl.combat;

import com.google.common.eventbus.Subscribe;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.TridentItem;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import xd.harm.config.FriendStorage;
import xd.harm.events.movement.EventMotion;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.Setting;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.ModeListSetting;
import xd.harm.modules.settings.impl.SliderSetting;
import xd.harm.utils.math.SensUtils;
import xd.harm.utils.player.PlayerUtils;

@ModuleRegister(name = "AimHelper", category = Category.Combat, desc = "Прицеливание для оружий дальнего боя с предсказом позиции")
public class AimHelper extends Module {

    private final BooleanSetting predictPosition = new BooleanSetting("Предсказание позиции", true);
    private final BooleanSetting autoShoot = new BooleanSetting("Авто выстрел", true);
    private final SliderSetting minCharge = new SliderSetting("Мин. натяжение", 80.0F, 10.0F, 100.0F, 1.0F);
    private final SliderSetting aimRange = new SliderSetting("Дальность прицеливания", 30.0F, 10.0F, 100.0F, 1.0F);
    private final SliderSetting fov = new SliderSetting("Поле зрения", 90.0F, 1.0F, 180.0F, 1.0F);
    private final BooleanSetting ignoreWalls = new BooleanSetting("Игнорировать за стеной", true);
    private final BooleanSetting ignoreNaked = new BooleanSetting("Игнорировать голых", true);
    private final ModeListSetting weapons = new ModeListSetting("Оружие",
            new BooleanSetting("Лук", true),
            new BooleanSetting("Трезубец", true),
            new BooleanSetting("Арбалет", true)
    );

    private PlayerEntity target;
    private Vector2f targetRotation = new Vector2f(0.0F, 0.0F);
    private Vector2f smoothedRotation = new Vector2f(0.0F, 0.0F);
    private boolean hasTarget = false;
    private boolean rotationActive = false;
    private boolean wasHoldingWeapon = false;

    private final Map<PlayerEntity, List<Vector3d>> positionHistory = new WeakHashMap<>();
    private final Map<PlayerEntity, List<Long>> timeHistory = new WeakHashMap<>();

    public AimHelper() {
        this.addSettings(new Setting[]{
                this.weapons,
                this.aimRange,
                this.fov,
                this.autoShoot,
                this.minCharge,
                this.predictPosition,
                this.ignoreWalls,
                this.ignoreNaked
        });
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) {
            resetTarget();
            return;
        }

        boolean holdingWeapon = isHoldingWeapon();
        if (!holdingWeapon && wasHoldingWeapon) {
            resetTarget();
        }
        wasHoldingWeapon = holdingWeapon;

        if (!holdingWeapon) {
            return;
        }

        findTarget();

        if (target != null && target.isAlive()) {
            if (!hasTarget) {
                rotationActive = true;
            }

            updateRotation();

            if (autoShoot.get()) {
                tryAutoShoot();
            }

            synchronized (positionHistory) {
                List<Vector3d> positions = positionHistory.computeIfAbsent(target, p -> new ArrayList<>(5));
                List<Long> times = timeHistory.computeIfAbsent(target, p -> new ArrayList<>(5));
                positions.add(0, target.getPositionVec());
                times.add(0, System.currentTimeMillis());
                if (positions.size() > 5) {
                    positions.remove(positions.size() - 1);
                    times.remove(times.size() - 1);
                }
            }
        }
    }

    @Subscribe
    private void onMotion(EventMotion event) {
        if (target == null || !target.isAlive() || !isHoldingWeapon()) {
            return;
        }
        event.setYaw(smoothedRotation.x);
        event.setPitch(smoothedRotation.y);
        mc.player.rotationYawHead = smoothedRotation.x;
        mc.player.renderYawOffset = PlayerUtils.calculateCorrectYawOffset2(smoothedRotation.x);
        mc.player.rotationPitchHead = smoothedRotation.y;
    }

    private void updateRotation() {
        if (target == null || !target.isAlive()) {
            return;
        }

        float gravity = getProjectileGravity();
        Vector3d targetPos;

        if (predictPosition.get()) {
            float predictionFactor = 10.0F;
            double predX = target.getPosX() + (target.getPosX() - target.prevPosX) * predictionFactor;
            double predY = target.getPosY() + (target.getPosY() - target.prevPosY) * predictionFactor / 5.0 + target.getHeight() / 2.0F;
            double predZ = target.getPosZ() + (target.getPosZ() - target.prevPosZ) * predictionFactor;
            targetPos = new Vector3d(predX, predY, predZ);
        } else {
            targetPos = target.getPositionVec().add(0.0, target.getHeight() / 2.0, 0.0);
        }

        Vector3d eyePos = mc.player.getEyePosition(1.0F);
        Vector3d diff = targetPos.subtract(eyePos);

        float yaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0);
        float horizontalDist = (float) Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        float pitch = (float) (-Math.toDegrees(Math.atan2(diff.y, horizontalDist)));

        float gravityCompensation = mc.player.getDistance(target) * gravity;
        pitch -= gravityCompensation;
        pitch = MathHelper.clamp(pitch, -89.0F, 89.0F);

        targetRotation = new Vector2f(yaw, pitch);

        float gcd = SensUtils.getGCDValue();
        if (gcd > 0.0F) {
            yaw -= (yaw - smoothedRotation.x) % gcd;
            pitch -= (pitch - smoothedRotation.y) % gcd;
        }

        smoothedRotation = new Vector2f(yaw, pitch);
        hasTarget = true;
    }

    private void tryAutoShoot() {
        if (!autoShoot.get() || target == null || !target.isAlive()) {
            return;
        }

        if (!isCloseToTarget()) {
            return;
        }

        if (ignoreWalls.get() && !canSeeEntity(target)) {
            return;
        }

        ItemStack stack = mc.player.getHeldItemMainhand();

        if (isCrossbowCharged()) {
            mc.gameSettings.keyBindUseItem.setPressed(true);
            return;
        }

        if (isBowDrawing() || isTridentDrawing()) {
            int useTime = mc.player.getItemInUseMaxCount();
            int maxChargeTime = getMaxChargeTime(stack);
            float chargePercent = (float) useTime / (float) maxChargeTime * 100.0F;

            if (chargePercent >= minCharge.get()) {
                mc.playerController.onStoppedUsingItem(mc.player);
            }
        }
    }

    private boolean isCloseToTarget() {
        if (target != null && targetRotation != null && smoothedRotation != null) {
            float yawDiff = MathHelper.wrapDegrees(targetRotation.x - smoothedRotation.x);
            float pitchDiff = MathHelper.wrapDegrees(targetRotation.y - smoothedRotation.y);
            float dist = (float) Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
            return dist < 5.0F;
        }
        return false;
    }

    private float getProjectileGravity() {
        ItemStack stack = mc.player.getHeldItemMainhand();
        if (stack.getItem() instanceof BowItem && (Boolean) weapons.getValueByName("Лук").get()) {
            return 0.17F;
        }
        if (stack.getItem() instanceof CrossbowItem && (Boolean) weapons.getValueByName("Арбалет").get()) {
            return 0.16F;
        }
        if (stack.getItem() instanceof TridentItem && (Boolean) weapons.getValueByName("Трезубец").get()) {
            return 0.22F;
        }
        return 0.0F;
    }

    private boolean isHoldingWeapon() {
        if (mc.player == null || mc.player.getHeldItemMainhand().isEmpty()) {
            return false;
        }
        return isBowDrawing() || isTridentDrawing() || isCrossbowCharged();
    }

    private boolean isBowDrawing() {
        ItemStack stack = mc.player.getHeldItemMainhand();
        return stack.getItem() instanceof BowItem
                && (Boolean) weapons.getValueByName("Лук").get()
                && mc.player.isHandActive();
    }

    private boolean isTridentDrawing() {
        ItemStack stack = mc.player.getHeldItemMainhand();
        return stack.getItem() instanceof TridentItem
                && (Boolean) weapons.getValueByName("Трезубец").get()
                && mc.player.isHandActive();
    }

    private boolean isCrossbowCharged() {
        ItemStack stack = mc.player.getHeldItemMainhand();
        return stack.getItem() instanceof CrossbowItem
                && (Boolean) weapons.getValueByName("Арбалет").get()
                && CrossbowItem.isCharged(stack);
    }

    private int getMaxChargeTime(ItemStack stack) {
        if (stack.getItem() instanceof BowItem) {
            return 20;
        }
        if (stack.getItem() instanceof TridentItem) {
            return 10;
        }
        return 20;
    }

    private boolean canSeeEntity(PlayerEntity entity) {
        Vector3d eyePos = mc.player.getEyePosition(1.0F);
        Vector3d targetPos = entity.getPositionVec().add(0.0, entity.getHeight() * 0.5, 0.0);
        RayTraceContext context = new RayTraceContext(
                eyePos, targetPos,
                RayTraceContext.BlockMode.COLLIDER,
                RayTraceContext.FluidMode.NONE,
                mc.player
        );
        RayTraceResult result = mc.world.rayTraceBlocks(context);
        return result.getType() == RayTraceResult.Type.MISS;
    }

    private double getAngleTo(PlayerEntity entity) {
        Vector3d eyePos = mc.player.getEyePosition(1.0F);
        Vector3d targetPos = entity.getPositionVec().add(0.0, entity.getHeight() * 0.5, 0.0);
        Vector3d diff = targetPos.subtract(eyePos);

        float yaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0);
        float pitch = (float) (-Math.toDegrees(Math.atan2(diff.y, Math.sqrt(diff.x * diff.x + diff.z * diff.z))));

        float yawDiff = MathHelper.wrapDegrees(yaw - mc.player.rotationYaw);
        float pitchDiff = MathHelper.wrapDegrees(pitch - mc.player.rotationPitch);

        return Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);
    }

    private boolean isValidTarget(Entity entity) {
        if (!(entity instanceof PlayerEntity)) return false;
        if (entity instanceof ClientPlayerEntity) return false;
        if (entity instanceof ArmorStandEntity) return false;

        PlayerEntity player = (PlayerEntity) entity;
        if (!player.isAlive()) return false;
        if (player.isInvulnerable()) return false;
        if (player.isCreative()) return false;

        if (player.getName().getString().equalsIgnoreCase(mc.player.getName().getString())) return false;
        if (FriendStorage.isFriend(player.getName().getString())) return false;

        if (ignoreNaked.get()) {
            if (player.getTotalArmorValue() == 0) {
                return false;
            }
        }

        double distSq = mc.player.getDistanceSq(player);
        float range = aimRange.get();
        if (distSq > range * range) {
            return false;
        }

        if (ignoreWalls.get() && !canSeeEntity(player)) {
            return false;
        }

        return getAngleTo(player) <= fov.get() * 2.0F;
    }

    private void findTarget() {
        List<PlayerEntity> targets = new ArrayList<>();
        for (PlayerEntity entity : mc.world.getPlayers()) {
            if (isValidTarget(entity)) {
                targets.add(entity);
            }
        }
        targets.sort(Comparator.comparingDouble(this::getAngleTo));
        target = targets.isEmpty() ? null : targets.get(0);
    }

    private void resetTarget() {
        if (mc.player != null) {
            smoothedRotation = new Vector2f(mc.player.rotationYaw, mc.player.rotationPitch);
            targetRotation = new Vector2f(mc.player.rotationYaw, mc.player.rotationPitch);
            mc.player.renderYawOffset = Integer.MIN_VALUE;
        }
        target = null;
        hasTarget = false;
        rotationActive = false;
    }

    @Override
    public boolean onEnable() {
        super.onEnable();
        resetTarget();
        return false;
    }

    @Override
    public boolean onDisable() {
        super.onDisable();
        resetTarget();
        return false;
    }

    public PlayerEntity getTarget() {
        return target;
    }
}
