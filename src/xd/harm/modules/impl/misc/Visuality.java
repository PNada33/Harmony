package xd.harm.modules.impl.misc;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.play.server.SChatPacket;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import xd.harm.events.network.EventPacket;
import xd.harm.events.render.EventDisplay;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.CategorySetting;
import xd.harm.utils.math.MathUtil;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.CategorySetting;
import xd.harm.utils.math.MathUtil;
import xd.harm.utils.projections.ProjectionUtil;
import xd.harm.utils.render.ChunkAnimatorHandler;
import xd.harm.utils.render.color.ColorUtils;
import xd.harm.utils.render.rect.RenderUtility;
import xd.harm.utils.text.font.ClientFonts;

@ModuleRegister(
        name = "Visuality",
        category = Category.Misc,
        desc = "\u0412\u0438\u0437\u0443\u0430\u043b\u044c\u043d\u044b\u0435 \u0443\u043b\u0443\u0447\u0448\u0435\u043d\u0438\u044f"
)
public class Visuality extends Module {

    public static Visuality get;

    private static final float SWITCH_ANIM_SPEED = 0.14F;
    private static final float DISTANCE_SPEED = 0.07F;
    private static final float ROTATION_SMOOTH = 0.11F;
    private static final float CAMERA_DISTANCE = 4.1F;
    private static final float SNEAK_OFFSET = 0.5F;
    private static final float JUMP_MULTIPLIER = 2.0F;
    private static final float ANIM_SPEED = 0.09F;
    private static final float MAX_DELTA_SECONDS = 0.05F;

    public final BooleanSetting smoothChat =
            new BooleanSetting("\u041f\u043b\u0430\u0432\u043d\u044b\u0439 \u0447\u0430\u0442", true);
    public final BooleanSetting betterTab =
            new BooleanSetting("\u0423\u043b\u0443\u0447\u0448\u0435\u043d\u043d\u044b\u0439 \u0442\u0430\u0431", true);
    public final BooleanSetting betterChat =
            new BooleanSetting("\u0423\u043b\u0443\u0447\u0448\u0435\u043d\u043d\u044b\u0439 \u0447\u0430\u0442", true);
    public final BooleanSetting smoothCamera =
            new BooleanSetting("\u041f\u043b\u0430\u0432\u043d\u0430\u044f \u043a\u0430\u043c\u0435\u0440\u0430", true);
    public final BooleanSetting improvedAnimations =
            new BooleanSetting("\u0423\u043b\u0443\u0447\u0448\u0435\u043d\u043d\u044b\u0435 \u0430\u043d\u0438\u043c\u0430\u0446\u0438\u0438", true);
    public final BooleanSetting chatBubbles =
            new BooleanSetting("\u0421\u043e\u043e\u0431\u0449\u0435\u043d\u0438\u0435 \u043d\u0430\u0434 \u0438\u0433\u0440\u043e\u043a\u043e\u043c", true);
    public final BooleanSetting animatedChunks =
            new BooleanSetting("\u0410\u043d\u0438\u043c\u0438\u0440\u043e\u0432\u0430\u043d\u043d\u044b\u0435 \u0447\u0430\u043d\u043a\u0438", true);
    public final BooleanSetting fancyBlocks =
            new BooleanSetting("3\u0434 \u043f\u0430\u0440\u0442\u0438\u043a\u043b\u044b \u043b\u043e\u043c\u0430\u043d\u0438\u044f", true);
    public final CategorySetting mainAnimationsCategory =
            new CategorySetting("\u041e\u0441\u043d\u043e\u0432\u043d\u043e\u0435").setVisible(this::isImprovedAnimationsSectionVisible);
    public final BooleanSetting eatDrinkAnimation =
            new BooleanSetting("\u0415\u0434\u0430 \u0438 \u0437\u0435\u043b\u044c\u044f", true).setVisible(this::isImprovedAnimationsSectionVisible);
    public final BooleanSetting elytraAnimation =
            new BooleanSetting("\u0410\u043d\u0438\u043c\u0430\u0446\u0438\u044f \u044d\u043b\u0438\u0442\u0440\u044b", true).setVisible(this::isImprovedAnimationsSectionVisible);
    public final CategorySetting movementAnimationsCategory =
            new CategorySetting("\u0414\u0432\u0438\u0436\u0435\u043d\u0438\u0435").setVisible(this::isImprovedAnimationsSectionVisible);
    public final BooleanSetting walkAnimation =
            new BooleanSetting("\u0410\u043d\u0438\u043c\u0430\u0446\u0438\u044f \u0445\u043e\u0434\u044c\u0431\u044b", true).setVisible(this::isImprovedAnimationsSectionVisible);
    public final BooleanSetting sprintAnimation =
            new BooleanSetting("\u0410\u043d\u0438\u043c\u0430\u0446\u0438\u044f \u0441\u043f\u0440\u0438\u043d\u0442\u0430", true).setVisible(this::isImprovedAnimationsSectionVisible);
    public final BooleanSetting crouchAnimation =
            new BooleanSetting("\u0410\u043d\u0438\u043c\u0430\u0446\u0438\u044f \u043f\u0440\u0438\u0441\u0435\u0434\u0430", true).setVisible(this::isImprovedAnimationsSectionVisible);
    public final BooleanSetting swimAnimationOverhaul =
            new BooleanSetting("\u0410\u043d\u0438\u043c\u0430\u0446\u0438\u044f \u043f\u043b\u0430\u0432\u0430\u043d\u0438\u044f", true).setVisible(this::isImprovedAnimationsSectionVisible);
    public final BooleanSetting landingAnimation =
            new BooleanSetting("\u0410\u043d\u0438\u043c\u0430\u0446\u0438\u044f \u043f\u0440\u0438\u0437\u0435\u043c\u043b\u0435\u043d\u0438\u044f", true).setVisible(this::isImprovedAnimationsSectionVisible);
    public final BooleanSetting ladderAnimation =
            new BooleanSetting("\u0410\u043d\u0438\u043c\u0430\u0446\u0438\u044f \u043b\u0435\u0441\u0442\u043d\u0438\u0446\u044b", true).setVisible(this::isImprovedAnimationsSectionVisible);
    public final BooleanSetting fallingAnimation =
            new BooleanSetting("\u0410\u043d\u0438\u043c\u0430\u0446\u0438\u044f \u043f\u0430\u0434\u0435\u043d\u0438\u044f", true).setVisible(this::isImprovedAnimationsSectionVisible);
    public final CategorySetting vehicleAnimationsCategory =
            new CategorySetting("\u0422\u0440\u0430\u043d\u0441\u043f\u043e\u0440\u0442").setVisible(this::isImprovedAnimationsSectionVisible);
    public final BooleanSetting boatAnimation =
            new BooleanSetting("\u0410\u043d\u0438\u043c\u0430\u0446\u0438\u044f \u043b\u043e\u0434\u043a\u0438", true).setVisible(this::isImprovedAnimationsSectionVisible);
    public final BooleanSetting horseAnimation =
            new BooleanSetting("\u0410\u043d\u0438\u043c\u0430\u0446\u0438\u044f \u043b\u043e\u0448\u0430\u0434\u0438", true).setVisible(this::isImprovedAnimationsSectionVisible);
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
    private final Map<Entity, ChatMessage> chatBubblesMap = new ConcurrentHashMap<>();

    public Visuality() {
        get = this;
        fancyBlocks.setConfigKey("3\u0434 \u043f\u0430\u0440\u0442\u0438\u043a\u043b\u044b \u043b\u043e\u043c\u0430\u043d\u0438\u044f");
        addSettings(
                betterTab,
                betterChat,
                smoothChat,
                smoothCamera,
                animatedChunks,
                fancyBlocks,
                chatBubbles,
                improvedAnimations,
                mainAnimationsCategory,
                eatDrinkAnimation,
                elytraAnimation,
                movementAnimationsCategory,
                walkAnimation,
                sprintAnimation,
                crouchAnimation,
                swimAnimationOverhaul,
                landingAnimation,
                ladderAnimation,
                fallingAnimation,
                vehicleAnimationsCategory,
                boatAnimation,
                horseAnimation
        );
    }

    @Subscribe
    public void onPacket(EventPacket event) {
        if (!chatBubbles.get() || !isState()) {
            return;
        }

        if (event.isReceive() && event.getPacket() instanceof SChatPacket) {
            SChatPacket packet = (SChatPacket) event.getPacket();
            if (packet.getType() != ChatType.CHAT || mc.world == null) {
                return;
            }

            UUID senderId = packet.func_240810_e_();
            if (senderId == null) {
                return;
            }

            PlayerEntity sender = mc.world.getPlayerByUuid(senderId);
            if (sender == null) {
                return;
            }

            String message = extractChatMessage(packet, sender);
            if (!message.isEmpty()) {
                chatBubblesMap.put(sender, new ChatMessage(message));
            }
        }
    }

    @Subscribe
    public void onDisplay(EventDisplay event) {
        if (!chatBubbles.get() || chatBubblesMap.isEmpty()) {
            return;
        }
        if (event.getType() != EventDisplay.Type.HIGH) {
            return;
        }

        long now = System.currentTimeMillis();
        for (Map.Entry<Entity, ChatMessage> entry : chatBubblesMap.entrySet()) {
            Entity entity = entry.getKey();
            ChatMessage message = entry.getValue();

            long passed = now - message.time;
            if (passed > 2000L || !entity.isAlive() || entity.getDistance(mc.player) > 64.0F) {
                chatBubblesMap.remove(entity);
                continue;
            }
            if (entity == mc.player && mc.gameSettings.getPointOfView().func_243192_a()) {
                continue;
            }

            double x = MathUtil.interpolate(entity.getPosX(), entity.lastTickPosX, event.getPartialTicks());
            double y = MathUtil.interpolate(entity.getPosY(), entity.lastTickPosY, event.getPartialTicks()) + entity.getHeight() + 0.6;
            double z = MathUtil.interpolate(entity.getPosZ(), entity.lastTickPosZ, event.getPartialTicks());
            if (!isChatBubbleVisible(entity, x, y, z)) {
                continue;
            }
            Vector2f projected = ProjectionUtil.project(x, y, z);
            if (projected == null) {
                continue;
            }
            if (projected.x < -100.0F || projected.x > mc.getMainWindow().getScaledWidth() + 100.0F) {
                continue;
            }

            float distance = mc.player.getDistance(entity);
            float scale = Math.max(0.6F, 1.0F - distance / 40.0F);
            if (passed < 200L) {
                scale *= passed / 200.0F;
            } else if (passed > 1800L) {
                scale *= (2000L - passed) / 200.0F;
            }

            float textWidth = ClientFonts.msSemiBold[16].getWidth(message.text);
            float textHeight = ClientFonts.msSemiBold[16].getFontHeight();
            float padding = 4.0F;
            float boxWidth = textWidth + padding * 2.0F;
            float boxHeight = textHeight + padding * 2.0F;
            int background = ColorUtils.rgba(20, 20, 20, 180);

            RenderSystem.pushMatrix();
            RenderSystem.translatef(projected.x, projected.y, 0.0F);
            RenderSystem.scalef(scale, scale, 1.0F);
            RenderUtility.drawShadow(-boxWidth / 2.0F, -boxHeight, boxWidth, boxHeight, 10, background);
            RenderUtility.drawRoundedRect(-boxWidth / 2.0F, -boxHeight, boxWidth, boxHeight, 4.0F, background);
            RenderSystem.popMatrix();

            MatrixStack matrixStack = event.getMatrixStack();
            matrixStack.push();
            matrixStack.translate(projected.x, projected.y, 0.0);
            matrixStack.scale(scale, scale, 1.0F);

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableTexture();
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            ClientFonts.msSemiBold[16].drawCenteredString(matrixStack, message.text, 0.0F, -boxHeight + padding + 1.0F, -1);
            matrixStack.pop();
        }
    }

    private String extractChatMessage(SChatPacket packet, PlayerEntity sender) {
        ITextComponent component = packet.getChatComponent();
        if (component instanceof TranslationTextComponent) {
            TranslationTextComponent translation = (TranslationTextComponent) component;
            Object[] args = translation.getFormatArgs();
            if (args.length >= 2) {
                String directMessage = stringifyChatArg(args[1]);
                if (!directMessage.isEmpty()) {
                    return directMessage;
                }
            }
        }

        String plainText = TextFormatting.getTextWithoutFormattingCodes(component.getString());
        if (plainText == null) {
            return "";
        }

        return stripSenderPrefix(plainText, sender.getGameProfile().getName());
    }

    private String stringifyChatArg(Object arg) {
        if (arg == null) {
            return "";
        }
        if (arg instanceof ITextComponent) {
            String text = TextFormatting.getTextWithoutFormattingCodes(((ITextComponent) arg).getString());
            return text == null ? "" : text.trim();
        }
        String text = TextFormatting.getTextWithoutFormattingCodes(String.valueOf(arg));
        return text == null ? "" : text.trim();
    }

    private String stripSenderPrefix(String text, String playerName) {
        String cleaned = text.trim();
        String[] directPrefixes = {
                "<" + playerName + ">",
                playerName + ":",
                playerName + " >",
                playerName + " >>",
                playerName + " »",
                playerName + " -"
        };

        for (String prefix : directPrefixes) {
            if (cleaned.startsWith(prefix)) {
                return cleaned.substring(prefix.length()).trim();
            }
        }

        int nameIndex = cleaned.indexOf(playerName);
        if (nameIndex >= 0) {
            int searchStart = nameIndex + playerName.length();
            int bestCut = -1;
            for (char delimiter : new char[] {'>', ':', '»', '-'} ) {
                int delimiterIndex = cleaned.indexOf(delimiter, searchStart);
                if (delimiterIndex != -1 && (bestCut == -1 || delimiterIndex < bestCut)) {
                    bestCut = delimiterIndex;
                }
            }
            if (bestCut != -1 && bestCut + 1 < cleaned.length()) {
                return cleaned.substring(bestCut + 1).trim();
            }
        }

        return cleaned;
    }

    private boolean isChatBubbleVisible(Entity entity, double x, double y, double z) {
        if (mc.world == null || mc.player == null || mc.getRenderManager().info == null) {
            return true;
        }

        Vector3d cameraPos = mc.getRenderManager().info.getProjectedView();
        Vector3d targetPos = new Vector3d(x, y, z);
        BlockRayTraceResult result = mc.world.rayTraceBlocks(new RayTraceContext(
                cameraPos,
                targetPos,
                RayTraceContext.BlockMode.COLLIDER,
                RayTraceContext.FluidMode.NONE,
                entity
        ));

        if (result.getType() != RayTraceResult.Type.BLOCK) {
            return true;
        }

        double distanceToBlock = cameraPos.distanceTo(result.getHitVec());
        double distanceToBubble = cameraPos.distanceTo(targetPos);
        return distanceToBlock >= distanceToBubble - 0.05D;
    }

    public boolean isSmoothCameraActive() {
        return isState() && smoothCamera.get();
    }

    public boolean isImprovedAnimationsActive() {
        return isState() && improvedAnimations.get();
    }

    private boolean isImprovedAnimationsSectionVisible() {
        return improvedAnimations.get();
    }

    public boolean isEatDrinkAnimationActive() {
        return isImprovedAnimationsActive() && eatDrinkAnimation.get();
    }

    public boolean isWalkAnimationActive() {
        return isImprovedAnimationsActive() && walkAnimation.get();
    }

    public boolean isSprintAnimationActive() {
        return isImprovedAnimationsActive() && sprintAnimation.get();
    }

    public boolean isCrouchAnimationActive() {
        return isImprovedAnimationsActive() && crouchAnimation.get();
    }

    public boolean isSwimAnimationOverhaulActive() {
        return isImprovedAnimationsActive() && swimAnimationOverhaul.get();
    }

    public boolean isLandingAnimationActive() {
        return isImprovedAnimationsActive() && landingAnimation.get();
    }

    public boolean isLadderAnimationActive() {
        return isImprovedAnimationsActive() && ladderAnimation.get();
    }

    public boolean isBoatAnimationActive() {
        return isImprovedAnimationsActive() && boatAnimation.get();
    }

    public boolean isHorseAnimationActive() {
        return isImprovedAnimationsActive() && horseAnimation.get();
    }

    public boolean isElytraAnimationActive() {
        return isImprovedAnimationsActive() && elytraAnimation.get();
    }

    public boolean isFallingAnimationActive() {
        return isImprovedAnimationsActive() && fallingAnimation.get();
    }

    public boolean isFancyBlocksActive() {
        return isState() && fancyBlocks.get();
    }

    public boolean isAnimatedChunksActive() {
        return isState() && animatedChunks.get();
    }

    public static boolean isImprovedAnimationsEnabled() {
        return get != null && get.isImprovedAnimationsActive();
    }

    public static boolean isEatDrinkAnimationEnabled() {
        return get != null && get.isEatDrinkAnimationActive();
    }

    public static boolean isWalkAnimationEnabled() {
        return get != null && get.isWalkAnimationActive();
    }

    public static boolean isSprintAnimationEnabled() {
        return get != null && get.isSprintAnimationActive();
    }

    public static boolean isCrouchAnimationEnabled() {
        return get != null && get.isCrouchAnimationActive();
    }

    public static boolean isSwimAnimationOverhaulEnabled() {
        return get != null && get.isSwimAnimationOverhaulActive();
    }

    public static boolean isLandingAnimationEnabled() {
        return get != null && get.isLandingAnimationActive();
    }

    public static boolean isLadderAnimationEnabled() {
        return get != null && get.isLadderAnimationActive();
    }

    public static boolean isBoatAnimationEnabled() {
        return get != null && get.isBoatAnimationActive();
    }

    public static boolean isHorseAnimationEnabled() {
        return get != null && get.isHorseAnimationActive();
    }

    public static boolean isElytraAnimationEnabled() {
        return get != null && get.isElytraAnimationActive();
    }

    public static boolean isFallingAnimationEnabled() {
        return get != null && get.isFallingAnimationActive();
    }

    public static boolean isFancyBlocksEnabled() {
        return get != null && get.isFancyBlocksActive();
    }

    public static boolean isAnimatedChunksEnabled() {
        return get != null && get.isAnimatedChunksActive();
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
        float deltaSeconds = lastUpdateTimeNanos == 0L
                ? 1.0F / 60.0F
                : MathHelper.clamp((now - lastUpdateTimeNanos) / 1_000_000_000.0F, 0.0F, MAX_DELTA_SECONDS);
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

        float targetOffset = entity.isSneaking() ? -SNEAK_OFFSET : 0.0F;
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

    @Override
    public boolean onEnable() {
        needsInit = true;
        wasThirdPerson = false;
        switchAnimating = false;
        lastUpdateTimeNanos = 0L;
        ChunkAnimatorHandler.clear();
        chatBubblesMap.clear();
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
        ChunkAnimatorHandler.clear();
        chatBubblesMap.clear();
        return super.onDisable();
    }

    private static class ChatMessage {
        private final String text;
        private final long time;

        private ChatMessage(String text) {
            this.text = text;
            this.time = System.currentTimeMillis();
        }
    }
}
