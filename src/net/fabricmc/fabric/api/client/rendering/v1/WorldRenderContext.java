package net.fabricmc.fabric.api.client.rendering.v1;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.vector.Matrix4f;

public interface WorldRenderContext {
    ClientWorld world();

    ActiveRenderInfo camera();

    MatrixStack matrixStack();

    float tickDelta();

    Matrix4f projectionMatrix();
}
