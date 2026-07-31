/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  kotlin.Metadata
 *  kotlin.ResultKt
 *  kotlin.Unit
 *  kotlin.coroutines.Continuation
 *  kotlin.coroutines.intrinsics.IntrinsicsKt
 *  kotlin.coroutines.jvm.internal.Boxing
 *  kotlin.coroutines.jvm.internal.DebugMetadata
 *  kotlin.coroutines.jvm.internal.SpillingKt
 *  kotlin.coroutines.jvm.internal.SuspendLambda
 *  kotlin.jvm.functions.Function0
 *  kotlin.jvm.functions.Function3
 *  kotlin.jvm.internal.Intrinsics
 *  kotlin.jvm.internal.Ref$BooleanRef
 *  kotlin.jvm.internal.SourceDebugExtension
 *  kotlin.random.Random
 *  kotlin.ranges.IntRange
 *  kotlin.ranges.RangesKt
 *  kotlinx.coroutines.CoroutineScope
 *  net.ccbluex.liquidbounce.event.CoroutineTickerKt
 *  net.ccbluex.liquidbounce.event.events.GameTickEvent
 *  net.ccbluex.liquidbounce.features.misc.DebuggedOwner
 *  net.ccbluex.liquidbounce.features.module.ClientModule
 *  net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
 *  net.ccbluex.liquidbounce.utils.aiming.RotationManager
 *  net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup
 *  net.ccbluex.liquidbounce.utils.aiming.data.Rotation
 *  net.ccbluex.liquidbounce.utils.aiming.utils.RotationUtilKt
 *  net.ccbluex.liquidbounce.utils.block.BlockExtensionsKt
 *  net.ccbluex.liquidbounce.utils.block.SwingMode
 *  net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTarget
 *  net.ccbluex.liquidbounce.utils.entity.EntityExtensionsKt
 *  net.ccbluex.liquidbounce.utils.kotlin.Priority
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.network.protocol.Packet
 *  net.minecraft.network.protocol.game.ServerboundMovePlayerPacket$PosRot
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.Vec3
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold;

import kotlin.Metadata;
import kotlin.ResultKt;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.intrinsics.IntrinsicsKt;
import kotlin.coroutines.jvm.internal.Boxing;
import kotlin.coroutines.jvm.internal.DebugMetadata;
import kotlin.coroutines.jvm.internal.SpillingKt;
import kotlin.coroutines.jvm.internal.SuspendLambda;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function3;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.Ref;
import kotlin.jvm.internal.SourceDebugExtension;
import kotlin.random.Random;
import kotlin.ranges.IntRange;
import kotlin.ranges.RangesKt;
import kotlinx.coroutines.CoroutineScope;
import net.ccbluex.liquidbounce.event.CoroutineTickerKt;
import net.ccbluex.liquidbounce.event.events.GameTickEvent;
import net.ccbluex.liquidbounce.features.misc.DebuggedOwner;
import net.ccbluex.liquidbounce.features.module.ClientModule;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ScaffoldBlockItemSelection;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldAutoBlockFeature;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldMovementPrediction;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.ScaffoldTechnique;
import net.ccbluex.liquidbounce.utils.aiming.RotationManager;
import net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup;
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation;
import net.ccbluex.liquidbounce.utils.aiming.utils.RotationUtilKt;
import net.ccbluex.liquidbounce.utils.block.BlockExtensionsKt;
import net.ccbluex.liquidbounce.utils.block.SwingMode;
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTarget;
import net.ccbluex.liquidbounce.utils.entity.EntityExtensionsKt;
import net.ccbluex.liquidbounce.utils.kotlin.Priority;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

@DebugMetadata(f="ModuleScaffold.kt", l={290}, nl={292}, i={0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, s={"L$0", "L$1", "L$2", "L$3", "L$4", "L$5", "L$6", "L$7", "L$8", "L$9", "L$10", "I$0", "I$1", "Z$0", "Z$1"}, n={"$this$suspendHandler", "$completion", "$this$tickHandler_u24lambda_u240", "target", "technique", "currentRotation", "currentCrosshairTarget", "handToInteractWith", "wasSuccessful", "previousFallOffPos", "suitableHand", "$i$a$-tickHandler$default-ModuleScaffold$tickHandler$1", "currentDelay", "hasBlockInMainHand", "hasBlockInOffHand"}, m="invokeSuspend", c="net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold$special$$inlined$tickHandler$default$1", v=2)
@Metadata(mv={2, 3, 0}, k=3, xi=50, d1={"\u0000\u0012\n\u0000\n\u0002\u0010\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\u0010\u0000\u001a\u00020\u0001*\u00020\u00022\u0006\u0010\u0003\u001a\u00020\u0004H\n\u00a8\u0006\u0005"}, d2={"<anonymous>", "", "Lkotlinx/coroutines/CoroutineScope;", "it", "Lnet/ccbluex/liquidbounce/event/events/GameTickEvent;", "net/ccbluex/liquidbounce/event/SuspendHandlersKt$tickHandler$1"})
@SourceDebugExtension(value={"SMAP\nSuspendHandlers.kt\nKotlin\n*S Kotlin\n*F\n+ 1 SuspendHandlers.kt\nnet/ccbluex/liquidbounce/event/SuspendHandlersKt$tickHandler$1\n+ 2 ModuleScaffold.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/ModuleScaffold\n+ 3 ModuleDebug.kt\nnet/ccbluex/liquidbounce/features/module/modules/render/ModuleDebug\n+ 4 _Collections.kt\nkotlin/collections/CollectionsKt___CollectionsKt\n+ 5 fake.kt\nkotlin/jvm/internal/FakeKt\n*L\n1#1,157:1\n493#2,15:158\n508#2:179\n510#2,20:186\n530#2:207\n541#2,61:209\n603#2,22:271\n285#3,6:173\n285#3,6:180\n296#4:206\n297#4:208\n1#5:270\n*S KotlinDebug\n*F\n+ 1 ModuleScaffold.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/ModuleScaffold\n*L\n507#1:173,6\n508#1:180,6\n529#1:206\n529#1:208\n*E\n"})
public static final class ModuleScaffold$special$.inlined.tickHandler.default.1
extends SuspendLambda
implements Function3<CoroutineScope, GameTickEvent, Continuation<? super Unit>, Object> {
    int label;
    private /* synthetic */ Object L$0;
    Object L$1;
    Object L$2;
    Object L$3;
    Object L$4;
    Object L$5;
    Object L$6;
    Object L$7;
    Object L$8;
    Object L$9;
    Object L$10;
    int I$0;
    int I$1;
    boolean Z$0;
    boolean Z$1;

    public ModuleScaffold$special$.inlined.tickHandler.default.1(Continuation $completion) {
    }

    /*
     * Unable to fully structure code
     */
    public final Object invokeSuspend(Object $result) {
        var2_2 = (CoroutineScope)this.L$0;
        var3_3 = IntrinsicsKt.getCOROUTINE_SUSPENDED();
        switch (this.label) {
            case 0: {
                ResultKt.throwOnFailure((Object)$result);
                var4_4 = (Continuation)this;
                $this$tickHandler_u24lambda_u240 = $this$suspendHandler;
                $i$a$-tickHandler$default-ModuleScaffold$tickHandler$1 = 0;
                ModuleScaffold.access$updateRenderCount(ModuleScaffold.INSTANCE, Boxing.boxInt((int)ModuleScaffold.INSTANCE.getBlockCount()));
                if (ModuleScaffold.INSTANCE.getPlayer().onGround()) {
                    ModuleScaffold.access$setPlacementY$p(ModuleScaffold.INSTANCE.getPlayer().blockPosition().getY() - 1);
                    var7_10 = ModuleScaffold.access$getJumps$p();
                    ModuleScaffold.access$setJumps$p(var7_10 + 1);
                    ModuleScaffold.access$setWasTowering$p(false);
                }
                if (ModuleScaffold.INSTANCE.getMc().options.keyJump.isDown()) {
                    ModuleScaffold.access$setStartY$p(ModuleScaffold.INSTANCE.getPlayer().blockPosition().getY());
                    ModuleScaffold.access$setJumps$p(2);
                }
                var7_11 = ModuleDebug.INSTANCE;
                var8_13 = (DebuggedOwner)ModuleScaffold.INSTANCE;
                name$iv = "IsTowering";
                $i$f$debugParameter = false;
                if (this_$iv.getRunning()) {
                    var11_20 = name$iv;
                    var12_21 = $this$debugParameter$iv;
                    var13_22 = this_$iv;
                    $i$a$-debugParameter-ModuleScaffold$tickHandler$1$1 = false;
                    var15_25 = Boxing.boxBoolean((boolean)ModuleScaffold.INSTANCE.isTowering$liquidbounce());
                    var13_22.debugParameter(var12_21, var11_20, (Object)var15_25);
                }
                this_$iv = ModuleDebug.INSTANCE;
                $this$debugParameter$iv = (DebuggedOwner)ModuleScaffold.INSTANCE;
                name$iv = "WasTowering";
                $i$f$debugParameter = false;
                if (this_$iv.getRunning()) {
                    var11_20 = name$iv;
                    var12_21 = $this$debugParameter$iv;
                    var13_22 = this_$iv;
                    $i$a$-debugParameter-ModuleScaffold$tickHandler$1$2 = false;
                    var15_25 = Boxing.boxBoolean((boolean)ModuleScaffold.access$getWasTowering$p());
                    var13_22.debugParameter(var12_21, var11_20, (Object)var15_25);
                }
                target = ModuleScaffold.access$getCurrentTarget$p();
                technique = ModuleScaffold.access$getActiveTechnique(ModuleScaffold.INSTANCE);
                if ((ModuleScaffold.ScaffoldRotationValueGroup.INSTANCE.getRotationTiming() == ModuleScaffold.ScaffoldRotationValueGroup.RotationTimingMode.ON_TICK || ModuleScaffold.ScaffoldRotationValueGroup.INSTANCE.getRotationTiming() == ModuleScaffold.ScaffoldRotationValueGroup.RotationTimingMode.ON_TICK_SNAP) && target != null) {
                    v0 = technique.getRotations(target);
                    if (v0 == null && (v0 = RotationManager.INSTANCE.getCurrentRotation()) == null) {
                        v0 = EntityExtensionsKt.getRotation((Entity)((Entity)ModuleScaffold.INSTANCE.getPlayer()));
                    }
                } else {
                    v0 = RotationManager.INSTANCE.getCurrentRotation();
                    if (v0 == null) {
                        v0 = EntityExtensionsKt.getRotation((Entity)((Entity)ModuleScaffold.INSTANCE.getPlayer()));
                    }
                }
                currentRotation = v0.normalize();
                currentCrosshairTarget = technique.getCrosshairTarget(target, currentRotation);
                currentDelay = RangesKt.random((IntRange)ModuleScaffold.access$getDelay(ModuleScaffold.INSTANCE), (Random)((Random)Random.Default));
                hasBlockInMainHand = ScaffoldBlockItemSelection.INSTANCE.isValidBlock(ModuleScaffold.INSTANCE.getPlayer().getInventory().getItem(ModuleScaffold.INSTANCE.getPlayer().getInventory().getSelectedSlot()));
                hasBlockInOffHand = ScaffoldBlockItemSelection.INSTANCE.isValidBlock(ModuleScaffold.INSTANCE.getPlayer().getOffhandItem());
                if (ScaffoldAutoBlockFeature.INSTANCE.getAlwaysHoldBlock()) {
                    hasBlockInMainHand = ModuleScaffold.access$handleSilentBlockSelection(ModuleScaffold.INSTANCE, hasBlockInMainHand != false, hasBlockInOffHand != false);
                }
                $this$firstOrNull$iv = (Iterable)ModuleScaffold.EntriesMappings.entries$0;
                $i$f$firstOrNull = false;
                for (T element$iv : $this$firstOrNull$iv) {
                    it = (InteractionHand)element$iv;
                    $i$a$-firstOrNull-ModuleScaffold$tickHandler$1$suitableHand$1 = false;
                    if (!ScaffoldBlockItemSelection.INSTANCE.isValidBlock(ModuleScaffold.INSTANCE.getPlayer().getItemInHand(it))) continue;
                    v1 = element$iv;
                    ** GOTO lbl68
                }
                v1 = null;
lbl68:
                // 2 sources

                suitableHand = v1;
                if (ModuleScaffold.access$simulatePlacementAttempts(ModuleScaffold.INSTANCE, currentCrosshairTarget, suitableHand) && EntityExtensionsKt.getMoving((LocalPlayer)ModuleScaffold.INSTANCE.getPlayer()) && ModuleScaffold.SimulatePlacementAttempts.INSTANCE.getClicker().isClickTick()) {
                    ModuleScaffold.SimulatePlacementAttempts.INSTANCE.getClicker().click((Function0)new Function0<Boolean>(currentCrosshairTarget, suitableHand){
                        final /* synthetic */ BlockHitResult $currentCrosshairTarget;
                        final /* synthetic */ InteractionHand $suitableHand;
                        {
                            this.$currentCrosshairTarget = $currentCrosshairTarget;
                            this.$suitableHand = $suitableHand;
                        }

                        public final Boolean invoke() {
                            BlockHitResult blockHitResult = this.$currentCrosshairTarget;
                            Intrinsics.checkNotNull((Object)blockHitResult);
                            InteractionHand interactionHand = this.$suitableHand;
                            Intrinsics.checkNotNull((Object)interactionHand);
                            BlockExtensionsKt.doPlacement$default((BlockHitResult)blockHitResult, (InteractionHand)interactionHand, (Function0)((Function0)new Function0<Boolean>(this.$currentCrosshairTarget){
                                final /* synthetic */ BlockHitResult $currentCrosshairTarget;
                                {
                                    this.$currentCrosshairTarget = $currentCrosshairTarget;
                                }

                                public final Boolean invoke() {
                                    ModuleScaffold.access$tickHandler$lambda$0$commonPlaceSucceed(BlockExtensionsKt.getTargetBlockPos((BlockHitResult)this.$currentCrosshairTarget));
                                    return true;
                                }
                            }), null, (SwingMode)ModuleScaffold.access$getSwingMode(ModuleScaffold.INSTANCE), (int)8, null);
                            return true;
                        }
                    });
                }
                if (target != null && currentCrosshairTarget != null && target.doesCrosshairTargetMatchRequirements(currentCrosshairTarget) && ModuleScaffold.INSTANCE.isValidCrosshairTarget$liquidbounce(currentCrosshairTarget)) {
                    if (!ScaffoldAutoBlockFeature.INSTANCE.getAlwaysHoldBlock()) {
                        hasBlockInMainHand = ModuleScaffold.access$handleSilentBlockSelection(ModuleScaffold.INSTANCE, hasBlockInMainHand != false, hasBlockInOffHand != false);
                    }
                    if (hasBlockInMainHand || hasBlockInOffHand) {
                        handToInteractWith = hasBlockInMainHand != false ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
                        wasSuccessful = new Ref.BooleanRef();
                        if (ModuleScaffold.ScaffoldRotationValueGroup.INSTANCE.getRotationTiming() == ModuleScaffold.ScaffoldRotationValueGroup.RotationTimingMode.ON_TICK || ModuleScaffold.ScaffoldRotationValueGroup.INSTANCE.getRotationTiming() == ModuleScaffold.ScaffoldRotationValueGroup.RotationTimingMode.ON_TICK_SNAP) {
                            if (!Intrinsics.areEqual((Object)currentRotation, (Object)RotationManager.INSTANCE.getServerRotation())) {
                                ModuleScaffold.INSTANCE.getNetwork().send((Packet)new ServerboundMovePlayerPacket.PosRot(ModuleScaffold.INSTANCE.getPlayer().getX(), ModuleScaffold.INSTANCE.getPlayer().getY(), ModuleScaffold.INSTANCE.getPlayer().getZ(), currentRotation.yaw(), currentRotation.pitch(), ModuleScaffold.INSTANCE.getPlayer().onGround(), ModuleScaffold.INSTANCE.getPlayer().horizontalCollision));
                            }
                            if (ModuleScaffold.ScaffoldRotationValueGroup.INSTANCE.getRotationTiming() == ModuleScaffold.ScaffoldRotationValueGroup.RotationTimingMode.ON_TICK_SNAP) {
                                var20_35 = RotationManager.INSTANCE;
                                var21_38 = ModuleScaffold.ScaffoldRotationValueGroup.INSTANCE.getConsiderInventory();
                                var22_39 = ModuleScaffold.ScaffoldRotationValueGroup.INSTANCE;
                                $i$a$-firstOrNull-ModuleScaffold$tickHandler$1$suitableHand$1 = ModuleScaffold.INSTANCE;
                                var25_45 = Priority.IMPORTANT_FOR_PLAYER_LIFE;
                                RotationManager.setRotationTarget$default((RotationManager)var20_35, (Rotation)currentRotation, (boolean)(var21_38 != false), (RotationsValueGroup)var22_39, (Priority)var25_45, (ClientModule)$i$a$-firstOrNull-ModuleScaffold$tickHandler$1$suitableHand$1, null, (int)32, null);
                            }
                        }
                        v2 = ModuleScaffold.INSTANCE.getCurrentOptimalLine();
                        if (v2 != null) {
                            l = v2;
                            $i$a$-let-ModuleScaffold$tickHandler$1$previousFallOffPos$1 = false;
                            v3 = ScaffoldMovementPrediction.INSTANCE.getFallOffPositionOnLine(l);
                        } else {
                            v3 = null;
                        }
                        previousFallOffPos = v3;
                        BlockExtensionsKt.doPlacement$default((BlockHitResult)currentCrosshairTarget, (InteractionHand)handToInteractWith, (Function0)((Function0)new Function0<Boolean>(target, wasSuccessful){
                            final /* synthetic */ BlockPlacementTarget $target;
                            final /* synthetic */ Ref.BooleanRef $wasSuccessful;
                            {
                                this.$target = $target;
                                this.$wasSuccessful = $wasSuccessful;
                            }

                            public final Boolean invoke() {
                                ModuleScaffold.access$tickHandler$lambda$0$commonPlaceSucceed(this.$target.getPlacedBlock());
                                ModuleScaffold.access$setCurrentTarget$p(null);
                                this.$wasSuccessful.element = true;
                                return true;
                            }
                        }), null, (SwingMode)ModuleScaffold.access$getSwingMode(ModuleScaffold.INSTANCE), (int)8, null);
                        if (ModuleScaffold.ScaffoldRotationValueGroup.INSTANCE.getRotationTiming() == ModuleScaffold.ScaffoldRotationValueGroup.RotationTimingMode.ON_TICK && !Intrinsics.areEqual((Object)RotationManager.INSTANCE.getServerRotation(), (Object)EntityExtensionsKt.getRotation((Entity)((Entity)ModuleScaffold.INSTANCE.getPlayer())))) {
                            ModuleScaffold.INSTANCE.getNetwork().send((Packet)new ServerboundMovePlayerPacket.PosRot(ModuleScaffold.INSTANCE.getPlayer().getX(), ModuleScaffold.INSTANCE.getPlayer().getY(), ModuleScaffold.INSTANCE.getPlayer().getZ(), RotationUtilKt.withFixedYaw((LocalPlayer)ModuleScaffold.INSTANCE.getPlayer(), (Rotation)currentRotation), ModuleScaffold.INSTANCE.getPlayer().getXRot(), ModuleScaffold.INSTANCE.getPlayer().onGround(), ModuleScaffold.INSTANCE.getPlayer().horizontalCollision));
                        }
                        if (wasSuccessful.element) {
                            ScaffoldMovementPrediction.INSTANCE.onPlace(ModuleScaffold.INSTANCE.getCurrentOptimalLine(), previousFallOffPos);
                            this.L$0 = SpillingKt.nullOutSpilledVariable((Object)$this$suspendHandler);
                            this.L$1 = SpillingKt.nullOutSpilledVariable((Object)$completion);
                            this.L$2 = SpillingKt.nullOutSpilledVariable((Object)$this$tickHandler_u24lambda_u240);
                            this.L$3 = SpillingKt.nullOutSpilledVariable((Object)target);
                            this.L$4 = SpillingKt.nullOutSpilledVariable((Object)technique);
                            this.L$5 = SpillingKt.nullOutSpilledVariable((Object)currentRotation);
                            this.L$6 = SpillingKt.nullOutSpilledVariable((Object)currentCrosshairTarget);
                            this.L$7 = SpillingKt.nullOutSpilledVariable((Object)handToInteractWith);
                            this.L$8 = SpillingKt.nullOutSpilledVariable((Object)wasSuccessful);
                            this.L$9 = SpillingKt.nullOutSpilledVariable((Object)previousFallOffPos);
                            this.L$10 = SpillingKt.nullOutSpilledVariable((Object)suitableHand);
                            this.I$0 = $i$a$-tickHandler$default-ModuleScaffold$tickHandler$1;
                            this.I$1 = currentDelay;
                            this.Z$0 = hasBlockInMainHand;
                            this.Z$1 = hasBlockInOffHand;
                            this.label = 1;
                            v4 = CoroutineTickerKt.waitTicks((int)currentDelay, (Continuation)this);
                            if (v4 == var3_3) {
                                return var3_3;
                            }
                        }
                    }
                }
                ** GOTO lbl137
            }
            case 1: {
                hasBlockInOffHand = this.Z$1;
                hasBlockInMainHand = this.Z$0;
                currentDelay = this.I$1;
                $i$a$-tickHandler$default-ModuleScaffold$tickHandler$1 = this.I$0;
                suitableHand = (InteractionHand)this.L$10;
                previousFallOffPos = (Vec3)this.L$9;
                wasSuccessful = (Ref.BooleanRef)this.L$8;
                handToInteractWith = (InteractionHand)this.L$7;
                currentCrosshairTarget = (BlockHitResult)this.L$6;
                currentRotation = (Rotation)this.L$5;
                technique = (ScaffoldTechnique)this.L$4;
                target = (BlockPlacementTarget)this.L$3;
                $this$tickHandler_u24lambda_u240 = (CoroutineScope)this.L$2;
                $completion = (Continuation)this.L$1;
                ResultKt.throwOnFailure((Object)$result);
                v4 = $result;
lbl137:
                // 2 sources

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

