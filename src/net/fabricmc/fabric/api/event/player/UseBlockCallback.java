package net.fabricmc.fabric.api.event.player;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.World;

public interface UseBlockCallback {
    Event<UseBlockCallback> EVENT = EventFactory.createArrayBacked(UseBlockCallback.class, listeners -> (player, world, hand, hitResult) -> {
        for (UseBlockCallback listener : listeners) {
            ActionResultType result = listener.interact(player, world, hand, hitResult);

            if (result != ActionResultType.PASS) {
                return result;
            }
        }

        return ActionResultType.PASS;
    });

    ActionResultType interact(PlayerEntity player, World world, Hand hand, BlockRayTraceResult hitResult);
}
