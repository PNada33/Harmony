package net.minecraft.client.network.play;

import net.minecraft.world.GameType;

public final class NetworkPlayerInfoAccessor {
    private NetworkPlayerInfoAccessor() {
    }

    public static void setGameType(NetworkPlayerInfo info, GameType gameType) {
        info.setGameType(gameType);
    }

    public static void setResponseTime(NetworkPlayerInfo info, int responseTime) {
        info.setResponseTime(responseTime);
    }
}
