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
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.normal;

import kotlin.Metadata;
import kotlin.enums.EnumEntries;
import kotlin.enums.EnumEntriesKt;
import net.ccbluex.liquidbounce.config.types.list.Tagged;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={2, 3, 0}, k=1, xi=50, d1={"\u0000\u0016\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0007\b\u0086\u0081\u0002\u0018\u00002\u00020\u00012\b\u0012\u0004\u0012\u00020\u00000\u0002B\u0011\b\u0002\u0012\u0006\u0010\u0003\u001a\u00020\u0004\u00a2\u0006\u0004\b\u0005\u0010\u0006R\u0014\u0010\u0003\u001a\u00020\u0004X\u0096\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\bj\u0002\b\tj\u0002\b\n\u00a8\u0006\u000b"}, d2={"Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/techniques/normal/ScaffoldTellyFeature$Mode;", "Lnet/ccbluex/liquidbounce/config/types/list/Tagged;", "", "tag", "", "<init>", "(Ljava/lang/String;ILjava/lang/String;)V", "getTag", "()Ljava/lang/String;", "REVERSE", "RESET", "liquidbounce"})
public static final class ScaffoldTellyFeature.Mode
extends Enum<ScaffoldTellyFeature.Mode>
implements Tagged {
    @NotNull
    private final String tag;
    public static final /* enum */ ScaffoldTellyFeature.Mode REVERSE = new ScaffoldTellyFeature.Mode("Reverse");
    public static final /* enum */ ScaffoldTellyFeature.Mode RESET = new ScaffoldTellyFeature.Mode("Reset");
    private static final /* synthetic */ ScaffoldTellyFeature.Mode[] $VALUES;
    private static final /* synthetic */ EnumEntries $ENTRIES;

    private ScaffoldTellyFeature.Mode(String tag) {
        this.tag = tag;
    }

    @NotNull
    public String getTag() {
        return this.tag;
    }

    public static ScaffoldTellyFeature.Mode[] values() {
        return (ScaffoldTellyFeature.Mode[])$VALUES.clone();
    }

    public static ScaffoldTellyFeature.Mode valueOf(String value) {
        return Enum.valueOf(ScaffoldTellyFeature.Mode.class, value);
    }

    @NotNull
    public static EnumEntries<ScaffoldTellyFeature.Mode> getEntries() {
        return $ENTRIES;
    }

    static {
        $VALUES = modeArray = new ScaffoldTellyFeature.Mode[]{ScaffoldTellyFeature.Mode.REVERSE, ScaffoldTellyFeature.Mode.RESET};
        $ENTRIES = EnumEntriesKt.enumEntries((Enum[])$VALUES);
    }
}

