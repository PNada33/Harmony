/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  kotlin.Metadata
 *  net.ccbluex.liquidbounce.utils.block.targetfinding.AimMode
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques;

import kotlin.Metadata;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.normal.ScaffoldTellyFeature;
import net.ccbluex.liquidbounce.utils.block.targetfinding.AimMode;

@Metadata(mv={2, 3, 0}, k=3, xi=50)
public static final class ScaffoldNormalTechnique.WhenMappings {
    public static final /* synthetic */ int[] $EnumSwitchMapping$0;
    public static final /* synthetic */ int[] $EnumSwitchMapping$1;

    static {
        int[] nArray = new int[ScaffoldTellyFeature.Mode.values().length];
        try {
            nArray[ScaffoldTellyFeature.Mode.REVERSE.ordinal()] = 1;
        }
        catch (NoSuchFieldError noSuchFieldError) {
            // empty catch block
        }
        try {
            nArray[ScaffoldTellyFeature.Mode.RESET.ordinal()] = 2;
        }
        catch (NoSuchFieldError noSuchFieldError) {
            // empty catch block
        }
        $EnumSwitchMapping$0 = nArray;
        nArray = new int[AimMode.values().length];
        try {
            nArray[AimMode.CENTER.ordinal()] = 1;
        }
        catch (NoSuchFieldError noSuchFieldError) {
            // empty catch block
        }
        try {
            nArray[AimMode.RANDOM.ordinal()] = 2;
        }
        catch (NoSuchFieldError noSuchFieldError) {
            // empty catch block
        }
        try {
            nArray[AimMode.STABILIZED.ordinal()] = 3;
        }
        catch (NoSuchFieldError noSuchFieldError) {
            // empty catch block
        }
        try {
            nArray[AimMode.NEAREST_ROTATION.ordinal()] = 4;
        }
        catch (NoSuchFieldError noSuchFieldError) {
            // empty catch block
        }
        try {
            nArray[AimMode.REVERSE_YAW.ordinal()] = 5;
        }
        catch (NoSuchFieldError noSuchFieldError) {
            // empty catch block
        }
        try {
            nArray[AimMode.DIAGONAL_YAW.ordinal()] = 6;
        }
        catch (NoSuchFieldError noSuchFieldError) {
            // empty catch block
        }
        try {
            nArray[AimMode.ANGLE_YAW.ordinal()] = 7;
        }
        catch (NoSuchFieldError noSuchFieldError) {
            // empty catch block
        }
        try {
            nArray[AimMode.EDGE_POINT.ordinal()] = 8;
        }
        catch (NoSuchFieldError noSuchFieldError) {
            // empty catch block
        }
        $EnumSwitchMapping$1 = nArray;
    }
}

