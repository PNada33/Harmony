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
 *  kotlin.jvm.internal.SourceDebugExtension
 *  kotlin.math.MathKt
 *  kotlinx.coroutines.CoroutineScope
 *  net.ccbluex.liquidbounce.event.events.GameTickEvent
 *  net.minecraft.stats.Stats
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower;

import kotlin.Metadata;
import kotlin.ResultKt;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.intrinsics.IntrinsicsKt;
import kotlin.coroutines.jvm.internal.DebugMetadata;
import kotlin.coroutines.jvm.internal.SuspendLambda;
import kotlin.jvm.functions.Function3;
import kotlin.jvm.internal.SourceDebugExtension;
import kotlin.math.MathKt;
import kotlinx.coroutines.CoroutineScope;
import net.ccbluex.liquidbounce.event.events.GameTickEvent;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower.ScaffoldTowerMotion;
import net.minecraft.stats.Stats;

@DebugMetadata(f="ScaffoldTowerMotion.kt", l={}, nl={}, i={}, s={}, n={}, m="invokeSuspend", c="net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower.ScaffoldTowerMotion$special$$inlined$tickHandler$default$1", v=2)
@Metadata(mv={2, 3, 0}, k=3, xi=50, d1={"\u0000\u0012\n\u0000\n\u0002\u0010\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\u0010\u0000\u001a\u00020\u0001*\u00020\u00022\u0006\u0010\u0003\u001a\u00020\u0004H\n\u00a8\u0006\u0005"}, d2={"<anonymous>", "", "Lkotlinx/coroutines/CoroutineScope;", "it", "Lnet/ccbluex/liquidbounce/event/events/GameTickEvent;", "net/ccbluex/liquidbounce/event/SuspendHandlersKt$tickHandler$1"})
@SourceDebugExtension(value={"SMAP\nSuspendHandlers.kt\nKotlin\n*S Kotlin\n*F\n+ 1 SuspendHandlers.kt\nnet/ccbluex/liquidbounce/event/SuspendHandlersKt$tickHandler$1\n+ 2 ScaffoldTowerMotion.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/tower/ScaffoldTowerMotion\n*L\n1#1,157:1\n47#2,23:158\n*E\n"})
public static final class ScaffoldTowerMotion$special$.inlined.tickHandler.default.1
extends SuspendLambda
implements Function3<CoroutineScope, GameTickEvent, Continuation<? super Unit>, Object> {
    int label;
    private /* synthetic */ Object L$0;

    public ScaffoldTowerMotion$special$.inlined.tickHandler.default.1(Continuation $completion) {
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
}

