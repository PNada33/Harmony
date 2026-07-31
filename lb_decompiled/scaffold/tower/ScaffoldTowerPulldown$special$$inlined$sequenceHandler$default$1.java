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
 *  kotlin.coroutines.jvm.internal.SpillingKt
 *  kotlin.coroutines.jvm.internal.SuspendLambda
 *  kotlin.jvm.functions.Function3
 *  kotlin.jvm.internal.SourceDebugExtension
 *  kotlinx.coroutines.CoroutineScope
 *  net.ccbluex.liquidbounce.event.CoroutineTickerKt
 *  net.ccbluex.liquidbounce.event.Event
 *  net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower;

import java.util.function.IntPredicate;
import kotlin.Metadata;
import kotlin.ResultKt;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.intrinsics.IntrinsicsKt;
import kotlin.coroutines.jvm.internal.DebugMetadata;
import kotlin.coroutines.jvm.internal.SpillingKt;
import kotlin.coroutines.jvm.internal.SuspendLambda;
import kotlin.jvm.functions.Function3;
import kotlin.jvm.internal.SourceDebugExtension;
import kotlinx.coroutines.CoroutineScope;
import net.ccbluex.liquidbounce.event.CoroutineTickerKt;
import net.ccbluex.liquidbounce.event.Event;
import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower.ScaffoldTowerPulldown;

@DebugMetadata(f="ScaffoldTowerPulldown.kt", l={163}, nl={164}, i={0, 0, 0, 0, 0, 0}, s={"L$0", "L$1", "L$2", "L$3", "L$4", "I$0"}, n={"$this$suspendHandler", "it", "$completion", "event", "$this$jumpHandler_u24lambda_u240", "$i$a$-sequenceHandler$default-ScaffoldTowerPulldown$jumpHandler$1"}, m="invokeSuspend", c="net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower.ScaffoldTowerPulldown$special$$inlined$sequenceHandler$default$1", v=2)
@Metadata(mv={2, 3, 0}, k=3, xi=50, d1={"\u0000\u0014\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\u0010\u0000\u001a\u00020\u0001\"\n\b\u0000\u0010\u0002\u0018\u0001*\u00020\u0003*\u00020\u00042\u0006\u0010\u0005\u001a\u0002H\u0002H\n\u00a8\u0006\u0006"}, d2={"<anonymous>", "", "T", "Lnet/ccbluex/liquidbounce/event/Event;", "Lkotlinx/coroutines/CoroutineScope;", "it", "net/ccbluex/liquidbounce/event/SuspendHandlersKt$sequenceHandler$1"})
@SourceDebugExtension(value={"SMAP\nSuspendHandlers.kt\nKotlin\n*S Kotlin\n*F\n+ 1 SuspendHandlers.kt\nnet/ccbluex/liquidbounce/event/SuspendHandlersKt$sequenceHandler$1\n+ 2 ScaffoldTowerPulldown.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/tower/ScaffoldTowerPulldown\n*L\n1#1,157:1\n33#2,10:158\n*E\n"})
public static final class ScaffoldTowerPulldown$special$.inlined.sequenceHandler.default.1
extends SuspendLambda
implements Function3<CoroutineScope, PlayerJumpEvent, Continuation<? super Unit>, Object> {
    int label;
    private /* synthetic */ Object L$0;
    /* synthetic */ Object L$1;
    Object L$2;
    Object L$3;
    Object L$4;
    int I$0;

    public ScaffoldTowerPulldown$special$.inlined.sequenceHandler.default.1(Continuation $completion) {
    }

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
                $i$a$-sequenceHandler$default-ScaffoldTowerPulldown$jumpHandler$1 = 0;
                if (event.getMotion() == 0.0f || event.isCancelled()) ** GOTO lbl33
                this.L$0 = SpillingKt.nullOutSpilledVariable((Object)$this$suspendHandler);
                this.L$1 = SpillingKt.nullOutSpilledVariable((Object)it);
                this.L$2 = SpillingKt.nullOutSpilledVariable((Object)$completion);
                this.L$3 = SpillingKt.nullOutSpilledVariable((Object)event);
                this.L$4 = SpillingKt.nullOutSpilledVariable((Object)$this$jumpHandler_u24lambda_u240);
                this.I$0 = $i$a$-sequenceHandler$default-ScaffoldTowerPulldown$jumpHandler$1;
                this.label = 1;
                v0 = CoroutineTickerKt.tickUntil((IntPredicate)ScaffoldTowerPulldown.jumpHandler.1.1.INSTANCE, (Continuation)this);
                if (v0 == var4_4) {
                    return var4_4;
                }
                ** GOTO lbl30
            }
            case 1: {
                $i$a$-sequenceHandler$default-ScaffoldTowerPulldown$jumpHandler$1 = this.I$0;
                $this$jumpHandler_u24lambda_u240 = (CoroutineScope)this.L$4;
                event = (PlayerJumpEvent)this.L$3;
                $completion = (Continuation)this.L$2;
                ResultKt.throwOnFailure((Object)$result);
                v0 = $result;
lbl30:
                // 2 sources

                if (ModuleScaffold.INSTANCE.isBlockBelow()) {
                    ScaffoldTowerPulldown.INSTANCE.getPlayer().getDeltaMovement().y = -1.0;
                }
lbl33:
                // 4 sources

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
}

