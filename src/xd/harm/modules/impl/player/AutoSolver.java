package xd.harm.modules.impl.player;

import com.google.common.eventbus.Subscribe;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.util.ScreenShotHelper;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.ModeSetting;
import xd.harm.modules.settings.impl.SliderSetting;
import xd.harm.utils.math.TimerHelper;

@ModuleRegister(name = "AutoSolver", category = Category.Player, desc = "Automatically solves 3x3 rotation captchas")
public class AutoSolver extends Module {

    private final SliderSetting gridX = new SliderSetting("Grid X", 555, 0, 4000, 1);
    private final SliderSetting gridY = new SliderSetting("Grid Y", 142, 0, 4000, 1);
    private final SliderSetting tileSize = new SliderSetting("Tile size", 277, 20, 400, 1);
    private final SliderSetting gap = new SliderSetting("Gap", 2, 0, 20, 1);
    private final ModeSetting start = new ModeSetting("Start", "Off", "Off", "Solve");
    private final BooleanSetting debug = new BooleanSetting("Debug", false);

    private final TimerHelper timer = new TimerHelper();
    private boolean solving = false;
    private NativeImage screenshot = null;

    public AutoSolver() {
        addSettings(gridX, gridY, tileSize, gap, start, debug);
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (!start.is("Solve")) return;
        if (solving) return;
        if (!timer.hasReached(1000)) return;

        solving = true;
        try {
            solve();
        } catch (Exception e) {
            e.printStackTrace();
            print("AutoSolver error: " + e.getMessage());
        } finally {
            breakScreenshot();
            start.set("Off");
            solving = false;
            timer.reset();
        }
    }

    private void solve() throws Exception {
        int gx = gridX.getInt();
        int gy = gridY.getInt();
        int size = tileSize.getInt();
        int g = gap.getInt();
        int step = size + g;

        captureScreenshot();
        if (screenshot == null) {
            print("AutoSolver: screenshot failed");
            return;
        }

        int fbW = screenshot.getWidth();
        int fbH = screenshot.getHeight();

        int totalW = step * 3;
        int totalH = step * 3;

        if (gx < 0 || gy < 0 || gx + totalW > fbW || gy + totalH > fbH) {
            print("AutoSolver: captcha out of bounds (gx=" + gx + " gy=" + gy + " fb=" + fbW + "x" + fbH + ")");
            return;
        }

        int[][][][] tilePixels = new int[3][3][size][size];
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int originX = gx + col * step;
                int originY = gy + row * step;
                for (int ty = 0; ty < size; ty++) {
                    for (int tx = 0; tx < size; tx++) {
                        tilePixels[row][col][ty][tx] = screenshot.getPixelRGBA(originX + tx, originY + ty);
                    }
                }
            }
        }

        int[][] rots = solveRotations(tilePixels, size);

        if (debug.get()) {
            print("Solved: " + formatRots(rots));
        }

        double guiScale = mc.getMainWindow().getGuiScaleFactor();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int clicks = rots[row][col];
                if (clicks <= 0) continue;

                int cx = gx + col * step + size / 2;
                int cy = gy + row * step + size / 2;

                int guiCx = (int) (cx / guiScale);
                int guiCy = (int) (cy / guiScale);

                if (mc.currentScreen != null) {
                    for (int c = 0; c < clicks; c++) {
                        mc.currentScreen.mouseClicked(guiCx, guiCy, 0);
                        if (mc.currentScreen == null) break;
                    }
                }
            }
        }
    }

    private void captureScreenshot() {
        breakScreenshot();
        try {
            Framebuffer fb = mc.getFramebuffer();
            screenshot = ScreenShotHelper.createScreenshot(fb.framebufferWidth, fb.framebufferHeight, fb);
        } catch (Exception e) {
            e.printStackTrace();
            screenshot = null;
        }
    }

    private void breakScreenshot() {
        if (screenshot != null) {
            screenshot.close();
            screenshot = null;
        }
    }

    private int[][] solveRotations(int[][][][] tilePixels, int size) {
        int[][] rots = new int[3][3];

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                if (row == 0 && col == 0) {
                    rots[row][col] = 0;
                    continue;
                }

                int[][] neighbor;
                int neighborRot;
                boolean isHorizontal;

                if (col > 0) {
                    neighbor = tilePixels[row][col - 1];
                    neighborRot = rots[row][col - 1];
                    isHorizontal = true;
                } else {
                    neighbor = tilePixels[row - 1][col];
                    neighborRot = rots[row - 1][col];
                    isHorizontal = false;
                }

                rots[row][col] = findBestRotation(tilePixels[row][col], neighbor, neighborRot, isHorizontal, size);
            }
        }

        return rots;
    }

    private int findBestRotation(int[][] tile, int[][] neighbor, int neighborRot, boolean horizontal, int size) {
        int bestRot = 0;
        double bestScore = Double.MAX_VALUE;

        for (int rot = 0; rot < 4; rot++) {
            double score = edgeScore(neighbor, tile, neighborRot, rot, horizontal, size);
            if (score < bestScore) {
                bestScore = score;
                bestRot = rot;
            }
        }

        return bestRot;
    }

    private double edgeScore(int[][] a, int[][] b, int rotA, int rotB, boolean horizontal, int size) {
        double totalDiff = 0;
        int count = 0;

        for (int i = 0; i < size; i++) {
            int aPx = getEdgePixel(a, rotA, horizontal, i, size, true);
            int bPx = getEdgePixel(b, rotB, horizontal, i, size, false);
            totalDiff += colorDiff(aPx, bPx);
            count++;
        }

        return count > 0 ? totalDiff / count : Double.MAX_VALUE;
    }

    private int getEdgePixel(int[][] img, int rot, boolean horizontal, int idx, int size, boolean isLeftOrTop) {
        int x, y;
        switch (rot % 4) {
            case 0:
                if (horizontal) {
                    x = isLeftOrTop ? size - 1 : 0;
                    y = idx;
                } else {
                    x = idx;
                    y = isLeftOrTop ? size - 1 : 0;
                }
                break;
            case 1:
                if (horizontal) {
                    x = isLeftOrTop ? idx : size - 1 - idx;
                    y = isLeftOrTop ? 0 : size - 1;
                } else {
                    x = isLeftOrTop ? 0 : size - 1;
                    y = isLeftOrTop ? size - 1 - idx : idx;
                }
                break;
            case 2:
                if (horizontal) {
                    x = isLeftOrTop ? 0 : size - 1;
                    y = size - 1 - idx;
                } else {
                    x = size - 1 - idx;
                    y = isLeftOrTop ? 0 : size - 1;
                }
                break;
            case 3:
                if (horizontal) {
                    x = isLeftOrTop ? size - 1 - idx : idx;
                    y = isLeftOrTop ? size - 1 : 0;
                } else {
                    x = isLeftOrTop ? size - 1 : 0;
                    y = isLeftOrTop ? idx : size - 1 - idx;
                }
                break;
            default:
                x = 0; y = 0;
        }
        return img[y][x];
    }

    private double colorDiff(int argb1, int argb2) {
        int r1 = (argb1 >> 16) & 0xFF, g1 = (argb1 >> 8) & 0xFF, b1 = argb1 & 0xFF;
        int r2 = (argb2 >> 16) & 0xFF, g2 = (argb2 >> 8) & 0xFF, b2 = argb2 & 0xFF;
        return Math.abs(r1 - r2) + Math.abs(g1 - g2) + Math.abs(b1 - b2);
    }

    private String formatRots(int[][] rots) {
        StringBuilder sb = new StringBuilder();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                sb.append(rots[row][col]).append(col < 2 ? " " : "");
            }
            sb.append(" | ");
        }
        return sb.toString();
    }

    @Override
    public boolean onEnable() {
        timer.reset();
        return super.onEnable();
    }

    @Override
    public boolean onDisable() {
        breakScreenshot();
        return super.onDisable();
    }
}
