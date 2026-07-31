/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  kotlin.Metadata
 *  kotlin.enums.EnumEntries
 *  kotlin.enums.EnumEntriesKt
 *  kotlin.jvm.internal.Intrinsics
 *  kotlin.jvm.internal.PropertyReference1
 *  kotlin.jvm.internal.PropertyReference1Impl
 *  kotlin.jvm.internal.Reflection
 *  kotlin.jvm.internal.SourceDebugExtension
 *  kotlin.reflect.KProperty
 *  net.ccbluex.liquidbounce.config.types.Value
 *  net.ccbluex.liquidbounce.config.types.group.ValueGroup
 *  net.ccbluex.liquidbounce.config.types.list.ChoiceListValue
 *  net.ccbluex.liquidbounce.config.types.list.Tagged
 *  net.ccbluex.liquidbounce.event.EventListener
 *  net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup
 *  org.jetbrains.annotations.NotNull
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold;

import java.util.EnumSet;
import java.util.Set;
import kotlin.Metadata;
import kotlin.enums.EnumEntries;
import kotlin.enums.EnumEntriesKt;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.PropertyReference1;
import kotlin.jvm.internal.PropertyReference1Impl;
import kotlin.jvm.internal.Reflection;
import kotlin.jvm.internal.SourceDebugExtension;
import kotlin.reflect.KProperty;
import net.ccbluex.liquidbounce.config.types.Value;
import net.ccbluex.liquidbounce.config.types.group.ValueGroup;
import net.ccbluex.liquidbounce.config.types.list.ChoiceListValue;
import net.ccbluex.liquidbounce.config.types.list.Tagged;
import net.ccbluex.liquidbounce.event.EventListener;
import net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={2, 3, 0}, k=1, xi=50, d1={"\u0000\u001c\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000b\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0006\b\u00c0\u0002\u0018\u00002\u00020\u0001:\u0001\u0010B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003R\u001b\u0010\u0004\u001a\u00020\u00058FX\u0086\u0084\u0002\u00a2\u0006\f\n\u0004\b\b\u0010\t\u001a\u0004\b\u0006\u0010\u0007R\u001b\u0010\n\u001a\u00020\u000b8FX\u0086\u0084\u0002\u00a2\u0006\f\n\u0004\b\u000e\u0010\u000f\u001a\u0004\b\f\u0010\r\u00a8\u0006\u0011"}, d2={"Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/ModuleScaffold$ScaffoldRotationValueGroup;", "Lnet/ccbluex/liquidbounce/utils/aiming/RotationsValueGroup;", "<init>", "()V", "considerInventory", "", "getConsiderInventory", "()Z", "considerInventory$delegate", "Lnet/ccbluex/liquidbounce/config/types/Value;", "rotationTiming", "Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/ModuleScaffold$ScaffoldRotationValueGroup$RotationTimingMode;", "getRotationTiming", "()Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/ModuleScaffold$ScaffoldRotationValueGroup$RotationTimingMode;", "rotationTiming$delegate", "Lnet/ccbluex/liquidbounce/config/types/list/ChoiceListValue;", "RotationTimingMode", "liquidbounce"})
@SourceDebugExtension(value={"SMAP\nModuleScaffold.kt\nKotlin\n*S Kotlin\n*F\n+ 1 ModuleScaffold.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/ModuleScaffold$ScaffoldRotationValueGroup\n+ 2 ValueGroup.kt\nnet/ccbluex/liquidbounce/config/types/group/ValueGroup\n+ 3 enum-set.kt\nnet/ccbluex/fastutil/enum-set\n*L\n1#1,740:1\n558#2:741\n216#3:742\n*S KotlinDebug\n*F\n+ 1 ModuleScaffold.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/ModuleScaffold$ScaffoldRotationValueGroup\n*L\n-1#1:741\n-1#1:742\n*E\n"})
public static final class ModuleScaffold.ScaffoldRotationValueGroup
extends RotationsValueGroup {
    @NotNull
    public static final ModuleScaffold.ScaffoldRotationValueGroup INSTANCE;
    static final /* synthetic */ KProperty<Object>[] $$delegatedProperties;
    @NotNull
    private static final Value considerInventory$delegate;
    @NotNull
    private static final ChoiceListValue rotationTiming$delegate;

    private ModuleScaffold.ScaffoldRotationValueGroup() {
        super((EventListener)INSTANCE, null, false, 6, null);
    }

    public final boolean getConsiderInventory() {
        return (Boolean)considerInventory$delegate.getValue((Object)this, $$delegatedProperties[0]);
    }

    @NotNull
    public final RotationTimingMode getRotationTiming() {
        return (RotationTimingMode)((Object)rotationTiming$delegate.getValue((Object)this, $$delegatedProperties[1]));
    }

    /*
     * WARNING - void declaration
     */
    static {
        void name$iv;
        void this_$iv;
        ValueGroup valueGroup = new ValueGroup[]{Reflection.property1((PropertyReference1)((PropertyReference1)new PropertyReference1Impl(ModuleScaffold.ScaffoldRotationValueGroup.class, "considerInventory", "getConsiderInventory()Z", 0))), Reflection.property1((PropertyReference1)((PropertyReference1)new PropertyReference1Impl(ModuleScaffold.ScaffoldRotationValueGroup.class, "rotationTiming", "getRotationTiming()Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/ModuleScaffold$ScaffoldRotationValueGroup$RotationTimingMode;", 0)))};
        $$delegatedProperties = valueGroup;
        INSTANCE = new ModuleScaffold.ScaffoldRotationValueGroup();
        considerInventory$delegate = ValueGroup.boolean$default((ValueGroup)((ValueGroup)INSTANCE), (String)"ConsiderInventory", (boolean)false, null, (int)4, null);
        valueGroup = (ValueGroup)INSTANCE;
        String string = "RotationTiming";
        Enum default$iv = RotationTimingMode.NORMAL;
        boolean $i$f$enumChoice = false;
        Tagged tagged = (Tagged)default$iv;
        boolean $i$f$enumSetAllOf = false;
        EnumSet<RotationTimingMode> enumSet = EnumSet.allOf(RotationTimingMode.class);
        Intrinsics.checkNotNullExpressionValue(enumSet, (String)"allOf(...)");
        rotationTiming$delegate = this_$iv.enumChoice((String)name$iv, tagged, (Set)enumSet);
    }

    @Metadata(mv={2, 3, 0}, k=1, xi=50, d1={"\u0000\u0016\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0000\n\u0002\u0010\u000e\n\u0002\b\b\b\u0086\u0081\u0002\u0018\u00002\u00020\u00012\b\u0012\u0004\u0012\u00020\u00000\u0002B\u0011\b\u0002\u0012\u0006\u0010\u0003\u001a\u00020\u0004\u00a2\u0006\u0004\b\u0005\u0010\u0006R\u0014\u0010\u0003\u001a\u00020\u0004X\u0096\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\bj\u0002\b\tj\u0002\b\nj\u0002\b\u000b\u00a8\u0006\f"}, d2={"Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/ModuleScaffold$ScaffoldRotationValueGroup$RotationTimingMode;", "Lnet/ccbluex/liquidbounce/config/types/list/Tagged;", "", "tag", "", "<init>", "(Ljava/lang/String;ILjava/lang/String;)V", "getTag", "()Ljava/lang/String;", "NORMAL", "ON_TICK", "ON_TICK_SNAP", "liquidbounce"})
    public static final class RotationTimingMode
    extends Enum<RotationTimingMode>
    implements Tagged {
        @NotNull
        private final String tag;
        public static final /* enum */ RotationTimingMode NORMAL = new RotationTimingMode("Normal");
        public static final /* enum */ RotationTimingMode ON_TICK = new RotationTimingMode("OnTick");
        public static final /* enum */ RotationTimingMode ON_TICK_SNAP = new RotationTimingMode("OnTickSnap");
        private static final /* synthetic */ RotationTimingMode[] $VALUES;
        private static final /* synthetic */ EnumEntries $ENTRIES;

        private RotationTimingMode(String tag) {
            this.tag = tag;
        }

        @NotNull
        public String getTag() {
            return this.tag;
        }

        public static RotationTimingMode[] values() {
            return (RotationTimingMode[])$VALUES.clone();
        }

        public static RotationTimingMode valueOf(String value) {
            return Enum.valueOf(RotationTimingMode.class, value);
        }

        @NotNull
        public static EnumEntries<RotationTimingMode> getEntries() {
            return $ENTRIES;
        }

        static {
            $VALUES = rotationTimingModeArray = new RotationTimingMode[]{RotationTimingMode.NORMAL, RotationTimingMode.ON_TICK, RotationTimingMode.ON_TICK_SNAP};
            $ENTRIES = EnumEntriesKt.enumEntries((Enum[])$VALUES);
        }
    }
}

