/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  kotlin.Metadata
 *  kotlin.NoWhenBranchMatchedException
 *  kotlin.jvm.internal.Intrinsics
 *  kotlin.jvm.internal.PropertyReference1
 *  kotlin.jvm.internal.PropertyReference1Impl
 *  kotlin.jvm.internal.Reflection
 *  kotlin.jvm.internal.SourceDebugExtension
 *  kotlin.random.Random
 *  kotlin.reflect.KProperty
 *  net.ccbluex.liquidbounce.config.types.Value
 *  net.ccbluex.liquidbounce.config.types.group.ValueGroup
 *  net.ccbluex.liquidbounce.config.types.list.ChoiceListValue
 *  net.ccbluex.liquidbounce.config.types.list.Tagged
 *  net.ccbluex.liquidbounce.event.EventHook
 *  net.ccbluex.liquidbounce.event.EventListener
 *  net.ccbluex.liquidbounce.event.EventListenerKt
 *  net.ccbluex.liquidbounce.event.events.PlayerAfterJumpEvent
 *  net.ccbluex.liquidbounce.features.module.modules.movement.ModuleFreeze
 *  net.ccbluex.liquidbounce.utils.aiming.data.Rotation
 *  net.ccbluex.liquidbounce.utils.block.targetfinding.AimMode
 *  net.ccbluex.liquidbounce.utils.block.targetfinding.AngleYawTargetPositionFactory
 *  net.ccbluex.liquidbounce.utils.block.targetfinding.BlockOffsetOptions
 *  net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTarget
 *  net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTargetFindingOptions
 *  net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPosOffsets
 *  net.ccbluex.liquidbounce.utils.block.targetfinding.CenterTargetPositionFactory
 *  net.ccbluex.liquidbounce.utils.block.targetfinding.DiagonalYawTargetPositionFactory
 *  net.ccbluex.liquidbounce.utils.block.targetfinding.EdgePointTargetPositionFactory
 *  net.ccbluex.liquidbounce.utils.block.targetfinding.FaceHandlingOptions
 *  net.ccbluex.liquidbounce.utils.block.targetfinding.FaceTargetPositionFactory
 *  net.ccbluex.liquidbounce.utils.block.targetfinding.NearestRotationTargetPositionFactory
 *  net.ccbluex.liquidbounce.utils.block.targetfinding.PlayerLocationOnPlacement
 *  net.ccbluex.liquidbounce.utils.block.targetfinding.PositionFactoryConfiguration
 *  net.ccbluex.liquidbounce.utils.block.targetfinding.RandomTargetPositionFactory
 *  net.ccbluex.liquidbounce.utils.block.targetfinding.ReverseYawTargetPositionFactory
 *  net.ccbluex.liquidbounce.utils.block.targetfinding.StabilizedRotationTargetPositionFactory
 *  net.ccbluex.liquidbounce.utils.block.targetfinding.TargetFindingKt
 *  net.ccbluex.liquidbounce.utils.entity.EntityExtensionsKt
 *  net.ccbluex.liquidbounce.utils.math.geometry.Line
 *  net.ccbluex.liquidbounce.utils.raytracing.RaytracingKt
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.Pose
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.HitResult$Type
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import kotlin.Metadata;
import kotlin.NoWhenBranchMatchedException;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.PropertyReference1;
import kotlin.jvm.internal.PropertyReference1Impl;
import kotlin.jvm.internal.Reflection;
import kotlin.jvm.internal.SourceDebugExtension;
import kotlin.random.Random;
import kotlin.reflect.KProperty;
import net.ccbluex.liquidbounce.config.types.Value;
import net.ccbluex.liquidbounce.config.types.group.ValueGroup;
import net.ccbluex.liquidbounce.config.types.list.ChoiceListValue;
import net.ccbluex.liquidbounce.config.types.list.Tagged;
import net.ccbluex.liquidbounce.event.EventHook;
import net.ccbluex.liquidbounce.event.EventListener;
import net.ccbluex.liquidbounce.event.EventListenerKt;
import net.ccbluex.liquidbounce.event.events.PlayerAfterJumpEvent;
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleFreeze;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldCeilingFeature;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldHeadHitterFeature;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.ScaffoldTechnique;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.normal.ScaffoldDownFeature;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.normal.ScaffoldEagleFeature;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.normal.ScaffoldStabilizeMovementFeature;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.normal.ScaffoldTellyFeature;
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation;
import net.ccbluex.liquidbounce.utils.block.targetfinding.AimMode;
import net.ccbluex.liquidbounce.utils.block.targetfinding.AngleYawTargetPositionFactory;
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockOffsetOptions;
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTarget;
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTargetFindingOptions;
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPosOffsets;
import net.ccbluex.liquidbounce.utils.block.targetfinding.CenterTargetPositionFactory;
import net.ccbluex.liquidbounce.utils.block.targetfinding.DiagonalYawTargetPositionFactory;
import net.ccbluex.liquidbounce.utils.block.targetfinding.EdgePointTargetPositionFactory;
import net.ccbluex.liquidbounce.utils.block.targetfinding.FaceHandlingOptions;
import net.ccbluex.liquidbounce.utils.block.targetfinding.FaceTargetPositionFactory;
import net.ccbluex.liquidbounce.utils.block.targetfinding.NearestRotationTargetPositionFactory;
import net.ccbluex.liquidbounce.utils.block.targetfinding.PlayerLocationOnPlacement;
import net.ccbluex.liquidbounce.utils.block.targetfinding.PositionFactoryConfiguration;
import net.ccbluex.liquidbounce.utils.block.targetfinding.RandomTargetPositionFactory;
import net.ccbluex.liquidbounce.utils.block.targetfinding.ReverseYawTargetPositionFactory;
import net.ccbluex.liquidbounce.utils.block.targetfinding.StabilizedRotationTargetPositionFactory;
import net.ccbluex.liquidbounce.utils.block.targetfinding.TargetFindingKt;
import net.ccbluex.liquidbounce.utils.entity.EntityExtensionsKt;
import net.ccbluex.liquidbounce.utils.math.geometry.Line;
import net.ccbluex.liquidbounce.utils.raytracing.RaytracingKt;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Metadata(mv={2, 3, 0}, k=1, xi=50, d1={"\u0000b\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u000b\n\u0002\b\u0005\n\u0002\u0010\u0006\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003J,\u0010\u0012\u001a\u0004\u0018\u00010\u00132\u0006\u0010\u0014\u001a\u00020\u00152\u0006\u0010\u0016\u001a\u00020\u00172\b\u0010\u0018\u001a\u0004\u0018\u00010\u00192\u0006\u0010\u001a\u001a\u00020\u001bH\u0016J\u0014\u0010\u001c\u001a\u0004\u0018\u00010\u001d2\b\u0010\u001e\u001a\u0004\u0018\u00010\u0013H\u0016J\u001c\u0010\u001f\u001a\u0004\u0018\u00010 2\b\u0010\u001e\u001a\u0004\u0018\u00010\u00132\u0006\u0010!\u001a\u00020\u001dH\u0016J\"\u0010\"\u001a\u00020#2\u0006\u0010\u0014\u001a\u00020\u00152\u0006\u0010\u0016\u001a\u00020\u00172\b\u0010\u0018\u001a\u0004\u0018\u00010\u0019H\u0002R\u001b\u0010\u0004\u001a\u00020\u00058BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\b\u0010\t\u001a\u0004\b\u0006\u0010\u0007R\u001b\u0010\n\u001a\u00020\u000b8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u000e\u0010\u000f\u001a\u0004\b\f\u0010\rR\u000e\u0010\u0010\u001a\u00020\u0011X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001a\u0010$\u001a\b\u0012\u0004\u0012\u00020&0%X\u0082\u0004\u00a2\u0006\b\n\u0000\u0012\u0004\b'\u0010\u0003\u00a8\u0006("}, d2={"Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/techniques/ScaffoldNormalTechnique;", "Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/techniques/ScaffoldTechnique;", "<init>", "()V", "aimMode", "Lnet/ccbluex/liquidbounce/utils/block/targetfinding/AimMode;", "getAimMode", "()Lnet/ccbluex/liquidbounce/utils/block/targetfinding/AimMode;", "aimMode$delegate", "Lnet/ccbluex/liquidbounce/config/types/list/ChoiceListValue;", "requiresSight", "", "getRequiresSight", "()Z", "requiresSight$delegate", "Lnet/ccbluex/liquidbounce/config/types/Value;", "randomization", "", "findPlacementTarget", "Lnet/ccbluex/liquidbounce/utils/block/targetfinding/BlockPlacementTarget;", "predictedPos", "Lnet/minecraft/world/phys/Vec3;", "predictedPose", "Lnet/minecraft/world/entity/Pose;", "optimalLine", "Lnet/ccbluex/liquidbounce/utils/math/geometry/Line;", "bestStack", "Lnet/minecraft/world/item/ItemStack;", "getRotations", "Lnet/ccbluex/liquidbounce/utils/aiming/data/Rotation;", "target", "getCrosshairTarget", "Lnet/minecraft/world/phys/BlockHitResult;", "rotation", "getFacePositionFactoryForConfig", "Lnet/ccbluex/liquidbounce/utils/block/targetfinding/FaceTargetPositionFactory;", "afterJumpEvent", "Lnet/ccbluex/liquidbounce/event/EventHook;", "Lnet/ccbluex/liquidbounce/event/events/PlayerAfterJumpEvent;", "getAfterJumpEvent$annotations", "liquidbounce"})
@SourceDebugExtension(value={"SMAP\nScaffoldNormalTechnique.kt\nKotlin\n*S Kotlin\n*F\n+ 1 ScaffoldNormalTechnique.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/techniques/ScaffoldNormalTechnique\n+ 2 MinecraftVectorExtensions.kt\nnet/ccbluex/liquidbounce/utils/math/MinecraftVectorExtensionsKt\n+ 3 ValueGroup.kt\nnet/ccbluex/liquidbounce/config/types/group/ValueGroup\n+ 4 enum-set.kt\nnet/ccbluex/fastutil/enum-set\n+ 5 EventListener.kt\nnet/ccbluex/liquidbounce/event/EventListenerKt\n*L\n1#1,182:1\n203#2,5:183\n558#3:188\n216#4:189\n99#5:190\n*S KotlinDebug\n*F\n+ 1 ScaffoldNormalTechnique.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/techniques/ScaffoldNormalTechnique\n*L\n114#1:183,5\n-1#1:188\n-1#1:189\n-1#1:190\n*E\n"})
public final class ScaffoldNormalTechnique
extends ScaffoldTechnique {
    @NotNull
    public static final ScaffoldNormalTechnique INSTANCE;
    static final /* synthetic */ KProperty<Object>[] $$delegatedProperties;
    @NotNull
    private static final ChoiceListValue aimMode$delegate;
    @NotNull
    private static final Value requiresSight$delegate;
    private static double randomization;
    @NotNull
    private static final EventHook<PlayerAfterJumpEvent> afterJumpEvent;

    private ScaffoldNormalTechnique() {
        super("Normal", null);
    }

    private final AimMode getAimMode() {
        return (AimMode)aimMode$delegate.getValue((Object)this, $$delegatedProperties[0]);
    }

    private final boolean getRequiresSight() {
        return (Boolean)requiresSight$delegate.getValue((Object)this, $$delegatedProperties[1]);
    }

    @Override
    @Nullable
    public BlockPlacementTarget findPlacementTarget(@NotNull Vec3 predictedPos, @NotNull Pose predictedPose, @Nullable Line optimalLine, @NotNull ItemStack bestStack) {
        Intrinsics.checkNotNullParameter((Object)predictedPos, (String)"predictedPos");
        Intrinsics.checkNotNullParameter((Object)predictedPose, (String)"predictedPose");
        Intrinsics.checkNotNullParameter((Object)bestStack, (String)"bestStack");
        Comparator<BlockPos> priorityComparator = this.priorityComparator(predictedPos, optimalLine);
        List offsets = ModuleFreeze.INSTANCE.getRunning() ? BlockPosOffsets.FULL.getOffsets() : (ScaffoldDownFeature.INSTANCE.getShouldGoDown() ? BlockPosOffsets.DOWN.getOffsets() : BlockPosOffsets.NORMAL.getOffsets());
        FaceTargetPositionFactory facePositionFactory = this.getFacePositionFactoryForConfig(predictedPos, predictedPose, optimalLine);
        BlockPlacementTargetFindingOptions searchOptions = new BlockPlacementTargetFindingOptions(new BlockOffsetOptions(offsets, priorityComparator), new FaceHandlingOptions(facePositionFactory, ScaffoldDownFeature.INSTANCE.getShouldGoDown()), bestStack, new PlayerLocationOnPlacement(predictedPos, predictedPose));
        Vec3 $this$toBlockPos_u24default$iv = predictedPos;
        double xOffset$iv = 0.0;
        double yOffset$iv = 0.0;
        double zOffset$iv = 0.0;
        boolean $i$f$toBlockPos = false;
        BlockPos blockPos = BlockPos.containing((double)($this$toBlockPos_u24default$iv.x + xOffset$iv), (double)($this$toBlockPos_u24default$iv.y + yOffset$iv), (double)($this$toBlockPos_u24default$iv.z + zOffset$iv));
        Intrinsics.checkNotNullExpressionValue((Object)blockPos, (String)"containing(...)");
        return TargetFindingKt.findBestBlockPlacementTarget((BlockPos)ModuleScaffold.INSTANCE.getTargetedPosition$liquidbounce(blockPos), (BlockPlacementTargetFindingOptions)searchOptions);
    }

    @Override
    @Nullable
    public Rotation getRotations(@Nullable BlockPlacementTarget target) {
        if (ScaffoldTellyFeature.INSTANCE.getEnabled() && ScaffoldTellyFeature.INSTANCE.getDoNotAim()) {
            return switch (WhenMappings.$EnumSwitchMapping$0[ScaffoldTellyFeature.INSTANCE.getResetMode().ordinal()]) {
                case 1 -> new Rotation((float)Math.rint(EntityExtensionsKt.getRotation((Entity)((Entity)this.getPlayer())).yaw() / (float)45) * (float)45, this.getPlayer().getXRot() < 45.0f ? 45.0f : this.getPlayer().getXRot(), false, 4, null);
                case 2 -> null;
                default -> throw new NoWhenBranchMatchedException();
            };
        }
        if (this.getRequiresSight()) {
            BlockPlacementTarget blockPlacementTarget = target;
            if (blockPlacementTarget == null) {
                return null;
            }
            BlockPlacementTarget target2 = blockPlacementTarget;
            BlockHitResult raycast = RaytracingKt.traceFromPlayer$default((Rotation)target2.getRotation(), (double)0.0, null, (boolean)false, (float)0.0f, (int)30, null);
            if (raycast.getType() == HitResult.Type.BLOCK && Intrinsics.areEqual((Object)raycast.getBlockPos(), (Object)target2.getInteractedBlockPos())) {
                return target2.getRotation();
            }
        }
        return super.getRotations(target);
    }

    @Override
    @Nullable
    public BlockHitResult getCrosshairTarget(@Nullable BlockPlacementTarget target, @NotNull Rotation rotation) {
        Intrinsics.checkNotNullParameter((Object)rotation, (String)"rotation");
        BlockPlacementTarget blockPlacementTarget = target;
        if (blockPlacementTarget == null) {
            return null;
        }
        BlockHitResult crosshairTarget = super.getCrosshairTarget(blockPlacementTarget, rotation);
        if (crosshairTarget != null && target.doesCrosshairTargetMatchRequirements(crosshairTarget)) {
            return crosshairTarget;
        }
        if (ScaffoldDownFeature.INSTANCE.getShouldGoDown()) {
            return target.getBlockHitResult();
        }
        return null;
    }

    private final FaceTargetPositionFactory getFacePositionFactoryForConfig(Vec3 predictedPos, Pose predictedPose, Line optimalLine) {
        Vec3 vec3 = predictedPos.add(0.0, (double)this.getPlayer().getEyeHeight(predictedPose), 0.0);
        Intrinsics.checkNotNullExpressionValue((Object)vec3, (String)"add(...)");
        PositionFactoryConfiguration config = new PositionFactoryConfiguration(vec3, randomization);
        return switch (WhenMappings.$EnumSwitchMapping$1[this.getAimMode().ordinal()]) {
            case 1 -> (FaceTargetPositionFactory)CenterTargetPositionFactory.INSTANCE;
            case 2 -> (FaceTargetPositionFactory)RandomTargetPositionFactory.INSTANCE;
            case 3 -> (FaceTargetPositionFactory)new StabilizedRotationTargetPositionFactory(config, optimalLine);
            case 4 -> (FaceTargetPositionFactory)new NearestRotationTargetPositionFactory(config);
            case 5 -> (FaceTargetPositionFactory)new ReverseYawTargetPositionFactory(config);
            case 6 -> (FaceTargetPositionFactory)new DiagonalYawTargetPositionFactory(config);
            case 7 -> (FaceTargetPositionFactory)new AngleYawTargetPositionFactory(config);
            case 8 -> (FaceTargetPositionFactory)new EdgePointTargetPositionFactory(config);
            default -> throw new NoWhenBranchMatchedException();
        };
    }

    private static /* synthetic */ void getAfterJumpEvent$annotations() {
    }

    private static final void afterJumpEvent$lambda$0(PlayerAfterJumpEvent it) {
        Intrinsics.checkNotNullParameter((Object)it, (String)"it");
        randomization = Random.Default.nextDouble(-0.01, 0.01);
    }

    /*
     * WARNING - void declaration
     */
    static {
        void priority$iv;
        void $this$handler$iv;
        void name$iv22;
        EventListener this_$iv;
        ValueGroup valueGroup = new ValueGroup[]{Reflection.property1((PropertyReference1)((PropertyReference1)new PropertyReference1Impl(ScaffoldNormalTechnique.class, "aimMode", "getAimMode()Lnet/ccbluex/liquidbounce/utils/block/targetfinding/AimMode;", 0))), Reflection.property1((PropertyReference1)((PropertyReference1)new PropertyReference1Impl(ScaffoldNormalTechnique.class, "requiresSight", "getRequiresSight()Z", 0)))};
        $$delegatedProperties = valueGroup;
        INSTANCE = new ScaffoldNormalTechnique();
        valueGroup = (ValueGroup)INSTANCE;
        String string = "RotationMode";
        Enum default$iv = (Enum)AimMode.STABILIZED;
        boolean $i$f$enumChoice = false;
        Tagged tagged = (Tagged)default$iv;
        boolean $i$f$enumSetAllOf = false;
        EnumSet<AimMode> enumSet = EnumSet.allOf(AimMode.class);
        Intrinsics.checkNotNullExpressionValue(enumSet, (String)"allOf(...)");
        aimMode$delegate = this_$iv.enumChoice((String)name$iv22, tagged, (Set)enumSet);
        requiresSight$delegate = ValueGroup.boolean$default((ValueGroup)((ValueGroup)INSTANCE), (String)"RequiresSight", (boolean)false, null, (int)4, null);
        INSTANCE.tree((ValueGroup)ScaffoldEagleFeature.INSTANCE);
        INSTANCE.tree((ValueGroup)ScaffoldTellyFeature.INSTANCE);
        INSTANCE.tree((ValueGroup)ScaffoldDownFeature.INSTANCE);
        INSTANCE.tree((ValueGroup)ScaffoldStabilizeMovementFeature.INSTANCE);
        INSTANCE.tree((ValueGroup)ScaffoldCeilingFeature.INSTANCE);
        INSTANCE.tree((ValueGroup)ScaffoldHeadHitterFeature.INSTANCE);
        randomization = Random.Default.nextDouble(-0.02, 0.02);
        this_$iv = (EventListener)INSTANCE;
        int name$iv22 = -50;
        Consumer<PlayerAfterJumpEvent> handler$iv = ScaffoldNormalTechnique::afterJumpEvent$lambda$0;
        boolean $i$f$handler = false;
        afterJumpEvent = EventListenerKt.handler((EventListener)$this$handler$iv, PlayerAfterJumpEvent.class, (short)priority$iv, handler$iv);
    }

    @Metadata(mv={2, 3, 0}, k=3, xi=50)
    public static final class WhenMappings {
        public static final /* synthetic */ int[] $EnumSwitchMapping$0;
        public static final /* synthetic */ int[] $EnumSwitchMapping$1;

        static {
            int[] nArray = new int[ScaffoldTellyFeature.Mode.values().length];
            try {
                nArray[ScaffoldTellyFeature.Mode.REVERSE.ordinal()] = 1;
            }
            catch (NoSuchFieldError noSuchFieldError) {
                // empty catch block
            }
            try {
                nArray[ScaffoldTellyFeature.Mode.RESET.ordinal()] = 2;
            }
            catch (NoSuchFieldError noSuchFieldError) {
                // empty catch block
            }
            $EnumSwitchMapping$0 = nArray;
            nArray = new int[AimMode.values().length];
            try {
                nArray[AimMode.CENTER.ordinal()] = 1;
            }
            catch (NoSuchFieldError noSuchFieldError) {
                // empty catch block
            }
            try {
                nArray[AimMode.RANDOM.ordinal()] = 2;
            }
            catch (NoSuchFieldError noSuchFieldError) {
                // empty catch block
            }
            try {
                nArray[AimMode.STABILIZED.ordinal()] = 3;
            }
            catch (NoSuchFieldError noSuchFieldError) {
                // empty catch block
            }
            try {
                nArray[AimMode.NEAREST_ROTATION.ordinal()] = 4;
            }
            catch (NoSuchFieldError noSuchFieldError) {
                // empty catch block
            }
            try {
                nArray[AimMode.REVERSE_YAW.ordinal()] = 5;
            }
            catch (NoSuchFieldError noSuchFieldError) {
                // empty catch block
            }
            try {
                nArray[AimMode.DIAGONAL_YAW.ordinal()] = 6;
            }
            catch (NoSuchFieldError noSuchFieldError) {
                // empty catch block
            }
            try {
                nArray[AimMode.ANGLE_YAW.ordinal()] = 7;
            }
            catch (NoSuchFieldError noSuchFieldError) {
                // empty catch block
            }
            try {
                nArray[AimMode.EDGE_POINT.ordinal()] = 8;
            }
            catch (NoSuchFieldError noSuchFieldError) {
                // empty catch block
            }
            $EnumSwitchMapping$1 = nArray;
        }
    }
}

