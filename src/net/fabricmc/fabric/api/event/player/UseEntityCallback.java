package net.fabricmc.fabric.api.event.player;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.world.World;

public interface UseEntityCallback {
    Event<UseEntityCallback> EVENT = EventFactory.createArrayBacked(UseEntityCallback.class, listeners -> (player, world, hand, entity, hitResult) -> {
        for (UseEntityCallback listener : listeners) {
            ActionResultType result = listener.interact(player, world, hand, entity, hitResult);

            if (result != ActionResultType.PASS) {
                return result;
            }
        }

        return ActionResultType.PASS;
    });

    ActionResultType interact(PlayerEntity player, World world, Hand hand, Entity entity, EntityRayTraceResult hitResult);
}
