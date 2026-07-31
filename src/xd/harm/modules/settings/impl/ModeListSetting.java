package xd.harm.modules.settings.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import xd.harm.modules.settings.Setting;

public class ModeListSetting extends Setting<List<BooleanSetting>> {
    private static final int MAX_DISPLAY_LENGTH = 12;
    private static final long SCROLL_INTERVAL = 5000;
    private static final long SCROLL_DURATION = 300;
    private static final int MAX_NAME_LENGTH = 15;
    private long componentCreationTime = System.currentTimeMillis();

    public ModeListSetting(String name, BooleanSetting... settings) {
        super(name, Arrays.asList(settings));
        if (settings.length == 0) {
            System.err.println("Warning: ModeListSetting '" + name + "' initialized with no settings");
        }
    }

    public BooleanSetting getValueByName(String settingName) {
        if (settingName == null) {
            System.err.println("Error: Setting name is null in ModeListSetting '" + getName() + "'");
            return new BooleanSetting("Fallback", false);
        }

        List<BooleanSetting> settings = get();
        if (settings == null || settings.isEmpty()) {
            System.err.println("Error: No settings available in ModeListSetting '" + getName() + "' for name '" + settingName + "'");
            return new BooleanSetting("Fallback", false);
        }

        BooleanSetting result = null;
        for (int i = 0, size = settings.size(); i < size; i++) {
            BooleanSetting setting = settings.get(i);
            if (setting.getName().equalsIgnoreCase(settingName)) {
                result = setting;
                break;
            }
        }

        if (result == null) {
            StringBuilder availableSettings = new StringBuilder();
            for (int i = 0, size = settings.size(); i < size; i++) {
                if (i > 0) {
                    availableSettings.append(", ");
                }
                availableSettings.append(settings.get(i).getName());
            }
            System.err.println("Warning: No BooleanSetting found for name '" + settingName + "' in ModeListSetting '" + getName() + "'. Available settings: " +
                    availableSettings);
            return new BooleanSetting("Fallback", false);
        }

        return result;
    }

    public String getNames() {
        List<String> includedOptions = new ArrayList<>();
        List<BooleanSetting> settings = get();
        if (settings != null) {
            for (BooleanSetting option : settings) {
                if (option.get()) {
                    includedOptions.add(option.getName());
                }
            }
        }

        String fullText = String.join(", ", includedOptions);

        if (fullText.length() <= MAX_DISPLAY_LENGTH) {
            return fullText;
        }

        return getScrolledText(fullText);
    }

    private String getScrolledText(String text) {
        long currentTime = System.currentTimeMillis();
        long cycleTime = currentTime % SCROLL_INTERVAL;

        if (cycleTime < SCROLL_DURATION) {
            float progress = (float) cycleTime / SCROLL_DURATION;
            int scrollAmount = (int) (progress * (text.length() - MAX_DISPLAY_LENGTH + 3));

            String paddedText = text + "   " + text;
            int startPos = scrollAmount;
            int endPos = Math.min(startPos + MAX_DISPLAY_LENGTH, paddedText.length());

            if (endPos > paddedText.length()) {
                endPos = paddedText.length();
            }

            String result = paddedText.substring(startPos, endPos);
            if (result.length() < MAX_DISPLAY_LENGTH) {
                result += paddedText.substring(0, MAX_DISPLAY_LENGTH - result.length());
            }

            return result;
        } else {
            return text.substring(0, Math.min(text.length(), MAX_DISPLAY_LENGTH));
        }
    }

    public String getScrolledName() {
        String name = getName();
        if (name.length() <= MAX_NAME_LENGTH) {
            return name;
        }

        long currentTime = System.currentTimeMillis();
        long timeSinceCreation = currentTime - componentCreationTime;

        long cycleTime = 4000;
        long pauseTime = 1500;
        long scrollTime = 2000;

        long currentCycle = timeSinceCreation % cycleTime;

        if (currentCycle < pauseTime) {
            return name.substring(0, Math.min(name.length(), MAX_NAME_LENGTH));
        } else if (currentCycle < pauseTime + scrollTime) {
            float progress = (float)(currentCycle - pauseTime) / scrollTime;
            progress = easeInOutQuad(progress);

            String paddedText = name + "   " + name;
            int maxScroll = name.length() + 3;
            int currentScroll = (int)(progress * maxScroll);

            int startPos = currentScroll % paddedText.length();
            int endPos = Math.min(startPos + MAX_NAME_LENGTH, paddedText.length());

            String result = paddedText.substring(startPos, endPos);
            if (result.length() < MAX_NAME_LENGTH) {
                result += paddedText.substring(0, MAX_NAME_LENGTH - result.length());
            }

            return result;
        } else {
            return name.substring(0, Math.min(name.length(), MAX_NAME_LENGTH));
        }
    }

    private float easeInOutQuad(float t) {
        return t < 0.5f ? 2 * t * t : -1 + (4 - 2 * t) * t;
    }

    public String getNamesNoScroll() {
        List<String> includedOptions = new ArrayList<>();
        List<BooleanSetting> settings = get();
        if (settings != null) {
            for (BooleanSetting option : settings) {
                if (option.get()) {
                    includedOptions.add(option.getName());
                }
            }
        }
        return String.join(", ", includedOptions);
    }

    public BooleanSetting get(int index) {
        List<BooleanSetting> settings = get();
        if (settings == null || index < 0 || index >= settings.size()) {
            System.err.println("Error: Invalid index " + index + " for ModeListSetting '" + getName() + "'");
            return new BooleanSetting("Fallback", false);
        }
        return settings.get(index);
    }

    @Override
    public ModeListSetting setVisible(Supplier<Boolean> bool) {
        return (ModeListSetting) super.setVisible(bool);
    }
}
