package xd.harm.events.combat;

import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockRayTraceResult;
import xd.harm.events.CancelEvent;

public class EventClickBlockRight extends CancelEvent {

    private final ClientPlayerEntity player;
    private final ClientWorld world;
    private final Hand hand;
    private final BlockRayTraceResult result;

    public EventClickBlockRight(ClientPlayerEntity player, ClientWorld world, Hand hand, BlockRayTraceResult result) {
        this.player = player;
        this.world = world;
        this.hand = hand;
        this.result = result;
    }

    public ClientPlayerEntity getPlayer() {
        return this.player;
    }

    public ClientWorld getWorld() {
        return this.world;
    }

    public Hand getHand() {
        return this.hand;
    }

    public BlockRayTraceResult getResult() {
        return this.result;
    }
}
