package xd.harm.modules.impl.player;

import com.google.common.collect.Lists;
import com.google.common.eventbus.Subscribe;
import xd.harm.events.network.EventPacket;
import xd.harm.events.world.EventUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.ModeSetting;
import xd.harm.modules.settings.impl.StringSetting;
import xd.harm.utils.math.StopWatch;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.server.SDisconnectPacket;

import java.util.List;

@ModuleRegister(name = "PlayerFinder", category = Category.Player, desc = "Поиск игроков")
public class PlayerFinder extends Module {

    private static final int[] ANARCHY_NUMBERS = {102, 103, 104, 105, 106, 107, 108, 109, 110, 203, 204, 205, 206, 207, 208, 209, 210, 211, 212, 213, 214, 215, 216, 217, 218, 219, 220, 221, 222, 223, 224, 225, 226, 227, 228, 229, 302, 303, 304, 305, 306, 307, 308, 309, 310, 311, 312, 313, 314, 315, 316, 317, 318, 502, 503, 504, 505, 506, 507, 508, 509, 510, 511, 602, 603, 604, 605, 606};
    private static final int[] SPOOKYTIME_ANARCHIES = {101, 102, 103, 104, 105, 106, 107, 108, 201, 202, 203, 204, 205, 206, 207, 208, 209, 210, 211, 212, 213, 214, 215, 301, 302, 303, 304, 305, 306, 401, 402, 501, 502, 503, 504, 601, 602, 603};

    private final StringSetting playerName = new StringSetting("Ник", "Pid0ras", "Укажите имя пользователя игрока для поиска");
    private final ModeSetting serverMode = new ModeSetting("Сервер", "Фантайм", "Фантайм", "CпукиТайм");
    private final BooleanSetting searchPremium = new BooleanSetting("Поиск премиум", false);

    private int currentAnarchyIndex = 0;
    private boolean isInHub = false;
    private boolean hubCommandSent = false;
    private boolean serverCommandSent = false;
    private String currentServer = "";
    private final List<String> checkedServers = Lists.newArrayList();
    private final StopWatch hubTimer = new StopWatch();
    private final StopWatch serverTimer = new StopWatch();
    private final StopWatch connectionTimer = new StopWatch();

    public PlayerFinder() {
        addSettings(playerName, serverMode, searchPremium);
    }

    @Subscribe
    private void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        int[] anarchyList = serverMode.is("CпукиТайм") ? SPOOKYTIME_ANARCHIES : ANARCHY_NUMBERS;

        if (!isInHub && currentAnarchyIndex == 0) {
            if (!hubCommandSent) {
                mc.player.sendChatMessage("/hub");
                hubCommandSent = true;
                hubTimer.reset();
            }
            if (hubTimer.isReached(1500)) {
                isInHub = true;
                serverTimer.reset();
            }
            return;
        }

        if (isInHub && currentAnarchyIndex < anarchyList.length) {
            if (serverCommandSent) {
                if (connectionTimer.isReached(280)) {
                    if (findPlayer(playerName.get())) {
                        print("Нашёл игрока " + playerName.get() + " на анархии " + currentServer);
                        toggle();
                        return;
                    }
                    serverCommandSent = false;
                    serverTimer.reset();
                }
                return;
            }

            if (!serverCommandSent && serverTimer.isReached(1000)) {
                currentServer = "/an" + anarchyList[currentAnarchyIndex];
                if (!checkedServers.contains(currentServer)) {
                    mc.player.sendChatMessage(currentServer);
                    checkedServers.add(currentServer);
                    print("Проверяю " + currentServer);
                    currentAnarchyIndex++;
                    serverCommandSent = true;
                    connectionTimer.reset();
                }
            }
        } else if (currentAnarchyIndex >= anarchyList.length) {
            print("Игрок " + playerName.get() + " не найден.");
            toggle();
        }
    }

    @Subscribe
    private void onPacket(EventPacket event) {
        if (event.isReceive()) {
            IPacket<?> packet = event.getPacket();
            if (packet instanceof SDisconnectPacket) {
                toggle();
            }
        }
    }

    private boolean findPlayer(String username) {
        return mc.player.connection.getPlayerInfoMap().stream()
                .map(NetworkPlayerInfo::getGameProfile)
                .map(profile -> profile.getName())
                .anyMatch(name -> name.equalsIgnoreCase(username));
    }

    @Override
    public boolean onEnable() {
        super.onEnable();
        if (mc.player != null) {
            mc.player.closeScreen();
        }
        currentAnarchyIndex = 0;
        isInHub = false;
        hubCommandSent = false;
        serverCommandSent = false;
        currentServer = "";
        checkedServers.clear();
        hubTimer.reset();
        serverTimer.reset();
        connectionTimer.reset();
        return false;
    }

    @Override
    public boolean onDisable() {
        super.onDisable();
        currentAnarchyIndex = 0;
        isInHub = false;
        hubCommandSent = false;
        serverCommandSent = false;
        currentServer = "";
        checkedServers.clear();
        hubTimer.reset();
        serverTimer.reset();
        connectionTimer.reset();
        return false;
    }
}
