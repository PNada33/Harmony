package xd.harm.modules.impl.misc;

import com.google.common.eventbus.Subscribe;
import xd.harm.bot.BotSessionManager;
import xd.harm.command.feature.WayCommand;
import xd.harm.events.world.EventEntityLeave;
import xd.harm.events.network.EventPacket;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.CategorySetting;
import xd.harm.modules.settings.impl.SliderSetting;
import xd.harm.utils.text.GradientUtil;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PotionEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.UseAction;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.client.CConfirmTeleportPacket;
import net.minecraft.network.play.client.CPlayerDiggingPacket;
import net.minecraft.network.play.client.CPlayerTryUseItemPacket;
import net.minecraft.network.play.client.CResourcePackStatusPacket;
import net.minecraft.network.play.server.SEntityStatusPacket;
import net.minecraft.network.play.server.SSendResourcePackPacket;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@ModuleRegister(name = "PlayerHelper", category = Category.Player, desc = "Хелпер для игрока")
public class PlayerHelper extends Module {

    private static final long CONSUME_COOLDOWN_MS = 900L;
    private static final long TOTEM_COOLDOWN_MS = 500L;

    public final BooleanSetting portalgodmode = new BooleanSetting("Бессмертие в портале", false);
    public final BooleanSetting srpspoofer = new BooleanSetting("Обход ресурспаков", false);
    public final BooleanSetting speedmine = new BooleanSetting("Быстрое копание", false);
    public final BooleanSetting ultraFast = new BooleanSetting("Мгновенно", false).setVisible(() -> speedmine.get());
    public final BooleanSetting deathPosition = new BooleanSetting("Точка смерти", false);
    public final BooleanSetting noJumpDelay = new BooleanSetting("Прыжки без задержки", false);
    public final BooleanSetting hungryBar = new BooleanSetting("Полоска отхила", false);

    public final CategorySetting speedMineCategory = new CategorySetting("Быстрое копание");
    public final BooleanSetting tracker = new BooleanSetting("Отслеживание", true);
    public final CategorySetting trackerCategory = new CategorySetting("Отслеживание");
    public final BooleanSetting trackSelf = new BooleanSetting("Отслеживать себя", false).setVisible(this::isTrackerVisible);
    public final BooleanSetting trackerPotions = new BooleanSetting("Показывать зелья", true).setVisible(this::isTrackerVisible);
    public final BooleanSetting trackerEffects = new BooleanSetting("Показывать эффекты", true).setVisible(() -> tracker.get() && trackerPotions.get());
    public final BooleanSetting trackerConsume = new BooleanSetting("Кто выпил/съел", true).setVisible(this::isTrackerVisible);
    public final BooleanSetting trackerTotems = new BooleanSetting("Кто потерял тотем", true).setVisible(this::isTrackerVisible);
    public final SliderSetting trackerRadius = new SliderSetting("Радиус трекера", 50.0F, 10.0F, 100.0F, 1.0F).setVisible(this::isTrackerVisible);

    private final Map<Integer, PotionData> trackedPotions = new HashMap<>();
    private final Map<UUID, Boolean> lastSeenTotemEnchanted = new HashMap<>();
    private final Map<UUID, Long> consumeMessageCooldown = new HashMap<>();
    private final Map<UUID, Long> totemMessageCooldown = new HashMap<>();
    private ActiveUseData lastSelfUsePacket;
    private long lastSelfUsePacketTime;

    public PlayerHelper() {
        addSettings(
                portalgodmode,
                srpspoofer,
                deathPosition,
                noJumpDelay,
                hungryBar,
                speedMineCategory,
                speedmine,
                ultraFast,
                trackerCategory,
                tracker,
                trackSelf,
                trackerPotions,
                trackerEffects,
                trackerConsume,
                trackerTotems,
                trackerRadius
        );
    }


    @Subscribe
    public void onPacket(EventPacket event) {
        if (event.getPacket() instanceof CConfirmTeleportPacket && portalgodmode.get()) {
            event.cancel();
        }

        if (event.getPacket() instanceof SSendResourcePackPacket && srpspoofer.get()) {
            mc.player.connection.sendPacket(new CResourcePackStatusPacket(CResourcePackStatusPacket.Action.ACCEPTED));
            mc.player.connection.sendPacket(new CResourcePackStatusPacket(CResourcePackStatusPacket.Action.SUCCESSFULLY_LOADED));
            if (mc.currentScreen != null) {
                mc.player.closeScreen();
            }
            event.cancel();
        }

        if (deathPosition.get() && event.getPacket() instanceof SEntityStatusPacket) {
            SEntityStatusPacket packet = (SEntityStatusPacket) event.getPacket();
            if (packet.getOpCode() == 3) {
                Entity entity = packet.getEntity(mc.world);
                if (entity != null && entity == mc.player) {
                    int x = (int) mc.player.getPosX();
                    int y = (int) mc.player.getPosY();
                    int z = (int) mc.player.getPosZ();

                    WayCommand wayCommand = WayCommand.getInstance();
                    if (wayCommand != null) {
                        wayCommand.addDeathPoint(x, y, z);
                        print("Точка смерти добавлена: " + x + ", " + y + ", " + z);
                    }
                }
            }
        }

        if (!tracker.get() || mc.world == null || mc.player == null) {
            return;
        }

        if (event.isSendPacket()) {
            handleSelfConsumePacket(event.getPacket());
        }

        if (event.isReceivePacket() && event.getPacket() instanceof SEntityStatusPacket) {
            SEntityStatusPacket packet = (SEntityStatusPacket) event.getPacket();
            if (trackerTotems.get()) {
                handleTotemPop(packet);
            }
            if (trackerConsume.get()) {
                handleConsumeFinishPacket(packet);
            }
        }
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        if (speedmine.get()) {
            mc.playerController.blockHitDelay = 0;
            if (!ultraFast.get()) {
                mc.playerController.resetBlockRemoving();
            }
            if (ultraFast.get() && mc.player.isOnGround()) {
                mc.playerController.curBlockDamageMP = 1;
            }
        }

        if (noJumpDelay.get()) {
            mc.player.jumpTicks = 0;
        }

        if (!tracker.get()) {
            return;
        }

        updateTrackerState();

        if (trackerPotions.get()) {
            handlePotionTracker();
        }
    }

    private void updateTrackerState() {
        long now = System.currentTimeMillis();
        Set<UUID> processed = new HashSet<>();

        if (trackSelf.get() && mc.player != null) {
            UUID selfUuid = mc.player.getUniqueID();
            processed.add(selfUuid);
            updateTotemSnapshot(mc.player);
        } else if (mc.player != null) {
            lastSelfUsePacket = null;
        }

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == null) {
                continue;
            }

            UUID uuid = player.getUniqueID();
            if (processed.contains(uuid)) {
                continue;
            }
            processed.add(uuid);

            if (!isTrackedPlayer(player)) {
                continue;
            }

            updateTotemSnapshot(player);
        }

        consumeMessageCooldown.entrySet().removeIf(entry -> now - entry.getValue() > 5000L);
        totemMessageCooldown.entrySet().removeIf(entry -> now - entry.getValue() > 5000L);
        if (lastSelfUsePacket != null && now - lastSelfUsePacketTime > 6000L) {
            lastSelfUsePacket = null;
        }
    }

    private void updateTotemSnapshot(PlayerEntity player) {
        ItemStack main = player.getHeldItemMainhand();
        ItemStack off = player.getHeldItemOffhand();
        boolean mainTotem = main.getItem() == Items.TOTEM_OF_UNDYING;
        boolean offTotem = off.getItem() == Items.TOTEM_OF_UNDYING;
        if (mainTotem || offTotem) {
            boolean enchanted = (mainTotem && main.isEnchanted()) || (offTotem && off.isEnchanted());
            lastSeenTotemEnchanted.put(player.getUniqueID(), enchanted);
        }
    }

    private void handleSelfConsumePacket(IPacket<?> packet) {
        if (!trackSelf.get() || !trackerConsume.get()) {
            lastSelfUsePacket = null;
            return;
        }

        if (packet instanceof CPlayerTryUseItemPacket) {
            Hand hand = ((CPlayerTryUseItemPacket) packet).getHand();
            ItemStack stack = mc.player.getHeldItem(hand);
            lastSelfUsePacket = isConsumable(stack) ? new ActiveUseData(hand, stack.copy()) : null;
            lastSelfUsePacketTime = System.currentTimeMillis();
            return;
        }

        if (packet instanceof CPlayerDiggingPacket) {
            CPlayerDiggingPacket.Action action = ((CPlayerDiggingPacket) packet).getAction();
            if (action == CPlayerDiggingPacket.Action.DROP_ITEM
                    || action == CPlayerDiggingPacket.Action.DROP_ALL_ITEMS
                    || action == CPlayerDiggingPacket.Action.SWAP_ITEM_WITH_OFFHAND
                    || action == CPlayerDiggingPacket.Action.RELEASE_USE_ITEM) {
                lastSelfUsePacket = null;
            }
        }
    }

    private void handleConsumeFinishPacket(SEntityStatusPacket packet) {
        if (packet.getOpCode() != 9) {
            return;
        }

        Entity entity = packet.getEntity(mc.world);
        if (!(entity instanceof PlayerEntity)) {
            return;
        }

        PlayerEntity player = (PlayerEntity) entity;
        if (!isTrackedPlayer(player)) {
            return;
        }

        ItemStack consumedStack = getPacketFinishedUseStack(player);
        if (!isConsumable(consumedStack)) {
            return;
        }

        UUID uuid = player.getUniqueID();
        long now = System.currentTimeMillis();
        long lastMessage = consumeMessageCooldown.getOrDefault(uuid, 0L);
        if (now - lastMessage < CONSUME_COOLDOWN_MS) {
            return;
        }

        consumeMessageCooldown.put(uuid, now);
        printConsumeMessage(player, consumedStack);
    }

    private ItemStack getPacketFinishedUseStack(PlayerEntity player) {
        if (isSelf(player) && lastSelfUsePacket != null) {
            ItemStack stack = lastSelfUsePacket.stack.copy();
            lastSelfUsePacket = null;
            return stack;
        }

        ItemStack activeStack = player.getActiveItemStack();
        if (!activeStack.isEmpty()) {
            return activeStack.copy();
        }

        if (player.isHandActive()) {
            return player.getHeldItem(player.getActiveHand()).copy();
        }

        return ItemStack.EMPTY;
    }

    private void handlePotionTracker() {
        Set<Integer> currentPotions = new HashSet<>();

        for (Entity entity : mc.world.getAllEntities()) {
            if (!(entity instanceof PotionEntity)) {
                continue;
            }

            int entityId = entity.getEntityId();
            if (mc.player.getDistance(entity) > trackerRadius.get()) {
                continue;
            }

            currentPotions.add(entityId);
            if (!trackedPotions.containsKey(entityId)) {
                PotionEntity potionEntity = (PotionEntity) entity;
                ItemStack potionStack = potionEntity.getItem();
                trackedPotions.put(entityId, new PotionData(potionStack.copy(), entity.getPosX(), entity.getPosY(), entity.getPosZ()));
            } else {
                PotionData data = trackedPotions.get(entityId);
                data.lastX = entity.getPosX();
                data.lastY = entity.getPosY();
                data.lastZ = entity.getPosZ();
            }
        }

        Set<Integer> removedPotions = new HashSet<>(trackedPotions.keySet());
        removedPotions.removeAll(currentPotions);

        for (int entityId : removedPotions) {
            PotionData data = trackedPotions.get(entityId);
            AxisAlignedBB potionBB = new AxisAlignedBB(
                    data.lastX - 4.0F,
                    data.lastY - 2.0F,
                    data.lastZ - 4.0F,
                    data.lastX + 4.0F,
                    data.lastY + 2.0F,
                    data.lastZ + 4.0F
            );

            Set<UUID> notified = new HashSet<>();
            for (LivingEntity hitEntity : mc.world.getEntitiesWithinAABB(LivingEntity.class, potionBB)) {
                if (!(hitEntity instanceof PlayerEntity)) {
                    continue;
                }

                PlayerEntity player = (PlayerEntity) hitEntity;
                if (!isTrackedPlayer(player)) {
                    continue;
                }
                if (!isPotionHit(player, data)) {
                    continue;
                }
                UUID id = isSelf(player) ? mc.player.getUniqueID() : player.getUniqueID();
                if (!notified.add(id)) {
                    continue;
                }
                showPotionHit(player, data);
            }

            if (trackSelf.get() && mc.player != null && isPotionHit(mc.player, data)) {
                UUID selfUuid = mc.player.getUniqueID();
                if (!notified.contains(selfUuid)) {
                    showPotionHit(mc.player, data);
                }
            }

            trackedPotions.remove(entityId);
        }
    }

    private void showPotionHit(PlayerEntity player, PotionData data) {
        double hitChance = getPotionHitChance(player, data);
        ItemStack potionStack = data.stack;
        List<EffectInstance> effects = PotionUtils.getEffectsFromStack(potionStack);

        printPotionMessage(player.getName().getString(), potionStack);
        TextFormatting chanceColor = getChanceColor(hitChance);
        String hitMessage = TextFormatting.DARK_GRAY + "• " + TextFormatting.GRAY + "Успешность: " + chanceColor + String.format("%.0f%%", hitChance);
        mc.ingameGUI.getChatGUI().printChatMessage(new StringTextComponent(hitMessage));

        if (trackerEffects.get() && !effects.isEmpty()) {
            for (EffectInstance effect : effects) {
                String effectName = effect.getPotion().getDisplayName().getString();
                int amplifier = effect.getAmplifier() + 1;
                int duration = effect.getDuration() / 20;
                int adjustedDuration = (int) (duration * (hitChance / 100.0F));
                int minutes = adjustedDuration / 60;
                int seconds = adjustedDuration % 60;
                String effectMessage = TextFormatting.DARK_GRAY + "• " + TextFormatting.RED + effectName + TextFormatting.RED + " " + toRoman(amplifier) + TextFormatting.GRAY + " (" + minutes + ":" + String.format("%02d", seconds) + ")";
                mc.ingameGUI.getChatGUI().printChatMessage(new StringTextComponent(effectMessage));
            }
        }
    }

    private void handleTotemPop(SEntityStatusPacket packet) {
        if (packet.getOpCode() != 35) {
            return;
        }

        Entity entity = packet.getEntity(mc.world);
        if (!(entity instanceof PlayerEntity)) {
            return;
        }

        PlayerEntity player = (PlayerEntity) entity;
        if (!isTrackedPlayer(player)) {
            return;
        }

        UUID uuid = player.getUniqueID();
        long now = System.currentTimeMillis();
        long last = totemMessageCooldown.getOrDefault(uuid, 0L);
        if (now - last < TOTEM_COOLDOWN_MS) {
            return;
        }
        totemMessageCooldown.put(uuid, now);

        boolean enchanted = lastSeenTotemEnchanted.getOrDefault(uuid, false);
        String mark = enchanted ? TextFormatting.GREEN + "✔" : TextFormatting.RED + "✖";
        print(TextFormatting.WHITE + player.getName().getString() + TextFormatting.GRAY + " потерял " + TextFormatting.RED + "Тотем Бессмертия" + TextFormatting.GRAY + ", зачарован: " + mark);
    }

    private boolean isConsumable(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        UseAction action = stack.getUseAction();
        return action == UseAction.DRINK || action == UseAction.EAT;
    }

    private void printPotionMessage(String playerName, ItemStack potionStack) {
        mc.ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(
                GradientUtil.gradient("Harmony")
                        .append(new StringTextComponent(" "))
                        .append(GradientUtil.gradient("»"))
                        .append(new StringTextComponent(TextFormatting.DARK_GRAY + " " + TextFormatting.RESET + TextFormatting.WHITE + playerName + TextFormatting.GRAY + " получил "))
                        .append(potionStack.getDisplayName().deepCopy()),
                0
        );
    }

    private void printConsumeMessage(PlayerEntity player, ItemStack consumedStack) {
        UseAction useAction = consumedStack.getUseAction();
        String action = useAction == UseAction.DRINK ? "выпил" : "съел";
        if (consumedStack.getItem() == Items.GOLDEN_APPLE || consumedStack.getItem() == Items.ENCHANTED_GOLDEN_APPLE) {
            action = "съел";
        }

        String itemName = consumedStack.getDisplayName().getString();
        print(TextFormatting.WHITE + player.getName().getString() + TextFormatting.GRAY + " " + action + " " + TextFormatting.RED + itemName);
    }

    private TextFormatting getChanceColor(double chance) {
        if (chance >= 80.0D) {
            return TextFormatting.GREEN;
        }
        if (chance >= 60.0D) {
            return TextFormatting.YELLOW;
        }
        if (chance >= 40.0D) {
            return TextFormatting.GOLD;
        }
        return TextFormatting.RED;
    }

    private String toRoman(int number) {
        return switch (number) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> String.valueOf(number);
        };
    }

    private boolean isTrackerVisible() {
        return tracker != null && tracker.get();
    }

    private boolean isTrackedPlayer(PlayerEntity player) {
        if (player == null || mc.player == null) {
            return false;
        }
        if (isSelf(player)) {
            return trackSelf.get();
        }
        return mc.player.getDistance(player) <= trackerRadius.get();
    }

    private boolean isEntityValid(Entity entity) {
        if (!(entity instanceof AbstractClientPlayerEntity)) {
            return false;
        }
        if (BotSessionManager.isManagedGhostEntityId(entity.getEntityId())) {
            return false;
        }
        if (entity instanceof PlayerEntity && ((PlayerEntity) entity).isBot) {
            return false;
        }
        if (!trackSelf.get() && isSelf(entity)) {
            return false;
        }
        return !(mc.player.getDistance(entity) < 100);
    }

    private boolean isSelf(Entity entity) {
        return entity != null
                && mc.player != null
                && (entity == mc.player
                || entity.getEntityId() == mc.player.getEntityId()
                || mc.player.getUniqueID().equals(entity.getUniqueID()));
    }

    private boolean isPotionHit(PlayerEntity player, PotionData data) {
        double dx = player.getPosX() - data.lastX;
        double dz = player.getPosZ() - data.lastZ;
        return Math.sqrt(dx * dx + dz * dz) <= 4.0F;
    }

    private double getPotionHitChance(PlayerEntity player, PotionData data) {
        double dx = player.getPosX() - data.lastX;
        double dz = player.getPosZ() - data.lastZ;
        double distance = Math.sqrt(dx * dx + dz * dz);
        double proximity = Math.max(0.0F, 1.0F - distance / 4.0F);
        return proximity * 100.0F;
    }

    public boolean isHungryBarEnabled() {
        return hungryBar.get();
    }

    @Override
    public boolean onDisable() {
        trackedPotions.clear();
        lastSeenTotemEnchanted.clear();
        consumeMessageCooldown.clear();
        totemMessageCooldown.clear();
        lastSelfUsePacket = null;
        lastSelfUsePacketTime = 0L;
        super.onDisable();
        return false;
    }

    private static class PotionData {
        private final ItemStack stack;
        private double lastX;
        private double lastY;
        private double lastZ;

        private PotionData(ItemStack stack, double lastX, double lastY, double lastZ) {
            this.stack = stack;
            this.lastX = lastX;
            this.lastY = lastY;
            this.lastZ = lastZ;
        }
    }

    private static class ActiveUseData {
        private final Hand hand;
        private final ItemStack stack;

        private ActiveUseData(Hand hand, ItemStack stack) {
            this.hand = hand;
            this.stack = stack;
        }
    }
}
