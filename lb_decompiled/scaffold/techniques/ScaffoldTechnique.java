/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  kotlin.Metadata
 *  kotlin.jvm.internal.DefaultConstructorMarker
 *  kotlin.jvm.internal.Intrinsics
 *  net.ccbluex.liquidbounce.config.types.group.Mode
 *  net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
 *  net.ccbluex.liquidbounce.utils.aiming.data.Rotation
 *  net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTarget
 *  net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTargetFindingOptions
 *  net.ccbluex.liquidbounce.utils.math.geometry.Line
 *  net.ccbluex.liquidbounce.utils.raytracing.RaytracingKt
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.entity.Pose
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques;

import java.util.Comparator;
import kotlin.Metadata;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import net.ccbluex.liquidbounce.config.types.group.Mode;
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.ScaffoldBreezilyTechnique;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.ScaffoldExpandTechnique;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.ScaffoldGodBridgeTechnique;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.ScaffoldNormalTechnique;
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation;
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTarget;
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTargetFindingOptions;
import net.ccbluex.liquidbounce.utils.math.geometry.Line;
import net.ccbluex.liquidbounce.utils.raytracing.RaytracingKt;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Metadata(mv={2, 3, 0}, k=1, xi=50, d1={"\u0000f\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\b6\u0018\u00002\u00020\u0001B\u0011\b\u0004\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0004\b\u0004\u0010\u0005J,\u0010\n\u001a\u0004\u0018\u00010\u000b2\u0006\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u000f2\b\u0010\u0010\u001a\u0004\u0018\u00010\u00112\u0006\u0010\u0012\u001a\u00020\u0013H&J\u0014\u0010\u0014\u001a\u0004\u0018\u00010\u00152\b\u0010\u0016\u001a\u0004\u0018\u00010\u000bH\u0016J\u001c\u0010\u0017\u001a\u0004\u0018\u00010\u00182\b\u0010\u0016\u001a\u0004\u0018\u00010\u000b2\u0006\u0010\u0019\u001a\u00020\u0015H\u0016J*\u0010\u001a\u001a\u0012\u0012\u0004\u0012\u00020\u001c0\u001bj\b\u0012\u0004\u0012\u00020\u001c`\u001d2\u0006\u0010\f\u001a\u00020\r2\b\u0010\u0010\u001a\u0004\u0018\u00010\u0011H\u0004R\u0017\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00000\u00078F\u00a2\u0006\u0006\u001a\u0004\b\b\u0010\t\u0082\u0001\u0004\u001e\u001f !\u00a8\u0006\""}, d2={"Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/techniques/ScaffoldTechnique;", "Lnet/ccbluex/liquidbounce/config/types/group/Mode;", "name", "", "<init>", "(Ljava/lang/String;)V", "parent", "Lnet/ccbluex/liquidbounce/config/types/group/ModeValueGroup;", "getParent", "()Lnet/ccbluex/liquidbounce/config/types/group/ModeValueGroup;", "findPlacementTarget", "Lnet/ccbluex/liquidbounce/utils/block/targetfinding/BlockPlacementTarget;", "predictedPos", "Lnet/minecraft/world/phys/Vec3;", "predictedPose", "Lnet/minecraft/world/entity/Pose;", "optimalLine", "Lnet/ccbluex/liquidbounce/utils/math/geometry/Line;", "bestStack", "Lnet/minecraft/world/item/ItemStack;", "getRotations", "Lnet/ccbluex/liquidbounce/utils/aiming/data/Rotation;", "target", "getCrosshairTarget", "Lnet/minecraft/world/phys/BlockHitResult;", "rotation", "priorityComparator", "Ljava/util/Comparator;", "Lnet/minecraft/core/BlockPos;", "Lkotlin/Comparator;", "Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/techniques/ScaffoldBreezilyTechnique;", "Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/techniques/ScaffoldExpandTechnique;", "Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/techniques/ScaffoldGodBridgeTechnique;", "Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/techniques/ScaffoldNormalTechnique;", "liquidbounce"})
public abstract sealed class ScaffoldTechnique
extends Mode
permits ScaffoldBreezilyTechnique, ScaffoldExpandTechnique, ScaffoldGodBridgeTechnique, ScaffoldNormalTechnique {
    private ScaffoldTechnique(String name) {
        super(name, null, 2, null);
    }

    @NotNull
    public final ModeValueGroup<ScaffoldTechnique> getParent() {
        return ModuleScaffold.INSTANCE.getTechnique$liquidbounce();
    }

    @Nullable
    public abstract BlockPlacementTarget findPlacementTarget(@NotNull Vec3 var1, @NotNull Pose var2, @Nullable Line var3, @NotNull ItemStack var4);

    @Nullable
    public Rotation getRotations(@Nullable BlockPlacementTarget target) {
        BlockPlacementTarget blockPlacementTarget = target;
        return blockPlacementTarget != null ? blockPlacementTarget.getRotation() : null;
    }

    @Nullable
    public BlockHitResult getCrosshairTarget(@Nullable BlockPlacementTarget target, @NotNull Rotation rotation) {
        Intrinsics.checkNotNullParameter((Object)rotation, (String)"rotation");
        return RaytracingKt.traceFromPlayer$default((Rotation)rotation, (double)0.0, null, (boolean)false, (float)0.0f, (int)30, null);
    }

    @NotNull
    protected final Comparator<BlockPos> priorityComparator(@NotNull Vec3 predictedPos, @Nullable Line optimalLine) {
        Intrinsics.checkNotNullParameter((Object)predictedPos, (String)"predictedPos");
        return optimalLine != null ? BlockPlacementTargetFindingOptions.Companion.leastBlockDistanceToLine(optimalLine) : BlockPlacementTargetFindingOptions.Companion.leastBlockDistanceToPos(predictedPos);
    }

    public /* synthetic */ ScaffoldTechnique(String name, DefaultConstructorMarker $constructor_marker) {
        this(name);
    }
}

