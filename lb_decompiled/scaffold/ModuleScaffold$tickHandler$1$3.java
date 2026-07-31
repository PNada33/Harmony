/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  kotlin.Metadata
 *  kotlin.jvm.functions.Function0
 *  kotlin.jvm.internal.Intrinsics
 *  net.ccbluex.liquidbounce.utils.block.BlockExtensionsKt
 *  net.ccbluex.liquidbounce.utils.block.SwingMode
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.phys.BlockHitResult
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold;

import kotlin.Metadata;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.internal.Intrinsics;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold;
import net.ccbluex.liquidbounce.utils.block.BlockExtensionsKt;
import net.ccbluex.liquidbounce.utils.block.SwingMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;

@Metadata(mv={2, 3, 0}, k=3, xi=50)
static final class ModuleScaffold.tickHandler.1.3
implements Function0<Boolean> {
    final /* synthetic */ BlockHitResult $currentCrosshairTarget;
    final /* synthetic */ InteractionHand $suitableHand;

    ModuleScaffold.tickHandler.1.3(BlockHitResult $currentCrosshairTarget, InteractionHand $suitableHand) {
        this.$currentCrosshairTarget = $currentCrosshairTarget;
        this.$suitableHand = $suitableHand;
    }

    public final Boolean invoke() {
        BlockHitResult blockHitResult = this.$currentCrosshairTarget;
        Intrinsics.checkNotNull((Object)blockHitResult);
        InteractionHand interactionHand = this.$suitableHand;
        Intrinsics.checkNotNull((Object)interactionHand);
        BlockExtensionsKt.doPlacement$default((BlockHitResult)blockHitResult, (InteractionHand)interactionHand, (Function0)((Function0)new Function0<Boolean>(){

            public final Boolean invoke() {
                ModuleScaffold.tickHandler$lambda$0$commonPlaceSucceed(BlockExtensionsKt.getTargetBlockPos((BlockHitResult)$currentCrosshairTarget));
                return true;
            }
        }), null, (SwingMode)ModuleScaffold.INSTANCE.getSwingMode(), (int)8, null);
        return true;
    }
}

