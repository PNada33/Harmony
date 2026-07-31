package xd.harm.utils.render;

import net.minecraft.inventory.container.Slot;
import net.minecraft.util.math.MathHelper;
import xd.harm.modules.impl.misc.Visuality;

public final class VisualityGuiAnimationHelper {
    private static final float ITEM_ANIMATION_SPEED = 0.5F;
    private static final float ITEM_ANIMATION_SCALE = 1.4F;

    private VisualityGuiAnimationHelper() {
    }

    public static boolean isTinyItemAnimationsEnabled() {
        return Visuality.isImprovedAnimationsEnabled();
    }

    public static float advanceProgress(float progress, float deltaTicks) {
        return MathHelper.clamp(progress + deltaTicks * ITEM_ANIMATION_SPEED, 0.0F, 1.0F);
    }

    public static float decayProgress(float progress, float deltaTicks) {
        return Math.max(0.0F, progress - deltaTicks * ITEM_ANIMATION_SPEED);
    }

    public static float getScale(float progress) {
        float clampedProgress = MathHelper.clamp(progress, 0.0F, 1.0F);
        return 1.0F + (ITEM_ANIMATION_SCALE - 1.0F) * (1.0F - (float) Math.pow(1.0F - clampedProgress, 5.0F));
    }

    public static float getSlotScale(Slot slot, float deltaTicks) {
        float progress = slot.getVisualityAnimationProgress();
        slot.setVisualityAnimationProgress(decayProgress(progress, deltaTicks));
        return getScale(progress);
    }
}
