package xd.harm.voicechat;

import de.maxhenkel.voicechat.permission.Permission;
import de.maxhenkel.voicechat.permission.PermissionManager;
import de.maxhenkel.voicechat.permission.PermissionType;
import net.minecraft.entity.player.ServerPlayerEntity;

public class HarmonyPermissionManager extends PermissionManager {

    @Override
    public Permission createPermissionInternal(String modId, String node, PermissionType type) {
        return new HarmonyPermission(type);
    }

    private static class HarmonyPermission implements Permission {
        private final PermissionType type;

        private HarmonyPermission(PermissionType type) {
            this.type = type;
        }

        @Override
        public boolean hasPermission(ServerPlayerEntity player) {
            return type != PermissionType.NOONE;
        }

        @Override
        public PermissionType getPermissionType() {
            return type;
        }
    }
}
