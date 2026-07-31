/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  kotlin.Metadata
 *  kotlin.ResultKt
 *  kotlin.Unit
 *  kotlin.coroutines.Continuation
 *  kotlin.coroutines.intrinsics.IntrinsicsKt
 *  kotlin.coroutines.jvm.internal.DebugMetadata
 *  kotlin.coroutines.jvm.internal.SuspendLambda
 *  kotlin.jvm.functions.Function3
 *  kotlin.jvm.internal.Intrinsics
 *  kotlin.jvm.internal.SourceDebugExtension
 *  kotlin.ranges.RangesKt
 *  kotlinx.coroutines.CoroutineScope
 *  net.ccbluex.liquidbounce.event.events.GameTickEvent
 *  net.ccbluex.liquidbounce.utils.entity.EntityExtensionsKt
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.world.phys.Vec3
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower;

import java.util.concurrent.ThreadLocalRandom;
import kotlin.Metadata;
import kotlin.ResultKt;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.intrinsics.IntrinsicsKt;
import kotlin.coroutines.jvm.internal.DebugMetadata;
import kotlin.coroutines.jvm.internal.SuspendLambda;
import kotlin.jvm.functions.Function3;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.SourceDebugExtension;
import kotlin.ranges.RangesKt;
import kotlinx.coroutines.CoroutineScope;
import net.ccbluex.liquidbounce.event.events.GameTickEvent;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower.ScaffoldTowerHypixel;
import net.ccbluex.liquidbounce.utils.entity.EntityExtensionsKt;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

@DebugMetadata(f="ScaffoldTowerHypixel.kt", l={}, nl={}, i={}, s={}, n={}, m="invokeSuspend", c="net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower.ScaffoldTowerHypixel$special$$inlined$tickHandler$default$1", v=2)
@Metadata(mv={2, 3, 0}, k=3, xi=50, d1={"\u0000\u0012\n\u0000\n\u0002\u0010\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\u0010\u0000\u001a\u00020\u0001*\u00020\u00022\u0006\u0010\u0003\u001a\u00020\u0004H\n\u00a8\u0006\u0005"}, d2={"<anonymous>", "", "Lkotlinx/coroutines/CoroutineScope;", "it", "Lnet/ccbluex/liquidbounce/event/events/GameTickEvent;", "net/ccbluex/liquidbounce/event/SuspendHandlersKt$tickHandler$1"})
@SourceDebugExtension(value={"SMAP\nSuspendHandlers.kt\nKotlin\n*S Kotlin\n*F\n+ 1 SuspendHandlers.kt\nnet/ccbluex/liquidbounce/event/SuspendHandlersKt$tickHandler$1\n+ 2 ScaffoldTowerHypixel.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/tower/ScaffoldTowerHypixel\n*L\n1#1,157:1\n37#2,26:158\n*E\n"})
public static final class ScaffoldTowerHypixel$special$.inlined.tickHandler.default.1
extends SuspendLambda
implements Function3<CoroutineScope, GameTickEvent, Continuation<? super Unit>, Object> {
    int label;
    private /* synthetic */ Object L$0;

    public ScaffoldTowerHypixel$special$.inlined.tickHandler.default.1(Continuation $completion) {
    }

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
}

