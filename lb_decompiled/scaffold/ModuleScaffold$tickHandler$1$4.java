/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  kotlin.Metadata
 *  kotlin.jvm.functions.Function0
 *  kotlin.jvm.internal.Ref$BooleanRef
 *  net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTarget
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold;

import kotlin.Metadata;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.internal.Ref;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold;
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTarget;

@Metadata(mv={2, 3, 0}, k=3, xi=50)
static final class ModuleScaffold.tickHandler.1.4
implements Function0<Boolean> {
    final /* synthetic */ BlockPlacementTarget $target;
    final /* synthetic */ Ref.BooleanRef $wasSuccessful;

    ModuleScaffold.tickHandler.1.4(BlockPlacementTarget $target, Ref.BooleanRef $wasSuccessful) {
        this.$target = $target;
        this.$wasSuccessful = $wasSuccessful;
    }

    public final Boolean invoke() {
        ModuleScaffold.tickHandler$lambda$0$commonPlaceSucceed(this.$target.getPlacedBlock());
        currentTarget = null;
        this.$wasSuccessful.element = true;
        return true;
    }
}

