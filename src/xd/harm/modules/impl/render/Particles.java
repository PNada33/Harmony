package xd.harm.modules.impl.render;

import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.CategorySetting;
import xd.harm.modules.settings.impl.ColorSetting;
import xd.harm.modules.settings.impl.ModeListSetting;
import xd.harm.modules.settings.impl.SliderSetting;
import xd.harm.utils.animations.easing.CompactAnimation;
import xd.harm.utils.animations.easing.Easing;
import xd.harm.utils.client.TimerUtility;
import xd.harm.utils.math.MathUtil;
import xd.harm.utils.player.BlockUtils;
import com.google.common.eventbus.Subscribe;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.ModeSetting;
import xd.harm.utils.render.color.ColorUtils;
import xd.harm.utils.render.GLUtility;
import xd.harm.utils.render.rect.RenderUtility;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.*;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.EnderPearlEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.ThrowableEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.world.gen.Heightmap;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import xd.harm.events.world.EventUpdate;
import java.util.concurrent.ThreadLocalRandom;

import static org.lwjgl.opengl.GL11C.GL_QUADS;
import static net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_COLOR_TEX;
import xd.harm.events.combat.AttackEvent;
import xd.harm.events.movement.EventMotion;
import xd.harm.events.movement.JumpEvent;
import xd.harm.events.render.DEngineEvent;
import xd.harm.events.render.EventDisplay;
import xd.harm.events.world.EventChangeWorld;

@ModuleRegister(name = "Particles", category = Category.Render, desc = "Партиклы при ударе")
public class Particles extends Module {
    private final ModeSetting particleType = new ModeSetting("Тип частиц", "Свечение", "Свечение", "Звёзды", "Сердце", "Молния", "Доллар", "Снежинки", "Маленькая Звезда", "Полигон", "Корона", "Рандом");
    private final List<Entity> cachedProjectiles = new ArrayList<>();

    @Subscribe
    public void onUpdate(EventUpdate e) {
        if (mc.world == null) {
            cachedProjectiles.clear();
            return;
        }
        if (!elements.getValueByName("Бросаемый предмет").get()) {
            cachedProjectiles.clear();
            return;
        }
        cachedProjectiles.clear();
        for (Entity entity : mc.world.getAllEntities()) {
            if (entity instanceof EnderPearlEntity || entity instanceof ArrowEntity || entity instanceof TridentEntity) {
                cachedProjectiles.add(entity);
            }
        }
    }

    public final ModeListSetting elements = new ModeListSetting("Триггер",
            new BooleanSetting("Удар", true),
            new BooleanSetting("Ходьба", true),
            new BooleanSetting("Бросаемый предмет", true),
            new BooleanSetting("Прыжок", true),
            new BooleanSetting("Тотем", true),
            new BooleanSetting("Бездействие", true),
            new BooleanSetting("Курсор", false)
    );

    private final SliderSetting speed = new SliderSetting("Скорость", 1.5F, 0.1F, 3F, 0.1F);
    private final SliderSetting size = new SliderSetting("Размер", 0.2F, 0.0F, 1F, 0.1F);
    private final SliderSetting attackcos = new SliderSetting("Кол-в атаки", 30, 5, 50, 1);
    private final SliderSetting totem = new SliderSetting("Кол-в тотеме", 8, 2, 16, 1);
    private final SliderSetting move = new SliderSetting("Кол-в движении", 2, 1, 6, 1);
    private final SliderSetting brosok = new SliderSetting("Кол-в броске", 6, 1, 16, 1);
    private final SliderSetting countAFK = new SliderSetting("Кол-во при бездействии", 5, 1, 25, 1);
    private final SliderSetting range = new SliderSetting("Дистанция при бездействии", 16, 4, 32, 1);
    private final SliderSetting cursorCount = new SliderSetting("Кол-в курсор", 3, 1, 10, 1).setVisible(() -> elements.getValueByName("Курсор").get());
    private final SliderSetting cursorLifetime = new SliderSetting("Жизнь курсор", 1.0F, 0.3F, 3.0F, 0.1F).setVisible(() -> elements.getValueByName("Курсор").get());
    private final SliderSetting cursorSize = new SliderSetting("Размер курсор", 8.0F, 2.0F, 20.0F, 1.0F).setVisible(() -> elements.getValueByName("Курсор").get());
    private final SliderSetting cursorSpread = new SliderSetting("Разброс курсор", 5.0F, 0.0F, 20.0F, 1.0F).setVisible(() -> elements.getValueByName("Курсор").get());
    private final BooleanSetting cursorFade = new BooleanSetting("Затухание курсор", true).setVisible(() -> elements.getValueByName("Курсор").get());
    private final CategorySetting cursorCategory = new CategorySetting("Курсор").setVisible(() -> elements.getValueByName("Курсор").get());

    private final BooleanSetting rotation = new BooleanSetting("Кручения", false);
    private final BooleanSetting glowEffect = new BooleanSetting("Эффект свечения", true);
    private final BooleanSetting viefForStinka = new BooleanSetting("Видить через стену", false);
    private final ModeSetting particleColorMode = new ModeSetting(
            "\u0412\u0438\u0434",
            "\u0420\u0430\u043d\u0434\u043e\u043c",
            "\u0420\u0430\u043d\u0434\u043e\u043c",
            "\u041a\u043b\u0438\u0435\u043d\u0442",
            "\u0421\u0432\u043e\u0439"
    );
    private final ColorSetting particleColor = new ColorSetting(
            "Цвет частиц",
            ColorUtils.rgb(255, 255, 255)
    ).setVisible(() -> particleColorMode.get().contains("Cвой"));

    private static final float PARTICLE_SIZE = 0.15f;
    private static final float PARTICLE_SIZE_MULTIPLIER = 1.3f;
    private static final float ROTATE_SPEED = 0.5f;
    private static final int WALK_PARTICLE_LIFETIME = 3500;
    private static final float GRAVITY = 0.00001f;
    private static final float FRICTION = 0.9999f;
    private static final float BOUNCE = 0.8f;
    private static final float ATTACK_GRAVITY = 0.00002f;
    private static final float ATTACK_FRICTION = 0.9995f;
    private static final float ATTACK_BOUNCE = 0.65f;
    private static final int ATTACK_LIFETIME = 4000;
    private static final boolean ENHANCED_PHYSICS = true;
    private static final int JUMP_PARTICLES_COUNT = 6;
    private static final int MAX_CURSOR_PARTICLES = 400;
    private static final int[] TOTEM_RANDOM_COLORS = {
            ColorUtils.getColor(221, 218, 127),
            ColorUtils.getColor(127, 221, 144)
    };

    private final List<Particle3D> targetParticles = new ArrayList<>();
    private final List<Particle3D> flameParticles = new ArrayList<>();
    private final List<Particle3D> worldParticles = new ArrayList<>();
    private final List<CursorParticle> cursorParticles = new ArrayList<>();
    private final Map<String, List<Particle3D>> particleBatches = new HashMap<>();
    private double lastCursorX = -1;
    private double lastCursorY = -1;
    private static final int MAX_TARGET_PARTICLES = 80;
    private static final int MAX_FLAME_PARTICLES = 120;

    private final Map<String, ResourceLocation> textureCache = new HashMap<>();

    private static final String[] PARTICLE_TYPES = {"Свечение", "Звёзды", "Сердце", "Молния", "Доллар", "Снежинки", "Маленькая Звезда", "Полигон", "Корона"};

    private long totemPartictTime = 0;
    private final long totemSpawnDuration = 2500;
    private boolean spawningTotemParticles = false;
    private Entity currentTotemTarget = null;

    public Particles() {
        particleColorMode.setConfigKey("ParticleColorMode");
        particleColor.setConfigKey("ParticleColor");
        addSettings(particleType, elements, speed, size, attackcos, totem, move, brosok, countAFK, range, rotation, glowEffect, viefForStinka, particleColorMode, particleColor, cursorCategory, cursorCount, cursorLifetime, cursorSize, cursorSpread, cursorFade);
        initTextureCache();
    }

    private void initTextureCache() {
        textureCache.put("Свечение", new ResourceLocation("harmony/images/particles/bloom.png"));
        textureCache.put("Звёзды", new ResourceLocation("harmony/images/particles/stars.png"));
        textureCache.put("Сердце", new ResourceLocation("harmony/images/particles/heart.png"));
        textureCache.put("Молния", new ResourceLocation("harmony/images/particles/lightning.png"));
        textureCache.put("Доллар", new ResourceLocation("harmony/images/particles/dollar.png"));
        textureCache.put("Снежинки", new ResourceLocation("harmony/images/particles/snowflake.png"));
        textureCache.put("Маленькая Звезда", new ResourceLocation("harmony/images/particles/starnew.png"));
        textureCache.put("Полигон", new ResourceLocation("harmony/images/particles/polygon.png"));
        textureCache.put("Корона", new ResourceLocation("harmony/images/particles/crown.png"));
    }

    private ResourceLocation getTextureByType(String type) {
        return textureCache.getOrDefault(type, textureCache.get("Свечение"));
    }

    private String getRandomParticleType() {
        return PARTICLE_TYPES[ThreadLocalRandom.current().nextInt(PARTICLE_TYPES.length)];
    }

    private String getParticleTypeForSpawn() {
        String type = particleType.get();
        if (type.equals("Рандом")) {
            return getRandomParticleType();
        }
        return type;
    }

    private boolean isRandomParticleColor() {
        return particleColorMode.is("\u0420\u0430\u043d\u0434\u043e\u043c");
    }

    private int getStaticParticleColor() {
        return particleColorMode.get().contains("\u0421\u0432\u043e\u0439") ? particleColor.get() : Theme.MainColor(0);
    }

    private int getParticleColor(int index) {
        if (isRandomParticleColor()) {
            float hue = (index * 0.618033988749895f) % 1.0f;
            return Color.HSBtoRGB(hue, 0.9f, 0.95f);
        } else {
            return getStaticParticleColor();
        }
    }

    private float getParticleSize(String type) {
        float baseSize = PARTICLE_SIZE * PARTICLE_SIZE_MULTIPLIER + (this.size.get() * 0.2F);
        switch (type) {
            case "Свечение":
                return baseSize * 1.1f;
            case "Сердце":
                return baseSize * 1.2f;
            case "Молния":
                return baseSize * 1.3f;
            case "Звёзды":
                return baseSize * 0.9f;
            case "Доллар":
                return baseSize * 1.0f;
            case "Снежинки":
                return baseSize * 1.15f;
            case "Маленькая Звезда":
                return baseSize * 0.95f;
            case "Полигон":
                return baseSize * 1.0f;
            case "Корона":
                return baseSize * 1.0f;
            default:
                return baseSize;
        }
    }

    @Override
    public boolean onEnable() {
        super.onEnable();
        targetParticles.clear();
        flameParticles.clear();
        worldParticles.clear();
        cursorParticles.clear();
        lastCursorX = -1;
        lastCursorY = -1;
        return false;
    }

    @Subscribe
    public void onAttack(AttackEvent event) {
        Entity target = event.entity;
        float motion = 1.35F;
        if (elements.getValueByName("Удар").get()) {
            String currentType = getParticleTypeForSpawn();

            if (targetParticles.size() > MAX_TARGET_PARTICLES) {
                targetParticles.subList(0, (int) (targetParticles.size() - MAX_TARGET_PARTICLES + attackcos.get())).clear();
            }

            for (int i = 0; i < attackcos.get(); i++) {
                targetParticles.add(new Particle3D(
                        new org.joml.Vector3d(
                                target.getPosX() + MathUtil.random(-0.4F, 0.4F),
                                target.getPosY() + MathUtil.random(0, target.getHeight()),
                                target.getPosZ() + MathUtil.random(-0.4F, 0.4F)
                        ),
                        new org.joml.Vector3d(
                                MathUtil.random(-motion, motion),
                                MathUtil.random(-1.25F, 1.25F),
                                MathUtil.random(-motion, motion)
                        ),
                        targetParticles.size(),
                        getParticleColor(targetParticles.size()),
                        currentType,
                        MathUtil.random(0.8f, 1.1f),
                        true
                ));
            }
        }
    }

    public void spawnTotemParticles(Entity target) {
        if (!elements.getValueByName("Тотем").get()) return;
        spawningTotemParticles = true;
        totemPartictTime = System.currentTimeMillis();
        currentTotemTarget = target;
    }

    public void updateTotemParticles() {
        if (!spawningTotemParticles || currentTotemTarget == null) return;

        long elapsed = System.currentTimeMillis() - totemPartictTime;
        if (elapsed > totemSpawnDuration) {
            spawningTotemParticles = false;
            currentTotemTarget = null;
            return;
        }

        String currentType = getParticleTypeForSpawn();
        float motion = 1;

        for (int i = 0; i < totem.get(); i++) {
            int col;
            if (isRandomParticleColor()) {
                col = TOTEM_RANDOM_COLORS[ThreadLocalRandom.current().nextInt(TOTEM_RANDOM_COLORS.length)];
            } else {
                col = getStaticParticleColor();
            }

            flameParticles.add(new Particle3D(
                    new org.joml.Vector3d(
                            currentTotemTarget.getPosX() + MathUtil.random(-0.4F, 0.4F),
                            currentTotemTarget.getPosY() + MathUtil.random(0, 2),
                            currentTotemTarget.getPosZ() + MathUtil.random(-0.4F, 0.4F)
                    ),
                    new org.joml.Vector3d(
                            MathUtil.random(-0.8F, 0.8F),
                            MathUtil.random(-0.6F, 0.1F),
                            MathUtil.random(-0.8F, 0.8F)
                    ),
                    flameParticles.size(),
                    col,
                    currentType,
                    MathUtil.random(0.7f, 1.0f),
                    false
            ));
        }
    }

    @Subscribe
    public void onJump(JumpEvent e) {
        LivingEntity target = mc.player;
        float motion = 2.0f;
        if (elements.getValueByName("Прыжок").get()) {
            String currentType = getParticleTypeForSpawn();

            if (flameParticles.size() > MAX_FLAME_PARTICLES) {
                flameParticles.subList(0, flameParticles.size() - MAX_FLAME_PARTICLES + JUMP_PARTICLES_COUNT).clear();
            }

            for (int i = 0; i < JUMP_PARTICLES_COUNT; i++) {
                float velocityMultiplier = ENHANCED_PHYSICS ?
                        MathUtil.random(0.8f, 1.2f) : 1.0f;

                flameParticles.add(new Particle3D(
                        new org.joml.Vector3d(
                                target.getPosX() + MathUtil.random(-0.3f, 0.3f),
                                target.getPosY(),
                                target.getPosZ() + MathUtil.random(-0.3f, 0.3f)
                        ),
                        new org.joml.Vector3d(
                                MathUtil.random(-motion, motion) * velocityMultiplier,
                                MathUtil.random(0.8f, 2.5f) * velocityMultiplier,
                                MathUtil.random(-motion, motion) * velocityMultiplier
                        ),
                        flameParticles.size(),
                        getParticleColor(flameParticles.size()),
                        currentType,
                        MathUtil.random(0.7f, 1.0f),
                        false
                ));
            }
        }
    }

    @Subscribe
    public void onMotion(EventMotion e) {
        updateTotemParticles();

        if (targetParticles.size() > MAX_TARGET_PARTICLES) {
            targetParticles.subList(0, targetParticles.size() - MAX_TARGET_PARTICLES).clear();
        }

        if (flameParticles.size() > MAX_FLAME_PARTICLES) {
            flameParticles.subList(0, flameParticles.size() - MAX_FLAME_PARTICLES).clear();
        }

        if (elements.getValueByName("Бездействие").get()) {
            int r = range.get().intValue();
            String currentType = getParticleTypeForSpawn();
            for (int i = 0; i < countAFK.get().intValue(); i++) {
                Vector3d additional = mc.player.getPositionVec().add(
                        MathUtil.random(-r, r), 0, MathUtil.random(-r, r)
                );
                BlockPos pos = mc.world.getHeight(Heightmap.Type.MOTION_BLOCKING, new BlockPos(additional));

                double x = pos.getX() + MathUtil.random(0, 1);
                double z = pos.getZ() + MathUtil.random(0, 1);
                double y = mc.player.getPosY() + MathUtil.random(mc.player.getHeight(), r);

                org.joml.Vector3d spawnPos = new org.joml.Vector3d(x, y, z);

                while (!mc.world.isAirBlock(new BlockPos((int)spawnPos.x, (int)spawnPos.y, (int)spawnPos.z)) && spawnPos.y < mc.world.getHeight()) {
                    spawnPos = new org.joml.Vector3d(spawnPos.x, spawnPos.y + 1, spawnPos.z);
                }

                worldParticles.add(new Particle3D(
                        spawnPos,
                        new org.joml.Vector3d(
                                mc.player.getMotion().x + MathUtil.random(-0.5f, 0.5f),
                                MathUtil.random(-0.06f, 0.06f),
                                mc.player.getMotion().z + MathUtil.random(-0.5f, 0.5f)
                        ),
                        worldParticles.size(),
                        getParticleColor(worldParticles.size()),
                        currentType,
                        MathUtil.random(0.6f, 0.9f),
                        false
                ));
            }
        }

        if (elements.getValueByName("Ходьба").get()) {
            if (mc.player.lastTickPosX != mc.player.getPosX() || mc.player.lastTickPosY != mc.player.getPosY() || mc.player.lastTickPosZ != mc.player.getPosZ()) {
                String currentType = getParticleTypeForSpawn();
                for (int i = 0; i < move.get(); i++) {
                    flameParticles.add(new Particle3D(
                            new org.joml.Vector3d(
                                    mc.player.getPosX() + MathUtil.random(-0.45f, 0.45f),
                                    mc.player.getPosY() + MathUtil.random(0, mc.player.getHeight()),
                                    mc.player.getPosZ() + MathUtil.random(-0.45f, 0.45f)
                            ),
                            new org.joml.Vector3d(
                                    mc.player.getMotion().x + MathUtil.random(-0.1f, 0.1f),
                                    MathUtil.random(-0.1f, 0.1f),
                                    mc.player.getMotion().z + MathUtil.random(-0.1f, 0.1f)
                            ).mul(0.2F),
                            flameParticles.size(),
                            getParticleColor(flameParticles.size()),
                            currentType,
                            MathUtil.random(0.6f, 0.9f),
                            false
                    ));
                }
            }
        }

        if (elements.getValueByName("Бросаемый предмет").get()) {
            for (Entity entity : cachedProjectiles) {

                    if (entity instanceof TridentEntity) {
                        TridentEntity trident = (TridentEntity) entity;
                        if (trident.func_234616_v_() != null && trident.dealtDamage) {
                            continue;
                        }
                    }

                    boolean isMoving = entity.prevPosX != entity.getPosX() ||
                            entity.prevPosY != entity.getPosY() ||
                            entity.prevPosZ != entity.getPosZ();
                    if (!isMoving) {
                        continue;
                    }

                    String currentType = getParticleTypeForSpawn();
                    Vector3d pos = entity.getPositionVec();
                    for (int i = 0; i < brosok.get(); i++) {
                        flameParticles.add(new Particle3D(
                                new org.joml.Vector3d(
                                        pos.x + MathUtil.random(-0.5f, 0.5f),
                                        pos.y + MathUtil.random(-0.5f, 0.5f),
                                        pos.z + MathUtil.random(-0.5f, 0.5f)
                                ),
                                new org.joml.Vector3d(
                                        MathUtil.random(-0.06f, 0.06f),
                                        MathUtil.random(-0.06f, 0.06f),
                                        MathUtil.random(-0.06f, 0.06f)
                                ),
                                flameParticles.size(),
                                getParticleColor(flameParticles.size()),
                                currentType,
                                MathUtil.random(0.6f, 0.9f),
                                false
                        ));
                    }
            }
        }

        removeExpiredParticles(targetParticles, 1000);
        removeExpiredParticles(worldParticles, 2000);
        removeExpiredParticles(flameParticles, 2000);
    }

    private void removeExpiredParticles(List<Particle3D> particles, long timeout) {
        for (int i = particles.size() - 1; i >= 0; --i) {
            if (particles.get(i).time.isReached(timeout)) {
                particles.remove(i);
            }
        }
    }

    @Subscribe
    public void onChange(EventChangeWorld e) {
        targetParticles.clear();
        flameParticles.clear();
        worldParticles.clear();
        cursorParticles.clear();
        lastCursorX = -1;
        lastCursorY = -1;
    }

    @Subscribe
    public void onDisplay(EventDisplay e) {
        if (mc.player == null || mc.world == null || e.getType() != EventDisplay.Type.POST_SCREEN) {
            return;
        }

        if (mc.currentScreen == null) {
            if (!cursorParticles.isEmpty()) {
                cursorParticles.clear();
            }
            lastCursorX = -1;
            lastCursorY = -1;
            return;
        }

        if (!elements.getValueByName("Курсор").get()) {
            if (!cursorParticles.isEmpty()) {
                cursorParticles.clear();
            }
            lastCursorX = -1;
            lastCursorY = -1;
            return;
        }

        int mouseX = (int) (mc.mouseHelper.getMouseX() * mc.getMainWindow().getScaledWidth() / (double) mc.getMainWindow().getWidth());
        int mouseY = (int) (mc.mouseHelper.getMouseY() * mc.getMainWindow().getScaledHeight() / (double) mc.getMainWindow().getHeight());
        updateCursorTrail(mouseX, mouseY);
        renderCursorTrail();
    }

    @Subscribe
    public void onRender(DEngineEvent event) {
        MatrixStack matrix = event.getMatrix();
        ActiveRenderInfo camera = event.getActiveRenderInfo();

        RenderSystem.pushMatrix();
        RenderSystem.disableLighting();

        if (viefForStinka.get()) {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
        }

        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.shadeModel(GL11.GL_SMOOTH);
        RenderSystem.disableCull();
        RenderSystem.disableAlphaTest();
        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ZERO);

        float speedValue = speed.get();
        if (!targetParticles.isEmpty()) {
            for (Particle3D particle : targetParticles) {
                particle.update(ATTACK_GRAVITY * speedValue, ATTACK_FRICTION, ATTACK_BOUNCE);
            }
            renderParticlesBatched(matrix, camera, targetParticles, 400);
        }

        if (!worldParticles.isEmpty()) {
            for (Particle3D particle : worldParticles) {
                particle.update(GRAVITY * speedValue, FRICTION, BOUNCE);
            }
            renderParticlesBatched(matrix, camera, worldParticles, 800);
        }

        if (!flameParticles.isEmpty()) {
            for (Particle3D particle : flameParticles) {
                particle.update(GRAVITY * speedValue, FRICTION, BOUNCE);
            }
            renderParticlesBatched(matrix, camera, flameParticles, 700);
        }

        if (viefForStinka.get()) {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        }

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.enableAlphaTest();
        RenderSystem.popMatrix();
    }

    private void renderParticlesBatched(MatrixStack ms, ActiveRenderInfo camera, List<Particle3D> particles, long fadeInTime) {
        clearParticleBatches();
        for (Particle3D p : particles) {
            List<Particle3D> batch = particleBatches.get(p.particleType);
            if (batch == null) {
                batch = new ArrayList<>();
                particleBatches.put(p.particleType, batch);
            }
            batch.add(p);
        }

        Vector3d cameraPos = camera.getProjectedView();
        double cameraX = cameraPos.getX();
        double cameraY = cameraPos.getY();
        double cameraZ = cameraPos.getZ();
        Quaternion cameraRotation = camera.getRotation();
        float partialTicks = mc.getRenderPartialTicks();
        boolean rotateParticles = rotation.get();
        BufferBuilder buffer = tessellator.getBuffer();

        for (Map.Entry<String, List<Particle3D>> entry : particleBatches.entrySet()) {
            String type = entry.getKey();
            List<Particle3D> batch = entry.getValue();
            if (batch.isEmpty()) continue;

            if (glowEffect.get()) {
                mc.getTextureManager().bindTexture(textureCache.get("Свечение"));
                buffer.begin(GL_QUADS, POSITION_COLOR_TEX);
                for (Particle3D p : batch) {
                    renderParticleGlow(ms, buffer, p, cameraX, cameraY, cameraZ, cameraRotation, partialTicks, rotateParticles, fadeInTime);
                }
                tessellator.draw();
            }

            mc.getTextureManager().bindTexture(getTextureByType(type));
            buffer.begin(GL_QUADS, POSITION_COLOR_TEX);
            for (Particle3D p : batch) {
                renderParticleEnhanced(ms, buffer, p, cameraX, cameraY, cameraZ, cameraRotation, partialTicks, rotateParticles, fadeInTime);
            }
            tessellator.draw();
        }
    }

    private void clearParticleBatches() {
        for (List<Particle3D> batch : particleBatches.values()) {
            batch.clear();
        }
    }

    private void renderParticleGlow(MatrixStack ms, BufferBuilder buffer, Particle3D p,
                                    double cameraX, double cameraY, double cameraZ,
                                    Quaternion cameraRotation, float partialTicks,
                                    boolean rotateParticles, long fadeInTime) {
        if ((int) p.animation.getValue() != 255 && !p.time.isReached(fadeInTime)) {
            p.animation.run(255);
        }

        if ((int) p.animation.getValue() != 0 && p.time.isReached(600)) {
            p.animation.run(0);
        }

        int color;
        if (isRandomParticleColor()) {
            float hue = (p.index * 0.618033988749895f) % 1.0f;
            float saturation = 0.9f;
            float brightness = 0.95f;
            int baseColor = Color.HSBtoRGB(hue, saturation, brightness);
            color = ColorUtils.setAlpha(baseColor, (int) (p.animation.getValue() * 0.1F));
        } else {
            color = ColorUtils.setAlpha(p.color, (int) (p.animation.getValue() * 0.1F));
        }

        final org.joml.Vector3d v = p.position;

        final float x = (float) lerp(p.prevX, v.x, partialTicks);
        final float y = (float) lerp(p.prevY, v.y, partialTicks);
        final float z = (float) lerp(p.prevZ, v.z, partialTicks);

        float size = getParticleSize(p.particleType) * p.sizeMultiplier * 2;

        ms.push();
        ms.translate(-cameraX, -cameraY, -cameraZ);

        ms.translate(x, y, z);

        ms.rotate(cameraRotation);

        if (rotateParticles) {
            ms.rotate(Vector3f.ZP.rotationDegrees(p.rotation));
        }

        int r = ColorUtils.getRed(color);
        int g = ColorUtils.getGreen(color);
        int b = ColorUtils.getBlue(color);
        int a = ColorUtils.getAlpha(color);

        Matrix4f matrix = ms.getLast().getMatrix();
        buffer.pos(matrix, -size, -size, 0)
                .color(r, g, b, a).tex(0, 0).endVertex();
        buffer.pos(matrix, -size, size, 0)
                .color(r, g, b, a).tex(0, 1).endVertex();
        buffer.pos(matrix, size, size, 0)
                .color(r, g, b, a).tex(1, 1).endVertex();
        buffer.pos(matrix, size, -size, 0)
                .color(r, g, b, a).tex(1, 0).endVertex();

        ms.pop();
    }

    private void renderParticleEnhanced(MatrixStack ms, BufferBuilder buffer, Particle3D p,
                                        double cameraX, double cameraY, double cameraZ,
                                        Quaternion cameraRotation, float partialTicks,
                                        boolean rotateParticles, long fadeInTime) {
        if ((int) p.animation.getValue() != 255 && !p.time.isReached(fadeInTime)) {
            p.animation.run(255);
        }

        if ((int) p.animation.getValue() != 0 && p.time.isReached(600)) {
            p.animation.run(0);
        }

        int color;
        if (isRandomParticleColor()) {
            float hue = (p.index * 0.618033988749895f) % 1.0f;
            float saturation = 0.9f;
            float brightness = 0.95f;
            int baseColor = Color.HSBtoRGB(hue, saturation, brightness);
            color = ColorUtils.setAlpha(baseColor, (int) (p.animation.getValue()));
        } else {
            color = ColorUtils.setAlpha(p.color, (int) (p.animation.getValue()));
        }

        final org.joml.Vector3d v = p.position;

        final float x = (float) lerp(p.prevX, v.x, partialTicks);
        final float y = (float) lerp(p.prevY, v.y, partialTicks);
        final float z = (float) lerp(p.prevZ, v.z, partialTicks);

        float size = getParticleSize(p.particleType) * p.sizeMultiplier;

        long alive = p.time.getTime();
        float lifeProgress = (float) alive / (p.isAttackParticle ? ATTACK_LIFETIME : WALK_PARTICLE_LIFETIME);

        float pulseAmount = (float) (Math.sin(alive * 0.01) * 0.05 + Math.sin(alive * 0.005) * 0.03 + 1.0);
        float pulsedSize = size * pulseAmount;

        if (lifeProgress > 0.7f) {
            float fadeProgress = (lifeProgress - 0.7f) / 0.3f;
            pulsedSize *= (1.0f - fadeProgress * 0.3f);
        }

        ms.push();

        ms.translate(-cameraX, -cameraY, -cameraZ);

        ms.translate(x, y, z);

        ms.rotate(cameraRotation);

        if (rotateParticles) {
            ms.rotate(Vector3f.ZP.rotationDegrees(p.rotation));
        }

        int baseR = ColorUtils.getRed(color);
        int baseG = ColorUtils.getGreen(color);
        int baseB = ColorUtils.getBlue(color);
        int baseA = ColorUtils.getAlpha(color);

        float brightness = Math.max(1.0f, 1.5f - lifeProgress * 0.5f);
        int red = Math.min(255, (int)(baseR * brightness));
        int green = Math.min(255, (int)(baseG * brightness));
        int blue = Math.min(255, (int)(baseB * brightness));

        if (p.isAttackParticle) {
            red = Math.min(255, red + 30);
            green = Math.min(255, green + 30);
            blue = Math.min(255, blue + 30);
        }

        Matrix4f matrix = ms.getLast().getMatrix();
        buffer.pos(matrix, -pulsedSize, -pulsedSize, 0)
                .color(red, green, blue, baseA).tex(0, 0).endVertex();
        buffer.pos(matrix, -pulsedSize, pulsedSize, 0)
                .color(red, green, blue, baseA).tex(0, 1).endVertex();
        buffer.pos(matrix, pulsedSize, pulsedSize, 0)
                .color(red, green, blue, baseA).tex(1, 1).endVertex();
        buffer.pos(matrix, pulsedSize, -pulsedSize, 0)
                .color(red, green, blue, baseA).tex(1, 0).endVertex();

        ms.pop();
    }

    private double lerp(double start, double end, float percent) {
        return start + (end - start) * percent;
    }

    private void updateCursorTrail(float mouseX, float mouseY) {
        if (lastCursorX < 0 || lastCursorY < 0) {
            lastCursorX = mouseX;
            lastCursorY = mouseY;
            return;
        }

        double dx = mouseX - lastCursorX;
        double dy = mouseY - lastCursorY;
        double dist = Math.sqrt(dx * dx + dy * dy);

        if (dist >= 2.0) {
            int count = Math.max(1, cursorCount.get().intValue());
            for (int i = 0; i < count; i++) {
                String type = getParticleTypeForSpawn();
                cursorParticles.add(new CursorParticle((float) mouseX, (float) mouseY, type, getParticleColor(cursorParticles.size())));
            }
        }

        lastCursorX = mouseX;
        lastCursorY = mouseY;

        float life = cursorLifetime.get();
        long now = System.currentTimeMillis();
        for (int i = cursorParticles.size() - 1; i >= 0; --i) {
            if ((now - cursorParticles.get(i).creationTime) / 1000.0f > life) {
                cursorParticles.remove(i);
            }
        }

        if (cursorParticles.size() > MAX_CURSOR_PARTICLES) {
            cursorParticles.subList(0, cursorParticles.size() - MAX_CURSOR_PARTICLES).clear();
        }
    }

    private void renderCursorTrail() {
        if (cursorParticles.isEmpty()) return;

        float life = cursorLifetime.get();
        float baseSize = cursorSize.get();
        float spread = cursorSpread.get();
        long now = System.currentTimeMillis();

        RenderSystem.pushMatrix();
        RenderSystem.enableBlend();
        RenderSystem.enableTexture();
        RenderSystem.shadeModel(GL11.GL_SMOOTH);
        RenderSystem.defaultBlendFunc();

        for (CursorParticle p : cursorParticles) {
            float age = (now - p.creationTime) / 1000.0f;
            float alpha = cursorFade.get() ? (1.0f - age / life) : 1.0f;
            if (alpha <= 0.0f) continue;

            float size = baseSize * (1.0f - age / life * 0.5f);
            float px = p.x + p.offsetX * spread;
            float py = p.y + p.offsetY * spread;
            int color = ColorUtils.setAlpha(p.color, (int) (alpha * 255.0f));
            float rotationValue = 0.0f;

            if (rotation.get()) {
                p.rotation += p.rotationSpeed;
                rotationValue = p.rotation;
            }

            if (glowEffect.get()) {
                RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ZERO);
                ResourceLocation glowTexture = textureCache.get("Свечение");
                float glowSize = size * 1.6f;
                int glowColor = ColorUtils.setAlpha(p.color, (int) (alpha * 120.0f));
                drawCursorQuad(glowTexture != null ? glowTexture : getTextureByType(p.particleType),
                        px - glowSize / 2.0f, py - glowSize / 2.0f, glowSize, glowColor, rotationValue);
                RenderSystem.defaultBlendFunc();
            }

            drawCursorQuad(getTextureByType(p.particleType), px - size / 2.0f, py - size / 2.0f, size, color, rotationValue);
        }

        RenderSystem.shadeModel(GL11.GL_FLAT);
        RenderSystem.disableBlend();
        RenderSystem.popMatrix();
    }

    private void drawCursorQuad(ResourceLocation texture, float x, float y, float size, int color, float rotationValue) {
        mc.getTextureManager().bindTexture(texture);
        if (rotationValue != 0.0f) {
            float cx = x + size / 2.0f;
            float cy = y + size / 2.0f;
            GLUtility.rotate(cx, cy, rotationValue, () -> RenderUtility.quads(x, y, size, size, 7, color));
        } else {
            RenderUtility.quads(x, y, size, size, 7, color);
        }
    }

    public class Particle3D {
        private final int index;
        private final int color;
        private final TimerUtility time = new TimerUtility();
        private final CompactAnimation animation = new CompactAnimation(Easing.LINEAR, 300);
        private final ResourceLocation texture;
        private final String particleType;
        private final float sizeMultiplier;
        public final org.joml.Vector3d position;
        private final org.joml.Vector3d delta;
        public final boolean isAttackParticle;

        private float rotate = 0;
        public float rotation;
        private final float rotationSpeed;

        public double prevX, prevY, prevZ;

        public Particle3D(final org.joml.Vector3d position, final org.joml.Vector3d velocity, final int index, int color, String particleType, float sizeMultiplier, boolean isAttackParticle) {
            this.position = position;
            this.prevX = position.x;
            this.prevY = position.y;
            this.prevZ = position.z;
            this.delta = new org.joml.Vector3d(velocity.x * 0.01, velocity.y * 0.01, velocity.z * 0.01);
            this.index = index;
            this.color = color;
            this.particleType = particleType;
            this.texture = Particles.this.getTextureByType(particleType);
            this.sizeMultiplier = sizeMultiplier;
            this.isAttackParticle = isAttackParticle;
            this.rotation = ThreadLocalRandom.current().nextFloat() * 360;
            this.rotationSpeed = ROTATE_SPEED * (0.7f + ThreadLocalRandom.current().nextFloat() * 0.6f) *
                    (ThreadLocalRandom.current().nextBoolean() ? 1 : -1);
            this.time.reset();
        }

        public void update(float gravity, float friction, float bounceCoefficient) {
            prevX = position.x;
            prevY = position.y;
            prevZ = position.z;

            if (Particles.this.rotation.get()) {
                rotate += rotationSpeed;
                rotation = rotate;
            }

            final Block block1 = BlockUtils.getBlock(this.position.x, this.position.y, this.position.z + this.delta.z);
            if (isValidBlock(block1))
                this.delta.z *= -bounceCoefficient;

            final Block block2 = BlockUtils.getBlock(this.position.x, this.position.y + this.delta.y, this.position.z);
            if (isValidBlock(block2)) {
                this.delta.x *= 0.999F;
                this.delta.z *= 0.999F;
                this.delta.y *= -bounceCoefficient;
            }

            final Block block3 = BlockUtils.getBlock(this.position.x + this.delta.x, this.position.y, this.position.z);
            if (isValidBlock(block3))
                this.delta.x *= -bounceCoefficient;

            this.updateWithoutPhysics(gravity, friction);

            if (ThreadLocalRandom.current().nextInt(20) == 0) {
                this.delta.x += MathUtil.random(-0.0002f, 0.0002f);
                this.delta.y += MathUtil.random(-0.0002f, 0.0002f);
                this.delta.z += MathUtil.random(-0.0002f, 0.0002f);
            }
        }

        private boolean isValidBlock(Block block) {
            return !(block instanceof AirBlock)
                    && !(block instanceof BushBlock)
                    && !(block instanceof AbstractButtonBlock)
                    && !(block instanceof TorchBlock)
                    && !(block instanceof LeverBlock)
                    && !(block instanceof AbstractPressurePlateBlock)
                    && !(block instanceof CarpetBlock)
                    && !(block instanceof FlowingFluidBlock);
        }

        public void updateWithoutPhysics(float gravity, float friction) {
            this.position.x += this.delta.x * speed.get();
            this.position.y += this.delta.y * speed.get();
            this.position.z += this.delta.z * speed.get();

            this.delta.x /= friction;
            this.delta.y -= gravity;
            this.delta.z /= friction;
        }

        public String getParticleType() {
            return particleType;
        }

        public float getSizeMultiplier() {
            return sizeMultiplier;
        }
    }

    private class CursorParticle {
        final float x;
        final float y;
        final String particleType;
        final int color;
        final long creationTime;
        final float offsetX;
        final float offsetY;
        float rotation;
        final float rotationSpeed;

        private CursorParticle(float x, float y, String particleType, int color) {
            this.x = x;
            this.y = y;
            this.particleType = particleType;
            this.color = color;
            this.creationTime = System.currentTimeMillis();
            this.offsetX = (ThreadLocalRandom.current().nextFloat() - 0.5f) * 2.0f;
            this.offsetY = (ThreadLocalRandom.current().nextFloat() - 0.5f) * 2.0f;
            this.rotation = ThreadLocalRandom.current().nextFloat() * 360.0f;
            this.rotationSpeed = ROTATE_SPEED * (0.7f + ThreadLocalRandom.current().nextFloat() * 0.6f) *
                    (ThreadLocalRandom.current().nextBoolean() ? 1 : -1);
        }
    }
}
