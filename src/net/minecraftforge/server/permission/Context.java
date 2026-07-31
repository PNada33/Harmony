package net.minecraftforge.server.permission;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import net.minecraftforge.server.permission.context.ContextKey;
import net.minecraftforge.server.permission.context.IContext;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class Context
        implements IContext {
    private Map<ContextKey<?>, Object> map;

    @Nullable
    public World getWorld() {
        return null;
    }

    @Nullable
    public PlayerEntity getPlayer() {
        return null;
    }

    @Nullable
    public <T> T get(ContextKey<T> key) {
        return (T)(this.map == null || this.map.isEmpty() ? null : this.map.get(key));
    }

    public boolean has(ContextKey<?> key) {
        return this.covers(key) || this.map != null && !this.map.isEmpty() && this.map.containsKey(key);
    }

    public <T> Context set(ContextKey<T> key, @Nullable T obj) {
        if (this.covers(key)) {
            return this;
        }
        if (this.map == null) {
            this.map = new HashMap();
        }
        this.map.put(key, obj);
        return this;
    }

    protected boolean covers(ContextKey<?> key) {
        return false;
    }
}

