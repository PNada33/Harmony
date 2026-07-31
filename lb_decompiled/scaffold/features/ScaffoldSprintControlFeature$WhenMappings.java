/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  kotlin.Metadata
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features;

import kotlin.Metadata;
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldSprintControlFeature;

@Metadata(mv={2, 3, 0}, k=3, xi=50)
public static final class ScaffoldSprintControlFeature.WhenMappings {
    public static final /* synthetic */ int[] $EnumSwitchMapping$0;

    static {
        int[] nArray = new int[ScaffoldSprintControlFeature.SprintMode.values().length];
        try {
            nArray[ScaffoldSprintControlFeature.SprintMode.FORCE_SPRINT.ordinal()] = 1;
        }
        catch (NoSuchFieldError noSuchFieldError) {
            // empty catch block
        }
        try {
            nArray[ScaffoldSprintControlFeature.SprintMode.FORCE_NO_SPRINT.ordinal()] = 2;
        }
        catch (NoSuchFieldError noSuchFieldError) {
            // empty catch block
        }
        try {
            nArray[ScaffoldSprintControlFeature.SprintMode.NO_SPRINT_ON_PLACE.ordinal()] = 3;
        }
        catch (NoSuchFieldError noSuchFieldError) {
            // empty catch block
        }
        try {
            nArray[ScaffoldSprintControlFeature.SprintMode.NO_SPRINT_ON_GROUND.ordinal()] = 4;
        }
        catch (NoSuchFieldError noSuchFieldError) {
            // empty catch block
        }
        try {
            nArray[ScaffoldSprintControlFeature.SprintMode.DO_NOT_CHANGE.ordinal()] = 5;
        }
        catch (NoSuchFieldError noSuchFieldError) {
            // empty catch block
        }
        $EnumSwitchMapping$0 = nArray;
    }
}

