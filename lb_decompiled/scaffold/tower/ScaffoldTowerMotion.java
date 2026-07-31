/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  kotlin.Metadata
 *  kotlin.ResultKt
 *  kotlin.Unit
 *  kotlin.coroutines.Continuation
 *  kotlin.coroutines.ContinuationInterceptor
 *  kotlin.coroutines.CoroutineContext
 *  kotlin.coroutines.CoroutineContext$Key
 *  kotlin.coroutines.intrinsics.IntrinsicsKt
 *  kotlin.jvm.functions.Function3
 *  kotlin.jvm.internal.Intrinsics
 *  kotlin.jvm.internal.PropertyReference1
 *  kotlin.jvm.internal.PropertyReference1Impl
 *  kotlin.jvm.internal.Reflection
 *  kotlin.jvm.internal.SourceDebugExtension
 *  kotlin.math.MathKt
 *  kotlin.ranges.ClosedFloatingPointRange
 *  kotlin.ranges.RangesKt
 *  kotlin.reflect.KProperty
 *  kotlinx.coroutines.CoroutineScope
 *  net.ccbluex.liquidbounce.config.types.RangedValue
 *  net.ccbluex.liquidbounce.config.types.group.ValueGroup
 *  net.ccbluex.liquidbounce.event.EventHook
 *  net.ccbluex.liquidbounce.event.EventListener
 *  net.ccbluex.liquidbounce.event.EventListenerKt
 *  net.ccbluex.liquidbounce.event.EventListenerScopeKt
 *  net.ccbluex.liquidbounce.event.SuspendHandlerBehavior
 *  net.ccbluex.liquidbounce.event.SuspendHandlerBehavior$DiscardLatest
 *  net.ccbluex.liquidbounce.event.events.GameTickEvent
 *  net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
 *  net.minecraft.stats.Stats
 *  org.jetbrains.annotations.NotNull
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower;

import java.util.function.Consumer;
import kotlin.Metadata;
import kotlin.ResultKt;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.ContinuationInterceptor;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.intrinsics.IntrinsicsKt;
import kotlin.jvm.functions.Function3;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.PropertyReference1;
import kotlin.jvm.internal.PropertyReference1Impl;
import kotlin.jvm.internal.Reflection;
import kotlin.jvm.internal.SourceDebugExtension;
import kotlin.math.MathKt;
import kotlin.ranges.ClosedFloatingPointRange;
import kotlin.ranges.RangesKt;
import kotlin.reflect.KProperty;
import kotlinx.coroutines.CoroutineScope;
import net.ccbluex.liquidbounce.config.types.RangedValue;
import net.ccbluex.liquidbounce.config.types.group.ValueGroup;
import net.ccbluex.liquidbounce.event.EventHook;
import net.ccbluex.liquidbounce.event.EventListener;
import net.ccbluex.liquidbounce.event.EventListenerKt;
import net.ccbluex.liquidbounce.event.EventListenerScopeKt;
import net.ccbluex.liquidbounce.event.SuspendHandlerBehavior;
import net.ccbluex.liquidbounce.event.events.GameTickEvent;
import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower.ScaffoldTower;
import net.minecraft.stats.Stats;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={2, 3, 0}, k=1, xi=50, d1={"\u0000.\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0007\n\u0002\b\u000b\n\u0002\u0010\u0006\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003R\u001b\u0010\u0004\u001a\u00020\u00058BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\b\u0010\t\u001a\u0004\b\u0006\u0010\u0007R\u001b\u0010\n\u001a\u00020\u00058BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\f\u0010\t\u001a\u0004\b\u000b\u0010\u0007R\u001b\u0010\r\u001a\u00020\u00058BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u000f\u0010\t\u001a\u0004\b\u000e\u0010\u0007R\u000e\u0010\u0010\u001a\u00020\u0011X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0012\u001a\b\u0012\u0004\u0012\u00020\u00140\u0013X\u0082\u0004\u00a2\u0006\b\n\u0000\u0012\u0004\b\u0015\u0010\u0003R\u001a\u0010\u0016\u001a\b\u0012\u0004\u0012\u00020\u00170\u0013X\u0082\u0004\u00a2\u0006\b\n\u0000\u0012\u0004\b\u0018\u0010\u0003\u00a8\u0006\u0019"}, d2={"Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/tower/ScaffoldTowerMotion;", "Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/tower/ScaffoldTower;", "<init>", "()V", "motion", "", "getMotion", "()F", "motion$delegate", "Lnet/ccbluex/liquidbounce/config/types/RangedValue;", "triggerHeight", "getTriggerHeight", "triggerHeight$delegate", "slow", "getSlow", "slow$delegate", "jumpOffPosition", "", "jumpHandler", "Lnet/ccbluex/liquidbounce/event/EventHook;", "Lnet/ccbluex/liquidbounce/event/events/PlayerJumpEvent;", "getJumpHandler$annotations", "tickHandler", "Lnet/ccbluex/liquidbounce/event/events/GameTickEvent;", "getTickHandler$annotations", "liquidbounce"})
@SourceDebugExtension(value={"SMAP\nScaffoldTowerMotion.kt\nKotlin\n*S Kotlin\n*F\n+ 1 ScaffoldTowerMotion.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/tower/ScaffoldTowerMotion\n+ 2 EventListener.kt\nnet/ccbluex/liquidbounce/event/EventListenerKt\n+ 3 SuspendHandlers.kt\nnet/ccbluex/liquidbounce/event/SuspendHandlersKt\n+ 4 fake.kt\nkotlin/jvm/internal/FakeKt\n*L\n1#1,72:1\n96#2,4:73\n57#3,7:77\n77#3,8:84\n85#3,2:93\n66#3:95\n1#4:92\n*S KotlinDebug\n*F\n+ 1 ScaffoldTowerMotion.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/tower/ScaffoldTowerMotion\n*L\n-1#1:73,4\n-1#1:77,7\n-1#1:84,8\n-1#1:93,2\n-1#1:95\n-1#1:92\n*E\n"})
public final class ScaffoldTowerMotion
extends ScaffoldTower {
    @NotNull
    public static final ScaffoldTowerMotion INSTANCE;
    static final /* synthetic */ KProperty<Object>[] $$delegatedProperties;
    @NotNull
    private static final RangedValue motion$delegate;
    @NotNull
    private static final RangedValue triggerHeight$delegate;
    @NotNull
    private static final RangedValue slow$delegate;
    private static double jumpOffPosition;
    @NotNull
    private static final EventHook<PlayerJumpEvent> jumpHandler;
    @NotNull
    private static final EventHook<GameTickEvent> tickHandler;

    private ScaffoldTowerMotion() {
        super("Motion", null);
    }

    private final float getMotion() {
        return ((Number)motion$delegate.getValue((Object)this, $$delegatedProperties[0])).floatValue();
    }

    private final float getTriggerHeight() {
        return ((Number)triggerHeight$delegate.getValue((Object)this, $$delegatedProperties[1])).floatValue();
    }

    private final float getSlow() {
        return ((Number)slow$delegate.getValue((Object)this, $$delegatedProperties[2])).floatValue();
    }

    private static /* synthetic */ void getJumpHandler$annotations() {
    }

    private static /* synthetic */ void getTickHandler$annotations() {
    }

    private static final void jumpHandler$lambda$0(PlayerJumpEvent it) {
        Intrinsics.checkNotNullParameter((Object)it, (String)"it");
        jumpOffPosition = INSTANCE.getPlayer().getY();
    }

    public static final /* synthetic */ void access$setJumpOffPosition$p(double d) {
        jumpOffPosition = d;
    }

    public static final /* synthetic */ double access$getJumpOffPosition$p() {
        return jumpOffPosition;
    }

    public static final /* synthetic */ float access$getTriggerHeight(ScaffoldTowerMotion $this) {
        return $this.getTriggerHeight();
    }

    public static final /* synthetic */ float access$getMotion(ScaffoldTowerMotion $this) {
        return $this.getMotion();
    }

    public static final /* synthetic */ float access$getSlow(ScaffoldTowerMotion $this) {
        return $this.getSlow();
    }

    /*
     * WARNING - void declaration
     */
    static {
        void behavior$iv$iv;
        void $this$suspendHandler_u24default$iv$iv;
        ContinuationInterceptor continuationInterceptor;
        short priority$iv$iv;
        Function3 handler$iv$iv;
        block3: {
            void context$iv$iv;
            block2: {
                void $this$handler_u24default$iv;
                EventListener eventListener = new EventListener[]{Reflection.property1((PropertyReference1)((PropertyReference1)new PropertyReference1Impl(ScaffoldTowerMotion.class, "motion", "getMotion()F", 0))), Reflection.property1((PropertyReference1)((PropertyReference1)new PropertyReference1Impl(ScaffoldTowerMotion.class, "triggerHeight", "getTriggerHeight()F", 0))), Reflection.property1((PropertyReference1)((PropertyReference1)new PropertyReference1Impl(ScaffoldTowerMotion.class, "slow", "getSlow()F", 0)))};
                $$delegatedProperties = eventListener;
                INSTANCE = new ScaffoldTowerMotion();
                motion$delegate = ValueGroup.float$default((ValueGroup)((ValueGroup)INSTANCE), (String)"Motion", (float)0.42f, (ClosedFloatingPointRange)RangesKt.rangeTo((float)0.0f, (float)1.0f), null, null, (int)24, null);
                triggerHeight$delegate = ValueGroup.float$default((ValueGroup)((ValueGroup)INSTANCE), (String)"TriggerHeight", (float)0.78f, (ClosedFloatingPointRange)RangesKt.rangeTo((float)0.76f, (float)1.0f), null, null, (int)24, null);
                slow$delegate = ValueGroup.float$default((ValueGroup)((ValueGroup)INSTANCE), (String)"Slow", (float)1.0f, (ClosedFloatingPointRange)RangesKt.rangeTo((float)0.0f, (float)3.0f), null, null, (int)24, null);
                jumpOffPosition = Double.NaN;
                eventListener = (EventListener)INSTANCE;
                Consumer<PlayerJumpEvent> handler$iv = ScaffoldTowerMotion::jumpHandler$lambda$0;
                short priority$iv = 0;
                boolean $i$f$handler = false;
                jumpHandler = EventListenerKt.handler((EventListener)$this$handler_u24default$iv, PlayerJumpEvent.class, (short)priority$iv, handler$iv);
                EventListener $this$tickHandler_u24default$iv = (EventListener)INSTANCE;
                ContinuationInterceptor dispatcher$iv = null;
                Runnable onCancellation$iv = null;
                boolean $i$f$tickHandler = false;
                EventListener eventListener2 = $this$tickHandler_u24default$iv;
                CoroutineContext coroutineContext = (CoroutineContext)EventListenerScopeKt.wrapContinuationInterceptor((EventListener)$this$tickHandler_u24default$iv, dispatcher$iv);
                SuspendHandlerBehavior suspendHandlerBehavior = (SuspendHandlerBehavior)new SuspendHandlerBehavior.DiscardLatest(onCancellation$iv);
                handler$iv$iv = (Function3)new Function3<CoroutineScope, GameTickEvent, Continuation<? super Unit>, Object>(null){
                    int label;
                    private /* synthetic */ Object L$0;

                    /*
                     * WARNING - void declaration
                     */
                    public final Object invokeSuspend(Object $result) {
                        CoroutineScope coroutineScope = (CoroutineScope)this.L$0;
                        IntrinsicsKt.getCOROUTINE_SUSPENDED();
                        switch (this.label) {
                            case 0: {
                                void $this$suspendHandler;
                                ResultKt.throwOnFailure((Object)$result);
                                Continuation continuation = (Continuation)this;
                                void $this$tickHandler_u24lambda_u240 = $this$suspendHandler;
                                boolean bl = false;
                                if (!ScaffoldTowerMotion.INSTANCE.getMc().options.keyJump.isDown() || ModuleScaffold.INSTANCE.getBlockCount() <= 0 || !ModuleScaffold.INSTANCE.isBlockBelow()) {
                                    ScaffoldTowerMotion.access$setJumpOffPosition$p(Double.NaN);
                                } else if (!Double.isNaN(ScaffoldTowerMotion.access$getJumpOffPosition$p()) && ScaffoldTowerMotion.INSTANCE.getPlayer().getY() > ScaffoldTowerMotion.access$getJumpOffPosition$p() + (double)ScaffoldTowerMotion.access$getTriggerHeight(ScaffoldTowerMotion.INSTANCE)) {
                                    ScaffoldTowerMotion.INSTANCE.getPlayer().setPos(ScaffoldTowerMotion.INSTANCE.getPlayer().getX(), MathKt.truncate((double)ScaffoldTowerMotion.INSTANCE.getPlayer().getY()), ScaffoldTowerMotion.INSTANCE.getPlayer().getZ());
                                    ScaffoldTowerMotion.INSTANCE.getPlayer().getDeltaMovement().y = ScaffoldTowerMotion.access$getMotion(ScaffoldTowerMotion.INSTANCE);
                                    ScaffoldTowerMotion.INSTANCE.getPlayer().setDeltaMovement(ScaffoldTowerMotion.INSTANCE.getPlayer().getDeltaMovement().multiply((double)ScaffoldTowerMotion.access$getSlow(ScaffoldTowerMotion.INSTANCE), 1.0, (double)ScaffoldTowerMotion.access$getSlow(ScaffoldTowerMotion.INSTANCE)));
                                    ScaffoldTowerMotion.INSTANCE.getPlayer().awardStat(Stats.JUMP);
                                    ScaffoldTowerMotion.access$setJumpOffPosition$p(ScaffoldTowerMotion.INSTANCE.getPlayer().getY());
                                }
                                return Unit.INSTANCE;
                            }
                        }
                        throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
                    }

                    public final Object invoke(CoroutineScope p1, GameTickEvent p2, Continuation<? super Unit> p3) {
                        var var4_4 = new /* invalid duplicate definition of identical inner class */;
                        var4_4.L$0 = p1;
                        return var4_4.invokeSuspend(Unit.INSTANCE);
                    }
                };
                priority$iv$iv = 0;
                boolean $i$f$suspendHandler = false;
                continuationInterceptor = (ContinuationInterceptor)context$iv$iv.get((CoroutineContext.Key)ContinuationInterceptor.Key);
                if (continuationInterceptor == null) break block2;
                ContinuationInterceptor it$iv$iv = continuationInterceptor;
                boolean bl = false;
                CoroutineContext coroutineContext2 = context$iv$iv.plus((CoroutineContext)EventListenerScopeKt.wrapContinuationInterceptor((EventListener)$this$suspendHandler_u24default$iv$iv, (ContinuationInterceptor)it$iv$iv));
                continuationInterceptor = coroutineContext2;
                if (coroutineContext2 != null) break block3;
            }
            continuationInterceptor = context$iv$iv;
        }
        ContinuationInterceptor context$iv$iv = continuationInterceptor;
        void $this$suspendHandler_u24lambda_u241$iv$iv = behavior$iv$iv;
        boolean bl = false;
        tickHandler = $this$suspendHandler_u24lambda_u241$iv$iv.createEventHook((EventListener)$this$suspendHandler_u24default$iv$iv, GameTickEvent.class, (CoroutineContext)context$iv$iv, priority$iv$iv, handler$iv$iv);
    }
}

