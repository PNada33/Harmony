/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  kotlin.Metadata
 *  kotlin.jvm.JvmField
 *  kotlin.jvm.internal.DefaultConstructorMarker
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features;

import kotlin.Metadata;
import kotlin.jvm.JvmField;
import kotlin.jvm.internal.DefaultConstructorMarker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Metadata(mv={2, 3, 0}, k=1, xi=50, d1={"\u0000(\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\b\n\u0002\b\r\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0002\b\u0086\b\u0018\u0000 \u00172\u00020\u0001:\u0001\u0017B/\u0012\b\b\u0002\u0010\u0002\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0004\u001a\u00020\u0005\u0012\b\b\u0002\u0010\u0006\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0007\u001a\u00020\u0003\u00a2\u0006\u0004\b\b\u0010\tJ\t\u0010\f\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\r\u001a\u00020\u0005H\u00c6\u0003J\t\u0010\u000e\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u000f\u001a\u00020\u0003H\u00c6\u0003J1\u0010\u0010\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u00032\b\b\u0002\u0010\u0007\u001a\u00020\u0003H\u00c6\u0001J\u0014\u0010\u0011\u001a\u00020\u00032\b\u0010\u0012\u001a\u0004\u0018\u00010\u0013H\u00d6\u0083\u0004J\n\u0010\u0014\u001a\u00020\u0005H\u00d6\u0081\u0004J\n\u0010\u0015\u001a\u00020\u0016H\u00d6\u0081\u0004R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0002\u0010\nR\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0004\u0010\u000bR\u0011\u0010\u0006\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0006\u0010\nR\u0011\u0010\u0007\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\n\u00a8\u0006\u0018"}, d2={"Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/LedgeAction;", "Ljava/lang/Record;", "jump", "", "sneakTime", "", "stopInput", "stepBack", "<init>", "(ZIZZ)V", "()Z", "()I", "component1", "component2", "component3", "component4", "copy", "equals", "other", "", "hashCode", "toString", "", "Companion", "liquidbounce"})
public final class LedgeAction
extends Record {
    @NotNull
    public static final Companion Companion = new Companion(null);
    private final boolean jump;
    private final int sneakTime;
    private final boolean stopInput;
    private final boolean stepBack;
    @JvmField
    @NotNull
    public static final LedgeAction NO_LEDGE = new LedgeAction(false, 0, false, false);

    public LedgeAction(boolean jump, int sneakTime, boolean stopInput, boolean stepBack) {
        this.jump = jump;
        this.sneakTime = sneakTime;
        this.stopInput = stopInput;
        this.stepBack = stepBack;
    }

    public /* synthetic */ LedgeAction(boolean bl, int n, boolean bl2, boolean bl3, int n2, DefaultConstructorMarker defaultConstructorMarker) {
        if ((n2 & 1) != 0) {
            bl = false;
        }
        if ((n2 & 2) != 0) {
            n = 0;
        }
        if ((n2 & 4) != 0) {
            bl2 = false;
        }
        if ((n2 & 8) != 0) {
            bl3 = false;
        }
        this(bl, n, bl2, bl3);
    }

    public final boolean jump() {
        return this.jump;
    }

    public final int sneakTime() {
        return this.sneakTime;
    }

    public final boolean stopInput() {
        return this.stopInput;
    }

    public final boolean stepBack() {
        return this.stepBack;
    }

    public final boolean component1() {
        return this.jump;
    }

    public final int component2() {
        return this.sneakTime;
    }

    public final boolean component3() {
        return this.stopInput;
    }

    public final boolean component4() {
        return this.stepBack;
    }

    @NotNull
    public final LedgeAction copy(boolean jump, int sneakTime, boolean stopInput, boolean stepBack) {
        return new LedgeAction(jump, sneakTime, stopInput, stepBack);
    }

    public static /* synthetic */ LedgeAction copy$default(LedgeAction ledgeAction, boolean bl, int n, boolean bl2, boolean bl3, int n2, Object object) {
        if ((n2 & 1) != 0) {
            bl = ledgeAction.jump;
        }
        if ((n2 & 2) != 0) {
            n = ledgeAction.sneakTime;
        }
        if ((n2 & 4) != 0) {
            bl2 = ledgeAction.stopInput;
        }
        if ((n2 & 8) != 0) {
            bl3 = ledgeAction.stepBack;
        }
        return ledgeAction.copy(bl, n, bl2, bl3);
    }

    @Override
    @NotNull
    public String toString() {
        return "LedgeAction(jump=" + this.jump + ", sneakTime=" + this.sneakTime + ", stopInput=" + this.stopInput + ", stepBack=" + this.stepBack + ")";
    }

    @Override
    public int hashCode() {
        int result = Boolean.hashCode(this.jump);
        result = result * 31 + Integer.hashCode(this.sneakTime);
        result = result * 31 + Boolean.hashCode(this.stopInput);
        result = result * 31 + Boolean.hashCode(this.stepBack);
        return result;
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof LedgeAction)) {
            return false;
        }
        LedgeAction ledgeAction = (LedgeAction)other;
        if (this.jump != ledgeAction.jump) {
            return false;
        }
        if (this.sneakTime != ledgeAction.sneakTime) {
            return false;
        }
        if (this.stopInput != ledgeAction.stopInput) {
            return false;
        }
        return this.stepBack == ledgeAction.stepBack;
    }

    public LedgeAction() {
        this(false, 0, false, false, 15, null);
    }

    @Metadata(mv={2, 3, 0}, k=1, xi=50, d1={"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\t\b\u0002\u00a2\u0006\u0004\b\u0002\u0010\u0003R\u0010\u0010\u0004\u001a\u00020\u00058\u0006X\u0087\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0006"}, d2={"Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/LedgeAction$Companion;", "", "<init>", "()V", "NO_LEDGE", "Lnet/ccbluex/liquidbounce/features/module/modules/world/scaffold/features/LedgeAction;", "liquidbounce"})
    public static final class Companion {
        private Companion() {
        }

        public /* synthetic */ Companion(DefaultConstructorMarker $constructor_marker) {
            this();
        }
    }
}

