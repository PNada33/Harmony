/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  kotlin.Metadata
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower;

import java.util.function.IntPredicate;
import kotlin.Metadata;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower.ScaffoldTowerKarhu;

@Metadata(mv={2, 3, 0}, k=3, xi=50)
static final class ScaffoldTowerKarhu.jumpHandler.1.1
implements IntPredicate {
    public static final ScaffoldTowerKarhu.jumpHandler.1.1 INSTANCE = new /* invalid duplicate definition of identical inner class */;

    ScaffoldTowerKarhu.jumpHandler.1.1() {
    }

    @Override
    public final boolean test(int it) {
        return !ScaffoldTowerKarhu.INSTANCE.getPlayer().onGround();
    }
}

