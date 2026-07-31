/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  kotlin.Metadata
 *  kotlin.jvm.internal.DefaultConstructorMarker
 *  kotlin.jvm.internal.Intrinsics
 *  net.ccbluex.liquidbounce.config.types.group.Mode
 *  net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
 *  net.minecraft.core.BlockPos
 *  org.jetbrains.annotations.NotNull
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower;

import kotlin.Metadata;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import net.ccbluex.liquidbounce.config.types.group.Mode;
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower.ScaffoldTowerHypixel;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower.ScaffoldTowerKarhu;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower.ScaffoldTowerMotion;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower.ScaffoldTowerNone;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower.ScaffoldTowerPulldown;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower.ScaffoldTowerVulcan;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={2, 3, 0}, k=1, xi=50, d1={"\u0000:\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\b6\u0018\u00002\u00020\u0001B\u0011\b\u0004\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0004\b\u0004\u0010\u0005J\u0010\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\u000bH\u0016R\u0015\u0010\u0006\u001a\u0006\u0012\u0002\b\u00030\u00078F\u00a2\u0006\u0006\u001a\u0004\b\b\u0010\t\u0082\u0001\u0006\r\u000e\u000f\u0010\u0011\u0012\u00a8\u0006\u0013"}, d2={"Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/tower/ScaffoldTower;", "Lnet/ccbluex/liquidbounce/config/types/group/Mode;", "name", "", "<init>", "(Ljava/lang/String;)V", "parent", "Lnet/ccbluex/liquidbounce/config/types/group/ModeValueGroup;", "getParent", "()Lnet/ccbluex/liquidbounce/config/types/group/ModeValueGroup;", "getTargetedPosition", "Lnet/minecraft/core/BlockPos;", "blockPos", "Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/tower/ScaffoldTowerHypixel;", "Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/tower/ScaffoldTowerKarhu;", "Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/tower/ScaffoldTowerMotion;", "Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/tower/ScaffoldTowerNone;", "Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/tower/ScaffoldTowerPulldown;", "Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/tower/ScaffoldTowerVulcan;", "liquidbounce"})
public abstract sealed class ScaffoldTower
extends Mode
permits ScaffoldTowerHypixel, ScaffoldTowerKarhu, ScaffoldTowerMotion, ScaffoldTowerNone, ScaffoldTowerPulldown, ScaffoldTowerVulcan {
    private ScaffoldTower(String name) {
        super(name, null, 2, null);
    }

    @NotNull
    public final ModeValueGroup<?> getParent() {
        return ModuleScaffold.INSTANCE.getTowerMode();
    }

    @NotNull
    public BlockPos getTargetedPosition(@NotNull BlockPos blockPos) {
        Intrinsics.checkNotNullParameter((Object)blockPos, (String)"blockPos");
        BlockPos blockPos2 = blockPos.below();
        Intrinsics.checkNotNullExpressionValue((Object)blockPos2, (String)"below(...)");
        return blockPos2;
    }

    public /* synthetic */ ScaffoldTower(String name, DefaultConstructorMarker $constructor_marker) {
        this(name);
    }
}

