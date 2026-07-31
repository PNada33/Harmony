package server.runtime;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import xd.harm.Harmony;
import xd.harm.config.HitAuraRotationCloud;
import xd.harm.config.rotation.HitAuraRotationProvider;
import xd.harm.modules.impl.combat.HitAura;
import xd.harm.modules.impl.movement.ElytraPredict;
import xd.harm.modules.impl.movement.ElytraTarget;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.ModeListSetting;
import xd.harm.utils.math.SensUtils;
import xd.harm.utils.recording.RecordingManager;

public class ServerHitAuraRotationProvider implements HitAuraRotationProvider {
    private static final int MORE_OPTION_RUN_PAST_INDEX = 4;
    private Method updateAttackMethod;

    private float spookyNoiseYaw = 0;
    private float spookyNoisePitch = 0;
    private float spookyExtraYaw = 0;
    private float spookyExtraPitch = 0;
    private long spookyLastNoiseUpdate = 0;
    private int spookyVisibilityCounter = 0;

    private float funTimeYawMicroShake = 0.0F;
    private float funTimePitchMicroShake = 0.0F;
    private long funTimeLastMicroShakeUpdate = System.currentTimeMillis();
    private float funTimeBaseYawSpeed = randBetween(1608.0F, 1609.0F);
    private float funTimeBasePitchSpeed = randBetween(1.3F, 2.8F);
    private boolean funTimeIsFlickPhase = false;
    private boolean funTimeIsLookingDownPhase = false;
    private long funTimeLastPhaseChange = System.currentTimeMillis();
    private long funTimeNextPhaseChangeDelay = ThreadLocalRandom.current().nextLong(50000L, 100000L);
    private long funTimeFlickStartTime = 0L;
    private int funTimeFlickDurationTicks = 0;
    private int funTimeLastSwingTicks = 0;
    private float funTimeAttackSnapTimer = 0.0F;

    private float rwSnapJitterAmount = 0.0F;

    private float trainingYawMicroJitter = 0.0F;
    private float trainingPitchMicroJitter = 0.0F;
    private long trainingLastMicroUpdate = System.currentTimeMillis();
    private long trainingLastRotateUpdate = System.currentTimeMillis();
    private long trainingLastSpeedRefresh = System.currentTimeMillis();
    private float trainingBaseYawSpeed = randBetween(18.0F, 28.0F);
    private float trainingBasePitchSpeed = randBetween(8.0F, 14.0F);
    private LivingEntity trainingTrackedTarget;
    private float trainingLockedPitch = 0.0F;
    private boolean trainingPitchLocked = false;

    private LivingEntity slotAcTrackedTarget;
    private float slotAcCurrentYaw;
    private float slotAcCurrentPitch;
    private float slotAcVelocityYaw;
    private float slotAcVelocityPitch;
    private double slotAcAimPointX;
    private double slotAcAimPointY;
    private double slotAcAimPointZ;
    private float slotAcNoiseAngle;
    private int slotAcHitPhase;
    private int slotAcHitTimer;
    private float slotAcPitchBeforeHit;
    private long slotAcFirstSeenTime;
    private int slotAcReactionMs;
    private boolean slotAcReactionComplete;
    private float slotAcLastSentYaw;
    private float slotAcLastSentPitch;
    private float slotAcElytraSpeed;
    private boolean slotAcElytraReturning;
    private float slotAcNoiseAmplitude = 1.8F;
    private float slotAcAdjYaw;
    private float slotAcAdjPitch;
    private long slotAcLastAimPointChangeTime;

    public ServerHitAuraRotationProvider() {}

    @Override
    public void onEnable(HitAura hitAura) {
        this.updateAttackMethod = null;
        this.spookyNoiseYaw = 0;
        this.spookyNoisePitch = 0;
        this.spookyExtraYaw = 0;
        this.spookyExtraPitch = 0;
        this.spookyLastNoiseUpdate = 0;
        this.spookyVisibilityCounter = 0;
        this.funTimeYawMicroShake = 0.0F;
        this.funTimePitchMicroShake = 0.0F;
        this.funTimeLastMicroShakeUpdate = System.currentTimeMillis();
        this.funTimeBaseYawSpeed = randBetween(1608.0F, 1609.0F);
        this.funTimeBasePitchSpeed = randBetween(1.3F, 2.8F);
        this.funTimeIsFlickPhase = false;
        this.funTimeIsLookingDownPhase = false;
        this.funTimeLastPhaseChange = System.currentTimeMillis();
        this.funTimeNextPhaseChangeDelay = ThreadLocalRandom.current().nextLong(50000L, 100000L);
        this.funTimeFlickStartTime = 0L;
        this.funTimeFlickDurationTicks = 0;
        this.funTimeLastSwingTicks = 0;
        this.funTimeAttackSnapTimer = 0.0F;
        this.rwSnapJitterAmount = 0.0F;
        this.trainingYawMicroJitter = 0.0F;
        this.trainingPitchMicroJitter = 0.0F;
        this.trainingLastMicroUpdate = System.currentTimeMillis();
        this.trainingLastRotateUpdate = System.currentTimeMillis();
        this.trainingLastSpeedRefresh = System.currentTimeMillis();
        this.trainingBaseYawSpeed = randBetween(18.0F, 28.0F);
        this.trainingBasePitchSpeed = randBetween(8.0F, 14.0F);
        this.trainingTrackedTarget = null;
        this.trainingLockedPitch = 0.0F;
        this.trainingPitchLocked = false;
        this.resetSlotAcState();
        if (hitAura != null && HitAuraRotationCloud.isMode((String) hitAura.getType().get(), "recording")) {
            RecordingManager rm = Harmony.getInstance().getRecordingManager();
            if (rm != null && !rm.isModelLoaded()) {
                rm.loadAllRecordings();
            }
        }
    }

    @Override
    public void onDisable(HitAura hitAura) {}

    @Override
    public boolean rotate(HitAura hitAura, LivingEntity target, boolean isAttacking) {
        if (hitAura == null || target == null) return false;
        Minecraft mc = Minecraft.getInstance();
        if (Minecraft.player == null) return false;

        String mode = (String) hitAura.getType().get();
        ElytraPredict elytraPredict = Harmony.getInstance().getModuleManager().getElytraPredict();

        if (!isSlotAcMode(mode) && Minecraft.player.isElytraFlying() && target.isElytraFlying() && elytraPredict != null && elytraPredict.isState()) {
            rotationAnglesElytra(mc, target, elytraPredict);
            return true;
        }
        if (isSlotAcMode(mode)) return this.rotateSlotAc(hitAura, mc, target, isAttacking);
        if (HitAuraRotationCloud.isMode(mode, "spooky")) return rotateSpooky(hitAura, mc, target, isAttacking);
        if (HitAuraRotationCloud.isMode(mode, "funtime")) return rotateFunTime(hitAura, mc, target, isAttacking);
        if (HitAuraRotationCloud.isMode(mode, "rilliworld")) return rotateRilliWorld(hitAura, mc, target, isAttacking);
        if (HitAuraRotationCloud.isMode(mode, "training")) return rotateTraining(hitAura, mc, target, isAttacking);
        if (HitAuraRotationCloud.isMode(mode, "grim")) return rotateGrim(hitAura, mc, target, isAttacking);
        if (HitAuraRotationCloud.isMode(mode, "recording")) return rotateRecording(hitAura, mc, target, isAttacking);
        return false;
    }

    // ===== SPOOKY =====
    private boolean rotateSpooky(HitAura hitAura, Minecraft mc, LivingEntity target, boolean isAttacking) {
        long now = System.currentTimeMillis();

        // Обновление шума (каждые 80мс, 35% шанс)
        if (now - this.spookyLastNoiseUpdate > 80) {
            if (ThreadLocalRandom.current().nextInt(100) < 35) {
                float intensity = 4.2F;
                this.spookyNoiseYaw = (ThreadLocalRandom.current().nextFloat() - 0.5F) * intensity;
                this.spookyNoisePitch = (ThreadLocalRandom.current().nextFloat() - 0.5F) * (intensity * 0.7F);

                if (ThreadLocalRandom.current().nextInt(100) < 25) {
                    this.spookyExtraYaw = (float) (Math.sin(now / 50.0) * 2.5);
                    this.spookyExtraPitch = (float) (Math.cos(now / 70.0) * 1.8);
                }
            }
            this.spookyLastNoiseUpdate = now;
        }

        // Базовые углы на цель
        Vector3d eyePos = Minecraft.player.getEyePosition(1.0F);
        Vector3d aimPos = target.getPositionVec().add(0, target.getHeight() * 0.9, 0);
        Vector3d direction = aimPos.subtract(eyePos).normalize();

        float targetYaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        float targetPitch = (float) -Math.toDegrees(Math.atan2(direction.y, Math.hypot(direction.x, direction.z)));

        // SpookyTime: шум + экстра + синусоидальные микро-коррекции
        float finalYaw = targetYaw + this.spookyNoiseYaw + this.spookyExtraYaw;
        float finalPitch = targetPitch + this.spookyNoisePitch + this.spookyExtraPitch;

        finalYaw += (float) (Math.sin(now / 80.0) * 1.8);
        finalPitch += (float) (Math.cos(now / 120.0) * 1.3);

        // Сглаживание (SMOOTH_FACTOR = 0.85)
        float currentYaw = HitAura.rotateVector.x;
        float currentPitch = HitAura.rotateVector.y;

        float newYaw = currentYaw + (finalYaw - currentYaw) * 0.85F;
        float newPitch = MathHelper.clamp(currentPitch + (finalPitch - currentPitch) * 0.85F, -90.0F, 90.0F);

        HitAura.rotateVector = new Vector2f(newYaw, newPitch);
        return true;
    }


    private boolean rotateFunTime(HitAura hitAura, Minecraft mc, LivingEntity target, boolean isAttacking) {
        long now = System.currentTimeMillis();
        if (!this.funTimeIsFlickPhase && !this.funTimeIsLookingDownPhase && now - this.funTimeLastPhaseChange >= this.funTimeNextPhaseChangeDelay) this.funTimeIsLookingDownPhase = true;
        int swingTicks = Minecraft.player.ticksSinceLastSwing;
        if (this.funTimeIsLookingDownPhase && swingTicks < this.funTimeLastSwingTicks) {
            this.funTimeIsFlickPhase = true; this.funTimeIsLookingDownPhase = false;
            this.funTimeFlickStartTime = now; this.funTimeFlickDurationTicks = ThreadLocalRandom.current().nextInt(231, 300);
            this.funTimeLastPhaseChange = now; this.funTimeNextPhaseChangeDelay = ThreadLocalRandom.current().nextLong(60000L, 180000L);
        }
        this.funTimeLastSwingTicks = swingTicks;
        if (this.funTimeIsFlickPhase && now - this.funTimeFlickStartTime >= (long) this.funTimeFlickDurationTicks * 50L) this.funTimeIsFlickPhase = false;
        Vector3d eyePos = Minecraft.player.getEyePosition(mc.getRenderPartialTicks());
        double hr = (double) target.getHeight() * 0.85 / Math.max(3.0, (double) hitAura.attackDistance());
        Vector3d aimPos = target.getPositionVec().add(0.0, MathHelper.clamp(eyePos.y - target.getPosY(), 0.0, (double) target.getHeight() * hr), 0.0);
        double lf = getLeadFactor(hitAura, mc);
        if (lf > 0.0) aimPos = aimPos.add((target.getPosX() - target.prevPosX) * lf, 0.0, (target.getPosZ() - target.prevPosZ) * lf);
        Vector3d dir = aimPos.subtract(eyePos).normalize();
        float tYaw = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        float tPitch = (float) MathHelper.clamp(-Math.toDegrees(Math.atan2(dir.y, Math.hypot(dir.x, dir.z))), -90.0, 90.0);
        float cY = HitAura.rotateVector.x, cP = HitAura.rotateVector.y;
        float ys = randBetween(0.0F, 2.0F), ps = randBetween(0.0F, 2.0F);
        boolean snap = false;
        if (isAttacking) this.funTimeAttackSnapTimer = 4.0F;
        if (this.funTimeAttackSnapTimer > 1.0F) { snap = true; --this.funTimeAttackSnapTimer; }
        if (snap) { cP = tPitch; ps = randBetween(60.0F, 80.0F); cY = tYaw; ys = randBetween(70.0F, 95.0F); }
        float t = (float) now / 1000.0F; float am = snap ? 0.3F : 1.0F;
        float mY = (float) Math.sin(t * 0.6) * 0.2F; float mP = (float) Math.cos(t * 0.8) * 0.8F * am;
        this.funTimeYawMicroShake += (mY - this.funTimeYawMicroShake) * 0.75F;
        this.funTimePitchMicroShake += (mP - this.funTimePitchMicroShake) * 0.75F;
        float dY = MathHelper.wrapDegrees(cY - HitAura.rotateVector.x);
        float dP = MathHelper.wrapDegrees((this.funTimeIsLookingDownPhase ? cP : (this.funTimeIsFlickPhase ? randBetween(-75.0F, -66.0F) : cP)) - HitAura.rotateVector.y);
        float fPS = this.funTimeIsLookingDownPhase ? ps : (this.funTimeIsFlickPhase ? 360.0F : ps);
        float rY = HitAura.rotateVector.x + dY * Math.min(1.0F, ys / Math.max(1.0F, Math.abs(dY)));
        float rP = HitAura.rotateVector.y + dP * Math.min(1.0F, fPS / Math.max(1.0F, Math.abs(dP)));
        rY = MathHelper.wrapDegrees(rY + this.funTimeYawMicroShake);
        rP = this.funTimeIsFlickPhase ? MathHelper.clamp(randBetween(-75.0F, -66.0F) + this.funTimePitchMicroShake, -90.0F, 90.0F) : MathHelper.clamp(rP + this.funTimePitchMicroShake, -90.0F, 90.0F);
        float gcd = SensUtils.getGCDValue();
        rY -= (rY - HitAura.rotateVector.x) % gcd; rP -= (rP - HitAura.rotateVector.y) % gcd;
        HitAura.rotateVector = new Vector2f(rY, rP);
        return true;
    }

    private boolean rotateRilliWorld(HitAura hitAura, Minecraft mc, LivingEntity target, boolean isAttacking) {
        if (hitAura.shouldPlayerFalling()) this.rwSnapJitterAmount = randBetween(0.05F, 0.3F);
        double radius = 0.3, hh = (double) target.getHeight() / 2.0, speed = 0.15;
        double time = (double) System.currentTimeMillis() / 1000.0, angle = time * speed * 2.0 * Math.PI;
        double aX = target.getPosX() + Math.cos(angle) * radius, aZ = target.getPosZ() + Math.sin(angle) * radius, aY = target.getPosY() + hh;
        Vector3d aimPos = new Vector3d(aX, aY, aZ);
        double yawRad = Math.toRadians(target.rotationYaw), rpD = (double) (Float) hitAura.runPastBlocks.get();
        double rpX = -Math.sin(yawRad) * rpD, rpZ = Math.cos(yawRad) * rpD;
        boolean rp = isRunPastEnabled(hitAura);
        double pS = Math.sqrt(Math.pow(Minecraft.player.getPosX() - Minecraft.player.prevPosX, 2) + Math.pow(Minecraft.player.getPosZ() - Minecraft.player.prevPosZ, 2)) * 20.0;
        double tS = Math.sqrt(Math.pow(target.getPosX() - target.prevPosX, 2) + Math.pow(target.getPosZ() - target.prevPosZ, 2)) * 20.0;
        Vector3d toT = aimPos.subtract(Minecraft.player.getEyePosition(1.0F)).add(rp && pS >= tS ? rpX : 0.0, 0.0, rp ? rpZ : 0.0);
        float dY = (float) (Math.toDegrees(Math.atan2(toT.z, toT.x)) - 90.0);
        float dP = (float) (-Math.toDegrees(Math.atan2(toT.y, Math.hypot(toT.x, toT.z))));
        AxisAlignedBB bb = target.getBoundingBox(); Vector3d eye = Minecraft.player.getEyePosition(1.0F);
        float minY = Float.POSITIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, minP2 = Float.POSITIVE_INFINITY, maxP2 = Float.NEGATIVE_INFINITY;
        for (double x : new double[]{bb.minX, bb.maxX}) for (double y : new double[]{bb.minY, bb.maxY}) for (double z : new double[]{bb.minZ, bb.maxZ}) {
            Vector3d c = new Vector3d(x, y, z).subtract(eye);
            float cy = (float) (Math.toDegrees(Math.atan2(c.z, c.x)) - 90.0), cp = (float) (-Math.toDegrees(Math.atan2(c.y, Math.hypot(c.x, c.z))));
            minY = Math.min(minY, cy); maxY = Math.max(maxY, cy); minP2 = Math.min(minP2, cp); maxP2 = Math.max(maxP2, cp);
        }
        dY = MathHelper.clamp(dY, minY, maxY); dP = MathHelper.clamp(dP, minP2, maxP2);
        float ddY = MathHelper.wrapDegrees(dY - HitAura.rotateVector.x), ddP = MathHelper.wrapDegrees(dP - HitAura.rotateVector.y);
        float yS = Math.min(Math.max(Math.abs(ddY), 1.0F), randBetween(5000.0F, 9999.0F)), pSt = Math.max(Math.abs(ddP), 1.0F);
        float rY = HitAura.rotateVector.x + (ddY > 0 ? yS : -yS);
        float rP = MathHelper.clamp(HitAura.rotateVector.y + (ddP > 0 ? pSt : -pSt), -89.0F, 89.0F);
        float gcd = SensUtils.getGCDValue();
        rY -= (rY - HitAura.rotateVector.x) % gcd; rP -= (rP - HitAura.rotateVector.y) % gcd;
        HitAura.rotateVector = new Vector2f(rY, rP);
        return true;
    }

    private boolean rotateTraining(HitAura hitAura, Minecraft mc, LivingEntity target, boolean isAttacking) {
        long now = System.currentTimeMillis();
        if (now - this.trainingLastSpeedRefresh >= 600L) { this.trainingBaseYawSpeed = randBetween(18.0F, 28.0F); this.trainingBasePitchSpeed = randBetween(8.0F, 14.0F); this.trainingLastSpeedRefresh = now; }
        Vector3d eyePos = Minecraft.player.getEyePosition(mc.getRenderPartialTicks());
        double ah = MathHelper.clamp((double) target.getHeight() * 0.62, 0.45, Math.max(0.45, (double) target.getHeight() - 0.2));
        Vector3d aimPos = target.getPositionVec().add(0.0, ah, 0.0);
        double dx = target.getPosX() - target.prevPosX, dz = target.getPosZ() - target.prevPosZ;
        double lf = getLeadFactor(hitAura, mc), lead = lf > 0.0 ? lf * 0.35 : 0.08;
        aimPos = aimPos.add(dx * lead, 0.0, dz * lead);
        Vector3d dir = aimPos.subtract(eyePos).normalize();
        float tY = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        float tP = (float) MathHelper.clamp(-Math.toDegrees(Math.atan2(dir.y, Math.hypot(dir.x, dir.z))), -90.0, 90.0);
        if (this.trainingTrackedTarget != target) { this.trainingTrackedTarget = target; this.trainingLockedPitch = tP; this.trainingPitchLocked = true; }
        float cY = HitAura.rotateVector.x, cP = HitAura.rotateVector.y;
        float yDiff = MathHelper.wrapDegrees(tY - cY);
        if (now - this.trainingLastMicroUpdate >= 120L) { this.trainingYawMicroJitter = randBetween(-0.16F, 0.16F); this.trainingPitchMicroJitter = 0.0F; this.trainingLastMicroUpdate = now; }
        float os = 0.0F;
        if (Math.abs(yDiff) > 35.0F) os = Math.signum(yDiff) * randBetween(0.35F, 1.15F);
        else if (Math.abs(yDiff) < 4.5F) os = Math.signum(yDiff) * randBetween(-0.08F, 0.18F);
        float desY = tY + this.trainingYawMicroJitter + os, desP = this.trainingLockedPitch;
        long dt2 = Math.max(1L, now - this.trainingLastRotateUpdate);
        float ySpd = (this.trainingBaseYawSpeed + (isAttacking ? randBetween(7.0F, 12.0F) : randBetween(0.0F, 3.0F))) * (float) dt2 / 50.0F;
        float pSpd = (this.trainingBasePitchSpeed + (isAttacking ? randBetween(4.0F, 8.0F) : randBetween(0.0F, 2.0F))) * (float) dt2 / 50.0F;
        float rY = approachAngle(cY, desY, ySpd), rP = MathHelper.clamp(approachAngle(cP, desP, pSpd), -90.0F, 90.0F);
        float gcd = SensUtils.getGCDValue();
        rY -= (rY - cY) % gcd; rP -= (rP - cP) % gcd;
        HitAura.rotateVector = new Vector2f(rY, rP); this.trainingLastRotateUpdate = now;
        return true;
    }

    private boolean rotateGrim(HitAura hitAura, Minecraft mc, LivingEntity target, boolean isAttacking) {
        if (isAttacking) this.invokeUpdateAttack(hitAura);
        Vector3d eyePos = Minecraft.player.getEyePosition(mc.getRenderPartialTicks());
        Vector3d aimPos = target.getPositionVec().add(0.0, MathHelper.clamp(eyePos.y - target.getPosY(), 0.0, (double) target.getHeight() / 2.0), 0.0);
        Vector3d dir = aimPos.subtract(eyePos).normalize();
        float tY = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        float tP = (float) MathHelper.clamp(-Math.toDegrees(Math.atan2(dir.y, Math.hypot(dir.x, dir.z))), -90.0, 90.0);
        float dY = MathHelper.wrapDegrees(tY - HitAura.rotateVector.x), dP = MathHelper.wrapDegrees(tP - HitAura.rotateVector.y);
        float rY = HitAura.rotateVector.x + dY, rP = MathHelper.clamp(HitAura.rotateVector.y + dP, -89.0F, 89.0F);
        float gcd = SensUtils.getGCDValue();
        rY -= (rY - HitAura.rotateVector.x) % gcd; rP -= (rP - HitAura.rotateVector.y) % gcd;
        HitAura.rotateVector = new Vector2f(rY, rP);
        return true;
    }

    private boolean rotateRecording(HitAura hitAura, Minecraft mc, LivingEntity target, boolean isAttacking) {
        RecordingManager rm = Harmony.getInstance().getRecordingManager();
        Vector3d eyePos = Minecraft.player.getEyePosition(1.0F);
        float offY = rm != null ? rm.getAimOffsetY() : 0, offX = rm != null ? rm.getAimOffsetX() : 0;
        double clY = MathHelper.clamp(eyePos.y - target.getPosY(), 0.2, (double) target.getHeight() * 0.9) + offY;
        Vector3d aimPos = target.getPositionVec().add(offX, clY, 0.0);
        double dx = target.getPosX() - target.prevPosX, dz = target.getPosZ() - target.prevPosZ;
        double lf = getLeadFactor(hitAura, mc), lead = lf > 0.0 ? lf : 1.5 + ThreadLocalRandom.current().nextDouble() * 1.5;
        aimPos = aimPos.add(dx * lead, 0.0, dz * lead);
        Vector3d dir = aimPos.subtract(eyePos).normalize();
        float tY = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        float tP = (float) MathHelper.clamp(-Math.toDegrees(Math.atan2(dir.y, Math.hypot(dir.x, dir.z))), -90.0, 90.0);
        float dYaw = MathHelper.wrapDegrees(tY - HitAura.rotateVector.x), dPitch = MathHelper.wrapDegrees(tP - HitAura.rotateVector.y);
        float dist = Minecraft.player.getDistance(target), cd = Minecraft.player.getCooledAttackStrength(0.0F);
        float sY, sP;
        if (rm != null && rm.isModelLoaded()) {
            float[] l = rm.queryLearnedRotation(dYaw, dPitch, dist, cd); sY = l[0]; sP = l[1];
            float[] sh = rm.getRecordedShake(); float m = Math.abs(dYaw) < 8.0F ? 1.0F : 0.3F; sY += sh[0] * m; sP += sh[1] * m;
        } else {
            float sp = 15.0F + ThreadLocalRandom.current().nextFloat() * 15.0F;
            sY = dYaw * Math.min(1.0F, sp / Math.max(1.0F, Math.abs(dYaw))); sP = dPitch * Math.min(1.0F, sp / Math.max(1.0F, Math.abs(dPitch)));
            sY += (ThreadLocalRandom.current().nextFloat() - 0.5F) * 0.4F; sP += (ThreadLocalRandom.current().nextFloat() - 0.5F) * 0.3F;
        }
        float rY = HitAura.rotateVector.x + sY, rP = MathHelper.clamp(HitAura.rotateVector.y + sP, -90.0F, 90.0F);
        float gcd = SensUtils.getGCDValue();
        rY -= (rY - HitAura.rotateVector.x) % gcd; rP -= (rP - HitAura.rotateVector.y) % gcd;
        HitAura.rotateVector = new Vector2f(rY, rP);
        return true;
    }

    @Override
    public void onAttack(HitAura hitAura, LivingEntity target) {
        if (hitAura == null || target == null) return;
        if (isSlotAcMode((String) hitAura.getType().get())) {
            this.slotAcTrackedTarget = target; this.slotAcHitPhase = 1; this.slotAcHitTimer = 0;
            this.slotAcPitchBeforeHit = HitAura.rotateVector.y; this.slotAcCurrentPitch = HitAura.rotateVector.y;
        }
    }

    // SlotAC stub - delegates to simple aim when not fully implemented in source
    private boolean rotateSlotAc(HitAura hitAura, Minecraft mc, LivingEntity target, boolean isAttacking) {
        Vector3d eyePos = Minecraft.player.getEyePosition(1.0F);
        Vector3d center = target.getBoundingBox().getCenter();
        Vector3d toAim = center.subtract(eyePos);
        float dY = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(toAim.z, toAim.x)) - 90.0);
        float dP = (float) (-Math.toDegrees(Math.atan2(toAim.y, Math.hypot(toAim.x, toAim.z))));
        float diffY = MathHelper.wrapDegrees(dY - HitAura.rotateVector.x);
        float diffP = dP - HitAura.rotateVector.y;
        float speed = 0.18F + (float) Math.random() * 0.04F;
        float damping = 0.34F;
        this.slotAcVelocityYaw = this.slotAcVelocityYaw * (1.0F - damping) + diffY * speed;
        this.slotAcVelocityPitch = this.slotAcVelocityPitch * (1.0F - damping) + diffP * speed * 0.87F;
        this.slotAcVelocityYaw = MathHelper.clamp(this.slotAcVelocityYaw, -30.0F, 30.0F);
        this.slotAcVelocityPitch = MathHelper.clamp(this.slotAcVelocityPitch, -20.0F, 20.0F);
        float rY = HitAura.rotateVector.x + this.slotAcVelocityYaw;
        float rP = MathHelper.clamp(HitAura.rotateVector.y + this.slotAcVelocityPitch, -89.0F, 89.0F);
        float gcd = SensUtils.getGCDValue();
        rY -= (rY - HitAura.rotateVector.x) % gcd; rP -= (rP - HitAura.rotateVector.y) % gcd;
        HitAura.rotateVector = new Vector2f(rY, rP);
        return true;
    }

    private void resetSlotAcState() {
        this.slotAcTrackedTarget = null; this.slotAcVelocityYaw = 0; this.slotAcVelocityPitch = 0;
        this.slotAcAimPointX = 0; this.slotAcAimPointY = 0; this.slotAcAimPointZ = 0;
        this.slotAcNoiseAngle = 0; this.slotAcHitPhase = 0; this.slotAcHitTimer = 0;
        this.slotAcFirstSeenTime = 0; this.slotAcReactionMs = 0; this.slotAcReactionComplete = false;
        this.slotAcElytraSpeed = 0; this.slotAcElytraReturning = false;
        this.slotAcAdjYaw = 1.0F; this.slotAcAdjPitch = 1.0F; this.slotAcLastAimPointChangeTime = 0;
        this.slotAcCurrentYaw = HitAura.rotateVector != null ? HitAura.rotateVector.x : 0;
        this.slotAcCurrentPitch = HitAura.rotateVector != null ? HitAura.rotateVector.y : 0;
        this.slotAcLastSentYaw = this.slotAcCurrentYaw; this.slotAcLastSentPitch = this.slotAcCurrentPitch;
    }

    // === Utilities ===
    private static float randBetween(float min, float max) { return min + ThreadLocalRandom.current().nextFloat() * (max - min); }
    private static boolean isSlotAcMode(String m) { if (m == null) return false; String n = m.trim().toLowerCase(Locale.ROOT).replace(" ","").replace("_","").replace("-",""); return "slotac".equals(n)||"slot".equals(n)||"sloth".equals(n)||"slothrotation".equals(n)||"слотас".equals(n); }
    private static float approachAngle(float c, float t, float s) { float d = MathHelper.wrapDegrees(t - c); return Math.abs(d) <= s ? t : c + Math.signum(d) * s; }
    private static float approach(float c, float t, float s) { return Math.abs(t - c) <= s ? t : c + Math.signum(t - c) * s; }
    private static boolean canUseElytraLead(Minecraft mc) { ElytraTarget et = Harmony.getInstance().getModuleManager().getElytraTarget(); return Minecraft.player != null && Minecraft.player.isElytraFlying() && et != null && et.isState() && ElytraTarget.shouldElytraTarget; }
    private static double getLeadFactor(HitAura h, Minecraft mc) { if (canUseElytraLead(mc)) { ElytraTarget et = Harmony.getInstance().getModuleManager().getElytraTarget(); return et != null ? (double)(Float)et.elytraForward.get() : 0.0; } return h.predict != null && Boolean.TRUE.equals(h.predict.get()) && h.predictStrength != null ? (double)(Float)h.predictStrength.get() * 10.0 : 0.0; }
    private static boolean isRunPastEnabled(HitAura h) { ModeListSetting mo = h.getMoreOptions(); if (mo == null) return false; List<?> l = (List<?>)mo.get(); if (l != null && l.size() > MORE_OPTION_RUN_PAST_INDEX) { BooleanSetting s = (BooleanSetting)l.get(MORE_OPTION_RUN_PAST_INDEX); return s != null && Boolean.TRUE.equals(s.get()); } return false; }
    private void invokeUpdateAttack(HitAura h) { try { Method m = this.updateAttackMethod; if (m == null || m.getDeclaringClass() != h.getClass()) { m = h.getClass().getDeclaredMethod("updateAttack"); m.setAccessible(true); this.updateAttackMethod = m; } m.invoke(h); } catch (Throwable ignored) {} }
    private static void rotationAnglesElytra(Minecraft mc, LivingEntity target, ElytraPredict ep) {
        double pd = ep.getDistance(target); Vector3d off = new Vector3d(0, MathHelper.clamp(target.getPosY()-(double)target.getHeight(),0,(double)(target.getHeight()/2.0F)),0);
        Vector3d aim = target.getPositionVec().add(off); Vector3d eye = Minecraft.player.getEyePosition(1.0F);
        Vector3d mot = target.getMotion().mul(pd,pd,pd); Vector3d toT = aim.subtract(eye);
        if (ep.canPredict(target)) toT = toT.add(mot);
        float y = (float)(((Math.toDegrees(Math.atan2(toT.z,toT.x))-90.0-Minecraft.player.rotationYaw)%360.0+540.0)%360.0-180.0);
        float p = (float)Math.min(90.0,-Math.toDegrees(Math.atan2(toT.y,Math.hypot(toT.x,toT.z))));
        float rY = Minecraft.player.rotationYaw+y, rP = MathHelper.clamp(p,-90.0F,90.0F);
        float gcd = SensUtils.getGCDValue(); rY -= rY%gcd; rP -= rP%gcd;
        HitAura.rotateVector = new Vector2f(rY, rP);
    }
}
