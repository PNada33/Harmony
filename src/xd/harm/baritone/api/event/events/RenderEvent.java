

package xd.harm.baritone.api.event.events;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.util.math.vector.Matrix4f;

/**
 * @author Brady
 * @since 8/5/2018
 */
public final class RenderEvent {

    /**
     * The current render partial ticks
     */
    private final float partialTicks;

    private final Matrix4f projectionMatrix;
    private final MatrixStack modelViewStack;

    public RenderEvent(float partialTicks, MatrixStack modelViewStack, Matrix4f projectionMatrix) {
        this.partialTicks = partialTicks;
        this.modelViewStack = modelViewStack;
        this.projectionMatrix = projectionMatrix;
    }

    /**
     * @return The current render partial ticks
     */
    public final float getPartialTicks() {
        return this.partialTicks;
    }

    public MatrixStack getModelViewStack() {
        return this.modelViewStack;
    }

    public Matrix4f getProjectionMatrix() {
        return this.projectionMatrix;
    }
}
