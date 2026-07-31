package net.minecraftforge.client.settings;

public interface IKeyConflictContext {
    public boolean isActive();

    public boolean conflicts(IKeyConflictContext var1);
}
