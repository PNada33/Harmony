/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  kotlin.Metadata
 *  kotlin.jvm.internal.Intrinsics
 *  kotlin.jvm.internal.PropertyReference1
 *  kotlin.jvm.internal.PropertyReference1Impl
 *  kotlin.jvm.internal.Reflection
 *  kotlin.jvm.internal.SourceDebugExtension
 *  kotlin.ranges.ClosedFloatingPointRange
 *  kotlin.ranges.RangesKt
 *  kotlin.reflect.KProperty
 *  net.ccbluex.liquidbounce.config.types.RangedValue
 *  net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
 *  net.ccbluex.liquidbounce.config.types.group.ValueGroup
 *  net.ccbluex.liquidbounce.event.EventHook
 *  net.ccbluex.liquidbounce.event.EventListener
 *  net.ccbluex.liquidbounce.event.EventListenerKt
 *  net.ccbluex.liquidbounce.event.events.MovementInputEvent
 *  net.ccbluex.liquidbounce.utils.movement.DirectionalInput
 *  net.minecraft.world.entity.Entity
 *  org.jetbrains.annotations.NotNull
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features;

import java.util.function.Consumer;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.PropertyReference1;
import kotlin.jvm.internal.PropertyReference1Impl;
import kotlin.jvm.internal.Reflection;
import kotlin.jvm.internal.SourceDebugExtension;
import kotlin.ranges.ClosedFloatingPointRange;
import kotlin.ranges.RangesKt;
import kotlin.reflect.KProperty;
import net.ccbluex.liquidbounce.config.types.RangedValue;
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup;
import net.ccbluex.liquidbounce.config.types.group.ValueGroup;
import net.ccbluex.liquidbounce.event.EventHook;
import net.ccbluex.liquidbounce.event.EventListener;
import net.ccbluex.liquidbounce.event.EventListenerKt;
import net.ccbluex.liquidbounce.event.events.MovementInputEvent;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold;
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={2, 3, 0}, k=1, xi=50, d1={"\u0000 \n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0007\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003R\u001b\u0010\u0004\u001a\u00020\u00058BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\b\u0010\t\u001a\u0004\b\u0006\u0010\u0007R\u001a\u0010\n\u001a\b\u0012\u0004\u0012\u00020\f0\u000bX\u0082\u0004\u00a2\u0006\b\n\u0000\u0012\u0004\b\r\u0010\u0003\u00a8\u0006\u000e"}, d2={"Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/ScaffoldSpeedLimiterFeature;", "Lnet/ccbluex/liquidbounce/config/types/group/ToggleableValueGroup;", "<init>", "()V", "speedLimit", "", "getSpeedLimit", "()F", "speedLimit$delegate", "Lnet/ccbluex/liquidbounce/config/types/RangedValue;", "moveEvent", "Lnet/ccbluex/liquidbounce/event/EventHook;", "Lnet/ccbluex/liquidbounce/event/events/MovementInputEvent;", "getMoveEvent$annotations", "liquidbounce"})
@SourceDebugExtension(value={"SMAP\nScaffoldSpeedLimiterFeature.kt\nKotlin\n*S Kotlin\n*F\n+ 1 ScaffoldSpeedLimiterFeature.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/ScaffoldSpeedLimiterFeature\n+ 2 EntityExtensions.kt\nnet/ccbluex/liquidbounce/utils/entity/EntityExtensionsKt\n+ 3 EventListener.kt\nnet/ccbluex/liquidbounce/event/EventListenerKt\n*L\n1#1,41:1\n350#2:42\n99#3:43\n*S KotlinDebug\n*F\n+ 1 ScaffoldSpeedLimiterFeature.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/ScaffoldSpeedLimiterFeature\n*L\n35#1:42\n-1#1:43\n*E\n"})
public final class ScaffoldSpeedLimiterFeature
extends ToggleableValueGroup {
    @NotNull
    public static final ScaffoldSpeedLimiterFeature INSTANCE;
    static final /* synthetic */ KProperty<Object>[] $$delegatedProperties;
    @NotNull
    private static final RangedValue speedLimit$delegate;
    @NotNull
    private static final EventHook<MovementInputEvent> moveEvent;

    private ScaffoldSpeedLimiterFeature() {
        super((EventListener)ModuleScaffold.INSTANCE, "SpeedLimiter", false, null, 8, null);
    }

    private final float getSpeedLimit() {
        return ((Number)speedLimit$delegate.getValue((Object)this, $$delegatedProperties[0])).floatValue();
    }

    private static /* synthetic */ void getMoveEvent$annotations() {
    }

    private static final void moveEvent$lambda$0(MovementInputEvent it) {
        Intrinsics.checkNotNullParameter((Object)it, (String)"it");
        Entity $this$horizontalSpeed$iv = (Entity)INSTANCE.getPlayer();
        boolean $i$f$getHorizontalSpeed = false;
        if ($this$horizontalSpeed$iv.getDeltaMovement().horizontalDistance() > (double)INSTANCE.getSpeedLimit()) {
            it.setDirectionalInput(DirectionalInput.NONE);
        }
    }

    /*
     * WARNING - void declaration
     */
    static {
        void priority$iv;
        void $this$handler$iv;
        EventListener eventListener = new EventListener[]{Reflection.property1((PropertyReference1)((PropertyReference1)new PropertyReference1Impl(ScaffoldSpeedLimiterFeature.class, "speedLimit", "getSpeedLimit()F", 0)))};
        $$delegatedProperties = eventListener;
        INSTANCE = new ScaffoldSpeedLimiterFeature();
        speedLimit$delegate = ValueGroup.float$default((ValueGroup)((ValueGroup)INSTANCE), (String)"SpeedLimit", (float)0.11f, (ClosedFloatingPointRange)RangesKt.rangeTo((float)0.01f, (float)0.4f), null, null, (int)24, null);
        eventListener = (EventListener)INSTANCE;
        int n = -50;
        Consumer<MovementInputEvent> handler$iv = ScaffoldSpeedLimiterFeature::moveEvent$lambda$0;
        boolean $i$f$handler = false;
        moveEvent = EventListenerKt.handler((EventListener)$this$handler$iv, MovementInputEvent.class, (short)priority$iv, handler$iv);
    }
}

