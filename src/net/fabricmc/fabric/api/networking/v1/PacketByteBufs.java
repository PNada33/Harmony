package net.fabricmc.fabric.api.networking.v1;

import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketBuffer;

public final class PacketByteBufs {
    private PacketByteBufs() {
    }

    public static PacketBuffer create() {
        return new PacketBuffer(Unpooled.buffer());
    }
}
