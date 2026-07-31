package xd.harm.modules.impl.misc;

import com.google.common.eventbus.Subscribe;
import xd.harm.events.network.EventPacket;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.api.Notify;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.CategorySetting;
import xd.harm.modules.settings.impl.SliderSetting;
import xd.harm.ui.notify.NotificationManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SChatPacket;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@ModuleRegister(name = "Notifications", category = Category.Misc, desc = "Уведомления")
public class Notifications extends Module {

    private static final long SPEC_COOLDOWN_MS = 2000L;
    private static final float ARMOR_WARN_RESET_DELTA = 2.0f;
    private static final float LOW_HP_RESET_DELTA = 1.0f;

    private final CategorySetting moduleCategory = new CategorySetting("Модули");
    private final CategorySetting specCategory = new CategorySetting("Спек");
    private final CategorySetting potionCategory = new CategorySetting("Эффекты");
    private final CategorySetting armorCategory = new CategorySetting("Броня");
    private final CategorySetting healthCategory = new CategorySetting("Здоровье");

    public final BooleanSetting moduleToggleNotify = new BooleanSetting("Включение модулей", true);
    public final BooleanSetting specNotify = new BooleanSetting("Игрок просит спек", true);
    public final SliderSetting specRadius = new SliderSetting("Радиус спек", 50, 5, 100, 1)
            .setVisible(() -> specNotify.get());
    public final BooleanSetting ftHelperNotify = new BooleanSetting("FtHelper уведомления", true);
    public final BooleanSetting potionNotify = new BooleanSetting("Эффект", true);
    public final SliderSetting potionWarnSeconds = new SliderSetting("Скоро конец (сек)", 10, 1, 60, 1)
            .setVisible(() -> potionNotify.get());
    public final BooleanSetting potionEndNotify = new BooleanSetting("Окончание эффекта", true)
            .setVisible(() -> potionNotify.get());
    public final BooleanSetting armorWarnNotify = new BooleanSetting("Броня скоро сломается", true);
    public final SliderSetting armorWarnPercent = new SliderSetting("Порог брони (%)", 10, 1, 50, 1)
            .setVisible(() -> armorWarnNotify.get());
    public final BooleanSetting lowHpNotify = new BooleanSetting("Мало хп", true);
    public final SliderSetting lowHpThreshold = new SliderSetting("Порог хп", 6.0f, 1.0f, 20.0f, 0.5f)
            .setVisible(() -> lowHpNotify.get());

    private final Map<Effect, PotionSnapshot> lastPotions = new HashMap<>();
    private final Set<Effect> warnedPotions = new HashSet<>();
    private final Map<UUID, Long> specCooldowns = new HashMap<>();
    private final Set<Integer> warnedArmorSlots = new HashSet<>();
    private final Item[] lastArmorItems = new Item[4];
    private final int[] lastArmorMax = new int[4];
    private boolean lowHpWarned = false;

    public Notifications() {
        addSettings(
                moduleCategory,
                moduleToggleNotify,
                ftHelperNotify,
                specCategory,
                specNotify,
                specRadius,
                potionCategory,
                potionNotify,
                potionWarnSeconds,
                potionEndNotify,
                armorCategory,
                armorWarnNotify,
                armorWarnPercent,
                healthCategory,
                lowHpNotify,
                lowHpThreshold
        );
    }

    public boolean isModuleToggleNotifyEnabled() {
        return moduleToggleNotify.get();
    }

    public boolean isFtHelperNotifyEnabled() {
        return ftHelperNotify.get();
    }

    @Subscribe
    public void onPacket(EventPacket event) {
        if (!event.isReceivePacket()) {
            return;
        }
        if (!(event.getPacket() instanceof SChatPacket)) {
            return;
        }
        if (mc.player == null || mc.world == null) {
            return;
        }

        SChatPacket packet = (SChatPacket) event.getPacket();
        String message = packet.getChatComponent().getString();
        if (message == null || message.isEmpty()) {
            return;
        }

        if (specNotify.get()) {
            handleSpecRequest(message);
        }
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (mc.player == null) {
            return;
        }

        if (armorWarnNotify.get()) {
            handleArmorWarnings();
        } else {
            clearArmorState();
        }

        if (lowHpNotify.get()) {
            handleLowHpWarning();
        } else {
            lowHpWarned = false;
        }

        if (potionNotify.get()) {
            handlePotionWarnings();
        } else {
            clearPotionState();
        }

        cleanupSpecCooldowns();
    }

    private void handlePotionWarnings() {
        Map<Effect, PotionSnapshot> currentPotions = new HashMap<>();
        int warnTicks = Math.max(1, (int) (potionWarnSeconds.get() * 20.0f));

        for (EffectInstance effectInstance : mc.player.getActivePotionEffects()) {
            if (effectInstance.getDuration() <= 0) {
                continue;
            }
            Effect effect = effectInstance.getPotion();
            PotionSnapshot snapshot = new PotionSnapshot(effectInstance);
            currentPotions.put(effect, snapshot);

            PotionSnapshot lastSnapshot = lastPotions.get(effect);
            if (lastSnapshot == null || snapshot.duration > lastSnapshot.duration + 5) {
                warnedPotions.remove(effect);
            }

            if (snapshot.duration <= warnTicks && !warnedPotions.contains(effect)) {
                Notify.NOTIFICATION_MANAGER.add("Эффект " + formatPotionName(effect, snapshot.amplifier) + " скоро закончится" , "", 3, NotificationManager.NotificationType.POTION_WARN, effect);
                warnedPotions.add(effect);
            }
        }

        if (potionEndNotify.get()) {
            for (Map.Entry<Effect, PotionSnapshot> entry : lastPotions.entrySet()) {
                if (!currentPotions.containsKey(entry.getKey())) {
                    PotionSnapshot snapshot = entry.getValue();
                    Notify.NOTIFICATION_MANAGER.add(
                            "Эффект " + formatPotionName(entry.getKey(), snapshot.amplifier) + " закончился",
                            "",
                            3,
                            NotificationManager.NotificationType.POTION_END,
                            entry.getKey()
                    );
                }
            }
        }

        lastPotions.clear();
        lastPotions.putAll(currentPotions);
    }

    @Override
    public boolean onDisable() {
        clearPotionState();
        clearArmorState();
        lowHpWarned = false;
        specCooldowns.clear();
        return super.onDisable();
    }

    private void clearPotionState() {
        lastPotions.clear();
        warnedPotions.clear();
    }

    private void clearArmorState() {
        warnedArmorSlots.clear();
        for (int i = 0; i < lastArmorItems.length; i++) {
            lastArmorItems[i] = null;
            lastArmorMax[i] = 0;
        }
    }

    private void handleLowHpWarning() {
        float health = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        float threshold = lowHpThreshold.get();
        if (health <= threshold) {
            if (!lowHpWarned) {
                String value = String.format(Locale.ROOT, "%.1f", health);
                Notify.NOTIFICATION_MANAGER.add("Мало хп: " + value, "", 3);
                lowHpWarned = true;
            }
        } else if (health >= threshold + LOW_HP_RESET_DELTA) {
            lowHpWarned = false;
        }
    }

    private void cleanupSpecCooldowns() {
        long now = System.currentTimeMillis();
        specCooldowns.entrySet().removeIf(entry -> now - entry.getValue() > 15000L);
    }

    private void handleArmorWarnings() {
        int size = mc.player.inventory.armorInventory.size();
        for (int slot = 0; slot < size; slot++) {
            ItemStack stack = mc.player.inventory.armorInventory.get(slot);
            if (stack == null || stack.isEmpty() || !stack.isDamageable()) {
                clearArmorSlot(slot);
                continue;
            }

            Item item = stack.getItem();
            int maxDamage = stack.getMaxDamage();
            if (maxDamage <= 0) {
                clearArmorSlot(slot);
                continue;
            }

            if (slot < lastArmorItems.length && (lastArmorItems[slot] != item || lastArmorMax[slot] != maxDamage)) {
                lastArmorItems[slot] = item;
                lastArmorMax[slot] = maxDamage;
                warnedArmorSlots.remove(slot);
            }

            int remaining = maxDamage - stack.getDamage();
            float remainingPercent = (remaining / (float) maxDamage) * 100.0f;

            float warnPercent = armorWarnPercent.get();
            float resetPercent = warnPercent + ARMOR_WARN_RESET_DELTA;
            if (remainingPercent <= warnPercent) {
                if (!warnedArmorSlots.contains(slot)) {
                    String name = stack.getDisplayName().getString();
                    Notify.NOTIFICATION_MANAGER.add(
                            name + " скоро сломается" ,
                            "",
                            3,
                            NotificationManager.NotificationType.ARMOR_WARN
                    );
                    warnedArmorSlots.add(slot);
                }
            } else if (remainingPercent >= resetPercent) {
                warnedArmorSlots.remove(slot);
            }
        }
    }

    private void clearArmorSlot(int slot) {
        warnedArmorSlots.remove(slot);
        if (slot >= 0 && slot < lastArmorItems.length) {
            lastArmorItems[slot] = null;
            lastArmorMax[slot] = 0;
        }
    }

    private void handleSpecRequest(String message) {
        if (!containsSpecRequest(message)) {
            return;
        }

        PlayerEntity sender = findPlayerInMessage(message);
        if (sender == null || sender == mc.player) {
            return;
        }

        if (mc.player.getDistance(sender) > specRadius.get()) {
            return;
        }

        long now = System.currentTimeMillis();
        UUID senderId = sender.getUniqueID();
        long lastNotify = specCooldowns.getOrDefault(senderId, 0L);
        if (now - lastNotify < SPEC_COOLDOWN_MS) {
            return;
        }
        specCooldowns.put(senderId, now);

        Notify.NOTIFICATION_MANAGER.add("спек " + sender.getName().getString(), "", 3, NotificationManager.NotificationType.SPEC);
    }

    private boolean containsSpecRequest(String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        return containsToken(lower, "spec") || containsToken(lower, "spek") || containsToken(lower, "спек");
    }

    private boolean containsToken(String text, String token) {
        int index = text.indexOf(token);
        while (index != -1) {
            int before = index - 1;
            int after = index + token.length();
            boolean beforeOk = before < 0 || !Character.isLetterOrDigit(text.charAt(before));
            boolean afterOk = after >= text.length() || !Character.isLetterOrDigit(text.charAt(after));
            if (beforeOk && afterOk) {
                return true;
            }
            index = text.indexOf(token, index + 1);
        }
        return false;
    }

    private PlayerEntity findPlayerInMessage(String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == null) {
                continue;
            }
            String name = player.getName().getString();
            if (name == null || name.isEmpty()) {
                continue;
            }
            if (lower.contains(name.toLowerCase(Locale.ROOT))) {
                return player;
            }
        }
        return null;
    }

    private String formatPotionName(Effect effect, int amplifier) {
        String name = I18n.format(effect.getName());
        if (amplifier >= 1 && amplifier <= 9) {
            name += " " + I18n.format("enchantment.level." + (amplifier + 1));
        } else if (amplifier >= 1) {
            name += " " + (amplifier + 1);
        }
        return name;
    }

    private static class PotionSnapshot {
        private final int duration;
        private final int amplifier;

        private PotionSnapshot(EffectInstance instance) {
            this.duration = instance.getDuration();
            this.amplifier = instance.getAmplifier();
        }
    }
}
