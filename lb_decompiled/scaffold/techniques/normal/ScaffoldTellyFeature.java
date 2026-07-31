/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  kotlin.Metadata
 *  kotlin.NoWhenBranchMatchedException
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
 *  net.ccbluex.liquidbounce.config.types.Value
 *  net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
 *  net.ccbluex.liquidbounce.config.types.group.ValueGroup
 *  net.ccbluex.liquidbounce.config.types.list.ChoiceListValue
 *  net.ccbluex.liquidbounce.config.types.list.Tagged
 *  net.ccbluex.liquidbounce.event.EventHook
 *  net.ccbluex.liquidbounce.event.EventListener
 *  net.ccbluex.liquidbounce.event.EventListenerKt
 *  net.ccbluex.liquidbounce.event.events.GameTickEvent
 *  net.ccbluex.liquidbounce.event.events.MovementInputEvent
 *  net.ccbluex.liquidbounce.event.events.PlayerAfterJumpEvent
 *  net.ccbluex.liquidbounce.utils.aiming.RotationManager
 *  net.ccbluex.liquidbounce.utils.entity.EntityExtensionsKt
 *  net.minecraft.client.player.LocalPlayer
 *  org.jetbrains.annotations.NotNull
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.normal;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;
import kotlin.Metadata;
import kotlin.NoWhenBranchMatchedException;
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
import net.ccbluex.liquidbounce.config.types.Value;
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup;
import net.ccbluex.liquidbounce.config.types.group.ValueGroup;
import net.ccbluex.liquidbounce.config.types.list.ChoiceListValue;
import net.ccbluex.liquidbounce.config.types.list.Tagged;
import net.ccbluex.liquidbounce.event.EventHook;
import net.ccbluex.liquidbounce.event.EventListener;
import net.ccbluex.liquidbounce.event.EventListenerKt;
import net.ccbluex.liquidbounce.event.events.GameTickEvent;
import net.ccbluex.liquidbounce.event.events.MovementInputEvent;
import net.ccbluex.liquidbounce.event.events.PlayerAfterJumpEvent;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.ScaffoldNormalTechnique;
import net.ccbluex.liquidbounce.utils.aiming.RotationManager;
import net.ccbluex.liquidbounce.utils.entity.EntityExtensionsKt;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={2, 3, 0}, k=1, xi=50, d1={"\u0000F\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0004\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\n\n\u0002\u0018\u0002\n\u0002\b\t\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u00c6\u0002\u0018\u00002\u00020\u0001:\u0001*B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003R\u0011\u0010\u0004\u001a\u00020\u00058F\u00a2\u0006\u0006\u001a\u0004\b\u0006\u0010\u0007R\u0011\u0010\b\u001a\u00020\u00058F\u00a2\u0006\u0006\u001a\u0004\b\b\u0010\u0007R\u000e\u0010\t\u001a\u00020\nX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001b\u0010\u000b\u001a\u00020\f8FX\u0086\u0084\u0002\u00a2\u0006\f\n\u0004\b\u000f\u0010\u0010\u001a\u0004\b\r\u0010\u000eR\u001b\u0010\u0011\u001a\u00020\n8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0014\u0010\u0015\u001a\u0004\b\u0012\u0010\u0013R\u001b\u0010\u0016\u001a\u00020\u00178BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u001a\u0010\u0015\u001a\u0004\b\u0018\u0010\u0019R\u001b\u0010\u001b\u001a\u00020\u00058BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u001d\u0010\u001e\u001a\u0004\b\u001c\u0010\u0007R\u000e\u0010\u001f\u001a\u00020\nX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001a\u0010 \u001a\b\u0012\u0004\u0012\u00020\"0!X\u0082\u0004\u00a2\u0006\b\n\u0000\u0012\u0004\b#\u0010\u0003R\u001a\u0010$\u001a\b\u0012\u0004\u0012\u00020%0!X\u0082\u0004\u00a2\u0006\b\n\u0000\u0012\u0004\b&\u0010\u0003R\u001a\u0010'\u001a\b\u0012\u0004\u0012\u00020(0!X\u0082\u0004\u00a2\u0006\b\n\u0000\u0012\u0004\b)\u0010\u0003\u00a8\u0006+"}, d2={"Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/techniques/normal/ScaffoldTellyFeature;", "Lnet/ccbluex/liquidbounce/config/types/group/ToggleableValueGroup;", "<init>", "()V", "doNotAim", "", "getDoNotAim", "()Z", "isTellyBridging", "ticksUntilJump", "", "resetMode", "Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/techniques/normal/ScaffoldTellyFeature$Mode;", "getResetMode", "()Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/techniques/normal/ScaffoldTellyFeature$Mode;", "resetMode$delegate", "Lnet/ccbluex/liquidbounce/config/types/list/ChoiceListValue;", "straightTicks", "getStraightTicks", "()I", "straightTicks$delegate", "Lnet/ccbluex/liquidbounce/config/types/RangedValue;", "jumpTicksOpt", "Lkotlin/ranges/IntRange;", "getJumpTicksOpt", "()Lkotlin/ranges/IntRange;", "jumpTicksOpt$delegate", "aimOnTower", "getAimOnTower", "aimOnTower$delegate", "Lnet/ccbluex/liquidbounce/config/types/Value;", "jumpTicks", "gameHandler", "Lnet/ccbluex/liquidbounce/event/EventHook;", "Lnet/ccbluex/liquidbounce/event/events/GameTickEvent;", "getGameHandler$annotations", "movementInputHandler", "Lnet/ccbluex/liquidbounce/event/events/MovementInputEvent;", "getMovementInputHandler$annotations", "afterJumpHandler", "Lnet/ccbluex/liquidbounce/event/events/PlayerAfterJumpEvent;", "getAfterJumpHandler$annotations", "Mode", "liquidbounce"})
@SourceDebugExtension(value={"SMAP\nScaffoldTellyFeature.kt\nKotlin\n*S Kotlin\n*F\n+ 1 ScaffoldTellyFeature.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/techniques/normal/ScaffoldTellyFeature\n+ 2 ValueGroup.kt\nnet/ccbluex/liquidbounce/config/types/group/ValueGroup\n+ 3 enum-set.kt\nnet/ccbluex/fastutil/enum-set\n+ 4 EventListener.kt\nnet/ccbluex/liquidbounce/event/EventListenerKt\n*L\n1#1,94:1\n558#2:95\n216#3:96\n96#4,4:97\n*S KotlinDebug\n*F\n+ 1 ScaffoldTellyFeature.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/techniques/normal/ScaffoldTellyFeature\n*L\n-1#1:95\n-1#1:96\n-1#1:97,4\n*E\n"})
public final class ScaffoldTellyFeature
extends ToggleableValueGroup {
    @NotNull
    public static final ScaffoldTellyFeature INSTANCE;
    static final /* synthetic */ KProperty<Object>[] $$delegatedProperties;
    private static int ticksUntilJump;
    @NotNull
    private static final ChoiceListValue resetMode$delegate;
    @NotNull
    private static final RangedValue straightTicks$delegate;
    @NotNull
    private static final RangedValue jumpTicksOpt$delegate;
    @NotNull
    private static final Value aimOnTower$delegate;
    private static int jumpTicks;
    @NotNull
    private static final EventHook<GameTickEvent> gameHandler;
    @NotNull
    private static final EventHook<MovementInputEvent> movementInputHandler;
    @NotNull
    private static final EventHook<PlayerAfterJumpEvent> afterJumpHandler;

    private ScaffoldTellyFeature() {
        super((EventListener)ScaffoldNormalTechnique.INSTANCE, "Telly", false, null, 8, null);
    }

    public final boolean getDoNotAim() {
        return EntityExtensionsKt.getAirTicks((LocalPlayer)this.getPlayer()) <= this.getStraightTicks() && ticksUntilJump >= jumpTicks && (!ModuleScaffold.INSTANCE.isTowering$liquidbounce() || !this.getAimOnTower());
    }

    public final boolean isTellyBridging() {
        return ticksUntilJump >= jumpTicks && EntityExtensionsKt.getMoving((LocalPlayer)this.getPlayer()) && this.getEnabled();
    }

    @NotNull
    public final Mode getResetMode() {
        return (Mode)((Object)resetMode$delegate.getValue((Object)this, $$delegatedProperties[0]));
    }

    private final int getStraightTicks() {
        return ((Number)straightTicks$delegate.getValue((Object)this, $$delegatedProperties[1])).intValue();
    }

    private final IntRange getJumpTicksOpt() {
        return (IntRange)jumpTicksOpt$delegate.getValue((Object)this, $$delegatedProperties[2]);
    }

    private final boolean getAimOnTower() {
        return (Boolean)aimOnTower$delegate.getValue((Object)this, $$delegatedProperties[3]);
    }

    private static /* synthetic */ void getGameHandler$annotations() {
    }

    private static /* synthetic */ void getMovementInputHandler$annotations() {
    }

    private static /* synthetic */ void getAfterJumpHandler$annotations() {
    }

    private static final void gameHandler$lambda$0(GameTickEvent it) {
        Intrinsics.checkNotNullParameter((Object)it, (String)"it");
        if (INSTANCE.getPlayer().onGround()) {
            int n = ticksUntilJump;
            ticksUntilJump = n + 1;
        }
    }

    private static final void movementInputHandler$lambda$0(MovementInputEvent event) {
        Intrinsics.checkNotNullParameter((Object)event, (String)"event");
        if (!EntityExtensionsKt.getMoving((LocalPlayer)INSTANCE.getPlayer()) || ModuleScaffold.INSTANCE.getBlockCount() <= 0 || !INSTANCE.getPlayer().onGround()) {
            return;
        }
        boolean isStraight = RotationManager.INSTANCE.getCurrentRotation() == null || INSTANCE.getStraightTicks() == 0;
        switch (WhenMappings.$EnumSwitchMapping$0[INSTANCE.getResetMode().ordinal()]) {
            case 1: {
                event.setJump(true);
                break;
            }
            case 2: {
                if (!isStraight || ticksUntilJump < jumpTicks) break;
                event.setJump(true);
                break;
            }
            default: {
                throw new NoWhenBranchMatchedException();
            }
        }
    }

    private static final void afterJumpHandler$lambda$0(PlayerAfterJumpEvent it) {
        Intrinsics.checkNotNullParameter((Object)it, (String)"it");
        ticksUntilJump = 0;
        jumpTicks = RangesKt.random((IntRange)INSTANCE.getJumpTicksOpt(), (Random)((Random)Random.Default));
    }

    /*
     * WARNING - void declaration
     */
    static {
        EventListener $this$handler_u24default$iv;
        void name$iv;
        EventListener this_$iv;
        ValueGroup valueGroup = new ValueGroup[]{Reflection.property1((PropertyReference1)((PropertyReference1)new PropertyReference1Impl(ScaffoldTellyFeature.class, "resetMode", "getResetMode()Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/techniques/normal/ScaffoldTellyFeature$Mode;", 0))), Reflection.property1((PropertyReference1)((PropertyReference1)new PropertyReference1Impl(ScaffoldTellyFeature.class, "straightTicks", "getStraightTicks()I", 0))), Reflection.property1((PropertyReference1)((PropertyReference1)new PropertyReference1Impl(ScaffoldTellyFeature.class, "jumpTicksOpt", "getJumpTicksOpt()Lkotlin/ranges/IntRange;", 0))), Reflection.property1((PropertyReference1)((PropertyReference1)new PropertyReference1Impl(ScaffoldTellyFeature.class, "aimOnTower", "getAimOnTower()Z", 0)))};
        $$delegatedProperties = valueGroup;
        INSTANCE = new ScaffoldTellyFeature();
        valueGroup = (ValueGroup)INSTANCE;
        String string = "ResetMode";
        Enum default$iv = Mode.RESET;
        boolean $i$f$enumChoice = false;
        Tagged tagged = (Tagged)default$iv;
        boolean $i$f$enumSetAllOf = false;
        EnumSet<Mode> enumSet = EnumSet.allOf(Mode.class);
        Intrinsics.checkNotNullExpressionValue(enumSet, (String)"allOf(...)");
        resetMode$delegate = this_$iv.enumChoice((String)name$iv, tagged, (Set)enumSet);
        straightTicks$delegate = ValueGroup.int$default((ValueGroup)((ValueGroup)INSTANCE), (String)"Straight", (int)0, (IntRange)new IntRange(0, 5), (String)"ticks", null, (int)16, null);
        jumpTicksOpt$delegate = ValueGroup.intRange$default((ValueGroup)((ValueGroup)INSTANCE), (String)"Jump", (IntRange)new IntRange(0, 0), (IntRange)new IntRange(0, 10), (String)"ticks", null, (int)16, null);
        aimOnTower$delegate = ValueGroup.boolean$default((ValueGroup)((ValueGroup)INSTANCE), (String)"AimOnTower", (boolean)true, null, (int)4, null);
        jumpTicks = RangesKt.random((IntRange)INSTANCE.getJumpTicksOpt(), (Random)((Random)Random.Default));
        this_$iv = (EventListener)INSTANCE;
        Consumer<GameTickEvent> handler$iv = ScaffoldTellyFeature::gameHandler$lambda$0;
        short priority$iv = 0;
        boolean $i$f$handler = false;
        gameHandler = EventListenerKt.handler((EventListener)$this$handler_u24default$iv, GameTickEvent.class, (short)priority$iv, handler$iv);
        $this$handler_u24default$iv = (EventListener)INSTANCE;
        handler$iv = ScaffoldTellyFeature::movementInputHandler$lambda$0;
        priority$iv = 0;
        $i$f$handler = false;
        movementInputHandler = EventListenerKt.handler((EventListener)$this$handler_u24default$iv, MovementInputEvent.class, (short)priority$iv, handler$iv);
        $this$handler_u24default$iv = (EventListener)INSTANCE;
        handler$iv = ScaffoldTellyFeature::afterJumpHandler$lambda$0;
        priority$iv = 0;
        $i$f$handler = false;
        afterJumpHandler = EventListenerKt.handler((EventListener)$this$handler_u24default$iv, PlayerAfterJumpEvent.class, (short)priority$iv, handler$iv);
    }

    @Metadata(mv={2, 3, 0}, k=1, xi=50, d1={"\u0000\u0016\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0007\b\u0086\u0081\u0002\u0018\u00002\u00020\u00012\b\u0012\u0004\u0012\u00020\u00000\u0002B\u0011\b\u0002\u0012\u0006\u0010\u0003\u001a\u00020\u0004\u00a2\u0006\u0004\b\u0005\u0010\u0006R\u0014\u0010\u0003\u001a\u00020\u0004X\u0096\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\bj\u0002\b\tj\u0002\b\n\u00a8\u0006\u000b"}, d2={"Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/techniques/normal/ScaffoldTellyFeature$Mode;", "Lnet/ccbluex/liquidbounce/config/types/list/Tagged;", "", "tag", "", "<init>", "(Ljava/lang/String;ILjava/lang/String;)V", "getTag", "()Ljava/lang/String;", "REVERSE", "RESET", "liquidbounce"})
    public static final class Mode
    extends Enum<Mode>
    implements Tagged {
        @NotNull
        private final String tag;
        public static final /* enum */ Mode REVERSE = new Mode("Reverse");
        public static final /* enum */ Mode RESET = new Mode("Reset");
        private static final /* synthetic */ Mode[] $VALUES;
        private static final /* synthetic */ EnumEntries $ENTRIES;

        private Mode(String tag) {
            this.tag = tag;
        }

        @NotNull
        public String getTag() {
            return this.tag;
        }

        public static Mode[] values() {
            return (Mode[])$VALUES.clone();
        }

        public static Mode valueOf(String value) {
            return Enum.valueOf(Mode.class, value);
        }

        @NotNull
        public static EnumEntries<Mode> getEntries() {
            return $ENTRIES;
        }

        static {
            $VALUES = modeArray = new Mode[]{Mode.REVERSE, Mode.RESET};
            $ENTRIES = EnumEntriesKt.enumEntries((Enum[])$VALUES);
        }
    }

    @Metadata(mv={2, 3, 0}, k=3, xi=50)
    public static final class WhenMappings {
        public static final /* synthetic */ int[] $EnumSwitchMapping$0;

        static {
            int[] nArray = new int[Mode.values().length];
            try {
                nArray[Mode.REVERSE.ordinal()] = 1;
            }
            catch (NoSuchFieldError noSuchFieldError) {
                // empty catch block
            }
            try {
                nArray[Mode.RESET.ordinal()] = 2;
            }
            catch (NoSuchFieldError noSuchFieldError) {
                // empty catch block
            }
            $EnumSwitchMapping$0 = nArray;
        }
    }
}

