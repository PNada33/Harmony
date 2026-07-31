package xd.harm.modules.impl.movement.scaffoldliquid;

import net.minecraft.block.AirBlock;
import net.minecraft.block.FlowingFluidBlock;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import xd.harm.utils.client.IMinecraft;

import java.util.ArrayDeque;

/**
 * Movement planner — предсказывает оптимальную линию для установки блоков
 * (портировано из LiquidBounce ScaffoldMovementPlanner)
 */
public class ScaffoldLiquidMovementPlanner implements IMinecraft {

    private static final int MAX_LAST_PLACE_BLOCKS = 4;
    private static final double[] OFFSETS_TO_TRY = {0.301, 0.0, -0.301};

    private final ArrayDeque<BlockPos> lastPlacedBlocks = new ArrayDeque<>(MAX_LAST_PLACE_BLOCKS);
    private BlockPos lastPosition;

    /**
     * Находит оптимальную линию для установки блоков на основе направления движения
     */
    public Line getOptimalMovementLine(float movementYaw) {
        BlockPos blockUnderPlayer = findBlockPlayerStandsOn();
        if (blockUnderPlayer == null) return null;

        Vector3d direction = chooseDirection(movementYaw);

        Line lastBlocksLine = fitLinesThroughLastPlacedBlocks();
        Vector3d lineBase;

        if (lastBlocksLine != null && !divergesTooMuchFromDirection(lastBlocksLine, direction)) {
            lineBase = lastBlocksLine.position;
        } else {
            lineBase = new Vector3d(
                    blockUnderPlayer.getX() + 0.5,
                    blockUnderPlayer.getY(),
                    blockUnderPlayer.getZ() + 0.5
            );
        }

        return new Line(
                new Vector3d(lineBase.x, mc.player.getPosY(), lineBase.z),
                direction
        );
    }

    /**
     * Проверяет, не слишком ли сильно линия отклоняется от направления
     */
    private boolean divergesTooMuchFromDirection(Line lastBlocksLine, Vector3d direction) {
        return lastBlocksLine.direction.dotProduct(direction) < 0.5;
    }

    /**
     * Строит линию через последние установленные блоки
     */
    private Line fitLinesThroughLastPlacedBlocks() {
        if (lastPlacedBlocks.size() < 2) return null;

        BlockPos last = lastPlacedBlocks.getLast();
        BlockPos[] arr = lastPlacedBlocks.toArray(new BlockPos[0]);
        BlockPos secondToLast = arr[arr.length - 2];

        // Средняя точка между двумя последними блоками
        Vector3d avgPos = new Vector3d(
                (secondToLast.getX() + last.getX()) / 2.0 + 0.5,
                (secondToLast.getY() + last.getY()) / 2.0,
                (secondToLast.getZ() + last.getZ()) / 2.0 + 0.5
        );

        // Направление от предпоследнего к последнему
        Vector3d dir = new Vector3d(
                last.getX() - secondToLast.getX(),
                last.getY() - secondToLast.getY(),
                last.getZ() - secondToLast.getZ()
        ).normalize();

        return new Line(avgPos, dir);
    }

    /**
     * Находит блок, на котором стоит игрок
     */
    private BlockPos findBlockPlayerStandsOn() {
        java.util.Set<BlockPos> candidates = new java.util.HashSet<>();

        for (double xOffset : OFFSETS_TO_TRY) {
            for (double zOffset : OFFSETS_TO_TRY) {
                BlockPos pos = new BlockPos(
                        mc.player.getPosX() + xOffset,
                        mc.player.getPosY() - 1.0,
                        mc.player.getPosZ() + zOffset
                );

                if (isSolidBlock(pos)) {
                    candidates.add(pos);
                }
            }
        }

        // Сначала проверяем последний установленный блок
        BlockPos lastPlaced = lastPlacedBlocks.peekLast();
        if (lastPlaced != null && candidates.contains(lastPlaced)) {
            return lastPlaced;
        }

        // Потом предыдущую позицию
        if (lastPosition != null && candidates.contains(lastPosition)) {
            return lastPosition;
        }

        // Иначе первый подходящий
        BlockPos first = candidates.stream().findFirst().orElse(null);
        if (first != null) {
            lastPosition = first;
        }
        return first;
    }

    /**
     * Выбирает направление на основе текущего yaw игрока
     */
    private Vector3d chooseDirection(float yaw) {
        float currentDirection = yaw / 180f * 4f + 4f;
        float newDirectionNumber = Math.round(currentDirection);
        float newDirectionAngle = MathHelper.wrapDegrees(
                (newDirectionNumber - 4f) / 4f * 180f);
        return Vector3d.fromPitchYaw(0f, newDirectionAngle);
    }

    /**
     * Отслеживает установленный блок
     */
    public void trackPlacedBlock(BlockPos target) {
        if (target.equals(lastPlacedBlocks.peekLast())) return;
        while (lastPlacedBlocks.size() >= MAX_LAST_PLACE_BLOCKS) {
            lastPlacedBlocks.removeFirst();
        }
        lastPlacedBlocks.add(target);
    }

    /**
     * Сбрасывает состояние
     */
    public void reset() {
        lastPosition = null;
        lastPlacedBlocks.clear();
    }

    private boolean isSolidBlock(BlockPos pos) {
        if (pos.getY() < 0 || pos.getY() > 255) return false;
        net.minecraft.block.Block block = mc.world.getBlockState(pos).getBlock();
        return !(block instanceof AirBlock) && !(block instanceof FlowingFluidBlock);
    }

    /**
     * Класс, представляющий линию (позиция + направление)
     */
    public static class Line {
        public final Vector3d position;
        public final Vector3d direction;

        public Line(Vector3d position, Vector3d direction) {
            this.position = position;
            this.direction = direction;
        }
    }
}
