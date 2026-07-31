package xd.harm.ui.clickgui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector4f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import ru.hogoshi.Animation;
import ru.hogoshi.util.Easings;
import xd.harm.Harmony;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.impl.render.FiguraCosmetic;
import xd.harm.modules.impl.render.ClickGui;
import xd.harm.modules.impl.render.Theme;
import xd.harm.modules.settings.Setting;
import xd.harm.modules.settings.impl.*;
import xd.harm.ui.clickgui.components.builder.Component;
import xd.harm.ui.clickgui.components.settings.*;
import xd.harm.utils.SoundUtil;
import xd.harm.utils.math.AnimationMath;
import xd.harm.utils.math.Vector4i;
import xd.harm.utils.render.KawaseBlur;
import xd.harm.utils.render.color.ColorUtils;
import xd.harm.utils.render.font.Fonts;
import xd.harm.utils.render.gl.Scissor;
import xd.harm.utils.render.gl.Stencil;
import xd.harm.utils.render.rect.RenderUtility;
import xd.harm.utils.client.KeyStorage;
import xd.harm.utils.text.font.ClientFonts;
import xd.harm.config.ConfigStorage;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;

public class MenuPanel extends Screen {

    private static final float PANEL_WIDTH = 420.0f;
    private static final float TOP_BAR_WIDTH = 114.0f;
    private static final float PANEL_HEIGHT = 260.0f;
    private static final float PANEL_RADIUS = 8.0f;
    private static final float HEADER_HEIGHT = 24.0f;
    private static final float CATEGORY_BAR_HEIGHT = 20.0f;
    private static final float CATEGORY_PADDING = 8.0f;
    private static final float CATEGORY_FONT_SIZE = 6.5f;
    private static final int CATEGORY_ICON_FONT_SIZE = 20;
    private static final float CATEGORY_ICON_SCALE = 1.0f;
    private static final float CATEGORY_SEPARATOR_GAP = 3.0f;
    private static final float CATEGORY_SEPARATOR_WIDTH = 0.5f;
    private static final float CATEGORY_HITBOX_PADDING_X = 2.0f;
    private static final Category[] CATEGORIES = Category.values();
    private static final CategoryBarLayout CATEGORY_BAR_LAYOUT = new CategoryBarLayout(CATEGORIES, CATEGORIES.length);
    private static boolean categoryBarMetricsReady;

    private static final float MODULE_CELL_WIDTH = 120.0f;
    private static final float MODULE_CELL_HEIGHT = 16.0f;
    private static final float MODULE_GAP_X = 4.0f;
    private static final float MODULE_GAP_Y = 3.0f;
    private static final int MODULES_PER_ROW = 2;
    private static final float MODULE_SEARCH_AREA_HEIGHT = 12.0f;
    private static final int MODULE_SEARCH_MAX_CHARS = 96;
    private static final String MODULE_SEARCH_PLACEHOLDER = "\u041F\u043E\u0438\u0441\u043A \u0424\u0443\u043D\u043A\u0446\u0438\u0439 Ctrl + F";
    private static String moduleSearchSavedText = "";

    private static final float LEFT_PANEL_WIDTH = 260.0f;
    private static final float SETTINGS_PANEL_WIDTH = 148.0f;
    private static final float CLICKGUI_IMAGE_MARGIN = 12.0f;
    private static final ClickGuiImageLayout[] CLICKGUI_IMAGE_LAYOUTS = {
            new ClickGuiImageLayout("1.png", 200.0f, 280.0f, 20.0f, 0.0f),
            new ClickGuiImageLayout("2.png", 210.0f, 220.0f, 0.0f, - 20f),
            new ClickGuiImageLayout("3.png", 200.0f, 220.0f, 0.0f, - 20f),
            new ClickGuiImageLayout("4.png", 210.0f, 220.0f, 0.0f, - 20f),
            new ClickGuiImageLayout("5.png", 184.0f, 250.0f, 0.0f, 0.0f),
            new ClickGuiImageLayout("6.png", 150.0f, 250.0f, 0.0f, - 30f)
    };

    private static final int TAB_MAIN = 0;
    private static final int TAB_CONFIGS = 1;
    private static final int TAB_AUTOBUY = 2;
    private static final int TAB_THEME = 3;
    private static final int TAB_BOTCONFIGS = 4;
    private static final String[] TAB_ICONS = {"A", "B", "C", "D", "E"};
    private static final String[] TAB_NAMES = {"Main", "Configs", "AutoBuy", "Theme", "BotConfigs"};
    private static final int TAB_COUNT = 5;
    private static final float TOP_TAB_SPACING = 22.0f;
    private static final float TOP_TAB_PILL_WIDTH = 15.0f;
    private static final float TOP_TAB_PILL_HEIGHT = 14.0f;
    private static final float[] TOP_TAB_ICON_OFFSETS_X = {0.0f, 0.0f, 0.0f, 0.0f, 0.0f};
    private static final float TOP_TAB_ICON_OFFSET_Y = 0.0f;
    private static float TOP_TAB_PILL_OFFSET_MAIN_X = 0f;
    private static float TOP_TAB_PILL_OFFSET_CONFIGS_X = 0.1f;
    private static float TOP_TAB_PILL_OFFSET_AUTOBUY_X = -0.5f;
    private static float TOP_TAB_PILL_OFFSET_THEME_X = -0.5f;
    private static float TOP_TAB_PILL_OFFSET_BOTCONFIGS_X = -0.5f;
    private static final float TOP_TAB_PILL_OFFSET_Y = 0.0f;

    private int selectedTab = TAB_MAIN;

    // Вкладки Configs и BotConfigs используют одну и ту же панель конфигов.
    private boolean isConfigTab() {
        return selectedTab == TAB_CONFIGS || selectedTab == TAB_BOTCONFIGS;
    }
    private float tabPillX = 0;
    private boolean tabPillInit = false;
    private final float[] tabHoverAnims = new float[TAB_COUNT];
    private float contentSlideX = 1.0f;
    private float contentSlideTargetX = 1.0f;
    private int prevTab = TAB_MAIN;
    private boolean tabSwitchTransitioning = false;
    private int tabSwitchDirection = 1;

    private String configInputText = "";
    private boolean configInputFocused = false;
    private int configInputCursor = 0;
    private int configInputSelection = 0;
    private float configInputEditPulse = 0.0f;
    private int configInputEditDirection = 1;
    private long configRepeatHoldStart = 0L;
    private long configRepeatLastStep = 0L;
    private int configRepeatKey = -1;
    private float configScroll = 0;
    private float configAnimatedScroll = 0;
    private final float[] configButtonHoverAnims = new float[4];
    private final float[] configButtonClickAnims = new float[4];
    private boolean configTextSelectionDragging = false;
    private boolean configTextSelectionDragTag = false;
    private int configTextSelectionAnchor = 0;
    private float configTextSelectionDragTextX = 0.0f;
    private float configTextSelectionDragMaxWidth = 0.0f;
    private float configTextSelectionDragFontSize = 6.0f;

    private List<xd.harm.config.Config> cachedConfigs = null;
    private boolean configsLoading = false;
    private boolean configsLoaded = false;
    private final ConfigMetaStore configMetaStore = new ConfigMetaStore();
    private String configTagEditingName = null;
    private String configTagInputText = "";
    private int configTagCursor = 0;
    private int configTagSelection = 0;
    private float configTagEditPulse = 0.0f;
    private int configTagEditDirection = 1;
    private int configDragIndex = -1;
    private float configDragOffsetX = 0.0f;
    private float configDragOffsetY = 0.0f;
    private float configDragMouseY = 0.0f;
    private float configDragStartMouseY = 0.0f;
    private boolean configCardDragging = false;
    private boolean configDragMoved = false;
    private final Map<String, Float> configCardHoverAnims = new HashMap<>();
    private final Map<String, Float> configDelHoverAnims = new HashMap<>();
    private final Map<String, float[]> configCardAnimPos = new HashMap<>();
    private float lastConfigPanelX = Float.NaN;
    private float lastConfigContentTop = Float.NaN;

    private int autoBuySelectedIndex = 0;
    private float autoBuyScroll = 0;
    private float autoBuyAnimScroll = 0;
    private AutoBuyField autoBuyBuyField = null;
    private AutoBuyField autoBuySellField = null;
    private int autoBuyEditorIndex = -1;
    private boolean autoBuyScrollDragging = false;
    private float autoBuyScrollDragOffset = 0;
    private float autoBuyItemRotation = 0;
    private static final float AB_ITEM_SIZE = 28;
    private static final float AB_ITEM_GAP = 5;
    private static final int AB_COLS = 5;

    private boolean configScrollDragging = false;
    private float configScrollDragOffset = 0;
    private boolean moduleScrollDragging = false;
    private float moduleScrollDragOffset = 0;
    private boolean moduleSearchSelectionDragging = false;
    private int moduleSearchSelectionAnchor = 0;
    private long moduleSearchRepeatHoldStart = 0L;
    private long moduleSearchRepeatLastStep = 0L;
    private int moduleSearchRepeatKey = -1;
    private long suppressBindCharTypedUntil = 0L;
    private boolean panelDragging = false;
    private float panelDragOffsetX = 0.0f;
    private float panelDragOffsetY = 0.0f;

    private final List<Ripple> clickRipples = new ArrayList<>();
    private final ColorComponent themeCustomPicker = new ColorComponent(Theme.visualscolor)
            .setHeaderVisible(false)
            .setActionButtonsVisible(false)
            .setPickerPanelHeight(62f);
    private float themeCustomPickerOffsetX = 0.0f;
    private float themeCustomPickerOffsetY = 0.0f;
    private float themeCustomPickerAnchorX = Float.NaN;
    private float themeCustomPickerAnchorY = Float.NaN;
    private float themePanelScroll = 0;
    private float themePanelAnimatedScroll = 0;

    private Category selectedCategory = Category.Combat;
    private TextFieldWidget moduleSearchField = null;
    private float pillX = 0;
    private float pillWidth = 0;
    private boolean pillInitialized = false;
    private float lastCategoryBarX = Float.NaN;
    private float lastCategoryBarY = Float.NaN;
    private float scroll = 0;
    private float animatedScroll = 0;

    private Module selectedModule = null;
    private Module pendingModule = null;
    private boolean settingsTransitioning = false;
    private boolean settingsSwapping = false;
    private final ObjectArrayList<Component> settingsComponents = new ObjectArrayList<>();
    // Сохраняет свёрнутые категории: ключ "имяМодуля|имяКатегории" → true/false
    private final Map<String, Boolean> collapsedCategories = new HashMap<>();
    private float settingsScroll = 0;
    private float settingsAnimatedScroll = 0;
    private float settingsAlpha = 0;

    private Animation openAnimation = new Animation();
    private boolean closing = false;
    private boolean closeSoundPlayed = false;

    private static float panelPosX = -1;
    private static float panelPosY = -1;

    private final Map<String, Float> moduleToggleAnims = new HashMap<>();
    private final Map<String, Boolean> moduleToggleStates = new HashMap<>();
    private final Map<String, Float> enabledAnims = new HashMap<>();
    private final Map<String, Float> bindAnims = new HashMap<>();
    private final List<Module> visibleModulesCache = new ArrayList<>();
    private Category visibleModulesCacheCategory = null;
    private String visibleModulesCacheQuery = null;
    private int visibleModulesCacheSourceSize = -1;
    private boolean visibleModulesDirty = true;
    private int lastPanelBlurFrame = -1;

    private static Field moduleSearchSelectionEndField;
    private static boolean moduleSearchSelectionFieldInit;

    private Module bindingModule = null;

    private final List<Snowflake> snowflakes = new ArrayList<>();
    private final java.util.Random snowRandom = new java.util.Random();
    private long lastSnowUpdate = System.currentTimeMillis();
    private static final int SNOW_COUNT = 45;

    public MenuPanel() {
        super(new StringTextComponent(""));
    }

    public MenuPanel openAutoBuyTab() {
        selectedTab = TAB_AUTOBUY;
        prevTab = TAB_AUTOBUY;
        tabSwitchTransitioning = false;
        contentSlideX = 1.0f;
        contentSlideTargetX = 1.0f;
        closeSettingsOverlays();
        setModuleSearchFocused(false);
        blurAutoBuyFieldFocus();
        return this;
    }

    public MenuPanel openConfigsTab() {
        selectedTab = TAB_CONFIGS;
        prevTab = TAB_CONFIGS;
        tabSwitchTransitioning = false;
        contentSlideX = 1.0f;
        contentSlideTargetX = 1.0f;
        closeSettingsOverlays();
        setModuleSearchFocused(false);
        cachedConfigs = null;
        configsLoaded = false;
        return this;
    }

    public MenuPanel openThemeTab() {
        selectedTab = TAB_THEME;
        prevTab = TAB_THEME;
        tabSwitchTransitioning = false;
        contentSlideX = 1.0f;
        contentSlideTargetX = 1.0f;
        closeSettingsOverlays();
        setModuleSearchFocused(false);
        return this;
    }

    public MenuPanel openBotConfigsTab() {
        selectedTab = TAB_BOTCONFIGS;
        prevTab = TAB_BOTCONFIGS;
        tabSwitchTransitioning = false;
        contentSlideX = 1.0f;
        contentSlideTargetX = 1.0f;
        closeSettingsOverlays();
        setModuleSearchFocused(false);
        cachedConfigs = null;
        configsLoaded = false;
        return this;
    }

    @Override
    protected void init() {
        openAnimation.setValue(0.01);
        openAnimation.animate(1, 0.45, Easings.BACK_OUT);
        closing = false;
        closeSoundPlayed = false;
        SoundUtil.playSound("dropdownen.wav");
        clickRipples.clear();
        autoBuyScrollDragging = false;
        configScrollDragging = false;
        resetConfigDrag();
        configTagEditingName = null;
        moduleScrollDragging = false;
        panelDragging = false;
        prevTab = selectedTab;
        tabSwitchTransitioning = false;
        contentSlideX = 1.0f;
        contentSlideTargetX = 1.0f;
        if (moduleSearchField == null) {
            moduleSearchField = new TextFieldWidget(font, 0, 0, width - 16, 9, new StringTextComponent(""));
            moduleSearchField.setMaxStringLength(MODULE_SEARCH_MAX_CHARS);
            moduleSearchField.setEnableBackgroundDrawing(false);
            moduleSearchField.setCanLoseFocus(true);
        }
        moduleSearchField.setText(moduleSearchSavedText);
        moduleSearchField.setFocused2(false);
        invalidateVisibleModules();
        closeSettingsOverlays();
        resetAutoBuyEditors();

        if (panelPosX < 0 || panelPosY < 0) {
            panelPosX = (width - PANEL_WIDTH) / 2.0f;
            panelPosY = (height - PANEL_HEIGHT) / 2.0f;
        }

        super.init();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        if (moduleSearchField != null) {
            moduleSearchField.tick();
        }
        updateConfigHeldKeyRepeat();
        updateModuleSearchHeldKeyRepeat();
        super.tick();
    }

    private void rebuildSettingsComponents() {
        closeSettingsOverlays();
        settingsComponents.clear();
        settingsScroll = 0;
        settingsAnimatedScroll = 0;
        if (selectedModule == null) return;

        for (Setting<?> setting : selectedModule.getSettings()) {
            Component comp = null;
            if (setting instanceof BooleanSetting bool) {
                comp = new BooleanComponent(bool);
            } else if (setting instanceof SliderSetting slider) {
                comp = new SliderComponent(slider);
            } else if (setting instanceof BindSetting bind) {
                comp = new BindComponent(bind);
            } else if (setting instanceof ModeSetting mode) {
                comp = new ModeComponent(mode);
            } else if (setting instanceof ModeListSetting mode) {
                comp = new MultiBoxComponent(mode);
            } else if (setting instanceof StringSetting string) {
                comp = new StringComponent(string);
            } else if (setting instanceof ColorSetting color) {
                comp = new ColorComponent(color);
            } else if (setting instanceof CategorySetting category) {
                CategoryComponent catComp = new CategoryComponent(category);
                // Восстанавливаем сохранённое свёрнутое состояние
                String key = selectedModule.getName() + "|" + category.getName();
                Boolean saved = collapsedCategories.get(key);
                if (saved != null && saved) {
                    catComp.setCollapsed(true);
                }
                comp = catComp;
            }
            if (comp != null) {
                comp.setSticky(setting.isSticky());
                settingsComponents.add(comp);
            }
        }
    }

    /**
     * Проверяет, скрыт ли компонент (по индексу в settingsComponents)
     * из-за того, что лежит под свёрнутой категорией.
     * Сам CategoryComponent (заголовок) никогда не скрывается.
     */
    private boolean isHiddenByCollapsedCategory(int index) {
        if (index < 0 || index >= settingsComponents.size()) return false;
        Component current = settingsComponents.get(index);
        if (current instanceof CategoryComponent) return false;
        for (int i = index - 1; i >= 0; i--) {
            Component c = settingsComponents.get(i);
            if (c instanceof CategoryComponent cat) {
                return cat.getCollapseAnim() >= 0.99f;
            }
        }
        return false;
    }

    /**
     * Возвращает прогресс сворачивания для компонента (0..1).
     * 0 = полностью виден, 1 = полностью скрыт.
     * Берёт ближайшую категорию выше по списку и её collapseAnim.
     */
    private float getCollapseProgressForComponent(int index) {
        if (index < 0 || index >= settingsComponents.size()) return 0;
        Component current = settingsComponents.get(index);
        if (current instanceof CategoryComponent) return 0;
        for (int i = index - 1; i >= 0; i--) {
            Component c = settingsComponents.get(i);
            if (c instanceof CategoryComponent cat) {
                return cat.getCollapseAnim();
            }
        }
        return 0;
    }

    /**
     * Сохраняет текущие свёрнутые состояния всех категорий в collapsedCategories.
     * Вызывается перед сменой выбранного модуля.
     */
    private void saveCollapsedStates() {
        if (selectedModule == null) return;
        String moduleName = selectedModule.getName();
        for (Component comp : settingsComponents) {
            if (comp instanceof CategoryComponent cat) {
                String key = moduleName + "|" + cat.getSetting().getName();
                collapsedCategories.put(key, cat.isCollapsed());
            }
        }
    }

    @Override
    public void render(MatrixStack stack, int mouseX, int mouseY, float partialTicks) {
        openAnimation.update();
        autoBuyItemRotation += partialTicks * 1.7f;
        float animValue = MathHelper.clamp((float) openAnimation.getValue(), 0.0f, 1.0f);

        if (closing && animValue <= 0.01f) {
            minecraft.displayGuiScreen(null);
            return;
        }

        int bgAlpha = (int) (120 * animValue);
        fill(stack, 0, 0, width, height, ColorUtils.rgba(0, 0, 0, bgAlpha));
        renderClickGuiSelectedImage(stack, animValue);

        float slideY = closing ? (1.0f - animValue) * 15.0f : (1.0f - animValue) * 15.0f;

        float baseX = panelPosX;
        float y = panelPosY + slideY;
        float panelX = baseX;
        float panelY = y;
        float alphaFactor = animValue;

        if (animValue >= 0.02f) {
            int frame = minecraft.getFrameTimer().getIndex();
            if (frame != lastPanelBlurFrame) {
                KawaseBlur.blur.updateBlur(3.0f, 3);
                lastPanelBlurFrame = frame;
            }
            float finalPanelX = panelX;
            float finalPanelY = panelY;
            KawaseBlur.blur.render(() -> {
                GL11.glEnable(GL11.GL_ALPHA_TEST);
                GL11.glAlphaFunc(GL11.GL_GREATER, 0.01f);
                RenderUtility.drawRoundedRect(finalPanelX, finalPanelY, PANEL_WIDTH, PANEL_HEIGHT, new Vector4f(PANEL_RADIUS, PANEL_RADIUS, PANEL_RADIUS, PANEL_RADIUS), ColorUtils.rgba(255, 255, 255, (int)(255 * animValue)));
                GL11.glDisable(GL11.GL_ALPHA_TEST);
            });
        }

        renderClickRipples(panelX, panelY, alphaFactor);

        RenderUtility.drawRoundedRect(
                panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT,
                new Vector4f(PANEL_RADIUS, PANEL_RADIUS, PANEL_RADIUS, PANEL_RADIUS),
                ColorUtils.rgba(0, 0, 0, (int)(160 * alphaFactor))
        );

        int headerColor = ColorUtils.rgba(8, 8, 8, (int)(185 * alphaFactor));
        RenderUtility.drawRoundedRect(panelX, panelY, PANEL_WIDTH, HEADER_HEIGHT, new Vector4f(0.0f, PANEL_RADIUS, 0.0f, PANEL_RADIUS), headerColor);

        renderHeader(stack, panelX, panelY, alphaFactor);

        float contentTop = panelY + HEADER_HEIGHT + 2;
        float contentBottom = panelY + PANEL_HEIGHT - 4;
        float contentHeight = contentBottom - contentTop;

        Scissor.push();
        Scissor.setFromComponentCoordinates(panelX + 1.0f, panelY + HEADER_HEIGHT + 0.5f, PANEL_WIDTH - 2.0f, PANEL_HEIGHT - HEADER_HEIGHT - 1.5f);
        renderTabContent(stack, selectedTab, panelX, panelY, contentTop, contentHeight, contentBottom, mouseX, mouseY, alphaFactor);
        Scissor.pop();
        renderModuleSearchField(stack, mouseX, mouseY, alphaFactor);

        RenderUtility.drawRoundedRectOutline(
                panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT,
                PANEL_RADIUS, 1.0f,
                ColorUtils.rgba(255, 255, 255, (int)(35 * alphaFactor))
        );
        renderSnow(0, 0, width, height, 0, alphaFactor);
        renderMainTabOverlays(stack, mouseX, mouseY);
        renderClickGuiSelectedHighImage(stack, alphaFactor);
        renderTabBar(stack, baseX, y, mouseX, mouseY, animValue);

        super.render(stack, mouseX, mouseY, partialTicks);
    }

    private void startTabSwitchTransition(int newTab) {
        if (newTab == selectedTab) {
            return;
        }
        prevTab = selectedTab;
        selectedTab = newTab;
        tabSwitchDirection = newTab > prevTab ? 1 : -1;
        tabSwitchTransitioning = false;
        contentSlideX = 1.0f;
        contentSlideTargetX = 1.0f;
    }

    private void renderTabContent(MatrixStack stack, int tab, float panelX, float panelY, float contentTop, float contentHeight, float contentBottom, int mouseX, int mouseY, float alpha) {
        switch (tab) {
            case TAB_MAIN:
                renderCategoryBar(stack, panelX, panelY + HEADER_HEIGHT, mouseX, mouseY, alpha);
                float mainContentTop = panelY + HEADER_HEIGHT + CATEGORY_BAR_HEIGHT + 2;
                float mainContentHeight = contentBottom - mainContentTop;
                updateToggleAnimations();
                renderModuleGrid(stack, panelX, mainContentTop, mainContentHeight, mouseX, mouseY, alpha);
                renderSettingsPanel(stack, panelX, mainContentTop, mainContentHeight, mouseX, mouseY, alpha);
                break;
            case TAB_CONFIGS:
            case TAB_BOTCONFIGS:
                renderConfigsPanel(stack, panelX, contentTop, contentHeight, mouseX, mouseY, alpha);
                break;
            case TAB_AUTOBUY:
                renderAutoBuyPanel(stack, panelX, contentTop, contentHeight, mouseX, mouseY, alpha);
                break;
            case TAB_THEME:
                renderThemePanel(stack, panelX, contentTop, contentHeight, mouseX, mouseY, alpha);
                break;
            default:
                break;
        }
    }

    private float easeInOutCubic(float value) {
        float t = MathHelper.clamp(value, 0.0f, 1.0f);
        if (t < 0.5f) {
            return 4.0f * t * t * t;
        }
        float inv = -2.0f * t + 2.0f;
        return 1.0f - (inv * inv * inv) / 2.0f;
    }

    private void renderHeader(MatrixStack stack, float x, float y, float alpha) {
        double time = System.currentTimeMillis() / 1000.0;

        String title = "Harmony";
        float titleSize = 7.2f;
        float logoWidth = 8.0f;
        float gap = 3.0f;
        float titleWidth = Fonts.sfuy.getWidth(title, titleSize);
        float groupWidth = logoWidth + gap + titleWidth;
        float groupStart = x + (PANEL_WIDTH - groupWidth) / 2.0f;

        float logoFloatY = (float) (Math.sin(time * 2.0) * 1.2);

        int themeColor = Theme.MainColor(0);
        int logoColor = ColorUtils.setAlpha(themeColor, (int)(255 * alpha));

        float logoX = groupStart + logoWidth / 2.0f;
        float logoY = y + HEADER_HEIGHT / 2.0f - 1.2f + logoFloatY;

        RenderUtility.drawShadow(logoX - 4, logoY - 3, 8, 6, 10, ColorUtils.setAlpha(themeColor, (int)(60 * alpha)));
        RenderUtility.drawShadow(logoX - 6, logoY - 5, 12, 10, 16, ColorUtils.setAlpha(themeColor, (int)(25 * alpha)));

        ClientFonts.watermark[17].drawString(stack, "A", groupStart, logoY, logoColor);

        Fonts.sfuy.drawText(stack, title, groupStart + logoWidth + gap, y + HEADER_HEIGHT / 2.0f - titleSize / 2.0f + 0.6f, ColorUtils.rgba(255, 255, 255, (int)(245 * alpha)), titleSize);

        renderBetaBadge(stack, groupStart + groupWidth, y, alpha);
    }

    private void renderBetaBadge(MatrixStack stack, float anchorX, float panelY, float alpha) {
        if (alpha < 0.01f) return;

        double time = System.currentTimeMillis() / 1000.0;
        String betaText = "Beta";
        float fontSize = 5.0f;
        float textWidth = Fonts.sfuy.getWidth(betaText, fontSize);

        float padX = 3.5f;
        float padY = 1.5f;
        float badgeW = textWidth + padX * 2f;
        float badgeH = fontSize + padY * 2f + 1f;
        float badgeX = anchorX + 3f;
        float floatY = (float) (Math.sin(time * 1.8) * 0.8);
        float badgeY = panelY + HEADER_HEIGHT / 2.0f - badgeH / 2.0f + floatY;
        float cornerR = 3.0f;

        int themeColor = Theme.MainColor(0);
        int tr = (themeColor >> 16) & 0xFF;
        int tg = (themeColor >> 8) & 0xFF;
        int tb = themeColor & 0xFF;

        float colorShift = (float)(Math.sin(time * 1.5) * 0.5 + 0.5);
        int cr = (int)(tr * (0.8f + 0.2f * colorShift));
        int cg = (int)(tg * (0.8f + 0.2f * colorShift));
        int cb = (int)(tb * (0.8f + 0.2f * colorShift));

        int glassTL = ColorUtils.rgba(cr, cg, cb, (int)(alpha * 25));
        int glassTR = ColorUtils.rgba(Math.min(255, cr + 30), Math.min(255, cg + 30), Math.min(255, cb + 30), (int)(alpha * 15));
        int glassBL = ColorUtils.rgba(cr / 2, cg / 2, cb / 2, (int)(alpha * 30));
        int glassBR = ColorUtils.rgba(cr, cg, cb, (int)(alpha * 20));

        RenderUtility.drawRoundedRect(badgeX, badgeY, badgeW, badgeH,
                new Vector4f(cornerR, cornerR, cornerR, cornerR),
                new Vector4i(glassTL, glassTR, glassBL, glassBR));

        float breathe = (float)(Math.sin(time * 2.0) * 0.5 + 0.5);
        int outlineAlpha = (int)(alpha * (40 + 25 * breathe));

        GL11.glPushMatrix();
        GL11.glTranslatef(badgeX, badgeY, 0);
        RenderUtility.drawRoundedOutline(0, 0, badgeW, badgeH, cornerR, 0.5f, ColorUtils.rgba(cr, cg, cb, outlineAlpha));
        GL11.glPopMatrix();

        float startTextX = badgeX + padX;
        for (int i = 0; i < betaText.length(); i++) {
            String ch = String.valueOf(betaText.charAt(i));
            float charWidth = Fonts.sfuy.getWidth(ch, fontSize);

            float charPhase = (float)(Math.sin(time * 2.5 + i * 0.6) * 0.5 + 0.5);
            float charBright = 0.65f + 0.35f * charPhase;

            int charColor = ColorUtils.rgba(
                    Math.min(255, (int)(cr * 0.4f + 255 * 0.6f * charBright)),
                    Math.min(255, (int)(cg * 0.4f + 255 * 0.6f * charBright)),
                    Math.min(255, (int)(cb * 0.4f + 255 * 0.6f * charBright)),
                    (int)(alpha * (200 + 55 * charPhase))
            );

            float charYOff = (float)(Math.sin(time * 3.0 + i * 0.8) * 0.3);
            Fonts.sfuy.drawText(stack, ch, startTextX, badgeY + padY + charYOff, charColor, fontSize);
            startTextX += charWidth;
        }
    }


    private static final int TAB_ICON_FONT_SIZE = 25;

    private void renderTabBar(MatrixStack stack, float panelX, float panelY, int mouseX, int mouseY, float alpha) {
        int themeColor = Theme.MainColor(0);
        float barW = Math.min(PANEL_WIDTH, TOP_BAR_WIDTH);
        float barX = getStaticTopTabBarX();
        float tabSpacing = TOP_TAB_SPACING;
        float totalWidth = (TAB_COUNT - 1) * tabSpacing;
        float startX = barX + (barW - totalWidth) / 2.0f;

        float barH = 18.0f;
        float barY = 4;
        float iconY = barY + barH / 2.0f + TOP_TAB_ICON_OFFSET_Y;

        float pillW = TOP_TAB_PILL_WIDTH;
        float pillH = TOP_TAB_PILL_HEIGHT;

        float selectedIconX = getTopTabCenterX(startX, tabSpacing, selectedTab);
        float targetPillX = selectedIconX - pillW / 2.0f + getTopTabPillOffsetX(selectedTab);
        if (!tabPillInit) {
            tabPillX = targetPillX;
            tabPillInit = true;
        }
        tabPillX = AnimationMath.fast(tabPillX, targetPillX, 12);

        float pillYPos = barY + (barH - pillH) / 2.0f + TOP_TAB_PILL_OFFSET_Y;

        RenderUtility.drawRoundedRect(barX, barY, barW, barH, 5.0f, ColorUtils.rgba(10, 10, 10, (int)(170 * alpha)));
        RenderUtility.drawRoundedRectOutline(barX, barY, barW, barH, 5.0f, 0.5f, ColorUtils.rgba(255, 255, 255, (int)(20 * alpha)));

        RenderUtility.drawRoundedRect(tabPillX, pillYPos, pillW, pillH, 4.0f, ColorUtils.setAlpha(themeColor, (int)(40 * alpha)));

        for (int i = 0; i < TAB_COUNT; i++) {
            float iconX = getTopTabCenterX(startX, tabSpacing, i);
            boolean isSelected = i == selectedTab;
            boolean hovered = RenderUtility.isInRegion(mouseX, mouseY, iconX - pillW / 2f, pillYPos, pillW, pillH);

            float hoverTarget = hovered ? 1.0f : 0.0f;
            tabHoverAnims[i] = AnimationMath.fast(tabHoverAnims[i], hoverTarget, 10);

            int iconColor;
            if (isSelected) {
                iconColor = ColorUtils.setAlpha(themeColor, (int)(255 * alpha));
            } else {
                int baseAlpha = (int)((80 + 60 * tabHoverAnims[i]) * alpha);
                iconColor = ColorUtils.rgba(255, 255, 255, baseAlpha);
            }

            if (isSelected) {
                float glowSize = 6.0f;
                RenderUtility.drawShadow(iconX - glowSize, iconY - glowSize - 2, glowSize * 2, glowSize * 2, 8, ColorUtils.setAlpha(themeColor, (int)(50 * alpha)));
            }

            float iconW = ClientFonts.upico[TAB_ICON_FONT_SIZE].getWidth(TAB_ICONS[i]);
            float iconH = ClientFonts.upico[TAB_ICON_FONT_SIZE].getFontHeight();
            float iconDrawX = iconX - iconW / 2f;
            float iconDrawY = iconY - iconH / 2f + 5f;
            ClientFonts.upico[TAB_ICON_FONT_SIZE].drawString(stack, TAB_ICONS[i], iconDrawX, iconDrawY, iconColor);

            if (tabHoverAnims[i] > 0.05f) {
                float labelAlpha = tabHoverAnims[i] * alpha;
                float labelY = barY + barH + 2;
                Fonts.sfuy.drawCenteredText(stack, TAB_NAMES[i], iconX, labelY, ColorUtils.rgba(255, 255, 255, (int)(200 * labelAlpha)), 5.0f);
            }
        }
    }

    private void updateToggleAnimations() {
        Iterator<Map.Entry<String, Float>> toggleIterator = moduleToggleAnims.entrySet().iterator();
        while (toggleIterator.hasNext()) {
            Map.Entry<String, Float> entry = toggleIterator.next();
            float val = AnimationMath.fast(entry.getValue(), 0, 6);
            if (val <= 0.01f) {
                toggleIterator.remove();
            } else {
                entry.setValue(val);
            }
        }

        List<Module> modules = getVisibleModules();
        for (Module m : modules) {
            float target = m.isState() ? 1.0f : 0.0f;
            float current = enabledAnims.getOrDefault(m.getName(), 0.0f);
            float newVal = AnimationMath.fast(current, target, 8);
            enabledAnims.put(m.getName(), newVal);
        }

        if (settingsTransitioning) {
            settingsAlpha = AnimationMath.fast(settingsAlpha, 0, 12);
            if (settingsAlpha <= 0.05f) {
                settingsTransitioning = false;
                closeSettingsOverlays();
                saveCollapsedStates();
                selectedModule = pendingModule;
                pendingModule = null;
                if (selectedModule != null) {
                    rebuildSettingsComponents();
                } else {
                    settingsComponents.clear();
                }
                settingsScroll = 0;
                settingsAnimatedScroll = 0;
            }
        } else {
            float settingsTarget = selectedModule != null ? 1.0f : 0.0f;
            settingsAlpha = AnimationMath.fast(settingsAlpha, settingsTarget, 10);
        }
    }

    private void renderCategoryBar(MatrixStack stack, float barX, float barY, int mouseX, int mouseY, float alpha) {
        CategoryBarLayout layout = createCategoryBarLayout(barX, barY);
        Category[] categories = layout.categories;
        int count = categories.length;
        float barInnerX = layout.barInnerX;
        float barInnerWidth = layout.barInnerWidth;
        float barRectY = layout.barRectY;
        float barRectH = layout.barRectH;

        RenderUtility.drawRoundedRect(barInnerX, barRectY, barInnerWidth, barRectH, 5.0f, ColorUtils.rgba(255, 255, 255, (int)(6 * alpha)));

        float targetPillX = 0;
        float targetPillWidth = 0;

        for (int i = 0; i < count; i++) {
            if (categories[i] == selectedCategory) {
                targetPillX = layout.tabXPositions[i];
                targetPillWidth = layout.tabWidths[i];
            }
        }

        boolean categoryBarMoved = Float.isNaN(lastCategoryBarX) || Float.isNaN(lastCategoryBarY)
                || Math.abs(lastCategoryBarX - barX) > 0.01f
                || Math.abs(lastCategoryBarY - barY) > 0.01f;

        if (!pillInitialized || categoryBarMoved || panelDragging) {
            pillX = targetPillX;
            pillWidth = targetPillWidth;
            pillInitialized = true;
        } else {
            pillX = AnimationMath.fast(pillX, targetPillX, 12);
            pillWidth = AnimationMath.fast(pillWidth, targetPillWidth, 12);
        }

        lastCategoryBarX = barX;
        lastCategoryBarY = barY;

        float pillH = barRectH - 4;
        float pillY = barRectY + 2;

        RenderUtility.drawRoundedRect(pillX, pillY, pillWidth, pillH, 3.5f, ColorUtils.setAlpha(Theme.MainColor(0), (int)(60 * alpha)));

        int themeColor = Theme.MainColor(0);
        float tR = ((themeColor >> 16) & 0xFF) / 255.0f;
        float tG = ((themeColor >> 8) & 0xFF) / 255.0f;
        float tB = (themeColor & 0xFF) / 255.0f;

        double time = System.currentTimeMillis() / 1000.0;
        float headAngle = (float)((time * 2.0) % (Math.PI * 2));

        {
            float px = pillX;
            float py = pillY;
            float pw = pillWidth;
            float ph = pillH;
            float r = Math.min(3.5f, Math.min(pw, ph) / 2f);
            float x2 = px + pw;
            float y2 = py + ph;
            float cx = px + pw / 2f;
            float cy = py + ph / 2f;
            int segments = 12;
            float tailLen = 1.8f;

            GlStateManager.pushMatrix();
            GlStateManager.enableBlend();
            GlStateManager.disableTexture();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
            GL11.glLineWidth(1.5f);
            GL11.glShadeModel(GL11.GL_SMOOTH);

            GL11.glBegin(GL11.GL_LINE_LOOP);


            for (int j = 0; j <= segments; j++) {
                double a = Math.toRadians(180 + (90.0 * j / segments));
                float vx = (float)(px + r + Math.cos(a) * r);
                float vy = (float)(py + r + Math.sin(a) * r);
                setOutlineVertexColor(vx, vy, cx, cy, headAngle, tailLen, tR, tG, tB, alpha);
                GL11.glVertex2f(vx, vy);
            }
            setOutlineVertexColor(px + r, py, cx, cy, headAngle, tailLen, tR, tG, tB, alpha);
            GL11.glVertex2f(px + r, py);
            setOutlineVertexColor(x2 - r, py, cx, cy, headAngle, tailLen, tR, tG, tB, alpha);
            GL11.glVertex2f(x2 - r, py);
            for (int j = 0; j <= segments; j++) {
                double a = Math.toRadians(270 + (90.0 * j / segments));
                float vx = (float)(x2 - r + Math.cos(a) * r);
                float vy = (float)(py + r + Math.sin(a) * r);
                setOutlineVertexColor(vx, vy, cx, cy, headAngle, tailLen, tR, tG, tB, alpha);
                GL11.glVertex2f(vx, vy);
            }
            setOutlineVertexColor(x2, py + r, cx, cy, headAngle, tailLen, tR, tG, tB, alpha);
            GL11.glVertex2f(x2, py + r);
            setOutlineVertexColor(x2, y2 - r, cx, cy, headAngle, tailLen, tR, tG, tB, alpha);
            GL11.glVertex2f(x2, y2 - r);
            for (int j = 0; j <= segments; j++) {
                double a = Math.toRadians(0 + (90.0 * j / segments));
                float vx = (float)(x2 - r + Math.cos(a) * r);
                float vy = (float)(y2 - r + Math.sin(a) * r);
                setOutlineVertexColor(vx, vy, cx, cy, headAngle, tailLen, tR, tG, tB, alpha);
                GL11.glVertex2f(vx, vy);
            }
            setOutlineVertexColor(x2 - r, y2, cx, cy, headAngle, tailLen, tR, tG, tB, alpha);
            GL11.glVertex2f(x2 - r, y2);
            setOutlineVertexColor(px + r, y2, cx, cy, headAngle, tailLen, tR, tG, tB, alpha);
            GL11.glVertex2f(px + r, y2);
            for (int j = 0; j <= segments; j++) {
                double a = Math.toRadians(90 + (90.0 * j / segments));
                float vx = (float)(px + r + Math.cos(a) * r);
                float vy = (float)(y2 - r + Math.sin(a) * r);
                setOutlineVertexColor(vx, vy, cx, cy, headAngle, tailLen, tR, tG, tB, alpha);
                GL11.glVertex2f(vx, vy);
            }
            setOutlineVertexColor(px, y2 - r, cx, cy, headAngle, tailLen, tR, tG, tB, alpha);
            GL11.glVertex2f(px, y2 - r);
            setOutlineVertexColor(px, py + r, cx, cy, headAngle, tailLen, tR, tG, tB, alpha);
            GL11.glVertex2f(px, py + r);

            GL11.glEnd();

            GL11.glShadeModel(GL11.GL_FLAT);
            GL11.glDisable(GL11.GL_LINE_SMOOTH);
            GlStateManager.enableTexture();
            GlStateManager.disableBlend();
            GlStateManager.popMatrix();
        }

        for (int i = 0; i < count; i++) {
            float tabCenterX = layout.tabXPositions[i] + layout.tabWidths[i] / 2.0f;
            float contentStartX = tabCenterX - layout.fullWidths[i] / 2.0f;
            float textY = barRectY + barRectH / 2.0f - CATEGORY_FONT_SIZE / 2.0f;

            boolean isSelected = categories[i] == selectedCategory;
            int nameColor = isSelected
                    ? ColorUtils.rgba(255, 255, 255, (int)(240 * alpha))
                    : ColorUtils.rgba(255, 255, 255, (int)(100 * alpha));
            int iconColor = isSelected
                    ? ColorUtils.setAlpha(themeColor, (int)(255 * alpha))
                    : ColorUtils.rgba(255, 255, 255, (int)(90 * alpha));

            float iconH = ClientFonts.keybind[CATEGORY_ICON_FONT_SIZE].getFontHeight() * CATEGORY_ICON_SCALE;
            float iconDrawY = barRectY + (barRectH - iconH) + 2.1f;

            if (isSelected) {
                float iconW = layout.iconWidths[i];
                RenderUtility.drawShadow(
                        contentStartX - 1, iconDrawY - 4,
                        iconW + 2, iconH + 2,
                        8,
                        ColorUtils.setAlpha(themeColor, (int)(80 * alpha))
                );
            }

            GL11.glPushMatrix();
            GL11.glTranslatef(contentStartX, iconDrawY, 0);
            GL11.glScalef(CATEGORY_ICON_SCALE, CATEGORY_ICON_SCALE, 1.0f);
            ClientFonts.keybind[CATEGORY_ICON_FONT_SIZE].drawString(stack, categories[i].getIcon(), 0, 0, iconColor);
            GL11.glPopMatrix();

            float sepX = contentStartX + layout.iconWidths[i] + CATEGORY_SEPARATOR_GAP;
            float sepH = barRectH * 0.45f;
            float sepY = barRectY + (barRectH - sepH) / 2.0f;
            int sepColor = ColorUtils.rgba(255, 255, 255, (int)(30 * alpha));
            RenderUtility.drawRectW(sepX, sepY, CATEGORY_SEPARATOR_WIDTH, sepH, sepColor);

            Fonts.sfuy.drawText(stack, categories[i].getName(), contentStartX + layout.iconWidths[i] + layout.separatorTotalWidth, textY, nameColor, CATEGORY_FONT_SIZE);
        }
    }

    private CategoryBarLayout createCategoryBarLayout(float barX, float barY) {
        Category[] categories = CATEGORIES;
        int count = categories.length;
        CategoryBarLayout layout = CATEGORY_BAR_LAYOUT;
        layout.barInnerX = barX + 8.0f;
        layout.barInnerWidth = PANEL_WIDTH - 16.0f;
        layout.barRectY = barY + 3.0f;
        layout.barRectH = CATEGORY_BAR_HEIGHT - 6.0f;
        layout.separatorTotalWidth = CATEGORY_SEPARATOR_GAP + CATEGORY_SEPARATOR_WIDTH + CATEGORY_SEPARATOR_GAP;

        if (!categoryBarMetricsReady) {
            for (int i = 0; i < count; i++) {
                layout.iconWidths[i] = ClientFonts.keybind[CATEGORY_ICON_FONT_SIZE].getWidth(categories[i].getIcon()) * CATEGORY_ICON_SCALE;
                layout.nameWidths[i] = Fonts.sfuy.getWidth(categories[i].getName(), CATEGORY_FONT_SIZE);
                layout.fullWidths[i] = layout.iconWidths[i] + layout.separatorTotalWidth + layout.nameWidths[i];
            }
            categoryBarMetricsReady = true;
        }

        float totalContentWidth = 0.0f;
        for (int i = 0; i < count; i++) {
            totalContentWidth += layout.fullWidths[i];
        }

        float totalPadding = CATEGORY_PADDING * count;
        float spacing = (layout.barInnerWidth - totalPadding - totalContentWidth) / (count + 1);
        float currentX = layout.barInnerX + spacing;
        for (int i = 0; i < count; i++) {
            layout.tabXPositions[i] = currentX;
            layout.tabWidths[i] = layout.fullWidths[i] + CATEGORY_PADDING;
            currentX += layout.tabWidths[i] + spacing;
        }

        return layout;
    }

    private static final class CategoryBarLayout {
        private final Category[] categories;
        private final float[] iconWidths;
        private final float[] nameWidths;
        private final float[] fullWidths;
        private final float[] tabXPositions;
        private final float[] tabWidths;
        private float barInnerX;
        private float barInnerWidth;
        private float barRectY;
        private float barRectH;
        private float separatorTotalWidth;

        private CategoryBarLayout(Category[] categories, int count) {
            this.categories = categories;
            this.iconWidths = new float[count];
            this.nameWidths = new float[count];
            this.fullWidths = new float[count];
            this.tabXPositions = new float[count];
            this.tabWidths = new float[count];
        }
    }

    private void setOutlineVertexColor(float vx, float vy, float cx, float cy, float headAngle, float tailLen, float r, float g, float b, float alpha) {
        float angle = (float) Math.atan2(vy - cy, vx - cx);
        float diff = angle - headAngle;
        diff = (float)((diff % (Math.PI * 2) + Math.PI * 3) % (Math.PI * 2) - Math.PI);
        float absDiff = Math.abs(diff);
        float brightness;
        if (absDiff < 0.15f) {
            brightness = 1.0f;
        } else if (absDiff < tailLen) {
            brightness = 1.0f - (absDiff - 0.15f) / (tailLen - 0.15f);
            brightness = brightness * brightness;
        } else {
            brightness = 0.0f;
        }
        float minAlpha = 0.08f;
        float finalAlpha = (minAlpha + brightness * (1.0f - minAlpha)) * alpha;
        GL11.glColor4f(r, g, b, finalAlpha);
    }

    private void renderModuleGrid(MatrixStack stack, float panelX, float contentTop, float contentHeight, int mouseX, int mouseY, float alpha) {
        List<Module> modules = getVisibleModules();
        float gridContentHeight = getMainGridContentHeight(contentHeight);

        int rows = (int) Math.ceil((double) modules.size() / MODULES_PER_ROW);
        float totalHeight = rows * (MODULE_CELL_HEIGHT + MODULE_GAP_Y);
        float maxScroll = Math.max(0, totalHeight - gridContentHeight + 4);
        scroll = MathHelper.clamp(scroll, -maxScroll, 0);
        animatedScroll = AnimationMath.fast(animatedScroll, scroll, 15);

        float gridX = panelX + 8;
        float gridWidth = LEFT_PANEL_WIDTH - 12;

        Stencil.initStencilToWrite();
        RenderUtility.drawRoundedRect(gridX, contentTop, gridWidth, gridContentHeight, 4.0f, -1);
        Stencil.readStencilBuffer(1);

        int themeColor = Theme.MainColor(0);
        double time = System.currentTimeMillis() / 1000.0;
        float rowHeight = MODULE_CELL_HEIGHT + MODULE_GAP_Y;
        int firstVisibleRow = Math.max(0, (int) Math.floor((-animatedScroll - MODULE_CELL_HEIGHT - 2.0f) / rowHeight));
        int lastVisibleRow = Math.min(rows - 1, (int) Math.ceil((gridContentHeight - 2.0f - animatedScroll) / rowHeight));
        int startIndex = Math.max(0, firstVisibleRow * MODULES_PER_ROW);
        int endIndex = Math.min(modules.size(), (lastVisibleRow + 1) * MODULES_PER_ROW);

        for (int i = startIndex; i < endIndex; i++) {
            Module module = modules.get(i);
            int col = i % MODULES_PER_ROW;
            int row = i / MODULES_PER_ROW;

            float cellX = gridX + col * (MODULE_CELL_WIDTH + MODULE_GAP_X);
            float cellY = contentTop + 2 + row * (MODULE_CELL_HEIGHT + MODULE_GAP_Y) + animatedScroll;

            if (cellY + MODULE_CELL_HEIGHT < contentTop || cellY > contentTop + gridContentHeight) continue;

            boolean hovered = RenderUtility.isInRegion(mouseX, mouseY, cellX, cellY, MODULE_CELL_WIDTH, MODULE_CELL_HEIGHT);
            boolean enabled = module.isState();
            boolean isSettings = selectedModule == module;
            boolean isBinding = bindingModule == module;

            Float toggleAnim = moduleToggleAnims.get(module.getName());
            float toggleGlow = toggleAnim != null ? toggleAnim : 0;
            float enabledAnim = enabledAnims.getOrDefault(module.getName(), enabled ? 1.0f : 0.0f);

            float bindAnim = bindAnims.getOrDefault(module.getName(), 0.0f);
            float bindTarget = isBinding ? 1.0f : 0.0f;
            bindAnim = AnimationMath.fast(bindAnim, bindTarget, 10);
            bindAnims.put(module.getName(), bindAnim);

            int baseBg = ColorUtils.rgba(0, 0, 0, (int)(30 * alpha));
            int enabledBg = ColorUtils.setAlpha(themeColor, (int)(15 * alpha));
            int bgColor = ColorUtils.interpolateColor(baseBg, enabledBg, enabledAnim);

            if (isSettings) {
                bgColor = ColorUtils.setAlpha(themeColor, (int)((18 + 8 * enabledAnim) * alpha));
            } else if (hovered) {
                bgColor = ColorUtils.interpolateColor(
                        ColorUtils.rgba(255, 255, 255, (int)(12 * alpha)),
                        ColorUtils.setAlpha(themeColor, (int)(20 * alpha)),
                        enabledAnim
                );
            }
            RenderUtility.drawRoundedRect(cellX, cellY, MODULE_CELL_WIDTH, MODULE_CELL_HEIGHT, 3.5f, bgColor);

            if (enabledAnim > 0.01f) {
                RenderUtility.drawRoundedRectOutline(cellX, cellY, MODULE_CELL_WIDTH, MODULE_CELL_HEIGHT, 3.5f, 0.5f,
                        ColorUtils.setAlpha(themeColor, (int)(20 * enabledAnim * alpha)));
            }

            if (toggleGlow > 0) {
                float glowSize = 2.0f * toggleGlow;
                RenderUtility.drawShadow(
                        cellX - glowSize, cellY - glowSize,
                        MODULE_CELL_WIDTH + glowSize * 2, MODULE_CELL_HEIGHT + glowSize * 2,
                        8,
                        ColorUtils.setAlpha(themeColor, (int)(25 * toggleGlow * alpha))
                );
            }

            String catIcon = module.getCategory().getIcon();
            float iconScale = 0.9f;
            int iconFontSize = 24;
            float iconRawH = ClientFonts.keybind[iconFontSize].getFontHeight();
            float iconH = iconRawH * iconScale;
            float iconX = cellX + 4;
            float iconY = cellY + (MODULE_CELL_HEIGHT - iconH) / 0.8f;

            int catIconColor = ColorUtils.setAlpha(themeColor, (int)((80 + 175 * enabledAnim) * alpha));

            if (enabledAnim > 0.05f) {
                float iconW = iconH;
                float glowBright = 40 + 40 * enabledAnim;
                float glowDiffuse = 15 + 20 * enabledAnim;
                RenderUtility.drawShadow(iconX - 0.1f, iconY - 4, iconW + 0.1f, iconH + 0.1f, 10, ColorUtils.setAlpha(themeColor, (int)(glowBright * alpha)));
                RenderUtility.drawShadow(iconX + 2, iconY - 2, iconW + 0.1f, iconH + 0.1f, 10, ColorUtils.setAlpha(themeColor, (int)(glowDiffuse * alpha)));
            }

            GL11.glPushMatrix();
            GL11.glTranslatef(iconX, iconY, 0);
            GL11.glScalef(iconScale, iconScale, 1.0f);
            ClientFonts.keybind[iconFontSize].drawString(stack, catIcon, 0, 0, catIconColor);
            GL11.glPopMatrix();

            float sepX = cellX + 17;
            float sepH = MODULE_CELL_HEIGHT * 0.45f;
            float sepY = cellY + (MODULE_CELL_HEIGHT - sepH) / 2.0f;
            int sepColor = ColorUtils.rgba(255, 255, 255, (int)(30 * alpha));
            RenderUtility.drawRectW(sepX, sepY, 0.5f, sepH, sepColor);

            float fontSize = 6.0f;
            int offColor = ColorUtils.rgba(255, 255, 255, (int)(100 * alpha));
            int onColor = ColorUtils.rgba(255, 255, 255, (int)(220 * alpha));
            int nameColor = ColorUtils.interpolateColor(offColor, onColor, enabledAnim);

            float textX = cellX + 20;
            float textY = cellY + MODULE_CELL_HEIGHT / 2.0f - fontSize / 2.0f;
            Fonts.sfuy.drawText(stack, module.getName(), textX, textY, nameColor, fontSize);

            int bind = module.getBind();
            String bindText;
            if (isBinding) {
                long blinkMs = System.currentTimeMillis() % 1000;
                String dots = blinkMs < 333 ? "." : blinkMs < 666 ? ".." : "...";
                bindText = "Bind" + dots;
            } else {
                bindText = bind != 0 ? KeyStorage.getKey(bind) : "";
            }

            if (!bindText.isEmpty()) {
                float bindFontSize = 5.0f;
                float bindTextW = Fonts.sfuy.getWidth(bindText, bindFontSize);
                float badgePadX = 3.5f;
                float badgePadY = 1.5f;
                float badgeW = bindTextW + badgePadX * 2;
                float badgeH = bindFontSize + badgePadY * 2 + 1;
                float badgeX = cellX + MODULE_CELL_WIDTH - badgeW - 3;
                float badgeY = cellY + MODULE_CELL_HEIGHT / 2.0f - badgeH / 2.0f;
                float badgeR = 2.5f;

                int badgeBg = ColorUtils.rgba(0, 0, 0, (int)(60 * alpha));
                RenderUtility.drawRoundedRect(badgeX, badgeY, badgeW, badgeH, badgeR, badgeBg);

                float breathe = (float)(Math.sin(time * 2.5 + i * 0.4) * 0.5 + 0.5);
                int outAlpha = (int)(alpha * (30 + 30 * breathe + 60 * bindAnim));
                float outWidth = 0.5f + 0.3f * breathe + 0.5f * bindAnim;
                RenderUtility.drawRoundedRectOutline(badgeX, badgeY, badgeW, badgeH, badgeR, outWidth,
                        ColorUtils.setAlpha(themeColor, outAlpha));

                int bindColor;
                if (isBinding) {
                    float flash = (float)(Math.sin(time * 4.0) * 0.5 + 0.5);
                    bindColor = ColorUtils.setAlpha(themeColor, (int)(alpha * (150 + 105 * flash)));
                } else {
                    bindColor = ColorUtils.rgba(255, 255, 255, (int)(180 * alpha));
                }
                Fonts.sfuy.drawText(stack, bindText, badgeX + badgePadX, badgeY + badgePadY + 1, bindColor, bindFontSize);
            }
        }

        Stencil.uninitStencilBuffer();

        if (maxScroll > 0.0f) {
            float trackX = gridX + gridWidth - 3.2f;
            float trackY = contentTop + 2.0f;
            float trackW = 2.2f;
            float trackH = gridContentHeight - 4.0f;
            float thumbH = Math.max(14.0f, (trackH / Math.max(1.0f, totalHeight)) * trackH);
            float thumbRange = Math.max(0.0f, trackH - thumbH);
            float progress = MathHelper.clamp(-animatedScroll / maxScroll, 0.0f, 1.0f);
            float thumbY = trackY + thumbRange * progress;
            boolean thumbHover = RenderUtility.isInRegion(mouseX, mouseY, trackX - 1.5f, thumbY, trackW + 3.0f, thumbH);

            RenderUtility.drawRoundedRect(trackX, trackY, trackW, trackH, 1.2f, ColorUtils.rgba(255, 255, 255, (int)(22 * alpha)));
            RenderUtility.drawRoundedRect(trackX, thumbY, trackW, thumbH, 1.2f,
                    ColorUtils.setAlpha(themeColor, (int)(((moduleScrollDragging || thumbHover) ? 150 : 95) * alpha)));
        }

    }

    private void renderSettingsPanel(MatrixStack stack, float panelX, float contentTop, float contentHeight, int mouseX, int mouseY, float alpha) {
        float settingsX = panelX + LEFT_PANEL_WIDTH + 2;
        float settingsWidth = SETTINGS_PANEL_WIDTH;

        RenderUtility.drawRoundedRect(settingsX, contentTop, settingsWidth, contentHeight, 5.0f, ColorUtils.rgba(0, 0, 0, (int)(40 * alpha)));

        float sa = settingsAlpha;

        Stencil.initStencilToWrite();
        RenderUtility.drawRoundedRect(settingsX, contentTop, settingsWidth, contentHeight, 5.0f, -1);
        Stencil.readStencilBuffer(1);

        if (selectedModule == null) {
            float emptyAlpha = Math.max(0, 1.0f - sa);
            if (emptyAlpha > 0.01f) {
                float cx = settingsX + settingsWidth / 2.0f;
                float cy = contentTop + contentHeight / 2.0f;

                Fonts.sfuy.drawCenteredText(stack, "\u0422\u0443\u0442 \u043D\u0438\u0447\u0435\u0433\u043E \u043D\u0435\u0442", cx, cy - 10, ColorUtils.rgba(255, 255, 255, (int)(80 * alpha * emptyAlpha)), 6.0f);

                float shakeX = (float) (Math.sin(System.currentTimeMillis() * 0.02) * 1.5);
                float shakeY = (float) (Math.cos(System.currentTimeMillis() * 0.025) * 1.0);

                Fonts.sfuy.drawCenteredText(stack, ">_<", cx + shakeX, cy + 2 + shakeY, ColorUtils.rgba(255, 255, 255, (int)(60 * alpha * emptyAlpha)), 7.0f);
            }
            Stencil.uninitStencilBuffer();
            return;
        }

        float slideX = 0;
        float slideY = 0;
        if (settingsTransitioning && settingsSwapping) {
            slideX = -(1.0f - sa) * 20.0f;
        } else if (settingsTransitioning && !settingsSwapping) {
            slideY = (1.0f - sa) * 10.0f;
        } else if (!settingsTransitioning && sa < 0.95f && settingsSwapping) {
            slideX = (1.0f - sa) * 20.0f;
        } else {
            slideY = (1.0f - sa) * 6.0f;
        }

        Fonts.sfuy.drawText(stack, selectedModule.getName(), settingsX + 6 + slideX, contentTop + 4 + slideY, ColorUtils.rgba(255, 255, 255, (int)(200 * alpha * sa)), 6.5f);

        RenderUtility.drawRectW(settingsX + 6, contentTop + 14 + slideY, settingsWidth - 12, 0.5f, ColorUtils.rgba(255, 255, 255, (int)(20 * alpha * sa)));

        float settingContentTop = contentTop + 16;
        float settingContentHeight = contentHeight - 16;

        // --- Sticky components: calculate total height of sticky components ---
        float stickyHeight = 0;
        for (int ci = 0; ci < settingsComponents.size(); ci++) {
            Component comp = settingsComponents.get(ci);
            if (comp.isVisible() && comp.isSticky()) {
                float progress = getCollapseProgressForComponent(ci);
                stickyHeight += comp.getHeight() * (1.0f - progress);
            }
        }

        // --- Render sticky components OUTSIDE the scroll area (pinned at top) ---
        float stickyY = settingContentTop + 2 + slideY;
        for (int ci = 0; ci < settingsComponents.size(); ci++) {
            Component comp = settingsComponents.get(ci);
            if (!comp.isVisible() || !comp.isSticky()) continue;
            float progress = getCollapseProgressForComponent(ci);
            if (progress >= 0.99f) continue;

            comp.setX(settingsX + 4 + slideX);
            comp.setY(stickyY);
            comp.setWidth(settingsWidth - 8);

            if (stickyY + comp.getHeight() > settingContentTop && stickyY < settingContentTop + settingContentHeight) {
                comp.render(stack, mouseX, mouseY);
            }

            stickyY += comp.getHeight() * (1.0f - progress);
        }

        // --- Adjusted content area for scrollable components (below sticky) ---
        // Небольшой зазор чтобы скроллируемый контент не залезал под sticky
        // при прокрутке вверх (покрывает отступ +2 и даёт буфер).
        float STICKY_SEPARATOR_GAP = 4.0f;
        float scrollContentTop = settingContentTop + stickyHeight + STICKY_SEPARATOR_GAP;
        float scrollContentHeight = settingContentHeight - stickyHeight - STICKY_SEPARATOR_GAP;
        if (scrollContentHeight < 0) scrollContentHeight = 0;

        float totalSettingsHeight = 0;
        for (int ci = 0; ci < settingsComponents.size(); ci++) {
            Component comp = settingsComponents.get(ci);
            if (comp.isVisible() && !comp.isSticky()) {
                float progress = getCollapseProgressForComponent(ci);
                totalSettingsHeight += comp.getHeight() * (1.0f - progress);
            }
        }

        if (totalSettingsHeight == 0 && stickyHeight == 0) {
            float cx = settingsX + settingsWidth / 2.0f;
            float cy = contentTop + contentHeight / 2.0f;

            Fonts.sfuy.drawCenteredText(stack, "\u0422\u0443\u0442 \u043D\u0438\u0447\u0435\u0433\u043E \u043D\u0435\u0442", cx + slideX, cy - 10 + slideY, ColorUtils.rgba(255, 255, 255, (int)(80 * alpha * sa)), 6.0f);

            float shakeX = (float) (Math.sin(System.currentTimeMillis() * 0.02) * 1.5);
            float shakeY2 = (float) (Math.cos(System.currentTimeMillis() * 0.025) * 1.0);

            Fonts.sfuy.drawCenteredText(stack, ">_<", cx + shakeX + slideX, cy + 2 + shakeY2 + slideY, ColorUtils.rgba(255, 255, 255, (int)(60 * alpha * sa)), 7.0f);
            Stencil.uninitStencilBuffer();
            return;
        }

        float maxSettScroll = Math.max(0, totalSettingsHeight - scrollContentHeight + 4);
        settingsScroll = MathHelper.clamp(settingsScroll, -maxSettScroll, 0);
        settingsAnimatedScroll = AnimationMath.fast(settingsAnimatedScroll, settingsScroll, 15);

        if (totalSettingsHeight > 0) {
            Scissor.push();
            Scissor.setFromComponentCoordinates(settingsX + 2.0f, scrollContentTop, settingsWidth - 4.0f, scrollContentHeight);

            float cy = scrollContentTop + 2 + settingsAnimatedScroll + slideY;
            for (int ci = 0; ci < settingsComponents.size(); ci++) {
                Component comp = settingsComponents.get(ci);
                if (!comp.isVisible() || comp.isSticky()) continue;

                float progress = getCollapseProgressForComponent(ci);
                if (progress >= 0.99f) continue;

                comp.setX(settingsX + 4 + slideX);
                comp.setY(cy);
                comp.setWidth(settingsWidth - 8);

                if (cy + comp.getHeight() > scrollContentTop && cy < scrollContentTop + scrollContentHeight) {
                    comp.render(stack, mouseX, mouseY);
                }

                cy += comp.getHeight() * (1.0f - progress);
            }

            Scissor.pop();
        }

        Stencil.uninitStencilBuffer();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float x = panelPosX;
        float y = panelPosY;

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            boolean inPanel = RenderUtility.isInRegion(mouseX, mouseY, x, y, PANEL_WIDTH, PANEL_HEIGHT);
            float topBarW = Math.min(PANEL_WIDTH, TOP_BAR_WIDTH);
            float topBarX = getStaticTopTabBarX();
            boolean inTabs = RenderUtility.isInRegion(mouseX, mouseY, topBarX, 4, topBarW, 18.0f);
            if (inPanel || inTabs) {
                clickRipples.add(new Ripple((float) mouseX, (float) mouseY));
            }
        }

        if (selectedTab == TAB_MAIN) {
            if (dispatchOpenedColorPickerClick((float) mouseX, (float) mouseY, button)) {
                return true;
            }
            if (MultiBoxOverlayManager.mouseClick((float) mouseX, (float) mouseY, button)) {
                return true;
            }
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            float topBarW = Math.min(PANEL_WIDTH, TOP_BAR_WIDTH);
            float topBarX = getStaticTopTabBarX();
            float tabSpacing = TOP_TAB_SPACING;
            float totalWidth = (TAB_COUNT - 1) * tabSpacing;
            float startX = topBarX + (topBarW - totalWidth) / 2.0f;
            float pillW = TOP_TAB_PILL_WIDTH;
            float pillH = TOP_TAB_PILL_HEIGHT;
            float barH = 18.0f;
            float barY = 4;
            float pillYPos = barY + (barH - pillH) / 2.0f + TOP_TAB_PILL_OFFSET_Y;

            for (int i = 0; i < TAB_COUNT; i++) {
                float iconX = getTopTabCenterX(startX, tabSpacing, i);
                if (RenderUtility.isInRegion(mouseX, mouseY, iconX - pillW / 2f, pillYPos, pillW, pillH)) {
                    if (selectedTab != i) {
                        // Если кликнули на Main и стиль MiniDropDown — возвращаем в MiniDropDown
                        if (i == TAB_MAIN && ClickGui.guiStyle.get().equals("MiniDropDown")) {
                            SoundUtil.playSound("switchcategory.wav");
                            Harmony.getInstance().openMiniDropDown();
                            return true;
                        }
                        SoundUtil.playSound("switchcategory.wav");
                        startTabSwitchTransition(i);
                        setModuleSearchFocused(false);
                        closeSettingsOverlays();
                        autoBuyScrollDragging = false;
                        configScrollDragging = false;
                        resetConfigDrag();
                        configTagEditingName = null;
                        moduleScrollDragging = false;
                        blurAutoBuyFieldFocus();
                        if (i == TAB_CONFIGS || i == TAB_BOTCONFIGS) {
                            configsLoaded = false;
                            cachedConfigs = null;
                        }
                    }
                    return true;
                }
            }
        }

        if (tabSwitchTransitioning) {
            return true;
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && isPanelHeaderDragArea(mouseX, mouseY, x, y)) {
            panelDragging = true;
            panelDragOffsetX = (float) mouseX - panelPosX;
            panelDragOffsetY = (float) mouseY - panelPosY;
            return true;
        }

        if (isConfigTab() && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return handleConfigsClick(mouseX, mouseY, x, y) || super.mouseClicked(mouseX, mouseY, button);
        }

        if (selectedTab == TAB_THEME) {
            return handleThemeClick(mouseX, mouseY, button, x, y) || super.mouseClicked(mouseX, mouseY, button);
        }

        if (selectedTab == TAB_AUTOBUY) {
            return handleAutoBuyClick(mouseX, mouseY, button, x, y) || super.mouseClicked(mouseX, mouseY, button);
        }

        if (selectedTab != TAB_MAIN) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        float barY = y + HEADER_HEIGHT;
        CategoryBarLayout categoryLayout = createCategoryBarLayout(x, barY);
        for (int i = 0; i < categoryLayout.categories.length; i++) {
            float hitboxX = categoryLayout.tabXPositions[i] - CATEGORY_HITBOX_PADDING_X;
            float hitboxW = categoryLayout.tabWidths[i] + CATEGORY_HITBOX_PADDING_X * 2.0f;

            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && RenderUtility.isInRegion(mouseX, mouseY, hitboxX, categoryLayout.barRectY, hitboxW, categoryLayout.barRectH)) {
                setModuleSearchFocused(false);
                if (categoryLayout.categories[i] != selectedCategory) {
                    SoundUtil.playSound("switchcategory.wav");
                    selectedCategory = categoryLayout.categories[i];
                    invalidateVisibleModules();
                    scroll = 0;
                    animatedScroll = 0;
                    if (selectedModule != null) {
                        settingsTransitioning = true;
                        settingsSwapping = false;
                        pendingModule = null;
                    }
                }
                return true;
            }
        }

        float contentTop = y + HEADER_HEIGHT + CATEGORY_BAR_HEIGHT + 2;
        float contentBottom = y + PANEL_HEIGHT - 4;
        float contentHeight = contentBottom - contentTop;
        float gridX = x + 8;
        float gridWidth = LEFT_PANEL_WIDTH - 12;
        float gridContentHeight = getMainGridContentHeight(contentHeight);
        List<Module> modules = getVisibleModules();

        int rows = (int) Math.ceil((double) modules.size() / MODULES_PER_ROW);
        float totalHeight = rows * (MODULE_CELL_HEIGHT + MODULE_GAP_Y);
        float maxScroll = Math.max(0, totalHeight - gridContentHeight + 4);

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && handleModuleSearchClick(mouseX, mouseY)) {
            return true;
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && maxScroll > 0.0f) {
            float trackX = gridX + gridWidth - 3.2f;
            float trackY = contentTop + 2.0f;
            float trackW = 2.2f;
            float trackH = gridContentHeight - 4.0f;
            float thumbH = Math.max(14.0f, (trackH / Math.max(1.0f, totalHeight)) * trackH);
            float thumbRange = Math.max(0.0f, trackH - thumbH);
            float progress = MathHelper.clamp(-animatedScroll / maxScroll, 0.0f, 1.0f);
            float thumbY = trackY + thumbRange * progress;

            if (RenderUtility.isInRegion(mouseX, mouseY, trackX - 1.5f, trackY, trackW + 3.0f, trackH)) {
                moduleScrollDragging = true;
                if (RenderUtility.isInRegion(mouseX, mouseY, trackX - 1.5f, thumbY, trackW + 3.0f, thumbH)) {
                    moduleScrollDragOffset = (float) (mouseY - thumbY);
                } else {
                    moduleScrollDragOffset = thumbH / 2.0f;
                    updateModuleScrollFromDrag(mouseY, y);
                }
                return true;
            }
        } else if (maxScroll <= 0.0f) {
            moduleScrollDragging = false;
        }

        if (selectedModule != null) {
            float settingsXPos = x + LEFT_PANEL_WIDTH + 2;
            float settingsWidth = SETTINGS_PANEL_WIDTH;
            float settingContentTop = contentTop + 16;
            float settingContentHeight = contentHeight - 16;

            if (RenderUtility.isInRegion(mouseX, mouseY, settingsXPos, settingContentTop, settingsWidth, settingContentHeight)) {
                for (int ci = 0; ci < settingsComponents.size(); ci++) {
                    Component comp = settingsComponents.get(ci);
                    if (comp.isVisible() && !isHiddenByCollapsedCategory(ci) && comp.isMouseOverComponent((float) mouseX, (float) mouseY)) {
                        comp.mouseClick((float) mouseX, (float) mouseY, button);
                        if (comp instanceof BindComponent bindComponent && bindComponent.isActivated()) {
                            setModuleSearchFocused(false);
                        }
                    }
                }
                return true;
            }
        }

        for (int i = 0; i < modules.size(); i++) {
            Module module = modules.get(i);
            int col = i % MODULES_PER_ROW;
            int row = i / MODULES_PER_ROW;

            float cellX = gridX + col * (MODULE_CELL_WIDTH + MODULE_GAP_X);
            float cellY = contentTop + 2 + row * (MODULE_CELL_HEIGHT + MODULE_GAP_Y) + animatedScroll;

            if (cellY + MODULE_CELL_HEIGHT >= contentTop && cellY <= contentTop + gridContentHeight) {
                if (RenderUtility.isInRegion(mouseX, mouseY, cellX, cellY, MODULE_CELL_WIDTH, MODULE_CELL_HEIGHT)) {
                    if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                        module.toggle();
                        SoundUtil.playSound(module.isState() ? "moduleopen.wav" : "moduleclose.wav");
                        moduleToggleAnims.put(module.getName(), 1.0f);
                        moduleToggleStates.put(module.getName(), module.isState());
                    } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                        if (selectedModule == module) {
                            SoundUtil.playSound("closemodescreen.wav");
                            settingsTransitioning = true;
                            settingsSwapping = false;
                            pendingModule = null;
                        } else {
                            SoundUtil.playSound("openmodescreen.wav");
                            saveCollapsedStates();
                            if (selectedModule == null) {
                                selectedModule = module;
                                settingsSwapping = true;
                                rebuildSettingsComponents();
                                settingsScroll = 0;
                                settingsAnimatedScroll = 0;
                            } else {
                                settingsTransitioning = true;
                                settingsSwapping = true;
                                pendingModule = module;
                            }
                        }
                    } else if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
                        if (bindingModule == module) {
                            bindingModule = null;
                        } else {
                            bindingModule = module;
                            setModuleSearchFocused(false);
                        }
                    }
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && isConfigTab() && configCardDragging) {
            finishConfigDrag(mouseX, mouseY);
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            autoBuyScrollDragging = false;
            configScrollDragging = false;
            configTextSelectionDragging = false;
            moduleScrollDragging = false;
            moduleSearchSelectionDragging = false;
            panelDragging = false;
        }

        if (selectedTab == TAB_MAIN) {
            if (dispatchOpenedColorPickerRelease((float) mouseX, (float) mouseY, button)) {
                return true;
            }
            if (MultiBoxOverlayManager.mouseRelease((float) mouseX, (float) mouseY, button)) {
                return true;
            }
        }

        if (selectedTab == TAB_THEME && dispatchThemeCustomPickerRelease((float) mouseX, (float) mouseY, button, panelPosX, panelPosY)) {
            return true;
        }

        for (int ci = 0; ci < settingsComponents.size(); ci++) {
            Component comp = settingsComponents.get(ci);
            if (comp.isVisible() && !isHiddenByCollapsedCategory(ci)) {
                comp.mouseRelease((float) mouseX, (float) mouseY, button);
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (tabSwitchTransitioning) {
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (panelDragging) {
                updatePanelPositionFromDrag(mouseX, mouseY);
                return true;
            }
            if (selectedTab == TAB_AUTOBUY && autoBuyScrollDragging) {
                updateAutoBuyScrollFromDrag(mouseY, panelPosY);
                return true;
            }
            if (isConfigTab() && configScrollDragging) {
                updateConfigScrollFromDrag(mouseY, panelPosY);
                return true;
            }
            if (isConfigTab() && configTextSelectionDragging) {
                updateConfigTextSelectionFromMouse((float) mouseX);
                return true;
            }
            if (isConfigTab() && configCardDragging) {
                configDragMouseY = (float) mouseY;
                if (Math.abs(configDragMouseY - configDragStartMouseY) > 3.0f || Math.abs(dragY) > 1.0) {
                    configDragMoved = true;
                    updateConfigDragSwap(mouseX, mouseY);
                }
                return true;
            }
            if (selectedTab == TAB_MAIN && moduleScrollDragging) {
                updateModuleScrollFromDrag(mouseY, panelPosY);
                return true;
            }
            if (selectedTab == TAB_MAIN && moduleSearchField != null && moduleSearchField.isFocused() && moduleSearchSelectionDragging) {
                updateModuleSearchSelectionFromMouse(mouseX);
                return true;
            }
            if (selectedTab == TAB_THEME && dispatchThemeCustomPickerDrag((float) mouseX, (float) mouseY, button, panelPosX, panelPosY)) {
                return true;
            }
            if (selectedTab == TAB_MAIN) {
                ColorComponent opened = ColorComponent.getOpened();
                if (opened != null && opened.isPanelOpened()) {
                    opened.handleClick((int) mouseX, (int) mouseY);
                }
                if (opened != null && opened.isDraggingFloating()) {
                    return true;
                }
                MultiBoxComponent openedMulti = MultiBoxComponent.getCurrentlyOpen();
                if (openedMulti != null && openedMulti.isDropdownOpen()) {
                    return true;
                }
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }



    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (tabSwitchTransitioning) {
            return true;
        }
        float x = panelPosX;
        float y = panelPosY;

        if (isConfigTab()) {
            configScroll += (float)(delta * 10);
            return true;
        }

        if (selectedTab == TAB_AUTOBUY) {
            xd.harm.modules.impl.player.AutoBuy autoBuy = Harmony.getInstance().getModuleManager().getAutoBuy();
            List<xd.harm.modules.impl.player.autobuy.AutoBuyItem> items = autoBuy.getManager().getItems();

            float contentTop = y + HEADER_HEIGHT + 2;
            float contentBottom = y + PANEL_HEIGHT - 4;
            float contentHeight = contentBottom - contentTop;
            float pad = 6;
            float toggleH = 14;
            float toggleY = contentTop + 2;
            float gridTop = toggleY + toggleH + 4;
            float gridBottom = contentTop + contentHeight;
            float leftW = 180;
            if (RenderUtility.isInRegion(mouseX, mouseY, x + pad, gridTop, leftW, gridBottom - gridTop)) {
                int rows = (int) Math.ceil(items.size() / (float) AB_COLS);
                float totalGridH = rows * (AB_ITEM_SIZE + AB_ITEM_GAP);
                float gridH = gridBottom - gridTop;
                float maxScr = Math.max(0, totalGridH - gridH + 6);
                autoBuyScroll = MathHelper.clamp(autoBuyScroll + (float) (delta * 22), -maxScr, 0);
                return true;
            }
            return false;
        }

        if (selectedTab == TAB_THEME) {
            float contentTop = y + HEADER_HEIGHT + 2;
            float contentBottom = y + PANEL_HEIGHT - 4;
            float contentHeight = contentBottom - contentTop;
            if (RenderUtility.isInRegion(mouseX, mouseY, x + 8, contentTop, PANEL_WIDTH - 16, contentHeight)) {
                themePanelScroll += (float) (delta * 10);
                return true;
            }
            return super.mouseScrolled(mouseX, mouseY, delta);
        }

        if (selectedTab != TAB_MAIN) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }

        float contentTop = y + HEADER_HEIGHT + CATEGORY_BAR_HEIGHT + 2;
        float contentHeight = y + PANEL_HEIGHT - 4 - contentTop;
        float gridContentHeight = getMainGridContentHeight(contentHeight);

        if (selectedModule != null) {
            float settingsXPos = x + LEFT_PANEL_WIDTH + 2;
            float settingContentTop = contentTop + 16;
            float settingContentHeight = contentHeight - 16;
            if (RenderUtility.isInRegion(mouseX, mouseY, settingsXPos, settingContentTop, SETTINGS_PANEL_WIDTH, settingContentHeight)) {
                settingsScroll += (float) (delta * 10);
                return true;
            }
        }

        float gridX = x + 8;
        float gridWidth = LEFT_PANEL_WIDTH - 12;
        if (RenderUtility.isInRegion(mouseX, mouseY, gridX, contentTop, gridWidth, gridContentHeight)) {
            scroll += (float) (delta * 10);
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private boolean isPanelHeaderDragArea(double mouseX, double mouseY, float panelX, float panelY) {
        return RenderUtility.isInRegion(mouseX, mouseY, panelX, panelY, PANEL_WIDTH, HEADER_HEIGHT)
                && !isTopTabBarArea(mouseX, mouseY, panelX);
    }

    private boolean isTopTabBarArea(double mouseX, double mouseY, float panelX) {
        float topBarW = Math.min(PANEL_WIDTH, TOP_BAR_WIDTH);
        float topBarX = getStaticTopTabBarX();
        return RenderUtility.isInRegion(mouseX, mouseY, topBarX, 4, topBarW, 18.0f);
    }

    private float getStaticTopTabBarX() {
        float topBarW = Math.min(PANEL_WIDTH, TOP_BAR_WIDTH);
        return (width - topBarW) / 2.0f;
    }

    private void updatePanelPositionFromDrag(double mouseX, double mouseY) {
        float minX = 4.0f;
        float minY = 24.0f;
        float maxX = Math.max(minX, width - PANEL_WIDTH - 4.0f);
        float maxY = Math.max(minY, height - PANEL_HEIGHT - 4.0f);

        panelPosX = MathHelper.clamp((float) mouseX - panelDragOffsetX, minX, maxX);
        panelPosY = MathHelper.clamp((float) mouseY - panelDragOffsetY, minY, maxY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (isConfigTab() && configTagEditingName != null) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_ESCAPE) {
                configTagInputText = normalizeConfigTagText(configTagInputText);
                configMetaStore.setTags(configTagEditingName, configTagInputText);
                configTagEditingName = null;
                resetConfigKeyRepeat();
            } else {
                handleConfigTextKeyPressed(keyCode, modifiers);
            }
            return true;
        }
        if (isConfigTab() && configInputFocused) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                configInputFocused = false;
                resetConfigKeyRepeat();
            } else {
                handleConfigTextKeyPressed(keyCode, modifiers);
            }
            return true;
        }

        if (selectedTab == TAB_AUTOBUY && handleAutoBuyKeyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }

        if (selectedTab == TAB_THEME && isMenuPanelCustomThemeSelected()) {
            boolean wasFocused = themeCustomPicker.isFocused();
            themeCustomPicker.keyPressed(keyCode, scanCode, modifiers);
            if (wasFocused || themeCustomPicker.isFocused()) {
                return true;
            }
        }

        if (bindingModule != null) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                bindingModule.setBind(0);
            } else if (keyCode != GLFW.GLFW_KEY_LEFT_SHIFT && keyCode != GLFW.GLFW_KEY_RIGHT_SHIFT
                    && keyCode != GLFW.GLFW_KEY_LEFT_CONTROL && keyCode != GLFW.GLFW_KEY_RIGHT_CONTROL
                    && keyCode != GLFW.GLFW_KEY_LEFT_ALT && keyCode != GLFW.GLFW_KEY_RIGHT_ALT) {
                bindingModule.setBind(keyCode);
            }
            bindingModule = null;
            suppressNextBindCharTyped();
            return true;
        }

        if (selectedTab == TAB_MAIN && dispatchActiveBindComponentKeyPress(keyCode, scanCode, modifiers)) {
            return true;
        }

        if (selectedTab == TAB_MAIN && handleModuleSearchKeyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (!closeSoundPlayed) {
                SoundUtil.playSound("dropdowndis.wav");
                closeSoundPlayed = true;
            }
            setModuleSearchFocused(false);
            closeSettingsOverlays();
            closing = true;
            openAnimation.animate(0, 0.3, Easings.EXPO_IN);
            return true;
        }

        for (int ci = 0; ci < settingsComponents.size(); ci++) {
            Component comp = settingsComponents.get(ci);
            if (comp.isVisible() && !isHiddenByCollapsedCategory(ci)) {
                comp.keyPressed(keyCode, scanCode, modifiers);
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (shouldSuppressBindCharTyped()) {
            return true;
        }
        if (isConfigTab() && configTagEditingName != null) {
            insertConfigText(String.valueOf(codePoint));
            return true;
        }
        if (isConfigTab() && configInputFocused) {
            insertConfigText(String.valueOf(codePoint));
            return true;
        }
        if (selectedTab == TAB_AUTOBUY && handleAutoBuyCharTyped(codePoint, modifiers)) {
            return true;
        }
        if (selectedTab == TAB_THEME && isMenuPanelCustomThemeSelected() && themeCustomPicker.isFocused()) {
            themeCustomPicker.charTyped(codePoint, modifiers);
            return true;
        }
        if (selectedTab == TAB_MAIN && handleModuleSearchCharTyped(codePoint, modifiers)) {
            return true;
        }
        for (int ci = 0; ci < settingsComponents.size(); ci++) {
            Component comp = settingsComponents.get(ci);
            if (comp.isVisible() && !isHiddenByCollapsedCategory(ci)) {
                comp.charTyped(codePoint, modifiers);
            }
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void onClose() {
        if (!closeSoundPlayed) {
            SoundUtil.playSound("dropdowndis.wav");
            closeSoundPlayed = true;
        }
        closeSettingsOverlays();
        setModuleSearchFocused(false);
        panelDragging = false;
        themeCustomPicker.setFocused(false);
        themeCustomPicker.mouseRelease(-1, -1, GLFW.GLFW_MOUSE_BUTTON_LEFT);
        resetAutoBuyEditors();
        super.onClose();
    }

    private boolean isConfigTextEditing() {
        return isConfigTab() && (configInputFocused || configTagEditingName != null);
    }

    private boolean dispatchActiveBindComponentKeyPress(int keyCode, int scanCode, int modifiers) {
        for (int ci = 0; ci < settingsComponents.size(); ci++) {
            Component comp = settingsComponents.get(ci);
            if (comp.isVisible() && !isHiddenByCollapsedCategory(ci) && comp instanceof BindComponent bindComponent && bindComponent.isActivated()) {
                bindComponent.keyPressed(keyCode, scanCode, modifiers);
                suppressNextBindCharTyped();
                return true;
            }
        }
        return false;
    }

    private void suppressNextBindCharTyped() {
        suppressBindCharTypedUntil = System.currentTimeMillis() + 120L;
    }

    private boolean shouldSuppressBindCharTyped() {
        long now = System.currentTimeMillis();
        if (suppressBindCharTypedUntil <= 0L) {
            return false;
        }
        if (now <= suppressBindCharTypedUntil) {
            suppressBindCharTypedUntil = 0L;
            return true;
        }
        suppressBindCharTypedUntil = 0L;
        return false;
    }

    private boolean isConfigTagEditing() {
        return configTagEditingName != null;
    }

    private int getConfigTextMaxLength() {
        return isConfigTagEditing() ? 80 : 4096;
    }

    private String getConfigText() {
        return isConfigTagEditing() ? configTagInputText : configInputText;
    }

    private int getConfigCursor() {
        return isConfigTagEditing() ? configTagCursor : configInputCursor;
    }

    private int getConfigSelection() {
        return isConfigTagEditing() ? configTagSelection : configInputSelection;
    }

    private void setConfigCursorValues(int cursor, int selection) {
        if (isConfigTagEditing()) {
            configTagCursor = MathHelper.clamp(cursor, 0, configTagInputText.length());
            configTagSelection = MathHelper.clamp(selection, 0, configTagInputText.length());
        } else {
            configInputCursor = MathHelper.clamp(cursor, 0, configInputText.length());
            configInputSelection = MathHelper.clamp(selection, 0, configInputText.length());
        }
    }

    private void setConfigText(String value, int cursor, int selection, int editDirection) {
        String next = value == null ? "" : value.replace('\n', ' ').replace('\r', ' ');
        int max = getConfigTextMaxLength();
        if (next.length() > max) {
            next = next.substring(0, max);
        }

        if (isConfigTagEditing()) {
            next = normalizeConfigTagText(next);
            configTagInputText = next;
            configTagEditPulse = 1.0f;
            configTagEditDirection = editDirection;
            configMetaStore.setTags(configTagEditingName, configTagInputText);
        } else {
            configInputText = next;
            configInputEditPulse = 1.0f;
            configInputEditDirection = editDirection;
        }

        setConfigCursorValues(cursor, selection);
        SoundUtil.playSound("searchtyping.wav");
    }

    private String normalizeConfigTagText(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        String result = value;
        while (result.startsWith("'") || result.startsWith("`") || result.startsWith("’")) {
            result = result.substring(1);
        }
        return result;
    }

    private boolean hasConfigSelection() {
        return getConfigCursor() != getConfigSelection();
    }

    private int getConfigSelectionStart() {
        return Math.min(getConfigCursor(), getConfigSelection());
    }

    private int getConfigSelectionEnd() {
        return Math.max(getConfigCursor(), getConfigSelection());
    }

    private void setConfigCursorPosition(int position, boolean keepSelection) {
        int cursor = MathHelper.clamp(position, 0, getConfigText().length());
        setConfigCursorValues(cursor, keepSelection ? getConfigSelection() : cursor);
    }

    private void insertConfigText(String value) {
        if (!isConfigTextEditing() || value == null || value.isEmpty()) {
            return;
        }

        String insert = value.replace('\n', ' ').replace('\r', ' ');
        StringBuilder filtered = new StringBuilder(insert.length());
        for (int i = 0; i < insert.length(); i++) {
            char c = insert.charAt(i);
            if (c >= ' ') {
                filtered.append(c);
            }
        }
        insert = filtered.toString();
        if (insert.isEmpty()) {
            return;
        }

        String text = getConfigText();
        int start = hasConfigSelection() ? getConfigSelectionStart() : getConfigCursor();
        int end = hasConfigSelection() ? getConfigSelectionEnd() : getConfigCursor();
        int available = getConfigTextMaxLength() - (text.length() - (end - start));
        if (available <= 0) {
            return;
        }
        if (insert.length() > available) {
            insert = insert.substring(0, available);
        }

        String next = text.substring(0, start) + insert + text.substring(end);
        int cursor = start + insert.length();
        setConfigText(next, cursor, cursor, 1);
    }

    private void deleteConfigSelection() {
        if (!hasConfigSelection()) {
            return;
        }
        String text = getConfigText();
        int start = getConfigSelectionStart();
        int end = getConfigSelectionEnd();
        setConfigText(text.substring(0, start) + text.substring(end), start, start, -1);
    }

    private int getConfigPreviousWordBoundary(int from) {
        String text = getConfigText();
        int pos = MathHelper.clamp(from, 0, text.length());
        if (pos <= 0) {
            return 0;
        }
        pos--;
        while (pos > 0 && !Character.isLetterOrDigit(text.charAt(pos)) && text.charAt(pos) != '_') {
            pos--;
        }
        while (pos > 0 && (Character.isLetterOrDigit(text.charAt(pos - 1)) || text.charAt(pos - 1) == '_')) {
            pos--;
        }
        return pos;
    }

    private int getConfigNextWordBoundary(int from) {
        String text = getConfigText();
        int pos = MathHelper.clamp(from, 0, text.length());
        while (pos < text.length() && !Character.isLetterOrDigit(text.charAt(pos)) && text.charAt(pos) != '_') {
            pos++;
        }
        while (pos < text.length() && (Character.isLetterOrDigit(text.charAt(pos)) || text.charAt(pos) == '_')) {
            pos++;
        }
        return pos;
    }

    private void deleteConfigPrevious(boolean byWord) {
        if (!isConfigTextEditing()) {
            return;
        }
        if (hasConfigSelection()) {
            deleteConfigSelection();
            return;
        }
        String text = getConfigText();
        int cursor = getConfigCursor();
        if (cursor <= 0) {
            return;
        }
        int start = byWord ? getConfigPreviousWordBoundary(cursor) : cursor - 1;
        setConfigText(text.substring(0, start) + text.substring(cursor), start, start, -1);
    }

    private void deleteConfigNext(boolean byWord) {
        if (!isConfigTextEditing()) {
            return;
        }
        if (hasConfigSelection()) {
            deleteConfigSelection();
            return;
        }
        String text = getConfigText();
        int cursor = getConfigCursor();
        if (cursor >= text.length()) {
            return;
        }
        int end = byWord ? getConfigNextWordBoundary(cursor) : cursor + 1;
        setConfigText(text.substring(0, cursor) + text.substring(end), cursor, cursor, -1);
    }

    private void handleConfigTextKeyPressed(int keyCode, int modifiers) {
        boolean ctrlDown = Screen.hasControlDown();
        boolean shiftDown = Screen.hasShiftDown();

        if (!isConfigTagEditing() && ctrlDown) {
            if (keyCode == GLFW.GLFW_KEY_S) {
                performConfigAction(0);
                return;
            }
            if (keyCode == GLFW.GLFW_KEY_O || keyCode == GLFW.GLFW_KEY_L) {
                performConfigAction(1);
                return;
            }
            if (keyCode == GLFW.GLFW_KEY_I) {
                performConfigAction(2);
                return;
            }
            if (keyCode == GLFW.GLFW_KEY_E) {
                performConfigAction(3);
                return;
            }
        }

        if (Screen.isSelectAll(keyCode)) {
            setConfigCursorValues(getConfigText().length(), 0);
            return;
        }
        if (Screen.isCopy(keyCode)) {
            String text = getConfigText();
            String copy = hasConfigSelection() ? text.substring(getConfigSelectionStart(), getConfigSelectionEnd()) : text;
            minecraft.keyboardListener.setClipboardString(copy);
            return;
        }
        if (Screen.isCut(keyCode)) {
            String text = getConfigText();
            String copy = hasConfigSelection() ? text.substring(getConfigSelectionStart(), getConfigSelectionEnd()) : text;
            minecraft.keyboardListener.setClipboardString(copy);
            if (hasConfigSelection()) {
                deleteConfigSelection();
            } else {
                setConfigText("", 0, 0, -1);
            }
            return;
        }
        if (Screen.isPaste(keyCode)) {
            try {
                insertConfigText(minecraft.keyboardListener.getClipboardString());
            } catch (Exception ignored) {
            }
            return;
        }

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            startConfigKeyRepeat(GLFW.GLFW_KEY_BACKSPACE);
            deleteConfigPrevious(ctrlDown);
            return;
        }
        if (keyCode == GLFW.GLFW_KEY_DELETE) {
            startConfigKeyRepeat(GLFW.GLFW_KEY_DELETE);
            deleteConfigNext(ctrlDown);
            return;
        }

        resetConfigKeyRepeat();

        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            setConfigCursorPosition(ctrlDown ? getConfigPreviousWordBoundary(getConfigCursor()) : getConfigCursor() - 1, shiftDown);
        } else if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            setConfigCursorPosition(ctrlDown ? getConfigNextWordBoundary(getConfigCursor()) : getConfigCursor() + 1, shiftDown);
        } else if (keyCode == GLFW.GLFW_KEY_HOME) {
            setConfigCursorPosition(0, shiftDown);
        } else if (keyCode == GLFW.GLFW_KEY_END) {
            setConfigCursorPosition(getConfigText().length(), shiftDown);
        }
    }

    private void startConfigKeyRepeat(int keyCode) {
        long now = System.currentTimeMillis();
        configRepeatKey = keyCode;
        configRepeatHoldStart = now;
        configRepeatLastStep = now;
    }

    private void resetConfigKeyRepeat() {
        configRepeatKey = -1;
        configRepeatHoldStart = 0L;
        configRepeatLastStep = 0L;
    }

    private void updateConfigHeldKeyRepeat() {
        configInputEditPulse = AnimationMath.fast(configInputEditPulse, 0.0f, 8);
        configTagEditPulse = AnimationMath.fast(configTagEditPulse, 0.0f, 8);

        if (!isConfigTextEditing() || minecraft == null || minecraft.getMainWindow() == null) {
            resetConfigKeyRepeat();
            return;
        }

        long handle = minecraft.getMainWindow().getHandle();
        boolean backspaceDown = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_BACKSPACE) == GLFW.GLFW_PRESS;
        boolean deleteDown = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_DELETE) == GLFW.GLFW_PRESS;
        int heldKey = backspaceDown ? GLFW.GLFW_KEY_BACKSPACE : (deleteDown ? GLFW.GLFW_KEY_DELETE : -1);

        if (heldKey == -1) {
            resetConfigKeyRepeat();
            return;
        }

        long now = System.currentTimeMillis();
        if (configRepeatKey != heldKey) {
            configRepeatKey = heldKey;
            configRepeatHoldStart = now;
            configRepeatLastStep = now;
            return;
        }

        if (now - configRepeatHoldStart < 260L || now - configRepeatLastStep < 38L) {
            return;
        }

        boolean ctrlDown = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
        if (heldKey == GLFW.GLFW_KEY_BACKSPACE) {
            deleteConfigPrevious(ctrlDown);
        } else {
            deleteConfigNext(ctrlDown);
        }
        configRepeatLastStep = now;
    }

    private void renderConfigsPanel(MatrixStack stack, float x, float contentTop, float contentHeight, int mouseX, int mouseY, float alpha) {
        boolean configPanelMoved = Float.isNaN(lastConfigPanelX) || Float.isNaN(lastConfigContentTop)
                || Math.abs(lastConfigPanelX - x) > 0.01f
                || Math.abs(lastConfigContentTop - contentTop) > 0.01f;
        lastConfigPanelX = x;
        lastConfigContentTop = contentTop;

        if (!configsLoaded && !configsLoading) {
            configsLoading = true;
            new Thread(() -> {
                try {
                    List<xd.harm.config.Config> list = Harmony.getInstance().getConfigStorage().getConfigs();
                    cachedConfigs = sortConfigsByMeta(list != null ? list : new ArrayList<>());
                } catch (Exception e) {
                    cachedConfigs = new ArrayList<>();
                }
                configsLoaded = true;
                configsLoading = false;
            }).start();
        }

        float pad = 10;
        float innerX = x + pad;
        float innerW = PANEL_WIDTH - pad * 2;
        int themeColor = Theme.MainColor(0);

        if (!configsLoaded) {
            float cx = x + PANEL_WIDTH / 2f;
            float cy = contentTop + contentHeight / 2f;
            double time = System.currentTimeMillis() / 1000.0;
            float dots = (float)(time % 1.0);
            String loadingText = "Loading" + ".".repeat((int)(dots * 3) + 1);
            Fonts.sfuy.drawCenteredText(stack, loadingText, cx, cy - 4, ColorUtils.rgba(255, 255, 255, (int)(160 * alpha)), 7.0f);

            float spinnerR = 8;
            float angle = (float)(time * 4.0);
            int segments = 20;
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glLineWidth(2.0f);
            GL11.glBegin(GL11.GL_LINE_STRIP);
            for (int i = 0; i < segments; i++) {
                float t = (float) i / (segments - 1);
                float a = angle + t * (float)(Math.PI * 1.5);
                float px = cx + (float)(Math.cos(a) * spinnerR);
                float py = cy + 18 + (float)(Math.sin(a) * spinnerR);
                float segAlpha = t * alpha;
                int r2 = (themeColor >> 16) & 0xFF;
                int g2 = (themeColor >> 8) & 0xFF;
                int b2 = themeColor & 0xFF;
                GL11.glColor4f(r2 / 255f, g2 / 255f, b2 / 255f, segAlpha);
                GL11.glVertex2f(px, py);
            }
            GL11.glEnd();
            GL11.glDisable(GL11.GL_LINE_SMOOTH);
            return;
        }

        configMetaStore.ensure(cachedConfigs);

        float inputH = 15;
        float inputY = contentTop + 4;

        drawConfigEditorField(stack, innerX, inputY, innerW, inputH, mouseX, mouseY, alpha, themeColor);

        float btnW = (innerW - 12) / 4f;
        float btnH = 12;
        float btnY = inputY + inputH + 4;
        String[] btnLabels = {"Save", "Load", "Import", "Export"};
        for (int i = 0; i < 4; i++) {
            float bx = innerX + i * (btnW + 4);
            boolean hovered = RenderUtility.isInRegion(mouseX, mouseY, bx, btnY, btnW, btnH);
            drawConfigTopButton(stack, btnLabels[i], bx, btnY, btnW, btnH, hovered, alpha, themeColor, i);
        }

        float listY = btnY + btnH + 5;
        float listH = contentTop + contentHeight - listY;

        List<xd.harm.config.Config> configs = cachedConfigs != null ? cachedConfigs : Collections.emptyList();
        int cols = 3;
        float itemH = 58.0f;
        float itemGap = 5.0f;
        float rowGap = 6.0f;
        float gridX = innerX + 6.0f;
        float itemW = (innerW - 12.0f - itemGap * (cols - 1)) / cols;
        int rows = (configs.size() + cols - 1) / cols;
        float totalH = rows * (itemH + rowGap);
        float maxScr = Math.max(0, totalH - listH);
        configScroll = MathHelper.clamp(configScroll, -maxScr, 0);
        configAnimatedScroll = AnimationMath.fast(configAnimatedScroll, configScroll, 15);

        Stencil.initStencilToWrite();
        RenderUtility.drawRectW(innerX, listY - 2.0f, innerW, listH + 4.0f, ColorUtils.rgba(255, 255, 255, 255));
        Stencil.readStencilBuffer(1);

        float cy = listY + configAnimatedScroll;
        int draggedIdx = -1;
        for (int i = 0; i < configs.size(); i++) {
            xd.harm.config.Config cfg = configs.get(i);
            String cardId = cfg.getName();
            int row = i / cols;
            int col = i % cols;
            float targetX = gridX + col * (itemW + itemGap);
            float targetY = cy + row * (itemH + rowGap);
            boolean isDragged = configCardDragging && configDragIndex == i;
            if (isDragged) {
                draggedIdx = i;
                continue;
            }
            float[] animPos = configCardAnimPos.computeIfAbsent(cardId, k -> new float[]{targetX, targetY});
            if (configPanelMoved || panelDragging) {
                animPos[0] = targetX;
                animPos[1] = targetY;
            } else {
                animPos[0] = AnimationMath.fast(animPos[0], targetX, 12);
                animPos[1] = AnimationMath.fast(animPos[1], targetY, 12);
            }
            float itemX = animPos[0];
            float itemY = animPos[1];
            if (itemY + itemH < listY || itemY > listY + listH) continue;
            ConfigMeta meta = configMetaStore.get(cardId);
            boolean hovered = RenderUtility.isInRegion(mouseX, mouseY, itemX, itemY, itemW, itemH);
            drawConfigCard(stack, cfg, meta, itemX, itemY, itemW, itemH, hovered, alpha, themeColor, mouseX, mouseY);
        }
        if (draggedIdx >= 0 && draggedIdx < configs.size()) {
            xd.harm.config.Config cfg = configs.get(draggedIdx);
            ConfigMeta meta = configMetaStore.get(cfg.getName());
            float dx = MathHelper.clamp((float) mouseX - configDragOffsetX, innerX, innerX + innerW - itemW);
            float dy = MathHelper.clamp((float) mouseY - configDragOffsetY, listY, listY + listH - itemH);
            configCardAnimPos.put(cfg.getName(), new float[]{dx, dy});
            drawConfigCard(stack, cfg, meta, dx, dy, itemW, itemH, true, alpha, themeColor, mouseX, mouseY);
        }

        Stencil.uninitStencilBuffer();

        if (totalH > listH + 0.1f) {
            float trackX = innerX + innerW - 3.2f;
            float trackY = listY + 2.0f;
            float trackW = 2.2f;
            float trackH = listH - 4.0f;
            float thumbH = Math.max(14.0f, (trackH / totalH) * trackH);
            float thumbRange = Math.max(0.0f, trackH - thumbH);
            float progress = maxScr > 0 ? MathHelper.clamp(-configAnimatedScroll / maxScr, 0.0f, 1.0f) : 0.0f;
            float thumbY = trackY + thumbRange * progress;

            RenderUtility.drawRoundedRect(trackX, trackY, trackW, trackH, 1.2f, ColorUtils.rgba(255, 255, 255, (int)(22 * alpha)));
            RenderUtility.drawRoundedRect(trackX, thumbY, trackW, thumbH, 1.2f,
                    ColorUtils.setAlpha(themeColor, (int)((configScrollDragging ? 150 : 95) * alpha)));
        }
    }

    private void drawConfigEditorField(MatrixStack stack, float x, float y, float w, float h, int mouseX, int mouseY, float alpha, int themeColor) {
        boolean hovered = RenderUtility.isInRegion(mouseX, mouseY, x, y, w, h);
        float pulse = MathHelper.clamp(configInputEditPulse, 0.0f, 1.0f);
        int base = configInputFocused
                ? ColorUtils.rgba(22, 26, 36, (int)(228 * alpha))
                : ColorUtils.rgba(14, 16, 22, (int)(208 * alpha));

        RenderUtility.drawShadow(x - 0.4f, y - 0.4f, w + 0.8f, h + 0.8f, 6,
                ColorUtils.setAlpha(themeColor, (int)((configInputFocused ? 15 : 5) * alpha)));
        RenderUtility.drawRoundedRect(x, y, w, h, 4.5f, base);

        float glowAlpha = (configInputFocused ? 0.24f : hovered ? 0.14f : 0.05f) + pulse * 0.10f;
        RenderUtility.drawRoundedRectWithRotatingGradient(
                x - 0.22f, y - 0.22f, w + 0.44f, h + 0.44f, 4.7f,
                themeColor, (System.currentTimeMillis() % 2400L) / 2400.0f * 360.0f,
                glowAlpha * alpha
        );
        RenderUtility.drawRoundedRect(x + 0.55f, y + 0.55f, w - 1.1f, h - 1.1f, 4.0f, base);

        Scissor.push();
        Scissor.setFromComponentCoordinates(x + 4.0f, y + 1.0f, w - 8.0f, h - 2.0f);
        drawConfigEditableText(stack, configInputText, configInputCursor, configInputSelection,
                configInputFocused, "Название кфг/код конфига", x + 5.0f, y + h / 2.0f,
                w - 10.0f, 6.0f, alpha, pulse, configInputEditDirection, themeColor);
        Scissor.pop();
    }

    private void drawConfigTopButton(MatrixStack stack, String label, float x, float y, float w, float h, boolean hovered, float alpha, int themeColor, int index) {
        configButtonHoverAnims[index] = AnimationMath.fast(configButtonHoverAnims[index], hovered ? 1.0f : 0.0f, 10);
        configButtonClickAnims[index] = AnimationMath.fast(configButtonClickAnims[index], 0.0f, 8);
        float hover = MathHelper.clamp(configButtonHoverAnims[index], 0.0f, 1.0f);
        float click = MathHelper.clamp(configButtonClickAnims[index], 0.0f, 1.0f);
        int bg = ColorUtils.interpolateColor(
                ColorUtils.rgba(18, 21, 30, (int)(210 * alpha)),
                ColorUtils.setAlpha(themeColor, (int)(78 * alpha)),
                hover
        );

        float cx = x + w / 2.0f;
        float cy = y + h / 2.0f;
        float scale = 1.0f + hover * 0.045f - click * 0.035f;

        GL11.glPushMatrix();
        GL11.glTranslatef(cx, cy, 0.0f);
        GL11.glScalef(scale, scale, 1.0f);
        GL11.glTranslatef(-cx, -cy, 0.0f);

        RenderUtility.drawShadow(x - 1.0f - hover, y - 1.0f - hover, w + 2.0f + hover * 2.0f, h + 2.0f + hover * 2.0f,
                (int)(5 + hover * 7 + click * 3),
                ColorUtils.setAlpha(themeColor, (int)((8 + hover * 28 + click * 18) * alpha)));
        RenderUtility.drawRoundedRectWithRotatingGradient(
                x - 0.4f, y - 0.4f, w + 0.8f, h + 0.8f, 3.4f,
                themeColor, (System.currentTimeMillis() % 2200L) / 2200.0f * 360.0f + index * 34.0f,
                (0.16f + hover * 0.40f + click * 0.30f) * alpha
        );
        RenderUtility.drawRoundedRect(x, y, w, h, 3.0f, bg);

        if (click > 0.02f) {
            RenderUtility.drawRoundedRect(x + 1.0f, y + 1.0f, w - 2.0f, h - 2.0f, 2.4f,
                    ColorUtils.rgba(255, 255, 255, (int)(32 * click * alpha)));
        }

        if (hover > 0.02f || click > 0.02f) {
            Scissor.push();
            Scissor.setFromComponentCoordinates(x, y, w, h);
            RenderSystem.enableBlend();
            RenderSystem.disableTexture();
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            float sweep = ((System.currentTimeMillis() + index * 180L) % 1300L) / 1300.0f;
            drawConfigShineBeam(x - 4.0f, y + 1.0f, w + 8.0f, h - 2.0f, sweep, (0.08f * hover + 0.10f * click) * alpha);
            GL11.glBegin(GL11.GL_QUADS);
            int tr = (themeColor >> 16) & 0xFF;
            int tg = (themeColor >> 8) & 0xFF;
            int tb = themeColor & 0xFF;
            GL11.glColor4f(tr / 255.0f, tg / 255.0f, tb / 255.0f, 0.13f * hover * alpha);
            GL11.glVertex2f(x, y + h - 1.0f);
            GL11.glVertex2f(x + w * hover, y + h - 1.0f);
            GL11.glColor4f(tr / 255.0f, tg / 255.0f, tb / 255.0f, 0.0f);
            GL11.glVertex2f(x + w * hover, y + h);
            GL11.glVertex2f(x, y + h);
            GL11.glEnd();
            RenderSystem.enableTexture();
            RenderSystem.defaultBlendFunc();
            Scissor.pop();
        }

        Fonts.sfuy.drawCenteredText(stack, label, x + w / 2f, y + h / 2f - 2.8f,
                ColorUtils.rgba(245, 247, 255, (int)((190 + 55 * hover + 10 * click) * alpha)), 5.5f);
        GL11.glPopMatrix();
    }

    private void drawConfigEditableText(MatrixStack stack, String text, int cursor, int selection, boolean focused,
                                        String placeholder, float x, float y, float maxWidth, float size, float alpha,
                                        float pulse, int editDirection, int themeColor) {
        String safeText = text == null ? "" : text;
        int safeCursor = MathHelper.clamp(cursor, 0, safeText.length());
        int safeSelection = MathHelper.clamp(selection, 0, safeText.length());
        boolean empty = safeText.isEmpty();
        String renderText = empty && !focused ? placeholder : safeText;
        int color = empty && !focused
                ? ColorUtils.rgba(255, 255, 255, (int)(60 * alpha))
                : ColorUtils.rgba(232, 236, 248, (int)(220 * alpha));

        float textY = y - size / 2.0f + 0.35f;
        float caretTop = textY - 1.0f;
        float textAlpha = empty && !focused ? alpha : alpha * (1.0f - pulse * 0.06f);

        if (focused && safeCursor != safeSelection && !safeText.isEmpty()) {
            int start = Math.min(safeCursor, safeSelection);
            int end = Math.max(safeCursor, safeSelection);
            float selX = x + Fonts.sfuy.getWidth(safeText.substring(0, start), size);
            float selW = Fonts.sfuy.getWidth(safeText.substring(start, end), size);
            RenderUtility.drawRoundedRect(selX, caretTop, selW, size + 2.0f, 2.0f,
                    ColorUtils.setAlpha(themeColor, (int)(95 * alpha)));
        }

        if ((focused || pulse > 0.02f) && !empty) {
            float lineY = textY + size + 1.1f;
            float textWidth = Math.min(Fonts.sfuy.getWidth(safeText, size), maxWidth);
            float caretX = x + Math.min(Fonts.sfuy.getWidth(safeText.substring(0, safeCursor), size), maxWidth);
            float sweep = ((System.currentTimeMillis() % 1500L) / 1500.0f);
            float sweepCenter = focused ? MathHelper.clamp(caretX - x, 0.0f, Math.max(1.0f, textWidth)) : textWidth * sweep;
            float segment = MathHelper.clamp(18.0f + pulse * 18.0f, 12.0f, Math.max(12.0f, textWidth));
            float lineStart = MathHelper.clamp(sweepCenter - segment * 0.55f, 0.0f, textWidth);
            float lineEnd = MathHelper.clamp(sweepCenter + segment * 0.45f, 0.0f, textWidth);
            int tr = (themeColor >> 16) & 0xFF;
            int tg = (themeColor >> 8) & 0xFF;
            int tb = themeColor & 0xFF;
            float baseAlpha = (focused ? 0.38f : 0.0f) + pulse * 0.24f;
            RenderUtility.drawRoundedRect(x, lineY, textWidth, 0.35f, 0.2f,
                    ColorUtils.rgba(tr, tg, tb, (int)(42 * baseAlpha * alpha)));
            if (lineEnd > lineStart + 0.5f) {
                RenderSystem.enableBlend();
                RenderSystem.disableTexture();
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                GL11.glShadeModel(GL11.GL_SMOOTH);
                GL11.glBegin(GL11.GL_QUADS);
                GL11.glColor4f(tr / 255.0f, tg / 255.0f, tb / 255.0f, 0.0f);
                GL11.glVertex2f(x + lineStart - 5.0f, lineY - 0.25f);
                GL11.glVertex2f(x + lineStart, lineY - 0.25f);
                GL11.glColor4f(tr / 255.0f, tg / 255.0f, tb / 255.0f, (0.24f + pulse * 0.20f) * alpha);
                GL11.glVertex2f(x + lineEnd, lineY - 0.25f);
                GL11.glVertex2f(x + lineEnd + 5.0f, lineY - 0.25f);
                GL11.glColor4f(tr / 255.0f, tg / 255.0f, tb / 255.0f, (0.24f + pulse * 0.20f) * alpha);
                GL11.glVertex2f(x + lineEnd + 5.0f, lineY + 0.85f);
                GL11.glVertex2f(x + lineEnd, lineY + 0.85f);
                GL11.glColor4f(tr / 255.0f, tg / 255.0f, tb / 255.0f, 0.0f);
                GL11.glVertex2f(x + lineStart, lineY + 0.85f);
                GL11.glVertex2f(x + lineStart - 5.0f, lineY + 0.85f);
                GL11.glEnd();
                GL11.glShadeModel(GL11.GL_FLAT);
                RenderSystem.enableTexture();
                RenderSystem.defaultBlendFunc();
            }
        }

        Fonts.sfuy.drawText(stack, trimToWidth(renderText, maxWidth, size), x, textY,
                ColorUtils.setAlpha(color, (int)(ColorUtils.getAlphaFromColor(color) * textAlpha)), size);

        if (focused && System.currentTimeMillis() % 1000L > 500L) {
            float caretX = x + Math.min(Fonts.sfuy.getWidth(safeText.substring(0, safeCursor), size), maxWidth);
            RenderUtility.drawRoundedRect(caretX, caretTop, 0.6f + pulse * 0.35f, size + 2.0f, 0.3f,
                    ColorUtils.rgba(255, 255, 255, (int)(230 * alpha)));
        }
    }

    private int getConfigCursorFromMouse(float mouseX, float textX, float maxWidth, float size) {
        String text = getConfigText();
        float localX = MathHelper.clamp(mouseX - textX, 0.0f, maxWidth);
        float prevWidth = 0.0f;
        for (int i = 1; i <= text.length(); i++) {
            float width = Fonts.sfuy.getWidth(text.substring(0, i), size);
            float midpoint = prevWidth + (width - prevWidth) * 0.5f;
            if (localX < midpoint) {
                return i - 1;
            }
            prevWidth = width;
        }
        return text.length();
    }

    private void startConfigTextSelectionDrag(boolean tagField, int anchor, float textX, float maxWidth, float fontSize) {
        configTextSelectionDragging = true;
        configTextSelectionDragTag = tagField;
        configTextSelectionAnchor = anchor;
        configTextSelectionDragTextX = textX;
        configTextSelectionDragMaxWidth = maxWidth;
        configTextSelectionDragFontSize = fontSize;
    }

    private void updateConfigTextSelectionFromMouse(float mouseX) {
        if (!configTextSelectionDragging) {
            return;
        }

        if (configTextSelectionDragTag && configTagEditingName == null) {
            configTextSelectionDragging = false;
            return;
        }
        if (!configTextSelectionDragTag && !configInputFocused) {
            configTextSelectionDragging = false;
            return;
        }

        int cursor = getConfigCursorFromMouse(mouseX, configTextSelectionDragTextX,
                configTextSelectionDragMaxWidth, configTextSelectionDragFontSize);
        setConfigCursorValues(cursor, configTextSelectionAnchor);
    }

    private void performConfigAction(int action) {
        if (action >= 0 && action < configButtonClickAnims.length) {
            configButtonClickAnims[action] = 1.0f;
        }
        ConfigStorage storage = Harmony.getInstance().getConfigStorage();
        String name = configInputText.trim();
        switch (action) {
            case 0:
                if (!name.isEmpty() && storage.saveConfiguration(name)) {
                    configMetaStore.touch(name);
                }
                break;
            case 1:
                if (!name.isEmpty()) {
                    if (selectedTab == TAB_BOTCONFIGS) {
                        // Вкладка BotConfigs: применяем конфиг к ботам
                        // (HitAura/Scaffold/Velocity/AutoSprint -> bot_module_config.json)
                        xd.harm.command.feature.BotCommand.applyBotConfig(name);
                    } else {
                        storage.loadConfiguration(name);
                    }
                }
                break;
            case 2:
                String importCode = name;
                if (importCode.isEmpty() && minecraft != null && minecraft.keyboardListener != null) {
                    String clipboard = minecraft.keyboardListener.getClipboardString();
                    importCode = clipboard == null ? "" : clipboard.trim();
                }
                storage.importFromCode(importCode, "imported-config");
                break;
            case 3:
                if (!name.isEmpty()) {
                    String shareCode = storage.exportConfigurationCode(name);
                    if (shareCode != null && minecraft != null && minecraft.keyboardListener != null) {
                        minecraft.keyboardListener.setClipboardString(shareCode);
                    }
                }
                break;
            default:
                return;
        }
        configsLoaded = false;
        cachedConfigs = null;
    }

    private boolean handleConfigsClick(double mouseX, double mouseY, float x, float y) {
        float pad = 10;
        float innerX = x + pad;
        float innerW = PANEL_WIDTH - pad * 2;
        float contentTop = y + HEADER_HEIGHT + 2;
        float contentBottom = y + PANEL_HEIGHT - 4;
        float contentHeight = contentBottom - contentTop;

        float inputH = 15;
        float inputY = contentTop + 4;

        if (RenderUtility.isInRegion(mouseX, mouseY, innerX, inputY, innerW, inputH)) {
            configInputFocused = true;
            configTagEditingName = null;
            int cursor = getConfigCursorFromMouse((float) mouseX, innerX + 5.0f, innerW - 10.0f, 6.0f);
            configInputCursor = cursor;
            configInputSelection = cursor;
            startConfigTextSelectionDrag(false, cursor, innerX + 5.0f, innerW - 10.0f, 6.0f);
            return true;
        } else {
            configInputFocused = false;
            resetConfigKeyRepeat();
        }

        float btnW = (innerW - 12) / 4f;
        float btnH = 12;
        float btnY = inputY + inputH + 4;

        ConfigStorage storage = Harmony.getInstance().getConfigStorage();

        for (int i = 0; i < 4; i++) {
            float bx = innerX + i * (btnW + 4);
            if (RenderUtility.isInRegion(mouseX, mouseY, bx, btnY, btnW, btnH)) {
                performConfigAction(i);
                return true;
            }
        }

        float listY = btnY + btnH + 5;
        float listH = contentTop + contentHeight - listY;
        List<xd.harm.config.Config> configs = cachedConfigs != null ? cachedConfigs : storage.getConfigs();
        int cols = 3;
        float itemH = 58.0f;
        float itemGap = 5.0f;
        float rowGap = 6.0f;
        float gridX = innerX + 6.0f;
        float itemW = (innerW - 12.0f - itemGap * (cols - 1)) / cols;
        int rows = (configs.size() + cols - 1) / cols;
        float totalH = rows * (itemH + rowGap);
        float maxScr = Math.max(0, totalH - listH);

        if (maxScr > 0.0f) {
            float trackX = innerX + innerW - 3.2f;
            float trackY = listY + 2.0f;
            float trackW = 2.2f;
            float trackH = listH - 4.0f;
            float thumbH = Math.max(14.0f, (trackH / totalH) * trackH);
            float thumbRange = Math.max(0.0f, trackH - thumbH);
            float progress = MathHelper.clamp(-configAnimatedScroll / maxScr, 0.0f, 1.0f);
            float thumbY = trackY + thumbRange * progress;

            if (RenderUtility.isInRegion(mouseX, mouseY, trackX - 1.5f, trackY, trackW + 3.0f, trackH)) {
                configScrollDragging = true;
                if (RenderUtility.isInRegion(mouseX, mouseY, trackX - 1.5f, thumbY, trackW + 3.0f, thumbH)) {
                    configScrollDragOffset = (float) (mouseY - thumbY);
                } else {
                    configScrollDragOffset = thumbH / 2.0f;
                    updateConfigScrollFromDrag(mouseY, y);
                }
                return true;
            }
        } else {
            configScrollDragging = false;
        }

        float cy = listY + configAnimatedScroll;

        for (int i = 0; i < configs.size(); i++) {
            xd.harm.config.Config config = configs.get(i);
            String name = config.getName();
            int row = i / cols;
            int col = i % cols;
            float itemY = cy + row * (itemH + rowGap);
            if (itemY + itemH < listY || itemY > listY + listH) continue;

            float cardX = gridX + col * (itemW + itemGap);
            float cardW = itemW;
            float delX = cardX + cardW - 14.0f;
            float delY = itemY + 6.0f;
            float delSize = 8.0f;
            float tagX = cardX + 6.0f;
            float tagY = itemY + 38.0f;
            float tagW = cardW - 12.0f;
            float tagH = 12.0f;

            if (RenderUtility.isInRegion(mouseX, mouseY, delX, delY, delSize, delSize)) {
                storage.removeConfiguration(name);
                configMetaStore.remove(name);
                configsLoaded = false;
                cachedConfigs = null;
                return true;
            }

            if (RenderUtility.isInRegion(mouseX, mouseY, tagX, tagY, tagW, tagH)) {
                configTagEditingName = name;
                configTagInputText = normalizeConfigTagText(configMetaStore.get(name).tags);
                int tagCursor = getConfigCursorFromMouse((float) mouseX, tagX + 5.0f, tagW - 10.0f, 5.5f);
                configTagCursor = tagCursor;
                configTagSelection = tagCursor;
                configInputFocused = false;
                startConfigTextSelectionDrag(true, tagCursor, tagX + 5.0f, tagW - 10.0f, 5.5f);
                return true;
            }

            float dragZoneH = 34.0f;
            if (RenderUtility.isInRegion(mouseX, mouseY, cardX, itemY, cardW, dragZoneH)) {
                configInputText = name;
                configInputCursor = configInputText.length();
                configInputSelection = configInputCursor;
                configInputFocused = true;
                configTagEditingName = null;
                configDragIndex = i;
                configDragOffsetX = (float) mouseX - cardX;
                configDragOffsetY = (float) mouseY - itemY;
                configDragMouseY = (float) mouseY;
                configDragStartMouseY = (float) mouseY;
                configCardDragging = true;
                configDragMoved = false;
                return true;
            }
        }
        return false;
    }

    private void drawConfigCard(MatrixStack stack, xd.harm.config.Config cfg, ConfigMeta meta,
                                float x, float y, float w, float h, boolean hovered,
                                float alpha, int themeColor, int mouseX, int mouseY) {
        String cardId = cfg.getName();
        boolean dragged = configCardDragging && configDragIndex >= 0 && cachedConfigs != null
                && configDragIndex < cachedConfigs.size() && cachedConfigs.get(configDragIndex).getName().equals(cardId);
        float ha = configCardHoverAnims.getOrDefault(cardId, 0.0f);
        ha = AnimationMath.fast(ha, (hovered || dragged) ? 1.0f : 0.0f, dragged ? 14 : 8);
        configCardHoverAnims.put(cardId, ha);
        float scale = 1.0f + ha * 0.035f + (dragged ? 0.025f : 0.0f);
        float cx = x + w / 2f, cy = y + h / 2f;
        GL11.glPushMatrix();
        GL11.glTranslatef(cx, cy, 0);
        GL11.glScalef(scale, scale, 1);
        GL11.glTranslatef(-cx, -cy, 0);
        drawConfigCardBackground(x, y, w, h, hovered, dragged, alpha, themeColor, cardId.hashCode(), ha, mouseX, mouseY);
        if (ha > 0.02f) {
            drawConfigRunningBorder(x, y, w, h, 7.0f, ha * alpha, themeColor, cardId.hashCode());
        }
        float sba = 1.0f - ha * 0.8f;
        if (sba > 0.01f) {
            RenderUtility.drawRoundedRectOutline(x, y, w, h, 7.0f, 0.5f, ColorUtils.rgba(255, 255, 255, (int)(22 * alpha * sba)));
        }
        if (ha > 0.02f) {
            Scissor.push();
            Scissor.setFromComponentCoordinates(x, y, w, h);
            int tr = (themeColor >> 16) & 0xFF, tg = (themeColor >> 8) & 0xFF, tb = themeColor & 0xFF;
            if (!dragged) {
                float rx = MathHelper.clamp((float)mouseX, x, x + w);
                float ry = MathHelper.clamp((float)mouseY, y, y + h);
                float glowR = 28.0f;
                int segs = 16;
                RenderSystem.enableBlend();
                RenderSystem.disableTexture();
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
                GL11.glShadeModel(GL11.GL_SMOOTH);
                GL11.glBegin(GL11.GL_TRIANGLE_FAN);
                GL11.glColor4f(tr / 255.0f, tg / 255.0f, tb / 255.0f, 0.22f * ha * alpha);
                GL11.glVertex2f(rx, ry);
                GL11.glColor4f(tr / 255.0f, tg / 255.0f, tb / 255.0f, 0.0f);
                for (int s = 0; s <= segs; s++) {
                    double ang = Math.PI * 2.0 * s / segs;
                    GL11.glVertex2f(rx + (float)Math.cos(ang) * glowR, ry + (float)Math.sin(ang) * glowR);
                }
                GL11.glEnd();
                GL11.glShadeModel(GL11.GL_FLAT);
                RenderSystem.defaultBlendFunc();
                RenderSystem.enableTexture();
            }
            GL11.glShadeModel(GL11.GL_FLAT);
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableTexture();
            Scissor.pop();
        }
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        float iconSz = 18.0f, iX = x + 7.0f, iY = y + 6.0f;
        int iconAlpha = (int)((180 + 75 * ha) * alpha);
        ClientFonts.AltManager[50].drawString(stack, "H", iX - 3, iY - 3.0f, ColorUtils.setAlpha(themeColor, iconAlpha));
        String name = trimToWidth(cfg.getName(), w - 70.0f, 6.2f);
        boolean isDefaultConfig = selectedTab == TAB_BOTCONFIGS && cfg.getName().equalsIgnoreCase("Bot");
        Fonts.sfuy.drawText(stack, name, iX + iconSz + 7.0f, y + 6.0f, ColorUtils.rgba(244, 246, 255, (int)((220 + 35 * ha) * alpha)), 6.2f);
        if (isDefaultConfig) {
            String defText = "Default";
            float defW = Fonts.sfuy.getWidth(defText, 4.5f);
            Fonts.sfuy.drawText(stack, defText, x + w - defW - 6.0f, y + 27.0f,
                    ColorUtils.setAlpha(themeColor, (int)(235 * alpha)), 4.5f);
        }
        Fonts.sfuy.drawText(stack, trimToWidth(formatConfigDate(meta.createdAt), w - 40.0f, 4.5f), x + 7.0f, y + 27.0f, ColorUtils.rgba(130, 140, 160, (int)(170 * alpha)), 4.5f);
        float delX = x + w - 14.0f, delY = y + 6.0f;
        boolean delHov = RenderUtility.isInRegion(mouseX, mouseY, delX, delY, 8.0f, 8.0f);
        float da = configDelHoverAnims.getOrDefault(cardId, 0.0f);
        da = AnimationMath.fast(da, delHov ? 1.0f : 0.0f, 10);
        configDelHoverAnims.put(cardId, da);
        int dR = (int)(255 * da + 255 * (1.0f - da));
        int dG = (int)(70 * da + 255 * (1.0f - da));
        int dB = (int)(70 * da + 255 * (1.0f - da));
        int dA = (int)((100 + 130 * da) * alpha);
        float delScale = 1.0f + da * 0.2f;
        float dcx = delX + 4.0f, dcy = delY + 4.0f;
        GL11.glPushMatrix();
        GL11.glTranslatef(dcx, dcy, 0);
        GL11.glScalef(delScale, delScale, 1);
        GL11.glTranslatef(-dcx, -dcy, 0);
        ClientFonts.AltManager[14].drawString(stack, "D", delX, delY + 1.0f, ColorUtils.rgba(dR, dG, dB, dA));
        GL11.glPopMatrix();
        float tagX = x + 6.0f, tagY = y + 38.0f, tagW = w - 12.0f, tagH = 12.0f;
        boolean tagFocused = cardId.equals(configTagEditingName);
        String tags = tagFocused ? configTagInputText : meta.tags;
        if (tagFocused) {
            float phase = ((System.currentTimeMillis() + Math.abs(cardId.hashCode() % 1000)) % 2100L) / 2100.0f;
            drawConfigTagPillSurface(tagX, tagY + 1.3f, tagW, tagH - 2.6f, phase, alpha, themeColor, 1.0f, MathHelper.clamp(configTagEditPulse, 0.0f, 1.0f));
            Scissor.push();
            Scissor.setFromComponentCoordinates(tagX + 4.0f, tagY, tagW - 8.0f, tagH);
            drawConfigEditableText(stack, tags, configTagCursor, configTagSelection, true,
                    "tag1, tag2...", tagX + 5.0f, tagY + tagH / 2.0f, tagW - 10.0f,
                    5.5f, alpha, configTagEditPulse, configTagEditDirection, themeColor);
            Scissor.pop();
        } else {
            drawConfigTagPills(stack, tags, tagX, tagY, tagW, tagH, alpha, themeColor, ha);
        }
        GlStateManager.disableBlend();
        GL11.glPopMatrix();
    }

    private void drawConfigAction(MatrixStack stack, String label, float x, float y, float sz, boolean cardHovered, boolean hovered, float alpha, int themeColor, boolean danger) {
        int bg = hovered
                ? (danger ? ColorUtils.rgba(210, 62, 74, (int) (135 * alpha)) : ColorUtils.setAlpha(themeColor, (int) (120 * alpha)))
                : ColorUtils.rgba(255, 255, 255, (int) ((cardHovered ? 30 : 18) * alpha));
        RenderUtility.drawRoundedRect(x, y, sz, sz, 3.0f, bg);
        RenderUtility.drawRoundedRectOutline(x, y, sz, sz, 3.0f, 0.35f, hovered ? ColorUtils.rgba(255, 255, 255, (int)(50 * alpha)) : ColorUtils.rgba(255, 255, 255, (int)(12 * alpha)));
        Fonts.sfuy.drawCenteredText(stack, label, x + sz / 2.0f, y + sz / 2.0f - 2.5f,
                ColorUtils.rgba(255, 255, 255, (int) (220 * alpha)), 5.0f);
    }

    private void drawConfigTagPills(MatrixStack stack, String tags, float areaX, float areaY, float areaW, float areaH, float alpha, int themeColor, float ha) {
        int tr = (themeColor >> 16) & 0xFF, tg = (themeColor >> 8) & 0xFF, tb = themeColor & 0xFF;
        if (tags == null || tags.trim().isEmpty()) {
            String emptyText = "+ tag";
            float emptyFont = 5.0f;
            float emptyPadX = 4.0f;
            float emptyW = Fonts.sfuy.getWidth(emptyText, emptyFont) + emptyPadX * 2.0f;
            float emptyH = 8.5f;
            float emptyY = areaY + (areaH - emptyH) / 2.0f;
            float phase = (System.currentTimeMillis() % 2100L) / 2100.0f;
            drawConfigTagPillSurface(areaX, emptyY, emptyW, emptyH, phase, alpha, themeColor, ha, 0.25f);
            Fonts.sfuy.drawText(stack, emptyText, areaX + emptyPadX, emptyY + 1.85f,
                    ColorUtils.rgba(255, 255, 255, (int)((58 + 45 * ha) * alpha)), 5.0f);
            return;
        }

        String[] parts = tags.split(",");
        float pillGap = 3.5f;
        float pillPadX = 5.5f;
        float pillH = 9.5f;
        float fontSize = 5.0f;
        float curX = areaX;
        float pillY = areaY + (areaH - pillH) / 2.0f;
        float maxX = areaX + areaW;
        int visible = 0;
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            String tag = normalizeConfigTagText(part.trim());
            if (tag.isEmpty()) continue;
            float textW = Fonts.sfuy.getWidth(tag, fontSize);
            float pillW = textW + pillPadX * 2;
            if (curX + pillW > maxX) {
                drawConfigTagOverflow(stack, maxX - 15.0f, pillY, 15.0f, pillH, alpha, themeColor, ha);
                break;
            }

            float phase = ((System.currentTimeMillis() + Math.abs(tag.hashCode() % 900)) % 2400L) / 2400.0f;
            float pulse = 0.5f + 0.5f * (float)Math.sin((phase + visible * 0.12f) * Math.PI * 2.0);

            drawConfigTagPillSurface(curX, pillY, pillW, pillH, phase + visible * 0.08f, alpha, themeColor, ha, pulse);

            Fonts.sfuy.drawText(stack, tag, curX + pillPadX - 0.8f, pillY + 2.15f,
                    ColorUtils.rgba(Math.min(255, tr + 112), Math.min(255, tg + 112), Math.min(255, tb + 112), (int)((190 + 35 * ha) * alpha)), fontSize);
            curX += pillW + pillGap;
            visible++;
        }
    }

    private void drawConfigTagOverflow(MatrixStack stack, float x, float y, float w, float h, float alpha, int themeColor, float ha) {
        if (w <= 4.0f) {
            return;
        }
        RenderUtility.drawRoundedRectWithRotatingGradient(x, y, w, h, 4.5f, themeColor,
                (System.currentTimeMillis() % 1800L) / 1800.0f * 360.0f,
                (0.08f + 0.08f * ha) * alpha);
        RenderUtility.drawRoundedRect(x + 0.5f, y + 0.5f, w - 1.0f, h - 1.0f, 4.0f,
                ColorUtils.rgba(12, 14, 20, (int)(135 * alpha)));
        Fonts.sfuy.drawCenteredText(stack, "...", x + w / 2.0f, y + h / 2.0f - 2.8f,
                ColorUtils.rgba(230, 235, 255, (int)((125 + 65 * ha) * alpha)), 5.0f);
    }

    private void drawConfigTagPillSurface(float x, float y, float w, float h, float phase, float alpha, int themeColor, float ha, float pulse) {
        int tr = (themeColor >> 16) & 0xFF, tg = (themeColor >> 8) & 0xFF, tb = themeColor & 0xFF;
        RenderUtility.drawShadow(x + 0.2f, y + 0.2f, w - 0.4f, h - 0.4f, 4,
                ColorUtils.rgba(tr / 4, tg / 4, tb / 4, (int)((5 + 9 * ha) * alpha)));
        RenderUtility.drawRoundedRectWithRotatingGradient(
                x - 0.18f, y - 0.18f, w + 0.36f, h + 0.36f, h / 2.0f,
                themeColor, phase * 360.0f,
                (0.065f + 0.075f * ha + 0.025f * pulse) * alpha
        );
        RenderUtility.drawRoundedRect(x + 0.35f, y + 0.35f, w - 0.7f, h - 0.7f, h / 2.0f - 0.35f,
                ColorUtils.rgba(10 + (int)(tr * 0.055f), 12 + (int)(tg * 0.055f), 16 + (int)(tb * 0.055f), (int)((118 + 28 * ha) * alpha)));

        Scissor.push();
        Scissor.setFromComponentCoordinates(x, y, w, h);
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        drawConfigTagInnerShader(x, y, w, h, phase, alpha, themeColor, ha, pulse);
        drawConfigTagGlint(x, y, w, h, phase, (0.035f + 0.045f * ha + 0.018f * pulse) * alpha);
        RenderSystem.enableTexture();
        RenderSystem.defaultBlendFunc();
        Scissor.pop();
    }

    private void drawConfigTagInnerShader(float x, float y, float w, float h, float phase, float alpha, int themeColor, float ha, float pulse) {
        int tr = (themeColor >> 16) & 0xFF, tg = (themeColor >> 8) & 0xFF, tb = themeColor & 0xFF;
        float wave = 0.5f + 0.5f * (float)Math.sin(phase * Math.PI * 2.0f);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glColor4f(tr / 255.0f, tg / 255.0f, tb / 255.0f, (0.030f + 0.035f * ha + 0.018f * wave) * alpha);
        GL11.glVertex2f(x + 0.5f, y + 0.7f);
        GL11.glVertex2f(x + w - 0.5f, y + 0.7f);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, (0.018f + 0.026f * pulse) * alpha);
        GL11.glVertex2f(x + w - 0.5f, y + h * 0.52f);
        GL11.glVertex2f(x + 0.5f, y + h * 0.52f);

        GL11.glColor4f(tr / 255.0f, tg / 255.0f, tb / 255.0f, 0.0f);
        GL11.glVertex2f(x + w * 0.10f, y + h - 0.55f);
        GL11.glColor4f(tr / 255.0f, tg / 255.0f, tb / 255.0f, (0.055f + 0.050f * ha) * alpha);
        GL11.glVertex2f(x + w * (0.35f + 0.45f * wave), y + h - 0.55f);
        GL11.glVertex2f(x + w * (0.52f + 0.42f * wave), y + h - 0.15f);
        GL11.glColor4f(tr / 255.0f, tg / 255.0f, tb / 255.0f, 0.0f);
        GL11.glVertex2f(x + w - 0.8f, y + h - 0.15f);
        GL11.glEnd();
        GL11.glShadeModel(GL11.GL_FLAT);
    }

    private void drawConfigTagGlint(float x, float y, float w, float h, float phase, float glintAlpha) {
        float beamW = 3.8f;
        float pos = -beamW * 2.0f + (w + beamW * 4.0f) * phase;
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.0f);
        GL11.glVertex2f(x + pos - beamW, y);
        GL11.glVertex2f(x + pos, y);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, glintAlpha);
        GL11.glVertex2f(x + pos + beamW * 1.8f, y + h);
        GL11.glVertex2f(x + pos + beamW * 0.8f, y + h);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.0f);
        GL11.glVertex2f(x + pos + beamW * 2.8f, y + h);
        GL11.glVertex2f(x + pos + beamW * 1.8f, y + h);
        GL11.glEnd();
    }

    private void drawConfigCardBackground(float x, float y, float w, float h, boolean hovered, boolean dragged, float alpha, int themeColor, int seed, float ha, int mouseX, int mouseY) {
        int tr = (themeColor >> 16) & 0xFF, tg = (themeColor >> 8) & 0xFF, tb = themeColor & 0xFF;
        float shadowSz = 6.0f + 6.0f * ha + (dragged ? 4.0f : 0.0f);
        int shadowA = (int)((18 + 38 * ha + (dragged ? 18 : 0)) * alpha);
        RenderUtility.drawShadow(x - 2, y - 2, w + 4, h + 4, (int)shadowSz, ColorUtils.rgba(tr / 4, tg / 4, tb / 4, shadowA));
        if (ha > 0.1f) RenderUtility.drawShadow(x - 1, y - 1, w + 2, h + 2, 10, ColorUtils.setAlpha(themeColor, (int)(14 * ha * alpha)));
        float bb = 10 + 2 * ha + (dragged ? 4 : 0);
        float ts = 0.03f + 0.05f * ha;
        int bgR = (int)Math.min(255, bb + tr * ts), bgG = (int)Math.min(255, bb + tg * ts), bgB = (int)Math.min(255, bb + tb * ts);
        int bgA = (int)((dragged ? 230 : 215 + 20 * ha) * alpha);
        RenderUtility.drawRoundedRect(x, y, w, h, 7.0f, ColorUtils.rgba(3, 4, 8, (int)(110 * alpha)));
        RenderUtility.drawRoundedRect(x, y, w, h, 7.0f, ColorUtils.rgba(bgR, bgG, bgB, bgA));
        Scissor.push();
        Scissor.setFromComponentCoordinates(x, y, w, h);
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        if (ha > 0.02f) {
            float rx = MathHelper.clamp((float)mouseX, x + 5, x + w - 5);
            float ry = MathHelper.clamp((float)mouseY, y + 5, y + h - 5);
            float glowR = 35.0f;
            int segs = 20;
            GL11.glShadeModel(GL11.GL_SMOOTH);
            GL11.glBegin(GL11.GL_TRIANGLE_FAN);
            GL11.glColor4f(tr / 255.0f, tg / 255.0f, tb / 255.0f, 0.18f * ha * alpha);
            GL11.glVertex2f(rx, ry);
            GL11.glColor4f(tr / 255.0f, tg / 255.0f, tb / 255.0f, 0.0f);
            for (int s = 0; s <= segs; s++) {
                double ang = Math.PI * 2.0 * s / segs;
                GL11.glVertex2f(rx + (float)Math.cos(ang) * glowR, ry + (float)Math.sin(ang) * glowR);
            }
            GL11.glEnd();
            GL11.glShadeModel(GL11.GL_FLAT);
        }
        float phase = ((System.currentTimeMillis() + Math.abs(seed % 900)) % 1800L) / 1800.0f;
        float pulse = 0.35f + 0.25f * (float)Math.sin(phase * Math.PI * 2.0);
        GL11.glBegin(GL11.GL_TRIANGLES);
        GL11.glColor4f(tr / 255.0f, tg / 255.0f, tb / 255.0f, (0.08f + pulse * 0.04f + ha * 0.04f) * alpha);
        GL11.glVertex2f(x, y);
        GL11.glColor4f(tr / 255.0f, tg / 255.0f, tb / 255.0f, 0.0f);
        GL11.glVertex2f(x + w * 0.55f, y);
        GL11.glVertex2f(x, y + h * 0.65f);
        GL11.glEnd();
        float shBase = ((System.currentTimeMillis() + Math.abs(seed % 1300)) % 2600L) / 2600.0f;
        drawConfigShineBeam(x + 2, y + 2, w - 4, h - 4, shBase, 0.055f * alpha);
        RenderSystem.enableTexture();
        RenderSystem.defaultBlendFunc();
        Scissor.pop();
    }

    private void drawConfigRunningBorder(float x, float y, float w, float h, float radius, float alpha, int themeColor, int seed) {
        double time = System.currentTimeMillis() / 1000.0;
        float headAngle = (float)(((time * 1.5) + (Math.abs(seed) % 100) * 0.01) % (Math.PI * 2));
        float tR = ((themeColor >> 16) & 0xFF) / 255.0f;
        float tG = ((themeColor >> 8) & 0xFF) / 255.0f;
        float tB = (themeColor & 0xFF) / 255.0f;
        float cx = x + w / 2f, cy = y + h / 2f;
        float tailLen = 2.0f;
        float rd = Math.min(radius, Math.min(w, h) / 2f);
        float x2 = x + w, y2 = y + h;
        int seg = 10;
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glLineWidth(1.3f);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int j = 0; j <= seg; j++) { double a = Math.toRadians(180 + 90.0 * j / seg); float vx = (float)(x + rd + Math.cos(a) * rd); float vy = (float)(y + rd + Math.sin(a) * rd); setOutlineVertexColor(vx, vy, cx, cy, headAngle, tailLen, tR, tG, tB, alpha); GL11.glVertex2f(vx, vy); }
        setOutlineVertexColor(x + rd, y, cx, cy, headAngle, tailLen, tR, tG, tB, alpha); GL11.glVertex2f(x + rd, y);
        setOutlineVertexColor(x2 - rd, y, cx, cy, headAngle, tailLen, tR, tG, tB, alpha); GL11.glVertex2f(x2 - rd, y);
        for (int j = 0; j <= seg; j++) { double a = Math.toRadians(270 + 90.0 * j / seg); float vx = (float)(x2 - rd + Math.cos(a) * rd); float vy = (float)(y + rd + Math.sin(a) * rd); setOutlineVertexColor(vx, vy, cx, cy, headAngle, tailLen, tR, tG, tB, alpha); GL11.glVertex2f(vx, vy); }
        setOutlineVertexColor(x2, y + rd, cx, cy, headAngle, tailLen, tR, tG, tB, alpha); GL11.glVertex2f(x2, y + rd);
        setOutlineVertexColor(x2, y2 - rd, cx, cy, headAngle, tailLen, tR, tG, tB, alpha); GL11.glVertex2f(x2, y2 - rd);
        for (int j = 0; j <= seg; j++) { double a = Math.toRadians(90.0 * j / seg); float vx = (float)(x2 - rd + Math.cos(a) * rd); float vy = (float)(y2 - rd + Math.sin(a) * rd); setOutlineVertexColor(vx, vy, cx, cy, headAngle, tailLen, tR, tG, tB, alpha); GL11.glVertex2f(vx, vy); }
        setOutlineVertexColor(x2 - rd, y2, cx, cy, headAngle, tailLen, tR, tG, tB, alpha); GL11.glVertex2f(x2 - rd, y2);
        setOutlineVertexColor(x + rd, y2, cx, cy, headAngle, tailLen, tR, tG, tB, alpha); GL11.glVertex2f(x + rd, y2);
        for (int j = 0; j <= seg; j++) { double a = Math.toRadians(90 + 90.0 * j / seg); float vx = (float)(x + rd + Math.cos(a) * rd); float vy = (float)(y2 - rd + Math.sin(a) * rd); setOutlineVertexColor(vx, vy, cx, cy, headAngle, tailLen, tR, tG, tB, alpha); GL11.glVertex2f(vx, vy); }
        setOutlineVertexColor(x, y2 - rd, cx, cy, headAngle, tailLen, tR, tG, tB, alpha); GL11.glVertex2f(x, y2 - rd);
        setOutlineVertexColor(x, y + rd, cx, cy, headAngle, tailLen, tR, tG, tB, alpha); GL11.glVertex2f(x, y + rd);
        GL11.glEnd();
        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.enableTexture();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private void drawConfigShineBeam(float x, float y, float w, float h, float progress, float beamAlpha) {
        float travel = w + h + 34.0f;
        float pos = -h - 16.0f + travel * progress;
        float beamW = 8.0f;
        float tilt = h * 0.75f;
        float x1 = x + pos, x2 = x1 + beamW;
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.0f);
        GL11.glVertex2f(x1 - beamW, y);
        GL11.glVertex2f(x2 - beamW, y);
        GL11.glColor4f(1.0f, 1.0f, 1.0f, beamAlpha);
        GL11.glVertex2f(x2 + tilt, y + h);
        GL11.glVertex2f(x1 + tilt, y + h);
        GL11.glEnd();
    }

    private List<xd.harm.config.Config> sortConfigsByMeta(List<xd.harm.config.Config> configs) {
        configMetaStore.ensure(configs);
        configs.sort((a, b) -> Integer.compare(configMetaStore.get(a.getName()).order, configMetaStore.get(b.getName()).order));
        return configs;
    }

    private String makeConfigCopyName(String sourceName, ConfigStorage storage) {
        String base = sourceName + "_copy";
        String candidate = base;
        int index = 2;
        while (storage.existsConfiguration(candidate)) {
            candidate = base + index++;
        }
        return candidate;
    }

    private String trimToWidth(String text, float width, float size) {
        if (text == null) {
            return "";
        }
        if (Fonts.sfuy.getWidth(text, size) <= width) {
            return text;
        }
        String result = text;
        while (result.length() > 1 && Fonts.sfuy.getWidth(result + "...", size) > width) {
            result = result.substring(0, result.length() - 1);
        }
        return result + "...";
    }

    private String formatConfigDate(long createdAt) {
        return new SimpleDateFormat("dd.MM.yyyy HH:mm").format(new Date(createdAt));
    }

    private void updateConfigDragSwap(double mouseX, double mouseY) {
        if (cachedConfigs == null || configDragIndex < 0 || configDragIndex >= cachedConfigs.size()) {
            return;
        }
        int targetIndex = getConfigDragTargetIndex(mouseX, mouseY);
        if (targetIndex != configDragIndex) {
            Collections.swap(cachedConfigs, configDragIndex, targetIndex);
            configDragIndex = targetIndex;
            configMetaStore.saveOrder(cachedConfigs);
        }
    }

    private int getConfigDragTargetIndex(double mouseX, double mouseY) {
        if (cachedConfigs == null || cachedConfigs.isEmpty()) {
            return -1;
        }
        float contentTop = panelPosY + HEADER_HEIGHT + 2;
        float inputY = contentTop + 4;
        float listY = inputY + 15.0f + 4.0f + 12.0f + 5.0f;
        float pad = 10.0f;
        float innerX = panelPosX + pad;
        float innerW = PANEL_WIDTH - pad * 2.0f;
        int cols = 3;
        float itemH = 58.0f;
        float itemGap = 5.0f;
        float rowGap = 6.0f;
        float gridX = innerX + 6.0f;
        float itemW = (innerW - 12.0f - itemGap * (cols - 1)) / cols;
        int row = MathHelper.clamp((int) (((float) mouseY - listY - configAnimatedScroll) / (itemH + rowGap)), 0, Math.max(0, (cachedConfigs.size() + cols - 1) / cols - 1));
        int col = MathHelper.clamp((int) (((float) mouseX - gridX) / (itemW + itemGap)), 0, cols - 1);
        return MathHelper.clamp(row * cols + col, 0, cachedConfigs.size() - 1);
    }

    private void finishConfigDrag(double mouseX, double mouseY) {
        if (configDragMoved) {
            updateConfigDragSwap(mouseX, mouseY);
        }
        resetConfigDrag();
    }

    private void resetConfigDrag() {
        configDragIndex = -1;
        configDragOffsetX = 0.0f;
        configDragOffsetY = 0.0f;
        configDragMouseY = 0.0f;
        configDragStartMouseY = 0.0f;
        configCardDragging = false;
        configDragMoved = false;
    }

    private void renderAutoBuyPanel(MatrixStack stack, float x, float contentTop, float contentHeight, int mouseX, int mouseY, float alpha) {
        int themeColor = Theme.MainColor(0);
        xd.harm.modules.impl.player.AutoBuy autoBuy = Harmony.getInstance().getModuleManager().getAutoBuy();
        xd.harm.modules.impl.player.autobuy.AutoBuyManager manager = autoBuy.getManager();
        List<xd.harm.modules.impl.player.autobuy.AutoBuyItem> items = manager.getItems();

        if (items.isEmpty()) {
            autoBuySelectedIndex = -1;
            resetAutoBuyEditors();
        } else if (autoBuySelectedIndex < 0 || autoBuySelectedIndex >= items.size()) {
            autoBuySelectedIndex = 0;
            autoBuyEditorIndex = -1;
        }

        float pad = 6;

        float toggleH = 14;
        float toggleY = contentTop + 2;
        float toggleGap = 4;
        float toggleW = (PANEL_WIDTH - pad * 2 - toggleGap) / 2.0f;
        float parserX = x + pad + toggleW + toggleGap;
        boolean isOn = autoBuy.isState();
        boolean toggleHov = RenderUtility.isInRegion(mouseX, mouseY, x + pad, toggleY, toggleW, toggleH);
        RenderUtility.drawRoundedRect(x + pad, toggleY, toggleW, toggleH, 3.5f,
                isOn ? ColorUtils.setAlpha(themeColor, (int)(45 * alpha)) : ColorUtils.rgba(22, 22, 22, (int)(180 * alpha)));
        RenderUtility.drawRoundedRectOutline(x + pad, toggleY, toggleW, toggleH, 3.5f, 0.5f,
                isOn ? ColorUtils.setAlpha(themeColor, (int)(80 * alpha)) : ColorUtils.rgba(255, 255, 255, (int)((toggleHov ? 35 : 15) * alpha)));
        Fonts.sfuy.drawText(stack, isOn ? "AutoBuy: ON" : "AutoBuy: OFF", x + pad + 5, toggleY + toggleH / 2f - 3f,
                isOn ? ColorUtils.setAlpha(themeColor, (int)(255 * alpha)) : ColorUtils.rgba(255, 255, 255, (int)(130 * alpha)), 5.5f);
        float dotSz = 3.5f;
        RenderUtility.drawRoundedRect(x + pad + toggleW - dotSz - 5, toggleY + (toggleH - dotSz) / 2f, dotSz, dotSz, dotSz / 2f,
                isOn ? ColorUtils.rgba(80, 255, 80, (int)(255 * alpha)) : ColorUtils.rgba(255, 80, 80, (int)(200 * alpha)));
        boolean parserOn = autoBuy.getParser().get();
        boolean parserRunning = autoBuy.getSystem().isParsingPrices();
        boolean parserHov = RenderUtility.isInRegion(mouseX, mouseY, parserX, toggleY, toggleW, toggleH);
        int parserAccent = parserOn ? ColorUtils.rgba(255, 188, 74, 255) : ColorUtils.rgba(120, 130, 150, 255);
        RenderUtility.drawRoundedRect(parserX, toggleY, toggleW, toggleH, 3.5f,
                parserOn ? ColorUtils.setAlpha(parserAccent, (int)(38 * alpha)) : ColorUtils.rgba(22, 22, 22, (int)(180 * alpha)));
        RenderUtility.drawRoundedRectOutline(parserX, toggleY, toggleW, toggleH, 3.5f, 0.5f,
                parserOn ? ColorUtils.setAlpha(parserAccent, (int)(90 * alpha)) : ColorUtils.rgba(255, 255, 255, (int)((parserHov ? 35 : 15) * alpha)));
        Fonts.sfuy.drawText(stack, parserRunning ? "Parser: RUN" : (parserOn ? "Parser: ON" : "Parser: OFF"),
                parserX + 5, toggleY + toggleH / 2f - 3f,
                parserOn ? ColorUtils.setAlpha(parserAccent, (int)(255 * alpha)) : ColorUtils.rgba(255, 255, 255, (int)(130 * alpha)), 5.5f);
        RenderUtility.drawRoundedRect(parserX + toggleW - dotSz - 5, toggleY + (toggleH - dotSz) / 2f, dotSz, dotSz, dotSz / 2f,
                parserOn ? ColorUtils.rgba(255, 200, 80, (int)(240 * alpha)) : ColorUtils.rgba(255, 80, 80, (int)(200 * alpha)));

        float gridTop = toggleY + toggleH + 4;
        float gridBottom = contentTop + contentHeight;
        float leftW = 180;
        float rightX = x + pad + leftW + 4;
        float rightW = PANEL_WIDTH - pad * 2 - leftW - 4;

        RenderUtility.drawRoundedRect(x + pad, gridTop, leftW, gridBottom - gridTop, 4.0f, ColorUtils.rgba(15, 15, 15, (int)(120 * alpha)));

        float gridH = gridBottom - gridTop;
        int rows = (int) Math.ceil(items.size() / (float) AB_COLS);
        float totalGridH = rows * (AB_ITEM_SIZE + AB_ITEM_GAP);
        float maxScr = Math.max(0, totalGridH - gridH + 6);
        autoBuyScroll = MathHelper.clamp(autoBuyScroll, -maxScr, 0);
        autoBuyAnimScroll = AnimationMath.fast(autoBuyAnimScroll, autoBuyScroll, 15);

        Stencil.initStencilToWrite();
        RenderUtility.drawRoundedRect(x + pad, gridTop, leftW, gridH, 4.0f, ColorUtils.rgba(255, 255, 255, 255));
        Stencil.readStencilBuffer(1);

        float gx = x + pad + 4;
        float gy = gridTop + 3 + autoBuyAnimScroll;
        for (int i = 0; i < items.size(); i++) {
            int col = i % AB_COLS;
            int row = i / AB_COLS;
            float ix = gx + col * (AB_ITEM_SIZE + AB_ITEM_GAP);
            float iy = gy + row * (AB_ITEM_SIZE + AB_ITEM_GAP);
            if (iy + AB_ITEM_SIZE < gridTop || iy > gridBottom) continue;

            boolean sel = i == autoBuySelectedIndex;
            boolean hov = RenderUtility.isInRegion(mouseX, mouseY, ix, iy, AB_ITEM_SIZE, AB_ITEM_SIZE);
            int bg = sel ? ColorUtils.setAlpha(themeColor, (int)(40 * alpha))
                    : ColorUtils.rgba(25, 25, 30, (int)((hov ? 180 : 140) * alpha));
            RenderUtility.drawRoundedRect(ix, iy, AB_ITEM_SIZE, AB_ITEM_SIZE, 5.0f, bg);
            if (sel) {
                RenderUtility.drawRoundedRectOutline(ix, iy, AB_ITEM_SIZE, AB_ITEM_SIZE, 5.0f, 0.8f, ColorUtils.setAlpha(themeColor, (int)(180 * alpha)));
            } else if (hov) {
                RenderUtility.drawRoundedRectOutline(ix, iy, AB_ITEM_SIZE, AB_ITEM_SIZE, 5.0f, 0.5f, ColorUtils.rgba(255, 255, 255, (int)(60 * alpha)));
            }

            xd.harm.modules.impl.player.autobuy.AutoBuyItem item = items.get(i);
            if (item.itemStack != null) {
                RenderSystem.pushMatrix();
                RenderSystem.translatef(ix + (AB_ITEM_SIZE - 16) / 2f, iy + (AB_ITEM_SIZE - 16) / 2f, 0);
                RenderSystem.scalef(0.9f, 0.9f, 1);
                minecraft.getItemRenderer().renderItemIntoGUI(item.itemStack, 0, 0);
                RenderSystem.popMatrix();
            }

            if (item.buyPrice > 0 || item.parsingEnabled) {
                float bsz = 4;
                int badgeCol = item.parsingEnabled ? ColorUtils.rgba(255, 200, 80, (int)(220 * alpha))
                        : ColorUtils.setAlpha(themeColor, (int)(200 * alpha));
                RenderUtility.drawRoundedRect(ix + AB_ITEM_SIZE - bsz - 2, iy + 2, bsz, bsz, bsz / 2f, badgeCol);
            }
        }
        Stencil.uninitStencilBuffer();

        if (maxScr > 0.0f) {
            float trackX = x + pad + leftW - 3.2f;
            float trackY = gridTop + 2.0f;
            float trackW = 2.2f;
            float trackH = gridH - 4.0f;
            float thumbH = Math.max(14.0f, (trackH / totalGridH) * trackH);
            float thumbRange = Math.max(0.0f, trackH - thumbH);
            float progress = MathHelper.clamp(-autoBuyAnimScroll / maxScr, 0.0f, 1.0f);
            float thumbY = trackY + thumbRange * progress;
            boolean thumbHover = RenderUtility.isInRegion(mouseX, mouseY, trackX - 1.5f, thumbY, trackW + 3.0f, thumbH);

            RenderUtility.drawRoundedRect(trackX, trackY, trackW, trackH, 1.2f, ColorUtils.rgba(255, 255, 255, (int)(22 * alpha)));
            RenderUtility.drawRoundedRect(trackX, thumbY, trackW, thumbH, 1.2f,
                    ColorUtils.setAlpha(themeColor, (int)(((autoBuyScrollDragging || thumbHover) ? 150 : 95) * alpha)));
        }

        RenderUtility.drawRoundedRect(rightX, gridTop, rightW, gridBottom - gridTop, 4.0f, ColorUtils.rgba(15, 15, 15, (int)(100 * alpha)));

        if (autoBuySelectedIndex >= 0 && autoBuySelectedIndex < items.size()) {
            xd.harm.modules.impl.player.autobuy.AutoBuyItem selItem = items.get(autoBuySelectedIndex);
            float ry = gridTop + 6;

            String name = selItem.itemName;
            if (name.length() > 24) name = name.substring(0, 22) + "..";
            Fonts.sfuy.drawCenteredText(stack, name, rightX + rightW / 2f, ry,
                    ColorUtils.rgba(255, 255, 255, (int)(230 * alpha)), 7.0f);
            ry += 12;

            if (selItem.itemStack != null) {
                float previewSize = 44;
                float previewX = rightX + (rightW - previewSize) / 2f;
                renderRotatingAutoBuyItem(selItem.itemStack, previewX + previewSize / 2.0f, ry + previewSize / 2.0f + 1.0f, 22.0f, autoBuyItemRotation);
                ry += previewSize + 8;
            }

            float fieldW = rightW - 12;
            float fieldH = 12;
            float fieldX = rightX + 6;
            float inputX = fieldX + 10.0f;
            float inputW = fieldW - 14.0f;

            float buyFieldY;
            float sellFieldY;

            Fonts.sfuy.drawText(stack, "\u0426\u0435\u043d\u0430 \u043f\u043e\u043a\u0443\u043f\u043a\u0438", fieldX, ry, ColorUtils.rgba(255, 255, 255, (int)(80 * alpha)), 4.5f);
            ry += 8;
            buyFieldY = ry;

            syncAutoBuyEditors(selItem, inputX, buyFieldY, inputW, fieldH, false);

            boolean buyFocused = autoBuyBuyField != null && autoBuyBuyField.isFocused();
            RenderUtility.drawRoundedRect(fieldX, ry, fieldW, fieldH, 3.0f,
                    buyFocused ? ColorUtils.rgba(35, 35, 40, (int)(200 * alpha)) : ColorUtils.rgba(22, 22, 22, (int)(180 * alpha)));
            RenderUtility.drawRoundedRectOutline(fieldX, ry, fieldW, fieldH, 3.0f, 0.5f,
                    buyFocused ? ColorUtils.setAlpha(themeColor, (int)(100 * alpha)) : ColorUtils.rgba(255, 255, 255, (int)(20 * alpha)));
            Fonts.sfuy.drawText(stack, "$", fieldX + 4, ry + fieldH / 2f - 2.5f,
                    ColorUtils.rgba(255, 255, 255, (int)((buyFocused ? 210 : 130) * alpha)), 5.0f);
            drawAutoBuyFieldValue(stack, autoBuyBuyField, selItem.buyPrice > 0 ? String.valueOf(selItem.buyPrice) : "0",
                    inputX, inputW - 1.0f, ry, fieldH, buyFocused, alpha, themeColor);
            ry += fieldH + 4;

            Fonts.sfuy.drawText(stack, "\u0426\u0435\u043d\u0430 \u043f\u0440\u043e\u0434\u0430\u0436\u0438", fieldX, ry, ColorUtils.rgba(255, 255, 255, (int)(80 * alpha)), 4.5f);
            ry += 8;
            sellFieldY = ry;

            syncAutoBuyEditors(selItem, inputX, sellFieldY, inputW, fieldH, true);

            boolean sellFocused = autoBuySellField != null && autoBuySellField.isFocused();
            RenderUtility.drawRoundedRect(fieldX, ry, fieldW, fieldH, 3.0f,
                    sellFocused ? ColorUtils.rgba(35, 35, 40, (int)(200 * alpha)) : ColorUtils.rgba(22, 22, 22, (int)(180 * alpha)));
            RenderUtility.drawRoundedRectOutline(fieldX, ry, fieldW, fieldH, 3.0f, 0.5f,
                    sellFocused ? ColorUtils.setAlpha(themeColor, (int)(100 * alpha)) : ColorUtils.rgba(255, 255, 255, (int)(20 * alpha)));
            Fonts.sfuy.drawText(stack, "$", fieldX + 4, ry + fieldH / 2f - 2.5f,
                    ColorUtils.rgba(255, 255, 255, (int)((sellFocused ? 210 : 130) * alpha)), 5.0f);
            drawAutoBuyFieldValue(stack, autoBuySellField, selItem.sellPrice > 0 ? String.valueOf(selItem.sellPrice) : "0",
                    inputX, inputW - 1.0f, ry, fieldH, sellFocused, alpha, themeColor);
            ry += fieldH + 6;

            float btnH = 12;
            boolean saveHov = RenderUtility.isInRegion(mouseX, mouseY, fieldX, ry, fieldW, btnH);
            drawAutoBuyActionButton(stack, "\u0421\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c", fieldX, ry, fieldW, btnH, saveHov, themeColor, alpha, false);
            ry += btnH + 4;

            boolean parseHov = RenderUtility.isInRegion(mouseX, mouseY, fieldX, ry, fieldW, btnH);
            int parseColor = selItem.parsingEnabled ? ColorUtils.rgba(255, 188, 74, 255) : ColorUtils.rgba(120, 130, 150, 255);
            String parseText = "Parse: " + (selItem.parsingEnabled ? "ON" : "OFF");
            drawAutoBuyActionButton(stack, parseText, fieldX, ry, fieldW, btnH, parseHov, parseColor, alpha, selItem.parsingEnabled);
        } else {
            resetAutoBuyEditors();
            Fonts.sfuy.drawCenteredText(stack, "\u0412\u044b\u0431\u0435\u0440\u0438\u0442\u0435 \u043f\u0440\u0435\u0434\u043c\u0435\u0442", rightX + rightW / 2f, gridTop + (gridBottom - gridTop) / 2f,
                    ColorUtils.rgba(255, 255, 255, (int)(60 * alpha)), 6.0f);
        }
    }

    private boolean handleAutoBuyClick(double mouseX, double mouseY, int button, float x, float y) {
        xd.harm.modules.impl.player.AutoBuy autoBuy = Harmony.getInstance().getModuleManager().getAutoBuy();
        xd.harm.modules.impl.player.autobuy.AutoBuyManager manager = autoBuy.getManager();
        List<xd.harm.modules.impl.player.autobuy.AutoBuyItem> items = manager.getItems();

        if (items.isEmpty()) {
            autoBuySelectedIndex = -1;
            resetAutoBuyEditors();
        } else if (autoBuySelectedIndex < 0 || autoBuySelectedIndex >= items.size()) {
            autoBuySelectedIndex = 0;
            autoBuyEditorIndex = -1;
        }

        float contentTop = y + HEADER_HEIGHT + 2;
        float contentBottom = y + PANEL_HEIGHT - 4;
        float contentHeight = contentBottom - contentTop;
        float pad = 6;
        float toggleH = 14;
        float toggleY = contentTop + 2;
        float toggleGap = 4;
        float toggleW = (PANEL_WIDTH - pad * 2 - toggleGap) / 2.0f;
        float parserX = x + pad + toggleW + toggleGap;
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && RenderUtility.isInRegion(mouseX, mouseY, x + pad, toggleY, toggleW, toggleH)) {
            autoBuy.toggle();
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && RenderUtility.isInRegion(mouseX, mouseY, parserX, toggleY, toggleW, toggleH)) {
            boolean next = !autoBuy.getParser().get();
            if (next && !autoBuy.isState()) {
                autoBuy.toggle();
            }
            autoBuy.getParser().set(next);
            if (next) {
                minecraft.displayGuiScreen(null);
                autoBuy.requestParserStart();
            } else {
                autoBuy.getSystem().stopPriceParsing();
            }
            return true;
        }

        float gridTop = toggleY + toggleH + 4;
        float gridBottom = contentTop + contentHeight;
        float leftW = 180;
        float rightX = x + pad + leftW + 4;
        float rightW = PANEL_WIDTH - pad * 2 - leftW - 4;

        float gridH = gridBottom - gridTop;
        int rows = (int) Math.ceil(items.size() / (float) AB_COLS);
        float totalGridH = rows * (AB_ITEM_SIZE + AB_ITEM_GAP);
        float maxScr = Math.max(0, totalGridH - gridH + 6);

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && maxScr > 0.0f) {
            float trackX = x + pad + leftW - 3.2f;
            float trackY = gridTop + 2.0f;
            float trackW = 2.2f;
            float trackH = gridH - 4.0f;
            float thumbH = Math.max(14.0f, (trackH / totalGridH) * trackH);
            float thumbRange = Math.max(0.0f, trackH - thumbH);
            float progress = MathHelper.clamp(-autoBuyAnimScroll / maxScr, 0.0f, 1.0f);
            float thumbY = trackY + thumbRange * progress;

            if (RenderUtility.isInRegion(mouseX, mouseY, trackX - 1.5f, trackY, trackW + 3.0f, trackH)) {
                autoBuyScrollDragging = true;
                if (RenderUtility.isInRegion(mouseX, mouseY, trackX - 1.5f, thumbY, trackW + 3.0f, thumbH)) {
                    autoBuyScrollDragOffset = (float) (mouseY - thumbY);
                } else {
                    autoBuyScrollDragOffset = thumbH / 2.0f;
                    updateAutoBuyScrollFromDrag(mouseY, y);
                }
                return true;
            }
        }

        if (RenderUtility.isInRegion(mouseX, mouseY, x + pad, gridTop, leftW, gridBottom - gridTop)) {
            float gx = x + pad + 4;
            float gy = gridTop + 3 + autoBuyAnimScroll;
            for (int i = 0; i < items.size(); i++) {
                int col = i % AB_COLS;
                int row = i / AB_COLS;
                float ix = gx + col * (AB_ITEM_SIZE + AB_ITEM_GAP);
                float iy = gy + row * (AB_ITEM_SIZE + AB_ITEM_GAP);
                if (iy + AB_ITEM_SIZE < gridTop || iy > gridBottom) continue;

                if (RenderUtility.isInRegion(mouseX, mouseY, ix, iy, AB_ITEM_SIZE, AB_ITEM_SIZE)) {
                    xd.harm.modules.impl.player.autobuy.AutoBuyItem item = items.get(i);
                    if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                        autoBuySelectedIndex = i;
                        resetAutoBuyEditors();
                    } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                        item.buyPrice = 0;
                        item.sellPrice = 0;
                        manager.saveConfig();
                        if (autoBuySelectedIndex == i) {
                            if (autoBuyBuyField != null) autoBuyBuyField.setText("");
                            if (autoBuySellField != null) autoBuySellField.setText("");
                        }
                    } else if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
                        item.parsingEnabled = !item.parsingEnabled;
                        manager.saveConfig();
                    }
                    return true;
                }
            }
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                blurAutoBuyFieldFocus();
                return true;
            }
        }

        if (autoBuySelectedIndex < 0 || autoBuySelectedIndex >= items.size()) {
            blurAutoBuyFieldFocus();
            return false;
        }

        xd.harm.modules.impl.player.autobuy.AutoBuyItem selItem = items.get(autoBuySelectedIndex);
        float ry = gridTop + 6;
        ry += 12;
        if (selItem.itemStack != null) {
            float previewSize = 44;
            ry += previewSize + 8;
        }

        float fieldW = rightW - 12;
        float fieldH = 12;
        float fieldX = rightX + 6;
        float buyFieldY = ry + 8;
        float sellFieldY = buyFieldY + fieldH + 12;
        float inputX = fieldX + 10.0f;
        float inputW = fieldW - 14.0f;
        float btnH = 12;
        float saveBtnY = sellFieldY + fieldH + 6;
        float parseBtnY = saveBtnY + btnH + 4;

        syncAutoBuyEditors(selItem, inputX, buyFieldY, inputW, fieldH, false);
        syncAutoBuyEditors(selItem, inputX, sellFieldY, inputW, fieldH, true);

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (RenderUtility.isInRegion(mouseX, mouseY, fieldX, buyFieldY, fieldW, fieldH)) {
                if (autoBuyBuyField != null) {
                    if (!autoBuyBuyField.mouseClicked(mouseX, mouseY, button)) {
                        autoBuyBuyField.setCursorPositionZero();
                        autoBuyBuyField.setSelectionPos(autoBuyBuyField.getCursorPosition());
                    }
                    autoBuyBuyField.setFocused2(true);
                }
                if (autoBuySellField != null) autoBuySellField.setFocused2(false);
                return true;
            }

            if (RenderUtility.isInRegion(mouseX, mouseY, fieldX, sellFieldY, fieldW, fieldH)) {
                if (autoBuySellField != null) {
                    if (!autoBuySellField.mouseClicked(mouseX, mouseY, button)) {
                        autoBuySellField.setCursorPositionZero();
                        autoBuySellField.setSelectionPos(autoBuySellField.getCursorPosition());
                    }
                    autoBuySellField.setFocused2(true);
                }
                if (autoBuyBuyField != null) autoBuyBuyField.setFocused2(false);
                return true;
            }

            if (RenderUtility.isInRegion(mouseX, mouseY, fieldX, saveBtnY, fieldW, btnH)) {
                saveAutoBuyItem(selItem, manager);
                blurAutoBuyFieldFocus();
                return true;
            }

            if (RenderUtility.isInRegion(mouseX, mouseY, fieldX, parseBtnY, fieldW, btnH)) {
                selItem.parsingEnabled = !selItem.parsingEnabled;
                manager.saveConfig();
                return true;
            }

            if (RenderUtility.isInRegion(mouseX, mouseY, rightX, gridTop, rightW, gridBottom - gridTop)) {
                blurAutoBuyFieldFocus();
                return true;
            }
        }

        return false;
    }

    private void saveAutoBuyItem(xd.harm.modules.impl.player.autobuy.AutoBuyItem item, xd.harm.modules.impl.player.autobuy.AutoBuyManager manager) {
        if (autoBuyBuyField != null) {
            item.buyPrice = parseAutoBuyPrice(autoBuyBuyField.getText());
        }
        if (autoBuySellField != null) {
            item.sellPrice = parseAutoBuyPrice(autoBuySellField.getText());
        }
        manager.saveConfig();
    }

    private long parseAutoBuyPrice(String value) {
        try {
            return Long.parseLong(value.replaceAll("\\D", ""));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private boolean handleAutoBuyKeyPressed(int keyCode, int scanCode, int modifiers) {
        boolean buyFocused = autoBuyBuyField != null && autoBuyBuyField.isFocused();
        boolean sellFocused = autoBuySellField != null && autoBuySellField.isFocused();
        if (!buyFocused && !sellFocused) {
            return false;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            blurAutoBuyFieldFocus();
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_TAB) {
            if (buyFocused) {
                if (autoBuyBuyField != null) autoBuyBuyField.setFocused2(false);
                if (autoBuySellField != null) autoBuySellField.setFocused2(true);
            } else {
                if (autoBuySellField != null) autoBuySellField.setFocused2(false);
                if (autoBuyBuyField != null) autoBuyBuyField.setFocused2(true);
            }
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            xd.harm.modules.impl.player.AutoBuy autoBuy = Harmony.getInstance().getModuleManager().getAutoBuy();
            List<xd.harm.modules.impl.player.autobuy.AutoBuyItem> items = autoBuy.getManager().getItems();
            if (autoBuySelectedIndex >= 0 && autoBuySelectedIndex < items.size()) {
                saveAutoBuyItem(items.get(autoBuySelectedIndex), autoBuy.getManager());
            }
            blurAutoBuyFieldFocus();
            return true;
        }

        if (buyFocused && autoBuyBuyField != null && autoBuyBuyField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (sellFocused && autoBuySellField != null && autoBuySellField.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }

        return true;
    }

    private boolean handleAutoBuyCharTyped(char codePoint, int modifiers) {
        boolean buyFocused = autoBuyBuyField != null && autoBuyBuyField.isFocused();
        boolean sellFocused = autoBuySellField != null && autoBuySellField.isFocused();
        if (!buyFocused && !sellFocused) {
            return false;
        }

        if (buyFocused && autoBuyBuyField != null) {
            autoBuyBuyField.charTyped(codePoint, modifiers);
            return true;
        }
        if (sellFocused && autoBuySellField != null) {
            autoBuySellField.charTyped(codePoint, modifiers);
            return true;
        }

        return true;
    }

    private void blurAutoBuyFieldFocus() {
        if (autoBuyBuyField != null) autoBuyBuyField.setFocused2(false);
        if (autoBuySellField != null) autoBuySellField.setFocused2(false);
    }

    private void resetAutoBuyEditors() {
        autoBuyEditorIndex = -1;
        autoBuyBuyField = null;
        autoBuySellField = null;
    }

    private void syncAutoBuyEditors(xd.harm.modules.impl.player.autobuy.AutoBuyItem item, float fieldX, float fieldY, float fieldW, float fieldH, boolean sellField) {
        boolean recreate = autoBuyEditorIndex != autoBuySelectedIndex || autoBuyBuyField == null || autoBuySellField == null;
        if (recreate) {
            autoBuyBuyField = new AutoBuyField(font, (int) fieldX, (int) fieldY, (int) fieldW, (int) fieldH, StringTextComponent.EMPTY);
            autoBuySellField = new AutoBuyField(font, (int) fieldX, (int) fieldY, (int) fieldW, (int) fieldH, StringTextComponent.EMPTY);

            int hiddenInputColor = 0x01000000;
            autoBuyBuyField.setEnableBackgroundDrawing(false);
            autoBuyBuyField.setMaxStringLength(12);
            autoBuyBuyField.setTextColor(hiddenInputColor);
            autoBuyBuyField.setDisabledTextColour(hiddenInputColor);
            autoBuyBuyField.setValidator(value -> value != null && value.matches("\\d*"));

            autoBuySellField.setEnableBackgroundDrawing(false);
            autoBuySellField.setMaxStringLength(12);
            autoBuySellField.setTextColor(hiddenInputColor);
            autoBuySellField.setDisabledTextColour(hiddenInputColor);
            autoBuySellField.setValidator(value -> value != null && value.matches("\\d*"));

            autoBuyBuyField.setText(item.buyPrice > 0 ? String.valueOf(item.buyPrice) : "");
            autoBuySellField.setText(item.sellPrice > 0 ? String.valueOf(item.sellPrice) : "");
            autoBuyEditorIndex = autoBuySelectedIndex;
        }

        if (sellField) {
            autoBuySellField.x = (int) fieldX;
            autoBuySellField.y = (int) fieldY;
            autoBuySellField.setWidth((int) fieldW);
            autoBuySellField.tick();
        } else {
            autoBuyBuyField.x = (int) fieldX;
            autoBuyBuyField.y = (int) fieldY;
            autoBuyBuyField.setWidth((int) fieldW);
            autoBuyBuyField.tick();
        }
    }

    private void drawAutoBuyFieldValue(MatrixStack stack, AutoBuyField field, String placeholder, float textX, float textMaxW, float fieldY, float fieldH, boolean focused, float alpha, int themeColor) {
        String fullText = field != null ? field.getText() : "";
        float textY = fieldY + fieldH / 2f - 2.5f;
        float textSize = 5.0f;

        if (!focused && fullText.isEmpty()) {
            Fonts.sfuy.drawText(stack, placeholder, textX, textY, ColorUtils.rgba(255, 255, 255, (int)(60 * alpha)), textSize);
            return;
        }

        int cursor = field != null ? MathHelper.clamp(field.getCursorPosition(), 0, fullText.length()) : fullText.length();
        int trimStart = 0;
        String visible = fullText;
        float maxWidth = Math.max(4.0f, textMaxW);

        if (Fonts.sfuy.getWidth(visible, textSize) > maxWidth) {
            if (cursor >= fullText.length()) {
                while (trimStart < fullText.length() && Fonts.sfuy.getWidth(fullText.substring(trimStart), textSize) > maxWidth) {
                    trimStart++;
                }
                visible = fullText.substring(trimStart);
            } else {
                int start = MathHelper.clamp(cursor - 1, 0, fullText.length());
                int end = start;
                while (end < fullText.length() && Fonts.sfuy.getWidth(fullText.substring(start, end + 1), textSize) <= maxWidth) {
                    end++;
                }
                while (start > 0 && Fonts.sfuy.getWidth(fullText.substring(start - 1, Math.max(start, end)), textSize) <= maxWidth) {
                    start--;
                }
                trimStart = start;
                visible = fullText.substring(start, Math.max(start, end));
                while (!visible.isEmpty() && Fonts.sfuy.getWidth(visible, textSize) > maxWidth) {
                    trimStart++;
                    visible = fullText.substring(trimStart, Math.max(trimStart, end));
                }
            }
        }

        int textColor = ColorUtils.rgba(255, 255, 255, (int)((focused ? 210 : 200) * alpha));

        Stencil.initStencilToWrite();
        RenderUtility.drawRectW(textX - 0.4f, fieldY + 1.0f, maxWidth + 0.8f, fieldH - 2.0f, -1);
        Stencil.readStencilBuffer(1);
        if (!visible.isEmpty()) {
            Fonts.sfuy.drawText(stack, visible, textX, textY, textColor, textSize);
        }

        if (focused && ((System.currentTimeMillis() / 450L) % 2L == 0L) && field != null) {
            int visibleCursor = MathHelper.clamp(cursor - trimStart, 0, visible.length());
            String beforeCursor = visible.substring(0, visibleCursor);
            float caretX = textX + Fonts.sfuy.getWidth(beforeCursor, textSize) + 0.7f;
            RenderUtility.drawRectW(caretX, textY - 0.2f, 0.8f, textSize + 1.8f, ColorUtils.setAlpha(themeColor, (int)(220 * alpha)));
        }
        Stencil.uninitStencilBuffer();
    }

    private void drawAutoBuyActionButton(MatrixStack stack, String text, float x, float y, float w, float h, boolean hovered, int accentColor, float alpha, boolean toggled) {
        float time = System.currentTimeMillis() / 1000.0f;
        int base = toggled ? ColorUtils.rgba(24, 24, 30, (int)(220 * alpha)) : ColorUtils.rgba(20, 20, 24, (int)(205 * alpha));
        int border = toggled ? ColorUtils.setAlpha(accentColor, (int)(185 * alpha)) : ColorUtils.rgba(255, 255, 255, (int)(38 * alpha));
        float hoverValue = hovered ? 1.0f : 0.0f;

        if (hovered || toggled) {
            int glowAlpha = (int)((hovered ? 70 : 45) * alpha);
            RenderUtility.drawShadow(x - 0.5f, y - 0.5f, w + 1.0f, h + 1.0f, 8, ColorUtils.setAlpha(accentColor, glowAlpha));
        }

        RenderUtility.drawRoundedRect(x, y, w, h, 3.3f, base);

        Stencil.initStencilToWrite();
        RenderUtility.drawRoundedRect(x, y, w, h, 3.3f, -1);
        Stencil.readStencilBuffer(1);
        for (float i = 0; i < w; i += 2.0f) {
            float pct = i / w;
            float wave = (float) (Math.sin(pct * 5.0 + time * 2.2) * 0.5 + 0.5);
            int c1 = ColorUtils.setAlpha(accentColor, (int)(180 * alpha));
            int c2 = ColorUtils.setAlpha(ColorUtils.getOppositeColor(accentColor), (int)(120 * alpha));
            int mixed = ColorUtils.interpolateColor(c1, c2, wave);
            float lineAlpha = (0.07f + hoverValue * 0.09f + (toggled ? 0.06f : 0.0f)) * alpha;
            RenderUtility.drawRectW(x + i, y, 2.0f, h, ColorUtils.setAlpha(mixed, (int)(255 * lineAlpha)));
        }
        Stencil.uninitStencilBuffer();

        RenderUtility.drawRoundedRectOutline(x, y, w, h, 3.3f, 0.7f, border);
        int textColor = toggled
                ? ColorUtils.setAlpha(accentColor, (int)(240 * alpha))
                : ColorUtils.rgba(255, 255, 255, (int)((hovered ? 225 : 190) * alpha));
        Fonts.sfuy.drawCenteredText(stack, text, x + w / 2f, y + h / 2f - 2.5f, textColor, 5.05f);
    }

    private void updateModuleScrollFromDrag(double mouseY, float panelY) {
        List<Module> modules = getVisibleModules();

        float contentTop = panelY + HEADER_HEIGHT + CATEGORY_BAR_HEIGHT + 2;
        float contentBottom = panelY + PANEL_HEIGHT - 4;
        float contentHeight = contentBottom - contentTop;
        float gridContentHeight = getMainGridContentHeight(contentHeight);

        int rows = (int) Math.ceil((double) modules.size() / MODULES_PER_ROW);
        float totalHeight = rows * (MODULE_CELL_HEIGHT + MODULE_GAP_Y);
        float maxScr = Math.max(0, totalHeight - gridContentHeight + 4);
        if (maxScr <= 0.0f) {
            scroll = 0;
            moduleScrollDragging = false;
            return;
        }

        float trackY = contentTop + 2.0f;
        float trackH = gridContentHeight - 4.0f;
        float thumbH = Math.max(14.0f, (trackH / Math.max(1.0f, totalHeight)) * trackH);
        float thumbRange = Math.max(0.0f, trackH - thumbH);
        if (thumbRange <= 0.0f) {
            scroll = 0;
            moduleScrollDragging = false;
            return;
        }

        float thumbY = MathHelper.clamp((float) mouseY - moduleScrollDragOffset, trackY, trackY + thumbRange);
        float progress = (thumbY - trackY) / thumbRange;
        scroll = MathHelper.clamp(-maxScr * progress, -maxScr, 0);
    }

    private boolean handleModuleSearchKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (moduleSearchField == null) {
            return false;
        }

        if (Screen.hasControlDown() && keyCode == GLFW.GLFW_KEY_F) {
            setModuleSearchFocused(true);
            moduleSearchField.setCursorPositionEnd();
            moduleSearchField.setSelectionPos(moduleSearchField.getCursorPosition());
            return true;
        }

        if (!moduleSearchField.isFocused()) {
            resetModuleSearchKeyRepeat();
            return false;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            setModuleSearchFocused(false);
            return true;
        }

        String before = moduleSearchField.getText();
        boolean handled = moduleSearchField.keyPressed(keyCode, scanCode, modifiers);
        if (handled && !before.equals(moduleSearchField.getText())) {
            saveModuleSearchText();
            resetModuleGridScroll();
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE) {
            startModuleSearchKeyRepeat(keyCode);
        } else if (handled) {
            resetModuleSearchKeyRepeat();
        }
        return handled;
    }

    private boolean handleModuleSearchCharTyped(char codePoint, int modifiers) {
        if (moduleSearchField == null || !moduleSearchField.isFocused()) {
            return false;
        }

        String before = moduleSearchField.getText();
        boolean handled = moduleSearchField.charTyped(codePoint, modifiers);
        if (handled && !before.equals(moduleSearchField.getText())) {
            saveModuleSearchText();
            resetModuleGridScroll();
        }
        return handled;
    }

    private void saveModuleSearchText() {
        moduleSearchSavedText = moduleSearchField == null ? "" : moduleSearchField.getText();
        invalidateVisibleModules();
    }

    private void startModuleSearchKeyRepeat(int keyCode) {
        long now = System.currentTimeMillis();
        moduleSearchRepeatKey = keyCode;
        moduleSearchRepeatHoldStart = now;
        moduleSearchRepeatLastStep = now;
    }

    private void resetModuleSearchKeyRepeat() {
        moduleSearchRepeatKey = -1;
        moduleSearchRepeatHoldStart = 0L;
        moduleSearchRepeatLastStep = 0L;
    }

    private void updateModuleSearchHeldKeyRepeat() {
        if (moduleSearchField == null || !moduleSearchField.isFocused() || selectedTab != TAB_MAIN
                || minecraft == null || minecraft.getMainWindow() == null) {
            resetModuleSearchKeyRepeat();
            return;
        }

        long handle = minecraft.getMainWindow().getHandle();
        boolean backspaceDown = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_BACKSPACE) == GLFW.GLFW_PRESS;
        boolean deleteDown = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_DELETE) == GLFW.GLFW_PRESS;
        int heldKey = backspaceDown ? GLFW.GLFW_KEY_BACKSPACE : (deleteDown ? GLFW.GLFW_KEY_DELETE : -1);

        if (heldKey == -1) {
            resetModuleSearchKeyRepeat();
            return;
        }

        long now = System.currentTimeMillis();
        if (moduleSearchRepeatKey != heldKey) {
            moduleSearchRepeatKey = heldKey;
            moduleSearchRepeatHoldStart = now;
            moduleSearchRepeatLastStep = now;
            return;
        }

        if (now - moduleSearchRepeatHoldStart < 260L || now - moduleSearchRepeatLastStep < 38L) {
            return;
        }

        String before = moduleSearchField.getText();
        moduleSearchField.keyPressed(heldKey, 0, Screen.hasControlDown() ? GLFW.GLFW_MOD_CONTROL : 0);
        if (!before.equals(moduleSearchField.getText())) {
            saveModuleSearchText();
            resetModuleGridScroll();
        }
        moduleSearchRepeatLastStep = now;
    }

    private boolean handleModuleSearchClick(double mouseX, double mouseY) {
        if (moduleSearchField == null) {
            return false;
        }

        syncModuleSearchFieldGeometry();
        boolean clicked = moduleSearchField.mouseClicked(mouseX, mouseY, GLFW.GLFW_MOUSE_BUTTON_LEFT);
        if (clicked) {
            moduleSearchSelectionDragging = true;
            moduleSearchSelectionAnchor = moduleSearchField.getCursorPosition();
            return true;
        }

        moduleSearchSelectionDragging = false;
        setModuleSearchFocused(false);
        return false;
    }

    private void renderModuleSearchField(MatrixStack stack, int mouseX, int mouseY, float alpha) {
        if (moduleSearchField == null || selectedTab != TAB_MAIN) {
            return;
        }

        syncModuleSearchFieldGeometry();
        String query = moduleSearchField.getText();
        boolean focused = moduleSearchField.isFocused();
        if (query.isEmpty() && !focused) {
            Fonts.sfuy.drawText(stack, MODULE_SEARCH_PLACEHOLDER, moduleSearchField.x, moduleSearchField.y + 1.0f,
                    ColorUtils.rgba(255, 255, 255, (int)(165 * alpha)), 6.0f);
            return;
        }

        drawModuleSearchFieldValue(
                stack,
                moduleSearchField,
                moduleSearchField.x,
                moduleSearchField.y + 1.0f,
                Math.max(4.0f, moduleSearchField.getWidth()),
                focused,
                alpha,
                Theme.MainColor(0),
                6.0f
        );
    }

    private void drawModuleSearchFieldValue(MatrixStack stack, TextFieldWidget field, float textX, float textY, float maxWidth, boolean focused, float alpha, int caretColor, float textSize) {
        String fullText = field.getText();
        int cursor = MathHelper.clamp(field.getCursorPosition(), 0, fullText.length());
        int[] selectionRange = getTextFieldSelectionRange(field, fullText);
        int selectionStart = selectionRange[0];
        int selectionEnd = selectionRange[1];
        int trimStart = 0;
        String visible = fullText;
        float glyphY = textY + 0.7f;

        if (Fonts.sfuy.getWidth(visible, textSize) > maxWidth) {
            if (cursor >= fullText.length()) {
                while (trimStart < fullText.length() && Fonts.sfuy.getWidth(fullText.substring(trimStart), textSize) > maxWidth) {
                    trimStart++;
                }
                visible = fullText.substring(trimStart);
            } else {
                int start = MathHelper.clamp(cursor - 1, 0, fullText.length());
                int end = start;
                while (end < fullText.length() && Fonts.sfuy.getWidth(fullText.substring(start, end + 1), textSize) <= maxWidth) {
                    end++;
                }
                while (start > 0 && Fonts.sfuy.getWidth(fullText.substring(start - 1, Math.max(start, end)), textSize) <= maxWidth) {
                    start--;
                }
                trimStart = start;
                visible = fullText.substring(start, Math.max(start, end));
                while (!visible.isEmpty() && Fonts.sfuy.getWidth(visible, textSize) > maxWidth) {
                    trimStart++;
                    visible = fullText.substring(trimStart, Math.max(trimStart, end));
                }
            }
        }

        int textColor = ColorUtils.rgba(255, 255, 255, (int)((focused ? 220 : 200) * alpha));
        Stencil.initStencilToWrite();
        RenderUtility.drawRectW(textX - 0.4f, textY - 0.2f, maxWidth + 0.8f, textSize + 2.0f, -1);
        Stencil.readStencilBuffer(1);
        if (focused && selectionEnd > selectionStart && !visible.isEmpty()) {
            int visibleStart = trimStart;
            int visibleEnd = trimStart + visible.length();
            int clippedStart = MathHelper.clamp(selectionStart, visibleStart, visibleEnd);
            int clippedEnd = MathHelper.clamp(selectionEnd, visibleStart, visibleEnd);
            if (clippedEnd > clippedStart) {
                int relativeStart = clippedStart - visibleStart;
                int relativeEnd = clippedEnd - visibleStart;
                float selX1 = textX + Fonts.sfuy.getWidth(visible.substring(0, relativeStart), textSize);
                float selX2 = textX + Fonts.sfuy.getWidth(visible.substring(0, relativeEnd), textSize);
                float selW = Math.max(1.0f, selX2 - selX1);
                RenderUtility.drawRoundedRect(
                        selX1,
                        textY - 1.55f,
                        selW,
                        textSize + 3.1f,
                        2.0f,
                        ColorUtils.setAlpha(Theme.MainColor(0), (int) (100 * alpha))
                );
            }
        }

        if (!visible.isEmpty()) {
            Fonts.sfuy.drawText(stack, visible, textX, glyphY, textColor, textSize);
        }

        if (focused && ((System.currentTimeMillis() / 450L) % 2L == 0L)) {
            int visibleCursor = MathHelper.clamp(cursor - trimStart, 0, visible.length());
            String beforeCursor = visible.substring(0, visibleCursor);
            float caretX = textX + Fonts.sfuy.getWidth(beforeCursor, textSize) + 0.6f;
            RenderUtility.drawRectW(caretX, textY - 0.1f, 0.8f, textSize + 1.8f, ColorUtils.setAlpha(caretColor, (int)(220 * alpha)));
        }
        Stencil.uninitStencilBuffer();
    }

    private int getTextFieldSelectionPos(TextFieldWidget field) {
        if (field == null) {
            return 0;
        }
        if (!moduleSearchSelectionFieldInit) {
            moduleSearchSelectionFieldInit = true;
            try {
                moduleSearchSelectionEndField = TextFieldWidget.class.getDeclaredField("selectionEnd");
                moduleSearchSelectionEndField.setAccessible(true);
            } catch (Exception ignored) {
                moduleSearchSelectionEndField = null;
            }
        }
        if (moduleSearchSelectionEndField != null) {
            try {
                return moduleSearchSelectionEndField.getInt(field);
            } catch (Exception ignored) {
            }
        }
        return field.getCursorPosition();
    }

    private int[] getTextFieldSelectionRange(TextFieldWidget field, String fullText) {
        int cursor = MathHelper.clamp(field.getCursorPosition(), 0, fullText.length());
        int reflectedSelection = MathHelper.clamp(getTextFieldSelectionPos(field), 0, fullText.length());
        if (reflectedSelection != cursor) {
            return new int[] {Math.min(cursor, reflectedSelection), Math.max(cursor, reflectedSelection)};
        }

        String selected = field.getSelectedText();
        if (selected == null || selected.isEmpty()) {
            return new int[] {cursor, cursor};
        }

        int len = selected.length();
        boolean canLeft = cursor - len >= 0;
        boolean canRight = cursor + len <= fullText.length();
        boolean leftMatch = canLeft && fullText.substring(cursor - len, cursor).equals(selected);
        boolean rightMatch = canRight && fullText.substring(cursor, cursor + len).equals(selected);

        if (leftMatch && !rightMatch) {
            return new int[] {cursor - len, cursor};
        }
        if (rightMatch && !leftMatch) {
            return new int[] {cursor, cursor + len};
        }
        if (leftMatch) {
            return new int[] {cursor - len, cursor};
        }
        if (canRight) {
            return new int[] {cursor, cursor + len};
        }
        if (canLeft) {
            return new int[] {cursor - len, cursor};
        }
        return new int[] {cursor, cursor};
    }

    private void syncModuleSearchFieldGeometry() {
        if (moduleSearchField == null) {
            return;
        }

        int searchX = 8;
        int searchY = height - Math.round(MODULE_SEARCH_AREA_HEIGHT) - 4;
        int searchWidth = Math.max(40, width - 16);
        moduleSearchField.setX(searchX);
        moduleSearchField.y = searchY;
        moduleSearchField.setWidth(searchWidth);
    }

    private void setModuleSearchFocused(boolean focused) {
        if (moduleSearchField != null) {
            moduleSearchField.setFocused2(focused);
        }
        if (!focused) {
            moduleSearchSelectionDragging = false;
            resetModuleSearchKeyRepeat();
        }
    }

    private void updateModuleSearchSelectionFromMouse(double mouseX) {
        if (moduleSearchField == null) {
            return;
        }

        String fullText = moduleSearchField.getText();
        int len = fullText.length();
        float textStartX = moduleSearchField.x;
        float localX = (float) mouseX - textStartX;
        int targetCursor;
        if (localX <= 0f) {
            targetCursor = 0;
        } else {
            targetCursor = len;
            float prevWidth = 0f;
            for (int i = 1; i <= len; i++) {
                float currentWidth = Fonts.sfuy.getWidth(fullText.substring(0, i), 6.0f);
                float midpoint = prevWidth + (currentWidth - prevWidth) * 0.5f;
                if (localX < midpoint) {
                    targetCursor = i - 1;
                    break;
                }
                prevWidth = currentWidth;
            }
        }

        targetCursor = MathHelper.clamp(targetCursor, 0, len);
        moduleSearchField.setCursorPosition(targetCursor);
        moduleSearchField.setSelectionPos(moduleSearchSelectionAnchor);
    }

    private void resetModuleGridScroll() {
        scroll = 0;
        animatedScroll = 0;
        moduleScrollDragging = false;
    }

    private void invalidateVisibleModules() {
        visibleModulesDirty = true;
    }

    private float getMainGridContentHeight(float contentHeight) {
        return contentHeight;
    }

    private List<Module> getVisibleModules() {
        String query = moduleSearchField == null ? "" : moduleSearchField.getText().trim().toLowerCase(Locale.ROOT);
        List<Module> modules = query.isEmpty()
                ? Harmony.getInstance().getModuleManager().getModulesByCategory(selectedCategory)
                : Harmony.getInstance().getModuleManager().getSortedAlphabetically();

        if (!visibleModulesDirty
                && selectedCategory == visibleModulesCacheCategory
                && query.equals(visibleModulesCacheQuery)
                && modules.size() == visibleModulesCacheSourceSize) {
            return visibleModulesCache;
        }

        visibleModulesCache.clear();
        for (Module module : modules) {
            if (!module.isVisibleInClickGui()) {
                continue;
            }
            if (query.isEmpty() || module.getName().toLowerCase(Locale.ROOT).contains(query)) {
                visibleModulesCache.add(module);
            }
        }

        visibleModulesCacheCategory = selectedCategory;
        visibleModulesCacheQuery = query;
        visibleModulesCacheSourceSize = modules.size();
        visibleModulesDirty = false;
        return visibleModulesCache;
    }

    private void updateAutoBuyScrollFromDrag(double mouseY, float panelY) {
        xd.harm.modules.impl.player.AutoBuy autoBuy = Harmony.getInstance().getModuleManager().getAutoBuy();
        List<xd.harm.modules.impl.player.autobuy.AutoBuyItem> items = autoBuy.getManager().getItems();

        float contentTop = panelY + HEADER_HEIGHT + 2;
        float contentBottom = panelY + PANEL_HEIGHT - 4;
        float contentHeight = contentBottom - contentTop;
        float pad = 6;
        float toggleH = 14;
        float toggleY = contentTop + 2;
        float gridTop = toggleY + toggleH + 4;
        float gridBottom = contentTop + contentHeight;
        float gridH = gridBottom - gridTop;

        int rows = (int) Math.ceil(items.size() / (float) AB_COLS);
        float totalGridH = rows * (AB_ITEM_SIZE + AB_ITEM_GAP);
        float maxScr = Math.max(0, totalGridH - gridH + 6);
        if (maxScr <= 0) {
            autoBuyScroll = 0;
            return;
        }

        float trackY = gridTop + 2.0f;
        float trackH = gridH - 4.0f;
        float thumbH = Math.max(14.0f, (trackH / totalGridH) * trackH);
        float thumbRange = Math.max(0.0f, trackH - thumbH);
        if (thumbRange <= 0.0f) {
            autoBuyScroll = 0;
            return;
        }

        float thumbY = MathHelper.clamp((float) mouseY - autoBuyScrollDragOffset, trackY, trackY + thumbRange);
        float progress = (thumbY - trackY) / thumbRange;
        autoBuyScroll = MathHelper.clamp(-maxScr * progress, -maxScr, 0);
    }

    private void updateConfigScrollFromDrag(double mouseY, float panelY) {
        float contentTop = panelY + HEADER_HEIGHT + 2;
        float contentBottom = panelY + PANEL_HEIGHT - 4;
        float contentHeight = contentBottom - contentTop;

        float inputH = 15;
        float inputY = contentTop + 4;
        float btnH = 12;
        float btnY = inputY + inputH + 4;
        float listY = btnY + btnH + 5;
        float listH = contentTop + contentHeight - listY;

        List<xd.harm.config.Config> configs = cachedConfigs != null ? cachedConfigs : new ArrayList<>();
        int cols = 3;
        int rows = (configs.size() + cols - 1) / cols;
        float totalH = rows * (50.0f + 6.0f);
        float maxScr = Math.max(0, totalH - listH);
        if (maxScr <= 0) {
            configScroll = 0;
            return;
        }

        float trackY = listY + 2.0f;
        float trackH = listH - 4.0f;
        float thumbH = Math.max(14.0f, (trackH / totalH) * trackH);
        float thumbRange = Math.max(0.0f, trackH - thumbH);
        if (thumbRange <= 0.0f) {
            configScroll = 0;
            return;
        }

        float thumbY = MathHelper.clamp((float) mouseY - configScrollDragOffset, trackY, trackY + thumbRange);
        float progress = (thumbY - trackY) / thumbRange;
        configScroll = MathHelper.clamp(-maxScr * progress, -maxScr, 0);
    }

    private void renderRotatingAutoBuyItem(ItemStack itemStack, float x, float y, float scale, float rotation) {
        if (itemStack == null || itemStack.isEmpty()) return;
        RenderSystem.pushMatrix();
        RenderSystem.translatef(x, y, 280.0f);
        RenderSystem.scalef(scale, -scale, scale);
        RenderSystem.rotatef(rotation, 0, 1, 0);
        RenderSystem.rotatef(15.0f, 1, 0, 0);
        RenderSystem.enableDepthTest();
        IRenderTypeBuffer.Impl buffer = minecraft.getRenderTypeBuffers().getBufferSource();
        IBakedModel model = minecraft.getItemRenderer().getItemModelWithOverrides(itemStack, null, minecraft.player);
        minecraft.getItemRenderer().renderItem(itemStack, ItemCameraTransforms.TransformType.GUI, false, new MatrixStack(), buffer, 15728880, OverlayTexture.NO_OVERLAY, model);
        buffer.finish();
        RenderSystem.disableDepthTest();
        RenderSystem.popMatrix();
    }

    private void renderClickRipples(float panelX, float panelY, float alpha) {
        if (clickRipples.isEmpty()) return;

        Iterator<Ripple> iterator = clickRipples.iterator();
        while (iterator.hasNext()) {
            Ripple ripple = iterator.next();
            if (ripple.isFinished()) {
                iterator.remove();
            }
        }

        if (clickRipples.isEmpty()) return;

        Stencil.initStencilToWrite();
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.01f);
        RenderUtility.drawRoundedRect(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, PANEL_RADIUS, -1);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        Stencil.readStencilBuffer(1);
        for (Ripple ripple : clickRipples) {
            float progress = ripple.getProgress();
            float radius = 230.0f * progress;
            int color = ColorUtils.setAlpha(Theme.MainColor(0), (int)(145 * (1.0f - progress) * alpha));
            drawRadialGradient(ripple.x, ripple.y, radius, color, ColorUtils.setAlpha(color, 0));
        }
        Stencil.uninitStencilBuffer();

        Stencil.initStencilToWrite();
        float topBarW = Math.min(PANEL_WIDTH, TOP_BAR_WIDTH);
        float topBarX = getStaticTopTabBarX();
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.01f);
        RenderUtility.drawRoundedRect(topBarX, 4, topBarW, 18.0f, 5.0f, -1);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        Stencil.readStencilBuffer(1);
        for (Ripple ripple : clickRipples) {
            float progress = ripple.getProgress();
            float radius = 130.0f * progress;
            int color = ColorUtils.setAlpha(Theme.MainColor(0), (int)(115 * (1.0f - progress) * alpha));
            drawRadialGradient(ripple.x, ripple.y, radius, color, ColorUtils.setAlpha(color, 0));
        }
        Stencil.uninitStencilBuffer();
    }

    private void drawRadialGradient(float x, float y, float radius, int startColor, int endColor) {
        float r1 = (float)(startColor >> 16 & 255) / 255.0F;
        float g1 = (float)(startColor >> 8 & 255) / 255.0F;
        float b1 = (float)(startColor & 255) / 255.0F;
        float a1 = (float)(startColor >> 24 & 255) / 255.0F;
        float r2 = (float)(endColor >> 16 & 255) / 255.0F;
        float g2 = (float)(endColor >> 8 & 255) / 255.0F;
        float b2 = (float)(endColor & 255) / 255.0F;
        float a2 = (float)(endColor >> 24 & 255) / 255.0F;

        GlStateManager.disableTexture();
        GlStateManager.enableBlend();
        GlStateManager.disableAlphaTest();
        GlStateManager.blendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(7425);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glColor4f(r1, g1, b1, a1);
        GL11.glVertex2f(x, y);
        GL11.glColor4f(r2, g2, b2, a2);
        for (int i = 0; i <= 360; i += 5) {
            double angle = Math.toRadians(i);
            GL11.glVertex2d(x + Math.sin(angle) * radius, y + Math.cos(angle) * radius);
        }
        GL11.glEnd();
        GlStateManager.shadeModel(7424);
        GlStateManager.disableBlend();
        GlStateManager.enableAlphaTest();
        GlStateManager.enableTexture();
    }

    private float snapHalf(float value) {
        return Math.round(value * 2.0f) / 2.0f;
    }

    private float getTopTabCenterX(float startX, float spacing, int tabIndex) {
        return snapHalf(startX + tabIndex * spacing + getTopTabIconOffsetX(tabIndex));
    }

    private float getTopTabIconOffsetX(int tabIndex) {
        if (tabIndex < 0 || tabIndex >= TOP_TAB_ICON_OFFSETS_X.length) {
            return 0.0f;
        }
        return TOP_TAB_ICON_OFFSETS_X[tabIndex];
    }

    private float getTopTabPillOffsetX(int tabIndex) {
        switch (tabIndex) {
            case TAB_MAIN:
                return TOP_TAB_PILL_OFFSET_MAIN_X;
            case TAB_CONFIGS:
                return TOP_TAB_PILL_OFFSET_CONFIGS_X;
            case TAB_AUTOBUY:
                return TOP_TAB_PILL_OFFSET_AUTOBUY_X;
            case TAB_THEME:
                return TOP_TAB_PILL_OFFSET_THEME_X;
            case TAB_BOTCONFIGS:
                return TOP_TAB_PILL_OFFSET_BOTCONFIGS_X;
            default:
                return 0.0f;
        }
    }

    private void renderThemePanel(MatrixStack stack, float x, float contentTop, float contentHeight, int mouseX, int mouseY, float alpha) {
        String[] themeNames = {"Лиловое сияние", "Синий с белым", "Милое ^_^", "Пламя", "Аквамарин", "Токсичное",
                "Синие Комбо", "Малиновое", "Кровавое", "Аметист", "Апельсиновое", "Свой"};
        if (Theme.THEME.strings != null && Theme.THEME.strings.length > 0) {
            themeNames = Theme.THEME.strings;
        }

        int cols = 3;
        float pad = 10;
        float gap = 7;
        float cardW = (PANEL_WIDTH - pad * 2 - gap * (cols - 1)) / cols;
        float cardH = 34;

        String currentTheme = Theme.THEME.get();

        int rows = (int) Math.ceil(themeNames.length / (float) cols);
        float totalHeight = rows * (cardH + gap) - gap;
        float gridClipY = contentTop + 8;
        float gridClipHeight = contentHeight - 8;
        float maxScroll = Math.max(0, totalHeight - gridClipHeight + 4);
        themePanelScroll = MathHelper.clamp(themePanelScroll, -maxScroll, 0);
        themePanelAnimatedScroll = AnimationMath.fast(themePanelAnimatedScroll, themePanelScroll, 15);

        Stencil.initStencilToWrite();
        RenderUtility.drawRoundedRect(x + pad, gridClipY, PANEL_WIDTH - pad * 2, gridClipHeight, 4.0f, -1);
        Stencil.readStencilBuffer(1);

        for (int i = 0; i < themeNames.length; i++) {
            int col = i % cols;
            int row = i / cols;
            float cx = x + pad + col * (cardW + gap);
            float cy = gridClipY + row * (cardH + gap) + themePanelAnimatedScroll;

            boolean selected = themeNames[i].equals(currentTheme);
            boolean hovered = RenderUtility.isInRegion(mouseX, mouseY, cx, cy, cardW, cardH);

            Theme.THEME.set(themeNames[i]);
            int mainColor = Theme.MainColor(0);
            int cr = Math.min(255, (int)(ColorUtils.getRed(mainColor) * 1.2f));
            int cg = Math.min(255, (int)(ColorUtils.getGreen(mainColor) * 1.2f));
            int cb = Math.min(255, (int)(ColorUtils.getBlue(mainColor) * 1.2f));
            Theme.THEME.set(currentTheme);

            int cardGlass = ColorUtils.rgba(255, 255, 255, (int)((hovered ? 36 : 24) * alpha));
            RenderUtility.drawRoundedRect(cx, cy, cardW, cardH, 5.0f, cardGlass);
            int tintAlpha = selected ? 92 : 122;
            RenderUtility.drawRoundedRect(cx, cy, cardW, cardH, 5.0f, ColorUtils.rgba(12, 14, 19, (int)(tintAlpha * alpha)));

            if (selected) {
                RenderUtility.drawShadow(cx - 0.3f, cy - 0.3f, cardW + 0.6f, cardH + 0.6f, 10,
                        ColorUtils.rgba(cr, cg, cb, (int)(95 * alpha)));
                drawRadialGradient(cx + cardW / 2f, cy + cardH / 2f, cardW * 0.55f,
                        ColorUtils.rgba(cr, cg, cb, (int)(52 * alpha)),
                        ColorUtils.rgba(cr, cg, cb, 0));
                RenderUtility.drawRoundedRectOutline(cx, cy, cardW, cardH, 5.0f, 1.0f, ColorUtils.rgba(cr, cg, cb, (int)(235 * alpha)));
            } else {
                RenderUtility.drawRoundedRectOutline(cx, cy, cardW, cardH, 5.0f, 0.6f, ColorUtils.rgba(255, 255, 255, (int)(28 * alpha)));
            }

            float swatchSize = 10;
            float swatchX = cx + 7;
            float swatchY = cy + (cardH - swatchSize) / 2f;
            int swatchColor = ColorUtils.rgba(cr, cg, cb, (int)(255 * alpha));
            RenderUtility.drawRoundedRect(swatchX, swatchY, swatchSize, swatchSize, 2.0f, swatchColor);
            RenderUtility.drawRoundedRectOutline(swatchX, swatchY, swatchSize, swatchSize, 2.0f, 0.6f,
                    ColorUtils.rgba(255, 255, 255, (int)(52 * alpha)));

            if (selected) {
                RenderUtility.drawShadow(swatchX - 2, swatchY - 2, swatchSize + 4, swatchSize + 4, 7,
                        ColorUtils.rgba(cr, cg, cb, (int)(80 * alpha)));
            }

            Fonts.sfuy.drawText(stack, themeNames[i], cx + 21, cy + cardH / 2f - 3f,
                    ColorUtils.rgba(255, 255, 255, (int)((selected ? 242 : 165) * alpha)), 5.5f);
        }

        if (maxScroll > 0.0f) {
            float trackX = x + PANEL_WIDTH - pad - 3.2f;
            float trackY = gridClipY + 1.0f;
            float trackW = 2.2f;
            float trackH = gridClipHeight - 2.0f;
            float thumbH = Math.max(14.0f, (trackH / (totalHeight + 4)) * trackH);
            float thumbRange = Math.max(0.0f, trackH - thumbH);
            float progress = MathHelper.clamp(-themePanelAnimatedScroll / maxScroll, 0.0f, 1.0f);
            float thumbY = trackY + thumbRange * progress;
            RenderUtility.drawRoundedRect(trackX, trackY, trackW, trackH, 1.1f,
                    ColorUtils.rgba(255, 255, 255, (int)(16 * alpha)));
            RenderUtility.drawRoundedRect(trackX, thumbY, trackW, thumbH, 1.1f,
                    ColorUtils.rgba(255, 255, 255, (int)(80 * alpha)));
        }

        Stencil.uninitStencilBuffer();

        if (isMenuPanelCustomThemeSelected()) {
            syncMenuThemeCustomColors();
            updateThemeCustomPickerLayout(x, contentTop, contentHeight, themeNames.length);
        }
    }

    private boolean handleThemeClick(double mouseX, double mouseY, int button, float x, float y) {
        String[] themeNames = {"Лиловое сияние", "Синий с белым", "Милое ^_^", "Пламя", "Аквамарин", "Токсичное",
                "Синие Комбо", "Малиновое", "Кровавое", "Аметист", "Апельсиновое", "Свой"};
        if (Theme.THEME.strings != null && Theme.THEME.strings.length > 0) {
            themeNames = Theme.THEME.strings;
        }

        float contentTop = y + HEADER_HEIGHT + 2;
        float contentHeight = y + PANEL_HEIGHT - 4 - contentTop;

        if (handleThemeCustomPickerClick((float) mouseX, (float) mouseY, button, x, contentTop, contentHeight, themeNames.length)) {
            return true;
        }

        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }

        int cols = 3;
        float pad = 10;
        float gap = 7;
        float cardW = (PANEL_WIDTH - pad * 2 - gap * (cols - 1)) / cols;
        float cardH = 34;

        for (int i = 0; i < themeNames.length; i++) {
            int col = i % cols;
            int row = i / cols;
            float cx = x + pad + col * (cardW + gap);
            float cy = contentTop + 8 + row * (cardH + gap) + themePanelAnimatedScroll;

            if (RenderUtility.isInRegion(mouseX, mouseY, cx, cy, cardW, cardH)) {
                Theme.THEME.set(themeNames[i]);
                return true;
            }
        }
        return false;
    }

    private boolean isMenuPanelCustomThemeSelected() {
        String[] names = Theme.THEME.strings;
        if (names == null || names.length == 0) {
            return false;
        }
        return Theme.THEME.is(names[names.length - 1]);
    }

    private void syncMenuThemeCustomColors() {
        int unified = Theme.visualscolor.get();
        if (Theme.textcolor.get() != unified) {
            Theme.textcolor.set(unified);
        }
        if (Theme.rectcolor.get() != unified) {
            Theme.rectcolor.set(unified);
        }
    }

    private ThemeCustomPickerLayout getThemeCustomPickerLayout(float panelX, float contentTop, float contentHeight, int themeCount) {
        int cols = 3;
        float gap = 7;
        float cardH = 34;
        int rows = (int) Math.ceil(themeCount / (float) cols);
        float cardsBottom = contentTop + 8 + rows * cardH + Math.max(0, rows - 1) * gap;

        float panelMargin = 3f;
        float pickerHeight = 62f;
        float pickerX = panelX + 10f;
        float pickerWidth = PANEL_WIDTH - 20f;
        float pickerY = cardsBottom + panelMargin;

        float maxY = contentTop + contentHeight - pickerHeight - 3f;
        if (pickerY > maxY) {
            pickerY = maxY;
        }
        return new ThemeCustomPickerLayout(pickerX, pickerY, pickerWidth, pickerHeight);
    }

    private void layoutThemeCustomPicker(ThemeCustomPickerLayout layout) {
        float pickerWidth = 65.0f;
        float anchorX = layout.x + Math.max(0.0f, (layout.width - pickerWidth) / 2.0f);
        float anchorY = layout.y;

        if (!Float.isNaN(themeCustomPickerAnchorX) && !Float.isNaN(themeCustomPickerAnchorY)) {
            themeCustomPickerOffsetX = themeCustomPicker.getX() - themeCustomPickerAnchorX;
            themeCustomPickerOffsetY = themeCustomPicker.getY() - themeCustomPickerAnchorY;
        }

        float pickerHeight = Math.max(layout.height, themeCustomPicker.getHeight() > 0.0f ? themeCustomPicker.getHeight() : layout.height);
        float screenW = minecraft.getMainWindow().getScaledWidth();
        float screenH = minecraft.getMainWindow().getScaledHeight();

        float pickerX = MathHelper.clamp(anchorX + themeCustomPickerOffsetX, 4.0f, Math.max(4.0f, screenW - pickerWidth - 4.0f));
        float pickerY = MathHelper.clamp(anchorY + themeCustomPickerOffsetY, 4.0f, Math.max(4.0f, screenH - pickerHeight - 4.0f));

        themeCustomPickerOffsetX = pickerX - anchorX;
        themeCustomPickerOffsetY = pickerY - anchorY;
        themeCustomPickerAnchorX = anchorX;
        themeCustomPickerAnchorY = anchorY;

        themeCustomPicker.setX(pickerX);
        themeCustomPicker.setY(pickerY);
        themeCustomPicker.setWidth(pickerWidth);
    }

    private void updateThemeCustomPickerLayout(float panelX, float contentTop, float contentHeight, int themeCount) {
        ThemeCustomPickerLayout layout = getThemeCustomPickerLayout(panelX, contentTop, contentHeight, themeCount);
        layoutThemeCustomPicker(layout);
    }

    private boolean handleThemeCustomPickerClick(float mouseX, float mouseY, int button, float panelX, float contentTop, float contentHeight, int themeCount) {
        if (!isMenuPanelCustomThemeSelected()) {
            return false;
        }
        updateThemeCustomPickerLayout(panelX, contentTop, contentHeight, themeCount);

        if (themeCustomPicker.handleClick((int) mouseX, (int) mouseY)) {
            return true;
        }

        return false;
    }

    private boolean dispatchThemeCustomPickerRelease(float mouseX, float mouseY, int button, float panelX, float panelY) {
        if (!isMenuPanelCustomThemeSelected()) {
            return false;
        }

        String[] themeNames = Theme.THEME.strings;
        if (themeNames == null || themeNames.length == 0) {
            return false;
        }

        float contentTop = panelY + HEADER_HEIGHT + 2;
        float contentHeight = panelY + PANEL_HEIGHT - 4 - contentTop;
        updateThemeCustomPickerLayout(panelX, contentTop, contentHeight, themeNames.length);

        boolean hovered = themeCustomPicker.isMouseOverFloatingWindow(mouseX, mouseY);
        boolean dragging = themeCustomPicker.isDraggingFloating();
        if (!hovered && !dragging) {
            return false;
        }

        themeCustomPicker.mouseRelease(mouseX, mouseY, button);
        return true;
    }

    private boolean dispatchThemeCustomPickerDrag(float mouseX, float mouseY, int button, float panelX, float panelY) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT || !isMenuPanelCustomThemeSelected()) {
            return false;
        }

        String[] themeNames = Theme.THEME.strings;
        if (themeNames == null || themeNames.length == 0) {
            return false;
        }

        float contentTop = panelY + HEADER_HEIGHT + 2;
        float contentHeight = panelY + PANEL_HEIGHT - 4 - contentTop;
        updateThemeCustomPickerLayout(panelX, contentTop, contentHeight, themeNames.length);

        boolean hovered = themeCustomPicker.isMouseOverFloatingWindow(mouseX, mouseY);
        boolean dragging = themeCustomPicker.isDraggingFloating();
        if (!hovered && !dragging) {
            return false;
        }

        themeCustomPicker.handleClick((int) mouseX, (int) mouseY);
        return true;
    }

    private void renderMainTabOverlays(MatrixStack stack, float mouseX, float mouseY) {
        if (selectedTab == TAB_MAIN) {
            for (int ci = 0; ci < settingsComponents.size(); ci++) {
                Component comp = settingsComponents.get(ci);
                if (!isHiddenByCollapsedCategory(ci) && comp instanceof ColorComponent) {
                    ((ColorComponent) comp).renderFloatingWindow(stack, mouseX, mouseY);
                }
            }
            MultiBoxOverlayManager.renderAll(stack, mouseX, mouseY);
        } else if (selectedTab == TAB_THEME) {
            if (isMenuPanelCustomThemeSelected()) {
                themeCustomPicker.render(stack, mouseX, mouseY);
            }
        }
    }

    private void renderClickGuiSelectedImage(MatrixStack stack, float alpha) {
        if (!ClickGui.showImage.get() || alpha <= 0.01f) {
            return;
        }

        renderAnchoredClickGuiImage(ClickGui.getSelectedImageResource(), getSelectedClickGuiImageLayout(), alpha, false);
    }

    private void renderClickGuiSelectedHighImage(MatrixStack stack, float alpha) {
        if (!ClickGui.showHighImage.get() || alpha <= 0.01f) {
            return;
        }

        ResourceLocation image = ClickGui.getSelectedHighImageResource();
        ClickGuiImageLayout layout = getSelectedClickGuiHighImageLayout();
        float slideY = (1.0f - alpha) * 15.0f;
        double idleTime = System.currentTimeMillis() / 700.0;
        float idlePhase = (float) ((Math.sin(idleTime) + 1.0) * 0.5);
        idlePhase *= idlePhase;
        float idleDrop = idlePhase * 2.2f * alpha;
        float renderWidth = layout.width;
        float renderHeight = layout.height;
        float imageX = getAnimatedPanelX() + layout.offsetX;
        float imageY = getAnimatedPanelY(alpha) + layout.offsetY + idleDrop;

        RenderUtility.drawImageAlphaSmooth(image, imageX, imageY, renderWidth, renderHeight,
                ColorUtils.rgba(255, 255, 255, (int)(255 * alpha)));
    }

    private void renderAnchoredClickGuiImage(ResourceLocation image, ClickGuiImageLayout layout, float alpha, boolean topLeft) {
        float availableWidth = width - CLICKGUI_IMAGE_MARGIN * 2.0f;
        float availableHeight = height - CLICKGUI_IMAGE_MARGIN * 2.0f;
        if (availableWidth <= 36.0f || availableHeight <= 36.0f) {
            return;
        }

        float scale = Math.min(1.0f, Math.min(availableWidth / layout.width, availableHeight / layout.height));
        scale = Math.min(scale, 1.0f);
        if (scale <= 0.0f) {
            return;
        }

        float imageWidth = layout.width * scale;
        float imageHeight = layout.height * scale;
        float imageX;
        float imageY;

        if (topLeft) {
            imageX = CLICKGUI_IMAGE_MARGIN + layout.offsetX - (1.0f - alpha) * 20.0f;
            imageY = CLICKGUI_IMAGE_MARGIN + layout.offsetY;
        } else {
            imageX = width - imageWidth - CLICKGUI_IMAGE_MARGIN - layout.offsetX + (1.0f - alpha) * 20.0f;
            imageY = height - imageHeight - CLICKGUI_IMAGE_MARGIN - layout.offsetY;
        }

        RenderUtility.drawImageAlpha(image, imageX, imageY, imageWidth, imageHeight,
                ColorUtils.rgba(255, 255, 255, (int)(255 * alpha)));
    }

    private float getAnimatedPanelX() {
        return panelPosX;
    }

    private float getAnimatedPanelY(float alpha) {
        return panelPosY + (1.0f - MathHelper.clamp(alpha, 0.0f, 1.0f)) * 15.0f;
    }

    private ClickGuiImageLayout getSelectedClickGuiImageLayout() {
        String selectedImage = ClickGui.normalizeImageName(ClickGui.imageMode.get());
        for (ClickGuiImageLayout layout : CLICKGUI_IMAGE_LAYOUTS) {
            if (layout.fileName.equalsIgnoreCase(selectedImage)) {
                return layout;
            }
        }
        return CLICKGUI_IMAGE_LAYOUTS[0];
    }

    private ClickGuiImageLayout getSelectedClickGuiHighImageLayout() {
        String selectedImage = ClickGui.normalizeImageName(ClickGui.highImageMode.get());
        if ("2.png".equalsIgnoreCase(selectedImage)) {
            return createHighImageLayout("2.png", 100.0f, 0.0f, -48.0f);
        }
        return createHighImageLayout("1.png", 130.0f, 0.0f, -48.0f);
    }

    private ClickGuiImageLayout createHighImageLayout(String fileName, float size, float offsetX, float offsetY) {
        return new ClickGuiImageLayout(fileName, size, size * getHighImageAspect(fileName), offsetX, offsetY);
    }

    private float getHighImageAspect(String fileName) {
        if ("2.png".equalsIgnoreCase(fileName)) {
            return 157.0f / 284.0f;
        }
        return 329.0f / 759.0f;
    }

    private boolean dispatchOpenedColorPickerClick(float mouseX, float mouseY, int button) {
        ColorComponent opened = ColorComponent.getOpened();
        if (opened == null) {
            return false;
        }

        boolean overColorRect = opened.isMouseOverColorRect(mouseX, mouseY);
        boolean overFloatingWindow = opened.isMouseOverFloatingWindow(mouseX, mouseY);
        if (overColorRect || overFloatingWindow) {
            opened.mouseClick(mouseX, mouseY, button);
            return true;
        }

        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && opened.isPanelOpened()) {
            ColorComponent.closeAll();
        }
        return false;
    }

    private boolean dispatchOpenedColorPickerRelease(float mouseX, float mouseY, int button) {
        ColorComponent opened = ColorComponent.getOpened();
        if (opened == null) {
            return false;
        }

        boolean overColorRect = opened.isMouseOverColorRect(mouseX, mouseY);
        boolean overFloatingWindow = opened.isMouseOverFloatingWindow(mouseX, mouseY);
        boolean dragging = opened.isDraggingFloating();
        if (!overColorRect && !overFloatingWindow && !dragging) {
            return false;
        }

        opened.mouseRelease(mouseX, mouseY, button);
        return true;
    }

    private void closeSettingsOverlays() {
        ColorComponent.closeAll();
        MultiBoxComponent.closeAllOpen();
    }

    private void renderSnow(float px, float py, float pw, float ph, float radius, float alpha) {
        if (snowflakes.isEmpty()) {
            for (int i = 0; i < SNOW_COUNT; i++) {
                Snowflake s = new Snowflake();
                s.x = snowRandom.nextFloat() * pw;
                s.y = snowRandom.nextFloat() * ph;
                s.speed = 0.3f + snowRandom.nextFloat() * 0.5f;
                s.size = 1.2f + snowRandom.nextFloat() * 1.8f;
                s.drift = (snowRandom.nextFloat() - 0.5f) * 0.3f;
                s.phase = snowRandom.nextFloat() * (float)(Math.PI * 2);
                s.alpha = 0.3f + snowRandom.nextFloat() * 0.5f;
                snowflakes.add(s);
            }
        }

        long now = System.currentTimeMillis();
        float dt = Math.min((now - lastSnowUpdate) / 16.0f, 3.0f);
        lastSnowUpdate = now;

        for (Snowflake s : snowflakes) {
            s.y += s.speed * dt;
            s.x += (s.drift + (float)Math.sin(now / 1500.0 + s.phase) * 0.15f) * dt;

            if (s.y > ph + 2) {
                s.y = -2;
                s.x = snowRandom.nextFloat() * pw;
            }
            if (s.x < -2) s.x = pw + 2;
            if (s.x > pw + 2) s.x = -2;
        }

        int themeColor = Theme.MainColor(0);

        for (Snowflake s : snowflakes) {
            float sx2 = px + s.x;
            float sy2 = py + s.y;
            float a = s.alpha * alpha;

            RenderUtility.drawShadow(
                    sx2 - s.size, sy2 - s.size,
                    s.size * 2, s.size * 2,
                    4,
                    ColorUtils.setAlpha(themeColor, (int)(40 * a))
            );

            RenderUtility.drawRoundedRect(
                    sx2 - s.size / 2f, sy2 - s.size / 2f,
                    s.size, s.size,
                    s.size / 2f,
                    ColorUtils.rgba(255, 255, 255, (int)(200 * a))
            );
        }
    }

    private static class ThemeCustomPickerLayout {
        final float x;
        final float y;
        final float width;
        final float height;

        ThemeCustomPickerLayout(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }

    private static class ClickGuiImageLayout {
        final String fileName;
        final float width;
        final float height;
        final float offsetX;
        final float offsetY;

        ClickGuiImageLayout(String fileName, float width, float height, float offsetX, float offsetY) {
            this.fileName = fileName;
            this.width = width;
            this.height = height;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
        }
    }

    private static class AutoBuyField extends TextFieldWidget {
        public AutoBuyField(FontRenderer font, int x, int y, int width, int height, net.minecraft.util.text.ITextComponent title) {
            super(font, x, y, width, height, title);
        }
    }

    private static class Ripple {
        final float x;
        final float y;
        final long startTime;

        Ripple(float x, float y) {
            this.x = x;
            this.y = y;
            this.startTime = System.currentTimeMillis();
        }

        float getProgress() {
            return Math.min((System.currentTimeMillis() - startTime) / 650.0f, 1.0f);
        }

        boolean isFinished() {
            return System.currentTimeMillis() - startTime > 650;
        }
    }

    private static class ConfigMeta {
        long createdAt;
        int order;
        String tags = "";
    }

    private static class ConfigMetaStore {
        private static final JsonParser PARSER = new JsonParser();
        private static final byte[] META_MASK = "HarmonyCardsMaskV1".getBytes(StandardCharsets.UTF_8);
        private static final String DEFAULT_META_PAYLOAD = "M0MRAgEIECQSUF4oNkMdCjtUaltQDw4NEjYRLQccPRgVDDJXOxIUCRwIHTAFFBcXKxIXDXQdagIACA4aHCcgBkZJfFZEXGQDfVJHWFxfSG9DHRYXKBNRUWIdahUTChxMQ2FDD0gIbw8SBjMTckMQDAwFDDM+EQsDNAcUDzBCOwcWHgkKCicHAQAVPgUVGDJCLAcBCQlMVWECAAESOQQXKiITclBFWlhcS3ZSR1JAdFNfSTlDLAQAT1VeVWEVEwMAb1tRSSsdM0McDAILW3lDEAUQJhQDNDVeOBhQQU0NCyYABgEXDBVRUWcGf1ZAXVhaSHFUQ1dfbw4BDzNDaltDQU0aGCQSUF5RPQgXBCQdaA8bCggPWz5NCUYdLAwWSWwTKgARBhoeJiAOAh0VKgUVGCVXLBIUCU1CWyATFwUHKAUyH3QLeVZFWl1cTHBURlBBe01RBCRVLRNQV11CWzcAFRdRd0NRFnpKag8TAApMQ2EDEwcYOBEsCDlBMQcVCQlMVWECAAESOQQXKiITclBFWlhcS3ZSR1ZGelVfSTlDLAQAT1VdVWEVEwMAb1tRSStsNQ==";
        private final Map<String, ConfigMeta> entries = new HashMap<>();
        private boolean loaded;

        ConfigMetaStore() {
        }

        ConfigMeta get(String name) {
            load();
            return entries.computeIfAbsent(name, key -> {
                ConfigMeta meta = new ConfigMeta();
                meta.createdAt = System.currentTimeMillis();
                meta.order = entries.size();
                return meta;
            });
        }

        void ensure(List<xd.harm.config.Config> configs) {
            load();
            if (configs == null) {
                return;
            }

            boolean changed = false;
            int nextOrder = entries.values().stream().mapToInt(meta -> meta.order).max().orElse(-1) + 1;
            for (xd.harm.config.Config config : configs) {
                if (config == null || config.getName() == null) {
                    continue;
                }
                if (!entries.containsKey(config.getName())) {
                    ConfigMeta meta = new ConfigMeta();
                    meta.createdAt = System.currentTimeMillis();
                    meta.order = nextOrder++;
                    entries.put(config.getName(), meta);
                    changed = true;
                }
            }
            if (changed) {
                save();
            }
        }

        void touch(String name) {
            ConfigMeta meta = get(name);
            if (meta.createdAt <= 0L) {
                meta.createdAt = System.currentTimeMillis();
            }
            save();
        }

        void setTags(String name, String tags) {
            ConfigMeta meta = get(name);
            meta.tags = tags == null ? "" : tags.trim();
            save();
        }

        void copy(String source, String target) {
            ConfigMeta sourceMeta = get(source);
            ConfigMeta targetMeta = get(target);
            targetMeta.createdAt = System.currentTimeMillis();
            targetMeta.tags = sourceMeta.tags;
            targetMeta.order = entries.values().stream().mapToInt(meta -> meta.order).max().orElse(0) + 1;
            save();
        }

        void remove(String name) {
            load();
            entries.remove(name);
            save();
        }

        void saveOrder(List<xd.harm.config.Config> configs) {
            load();
            for (int i = 0; i < configs.size(); i++) {
                xd.harm.config.Config config = configs.get(i);
                if (config != null && config.getName() != null) {
                    get(config.getName()).order = i;
                }
            }
            save();
        }

        private void load() {
            if (loaded) {
                return;
            }
            loaded = true;
            try {
                JsonElement rootElement = PARSER.parse(decode(DEFAULT_META_PAYLOAD));
                if (rootElement == null || !rootElement.isJsonObject()) {
                    return;
                }
                JsonObject root = rootElement.getAsJsonObject();
                JsonArray configs = root.getAsJsonArray("configs");
                if (configs == null) {
                    return;
                }
                for (JsonElement element : configs) {
                    if (!element.isJsonObject()) {
                        continue;
                    }
                    JsonObject object = element.getAsJsonObject();
                    JsonElement nameElement = object.get("name");
                    if (nameElement == null || !nameElement.isJsonPrimitive()) {
                        continue;
                    }
                    ConfigMeta meta = new ConfigMeta();
                    meta.createdAt = object.has("createdAt") ? object.get("createdAt").getAsLong() : System.currentTimeMillis();
                    meta.order = object.has("order") ? object.get("order").getAsInt() : entries.size();
                    meta.tags = object.has("tags") ? object.get("tags").getAsString() : "";
                    entries.put(nameElement.getAsString(), meta);
                }
            } catch (Exception ignored) {
            }
        }

        private void save() {
            // Card metadata is intentionally code-backed, not persisted to a JSON file.
        }

        private String decode(String value) {
            byte[] bytes = Base64.getDecoder().decode(value == null ? "" : value.trim());
            xor(bytes);
            return new String(bytes, StandardCharsets.UTF_8);
        }

        private void xor(byte[] bytes) {
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = (byte) (bytes[i] ^ META_MASK[i % META_MASK.length]);
            }
        }
    }

    private static class Snowflake {
        float x, y, speed, size, drift, phase, alpha;
    }
}
