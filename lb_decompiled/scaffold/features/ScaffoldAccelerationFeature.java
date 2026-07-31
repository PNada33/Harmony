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
 *  net.ccbluex.liquidbounce.config.types.Value
 *  net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
 *  net.ccbluex.liquidbounce.config.types.group.ValueGroup
 *  net.ccbluex.liquidbounce.event.EventHook
 *  net.ccbluex.liquidbounce.event.EventListener
 *  net.ccbluex.liquidbounce.event.EventListenerKt
 *  net.ccbluex.liquidbounce.event.events.GameTickEvent
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.world.phys.Vec3
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
import net.ccbluex.liquidbounce.config.types.Value;
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup;
import net.ccbluex.liquidbounce.config.types.group.ValueGroup;
import net.ccbluex.liquidbounce.event.EventHook;
import net.ccbluex.liquidbounce.event.EventListener;
import net.ccbluex.liquidbounce.event.EventListenerKt;
import net.ccbluex.liquidbounce.event.events.GameTickEvent;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={2, 3, 0}, k=1, xi=50, d1={"\u0000(\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0007\n\u0002\b\u0005\n\u0002\u0010\u000b\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003R\u001b\u0010\u0004\u001a\u00020\u00058BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\b\u0010\t\u001a\u0004\b\u0006\u0010\u0007R\u001b\u0010\n\u001a\u00020\u000b8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u000e\u0010\u000f\u001a\u0004\b\f\u0010\rR\u001d\u0010\u0010\u001a\b\u0012\u0004\u0012\u00020\u00120\u0011\u00a2\u0006\u000e\n\u0000\u0012\u0004\b\u0013\u0010\u0003\u001a\u0004\b\u0014\u0010\u0015\u00a8\u0006\u0016"}, d2={"Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/ScaffoldAccelerationFeature;", "Lnet/ccbluex/liquidbounce/config/types/group/ToggleableValueGroup;", "<init>", "()V", "speedMultiplier", "", "getSpeedMultiplier", "()F", "speedMultiplier$delegate", "Lnet/ccbluex/liquidbounce/config/types/RangedValue;", "onlyOnGround", "", "getOnlyOnGround", "()Z", "onlyOnGround$delegate", "Lnet/ccbluex/liquidbounce/config/types/Value;", "stateUpdateHandler", "Lnet/ccbluex/liquidbounce/event/EventHook;", "Lnet/ccbluex/liquidbounce/event/events/GameTickEvent;", "getStateUpdateHandler$annotations", "getStateUpdateHandler", "()Lnet/ccbluex/liquidbounce/event/EventHook;", "liquidbounce"})
@SourceDebugExtension(value={"SMAP\nScaffoldAccelerationFeature.kt\nKotlin\n*S Kotlin\n*F\n+ 1 ScaffoldAccelerationFeature.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/ScaffoldAccelerationFeature\n+ 2 MinecraftVectorExtensions.kt\nnet/ccbluex/liquidbounce/utils/math/MinecraftVectorExtensionsKt\n+ 3 EventListener.kt\nnet/ccbluex/liquidbounce/event/EventListenerKt\n*L\n1#1,40:1\n163#2,2:41\n96#3,4:43\n*S KotlinDebug\n*F\n+ 1 ScaffoldAccelerationFeature.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/ScaffoldAccelerationFeature\n*L\n37#1:41,2\n-1#1:43,4\n*E\n"})
public final class ScaffoldAccelerationFeature
extends ToggleableValueGroup {
    @NotNull
    public static final ScaffoldAccelerationFeature INSTANCE;
    static final /* synthetic */ KProperty<Object>[] $$delegatedProperties;
    @NotNull
    private static final RangedValue speedMultiplier$delegate;
    @NotNull
    private static final Value onlyOnGround$delegate;
    @NotNull
    private static final EventHook<GameTickEvent> stateUpdateHandler;

    private ScaffoldAccelerationFeature() {
        super((EventListener)ModuleScaffold.INSTANCE, "Acceleration", false, null, 8, null);
    }

    private final float getSpeedMultiplier() {
        return ((Number)speedMultiplier$delegate.getValue((Object)this, $$delegatedProperties[0])).floatValue();
    }

    private final boolean getOnlyOnGround() {
        return (Boolean)onlyOnGround$delegate.getValue((Object)this, $$delegatedProperties[1]);
    }

    @NotNull
    public final EventHook<GameTickEvent> getStateUpdateHandler() {
        return stateUpdateHandler;
    }

    public static /* synthetic */ void getStateUpdateHandler$annotations() {
    }

    /*
     * WARNING - void declaration
     */
    private static final void stateUpdateHandler$lambda$0(GameTickEvent it) {
        void factorX$iv;
        void $this$multiply_u24default$iv;
        Intrinsics.checkNotNullParameter((Object)it, (String)"it");
        if (INSTANCE.getOnlyOnGround() && !INSTANCE.getPlayer().onGround()) {
            return;
        }
        LocalPlayer localPlayer = INSTANCE.getPlayer();
        Vec3 vec3 = INSTANCE.getPlayer().getDeltaMovement();
        Intrinsics.checkNotNullExpressionValue((Object)vec3, (String)"getDeltaMovement(...)");
        Vec3 vec32 = vec3;
        float f = INSTANCE.getSpeedMultiplier();
        float factorZ$iv = INSTANCE.getSpeedMultiplier();
        float factorY$iv = 1.0f;
        boolean $i$f$multiply = false;
        Vec3 vec33 = $this$multiply_u24default$iv.multiply((double)factorX$iv, (double)factorY$iv, (double)factorZ$iv);
        Intrinsics.checkNotNullExpressionValue((Object)vec33, (String)"multiply(...)");
        localPlayer.setDeltaMovement(vec33);
    }

    /*
     * WARNING - void declaration
     */
    static {
        void $this$handler_u24default$iv;
        EventListener eventListener = new EventListener[]{Reflection.property1((PropertyReference1)((PropertyReference1)new PropertyReference1Impl(ScaffoldAccelerationFeature.class, "speedMultiplier", "getSpeedMultiplier()F", 0))), Reflection.property1((PropertyReference1)((PropertyReference1)new PropertyReference1Impl(ScaffoldAccelerationFeature.class, "onlyOnGround", "getOnlyOnGround()Z", 0)))};
        $$delegatedProperties = eventListener;
        INSTANCE = new ScaffoldAccelerationFeature();
        speedMultiplier$delegate = ValueGroup.float$default((ValueGroup)((ValueGroup)INSTANCE), (String)"SpeedMultiplier", (float)0.6f, (ClosedFloatingPointRange)RangesKt.rangeTo((float)0.1f, (float)3.0f), null, null, (int)24, null);
        onlyOnGround$delegate = ValueGroup.boolean$default((ValueGroup)((ValueGroup)INSTANCE), (String)"OnlyOnGround", (boolean)false, null, (int)4, null);
        eventListener = (EventListener)INSTANCE;
        Consumer<GameTickEvent> handler$iv = ScaffoldAccelerationFeature::stateUpdateHandler$lambda$0;
        short priority$iv = 0;
        boolean $i$f$handler = false;
        stateUpdateHandler = EventListenerKt.handler((EventListener)$this$handler_u24default$iv, GameTickEvent.class, (short)priority$iv, handler$iv);
    }
}

