/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Supplier
 *  com.google.common.base.Suppliers
 *  kotlin.Metadata
 *  kotlin.collections.CollectionsKt
 *  kotlin.enums.EnumEntries
 *  kotlin.enums.EnumEntriesKt
 *  kotlin.jvm.internal.Intrinsics
 *  kotlin.jvm.internal.PropertyReference1
 *  kotlin.jvm.internal.PropertyReference1Impl
 *  kotlin.jvm.internal.Reflection
 *  kotlin.jvm.internal.SourceDebugExtension
 *  kotlin.random.Random
 *  kotlin.ranges.IntRange
 *  kotlin.ranges.RangesKt
 *  kotlin.reflect.KProperty
 *  net.ccbluex.liquidbounce.config.types.RangedValue
 *  net.ccbluex.liquidbounce.config.types.group.ValueGroup
 *  net.ccbluex.liquidbounce.config.types.list.MultiChoiceListValue
 *  net.ccbluex.liquidbounce.config.types.list.Tagged
 *  net.ccbluex.liquidbounce.features.misc.DebuggedOwner
 *  net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
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
 *  net.ccbluex.liquidbounce.utils.entity.PlayerSimulationCache
 *  net.ccbluex.liquidbounce.utils.entity.SimulatedPlayerCache
 *  net.ccbluex.liquidbounce.utils.entity.SimulatedPlayerSnapshot
 *  net.ccbluex.liquidbounce.utils.math.geometry.Line
 *  net.ccbluex.liquidbounce.utils.movement.DirectionalInput
 *  net.ccbluex.liquidbounce.utils.raytracing.RaytracingKt
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.world.entity.Pose
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques;

import com.google.common.base.Suppliers;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Supplier;
import kotlin.Metadata;
import kotlin.collections.CollectionsKt;
import kotlin.enums.EnumEntries;
import kotlin.enums.EnumEntriesKt;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.PropertyReference1;
import kotlin.jvm.internal.PropertyReference1Impl;
import kotlin.jvm.internal.Reflection;
import kotlin.jvm.internal.SourceDebugExtension;
import kotlin.random.Random;
import kotlin.ranges.IntRange;
import kotlin.ranges.RangesKt;
import kotlin.reflect.KProperty;
import net.ccbluex.liquidbounce.config.types.RangedValue;
import net.ccbluex.liquidbounce.config.types.group.ValueGroup;
import net.ccbluex.liquidbounce.config.types.list.MultiChoiceListValue;
import net.ccbluex.liquidbounce.config.types.list.Tagged;
import net.ccbluex.liquidbounce.features.misc.DebuggedOwner;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.LedgeAction;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldLedgeExtension;
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
import net.ccbluex.liquidbounce.utils.entity.PlayerSimulationCache;
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayerCache;
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayerSnapshot;
import net.ccbluex.liquidbounce.utils.math.geometry.Line;
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput;
import net.ccbluex.liquidbounce.utils.raytracing.RaytracingKt;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Metadata(mv={2, 3, 0}, k=1, xi=50, d1={"\u0000h\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010#\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\b\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0007\n\u0002\b\u0004\b\u00c6\u0002\u0018\u00002\u00020\u00012\u00020\u0002:\u0001/B\t\b\u0002\u00a2\u0006\u0004\b\u0003\u0010\u0004J\u001a\u0010\u0018\u001a\u00020\u00192\b\u0010\u001a\u001a\u0004\u0018\u00010\u001b2\u0006\u0010\u001c\u001a\u00020\u001dH\u0016J,\u0010 \u001a\u0004\u0018\u00010\u001b2\u0006\u0010!\u001a\u00020\"2\u0006\u0010#\u001a\u00020$2\b\u0010%\u001a\u0004\u0018\u00010&2\u0006\u0010'\u001a\u00020(H\u0016J\u0014\u0010)\u001a\u0004\u0018\u00010\u001d2\b\u0010\u001a\u001a\u0004\u0018\u00010\u001bH\u0016J\u0010\u0010*\u001a\u00020\u001d2\u0006\u0010+\u001a\u00020,H\u0002J\u0010\u0010-\u001a\u00020\u001d2\u0006\u0010+\u001a\u00020,H\u0002J\u0010\u0010.\u001a\u00020\u001d2\u0006\u0010\u001a\u001a\u00020\u001bH\u0002R)\u0010\u0005\u001a\u0010\u0012\f\u0012\n \b*\u0004\u0018\u00010\u00070\u00070\u00068BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u000b\u0010\f\u001a\u0004\b\t\u0010\nR\u001b\u0010\r\u001a\u00020\u000e8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0011\u0010\u0012\u001a\u0004\b\u000f\u0010\u0010R\u001b\u0010\u0013\u001a\u00020\u00148BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0017\u0010\u0012\u001a\u0004\b\u0015\u0010\u0016R\u000e\u0010\u001e\u001a\u00020\u001fX\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u00060"}, d2={"Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/techniques/ScaffoldGodBridgeTechnique;", "Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/techniques/ScaffoldTechnique;", "Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/ScaffoldLedgeExtension;", "<init>", "()V", "modes", "", "Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/techniques/ScaffoldGodBridgeTechnique$Mode;", "kotlin.jvm.PlatformType", "getModes", "()Ljava/util/Set;", "modes$delegate", "Lnet/ccbluex/liquidbounce/config/types/list/MultiChoiceListValue;", "forceSneakBelowCount", "", "getForceSneakBelowCount", "()I", "forceSneakBelowCount$delegate", "Lnet/ccbluex/liquidbounce/config/types/RangedValue;", "sneakTime", "Lkotlin/ranges/IntRange;", "getSneakTime", "()Lkotlin/ranges/IntRange;", "sneakTime$delegate", "ledge", "Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/LedgeAction;", "target", "Lnet/ccbluex/liquidbounce/utils/block/targetfinding/BlockPlacementTarget;", "rotation", "Lnet/ccbluex/liquidbounce/utils/aiming/data/Rotation;", "isOnRightSide", "", "findPlacementTarget", "predictedPos", "Lnet/minecraft/world/phys/Vec3;", "predictedPose", "Lnet/minecraft/world/entity/Pose;", "optimalLine", "Lnet/ccbluex/liquidbounce/utils/math/geometry/Line;", "bestStack", "Lnet/minecraft/world/item/ItemStack;", "getRotations", "getRotationForStraightInput", "movingYaw", "", "getRotationForDiagonalInput", "getRotationForNoInput", "Mode", "liquidbounce"})
@SourceDebugExtension(value={"SMAP\nScaffoldGodBridgeTechnique.kt\nKotlin\n*S Kotlin\n*F\n+ 1 ScaffoldGodBridgeTechnique.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/techniques/ScaffoldGodBridgeTechnique\n+ 2 ModuleDebug.kt\nnet/ccbluex/liquidbounce/features/module/modules/render/ModuleDebug\n+ 3 MinecraftVectorExtensions.kt\nnet/ccbluex/liquidbounce/utils/math/MinecraftVectorExtensionsKt\n+ 4 MathExtensions.kt\nnet/ccbluex/liquidbounce/utils/math/MathExtensionsKt\n+ 5 BlockExtensions.kt\nnet/ccbluex/liquidbounce/utils/block/BlockExtensionsKt\n+ 6 ValueGroup.kt\nnet/ccbluex/liquidbounce/config/types/group/ValueGroup\n+ 7 enum-set.kt\nnet/ccbluex/fastutil/enum-set\n*L\n1#1,198:1\n285#2,6:199\n285#2,6:205\n285#2,6:211\n285#2,6:217\n203#3,5:223\n203#3,5:230\n28#4:228\n28#4:229\n130#5:235\n130#5:236\n522#6:237\n531#6,4:241\n537#6:246\n187#7,3:238\n216#7:245\n*S KotlinDebug\n*F\n+ 1 ScaffoldGodBridgeTechnique.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/techniques/ScaffoldGodBridgeTechnique\n*L\n89#1:199,6\n102#1:205,6\n103#1:211,6\n114#1:217,6\n139#1:223,5\n170#1:230,5\n165#1:228\n166#1:229\n172#1:235\n173#1:236\n-1#1:237\n-1#1:241,4\n-1#1:246\n-1#1:238,3\n-1#1:245\n*E\n"})
public final class ScaffoldGodBridgeTechnique
extends ScaffoldTechnique
implements ScaffoldLedgeExtension {
    @NotNull
    public static final ScaffoldGodBridgeTechnique INSTANCE;
    static final /* synthetic */ KProperty<Object>[] $$delegatedProperties;
    @NotNull
    private static final MultiChoiceListValue modes$delegate;
    @NotNull
    private static final RangedValue forceSneakBelowCount$delegate;
    @NotNull
    private static final RangedValue sneakTime$delegate;
    private static boolean isOnRightSide;

    private ScaffoldGodBridgeTechnique() {
        super("GodBridge", null);
    }

    private final Set<Mode> getModes() {
        return (Set)modes$delegate.getValue((Object)this, $$delegatedProperties[0]);
    }

    private final int getForceSneakBelowCount() {
        return ((Number)forceSneakBelowCount$delegate.getValue((Object)this, $$delegatedProperties[1])).intValue();
    }

    private final IntRange getSneakTime() {
        return (IntRange)sneakTime$delegate.getValue((Object)this, $$delegatedProperties[2]);
    }

    /*
     * WARNING - void declaration
     */
    @Override
    @NotNull
    public LedgeAction ledge(@Nullable BlockPlacementTarget target, @NotNull Rotation rotation) {
        LedgeAction ledgeAction;
        Boolean bl;
        ModuleDebug moduleDebug;
        DebuggedOwner debuggedOwner;
        String string;
        void this_$iv;
        Intrinsics.checkNotNullParameter((Object)rotation, (String)"rotation");
        if (!this.isSelected$liquidbounce()) {
            return LedgeAction.NO_LEDGE;
        }
        SimulatedPlayerCache simulatedPlayerCache = PlayerSimulationCache.INSTANCE.getSimulationForLocalPlayer();
        SimulatedPlayerSnapshot snapshotOne = simulatedPlayerCache.getSnapshotAt(1);
        ModuleDebug moduleDebug2 = ModuleDebug.INSTANCE;
        DebuggedOwner debuggedOwner2 = (DebuggedOwner)this;
        String name$iv = "Snapshot Ledged";
        boolean $i$f$debugParameter = false;
        if (this_$iv.getRunning()) {
            void $this$debugParameter$iv;
            string = name$iv;
            debuggedOwner = $this$debugParameter$iv;
            moduleDebug = this_$iv;
            boolean bl2 = false;
            bl = snapshotOne.getClipLedged();
            moduleDebug.debugParameter(debuggedOwner, string, (Object)bl);
        }
        if (snapshotOne.getClipLedged()) {
            void this_$iv2;
            DebuggedOwner $this$debugParameter$iv;
            ModuleDebug this_$iv3;
            Vec3 vec3 = snapshotOne.getPos().add(0.0, (double)this.getPlayer().getEyeHeight(), 0.0);
            Intrinsics.checkNotNullExpressionValue((Object)vec3, (String)"add(...)");
            Vec3 cameraPosition = vec3;
            BlockHitResult currentCrosshairTarget = RaytracingKt.traceFromPoint$default((double)0.0, null, (boolean)false, (Vec3)cameraPosition, (Vec3)rotation.directionVector(), null, (int)39, null);
            if (target == null) {
                return LedgeAction.NO_LEDGE;
            }
            boolean targetFulfillsRequirements = target.doesCrosshairTargetMatchRequirements(currentCrosshairTarget);
            boolean isValidCrosshairTarget = ModuleScaffold.INSTANCE.isValidCrosshairTarget$liquidbounce(currentCrosshairTarget);
            ModuleDebug bl2 = ModuleDebug.INSTANCE;
            Object object = (DebuggedOwner)this;
            String name$iv2 = "targetFulfillsRequirements";
            boolean $i$f$debugParameter2 = false;
            if (this_$iv3.getRunning()) {
                string = name$iv2;
                debuggedOwner = $this$debugParameter$iv;
                moduleDebug = this_$iv3;
                boolean bl3 = false;
                bl = targetFulfillsRequirements;
                moduleDebug.debugParameter(debuggedOwner, string, (Object)bl);
            }
            this_$iv3 = ModuleDebug.INSTANCE;
            $this$debugParameter$iv = (DebuggedOwner)this;
            name$iv2 = "isValidCrosshairTarget";
            $i$f$debugParameter2 = false;
            if (this_$iv3.getRunning()) {
                string = name$iv2;
                debuggedOwner = $this$debugParameter$iv;
                moduleDebug = this_$iv3;
                boolean bl4 = false;
                bl = isValidCrosshairTarget;
                moduleDebug.debugParameter(debuggedOwner, string, (Object)bl);
            }
            if (targetFulfillsRequirements && isValidCrosshairTarget) {
                return LedgeAction.NO_LEDGE;
            }
            Mode currentMode = ModuleScaffold.INSTANCE.getBlockCount() < this.getForceSneakBelowCount() ? Mode.SNEAK : (Mode)((Object)CollectionsKt.random((Collection)this.getModes(), (Random)((Random)Random.Default)));
            LedgeAction ledgeAction2 = currentMode.getCreator().get();
            Intrinsics.checkNotNullExpressionValue((Object)ledgeAction2, (String)"get(...)");
            object = ledgeAction2;
            LedgeAction it = (LedgeAction)object;
            boolean bl5 = false;
            ModuleDebug bl4 = ModuleDebug.INSTANCE;
            DebuggedOwner debuggedOwner3 = (DebuggedOwner)INSTANCE;
            String name$iv3 = "LastLedgeAction";
            boolean $i$f$debugParameter3 = false;
            if (this_$iv2.getRunning()) {
                void $this$debugParameter$iv2;
                String string2 = name$iv3;
                void var18_25 = $this$debugParameter$iv2;
                void var19_26 = this_$iv2;
                boolean bl6 = false;
                LedgeAction ledgeAction3 = it;
                var19_26.debugParameter((DebuggedOwner)var18_25, string2, (Object)ledgeAction3);
            }
            ledgeAction = (LedgeAction)object;
        } else {
            ledgeAction = LedgeAction.NO_LEDGE;
        }
        return ledgeAction;
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

    /*
     * Unable to fully structure code
     */
    private final Rotation getRotationForStraightInput(float movingYaw) {
        block3: {
            if (!this.getPlayer().onGround()) break block3;
            $this$toRadians$iv = movingYaw;
            $i$f$toRadians = false;
            if (!(Math.floor(this.getPlayer().getX() + (double)((float)Math.cos($this$toRadians$iv * 0.017453292f)) * 0.5) == Math.floor(this.getPlayer().getX()))) ** GOTO lbl-1000
            $this$toRadians$iv = movingYaw;
            $i$f$toRadians = false;
            if (!(Math.floor(this.getPlayer().getZ() + (double)((float)Math.sin($this$toRadians$iv * 0.017453292f)) * 0.5) == Math.floor(this.getPlayer().getZ()))) lbl-1000:
            // 2 sources

            {
                v0 = true;
            } else {
                v0 = false;
            }
            ScaffoldGodBridgeTechnique.isOnRightSide = v0;
            v1 = this.getPlayer().position().relative(Direction.fromYRot((double)movingYaw), 0.6);
            Intrinsics.checkNotNullExpressionValue((Object)v1, (String)"relative(...)");
            $this$toBlockPos_u24default$iv = v1;
            xOffset$iv = 0.0;
            yOffset$iv = 0.0;
            zOffset$iv = 0.0;
            $i$f$toBlockPos = false;
            v2 = BlockPos.containing((double)($this$toBlockPos_u24default$iv.x + xOffset$iv), (double)($this$toBlockPos_u24default$iv.y + yOffset$iv), (double)($this$toBlockPos_u24default$iv.z + zOffset$iv));
            Intrinsics.checkNotNullExpressionValue((Object)v2, (String)"containing(...)");
            posInDirection = v2;
            v3 = this.getPlayer().blockPosition().below();
            Intrinsics.checkNotNullExpressionValue((Object)v3, (String)"below(...)");
            $this$getState$iv = v3;
            $i$f$getState-deprecated = false;
            v4 = BlockExtensionsKt.getState((BlockPos)$this$getState$iv);
            isLeaningOffBlock = v4 != null ? v4.isAir() : false;
            v5 = posInDirection.below();
            Intrinsics.checkNotNullExpressionValue((Object)v5, (String)"below(...)");
            $this$getState$iv = v5;
            $i$f$getState-deprecated = false;
            v6 = BlockExtensionsKt.getState((BlockPos)$this$getState$iv);
            v7 = v6 != null ? v6.isAir() : (nextBlockIsAir = false);
            if (isLeaningOffBlock && nextBlockIsAir) {
                ScaffoldGodBridgeTechnique.isOnRightSide = ScaffoldGodBridgeTechnique.isOnRightSide == false;
            }
        }
        finalYaw = movingYaw + (float)(ScaffoldGodBridgeTechnique.isOnRightSide != false ? 45 : -45);
        return new Rotation(finalYaw, 75.7f, false, 4, null);
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

    /*
     * WARNING - void declaration
     */
    static {
        void default$iv$iv;
        void name$iv$iv;
        void $this$iv$iv;
        void default$iv;
        void name$iv;
        void this_$iv;
        ValueGroup valueGroup = new ValueGroup[]{Reflection.property1((PropertyReference1)((PropertyReference1)new PropertyReference1Impl(ScaffoldGodBridgeTechnique.class, "modes", "getModes()Ljava/util/Set;", 0))), Reflection.property1((PropertyReference1)((PropertyReference1)new PropertyReference1Impl(ScaffoldGodBridgeTechnique.class, "forceSneakBelowCount", "getForceSneakBelowCount()I", 0))), Reflection.property1((PropertyReference1)((PropertyReference1)new PropertyReference1Impl(ScaffoldGodBridgeTechnique.class, "sneakTime", "getSneakTime()Lkotlin/ranges/IntRange;", 0)))};
        $$delegatedProperties = valueGroup;
        INSTANCE = new ScaffoldGodBridgeTechnique();
        valueGroup = (ValueGroup)INSTANCE;
        String string = "Modes";
        Enum[] enumArray = new Mode[]{Mode.JUMP};
        enumArray = enumArray;
        boolean canBeNone$iv = false;
        boolean $i$f$multiEnumChoice = false;
        void var5_5 = this_$iv;
        void var6_6 = name$iv;
        EnumSet<Mode> $this$toEnumSet$iv$iv = default$iv;
        boolean $i$f$toEnumSet = false;
        EnumSet<Mode> set$iv$iv = EnumSet.noneOf(Mode.class);
        for (void var12_14 : $this$toEnumSet$iv$iv) {
            set$iv$iv.add((Mode)var12_14);
        }
        Intrinsics.checkNotNull(set$iv$iv);
        $this$toEnumSet$iv$iv = set$iv$iv;
        boolean canBeNone$iv$iv = canBeNone$iv;
        boolean $i$f$enumSetAllOf = false;
        EnumSet<Mode> enumSet = EnumSet.allOf(Mode.class);
        Intrinsics.checkNotNullExpressionValue(enumSet, (String)"allOf(...)");
        EnumSet<Mode> choices$iv$iv = enumSet;
        boolean $i$f$multiEnumChoice2 = false;
        modes$delegate = $this$iv$iv.multiEnumChoice((String)name$iv$iv, (Set)default$iv$iv, (Set)choices$iv$iv, canBeNone$iv$iv, false);
        forceSneakBelowCount$delegate = ValueGroup.int$default((ValueGroup)((ValueGroup)INSTANCE), (String)"ForceSneakBelowCount", (int)3, (IntRange)new IntRange(0, 10), null, null, (int)24, null);
        sneakTime$delegate = ValueGroup.intRange$default((ValueGroup)((ValueGroup)INSTANCE), (String)"SneakTime", (IntRange)new IntRange(1, 1), (IntRange)new IntRange(1, 10), null, null, (int)24, null);
    }

    @Metadata(mv={2, 3, 0}, k=1, xi=50, d1={"\u0000 \n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\r\b\u0082\u0081\u0002\u0018\u00002\u00020\u00012\b\u0012\u0004\u0012\u00020\u00000\u0002B\u001f\b\u0002\u0012\u0006\u0010\u0003\u001a\u00020\u0004\u0012\f\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006\u00a2\u0006\u0004\b\b\u0010\tB\u0019\b\u0012\u0012\u0006\u0010\u0003\u001a\u00020\u0004\u0012\u0006\u0010\n\u001a\u00020\u0007\u00a2\u0006\u0004\b\b\u0010\u000bR\u0014\u0010\u0003\u001a\u00020\u0004X\u0096\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\rR\u0017\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\u000fj\u0002\b\u0010j\u0002\b\u0011j\u0002\b\u0012j\u0002\b\u0013\u00a8\u0006\u0014"}, d2={"Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/techniques/ScaffoldGodBridgeTechnique$Mode;", "Lnet/ccbluex/liquidbounce/config/types/list/Tagged;", "", "tag", "", "creator", "Ljava/util/function/Supplier;", "Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/LedgeAction;", "<init>", "(Ljava/lang/String;ILjava/lang/String;Ljava/util/function/Supplier;)V", "ledgeAction", "(Ljava/lang/String;ILjava/lang/String;Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/LedgeAction;)V", "getTag", "()Ljava/lang/String;", "getCreator", "()Ljava/util/function/Supplier;", "JUMP", "SNEAK", "STOP_INPUT", "BACKWARDS", "liquidbounce"})
    private static final class Mode
    extends Enum<Mode>
    implements Tagged {
        @NotNull
        private final String tag;
        @NotNull
        private final Supplier<LedgeAction> creator;
        public static final /* enum */ Mode JUMP = new Mode("Jump", new LedgeAction(true, 0, false, false, 14, null));
        public static final /* enum */ Mode SNEAK = new Mode("Sneak", Mode::_init_$lambda$0);
        public static final /* enum */ Mode STOP_INPUT = new Mode("StopInput", new LedgeAction(false, 0, true, false, 11, null));
        public static final /* enum */ Mode BACKWARDS = new Mode("Backwards", new LedgeAction(false, 0, false, true, 7, null));
        private static final /* synthetic */ Mode[] $VALUES;
        private static final /* synthetic */ EnumEntries $ENTRIES;

        private Mode(String tag, Supplier<LedgeAction> creator) {
            this.tag = tag;
            this.creator = creator;
        }

        @NotNull
        public String getTag() {
            return this.tag;
        }

        @NotNull
        public final Supplier<LedgeAction> getCreator() {
            return this.creator;
        }

        private Mode(String tag, LedgeAction ledgeAction) {
            com.google.common.base.Supplier supplier = Suppliers.ofInstance((Object)ledgeAction);
            Intrinsics.checkNotNullExpressionValue((Object)supplier, (String)"ofInstance(...)");
            this(tag, (Supplier)supplier);
        }

        public static Mode[] values() {
            return (Mode[])$VALUES.clone();
        }

        public static Mode valueOf(String value) {
            return Enum.valueOf(Mode.class, value);
        }

        @NotNull
        public static EnumEntries<Mode> getEntries() {
            return $ENTRIES;
        }

        private static final LedgeAction _init_$lambda$0() {
            return new LedgeAction(false, RangesKt.random((IntRange)INSTANCE.getSneakTime(), (Random)((Random)Random.Default)), false, false, 13, null);
        }

        static {
            $VALUES = modeArray = new Mode[]{Mode.JUMP, Mode.SNEAK, Mode.STOP_INPUT, Mode.BACKWARDS};
            $ENTRIES = EnumEntriesKt.enumEntries((Enum[])$VALUES);
        }
    }
}

