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
 *  kotlin.jvm.internal.SourceDebugExtension
 *  kotlinx.coroutines.CoroutineScope
 *  net.ccbluex.liquidbounce.event.EventHook
 *  net.ccbluex.liquidbounce.event.EventListener
 *  net.ccbluex.liquidbounce.event.EventListenerKt
 *  net.ccbluex.liquidbounce.event.EventListenerScopeKt
 *  net.ccbluex.liquidbounce.event.SuspendHandlerBehavior
 *  net.ccbluex.liquidbounce.event.SuspendHandlerBehavior$DiscardLatest
 *  net.ccbluex.liquidbounce.event.events.GameTickEvent
 *  net.ccbluex.liquidbounce.event.events.PacketEvent
 *  net.ccbluex.liquidbounce.utils.entity.EntityExtensionsKt
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.network.protocol.Packet
 *  net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
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
import kotlin.jvm.internal.SourceDebugExtension;
import kotlinx.coroutines.CoroutineScope;
import net.ccbluex.liquidbounce.event.EventHook;
import net.ccbluex.liquidbounce.event.EventListener;
import net.ccbluex.liquidbounce.event.EventListenerKt;
import net.ccbluex.liquidbounce.event.EventListenerScopeKt;
import net.ccbluex.liquidbounce.event.SuspendHandlerBehavior;
import net.ccbluex.liquidbounce.event.events.GameTickEvent;
import net.ccbluex.liquidbounce.event.events.PacketEvent;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower.ScaffoldTower;
import net.ccbluex.liquidbounce.utils.entity.EntityExtensionsKt;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.stats.Stats;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={2, 3, 0}, k=1, xi=50, d1={"\u0000 \n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003R\u001a\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005X\u0082\u0004\u00a2\u0006\b\n\u0000\u0012\u0004\b\u0007\u0010\u0003R\u001a\u0010\b\u001a\b\u0012\u0004\u0012\u00020\t0\u0005X\u0082\u0004\u00a2\u0006\b\n\u0000\u0012\u0004\b\n\u0010\u0003\u00a8\u0006\u000b"}, d2={"Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/tower/ScaffoldTowerVulcan;", "Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/tower/ScaffoldTower;", "<init>", "()V", "tickHandler", "Lnet/ccbluex/liquidbounce/event/EventHook;", "Lnet/ccbluex/liquidbounce/event/events/GameTickEvent;", "getTickHandler$annotations", "packetHandler", "Lnet/ccbluex/liquidbounce/event/events/PacketEvent;", "getPacketHandler$annotations", "liquidbounce"})
@SourceDebugExtension(value={"SMAP\nScaffoldTowerVulcan.kt\nKotlin\n*S Kotlin\n*F\n+ 1 ScaffoldTowerVulcan.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/tower/ScaffoldTowerVulcan\n+ 2 SuspendHandlers.kt\nnet/ccbluex/liquidbounce/event/SuspendHandlersKt\n+ 3 fake.kt\nkotlin/jvm/internal/FakeKt\n+ 4 EventListener.kt\nnet/ccbluex/liquidbounce/event/EventListenerKt\n*L\n1#1,58:1\n57#2,7:59\n77#2,8:66\n85#2,2:75\n66#2:77\n1#3:74\n96#4,4:78\n*S KotlinDebug\n*F\n+ 1 ScaffoldTowerVulcan.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/tower/ScaffoldTowerVulcan\n*L\n33#1:59,7\n33#1:66,8\n33#1:75,2\n33#1:77\n33#1:74\n47#1:78,4\n*E\n"})
public final class ScaffoldTowerVulcan
extends ScaffoldTower {
    @NotNull
    public static final ScaffoldTowerVulcan INSTANCE;
    @NotNull
    private static final EventHook<GameTickEvent> tickHandler;
    @NotNull
    private static final EventHook<PacketEvent> packetHandler;

    private ScaffoldTowerVulcan() {
        super("Vulcan", null);
    }

    private static /* synthetic */ void getTickHandler$annotations() {
    }

    private static /* synthetic */ void getPacketHandler$annotations() {
    }

    private static final void packetHandler$lambda$0(PacketEvent event) {
        Intrinsics.checkNotNullParameter((Object)event, (String)"event");
        Packet packet = event.getPacket();
        if (packet instanceof ServerboundMovePlayerPacket && !EntityExtensionsKt.getMoving((LocalPlayer)INSTANCE.getPlayer()) && ScaffoldTowerVulcan.INSTANCE.getPlayer().tickCount % 2 == 0) {
            ServerboundMovePlayerPacket serverboundMovePlayerPacket = (ServerboundMovePlayerPacket)packet;
            serverboundMovePlayerPacket.x += 0.1;
            serverboundMovePlayerPacket = (ServerboundMovePlayerPacket)packet;
            serverboundMovePlayerPacket.z += 0.1;
        }
    }

    /*
     * WARNING - void declaration
     */
    static {
        void $this$handler_u24default$iv;
        void behavior$iv$iv;
        void $this$suspendHandler_u24default$iv$iv;
        ContinuationInterceptor continuationInterceptor;
        short priority$iv$iv;
        Function3 handler$iv$iv;
        EventListener $this$tickHandler_u24default$iv;
        block3: {
            void context$iv$iv;
            block2: {
                INSTANCE = new ScaffoldTowerVulcan();
                $this$tickHandler_u24default$iv = (EventListener)INSTANCE;
                ContinuationInterceptor dispatcher$iv = null;
                Runnable onCancellation$iv = null;
                boolean $i$f$tickHandler = false;
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
                                void $this$tickHandler_u24lambda_u240 = $this$suspendHandler;
                                boolean bl = false;
                                if (ScaffoldTowerVulcan.INSTANCE.getMc().options.keyJump.isDown() && ModuleScaffold.INSTANCE.getBlockCount() > 0 && ModuleScaffold.INSTANCE.isBlockBelow()) {
                                    if (ScaffoldTowerVulcan.INSTANCE.getPlayer().tickCount % 2 == 0) {
                                        ScaffoldTowerVulcan.INSTANCE.getPlayer().getDeltaMovement().y = 0.7;
                                    } else {
                                        ScaffoldTowerVulcan.INSTANCE.getPlayer().getDeltaMovement().y = EntityExtensionsKt.getMoving((LocalPlayer)ScaffoldTowerVulcan.INSTANCE.getPlayer()) ? (double)0.42f : 0.6;
                                        ScaffoldTowerVulcan.INSTANCE.getPlayer().awardStat(Stats.JUMP);
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
        $this$tickHandler_u24default$iv = (EventListener)INSTANCE;
        Consumer<PacketEvent> handler$iv = ScaffoldTowerVulcan::packetHandler$lambda$0;
        short priority$iv = 0;
        boolean $i$f$handler = false;
        packetHandler = EventListenerKt.handler((EventListener)$this$handler_u24default$iv, PacketEvent.class, (short)priority$iv, handler$iv);
    }
}

