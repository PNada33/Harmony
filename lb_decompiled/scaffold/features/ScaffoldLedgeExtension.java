/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  kotlin.Metadata
 *  net.ccbluex.liquidbounce.utils.aiming.data.Rotation
 *  net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTarget
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features;

import kotlin.Metadata;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.LedgeAction;
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation;
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTarget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Metadata(mv={2, 3, 0}, k=1, xi=50, d1={"\u0000\u001c\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\b\u00e6\u0080\u0001\u0018\u00002\u00020\u0001J\u001a\u0010\u0002\u001a\u00020\u00032\b\u0010\u0004\u001a\u0004\u0018\u00010\u00052\u0006\u0010\u0006\u001a\u00020\u0007H&\u00a8\u0006\b\u00c0\u0006\u0003"}, d2={"Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/ScaffoldLedgeExtension;", "", "ledge", "Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/LedgeAction;", "target", "Lnet/ccbluex/liquidbounce/utils/block/targetfinding/BlockPlacementTarget;", "rotation", "Lnet/ccbluex/liquidbounce/utils/aiming/data/Rotation;", "liquidbounce"})
public interface ScaffoldLedgeExtension {
    @NotNull
    public LedgeAction ledge(@Nullable BlockPlacementTarget var1, @NotNull Rotation var2);
}

