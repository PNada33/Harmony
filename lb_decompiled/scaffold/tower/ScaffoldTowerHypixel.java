/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  kotlin.Metadata
 *  kotlin.ResultKt
 *  kotlin.Unit
 *  kotlin.collections.ArraysKt
 *  kotlin.coroutines.Continuation
 *  kotlin.coroutines.ContinuationInterceptor
 *  kotlin.coroutines.CoroutineContext
 *  kotlin.coroutines.CoroutineContext$Key
 *  kotlin.coroutines.intrinsics.IntrinsicsKt
 *  kotlin.jvm.functions.Function3
 *  kotlin.jvm.internal.Intrinsics
 *  kotlin.jvm.internal.SourceDebugExtension
 *  kotlin.ranges.RangesKt
 *  kotlinx.coroutines.CoroutineScope
 *  net.ccbluex.liquidbounce.event.EventHook
 *  net.ccbluex.liquidbounce.event.EventListener
 *  net.ccbluex.liquidbounce.event.EventListenerScopeKt
 *  net.ccbluex.liquidbounce.event.SuspendHandlerBehavior
 *  net.ccbluex.liquidbounce.event.SuspendHandlerBehavior$DiscardLatest
 *  net.ccbluex.liquidbounce.event.events.GameTickEvent
 *  net.ccbluex.liquidbounce.utils.block.BlockExtensionsKt
 *  net.ccbluex.liquidbounce.utils.entity.EntityExtensionsKt
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.core.BlockPos
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.phys.Vec3
 *  org.jetbrains.annotations.NotNull
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower;

import java.util.concurrent.ThreadLocalRandom;
import kotlin.Metadata;
import kotlin.ResultKt;
import kotlin.Unit;
import kotlin.collections.ArraysKt;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.ContinuationInterceptor;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.intrinsics.IntrinsicsKt;
import kotlin.jvm.functions.Function3;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.SourceDebugExtension;
import kotlin.ranges.RangesKt;
import kotlinx.coroutines.CoroutineScope;
import net.ccbluex.liquidbounce.event.EventHook;
import net.ccbluex.liquidbounce.event.EventListener;
import net.ccbluex.liquidbounce.event.EventListenerScopeKt;
import net.ccbluex.liquidbounce.event.SuspendHandlerBehavior;
import net.ccbluex.liquidbounce.event.events.GameTickEvent;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower.ScaffoldTower;
import net.ccbluex.liquidbounce.utils.block.BlockExtensionsKt;
import net.ccbluex.liquidbounce.utils.entity.EntityExtensionsKt;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={2, 3, 0}, k=1, xi=50, d1={"\u0000 \n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003J\u0010\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\tH\u0016R\u001a\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005X\u0082\u0004\u00a2\u0006\b\n\u0000\u0012\u0004\b\u0007\u0010\u0003\u00a8\u0006\u000b"}, d2={"Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/tower/ScaffoldTowerHypixel;", "Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/tower/ScaffoldTower;", "<init>", "()V", "tickHandler", "Lnet/ccbluex/liquidbounce/event/EventHook;", "Lnet/ccbluex/liquidbounce/event/events/GameTickEvent;", "getTickHandler$annotations", "getTargetedPosition", "Lnet/minecraft/core/BlockPos;", "blockPos", "liquidbounce"})
@SourceDebugExtension(value={"SMAP\nScaffoldTowerHypixel.kt\nKotlin\n*S Kotlin\n*F\n+ 1 ScaffoldTowerHypixel.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/tower/ScaffoldTowerHypixel\n+ 2 _Arrays.kt\nkotlin/collections/ArraysKt___ArraysKt\n+ 3 BlockExtensions.kt\nnet/ccbluex/liquidbounce/utils/block/BlockExtensionsKt\n+ 4 SuspendHandlers.kt\nnet/ccbluex/liquidbounce/event/SuspendHandlersKt\n+ 5 fake.kt\nkotlin/jvm/internal/FakeKt\n*L\n1#1,89:1\n17711#2,14:90\n130#3:104\n57#4,7:105\n77#4,8:112\n85#4,2:121\n66#4:123\n1#5:120\n*S KotlinDebug\n*F\n+ 1 ScaffoldTowerHypixel.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/tower/ScaffoldTowerHypixel\n*L\n74#1:90,14\n79#1:104\n36#1:105,7\n36#1:112,8\n36#1:121,2\n36#1:123\n36#1:120\n*E\n"})
public final class ScaffoldTowerHypixel
extends ScaffoldTower {
    @NotNull
    public static final ScaffoldTowerHypixel INSTANCE;
    @NotNull
    private static final EventHook<GameTickEvent> tickHandler;

    private ScaffoldTowerHypixel() {
        super("Hypixel", null);
    }

    private static /* synthetic */ void getTickHandler$annotations() {
    }

    @Override
    @NotNull
    public BlockPos getTargetedPosition(@NotNull BlockPos blockPos) {
        Intrinsics.checkNotNullParameter((Object)blockPos, (String)"blockPos");
        if (!EntityExtensionsKt.getMoving((LocalPlayer)this.getPlayer())) {
            BlockPos blockOffset;
            BlockPos blockPos2;
            BlockPos[] blocks;
            BlockPos[] blockPosArray = new BlockPos[]{blockPos.offset(0, 0, 1), blockPos.offset(0, 0, -1), blockPos.offset(1, 0, 0), blockPos.offset(-1, 0, 0)};
            BlockPos[] $this$minByOrNull$iv = blocks = blockPosArray;
            boolean $i$f$minByOrNull = false;
            if ($this$minByOrNull$iv.length == 0) {
                blockPos2 = null;
            } else {
                BlockPos minElem$iv = $this$minByOrNull$iv[0];
                int lastIndex$iv = ArraysKt.getLastIndex((Object[])$this$minByOrNull$iv);
                if (lastIndex$iv == 0) {
                    blockPos2 = minElem$iv;
                } else {
                    BlockPos blockPos3 = minElem$iv;
                    boolean bl = false;
                    Intrinsics.checkNotNull((Object)blockPos3);
                    double minValue$iv = BlockExtensionsKt.getCenterDistanceSquared((BlockPos)blockPos3);
                    int i$iv = 1;
                    if (i$iv <= lastIndex$iv) {
                        while (true) {
                            BlockPos e$iv;
                            BlockPos blockPos4 = e$iv = $this$minByOrNull$iv[i$iv];
                            $i$a$-minByOrNull-ScaffoldTowerHypixel$getTargetedPosition$blockOffset$1 = false;
                            Intrinsics.checkNotNull((Object)blockPos4);
                            double v$iv = BlockExtensionsKt.getCenterDistanceSquared((BlockPos)blockPos4);
                            if (Double.compare(minValue$iv, v$iv) > 0) {
                                minElem$iv = e$iv;
                                minValue$iv = v$iv;
                            }
                            if (i$iv == lastIndex$iv) break;
                            ++i$iv;
                        }
                    }
                    blockPos2 = minElem$iv;
                }
            }
            BlockPos blockPos5 = blockPos2;
            BlockPos $this$getState$iv = blockOffset = blockPos5 != null && ($this$minByOrNull$iv = blockPos5.below()) != null ? $this$minByOrNull$iv : blockPos;
            boolean bl = false;
            BlockState blockState = BlockExtensionsKt.getState((BlockPos)$this$getState$iv);
            Intrinsics.checkNotNull((Object)blockState);
            if (!blockState.isRedstoneConductor((BlockGetter)this.getWorld(), blockOffset)) {
                return blockOffset;
            }
        }
        return super.getTargetedPosition(blockPos);
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
                INSTANCE = new ScaffoldTowerHypixel();
                EventListener $this$tickHandler_u24default$iv = (EventListener)INSTANCE;
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
                                if (ScaffoldTowerHypixel.INSTANCE.getMc().options.keyJump.isDown() && ModuleScaffold.INSTANCE.getBlockCount() > 0 && ModuleScaffold.INSTANCE.isBlockBelow()) {
                                    if (!(ScaffoldTowerHypixel.INSTANCE.getPlayer().getX() % 1.0 == 0.0) && !EntityExtensionsKt.getMoving((LocalPlayer)ScaffoldTowerHypixel.INSTANCE.getPlayer())) {
                                        ScaffoldTowerHypixel.INSTANCE.getPlayer().getDeltaMovement().x = RangesKt.coerceAtMost((double)(Math.rint(ScaffoldTowerHypixel.INSTANCE.getPlayer().getX()) - ScaffoldTowerHypixel.INSTANCE.getPlayer().getX()), (double)0.281);
                                    }
                                    if (EntityExtensionsKt.getAirTicks((LocalPlayer)ScaffoldTowerHypixel.INSTANCE.getPlayer()) > 14) {
                                        Vec3 vec3 = ScaffoldTowerHypixel.INSTANCE.getPlayer().getDeltaMovement();
                                        vec3.y -= 0.09;
                                        ScaffoldTowerHypixel.INSTANCE.getPlayer().setDeltaMovement(ScaffoldTowerHypixel.INSTANCE.getPlayer().getDeltaMovement().multiply(0.6, 1.0, 0.6));
                                    } else {
                                        switch (EntityExtensionsKt.getAirTicks((LocalPlayer)ScaffoldTowerHypixel.INSTANCE.getPlayer()) % 3) {
                                            case 0: {
                                                ScaffoldTowerHypixel.INSTANCE.getPlayer().getDeltaMovement().y = 0.42;
                                                LocalPlayer localPlayer = ScaffoldTowerHypixel.INSTANCE.getPlayer();
                                                Vec3 vec3 = ScaffoldTowerHypixel.INSTANCE.getPlayer().getDeltaMovement();
                                                Intrinsics.checkNotNullExpressionValue((Object)vec3, (String)"getDeltaMovement(...)");
                                                localPlayer.setDeltaMovement(EntityExtensionsKt.withStrafe$default((Vec3)vec3, (double)(0.247 - (double)(ThreadLocalRandom.current().nextFloat() / 100.0f)), (double)0.0, null, (float)0.0f, (int)14, null));
                                                break;
                                            }
                                            case 2: {
                                                ScaffoldTowerHypixel.INSTANCE.getPlayer().getDeltaMovement().y = 1.0 - ScaffoldTowerHypixel.INSTANCE.getPlayer().getY() % 1.0;
                                            }
                                        }
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
    }
}

