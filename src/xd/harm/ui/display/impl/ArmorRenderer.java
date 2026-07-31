package xd.harm.ui.display.impl;

import xd.harm.events.render.EventDisplay;
import xd.harm.ui.display.ElementRenderer;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.util.HandSide;

import com.mojang.blaze3d.systems.RenderSystem;
import xd.harm.utils.animations.Direction;
import xd.harm.utils.math.MathUtil;

@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class ArmorRenderer implements ElementRenderer {

    final float[] itemScales = new float[]{1f, 1f, 1f, 1f};
    final float[] itemYOffsets = new float[]{0f, 0f, 0f, 0f};

    @Override
    public void render(EventDisplay eventDisplay) {

        boolean isLeftHanded = mc.gameSettings.mainHand == HandSide.LEFT;

        int offsetX = isLeftHanded ? -95 - (16 * 4 + 6) : 95;

        int posX = window.getScaledWidth() / 2 + offsetX;
        int posY = window.getScaledHeight() - (16 + 2) + getChatHudYOffset();

        boolean hasBroken = false;
        for (ItemStack itemStack : mc.player.getArmorInventoryList()) {
            if (isBroken(itemStack)) {
                hasBroken = true;
                break;
            }
        }

        int i = 0;
        for (ItemStack itemStack : mc.player.getArmorInventoryList()) {
            if (itemStack.isEmpty()) {
                i++;
                continue;
            }

            boolean broken = isBroken(itemStack);
            float targetScale = 1.0f;
            float targetY = 0f;
            
            if (hasBroken) {
                targetScale = broken ? 1.10f : 0.9f;
                targetY = broken ? -6f : 2f;
            }

            itemScales[i] = MathUtil.fast(itemScales[i], targetScale, 15f);
            itemYOffsets[i] = MathUtil.fast(itemYOffsets[i], targetY, 15f);

            float rotate = 0f;
            float hoverY = 0f;

            if (broken && itemScales[i] > 1.05f) {
                rotate = (float) Math.sin((System.currentTimeMillis() + i * 150) / 40.0) * 4f;
                hoverY = (float) Math.sin(System.currentTimeMillis() / 250.0) * 1.5f;
            }

            RenderSystem.pushMatrix();
            RenderSystem.translatef(posX + 8, posY + 8 + itemYOffsets[i] + hoverY, 0);
            RenderSystem.rotatef(rotate, 0, 0, 1.0f);
            RenderSystem.scalef(itemScales[i], itemScales[i], 1.0f);

            if (broken) {
                float alpha = (float) Math.abs(Math.sin(System.currentTimeMillis() / 200.0)) * 0.6f + 0.3f;
                int redColor = xd.harm.utils.render.color.ColorUtils.rgba(255, 30, 30, (int)(alpha * 255));
                xd.harm.utils.render.rect.RenderUtility.drawShadow(-8, -8, 16, 16, 12, redColor);
            }

            mc.getItemRenderer().renderItemAndEffectIntoGUI(itemStack, -8, -8);
            mc.getItemRenderer().renderItemOverlayIntoGUI(mc.fontRenderer, itemStack, -8, -8, null);

            RenderSystem.popMatrix();

            posX += 16 + 2;
            i++;
        }
    }

    private boolean isBroken(ItemStack itemStack) {
        if (itemStack.isEmpty() || !itemStack.isDamageable()) return false;
        return (itemStack.getMaxDamage() - itemStack.getDamage()) / (float) itemStack.getMaxDamage() <= 0.2f;
    }

    private int getChatHudYOffset() {
        if (mc.currentScreen instanceof ChatScreen) {
            return Math.round(-15.0F * ChatScreen.getHotbarAnimationOutput());
        }

        if (ChatScreen.hotbarAnimation != null && ChatScreen.hotbarAnimation.getDirection() == Direction.BACKWARDS) {
            return Math.round(-15.0F * ChatScreen.getHotbarAnimationOutput());
        }

        return 0;
    }
}
