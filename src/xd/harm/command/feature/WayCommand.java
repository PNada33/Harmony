package xd.harm.command.feature;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;
import xd.harm.Harmony;
import xd.harm.command.api.CommandException;
import xd.harm.command.interfaces.*;
import xd.harm.events.render.EventDisplay;
import xd.harm.utils.client.IMinecraft;
import xd.harm.utils.render.color.ColorUtils;
import xd.harm.utils.render.font.Fonts;
import xd.harm.utils.text.font.ClientFonts;
import com.google.common.eventbus.Subscribe;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.text.TextFormatting;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class WayCommand implements Command, MultiNamedCommand, IMinecraft {

    private static WayCommand instance;

    final Prefix prefix;
    final Logger logger;
    final Map<String, WayPoint> WayMap = new LinkedHashMap<>();

    private static class WayPoint {
        final int x;
        final int y;
        final int z;

        WayPoint(int x, int y, int z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public int x() { return x; }
        public int y() { return y; }
        public int z() { return z; }
    }

    private static class Indicator {
        float x, y;
        float angleDeg;
        boolean onScreen;
        Indicator(float x, float y, float angleDeg, boolean onScreen) {
            this.x = x; this.y = y; this.angleDeg = angleDeg; this.onScreen = onScreen;
        }
    }

    public WayCommand(Prefix prefix, Logger logger) {
        this.prefix = prefix;
        this.logger = logger;
        instance = this;
        Harmony.getInstance().getEventBus().register(this);
    }

    public static WayCommand getInstance() {
        return instance;
    }

    public void addDeathPoint(int x, int y, int z) {
        WayPoint pos = new WayPoint(x, y, z);
        WayMap.put("Смерть", pos);
    }

    public void removeDeathPoint() {
        WayMap.remove("Смерть");
    }

    public void addWaypoint(String name, int x, int y, int z) {
        WayPoint pos = new WayPoint(x, y, z);
        WayMap.put(name, pos);
    }

    @Override
    public void execute(Parameters parameters) {
        String commandType = parameters.asString(0).orElseThrow(() ->
                new CommandException(TextFormatting.RED + "× " + TextFormatting.WHITE + "Действия: " + TextFormatting.GRAY + "add · remove · clear · list"));

        switch (commandType.toLowerCase()) {
            case "add" -> addWay(parameters);
            case "remove" -> removeWay(parameters);
            case "clear" -> {
                if (WayMap.isEmpty()) {
                    logger.log(TextFormatting.RED + "○ " + TextFormatting.GRAY + "Список точек уже пуст");
                    return;
                }
                int count = WayMap.size();
                WayMap.clear();
                logger.log(TextFormatting.DARK_GRAY + "" + TextFormatting.GREEN + "✓ " + TextFormatting.WHITE + "Удалено точек: " + count);
            }
            case "list" -> listWayPoints();
            default ->
                    throw new CommandException(TextFormatting.RED + "× " + TextFormatting.WHITE + "Действия: " + TextFormatting.GRAY + "add · remove · clear · list");
        }
    }

    private void addWay(Parameters param) {
        String name = param.asString(1)
                .orElseThrow(() -> new CommandException(TextFormatting.RED + "× " + TextFormatting.RED + "Укажите имя точки"));
        int x = param.asInt(2)
                .orElseThrow(() -> new CommandException(TextFormatting.RED + "× " + TextFormatting.RED + "Укажите координату X"));
        int z = param.asInt(3)
                .orElseThrow(() -> new CommandException(TextFormatting.RED + "× " + TextFormatting.RED + "Укажите координату Z"));
        Minecraft mc = Minecraft.getInstance();
        int y = (int) mc.player.getPosY();
        WayPoint pos = new WayPoint(x, y, z);
        if (WayMap.containsKey(name)) {
            WayMap.put(name, pos);
            logger.log(TextFormatting.DARK_GRAY + "" + TextFormatting.GREEN + "⟳ " + TextFormatting.WHITE + name + TextFormatting.GRAY + " обновлена " + TextFormatting.DARK_GRAY + "(" + TextFormatting.GRAY + x + ", " + y + ", " + z + TextFormatting.DARK_GRAY + ")");
        } else {
            WayMap.put(name, pos);
            logger.log(TextFormatting.DARK_GRAY + "" + TextFormatting.GREEN + "✓ " + TextFormatting.WHITE + name + TextFormatting.GRAY + " добавлена " + TextFormatting.DARK_GRAY + "(" + TextFormatting.GRAY + x + ", " + y + ", " + z + TextFormatting.DARK_GRAY + ")");
        }
    }

    private void removeWay(Parameters param) {
        String name = param.asString(1)
                .orElseThrow(() -> new CommandException(TextFormatting.RED + "× " + TextFormatting.RED + "Укажите имя точки"));
        if (WayMap.remove(name) != null) {
            logger.log(TextFormatting.DARK_GRAY + "" + TextFormatting.GREEN + "✓ " + TextFormatting.WHITE + name + TextFormatting.GRAY + " удалена");
        } else {
            logger.log(TextFormatting.DARK_GRAY + "" + TextFormatting.RED + "× " + TextFormatting.WHITE + "Точка " + name + TextFormatting.GRAY + " не найдена");
        }
    }

    private void listWayPoints() {
        if (WayMap.isEmpty()) {
            logger.log(TextFormatting.RED + "○ " + TextFormatting.GRAY + "Точек пока нет");
            return;
        }
        logger.log(TextFormatting.DARK_GRAY + "┌ " + TextFormatting.WHITE + "Way-точки " + TextFormatting.DARK_GRAY + "(" + WayMap.size() + ")");
        for (String name : WayMap.keySet()) {
            WayPoint pos = WayMap.get(name);
            logger.log(TextFormatting.DARK_GRAY + "│ " + TextFormatting.GREEN + "◆ " + TextFormatting.WHITE + name + TextFormatting.DARK_GRAY + " → " + TextFormatting.GRAY + pos.x() + ", " + pos.y() + ", " + pos.z());
        }
        logger.log(TextFormatting.DARK_GRAY + "└───────────────");
    }

    @Subscribe
    private void onDisplay(EventDisplay e) {
        if (mc.player == null || e.getType() != EventDisplay.Type.HIGH) {
            return;
        }

        GlStateManager.pushMatrix();

        GlStateManager.color4f(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.enableTexture();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        Minecraft mc = Minecraft.getInstance();
        for (String name : WayMap.keySet()) {
            WayPoint pos = WayMap.get(name);
            Vector3d vec3d = new Vector3d(pos.x() + 0.5, pos.y() + 0.5, pos.z() + 0.5);

            Indicator ind = projectClampedFixed(vec3d, 45.0f, 18.0f);
            if (ind == null) continue;

            double dx = mc.player.getPosX() - vec3d.x;
            double dy = mc.player.getPosY() - vec3d.y;
            double dz = mc.player.getPosZ() - vec3d.z;
            int distance = (int) Math.sqrt(dx * dx + dy * dy + dz * dz);

            String labelText = name + " >> " + distance + "м";
            float labelWidth = Fonts.sfuy.getWidth(labelText, 7);

            float cx = ind.x;
            float cy = ind.y;

            float labelPosX = cx - labelWidth / 2f;
            float labelPosY = cy + 4;

            String icon = name.equals("Смерть") ? "M" : "A";
            float iconWidth;

            if (name.equals("Смерть")) {
                iconWidth = ClientFonts.icons_nur[24].getWidth(icon);
                ClientFonts.icons_nur[24].drawString(
                        e.getMatrixStack(),
                        icon,
                        cx - iconWidth / 2f,
                        cy - 12,
                        ColorUtils.reAlphaInt(-1, 215)
                );
            } else {
                iconWidth = ClientFonts.icons_client[22].getWidth(icon);
                ClientFonts.icons_client[22].drawString(
                        e.getMatrixStack(),
                        icon,
                        cx - iconWidth / 2f,
                        cy - 12,
                        ColorUtils.reAlphaInt(-1, 215)
                );
            }

            Fonts.sfuy.drawText(
                    e.getMatrixStack(),
                    labelText,
                    labelPosX,
                    labelPosY,
                    ColorUtils.setAlpha(-1, 225),
                    7
            );
        }

        GlStateManager.color4f(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.popMatrix();
    }

    private Indicator projectClampedFixed(Vector3d worldPos, float paddingX, float paddingY) {
        int w = mc.getMainWindow().getScaledWidth();
        int h = mc.getMainWindow().getScaledHeight();
        float cx = w / 2f, cy = h / 2f;

        if (mc.getRenderManager().info == null) {
            return null;
        }

        Vector3d camPos = mc.getRenderManager().info.getProjectedView();
        Quaternion cameraRotation = mc.getRenderManager().getCameraOrientation().copy();
        cameraRotation.conjugate();

        Vector3f relative = new Vector3f(
                (float) (camPos.x - worldPos.x),
                (float) (camPos.y - worldPos.y),
                (float) (camPos.z - worldPos.z)
        );
        relative.transform(cameraRotation);

        float depth = relative.getZ();
        if (Math.abs(depth) < 1.0E-4F) {
            depth = depth >= 0.0F ? 1.0E-4F : -1.0E-4F;
        }

        float fov = (float) mc.gameRenderer.getFOVModifier(mc.getRenderManager().info, mc.getRenderPartialTicks(), true);
        float scale = (h / 2f) / (float) Math.tan(Math.toRadians(fov / 2f));
        float screenX = -relative.getX() * scale / depth + cx;
        float screenY = cy - relative.getY() * scale / depth;
        boolean inFront = depth < 0.0F;

        float minX = paddingX, maxX = w - paddingX;
        float minY = paddingY, maxY = h - paddingY;

        if (inFront && screenX >= minX && screenX <= maxX && screenY >= minY && screenY <= maxY) {
            return new Indicator(screenX, screenY, 0.0F, true);
        }

        float dirX = screenX - cx;
        float dirY = screenY - cy;

        if (!inFront) {
            dirX = -dirX;
            dirY = -dirY;
        }

        float len = MathHelper.sqrt(dirX * dirX + dirY * dirY);
        if (len < 1.0E-4F) {
            dirX = 0.0F;
            dirY = -1.0F;
        } else {
            dirX /= len;
            dirY /= len;
        }

        float tHoriz = dirX > 0 ? (maxX - cx) / dirX : (dirX < 0 ? (minX - cx) / dirX : Float.POSITIVE_INFINITY);
        float tVert = dirY > 0 ? (maxY - cy) / dirY : (dirY < 0 ? (minY - cy) / dirY : Float.POSITIVE_INFINITY);
        float t = Math.min(tHoriz, tVert);

        return new Indicator(cx + dirX * t, cy + dirY * t, (float) Math.toDegrees(Math.atan2(dirY, dirX)), false);
    }

    @Override
    public String name() { return "way"; }

    @Override
    public String description() { return "Управление Way-точками"; }

    @Override
    public List<String> aliases() { return List.of("g"); }
}
