package xd.harm.modules.impl.render;

import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.Setting;
import xd.harm.modules.settings.impl.ColorSetting;
import xd.harm.modules.settings.impl.ModeSetting;
import xd.harm.modules.settings.impl.SliderSetting;
import xd.harm.utils.render.color.ColorUtils;

@ModuleRegister(name = "Theme", category = Category.Render, desc = "Темки")
public class Theme extends Module {

    public static final ModeSetting THEME = new ModeSetting("Выбор Цвета",
            "Лиловое сияние", "Лиловое сияние", "Синий с белым", "Милое ^_^",
            "Пламя", "Аквамарин", "Токсичное", "Синие Комбо", "Малиновое",
            "Кровавое", "Аметист", "Апельсиновое",
            "Aubergine", "Aqua", "Banana", "Blend", "Blossom", "Bubblegum",
            "Candy Cane", "Cherry", "Christmas", "Coral", "Digital Horizon",
            "Express", "Lime Water", "Lush", "Halogen", "Hyper", "Magic",
            "May", "Orange Juice", "Pastel", "Pumpkin", "Satin", "Snowy Sky",
            "Steel Fade", "Sundae", "Sunkist", "Water", "Winter", "Wood",
            "Свой"
    );

    public static final SliderSetting speedColors = new SliderSetting("Скорость цвета", 10.0F, 0.0F, 20.0F, 1.0F);
    public static final ColorSetting textcolor = (new ColorSetting("Цвет текста", ColorUtils.rgb(255, 255, 255))).setVisible(() -> THEME.is("Свой"));
    public static final ColorSetting visualscolor = (new ColorSetting("Цвет визуалов", ColorUtils.rgb(255, 255, 255))).setVisible(() -> THEME.is("Свой"));
    public static final ColorSetting rectcolor = (new ColorSetting("Худа и гуи", ColorUtils.rgb(255, 255, 255))).setVisible(() -> THEME.is("Свой"));

    public Theme() {
        this.toggle();
        this.addSettings(new Setting[]{THEME, textcolor, visualscolor, rectcolor});
    }

    @Override
    public boolean isVisibleInClickGui() {
        return false;
    }

    @Override
    public boolean isVisibleInKeyBindHud() {
        return false;
    }

    @Override
    public boolean isVisibleInArrayList() {
        return false;
    }

    @Override
    public void toggle() {
        if (!isState()) {
            super.toggle();
        }
    }

    public static int MainColor(int index) {
        int[] colors = getTemkaColors(index);
        return colors[0];
    }

    public static int RectColor(int index) {
        int[] colors = getTemkaColors(index);
        return colors[2];
    }

    public static int Text(int index) {
        int[] colors = getTemkaColors(index);
        return colors[1];
    }
    
    public static int getColor(int index, float mult) {
        int[] colors = getTemkaColors(index);
        return ColorUtils.gradient(colors[0], colors[2], (int) (index * mult), 10);
    }

    public static int getMainMenu(int index, float mult) {
        return ColorUtils.gradient(ColorUtils.rgb(190, 190, 190), ColorUtils.rgb(50, 0, 100), (int) (index * mult), 30);
    }

    public static int gradientcolor1(int index, float mult) {
        int[] colors = getTemkaColors(index);
        return ColorUtils.gradient(colors[0], colors[0], (int) (index * mult), 8);
    }

    public static int gradientcolor2(int index, float mult) {
        int[] colors = getTemkaColors(index);
        return ColorUtils.gradient(colors[1], colors[1], (int) (index * mult), 8);
    }

    private static float getRiseBlend() {
        return (float) (Math.sin(System.currentTimeMillis() / (speedColors.get() * 50.0)) * 0.5 + 0.5);
    }

    private static int[] getTemkaColors(int index) {
        int theme = THEME.getIndex();
        int text = 1;
        int rect = 1;
        int visik = 1;

        if (THEME.is("Лиловое сияние")) {
            visik = ColorUtils.rgb(89, 76, 211);
            rect = ColorUtils.rgb(36, 20, 76);
            text = ColorUtils.rgb(180, 170, 255);
        }
        if (THEME.is("Синий с белым")) {
            visik = ColorUtils.rgb(152, 245, 249);
            rect = ColorUtils.rgb(226, 234, 244);
            text = ColorUtils.rgb(100, 100, 100);
        }
        if (THEME.is("Милое ^_^")) {
            visik = ColorUtils.rgb(230, 137, 189);
            rect = ColorUtils.rgb(234, 225, 225);
            text = ColorUtils.rgb(255, 200, 230);
        }
        if (THEME.is("Пламя")) {
            visik = ColorUtils.rgb(251, 210, 71);
            rect = ColorUtils.rgb(227, 142, 63);
            text = ColorUtils.rgb(255, 255, 200);
        }
        if (THEME.is("Аквамарин")) {
            visik = ColorUtils.rgb(39, 207, 164);
            rect = ColorUtils.rgb(105, 255, 255);
            text = ColorUtils.rgb(200, 255, 240);
        }
        if (THEME.is("Токсичное")) {
            visik = ColorUtils.rgb(103, 228, 74);
            rect = ColorUtils.rgb(88, 165, 63);
            text = ColorUtils.rgb(200, 255, 180);
        }
        if (THEME.is("Синие Комбо")) {
            visik = ColorUtils.rgb(0, 88, 255);
            rect = ColorUtils.rgb(123, 191, 236);
            text = ColorUtils.rgb(200, 230, 255);
        }
        if (THEME.is("Малиновое")) {
            visik = ColorUtils.rgb(221, 127, 161);
            rect = ColorUtils.rgb(150, 70, 100);
            text = ColorUtils.rgb(255, 200, 220);
        }
        if (THEME.is("Кровавое")) {
            visik = ColorUtils.rgb(225, 138, 134);
            rect = ColorUtils.rgb(140, 60, 60);
            text = ColorUtils.rgb(255, 210, 210);
        }
        if (THEME.is("Аметист")) {
            visik = ColorUtils.rgb(170, 137, 193);
            rect = ColorUtils.rgb(100, 70, 130);
            text = ColorUtils.rgb(230, 210, 255);
        }
        if (THEME.is("Апельсиновое")) {
            visik = ColorUtils.rgb(221, 177, 127);
            rect = ColorUtils.rgb(160, 110, 60);
            text = ColorUtils.rgb(255, 235, 200);
        }

        if (THEME.is("Aubergine")) {
            float blend = getRiseBlend();
            int first = ColorUtils.rgb(170, 7, 107);
            int second = ColorUtils.rgb(97, 4, 95);
            visik = ColorUtils.interpolateColor(first, second, blend);
            text = ColorUtils.interpolateColor(second, first, blend);
            rect = visik;
        }
        if (THEME.is("Aqua")) {
            float blend = getRiseBlend();
            int first = ColorUtils.rgb(185, 250, 255);
            int second = ColorUtils.rgb(79, 199, 200);
            visik = ColorUtils.interpolateColor(first, second, blend);
            text = ColorUtils.interpolateColor(second, first, blend);
            rect = visik;
        }
        if (THEME.is("Banana")) {
            float blend = getRiseBlend();
            int first = ColorUtils.rgb(253, 236, 177);
            int second = ColorUtils.rgb(255, 255, 255);
            visik = ColorUtils.interpolateColor(first, second, blend);
            text = ColorUtils.interpolateColor(second, first, blend);
            rect = visik;
        }
        if (THEME.is("Blend")) {
            float blend = getRiseBlend();
            int first = ColorUtils.rgb(71, 148, 253);
            int second = ColorUtils.rgb(71, 253, 160);
            visik = ColorUtils.interpolateColor(first, second, blend);
            text = ColorUtils.interpolateColor(second, first, blend);
            rect = visik;
        }
        if (THEME.is("Blossom")) {
            float blend = getRiseBlend();
            int first = ColorUtils.rgb(226, 208, 249);
            int second = ColorUtils.rgb(49, 119, 115);
            visik = ColorUtils.interpolateColor(first, second, blend);
            text = ColorUtils.interpolateColor(second, first, blend);
            rect = visik;
        }
        if (THEME.is("Bubblegum")) {
            float blend = getRiseBlend();
            int first = ColorUtils.rgb(243, 145, 216);
            int second = ColorUtils.rgb(152, 165, 243);
            visik = ColorUtils.interpolateColor(first, second, blend);
            text = ColorUtils.interpolateColor(second, first, blend);
            rect = visik;
        }
        if (THEME.is("Candy Cane")) {
            float blend = getRiseBlend();
            int first = ColorUtils.rgb(255, 255, 255);
            int second = ColorUtils.rgb(255, 0, 0);
            visik = ColorUtils.interpolateColor(first, second, blend);
            text = ColorUtils.interpolateColor(second, first, blend);
            rect = visik;
        }
        if (THEME.is("Cherry")) {
            float blend = getRiseBlend();
            int first = ColorUtils.rgb(187, 55, 125);
            int second = ColorUtils.rgb(251, 211, 233);
            visik = ColorUtils.interpolateColor(first, second, blend);
            text = ColorUtils.interpolateColor(second, first, blend);
            rect = visik;
        }
        if (THEME.is("Christmas")) {
            float blend = getRiseBlend();
            int first = ColorUtils.rgb(255, 64, 64);
            int second = ColorUtils.rgb(255, 255, 255);
            int third = ColorUtils.rgb(64, 255, 64);
            visik = ColorUtils.interpolateColor(first, second, blend);
            text = ColorUtils.interpolateColor(second, third, blend);
            rect = ColorUtils.interpolateColor(third, first, blend);
        }
        if (THEME.is("Coral")) {
            float blend = getRiseBlend();
            int first = ColorUtils.rgb(244, 168, 150);
            int second = ColorUtils.rgb(52, 133, 151);
            visik = ColorUtils.interpolateColor(first, second, blend);
            text = ColorUtils.interpolateColor(second, first, blend);
            rect = visik;
        }
        if (THEME.is("Digital Horizon")) {
            float blend = getRiseBlend();
            int first = ColorUtils.rgb(95, 195, 228);
            int second = ColorUtils.rgb(229, 93, 135);
            visik = ColorUtils.interpolateColor(first, second, blend);
            text = ColorUtils.interpolateColor(second, first, blend);
            rect = visik;
        }
        if (THEME.is("Express")) {
            float blend = getRiseBlend();
            int first = ColorUtils.rgb(173, 83, 137);
            int second = ColorUtils.rgb(60, 16, 83);
            visik = ColorUtils.interpolateColor(first, second, blend);
            text = ColorUtils.interpolateColor(second, first, blend);
            rect = visik;
        }
        if (THEME.is("Lime Water")) {
            float blend = getRiseBlend();
            int first = ColorUtils.rgb(18, 255, 247);
            int second = ColorUtils.rgb(179, 255, 171);
            visik = ColorUtils.interpolateColor(first, second, blend);
            text = ColorUtils.interpolateColor(second, first, blend);
            rect = visik;
        }
        if (THEME.is("Lush")) {
            float blend = getRiseBlend();
            int first = ColorUtils.rgb(168, 224, 99);
            int second = ColorUtils.rgb(86, 171, 47);
            visik = ColorUtils.interpolateColor(first, second, blend);
            text = ColorUtils.interpolateColor(second, first, blend);
            rect = visik;
        }
        if (THEME.is("Halogen")) {
            float blend = getRiseBlend();
            int first = ColorUtils.rgb(255, 65, 108);
            int second = ColorUtils.rgb(255, 75, 43);
            visik = ColorUtils.interpolateColor(first, second, blend);
            text = ColorUtils.interpolateColor(second, first, blend);
            rect = visik;
        }
        if (THEME.is("Hyper")) {
            float blend = getRiseBlend();
            int first = ColorUtils.rgb(236, 110, 173);
            int second = ColorUtils.rgb(52, 148, 230);
            visik = ColorUtils.interpolateColor(first, second, blend);
            text = ColorUtils.interpolateColor(second, first, blend);
            rect = visik;
        }
        if (THEME.is("Magic")) {
            float blend = getRiseBlend();
            int first = ColorUtils.rgb(74, 0, 224);
            int second = ColorUtils.rgb(142, 45, 226);
            visik = ColorUtils.interpolateColor(first, second, blend);
            text = ColorUtils.interpolateColor(second, first, blend);
            rect = visik;
        }
        if (THEME.is("May")) {
            float blend = getRiseBlend();
            int first = ColorUtils.rgb(253, 219, 245);
            int second = ColorUtils.rgb(238, 79, 238);
            visik = ColorUtils.interpolateColor(first, second, blend);
            text = ColorUtils.interpolateColor(second, first, blend);
            rect = visik;
        }
        if (THEME.is("Orange Juice")) {
            float blend = getRiseBlend();
            int first = ColorUtils.rgb(252, 74, 26);
            int second = ColorUtils.rgb(247, 183, 51);
            visik = ColorUtils.interpolateColor(first, second, blend);
            text = ColorUtils.interpolateColor(second, first, blend);
            rect = visik;
        }
        if (THEME.is("Pastel")) {
            float blend = getRiseBlend();
            int first = ColorUtils.rgb(243, 155, 178);
            int second = ColorUtils.rgb(207, 196, 243);
            visik = ColorUtils.interpolateColor(first, second, blend);
            text = ColorUtils.interpolateColor(second, first, blend);
            rect = visik;
        }
        if (THEME.is("Pumpkin")) {
            float blend = getRiseBlend();
            int first = ColorUtils.rgb(241, 166, 98);
            int second = ColorUtils.rgb(255, 216, 169);
            int third = ColorUtils.rgb(227, 139, 42);
            visik = ColorUtils.interpolateColor(first, second, blend);
            text = ColorUtils.interpolateColor(second, third, blend);
            rect = ColorUtils.interpolateColor(third, first, blend);
        }
        if (THEME.is("Satin")) {
            float blend = getRiseBlend();
            int first = ColorUtils.rgb(215, 60, 67);
            int second = ColorUtils.rgb(140, 23, 39);
            visik = ColorUtils.interpolateColor(first, second, blend);
            text = ColorUtils.interpolateColor(second, first, blend);
            rect = visik;
        }
        if (THEME.is("Snowy Sky")) {
            float blend = getRiseBlend();
            int first = ColorUtils.rgb(1, 171, 179);
            int second = ColorUtils.rgb(234, 234, 234);
            int third = ColorUtils.rgb(18, 232, 232);
            visik = ColorUtils.interpolateColor(first, second, blend);
            text = ColorUtils.interpolateColor(second, third, blend);
            rect = ColorUtils.interpolateColor(third, first, blend);
        }
        if (THEME.is("Steel Fade")) {
            float blend = getRiseBlend();
            int first = ColorUtils.rgb(66, 134, 244);
            int second = ColorUtils.rgb(55, 59, 68);
            visik = ColorUtils.interpolateColor(first, second, blend);
            text = ColorUtils.interpolateColor(second, first, blend);
            rect = visik;
        }
        if (THEME.is("Sundae")) {
            float blend = getRiseBlend();
            int first = ColorUtils.rgb(206, 74, 126);
            int second = ColorUtils.rgb(122, 44, 77);
            visik = ColorUtils.interpolateColor(first, second, blend);
            text = ColorUtils.interpolateColor(second, first, blend);
            rect = visik;
        }
        if (THEME.is("Sunkist")) {
            float blend = getRiseBlend();
            int first = ColorUtils.rgb(242, 201, 76);
            int second = ColorUtils.rgb(242, 153, 74);
            visik = ColorUtils.interpolateColor(first, second, blend);
            text = ColorUtils.interpolateColor(second, first, blend);
            rect = visik;
        }
        if (THEME.is("Water")) {
            float blend = getRiseBlend();
            int first = ColorUtils.rgb(12, 232, 199);
            int second = ColorUtils.rgb(12, 163, 232);
            visik = ColorUtils.interpolateColor(first, second, blend);
            text = ColorUtils.interpolateColor(second, first, blend);
            rect = visik;
        }
        if (THEME.is("Winter")) {
            float blend = getRiseBlend();
            int first = ColorUtils.rgb(255, 255, 255);
            int second = ColorUtils.rgb(255, 255, 255);
            visik = ColorUtils.interpolateColor(first, second, blend);
            text = ColorUtils.interpolateColor(second, first, blend);
            rect = visik;
        }
        if (THEME.is("Wood")) {
            float blend = getRiseBlend();
            int first = ColorUtils.rgb(79, 109, 81);
            int second = ColorUtils.rgb(170, 139, 87);
            int third = ColorUtils.rgb(240, 235, 206);
            visik = ColorUtils.interpolateColor(first, second, blend);
            text = ColorUtils.interpolateColor(second, third, blend);
            rect = ColorUtils.interpolateColor(third, first, blend);
        }
        if (THEME.is("Свой")) {
            text = textcolor.get();
            rect = rectcolor.get();
            visik = visualscolor.get();
        }
        return new int[]{visik, text, rect};
    }
}
