/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  kotlin.Metadata
 *  kotlin.jvm.internal.Intrinsics
 *  kotlin.jvm.internal.SourceDebugExtension
 *  net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
 *  net.ccbluex.liquidbounce.event.EventHook
 *  net.ccbluex.liquidbounce.event.EventListener
 *  net.ccbluex.liquidbounce.event.EventListenerKt
 *  net.ccbluex.liquidbounce.event.events.MovementInputEvent
 *  net.ccbluex.liquidbounce.event.events.PlayerSafeWalkEvent
 *  net.ccbluex.liquidbounce.utils.block.BlockExtensionsKt
 *  net.minecraft.core.BlockPos
 *  org.jetbrains.annotations.NotNull
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.normal;

import java.util.function.Consumer;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.SourceDebugExtension;
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup;
import net.ccbluex.liquidbounce.event.EventHook;
import net.ccbluex.liquidbounce.event.EventListener;
import net.ccbluex.liquidbounce.event.EventListenerKt;
import net.ccbluex.liquidbounce.event.events.MovementInputEvent;
import net.ccbluex.liquidbounce.event.events.PlayerSafeWalkEvent;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.ScaffoldNormalTechnique;
import net.ccbluex.liquidbounce.utils.block.BlockExtensionsKt;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={2, 3, 0}, k=1, xi=50, d1={"\u0000(\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0005\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003J\r\u0010\u0011\u001a\u00020\u000eH\u0000\u00a2\u0006\u0002\b\u0012R\u0017\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\bR\u001d\u0010\t\u001a\b\u0012\u0004\u0012\u00020\n0\u0005\u00a2\u0006\u000e\n\u0000\u0012\u0004\b\u000b\u0010\u0003\u001a\u0004\b\f\u0010\bR\u0011\u0010\r\u001a\u00020\u000e8F\u00a2\u0006\u0006\u001a\u0004\b\u000f\u0010\u0010\u00a8\u0006\u0013"}, d2={"Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/techniques/normal/ScaffoldDownFeature;", "Lnet/ccbluex/liquidbounce/config/types/group/ToggleableValueGroup;", "<init>", "()V", "handleMoveInput", "Lnet/ccbluex/liquidbounce/event/EventHook;", "Lnet/ccbluex/liquidbounce/event/events/MovementInputEvent;", "getHandleMoveInput", "()Lnet/ccbluex/liquidbounce/event/EventHook;", "handleSafeWalk", "Lnet/ccbluex/liquidbounce/event/events/PlayerSafeWalkEvent;", "getHandleSafeWalk$annotations", "getHandleSafeWalk", "shouldGoDown", "", "getShouldGoDown", "()Z", "shouldFallOffBlock", "shouldFallOffBlock$liquidbounce", "liquidbounce"})
@SourceDebugExtension(value={"SMAP\nScaffoldDownFeature.kt\nKotlin\n*S Kotlin\n*F\n+ 1 ScaffoldDownFeature.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/techniques/normal/ScaffoldDownFeature\n+ 2 EventListener.kt\nnet/ccbluex/liquidbounce/event/EventListenerKt\n*L\n1#1,52:1\n99#2:53\n99#2:54\n*S KotlinDebug\n*F\n+ 1 ScaffoldDownFeature.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/techniques/normal/ScaffoldDownFeature\n*L\n31#1:53\n38#1:54\n*E\n"})
public final class ScaffoldDownFeature
extends ToggleableValueGroup {
    @NotNull
    public static final ScaffoldDownFeature INSTANCE;
    @NotNull
    private static final EventHook<MovementInputEvent> handleMoveInput;
    @NotNull
    private static final EventHook<PlayerSafeWalkEvent> handleSafeWalk;

    private ScaffoldDownFeature() {
        super((EventListener)ScaffoldNormalTechnique.INSTANCE, "Down", false, null, 8, null);
    }

    @NotNull
    public final EventHook<MovementInputEvent> getHandleMoveInput() {
        return handleMoveInput;
    }

    @NotNull
    public final EventHook<PlayerSafeWalkEvent> getHandleSafeWalk() {
        return handleSafeWalk;
    }

    public static /* synthetic */ void getHandleSafeWalk$annotations() {
    }

    public final boolean getShouldGoDown() {
        return this.getEnabled() && this.getMc().options.keyShift.isDown();
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public final boolean shouldFallOffBlock$liquidbounce() {
        if (!this.getShouldGoDown()) return false;
        BlockPos blockPos = this.getPlayer().blockPosition().offset(0, -2, 0);
        Intrinsics.checkNotNullExpressionValue((Object)blockPos, (String)"offset(...)");
        if (!BlockExtensionsKt.canStandOn((BlockPos)blockPos)) return false;
        return true;
    }

    private static final void handleMoveInput$lambda$0(MovementInputEvent it) {
        Intrinsics.checkNotNullParameter((Object)it, (String)"it");
        if (INSTANCE.shouldFallOffBlock$liquidbounce()) {
            it.setSneak(false);
        }
    }

    private static final void handleSafeWalk$lambda$0(PlayerSafeWalkEvent it) {
        Intrinsics.checkNotNullParameter((Object)it, (String)"it");
        if (INSTANCE.shouldFallOffBlock$liquidbounce()) {
            it.setSafeWalk(false);
        }
    }

    static {
        short priority$iv;
        EventListener $this$handler$iv;
        INSTANCE = new ScaffoldDownFeature();
        EventListener eventListener = (EventListener)INSTANCE;
        int n = -100;
        Consumer<MovementInputEvent> handler$iv = ScaffoldDownFeature::handleMoveInput$lambda$0;
        boolean $i$f$handler = false;
        handleMoveInput = EventListenerKt.handler((EventListener)$this$handler$iv, MovementInputEvent.class, (short)priority$iv, handler$iv);
        $this$handler$iv = (EventListener)INSTANCE;
        priority$iv = -100;
        handler$iv = ScaffoldDownFeature::handleSafeWalk$lambda$0;
        $i$f$handler = false;
        handleSafeWalk = EventListenerKt.handler((EventListener)$this$handler$iv, PlayerSafeWalkEvent.class, (short)priority$iv, handler$iv);
    }
}

