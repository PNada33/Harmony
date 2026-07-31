/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  kotlin.Metadata
 *  kotlin.jvm.functions.Function1
 *  kotlin.jvm.internal.FunctionReferenceImpl
 *  kotlin.jvm.internal.Intrinsics
 *  net.ccbluex.liquidbounce.config.types.group.Mode
 *  net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
 *  net.ccbluex.liquidbounce.features.module.modules.movement.ModuleSafeWalk
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold;

import kotlin.Metadata;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.FunctionReferenceImpl;
import kotlin.jvm.internal.Intrinsics;
import net.ccbluex.liquidbounce.config.types.group.Mode;
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup;
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleSafeWalk;

@Metadata(mv={2, 3, 0}, k=3, xi=50)
static final class ModuleScaffold.safeWalkMode.1
extends FunctionReferenceImpl
implements Function1<ModeValueGroup<Mode>, Mode[]> {
    ModuleScaffold.safeWalkMode.1(Object receiver) {
        super(1, receiver, ModuleSafeWalk.class, "safeWalkChoices", "safeWalkChoices(Lnet/ccbluex/liquidbounce/config/types/group/ModeValueGroup;)[Lnet/ccbluex/liquidbounce/config/types/group/Mode;", 0);
    }

    public final Mode[] invoke(ModeValueGroup<Mode> p0) {
        Intrinsics.checkNotNullParameter(p0, (String)"p0");
        return ((ModuleSafeWalk)this.receiver).safeWalkChoices(p0);
    }
}

