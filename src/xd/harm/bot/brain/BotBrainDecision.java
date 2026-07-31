package xd.harm.bot.brain;

/**
 * Решение "мозга" бота. Используется BotMode для управления поведением
 * и BotAttack — для отправки сообщений в чат.
 */
public final class BotBrainDecision {

    /** 0..1 — насколько сильно хотим начать/продолжить строить мост. */
    public float bridgeDesire = 0f;

    /** true — строить мост до центра, false — идти к вражеской базе. */
    public boolean bridgeTargetCenter = true;

    /** Сколько тиков (20 тик = 1с) ещё стоять на генераторе изумруда. */
    public int emeraldWaitTicks = 0;

    /** Сколько тиков ещё стоять на генераторе алмаза. */
    public int diamondWaitTicks = 0;

    /** Индекс фразы для чата (см. BotBrainChat.LINES), -1 = молчать. */
    public int chatIndex = -1;

    /** 0..1 — уверенность в выборе фразы (для порога отправки). */
    public float chatConfidence = 0f;

    /** true — решение от нейросети, false — от эвристики-фоллбэка. */
    public boolean fromNeuralNet = false;
}
