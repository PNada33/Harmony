/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.common.base.Supplier
 *  com.google.common.base.Suppliers
 *  kotlin.Metadata
 *  kotlin.enums.EnumEntries
 *  kotlin.enums.EnumEntriesKt
 *  kotlin.jvm.internal.Intrinsics
 *  kotlin.random.Random
 *  kotlin.ranges.IntRange
 *  kotlin.ranges.RangesKt
 *  net.ccbluex.liquidbounce.config.types.list.Tagged
 *  org.jetbrains.annotations.NotNull
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import kotlin.Metadata;
import kotlin.enums.EnumEntries;
import kotlin.enums.EnumEntriesKt;
import kotlin.jvm.internal.Intrinsics;
import kotlin.random.Random;
import kotlin.ranges.IntRange;
import kotlin.ranges.RangesKt;
import net.ccbluex.liquidbounce.config.types.list.Tagged;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.LedgeAction;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={2, 3, 0}, k=1, xi=50, d1={"\u0000 \n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\r\b\u0082\u0081\u0002\u0018\u00002\u00020\u00012\b\u0012\u0004\u0012\u00020\u00000\u0002B\u001f\b\u0002\u0012\u0006\u0010\u0003\u001a\u00020\u0004\u0012\f\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006\u00a2\u0006\u0004\b\b\u0010\tB\u0019\b\u0012\u0012\u0006\u0010\u0003\u001a\u00020\u0004\u0012\u0006\u0010\n\u001a\u00020\u0007\u00a2\u0006\u0004\b\b\u0010\u000bR\u0014\u0010\u0003\u001a\u00020\u0004X\u0096\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\rR\u0017\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\u000fj\u0002\b\u0010j\u0002\b\u0011j\u0002\b\u0012j\u0002\b\u0013\u00a8\u0006\u0014"}, d2={"Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/techniques/ScaffoldGodBridgeTechnique$Mode;", "Lnet/ccbluex/liquidbounce/config/types/list/Tagged;", "", "tag", "", "creator", "Ljava/util/function/Supplier;", "Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/LedgeAction;", "<init>", "(Ljava/lang/String;ILjava/lang/String;Ljava/util/function/Supplier;)V", "ledgeAction", "(Ljava/lang/String;ILjava/lang/String;Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/LedgeAction;)V", "getTag", "()Ljava/lang/String;", "getCreator", "()Ljava/util/function/Supplier;", "JUMP", "SNEAK", "STOP_INPUT", "BACKWARDS", "liquidbounce"})
private static final class ScaffoldGodBridgeTechnique.Mode
extends Enum<ScaffoldGodBridgeTechnique.Mode>
implements Tagged {
    @NotNull
    private final String tag;
    @NotNull
    private final java.util.function.Supplier<LedgeAction> creator;
    public static final /* enum */ ScaffoldGodBridgeTechnique.Mode JUMP = new ScaffoldGodBridgeTechnique.Mode("Jump", new LedgeAction(true, 0, false, false, 14, null));
    public static final /* enum */ ScaffoldGodBridgeTechnique.Mode SNEAK = new ScaffoldGodBridgeTechnique.Mode("Sneak", ScaffoldGodBridgeTechnique.Mode::_init_$lambda$0);
    public static final /* enum */ ScaffoldGodBridgeTechnique.Mode STOP_INPUT = new ScaffoldGodBridgeTechnique.Mode("StopInput", new LedgeAction(false, 0, true, false, 11, null));
    public static final /* enum */ ScaffoldGodBridgeTechnique.Mode BACKWARDS = new ScaffoldGodBridgeTechnique.Mode("Backwards", new LedgeAction(false, 0, false, true, 7, null));
    private static final /* synthetic */ ScaffoldGodBridgeTechnique.Mode[] $VALUES;
    private static final /* synthetic */ EnumEntries $ENTRIES;

    private ScaffoldGodBridgeTechnique.Mode(String tag, java.util.function.Supplier<LedgeAction> creator) {
        this.tag = tag;
        this.creator = creator;
    }

    @NotNull
    public String getTag() {
        return this.tag;
    }

    @NotNull
    public final java.util.function.Supplier<LedgeAction> getCreator() {
        return this.creator;
    }

    private ScaffoldGodBridgeTechnique.Mode(String tag, LedgeAction ledgeAction) {
        Supplier supplier = Suppliers.ofInstance((Object)ledgeAction);
        Intrinsics.checkNotNullExpressionValue((Object)supplier, (String)"ofInstance(...)");
        this(tag, (java.util.function.Supplier)supplier);
    }

    public static ScaffoldGodBridgeTechnique.Mode[] values() {
        return (ScaffoldGodBridgeTechnique.Mode[])$VALUES.clone();
    }

    public static ScaffoldGodBridgeTechnique.Mode valueOf(String value) {
        return Enum.valueOf(ScaffoldGodBridgeTechnique.Mode.class, value);
    }

    @NotNull
    public static EnumEntries<ScaffoldGodBridgeTechnique.Mode> getEntries() {
        return $ENTRIES;
    }

    private static final LedgeAction _init_$lambda$0() {
        return new LedgeAction(false, RangesKt.random((IntRange)INSTANCE.getSneakTime(), (Random)((Random)Random.Default)), false, false, 13, null);
    }

    static {
        $VALUES = modeArray = new ScaffoldGodBridgeTechnique.Mode[]{ScaffoldGodBridgeTechnique.Mode.JUMP, ScaffoldGodBridgeTechnique.Mode.SNEAK, ScaffoldGodBridgeTechnique.Mode.STOP_INPUT, ScaffoldGodBridgeTechnique.Mode.BACKWARDS};
        $ENTRIES = EnumEntriesKt.enumEntries((Enum[])$VALUES);
    }
}

