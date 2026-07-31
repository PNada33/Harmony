/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  kotlin.Metadata
 *  kotlin.enums.EnumEntries
 *  kotlin.enums.EnumEntriesKt
 *  kotlin.jvm.functions.Function1
 *  kotlin.jvm.internal.Intrinsics
 *  kotlin.jvm.internal.SourceDebugExtension
 *  net.ccbluex.liquidbounce.config.types.list.Tagged
 *  net.minecraft.core.BlockPos
 *  org.jetbrains.annotations.NotNull
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold;

import kotlin.Metadata;
import kotlin.enums.EnumEntries;
import kotlin.enums.EnumEntriesKt;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.SourceDebugExtension;
import net.ccbluex.liquidbounce.config.types.list.Tagged;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;

@Metadata(mv={2, 3, 0}, k=1, xi=50, d1={"\u0000 \n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0010\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u000b\b\u0082\u0081\u0002\u0018\u00002\u00020\u00012\b\u0012\u0004\u0012\u00020\u00000\u0002B'\b\u0002\u0012\u0006\u0010\u0003\u001a\u00020\u0004\u0012\u0014\u0010\u0005\u001a\u0010\u0012\u0004\u0012\u00020\u0007\u0012\u0006\u0012\u0004\u0018\u00010\u00070\u0006\u00a2\u0006\u0004\b\b\u0010\tR\u0014\u0010\u0003\u001a\u00020\u0004X\u0096\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000bR\u001f\u0010\u0005\u001a\u0010\u0012\u0004\u0012\u00020\u0007\u0012\u0006\u0012\u0004\u0018\u00010\u00070\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\rj\u0002\b\u000ej\u0002\b\u000fj\u0002\b\u0010j\u0002\b\u0011\u00a8\u0006\u0012"}, d2={"Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/ModuleScaffold$SameYMode;", "Lnet/ccbluex/liquidbounce/config/types/list/Tagged;", "", "tag", "", "getTargetedBlockPos", "Lkotlin/Function1;", "Lnet/minecraft/core/BlockPos;", "<init>", "(Ljava/lang/String;ILjava/lang/String;Lkotlin/jvm/functions/Function1;)V", "getTag", "()Ljava/lang/String;", "getGetTargetedBlockPos", "()Lkotlin/jvm/functions/Function1;", "OFF", "ON", "FALLING", "HYPIXEL", "liquidbounce"})
@SourceDebugExtension(value={"SMAP\nModuleScaffold.kt\nKotlin\n*S Kotlin\n*F\n+ 1 ModuleScaffold.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/ModuleScaffold$SameYMode\n+ 2 MinecraftVectorExtensions.kt\nnet/ccbluex/liquidbounce/utils/math/MinecraftVectorExtensionsKt\n+ 3 fake.kt\nkotlin/jvm/internal/FakeKt\n*L\n1#1,740:1\n78#2:741\n78#2:742\n78#2:744\n78#2:745\n1#3:743\n*S KotlinDebug\n*F\n+ 1 ModuleScaffold.kt\nnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/ModuleScaffold$SameYMode\n*L\n155#1:741\n160#1:742\n170#1:744\n172#1:745\n*E\n"})
private static final class ModuleScaffold.SameYMode
extends Enum<ModuleScaffold.SameYMode>
implements Tagged {
    @NotNull
    private final String tag;
    @NotNull
    private final Function1<BlockPos, BlockPos> getTargetedBlockPos;
    public static final /* enum */ ModuleScaffold.SameYMode OFF = new ModuleScaffold.SameYMode("Off", (Function1<? super BlockPos, ? extends BlockPos>)((Function1)ModuleScaffold.SameYMode::_init_$lambda$0));
    public static final /* enum */ ModuleScaffold.SameYMode ON = new ModuleScaffold.SameYMode("On", (Function1<? super BlockPos, ? extends BlockPos>)((Function1)ModuleScaffold.SameYMode::_init_$lambda$1));
    public static final /* enum */ ModuleScaffold.SameYMode FALLING = new ModuleScaffold.SameYMode("Falling", (Function1<? super BlockPos, ? extends BlockPos>)((Function1)ModuleScaffold.SameYMode::_init_$lambda$2));
    public static final /* enum */ ModuleScaffold.SameYMode HYPIXEL = new ModuleScaffold.SameYMode("Hypixel", (Function1<? super BlockPos, ? extends BlockPos>)((Function1)ModuleScaffold.SameYMode::_init_$lambda$3));
    private static final /* synthetic */ ModuleScaffold.SameYMode[] $VALUES;
    private static final /* synthetic */ EnumEntries $ENTRIES;

    private ModuleScaffold.SameYMode(String tag, Function1<? super BlockPos, ? extends BlockPos> getTargetedBlockPos) {
        this.tag = tag;
        this.getTargetedBlockPos = getTargetedBlockPos;
    }

    @NotNull
    public String getTag() {
        return this.tag;
    }

    @NotNull
    public final Function1<BlockPos, BlockPos> getGetTargetedBlockPos() {
        return this.getTargetedBlockPos;
    }

    public static ModuleScaffold.SameYMode[] values() {
        return (ModuleScaffold.SameYMode[])$VALUES.clone();
    }

    public static ModuleScaffold.SameYMode valueOf(String value) {
        return Enum.valueOf(ModuleScaffold.SameYMode.class, value);
    }

    @NotNull
    public static EnumEntries<ModuleScaffold.SameYMode> getEntries() {
        return $ENTRIES;
    }

    private static final BlockPos _init_$lambda$0(BlockPos it) {
        Intrinsics.checkNotNullParameter((Object)it, (String)"it");
        return null;
    }

    /*
     * WARNING - void declaration
     */
    private static final BlockPos _init_$lambda$1(BlockPos blockPos) {
        void $this$copy_u24default$iv;
        Intrinsics.checkNotNullParameter((Object)blockPos, (String)"blockPos");
        BlockPos blockPos2 = blockPos;
        int y$iv = placementY;
        int x$iv = $this$copy_u24default$iv.getX();
        int z$iv = $this$copy_u24default$iv.getZ();
        boolean $i$f$copy = false;
        return new BlockPos(x$iv, y$iv, z$iv);
    }

    /*
     * WARNING - void declaration
     */
    private static final BlockPos _init_$lambda$2(BlockPos blockPos) {
        void $this$copy_u24default$iv;
        Intrinsics.checkNotNullParameter((Object)blockPos, (String)"blockPos");
        BlockPos blockPos2 = blockPos;
        int y$iv = placementY;
        int x$iv = $this$copy_u24default$iv.getX();
        int z$iv = $this$copy_u24default$iv.getZ();
        boolean $i$f$copy = false;
        BlockPos it = blockPos2 = new BlockPos(x$iv, y$iv, z$iv);
        boolean bl = false;
        return ModuleScaffold.INSTANCE.getPlayer().getDeltaMovement().y < 0.2 ? blockPos2 : null;
    }

    /*
     * WARNING - void declaration
     */
    private static final BlockPos _init_$lambda$3(BlockPos blockPos) {
        BlockPos blockPos2;
        Intrinsics.checkNotNullParameter((Object)blockPos, (String)"blockPos");
        if (ModuleScaffold.INSTANCE.getPlayer().getDeltaMovement().y == -0.15233518685055708 && jumps >= 2) {
            void $this$copy_u24default$iv;
            jumps = 0;
            BlockPos blockPos3 = blockPos;
            int y$iv = startY;
            int x$iv = $this$copy_u24default$iv.getX();
            int z$iv = $this$copy_u24default$iv.getZ();
            boolean $i$f$copy = false;
            blockPos2 = new BlockPos(x$iv, y$iv, z$iv);
        } else {
            BlockPos $this$copy_u24default$iv = blockPos;
            int y$iv = startY - 1;
            int x$iv = $this$copy_u24default$iv.getX();
            int z$iv = $this$copy_u24default$iv.getZ();
            boolean $i$f$copy = false;
            blockPos2 = new BlockPos(x$iv, y$iv, z$iv);
        }
        return blockPos2;
    }

    static {
        $VALUES = sameYModeArray = new ModuleScaffold.SameYMode[]{ModuleScaffold.SameYMode.OFF, ModuleScaffold.SameYMode.ON, ModuleScaffold.SameYMode.FALLING, ModuleScaffold.SameYMode.HYPIXEL};
        $ENTRIES = EnumEntriesKt.enumEntries((Enum[])$VALUES);
    }
}

