/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  kotlin.Metadata
 *  kotlin.enums.EnumEntries
 *  kotlin.enums.EnumEntriesKt
 *  net.ccbluex.liquidbounce.config.types.list.Tagged
 *  org.jetbrains.annotations.NotNull
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold;

import kotlin.Metadata;
import kotlin.enums.EnumEntries;
import kotlin.enums.EnumEntriesKt;
import net.ccbluex.liquidbounce.config.types.list.Tagged;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={2, 3, 0}, k=1, xi=50, d1={"\u0000\u0016\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0000\n\u0002\u0010\u000e\n\u0002\b\b\b\u0086\u0081\u0002\u0018\u00002\u00020\u00012\b\u0012\u0004\u0012\u00020\u00000\u0002B\u0011\b\u0002\u0012\u0006\u0010\u0003\u001a\u00020\u0004\u00a2\u0006\u0004\b\u0005\u0010\u0006R\u0014\u0010\u0003\u001a\u00020\u0004X\u0096\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\bj\u0002\b\tj\u0002\b\nj\u0002\b\u000b\u00a8\u0006\f"}, d2={"Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/ModuleScaffold$ScaffoldRotationValueGroup$RotationTimingMode;", "Lnet/ccbluex/liquidbounce/config/types/list/Tagged;", "", "tag", "", "<init>", "(Ljava/lang/String;ILjava/lang/String;)V", "getTag", "()Ljava/lang/String;", "NORMAL", "ON_TICK", "ON_TICK_SNAP", "liquidbounce"})
public static final class ModuleScaffold.ScaffoldRotationValueGroup.RotationTimingMode
extends Enum<ModuleScaffold.ScaffoldRotationValueGroup.RotationTimingMode>
implements Tagged {
    @NotNull
    private final String tag;
    public static final /* enum */ ModuleScaffold.ScaffoldRotationValueGroup.RotationTimingMode NORMAL = new ModuleScaffold.ScaffoldRotationValueGroup.RotationTimingMode("Normal");
    public static final /* enum */ ModuleScaffold.ScaffoldRotationValueGroup.RotationTimingMode ON_TICK = new ModuleScaffold.ScaffoldRotationValueGroup.RotationTimingMode("OnTick");
    public static final /* enum */ ModuleScaffold.ScaffoldRotationValueGroup.RotationTimingMode ON_TICK_SNAP = new ModuleScaffold.ScaffoldRotationValueGroup.RotationTimingMode("OnTickSnap");
    private static final /* synthetic */ ModuleScaffold.ScaffoldRotationValueGroup.RotationTimingMode[] $VALUES;
    private static final /* synthetic */ EnumEntries $ENTRIES;

    private ModuleScaffold.ScaffoldRotationValueGroup.RotationTimingMode(String tag) {
        this.tag = tag;
    }

    @NotNull
    public String getTag() {
        return this.tag;
    }

    public static ModuleScaffold.ScaffoldRotationValueGroup.RotationTimingMode[] values() {
        return (ModuleScaffold.ScaffoldRotationValueGroup.RotationTimingMode[])$VALUES.clone();
    }

    public static ModuleScaffold.ScaffoldRotationValueGroup.RotationTimingMode valueOf(String value) {
        return Enum.valueOf(ModuleScaffold.ScaffoldRotationValueGroup.RotationTimingMode.class, value);
    }

    @NotNull
    public static EnumEntries<ModuleScaffold.ScaffoldRotationValueGroup.RotationTimingMode> getEntries() {
        return $ENTRIES;
    }

    static {
        $VALUES = rotationTimingModeArray = new ModuleScaffold.ScaffoldRotationValueGroup.RotationTimingMode[]{ModuleScaffold.ScaffoldRotationValueGroup.RotationTimingMode.NORMAL, ModuleScaffold.ScaffoldRotationValueGroup.RotationTimingMode.ON_TICK, ModuleScaffold.ScaffoldRotationValueGroup.RotationTimingMode.ON_TICK_SNAP};
        $ENTRIES = EnumEntriesKt.enumEntries((Enum[])$VALUES);
    }
}

