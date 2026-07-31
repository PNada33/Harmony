package xd.harm.utils.drag;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import xd.harm.modules.api.Module;
import xd.harm.utils.client.ClientUtil;
import xd.harm.utils.client.Vec2i;
import xd.harm.utils.math.MathUtil;
import xd.harm.utils.render.rect.RenderUtility;
import net.minecraft.client.MainWindow;

import java.util.ArrayList;
import java.util.List;

public class Dragging {
    public static boolean alignmentEnabled = true;
    @Expose @SerializedName("x")
    private float xPos;
    @Expose @SerializedName("y")
    private float yPos;

    public float initialXVal;
    public float initialYVal;

    private float width, height;
    private float startX, startY;
    private boolean dragging;
    private float grabAnim;
    private float wobbleX;
    private float wobbleY;
    private float lastXPos;
    private float lastYPos;
    private float velX;
    private float tiltAngle;
    private float tiltVelocity;
    private float grabSideX;
    private float lastDeltaTime;
    private float dragDeltaX;
    private float dragDeltaY;
    private long grabStartTime;
    private boolean allowOffscreen;

    @Expose @SerializedName("name")
    private final String name;
    private final Module module;


    private static final float MAGNET_DISTANCE = 8f;
    private static final List<Dragging> ALL_DRAGGABLES = new ArrayList<>();


    private final List<AlignmentLine> activeAlignments = new ArrayList<>();
    private float alignmentAlpha = 0f;
    private float bestVerticalAlignmentDistance = MAGNET_DISTANCE;
    private float bestHorizontalAlignmentDistance = MAGNET_DISTANCE;
    private long lastUpdateTime;

    public Dragging(Module module, String name, float initialXVal, float initialYVal) {
        this.module = module;
        this.name = name;
        this.xPos = initialXVal;
        this.yPos = initialYVal;
        this.initialXVal = initialXVal;
        this.initialYVal = initialYVal;
        this.lastUpdateTime = System.currentTimeMillis();
        this.lastXPos = this.xPos;
        this.lastYPos = this.yPos;

        ALL_DRAGGABLES.add(this);
        DragManager.applyLoadedPosition(this);
    }

    public void onDraw(int mouseX, int mouseY, MainWindow res) {
        Vec2i mouse = ClientUtil.getMouse(mouseX, mouseY);
        mouseX = mouse.getX();
        mouseY = mouse.getY();

        if (!alignmentEnabled) {
            alignmentAlpha = 0.0f;
            activeAlignments.clear();
        }

        if (dragging) {
            float newX = mouseX - startX;
            float newY = mouseY - startY;
            float maxX = res.getScaledWidth() - width;
            float maxY = res.getScaledHeight() - height;

            if (net.minecraft.client.gui.screen.ChatScreen.correctPosition) {
                float hotbarOffset = net.minecraft.client.gui.screen.ChatScreen.getHotbarAnimationOutput() * 15f;
                maxY -= hotbarOffset;
            }

            activeAlignments.clear();
            bestVerticalAlignmentDistance = MAGNET_DISTANCE;
            bestHorizontalAlignmentDistance = MAGNET_DISTANCE;

            if (alignmentEnabled) {
                checkScreenAlignment(newX, newY, maxX, maxY, res);

                for (Dragging other : ALL_DRAGGABLES) {
                    if (other == this || other.module == null || !other.module.isState() || other.width <= 0.0f || other.height <= 0.0f) continue;

                    checkAlignment(newX, newY, other);
                }
            }

            if (!activeAlignments.isEmpty()) {
                for (AlignmentLine alignment : activeAlignments) {
                    if (alignment.isVertical) {
                        newX = alignment.targetPosition;
                    } else {
                        newY = alignment.targetPosition;
                    }
                }
                alignmentAlpha = Math.min(1f, alignmentAlpha + 0.1f);
            } else {
                alignmentAlpha = Math.max(0f, alignmentAlpha - 0.1f);
            }

            float dx = newX - xPos;
            float dy = newY - yPos;
            if (Math.abs(dx) < 0.25f) {
                dx = 0.0f;
            }
            if (Math.abs(dy) < 0.25f) {
                dy = 0.0f;
            }
            dragDeltaX = dx;
            dragDeltaY = dy;

            if (allowOffscreen) {
                xPos = newX;
                yPos = newY;
            } else {
                xPos = Math.max(0, Math.min(newX, maxX));
                yPos = Math.max(0, Math.min(newY, maxY));
            }
        } else {
            alignmentAlpha = Math.max(0f, alignmentAlpha - 0.05f);
            if (alignmentAlpha <= 0) {
                activeAlignments.clear();
            }
            dragDeltaX = 0.0f;
            dragDeltaY = 0.0f;
        }
        drawAlignmentLines(res);
    }

    private void checkAlignment(float newX, float newY, Dragging other) {
        float otherX = other.getX();
        float otherY = other.getY();
        float otherWidth = other.getWidth();
        float otherHeight = other.getHeight();

        offerAlignment(otherX, otherY, otherHeight, true, AlignType.EDGE, Math.abs(newX - otherX));
        offerAlignment(otherX + otherWidth - width, otherY, otherHeight, true, AlignType.EDGE, Math.abs((newX + width) - (otherX + otherWidth)));
        offerAlignment(otherX + otherWidth, otherY, otherHeight, true, AlignType.CONNECT, Math.abs(newX - (otherX + otherWidth)));
        offerAlignment(otherX - width, otherY, otherHeight, true, AlignType.CONNECT, Math.abs((newX + width) - otherX));
        offerAlignment(otherX + otherWidth / 2.0f - width / 2.0f, otherY, otherHeight, true, AlignType.CENTER, Math.abs((newX + width / 2.0f) - (otherX + otherWidth / 2.0f)));

        offerAlignment(otherY, otherX, otherWidth, false, AlignType.EDGE, Math.abs(newY - otherY));
        offerAlignment(otherY + otherHeight - height, otherX, otherWidth, false, AlignType.EDGE, Math.abs((newY + height) - (otherY + otherHeight)));
        offerAlignment(otherY + otherHeight, otherX, otherWidth, false, AlignType.CONNECT, Math.abs(newY - (otherY + otherHeight)));
        offerAlignment(otherY - height, otherX, otherWidth, false, AlignType.CONNECT, Math.abs((newY + height) - otherY));
        offerAlignment(otherY + otherHeight / 2.0f - height / 2.0f, otherX, otherWidth, false, AlignType.CENTER, Math.abs((newY + height / 2.0f) - (otherY + otherHeight / 2.0f)));
    }

    private void checkScreenAlignment(float newX, float newY, float maxX, float maxY, MainWindow res) {
        offerAlignment(0.0f, 0.0f, res.getScaledHeight(), true, AlignType.EDGE, Math.abs(newX));
        offerAlignment(maxX, 0.0f, res.getScaledHeight(), true, AlignType.EDGE, Math.abs(newX - maxX));
        offerAlignment(res.getScaledWidth() / 2.0f - width / 2.0f, 0.0f, res.getScaledHeight(), true, AlignType.CENTER, Math.abs((newX + width / 2.0f) - res.getScaledWidth() / 2.0f));

        offerAlignment(0.0f, 0.0f, res.getScaledWidth(), false, AlignType.EDGE, Math.abs(newY));
        offerAlignment(maxY, 0.0f, res.getScaledWidth(), false, AlignType.EDGE, Math.abs(newY - maxY));
        offerAlignment(res.getScaledHeight() / 2.0f - height / 2.0f, 0.0f, res.getScaledWidth(), false, AlignType.CENTER, Math.abs((newY + height / 2.0f) - res.getScaledHeight() / 2.0f));
    }

    private void offerAlignment(float targetPosition, float otherPos, float otherSize, boolean isVertical, AlignType type, float distance) {
        if (distance >= MAGNET_DISTANCE) {
            return;
        }

        if (isVertical) {
            if (distance >= bestVerticalAlignmentDistance) {
                return;
            }

            removeAlignment(true);
            bestVerticalAlignmentDistance = distance;
        } else {
            if (distance >= bestHorizontalAlignmentDistance) {
                return;
            }

            removeAlignment(false);
            bestHorizontalAlignmentDistance = distance;
        }

        activeAlignments.add(new AlignmentLine(targetPosition, otherPos, otherSize, isVertical, type));
    }

    private void removeAlignment(boolean isVertical) {
        activeAlignments.removeIf(alignment -> alignment.isVertical == isVertical);
    }

    private void drawAlignmentLines(MainWindow res) {
        if (!alignmentEnabled || alignmentAlpha <= 0 || activeAlignments.isEmpty()) return;

        for (AlignmentLine line : activeAlignments) {

            int alpha = (int)(alignmentAlpha * 150);
            int finalColor = (alpha << 24) | 0xFFFFFF;

            if (line.isVertical) {

                float x = line.targetPosition + (line.type == AlignType.EDGE ? 0 : width/2);
                if (line.type == AlignType.CONNECT) {
                    x = line.targetPosition < xPos ? line.targetPosition : line.targetPosition + width;
                }


                RenderUtility.drawRoundedRect(x - 0.5f, 0, 1, res.getScaledHeight(), 0, finalColor);
            } else {

                float y = line.targetPosition + (line.type == AlignType.EDGE ? 0 : height/2);
                if (line.type == AlignType.CONNECT) {
                    y = line.targetPosition < yPos ? line.targetPosition : line.targetPosition + height;
                }


                RenderUtility.drawRoundedRect(0, y - 0.5f, res.getScaledWidth(), 1, 0, finalColor);
            }
        }
    }

    private void tick() {
        long currentTime = System.currentTimeMillis();
        if (currentTime == lastUpdateTime) {
            return;
        }
        float deltaTime = (currentTime - lastUpdateTime) / 1000f;
        lastUpdateTime = currentTime;
        lastDeltaTime = deltaTime;

        float dt = Math.max(deltaTime, 0.001f);
        boolean activeDrag = dragging;
        float dx = activeDrag ? dragDeltaX : 0.0f;
        float dy = activeDrag ? dragDeltaY : 0.0f;
        float speed = (float) Math.sqrt(dx * dx + dy * dy) / dt;

        float target = activeDrag ? 1.0f : 0.0f;
        float animSpeed = activeDrag ? 14.0f : 10.0f;
        float step = MathUtil.clamp(dt * animSpeed, 0.0f, 1.0f);
        grabAnim = MathUtil.lerp(grabAnim, target, step);

        float settle = MathUtil.clamp(dt * 12.0f, 0.0f, 1.0f);
        wobbleX = MathUtil.lerp(wobbleX, 0.0f, settle);
        wobbleY = MathUtil.lerp(wobbleY, 0.0f, settle);

        float targetVelX = dx / dt;
        float velStep = MathUtil.clamp(dt * 14.0f, 0.0f, 1.0f);
        velX = MathUtil.lerp(velX, targetVelX, velStep);

        if (activeDrag && grabSideX == 0.0f && width > 2.0f) {
            float center = width * 0.5f;
            grabSideX = MathUtil.clamp((startX - center) / center, -1.0f, 1.0f);
            if (grabSideX == 0.0f) {
                grabSideX = startX < center ? -1.0f : 1.0f;
            }
        }

        float frameDist = (float) Math.sqrt(dx * dx + dy * dy);
        boolean ignoreTilt = activeDrag && (currentTime - grabStartTime) < 120L;
        float motion = ignoreTilt ? 0.0f : MathUtil.clamp((frameDist - 0.15f) / 6.0f, 0.0f, 1.0f);
        motion *= motion;
        float side = grabSideX == 0.0f ? 0.0f : Math.signum(grabSideX);
        float targetTilt = activeDrag ? side * (28.0f * motion) : 0.0f;

        float stiffness = activeDrag ? 220.0f : 320.0f;
        float damping = activeDrag ? 45.0f : 70.0f;
        float accel = (targetTilt - tiltAngle) * stiffness;
        tiltVelocity += accel * dt;
        tiltVelocity *= Math.max(0.0f, 1.0f - damping * dt);
        tiltAngle += tiltVelocity * dt;

        if (ignoreTilt) {
            tiltAngle = 0.0f;
            tiltVelocity = 0.0f;
        }

        if (activeDrag && targetTilt != 0.0f && Math.signum(tiltAngle) != 0.0f
                && Math.signum(tiltAngle) != Math.signum(targetTilt)) {
            tiltAngle = targetTilt;
            tiltVelocity = 0.0f;
        }

        if (!activeDrag) {
            float snap = MathUtil.clamp(dt * 60.0f, 0.0f, 1.0f);
            tiltAngle = MathUtil.lerp(tiltAngle, 0.0f, snap);
            tiltVelocity = MathUtil.lerp(tiltVelocity, 0.0f, snap);
            if (Math.abs(tiltAngle) < 0.02f && Math.abs(tiltVelocity) < 0.02f) {
                tiltAngle = 0.0f;
                tiltVelocity = 0.0f;
            }
        }

        lastXPos = xPos;
        lastYPos = yPos;
    }

    public boolean onClick(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        Vec2i mouse = ClientUtil.getMouse((int) mouseX, (int) mouseY);

        if (MathUtil.isHovered(mouse.getX(), mouse.getY(), xPos, yPos, width, height)) {
            dragging = true;
            startX = mouse.getX() - xPos;
            startY = mouse.getY() - yPos;
            grabStartTime = System.currentTimeMillis();
            tiltAngle = 0.0f;
            tiltVelocity = 0.0f;
            velX = 0.0f;
            wobbleX = 0.0f;
            wobbleY = 0.0f;
            dragDeltaX = 0.0f;
            dragDeltaY = 0.0f;
            lastXPos = xPos;
            lastYPos = yPos;
            if (width > 1.0f) {
                float center = width * 0.5f;
                grabSideX = MathUtil.clamp((startX - center) / center, -1.0f, 1.0f);
                if (grabSideX == 0.0f) {
                    grabSideX = startX < center ? -1.0f : 1.0f;
                }
            } else {
                float center = xPos + width * 0.5f;
                grabSideX = mouse.getX() < center ? -1.0f : 1.0f;
            }
            lastUpdateTime = System.currentTimeMillis();
            return true;
        }

        return false;
    }

    public void onRelease(int button) {
        if (button == 0) {
            dragging = false;
            grabSideX = 0.0f;
        }
    }


    private enum AlignType {
        EDGE,
        CENTER,
        CONNECT
    }

    private static class AlignmentLine {
        final float targetPosition;
        final float otherPos;
        final float otherSize;
        final boolean isVertical;
        final AlignType type;

        AlignmentLine(float targetPosition, float otherPos, float otherSize, boolean isVertical, AlignType type) {
            this.targetPosition = targetPosition;
            this.otherPos = otherPos;
            this.otherSize = otherSize;
            this.isVertical = isVertical;
            this.type = type;
        }
    }

    public Module getModule() { return module; }
    public String getName() { return name; }
    public float getX() { return xPos; }
    public float getY() { return yPos; }
    public float getWidth() { return width; }
    public float getHeight() { return height; }
    public boolean isDragging() { return dragging; }
    public float getGrabScale() { tick(); return 1.0f; }
    public float getWobbleX() { tick(); return 0.0f; }
    public float getWobbleY() { tick(); return 0.0f; }
    public float getWobbleAngle() { tick(); return 0.0f; }

    public void setX(float x) { this.xPos = x; }
    public void setY(float y) { this.yPos = y; }
    public void setWidth(float width) { this.width = width; }
    public void setHeight(float height) { this.height = height; }
    public void setAllowOffscreen(boolean allowOffscreen) { this.allowOffscreen = allowOffscreen; }

    public void resetPosition() {
        this.xPos = this.initialXVal;
        this.yPos = this.initialYVal;
    }


    public void cleanup() {
        ALL_DRAGGABLES.remove(this);
    }
}
