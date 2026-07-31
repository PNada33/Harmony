package xd.harm.modules.impl.misc;

import com.google.common.eventbus.Subscribe;
import xd.harm.events.network.EventPacket;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.SliderSetting;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.network.play.server.STitlePacket;
import net.minecraft.network.play.server.SWindowItemsPacket;

@ModuleRegister(name = "BWAutoLeave", category = Category.Misc, desc = "При смерти (кровать уничтожена) пишет /leave и кликает зелёное стекло")
public class BWAutoLeave extends Module {

    private static final int STATE_IDLE = 0;
    private static final int STATE_WAITING_LEAVE = 1;
    private static final int STATE_SENT_LEAVE = 2;
    private static final int STATE_CONFIRM_CLICKED = 3;

    private int state = STATE_IDLE;
    private long stateTime;
    private String lastSubtitle = "";

    private final SliderSetting reloadMs = new SliderSetting("Reload ms", 1000, 0, 5000, 100);
    private final SliderSetting delay = new SliderSetting("Delay", 2, 0, 15, 1);

    public BWAutoLeave() {
        addSettings(delay, reloadMs);
    }

    @Override
    public boolean onEnable() {
        state = STATE_IDLE;
        lastSubtitle = "";
        return super.onEnable();
    }

    @Subscribe
    public void onUpdate(EventUpdate e) {
        if (!isState()) return;

        // Задержка перед отправкой /leave — тикаем тут, не в onPacket
        if (state == STATE_WAITING_LEAVE) {
            if (System.currentTimeMillis() >= stateTime) {
                mc.player.sendChatMessage("/leave");
                state = STATE_SENT_LEAVE;
                stateTime = System.currentTimeMillis();
                System.out.println("[BWAutoLeave] Delay elapsed, sent /leave");
            }
        }

        // Закрытие экрана после клика по зелёному стеклу
        if (state == STATE_CONFIRM_CLICKED && System.currentTimeMillis() - stateTime >= reloadMs.get().intValue()) {
            mc.player.closeScreen();
            state = STATE_IDLE;
        }
    }

    @Subscribe
    public void onPacket(EventPacket event) {
        if (!isState()) return;
        if (!event.isReceive()) return;

        // Детект титула "Вы мертвы!" / "Defeat"
        if (event.getPacket() instanceof STitlePacket titlePacket) {
            if (titlePacket.getType() == STitlePacket.Type.TITLE && titlePacket.getMessage() != null) {
                String title = titlePacket.getMessage().getString();
                if (title.contains("Вы мертвы") || title.contains("Defeat") || title.contains("DEFEAT")) {
                    if (!lastSubtitle.contains("возродитесь") && !lastSubtitle.contains("respawn")
                            && !lastSubtitle.contains("Respawn")) {
                        int delayTicks = delay.get().intValue();
                        if (delayTicks <= 0) {
                            mc.player.sendChatMessage("/leave");
                            state = STATE_SENT_LEAVE;
                            stateTime = System.currentTimeMillis();
                            System.out.println("[BWAutoLeave] Death detected, sent /leave");
                        } else {
                            state = STATE_WAITING_LEAVE;
                            stateTime = System.currentTimeMillis() + (delayTicks * 50L);
                            System.out.println("[BWAutoLeave] Death detected, waiting " + delayTicks + " ticks");
                        }
                    }
                }
                lastSubtitle = "";
            } else if (titlePacket.getType() == STitlePacket.Type.SUBTITLE && titlePacket.getMessage() != null) {
                lastSubtitle = titlePacket.getMessage().getString();
            } else if (titlePacket.getType() == STitlePacket.Type.CLEAR || titlePacket.getType() == STitlePacket.Type.RESET) {
                lastSubtitle = "";
            }
        }

        // Обработка GUI — только после того как /leave реально отправлен
        if (event.getPacket() instanceof SWindowItemsPacket windowPacket) {
            // Подтверждение (слот 2 = "Подтвердить"/"Confirm")
            if (state == STATE_SENT_LEAVE) {
                java.util.List<net.minecraft.item.ItemStack> items = windowPacket.getItemStacks();
                if (items.size() > 2 && items.size() < 46) {
                    String displayName = items.get(2).getDisplayName().getString();
                    if (displayName.contains("Подтвердить") || displayName.contains("Confirm")) {
                        mc.playerController.windowClick(mc.player.openContainer.windowId, 2, 0, ClickType.PICKUP, mc.player);
                        state = STATE_CONFIRM_CLICKED;
                        stateTime = System.currentTimeMillis();
                        event.cancel();
                        System.out.println("[BWAutoLeave] Clicked confirm at slot 2");
                    }
                }
            }

            // Зелёное стекло в хабе
            if (state == STATE_CONFIRM_CLICKED) {
                java.util.List<net.minecraft.item.ItemStack> items = windowPacket.getItemStacks();
                int totalItems = items.size();
                int containerSize = totalItems;
                if (containerSize > 36) containerSize = totalItems - 36;
                if (containerSize > 54) containerSize = totalItems;
                for (int i = 0; i < containerSize && i < totalItems; i++) {
                    net.minecraft.item.ItemStack stack = items.get(i);
                    if (stack.isEmpty()) continue;
                    boolean isGreenGlass = stack.getItem() == net.minecraft.item.Items.LIME_STAINED_GLASS_PANE;
                    if (!isGreenGlass && stack.hasDisplayName()) {
                        String name = stack.getDisplayName().getString().toLowerCase(java.util.Locale.ROOT);
                        isGreenGlass = name.contains("играть") || name.contains("play") || name.contains("play again")
                                || name.contains("сыграть") || name.contains("вернуть");
                    }
                    if (isGreenGlass) {
                        mc.playerController.windowClick(windowPacket.getWindowId(), i, 0, ClickType.PICKUP, mc.player);
                        state = STATE_IDLE;
                        System.out.println("[BWAutoLeave] CLICKED green glass at slot " + i + " (" + stack.getDisplayName().getString() + ")");
                        break;
                    }
                }
            }
        }
    }
}
