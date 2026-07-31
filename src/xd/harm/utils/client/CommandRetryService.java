package xd.harm.utils.client;

import com.google.common.eventbus.Subscribe;
import net.minecraft.network.play.server.SChatPacket;
import net.minecraft.util.text.TextFormatting;
import xd.harm.Harmony;
import xd.harm.events.network.EventPacket;
import xd.harm.events.world.EventUpdate;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandRetryService implements IMinecraft {
    private static final Pattern DELAY_PATTERN = Pattern.compile("(\\d+)");
    private static final Pattern COMMAND_PATTERN = Pattern.compile("/[A-Za-z0-9_]+");

    private long lastDelayAt = 0L;
    private int lastDelaySec = -1;
    private String pendingCommand = null;
    private long retryAt = -1L;

    public CommandRetryService() {
        Harmony.getInstance().getEventBus().register(this);
    }

    @Subscribe
    public void onPacket(EventPacket e) {
        if (!e.isReceive()) return;
        if (!(e.getPacket() instanceof SChatPacket chat)) return;

        String raw = chat.getChatComponent().getString();
        String msg = TextFormatting.getTextWithoutFormattingCodes(raw);
        if (msg == null) msg = raw;
        String lower = msg.toLowerCase(Locale.ROOT);

        if (lower.contains("слишком много людей пишет эту команду") && lower.contains("попробуйте через")) {
            Matcher m = DELAY_PATTERN.matcher(lower);
            if (m.find()) {
                try {
                    lastDelaySec = Integer.parseInt(m.group(1));
                    lastDelayAt = System.currentTimeMillis();
                } catch (Exception ignored) {
                }
            }
            return;
        }

        if (lower.contains("команда") && lower.contains("/")) {
            Matcher m = COMMAND_PATTERN.matcher(msg);
            if (m.find()) {
                String cmd = m.group();
                if (lastDelaySec > 0 && (System.currentTimeMillis() - lastDelayAt) <= 5000L) {
                    scheduleRetry(cmd, lastDelaySec);
                }
            }
        }
    }

    @Subscribe
    public void onUpdate(EventUpdate e) {
        if (pendingCommand == null || retryAt <= 0) return;
        if (mc.player == null) return;
        if (System.currentTimeMillis() >= retryAt) {
            mc.player.sendChatMessage(pendingCommand);
            pendingCommand = null;
            retryAt = -1L;
        }
    }

    private void scheduleRetry(String command, int delaySec) {
        pendingCommand = command;
        retryAt = System.currentTimeMillis() + (delaySec * 1000L);
    }
}

