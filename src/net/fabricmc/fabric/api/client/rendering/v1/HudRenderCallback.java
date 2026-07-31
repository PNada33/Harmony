package net.fabricmc.fabric.api.client.rendering.v1;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface HudRenderCallback {
    Event<HudRenderCallback> EVENT = EventFactory.createArrayBacked(HudRenderCallback.class, listeners -> (matrixStack, tickDelta) -> {
        for (HudRenderCallback listener : listeners) {
            listener.onHudRender(matrixStack, tickDelta);
        }
    });

    void onHudRender(MatrixStack matrixStack, float tickDelta);
}
