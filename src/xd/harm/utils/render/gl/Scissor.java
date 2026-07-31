package xd.harm.utils.render.gl;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;

public class Scissor {
    private static class State {
        public boolean enabled;
        public int transX;
        public int transY;
        public int x;
        public int y;
        public int width;
        public int height;

        public State() {
        }

        public State(State other) {
            this.enabled = other.enabled;
            this.transX = other.transX;
            this.transY = other.transY;
            this.x = other.x;
            this.y = other.y;
            this.width = other.width;
            this.height = other.height;
        }
    }

    private static State state = new State();

    private static final List<State> stateStack = new ArrayList<>();

    public static void push() {
        stateStack.add(new State(state));
        GL11.glPushAttrib(GL11.GL_SCISSOR_BIT);
    }

    public static void pop() {
        state = stateStack.remove(stateStack.size() - 1);
        GL11.glPopAttrib();
    }

    public static void unset() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        state.enabled = false;
    }

    public static void setFromComponentCoordinates(int x, int y, int width, int height) {
        setFromComponentCoordinates((double) x, (double) y, (double) width, (double) height);
    }

    public static void setFromComponentCoordinates(double x, double y, double width, double height) {
        MainWindow res = Minecraft.getInstance().getMainWindow();
        double scaleFactor = res.getGuiScaleFactor();
        int padding = 1;

        int screenX = (int) Math.floor(x * scaleFactor) - padding;
        int screenY = (int) Math.floor((res.getScaledHeight() - (y + height)) * scaleFactor) - padding;
        int screenWidth = (int) Math.ceil(width * scaleFactor) + padding * 2;
        int screenHeight = (int) Math.ceil(height * scaleFactor) + padding * 2;
        set(screenX, screenY, screenWidth, screenHeight);
    }
    public static void scissor(MainWindow window, double x, double y, double width, double height) {
        if (x + width == x || y + height == y || x < 0 || y + height < 0) return;
        final double scaleFactor = window.getScaleFactor();
        final int scaledHeight = window.getScaledHeight();
        GL11.glScissor((int) Math.round(x * scaleFactor), (int) Math.round((scaledHeight - (y + height)) * scaleFactor), (int) Math.round(width * scaleFactor), (int) Math.round(height * scaleFactor));
    }

    public static void setFromComponentCoordinates(double x, double y, double width, double height, float scale) {
        MainWindow res = Minecraft.getInstance().getMainWindow();

        float animationValue = scale;

        float halfAnimationValueRest = (1 - animationValue) / 2f;
        double testX = x + (width * halfAnimationValueRest);
        double testY = y + (height * halfAnimationValueRest);
        double testW = width * animationValue;
        double testH = height * animationValue;

        testX = testX * animationValue + ((res.getScaledWidth() - testW) *
                halfAnimationValueRest);

        double scaleFactor = res.getGuiScaleFactor();

        int padding = 1;

        int screenX = (int) Math.floor(testX * scaleFactor) - padding;
        int screenY = (int) Math.floor((res.getScaledHeight() - (testY + testH)) * scaleFactor) - padding;
        int screenWidth = (int) Math.ceil(testW * scaleFactor) + padding * 2;
        int screenHeight = (int) Math.ceil(testH * scaleFactor) + padding * 2;
        set(screenX, screenY, screenWidth, screenHeight);
    }

    public static void set(int x, int y, int width, int height) {
        MainWindow window = Minecraft.getInstance().getMainWindow();
        int screenWidth = window.getWidth();
        int screenHeight = window.getHeight();

        int currentX = state.enabled ? state.x : 0;
        int currentY = state.enabled ? state.y : 0;
        int currentRight = currentX + (state.enabled ? state.width : screenWidth);
        int currentBottom = currentY + (state.enabled ? state.height : screenHeight);

        int targetX = x + state.transX;
        int targetY = y + state.transY;
        int targetRight = targetX + width;
        int targetBottom = targetY + height;

        int resultX = Math.max(Math.max(currentX, targetX), 0);
        int resultY = Math.max(Math.max(currentY, targetY), 0);
        int resultRight = Math.min(Math.min(currentRight, targetRight), screenWidth);
        int resultBottom = Math.min(Math.min(currentBottom, targetBottom), screenHeight);
        int resultWidth = Math.max(0, resultRight - resultX);
        int resultHeight = Math.max(0, resultBottom - resultY);

        state.enabled = true;
        state.x = resultX;
        state.y = resultY;
        state.width = resultWidth;
        state.height = resultHeight;
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(resultX, resultY, resultWidth, resultHeight);
    }

    public static void translate(int x, int y) {
        state.transX = x;
        state.transY = y;
    }

    public static void translateFromComponentCoordinates(int x, int y) {
        MainWindow res = Minecraft.getInstance().getMainWindow();
        int totalHeight = res.getScaledHeight();
        double scaleFactor = res.getGuiScaleFactor();

        int screenX = (int) Math.floor(x * scaleFactor);
        int screenY = (int) Math.floor((totalHeight - y) * scaleFactor);
        translate(screenX, screenY);
    }

}
