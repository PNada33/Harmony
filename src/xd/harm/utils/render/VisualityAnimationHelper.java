package xd.harm.utils.render;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.BoatEntity;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.UseAction;
import net.minecraft.util.Hand;
import net.minecraft.util.HandSide;
import net.minecraft.util.math.MathHelper;
import xd.harm.modules.impl.misc.Visuality;

public final class VisualityAnimationHelper {
    private static final double MIN_FALLING_MOTION_Y = -0.08D;
    private static final float MIN_FALL_ANIMATION_DISTANCE = 5.0F;
    private static final float LANDING_TRIGGER_DISTANCE = 3.0F;
    private static final double TELEPORT_SNAP_DISTANCE_SQ = 1.0D;
    private static final float MOVEMENT_SPEED_DEADZONE = 0.0035F;
    private static final float MOVE_AMOUNT_SNAP = 0.025F;
    private static final float SPRINT_AMOUNT_SNAP = 0.03F;
    private static final float TURN_RATE_DEADZONE = 0.6F;
    private static final float TURN_VALUE_SNAP = 0.0035F;
    private static final float HEAD_YAW_SNAP = 0.01F;
    private static final int IDLE_SETTLE_TICKS = 6;
    private static final float WALK_BLEND_SNAP = 0.01F;
    private static final float SPRINT_BLEND_SNAP = 0.01F;
    private static final float IDLE_BLEND_SNAP = 0.01F;
    private static final float CROUCH_BLEND_SNAP = 0.01F;
    private static final float CROUCH_IDLE_BODY_X_ROT = 0.565F;
    private static final float CROUCH_WALK_BODY_X_ROT = 0.37975F;
    private static final float CROUCH_IDLE_BODY_Y_ROT = -0.082F;
    private static final float CROUCH_WALK_BODY_Y_ROT = -0.04354F;
    private static final float CROUCH_IDLE_BODY_Z_ROT = -0.019F;
    private static final float CROUCH_WALK_BODY_Z_ROT = -0.03832F;
    private static final float CROUCH_IDLE_BODY_Y_POINT = 2.58F;
    private static final float CROUCH_WALK_BODY_Y_POINT = 2.15F;
    private static final float CROUCH_IDLE_HEAD_Y_POINT = 3.52F;
    private static final float CROUCH_WALK_HEAD_Y_POINT = 3.05F;
    private static final float CROUCH_IDLE_BODY_Z_POINT = -0.42F;
    private static final float CROUCH_WALK_BODY_Z_POINT = -0.86F;
    private static final float CROUCH_IDLE_HEAD_Z_POINT = -0.58F;
    private static final float CROUCH_WALK_HEAD_Z_POINT = -0.98F;
    private static final float CROUCH_IDLE_RIGHT_ARM_Y = 4.34F;
    private static final float CROUCH_IDLE_LEFT_ARM_Y = 4.28F;
    private static final float CROUCH_WALK_RIGHT_ARM_Y = 3.88F;
    private static final float CROUCH_WALK_LEFT_ARM_Y = 3.72F;
    private static final float CROUCH_IDLE_RIGHT_ARM_Z = -0.54F;
    private static final float CROUCH_IDLE_LEFT_ARM_Z = -0.38F;
    private static final float CROUCH_WALK_RIGHT_ARM_Z = -0.92F;
    private static final float CROUCH_WALK_LEFT_ARM_Z = -0.76F;
    private static final float CROUCH_IDLE_LEG_Y = 12.11F;
    private static final float CROUCH_WALK_RIGHT_LEG_Y = 12.29F;
    private static final float CROUCH_WALK_LEFT_LEG_Y = 12.23F;
    private static final float CROUCH_IDLE_LEG_Z = 3.18F;
    private static final float CROUCH_WALK_RIGHT_LEG_Z = 2.64F;
    private static final float CROUCH_WALK_LEFT_LEG_Z = 2.56F;
    private static final float CROUCH_IDLE_RIGHT_ARM_X_ROT = 0.1499F;
    private static final float CROUCH_IDLE_LEFT_ARM_X_ROT = 0.1329F;
    private static final float CROUCH_WALK_RIGHT_ARM_X_ROT = 0.0887F;
    private static final float CROUCH_WALK_LEFT_ARM_X_ROT = 0.1147F;
    private static final float CROUCH_IDLE_RIGHT_ARM_Y_ROT = -0.0405F;
    private static final float CROUCH_IDLE_LEFT_ARM_Y_ROT = -0.1092F;
    private static final float CROUCH_WALK_RIGHT_ARM_Y_ROT = -0.2471F;
    private static final float CROUCH_WALK_LEFT_ARM_Y_ROT = 0.0393F;
    private static final float CROUCH_IDLE_RIGHT_ARM_Z_ROT = 0.0252F;
    private static final float CROUCH_IDLE_LEFT_ARM_Z_ROT = -0.1037F;
    private static final float CROUCH_WALK_RIGHT_ARM_Z_ROT = 0.0559F;
    private static final float CROUCH_WALK_LEFT_ARM_Z_ROT = -0.1539F;
    private static final float CROUCH_IDLE_RIGHT_LEG_X_ROT = 0.1695F;
    private static final float CROUCH_IDLE_LEFT_LEG_X_ROT = 0.1769F;
    private static final float CROUCH_WALK_RIGHT_LEG_X_ROT = 0.1213F;
    private static final float CROUCH_WALK_LEFT_LEG_X_ROT = 0.0693F;
    private static final float CROUCH_IDLE_RIGHT_LEG_Y_ROT = 0.0304F;
    private static final float CROUCH_IDLE_LEFT_LEG_Y_ROT = -0.0215F;
    private static final float CROUCH_WALK_LEG_Y_ROT = -0.0215F;
    private static final float CROUCH_IDLE_RIGHT_LEG_Z_ROT = 0.0247F;
    private static final float CROUCH_IDLE_LEFT_LEG_Z_ROT = -0.0059F;
    private static final float CROUCH_WALK_RIGHT_LEG_Z_ROT = 0.0102F;
    private static final float CROUCH_WALK_LEFT_LEG_Z_ROT = 0.0147F;
    private static final Map<Integer, PlayerAnimationState> PLAYER_STATES = new ConcurrentHashMap<>();

    private VisualityAnimationHelper() {
    }

    public static <T extends LivingEntity> void applyPlayerVisualityAnimations(
            BipedModel<T> model,
            T entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks
    ) {
        if (!(entity instanceof PlayerEntity)
                || !Visuality.isImprovedAnimationsEnabled()
                || isLocalPlayerFirstPerson(entity)) {
            return;
        }

        PlayerEntity player = (PlayerEntity) entity;
        PlayerAnimationState state = getPlayerAnimationState(player);
        tickState(player, state);
        model.bipedHead.rotateAngleZ = 0.0F;
        model.bipedBody.rotateAngleZ = 0.0F;

        if (applyLandingAnimation(model, player, state)) {
            return;
        }

        if (    applyLadderAnimation(model, player)) {
            return;
        }

        if (applyElytraAnimation(model, player, ageInTicks)) {
            return;
        }

        if (applyBoatAnimation(model, player)) {
            return;
        }

        if (applyHorseAnimation(model, player)) {
            return;
        }

        if (applyEatDrinkAnimation(model, player)) {
            return;
        }

        if (applySwimAnimation(model, player, limbSwing, limbSwingAmount, ageInTicks)) {
            return;
        }

        if (applyFallingAnimation(model, player, ageInTicks)) {
            return;
        }

        boolean crouching = applyCrouchAnimation(model, player, state, limbSwing);
        boolean sprinting = !crouching && applySprintAnimation(model, player, state, limbSwing);

        if (!crouching && !sprinting) {
            applyWalkAnimation(model, player, state, limbSwing, ageInTicks);
            applyIdleAnimation(model, player, state, ageInTicks);
        }

        applyTurnAnimation(model, player, state, crouching || sprinting);
    }

    private static boolean isLocalPlayerFirstPerson(LivingEntity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null
                && entity == minecraft.player
                && minecraft.gameSettings.getPointOfView().func_243192_a();
    }

    private static PlayerAnimationState getPlayerAnimationState(PlayerEntity player) {
        return PLAYER_STATES.computeIfAbsent(player.getEntityId(), id -> new PlayerAnimationState());
    }

    private static void tickState(PlayerEntity player, PlayerAnimationState state) {
        if (state.lastTick == player.ticksExisted) {
            return;
        }

        double deltaX = player.getPosX() - state.lastPosX;
        double deltaZ = player.getPosZ() - state.lastPosZ;
        if (!state.hasPosition || deltaX * deltaX + deltaZ * deltaZ > TELEPORT_SNAP_DISTANCE_SQ) {
            deltaX = 0.0D;
            deltaZ = 0.0D;
            state.hasPosition = true;
        }

        float horizontalSpeed = MathHelper.sqrt((float) (deltaX * deltaX + deltaZ * deltaZ));
        if (horizontalSpeed < MOVEMENT_SPEED_DEADZONE) {
            horizontalSpeed = 0.0F;
        }

        float targetMoveAmount = MathHelper.clamp(horizontalSpeed * 8.5F, 0.0F, 1.0F);
        float moveSmoothing = player.onGround ? 0.24F : 0.14F;
        state.moveAmount = approach(state.moveAmount, targetMoveAmount, moveSmoothing);
        if (targetMoveAmount == 0.0F && state.moveAmount < MOVE_AMOUNT_SNAP) {
            state.moveAmount = 0.0F;
        }

        float targetSprintAmount = player.isSprinting()
                && player.onGround
                && !player.isSneaking()
                && !player.isPassenger()
                ? state.moveAmount
                : 0.0F;
        state.sprintAmount = approach(state.sprintAmount, targetSprintAmount, player.onGround ? 0.18F : 0.1F);
        if (targetSprintAmount == 0.0F && state.sprintAmount < SPRINT_AMOUNT_SNAP) {
            state.sprintAmount = 0.0F;
        }

        state.stillTicks = state.moveAmount == 0.0F && state.sprintAmount == 0.0F
                ? Math.min(state.stillTicks + 1, 40)
                : 0;
        state.idleAmount = 1.0F - MathHelper.clamp(state.moveAmount * 1.7F, 0.0F, 1.0F);

        boolean canUseLocomotionPose = player.onGround
                && !player.isSneaking()
                && !player.isActualySwimming()
                && !player.isPassenger()
                && !player.isOnLadder();
        boolean canUseCrouchPose = player.onGround
                && player.isSneaking()
                && !player.isActualySwimming()
                && !player.isPassenger()
                && !player.isOnLadder();
        float targetCrouchBlend = canUseCrouchPose ? 1.0F : 0.0F;
        state.crouchBlend = approachTimed(state.crouchBlend, targetCrouchBlend, 0.16F, 0.12F);
        if (state.crouchBlend < CROUCH_BLEND_SNAP) {
            state.crouchBlend = 0.0F;
        }

        float targetSprintBlend = canUseLocomotionPose && player.isSprinting()
                ? MathHelper.clamp(Math.max(state.sprintAmount, state.moveAmount), 0.0F, 1.0F)
                : 0.0F;
        state.sprintBlend = approachTimed(state.sprintBlend, targetSprintBlend, 0.16F, 0.1F);
        if (state.sprintBlend < SPRINT_BLEND_SNAP) {
            state.sprintBlend = 0.0F;
        }

        float targetWalkBlend = canUseLocomotionPose && !player.isSprinting()
                ? MathHelper.clamp(state.moveAmount * 1.4F, 0.0F, 1.0F)
                : 0.0F;
        state.walkBlend = approachTimed(state.walkBlend, targetWalkBlend, 0.18F, 0.12F);
        if (state.walkBlend < WALK_BLEND_SNAP) {
            state.walkBlend = 0.0F;
        }

        boolean canUseIdlePose = canUseLocomotionPose
                && !player.isHandActive()
                && state.stillTicks >= IDLE_SETTLE_TICKS;
        float targetIdleBlend = canUseIdlePose ? state.idleAmount : 0.0F;
        state.idleBlend = approachTimed(state.idleBlend, targetIdleBlend, 0.08F, 0.06F);
        if (state.idleBlend < IDLE_BLEND_SNAP) {
            state.idleBlend = 0.0F;
        }

        double verticalMotion = player.getMotion().y;
        boolean canTrackJump = !player.onGround
                && !player.isOnLadder()
                && !player.isActualySwimming()
                && !player.isPassenger()
                && !player.isElytraFlying()
                && !player.abilities.isFlying;
        if (canTrackJump && state.wasOnGround && verticalMotion > 0.02D) {
            state.jumpActive = true;
        } else if (!canTrackJump) {
            state.jumpActive = false;
        }

        if (player.onGround) {
            boolean shouldPlayLanding = !state.wasOnGround
                    && (state.jumpActive || state.lastFallDistance >= LANDING_TRIGGER_DISTANCE);
            if (shouldPlayLanding) {
                float landingPower = state.jumpActive
                        ? Math.max(state.lastFallDistance, 1.0F)
                        : state.lastFallDistance;
                int duration = MathHelper.clamp(6 + MathHelper.floor(Math.min(landingPower, 8.0F) * 0.5F), 6, 10);
                state.landingTicks = duration;
                state.landingDuration = duration;
                state.jumpActive = false;
            } else if (state.landingTicks > 0) {
                state.landingTicks--;
            }
        } else if (state.landingTicks > 0) {
            state.landingTicks = Math.max(0, state.landingTicks - 1);
        }

        float yawDelta = MathHelper.wrapDegrees(player.renderYawOffset - state.lastBodyYaw);
        float targetTurnRate = Math.abs(yawDelta) < TURN_RATE_DEADZONE
                ? 0.0F
                : MathHelper.clamp(yawDelta, -22.0F, 22.0F);
        state.turnRate = approach(state.turnRate, targetTurnRate, player.onGround ? 0.32F : 0.16F);
        if (targetTurnRate == 0.0F && Math.abs(state.turnRate) < TURN_RATE_DEADZONE) {
            state.turnRate = 0.0F;
        }

        float movementDamping = 1.0F - MathHelper.clamp(state.moveAmount * 0.85F + state.sprintAmount * 0.25F, 0.0F, 0.9F);
        float targetTurnTwist = MathHelper.clamp((float) Math.toRadians(state.turnRate) * 1.35F, -0.42F, 0.42F) * movementDamping;
        float targetTurnLean = MathHelper.clamp((float) Math.toRadians(state.turnRate) * 0.55F, -0.14F, 0.14F) * movementDamping;
        state.turnTwist = approach(state.turnTwist, targetTurnTwist, 0.22F);
        state.turnLean = approach(state.turnLean, targetTurnLean, 0.2F);
        if (Math.abs(state.turnTwist) < TURN_VALUE_SNAP) {
            state.turnTwist = 0.0F;
        }
        if (Math.abs(state.turnLean) < TURN_VALUE_SNAP) {
            state.turnLean = 0.0F;
        }

        float headYawDelta = MathHelper.wrapDegrees(player.rotationYawHead - player.renderYawOffset);
        float targetHeadYawOffset = MathHelper.clamp((float) Math.toRadians(headYawDelta), -0.7F, 0.7F);
        state.headYawOffset = approach(state.headYawOffset, targetHeadYawOffset, 0.14F);
        if (Math.abs(state.headYawOffset) < HEAD_YAW_SNAP) {
            state.headYawOffset = 0.0F;
        }

        state.lastBodyYaw = player.renderYawOffset;
        state.lastFallDistance = player.fallDistance;
        state.wasOnGround = player.onGround;
        state.lastTick = player.ticksExisted;
        state.lastPosX = player.getPosX();
        state.lastPosZ = player.getPosZ();

        if (PLAYER_STATES.size() > 256) {
            PLAYER_STATES.clear();
        }
    }

    private static boolean applyLandingAnimation(
            BipedModel<?> model,
            PlayerEntity player,
            PlayerAnimationState state
    ) {
        if (!Visuality.isLandingAnimationEnabled()
                || state.landingTicks <= 0
                || !player.onGround
                || player.isPassenger()
                || player.isOnLadder()
                || player.isActualySwimming()
                || player.isElytraFlying()) {
            return false;
        }

        float progress = state.landingDuration <= 0
                ? 1.0F
                : 1.0F - MathHelper.clamp((state.landingTicks - 1.0F) / (float) state.landingDuration, 0.0F, 1.0F);
        float touch = easeOutCubic(MathHelper.clamp(progress / 0.22F, 0.0F, 1.0F));
        float absorb = easeInOutSine(MathHelper.clamp((progress - 0.08F) / 0.4F, 0.0F, 1.0F));
        float recoverProgress = MathHelper.clamp((progress - 0.52F) / 0.48F, 0.0F, 1.0F);
        float recover = easeOutCubic(recoverProgress);
        float touchWeight = touch * (1.0F - absorb);
        float crouchWeight = absorb * (1.0F - recover);
        float rebound = MathHelper.sin(recoverProgress * (float) Math.PI) * 0.04F;
        float bodyX = 0.04F * touchWeight + 0.42F * crouchWeight - rebound;
        float armX = -0.03F * touchWeight + 0.16F * crouchWeight - rebound * 0.2F;
        float armSpread = 0.03F * touchWeight + 0.05F * crouchWeight;
        float legX = 0.04F * touchWeight - 0.76F * crouchWeight + rebound * 0.5F;
        float legY = 0.01F * touchWeight + 0.03F * crouchWeight;
        float legZ = 0.015F * touchWeight + 0.04F * crouchWeight;
        float pelvisDrop = 0.05F * touchWeight + 0.74F * crouchWeight - rebound;
        float headDrop = 0.08F * touchWeight + 0.86F * crouchWeight - rebound * 1.1F;
        float headBob = 0.03F * touchWeight + 0.08F * crouchWeight - rebound * 0.35F;

        model.bipedBody.rotateAngleX = MathHelper.lerp(1.0F, model.bipedBody.rotateAngleX, bodyX);
        model.bipedBody.rotateAngleY = MathHelper.lerp(1.0F, model.bipedBody.rotateAngleY, state.turnTwist * 0.05F);
        model.bipedRightArm.rotateAngleX = MathHelper.lerp(1.0F, model.bipedRightArm.rotateAngleX, armX);
        model.bipedLeftArm.rotateAngleX = MathHelper.lerp(1.0F, model.bipedLeftArm.rotateAngleX, armX);
        model.bipedRightArm.rotateAngleZ = MathHelper.lerp(1.0F, model.bipedRightArm.rotateAngleZ, armSpread);
        model.bipedLeftArm.rotateAngleZ = MathHelper.lerp(1.0F, model.bipedLeftArm.rotateAngleZ, -armSpread);
        model.bipedRightLeg.rotateAngleX = MathHelper.lerp(1.0F, model.bipedRightLeg.rotateAngleX, legX);
        model.bipedLeftLeg.rotateAngleX = MathHelper.lerp(1.0F, model.bipedLeftLeg.rotateAngleX, legX);
        model.bipedRightLeg.rotateAngleY = MathHelper.lerp(1.0F, model.bipedRightLeg.rotateAngleY, legY);
        model.bipedLeftLeg.rotateAngleY = MathHelper.lerp(1.0F, model.bipedLeftLeg.rotateAngleY, -legY);
        model.bipedRightLeg.rotateAngleZ = MathHelper.lerp(1.0F, model.bipedRightLeg.rotateAngleZ, legZ);
        model.bipedLeftLeg.rotateAngleZ = MathHelper.lerp(1.0F, model.bipedLeftLeg.rotateAngleZ, -legZ);
        model.bipedBody.rotationPointY = MathHelper.lerp(1.0F, model.bipedBody.rotationPointY, pelvisDrop);
        model.bipedHead.rotationPointY = MathHelper.lerp(1.0F, model.bipedHead.rotationPointY, headDrop);
        model.bipedHead.rotateAngleX = MathHelper.lerp(
                1.0F,
                model.bipedHead.rotateAngleX,
                model.bipedHead.rotateAngleX + headBob
        );
        return true;
    }

    private static boolean applyLadderAnimation(BipedModel<?> model, LivingEntity entity) {
        if (!Visuality.isLadderAnimationEnabled()
                || !entity.isOnLadder()
                || entity.onGround
                || entity.isPassenger()) {
            return false;
        }

        float rotation = -MathHelper.cos((float) (entity.getPosY() * 2.0F)) * 0.35F;
        applyArmTransforms(model, HandSide.RIGHT, -1.7F - rotation, -0.2F, 0.3F);
        applyArmTransforms(model, HandSide.LEFT, -1.7F + rotation, -0.2F, 0.3F);
        applyLegTransforms(model, true, -1.0F + rotation, -0.2F, 0.3F);
        applyLegTransforms(model, false, -1.0F - rotation, -0.2F, 0.3F);
        model.bipedBody.rotateAngleX = entity.isSneaking() ? 0.2F : 0.0F;
        return true;
    }

    private static boolean applyElytraAnimation(BipedModel<?> model, LivingEntity entity, float ageInTicks) {
        if (!Visuality.isElytraAnimationEnabled() || !entity.isElytraFlying()) {
            return false;
        }

        float speed = (float) entity.getMotion().lengthSquared();
        speed /= 0.2F;
        speed = speed * speed * speed;
        if (speed < 1.0F) {
            speed = 1.0F;
        }

        float moveOut = MathHelper.clamp(0.1507964F / speed, 0.1F, 0.25F);
        model.bipedLeftArm.rotateAngleX = MathHelper.cos(ageInTicks * 0.6662F) * 0.5F / speed;
        model.bipedLeftArm.rotateAngleZ = -moveOut;
        model.bipedRightArm.rotateAngleX = MathHelper.cos(ageInTicks * 0.6662F + (float) Math.PI) * 0.5F / speed;
        model.bipedRightArm.rotateAngleZ = moveOut;
        model.bipedLeftLeg.rotateAngleX = MathHelper.cos(ageInTicks * 0.6662F + (float) Math.PI) * 0.7F / speed;
        model.bipedLeftLeg.rotateAngleZ = -moveOut;
        model.bipedRightLeg.rotateAngleX = MathHelper.cos(ageInTicks * 0.6662F) * 0.7F / speed;
        model.bipedRightLeg.rotateAngleZ = moveOut;
        return true;
    }

    private static boolean applyBoatAnimation(BipedModel<?> model, LivingEntity entity) {
        if (!Visuality.isBoatAnimationEnabled()) {
            return false;
        }

        Entity riding = entity.getRidingEntity();
        if (!(riding instanceof BoatEntity)) {
            return false;
        }

        BoatEntity boat = (BoatEntity) riding;
        if (boat.getPassengers().indexOf(entity) != 0) {
            return false;
        }

        float leftPaddle = boat.getRowingTime(0, 1.0F);
        float rightPaddle = boat.getRowingTime(1, 1.0F);
        applyArmTransforms(model, HandSide.LEFT, -1.1F - MathHelper.sin(leftPaddle) * 0.3F, 0.2F, 0.3F);
        applyArmTransforms(model, HandSide.RIGHT, -1.1F - MathHelper.sin(rightPaddle) * 0.3F, 0.2F, 0.3F);
        return true;
    }

    private static boolean applyHorseAnimation(BipedModel<?> model, LivingEntity entity) {
        if (!Visuality.isHorseAnimationEnabled()) {
            return false;
        }

        Entity riding = entity.getRidingEntity();
        if (!(riding instanceof AbstractHorseEntity)) {
            return false;
        }

        AbstractHorseEntity horse = (AbstractHorseEntity) riding;
        if (horse.getPassengers().indexOf(entity) != 0) {
            return false;
        }

        float rotation = -MathHelper.cos(horse.limbSwing * 0.3F) * 0.1F;
        applyArmTransforms(model, HandSide.LEFT, -1.1F - rotation, -0.2F, 0.3F);
        applyArmTransforms(model, HandSide.RIGHT, -1.1F - rotation, -0.2F, 0.3F);
        return true;
    }

    private static boolean applySwimAnimation(
            BipedModel<?> model,
            PlayerEntity player,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks
    ) {
        if (!Visuality.isSwimAnimationOverhaulEnabled()
                || player.isPassenger()
                || player.isOnLadder()
                || player.isElytraFlying()) {
            return false;
        }

        if (!player.isActualySwimming() && !player.isVisuallySwimming()) {
            return false;
        }

        float cycle = limbSwing * 0.45F;
        float stroke = MathHelper.sin(cycle);
        float kick = MathHelper.cos(cycle * 2.0F);
        float blend = MathHelper.clamp(Math.max(0.35F, limbSwingAmount * 1.8F), 0.35F, 1.0F);

        if (player.isVisuallySwimming() && !player.isInWater()) {
            model.bipedBody.rotateAngleX = MathHelper.lerp(blend, model.bipedBody.rotateAngleX, 1.3F);
            model.bipedRightArm.rotateAngleX = MathHelper.lerp(blend, model.bipedRightArm.rotateAngleX, -1.7F + stroke * 0.35F);
            model.bipedLeftArm.rotateAngleX = MathHelper.lerp(blend, model.bipedLeftArm.rotateAngleX, -1.7F - stroke * 0.35F);
            model.bipedRightArm.rotateAngleZ = MathHelper.lerp(blend, model.bipedRightArm.rotateAngleZ, 0.28F);
            model.bipedLeftArm.rotateAngleZ = MathHelper.lerp(blend, model.bipedLeftArm.rotateAngleZ, -0.28F);
            model.bipedRightLeg.rotateAngleX = MathHelper.lerp(blend, model.bipedRightLeg.rotateAngleX, 0.35F - stroke * 0.25F);
            model.bipedLeftLeg.rotateAngleX = MathHelper.lerp(blend, model.bipedLeftLeg.rotateAngleX, 0.35F + stroke * 0.25F);
            return true;
        }

        float wave = MathHelper.sin(ageInTicks * 0.18F) * 0.06F;
        model.bipedBody.rotateAngleX = MathHelper.lerp(blend, model.bipedBody.rotateAngleX, 0.95F + wave);
        model.bipedRightArm.rotateAngleX = MathHelper.lerp(blend, model.bipedRightArm.rotateAngleX, -2.05F + stroke * 0.55F);
        model.bipedLeftArm.rotateAngleX = MathHelper.lerp(blend, model.bipedLeftArm.rotateAngleX, -2.05F - stroke * 0.55F);
        model.bipedRightArm.rotateAngleZ = MathHelper.lerp(blend, model.bipedRightArm.rotateAngleZ, 0.1F + stroke * 0.2F);
        model.bipedLeftArm.rotateAngleZ = MathHelper.lerp(blend, model.bipedLeftArm.rotateAngleZ, -0.1F - stroke * 0.2F);
        model.bipedRightLeg.rotateAngleX = MathHelper.lerp(blend, model.bipedRightLeg.rotateAngleX, kick * 0.3F - 0.12F);
        model.bipedLeftLeg.rotateAngleX = MathHelper.lerp(blend, model.bipedLeftLeg.rotateAngleX, -kick * 0.3F - 0.12F);
        model.bipedRightLeg.rotateAngleZ = MathHelper.lerp(blend, model.bipedRightLeg.rotateAngleZ, 0.08F);
        model.bipedLeftLeg.rotateAngleZ = MathHelper.lerp(blend, model.bipedLeftLeg.rotateAngleZ, -0.08F);
        return true;
    }

    private static boolean applyEatDrinkAnimation(BipedModel<?> model, LivingEntity entity) {
        if (!Visuality.isEatDrinkAnimationEnabled() || !entity.isHandActive()) {
            return false;
        }

        UseAction action = entity.getActiveItemStack().getUseAction();
        if (action != UseAction.EAT && action != UseAction.DRINK) {
            return false;
        }

        HandSide arm = getUsedArm(entity);
        int useDuration = Math.max(1, entity.getActiveItemStack().getUseDuration());
        float usedTicks = useDuration - entity.getItemInUseCount() + 1.0F;
        float useTicks = entity.getItemInUseCount() + 1.0F;
        float blend = MathHelper.clamp(usedTicks / 6.0F, 0.0F, 1.0F);
        blend = 1.0F - (float) Math.pow(1.0F - blend, 3.0F);
        float eatDrinkPitch = -MathHelper.lerp(
                (-1.0F * (entity.rotationPitch - 90.0F) / 180.0F),
                1.0F,
                2.0F
        );
        eatDrinkPitch += MathHelper.abs(MathHelper.cos(useTicks / 5.0F * (float) Math.PI) * 0.15F);

        model.bipedBody.rotateAngleY = MathHelper.lerp(blend, model.bipedBody.rotateAngleY, model.bipedHead.rotateAngleY * 0.2F);
        float armYaw = arm == HandSide.RIGHT
                ? model.bipedHead.rotateAngleY - 0.35F
                : -model.bipedHead.rotateAngleY - 0.35F;
        float armPitch = eatDrinkPitch + model.bipedHead.rotateAngleX * 0.15F;

        if (arm == HandSide.RIGHT) {
            model.bipedRightArm.rotateAngleX = MathHelper.lerp(blend, model.bipedRightArm.rotateAngleX, armPitch);
            model.bipedRightArm.rotateAngleY = MathHelper.lerp(blend, model.bipedRightArm.rotateAngleY, armYaw);
            model.bipedRightArm.rotateAngleZ = MathHelper.lerp(blend, model.bipedRightArm.rotateAngleZ, 0.3F);
        } else {
            model.bipedLeftArm.rotateAngleX = MathHelper.lerp(blend, model.bipedLeftArm.rotateAngleX, armPitch);
            model.bipedLeftArm.rotateAngleY = MathHelper.lerp(blend, model.bipedLeftArm.rotateAngleY, -armYaw);
            model.bipedLeftArm.rotateAngleZ = MathHelper.lerp(blend, model.bipedLeftArm.rotateAngleZ, -0.3F);
        }
        return true;
    }

    private static boolean applyFallingAnimation(BipedModel<?> model, LivingEntity entity, float ageInTicks) {
        if (!Visuality.isFallingAnimationEnabled()
                || entity.onGround
                || entity.isOnLadder()
                || entity.isInWater()
                || entity.isPassenger()
                || entity.isActualySwimming()
                || entity.isElytraFlying()) {
            return false;
        }

        if (entity instanceof PlayerEntity && ((PlayerEntity) entity).abilities.isFlying) {
            return false;
        }

        double verticalMotion = entity.getMotion().y;
        if (verticalMotion >= MIN_FALLING_MOTION_Y || entity.fallDistance < MIN_FALL_ANIMATION_DISTANCE) {
            return false;
        }

        float fallSpeed = (float) Math.min(1.0D, -verticalMotion * 3.5D);
        float bodySwing = Math.min(1.0F, fallSpeed);
        float armSpread = Math.min(1.0F, fallSpeed * 2.0F) * 1.9F;
        float legSpread = 0.6F * bodySwing;

        model.bipedLeftArm.rotateAngleX = MathHelper.cos(ageInTicks * 0.6662F) * bodySwing;
        model.bipedLeftArm.rotateAngleZ = -armSpread;
        model.bipedRightArm.rotateAngleX = MathHelper.cos(ageInTicks * 0.6662F + (float) Math.PI) * bodySwing;
        model.bipedRightArm.rotateAngleZ = armSpread;
        model.bipedLeftLeg.rotateAngleX = MathHelper.cos(ageInTicks * 0.6662F + (float) Math.PI) * 1.4F * bodySwing;
        model.bipedLeftLeg.rotateAngleZ = -legSpread;
        model.bipedRightLeg.rotateAngleX = MathHelper.cos(ageInTicks * 0.6662F) * 1.4F * bodySwing;
        model.bipedRightLeg.rotateAngleZ = legSpread;
        return true;
    }

    private static boolean applyCrouchAnimation(
            BipedModel<?> model,
            PlayerEntity player,
            PlayerAnimationState state,
            float limbSwing
    ) {
        if (!Visuality.isCrouchAnimationEnabled()) {
            return false;
        }

        if (state.crouchBlend <= 0.0F
                && (!player.isSneaking()
                || !player.onGround
                || player.isActualySwimming()
                || player.isPassenger()
                || player.isOnLadder())) {
            return false;
        }

        float crouchBlend = easeInOutSine(state.crouchBlend);
        float crouchMove = easeInOutSine(MathHelper.clamp(state.moveAmount * 2.6F, 0.0F, 1.0F));
        float idleWeight = 1.0F - crouchMove;
        float cycle = limbSwing * 1.18F;
        float strideSin = MathHelper.sin(cycle);
        float strideCos = MathHelper.cos(cycle);
        float idleTime = player.ticksExisted * 0.08F;
        float idleSin = MathHelper.sin(idleTime);
        float idleCos = MathHelper.cos(idleTime);

        float bodyYPoint = MathHelper.lerp(crouchMove, CROUCH_IDLE_BODY_Y_POINT, CROUCH_WALK_BODY_Y_POINT);
        float headYPoint = MathHelper.lerp(crouchMove, CROUCH_IDLE_HEAD_Y_POINT, CROUCH_WALK_HEAD_Y_POINT);
        float bodyZPoint = MathHelper.lerp(crouchMove, CROUCH_IDLE_BODY_Z_POINT, CROUCH_WALK_BODY_Z_POINT);
        float headZPoint = MathHelper.lerp(crouchMove, CROUCH_IDLE_HEAD_Z_POINT, CROUCH_WALK_HEAD_Z_POINT);
        float rightArmYPoint = MathHelper.lerp(crouchMove, CROUCH_IDLE_RIGHT_ARM_Y, CROUCH_WALK_RIGHT_ARM_Y);
        float leftArmYPoint = MathHelper.lerp(crouchMove, CROUCH_IDLE_LEFT_ARM_Y, CROUCH_WALK_LEFT_ARM_Y);
        float rightArmZPoint = MathHelper.lerp(crouchMove, CROUCH_IDLE_RIGHT_ARM_Z, CROUCH_WALK_RIGHT_ARM_Z);
        float leftArmZPoint = MathHelper.lerp(crouchMove, CROUCH_IDLE_LEFT_ARM_Z, CROUCH_WALK_LEFT_ARM_Z);
        float rightLegYPoint = MathHelper.lerp(crouchMove, CROUCH_IDLE_LEG_Y, CROUCH_WALK_RIGHT_LEG_Y);
        float leftLegYPoint = MathHelper.lerp(crouchMove, CROUCH_IDLE_LEG_Y, CROUCH_WALK_LEFT_LEG_Y);
        float rightLegZPoint = MathHelper.lerp(crouchMove, CROUCH_IDLE_LEG_Z, CROUCH_WALK_RIGHT_LEG_Z);
        float leftLegZPoint = MathHelper.lerp(crouchMove, CROUCH_IDLE_LEG_Z, CROUCH_WALK_LEFT_LEG_Z);

        model.bipedHead.rotationPointY = MathHelper.lerp(crouchBlend, model.bipedHead.rotationPointY, headYPoint);
        model.bipedHead.rotationPointZ = MathHelper.lerp(crouchBlend, model.bipedHead.rotationPointZ, headZPoint);
        model.bipedBody.rotationPointY = MathHelper.lerp(crouchBlend, model.bipedBody.rotationPointY, bodyYPoint);
        model.bipedBody.rotationPointZ = MathHelper.lerp(crouchBlend, model.bipedBody.rotationPointZ, bodyZPoint);
        model.bipedRightArm.rotationPointY = MathHelper.lerp(crouchBlend, model.bipedRightArm.rotationPointY, rightArmYPoint);
        model.bipedLeftArm.rotationPointY = MathHelper.lerp(crouchBlend, model.bipedLeftArm.rotationPointY, leftArmYPoint);
        model.bipedRightArm.rotationPointZ = MathHelper.lerp(crouchBlend, model.bipedRightArm.rotationPointZ, rightArmZPoint);
        model.bipedLeftArm.rotationPointZ = MathHelper.lerp(crouchBlend, model.bipedLeftArm.rotationPointZ, leftArmZPoint);
        model.bipedRightLeg.rotationPointY = MathHelper.lerp(crouchBlend, model.bipedRightLeg.rotationPointY, rightLegYPoint);
        model.bipedLeftLeg.rotationPointY = MathHelper.lerp(crouchBlend, model.bipedLeftLeg.rotationPointY, leftLegYPoint);
        model.bipedRightLeg.rotationPointZ = MathHelper.lerp(crouchBlend, model.bipedRightLeg.rotationPointZ, rightLegZPoint);
        model.bipedLeftLeg.rotationPointZ = MathHelper.lerp(crouchBlend, model.bipedLeftLeg.rotationPointZ, leftLegZPoint);

        float bodyX = MathHelper.lerp(crouchMove, CROUCH_IDLE_BODY_X_ROT, CROUCH_WALK_BODY_X_ROT)
                + idleCos * 0.012F * idleWeight
                + strideSin * 0.046F * crouchMove;
        float bodyY = MathHelper.lerp(crouchMove, CROUCH_IDLE_BODY_Y_ROT, CROUCH_WALK_BODY_Y_ROT)
                + idleSin * 0.024F * idleWeight
                - strideCos * 0.055F * crouchMove
                + state.turnTwist * 0.12F;
        float bodyZ = MathHelper.lerp(crouchMove, CROUCH_IDLE_BODY_Z_ROT, CROUCH_WALK_BODY_Z_ROT)
                + idleCos * 0.008F * idleWeight
                - strideSin * 0.013F * crouchMove;
        float headExtraX = 0.008F * idleWeight + strideSin * 0.008F * crouchMove;

        float rightArmX = MathHelper.lerp(crouchMove, CROUCH_IDLE_RIGHT_ARM_X_ROT, CROUCH_WALK_RIGHT_ARM_X_ROT)
                - strideCos * 0.44F * crouchMove
                + idleCos * 0.01F * idleWeight;
        float leftArmX = MathHelper.lerp(crouchMove, CROUCH_IDLE_LEFT_ARM_X_ROT, CROUCH_WALK_LEFT_ARM_X_ROT)
                + strideCos * 0.44F * crouchMove
                + idleCos * 0.01F * idleWeight;
        float rightArmY = MathHelper.lerp(crouchMove, CROUCH_IDLE_RIGHT_ARM_Y_ROT, CROUCH_WALK_RIGHT_ARM_Y_ROT)
                - strideSin * 0.1F * crouchMove
                - bodyY * 0.12F;
        float leftArmY = MathHelper.lerp(crouchMove, CROUCH_IDLE_LEFT_ARM_Y_ROT, CROUCH_WALK_LEFT_ARM_Y_ROT)
                + strideSin * 0.1F * crouchMove
                - bodyY * 0.12F;
        float rightArmZ = MathHelper.lerp(crouchMove, CROUCH_IDLE_RIGHT_ARM_Z_ROT, CROUCH_WALK_RIGHT_ARM_Z_ROT)
                + idleSin * 0.008F * idleWeight
                + strideSin * 0.028F * crouchMove;
        float leftArmZ = MathHelper.lerp(crouchMove, CROUCH_IDLE_LEFT_ARM_Z_ROT, CROUCH_WALK_LEFT_ARM_Z_ROT)
                - idleSin * 0.008F * idleWeight
                - strideSin * 0.028F * crouchMove;

        float rightLegX = MathHelper.lerp(crouchMove, CROUCH_IDLE_RIGHT_LEG_X_ROT, CROUCH_WALK_RIGHT_LEG_X_ROT)
                + strideCos * 0.62F * crouchMove;
        float leftLegX = MathHelper.lerp(crouchMove, CROUCH_IDLE_LEFT_LEG_X_ROT, CROUCH_WALK_LEFT_LEG_X_ROT)
                - strideCos * 0.62F * crouchMove;
        float rightLegY = MathHelper.lerp(crouchMove, CROUCH_IDLE_RIGHT_LEG_Y_ROT, CROUCH_WALK_LEG_Y_ROT) - bodyY * 0.08F;
        float leftLegY = MathHelper.lerp(crouchMove, CROUCH_IDLE_LEFT_LEG_Y_ROT, CROUCH_WALK_LEG_Y_ROT) + bodyY * 0.08F;
        float rightLegZ = MathHelper.lerp(crouchMove, CROUCH_IDLE_RIGHT_LEG_Z_ROT, CROUCH_WALK_RIGHT_LEG_Z_ROT)
                + strideSin * 0.024F * crouchMove;
        float leftLegZ = MathHelper.lerp(crouchMove, CROUCH_IDLE_LEFT_LEG_Z_ROT, CROUCH_WALK_LEFT_LEG_Z_ROT)
                - strideSin * 0.024F * crouchMove;

        model.bipedBody.rotateAngleX = MathHelper.lerp(crouchBlend, model.bipedBody.rotateAngleX, bodyX);
        model.bipedBody.rotateAngleY = MathHelper.lerp(crouchBlend, model.bipedBody.rotateAngleY, bodyY);
        model.bipedBody.rotateAngleZ = MathHelper.lerp(crouchBlend, model.bipedBody.rotateAngleZ, bodyZ);
        model.bipedHead.rotateAngleX += headExtraX * crouchBlend;
        model.bipedRightArm.rotateAngleX = MathHelper.lerp(crouchBlend, model.bipedRightArm.rotateAngleX, rightArmX);
        model.bipedLeftArm.rotateAngleX = MathHelper.lerp(crouchBlend, model.bipedLeftArm.rotateAngleX, leftArmX);
        model.bipedRightArm.rotateAngleY = MathHelper.lerp(crouchBlend, model.bipedRightArm.rotateAngleY, rightArmY);
        model.bipedLeftArm.rotateAngleY = MathHelper.lerp(crouchBlend, model.bipedLeftArm.rotateAngleY, leftArmY);
        model.bipedRightArm.rotateAngleZ = MathHelper.lerp(crouchBlend, model.bipedRightArm.rotateAngleZ, rightArmZ);
        model.bipedLeftArm.rotateAngleZ = MathHelper.lerp(crouchBlend, model.bipedLeftArm.rotateAngleZ, leftArmZ);
        model.bipedRightLeg.rotateAngleX = MathHelper.lerp(crouchBlend, model.bipedRightLeg.rotateAngleX, rightLegX);
        model.bipedLeftLeg.rotateAngleX = MathHelper.lerp(crouchBlend, model.bipedLeftLeg.rotateAngleX, leftLegX);
        model.bipedRightLeg.rotateAngleY = MathHelper.lerp(crouchBlend, model.bipedRightLeg.rotateAngleY, rightLegY);
        model.bipedLeftLeg.rotateAngleY = MathHelper.lerp(crouchBlend, model.bipedLeftLeg.rotateAngleY, leftLegY);
        model.bipedRightLeg.rotateAngleZ = MathHelper.lerp(crouchBlend, model.bipedRightLeg.rotateAngleZ, rightLegZ);
        model.bipedLeftLeg.rotateAngleZ = MathHelper.lerp(crouchBlend, model.bipedLeftLeg.rotateAngleZ, leftLegZ);
        return true;
    }

    private static boolean applySprintAnimation(
            BipedModel<?> model,
            PlayerEntity player,
            PlayerAnimationState state,
            float limbSwing
    ) {
        if (!Visuality.isSprintAnimationEnabled()
                || !player.onGround
                || player.isSneaking()
                || player.isActualySwimming()
                || player.isPassenger()
                || player.isOnLadder()) {
            return false;
        }

        float speed = MathHelper.clamp(Math.max(state.sprintAmount, state.moveAmount * 0.82F), 0.0F, 1.0F);
        float poseWeight = state.sprintBlend;
        if (speed < 0.04F && poseWeight < 0.04F) {
            return false;
        }

        float cycle = limbSwing * 0.8F;
        float armSwing = MathHelper.cos(cycle);
        float torsoTwist = MathHelper.sin(cycle) * 0.05F * speed + state.turnTwist * 0.25F;
        float blend = easeInOutSine(poseWeight);
        float rightArmX = -0.35F - armSwing * 0.95F * speed;
        float leftArmX = -0.35F + armSwing * 0.95F * speed;
        float rightLegX = MathHelper.cos(cycle) * 1.45F * speed;
        float leftLegX = MathHelper.cos(cycle + (float) Math.PI) * 1.45F * speed;

        model.bipedBody.rotateAngleX = MathHelper.lerp(blend, model.bipedBody.rotateAngleX, 0.24F);
        model.bipedBody.rotateAngleY = MathHelper.lerp(blend, model.bipedBody.rotateAngleY, torsoTwist);
        model.bipedRightArm.rotateAngleX = MathHelper.lerp(blend, model.bipedRightArm.rotateAngleX, rightArmX);
        model.bipedLeftArm.rotateAngleX = MathHelper.lerp(blend, model.bipedLeftArm.rotateAngleX, leftArmX);
        model.bipedRightArm.rotateAngleY = MathHelper.lerp(blend, model.bipedRightArm.rotateAngleY, torsoTwist * 0.85F);
        model.bipedLeftArm.rotateAngleY = MathHelper.lerp(blend, model.bipedLeftArm.rotateAngleY, torsoTwist * 0.85F);
        model.bipedRightArm.rotateAngleZ = MathHelper.lerp(blend, model.bipedRightArm.rotateAngleZ, 0.12F);
        model.bipedLeftArm.rotateAngleZ = MathHelper.lerp(blend, model.bipedLeftArm.rotateAngleZ, -0.12F);
        model.bipedRightLeg.rotateAngleX = MathHelper.lerp(blend, model.bipedRightLeg.rotateAngleX, rightLegX);
        model.bipedLeftLeg.rotateAngleX = MathHelper.lerp(blend, model.bipedLeftLeg.rotateAngleX, leftLegX);
        model.bipedRightLeg.rotateAngleY = MathHelper.lerp(blend, model.bipedRightLeg.rotateAngleY, -torsoTwist * 0.45F);
        model.bipedLeftLeg.rotateAngleY = MathHelper.lerp(blend, model.bipedLeftLeg.rotateAngleY, -torsoTwist * 0.45F);
        return true;
    }

    private static void applyWalkAnimation(
            BipedModel<?> model,
            PlayerEntity player,
            PlayerAnimationState state,
            float limbSwing,
            float ageInTicks
    ) {
        if (!Visuality.isWalkAnimationEnabled()
                || !player.onGround
                || player.isSneaking()
                || player.isSprinting()
                || player.isActualySwimming()
                || player.isPassenger()
                || player.isOnLadder()) {
            return;
        }

        float amount = MathHelper.clamp(state.moveAmount, 0.0F, 1.0F);
        float poseWeight = state.walkBlend;
        if (amount < 0.03F && poseWeight < 0.03F) {
            return;
        }

        float cycle = limbSwing * 0.6662F;
        float armSwing = MathHelper.cos(cycle + (float) Math.PI);
        float legSwing = MathHelper.cos(cycle);
        float torsoTwist = MathHelper.sin(cycle) * 0.06F * amount + state.turnTwist * 0.12F;
        float shoulderLift = MathHelper.cos(ageInTicks * 0.12F) * 0.015F * amount;
        float blend = easeInOutSine(poseWeight);
        float rightArmX = -0.06F + armSwing * 0.78F * amount;
        float leftArmX = -0.06F - armSwing * 0.78F * amount;
        float rightLegX = legSwing * 1.02F * amount;
        float leftLegX = MathHelper.cos(cycle + (float) Math.PI) * 1.02F * amount;

        model.bipedBody.rotateAngleX = MathHelper.lerp(blend * 0.45F, model.bipedBody.rotateAngleX, 0.04F * amount);
        model.bipedBody.rotateAngleY = MathHelper.lerp(blend, model.bipedBody.rotateAngleY, torsoTwist);
        model.bipedRightArm.rotateAngleX = MathHelper.lerp(blend, model.bipedRightArm.rotateAngleX, rightArmX);
        model.bipedLeftArm.rotateAngleX = MathHelper.lerp(blend, model.bipedLeftArm.rotateAngleX, leftArmX);
        model.bipedRightArm.rotateAngleZ = MathHelper.lerp(blend, model.bipedRightArm.rotateAngleZ, 0.08F + shoulderLift);
        model.bipedLeftArm.rotateAngleZ = MathHelper.lerp(blend, model.bipedLeftArm.rotateAngleZ, -0.08F - shoulderLift);
        model.bipedRightLeg.rotateAngleX = MathHelper.lerp(blend, model.bipedRightLeg.rotateAngleX, rightLegX);
        model.bipedLeftLeg.rotateAngleX = MathHelper.lerp(blend, model.bipedLeftLeg.rotateAngleX, leftLegX);
        model.bipedRightLeg.rotateAngleY = MathHelper.lerp(blend, model.bipedRightLeg.rotateAngleY, -torsoTwist * 0.45F);
        model.bipedLeftLeg.rotateAngleY = MathHelper.lerp(blend, model.bipedLeftLeg.rotateAngleY, torsoTwist * 0.45F);
    }

    private static void applyIdleAnimation(
            BipedModel<?> model,
            PlayerEntity player,
            PlayerAnimationState state,
            float ageInTicks
    ) {
        if (!Visuality.isWalkAnimationEnabled()
                || !player.onGround
                || player.isPassenger()
                || player.isHandActive()
                || player.isActualySwimming()
                || player.isOnLadder()
                || state.idleBlend <= 0.0F) {
            return;
        }

        float idleTime = ageInTicks * 0.08F;
        float idleAmount = state.idleBlend;
        model.bipedBody.rotateAngleY += MathHelper.sin(idleTime) * 0.035F * idleAmount;
        model.bipedRightArm.rotateAngleZ += (0.03F + MathHelper.cos(idleTime) * 0.02F) * idleAmount;
        model.bipedLeftArm.rotateAngleZ -= (0.03F + MathHelper.sin(idleTime) * 0.02F) * idleAmount;
    }

    private static void applyTurnAnimation(
            BipedModel<?> model,
            PlayerEntity player,
            PlayerAnimationState state,
            boolean strongMovementPose
    ) {
        if (!Visuality.isWalkAnimationEnabled()
                || player.isPassenger()
                || player.isActualySwimming()
                || player.isOnLadder()) {
            return;
        }

        float headFreedom = 1.0F - MathHelper.clamp(Math.abs(state.headYawOffset) / 0.7F, 0.0F, 0.6F);
        float poseMultiplier = strongMovementPose ? 0.45F : 1.0F;
        float twist = state.turnTwist * poseMultiplier * headFreedom;
        float lean = state.turnLean * (strongMovementPose ? 0.35F : 0.7F) * headFreedom;
        if (Math.abs(twist) < 0.005F && Math.abs(lean) < 0.005F) {
            return;
        }

        model.bipedBody.rotateAngleY += twist;
        model.bipedBody.rotateAngleZ += lean;
        model.bipedRightArm.rotateAngleY += twist * 0.85F;
        model.bipedLeftArm.rotateAngleY += twist * 0.85F;
        model.bipedRightArm.rotateAngleZ += lean * 0.5F;
        model.bipedLeftArm.rotateAngleZ += lean * 0.5F;
        model.bipedRightLeg.rotateAngleY -= twist * 0.35F;
        model.bipedLeftLeg.rotateAngleY -= twist * 0.35F;
        model.bipedRightLeg.rotateAngleZ += lean * 0.12F;
        model.bipedLeftLeg.rotateAngleZ += lean * 0.12F;
    }

    private static float approach(float current, float target, float factor) {
        return current + (target - current) * factor;
    }

    private static float approachTimed(float current, float target, float inFactor, float outFactor) {
        return current + (target - current) * (target > current ? inFactor : outFactor);
    }

    private static float easeInOutSine(float value) {
        float clamped = MathHelper.clamp(value, 0.0F, 1.0F);
        return 0.5F - MathHelper.cos(clamped * (float) Math.PI) * 0.5F;
    }

    private static float easeOutCubic(float value) {
        float clamped = MathHelper.clamp(value, 0.0F, 1.0F);
        float inverted = 1.0F - clamped;
        return 1.0F - inverted * inverted * inverted;
    }

    private static HandSide getUsedArm(LivingEntity entity) {
        Hand activeHand = entity.getActiveHand();
        if (activeHand == Hand.MAIN_HAND) {
            return entity.getPrimaryHand();
        }
        return entity.getPrimaryHand().opposite();
    }

    private static void applyArmTransforms(BipedModel<?> model, HandSide side, float pitch, float yaw, float roll) {
        if (side == HandSide.RIGHT) {
            model.bipedRightArm.rotateAngleX = pitch;
            model.bipedRightArm.rotateAngleY = yaw;
            model.bipedRightArm.rotateAngleZ = roll;
            return;
        }

        model.bipedLeftArm.rotateAngleX = pitch;
        model.bipedLeftArm.rotateAngleY = -yaw;
        model.bipedLeftArm.rotateAngleZ = -roll;
    }

    private static void applyLegTransforms(BipedModel<?> model, boolean leftLeg, float pitch, float yaw, float roll) {
        if (leftLeg) {
            model.bipedLeftLeg.rotateAngleX = pitch;
            model.bipedLeftLeg.rotateAngleY = -yaw;
            model.bipedLeftLeg.rotateAngleZ = -roll;
            return;
        }

        model.bipedRightLeg.rotateAngleX = pitch;
        model.bipedRightLeg.rotateAngleY = yaw;
        model.bipedRightLeg.rotateAngleZ = roll;
    }

    private static final class PlayerAnimationState {
        private int lastTick = Integer.MIN_VALUE;
        private boolean wasOnGround = true;
        private float lastFallDistance;
        private int landingTicks;
        private int landingDuration;
        private float lastBodyYaw;
        private double lastPosX;
        private double lastPosZ;
        private boolean hasPosition;
        private float moveAmount;
        private float sprintAmount;
        private float idleAmount = 1.0F;
        private float walkBlend;
        private float sprintBlend;
        private float idleBlend;
        private float crouchBlend;
        private boolean jumpActive;
        private float turnRate;
        private float turnTwist;
        private float turnLean;
        private float headYawOffset;
        private int stillTicks;
    }
}
