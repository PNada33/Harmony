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
 *  net.ccbluex.liquidbounce.utils.math.geometry.Line
 *  net.ccbluex.liquidbounce.utils.movement.DirectionalInput
 *  net.ccbluex.liquidbounce.utils.movement.MovementUtilsKt
 *  net.minecraft.world.phys.Vec3
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
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.ScaffoldNormalTechnique;
import net.ccbluex.liquidbounce.utils.math.geometry.Line;
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput;
import net.ccbluex.liquidbounce.utils.movement.MovementUtilsKt;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={2, 3, 0}, k=1, xi=50, d1={"\u0000 \n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0006\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0004\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0005X\u0082T\u00a2\u0006\u0002\n\u0000R\u001d\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\t0\b\u00a2\u0006\u000e\n\u0000\u0012\u0004\b\n\u0010\u0003\u001a\u0004\b\u000b\u0010\f\u00a8\u0006\r"}, d2={"Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/techniques/normal/ScaffoldStabilizeMovementFeature;", "Lnet/ccbluex/liquidbounce/config/types/group/ToggleableValueGroup;", "<init>", "()V", "MAX_CENTER_DEVIATION", "", "MAX_CENTER_DEVIATION_IF_MOVING_TOWARDS", "moveEvent", "Lnet/ccbluex/liquidbounce/event/EventHook;", "Lnet/ccbluex/liquidbounce/event/events/MovementInputEvent;", "getMoveEvent$annotations", "getMoveEvent", "()Lnet/ccbluex/liquidbounce/event/EventHook;", "liquidbounce"})
@SourceDebugExtension(value={"SMAP\nScaffoldStabilizeMovementFeature.kt\nKotlin\n*S Kotlin\n*F\n+ 1 ScaffoldStabilizeMovementFeature.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/techniques/normal/ScaffoldStabilizeMovementFeature\n+ 2 MinecraftVectorExtensions.kt\nnet/ccbluex/liquidbounce/utils/math/MinecraftVectorExtensionsKt\n+ 3 EventListener.kt\nnet/ccbluex/liquidbounce/event/EventListenerKt\n*L\n1#1,80:1\n149#2:81\n99#3:82\n*S KotlinDebug\n*F\n+ 1 ScaffoldStabilizeMovementFeature.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/techniques/normal/ScaffoldStabilizeMovementFeature\n*L\n50#1:81\n38#1:82\n*E\n"})
public final class ScaffoldStabilizeMovementFeature
extends ToggleableValueGroup {
    @NotNull
    public static final ScaffoldStabilizeMovementFeature INSTANCE;
    private static final double MAX_CENTER_DEVIATION = 0.2;
    private static final double MAX_CENTER_DEVIATION_IF_MOVING_TOWARDS = 0.075;
    @NotNull
    private static final EventHook<MovementInputEvent> moveEvent;

    private ScaffoldStabilizeMovementFeature() {
        super((EventListener)ScaffoldNormalTechnique.INSTANCE, "StabilizeMovement", true, null, 8, null);
    }

    @NotNull
    public final EventHook<MovementInputEvent> getMoveEvent() {
        return moveEvent;
    }

    public static /* synthetic */ void getMoveEvent$annotations() {
    }

    /*
     * WARNING - void declaration
     */
    private static final void moveEvent$lambda$0(MovementInputEvent event) {
        double maxDeviation;
        void $this$copy_u24default$iv;
        Intrinsics.checkNotNullParameter((Object)event, (String)"event");
        if (event.getJump() && INSTANCE.getPlayer().onGround()) {
            return;
        }
        Line line = ModuleScaffold.INSTANCE.getCurrentOptimalLine();
        if (line == null) {
            return;
        }
        Line optimalLine = line;
        DirectionalInput currentInput = event.getDirectionalInput();
        Vec3 vec3 = INSTANCE.getPlayer().position();
        Intrinsics.checkNotNullExpressionValue((Object)vec3, (String)"position(...)");
        Vec3 nearestPointOnLine = optimalLine.getNearestPointTo(vec3);
        Vec3 vec32 = nearestPointOnLine.subtract(INSTANCE.getPlayer().position());
        Intrinsics.checkNotNullExpressionValue((Object)vec32, (String)"subtract(...)");
        Vec3 vecToLine = vec32;
        Vec3 vec33 = INSTANCE.getPlayer().getDeltaMovement();
        Intrinsics.checkNotNullExpressionValue((Object)vec33, (String)"getDeltaMovement(...)");
        Vec3 vec34 = vec33;
        double y$iv = 0.0;
        double x$iv = $this$copy_u24default$iv.x;
        double z$iv = $this$copy_u24default$iv.z;
        boolean $i$f$copy = false;
        Vec3 horizontalVelocity = new Vec3(x$iv, y$iv, z$iv);
        boolean isRunningTowardsLine = vecToLine.dot(horizontalVelocity) > 0.0;
        double d = maxDeviation = isRunningTowardsLine ? 0.075 : 0.2;
        if (nearestPointOnLine.distanceToSqr(INSTANCE.getPlayer().position()) < maxDeviation * maxDeviation) {
            return;
        }
        Vec3 vec35 = nearestPointOnLine.subtract(INSTANCE.getPlayer().position());
        Intrinsics.checkNotNullExpressionValue((Object)vec35, (String)"subtract(...)");
        float dgs = MovementUtilsKt.getDegreesRelativeToView((Vec3)vec35, (float)INSTANCE.getPlayer().getYRot());
        DirectionalInput newDirectionalInput = MovementUtilsKt.getDirectionalInputForDegrees((DirectionalInput)DirectionalInput.NONE, (float)dgs, (float)0.0f);
        boolean frontalAxisBlocked = currentInput.getForwards() || currentInput.getBackwards();
        boolean sagittalAxisBlocked = currentInput.getRight() || currentInput.getLeft();
        event.setDirectionalInput(new DirectionalInput(frontalAxisBlocked ? currentInput.getForwards() : newDirectionalInput.getForwards(), frontalAxisBlocked ? currentInput.getBackwards() : newDirectionalInput.getBackwards(), sagittalAxisBlocked ? currentInput.getLeft() : newDirectionalInput.getLeft(), sagittalAxisBlocked ? currentInput.getRight() : newDirectionalInput.getRight()));
    }

    /*
     * WARNING - void declaration
     */
    static {
        void priority$iv;
        void $this$handler$iv;
        INSTANCE = new ScaffoldStabilizeMovementFeature();
        EventListener eventListener = (EventListener)INSTANCE;
        int n = -10;
        Consumer<MovementInputEvent> handler$iv = ScaffoldStabilizeMovementFeature::moveEvent$lambda$0;
        boolean $i$f$handler = false;
        moveEvent = EventListenerKt.handler((EventListener)$this$handler$iv, MovementInputEvent.class, (short)priority$iv, handler$iv);
    }
}

