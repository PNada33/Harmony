package net.fabricmc.fabric.api.client.event.lifecycle.v1;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.Minecraft;

public final class ClientLifecycleEvents {
    public static final Event<ClientStopping> CLIENT_STOPPING = EventFactory.createArrayBacked(ClientStopping.class, listeners -> client -> {
        for (ClientStopping listener : listeners) {
            listener.onClientStopping(client);
        }
    });

    private ClientLifecycleEvents() {
    }

    public interface ClientStopping {
        void onClientStopping(Minecraft client);
    }
}
