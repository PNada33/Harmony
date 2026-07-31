package xd.harm.modules.impl.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.play.server.SUpdateTimePacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.GameRules;
import org.lwjgl.opengl.GL11;
import xd.harm.Harmony;
import xd.harm.events.network.EventPacket;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.ColorSetting;
import xd.harm.modules.settings.impl.ModeListSetting;
import xd.harm.modules.settings.impl.ModeSetting;
import xd.harm.modules.settings.impl.SliderSetting;
import xd.harm.ui.styles.StyleManager;
import xd.harm.utils.render.color.ColorUtils;

import java.awt.Color;

@ModuleRegister(name = "WorldTweaks", category = Category.Render, desc = "Настройки мира")
public class WorldTweaks extends Module {

    private static final float SMALL_PLAYER_SCALE = 0.5F;
    private static final BooleanSetting SMALL_PLAYER_OPTION = new BooleanSetting("Маленький игрок", false);
    private static final BooleanSetting CUSTOM_FOG_OPTION = new BooleanSetting("Кастомный туман", false);
    private static final BooleanSetting TIME_OPTION = new BooleanSetting("Время", true);
    private static final BooleanSetting WORLD_COLOR_OPTION = new BooleanSetting("Цвет мира", false);

    public static ModeListSetting options = new ModeListSetting("Опции",
            SMALL_PLAYER_OPTION,
            CUSTOM_FOG_OPTION,
            TIME_OPTION,
            WORLD_COLOR_OPTION
    );

    public final BooleanSetting smallPlayerCameraDown = new BooleanSetting("Опустить камеру", true)
            .setVisible(() -> SMALL_PLAYER_OPTION.get());

    public static ModeSetting mode = new ModeSetting("Вид", "Клиент", "Радужный", "Клиент", "Свой", "Свой 2 цвета")
            .setVisible(() -> CUSTOM_FOG_OPTION.get());
    public static ColorSetting colorFog = new ColorSetting("Цвет тумана", ColorUtils.rgb(255, 255, 255))
            .setVisible(() -> mode.get().contains("Свой"));
    public static ColorSetting colorFog2 = new ColorSetting("Цвет тумана 2", ColorUtils.rgb(100, 100, 255))
            .setVisible(() -> mode.is("Свой 2 цвета"));

    private static ModeSetting time = new ModeSetting("Время", "Ночь", "Ночь", "День")
            .setVisible(() -> TIME_OPTION.get());

    public static ModeSetting worldColorMode = new ModeSetting("Вид мира", "Клиент", "Радужный", "Клиент", "Свой", "Свой 2 цвета")
            .setVisible(() -> WORLD_COLOR_OPTION.get());

    public final ColorSetting worldColor = new ColorSetting("Цвет мира", new Color(255, 255, 255).getRGB())
            .setVisible(() -> WORLD_COLOR_OPTION.get() && worldColorMode.get().contains("Свой"));

    public final ColorSetting worldColor2 = new ColorSetting("Цвет мира 2", new Color(100, 100, 255).getRGB())
            .setVisible(() -> WORLD_COLOR_OPTION.get() && worldColorMode.is("Свой 2 цвета"));

    public final SliderSetting worldIntensity = new SliderSetting("Сила", 70, 0, 100, 1)
            .setVisible(() -> WORLD_COLOR_OPTION.get());

    public static boolean child;
    private static WorldTweaks instance;
    private float savedGamma = 1.0F;
    private boolean isRenderingColorFilter = false;
    private boolean timeWasForced = false;
    private long lastForcedTime = Long.MIN_VALUE;

    public WorldTweaks() {
        instance = this;
        addSettings(options, smallPlayerCameraDown, time, mode, colorFog, colorFog2, worldColorMode, worldColor, worldColor2, worldIntensity);
    }

    public static WorldTweaks getInstance() {
        return instance;
    }

    public static int getFogColor() {
        String modeValue = mode.get();
        switch (modeValue) {
            case "Радужный":
                return ColorUtils.rainbow(8, 0, 0.85F, 1.0F, 1.0F);
            case "Клиент":
                StyleManager styleManager = Harmony.getInstance().getStyleManager();
                return styleManager.getCurrentStyle().getFirstColor().getRGB();
            case "Свой":
                return colorFog.get();
            case "Свой 2 цвета":
                float t = (float) ((Math.sin(System.currentTimeMillis() / 1000.0) + 1.0) * 0.5);
                return ColorUtils.interpolateColor(colorFog.get(), colorFog2.get(), t);
            default:
                return -1;
        }
    }

    public int getWorldColor() {
        String modeValue = worldColorMode.get();
        switch (modeValue) {
            case "Радужный":
                return ColorUtils.rainbow(8, 0, 0.85F, 1.0F, 1.0F);
            case "Клиент":
                StyleManager styleManager = Harmony.getInstance().getStyleManager();
                return styleManager.getCurrentStyle().getFirstColor().getRGB();
            case "Свой":
                return worldColor.get();
            case "Свой 2 цвета":
                float t = (float) ((Math.sin(System.currentTimeMillis() / 1000.0) + 1.0) * 0.5);
                return ColorUtils.interpolateColor(worldColor.get(), worldColor2.get(), t);
            default:
                return -1;
        }
    }

    public static void applyWorldColorFilterEarly() {
        WorldTweaks worldTweaks = instance;
        if (worldTweaks == null || !worldTweaks.isState() || worldTweaks.isRenderingColorFilter) {
            return;
        }
        if (worldTweaks.mc.world == null || worldTweaks.mc.player == null) {
            return;
        }
        if (worldTweaks.isWorldColorOptionEnabled() && worldTweaks.worldIntensity.get().floatValue() > 0.0F) {
            worldTweaks.applyColorFilter();
        }
    }

    @Override
    public boolean onEnable() {
        super.onEnable();
        if (mc.gameSettings != null) {
            savedGamma = (float) mc.gameSettings.gamma;
        }
        resetAllGLStates();
        return false;
    }

    @Override
    public boolean onDisable() {
        super.onDisable();
        child = false;
        isRenderingColorFilter = false;
        if (mc.gameSettings != null) {
            mc.gameSettings.gamma = savedGamma;
        }
        releaseForcedTime();
        resetAllGLStates();
        return false;
    }

    private void resetAllGLStates() {
        try {
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.enableAlphaTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        } catch (Exception ignored) {
        }
    }

    @Subscribe
    public void onPacket(EventPacket event) {
        if (!(event.getPacket() instanceof SUpdateTimePacket)) {
            return;
        }

        SUpdateTimePacket packet = (SUpdateTimePacket) event.getPacket();
        if (!TIME_OPTION.get()) {
            return;
        }

        packet.worldTime = -getForcedTime();
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        boolean smallPlayerEnabled = SMALL_PLAYER_OPTION.get();
        boolean customFogEnabled = CUSTOM_FOG_OPTION.get();
        boolean timeEnabled = TIME_OPTION.get();
        boolean worldColorEnabled = WORLD_COLOR_OPTION.get();

        if (!smallPlayerEnabled
                && !customFogEnabled
                && !timeEnabled
                && !worldColorEnabled) {
            toggle();
        }

        if (timeEnabled) {
            applyForcedTime();
        } else {
            releaseForcedTime();
        }

        child = smallPlayerEnabled;
    }

    private static long getForcedTime() {
        return time.get().equalsIgnoreCase("День") ? 1000L : 18000L;
    }

    private void applyForcedTime() {
        if (mc.world == null) {
            return;
        }

        long forcedTime = getForcedTime();
        boolean daylightCycle = mc.world.getGameRules().getBoolean(GameRules.DO_DAYLIGHT_CYCLE);
        if (!timeWasForced || lastForcedTime != forcedTime || mc.world.getDayTime() != forcedTime || daylightCycle) {
            mc.world.setDayTime(-forcedTime);
            lastForcedTime = forcedTime;
            timeWasForced = true;
        }
    }

    private void releaseForcedTime() {
        if (!timeWasForced) {
            return;
        }

        if (mc.world != null) {
            mc.world.setDayTime(Math.abs(mc.world.getDayTime()));
        }
        timeWasForced = false;
        lastForcedTime = Long.MIN_VALUE;
    }

    private void applyColorFilter() {
        if (isRenderingColorFilter) {
            return;
        }

        int width = mc.getMainWindow().getWidth();
        int height = mc.getMainWindow().getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        try {
            isRenderingColorFilter = true;

            RenderSystem.pushMatrix();
            RenderSystem.matrixMode(GL11.GL_PROJECTION);
            RenderSystem.pushMatrix();
            RenderSystem.loadIdentity();
            RenderSystem.ortho(0.0D, width, height, 0.0D, 1000.0D, 3000.0D);
            RenderSystem.matrixMode(GL11.GL_MODELVIEW);
            RenderSystem.pushMatrix();
            RenderSystem.loadIdentity();
            RenderSystem.translatef(0.0F, 0.0F, -2000.0F);

            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableAlphaTest();
            RenderSystem.disableCull();
            RenderSystem.enableBlend();
            RenderSystem.disableTexture();
            RenderSystem.blendFunc(GL11.GL_DST_COLOR, GL11.GL_SRC_COLOR);

            int selectedColor = getWorldColor();
            float intensity = Math.min(worldIntensity.get().floatValue() / 100.0F, 0.8F);
            float cr = (selectedColor >> 16 & 255) / 255.0F;
            float cg = (selectedColor >> 8 & 255) / 255.0F;
            float cb = (selectedColor & 255) / 255.0F;
            float r = lerp(1.0F, cr, intensity);
            float g = lerp(1.0F, cg, intensity);
            float b = lerp(1.0F, cb, intensity);

            RenderSystem.color4f(Math.max(r, 0.4F), Math.max(g, 0.4F), Math.max(b, 0.4F), 1.0F);
            drawFullScreenQuad(width, height);

            RenderSystem.matrixMode(GL11.GL_MODELVIEW);
            RenderSystem.popMatrix();
            RenderSystem.matrixMode(GL11.GL_PROJECTION);
            RenderSystem.popMatrix();
            RenderSystem.matrixMode(GL11.GL_MODELVIEW);
            RenderSystem.popMatrix();

            RenderSystem.enableTexture();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);
            RenderSystem.enableAlphaTest();
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        } catch (Exception e) {
            try {
                RenderSystem.matrixMode(GL11.GL_MODELVIEW);
                RenderSystem.popMatrix();
                RenderSystem.matrixMode(GL11.GL_PROJECTION);
                RenderSystem.popMatrix();
                RenderSystem.matrixMode(GL11.GL_MODELVIEW);
                RenderSystem.popMatrix();
            } catch (Exception ignored) {
            }
            resetAllGLStates();
        } finally {
            isRenderingColorFilter = false;
        }
    }

    private void drawFullScreenQuad(int width, int height) {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(0.0F, 0.0F);
        GL11.glVertex2f(0.0F, height);
        GL11.glVertex2f(width, height);
        GL11.glVertex2f(width, 0.0F);
        GL11.glEnd();
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    public static boolean isSmallPlayerEnabled() {
        WorldTweaks worldTweaks = instance;
        return worldTweaks != null && worldTweaks.isState() && SMALL_PLAYER_OPTION.get();
    }

    public static float getSmallPlayerScale() {
        return SMALL_PLAYER_SCALE;
    }

    public static float getSmallPlayerRenderScale(Entity entity) {
        if (!isSmallPlayerEntity(entity)) {
            return 1.0F;
        }

        return getSmallPlayerRenderScaleUnchecked(entity);
    }

    private static float getSmallPlayerRenderScaleUnchecked(Entity entity) {
        float baseScale = SMALL_PLAYER_SCALE;
        WorldTweaks worldTweaks = instance;
        if (worldTweaks == null || !worldTweaks.smallPlayerCameraDown.get()) {
            return baseScale;
        }

        float eyeHeight = entity.getEyeHeight();
        if (eyeHeight <= 1.0E-4F) {
            return baseScale;
        }

        float adjustedEyeHeight = eyeHeight + getSmallPlayerCameraYOffsetUnchecked(entity);
        return MathHelper.clamp(adjustedEyeHeight / eyeHeight, baseScale, 0.95F);
    }

    public static boolean isSmallPlayerEntity(Entity entity) {
        if (!(entity instanceof PlayerEntity)) {
            return false;
        }

        WorldTweaks worldTweaks = instance;
        if (worldTweaks == null || !worldTweaks.isState() || !SMALL_PLAYER_OPTION.get()) {
            return false;
        }

        PlayerEntity player = Minecraft.getInstance().player;
        return player != null && player.getUniqueID().equals(entity.getUniqueID());
    }

    public static float getSmallPlayerYOffset(Entity entity) {
        if (!isSmallPlayerEntity(entity)) {
            return 0.0F;
        }

        float renderScale = getSmallPlayerRenderScaleUnchecked(entity);
        return -(1.0F - renderScale) * entity.getHeight() * 0.76F;
    }

    public static float getSmallPlayerCameraYOffset(Entity entity) {
        if (!isSmallPlayerEntity(entity)) {
            return 0.0F;
        }

        WorldTweaks worldTweaks = instance;
        if (worldTweaks == null || !worldTweaks.smallPlayerCameraDown.get()) {
            return 0.0F;
        }

        return getSmallPlayerCameraYOffsetUnchecked(entity);
    }

    private static float getSmallPlayerCameraYOffsetUnchecked(Entity entity) {
        float offset = -(1.0F - SMALL_PLAYER_SCALE) * entity.getHeight() * 0.68F;
        return MathHelper.clamp(offset, -0.75F, -0.25F);
    }

    public boolean isEnabled(String name) {
        BooleanSetting cachedOption = getCachedOption(name);
        return cachedOption != null ? cachedOption.get() : options.getValueByName(name).get();
    }

    private static BooleanSetting getCachedOption(String name) {
        if (name == null) {
            return null;
        }
        if (SMALL_PLAYER_OPTION.getName().equalsIgnoreCase(name)) {
            return SMALL_PLAYER_OPTION;
        }
        if (CUSTOM_FOG_OPTION.getName().equalsIgnoreCase(name)) {
            return CUSTOM_FOG_OPTION;
        }
        if (TIME_OPTION.getName().equalsIgnoreCase(name)) {
            return TIME_OPTION;
        }
        if (WORLD_COLOR_OPTION.getName().equalsIgnoreCase(name)) {
            return WORLD_COLOR_OPTION;
        }
        return null;
    }

    private boolean isWorldColorOptionEnabled() {
        return WORLD_COLOR_OPTION.get();
    }
}
