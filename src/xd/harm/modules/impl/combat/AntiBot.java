package xd.harm.modules.impl.combat;

import com.google.common.eventbus.Subscribe;
import com.mojang.authlib.GameProfile;
import xd.harm.events.network.EventPacket;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import io.netty.util.internal.ConcurrentSet;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.play.server.SPlayerListItemPacket;

import java.util.*;

@ModuleRegister(name = "AntiBot", category = Category.Combat, desc = "Убирает бота который летает сзади вас.")
public class AntiBot extends Module {

    private final Set<UUID> susPlayers = new ConcurrentSet<>();
    private static final Map<UUID, Boolean> botsMap = new HashMap<>();
    public static List<Entity> botList = new ArrayList<>();

    @Subscribe
    private void onUpdate(EventUpdate e) {
        for (UUID susPlayer : susPlayers) {
            PlayerEntity entity = mc.world.getPlayerByUuid(susPlayer);

            if (entity != null) {
                Iterator<ItemStack> armor = entity.getArmorInventoryList().iterator();

                int count = 0;

                while (armor.hasNext()) {
                    ItemStack current = armor.next();

                    if (!current.isEmpty()) {
                        count++;
                    }
                }

                boolean isFullArmor = count == 4;

                count = 0;

                for (NetworkPlayerInfo networkPlayerInfo : mc.player.connection.getPlayerInfoMap()) {
                    GameProfile profile = networkPlayerInfo.getGameProfile();

                    if (entity.getGameProfile().getName().equals(profile.getName())) {
                        count++;
                    }
                }

                boolean isBotDetected = isFullArmor || !entity.getUniqueID().equals(PlayerEntity.getOfflineUUID(entity.getGameProfile().getName()));

                botsMap.put(susPlayer, isBotDetected);
            }

            susPlayers.remove(susPlayer);
        }

        for (PlayerEntity entity : mc.world.getPlayers()) {
            if (mc.player != entity && entity.inventory != null && entity.inventory.armorInventory != null && entity.inventory.armorInventory.size() >= 4
                    && entity.inventory.armorInventory.get(0) != null && entity.inventory.armorInventory.get(0).getItem() != Items.AIR
                    && entity.inventory.armorInventory.get(1) != null && entity.inventory.armorInventory.get(1).getItem() != Items.AIR
                    && entity.inventory.armorInventory.get(2) != null && entity.inventory.armorInventory.get(2).getItem() != Items.AIR
                    && entity.inventory.armorInventory.get(3) != null && entity.inventory.armorInventory.get(3).getItem() != Items.AIR
                    && entity.inventory.armorInventory.get(0).isEnchantable()
                    && entity.inventory.armorInventory.get(1).isEnchantable()
                    && entity.inventory.armorInventory.get(2).isEnchantable()
                    && entity.inventory.armorInventory.get(3).isEnchantable()
                    && entity.getHeldItemOffhand() != null && entity.getHeldItemOffhand().getItem() == Items.AIR
                    && (entity.inventory.armorInventory.get(0).getItem() == Items.LEATHER_BOOTS || entity.inventory.armorInventory.get(1).getItem() == Items.LEATHER_LEGGINGS || entity.inventory.armorInventory.get(2).getItem() == Items.LEATHER_CHESTPLATE || entity.inventory.armorInventory.get(3).getItem() == Items.LEATHER_HELMET || entity.inventory.armorInventory.get(0).getItem() == Items.IRON_BOOTS || entity.inventory.armorInventory.get(1).getItem() == Items.IRON_LEGGINGS || entity.inventory.armorInventory.get(2).getItem() == Items.IRON_CHESTPLATE || entity.inventory.armorInventory.get(3).getItem() == Items.IRON_HELMET || entity.inventory.armorInventory.get(0).getItem() == Items.DIAMOND_BOOTS || entity.inventory.armorInventory.get(1).getItem() == Items.DIAMOND_LEGGINGS || entity.inventory.armorInventory.get(2).getItem() == Items.DIAMOND_CHESTPLATE || entity.inventory.armorInventory.get(3).getItem() == Items.DIAMOND_HELMET)
                    && (entity.getHeldItemMainhand() == null || entity.getHeldItemMainhand().getItem() == Items.AIR || entity.getHeldItemMainhand().getItem() == Items.DIAMOND_SWORD || entity.getHeldItemMainhand().getItem() == Items.IRON_SWORD || entity.getHeldItemMainhand().getItem() == Items.FISHING_ROD || entity.getHeldItemMainhand().getItem() == Items.IRON_AXE || entity.getHeldItemMainhand().getItem() == Items.DIAMOND_AXE || entity.getHeldItemMainhand().getItem() == Items.IRON_PICKAXE || entity.getHeldItemMainhand().getItem() == Items.DIAMOND_PICKAXE || entity.getHeldItemMainhand().getItem() == Items.TORCH)
                    && entity.getHeldItemOffhand() != null && entity.getHeldItemOffhand().getItem() == Items.AIR
                    && !entity.inventory.armorInventory.get(0).isDamaged()
                    && !entity.inventory.armorInventory.get(1).isDamaged()
                    && !entity.inventory.armorInventory.get(2).isDamaged()
                    && !entity.inventory.armorInventory.get(3).isDamaged()
                    && entity.getFoodStats() != null && entity.getFoodStats().getFoodLevel() == 20) {

                if (!botList.contains(entity)) {
                    botList.add(entity);
                }
            } else {
                botList.remove(entity);
            }
        }

        if (mc.player.ticksExisted % 100 == 0) {
            botsMap.keySet().removeIf(uuid -> mc.world.getPlayerByUuid(uuid) == null);
        }
    }

    @Subscribe
    private void onPacket(EventPacket e) {
        if (e.getPacket() instanceof SPlayerListItemPacket p) {
            if (p.getAction() == SPlayerListItemPacket.Action.ADD_PLAYER) {
                for (SPlayerListItemPacket.AddPlayerData entry : p.getEntries()) {
                    GameProfile profile = entry.getProfile();

                    if (botsMap.containsKey(profile.getId()) || susPlayers.contains(profile.getId())) {
                        continue;
                    }

                    boolean isInvalid = profile.getProperties().isEmpty() && entry.getPing() != 0;

                    if (isInvalid) {
                        susPlayers.add(profile.getId());
                    }
                }
            }
        }
    }

    public static boolean isBot(Entity entity) {
        return entity instanceof PlayerEntity && (botsMap.getOrDefault(entity.getUniqueID(), false) || botList.contains(entity));
    }

    public static boolean checkBot(LivingEntity entity) {
        return entity instanceof PlayerEntity && botList.contains(entity);
    }

    public static boolean isBotU(Entity entity) {
        if (!entity.getUniqueID().equals(PlayerEntity.getOfflineUUID(entity.getName().getString()))) {
            return entity.isInvisible();
        }
        return false;
    }

    @Override
    public boolean onDisable() {
        super.onDisable();
        botsMap.clear();
        botList.clear();
        return false;
    }
}
