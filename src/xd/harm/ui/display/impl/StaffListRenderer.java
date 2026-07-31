package xd.harm.ui.display.impl;

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
import xd.harm.utils.text.GradientUtil;
import xd.harm.utils.text.font.ClientFonts;
import xd.harm.utils.math.Vector4i;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class StaffListRenderer implements ElementRenderer, ElementUpdater {

    final Dragging dragging;
    final Pattern namePattern = Pattern.compile("^\\w{3,16}$");
    final Pattern prefixMatches = Pattern.compile(".*(mod|der|adm|help|wne|С…РµР»РїРµСЂ|СЃС‚Р°Р¶|РјРѕРґРµСЂ|СЃС‚Р°Р¶Рµ|СЃС‚Р°Р¶С‘СЂ|СЃС‚Р°Р¶РµСЂ|Р°РґРј|РїРѕРґРґРµСЂР¶РєР°|РєСѓСЂР°|own|taf|curat|dev|supp|yt|СЃРѕС‚СЂСѓРґ).*");

    private final CompactAnimation widthAnimation = new CompactAnimation(Easing.EASE_OUT_QUART, 100);
    final Animation alphaAnimation = new DecelerateAnimation(300, 1, Direction.FORWARDS);

    final List<Staff> staffPlayers = new ArrayList<>();
    final List<ScorePlayerTeam> sortedTeams = new ArrayList<>();
    float currentWidth;
    float targetWidth;
    float currentHeight;
    float targetHeight;
    boolean sizeInitialized = false;

    float[] colorShiftOffsets = new float[4];
    float[] colorShiftSpeeds = {0.00025f, 0.00018f, 0.00022f, 0.00015f};

    float animatedX = -1;
    float animatedY = -1;
    float dragSpeed = 0.15f;

    float[] staffAnimValues = new float[100];
    Animation[] nameTextAnimations = new Animation[100];
    Animation[] iconAnimations = new Animation[100];
    boolean[] staffWasActive = new boolean[100];
    boolean[] animationsInitialized = new boolean[100];
    boolean isFirstRender = true;
    long lastUpdateTime = -1;

    private final Map<String, StaffAppearAnim> appearAnimations = new HashMap<>();

    private static final ResourceLocation STEVE_SKIN = new ResourceLocation("textures/entity/steve.png");

    private static class StaffAppearAnim {
        Boolean lastState = null;
        float fadeAlpha = 0f;
        float scaleY = 0f;
        float slideX = -20f;

        void reset() {
            fadeAlpha = 0f;
            scaleY = 0f;
            slideX = -20f;
        }

        void update(boolean isActive, float delta) {
            float d60 = delta * 60f;
            if (isActive) {
                fadeAlpha += (1f - fadeAlpha) * (1f - (float) Math.pow(1f - 0.1f, d60));
                scaleY += (1f - scaleY) * (1f - (float) Math.pow(1f - 0.15f, d60));
                slideX += (0f - slideX) * (1f - (float) Math.pow(1f - 0.12f, d60));
            } else {
                fadeAlpha += (0f - fadeAlpha) * (1f - (float) Math.pow(1f - 0.04f, d60));
                scaleY += (0f - scaleY) * (1f - (float) Math.pow(1f - 0.03f, d60));
                slideX += (-20f - slideX) * (1f - (float) Math.pow(1f - 0.035f, d60));
            }
        }

        boolean isFullyHidden() {
            return fadeAlpha < 0.01f && (lastState == null || !lastState);
        }

        void checkAndUpdate(boolean currentState, float delta) {
            if (lastState == null) {
                lastState = false;
                if (currentState) {
                    reset();
                }
            } else if (currentState && !lastState) {
                reset();
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

        float wobbleX = dragging.getWobbleX();
        float wobbleY = dragging.getWobbleY();
        float grabScale = dragging.getGrabScale();
        float grabRot = dragging.getWobbleAngle();

        float posX = animatedX + wobbleX;
        float posY = animatedY + wobbleY;

        float titleFontSize = 7f;
        float staffFontSize = 6f;
        float padding = 0.8f;
        float iconWidth = 14;
        float iconPadding = 1;
        float sidePadding = 3.5f;
        float rectHeight = 13;
        float moduleRectHeight = 11;
        float cornerRadius = 3.5f;
        float headSize = 8;
        float headPadding = 2.5f;
        float iconGap = 3.5f;

        ITextComponent name = GradientUtil.gradient("Staff's View");
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
            isAnyStaffActive = true;

            float effectiveAnim = anim.fadeAlpha;
            if (staffIndex < staffAnimValues.length) {
                staffAnimValues[staffIndex] = effectiveAnim;
            }
            staffIndex++;
            totalStaffHeight += (moduleRectHeight + padding) * effectiveAnim;
        }

        for (Map.Entry<String, StaffAppearAnim> entry : appearAnimations.entrySet()) {
            StaffAppearAnim anim = entry.getValue();
            if (anim.fadeAlpha > 0.01f && anim.lastState != null && !anim.lastState) {
                isAnyStaffActive = true;
                totalStaffHeight += (moduleRectHeight + padding) * anim.fadeAlpha;
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

        float textWidth = Fonts.sfuy.getWidth("Staff's View", titleFontSize);
        float headerWidth = iconWidth + iconPadding + textWidth + sidePadding * 2;

        float maxStaffWidth = 0;
        for (Staff f : staffPlayers) {
            ITextComponent prefix = f.getPrefix();
            String prefixText = prefix != null ? prefix.getString() : "";
            String staff = f.getName();
            float nameWidth = Fonts.sfuy.getWidth(staff, staffFontSize);

            float prefixWidth = 0;
            if (prefix != null && !prefixText.isEmpty()) {
                prefixWidth = Fonts.sfuy.getWidth(prefixText, staffFontSize);
                prefixWidth += Fonts.sfuy.getWidth(" ", staffFontSize);
            }

            float localWidth = sidePadding + headSize + headPadding + prefixWidth + nameWidth + iconGap + 7 + sidePadding;
            if (localWidth > maxStaffWidth) {
                maxStaffWidth = localWidth;
            }
        }

        targetWidth = Math.max(headerWidth, maxStaffWidth);

        boolean hasActiveStaff = !staffPlayers.isEmpty();
        targetHeight = rectHeight + (hasActiveStaff ? totalStaffHeight : (isChatOpen ? 0 : totalStaffHeight));

        if (!sizeInitialized) {
            currentWidth = targetWidth;
            currentHeight = targetHeight;
            sizeInitialized = true;
        } else {
            currentWidth = animateFastDelta(currentWidth, targetWidth, 8, delta);
            currentHeight = animateFastDelta(currentHeight, targetHeight, 8, delta);
        }

        updateColorAnimation(delta);

        int themeColor1 = Theme.MainColor(0);
        int themeColor2 = Theme.MainColor(1);

        boolean isDragging = dragging.isDragging();
        boolean grab = Math.abs(grabScale - 1.0f) > 0.001f || Math.abs(grabRot) > 0.001f;

        KawaseBlur.blur.updateBlur(3.0f, 3);

        if (currentWidth > 2f && currentHeight > 2f) {
            final float blurRadius = cornerRadius + 0.75f;
            final float blurInset = 0.35f;
            final float bpx = posX;
            final float bpy = posY;
            final float bw = currentWidth;
            final float bh = currentHeight;
            KawaseBlur.blur.render(() -> renderBlurMask(() ->
                    RenderUtility.drawRoundedRect(bpx + blurInset, bpy + blurInset,
                            bw - blurInset * 2f, bh - blurInset * 2f,
                            new Vector4f(blurRadius, blurRadius, blurRadius, blurRadius),
                            ColorUtils.rgba(255, 255, 255, 255))
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

        int wmBgAlpha = Math.max(1, (int) (150 * finalAlpha));
        RenderUtility.drawRoundedRect(0, 0, currentWidth, currentHeight,
                new Vector4f(cornerRadius, cornerRadius, cornerRadius, cornerRadius),
                ColorUtils.rgba(0, 0, 0, wmBgAlpha));

        int logoColor = ColorUtils.interpolateColor(themeColor1, ColorUtils.rgb(255, 255, 255), 0.2f);
        logoColor = ColorUtils.setAlpha(logoColor, (int) (255 * finalAlpha));
        Fonts.ico.drawText(ms, "k", sidePadding, (rectHeight - 7.5f) / 2 + 0.5f, logoColor, 8.5f);

        float xOffset = iconWidth + iconPadding;
        for (ITextComponent sibling : name.getSiblings()) {
            String charText = sibling.getString();
            Color color = sibling.getStyle().getColor();
            int colorInt = color != null ? color.getColor() : ColorUtils.rgb(255, 255, 255);
            colorInt = ColorUtils.interpolateColor(colorInt, ColorUtils.rgb(255, 255, 255), 0.15f);
            colorInt = ColorUtils.setAlpha(colorInt, (int) (255 * finalAlpha));
            Fonts.sfuy.drawText(ms, charText, xOffset, (rectHeight - titleFontSize) / 2, colorInt, titleFontSize);
            xOffset += Fonts.sfuy.getWidth(charText, titleFontSize);
        }

        if (hasActiveStaff) {
            boolean useScissor = !grab;

            if (useScissor) {
                MainWindow window = mc.getMainWindow();
                double scale = window.getGuiScaleFactor();
                int scissorX = (int) (posX * scale);
                int scissorY = (int) ((window.getScaledHeight() - posY - currentHeight) * scale);
                int scissorW = (int) (currentWidth * scale);
                int scissorH = (int) (currentHeight * scale);
                GL11.glEnable(GL11.GL_SCISSOR_TEST);
                GL11.glScissor(scissorX, scissorY, Math.max(1, scissorW), Math.max(1, scissorH));
            }

            float staffPosY = rectHeight;
            staffIndex = 0;
            for (Staff f : staffPlayers) {
                StaffAppearAnim anim = getOrCreateAnim(f.getName());

                ITextComponent prefix = f.getPrefix();
                String prefixText = prefix != null ? prefix.getString() : "";
                String staff = f.getName();

                float animValue = anim.fadeAlpha;
                boolean isStaffActive = true;

                if (nameTextAnimations[staffIndex] == null) {
                    nameTextAnimations[staffIndex] = new DecelerateAnimation(300, 1, Direction.FORWARDS);
                }
                if (iconAnimations[staffIndex] == null) {
                    iconAnimations[staffIndex] = new DecelerateAnimation(300, 1, Direction.FORWARDS);
                }

                if (!animationsInitialized[staffIndex]) {
                    if (isStaffActive) {
                        nameTextAnimations[staffIndex].timerUtil.setTime(System.currentTimeMillis() - 300);
                        iconAnimations[staffIndex].timerUtil.setTime(System.currentTimeMillis() - 300);
                        staffWasActive[staffIndex] = true;
                    }
                    animationsInitialized[staffIndex] = true;
                }

                if (isStaffActive != staffWasActive[staffIndex]) {
                    if (isStaffActive) {
                        nameTextAnimations[staffIndex].reset();
                        iconAnimations[staffIndex].reset();
                        nameTextAnimations[staffIndex].setDirection(Direction.FORWARDS);
                        iconAnimations[staffIndex].setDirection(Direction.FORWARDS);
                    } else {
                        nameTextAnimations[staffIndex].setDirection(Direction.BACKWARDS);
                        iconAnimations[staffIndex].setDirection(Direction.BACKWARDS);
                    }
                    staffWasActive[staffIndex] = isStaffActive;
                }

                nameTextAnimations[staffIndex].isDone();
                iconAnimations[staffIndex].isDone();

                float nameAnimValue = (float) nameTextAnimations[staffIndex].getOutput();
                float iconAnimValue = (float) iconAnimations[staffIndex].getOutput();

                float combinedAlpha = animValue * finalAlpha;

                if (combinedAlpha < 0.01f) {
                    staffPosY += (moduleRectHeight + padding) * animValue;
                    staffIndex++;
                    continue;
                }

                float headX = sidePadding;
                float headY = staffPosY + (moduleRectHeight - headSize) / 2;
                float headRadius = headSize / 3f;
                ResourceLocation skinLocation = getSkinLocation(staff);

                Stencil.initStencilToWrite();
                RenderUtility.drawRoundedRect(headX, headY, headSize, headSize, headRadius,
                        ColorUtils.reAlphaInt(ColorUtils.rgba(22, 22, 30, 255), (int) (210 * combinedAlpha * nameAnimValue)));
                Stencil.readStencilBuffer(1);
                RenderUtility.drawHead(skinLocation, headX - 1.5f, headY - 1.5f, headSize, headSize, headRadius, combinedAlpha * nameAnimValue, 0f);
                Stencil.uninitStencilBuffer();

                float prefixXOffset = headX + headSize + headPadding;
                float nameOffset = -35 * (1 - nameAnimValue);

                if (prefix != null && !prefixText.isEmpty()) {
                    if (!prefix.getSiblings().isEmpty()) {
                        for (ITextComponent part : prefix.getSiblings()) {
                            String partText = part.getString();
                            if (!partText.isEmpty()) {
                                Color color = part.getStyle().getColor();
                                int colorInt = color != null ? color.getColor() : ColorUtils.rgb(255, 255, 255);
                                colorInt = ColorUtils.setAlpha(colorInt, (int) (255 * combinedAlpha * nameAnimValue));
                                Fonts.sfuy.drawText(ms, partText, prefixXOffset + nameOffset, staffPosY + (moduleRectHeight - staffFontSize) / 2,
                                        colorInt, staffFontSize);
                                prefixXOffset += Fonts.sfuy.getWidth(partText, staffFontSize);
                            }
                        }
                    } else {
                        Color color = prefix.getStyle().getColor();
                        int colorInt = color != null ? color.getColor() : ColorUtils.rgb(255, 255, 255);
                        colorInt = ColorUtils.setAlpha(colorInt, (int) (255 * combinedAlpha * nameAnimValue));
                        Fonts.sfuy.drawText(ms, prefixText, prefixXOffset + nameOffset, staffPosY + (moduleRectHeight - staffFontSize) / 2,
                                colorInt, staffFontSize);
                        prefixXOffset += Fonts.sfuy.getWidth(prefixText, staffFontSize);
                    }
                    prefixXOffset += Fonts.sfuy.getWidth(" ", staffFontSize);
                }

                Fonts.sfuy.drawText(ms, staff, prefixXOffset + nameOffset, staffPosY + (moduleRectHeight - staffFontSize) / 2,
                        ColorUtils.setAlpha(-1, (int) (255 * combinedAlpha * nameAnimValue)), staffFontSize);

                float nameEndX = prefixXOffset + Fonts.sfuy.getWidth(staff, staffFontSize);

                int iconColor = ColorUtils.rgb(100, 255, 100);

                if (f.getStatus() == Status.VANISHED) {
                    iconColor = ColorUtils.rgb(255, 100, 100);
                } else {
                    for (NetworkPlayerInfo info : mc.getConnection().getPlayerInfoMap()) {
                        if (info.getGameProfile().getName().equals(f.getName())) {
                            if (info.getGameType() == GameType.SPECTATOR) {
                                iconColor = ColorUtils.rgb(255, 255, 100);
                            }
                            break;
                        }
                    }
                }

                float iconX = nameEndX + iconGap;

                iconColor = ColorUtils.setAlpha(iconColor, (int) (255 * combinedAlpha * iconAnimValue));
                ClientFonts.icons_wex[15].drawString(ms, "f", iconX, staffPosY + moduleRectHeight / 2 - 0.5f, iconColor);

                staffPosY += (moduleRectHeight + padding) * animValue;
                staffIndex++;
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

    private void drawWatermarkBackground(float x, float y, float w, float h, float r, int alpha, float finalAlpha, int themeColor1, int themeColor2) {
        alpha = Math.max(0, Math.min(255, alpha));
        finalAlpha = Math.max(0f, Math.min(1f, finalAlpha));

        int colorTopLeft = ColorUtils.rgba(8, 8, 12, alpha);
        int colorTopRight = ColorUtils.rgba(8, 8, 12, alpha);

        int colorBottomLeft = ColorUtils.interpolateColor(ColorUtils.rgba(12, 12, 18, 255), themeColor1, 0.15f);
        colorBottomLeft = ColorUtils.reAlphaInt(colorBottomLeft, alpha);

        int colorBottomRight = ColorUtils.interpolateColor(ColorUtils.rgba(12, 12, 18, 255), themeColor2, 0.15f);
        colorBottomRight = ColorUtils.reAlphaInt(colorBottomRight, alpha);

        int borderLeft = ColorUtils.reAlphaInt(themeColor1, (int) (40 * finalAlpha));
        int borderRight = ColorUtils.reAlphaInt(themeColor2, (int) (120 * finalAlpha));

        RenderUtility.drawRoundedRect(x, y, w, h, new Vector4f(r, r, r, r),
                new Vector4i(colorTopLeft, colorTopRight, colorBottomLeft, colorBottomRight));

        RenderUtility.drawGradientRoundedOutlineHorizontal(x, y, x + w, y + h, r, 0.55f, borderLeft, borderRight);
    }

    private float animateFastDelta(float current, float target, float speed, float delta) {
        float diff = target - current;
        if (Math.abs(diff) < 0.1f) return target;
        return current + diff / speed * delta;
    }

    private void updateColorAnimation(float delta) {
        for (int i = 0; i < 4; i++) {
            colorShiftOffsets[i] = (colorShiftOffsets[i] + colorShiftSpeeds[i] * delta) % 1.0f;
        }
    }

    @AllArgsConstructor
    @Data
    public static class Staff {
        ITextComponent prefix;
        String name;
        boolean isSpec;
        Status status;

        public void updateStatus() {
            if (mc.getConnection() == null || mc.getConnection().getPlayerInfoMap() == null) {
                status = Status.VANISHED;
                return;
            }

            for (NetworkPlayerInfo info : mc.getConnection().getPlayerInfoMap()) {
                if (info.getGameProfile().getName().equals(name)) {
                    if (info.getGameType() == GameType.SPECTATOR) {
                        status = Status.NONE;
                        return;
                    }
                    status = Status.NONE;
                    return;
                }
            }
            status = Status.VANISHED;
        }
    }

    public enum Status {
        NONE("", -1),
        VANISHED("V", ColorUtils.rgb(254, 68, 68));

        public final String string;
        public final int color;

        Status(String string, int color) {
            this.string = string;
            this.color = color;
        }
    }
}
