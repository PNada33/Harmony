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
 *  kotlin.ranges.ClosedFloatingPointRange
 *  kotlin.ranges.RangesKt
 *  kotlin.reflect.KProperty
 *  kotlinx.coroutines.CoroutineScope
 *  net.ccbluex.liquidbounce.config.types.RangedValue
 *  net.ccbluex.liquidbounce.config.types.Value
 *  net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
 *  net.ccbluex.liquidbounce.config.types.group.ValueGroup
 *  net.ccbluex.liquidbounce.event.EventHook
 *  net.ccbluex.liquidbounce.event.EventListener
 *  net.ccbluex.liquidbounce.event.EventListenerScopeKt
 *  net.ccbluex.liquidbounce.event.SuspendHandlerBehavior
 *  net.ccbluex.liquidbounce.event.SuspendHandlerBehavior$DiscardLatest
 *  net.ccbluex.liquidbounce.event.events.GameTickEvent
 *  net.ccbluex.liquidbounce.utils.entity.EntityExtensionsKt
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.world.effect.MobEffectInstance
 *  net.minecraft.world.effect.MobEffects
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.NotNull
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features;

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
import kotlin.ranges.ClosedFloatingPointRange;
import kotlin.ranges.RangesKt;
import kotlin.reflect.KProperty;
import kotlinx.coroutines.CoroutineScope;
import net.ccbluex.liquidbounce.config.types.RangedValue;
import net.ccbluex.liquidbounce.config.types.Value;
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup;
import net.ccbluex.liquidbounce.config.types.group.ValueGroup;
import net.ccbluex.liquidbounce.event.EventHook;
import net.ccbluex.liquidbounce.event.EventListener;
import net.ccbluex.liquidbounce.event.EventListenerScopeKt;
import net.ccbluex.liquidbounce.event.SuspendHandlerBehavior;
import net.ccbluex.liquidbounce.event.events.GameTickEvent;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold;
import net.ccbluex.liquidbounce.utils.entity.EntityExtensionsKt;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={2, 3, 0}, k=1, xi=50, d1={"\u00006\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0007\n\u0002\b\u0005\n\u0002\u0010\u000b\n\u0002\b\b\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003J\b\u0010\u0015\u001a\u00020\u0016H\u0016J\b\u0010\u0017\u001a\u00020\u0016H\u0016R\u001b\u0010\u0004\u001a\u00020\u00058BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\b\u0010\t\u001a\u0004\b\u0006\u0010\u0007R\u001b\u0010\n\u001a\u00020\u000b8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u000e\u0010\u000f\u001a\u0004\b\f\u0010\rR\u001b\u0010\u0010\u001a\u00020\u000b8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0012\u0010\u000f\u001a\u0004\b\u0011\u0010\rR\u000e\u0010\u0013\u001a\u00020\u0014X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0018\u001a\b\u0012\u0004\u0012\u00020\u001a0\u0019X\u0082\u0004\u00a2\u0006\b\n\u0000\u0012\u0004\b\u001b\u0010\u0003R\u001a\u0010\u001c\u001a\b\u0012\u0004\u0012\u00020\u001a0\u0019X\u0082\u0004\u00a2\u0006\b\n\u0000\u0012\u0004\b\u001d\u0010\u0003\u00a8\u0006\u001e"}, d2={"Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/ScaffoldStrafeFeature;", "Lnet/ccbluex/liquidbounce/config/types/group/ToggleableValueGroup;", "<init>", "()V", "speed", "", "getSpeed", "()F", "speed$delegate", "Lnet/ccbluex/liquidbounce/config/types/RangedValue;", "hypixel", "", "getHypixel", "()Z", "hypixel$delegate", "Lnet/ccbluex/liquidbounce/config/types/Value;", "onlyOnGround", "getOnlyOnGround", "onlyOnGround$delegate", "moveTicks", "", "onEnabled", "", "onDisabled", "moveTickHandler", "Lnet/ccbluex/liquidbounce/event/EventHook;", "Lnet/ccbluex/liquidbounce/event/events/GameTickEvent;", "getMoveTickHandler$annotations", "strafeHandler", "getStrafeHandler$annotations", "liquidbounce"})
@SourceDebugExtension(value={"SMAP\nScaffoldStrafeFeature.kt\nKotlin\n*S Kotlin\n*F\n+ 1 ScaffoldStrafeFeature.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/ScaffoldStrafeFeature\n+ 2 SuspendHandlers.kt\nnet/ccbluex/liquidbounce/event/SuspendHandlersKt\n+ 3 fake.kt\nkotlin/jvm/internal/FakeKt\n*L\n1#1,85:1\n57#2,7:86\n77#2,8:93\n85#2,2:102\n66#2:104\n77#2,10:105\n66#2:115\n1#3:101\n*S KotlinDebug\n*F\n+ 1 ScaffoldStrafeFeature.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/ScaffoldStrafeFeature\n*L\n-1#1:86,7\n-1#1:93,8\n-1#1:102,2\n-1#1:104\n-1#1:105,10\n-1#1:115\n-1#1:101\n*E\n"})
public final class ScaffoldStrafeFeature
extends ToggleableValueGroup {
    @NotNull
    public static final ScaffoldStrafeFeature INSTANCE;
    static final /* synthetic */ KProperty<Object>[] $$delegatedProperties;
    @NotNull
    private static final RangedValue speed$delegate;
    @NotNull
    private static final Value hypixel$delegate;
    @NotNull
    private static final Value onlyOnGround$delegate;
    private static int moveTicks;
    @NotNull
    private static final EventHook<GameTickEvent> moveTickHandler;
    @NotNull
    private static final EventHook<GameTickEvent> strafeHandler;

    private ScaffoldStrafeFeature() {
        super((EventListener)ModuleScaffold.INSTANCE, "Strafe", false, null, 8, null);
    }

    private final float getSpeed() {
        return ((Number)speed$delegate.getValue((Object)this, $$delegatedProperties[0])).floatValue();
    }

    private final boolean getHypixel() {
        return (Boolean)hypixel$delegate.getValue((Object)this, $$delegatedProperties[1]);
    }

    private final boolean getOnlyOnGround() {
        return (Boolean)onlyOnGround$delegate.getValue((Object)this, $$delegatedProperties[2]);
    }

    public void onEnabled() {
        moveTicks = 0;
        super.onEnabled();
    }

    public void onDisabled() {
        if (!this.getHypixel()) {
            return;
        }
        this.getPlayer().setDeltaMovement(this.getPlayer().getDeltaMovement().multiply(0.5, 1.0, 0.5));
        super.onDisabled();
    }

    private static /* synthetic */ void getMoveTickHandler$annotations() {
    }

    private static /* synthetic */ void getStrafeHandler$annotations() {
    }

    public static final /* synthetic */ int access$getMoveTicks$p() {
        return moveTicks;
    }

    public static final /* synthetic */ void access$setMoveTicks$p(int n) {
        moveTicks = n;
    }

    public static final /* synthetic */ boolean access$getOnlyOnGround(ScaffoldStrafeFeature $this) {
        return $this.getOnlyOnGround();
    }

    public static final /* synthetic */ boolean access$getHypixel(ScaffoldStrafeFeature $this) {
        return $this.getHypixel();
    }

    public static final /* synthetic */ float access$getSpeed(ScaffoldStrafeFeature $this) {
        return $this.getSpeed();
    }

    static {
        ContinuationInterceptor continuationInterceptor;
        boolean bl;
        SuspendHandlerBehavior $this$suspendHandler_u24lambda_u241$iv$iv;
        SuspendHandlerBehavior behavior$iv$iv;
        ContinuationInterceptor context$iv$iv;
        EventListener $this$suspendHandler_u24default$iv$iv;
        short priority$iv$iv;
        Function3 handler$iv$iv;
        block7: {
            CoroutineContext context$iv$iv2;
            block6: {
                boolean bl2;
                ContinuationInterceptor it$iv$iv;
                ContinuationInterceptor continuationInterceptor2;
                boolean $i$f$suspendHandler;
                boolean $i$f$tickHandler;
                Runnable onCancellation$iv;
                ContinuationInterceptor dispatcher$iv;
                EventListener $this$tickHandler_u24default$iv;
                block5: {
                    block4: {
                        KProperty[] kPropertyArray = new KProperty[]{Reflection.property1((PropertyReference1)((PropertyReference1)new PropertyReference1Impl(ScaffoldStrafeFeature.class, "speed", "getSpeed()F", 0))), Reflection.property1((PropertyReference1)((PropertyReference1)new PropertyReference1Impl(ScaffoldStrafeFeature.class, "hypixel", "getHypixel()Z", 0))), Reflection.property1((PropertyReference1)((PropertyReference1)new PropertyReference1Impl(ScaffoldStrafeFeature.class, "onlyOnGround", "getOnlyOnGround()Z", 0)))};
                        $$delegatedProperties = kPropertyArray;
                        INSTANCE = new ScaffoldStrafeFeature();
                        speed$delegate = ValueGroup.float$default((ValueGroup)((ValueGroup)INSTANCE), (String)"Speed", (float)0.247f, (ClosedFloatingPointRange)RangesKt.rangeTo((float)0.0f, (float)5.0f), null, null, (int)24, null);
                        hypixel$delegate = ValueGroup.boolean$default((ValueGroup)((ValueGroup)INSTANCE), (String)"Hypixel", (boolean)false, null, (int)4, null);
                        onlyOnGround$delegate = ValueGroup.boolean$default((ValueGroup)((ValueGroup)INSTANCE), (String)"OnlyOnGround", (boolean)false, null, (int)4, null);
                        $this$tickHandler_u24default$iv = (EventListener)INSTANCE;
                        dispatcher$iv = null;
                        onCancellation$iv = null;
                        $i$f$tickHandler = false;
                        EventListener eventListener = $this$tickHandler_u24default$iv;
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
                                        void $this$moveTickHandler_u24lambda_u240 = $this$suspendHandler;
                                        boolean bl = false;
                                        if (EntityExtensionsKt.getMoving((LocalPlayer)ScaffoldStrafeFeature.INSTANCE.getPlayer())) {
                                            ScaffoldStrafeFeature.access$setMoveTicks$p(ScaffoldStrafeFeature.access$getMoveTicks$p() + 1);
                                        } else {
                                            ScaffoldStrafeFeature.access$setMoveTicks$p(0);
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
                        $i$f$suspendHandler = false;
                        continuationInterceptor2 = (ContinuationInterceptor)context$iv$iv2.get((CoroutineContext.Key)ContinuationInterceptor.Key);
                        if (continuationInterceptor2 == null) break block4;
                        it$iv$iv = continuationInterceptor2;
                        bl2 = false;
                        CoroutineContext coroutineContext2 = context$iv$iv2.plus((CoroutineContext)EventListenerScopeKt.wrapContinuationInterceptor((EventListener)$this$suspendHandler_u24default$iv$iv, (ContinuationInterceptor)it$iv$iv));
                        continuationInterceptor2 = coroutineContext2;
                        if (coroutineContext2 != null) break block5;
                    }
                    continuationInterceptor2 = context$iv$iv2;
                }
                context$iv$iv = continuationInterceptor2;
                $this$suspendHandler_u24lambda_u241$iv$iv = behavior$iv$iv;
                bl = false;
                moveTickHandler = $this$suspendHandler_u24lambda_u241$iv$iv.createEventHook($this$suspendHandler_u24default$iv$iv, GameTickEvent.class, (CoroutineContext)context$iv$iv, priority$iv$iv, handler$iv$iv);
                $this$tickHandler_u24default$iv = (EventListener)INSTANCE;
                dispatcher$iv = null;
                onCancellation$iv = null;
                $i$f$tickHandler = false;
                $this$suspendHandler_u24default$iv$iv = $this$tickHandler_u24default$iv;
                context$iv$iv2 = (CoroutineContext)EventListenerScopeKt.wrapContinuationInterceptor((EventListener)$this$tickHandler_u24default$iv, dispatcher$iv);
                behavior$iv$iv = (SuspendHandlerBehavior)new SuspendHandlerBehavior.DiscardLatest(onCancellation$iv);
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
                                void $this$strafeHandler_u24lambda_u240 = $this$suspendHandler;
                                boolean bl = false;
                                if (!ScaffoldStrafeFeature.access$getOnlyOnGround(ScaffoldStrafeFeature.INSTANCE) || ScaffoldStrafeFeature.INSTANCE.getPlayer().onGround()) {
                                    if (ScaffoldStrafeFeature.access$getHypixel(ScaffoldStrafeFeature.INSTANCE)) {
                                        double speed = 0.207;
                                        MobEffectInstance mobEffectInstance = ScaffoldStrafeFeature.INSTANCE.getPlayer().getEffect(MobEffects.SPEED);
                                        if ((mobEffectInstance != null ? mobEffectInstance.getAmplifier() : -1) >= 0) {
                                            speed = 0.295;
                                        }
                                        if (ScaffoldStrafeFeature.INSTANCE.getPlayer().tickCount % 20 == 0 || ScaffoldStrafeFeature.access$getMoveTicks$p() <= 7) {
                                            speed = 0.09800000190734863;
                                        }
                                        LocalPlayer localPlayer = ScaffoldStrafeFeature.INSTANCE.getPlayer();
                                        Vec3 vec3 = ScaffoldStrafeFeature.INSTANCE.getPlayer().getDeltaMovement();
                                        Intrinsics.checkNotNullExpressionValue((Object)vec3, (String)"getDeltaMovement(...)");
                                        localPlayer.setDeltaMovement(EntityExtensionsKt.withStrafe$default((Vec3)vec3, (double)speed, (double)0.0, null, (float)0.0f, (int)14, null));
                                    } else {
                                        LocalPlayer localPlayer = ScaffoldStrafeFeature.INSTANCE.getPlayer();
                                        Vec3 vec3 = ScaffoldStrafeFeature.INSTANCE.getPlayer().getDeltaMovement();
                                        Intrinsics.checkNotNullExpressionValue((Object)vec3, (String)"getDeltaMovement(...)");
                                        localPlayer.setDeltaMovement(EntityExtensionsKt.withStrafe$default((Vec3)vec3, (double)ScaffoldStrafeFeature.access$getSpeed(ScaffoldStrafeFeature.INSTANCE), (double)0.0, null, (float)0.0f, (int)14, null));
                                    }
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
                $i$f$suspendHandler = false;
                continuationInterceptor = (ContinuationInterceptor)context$iv$iv2.get((CoroutineContext.Key)ContinuationInterceptor.Key);
                if (continuationInterceptor == null) break block6;
                it$iv$iv = continuationInterceptor;
                bl2 = false;
                CoroutineContext coroutineContext = context$iv$iv2.plus((CoroutineContext)EventListenerScopeKt.wrapContinuationInterceptor((EventListener)$this$suspendHandler_u24default$iv$iv, (ContinuationInterceptor)it$iv$iv));
                continuationInterceptor = coroutineContext;
                if (coroutineContext != null) break block7;
            }
            continuationInterceptor = context$iv$iv2;
        }
        context$iv$iv = continuationInterceptor;
        $this$suspendHandler_u24lambda_u241$iv$iv = behavior$iv$iv;
        bl = false;
        strafeHandler = $this$suspendHandler_u24lambda_u241$iv$iv.createEventHook($this$suspendHandler_u24default$iv$iv, GameTickEvent.class, (CoroutineContext)context$iv$iv, priority$iv$iv, handler$iv$iv);
    }
}

