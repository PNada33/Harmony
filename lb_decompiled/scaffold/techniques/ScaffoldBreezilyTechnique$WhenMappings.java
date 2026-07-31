/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  kotlin.Metadata
 *  net.minecraft.core.Direction
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques;

import kotlin.Metadata;
import net.minecraft.core.Direction;

@Metadata(mv={2, 3, 0}, k=3, xi=50)
public static final class ScaffoldBreezilyTechnique.WhenMappings {
    public static final /* synthetic */ int[] $EnumSwitchMapping$0;

    static {
        int[] nArray = new int[Direction.values().length];
        try {
            nArray[Direction.SOUTH.ordinal()] = 1;
        }
        catch (NoSuchFieldError noSuchFieldError) {
            // empty catch block
        }
        try {
            nArray[Direction.NORTH.ordinal()] = 2;
        }
        catch (NoSuchFieldError noSuchFieldError) {
            // empty catch block
        }
        try {
            nArray[Direction.EAST.ordinal()] = 3;
        }
        catch (NoSuchFieldError noSuchFieldError) {
            // empty catch block
        }
        try {
            nArray[Direction.WEST.ordinal()] = 4;
        }
        catch (NoSuchFieldError noSuchFieldError) {
            // empty catch block
        }
        $EnumSwitchMapping$0 = nArray;
    }
}

