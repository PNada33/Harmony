package xd.harm.modules.impl.render;

import com.google.common.eventbus.Subscribe;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.glfw.GLFW;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BindSetting;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.ModeSetting;

@ModuleRegister(name = "ClickGui", category = Category.Render, desc = "\u041c\u0435\u043d\u044e\u0448\u043a\u0430")
public class ClickGui extends Module {

    public static final BindSetting bind = new BindSetting("\u041a\u043d\u043e\u043f\u043a\u0430 \u043e\u0442\u043a\u0440\u044b\u0442\u0438\u044f", GLFW.GLFW_KEY_RIGHT_SHIFT);
    public static final ModeSetting guiStyle = new ModeSetting("\u0421\u0442\u0438\u043b\u044c GUI", "Windowed", new String[]{"Windowed", "MiniDropDown"});

    // Windowed-only настройки
    public static final String[] IMAGE_OPTIONS = {"1", "2", "3", "4", "5", "6"};
    public static final String[] HIGH_IMAGE_OPTIONS = {"1", "2"};
    public static final BooleanSetting showImage = new BooleanSetting("\u041a\u0430\u0440\u0442\u0438\u043d\u043a\u0430", false)
            .setVisible(() -> guiStyle.get().equals("Windowed"));
    public static final ModeSetting imageMode = new ModeSetting("\u0412\u044b\u0431\u043e\u0440 \u043a\u0430\u0440\u0442\u0438\u043d\u043a\u0438", IMAGE_OPTIONS[0], IMAGE_OPTIONS)
            .setVisible(() -> guiStyle.get().equals("Windowed") && showImage.get());
    public static final BooleanSetting showHighImage = new BooleanSetting("\u041a\u0430\u0440\u0442\u0438\u043d\u043a\u0430 \u0421\u0432\u0435\u0440\u0445\u0443", false)
            .setVisible(() -> guiStyle.get().equals("Windowed"));
    public static final ModeSetting highImageMode = new ModeSetting("\u0412\u044b\u0431\u043e\u0440 \u041a\u0430\u0440\u0442\u0438\u043d\u043a\u0438 \u0441\u0432\u0435\u0440\u0445\u0443", HIGH_IMAGE_OPTIONS[0], HIGH_IMAGE_OPTIONS)
            .setVisible(() -> guiStyle.get().equals("Windowed") && showHighImage.get());

    // MiniDropDown-only настройки
    public static final BooleanSetting mdBlur = new BooleanSetting("\u0420\u0430\u0437\u043c\u044b\u0442\u0438\u0435 \u0444\u043e\u043d\u0430", true)
            .setVisible(() -> guiStyle.get().equals("MiniDropDown"));
    public static final BooleanSetting mdAnimations = new BooleanSetting("\u0410\u043d\u0438\u043c\u0430\u0446\u0438\u0438", true)
            .setVisible(() -> guiStyle.get().equals("MiniDropDown"));
    public static final BooleanSetting mdBackground = new BooleanSetting("\u0413\u0440\u0430\u0434\u0438\u0435\u043d\u0442 \u0444\u043e\u043d\u0430", true)
            .setVisible(() -> guiStyle.get().equals("MiniDropDown"));

    private static final String IMAGE_RESOURCE_PATH = "harmony/images/png/";
    private static final String HIGH_IMAGE_RESOURCE_PATH = "harmony/images/highpng/";
    private static final String IMAGE_EXTENSION = ".png";

    private static String cachedImageMode;
    private static ResourceLocation cachedImageResource;
    private static String cachedHighImageMode;
    private static ResourceLocation cachedHighImageResource;
    private static String cachedNormalizedNameA;
    private static String cachedNormalizedValueA;
    private static String cachedNormalizedNameB;
    private static String cachedNormalizedValueB;

    public ClickGui() {
        addSettings(bind, guiStyle,
                showImage, imageMode, showHighImage, highImageMode,
                mdBlur, mdAnimations, mdBackground);
    }

    public static ResourceLocation getSelectedImageResource() {
        String selectedMode = imageMode.get();
        ResourceLocation selectedResource = cachedImageResource;
        if (selectedResource == null || !selectedMode.equals(cachedImageMode)) {
            cachedImageMode = selectedMode;
            selectedResource = new ResourceLocation(IMAGE_RESOURCE_PATH + normalizeImageName(selectedMode));
            cachedImageResource = selectedResource;
        }
        return selectedResource;
    }

    public static ResourceLocation getSelectedHighImageResource() {
        String selectedMode = highImageMode.get();
        ResourceLocation selectedResource = cachedHighImageResource;
        if (selectedResource == null || !selectedMode.equals(cachedHighImageMode)) {
            cachedHighImageMode = selectedMode;
            selectedResource = new ResourceLocation(HIGH_IMAGE_RESOURCE_PATH + normalizeImageName(selectedMode));
            cachedHighImageResource = selectedResource;
        }
        return cachedHighImageResource;
    }

    public static String normalizeImageName(String imageName) {
        if (imageName.equals(cachedNormalizedNameA)) {
            return cachedNormalizedValueA;
        }
        if (imageName.equals(cachedNormalizedNameB)) {
            return cachedNormalizedValueB;
        }
        if (imageName.endsWith(IMAGE_EXTENSION)) {
            return imageName;
        }

        String normalizedName = imageName + IMAGE_EXTENSION;
        cachedNormalizedNameB = cachedNormalizedNameA;
        cachedNormalizedValueB = cachedNormalizedValueA;
        cachedNormalizedNameA = imageName;
        cachedNormalizedValueA = normalizedName;
        return normalizedName;
    }

    @Override
    public boolean onDisable() {
        super.onDisable();
        return false;
    }

    @Override
    public boolean onEnable() {
        super.onEnable();
        return false;
    }

    @Subscribe
    public void onUpdate(EventUpdate e) {
        toggle();
    }
}
