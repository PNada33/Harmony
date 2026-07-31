package xd.harm.modules.models.dragon;

import com.google.common.eventbus.Subscribe;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import xd.harm.Harmony;
import xd.harm.events.world.EventUpdate;
import xd.harm.utils.animations.InfinityAnimation;
import xd.harm.utils.client.IMinecraft;
import xd.harm.utils.client.TimerUtility;
import xd.harm.utils.math.MathUtil;
import xd.harm.modules.models.dog.RotateUtility;
import xd.harm.utils.player.PlayerUtils;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class DragonBrain implements IMinecraft {

    Vector3d pos;
    Vector3d motion = Vector3d.ZERO;
    float direction = MathUtil.random(0, 360);
    float yaw, body, pitch;
    int speed = 50;

    final InfinityAnimation x = new InfinityAnimation();
    final InfinityAnimation y = new InfinityAnimation();
    final InfinityAnimation z = new InfinityAnimation();

    final InfinityAnimation bodyAnim = new InfinityAnimation();
    final InfinityAnimation yawAnim = new InfinityAnimation();
    final InfinityAnimation pitchAnim = new InfinityAnimation();


    public float wingFlap = 0;
    public float prevWingFlap = 0;
    public float tailSway = 0;
    public float prevTailSway = 0;

    final TimerUtility circleTimer = new TimerUtility();
    float flyHeight = 1.5f;

    @Setter
    private PlayerEntity entity;

    @Subscribe
    public void onUpdate(EventUpdate e) {
        if (entity == null) return;

        Vector3d playerPos = entity.getPositionVec();

        if (pos == null || pos.distanceTo(playerPos) > 15) {
            Vector3d startPos = new Vector3d(playerPos.x, playerPos.y + flyHeight, playerPos.z);
            pos = findSafePosition(startPos, playerPos);
            x.animate((float) pos.x, 1);
            y.animate((float) pos.y, 1);
            z.animate((float) pos.z, 1);
        }

        LivingEntity target = Harmony.getInstance().getModuleManager().getHitAura().getTarget();
        
        Vector3d newTargetPos;
        if (target != null && entity instanceof ClientPlayerEntity) {
            Vector3d targetPos = target.getPositionVec();
            double targetX = targetPos.x + 1.5; // Рядом с целью
            double targetY = targetPos.y + flyHeight;
            double targetZ = targetPos.z + 1.5;
            
            newTargetPos = new Vector3d(targetX, targetY, targetZ);
        } else {
            double targetX = playerPos.x + 2.0;
            double targetY = playerPos.y + flyHeight + Math.sin(mc.player.ticksExisted * 0.1f) * 0.3;
            double targetZ = playerPos.z;

            newTargetPos = new Vector3d(targetX, targetY, targetZ);
        }


        Vector3d safeTargetPos = findSafePosition(newTargetPos, playerPos);
        

        Vector3d diff = safeTargetPos.subtract(pos);
        double speed = target != null ? 0.2 : 0.15;
        motion = diff.scale(speed);

        Vector3d newPos = pos.add(motion);
        if (!PlayerUtils.isBlockSolid(newPos.x, newPos.y, newPos.z) &&
            !PlayerUtils.isBlockSolid(newPos.x, newPos.y + 0.5, newPos.z)) {
            pos = newPos;
        } else {
            Vector3d horizontalPos = pos.add(motion.x * 0.35, 0.0, motion.z * 0.35);
            if (!PlayerUtils.isBlockSolid(horizontalPos.x, horizontalPos.y, horizontalPos.z) &&
                !PlayerUtils.isBlockSolid(horizontalPos.x, horizontalPos.y + 0.5, horizontalPos.z)) {
                pos = horizontalPos;
            }
        }

        handleRotation();

        speed = 100;
        x.animate((float) pos.x, (int) speed);
        y.animate((float) pos.y, (int) speed);
        z.animate((float) pos.z, (int) speed);

        updateDragonAnimations();
    }

    private void updateDragonAnimations() {
        prevWingFlap = wingFlap;
        prevTailSway = tailSway;

        wingFlap += 0.6f;
        if (wingFlap > Math.PI * 2) wingFlap = 0;
        tailSway += 0.15f;
        if (tailSway > Math.PI * 2) tailSway = 0;
    }

    private void handleRotation() {
        LivingEntity target = Harmony.getInstance().getModuleManager().getHitAura().getTarget();
        
        if (target != null && entity instanceof ClientPlayerEntity) {
            Vector2f targetRotation = RotateUtility.get(pos, target.getPositionVec());
            yaw = targetRotation.x;
        } else {
            if (motion.x != 0 || motion.z != 0) {
                double angle = Math.atan2(motion.z, motion.x);
                yaw = (float) Math.toDegrees(angle) - 90;
                yaw %= 360;
                if (yaw < 0) yaw += 360;
            }
        }

        bodyAnim.animate(yaw, 80);
        yawAnim.animate(yaw, 100);
        pitchAnim.animate(0, 100);

        body = bodyAnim.get();
    }

    public float getBody() {
        return bodyAnim.get();
    }

    public float getYaw() {
        return yawAnim.get();
    }

    public float getPitch() {
        return pitchAnim.get();
    }

    public Vector3d getPos() {
        return new Vector3d(x.get(), y.get(), z.get());
    }

    public float getWingFlap() {
        return wingFlap;
    }

    public float getTailSway() {
        return tailSway;
    }

    public boolean isFlying() {
        return true;
    }

    public boolean isPlayerMoving() {
        if (entity == null) return false;
        return entity.getMotion().lengthSquared() > 0.01;
    }

    private Vector3d findSafePosition(Vector3d targetPos, Vector3d playerPos) {
        if (!PlayerUtils.isBlockSolid(targetPos.x, targetPos.y, targetPos.z) && 
            !PlayerUtils.isBlockSolid(targetPos.x, targetPos.y + 0.5, targetPos.z)) {
            return targetPos;
        }

        double[] radii = new double[]{0.5, 1.0, 1.5, 2.0};
        for (double radius : radii) {
            for (int i = 0; i < 8; i++) {
                double angle = Math.toRadians(i * 45.0);
                Vector3d testPos = new Vector3d(
                        targetPos.x + Math.cos(angle) * radius,
                        targetPos.y,
                        targetPos.z + Math.sin(angle) * radius
                );
                if (!PlayerUtils.isBlockSolid(testPos.x, testPos.y, testPos.z) &&
                    !PlayerUtils.isBlockSolid(testPos.x, testPos.y + 0.5, testPos.z)) {
                    return testPos;
                }
            }
        }

        return new Vector3d(playerPos.x, playerPos.y + flyHeight, playerPos.z);
    }
}
