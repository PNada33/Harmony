package net.fabricmc.fabric.api.event.player;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public interface UseItemCallback {
    Event<UseItemCallback> EVENT = EventFactory.createArrayBacked(UseItemCallback.class, listeners -> (player, world, hand) -> {
        for (UseItemCallback listener : listeners) {
            ActionResult<ItemStack> result = listener.interact(player, world, hand);

            if (result.getType() != ActionResultType.PASS) {
                return result;
            }
        }

        return ActionResult.resultPass(player.getHeldItem(hand));
    });

    ActionResult<ItemStack> interact(PlayerEntity player, World world, Hand hand);
}
