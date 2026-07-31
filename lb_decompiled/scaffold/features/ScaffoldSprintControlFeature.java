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
 *  kotlin.reflect.KProperty
 *  net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
 *  net.ccbluex.liquidbounce.config.types.group.ValueGroup
 *  net.ccbluex.liquidbounce.config.types.list.ChoiceListValue
 *  net.ccbluex.liquidbounce.config.types.list.Tagged
 *  net.ccbluex.liquidbounce.event.EventHook
 *  net.ccbluex.liquidbounce.event.EventListener
 *  net.ccbluex.liquidbounce.event.EventListenerKt
 *  net.ccbluex.liquidbounce.event.events.GameTickEvent
 *  net.ccbluex.liquidbounce.event.events.SprintEvent
 *  net.ccbluex.liquidbounce.event.events.SprintEvent$Source
 *  org.jetbrains.annotations.NotNull
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features;

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
import kotlin.reflect.KProperty;
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup;
import net.ccbluex.liquidbounce.config.types.group.ValueGroup;
import net.ccbluex.liquidbounce.config.types.list.ChoiceListValue;
import net.ccbluex.liquidbounce.config.types.list.Tagged;
import net.ccbluex.liquidbounce.event.EventHook;
import net.ccbluex.liquidbounce.event.EventListener;
import net.ccbluex.liquidbounce.event.EventListenerKt;
import net.ccbluex.liquidbounce.event.events.GameTickEvent;
import net.ccbluex.liquidbounce.event.events.SprintEvent;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={2, 3, 0}, k=1, xi=50, d1={"\u00008\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\b\n\u0002\u0010\u000b\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001:\u0001\u001bB\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003J\u0006\u0010\u0019\u001a\u00020\u001aR\u001b\u0010\u0004\u001a\u00020\u00058BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\b\u0010\t\u001a\u0004\b\u0006\u0010\u0007R\u001b\u0010\n\u001a\u00020\u00058BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\f\u0010\t\u001a\u0004\b\u000b\u0010\u0007R\u000e\u0010\r\u001a\u00020\u000eX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0011\u0010\u000f\u001a\u00020\u000e8F\u00a2\u0006\u0006\u001a\u0004\b\u0010\u0010\u0011R\u001a\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00140\u0013X\u0082\u0004\u00a2\u0006\b\n\u0000\u0012\u0004\b\u0015\u0010\u0003R\u001a\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u00170\u0013X\u0082\u0004\u00a2\u0006\b\n\u0000\u0012\u0004\b\u0018\u0010\u0003\u00a8\u0006\u001c"}, d2={"Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/ScaffoldSprintControlFeature;", "Lnet/ccbluex/liquidbounce/config/types/group/ToggleableValueGroup;", "<init>", "()V", "clientMode", "Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/ScaffoldSprintControlFeature$SprintMode;", "getClientMode", "()Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/ScaffoldSprintControlFeature$SprintMode;", "clientMode$delegate", "Lnet/ccbluex/liquidbounce/config/types/list/ChoiceListValue;", "serverMode", "getServerMode", "serverMode$delegate", "wasPlaced", "", "allowOmnidirectionalSprint", "getAllowOmnidirectionalSprint", "()Z", "gameTickHandler", "Lnet/ccbluex/liquidbounce/event/EventHook;", "Lnet/ccbluex/liquidbounce/event/events/GameTickEvent;", "getGameTickHandler$annotations", "sprintHandler", "Lnet/ccbluex/liquidbounce/event/events/SprintEvent;", "getSprintHandler$annotations", "onBlockPlacement", "", "SprintMode", "liquidbounce"})
@SourceDebugExtension(value={"SMAP\nScaffoldSprintControlFeature.kt\nKotlin\n*S Kotlin\n*F\n+ 1 ScaffoldSprintControlFeature.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/ScaffoldSprintControlFeature\n+ 2 ValueGroup.kt\nnet/ccbluex/liquidbounce/config/types/group/ValueGroup\n+ 3 enum-set.kt\nnet/ccbluex/fastutil/enum-set\n+ 4 EventListener.kt\nnet/ccbluex/liquidbounce/event/EventListenerKt\n*L\n1#1,149:1\n558#2:150\n216#3:151\n99#4:152\n*S KotlinDebug\n*F\n+ 1 ScaffoldSprintControlFeature.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/ScaffoldSprintControlFeature\n*L\n-1#1:150\n-1#1:151\n-1#1:152\n*E\n"})
public final class ScaffoldSprintControlFeature
extends ToggleableValueGroup {
    @NotNull
    public static final ScaffoldSprintControlFeature INSTANCE;
    static final /* synthetic */ KProperty<Object>[] $$delegatedProperties;
    @NotNull
    private static final ChoiceListValue clientMode$delegate;
    @NotNull
    private static final ChoiceListValue serverMode$delegate;
    private static boolean wasPlaced;
    @NotNull
    private static final EventHook<GameTickEvent> gameTickHandler;
    @NotNull
    private static final EventHook<SprintEvent> sprintHandler;

    private ScaffoldSprintControlFeature() {
        super((EventListener)ModuleScaffold.INSTANCE, "SprintControl", false, null, 8, null);
    }

    private final SprintMode getClientMode() {
        return (SprintMode)((Object)clientMode$delegate.getValue((Object)this, $$delegatedProperties[0]));
    }

    private final SprintMode getServerMode() {
        return (SprintMode)((Object)serverMode$delegate.getValue((Object)this, $$delegatedProperties[1]));
    }

    public final boolean getAllowOmnidirectionalSprint() {
        return this.getRunning() && this.getClientMode() == SprintMode.FORCE_SPRINT;
    }

    private static /* synthetic */ void getGameTickHandler$annotations() {
    }

    private static /* synthetic */ void getSprintHandler$annotations() {
    }

    public final void onBlockPlacement() {
        wasPlaced = true;
    }

    private static final void gameTickHandler$lambda$0(GameTickEvent it) {
        Intrinsics.checkNotNullParameter((Object)it, (String)"it");
        if (wasPlaced) {
            wasPlaced = false;
        }
    }

    private static final void sprintHandler$lambda$0(SprintEvent event) {
        Intrinsics.checkNotNullParameter((Object)event, (String)"event");
        boolean willPlace = false;
        if (event.getSource() == SprintEvent.Source.MOVEMENT_TICK || event.getSource() == SprintEvent.Source.INPUT) {
            switch (WhenMappings.$EnumSwitchMapping$0[INSTANCE.getClientMode().ordinal()]) {
                case 1: {
                    if (!event.getDirectionalInput().isMoving()) break;
                    event.setSprint(true);
                    break;
                }
                case 2: {
                    event.setSprint(false);
                    break;
                }
                case 3: {
                    if (!wasPlaced) break;
                    event.setSprint(false);
                    break;
                }
                case 4: {
                    event.setSprint(!INSTANCE.getPlayer().onGround());
                    break;
                }
                case 5: {
                    break;
                }
                default: {
                    throw new NoWhenBranchMatchedException();
                }
            }
        }
        if (event.getSource() == SprintEvent.Source.NETWORK || event.getSource() == SprintEvent.Source.INPUT) {
            switch (WhenMappings.$EnumSwitchMapping$0[INSTANCE.getServerMode().ordinal()]) {
                case 1: {
                    if (!event.getDirectionalInput().isMoving()) break;
                    event.setSprint(true);
                    break;
                }
                case 2: {
                    event.setSprint(false);
                    break;
                }
                case 3: {
                    if (!wasPlaced) break;
                    event.setSprint(false);
                    break;
                }
                case 4: {
                    event.setSprint(!INSTANCE.getPlayer().onGround());
                    break;
                }
                case 5: {
                    break;
                }
                default: {
                    throw new NoWhenBranchMatchedException();
                }
            }
        }
    }

    static {
        short priority$iv;
        EventListener $this$handler$iv;
        String name$iv;
        ValueGroup this_$iv;
        ValueGroup valueGroup = new ValueGroup[]{Reflection.property1((PropertyReference1)((PropertyReference1)new PropertyReference1Impl(ScaffoldSprintControlFeature.class, "clientMode", "getClientMode()Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/ScaffoldSprintControlFeature$SprintMode;", 0))), Reflection.property1((PropertyReference1)((PropertyReference1)new PropertyReference1Impl(ScaffoldSprintControlFeature.class, "serverMode", "getServerMode()Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/ScaffoldSprintControlFeature$SprintMode;", 0)))};
        $$delegatedProperties = valueGroup;
        INSTANCE = new ScaffoldSprintControlFeature();
        valueGroup = (ValueGroup)INSTANCE;
        String string = "Client";
        Enum default$iv = SprintMode.DO_NOT_CHANGE;
        boolean $i$f$enumChoice = false;
        Tagged tagged = (Tagged)default$iv;
        boolean $i$f$enumSetAllOf = false;
        EnumSet<SprintMode> enumSet = EnumSet.allOf(SprintMode.class);
        Intrinsics.checkNotNullExpressionValue(enumSet, (String)"allOf(...)");
        clientMode$delegate = this_$iv.enumChoice(name$iv, tagged, (Set)enumSet);
        this_$iv = (ValueGroup)INSTANCE;
        name$iv = "Server";
        default$iv = SprintMode.DO_NOT_CHANGE;
        $i$f$enumChoice = false;
        Tagged tagged2 = (Tagged)default$iv;
        $i$f$enumSetAllOf = false;
        EnumSet<SprintMode> enumSet2 = EnumSet.allOf(SprintMode.class);
        Intrinsics.checkNotNullExpressionValue(enumSet2, (String)"allOf(...)");
        serverMode$delegate = this_$iv.enumChoice(name$iv, tagged2, (Set)enumSet2);
        this_$iv = (EventListener)INSTANCE;
        int name$iv2 = 1000;
        Consumer<GameTickEvent> handler$iv = ScaffoldSprintControlFeature::gameTickHandler$lambda$0;
        boolean $i$f$handler = false;
        gameTickHandler = EventListenerKt.handler((EventListener)$this$handler$iv, GameTickEvent.class, (short)priority$iv, handler$iv);
        $this$handler$iv = (EventListener)INSTANCE;
        priority$iv = -10;
        handler$iv = ScaffoldSprintControlFeature::sprintHandler$lambda$0;
        $i$f$handler = false;
        sprintHandler = EventListenerKt.handler((EventListener)$this$handler$iv, SprintEvent.class, (short)priority$iv, handler$iv);
    }

    @Metadata(mv={2, 3, 0}, k=1, xi=50, d1={"\u0000\u0016\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0000\n\u0002\u0010\u000e\n\u0002\b\n\b\u0082\u0081\u0002\u0018\u00002\u00020\u00012\b\u0012\u0004\u0012\u00020\u00000\u0002B\u0011\b\u0002\u0012\u0006\u0010\u0003\u001a\u00020\u0004\u00a2\u0006\u0004\b\u0005\u0010\u0006R\u0014\u0010\u0003\u001a\u00020\u0004X\u0096\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\bj\u0002\b\tj\u0002\b\nj\u0002\b\u000bj\u0002\b\fj\u0002\b\r\u00a8\u0006\u000e"}, d2={"Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/ScaffoldSprintControlFeature$SprintMode;", "Lnet/ccbluex/liquidbounce/config/types/list/Tagged;", "", "tag", "", "<init>", "(Ljava/lang/String;ILjava/lang/String;)V", "getTag", "()Ljava/lang/String;", "DO_NOT_CHANGE", "FORCE_SPRINT", "FORCE_NO_SPRINT", "NO_SPRINT_ON_PLACE", "NO_SPRINT_ON_GROUND", "liquidbounce"})
    private static final class SprintMode
    extends Enum<SprintMode>
    implements Tagged {
        @NotNull
        private final String tag;
        public static final /* enum */ SprintMode DO_NOT_CHANGE = new SprintMode("DoNotChange");
        public static final /* enum */ SprintMode FORCE_SPRINT = new SprintMode("ForceSprint");
        public static final /* enum */ SprintMode FORCE_NO_SPRINT = new SprintMode("ForceNoSprint");
        public static final /* enum */ SprintMode NO_SPRINT_ON_PLACE = new SprintMode("NoSprintOnPlace");
        public static final /* enum */ SprintMode NO_SPRINT_ON_GROUND = new SprintMode("NoSprintOnGround");
        private static final /* synthetic */ SprintMode[] $VALUES;
        private static final /* synthetic */ EnumEntries $ENTRIES;

        private SprintMode(String tag) {
            this.tag = tag;
        }

        @NotNull
        public String getTag() {
            return this.tag;
        }

        public static SprintMode[] values() {
            return (SprintMode[])$VALUES.clone();
        }

        public static SprintMode valueOf(String value) {
            return Enum.valueOf(SprintMode.class, value);
        }

        @NotNull
        public static EnumEntries<SprintMode> getEntries() {
            return $ENTRIES;
        }

        static {
            $VALUES = sprintModeArray = new SprintMode[]{SprintMode.DO_NOT_CHANGE, SprintMode.FORCE_SPRINT, SprintMode.FORCE_NO_SPRINT, SprintMode.NO_SPRINT_ON_PLACE, SprintMode.NO_SPRINT_ON_GROUND};
            $ENTRIES = EnumEntriesKt.enumEntries((Enum[])$VALUES);
        }
    }

    @Metadata(mv={2, 3, 0}, k=3, xi=50)
    public static final class WhenMappings {
        public static final /* synthetic */ int[] $EnumSwitchMapping$0;

        static {
            int[] nArray = new int[SprintMode.values().length];
            try {
                nArray[SprintMode.FORCE_SPRINT.ordinal()] = 1;
            }
            catch (NoSuchFieldError noSuchFieldError) {
                // empty catch block
            }
            try {
                nArray[SprintMode.FORCE_NO_SPRINT.ordinal()] = 2;
            }
            catch (NoSuchFieldError noSuchFieldError) {
                // empty catch block
            }
            try {
                nArray[SprintMode.NO_SPRINT_ON_PLACE.ordinal()] = 3;
            }
            catch (NoSuchFieldError noSuchFieldError) {
                // empty catch block
            }
            try {
                nArray[SprintMode.NO_SPRINT_ON_GROUND.ordinal()] = 4;
            }
            catch (NoSuchFieldError noSuchFieldError) {
                // empty catch block
            }
            try {
                nArray[SprintMode.DO_NOT_CHANGE.ordinal()] = 5;
            }
            catch (NoSuchFieldError noSuchFieldError) {
                // empty catch block
            }
            $EnumSwitchMapping$0 = nArray;
        }
    }
}

