package xd.harm.ui.notify;

import xd.harm.config.FriendStorage;
import xd.harm.modules.impl.render.Theme;
import xd.harm.utils.animations.Animation;
import xd.harm.utils.animations.Direction;
import xd.harm.utils.animations.impl.EaseBackIn;
import xd.harm.utils.animations.impl.EaseInOutQuad;
import xd.harm.utils.math.MathUtil;
import xd.harm.utils.math.Vector4i;
import xd.harm.utils.render.KawaseBlur;
import xd.harm.utils.render.color.ColorUtils;
import xd.harm.utils.render.font.Fonts;
import xd.harm.utils.render.rect.RenderUtility;
import xd.harm.utils.text.font.ClientFonts;
import xd.harm.utils.text.GradientUtil;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.potion.Effect;
import net.minecraft.util.math.vector.Vector4f;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.ITextComponent;
import org.lwjgl.opengl.GL11;
import xd.harm.utils.text.font.styled.StyledFont;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.ArrayList;

import static xd.harm.utils.client.IMinecraft.mc;

public class NotificationManager {
    public static final FriendStorage NOTIFICATION_MANAGER = null;
    private final CopyOnWriteArrayList<Notification> notifications = new CopyOnWriteArrayList();
    private MathUtil AnimationMath;
    boolean state;

    private static boolean isLoadingConfig = false;

    private static final ItemStack ENDER_EYE_STACK = new ItemStack(Items.ENDER_EYE);
    private static final ItemStack SPLASH_POTION_STACK = new ItemStack(Items.SPLASH_POTION);
    private static final ItemStack NETHERITE_SCRAP_STACK = new ItemStack(Items.NETHERITE_SCRAP);
    private static final ItemStack CROSSBOW_STACK = new ItemStack(Items.CROSSBOW);
    private static final ItemStack DRIED_KELP_STACK = new ItemStack(Items.DRIED_KELP);
    private static final ItemStack SUGAR_STACK = new ItemStack(Items.SUGAR);
    private static final ItemStack PHANTOM_MEMBRANE_STACK = new ItemStack(Items.PHANTOM_MEMBRANE);
    private static final ItemStack SNOWBALL_STACK = new ItemStack(Items.SNOWBALL);
    private static final ItemStack FIRE_CHARGE_STACK = new ItemStack(Items.FIRE_CHARGE);

    public static void setLoadingConfig(boolean loading) {
        isLoadingConfig = loading;
    }

    public void add(String text, String content, int time, NotificationType type) {
        if (isLoadingConfig) {
            return;
        }
        this.notifications.add(new Notification(text, content, time, type, null));
    }

    public void add(String text, String content, int time, NotificationType type, Effect effect) {
        if (isLoadingConfig) {
            return;
        }
        this.notifications.add(new Notification(text, content, time, type, effect));
    }

    public void add(String text, String content, int time) {
        if (isLoadingConfig) {
            return;
        }
        this.notifications.add(new Notification(text, content, time, NotificationType.DEFAULT, null));
    }

    public void draw(MatrixStack stack) {
        int yOffset = 0;
        for (Notification notification : this.notifications) {
            if (System.currentTimeMillis() - notification.getTime() <= (long)notification.time2 * 1000L - 300L) {
                notification.yAnimation.setDirection(Direction.FORWARDS);
                notification.animation.setDirection(Direction.FORWARDS);
            }
            notification.alpha = (float)notification.animation.getOutput();
            if (System.currentTimeMillis() - notification.getTime() > (long)notification.time2 * 1000L) {
                notification.yAnimation.setDirection(Direction.BACKWARDS);
                notification.animation.setDirection(Direction.BACKWARDS);
            }
            if (notification.animation.finished(Direction.BACKWARDS)) {
                this.notifications.remove(notification);
                continue;
            }
            float x = (float) mc.getMainWindow().scaledWidth() - (Fonts.sfMedium.getWidth(notification.getText(), 7.0f) + 8.0f) - 10.0f;
            float y = mc.getMainWindow().scaledHeight() - 40;
            notification.yAnimation.setEndPoint(yOffset);
            notification.yAnimation.setDuration(1000);
            notification.setX(x);
            notification.setY(MathUtil.fast(notification.getY(), y -= (float)((double)notification.draw(stack) * notification.yAnimation.getOutput() + 3.0), 15.0f));
            ++yOffset;
        }
    }

    public enum NotificationType {
        DEFAULT, SPEC, POTION_WARN, POTION_END, ARMOR_WARN,
        USED_DISORIENTATION, NOT_FOUND_DISORIENTATION, USED_SERKA, NOT_FOUND_SERKA,
        USED_TRAP, NOT_FOUND_TRAP, USED_CROSSBOW, NOT_FOUND_CROSSBOW,
        USED_OTRIGA, NOT_FOUND_OTRIGA, USED_PLAST, NOT_FOUND_PLAST,
        USED_BLATANT, NOT_FOUND_BLATANT, USED_PHANTOM, NOT_FOUND_PHANTOM,
        USED_SNOWBALL, USED_FIRECHARGE, NOT_FOUND_FIRECHARGE, NOT_FOUND_SNOWBALL
    }


    private class Notification {
        private float x = 0.0f;
        private float y = mc.getMainWindow().scaledHeight() + 28;
        private String text;
        private String content;
        private long time = System.currentTimeMillis();
        public Animation animation = new EaseInOutQuad(1000, 1.0, Direction.FORWARDS);
        public Animation yAnimation = new EaseBackIn(1000, 1.0, 1.0f);
        float alpha;
        int time2 = 3;
        private boolean isState;
        private boolean state;
        private final NotificationType type;
        private final Effect effect;

        public Notification(String text, String content, int time, NotificationType type, Effect effect) {
            this.text = text;
            this.content = content;
            this.time2 = time;
            this.type = type;
            this.effect = effect;
        }

        class TextPart {
            String text;
            int color;
            boolean isThemed;
            TextPart(String t, int c, boolean themed) { this.text = t; this.color = c; this.isThemed = themed; }
        }

        public float draw(MatrixStack stack) {
            mc.gameRenderer.setupOverlayRendering(2);

            float posX = mc.getMainWindow().getScaledWidth() / 2.0F;
            float posY = (float) mc.getMainWindow().getScaledHeight() + (float) mc.getMainWindow().getScaledHeight() / 2 - y - 30;

            float fontSize = 5.5f;
            float notificationHeight = 12.5F;
            float cornerRadius = 2.5f;

            float combinedAlpha = Math.max(0.0f, Math.min(1.0f, (float) animation.getOutput()));

            if (combinedAlpha <= 0.01f) {
                return notificationHeight + 4.0F;
            }

            int themeColor1 = Theme.MainColor(0);
            int whiteColor = ColorUtils.rgba(240, 240, 245, (int)(255 * combinedAlpha));
            int themeColorAlpha = ColorUtils.reAlphaInt(themeColor1, (int)(255 * combinedAlpha));

            List<TextPart> parts = new ArrayList<>();

            if (this.text.contains("Свапнул на")) {
                String[] splitParts = this.text.split(" ", 3);
                if (splitParts.length >= 3) {
                    parts.add(new TextPart(splitParts[0] + " " + splitParts[1] + " ", whiteColor, false));
                    parts.add(new TextPart(splitParts[2], themeColorAlpha, true));
                } else {
                    parts.add(new TextPart(this.text, whiteColor, false));
                }
            } else if (type == NotificationType.SPEC) {
                String prefix = "спек ";
                String nick = this.text;
                if (this.text.startsWith(prefix)) {
                    nick = this.text.substring(prefix.length());
                } else {
                    int spaceIndex = this.text.indexOf(' ');
                    if (spaceIndex != -1 && spaceIndex + 1 < this.text.length()) {
                        prefix = this.text.substring(0, spaceIndex + 1);
                        nick = this.text.substring(spaceIndex + 1);
                    } else {
                        prefix = "";
                        nick = this.text;
                    }
                }
                if (!prefix.isEmpty()) parts.add(new TextPart(prefix, whiteColor, false));
                if (!nick.isEmpty()) parts.add(new TextPart(nick, themeColorAlpha, true));
            } else if (type == NotificationType.POTION_WARN || type == NotificationType.POTION_END) {
                String effectName = "";
                String beforeEffect = "";
                String afterEffect = "";
                if (this.text.startsWith("Эффект ")) {
                    if (this.text.contains(" скоро закончится")) {
                        beforeEffect = "Эффект ";
                        afterEffect = " скоро закончится";
                        effectName = this.text.substring(beforeEffect.length(), this.text.length() - afterEffect.length());
                    } else if (this.text.contains(" закончился")) {
                        beforeEffect = "Эффект ";
                        afterEffect = " закончился";
                        effectName = this.text.substring(beforeEffect.length(), this.text.length() - afterEffect.length());
                    }
                }
                if (!effectName.isEmpty()) {
                    parts.add(new TextPart(beforeEffect, whiteColor, false));
                    parts.add(new TextPart(effectName, themeColorAlpha, true));
                    parts.add(new TextPart(afterEffect, whiteColor, false));
                } else {
                    parts.add(new TextPart(this.text, whiteColor, false));
                }
            } else if (type == NotificationType.ARMOR_WARN) {
                String prefix = "Броня скоро сломается: ";
                String suffix = " скоро сломается";
                String itemName = this.text;
                String before = "";
                String after = "";

                if (this.text.startsWith(prefix)) {
                    itemName = this.text.substring(prefix.length());
                    before = prefix;
                } else if (this.text.endsWith(suffix)) {
                    itemName = this.text.substring(0, this.text.length() - suffix.length());
                    after = suffix;
                } else {
                    int splitIndex = this.text.indexOf(": ");
                    if (splitIndex != -1 && splitIndex + 2 <= this.text.length()) {
                        before = this.text.substring(0, splitIndex + 2);
                        itemName = this.text.substring(splitIndex + 2);
                    }
                }

                if (!before.isEmpty()) parts.add(new TextPart(before, whiteColor, false));
                if (!itemName.isEmpty()) parts.add(new TextPart(itemName, themeColorAlpha, true));
                if (!after.isEmpty()) parts.add(new TextPart(after, whiteColor, false));
            } else if (this.text.startsWith("\u041c\u0430\u043b\u043e \u0445\u043f: ")) {
                String prefix = "\u041c\u0430\u043b\u043e \u0445\u043f: ";
                String hpValue = this.text.substring(prefix.length());
                parts.add(new TextPart(prefix, whiteColor, false));
                parts.add(new TextPart(hpValue, themeColorAlpha, true));
            } else {
                String lower = this.text.toLowerCase();
                String[] statuses = {"включен", "выключен", "использован", "использована", "использовано", "не найден", "не найдена", "не найдено", "успешно", "disabled", "enabled", "loaded config", "default notification"};
                boolean found = false;
                for (String status : statuses) {
                    int idx = lower.indexOf(status);
                    if (idx != -1) {
                        String actualStatus = this.text.substring(idx, idx + status.length());
                        if (idx == 0) {
                            parts.add(new TextPart(actualStatus + " ", whiteColor, false));
                            parts.add(new TextPart(this.text.substring(status.length()).trim(), themeColorAlpha, true));
                        } else {
                            parts.add(new TextPart(this.text.substring(0, idx).trim() + " ", themeColorAlpha, true));
                            parts.add(new TextPart(actualStatus, whiteColor, false));
                        }
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    parts.add(new TextPart(this.text, whiteColor, false));
                }
            }

            float textWidth = 0;
            for (TextPart p : parts) {
                if (!p.text.isEmpty()) {
                    textWidth += Fonts.sfuy.getWidth(p.text, fontSize);
                }
            }

            float paddingLeft = 4.5f;
            float paddingRight = 4.5f;
            float iconArea = 8.0f;

            float slashArea = Fonts.sfuy.getWidth("/", fontSize) + 4.0f;

            float vertSepWidth = 1.0f;
            float vertSepGap = 3.5f;

            float circleArea = 8.0f;

            float totalWidth = paddingLeft + iconArea + slashArea + textWidth + vertSepGap + vertSepWidth + vertSepGap + circleArea + paddingRight;
            float XStart = posX - totalWidth / 2.0F;
            float YStart = posY + 15.0F;

            float blurRadius = 3.0f * combinedAlpha;
            if (blurRadius >= 0.05f) {
                KawaseBlur.blur.updateBlur(blurRadius, 3);
                KawaseBlur.blur.render(() -> {
                    GL11.glEnable(GL11.GL_ALPHA_TEST);
                    GL11.glAlphaFunc(GL11.GL_GREATER, 0.01f);
                    RenderUtility.drawRoundedRect(XStart, YStart, totalWidth, notificationHeight, new Vector4f(cornerRadius, cornerRadius, cornerRadius, cornerRadius), ColorUtils.rgba(255, 255, 255, 255));
                    GL11.glDisable(GL11.GL_ALPHA_TEST);
                });
            }

            int bgAlpha = (int) (180 * combinedAlpha);
            if (bgAlpha > 0) {
                RenderUtility.drawRoundedRect(XStart, YStart, totalWidth, notificationHeight, new Vector4f(cornerRadius, cornerRadius, cornerRadius, cornerRadius), ColorUtils.rgba(16, 16, 20, bgAlpha));
            }

            int outlineAlpha = (int) (25 * combinedAlpha);
            if (outlineAlpha > 0) {
                RenderUtility.drawRoundedRectOutline(XStart, YStart, totalWidth, notificationHeight, cornerRadius, 0.5f, ColorUtils.rgba(255, 255, 255, outlineAlpha));
            }

            float currentX = XStart + paddingLeft;

            ItemStack iconStack = ItemStack.EMPTY;
            TextureAtlasSprite potionSprite = null;

            switch (type) {
                case USED_DISORIENTATION: case NOT_FOUND_DISORIENTATION: iconStack = ENDER_EYE_STACK; break;
                case USED_SERKA: case NOT_FOUND_SERKA: case USED_OTRIGA: case NOT_FOUND_OTRIGA: iconStack = SPLASH_POTION_STACK; break;
                case USED_TRAP: case NOT_FOUND_TRAP: iconStack = NETHERITE_SCRAP_STACK; break;
                case USED_CROSSBOW: case NOT_FOUND_CROSSBOW: iconStack = CROSSBOW_STACK; break;
                case USED_PLAST: case NOT_FOUND_PLAST: iconStack = DRIED_KELP_STACK; break;
                case USED_BLATANT: case NOT_FOUND_BLATANT: iconStack = SUGAR_STACK; break;
                case USED_PHANTOM: case NOT_FOUND_PHANTOM: iconStack = PHANTOM_MEMBRANE_STACK; break;
                case USED_SNOWBALL: case NOT_FOUND_SNOWBALL: iconStack = SNOWBALL_STACK; break;
                case USED_FIRECHARGE: case NOT_FOUND_FIRECHARGE: iconStack = FIRE_CHARGE_STACK; break;
                default: break;
            }

            if (effect != null && (type == NotificationType.POTION_WARN || type == NotificationType.POTION_END)) {
                potionSprite = mc.getPotionSpriteUploader().getSprite(effect);
            }

            float iconSize = 6.5f;
            float iconX = currentX + (iconArea - iconSize) / 2.0f;
            float iconY = YStart + (notificationHeight - iconSize) / 2.0f;

            float iconGlow1 = 3.0f;
            RenderUtility.drawShadow(iconX + iconSize / 2.0f - iconGlow1 / 2.0f, iconY + iconSize / 2.0f - iconGlow1 / 2.0f, iconGlow1, iconGlow1, 8, ColorUtils.reAlphaInt(themeColorAlpha, (int)(100 * combinedAlpha)));
            float iconGlow2 = 6.0f;
            RenderUtility.drawShadow(iconX + iconSize / 2.0f - iconGlow2 / 2.0f, iconY + iconSize / 2.0f - iconGlow2 / 2.0f, iconGlow2, iconGlow2, 12, ColorUtils.reAlphaInt(themeColorAlpha, (int)(40 * combinedAlpha)));

            if (potionSprite != null) {
                drawPotionSprite(potionSprite, iconX, iconY, iconSize, combinedAlpha);
            } else if (!iconStack.isEmpty()) {
                try {
                    drawItemStack(iconStack, iconX, iconY, iconSize, combinedAlpha);
                } catch (Exception e) {
                    ClientFonts.icons_wex[18].drawString(stack, "B", iconX, iconY, themeColorAlpha);
                }
            } else {
                String fontIcon = "N";
                StyledFont fontRender = ClientFonts.icons_wex[18];
                float yOffset = -0.5f;

                String lower = this.text.toLowerCase();
                if (type == NotificationType.SPEC || type == NotificationType.POTION_WARN || type == NotificationType.POTION_END || type == NotificationType.ARMOR_WARN || lower.startsWith("\u043c\u0430\u043b\u043e \u0445\u043f: ")) {
                    fontIcon = "m";
                    fontRender = ClientFonts.iconsnew[12];
                } else if (lower.contains("включен") || lower.contains("enabled")) {
                    fontIcon = "K";
                } else if (lower.contains("выключен") || lower.contains("disabled")) {
                    fontIcon = "I";
                } else if (lower.contains("config") || lower.contains("loaded")) {
                    fontIcon = "s";
                } else if (lower.contains("default") || lower.contains("уведомлен")) {
                    fontIcon = "b";
                } else if (lower.contains("до следующего ивента")) {
                    fontIcon = "L";
                    fontRender = ClientFonts.icons_wex[20];
                    yOffset = 0.0f;
                } else if (lower.contains("свапнул на")) {
                    fontIcon = "j";
                }

                fontRender.drawCenteredString(stack, fontIcon, currentX + iconArea / 2.0f, YStart + notificationHeight / 2.0f + yOffset, themeColorAlpha);
            }
            currentX += iconArea;

            int sepAndSlashColor = ColorUtils.rgba(50, 50, 58, (int)(200 * combinedAlpha));

            Fonts.sfuy.drawText(stack, "/", currentX + (slashArea - Fonts.sfuy.getWidth("/", fontSize)) / 2.0f, YStart + (notificationHeight - fontSize) / 2.0f + 0.5f, sepAndSlashColor, fontSize);
            currentX += slashArea;

            float textY = YStart + (notificationHeight - fontSize) / 2.0f + 0.5f;
            for (TextPart p : parts) {
                if (!p.text.isEmpty()) {
                    if (p.isThemed) {
                        float tw = Fonts.sfuy.getWidth(p.text, fontSize);
                        float th = fontSize;
                        float textGlow1 = tw + 4.0f;
                        float textGlowH1 = th + 2.0f;
                        RenderUtility.drawShadow(currentX + tw / 2.0f - textGlow1 / 2.0f, textY + th / 2.0f - textGlowH1 / 2.0f, textGlow1, textGlowH1, 10, ColorUtils.reAlphaInt(themeColorAlpha, (int)(55 * combinedAlpha)));
                        float textGlow2 = tw + 8.0f;
                        float textGlowH2 = th + 5.0f;
                        RenderUtility.drawShadow(currentX + tw / 2.0f - textGlow2 / 2.0f, textY + th / 2.0f - textGlowH2 / 2.0f, textGlow2, textGlowH2, 16, ColorUtils.reAlphaInt(themeColorAlpha, (int)(22 * combinedAlpha)));
                    }
                    Fonts.sfuy.drawText(stack, p.text, currentX, textY, p.color, fontSize);
                    currentX += Fonts.sfuy.getWidth(p.text, fontSize);
                }
            }

            currentX += vertSepGap;

            float sepInset = 2.5f;
            RenderUtility.drawRoundedRect(currentX, YStart + sepInset, vertSepWidth, notificationHeight - sepInset * 2.0f, new Vector4f(0.5f, 0.5f, 0.5f, 0.5f), sepAndSlashColor);
            currentX += vertSepWidth + vertSepGap;

            float circleRadius = 2.2f;
            float circleX = currentX + circleArea / 2.0f;
            float circleY = YStart + notificationHeight / 2.0f;

            float progress = 1.0f - Math.max(0.0f, Math.min(1.0f, (float)(System.currentTimeMillis() - this.time) / (this.time2 * 1000.0f)));

            int themeR = (themeColorAlpha >> 16) & 0xFF;
            int themeG = (themeColorAlpha >> 8) & 0xFF;
            int themeB = themeColorAlpha & 0xFF;

            float glowR = themeR / 255.0f;
            float glowG = themeG / 255.0f;
            float glowB = themeB / 255.0f;

            float circleGlow1 = circleRadius * 2.0f;
            RenderUtility.drawShadow(circleX - circleGlow1 / 2.0f, circleY - circleGlow1 / 2.0f, circleGlow1, circleGlow1, 8, ColorUtils.reAlphaInt(themeColorAlpha, (int)(110 * combinedAlpha * progress)));
            float circleGlow2 = circleRadius * 4.0f;
            RenderUtility.drawShadow(circleX - circleGlow2 / 2.0f, circleY - circleGlow2 / 2.0f, circleGlow2, circleGlow2, 14, ColorUtils.reAlphaInt(themeColorAlpha, (int)(45 * combinedAlpha * progress)));
            float circleGlow3 = circleRadius * 6.5f;
            RenderUtility.drawShadow(circleX - circleGlow3 / 2.0f, circleY - circleGlow3 / 2.0f, circleGlow3, circleGlow3, 20, ColorUtils.reAlphaInt(themeColorAlpha, (int)(20 * combinedAlpha * progress)));

            RenderSystem.pushMatrix();
            RenderSystem.enableBlend();
            RenderSystem.disableTexture();
            RenderSystem.defaultBlendFunc();
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);

            GL11.glLineWidth(1.5f);
            RenderSystem.color4f(glowR * 0.2f, glowG * 0.2f, glowB * 0.2f, combinedAlpha * 0.25f);
            GL11.glBegin(GL11.GL_LINE_LOOP);
            for (int i = 0; i <= 360; i += 5) {
                double angle = Math.toRadians(i);
                GL11.glVertex2d(circleX + Math.sin(angle) * circleRadius, circleY + Math.cos(angle) * circleRadius);
            }
            GL11.glEnd();

            GL11.glLineWidth(1.5f);

            int totalSegments = (int)(360 * progress);
            if (totalSegments > 0) {
                GL11.glBegin(GL11.GL_LINE_STRIP);
                for (int i = 0; i <= totalSegments; i += 3) {
                    float segProgress = (float) i / 360.0f;
                    float alphaGrad = 0.5f + 0.5f * segProgress;
                    RenderSystem.color4f(glowR, glowG, glowB, combinedAlpha * alphaGrad);
                    double angle = Math.toRadians(i - 90);
                    GL11.glVertex2d(circleX + Math.cos(angle) * circleRadius, circleY + Math.sin(angle) * circleRadius);
                }
                GL11.glEnd();

                double endAngle = Math.toRadians(totalSegments - 90);
                float dotX = (float)(circleX + Math.cos(endAngle) * circleRadius);
                float dotY = (float)(circleY + Math.sin(endAngle) * circleRadius);
                float dotRadius = 0.7f;

                RenderSystem.color4f(1.0f, 1.0f, 1.0f, combinedAlpha * 0.95f);
                GL11.glBegin(GL11.GL_TRIANGLE_FAN);
                GL11.glVertex2d(dotX, dotY);
                for (int i = 0; i <= 360; i += 15) {
                    double a = Math.toRadians(i);
                    GL11.glVertex2d(dotX + Math.cos(a) * dotRadius, dotY + Math.sin(a) * dotRadius);
                }
                GL11.glEnd();

                float dotGlow1 = 2.5f;
                GL11.glBegin(GL11.GL_TRIANGLE_FAN);
                RenderSystem.color4f(glowR, glowG, glowB, combinedAlpha * 0.35f);
                GL11.glVertex2d(dotX, dotY);
                RenderSystem.color4f(glowR, glowG, glowB, 0.0f);
                for (int i = 0; i <= 360; i += 15) {
                    double a = Math.toRadians(i);
                    GL11.glVertex2d(dotX + Math.cos(a) * dotGlow1, dotY + Math.sin(a) * dotGlow1);
                }
                GL11.glEnd();

                float dotGlow2 = 4.5f;
                GL11.glBegin(GL11.GL_TRIANGLE_FAN);
                RenderSystem.color4f(glowR, glowG, glowB, combinedAlpha * 0.12f);
                GL11.glVertex2d(dotX, dotY);
                RenderSystem.color4f(glowR, glowG, glowB, 0.0f);
                for (int i = 0; i <= 360; i += 15) {
                    double a = Math.toRadians(i);
                    GL11.glVertex2d(dotX + Math.cos(a) * dotGlow2, dotY + Math.sin(a) * dotGlow2);
                }
                GL11.glEnd();
            }

            GL11.glDisable(GL11.GL_LINE_SMOOTH);
            RenderSystem.enableTexture();
            RenderSystem.popMatrix();

            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glLineWidth(1.0f);

            mc.gameRenderer.setupOverlayRendering();

            return notificationHeight + 4.0F;
        }

        private void drawItemStack(ItemStack itemStack, float x, float y, float size, float alpha) {
            if (itemStack == null || itemStack.isEmpty()) {
                return;
            }

            float scale = size / 16.0f;
            GL11.glPushMatrix();
            GL11.glTranslatef(x, y, 0.0f);
            GL11.glScalef(scale, scale, 1.0f);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.color4f(1.0f, 1.0f, 1.0f, alpha);
            mc.getItemRenderer().renderItemIntoGUI(itemStack, 0, 0);
            RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
            GL11.glPopMatrix();
        }

        private void drawPotionSprite(TextureAtlasSprite sprite, float x, float y, float size, float alpha) {
            if (sprite == null) return;
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.color4f(1.0f, 1.0f, 1.0f, alpha);
            mc.getTextureManager().bindTexture(sprite.getAtlasTexture().getTextureLocation());
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferBuilder = tessellator.getBuffer();
            bufferBuilder.begin(7, DefaultVertexFormats.POSITION_TEX);
            bufferBuilder.pos(x, y + size, 0).tex(sprite.getMinU(), sprite.getMaxV()).endVertex();
            bufferBuilder.pos(x + size, y + size, 0).tex(sprite.getMaxU(), sprite.getMaxV()).endVertex();
            bufferBuilder.pos(x + size, y, 0).tex(sprite.getMaxU(), sprite.getMinV()).endVertex();
            bufferBuilder.pos(x, y, 0).tex(sprite.getMinU(), sprite.getMinV()).endVertex();
            tessellator.draw();
            RenderSystem.disableBlend();
            RenderSystem.color4f(1.0f, 1.0f, 1.0f, 1.0f);
        }

        public float getX() {
            return this.x;
        }

        public float getY() {
            return this.y;
        }

        public void setX(float x) {
            this.x = x;
        }

        public void setY(float y) {
            this.y = y;
        }

        public String getText() {
            return this.text;
        }

        public String getContent() {
            return this.content;
        }

        public long getTime() {
            return this.time;
        }
    }
}
