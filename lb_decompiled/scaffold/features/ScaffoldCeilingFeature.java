/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  kotlin.Metadata
 *  kotlin.jvm.internal.Intrinsics
 *  kotlin.jvm.internal.SourceDebugExtension
 *  net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
 *  net.ccbluex.liquidbounce.event.EventListener
 *  net.ccbluex.liquidbounce.utils.block.BlockExtensionsKt
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.level.block.state.BlockState
 *  org.jetbrains.annotations.NotNull
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features;

import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.SourceDebugExtension;
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup;
import net.ccbluex.liquidbounce.event.EventListener;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.ScaffoldNormalTechnique;
import net.ccbluex.liquidbounce.utils.block.BlockExtensionsKt;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={2, 3, 0}, k=1, xi=50, d1={"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0000\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003J\u0006\u0010\u0004\u001a\u00020\u0005\u00a8\u0006\u0006"}, d2={"Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/ScaffoldCeilingFeature;", "Lnet/ccbluex/liquidbounce/config/types/group/ToggleableValueGroup;", "<init>", "()V", "canConstructCeiling", "", "liquidbounce"})
@SourceDebugExtension(value={"SMAP\nScaffoldCeilingFeature.kt\nKotlin\n*S Kotlin\n*F\n+ 1 ScaffoldCeilingFeature.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/ScaffoldCeilingFeature\n+ 2 BlockExtensions.kt\nnet/ccbluex/liquidbounce/utils/block/BlockExtensionsKt\n*L\n1#1,28:1\n130#2:29\n*S KotlinDebug\n*F\n+ 1 ScaffoldCeilingFeature.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/ScaffoldCeilingFeature\n*L\n26#1:29\n*E\n"})
public final class ScaffoldCeilingFeature
extends ToggleableValueGroup {
    @NotNull
    public static final ScaffoldCeilingFeature INSTANCE = new ScaffoldCeilingFeature();

    private ScaffoldCeilingFeature() {
        super((EventListener)ScaffoldNormalTechnique.INSTANCE, "Ceiling", false, null, 8, null);
    }

    public final boolean canConstructCeiling() {
        BlockPos blockPos = this.getPlayer().blockPosition().below();
        Intrinsics.checkNotNullExpressionValue((Object)blockPos, (String)"below(...)");
        BlockPos $this$getState$iv = blockPos;
        boolean bl = false;
        BlockState blockState = BlockExtensionsKt.getState((BlockPos)$this$getState$iv);
        Intrinsics.checkNotNull((Object)blockState);
        return !blockState.isAir();
    }
}

