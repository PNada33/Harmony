package net.minecraftforge.server.permission.context;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public interface IContext {
    @Nullable
    public World getWorld();

    @Nullable
    public PlayerEntity getPlayer();

    @Nullable
    public <T> T get(ContextKey<T> var1);

    public boolean has(ContextKey<?> var1);
}
