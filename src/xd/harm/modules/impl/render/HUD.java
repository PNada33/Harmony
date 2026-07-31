package xd.harm.modules.impl.render;

import com.google.common.eventbus.Subscribe;
import xd.harm.Harmony;
import xd.harm.events.network.EventPacket;
import xd.harm.events.render.EventDisplay;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.CategorySetting;
import xd.harm.modules.settings.impl.ColorSetting;
import xd.harm.modules.settings.impl.ModeListSetting;
import xd.harm.modules.settings.impl.ModeSetting;
import xd.harm.ui.display.impl.*;
import xd.harm.ui.display.impl.hud2.WatermarkRenderer2;
import xd.harm.ui.display.impl.hud2.KeyBindRenderer2;
import xd.harm.ui.display.impl.hud2.StaffListRenderer2;
import xd.harm.ui.display.impl.hud2.PotionRenderer2;
import xd.harm.ui.styles.StyleManager;
import xd.harm.utils.drag.DragManager;
import xd.harm.utils.drag.Dragging;
import xd.harm.utils.render.KawaseBlur;
import xd.harm.utils.render.rect.RenderUtility;
import xd.harm.utils.render.color.ColorUtils;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gui.screen.ChatScreen;
import org.lwjgl.opengl.GL11;

@FieldDefaults(level = AccessLevel.PRIVATE)
@ModuleRegister(name = "HUD", category = Category.Render, desc = "Худ")
public class HUD extends Module {

    public ModeSetting hudMode = new ModeSetting("Стиль худа", "Обычный", "Обычный", "Флов");

    public ModeSetting targetHudMode = new ModeSetting(
            "\u0421\u0442\u0438\u043B\u044C \u0442\u0430\u0440\u0433\u0435\u0442\u0445\u0443\u0434\u0430",
            "\u0421\u043e\u0447\u043d\u044b\u0439",
            "\u0421\u043e\u0447\u043d\u044b\u0439",
            "\u041C\u0430\u043B\u0435\u043D\u044C\u043A\u0438\u0439"
    );
    public BooleanSetting d3 = new BooleanSetting("3д таргет", false);

    public final ModeListSetting elements = new ModeListSetting("Элементы",
            new BooleanSetting("Ватермарка", true),
            new BooleanSetting("Эффекты", true),
            new BooleanSetting("Список модерации", true),
            new BooleanSetting("Активные бинды", true),
            new BooleanSetting("Броня", true),
            new BooleanSetting("Активный таргет", true),
            new BooleanSetting("Расходники", true)
    );

    public final ModeSetting arrayListColorMode = new ModeSetting("Цвет Списка Модулей", "Клиент",
            "Радужный", "Клиент", "Свой", "Свой 2 цвета")
            .setVisible(() -> elements.getValueByName("Список Модулей").get());

    public final ColorSetting arrayListColor1 = new ColorSetting("Цвет 1", ColorUtils.rgb(100, 255, 100))
            .setVisible(() -> elements.getValueByName("Список Модулей").get() &&
                    arrayListColorMode.get().contains("Свой"));

    public final ColorSetting arrayListColor2 = new ColorSetting("Цвет 2", ColorUtils.rgb(60, 60, 255))
            .setVisible(() -> elements.getValueByName("Список Модулей").get() &&
                    arrayListColorMode.get().equalsIgnoreCase("Свой 2 цвета"));

    public final ModeListSetting arrayListDisplay = new ModeListSetting("Отображать",
            new BooleanSetting("Combat", true),
            new BooleanSetting("Movement", true),
            new BooleanSetting("Player", true),
            new BooleanSetting("Render", true),
            new BooleanSetting("Misc", true)
    ).setVisible(() -> elements.getValueByName("Список Модулей").get());

    public BooleanSetting targetHudRaycast = new BooleanSetting("Таргет по наведению", false)
            .setVisible(() -> elements.getValueByName("Активный таргет").get());

    public final ModeListSetting watermarkElements = new ModeListSetting("Элементы вотермарки",
            new BooleanSetting("Время", true),
            new BooleanSetting("Пинг", true),
            new BooleanSetting("Фпс", false),
            new BooleanSetting("Ник", false),
            new BooleanSetting("Бпс", false),
            new BooleanSetting("Координаты", false),
            new BooleanSetting("Сервер", false)
    ).setVisible(() -> elements.getValueByName("Ватермарка").get());

    private final CategorySetting mainCategory = new CategorySetting("Основное");
    private final CategorySetting targetHudCategory = (new CategorySetting("Таргет HUD"))
            .setVisible(() -> elements.getValueByName("Активный таргет").get());
    private final CategorySetting watermarkCategory = (new CategorySetting("Ватермарка"))
            .setVisible(() -> elements.getValueByName("Ватермарка").get());
    final WatermarkRenderer watermarkRenderer;
    final WatermarkRenderer2 watermarkRenderer2;
    final PotionRenderer potionRenderer;
    final PotionRenderer2 potionRenderer2;
    final KeyBindRenderer keyBindRenderer;
    final KeyBindRenderer2 keyBindRenderer2;
    final TargetInfoRenderer targetInfoRenderer;
    final Dragging targetHudDrag;
    final ArmorRenderer armorRenderer;
    final StaffListRenderer staffListRenderer;
    final StaffListRenderer2 staffListRenderer2;
    final ConsumablesRenderer consumablesRenderer;
    final ArrayListRenderer arrayListRenderer;

    final Dragging watermarkDrag;
    final Dragging potionsDrag;
    final Dragging keyBindsDrag;
    final Dragging staffListDrag;
    final Dragging consumablesDrag;
    final Dragging[] hoverOutlineDrags;
    final float[] hoverOutlineAlpha;

    String lastHudMode = "";

    static final int WATERMARK_ELEMENT_INDEX = 0;
    static final int EFFECTS_ELEMENT_INDEX = 1;
    static final int STAFF_LIST_ELEMENT_INDEX = 2;
    static final int KEY_BINDS_ELEMENT_INDEX = 3;
    static final int ARMOR_ELEMENT_INDEX = 4;
    static final int TARGET_HUD_ELEMENT_INDEX = 5;
    static final int CONSUMABLES_ELEMENT_INDEX = 6;
    static final int TARGET_HUD_HOVER_INDEX = 4;
    static final int CONSUMABLES_HOVER_INDEX = 5;
    boolean[] normalWatermarkElementsState;
    boolean[] flowWatermarkElementsState;

    public HUD() {
        sanitizeTargetHudMode();
        watermarkDrag = Harmony.getInstance().createDrag(this, "Watermark", 4, 4);
        watermarkRenderer = new WatermarkRenderer(watermarkDrag);
        watermarkRenderer2 = new WatermarkRenderer2(watermarkDrag);

        potionsDrag = Harmony.getInstance().createDrag(this, "Potions", 278, 5);
        potionRenderer = new PotionRenderer(potionsDrag);
        potionRenderer2 = new PotionRenderer2(potionsDrag);

        armorRenderer = new ArmorRenderer();

        keyBindsDrag = Harmony.getInstance().createDrag(this, "KeyBinds", 185, 5);
        keyBindRenderer = new KeyBindRenderer(keyBindsDrag);
        keyBindRenderer2 = new KeyBindRenderer2(keyBindsDrag);

        targetHudDrag = Harmony.getInstance().createDrag(this, "TargetHUD", 74, 128);
        targetInfoRenderer = new TargetInfoRenderer(targetHudDrag);

        staffListDrag = Harmony.getInstance().createDrag(this, "StaffList", 96, 5);
        staffListRenderer = new StaffListRenderer(staffListDrag);
        staffListRenderer2 = new StaffListRenderer2(staffListDrag);

        consumablesDrag = Harmony.getInstance().createDrag(this, "Consumables", 200, 200);
        consumablesRenderer = new ConsumablesRenderer(consumablesDrag);
        hoverOutlineDrags = new Dragging[]{
                watermarkDrag,
                potionsDrag,
                keyBindsDrag,
                staffListDrag,
                targetHudDrag,
                consumablesDrag
        };
        hoverOutlineAlpha = new float[hoverOutlineDrags.length];

        arrayListRenderer = new ArrayListRenderer();

        lastHudMode = hudMode.get();
        normalWatermarkElementsState = captureWatermarkElementsState();
        flowWatermarkElementsState = null;

        addSettings(
                mainCategory,
                hudMode,
                elements,
                targetHudCategory,
                targetHudMode,
                d3,
                targetHudRaycast,
                watermarkCategory,
                watermarkElements
        );
        DragManager.load();
    }

    @Subscribe
    private void onUpdate(EventUpdate e) {
        sanitizeTargetHudMode();
        syncWatermarkElementsForHudMode();

        if (mc.gameSettings.showDebugInfo) {
            return;
        }

        if (elements.getValueByName("Список модерации").get()) {
            if (isNormalHudMode()) {
                staffListRenderer.update(e);
            } else {
                staffListRenderer2.update(e);
            }
        }
    }

    private void sanitizeTargetHudMode() {
        if ("Круглый".equalsIgnoreCase(targetHudMode.get())) {
            targetHudMode.set("Сочный");
        }
    }

    @Subscribe
    private void onDisplay(EventDisplay e) {
        if (mc.gameSettings.showDebugInfo || e.getType() != EventDisplay.Type.HIGH || Boolean.getBoolean("bot.mode")) {
            return;
        }

        KawaseBlur.blur.updateBlur(3.0f, 3);

        boolean normalHudMode = isNormalHudMode();
        boolean renderWatermark = elements.get(WATERMARK_ELEMENT_INDEX).get();
        boolean renderEffects = elements.get(EFFECTS_ELEMENT_INDEX).get();
        boolean renderStaffList = elements.get(STAFF_LIST_ELEMENT_INDEX).get();
        boolean renderKeyBinds = elements.get(KEY_BINDS_ELEMENT_INDEX).get();
        boolean renderArmor = elements.get(ARMOR_ELEMENT_INDEX).get();
        boolean renderTargetHud = elements.get(TARGET_HUD_ELEMENT_INDEX).get();
        boolean renderConsumables = elements.get(CONSUMABLES_ELEMENT_INDEX).get();

        try {
            if (renderWatermark) {
                if (normalHudMode) {
                    watermarkRenderer.render(e);
                } else {
                    watermarkRenderer2.render(e);
                }
                resetHudGlState();
            }
            if (renderEffects) {
                if (normalHudMode) {
                    potionRenderer.render(e);
                } else {
                    potionRenderer2.render(e);
                }
            }
            if (renderKeyBinds) {
                if (normalHudMode) {
                    keyBindRenderer.render(e);
                } else {
                    keyBindRenderer2.render(e);
                }
            }
            if (renderStaffList) {
                if (normalHudMode) {
                    staffListRenderer.render(e);
                } else {
                    staffListRenderer2.render(e);
                }
            }
            if (renderTargetHud) {
                targetInfoRenderer.render(e);
            }
            if (renderConsumables) {
                resetHudGlState();
                consumablesRenderer.render(e);
                resetHudGlState();
            }
            if (renderArmor) {
                armorRenderer.render(e);
            }
            renderHoverOutlines();
        } finally {
            resetHudGlState();
        }
    }

    private void resetHudGlState() {
        xd.harm.utils.shader.ShaderUtil.dashedOutline.detach();
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
        GL11.glDisable(GL11.GL_POINT_SMOOTH);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        com.mojang.blaze3d.systems.RenderSystem.disableAlphaTest();
        com.mojang.blaze3d.systems.RenderSystem.enableAlphaTest();
        com.mojang.blaze3d.systems.RenderSystem.defaultAlphaFunc();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glLoadIdentity();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        com.mojang.blaze3d.platform.GlStateManager.activeTexture(org.lwjgl.opengl.GL13.GL_TEXTURE0);
        com.mojang.blaze3d.systems.RenderSystem.enableTexture();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        com.mojang.blaze3d.systems.RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Subscribe
    private void onPacket(EventPacket e) {
        if (!elements.getValueByName("Активный таргет").get()) {
            return;
        }
        targetInfoRenderer.onPacket(e);
    }

    private void syncWatermarkElementsForHudMode() {
        String currentMode = hudMode.get();
        if (currentMode == null) {
            return;
        }

        boolean[] currentState = captureWatermarkElementsState();

        if (!currentMode.equalsIgnoreCase(lastHudMode)) {
            saveCurrentWatermarkState(lastHudMode);

            if (isNormalHudMode(currentMode)) {
                if (normalWatermarkElementsState != null && !areStatesEqual(currentState, normalWatermarkElementsState)) {
                    applyWatermarkElementsState(normalWatermarkElementsState);
                    currentState = captureWatermarkElementsState();
                }
            } else {
                if (flowWatermarkElementsState == null) {
                    boolean[] baseState = normalWatermarkElementsState != null
                            ? copyState(normalWatermarkElementsState)
                            : currentState;
                    flowWatermarkElementsState = limitToTwoEnabled(baseState);
                } else {
                    flowWatermarkElementsState = limitToTwoEnabled(flowWatermarkElementsState);
                }
                if (!areStatesEqual(currentState, flowWatermarkElementsState)) {
                    applyWatermarkElementsState(flowWatermarkElementsState);
                    currentState = captureWatermarkElementsState();
                }
            }

            lastHudMode = currentMode;
        }

        if (isNormalHudMode(currentMode)) {
            normalWatermarkElementsState = copyState(currentState);
            return;
        }

        boolean[] limitedState = limitToTwoEnabled(currentState);
        if (!areStatesEqual(currentState, limitedState)) {
            applyWatermarkElementsState(limitedState);
            currentState = captureWatermarkElementsState();
        }
        flowWatermarkElementsState = copyState(currentState);
    }

    private void saveCurrentWatermarkState(String mode) {
        if (mode == null) {
            return;
        }

        if (isNormalHudMode(mode)) {
            normalWatermarkElementsState = captureWatermarkElementsState();
            return;
        }

        flowWatermarkElementsState = limitToTwoEnabled(captureWatermarkElementsState());
    }

    public boolean isNormalHudMode() {
        return isNormalHudMode(hudMode.get());
    }

    public boolean isFlowHudMode() {
        return isFlowHudMode(hudMode.get());
    }

    private boolean isNormalHudMode(String mode) {
        return !isFlowHudMode(mode);
    }

    private boolean isFlowHudMode(String mode) {
        return mode != null && (
                mode.equalsIgnoreCase("Флов")
                        || mode.equalsIgnoreCase("\u0424\u043B\u043E\u0432")
                        || mode.equalsIgnoreCase("Flow")
        );
    }

    private boolean[] captureWatermarkElementsState() {
        int size = watermarkElements.get().size();
        boolean[] state = new boolean[size];
        for (int i = 0; i < size; i++) {
            state[i] = watermarkElements.get(i).get();
        }
        return state;
    }

    private void applyWatermarkElementsState(boolean[] state) {
        if (state == null) {
            return;
        }

        int size = Math.min(state.length, watermarkElements.get().size());
        for (int i = 0; i < size; i++) {
            BooleanSetting option = watermarkElements.get(i);
            if (option.get() != state[i]) {
                option.set(state[i]);
            }
        }
    }

    private boolean[] limitToTwoEnabled(boolean[] state) {
        if (state == null) {
            return null;
        }

        boolean[] limitedState = copyState(state);
        int enabledCount = 0;
        for (int i = 0; i < limitedState.length; i++) {
            if (!limitedState[i]) {
                continue;
            }
            enabledCount++;
            if (enabledCount > 2) {
                limitedState[i] = false;
            }
        }
        return limitedState;
    }

    private boolean[] copyState(boolean[] sourceState) {
        if (sourceState == null) {
            return null;
        }
        return sourceState.clone();
    }

    private boolean areStatesEqual(boolean[] firstState, boolean[] secondState) {
        if (firstState == secondState) {
            return true;
        }
        if (firstState == null || secondState == null || firstState.length != secondState.length) {
            return false;
        }
        for (int i = 0; i < firstState.length; i++) {
            if (firstState[i] != secondState[i]) {
                return false;
            }
        }
        return true;
    }

    private void renderHoverOutlines() {
        if (!(mc.currentScreen instanceof ChatScreen)) {
            for (int i = 0; i < hoverOutlineAlpha.length; i++) {
                hoverOutlineAlpha[i] = Math.max(0f, hoverOutlineAlpha[i] - 0.06f);
            }
            return;
        }

        net.minecraft.client.MainWindow window = mc.getMainWindow();
        int scaledWidth = window.getScaledWidth();
        int scaledHeight = window.getScaledHeight();
        int mouseX = (int) (mc.mouseHelper.getMouseX() * scaledWidth / (double) window.getWidth());
        int mouseY = (int) (mc.mouseHelper.getMouseY() * scaledHeight / (double) window.getHeight());
        float outlineTime = (System.currentTimeMillis() % 2500L) / 2500f;

        for (int i = 0; i < hoverOutlineDrags.length; i++) {
            Dragging drag = hoverOutlineDrags[i];
            if (drag == null || drag.getWidth() < 1 || drag.getHeight() < 1) continue;

            float dx = drag.getX();
            float dy = drag.getY();
            float dw = drag.getWidth();
            float dh = drag.getHeight();

            boolean hovered = (mouseX >= dx && mouseX <= dx + dw && mouseY >= dy && mouseY <= dy + dh) || drag.isDragging();

            float alpha = hoverOutlineAlpha[i];
            if (hovered) {
                alpha = Math.min(1f, alpha + 0.08f);
            } else {
                alpha = Math.max(0f, alpha - 0.06f);
            }
            hoverOutlineAlpha[i] = alpha;

            if (alpha < 0.01f) continue;

            float expand = i == CONSUMABLES_HOVER_INDEX ? 1f : 2.5f;
            float x = dx - expand;
            float y = dy - expand;
            float width = dw + expand * 2f;
            float height = dh + expand * 2f;
            float radius = i == TARGET_HUD_HOVER_INDEX ? 8f : 5f;

            GL11.glPushMatrix();
            try {
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glEnable(GL11.GL_TEXTURE_2D);
                GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

                xd.harm.utils.shader.ShaderUtil.dashedOutline.attach();

                xd.harm.utils.shader.ShaderUtil.dashedOutline.setUniform("quadSize", (width + 10f) * 2f, (height + 10f) * 2f);
                xd.harm.utils.shader.ShaderUtil.dashedOutline.setUniform("innerSize", width * 2f, height * 2f);
                xd.harm.utils.shader.ShaderUtil.dashedOutline.setUniform("round", radius * 2f, radius * 2f, radius * 2f, radius * 2f);
                xd.harm.utils.shader.ShaderUtil.dashedOutline.setUniform("smoothness", 0.0f, 1.5f);
                xd.harm.utils.shader.ShaderUtil.dashedOutline.setUniform("outlineColor", 1.0f, 1.0f, 1.0f, 1.0f);
                xd.harm.utils.shader.ShaderUtil.dashedOutline.setUniform("outlineThickness", 0.5f * 2f);
                xd.harm.utils.shader.ShaderUtil.dashedOutline.setUniform("time", outlineTime);
                xd.harm.utils.shader.ShaderUtil.dashedOutline.setUniform("dashLength", 12.0f * 2f);
                xd.harm.utils.shader.ShaderUtil.dashedOutline.setUniform("gapLength", 8.0f * 2f);
                xd.harm.utils.shader.ShaderUtil.dashedOutline.setUniform("glareSize", 30.0f * 2f);
                xd.harm.utils.shader.ShaderUtil.dashedOutline.setUniform("globalAlpha", alpha);

                xd.harm.utils.render.rect.RenderUtility.drawQuads(x - 5f, y - 5f, width + 10f, height + 10f, 7);
            } finally {
                resetHudGlState();
                GL11.glPopMatrix();
            }
        }
    }


    public static int getColor(int index) {
        StyleManager styleManager = Harmony.getInstance().getStyleManager();
        return ColorUtils.gradient(styleManager.getCurrentStyle().getFirstColor().getRGB(),
                styleManager.getCurrentStyle().getSecondColor().getRGB(), index * 16, 10);
    }

    public static int getColor(int index, float mult) {
        StyleManager styleManager = Harmony.getInstance().getStyleManager();
        return ColorUtils.gradient(styleManager.getCurrentStyle().getFirstColor().getRGB(),
                styleManager.getCurrentStyle().getSecondColor().getRGB(), (int) (index * mult), 10);
    }

    public static int getColor(int firstColor, int secondColor, int index, float mult) {
        return ColorUtils.gradient(firstColor, secondColor, (int) (index * mult), 10);
    }

    public int getArrayListColor(int index, float mult) {
        String mode = arrayListColorMode.get();

        switch (mode) {
            case "Радужный":
                return ColorUtils.rainbow(8, (int)(index * mult), 0.85f, 1.0f, 1.0f);

            case "Клиент":
                return Theme.getColor(index, mult);

            case "Свой":
                return arrayListColor1.get();

            case "Свой 2 цвета":
                return ColorUtils.gradient(
                        arrayListColor1.get(),
                        arrayListColor2.get(),
                        (int) (index * mult), 10
                );

            default:
                return -1;
        }
    }

    public boolean isArrayListCategoryEnabled(Category category) {
        if (category == null) return true;

        String categoryName = category.name();
        return arrayListDisplay.getValueByName(categoryName).get();
    }
}
