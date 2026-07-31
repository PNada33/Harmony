package xd.harm.modules.impl.combat;

import com.google.common.eventbus.Subscribe;
import xd.harm.events.input.EventInput;
import xd.harm.events.network.EventPacket;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.ModeListSetting;
import xd.harm.modules.settings.impl.SliderSetting;
import xd.harm.utils.player.InventoryUtil;
import net.minecraft.block.Blocks;
import net.minecraft.entity.item.EnderCrystalEntity;
import net.minecraft.entity.item.TNTEntity;
import net.minecraft.entity.item.minecart.TNTMinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.play.server.SEntityStatusPacket;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.List;

@ModuleRegister(name = "AutoTotem", category = Category.Combat, desc = "берёт тотем при определённых условиях")
public class AutoTotem extends Module {

    private final SliderSetting healthThreshold = new SliderSetting("Порог здоровья", 4.5F, 1.0F, 20.0F, 0.5F);

    private final ModeListSetting settings = new ModeListSetting("Настройки",
            new BooleanSetting("Сохранять зачарованные", false),
            new BooleanSetting("Возвращать предмет", true),
            new BooleanSetting("Не брать если шар", false),
            new BooleanSetting("Не брать если ешь", false));

    private final ModeListSetting modes = new ModeListSetting("Учитывать",
            new BooleanSetting("Поглощение", true),
            new BooleanSetting("Здоровье на элитре", true),
            new BooleanSetting("Падение", true),
            new BooleanSetting("Кристаллы", true),
            new BooleanSetting("Обсидиан", true),
            new BooleanSetting("ТНТ", true),
            new BooleanSetting("Якорь", false));

    private final SliderSetting elytraHealth = new SliderSetting("Здоровье на элитре", 8.5F, 1.0F, 20.0F, 0.5F)
            .setVisible(() -> modes.get(MODE_ELYTRA_HEALTH).get());

    private final SliderSetting crystalDistance = new SliderSetting("Дистанция кристалла", 4.0F, 1.0F, 6.0F, 0.5F)
            .setVisible(() -> modes.get(MODE_CRYSTALS).get());

    private final SliderSetting obsidianDistance = new SliderSetting("Дистанция обсидиана", 4.0F, 1.0F, 6.0F, 0.5F)
            .setVisible(() -> modes.get(MODE_OBSIDIAN).get());

    private final SliderSetting tntDistance = new SliderSetting("Дистанция ТНТ", 10.0F, 1.0F, 50.0F, 1.0F)
            .setVisible(() -> modes.get(MODE_TNT).get());

    private int oldSlot = -1;
    private ItemStack backItemStack = ItemStack.EMPTY;
    private boolean totemIsUsed = false;
    private long lastTotemUseTime = 0;
    private boolean swappingTotem = false;
    private long lastSwapAttempt = 0;
    private long lastSuccessfulSwap = 0;
    private int verifyTicksRemaining = -1;
    private int autoStopTicks = 0;
    private boolean savedEnchantedTotem = false;
    private boolean returningSavedItem = false;
    private long lastReturnAttempt = 0;
    private int returnAttempts = 0;
    private static final long RETURN_RETRY_DELAY_MS = 90L;
    private static final int MAX_RETURN_ATTEMPTS = 3;
    private static final int SETTING_SAVE_ENCHANTED = 0;
    private static final int SETTING_RETURN_ITEM = 1;
    private static final int SETTING_NO_SKULL = 2;
    private static final int SETTING_NO_EAT = 3;
    private static final int MODE_ABSORPTION = 0;
    private static final int MODE_ELYTRA_HEALTH = 1;
    private static final int MODE_FALL = 2;
    private static final int MODE_CRYSTALS = 3;
    private static final int MODE_OBSIDIAN = 4;
    private static final int MODE_TNT = 5;
    private static final int MODE_ANCHOR = 6;

    public AutoTotem() {
        addSettings(settings, modes, healthThreshold, elytraHealth, crystalDistance, obsidianDistance, tntDistance);
    }

    @Subscribe
    public void onEvent(EventUpdate event) {
        if (mc.player == null || mc.world == null || mc.playerController == null) {
            return;
        }

        if (autoStopTicks > 0) {
            autoStopTicks--;
        }

        long currentTime = System.currentTimeMillis();

        if (verifyTicksRemaining > 0) {
            verifyTicksRemaining--;
        } else if (verifyTicksRemaining == 0) {
            verifyTicksRemaining = -1;
            if (isTotemInOffhand() && !mc.player.getHeldItemOffhand().isEnchanted()) {
                lastSuccessfulSwap = currentTime;
            }
            swappingTotem = false;
        }

        float health = mc.player.getHealth();
        float effectiveHealth = health;

        if (modes.get(MODE_ABSORPTION).get()) {
            effectiveHealth += mc.player.getAbsorptionAmount();
        }

        boolean shouldSwap = effectiveHealth <= (Float) healthThreshold.get()
                || (totemIsUsed && getTotemCount() > 0 && currentTime - lastTotemUseTime >= 500);

        if (modes.get(MODE_ELYTRA_HEALTH).get()
                && mc.player.isElytraFlying() && health <= (Float) elytraHealth.get()) {
            shouldSwap = true;
        }

        if (modes.get(MODE_FALL).get() && mc.player.fallDistance > 10) {
            shouldSwap = true;
        }

        if (modes.get(MODE_CRYSTALS).get()) {
            double dist = getClosestCrystalDistance();
            if (dist <= (Float) crystalDistance.get()) {
                if (settings.get(SETTING_NO_SKULL).get() && isHoldingSkull()) {
                    shouldSwap = effectiveHealth <= (Float) healthThreshold.get();
                } else {
                    shouldSwap = true;
                }
            }
        }

        if (modes.get(MODE_OBSIDIAN).get()) {
            double dist = getClosestObsidianDistance();
            if (dist <= (Float) obsidianDistance.get()) {
                if (settings.get(SETTING_NO_SKULL).get() && isHoldingSkull()) {
                    shouldSwap = effectiveHealth <= (Float) healthThreshold.get();
                } else {
                    shouldSwap = true;
                }
            }
        }

        if (modes.get(MODE_TNT).get()) {
            double dist = getClosestTntDistance();
            if (dist <= (Float) tntDistance.get()) {
                shouldSwap = true;
            }
        }

        if (modes.get(MODE_ANCHOR).get()) {
            if (hasRespawnAnchorNearby()) {
                shouldSwap = true;
            }
        }

        if (settings.get(SETTING_NO_EAT).get()
                && mc.player.getItemInUseCount() > 0
                && mc.player.getActiveItemStack().getItem().isFood()) {
            shouldSwap = false;
        }

        ItemStack offhandStack = mc.player.getHeldItemOffhand();
        boolean isTotemInOffhand = isTotemInOffhand();
        boolean isEnchantedTotemInOffhand = offhandStack.getItem() == Items.TOTEM_OF_UNDYING
                && offhandStack.isEnchanted();

        boolean hasNormalTotem = isTotemInOffhand && !isEnchantedTotemInOffhand;

        if (hasNormalTotem && !shouldSwap) {
            swappingTotem = false;
        }

        if (returningSavedItem && handlePendingReturn(shouldSwap)) {
            return;
        }

        boolean shouldReturn = !shouldSwap && oldSlot != -1 && !backItemStack.isEmpty();
        if (shouldReturn && tryReturnSavedItem()) {
            return;
        }

        if (shouldSwap && (!hasNormalTotem || isEnchantedTotemInOffhand)) {
            if (hasNormalTotem && !isEnchantedTotemInOffhand) {
                swappingTotem = false;
                return;
            }

            if (swappingTotem) {
                return;
            }

            if (currentTime - lastSwapAttempt < 100 || currentTime - lastSuccessfulSwap < 200) {
                return;
            }

            if (mc.player.getCooldownTracker().hasCooldown(Items.TOTEM_OF_UNDYING)) {
                return;
            }

            int totemSlot = findTotemSlot();
            if (totemSlot == -1) {
                return;
            }

            if (isTotemInOffhand) {
                ItemStack currentOffhand = mc.player.getHeldItemOffhand();
                ItemStack foundTotem = getStackAtSlot(totemSlot);

                if (currentOffhand.getItem() == foundTotem.getItem()) {
                    boolean currentEnchanted = currentOffhand.isEnchanted();
                    boolean foundEnchanted = foundTotem.isEnchanted();

                    if (currentEnchanted == foundEnchanted) {
                        return;
                    }
                    if (!currentEnchanted && foundEnchanted) {
                        return;
                    }
                }
            }

            lastSwapAttempt = currentTime;
            swappingTotem = true;

            if (!offhandStack.isEmpty() && oldSlot == -1
                    && settings.get(SETTING_RETURN_ITEM).get()) {
                oldSlot = totemSlot;
                backItemStack = offhandStack.copy();
                savedEnchantedTotem = isEnchantedTotem(backItemStack);
            }

            doSwap(totemSlot);
            totemIsUsed = false;

            verifyTicksRemaining = 2;
        } else if (swappingTotem && hasNormalTotem) {
            swappingTotem = false;
        }
    }

    @Subscribe
    public void onEvent(EventPacket eventPacket) {
        if (eventPacket.isReceive()) {
            if (eventPacket.getPacket() instanceof SEntityStatusPacket) {
                SEntityStatusPacket statusPacket = (SEntityStatusPacket) eventPacket.getPacket();
                if (statusPacket.getOpCode() == 35 && statusPacket.getEntity(mc.world) == mc.player) {
                    totemIsUsed = true;
                    lastTotemUseTime = System.currentTimeMillis();
                }
            }
        }
    }

    private void doSwap(int slot) {
        autoStopTicks = 4;
        InventoryUtil.swapToOffhand(slot);
    }

    @Subscribe
    private void onMoveInput(EventInput e) {
        InventoryUtil.handleOffhandSwapStop(e);
        if (swappingTotem || autoStopTicks > 0 || InventoryUtil.isOffhandSwapInProgress()) {
            e.setForward(0.0F);
            e.setStrafe(0.0F);
        }
    }

    private boolean isHoldingSkull() {
        ItemStack mainHand = mc.player.getHeldItemMainhand();
        ItemStack offHand = mc.player.getHeldItemOffhand();
        return isSkull(mainHand) || isSkull(offHand);
    }

    private boolean isSkull(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return stack.getItem() == Items.SKELETON_SKULL
                || stack.getItem() == Items.WITHER_SKELETON_SKULL
                || stack.getItem() == Items.ZOMBIE_HEAD
                || stack.getItem() == Items.PLAYER_HEAD
                || stack.getItem() == Items.CREEPER_HEAD
                || stack.getItem() == Items.DRAGON_HEAD;
    }

    private double getClosestCrystalDistance() {
        double maxDist = (Float) crystalDistance.get();
        AxisAlignedBB bb = mc.player.getBoundingBox().grow(maxDist);
        List<EnderCrystalEntity> crystals = mc.world.getEntitiesWithinAABB(EnderCrystalEntity.class, bb);
        double minDist = Double.MAX_VALUE;
        for (EnderCrystalEntity crystal : crystals) {
            double dist = mc.player.getDistance(crystal);
            if (dist < minDist) {
                minDist = dist;
            }
        }
        return minDist;
    }

    private double getClosestObsidianDistance() {
        double minDist = Double.MAX_VALUE;
        BlockPos playerPos = mc.player.getPosition();
        int dist = (int) Math.ceil((Float) obsidianDistance.get());
        for (int x = -dist; x <= dist; x++) {
            for (int y = -dist; y <= dist; y++) {
                for (int z = -dist; z <= dist; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    if (mc.world.getBlockState(pos).getBlock() == Blocks.OBSIDIAN) {
                        double d = MathHelper.sqrt((float) playerPos.distanceSq(pos));
                        if (d < minDist) {
                            minDist = d;
                        }
                    }
                }
            }
        }
        return minDist;
    }

    private double getClosestTntDistance() {
        double maxDist = (Float) tntDistance.get();
        AxisAlignedBB bb = mc.player.getBoundingBox().grow(maxDist);
        List<TNTEntity> tntEntities = mc.world.getEntitiesWithinAABB(TNTEntity.class, bb);
        double minDist = Double.MAX_VALUE;
        for (TNTEntity tnt : tntEntities) {
            double dist = mc.player.getDistance(tnt);
            if (dist < minDist) {
                minDist = dist;
            }
        }
        List<TNTMinecartEntity> tntMinecarts = mc.world.getEntitiesWithinAABB(TNTMinecartEntity.class, bb);
        for (TNTMinecartEntity cart : tntMinecarts) {
            double dist = mc.player.getDistance(cart);
            if (dist < minDist) {
                minDist = dist;
            }
        }
        return minDist;
    }

    private boolean hasRespawnAnchorNearby() {
        BlockPos playerPos = mc.player.getPosition();
        int dist = 6;
        for (int x = -dist; x <= dist; x++) {
            for (int y = -dist; y <= dist; y++) {
                for (int z = -dist; z <= dist; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    if (mc.world.getBlockState(pos).getBlock() == Blocks.RESPAWN_ANCHOR) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isTotemInOffhand() {
        return mc.player.getHeldItemOffhand().getItem() == Items.TOTEM_OF_UNDYING;
    }

    private boolean canReturnSavedItem() {
        return settings.get(SETTING_RETURN_ITEM).get();
    }

    private boolean isEnchantedTotem(ItemStack stack) {
        return !stack.isEmpty()
                && stack.getItem() == Items.TOTEM_OF_UNDYING
                && stack.isEnchanted();
    }

    private boolean tryReturnSavedItem() {
        if (oldSlot == -1 || backItemStack.isEmpty()) {
            return false;
        }

        if (!canReturnSavedItem()) {
            clearReturnState();
            return false;
        }

        if (mc.player.getHeldItemOffhand().getItem() == Items.TOTEM_OF_UNDYING) {
            int returnSlot = oldSlot;
            swappingTotem = false;
            doSwap(returnSlot);
            returningSavedItem = true;
            returnAttempts = 1;
            lastReturnAttempt = System.currentTimeMillis();
            return true;
        }

        clearReturnState();
        return false;
    }

    private boolean handlePendingReturn(boolean shouldSwap) {
        if (oldSlot == -1 || backItemStack.isEmpty()) {
            clearReturnState();
            return false;
        }

        if (shouldSwap) {
            returningSavedItem = false;
            return false;
        }

        ItemStack offhandStack = mc.player.getHeldItemOffhand();
        if (isReturnedSavedItem(offhandStack)
                || (backItemStack.getItem() != Items.TOTEM_OF_UNDYING
                && offhandStack.getItem() != Items.TOTEM_OF_UNDYING)) {
            clearReturnState();
            totemIsUsed = false;
            return true;
        }

        long currentTime = System.currentTimeMillis();
        if (returnAttempts < MAX_RETURN_ATTEMPTS && currentTime - lastReturnAttempt >= RETURN_RETRY_DELAY_MS) {
            int returnSlot = oldSlot;
            doSwap(returnSlot);
            returnAttempts++;
            lastReturnAttempt = currentTime;
            return true;
        }

        if (returnAttempts >= MAX_RETURN_ATTEMPTS && currentTime - lastReturnAttempt >= 180L) {
            clearReturnState();
            totemIsUsed = false;
            return false;
        }

        return true;
    }

    private boolean isReturnedSavedItem(ItemStack stack) {
        if (stack.isEmpty() || backItemStack.isEmpty()) {
            return false;
        }
        if (stack.getItem() != backItemStack.getItem()) {
            return false;
        }
        if (stack.isEnchanted() != backItemStack.isEnchanted()) {
            return false;
        }
        return ItemStack.areItemStackTagsEqual(stack, backItemStack);
    }

    private void clearReturnState() {
        oldSlot = -1;
        backItemStack = ItemStack.EMPTY;
        savedEnchantedTotem = false;
        returningSavedItem = false;
        lastReturnAttempt = 0;
        returnAttempts = 0;
    }

    private int findTotemSlot() {
        boolean saveEnchanted = settings.get(SETTING_SAVE_ENCHANTED).get();
        if (saveEnchanted) {
            int nonEnchantedSlot = -1;
            int enchantedSlot = -1;
            for (int i = 0; i < 36; i++) {
                ItemStack stack = mc.player.inventory.getStackInSlot(i);
                if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                    int containerSlot = i < 9 ? i + 36 : i;
                    if (!stack.isEnchanted()) {
                        nonEnchantedSlot = containerSlot;
                    } else {
                        enchantedSlot = containerSlot;
                    }
                }
            }
            if (nonEnchantedSlot != -1) return nonEnchantedSlot;
            if (enchantedSlot != -1) return enchantedSlot;
        } else {
            for (int i = 0; i < 36; i++) {
                ItemStack stack = mc.player.inventory.getStackInSlot(i);
                if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
                    return i < 9 ? i + 36 : i;
                }
            }
        }
        return -1;
    }

    private int getTotemCount() {
        int count = 0;
        for (int i = 0; i < 36; i++) {
            if (mc.player.inventory.getStackInSlot(i).getItem() == Items.TOTEM_OF_UNDYING) {
                count++;
            }
        }
        return count;
    }

    private ItemStack getStackAtSlot(int containerSlot) {
        if (containerSlot >= 36 && containerSlot <= 44) {
            return mc.player.inventory.getStackInSlot(containerSlot - 36);
        } else if (containerSlot >= 9 && containerSlot <= 35) {
            return mc.player.inventory.getStackInSlot(containerSlot);
        } else if (containerSlot == 45) {
            return mc.player.getHeldItemOffhand();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean onDisable() {
        clearReturnState();
        totemIsUsed = false;
        lastTotemUseTime = 0;
        swappingTotem = false;
        lastSwapAttempt = 0;
        lastSuccessfulSwap = 0;
        verifyTicksRemaining = -1;
        autoStopTicks = 0;
        returningSavedItem = false;
        lastReturnAttempt = 0;
        returnAttempts = 0;
        super.onDisable();
        return false;
    }

    public ModeListSetting getMode() {
        return modes;
    }
}
