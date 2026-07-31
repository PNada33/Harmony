package xd.harm.ui.display.impl.hud2;

import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.MainWindow;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector4f;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.GameType;
import org.lwjgl.opengl.GL11;
import xd.harm.config.StaffStorage;
import xd.harm.events.render.EventDisplay;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.impl.render.Theme;
import xd.harm.ui.display.ElementRenderer;
import xd.harm.ui.display.ElementUpdater;
import xd.harm.utils.animations.Animation;
import xd.harm.utils.animations.Direction;
import xd.harm.utils.animations.easing.CompactAnimation;
import xd.harm.utils.animations.easing.Easing;
import xd.harm.utils.animations.impl.DecelerateAnimation;
import xd.harm.utils.drag.Dragging;
import xd.harm.utils.render.KawaseBlur;
import xd.harm.utils.render.rect.RenderUtility;
import xd.harm.utils.render.color.ColorUtils;
import xd.harm.utils.render.font.Fonts;
import xd.harm.utils.render.gl.Stencil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class StaffListRenderer2 implements ElementRenderer, ElementUpdater {

    final Dragging dragging;
    final Pattern namePattern = Pattern.compile("^\\w{3,16}$");
    final Pattern prefixMatches = Pattern.compile(".*(mod|der|adm|help|wne|С…РµР»РїРµСЂ|СЃС‚Р°Р¶|РјРѕРґРµСЂ|СЃС‚Р°Р¶Рµ|СЃС‚Р°Р¶С‘СЂ|СЃС‚Р°Р¶РµСЂ|Р°РґРј|РїРѕРґРґРµСЂР¶РєР°|РєСѓСЂР°|own|taf|curat|dev|supp|yt|СЃРѕС‚СЂСѓРґ).*");

    final CompactAnimation widthAnimation = new CompactAnimation(Easing.EASE_OUT_QUART, 100);
    final Animation alphaAnimation = new DecelerateAnimation(300, 1, Direction.FORWARDS);

    final List<Staff> staffPlayers = new ArrayList<>();
    final List<ScorePlayerTeam> sortedTeams = new ArrayList<>();
    float currentWidth;
    float targetWidth;
    float currentHeight;
    float targetHeight;
    boolean sizeInitialized = false;

    float animatedX = -1;
    float animatedY = -1;
    float dragSpeed = 0.15f;

    boolean isFirstRender = true;
    long lastUpdateTime = -1;

    final Map<String, StaffAppearAnim> appearAnimations = new HashMap<>();

    private static final ResourceLocation STEVE_SKIN = new ResourceLocation("textures/entity/steve.png");

    private static class StaffAppearAnim {
        Boolean lastState = null;
        float fadeAlpha = 0f;

        void update(boolean isActive, float delta) {
            float d60 = delta * 60f;
            if (isActive) {
                fadeAlpha += (1f - fadeAlpha) * (1f - (float) Math.pow(1f - 0.15f, d60));
            } else {
                fadeAlpha += (0f - fadeAlpha) * (1f - (float) Math.pow(1f - 0.15f, d60));
            }
        }

        void checkAndUpdate(boolean currentState, float delta) {
            if (lastState == null) {
                lastState = false;
            }
            lastState = currentState;
            update(currentState, delta);
        }
    }

    private StaffAppearAnim getOrCreateAnim(String staffName) {
        return appearAnimations.computeIfAbsent(staffName, k -> new StaffAppearAnim());
    }

    private void renderBlurMask(Runnable draw) {
        GL11.glEnable(0x809D);
        GL11.glEnable(0x809E);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.01f);
        draw.run();
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(0x809E);
        GL11.glDisable(0x809D);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1f);
    }

    @Override
    public void update(EventUpdate e) {
        staffPlayers.clear();

        if (mc.world == null || mc.world.getScoreboard() == null || mc.getConnection() == null) {
            return;
        }

        sortedTeams.clear();
        sortedTeams.addAll(mc.world.getScoreboard().getTeams());
        sortedTeams.sort(Comparator.comparing(Team::getName));

        for (ScorePlayerTeam team : sortedTeams) {
            String name = team.getMembershipCollection().toString().replaceAll("[\\[\\]]", "");
            boolean vanish = true;
            for (NetworkPlayerInfo info : mc.getConnection().getPlayerInfoMap()) {
                if (info.getGameProfile().getName().equals(name)) {
                    vanish = false;
                }
            }
            if (namePattern.matcher(name).matches() && !name.equals(mc.player.getName().getString())) {
                String prefixText = team.getPrefix().getString();

                if (!vanish) {
                    if (prefixMatches.matcher(prefixText.toLowerCase(Locale.ROOT)).matches() || StaffStorage.isStaff(name)) {
                        Staff staff = new Staff(team.getPrefix(), name, false, Status.NONE);
                        staffPlayers.add(staff);
                    }
                }
                if (vanish && !prefixText.isEmpty()) {
                    if (prefixMatches.matcher(prefixText.toLowerCase(Locale.ROOT)).matches() || StaffStorage.isStaff(name)) {
                        Staff staff = new Staff(team.getPrefix(), name, true, Status.VANISHED);
                        staffPlayers.add(staff);
                    }
                }
            }
        }
    }

    @Override
    public void render(EventDisplay eventDisplay) {
        MatrixStack ms = eventDisplay.getMatrixStack();

        long currentTime = System.currentTimeMillis();
        if (lastUpdateTime == -1) {
            lastUpdateTime = currentTime;
        }
        float deltaMs = currentTime - lastUpdateTime;
        lastUpdateTime = currentTime;
        float delta = deltaMs / 16.666f;
        delta = Math.max(0.001f, Math.min(delta, 5f));

        if (isFirstRender) {
            animatedX = dragging.getX();
            animatedY = dragging.getY();
            isFirstRender = false;
        }

        float targetX = dragging.getX();
        float targetY = dragging.getY();

        if (dragging.isDragging()) {
            animatedX = targetX;
            animatedY = targetY;
        } else if (Math.abs(targetX - animatedX) > 0.01f || Math.abs(targetY - animatedY) > 0.01f) {
            float lerpFactor = 1f - (float) Math.pow(1f - dragSpeed, delta);
            animatedX += (targetX - animatedX) * lerpFactor;
            animatedY += (targetY - animatedY) * lerpFactor;
            if (Math.abs(targetX - animatedX) < 0.1f) animatedX = targetX;
            if (Math.abs(targetY - animatedY) < 0.1f) animatedY = targetY;
        }

        float posX = animatedX + dragging.getWobbleX();
        float posY = animatedY + dragging.getWobbleY();
        float grabScale = dragging.getGrabScale();
        float grabRot = dragging.getWobbleAngle();

        float headerHeight = 16f;
        float itemHeight = 12f;
        float headerFontSize = 6.5f;
        float itemFontSize = 5.5f;
        float padding = 5f;
        float radius = 3.5f;
        float headSize = 7f;
        float headPadding = 2.5f;
        float statusDotSize = 4f;

        boolean isAnyStaffActive = false;
        boolean isChatOpen = mc.currentScreen instanceof ChatScreen;

        for (Staff f : staffPlayers) {
            StaffAppearAnim anim = getOrCreateAnim(f.getName());
            anim.checkAndUpdate(true, delta);
        }

        for (Map.Entry<String, StaffAppearAnim> entry : appearAnimations.entrySet()) {
            boolean stillActive = false;
            for (Staff f : staffPlayers) {
                if (f.getName().equals(entry.getKey())) {
                    stillActive = true;
                    break;
                }
            }
            if (!stillActive) {
                entry.getValue().checkAndUpdate(false, delta);
            }
        }

        float totalStaffHeight = 0;
        int staffIndex = 0;
        for (Staff f : staffPlayers) {
            StaffAppearAnim anim = getOrCreateAnim(f.getName());
            if (anim.fadeAlpha > 0.01f) {
                isAnyStaffActive = true;
                totalStaffHeight += itemHeight * anim.fadeAlpha;
                staffIndex++;
            }
        }

        for (Map.Entry<String, StaffAppearAnim> entry : appearAnimations.entrySet()) {
            StaffAppearAnim anim = entry.getValue();
            if (anim.fadeAlpha > 0.01f && anim.lastState != null && !anim.lastState) {
                isAnyStaffActive = true;
                totalStaffHeight += itemHeight * anim.fadeAlpha;
            }
        }

        isAnyStaffActive = isAnyStaffActive || isChatOpen;

        Direction targetDirection = isAnyStaffActive ? Direction.FORWARDS : Direction.BACKWARDS;
        if (alphaAnimation.getDirection() != targetDirection) {
            alphaAnimation.setDirection(targetDirection);
            if (targetDirection == Direction.FORWARDS) {
                alphaAnimation.reset();
            }
        }
        alphaAnimation.isDone();

        float finalAlpha = (float) alphaAnimation.getOutput();
        if (finalAlpha <= 0.01) {
            sizeInitialized = false;
            return;
        }

        String headerText = "stafflist";
        float iconWidth = 8f;
        float headerTextWidth = Fonts.sfuy.getWidth(headerText, headerFontSize);
        float totalHeaderContentWidth = iconWidth + 3f + headerTextWidth;
        float minWidth = totalHeaderContentWidth + 20f;

        float maxStaffWidth = 0;
        for (Staff f : staffPlayers) {
            StaffAppearAnim anim = getOrCreateAnim(f.getName());
            if (anim.fadeAlpha > 0.01f) {
                ITextComponent prefix = f.getPrefix();
                String prefixText = prefix != null ? prefix.getString() : "";
                String staffName = f.getName();

                float prefixWidth = 0;
                if (!prefixText.isEmpty()) {
                    prefixWidth = Fonts.sfuy.getWidth(prefixText + " ", itemFontSize);
                }

                float nameWidth = Fonts.sfuy.getWidth(staffName, itemFontSize);
                float localWidth = padding + headSize + headPadding + prefixWidth + nameWidth + 8f + statusDotSize + padding;
                if (localWidth > maxStaffWidth) {
                    maxStaffWidth = localWidth;
                }
            }
        }

        targetWidth = Math.max(minWidth, maxStaffWidth);
        boolean hasActiveStaff = staffIndex > 0;
        targetHeight = headerHeight + (hasActiveStaff ? totalStaffHeight : (isChatOpen ? 0 : totalStaffHeight));

        if (!sizeInitialized) {
            currentWidth = targetWidth;
            currentHeight = targetHeight;
            sizeInitialized = true;
        } else {
            currentWidth = animateFastDelta(currentWidth, targetWidth, 8, delta);
            currentHeight = animateFastDelta(currentHeight, targetHeight, 8, delta);
        }

        boolean grab = Math.abs(grabScale - 1.0f) > 0.001f || Math.abs(grabRot) > 0.001f;

        KawaseBlur.blur.updateBlur(3.0f, 3);

        if (currentWidth > 2f && headerHeight > 2f) {
            KawaseBlur.blur.render(() -> renderBlurMask(() ->
                    RenderUtility.drawRoundedRect(posX, posY, currentWidth, currentHeight, new Vector4f(radius, radius, radius, radius), ColorUtils.rgba(255, 255, 255, 255))
            ));
        }

        if (grab) {
            GL11.glPushMatrix();
            RenderUtility.customScaledObject2D(posX, posY, currentWidth, currentHeight, grabScale);
        }

        GL11.glPushMatrix();
        GL11.glTranslatef(posX, posY, 0);
        if (Math.abs(grabRot) > 0.001f) {
            RenderUtility.customRotatedObject2D(0, 0, currentWidth, currentHeight, grabRot);
        }

        int bgAlpha = (int) (160 * finalAlpha);
        RenderUtility.drawRoundedRect(0, 0, currentWidth, currentHeight, new Vector4f(radius, radius, radius, radius), ColorUtils.rgba(25, 25, 30, bgAlpha));

        if (currentHeight > headerHeight && hasActiveStaff) {
            int bodyAlpha = (int) (120 * finalAlpha);
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            MainWindow window = mc.getMainWindow();
            double scale = window.getGuiScaleFactor();
            int scissorX = (int) (posX * scale);
            int scissorY = (int) ((window.getScaledHeight() - posY - currentHeight) * scale);
            int scissorW = (int) (currentWidth * scale);
            int scissorH = (int) ((currentHeight - headerHeight) * scale);
            GL11.glScissor(scissorX, scissorY, Math.max(1, scissorW), Math.max(1, scissorH));
            RenderUtility.drawRoundedRect(0, 0, currentWidth, currentHeight, new Vector4f(radius, radius, radius, radius), ColorUtils.rgba(0, 0, 0, bodyAlpha));
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        }

        RenderUtility.drawRoundedRectOutline(0, 0, currentWidth, currentHeight, radius, 0.5f, ColorUtils.rgba(255, 255, 255, (int) (30 * finalAlpha)));

        int textColor = ColorUtils.rgba(255, 255, 255, (int) (255 * finalAlpha));
        float headerContentX = (currentWidth - totalHeaderContentWidth) / 2f;

        Fonts.ico.drawText(ms, "k", headerContentX, (headerHeight - 7.5f) / 2 + 0.5f, ColorUtils.setAlpha(Theme.MainColor(0), (int) (255 * finalAlpha)), 8.5f);
        Fonts.sfuy.drawText(ms, headerText, headerContentX + iconWidth + 3f, (headerHeight - headerFontSize) / 2f + 0.5f, textColor, headerFontSize);

        if (hasActiveStaff) {
            boolean useScissor = !grab;

            if (useScissor) {
                MainWindow window = mc.getMainWindow();
                double scale = window.getGuiScaleFactor();
                int scissorX = (int) (posX * scale);
                int scissorY = (int) ((window.getScaledHeight() - posY - currentHeight) * scale);
                int scissorW = (int) (currentWidth * scale);
                int scissorH = (int) ((currentHeight - headerHeight) * scale);
                GL11.glEnable(GL11.GL_SCISSOR_TEST);
                GL11.glScissor(scissorX, scissorY, Math.max(1, scissorW), Math.max(1, scissorH));
            }

            float staffPosY = headerHeight;

            for (Staff f : staffPlayers) {
                StaffAppearAnim anim = getOrCreateAnim(f.getName());

                if (anim.fadeAlpha <= 0.01f) continue;

                float animAlpha = anim.fadeAlpha * finalAlpha;
                if (animAlpha < 0.01f) {
                    staffPosY += itemHeight * anim.fadeAlpha;
                    continue;
                }

                ITextComponent prefix = f.getPrefix();
                String prefixText = prefix != null ? prefix.getString() : "";
                String staffName = f.getName();

                float headX = padding;
                float headY = staffPosY + (itemHeight - headSize) / 2;
                float headRadius = headSize / 3f;
                ResourceLocation skinLocation = getSkinLocation(staffName);

                Stencil.initStencilToWrite();
                RenderUtility.drawRoundedRect(headX, headY, headSize, headSize, headRadius,
                        ColorUtils.reAlphaInt(ColorUtils.rgba(22, 22, 30, 255), (int) (210 * animAlpha)));
                Stencil.readStencilBuffer(1);
                RenderUtility.drawHead(skinLocation, headX - 2f, headY - 2f, headSize, headSize, headRadius, animAlpha, 0f);
                Stencil.uninitStencilBuffer();

                float textX = headX + headSize + headPadding;
                float textY = staffPosY + (itemHeight - itemFontSize) / 2f;

                if (prefix != null && !prefixText.isEmpty()) {
                    if (!prefix.getSiblings().isEmpty()) {
                        for (ITextComponent part : prefix.getSiblings()) {
                            String partText = part.getString();
                            if (!partText.isEmpty()) {
                                Color color = part.getStyle().getColor();
                                int colorInt = color != null ? color.getColor() : ColorUtils.rgb(255, 255, 255);
                                colorInt = ColorUtils.setAlpha(colorInt, (int) (255 * animAlpha));
                                Fonts.sfuy.drawText(ms, partText, textX, textY, colorInt, itemFontSize);
                                textX += Fonts.sfuy.getWidth(partText, itemFontSize);
                            }
                        }
                    } else {
                        Color color = prefix.getStyle().getColor();
                        int colorInt = color != null ? color.getColor() : ColorUtils.rgb(255, 255, 255);
                        colorInt = ColorUtils.setAlpha(colorInt, (int) (255 * animAlpha));
                        Fonts.sfuy.drawText(ms, prefixText, textX, textY, colorInt, itemFontSize);
                        textX += Fonts.sfuy.getWidth(prefixText, itemFontSize);
                    }
                    textX += Fonts.sfuy.getWidth(" ", itemFontSize);
                }

                int nameColor = ColorUtils.rgba(255, 255, 255, (int) (255 * animAlpha));
                Fonts.sfuy.drawText(ms, staffName, textX, textY, nameColor, itemFontSize);

                int dotColor;
                if (f.getStatus() == Status.VANISHED) {
                    dotColor = ColorUtils.rgba(255, 80, 80, (int) (255 * animAlpha));
                } else {
                    dotColor = ColorUtils.rgba(80, 255, 80, (int) (255 * animAlpha));
                    for (NetworkPlayerInfo info : mc.getConnection().getPlayerInfoMap()) {
                        if (info.getGameProfile().getName().equals(f.getName())) {
                            if (info.getGameType() == GameType.SPECTATOR) {
                                dotColor = ColorUtils.rgba(255, 200, 80, (int) (255 * animAlpha));
                            }
                            break;
                        }
                    }
                }

                float dotX = currentWidth - padding - statusDotSize;
                float dotY = staffPosY + (itemHeight - statusDotSize) / 2f;

                RenderUtility.drawShadow(dotX, dotY, statusDotSize, statusDotSize, 4, dotColor);
                RenderUtility.drawRoundedRect(dotX, dotY, statusDotSize, statusDotSize, new Vector4f(2f, 2f, 2f, 2f), dotColor);

                staffPosY += itemHeight * anim.fadeAlpha;
            }

            if (useScissor) {
                GL11.glDisable(GL11.GL_SCISSOR_TEST);
            }
        }

        GL11.glPopMatrix();

        if (grab) {
            GL11.glPopMatrix();
        }

        widthAnimation.run(targetWidth);
        currentWidth = (float) widthAnimation.getValue();
        dragging.setWidth(currentWidth);
        dragging.setHeight(currentHeight);
    }

    private ResourceLocation getSkinLocation(String playerName) {
        if (mc.getConnection() == null) {
            return STEVE_SKIN;
        }

        for (NetworkPlayerInfo info : mc.getConnection().getPlayerInfoMap()) {
            if (info.getGameProfile().getName().equals(playerName)) {
                return info.getLocationSkin();
            }
        }

        return STEVE_SKIN;
    }

    private float animateFastDelta(float current, float target, float speed, float delta) {
        float diff = target - current;
        if (Math.abs(diff) < 0.1f) return target;
        return current + diff / speed * delta;
    }

    @AllArgsConstructor
    @Data
    public static class Staff {
        ITextComponent prefix;
        String name;
        boolean isSpec;
        Status status;
    }

    public enum Status {
        NONE( -1),
        VANISHED( ColorUtils.rgb(254, 68, 68));

        public final int color;

        Status(int color) {
            this.color = color;
        }
    }
}
