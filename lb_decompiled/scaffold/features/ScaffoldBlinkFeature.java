/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  kotlin.Metadata
 *  kotlin.enums.EnumEntries
 *  kotlin.enums.EnumEntriesKt
 *  kotlin.jvm.internal.Intrinsics
 *  kotlin.jvm.internal.PropertyReference1
 *  kotlin.jvm.internal.PropertyReference1Impl
 *  kotlin.jvm.internal.Reflection
 *  kotlin.jvm.internal.SourceDebugExtension
 *  kotlin.random.Random
 *  kotlin.ranges.IntRange
 *  kotlin.ranges.RangesKt
 *  kotlin.reflect.KProperty
 *  net.ccbluex.liquidbounce.config.types.RangedValue
 *  net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
 *  net.ccbluex.liquidbounce.config.types.group.ValueGroup
 *  net.ccbluex.liquidbounce.config.types.list.MultiChoiceListValue
 *  net.ccbluex.liquidbounce.config.types.list.Tagged
 *  net.ccbluex.liquidbounce.event.EventHook
 *  net.ccbluex.liquidbounce.event.EventListener
 *  net.ccbluex.liquidbounce.event.EventListenerKt
 *  net.ccbluex.liquidbounce.event.events.BlinkPacketEvent
 *  net.ccbluex.liquidbounce.event.events.TransferOrigin
 *  net.ccbluex.liquidbounce.features.blink.BlinkManager$Action
 *  net.ccbluex.liquidbounce.utils.client.Chronometer
 *  net.ccbluex.liquidbounce.utils.kotlin.CollectionExtensionsKt
 *  net.minecraft.network.protocol.Packet
 *  net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import kotlin.Metadata;
import kotlin.enums.EnumEntries;
import kotlin.enums.EnumEntriesKt;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.PropertyReference1;
import kotlin.jvm.internal.PropertyReference1Impl;
import kotlin.jvm.internal.Reflection;
import kotlin.jvm.internal.SourceDebugExtension;
import kotlin.random.Random;
import kotlin.ranges.IntRange;
import kotlin.ranges.RangesKt;
import kotlin.reflect.KProperty;
import net.ccbluex.liquidbounce.config.types.RangedValue;
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup;
import net.ccbluex.liquidbounce.config.types.group.ValueGroup;
import net.ccbluex.liquidbounce.config.types.list.MultiChoiceListValue;
import net.ccbluex.liquidbounce.config.types.list.Tagged;
import net.ccbluex.liquidbounce.event.EventHook;
import net.ccbluex.liquidbounce.event.EventListener;
import net.ccbluex.liquidbounce.event.EventListenerKt;
import net.ccbluex.liquidbounce.event.events.BlinkPacketEvent;
import net.ccbluex.liquidbounce.event.events.TransferOrigin;
import net.ccbluex.liquidbounce.features.blink.BlinkManager;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold;
import net.ccbluex.liquidbounce.utils.client.Chronometer;
import net.ccbluex.liquidbounce.utils.kotlin.CollectionExtensionsKt;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Metadata(mv={2, 3, 0}, k=1, xi=50, d1={"\u0000>\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010#\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\t\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u00c6\u0002\u0018\u00002\u00020\u0001:\u0001\u001cB\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003J\u0006\u0010\u0016\u001a\u00020\u0017R\u001b\u0010\u0004\u001a\u00020\u00058BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\b\u0010\t\u001a\u0004\b\u0006\u0010\u0007R)\u0010\n\u001a\u0010\u0012\f\u0012\n \r*\u0004\u0018\u00010\f0\f0\u000b8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0010\u0010\u0011\u001a\u0004\b\u000e\u0010\u000fR\u000e\u0010\u0012\u001a\u00020\u0013X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0014\u001a\u00020\u0015X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\u001a0\u0019X\u0082\u0004\u00a2\u0006\b\n\u0000\u0012\u0004\b\u001b\u0010\u0003\u00a8\u0006\u001d"}, d2={"Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/ScaffoldBlinkFeature;", "Lnet/ccbluex/liquidbounce/config/types/group/ToggleableValueGroup;", "<init>", "()V", "time", "Lkotlin/ranges/IntRange;", "getTime", "()Lkotlin/ranges/IntRange;", "time$delegate", "Lnet/ccbluex/liquidbounce/config/types/RangedValue;", "flushOn", "", "Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/ScaffoldBlinkFeature$FlushOn;", "kotlin.jvm.PlatformType", "getFlushOn", "()Ljava/util/Set;", "flushOn$delegate", "Lnet/ccbluex/liquidbounce/config/types/list/MultiChoiceListValue;", "pulseTime", "", "pulseTimer", "Lnet/ccbluex/liquidbounce/utils/client/Chronometer;", "onBlockPlacement", "", "fakeLagHandler", "Lnet/ccbluex/liquidbounce/event/EventHook;", "Lnet/ccbluex/liquidbounce/event/events/BlinkPacketEvent;", "getFakeLagHandler$annotations", "FlushOn", "liquidbounce"})
@SourceDebugExtension(value={"SMAP\nScaffoldBlinkFeature.kt\nKotlin\n*S Kotlin\n*F\n+ 1 ScaffoldBlinkFeature.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/ScaffoldBlinkFeature\n+ 2 ValueGroup.kt\nnet/ccbluex/liquidbounce/config/types/group/ValueGroup\n+ 3 enum-set.kt\nnet/ccbluex/fastutil/enum-set\n+ 4 EventListener.kt\nnet/ccbluex/liquidbounce/event/EventListenerKt\n*L\n1#1,88:1\n531#2,3:89\n534#2:93\n535#2,3:95\n9#3:92\n216#3:94\n96#4,4:98\n*S KotlinDebug\n*F\n+ 1 ScaffoldBlinkFeature.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/ScaffoldBlinkFeature\n*L\n-1#1:89,3\n-1#1:93\n-1#1:95,3\n-1#1:92\n-1#1:94\n-1#1:98,4\n*E\n"})
public final class ScaffoldBlinkFeature
extends ToggleableValueGroup {
    @NotNull
    public static final ScaffoldBlinkFeature INSTANCE;
    static final /* synthetic */ KProperty<Object>[] $$delegatedProperties;
    @NotNull
    private static final RangedValue time$delegate;
    @NotNull
    private static final MultiChoiceListValue flushOn$delegate;
    private static long pulseTime;
    @NotNull
    private static final Chronometer pulseTimer;
    @NotNull
    private static final EventHook<BlinkPacketEvent> fakeLagHandler;

    private ScaffoldBlinkFeature() {
        super((EventListener)ModuleScaffold.INSTANCE, "Blink", false, null, 8, null);
    }

    private final IntRange getTime() {
        return (IntRange)time$delegate.getValue((Object)this, $$delegatedProperties[0]);
    }

    private final Set<FlushOn> getFlushOn() {
        return (Set)flushOn$delegate.getValue((Object)this, $$delegatedProperties[1]);
    }

    public final void onBlockPlacement() {
        pulseTime = RangesKt.random((IntRange)this.getTime(), (Random)((Random)Random.Default));
    }

    private static /* synthetic */ void getFakeLagHandler$annotations() {
    }

    private static final void fakeLagHandler$lambda$0(BlinkPacketEvent event) {
        Intrinsics.checkNotNullParameter((Object)event, (String)"event");
        if (event.getOrigin() != TransferOrigin.OUTGOING) {
            return;
        }
        if (pulseTimer.hasElapsed(pulseTime) || CollectionExtensionsKt.matchesAny((Iterable)INSTANCE.getFlushOn(), (Object)event.getPacket())) {
            Chronometer.reset$default((Chronometer)pulseTimer, (long)0L, (int)1, null);
            return;
        }
        if (!INSTANCE.getPlayer().onGround() || !pulseTimer.hasElapsed(pulseTime)) {
            event.setAction(BlinkManager.Action.QUEUE);
        }
    }

    /*
     * WARNING - void declaration
     */
    static {
        void $this$handler_u24default$iv;
        EventListener $this$iv;
        ValueGroup valueGroup = new ValueGroup[]{Reflection.property1((PropertyReference1)((PropertyReference1)new PropertyReference1Impl(ScaffoldBlinkFeature.class, "time", "getTime()Lkotlin/ranges/IntRange;", 0))), Reflection.property1((PropertyReference1)((PropertyReference1)new PropertyReference1Impl(ScaffoldBlinkFeature.class, "flushOn", "getFlushOn()Ljava/util/Set;", 0)))};
        $$delegatedProperties = valueGroup;
        INSTANCE = new ScaffoldBlinkFeature();
        time$delegate = ValueGroup.intRange$default((ValueGroup)((ValueGroup)INSTANCE), (String)"Time", (IntRange)new IntRange(50, 250), (IntRange)new IntRange(0, 3000), (String)"ms", null, (int)16, null);
        valueGroup = (ValueGroup)INSTANCE;
        String name$iv = "FlushOn";
        boolean $i$f$enumSetOf = false;
        EnumSet<FlushOn> enumSet = EnumSet.noneOf(FlushOn.class);
        Intrinsics.checkNotNullExpressionValue(enumSet, (String)"noneOf(...)");
        EnumSet<FlushOn> default$iv = enumSet;
        boolean $i$f$enumSetAllOf = false;
        EnumSet<FlushOn> enumSet2 = EnumSet.allOf(FlushOn.class);
        Intrinsics.checkNotNullExpressionValue(enumSet2, (String)"allOf(...)");
        EnumSet<FlushOn> choices$iv = enumSet2;
        boolean canBeNone$iv = true;
        boolean $i$f$multiEnumChoice = false;
        flushOn$delegate = $this$iv.multiEnumChoice(name$iv, (Set)default$iv, (Set)choices$iv, canBeNone$iv, false);
        pulseTimer = new Chronometer(0L, 1, null);
        $this$iv = (EventListener)INSTANCE;
        Consumer<BlinkPacketEvent> handler$iv = ScaffoldBlinkFeature::fakeLagHandler$lambda$0;
        short priority$iv = 0;
        boolean $i$f$handler = false;
        fakeLagHandler = EventListenerKt.handler((EventListener)$this$handler_u24default$iv, BlinkPacketEvent.class, (short)priority$iv, handler$iv);
    }

    @Metadata(mv={2, 3, 0}, k=1, xi=50, d1={"\u0000*\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0000\n\u0002\u0010\u000e\n\u0002\b\f\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0000\b\u0082\u0081\u0002\u0018\u00002\u00020\u00012\u000e\u0012\n\u0012\b\u0012\u0002\b\u0003\u0018\u00010\u00030\u00022\b\u0012\u0004\u0012\u00020\u00000\u0004B%\b\u0002\u0012\u0006\u0010\u0005\u001a\u00020\u0006\u0012\u0012\u0010\u0007\u001a\u000e\u0012\n\u0012\b\u0012\u0002\b\u0003\u0018\u00010\u00030\u0002\u00a2\u0006\u0004\b\b\u0010\tJ\u001c\u0010\u0012\u001a\u00020\u00132\u0011\u0010\u0014\u001a\r\u0012\u0002\b\u0003\u0018\u00010\u0003\u00a2\u0006\u0002\b\u0015H\u0096\u0001R\u0014\u0010\u0005\u001a\u00020\u0006X\u0096\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000bR\u001a\u0010\u0007\u001a\u000e\u0012\n\u0012\b\u0012\u0002\b\u0003\u0018\u00010\u00030\u0002X\u0082\u0004\u00a2\u0006\u0002\n\u0000j\u0002\b\fj\u0002\b\rj\u0002\b\u000ej\u0002\b\u000fj\u0002\b\u0010j\u0002\b\u0011\u00a8\u0006\u0016"}, d2={"Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/ScaffoldBlinkFeature$FlushOn;", "Lnet/ccbluex/liquidbounce/config/types/list/Tagged;", "Ljava/util/function/Predicate;", "Lnet/minecraft/network/protocol/Packet;", "", "tag", "", "cond", "<init>", "(Ljava/lang/String;ILjava/lang/String;Ljava/util/function/Predicate;)V", "getTag", "()Ljava/lang/String;", "PLACE", "TOWERING", "SNEAKING", "NOT_SNEAKING", "ON_GROUND", "IN_AIR", "test", "", "p0", "Lkotlin/jvm/internal/EnhancedNullability;", "liquidbounce"})
    private static final class FlushOn
    extends Enum<FlushOn>
    implements Tagged,
    Predicate<Packet<?>> {
        @NotNull
        private final String tag;
        @NotNull
        private final Predicate<Packet<?>> cond;
        public static final /* enum */ FlushOn PLACE = new FlushOn("Place", FlushOn::_init_$lambda$0);
        public static final /* enum */ FlushOn TOWERING = new FlushOn("Towering", FlushOn::_init_$lambda$1);
        public static final /* enum */ FlushOn SNEAKING = new FlushOn("Sneaking", FlushOn::_init_$lambda$2);
        public static final /* enum */ FlushOn NOT_SNEAKING = new FlushOn("NotSneaking", FlushOn::_init_$lambda$3);
        public static final /* enum */ FlushOn ON_GROUND = new FlushOn("OnGround", FlushOn::_init_$lambda$4);
        public static final /* enum */ FlushOn IN_AIR = new FlushOn("InAir", FlushOn::_init_$lambda$5);
        private static final /* synthetic */ FlushOn[] $VALUES;
        private static final /* synthetic */ EnumEntries $ENTRIES;

        private FlushOn(String tag, Predicate<Packet<?>> cond) {
            this.tag = tag;
            this.cond = cond;
        }

        @NotNull
        public String getTag() {
            return this.tag;
        }

        public static FlushOn[] values() {
            return (FlushOn[])$VALUES.clone();
        }

        public static FlushOn valueOf(String value) {
            return Enum.valueOf(FlushOn.class, value);
        }

        @NotNull
        public static EnumEntries<FlushOn> getEntries() {
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
            $VALUES = flushOnArray = new FlushOn[]{FlushOn.PLACE, FlushOn.TOWERING, FlushOn.SNEAKING, FlushOn.NOT_SNEAKING, FlushOn.ON_GROUND, FlushOn.IN_AIR};
            $ENTRIES = EnumEntriesKt.enumEntries((Enum[])$VALUES);
        }
    }
}

