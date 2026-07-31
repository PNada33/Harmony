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
 *  kotlin.ranges.ClosedFloatingPointRange
 *  kotlin.ranges.RangesKt
 *  kotlin.reflect.KProperty
 *  net.ccbluex.liquidbounce.config.types.RangedValue
 *  net.ccbluex.liquidbounce.config.types.group.ValueGroup
 *  net.ccbluex.liquidbounce.event.EventHook
 *  net.ccbluex.liquidbounce.event.EventListener
 *  net.ccbluex.liquidbounce.event.EventListenerKt
 *  net.ccbluex.liquidbounce.event.events.MovementInputEvent
 *  net.ccbluex.liquidbounce.utils.aiming.data.Rotation
 *  net.ccbluex.liquidbounce.utils.block.BlockExtensionsKt
 *  net.ccbluex.liquidbounce.utils.block.targetfinding.BlockOffsetOptions
 *  net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTarget
 *  net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTargetFindingOptions
 *  net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPosOffsets
 *  net.ccbluex.liquidbounce.utils.block.targetfinding.CenterTargetPositionFactory
 *  net.ccbluex.liquidbounce.utils.block.targetfinding.FaceHandlingOptions
 *  net.ccbluex.liquidbounce.utils.block.targetfinding.FaceTargetPositionFactory
 *  net.ccbluex.liquidbounce.utils.block.targetfinding.PlayerLocationOnPlacement
 *  net.ccbluex.liquidbounce.utils.block.targetfinding.TargetFindingKt
 *  net.ccbluex.liquidbounce.utils.entity.EntityExtensionsKt
 *  net.ccbluex.liquidbounce.utils.kotlin.ArrayExtensionsKt
 *  net.ccbluex.liquidbounce.utils.math.geometry.Line
 *  net.ccbluex.liquidbounce.utils.movement.DirectionalInput
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.world.entity.Pose
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques;

import java.util.function.Consumer;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.PropertyReference1;
import kotlin.jvm.internal.PropertyReference1Impl;
import kotlin.jvm.internal.Reflection;
import kotlin.jvm.internal.SourceDebugExtension;
import kotlin.ranges.ClosedFloatingPointRange;
import kotlin.ranges.RangesKt;
import kotlin.reflect.KProperty;
import net.ccbluex.liquidbounce.config.types.RangedValue;
import net.ccbluex.liquidbounce.config.types.group.ValueGroup;
import net.ccbluex.liquidbounce.event.EventHook;
import net.ccbluex.liquidbounce.event.EventListener;
import net.ccbluex.liquidbounce.event.EventListenerKt;
import net.ccbluex.liquidbounce.event.events.MovementInputEvent;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.ScaffoldTechnique;
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation;
import net.ccbluex.liquidbounce.utils.block.BlockExtensionsKt;
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockOffsetOptions;
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTarget;
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTargetFindingOptions;
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPosOffsets;
import net.ccbluex.liquidbounce.utils.block.targetfinding.CenterTargetPositionFactory;
import net.ccbluex.liquidbounce.utils.block.targetfinding.FaceHandlingOptions;
import net.ccbluex.liquidbounce.utils.block.targetfinding.FaceTargetPositionFactory;
import net.ccbluex.liquidbounce.utils.block.targetfinding.PlayerLocationOnPlacement;
import net.ccbluex.liquidbounce.utils.block.targetfinding.TargetFindingKt;
import net.ccbluex.liquidbounce.utils.entity.EntityExtensionsKt;
import net.ccbluex.liquidbounce.utils.kotlin.ArrayExtensionsKt;
import net.ccbluex.liquidbounce.utils.math.geometry.Line;
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Metadata(mv={2, 3, 0}, k=1, xi=50, d1={"\u0000X\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0007\n\u0000\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u0006\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0006\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003J,\u0010\u0010\u001a\u0004\u0018\u00010\u00112\u0006\u0010\u0012\u001a\u00020\u00132\u0006\u0010\u0014\u001a\u00020\u00152\b\u0010\u0016\u001a\u0004\u0018\u00010\u00172\u0006\u0010\u0018\u001a\u00020\u0019H\u0016J\u0014\u0010\u001e\u001a\u0004\u0018\u00010\u001f2\b\u0010 \u001a\u0004\u0018\u00010\u0011H\u0016J\u0010\u0010!\u001a\u00020\u001f2\u0006\u0010\"\u001a\u00020\u0005H\u0002J\u0010\u0010#\u001a\u00020\u001f2\u0006\u0010\"\u001a\u00020\u0005H\u0002J\u0010\u0010$\u001a\u00020\u001f2\u0006\u0010 \u001a\u00020\u0011H\u0002R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u000e\u00a2\u0006\u0002\n\u0000R!\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u00050\u000b8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u000e\u0010\u000f\u001a\u0004\b\f\u0010\rR\u001a\u0010\u001a\u001a\b\u0012\u0004\u0012\u00020\u001c0\u001bX\u0082\u0004\u00a2\u0006\b\n\u0000\u0012\u0004\b\u001d\u0010\u0003\u00a8\u0006%"}, d2={"Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/techniques/ScaffoldBreezilyTechnique;", "Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/techniques/ScaffoldTechnique;", "<init>", "()V", "lastSideways", "", "lastAirTime", "", "currentEdgeDistanceRandom", "", "edgeDistance", "Lkotlin/ranges/ClosedFloatingPointRange;", "getEdgeDistance", "()Lkotlin/ranges/ClosedFloatingPointRange;", "edgeDistance$delegate", "Lnet/ccbluex/liquidbounce/config/types/RangedValue;", "findPlacementTarget", "Lnet/ccbluex/liquidbounce/utils/block/targetfinding/BlockPlacementTarget;", "predictedPos", "Lnet/minecraft/world/phys/Vec3;", "predictedPose", "Lnet/minecraft/world/entity/Pose;", "optimalLine", "Lnet/ccbluex/liquidbounce/utils/math/geometry/Line;", "bestStack", "Lnet/minecraft/world/item/ItemStack;", "handleMovementInput", "Lnet/ccbluex/liquidbounce/event/EventHook;", "Lnet/ccbluex/liquidbounce/event/events/MovementInputEvent;", "getHandleMovementInput$annotations", "getRotations", "Lnet/ccbluex/liquidbounce/utils/aiming/data/Rotation;", "target", "getRotationForStraightInput", "movingYaw", "getRotationForDiagonalInput", "getRotationForNoInput", "liquidbounce"})
@SourceDebugExtension(value={"SMAP\nScaffoldBreezilyTechnique.kt\nKotlin\n*S Kotlin\n*F\n+ 1 ScaffoldBreezilyTechnique.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/techniques/ScaffoldBreezilyTechnique\n+ 2 MinecraftVectorExtensions.kt\nnet/ccbluex/liquidbounce/utils/math/MinecraftVectorExtensionsKt\n+ 3 BlockExtensions.kt\nnet/ccbluex/liquidbounce/utils/block/BlockExtensionsKt\n+ 4 EventListener.kt\nnet/ccbluex/liquidbounce/event/EventListenerKt\n*L\n1#1,169:1\n203#2,5:170\n130#3:175\n99#4:176\n*S KotlinDebug\n*F\n+ 1 ScaffoldBreezilyTechnique.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/techniques/ScaffoldBreezilyTechnique\n*L\n74#1:170,5\n85#1:175\n-1#1:176\n*E\n"})
public final class ScaffoldBreezilyTechnique
extends ScaffoldTechnique {
    @NotNull
    public static final ScaffoldBreezilyTechnique INSTANCE;
    static final /* synthetic */ KProperty<Object>[] $$delegatedProperties;
    private static float lastSideways;
    private static long lastAirTime;
    private static double currentEdgeDistanceRandom;
    @NotNull
    private static final RangedValue edgeDistance$delegate;
    @NotNull
    private static final EventHook<MovementInputEvent> handleMovementInput;

    private ScaffoldBreezilyTechnique() {
        super("Breezily", null);
    }

    private final ClosedFloatingPointRange<Float> getEdgeDistance() {
        return (ClosedFloatingPointRange)edgeDistance$delegate.getValue((Object)this, $$delegatedProperties[0]);
    }

    @Override
    @Nullable
    public BlockPlacementTarget findPlacementTarget(@NotNull Vec3 predictedPos, @NotNull Pose predictedPose, @Nullable Line optimalLine, @NotNull ItemStack bestStack) {
        Intrinsics.checkNotNullParameter((Object)predictedPos, (String)"predictedPos");
        Intrinsics.checkNotNullParameter((Object)predictedPose, (String)"predictedPose");
        Intrinsics.checkNotNullParameter((Object)bestStack, (String)"bestStack");
        BlockPlacementTargetFindingOptions searchOptions = new BlockPlacementTargetFindingOptions(new BlockOffsetOptions(BlockPosOffsets.NORMAL.getOffsets(), BlockPlacementTargetFindingOptions.Companion.leastBlockDistanceToPos(predictedPos)), new FaceHandlingOptions((FaceTargetPositionFactory)CenterTargetPositionFactory.INSTANCE, false, 2, null), bestStack, new PlayerLocationOnPlacement(predictedPos, predictedPose));
        Vec3 $this$toBlockPos_u24default$iv = predictedPos;
        double xOffset$iv = 0.0;
        double yOffset$iv = 0.0;
        double zOffset$iv = 0.0;
        boolean $i$f$toBlockPos = false;
        BlockPos blockPos = BlockPos.containing((double)($this$toBlockPos_u24default$iv.x + xOffset$iv), (double)($this$toBlockPos_u24default$iv.y + yOffset$iv), (double)($this$toBlockPos_u24default$iv.z + zOffset$iv));
        Intrinsics.checkNotNullExpressionValue((Object)blockPos, (String)"containing(...)");
        return TargetFindingKt.findBestBlockPlacementTarget((BlockPos)ModuleScaffold.INSTANCE.getTargetedPosition$liquidbounce(blockPos), (BlockPlacementTargetFindingOptions)searchOptions);
    }

    private static /* synthetic */ void getHandleMovementInput$annotations() {
    }

    @Override
    @Nullable
    public Rotation getRotations(@Nullable BlockPlacementTarget target) {
        if (Intrinsics.areEqual((Object)ModuleScaffold.INSTANCE.getRawInput(), (Object)DirectionalInput.NONE)) {
            if (target == null) {
                return null;
            }
            return this.getRotationForNoInput(target);
        }
        float direction = EntityExtensionsKt.getMovementDirectionOfInput((LocalPlayer)this.getPlayer(), (DirectionalInput)ModuleScaffold.INSTANCE.getRawInput()) + (float)180;
        float movingYaw = (float)Math.rint(direction / (float)45) * (float)45;
        boolean isMovingStraight = movingYaw % (float)90 == 0.0f;
        return isMovingStraight ? this.getRotationForStraightInput(movingYaw) : this.getRotationForDiagonalInput(movingYaw);
    }

    private final Rotation getRotationForStraightInput(float movingYaw) {
        return new Rotation(movingYaw, 80.0f, false, 4, null);
    }

    private final Rotation getRotationForDiagonalInput(float movingYaw) {
        return new Rotation(movingYaw, 75.6f, false, 4, null);
    }

    private final Rotation getRotationForNoInput(BlockPlacementTarget target) {
        float axisMovement = (float)Math.floor(target.getRotation().yaw() / (float)90) * (float)90;
        float yaw = axisMovement + (float)45;
        float pitch = 75.0f;
        return new Rotation(yaw, pitch, false, 4, null);
    }

    private static final void handleMovementInput$lambda$0(MovementInputEvent event) {
        Intrinsics.checkNotNullParameter((Object)event, (String)"event");
        if (!event.getDirectionalInput().getForwards() || INSTANCE.getPlayer().isShiftKeyDown()) {
            return;
        }
        BlockPos blockPos = INSTANCE.getPlayer().blockPosition().below();
        Intrinsics.checkNotNullExpressionValue((Object)blockPos, (String)"below(...)");
        BlockPos $this$getState$iv = blockPos;
        boolean bl = false;
        BlockState blockState = BlockExtensionsKt.getState((BlockPos)$this$getState$iv);
        Intrinsics.checkNotNull((Object)blockState);
        if (blockState.isAir()) {
            lastAirTime = System.currentTimeMillis();
        } else if (System.currentTimeMillis() - lastAirTime > 500L) {
            return;
        }
        double modX = INSTANCE.getPlayer().getX() - Math.floor(INSTANCE.getPlayer().getX());
        double modZ = INSTANCE.getPlayer().getZ() - Math.floor(INSTANCE.getPlayer().getZ());
        double ma = 1.0 - currentEdgeDistanceRandom;
        float currentSideways = 0.0f;
        switch (WhenMappings.$EnumSwitchMapping$0[Direction.fromYRot((double)INSTANCE.getPlayer().getYRot()).ordinal()]) {
            case 1: {
                if (modX > ma) {
                    currentSideways = 1.0f;
                }
                if (!(modX < currentEdgeDistanceRandom)) break;
                currentSideways = -1.0f;
                break;
            }
            case 2: {
                if (modX > ma) {
                    currentSideways = -1.0f;
                }
                if (!(modX < currentEdgeDistanceRandom)) break;
                currentSideways = 1.0f;
                break;
            }
            case 3: {
                if (modZ > ma) {
                    currentSideways = -1.0f;
                }
                if (!(modZ < currentEdgeDistanceRandom)) break;
                currentSideways = 1.0f;
                break;
            }
            case 4: {
                if (modZ > ma) {
                    currentSideways = 1.0f;
                }
                if (!(modZ < currentEdgeDistanceRandom)) break;
                currentSideways = -1.0f;
            }
        }
        if (!(lastSideways == currentSideways) && !(currentSideways == 0.0f)) {
            lastSideways = currentSideways;
            currentEdgeDistanceRandom = ArrayExtensionsKt.random(INSTANCE.getEdgeDistance());
        }
        event.setDirectionalInput(new DirectionalInput(event.getDirectionalInput().getForwards(), event.getDirectionalInput().getBackwards(), lastSideways == -1.0f, lastSideways == 1.0f));
    }

    /*
     * WARNING - void declaration
     */
    static {
        void priority$iv;
        void $this$handler$iv;
        EventListener eventListener = new EventListener[]{Reflection.property1((PropertyReference1)((PropertyReference1)new PropertyReference1Impl(ScaffoldBreezilyTechnique.class, "edgeDistance", "getEdgeDistance()Lkotlin/ranges/ClosedFloatingPointRange;", 0)))};
        $$delegatedProperties = eventListener;
        INSTANCE = new ScaffoldBreezilyTechnique();
        currentEdgeDistanceRandom = 0.45;
        edgeDistance$delegate = ValueGroup.floatRange$default((ValueGroup)((ValueGroup)INSTANCE), (String)"EdgeDistance", (ClosedFloatingPointRange)RangesKt.rangeTo((float)0.45f, (float)0.5f), (ClosedFloatingPointRange)RangesKt.rangeTo((float)0.25f, (float)0.5f), (String)"blocks", null, (int)16, null);
        eventListener = (EventListener)INSTANCE;
        int n = -50;
        Consumer<MovementInputEvent> handler$iv = ScaffoldBreezilyTechnique::handleMovementInput$lambda$0;
        boolean $i$f$handler = false;
        handleMovementInput = EventListenerKt.handler((EventListener)$this$handler$iv, MovementInputEvent.class, (short)priority$iv, handler$iv);
    }

    @Metadata(mv={2, 3, 0}, k=3, xi=50)
    public static final class WhenMappings {
        public static final /* synthetic */ int[] $EnumSwitchMapping$0;

        static {
            int[] nArray = new int[Direction.values().length];
            try {
                nArray[Direction.SOUTH.ordinal()] = 1;
            }
            catch (NoSuchFieldError noSuchFieldError) {
                // empty catch block
            }
            try {
                nArray[Direction.NORTH.ordinal()] = 2;
            }
            catch (NoSuchFieldError noSuchFieldError) {
                // empty catch block
            }
            try {
                nArray[Direction.EAST.ordinal()] = 3;
            }
            catch (NoSuchFieldError noSuchFieldError) {
                // empty catch block
            }
            try {
                nArray[Direction.WEST.ordinal()] = 4;
            }
            catch (NoSuchFieldError noSuchFieldError) {
                // empty catch block
            }
            $EnumSwitchMapping$0 = nArray;
        }
    }
}

