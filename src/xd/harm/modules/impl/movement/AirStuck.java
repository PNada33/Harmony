package xd.harm.modules.impl.movement;

import com.google.common.eventbus.Subscribe;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.client.CPlayerPacket;
import net.minecraft.util.MovementInput;
import net.minecraft.util.MovementInputFromOptions;
import xd.harm.Harmony;
import xd.harm.events.movement.EventMotion;
import xd.harm.events.network.EventPacket;
import xd.harm.events.world.EventLivingUpdate;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.impl.combat.HitAura;
import xd.harm.modules.settings.impl.BooleanSetting;

@ModuleRegister(name = "AirStuck", category = Category.Movement, desc = "Позволяет зависнуть в воздухе")
public class AirStuck extends Module {
    private final BooleanSetting headRotate = new BooleanSetting("Поворот головы", true);
    public final BooleanSetting crit = new BooleanSetting("Крит", false);
    private boolean oldIsFlying;
    public boolean critAwaited;
    float yaw;
    float pitch;
    float yawoff;

    public AirStuck() {
        addSettings(headRotate, crit);
    }

    @Subscribe
    public void onMotion(EventMotion event) {
        if (mc.player == null || critAwaited) {
            return;
        }

        ClientPlayNetHandler connection = mc.player.connection;
        if (mc.player.ticksExisted % 10 == 0) {
            connection.sendPacket(new CPlayerPacket(mc.player.isOnGround()));
        }
        if (shouldUseAuraRotation()) {
            connection.sendPacket(new CPlayerPacket.RotationPacket(HitAura.rotateVector.x, HitAura.rotateVector.y, mc.player.isOnGround()));
        }

        event.cancel();

        if (mc.player.isSprinting()) {
            mc.player.setSprinting(false);
        }

        if (headRotate.get()) {
            mc.player.rotationYawHead = mc.player.rotationYaw;
            mc.player.rotationPitchHead = mc.player.rotationPitch;
            mc.player.renderYawOffset = this.yawoff;
        } else {
            mc.player.rotationYaw = this.yaw;
            mc.player.rotationPitch = this.pitch;
            mc.player.rotationYawHead = this.yaw;
            mc.player.renderYawOffset = this.yawoff;
            mc.player.rotationPitchHead = this.pitch;
        }
    }

    @Subscribe
    public void onLivingUpdate(EventLivingUpdate event) {
        if (mc.player != null && !critAwaited) {
            mc.player.noClip = true;
            mc.player.setOnGround(false);
            mc.player.setMotion(0.0F, 0.0F, 0.0F);
            mc.player.abilities.isFlying = true;
        }
    }

    @Subscribe
    public void onPacket(EventPacket event) {
        if (mc.player != null && !critAwaited) {
            IPacket packet = event.getPacket();
            if (packet instanceof CPlayerPacket) {
                CPlayerPacket playerPacket = (CPlayerPacket) packet;
                if (playerPacket.moving) {
                    playerPacket.x = mc.player.getPosX();
                    playerPacket.y = mc.player.getPosY();
                    playerPacket.z = mc.player.getPosZ();
                }
                playerPacket.onGround = mc.player.isOnGround();
                if (playerPacket.rotating) {
                    playerPacket.yaw = getPacketYaw();
                    playerPacket.pitch = getPacketPitch();
                }
            }
        }
    }

    private boolean shouldUseAuraRotation() {
        return Harmony.getInstance().getModuleManager().getHitAura() != null
                && Harmony.getInstance().getModuleManager().getHitAura().isState()
                && HitAura.getTarget() != null;
    }

    private float getPacketYaw() {
        return shouldUseAuraRotation() ? HitAura.rotateVector.x : this.yaw;
    }

    private float getPacketPitch() {
        return shouldUseAuraRotation() ? HitAura.rotateVector.y : this.pitch;
    }

    public void startFreeze() {
        if (mc.player != null) {
            critAwaited = false;
            ClientPlayerEntity player = mc.player;
            player.movementInput = new MovementInput();
            mc.player.moveForward = 0.0F;
            mc.player.moveStrafing = 0.0F;
            this.yaw = mc.player.rotationYaw;
            this.pitch = mc.player.rotationPitch;
            this.yawoff = mc.player.renderYawOffset;
        }
    }

    public boolean onEnable() {
        super.onEnable();
        if (mc.player != null) {
            this.oldIsFlying = mc.player.abilities.isFlying;
            critAwaited = crit.get();
            if (!critAwaited) {
                startFreeze();
            }
        }
        return false;
    }

    public boolean onDisable() {
        super.onDisable();
        if (mc.player != null) {
            ClientPlayerEntity player = mc.player;
            player.movementInput = new MovementInputFromOptions(mc.gameSettings);
            mc.player.abilities.isFlying = this.oldIsFlying;
        }
        return false;
    }
}
