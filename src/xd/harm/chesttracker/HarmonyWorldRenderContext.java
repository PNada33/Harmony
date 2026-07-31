package xd.harm.chesttracker;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.vector.Matrix4f;

public final class HarmonyWorldRenderContext implements WorldRenderContext {
    private final ClientWorld world;
    private final ActiveRenderInfo camera;
    private final MatrixStack matrixStack;
    private final float tickDelta;
    private final Matrix4f projectionMatrix;

    public HarmonyWorldRenderContext(ClientWorld world, ActiveRenderInfo camera, MatrixStack matrixStack, float tickDelta, Matrix4f projectionMatrix) {
        this.world = world;
        this.camera = camera;
        this.matrixStack = matrixStack;
        this.tickDelta = tickDelta;
        this.projectionMatrix = projectionMatrix;
    }

    @Override
    public ClientWorld world() {
        return this.world;
    }

    @Override
    public ActiveRenderInfo camera() {
        return this.camera;
    }

    @Override
    public MatrixStack matrixStack() {
        return this.matrixStack;
    }

    @Override
    public float tickDelta() {
        return this.tickDelta;
    }

    @Override
    public Matrix4f projectionMatrix() {
        return this.projectionMatrix;
    }
}
