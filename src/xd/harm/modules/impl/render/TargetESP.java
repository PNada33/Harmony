package xd.harm.modules.impl.render;

import static com.mojang.blaze3d.platform.GlStateManager.GL_QUADS;
import static net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_COLOR_TEX;
import static com.mojang.blaze3d.systems.RenderSystem.depthMask;

import xd.harm.Harmony;
import xd.harm.events.combat.AttackEvent;
import xd.harm.events.render.EventDisplay;
import xd.harm.events.world.EventUpdate;
import xd.harm.events.render.WorldEvent;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.impl.combat.HitAura;
import xd.harm.modules.settings.Setting;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.ModeSetting;
import xd.harm.modules.settings.impl.SliderSetting;
import xd.harm.utils.animations.Animation;
import xd.harm.utils.animations.Direction;
import xd.harm.utils.animations.impl.DecelerateAnimation;
import xd.harm.utils.math.Vector4i;
import xd.harm.utils.projections.ProjectionUtil;
import xd.harm.utils.render.color.ColorUtils;
import xd.harm.utils.render.rect.RenderUtility;
import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ThreadLocalRandom;

@ModuleRegister(name = "TargetESP", category = Category.Render, desc = "Отображает активного таргета")
public class TargetESP extends Module {
    private static final int ICE_CRYSTAL_COUNT = 8;
    private static final ResourceLocation TARGET_TEXTURE_DIAMOND = new ResourceLocation("harmony/images/targetesp/target.png");
    private static final ResourceLocation TARGET_TEXTURE_SPIKE = new ResourceLocation("harmony/images/targetesp/target2.png");
    private static final ResourceLocation TARGET_TEXTURE_NANO = new ResourceLocation("harmony/images/targetesp/target3.png");
    private static final ResourceLocation SKULL_TEXTURE_HEALTHY = new ResourceLocation("harmony/images/targetesp/skull_state_0.png");
    private static final ResourceLocation SKULL_TEXTURE_DAMAGED = new ResourceLocation("harmony/images/targetesp/skull_state_1.png");
    private static final ResourceLocation SKULL_TEXTURE_CRITICAL = new ResourceLocation("harmony/images/targetesp/skull_state_2.png");
    private static final ResourceLocation PARTICLE_BLOOM_TEXTURE = new ResourceLocation("harmony/images/particles/bloom.png");
    private static final ResourceLocation CHAIN_TEXTURE = new ResourceLocation("harmony/images/targetesp/chain.png");
    private static final Vector3d ICE_FALLBACK_UP = new Vector3d(0.0, 1.0, 0.0);
    private static final AxisAlignedBB CUBE_PARTICLE_BOX = new AxisAlignedBB(-0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f);
    private static final String ICE_MODE = "Кристалл";
    private ModeSetting mode = new ModeSetting("Режим", "Дефолт", "Дефолт",  "Цепи",  "Призраки", "Кубики", "Демоны", ICE_MODE);
    private ModeSetting targetImage = new ModeSetting("Картинка", "Ромб", "Ромб", "Спайк", "Нано")
            .setVisible(() -> mode.is("Дефолт"));
    private BooleanSetting throughWalls = new BooleanSetting("Через стены", false);
    private SliderSetting diamondSize = new SliderSetting("Размер", 100, 50, 200, 10)
            .setVisible(() -> mode.is("Дефолт"));
    private SliderSetting cubeSize = new SliderSetting("Размер кубиков", 0.12f, 0.05f, 0.3f, 0.01f)
            .setVisible(() -> mode.is("Кубики"));
    private SliderSetting circleRadius = new SliderSetting("Радиус круга", 0.7f, 0.3f, 1.5f, 0.1f)
            .setVisible(() -> mode.is("Кубики"));
    private SliderSetting circleSegments = new SliderSetting("Сегменты круга", 32, 16, 64, 4)
            .setVisible(() -> mode.is("Кубики"));
    private SliderSetting devilCircleRadius = new SliderSetting("Радиус дьявольского круга", 1.2f, 0.5f, 2.5f, 0.1f)
            .setVisible(() -> mode.is("Демоны"));
    private SliderSetting swordCount = new SliderSetting("Количество мечей", 6, 3, 12, 1)
            .setVisible(() -> mode.is("Демоны"));
    private SliderSetting swordSize = new SliderSetting("Размер мечей", 0.5f, 0.2f, 1.0f, 0.05f)
            .setVisible(() -> mode.is("Демоны"));

    private final Animation targetEspAnim = new DecelerateAnimation(300, 255);
    private final Animation chainTargetAnim = new DecelerateAnimation(400, 255);
    private final Animation chainTarget2Anim = new DecelerateAnimation(400, 255);
    private final Animation hurtAnim = new DecelerateAnimation(100, 255);
    private final Animation alpha = new DecelerateAnimation(600, 255);
    private final Animation scaleAnimation = new DecelerateAnimation(400, 255);
    private final Animation devilCircleAnim = new DecelerateAnimation(500, 255);
    private LivingEntity prevTarget;
    private LivingEntity target;
    private LivingEntity currentTarget;
    private long lastTime = System.currentTimeMillis();

    private final BufferBuilder BUILDER = Tessellator.getInstance().getBuffer();
    private final Tessellator TESSELLATOR = Tessellator.getInstance();

    private final ArrayList<CubeParticle> cubeParticles = new ArrayList<>();
    private static final long PARTICLE_LIFE_TIME = 1000L;
    private static final int PARTICLES_PER_SPAWN = 1;
    private static final float SPAWN_INTERVAL = 0.017f;
    private float spawnAccumulator = 0f;
    private float deltaTime = 0.0f;
    private long lastParticleTime = System.currentTimeMillis();

    private final ArrayList<DemonHand> demonHands = new ArrayList<>();
    private final ArrayList<SoulParticle> soulParticles = new ArrayList<>();
    private final ArrayList<HellPortal> hellPortals = new ArrayList<>();
    private final ArrayList<DemonChain> demonChains = new ArrayList<>();
    private final ArrayList<BloodParticle> bloodParticles = new ArrayList<>();
    private final ArrayList<CrushingWall> crushingWalls = new ArrayList<>();
    private final ArrayList<FallingCoffin> fallingCoffins = new ArrayList<>();

    private float lastTargetHealth = 0f;
    private LivingEntity lastDeathTarget = null;
    private LivingEntity lastTarget = null;
    private int iceLastHurtTime = 0;
    private int iceLastTargetId = -1;
    private boolean icePendingCritical = false;
    private long iceHitTime = -1L;
    private long iceCritTime = -1L;
    private final ArrayList<IceSparkParticle> iceSparks = new ArrayList<>();
    private final ArrayList<IceShardParticle> iceShards = new ArrayList<>();
    private static final long ICE_HIT_DURATION = 320L;
    private static final long ICE_CRIT_DURATION = 420L;
    private Framebuffer iceGlowMaskBuffer;
    private final ArrayList<Framebuffer> iceBloomBuffers = new ArrayList<>();
    private int iceKawaseDownProgram = -1;
    private int iceKawaseUpProgram = -1;
    private int iceOuterGlowProgram = -1;
    private boolean iceGlowPipelineFailed = false;
    private int iceKawaseDownTextureUniform = -1;
    private int iceKawaseDownSizeUniform = -1;
    private int iceKawaseDownOffsetUniform = -1;
    private int iceKawaseDownHalfPixelUniform = -1;
    private int iceKawaseUpTextureUniform = -1;
    private int iceKawaseUpSizeUniform = -1;
    private int iceKawaseUpOffsetUniform = -1;
    private int iceKawaseUpHalfPixelUniform = -1;
    private int iceKawaseUpColorUniform = -1;
    private int iceOuterBloomTextureUniform = -1;
    private int iceOuterMaskTextureUniform = -1;
    private int iceOuterGlowColor1Uniform = -1;
    private int iceOuterGlowColor2Uniform = -1;
    private final Vector3d[] iceCrystalPositions = new Vector3d[ICE_CRYSTAL_COUNT];
    private final Vector3d[] iceCrystalDirections = new Vector3d[ICE_CRYSTAL_COUNT];
    private final float[] iceCrystalRolls = new float[ICE_CRYSTAL_COUNT];
    private final Vector4i diamondColors = new Vector4i(0, 0, 0, 0);
    private final int[] blendSrcScratch = new int[1];
    private final int[] blendDstScratch = new int[1];
    private HitAura cachedHitAura;

    private float vortexRotation = 0f;
    private long lastVortexSpawn = 0;

    public TargetESP() {
        addSettings(new Setting[]{mode, targetImage, throughWalls, diamondSize, cubeSize, circleRadius, circleSegments, devilCircleRadius, swordCount, swordSize});
    }

    private ResourceLocation getTargetTexture() {
        if (targetImage.is("Ромб")) {
            return TARGET_TEXTURE_DIAMOND;
        } else if (targetImage.is("Спайк")) {
            return TARGET_TEXTURE_SPIKE;
        } else if (targetImage.is("Нано")) {
            return TARGET_TEXTURE_NANO;
        }
        return TARGET_TEXTURE_DIAMOND;
    }

    private ResourceLocation getSkullTexture(LivingEntity entity) {
        if (entity == null) {
            return SKULL_TEXTURE_HEALTHY;
        }
        float health = entity.getHealth();
        float maxHealth = entity.getMaxHealth();
        float healthPercent = health / maxHealth;
        if (healthPercent <= 0.25f) {
            return SKULL_TEXTURE_CRITICAL;
        } else if (healthPercent <= 0.5f) {
            return SKULL_TEXTURE_DAMAGED;
        } else {
            return SKULL_TEXTURE_HEALTHY;
        }
    }

    private HitAura getHitAuraModule() {
        if (cachedHitAura != null) {
            return cachedHitAura;
        }
        for (Module module : Harmony.getInstance().getModuleManager().getModules()) {
            if (module instanceof HitAura) {
                cachedHitAura = (HitAura) module;
                return cachedHitAura;
            }
        }
        return null;
    }

    private LivingEntity resolveTarget(HitAura hitAura) {
        LivingEntity auraTarget = hitAura != null ? hitAura.getTarget() : null;
        return mode.is(ICE_MODE) && !isIceRenderableTarget(auraTarget) ? null : auraTarget;
    }

    private boolean isIceRenderableTarget(LivingEntity entity) {
        return entity != null && entity != mc.player && entity.isAlive() && entity.getHealth() > 0.0f;
    }

    @Subscribe
    private void onAttack(AttackEvent e) {
        if (!mode.is(ICE_MODE) || e == null || !(e.entity instanceof LivingEntity)) return;
        LivingEntity attacked = (LivingEntity) e.entity;
        LivingEntity active = currentTarget != null ? currentTarget : target;
        if (active == null || attacked.getEntityId() != active.getEntityId()) return;
        if (isLocalCriticalState()) {
            icePendingCritical = true;
        }
    }

    @Subscribe
    private void onUpdate(EventUpdate e) {
        HitAura hitAura = getHitAuraModule();
        this.target = resolveTarget(hitAura);

        if (target != null) {
            if (currentTarget != null && currentTarget != target) {
                if (currentTarget.getHealth() <= 0 || !currentTarget.isAlive()) {
                    spawnDeathEffect(currentTarget);
                }


                vortexRotation = 0f;
            }

            if (prevTarget != null && prevTarget == target) {
                if (lastTargetHealth > 0 && target.getHealth() <= 0) {
                    spawnDeathEffect(target);
                }
            }
            lastTargetHealth = target.getHealth();
            this.prevTarget = target;
            this.currentTarget = target;
        } else {
            if (prevTarget != null && lastTargetHealth > 0) {
                if (prevTarget.getHealth() <= 0 || !prevTarget.isAlive()) {
                    spawnDeathEffect(prevTarget);
                }
            }
            lastTargetHealth = 0;

            if (alpha.finished(Direction.BACKWARDS)) {
                prevTarget = null;
                currentTarget = null;
                lastDeathTarget = null;
            }
        }

        boolean hasTarget = target != null;
        targetEspAnim.setDirection(hasTarget ? Direction.FORWARDS : Direction.BACKWARDS);
        chainTargetAnim.setDirection(hasTarget ? Direction.FORWARDS : Direction.BACKWARDS);
        chainTarget2Anim.setDirection(chainTargetAnim.getOutput() >= 0.95f ? Direction.FORWARDS : Direction.BACKWARDS);
        alpha.setDirection(hasTarget ? Direction.FORWARDS : Direction.BACKWARDS);
        scaleAnimation.setDirection(hasTarget ? Direction.FORWARDS : Direction.BACKWARDS);
        devilCircleAnim.setDirection(hasTarget ? Direction.FORWARDS : Direction.BACKWARDS);

        if (prevTarget != null) {
            hurtAnim.setDirection(prevTarget.hurtTime > 0 ? Direction.FORWARDS : Direction.BACKWARDS);
        }

        updateIceFocus();

        Iterator<DemonHand> handIterator = demonHands.iterator();
        while (handIterator.hasNext()) {
            DemonHand hand = handIterator.next();
            if (hand.shouldRemove()) {
                handIterator.remove();
            }
        }

        Iterator<SoulParticle> soulIterator = soulParticles.iterator();
        while (soulIterator.hasNext()) {
            SoulParticle soul = soulIterator.next();
            soul.update();
            if (soul.shouldRemove()) {
                soulIterator.remove();
            }
        }

        Iterator<HellPortal> portalIterator = hellPortals.iterator();
        while (portalIterator.hasNext()) {
            HellPortal portal = portalIterator.next();
            if (portal.shouldRemove()) {
                portalIterator.remove();
            }
        }

        Iterator<DemonChain> chainIterator = demonChains.iterator();
        while (chainIterator.hasNext()) {
            DemonChain chain = chainIterator.next();
            chain.update();
            if (chain.shouldRemove()) {
                chainIterator.remove();
            }
        }

        Iterator<BloodParticle> bloodIterator = bloodParticles.iterator();
        while (bloodIterator.hasNext()) {
            BloodParticle blood = bloodIterator.next();
            blood.update();
            if (blood.shouldRemove()) {
                bloodIterator.remove();
            }
        }

        Iterator<CrushingWall> wallIterator = crushingWalls.iterator();
        while (wallIterator.hasNext()) {
            CrushingWall wall = wallIterator.next();
            wall.update();
            if (wall.shouldRemove()) {
                wallIterator.remove();
            }
        }


        Iterator<FallingCoffin> coffinIterator = fallingCoffins.iterator();
        while (coffinIterator.hasNext()) {
            FallingCoffin coffin = coffinIterator.next();
            coffin.update();
            if (coffin.shouldRemove()) {
                coffinIterator.remove();
            }
        }

        if (mode.is("Кровь") && currentTarget != null) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastVortexSpawn > 30) {
                lastVortexSpawn = currentTime;
            }

            if (currentTarget.hurtTime > 0 && ThreadLocalRandom.current().nextDouble() > 0.6) {
                spawnBloodSpurt(currentTarget);
            }
        }
    }

    private void updateIceFocus() {
        if (!mode.is(ICE_MODE)) {
            iceSparks.clear();
            iceShards.clear();
            iceLastHurtTime = 0;
            iceLastTargetId = -1;
            icePendingCritical = false;
            return;
        }

        long now = System.currentTimeMillis();
        Iterator<IceSparkParticle> sparkIterator = iceSparks.iterator();
        while (sparkIterator.hasNext()) {
            IceSparkParticle spark = sparkIterator.next();
            spark.update();
            if (spark.shouldRemove(now)) {
                sparkIterator.remove();
            }
        }

        Iterator<IceShardParticle> shardIterator = iceShards.iterator();
        while (shardIterator.hasNext()) {
            IceShardParticle shard = shardIterator.next();
            shard.update();
            if (shard.shouldRemove(now)) {
                shardIterator.remove();
            }
        }

        if (currentTarget == null) {
            iceLastHurtTime = 0;
            iceLastTargetId = -1;
            icePendingCritical = false;
            return;
        }

        int targetId = currentTarget.getEntityId();
        if (targetId != iceLastTargetId) {
            iceLastTargetId = targetId;
            iceLastHurtTime = 0;
            icePendingCritical = false;
        }

        int hurtTime = currentTarget.hurtTime;
        if (hurtTime > 0 && hurtTime > iceLastHurtTime) {
            boolean critical = icePendingCritical || isLocalCriticalState();
            iceHitTime = now;
            if (critical) {
                iceCritTime = now;
            }
            spawnIceImpact(currentTarget, critical);
            icePendingCritical = false;
        }
        iceLastHurtTime = hurtTime;
    }

    private boolean isLocalCriticalState() {
        return mc.player != null && !mc.player.isOnGround() && mc.player.fallDistance > 0.0f;
    }

    private void spawnDeathEffect(LivingEntity entity) {
        if (entity == null) return;
        if (entity == lastDeathTarget) return;

        lastDeathTarget = entity;

        double x = entity.getPosX();
        double y = entity.getPosY();
        double z = entity.getPosZ();

        if (mode.is("Кровь")) {
            float playerYaw = mc.player.rotationYaw;
            fallingCoffins.add(new FallingCoffin(x, y, z, entity.getHeight(), entity.getWidth(), playerYaw));

            for (int i = 0; i < 20; i++) {
                float angle = (float) (Math.random() * Math.PI * 2);
                float speed = 0.4f + (float) Math.random() * 0.8f;
                double vx = Math.cos(angle) * speed;
                double vz = Math.sin(angle) * speed;
                bloodParticles.add(new BloodParticle(x, y + entity.getHeight() / 2, z, vx, 0.5 + Math.random() * 0.4, vz, true));
            }

            for (int i = 0; i < 4; i++) {
                crushingWalls.add(new CrushingWall(x, y, z, i * 90, entity.getHeight()));
            }
        } else if (mode.is("Демоны")) {
            float radius = devilCircleRadius.get().floatValue();
            hellPortals.add(new HellPortal(x, y, z, radius));

            int handCount = 8;
            for (int i = 0; i < handCount; i++) {
                float angle = (float) (i * Math.PI * 2 / handCount);
                double handX = x + Math.cos(angle) * radius * 0.8;
                double handZ = z + Math.sin(angle) * radius * 0.8;
                demonHands.add(new DemonHand(handX, y, handZ, x, y + 1, z, (float) Math.toDegrees(angle) + 180, i * 50));
            }

            int chainCount = 6;
            for (int i = 0; i < chainCount; i++) {
                float angle = (float) (i * Math.PI * 2 / chainCount + Math.PI / 6);
                demonChains.add(new DemonChain(x, y, z, angle, radius, i * 80));
            }

            int soulCount = 12;
            for (int i = 0; i < soulCount; i++) {
                double soulX = x + ThreadLocalRandom.current().nextDouble(-0.5, 0.5);
                double soulY = y + 0.5 + ThreadLocalRandom.current().nextDouble(0, 1.5);
                double soulZ = z + ThreadLocalRandom.current().nextDouble(-0.5, 0.5);
                soulParticles.add(new SoulParticle(soulX, soulY, soulZ, x, y, z));
            }
        } else if (mode.is(ICE_MODE)) {
            spawnIceDeathEffect(entity);
        }
    }

    private void spawnBloodSpurt(LivingEntity entity) {
        double x = entity.getPosX();
        double y = entity.getPosY() + entity.getHeight() / 2;
        double z = entity.getPosZ();

        float angle = (float) (Math.random() * Math.PI * 2);
        float speed = 0.2f + (float) Math.random() * 0.4f;
        double vx = Math.cos(angle) * speed;
        double vz = Math.sin(angle) * speed;

        bloodParticles.add(new BloodParticle(x, y, z, vx, 0.2 + Math.random() * 0.2, vz, false));
    }

    @Subscribe
    public void onRender(WorldEvent e) {
        if (prevTarget == null && !mode.is("Кубики") && !mode.is("Демоны") && !mode.is("Кровь") && !mode.is(ICE_MODE)
                && demonHands.isEmpty() && soulParticles.isEmpty() && hellPortals.isEmpty() && demonChains.isEmpty()
                && bloodParticles.isEmpty() && crushingWalls.isEmpty() && fallingCoffins.isEmpty()
                && iceSparks.isEmpty() && iceShards.isEmpty()) return;

        MatrixStack ms = new MatrixStack();

        if (mode.is("Кровь")) {

            if (!bloodParticles.isEmpty()) {
                RenderSystem.enableBlend();
                RenderSystem.disableTexture();
                RenderSystem.disableCull();
                RenderSystem.depthMask(false);
                RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, 0, 1);
                BUILDER.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
                float yaw = mc.getRenderManager().info.getYaw();
                float pitch = mc.getRenderManager().info.getPitch();
                for (BloodParticle blood : bloodParticles) {
                    blood.renderBatched(ms, yaw, pitch);
                }
                TESSELLATOR.draw();
                RenderSystem.enableTexture();
                RenderSystem.depthMask(true);
                RenderSystem.enableCull();
                RenderSystem.defaultBlendFunc();
            }

            for (FallingCoffin coffin : fallingCoffins) {
                coffin.render(ms);
            }

            if (currentTarget != null) {
            }

        } else if (mode.is("Цепи")) {
            renderChain(ms, target);

        } else if (mode.is("Призраки")) {
            renderGhosts(ms, e);

        } else if (mode.is("Кубики")) {
            renderCubes(ms, e);

        } else if (mode.is(ICE_MODE)) {
            renderIceFocus(ms, e);

        } else if (mode.is("Демоны")) {
            renderDevilCircle(ms, e);

            for (HellPortal portal : hellPortals) {
                portal.render(ms);
            }

            for (DemonChain chain : demonChains) {
                chain.render(ms);
            }

            for (DemonHand hand : demonHands) {
                hand.render(ms);
            }

            if (!soulParticles.isEmpty()) {
                RenderSystem.enableBlend();
                RenderSystem.disableTexture();
                RenderSystem.disableCull();
                RenderSystem.depthMask(false);
                RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, 0, 1);
                BUILDER.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
                float yaw = mc.getRenderManager().info.getYaw();
                float pitch = mc.getRenderManager().info.getPitch();
                for (SoulParticle soul : soulParticles) {
                    soul.renderBatched(ms, yaw, pitch);
                }
                TESSELLATOR.draw();
                RenderSystem.enableTexture();
                RenderSystem.depthMask(true);
                RenderSystem.enableCull();
                RenderSystem.defaultBlendFunc();
            }
        }
    }

    private void renderIceFocus(MatrixStack matrixStack, WorldEvent e) {
        LivingEntity entity = currentTarget != null ? currentTarget : prevTarget;
        boolean hasLiveTarget = entity != null && entity != mc.player && entity.isAlive() && entity.getHealth() > 0.0f;
        boolean hasParticles = !iceSparks.isEmpty() || !iceShards.isEmpty();
        if (!hasLiveTarget && !hasParticles) return;

        Vector3d camera = mc.getRenderManager().info.getProjectedView();
        long now = System.currentTimeMillis();
        float alphaValue = (float) alpha.getOutput() / 255.0f;
        float hitProgress = getIceTimedFade(now, iceHitTime, ICE_HIT_DURATION);

        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.shadeModel(7425);
        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, 0, 1);
        RenderSystem.enableDepthTest();

        matrixStack.push();
        matrixStack.translate(-camera.x, -camera.y, -camera.z);

        if (hasLiveTarget && alphaValue > 0.01f) {
            float visualAlpha = getIceVisualFade(alphaValue);
            float partialTicks = e.getPartialTicks();
            Vector3d center = getIceCenter(entity, partialTicks);
            updateIceCrystalFrame(entity, center, hitProgress, now);
            Vector3d[] crystalPositions = iceCrystalPositions;
            Vector3d[] crystalDirections = iceCrystalDirections;
            float[] crystalRolls = iceCrystalRolls;
            int baseColor = getIceThemeBaseColor(hitProgress);
            Matrix4f matrix = matrixStack.getLast().getMatrix();
            float crystalLength = (float) (0.24 + entity.getWidth() * 0.2);
            boolean xrayPass = throughWalls.get();

            RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 0, 1);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            BUILDER.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
            for (int i = 0; i < ICE_CRYSTAL_COUNT; i++) {
                Vector3d direction = crystalDirections[i];
                float roll = crystalRolls[i];
                float popScale = 0.82f + visualAlpha * 0.18f;
                int faceAlpha = MathHelper.clamp((int) (visualAlpha * (92 + hitProgress * 38)), 0, 138);
                addIceCrystalGradientFaces(matrix, crystalPositions[i], direction, crystalLength * popScale, 0.055f * popScale, baseColor, faceAlpha, roll, hitProgress);
            }
            TESSELLATOR.draw();

            if (xrayPass) {
                GL11.glDepthFunc(GL11.GL_GREATER);
                int xrayColor = getIceThemeHighlightColor(baseColor, 0.36f);
                BUILDER.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
                for (int i = 0; i < ICE_CRYSTAL_COUNT; i++) {
                    Vector3d direction = crystalDirections[i];
                    float roll = crystalRolls[i];
                    float popScale = 0.82f + visualAlpha * 0.18f;
                    int faceAlpha = MathHelper.clamp((int) (visualAlpha * (84 + hitProgress * 30)), 0, 118);
                    addIceCrystalGradientFaces(matrix, crystalPositions[i], direction, crystalLength * popScale, 0.055f * popScale, xrayColor, faceAlpha, roll, hitProgress * 0.7f);
                }
                TESSELLATOR.draw();
            }

            RenderSystem.depthMask(false);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, 0, 1);
            renderIceShaderGlow(matrix, crystalPositions, crystalDirections, crystalRolls, crystalLength, visualAlpha, hitProgress, now, baseColor);

            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            GL11.glColorMask(false, false, false, false);
            BUILDER.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
            for (int i = 0; i < ICE_CRYSTAL_COUNT; i++) {
                Vector3d direction = crystalDirections[i];
                float roll = crystalRolls[i];
                float popScale = 0.82f + visualAlpha * 0.18f;
                addIceCrystalFaces(matrix, crystalPositions[i], direction, crystalLength * popScale, 0.055f * popScale, ColorUtils.rgb(255, 255, 255), 255, roll);
            }
            TESSELLATOR.draw();
            GL11.glColorMask(true, true, true, true);

            RenderSystem.depthMask(false);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, 0, 1);
            renderIceOutline(matrix, crystalPositions, crystalDirections, crystalRolls, crystalLength, baseColor, visualAlpha, hitProgress, now);
            if (xrayPass) {
                GL11.glDepthFunc(GL11.GL_GREATER);
                renderIceOutline(matrix, crystalPositions, crystalDirections, crystalRolls, crystalLength, baseColor, visualAlpha * 0.68f, hitProgress, now);
            }
            RenderSystem.enableDepthTest();
            GL11.glDepthFunc(GL11.GL_LEQUAL);
        }

        renderIceParticles(matrixStack);
        matrixStack.pop();

        RenderSystem.shadeModel(7424);
        RenderSystem.lineWidth(1.0f);
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableTexture();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private Vector3d getIceCenter(LivingEntity entity, float partialTicks) {
        double x = entity.lastTickPosX + (entity.getPosX() - entity.lastTickPosX) * partialTicks;
        double y = entity.lastTickPosY + (entity.getPosY() - entity.lastTickPosY) * partialTicks;
        double z = entity.lastTickPosZ + (entity.getPosZ() - entity.lastTickPosZ) * partialTicks;
        return new Vector3d(x, y + entity.getHeight() * 0.52, z);
    }

    private void updateIceCrystalFrame(LivingEntity entity, Vector3d center, float hitProgress, long now) {
        fillIceCrystalPositions(iceCrystalPositions, entity, center, hitProgress, now);
        for (int i = 0; i < ICE_CRYSTAL_COUNT; i++) {
            Vector3d pos = iceCrystalPositions[i];
            iceCrystalDirections[i] = normalizeIce(center.x - pos.x, center.y - pos.y, center.z - pos.z, ICE_FALLBACK_UP);
            iceCrystalRolls[i] = getIceCrystalRoll(now, i);
        }
    }

    private Vector3d[] getIceCrystalPositions(LivingEntity entity, Vector3d center, float hitProgress, long now) {
        Vector3d[] result = new Vector3d[ICE_CRYSTAL_COUNT];
        fillIceCrystalPositions(result, entity, center, hitProgress, now);
        return result;
    }

    private void fillIceCrystalPositions(Vector3d[] result, LivingEntity entity, Vector3d center, float hitProgress, long now) {
        double entityHeight = entity.getHeight();
        double baseY = center.y - entityHeight * 0.52;
        double halfWidth = entity.getWidth() * 0.5 + 0.025;
        double minX = center.x - halfWidth;
        double maxX = center.x + halfWidth;
        double minY = baseY + 0.16;
        double maxY = baseY + entityHeight + 0.02;
        double minZ = center.z - halfWidth;
        double maxZ = center.z + halfWidth;
        double pinch = 0.72 * hitProgress;
        setIceCrystalPosition(result, 0, minX, minY, minZ, center, pinch, hitProgress, now);
        setIceCrystalPosition(result, 1, minX, minY, maxZ, center, pinch, hitProgress, now);
        setIceCrystalPosition(result, 2, maxX, minY, minZ, center, pinch, hitProgress, now);
        setIceCrystalPosition(result, 3, maxX, minY, maxZ, center, pinch, hitProgress, now);
        setIceCrystalPosition(result, 4, minX, maxY, minZ, center, pinch, hitProgress, now);
        setIceCrystalPosition(result, 5, minX, maxY, maxZ, center, pinch, hitProgress, now);
        setIceCrystalPosition(result, 6, maxX, maxY, minZ, center, pinch, hitProgress, now);
        setIceCrystalPosition(result, 7, maxX, maxY, maxZ, center, pinch, hitProgress, now);
    }

    private void setIceCrystalPosition(Vector3d[] result, int index, double x, double y, double z,
                                       Vector3d center, double pinch, float hitProgress, long now) {
        double bob = Math.sin(now * 0.004 + index * 0.83) * 0.035 * (1.0 - hitProgress * 0.55);
        result[index] = new Vector3d(
                x + (center.x - x) * pinch,
                y + (center.y - y) * pinch + bob,
                z + (center.z - z) * pinch
        );
    }

    private float getIceCrystalRoll(long now, int index) {
        return now * 0.0045f + index * 1.37f;
    }

    private float getIceVisualFade(float alphaValue) {
        return smoothIceStep(MathHelper.clamp(alphaValue, 0.0f, 1.0f));
    }

    private int getIceThemeBaseColor(float hitProgress) {
        float hit = MathHelper.clamp(hitProgress, 0.0f, 1.0f);
        int mainColor = ColorUtils.setAlpha(Theme.MainColor(0), 255);
        int secondColor = ColorUtils.setAlpha(Theme.RectColor(0), 255);
        int themeColor = ColorUtils.interpolateColor(mainColor, secondColor, 0.28f + hit * 0.22f);
        return ColorUtils.brighter(themeColor, 0.12f + hit * 0.12f);
    }

    private int getIceThemeSecondColor() {
        return ColorUtils.setAlpha(Theme.RectColor(0), 255);
    }

    private int getIceThemeHighlightColor(int baseColor, float amount) {
        int mixedColor = ColorUtils.interpolateColor(baseColor, Theme.MainColor(0), 0.35f);
        return ColorUtils.setAlpha(ColorUtils.brighter(mixedColor, MathHelper.clamp(amount, 0.0f, 1.0f)), 255);
    }

    private int getIceThemeShadowColor(int baseColor, float amount) {
        float shadow = MathHelper.clamp(amount, 0.0f, 1.0f);
        int themeDark = ColorUtils.darker(ColorUtils.interpolateColor(baseColor, getIceThemeSecondColor(), 0.58f), 0.48f + shadow * 0.22f);
        return ColorUtils.interpolateColor(themeDark, ColorUtils.rgb(0, 0, 0), 0.32f + shadow * 0.42f);
    }

    private void renderIceBloom(Matrix4f matrix, Vector3d[] crystalPositions, Vector3d[] crystalDirections,
                                float[] crystalRolls, float crystalLength,
                                float alphaValue, float hitProgress, long now) {
        if (alphaValue <= 0.02f) return;

        RenderSystem.disableTexture();
        int baseColor = getIceThemeBaseColor(hitProgress);
        int outerColor = getIceThemeHighlightColor(baseColor, 0.22f);
        int innerColor = getIceThemeHighlightColor(baseColor, 0.58f);
        int hitColor = ColorUtils.interpolateColor(baseColor, getIceThemeSecondColor(), 0.62f);
        BUILDER.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < ICE_CRYSTAL_COUNT; i++) {
            Vector3d direction = crystalDirections[i];
            float roll = crystalRolls[i];
            float pulse = 0.92f + (float) Math.sin(now * 0.006 + i * 0.72f) * 0.08f;
            int outerAlpha = MathHelper.clamp((int) (alphaValue * (26 + hitProgress * 48)), 0, 88);
            int innerAlpha = MathHelper.clamp((int) (alphaValue * (14 + hitProgress * 24)), 0, 48);
            addIceCrystalFaces(matrix, crystalPositions[i], direction, crystalLength * (1.64f + hitProgress * 0.16f) * pulse, 0.105f * pulse, outerColor, outerAlpha, roll);
            addIceCrystalFaces(matrix, crystalPositions[i], direction, crystalLength * (1.18f + hitProgress * 0.1f) * pulse, 0.074f * pulse, innerColor, innerAlpha, roll);
            if (hitProgress > 0.05f) {
                addIceCrystalFaces(matrix, crystalPositions[i], direction, crystalLength * 1.08f * pulse, 0.058f * pulse, hitColor, (int) (innerAlpha * hitProgress), roll);
            }
        }
        TESSELLATOR.draw();
    }

    private void renderIceShaderGlow(Matrix4f matrix, Vector3d[] crystalPositions, Vector3d[] crystalDirections,
                                     float[] crystalRolls, float crystalLength,
                                     float alphaValue, float hitProgress, long now, int baseColor) {
        if (alphaValue <= 0.02f) return;
        if (!ensureIceGlowPipeline()) return;

        Framebuffer mainBuffer = mc.getFramebuffer();
        Framebuffer maskBuffer = getIceGlowMaskBuffer();
        if (mainBuffer == null || maskBuffer == null) return;

        maskBuffer.framebufferClear(Minecraft.IS_RUNNING_ON_MAC);
        copyIceDepth(mainBuffer, maskBuffer);
        maskBuffer.bindFramebuffer(true);

        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.shadeModel(7425);
        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.enableDepthTest();
        GL11.glDepthFunc(GL11.GL_LEQUAL);

        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ONE);
        BUILDER.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < ICE_CRYSTAL_COUNT; i++) {
            Vector3d direction = crystalDirections[i];
            float roll = crystalRolls[i];
            float pulse = 0.97f + (float) Math.sin(now * 0.0048 + i * 0.74f) * 0.022f;
            int faceMaskAlpha = MathHelper.clamp((int) (alphaValue * (118 + hitProgress * 42)), 0, 168);
            addIceCrystalShaderGlowFaces(matrix, crystalPositions[i], direction, crystalLength * 1.018f * pulse, 0.062f * pulse, baseColor, faceMaskAlpha, roll, hitProgress);
        }
        TESSELLATOR.draw();

        mainBuffer.bindFramebuffer(true);
        setupIceGlow2D();
        int bloomTexture = generateIceKawaseBloom(4);
        renderIceOuterGlow(bloomTexture, getTextureId(maskBuffer), alphaValue, hitProgress, baseColor);
        restoreIceGlow3D();
        mainBuffer.bindFramebuffer(true);

        RenderSystem.disableTexture();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
    }

    private void renderIceOutline(Matrix4f matrix, Vector3d[] crystalPositions, Vector3d[] crystalDirections,
                                  float[] crystalRolls, float crystalLength,
                                  int baseColor, float alphaValue, float hitProgress, long now) {
        if (alphaValue <= 0.02f) return;

        RenderSystem.disableTexture();
        float softFade = alphaValue * alphaValue;
        float outlineFade = MathHelper.clamp(alphaValue * (0.55f + alphaValue * 0.45f), 0.0f, 1.0f);
        if (outlineFade <= 0.01f) return;
        int glowColor = ColorUtils.interpolateColor(baseColor, getIceThemeHighlightColor(baseColor, 0.58f), 0.68f);

        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, 0, 1);
        BUILDER.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < ICE_CRYSTAL_COUNT; i++) {
            Vector3d direction = crystalDirections[i];
            float roll = crystalRolls[i];
            float pulse = 0.97f + (float) Math.sin(now * 0.006 + i * 0.72f) * 0.03f;
            int shellAlpha = MathHelper.clamp((int) (softFade * (12 + hitProgress * 8)), 0, 28);
            addIceCrystalFaces(matrix, crystalPositions[i], direction, crystalLength * 1.07f * pulse, 0.069f * pulse, glowColor, shellAlpha, roll);
        }
        TESSELLATOR.draw();

        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        RenderSystem.lineWidth(1.0f);
        BUILDER.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < ICE_CRYSTAL_COUNT; i++) {
            Vector3d direction = crystalDirections[i];
            float roll = crystalRolls[i];
            float pulse = 0.97f + (float) Math.sin(now * 0.006 + i * 0.72f) * 0.03f;
            int glowAlpha = MathHelper.clamp((int) (outlineFade * (56 + hitProgress * 34)), 0, 115);
            addIceCrystalSoftEdges(matrix, crystalPositions[i], direction, crystalLength * 1.045f * pulse, 0.066f * pulse, glowColor, glowAlpha, roll, true);
            int sweepAlpha = MathHelper.clamp((int) (outlineFade * (96 + hitProgress * 52)), 0, 170);
            addIceCrystalAnimatedOuterEdges(matrix, crystalPositions[i], direction, crystalLength * 1.055f * pulse, 0.068f * pulse, glowColor, sweepAlpha, roll, now, i);
        }
        TESSELLATOR.draw();

        int coreColor = ColorUtils.interpolateColor(getIceThemeHighlightColor(baseColor, 0.72f), getIceThemeBaseColor(hitProgress), hitProgress * 0.36f);
        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 0, 1);
        RenderSystem.lineWidth(1.15f);
        BUILDER.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < ICE_CRYSTAL_COUNT; i++) {
            Vector3d direction = crystalDirections[i];
            float roll = crystalRolls[i];
            int coreAlpha = MathHelper.clamp((int) (outlineFade * (238 + hitProgress * 17)), 0, 255);
            addIceCrystalSoftEdges(matrix, crystalPositions[i], direction, crystalLength, 0.058f, coreColor, coreAlpha, roll, false);
        }
        TESSELLATOR.draw();

        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, 0, 1);
        RenderSystem.lineWidth(1.0f);
    }

    private void renderIceParticles(MatrixStack matrixStack) {
        Matrix4f matrix = matrixStack.getLast().getMatrix();
        int particleBaseColor = getIceThemeBaseColor(0.0f);
        int particleFaceColor = ColorUtils.interpolateColor(particleBaseColor, getIceThemeHighlightColor(particleBaseColor, 0.5f), 0.42f);
        int particleEdgeColor = getIceThemeHighlightColor(particleBaseColor, 0.62f);
        int criticalSparkColor = getIceThemeHighlightColor(particleBaseColor, 0.82f);
        int sparkColor = getIceThemeHighlightColor(particleBaseColor, 0.48f);
        long now = System.currentTimeMillis();

        if (!iceShards.isEmpty()) {
            BUILDER.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
            for (IceShardParticle shard : iceShards) {
                int alpha = MathHelper.clamp((int) (shard.alpha(now) * 115), 0, 140);
                Vector3d direction = normalizeIce(shard.motionX, shard.motionY, shard.motionZ, ICE_FALLBACK_UP);
                addIceCrystalFaces(matrix, new Vector3d(shard.x, shard.y, shard.z), direction, shard.size, shard.size * 0.22f, particleFaceColor, alpha);
            }
            TESSELLATOR.draw();

            BUILDER.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
            for (IceShardParticle shard : iceShards) {
                int alpha = MathHelper.clamp((int) (shard.alpha(now) * 210), 0, 230);
                Vector3d direction = normalizeIce(shard.motionX, shard.motionY, shard.motionZ, ICE_FALLBACK_UP);
                addIceCrystalEdges(matrix, new Vector3d(shard.x, shard.y, shard.z), direction, shard.size, shard.size * 0.24f, particleEdgeColor, alpha);
            }
            TESSELLATOR.draw();
        }

        if (!iceSparks.isEmpty()) {
            BUILDER.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
            for (IceSparkParticle spark : iceSparks) {
                int alpha = MathHelper.clamp((int) (spark.alpha(now) * 235), 0, 255);
                int color = spark.critical ? criticalSparkColor : sparkColor;
                int r = ColorUtils.getRed(color);
                int g = ColorUtils.getGreen(color);
                int b = ColorUtils.getBlue(color);
                float tail = spark.critical ? 3.8f : 2.5f;
                BUILDER.pos(matrix, (float) spark.x, (float) spark.y, (float) spark.z).color(r, g, b, alpha).endVertex();
                BUILDER.pos(matrix, (float) (spark.x - spark.motionX * tail), (float) (spark.y - spark.motionY * tail), (float) (spark.z - spark.motionZ * tail)).color(r, g, b, 0).endVertex();
            }
            TESSELLATOR.draw();
        }
    }

    private void addIceCrystalFaces(Matrix4f matrix, Vector3d pos, Vector3d direction, float length, float radius,
                                    int color, int alpha) {
        addIceCrystalFaces(matrix, pos, direction, length, radius, color, alpha, 0.0f);
    }

    private void addIceCrystalFaces(Matrix4f matrix, Vector3d pos, Vector3d direction, float length, float radius,
                                    int color, int alpha, float roll) {
        IceShape shape = makeIceShape(pos, direction, length, radius, roll);
        int r = ColorUtils.getRed(color);
        int g = ColorUtils.getGreen(color);
        int b = ColorUtils.getBlue(color);
        iceTriangle(matrix, shape.tip, shape.right, shape.up, r, g, b, alpha);
        iceTriangle(matrix, shape.tip, shape.up, shape.left, r, g, b, Math.max(0, alpha - 10));
        iceTriangle(matrix, shape.tip, shape.left, shape.down, r, g, b, Math.max(0, alpha - 22));
        iceTriangle(matrix, shape.tip, shape.down, shape.right, r, g, b, Math.max(0, alpha - 8));
        iceTriangle(matrix, shape.right, shape.up, shape.left, r, g, b, Math.max(0, alpha - 32));
        iceTriangle(matrix, shape.right, shape.left, shape.down, r, g, b, Math.max(0, alpha - 40));
    }

    private void addIceCrystalGradientFaces(Matrix4f matrix, Vector3d pos, Vector3d direction, float length, float radius,
                                            int baseColor, int alpha, float roll, float darkPower) {
        IceShape shape = makeIceShape(pos, direction, length, radius, roll);
        float darkness = MathHelper.clamp(0.36f + darkPower * 0.22f, 0.0f, 0.72f);
        int secondColor = getIceThemeSecondColor();
        int tipColor = getIceThemeHighlightColor(baseColor, 0.62f);
        int brightSideColor = ColorUtils.brighter(ColorUtils.interpolateColor(baseColor, secondColor, 0.16f), 0.34f);
        int coolSideColor = ColorUtils.darker(ColorUtils.interpolateColor(baseColor, secondColor, 0.42f), 0.12f + darkPower * 0.08f);
        int deepColor = getIceThemeShadowColor(baseColor, darkness);
        int baseShadeColor = getIceThemeShadowColor(baseColor, 0.62f + darkPower * 0.18f);

        int tipAlpha = MathHelper.clamp((int) (alpha * 1.08f), 0, 172);
        int sideAlpha = MathHelper.clamp((int) (alpha * 0.96f), 0, 160);
        int coolAlpha = MathHelper.clamp((int) (alpha * 0.9f), 0, 150);
        int deepAlpha = MathHelper.clamp((int) (alpha * 0.9f), 0, 142);
        int baseAlpha = MathHelper.clamp((int) (alpha * 0.78f), 0, 126);

        iceTriangleGradient(matrix, shape.tip, shape.right, shape.up, tipColor, brightSideColor, tipColor, tipAlpha, sideAlpha, tipAlpha);
        iceTriangleGradient(matrix, shape.tip, shape.up, shape.left, tipColor, tipColor, coolSideColor, tipAlpha, MathHelper.clamp((int) (tipAlpha * 0.92f), 0, 172), coolAlpha);
        iceTriangleGradient(matrix, shape.tip, shape.left, shape.down, tipColor, coolSideColor, deepColor, MathHelper.clamp((int) (tipAlpha * 0.96f), 0, 172), coolAlpha, deepAlpha);
        iceTriangleGradient(matrix, shape.tip, shape.down, shape.right, tipColor, deepColor, brightSideColor, tipAlpha, deepAlpha, sideAlpha);
        iceTriangleGradient(matrix, shape.right, shape.up, shape.left, brightSideColor, tipColor, coolSideColor, MathHelper.clamp((int) (sideAlpha * 0.72f), 0, 135), MathHelper.clamp((int) (sideAlpha * 0.76f), 0, 138), MathHelper.clamp((int) (coolAlpha * 0.8f), 0, 125));
        iceTriangleGradient(matrix, shape.right, shape.left, shape.down, brightSideColor, coolSideColor, baseShadeColor, MathHelper.clamp((int) (sideAlpha * 0.62f), 0, 120), MathHelper.clamp((int) (coolAlpha * 0.7f), 0, 112), MathHelper.clamp((int) (baseAlpha * 0.82f), 0, 108));
    }

    private void addIceCrystalShaderGlowFaces(Matrix4f matrix, Vector3d pos, Vector3d direction, float length, float radius,
                                              int baseColor, int alpha, float roll, float hitProgress) {
        IceShape shape = makeIceShape(pos, direction, length, radius, roll);
        int secondColor = getIceThemeSecondColor();
        int tipColor = getIceThemeHighlightColor(baseColor, 0.72f);
        int sideColor = ColorUtils.brighter(ColorUtils.interpolateColor(baseColor, secondColor, 0.28f), 0.32f);
        int deepColor = ColorUtils.darker(ColorUtils.interpolateColor(baseColor, secondColor, 0.52f), 0.28f + hitProgress * 0.12f);
        int blackIce = getIceThemeShadowColor(baseColor, 0.48f + hitProgress * 0.24f);

        int tipAlpha = MathHelper.clamp((int) (alpha * 1.08f), 0, 165);
        int sideAlpha = MathHelper.clamp(alpha, 0, 145);
        int deepAlpha = MathHelper.clamp((int) (alpha * 0.72f), 0, 105);
        int fadeAlpha = MathHelper.clamp((int) (alpha * 0.3f), 0, 62);

        iceTriangleGradient(matrix, shape.tip, shape.right, shape.up, tipColor, sideColor, tipColor, tipAlpha, sideAlpha, tipAlpha);
        iceTriangleGradient(matrix, shape.tip, shape.up, shape.left, tipColor, sideColor, deepColor, tipAlpha, sideAlpha, deepAlpha);
        iceTriangleGradient(matrix, shape.tip, shape.left, shape.down, tipColor, deepColor, blackIce, tipAlpha, deepAlpha, fadeAlpha);
        iceTriangleGradient(matrix, shape.tip, shape.down, shape.right, tipColor, blackIce, sideColor, tipAlpha, fadeAlpha, sideAlpha);
        iceTriangleGradient(matrix, shape.right, shape.up, shape.left, sideColor, tipColor, deepColor, fadeAlpha, fadeAlpha, deepAlpha);
        iceTriangleGradient(matrix, shape.right, shape.left, shape.down, sideColor, deepColor, blackIce, fadeAlpha, deepAlpha, fadeAlpha);
    }

    private void addIceCrystalEdges(Matrix4f matrix, Vector3d pos, Vector3d direction, float length, float radius,
                                    int color, int alpha) {
        addIceCrystalEdges(matrix, pos, direction, length, radius, color, alpha, 0.0f);
    }

    private void addIceCrystalEdges(Matrix4f matrix, Vector3d pos, Vector3d direction, float length, float radius,
                                    int color, int alpha, float roll) {
        IceShape shape = makeIceShape(pos, direction, length, radius, roll);
        int r = ColorUtils.getRed(color);
        int g = ColorUtils.getGreen(color);
        int b = ColorUtils.getBlue(color);
        iceLine(matrix, shape.tip, shape.right, r, g, b, alpha);
        iceLine(matrix, shape.tip, shape.up, r, g, b, alpha);
        iceLine(matrix, shape.tip, shape.left, r, g, b, alpha);
        iceLine(matrix, shape.tip, shape.down, r, g, b, alpha);
        iceLine(matrix, shape.right, shape.up, r, g, b, alpha / 2);
        iceLine(matrix, shape.up, shape.left, r, g, b, alpha / 2);
        iceLine(matrix, shape.left, shape.down, r, g, b, alpha / 2);
        iceLine(matrix, shape.down, shape.right, r, g, b, alpha / 2);
    }

    private void addIceCrystalGlowMaskEdges(Matrix4f matrix, Vector3d pos, Vector3d direction, float length, float radius,
                                            int color, int alpha, float roll, long now, int index) {
        int maskColor = getIceThemeHighlightColor(color, 0.72f);
        addIceCrystalSoftEdges(matrix, pos, direction, length, radius, maskColor, alpha, roll, true);
        addIceCrystalAnimatedOuterEdges(matrix, pos, direction, length * 1.02f, radius * 1.04f, maskColor,
                MathHelper.clamp((int) (alpha * 0.72f), 0, 190), roll, now, index);
    }

    private void addIceCrystalAnimatedOuterEdges(Matrix4f matrix, Vector3d pos, Vector3d direction, float length, float radius,
                                                 int color, int alpha, float roll, long now, int index) {
        if (alpha <= 0) return;
        IceShape shape = makeIceShape(pos, direction, length, radius, roll);
        int hotColor = getIceThemeHighlightColor(color, 0.78f);
        float travel = (float) ((now * 0.00105 + index * 0.173) % 1.0);
        float secondaryTravel = travel + 0.43f;
        if (secondaryTravel > 1.0f) secondaryTravel -= 1.0f;

        addIceTravelLine(matrix, shape.tip, shape.right, travel, color, hotColor, alpha);
        addIceTravelLine(matrix, shape.tip, shape.up, secondaryTravel, color, hotColor, MathHelper.clamp((int) (alpha * 0.86f), 0, 255));
        addIceTravelLine(matrix, shape.tip, shape.left, 1.0f - travel, color, hotColor, MathHelper.clamp((int) (alpha * 0.72f), 0, 255));
        addIceTravelLine(matrix, shape.tip, shape.down, 1.0f - secondaryTravel, color, hotColor, MathHelper.clamp((int) (alpha * 0.66f), 0, 255));
    }

    private void addIceTravelLine(Matrix4f matrix, Vector3d a, Vector3d b, float center, int baseColor, int hotColor, int alpha) {
        float half = 0.18f;
        float start = MathHelper.clamp(center - half, 0.0f, 1.0f);
        float end = MathHelper.clamp(center + half, 0.0f, 1.0f);
        if (end - start < 0.035f) return;

        float mid = MathHelper.clamp(center, start, end);
        int edgeAlpha = MathHelper.clamp((int) (alpha * 0.12f), 0, 255);
        iceLineGradientLerp(matrix, a, b, start, mid, baseColor, hotColor, edgeAlpha, alpha);
        iceLineGradientLerp(matrix, a, b, mid, end, hotColor, baseColor, alpha, edgeAlpha);
    }

    private void iceLineGradientLerp(Matrix4f matrix, Vector3d a, Vector3d b, float start, float end,
                                     int colorA, int colorB, int alphaA, int alphaB) {
        iceVertexLerp(matrix, a, b, start, colorA, alphaA);
        iceVertexLerp(matrix, a, b, end, colorB, alphaB);
    }

    private void iceVertexLerp(Matrix4f matrix, Vector3d a, Vector3d b, float value, int color, int alpha) {
        BUILDER.pos(matrix,
                        (float) (a.x + (b.x - a.x) * value),
                        (float) (a.y + (b.y - a.y) * value),
                        (float) (a.z + (b.z - a.z) * value))
                .color(ColorUtils.getRed(color), ColorUtils.getGreen(color), ColorUtils.getBlue(color), MathHelper.clamp(alpha, 0, 255))
                .endVertex();
    }

    private void addIceCrystalSoftEdges(Matrix4f matrix, Vector3d pos, Vector3d direction, float length, float radius,
                                        int color, int alpha, float roll, boolean outer) {
        IceShape shape = makeIceShape(pos, direction, length, radius, roll);
        int outlineColor = getIceThemeHighlightColor(color, outer ? 0.34f : 0.58f);
        int sideColor = ColorUtils.interpolateColor(color, outlineColor, outer ? 0.24f : 0.4f);
        int deepColor = ColorUtils.interpolateColor(color, outlineColor, outer ? 0.16f : 0.28f);

        int tipAlpha = MathHelper.clamp(outer ? (int) (alpha * 0.72f) : alpha, 0, 255);
        int sideAlpha = MathHelper.clamp(outer ? (int) (alpha * 0.46f) : (int) (alpha * 0.94f), 0, 245);
        int darkAlpha = MathHelper.clamp(outer ? (int) (alpha * 0.34f) : (int) (alpha * 0.86f), 0, 235);
        int cornerAlpha = MathHelper.clamp(outer ? (int) (alpha * 0.24f) : (int) (alpha * 0.7f), 0, 210);

        iceLineGradient(matrix, shape.tip, shape.right, outlineColor, sideColor, tipAlpha, sideAlpha);
        iceLineGradient(matrix, shape.tip, shape.up, outlineColor, outlineColor, tipAlpha, MathHelper.clamp((int) (sideAlpha * 1.08f), 0, 235));
        iceLineGradient(matrix, shape.tip, shape.left, outlineColor, deepColor, MathHelper.clamp((int) (tipAlpha * 0.92f), 0, 245), darkAlpha);
        iceLineGradient(matrix, shape.tip, shape.down, outlineColor, deepColor, MathHelper.clamp((int) (tipAlpha * 0.86f), 0, 240), darkAlpha);

        iceLineGradient(matrix, shape.right, shape.up, sideColor, outlineColor, cornerAlpha, MathHelper.clamp((int) (cornerAlpha * 0.82f), 0, 180));
        iceLineGradient(matrix, shape.up, shape.left, outlineColor, deepColor, MathHelper.clamp((int) (cornerAlpha * 0.8f), 0, 180), cornerAlpha);
        iceLineGradient(matrix, shape.left, shape.down, deepColor, deepColor, MathHelper.clamp((int) (cornerAlpha * 0.7f), 0, 180), MathHelper.clamp((int) (cornerAlpha * 0.55f), 0, 180));
        iceLineGradient(matrix, shape.down, shape.right, deepColor, sideColor, MathHelper.clamp((int) (cornerAlpha * 0.62f), 0, 180), cornerAlpha);
    }

    private IceShape makeIceShape(Vector3d pos, Vector3d direction, float length, float radius) {
        return makeIceShape(pos, direction, length, radius, 0.0f);
    }

    private IceShape makeIceShape(Vector3d pos, Vector3d direction, float length, float radius, float roll) {
        boolean vertical = Math.abs(direction.y) > 0.82;
        double fallbackX = vertical ? 1.0 : 0.0;
        double fallbackY = vertical ? 0.0 : 1.0;
        double fallbackZ = 0.0;

        double rightX = direction.y * fallbackZ - direction.z * fallbackY;
        double rightY = direction.z * fallbackX - direction.x * fallbackZ;
        double rightZ = direction.x * fallbackY - direction.y * fallbackX;
        double rightLength = Math.sqrt(rightX * rightX + rightY * rightY + rightZ * rightZ);
        if (rightLength < 1.0E-5) {
            rightX = 1.0;
            rightY = 0.0;
            rightZ = 0.0;
        } else {
            rightX /= rightLength;
            rightY /= rightLength;
            rightZ /= rightLength;
        }

        double upX = rightY * direction.z - rightZ * direction.y;
        double upY = rightZ * direction.x - rightX * direction.z;
        double upZ = rightX * direction.y - rightY * direction.x;
        double upLength = Math.sqrt(upX * upX + upY * upY + upZ * upZ);
        if (upLength < 1.0E-5) {
            upX = 0.0;
            upY = 1.0;
            upZ = 0.0;
        } else {
            upX /= upLength;
            upY /= upLength;
            upZ /= upLength;
        }

        double sin = Math.sin(roll);
        double cos = Math.cos(roll);

        double rolledRightX = rightX * cos + upX * sin;
        double rolledRightY = rightY * cos + upY * sin;
        double rolledRightZ = rightZ * cos + upZ * sin;
        double rolledRightLength = Math.sqrt(rolledRightX * rolledRightX + rolledRightY * rolledRightY + rolledRightZ * rolledRightZ);
        if (rolledRightLength < 1.0E-5) {
            rolledRightX = rightX;
            rolledRightY = rightY;
            rolledRightZ = rightZ;
        } else {
            rolledRightX /= rolledRightLength;
            rolledRightY /= rolledRightLength;
            rolledRightZ /= rolledRightLength;
        }

        double rolledUpX = upX * cos - rightX * sin;
        double rolledUpY = upY * cos - rightY * sin;
        double rolledUpZ = upZ * cos - rightZ * sin;
        double rolledUpLength = Math.sqrt(rolledUpX * rolledUpX + rolledUpY * rolledUpY + rolledUpZ * rolledUpZ);
        if (rolledUpLength < 1.0E-5) {
            rolledUpX = upX;
            rolledUpY = upY;
            rolledUpZ = upZ;
        } else {
            rolledUpX /= rolledUpLength;
            rolledUpY /= rolledUpLength;
            rolledUpZ /= rolledUpLength;
        }

        double tipScale = length * 0.74;
        double baseScale = -length * 0.24;
        double baseX = pos.x + direction.x * baseScale;
        double baseY = pos.y + direction.y * baseScale;
        double baseZ = pos.z + direction.z * baseScale;
        double upRadius = radius * 0.86;

        Vector3d tip = new Vector3d(pos.x + direction.x * tipScale, pos.y + direction.y * tipScale, pos.z + direction.z * tipScale);
        Vector3d base = new Vector3d(baseX, baseY, baseZ);
        Vector3d right = new Vector3d(baseX + rolledRightX * radius, baseY + rolledRightY * radius, baseZ + rolledRightZ * radius);
        Vector3d left = new Vector3d(baseX - rolledRightX * radius, baseY - rolledRightY * radius, baseZ - rolledRightZ * radius);
        Vector3d up = new Vector3d(baseX + rolledUpX * upRadius, baseY + rolledUpY * upRadius, baseZ + rolledUpZ * upRadius);
        Vector3d down = new Vector3d(baseX - rolledUpX * upRadius, baseY - rolledUpY * upRadius, baseZ - rolledUpZ * upRadius);
        return new IceShape(tip, base, right, left, up, down);
    }

    private void iceTriangle(Matrix4f matrix, Vector3d a, Vector3d b, Vector3d c, int r, int g, int blue, int alpha) {
        BUILDER.pos(matrix, (float) a.x, (float) a.y, (float) a.z).color(r, g, blue, alpha).endVertex();
        BUILDER.pos(matrix, (float) b.x, (float) b.y, (float) b.z).color(r, g, blue, Math.max(0, alpha - 8)).endVertex();
        BUILDER.pos(matrix, (float) c.x, (float) c.y, (float) c.z).color(r, g, blue, Math.max(0, alpha - 14)).endVertex();
    }

    private void iceTriangleRaw(Matrix4f matrix, Vector3d a, Vector3d b, Vector3d c,
                                int r, int g, int blue, int alphaA, int alphaB, int alphaC) {
        BUILDER.pos(matrix, (float) a.x, (float) a.y, (float) a.z).color(r, g, blue, alphaA).endVertex();
        BUILDER.pos(matrix, (float) b.x, (float) b.y, (float) b.z).color(r, g, blue, alphaB).endVertex();
        BUILDER.pos(matrix, (float) c.x, (float) c.y, (float) c.z).color(r, g, blue, alphaC).endVertex();
    }

    private void iceTriangleGradient(Matrix4f matrix, Vector3d a, Vector3d b, Vector3d c,
                                     int colorA, int colorB, int colorC, int alphaA, int alphaB, int alphaC) {
        iceVertex(matrix, a, colorA, alphaA);
        iceVertex(matrix, b, colorB, alphaB);
        iceVertex(matrix, c, colorC, alphaC);
    }

    private void iceLine(Matrix4f matrix, Vector3d a, Vector3d b, int r, int g, int blue, int alpha) {
        BUILDER.pos(matrix, (float) a.x, (float) a.y, (float) a.z).color(r, g, blue, alpha).endVertex();
        BUILDER.pos(matrix, (float) b.x, (float) b.y, (float) b.z).color(r, g, blue, alpha).endVertex();
    }

    private void iceLineGradient(Matrix4f matrix, Vector3d a, Vector3d b, int colorA, int colorB, int alphaA, int alphaB) {
        iceVertex(matrix, a, colorA, alphaA);
        iceVertex(matrix, b, colorB, alphaB);
    }

    private void iceVertex(Matrix4f matrix, Vector3d pos, int color, int alpha) {
        BUILDER.pos(matrix, (float) pos.x, (float) pos.y, (float) pos.z)
                .color(ColorUtils.getRed(color), ColorUtils.getGreen(color), ColorUtils.getBlue(color), MathHelper.clamp(alpha, 0, 255))
                .endVertex();
    }

    private Vector3d lerpIce(Vector3d a, Vector3d b, float value) {
        return new Vector3d(
                a.x + (b.x - a.x) * value,
                a.y + (b.y - a.y) * value,
                a.z + (b.z - a.z) * value
        );
    }

    private boolean ensureIceGlowPipeline() {
        if (iceKawaseDownProgram > 0 && iceKawaseUpProgram > 0 && iceOuterGlowProgram > 0) return true;
        if (iceGlowPipelineFailed) return false;

        String vertex = "#version 120\n" +
                "void main() {\n" +
                "    gl_TexCoord[0] = gl_MultiTexCoord0;\n" +
                "    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;\n" +
                "}";

        String kawaseDown = "#version 120\n" +
                "uniform sampler2D inTexture;\n" +
                "uniform vec2 uOffset, uHalfPixel, uSize;\n" +
                "void main() {\n" +
                "    vec2 uv = gl_TexCoord[0].xy;\n" +
                "    vec2 halfPixel = uHalfPixel * uOffset;\n" +
                "    vec4 sum = texture2D(inTexture, uv) * 4.0;\n" +
                "    sum += texture2D(inTexture, uv - halfPixel);\n" +
                "    sum += texture2D(inTexture, uv + halfPixel);\n" +
                "    sum += texture2D(inTexture, uv + vec2(halfPixel.x, -halfPixel.y));\n" +
                "    sum += texture2D(inTexture, uv - vec2(halfPixel.x, -halfPixel.y));\n" +
                "    gl_FragColor = sum / 8.0;\n" +
                "}";

        String kawaseUp = "#version 120\n" +
                "uniform sampler2D inTexture;\n" +
                "uniform vec2 uOffset, uHalfPixel, uSize;\n" +
                "uniform vec3 color;\n" +
                "void main() {\n" +
                "    vec2 uv = gl_TexCoord[0].xy;\n" +
                "    vec2 halfPixel = uHalfPixel * uOffset;\n" +
                "    vec4 sum = texture2D(inTexture, uv + vec2(-halfPixel.x * 2.0, 0.0));\n" +
                "    sum += texture2D(inTexture, uv + vec2(-halfPixel.x, halfPixel.y)) * 2.0;\n" +
                "    sum += texture2D(inTexture, uv + vec2(0.0, halfPixel.y * 2.0));\n" +
                "    sum += texture2D(inTexture, uv + vec2(halfPixel.x, halfPixel.y)) * 2.0;\n" +
                "    sum += texture2D(inTexture, uv + vec2(halfPixel.x * 2.0, 0.0));\n" +
                "    sum += texture2D(inTexture, uv + vec2(halfPixel.x, -halfPixel.y)) * 2.0;\n" +
                "    sum += texture2D(inTexture, uv + vec2(0.0, -halfPixel.y * 2.0));\n" +
                "    sum += texture2D(inTexture, uv + vec2(-halfPixel.x, -halfPixel.y)) * 2.0;\n" +
                "    vec4 result = sum / 12.0;\n" +
                "    gl_FragColor = vec4(result.rgb * color, result.a);\n" +
                "}";

        String outerGlow = "#version 120\n" +
                "uniform sampler2D bloomTexture;\n" +
                "uniform sampler2D maskTexture;\n" +
                "uniform vec3 glowColor1;\n" +
                "uniform vec3 glowColor2;\n" +
                "void main() {\n" +
                "    vec2 uv = gl_TexCoord[0].xy;\n" +
                "    vec4 bloom = texture2D(bloomTexture, uv);\n" +
                "    vec4 mask = texture2D(maskTexture, uv);\n" +
                "    float intensity = bloom.a * (1.0 - mask.a);\n" +
                "    vec3 gradientColor = mix(glowColor1, glowColor2, uv.y);\n" +
                "    gl_FragColor = vec4(gradientColor, intensity * gl_Color.a);\n" +
                "}";

        iceKawaseDownProgram = createIceGlowProgram(vertex, kawaseDown);
        iceKawaseUpProgram = createIceGlowProgram(vertex, kawaseUp);
        iceOuterGlowProgram = createIceGlowProgram(vertex, outerGlow);
        iceGlowPipelineFailed = iceKawaseDownProgram <= 0 || iceKawaseUpProgram <= 0 || iceOuterGlowProgram <= 0;
        if (!iceGlowPipelineFailed) {
            iceKawaseDownTextureUniform = GL20.glGetUniformLocation(iceKawaseDownProgram, "inTexture");
            iceKawaseDownSizeUniform = GL20.glGetUniformLocation(iceKawaseDownProgram, "uSize");
            iceKawaseDownOffsetUniform = GL20.glGetUniformLocation(iceKawaseDownProgram, "uOffset");
            iceKawaseDownHalfPixelUniform = GL20.glGetUniformLocation(iceKawaseDownProgram, "uHalfPixel");
            iceKawaseUpTextureUniform = GL20.glGetUniformLocation(iceKawaseUpProgram, "inTexture");
            iceKawaseUpSizeUniform = GL20.glGetUniformLocation(iceKawaseUpProgram, "uSize");
            iceKawaseUpOffsetUniform = GL20.glGetUniformLocation(iceKawaseUpProgram, "uOffset");
            iceKawaseUpHalfPixelUniform = GL20.glGetUniformLocation(iceKawaseUpProgram, "uHalfPixel");
            iceKawaseUpColorUniform = GL20.glGetUniformLocation(iceKawaseUpProgram, "color");
            iceOuterBloomTextureUniform = GL20.glGetUniformLocation(iceOuterGlowProgram, "bloomTexture");
            iceOuterMaskTextureUniform = GL20.glGetUniformLocation(iceOuterGlowProgram, "maskTexture");
            iceOuterGlowColor1Uniform = GL20.glGetUniformLocation(iceOuterGlowProgram, "glowColor1");
            iceOuterGlowColor2Uniform = GL20.glGetUniformLocation(iceOuterGlowProgram, "glowColor2");
        }
        return !iceGlowPipelineFailed;
    }

    private int createIceGlowProgram(String vertexSource, String fragmentSource) {
        int vertexShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vertexShader, vertexSource);
        GL20.glCompileShader(vertexShader);
        if (GL20.glGetShaderi(vertexShader, GL20.GL_COMPILE_STATUS) == 0) {
            GL20.glDeleteShader(vertexShader);
            return -1;
        }

        int fragmentShader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fragmentShader, fragmentSource);
        GL20.glCompileShader(fragmentShader);
        if (GL20.glGetShaderi(fragmentShader, GL20.GL_COMPILE_STATUS) == 0) {
            GL20.glDeleteShader(vertexShader);
            GL20.glDeleteShader(fragmentShader);
            return -1;
        }

        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vertexShader);
        GL20.glAttachShader(program, fragmentShader);
        GL20.glLinkProgram(program);
        GL20.glDeleteShader(vertexShader);
        GL20.glDeleteShader(fragmentShader);
        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == 0) {
            GL20.glDeleteProgram(program);
            return -1;
        }
        return program;
    }

    private Framebuffer getIceGlowMaskBuffer() {
        int width = mc.getMainWindow().getFramebufferWidth();
        int height = mc.getMainWindow().getFramebufferHeight();
        if (iceGlowMaskBuffer == null) {
            iceGlowMaskBuffer = new Framebuffer(width, height, true, Minecraft.IS_RUNNING_ON_MAC);
            iceGlowMaskBuffer.setFramebufferFilter(GL11.GL_LINEAR);
            iceGlowMaskBuffer.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
        }
        if (iceGlowMaskBuffer.framebufferWidth != width || iceGlowMaskBuffer.framebufferHeight != height) {
            iceGlowMaskBuffer.resize(width, height, Minecraft.IS_RUNNING_ON_MAC);
            iceGlowMaskBuffer.setFramebufferFilter(GL11.GL_LINEAR);
        }
        return iceGlowMaskBuffer;
    }

    private int getTextureId(Framebuffer framebuffer) {
        return framebuffer.func_242996_f();
    }

    private void copyIceDepth(Framebuffer source, Framebuffer target) {
        if (source.depthBuffer <= 0 || target.depthBuffer <= 0) return;
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, source.framebufferObject);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, target.framebufferObject);
        GL30.glBlitFramebuffer(0, 0, source.framebufferWidth, source.framebufferHeight,
                0, 0, target.framebufferWidth, target.framebufferHeight,
                GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST);
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, target.framebufferObject);
    }

    private int generateIceKawaseBloom(int iterations) {
        setupIceBloomBuffers(iterations);
        int currentTexture = getTextureId(iceGlowMaskBuffer);
        int offset = 1;

        GL20.glUseProgram(iceKawaseDownProgram);
        GL20.glUniform1i(iceKawaseDownTextureUniform, 0);
        for (int i = 0; i < iterations; i++) {
            Framebuffer buffer = iceBloomBuffers.get(i);
            buffer.framebufferClear(Minecraft.IS_RUNNING_ON_MAC);
            buffer.bindFramebuffer(true);
            GL20.glUniform2f(iceKawaseDownSizeUniform, buffer.framebufferWidth, buffer.framebufferHeight);
            GL20.glUniform2f(iceKawaseDownOffsetUniform, offset + i, offset + i);
            GL20.glUniform2f(iceKawaseDownHalfPixelUniform, 0.5f / buffer.framebufferWidth, 0.5f / buffer.framebufferHeight);
            GlStateManager.activeTexture(GL13.GL_TEXTURE0);
            GlStateManager.bindTexture(currentTexture);
            drawIceScreenQuad();
            currentTexture = getTextureId(buffer);
        }

        GL20.glUseProgram(iceKawaseUpProgram);
        GL20.glUniform1i(iceKawaseUpTextureUniform, 0);
        for (int i = iterations - 1; i >= 1; i--) {
            Framebuffer buffer = iceBloomBuffers.get(i - 1);
            buffer.bindFramebuffer(true);
            GL20.glUniform2f(iceKawaseUpSizeUniform, buffer.framebufferWidth, buffer.framebufferHeight);
            GL20.glUniform2f(iceKawaseUpOffsetUniform, offset + i, offset + i);
            GL20.glUniform2f(iceKawaseUpHalfPixelUniform, 0.5f / buffer.framebufferWidth, 0.5f / buffer.framebufferHeight);
            GL20.glUniform3f(iceKawaseUpColorUniform, 1.0f, 1.0f, 1.0f);
            GlStateManager.activeTexture(GL13.GL_TEXTURE0);
            GlStateManager.bindTexture(currentTexture);
            drawIceScreenQuad();
            currentTexture = getTextureId(buffer);
        }

        GL20.glUseProgram(0);
        mc.getFramebuffer().bindFramebuffer(true);
        return currentTexture;
    }

    private void renderIceOuterGlow(int bloomTexture, int maskTexture, float alphaValue, float hitProgress, int baseColor) {
        int glowColor1 = getIceThemeHighlightColor(baseColor, 0.64f);
        int glowColor2 = ColorUtils.brighter(ColorUtils.interpolateColor(baseColor, getIceThemeSecondColor(), 0.58f), hitProgress * 0.18f);
        float exposure = (1.5f + hitProgress * 0.45f) * alphaValue;

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL20.glUseProgram(iceOuterGlowProgram);
        GL20.glUniform1i(iceOuterBloomTextureUniform, 0);
        GL20.glUniform1i(iceOuterMaskTextureUniform, 1);
        GL20.glUniform3f(iceOuterGlowColor1Uniform,
                ColorUtils.getRed(glowColor1) / 255.0f,
                ColorUtils.getGreen(glowColor1) / 255.0f,
                ColorUtils.getBlue(glowColor1) / 255.0f);
        GL20.glUniform3f(iceOuterGlowColor2Uniform,
                ColorUtils.getRed(glowColor2) / 255.0f,
                ColorUtils.getGreen(glowColor2) / 255.0f,
                ColorUtils.getBlue(glowColor2) / 255.0f);
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, exposure);
        GlStateManager.activeTexture(GL13.GL_TEXTURE1);
        GlStateManager.bindTexture(maskTexture);
        GlStateManager.activeTexture(GL13.GL_TEXTURE0);
        GlStateManager.bindTexture(bloomTexture);
        drawIceScreenQuad();
        GL20.glUseProgram(0);
        GlStateManager.activeTexture(GL13.GL_TEXTURE1);
        GlStateManager.bindTexture(0);
        GlStateManager.activeTexture(GL13.GL_TEXTURE0);
        GlStateManager.bindTexture(0);
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void setupIceBloomBuffers(int iterations) {
        int framebufferWidth = mc.getMainWindow().getFramebufferWidth();
        int framebufferHeight = mc.getMainWindow().getFramebufferHeight();
        if (iceBloomBuffers.size() != iterations) {
            for (Framebuffer framebuffer : iceBloomBuffers) {
                framebuffer.deleteFramebuffer();
            }
            iceBloomBuffers.clear();
            for (int i = 0; i < iterations; i++) {
                int divisor = 1 << (i + 1);
                int width = Math.max(2, framebufferWidth / divisor);
                int height = Math.max(2, framebufferHeight / divisor);
                Framebuffer framebuffer = new Framebuffer(width, height, false, Minecraft.IS_RUNNING_ON_MAC);
                framebuffer.setFramebufferFilter(GL11.GL_LINEAR);
                framebuffer.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
                iceBloomBuffers.add(framebuffer);
            }
        }
        for (int i = 0; i < iterations; i++) {
            int divisor = 1 << (i + 1);
            int width = Math.max(2, framebufferWidth / divisor);
            int height = Math.max(2, framebufferHeight / divisor);
            Framebuffer framebuffer = iceBloomBuffers.get(i);
            if (framebuffer.framebufferWidth != width || framebuffer.framebufferHeight != height) {
                framebuffer.resize(width, height, Minecraft.IS_RUNNING_ON_MAC);
                framebuffer.setFramebufferFilter(GL11.GL_LINEAR);
            }
        }
    }

    private void drawIceScreenQuad() {
        float width = mc.getMainWindow().getScaledWidth();
        float height = mc.getMainWindow().getScaledHeight();
        BUILDER.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        BUILDER.pos(0, height, 0).tex(0, 0).endVertex();
        BUILDER.pos(width, height, 0).tex(1, 0).endVertex();
        BUILDER.pos(width, 0, 0).tex(1, 1).endVertex();
        BUILDER.pos(0, 0, 0).tex(0, 1).endVertex();
        TESSELLATOR.draw();
    }

    private void setupIceGlow2D() {
        RenderSystem.matrixMode(GL11.GL_PROJECTION);
        RenderSystem.pushMatrix();
        RenderSystem.loadIdentity();
        RenderSystem.ortho(0.0D, mc.getMainWindow().getScaledWidth(), mc.getMainWindow().getScaledHeight(), 0.0D, 1000.0D, 3000.0D);
        RenderSystem.matrixMode(GL11.GL_MODELVIEW);
        RenderSystem.pushMatrix();
        RenderSystem.loadIdentity();
        RenderSystem.translatef(0.0F, 0.0F, -2000.0F);
        RenderSystem.disableDepthTest();
        RenderSystem.disableAlphaTest();
        RenderSystem.enableTexture();
    }

    private void restoreIceGlow3D() {
        RenderSystem.enableDepthTest();
        RenderSystem.enableAlphaTest();
        RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.bindTexture(0);
        RenderSystem.matrixMode(GL11.GL_PROJECTION);
        RenderSystem.popMatrix();
        RenderSystem.matrixMode(GL11.GL_MODELVIEW);
        RenderSystem.popMatrix();
    }

    private void cleanupIceGlowShader() {
        if (iceKawaseDownProgram > 0) GL20.glDeleteProgram(iceKawaseDownProgram);
        if (iceKawaseUpProgram > 0) GL20.glDeleteProgram(iceKawaseUpProgram);
        if (iceOuterGlowProgram > 0) GL20.glDeleteProgram(iceOuterGlowProgram);
        iceKawaseDownProgram = -1;
        iceKawaseUpProgram = -1;
        iceOuterGlowProgram = -1;
        iceKawaseDownTextureUniform = -1;
        iceKawaseDownSizeUniform = -1;
        iceKawaseDownOffsetUniform = -1;
        iceKawaseDownHalfPixelUniform = -1;
        iceKawaseUpTextureUniform = -1;
        iceKawaseUpSizeUniform = -1;
        iceKawaseUpOffsetUniform = -1;
        iceKawaseUpHalfPixelUniform = -1;
        iceKawaseUpColorUniform = -1;
        iceOuterBloomTextureUniform = -1;
        iceOuterMaskTextureUniform = -1;
        iceOuterGlowColor1Uniform = -1;
        iceOuterGlowColor2Uniform = -1;
        iceGlowPipelineFailed = false;
        if (iceGlowMaskBuffer != null) {
            iceGlowMaskBuffer.deleteFramebuffer();
            iceGlowMaskBuffer = null;
        }
        for (Framebuffer framebuffer : iceBloomBuffers) {
            framebuffer.deleteFramebuffer();
        }
        iceBloomBuffers.clear();
    }

    private Vector3d addIce(Vector3d a, Vector3d b) {
        return new Vector3d(a.x + b.x, a.y + b.y, a.z + b.z);
    }

    private Vector3d scaleIce(Vector3d v, double value) {
        return new Vector3d(v.x * value, v.y * value, v.z * value);
    }

    private Vector3d crossIce(Vector3d a, Vector3d b) {
        return new Vector3d(a.y * b.z - a.z * b.y, a.z * b.x - a.x * b.z, a.x * b.y - a.y * b.x);
    }

    private Vector3d normalizeIce(Vector3d vector, Vector3d fallback) {
        return normalizeIce(vector.x, vector.y, vector.z, fallback);
    }

    private Vector3d normalizeIce(double x, double y, double z, Vector3d fallback) {
        double length = Math.sqrt(x * x + y * y + z * z);
        if (length < 1.0E-5) return fallback;
        return new Vector3d(x / length, y / length, z / length);
    }

    private float getIceTimedFade(long now, long start, long duration) {
        if (start < 0L) return 0.0f;
        float elapsed = now - start;
        if (elapsed >= duration) return 0.0f;
        float attack = Math.min(55.0f, duration * 0.28f);
        if (elapsed < attack) {
            return smoothIceStep(MathHelper.clamp(elapsed / attack, 0.0f, 1.0f));
        }
        float release = (elapsed - attack) / Math.max(1.0f, duration - attack);
        float fade = 1.0f - MathHelper.clamp(release, 0.0f, 1.0f);
        return smoothIceStep(fade);
    }

    private float smoothIceStep(float value) {
        return value * value * (3.0f - 2.0f * value);
    }

    private void spawnIceImpact(LivingEntity entity, boolean critical) {
        if (entity == null) return;
        Vector3d center = new Vector3d(entity.getPosX(), entity.getPosY() + entity.getHeight() * 0.53, entity.getPosZ());
        int count = critical ? 38 : 18;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < count; i++) {
            double angle = critical ? (Math.PI * 2.0 * i / count) : random.nextDouble(0.0, Math.PI * 2.0);
            double speed = critical ? random.nextDouble(0.07, 0.13) : random.nextDouble(0.035, 0.085);
            double mx = Math.cos(angle) * speed;
            double mz = Math.sin(angle) * speed;
            double my = critical ? random.nextDouble(-0.005, 0.018) : random.nextDouble(0.0, 0.055);
            if (!critical) {
                mx += random.nextDouble(-0.035, 0.035);
                mz += random.nextDouble(-0.035, 0.035);
            }
            iceSparks.add(new IceSparkParticle(center.x, center.y, center.z, mx, my, mz, critical));
        }
        while (iceSparks.size() > 140) {
            iceSparks.remove(0);
        }
    }

    private void spawnIceDeathEffect(LivingEntity entity) {
        if (entity == null) return;
        Vector3d center = new Vector3d(entity.getPosX(), entity.getPosY() + entity.getHeight() * 0.52, entity.getPosZ());
        Vector3d[] corners = getIceCrystalPositions(entity, center, 0.0f, System.currentTimeMillis());
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (Vector3d corner : corners) {
            Vector3d baseDirection = normalizeIce(corner.x - center.x, corner.y - center.y, corner.z - center.z, ICE_FALLBACK_UP);
            for (int i = 0; i < 6; i++) {
                double speed = random.nextDouble(0.055, 0.14);
                double mx = baseDirection.x * speed + random.nextDouble(-0.045, 0.045);
                double my = baseDirection.y * speed + random.nextDouble(0.025, 0.12);
                double mz = baseDirection.z * speed + random.nextDouble(-0.045, 0.045);
                iceShards.add(new IceShardParticle(
                        corner.x + random.nextDouble(-0.035, 0.035),
                        corner.y + random.nextDouble(-0.035, 0.035),
                        corner.z + random.nextDouble(-0.035, 0.035),
                        mx, my, mz,
                        (float) random.nextDouble(0.105, 0.18)
                ));
            }
        }
        while (iceShards.size() > 120) {
            iceShards.remove(0);
        }
    }

    private static class IceShape {
        final Vector3d tip;
        final Vector3d tail;
        final Vector3d right;
        final Vector3d left;
        final Vector3d up;
        final Vector3d down;

        IceShape(Vector3d tip, Vector3d tail, Vector3d right, Vector3d left, Vector3d up, Vector3d down) {
            this.tip = tip;
            this.tail = tail;
            this.right = right;
            this.left = left;
            this.up = up;
            this.down = down;
        }
    }

    private static class IceSparkParticle {
        double x, y, z;
        double motionX, motionY, motionZ;
        final long spawnTime = System.currentTimeMillis();
        final boolean critical;
        final long lifeTime;

        IceSparkParticle(double x, double y, double z, double motionX, double motionY, double motionZ, boolean critical) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.motionX = motionX;
            this.motionY = motionY;
            this.motionZ = motionZ;
            this.critical = critical;
            this.lifeTime = critical ? 520L : 340L;
        }

        void update() {
            x += motionX;
            y += motionY;
            z += motionZ;
            motionY -= critical ? 0.0045 : 0.007;
            motionX *= 0.94;
            motionY *= 0.96;
            motionZ *= 0.94;
        }

        float alpha() {
            return alpha(System.currentTimeMillis());
        }

        float alpha(long now) {
            float age = (now - spawnTime) / (float) lifeTime;
            float fade = 1.0f - MathHelper.clamp(age, 0.0f, 1.0f);
            return fade * fade;
        }

        boolean shouldRemove() {
            return shouldRemove(System.currentTimeMillis());
        }

        boolean shouldRemove(long now) {
            return now - spawnTime > lifeTime;
        }
    }

    private static class IceShardParticle {
        double x, y, z;
        double motionX, motionY, motionZ;
        final long spawnTime = System.currentTimeMillis();
        final long lifeTime = 620L;
        final float size;

        IceShardParticle(double x, double y, double z, double motionX, double motionY, double motionZ, float size) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.motionX = motionX;
            this.motionY = motionY;
            this.motionZ = motionZ;
            this.size = size;
        }

        void update() {
            x += motionX;
            y += motionY;
            z += motionZ;
            motionY -= 0.012;
            motionX *= 0.91;
            motionY *= 0.965;
            motionZ *= 0.91;
        }

        float alpha() {
            return alpha(System.currentTimeMillis());
        }

        float alpha(long now) {
            float age = (now - spawnTime) / (float) lifeTime;
            float fade = 1.0f - MathHelper.clamp(age, 0.0f, 1.0f);
            return fade * fade;
        }

        boolean shouldRemove() {
            return shouldRemove(System.currentTimeMillis());
        }

        boolean shouldRemove(long now) {
            return now - spawnTime > lifeTime;
        }
    }

    private class FallingCoffin {
        double x, y, z;
        float entityHeight;
        float entityWidth;
        long spawnTime;
        float fixedRotation;
        static final long FALL_DURATION = 700L;
        static final long TOTAL_DURATION = 6000L;

        public FallingCoffin(double x, double y, double z, float height, float width, float playerYaw) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.entityHeight = Math.max(height, 1.8f);
            this.entityWidth = Math.max(width, 0.6f);
            this.spawnTime = System.currentTimeMillis();
            this.fixedRotation = playerYaw;
        }

        public void update() {}

        public boolean shouldRemove() {
            return System.currentTimeMillis() - spawnTime > TOTAL_DURATION;
        }

        public void render(MatrixStack matrixStack) {
            long elapsed = System.currentTimeMillis() - spawnTime;
            float progress = elapsed / (float) TOTAL_DURATION;

            float fallProgress = Math.min(1.0f, elapsed / (float) FALL_DURATION);
            float fadeProgress = Math.max(0, progress - 0.85f) / 0.15f;
            float alphaVal = 1.0f - fadeProgress;

            if (alphaVal <= 0) return;

            float startHeight = 6.0f;
            float currentHeight = startHeight * (1.0f - easeOutBounce(fallProgress));

            float shake = 0;
            if (fallProgress >= 1.0f && elapsed < FALL_DURATION + 250) {
                float shakeProgress = (elapsed - FALL_DURATION) / 250.0f;
                shake = (float) Math.sin(shakeProgress * Math.PI * 8) * 0.025f * (1 - shakeProgress);
            }

            Vector3d camera = mc.getRenderManager().info.getProjectedView();

            double renderX = x - camera.x + shake;
            double renderY = y - camera.y + currentHeight;
            double renderZ = z - camera.z;

            matrixStack.push();

            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
            RenderSystem.disableTexture();
            RenderSystem.disableCull();
            RenderSystem.shadeModel(7425);

            matrixStack.translate(renderX, renderY, renderZ);
            matrixStack.rotate(Vector3f.YP.rotationDegrees(-fixedRotation));

            Matrix4f matrix = matrixStack.getLast().getMatrix();

            float coffinWidth = entityWidth * 1.2f;
            float coffinHeight = entityHeight * 1.3f;
            float coffinDepth = entityWidth * 0.6f;

            int baseAlpha = (int) (255 * alphaVal);

            renderCoffinBox(matrix, coffinWidth, coffinHeight, coffinDepth, baseAlpha);
            renderCrossOnCoffin(matrix, coffinWidth, coffinHeight, coffinDepth, baseAlpha);

            if (fallProgress >= 1.0f) {
                renderCoffinBlood(matrix, coffinWidth, elapsed, alphaVal);
            }

            RenderSystem.shadeModel(7424);
            RenderSystem.enableTexture();
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();

            matrixStack.pop();
        }

        private void renderCoffinBox(Matrix4f matrix, float w, float h, float d, int alpha) {
            int woodDark = 45;
            int woodMid = 70;
            int woodLight = 95;

            BUILDER.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            BUILDER.pos(matrix, -w/2, 0, -d/2).color(woodMid, woodMid/2 + 5, woodMid/4, alpha).endVertex();
            BUILDER.pos(matrix, w/2, 0, -d/2).color(woodMid, woodMid/2 + 5, woodMid/4, alpha).endVertex();
            BUILDER.pos(matrix, w/2, h, -d/2).color(woodLight, woodLight/2 + 10, woodLight/4 + 5, alpha).endVertex();
            BUILDER.pos(matrix, -w/2, h, -d/2).color(woodLight, woodLight/2 + 10, woodLight/4 + 5, alpha).endVertex();
            TESSELLATOR.draw();

            BUILDER.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            BUILDER.pos(matrix, w/2, 0, d/2).color(woodDark, woodDark/2, woodDark/4, alpha).endVertex();
            BUILDER.pos(matrix, -w/2, 0, d/2).color(woodDark, woodDark/2, woodDark/4, alpha).endVertex();
            BUILDER.pos(matrix, -w/2, h, d/2).color(woodMid-15, (woodMid-15)/2, (woodMid-15)/4, alpha).endVertex();
            BUILDER.pos(matrix, w/2, h, d/2).color(woodMid-15, (woodMid-15)/2, (woodMid-15)/4, alpha).endVertex();
            TESSELLATOR.draw();

            BUILDER.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            BUILDER.pos(matrix, -w/2, 0, d/2).color(woodDark-5, (woodDark-5)/2, (woodDark-5)/4, alpha).endVertex();
            BUILDER.pos(matrix, -w/2, 0, -d/2).color(woodDark-5, (woodDark-5)/2, (woodDark-5)/4, alpha).endVertex();
            BUILDER.pos(matrix, -w/2, h, -d/2).color(woodMid-20, (woodMid-20)/2, (woodMid-20)/4, alpha).endVertex();
            BUILDER.pos(matrix, -w/2, h, d/2).color(woodMid-20, (woodMid-20)/2, (woodMid-20)/4, alpha).endVertex();
            TESSELLATOR.draw();

            BUILDER.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            BUILDER.pos(matrix, w/2, 0, -d/2).color(woodDark+5, (woodDark+5)/2, (woodDark+5)/4, alpha).endVertex();
            BUILDER.pos(matrix, w/2, 0, d/2).color(woodDark+5, (woodDark+5)/2, (woodDark+5)/4, alpha).endVertex();
            BUILDER.pos(matrix, w/2, h, d/2).color(woodMid-10, (woodMid-10)/2, (woodMid-10)/4, alpha).endVertex();
            BUILDER.pos(matrix, w/2, h, -d/2).color(woodMid-10, (woodMid-10)/2, (woodMid-10)/4, alpha).endVertex();
            TESSELLATOR.draw();

            BUILDER.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            BUILDER.pos(matrix, -w/2, h, -d/2).color(woodLight+15, (woodLight+15)/2 + 10, (woodLight+15)/4 + 5, alpha).endVertex();
            BUILDER.pos(matrix, w/2, h, -d/2).color(woodLight+15, (woodLight+15)/2 + 10, (woodLight+15)/4 + 5, alpha).endVertex();
            BUILDER.pos(matrix, w/2, h, d/2).color(woodLight+5, (woodLight+5)/2 + 5, (woodLight+5)/4, alpha).endVertex();
            BUILDER.pos(matrix, -w/2, h, d/2).color(woodLight+5, (woodLight+5)/2 + 5, (woodLight+5)/4, alpha).endVertex();
            TESSELLATOR.draw();

            BUILDER.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            BUILDER.pos(matrix, -w/2, 0, d/2).color(woodDark-15, (woodDark-15)/2, (woodDark-15)/4, alpha).endVertex();
            BUILDER.pos(matrix, w/2, 0, d/2).color(woodDark-15, (woodDark-15)/2, (woodDark-15)/4, alpha).endVertex();
            BUILDER.pos(matrix, w/2, 0, -d/2).color(woodDark-15, (woodDark-15)/2, (woodDark-15)/4, alpha).endVertex();
            BUILDER.pos(matrix, -w/2, 0, -d/2).color(woodDark-15, (woodDark-15)/2, (woodDark-15)/4, alpha).endVertex();
            TESSELLATOR.draw();
        }

        private void renderCrossOnCoffin(Matrix4f matrix, float w, float h, float d, int alpha) {
            float crossWidth = 0.07f;
            float crossArmLen = w * 0.35f;
            float crossHeight = h * 0.45f;
            float crossY = h * 0.45f;
            float zOffset = -d/2 - 0.015f;

            int gold = 230;
            int goldG = 200;
            int goldB = 110;

            BUILDER.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            BUILDER.pos(matrix, -crossWidth, crossY, zOffset).color(gold-20, goldG-20, goldB-15, alpha).endVertex();
            BUILDER.pos(matrix, crossWidth, crossY, zOffset).color(gold-20, goldG-20, goldB-15, alpha).endVertex();
            BUILDER.pos(matrix, crossWidth, crossY + crossHeight, zOffset).color(gold+10, goldG+5, goldB+10, alpha).endVertex();
            BUILDER.pos(matrix, -crossWidth, crossY + crossHeight, zOffset).color(gold+10, goldG+5, goldB+10, alpha).endVertex();
            TESSELLATOR.draw();

            float armY = crossY + crossHeight * 0.68f;
            BUILDER.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            BUILDER.pos(matrix, -crossArmLen, armY - crossWidth, zOffset).color(gold-25, goldG-25, goldB-20, alpha).endVertex();
            BUILDER.pos(matrix, crossArmLen, armY - crossWidth, zOffset).color(gold-25, goldG-25, goldB-20, alpha).endVertex();
            BUILDER.pos(matrix, crossArmLen, armY + crossWidth, zOffset).color(gold+10, goldG+5, goldB+10, alpha).endVertex();
            BUILDER.pos(matrix, -crossArmLen, armY + crossWidth, zOffset).color(gold+10, goldG+5, goldB+10, alpha).endVertex();
            TESSELLATOR.draw();

            float ornSize = crossWidth * 1.2f;
            float topY = crossY + crossHeight;
            BUILDER.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
            BUILDER.pos(matrix, 0, topY + ornSize * 1.5f, zOffset - 0.001f).color(gold+15, goldG+10, goldB+15, alpha).endVertex();
            BUILDER.pos(matrix, -ornSize, topY, zOffset - 0.001f).color(gold, goldG, goldB, alpha).endVertex();
            BUILDER.pos(matrix, ornSize, topY, zOffset - 0.001f).color(gold, goldG, goldB, alpha).endVertex();
            TESSELLATOR.draw();
        }

        private void renderCoffinBlood(Matrix4f matrix, float w, long elapsed, float alphaVal) {
            float bloodSpread = Math.min(1.0f, (elapsed - FALL_DURATION) / 600.0f);
            int bloodAlpha = (int) (180 * alphaVal * bloodSpread);

            BUILDER.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
            BUILDER.pos(matrix, 0, 0.015f, 0).color(160, 25, 25, bloodAlpha).endVertex();
            int segments = 20;
            for (int i = 0; i <= segments; i++) {
                double angle = (2 * Math.PI * i) / segments;
                float radius = (w * 0.8f + (float) Math.sin(angle * 5) * 0.12f) * bloodSpread;
                float px = (float) Math.cos(angle) * radius;
                float pz = (float) Math.sin(angle) * radius;
                BUILDER.pos(matrix, px, 0.015f, pz).color(110, 12, 12, 0).endVertex();
            }
            TESSELLATOR.draw();
        }

        private float easeOutBounce(float t) {
            if (t < 1 / 2.75f) {
                return 7.5625f * t * t;
            } else if (t < 2 / 2.75f) {
                t -= 1.5f / 2.75f;
                return 7.5625f * t * t + 0.75f;
            } else if (t < 2.5 / 2.75f) {
                t -= 2.25f / 2.75f;
                return 7.5625f * t * t + 0.9375f;
            } else {
                t -= 2.625f / 2.75f;
                return 7.5625f * t * t + 0.984375f;
            }
        }
    }

    private class BloodParticle {
        double x, y, z;
        double vx, vy, vz;
        long spawnTime;
        boolean isBurst;
        static final long DURATION = 2500L;
        float size;

        public BloodParticle(double x, double y, double z, double vx, double vy, double vz, boolean isBurst) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
            this.spawnTime = System.currentTimeMillis();
            this.isBurst = isBurst;
            this.size = isBurst ? (0.1f + (float) Math.random() * 0.12f) : (0.06f + (float) Math.random() * 0.06f);
        }

        public void update() {
            this.vy -= 0.025f;
            this.x += vx;
            this.y += vy;
            this.z += vz;

            this.vx *= 0.96f;
            this.vz *= 0.96f;
        }

        public boolean shouldRemove() {
            return System.currentTimeMillis() - spawnTime > DURATION;
        }

        public void renderBatched(MatrixStack matrixStack, float yaw, float pitch) {
            float progress = (System.currentTimeMillis() - spawnTime) / (float) DURATION;
            float alphaVal = 1.0f;
            if (progress > 0.75f) {
                alphaVal = (1.0f - progress) / 0.25f;
            }

            Vector3d camera = mc.getRenderManager().info.getProjectedView();
            double renderX = x - camera.x;
            double renderY = y - camera.y;
            double renderZ = z - camera.z;

            matrixStack.push();

            matrixStack.translate(renderX, renderY, renderZ);

            matrixStack.rotate(Vector3f.YP.rotationDegrees(-yaw));
            matrixStack.rotate(Vector3f.XP.rotationDegrees(pitch));

            Matrix4f matrix = matrixStack.getLast().getMatrix();

            int a = (int) (220 * alphaVal);

            int segments = 10;
            for (int i = 0; i < segments; i++) {
                double angle1 = (2 * Math.PI * i) / segments;
                float px1 = (float) (Math.cos(angle1) * size);
                float py1 = (float) (Math.sin(angle1) * size);

                double angle2 = (2 * Math.PI * (i + 1)) / segments;
                float px2 = (float) (Math.cos(angle2) * size);
                float py2 = (float) (Math.sin(angle2) * size);

                BUILDER.pos(matrix, 0, 0, 0).color(230, 25, 25, a).endVertex();
                BUILDER.pos(matrix, px1, py1, 0).color(180, 15, 15, 0).endVertex();
                BUILDER.pos(matrix, px2, py2, 0).color(180, 15, 15, 0).endVertex();
            }

            matrixStack.pop();
        }
    }

    private class CrushingWall {
        double x, y, z;
        int rotationAngle;
        float height;
        long spawnTime;
        static final long DURATION = 3000L;
        float width;

        public CrushingWall(double x, double y, double z, int angle, float height) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.rotationAngle = angle;
            this.height = height;
            this.spawnTime = System.currentTimeMillis();
            this.width = 2.0f;
        }

        public void update() {}

        public boolean shouldRemove() {
            return System.currentTimeMillis() - spawnTime > DURATION;
        }

        public void render(MatrixStack matrixStack) {
            long elapsed = System.currentTimeMillis() - spawnTime;
            float progress = elapsed / (float) DURATION;

            float fallProgress = Math.max(0, progress - 0.2f) / 0.5f;
            float crushProgress = Math.max(0, progress - 0.7f) / 0.3f;

            float alphaVal = 1.0f - crushProgress;

            Vector3d camera = mc.getRenderManager().info.getProjectedView();
            double renderX = x - camera.x;
            double renderY = y - camera.y;
            double renderZ = z - camera.z;

            matrixStack.push();
            RenderSystem.enableBlend();
            RenderSystem.disableTexture();
            RenderSystem.disableCull();
            RenderSystem.depthMask(false);
            RenderSystem.shadeModel(7425);
            RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, 0, 1);

            matrixStack.translate(renderX, renderY, renderZ);
            matrixStack.rotate(Vector3f.YP.rotationDegrees(rotationAngle));

            float fallDistance = Math.min(1.0f, fallProgress) * (height + 1.5f);
            matrixStack.translate(0, -fallDistance, 0);

            float crushScale = 1.0f - crushProgress * 0.9f;
            matrixStack.scale(1.0f, crushScale, 1.0f);

            Matrix4f matrix = matrixStack.getLast().getMatrix();

            int wallAlpha = (int) (alphaVal * 200);

            BUILDER.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            BUILDER.pos(matrix, -width / 2, 0, -0.1f).color(80, 20, 20, wallAlpha).endVertex();
            BUILDER.pos(matrix, width / 2, 0, -0.1f).color(80, 20, 20, wallAlpha).endVertex();
            BUILDER.pos(matrix, width / 2, height, -0.1f).color(100, 30, 30, wallAlpha).endVertex();
            BUILDER.pos(matrix, -width / 2, height, -0.1f).color(100, 30, 30, wallAlpha).endVertex();
            TESSELLATOR.draw();

            BUILDER.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            BUILDER.pos(matrix, -width / 2, 0, 0.1f).color(60, 15, 15, wallAlpha).endVertex();
            BUILDER.pos(matrix, width / 2, 0, 0.1f).color(60, 15, 15, wallAlpha).endVertex();
            BUILDER.pos(matrix, width / 2, height, 0.1f).color(80, 20, 20, wallAlpha).endVertex();
            BUILDER.pos(matrix, -width / 2, height, 0.1f).color(80, 20, 20, wallAlpha).endVertex();
            TESSELLATOR.draw();

            RenderSystem.shadeModel(7424);
            RenderSystem.enableTexture();
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            matrixStack.pop();
        }
    }

    @Subscribe
    private void onDisplay(EventDisplay e) {
        if (this.alpha.finished(Direction.BACKWARDS)) return;
        if (e.getType() != EventDisplay.Type.HIGH) return;

        if (mode.is("Дефолт")) {
            renderDiamond(e);
        }
    }

    @Override
    public boolean onDisable() {
        prevTarget = null;
        target = null;
        currentTarget = null;
        cubeParticles.clear();
        demonHands.clear();
        soulParticles.clear();
        hellPortals.clear();
        demonChains.clear();
        bloodParticles.clear();
        crushingWalls.clear();
        fallingCoffins.clear();
        cleanupIceGlowShader();
        lastDeathTarget = null;
        vortexRotation = 0f;
        return false;
    }

    private class DemonHand {
        double x, y, z;
        double targetX, targetY, targetZ;
        float angle;
        long spawnTime;
        int delay;
        float maxHeight;
        static final long RISE_DURATION = 500L;
        static final long GRAB_DURATION = 600L;
        static final long PULL_DURATION = 800L;

        public DemonHand(double x, double y, double z, double targetX, double targetY, double targetZ, float angle, int delay) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.targetX = targetX;
            this.targetY = targetY;
            this.targetZ = targetZ;
            this.angle = angle;
            this.delay = delay;
            this.spawnTime = System.currentTimeMillis();
            this.maxHeight = 1.8f + ThreadLocalRandom.current().nextFloat() * 0.4f;
        }

        public boolean shouldRemove() {
            return System.currentTimeMillis() - spawnTime - delay > RISE_DURATION + GRAB_DURATION + PULL_DURATION;
        }

        public void render(MatrixStack matrixStack) {
            long elapsed = System.currentTimeMillis() - spawnTime - delay;
            if (elapsed < 0) return;

            float riseProgress = Math.min(1.0f, elapsed / (float) RISE_DURATION);
            float grabProgress = Math.max(0, Math.min(1.0f, (elapsed - RISE_DURATION) / (float) GRAB_DURATION));
            float pullProgress = Math.max(0, Math.min(1.0f, (elapsed - RISE_DURATION - GRAB_DURATION) / (float) PULL_DURATION));

            float alphaVal = 1.0f - pullProgress;
            float height = maxHeight * easeOutBack(riseProgress) * (1.0f - pullProgress * 0.8f);

            Vector3d camera = mc.getRenderManager().info.getProjectedView();
            double renderX = x - camera.x;
            double renderY = y - camera.y;
            double renderZ = z - camera.z;

            matrixStack.push();
            RenderSystem.enableBlend();
            RenderSystem.disableTexture();
            RenderSystem.disableCull();
            RenderSystem.depthMask(false);
            RenderSystem.shadeModel(7425);
            RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, 0, 1);

            matrixStack.translate(renderX, renderY, renderZ);
            matrixStack.rotate(Vector3f.YP.rotationDegrees(angle));

            Matrix4f matrix = matrixStack.getLast().getMatrix();

            int baseAlpha = (int) (200 * alphaVal);
            int clawAlpha = (int) (255 * alphaVal);

            float armWidth = 0.1f;
            float waveTime = elapsed / 150.0f;

            BUILDER.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
            int segments = 16;
            for (int i = 0; i <= segments; i++) {
                float t = i / (float) segments;
                float segmentHeight = height * t;
                float wave = (float) Math.sin(t * Math.PI * 3 + waveTime) * 0.04f * (1 - t);
                float currentWidth = armWidth * (0.6f + t * 0.6f);
                int segmentAlpha = (int) (baseAlpha * (0.7f + t * 0.3f));

                BUILDER.pos(matrix, -currentWidth + wave, segmentHeight, 0).color(100, 20, 20, segmentAlpha).endVertex();
                BUILDER.pos(matrix, currentWidth + wave, segmentHeight, 0).color(60, 10, 10, segmentAlpha).endVertex();
            }
            TESSELLATOR.draw();

            BUILDER.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
            for (int i = 0; i <= segments; i++) {
                float t = i / (float) segments;
                float segmentHeight = height * t;
                float wave = (float) Math.sin(t * Math.PI * 3 + waveTime + 1) * 0.04f * (1 - t);
                float currentWidth = armWidth * (0.6f + t * 0.6f);
                int segmentAlpha = (int) (baseAlpha * (0.7f + t * 0.3f));

                BUILDER.pos(matrix, 0, segmentHeight, -currentWidth + wave).color(80, 15, 15, segmentAlpha).endVertex();
                BUILDER.pos(matrix, 0, segmentHeight, currentWidth + wave).color(50, 8, 8, segmentAlpha).endVertex();
            }
            TESSELLATOR.draw();

            matrixStack.translate(0, height, 0);

            float grabAngle = grabProgress * 50;
            float clawLength = 0.35f;
            float fingerSpacing = 25;

            for (int finger = 0; finger < 5; finger++) {
                float fingerAngle = -50 + finger * fingerSpacing;
                float bendAngle = 15 + grabAngle * (0.8f + finger * 0.05f);
                float fingerLength = clawLength * (finger == 2 ? 1.1f : (finger == 0 || finger == 4 ? 0.8f : 1.0f));

                matrixStack.push();
                matrixStack.rotate(Vector3f.ZP.rotationDegrees(fingerAngle));
                matrixStack.rotate(Vector3f.XP.rotationDegrees(-bendAngle));

                Matrix4f fingerMatrix = matrixStack.getLast().getMatrix();

                BUILDER.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
                int fingerSegments = 8;
                for (int i = 0; i <= fingerSegments; i++) {
                    float ft = i / (float) fingerSegments;
                    float fy = ft * fingerLength;
                    float fw = 0.025f * (1 - ft * 0.6f);
                    int fAlpha = (int) (clawAlpha * (1 - ft * 0.3f));

                    BUILDER.pos(fingerMatrix, -fw, fy, 0).color(80, 20, 20, fAlpha).endVertex();
                    BUILDER.pos(fingerMatrix, fw, fy, 0).color(50, 10, 10, fAlpha).endVertex();
                }
                TESSELLATOR.draw();

                matrixStack.translate(0, fingerLength, 0);
                matrixStack.rotate(Vector3f.XP.rotationDegrees(-40 - grabAngle * 0.6f));
                fingerMatrix = matrixStack.getLast().getMatrix();

                float tipLength = fingerLength * 0.5f;
                BUILDER.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
                BUILDER.pos(fingerMatrix, -0.018f, 0, 0).color(60, 15, 15, clawAlpha).endVertex();
                BUILDER.pos(fingerMatrix, 0.018f, 0, 0).color(60, 15, 15, clawAlpha).endVertex();
                BUILDER.pos(fingerMatrix, 0, tipLength, 0).color(180, 50, 50, clawAlpha).endVertex();
                TESSELLATOR.draw();

                matrixStack.pop();
            }

            RenderSystem.shadeModel(7424);
            RenderSystem.enableTexture();
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            matrixStack.pop();
        }

        private float easeOutBack(float t) {
            float c1 = 1.70158f;
            float c3 = c1 + 1;
            return 1 + c3 * (float) Math.pow(t - 1, 3) + c1 * (float) Math.pow(t - 1, 2);
        }
    }

    private class DemonChain {
        double centerX, centerY, centerZ;
        float angle;
        float radius;
        int delay;
        long spawnTime;
        static final long DURATION = 1800L;

        public DemonChain(double x, double y, double z, float angle, float radius, int delay) {
            this.centerX = x;
            this.centerY = y;
            this.centerZ = z;
            this.angle = angle;
            this.radius = radius;
            this.delay = delay;
            this.spawnTime = System.currentTimeMillis();
        }

        public void update() {}

        public boolean shouldRemove() {
            return System.currentTimeMillis() - spawnTime - delay > DURATION;
        }

        public void render(MatrixStack matrixStack) {
            long elapsed = System.currentTimeMillis() - spawnTime - delay;
            if (elapsed < 0) return;

            float progress = elapsed / (float) DURATION;
            float alphaVal = progress < 0.7f ? 1.0f : 1.0f - (progress - 0.7f) / 0.3f;

            float pullProgress = Math.min(1.0f, progress * 1.5f);
            float currentRadius = radius * (1.0f - pullProgress * 0.8f);
            float chainHeight = 1.5f * (1.0f - pullProgress * 0.7f);

            double startX = centerX + Math.cos(angle) * currentRadius;
            double startZ = centerZ + Math.sin(angle) * currentRadius;
            double startY = centerY - 0.5f;

            double endX = centerX + Math.cos(angle) * currentRadius * 0.2f;
            double endZ = centerZ + Math.sin(angle) * currentRadius * 0.2f;
            double endY = centerY + chainHeight;

            Vector3d camera = mc.getRenderManager().info.getProjectedView();

            matrixStack.push();
            RenderSystem.enableBlend();
            RenderSystem.disableTexture();
            RenderSystem.disableCull();
            RenderSystem.depthMask(false);
            RenderSystem.shadeModel(7425);
            RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, 0, 1);

            Matrix4f matrix = matrixStack.getLast().getMatrix();

            int chainAlpha = (int) (220 * alphaVal);
            int segments = 12;
            float time = elapsed / 200.0f;

            for (int i = 0; i < segments; i++) {
                float t1 = i / (float) segments;
                float t2 = (i + 1) / (float) segments;

                float wave1 = (float) Math.sin(t1 * Math.PI * 4 + time) * 0.05f;
                float wave2 = (float) Math.sin(t2 * Math.PI * 4 + time) * 0.05f;

                double x1 = startX + (endX - startX) * t1 + wave1 - camera.x;
                double y1 = startY + (endY - startY) * t1 - camera.y;
                double z1 = startZ + (endZ - startZ) * t1 - camera.z;

                double x2 = startX + (endX - startX) * t2 + wave2 - camera.x;
                double y2 = startY + (endY - startY) * t2 - camera.y;
                double z2 = startZ + (endZ - startZ) * t2 - camera.z;

                float width = 0.04f * (1.0f - t1 * 0.3f);

                BUILDER.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
                BUILDER.pos(matrix, (float) x1 - width, (float) y1, (float) z1).color(120, 30, 30, chainAlpha).endVertex();
                BUILDER.pos(matrix, (float) x1 + width, (float) y1, (float) z1).color(80, 20, 20, chainAlpha).endVertex();
                BUILDER.pos(matrix, (float) x2 + width, (float) y2, (float) z2).color(80, 20, 20, chainAlpha).endVertex();
                BUILDER.pos(matrix, (float) x2 - width, (float) y2, (float) z2).color(120, 30, 30, chainAlpha).endVertex();
                TESSELLATOR.draw();

                if (i % 2 == 0) {
                    float linkSize = 0.06f;
                    BUILDER.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
                    BUILDER.pos(matrix, (float) x1, (float) y1, (float) z1).color(150, 40, 40, chainAlpha).endVertex();
                    for (int j = 0; j <= 8; j++) {
                        double linkAngle = j * Math.PI * 2 / 8;
                        float lx = (float) (x1 + Math.cos(linkAngle) * linkSize);
                        float ly = (float) (y1 + Math.sin(linkAngle) * linkSize * 0.5f);
                        BUILDER.pos(matrix, lx, ly, (float) z1).color(100, 25, 25, chainAlpha / 2).endVertex();
                    }
                    TESSELLATOR.draw();
                }
            }

            RenderSystem.shadeModel(7424);
            RenderSystem.enableTexture();
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            matrixStack.pop();
        }
    }

    private class SoulParticle {
        double x, y, z;
        double startX, startY, startZ;
        double targetX, targetY, targetZ;
        long spawnTime;
        float rotationSpeed;
        float orbitRadius;
        int color;
        static final long DURATION = 1800L;

        public SoulParticle(double x, double y, double z, double targetX, double targetY, double targetZ) {
            this.startX = x;
            this.startY = y;
            this.startZ = z;
            this.x = x;
            this.y = y;
            this.z = z;
            this.targetX = targetX;
            this.targetY = targetY - 2;
            this.targetZ = targetZ;
            this.spawnTime = System.currentTimeMillis();
            this.rotationSpeed = 2 + ThreadLocalRandom.current().nextFloat() * 3;
            this.orbitRadius = 0.3f + ThreadLocalRandom.current().nextFloat() * 0.4f;

            int colorType = ThreadLocalRandom.current().nextInt(3);
            if (colorType == 0) {
                this.color = ColorUtils.rgb(200, 80, 80);
            } else if (colorType == 1) {
                this.color = ColorUtils.rgb(150, 50, 50);
            } else {
                this.color = ColorUtils.rgb(180, 60, 100);
            }
        }

        public void update() {
            float progress = (System.currentTimeMillis() - spawnTime) / (float) DURATION;
            progress = Math.min(1.0f, progress);

            float spiralProgress = easeInQuad(progress);

            double baseX = startX + (targetX - startX) * spiralProgress;
            double baseY = startY + (targetY - startY) * spiralProgress;
            double baseZ = startZ + (targetZ - startZ) * spiralProgress;

            float angleVal = (System.currentTimeMillis() - spawnTime) / 1000.0f * rotationSpeed * (float) Math.PI * 2;
            float currentRadius = orbitRadius * (1.0f - spiralProgress);

            x = baseX + Math.cos(angleVal) * currentRadius;
            y = baseY;
            z = baseZ + Math.sin(angleVal) * currentRadius;
        }

        private float easeInQuad(float t) {
            return t * t;
        }

        public boolean shouldRemove() {
            return System.currentTimeMillis() - spawnTime > DURATION;
        }

        public void renderBatched(MatrixStack matrixStack, float yaw, float pitch) {
            float progress = (System.currentTimeMillis() - spawnTime) / (float) DURATION;
            float alphaVal = 1.0f;

            if (progress > 0.7f) {
                alphaVal = (1.0f - progress) / 0.3f;
            }

            Vector3d camera = mc.getRenderManager().info.getProjectedView();
            double renderX = x - camera.x;
            double renderY = y - camera.y;
            double renderZ = z - camera.z;

            matrixStack.push();

            matrixStack.translate(renderX, renderY, renderZ);
            matrixStack.rotate(Vector3f.YP.rotationDegrees(-yaw));
            matrixStack.rotate(Vector3f.XP.rotationDegrees(pitch));

            Matrix4f matrix = matrixStack.getLast().getMatrix();

            int r = ColorUtils.getRed(color);
            int g = ColorUtils.getGreen(color);
            int b = ColorUtils.getBlue(color);
            int a = (int) (200 * alphaVal);

            float size = 0.12f * (1.0f - progress * 0.4f);

            int segments = 12;
            for (int i = 0; i < segments; i++) {
                double angle1 = (2 * Math.PI * i) / segments;
                float px1 = (float) (Math.cos(angle1) * size);
                float py1 = (float) (Math.sin(angle1) * size);

                double angle2 = (2 * Math.PI * (i + 1)) / segments;
                float px2 = (float) (Math.cos(angle2) * size);
                float py2 = (float) (Math.sin(angle2) * size);

                BUILDER.pos(matrix, 0, 0, 0).color(255, 200, 200, a).endVertex();
                BUILDER.pos(matrix, px1, py1, 0).color(r, g, b, 0).endVertex();
                BUILDER.pos(matrix, px2, py2, 0).color(r, g, b, 0).endVertex();
            }

            float tailLength = 0.5f;
            float time = (System.currentTimeMillis() - spawnTime) / 150.0f;

            int tailSegments = 10;
            for (int i = 0; i < tailSegments; i++) {
                float t1 = i / (float) tailSegments;
                float t2 = (i + 1) / (float) tailSegments;

                float tailY1 = -t1 * tailLength;
                float tailY2 = -t2 * tailLength;

                float tailWidth1 = size * (1.0f - t1 * 0.9f);
                float tailWidth2 = size * (1.0f - t2 * 0.9f);

                float wave1 = (float) Math.sin(time + t1 * 5) * 0.02f;
                float wave2 = (float) Math.sin(time + t2 * 5) * 0.02f;

                int tailAlpha1 = (int) (a * (1.0f - t1 * 0.8f));
                int tailAlpha2 = (int) (a * (1.0f - t2 * 0.8f));

                BUILDER.pos(matrix, -tailWidth1 + wave1, tailY1, 0).color(r, g, b, tailAlpha1).endVertex();
                BUILDER.pos(matrix, tailWidth1 + wave1, tailY1, 0).color(r, g, b, tailAlpha1).endVertex();
                BUILDER.pos(matrix, -tailWidth2 + wave2, tailY2, 0).color(r, g, b, tailAlpha2).endVertex();

                BUILDER.pos(matrix, tailWidth1 + wave1, tailY1, 0).color(r, g, b, tailAlpha1).endVertex();
                BUILDER.pos(matrix, tailWidth2 + wave2, tailY2, 0).color(r, g, b, tailAlpha2).endVertex();
                BUILDER.pos(matrix, -tailWidth2 + wave2, tailY2, 0).color(r, g, b, tailAlpha2).endVertex();
            }

            matrixStack.pop();
        }
    }

    private class HellPortal {
        double x, y, z;
        long spawnTime;
        float radius;
        static final long DURATION = 2200L;

        public HellPortal(double x, double y, double z, float radius) {
            this.x = x;
            this.y = y + 0.02;
            this.z = z;
            this.radius = radius;
            this.spawnTime = System.currentTimeMillis();
        }

        public boolean shouldRemove() {
            return System.currentTimeMillis() - spawnTime > DURATION;
        }

        public void render(MatrixStack matrixStack) {
            long elapsed = System.currentTimeMillis() - spawnTime;
            float progress = elapsed / (float) DURATION;

            float openProgress = Math.min(1.0f, progress * 2.5f);
            float closeProgress = Math.max(0, (progress - 0.5f) / 0.5f);

            float alphaVal = openProgress * (1.0f - closeProgress);
            float currentRadius = radius * easeOutBack(openProgress) * (1.0f - closeProgress * 0.6f);

            Vector3d camera = mc.getRenderManager().info.getProjectedView();
            double renderX = x - camera.x;
            double renderY = y - camera.y;
            double renderZ = z - camera.z;

            matrixStack.push();
            RenderSystem.enableBlend();
            RenderSystem.disableTexture();
            RenderSystem.disableCull();
            RenderSystem.depthMask(false);
            RenderSystem.shadeModel(7425);
            RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, 0, 1);

            matrixStack.translate(renderX, renderY, renderZ);

            Matrix4f matrix = matrixStack.getLast().getMatrix();

            int segments = 64;
            float rotation = elapsed / 40.0f;

            int centerA = (int) (220 * alphaVal);
            BUILDER.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
            BUILDER.pos(matrix, 0, 0, 0).color(0, 0, 0, centerA).endVertex();
            for (int i = 0; i <= segments; i++) {
                double angle = (2 * Math.PI * i) / segments + rotation * 0.01;
                float px = (float) (Math.cos(angle) * currentRadius * 0.4f);
                float pz = (float) (Math.sin(angle) * currentRadius * 0.4f);
                BUILDER.pos(matrix, px, 0, pz).color(80, 0, 0, (int) (120 * alphaVal)).endVertex();
            }
            TESSELLATOR.draw();

            for (int ring = 0; ring < 4; ring++) {
                float ringRadius = currentRadius * (0.5f + ring * 0.15f);
                float ringAlpha = alphaVal * (1.0f - ring * 0.15f);
                float ringRotation = rotation * (1.2f + ring * 0.3f) * (ring % 2 == 0 ? 1 : -1);

                BUILDER.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
                for (int i = 0; i <= segments; i++) {
                    double angle = (2 * Math.PI * i) / segments + ringRotation * 0.02;
                    float innerR = ringRadius - 0.04f;
                    float outerR = ringRadius;

                    float xInner = (float) (Math.cos(angle) * innerR);
                    float zInner = (float) (Math.sin(angle) * innerR);
                    float xOuter = (float) (Math.cos(angle) * outerR);
                    float zOuter = (float) (Math.sin(angle) * outerR);

                    int r = 140 + ring * 25;
                    int innerA = (int) (180 * ringAlpha);
                    int outerA = (int) (60 * ringAlpha);

                    BUILDER.pos(matrix, xInner, 0, zInner).color(r, 0, 0, innerA).endVertex();
                    BUILDER.pos(matrix, xOuter, 0, zOuter).color(r - 50, 0, 0, outerA).endVertex();
                }
                TESSELLATOR.draw();
            }

            RenderSystem.shadeModel(7424);
            RenderSystem.enableTexture();
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            matrixStack.pop();
        }

        private float easeOutBack(float t) {
            float c1 = 1.70158f;
            float c3 = c1 + 1;
            return 1 + c3 * (float) Math.pow(t - 1, 3) + c1 * (float) Math.pow(t - 1, 2);
        }
    }

    private void renderDevilCircle(MatrixStack matrixStack, WorldEvent e) {
        if (prevTarget == null) return;

        float alphaValue = (float) (devilCircleAnim.getOutput() / 255.0);

        if (alphaValue < 0.01f) return;

        float partialTicks = e.getPartialTicks();
        double entityX = prevTarget.lastTickPosX + (prevTarget.getPosX() - prevTarget.lastTickPosX) * partialTicks;
        double entityY = prevTarget.lastTickPosY + (prevTarget.getPosY() - prevTarget.lastTickPosY) * partialTicks;
        double entityZ = prevTarget.lastTickPosZ + (prevTarget.getPosZ() - prevTarget.lastTickPosZ) * partialTicks;

        Vector3d cameraPos = mc.getRenderManager().info.getProjectedView();
        double renderX = entityX - cameraPos.getX();
        double renderY = entityY - cameraPos.getY() + 0.02;
        double renderZ = entityZ - cameraPos.getZ();

        float radius = devilCircleRadius.get().floatValue();
        long now = System.currentTimeMillis();
        float rotation = (now % 36000) / 100.0f;

        int baseColor = ColorUtils.rgb(180, 0, 0);

        if (prevTarget.hurtTime > 0) {
            float hurtProgress = prevTarget.hurtTime / 10f;
            baseColor = ColorUtils.interpolateColor(baseColor, ColorUtils.rgb(255, 50, 50), hurtProgress);
        }

        matrixStack.push();
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.shadeModel(7425);
        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, 0, 1);

        matrixStack.translate(renderX, renderY, renderZ);

        Matrix4f matrix = matrixStack.getLast().getMatrix();

        drawPentagram(matrix, radius * 0.6f, rotation, baseColor, alphaValue);
        drawOuterCircle(matrix, radius, baseColor, alphaValue);
        drawInnerCircle(matrix, radius * 0.75f, ColorUtils.rgb(120, 0, 0), alphaValue * 0.7f);
        drawRunicSymbols(matrix, radius * 0.88f, rotation * 0.5f, baseColor, alphaValue);

        RenderSystem.shadeModel(7424);
        RenderSystem.enableTexture();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        matrixStack.pop();

        renderSwords(matrixStack, renderX, renderY, renderZ, radius, rotation, baseColor, alphaValue, 1.0f, now);
    }

    private void drawPentagram(Matrix4f matrix, float radius, float rotation, int color, float alpha) {
        int r = ColorUtils.getRed(color);
        int g = ColorUtils.getGreen(color);
        int b = ColorUtils.getBlue(color);
        int a = (int) (alpha * 200);

        for (int i = 0; i < 5; i++) {
            float angle1 = (float) Math.toRadians(rotation + i * 72 - 90);
            float angle2 = (float) Math.toRadians(rotation + ((i + 2) % 5) * 72 - 90);

            float x1 = (float) Math.cos(angle1) * radius;
            float z1 = (float) Math.sin(angle1) * radius;
            float x2 = (float) Math.cos(angle2) * radius;
            float z2 = (float) Math.sin(angle2) * radius;

            float dx = x2 - x1;
            float dz = z2 - z1;
            float length = (float) Math.sqrt(dx * dx + dz * dz);
            float nx = -dz / length * 0.03f;
            float nz = dx / length * 0.03f;

            BUILDER.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            BUILDER.pos(matrix, x1 - nx, 0, z1 - nz).color(r, g, b, a).endVertex();
            BUILDER.pos(matrix, x1 + nx, 0, z1 + nz).color(r, g, b, a).endVertex();
            BUILDER.pos(matrix, x2 + nx, 0, z2 + nz).color(r, g, b, a).endVertex();
            BUILDER.pos(matrix, x2 - nx, 0, z2 - nz).color(r, g, b, a).endVertex();
            TESSELLATOR.draw();
        }

        int glowA = (int) (alpha * 50);
        BUILDER.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        BUILDER.pos(matrix, 0, 0, 0).color(r, g, b, glowA).endVertex();
        for (int i = 0; i <= 5; i++) {
            float angle = (float) Math.toRadians(rotation + i * 72 - 90);
            float x = (float) Math.cos(angle) * radius;
            float z = (float) Math.sin(angle) * radius;
            BUILDER.pos(matrix, x, 0, z).color(r, g, b, 0).endVertex();
        }
        TESSELLATOR.draw();
    }

    private void drawOuterCircle(Matrix4f matrix, float radius, int color, float alpha) {
        int r = ColorUtils.getRed(color);
        int g = ColorUtils.getGreen(color);
        int b = ColorUtils.getBlue(color);
        int a = (int) (alpha * 220);

        int segments = 64;
        float lineWidth = 0.04f;

        BUILDER.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            double angle = (2 * Math.PI * i) / segments;
            float xInner = (float) (Math.cos(angle) * (radius - lineWidth));
            float zInner = (float) (Math.sin(angle) * (radius - lineWidth));
            float xOuter = (float) (Math.cos(angle) * radius);
            float zOuter = (float) (Math.sin(angle) * radius);

            BUILDER.pos(matrix, xInner, 0, zInner).color(r, g, b, a).endVertex();
            BUILDER.pos(matrix, xOuter, 0, zOuter).color(r, g, b, a).endVertex();
        }
        TESSELLATOR.draw();

        float glowRadius = radius + 0.12f;
        int glowA = (int) (alpha * 35);

        BUILDER.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            double angle = (2 * Math.PI * i) / segments;
            float xInner = (float) (Math.cos(angle) * radius);
            float zInner = (float) (Math.sin(angle) * radius);
            float xOuter = (float) (Math.cos(angle) * glowRadius);
            float zOuter = (float) (Math.sin(angle) * glowRadius);

            BUILDER.pos(matrix, xInner, 0, zInner).color(r, g, b, glowA).endVertex();
            BUILDER.pos(matrix, xOuter, 0, zOuter).color(r, g, b, 0).endVertex();
        }
        TESSELLATOR.draw();
    }

    private void drawInnerCircle(Matrix4f matrix, float radius, int color, float alpha) {
        int r = ColorUtils.getRed(color);
        int g = ColorUtils.getGreen(color);
        int b = ColorUtils.getBlue(color);
        int a = (int) (alpha * 180);

        int segments = 48;
        float lineWidth = 0.025f;

        BUILDER.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            double angle = (2 * Math.PI * i) / segments;
            float xInner = (float) (Math.cos(angle) * (radius - lineWidth));
            float zInner = (float) (Math.sin(angle) * (radius - lineWidth));
            float xOuter = (float) (Math.cos(angle) * radius);
            float zOuter = (float) (Math.sin(angle) * radius);

            BUILDER.pos(matrix, xInner, 0, zInner).color(r, g, b, a).endVertex();
            BUILDER.pos(matrix, xOuter, 0, zOuter).color(r, g, b, a).endVertex();
        }
        TESSELLATOR.draw();
    }

    private void drawRunicSymbols(Matrix4f matrix, float radius, float rotation, int color, float alpha) {
        int r = ColorUtils.getRed(color);
        int g = ColorUtils.getGreen(color);
        int b = ColorUtils.getBlue(color);
        int a = (int) (alpha * 150);

        int symbolCount = 12;
        float symbolSize = 0.07f;

        for (int i = 0; i < symbolCount; i++) {
            float angle = (float) Math.toRadians(rotation + i * (360.0f / symbolCount));
            float x = (float) Math.cos(angle) * radius;
            float z = (float) Math.sin(angle) * radius;

            BUILDER.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
            BUILDER.pos(matrix, x, 0, z - symbolSize).color(r, g, b, a).endVertex();
            BUILDER.pos(matrix, x - symbolSize * 0.5f, 0, z + symbolSize * 0.5f).color(r, g, b, a).endVertex();
            BUILDER.pos(matrix, x + symbolSize * 0.5f, 0, z + symbolSize * 0.5f).color(r, g, b, a).endVertex();
            TESSELLATOR.draw();
        }
    }

    private void renderSwords(MatrixStack matrixStack, double renderX, double renderY, double renderZ, float circleRadius, float rotation, int color, float alpha, float scale, long now) {
        int swords = swordCount.get().intValue();
        float size = swordSize.get().floatValue();
        float swordRotation = -rotation * 2;
        float hoverHeight = 0.8f + (float) Math.sin(now / 500.0) * 0.1f;
        int r = ColorUtils.getRed(color);
        int g = ColorUtils.getGreen(color);
        int b = ColorUtils.getBlue(color);
        int a = (int) (alpha * 255);
        int glowA = (int) (alpha * 80);

        for (int i = 0; i < swords; i++) {
            float angle = (float) Math.toRadians(swordRotation + i * (360.0f / swords));
            float swordX = (float) (Math.cos(angle) * circleRadius * 0.9f);
            float swordZ = (float) (Math.sin(angle) * circleRadius * 0.9f);

            matrixStack.push();
            RenderSystem.enableBlend();
            RenderSystem.disableTexture();
            RenderSystem.disableCull();
            RenderSystem.depthMask(false);
            RenderSystem.shadeModel(7425);
            RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, 0, 1);

            matrixStack.translate(renderX + swordX * scale, renderY + hoverHeight, renderZ + swordZ * scale);
            matrixStack.rotate(Vector3f.YP.rotationDegrees((float) Math.toDegrees(-angle) + 90));
            matrixStack.rotate(Vector3f.XP.rotationDegrees(15 + (float) Math.sin(now / 300.0 + i) * 5));
            matrixStack.scale(size * scale, size * scale, size * scale);

            Matrix4f matrix = matrixStack.getLast().getMatrix();

            BUILDER.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
            BUILDER.pos(matrix, 0, 1.2f, 0).color(255, 255, 255, a).endVertex();
            BUILDER.pos(matrix, -0.08f, 0.3f, 0).color(r, g, b, a).endVertex();
            BUILDER.pos(matrix, 0.08f, 0.3f, 0).color(r, g, b, a).endVertex();
            TESSELLATOR.draw();

            BUILDER.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            BUILDER.pos(matrix, -0.08f, 0.3f, 0).color(r, g, b, a).endVertex();
            BUILDER.pos(matrix, 0.08f, 0.3f, 0).color(r, g, b, a).endVertex();
            BUILDER.pos(matrix, 0.05f, -0.1f, 0).color(r / 2, g / 2, b / 2, a).endVertex();
            BUILDER.pos(matrix, -0.05f, -0.1f, 0).color(r / 2, g / 2, b / 2, a).endVertex();
            TESSELLATOR.draw();

            BUILDER.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            BUILDER.pos(matrix, -0.25f, 0.25f, 0).color(r, g, b, a).endVertex();
            BUILDER.pos(matrix, 0.25f, 0.25f, 0).color(r, g, b, a).endVertex();
            BUILDER.pos(matrix, 0.25f, 0.2f, 0).color(r / 2, g / 2, b / 2, a).endVertex();
            BUILDER.pos(matrix, -0.25f, 0.2f, 0).color(r / 2, g / 2, b / 2, a).endVertex();
            TESSELLATOR.draw();

            BUILDER.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            BUILDER.pos(matrix, -0.04f, -0.1f, 0).color(80, 50, 30, a).endVertex();
            BUILDER.pos(matrix, 0.04f, -0.1f, 0).color(80, 50, 30, a).endVertex();
            BUILDER.pos(matrix, 0.04f, -0.35f, 0).color(60, 35, 20, a).endVertex();
            BUILDER.pos(matrix, -0.04f, -0.35f, 0).color(60, 35, 20, a).endVertex();
            TESSELLATOR.draw();

            BUILDER.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
            BUILDER.pos(matrix, 0, 0.7f, 0).color(r, g, b, glowA).endVertex();
            int glowSegments = 16;
            float glowSize = 0.4f;
            for (int j = 0; j <= glowSegments; j++) {
                float glowAngle = (float) (2 * Math.PI * j / glowSegments);
                float gx = (float) Math.cos(glowAngle) * glowSize;
                float gy = (float) Math.sin(glowAngle) * glowSize * 2 + 0.7f;
                BUILDER.pos(matrix, gx, gy, 0).color(r, g, b, 0).endVertex();
            }
            TESSELLATOR.draw();

            RenderSystem.shadeModel(7424);
            RenderSystem.enableTexture();
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            matrixStack.pop();
        }
    }

    private void renderCubes(MatrixStack matrixStack, WorldEvent e) {
        long currentTime = System.currentTimeMillis();
        deltaTime = (currentTime - lastParticleTime) / 1000.0f;

        if (deltaTime > 0.1f) {
            deltaTime = 0.016f;
        }

        lastParticleTime = currentTime;

        if (currentTarget != lastTarget) {
            cubeParticles.clear();
            spawnAccumulator = 0;
            lastTarget = currentTarget;
        }

        Iterator<CubeParticle> iterator = cubeParticles.iterator();
        while (iterator.hasNext()) {
            CubeParticle particle = iterator.next();
            particle.update(deltaTime);
            if (particle.shouldRemove()) {
                iterator.remove();
            }
        }

        boolean show = target != null;
        if (show && currentTarget != null) {
            spawnAccumulator += deltaTime;
            ThreadLocalRandom random = ThreadLocalRandom.current();
            float radius = circleRadius.get().floatValue();

            while (spawnAccumulator >= SPAWN_INTERVAL) {
                spawnAccumulator -= SPAWN_INTERVAL;

                for (int i = 0; i < PARTICLES_PER_SPAWN; i++) {
                    double rand = random.nextDouble(0.0, Math.PI * 2.0);
                    double x = Math.cos(rand) * radius;
                    double y = MathHelper.lerp(random.nextFloat(), 0.2f, currentTarget.getHeight() - 0.8f);
                    double z = Math.sin(rand) * radius;
                    cubeParticles.add(new CubeParticle(currentTarget, x, y, z));
                }
            }
        }

        double alphaOutput = alpha.getOutput();
        if (!cubeParticles.isEmpty() || (currentTarget != null && alphaOutput > 0)) {
            int baseThemeColor = Theme.MainColor(0);

            if (currentTarget != null && currentTarget.hurtTime > 0) {
                float hurtProgress = currentTarget.hurtTime / 10f;
                baseThemeColor = ColorUtils.interpolateColor(baseThemeColor, ColorUtils.rgb(255, 100, 100), hurtProgress);
            }

            if (currentTarget != null && alphaOutput > 0) {
                renderCircle(matrixStack, baseThemeColor);
            }

            float particleSize = cubeSize.get().floatValue();
            for (CubeParticle particle : cubeParticles) {
                particle.render(matrixStack, baseThemeColor, particleSize);
            }
        }
    }

    private void renderCircle(MatrixStack matrixStack, int baseColor) {
        if (currentTarget == null) return;

        float partialTicks = mc.getRenderPartialTicks();
        double entityX = currentTarget.lastTickPosX + (currentTarget.getPosX() - currentTarget.lastTickPosX) * partialTicks;
        double entityY = currentTarget.lastTickPosY + (currentTarget.getPosY() - currentTarget.lastTickPosY) * partialTicks;
        double entityZ = currentTarget.lastTickPosZ + (currentTarget.getPosZ() - currentTarget.lastTickPosZ) * partialTicks;

        Vector3d cameraPos = mc.getRenderManager().info.getProjectedView();
        double renderX = entityX - cameraPos.getX();
        double renderY = entityY - cameraPos.getY() + 0.05;
        double renderZ = entityZ - cameraPos.getZ();

        float radius = circleRadius.get().floatValue();
        int segments = circleSegments.get().intValue();
        float alphaValue = (float) (alpha.getOutput() / 255.0);
        float scale = (float) (scaleAnimation.getOutput() / 255.0);

        matrixStack.push();
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.shadeModel(7425);
        RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, 0, 1);

        matrixStack.translate(renderX, renderY, renderZ);
        matrixStack.scale(scale, 1.0f, scale);

        Matrix4f matrix = matrixStack.getLast().getMatrix();

        int centerColor = ColorUtils.setAlpha(baseColor, (int) (alphaValue * 60));
        int edgeColor = ColorUtils.setAlpha(baseColor, 0);
        int centerR = ColorUtils.getRed(centerColor);
        int centerG = ColorUtils.getGreen(centerColor);
        int centerB = ColorUtils.getBlue(centerColor);
        int centerA = ColorUtils.getAlpha(centerColor);
        int edgeR = ColorUtils.getRed(edgeColor);
        int edgeG = ColorUtils.getGreen(edgeColor);
        int edgeB = ColorUtils.getBlue(edgeColor);
        int edgeA = ColorUtils.getAlpha(edgeColor);

        BUILDER.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
        BUILDER.pos(matrix, 0, 0, 0)
                .color(centerR, centerG, centerB, centerA)
                .endVertex();

        for (int i = 0; i <= segments; i++) {
            double angle = (2 * Math.PI * i) / segments;
            float x = (float) (Math.cos(angle) * radius);
            float z = (float) (Math.sin(angle) * radius);
            BUILDER.pos(matrix, x, 0, z)
                    .color(edgeR, edgeG, edgeB, edgeA)
                    .endVertex();
        }
        TESSELLATOR.draw();

        int ringColor = ColorUtils.setAlpha(baseColor, (int) (alphaValue * 180));
        int innerRingColor = ColorUtils.setAlpha(baseColor, (int) (alphaValue * 100));
        int ringR = ColorUtils.getRed(ringColor);
        int ringG = ColorUtils.getGreen(ringColor);
        int ringB = ColorUtils.getBlue(ringColor);
        int ringA = ColorUtils.getAlpha(ringColor);
        int innerRingR = ColorUtils.getRed(innerRingColor);
        int innerRingG = ColorUtils.getGreen(innerRingColor);
        int innerRingB = ColorUtils.getBlue(innerRingColor);
        int innerRingA = ColorUtils.getAlpha(innerRingColor);
        float ringWidth = 0.08f;

        BUILDER.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            double angle = (2 * Math.PI * i) / segments;
            float xInner = (float) (Math.cos(angle) * (radius - ringWidth));
            float zInner = (float) (Math.sin(angle) * (radius - ringWidth));
            float xOuter = (float) (Math.cos(angle) * radius);
            float zOuter = (float) (Math.sin(angle) * radius);

            BUILDER.pos(matrix, xInner, 0, zInner)
                    .color(innerRingR, innerRingG, innerRingB, innerRingA)
                    .endVertex();
            BUILDER.pos(matrix, xOuter, 0, zOuter)
                    .color(ringR, ringG, ringB, ringA)
                    .endVertex();
        }
        TESSELLATOR.draw();

        float glowRadius = radius + 0.15f;
        int glowColor = ColorUtils.setAlpha(baseColor, (int) (alphaValue * 40));
        int glowEdge = ColorUtils.setAlpha(baseColor, 0);
        int glowR = ColorUtils.getRed(glowColor);
        int glowG = ColorUtils.getGreen(glowColor);
        int glowB = ColorUtils.getBlue(glowColor);
        int glowA = ColorUtils.getAlpha(glowColor);
        int glowEdgeR = ColorUtils.getRed(glowEdge);
        int glowEdgeG = ColorUtils.getGreen(glowEdge);
        int glowEdgeB = ColorUtils.getBlue(glowEdge);
        int glowEdgeA = ColorUtils.getAlpha(glowEdge);

        BUILDER.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i <= segments; i++) {
            double angle = (2 * Math.PI * i) / segments;
            float xInner = (float) (Math.cos(angle) * radius);
            float zInner = (float) (Math.sin(angle) * radius);
            float xOuter = (float) (Math.cos(angle) * glowRadius);
            float zOuter = (float) (Math.sin(angle) * glowRadius);

            BUILDER.pos(matrix, xInner, 0, zInner)
                    .color(glowR, glowG, glowB, glowA)
                    .endVertex();
            BUILDER.pos(matrix, xOuter, 0, zOuter)
                    .color(glowEdgeR, glowEdgeG, glowEdgeB, glowEdgeA)
                    .endVertex();
        }
        TESSELLATOR.draw();

        RenderSystem.shadeModel(7424);
        RenderSystem.enableTexture();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
        matrixStack.pop();
    }

    private class CubeParticle {
        double x, y, z;
        double posX, posY, posZ;
        double motionX, motionY, motionZ;
        long time;
        LivingEntity entity;
        float animationProgress = 1f;
        boolean fadingOut = false;
        boolean initialized = false;

        public CubeParticle(LivingEntity entity, double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.entity = entity;
            this.time = System.currentTimeMillis();
            this.animationProgress = 1f;
            this.initialized = false;
        }

        public long getTime() {
            return time;
        }

        public boolean shouldRemove() {
            return fadingOut && animationProgress <= 0;
        }

        public void update(float deltaTime) {
            boolean alive = (System.currentTimeMillis() - this.getTime()) <= PARTICLE_LIFE_TIME - 200L;
            fadingOut = !alive;

            if (!fadingOut) {
                animationProgress = Math.min(1f, animationProgress + deltaTime * 4f);
            } else {
                animationProgress = Math.max(0f, animationProgress - deltaTime * 4f);
            }

            float randomY = MathHelper.lerp(ThreadLocalRandom.current().nextFloat(), 0.01f, 0.04f);
            this.y += randomY * (deltaTime * 60);

            if (entity != null) {
                float partialTicks = mc.getRenderPartialTicks();
                double interpX = entity.lastTickPosX + (entity.getPosX() - entity.lastTickPosX) * partialTicks;
                double interpY = entity.lastTickPosY + (entity.getPosY() - entity.lastTickPosY) * partialTicks;
                double interpZ = entity.lastTickPosZ + (entity.getPosZ() - entity.lastTickPosZ) * partialTicks;

                this.motionX = x + interpX;
                this.motionY = y + interpY;
                this.motionZ = z + interpZ;

                if (!initialized) {
                    Vector3d cameraPos = mc.getRenderManager().info.getProjectedView();
                    double camX = cameraPos.getX();
                    double camY = cameraPos.getY();
                    double camZ = cameraPos.getZ();

                    posX = motionX - camX;
                    posY = motionY - camY;
                    posZ = motionZ - camZ;
                    initialized = true;
                }
            }
        }

        public void render(MatrixStack matrixStack, int colors, float scale) {
            if (entity == null || !initialized) return;

            long now = System.currentTimeMillis();
            double rotation = (now - this.getTime()) / 10.0;

            Vector3d cameraPos = mc.getRenderManager().info.getProjectedView();
            double camX = cameraPos.getX();
            double camY = cameraPos.getY();
            double camZ = cameraPos.getZ();

            double targetX = this.motionX - camX;
            double targetY = this.motionY - camY;
            double targetZ = this.motionZ - camZ;

            posX = MathHelper.lerp(0.2f, (float) posX, (float) targetX);
            posY = MathHelper.lerp(0.2f, (float) posY, (float) targetY);
            posZ = MathHelper.lerp(0.2f, (float) posZ, (float) targetZ);

            float yaw = mc.getRenderManager().info.getYaw();
            float pitch = mc.getRenderManager().info.getPitch();

            int color = ColorUtils.setAlpha(colors, (int) (animationProgress * 255));
            int colorGlow = ColorUtils.setAlpha(colors, (int) (animationProgress * 80));

            matrixStack.push();
            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();
            RenderSystem.disableCull();
            RenderSystem.depthMask(false);
            RenderSystem.disableTexture();
            RenderSystem.enableAlphaTest();
            RenderSystem.shadeModel(7425);
            RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, 0, 1);
            RenderSystem.disableLighting();
            RenderSystem.lineWidth(1);
            RenderSystem.alphaFunc(GL11.GL_GREATER, 0.003921569F);

            matrixStack.push();
            matrixStack.translate(posX, posY, posZ);
            matrixStack.rotate(Vector3f.XP.rotationDegrees((float) rotation));
            matrixStack.rotate(Vector3f.YP.rotationDegrees((float) rotation));
            matrixStack.rotate(Vector3f.ZP.rotationDegrees((float) rotation));
            matrixStack.scale(scale, scale, scale);

            int colorOut = ColorUtils.setAlpha(color, (int) (ColorUtils.getAlpha(color) / 4.0F));
            int colorFill = ColorUtils.setAlpha(color, (int) (ColorUtils.getAlpha(color) / 12.0F));

            drawAxisBox(matrixStack, CUBE_PARTICLE_BOX, colorOut, colorFill);
            matrixStack.pop();

            RenderSystem.alphaFunc(GL11.GL_GREATER, 0.1F);
            RenderSystem.lineWidth(1.0F);
            RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 0, 1);
            RenderSystem.shadeModel(7424);
            RenderSystem.enableTexture();
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();

            matrixStack.push();
            RenderSystem.depthMask(false);
            matrixStack.translate(posX, posY, posZ);
            matrixStack.rotate(Vector3f.YP.rotationDegrees(-yaw));
            matrixStack.rotate(Vector3f.XP.rotationDegrees(pitch));
            matrixStack.scale(-scale, -scale, scale);

            mc.getTextureManager().bindTexture(PARTICLE_BLOOM_TEXTURE);

            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, 0, 1);

            BUILDER.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
            Matrix4f matrix = matrixStack.getLast().getMatrix();

            int r = ColorUtils.getRed(colorGlow);
            int g = ColorUtils.getGreen(colorGlow);
            int b = ColorUtils.getBlue(colorGlow);
            int a = ColorUtils.getAlpha(colorGlow);

            BUILDER.pos(matrix, -2.5f, -2.5f, 0).color(r, g, b, a).tex(0, 0).endVertex();
            BUILDER.pos(matrix, -2.5f, 2.5f, 0).color(r, g, b, a).tex(0, 1).endVertex();
            BUILDER.pos(matrix, 2.5f, 2.5f, 0).color(r, g, b, a).tex(1, 1).endVertex();
            BUILDER.pos(matrix, 2.5f, -2.5f, 0).color(r, g, b, a).tex(1, 0).endVertex();

            TESSELLATOR.draw();

            matrixStack.pop();
            RenderSystem.depthMask(true);
            RenderSystem.defaultBlendFunc();
            RenderSystem.color4f(1, 1, 1, 1);

            matrixStack.pop();
        }
    }

    private void drawAxisBox(MatrixStack matrixStack, AxisAlignedBB aabb, int colorOut, int colorFill) {
        if (aabb == null || (ColorUtils.getAlpha(colorOut) == 0 && ColorUtils.getAlpha(colorFill) == 0)) return;

        Matrix4f matrix = matrixStack.getLast().getMatrix();

        BUILDER.begin(2, DefaultVertexFormats.POSITION_COLOR);
        BUILDER.pos(matrix, (float) aabb.minX, (float) aabb.maxY, (float) aabb.minZ).color(colorOut).endVertex();
        BUILDER.pos(matrix, (float) aabb.minX, (float) aabb.maxY, (float) aabb.maxZ).color(colorOut).endVertex();
        BUILDER.pos(matrix, (float) aabb.maxX, (float) aabb.maxY, (float) aabb.maxZ).color(colorOut).endVertex();
        BUILDER.pos(matrix, (float) aabb.maxX, (float) aabb.maxY, (float) aabb.minZ).color(colorOut).endVertex();
        TESSELLATOR.draw();

        BUILDER.begin(2, DefaultVertexFormats.POSITION_COLOR);
        BUILDER.pos(matrix, (float) aabb.minX, (float) aabb.minY, (float) aabb.minZ).color(colorOut).endVertex();
        BUILDER.pos(matrix, (float) aabb.minX, (float) aabb.minY, (float) aabb.maxZ).color(colorOut).endVertex();
        BUILDER.pos(matrix, (float) aabb.maxX, (float) aabb.minY, (float) aabb.maxZ).color(colorOut).endVertex();
        BUILDER.pos(matrix, (float) aabb.maxX, (float) aabb.minY, (float) aabb.minZ).color(colorOut).endVertex();
        TESSELLATOR.draw();

        BUILDER.begin(2, DefaultVertexFormats.POSITION_COLOR);
        BUILDER.pos(matrix, (float) aabb.minX, (float) aabb.minY, (float) aabb.minZ).color(colorOut).endVertex();
        BUILDER.pos(matrix, (float) aabb.minX, (float) aabb.maxY, (float) aabb.minZ).color(colorOut).endVertex();
        BUILDER.pos(matrix, (float) aabb.maxX, (float) aabb.maxY, (float) aabb.minZ).color(colorOut).endVertex();
        BUILDER.pos(matrix, (float) aabb.maxX, (float) aabb.minY, (float) aabb.minZ).color(colorOut).endVertex();
        TESSELLATOR.draw();

        BUILDER.begin(2, DefaultVertexFormats.POSITION_COLOR);
        BUILDER.pos(matrix, (float) aabb.minX, (float) aabb.minY, (float) aabb.maxZ).color(colorOut).endVertex();
        BUILDER.pos(matrix, (float) aabb.minX, (float) aabb.maxY, (float) aabb.maxZ).color(colorOut).endVertex();
        BUILDER.pos(matrix, (float) aabb.maxX, (float) aabb.maxY, (float) aabb.maxZ).color(colorOut).endVertex();
        BUILDER.pos(matrix, (float) aabb.maxX, (float) aabb.minY, (float) aabb.maxZ).color(colorOut).endVertex();
        TESSELLATOR.draw();

        BUILDER.begin(2, DefaultVertexFormats.POSITION_COLOR);
        BUILDER.pos(matrix, (float) aabb.minX, (float) aabb.minY, (float) aabb.maxZ).color(colorOut).endVertex();
        BUILDER.pos(matrix, (float) aabb.minX, (float) aabb.maxY, (float) aabb.maxZ).color(colorOut).endVertex();
        BUILDER.pos(matrix, (float) aabb.minX, (float) aabb.maxY, (float) aabb.minZ).color(colorOut).endVertex();
        BUILDER.pos(matrix, (float) aabb.minX, (float) aabb.minY, (float) aabb.minZ).color(colorOut).endVertex();
        TESSELLATOR.draw();

        BUILDER.begin(2, DefaultVertexFormats.POSITION_COLOR);
        BUILDER.pos(matrix, (float) aabb.maxX, (float) aabb.minY, (float) aabb.maxZ).color(colorOut).endVertex();
        BUILDER.pos(matrix, (float) aabb.maxX, (float) aabb.maxY, (float) aabb.maxZ).color(colorOut).endVertex();
        BUILDER.pos(matrix, (float) aabb.maxX, (float) aabb.maxY, (float) aabb.minZ).color(colorOut).endVertex();
        BUILDER.pos(matrix, (float) aabb.maxX, (float) aabb.minY, (float) aabb.minZ).color(colorOut).endVertex();
        TESSELLATOR.draw();

        BUILDER.begin(7, DefaultVertexFormats.POSITION_COLOR);
        BUILDER.pos(matrix, (float) aabb.minX, (float) aabb.minY, (float) aabb.minZ).color(colorFill).endVertex();
        BUILDER.pos(matrix, (float) aabb.minX, (float) aabb.maxY, (float) aabb.minZ).color(colorFill).endVertex();
        BUILDER.pos(matrix, (float) aabb.maxX, (float) aabb.minY, (float) aabb.minZ).color(colorFill).endVertex();
        BUILDER.pos(matrix, (float) aabb.maxX, (float) aabb.maxY, (float) aabb.minZ).color(colorFill).endVertex();
        BUILDER.pos(matrix, (float) aabb.maxX, (float) aabb.minY, (float) aabb.maxZ).color(colorFill).endVertex();
        BUILDER.pos(matrix, (float) aabb.maxX, (float) aabb.maxY, (float) aabb.maxZ).color(colorFill).endVertex();
        BUILDER.pos(matrix, (float) aabb.minX, (float) aabb.minY, (float) aabb.maxZ).color(colorFill).endVertex();
        BUILDER.pos(matrix, (float) aabb.minX, (float) aabb.maxY, (float) aabb.maxZ).color(colorFill).endVertex();
        TESSELLATOR.draw();

        BUILDER.begin(7, DefaultVertexFormats.POSITION_COLOR);
        BUILDER.pos(matrix, (float) aabb.maxX, (float) aabb.maxY, (float) aabb.minZ).color(colorFill).endVertex();
        BUILDER.pos(matrix, (float) aabb.maxX, (float) aabb.minY, (float) aabb.minZ).color(colorFill).endVertex();
        BUILDER.pos(matrix, (float) aabb.minX, (float) aabb.maxY, (float) aabb.minZ).color(colorFill).endVertex();
        BUILDER.pos(matrix, (float) aabb.minX, (float) aabb.minY, (float) aabb.minZ).color(colorFill).endVertex();
        BUILDER.pos(matrix, (float) aabb.minX, (float) aabb.maxY, (float) aabb.maxZ).color(colorFill).endVertex();
        BUILDER.pos(matrix, (float) aabb.minX, (float) aabb.minY, (float) aabb.maxZ).color(colorFill).endVertex();
        BUILDER.pos(matrix, (float) aabb.maxX, (float) aabb.maxY, (float) aabb.maxZ).color(colorFill).endVertex();
        BUILDER.pos(matrix, (float) aabb.maxX, (float) aabb.minY, (float) aabb.maxZ).color(colorFill).endVertex();
        TESSELLATOR.draw();

        BUILDER.begin(7, DefaultVertexFormats.POSITION_COLOR);
        BUILDER.pos(matrix, (float) aabb.minX, (float) aabb.maxY, (float) aabb.minZ).color(colorFill).endVertex();
        BUILDER.pos(matrix, (float) aabb.maxX, (float) aabb.maxY, (float) aabb.minZ).color(colorFill).endVertex();
        BUILDER.pos(matrix, (float) aabb.maxX, (float) aabb.maxY, (float) aabb.maxZ).color(colorFill).endVertex();
        BUILDER.pos(matrix, (float) aabb.minX, (float) aabb.maxY, (float) aabb.maxZ).color(colorFill).endVertex();
        BUILDER.pos(matrix, (float) aabb.minX, (float) aabb.maxY, (float) aabb.minZ).color(colorFill).endVertex();
        BUILDER.pos(matrix, (float) aabb.minX, (float) aabb.maxY, (float) aabb.maxZ).color(colorFill).endVertex();
        BUILDER.pos(matrix, (float) aabb.maxX, (float) aabb.maxY, (float) aabb.maxZ).color(colorFill).endVertex();
        BUILDER.pos(matrix, (float) aabb.maxX, (float) aabb.maxY, (float) aabb.minZ).color(colorFill).endVertex();
        TESSELLATOR.draw();

        BUILDER.begin(7, DefaultVertexFormats.POSITION_COLOR);
        BUILDER.pos(matrix, (float) aabb.minX, (float) aabb.minY, (float) aabb.minZ).color(colorFill).endVertex();
        BUILDER.pos(matrix, (float) aabb.maxX, (float) aabb.minY, (float) aabb.minZ).color(colorFill).endVertex();
        BUILDER.pos(matrix, (float) aabb.maxX, (float) aabb.minY, (float) aabb.maxZ).color(colorFill).endVertex();
        BUILDER.pos(matrix, (float) aabb.minX, (float) aabb.minY, (float) aabb.maxZ).color(colorFill).endVertex();
        BUILDER.pos(matrix, (float) aabb.minX, (float) aabb.minY, (float) aabb.minZ).color(colorFill).endVertex();
        BUILDER.pos(matrix, (float) aabb.minX, (float) aabb.minY, (float) aabb.maxZ).color(colorFill).endVertex();
        BUILDER.pos(matrix, (float) aabb.maxX, (float) aabb.minY, (float) aabb.maxZ).color(colorFill).endVertex();
        BUILDER.pos(matrix, (float) aabb.maxX, (float) aabb.minY, (float) aabb.minZ).color(colorFill).endVertex();
        TESSELLATOR.draw();

        BUILDER.begin(7, DefaultVertexFormats.POSITION_COLOR);
        BUILDER.pos(matrix, (float) aabb.minX, (float) aabb.minY, (float) aabb.minZ).color(colorFill).endVertex();
        BUILDER.pos(matrix, (float) aabb.minX, (float) aabb.maxY, (float) aabb.minZ).color(colorFill).endVertex();
        BUILDER.pos(matrix, (float) aabb.minX, (float) aabb.minY, (float) aabb.maxZ).color(colorFill).endVertex();
        BUILDER.pos(matrix, (float) aabb.minX, (float) aabb.maxY, (float) aabb.maxZ).color(colorFill).endVertex();
        BUILDER.pos(matrix, (float) aabb.maxX, (float) aabb.minY, (float) aabb.maxZ).color(colorFill).endVertex();
        BUILDER.pos(matrix, (float) aabb.maxX, (float) aabb.maxY, (float) aabb.maxZ).color(colorFill).endVertex();
        BUILDER.pos(matrix, (float) aabb.maxX, (float) aabb.minY, (float) aabb.minZ).color(colorFill).endVertex();
        BUILDER.pos(matrix, (float) aabb.maxX, (float) aabb.maxY, (float) aabb.minZ).color(colorFill).endVertex();
        TESSELLATOR.draw();

        BUILDER.begin(7, DefaultVertexFormats.POSITION_COLOR);
        BUILDER.pos(matrix, (float) aabb.minX, (float) aabb.maxY, (float) aabb.maxZ).color(colorFill).endVertex();
        BUILDER.pos(matrix, (float) aabb.minX, (float) aabb.minY, (float) aabb.maxZ).color(colorFill).endVertex();
        BUILDER.pos(matrix, (float) aabb.minX, (float) aabb.maxY, (float) aabb.minZ).color(colorFill).endVertex();
        BUILDER.pos(matrix, (float) aabb.minX, (float) aabb.minY, (float) aabb.minZ).color(colorFill).endVertex();
        BUILDER.pos(matrix, (float) aabb.maxX, (float) aabb.maxY, (float) aabb.minZ).color(colorFill).endVertex();
        BUILDER.pos(matrix, (float) aabb.maxX, (float) aabb.minY, (float) aabb.minZ).color(colorFill).endVertex();
        BUILDER.pos(matrix, (float) aabb.maxX, (float) aabb.maxY, (float) aabb.maxZ).color(colorFill).endVertex();
        BUILDER.pos(matrix, (float) aabb.maxX, (float) aabb.minY, (float) aabb.maxZ).color(colorFill).endVertex();
        TESSELLATOR.draw();
    }


    private void renderDiamond(EventDisplay e) {
        if (this.currentTarget == null || this.currentTarget == mc.player) return;

        long now = System.currentTimeMillis();
        double sin = Math.sin(now / 1000.0);
        Vector3d interpolated = getInterpolatedPos(currentTarget, e.getPartialTicks());

        float baseSize = diamondSize.get().floatValue();

        Vector2f pos = ProjectionUtil.project((float)interpolated.x,
                (float)(interpolated.y + currentTarget.getHeight() / 1.95F),
                (float)interpolated.z);
        if (pos == null) return;

        Vector3d cameraPos = mc.getRenderManager().info.getProjectedView();
        double distance = cameraPos.distanceTo(interpolated);

        float perspectiveCompensation = (float)(distance / 10.0);
        float size = baseSize * perspectiveCompensation;
        size = MathHelper.clamp(size, baseSize * 0.8f, baseSize * 1.2f);

        boolean wasBlendEnabled = GL11.glIsEnabled(GL11.GL_BLEND);
        boolean wasAlphaTestEnabled = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);
        boolean wasCullEnabled = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean wasDepthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);

        GL11.glGetIntegerv(GL11.GL_BLEND_SRC, blendSrcScratch);
        GL11.glGetIntegerv(GL11.GL_BLEND_DST, blendDstScratch);
        int blendSrc = blendSrcScratch[0];
        int blendDst = blendDstScratch[0];

        GlStateManager.pushMatrix();

        try {
            GlStateManager.clearCurrentColor();
            GlStateManager.color4f(1.0f, 1.0f, 1.0f, 1.0f);

            GlStateManager.translatef(pos.x, pos.y, 0.0F);

            float scale = (float) this.scaleAnimation.getOutput() / 255f;
            GlStateManager.scalef(scale, scale, 1.0F);

            GlStateManager.rotatef((float) sin * 360.0F, 0.0F, 0.0F, 1.0F);
            GlStateManager.translatef(-pos.x, -pos.y, 0.0F);

            GlStateManager.enableBlend();
            GlStateManager.disableAlphaTest();
            GlStateManager.disableCull();
            GlStateManager.depthMask(false);

            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            GlStateManager.enableTexture();

            int alphaValue = (int) this.alpha.getOutput();
            int themeColor = Theme.MainColor(0);

            if (currentTarget.hurtTime > 0) {
                float hurtProgress = currentTarget.hurtTime / 10f;
                themeColor = ColorUtils.interpolateColor(themeColor, ColorUtils.rgb(255, 100, 100), hurtProgress);
            }

            int color1 = ColorUtils.setAlpha(ColorUtils.brighter(themeColor, 0.3f), alphaValue);
            int color2 = ColorUtils.setAlpha(themeColor, alphaValue);
            int color3 = ColorUtils.setAlpha(ColorUtils.brighter(themeColor, 0.2f), alphaValue);
            int color4 = ColorUtils.setAlpha(ColorUtils.darker(themeColor, 0.2f), alphaValue);

            ResourceLocation targetTexture = getTargetTexture();
            diamondColors.x = color1;
            diamondColors.y = color2;
            diamondColors.z = color3;
            diamondColors.w = color4;

            RenderUtility.drawImageAlpha(targetTexture,
                    pos.x - size / 2.0F, pos.y - size / 2.0F, size, size,
                    diamondColors);

        } finally {
            GlStateManager.popMatrix();

            GlStateManager.clearCurrentColor();
            GlStateManager.color4f(1.0f, 1.0f, 1.0f, 1.0f);

            GlStateManager.depthMask(wasDepthMask);

            if (wasBlendEnabled) {
                GlStateManager.enableBlend();
            } else {
                GlStateManager.disableBlend();
            }
            GlStateManager.blendFunc(blendSrc, blendDst);

            if (wasAlphaTestEnabled) {
                GlStateManager.enableAlphaTest();
            } else {
                GlStateManager.disableAlphaTest();
            }

            if (wasCullEnabled) {
                GlStateManager.enableCull();
            } else {
                GlStateManager.disableCull();
            }

            GlStateManager.enableTexture();
        }
    }

    private void renderGhosts(MatrixStack ms, WorldEvent e) {
        if (this.currentTarget == null || this.currentTarget == mc.player) return;

        ms.push();
        RenderSystem.pushMatrix();
        RenderSystem.disableLighting();
        depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.shadeModel(7425);
        RenderSystem.disableCull();
        RenderSystem.disableAlphaTest();
        RenderSystem.blendFuncSeparate(770, 1, 0, 1);

        float entityWidth = currentTarget.getWidth();
        float entityHeight = currentTarget.getHeight();
        float baseWidth = 0.6f;
        float baseHeight = 1.8f;

        float widthScale = entityWidth / baseWidth;
        float heightScale = entityHeight / baseHeight;
        float entityScale = (widthScale + heightScale) / 2.0f;

        double radius = 0.67 * entityScale;
        float speed = 45;
        float size = 0.4f * entityScale;
        double distance = 19;
        int length = 20;
        int maxAlpha = (int) (255 * this.alpha.getOutput() / 255.0);
        int alphaFactor = 15;
        ActiveRenderInfo camera = mc.getRenderManager().info;
        Vector3d cameraPos = camera.getProjectedView();
        long now = System.currentTimeMillis();

        ms.translate(-cameraPos.getX(), -cameraPos.getY(), -cameraPos.getZ());

        Vector3d interpolated = getInterpolatedPos(currentTarget, e.getPartialTicks());

        float yOffset = 0.75f * heightScale;
        float yTranslate = 0.5f * heightScale;

        interpolated = interpolated.add(0, yOffset, 0);
        ms.translate(interpolated.x + 0.2f * widthScale, interpolated.y + yTranslate, interpolated.z);

        float scale = (float) this.scaleAnimation.getOutput() / 255f;
        ms.scale(scale, scale, scale);

        mc.getTextureManager().bindTexture(PARTICLE_BLOOM_TEXTURE);

        int baseThemeColor = Theme.MainColor(0);

        if (currentTarget.hurtTime > 0) {
            float hurtProgress = currentTarget.hurtTime / 10f;
            baseThemeColor = ColorUtils.interpolateColor(baseThemeColor, ColorUtils.rgb(255, 100, 100), hurtProgress);
        }

        int ghostColorBright = ColorUtils.brighter(baseThemeColor, 0.3f);
        int ghostColorBase = baseThemeColor;
        int ghostColorDark = ColorUtils.darker(baseThemeColor, 0.2f);

        for (int colorIndex = 0; colorIndex < 3; colorIndex++) {
            for (int i = 0; i < length; i++) {
                Quaternion r = camera.getRotation().copy();
                BUILDER.begin(GL_QUADS, POSITION_COLOR_TEX);

                double angle = 0.15f * (now - lastTime - (i * distance)) / speed;
                double s, c;

                if (colorIndex == 0) {
                    s = Math.sin(angle) * radius;
                    c = Math.cos(angle) * radius;
                    ms.translate(s, c, -c);
                } else if (colorIndex == 1) {
                    s = Math.sin(angle) * radius;
                    c = Math.cos(angle) * radius;
                    ms.translate(-s, s, -c);
                } else {
                    s = Math.sin(angle) * radius;
                    c = Math.cos(angle) * radius;
                    ms.translate(-s, -s, c);
                }

                ms.translate(-size / 2f, -size / 2f, 0);
                ms.rotate(r);
                ms.translate(size / 2f, size / 2f, 0);

                int color = colorIndex == 0 ? ghostColorBright : colorIndex == 1 ? ghostColorBase : ghostColorDark;
                int alphaVal = MathHelper.clamp(maxAlpha - (i * alphaFactor), 0, maxAlpha);
                int finalColor = ColorUtils.setAlpha(color, alphaVal);
                Matrix4f matrix = ms.getLast().getMatrix();
                int rColor = ColorUtils.getRed(finalColor);
                int gColor = ColorUtils.getGreen(finalColor);
                int bColor = ColorUtils.getBlue(finalColor);
                int aColor = ColorUtils.getAlpha(finalColor);

                BUILDER.pos(matrix, 0, -size, 0).color(rColor, gColor, bColor, aColor).tex(0, 0).endVertex();
                BUILDER.pos(matrix, -size, -size, 0).color(rColor, gColor, bColor, aColor).tex(0, 1).endVertex();
                BUILDER.pos(matrix, -size, 0, 0).color(rColor, gColor, bColor, aColor).tex(1, 1).endVertex();
                BUILDER.pos(matrix, 0, 0, 0).color(rColor, gColor, bColor, aColor).tex(1, 0).endVertex();
                TESSELLATOR.draw();

                ms.translate(-size / 2f, -size / 2f, 0);
                r.conjugate();
                ms.rotate(r);
                ms.translate(size / 2f, size / 2f, 0);

                if (colorIndex == 0) {
                    ms.translate(-s, -c, c);
                } else if (colorIndex == 1) {
                    ms.translate(s, -s, c);
                } else {
                    ms.translate(s, s, -c);
                }
            }
        }

        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.enableAlphaTest();
        depthMask(true);
        RenderSystem.popMatrix();
        ms.pop();
    }

    private void renderChain(MatrixStack ms, LivingEntity target) {
        this.chainTargetAnim.setDirection(this.target != null ? Direction.FORWARDS : Direction.BACKWARDS);
        this.chainTarget2Anim.setDirection(this.target != null ? Direction.FORWARDS : Direction.BACKWARDS);
        if (this.chainTargetAnim.finished(Direction.BACKWARDS)) return;

        if (this.prevTarget == null) return;

        if (throughWalls.get()) {
            RenderSystem.disableDepthTest();
            RenderSystem.disableCull();
        }

        this.hurtAnim.setDirection(this.prevTarget.hurtTime > 0 ? Direction.FORWARDS : Direction.BACKWARDS);

        ActiveRenderInfo camera = mc.getRenderManager().info;
        Vector3d cameraPos = camera.getProjectedView();
        float partialTicks = mc.getRenderPartialTicks();
        double entX = prevTarget.lastTickPosX + (prevTarget.getPosX() - prevTarget.lastTickPosX) * partialTicks - cameraPos.getX();
        double entY = prevTarget.lastTickPosY + (prevTarget.getPosY() - prevTarget.lastTickPosY) * partialTicks - cameraPos.getY() - 0.5f;
        double entZ = prevTarget.lastTickPosZ + (prevTarget.getPosZ() - prevTarget.lastTickPosZ) * partialTicks - cameraPos.getZ();

        float rotSpeed = 0.5f;
        float chainSize = 4;
        float down = 1;

        float movingValue = getMovingValue();
        float movingSin = (float) Math.sin(Math.toRadians(movingValue));
        float gradusX = 20 * Math.min(1 + movingSin, 1);
        float gradusZ = 20 * (Math.min(1 + movingSin, 2) - 1);
        float movingRotation = movingValue * rotSpeed;
        float width = prevTarget.getWidth() * 1.5f;
        int modif = 45 / 2;

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        RenderSystem.disableCull();

        int themeColor = Theme.MainColor(0);
        int secondThemeColor = Theme.RectColor(0);

        if (prevTarget.hurtTime > 0) {
            float hurtProgress = prevTarget.hurtTime / 10f;
            themeColor = ColorUtils.interpolateColor(themeColor, ColorUtils.rgb(255, 100, 100), hurtProgress);
            secondThemeColor = ColorUtils.interpolateColor(secondThemeColor, ColorUtils.rgb(200, 80, 80), hurtProgress);
        }

        for (int chain = 0; chain < 2; chain++) {
            float val = 1.2f - 0.5f * (chain == 0 ? (float) chainTargetAnim.getOutput() / 255f : (float) this.chainTarget2Anim.getOutput() / 255f);
            float chainGradusX = chain == 0 ? gradusX : -gradusX;
            float chainGradusZ = chain == 0 ? gradusZ : -gradusZ;
            float chainOffsetX = chainGradusX / 100F;
            float chainOffsetZ = -chainGradusZ / 100F;

            for (int glowLayer = 2; glowLayer >= 0; glowLayer--) {
                ms.push();
                ms.translate(entX, entY + prevTarget.getHeight() / 2, entZ);
                float x = 0, y = 0, z = 0;

                Matrix4f matrix = ms.getLast().getMatrix();
                ms.rotate(Vector3f.ZP.rotationDegrees(chainGradusX));
                ms.rotate(Vector3f.XP.rotationDegrees(chainGradusZ));

                int alphaVal = chain == 0 ? (int) chainTargetAnim.getOutput() : (int) this.chainTarget2Anim.getOutput();

                if (glowLayer == 0) {
                    mc.getTextureManager().bindTexture(CHAIN_TEXTURE);
                    RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);

                    BUILDER.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX_LIGHTMAP);

                    for (int i = 0; i < 360 * 2; i += modif) {
                        float prevSin = (float) (x + chainOffsetX + Math.sin(Math.toRadians(i - modif + movingRotation)) * width * val);
                        float prevCos = (float) (z + chainOffsetZ + Math.cos(Math.toRadians(i - modif + movingRotation)) * width * val);

                        float sin = (float) (x + chainOffsetX + Math.sin(Math.toRadians(i + movingRotation)) * width * val);
                        float cos = (float) (z + chainOffsetZ + Math.cos(Math.toRadians(i + movingRotation)) * width * val);

                        BUILDER.pos(matrix, prevSin, y, prevCos).color(255, 255, 255, alphaVal).tex(1 / 360F * (float) (i - modif) * chainSize, 0).lightmap(0, 240).endVertex();
                        BUILDER.pos(matrix, sin, y, cos).color(255, 255, 255, alphaVal).tex(1 / 360F * (float) (i) * chainSize, 0).lightmap(0, 240).endVertex();
                        BUILDER.pos(matrix, sin, y + down, cos).color(255, 255, 255, alphaVal).tex(1 / 360F * (float) (i) * chainSize, 1 - 0.01f).lightmap(0, 240).endVertex();
                        BUILDER.pos(matrix, prevSin, y + down, prevCos).color(255, 255, 255, alphaVal).tex(1 / 360F * (float) (i - modif) * chainSize, 1 - 0.01f).lightmap(0, 240).endVertex();
                    }
                } else {
                    RenderSystem.disableTexture();

                    float glowSize = 1.0f + glowLayer * 0.4f;
                    float glowAlpha = alphaVal * (0.6f - glowLayer * 0.2f);

                    int glowColor = chain == 0 ?
                            ColorUtils.setAlpha(themeColor, (int) glowAlpha) :
                            ColorUtils.setAlpha(secondThemeColor, (int) glowAlpha);

                    BUILDER.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
                    int r = ColorUtils.getRed(glowColor);
                    int g = ColorUtils.getGreen(glowColor);
                    int b = ColorUtils.getBlue(glowColor);
                    int a = ColorUtils.getAlpha(glowColor);
                    float glowDown = down * glowSize;
                    float glowWidth = width * val * glowSize;

                    for (int i = 0; i < 360 * 2; i += modif) {
                        float prevSin = (float) (x + chainOffsetX + Math.sin(Math.toRadians(i - modif + movingRotation)) * glowWidth);
                        float prevCos = (float) (z + chainOffsetZ + Math.cos(Math.toRadians(i - modif + movingRotation)) * glowWidth);

                        float sin = (float) (x + chainOffsetX + Math.sin(Math.toRadians(i + movingRotation)) * glowWidth);
                        float cos = (float) (z + chainOffsetZ + Math.cos(Math.toRadians(i + movingRotation)) * glowWidth);

                        BUILDER.pos(matrix, prevSin, y - glowDown * 0.1f, prevCos).color(r, g, b, a).endVertex();
                        BUILDER.pos(matrix, sin, y - glowDown * 0.1f, cos).color(r, g, b, a).endVertex();
                        BUILDER.pos(matrix, sin, y + glowDown * 1.1f, cos).color(r, g, b, a).endVertex();
                        BUILDER.pos(matrix, prevSin, y + glowDown * 1.1f, prevCos).color(r, g, b, a).endVertex();
                    }

                    RenderSystem.enableTexture();
                }

                TESSELLATOR.draw();
                ms.pop();
            }
        }

        RenderSystem.defaultBlendFunc();
        RenderSystem.disableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
    }

    private Vector3d getInterpolatedPos(LivingEntity entity, float partialTicks) {
        return new Vector3d(
                entity.lastTickPosX + (entity.getPosX() - entity.lastTickPosX) * partialTicks,
                entity.lastTickPosY + (entity.getPosY() - entity.lastTickPosY) * partialTicks,
                entity.lastTickPosZ + (entity.getPosZ() - entity.lastTickPosZ) * partialTicks
        );
    }

    private float getMovingValue() {
        return (System.currentTimeMillis() % 360000) * 0.15f;
    }
}
