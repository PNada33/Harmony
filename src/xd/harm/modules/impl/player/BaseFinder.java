package xd.harm.modules.impl.player;

import com.google.common.eventbus.Subscribe;
import xd.harm.events.world.EventUpdate;
import xd.harm.events.input.EventKey;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.SliderSetting;
import xd.harm.modules.settings.impl.BindSetting;
import xd.harm.ui.basefinder.BaseFinderUI;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;

import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.text.SimpleDateFormat;
import java.util.Date;

@ModuleRegister(name = "BaseFinder", category = Category.Player, desc = "Ищет базы игроков")
public class BaseFinder extends Module {

    private final Minecraft mc = Minecraft.getInstance();

    private final SliderSetting height = new SliderSetting("Высота полета", 120f, 80f, 250f, 5f);
    private final SliderSetting searchRadius = new SliderSetting("Радиус поиска", 100f, 50f, 200f, 10f);
    private final SliderSetting gridSize = new SliderSetting("Размер сетки", 100f, 50f, 200f, 25f);
    private final BooleanSetting autoSprint = new BooleanSetting("Авто спринт", true);
    private final SliderSetting speed = new SliderSetting("Скорость", 0.5f, 0.1f, 2.0f, 0.1f);
    private final BindSetting guiKey = new BindSetting("Открыть GUI", -98);

    private static final int MAP_SIZE = 2500;
    private static final int MAP_CENTER = 0;
    private static final int MAP_BORDER_POS = MAP_SIZE;
    private static final int MAP_BORDER_NEG = -MAP_SIZE;

    private enum State {
        MOVING_TO_START,
        SEARCHING,
        PLAYER_FOUND,
        COMPLETED,
        PAUSED
    }

    private State currentState = State.MOVING_TO_START;
    private State previousState = State.MOVING_TO_START;
    private Vector3d targetPos;
    private long playerFoundTime = 0;
    private boolean isPaused = false;

    private int currentGridX = 0;
    private int currentGridZ = 0;
    private int totalGridsX;
    private int totalGridsZ;
    private boolean movingRight = true;
    private int completedLines = 0;

    private BaseFinderUI ui;
    private Set<String> foundPlayersNames = new HashSet<>();

    private List<String> consoleMessages = new ArrayList<>();
    private static final int MAX_CONSOLE_MESSAGES = 100;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    public BaseFinder() {
        addSettings(height, searchRadius, gridSize, autoSprint, speed, guiKey);
    }

    private void addConsoleMessage(String message) {
        String timestamp = "[" + timeFormat.format(new Date()) + "] ";
        consoleMessages.add(timestamp + message);

        if (consoleMessages.size() > MAX_CONSOLE_MESSAGES) {
            consoleMessages.remove(0);
        }

        if (ui != null) {
            ui.updateConsole(new ArrayList<>(consoleMessages));
        }
    }

    public void clearConsole() {
        consoleMessages.clear();
        if (ui != null) {
            ui.updateConsole(new ArrayList<>(consoleMessages));
        }
    }

    public List<String> getConsoleMessages() {
        return new ArrayList<>(consoleMessages);
    }

    @Subscribe
    public void onKey(EventKey e) {
        if (e.getKey() == guiKey.get()) {
            if (ui == null) {
                ui = new BaseFinderUI(this);
            }
            mc.displayGuiScreen(ui);
        }
    }

    public void pauseSearch() {
        if (!isPaused && currentState != State.COMPLETED) {
            isPaused = true;
            previousState = currentState;
            currentState = State.PAUSED;

            addConsoleMessage("§eПоиск приостановлен");

            if (mc.player != null) {
                mc.player.setMotion(0, 0, 0);
                mc.player.setSprinting(false);
            }
        }
    }

    public void resumeSearch() {
        if (isPaused) {
            isPaused = false;
            currentState = previousState;
            addConsoleMessage("§aПоиск возобновлен");
        }
    }

    public boolean isPaused() {
        return isPaused;
    }

    @Override
    public boolean onEnable() {
        super.onEnable();
        if (mc.player == null) return false;

        currentState = State.MOVING_TO_START;
        isPaused = false;
        consoleMessages.clear();

        int mapWidth = MAP_SIZE * 2;
        int gridSizeInt = (int) gridSize.get().floatValue();
        totalGridsX = (mapWidth / gridSizeInt) + 1;
        totalGridsZ = (mapWidth / gridSizeInt) + 1;

        currentGridX = 0;
        currentGridZ = 0;
        movingRight = true;
        completedLines = 0;

        double startX = MAP_BORDER_NEG;
        double startZ = MAP_BORDER_NEG;
        targetPos = new Vector3d(startX, height.get(), startZ);

        if (ui == null) {
            ui = new BaseFinderUI(this);
        }
        mc.displayGuiScreen(ui);

        addConsoleMessage(String.format("В§aBaseFinder активирован! Сетка: %dx%d точек, размер сетки: %.0f блоков",
                totalGridsX, totalGridsZ, gridSize.get()));

        return true;
    }

    @Override
    public boolean onDisable() {
        super.onDisable();
        addConsoleMessage("В§cBaseFinder отключен");

        if (mc.player != null) {
            mc.player.setSprinting(false);
        }
        if (ui != null && mc.currentScreen == ui) {
            mc.displayGuiScreen(null);
        }
        return false;
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;

        if (ui != null) {
            ui.updateProgress(getProgress(), getStatusString(), getFoundPlayersCount());
            ui.updateCurrentPoint(getCurrentPointString());
        }

        if (isPaused || currentState == State.PAUSED) {
            return;
        }

        checkForPlayers();

        if (autoSprint.get() && currentState != State.PLAYER_FOUND && currentState != State.COMPLETED) {
            mc.player.setSprinting(true);
        }

        switch (currentState) {
            case MOVING_TO_START:
                handleMovingToStart();
                break;
            case SEARCHING:
                handleSearching();
                break;
            case PLAYER_FOUND:
                handlePlayerFound();
                break;
            case COMPLETED:
                handleCompleted();
                break;
        }

        if (targetPos != null && currentState != State.PLAYER_FOUND && currentState != State.COMPLETED) {
            moveToTarget();
        }
    }

    private void handleMovingToStart() {
        if (isNearTarget(targetPos, 5.0)) {
            currentState = State.SEARCHING;
            addConsoleMessage("§bНачинаю систематический поиск по всей карте");
            moveToNextGridPoint();
        }
    }

    private void handleSearching() {
        if (isNearTarget(targetPos, 3.0)) {
            if (moveToNextGridPoint()) {
                currentState = State.COMPLETED;
                mc.player.setMotion(0, 0, 0);
                mc.player.setSprinting(false);
                addConsoleMessage("§2Поиск по всей карте завершен!");
            }
        }
    }

    private void handlePlayerFound() {
        if (System.currentTimeMillis() - playerFoundTime > 1000) {
            currentState = State.SEARCHING;
            addConsoleMessage("§eПродолжаю поиск");
            moveToNextGridPoint();
        }
    }

    private void handleCompleted() {
        mc.player.setMotion(0, 0, 0);
        mc.player.setSprinting(false);
    }

    private boolean moveToNextGridPoint() {
        if (movingRight) {
            currentGridX++;
            if (currentGridX >= totalGridsX) {
                currentGridX = totalGridsX - 1;
                currentGridZ++;
                movingRight = false;
                completedLines++;

                addConsoleMessage(String.format("§6Завершена линия %d из %d", completedLines, totalGridsZ));
            }
        } else {
            currentGridX--;
            if (currentGridX < 0) {
                currentGridX = 0;
                currentGridZ++;
                movingRight = true;
                completedLines++;

                addConsoleMessage(String.format("§6Завершена линия %d из %d", completedLines, totalGridsZ));
            }
        }

        if (currentGridZ >= totalGridsZ) {
            return true;
        }

        double gridSizeDouble = gridSize.get().doubleValue();
        double newX = MAP_BORDER_NEG + (currentGridX * gridSizeDouble);
        double newZ = MAP_BORDER_NEG + (currentGridZ * gridSizeDouble);

        newX = Math.max(MAP_BORDER_NEG, Math.min(MAP_BORDER_POS, newX));
        newZ = Math.max(MAP_BORDER_NEG, Math.min(MAP_BORDER_POS, newZ));

        targetPos = new Vector3d(newX, height.get(), newZ);

        return false;
    }

    private void moveToTarget() {
        Vector3d playerPos = mc.player.getPositionVec();
        Vector3d direction = targetPos.subtract(playerPos).normalize();

        if (checkObstacleAbove()) {
            targetPos = new Vector3d(targetPos.x, targetPos.y + 10, targetPos.z);
            addConsoleMessage("§eОбнаружено препятствие, поднимаюсь выше");
        }

        Vector3d motion = direction.scale(speed.get());
        mc.player.setMotion(motion.x, motion.y, motion.z);

        float yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        mc.player.rotationYaw = yaw;
        mc.player.rotationYawHead = yaw;
    }

    private boolean checkObstacleAbove() {
        BlockPos playerPos = mc.player.getPosition();

        for (int y = 1; y <= 5; y++) {
            BlockPos checkPos = playerPos.add(0, y, 0);
            Block block = mc.world.getBlockState(checkPos).getBlock();

            if (block != Blocks.AIR && block != Blocks.WATER && block != Blocks.LAVA) {
                return true;
            }
        }

        return false;
    }

    private void checkForPlayers() {
        if (currentState == State.PLAYER_FOUND || currentState == State.COMPLETED) return;

        Vector3d playerPos = mc.player.getPositionVec();

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;

            String playerName = player.getName().getString();
            if (foundPlayersNames.contains(playerName)) continue;

            double distance = playerPos.distanceTo(player.getPositionVec());

            if (distance <= searchRadius.get()) {
                currentState = State.PLAYER_FOUND;
                playerFoundTime = System.currentTimeMillis();

                mc.player.setMotion(0, 0, 0);
                mc.player.setSprinting(false);

                foundPlayersNames.add(playerName);

                String message = String.format("§aНашёл игрока: %s на расстоянии %.1f блоков! Координаты: %.0f %.0f %.0f",
                        player.getName().getString(), distance,
                        player.getPosX(), player.getPosY(), player.getPosZ());

                addConsoleMessage(message);

                double progress = getProgress();
                addConsoleMessage(String.format("§bПрогресс поиска: %.1f%%", progress));

                if (ui != null) {
                    ui.addFoundPlayer(player);
                }

                break;
            }
        }
    }

    private boolean isNearTarget(Vector3d target, double threshold) {
        if (target == null) return false;
        return mc.player.getPositionVec().distanceTo(target) <= threshold;
    }

    private double getProgress() {
        if (totalGridsX <= 0 || totalGridsZ <= 0) return 0.0;

        double totalPoints = (double) totalGridsX * totalGridsZ;
        double currentPoint = (double) (currentGridZ * totalGridsX + currentGridX);

        if (totalPoints <= 0) return 0.0;

        double progress = (currentPoint / totalPoints) * 100.0;
        return Math.min(100.0, Math.max(0.0, progress));
    }

    private int getFoundPlayersCount() {
        return (int) mc.world.getPlayers().stream().filter(p -> p != mc.player).count();
    }

    private String getCurrentPointString() {
        if (totalGridsX <= 0 || totalGridsZ <= 0) {
            return "Точка: 0/0";
        }

        int totalPoints = totalGridsX * totalGridsZ;
        int currentPoint = Math.min(totalPoints, Math.max(1, (currentGridZ * totalGridsX) + currentGridX + 1));

        double gridSizeDouble = gridSize.get().doubleValue();
        double currentX = MAP_BORDER_NEG + (currentGridX * gridSizeDouble);
        double currentZ = MAP_BORDER_NEG + (currentGridZ * gridSizeDouble);

        return String.format("Точка: %d/%d (%.0f, %.0f)", currentPoint, totalPoints, currentX, currentZ);
    }

    private String getStatusString() {
        if (isPaused) {
            return "На паузе";
        }

        switch (currentState) {
            case MOVING_TO_START:
                return "Движение к началу";
            case SEARCHING:
                return "Поиск";
            case PLAYER_FOUND:
                return "Игрок найден";
            case COMPLETED:
                return "Завершено";
            default:
                return "Неизвестно";
        }
    }

    public SliderSetting getHeight() { return height; }
    public SliderSetting getSearchRadius() { return searchRadius; }
    public SliderSetting getGridSize() { return gridSize; }
    public BooleanSetting getAutoSprint() { return autoSprint; }
    public SliderSetting getSpeed() { return speed; }
}
