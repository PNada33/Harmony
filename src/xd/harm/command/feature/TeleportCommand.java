package xd.harm.command.feature;

import com.google.common.eventbus.Subscribe;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.AbstractClientPlayerEntity;
import net.minecraft.network.play.client.CPlayerPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TextFormatting;
import xd.harm.Harmony;
import xd.harm.command.api.CommandException;
import xd.harm.command.interfaces.Command;
import xd.harm.command.interfaces.Logger;
import xd.harm.command.interfaces.MultiNamedCommand;
import xd.harm.command.interfaces.Parameters;
import xd.harm.command.interfaces.Prefix;
import xd.harm.events.world.EventUpdate;
import xd.harm.utils.client.IMinecraft;

import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class TeleportCommand implements Command, MultiNamedCommand, IMinecraft {

    private static TeleportCommand instance;

    final Prefix prefix;
    final Logger logger;

    private static final double CLOSE_RANGE_THRESHOLD = 2.0;
    private static final double ARRIVAL_THRESHOLD = 0.5;
    private static final long TELEPORT_INTERVAL = 400L;
    private static final double MOVEMENT_SPEED = 0.3;

    private Vector3d targetPosition = null;
    private boolean isTeleporting = false;
    private boolean isInCloseRange = false;
    private int teleportAttempts = 0;
    private long lastTeleportTime = 0L;

    public TeleportCommand(Prefix prefix, Logger logger) {
        this.prefix = prefix;
        this.logger = logger;
        instance = this;
        Harmony.getInstance().getEventBus().register(this);
    }

    public static TeleportCommand getInstance() {
        return instance;
    }

    @Override
    public void execute(Parameters parameters) {
        String targetPlayerName = parameters.asString(0)
                .orElseThrow(() -> new CommandException(TextFormatting.RED + "Использование: " + TextFormatting.GRAY + ".tpp <ник>"));

        if (mc.world == null || mc.player == null) {
            throw new CommandException(TextFormatting.RED + "Вы должны быть в игре!");
        }

        AbstractClientPlayerEntity targetPlayer = mc.world.getPlayers().stream()
                .filter(p -> p.getName().getString().equalsIgnoreCase(targetPlayerName))
                .findFirst()
                .orElse(null);

        if (targetPlayer == null) {
            throw new CommandException(TextFormatting.RED + "Игрок " + targetPlayerName + " не найден.");
        }

        if (targetPlayer == mc.player) {
            throw new CommandException(TextFormatting.RED + "Нельзя телепортироваться к себе.");
        }

        if (this.isTeleporting) {
            this.reset();
        }

        this.targetPosition = new Vector3d(
                targetPlayer.getPosX(),
                targetPlayer.getPosY(),
                targetPlayer.getPosZ()
        );
        this.isTeleporting = true;
        this.isInCloseRange = false;
        this.teleportAttempts = 0;
        this.lastTeleportTime = 0L;
        logger.log("Телепортация к " + TextFormatting.WHITE + targetPlayerName + TextFormatting.GRAY + "...");
    }

    @Subscribe
    public void onUpdate(EventUpdate event) {
        if (!this.isTeleporting || this.targetPosition == null) {
            return;
        }

        if (mc.player == null || mc.world == null) {
            this.reset();
            return;
        }

        boolean stillTeleporting = this.processTeleport();
        if (!stillTeleporting) {
            logger.log(TextFormatting.GREEN + "Телепортация завершена!");
            this.isTeleporting = false;
        }
    }

    private boolean processTeleport() {
        if (this.targetPosition == null || !this.isTeleporting) {
            return false;
        }

        if (mc.player == null || mc.world == null) {
            this.reset();
            return false;
        }

        Vector3d currentPos = mc.player.getPositionVec();
        double deltaX = this.targetPosition.x - currentPos.x;
        double deltaZ = this.targetPosition.z - currentPos.z;
        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        if (this.isInCloseRange) {
            double totalDistance = currentPos.distanceTo(this.targetPosition);

            if (totalDistance <= ARRIVAL_THRESHOLD) {
                this.reset();
                return false;
            }

            if (this.teleportAttempts >= 100) {
                this.reset();
                return false;
            }

            long currentTime = System.currentTimeMillis();
            if (currentTime - this.lastTeleportTime >= TELEPORT_INTERVAL) {
                this.sendTeleportPackets(
                        this.targetPosition.x,
                        this.targetPosition.y,
                        this.targetPosition.z
                );
                ++this.teleportAttempts;
                this.lastTeleportTime = currentTime;
            }

            return true;
        }

        if (horizontalDistance <= CLOSE_RANGE_THRESHOLD) {
            this.isInCloseRange = true;
            this.teleportAttempts = 0;
            return true;
        }

        this.setMovementDirection();

        double newX = mc.player.getPosX() + mc.player.getMotion().x;
        double newY = mc.player.getPosY() + mc.player.getMotion().y;
        double newZ = mc.player.getPosZ() + mc.player.getMotion().z;

        mc.player.setPosition(newX, newY, newZ);
        this.rotateToTarget(this.targetPosition.x, this.targetPosition.z);

        return true;
    }

    private void setMovementDirection() {
        Vector3d currentPos = mc.player.getPositionVec();
        double deltaX = this.targetPosition.x - currentPos.x;
        double deltaZ = this.targetPosition.z - currentPos.z;
        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        if (horizontalDistance > 0.1) {
            double dirX = deltaX / horizontalDistance;
            double dirZ = deltaZ / horizontalDistance;
            mc.player.setMotion(dirX * MOVEMENT_SPEED, 0.0, dirZ * MOVEMENT_SPEED);
        } else {
            mc.player.setMotion(0.0, 0.0, 0.0);
        }
    }

    private void rotateToTarget(double targetX, double targetZ) {
        Vector3d eyePos = mc.player.getEyePosition(1.0F);
        double deltaX = targetX - eyePos.x;
        double deltaZ = targetZ - eyePos.z;
        float yaw = (float) Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90.0F;
        yaw = MathHelper.wrapDegrees(yaw);
        mc.player.rotationYaw = yaw;
        mc.player.rotationYawHead = yaw;
        mc.player.prevRotationYaw = yaw;
    }

    private void sendTeleportPackets(double x, double y, double z) {
        if (mc.player == null) {
            return;
        }

        double currentX = mc.player.getPosX();
        double currentY = mc.player.getPosY();
        double currentZ = mc.player.getPosZ();
        double deltaX = x - currentX;
        double deltaY = y - currentY;
        double deltaZ = z - currentZ;
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

        if (distance < 0.1) {
            return;
        }

        double stepSize = 1.0;
        int steps = (int) Math.ceil(distance / stepSize);
        double stepX = deltaX / steps;
        double stepY = deltaY / steps;
        double stepZ = deltaZ / steps;
        double newX = currentX;
        double newY = currentY;
        double newZ = currentZ;

        for (int i = 0; i < steps; ++i) {
            newX += stepX;
            newY += stepY;
            newZ += stepZ;
            mc.player.connection.sendPacket(
                    new CPlayerPacket.PositionPacket(newX, newY, newZ, false)
            );
        }

        mc.player.connection.sendPacket(
                new CPlayerPacket.PositionPacket(x, y, z, true)
        );
    }

    private void reset() {
        this.targetPosition = null;
        this.isTeleporting = false;
        this.isInCloseRange = false;
        this.teleportAttempts = 0;
        this.lastTeleportTime = 0L;
    }

    public boolean isTeleporting() {
        return this.isTeleporting;
    }

    public void stopTeleport() {
        if (this.isTeleporting) {
            this.reset();
            logger.log(TextFormatting.YELLOW + "Телепортация отменена.");
        }
    }

    @Override
    public String name() {
        return "tpp";
    }

    @Override
    public String description() {
        return "Телепортация к игроку (СпукиТайм)";
    }

    @Override
    public List<String> aliases() {
        return List.of("teleportplayer", "tpplayer");
    }
}
