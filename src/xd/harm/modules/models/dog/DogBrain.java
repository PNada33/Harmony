package xd.harm.modules.models.dog;

import com.google.common.eventbus.Subscribe;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import xd.harm.Harmony;
import xd.harm.events.world.EventUpdate;
import xd.harm.utils.animations.InfinityAnimation;
import xd.harm.utils.client.IMinecraft;
import xd.harm.utils.client.TimerUtility;
import xd.harm.utils.math.MathUtil;
import xd.harm.utils.player.PlayerUtils;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class DogBrain implements IMinecraft {

    Vector3d pos;
    Vector3d motion = Vector3d.ZERO;
    float direction = MathUtil.random(0, 360);
    float yaw, body;
    int speed = 50;

    final InfinityAnimation x = new InfinityAnimation();
    final InfinityAnimation y = new InfinityAnimation();
    final InfinityAnimation z = new InfinityAnimation();

    final InfinityAnimation bodyAnim = new InfinityAnimation();
    final InfinityAnimation yawAnim = new InfinityAnimation();
    final InfinityAnimation pitchAnim = new InfinityAnimation();

    @Getter
    boolean lay;
    final TimerUtility staying = new TimerUtility();

    public float prevLimbSwingAmount;
    public float limbSwingAmount;
    public float limbSwing;

    @Setter
    private PlayerEntity entity;

    @Subscribe
    public void onUpdate(EventUpdate e) {
        if (entity == null) return;

        Vector3d playerPos = entity.getPositionVec();

        if (pos == null || pos.distanceTo(playerPos) > 10) {
            pos = playerPos;
            x.animate((float) pos.x, 1);
            y.animate((float) pos.y, 1);
            z.animate((float) pos.z, 1);
        }

        motion = motion.add(0, -0.2f, 0);

        Vector3d newPos = pos.add(motion);

        if (PlayerUtils.isBlockSolid(newPos.x, newPos.y, newPos.z)) {
            int blockY = (int) newPos.y;
            double correctedY = blockY + 1 + 0.1;
            newPos = new Vector3d(newPos.x, correctedY, newPos.z);
            motion = new Vector3d(motion.x, 0, motion.z);
        }

        motion = new Vector3d(motion.x, 0, motion.z);

        LivingEntity target = Harmony.getInstance().getModuleManager().getHitAura().getTarget();
        if (target != null && entity instanceof ClientPlayerEntity) {
            if (PlayerUtils.isBlockSolid(newPos.x, newPos.y - 0.1f, newPos.z)) {
                motion = new Vector3d(motion.x, 0.62f, motion.z);
            }

            AxisAlignedBB box = new AxisAlignedBB(getPos().subtract(0.4, 0, 0.4), getPos().add(0.4, 0.4, 0.4));
            AxisAlignedBB targetbox = target.getBoundingBox().expand(-0.1f, 0, -0.1f);

            motion = motion.add(target.getPositionVec().subtract(newPos).normalize());

            if (box.maxX > targetbox.minX
                    && box.maxY > targetbox.minY
                    && box.maxZ > targetbox.minZ
                    && box.minX < targetbox.maxX
                    && box.minY < targetbox.maxY
                    && box.minZ < targetbox.maxZ) {
                motion = motion.mul(-1, 1, -1);
            }
        } else {
            if (newPos.distanceTo(playerPos) > 2) {
                motion = motion.add(playerPos.subtract(newPos).normalize());
            }
        }

        handleRotation();

        pos = newPos;

        if (pos.distanceTo(playerPos) < 0.1f) {
            direction = MathUtil.random(0, 360);
            double xMot = -Math.sin(Math.toRadians(direction)) * 0.1;
            double zMot = Math.cos(Math.toRadians(direction)) * 0.1;
            motion = motion.add(xMot, 0, zMot);
        }

        motion = motion.scale(0.5);

        speed = 150;
        x.animate((float) pos.x, speed);
        y.animate((float) pos.y, speed);
        z.animate((float) pos.z, speed);

        limbTick();

        if (Math.abs(pos.x - x.get()) > 0.1f || Math.abs(pos.z - z.get()) > 0.1f) {
            staying.reset();
        }

        lay = staying.hasTimeElapsed(1000);
    }

    private void handleRotation() {
        if (motion.x != 0 || motion.z != 0) {
            double angle = Math.atan2(motion.z, motion.x);
            yaw = (float) Math.toDegrees(angle) - 90;
            yaw %= 360;
            if (yaw < 0) yaw += 360;
        }

        Vector2f rotation = RotateUtility.get(pos, entity.getEyePosition(0));

        LivingEntity target = Harmony.getInstance().getModuleManager().getHitAura().getTarget();
        if (target != null && entity instanceof ClientPlayerEntity) {
            rotation = RotateUtility.get(pos, target.getPositionVec());
        }

        float gradus = lay ? 200 : 150;
        float gradus1 = lay ? 100 : 50;
        if (rotation.x - yaw < -gradus || rotation.x - yaw > gradus) {
            yaw = rotation.x;
        }

        float shortestYawPath = (float) (((((yaw - body) % 360) + 540) % 360) - 180);

        if (!lay)
            bodyAnim.animate(body + shortestYawPath, 150);
        yawAnim.animate(MathHelper.clamp(rotation.x - yaw, -gradus1, gradus1), 150);
        pitchAnim.animate(rotation.y, 150);

        body = body + shortestYawPath;
    }

    public void limbTick() {
        prevLimbSwingAmount = limbSwingAmount;
        double d0 = x.get() - pos.x;
        double d1 = 0.0D;
        double d2 = z.get() - pos.z;
        float f = MathHelper.sqrt(d0 * d0 + d1 * d1 + d2 * d2) * 4.0F;

        if (f > 1.0F) {
            f = 1.0F;
        }

        limbSwingAmount += (f - limbSwingAmount) * 0.4F;
        limbSwing += limbSwingAmount;
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
}

