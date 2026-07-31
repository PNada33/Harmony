#!/usr/bin/env python3
"""
train_bot_brain.py — обучает BotBrain (MLP) по записям из bot_demos.jsonl
и сохраняет веса в bot_brain.json (формат, который читает BotBrain.load()).

Зависимости: только numpy.
  pip install numpy

Запуск:
  python train_bot_brain.py

Опционально:
  python train_bot_brain.py --epochs 200 --lr 0.01 --hidden 32,24

Формат bot_demos.jsonl (пишет клиент):
  {"in":[24 числа -1..1], "out":[8 чисел -1..1]}

Архитектура должна совпадать с xd.harm.bot.brain.BotBrain:
  INPUT=24 -> 32 -> 24 -> OUTPUT=8, tanh везде.
"""
import argparse
import json
import math
import os

import numpy as np

INPUT_DIM = 32
OUTPUT_DIM = 9
HIDDEN = [32, 24]  # совпадает с BotBrain.HIDDEN


def load_data(path):
    X, Y = [], []
    with open(path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            try:
                obj = json.loads(line)
                x = np.asarray(obj["in"], dtype=np.float32)
                y = np.asarray(obj["out"], dtype=np.float32)
                if x.shape == (INPUT_DIM,) and y.shape == (OUTPUT_DIM,):
                    X.append(x)
                    Y.append(y)
            except Exception:
                continue
    if not X:
        raise SystemExit("Нет валидных записей в %s" % path)
    return np.asarray(X), np.asarray(Y)


def build_weights(seed=0):
    rng = np.random.default_rng(seed)
    dims = [INPUT_DIM] + HIDDEN + [OUTPUT_DIM]
    weights, biases = [], []
    for i in range(len(dims) - 1):
        # Xavier-подобная инициализация
        lim = math.sqrt(6.0 / (dims[i] + dims[i + 1]))
        w = rng.uniform(-lim, lim, size=(dims[i + 1], dims[i])).astype(np.float32)
        b = np.zeros(dims[i + 1], dtype=np.float32)
        weights.append(w)
        biases.append(b)
    return weights, biases


def forward(x, weights, biases):
    a = x
    for i in range(len(weights)):
        a = np.tanh(a @ weights[i].T + biases[i])
    return a


def train(X, Y, epochs, lr, batch):
    weights, biases = build_weights()
    n = X.shape[0]
    idx = np.arange(n)
    for ep in range(epochs):
        np.random.shuffle(idx)
        total_loss = 0.0
        for start in range(0, n, batch):
            bidx = idx[start:start + batch]
            xb = X[bidx]
            yb = Y[bidx]
            # forward (накопляем градиенты)
            acts = [xb]
            a = xb
            for i in range(len(weights)):
                a = np.tanh(a @ weights[i].T + biases[i])
                acts.append(a)
            # MSE loss; delta на выходе
            delta = (acts[-1] - yb) / xb.shape[0]
            total_loss += float(np.mean((acts[-1] - yb) ** 2)) * xb.shape[0]
            # backprop (только градиенты, без оптимизатора)
            for i in range(len(weights) - 1, -1, -1):
                a_prev = acts[i]
                grad_w = delta.T @ a_prev
                grad_b = delta.sum(axis=0)
                # d(tanh)/dx = 1 - tanh^2
                da = (1.0 - acts[i] ** 2 + 1e-8)
                if i > 0:
                    delta = (delta @ weights[i]) * da
                # обновление
                weights[i] -= lr * grad_w
                biases[i] -= lr * grad_b
        if (ep + 1) % max(1, epochs // 20) == 0:
            print(f"epoch {ep+1:4d}/{epochs}  loss={total_loss / n:.5f}")
    return weights, biases


def export(weights, biases, path):
    # Формат: на каждый слой строка "w0_0,...,w0_n,b0; w1_0,...,b1; ..."
    # Без пробелов внутри чисел; разделитель весов ',' , нейронов ';'
    lines = []
    for l in range(len(weights)):
        parts = []
        for o in range(weights[l].shape[0]):
            vals = [f"{weights[l][o][j]:.6g}" for j in range(weights[l].shape[1])]
            vals.append(f"{biases[l][o]:.6g}")
            parts.append(",".join(vals))
        lines.append(";".join(parts))
    with open(path, "w", encoding="utf-8") as f:
        f.write("# BotBrain weights. Format per layer: w0_0,w0_1,...,w0_n,b0; w1_0,...,b1; ...\n")
        f.write("\n".join(lines) + "\n")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--data", default="bot_demos.jsonl")
    ap.add_argument("--out", default="bot_brain.json")
    ap.add_argument("--epochs", type=int, default=200)
    ap.add_argument("--lr", type=float, default=0.01)
    ap.add_argument("--batch", type=int, default=32)
    ap.add_argument("--hidden", default="32,24", help="через запятую, должно совпадать с BotBrain.HIDDEN")
    args = ap.parse_args()

    global HIDDEN
    HIDDEN = [int(x) for x in args.hidden.split(",")]

    here = os.path.dirname(os.path.abspath(__file__))
    data_path = args.data if os.path.isabs(args.data) else os.path.join(here, args.data)
    out_path = args.out if os.path.isabs(args.out) else os.path.join(here, args.out)

    print(f"Загрузка данных из {data_path} ...")
    X, Y = load_data(data_path)
    print(f"Записей: {X.shape[0]}  (in={X.shape[1]}, out={Y.shape[1]})")

    print(f"Обучение MLP {INPUT_DIM}->{','.join(map(str,HIDDEN))}->{OUTPUT_DIM} ...")
    weights, biases = train(X, Y, args.epochs, args.lr, args.batch)

    export(weights, biases, out_path)
    print(f"Веса сохранены в {out_path}")


if __name__ == "__main__":
    main()
