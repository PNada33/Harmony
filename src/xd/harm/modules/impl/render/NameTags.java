package xd.harm.modules.impl.render;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.Texture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.settings.PointOfView;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreCriteria;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.ITextProperties;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import org.joml.Vector2d;
import org.lwjgl.opengl.ARBShaderObjects;
import xd.harm.config.FriendStorage;
import xd.harm.events.render.EventDisplay;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.impl.misc.StreamerMode;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.ModeListSetting;
import xd.harm.utils.projections.ProjectionUtil;
import xd.harm.utils.render.font.Fonts;
import xd.harm.utils.render.rect.RenderUtility;
import xd.harm.utils.shader.ShaderUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ModuleRegister(name = "NameTags", category = Category.Render, desc = "NameTags")
public class NameTags extends Module {
    private static final float HEAD_SIZE = 8.0F;
    private static final float TAG_TEXT_SIZE = 7.0F;
    private static final float COLORED_ITEM_TEXT_SIZE = TAG_TEXT_SIZE + 2.0F;
    private static final float DROPPED_ITEM_ICON_SCALE = 0.5F;
    private static final float COLORED_DROPPED_ITEM_ICON_SCALE = 0.6F;
    private static final float DROPPED_ITEM_ICON_PADDING = 2.5F;
    private static final float ARMOR_ITEM_SCALE = 0.6F;
    private static final float ARMOR_ITEM_STEP = 11.0F;
    private static final float ARMOR_ITEM_Y_OFFSET = 32.0F;
    private static final float ENCHANT_TEXT_SIZE = 4.0F;
    private static final float TAG_SCALE = 0.9F;
    private static final long REMEMBERED_ITEM_LIFETIME_MS = 10L * 60L * 1000L;
    private static final int MAX_REMEMBERED_ITEMS = 4;
    private static final float REMEMBERED_ITEM_SCALE = 0.65F;
    private static final float REMEMBERED_ITEM_TEXT_SIZE = 4.5F;
    private static final char FORMAT_PREFIX = '\u00A7';
    private static final int BACKGROUND_COLOR = 0x64000000;
    private static final int FRIEND_BACKGROUND_COLOR = 0x64009700;
    private static final int BRACKET_COLOR = 0xFFCAC9C9;
    private static final int FRIEND_COLOR = 0xFF07EE00;
    private static final int HEALTH_LOW_COLOR = 0xFFFF4747;
    private static final int HEALTH_MEDIUM_COLOR = 0xFFEEE300;
    private static final int HEALTH_HIGH_COLOR = 0xFF07EE00;
    private static final Style BRACKET_STYLE = styleFromColor(BRACKET_COLOR);
    private static final Style FRIEND_STYLE = styleFromColor(FRIEND_COLOR);
    private static final Style HEALTH_LOW_STYLE = styleFromColor(HEALTH_LOW_COLOR);
    private static final Style HEALTH_MEDIUM_STYLE = styleFromColor(HEALTH_MEDIUM_COLOR);
    private static final Style HEALTH_HIGH_STYLE = styleFromColor(HEALTH_HIGH_COLOR);
    private static final Comparator<RememberedItem> REMEMBERED_ITEM_RECENCY =
            (first, second) -> Long.compare(second.lastSeen, first.lastSeen);
    private static final ShaderUtil HEAD_SHADER = new ShaderUtil("round-head");
    private static final int HEAD_TEXTURE_UNIFORM = HEAD_SHADER.getUniform("texture");
    private static final int HEAD_SIZE_UNIFORM = HEAD_SHADER.getUniform("size");
    private static final int HEAD_RADIUS_UNIFORM = HEAD_SHADER.getUniform("radius");
    private static final int HEAD_HURT_TIME_UNIFORM = HEAD_SHADER.getUniform("hurt_time");
    private static final int HEAD_ALPHA_UNIFORM = HEAD_SHADER.getUniform("alpha");
    private static final int HEAD_START_X_UNIFORM = HEAD_SHADER.getUniform("startX");
    private static final int HEAD_END_X_UNIFORM = HEAD_SHADER.getUniform("endX");
    private static final int HEAD_START_Y_UNIFORM = HEAD_SHADER.getUniform("startY");
    private static final int HEAD_END_Y_UNIFORM = HEAD_SHADER.getUniform("endY");
    private static final int HEAD_TEX_X_SIZE_UNIFORM = HEAD_SHADER.getUniform("texXSize");
    private static final int HEAD_TEX_Y_SIZE_UNIFORM = HEAD_SHADER.getUniform("texYSize");
    private final Map<UUID, List<RememberedItem>> rememberedItems = new HashMap<>();
    private final StringBuilder formattedTextPart = new StringBuilder(64);
    private final StringBuilder strippedText = new StringBuilder(64);
    private MatrixStack activeTextStack;
    private float activeTextX;
    private float activeTextY;
    private float activeTextSize;
    private float activeTextOffset;
    private int activeTextFallbackColor;
    private float activeMeasureSize;
    private float activeMeasureWidth;
    private boolean activeColoredText;
    private final ITextProperties.IStyledTextAcceptor<Object> styledTextDrawer = (style, text) -> {
        int color = style.getColor() == null ? activeTextFallbackColor : style.getColor().getColor();
        activeTextOffset += drawFormattedString(activeTextStack, text,
                activeTextX + activeTextOffset, activeTextY, activeTextSize, color);
        return Optional.empty();
    };
    private final ITextProperties.IStyledTextAcceptor<Object> styledTextMeasurer = (style, text) -> {
        activeMeasureWidth += getFormattedStringWidth(text, activeMeasureSize);
        return Optional.empty();
    };
    private final ITextProperties.IStyledTextAcceptor<Object> styledColorDetector = (style, text) -> {
        if (style.getColor() != null || hasFormattingColor(text)) {
            activeColoredText = true;
        }
        return Optional.empty();
    };

    public static BooleanSetting showEnchantments = new BooleanSetting("Зачарования", false);
    public static BooleanSetting friendsOutline = new BooleanSetting("Обводка друзей", true);
    public static BooleanSetting healthBypass = new BooleanSetting("Фикс здоровья", false);
    public static ModeListSetting show = new ModeListSetting("Отображать",
            new BooleanSetting("Игроки", true),
            new BooleanSetting("Друзья", true),
            new BooleanSetting("Себя", true),
            new BooleanSetting("Голые", false));

    public static BooleanSetting showItems = new BooleanSetting("Предметы", true);

    public static BooleanSetting rememberBonusItems = new BooleanSetting("\u0417\u0430\u043f\u043e\u043c\u0438\u043d\u0430\u0442\u044c \u0422\u0430\u043b\u0438\u0441\u043c\u0430\u043d\u044b/\u0421\u0444\u0435\u0440\u044b", true);

    public NameTags() {
        addSettings(friendsOutline, healthBypass, showEnchantments, show, showItems, rememberBonusItems);
    }

    @Override
    public boolean onDisable() {
        rememberedItems.clear();
        return super.onDisable();
    }

    @Subscribe
    public void onDisplay(EventDisplay event) {
        if (event.getType() != EventDisplay.Type.POST || mc.world == null || mc.player == null) {
            return;
        }

        float partialTicks = event.getPartialTicks();
        MatrixStack stack = new MatrixStack();
        long now = System.currentTimeMillis();
        boolean rememberEnabled = rememberBonusItems.get();
        boolean showItemsEnabled = showItems.get();
        boolean showEnchantmentsEnabled = showEnchantments.get();
        boolean friendsOutlineEnabled = friendsOutline.get();
        boolean healthBypassEnabled = healthBypass.get();
        boolean showSelfEnabled = show.getValueByName("\u0421\u0435\u0431\u044f").get();
        boolean showPlayersEnabled = show.getValueByName("\u0418\u0433\u0440\u043e\u043a\u0438").get();
        boolean showNakedEnabled = show.getValueByName("\u0413\u043e\u043b\u044b\u0435").get();
        ClientPlayNetHandler connection = mc.getConnection();

        if (rememberEnabled) {
            cleanupRememberedItems(now);
        } else if (!rememberedItems.isEmpty()) {
            rememberedItems.clear();
        }

        for (PlayerEntity entity : mc.world.getPlayers()) {
            if (!isValid(entity, showSelfEnabled, showPlayersEnabled, showNakedEnabled)) {
                continue;
            }

            Vector2d screenPos = getNameTagPosition(entity, partialTicks);
            if (screenPos == null || screenPos.x == Float.MAX_VALUE || screenPos.y == Float.MAX_VALUE) {
                continue;
            }

            String playerName = entity.getScoreboardName();
            boolean isFriend = FriendStorage.isFriend(playerName);

            float displayHealth = getDisplayHealth(entity, healthBypassEnabled);
            IFormattableTextComponent nameComponent = buildNameComponent(entity, isFriend, displayHealth);
            int tagColor = getTagBackground(isFriend, friendsOutlineEnabled);

            if (rememberEnabled) {
                updateRememberedItems(entity, now);
            }

            if (showItemsEnabled) {
                renderPlayerItems(stack, entity, partialTicks, (float) screenPos.x, (float) screenPos.y, showEnchantmentsEnabled);
                if (rememberEnabled) {
                    renderRememberedItems(stack, entity, nameComponent, (float) screenPos.x, (float) screenPos.y - 15.0F, now);
                }
            }

            ResourceLocation skin = getSkin(entity.getUniqueID(), connection);
            renderNameTag(stack, nameComponent, (float) screenPos.x, (float) screenPos.y - 15.0F, tagColor, skin);
        }

        if (showItemsEnabled) {
            renderDroppedItems(stack, partialTicks);
        }
    }

    private void renderDroppedItems(MatrixStack stack, float partialTicks) {
        for (Entity entity : mc.world.getAllEntities()) {
            if (!(entity instanceof ItemEntity) || !entity.isAlive()) {
                continue;
            }

            ItemStack itemStack = ((ItemEntity) entity).getItem();
            if (itemStack.isEmpty()) {
                continue;
            }

            Bounds bounds = getEntityBounds(entity, partialTicks);
            if (bounds == null) {
                continue;
            }

            renderDroppedItemTag(stack, itemStack, bounds);
        }
    }

    private void renderPlayerItems(MatrixStack stack, PlayerEntity player, float partialTicks, float tagX, float tagY, boolean showEnchantmentsEnabled) {
        drawArmorItems(stack, player, tagX, tagY, showEnchantmentsEnabled);

        Bounds bounds = getEntityBounds(player, partialTicks);
        if (bounds == null) {
            return;
        }

        drawHandItems(stack, player, bounds, showEnchantmentsEnabled);
    }

    private void updateRememberedItems(PlayerEntity player, long now) {
        rememberHeldItem(player, player.getHeldItemMainhand(), now);
        rememberHeldItem(player, player.getHeldItemOffhand(), now);
    }

    private void rememberHeldItem(PlayerEntity player, ItemStack stack, long now) {
        if (!isRememberableBonusItem(stack)) {
            return;
        }

        UUID uuid = player.getUniqueID();
        String key = getRememberedItemKey(stack);
        List<RememberedItem> items = rememberedItems.get(uuid);
        if (items == null) {
            items = new ArrayList<>(MAX_REMEMBERED_ITEMS + 1);
            rememberedItems.put(uuid, items);
        }

        for (RememberedItem item : items) {
            if (item.key.equals(key)) {
                item.stack = stack.copy();
                item.lastSeen = now;
                items.sort(REMEMBERED_ITEM_RECENCY);
                return;
            }
        }

        items.add(0, new RememberedItem(key, stack.copy(), now));
        items.sort(REMEMBERED_ITEM_RECENCY);
        while (items.size() > MAX_REMEMBERED_ITEMS) {
            items.remove(items.size() - 1);
        }
    }

    private void cleanupRememberedItems(long now) {
        Iterator<Map.Entry<UUID, List<RememberedItem>>> entryIterator = rememberedItems.entrySet().iterator();
        while (entryIterator.hasNext()) {
            List<RememberedItem> items = entryIterator.next().getValue();
            for (int i = items.size() - 1; i >= 0; i--) {
                if (now - items.get(i).lastSeen >= REMEMBERED_ITEM_LIFETIME_MS) {
                    items.remove(i);
                }
            }
            if (items.isEmpty()) {
                entryIterator.remove();
            }
        }
    }

    private boolean isRememberableBonusItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() == Items.TOTEM_OF_UNDYING) {
            return stack.isEnchanted() || stack.hasEffect();
        }
        if (stack.getItem() == Items.PLAYER_HEAD) {
            return stack.hasTag() && (stack.isEnchanted() || stack.hasEffect() || stack.hasDisplayName() || stack.getTag().contains("display"));
        }
        return false;
    }

    private String getRememberedItemKey(ItemStack stack) {
        ResourceLocation itemId = Registry.ITEM.getKey(stack.getItem());
        return (itemId == null ? stack.getItem().toString() : itemId.toString()) + "|" + (stack.hasTag() ? stack.getTag().toString() : "");
    }

    private Vector2d getNameTagPosition(PlayerEntity player, float partialTicks) {
        return getEntityTagPosition(player, partialTicks, player.getEyeHeight() + 0.3D);
    }

    private Vector2d getEntityTagPosition(Entity entity, float partialTicks, double yOffset) {
        double x = MathHelper.lerp(partialTicks, entity.lastTickPosX, entity.getPosX());
        double y = MathHelper.lerp(partialTicks, entity.lastTickPosY, entity.getPosY());
        double z = MathHelper.lerp(partialTicks, entity.lastTickPosZ, entity.getPosZ());
        return ProjectionUtil.project2D(x, y + yOffset, z);
    }

    private float getDisplayHealth(PlayerEntity player, boolean healthBypassEnabled) {
        if (!healthBypassEnabled) {
            return player.getHealth();
        }

        Float fallbackHealth = null;
        for (Map.Entry<ScoreObjective, Score> entry : mc.world.getScoreboard().getObjectivesForEntity(player.getName().getString()).entrySet()) {
            int score = entry.getValue().getScorePoints();
            if (score <= 0 || score > 1000) {
                continue;
            }

            if (entry.getKey().getRenderType() == ScoreCriteria.RenderType.HEARTS) {
                return score;
            }

            if (fallbackHealth == null) {
                fallbackHealth = (float) score;
            }
        }

        return fallbackHealth != null ? fallbackHealth : player.getHealth();
    }

    private Style getHealthStyle(float health) {
        if (health < 7.0F) {
            return HEALTH_LOW_STYLE;
        }
        if (health < 13.0F) {
            return HEALTH_MEDIUM_STYLE;
        }
        return HEALTH_HIGH_STYLE;
    }

    private int getTagBackground(boolean isFriend, boolean friendsOutlineEnabled) {
        return friendsOutlineEnabled && isFriend ? FRIEND_BACKGROUND_COLOR : BACKGROUND_COLOR;
    }

    private IFormattableTextComponent buildNameComponent(PlayerEntity player, boolean isFriend, float displayHealth) {
        String healthValue = displayHealth > 900.0F ? "Unknown" : String.valueOf((int) displayHealth);
        ITextComponent displayName = player.getDisplayName();

        IFormattableTextComponent component = new StringTextComponent("");
        if (isFriend) {
            component.append(styledText("[", BRACKET_STYLE))
                    .append(styledText("F", FRIEND_STYLE))
                    .append(styledText("] ", BRACKET_STYLE));
        }
        component.append(displayName);
        if (!hasTrailingSpace(displayName)) {
            component.append(new StringTextComponent(" "));
        }
        component.append(styledText("[", BRACKET_STYLE))
                .append(styledText(healthValue, getHealthStyle(displayHealth)))
                .append(styledText("]", BRACKET_STYLE));

        return component;
    }

    private boolean hasTrailingSpace(ITextComponent component) {
        String text = component.getString();
        if (text == null || text.isEmpty()) {
            return false;
        }

        for (int i = text.length() - 1; i >= 0; i--) {
            if (i > 0 && text.charAt(i - 1) == FORMAT_PREFIX) {
                i--;
                continue;
            }
            return text.charAt(i) == ' ';
        }
        return false;
    }

    private void drawArmorItems(MatrixStack stack, PlayerEntity player, float x, float y, boolean showEnchantmentsEnabled) {
        int armorCount = 0;
        for (int i = 0; i < player.inventory.armorInventory.size(); i++) {
            if (!player.inventory.armorInventory.get(i).isEmpty()) {
                armorCount++;
            }
        }

        if (armorCount == 0) {
            return;
        }

        float startX = x - armorCount * 6.0F;
        float itemY = y - ARMOR_ITEM_Y_OFFSET;

        for (int i = player.inventory.armorInventory.size() - 1; i >= 0; i--) {
            ItemStack itemStack = player.inventory.armorInventory.get(i);
            if (itemStack.isEmpty()) {
                continue;
            }

            drawScaledInventoryItem(stack, itemStack, startX + 1.5F, itemY + 1.5F, ARMOR_ITEM_SCALE);

            if (showEnchantmentsEnabled) {
                renderEnchantmentsUnderArmor(stack, itemStack, startX, itemY + 5.0F);
            }

            startX += ARMOR_ITEM_STEP;
        }
    }

    private void renderEnchantmentsUnderArmor(MatrixStack stack, ItemStack itemStack, float x, float y) {
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(itemStack);
        if (enchantments.isEmpty()) {
            return;
        }

        float enchantY = y - 9.0F;
        float enchantX = x + 1.5F;
        int index = 0;

        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            String name = getEnchantmentDisplayName(entry.getKey());
            if (name.isEmpty()) {
                continue;
            }
            Fonts.sfui.drawText(stack, name + entry.getValue(), enchantX, enchantY - index * 5.0F, -1, ENCHANT_TEXT_SIZE);
            index++;
        }
    }

    private void drawScaledInventoryItem(MatrixStack stack, ItemStack itemStack, float x, float y, float scale) {
        stack.push();
        stack.translate(x, y, 0.0D);
        stack.scale(scale, scale, scale);

        RenderSystem.pushMatrix();
        RenderSystem.multMatrix(stack.getLast().getMatrix());
        mc.getItemRenderer().renderItemIntoGUI(itemStack, 0, 0);
        mc.getItemRenderer().renderItemOverlayIntoGUI(mc.fontRenderer, itemStack, 0, 0, null);
        RenderSystem.popMatrix();

        stack.pop();
    }

    private void drawHandItems(MatrixStack stack, PlayerEntity player, Bounds bounds, boolean showEnchantmentsEnabled) {
        ItemStack mainHand = player.getHeldItemMainhand();
        ItemStack offHand = player.getHeldItemOffhand();

        float centerX = bounds.minX + (bounds.maxX - bounds.minX) / 2.0F;
        float y = bounds.maxY + 3.0F;

        if (!mainHand.isEmpty()) {
            IFormattableTextComponent mainName = getHandItemName(mainHand);
            y = drawHandItem(stack, mainHand, mainName, centerX, y, BACKGROUND_COLOR, showEnchantmentsEnabled);
        }

        if (!offHand.isEmpty()) {
            IFormattableTextComponent offName = getHandItemName(offHand);
            drawHandItem(stack, offHand, offName, centerX, y, BACKGROUND_COLOR, showEnchantmentsEnabled);
        }
    }

    private IFormattableTextComponent getHandItemName(ItemStack itemStack) {
        IFormattableTextComponent name = new StringTextComponent("")
                .setStyle(Style.EMPTY.setFormatting(itemStack.getRarity().color))
                .append(itemStack.getDisplayName().deepCopy());
        if (itemStack.getCount() > 1) {
            name.append(new StringTextComponent(" " + itemStack.getCount() + "x"));
        }
        return name;
    }

    private float getItemTextSize(boolean coloredName) {
        return coloredName ? COLORED_ITEM_TEXT_SIZE : TAG_TEXT_SIZE;
    }

    private float getDroppedItemIconScale(boolean coloredName) {
        return coloredName ? COLORED_DROPPED_ITEM_ICON_SCALE : DROPPED_ITEM_ICON_SCALE;
    }

    private boolean hasColoredText(ITextComponent component) {
        activeColoredText = false;
        component.getComponentWithStyle(styledColorDetector, Style.EMPTY);
        return activeColoredText;
    }

    private boolean hasFormattingColor(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        for (int i = 0; i + 1 < text.length(); i++) {
            if (text.charAt(i) != FORMAT_PREFIX) {
                continue;
            }

            TextFormatting formatting = TextFormatting.fromFormattingCode(text.charAt(++i));
            if (formatting != null && formatting.isColor()) {
                return true;
            }
        }
        return false;
    }

    private float drawHandItem(MatrixStack stack, ItemStack itemStack, ITextComponent name, float centerX, float y, int bgColor, boolean showEnchantmentsEnabled) {
        float textSize = TAG_TEXT_SIZE;
        float textWidth = getStyledTextWidth(name, textSize);
        RenderUtility.drawRect(stack,
                centerX - textWidth / 2.0F - 2.0F,
                y - 1.0F,
                centerX + textWidth / 2.0F + 2.0F,
                y + textSize + 2.0F,
                bgColor);
        drawStyledTextComponent(stack, name, centerX - textWidth / 2.0F, y, textSize, 0xFFFFFF);
        y += textSize + 3.0F;

        if (showEnchantmentsEnabled) {
            y = drawEnchantments(stack, itemStack, centerX, y);
        }

        return y;
    }

    private float drawEnchantments(MatrixStack stack, ItemStack itemStack, float centerX, float y) {
        Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(itemStack);
        if (enchantments.isEmpty()) {
            return y;
        }

        float initialY = y;
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            String name = getEnchantmentDisplayName(entry.getKey());
            if (name.isEmpty()) {
                continue;
            }

            String enchantment = name + entry.getValue();
            float textWidth = Fonts.sfui.getWidth(enchantment, ENCHANT_TEXT_SIZE);
            Fonts.sfui.drawText(stack, enchantment, centerX - textWidth / 2.0F, y, -1, ENCHANT_TEXT_SIZE);
            y += ENCHANT_TEXT_SIZE + 1.0F;
        }

        return y == initialY ? initialY : y + 2.0F;
    }

    private String getEnchantmentDisplayName(Enchantment enchantment) {
        ResourceLocation id = Registry.ENCHANTMENT.getKey(enchantment);
        if (id == null) {
            return "";
        }

        String path = id.getPath();
        switch (path) {
            case "sharpness": return "Shr";
            case "protection": return "Pro";
            case "fire_protection": return "FPro";
            case "feather_falling": return "Fea";
            case "blast_protection": return "BPro";
            case "projectile_protection": return "PPro";
            case "respiration": return "Res";
            case "aqua_affinity": return "Aqu";
            case "thorns": return "Thr";
            case "depth_strider": return "Dep";
            case "frost_walker": return "Fro";
            case "binding_curse": return "Bind";
            case "soul_speed": return "Soul";
            case "unbreaking": return "Unb";
            case "mending": return "Men";
            case "vanishing_curse": return "Van";
            case "power": return "Pow";
            case "punch": return "Pun";
            case "flame": return "Fla";
            case "infinity": return "Inf";
            case "luck_of_the_sea": return "Luc";
            case "lure": return "Lur";
            case "loyalty": return "Loy";
            case "impaling": return "Imp";
            case "riptide": return "Rip";
            case "channeling": return "Cha";
            case "multishot": return "Mul";
            case "quick_charge": return "Qui";
            case "piercing": return "Pier";
            case "smite": return "Smi";
            case "bane_of_arthropods": return "Ban";
            case "knockback": return "Knb";
            case "fire_aspect": return "Fir";
            case "looting": return "Loo";
            case "sweeping": return "Swe";
            case "efficiency": return "Eff";
            case "silk_touch": return "Sil";
            case "fortune": return "For";
            default: return path.length() >= 3 ? path.substring(0, 3) : path;
        }
    }

    private Bounds getEntityBounds(Entity entity, float partialTicks) {
        double x = MathHelper.lerp(partialTicks, entity.lastTickPosX, entity.getPosX());
        double y = MathHelper.lerp(partialTicks, entity.lastTickPosY, entity.getPosY());
        double z = MathHelper.lerp(partialTicks, entity.lastTickPosZ, entity.getPosZ());
        AxisAlignedBB box = entity.getBoundingBox().offset(x - entity.getPosX(), y - entity.getPosY(), z - entity.getPosZ());

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        boolean projected = false;

        for (int xIndex = 0; xIndex < 2; xIndex++) {
            double pointX = xIndex == 0 ? box.minX : box.maxX;
            for (int yIndex = 0; yIndex < 2; yIndex++) {
                double pointY = yIndex == 0 ? box.minY : box.maxY;
                for (int zIndex = 0; zIndex < 2; zIndex++) {
                    double pointZ = zIndex == 0 ? box.minZ : box.maxZ;
                    Vector2d screenPos = ProjectionUtil.project2D(pointX, pointY, pointZ);
                    if (!isProjected(screenPos)) {
                        continue;
                    }

                    projected = true;
                    minX = Math.min(minX, (float) screenPos.x);
                    minY = Math.min(minY, (float) screenPos.y);
                    maxX = Math.max(maxX, (float) screenPos.x);
                    maxY = Math.max(maxY, (float) screenPos.y);
                }
            }
        }

        return projected ? new Bounds(minX, minY, maxX, maxY) : null;
    }

    private boolean isProjected(Vector2d screenPos) {
        return screenPos != null && screenPos.x != Float.MAX_VALUE && screenPos.y != Float.MAX_VALUE;
    }

    private void renderNameTag(MatrixStack stack, ITextComponent text, float x, float y, int color, ResourceLocation skin) {
        stack.push();
        stack.translate(x, y, 0.0D);
        stack.scale(TAG_SCALE, TAG_SCALE, TAG_SCALE);

        float headSize = HEAD_SIZE;
        float headGap = 2.0F;
        float textWidth = getTextComponentWidth(text, TAG_TEXT_SIZE) + 3.0F;
        float totalWidth = headSize + headGap + textWidth;
        float renderX = -(totalWidth / 2.0F);

        RenderUtility.drawRect(stack, renderX - 2.0F, -2.0F, renderX - 1.0F + totalWidth, 9.0F, color);
        drawHead(stack, skin, renderX, -0.5F, headSize, 0.0F, 1.0F);
        drawTextComponent(stack, text, renderX + headSize + headGap, 0.0F, TAG_TEXT_SIZE);

        stack.pop();
    }

    private void renderRememberedItems(MatrixStack stack, PlayerEntity player, ITextComponent tagText, float x, float y, long now) {
        List<RememberedItem> items = rememberedItems.get(player.getUniqueID());
        if (items == null || items.isEmpty()) {
            return;
        }

        float iconSize = 16.0F * REMEMBERED_ITEM_SCALE;
        float startX = x + getNameTagRenderedWidth(tagText) / 2.0F + 3.0F;
        float startY = y - 3.0F;
        float panelHeight = iconSize + REMEMBERED_ITEM_TEXT_SIZE + 5.0F;
        float currentX = startX;

        for (RememberedItem rememberedItem : items) {
            String timeText = formatRememberedTime(now - rememberedItem.lastSeen);
            float timeWidth = Fonts.sfui.getWidth(timeText, REMEMBERED_ITEM_TEXT_SIZE);
            float panelWidth = Math.max(iconSize, timeWidth) + 4.0F;
            float iconX = currentX + (panelWidth - iconSize) / 2.0F;
            float textX = currentX + (panelWidth - timeWidth) / 2.0F;

            RenderUtility.drawRect(stack, currentX, startY, currentX + panelWidth, startY + panelHeight, BACKGROUND_COLOR);
            drawScaledItem(stack, rememberedItem.stack, iconX, startY + 1.0F, REMEMBERED_ITEM_SCALE);
            Fonts.sfui.drawText(stack, timeText, textX, startY + iconSize + 3.0F, -1, REMEMBERED_ITEM_TEXT_SIZE);

            currentX += panelWidth + 2.0F;
        }
    }

    private float getNameTagRenderedWidth(ITextComponent text) {
        float headGap = 2.0F;
        float textWidth = getTextComponentWidth(text, TAG_TEXT_SIZE) + 3.0F;
        return (HEAD_SIZE + headGap + textWidth) * TAG_SCALE;
    }

    private String formatRememberedTime(long elapsedMs) {
        long totalSeconds = Math.max(0L, Math.min(REMEMBERED_ITEM_LIFETIME_MS / 1000L, elapsedMs / 1000L));
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return minutes + ":" + (seconds < 10L ? "0" : "") + seconds;
    }

    private void renderDroppedItemTag(MatrixStack stack, ItemStack itemStack, Bounds bounds) {
        IFormattableTextComponent name = getHandItemName(itemStack);
        boolean coloredName = hasColoredText(itemStack.getDisplayName());
        float textSize = getItemTextSize(coloredName);
        float iconScale = getDroppedItemIconScale(coloredName);
        float iconSize = 16.0F * iconScale;
        float centerX = bounds.minX + (bounds.maxX - bounds.minX) / 2.0F;
        float nameWidth = getStyledTextWidth(name, textSize);
        float totalWidth = iconSize + DROPPED_ITEM_ICON_PADDING + nameWidth + 1.0F;
        float bgX = centerX - totalWidth / 2.0F;
        float bgHeight = Math.max(10.0F, Math.max(textSize + 3.0F, iconSize + 2.0F));
        float nameY = bounds.minY - bgHeight - 2.0F;
        float iconY = nameY + (bgHeight - iconSize) / 2.0F;
        float textY = nameY + (bgHeight - textSize) / 2.0F + 0.5F;

        RenderUtility.drawRect(stack,
                bgX - 1.0F,
                nameY,
                bgX + totalWidth + 1.0F,
                nameY + bgHeight,
                BACKGROUND_COLOR);
        drawScaledItem(stack, itemStack, bgX, iconY, iconScale);
        drawStyledTextComponent(stack,
                name,
                bgX + iconSize + DROPPED_ITEM_ICON_PADDING,
                textY,
                textSize,
                0xFFFFFF);
    }

    private void drawScaledItem(MatrixStack stack, ItemStack itemStack, float x, float y, float scale) {
        stack.push();
        stack.translate(x, y, 0.0D);
        stack.scale(scale, scale, 1.0F);

        RenderSystem.pushMatrix();
        RenderSystem.multMatrix(stack.getLast().getMatrix());
        mc.getItemRenderer().renderItemAndEffectIntoGUI(itemStack, 0, 0);
        RenderSystem.popMatrix();

        stack.pop();
    }

    private void drawHead(MatrixStack stack, ResourceLocation skinLocation, float x, float y, float size, float radius, float alpha) {
        Texture texture = mc.getTextureManager().getTexture(skinLocation);

        if (texture == null) {
            mc.getTextureManager().bindTexture(skinLocation);
            texture = mc.getTextureManager().getTexture(skinLocation);
            if (texture == null) {
                return;
            }
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.bindTexture(texture.getGlTextureId());

        RenderSystem.pushMatrix();
        RenderSystem.multMatrix(stack.getLast().getMatrix());

        HEAD_SHADER.attach();
        ARBShaderObjects.glUniform1iARB(HEAD_TEXTURE_UNIFORM, 0);
        ARBShaderObjects.glUniform2fARB(HEAD_SIZE_UNIFORM, size, size);
        ARBShaderObjects.glUniform1fARB(HEAD_RADIUS_UNIFORM, radius);
        ARBShaderObjects.glUniform1fARB(HEAD_HURT_TIME_UNIFORM, 0.0F);
        ARBShaderObjects.glUniform1fARB(HEAD_ALPHA_UNIFORM, alpha);
        ARBShaderObjects.glUniform1fARB(HEAD_START_X_UNIFORM, 8.0F);
        ARBShaderObjects.glUniform1fARB(HEAD_END_X_UNIFORM, 16.0F);
        ARBShaderObjects.glUniform1fARB(HEAD_START_Y_UNIFORM, 8.0F);
        ARBShaderObjects.glUniform1fARB(HEAD_END_Y_UNIFORM, 16.0F);
        ARBShaderObjects.glUniform1fARB(HEAD_TEX_X_SIZE_UNIFORM, 64.0F);
        ARBShaderObjects.glUniform1fARB(HEAD_TEX_Y_SIZE_UNIFORM, 64.0F);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(x, y + size, 0.0D).tex(0.0F, 1.0F).endVertex();
        buffer.pos(x + size, y + size, 0.0D).tex(1.0F, 1.0F).endVertex();
        buffer.pos(x + size, y, 0.0D).tex(1.0F, 0.0F).endVertex();
        buffer.pos(x, y, 0.0D).tex(0.0F, 0.0F).endVertex();
        tessellator.draw();

        HEAD_SHADER.detach();
        RenderSystem.popMatrix();
        RenderSystem.disableBlend();
    }

    private ResourceLocation getSkin(UUID uuid, ClientPlayNetHandler connection) {
        if (connection != null) {
            NetworkPlayerInfo info = connection.getPlayerInfo(uuid);
            if (info != null) {
                return info.getLocationSkin();
            }
        }
        return DefaultPlayerSkin.getDefaultSkin(uuid);
    }

    public static int hexToInt(String hex) {
        hex = hex.replaceFirst("#", "");
        if (hex.length() == 6) {
            hex = "FF" + hex;
        }
        return (int) Long.parseLong(hex, 16);
    }

    public static IFormattableTextComponent solidColorText(String message, int color) {
        return new StringTextComponent(message)
                .setStyle(Style.EMPTY.setColor(net.minecraft.util.text.Color.fromInt(color & 0xFFFFFF)));
    }

    private static Style styleFromColor(int color) {
        return Style.EMPTY.setColor(net.minecraft.util.text.Color.fromInt(color & 0xFFFFFF));
    }

    private static IFormattableTextComponent styledText(String message, Style style) {
        return new StringTextComponent(message).setStyle(style);
    }

    private void drawTextComponent(MatrixStack stack, ITextComponent component, float x, float y, float size) {
        drawComponent(stack, component, x, y, size, 0xFFFFFF);
    }

    private void drawStyledTextComponent(MatrixStack stack, ITextComponent component, float x, float y, float size, int fallbackColor) {
        drawStyledComponent(stack, component, x, y, size, fallbackColor, Style.EMPTY);
    }

    private float getStyledTextWidth(ITextComponent component, float size) {
        return getStyledComponentWidth(component, size, Style.EMPTY);
    }

    private float drawStyledComponent(MatrixStack stack, ITextComponent component, float x, float y, float size, int fallbackColor, Style inheritedStyle) {
        activeTextStack = stack;
        activeTextX = x;
        activeTextY = y;
        activeTextSize = size;
        activeTextFallbackColor = fallbackColor;
        activeTextOffset = 0.0F;
        component.getComponentWithStyle(styledTextDrawer, inheritedStyle);
        return activeTextOffset;
    }

    private float getStyledComponentWidth(ITextComponent component, float size, Style inheritedStyle) {
        activeMeasureSize = size;
        activeMeasureWidth = 0.0F;
        component.getComponentWithStyle(styledTextMeasurer, inheritedStyle);
        return activeMeasureWidth;
    }

    private float drawComponent(MatrixStack stack, ITextComponent component, float x, float y, float size, int inheritedColor) {
        int color = component.getStyle().getColor() == null ? inheritedColor : component.getStyle().getColor().getColor();
        float offset = drawFormattedString(stack, component.getUnformattedComponentText(), x, y, size, color);

        for (ITextComponent sibling : component.getSiblings()) {
            offset += drawComponent(stack, sibling, x + offset, y, size, color);
        }

        return offset;
    }

    private float getTextComponentWidth(ITextComponent component, float size) {
        return getComponentWidth(component, size);
    }

    private float getComponentWidth(ITextComponent component, float size) {
        float width = getFormattedStringWidth(component.getUnformattedComponentText(), size);
        for (ITextComponent sibling : component.getSiblings()) {
            width += getComponentWidth(sibling, size);
        }
        return width;
    }

    private float drawFormattedString(MatrixStack stack, String text, float x, float y, float size, int baseColor) {
        if (text == null || text.isEmpty()) {
            return 0.0F;
        }

        float offset = 0.0F;
        int currentColor = baseColor & 0xFFFFFF;
        StringBuilder part = formattedTextPart;
        part.setLength(0);

        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == FORMAT_PREFIX && i + 1 < text.length()) {
                offset += drawPart(stack, part, x + offset, y, size, currentColor);

                TextFormatting formatting = TextFormatting.fromFormattingCode(text.charAt(++i));
                if (formatting == TextFormatting.RESET) {
                    currentColor = baseColor & 0xFFFFFF;
                } else if (formatting != null && formatting.isColor() && formatting.getColor() != null) {
                    currentColor = formatting.getColor();
                }
                continue;
            }

            part.append(ch);
        }

        offset += drawPart(stack, part, x + offset, y, size, currentColor);
        return offset;
    }

    private float drawPart(MatrixStack stack, StringBuilder part, float x, float y, float size, int color) {
        if (part.length() == 0) {
            return 0.0F;
        }

        String text = part.toString();
        Fonts.sfui.drawText(stack, text, x, y, 0xFF000000 | (color & 0xFFFFFF), size);
        part.setLength(0);
        return Fonts.sfui.getWidth(text, size);
    }

    private float getFormattedStringWidth(String text, float size) {
        if (text == null || text.isEmpty()) {
            return 0.0F;
        }
        if (text.indexOf(FORMAT_PREFIX) < 0) {
            return Fonts.sfui.getWidth(text, size);
        }

        strippedText.setLength(0);
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == FORMAT_PREFIX && i + 1 < text.length()) {
                i++;
                continue;
            }
            strippedText.append(ch);
        }
        return Fonts.sfui.getWidth(strippedText.toString(), size);
    }

    private boolean isValid(Entity entity, boolean showSelfEnabled, boolean showPlayersEnabled, boolean showNakedEnabled) {
        if (entity == mc.player) {
            if (!showSelfEnabled) {
                return false;
            }
            return mc.gameSettings.getPointOfView() != PointOfView.FIRST_PERSON;
        }
        if (entity instanceof PlayerEntity && !showPlayersEnabled) {
            return false;
        }
        if (entity instanceof PlayerEntity && ((PlayerEntity) entity).getTotalArmorValue() == 0 && !showNakedEnabled) {
            return false;
        }
        if (!entity.isAlive()) {
            return false;
        }
        return true;
    }

    public boolean isValid(Entity entity) {
        if (entity == mc.player) {
            if (!show.getValueByName("Себя").get()) {
                return false;
            }
            return mc.gameSettings.getPointOfView() != PointOfView.FIRST_PERSON;
        }
        if (entity instanceof PlayerEntity && !show.getValueByName("Игроки").get()) {
            return false;
        }
        if (entity instanceof PlayerEntity && ((PlayerEntity) entity).getTotalArmorValue() == 0 && !show.getValueByName("Голые").get()) {
            return false;
        }
        if (!entity.isAlive()) {
            return false;
        }
        return true;
    }


    private static final class RememberedItem {
        private final String key;
        private ItemStack stack;
        private long lastSeen;

        private RememberedItem(String key, ItemStack stack, long lastSeen) {
            this.key = key;
            this.stack = stack;
            this.lastSeen = lastSeen;
        }
    }

    private static final class Bounds {
        private final float minX;
        private final float minY;
        private final float maxX;
        private final float maxY;

        private Bounds(float minX, float minY, float maxX, float maxY) {
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
        }
    }
}
