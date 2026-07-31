/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
 *  kotlin.Metadata
 *  kotlin.collections.ArrayDeque
 *  kotlin.collections.CollectionsKt
 *  kotlin.jvm.internal.Intrinsics
 *  kotlin.jvm.internal.SourceDebugExtension
 *  net.ccbluex.liquidbounce.features.misc.DebuggedOwner
 *  net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
 *  net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug$DebuggedBox
 *  net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug$DebuggedGeometry
 *  net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug$DebuggedLine
 *  net.ccbluex.liquidbounce.render.engine.type.Color4b
 *  net.ccbluex.liquidbounce.utils.block.BlockExtensionsKt
 *  net.ccbluex.liquidbounce.utils.entity.EntityExtensionsKt
 *  net.ccbluex.liquidbounce.utils.math.geometry.Line
 *  net.ccbluex.liquidbounce.utils.movement.DirectionalInput
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Vec3i
 *  net.minecraft.util.Mth
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.util.List;
import kotlin.Metadata;
import kotlin.collections.ArrayDeque;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.SourceDebugExtension;
import net.ccbluex.liquidbounce.features.misc.DebuggedOwner;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold;
import net.ccbluex.liquidbounce.render.engine.type.Color4b;
import net.ccbluex.liquidbounce.utils.block.BlockExtensionsKt;
import net.ccbluex.liquidbounce.utils.entity.EntityExtensionsKt;
import net.ccbluex.liquidbounce.utils.math.geometry.Line;
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Metadata(mv={2, 3, 0}, k=1, xi=50, d1={"\u0000V\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010 \n\u0000\n\u0002\u0010\u0013\n\u0002\b\u0003\n\u0002\u0010\u0007\n\u0002\b\u0004\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003J\u0010\u0010\n\u001a\u0004\u0018\u00010\u000b2\u0006\u0010\f\u001a\u00020\rJ\u0018\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u000b2\u0006\u0010\u0011\u001a\u00020\u0012H\u0002J\n\u0010\u0013\u001a\u0004\u0018\u00010\u000bH\u0002J\u0016\u0010\u0014\u001a\u00020\u00152\f\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\b0\u0017H\u0002J\n\u0010\u001a\u001a\u0004\u0018\u00010\bH\u0002J\u0010\u0010\u001b\u001a\u00020\u00122\u0006\u0010\u001c\u001a\u00020\u001dH\u0002J\u000e\u0010\u001e\u001a\u00020\u00152\u0006\u0010\u001f\u001a\u00020\bJ\u0006\u0010 \u001a\u00020\u0015R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082T\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\b0\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0010\u0010\t\u001a\u0004\u0018\u00010\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0018\u001a\u00020\u0019X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006!"}, d2={"Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/ScaffoldMovementPlanner;", "", "<init>", "()V", "MAX_LAST_PLACE_BLOCKS", "", "lastPlacedBlocks", "Lkotlin/collections/ArrayDeque;", "Lnet/minecraft/core/BlockPos;", "lastPosition", "getOptimalMovementLine", "Lnet/ccbluex/liquidbounce/utils/math/geometry/Line;", "directionalInput", "Lnet/ccbluex/liquidbounce/utils/movement/DirectionalInput;", "divergesTooMuchFromDirection", "", "lastBlocksLine", "direction", "Lnet/minecraft/world/phys/Vec3;", "fitLinesThroughLastPlacedBlocks", "debugLastPlacedBlocks", "", "lastPlacedBlocksToConsider", "", "offsetsToTry", "", "findBlockPlayerStandsOn", "chooseDirection", "currentAngle", "", "trackPlacedBlock", "target", "reset", "liquidbounce"})
@SourceDebugExtension(value={"SMAP\nScaffoldMovementPlanner.kt\nKotlin\n*S Kotlin\n*F\n+ 1 ScaffoldMovementPlanner.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/ScaffoldMovementPlanner\n+ 2 MinecraftExtensions.kt\nnet/ccbluex/liquidbounce/utils/client/MinecraftExtensionsKt\n+ 3 MinecraftVectorExtensions.kt\nnet/ccbluex/liquidbounce/utils/math/MinecraftVectorExtensionsKt\n+ 4 ModuleDebug.kt\nnet/ccbluex/liquidbounce/features/module/modules/render/ModuleDebug\n+ 5 _Collections.kt\nkotlin/collections/CollectionsKt___CollectionsKt\n+ 6 mutable-set-factory.kt\nnet/ccbluex/fastutil/mutable-set-factory\n+ 7 fake.kt\nkotlin/jvm/internal/FakeKt\n*L\n1#1,191:1\n45#2:192\n43#2:193\n45#2:199\n43#2:200\n45#2:228\n43#2:229\n47#2:231\n43#2:232\n192#3,5:194\n192#3,5:207\n116#3:212\n192#3,5:213\n207#3:230\n269#4,6:201\n269#4,6:220\n1924#5,2:218\n1926#5:226\n94#6:227\n1#7:233\n*S KotlinDebug\n*F\n+ 1 ScaffoldMovementPlanner.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/ScaffoldMovementPlanner\n*L\n51#1:192\n51#1:193\n67#1:199\n67#1:200\n127#1:228\n127#1:229\n129#1:231\n129#1:232\n63#1:194,5\n97#1:207,5\n97#1:212\n98#1:213,5\n127#1:230\n70#1:201,6\n108#1:220,6\n105#1:218,2\n105#1:226\n123#1:227\n*E\n"})
public final class ScaffoldMovementPlanner {
    @NotNull
    public static final ScaffoldMovementPlanner INSTANCE = new ScaffoldMovementPlanner();
    private static final int MAX_LAST_PLACE_BLOCKS = 4;
    @NotNull
    private static final ArrayDeque<BlockPos> lastPlacedBlocks = new ArrayDeque(4);
    @Nullable
    private static BlockPos lastPosition;
    @NotNull
    private static final double[] offsetsToTry;

    private ScaffoldMovementPlanner() {
    }

    /*
     * WARNING - void declaration
     */
    @Nullable
    public final Line getOptimalMovementLine(@NotNull DirectionalInput directionalInput) {
        void this_$iv;
        Vec3 vec3;
        Intrinsics.checkNotNullParameter((Object)directionalInput, (String)"directionalInput");
        boolean $i$f$getPlayer = false;
        boolean $i$f$getMc = false;
        Minecraft minecraft = Minecraft.getInstance();
        Intrinsics.checkNotNullExpressionValue((Object)minecraft, (String)"getInstance(...)");
        LocalPlayer localPlayer = minecraft.player;
        Intrinsics.checkNotNull((Object)localPlayer);
        Vec3 direction = this.chooseDirection(EntityExtensionsKt.getMovementDirectionOfInput((LocalPlayer)localPlayer, (DirectionalInput)directionalInput));
        BlockPos blockPos = this.findBlockPlayerStandsOn();
        if (blockPos == null) {
            return null;
        }
        BlockPos blockUnderPlayer = blockPos;
        Line lastBlocksLine = this.fitLinesThroughLastPlacedBlocks();
        if (lastBlocksLine != null && !this.divergesTooMuchFromDirection(lastBlocksLine, direction)) {
            vec3 = lastBlocksLine.getPosition();
        } else {
            Vec3i $this$toVec3d_u24default$iv = (Vec3i)blockUnderPlayer;
            double xOffset$iv = 0.0;
            double yOffset$iv = 0.0;
            double zOffset$iv = 0.0;
            boolean $i$f$toVec3d = false;
            vec3 = new Vec3((double)$this$toVec3d_u24default$iv.getX() + xOffset$iv, (double)$this$toVec3d_u24default$iv.getY() + yOffset$iv, (double)$this$toVec3d_u24default$iv.getZ() + zOffset$iv);
        }
        Vec3 lineBaseBlock = vec3;
        double d = lineBaseBlock.x + 0.5;
        boolean $i$f$getPlayer22 = false;
        boolean $i$f$getMc22 = false;
        Minecraft minecraft2 = Minecraft.getInstance();
        Intrinsics.checkNotNullExpressionValue((Object)minecraft2, (String)"getInstance(...)");
        LocalPlayer localPlayer2 = minecraft2.player;
        Intrinsics.checkNotNull((Object)localPlayer2);
        Line optimalLine = new Line(new Vec3(d, localPlayer2.position().y, lineBaseBlock.z + 0.5), direction);
        ModuleDebug $i$f$getPlayer22 = ModuleDebug.INSTANCE;
        DebuggedOwner $i$f$getMc22 = (DebuggedOwner)ModuleScaffold.INSTANCE;
        String name$iv = "optimalLine";
        boolean $i$f$debugGeometry = false;
        if (this_$iv.getRunning()) {
            void $this$debugGeometry$iv;
            String string = name$iv;
            void var15_21 = $this$debugGeometry$iv;
            void var14_22 = this_$iv;
            boolean bl = false;
            ModuleDebug.DebuggedGeometry debuggedGeometry = (ModuleDebug.DebuggedGeometry)new ModuleDebug.DebuggedLine(optimalLine, lastBlocksLine == null ? Color4b.RED : Color4b.GREEN);
            var14_22.debugGeometry((DebuggedOwner)var15_21, string, debuggedGeometry);
        }
        return optimalLine;
    }

    private final boolean divergesTooMuchFromDirection(Line lastBlocksLine, Vec3 direction) {
        return lastBlocksLine.getDirection().dot(direction) < 0.5;
    }

    /*
     * WARNING - void declaration
     */
    private final Line fitLinesThroughLastPlacedBlocks() {
        void $this$times$iv;
        if (lastPlacedBlocks.size() < 2) {
            return null;
        }
        BlockPos last = (BlockPos)lastPlacedBlocks.last();
        BlockPos secondToLast = (BlockPos)lastPlacedBlocks.get(lastPlacedBlocks.size() - 2);
        if (ModuleDebug.INSTANCE.getRunning()) {
            Object[] objectArray = new BlockPos[]{secondToLast, last};
            this.debugLastPlacedBlocks(CollectionsKt.listOf((Object[])objectArray));
        }
        BlockPos blockPos = secondToLast.offset((Vec3i)last);
        Intrinsics.checkNotNullExpressionValue((Object)blockPos, (String)"offset(...)");
        Vec3i $this$toVec3d_u24default$iv = (Vec3i)blockPos;
        double xOffset$iv = 0.0;
        double yOffset$iv = 0.0;
        double zOffset$iv = 0.0;
        boolean $i$f$toVec3d = false;
        $this$toVec3d_u24default$iv = new Vec3((double)$this$toVec3d_u24default$iv.getX() + xOffset$iv, (double)$this$toVec3d_u24default$iv.getY() + yOffset$iv, (double)$this$toVec3d_u24default$iv.getZ() + zOffset$iv);
        double scalar$iv = 0.5;
        boolean $i$f$times = false;
        Vec3 vec3 = $this$times$iv.scale(scalar$iv);
        Intrinsics.checkNotNullExpressionValue((Object)vec3, (String)"scale(...)");
        Vec3 avgPos = vec3;
        BlockPos blockPos2 = last.subtract((Vec3i)secondToLast);
        Intrinsics.checkNotNullExpressionValue((Object)blockPos2, (String)"subtract(...)");
        Vec3i $this$toVec3d_u24default$iv2 = (Vec3i)blockPos2;
        double xOffset$iv2 = 0.0;
        double yOffset$iv2 = 0.0;
        double zOffset$iv2 = 0.0;
        boolean $i$f$toVec3d2 = false;
        Vec3 vec32 = new Vec3((double)$this$toVec3d_u24default$iv2.getX() + xOffset$iv2, (double)$this$toVec3d_u24default$iv2.getY() + yOffset$iv2, (double)$this$toVec3d_u24default$iv2.getZ() + zOffset$iv2).normalize();
        Intrinsics.checkNotNullExpressionValue((Object)vec32, (String)"normalize(...)");
        Vec3 dir = vec32;
        return new Line(avgPos, dir);
    }

    /*
     * WARNING - void declaration
     */
    private final void debugLastPlacedBlocks(List<? extends BlockPos> lastPlacedBlocksToConsider) {
        Iterable $this$forEachIndexed$iv = lastPlacedBlocksToConsider;
        boolean $i$f$forEachIndexed = false;
        int index$iv = 0;
        for (Object item$iv : $this$forEachIndexed$iv) {
            void pos;
            void $this$debugGeometry$iv;
            void this_$iv;
            int n;
            if ((n = index$iv++) < 0) {
                CollectionsKt.throwIndexOverflow();
            }
            BlockPos blockPos = (BlockPos)item$iv;
            int idx = n;
            boolean bl = false;
            int alpha = (int)((1.0 - (double)idx / (double)lastPlacedBlocksToConsider.size()) * 200.0);
            ModuleDebug moduleDebug = ModuleDebug.INSTANCE;
            DebuggedOwner debuggedOwner = (DebuggedOwner)ModuleScaffold.INSTANCE;
            String name$iv = "lastPlacedBlock" + idx;
            boolean $i$f$debugGeometry = false;
            if (!this_$iv.getRunning()) continue;
            String string = name$iv;
            void var17_17 = $this$debugGeometry$iv;
            void var18_18 = this_$iv;
            boolean bl2 = false;
            ModuleDebug.DebuggedGeometry debuggedGeometry = (ModuleDebug.DebuggedGeometry)new ModuleDebug.DebuggedBox(new AABB((BlockPos)pos), new Color4b(133, 155, 255, alpha));
            var18_18.debugGeometry((DebuggedOwner)var17_17, string, debuggedGeometry);
        }
    }

    /*
     * Could not resolve type clashes
     * Unable to fully structure code
     */
    private final BlockPos findBlockPlayerStandsOn() {
        $i$f$objectHashSetOf = false;
        candidates = new ObjectOpenHashSet();
        for (double xOffset : ScaffoldMovementPlanner.offsetsToTry) {
            for (double zOffset : ScaffoldMovementPlanner.offsetsToTry) {
                $i$f$getPlayer = false;
                $i$f$getMc = false;
                v0 = Minecraft.getInstance();
                Intrinsics.checkNotNullExpressionValue((Object)v0, (String)"getInstance(...)");
                v1 = v0.player;
                Intrinsics.checkNotNull((Object)v1);
                Intrinsics.checkNotNullExpressionValue((Object)v1.position(), (String)"position(...)");
                $i$f$getPlayer = xOffset;
                var16_21 = -1.0;
                zOffset$iv = zOffset;
                $i$f$toBlockPos = false;
                Intrinsics.checkNotNullExpressionValue((Object)BlockPos.containing((double)($this$toBlockPos$iv.x + xOffset$iv), (double)($this$toBlockPos$iv.y + yOffset$iv), (double)($this$toBlockPos$iv.z + zOffset$iv)), (String)"containing(...)");
                v2 = BlockExtensionsKt.getState((BlockPos)playerPos);
                if (v2 == null) ** GOTO lbl-1000
                $i$f$getWorld = false;
                $i$f$getMc = false;
                v3 = Minecraft.getInstance();
                Intrinsics.checkNotNullExpressionValue((Object)v3, (String)"getInstance(...)");
                v4 = v3.level;
                Intrinsics.checkNotNull((Object)v4);
                if ((v2 = v2.getCollisionShape((BlockGetter)v4, playerPos)) != null) {
                    v5 = v2.isEmpty();
                } else lbl-1000:
                // 2 sources

                {
                    v5 = isEmpty = true;
                }
                if (isEmpty) continue;
                candidates.add((Object)playerPos);
            }
        }
        v6 = (BlockPos)ScaffoldMovementPlanner.lastPlacedBlocks.lastOrNull();
        if (v6 != null) {
            lastPlacedBlock = v6;
            $i$a$-let-ScaffoldMovementPlanner$findBlockPlayerStandsOn$1 = false;
            if (candidates.contains((Object)lastPlacedBlock)) {
                return lastPlacedBlock;
            }
        }
        if (candidates.contains((Object)ScaffoldMovementPlanner.lastPosition)) {
            return ScaffoldMovementPlanner.lastPosition;
        }
        var2_2 /* !! */  = (double[])CollectionsKt.firstOrNull((Iterable)((Iterable)candidates));
        it = (BlockPos)var2_2 /* !! */ ;
        $i$a$-also-ScaffoldMovementPlanner$findBlockPlayerStandsOn$2 = false;
        ScaffoldMovementPlanner.lastPosition = it;
        return (BlockPos)var2_2 /* !! */ ;
    }

    private final Vec3 chooseDirection(float currentAngle) {
        float currentDirection = currentAngle / 180.0f * (float)4 + (float)4;
        float newDirectionNumber = (float)Math.rint(currentDirection);
        float newDirectionAngle = Mth.wrapDegrees((float)((newDirectionNumber - (float)4) / 4.0f * 180.0f));
        Vec3 vec3 = Vec3.directionFromRotation((float)0.0f, (float)newDirectionAngle);
        Intrinsics.checkNotNullExpressionValue((Object)vec3, (String)"directionFromRotation(...)");
        return vec3;
    }

    public final void trackPlacedBlock(@NotNull BlockPos target) {
        Intrinsics.checkNotNullParameter((Object)target, (String)"target");
        if (Intrinsics.areEqual((Object)target, (Object)lastPlacedBlocks.lastOrNull())) {
            return;
        }
        while (lastPlacedBlocks.size() >= 4) {
            lastPlacedBlocks.removeFirst();
        }
        lastPlacedBlocks.add((Object)target);
    }

    public final void reset() {
        lastPosition = null;
        lastPlacedBlocks.clear();
    }

    static {
        double[] dArray = new double[]{0.301, 0.0, -0.301};
        offsetsToTry = dArray;
    }
}

