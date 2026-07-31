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
 *  kotlin.coroutines.jvm.internal.SpillingKt
 *  kotlin.jvm.functions.Function3
 *  kotlin.jvm.internal.PropertyReference1
 *  kotlin.jvm.internal.PropertyReference1Impl
 *  kotlin.jvm.internal.Reflection
 *  kotlin.jvm.internal.SourceDebugExtension
 *  kotlin.ranges.ClosedFloatingPointRange
 *  kotlin.ranges.RangesKt
 *  kotlin.reflect.KProperty
 *  kotlinx.coroutines.CoroutineScope
 *  kotlinx.coroutines.CoroutineStart
 *  net.ccbluex.liquidbounce.config.types.RangedValue
 *  net.ccbluex.liquidbounce.config.types.Value
 *  net.ccbluex.liquidbounce.config.types.group.ValueGroup
 *  net.ccbluex.liquidbounce.event.CoroutineTickerKt
 *  net.ccbluex.liquidbounce.event.Event
 *  net.ccbluex.liquidbounce.event.EventHook
 *  net.ccbluex.liquidbounce.event.EventListener
 *  net.ccbluex.liquidbounce.event.EventListenerScopeKt
 *  net.ccbluex.liquidbounce.event.SuspendHandlerBehavior
 *  net.ccbluex.liquidbounce.event.SuspendHandlerBehavior$Parallel
 *  net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
 *  net.ccbluex.liquidbounce.features.module.ClientModule
 *  net.ccbluex.liquidbounce.utils.client.Timer
 *  net.ccbluex.liquidbounce.utils.kotlin.Priority
 *  org.jetbrains.annotations.NotNull
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower;

import java.util.function.IntPredicate;
import kotlin.Metadata;
import kotlin.ResultKt;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.ContinuationInterceptor;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.intrinsics.IntrinsicsKt;
import kotlin.coroutines.jvm.internal.SpillingKt;
import kotlin.jvm.functions.Function3;
import kotlin.jvm.internal.PropertyReference1;
import kotlin.jvm.internal.PropertyReference1Impl;
import kotlin.jvm.internal.Reflection;
import kotlin.jvm.internal.SourceDebugExtension;
import kotlin.ranges.ClosedFloatingPointRange;
import kotlin.ranges.RangesKt;
import kotlin.reflect.KProperty;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.CoroutineStart;
import net.ccbluex.liquidbounce.config.types.RangedValue;
import net.ccbluex.liquidbounce.config.types.Value;
import net.ccbluex.liquidbounce.config.types.group.ValueGroup;
import net.ccbluex.liquidbounce.event.CoroutineTickerKt;
import net.ccbluex.liquidbounce.event.Event;
import net.ccbluex.liquidbounce.event.EventHook;
import net.ccbluex.liquidbounce.event.EventListener;
import net.ccbluex.liquidbounce.event.EventListenerScopeKt;
import net.ccbluex.liquidbounce.event.SuspendHandlerBehavior;
import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent;
import net.ccbluex.liquidbounce.features.module.ClientModule;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower.ScaffoldTower;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower.ScaffoldTowerKarhu;
import net.ccbluex.liquidbounce.utils.client.Timer;
import net.ccbluex.liquidbounce.utils.kotlin.Priority;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={2, 3, 0}, k=1, xi=50, d1={"\u0000(\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0007\n\u0002\b\b\n\u0002\u0010\u000b\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003R\u001b\u0010\u0004\u001a\u00020\u00058BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\b\u0010\t\u001a\u0004\b\u0006\u0010\u0007R\u001b\u0010\n\u001a\u00020\u00058BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\f\u0010\t\u001a\u0004\b\u000b\u0010\u0007R\u001b\u0010\r\u001a\u00020\u000e8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0011\u0010\u0012\u001a\u0004\b\u000f\u0010\u0010R\u001a\u0010\u0013\u001a\b\u0012\u0004\u0012\u00020\u00150\u0014X\u0082\u0004\u00a2\u0006\b\n\u0000\u0012\u0004\b\u0016\u0010\u0003\u00a8\u0006\u0017"}, d2={"Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/tower/ScaffoldTowerKarhu;", "Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/tower/ScaffoldTower;", "<init>", "()V", "timerSpeed", "", "getTimerSpeed", "()F", "timerSpeed$delegate", "Lnet/ccbluex/liquidbounce/config/types/RangedValue;", "triggerMotion", "getTriggerMotion", "triggerMotion$delegate", "pulldown", "", "getPulldown", "()Z", "pulldown$delegate", "Lnet/ccbluex/liquidbounce/config/types/Value;", "jumpHandler", "Lnet/ccbluex/liquidbounce/event/EventHook;", "Lnet/ccbluex/liquidbounce/event/events/PlayerJumpEvent;", "getJumpHandler$annotations", "liquidbounce"})
@SourceDebugExtension(value={"SMAP\nScaffoldTowerKarhu.kt\nKotlin\n*S Kotlin\n*F\n+ 1 ScaffoldTowerKarhu.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/tower/ScaffoldTowerKarhu\n+ 2 SuspendHandlers.kt\nnet/ccbluex/liquidbounce/event/SuspendHandlersKt\n+ 3 fake.kt\nkotlin/jvm/internal/FakeKt\n*L\n1#1,58:1\n41#2,9:59\n84#2:68\n85#2,2:70\n52#2:72\n1#3:69\n*S KotlinDebug\n*F\n+ 1 ScaffoldTowerKarhu.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/tower/ScaffoldTowerKarhu\n*L\n-1#1:59,9\n-1#1:68\n-1#1:70,2\n-1#1:72\n-1#1:69\n*E\n"})
public final class ScaffoldTowerKarhu
extends ScaffoldTower {
    @NotNull
    public static final ScaffoldTowerKarhu INSTANCE;
    static final /* synthetic */ KProperty<Object>[] $$delegatedProperties;
    @NotNull
    private static final RangedValue timerSpeed$delegate;
    @NotNull
    private static final RangedValue triggerMotion$delegate;
    @NotNull
    private static final Value pulldown$delegate;
    @NotNull
    private static final EventHook<PlayerJumpEvent> jumpHandler;

    private ScaffoldTowerKarhu() {
        super("Karhu", null);
    }

    private final float getTimerSpeed() {
        return ((Number)timerSpeed$delegate.getValue((Object)this, $$delegatedProperties[0])).floatValue();
    }

    private final float getTriggerMotion() {
        return ((Number)triggerMotion$delegate.getValue((Object)this, $$delegatedProperties[1])).floatValue();
    }

    private final boolean getPulldown() {
        return (Boolean)pulldown$delegate.getValue((Object)this, $$delegatedProperties[2]);
    }

    private static /* synthetic */ void getJumpHandler$annotations() {
    }

    public static final /* synthetic */ float access$getTimerSpeed(ScaffoldTowerKarhu $this) {
        return $this.getTimerSpeed();
    }

    public static final /* synthetic */ boolean access$getPulldown(ScaffoldTowerKarhu $this) {
        return $this.getPulldown();
    }

    public static final /* synthetic */ float access$getTriggerMotion(ScaffoldTowerKarhu $this) {
        return $this.getTriggerMotion();
    }

    /*
     * WARNING - void declaration
     */
    static {
        void priority$iv$iv;
        void behavior$iv$iv;
        void $this$suspendHandler$iv$iv;
        ContinuationInterceptor continuationInterceptor;
        Function3 handler$iv$iv;
        block3: {
            void context$iv$iv;
            block2: {
                void $this$sequenceHandler_u24default$iv;
                EventListener eventListener = new EventListener[]{Reflection.property1((PropertyReference1)((PropertyReference1)new PropertyReference1Impl(ScaffoldTowerKarhu.class, "timerSpeed", "getTimerSpeed()F", 0))), Reflection.property1((PropertyReference1)((PropertyReference1)new PropertyReference1Impl(ScaffoldTowerKarhu.class, "triggerMotion", "getTriggerMotion()F", 0))), Reflection.property1((PropertyReference1)((PropertyReference1)new PropertyReference1Impl(ScaffoldTowerKarhu.class, "pulldown", "getPulldown()Z", 0)))};
                $$delegatedProperties = eventListener;
                INSTANCE = new ScaffoldTowerKarhu();
                timerSpeed$delegate = ValueGroup.float$default((ValueGroup)((ValueGroup)INSTANCE), (String)"Timer", (float)5.0f, (ClosedFloatingPointRange)RangesKt.rangeTo((float)0.1f, (float)10.0f), null, null, (int)24, null);
                triggerMotion$delegate = ValueGroup.float$default((ValueGroup)((ValueGroup)INSTANCE), (String)"Trigger", (float)0.06f, (ClosedFloatingPointRange)RangesKt.rangeTo((float)0.0f, (float)0.2f), (String)"Y/v", null, (int)16, null);
                pulldown$delegate = ValueGroup.boolean$default((ValueGroup)((ValueGroup)INSTANCE), (String)"Pulldown", (boolean)true, null, (int)4, null);
                eventListener = (EventListener)INSTANCE;
                int priority$iv = -1000;
                ContinuationInterceptor dispatcher$iv = null;
                Runnable onCancellation$iv = null;
                boolean $i$f$sequenceHandler = false;
                void var5_5 = $this$sequenceHandler_u24default$iv;
                CoroutineContext coroutineContext = (CoroutineContext)EventListenerScopeKt.wrapContinuationInterceptor((EventListener)$this$sequenceHandler_u24default$iv, dispatcher$iv);
                int n = priority$iv;
                SuspendHandlerBehavior suspendHandlerBehavior = (SuspendHandlerBehavior)new SuspendHandlerBehavior.Parallel(CoroutineStart.UNDISPATCHED, onCancellation$iv);
                handler$iv$iv = (Function3)new Function3<CoroutineScope, PlayerJumpEvent, Continuation<? super Unit>, Object>(null){
                    int label;
                    private /* synthetic */ Object L$0;
                    /* synthetic */ Object L$1;
                    Object L$2;
                    Object L$3;
                    Object L$4;
                    int I$0;

                    /*
                     * Unable to fully structure code
                     */
                    public final Object invokeSuspend(Object $result) {
                        var2_2 = (CoroutineScope)this.L$0;
                        var3_3 = (Event)this.L$1;
                        var4_4 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
                        switch (this.label) {
                            case 0: {
                                ResultKt.throwOnFailure((Object)$result);
                                var5_5 = (Continuation)this;
                                var6_7 = (PlayerJumpEvent)it;
                                $this$jumpHandler_u24lambda_u240 = $this$suspendHandler;
                                $i$a$-sequenceHandler$default-ScaffoldTowerKarhu$jumpHandler$1 = 0;
                                if (event.getMotion() == 0.0f || event.isCancelled()) ** GOTO lbl54
                                this.L$0 = SpillingKt.nullOutSpilledVariable((Object)$this$suspendHandler);
                                this.L$1 = SpillingKt.nullOutSpilledVariable((Object)it);
                                this.L$2 = SpillingKt.nullOutSpilledVariable((Object)$completion);
                                this.L$3 = SpillingKt.nullOutSpilledVariable((Object)event);
                                this.L$4 = SpillingKt.nullOutSpilledVariable((Object)$this$jumpHandler_u24lambda_u240);
                                this.I$0 = $i$a$-sequenceHandler$default-ScaffoldTowerKarhu$jumpHandler$1;
                                this.label = 1;
                                v0 = CoroutineTickerKt.tickUntil((IntPredicate)jumpHandler.1.1.INSTANCE, (Continuation)this);
                                if (v0 == var4_4) {
                                    return var4_4;
                                }
                                ** GOTO lbl30
                            }
                            case 1: {
                                $i$a$-sequenceHandler$default-ScaffoldTowerKarhu$jumpHandler$1 = this.I$0;
                                $this$jumpHandler_u24lambda_u240 = (CoroutineScope)this.L$4;
                                event = (PlayerJumpEvent)this.L$3;
                                $completion = (Continuation)this.L$2;
                                ResultKt.throwOnFailure((Object)$result);
                                v0 = $result;
lbl30:
                                // 2 sources

                                Timer.requestTimerSpeed$default((Timer)Timer.INSTANCE, (float)ScaffoldTowerKarhu.access$getTimerSpeed(ScaffoldTowerKarhu.INSTANCE), (Priority)Priority.IMPORTANT_FOR_USAGE_1, (ClientModule)ModuleScaffold.INSTANCE, (int)0, (int)8, null);
                                if (!ScaffoldTowerKarhu.access$getPulldown(ScaffoldTowerKarhu.INSTANCE)) ** GOTO lbl54
                                this.L$0 = SpillingKt.nullOutSpilledVariable((Object)$this$suspendHandler);
                                this.L$1 = SpillingKt.nullOutSpilledVariable((Object)it);
                                this.L$2 = SpillingKt.nullOutSpilledVariable((Object)$completion);
                                this.L$3 = SpillingKt.nullOutSpilledVariable((Object)event);
                                this.L$4 = SpillingKt.nullOutSpilledVariable((Object)$this$jumpHandler_u24lambda_u240);
                                this.I$0 = $i$a$-sequenceHandler$default-ScaffoldTowerKarhu$jumpHandler$1;
                                this.label = 2;
                                v1 = CoroutineTickerKt.tickUntil((IntPredicate)jumpHandler.1.2.INSTANCE, (Continuation)this);
                                if (v1 == var4_4) {
                                    return var4_4;
                                }
                                ** GOTO lbl50
                            }
                            case 2: {
                                $i$a$-sequenceHandler$default-ScaffoldTowerKarhu$jumpHandler$1 = this.I$0;
                                $this$jumpHandler_u24lambda_u240 = (CoroutineScope)this.L$4;
                                event = (PlayerJumpEvent)this.L$3;
                                $completion = (Continuation)this.L$2;
                                ResultKt.throwOnFailure((Object)$result);
                                v1 = $result;
lbl50:
                                // 2 sources

                                if (ModuleScaffold.INSTANCE.isBlockBelow()) {
                                    var9_13 = ScaffoldTowerKarhu.INSTANCE.getPlayer().getDeltaMovement();
                                    var9_13.y -= (double)1.0f;
                                }
lbl54:
                                // 5 sources

                                return Unit.INSTANCE;
                            }
                        }
                        throw new IllegalStateException("call to 'resume' before 'invoke' with coroutine");
                    }

                    /*
                     * Ignored method signature, as it can't be verified against descriptor
                     */
                    public final Object invoke(CoroutineScope p1, Event p2, Continuation p3) {
                        var var4_4 = new /* invalid duplicate definition of identical inner class */;
                        var4_4.L$0 = p1;
                        var4_4.L$1 = p2;
                        return var4_4.invokeSuspend(Unit.INSTANCE);
                    }
                };
                boolean $i$f$suspendHandler = false;
                continuationInterceptor = (ContinuationInterceptor)context$iv$iv.get((CoroutineContext.Key)ContinuationInterceptor.Key);
                if (continuationInterceptor == null) break block2;
                ContinuationInterceptor it$iv$iv = continuationInterceptor;
                boolean bl = false;
                CoroutineContext coroutineContext2 = context$iv$iv.plus((CoroutineContext)EventListenerScopeKt.wrapContinuationInterceptor((EventListener)$this$suspendHandler$iv$iv, (ContinuationInterceptor)it$iv$iv));
                continuationInterceptor = coroutineContext2;
                if (coroutineContext2 != null) break block3;
            }
            continuationInterceptor = context$iv$iv;
        }
        ContinuationInterceptor context$iv$iv = continuationInterceptor;
        void $this$suspendHandler_u24lambda_u241$iv$iv = behavior$iv$iv;
        boolean bl = false;
        jumpHandler = $this$suspendHandler_u24lambda_u241$iv$iv.createEventHook((EventListener)$this$suspendHandler$iv$iv, PlayerJumpEvent.class, (CoroutineContext)context$iv$iv, (short)priority$iv$iv, handler$iv$iv);
    }
}

