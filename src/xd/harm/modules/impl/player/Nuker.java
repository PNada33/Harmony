package xd.harm.modules.impl.player;

import xd.harm.events.movement.EventMotion;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.SliderSetting;
import com.google.common.eventbus.Subscribe;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;

@ModuleRegister(name = "Nuker", category = Category.Player, desc = "Автоматически ломает блоки вокруг")
public class Nuker extends Module {

    private final SliderSetting swapDelay = new SliderSetting("Задержка", 200f, 0f, 1000f, 50f);

    private BlockPos currentBlock = null;
    private BlockPos lastBlock = null;
    private long lastSwapTime = 0;

    public Nuker() {
        addSettings(swapDelay);
    }

    @Override
    public boolean onDisable() {
        mc.gameSettings.keyBindAttack.setPressed(false);
        currentBlock = null;
        lastBlock = null;
        return super.onDisable();
    }

    @Subscribe
    public void onMotion(EventMotion event) {
        if (mc.player == null || mc.world == null) return;

        currentBlock = findBlock();

        if (currentBlock != null) {
            lookAtBlock(currentBlock);

            if (lastBlock == null || !lastBlock.equals(currentBlock)) {
                lastBlock = currentBlock;
                lastSwapTime = System.currentTimeMillis();
            }

            if (System.currentTimeMillis() - lastSwapTime >= swapDelay.get().longValue()) {
                mc.gameSettings.keyBindAttack.setPressed(true);
            } else {
                mc.gameSettings.keyBindAttack.setPressed(false);
            }
        } else {
            mc.gameSettings.keyBindAttack.setPressed(false);
            lastBlock = null;
        }
    }

    private int getBlockPriority(BlockPos pos) {
        Block block = mc.world.getBlockState(pos).getBlock();

        if (block == Blocks.BEDROCK) {
            return 9;
        } else if (block == Blocks.OBSIDIAN || block == Blocks.CRYING_OBSIDIAN) {
            return 8;
        } else if (block == Blocks.ANCIENT_DEBRIS || block == Blocks.NETHERITE_BLOCK) {
            return 7;
        } else if (block == Blocks.ENDER_CHEST || block == Blocks.ENCHANTING_TABLE || block == Blocks.ANVIL) {
            return 6;
        } else if (block == Blocks.DIAMOND_BLOCK || block == Blocks.EMERALD_BLOCK) {
            return 5;
        } else if (block == Blocks.IRON_BLOCK || block == Blocks.GOLD_BLOCK) {
            return 4;
        } else if (block == Blocks.COAL_BLOCK || block == Blocks.LAPIS_BLOCK) {
            return 3;
        } else if (block == Blocks.COBBLESTONE || block == Blocks.STONE_BRICKS) {
            return 2;
        } else if (block == Blocks.DIRT) {
            return 1;
        }

        return 0;
    }

    private boolean isValidBlock(Block block) {
        return block == Blocks.OBSIDIAN || block == Blocks.CRYING_OBSIDIAN ||
                block == Blocks.ANCIENT_DEBRIS || block == Blocks.NETHERITE_BLOCK ||
                block == Blocks.ENDER_CHEST || block == Blocks.ENCHANTING_TABLE ||
                block == Blocks.ANVIL || block == Blocks.DIAMOND_BLOCK ||
                block == Blocks.EMERALD_BLOCK || block == Blocks.IRON_BLOCK ||
                block == Blocks.GOLD_BLOCK || block == Blocks.COAL_BLOCK ||
                block == Blocks.LAPIS_BLOCK || block == Blocks.COBBLESTONE ||
                block == Blocks.STONE_BRICKS || block == Blocks.STONE ||
                block == Blocks.BEDROCK || block == Blocks.GRASS_BLOCK ||
                block == Blocks.SAND || block == Blocks.GRAVEL ||
                block == Blocks.OAK_LOG || block == Blocks.SPRUCE_LOG ||
                block == Blocks.BIRCH_LOG || block == Blocks.JUNGLE_LOG ||
                block == Blocks.ACACIA_LOG || block == Blocks.DARK_OAK_LOG;
    }

    private BlockPos findBlock() {
        double reach = mc.playerController.getBlockReachDistance() + 0.5;
        int radius = (int) Math.ceil(reach);
        Vector3d eyePos = mc.player.getEyePosition(1.0f);
        BlockPos playerPos = mc.player.getPosition();

        BlockPos bestBlock = null;
        int bestPriority = -1;
        double bestDistance = Double.MAX_VALUE;

        for (int y = 0; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos pos = playerPos.add(x, y, z);
                    double distance = eyePos.squareDistanceTo(Vector3d.copyCentered(pos));

                    if (distance > reach * reach) continue;

                    Block block = mc.world.getBlockState(pos).getBlock();

                    if (block == Blocks.AIR) continue;
                    if (block.getDefaultState().getBlockHardness(mc.world, pos) < 0) continue;

                    if (!isValidBlock(block)) continue;

                    int priority = getBlockPriority(pos);

                    if (priority > bestPriority || (priority == bestPriority && distance < bestDistance)) {
                        bestPriority = priority;
                        bestDistance = distance;
                        bestBlock = pos;
                    }
                }
            }
        }

        return bestBlock;
    }

    private void lookAtBlock(BlockPos pos) {
        Vector3d blockVec = Vector3d.copyCentered(pos);
        Vector3d eyePos = mc.player.getEyePosition(1.0f);

        double diffX = blockVec.x - eyePos.x;
        double diffY = blockVec.y - eyePos.y;
        double diffZ = blockVec.z - eyePos.z;

        double dist = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0f;
        float pitch = (float) -Math.toDegrees(Math.atan2(diffY, dist));

        mc.player.rotationYaw = yaw;
        mc.player.rotationPitch = pitch;
    }
}
