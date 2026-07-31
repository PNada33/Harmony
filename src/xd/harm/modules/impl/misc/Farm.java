package xd.harm.modules.impl.misc;

import com.google.common.eventbus.Subscribe;
import net.minecraft.client.Minecraft;
import xd.harm.events.world.EventUpdate;
import xd.harm.events.network.EventPacket;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.ModeSetting;
import xd.harm.modules.settings.impl.SliderSetting;
import xd.harm.modules.settings.impl.ModeListSetting;
import xd.harm.utils.client.InvUtil;
import xd.harm.utils.client.TimerUtility;
import xd.harm.utils.math.StopWatch;
import xd.harm.utils.player.MoveUtils;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.gui.screen.inventory.ChestScreen;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.client.CAnimateHandPacket;
import net.minecraft.network.play.client.CPlayerDiggingPacket;
import net.minecraft.network.play.client.CPlayerTryUseItemOnBlockPacket;
import net.minecraft.network.play.client.CPlayerTryUseItemPacket;
import net.minecraft.network.play.server.SPlaySoundEffectPacket;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

@ModuleRegister(name = "AutoFarm", category = Category.Misc, desc = "Чо сам не понимаешь?")
public class Farm extends Module {

    private final ModeSetting farmMode = new ModeSetting("Тип фарма", "", "AutoFarmFT", "ExpBottleFill", "AutoFarmRW", "AutoFish");

    private final TimerUtility suspiciousTimer = new TimerUtility();
    private long lastMessageTime = 0L;
    private final StopWatch stopWatchMain = new StopWatch();
    private final StopWatch stopWatch = new StopWatch();
    private boolean repair, exp;
    private boolean isEating;
    private ModeSetting motyga = new ModeSetting("Вид мотыги", "Незер", "Незер", "Алмазная").setVisible(() -> farmMode.is("AutoFarmFT"));
    public final BooleanSetting clan = new BooleanSetting("Класть деньги в клан", false).setVisible(() -> farmMode.is("AutoFarmFT"));
    public final BooleanSetting kushat = new BooleanSetting("Анти Голод", false).setVisible(() -> farmMode.is("AutoFarmFT"));
    public final BooleanSetting camerablat = new BooleanSetting("Поварачивать Камеру", false).setVisible(() -> farmMode.is("AutoFarmFT"));
    private Item motygaitem;

    private boolean cmd = false;
    private boolean eblan228 = false;
    private final SliderSetting eblan227 = new SliderSetting("Уровней для заполнения", 15, 15, 50, 15).setVisible(() -> farmMode.is("ExpBottleFill"));

    private final Set<BlockPos> brokenBlocks = new HashSet<>();
    private final Map<BlockPos, Long> blockBreakingTimes = new HashMap<>();
    private final ModeListSetting elements = new ModeListSetting(
            "Что ломать?",
            new BooleanSetting("Арбуз", true),
            new BooleanSetting("Тыква", true),
            new BooleanSetting("Пшено", true),
            new BooleanSetting("Картофель", true),
            new BooleanSetting("Свекла", true),
            new BooleanSetting("Морковь", true),
            new BooleanSetting("Тростник", true),
            new BooleanSetting("Ягоды", true),
            new BooleanSetting("Трава", false),
            new BooleanSetting("Дерево", false),
            new BooleanSetting("Листва", false),
            new BooleanSetting("Кактусы", false),
            new BooleanSetting("Семена Тыковки", false),
            new BooleanSetting("Семена Арбуза", false),
            new BooleanSetting("Нарост", false),
            new BooleanSetting("Ламинария", false)
    ).setVisible(() -> farmMode.is("AutoFarmRW"));
    private final BooleanSetting rightClickBerries = new BooleanSetting("ПКМ на ягоды", true).setVisible(() -> farmMode.is("AutoFarmRW"));
    private boolean running = false;
    private Thread nukerThread;
    private BlockPos currentBreakingBlock = null;

    private final SliderSetting reactionSpeed = new SliderSetting("Скорость реакции", 100.0F, 10.0F, 610.0F, 10.0F).setVisible(() -> farmMode.is("AutoFish"));
    private final BooleanSetting swingArm = new BooleanSetting("Махать рукой", false).setVisible(() -> farmMode.is("AutoFish"));
    private final ModeSetting swingMode = new ModeSetting("Режим маха", "Normal", "Normal", "Packet").setVisible(() -> farmMode.is("AutoFish") && swingArm.get());
    private final BooleanSetting stopIfBreak = new BooleanSetting("Остановить если сломается", false).setVisible(() -> farmMode.is("AutoFish"));

    private final TimerUtility fishDelay = new TimerUtility();
    private boolean isHooked = false;
    private boolean needToHook = false;
    private long react;

    public Farm() {
        addSettings(farmMode, motyga, clan, kushat, camerablat, eblan227, elements, rightClickBerries,
                reactionSpeed, swingArm, swingMode, stopIfBreak);
    }

    @Override
    public boolean onEnable() {
        super.onEnable();
        repair = false;
        exp = false;
        cmd = false;
        eblan228 = false;

        isHooked = false;
        needToHook = false;
        if (farmMode.is("AutoFish")) {
            mc.player.closeScreen();
            mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
        }

        if (farmMode.is("AutoFarmRW")) {
            this.running = true;
            this.nukerThread = new Thread(() -> {
                while (this.running) {
                    if (mc == null || mc.player == null || mc.world == null) continue;
                    this.nukeBlocks();
                    this.clearBrokenBlocks();
                    try {
                        Thread.sleep(20L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
            this.nukerThread.start();
        }
        return false;
    }

    @Override
    public boolean onDisable() {
        cmd = false;
        eblan228 = false;
        this.running = false;
        if (this.nukerThread != null) {
            this.nukerThread.interrupt();
        }

        isHooked = false;
        needToHook = false;

        return super.onDisable();
    }

    @Subscribe
    public void onUpdate(EventUpdate e) {
        if (farmMode.is("AutoFarmFT")) {
            handleAutoFarm();
        } else if (farmMode.is("ExpBottleFill")) {
            handleExpBottleFill();
        } else if (farmMode.is("AutoFish")) {
            handleAutoFish();
        }
    }

    @Subscribe
    public void onPacket(EventPacket event) {
        if (!farmMode.is("AutoFish")) return;

        IPacket packet = event.getPacket();
        if (packet instanceof SPlaySoundEffectPacket) {
            SPlaySoundEffectPacket soundPacket = (SPlaySoundEffectPacket) packet;
            if (soundPacket.getSound().getName().getPath().equals("entity.fishing_bobber.splash") &&
                    mc.player.fishingBobber != null &&
                    mc.player.fishingBobber.getDistanceSq(soundPacket.getX(), soundPacket.getY(), soundPacket.getZ()) < 4.0F) {
                isHooked = true;
                fishDelay.reset();
            }
        }
    }

    private void handleAutoFish() {
        if (isFishingRodInHand(mc.player.getHeldItemMainhand()) || isFishingRodInHand(mc.player.getHeldItemOffhand())) {
            if (stopIfBreak.get()) {
                float durability = getDurabilityPercentage(mc.player.getHeldItemMainhand());
                if (durability <= 8.0F) {
                    print("Низкая прочность, остановка.");
                    toggle();
                    return;
                }
            }

            react = reactionSpeed.get().longValue();

            if (fishDelay.isReached(react) && isHooked) {
                mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
                if (swingArm.get()) {
                    if (swingMode.is("Normal")) {
                        mc.player.swingArm(Hand.MAIN_HAND);
                    } else {
                        mc.player.connection.sendPacket(new CAnimateHandPacket(Hand.MAIN_HAND));
                    }
                }
                isHooked = false;
                needToHook = true;
                fishDelay.reset();
            }

            if (fishDelay.isReached(react + 50L) && needToHook) {
                mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.MAIN_HAND));
                if (swingArm.get()) {
                    if (swingMode.is("Normal")) {
                        mc.player.swingArm(Hand.MAIN_HAND);
                    } else {
                        mc.player.connection.sendPacket(new CAnimateHandPacket(Hand.MAIN_HAND));
                    }
                }
                needToHook = false;
                fishDelay.reset();
            }
        }
    }

    private boolean isFishingRodInHand(ItemStack stack) {
        return stack.getItem() == Items.FISHING_ROD;
    }

    private float getDurabilityPercentage(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0.0F;
        }
        int damage = stack.getDamage();
        int maxDamage = stack.getMaxDamage();
        return (1.0F - (float) damage / maxDamage) * 100.0F;
    }

    private void handleAutoFarm() {
        List<Item> landingItems = List.of(Items.POTATO, Items.CARROT, Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS);
        Slot hoeSlot;
        if (motyga.is("Незер")) {
            hoeSlot = InvUtil.getInventorySlot(Items.NETHERITE_HOE);
            motygaitem = Items.NETHERITE_HOE;
        } else {
            hoeSlot = InvUtil.getInventorySlot(Items.DIAMOND_HOE);
            motygaitem = Items.DIAMOND_HOE;
        }
        Slot expSlot = InvUtil.getInventorySlot(Items.EXPERIENCE_BOTTLE);
        Slot landingSlot = InvUtil.getInventorySlot(landingItems);
        int expCount = InvUtil.getInventoryCount(Items.EXPERIENCE_BOTTLE);
        Item mainHandItem = mc.player.getHeldItemMainhand().getItem();
        Item offHandItem = mc.player.getHeldItemOffhand().getItem();

        if (hoeSlot == null || MoveUtils.isMoving() || !stopWatchMain.isReached(500)) return;

        float itemStrength = 1 - MathHelper.clamp((float) hoeSlot.getStack().getDamage() / (float) hoeSlot.getStack().getMaxDamage(), 0, 1);

        if (itemStrength < 0.04) {
            repair = true;
        } else if (repair && itemStrength > 0.85) {
            repair = false;
            stopWatchMain.reset();
            exp = false;
            return;
        }
        exp = expCount >= 55 || expCount != 0 && exp;
        if (camerablat.get())
            mc.player.rotationPitch = 90;

        if (mc.player.inventory.getFirstEmptyStack() == -1) {
            if (!landingItems.contains(offHandItem)) {
                InvUtil.clickSlot(landingSlot, 40, ClickType.SWAP, false);
                return;
            }
            if (mc.currentScreen instanceof ContainerScreen<?> screen) {
                if (screen.getTitle().getString().equals("в—Џ Выберите секцию")) {
                    InvUtil.clickSlotId(21, 0, ClickType.PICKUP, true);
                    return;
                }
                if (screen.getTitle().getString().equals("Скупщик еды")) {
                    int slotIdSell = offHandItem.equals(Items.CARROT) ? 10 : offHandItem.equals(Items.POTATO) ? 11 : offHandItem.equals(Items.BEETROOT_SEEDS) ? 12 : 14;
                    InvUtil.clickSlotId(slotIdSell, 0, ClickType.PICKUP, true);
                    return;
                }
            }
            if (stopWatch.isReached(1000)) {
                mc.player.sendChatMessage("/buyer");
                stopWatch.reset();
            }
        } else if (repair) {
            if (exp) {
                if (mc.currentScreen instanceof ContainerScreen<?>) {
                    mc.player.closeScreen();
                    stopWatchMain.reset();
                    if (stopWatch.isReached(100)) ;
                } else if (mainHandItem.equals(motygaitem) && offHandItem.equals(Items.EXPERIENCE_BOTTLE)) {
                    mc.player.connection.sendPacket(new CPlayerTryUseItemPacket(Hand.OFF_HAND));
                } else {
                    if (!offHandItem.equals(Items.EXPERIENCE_BOTTLE)) {
                        if (stopWatch.isReached(100)) ;
                        InvUtil.clickSlot(expSlot, 40, ClickType.SWAP, false);
                        if (stopWatch.isReached(100)) ;
                    }
                    if (!mainHandItem.equals(motygaitem)) {
                        if (stopWatch.isReached(100)) ;
                        InvUtil.clickSlot(hoeSlot, mc.player.inventory.currentItem, ClickType.SWAP, false);
                    }
                }
            } else if (stopWatch.isReached(800)) {
                if (mc.currentScreen instanceof ContainerScreen<?> screen) {
                    if (screen.getTitle().getString().contains("Пузырек опыта")) {
                        mc.player.openContainer.inventorySlots.stream().filter(s -> s.getStack().getTag() != null && s.slotNumber < 45)
                                .min(Comparator.comparingInt(s -> AutoBuyexcellent.getPrice(s.getStack()) / s.getStack().getCount()))
                                .ifPresent(s -> InvUtil.clickSlot(s, 0, ClickType.QUICK_MOVE, true));
                        stopWatch.reset();
                        return;
                    } else if (screen.getTitle().getString().contains("Подозрительная цена")) {
                        if (stopWatch.isReached(500)) {
                            InvUtil.clickSlotId(0, 0, ClickType.QUICK_MOVE, true);
                            suspiciousTimer.reset();
                        }
                        return;
                    }
                }
                mc.player.sendChatMessage("/ah search Пузырёк Опыта");
                stopWatch.reset();
            }
        } else {
            BlockPos pos = mc.player.getPosition();
            if (mc.world.getBlockState(pos).getBlock().equals(Blocks.FARMLAND)) {
                if (mainHandItem.equals(motygaitem) && landingItems.contains(offHandItem)) {
                    mc.player.connection.sendPacket(new CPlayerTryUseItemOnBlockPacket(Hand.OFF_HAND, new BlockRayTraceResult(mc.player.getPositionVec(), Direction.UP, pos, false)));
                    mc.player.connection.sendPacket(new CPlayerTryUseItemOnBlockPacket(Hand.MAIN_HAND, new BlockRayTraceResult(mc.player.getPositionVec(), Direction.UP, pos.up(), false)));
                    mc.player.connection.sendPacket(new CPlayerTryUseItemOnBlockPacket(Hand.MAIN_HAND, new BlockRayTraceResult(mc.player.getPositionVec(), Direction.UP, pos.up(), false)));
                    mc.player.connection.sendPacket(new CPlayerTryUseItemOnBlockPacket(Hand.MAIN_HAND, new BlockRayTraceResult(mc.player.getPositionVec(), Direction.UP, pos.up(), false)));
                    mc.player.connection.sendPacket(new CPlayerDiggingPacket(CPlayerDiggingPacket.Action.START_DESTROY_BLOCK, pos.up(), Direction.UP));
                } else {
                    if (mc.currentScreen instanceof ContainerScreen<?>) {
                        mc.player.closeScreen();
                        stopWatchMain.reset();
                        return;
                    }
                    if (!mainHandItem.equals(motygaitem)) {
                        InvUtil.clickSlot(hoeSlot, mc.player.inventory.currentItem, ClickType.SWAP, false);
                    }
                    if (!landingItems.contains(offHandItem)) {
                        InvUtil.clickSlot(landingSlot, 40, ClickType.SWAP, false);
                    }
                    if (clan.get())
                        if (System.currentTimeMillis() - this.lastMessageTime >= 190000L) {
                            Minecraft.player.sendChatMessage("/clan invest 1000000");
                            this.lastMessageTime = System.currentTimeMillis();
                        }

                    if (kushat.get())
                        if (mc.player.getFoodStats().getFoodLevel() < 10) {
                            startEating();
                        } else if (mc.player.getFoodStats().getFoodLevel() > 20) {
                            stopEating();
                        }
                }
            }
        }
    }

    private void handleExpBottleFill() {
        if (mc.player == null || mc.world == null) return;

        try {
        } catch (Exception ignored) {}

        if (mc.player.experienceLevel < 15) {
            if (!eblan228) {
                print("У вас нет лвла для заполнения пузырькоф");
                eblan228 = true;
            }
            if (mc.currentScreen instanceof ChestScreen) {
                mc.player.closeScreen();
            }
            return;
        } else {
            eblan228 = false;
        }

        if (mc.currentScreen instanceof ChestScreen chestScreen) {
            int level = Math.round(eblan227.get());
            String search = level + " Уров";
            for (int i = 0; i < chestScreen.getContainer().inventorySlots.size(); ++i) {
                ItemStack stack = chestScreen.getContainer().getSlot(i).getStack();
                String name = stack.getDisplayName().getString();
                if (name.contains(search) || stack.getItem() == Items.DRAGON_BREATH) {
                    mc.playerController.windowClick(chestScreen.getContainer().windowId, i, 0, ClickType.PICKUP, mc.player);
                    break;
                }
            }
        }

        if (!cmd) {
            mc.player.sendChatMessage("/exp");
            cmd = true;
        }
    }

    private void clearBrokenBlocks() {
        long currentTime = System.currentTimeMillis();
        brokenBlocks.removeIf(pos -> currentTime - blockBreakingTimes.getOrDefault(pos, currentTime) >= 2000);
    }

    private double getDistanceSquared(BlockPos pos1, BlockPos pos2) {
        double dx = pos1.getX() - pos2.getX();
        double dy = pos1.getY() - pos2.getY();
        double dz = pos1.getZ() - pos2.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private void nukeBlocks() {
        if (mc != null && mc.world != null && mc.player != null) {
            BlockPos playerPos = new BlockPos(mc.player.getPosition());
            int rangeValue = 3;
            int heightValue = 3;
            int heightValue1 = -4;
            List<BlockPos> blockPositions = new ArrayList<>();

            for (int x = -rangeValue; x <= rangeValue; x++) {
                for (int y = heightValue1; y <= heightValue; y++) {
                    for (int z = -rangeValue; z <= rangeValue; z++) {
                        BlockPos blockPos = playerPos.add(x, y, z);
                        Block block = mc.world.getBlockState(blockPos).getBlock();

                        if (((Boolean) this.elements.getValueByName("Арбуз").get()) && block == Blocks.MELON) {
                            blockPositions.add(blockPos);
                        }
                        if (((Boolean) this.elements.getValueByName("Тыква").get()) && block == Blocks.PUMPKIN) {
                            blockPositions.add(blockPos);
                        }
                        if (((Boolean) this.elements.getValueByName("Пшено").get()) && block == Blocks.WHEAT) {
                            blockPositions.add(blockPos);
                        }
                        if (((Boolean) this.elements.getValueByName("Картофель").get()) && block == Blocks.POTATOES) {
                            blockPositions.add(blockPos);
                        }
                        if (((Boolean) this.elements.getValueByName("Свекла").get()) && block == Blocks.BEETROOTS) {
                            blockPositions.add(blockPos);
                        }
                        if (((Boolean) this.elements.getValueByName("Морковь").get()) && block == Blocks.CARROTS) {
                            blockPositions.add(blockPos);
                        }
                        if (((Boolean) this.elements.getValueByName("Тростник").get()) && block == Blocks.SUGAR_CANE) {
                            blockPositions.add(blockPos);
                        }
                        if (((Boolean) this.elements.getValueByName("Ягоды").get()) && block == Blocks.SWEET_BERRY_BUSH) {
                            if ((Boolean) this.rightClickBerries.get()) {
                                mc.player.connection.sendPacket(
                                        new CPlayerTryUseItemOnBlockPacket(
                                                Hand.MAIN_HAND,
                                                new BlockRayTraceResult(new Vector3d(blockPos.getX(), blockPos.getY(), blockPos.getZ()), Direction.UP, blockPos, true)
                                        )
                                );
                            } else {
                                blockPositions.add(blockPos);
                            }
                        }
                    }
                }
            }

            Set<BlockPos> var10001 = this.brokenBlocks;
            Objects.requireNonNull(var10001);
            blockPositions.removeIf(var10001::contains);
            blockPositions.sort(Comparator.comparingDouble(pos -> this.getDistanceSquared((BlockPos) pos, playerPos)));
            if (!blockPositions.isEmpty()) {
                BlockPos blockToBreak = (BlockPos) blockPositions.get(0);
                if (this.currentBreakingBlock == null || !this.blockBreakingTimes.containsKey(this.currentBreakingBlock)) {
                    try {
                        if (!this.brokenBlocks.contains(blockToBreak)) {
                            mc.playerController.onPlayerDamageBlock(blockToBreak, mc.player.getHorizontalFacing());
                            this.blockBreakingTimes.put(blockToBreak, System.currentTimeMillis());
                        }
                    } catch (Exception sex) {
                        Exception e = sex;
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    class AutoBuyexcellent {
        public static int getPrice(ItemStack itemStack) {
            CompoundNBT tag = itemStack.getTag();
            if (tag == null) return -1;
            String price = StringUtils.substringBetween(tag.toString(), "\"text\":\" $", "\"}]");
            if (price == null || price.isEmpty()) return -1;
            price = price.replaceAll(" ", "").replaceAll(",", "");
            return Integer.parseInt(price);
        }
    }

    private void stopEating() {
        mc.gameSettings.keyBindUseItem.setPressed(false);
        isEating = false;
    }

    private void startEating() {
        if (mc.currentScreen != null) {
            mc.currentScreen.passEvents = true;
        }
        if (!mc.gameSettings.keyBindUseItem.isKeyDown()) {
            mc.gameSettings.keyBindUseItem.setPressed(true);
            isEating = true;
        }
    }
}
