package xd.harm.modules.impl.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.matrix.MatrixStack;
import xd.harm.Harmony;
import xd.harm.events.render.EventDisplay;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.ColorSetting;
import xd.harm.modules.settings.impl.ModeListSetting;
import xd.harm.modules.settings.impl.ModeSetting;
import xd.harm.modules.settings.impl.SliderSetting;
import xd.harm.utils.render.color.ColorUtils;
import xd.harm.utils.render.rect.RenderUtility;
import xd.harm.utils.text.font.ClientFonts;
import xd.harm.utils.text.font.styled.StyledFont;

import java.awt.Color;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@ModuleRegister(name = "HudElusive", category = Category.Render, desc = "Elusive WaterMark и ArrayList")
public class HudElusive extends Module {

    private Module previousHud;
    private boolean previousHudEnabled;

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a");

    public final ModeListSetting elements = new ModeListSetting("Элементы",
            new BooleanSetting("Ватермарка", true),
            new BooleanSetting("Список модулей", true));

    public final ModeListSetting watermarkElements = new ModeListSetting("Ватермарка",
            new BooleanSetting("Ник", true),
            new BooleanSetting("Фпс", true),
            new BooleanSetting("Время", true))
            .setVisible(() -> elements.getValueByName("Ватермарка").get());

    public final ModeSetting colorMode = new ModeSetting("Цвет", "Astolfo",
            "Astolfo", "Клиент", "Свой");

    public final ColorSetting customColor = new ColorSetting("Свой цвет", ColorUtils.rgb(235, 105, 255))
            .setVisible(() -> colorMode.is("Свой"));

    public final SliderSetting fontSize = new SliderSetting("Размер шрифта", 14.0f, 10.0f, 20.0f, 1.0f);
    public final SliderSetting backgroundAlpha = new SliderSetting("Прозрачность фона", 150.0f, 0.0f, 255.0f, 5.0f);
    public final SliderSetting radius = new SliderSetting("Скругление", 4.0f, 0.0f, 8.0f, 0.5f);
    public final BooleanSetting moduleBackground = new BooleanSetting("Фон списка", true)
            .setVisible(() -> elements.getValueByName("Список модулей").get());
    public final BooleanSetting onlyBound = new BooleanSetting("Только с биндом", false)
            .setVisible(() -> elements.getValueByName("Список модулей").get());

    public final ModeListSetting categories = new ModeListSetting("Категории",
            new BooleanSetting("Combat", true),
            new BooleanSetting("Movement", true),
            new BooleanSetting("Player", true),
            new BooleanSetting("Render", true),
            new BooleanSetting("Misc", true))
            .setVisible(() -> elements.getValueByName("Список модулей").get());

    public HudElusive() {
        addSettings(elements, watermarkElements, colorMode, customColor, fontSize,
                backgroundAlpha, radius, moduleBackground, onlyBound, categories);
    }

    @Override
    public boolean onEnable() {
        super.onEnable();
        previousHud = null;
        previousHudEnabled = false;
        for (Module module : Harmony.getInstance().getModuleManager().getModules()) {
            if (module != this && module.getName().equalsIgnoreCase("HUD")) {
                previousHud = module;
                previousHudEnabled = module.isState();
                if (previousHudEnabled) module.setState(false, false);
                break;
            }
        }
        return false;
    }

    @Override
    public boolean onDisable() {
        if (previousHud != null && previousHudEnabled && !previousHud.isState()) {
            previousHud.setState(true, false);
        }
        previousHud = null;
        previousHudEnabled = false;
        super.onDisable();
        return false;
    }

    @Subscribe
    public void onDisplay(EventDisplay event) {
        if (event.getType() != EventDisplay.Type.HIGH || mc.player == null
                || mc.gameSettings.showDebugInfo || Boolean.getBoolean("bot.mode")) {
            return;
        }

        MatrixStack matrix = event.getMatrixStack();
        if (elements.getValueByName("Ватермарка").get()) {
            drawWatermark(matrix);
        }
        if (elements.getValueByName("Список модулей").get()) {
            drawArrayList(matrix);
        }
    }

    private void drawWatermark(MatrixStack matrix) {
        int size = clampFontSize();
        StyledFont textFont = ClientFonts.elusiveText[size];
        StyledFont iconFont = ClientFonts.elusiveIcons[size];
        StyledFont logoFont = ClientFonts.elusiveLogo[Math.min(24, size + 4)];
        if (textFont == null || iconFont == null || logoFont == null) return;

        List<Part> parts = new ArrayList<>();
        if (watermarkElements.getValueByName("Ник").get()) parts.add(new Part("u", mc.getSession().getUsername()));
        if (watermarkElements.getValueByName("Фпс").get()) parts.add(new Part("f", mc.getDebugFPS() + " fps"));
        if (watermarkElements.getValueByName("Время").get()) parts.add(new Part("t", LocalTime.now().format(TIME_FORMAT)));

        float x=1.0f, y=2.0f;
        float height=Math.max(10.0f, textFont.getFontHeight()+4.0f);
        float logoWidth=15.0f;
        int bg=ColorUtils.rgba(5,5,5,Math.round(backgroundAlpha.get()));
        RenderUtility.drawRoundedRect(x,y,logoWidth,height,radius.get(),bg);
        logoFont.drawCenteredString(matrix,"q",x+logoWidth/2.0f,y+height/2.0f-logoFont.getFontHeight()/2.0f,-1);
        if (parts.isEmpty()) return;

        float bodyWidth=8.0f;
        for (Part part:parts) bodyWidth+=iconFont.getWidth(part.icon)+3.0f+textFont.getWidth(part.text)+6.5f;
        float bodyX=22.0f;
        RenderUtility.drawRoundedRect(bodyX,y,bodyWidth,height,radius.get(),bg);
        float currentX=bodyX+3.5f;
        float textY=y+height/2.0f-textFont.getFontHeight()/2.0f;
        float iconY=y+height/2.0f-iconFont.getFontHeight()/2.0f;
        for (Part part:parts) {
            iconFont.drawString(matrix,part.icon,currentX,iconY,-1);
            currentX+=iconFont.getWidth(part.icon)+3.0f;
            textFont.drawString(matrix,part.text,currentX,textY,-1);
            currentX+=textFont.getWidth(part.text)+6.5f;
        }
    }

    private void drawArrayList(MatrixStack matrix) {
        int size=clampFontSize();
        StyledFont font=ClientFonts.elusiveText[size];
        if (font==null) return;
        List<Module> enabled=new ArrayList<>();
        for (Module module:Harmony.getInstance().getModuleManager().getModules()) {
            if (!module.isState() || !module.isVisibleInArrayList() || !categoryEnabled(module.getCategory())) continue;
            if (onlyBound.get() && module.getBind()==0) continue;
            enabled.add(module);
        }
        enabled.sort(Comparator.comparingDouble((Module m)->font.getWidth(m.getName())).reversed());
        float screenWidth=mc.getMainWindow().getScaledWidth();
        float rowHeight=font.getFontHeight()+4.0f;
        int bg=ColorUtils.rgba(0,0,0,Math.round(backgroundAlpha.get()));
        int accent=getAccent(7);
        for (int i=0;i<enabled.size();i++) {
            String name=enabled.get(i).getName();
            float textWidth=font.getWidth(name);
            float y=i*rowHeight;
            float x=screenWidth-textWidth-4.5f;
            if (moduleBackground.get()) {
                RenderUtility.drawRoundedRect(x, y + 1.0f, screenWidth - x, rowHeight - 1.0f, 0.0f, bg);
            }
            RenderUtility.drawRoundedRect(screenWidth - 2.0f, y, 2.0f, rowHeight + 1.0f, 0.0f, accent);
            font.drawStringWithShadow(matrix,name,screenWidth-textWidth-3.0f,y+2.9f,accent);
        }
    }

    private boolean categoryEnabled(Category category) {
        return categories.getValueByName(category.getName()).get();
    }

    private int clampFontSize() {
        return Math.max(10, Math.min(20, Math.round(fontSize.get())));
    }

    private int getAccent(int offset) {
        if (colorMode.is("Клиент")) {
            return Theme.MainColor(offset);
        }
        if (colorMode.is("Свой")) {
            return customColor.get();
        }
        double wave = System.currentTimeMillis() % 3000L / 3000.0;
        float hue = (float) (0.83 + Math.sin(wave * Math.PI * 2.0) * 0.08);
        if (hue > 1.0f) {
            hue -= 1.0f;
        }
        return 0xFF000000 | (Color.HSBtoRGB(hue, 0.42f, 1.0f) & 0x00FFFFFF);
    }

    private static final class Part {
        private final String icon;
        private final String text;

        private Part(String icon, String text) {
            this.icon = icon;
            this.text = text;
        }
    }
}
