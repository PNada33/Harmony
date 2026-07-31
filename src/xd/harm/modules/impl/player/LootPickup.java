package xd.harm.modules.impl.player;

import com.google.common.eventbus.Subscribe;
import xd.harm.events.network.EventPacket;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.ModeListSetting;
import xd.harm.modules.settings.impl.ModeSetting;
import xd.harm.modules.settings.impl.SliderSetting;
import net.minecraft.client.gui.NewChatGui;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.client.CPlayerPacket;
import net.minecraft.network.play.server.SPlayerPositionLookPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@ModuleRegister(name = "LootPickup", category = Category.Player, desc = "Автоматический сбор ценных предметов")
public class LootPickup extends Module {

    public final ModeSetting server = new ModeSetting("Сервер", "ЛониГриф", "ВеллМайн", "Рилик/ФанТайм", "ХолиВорлд", "ЛониГриф");
    public final ModeSetting actionMode = new ModeSetting("Мод", "Принимает", "Принимает", "Отправляет").setVisible(() -> server.is("ВеллМайн"));
    public final ModeSetting leaveMethod = new ModeSetting("Куда ливать", "Наверх", "Наверх", "/hub", "/spawn", "Никуда");
    private final SliderSetting distanceSearch = new SliderSetting("Дистанция поиска", 15.0F, 8.0F, 100.0F, 1.0F);
    private final SliderSetting checkRadius = new SliderSetting("Радиус проверки игрока", 10.0F, 1.0F, 100.0F, 1.0F).setVisible(() -> server.is("ВеллМайн") && actionMode.is("Отправляет"));
    private final SliderSetting leaveTimeout = new SliderSetting("Таймаут лива", 400.0F, 100.0F, 2000.0F, 50.0F).setVisible(() -> leaveMethod.is("/hub"));
    private final BooleanSetting ignoreFriends = new BooleanSetting("Игнорировать друзей", true).setVisible(() -> server.is("ВеллМайн") && actionMode.is("Отправляет"));
    private final BooleanSetting disableAfterLeave = new BooleanSetting("Выключать после лива", false).setVisible(() -> server.is("ВеллМайн") && actionMode.is("Отправляет"));
    private final BooleanSetting disableAfterPickup = new BooleanSetting("Выключать после подбора", false);
    private final BooleanSetting flyBack = new BooleanSetting("Лететь назад", true).setVisible(() -> leaveMethod.is("Наверх"));
    private final SliderSetting flySpeed = new SliderSetting("Скорость полета", 5.0F, 1.0F, 60.0F, 0.5F).setVisible(() -> leaveMethod.is("Наверх") && flyBack.get());

    private final ModeListSetting itemFilter = new ModeListSetting("Фильтр предметов",
            new BooleanSetting("Незеритовая броня", true),
            new BooleanSetting("Элитры", true),
            new BooleanSetting("Тотемы", true),
            new BooleanSetting("Золотые яблоки", true),
            new BooleanSetting("Зачар. зол. яблоки", true),
            new BooleanSetting("Незеритовые мечи", true),
            new BooleanSetting("Алмазная броня", false),
            new BooleanSetting("Трезубцы", false),
            new BooleanSetting("Арбалеты", false),
            new BooleanSetting("Головы игроков", false)
    );

    private long lastLootTime = 0L;
    private long startTime = 0L;
    private List<ItemStack> inventorySnapshot = new ArrayList<>();
    private boolean itemLooted = false;
    private boolean teleportedToItem = false;
    private boolean flyingBack = false;
    private Vector3d flyBackTarget = null;
    private Vector3d nextPosition = null;
    private double savedX;
    private double savedY;
    private double savedZ;
    private boolean tpAccepted = false;
    private boolean leaveSent = false;
    private long lastTeleportTime = 0L;
    private boolean waitingForPickup = false;
    private long pickupStartTime = 0L;
    private BlockPos targetBlock = null;

    private long lastActionTime = 0L;
    private boolean elytraFlying = false;
    private long jumpTime = 0L;
    private int jumpCount = 0;
    private long groundTime = 0L;
    private ItemEntity targetItem = null;
    private long lastTpaTime = 0L;
    private long lastCheckTime = 0L;
    private boolean firstTpa = true;
    private long spawnCooldownTime = 0L;

    private boolean rwftMode = false;
    private Vector3d rwftTarget = null;
    private boolean spookyTimeMode = false;
    private Vector3d spookyTimeTarget = null;
    private boolean spookyTimeDescending = false;
    private int teleportAttempts = 0;
    private long lastTeleportAttempt = 0L;
    private boolean lonyGriefMode = false;
    private Vector3d lonyGriefTarget = null;
    private boolean lonyGriefDescending = false;

    private static final long TELEPORT_COOLDOWN = 1000L;
    private static final long PICKUP_TIMEOUT = 10000L;
    private static final long LOOT_DELAY = 3000L;
    private static final long TPA_COOLDOWN = 120000L;
    private static final long SPAWN_COOLDOWN = 10000L;
    private static final long TELEPORT_INTERVAL = 30L;
    private static final double FLY_HEIGHT = 6.0D;
    private static final double LONY_SPEED = 5.0D;
    private static final double LONY_REACH = 3.0D;
    private static final double LONY_TP_STEP = 5.0D;

    public LootPickup() {
        addSettings(server, actionMode, leaveMethod, distanceSearch, checkRadius, flySpeed, leaveTimeout,
                ignoreFriends, disableAfterLeave, disableAfterPickup, flyBack, itemFilter);
    }

    @Subscribe
    private void onPacket(EventPacket e) {
        IPacket<?> packet = e.getPacket();
        if (packet instanceof SPlayerPositionLookPacket) {
            SPlayerPositionLookPacket posPacket = (SPlayerPositionLookPacket) packet;
            Set<SPlayerPositionLookPacket.Flags> flags = posPacket.getFlags();
            if (!flags.isEmpty()) {
                NewChatGui chatGUI = mc.ingameGUI.getChatGUI();
                chatGUI.printChatMessage(ITextComponent.getTextComponentOrEmpty(
                        TextFormatting.RED + "[LootPickup] " + TextFormatting.GRAY + "Тебя флагнуло! Модуль отключен."
                ));
                toggle();
            }
        }
    }

    @Subscribe
    private void onUpdate(EventUpdate e) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        if (server.is("ВеллМайн") && actionMode.is("Отправляет")) {
            handleWellmineSender();
            return;
        }

        if (!mc.player.abilities.isFlying) {
            return;
        }

        if (inventorySnapshot.isEmpty()) {
            saveInventorySnapshot();
            startTime = System.currentTimeMillis();
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastLootTime < 200L) {
            return;
        }

        handleElytraJump();

        if (flyingBack && flyBack.get()) {
            handleFlyBack();
            return;
        }

        if (waitingForPickup) {
            handlePickupWait(currentTime);
            return;
        }


        if (server.is("ЛониГриф") && lonyGriefMode) {
            handleLonyGriefMode();
            return;
        }

        if (!server.is("ХолиВорлд")) {
            BlockPos playerPos = mc.player.getPosition();
            BlockPos nearItem = findNearbyItem(playerPos, 15);

            if (nearItem != null) {
                if (server.is("Рилик/ФанТайм") && !rwftMode) {
                    startRWFTMode(nearItem);
                    return;
                }

                if (server.is("ЛониГриф") && !lonyGriefMode) {
                    startLonyGriefMode(nearItem);
                    return;
                }

                if (isWithinDistance(playerPos, nearItem, 0.5D)) {
                    checkItemPickup();
                    return;
                }

                if (isValidTeleportDistance(nearItem, playerPos)) {
                    if (server.is("ВеллМайн") && lastTeleportTime > 0L) {
                        long elapsed = currentTime - lastTeleportTime;
                        if (elapsed < 1000L) {
                            return;
                        }
                    }
                    teleportToItem(nearItem);
                    return;
                }
            }

            BlockPos farItem = findNearbyItem(playerPos, 100);
            if (farItem != null) {
                if (server.is("Рилик/ФанТайм") && !rwftMode) {
                    startRWFTMode(farItem);
                    return;
                }

                if (server.is("ЛониГриф") && !lonyGriefMode) {
                    startLonyGriefMode(farItem);
                    return;
                }

                if (isWithinDistance(playerPos, farItem, 0.5D)) {
                    checkItemPickup();
                    return;
                }

                if (isValidTeleportDistance(farItem, playerPos)) {
                    if (server.is("ВеллМайн") && lastTeleportTime > 0L) {
                        long elapsed = currentTime - lastTeleportTime;
                        if (elapsed < 1000L) {
                            return;
                        }
                    }
                    teleportToItem(farItem);
                }
            } else if (leaveMethod.is("/hub") && !itemLooted && teleportedToItem &&
                    (float)(currentTime - startTime) >= leaveTimeout.get()) {
                mc.player.sendChatMessage("/hub");
                if (disableAfterPickup.get()) {
                    toggle();
                }
            }

            if (teleportedToItem && !tpAccepted && server.is("ВеллМайн")) {
                mc.player.sendChatMessage("/tpaccept");
                tpAccepted = true;
                if (!leaveSent) {
                    executeLeave();
                    leaveSent = true;
                }
            }

            checkItemPickup();
            return;
        }

        double minX = mc.player.getPosX() - 3.0D;
        double minY = mc.player.getPosY() - 3.0D;
        double minZ = mc.player.getPosZ() - 3.0D;
        double maxX = mc.player.getPosX() + 3.0D;
        double maxY = mc.player.getPosY() + 3.0D;
        double maxZ = mc.player.getPosZ() + 3.0D;
        AxisAlignedBB searchBox = new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);

        for (Entity entity : mc.world.getEntitiesWithinAABB(ItemEntity.class, searchBox)) {
            if (entity instanceof ItemEntity) {
                ItemEntity itemEntity = (ItemEntity) entity;
                if (itemEntity.getItem().getItem() == Items.TOTEM_OF_UNDYING) {
                    mc.playerController.interactWithEntity(mc.player, itemEntity, Hand.MAIN_HAND);
                    waitingForPickup = true;
                    pickupStartTime = System.currentTimeMillis();
                    return;
                }
            }
        }

        checkItemPickup();
    }

    private void startSpookyTimeMode(BlockPos pos) {
        double x = pos.getX() + 0.5D;
        double y = pos.getY();
        double z = pos.getZ() + 0.5D;
        spookyTimeTarget = new Vector3d(x, y, z);
        spookyTimeMode = true;
        spookyTimeDescending = false;
        teleportAttempts = 0;
        lastTeleportAttempt = 0L;
    }

    private void handleSpookyTimeMode() {
        if (spookyTimeTarget == null) {
            spookyTimeMode = false;
            return;
        }

        Vector3d playerPos = mc.player.getPositionVec();
        double dx = spookyTimeTarget.x - playerPos.x;
        double dz = spookyTimeTarget.z - playerPos.z;
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        if (spookyTimeDescending) {
            double dist = playerPos.distanceTo(spookyTimeTarget);
            if (dist <= 0.5D) {
                spookyTimeMode = false;
                spookyTimeDescending = false;
                spookyTimeTarget = null;
                teleportAttempts = 0;
                lastTeleportAttempt = 0L;
                checkItemPickup();
            } else if (teleportAttempts >= 10) {
                spookyTimeMode = false;
                spookyTimeDescending = false;
                spookyTimeTarget = null;
                teleportAttempts = 0;
                lastTeleportAttempt = 0L;
                checkItemPickup();
            } else {
                long now = System.currentTimeMillis();
                if (now - lastTeleportAttempt >= TELEPORT_INTERVAL) {
                    sendTeleportPackets(spookyTimeTarget.x, spookyTimeTarget.y, spookyTimeTarget.z);
                    ++teleportAttempts;
                    lastTeleportAttempt = now;
                }
            }
        } else if (horizontalDist <= 2.0D) {
            spookyTimeDescending = true;
            teleportAttempts = 0;
        } else {
            moveHorizontallySpooky();
            double newX = mc.player.getPosX() + mc.player.getMotion().x;
            double newY = mc.player.getPosY() + mc.player.getMotion().y;
            double newZ = mc.player.getPosZ() + mc.player.getMotion().z;
            mc.player.setPosition(newX, newY, newZ);
            lookAt(spookyTimeTarget.x, spookyTimeTarget.z);
        }
    }

    private void moveHorizontallySpooky() {
        Vector3d playerPos = mc.player.getPositionVec();
        double dx = spookyTimeTarget.x - playerPos.x;
        double dz = spookyTimeTarget.z - playerPos.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        double speed = 0.3D;

        if (dist > 0.1D) {
            double normalizedX = dx / dist;
            double normalizedZ = dz / dist;
            mc.player.setMotion(normalizedX * speed, 0.0D, normalizedZ * speed);
        } else {
            mc.player.setMotion(0.0D, 0.0D, 0.0D);
        }
    }

    private void lookAt(double x, double z) {
        Vector3d eyePos = mc.player.getEyePosition(1.0F);
        double dx = x - eyePos.x;
        double dz = z - eyePos.z;
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        yaw = MathHelper.wrapDegrees(yaw);
        mc.player.rotationYaw = yaw;
        mc.player.rotationYawHead = yaw;
        mc.player.renderYawOffset = yaw;
    }

    private void sendTeleportPackets(double x, double y, double z) {
        double currentX = mc.player.getPosX();
        double currentY = mc.player.getPosY();
        double currentZ = mc.player.getPosZ();
        double dx = x - currentX;
        double dy = y - currentY;
        double dz = z - currentZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (distance < 0.1D) {
            return;
        }

        double step = 1.0D;
        int steps = (int) Math.ceil(distance / step);
        double stepX = dx / steps;
        double stepY = dy / steps;
        double stepZ = dz / steps;

        for (int i = 0; i < steps; ++i) {
            currentX += stepX;
            currentY += stepY;
            currentZ += stepZ;
            mc.player.connection.sendPacket(new CPlayerPacket.PositionPacket(currentX, currentY, currentZ, false));
        }

        mc.player.connection.sendPacket(new CPlayerPacket.PositionPacket(x, y, z, true));
    }

    private void startLonyGriefMode(BlockPos pos) {
        if (!teleportedToItem) {
            savedX = mc.player.getPosX();
            savedY = mc.player.getPosY();
            savedZ = mc.player.getPosZ();
        }

        double x = pos.getX() + 0.5D;
        double y = pos.getY();
        double z = pos.getZ() + 0.5D;
        lonyGriefTarget = new Vector3d(x, y, z);
        lonyGriefMode = true;
        lonyGriefDescending = false;
    }

    private void handleLonyGriefMode() {
        if (lonyGriefTarget == null) {
            lonyGriefMode = false;
            return;
        }

        Vector3d playerPos = mc.player.getPositionVec();
        double dx = lonyGriefTarget.x - playerPos.x;
        double dz = lonyGriefTarget.z - playerPos.z;
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        if (lonyGriefDescending) {
            double dist = playerPos.distanceTo(lonyGriefTarget);
            if (dist <= 0.5D) {
                lonyGriefMode = false;
                lonyGriefDescending = false;
                lonyGriefTarget = null;
                checkItemPickup();
            } else {
                sendLonyGriefTeleport(lonyGriefTarget.x, lonyGriefTarget.y, lonyGriefTarget.z);
                lonyGriefMode = false;
                lonyGriefDescending = false;
                lonyGriefTarget = null;
                checkItemPickup();
            }
        } else if (horizontalDist <= LONY_REACH) {
            mc.player.setMotion(0.0D, 0.0D, 0.0D);
            lonyGriefDescending = true;
            teleportedToItem = true;
        } else {
            moveLonyGrief();
            double newX = mc.player.getPosX() + mc.player.getMotion().x;
            double newY = mc.player.getPosY() + mc.player.getMotion().y;
            double newZ = mc.player.getPosZ() + mc.player.getMotion().z;
            mc.player.setPosition(newX, newY, newZ);
            lookAt(lonyGriefTarget.x, lonyGriefTarget.z);
        }
    }

    private void moveLonyGrief() {
        Vector3d playerPos = mc.player.getPositionVec();
        double dx = lonyGriefTarget.x - playerPos.x;
        double dz = lonyGriefTarget.z - playerPos.z;
        double dist = Math.sqrt(dx * dx + dz * dz);

        if (dist > 0.1D) {
            double normalizedX = dx / dist;
            double normalizedZ = dz / dist;
            mc.player.setMotion(normalizedX * LONY_SPEED, 0.0D, normalizedZ * LONY_SPEED);
        } else {
            mc.player.setMotion(0.0D, 0.0D, 0.0D);
        }
    }

    private void sendLonyGriefTeleport(double x, double y, double z) {
        double currentX = mc.player.getPosX();
        double currentY = mc.player.getPosY();
        double currentZ = mc.player.getPosZ();
        double dx = x - currentX;
        double dy = y - currentY;
        double dz = z - currentZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (distance < 0.1D) {
            return;
        }

        int steps = (int) Math.ceil(distance / LONY_TP_STEP);
        double stepX = dx / steps;
        double stepY = dy / steps;
        double stepZ = dz / steps;

        for (int i = 0; i < steps; ++i) {
            currentX += stepX;
            currentY += stepY;
            currentZ += stepZ;
            mc.player.connection.sendPacket(new CPlayerPacket.PositionPacket(currentX, currentY, currentZ, false));
        }

        mc.player.connection.sendPacket(new CPlayerPacket.PositionPacket(x, y, z, true));
        mc.player.setPosition(x, y, z);
    }

    private void startRWFTMode(BlockPos pos) {
        double x = pos.getX() + 0.5D;
        double z = pos.getZ() + 0.5D;
        double y = Math.min(mc.player.getPosY(), pos.getY() + FLY_HEIGHT);
        rwftTarget = new Vector3d(x, y, z);
        rwftMode = true;
        lastActionTime = System.currentTimeMillis();
    }

    private void handleRWFTMode() {
        if (rwftTarget == null) {
            rwftMode = false;
            return;
        }

        Vector3d playerPos = mc.player.getPositionVec();
        double dist = playerPos.distanceTo(rwftTarget);

        if (dist <= 1.0D) {
            BlockPos nearItem = findNearbyItem(mc.player.getPosition(), 100);
            if (nearItem != null) {
                teleportToItem(nearItem);
            }
            rwftMode = false;
            rwftTarget = null;
        } else {
            moveToTarget(rwftTarget);
            double newX = mc.player.getPosX() + mc.player.getMotion().x;
            double newY = mc.player.getPosY() + mc.player.getMotion().y;
            double newZ = mc.player.getPosZ() + mc.player.getMotion().z;
            Vector3d newPos = new Vector3d(newX, newY, newZ);

            if (canMoveToPosition(playerPos, newPos)) {
                mc.player.setPosition(newX, newY, newZ);
                mc.player.connection.sendPacket(new CPlayerPacket.PositionPacket(newX, newY, newZ, mc.player.isOnGround()));
            }

            smoothLookAt(rwftTarget.x, rwftTarget.y, rwftTarget.z);
        }
    }

    private void moveToTarget(Vector3d target) {
        Vector3d playerPos = mc.player.getPositionVec();
        Vector3d direction = target.subtract(playerPos).normalize();
        double distance = playerPos.distanceTo(target);
        double maxSpeed = flySpeed.get() / 20.0D;
        double rampDistance = 15.0D;
        double speed;

        if (distance <= rampDistance) {
            speed = maxSpeed * (distance / rampDistance);
        } else {
            speed = maxSpeed;
        }

        if (distance < speed) {
            speed = distance;
        }

        mc.player.setMotion(direction.x * speed, direction.y * speed, direction.z * speed);

        double nextX = mc.player.getPosX() + mc.player.getMotion().x;
        double nextY = mc.player.getPosY() + mc.player.getMotion().y;
        double nextZ = mc.player.getPosZ() + mc.player.getMotion().z;
        nextPosition = new Vector3d(nextX, nextY, nextZ);
    }

    private boolean canMoveToPosition(Vector3d from, Vector3d to) {
        double width = 0.3D;
        double height = 1.8D;
        AxisAlignedBB playerBox = new AxisAlignedBB(
                to.x - width, to.y, to.z - width,
                to.x + width, to.y + height, to.z + width
        );

        for (int x = (int) Math.floor(playerBox.minX); x <= (int) Math.floor(playerBox.maxX); x++) {
            for (int y = (int) Math.floor(playerBox.minY); y <= (int) Math.floor(playerBox.maxY); y++) {
                for (int z = (int) Math.floor(playerBox.minZ); z <= (int) Math.floor(playerBox.maxZ); z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!mc.world.getBlockState(pos).isAir()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean isValidTeleportDistance(BlockPos from, BlockPos to) {
        double dx = Math.abs(from.getX() - to.getX());
        double dy = Math.abs(from.getY() - to.getY());
        double dz = Math.abs(from.getZ() - to.getZ());
        double verticalDiff = to.getY() - from.getY();
        return dx <= 100.0D && dy <= 100.0D && dz <= 100.0D;
    }

    private boolean isWithinDistance(BlockPos pos1, BlockPos pos2, double distance) {
        double dx = pos1.getX() - pos2.getX();
        double dy = pos1.getY() - pos2.getY();
        double dz = pos1.getZ() - pos2.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz) <= distance;
    }

    private BlockPos findNearbyItem(BlockPos center, int radius) {
        List<Item> targetItems = new ArrayList<>();

        if ((Boolean) itemFilter.getValueByName("Незеритовая броня").get()) {
            targetItems.add(Items.NETHERITE_HELMET);
            targetItems.add(Items.NETHERITE_CHESTPLATE);
            targetItems.add(Items.NETHERITE_LEGGINGS);
            targetItems.add(Items.NETHERITE_BOOTS);
        }
        if ((Boolean) itemFilter.getValueByName("Элитры").get()) {
            targetItems.add(Items.ELYTRA);
        }
        if ((Boolean) itemFilter.getValueByName("Тотемы").get()) {
            targetItems.add(Items.TOTEM_OF_UNDYING);
        }
        if ((Boolean) itemFilter.getValueByName("Золотые яблоки").get()) {
            targetItems.add(Items.GOLDEN_APPLE);
        }
        if ((Boolean) itemFilter.getValueByName("Зачар. зол. яблоки").get()) {
            targetItems.add(Items.ENCHANTED_GOLDEN_APPLE);
        }
        if ((Boolean) itemFilter.getValueByName("Незеритовые мечи").get()) {
            targetItems.add(Items.NETHERITE_SWORD);
        }
        if ((Boolean) itemFilter.getValueByName("Алмазная броня").get()) {
            targetItems.add(Items.DIAMOND_HELMET);
            targetItems.add(Items.DIAMOND_CHESTPLATE);
            targetItems.add(Items.DIAMOND_LEGGINGS);
            targetItems.add(Items.DIAMOND_BOOTS);
        }
        if ((Boolean) itemFilter.getValueByName("Трезубцы").get()) {
            targetItems.add(Items.TRIDENT);
        }
        if ((Boolean) itemFilter.getValueByName("Арбалеты").get()) {
            targetItems.add(Items.CROSSBOW);
        }
        if ((Boolean) itemFilter.getValueByName("Головы игроков").get()) {
            targetItems.add(Items.PLAYER_HEAD);
        }

        if (targetItems.isEmpty()) {
            return null;
        }

        AxisAlignedBB searchBox = new AxisAlignedBB(
                center.getX() - radius, center.getY() - radius, center.getZ() - radius,
                center.getX() + radius, center.getY() + radius, center.getZ() + radius
        );

        for (ItemEntity itemEntity : mc.world.getEntitiesWithinAABB(ItemEntity.class, searchBox)) {
            Item item = itemEntity.getItem().getItem();
            if (targetItems.contains(item)) {
                BlockPos itemPos = itemEntity.getPosition();
                double distance = Math.sqrt(center.distanceSq(itemPos));
                if (distance > 6.0D || System.currentTimeMillis() - lastLootTime >= LOOT_DELAY) {
                    return itemPos;
                }
            }
        }

        return null;
    }

    private void teleportWellmine(double x, double y, double z) {
        double currentX = mc.player.getPosX();
        double currentY = mc.player.getPosY();
        double currentZ = mc.player.getPosZ();
        double dx = x - currentX;
        double dy = y - currentY;
        double dz = z - currentZ;
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);

        if (horizontalDist > 69.0D || Math.abs(dy) > 69.0D) {
            return;
        }

        mc.player.connection.sendPacket(new CPlayerPacket.PositionPacket(x, y, z, false));
        mc.player.setPosition(x, y, z);
    }

    private void teleportRWFT(double x, double y, double z) {
        double currentX = mc.player.getPosX();
        double currentY = mc.player.getPosY();
        double currentZ = mc.player.getPosZ();
        double dx = x - currentX;
        double dy = y - currentY;
        double dz = z - currentZ;
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (horizontalDist > 69.0D || Math.abs(dy) > 69.0D) {
            return;
        }

        int steps = (int) Math.ceil(distance / 1.0D);
        double stepX = dx / steps;
        double stepY = dy / steps;
        double stepZ = dz / steps;

        for (int i = 0; i < steps; ++i) {
            currentX += stepX;
            currentY += stepY;
            currentZ += stepZ;
            mc.player.connection.sendPacket(new CPlayerPacket.PositionPacket(currentX, currentY, currentZ, false));
            mc.player.setPosition(currentX, currentY, currentZ);
        }

        mc.player.connection.sendPacket(new CPlayerPacket.PositionPacket(x, y, z, false));
        mc.player.setPosition(x, y, z);
    }

    private void teleportHolyWorld(double x, double y, double z) {
        double currentX = mc.player.getPosX();
        double currentY = mc.player.getPosY();
        double currentZ = mc.player.getPosZ();
        double dx = x - currentX;
        double dy = y - currentY;
        double dz = z - currentZ;
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

        if (distance < 0.1D) {
            return;
        }

        double step = 7.0D;
        int steps = (int) Math.ceil(distance / step);
        double stepX = dx / steps;
        double stepY = dy / steps;
        double stepZ = dz / steps;

        for (int i = 0; i < steps; ++i) {
            currentX += stepX;
            currentY += stepY;
            currentZ += stepZ;
            mc.player.connection.sendPacket(new CPlayerPacket.PositionPacket(currentX, currentY, currentZ, false));
        }

        mc.player.connection.sendPacket(new CPlayerPacket.PositionPacket(x, y, z, true));
        mc.player.setPosition(x, y, z);
    }

    private void teleportToItem(BlockPos pos) {
        if (server.is("Рилик/ФанТайм")) {
            rwftMode = false;
            rwftTarget = null;
        }

        savedX = mc.player.getPosX();
        savedY = mc.player.getPosY();
        savedZ = mc.player.getPosZ();

        if (server.is("ВеллМайн")) {
            teleportWellmine(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        } else if (server.is("Рилик/ФанТайм")) {
            teleportRWFT(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        } else if (server.is("ХолиВорлд")) {
            teleportHolyWorld(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        }

        teleportedToItem = true;
        lastLootTime = System.currentTimeMillis();
        startTime = System.currentTimeMillis();
        tpAccepted = false;
        leaveSent = false;
    }

    private void handleElytraJump() {
        if (mc.player.isOnGround()) {
            if (groundTime == 0L) {
                groundTime = System.currentTimeMillis();
            } else if (System.currentTimeMillis() - groundTime >= 50L) {
                elytraFlying = true;
                jumpCount = 0;
            }
        } else {
            groundTime = 0L;
        }

        if (mc.player.abilities.isFlying) {
            if (lastActionTime == 0L) {
                lastActionTime = System.currentTimeMillis();
            }
        } else {
            lastActionTime = 0L;
        }

        if (elytraFlying && jumpCount < 2 && System.currentTimeMillis() - jumpTime >= 70L) {
            mc.gameSettings.keyBindJump.setPressed(true);
            jumpTime = System.currentTimeMillis();
            ++jumpCount;
            if (jumpCount >= 2) {
                elytraFlying = false;
            }
        }
    }

    private void saveInventorySnapshot() {
        inventorySnapshot.clear();
        for (int i = 0; i < mc.player.inventory.getSizeInventory(); i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            inventorySnapshot.add(stack.copy());
        }
    }

    private void checkItemPickup() {
        if (itemLooted) {
            return;
        }

        for (int i = 0; i < mc.player.inventory.getSizeInventory(); i++) {
            ItemStack current = mc.player.inventory.getStackInSlot(i);
            ItemStack previous = i < inventorySnapshot.size() ? inventorySnapshot.get(i) : ItemStack.EMPTY;

            if (!ItemStack.areItemStacksEqual(current, previous) && !current.isEmpty()) {
                if (waitingForPickup) {
                    waitingForPickup = false;
                    targetBlock = null;
                }
                onItemLooted();
                return;
            }
        }
    }

    private void executeLeave() {
        if (!teleportedToItem) {
            return;
        }

        switch (leaveMethod.get()) {
            case "Наверх":
                if (flyBack.get()) {
                    flyingBack = true;
                    if (server.is("ЛониГриф")) {
                        flyBackTarget = new Vector3d(mc.player.getPosX(), savedY, mc.player.getPosZ());
                    } else {
                        flyBackTarget = new Vector3d(savedX, savedY, savedZ);
                    }
                    nextPosition = mc.player.getPositionVec();
                } else if (server.is("ВеллМайн")) {
                    teleportWellmine(savedX, savedY, savedZ);
                } else if (server.is("Рилик/ФанТайм")) {
                    teleportRWFT(savedX, savedY, savedZ);
                } else if (server.is("ЛониГриф")) {
                    sendLonyGriefTeleport(mc.player.getPosX(), savedY, mc.player.getPosZ());
                }
                break;
            case "/hub":
                mc.player.sendChatMessage("/hub");
                break;
            case "/spawn":
                mc.player.sendChatMessage("/spawn");
                break;
            case "Никуда":
                break;
        }

        if (server.is("ВеллМайн")) {
            lastTeleportTime = System.currentTimeMillis();
        }
    }

    private void onItemLooted() {
        itemLooted = true;

        if (server.is("ВеллМайн") && leaveSent) {
            if (disableAfterPickup.get()) {
                toggle();
            }
            return;
        }

        if (teleportedToItem && !leaveSent) {
            executeLeave();
            leaveSent = true;
        }

        if (disableAfterPickup.get()) {
            toggle();
        }
    }

    private void handleFlyBack() {
        if (flyBackTarget == null) {
            flyingBack = false;
            return;
        }

        Vector3d playerPos = mc.player.getPositionVec();
        double dist = playerPos.distanceTo(flyBackTarget);

        if (dist < 1.0D) {
            flyingBack = false;
            flyBackTarget = null;
            nextPosition = null;
        } else {
            moveToTarget(flyBackTarget);

            if (nextPosition != null && canMoveToPosition(playerPos, nextPosition)) {
                mc.player.setPosition(nextPosition.x, nextPosition.y, nextPosition.z);
            } else {
                flyingBack = false;
                flyBackTarget = null;
                nextPosition = null;
            }
        }
    }

    private void handlePickupWait(long currentTime) {
        if (currentTime - pickupStartTime >= PICKUP_TIMEOUT) {
            waitingForPickup = false;
            targetBlock = null;
        } else {
            checkItemPickup();
        }
    }

    private void handleWellmineSender() {
        long currentTime = System.currentTimeMillis();

        if (spawnCooldownTime > 0L) {
            long elapsed = currentTime - spawnCooldownTime;
            if (elapsed < SPAWN_COOLDOWN) {
                return;
            }
            spawnCooldownTime = 0L;
        }

        if (currentTime - lastCheckTime >= 100L) {
            lastCheckTime = currentTime;

            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player != mc.player) {
                    double distance = mc.player.getDistance(player);
                    if (distance <= checkRadius.get()) {
                        mc.player.sendChatMessage("/spawn");
                        spawnCooldownTime = currentTime;
                        if (disableAfterLeave.get()) {
                            toggle();
                        }
                        return;
                    }
                }
            }
        }

        if (firstTpa || (lastTpaTime > 0L && currentTime - lastTpaTime >= TPA_COOLDOWN)) {
            firstTpa = false;
            lastTpaTime = currentTime;
        }
    }

    private void smoothLookAt(double x, double y, double z) {
        float[] rotation = calculateRotation(x, y, z);
        float currentYaw = mc.player.rotationYaw;
        float currentPitch = mc.player.rotationPitch;
        float yawDiff = MathHelper.wrapDegrees(rotation[0] - currentYaw);
        float pitchDiff = MathHelper.wrapDegrees(rotation[1] - currentPitch);
        double distance = getDistance(x, y, z);
        float rotationSpeed = Math.max(1.5F, (float) (8.0D - distance / 15.0D));

        float newYaw;
        if (Math.abs(yawDiff) > rotationSpeed) {
            newYaw = currentYaw + (yawDiff > 0.0F ? rotationSpeed : -rotationSpeed);
        } else {
            newYaw = rotation[0];
        }

        float newPitch;
        if (Math.abs(pitchDiff) > rotationSpeed) {
            newPitch = currentPitch + (pitchDiff > 0.0F ? rotationSpeed : -rotationSpeed);
        } else {
            newPitch = rotation[1];
        }

        mc.player.rotationYaw = newYaw;
        mc.player.rotationPitch = newPitch;
        mc.player.rotationYawHead = newYaw;
        mc.player.renderYawOffset = newYaw;
    }

    private float[] calculateRotation(double x, double y, double z) {
        Vector3d eyePos = mc.player.getEyePosition(1.0F);
        Vector3d target = new Vector3d(x, y, z);
        double dx = target.x - eyePos.x;
        double dy = target.y - eyePos.y;
        double dz = target.z - eyePos.z;
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        yaw = MathHelper.wrapDegrees(yaw);
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        float pitch = (float) (-Math.toDegrees(Math.atan2(dy, horizontalDist)));
        pitch = MathHelper.clamp(pitch, -90.0F, 90.0F);
        return new float[]{yaw, pitch};
    }

    private double getDistance(double x, double y, double z) {
        double dx = mc.player.getPosX() - x;
        double dy = mc.player.getPosY() - y;
        double dz = mc.player.getPosZ() - z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    @Override
    public boolean onEnable() {
        super.onEnable();

        resetState();
        return false;
    }

    @Override
    public boolean onDisable() {
        super.onDisable();

        if (mc.player != null) {
            mc.player.setMotion(0.0, 0.0, 0.0);
        }

        resetState();
        return false;
    }

    private void resetState() {
        itemLooted = false;
        teleportedToItem = false;
        flyingBack = false;
        rwftMode = false;
        spookyTimeMode = false;
        lonyGriefMode = false;
        waitingForPickup = false;
        targetBlock = null;
        flyBackTarget = null;
        nextPosition = null;
        rwftTarget = null;
        spookyTimeTarget = null;
        spookyTimeDescending = false;
        teleportAttempts = 0;
        lastTeleportAttempt = 0L;
        lonyGriefTarget = null;
        lonyGriefDescending = false;
        targetItem = null;
        tpAccepted = false;
        leaveSent = false;
        lastTeleportTime = 0L;
        inventorySnapshot.clear();
        startTime = System.currentTimeMillis();
        lastTpaTime = 0L;
        lastCheckTime = 0L;
        firstTpa = true;
        spawnCooldownTime = 0L;
        elytraFlying = false;
        jumpCount = 0;
        lastActionTime = 0L;
        groundTime = 0L;
    }
}
