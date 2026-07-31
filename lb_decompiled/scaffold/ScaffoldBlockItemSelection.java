/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  kotlin.Metadata
 *  kotlin.jvm.internal.Intrinsics
 *  kotlin.jvm.internal.PropertyReference1
 *  kotlin.jvm.internal.PropertyReference1Impl
 *  kotlin.jvm.internal.Reflection
 *  kotlin.jvm.internal.SourceDebugExtension
 *  kotlin.reflect.KProperty
 *  net.ccbluex.liquidbounce.config.types.group.ValueGroup
 *  net.ccbluex.liquidbounce.config.types.list.RegistryListValue
 *  net.ccbluex.liquidbounce.utils.collection.RegistryExtensionsKt
 *  net.ccbluex.liquidbounce.utils.item.ItemExtensionsKt
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.block.BaseEntityBlock
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.FallingBlock
 *  net.minecraft.world.level.block.state.BlockState
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold;

import java.util.SortedSet;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.PropertyReference1;
import kotlin.jvm.internal.PropertyReference1Impl;
import kotlin.jvm.internal.Reflection;
import kotlin.jvm.internal.SourceDebugExtension;
import kotlin.reflect.KProperty;
import net.ccbluex.liquidbounce.config.types.group.ValueGroup;
import net.ccbluex.liquidbounce.config.types.list.RegistryListValue;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold;
import net.ccbluex.liquidbounce.utils.collection.RegistryExtensionsKt;
import net.ccbluex.liquidbounce.utils.item.ItemExtensionsKt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Metadata(mv={2, 3, 0}, k=1, xi=50, d1={"\u0000&\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\b\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003J\u0010\u0010\u000e\u001a\u00020\u000f2\b\u0010\u0010\u001a\u0004\u0018\u00010\u0011J\u000e\u0010\u0012\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u0011R!\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u00058BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\t\u0010\n\u001a\u0004\b\u0007\u0010\bR!\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\u00060\u00058BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\r\u0010\n\u001a\u0004\b\f\u0010\b\u00a8\u0006\u0013"}, d2={"Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/ScaffoldBlockItemSelection;", "Lnet/ccbluex/liquidbounce/config/types/group/ValueGroup;", "<init>", "()V", "disallowedBlocksToPlace", "Ljava/util/SortedSet;", "Lnet/minecraft/world/level/block/Block;", "getDisallowedBlocksToPlace", "()Ljava/util/SortedSet;", "disallowedBlocksToPlace$delegate", "Lnet/ccbluex/liquidbounce/config/types/list/RegistryListValue;", "unfavorableBlocksToPlace", "getUnfavorableBlocksToPlace", "unfavorableBlocksToPlace$delegate", "isValidBlock", "", "stack", "Lnet/minecraft/world/item/ItemStack;", "isBlockUnfavourable", "liquidbounce"})
@SourceDebugExtension(value={"SMAP\nScaffoldBlockItemSelection.kt\nKotlin\n*S Kotlin\n*F\n+ 1 ScaffoldBlockItemSelection.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/ScaffoldBlockItemSelection\n+ 2 MinecraftExtensions.kt\nnet/ccbluex/liquidbounce/utils/client/MinecraftExtensionsKt\n*L\n1#1,107:1\n47#2:108\n43#2,3:109\n*S KotlinDebug\n*F\n+ 1 ScaffoldBlockItemSelection.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/ScaffoldBlockItemSelection\n*L\n74#1:108\n74#1:109,3\n*E\n"})
public final class ScaffoldBlockItemSelection
extends ValueGroup {
    @NotNull
    public static final ScaffoldBlockItemSelection INSTANCE;
    static final /* synthetic */ KProperty<Object>[] $$delegatedProperties;
    @NotNull
    private static final RegistryListValue disallowedBlocksToPlace$delegate;
    @NotNull
    private static final RegistryListValue unfavorableBlocksToPlace$delegate;

    private ScaffoldBlockItemSelection() {
        super("BlockItemSelection", null, null, false, null, 30, null);
    }

    private final SortedSet<Block> getDisallowedBlocksToPlace() {
        return (SortedSet)disallowedBlocksToPlace$delegate.getValue((Object)this, $$delegatedProperties[0]);
    }

    private final SortedSet<Block> getUnfavorableBlocksToPlace() {
        return (SortedSet)unfavorableBlocksToPlace$delegate.getValue((Object)this, $$delegatedProperties[1]);
    }

    public final boolean isValidBlock(@Nullable ItemStack stack) {
        if (stack == null) {
            return false;
        }
        Block block = ItemExtensionsKt.getBlock((ItemStack)stack);
        if (block == null) {
            return false;
        }
        Block block2 = block;
        BlockState blockState = block2.defaultBlockState();
        Intrinsics.checkNotNullExpressionValue((Object)blockState, (String)"defaultBlockState(...)");
        BlockState defaultState = blockState;
        boolean $i$f$getWorld = false;
        boolean $i$f$getMc = false;
        Minecraft minecraft = Minecraft.getInstance();
        Intrinsics.checkNotNullExpressionValue((Object)minecraft, (String)"getInstance(...)");
        ClientLevel clientLevel = minecraft.level;
        Intrinsics.checkNotNull((Object)clientLevel);
        BlockGetter blockGetter = (BlockGetter)clientLevel;
        boolean $i$f$getPlayer = false;
        $i$f$getMc = false;
        Minecraft minecraft2 = Minecraft.getInstance();
        Intrinsics.checkNotNullExpressionValue((Object)minecraft2, (String)"getInstance(...)");
        LocalPlayer localPlayer = minecraft2.player;
        Intrinsics.checkNotNull((Object)localPlayer);
        return !defaultState.entityCanStandOnFace(blockGetter, BlockPos.ZERO, (Entity)localPlayer, Direction.UP) ? false : (block2 instanceof FallingBlock ? false : !this.getDisallowedBlocksToPlace().contains(block2));
    }

    public final boolean isBlockUnfavourable(@NotNull ItemStack stack) {
        boolean bl;
        Intrinsics.checkNotNullParameter((Object)stack, (String)"stack");
        Block block = ItemExtensionsKt.getBlock((ItemStack)stack);
        if (block == null) {
            return true;
        }
        Block block2 = block;
        if (block2.getFriction() > 0.6f) {
            bl = true;
        } else if (block2.getSpeedFactor() < 1.0f) {
            bl = true;
        } else if (block2.getJumpFactor() < 1.0f) {
            bl = true;
        } else if (block2 instanceof BaseEntityBlock) {
            bl = true;
        } else {
            BlockState blockState = block2.defaultBlockState();
            ClientLevel clientLevel = ModuleScaffold.INSTANCE.getMc().level;
            Intrinsics.checkNotNull((Object)clientLevel);
            bl = !blockState.isCollisionShapeFullBlock((BlockGetter)clientLevel, BlockPos.ZERO) ? true : this.getUnfavorableBlocksToPlace().contains(block2);
        }
        return bl;
    }

    static {
        KProperty[] kPropertyArray = new KProperty[]{Reflection.property1((PropertyReference1)((PropertyReference1)new PropertyReference1Impl(ScaffoldBlockItemSelection.class, "disallowedBlocksToPlace", "getDisallowedBlocksToPlace()Ljava/util/SortedSet;", 0))), Reflection.property1((PropertyReference1)((PropertyReference1)new PropertyReference1Impl(ScaffoldBlockItemSelection.class, "unfavorableBlocksToPlace", "getUnfavorableBlocksToPlace()Ljava/util/SortedSet;", 0)))};
        $$delegatedProperties = kPropertyArray;
        INSTANCE = new ScaffoldBlockItemSelection();
        kPropertyArray = new Block[3];
        Intrinsics.checkNotNullExpressionValue((Object)Blocks.TNT, (String)"TNT");
        Intrinsics.checkNotNullExpressionValue((Object)Blocks.COBWEB, (String)"COBWEB");
        Intrinsics.checkNotNullExpressionValue((Object)Blocks.NETHER_PORTAL, (String)"NETHER_PORTAL");
        disallowedBlocksToPlace$delegate = INSTANCE.blocks("Disallowed", RegistryExtensionsKt.blockSortedSetOf((Block[])kPropertyArray));
        kPropertyArray = new Block[7];
        Intrinsics.checkNotNullExpressionValue((Object)Blocks.CRAFTING_TABLE, (String)"CRAFTING_TABLE");
        Intrinsics.checkNotNullExpressionValue((Object)Blocks.JIGSAW, (String)"JIGSAW");
        Intrinsics.checkNotNullExpressionValue((Object)Blocks.SMITHING_TABLE, (String)"SMITHING_TABLE");
        Intrinsics.checkNotNullExpressionValue((Object)Blocks.FLETCHING_TABLE, (String)"FLETCHING_TABLE");
        Intrinsics.checkNotNullExpressionValue((Object)Blocks.ENCHANTING_TABLE, (String)"ENCHANTING_TABLE");
        Intrinsics.checkNotNullExpressionValue((Object)Blocks.CAULDRON, (String)"CAULDRON");
        Intrinsics.checkNotNullExpressionValue((Object)Blocks.MAGMA_BLOCK, (String)"MAGMA_BLOCK");
        unfavorableBlocksToPlace$delegate = INSTANCE.blocks("Unfavorable", RegistryExtensionsKt.blockSortedSetOf((Block[])kPropertyArray));
    }
}

