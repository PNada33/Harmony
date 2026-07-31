package xd.harm.modules.impl.render;

import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.SliderSetting;
import com.google.common.eventbus.Subscribe;
import xd.harm.Harmony;
import xd.harm.config.FriendStorage;
import xd.harm.events.world.EventChangeWorld;
import xd.harm.events.render.EventDisplay;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.impl.misc.StreamerMode;
import xd.harm.utils.math.MathUtil;
import xd.harm.utils.render.color.ColorUtils;
import xd.harm.utils.render.rect.RenderUtility;
import xd.harm.utils.text.font.ClientFonts;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.client.gui.screen.inventory.ChestScreen;
import net.minecraft.client.gui.screen.inventory.InventoryScreen;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import org.lwjgl.opengl.GL11;
import java.util.HashMap;
import java.util.Map;

@ModuleRegister(name = "Arrows", category = Category.Render, desc = "Рисует стрелку до игрока")
public class Arrows extends Module {

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);
    private static final float RAD_TO_DEG = (float) (180.0 / Math.PI);
    private static final float LOW_HEALTH_EXTRA_RADIUS = 40.0f;
    private static final long LOW_HEALTH_TIMER_MS = 10000L;
    private static final float ARROW_ROTATION_OFFSET = 90.0f;

    public final BooleanSetting ignore = new BooleanSetting("Игнорировать голых", false);
    public final BooleanSetting friendHealth = new BooleanSetting("Проверка здоровья друзей", true);
    public final BooleanSetting showDistance = new BooleanSetting("Показывать метры", true);

    final SliderSetting razmer = new SliderSetting("Размер", 60f, 30f, 100f, 1f);
    final SliderSetting opacity = new SliderSetting("Прозрачность", 1f, 0.3f, 1f, 0.05f);
    final SliderSetting healthThreshold = new SliderSetting("Порог здоровья", 10f, 1f, 20f, 1f);

    private final ResourceLocation arrow = new ResourceLocation("harmony/images/gui/arrow.png");
    private final Map<String, Long> lowHealthTimers = new HashMap<>();
    private final Map<String, Float> animatedSizes = new HashMap<>();

    public float animationStep;
    public float animatedRotation;

    public Arrows() {
        addSettings(ignore, friendHealth, showDistance, razmer, opacity, healthThreshold);
    }

    private boolean render = false;

    public boolean isRender() {
        return render;
    }

    public void setRender(boolean render) {
        this.render = render;
    }

    @Override
    public boolean onDisable() {
        super.onDisable();
        setRender(false);
        lowHealthTimers.clear();
        animatedSizes.clear();
        return false;
    }

    @Override
    public boolean onEnable() {
        super.onEnable();
        setRender(true);
        return false;
    }

    @Subscribe
    public void onWorldChange(EventChangeWorld e) {
        setRender(isRender());
        lowHealthTimers.clear();
        animatedSizes.clear();
    }

    @Subscribe
    public void onDisplay(EventDisplay event) {
        if (event.getType() != EventDisplay.Type.POST)
            return;

        if (mc.player == null || mc.world == null)
            return;

        ActiveRenderInfo activeRenderInfo = mc.getRenderManager().info;
        Vector3d projectedView = activeRenderInfo == null ? null : activeRenderInfo.getProjectedView();
        if (projectedView == null)
            return;

        float size = razmer.get();

        if (mc.currentScreen instanceof InventoryScreen || mc.currentScreen instanceof ChestScreen) {
            size += 100;
        }

        animationStep = MathUtil.fast(animationStep, size, 12);

        MatrixStack matrixStack = event.getMatrixStack();
        float partialTicks = mc.getRenderPartialTicks();
        double projectedX = projectedView.getX();
        double projectedZ = projectedView.getZ();
        float yawRad = activeRenderInfo.getYaw() * DEG_TO_RAD;
        double cos = MathHelper.cos(yawRad);
        double sin = MathHelper.sin(yawRad);
        float centerX = mc.getMainWindow().getScaledWidth() / 2f;
        float centerY = mc.getMainWindow().getScaledHeight() / 2f;
        boolean ignoreNaked = ignore.get();
        boolean checkFriendHealth = friendHealth.get();
        boolean renderDistance = showDistance.get();
        float baseOpacity = opacity.get();
        float healthLimit = checkFriendHealth ? healthThreshold.get() : 0.0f;
        long now = System.currentTimeMillis();

        for (AbstractClientPlayerEntity player : mc.world.getPlayers()) {
            if (mc.player == player)
                continue;

            if (ignoreNaked && isNaked(player)) continue;

            String originalName = player.getScoreboardName();
            boolean isFriend = FriendStorage.isFriend(originalName);
            boolean hasLowHealth = false;
            float health = 0.0f;
            float healthPercent = 1.0f;

            if (checkFriendHealth && isFriend) {
                health = player.getHealth();
                float maxHealth = player.getMaxHealth();
                healthPercent = maxHealth > 0.0f ? health / maxHealth : 0.0f;
                Long lowHealthStarted = lowHealthTimers.get(originalName);
                if (health <= healthLimit) {
                    if (lowHealthStarted == null) {
                        lowHealthTimers.put(originalName, now);
                    }
                    hasLowHealth = true;
                } else {
                    if (lowHealthStarted != null) {
                        if (now - lowHealthStarted >= LOW_HEALTH_TIMER_MS) {
                            lowHealthTimers.remove(originalName);
                        } else {
                            hasLowHealth = true;
                        }
                    }
                }
            }

            double x = player.lastTickPosX + (player.getPosX() - player.lastTickPosX) * partialTicks - projectedX;
            double z = player.lastTickPosZ + (player.getPosZ() - player.lastTickPosZ) * partialTicks - projectedZ;
            double rotY = -(z * cos - x * sin);
            double rotX = -(x * cos + z * sin);

            float angleRad = (float) Math.atan2(rotY, rotX);
            float angle = angleRad * RAD_TO_DEG;

            float targetSize = animationStep;
            if (hasLowHealth) {
                targetSize = animationStep + LOW_HEALTH_EXTRA_RADIUS;
            }

            Float animatedSize = animatedSizes.get(originalName);
            float currentSize;
            if (animatedSize == null) {
                currentSize = targetSize;
            } else {
                currentSize = MathUtil.fast(animatedSize, targetSize, 8);
            }
            animatedSizes.put(originalName, currentSize);

            double x2 = currentSize * MathHelper.cos(angleRad) + centerX;
            double y2 = currentSize * MathHelper.sin(angleRad) + centerY;

            boolean renderHealthLabel = hasLowHealth && isFriend;
            boolean renderText = renderDistance || renderHealthLabel;

            GL11.glPushMatrix();
            GL11.glTranslated(x2, y2, 0);
            GL11.glRotatef(angle + ARROW_ROTATION_OFFSET, 0, 0, 1f);

            int color = getPlayerHealthColor(isFriend, hasLowHealth, healthPercent);

            float arrowAlpha = baseOpacity;
            if (renderHealthLabel) {
                float pulse = (float)(Math.sin(now * 0.005) * 0.3 + 0.7);
                arrowAlpha = baseOpacity * pulse;
            }

            int alphaColor = ColorUtils.setAlpha(color, (int)(arrowAlpha * 255));

            RenderUtility.drawImageAlpha(arrow, -8.0F, -9.0F, 16, 16, alphaColor);

            if (renderText) {
                GL11.glRotatef(-(angle + ARROW_ROTATION_OFFSET), 0, 0, 1f);
                int textColor = ColorUtils.setAlpha(0xFFFFFF, (int)(255 * arrowAlpha));

                if (renderDistance) {
                    String distanceText = Math.round(mc.player.getDistance(player)) + "m";
                    float textWidth = ClientFonts.small_pixel[12].getWidth(distanceText);
                    ClientFonts.small_pixel[12].drawString(matrixStack, distanceText, -textWidth / 2, 10, textColor);
                }

                if (renderHealthLabel) {
                    String displayName = originalName;
                    Harmony harmony = Harmony.getInstance();
                    if (harmony != null && harmony.getModuleManager().getStreamerMode().isState()) {
                        StreamerMode streamerMode = harmony.getModuleManager().getStreamerMode();
                    if (streamerMode.options.getValueByName("Ник").get()) {
                            displayName = "Friend";
                        }
                    }

                    String healthText = displayName + " HP: " + String.format("%.1f", health);
                    float textWidth = ClientFonts.small_pixel[14].getWidth(healthText);

                    ClientFonts.small_pixel[14].drawString(matrixStack, healthText, -textWidth / 2, -22, textColor);
                }

            }

            GL11.glPopMatrix();
        }

        animatedRotation += 1f;
        if (animatedRotation > 360) animatedRotation = 0;
    }

    private int getPlayerHealthColor(boolean isFriend, boolean hasLowHealth, float healthPercent) {
        if (isFriend && hasLowHealth) {
            if (healthPercent <= 0.25f) {
                return ColorUtils.rgba(255, 0, 0, 255);
            } else if (healthPercent <= 0.5f) {
                return ColorUtils.rgba(255, 165, 0, 255);
            } else if (healthPercent <= 0.75f) {
                return ColorUtils.rgba(255, 255, 0, 255);
            }
        }

        if (isFriend) {
            return ColorUtils.rgba(80, 250, 80, 255);
        } else {
            return Theme.MainColor((int)animatedRotation);
        }
    }

    private boolean isNaked(AbstractClientPlayerEntity player) {
        return player.getItemStackFromSlot(EquipmentSlotType.HEAD).isEmpty()
                && player.getItemStackFromSlot(EquipmentSlotType.CHEST).isEmpty()
                && player.getItemStackFromSlot(EquipmentSlotType.LEGS).isEmpty()
                && player.getItemStackFromSlot(EquipmentSlotType.FEET).isEmpty();
    }
}
