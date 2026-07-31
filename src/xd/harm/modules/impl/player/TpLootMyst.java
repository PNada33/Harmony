package xd.harm.modules.impl.player;

import com.google.common.eventbus.Subscribe;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.client.gui.ClientBossInfo;
import net.minecraft.client.gui.overlay.PlayerTabOverlayGui;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.ChestContainer;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.BannerItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.network.play.client.CPlayerPacket;
import net.minecraft.network.play.client.CPlayerTryUseItemOnBlockPacket;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import org.lwjgl.glfw.GLFW;
import xd.harm.events.input.EventKey;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BindSetting;
import xd.harm.utils.math.StopWatch;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ModuleRegister(name = "TpLootMyst", category = Category.Player, desc = "Авто лут все дела")
public class TpLootMyst extends Module {

    private final BindSetting pauseKey = new BindSetting("Пауза", GLFW.GLFW_KEY_P);

    private final StopWatch lootTimer = new StopWatch();
    private final StopWatch openTimer = new StopWatch();
    private final StopWatch emptyTimer = new StopWatch();
    private final StopWatch clickTimer = new StopWatch();
    private final StopWatch chestOpenWaitTimer = new StopWatch();
    private final StopWatch eventStartDelayTimer = new StopWatch();
    private final StopWatch dropTimer = new StopWatch();
    private final StopWatch reopenDelayTimer = new StopWatch();

    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)\\s*сек|(\\d+)\\s*мин|(\\d+):(\\d+)");
    private static final Pattern ANARCHY_PATTERN = Pattern.compile("Анархия-(\\d+)");

    private static final double HORIZONTAL_THRESHOLD = 2.0;
    private static final double ARRIVAL_THRESHOLD = 0.5;
    private static final long TELEPORT_INTERVAL = 400L;
    private static final double MOVEMENT_SPEED = 0.3;
    private static final int SEARCH_RADIUS = 64;
    private static final int BEACON_SEARCH_RADIUS = 32;
    private static final long LOOT_DELAY = 40L;
    private static final long OPEN_DELAY = 150L;
    private static final long EMPTY_TIMEOUT = 300L;
    private static final long CLICK_DELAY = 50L;
    private static final long CHEST_OPEN_TIMEOUT = 300L;
    private static final long EVENT_START_DELAY = 850L;
    private static final long DROP_DELAY = 50L;
    private static final long REOPEN_DELAY = 150L;
    private static final int MAX_CLICKS = 3;
    private static final int NEARBY_CHEST_RADIUS = 12;
    private static final int MAX_OPEN_ATTEMPTS = 15;
    private static final int TRIGGER_SECONDS = 3;

    private Vector3d targetPosition = null;
    private BlockPos currentChest = null;
    private BlockPos enderChestPos = null;
    private BlockPos beaconPos = null;
    private BlockPos chestToReopenAfterDrop = null;

    private boolean isMovingToChest = false;
    private boolean isTeleportingUnder = false;
    private boolean isInCloseRange = false;
    private boolean waitingForEvent = false;
    private boolean joinedAnarchy = false;
    private boolean clickingLMB = false;
    private boolean clickedLMB = false;
    private boolean waitingForStart = false;
    private boolean readyToOpen = false;
    private boolean isLootingNearby = false;
    private boolean firstChestOpened = false;
    private boolean waitingForChestToOpen = false;
    private boolean usingBeacon = false;
    private boolean lootingFinished = false;
    private boolean eventStartedByBossBar = false;
    private boolean waitingEventStartDelay = false;
    private boolean isPaused = false;
    private boolean isDroppingTrash = false;
    private boolean waitingToReopenChest = false;
    private boolean justReopenedChest = false;
    private int teleportAttempts = 0;
    private long lastTeleportTime = 0L;
    private int openAttempts = 0;
    private int reopenAttempts = 0;
    private int clickCount = 0;
    private int lastAnnouncedSecond = -1;

    private int savedInitialSeconds = 0;
    private long savedStartTime = 0;
    private long pausedTime = 0;
    private boolean isTracking = false;
    private String savedAnarchyNumber = null;

    private Set<BlockPos> lootedChests = new HashSet<>();
    private Set<BlockPos> failedChests = new HashSet<>();
    private BlockPos currentNearbyChest = null;
    private BlockPos lastAttemptedChest = null;

    public TpLootMyst() {
        addSettings(pauseKey);
    }

    @Override
    public boolean onEnable() {
        super.onEnable();
        reset();
        print("§fВключён, ищу сундук...");
        startMovingToChest();
        return false;
    }

    @Override
    public boolean onDisable() {
        super.onDisable();
        reset();
        return false;
    }

    private void reset() {
        targetPosition = null;
        currentChest = null;
        enderChestPos = null;
        beaconPos = null;
        chestToReopenAfterDrop = null;
        isMovingToChest = false;
        isTeleportingUnder = false;
        isInCloseRange = false;
        waitingForEvent = false;
        joinedAnarchy = false;
        clickingLMB = false;
        clickedLMB = false;
        waitingForStart = false;
        readyToOpen = false;
        isLootingNearby = false;
        firstChestOpened = false;
        waitingForChestToOpen = false;
        usingBeacon = false;
        lootingFinished = false;
        eventStartedByBossBar = false;
        waitingEventStartDelay = false;
        isPaused = false;
        isDroppingTrash = false;
        waitingToReopenChest = false;
        justReopenedChest = false;
        teleportAttempts = 0;
        lastTeleportTime = 0L;
        openAttempts = 0;
        reopenAttempts = 0;
        clickCount = 0;
        lastAnnouncedSecond = -1;
        savedInitialSeconds = 0;
        savedStartTime = 0;
        pausedTime = 0;
        isTracking = false;
        savedAnarchyNumber = null;
        lootedChests.clear();
        failedChests.clear();
        currentNearbyChest = null;
        lastAttemptedChest = null;
    }

    private void resetForNextChest() {
        waitingForChestToOpen = false;
        waitingToReopenChest = false;
        justReopenedChest = false;
        isDroppingTrash = false;
        chestToReopenAfterDrop = null;
        openAttempts = 0;
        reopenAttempts = 0;
        currentNearbyChest = null;
        openTimer.reset();
        emptyTimer.reset();
        lootTimer.reset();
    }

    private boolean isInventoryFull() {
        if (mc.player == null) return false;
        for (int i = 9; i < 45; i++) {
            if (mc.player.container.getSlot(i).getStack().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private boolean isTrashItem(Item item) {
        if (item instanceof SpawnEggItem) return true;
        if (item instanceof BannerItem) return true;
        return item == Items.NAME_TAG
                || item == Items.GOLD_ORE
                || item == Items.GOLD_INGOT
                || item == Items.GOLD_NUGGET
                || item == Items.NETHER_GOLD_ORE
                || item == Items.DIAMOND
                || item == Items.DIAMOND_ORE
                || item == Items.IRON_ORE
                || item == Items.IRON_INGOT
                || item == Items.COAL
                || item == Items.COAL_ORE
                || item == Items.TRIDENT
                || item == Items.LAPIS_LAZULI
                || item == Items.LAPIS_ORE
                || item == Items.REDSTONE
                || item == Items.REDSTONE_ORE
                || item == Items.EMERALD
                || item == Items.EMERALD_ORE
                || item == Items.END_CRYSTAL
                || item == Items.GUNPOWDER
                || item == Items.BLAZE_ROD
                || item == Items.HONEY_BOTTLE
                || item == Items.DRAGON_BREATH
                || item == Items.PUFFERFISH
                || item == Items.BONE
                || item == Items.SLIME_BALL
                || item == Items.ENCHANTING_TABLE
                || item == Items.QUARTZ
                || item == Items.CONDUIT
                || item == Items.BOOKSHELF
                || item == Items.BOW
                || item == Items.WITHER_ROSE
                || item == Items.FERMENTED_SPIDER_EYE
                || item == Items.DIAMOND_SHOVEL
                || item == Items.NAUTILUS_SHELL
                || item == Items.FIRE_CHARGE
                || item == Items.NETHERITE_SHOVEL
                || item == Items.NETHER_QUARTZ_ORE;
    }

    private boolean hasTrashInInventory() {
        if (mc.player == null) return false;
        for (int i = 9; i < 45; i++) {
            ItemStack stack = mc.player.container.getSlot(i).getStack();
            if (!stack.isEmpty() && isTrashItem(stack.getItem())) {
                return true;
            }
        }
        return false;
    }

    private boolean dropOneTrashItem() {
        if (mc.player == null) return false;
        for (int i = 9; i < 45; i++) {
            ItemStack stack = mc.player.container.getSlot(i).getStack();
            if (!stack.isEmpty() && isTrashItem(stack.getItem())) {
                mc.playerController.windowClick(0, i, 1, ClickType.THROW, mc.player);
                return true;
            }
        }
        return false;
    }

    @Subscribe
    private void onKey(EventKey event) {
        if (event.getKey() == pauseKey.get()) {
            isPaused = !isPaused;
            if (isPaused) {
                pausedTime = System.currentTimeMillis();
                if (isChestOpen() && mc.player != null) {
                    mc.player.closeScreen();
                }
                print("§e§lПАУЗА §7(осталось сундуков: В§e" + countRemainingChests() + "В§7)");
            } else {
                if (pausedTime > 0 && savedStartTime > 0) {
                    long pauseDuration = System.currentTimeMillis() - pausedTime;
                    savedStartTime += pauseDuration;
                }
                pausedTime = 0;
                lootTimer.reset();
                openTimer.reset();
                emptyTimer.reset();
                clickTimer.reset();
                chestOpenWaitTimer.reset();
                dropTimer.reset();
                reopenDelayTimer.reset();
                waitingForChestToOpen = false;
                waitingToReopenChest = false;
                justReopenedChest = false;
                openAttempts = 0;
                reopenAttempts = 0;
                isDroppingTrash = false;
                chestToReopenAfterDrop = null;
                if (firstChestOpened && !isChestOpen()) {
                    isLootingNearby = true;
                    currentNearbyChest = null;
                }
                print("§a§lВОЗОБНОВЛЕНО");
            }
        }
    }

    private void sendChat(String message) {
        if (mc.player != null) {
            mc.player.sendMessage(new StringTextComponent(message), mc.player.getUniqueID());
        }
    }

    private int scanBossBarForSeconds() {
        if (mc.ingameGUI == null) return -1;
        Map<UUID, ClientBossInfo> bossInfoMap = mc.ingameGUI.getBossOverlay().mapBossInfos;
        if (bossInfoMap == null || bossInfoMap.isEmpty()) {
            return -1;
        }
        for (ClientBossInfo bossInfo : bossInfoMap.values()) {
            if (bossInfo == null) continue;
            if (bossInfo.getName() == null) continue;
            String text = bossInfo.getName().getString();
            if (isEventText(text)) {
                return parseTime(text);
            }
        }
        return -1;
    }

    private void scanBossBar() {
        if (mc.ingameGUI == null) return;
        Map<UUID, ClientBossInfo> bossInfoMap = mc.ingameGUI.getBossOverlay().mapBossInfos;
        if (bossInfoMap == null || bossInfoMap.isEmpty()) {
            return;
        }
        for (ClientBossInfo bossInfo : bossInfoMap.values()) {
            if (bossInfo == null) continue;
            if (bossInfo.getName() == null) continue;
            String text = bossInfo.getName().getString();
            if (isEventText(text)) {
                int seconds = parseTime(text);
                if (seconds > 0 && !isTracking) {
                    isTracking = true;
                    savedInitialSeconds = seconds;
                    savedStartTime = System.currentTimeMillis();
                    print("§fОбнаружен ивент!");
                }
                if (seconds == 0 && isTracking) {
                    eventStartedByBossBar = true;
                }
                return;
            }
        }
    }

    private boolean isEventText(String text) {
        return text.contains("До активации")
                || text.contains("До открытия")
                || text.contains("до активации")
                || text.contains("до открытия")
                || text.contains("До начала")
                || text.contains("до начала");
    }

    private int parseTime(String text) {
        int totalSeconds = 0;
        Matcher matcher = TIME_PATTERN.matcher(text);
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                totalSeconds += Integer.parseInt(matcher.group(1));
            } else if (matcher.group(2) != null) {
                totalSeconds += Integer.parseInt(matcher.group(2)) * 60;
            } else if (matcher.group(3) != null && matcher.group(4) != null) {
                totalSeconds += Integer.parseInt(matcher.group(3)) * 60 + Integer.parseInt(matcher.group(4));
            }
        }
        return totalSeconds;
    }

    private int getRemainingSeconds() {
        if (!isTracking || savedStartTime == 0) return -1;
        long elapsed = (System.currentTimeMillis() - savedStartTime) / 1000;
        return savedInitialSeconds - (int) elapsed;
    }

    private boolean isEventStarted() {
        if (eventStartedByBossBar) return true;
        int bossBarSeconds = scanBossBarForSeconds();
        if (bossBarSeconds == 0) return true;
        return getRemainingSeconds() <= 0;
    }

    private String formatTime(int seconds) {
        int min = seconds / 60;
        int sec = seconds % 60;
        if (min > 0) {
            return String.format("%d:%02d", min, sec);
        }
        return seconds + " сек";
    }

    private String findAnarchyNumberFromTab() {
        if (mc.player == null || mc.ingameGUI == null) return null;
        PlayerTabOverlayGui tabOverlay = mc.ingameGUI.getTabList();
        if (tabOverlay.header != null) {
            String headerText = tabOverlay.header.getString();
            Matcher matcher = ANARCHY_PATTERN.matcher(headerText);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        if (tabOverlay.footer != null) {
            String footerText = tabOverlay.footer.getString();
            Matcher matcher = ANARCHY_PATTERN.matcher(footerText);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        if (mc.player.connection != null) {
            Collection<NetworkPlayerInfo> players = mc.player.connection.getPlayerInfoMap();
            for (NetworkPlayerInfo info : players) {
                if (info.getDisplayName() != null) {
                    String name = info.getDisplayName().getString();
                    Matcher matcher = ANARCHY_PATTERN.matcher(name);
                    if (matcher.find()) {
                        return matcher.group(1);
                    }
                }
            }
        }
        return null;
    }

    private void clickAttack() {
        if (mc.player != null && mc.playerController != null) {
            BlockPos pos = beaconPos != null ? beaconPos : (enderChestPos != null ? enderChestPos : currentChest);
            if (pos != null) {
                mc.playerController.clickBlock(pos, Direction.UP);
            }
        }
    }

    private double getDistanceToChest(BlockPos pos) {
        if (mc.player == null || pos == null) return Double.MAX_VALUE;
        return Math.sqrt(mc.player.getDistanceSq(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
    }

    private boolean isChestStillExists(BlockPos pos) {
        if (mc.world == null || pos == null) return false;
        Block block = mc.world.getBlockState(pos).getBlock();
        return block instanceof ChestBlock || block instanceof EnderChestBlock;
    }

    private BlockPos findBeaconNearChests() {
        if (mc.player == null || mc.world == null) return null;
        BlockPos playerPos = mc.player.getPosition();
        BlockPos closestBeacon = null;
        double closestDistance = Double.MAX_VALUE;
        for (int y = -BEACON_SEARCH_RADIUS; y <= BEACON_SEARCH_RADIUS; ++y) {
            for (int x = -BEACON_SEARCH_RADIUS; x <= BEACON_SEARCH_RADIUS; ++x) {
                for (int z = -BEACON_SEARCH_RADIUS; z <= BEACON_SEARCH_RADIUS; ++z) {
                    BlockPos pos = playerPos.add(x, y, z);
                    Block block = mc.world.getBlockState(pos).getBlock();
                    if (block == Blocks.BEACON) {
                        if (hasChestsNearby(pos, NEARBY_CHEST_RADIUS)) {
                            double distance = mc.player.getDistanceSq(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                            if (distance < closestDistance) {
                                closestDistance = distance;
                                closestBeacon = pos;
                            }
                        }
                    }
                }
            }
        }
        return closestBeacon;
    }

    private boolean hasChestsNearby(BlockPos center, int radius) {
        if (mc.world == null) return false;
        for (int y = -radius; y <= radius; ++y) {
            for (int x = -radius; x <= radius; ++x) {
                for (int z = -radius; z <= radius; ++z) {
                    BlockPos pos = center.add(x, y, z);
                    Block block = mc.world.getBlockState(pos).getBlock();
                    if (block instanceof ChestBlock || block instanceof EnderChestBlock) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private BlockPos findAnyChestNearby() {
        if (mc.player == null || mc.world == null) return null;
        BlockPos playerPos = mc.player.getPosition();
        BlockPos closestChest = null;
        double closestDistance = Double.MAX_VALUE;
        for (int y = -SEARCH_RADIUS; y <= SEARCH_RADIUS; ++y) {
            for (int x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; ++x) {
                for (int z = -SEARCH_RADIUS; z <= SEARCH_RADIUS; ++z) {
                    BlockPos pos = playerPos.add(x, y, z);
                    Block block = mc.world.getBlockState(pos).getBlock();
                    if (block instanceof ChestBlock || block instanceof EnderChestBlock) {
                        if (lootedChests.contains(pos) || failedChests.contains(pos)) continue;
                        double distance = mc.player.getDistanceSq(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                        if (distance < closestDistance) {
                            closestDistance = distance;
                            closestChest = pos;
                        }
                    }
                }
            }
        }
        return closestChest;
    }

    private BlockPos findNearbyChestToLoot() {
        if (mc.player == null || mc.world == null) return null;
        BlockPos searchCenter = beaconPos != null ? beaconPos : mc.player.getPosition();
        List<BlockPos> availableChests = new ArrayList<>();
        for (int y = -NEARBY_CHEST_RADIUS; y <= NEARBY_CHEST_RADIUS; ++y) {
            for (int x = -NEARBY_CHEST_RADIUS; x <= NEARBY_CHEST_RADIUS; ++x) {
                for (int z = -NEARBY_CHEST_RADIUS; z <= NEARBY_CHEST_RADIUS; ++z) {
                    BlockPos pos = searchCenter.add(x, y, z);
                    Block block = mc.world.getBlockState(pos).getBlock();
                    if (block instanceof ChestBlock || block instanceof EnderChestBlock) {
                        if (lootedChests.contains(pos) || failedChests.contains(pos)) continue;
                        availableChests.add(pos);
                    }
                }
            }
        }
        availableChests.sort((a, b) -> {
            double distA = mc.player.getDistanceSq(a.getX() + 0.5, a.getY() + 0.5, a.getZ() + 0.5);
            double distB = mc.player.getDistanceSq(b.getX() + 0.5, b.getY() + 0.5, b.getZ() + 0.5);
            return Double.compare(distA, distB);
        });
        return availableChests.isEmpty() ? null : availableChests.get(0);
    }

    private int countRemainingChests() {
        if (mc.player == null || mc.world == null) return 0;
        BlockPos searchCenter = beaconPos != null ? beaconPos : mc.player.getPosition();
        int count = 0;
        for (int y = -NEARBY_CHEST_RADIUS; y <= NEARBY_CHEST_RADIUS; ++y) {
            for (int x = -NEARBY_CHEST_RADIUS; x <= NEARBY_CHEST_RADIUS; ++x) {
                for (int z = -NEARBY_CHEST_RADIUS; z <= NEARBY_CHEST_RADIUS; ++z) {
                    BlockPos pos = searchCenter.add(x, y, z);
                    Block block = mc.world.getBlockState(pos).getBlock();
                    if (block instanceof ChestBlock || block instanceof EnderChestBlock) {
                        if (!lootedChests.contains(pos) && !failedChests.contains(pos)) {
                            count++;
                        }
                    }
                }
            }
        }
        return count;
    }

    private BlockPos findEnderChestNear(BlockPos center) {
        if (mc.player == null || mc.world == null || center == null) return null;
        BlockPos closestEnder = null;
        double closestDistance = Double.MAX_VALUE;
        for (int y = -5; y <= 5; ++y) {
            for (int x = -5; x <= 5; ++x) {
                for (int z = -5; z <= 5; ++z) {
                    BlockPos pos = center.add(x, y, z);
                    Block block = mc.world.getBlockState(pos).getBlock();
                    if (block instanceof EnderChestBlock) {
                        double distance = center.distanceSq(pos);
                        if (distance < closestDistance) {
                            closestDistance = distance;
                            closestEnder = pos;
                        }
                    }
                }
            }
        }
        return closestEnder;
    }

    private void openChestOnce(BlockPos pos) {
        if (pos == null || mc.player == null) return;
        Vector3d hitVec = new Vector3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        BlockRayTraceResult rayTraceResult = new BlockRayTraceResult(hitVec, Direction.UP, pos, false);
        mc.player.connection.sendPacket(new CPlayerTryUseItemOnBlockPacket(Hand.MAIN_HAND, rayTraceResult));
        lastAttemptedChest = pos;
        waitingForChestToOpen = true;
        chestOpenWaitTimer.reset();
    }

    private boolean isChestOpen() {
        return mc.player != null && mc.player.openContainer instanceof ChestContainer;
    }

    private boolean hasItemsInChest() {
        if (!isChestOpen()) return false;
        ChestContainer container = (ChestContainer) mc.player.openContainer;
        IInventory inventory = container.getLowerChestInventory();
        for (int i = 0; i < inventory.getSizeInventory(); ++i) {
            if (inventory.getStackInSlot(i).getItem() != Items.AIR) {
                return true;
            }
        }
        return false;
    }

    private void finishCurrentChestAndMoveNext() {
        BlockPos chestToMark = currentNearbyChest != null ? currentNearbyChest : (enderChestPos != null ? enderChestPos : currentChest);
        if (chestToMark != null && !lootedChests.contains(chestToMark)) {
            lootedChests.add(chestToMark);
        }
        if (isChestOpen() && mc.player != null) {
            mc.player.closeScreen();
        }
        isLootingNearby = true;
        resetForNextChest();
        int remaining = countRemainingChests();
        print("§fЗалутан! Осталось: В§e" + remaining);
    }

    private void lootOpenedChest() {
        if (!isChestOpen()) return;
        waitingForChestToOpen = false;
        waitingToReopenChest = false;
        reopenAttempts = 0;
        if (justReopenedChest) {
            justReopenedChest = false;
            emptyTimer.reset();
            lootTimer.reset();
        }
        ChestContainer container = (ChestContainer) mc.player.openContainer;
        IInventory inventory = container.getLowerChestInventory();
        if (hasItemsInChest()) {
            emptyTimer.reset();
            if (lootTimer.hasTimeElapsed(LOOT_DELAY)) {
                for (int i = 0; i < inventory.getSizeInventory(); ++i) {
                    if (inventory.getStackInSlot(i).getItem() != Items.AIR) {
                        if (isInventoryFull()) {
                            if (hasTrashInInventory()) {
                                chestToReopenAfterDrop = currentNearbyChest != null ? currentNearbyChest : (enderChestPos != null ? enderChestPos : currentChest);
                                mc.player.closeScreen();
                                isDroppingTrash = true;
                                waitingForChestToOpen = false;
                                waitingToReopenChest = false;
                                reopenAttempts = 0;
                                dropTimer.reset();
                                return;
                            } else {
                                BlockPos chestToMark = currentNearbyChest != null ? currentNearbyChest : (enderChestPos != null ? enderChestPos : currentChest);
                                if (chestToMark != null) {
                                    lootedChests.add(chestToMark);
                                }
                                mc.player.closeScreen();
                                isLootingNearby = true;
                                resetForNextChest();
                                print("§cИнвентарь полон, нет мусора для выброса!");
                                return;
                            }
                        }
                        mc.playerController.windowClick(container.windowId, i, 0, ClickType.QUICK_MOVE, mc.player);
                        lootTimer.reset();
                        return;
                    }
                }
            }
        } else {
            if (emptyTimer.hasTimeElapsed(EMPTY_TIMEOUT)) {
                finishCurrentChestAndMoveNext();
            }
        }
    }

    private boolean processMoveToChest() {
        if (targetPosition == null || !isMovingToChest) {
            return false;
        }
        if (mc.player == null || mc.world == null) {
            reset();
            return false;
        }
        Vector3d currentPos = mc.player.getPositionVec();
        double deltaX = targetPosition.x - currentPos.x;
        double deltaZ = targetPosition.z - currentPos.z;
        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        if (horizontalDistance <= HORIZONTAL_THRESHOLD) {
            return false;
        }
        if (horizontalDistance > 0.1) {
            double dirX = deltaX / horizontalDistance;
            double dirZ = deltaZ / horizontalDistance;
            mc.player.setMotion(dirX * MOVEMENT_SPEED, 0.0, dirZ * MOVEMENT_SPEED);
        } else {
            mc.player.setMotion(0.0, 0.0, 0.0);
        }
        double newX = mc.player.getPosX() + mc.player.getMotion().x;
        double newY = mc.player.getPosY() + mc.player.getMotion().y;
        double newZ = mc.player.getPosZ() + mc.player.getMotion().z;
        mc.player.setPosition(newX, newY, newZ);
        rotateToTarget(targetPosition.x, targetPosition.z);
        return true;
    }

    private boolean processTeleportUnder() {
        if (targetPosition == null || !isTeleportingUnder) {
            return false;
        }
        if (mc.player == null || mc.world == null) {
            reset();
            return false;
        }
        Vector3d currentPos = mc.player.getPositionVec();
        double totalDistance = currentPos.distanceTo(targetPosition);
        if (isInCloseRange) {
            if (totalDistance <= ARRIVAL_THRESHOLD) {
                return false;
            }
            if (teleportAttempts >= 100) {
                return false;
            }
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastTeleportTime >= TELEPORT_INTERVAL) {
                sendTeleportPackets(targetPosition.x, targetPosition.y, targetPosition.z);
                ++teleportAttempts;
                lastTeleportTime = currentTime;
            }
            return true;
        }
        isInCloseRange = true;
        teleportAttempts = 0;
        return true;
    }

    private void rotateToTarget(double targetX, double targetZ) {
        Vector3d eyePos = mc.player.getEyePosition(1.0F);
        double deltaX = targetX - eyePos.x;
        double deltaZ = targetZ - eyePos.z;
        float yaw = (float) Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0F;
        yaw = MathHelper.wrapDegrees(yaw);
        mc.player.rotationYaw = yaw;
        mc.player.rotationYawHead = yaw;
        mc.player.prevRotationYaw = yaw;
    }

    private void sendTeleportPackets(double x, double y, double z) {
        if (mc.player == null) return;
        double currentX = mc.player.getPosX();
        double currentY = mc.player.getPosY();
        double currentZ = mc.player.getPosZ();
        double deltaX = x - currentX;
        double deltaY = y - currentY;
        double deltaZ = z - currentZ;
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
        if (distance < 0.1) return;
        double stepSize = 1.0;
        int steps = (int) Math.ceil(distance / stepSize);
        double stepX = deltaX / steps;
        double stepY = deltaY / steps;
        double stepZ = deltaZ / steps;
        double newX = currentX;
        double newY = currentY;
        double newZ = currentZ;
        for (int i = 0; i < steps; ++i) {
            newX += stepX;
            newY += stepY;
            newZ += stepZ;
            mc.player.connection.sendPacket(new CPlayerPacket.PositionPacket(newX, newY, newZ, false));
        }
        mc.player.connection.sendPacket(new CPlayerPacket.PositionPacket(x, y, z, true));
    }

    private void startMovingToChest() {
        if (mc.player == null || mc.world == null) return;
        beaconPos = findBeaconNearChests();
        if (beaconPos != null) {
            usingBeacon = true;
            currentChest = findAnyChestNearby();
            savedAnarchyNumber = findAnarchyNumberFromTab();
            if (savedAnarchyNumber != null) {
                print("§fЗапомнил анархию: В§e/an" + savedAnarchyNumber);
            } else {
                print("§fНе нашёл Анархия-X в табе!");
            }
            targetPosition = new Vector3d(
                    beaconPos.getX() + 0.5,
                    mc.player.getPosY(),
                    beaconPos.getZ() + 0.5
            );
            isMovingToChest = true;
            isTeleportingUnder = false;
            isInCloseRange = false;
            teleportAttempts = 0;
            lastTeleportTime = 0L;
            int totalChests = countRemainingChests();
            print("§fЛечу к маяку... (сундуков рядом: В§e" + totalChests + "В§f)");
        } else {
            usingBeacon = false;
            currentChest = findAnyChestNearby();
            if (currentChest != null) {
                savedAnarchyNumber = findAnarchyNumberFromTab();
                if (savedAnarchyNumber != null) {
                    print("§fЗапомнил анархию: В§e/an" + savedAnarchyNumber);
                } else {
                    print("§fНе нашёл Анархия-X в табе!");
                }
                targetPosition = new Vector3d(
                        currentChest.getX() + 0.5,
                        mc.player.getPosY(),
                        currentChest.getZ() + 0.5
                );
                isMovingToChest = true;
                isTeleportingUnder = false;
                isInCloseRange = false;
                teleportAttempts = 0;
                lastTeleportTime = 0L;
                int totalChests = countRemainingChests();
                print("§fЛечу к сундуку... (всего: В§e" + totalChests + "В§f)");
            } else {
                print("§fСундуки не найдены!");
            }
        }
    }

    private void startLooting() {
        waitingForStart = false;
        waitingEventStartDelay = false;
        BlockPos chestToOpen = findNearbyChestToLoot();
        if (chestToOpen != null) {
            currentNearbyChest = chestToOpen;
            openChestOnce(chestToOpen);
            firstChestOpened = true;
            readyToOpen = false;
            isLootingNearby = false;
            openAttempts = 0;
            openTimer.reset();
            emptyTimer.reset();
        } else {
            print("§cНет сундуков для лута!");
        }
    }

    @Subscribe
    private void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;
        if (isPaused) {
            return;
        }
        if (isDroppingTrash) {
            if (isChestOpen()) {
                mc.player.closeScreen();
                return;
            }
            if (dropTimer.hasTimeElapsed(DROP_DELAY)) {
                if (hasTrashInInventory()) {
                    dropOneTrashItem();
                    dropTimer.reset();
                } else {
                    isDroppingTrash = false;
                    if (chestToReopenAfterDrop != null && isChestStillExists(chestToReopenAfterDrop)) {
                        waitingToReopenChest = true;
                        waitingForChestToOpen = false;
                        reopenAttempts = 0;
                        reopenDelayTimer.reset();
                    } else {
                        if (chestToReopenAfterDrop != null) {
                            lootedChests.add(chestToReopenAfterDrop);
                        }
                        chestToReopenAfterDrop = null;
                        isLootingNearby = true;
                        resetForNextChest();
                    }
                }
            }
            return;
        }
        if (waitingToReopenChest) {
            if (isChestOpen()) {
                waitingToReopenChest = false;
                waitingForChestToOpen = false;
                justReopenedChest = true;
                chestToReopenAfterDrop = null;
                reopenAttempts = 0;
                return;
            }
            if (waitingForChestToOpen) {
                if (chestOpenWaitTimer.hasTimeElapsed(CHEST_OPEN_TIMEOUT)) {
                    waitingForChestToOpen = false;
                    reopenAttempts++;
                } else {
                    return;
                }
            }
            if (reopenDelayTimer.hasTimeElapsed(REOPEN_DELAY)) {
                if (chestToReopenAfterDrop != null && isChestStillExists(chestToReopenAfterDrop)) {
                    if (reopenAttempts >= MAX_OPEN_ATTEMPTS) {
                        lootedChests.add(chestToReopenAfterDrop);
                        chestToReopenAfterDrop = null;
                        waitingToReopenChest = false;
                        waitingForChestToOpen = false;
                        isLootingNearby = true;
                        resetForNextChest();
                        print("§cНе удалось переоткрыть сундук");
                        return;
                    }
                    currentNearbyChest = chestToReopenAfterDrop;
                    openChestOnce(chestToReopenAfterDrop);
                    reopenDelayTimer.reset();
                } else {
                    if (chestToReopenAfterDrop != null) {
                        lootedChests.add(chestToReopenAfterDrop);
                    }
                    chestToReopenAfterDrop = null;
                    waitingToReopenChest = false;
                    waitingForChestToOpen = false;
                    isLootingNearby = true;
                    resetForNextChest();
                }
            }
            return;
        }
        if (waitingForChestToOpen) {
            if (isChestOpen()) {
                waitingForChestToOpen = false;
            } else if (chestOpenWaitTimer.hasTimeElapsed(CHEST_OPEN_TIMEOUT)) {
                waitingForChestToOpen = false;
                openAttempts++;
            }
        }
        if (isChestOpen()) {
            lootOpenedChest();
            return;
        }
        if (clickingLMB) {
            if (clickCount < MAX_CLICKS) {
                if (clickTimer.hasTimeElapsed(CLICK_DELAY)) {
                    clickAttack();
                    clickCount++;
                    clickTimer.reset();
                }
            } else {
                clickingLMB = false;
                clickedLMB = true;
                waitingForStart = true;
                waitingForEvent = false;
            }
            return;
        }
        scanBossBar();
        int remaining = getRemainingSeconds();
        if (isTracking && remaining != lastAnnouncedSecond && remaining >= 0 && (waitingForEvent || waitingForStart)) {
            lastAnnouncedSecond = remaining;
            if (remaining > 0) {
                print("§fДо ивента: В§e" + formatTime(remaining));
            } else {
                print("§e§lИВЕНТ НАЧАЛСЯ!");
            }
        }
        if (isMovingToChest) {
            boolean stillMoving = processMoveToChest();
            if (!stillMoving) {
                isMovingToChest = false;
                BlockPos targetBlock;
                if (usingBeacon && beaconPos != null) {
                    targetBlock = beaconPos;
                } else {
                    enderChestPos = findEnderChestNear(currentChest);
                    targetBlock = enderChestPos != null ? enderChestPos : currentChest;
                }
                isTeleportingUnder = true;
                isInCloseRange = false;
                teleportAttempts = 0;
                lastTeleportTime = 0L;
                targetPosition = new Vector3d(
                        targetBlock.getX() + 0.5,
                        targetBlock.getY() - 3,
                        targetBlock.getZ() + 0.5
                );
            }
            return;
        }
        if (isTeleportingUnder) {
            boolean stillTeleporting = processTeleportUnder();
            if (!stillTeleporting) {
                isTeleportingUnder = false;
                isInCloseRange = false;
                teleportAttempts = 0;
                waitingForEvent = true;
            }
            return;
        }
        if (waitingForEvent) {
            if (!isTracking) {
                return;
            }
            if (savedAnarchyNumber == null) {
                print("§fНет сохранённого номера анархии!");
                return;
            }
            if (!joinedAnarchy && remaining <= TRIGGER_SECONDS && remaining >= 0) {
                mc.player.sendChatMessage("/an" + savedAnarchyNumber);
                joinedAnarchy = true;
                clickingLMB = true;
                clickCount = 0;
                clickTimer.reset();
                print("§fЗашёл на /an" + savedAnarchyNumber);
            }
            return;
        }
        if (waitingForStart) {
            if (isEventStarted() && !waitingEventStartDelay) {
                waitingEventStartDelay = true;
                eventStartDelayTimer.reset();
            }
            if (waitingEventStartDelay && eventStartDelayTimer.hasTimeElapsed(EVENT_START_DELAY)) {
                startLooting();
            }
            return;
        }
        if (isLootingNearby) {
            if (waitingForChestToOpen) return;
            BlockPos nearbyChest = findNearbyChestToLoot();
            if (nearbyChest != null) {
                if (!isChestStillExists(nearbyChest)) {
                    lootedChests.add(nearbyChest);
                    return;
                }
                if (currentNearbyChest == null || !currentNearbyChest.equals(nearbyChest)) {
                    currentNearbyChest = nearbyChest;
                    openAttempts = 0;
                    openTimer.reset();
                }
                if (openAttempts >= MAX_OPEN_ATTEMPTS) {
                    failedChests.add(nearbyChest);
                    print("§cПропускаю сундук (осталось: В§e" + countRemainingChests() + "В§c)");
                    currentNearbyChest = null;
                    openAttempts = 0;
                    openTimer.reset();
                    return;
                }
                if (openTimer.hasTimeElapsed(OPEN_DELAY)) {
                    openTimer.reset();
                    openChestOnce(nearbyChest);
                    emptyTimer.reset();
                }
            } else {
                if (!lootingFinished) {
                    lootingFinished = true;
                    int totalLooted = lootedChests.size();
                    int totalFailed = failedChests.size();
                    print("§aГотово! §fЗалутано: В§a" + totalLooted + "В§f, пропущено: В§c" + totalFailed);
                }
                isLootingNearby = false;
                currentNearbyChest = null;
                currentChest = null;
                enderChestPos = null;
                openAttempts = 0;
            }
            return;
        }
        if (firstChestOpened && !isChestOpen()) {
            if (waitingForChestToOpen) return;
            if (openTimer.hasTimeElapsed(OPEN_DELAY)) {
                BlockPos chestToOpen = currentNearbyChest != null ? currentNearbyChest : findNearbyChestToLoot();
                if (chestToOpen != null) {
                    if (!isChestStillExists(chestToOpen)) {
                        lootedChests.add(chestToOpen);
                        isLootingNearby = true;
                        currentNearbyChest = null;
                        openAttempts = 0;
                        openTimer.reset();
                        return;
                    }
                    if (currentNearbyChest == null || !currentNearbyChest.equals(chestToOpen)) {
                        currentNearbyChest = chestToOpen;
                        openAttempts = 0;
                    }
                    if (openAttempts >= MAX_OPEN_ATTEMPTS) {
                        failedChests.add(chestToOpen);
                        print("§cСундук не открывается, ищу другие...");
                        isLootingNearby = true;
                        currentNearbyChest = null;
                        openAttempts = 0;
                        openTimer.reset();
                        return;
                    }
                    openChestOnce(chestToOpen);
                    openTimer.reset();
                } else {
                    isLootingNearby = true;
                }
            }
        }
    }
}
