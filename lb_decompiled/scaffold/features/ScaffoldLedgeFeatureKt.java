/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  kotlin.Metadata
 *  kotlin.jvm.internal.Intrinsics
 *  kotlin.jvm.internal.SourceDebugExtension
 *  net.ccbluex.liquidbounce.features.misc.DebuggedOwner
 *  net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
 *  net.ccbluex.liquidbounce.utils.aiming.data.Rotation
 *  net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTarget
 *  net.ccbluex.liquidbounce.utils.entity.EntityExtensionsKt
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.player.LocalPlayer
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features;

import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.SourceDebugExtension;
import net.ccbluex.liquidbounce.features.misc.DebuggedOwner;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.LedgeAction;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldLedgeExtension;
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation;
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTarget;
import net.ccbluex.liquidbounce.utils.entity.EntityExtensionsKt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Metadata(mv={2, 3, 0}, k=2, xi=50, d1={"\u0000\u001a\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\u001a$\u0010\u0000\u001a\u00020\u00012\b\u0010\u0002\u001a\u0004\u0018\u00010\u00032\u0006\u0010\u0004\u001a\u00020\u00052\n\b\u0002\u0010\u0006\u001a\u0004\u0018\u00010\u0007\u00a8\u0006\b"}, d2={"ledge", "Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/LedgeAction;", "target", "Lnet/ccbluex/liquidbounce/utils/block/targetfinding/BlockPlacementTarget;", "rotation", "Lnet/ccbluex/liquidbounce/utils/aiming/data/Rotation;", "extension", "Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/ScaffoldLedgeExtension;", "liquidbounce"})
@SourceDebugExtension(value={"SMAP\nScaffoldLedgeFeature.kt\nKotlin\n*S Kotlin\n*F\n+ 1 ScaffoldLedgeFeature.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/ScaffoldLedgeFeatureKt\n+ 2 MinecraftExtensions.kt\nnet/ccbluex/liquidbounce/utils/client/MinecraftExtensionsKt\n*L\n1#1,73:1\n45#2:74\n43#2:75\n*S KotlinDebug\n*F\n+ 1 ScaffoldLedgeFeature.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/ScaffoldLedgeFeatureKt\n*L\n48#1:74\n48#1:75\n*E\n"})
public final class ScaffoldLedgeFeatureKt {
    @NotNull
    public static final LedgeAction ledge(@Nullable BlockPlacementTarget target, @NotNull Rotation rotation, @Nullable ScaffoldLedgeExtension extension) {
        Object object;
        Intrinsics.checkNotNullParameter((Object)rotation, (String)"rotation");
        boolean $i$f$getPlayer = false;
        boolean $i$f$getMc = false;
        Minecraft minecraft = Minecraft.getInstance();
        Intrinsics.checkNotNullExpressionValue((Object)minecraft, (String)"getInstance(...)");
        LocalPlayer localPlayer = minecraft.player;
        Intrinsics.checkNotNull((Object)localPlayer);
        if (EntityExtensionsKt.isCloseToEdge$default((LocalPlayer)localPlayer, null, (double)0.0, null, (int)7, null)) {
            boolean isNotReady;
            int ticks = ModuleScaffold.ScaffoldRotationValueGroup.INSTANCE.calculateTicks(rotation);
            ModuleDebug.INSTANCE.debugParameter((DebuggedOwner)ModuleScaffold.INSTANCE, "TicksUntilDestination", (Object)ticks);
            boolean isLowOnBlocks = ModuleScaffold.INSTANCE.getBlockCount() <= 0;
            boolean bl = isNotReady = ticks >= 1;
            if (isLowOnBlocks || isNotReady) {
                return new LedgeAction(false, Math.max(1, ticks), false, false, 12, null);
            }
        }
        if ((object = extension) == null || (object = object.ledge(target, rotation)) == null) {
            object = LedgeAction.NO_LEDGE;
        }
        return object;
    }

    public static /* synthetic */ LedgeAction ledge$default(BlockPlacementTarget blockPlacementTarget, Rotation rotation, ScaffoldLedgeExtension scaffoldLedgeExtension, int n, Object object) {
        if ((n & 4) != 0) {
            scaffoldLedgeExtension = null;
        }
        return ScaffoldLedgeFeatureKt.ledge(blockPlacementTarget, rotation, scaffoldLedgeExtension);
    }
}

