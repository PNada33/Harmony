/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  kotlin.Metadata
 *  kotlin.jvm.internal.PropertyReference1
 *  kotlin.jvm.internal.PropertyReference1Impl
 *  kotlin.jvm.internal.Reflection
 *  kotlin.ranges.IntRange
 *  kotlin.reflect.KProperty
 *  net.ccbluex.liquidbounce.config.types.RangedValue
 *  net.ccbluex.liquidbounce.config.types.Value
 *  net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
 *  net.ccbluex.liquidbounce.config.types.group.ValueGroup
 *  net.ccbluex.liquidbounce.event.EventListener
 *  org.jetbrains.annotations.NotNull
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features;

import kotlin.Metadata;
import kotlin.jvm.internal.PropertyReference1;
import kotlin.jvm.internal.PropertyReference1Impl;
import kotlin.jvm.internal.Reflection;
import kotlin.ranges.IntRange;
import kotlin.reflect.KProperty;
import net.ccbluex.liquidbounce.config.types.RangedValue;
import net.ccbluex.liquidbounce.config.types.Value;
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup;
import net.ccbluex.liquidbounce.config.types.group.ValueGroup;
import net.ccbluex.liquidbounce.event.EventListener;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={2, 3, 0}, k=1, xi=50, d1={"\u0000\u001c\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0005\n\u0002\u0010\b\n\u0002\b\b\b\u00c6\u0002\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003R\u001b\u0010\u0004\u001a\u00020\u00058FX\u0086\u0084\u0002\u00a2\u0006\f\n\u0004\b\b\u0010\t\u001a\u0004\b\u0006\u0010\u0007R\u001b\u0010\n\u001a\u00020\u000b8FX\u0086\u0084\u0002\u00a2\u0006\f\n\u0004\b\u000e\u0010\u000f\u001a\u0004\b\f\u0010\rR\u001b\u0010\u0010\u001a\u00020\u000b8FX\u0086\u0084\u0002\u00a2\u0006\f\n\u0004\b\u0012\u0010\u000f\u001a\u0004\b\u0011\u0010\r\u00a8\u0006\u0013"}, d2={"Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/ScaffoldAutoBlockFeature;", "Lnet/ccbluex/liquidbounce/config/types/group/ToggleableValueGroup;", "<init>", "()V", "alwaysHoldBlock", "", "getAlwaysHoldBlock", "()Z", "alwaysHoldBlock$delegate", "Lnet/ccbluex/liquidbounce/config/types/Value;", "slotResetDelay", "", "getSlotResetDelay", "()I", "slotResetDelay$delegate", "Lnet/ccbluex/liquidbounce/config/types/RangedValue;", "doNotUseBelowCount", "getDoNotUseBelowCount", "doNotUseBelowCount$delegate", "liquidbounce"})
public final class ScaffoldAutoBlockFeature
extends ToggleableValueGroup {
    @NotNull
    public static final ScaffoldAutoBlockFeature INSTANCE;
    static final /* synthetic */ KProperty<Object>[] $$delegatedProperties;
    @NotNull
    private static final Value alwaysHoldBlock$delegate;
    @NotNull
    private static final RangedValue slotResetDelay$delegate;
    @NotNull
    private static final RangedValue doNotUseBelowCount$delegate;

    private ScaffoldAutoBlockFeature() {
        super((EventListener)ModuleScaffold.INSTANCE, "AutoBlock", true, null, 8, null);
    }

    public final boolean getAlwaysHoldBlock() {
        return (Boolean)alwaysHoldBlock$delegate.getValue((Object)this, $$delegatedProperties[0]);
    }

    public final int getSlotResetDelay() {
        return ((Number)slotResetDelay$delegate.getValue((Object)this, $$delegatedProperties[1])).intValue();
    }

    public final int getDoNotUseBelowCount() {
        return ((Number)doNotUseBelowCount$delegate.getValue((Object)this, $$delegatedProperties[2])).intValue();
    }

    static {
        KProperty[] kPropertyArray = new KProperty[]{Reflection.property1((PropertyReference1)((PropertyReference1)new PropertyReference1Impl(ScaffoldAutoBlockFeature.class, "alwaysHoldBlock", "getAlwaysHoldBlock()Z", 0))), Reflection.property1((PropertyReference1)((PropertyReference1)new PropertyReference1Impl(ScaffoldAutoBlockFeature.class, "slotResetDelay", "getSlotResetDelay()I", 0))), Reflection.property1((PropertyReference1)((PropertyReference1)new PropertyReference1Impl(ScaffoldAutoBlockFeature.class, "doNotUseBelowCount", "getDoNotUseBelowCount()I", 0)))};
        $$delegatedProperties = kPropertyArray;
        INSTANCE = new ScaffoldAutoBlockFeature();
        alwaysHoldBlock$delegate = ValueGroup.boolean$default((ValueGroup)((ValueGroup)INSTANCE), (String)"Always", (boolean)false, null, (int)4, null);
        slotResetDelay$delegate = ValueGroup.int$default((ValueGroup)((ValueGroup)INSTANCE), (String)"SlotResetDelay", (int)5, (IntRange)new IntRange(0, 40), (String)"ticks", null, (int)16, null);
        doNotUseBelowCount$delegate = ValueGroup.int$default((ValueGroup)((ValueGroup)INSTANCE), (String)"DoNotUseBelowCount", (int)1, (IntRange)new IntRange(0, 64), null, null, (int)24, null);
    }
}

