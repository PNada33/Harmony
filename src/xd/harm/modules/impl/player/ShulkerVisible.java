package xd.harm.modules.impl.player;

import com.google.common.eventbus.Subscribe;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.Block;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.DyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector2f;
import org.lwjgl.opengl.GL11;
import xd.harm.events.render.EventDisplay;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.utils.math.MathUtil;
import xd.harm.utils.render.color.ColorUtils;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@ModuleRegister(name = "ShulkerVisible", category = Category.Player, desc = "Показывает содержимое шалкера")
public class ShulkerVisible extends Module {
    private static final double MAX_DISTANCE = 15.0D;
    private static final double MAX_DISTANCE_SQ = MAX_DISTANCE * MAX_DISTANCE;
    private static final int ITEM_CACHE_INTERVAL_TICKS = 5;

    public final BooleanSetting showInInventory = new BooleanSetting("Показ в инвентаре", true);

    private final BufferBuilder BUILDER = Tessellator.getInstance().getBuffer();
    private final Tessellator TESSELLATOR = Tessellator.getInstance();
    ResourceLocation SHULKER_TOOLTIP = new ResourceLocation("harmony/images/shulker/shulker_box_tooltip.png");

    public ShulkerVisible() {
        addSettings(showInInventory);
    }

    private final List<CachedDroppedShulker> cachedItems = new ArrayList<>();
    private final Map<Integer, CachedDroppedShulker> droppedShulkerCache = new HashMap<>();
    private int itemCacheTicks = ITEM_CACHE_INTERVAL_TICKS;

    @Subscribe
    public void onUpdate(EventUpdate e) {
        if (mc.world == null || mc.player == null) {
            cachedItems.clear();
            droppedShulkerCache.clear();
            itemCacheTicks = ITEM_CACHE_INTERVAL_TICKS;
            return;
        }

        if (++itemCacheTicks < ITEM_CACHE_INTERVAL_TICKS) {
            return;
        }

        itemCacheTicks = 0;
        cachedItems.clear();
        Set<Integer> aliveShulkers = new HashSet<>();
        for (Entity entity : mc.world.getAllEntities()) {
            if (!(entity instanceof ItemEntity) || !entity.isAlive()) {
                continue;
            }

            ItemEntity itemEntity = (ItemEntity) entity;
            if (isShulkerBox(itemEntity.getItem())) {
                int entityId = itemEntity.getEntityId();
                aliveShulkers.add(entityId);
                CachedDroppedShulker cached = droppedShulkerCache.get(entityId);
                if (cached == null) {
                    ItemStack stack = itemEntity.getItem().copy();
                    cached = new CachedDroppedShulker(itemEntity, stack, getItemsInShulker(stack));
                    droppedShulkerCache.put(entityId, cached);
                } else {
                    cached.entity = itemEntity;
                }
                cachedItems.add(cached);
            }
        }
        droppedShulkerCache.keySet().removeIf(entityId -> !aliveShulkers.contains(entityId));
    }

    @Subscribe
    public void onRender(EventDisplay event) {
        if (event.getType() != EventDisplay.Type.HIGH || mc.world == null || mc.player == null || mc.currentScreen != null) {
            return;
        }
        
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) {
                continue;
            }

            ItemStack stack = player.inventory.getCurrentItem();
            if (isShulkerBox(stack)) {
                renderShulker(stack, player.getPosX(), player.getPosY() + player.getHeight() + 1.5f, player.getPosZ(), event);
            }
        }

        for (CachedDroppedShulker cached : cachedItems) {
            ItemEntity itemEntity = cached.entity;
            if (itemEntity.isAlive()) {
                renderShulker(cached.stack, cached.items, itemEntity.getPosX(), itemEntity.getPosY() + 0.75f, itemEntity.getPosZ(), event);
            }
        }

        RenderSystem.disableBlend();
    }

    private void renderShulker(ItemStack stack, double x, double y, double z, EventDisplay event) {
        if (!isShulkerBox(stack)) {
            return;
        }

        double distanceSq = getDistanceSq(x, y, z);
        if (distanceSq > MAX_DISTANCE_SQ) {
            return;
        }

        renderShulker(stack, getItemsInShulker(stack), x, y, z, event);
    }

    private void renderShulker(ItemStack stack, List<ItemStack> items, double x, double y, double z, EventDisplay event) {
        if (!isShulkerBox(stack) || items.isEmpty()) {
            return;
        }

        double distanceSq = getDistanceSq(x, y, z);
        if (distanceSq > MAX_DISTANCE_SQ) {
            return;
        }

        Vector2f vec = xd.harm.utils.projections.ProjectionUtil.project((float) x, (float) y, (float) z);
        if (vec == null) {
            return;
        }

        double distance = Math.sqrt(distanceSq);
        double scale = MathUtil.lerp(0.5, 1.75, 2.5 / distance);

            GlStateManager.pushMatrix();
            GlStateManager.translatef(vec.x, vec.y, 0);
            GlStateManager.scalef((float) scale, (float) scale, 1.0f);

            float slotSize = 18.0f;
            float slotSpacing = 0.3f;

            float bgScale = 1.5f;
            float bgWidth = 174 * bgScale;
            float bgHeight = 166 * bgScale;

            float bgOffsetX = - 0.1f;
            float bgOffsetY = 11.5f;

            ShulkerBoxBlock shulkerBlock = (ShulkerBoxBlock) Block.getBlockFromItem(stack.getItem());
            DyeColor color = shulkerBlock.getColor();

            int shulkerColor;
            if (color != null) {
                float[] colorComponents = color.getColorComponentValues();
                shulkerColor = ColorUtils.rgba(
                        (int)(colorComponents[0] * 255),
                        (int)(colorComponents[1] * 255),
                        (int)(colorComponents[2] * 255),
                        255
                );
            } else {
                shulkerColor = ColorUtils.rgba(138, 93, 150, 255);
            }

            mc.getTextureManager().bindTexture(SHULKER_TOOLTIP);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

            Matrix4f matrix = event.getMatrixStack().getLast().getMatrix();
            BUILDER.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);

            int r = ColorUtils.getRed(shulkerColor);
            int g = ColorUtils.getGreen(shulkerColor);
            int b = ColorUtils.getBlue(shulkerColor);
            int a = ColorUtils.getAlpha(shulkerColor);

            float x1 = bgOffsetX;
            float y1 = bgOffsetY;
            float x2 = bgWidth + bgOffsetX;
            float y2 = bgHeight + bgOffsetY;

            float u1 = 0.0f;
            float v1 = 0.0f;
            float u2 = 1.0f;
            float v2 = 1.0f;

            BUILDER.pos(matrix, x1, y2, 0).color(r, g, b, a).tex(u1, v2).endVertex();
            BUILDER.pos(matrix, x2, y2, 0).color(r, g, b, a).tex(u2, v2).endVertex();
            BUILDER.pos(matrix, x2, y1, 0).color(r, g, b, a).tex(u2, v1).endVertex();
            BUILDER.pos(matrix, x1, y1, 0).color(r, g, b, a).tex(u1, v1).endVertex();

            TESSELLATOR.draw();

            float startX = 8;
            float startY = 18;

            float posX = startX;
            float posY = startY;

            int itemIndex = 0;
            for (ItemStack item : items) {
                if (!item.isEmpty()) {
                    mc.getItemRenderer().renderItemAndEffectIntoGUI(item, (int) posX, (int) posY);
                    mc.getItemRenderer().renderItemOverlayIntoGUI(mc.fontRenderer, item, (int) posX, (int) posY, null);
                }

                posX += slotSize + slotSpacing;
                itemIndex++;
                if (itemIndex % 9 == 0) {
                    posX = startX;
                    posY += slotSize + slotSpacing;
                }
            }
            GlStateManager.popMatrix();
    }

    private List<ItemStack> getItemsInShulker(ItemStack stack) {
        CompoundNBT compoundnbt = stack.getChildTag("BlockEntityTag");
        if (compoundnbt != null && compoundnbt.contains("Items", 9)) {
            NonNullList<ItemStack> nonnulllist = NonNullList.withSize(27, ItemStack.EMPTY);
            ItemStackHelper.loadAllItems(compoundnbt, nonnulllist);
            List<ItemStack> items = new ArrayList<>(27);
            for (ItemStack item : nonnulllist) {
                if (!item.isEmpty()) {
                    items.add(item);
                }
            }
            return items;
        }
        return Collections.emptyList();
    }

    private boolean isShulkerBox(ItemStack stack) {
        return stack != null && !stack.isEmpty() && Block.getBlockFromItem(stack.getItem()) instanceof ShulkerBoxBlock;
    }

    private double getDistanceSq(double x, double y, double z) {
        double dx = mc.player.getPosX() - x;
        double dy = mc.player.getPosY() - y;
        double dz = mc.player.getPosZ() - z;
        return dx * dx + dy * dy + dz * dz;
    }

    private static final class CachedDroppedShulker {
        private ItemEntity entity;
        private final ItemStack stack;
        private final List<ItemStack> items;

        private CachedDroppedShulker(ItemEntity entity, ItemStack stack, List<ItemStack> items) {
            this.entity = entity;
            this.stack = stack;
            this.items = items;
        }
    }
}
