package xd.harm.modules.impl.misc;

import com.google.common.eventbus.Subscribe;
import xd.harm.Harmony;
import xd.harm.config.FriendStorage;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.ModeListSetting;
import xd.harm.modules.settings.impl.BooleanSetting;
import net.minecraft.client.Minecraft;

import java.util.Set;

@ModuleRegister(name = "StreamerMode", category = Category.Misc, desc = "\u0421\u0442\u0440\u0438\u043c\u0435\u0440 \u043c\u043e\u0434")
public class StreamerMode extends Module {

    private static final String FRIEND_FAKE_NAME = "Friend";
    private final BooleanSetting nickSetting = new BooleanSetting("Себя", true);
    private final BooleanSetting friendsSetting = new BooleanSetting("\u0414\u0440\u0443\u0437\u0435\u0439", false);
    public final ModeListSetting options = new ModeListSetting("\u0421\u043a\u0440\u044b\u0432\u0430\u0442\u044c", new BooleanSetting[]{nickSetting, friendsSetting});
    public static String fakeName = "";

    public StreamerMode() {
        addSettings(options);
    }

    @Subscribe
    private void onUpdate(EventUpdate e) {
        fakeName = "Protected";
    }

    public static String getReplaced(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        Harmony harmony = Harmony.getInstance();
        if (harmony == null || harmony.getModuleManager() == null) return input;

        StreamerMode streamerMode = harmony.getModuleManager().getStreamerMode();
        if (streamerMode == null || !streamerMode.isState()) return input;

        if (streamerMode.nickSetting.get() && Minecraft.getInstance().session != null) {
            input = replaceNameToken(input, Minecraft.getInstance().session.getUsername(), fakeName);
        }
        if (streamerMode.friendsSetting.get()) {
            Set<String> friends = FriendStorage.getFriends();
            if (!friends.isEmpty()) {
                for (String friend : friends) {
                    input = replaceNameToken(input, friend, FRIEND_FAKE_NAME);
                }
            }
        }
        return input;
    }

    private static String replaceNameToken(String input, String name, String replacement) {
        if (name == null || name.isEmpty() || replacement == null || input.length() < name.length()) {
            return input;
        }

        StringBuilder result = null;
        int searchFrom = 0;
        int copyFrom = 0;
        while (searchFrom <= input.length() - name.length()) {
            int index = indexOfIgnoreCase(input, name, searchFrom);
            if (index < 0) {
                break;
            }

            int end = index + name.length();
            if (isNameBoundaryBefore(input, index) && isNameBoundaryAfter(input, end)) {
                if (result == null) {
                    result = new StringBuilder(input.length());
                }
                result.append(input, copyFrom, index).append(replacement);
                copyFrom = end;
            }
            searchFrom = end;
        }

        if (result == null) {
            return input;
        }
        result.append(input, copyFrom, input.length());
        return result.toString();
    }

    private static int indexOfIgnoreCase(String input, String target, int fromIndex) {
        int max = input.length() - target.length();
        for (int i = fromIndex; i <= max; i++) {
            if (input.regionMatches(true, i, target, 0, target.length())) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isNameBoundaryBefore(String input, int index) {
        if (index <= 0) {
            return true;
        }

        int previousIndex = index - 1;
        if (previousIndex > 0 && input.charAt(previousIndex - 1) == '\u00a7') {
            return true;
        }
        return !isNameChar(input.charAt(previousIndex));
    }

    private static boolean isNameBoundaryAfter(String input, int index) {
        return index >= input.length() || !isNameChar(input.charAt(index));
    }

    private static boolean isNameChar(char c) {
        return c == '_' || Character.isLetterOrDigit(c);
    }
}
