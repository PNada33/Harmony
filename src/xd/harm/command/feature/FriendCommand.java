package xd.harm.command.feature;

import xd.harm.command.interfaces.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.TextFormatting;
import java.util.Map;
import java.util.function.Consumer;
import xd.harm.config.FriendStorage;
import xd.harm.command.api.CommandException;
import xd.harm.utils.player.PlayerUtils;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FriendCommand implements Command {

    final Prefix prefix;
    final Logger logger;
    final Minecraft mc;

    @Override
    public void execute(Parameters parameters) {
        String action = parameters.asString(0).orElseThrow(() ->
                new CommandException(TextFormatting.RED + "× " + TextFormatting.WHITE + "Действия: " + TextFormatting.GRAY + "add · remove · clear · list"));

        String nickname = parameters.asString(1).orElse("");

        Map<String, Consumer<String>> actions = Map.of(
                "add", this::addFriend,
                "remove", this::removeFriend
        );

        Map<String, Runnable> simpleActions = Map.of(
                "clear", this::clearFriendList,
                "list", this::getFriendList
        );

        if (actions.containsKey(action)) {
            if (nickname.isEmpty()) {
                throw new CommandException(TextFormatting.RED + "× " + TextFormatting.WHITE + "Укажите никнейм");
            }
            actions.get(action).accept(nickname);
        } else if (simpleActions.containsKey(action)) {
            simpleActions.get(action).run();
        } else {
            throw new CommandException(TextFormatting.RED + "× " + TextFormatting.WHITE + "Действия: " + TextFormatting.GRAY + "add · remove · clear · list");
        }
    }

    @Override
    public String name() {
        return "friend";
    }

    @Override
    public String description() {
        return "Управление списком друзей";
    }

    private void addFriend(String name) {
        if (!PlayerUtils.isNameValid(name)) {
            logger.log(TextFormatting.DARK_GRAY + "" + TextFormatting.RED + "× " + TextFormatting.WHITE + "Некорректный никнейм");
            return;
        }
        if (name.equalsIgnoreCase(mc.player.getName().getString())) {
            logger.log(TextFormatting.DARK_GRAY + "" + TextFormatting.RED + "× " + TextFormatting.GRAY + "Ты и так свой лучший друг :)");
            return;
        }
        if (FriendStorage.isFriend(name)) {
            logger.log(TextFormatting.DARK_GRAY + "○ " + TextFormatting.WHITE + name + TextFormatting.GRAY + " уже в друзьях");
            return;
        }
        FriendStorage.add(name);
        logger.log(TextFormatting.DARK_GRAY + "" + TextFormatting.GREEN + "♡ " + TextFormatting.WHITE + name + TextFormatting.GRAY + " добавлен в друзья");
    }

    private void removeFriend(String name) {
        if (!FriendStorage.isFriend(name)) {
            logger.log(TextFormatting.DARK_GRAY + "" + TextFormatting.RED + "× " + TextFormatting.WHITE + name + TextFormatting.GRAY + " не в списке друзей");
            return;
        }
        FriendStorage.remove(name);
        logger.log(TextFormatting.DARK_GRAY + "" + TextFormatting.RED + "✗ " + TextFormatting.WHITE + name + TextFormatting.GRAY + " удалён из друзей");
    }

    private void getFriendList() {
        if (FriendStorage.getFriends().isEmpty()) {
            logger.log(TextFormatting.DARK_GRAY + "○ " + TextFormatting.GRAY + "Список друзей пуст");
            return;
        }
        logger.log(TextFormatting.DARK_GRAY + "┌ " + TextFormatting.WHITE + "Друзья " + TextFormatting.DARK_GRAY + "(" + FriendStorage.getFriends().size() + ")");
        for (String friend : FriendStorage.getFriends()) {
            TextFormatting heartColor = isFriendOnline(friend) ? TextFormatting.GREEN : TextFormatting.RED;
            logger.log(TextFormatting.DARK_GRAY + "│ " + heartColor + "♡ " + TextFormatting.WHITE + friend);
        }
        logger.log(TextFormatting.DARK_GRAY + "└───────────────");
    }

    private boolean isFriendOnline(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }

        if (mc.getConnection() != null) {
            for (NetworkPlayerInfo playerInfo : mc.getConnection().getPlayerInfoMap()) {
                if (playerInfo.getGameProfile() != null
                        && playerInfo.getGameProfile().getName().equalsIgnoreCase(name)) {
                    return true;
                }
            }
        }

        if (mc.world != null) {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player.getScoreboardName().equalsIgnoreCase(name)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void clearFriendList() {
        if (FriendStorage.getFriends().isEmpty()) {
            logger.log(TextFormatting.DARK_GRAY + "○ " + TextFormatting.GRAY + "Список уже пуст");
            return;
        }
        int count = FriendStorage.getFriends().size();
        FriendStorage.clear();
        logger.log(TextFormatting.DARK_GRAY + "" + TextFormatting.GREEN + "✓ " + TextFormatting.WHITE + "Удалено друзей: " + count);
    }
}
