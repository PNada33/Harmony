/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  kotlin.Metadata
 *  kotlin.collections.ArrayDeque
 *  kotlin.jvm.internal.Intrinsics
 *  kotlin.jvm.internal.PropertyReference1
 *  kotlin.jvm.internal.PropertyReference1Impl
 *  kotlin.jvm.internal.Reflection
 *  kotlin.jvm.internal.SourceDebugExtension
 *  kotlin.ranges.ClosedFloatingPointRange
 *  kotlin.ranges.IntRange
 *  kotlin.ranges.RangesKt
 *  kotlin.reflect.KProperty
 *  net.ccbluex.liquidbounce.LiquidBounce
 *  net.ccbluex.liquidbounce.config.types.RangedValue
 *  net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
 *  net.ccbluex.liquidbounce.config.types.group.ValueGroup
 *  net.ccbluex.liquidbounce.event.EventListener
 *  net.ccbluex.liquidbounce.utils.entity.EntityExtensionsKt
 *  net.ccbluex.liquidbounce.utils.math.MinecraftVectorExtensionsKt
 *  net.ccbluex.liquidbounce.utils.math.geometry.Line
 *  net.ccbluex.liquidbounce.utils.movement.MovementUtilsKt
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.core.Position
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features;

import kotlin.Metadata;
import kotlin.collections.ArrayDeque;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.PropertyReference1;
import kotlin.jvm.internal.PropertyReference1Impl;
import kotlin.jvm.internal.Reflection;
import kotlin.jvm.internal.SourceDebugExtension;
import kotlin.ranges.ClosedFloatingPointRange;
import kotlin.ranges.IntRange;
import kotlin.ranges.RangesKt;
import kotlin.reflect.KProperty;
import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.config.types.RangedValue;
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup;
import net.ccbluex.liquidbounce.config.types.group.ValueGroup;
import net.ccbluex.liquidbounce.event.EventListener;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold;
import net.ccbluex.liquidbounce.utils.entity.EntityExtensionsKt;
import net.ccbluex.liquidbounce.utils.math.MinecraftVectorExtensionsKt;
import net.ccbluex.liquidbounce.utils.math.geometry.Line;
import net.ccbluex.liquidbounce.utils.movement.MovementUtilsKt;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Position;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Metadata(mv={2, 3, 0}, k=1, xi=50, d1={"\u0000:\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u0007\n\u0002\b\f\n\u0002\u0010\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\b\n\u0002\u0010\u0006\n\u0000\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003J\u0006\u0010\u0016\u001a\u00020\u0017J\b\u0010\u0018\u001a\u00020\u0017H\u0016J\u001a\u0010\u0019\u001a\u00020\u00172\b\u0010\u001a\u001a\u0004\u0018\u00010\u001b2\b\u0010\u001c\u001a\u0004\u0018\u00010\u0006J\b\u0010\u001d\u001a\u0004\u0018\u00010\u0006J\u0012\u0010\u001e\u001a\u0004\u0018\u00010\u00062\b\u0010\u001a\u001a\u0004\u0018\u00010\u001bJ\u0010\u0010\u001f\u001a\u0004\u0018\u00010\u00062\u0006\u0010\u001a\u001a\u00020\u001bJ\u0018\u0010 \u001a\u00020\u00062\u0006\u0010!\u001a\u00020\u00062\u0006\u0010\"\u001a\u00020\u0006H\u0002J\b\u0010#\u001a\u00020$H\u0002R\u0014\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082T\u00a2\u0006\u0002\n\u0000R\u001b\u0010\t\u001a\u00020\n8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\r\u0010\u000e\u001a\u0004\b\u000b\u0010\fR\u001b\u0010\u000f\u001a\u00020\n8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0011\u0010\u000e\u001a\u0004\b\u0010\u0010\fR\u001b\u0010\u0012\u001a\u00020\b8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0015\u0010\u000e\u001a\u0004\b\u0013\u0010\u0014\u00a8\u0006%"}, d2={"Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/ScaffoldMovementPrediction;", "Lnet/ccbluex/liquidbounce/config/types/group/ToggleableValueGroup;", "<init>", "()V", "lastPlacementOffsets", "Lkotlin/collections/ArrayDeque;", "Lnet/minecraft/world/phys/Vec3;", "MAX_PLACEMENT_OFFSETS", "", "bootstrapBackoff", "", "getBootstrapBackoff", "()F", "bootstrapBackoff$delegate", "Lnet/ccbluex/liquidbounce/config/types/RangedValue;", "predictionCutoffDistance", "getPredictionCutoffDistance", "predictionCutoffDistance$delegate", "warmupPlacements", "getWarmupPlacements", "()I", "warmupPlacements$delegate", "reset", "", "onDisabled", "onPlace", "optimalLine", "Lnet/ccbluex/liquidbounce/utils/math/geometry/Line;", "lastFallOffPosition", "getAvgPlacementPos", "getPredictedPlacementPos", "getFallOffPositionOnLine", "getBootstrapPlacementPos", "fallOffPoint", "fallOffPointToPlayer", "getWarmupBlendFactor", "", "liquidbounce"})
@SourceDebugExtension(value={"SMAP\nScaffoldMovementPrediction.kt\nKotlin\n*S Kotlin\n*F\n+ 1 ScaffoldMovementPrediction.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/ScaffoldMovementPrediction\n+ 2 MinecraftVectorExtensions.kt\nnet/ccbluex/liquidbounce/utils/math/MinecraftVectorExtensionsKt\n*L\n1#1,151:1\n111#2:152\n111#2:153\n96#2:154\n96#2:155\n149#2:156\n111#2:157\n*S KotlinDebug\n*F\n+ 1 ScaffoldMovementPrediction.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/ScaffoldMovementPrediction\n*L\n68#1:152\n109#1:153\n114#1:154\n125#1:155\n129#1:156\n139#1:157\n*E\n"})
public final class ScaffoldMovementPrediction
extends ToggleableValueGroup {
    @NotNull
    public static final ScaffoldMovementPrediction INSTANCE;
    static final /* synthetic */ KProperty<Object>[] $$delegatedProperties;
    @NotNull
    private static final ArrayDeque<Vec3> lastPlacementOffsets;
    private static final int MAX_PLACEMENT_OFFSETS = 4;
    @NotNull
    private static final RangedValue bootstrapBackoff$delegate;
    @NotNull
    private static final RangedValue predictionCutoffDistance$delegate;
    @NotNull
    private static final RangedValue warmupPlacements$delegate;

    private ScaffoldMovementPrediction() {
        super((EventListener)ModuleScaffold.INSTANCE, "Prediction", true, null, 8, null);
    }

    private final float getBootstrapBackoff() {
        return ((Number)bootstrapBackoff$delegate.getValue((Object)this, $$delegatedProperties[0])).floatValue();
    }

    private final float getPredictionCutoffDistance() {
        return ((Number)predictionCutoffDistance$delegate.getValue((Object)this, $$delegatedProperties[1])).floatValue();
    }

    private final int getWarmupPlacements() {
        return ((Number)warmupPlacements$delegate.getValue((Object)this, $$delegatedProperties[2])).intValue();
    }

    public final void reset() {
        lastPlacementOffsets.clear();
    }

    public void onDisabled() {
        this.reset();
        super.onDisabled();
    }

    /*
     * WARNING - void declaration
     */
    public final void onPlace(@Nullable Line optimalLine, @Nullable Vec3 lastFallOffPosition) {
        void $this$minus$iv;
        if (optimalLine == null || !this.getEnabled()) {
            return;
        }
        Vec3 vec3 = lastFallOffPosition;
        if (vec3 == null) {
            return;
        }
        Vec3 fallOffPoint = vec3;
        float lineDirAngle = (float)Math.atan2(optimalLine.getDirection().z, optimalLine.getDirection().x);
        Vec3 vec32 = this.getPlayer().position();
        Intrinsics.checkNotNullExpressionValue((Object)vec32, (String)"position(...)");
        Vec3 vec33 = vec32;
        Position other$iv = (Position)fallOffPoint;
        boolean $i$f$minus = false;
        Vec3 vec34 = $this$minus$iv.subtract(other$iv.x(), other$iv.y(), other$iv.z());
        Intrinsics.checkNotNullExpressionValue((Object)vec34, (String)"subtract(...)");
        Vec3 vec35 = vec34.yRot(lineDirAngle);
        Intrinsics.checkNotNullExpressionValue((Object)vec35, (String)"yRot(...)");
        Vec3 unrotatedOffset = vec35;
        Vec3 x = this.getAvgPlacementPos();
        if (x != null) {
            LiquidBounce.INSTANCE.getLogger().debug((Object)x.distanceTo(unrotatedOffset));
        }
        lastPlacementOffsets.addLast((Object)unrotatedOffset);
        if (lastPlacementOffsets.size() > 4) {
            lastPlacementOffsets.removeFirst();
        }
    }

    @Nullable
    public final Vec3 getAvgPlacementPos() {
        if (lastPlacementOffsets.isEmpty()) {
            return null;
        }
        return MinecraftVectorExtensionsKt.average((Iterable)((Iterable)lastPlacementOffsets));
    }

    /*
     * WARNING - void declaration
     */
    @Nullable
    public final Vec3 getPredictedPlacementPos(@Nullable Line optimalLine) {
        void $this$plus$iv;
        void $this$minus$iv;
        Vec3 fallOffPoint;
        if (optimalLine == null || !this.getEnabled()) {
            return null;
        }
        if (EntityExtensionsKt.isCloseToEdge$default((LocalPlayer)this.getPlayer(), null, (double)this.getPredictionCutoffDistance(), null, (int)5, null)) {
            return null;
        }
        Vec3 vec3 = this.getFallOffPositionOnLine(optimalLine);
        if (vec3 == null) {
            return null;
        }
        Vec3 vec32 = fallOffPoint = vec3;
        Vec3 vec33 = this.getPlayer().position();
        Intrinsics.checkNotNullExpressionValue((Object)vec33, (String)"position(...)");
        Position other$iv = (Position)vec33;
        boolean $i$f$minus = false;
        Vec3 vec34 = $this$minus$iv.subtract(other$iv.x(), other$iv.y(), other$iv.z());
        Intrinsics.checkNotNullExpressionValue((Object)vec34, (String)"subtract(...)");
        Vec3 fallOffPointToPlayer = vec34;
        Vec3 bootstrapPos = this.getBootstrapPlacementPos(fallOffPoint, fallOffPointToPlayer);
        Vec3 vec35 = this.getAvgPlacementPos();
        if (vec35 == null) {
            return bootstrapPos;
        }
        Vec3 last = vec35;
        float lineDirAngle = (float)Math.atan2(optimalLine.getDirection().z, optimalLine.getDirection().x);
        Vec3 vec36 = fallOffPoint;
        Vec3 vec37 = last.yRot(-lineDirAngle);
        Intrinsics.checkNotNullExpressionValue((Object)vec37, (String)"yRot(...)");
        Position other$iv2 = (Position)vec37;
        boolean $i$f$plus = false;
        Vec3 vec38 = $this$plus$iv.add(other$iv2.x(), other$iv2.y(), other$iv2.z());
        Intrinsics.checkNotNullExpressionValue((Object)vec38, (String)"add(...)");
        Vec3 predictedPos = vec38;
        return bootstrapPos.lerp(predictedPos, this.getWarmupBlendFactor());
    }

    /*
     * WARNING - void declaration
     */
    @Nullable
    public final Vec3 getFallOffPositionOnLine(@NotNull Line optimalLine) {
        void $this$copy_u24default$iv;
        Vec3 edgeCollision;
        void $this$plus$iv;
        Vec3 fromLine;
        Intrinsics.checkNotNullParameter((Object)optimalLine, (String)"optimalLine");
        Vec3 vec3 = this.getPlayer().position();
        Intrinsics.checkNotNullExpressionValue((Object)vec3, (String)"position(...)");
        Vec3 nearestPosToPlayer = optimalLine.getNearestPointTo(vec3);
        Vec3 vec32 = nearestPosToPlayer.add(0.0, -0.1, 0.0);
        Intrinsics.checkNotNullExpressionValue((Object)vec32, (String)"add(...)");
        Vec3 vec33 = fromLine = vec32;
        Position other$iv = (Position)MinecraftVectorExtensionsKt.withLength((Vec3)optimalLine.getDirection(), (double)3.0);
        boolean $i$f$plus22 = false;
        Vec3 vec34 = $this$plus$iv.add(other$iv.x(), other$iv.y(), other$iv.z());
        Intrinsics.checkNotNullExpressionValue((Object)vec34, (String)"add(...)");
        Vec3 toLine = vec34;
        Vec3 vec35 = MovementUtilsKt.findEdgeCollision$default((Vec3)fromLine, (Vec3)toLine, (float)0.0f, (int)4, null);
        if (vec35 == null) {
            return null;
        }
        Vec3 $i$f$plus22 = edgeCollision = vec35;
        double y$iv = this.getPlayer().getY();
        double x$iv = $this$copy_u24default$iv.x;
        double z$iv = $this$copy_u24default$iv.z;
        boolean $i$f$copy = false;
        Vec3 fallOffPoint = new Vec3(x$iv, y$iv, z$iv);
        return fallOffPoint;
    }

    /*
     * WARNING - void declaration
     */
    private final Vec3 getBootstrapPlacementPos(Vec3 fallOffPoint, Vec3 fallOffPointToPlayer) {
        void $this$minus$iv;
        if (this.getBootstrapBackoff() <= 0.0f) {
            return fallOffPoint;
        }
        Vec3 vec3 = fallOffPoint;
        Position other$iv = (Position)MinecraftVectorExtensionsKt.withLength((Vec3)fallOffPointToPlayer, (double)this.getBootstrapBackoff());
        boolean $i$f$minus = false;
        Vec3 vec32 = $this$minus$iv.subtract(other$iv.x(), other$iv.y(), other$iv.z());
        Intrinsics.checkNotNullExpressionValue((Object)vec32, (String)"subtract(...)");
        return vec32;
    }

    private final double getWarmupBlendFactor() {
        if (this.getWarmupPlacements() <= 0) {
            return 1.0;
        }
        return RangesKt.coerceIn((double)((double)lastPlacementOffsets.size() / (double)this.getWarmupPlacements()), (double)0.0, (double)1.0);
    }

    static {
        KProperty[] kPropertyArray = new KProperty[]{Reflection.property1((PropertyReference1)((PropertyReference1)new PropertyReference1Impl(ScaffoldMovementPrediction.class, "bootstrapBackoff", "getBootstrapBackoff()F", 0))), Reflection.property1((PropertyReference1)((PropertyReference1)new PropertyReference1Impl(ScaffoldMovementPrediction.class, "predictionCutoffDistance", "getPredictionCutoffDistance()F", 0))), Reflection.property1((PropertyReference1)((PropertyReference1)new PropertyReference1Impl(ScaffoldMovementPrediction.class, "warmupPlacements", "getWarmupPlacements()I", 0)))};
        $$delegatedProperties = kPropertyArray;
        INSTANCE = new ScaffoldMovementPrediction();
        lastPlacementOffsets = new ArrayDeque(5);
        bootstrapBackoff$delegate = ValueGroup.float$default((ValueGroup)((ValueGroup)INSTANCE), (String)"BootstrapBackoff", (float)0.2f, (ClosedFloatingPointRange)RangesKt.rangeTo((float)0.0f, (float)0.4f), null, null, (int)24, null);
        predictionCutoffDistance$delegate = ValueGroup.float$default((ValueGroup)((ValueGroup)INSTANCE), (String)"PredictionCutoffDistance", (float)0.05f, (ClosedFloatingPointRange)RangesKt.rangeTo((float)0.0f, (float)0.3f), null, null, (int)24, null);
        warmupPlacements$delegate = ValueGroup.int$default((ValueGroup)((ValueGroup)INSTANCE), (String)"WarmupPlacements", (int)2, (IntRange)new IntRange(0, 4), null, null, (int)24, null);
    }
}

