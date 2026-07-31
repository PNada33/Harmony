package net.fabricmc.fabric.api.client.rendering.v1;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public final class WorldRenderEvents {
    public static final Event<Last> LAST = EventFactory.createArrayBacked(Last.class, listeners -> context -> {
        for (Last listener : listeners) {
            listener.onLast(context);
        }
    });

    private WorldRenderEvents() {
    }

    public interface Last {
        void onLast(WorldRenderContext context);
    }
}
