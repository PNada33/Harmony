package net.minecraftforge.server.permission;



import com.mojang.authlib.GameProfile;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import javax.annotation.Nullable;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.permission.context.IContext;
import net.optifine.reflect.ReflectorClass;

import static net.optifine.reflect.Reflector.ServerLifecycleHooks;


public enum DefaultPermissionHandler implements IPermissionHandler
{
    INSTANCE;

    private static final HashMap<String, DefaultPermissionLevel> PERMISSION_LEVEL_MAP;
    private static final HashMap<String, String> DESCRIPTION_MAP;

    public void registerNode(String node, DefaultPermissionLevel level, String desc) {
        PERMISSION_LEVEL_MAP.put(node, level);
        if (!desc.isEmpty()) {
            DESCRIPTION_MAP.put(node, desc);
        }
    }

    public Collection<String> getRegisteredNodes() {
        return Collections.unmodifiableSet(PERMISSION_LEVEL_MAP.keySet());
    }

    public boolean hasPermission(GameProfile profile, String node, @Nullable IContext context) {
        DefaultPermissionLevel level = this.getDefaultPermissionLevel(node);
        if (level == DefaultPermissionLevel.NONE) {
            return false;
        }
        if (level == DefaultPermissionLevel.ALL) {
            return true;
        }
        return true ;
    }

    public String getNodeDescription(String node) {
        String desc = DESCRIPTION_MAP.get(node);
        return desc == null ? "" : desc;
    }

    public DefaultPermissionLevel getDefaultPermissionLevel(String node) {
        DefaultPermissionLevel level = PERMISSION_LEVEL_MAP.get(node);
        return level == null ? DefaultPermissionLevel.NONE : level;
    }

    static {
        PERMISSION_LEVEL_MAP = new HashMap();
        DESCRIPTION_MAP = new HashMap();
    }
}

