package xd.harm.utils.recording;

import com.google.common.eventbus.Subscribe;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.monster.SlimeEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.registry.Registry;
import xd.harm.Harmony;
import xd.harm.config.FriendStorage;
import xd.harm.events.combat.AttackEvent;
import xd.harm.events.input.EventInput;
import xd.harm.events.input.EventMouseButtonPress;
import xd.harm.events.movement.EventRotate;
import xd.harm.events.world.EventChangeWorld;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.impl.combat.HitAura;
import xd.harm.utils.client.IMinecraft;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class RecordingManager implements IMinecraft {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final int FLUSH_EVERY_FRAMES = 20;
    private static final String CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";

    private boolean recording;
    private BufferedWriter writer;
    private File file;
    private long startTimeMs;
    private long frames;
    private int flushCounter;

    private float inputForward;
    private float inputStrafe;
    private boolean inputJump;
    private boolean inputSneak;
    private boolean inputSprint;

    private double rotateYawDelta;
    private double rotatePitchDelta;

    private final List<RotationSample> learnedSamples = new CopyOnWriteArrayList<>();
    private final List<float[]> shakePool = new ArrayList<>();
    private boolean modelLoaded;
    private String loadedFileName = "";

    private float momentumYaw = 0;
    private float momentumPitch = 0;
    private float aimOffsetY = 0;
    private float aimOffsetX = 0;
    private float targetAimOffsetY = 0;
    private float targetAimOffsetX = 0;
    private long lastOffsetChangeTime = 0;
    private long nextOffsetChangeDelay = 800;
    private int ticksSinceLastSwitch = 0;
    private float prevQueryYawToTarget = 0;
    private float prevQueryPitchToTarget = 0;
    private boolean wasOvershootingYaw = false;
    private boolean wasOvershootingPitch = false;
    private int correctionTicksYaw = 0;
    private int correctionTicksPitch = 0;

    private float prevDeltaYaw = 0;
    private float prevDeltaPitch = 0;

    private float avgSpeedSmallAngle = 2.5f;
    private float avgSpeedMedAngle = 8.0f;
    private float avgSpeedLargeAngle = 18.0f;
    private float maxRecordedDelta = 35.0f;
    private float avgShakeYaw = 0.3f;
    private float avgShakePitch = 0.2f;

    private int microCorrectionCounter = 0;
    private float lastAtkCooldown = 0;
    private boolean preSwingAdjust = false;
    private int preSwingTicks = 0;

    public RecordingManager() {
        Harmony.getInstance().getEventBus().register(this);
    }

    public synchronized boolean isRecording() {
        return recording;
    }

    public synchronized File getFile() {
        return file;
    }

    public synchronized long getFrames() {
        return frames;
    }

    public boolean isModelLoaded() {
        return modelLoaded;
    }

    public int getSampleCount() {
        return learnedSamples.size();
    }

    public String getLoadedFileName() {
        return loadedFileName;
    }

    public void resetPlaybackState() {
        momentumYaw = 0;
        momentumPitch = 0;
        aimOffsetY = 0;
        aimOffsetX = 0;
        targetAimOffsetY = 0;
        targetAimOffsetX = 0;
        lastOffsetChangeTime = System.currentTimeMillis();
        nextOffsetChangeDelay = 600 + ThreadLocalRandom.current().nextLong(800);
        ticksSinceLastSwitch = 0;
        prevQueryYawToTarget = 0;
        prevQueryPitchToTarget = 0;
        wasOvershootingYaw = false;
        wasOvershootingPitch = false;
        correctionTicksYaw = 0;
        correctionTicksPitch = 0;
        prevDeltaYaw = 0;
        prevDeltaPitch = 0;
        microCorrectionCounter = 0;
        lastAtkCooldown = 0;
        preSwingAdjust = false;
        preSwingTicks = 0;
    }

    public float getAimOffsetY() {
        long now = System.currentTimeMillis();
        if (now - lastOffsetChangeTime >= nextOffsetChangeDelay) {
            lastOffsetChangeTime = now;
            nextOffsetChangeDelay = 300 + ThreadLocalRandom.current().nextLong(1000);
            float r = ThreadLocalRandom.current().nextFloat();
            if (r < 0.35f) {
                targetAimOffsetY = -0.3f + ThreadLocalRandom.current().nextFloat() * 0.3f;
            } else if (r < 0.7f) {
                targetAimOffsetY = 0.0f + ThreadLocalRandom.current().nextFloat() * 0.4f;
            } else {
                targetAimOffsetY = -0.6f + ThreadLocalRandom.current().nextFloat() * 1.3f;
            }
            targetAimOffsetY = MathHelper.clamp(targetAimOffsetY, -0.7f, 0.7f);
            targetAimOffsetX = (ThreadLocalRandom.current().nextFloat() - 0.5f) * 0.5f;
        }
        float lerpSpeed = 0.06f + ThreadLocalRandom.current().nextFloat() * 0.06f;
        aimOffsetY += (targetAimOffsetY - aimOffsetY) * lerpSpeed;
        aimOffsetX += (targetAimOffsetX - aimOffsetX) * lerpSpeed;
        return aimOffsetY;
    }

    public float getAimOffsetX() {
        return aimOffsetX;
    }

    public float[] getRecordedShake() {
        if (shakePool.isEmpty()) {
            return new float[]{
                    (ThreadLocalRandom.current().nextFloat() - 0.5f) * avgShakeYaw * 0.4f,
                    (ThreadLocalRandom.current().nextFloat() - 0.5f) * avgShakePitch * 0.4f
            };
        }
        float[] base = shakePool.get(ThreadLocalRandom.current().nextInt(shakePool.size()));
        float varY = (ThreadLocalRandom.current().nextFloat() - 0.5f) * 0.12f;
        float varP = (ThreadLocalRandom.current().nextFloat() - 0.5f) * 0.08f;
        return new float[]{(base[0] + varY) * 0.5f, (base[1] + varP) * 0.5f};
    }

    public float getSpeedForAngle(float angle) {
        float absAngle = Math.abs(angle);
        if (absAngle < 5.0f) return avgSpeedSmallAngle;
        if (absAngle < 20.0f) {
            float t = (absAngle - 5.0f) / 15.0f;
            return avgSpeedSmallAngle + (avgSpeedMedAngle - avgSpeedSmallAngle) * t;
        }
        if (absAngle < 60.0f) {
            float t = (absAngle - 20.0f) / 40.0f;
            return avgSpeedMedAngle + (avgSpeedLargeAngle - avgSpeedMedAngle) * t;
        }
        return avgSpeedLargeAngle + (absAngle - 60.0f) * 0.35f;
    }

    public synchronized File start() throws IOException {
        if (recording) {
            return file;
        }
        if (mc.player == null || mc.world == null) {
            throw new IllegalStateException("Запись доступна только в игре");
        }

        File dir = getRecordingsDir();
        String randomName = generateRandomName(12);
        file = new File(dir, randomName + ".jsonl");
        while (file.exists()) {
            randomName = generateRandomName(12);
            file = new File(dir, randomName + ".jsonl");
        }

        writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        recording = true;
        startTimeMs = System.currentTimeMillis();
        frames = 0;
        flushCounter = 0;
        rotateYawDelta = 0;
        rotatePitchDelta = 0;

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("t", "meta");
        meta.put("v", 2);
        meta.put("startMs", startTimeMs);
        meta.put("client", Harmony.name);
        meta.put("build", Harmony.build);
        meta.put("singleplayer", mc.isIntegratedServerRunning());
        writeLine(meta);
        flush();

        return file;
    }

    public synchronized void stop() {
        if (!recording) {
            return;
        }
        File savedFile = file;
        recording = false;
        flushCounter = 0;
        try {
            flush();
        } catch (Exception ignored) {
        }
        try {
            if (writer != null) {
                writer.close();
            }
        } catch (Exception ignored) {
        } finally {
            writer = null;
        }
        if (savedFile != null && savedFile.exists()) {
            int count = loadRecordingFile(savedFile);
            if (count > 0) {
                loadedFileName = savedFile.getName();
                computeStatistics();
                print("\u00a7aРотация загружена: \u00a7f" + count + " сэмплов, " + shakePool.size() + " паттернов тряски");
                print("\u00a77Профиль скорости: малый=" + String.format("%.1f", avgSpeedSmallAngle) + " средний=" + String.format("%.1f", avgSpeedMedAngle) + " большой=" + String.format("%.1f", avgSpeedLargeAngle));
            } else {
                print("\u00a7cНе удалось извлечь сэмплы ротации из записи");
            }
        }
    }

    public void unload() {
        learnedSamples.clear();
        shakePool.clear();
        modelLoaded = false;
        loadedFileName = "";
        resetPlaybackState();
    }

    public int loadFile(String fileName) {
        File dir = getRecordingsDir();
        File f = new File(dir, fileName);
        if (!f.exists()) {
            if (!fileName.endsWith(".jsonl")) {
                f = new File(dir, fileName + ".jsonl");
            }
        }
        if (!f.exists()) {
            return -1;
        }
        learnedSamples.clear();
        shakePool.clear();
        modelLoaded = false;
        loadedFileName = "";
        resetPlaybackState();
        int count = loadRecordingFile(f);
        if (count > 0) {
            loadedFileName = f.getName();
            computeStatistics();
        }
        return count;
    }

    public int loadAllRecordings() {
        learnedSamples.clear();
        shakePool.clear();
        modelLoaded = false;
        loadedFileName = "";
        resetPlaybackState();
        File dir = getRecordingsDir();
        if (!dir.exists()) {
            return 0;
        }
        File[] files = dir.listFiles((d, n) -> n.endsWith(".jsonl"));
        if (files == null || files.length == 0) {
            return 0;
        }
        int totalSamples = 0;
        for (File f : files) {
            totalSamples += loadRecordingFile(f);
        }
        if (!learnedSamples.isEmpty()) {
            loadedFileName = "all (" + files.length + " files)";
            computeStatistics();
        }
        return totalSamples;
    }

    private void computeStatistics() {
        float sumSmall = 0, countSmall = 0;
        float sumMed = 0, countMed = 0;
        float sumLarge = 0, countLarge = 0;
        float maxDelta = 0;
        float sumShakeY = 0, sumShakeP = 0;
        int shakeCount = 0;

        for (RotationSample s : learnedSamples) {
            float absAngle = Math.abs(s.yawToTarget);
            float speed = (float) Math.sqrt(s.dYaw * s.dYaw + s.dPitch * s.dPitch);
            maxDelta = Math.max(maxDelta, Math.abs(s.dYaw));
            maxDelta = Math.max(maxDelta, Math.abs(s.dPitch));

            if (absAngle < 5.0f) {
                sumSmall += speed;
                countSmall++;
            } else if (absAngle < 20.0f) {
                sumMed += speed;
                countMed++;
            } else {
                sumLarge += speed;
                countLarge++;
            }
        }

        for (float[] sh : shakePool) {
            sumShakeY += Math.abs(sh[0]);
            sumShakeP += Math.abs(sh[1]);
            shakeCount++;
        }

        if (countSmall > 0) avgSpeedSmallAngle = sumSmall / countSmall;
        if (countMed > 0) avgSpeedMedAngle = sumMed / countMed;
        if (countLarge > 0) avgSpeedLargeAngle = sumLarge / countLarge;
        if (maxDelta > 0) maxRecordedDelta = maxDelta;
        if (shakeCount > 0) {
            avgShakeYaw = sumShakeY / shakeCount;
            avgShakePitch = sumShakeP / shakeCount;
        }

        if (avgSpeedSmallAngle < 0.5f) avgSpeedSmallAngle = 2.5f;
        if (avgSpeedMedAngle < 2.0f) avgSpeedMedAngle = 8.0f;
        if (avgSpeedLargeAngle < 5.0f) avgSpeedLargeAngle = 18.0f;
    }

    public List<String> listRecordings() {
        File dir = getRecordingsDir();
        if (!dir.exists()) return Collections.emptyList();
        File[] files = dir.listFiles((d, n) -> n.endsWith(".jsonl"));
        if (files == null) return Collections.emptyList();
        Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());
        List<String> names = new ArrayList<>();
        for (File f : files) names.add(f.getName());
        return names;
    }

    public Map<String, Object> getFileInfo(String fileName) {
        File dir = getRecordingsDir();
        File f = new File(dir, fileName);
        if (!f.exists() && !fileName.endsWith(".jsonl")) f = new File(dir, fileName + ".jsonl");
        if (!f.exists()) return Collections.emptyMap();
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("name", f.getName());
        info.put("size", f.length());
        long frameCount = 0, attackCount = 0, framesWithTarget = 0, firstMs = -1, lastMs = -1;
        boolean hasTargets = false;
        try (BufferedReader reader = Files.newBufferedReader(f.toPath(), StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                Map<String, Object> map;
                try { map = GSON.fromJson(line, new TypeToken<Map<String, Object>>() {}.getType()); } catch (Exception ex) { continue; }
                if (map == null) continue;
                String type = (String) map.get("t");
                if ("f".equals(type)) {
                    frameCount++;
                    if (map.containsKey("ms")) { long ms = ((Number) map.get("ms")).longValue(); if (firstMs == -1) firstMs = ms; lastMs = ms; }
                    if (map.containsKey("tid")) { framesWithTarget++; hasTargets = true; }
                } else if ("e".equals(type) && "attack".equals(map.get("k"))) { attackCount++; }
            }
        } catch (Exception ignored) {}
        info.put("frames", frameCount);
        info.put("attacks", attackCount);
        info.put("framesWithTarget", framesWithTarget);
        info.put("hasTargets", hasTargets);
        info.put("durationMs", firstMs >= 0 && lastMs >= 0 ? lastMs - firstMs : 0L);
        return info;
    }

    public boolean deleteRecording(String fileName) {
        File dir = getRecordingsDir();
        File f = new File(dir, fileName);
        if (!f.exists() && !fileName.endsWith(".jsonl")) f = new File(dir, fileName + ".jsonl");
        return f.exists() && f.delete();
    }

    private int loadRecordingFile(File f) {
        int count = 0;
        try (BufferedReader reader = Files.newBufferedReader(f.toPath(), StandardCharsets.UTF_8)) {
            String line;
            Map<String, Object> prevFrame = null;
            Map<String, Object> prevPrevFrame = null;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                Map<String, Object> map;
                try { map = GSON.fromJson(line, new TypeToken<Map<String, Object>>() {}.getType()); } catch (Exception ex) { continue; }
                if (map == null) continue;
                if ("f".equals(map.get("t"))) {
                    if (prevFrame != null) {
                        RotationSample sample = extractSample(prevPrevFrame, prevFrame, map);
                        if (sample != null) {
                            learnedSamples.add(sample);
                            count++;
                            if (Math.abs(sample.yawToTarget) < 8.0f && Math.abs(sample.pitchToTarget) < 8.0f) {
                                shakePool.add(new float[]{sample.dYaw, sample.dPitch});
                            }
                        }
                    }
                    prevPrevFrame = prevFrame;
                    prevFrame = map;
                }
            }
        } catch (Exception ignored) {}
        if (count > 0) modelLoaded = true;
        return count;
    }

    private RotationSample extractSample(Map<String, Object> prevPrev, Map<String, Object> prev, Map<String, Object> curr) {
        if (!curr.containsKey("yaw") || !curr.containsKey("pitch")) return null;
        if (!prev.containsKey("yaw") || !prev.containsKey("pitch")) return null;
        float prevYaw = toFloat(prev.get("yaw"));
        float prevPitch = toFloat(prev.get("pitch"));
        float currYaw = toFloat(curr.get("yaw"));
        float currPitch = toFloat(curr.get("pitch"));
        float dYaw = MathHelper.wrapDegrees(currYaw - prevYaw);
        float dPitch = MathHelper.wrapDegrees(currPitch - prevPitch);
        float prevDYaw = 0, prevDPitch = 0;
        if (prevPrev != null && prevPrev.containsKey("yaw") && prevPrev.containsKey("pitch")) {
            prevDYaw = MathHelper.wrapDegrees(prevYaw - toFloat(prevPrev.get("yaw")));
            prevDPitch = MathHelper.wrapDegrees(prevPitch - toFloat(prevPrev.get("pitch")));
        }
        boolean hasTargetCurr = curr.containsKey("tid") && curr.containsKey("tx") && curr.containsKey("tz");
        boolean hasTargetPrev = prev.containsKey("tid") && prev.containsKey("tx") && prev.containsKey("tz");
        float yawToTarget = 0, pitchToTarget = 0, dist = 3.0f;
        boolean hasTarget = false;
        Map<String, Object> tgtSource = hasTargetCurr ? curr : (hasTargetPrev ? prev : null);
        if (tgtSource != null) {
            float tx = toFloat(tgtSource.get("tx"));
            float ty = toFloat(tgtSource.get("ty"));
            float tz = toFloat(tgtSource.get("tz"));
            float px = toFloat(prev.get("x"));
            float py = toFloat(prev.get("y"));
            float pz = toFloat(prev.get("z"));
            if (tgtSource.containsKey("td")) dist = toFloat(tgtSource.get("td"));
            if (dist <= 0.01f) dist = (float) Math.sqrt((tx - px) * (tx - px) + (ty - py) * (ty - py) + (tz - pz) * (tz - pz));
            float eyeY = py + 1.62f;
            double dx = tx - px, dy = (ty + 0.9) - eyeY, dz = tz - pz;
            double horizDist = Math.sqrt(dx * dx + dz * dz);
            if (horizDist > 0.001) {
                float idealYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
                float idealPitch = (float) MathHelper.clamp(-Math.toDegrees(Math.atan2(dy, horizDist)), -90, 90);
                yawToTarget = MathHelper.wrapDegrees(idealYaw - prevYaw);
                pitchToTarget = MathHelper.wrapDegrees(idealPitch - prevPitch);
                hasTarget = true;
            }
        }
        if (!hasTarget) return null;
        RotationSample s = new RotationSample();
        s.yawToTarget = yawToTarget;
        s.pitchToTarget = pitchToTarget;
        s.distance = dist;
        s.dYaw = dYaw;
        s.dPitch = dPitch;
        s.prevDYaw = prevDYaw;
        s.prevDPitch = prevDPitch;
        s.attackCooldown = curr.containsKey("atkCd") ? toFloat(curr.get("atkCd")) : 0;
        s.onGround = curr.containsKey("onG") && Boolean.TRUE.equals(curr.get("onG"));
        return s;
    }

    public float[] queryLearnedRotation(float yawToTarget, float pitchToTarget, float distance, float atkCd) {
        ticksSinceLastSwitch++;

        boolean nearFullCooldown = atkCd > 0.85f;
        lastAtkCooldown = atkCd;

        if (nearFullCooldown && !preSwingAdjust) {
            preSwingAdjust = true;
            preSwingTicks = 2 + ThreadLocalRandom.current().nextInt(3);
        }
        if (preSwingAdjust) {
            preSwingTicks--;
            if (preSwingTicks <= 0) preSwingAdjust = false;
        }

        float absYaw = Math.abs(yawToTarget);
        float absPitch = Math.abs(pitchToTarget);

        if (learnedSamples.isEmpty()) {
            float[] fb = speedBasedRotation(yawToTarget, pitchToTarget);
            smoothOutput(fb, absYaw, absPitch);
            updateOvershoot(yawToTarget, pitchToTarget);
            return fb;
        }

        boolean yawSignChanged = absYaw > 1.5f && Math.abs(prevQueryYawToTarget) > 1.5f
                && Math.signum(yawToTarget) != Math.signum(prevQueryYawToTarget);
        boolean pitchSignChanged = absPitch > 1.5f && Math.abs(prevQueryPitchToTarget) > 1.5f
                && Math.signum(pitchToTarget) != Math.signum(prevQueryPitchToTarget);

        if (yawSignChanged && !wasOvershootingYaw) {
            wasOvershootingYaw = true;
            correctionTicksYaw = 2 + ThreadLocalRandom.current().nextInt(3);
        }
        if (pitchSignChanged && !wasOvershootingPitch) {
            wasOvershootingPitch = true;
            correctionTicksPitch = 1 + ThreadLocalRandom.current().nextInt(3);
        }
        if (correctionTicksYaw > 0) correctionTicksYaw--;
        else wasOvershootingYaw = false;
        if (correctionTicksPitch > 0) correctionTicksPitch--;
        else wasOvershootingPitch = false;

        boolean urgent = absYaw > 25.0f || absPitch > 20.0f;
        boolean medium = absYaw > 10.0f || absPitch > 8.0f;
        boolean close = absYaw < 5.0f && absPitch < 5.0f;

        List<ScoredSample> scored = new ArrayList<>();
        for (RotationSample s : learnedSamples) {
            float yDiff = Math.abs(MathHelper.wrapDegrees(s.yawToTarget - yawToTarget));
            float pDiff = Math.abs(MathHelper.wrapDegrees(s.pitchToTarget - pitchToTarget));
            float distDiff = Math.abs(s.distance - distance);
            float cdDiff = Math.abs(s.attackCooldown - atkCd);
            float momYawDiff = Math.abs(s.prevDYaw - momentumYaw);
            float momPitchDiff = Math.abs(s.prevDPitch - momentumPitch);
            boolean sameYawSign = (s.yawToTarget > 0) == (yawToTarget > 0) || absYaw < 3;
            boolean samePitchSign = (s.pitchToTarget > 0) == (pitchToTarget > 0) || absPitch < 3;
            float signPenalty = (sameYawSign ? 0 : 25) + (samePitchSign ? 0 : 15);
            float score = yDiff * 2.5f + pDiff * 2.0f + distDiff * 2.0f + cdDiff * 1.5f + momYawDiff * 1.0f + momPitchDiff * 0.7f + signPenalty;
            if (score < 350.0f) scored.add(new ScoredSample(s, score));
        }

        float dYaw, dPitch;

        if (scored.isEmpty()) {
            float[] fb = speedBasedRotation(yawToTarget, pitchToTarget);
            dYaw = fb[0];
            dPitch = fb[1];
        } else {
            scored.sort(Comparator.comparingDouble(a -> a.score));
            int poolSize = Math.min(scored.size(), 12);
            float totalWeight = 0;
            float[] weights = new float[poolSize];
            for (int i = 0; i < poolSize; i++) {
                float w = 1.0f / (scored.get(i).score + 0.3f);
                w = w * w;
                weights[i] = w;
                totalWeight += w;
            }
            float roll = ThreadLocalRandom.current().nextFloat() * totalWeight;
            float cumulative = 0;
            int pickedIndex = 0;
            for (int i = 0; i < poolSize; i++) {
                cumulative += weights[i];
                if (roll <= cumulative) {
                    pickedIndex = i;
                    break;
                }
            }
            RotationSample picked = scored.get(pickedIndex).sample;
            dYaw = picked.dYaw;
            dPitch = picked.dPitch;

            float angleRatio = 1.0f;
            if (Math.abs(picked.yawToTarget) > 0.5f) {
                angleRatio = absYaw / Math.abs(picked.yawToTarget);
                angleRatio = MathHelper.clamp(angleRatio, 0.5f, 2.2f);
            }
            dYaw *= angleRatio;

            float pitchRatio = 1.0f;
            if (Math.abs(picked.pitchToTarget) > 0.5f) {
                pitchRatio = absPitch / Math.abs(picked.pitchToTarget);
                pitchRatio = MathHelper.clamp(pitchRatio, 0.5f, 2.2f);
            }
            dPitch *= pitchRatio;
        }

        if (preSwingAdjust) {
            float focusFactor = 0.15f + ThreadLocalRandom.current().nextFloat() * 0.15f;
            dYaw += yawToTarget * focusFactor;
            dPitch += pitchToTarget * focusFactor;
        }

        if (!urgent && absYaw < 5.0f) {
            float decel = 0.55f + 0.45f * (absYaw / 5.0f);
            dYaw *= decel;
        }
        if (!urgent && absPitch < 4.0f) {
            float decel = 0.55f + 0.45f * (absPitch / 4.0f);
            dPitch *= decel;
        }

        float momentumBlend;
        if (urgent) {
            momentumBlend = 0.04f + ThreadLocalRandom.current().nextFloat() * 0.06f;
        } else if (medium) {
            momentumBlend = 0.08f + ThreadLocalRandom.current().nextFloat() * 0.10f;
        } else {
            momentumBlend = 0.15f + ThreadLocalRandom.current().nextFloat() * 0.12f;
        }
        dYaw = dYaw * (1.0f - momentumBlend) + momentumYaw * momentumBlend;
        dPitch = dPitch * (1.0f - momentumBlend) + momentumPitch * momentumBlend;

        if (wasOvershootingYaw && correctionTicksYaw > 0) {
            float corrStr = 0.18f + ThreadLocalRandom.current().nextFloat() * 0.22f;
            dYaw += yawToTarget * corrStr * 0.4f;
        }
        if (wasOvershootingPitch && correctionTicksPitch > 0) {
            float corrStr = 0.15f + ThreadLocalRandom.current().nextFloat() * 0.18f;
            dPitch += pitchToTarget * corrStr * 0.35f;
        }

        microCorrectionCounter++;
        if (close && microCorrectionCounter % (3 + ThreadLocalRandom.current().nextInt(4)) == 0) {
            float microY = yawToTarget * (0.08f + ThreadLocalRandom.current().nextFloat() * 0.12f);
            float microP = pitchToTarget * (0.06f + ThreadLocalRandom.current().nextFloat() * 0.10f);
            dYaw += microY;
            dPitch += microP;
        }

        float jitterScale;
        if (close) {
            jitterScale = 0.04f + ThreadLocalRandom.current().nextFloat() * 0.06f;
        } else {
            jitterScale = 0.02f + ThreadLocalRandom.current().nextFloat() * 0.04f;
        }
        dYaw += (ThreadLocalRandom.current().nextFloat() - 0.5f) * jitterScale;
        dPitch += (ThreadLocalRandom.current().nextFloat() - 0.5f) * jitterScale * 0.6f;

        if (absYaw > 2.0f && Math.signum(dYaw) != Math.signum(yawToTarget)) {
            if (ThreadLocalRandom.current().nextFloat() > 0.08f) {
                dYaw = Math.abs(dYaw) * Math.signum(yawToTarget);
            }
        }
        if (absPitch > 2.0f && Math.signum(dPitch) != Math.signum(pitchToTarget)) {
            if (ThreadLocalRandom.current().nextFloat() > 0.08f) {
                dPitch = Math.abs(dPitch) * Math.signum(pitchToTarget);
            }
        }

        if (absYaw > 4.0f) {
            float minYawSpeed = getSpeedForAngle(yawToTarget) * 0.3f;
            if (Math.abs(dYaw) < minYawSpeed) {
                dYaw = Math.signum(yawToTarget) * (minYawSpeed + ThreadLocalRandom.current().nextFloat() * minYawSpeed * 0.25f);
            }
        }
        if (absPitch > 4.0f) {
            float minPitchSpeed = getSpeedForAngle(pitchToTarget) * 0.25f;
            if (Math.abs(dPitch) < minPitchSpeed) {
                dPitch = Math.signum(pitchToTarget) * (minPitchSpeed + ThreadLocalRandom.current().nextFloat() * minPitchSpeed * 0.25f);
            }
        }

        float maxYawStep = Math.max(absYaw * 1.6f, maxRecordedDelta);
        float maxPitchStep = Math.max(absPitch * 1.6f, maxRecordedDelta * 0.8f);
        dYaw = MathHelper.clamp(dYaw, -maxYawStep, maxYawStep);
        dPitch = MathHelper.clamp(dPitch, -maxPitchStep, maxPitchStep);

        float maxAccelYaw, maxAccelPitch;
        if (urgent) {
            maxAccelYaw = 18.0f + ThreadLocalRandom.current().nextFloat() * 10.0f;
            maxAccelPitch = 14.0f + ThreadLocalRandom.current().nextFloat() * 8.0f;
        } else if (medium) {
            maxAccelYaw = 10.0f + ThreadLocalRandom.current().nextFloat() * 6.0f;
            maxAccelPitch = 8.0f + ThreadLocalRandom.current().nextFloat() * 5.0f;
        } else {
            maxAccelYaw = 5.5f + ThreadLocalRandom.current().nextFloat() * 3.5f;
            maxAccelPitch = 4.5f + ThreadLocalRandom.current().nextFloat() * 3.0f;
        }
        float accelYaw = dYaw - prevDeltaYaw;
        float accelPitch = dPitch - prevDeltaPitch;
        if (Math.abs(accelYaw) > maxAccelYaw) {
            dYaw = prevDeltaYaw + Math.signum(accelYaw) * maxAccelYaw;
        }
        if (Math.abs(accelPitch) > maxAccelPitch) {
            dPitch = prevDeltaPitch + Math.signum(accelPitch) * maxAccelPitch;
        }

        float emaFactor;
        if (urgent) {
            emaFactor = 0.03f + ThreadLocalRandom.current().nextFloat() * 0.05f;
        } else if (medium) {
            emaFactor = 0.08f + ThreadLocalRandom.current().nextFloat() * 0.08f;
        } else {
            emaFactor = 0.15f + ThreadLocalRandom.current().nextFloat() * 0.10f;
        }
        dYaw = prevDeltaYaw * emaFactor + dYaw * (1.0f - emaFactor);
        dPitch = prevDeltaPitch * emaFactor + dPitch * (1.0f - emaFactor);

        prevDeltaYaw = dYaw;
        prevDeltaPitch = dPitch;

        applyMomentum(new float[]{dYaw, dPitch});
        updateOvershoot(yawToTarget, pitchToTarget);

        return new float[]{dYaw, dPitch};
    }

    private void smoothOutput(float[] fb, float absYaw, float absPitch) {
        float ema;
        if (absYaw > 20.0f || absPitch > 15.0f) {
            ema = 0.05f + ThreadLocalRandom.current().nextFloat() * 0.05f;
        } else {
            ema = 0.15f + ThreadLocalRandom.current().nextFloat() * 0.10f;
        }
        fb[0] = prevDeltaYaw * ema + fb[0] * (1.0f - ema);
        fb[1] = prevDeltaPitch * ema + fb[1] * (1.0f - ema);
        prevDeltaYaw = fb[0];
        prevDeltaPitch = fb[1];
    }

    private float[] speedBasedRotation(float yawToTarget, float pitchToTarget) {
        float yawSpeed = getSpeedForAngle(yawToTarget) * (0.75f + ThreadLocalRandom.current().nextFloat() * 0.5f);
        float pitchSpeed = getSpeedForAngle(pitchToTarget) * (0.65f + ThreadLocalRandom.current().nextFloat() * 0.45f);
        float dYaw = yawToTarget * Math.min(1.0f, yawSpeed / Math.max(1.0f, Math.abs(yawToTarget)));
        float dPitch = pitchToTarget * Math.min(1.0f, pitchSpeed / Math.max(1.0f, Math.abs(pitchToTarget)));
        dYaw += (ThreadLocalRandom.current().nextFloat() - 0.5f) * 0.12f;
        dPitch += (ThreadLocalRandom.current().nextFloat() - 0.5f) * 0.08f;
        return new float[]{dYaw, dPitch};
    }

    private void applyMomentum(float[] delta) {
        float decay = 0.30f + ThreadLocalRandom.current().nextFloat() * 0.15f;
        momentumYaw = momentumYaw * decay + delta[0] * (1.0f - decay);
        momentumPitch = momentumPitch * decay + delta[1] * (1.0f - decay);
    }

    private void updateOvershoot(float yawToTarget, float pitchToTarget) {
        prevQueryYawToTarget = yawToTarget;
        prevQueryPitchToTarget = pitchToTarget;
    }

    @Subscribe
    public void onWorldChange(EventChangeWorld e) {
        stop();
    }

    @Subscribe
    public void onInput(EventInput e) {
        if (!recording) return;
        inputForward = e.getForward();
        inputStrafe = e.getStrafe();
        inputJump = e.isJump();
        inputSneak = e.isSneak();
        inputSprint = e.getSprintState();
    }

    @Subscribe
    public void onRotate(EventRotate e) {
        if (!recording) return;
        rotateYawDelta += e.getYaw();
        rotatePitchDelta += e.getPitch();
    }

    @Subscribe
    public void onAttack(AttackEvent e) {
        if (!recording) return;
        long ms = System.currentTimeMillis() - startTimeMs;
        Map<String, Object> ev = new LinkedHashMap<>();
        ev.put("t", "e");
        ev.put("ms", ms);
        ev.put("k", "attack");
        Entity entity = e.entity;
        if (entity != null) {
            ev.put("id", entity.getEntityId());
            ev.put("type", String.valueOf(Registry.ENTITY_TYPE.getKey(entity.getType())));
            ev.put("x", entity.getPosX());
            ev.put("y", entity.getPosY());
            ev.put("z", entity.getPosZ());
        }
        writeLineSafe(ev);
    }

    @Subscribe
    public void onMouse(EventMouseButtonPress e) {
        if (!recording) return;
        long ms = System.currentTimeMillis() - startTimeMs;
        Map<String, Object> ev = new LinkedHashMap<>();
        ev.put("t", "e");
        ev.put("ms", ms);
        ev.put("k", "mouse");
        ev.put("btn", e.getButton());
        writeLineSafe(ev);
    }

    @Subscribe
    public void onUpdate(EventUpdate e) {
        if (!recording) return;
        ClientPlayerEntity p = mc.player;
        if (p == null) return;
        long ms = System.currentTimeMillis() - startTimeMs;
        Vector3d motion = p.getMotion();
        Map<String, Object> frame = new LinkedHashMap<>();
        frame.put("t", "f");
        frame.put("i", frames++);
        frame.put("ms", ms);
        frame.put("x", p.getPosX());
        frame.put("y", p.getPosY());
        frame.put("z", p.getPosZ());
        frame.put("yaw", (double) p.rotationYaw);
        frame.put("pitch", (double) p.rotationPitch);
        frame.put("vx", motion.x);
        frame.put("vy", motion.y);
        frame.put("vz", motion.z);
        frame.put("onG", p.onGround);
        frame.put("hp", (double) p.getHealth());
        frame.put("hurt", p.hurtTime);
        frame.put("atkCd", (double) p.getCooledAttackStrength(0.0F));
        frame.put("fwd", (double) inputForward);
        frame.put("st", (double) inputStrafe);
        frame.put("j", inputJump);
        frame.put("sn", inputSneak);
        frame.put("spr", inputSprint);
        frame.put("dyaw", rotateYawDelta);
        frame.put("dpitch", rotatePitchDelta);
        LivingEntity nearest = findNearestTarget();
        if (nearest != null) {
            frame.put("tid", nearest.getEntityId());
            frame.put("tx", nearest.getPosX());
            frame.put("ty", nearest.getPosY());
            frame.put("tz", nearest.getPosZ());
            frame.put("td", (double) p.getDistance(nearest));
            frame.put("thp", (double) nearest.getHealth());
        }
        HitAura aura = Harmony.getInstance().getModuleManager().getHitAura();
        boolean auraOn = aura != null && aura.isState();
        frame.put("auraOn", auraOn);
        if (auraOn) {
            frame.put("aYaw", (double) HitAura.rotateVector.x);
            frame.put("aPitch", (double) HitAura.rotateVector.y);
        }
        rotateYawDelta = 0;
        rotatePitchDelta = 0;
        writeLineSafe(frame);
        flushCounter++;
        if (flushCounter >= FLUSH_EVERY_FRAMES) {
            flushCounter = 0;
            flushSafe();
        }
    }

    private LivingEntity findNearestTarget() {
        if (mc.player == null || mc.world == null) return null;
        LivingEntity best = null;
        double bestDist = 10.0;
        for (Entity entity : mc.world.getAllEntities()) {
            if (!(entity instanceof LivingEntity)) continue;
            LivingEntity living = (LivingEntity) entity;
            if (living instanceof ClientPlayerEntity) continue;
            if (!living.isAlive() || living.isInvulnerable() || living instanceof ArmorStandEntity) continue;
            if (living.ticksExisted < 2) continue;
            if (living instanceof PlayerEntity) {
                PlayerEntity pl = (PlayerEntity) living;
                if (pl.isCreative() || pl.isSpectator()) continue;
                if (pl.getName().getString().equalsIgnoreCase(mc.player.getName().getString())) continue;
            } else if (!(living instanceof MonsterEntity) && !(living instanceof SlimeEntity) && !(living instanceof AnimalEntity))
                continue;
            double d = mc.player.getDistance(living);
            if (d < bestDist) {
                bestDist = d;
                best = living;
            }
        }
        return best;
    }

    private float toFloat(Object o) {
        if (o == null) return 0;
        if (o instanceof Number) return ((Number) o).floatValue();
        try {
            return Float.parseFloat(o.toString());
        } catch (Exception e) {
            return 0;
        }
    }

    private String generateRandomName(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) sb.append(CHARS.charAt(ThreadLocalRandom.current().nextInt(CHARS.length())));
        return sb.toString();
    }

    private void writeLineSafe(Object obj) {
        try {
            writeLine(obj);
        } catch (Exception e) {
            stop();
            print("Ошибка записи: " + e.getMessage());
        }
    }

    private synchronized void writeLine(Object obj) throws IOException {
        if (!recording || writer == null) return;
        writer.write(GSON.toJson(obj));
        writer.newLine();
    }

    private synchronized void flush() throws IOException {
        if (writer != null) writer.flush();
    }

    private void flushSafe() {
        try {
            flush();
        } catch (Exception e) {
            stop();
        }
    }

    private File getRecordingsDir() {
        File dir = new File(mc.gameDir, "harmony" + File.separator + "files" + File.separator + "other" + File.separator + "recordings");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public static class RotationSample {
        float yawToTarget, pitchToTarget, distance, dYaw, dPitch, prevDYaw, prevDPitch, attackCooldown;
        boolean onGround;
    }

    private static class ScoredSample {
        RotationSample sample;
        float score;

        ScoredSample(RotationSample s, float sc) {
            this.sample = s;
            this.score = sc;
        }
    }
}
