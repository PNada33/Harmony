package net.minecraftforge.server.permission;

import com.mojang.authlib.GameProfile;
import net.minecraftforge.server.permission.context.IContext;

import javax.annotation.Nullable;
import java.util.Collection;

public interface IPermissionHandler {
    public void registerNode(String var1, DefaultPermissionLevel var2, String var3);

    public Collection<String> getRegisteredNodes();

    public boolean hasPermission(GameProfile var1, String var2, @Nullable IContext var3);

    public String getNodeDescription(String var1);
}

