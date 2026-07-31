package xd.harm.bot.brain;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Лёгкая нейросеть (MLP) для "мозга" бота в BedWars.
 *
 * Полностью на чистом Java, работает на CPU, без внешних библиотек.
 * При отсутствии файла весов использует эвристику (heuristic),
 * чтобы боты продолжали играть даже до обучения.
 *
 * Обучение: включи запись демо (bwai_record), поиграй сам —
 * данные пишутся в bot_demos.jsonl, затем скорми их train_bot_brain.py,
 * который сгенерирует bot_brain.json с весами. Загрузи его — мозг "оживёт".
 */
public final class BotBrain {

    public static final int INPUT_DIM = 32;
    public static final int OUTPUT_DIM = 9;

    // Архитектура: INPUT -> 32 -> 24 -> OUTPUT
    private static final int[] HIDDEN = {32, 24};

    private static final Path WEIGHTS_FILE = Paths.get("E:\\Мои Сурсы\\harmony\\bot_brain.json");

    // Веса: layers[i] — матрица [HIDDEN[i]][prevDim], biases[i] — вектор [HIDDEN[i]]
    private final float[][][] weights;
    private final float[][] biases;

    private static BotBrain instance;

    private BotBrain() {
        int[] dims = new int[HIDDEN.length + 1];
        dims[0] = INPUT_DIM;
        System.arraycopy(HIDDEN, 0, dims, 1, HIDDEN.length);
        weights = new float[HIDDEN.length + 1][][];
        biases = new float[HIDDEN.length + 1][];
        for (int i = 0; i < weights.length; i++) {
            int out = (i == weights.length - 1) ? OUTPUT_DIM : HIDDEN[i];
            int in = dims[i];
            weights[i] = new float[out][in];
            biases[i] = new float[out];
            // Маленькая случайная инициализация, чтобы сеть не была мёртвой до обучения
            for (int o = 0; o < out; o++) {
                for (int j = 0; j < in; j++) {
                    weights[i][o][j] = (float) ((Math.random() * 2 - 1) * 0.1);
                }
                biases[i][o] = 0f;
            }
        }
    }

    public static synchronized BotBrain getInstance() {
        if (instance == null) {
            instance = new BotBrain();
            instance.load(); // пытаемся загрузить веса; при неудаче остаётся random
        }
        return instance;
    }

    /** Загружены ли реальные (обученные) веса, а не стартовый мусор. */
    public boolean hasTrainedWeights() {
        return trained;
    }
    private boolean trained = false;

    // ---------- Прямой проход ----------

    public float[] forward(float[] input) {
        if (input == null || input.length != INPUT_DIM) {
            throw new IllegalArgumentException("BotBrain.forward: expected INPUT_DIM=" + INPUT_DIM + " got " + (input == null ? "null" : input.length));
        }
        float[] a = input;
        for (int l = 0; l < weights.length; l++) {
            float[] next = new float[weights[l].length];
            for (int o = 0; o < next.length; o++) {
                float sum = biases[l][o];
                float[] w = weights[l][o];
                for (int j = 0; j < a.length; j++) {
                    sum += w[j] * a[j];
                }
                // На скрытых слоях — tanh, на выходе — tanh (клиент сам маппит в 0..1)
                next[o] = (float) Math.tanh(sum);
            }
            a = next;
        }
        return a; // длина OUTPUT_DIM, значения в (-1, 1)
    }

    // ---------- Решения ----------

    /**
     * Принимает состояние бота и возвращает решение "мозга".
     * Если веса не обучены — использует эвристику.
     */
    public BotBrainDecision decide(BotBrainState s) {
        float[] out = forward(s.toVector());
        if (!trained) {
            return heuristic(s);
        }
        BotBrainDecision d = new BotBrainDecision();
        d.bridgeDesire = clamp01((out[0] + 1f) * 0.5f);
        d.bridgeTargetCenter = out[1] >= 0f; // >0 -> центр, <0 -> вражеская база
        d.emeraldWaitTicks = (int) (clamp01((out[2] + 1f) * 0.5f) * 600f); // до 30с стоять
        d.diamondWaitTicks = (int) (clamp01((out[3] + 1f) * 0.5f) * 600f);
        // Чат: argmax по логитам (out[4..8])
        int best = 4, second = 5;
        for (int i = 4; i < OUTPUT_DIM; i++) {
            if (out[i] > out[best]) { second = best; best = i; }
            else if (out[i] > out[second]) { second = i; }
        }
        d.chatIndex = best - 4;
        d.chatConfidence = clamp01((out[best] - out[second]) * 0.5f);
        d.fromNeuralNet = true;
        return d;
    }

    /** Эвристика-фоллбэк: работает до обучения. */
    private BotBrainDecision heuristic(BotBrainState s) {
        BotBrainDecision d = new BotBrainDecision();
        // Мост начинаем, когда набрали ресурсную цель (iron>=30, gold>=6 условно)
        boolean enough = s.iron >= 24 && s.gold >= 4;
        d.bridgeDesire = enough ? 0.9f : 0.1f;
        d.bridgeTargetCenter = true;
        // Стоим на спец-генераторах, пока не набрали запас
        d.emeraldWaitTicks = s.emerald < 4 ? 200 : 0;
        d.diamondWaitTicks = s.diamond < 4 ? 200 : 0;
        // Чат по фазе
        if (s.phase == BotBrainState.Phase.BRIDGE) d.chatIndex = 0;
        else if (s.phase == BotBrainState.Phase.COLLECT_EMERALD) d.chatIndex = 1;
        else if (s.phase == BotBrainState.Phase.DEFEND_BED) d.chatIndex = 2;
        else if (s.phase == BotBrainState.Phase.RUSH_ENEMY) d.chatIndex = 3;
        else d.chatIndex = 4; // COLLECT_IRON / SPAWN -> "Collecting resources"
        d.chatConfidence = 1f;
        d.fromNeuralNet = false;
        return d;
    }

    // ---------- Загрузка / сохранение весов ----------

    public void load() {
        trained = false;
        if (!Files.exists(WEIGHTS_FILE)) return;
        try (BufferedReader r = Files.newBufferedReader(WEIGHTS_FILE, StandardCharsets.UTF_8)) {
            String line;
            int layer = 0;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (line.startsWith("#")) continue;
                if (layer >= weights.length) break;
                String[] parts = line.split(";");
                for (int o = 0; o < parts.length && o < weights[layer].length; o++) {
                    String[] vals = parts[o].split(",");
                    for (int j = 0; j < vals.length && j < weights[layer][o].length; j++) {
                        weights[layer][o][j] = Float.parseFloat(vals[j].trim());
                    }
                    if (o < biases[layer].length && vals.length > weights[layer][o].length) {
                        biases[layer][o] = Float.parseFloat(vals[weights[layer][o].length].trim());
                    }
                }
                layer++;
            }
            // Проверяем, что хотя бы первый вес не нулевой (признак обученности)
            trained = Math.abs(weights[0][0][0]) > 1e-6f;
        } catch (Exception e) {
            trained = false;
        }
    }

    public void save() {
        try (BufferedWriter w = Files.newBufferedWriter(WEIGHTS_FILE, StandardCharsets.UTF_8)) {
            w.write("# BotBrain weights. Format per layer: w0_0,w0_1,...,w0_n,b0; w1_0,...,b1; ...\n");
            for (int l = 0; l < weights.length; l++) {
                StringBuilder sb = new StringBuilder();
                for (int o = 0; o < weights[l].length; o++) {
                    for (int j = 0; j < weights[l][o].length; j++) {
                        if (j > 0) sb.append(',');
                        sb.append(weights[l][o][j]);
                    }
                    sb.append(',').append(biases[l][o]);
                    if (o < weights[l].length - 1) sb.append(';');
                }
                w.write(sb.toString());
                w.write('\n');
            }
        } catch (IOException e) {
            // игнорируем — сохранение не критично
        }
    }

    // ---------- Утилиты ----------

    private static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    /** Доступ к сырым весам для тренера (экспорт/импорт из Python). */
    public float[][][] getWeights() { return weights; }
    public float[][] getBiases() { return biases; }
}
