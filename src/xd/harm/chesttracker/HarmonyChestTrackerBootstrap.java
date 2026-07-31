package xd.harm.chesttracker;

import com.mojang.blaze3d.matrix.MatrixStack;
import io.github.cottonmc.cotton.gui.client.LibGuiClient;
import io.github.cottonmc.cotton.gui.impl.LibGuiCommon;
import io.github.cottonmc.cotton.gui.impl.ScreenNetworkingImpl;
import me.shedaniel.cloth.api.client.events.v0.ClothClientHooks;
import me.shedaniel.cloth.api.client.events.v0.ScreenHooks;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ShulkerBoxTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.world.World;
import red.jackf.chesttracker.ChestTracker;
import red.jackf.chesttracker.memory.Memory;
import red.jackf.chesttracker.memory.MemoryDatabase;
import red.jackf.chesttracker.memory.MemoryUtils;
import red.jackf.whereisit.WhereIsIt;
import red.jackf.whereisit.WhereIsItClient;

import java.util.stream.Collectors;

public final class HarmonyChestTrackerBootstrap {
    private static boolean initialized;
    private static BlockPos latestInventoryPos;

    private HarmonyChestTrackerBootstrap() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }

        new LibGuiCommon().onInitialize();
        new LibGuiClient().onInitializeClient();
        ScreenNetworkingImpl.init();
        new WhereIsIt().onInitialize();
        new WhereIsItClient().onInitializeClient();
        new ChestTracker().onInitializeClient();
        initialized = true;
    }

    public static void fireClientTickStart(Minecraft minecraft) {
        if (initialized) {
            ClientTickEvents.START_CLIENT_TICK.invoker().onStartTick(minecraft);
        }
    }

    public static void fireClientTickEnd(Minecraft minecraft) {
        if (initialized) {
            ClientTickEvents.END_CLIENT_TICK.invoker().onEndTick(minecraft);
        }
    }

    public static void fireWorldTickEnd(ClientWorld world) {
        if (initialized && world != null) {
            ClientTickEvents.END_WORLD_TICK.invoker().onEndTick(world);
        }
    }

    public static void fireClientStopping(Minecraft minecraft) {
        if (initialized) {
            ClientLifecycleEvents.CLIENT_STOPPING.invoker().onClientStopping(minecraft);
        }
    }

    public static void fireScreenInitPost(Minecraft minecraft, Screen screen) {
        if (initialized && screen instanceof ScreenHooks) {
            ClothClientHooks.SCREEN_INIT_POST.invoker().init(minecraft, screen, (ScreenHooks) screen);
        }
    }

    public static void fireScreenLateRender(MatrixStack matrixStack, Screen screen, int mouseX, int mouseY, float partialTicks) {
        if (initialized) {
            ClothClientHooks.SCREEN_LATE_RENDER.invoker().render(matrixStack, Minecraft.getInstance(), screen, mouseX, mouseY, partialTicks);
        }
    }

    public static boolean fireScreenKeyReleased(Screen screen, int keyCode, int scanCode, int modifiers) {
        if (!initialized) {
            return false;
        }

        handleWhereIsItSlotSearch(screen, keyCode, scanCode);
        return ClothClientHooks.SCREEN_KEY_RELEASED.invoker().keyReleased(Minecraft.getInstance(), screen, keyCode, scanCode, modifiers) != ActionResultType.PASS;
    }

    private static void handleWhereIsItSlotSearch(Screen screen, int keyCode, int scanCode) {
        if (!WhereIsItClient.FIND_ITEMS.matchesKey(keyCode, scanCode) || !(screen instanceof ContainerScreen)) {
            return;
        }

        Slot slot = ((ContainerScreen<?>) screen).getHoveredSlot();

        if (slot != null && slot.getHasStack()) {
            ItemStack stack = slot.getStack();
            WhereIsItClient.searchForItem(stack.getItem(), Screen.hasShiftDown(), stack.getTag());
        }
    }

    public static void fireWorldRenderLast(MatrixStack matrixStack, float partialTicks, ActiveRenderInfo camera, Matrix4f projectionMatrix) {
        Minecraft minecraft = Minecraft.getInstance();

        if (initialized && minecraft.world != null) {
            WorldRenderEvents.LAST.invoker().onLast(new HarmonyWorldRenderContext(minecraft.world, camera, matrixStack, partialTicks, projectionMatrix));
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void handleScreenClose(Screen screen) {
        if (initialized && screen instanceof ContainerScreen) {
            if (MemoryUtils.getLatestPos() == null && latestInventoryPos != null) {
                MemoryUtils.setLatestPos(latestInventoryPos);
            }

            MemoryUtils.handleItemsFromScreen((ContainerScreen) screen);

            MemoryDatabase database = MemoryDatabase.getCurrent();
            if (database != null) {
                database.save();
            }
        }
    }

    public static void handleDisconnect() {
        if (initialized) {
            MemoryDatabase.clearCurrent();
        }
    }

    public static void handleBlockBreak(World world, BlockPos pos) {
        MemoryDatabase database = initialized ? MemoryDatabase.getCurrent() : null;

        if (database != null && world != null && pos != null) {
            database.removePos(world.getDimensionKey().getLocation(), pos);
        }
    }

    public static void handleUseBlock(World world, BlockRayTraceResult rayTrace) {
        if (!initialized || world == null || rayTrace == null || !world.isRemote) {
            return;
        }

        BlockPos pos = rayTrace.getPos();

        if (pos != null && MemoryUtils.isValidInventoryHolder(world.getBlockState(pos).getBlock(), world, pos)) {
            latestInventoryPos = pos.toImmutable();
            MemoryUtils.setLatestPos(pos);
        } else {
            latestInventoryPos = null;
            MemoryUtils.setLatestPos(null);
        }
    }

    public static void handleShulkerPlaced(World world, BlockPos pos, ItemStack stack) {
        if (!initialized || world == null || !world.isRemote || pos == null || stack == null) {
            return;
        }

        TileEntity tileEntity = world.getTileEntity(pos);
        MemoryDatabase database = MemoryDatabase.getCurrent();
        CompoundNBT tag = stack.getChildTag("BlockEntityTag");

        if (database == null || !(tileEntity instanceof ShulkerBoxTileEntity) || tag == null || !tag.contains("Items", 9)) {
            return;
        }

        NonNullList<ItemStack> items = NonNullList.withSize(((ShulkerBoxTileEntity) tileEntity).getSizeInventory(), ItemStack.EMPTY);
        ItemStackHelper.loadAllItems(tag, items);
        database.mergeItems(
                world.getDimensionKey().getLocation(),
                Memory.of(
                        pos,
                        MemoryUtils.condenseItems(items.stream().filter(itemStack -> !itemStack.isEmpty()).collect(Collectors.toList())),
                        stack.hasDisplayName() ? stack.getDisplayName() : null,
                        null
                )
        );
    }
}
