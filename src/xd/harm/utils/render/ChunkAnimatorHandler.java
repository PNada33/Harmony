package xd.harm.utils.render;

import com.mojang.blaze3d.platform.GlStateManager;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.util.math.BlockPos;
import xd.harm.modules.impl.misc.Visuality;

public final class ChunkAnimatorHandler {
    private static final long ANIMATION_DURATION_MS = 1000L;
    private static final WeakHashMap<ChunkRenderDispatcher.ChunkRender, AnimationData> ANIMATIONS = new WeakHashMap<>();

    private ChunkAnimatorHandler() {
    }

    public static boolean isActive() {
        return Visuality.isAnimatedChunksEnabled();
    }

    public static void setOrigin(ChunkRenderDispatcher.ChunkRender renderChunk, BlockPos position) {
        if (!isActive()) {
            ANIMATIONS.remove(renderChunk);
            return;
        }

        ANIMATIONS.put(renderChunk, new AnimationData(-1L, position.getY()));
    }

    public static void preRenderChunk(ChunkRenderDispatcher.ChunkRender renderChunk) {
        if (!isActive()) {
            ANIMATIONS.remove(renderChunk);
            return;
        }

        AnimationData animationData = ANIMATIONS.get(renderChunk);

        if (animationData == null) {
            return;
        }

        long startTime = animationData.startTime;

        if (startTime == -1L) {
            startTime = System.currentTimeMillis();
            animationData.startTime = startTime;
        }

        long elapsed = System.currentTimeMillis() - startTime;

        if (elapsed >= ANIMATION_DURATION_MS) {
            ANIMATIONS.remove(renderChunk);
            return;
        }

        double offsetY = -animationData.originY + easeOutSine(elapsed, 0.0D, animationData.originY, (double)ANIMATION_DURATION_MS);
        GlStateManager.translated(0.0D, offsetY, 0.0D);
    }

    public static void clear() {
        ANIMATIONS.clear();
    }

    private static double easeOutSine(long time, double begin, double change, double duration) {
        double progress = Math.min((double)time, duration) / duration;
        return change * Math.sin(progress * Math.PI * 0.5D) + begin;
    }

    private static final class AnimationData {
        private long startTime;
        private final int originY;

        private AnimationData(long startTime, int originY) {
            this.startTime = startTime;
            this.originY = originY;
        }
    }
}
