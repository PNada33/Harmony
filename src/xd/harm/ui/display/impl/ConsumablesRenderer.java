package xd.harm.ui.display.impl;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.vector.Vector4f;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.opengl.GL11;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.Tessellator;
import org.lwjgl.opengl.GL13;
import xd.harm.Harmony;
import xd.harm.events.render.EventDisplay;
import xd.harm.modules.impl.player.FTHelper;
import xd.harm.modules.settings.impl.BindSetting;
import xd.harm.ui.display.ElementRenderer;
import xd.harm.utils.client.KeyStorage;
import xd.harm.utils.drag.Dragging;
import xd.harm.utils.math.MathUtil;
import xd.harm.utils.math.Vector4i;
import xd.harm.utils.render.color.ColorUtils;
import xd.harm.utils.render.font.Fonts;
import xd.harm.utils.render.rect.RenderUtility;

import java.util.*;

@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class ConsumablesRenderer implements ElementRenderer {
    final Dragging dragging;
    float width;
    float height;
    float currentAlpha = 0f;

    private final Map<String, Float> cooldownAlphas = new HashMap<>();
    private final Map<String, Boolean> cooldownStates = new HashMap<>();
    private final Map<String, Long> cooldownStartMs = new HashMap<>();
    private final List<FunItem> visibleItems = new ArrayList<>();
    private final Set<String> addedItemNames = new HashSet<>();
    private final ItemStack razbafBadgeStack = new ItemStack(Items.PHANTOM_MEMBRANE);

    private static final float ALPHA_SPEED = 10f;
    private static final float COOLDOWN_ALPHA_SPEED = 15f;
    private static final int MAX_ITEMS_PER_ROW = 8;
    private static final float RADIUS = 4.5f;
    private static final float ITEM_HEIGHT = 24f;
    private static final float ICON_SCALE = 0.85f;
    private static final float ICON_SIZE = 16f * ICON_SCALE;
    private static final float PADDING = 2f;
    private static final float ITEM_SPACING = 4f;
    private static final float ROW_SPACING = 3f;
    private static final float KEY_FONT_SIZE = 6f;
    private static final float COOLDOWN_RING_RADIUS = 4.5f;
    private static final float COOLDOWN_RING_THICKNESS = 1.1f;
    private static final float COOLDOWN_RING_FEATHER = 1.1f;
    private static final float COOLDOWN_RING_GAP = 3.0f;
    private static final long USE_FLASH_MS = 350L;
    private static final float YELLOW_PULSE_SPEED = 160f;
    private static final float GREEN_PULSE_SPEED = 140f;
    private static final Vector4f RADIUS_VEC = new Vector4f(RADIUS, RADIUS, RADIUS, RADIUS);
    private static final String RAZBAF_NAME = "Разбаф";
    private static final int RAZBAF_POTION_COLOR = 0x7A35FF;

    private static final List<FunItemType> FUN_ITEM_TYPES = new ArrayList<>();
    private static final Map<Item, List<FunItemType>> TYPES_BY_ITEM = new HashMap<>();
    private static final int NO_POTION_COLOR = -1;
    private static final int[] WASTELAND_POTION_COLORS = new int[] {
            NO_POTION_COLOR, NO_POTION_COLOR, NO_POTION_COLOR, NO_POTION_COLOR,
            NO_POTION_COLOR, NO_POTION_COLOR, NO_POTION_COLOR, NO_POTION_COLOR,
            0x0C920E, 0xFFA500, 0xFFFFFF, 0xFF3333, 0xFF69B4, 0x11BB14,
            0xFFD400, 0xFF69B4, 0xFFFFFF, 0x8B0000, 0x00BFFF, 0x000000,
            0x00FF00, 0x0000FF
    };

    static {
        reg("Дезориентация", Items.ENDER_EYE, "desorientation", "дезориентация");
        reg("Божья Аура", Items.PHANTOM_MEMBRANE, "godsaura", "божья аура");
        reg("Явная Пыль", Items.SUGAR, "sheerdust", "явная пыль");
        reg("Пласт", Items.DRIED_KELP, "stratum", "пласт");
        reg("Трапка", Items.NETHERITE_SCRAP, "trap", "трапка");
        reg("Снежок Заморозка", Items.SNOWBALL, null, null);
        reg("Арбалет", Items.CROSSBOW, null, null);
        reg("Огненный Смерч", Items.FIRE_CHARGE, "fierytornado", "огненный смерч");
        reg("Серная кислота", Items.SPLASH_POTION, "acid", "серн");
        reg("Отрыжка", Items.SPLASH_POTION, "burp", "отрыжк");
        reg("Сопли флеша", Items.SPLASH_POTION, "flash", "флеш");
        reg("Зелье Киллера", Items.SPLASH_POTION, "potion-killer", "киллера");
        reg("Зелье Медика", Items.SPLASH_POTION, "potion-medic", "медика");
        reg("Зелье Победителя", Items.SPLASH_POTION, "potion-winner", "победителя");
        reg("Зелье Агента", Items.SPLASH_POTION, "potion-agent", "агента");
        reg("Хлопушка", Items.SPLASH_POTION, null, "хлопушка");
        reg("Святая Вода", Items.SPLASH_POTION, null, "святая вода");
        reg("Зелье Гнева", Items.SPLASH_POTION, null, "зелье гнева");
        reg("Зелье Паладина", Items.SPLASH_POTION, null, "зелье пал");
        reg("Зелье Ассасина", Items.SPLASH_POTION, null, "зелье ассасина");
        reg("Зелье Радиации", Items.SPLASH_POTION, null, "зелье радиации");
        reg("Снотворное", Items.SPLASH_POTION, null, "снотворное");
    }

    private static void reg(String name, Item item, String nbtTag, String displaySearch) {
        int colorIndex = FUN_ITEM_TYPES.size();
        int potionColor = colorIndex < WASTELAND_POTION_COLORS.length
                ? WASTELAND_POTION_COLORS[colorIndex]
                : NO_POTION_COLOR;
        FunItemType type = new FunItemType(name, item, nbtTag, displaySearch, potionColor);
        FUN_ITEM_TYPES.add(type);
        TYPES_BY_ITEM.computeIfAbsent(item, k -> new ArrayList<>()).add(type);
    }

    @Override
    public void render(EventDisplay eventDisplay) {
        FTHelper ftHelper = Harmony.getInstance().getModuleManager().getFTHelper();
        MatrixStack ms = eventDisplay.getMatrixStack();
        float posX = dragging.getX() + dragging.getWobbleX();
        float posY = dragging.getY() + dragging.getWobbleY();

        getVisibleItemsFromInventory(ftHelper, visibleItems);

        if (mc.ingameGUI.getChatGUI().getChatOpen() && visibleItems.isEmpty()) {
            buildTestItems(ftHelper, visibleItems);
        }

        if (visibleItems.isEmpty()) {
            width = MathUtil.lerp(width, 0, 20f);
            height = MathUtil.lerp(height, 0, 20f);
            dragging.setWidth(width);
            dragging.setHeight(height);
            return;
        }
        int alpha = 255;

        int total = visibleItems.size();
        int rowCount = (total + MAX_ITEMS_PER_ROW - 1) / MAX_ITEMS_PER_ROW;
        float maxRowWidth = 0;
        int idx = 0;

        for (int r = 0; r < rowCount; r++) {
            int rowSize = Math.min(MAX_ITEMS_PER_ROW, total - r * MAX_ITEMS_PER_ROW);
            float rw = PADDING * 2;
            for (int i = 0; i < rowSize; i++) {
                rw += visibleItems.get(idx++).itemWidth;
                if (i < rowSize - 1) rw += ITEM_SPACING;
            }
            maxRowWidth = Math.max(maxRowWidth, rw);
        }

        float totalHeight = PADDING * 2 + rowCount * ITEM_HEIGHT + (rowCount - 1) * ROW_SPACING;
        width = MathUtil.lerp(width, maxRowWidth, 20f);
        height = MathUtil.lerp(height, totalHeight, 20f);
        dragging.setWidth(width);
        dragging.setHeight(height);

        float finalAlpha = alpha / 255f;
        int adjAlpha = (int) (200 * finalAlpha);
        int colorTL = ColorUtils.rgba(20, 20, 24, adjAlpha);
        int colorBL = ColorUtils.rgba(10, 10, 12, adjAlpha);
        int colorBR = ColorUtils.rgba(14, 14, 16, adjAlpha);

        float grabScale = dragging.getGrabScale();
        float grabRot = dragging.getWobbleAngle();
        boolean grab = Math.abs(grabScale - 1.0f) > 0.001f || Math.abs(grabRot) > 0.001f;
        if (grab) {
            GL11.glPushMatrix();
            if (Math.abs(grabScale - 1.0f) > 0.001f) {
                RenderUtility.customScaledObject2D(posX, posY, width, height, grabScale);
            }
            if (Math.abs(grabRot) > 0.001f) {
                RenderUtility.customRotatedObject2D(posX, posY, width, height, grabRot);
            }
        }

        idx = 0;
        float curY = posY + PADDING;
        for (int r = 0; r < rowCount; r++) {
            int rowSize = Math.min(MAX_ITEMS_PER_ROW, total - r * MAX_ITEMS_PER_ROW);
            float curX = posX + PADDING;
            for (int i = 0; i < rowSize; i++) {
                FunItem fi = visibleItems.get(idx++);
                renderItem(ms, curX, curY, fi, alpha, colorTL, colorBL, colorBR);
                curX += fi.itemWidth + ITEM_SPACING;
            }
            curY += ITEM_HEIGHT + ROW_SPACING;
        }

        if (grab) {
            GL11.glPopMatrix();
        }
    }

    private void renderItem(MatrixStack ms, float x, float y, FunItem item, int alpha,
                            int colorTL, int colorBL, int colorBR) {
        float w = item.itemWidth;

        boolean hasCooldown = false;
        float cooldownProgress = 0f;
        if (mc.player != null && item.slotIndex != -1) {
            Item itemType = item.itemStack.getItem();
            hasCooldown = mc.player.getCooldownTracker().hasCooldown(itemType);
            if (hasCooldown) {
                cooldownProgress = mc.player.getCooldownTracker().getCooldown(itemType, mc.getRenderPartialTicks());
            }
        }

        long now = System.currentTimeMillis();
        boolean wasCooldown = cooldownStates.getOrDefault(item.name, false);
        if (hasCooldown && !wasCooldown) {
            cooldownStartMs.put(item.name, now);
        } else if (!hasCooldown && wasCooldown) {
            cooldownStartMs.remove(item.name);
        }
        cooldownStates.put(item.name, hasCooldown);

        int fillTL = colorTL;
        int fillBL = colorBL;
        int fillBR = colorBR;
        if (hasCooldown) {
            long start = cooldownStartMs.getOrDefault(item.name, now);
            long since = now - start;
            if (since < USE_FLASH_MS) {
                float pulse = (float) (Math.sin(now / GREEN_PULSE_SPEED) * 0.5f + 0.5f);
                int greenHi = ColorUtils.interpolateColor(ColorUtils.rgb(80, 210, 80), ColorUtils.rgb(130, 255, 130), pulse);
                int greenLo = ColorUtils.interpolateColor(ColorUtils.rgb(40, 140, 40), ColorUtils.rgb(80, 200, 80), pulse);
                fillTL = ColorUtils.reAlphaInt(greenHi, alpha);
                fillBL = ColorUtils.reAlphaInt(greenLo, alpha);
                fillBR = ColorUtils.reAlphaInt(greenLo, alpha);
            } else {
                float pulse = (float) (Math.sin(now / YELLOW_PULSE_SPEED) * 0.5f + 0.5f);
                int yellowHi = ColorUtils.interpolateColor(ColorUtils.rgb(255, 210, 70), ColorUtils.rgb(255, 235, 130), pulse);
                int yellowLo = ColorUtils.interpolateColor(ColorUtils.rgb(200, 145, 45), ColorUtils.rgb(230, 175, 65), pulse);
                fillTL = ColorUtils.reAlphaInt(yellowHi, alpha);
                fillBL = ColorUtils.reAlphaInt(yellowLo, alpha);
                fillBR = ColorUtils.reAlphaInt(yellowLo, alpha);
            }
        }

        RenderUtility.drawRoundedRect(x, y, w, ITEM_HEIGHT, RADIUS_VEC, new Vector4i(fillTL, fillTL, fillBL, fillBR));

        float cdAlpha = cooldownAlphas.getOrDefault(item.name, 0f);
        cdAlpha = MathUtil.lerp(cdAlpha, hasCooldown ? 255f : 0f, COOLDOWN_ALPHA_SPEED);
        cooldownAlphas.put(item.name, cdAlpha);

        if (cdAlpha > 5f) {
            drawCooldownOverlay(x, y, w, cooldownProgress, (int) cdAlpha);
        }

        float keyAreaHeight = KEY_FONT_SIZE + 2f;
        float iconAreaHeight = ITEM_HEIGHT - keyAreaHeight;
        float iconX = x + (ITEM_HEIGHT - ICON_SIZE) / 2f;
        float iconY = y + (iconAreaHeight - ICON_SIZE) / 2f;

        if (isRazbafItem(item)) {
            renderRazbafIcon(iconX, iconY, item.itemStack);
        } else {
            prepareItemGlState();
            GL11.glPushMatrix();
            GL11.glTranslatef(iconX, iconY, 0);
            GL11.glScalef(ICON_SCALE, ICON_SCALE, ICON_SCALE);
            mc.getItemRenderer().renderItemIntoGUI(item.itemStack, 0, 0);
            GL11.glPopMatrix();
            prepareItemGlState();
        }

        if (item.itemStack.getCount() != 1) {
            String countText = String.valueOf(item.itemStack.getCount());
            MatrixStack countMs = new MatrixStack();
            countMs.translate(0.0D, 0.0D, 200.0D);
            countMs.translate(iconX, iconY, 0.0D);
            countMs.scale(ICON_SCALE, ICON_SCALE, 1.0f);
            IRenderTypeBuffer.Impl buffer = IRenderTypeBuffer.getImpl(Tessellator.getInstance().getBuffer());
            mc.fontRenderer.renderString(
                    countText,
                    19 - 2 - mc.fontRenderer.getStringWidth(countText),
                    6 + 3,
                    0xFFFFFF,
                    true,
                    countMs.getLast().getMatrix(),
                    buffer,
                    false,
                    0,
                    15728880
            );
            buffer.finish();
        }

        float keyWidth = Fonts.consolas.getWidth(item.keyText, KEY_FONT_SIZE);
        float keyX = x + (w - keyWidth) / 2f;
        float keyY = y + ITEM_HEIGHT - KEY_FONT_SIZE - 2f;
        Fonts.consolas.drawText(ms, item.keyText, keyX, keyY, ColorUtils.rgba(200, 200, 200, alpha), KEY_FONT_SIZE, 0.05f);
    }

    private void renderRazbafIcon(float iconX, float iconY, ItemStack potionStack) {
        prepareItemGlState();
        GL11.glPushMatrix();
        GL11.glTranslatef(iconX - 1.0f, iconY - 0.6f, 0);
        GL11.glScalef(0.82f, 0.82f, 0.82f);
        mc.getItemRenderer().renderItemIntoGUI(potionStack, 0, 0);
        GL11.glPopMatrix();
        prepareItemGlState();

        float badgeX = iconX + 8.4f;
        float badgeY = iconY + 6.5f;

        prepareItemGlState();
        GL11.glPushMatrix();
        GL11.glTranslatef(badgeX - 0.8f, badgeY - 0.7f, 0);
        GL11.glScalef(0.58f, 0.58f, 0.58f);
        mc.getItemRenderer().renderItemIntoGUI(razbafBadgeStack, 0, 0);
        GL11.glPopMatrix();
        prepareItemGlState();
    }

    private void prepareItemGlState() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        GL11.glMatrixMode(GL11.GL_TEXTURE);
        GL11.glLoadIdentity();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GlStateManager.activeTexture(GL13.GL_TEXTURE0);
        RenderSystem.enableTexture();
        RenderSystem.disableAlphaTest();
        RenderSystem.enableAlphaTest();
        RenderSystem.defaultAlphaFunc();
        RenderSystem.defaultBlendFunc();
        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void drawCooldownOverlay(float x, float y, float w, float progress, int alpha) {
        float oh = ITEM_HEIGHT * MathUtil.clamp(progress, 0.0f, 1.0f);
        if (oh <= 0.1f) return;
        int color = ColorUtils.rgba(60, 60, 60, (int) (alpha * 0.6f));
        int sf = (int) mc.getMainWindow().getGuiScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        int scissorX = (int) Math.floor(x * sf);
        int scissorY = (int) Math.floor(mc.getMainWindow().getHeight() - (y + ITEM_HEIGHT) * sf);
        int scissorW = Math.max(1, (int) Math.ceil(w * sf));
        int scissorH = Math.max(1, (int) Math.ceil(oh * sf));
        GL11.glScissor(scissorX, scissorY, scissorW, scissorH);
        RenderUtility.drawRoundedRect(x, y, w, ITEM_HEIGHT, RADIUS_VEC, color);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private void getVisibleItemsFromInventory(FTHelper ftHelper, List<FunItem> result) {
        result.clear();
        addedItemNames.clear();
        if (mc.player == null || ftHelper == null) return;

        int invSize = mc.player.inventory.getSizeInventory();

        for (int i = 0; i < invSize; i++) {
            ItemStack stack = mc.player.inventory.getStackInSlot(i);
            if (stack.isEmpty()) continue;

            List<FunItemType> types = TYPES_BY_ITEM.get(stack.getItem());
            if (types == null) continue;

            String tagLower = null;
            String displayLower = null;
            boolean tagResolved = false;
            boolean displayResolved = false;

            for (FunItemType type : types) {
                if (addedItemNames.contains(type.name)) continue;

                boolean matches = false;
                if (type.nbtTag == null && type.displaySearchLower == null) {
                    matches = true;
                } else {
                    if (type.nbtTag != null && !tagResolved) {
                        tagResolved = true;
                        if (stack.hasTag()) tagLower = stack.getTag().toString().toLowerCase(Locale.ROOT);
                    }
                    if (type.nbtTagLower != null && tagLower != null && tagLower.contains(type.nbtTagLower)) {
                        matches = true;
                    }
                    if (!matches && type.displaySearchLower != null) {
                        if (!displayResolved) {
                            displayResolved = true;
                            String raw = TextFormatting.getTextWithoutFormattingCodes(stack.getDisplayName().getString());
                            if (raw != null) displayLower = raw.toLowerCase(Locale.ROOT);
                        }
                        if (displayLower != null && displayLower.contains(type.displaySearchLower)) {
                            matches = true;
                        }
                    }
                }

                if (!matches) continue;

                BindSetting bind = getBindSettingForItem(type, ftHelper);
                if (bind == null || bind.get() == -1) {
                    continue;
                }
                String keyText = resolveKeyText(bind);
                result.add(new FunItem(type.name, createDisplayStack(type, stack), i, keyText, computeItemWidth(keyText)));
                addedItemNames.add(type.name);
                break;
            }
        }
        addRazbafItem(result, ftHelper);
    }

    private void buildTestItems(FTHelper ftHelper, List<FunItem> items) {
        items.clear();
        if (ftHelper == null) return;
        for (FunItemType type : FUN_ITEM_TYPES) {
            BindSetting bind = getBindSettingForItem(type, ftHelper);
            if (bind == null || bind.get() == -1) continue;
            String keyText = resolveKeyText(bind);
            items.add(new FunItem(type.name, createDisplayStack(type, new ItemStack(type.item)), -1, keyText, computeItemWidth(keyText)));
        }
        addRazbafItem(items, ftHelper);
    }

    private void addRazbafItem(List<FunItem> items, FTHelper ftHelper) {
        if (ftHelper.razbafKey == null || ftHelper.razbafKey.get() == -1) {
            return;
        }

        String keyText = resolveKeyText(ftHelper.razbafKey);
        items.add(new FunItem(RAZBAF_NAME, createRazbafDisplayStack(), -1, keyText, computeItemWidth(keyText)));
    }

    private static ItemStack createDisplayStack(FunItemType type, ItemStack source) {
        ItemStack display = source.copy();
        if (type.potionColor != NO_POTION_COLOR && display.getItem() == Items.SPLASH_POTION) {
            display.getOrCreateTag().putInt("CustomPotionColor", type.potionColor);
        }
        return display;
    }

    private static ItemStack createRazbafDisplayStack() {
        ItemStack stack = new ItemStack(Items.SPLASH_POTION);
        stack.getOrCreateTag().putInt("CustomPotionColor", RAZBAF_POTION_COLOR);
        stack.addEnchantment(Enchantments.UNBREAKING, 1);
        return stack;
    }

    private String resolveKeyText(BindSetting bind) {
        if (bind == null || bind.get() == -1) return "-";
        return formatKeyName(KeyStorage.getKey(bind.get()));
    }

    private boolean isRazbafItem(FunItem item) {
        return item != null && RAZBAF_NAME.equals(item.name);
    }

    private float computeItemWidth(String keyText) {
        return ITEM_HEIGHT;
    }

    private String formatKeyName(String keyName) {
        if (keyName.startsWith("MOUSE")) return "M" + keyName.substring(5);
        return keyName.length() > 3 ? keyName.substring(0, 3) : keyName;
    }

    private BindSetting getBindSettingForItem(FunItemType itemType, FTHelper ftHelper) {
        switch (itemType.name) {
            case "Дезориентация": return ftHelper.disorientationKey;
            case "Трапка": return ftHelper.trapKey;
            case "Явная Пыль": return ftHelper.blatantKey;
            case "Арбалет": return ftHelper.bowKey;
            case "Отрыжка": return ftHelper.otrigaKey;
            case "Серная кислота": return ftHelper.serkaKey;
            case "Пласт": return ftHelper.plastKey;
            case "Сопли флеша": return ftHelper.flashKey;
            case "Огненный Смерч": return ftHelper.flameKey;
            case "Божья Аура": return ftHelper.auraKey;
            case "Снежок Заморозка": return ftHelper.snowballKey;
            case "Зелье Киллера": return ftHelper.killerKey;
            case "Зелье Медика": return ftHelper.medicKey;
            case "Зелье Победителя": return ftHelper.winnerKey;
            case "Зелье Агента": return ftHelper.agentKey;
            case "Хлопушка": return ftHelper.xlopyshkaKey;
            case "Святая Вода": return ftHelper.svatvodaKey;
            case "Зелье Гнева": return ftHelper.gnevkaKey;
            case "Зелье Паладина": return ftHelper.paladinKey;
            case "Зелье Ассасина": return ftHelper.assasinKey;
            case "Зелье Радиации": return ftHelper.radiaciaKey;
            case "Снотворное": return ftHelper.snotvornoeKey;
            default: return null;
        }
    }

    private static class FunItemType {
        final String name;
        final Item item;
        final String nbtTag;
        final String nbtTagLower;
        final String displaySearch;
        final String displaySearchLower;
        final int potionColor;

        FunItemType(String name, Item item, String nbtTag, String displaySearch, int potionColor) {
            this.name = name;
            this.item = item;
            this.nbtTag = nbtTag;
            this.nbtTagLower = nbtTag != null ? nbtTag.toLowerCase(Locale.ROOT) : null;
            this.displaySearch = displaySearch;
            this.displaySearchLower = displaySearch != null ? displaySearch.toLowerCase(Locale.ROOT) : null;
            this.potionColor = potionColor;
        }
    }

    private static class FunItem {
        final String name;
        final ItemStack itemStack;
        final int slotIndex;
        final String keyText;
        final float itemWidth;

        FunItem(String name, ItemStack itemStack, int slotIndex, String keyText, float itemWidth) {
            this.name = name;
            this.itemStack = itemStack;
            this.slotIndex = slotIndex;
            this.keyText = keyText;
            this.itemWidth = itemWidth;
        }
    }
}
