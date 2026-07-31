package xd.harm.modules.impl.render;

import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.SliderSetting;
import xd.harm.utils.render.CustomFramebuffer;


@ModuleRegister(name = "ViewModel", category = Category.Render, desc = "Позволяет изменять позицию предметов в руке")
public class ViewModel extends Module {

    private static final int RIGHT_X = 0;
    private static final int RIGHT_Y = 1;
    private static final int RIGHT_Z = 2;
    private static final int LEFT_X = 3;
    private static final int LEFT_Y = 4;
    private static final int LEFT_Z = 5;

    private static ViewModel instance;

    private float rightX;
    private float rightY;
    private float rightZ;
    private float leftX;
    private float leftY;
    private float leftZ;
    private boolean glassHandOnly;

    public final SliderSetting right_x = new CachedSliderSetting("Правый X", 0.0F, -2.0f, 2.0f, 0.1F, RIGHT_X);
    public final SliderSetting right_y = new CachedSliderSetting("Правый Y", 0.0F, -2.0f, 2.0f, 0.1F, RIGHT_Y);
    public final SliderSetting right_z = new CachedSliderSetting("Правый Z", 0.0F, -2.0f, 2.0f, 0.1F, RIGHT_Z);
    public final SliderSetting left_x = new CachedSliderSetting("Левый Х", 0.0F, -2.0f, 2.0f, 0.1F, LEFT_X);
    public final SliderSetting left_y = new CachedSliderSetting("Левый Y", 0.0F, -2.0f, 2.0f, 0.1F, LEFT_Y);
    public final SliderSetting left_z = new CachedSliderSetting("Левый Z", 0.0F, -2.0f, 2.0f, 0.1F, LEFT_Z);
    public final BooleanSetting glassHand = new CachedBooleanSetting("Только с киллаурой", false);
    public CustomFramebuffer hands;


    public ViewModel() {
        addSettings(right_x, right_y, right_z, left_x, left_y, left_z);
        refreshCache();
        instance = this;
    }

    public static ViewModel getInstance() {
        return instance;
    }

    public float getRightX() {
        return rightX;
    }

    public float getRightY() {
        return rightY;
    }

    public float getRightZ() {
        return rightZ;
    }

    public float getLeftX() {
        return leftX;
    }

    public float getLeftY() {
        return leftY;
    }

    public float getLeftZ() {
        return leftZ;
    }

    public float getX(boolean rightHand) {
        return rightHand ? rightX : leftX;
    }

    public float getY(boolean rightHand) {
        return rightHand ? rightY : leftY;
    }

    public float getZ(boolean rightHand) {
        return rightHand ? rightZ : leftZ;
    }

    public boolean isGlassHandOnly() {
        return glassHandOnly;
    }

    public void refreshCache() {
        rightX = right_x.getFloat();
        rightY = right_y.getFloat();
        rightZ = right_z.getFloat();
        leftX = left_x.getFloat();
        leftY = left_y.getFloat();
        leftZ = left_z.getFloat();
        glassHandOnly = glassHand.getBool();
    }

    private void cacheSliderValue(int axis, float value) {
        switch (axis) {
            case RIGHT_X:
                rightX = value;
                break;
            case RIGHT_Y:
                rightY = value;
                break;
            case RIGHT_Z:
                rightZ = value;
                break;
            case LEFT_X:
                leftX = value;
                break;
            case LEFT_Y:
                leftY = value;
                break;
            case LEFT_Z:
                leftZ = value;
                break;
            default:
                break;
        }
    }

    private final class CachedSliderSetting extends SliderSetting {
        private final int axis;

        private CachedSliderSetting(String name, float defaultVal, float min, float max, float increment, int axis) {
            super(name, defaultVal, min, max, increment);
            this.axis = axis;
            cacheSliderValue(axis, defaultVal);
        }

        @Override
        public void set(Float value) {
            super.set(value);
            cacheSliderValue(axis, value);
        }
    }

    private final class CachedBooleanSetting extends BooleanSetting {
        private CachedBooleanSetting(String name, Boolean defaultVal) {
            super(name, defaultVal);
            glassHandOnly = defaultVal;
        }

        @Override
        public void set(Boolean value) {
            super.set(value);
            glassHandOnly = value;
        }
    }

}
