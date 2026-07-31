package xd.harm.modules.impl.player;

import com.google.common.eventbus.Subscribe;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.util.text.ITextComponent;
import xd.harm.events.render.EventDisplay;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.impl.render.Theme;
import xd.harm.utils.render.KawaseBlur;
import xd.harm.utils.render.color.ColorUtils;
import xd.harm.utils.render.font.Fonts;
import xd.harm.utils.render.rect.RenderUtility;
import xd.harm.utils.text.font.ClientFonts;
import org.lwjgl.opengl.GL11;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ModuleRegister(name = "MineViewer", category = Category.Player, desc = "\u041f\u043e\u043a\u0430\u0437\u044b\u0432\u0430\u0435\u0442 \u0438\u043d\u0444\u043e\u0440\u043c\u0430\u0446\u0438\u044e \u043e \u0448\u0430\u0445\u0442\u0435")
public class MineViewer extends Module {
    private static final String AUTO_MINE_MARKER = "\u0430\u0432\u0442\u043e";
    private static final String AUTO_MINE_EN_MARKER = "auto";
    private static final String AUTO_MINE_TITLE = "\u0410\u0432\u0442\u043e \u0448\u0430\u0445\u0442\u0430";
    private static final String NEXT_LABEL = "\u0421\u043b\u0435\u0434\u0443\u044e\u0449\u0430\u044f:";
    private static final String UNKNOWN_TEXT = "\u041d\u0435\u0438\u0437\u0432\u0435\u0441\u0442\u043d\u043e";
    private static final Pattern TIME_FORMAT_PATTERN = Pattern.compile("^\\d{1,2}:\\d{2}(:\\d{2})?.*$");
    private static final Pattern TIME_EXTRACT_PATTERN = Pattern.compile("\\d{1,2}:\\d{2}(:\\d{2})?");
    private static final Pattern COMMON_RARITY_PATTERN = Pattern.compile("(?<![\\p{L}])(?:\\u043e\\u0431\\u044b\\u0447\\u043d\\u0430\\u044f|\\u043e\\u0431\\u044b\\u0447\\u043d\\u044b\\u0439|\\u043e\\u0431\\u044b\\u0447\\u043d\\u043e\\u0435)(?![\\p{L}])");
    private static final Pattern RARE_RARITY_PATTERN = Pattern.compile("(?<![\\p{L}])(?:\\u0440\\u0435\\u0434\\u043a\\u0430\\u044f|\\u0440\\u0435\\u0434\\u043a\\u0438\\u0439|\\u0440\\u0435\\u0434\\u043a\\u043e\\u0435)(?![\\p{L}])");
    private static final Pattern EPIC_RARITY_PATTERN = Pattern.compile("(?<![\\p{L}])(?:\\u044d\\u043f\\u0438\\u0447\\u0435\\u0441\\u043a\\u0430\\u044f|\\u044d\\u043f\\u0438\\u0447\\u0435\\u0441\\u043a\\u0438\\u0439|\\u044d\\u043f\\u0438\\u0447\\u0435\\u0441\\u043a\\u043e\\u0435|\\u0443\\u043d\\u0438\\u043a\\u0430\\u043b\\u044c\\u043d\\u0430\\u044f|\\u0443\\u043d\\u0438\\u043a\\u0430\\u043b\\u044c\\u043d\\u044b\\u0439|\\u0443\\u043d\\u0438\\u043a\\u0430\\u043b\\u044c\\u043d\\u043e\\u0435)(?![\\p{L}])");
    private static final Pattern MYTHIC_RARITY_PATTERN = Pattern.compile("(?<![\\p{L}])(?:\\u043c\\u0438\\u0444\\u0438\\u0447\\u0435\\u0441\\u043a\\u0430\\u044f|\\u043c\\u0438\\u0444\\u0438\\u0447\\u0435\\u0441\\u043a\\u0438\\u0439|\\u043c\\u0438\\u0444\\u0438\\u0447\\u0435\\u0441\\u043a\\u043e\\u0435)(?![\\p{L}])");
    private static final Pattern LEGENDARY_RARITY_PATTERN = Pattern.compile("(?<![\\p{L}])(?:\\u043b\\u0435\\u0433\\u0435\\u043d\\u0434\\u0430\\u0440\\u043d\\u0430\\u044f|\\u043b\\u0435\\u0433\\u0435\\u043d\\u0434\\u0430\\u0440\\u043d\\u044b\\u0439|\\u043b\\u0435\\u0433\\u0435\\u043d\\u0434\\u0430\\u0440\\u043d\\u043e\\u0435|\\u0431\\u043e\\u0436\\u0435\\u0441\\u0442\\u0432\\u0435\\u043d\\u043d\\u0430\\u044f|\\u0431\\u043e\\u0436\\u0435\\u0441\\u0442\\u0432\\u0435\\u043d\\u043d\\u044b\\u0439|\\u0431\\u043e\\u0436\\u0435\\u0441\\u0442\\u0432\\u0435\\u043d\\u043d\\u043e\\u0435)(?![\\p{L}])");
    private static final double AUTO_MINE_SCAN_DISTANCE = 16.0D * 16.0D;

    private final Minecraft mc = Minecraft.getInstance();
    private final List<Entity> cachedArmorStands = new CopyOnWriteArrayList<>();

    private String lastFoundText = "";
    private String lastFoundLevelText = "";
    private String displayTimeText = "";
    private String displayLevelText = "";

    private float animationProgress = 0.0f;
    private float pulseProgress = 0.0f;
    private float dataSwapProgress = 1.0f;
    private boolean hasData = false;
    private long lastUpdateTime = System.currentTimeMillis();

    @Subscribe
    public void onUpdate(EventUpdate e) {
        if (!shouldRenderOverlay()) {
            cachedArmorStands.clear();
            return;
        }

        cachedArmorStands.clear();
        for (Entity entity : mc.world.getAllEntities()) {
            if (entity instanceof ArmorStandEntity && entity.hasCustomName()) {
                String name = normalizeStandText(entity.getCustomName().getString());
                if (isAutoMineStand(name) || isValidTimeText(name) || isValidLevelText(name)) {
                    cachedArmorStands.add(entity);
                }
            }
        }
    }

    @Subscribe
    public void onEvent(EventDisplay e) {
        if (e.getMatrixStack() == null || !shouldRenderOverlay()) {
            hasData = false;
            animationProgress = 0.0f;
            return;
        }

        String timeText = "";
        String levelText = "";
        boolean foundData = false;

        if (mc.world != null && mc.player != null) {
            Entity[] entities = findNearestEntities();
            if (entities[0] != null && entities[0].getCustomName() != null) {
                timeText = entities[0].getCustomName().getString();
                foundData = true;
            }
            if (entities[1] != null && entities[1].getCustomName() != null) {
                levelText = entities[1].getCustomName().getString();
                foundData = true;
            }
        }

        boolean newHasData = foundData && (!timeText.isEmpty() || !levelText.isEmpty());

        if (newHasData) {
            if (!timeText.equals(displayTimeText) || !levelText.equals(displayLevelText)) {
                dataSwapProgress = 0.0f;
            }
            displayTimeText = timeText;
            displayLevelText = levelText;
        }

        hasData = newHasData;
        updateAnimation();

        if (animationProgress > 0.0f) {
            drawAutoMineInfo(e, displayTimeText, displayLevelText);
        }

        lastFoundText = timeText;
        lastFoundLevelText = levelText;
    }

    private boolean shouldRenderOverlay() {
        return mc.world != null
                && mc.player != null
                && mc.getMainWindow() != null
                && mc.currentScreen == null;
    }

    private void updateAnimation() {
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastUpdateTime) / 1000.0f;
        lastUpdateTime = currentTime;

        float animationSpeed = hasData ? 3.0f : 1.5f;
        pulseProgress = (pulseProgress + deltaTime) % 1000.0f;
        dataSwapProgress = Math.min(1.0f, dataSwapProgress + deltaTime * 5.5f);

        if (hasData) {
            animationProgress = Math.min(1.0f, animationProgress + deltaTime * animationSpeed);
        } else {
            animationProgress = Math.max(0.0f, animationProgress - deltaTime * animationSpeed);
        }
    }

    private float easeOutCubic(float t) {
        return 1.0f - (float) Math.pow(1.0f - t, 3);
    }

    private Entity[] findNearestEntities() {
        Entity anchorEntity = null;
        Entity nearestTimeEntity = null;
        Entity nearestLevelEntity = null;
        double minAnchorDistance = Double.MAX_VALUE;
        double minTimeDistance = Double.MAX_VALUE;
        double minLevelDistance = Double.MAX_VALUE;

        for (Entity entity : cachedArmorStands) {
            ITextComponent customNameComponent = entity.getCustomName();
            if (customNameComponent == null) {
                continue;
            }

            String customName = normalizeStandText(customNameComponent.getString());
            double distance = entity.getDistanceSq(mc.player.getPosX(), mc.player.getPosY(), mc.player.getPosZ());

            if (isAutoMineStand(customName) && distance < minAnchorDistance) {
                minAnchorDistance = distance;
                anchorEntity = entity;
            }
        }

        for (Entity entity : cachedArmorStands) {
            ITextComponent customNameComponent = entity.getCustomName();
            if (customNameComponent == null) {
                continue;
            }

            String customName = normalizeStandText(customNameComponent.getString());
            double distance = anchorEntity != null
                    ? entity.getDistanceSq(anchorEntity.getPosX(), anchorEntity.getPosY(), anchorEntity.getPosZ())
                    : entity.getDistanceSq(mc.player.getPosX(), mc.player.getPosY(), mc.player.getPosZ());

            if (anchorEntity != null && distance > AUTO_MINE_SCAN_DISTANCE) {
                continue;
            }

            if (isAutoMineStand(customName)) {
                String inlineTime = extractTimeText(customName);
                if (!inlineTime.isEmpty() && distance < minTimeDistance) {
                    minTimeDistance = distance;
                    nearestTimeEntity = entity;
                }
                if (isValidLevelText(customName) && distance < minLevelDistance) {
                    minLevelDistance = distance;
                    nearestLevelEntity = entity;
                }
            }

            if (isValidTimeText(customName) && distance < minTimeDistance) {
                minTimeDistance = distance;
                nearestTimeEntity = entity;
            } else if (isValidLevelText(customName) && distance < minLevelDistance) {
                minLevelDistance = distance;
                nearestLevelEntity = entity;
            }
        }

        return new Entity[]{nearestTimeEntity, nearestLevelEntity};
    }

    private boolean isAutoMineStand(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        String normalized = normalizeStandText(text);
        return normalized.contains(AUTO_MINE_MARKER) || normalized.contains(AUTO_MINE_EN_MARKER);
    }

    private boolean isValidTimeText(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return TIME_FORMAT_PATTERN.matcher(text).matches() || !extractTimeText(text).isEmpty();
    }

    private boolean isValidLevelText(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        String normalized = normalizeStandText(text);
        return COMMON_RARITY_PATTERN.matcher(normalized).find()
                || RARE_RARITY_PATTERN.matcher(normalized).find()
                || EPIC_RARITY_PATTERN.matcher(normalized).find()
                || MYTHIC_RARITY_PATTERN.matcher(normalized).find()
                || LEGENDARY_RARITY_PATTERN.matcher(normalized).find();
    }

    private void drawAutoMineInfo(EventDisplay e, String timeText, String levelText) {
        float easedProgress = easeOutCubic(animationProgress);
        float pulse = (float) (Math.sin(pulseProgress * 4.2f) * 0.5f + 0.5f);

        String displayTime = cleanTimeText(timeText);
        String rarityText = cleanRarityText(levelText);
        RarityStyle rarity = getRarityStyle(rarityText);

        float height = 22.0f;
        float radius = 4.0f;
        float padding = 4.0f;
        float iconBox = 12.0f;
        float timeSize = 5.7f;
        float raritySize = 5.4f;
        String safeTime = displayTime.isEmpty() ? "--:--" : displayTime;

        float timeWidth = Fonts.sfbold.getWidth(safeTime, timeSize);
        float rarityWidth = Fonts.sfbold.getWidth(rarityText, raritySize);
        float textWidth = Math.max(timeWidth, rarityWidth);
        float cardW = Math.max(48.0f, padding + iconBox + 4.0f + textWidth + padding);
        float cardH = height;
        float cardX = snapHalf(mc.getMainWindow().getScaledWidth() / 2.0f - cardW / 2.0f);
        float cardY = snapHalf(24.0f - (1.0f - easedProgress) * 10.0f);
        cardW = snapHalf(cardW);
        cardH = snapHalf(cardH);

        int alpha = (int) (255 * easedProgress);
        int textAlpha = alpha;
        int bgAlpha = (int) (166 * easedProgress);
        int softAlpha = (int) (50 * easedProgress);

        e.getMatrixStack().push();

        if (cardW > 2.0f && cardH > 2.0f) {
            KawaseBlur.blur.updateBlur(3.0f, 3);
            final float blurX = cardX;
            final float blurY = cardY;
            final float blurW = cardW;
            final float blurH = cardH;
            final float blurRadius = radius;
            final int blurAlpha = alpha;
            KawaseBlur.blur.render(() -> {
                GL11.glEnable(GL11.GL_ALPHA_TEST);
                GL11.glAlphaFunc(GL11.GL_GREATER, 0.01f);
                RenderUtility.drawRoundedRect(blurX, blurY, blurW, blurH, blurRadius,
                        ColorUtils.rgba(255, 255, 255, blurAlpha));
                GL11.glDisable(GL11.GL_ALPHA_TEST);
            });
        }

        RenderUtility.drawRoundedRect(cardX, cardY, cardW, cardH, radius,
                ColorUtils.rgba(25, 25, 30, bgAlpha));
        ClientFonts.icons_client[35].drawString(e.getMatrixStack(), "B", cardX + padding - 2.4f, cardY + 6.6f,
                ColorUtils.rgba(255, 255, 255, alpha));

        float textX = cardX + padding + iconBox + 4.0f;
        drawSoftText(e, safeTime, textX, cardY + 4.0f, ColorUtils.rgba(224, 230, 244, textAlpha), timeSize, true);
        drawRarityText(e, rarityText, textX, cardY + 13.0f, rarity, textAlpha, pulse, raritySize);

        RenderUtility.drawRoundedRectOutline(cardX, cardY, cardW, cardH, radius, 0.35f,
                ColorUtils.rgba(255, 255, 255, (int) (18 * easedProgress)));
        RenderUtility.drawRoundedRectOutline(cardX + 0.5f, cardY + 0.5f, cardW - 1.0f, cardH - 1.0f, radius - 0.5f, 0.25f,
                ColorUtils.setAlpha(Theme.MainColor(0), (int) (14 * easedProgress)));

        e.getMatrixStack().pop();
    }

    private float snapHalf(float value) {
        return Math.round(value * 2.0f) / 2.0f;
    }

    private String normalizeStandText(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("\u00A7[0-9A-FK-ORa-fk-or]", "")
                .replace('\u0451', '\u0435')
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private String extractTimeText(String text) {
        Matcher matcher = TIME_EXTRACT_PATTERN.matcher(text == null ? "" : text);
        return matcher.find() ? matcher.group() : "";
    }

    private String cleanTimeText(String text) {
        return extractTimeText(normalizeStandText(text));
    }

    private String cleanRarityText(String text) {
        String cleaned = text == null ? "" : text.replaceAll("\u00A7[0-9A-FK-ORa-fk-or]", "").trim();
        cleaned = cleaned
                .replaceAll("(?iu)\u0430\u0432\u0442\u043e\\s*-?\\s*\u0448\u0430\u0445\u0442[\u0430-\u044f]*", "")
                .replaceAll("(?iu)\u0430\u0432\u0442\u043e", "")
                .replaceAll("(?iu)\\bauto\\b", "")
                .replace(AUTO_MINE_TITLE, "")
                .replace(NEXT_LABEL, "")
                .replace(":", "")
                .trim();
        return cleaned.isEmpty() ? UNKNOWN_TEXT : cleaned;
    }

    private RarityStyle getRarityStyle(String text) {
        String lower = normalizeStandText(text);
        if (LEGENDARY_RARITY_PATTERN.matcher(lower).find()) {
            return new RarityStyle(ColorUtils.rgba(255, 182, 72, 255), ColorUtils.rgba(255, 226, 126, 255));
        }
        if (MYTHIC_RARITY_PATTERN.matcher(lower).find()) {
            return new RarityStyle(ColorUtils.rgba(205, 92, 255, 255), ColorUtils.rgba(255, 128, 232, 255));
        }
        if (EPIC_RARITY_PATTERN.matcher(lower).find()) {
            return new RarityStyle(ColorUtils.rgba(120, 108, 255, 255), ColorUtils.rgba(94, 205, 255, 255));
        }
        if (RARE_RARITY_PATTERN.matcher(lower).find()) {
            return new RarityStyle(ColorUtils.rgba(74, 160, 255, 255), ColorUtils.rgba(91, 232, 255, 255));
        }
        if (COMMON_RARITY_PATTERN.matcher(lower).find()) {
            return new RarityStyle(ColorUtils.rgba(132, 215, 144, 255), ColorUtils.rgba(192, 255, 172, 255));
        }
        return new RarityStyle(ColorUtils.rgba(180, 188, 204, 255), ColorUtils.rgba(235, 240, 255, 255));
    }

    private void drawSoftText(EventDisplay e, String text, float x, float y, int color, float size, boolean bold) {
        int shadowColor = ColorUtils.rgba(0, 0, 0, (int) (((color >> 24) & 255) * 0.42f));
        if (bold) {
            Fonts.sfbold.drawText(e.getMatrixStack(), text, x + 0.6f, y + 0.8f, shadowColor, size);
            Fonts.sfbold.drawText(e.getMatrixStack(), text, x, y, color, size);
        } else {
            Fonts.sfuy.drawText(e.getMatrixStack(), text, x + 0.5f, y + 0.7f, shadowColor, size);
            Fonts.sfuy.drawText(e.getMatrixStack(), text, x, y, color, size);
        }
    }

    private void drawRarityText(EventDisplay e, String text, float x, float y, RarityStyle rarity, int alpha, float pulse, float size) {
        float cursorX = x;
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            float wave = (float) (Math.sin(pulseProgress * 5.5f + i * 0.52f) * 0.5f + 0.5f);
            int color = ColorUtils.interpolateColor(rarity.mainColor, rarity.lightColor, Math.min(1.0f, wave * 0.85f + pulse * 0.15f));
            color = ColorUtils.setAlpha(color, alpha);
            Fonts.sfbold.drawText(e.getMatrixStack(), ch, cursorX + 0.5f, y + 0.8f, ColorUtils.rgba(0, 0, 0, (int) (alpha * 0.38f)), size);
            Fonts.sfbold.drawText(e.getMatrixStack(), ch, cursorX, y, color, size);
            cursorX += Fonts.sfbold.getWidth(ch, size);
        }
    }

    private static class RarityStyle {
        final int mainColor;
        final int lightColor;

        RarityStyle(int mainColor, int lightColor) {
            this.mainColor = mainColor;
            this.lightColor = lightColor;
        }
    }
}
