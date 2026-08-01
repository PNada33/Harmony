package xd.harm.ui.clickgui.figura;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.text.StringTextComponent;
import org.lwjgl.opengl.GL11;
import xd.harm.utils.render.font.Fonts;
import xd.harm.utils.figura.BbModel;
import xd.harm.utils.figura.BbModelRenderer;
import xd.harm.utils.figura.FiguraAvatarInstaller;
import xd.harm.utils.figura.FiguraAvatarLibrary;
import xd.harm.utils.figura.FiguraAvatarPreviews;
import xd.harm.utils.figura.FiguraPackSettings;
import xd.harm.utils.figura.FiguraWear;
import xd.harm.utils.figura.ModuleToggleBridge;
import xd.harm.utils.figura.PetModuleBridge;
import xd.harm.utils.render.color.ColorUtils;
import xd.harm.utils.render.rect.RenderUtility;
import xd.harm.Harmony;
import xd.harm.modules.impl.render.Theme;
import xd.harm.utils.figura.CosmeticFeatures;
import xd.harm.utils.figura.CosmeticPreviewRenderer;
import xd.harm.utils.figura.CosmeticRenderer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Окно библиотеки косметики в стиле Rich-клиента: вкладки секций, поиск,
 * сетка карточек с пагинацией, избранное и живое 3D-превью справа.
 *
 * Питомцы и косметические функции (Катана, Китайская шляпа, Плащ,
 * Погладить) живут только здесь — в ClickGUI их больше нет.
 */
public class FiguraCosmeticScreen extends Screen {

    private static final int BG = ColorUtils.rgba(16, 17, 22, 235);
    private static final int PANEL = ColorUtils.rgba(24, 26, 33, 255);
    private static final int CARD = ColorUtils.rgba(31, 34, 43, 255);
    private static final int CARD_HOVER = ColorUtils.rgba(41, 45, 57, 255);
    private static final int TEXT = ColorUtils.rgba(235, 237, 245, 255);
    private static final int TEXT_DIM = ColorUtils.rgba(150, 155, 170, 255);
    private static final int GOLD = ColorUtils.rgba(255, 199, 84, 255);

    private int accent() {
        return Theme.MainColor(0);
    }

    private static final int COLUMNS = 3;
    private static final int CARD_HEIGHT = 74;
    private static final int GAP = 8;

    private final Screen parent;

    private static FiguraAvatarLibrary.Section lastSection = FiguraAvatarLibrary.Section.ALL;

    private FiguraAvatarLibrary.Section section = lastSection;
    private String search = "";
    private boolean searchFocused;
    private boolean favoritesOnly;
    private int page;
    /** Анимация перелистывания: 1 — только что перелистнули, 0 — анимация закончилась. */
    private float pageFade;
    /** Направление последнего перелистывания: +1 вниз, -1 вверх. */
    private int pageDir = 1;
    /** Анимация переключения вкладки: 1 — только что переключили, 0 — закончилась. */
    private float tabFade;
    /** Предыдущая вкладка для анимации. */
    private FiguraAvatarLibrary.Section prevSection;

    private String selected;
    private float previewRotation;
    private long lastFrame = System.currentTimeMillis();

    private final Map<String, List<BbModelRenderer>> previewCache = new HashMap<String, List<BbModelRenderer>>();
    private final Set<String> previewLoading = new HashSet<String>();

    /** Открытая панель настроек пака (ПКМ по карточке) или null. */
    private FiguraAvatarLibrary.Entry settingsEntry;

    public FiguraCosmeticScreen(Screen parent) {
        super(new StringTextComponent("Harmony Cosmetics"));
        this.parent = parent;
        this.selected = FiguraWear.getWornFolder();
    }

    @Override
    protected void init() {
        FiguraWear.bootstrap();
        FiguraAvatarLibrary.all();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ------------------------------------------------------------ Геометрия

    private float panelW() {
        return Math.min(this.width - 40f, 720f);
    }

    private float panelH() {
        return Math.min(this.height - 40f, 430f);
    }

    private float panelX() {
        return (this.width - panelW()) / 2f;
    }

    private float panelY() {
        return (this.height - panelH()) / 2f;
    }

    private float rightX() {
        return panelX() + panelW() - 212f;
    }

    private float gridX() {
        return panelX() + 12f;
    }

    private float gridY() {
        return panelY() + 74f;
    }

    private float gridW() {
        return rightX() - 12f - gridX();
    }

    private float gridH() {
        return panelY() + panelH() - 34f - gridY();
    }

    private float cardW() {
        return (gridW() - GAP * (COLUMNS - 1)) / COLUMNS;
    }

    private int rows() {
        int rows = (int) ((gridH() + GAP) / (CARD_HEIGHT + GAP));
        return Math.max(1, rows);
    }

    private int perPage() {
        return rows() * COLUMNS;
    }

    private List<FiguraAvatarLibrary.Entry> visible() {
        return FiguraAvatarLibrary.filter(section, search, favoritesOnly);
    }

    /** Перелистывание с плавной анимацией. */
    private void gotoPage(int next) {
        if (next == page || next < 0) {
            return;
        }
        pageDir = next > page ? 1 : -1;
        page = next;
        pageFade = 1f;
    }

    private int pageCount(int total) {
        return Math.max(1, (total + perPage() - 1) / perPage());
    }

    private static boolean hovered(double mouseX, double mouseY, float x, float y, float w, float h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    // -------------------------------------------------------------- Рендер

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float partialTicks) {
        long now = System.currentTimeMillis();
        float delta = (now - lastFrame) / 1000f;
        lastFrame = now;
        previewRotation += delta * 28f;
        if (previewRotation > 360f) {
            previewRotation -= 360f;
        }
        if (pageFade > 0f) {
            pageFade -= delta * 5f;
            if (pageFade < 0f) {
                pageFade = 0f;
            }
        }
        if (tabFade > 0f) {
            tabFade -= delta * 6f;
            if (tabFade < 0f) {
                tabFade = 0f;
            }
        }

        this.renderBackground(ms);

        float px = panelX();
        float py = panelY();
        float pw = panelW();
        float ph = panelH();

        RenderUtility.drawRoundedRect(px, py, pw, ph, 10f, BG);
        RenderUtility.drawRoundedRect(px, py, pw, 62f, 10f, PANEL);

        Fonts.sfuy.drawText(ms, "Harmony Cosmetics", px + 14f, py + 14f, TEXT, 9f);
        String status = FiguraWear.isLoading() ? "Загрузка..." : FiguraWear.getStatus();
        if (status != null && !status.isEmpty()) {
            Fonts.sfuy.drawText(ms, status, px + 14f, py + 27f, TEXT_DIM, 6.5f);
        }

        renderSearch(ms, mouseX, mouseY);
        renderTabs(ms, mouseX, mouseY);
        renderCards(ms, mouseX, mouseY);
        renderPagination(ms, mouseX, mouseY);
        renderRightPanel(ms, mouseX, mouseY);
        renderSettingsPanel(ms, mouseX, mouseY);

        super.render(ms, mouseX, mouseY, partialTicks);
    }

    private void renderSearch(MatrixStack ms, int mouseX, int mouseY) {
        float x = rightX();
        float y = panelY() + 12f;
        float w = 212f - 12f;
        float h = 20f;
        int color = searchFocused ? ColorUtils.rgba(45, 50, 64, 255) : ColorUtils.rgba(35, 38, 48, 255);
        RenderUtility.drawRoundedRect(x, y, w, h, 5f, color);
        String text = search.isEmpty() && !searchFocused ? "Поиск..." : search;
        int textColor = search.isEmpty() && !searchFocused ? TEXT_DIM : TEXT;
        String shown = text;
        while (Fonts.sfuy.getWidth(shown, 7f) > w - 16f && shown.length() > 1) {
            shown = shown.substring(1);
        }
        Fonts.sfuy.drawText(ms, shown + (searchFocused && (System.currentTimeMillis() / 500) % 2 == 0 ? "|" : ""),
                x + 7f, y + 7f, textColor, 7f);
    }

    private void renderTabs(MatrixStack ms, int mouseX, int mouseY) {
        float x = panelX() + 12f;
        float y = panelY() + 44f;
        FiguraAvatarLibrary.Section[] sections = FiguraAvatarLibrary.Section.values();
        for (int i = 0; i < sections.length; i++) {
            FiguraAvatarLibrary.Section s = sections[i];
            String title = s.title;
            float w = Fonts.sfuy.getWidth(title, 7f) + 16f;
            boolean active = s == section;
            boolean wasActive = s == prevSection && tabFade > 0f;
            boolean hover = hovered(mouseX, mouseY, x, y, w, 20f);
            int bg = active ? accent() : (hover ? CARD_HOVER : CARD);
            if (wasActive && !active) {
                bg = ColorUtils.setAlpha(bg, (int) (255 * (1f - tabFade)));
            }
            RenderUtility.drawRoundedRect(x, y, w, 20f, 5f, bg);
            int textColor = active ? ColorUtils.rgba(16, 18, 24, 255) : TEXT;
            if (wasActive && !active) {
                textColor = ColorUtils.setAlpha(textColor, (int) (255 * (1f - tabFade)));
            }
            Fonts.sfuy.drawText(ms, title, x + 8f, y + 7f, textColor, 7f);
            x += w + 5f;
        }

        float favW = Fonts.sfuy.getWidth("Избранное", 7f) + 16f;
        boolean hover = hovered(mouseX, mouseY, x, y, favW, 20f);
        RenderUtility.drawRoundedRect(x, y, favW, 20f, 5f,
                favoritesOnly ? GOLD : (hover ? CARD_HOVER : CARD));
        Fonts.sfuy.drawText(ms, "Избранное", x + 8f, y + 7f,
                favoritesOnly ? ColorUtils.rgba(30, 24, 10, 255) : TEXT, 7f);
    }

    private void renderCards(MatrixStack ms, int mouseX, int mouseY) {
        List<FiguraAvatarLibrary.Entry> entries = visible();
        int pages = pageCount(entries.size());
        if (page >= pages) {
            page = pages - 1;
        }
        if (page < 0) {
            page = 0;
        }

        if (entries.isEmpty()) {
            String message = FiguraAvatarInstaller.isRunning()
                    ? "Установка набора..."
                    : "Аватары не найдены. Нажми «Установить набор».";
            Fonts.sfuy.drawText(ms, message, gridX(), gridY() + 10f, TEXT_DIM, 7f);
            return;
        }

        int from = page * perPage();
        int to = Math.min(entries.size(), from + perPage());
        float cw = cardW();

        // Плавная анимация скролла: карточки выезжают с появлением.
        float ease = pageFade * pageFade;
        float alpha = 1f - 0.95f * ease;

        for (int i = from; i < to; i++) {
            int index = i - from;
            int col = index % COLUMNS;
            int row = index / COLUMNS;
            float x = gridX() + col * (cw + GAP);
            float y = gridY() + row * (CARD_HEIGHT + GAP);
            // Нижние ряды догоняют чуть позже — получается мягкая волна.
            float rowEase = ease * (1f + row * 0.35f);
            if (rowEase > 1f) {
                rowEase = 1f;
            }
            float slide = pageDir * 22f * rowEase;
            renderCard(ms, entries.get(i), x, y + slide, cw, mouseX, mouseY, alpha);
        }
    }

    /** Приглушает прозрачность цвета для анимации скролла. */
    private static int fade(int color, float factor) {
        int alpha = (int) ((color >> 24 & 255) * factor);
        if (alpha < 0) {
            alpha = 0;
        }
        if (alpha > 255) {
            alpha = 255;
        }
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    private void renderCard(MatrixStack ms, FiguraAvatarLibrary.Entry entry, float x, float y, float w,
                            int mouseX, int mouseY, float anim) {
        boolean hover = hovered(mouseX, mouseY, x, y, w, CARD_HEIGHT);
        FiguraAvatarLibrary.Entry active = entry.activeVariant();
        boolean worn = entry.moduleCard ? isPetActive(entry) : FiguraWear.isGroupWorn(entry);
        boolean chosen = active.folder.equals(selected) || entry.folder.equals(selected);

        RenderUtility.drawRoundedRect(x, y, w, CARD_HEIGHT, 7f, fade(hover ? CARD_HOVER : CARD, anim));
        if (worn) {
            RenderUtility.drawRoundedRect(x, y, 3f, CARD_HEIGHT, 2f, fade(accent(), anim));
        } else if (chosen) {
            RenderUtility.drawRoundedRect(x, y, 3f, CARD_HEIGHT, 2f,
                    fade(ColorUtils.rgba(90, 95, 115, 255), anim));
        }

        float iconSize = 44f;
        float iconX = x + 8f;
        float iconY = y + (CARD_HEIGHT - iconSize) / 2f;
        // У всех карточек своя рисованная иконка в едином стиле.
        drawCardIcon(ms, entry, iconX, iconY, iconSize, worn, anim);

        float textX = iconX + iconSize + 8f;
        float maxText = x + w - textX - 16f;
        Fonts.sfuy.drawText(ms, trim(entry.name, maxText, 7.5f), textX, y + 16f, fade(TEXT, anim), 7.5f);
        Fonts.sfuy.drawText(ms, trim(entry.subtitle(), maxText, 6f), textX, y + 30f, fade(TEXT_DIM, anim), 6f);
        String bottom = worn ? "Надет" : entry.section.title;
        if (entry.moduleCard) {
            bottom = worn ? "Надет" : (entry.moduleName != null ? "Функция" : "Питомец");
        }
        if (entry.hasVariants()) {
            bottom = bottom + "  •  " + active.variantLabel + " (видов: " + entry.variants.size() + ")";
        }
        Fonts.sfuy.drawText(ms, trim(bottom, maxText, 6f), textX, y + 46f,
                fade(worn ? accent() : ColorUtils.rgba(120, 125, 140, 255), anim), 6f);

        float starX = x + w - 14f;
        float starY = y + 8f;
        boolean favorite = FiguraAvatarLibrary.isFavorite(entry.folder);
        RenderUtility.drawRoundedRect(starX, starY, 7f, 7f, 3.5f,
                fade(favorite ? GOLD : ColorUtils.rgba(70, 74, 88, 255), anim));
    }

    // ------------------------------------------------------- Рисованные иконки

    /** Стабильное число из имени карточки — из него рождается цвет и узор. */
    private int seedOf(FiguraAvatarLibrary.Entry entry) {
        String key = entry == null ? "?" : (entry.folder != null ? entry.folder : entry.name);
        if (key == null) {
            key = "?";
        }
        int hash = 7;
        for (int i = 0; i < key.length(); i++) {
            hash = hash * 31 + key.charAt(i);
        }
        return hash < 0 ? -hash : hash;
    }

    /** Свой оттенок для каждой карточки — палитра одного стиля, разные тона. */
    private int iconColor(FiguraAvatarLibrary.Entry entry) {
        int seed = seedOf(entry);
        float hue = (seed % 360) / 360f;
        return hsb(hue, 0.52f, 0.98f);
    }

    /** HSB ? RGB без java.awt: нужно для ровной палитры иконок. */
    private int hsb(float h, float s, float v) {
        float r = 0f;
        float g = 0f;
        float b = 0f;
        int sector = (int) (h * 6f) % 6;
        float f = h * 6f - (float) Math.floor(h * 6f);
        float p = v * (1f - s);
        float q = v * (1f - f * s);
        float t = v * (1f - (1f - f) * s);
        if (sector == 0) { r = v; g = t; b = p; }
        else if (sector == 1) { r = q; g = v; b = p; }
        else if (sector == 2) { r = p; g = v; b = t; }
        else if (sector == 3) { r = p; g = q; b = v; }
        else if (sector == 4) { r = t; g = p; b = v; }
        else { r = v; g = p; b = q; }
        return ColorUtils.rgba((int) (r * 255f), (int) (g * 255f), (int) (b * 255f), 255);
    }

    /** Какую пиктограмму рисовать для карточки. */
    private String iconKindOf(FiguraAvatarLibrary.Entry entry) {
        if (entry == null) {
            return "avatar";
        }
        if (entry.iconKind != null) {
            return entry.iconKind;
        }
        if (entry.headMount) {
            return "hat";
        }
        if (entry.section == FiguraAvatarLibrary.Section.PETS) {
            return "pet";
        }
        if (entry.section == FiguraAvatarLibrary.Section.ACCESSORIES) {
            return "accessory";
        }
        if (entry.section == FiguraAvatarLibrary.Section.WEAPONS) {
            return "sword";
        }
        if (entry.section == FiguraAvatarLibrary.Section.WINGS) {
            return "wing";
        }
        if (entry.section == FiguraAvatarLibrary.Section.SCRIPTS) {
            return "script";
        }
        if (entry.section == FiguraAvatarLibrary.Section.EMOTES) {
            return "emote";
        }
        return "avatar";
    }

    /**
     * Рисует иконку карточки: тёмная подложка с градиентом, свой узор,
     * пиктограмма типа и буква-монограмма. Стиль один для всех разделов.
     */
    private void drawCardIcon(MatrixStack ms, FiguraAvatarLibrary.Entry entry,
                              float x, float y, float size, boolean worn, float anim) {
        int tone = iconColor(entry);
        int deep = ColorUtils.interpolateColor(tone, ColorUtils.rgba(13, 14, 19, 255), 0.78f);
        int mid = ColorUtils.interpolateColor(tone, ColorUtils.rgba(20, 22, 28, 255), 0.5f);
        int seed = seedOf(entry);

        RenderUtility.drawRoundedRect(x, y, size, size, 5f, fade(deep, anim));

        // Мягкий градиент сверху вниз.
        int steps = 7;
        for (int i = 0; i < steps; i++) {
            float t = i / (float) steps;
            int alpha = (int) (60f * (1f - t));
            if (alpha <= 2) {
                continue;
            }
            RenderUtility.drawRoundedRect(x + 1.5f, y + 1.5f + t * (size - 3f), size - 3f,
                    (size - 3f) / steps, 2f, fade(ColorUtils.setAlpha(mid, alpha), anim));
        }

        // Узор — у каждой карточки свой рисунок из трёх вариантов.
        int pattern = seed % 3;
        int patternColor = ColorUtils.setAlpha(tone, 52);
        if (pattern == 0) {
            for (int i = 0; i < 5; i++) {
                float step = size / 5f;
                RenderUtility.drawRoundedRect(x + 2f + i * step, y + 2f, 1.2f, size - 4f, 0.6f,
                        fade(patternColor, anim));
            }
        } else if (pattern == 1) {
            for (int i = 0; i < 7; i++) {
                float d = 2f + i * (size - 6f) / 7f;
                RenderUtility.drawRoundedRect(x + d, y + d, 2.4f, 2.4f, 1.2f, fade(patternColor, anim));
            }
        } else {
            for (int i = 0; i < 4; i++) {
                float r = 4f + i * (size / 8f);
                RenderUtility.drawRoundedRect(x + size / 2f - r, y + size / 2f - r, r * 2f, 1.1f, 0.5f,
                        fade(patternColor, anim));
                RenderUtility.drawRoundedRect(x + size / 2f - r, y + size / 2f + r, r * 2f, 1.1f, 0.5f,
                        fade(patternColor, anim));
            }
        }

        int ink = worn
                ? ColorUtils.rgba(255, 255, 255, 255)
                : ColorUtils.interpolateColor(tone, ColorUtils.rgba(255, 255, 255, 255), 0.62f);
        drawGlyph(iconKindOf(entry), x, y, size, ink, anim);

        // Верхний блик и монограмма в углу.
        RenderUtility.drawRoundedRect(x + 1.5f, y + 1f, size - 3f, 1f, 0.5f,
                fade(ColorUtils.setAlpha(tone, 120), anim));
        String letter = entry == null || entry.name == null || entry.name.isEmpty()
                ? "?"
                : entry.name.substring(0, 1).toUpperCase();
        // Монограмма растёт вместе с иконкой: одна и та же отрисовка работает
        // и для карточки 44px, и для крупного превью в правой панели.
        float letterSize = Math.max(6f, size * 0.14f);
        Fonts.sfbold.drawText(ms, letter, x + size * 0.08f, y + size - letterSize - size * 0.06f,
                fade(ColorUtils.setAlpha(tone, 165), anim), letterSize);
    }

    /** Пиктограммы собраны из скруглённых прямоугольников на сетке 16?16. */
    private void drawGlyph(String kind, float x, float y, float size, int ink, float anim) {
        float u = size / 16f;
        float cx = x + size / 2f;
        int soft = ColorUtils.setAlpha(ink, 150);

        if ("hat".equals(kind)) {
            for (int i = 0; i < 5; i++) {
                float w = (2.4f + i * 2.1f) * u;
                RenderUtility.drawRoundedRect(cx - w / 2f, y + (3.2f + i * 1.55f) * u, w, 1.6f * u,
                        0.6f * u, fade(ink, anim));
            }
            RenderUtility.drawRoundedRect(cx - 6.4f * u, y + 11f * u, 12.8f * u, 1.9f * u, 0.9f * u,
                    fade(ink, anim));
            RenderUtility.drawRoundedRect(cx - 0.6f * u, y + 1.8f * u, 1.2f * u, 1.5f * u, 0.6f * u,
                    fade(soft, anim));
            return;
        }
        if ("coat".equals(kind)) {
            RenderUtility.drawRoundedRect(cx - 2.6f * u, y + 2.6f * u, 5.2f * u, 1.6f * u, 0.7f * u,
                    fade(soft, anim));
            for (int i = 0; i < 6; i++) {
                float w = (4.6f + i * 1.5f) * u;
                RenderUtility.drawRoundedRect(cx - w / 2f, y + (4.2f + i * 1.55f) * u, w, 1.5f * u,
                        0.6f * u, fade(ink, anim));
            }
            RenderUtility.drawRoundedRect(cx - 0.5f * u, y + 4.4f * u, 1f * u, 8.4f * u, 0.5f * u,
                    fade(ColorUtils.setAlpha(ink, 90), anim));
            return;
        }
        if ("sword".equals(kind)) {
            RenderUtility.drawRoundedRect(cx - 1.1f * u, y + 2.2f * u, 2.2f * u, 7.6f * u, 0.9f * u,
                    fade(ink, anim));
            RenderUtility.drawRoundedRect(cx - 3.6f * u, y + 9.6f * u, 7.2f * u, 1.4f * u, 0.6f * u,
                    fade(ink, anim));
            RenderUtility.drawRoundedRect(cx - 0.8f * u, y + 11f * u, 1.6f * u, 2.6f * u, 0.7f * u,
                    fade(soft, anim));
            RenderUtility.drawRoundedRect(cx - 1.2f * u, y + 13.2f * u, 2.4f * u, 1.2f * u, 0.6f * u,
                    fade(ink, anim));
            return;
        }
        if ("paw".equals(kind)) {
            RenderUtility.drawRoundedRect(cx - 3.2f * u, y + 7.6f * u, 6.4f * u, 5f * u, 2.4f * u,
                    fade(ink, anim));
            float[] px = {-3.6f, -1.4f, 0.9f, 3.1f};
            float[] py = {5.6f, 4.2f, 4.2f, 5.6f};
            for (int i = 0; i < 4; i++) {
                RenderUtility.drawRoundedRect(cx + px[i] * u, y + py[i] * u, 2f * u, 2.4f * u, 1f * u,
                        fade(ink, anim));
            }
            return;
        }
        if ("hand".equals(kind)) {
            RenderUtility.drawRoundedRect(cx - 3f * u, y + 7f * u, 6.4f * u, 5.6f * u, 1.6f * u,
                    fade(ink, anim));
            for (int i = 0; i < 3; i++) {
                RenderUtility.drawRoundedRect(cx - 2.6f * u + i * 2.1f * u, y + 3.4f * u, 1.6f * u,
                        4.2f * u, 0.8f * u, fade(ink, anim));
            }
            RenderUtility.drawRoundedRect(cx + 3f * u, y + 8f * u, 2.6f * u, 1.6f * u, 0.8f * u,
                    fade(ink, anim));
            RenderUtility.drawRoundedRect(cx - 5.6f * u, y + 4.4f * u, 1.4f * u, 1.4f * u, 0.7f * u,
                    fade(soft, anim));
            RenderUtility.drawRoundedRect(cx - 6.6f * u, y + 7f * u, 1.1f * u, 1.1f * u, 0.55f * u,
                    fade(soft, anim));
            return;
        }
        if ("emote".equals(kind)) {
            // Забавы — улыбающееся лицо с искорками.
            RenderUtility.drawRoundedRect(cx - 5.4f * u, y + 3f * u, 10.8f * u, 10.8f * u, 5.4f * u,
                    fade(ColorUtils.setAlpha(ink, 70), anim));
            RenderUtility.drawRoundedRect(cx - 2.6f * u, y + 6.2f * u, 1.6f * u, 2.4f * u, 0.8f * u,
                    fade(ink, anim));
            RenderUtility.drawRoundedRect(cx + 1f * u, y + 6.2f * u, 1.6f * u, 2.4f * u, 0.8f * u,
                    fade(ink, anim));
            RenderUtility.drawRoundedRect(cx - 3.2f * u, y + 10.2f * u, 6.4f * u, 1.5f * u, 0.7f * u,
                    fade(ink, anim));
            RenderUtility.drawRoundedRect(cx - 3.8f * u, y + 9.4f * u, 1.4f * u, 1.4f * u, 0.7f * u,
                    fade(ink, anim));
            RenderUtility.drawRoundedRect(cx + 2.4f * u, y + 9.4f * u, 1.4f * u, 1.4f * u, 0.7f * u,
                    fade(ink, anim));
            RenderUtility.drawRoundedRect(cx + 4.6f * u, y + 2.2f * u, 1.6f * u, 1.6f * u, 0.8f * u,
                    fade(soft, anim));
            RenderUtility.drawRoundedRect(cx - 6.4f * u, y + 4.4f * u, 1.2f * u, 1.2f * u, 0.6f * u,
                    fade(soft, anim));
            return;
        }
        if ("pet".equals(kind)) {
            RenderUtility.drawRoundedRect(cx - 4.4f * u, y + 6.6f * u, 7.4f * u, 4.4f * u, 2f * u,
                    fade(ink, anim));
            RenderUtility.drawRoundedRect(cx + 1.6f * u, y + 4f * u, 4.2f * u, 4.2f * u, 1.8f * u,
                    fade(ink, anim));
            RenderUtility.drawRoundedRect(cx + 2f * u, y + 2.6f * u, 1.3f * u, 1.8f * u, 0.6f * u,
                    fade(ink, anim));
            RenderUtility.drawRoundedRect(cx + 4.2f * u, y + 2.6f * u, 1.3f * u, 1.8f * u, 0.6f * u,
                    fade(ink, anim));
            RenderUtility.drawRoundedRect(cx - 5.8f * u, y + 5.2f * u, 1.4f * u, 3f * u, 0.7f * u,
                    fade(soft, anim));
            RenderUtility.drawRoundedRect(cx - 3.6f * u, y + 10.6f * u, 1.5f * u, 2.4f * u, 0.7f * u,
                    fade(ink, anim));
            RenderUtility.drawRoundedRect(cx + 0.4f * u, y + 10.6f * u, 1.5f * u, 2.4f * u, 0.7f * u,
                    fade(ink, anim));
            return;
        }
        if ("wing".equals(kind)) {
            for (int i = 0; i < 4; i++) {
                float w = (2f + i * 1.1f) * u;
                float h = (4.6f - i * 0.8f) * u;
                RenderUtility.drawRoundedRect(cx - 1.2f * u - w - i * 1.15f * u, y + (4.4f + i * 1.5f) * u,
                        w, h, 0.8f * u, fade(ink, anim));
                RenderUtility.drawRoundedRect(cx + 1.2f * u + i * 1.15f * u, y + (4.4f + i * 1.5f) * u,
                        w, h, 0.8f * u, fade(ink, anim));
            }
            RenderUtility.drawRoundedRect(cx - 0.7f * u, y + 4f * u, 1.4f * u, 8f * u, 0.7f * u,
                    fade(soft, anim));
            return;
        }
        if ("script".equals(kind)) {
            RenderUtility.drawRoundedRect(cx - 4.6f * u, y + 2.8f * u, 9.2f * u, 10.4f * u, 1.2f * u,
                    fade(ColorUtils.setAlpha(ink, 120), anim));
            float[] widths = {6.2f, 4.4f, 5.6f, 3.2f};
            for (int i = 0; i < widths.length; i++) {
                RenderUtility.drawRoundedRect(cx - 3.2f * u, y + (4.4f + i * 2.1f) * u, widths[i] * u,
                        1.1f * u, 0.5f * u, fade(ink, anim));
            }
            return;
        }
        if ("accessory".equals(kind)) {
            RenderUtility.drawRoundedRect(cx - 4.2f * u, y + 4.6f * u, 8.4f * u, 8.4f * u, 4.2f * u,
                    fade(ink, anim));
            RenderUtility.drawRoundedRect(cx - 2.4f * u, y + 6.4f * u, 4.8f * u, 4.8f * u, 2.4f * u,
                    fade(ColorUtils.setAlpha(ink, 40), anim));
            RenderUtility.drawRoundedRect(cx - 1.2f * u, y + 2.4f * u, 2.4f * u, 2.4f * u, 1.2f * u,
                    fade(ink, anim));
            return;
        }

        // avatar — базовая фигурка.
        RenderUtility.drawRoundedRect(cx - 2.6f * u, y + 2.8f * u, 5.2f * u, 5.2f * u, 1.6f * u,
                fade(ink, anim));
        RenderUtility.drawRoundedRect(cx - 3.8f * u, y + 8.6f * u, 7.6f * u, 4.8f * u, 1.6f * u,
                fade(ink, anim));
        RenderUtility.drawRoundedRect(cx - 5.6f * u, y + 9f * u, 1.5f * u, 3.4f * u, 0.7f * u,
                fade(soft, anim));
        RenderUtility.drawRoundedRect(cx + 4.1f * u, y + 9f * u, 1.5f * u, 3.4f * u, 0.7f * u,
                fade(soft, anim));
    }

    private String trim(String text, float maxWidth, float size) {
        if (text == null) {
            return "";
        }
        if (Fonts.sfuy.getWidth(text, size) <= maxWidth) {
            return text;
        }
        String result = text;
        while (result.length() > 1 && Fonts.sfuy.getWidth(result + "...", size) > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result + "...";
    }

    private void renderPagination(MatrixStack ms, int mouseX, int mouseY) {
        List<FiguraAvatarLibrary.Entry> entries = visible();
        int pages = pageCount(entries.size());
        float y = panelY() + panelH() - 26f;
        float x = gridX();

        boolean hoverPrev = hovered(mouseX, mouseY, x, y, 22f, 16f);
        boolean hoverNext = hovered(mouseX, mouseY, x + 26f, y, 22f, 16f);
        RenderUtility.drawRoundedRect(x, y, 22f, 16f, 4f, hoverPrev ? CARD_HOVER : CARD);
        RenderUtility.drawRoundedRect(x + 26f, y, 22f, 16f, 4f, hoverNext ? CARD_HOVER : CARD);
        Fonts.sfuy.drawText(ms, "<", x + 9f, y + 5f, TEXT, 7f);
        Fonts.sfuy.drawText(ms, ">", x + 35f, y + 5f, TEXT, 7f);
        Fonts.sfuy.drawText(ms, (page + 1) + " / " + pages + "   всего: " + entries.size(),
                x + 56f, y + 5f, TEXT_DIM, 6.5f);
    }

    // ------------------------------------------------------- Правая панель

    private void renderRightPanel(MatrixStack ms, int mouseX, int mouseY) {
        float x = rightX();
        float y = panelY() + 40f;
        float w = 200f;
        float h = panelY() + panelH() - 12f - y;
        RenderUtility.drawRoundedRect(x, y, w, h, 8f, PANEL);

        FiguraAvatarLibrary.Entry entry = selected == null ? null : FiguraAvatarLibrary.byFolder(selected);
        if (entry != null) {
            entry = entry.activeVariant();
        }
        if (entry == null) {
            entry = FiguraWear.getCurrentEntry();
        }

        float previewH = h - 96f;
        RenderUtility.drawRoundedRect(x + 8f, y + 8f, w - 16f, previewH, 6f, ColorUtils.rgba(18, 20, 26, 255));

        if (entry != null) {
            boolean drawn = false;
            if (entry.moduleCard) {
                drawn = renderModulePreview(ms, entry, x + 8f, y + 8f, w - 16f, previewH);
            }
            if (!drawn) {
                drawn = renderPreview3D(ms, entry, x + 8f, y + 8f, w - 16f, previewH);
            }
            if (!drawn) {
                ResourceLocation preview = FiguraAvatarPreviews.get(entry);
                if (preview != null) {
                    float size = Math.min(w - 32f, previewH - 16f);
                    drawTexture(ms, preview, x + (w - size) / 2f, y + 8f + (previewH - size) / 2f, size, size);
                }
            }
            Fonts.sfuy.drawCenteredText(ms, trim(entry.name, w - 20f, 8f), x + w / 2f, y + previewH + 14f, TEXT, 8f);
            Fonts.sfuy.drawCenteredText(ms, trim(entry.subtitle(), w - 20f, 6f), x + w / 2f, y + previewH + 26f, TEXT_DIM, 6f);
        } else {
            Fonts.sfuy.drawCenteredText(ms, "Выбери аватар", x + w / 2f, y + previewH / 2f, TEXT_DIM, 7f);
        }

        float by = y + h - 58f;
        drawButton(ms, "Надеть", x + 8f, by, (w - 22f) / 2f, 18f, mouseX, mouseY, accent(), true);
        drawButton(ms, "Снять", x + 14f + (w - 22f) / 2f, by, (w - 22f) / 2f, 18f, mouseX, mouseY, CARD, false);
        drawButton(ms, FiguraAvatarInstaller.isRunning() ? FiguraAvatarInstaller.getStatus() : "Установить набор",
                x + 8f, by + 22f, (w - 22f) / 2f, 18f, mouseX, mouseY, CARD, false);
        drawButton(ms, "Обновить", x + 14f + (w - 22f) / 2f, by + 22f, (w - 22f) / 2f, 18f, mouseX, mouseY, CARD, false);
    }

    private void drawButton(MatrixStack ms, String label, float x, float y, float w, float h,
                            int mouseX, int mouseY, int base, boolean isAccent) {
        boolean hover = hovered(mouseX, mouseY, x, y, w, h);
        RenderUtility.drawRoundedRect(x, y, w, h, 5f, hover ? ColorUtils.setAlpha(base, 255) : base);
        int textColor = isAccent ? ColorUtils.rgba(16, 18, 24, 255) : TEXT;
        Fonts.sfuy.drawCenteredText(ms, trim(label, w - 8f, 6.5f), x + w / 2f, y + h / 2f - 3f, textColor, 6.5f);
    }

    /** Живое 3D-превью модели в GUI. Возвращает false, если модель ещё грузится. */
    private boolean renderPreview3D(MatrixStack ms, FiguraAvatarLibrary.Entry entry,
                                    float x, float y, float w, float h) {
        List<BbModelRenderer> list = requestPreview(entry);
        if (list == null || list.isEmpty()) {
            return false;
        }

        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        float minX = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE;
        float maxZ = -Float.MAX_VALUE;
        for (int i = 0; i < list.size(); i++) {
            BbModelRenderer renderer = list.get(i);
            minY = Math.min(minY, renderer.getMinY());
            maxY = Math.max(maxY, renderer.getMaxY());
            minX = Math.min(minX, renderer.getMinX());
            maxX = Math.max(maxX, renderer.getMaxX());
            minZ = Math.min(minZ, renderer.getMinZ());
            maxZ = Math.max(maxZ, renderer.getMaxZ());
        }
        float height = maxY - minY;
        if (height <= 0.001f) {
            return false;
        }

        // Модель крутится, поэтому по ширине берём диагональ основания —
        // иначе длинные вещи (косы, крылья) вылезают за окошко.
        float spanX = Math.max(0f, maxX - minX);
        float spanZ = Math.max(0f, maxZ - minZ);
        float radius = (float) Math.sqrt(spanX * spanX + spanZ * spanZ);
        float centerX = (minX + maxX) / 2f;
        float centerZ = (minZ + maxZ) / 2f;

        float byHeight = (h * 0.80f) / height;
        float byWidth = radius > 0.001f ? (w * 0.82f) / radius : byHeight;
        float size = Math.min(byHeight, byWidth);
        BbModelRenderer.Pose pose = new BbModelRenderer.Pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableAlphaTest();
        RenderSystem.defaultAlphaFunc();
        RenderSystem.enableTexture();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.color4f(1f, 1f, 1f, 1f);

        ms.push();
        ms.translate(x + w / 2f, y + h * 0.9f, 150f);
        ms.scale(size, -size, size);
        ms.rotate(Vector3f.XP.rotationDegrees(12f));
        ms.rotate(Vector3f.YP.rotationDegrees(previewRotation));
        ms.translate(-centerX, -minY, -centerZ);
        for (int i = 0; i < list.size(); i++) {
            try {
                list.get(i).render(ms, pose);
            } catch (Throwable ignored) {
            }
        }
        ms.pop();

        RenderSystem.enableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.disableAlphaTest();
        RenderSystem.disableBlend();
        RenderSystem.color4f(1f, 1f, 1f, 1f);
        return true;
    }

    /**
     * Предпросмотр для карточек-функций и питомцев.
     *
     * Сначала пробуем нарисовать живое 3D-превью через отдельный рендерер.
     * Если для конкретной карточки не хватает данных (например, модель питомца
     * ещё грузится или для карточки вообще нет 3D-геометрии), оставляем старый
     * drawCardIcon() как безопасный fallback.
     */
    private boolean renderModulePreview(MatrixStack ms, FiguraAvatarLibrary.Entry entry,
                                        float x, float y, float w, float h) {
        if (entry == null || !entry.moduleCard) {
            return false;
        }

        List<BbModelRenderer> petPreview = entry.petName != null ? requestPreview(entry) : null;
        if (CosmeticPreviewRenderer.render(ms, entry, x, y, w, h, previewRotation, petPreview)) {
            return true;
        }

        boolean active = isPetActive(entry);
        int tone = iconColor(entry);

        float glow = Math.min(w, h) * 0.86f;
        for (int i = 4; i >= 1; i--) {
            float g = glow * (0.6f + i * 0.11f);
            RenderUtility.drawRoundedRect(x + (w - g) / 2f, y + (h - g) / 2f, g, g, g / 2f,
                    ColorUtils.setAlpha(tone, active ? 5 + i * 3 : 3 + i * 2));
        }

        float size = Math.min(w - 46f, h - 46f);
        if (size < 48f) {
            size = Math.min(w - 16f, h - 16f);
        }
        float ix = x + (w - size) / 2f;
        float iy = y + (h - size) / 2f;

        drawCardIcon(ms, entry, ix, iy, size, active, 1f);

        int frame = active ? accent() : ColorUtils.rgba(58, 62, 76, 255);
        RenderUtility.drawRoundedRect(ix - 2f, iy - 2f, size + 4f, 2f, 1f, frame);
        RenderUtility.drawRoundedRect(ix - 2f, iy + size, size + 4f, 2f, 1f, frame);
        RenderUtility.drawRoundedRect(ix - 2f, iy - 2f, 2f, size + 4f, 1f, frame);
        RenderUtility.drawRoundedRect(ix + size, iy - 2f, 2f, size + 4f, 1f, frame);

        String state = active
                ? "Надет"
                : (entry.moduleName != null ? "Функция выключена" : "Питомец не вызван");
        Fonts.sfuy.drawCenteredText(ms, state, x + w / 2f, iy + size + 12f,
                active ? accent() : TEXT_DIM, 6.5f);
        return true;
    }


    private boolean isPetActive(FiguraAvatarLibrary.Entry entry) {
        if (entry == null || !entry.moduleCard) {
            return false;
        }
        if (entry.moduleName != null) {
            return CosmeticFeatures.isEnabled(entry.moduleName);
        }
        if (entry.petName == null || !PetModuleBridge.isEnabled()) {
            return false;
        }
        String current = PetModuleBridge.currentPet();
        return current != null && current.equalsIgnoreCase(entry.petName);
    }

    /** Включает или выключает карточку-функцию. */
    private void setPetActive(FiguraAvatarLibrary.Entry entry, boolean active) {
        if (entry == null || !entry.moduleCard) {
            return;
        }
        if (entry.moduleName != null) {
            CosmeticFeatures.setEnabled(entry.moduleName, active);
            return;
        }
        if (entry.petName == null) {
            return;
        }
        if (active) {
            PetModuleBridge.summon(entry.petName);
        } else {
            PetModuleBridge.setEnabled(false);
        }
    }

    private List<BbModelRenderer> requestPreview(final FiguraAvatarLibrary.Entry entry) {
        if (entry == null || entry.models.isEmpty() || (entry.moduleCard && entry.petName == null)) {
            return null;
        }
        final String key = entry.key();
        synchronized (previewCache) {
            if (previewCache.containsKey(key)) {
                return previewCache.get(key);
            }
            if (previewLoading.contains(key)) {
                return null;
            }
            previewLoading.add(key);
        }

        Thread thread = new Thread(new Runnable() {
            public void run() {
                List<BbModelRenderer> built = new ArrayList<BbModelRenderer>();
                try {
                    int limit = 0;
                    for (int i = 0; i < entry.models.size() && limit < 8; i++) {
                        Path model = entry.models.get(i);
                        if (!entry.isModelVisible(model)) {
                            continue;
                        }
                        try {
                            BbModel parsed = BbModel.parse(model.toFile());
                            BbModelRenderer renderer = new BbModelRenderer(parsed, entry.modelId(model), entry);
                            if (entry.section == FiguraAvatarLibrary.Section.WEAPONS) {
                                // в паке может лежать две копии оружия (для руки и для спины) —
                                // в превью показываем только одну
                                try {
                                    String bone = renderer.pickWeaponBone(true);
                                    if (bone != null) {
                                        renderer.selectBone(bone);
                                    }
                                } catch (Throwable ignored) {
                                }
                            }
                            if (renderer.hasGeometry()) {
                                built.add(renderer);
                                limit++;
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                } catch (Throwable ignored) {
                }
                synchronized (previewCache) {
                    previewCache.put(key, built);
                    previewLoading.remove(key);
                }
            }
        }, "Harmony-FiguraLite-Preview");
        thread.setDaemon(true);
        thread.start();
        return null;
    }

    // -------------------------------------------------------------- Ввод

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (settingsEntry != null) {
            return settingsClicked(mouseX, mouseY, button);
        }

        float px = panelX();
        float py = panelY();

        // Поиск
        searchFocused = hovered(mouseX, mouseY, rightX(), py + 12f, 200f, 20f);

        // Вкладки
        float tx = px + 12f;
        float ty = py + 44f;
        FiguraAvatarLibrary.Section[] sections = FiguraAvatarLibrary.Section.values();
        for (int i = 0; i < sections.length; i++) {
            float w = Fonts.sfuy.getWidth(sections[i].title, 7f) + 16f;
            if (hovered(mouseX, mouseY, tx, ty, w, 20f)) {
                if (sections[i] != section) {
                    prevSection = section;
                    tabFade = 1f;
                }
                section = sections[i];
                lastSection = section;
                page = 0;
                pageFade = 0f;
                return true;
            }
            tx += w + 5f;
        }
        float favW = Fonts.sfuy.getWidth("Избранное", 7f) + 16f;
        if (hovered(mouseX, mouseY, tx, ty, favW, 20f)) {
            favoritesOnly = !favoritesOnly;
            page = 0;
            return true;
        }

        // Карточки
        List<FiguraAvatarLibrary.Entry> entries = visible();
        int from = page * perPage();
        int to = Math.min(entries.size(), from + perPage());
        float cw = cardW();
        for (int i = from; i < to; i++) {
            int index = i - from;
            float x = gridX() + (index % COLUMNS) * (cw + GAP);
            float y = gridY() + (index / COLUMNS) * (CARD_HEIGHT + GAP);
            if (!hovered(mouseX, mouseY, x, y, cw, CARD_HEIGHT)) {
                continue;
            }
            FiguraAvatarLibrary.Entry entry = entries.get(i);
            boolean starClicked = hovered(mouseX, mouseY, x + cw - 16f, y + 6f, 12f, 12f);
            if (starClicked) {
                FiguraAvatarLibrary.toggleFavorite(entry.folder);
            } else if (button == 1) {
                // ПКМ — настройки пака: виды и размещение.
                settingsEntry = entry;
                selected = entry.activeVariant().folder;
            } else if (entry.moduleCard) {
                // Карточка питомца: ЛКМ вызывает его или убирает, если уже активен.
                selected = entry.folder;
                setPetActive(entry, !isPetActive(entry));
            } else {
                FiguraAvatarLibrary.Entry active = entry.activeVariant();
                selected = active.folder;
                if (FiguraWear.isGroupWorn(entry)) {
                    FiguraWear.takeOff();
                } else {
                    FiguraWear.wear(active.folder);
                }
            }
            return true;
        }

        // Пагинация
        float pgY = py + panelH() - 26f;
        if (hovered(mouseX, mouseY, gridX(), pgY, 22f, 16f)) {
            gotoPage(Math.max(0, page - 1));
            return true;
        }
        if (hovered(mouseX, mouseY, gridX() + 26f, pgY, 22f, 16f)) {
            gotoPage(Math.min(pageCount(entries.size()) - 1, page + 1));
            return true;
        }

        // Кнопки справа
        float rx = rightX();
        float ry = py + 40f;
        float rh = py + panelH() - 12f - ry;
        float bw = (200f - 22f) / 2f;
        float by = ry + rh - 58f;
        if (hovered(mouseX, mouseY, rx + 8f, by, bw, 18f)) {
            if (selected != null) {
                FiguraAvatarLibrary.Entry chosen = FiguraAvatarLibrary.byFolder(selected);
                if (chosen != null && chosen.moduleCard) {
                    setPetActive(chosen, true);
                } else {
                    FiguraWear.wear(selected);
                }
            }
            return true;
        }
        if (hovered(mouseX, mouseY, rx + 14f + bw, by, bw, 18f)) {
            if (selected != null) {
                FiguraAvatarLibrary.Entry chosen = FiguraAvatarLibrary.byFolder(selected);
                if (chosen != null && chosen.moduleCard) {
                    setPetActive(chosen, false);
                    return true;
                }
            }
            FiguraWear.takeOff();
            selected = null;
            return true;
        }
        if (hovered(mouseX, mouseY, rx + 8f, by + 22f, bw, 18f)) {
            if (!FiguraAvatarInstaller.isRunning()) {
                FiguraAvatarInstaller.installAsync();
            }
            return true;
        }
        if (hovered(mouseX, mouseY, rx + 14f + bw, by + 22f, bw, 18f)) {
            reload();
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (hovered(mouseX, mouseY, gridX(), gridY(), gridW(), gridH())) {
            List<FiguraAvatarLibrary.Entry> entries = visible();
            if (amount < 0) {
                gotoPage(Math.min(pageCount(entries.size()) - 1, page + 1));
            } else if (amount > 0) {
                gotoPage(Math.max(0, page - 1));
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (searchFocused && codePoint >= ' ') {
            search += codePoint;
            page = 0;
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (settingsEntry != null) {
            if (keyCode == 256) {
                settingsEntry = null;
                return true;
            }
            return true;
        }
        if (searchFocused) {
            if (keyCode == 259) {
                if (!search.isEmpty()) {
                    search = search.substring(0, search.length() - 1);
                    page = 0;
                }
                return true;
            }
            if (keyCode == 257 || keyCode == 335) {
                searchFocused = false;
                return true;
            }
            if (keyCode == 256) {
                if (!search.isEmpty()) {
                    search = "";
                    page = 0;
                } else {
                    searchFocused = false;
                }
                return true;
            }
        }
        if (keyCode == 256) {
            closeScreen();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void reload() {
        FiguraAvatarLibrary.reload();
        FiguraAvatarPreviews.invalidate();
        disposePreviews();
        page = 0;
    }

    private void disposePreviews() {
        synchronized (previewCache) {
            for (List<BbModelRenderer> list : previewCache.values()) {
                if (list == null) {
                    continue;
                }
                for (int i = 0; i < list.size(); i++) {
                    try {
                        list.get(i).close();
                    } catch (Throwable ignored) {
                    }
                }
            }
            previewCache.clear();
        }
    }

    @Override
    public void closeScreen() {
        lastSection = section;
        disposePreviews();
        Minecraft.getInstance().displayGuiScreen(parent);
    }

    @Override
    public void onClose() {
        disposePreviews();
        super.onClose();
    }

    // ------------------------------------------- Настройки пака (ПКМ)

    /** Кнопка-таблетка внутри панели настроек. */
    private static final class Chip {
        float x;
        float y;
        float w;
        float h;
        String label;
        int kind;      // 0 = вид, 1 = пет, 2 = оружие, 3 = режим высоты, 4 = T-поза
        String value;
        boolean active;
    }

    /** Ползунок внутри панели настроек. */
    private static final class Slider {
        float x;
        float y;
        float w;
        float h;
        int kind;      // 0 = масштаб, 1 = поворот, 2 = высота
        float min;
        float max;
        float value;
        String suffix;
    }

    private static final int SLIDER_SCALE = 0;
    private static final int SLIDER_ROTATE = 1;
    private static final int SLIDER_HEIGHT = 2;
    /** Ползунки косметических функций. */
    private static final int SLIDER_FEATURE_HUE = 10;
    private static final int SLIDER_FEATURE_A = 11;
    private static final int SLIDER_FEATURE_B = 12;
    private static final int SLIDER_FEATURE_C = 13;
    /** Таблетки косметических функций. */
    private static final int CHIP_FEATURE_COLOR_MODE = 10;
    private static final int CHIP_FEATURE_BOOL = 11;

    private int draggingSlider = -1;

    private float settingsW() {
        return 340f;
    }

    private float settingsH() {
        FiguraAvatarLibrary.Entry entry = settingsEntry;
        if (entry == null) {
            return 220f;
        }
        float content = layout(entry, 0f, new ArrayList<String>(), new ArrayList<Float>(),
                new ArrayList<Chip>(), new ArrayList<Slider>());
        float h = 44f + content + 34f;
        if (h < 160f) {
            h = 160f;
        }
        if (h > this.height - 20f) {
            h = this.height - 20f;
        }
        return h;
    }

    private float settingsX() {
        return (this.width - settingsW()) / 2f;
    }

    private float settingsY() {
        return (this.height - settingsH()) / 2f;
    }

    /** Ряд кнопок-таблеток. Возвращает Y под рядом. */
    private float chipRow(List<Chip> out, int kind, String[] labels, String[] values, String active, float y) {
        float left = settingsX() + 14f;
        float right = settingsX() + settingsW() - 14f;
        float x = left;
        for (int i = 0; i < labels.length; i++) {
            Chip chip = new Chip();
            chip.label = labels[i];
            chip.kind = kind;
            chip.value = values[i];
            chip.active = values[i].equalsIgnoreCase(active);
            chip.w = Fonts.sfuy.getWidth(chip.label, 6.5f) + 16f;
            chip.h = 18f;
            if (x + chip.w > right) {
                x = left;
                y += 22f;
            }
            chip.x = x;
            chip.y = y;
            out.add(chip);
            x += chip.w + 6f;
        }
        return y + 22f;
    }

    private float sliderRow(List<Slider> out, int kind, float min, float max, float value, String suffix, float y) {
        Slider slider = new Slider();
        slider.x = settingsX() + 14f;
        slider.w = settingsW() - 28f;
        slider.y = y + 2f;
        slider.h = 7f;
        slider.kind = kind;
        slider.min = min;
        slider.max = max;
        slider.value = value;
        slider.suffix = suffix;
        out.add(slider);
        return y + 18f;
    }

    /**
     * Считает раскладку один в один для отрисовки и для кликов.
     * Возвращает высоту содержимого.
     */
    private float layout(FiguraAvatarLibrary.Entry entry, float originY, List<String> headers,
                         List<Float> headerY, List<Chip> chips, List<Slider> sliders) {
        if (entry == null) {
            return 0f;
        }
        float y = originY;
        String folder = entry.folder;

        if (entry.moduleCard) {
            boolean plainModule = entry.moduleName != null;
            headers.add(plainModule ? "Функция" : "Питомец");
            headerY.add(Float.valueOf(y));
            y += 15f;
            String[] labels = plainModule
                    ? new String[]{"Включить", "Выключить"}
                    : new String[]{"Вызвать", "Убрать"};
            y = chipRow(chips, 6, labels, new String[]{"true", "false"},
                    isPetActive(entry) ? "true" : "false", y) + 4f;

            if (plainModule) {
                y = featureLayout(entry.moduleName, y, headers, headerY, chips, sliders);
            }
            return y - originY;
        }

        if (entry.hasVariants()) {
            headers.add("Виды");
            headerY.add(Float.valueOf(y));
            y += 15f;
            String activeFolder = entry.activeVariant().folder;
            String[] labels = new String[entry.variants.size()];
            String[] values = new String[entry.variants.size()];
            for (int i = 0; i < entry.variants.size(); i++) {
                labels[i] = entry.variants.get(i).variantLabel;
                values[i] = entry.variants.get(i).folder;
            }
            y = chipRow(chips, 0, labels, values, activeFolder, y) + 6f;
        }

        // Настройки «Где сидит» больше нет: петы из раздела «Петы» всегда бегают у ног,
        // а вариант «на голове» — это отдельная карточка в «Аксессуарах».

        if (entry.section == FiguraAvatarLibrary.Section.WEAPONS) {
            headers.add("Где висит");
            headerY.add(Float.valueOf(y));
            y += 15f;
            FiguraPackSettings.WeaponPlacement[] all = FiguraPackSettings.WeaponPlacement.values();
            String[] labels = new String[all.length];
            String[] values = new String[all.length];
            for (int i = 0; i < all.length; i++) {
                labels[i] = all[i].title;
                values[i] = all[i].name();
            }
            y = chipRow(chips, 2, labels, values, FiguraPackSettings.getWeapon(folder).name(), y) + 6f;
        }

        // Высота: Авто (автомасштаб под рост игрока) или Менять (ручной ползунок).
        FiguraPackSettings.HeightMode heightMode = FiguraPackSettings.getHeightMode(folder);
        headers.add("Высота аватара");
        headerY.add(Float.valueOf(y));
        y += 15f;
        FiguraPackSettings.HeightMode[] modes = FiguraPackSettings.HeightMode.values();
        String[] modeLabels = new String[modes.length];
        String[] modeValues = new String[modes.length];
        for (int i = 0; i < modes.length; i++) {
            modeLabels[i] = modes[i] == FiguraPackSettings.HeightMode.AUTO
                    ? "Авто (под рост игрока)"
                    : "Менять";
            modeValues[i] = modes[i].name();
        }
        y = chipRow(chips, 3, modeLabels, modeValues, heightMode.name(), y) + 4f;

        if (heightMode == FiguraPackSettings.HeightMode.CUSTOM) {
            y = sliderRow(sliders, SLIDER_HEIGHT, FiguraPackSettings.HEIGHT_MIN, FiguraPackSettings.HEIGHT_MAX,
                    FiguraPackSettings.getHeight(folder), "", y) + 6f;
        } else {
            y += 2f;
        }

        headers.add("Масштаб аватара");
        headerY.add(Float.valueOf(y));
        y += 15f;
        y = sliderRow(sliders, SLIDER_SCALE, FiguraPackSettings.SCALE_MIN, FiguraPackSettings.SCALE_MAX,
                FiguraPackSettings.getScale(folder), "%", y) + 6f;

        headers.add("Поворот аватара");
        headerY.add(Float.valueOf(y));
        y += 15f;
        y = sliderRow(sliders, SLIDER_ROTATE, FiguraPackSettings.ROTATE_MIN, FiguraPackSettings.ROTATE_MAX,
                FiguraPackSettings.getRotate(folder), "°", y) + 6f;

        headers.add("Исправлять T-позу");
        headerY.add(Float.valueOf(y));
        y += 15f;
        y = chipRow(chips, 4, new String[]{"Вкл", "Выкл"}, new String[]{"true", "false"},
                FiguraPackSettings.getFixTPose(folder) ? "true" : "false", y) + 4f;

        return y - originY;
    }

    /**
     * Настройки косметической функции: цвет и всё, что раньше было
     * ползунками и галками у модуля.
     */
    private float featureLayout(String feature, float y, List<String> headers, List<Float> headerY,
                                List<Chip> chips, List<Slider> sliders) {
        headers.add("Цвет");
        headerY.add(Float.valueOf(y));
        y += 15f;
        y = chipRow(chips, CHIP_FEATURE_COLOR_MODE, new String[]{"Тема клиента", "Свой"},
                new String[]{"Тема", "Свой"},
                CosmeticFeatures.getMode(feature, "colorMode", "Тема"), y) + 4f;

        if (!CosmeticFeatures.usesThemeColor(feature)) {
            headers.add("Свой цвет");
            headerY.add(Float.valueOf(y));
            y += 15f;
            y = sliderRow(sliders, SLIDER_FEATURE_HUE, 0f, 360f, featureHue(feature), "°", y) + 6f;
        }

        if (CosmeticFeatures.KATANA.equals(feature)) {
            headers.add("Подсветка");
            headerY.add(Float.valueOf(y));
            y += 15f;
            y = chipRow(chips, CHIP_FEATURE_BOOL, new String[]{"Вкл", "Выкл"},
                    new String[]{"glow:true", "glow:false"},
                    CosmeticRenderer.katanaGlow() ? "glow:true" : "glow:false", y) + 4f;
            if (CosmeticRenderer.katanaGlow()) {
                headers.add("Уровень подсветки");
                headerY.add(Float.valueOf(y));
                y += 15f;
                y = sliderRow(sliders, SLIDER_FEATURE_A, 0f, 100f, CosmeticRenderer.katanaGlowLevel(), "%", y) + 6f;
            }
            headers.add("Прозрачность заливки");
            headerY.add(Float.valueOf(y));
            y += 15f;
            y = sliderRow(sliders, SLIDER_FEATURE_B, 0f, 100f, CosmeticRenderer.katanaFillAlpha(), "%", y) + 6f;
            headers.add("Прозрачность обводки");
            headerY.add(Float.valueOf(y));
            y += 15f;
            y = sliderRow(sliders, SLIDER_FEATURE_C, 0f, 100f, CosmeticRenderer.katanaOutlineAlpha(), "%", y) + 6f;
            return y;
        }

        if (CosmeticFeatures.CHINA_HAT.equals(feature)) {
            headers.add("Радиус");
            headerY.add(Float.valueOf(y));
            y += 15f;
            y = sliderRow(sliders, SLIDER_FEATURE_A, 2f, 12f, CosmeticRenderer.hatRadius(), "", y) + 6f;
            headers.add("Высота");
            headerY.add(Float.valueOf(y));
            y += 15f;
            y = sliderRow(sliders, SLIDER_FEATURE_B, 1f, 10f, CosmeticRenderer.hatHeight(), "", y) + 6f;
            headers.add("Сегменты");
            headerY.add(Float.valueOf(y));
            y += 15f;
            y = sliderRow(sliders, SLIDER_FEATURE_C, 12f, 96f, CosmeticRenderer.hatSegments(), "", y) + 6f;
            headers.add("Обводка");
            headerY.add(Float.valueOf(y));
            y += 15f;
            y = chipRow(chips, CHIP_FEATURE_BOOL, new String[]{"Вкл", "Выкл"},
                    new String[]{"outline:true", "outline:false"},
                    CosmeticRenderer.hatOutline() ? "outline:true" : "outline:false", y) + 4f;
            return y;
        }

        return y;
    }

    /** Оттенок своего цвета функции в градусах — им управляет ползунок. */
    private float featureHue(String feature) {
        int color = CosmeticFeatures.getColor(feature, "color", ColorUtils.rgb(255, 255, 255));
        float r = (color >> 16 & 255) / 255f;
        float g = (color >> 8 & 255) / 255f;
        float b = (color & 255) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float d = max - min;
        if (d <= 0.0001f) {
            return 0f;
        }
        float hue;
        if (max == r) {
            hue = ((g - b) / d) % 6f;
        } else if (max == g) {
            hue = (b - r) / d + 2f;
        } else {
            hue = (r - g) / d + 4f;
        }
        hue *= 60f;
        return hue < 0f ? hue + 360f : hue;
    }

    private String sliderText(Slider slider) {
        int value = Math.round(slider.value);
        return value + slider.suffix;
    }

    private void renderSettingsPanel(MatrixStack ms, int mouseX, int mouseY) {
        FiguraAvatarLibrary.Entry entry = settingsEntry;
        if (entry == null) {
            return;
        }

        RenderUtility.drawRoundedRect(0f, 0f, this.width, this.height, 0f, ColorUtils.rgba(0, 0, 0, 150));

        float x = settingsX();
        float y = settingsY();
        float w = settingsW();
        float h = settingsH();

        RenderUtility.drawRoundedRect(x, y, w, h, 9f, BG);
        RenderUtility.drawRoundedRect(x, y, w, 32f, 9f, PANEL);
        Fonts.sfuy.drawText(ms, trim("Настройки: " + entry.name, w - 28f, 8f), x + 14f, y + 12f, TEXT, 8f);

        List<String> headers = new ArrayList<String>();
        List<Float> headerY = new ArrayList<Float>();
        List<Chip> chips = new ArrayList<Chip>();
        List<Slider> sliders = new ArrayList<Slider>();
        layout(entry, y + 44f, headers, headerY, chips, sliders);

        for (int i = 0; i < headers.size(); i++) {
            Fonts.sfuy.drawText(ms, headers.get(i), x + 14f, headerY.get(i).floatValue(), TEXT_DIM, 6.5f);
        }

        for (int i = 0; i < chips.size(); i++) {
            Chip chip = chips.get(i);
            boolean hover = hovered(mouseX, mouseY, chip.x, chip.y, chip.w, chip.h);
            RenderUtility.drawRoundedRect(chip.x, chip.y, chip.w, chip.h, 5f,
                    chip.active ? accent() : (hover ? CARD_HOVER : CARD));
            Fonts.sfuy.drawCenteredText(ms, chip.label, chip.x + chip.w / 2f, chip.y + 6f,
                    chip.active ? ColorUtils.rgba(16, 18, 24, 255) : TEXT, 6.5f);
        }

        for (int i = 0; i < sliders.size(); i++) {
            Slider slider = sliders.get(i);
            float span = slider.max - slider.min;
            float t = span <= 0f ? 0f : (slider.value - slider.min) / span;
            if (t < 0f) t = 0f;
            if (t > 1f) t = 1f;

            RenderUtility.drawRoundedRect(slider.x, slider.y, slider.w, slider.h, 3.5f, CARD);
            RenderUtility.drawRoundedRect(slider.x, slider.y, slider.w * t, slider.h, 3.5f, accent());
            RenderUtility.drawRoundedRect(slider.x + slider.w * t - 3f, slider.y - 2f, 6f, slider.h + 4f, 3f, TEXT);

            String value = sliderText(slider);
            float tw = Fonts.sfuy.getWidth(value, 6.5f);
            Fonts.sfuy.drawText(ms, value, slider.x + slider.w - tw, slider.y - 14f, GOLD, 6.5f);
        }

        float bw = (w - 34f) / 2f;
        float by = y + h - 26f;
        String mainLabel;
        if (entry.moduleCard) {
            boolean on = isPetActive(entry);
            mainLabel = entry.moduleName != null
                    ? (on ? "Выключить" : "Включить")
                    : (on ? "Убрать" : "Вызвать");
        } else {
            mainLabel = FiguraWear.isGroupWorn(entry) ? "Снять" : "Надеть";
        }
        drawButton(ms, mainLabel, x + 14f, by, bw, 18f, mouseX, mouseY, accent(), true);
        drawButton(ms, "Закрыть", x + 20f + bw, by, bw, 18f, mouseX, mouseY, CARD, false);
    }

    private boolean settingsClicked(double mouseX, double mouseY, int button) {
        FiguraAvatarLibrary.Entry entry = settingsEntry;
        if (entry == null) {
            return false;
        }

        float x = settingsX();
        float y = settingsY();
        float w = settingsW();
        float h = settingsH();

        if (!hovered(mouseX, mouseY, x, y, w, h)) {
            settingsEntry = null;
            draggingSlider = -1;
            return true;
        }

        List<String> headers = new ArrayList<String>();
        List<Float> headerY = new ArrayList<Float>();
        List<Chip> chips = new ArrayList<Chip>();
        List<Slider> sliders = new ArrayList<Slider>();
        layout(entry, y + 44f, headers, headerY, chips, sliders);

        for (int i = 0; i < sliders.size(); i++) {
            Slider slider = sliders.get(i);
            if (!hovered(mouseX, mouseY, slider.x - 4f, slider.y - 6f, slider.w + 8f, slider.h + 12f)) {
                continue;
            }
            draggingSlider = slider.kind;
            applySliderValue(entry, slider, mouseX);
            return true;
        }

        for (int i = 0; i < chips.size(); i++) {
            Chip chip = chips.get(i);
            if (!hovered(mouseX, mouseY, chip.x, chip.y, chip.w, chip.h)) {
                continue;
            }
            if (chip.kind == 0) {
                FiguraPackSettings.setVariant(entry.folder, chip.value);
                selected = chip.value;
                if (FiguraWear.isGroupWorn(entry)) {
                    FiguraWear.wear(chip.value);
                }
            } else if (chip.kind == 1) {
                try {
                    FiguraPackSettings.setPet(entry.folder, FiguraPackSettings.PetPlacement.valueOf(chip.value));
                } catch (Exception ignored) {
                }
            } else if (chip.kind == 2) {
                try {
                    FiguraPackSettings.setWeapon(entry.folder, FiguraPackSettings.WeaponPlacement.valueOf(chip.value));
                } catch (Exception ignored) {
                }
            } else if (chip.kind == 3) {
                try {
                    FiguraPackSettings.setHeightMode(entry.folder, FiguraPackSettings.HeightMode.valueOf(chip.value));
                } catch (Exception ignored) {
                }
            } else if (chip.kind == 6) {
                setPetActive(entry, chip.value.equalsIgnoreCase("true"));
            } else if (chip.kind == CHIP_FEATURE_COLOR_MODE) {
                if (entry.moduleName != null) {
                    CosmeticFeatures.setMode(entry.moduleName, "colorMode", chip.value);
                }
            } else if (chip.kind == CHIP_FEATURE_BOOL) {
                if (entry.moduleName != null) {
                    int sep = chip.value.indexOf(':');
                    if (sep > 0) {
                        CosmeticFeatures.setBool(entry.moduleName, chip.value.substring(0, sep),
                                chip.value.substring(sep + 1).equalsIgnoreCase("true"));
                    }
                }
            } else if (chip.kind == 4) {
                boolean value = chip.value.equalsIgnoreCase("true");
                FiguraPackSettings.setFixTPose(entry.folder, value);
                // T-поза считается при сборке модели — пересобираем то, что видно.
                disposePreviews();
                FiguraAvatarLibrary.Entry active = entry.activeVariant();
                if (FiguraWear.isGroupWorn(entry)) {
                    FiguraWear.takeOff();
                    FiguraWear.wear(active.folder);
                }
            }
            return true;
        }

        float bw = (w - 34f) / 2f;
        float by = y + h - 26f;
        if (hovered(mouseX, mouseY, x + 14f, by, bw, 18f)) {
            if (entry.moduleCard) {
                setPetActive(entry, !isPetActive(entry));
                return true;
            }
            if (FiguraWear.isGroupWorn(entry)) {
                FiguraWear.takeOff();
            } else {
                FiguraAvatarLibrary.Entry active = entry.activeVariant();
                selected = active.folder;
                FiguraWear.wear(active.folder);
            }
            return true;
        }
        if (hovered(mouseX, mouseY, x + 20f + bw, by, bw, 18f)) {
            settingsEntry = null;
            draggingSlider = -1;
            return true;
        }
        return true;
    }

    private void applySliderValue(FiguraAvatarLibrary.Entry entry, Slider slider, double mouseX) {
        float t = (float) ((mouseX - slider.x) / slider.w);
        if (t < 0f) t = 0f;
        if (t > 1f) t = 1f;
        float value = slider.min + (slider.max - slider.min) * t;
        if (slider.kind == SLIDER_SCALE) {
            FiguraPackSettings.setScale(entry.folder, value);
        } else if (slider.kind == SLIDER_ROTATE) {
            FiguraPackSettings.setRotate(entry.folder, value);
        } else if (slider.kind == SLIDER_HEIGHT) {
            FiguraPackSettings.setHeight(entry.folder, value);
        } else if (entry.moduleName != null) {
            applyFeatureSlider(entry.moduleName, slider.kind, value);
        }
    }

    /** Запись значения ползунка косметической функции. */
    private void applyFeatureSlider(String feature, int kind, float value) {
        if (kind == SLIDER_FEATURE_HUE) {
            CosmeticFeatures.setColor(feature, "color", hsb(value / 360f, 0.75f, 1f) & 0x00FFFFFF);
            return;
        }
        if (CosmeticFeatures.KATANA.equals(feature)) {
            if (kind == SLIDER_FEATURE_A) {
                CosmeticFeatures.setFloat(feature, "glowLevel", value, 0f, 100f);
            } else if (kind == SLIDER_FEATURE_B) {
                CosmeticFeatures.setFloat(feature, "fillAlpha", value, 0f, 100f);
            } else if (kind == SLIDER_FEATURE_C) {
                CosmeticFeatures.setFloat(feature, "outlineAlpha", value, 0f, 100f);
            }
            return;
        }
        if (CosmeticFeatures.CHINA_HAT.equals(feature)) {
            if (kind == SLIDER_FEATURE_A) {
                CosmeticFeatures.setFloat(feature, "radius", value, 2f, 12f);
            } else if (kind == SLIDER_FEATURE_B) {
                CosmeticFeatures.setFloat(feature, "height", value, 1f, 10f);
            } else if (kind == SLIDER_FEATURE_C) {
                CosmeticFeatures.setFloat(feature, "segments", Math.round(value), 12f, 96f);
            }
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        FiguraAvatarLibrary.Entry entry = settingsEntry;
        if (entry != null && draggingSlider >= 0) {
            List<Slider> sliders = new ArrayList<Slider>();
            layout(entry, settingsY() + 44f, new ArrayList<String>(), new ArrayList<Float>(),
                    new ArrayList<Chip>(), sliders);
            for (int i = 0; i < sliders.size(); i++) {
                if (sliders.get(i).kind == draggingSlider) {
                    applySliderValue(entry, sliders.get(i), mouseX);
                    break;
                }
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingSlider = -1;
        return super.mouseReleased(mouseX, mouseY, button);
    }


    /** Отрисовка текстуры без зависимости от RenderUtility.drawImage. */
    private void drawTexture(MatrixStack ms, ResourceLocation tex, float x, float y, float w, float h) {
        if (tex == null) {
            return;
        }
        Minecraft.getInstance().getTextureManager().bindTexture(tex);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableTexture();
        RenderSystem.color4f(1f, 1f, 1f, 1f);

        Matrix4f matrix = ms.getLast().getMatrix();
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(matrix, x, y + h, 0f).tex(0f, 1f).endVertex();
        buffer.pos(matrix, x + w, y + h, 0f).tex(1f, 1f).endVertex();
        buffer.pos(matrix, x + w, y, 0f).tex(1f, 0f).endVertex();
        buffer.pos(matrix, x, y, 0f).tex(0f, 0f).endVertex();
        tessellator.draw();

        RenderSystem.disableBlend();
    }
}
