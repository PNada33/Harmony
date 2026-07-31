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
 *  kotlin.ranges.IntRange
 *  kotlin.reflect.KProperty
 *  net.ccbluex.liquidbounce.config.types.RangedValue
 *  net.ccbluex.liquidbounce.config.types.group.ValueGroup
 *  net.ccbluex.liquidbounce.utils.aiming.data.Rotation
 *  net.ccbluex.liquidbounce.utils.block.targetfinding.BlockOffsetOptions
 *  net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTarget
 *  net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTargetFindingOptions
 *  net.ccbluex.liquidbounce.utils.block.targetfinding.CenterTargetPositionFactory
 *  net.ccbluex.liquidbounce.utils.block.targetfinding.FaceHandlingOptions
 *  net.ccbluex.liquidbounce.utils.block.targetfinding.FaceTargetPositionFactory
 *  net.ccbluex.liquidbounce.utils.block.targetfinding.PlayerLocationOnPlacement
 *  net.ccbluex.liquidbounce.utils.block.targetfinding.TargetFindingKt
 *  net.ccbluex.liquidbounce.utils.math.geometry.Line
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.entity.Pose
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques;

import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.PropertyReference1;
import kotlin.jvm.internal.PropertyReference1Impl;
import kotlin.jvm.internal.Reflection;
import kotlin.jvm.internal.SourceDebugExtension;
import kotlin.ranges.IntRange;
import kotlin.reflect.KProperty;
import net.ccbluex.liquidbounce.config.types.RangedValue;
import net.ccbluex.liquidbounce.config.types.group.ValueGroup;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.ScaffoldTechnique;
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation;
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockOffsetOptions;
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTarget;
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTargetFindingOptions;
import net.ccbluex.liquidbounce.utils.block.targetfinding.CenterTargetPositionFactory;
import net.ccbluex.liquidbounce.utils.block.targetfinding.FaceHandlingOptions;
import net.ccbluex.liquidbounce.utils.block.targetfinding.FaceTargetPositionFactory;
import net.ccbluex.liquidbounce.utils.block.targetfinding.PlayerLocationOnPlacement;
import net.ccbluex.liquidbounce.utils.block.targetfinding.TargetFindingKt;
import net.ccbluex.liquidbounce.utils.math.geometry.Line;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Metadata(mv={2, 3, 0}, k=1, xi=50, d1={"\u0000P\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0007\n\u0000\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003J,\u0010\n\u001a\u0004\u0018\u00010\u000b2\u0006\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u000f2\b\u0010\u0010\u001a\u0004\u0018\u00010\u00112\u0006\u0010\u0012\u001a\u00020\u0013H\u0016J\u0014\u0010\u0014\u001a\u0004\u0018\u00010\u00152\b\u0010\u0016\u001a\u0004\u0018\u00010\u000bH\u0016J\u001c\u0010\u0017\u001a\u0004\u0018\u00010\u00182\b\u0010\u0016\u001a\u0004\u0018\u00010\u000b2\u0006\u0010\u0019\u001a\u00020\u0015H\u0016J\"\u0010\u001a\u001a\u00020\u001b2\u0006\u0010\u001c\u001a\u00020\r2\u0006\u0010\u001d\u001a\u00020\u00052\b\b\u0002\u0010\u001e\u001a\u00020\u001fH\u0002R\u001b\u0010\u0004\u001a\u00020\u00058BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\b\u0010\t\u001a\u0004\b\u0006\u0010\u0007\u00a8\u0006 "}, d2={"Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/techniques/ScaffoldExpandTechnique;", "Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/techniques/ScaffoldTechnique;", "<init>", "()V", "expandLength", "", "getExpandLength", "()I", "expandLength$delegate", "Lnet/ccbluex/liquidbounce/config/types/RangedValue;", "findPlacementTarget", "Lnet/ccbluex/liquidbounce/utils/block/targetfinding/BlockPlacementTarget;", "predictedPos", "Lnet/minecraft/world/phys/Vec3;", "predictedPose", "Lnet/minecraft/world/entity/Pose;", "optimalLine", "Lnet/ccbluex/liquidbounce/utils/math/geometry/Line;", "bestStack", "Lnet/minecraft/world/item/ItemStack;", "getRotations", "Lnet/ccbluex/liquidbounce/utils/aiming/data/Rotation;", "target", "getCrosshairTarget", "Lnet/minecraft/world/phys/BlockHitResult;", "rotation", "expandPos", "Lnet/minecraft/core/BlockPos;", "position", "expand", "yaw", "", "liquidbounce"})
@SourceDebugExtension(value={"SMAP\nScaffoldExpandTechnique.kt\nKotlin\n*S Kotlin\n*F\n+ 1 ScaffoldExpandTechnique.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/techniques/ScaffoldExpandTechnique\n+ 2 MinecraftVectorExtensions.kt\nnet/ccbluex/liquidbounce/utils/math/MinecraftVectorExtensionsKt\n+ 3 MathExtensions.kt\nnet/ccbluex/liquidbounce/utils/math/MathExtensionsKt\n*L\n1#1,95:1\n203#2,5:96\n28#3:101\n28#3:102\n*S KotlinDebug\n*F\n+ 1 ScaffoldExpandTechnique.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/techniques/ScaffoldExpandTechnique\n*L\n88#1:96,5\n89#1:101\n91#1:102\n*E\n"})
public final class ScaffoldExpandTechnique
extends ScaffoldTechnique {
    @NotNull
    public static final ScaffoldExpandTechnique INSTANCE;
    static final /* synthetic */ KProperty<Object>[] $$delegatedProperties;
    @NotNull
    private static final RangedValue expandLength$delegate;

    private ScaffoldExpandTechnique() {
        super("Expand", null);
    }

    private final int getExpandLength() {
        return ((Number)expandLength$delegate.getValue((Object)this, $$delegatedProperties[0])).intValue();
    }

    @Override
    @Nullable
    public BlockPlacementTarget findPlacementTarget(@NotNull Vec3 predictedPos, @NotNull Pose predictedPose, @Nullable Line optimalLine, @NotNull ItemStack bestStack) {
        Intrinsics.checkNotNullParameter((Object)predictedPos, (String)"predictedPos");
        Intrinsics.checkNotNullParameter((Object)predictedPose, (String)"predictedPose");
        Intrinsics.checkNotNullParameter((Object)bestStack, (String)"bestStack");
        BlockPlacementTargetFindingOptions searchOptions = new BlockPlacementTargetFindingOptions(BlockOffsetOptions.Default, new FaceHandlingOptions((FaceTargetPositionFactory)CenterTargetPositionFactory.INSTANCE, true), bestStack, new PlayerLocationOnPlacement(predictedPos, predictedPose));
        int i = 0;
        int n = this.getExpandLength();
        if (i <= n) {
            while (true) {
                BlockPos position = ModuleScaffold.INSTANCE.getTargetedPosition$liquidbounce(ScaffoldExpandTechnique.expandPos$default(this, predictedPos, i, 0.0f, 4, null));
                BlockPlacementTarget blockPlacementTarget = TargetFindingKt.findBestBlockPlacementTarget((BlockPos)position, (BlockPlacementTargetFindingOptions)searchOptions);
                if (blockPlacementTarget != null) {
                    return blockPlacementTarget;
                }
                if (i == n) break;
                ++i;
            }
        }
        return null;
    }

    @Override
    @Nullable
    public Rotation getRotations(@Nullable BlockPlacementTarget target) {
        BlockPlacementTarget blockPlacementTarget = target;
        if (blockPlacementTarget == null || (blockPlacementTarget = blockPlacementTarget.getPlacedBlock()) == null || (blockPlacementTarget = blockPlacementTarget.getCenter()) == null) {
            return null;
        }
        BlockPlacementTarget blockCenter = blockPlacementTarget;
        Vec3 vec3 = this.getPlayer().getEyePosition();
        Intrinsics.checkNotNullExpressionValue((Object)vec3, (String)"getEyePosition(...)");
        return Rotation.Companion.lookingAt((Vec3)blockCenter, vec3);
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
        return target.getBlockHitResult();
    }

    private final BlockPos expandPos(Vec3 position, int expand, float yaw) {
        Vec3 $this$toBlockPos_u24default$iv = position;
        double xOffset$iv = 0.0;
        double yOffset$iv = 0.0;
        double zOffset$iv = 0.0;
        boolean $i$f$toBlockPos = false;
        BlockPos blockPos = BlockPos.containing((double)($this$toBlockPos_u24default$iv.x + xOffset$iv), (double)($this$toBlockPos_u24default$iv.y + yOffset$iv), (double)($this$toBlockPos_u24default$iv.z + zOffset$iv));
        Intrinsics.checkNotNullExpressionValue((Object)blockPos, (String)"containing(...)");
        float $this$toRadians$iv = yaw;
        boolean $i$f$toRadians = false;
        int n = (int)(-((float)Math.sin($this$toRadians$iv * ((float)Math.PI / 180))) * (float)expand);
        $this$toRadians$iv = yaw;
        $i$f$toRadians = false;
        BlockPos blockPos2 = blockPos.offset(n, 0, (int)((float)Math.cos($this$toRadians$iv * ((float)Math.PI / 180)) * (float)expand));
        Intrinsics.checkNotNullExpressionValue((Object)blockPos2, (String)"offset(...)");
        return blockPos2;
    }

    static /* synthetic */ BlockPos expandPos$default(ScaffoldExpandTechnique scaffoldExpandTechnique, Vec3 vec3, int n, float f, int n2, Object object) {
        if ((n2 & 4) != 0) {
            f = scaffoldExpandTechnique.getPlayer().getYRot();
        }
        return scaffoldExpandTechnique.expandPos(vec3, n, f);
    }

    static {
        KProperty[] kPropertyArray = new KProperty[]{Reflection.property1((PropertyReference1)((PropertyReference1)new PropertyReference1Impl(ScaffoldExpandTechnique.class, "expandLength", "getExpandLength()I", 0)))};
        $$delegatedProperties = kPropertyArray;
        INSTANCE = new ScaffoldExpandTechnique();
        expandLength$delegate = ValueGroup.int$default((ValueGroup)((ValueGroup)INSTANCE), (String)"Length", (int)4, (IntRange)new IntRange(1, 10), (String)"blocks", null, (int)16, null);
    }
}

