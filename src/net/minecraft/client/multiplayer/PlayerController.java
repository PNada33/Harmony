package net.minecraft.client.multiplayer;

import com.mojang.datafixers.util.Pair;
import xd.harm.Harmony;
import xd.harm.chesttracker.HarmonyChestTrackerBootstrap;
import xd.harm.events.combat.AttackEvent;
import xd.harm.events.combat.EventClickBlockRight;
import xd.harm.modules.api.ModuleManager;
import xd.harm.modules.impl.combat.HitAura;
import xd.harm.modules.impl.misc.NoInteract;
import xd.harm.baritone.utils.accessor.IPlayerControllerMP;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.block.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.util.ClientRecipeBook;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.horse.AbstractHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.fluid.FluidState;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.network.play.client.*;
import net.minecraft.stats.StatisticsManager;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.GameType;
import net.minecraft.world.World;

public class PlayerController implements IPlayerControllerMP {

    private final Minecraft mc;
    private final ClientPlayNetHandler connection;
    private BlockPos currentBlock = new BlockPos(-1, -1, -1);
    private ItemStack currentItemHittingBlock = ItemStack.EMPTY;
    public float curBlockDamageMP;
    private float stepSoundTickCounter;
    public int blockHitDelay;
    private boolean isHittingBlock;
    private GameType currentGameType = GameType.SURVIVAL;
    private GameType field_239166_k_ = GameType.NOT_SET;
    private final Object2ObjectLinkedOpenHashMap<Pair<BlockPos, CPlayerDiggingPacket.Action>, Vector3d> unacknowledgedDiggingPackets = new Object2ObjectLinkedOpenHashMap<>();
    private int currentPlayerItem;
    private final AttackEvent event = new AttackEvent(null);

    public PlayerController(Minecraft mcIn, ClientPlayNetHandler netHandler) {
        this.mc = mcIn;
        this.connection = netHandler;
    }

    public void setPlayerCapabilities(PlayerEntity player) {
        this.currentGameType.configurePlayerCapabilities(player.abilities);
    }

    public void func_241675_a_(GameType type) {
        this.field_239166_k_ = type;
    }

    public void setGameType(GameType type) {
        if (type != this.currentGameType) {
            this.field_239166_k_ = this.currentGameType;
        }
        this.currentGameType = type;
        this.currentGameType.configurePlayerCapabilities(this.mc.player.abilities);
    }

    public boolean shouldDrawHUD() {
        return this.currentGameType.isSurvivalOrAdventure();
    }

    public boolean onPlayerDestroyBlock(BlockPos pos) {
        if (this.mc.player.blockActionRestricted(this.mc.world, pos, this.currentGameType)) {
            return false;
        }

        World world = this.mc.world;
        BlockState blockstate = world.getBlockState(pos);

        if (!this.mc.player.getHeldItemMainhand().getItem().canPlayerBreakBlockWhileHolding(blockstate, world, pos, this.mc.player)) {
            return false;
        }

        Block block = blockstate.getBlock();

        if ((block instanceof CommandBlockBlock || block instanceof StructureBlock || block instanceof JigsawBlock) && !this.mc.player.canUseCommandBlock()) {
            return false;
        }

        if (blockstate.isAir()) {
            return false;
        }

        block.onBlockHarvested(world, pos, blockstate, this.mc.player);
        FluidState fluidstate = world.getFluidState(pos);
        boolean flag = world.setBlockState(pos, fluidstate.getBlockState(), 11);

        if (flag) {
            block.onPlayerDestroy(world, pos, blockstate);
        }

        return flag;
    }

    public boolean clickBlock(BlockPos loc, Direction face) {
        if (this.mc.player.blockActionRestricted(this.mc.world, loc, this.currentGameType)) {
            return false;
        }

        if (!this.mc.world.getWorldBorder().contains(loc)) {
            return false;
        }

        if (this.currentGameType.isCreative()) {
            BlockState blockstate = this.mc.world.getBlockState(loc);
            this.mc.getTutorial().onHitBlock(this.mc.world, loc, blockstate, 1.0F);
            this.sendDiggingPacket(CPlayerDiggingPacket.Action.START_DESTROY_BLOCK, loc, face);
            this.onPlayerDestroyBlock(loc);
            this.blockHitDelay = 5;
        } else if (!this.isHittingBlock || !this.isHittingPosition(loc)) {
            if (this.isHittingBlock) {
                this.sendDiggingPacket(CPlayerDiggingPacket.Action.ABORT_DESTROY_BLOCK, this.currentBlock, face);
            }

            BlockState blockstate1 = this.mc.world.getBlockState(loc);
            this.mc.getTutorial().onHitBlock(this.mc.world, loc, blockstate1, 0.0F);
            this.sendDiggingPacket(CPlayerDiggingPacket.Action.START_DESTROY_BLOCK, loc, face);

            boolean flag = !blockstate1.isAir();

            if (flag && this.curBlockDamageMP == 0.0F) {
                blockstate1.onBlockClicked(this.mc.world, loc, this.mc.player);
            }

            if (flag && blockstate1.getPlayerRelativeBlockHardness(this.mc.player, this.mc.player.world, loc) >= 1.0F) {
                this.onPlayerDestroyBlock(loc);
            } else {
                this.isHittingBlock = true;
                this.currentBlock = loc;
                this.currentItemHittingBlock = this.mc.player.getHeldItemMainhand();
                this.curBlockDamageMP = 0.0F;
                this.stepSoundTickCounter = 0.0F;
                this.mc.world.sendBlockBreakProgress(this.mc.player.getEntityId(), this.currentBlock, (int) (this.curBlockDamageMP * 10.0F) - 1);
            }
        }

        return true;
    }

    public void resetBlockRemoving() {
        if (this.isHittingBlock) {
            BlockState blockstate = this.mc.world.getBlockState(this.currentBlock);
            this.mc.getTutorial().onHitBlock(this.mc.world, this.currentBlock, blockstate, -1.0F);
            this.sendDiggingPacket(CPlayerDiggingPacket.Action.ABORT_DESTROY_BLOCK, this.currentBlock, Direction.DOWN);
            this.isHittingBlock = false;
            this.curBlockDamageMP = 0.0F;
            this.mc.world.sendBlockBreakProgress(this.mc.player.getEntityId(), this.currentBlock, -1);
            this.mc.player.resetCooldown();
        }
    }

    public boolean onPlayerDamageBlock(BlockPos posBlock, Direction directionFacing) {
        this.syncCurrentPlayItem();

        if (this.blockHitDelay > 0) {
            --this.blockHitDelay;
            return true;
        }

        if (this.currentGameType.isCreative() && this.mc.world.getWorldBorder().contains(posBlock)) {
            this.blockHitDelay = 5;
            BlockState blockstate1 = this.mc.world.getBlockState(posBlock);
            this.mc.getTutorial().onHitBlock(this.mc.world, posBlock, blockstate1, 1.0F);
            this.sendDiggingPacket(CPlayerDiggingPacket.Action.START_DESTROY_BLOCK, posBlock, directionFacing);
            this.onPlayerDestroyBlock(posBlock);
            return true;
        }

        if (this.isHittingPosition(posBlock)) {
            BlockState blockstate = this.mc.world.getBlockState(posBlock);

            if (blockstate.isAir()) {
                this.isHittingBlock = false;
                return false;
            }

            this.curBlockDamageMP += blockstate.getPlayerRelativeBlockHardness(this.mc.player, this.mc.player.world, posBlock);

            if (this.stepSoundTickCounter % 4.0F == 0.0F) {
                SoundType soundtype = blockstate.getSoundType();
                this.mc.getSoundHandler().play(new SimpleSound(soundtype.getHitSound(), SoundCategory.BLOCKS, (soundtype.getVolume() + 1.0F) / 8.0F, soundtype.getPitch() * 0.5F, posBlock));
            }

            ++this.stepSoundTickCounter;
            this.mc.getTutorial().onHitBlock(this.mc.world, posBlock, blockstate, MathHelper.clamp(this.curBlockDamageMP, 0.0F, 1.0F));

            if (this.curBlockDamageMP >= 1.0F) {
                this.isHittingBlock = false;
                this.sendDiggingPacket(CPlayerDiggingPacket.Action.STOP_DESTROY_BLOCK, posBlock, directionFacing);
                this.onPlayerDestroyBlock(posBlock);
                this.curBlockDamageMP = 0.0F;
                this.stepSoundTickCounter = 0.0F;
                this.blockHitDelay = 5;
            }

            this.mc.world.sendBlockBreakProgress(this.mc.player.getEntityId(), this.currentBlock, (int) (this.curBlockDamageMP * 10.0F) - 1);
            return true;
        }

        return this.clickBlock(posBlock, directionFacing);
    }

    public float getBlockReachDistance() {
        return this.currentGameType.isCreative() ? 5.0F : 4.5F;
    }

    public void tick() {
        this.syncCurrentPlayItem();
        if (this.connection.getNetworkManager().isChannelOpen()) {
            this.connection.getNetworkManager().tick();
        } else {
            this.connection.getNetworkManager().handleDisconnection();
        }
    }

    private boolean isHittingPosition(BlockPos pos) {
        ItemStack itemstack = this.mc.player.getHeldItemMainhand();
        boolean flag = this.currentItemHittingBlock.isEmpty() && itemstack.isEmpty();

        if (!this.currentItemHittingBlock.isEmpty() && !itemstack.isEmpty()) {
            flag = itemstack.getItem() == this.currentItemHittingBlock.getItem()
                    && ItemStack.areItemStackTagsEqual(itemstack, this.currentItemHittingBlock)
                    && (itemstack.isDamageable() || itemstack.getDamage() == this.currentItemHittingBlock.getDamage());
        }

        return pos.equals(this.currentBlock) && flag;
    }

    public void syncCurrentPlayItem() {
        int i = this.mc.player.inventory.currentItem;
        if (i != this.currentPlayerItem) {
            this.currentPlayerItem = i;
            this.connection.sendPacket(new CHeldItemChangePacket(this.currentPlayerItem));
        }
    }

    public ActionResultType processRightClickBlock(ClientPlayerEntity player, ClientWorld world, Hand hand, BlockRayTraceResult rayTrace) {
        this.syncCurrentPlayItem();
        BlockPos blockpos = rayTrace.getPos();

        if (!this.mc.world.getWorldBorder().contains(blockpos)) {
            return ActionResultType.FAIL;
        }

        HarmonyChestTrackerBootstrap.handleUseBlock(world, rayTrace);
        ActionResultType fabricResult = UseBlockCallback.EVENT.invoker().interact(player, world, hand, rayTrace);

        if (fabricResult != ActionResultType.PASS) {
            return fabricResult;
        }

        EventClickBlockRight clickBlockRightEvent = new EventClickBlockRight(player, world, hand, rayTrace);
        Harmony.getInstance().getEventBus().post(clickBlockRightEvent);
        if (clickBlockRightEvent.isCancel()) {
            return ActionResultType.FAIL;
        }

        ItemStack itemstack = player.getHeldItem(hand);

        if (this.currentGameType == GameType.SPECTATOR) {
            this.connection.sendPacket(new CPlayerTryUseItemOnBlockPacket(hand, rayTrace));
            return ActionResultType.SUCCESS;
        }

        ModuleManager moduleManager = Harmony.getInstance().getModuleManager();
        HitAura hitAura = moduleManager.getHitAura();
        NoInteract noInteract = moduleManager.getNoInteract();

        boolean flag = !player.getHeldItemMainhand().isEmpty() || !player.getHeldItemOffhand().isEmpty();
        boolean flag1 = player.isSecondaryUseActive() && flag;
        boolean flag2 = (noInteract.isState() && noInteract.allBlocks.get())
                || (noInteract.isState() && noInteract.getBlocks().contains(world.getBlockState(rayTrace.getPos()).getBlockId()))
                || hitAura.getTarget() != null;

        if (!flag2) {
            if (!flag1) {
                ActionResultType actionresulttype = world.getBlockState(blockpos).onBlockActivated(world, player, hand, rayTrace);
                if (actionresulttype.isSuccessOrConsume()) {
                    this.connection.sendPacket(new CPlayerTryUseItemOnBlockPacket(hand, rayTrace));
                    return actionresulttype;
                }
            }
            this.connection.sendPacket(new CPlayerTryUseItemOnBlockPacket(hand, rayTrace));
        }

        if (!itemstack.isEmpty() && !player.getCooldownTracker().hasCooldown(itemstack.getItem())) {
            ItemUseContext itemusecontext = new ItemUseContext(player, hand, rayTrace);
            ActionResultType actionresulttype1;

            if (this.currentGameType.isCreative()) {
                int i = itemstack.getCount();
                actionresulttype1 = itemstack.onItemUse(itemusecontext);
                itemstack.setCount(i);
            } else {
                actionresulttype1 = itemstack.onItemUse(itemusecontext);
            }

            return actionresulttype1;
        }

        return ActionResultType.PASS;
    }

    public ActionResultType rightClickBlock(ClientPlayerEntity player, ClientWorld world, Hand hand, BlockRayTraceResult rayTraceResult) {
        return processRightClickBlock(player, world, hand, rayTraceResult);
    }

    public ActionResultType processRightClick(PlayerEntity player, World worldIn, Hand hand) {
        ActionResult<ItemStack> fabricResult = UseItemCallback.EVENT.invoker().interact(player, worldIn, hand);

        if (fabricResult.getType() != ActionResultType.PASS) {
            return fabricResult.getType();
        }

        if (this.currentGameType == GameType.SPECTATOR) {
            return ActionResultType.PASS;
        }

        this.syncCurrentPlayItem();
        this.connection.sendPacket(new CPlayerTryUseItemPacket(hand));

        ItemStack itemstack = player.getHeldItem(hand);

        if (player.getCooldownTracker().hasCooldown(itemstack.getItem())) {
            return ActionResultType.PASS;
        }

        ActionResult<ItemStack> actionresult = itemstack.useItemRightClick(worldIn, player, hand);
        ItemStack itemstack1 = actionresult.getResult();

        if (itemstack1 != itemstack) {
            player.setHeldItem(hand, itemstack1);
        }

        return actionresult.getType();
    }

    public ClientPlayerEntity createPlayer(ClientWorld worldIn, StatisticsManager statsManager, ClientRecipeBook recipes) {
        return this.func_239167_a_(worldIn, statsManager, recipes, false, false);
    }

    public ClientPlayerEntity func_239167_a_(ClientWorld world, StatisticsManager stats, ClientRecipeBook recipes, boolean lastSneaking, boolean lastSprinting) {
        return new ClientPlayerEntity(this.mc, world, this.connection, stats, recipes, lastSneaking, lastSprinting);
    }

    public void attackEntity(PlayerEntity playerIn, Entity targetEntity) {
        event.entity = targetEntity;
        Harmony.getInstance().getEventBus().post(event);
        this.syncCurrentPlayItem();
        this.connection.sendPacket(new CUseEntityPacket(targetEntity, playerIn.isSneaking()));

        if (this.currentGameType != GameType.SPECTATOR) {
            playerIn.attackTargetEntityWithCurrentItem(targetEntity);
            playerIn.resetCooldown();
        }
    }

    public ActionResultType interactWithEntity(PlayerEntity player, Entity target, Hand hand) {
        ActionResultType fabricResult = UseEntityCallback.EVENT.invoker().interact(player, target.world, hand, target, null);

        if (fabricResult != ActionResultType.PASS) {
            return fabricResult;
        }

        this.syncCurrentPlayItem();
        this.connection.sendPacket(new CUseEntityPacket(target, hand, player.isSneaking()));
        return this.currentGameType == GameType.SPECTATOR ? ActionResultType.PASS : player.interactOn(target, hand);
    }

    public ActionResultType interactWithEntity(PlayerEntity player, Entity target, EntityRayTraceResult ray, Hand hand) {
        ActionResultType fabricResult = UseEntityCallback.EVENT.invoker().interact(player, target.world, hand, target, ray);

        if (fabricResult != ActionResultType.PASS) {
            return fabricResult;
        }

        this.syncCurrentPlayItem();
        Vector3d vector3d = ray.getHitVec().subtract(target.getPosX(), target.getPosY(), target.getPosZ());
        this.connection.sendPacket(new CUseEntityPacket(target, hand, vector3d, player.isSneaking()));
        return this.currentGameType == GameType.SPECTATOR ? ActionResultType.PASS : target.applyPlayerInteraction(player, vector3d, hand);
    }

    public ItemStack windowClick(int windowId, int slotId, int mouseButton, ClickType type, PlayerEntity player) {
        short transactionId = player.openContainer.getNextTransactionID(player.inventory);
        ItemStack itemstack = player.openContainer.slotClick(slotId, mouseButton, type, player);
        this.connection.sendPacket(new CClickWindowPacket(windowId, slotId, mouseButton, type, itemstack, transactionId));
        return itemstack;
    }

    public void windowClickFixed(int windowId, int slotId, int mouseButton, ClickType type, PlayerEntity player, int timeWait) {
        mc.player.windowClickMemory.add(new ClientPlayerEntity.WindowClickMemory(windowId, slotId, mouseButton, type, player, timeWait));
    }

    public void sendPlaceRecipePacket(int containerId, IRecipe<?> recipe, boolean shiftDown) {
        this.connection.sendPacket(new CPlaceRecipePacket(containerId, recipe, shiftDown));
    }

    public void sendEnchantPacket(int windowID, int button) {
        this.connection.sendPacket(new CEnchantItemPacket(windowID, button));
    }

    public void sendSlotPacket(ItemStack itemStackIn, int slotId) {
        if (this.currentGameType.isCreative()) {
            this.connection.sendPacket(new CCreativeInventoryActionPacket(slotId, itemStackIn));
        }
    }

    public void sendPacketDropItem(ItemStack itemStackIn) {
        if (this.currentGameType.isCreative() && !itemStackIn.isEmpty()) {
            this.connection.sendPacket(new CCreativeInventoryActionPacket(-1, itemStackIn));
        }
    }

    public void onStoppedUsingItem(PlayerEntity playerIn) {
        this.syncCurrentPlayItem();
        this.connection.sendPacket(new CPlayerDiggingPacket(CPlayerDiggingPacket.Action.RELEASE_USE_ITEM, BlockPos.ZERO, Direction.DOWN));
        playerIn.stopActiveHand();
    }

    public boolean gameIsSurvivalOrAdventure() {
        return this.currentGameType.isSurvivalOrAdventure();
    }

    public boolean isNotCreative() {
        return !this.currentGameType.isCreative();
    }

    public boolean isInCreativeMode() {
        return this.currentGameType.isCreative();
    }

    public boolean extendedReach() {
        return this.currentGameType.isCreative();
    }

    public boolean isRidingHorse() {
        return this.mc.player.isPassenger() && this.mc.player.getRidingEntity() instanceof AbstractHorseEntity;
    }

    public boolean isSpectatorMode() {
        return this.currentGameType == GameType.SPECTATOR;
    }

    public GameType func_241822_k() {
        return this.field_239166_k_;
    }

    public GameType getCurrentGameType() {
        return this.currentGameType;
    }

    public boolean getIsHittingBlock() {
        return this.isHittingBlock;
    }

    public void pickItem(int index) {
        this.connection.sendPacket(new CPickItemPacket(index));
    }

    private void sendDiggingPacket(CPlayerDiggingPacket.Action action, BlockPos pos, Direction dir) {
        ClientPlayerEntity clientplayerentity = this.mc.player;
        this.unacknowledgedDiggingPackets.put(Pair.of(pos, action), clientplayerentity.getPositionVec());
        this.connection.sendPacket(new CPlayerDiggingPacket(action, pos, dir));
    }

    public void acknowledgePlayerDiggingReceived(ClientWorld worldIn, BlockPos pos, BlockState blockIn, CPlayerDiggingPacket.Action action, boolean successful) {
    }

    public void processRightClickBlock(ClientPlayerEntity player, ClientWorld world, BlockPos blockInFront, Direction horizontalFacing, Object o, Object o1) {
    }

    @Override
    public void setIsHittingBlock(boolean isHittingBlock) {
        this.isHittingBlock = isHittingBlock;
    }

    @Override
    public BlockPos getCurrentBlock() {
        return currentBlock;
    }

    @Override
    public void callSyncCurrentPlayItem() {
        syncCurrentPlayItem();
    }
}
