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
 *  net.ccbluex.liquidbounce.event.events.PlayerAfterJumpEvent
 *  net.ccbluex.liquidbounce.features.misc.DebuggedOwner
 *  net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
 *  net.ccbluex.liquidbounce.utils.entity.EntityExtensionsKt
 *  net.ccbluex.liquidbounce.utils.kotlin.ArrayExtensionsKt
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.NotNull
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features;

import java.util.Arrays;
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
import net.ccbluex.liquidbounce.event.events.PlayerAfterJumpEvent;
import net.ccbluex.liquidbounce.features.misc.DebuggedOwner;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold;
import net.ccbluex.liquidbounce.utils.entity.EntityExtensionsKt;
import net.ccbluex.liquidbounce.utils.kotlin.ArrayExtensionsKt;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={2, 3, 0}, k=1, xi=50, d1={"\u0000$\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0010\u0007\n\u0002\b\b\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003R!\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u00058BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\t\u0010\n\u001a\u0004\b\u0007\u0010\bR!\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\u00060\u00058BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\r\u0010\n\u001a\u0004\b\f\u0010\bR\u001a\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00100\u000fX\u0082\u0004\u00a2\u0006\b\n\u0000\u0012\u0004\b\u0011\u0010\u0003\u00a8\u0006\u0012"}, d2={"Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/ScaffoldJumpStrafe;", "Lnet/ccbluex/liquidbounce/config/types/group/ToggleableValueGroup;", "<init>", "()V", "straightSpeed", "Lkotlin/ranges/ClosedFloatingPointRange;", "", "getStraightSpeed", "()Lkotlin/ranges/ClosedFloatingPointRange;", "straightSpeed$delegate", "Lnet/ccbluex/liquidbounce/config/types/RangedValue;", "diagonalSpeed", "getDiagonalSpeed", "diagonalSpeed$delegate", "afterJumpHandler", "Lnet/ccbluex/liquidbounce/event/EventHook;", "Lnet/ccbluex/liquidbounce/event/events/PlayerAfterJumpEvent;", "getAfterJumpHandler$annotations", "liquidbounce"})
@SourceDebugExtension(value={"SMAP\nScaffoldJumpStrafe.kt\nKotlin\n*S Kotlin\n*F\n+ 1 ScaffoldJumpStrafe.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/ScaffoldJumpStrafe\n+ 2 EntityExtensions.kt\nnet/ccbluex/liquidbounce/utils/entity/EntityExtensionsKt\n+ 3 EventListener.kt\nnet/ccbluex/liquidbounce/event/EventListenerKt\n*L\n1#1,62:1\n350#2:63\n96#3,4:64\n*S KotlinDebug\n*F\n+ 1 ScaffoldJumpStrafe.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/ScaffoldJumpStrafe\n*L\n58#1:63\n-1#1:64,4\n*E\n"})
public final class ScaffoldJumpStrafe
extends ToggleableValueGroup {
    @NotNull
    public static final ScaffoldJumpStrafe INSTANCE;
    static final /* synthetic */ KProperty<Object>[] $$delegatedProperties;
    @NotNull
    private static final RangedValue straightSpeed$delegate;
    @NotNull
    private static final RangedValue diagonalSpeed$delegate;
    @NotNull
    private static final EventHook<PlayerAfterJumpEvent> afterJumpHandler;

    private ScaffoldJumpStrafe() {
        super((EventListener)ModuleScaffold.INSTANCE, "StrafeOnJump", false, null, 8, null);
    }

    private final ClosedFloatingPointRange<Float> getStraightSpeed() {
        return (ClosedFloatingPointRange)straightSpeed$delegate.getValue((Object)this, $$delegatedProperties[0]);
    }

    private final ClosedFloatingPointRange<Float> getDiagonalSpeed() {
        return (ClosedFloatingPointRange)diagonalSpeed$delegate.getValue((Object)this, $$delegatedProperties[1]);
    }

    private static /* synthetic */ void getAfterJumpHandler$annotations() {
    }

    private static final void afterJumpHandler$lambda$0(PlayerAfterJumpEvent it) {
        Intrinsics.checkNotNullParameter((Object)it, (String)"it");
        float direction = EntityExtensionsKt.getMovementDirectionOfInput$default((LocalPlayer)INSTANCE.getPlayer(), null, (int)1, null) + (float)180;
        float movingYaw = (float)Math.rint(direction / (float)45) * (float)45;
        boolean isMovingStraight = movingYaw % (float)90 == 0.0f;
        ClosedFloatingPointRange<Float> speed = isMovingStraight ? INSTANCE.getStraightSpeed() : INSTANCE.getDiagonalSpeed();
        LocalPlayer localPlayer = INSTANCE.getPlayer();
        Vec3 vec3 = INSTANCE.getPlayer().getDeltaMovement();
        Intrinsics.checkNotNullExpressionValue((Object)vec3, (String)"getDeltaMovement(...)");
        localPlayer.setDeltaMovement(EntityExtensionsKt.withStrafe$default((Vec3)vec3, (double)ArrayExtensionsKt.random(speed), (double)0.0, null, (float)0.0f, (int)14, null));
        DebuggedOwner debuggedOwner = (DebuggedOwner)ModuleScaffold.INSTANCE;
        String string = "%.2f";
        Object[] objectArray = new Object[1];
        Entity $this$horizontalSpeed$iv = (Entity)INSTANCE.getPlayer();
        boolean $i$f$getHorizontalSpeed = false;
        objectArray[0] = $this$horizontalSpeed$iv.getDeltaMovement().horizontalDistance();
        String string2 = String.format(string, Arrays.copyOf(objectArray, objectArray.length));
        Intrinsics.checkNotNullExpressionValue((Object)string2, (String)"format(...)");
        ModuleDebug.INSTANCE.debugParameter(debuggedOwner, "Telly-Speed", (Object)string2);
    }

    /*
     * WARNING - void declaration
     */
    static {
        void $this$handler_u24default$iv;
        EventListener eventListener = new EventListener[]{Reflection.property1((PropertyReference1)((PropertyReference1)new PropertyReference1Impl(ScaffoldJumpStrafe.class, "straightSpeed", "getStraightSpeed()Lkotlin/ranges/ClosedFloatingPointRange;", 0))), Reflection.property1((PropertyReference1)((PropertyReference1)new PropertyReference1Impl(ScaffoldJumpStrafe.class, "diagonalSpeed", "getDiagonalSpeed()Lkotlin/ranges/ClosedFloatingPointRange;", 0)))};
        $$delegatedProperties = eventListener;
        INSTANCE = new ScaffoldJumpStrafe();
        straightSpeed$delegate = ValueGroup.floatRange$default((ValueGroup)((ValueGroup)INSTANCE), (String)"StraightSpeed", (ClosedFloatingPointRange)RangesKt.rangeTo((float)0.48f, (float)0.49f), (ClosedFloatingPointRange)RangesKt.rangeTo((float)0.1f, (float)1.0f), null, null, (int)24, null);
        diagonalSpeed$delegate = ValueGroup.floatRange$default((ValueGroup)((ValueGroup)INSTANCE), (String)"DiagonalSpeed", (ClosedFloatingPointRange)RangesKt.rangeTo((float)0.48f, (float)0.49f), (ClosedFloatingPointRange)RangesKt.rangeTo((float)0.1f, (float)1.0f), null, null, (int)24, null);
        eventListener = (EventListener)INSTANCE;
        Consumer<PlayerAfterJumpEvent> handler$iv = ScaffoldJumpStrafe::afterJumpHandler$lambda$0;
        short priority$iv = 0;
        boolean $i$f$handler = false;
        afterJumpHandler = EventListenerKt.handler((EventListener)$this$handler_u24default$iv, PlayerAfterJumpEvent.class, (short)priority$iv, handler$iv);
    }
}

