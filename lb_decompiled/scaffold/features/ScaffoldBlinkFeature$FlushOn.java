/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  kotlin.Metadata
 *  kotlin.enums.EnumEntries
 *  kotlin.enums.EnumEntriesKt
 *  net.ccbluex.liquidbounce.config.types.list.Tagged
 *  net.minecraft.network.protocol.Packet
 *  net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features;

import java.util.function.Predicate;
import kotlin.Metadata;
import kotlin.enums.EnumEntries;
import kotlin.enums.EnumEntriesKt;
import net.ccbluex.liquidbounce.config.types.list.Tagged;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Metadata(mv={2, 3, 0}, k=1, xi=50, d1={"\u0000*\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0000\n\u0002\u0010\u000e\n\u0002\b\f\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0000\b\u0082\u0081\u0002\u0018\u00002\u00020\u00012\u000e\u0012\n\u0012\b\u0012\u0002\b\u0003\u0018\u00010\u00030\u00022\b\u0012\u0004\u0012\u00020\u00000\u0004B%\b\u0002\u0012\u0006\u0010\u0005\u001a\u00020\u0006\u0012\u0012\u0010\u0007\u001a\u000e\u0012\n\u0012\b\u0012\u0002\b\u0003\u0018\u00010\u00030\u0002\u00a2\u0006\u0004\b\b\u0010\tJ\u001c\u0010\u0012\u001a\u00020\u00132\u0011\u0010\u0014\u001a\r\u0012\u0002\b\u0003\u0018\u00010\u0003\u00a2\u0006\u0002\b\u0015H\u0096\u0001R\u0014\u0010\u0005\u001a\u00020\u0006X\u0096\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000bR\u001a\u0010\u0007\u001a\u000e\u0012\n\u0012\b\u0012\u0002\b\u0003\u0018\u00010\u00030\u0002X\u0082\u0004\u00a2\u0006\u0002\n\u0000j\u0002\b\fj\u0002\b\rj\u0002\b\u000ej\u0002\b\u000fj\u0002\b\u0010j\u0002\b\u0011\u00a8\u0006\u0016"}, d2={"Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/ScaffoldBlinkFeature$FlushOn;", "Lnet/ccbluex/liquidbounce/config/types/list/Tagged;", "Ljava/util/function/Predicate;", "Lnet/minecraft/network/protocol/Packet;", "", "tag", "", "cond", "<init>", "(Ljava/lang/String;ILjava/lang/String;Ljava/util/function/Predicate;)V", "getTag", "()Ljava/lang/String;", "PLACE", "TOWERING", "SNEAKING", "NOT_SNEAKING", "ON_GROUND", "IN_AIR", "test", "", "p0", "Lkotlin/jvm/internal/EnhancedNullability;", "liquidbounce"})
private static final class ScaffoldBlinkFeature.FlushOn
extends Enum<ScaffoldBlinkFeature.FlushOn>
implements Tagged,
Predicate<Packet<?>> {
    @NotNull
    private final String tag;
    @NotNull
    private final Predicate<Packet<?>> cond;
    public static final /* enum */ ScaffoldBlinkFeature.FlushOn PLACE = new ScaffoldBlinkFeature.FlushOn("Place", ScaffoldBlinkFeature.FlushOn::_init_$lambda$0);
    public static final /* enum */ ScaffoldBlinkFeature.FlushOn TOWERING = new ScaffoldBlinkFeature.FlushOn("Towering", ScaffoldBlinkFeature.FlushOn::_init_$lambda$1);
    public static final /* enum */ ScaffoldBlinkFeature.FlushOn SNEAKING = new ScaffoldBlinkFeature.FlushOn("Sneaking", ScaffoldBlinkFeature.FlushOn::_init_$lambda$2);
    public static final /* enum */ ScaffoldBlinkFeature.FlushOn NOT_SNEAKING = new ScaffoldBlinkFeature.FlushOn("NotSneaking", ScaffoldBlinkFeature.FlushOn::_init_$lambda$3);
    public static final /* enum */ ScaffoldBlinkFeature.FlushOn ON_GROUND = new ScaffoldBlinkFeature.FlushOn("OnGround", ScaffoldBlinkFeature.FlushOn::_init_$lambda$4);
    public static final /* enum */ ScaffoldBlinkFeature.FlushOn IN_AIR = new ScaffoldBlinkFeature.FlushOn("InAir", ScaffoldBlinkFeature.FlushOn::_init_$lambda$5);
    private static final /* synthetic */ ScaffoldBlinkFeature.FlushOn[] $VALUES;
    private static final /* synthetic */ EnumEntries $ENTRIES;

    private ScaffoldBlinkFeature.FlushOn(String tag, Predicate<Packet<?>> cond) {
        this.tag = tag;
        this.cond = cond;
    }

    @NotNull
    public String getTag() {
        return this.tag;
    }

    public static ScaffoldBlinkFeature.FlushOn[] values() {
        return (ScaffoldBlinkFeature.FlushOn[])$VALUES.clone();
    }

    public static ScaffoldBlinkFeature.FlushOn valueOf(String value) {
        return Enum.valueOf(ScaffoldBlinkFeature.FlushOn.class, value);
    }

    @NotNull
    public static EnumEntries<ScaffoldBlinkFeature.FlushOn> getEntries() {
        return $ENTRIES;
    }

    @Override
    public boolean test(@Nullable Packet<?> p0) {
        return this.cond.test(p0);
    }

    private static final boolean _init_$lambda$0(Packet packet) {
        return packet instanceof ServerboundUseItemOnPacket;
    }

    private static final boolean _init_$lambda$1(Packet it) {
        return ModuleScaffold.INSTANCE.isTowering$liquidbounce();
    }

    private static final boolean _init_$lambda$2(Packet it) {
        return INSTANCE.getPlayer().isShiftKeyDown();
    }

    private static final boolean _init_$lambda$3(Packet it) {
        return !INSTANCE.getPlayer().isShiftKeyDown();
    }

    private static final boolean _init_$lambda$4(Packet it) {
        return INSTANCE.getPlayer().onGround();
    }

    private static final boolean _init_$lambda$5(Packet it) {
        return !INSTANCE.getPlayer().onGround();
    }

    static {
        $VALUES = flushOnArray = new ScaffoldBlinkFeature.FlushOn[]{ScaffoldBlinkFeature.FlushOn.PLACE, ScaffoldBlinkFeature.FlushOn.TOWERING, ScaffoldBlinkFeature.FlushOn.SNEAKING, ScaffoldBlinkFeature.FlushOn.NOT_SNEAKING, ScaffoldBlinkFeature.FlushOn.ON_GROUND, ScaffoldBlinkFeature.FlushOn.IN_AIR};
        $ENTRIES = EnumEntriesKt.enumEntries((Enum[])$VALUES);
    }
}

