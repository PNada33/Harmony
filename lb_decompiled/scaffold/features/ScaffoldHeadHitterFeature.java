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
 *  net.ccbluex.liquidbounce.event.events.GameTickEvent
 *  net.ccbluex.liquidbounce.utils.block.BlockExtensionsKt
 *  net.ccbluex.liquidbounce.utils.entity.EntityExtensionsKt
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.core.BlockPos
 *  org.jetbrains.annotations.NotNull
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features;

import java.util.function.Consumer;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.SourceDebugExtension;
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup;
import net.ccbluex.liquidbounce.event.EventHook;
import net.ccbluex.liquidbounce.event.EventListener;
import net.ccbluex.liquidbounce.event.EventListenerKt;
import net.ccbluex.liquidbounce.event.events.GameTickEvent;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.ScaffoldNormalTechnique;
import net.ccbluex.liquidbounce.utils.block.BlockExtensionsKt;
import net.ccbluex.liquidbounce.utils.entity.EntityExtensionsKt;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={2, 3, 0}, k=1, xi=50, d1={"\u0000\u001e\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003J\b\u0010\u0004\u001a\u00020\u0005H\u0002R\u0017\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\b0\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\n\u00a8\u0006\u000b"}, d2={"Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/ScaffoldHeadHitterFeature;", "Lnet/ccbluex/liquidbounce/config/types/group/ToggleableValueGroup;", "<init>", "()V", "canHeadHit", "", "repeatable", "Lnet/ccbluex/liquidbounce/event/EventHook;", "Lnet/ccbluex/liquidbounce/event/events/GameTickEvent;", "getRepeatable", "()Lnet/ccbluex/liquidbounce/event/EventHook;", "liquidbounce"})
@SourceDebugExtension(value={"SMAP\nScaffoldHeadHitterFeature.kt\nKotlin\n*S Kotlin\n*F\n+ 1 ScaffoldHeadHitterFeature.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/ScaffoldHeadHitterFeature\n+ 2 EventListener.kt\nnet/ccbluex/liquidbounce/event/EventListenerKt\n*L\n1#1,38:1\n96#2,4:39\n*S KotlinDebug\n*F\n+ 1 ScaffoldHeadHitterFeature.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/ScaffoldHeadHitterFeature\n*L\n32#1:39,4\n*E\n"})
public final class ScaffoldHeadHitterFeature
extends ToggleableValueGroup {
    @NotNull
    public static final ScaffoldHeadHitterFeature INSTANCE;
    @NotNull
    private static final EventHook<GameTickEvent> repeatable;

    private ScaffoldHeadHitterFeature() {
        super((EventListener)ScaffoldNormalTechnique.INSTANCE, "HeadHitter", false, null, 8, null);
    }

    private final boolean canHeadHit() {
        BlockPos blockPos = this.getPlayer().blockPosition().above(2);
        Intrinsics.checkNotNullExpressionValue((Object)blockPos, (String)"above(...)");
        return !BlockExtensionsKt.getCollisionShape((BlockPos)blockPos).isEmpty() && this.getPlayer().onGround();
    }

    @NotNull
    public final EventHook<GameTickEvent> getRepeatable() {
        return repeatable;
    }

    private static final void repeatable$lambda$0(GameTickEvent it) {
        Intrinsics.checkNotNullParameter((Object)it, (String)"it");
        if (INSTANCE.canHeadHit() && EntityExtensionsKt.getMoving((LocalPlayer)INSTANCE.getPlayer())) {
            INSTANCE.getPlayer().jumpFromGround();
        }
    }

    /*
     * WARNING - void declaration
     */
    static {
        void $this$handler_u24default$iv;
        INSTANCE = new ScaffoldHeadHitterFeature();
        EventListener eventListener = (EventListener)INSTANCE;
        Consumer<GameTickEvent> handler$iv = ScaffoldHeadHitterFeature::repeatable$lambda$0;
        short priority$iv = 0;
        boolean $i$f$handler = false;
        repeatable = EventListenerKt.handler((EventListener)$this$handler_u24default$iv, GameTickEvent.class, (short)priority$iv, handler$iv);
    }
}

