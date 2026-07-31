/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  kotlin.Metadata
 *  kotlin.jvm.internal.Intrinsics
 *  kotlin.jvm.internal.PropertyReference1
 *  kotlin.jvm.internal.PropertyReference1Impl
 *  kotlin.jvm.internal.Reflection
 *  kotlin.jvm.internal.SourceDebugExtension
 *  kotlin.ranges.ClosedFloatingPointRange
 *  kotlin.ranges.IntRange
 *  kotlin.ranges.RangesKt
 *  kotlin.reflect.KProperty
 *  net.ccbluex.liquidbounce.config.types.RangedValue
 *  net.ccbluex.liquidbounce.config.types.Value
 *  net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
 *  net.ccbluex.liquidbounce.config.types.group.ValueGroup
 *  net.ccbluex.liquidbounce.config.utils.RefreshableIntState
 *  net.ccbluex.liquidbounce.config.utils.RefreshableRangeValueKt
 *  net.ccbluex.liquidbounce.event.EventHook
 *  net.ccbluex.liquidbounce.event.EventListener
 *  net.ccbluex.liquidbounce.event.EventListenerKt
 *  net.ccbluex.liquidbounce.event.events.MovementInputEvent
 *  net.ccbluex.liquidbounce.utils.entity.EntityExtensionsKt
 *  net.ccbluex.liquidbounce.utils.movement.DirectionalInput
 *  net.minecraft.client.player.LocalPlayer
 *  org.jetbrains.annotations.NotNull
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.normal;

import java.util.function.Consumer;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.PropertyReference1;
import kotlin.jvm.internal.PropertyReference1Impl;
import kotlin.jvm.internal.Reflection;
import kotlin.jvm.internal.SourceDebugExtension;
import kotlin.ranges.ClosedFloatingPointRange;
import kotlin.ranges.IntRange;
import kotlin.ranges.RangesKt;
import kotlin.reflect.KProperty;
import net.ccbluex.liquidbounce.config.types.RangedValue;
import net.ccbluex.liquidbounce.config.types.Value;
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup;
import net.ccbluex.liquidbounce.config.types.group.ValueGroup;
import net.ccbluex.liquidbounce.config.utils.RefreshableIntState;
import net.ccbluex.liquidbounce.config.utils.RefreshableRangeValueKt;
import net.ccbluex.liquidbounce.event.EventHook;
import net.ccbluex.liquidbounce.event.EventListener;
import net.ccbluex.liquidbounce.event.EventListenerKt;
import net.ccbluex.liquidbounce.event.events.MovementInputEvent;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.ScaffoldNormalTechnique;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.normal.ScaffoldDownFeature;
import net.ccbluex.liquidbounce.utils.entity.EntityExtensionsKt;
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={2, 3, 0}, k=1, xi=50, d1={"\u0000@\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0007\n\u0002\b\u0005\n\u0002\u0010\u000b\n\u0002\b\u0005\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003J\u000e\u0010\u0018\u001a\u00020\r2\u0006\u0010\u0019\u001a\u00020\u001aJ\u0006\u0010\u001b\u001a\u00020\u001cR\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u001b\u0010\u0006\u001a\u00020\u00078BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\n\u0010\u000b\u001a\u0004\b\b\u0010\tR\u001b\u0010\f\u001a\u00020\r8BX\u0082\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0010\u0010\u0011\u001a\u0004\b\u000e\u0010\u000fR\u000e\u0010\u0012\u001a\u00020\u0013X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u0014\u001a\b\u0012\u0004\u0012\u00020\u00160\u0015X\u0082\u0004\u00a2\u0006\b\n\u0000\u0012\u0004\b\u0017\u0010\u0003\u00a8\u0006\u001d"}, d2={"Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/techniques/normal/ScaffoldEagleFeature;", "Lnet/ccbluex/liquidbounce/config/types/group/ToggleableValueGroup;", "<init>", "()V", "blocksToEagle", "Lnet/ccbluex/liquidbounce/config/utils/RefreshableIntState;", "edgeDistance", "", "getEdgeDistance", "()F", "edgeDistance$delegate", "Lnet/ccbluex/liquidbounce/config/types/RangedValue;", "onlyOnGround", "", "getOnlyOnGround", "()Z", "onlyOnGround$delegate", "Lnet/ccbluex/liquidbounce/config/types/Value;", "placedBlocks", "", "stateUpdateHandler", "Lnet/ccbluex/liquidbounce/event/EventHook;", "Lnet/ccbluex/liquidbounce/event/events/MovementInputEvent;", "getStateUpdateHandler$annotations", "shouldEagle", "input", "Lnet/ccbluex/liquidbounce/utils/movement/DirectionalInput;", "onBlockPlacement", "", "liquidbounce"})
@SourceDebugExtension(value={"SMAP\nScaffoldEagleFeature.kt\nKotlin\n*S Kotlin\n*F\n+ 1 ScaffoldEagleFeature.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/techniques/normal/ScaffoldEagleFeature\n+ 2 EventListener.kt\nnet/ccbluex/liquidbounce/event/EventListenerKt\n*L\n1#1,75:1\n99#2:76\n*S KotlinDebug\n*F\n+ 1 ScaffoldEagleFeature.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/techniques/normal/ScaffoldEagleFeature\n*L\n-1#1:76\n*E\n"})
public final class ScaffoldEagleFeature
extends ToggleableValueGroup {
    @NotNull
    public static final ScaffoldEagleFeature INSTANCE;
    static final /* synthetic */ KProperty<Object>[] $$delegatedProperties;
    @NotNull
    private static final RefreshableIntState blocksToEagle;
    @NotNull
    private static final RangedValue edgeDistance$delegate;
    @NotNull
    private static final Value onlyOnGround$delegate;
    private static int placedBlocks;
    @NotNull
    private static final EventHook<MovementInputEvent> stateUpdateHandler;

    private ScaffoldEagleFeature() {
        super((EventListener)ScaffoldNormalTechnique.INSTANCE, "Eagle", false, null, 8, null);
    }

    private final float getEdgeDistance() {
        return ((Number)edgeDistance$delegate.getValue((Object)this, $$delegatedProperties[0])).floatValue();
    }

    private final boolean getOnlyOnGround() {
        return (Boolean)onlyOnGround$delegate.getValue((Object)this, $$delegatedProperties[1]);
    }

    private static /* synthetic */ void getStateUpdateHandler$annotations() {
    }

    public final boolean shouldEagle(@NotNull DirectionalInput input) {
        Intrinsics.checkNotNullParameter((Object)input, (String)"input");
        if (ScaffoldDownFeature.INSTANCE.shouldFallOffBlock$liquidbounce()) {
            return false;
        }
        if (!this.getPlayer().onGround() && this.getOnlyOnGround()) {
            return false;
        }
        boolean shouldBeActive = !this.getPlayer().getAbilities().flying && placedBlocks == 0;
        return shouldBeActive && EntityExtensionsKt.isCloseToEdge$default((LocalPlayer)this.getPlayer(), (DirectionalInput)input, (double)this.getEdgeDistance(), null, (int)4, null);
    }

    public final void onBlockPlacement() {
        if (!this.getEnabled()) {
            return;
        }
        int n = placedBlocks;
        if ((placedBlocks = n + 1) > blocksToEagle.getCurrent()) {
            placedBlocks = 0;
            blocksToEagle.refresh();
        }
    }

    private static final void stateUpdateHandler$lambda$0(MovementInputEvent it) {
        Intrinsics.checkNotNullParameter((Object)it, (String)"it");
        if (!it.getSneak() && INSTANCE.shouldEagle(it.getDirectionalInput())) {
            it.setSneak(true);
        }
    }

    /*
     * WARNING - void declaration
     */
    static {
        void priority$iv;
        void $this$handler$iv;
        EventListener eventListener = new EventListener[]{Reflection.property1((PropertyReference1)((PropertyReference1)new PropertyReference1Impl(ScaffoldEagleFeature.class, "edgeDistance", "getEdgeDistance()F", 0))), Reflection.property1((PropertyReference1)((PropertyReference1)new PropertyReference1Impl(ScaffoldEagleFeature.class, "onlyOnGround", "getOnlyOnGround()Z", 0)))};
        $$delegatedProperties = eventListener;
        INSTANCE = new ScaffoldEagleFeature();
        blocksToEagle = RefreshableRangeValueKt.asRefreshable((Value)((Value)ValueGroup.intRange$default((ValueGroup)((ValueGroup)INSTANCE), (String)"BlocksToEagle", (IntRange)new IntRange(0, 0), (IntRange)new IntRange(0, 10), null, null, (int)24, null)));
        edgeDistance$delegate = ValueGroup.float$default((ValueGroup)((ValueGroup)INSTANCE), (String)"EdgeDistance", (float)0.01f, (ClosedFloatingPointRange)RangesKt.rangeTo((float)0.01f, (float)1.3f), null, null, (int)24, null);
        onlyOnGround$delegate = ValueGroup.boolean$default((ValueGroup)((ValueGroup)INSTANCE), (String)"OnlyOnGround", (boolean)true, null, (int)4, null);
        eventListener = (EventListener)INSTANCE;
        int n = -50;
        Consumer<MovementInputEvent> handler$iv = ScaffoldEagleFeature::stateUpdateHandler$lambda$0;
        boolean $i$f$handler = false;
        stateUpdateHandler = EventListenerKt.handler((EventListener)$this$handler$iv, MovementInputEvent.class, (short)priority$iv, handler$iv);
    }
}

