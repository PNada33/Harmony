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
 *  kotlinx.coroutines.CoroutineScope
 *  net.ccbluex.liquidbounce.event.events.GameTickEvent
 *  net.ccbluex.liquidbounce.utils.entity.EntityExtensionsKt
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.world.effect.MobEffectInstance
 *  net.minecraft.world.effect.MobEffects
 *  net.minecraft.world.phys.Vec3
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features;

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
import kotlinx.coroutines.CoroutineScope;
import net.ccbluex.liquidbounce.event.events.GameTickEvent;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldStrafeFeature;
import net.ccbluex.liquidbounce.utils.entity.EntityExtensionsKt;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.Vec3;

@DebugMetadata(f="ScaffoldStrafeFeature.kt", l={}, nl={}, i={}, s={}, n={}, m="invokeSuspend", c="net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldStrafeFeature$special$$inlined$tickHandler$default$2", v=2)
@Metadata(mv={2, 3, 0}, k=3, xi=50, d1={"\u0000\u0012\n\u0000\n\u0002\u0010\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\u0010\u0000\u001a\u00020\u0001*\u00020\u00022\u0006\u0010\u0003\u001a\u00020\u0004H\n\u00a8\u0006\u0005"}, d2={"<anonymous>", "", "Lkotlinx/coroutines/CoroutineScope;", "it", "Lnet/ccbluex/liquidbounce/event/events/GameTickEvent;", "net/ccbluex/liquidbounce/event/SuspendHandlersKt$tickHandler$1"})
@SourceDebugExtension(value={"SMAP\nSuspendHandlers.kt\nKotlin\n*S Kotlin\n*F\n+ 1 SuspendHandlers.kt\nnet/ccbluex/liquidbounce/event/SuspendHandlersKt$tickHandler$1\n+ 2 ScaffoldStrafeFeature.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/ScaffoldStrafeFeature\n*L\n1#1,157:1\n64#2,20:158\n*E\n"})
public static final class ScaffoldStrafeFeature$special$.inlined.tickHandler.default.2
extends SuspendLambda
implements Function3<CoroutineScope, GameTickEvent, Continuation<? super Unit>, Object> {
    int label;
    private /* synthetic */ Object L$0;

    public ScaffoldStrafeFeature$special$.inlined.tickHandler.default.2(Continuation $completion) {
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
}

