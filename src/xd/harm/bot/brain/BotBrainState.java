package xd.harm.bot.brain;

/**
 * Входной вектор "мозга" бота. Заполняется из BotMode каждый тик.
 * Порядок полей строго соответствует INPUT_DIM = 24.
 */
public final class BotBrainState {

    public enum Phase {
        SPAWN, COLLECT_IRON, COLLECT_EMERALD, BRIDGE, FIGHT, DEFEND_BED, RUSH_ENEMY, DEAD
    }

    // --- Позиция / дистанции (нормализованы к ~100 блокам) ---
    public float selfX = 0, selfY = 0, selfZ = 0;     // [0..3]   позиция относительно центра карты
    public float distToCenter = 0;                    // [4]
    public float distToEmerald = 0;                   // [5]
    public float distToDiamond = 0;                   // [6]
    public float distToEnemyBed = 0;                  // [7]
    public float distToOwnGen = 0;                    // [8]     дистанция до ближайшего железа/золота

    // --- Ресурсы (нормализованы: /64) ---
    public int iron = 0;                              // [9]
    public int gold = 0;                              // [10]
    public int emerald = 0;                           // [11]
    public int diamond = 0;                           // [12]
    public int blocks = 0;                            // [13]    блоки для моста

    // --- Состояние игры ---
    public boolean ownBedAlive = true;                // [14]   1/0
    public boolean enemyBedAlive = true;              // [15]   1/0
    public float gameTimeNorm = 0;                   // [16]   время с начала / 600с
    public Phase phase = Phase.SPAWN;                // [17]   one-hot ниже
    public float health = 20;                         // [18]   /20
    public float armor = 0;                           // [19]   /20 (примерно)
    public int enemiesNearby = 0;                    // [20]
    public float onEmerald = 0;                       // [21]   1 если стоим на генераторе изумруда
    public float onDiamond = 0;                       // [22]   1 если стоим на генераторе алмаза
    public float bridging = 0;                        // [23]   1 если уже строим мост

    // --- Цель от игрока (стратегия + план закупа) ---
    public int strategyIndex = 0;                     // [24..28] one-hot: 0 Balanced,1 RushMid,2 Defensive,3 Aggressive,4 AggressiveMax
    public int ironTarget = 30;                       // [29]   цель по железу из "30i 6g" (норм /64)
    public int goldTarget = 6;                        // [30]   цель по золоту
    public float unused = 0;                          // [31]   резерв

    public float[] toVector() {
        float[] v = new float[BotBrain.INPUT_DIM];
        v[0] = selfX / 100f;
        v[1] = selfY / 100f;
        v[2] = selfZ / 100f;
        v[3] = distToCenter / 100f;
        v[4] = distToEmerald / 100f;
        v[5] = distToDiamond / 100f;
        v[6] = distToEnemyBed / 100f;
        v[7] = distToOwnGen / 100f;
        v[8] = norm(iron);
        v[9] = norm(gold);
        v[10] = norm(emerald);
        v[11] = norm(diamond);
        v[12] = norm(blocks);
        v[13] = ownBedAlive ? 1f : 0f;
        v[14] = enemyBedAlive ? 1f : 0f;
        v[15] = clamp01(gameTimeNorm);
        v[16] = phase.ordinal() / 8f;
        v[17] = clamp01(health / 20f);
        v[18] = clamp01(armor / 20f);
        v[19] = clamp01(enemiesNearby / 5f);
        v[20] = onEmerald;
        v[21] = onDiamond;
        v[22] = bridging;
        v[23] = ownBedAlive && !enemyBedAlive ? 1f : 0f; // бонус "враг без кровати"
        // one-hot стратегии
        for (int i = 0; i < 5; i++) v[24 + i] = (i == strategyIndex) ? 1f : 0f;
        v[29] = norm(ironTarget);
        v[30] = norm(goldTarget);
        v[31] = clamp01(unused);
        return v;
    }

    private static float norm(int v) { return clamp01(v / 64f); }
    private static float clamp01(float v) { return v < 0f ? 0f : (v > 1f ? 1f : v); }
}
