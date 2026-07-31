package xd.harm.modules.impl.misc;

import com.google.common.eventbus.Subscribe;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import xd.harm.events.movement.CameraEvent;
import xd.harm.events.movement.EventRotate;
import xd.harm.events.input.EventInput;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.utils.rotation.FreeLookHandler;

@ModuleRegister(name = "BetterMinecraft", category = Category.Misc, desc = "Улучшает игру")
public class BetterMinecraft extends Module {
    private static final float SWITCH_ANIM_SPEED = 0.14F;
    private static final float DISTANCE_SPEED = 0.07F;
    private static final float ROTATION_SMOOTH = 0.11F;
    private static final float CAMERA_DISTANCE = 4.1F;
    private static final float SNEAK_OFFSET = 0.5F;
    private static final float JUMP_MULTIPLIER = 2.0F;
    private static final float ANIM_SPEED = 0.09F;
    private static final float MAX_DELTA_SECONDS = 0.05F;

    public final BooleanSetting smoothChat = new BooleanSetting("Плавный чат", true);
    public final BooleanSetting smoothTab = new BooleanSetting("Плавный таб", true);
    public final BooleanSetting betterTab = new BooleanSetting("Улучшенный таб", true);
    public final BooleanSetting betterChat = new BooleanSetting("Улучшенный чат", true);
    public final BooleanSetting smoothCamera = new BooleanSetting("Плавная камера", true);
    public final BooleanSetting thirdPersonWASD = new BooleanSetting("WASD от 3 лица", true).setVisible(this::isSmoothCameraActive);

    private float currentDistance;
    private float prevDistance;
    private float currentYaw;
    private float prevYaw;
    private float currentPitch;
    private float prevPitch;
    private float heightOffset;
    private float prevHeightOffset;
    private boolean switchAnimating;
    private boolean wasThirdPerson;
    private boolean needsInit = true;
    private long lastUpdateTimeNanos;
    private boolean detachedCameraReady;
    private float detachedCameraYaw;
    private float detachedCameraPitch;

    public BetterMinecraft() {
        addSettings(betterTab, betterChat, smoothChat, smoothCamera, thirdPersonWASD);
    }

    public boolean isSmoothCameraActive() {
        return isState() && smoothCamera.get();
    }

    private boolean isThirdPersonView() {
        return mc.player != null && !mc.gameSettings.getPointOfView().func_243192_a();
    }

    private boolean shouldUseDetachedThirdPersonCamera() {
        return isSmoothCameraActive() && thirdPersonWASD.get() && isThirdPersonView() && !FreeLookHandler.isActive();
    }

    public void updateSmoothCameraState(Entity entity, boolean thirdPerson, float yaw, float pitch) {
        if (!isSmoothCameraActive() || entity == null) {
            if (!thirdPerson) {
                needsInit = true;
                wasThirdPerson = false;
                switchAnimating = false;
                lastUpdateTimeNanos = 0L;
            }
            return;
        }

        long now = System.nanoTime();
        float deltaSeconds;
        if (lastUpdateTimeNanos == 0L) {
            deltaSeconds = 1.0F / 60.0F;
        } else {
            deltaSeconds = MathHelper.clamp((now - lastUpdateTimeNanos) / 1_000_000_000.0F, 0.0F, MAX_DELTA_SECONDS);
        }
        lastUpdateTimeNanos = now;

        if (thirdPerson && !wasThirdPerson) {
            initSmoothCamera(true, yaw, pitch);
        }

        if (!thirdPerson && wasThirdPerson) {
            needsInit = true;
            switchAnimating = false;
        }

        wasThirdPerson = thirdPerson;
        if (!thirdPerson) {
            return;
        }

        if (needsInit) {
            initSmoothCamera(true, yaw, pitch);
            return;
        }

        prevYaw = currentYaw;
        prevPitch = currentPitch;
        prevDistance = currentDistance;
        prevHeightOffset = heightOffset;

        float rotationAlpha = toFrameAlpha(ROTATION_SMOOTH, deltaSeconds);
        currentYaw += MathHelper.wrapDegrees(yaw - currentYaw) * rotationAlpha;
        currentPitch = MathHelper.clamp(currentPitch + (pitch - currentPitch) * rotationAlpha, -90.0F, 90.0F);

        float distanceSpeed = switchAnimating ? SWITCH_ANIM_SPEED : DISTANCE_SPEED;
        currentDistance += (CAMERA_DISTANCE - currentDistance) * toFrameAlpha(distanceSpeed, deltaSeconds);
        if (switchAnimating && Math.abs(CAMERA_DISTANCE - currentDistance) <= 0.02F) {
            currentDistance = CAMERA_DISTANCE;
            switchAnimating = false;
        }

        float targetOffset = 0.0F;
        if (entity.isSneaking()) {
            targetOffset = -SNEAK_OFFSET;
        }
        if (!entity.isOnGround()) {
            targetOffset += (float) (-entity.getMotion().y * JUMP_MULTIPLIER);
        }
        heightOffset += (targetOffset - heightOffset) * toFrameAlpha(ANIM_SPEED, deltaSeconds);
    }

    public float getInterpolatedCameraYaw(float partialTicks, float fallbackYaw) {
        if (!isSmoothCameraActive() || needsInit) {
            return fallbackYaw;
        }
        return currentYaw;
    }

    public float getInterpolatedCameraPitch(float partialTicks, float fallbackPitch) {
        if (!isSmoothCameraActive() || needsInit) {
            return fallbackPitch;
        }
        return currentPitch;
    }

    public float getInterpolatedDistance(float partialTicks) {
        if (!isSmoothCameraActive() || needsInit) {
            return CAMERA_DISTANCE;
        }
        return currentDistance;
    }

    public float getInterpolatedHeightOffset(float partialTicks) {
        if (!isSmoothCameraActive() || needsInit) {
            return 0.0F;
        }
        return heightOffset;
    }

    private void initSmoothCamera(boolean animateSwitch, float yaw, float pitch) {
        currentYaw = prevYaw = yaw;
        currentPitch = prevPitch = pitch;
        currentDistance = prevDistance = animateSwitch ? 0.0F : CAMERA_DISTANCE;
        heightOffset = prevHeightOffset = 0.0F;
        switchAnimating = animateSwitch;
        needsInit = false;
        lastUpdateTimeNanos = System.nanoTime();
    }

    private float toFrameAlpha(float baseAlphaPerFrame, float deltaSeconds) {
        return 1.0F - (float) Math.pow(1.0F - baseAlphaPerFrame, deltaSeconds * 60.0F);
    }

    @Subscribe
    private void onRotate(EventRotate event) {
        if (!shouldUseDetachedThirdPersonCamera()) {
            detachedCameraReady = false;
            return;
        }

        if (!detachedCameraReady && mc.player != null) {
            detachedCameraYaw = mc.player.rotationYaw;
            detachedCameraPitch = mc.player.rotationPitch;
            detachedCameraReady = true;
        }

        detachedCameraYaw += (float) (event.getYaw() * 0.15D);
        detachedCameraPitch = MathHelper.clamp(detachedCameraPitch + (float) (event.getPitch() * 0.15D), -90.0F, 90.0F);
        event.cancel();
    }

    @Subscribe
    private void onCamera(CameraEvent event) {
        if (!shouldUseDetachedThirdPersonCamera()) {
            detachedCameraReady = false;
            return;
        }

        if (!detachedCameraReady) {
            detachedCameraYaw = event.yaw;
            detachedCameraPitch = event.pitch;
            detachedCameraReady = true;
        }

        event.yaw = detachedCameraYaw;
        event.pitch = detachedCameraPitch;
    }

    @Subscribe
    private void onInput(EventInput event) {
        if (!isSmoothCameraActive() || !thirdPersonWASD.get() || !isThirdPersonView()) {
            return;
        }

        float forward = event.getForward();
        float strafe = event.getStrafe();
        if (forward == 0.0F && strafe == 0.0F) {
            return;
        }

        float cameraYaw = getInterpolatedCameraYaw(1.0F, mc.player.rotationYaw);
        float moveYaw = calculateMovementYaw(cameraYaw, forward, strafe);
        mc.player.rotationYaw = moveYaw;
        mc.player.rotationYawHead = moveYaw;
        mc.player.renderYawOffset = moveYaw;
        event.setForward(1.0F);
        event.setStrafe(0.0F);
    }

    private float calculateMovementYaw(float baseYaw, float forward, float strafe) {
        float yaw = baseYaw;
        if (forward < 0.0F) {
            yaw += 180.0F;
        }
        float modifier = 1.0F;
        if (forward < 0.0F) {
            modifier = -0.5F;
        } else if (forward > 0.0F) {
            modifier = 0.5F;
        }
        if (strafe > 0.0F) {
            yaw -= 90.0F * modifier;
        }
        if (strafe < 0.0F) {
            yaw += 90.0F * modifier;
        }
        return yaw;
    }

    @Override
    public boolean onEnable() {
        needsInit = true;
        wasThirdPerson = false;
        switchAnimating = false;
        lastUpdateTimeNanos = 0L;
        detachedCameraReady = false;
        return super.onEnable();
    }

    @Override
    public boolean onDisable() {
        needsInit = true;
        wasThirdPerson = false;
        switchAnimating = false;
        lastUpdateTimeNanos = 0L;
        heightOffset = 0.0F;
        prevHeightOffset = 0.0F;
        detachedCameraReady = false;
        return super.onDisable();
    }
}
