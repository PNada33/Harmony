package xd.harm.modules.impl.render;

import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.SliderSetting;
import xd.harm.ui.clickgui.figura.FiguraCosmeticScreen;
import xd.harm.utils.figura.FiguraWear;

/**
 * Библиотека косметики FiguraLite.
 *
 * Вся косметика собрана здесь: аватары, питомцы и функции Катана,
 * Китайская шляпа, Плащ и Погладить. Отдельных модулей в ClickGUI
 * у них больше нет — только карточки в этом окне.
 */
@ModuleRegister(name = "FiguraCosmetic", category = Category.Render, desc = "Визуальная библиотека косметики Harmony")
public class FiguraCosmetic extends Module {

    public static volatile FiguraCosmetic INSTANCE;

    public final SliderSetting avatarScale = new SliderSetting("Масштаб аватара", 100f, 25f, 300f, 1f);
    public final SliderSetting avatarRotate = new SliderSetting("Поворот аватара", 0f, -180f, 180f, 5f);
    public final SliderSetting avatarHeight = new SliderSetting("Высота аватара", 0f, -100f, 100f, 1f);
    public final BooleanSetting autoFit = new BooleanSetting("Автомасштаб под рост игрока", true);
    public final BooleanSetting fixArms = new BooleanSetting("Исправлять T-позу", true);

    public FiguraCosmetic() {
        INSTANCE = this;
        addSettings(avatarScale, avatarRotate, avatarHeight, autoFit, fixArms);
        FiguraWear.bootstrap();
    }

    @Override
    public boolean onEnable() {
        FiguraWear.bootstrap();
        mc.displayGuiScreen(new FiguraCosmeticScreen(mc.currentScreen));
        return super.onEnable();
    }
}
