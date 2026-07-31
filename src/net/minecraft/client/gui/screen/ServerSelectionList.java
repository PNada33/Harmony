package net.minecraft.client.gui.screen;

import com.google.common.collect.Lists;
import com.google.common.hash.Hashing;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.gui.widget.list.ExtendedList;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.network.LanServerInfo;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.util.DefaultUncaughtExceptionHandler;
import net.minecraft.util.IReorderingProcessor;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SharedConstants;
import net.minecraft.util.Util;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

public class ServerSelectionList extends ExtendedList<ServerSelectionList.Entry> {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final ThreadPoolExecutor EXECUTOR = new ScheduledThreadPoolExecutor(5, new ThreadFactoryBuilder().setNameFormat("Server Pinger #%d").setDaemon(true).setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER)).build());
    private static final ResourceLocation UNKNOWN_SERVER = new ResourceLocation("textures/misc/unknown_server.png");
    private static final ResourceLocation SERVER_SELECTION = new ResourceLocation("textures/gui/server_selection.png");
    private static final ITextComponent SCANNING = new TranslationTextComponent("lanServer.scanning");
    private static final ITextComponent CANNOT_RESOLVE = new TranslationTextComponent("multiplayer.status.cannot_resolve").mergeStyle(TextFormatting.DARK_RED);
    private static final ITextComponent CANNOT_CONNECT = new TranslationTextComponent("multiplayer.status.cannot_connect").mergeStyle(TextFormatting.DARK_RED);
    private static final ITextComponent INCOMPATIBLE = new TranslationTextComponent("multiplayer.status.incompatible");
    private static final ITextComponent NO_CONNECTION = new TranslationTextComponent("multiplayer.status.no_connection");
    private static final ITextComponent PINGING = new TranslationTextComponent("multiplayer.status.pinging");
    private final MultiplayerScreen owner;
    private final List<NormalEntry> serverListInternet = Lists.newArrayList();
    private final Entry lanScanEntry = new LanScanEntry();
    private final List<LanDetectedEntry> serverListLan = Lists.newArrayList();

    public ServerSelectionList(MultiplayerScreen ownerIn, Minecraft mcIn, int widthIn, int heightIn, int topIn, int bottomIn, int slotHeightIn)
    {
        super(mcIn, widthIn, heightIn, topIn, bottomIn, slotHeightIn);
        this.owner = ownerIn;
    }

    private void setList() {
        this.clearEntries();
        this.serverListInternet.forEach(this::addEntry);
        this.addEntry(this.lanScanEntry);
        this.serverListLan.forEach(this::addEntry);
    }

    public void setSelected(Entry entry) {
        super.setSelected(entry);
        if (this.getSelected() instanceof NormalEntry) {
            NarratorChatListener.INSTANCE.say(new TranslationTextComponent("narrator.select", ((NormalEntry) this.getSelected()).server.serverName).getString());
        }
        this.owner.func_214295_b();
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        Entry entry = this.getSelected();
        return entry != null && entry.keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    protected void moveSelection(Ordering ordering) {
        this.func_241572_a_(ordering, entry -> !(entry instanceof LanScanEntry));
    }

    public void updateOnlineServers(ServerList serverList) {
        this.serverListInternet.clear();
        if (serverList.countServers() > 0) {
            serverList.set(0, new ServerData("SpookyTime", "spookytime.net", false));
        } else {
            serverList.addServerData(new ServerData("SpookyTime", "spookytime.net", false));
        }
        for (int i = 0; i < serverList.countServers(); ++i) {
            if (i == 0) {
                this.serverListInternet.add(new PenisEntry(this.owner, serverList.getServerData(i), true));
                continue;
            }
            this.serverListInternet.add(new NormalEntry(this.owner, serverList.getServerData(i)));
        }
        this.setList();
    }

    public void updateNetworkServers(List<LanServerInfo> lanServers) {
        this.serverListLan.clear();
        for (LanServerInfo lanServer : lanServers) {
            this.serverListLan.add(new LanDetectedEntry(this.owner, lanServer));
        }
        this.setList();
    }

    protected int getScrollbarPosition() {
        return super.getScrollbarPosition() + 30;
    }

    public int getRowWidth() {
        return super.getRowWidth() + 85;
    }

    protected boolean isFocused() {
        return this.owner.getListener() == this;
    }

    public abstract static class Entry extends AbstractListEntry<Entry> {
    }

    public static class LanDetectedEntry extends Entry {
        private static final ITextComponent LAN_SERVER = new TranslationTextComponent("lanServer.title");
        private static final ITextComponent HIDDEN_ADDRESS = new TranslationTextComponent("selectServer.hiddenAddress");
        private final MultiplayerScreen screen;
        protected final Minecraft mc;
        protected final LanServerInfo serverData;
        private long lastClickTime;

        protected LanDetectedEntry(MultiplayerScreen screen, LanServerInfo serverData) {
            this.screen = screen;
            this.serverData = serverData;
            this.mc = Minecraft.getInstance();
        }

        public void render(MatrixStack matrixStack, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTicks) {
            this.mc.fontRenderer.func_243248_b(matrixStack, LAN_SERVER, left + 32 + 3, top + 1, 0xFFFFFF);
            this.mc.fontRenderer.drawString(matrixStack, this.serverData.getServerMotd(), left + 32 + 3, top + 12, 0x808080);
            if (this.mc.gameSettings.hideServerAddress) {
                this.mc.fontRenderer.func_243248_b(matrixStack, HIDDEN_ADDRESS, left + 32 + 3, top + 12 + 11, 0x303030);
            } else {
                this.mc.fontRenderer.drawString(matrixStack, this.serverData.getServerIpPort(), left + 32 + 3, top + 12 + 11, 0x303030);
            }
        }

        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            this.screen.func_214287_a(this);
            if (Util.milliTime() - this.lastClickTime < 250L) {
                this.screen.connectToSelected();
            }
            this.lastClickTime = Util.milliTime();
            return false;
        }

        public LanServerInfo getServerData() {
            return this.serverData;
        }
    }

    public static class LanScanEntry extends Entry {
        private final Minecraft mc = Minecraft.getInstance();

        public void render(MatrixStack matrixStack, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTicks) {
            int y = top + height / 2 - 9 / 2;
            this.mc.fontRenderer.func_243248_b(matrixStack, SCANNING, this.mc.currentScreen.width / 2 - this.mc.fontRenderer.getStringPropertyWidth(SCANNING) / 2, y, 0xFFFFFF);
            String dots;
            switch ((int) (Util.milliTime() / 300L % 4L)) {
                case 0:
                default:
                    dots = "O o o";
                    break;
                case 1:
                case 3:
                    dots = "o O o";
                    break;
                case 2:
                    dots = "o o O";
            }
            this.mc.fontRenderer.drawString(matrixStack, dots, this.mc.currentScreen.width / 2 - this.mc.fontRenderer.getStringWidth(dots) / 2, y + 9, 0x808080);
        }
    }

    public class NormalEntry extends Entry {
        public final MultiplayerScreen owner;
        public final Minecraft mc;
        public final ServerData server;
        public final ResourceLocation serverIcon;
        public String lastIconB64;
        public DynamicTexture icon;
        public long lastClickTime;

        protected NormalEntry(MultiplayerScreen owner, ServerData server) {
            this.owner = owner;
            this.server = server;
            this.mc = Minecraft.getInstance();
            this.serverIcon = new ResourceLocation("servers/" + Hashing.sha1().hashUnencodedChars(server.serverIP) + "/icon");
            this.icon = (DynamicTexture) this.mc.getTextureManager().getTexture(this.serverIcon);
        }

        public void render(MatrixStack matrixStack, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTicks) {
            if (!this.server.pinged) {
                this.server.pinged = true;
                this.server.pingToServer = -2L;
                this.server.serverMOTD = StringTextComponent.EMPTY;
                this.server.populationInfo = StringTextComponent.EMPTY;
                EXECUTOR.submit(() -> {
                    try {
                        this.owner.getOldServerPinger().ping(this.server, () -> this.mc.execute(this::func_241613_a_));
                    } catch (UnknownHostException e) {
                        this.server.pingToServer = -1L;
                        this.server.serverMOTD = CANNOT_RESOLVE;
                    } catch (Exception e) {
                        this.server.pingToServer = -1L;
                        this.server.serverMOTD = CANNOT_CONNECT;
                    }
                });
            }
            boolean incompatible = this.server.version != SharedConstants.getVersion().getProtocolVersion();
            this.mc.fontRenderer.drawString(matrixStack, this.server.serverName, left + 32 + 3, top + 1, 0xFFFFFF);
            List<IReorderingProcessor> motdLines = this.mc.fontRenderer.trimStringToWidth(this.server.serverMOTD, width - 32 - 2);
            for (int i = 0; i < Math.min(motdLines.size(), 2); ++i) {
                this.mc.fontRenderer.func_238422_b_(matrixStack, motdLines.get(i), left + 32 + 3, top + 12 + 9 * i, 0x808080);
            }
            ITextComponent populationInfo = incompatible ? this.server.gameVersion.deepCopy().mergeStyle(TextFormatting.RED) : this.server.populationInfo;
            int infoWidth = this.mc.fontRenderer.getStringPropertyWidth(populationInfo);
            this.mc.fontRenderer.func_243248_b(matrixStack, populationInfo, left + width - infoWidth - 15 - 2, top + 1, 0x808080);
            int pingIcon = 0;
            int pingLevel;
            List<ITextComponent> playerList;
            ITextComponent status;
            if (incompatible) {
                pingLevel = 5;
                status = INCOMPATIBLE;
                playerList = this.server.playerList;
            } else if (this.server.pinged && this.server.pingToServer != -2L) {
                if (this.server.pingToServer < 0L) {
                    pingLevel = 5;
                } else if (this.server.pingToServer < 150L) {
                    pingLevel = 0;
                } else if (this.server.pingToServer < 300L) {
                    pingLevel = 1;
                } else if (this.server.pingToServer < 600L) {
                    pingLevel = 2;
                } else if (this.server.pingToServer < 1000L) {
                    pingLevel = 3;
                } else {
                    pingLevel = 4;
                }
                if (this.server.pingToServer < 0L) {
                    status = NO_CONNECTION;
                    playerList = Collections.emptyList();
                } else {
                    status = new TranslationTextComponent("multiplayer.status.ping", this.server.pingToServer);
                    playerList = this.server.playerList;
                }
            } else {
                pingIcon = 1;
                pingLevel = (int) (Util.milliTime() / 100L + (index * 2) & 7L);
                if (pingLevel > 4) {
                    pingLevel = 8 - pingLevel;
                }
                status = PINGING;
                playerList = Collections.emptyList();
            }
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            this.mc.getTextureManager().bindTexture(AbstractGui.GUI_ICONS_LOCATION);
            AbstractGui.blit(matrixStack, left + width - 15, top, pingIcon * 10, 176 + pingLevel * 8, 10, 8, 256, 256);
            String iconData = this.server.getBase64EncodedIconData();
            if (!Objects.equals(iconData, this.lastIconB64)) {
                if (this.func_241614_a_(iconData)) {
                    this.lastIconB64 = iconData;
                } else {
                    this.server.setBase64EncodedIconData(null);
                    this.func_241613_a_();
                }
            }
            if (this.icon != null) {
                this.func_238859_a_(matrixStack, left, top, this.serverIcon);
            } else {
                this.func_238859_a_(matrixStack, left, top, UNKNOWN_SERVER);
            }
            int relativeMouseX = mouseX - left;
            int relativeMouseY = mouseY - top;
            if (relativeMouseX >= width - 15 && relativeMouseX <= width - 5 && relativeMouseY >= 0 && relativeMouseY <= 8) {
                this.owner.func_238854_b_(Collections.singletonList(status));
            } else if (relativeMouseX >= width - infoWidth - 15 - 2 && relativeMouseX <= width - 15 - 2 && relativeMouseY >= 0 && relativeMouseY <= 8) {
                this.owner.func_238854_b_(playerList);
            }
            if (this.mc.gameSettings.touchscreen || hovered) {
                this.mc.getTextureManager().bindTexture(SERVER_SELECTION);
                AbstractGui.fill(matrixStack, left, top, left + 32, top + 32, 0xA0A0A0A0);
                RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                int relativeX = mouseX - left;
                int relativeY = mouseY - top;
                if (this.canJoin()) {
                    AbstractGui.blit(matrixStack, left, top, relativeX < 32 && relativeX > 16 ? 0.0F : 0.0F, 32.0F, 32, 32, 256, 256);
                }
                if (index > 0) {
                    AbstractGui.blit(matrixStack, left, top, relativeX < 16 && relativeY < 16 ? 96.0F : 96.0F, 32.0F, 32, 32, 256, 256);
                }
                if (index < this.owner.getServerList().countServers() - 1) {
                    AbstractGui.blit(matrixStack, left, top, relativeX < 16 && relativeY > 16 ? 64.0F : 64.0F, 32.0F, 32, 32, 256, 256);
                }
            }
        }

        public void func_241613_a_() {
            this.owner.getServerList().saveServerList();
        }

        protected void func_238859_a_(MatrixStack matrixStack, int x, int y, ResourceLocation resource) {
            this.mc.getTextureManager().bindTexture(resource);
            RenderSystem.enableBlend();
            AbstractGui.blit(matrixStack, x, y, 0.0F, 0.0F, 32, 32, 32, 32);
            RenderSystem.disableBlend();
        }

        boolean canJoin() {
            return true;
        }

        boolean func_241614_a_(String iconData) {
            if (iconData == null) {
                this.mc.getTextureManager().deleteTexture(this.serverIcon);
                if (this.icon != null && this.icon.getTextureData() != null) {
                    this.icon.getTextureData().close();
                }
                this.icon = null;
            } else {
                try {
                    NativeImage image = NativeImage.readBase64(iconData);
                    Validate.validState(image.getWidth() == 64, "Must be 64 pixels wide");
                    Validate.validState(image.getHeight() == 64, "Must be 64 pixels high");
                    if (this.icon == null) {
                        this.icon = new DynamicTexture(image);
                    } else {
                        this.icon.setTextureData(image);
                        this.icon.updateDynamicTexture();
                    }
                    this.mc.getTextureManager().loadTexture(this.serverIcon, this.icon);
                } catch (Throwable t) {
                    LOGGER.error("Invalid icon for server {} ({})", this.server.serverName, this.server.serverIP, t);
                    return false;
                }
            }
            return true;
        }

        public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
            if (Screen.hasShiftDown()) {
                ServerSelectionList list = this.owner.serverListSelector;
                int index = list.getEventListeners().indexOf(this);
                if (keyCode == 264 && index < this.owner.getServerList().countServers() - 1 || keyCode == 265 && index > 0) {
                    this.func_228196_a_(index, keyCode == 264 ? index + 1 : index - 1);
                    return true;
                }
            }
            return super.keyPressed(keyCode, scanCode, modifiers);
        }

        private void func_228196_a_(int fromIndex, int toIndex) {
            this.owner.getServerList().swapServers(fromIndex, toIndex);
            this.owner.serverListSelector.updateOnlineServers(this.owner.getServerList());
            Entry entry = this.owner.serverListSelector.getEventListeners().get(toIndex);
            this.owner.serverListSelector.setSelected(entry);
            ServerSelectionList.this.ensureVisible(entry);
        }

        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            double relativeX = mouseX - ServerSelectionList.this.getRowLeft();
            double relativeY = mouseY - ServerSelectionList.this.getRowTop(ServerSelectionList.this.getEventListeners().indexOf(this));
            if (relativeX <= 32.0D) {
                if (relativeX < 32.0D && relativeX > 16.0D && this.canJoin()) {
                    this.owner.func_214287_a(this);
                    this.owner.connectToSelected();
                    return true;
                }
                int index = this.owner.serverListSelector.getEventListeners().indexOf(this);
                if (relativeX < 16.0D && relativeY < 16.0D && index > 0) {
                    this.func_228196_a_(index, index - 1);
                    return true;
                }
                if (relativeX < 16.0D && relativeY > 16.0D && index < this.owner.getServerList().countServers() - 1) {
                    this.func_228196_a_(index, index + 1);
                    return true;
                }
            }
            this.owner.func_214287_a(this);
            if (Util.milliTime() - this.lastClickTime < 250L) {
                this.owner.connectToSelected();
            }
            this.lastClickTime = Util.milliTime();
            return false;
        }

        public ServerData getServerData() {
            return this.server;
        }
    }

    public class PenisEntry extends NormalEntry {
        boolean isExpa;

        protected PenisEntry(MultiplayerScreen owner, ServerData server, boolean isExpa) {
            super(owner, server);
            this.isExpa = isExpa;
        }

        public void render(MatrixStack matrixStack, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTicks) {
            if (!this.server.pinged) {
                this.server.pinged = true;
                this.server.pingToServer = -2L;
                this.server.serverMOTD = StringTextComponent.EMPTY;
                this.server.populationInfo = StringTextComponent.EMPTY;
                EXECUTOR.submit(() -> {
                    try {
                        this.owner.getOldServerPinger().ping(this.server, () -> this.mc.execute(this::func_241613_a_));
                    } catch (UnknownHostException e) {
                        this.server.pingToServer = -1L;
                        this.server.serverMOTD = CANNOT_RESOLVE;
                    } catch (Exception e) {
                        this.server.pingToServer = -1L;
                        this.server.serverMOTD = CANNOT_CONNECT;
                    }
                });
            }
            if (isExpa) {
                for (int i = 0; i < height; i++) {
                    float t = (float) i / (height - 1);
                    int color = interpolateColor(0xB44CAF50, 0xB4000000, t);
                    AbstractGui.fill(matrixStack, left, top + i, left + width - 5, top + i + 1, color);
                }
            } else {
                for (int i = 0; i < height; i++) {
                    float t = (float) i / (height - 1);
                    int color = interpolateColor(0xB4FC8510, 0xB4000000, t);
                    AbstractGui.fill(matrixStack, left, top + i, left + width - 5, top + i + 1, color);
                }
            }
            this.mc.fontRenderer.drawString(matrixStack, this.server.serverName, left + 32 + 3, top + 1, 0xFFFFFF);
            List<IReorderingProcessor> motdLines = this.mc.fontRenderer.trimStringToWidth(this.server.serverMOTD, width - 32 - 2);
            for (int i = 0; i < Math.min(motdLines.size(), 2); ++i) {
                this.mc.fontRenderer.func_238422_b_(matrixStack, motdLines.get(i), left + 32 + 3, top + 12 + 9 * i, 0x808080);
            }
            ITextComponent populationInfo = this.server.version != SharedConstants.getVersion().getProtocolVersion() ? this.server.gameVersion.deepCopy().mergeStyle(TextFormatting.RED) : this.server.populationInfo;
            int infoWidth = this.mc.fontRenderer.getStringPropertyWidth(populationInfo);
            this.mc.fontRenderer.func_243248_b(matrixStack, populationInfo, left + width - infoWidth - 15 - 2, top + 1, 0x808080);
            int pingIcon = 0;
            int pingLevel;
            List<ITextComponent> playerList;
            ITextComponent status;
            if (this.server.version != SharedConstants.getVersion().getProtocolVersion()) {
                pingLevel = 5;
                status = INCOMPATIBLE;
                playerList = this.server.playerList;
            } else if (this.server.pinged && this.server.pingToServer != -2L) {
                if (this.server.pingToServer < 0L) {
                    pingLevel = 5;
                } else if (this.server.pingToServer < 150L) {
                    pingLevel = 0;
                } else if (this.server.pingToServer < 300L) {
                    pingLevel = 1;
                } else if (this.server.pingToServer < 600L) {
                    pingLevel = 2;
                } else if (this.server.pingToServer < 1000L) {
                    pingLevel = 3;
                } else {
                    pingLevel = 4;
                }
                if (this.server.pingToServer < 0L) {
                    status = NO_CONNECTION;
                    playerList = Collections.emptyList();
                } else {
                    status = new TranslationTextComponent("multiplayer.status.ping", this.server.pingToServer);
                    playerList = this.server.playerList;
                }
            } else {
                pingIcon = 1;
                pingLevel = (int) (Util.milliTime() / 100L + (index * 2) & 7L);
                if (pingLevel > 4) {
                    pingLevel = 8 - pingLevel;
                }
                status = PINGING;
                playerList = Collections.emptyList();
            }
            RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
            this.mc.getTextureManager().bindTexture(AbstractGui.GUI_ICONS_LOCATION);
            AbstractGui.blit(matrixStack, left + width - 15, top, pingIcon * 10, 176 + pingLevel * 8, 10, 8, 256, 256);
            String iconData = this.server.getBase64EncodedIconData();
            if (!Objects.equals(iconData, this.lastIconB64)) {
                if (this.func_241614_a_(iconData)) {
                    this.lastIconB64 = iconData;
                } else {
                    this.server.setBase64EncodedIconData(null);
                    this.func_241613_a_();
                }
            }
            if (this.icon != null) {
                this.func_238859_a_(matrixStack, left, top, this.serverIcon);
            } else {
                this.func_238859_a_(matrixStack, left, top, UNKNOWN_SERVER);
            }
            int relativeMouseX = mouseX - left;
            int relativeMouseY = mouseY - top;
            if (relativeMouseX >= width - 15 && relativeMouseX <= width - 5 && relativeMouseY >= 0 && relativeMouseY <= 8) {
                this.owner.func_238854_b_(Collections.singletonList(status));
            } else if (relativeMouseX >= width - infoWidth - 15 - 2 && relativeMouseX <= width - 15 - 2 && relativeMouseY >= 0 && relativeMouseY <= 8) {
                this.owner.func_238854_b_(playerList);
            }
            if (this.mc.gameSettings.touchscreen || hovered) {
                this.mc.getTextureManager().bindTexture(SERVER_SELECTION);
                AbstractGui.fill(matrixStack, left, top, left + 32, top + 32, 0xA0A0A0A0);
                RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                int relativeX = mouseX - left;
                int relativeY = mouseY - top;
                if (this.canJoin()) {
                    AbstractGui.blit(matrixStack, left, top, relativeX < 32 && relativeX > 16 ? 0.0F : 0.0F, 32.0F, 32, 32, 256, 256);
                }
                if (index > 0) {
                    AbstractGui.blit(matrixStack, left, top, relativeX < 16 && relativeY < 16 ? 96.0F : 96.0F, 32.0F, 32, 32, 256, 256);
                }
                if (index < this.owner.getServerList().countServers() - 1) {
                    AbstractGui.blit(matrixStack, left, top, relativeX < 16 && relativeY > 16 ? 64.0F : 64.0F, 0.0F, 32, 32, 256, 256);
                }
            }
        }
    }

    private int interpolateColor(int color1, int color2, float t) {
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        int a = (int) (a1 + (a2 - a1) * t);
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
