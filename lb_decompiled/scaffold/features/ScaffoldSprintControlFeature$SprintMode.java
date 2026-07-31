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
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features;

import kotlin.Metadata;
import kotlin.enums.EnumEntries;
import kotlin.enums.EnumEntriesKt;
import net.ccbluex.liquidbounce.config.types.list.Tagged;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={2, 3, 0}, k=1, xi=50, d1={"\u0000\u0016\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0000\n\u0002\u0010\u000e\n\u0002\b\n\b\u0082\u0081\u0002\u0018\u00002\u00020\u00012\b\u0012\u0004\u0012\u00020\u00000\u0002B\u0011\b\u0002\u0012\u0006\u0010\u0003\u001a\u00020\u0004\u00a2\u0006\u0004\b\u0005\u0010\u0006R\u0014\u0010\u0003\u001a\u00020\u0004X\u0096\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\bj\u0002\b\tj\u0002\b\nj\u0002\b\u000bj\u0002\b\fj\u0002\b\r\u00a8\u0006\u000e"}, d2={"Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/ScaffoldSprintControlFeature$SprintMode;", "Lnet/ccbluex/liquidbounce/config/types/list/Tagged;", "", "tag", "", "<init>", "(Ljava/lang/String;ILjava/lang/String;)V", "getTag", "()Ljava/lang/String;", "DO_NOT_CHANGE", "FORCE_SPRINT", "FORCE_NO_SPRINT", "NO_SPRINT_ON_PLACE", "NO_SPRINT_ON_GROUND", "liquidbounce"})
private static final class ScaffoldSprintControlFeature.SprintMode
extends Enum<ScaffoldSprintControlFeature.SprintMode>
implements Tagged {
    @NotNull
    private final String tag;
    public static final /* enum */ ScaffoldSprintControlFeature.SprintMode DO_NOT_CHANGE = new ScaffoldSprintControlFeature.SprintMode("DoNotChange");
    public static final /* enum */ ScaffoldSprintControlFeature.SprintMode FORCE_SPRINT = new ScaffoldSprintControlFeature.SprintMode("ForceSprint");
    public static final /* enum */ ScaffoldSprintControlFeature.SprintMode FORCE_NO_SPRINT = new ScaffoldSprintControlFeature.SprintMode("ForceNoSprint");
    public static final /* enum */ ScaffoldSprintControlFeature.SprintMode NO_SPRINT_ON_PLACE = new ScaffoldSprintControlFeature.SprintMode("NoSprintOnPlace");
    public static final /* enum */ ScaffoldSprintControlFeature.SprintMode NO_SPRINT_ON_GROUND = new ScaffoldSprintControlFeature.SprintMode("NoSprintOnGround");
    private static final /* synthetic */ ScaffoldSprintControlFeature.SprintMode[] $VALUES;
    private static final /* synthetic */ EnumEntries $ENTRIES;

    private ScaffoldSprintControlFeature.SprintMode(String tag) {
        this.tag = tag;
    }

    @NotNull
    public String getTag() {
        return this.tag;
    }

    public static ScaffoldSprintControlFeature.SprintMode[] values() {
        return (ScaffoldSprintControlFeature.SprintMode[])$VALUES.clone();
    }

    public static ScaffoldSprintControlFeature.SprintMode valueOf(String value) {
        return Enum.valueOf(ScaffoldSprintControlFeature.SprintMode.class, value);
    }

    @NotNull
    public static EnumEntries<ScaffoldSprintControlFeature.SprintMode> getEntries() {
        return $ENTRIES;
    }

    static {
        $VALUES = sprintModeArray = new ScaffoldSprintControlFeature.SprintMode[]{ScaffoldSprintControlFeature.SprintMode.DO_NOT_CHANGE, ScaffoldSprintControlFeature.SprintMode.FORCE_SPRINT, ScaffoldSprintControlFeature.SprintMode.FORCE_NO_SPRINT, ScaffoldSprintControlFeature.SprintMode.NO_SPRINT_ON_PLACE, ScaffoldSprintControlFeature.SprintMode.NO_SPRINT_ON_GROUND};
        $ENTRIES = EnumEntriesKt.enumEntries((Enum[])$VALUES);
    }
}

