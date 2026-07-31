/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  kotlin.Metadata
 *  kotlin.jvm.functions.Function0
 *  net.ccbluex.liquidbounce.utils.block.BlockExtensionsKt
 *  net.minecraft.world.phys.BlockHitResult
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold;

import kotlin.Metadata;
import kotlin.jvm.functions.Function0;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold;
import net.ccbluex.liquidbounce.utils.block.BlockExtensionsKt;
import net.minecraft.world.phys.BlockHitResult;

@Metadata(mv={2, 3, 0}, k=3, xi=50)
static final class ModuleScaffold.tickHandler.1.1
implements Function0<Boolean> {
    final /* synthetic */ BlockHitResult $currentCrosshairTarget;

    ModuleScaffold.tickHandler.1.1(BlockHitResult $currentCrosshairTarget) {
        this.$currentCrosshairTarget = $currentCrosshairTarget;
    }

    public final Boolean invoke() {
        ModuleScaffold.tickHandler$lambda$0$commonPlaceSucceed(BlockExtensionsKt.getTargetBlockPos((BlockHitResult)this.$currentCrosshairTarget));
        return true;
    }
}

