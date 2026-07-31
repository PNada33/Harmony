/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  kotlin.Metadata
 *  kotlin.jvm.internal.Intrinsics
 *  kotlin.jvm.internal.PropertyReference1
 *  kotlin.jvm.internal.PropertyReference1Impl
 *  kotlin.jvm.internal.Reflection
 *  kotlin.reflect.KProperty
 *  net.ccbluex.liquidbounce.config.types.Value
 *  net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
 *  net.ccbluex.liquidbounce.config.types.group.ValueGroup
 *  net.ccbluex.liquidbounce.event.EventListener
 *  net.ccbluex.liquidbounce.utils.clicking.Clicker
 *  net.minecraft.client.KeyMapping
 *  org.jetbrains.annotations.NotNull
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold;

import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.PropertyReference1;
import kotlin.jvm.internal.PropertyReference1Impl;
import kotlin.jvm.internal.Reflection;
import kotlin.reflect.KProperty;
import net.ccbluex.liquidbounce.config.types.Value;
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup;
import net.ccbluex.liquidbounce.config.types.group.ValueGroup;
import net.ccbluex.liquidbounce.event.EventListener;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold;
import net.ccbluex.liquidbounce.utils.clicking.Clicker;
import net.minecraft.client.KeyMapping;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={2, 3, 0}, k=1, xi=50, d1={"\u0000 \n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0005\b\u00c2\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003R\u0017\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\bR\u001b\u0010\t\u001a\u00020\n8FX\u0086\u0084\u0002\u00a2\u0006\f\n\u0004\b\r\u0010\u000e\u001a\u0004\b\u000b\u0010\f\u00a8\u0006\u000f"}, d2={"Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/ModuleScaffold$SimulatePlacementAttempts;", "Lnet/ccbluex/liquidbounce/config/types/group/ToggleableValueGroup;", "<init>", "()V", "clicker", "Lnet/ccbluex/liquidbounce/utils/clicking/Clicker;", "Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/ModuleScaffold;", "getClicker", "()Lnet/ccbluex/liquidbounce/utils/clicking/Clicker;", "failedAttemptsOnly", "", "getFailedAttemptsOnly", "()Z", "failedAttemptsOnly$delegate", "Lnet/ccbluex/liquidbounce/config/types/Value;", "liquidbounce"})
private static final class ModuleScaffold.SimulatePlacementAttempts
extends ToggleableValueGroup {
    @NotNull
    public static final ModuleScaffold.SimulatePlacementAttempts INSTANCE;
    static final /* synthetic */ KProperty<Object>[] $$delegatedProperties;
    @NotNull
    private static final Clicker<ModuleScaffold> clicker;
    @NotNull
    private static final Value failedAttemptsOnly$delegate;

    private ModuleScaffold.SimulatePlacementAttempts() {
        super((EventListener)INSTANCE, "SimulatePlacementAttempts", false, null, 8, null);
    }

    @NotNull
    public final Clicker<ModuleScaffold> getClicker() {
        return clicker;
    }

    public final boolean getFailedAttemptsOnly() {
        return (Boolean)failedAttemptsOnly$delegate.getValue((Object)this, $$delegatedProperties[0]);
    }

    static {
        KProperty[] kPropertyArray = new KProperty[]{Reflection.property1((PropertyReference1)((PropertyReference1)new PropertyReference1Impl(ModuleScaffold.SimulatePlacementAttempts.class, "failedAttemptsOnly", "getFailedAttemptsOnly()Z", 0)))};
        $$delegatedProperties = kPropertyArray;
        INSTANCE = new ModuleScaffold.SimulatePlacementAttempts();
        EventListener eventListener = (EventListener)INSTANCE;
        KeyMapping keyMapping = ModuleScaffold.SimulatePlacementAttempts.INSTANCE.getMc().options.keyUse;
        Intrinsics.checkNotNullExpressionValue((Object)keyMapping, (String)"keyUse");
        clicker = (Clicker)INSTANCE.tree((ValueGroup)new Clicker(eventListener, keyMapping, null, 100, null, false, 48, null));
        failedAttemptsOnly$delegate = ValueGroup.boolean$default((ValueGroup)((ValueGroup)INSTANCE), (String)"FailedAttemptsOnly", (boolean)true, null, (int)4, null);
    }
}

