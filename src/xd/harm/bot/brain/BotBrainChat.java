package xd.harm.bot.brain;

import net.minecraft.client.Minecraft;

/**
 * Канонические фразы, которые "мозг" может написать в чат.
 * Индекс фразы совпадает с chatIndex из BotBrainDecision (0..3).
 */
public final class BotBrainChat {

    public static final String[] LINES = {
            "Going for mid",
            "Collecting emerald",
            "Defending bed",
            "Rushing enemy base",
            "Collecting resources"
    };

    /** Минимальная уверенность, чтобы вообще что-то написать. */
    public static final float SEND_THRESHOLD = 0.35f;

    /** Отправляет фразу в чат от лица бота (если toggleChat включён). */
    public static void maybeSend(BotBrainDecision d, boolean toggleChat) {
        if (!toggleChat) return;
        if (d == null || d.chatIndex < 0 || d.chatIndex >= LINES.length) return;
        if (d.chatConfidence < SEND_THRESHOLD) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;
        mc.player.sendChatMessage(LINES[d.chatIndex]);
    }
}
