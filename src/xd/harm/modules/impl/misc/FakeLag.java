package xd.harm.modules.impl.misc;

import xd.harm.events.combat.AttackEvent;
import xd.harm.events.render.EventRender3D;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.SliderSetting;
import xd.harm.events.network.EventPacket;
import xd.harm.events.movement.EventMotion;
import xd.harm.utils.client.TimerUtility;
import xd.harm.utils.math.AnimationMath;
import xd.harm.utils.math.animation.Animation;
import xd.harm.utils.render.color.ColorUtils;
import com.google.common.eventbus.Subscribe;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.settings.PointOfView;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.client.CPlayerPacket;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.optifine.render.RenderUtils;
import org.lwjgl.opengl.GL11;

@ModuleRegister(name = "FakeLag", category = Category.Misc, desc = "Заставляет вас лагать для сервера")
public final class FakeLag extends Module {
    private final CopyOnWriteArrayList<IPacket<?>> packets = new CopyOnWriteArrayList();
    private BooleanSetting delay = new BooleanSetting("Пульсации", false);
    private SliderSetting delayS = (new SliderSetting("Задержка", 100.0F, 50.0F, 1000.0F, 50.0F)).setVisible(() -> delay.get());
    private long started;
    public TimerUtility timerUtil = new TimerUtility();
    private Vector3d lastPos = new Vector3d(0.0F, 0.0F, 0.0F);
    private Vector3d animation;
    private boolean freezePosition = false;

    public FakeLag() {
        this.addSettings(delay, delayS);
    }

    public boolean onEnable() {
        super.onEnable();
        this.started = System.currentTimeMillis();
        this.lastPos = mc.player.getPositionVec();
        this.freezePosition = !delay.get();
        return false;
    }

    @Subscribe
    public void onPacket(EventPacket event) {
        if (mc.player != null && mc.world != null && !mc.isSingleplayer() && !mc.player.getShouldBeDead()) {
            if (delay.get()) {
                if (event.isSendPacket()) {
                    this.packets.add(event.getPacket());
                    event.cancel();
                    return;
                }
            } else if (this.freezePosition && event.isSendPacket()) {
                this.packets.add(event.getPacket());
                event.cancel();
                return;
            }

            if (event.isSend() && !event.isCancel()) {
                IPacket packet = event.getPacket();
                if (packet instanceof CPlayerPacket.PositionPacket) {
                    CPlayerPacket.PositionPacket posPacket = (CPlayerPacket.PositionPacket)packet;
                    this.lastPos = new Vector3d(posPacket.x, posPacket.y, posPacket.z);
                } else if (packet instanceof CPlayerPacket.PositionRotationPacket) {
                    CPlayerPacket.PositionRotationPacket posRotPacket = (CPlayerPacket.PositionRotationPacket)packet;
                    this.lastPos = new Vector3d(posRotPacket.x, posRotPacket.y, posRotPacket.z);
                }
            }

        } else {
            this.toggle();
        }
    }

    @Subscribe
    private void onAttack(AttackEvent event) {
        for(IPacket packet : this.packets) {
            mc.player.connection.getNetworkManager().sendPacketWithoutEvent(packet);
            if (packet instanceof CPlayerPacket.PositionPacket posPacket) {
                this.lastPos = new Vector3d(posPacket.x, posPacket.y, posPacket.z);
            } else if (packet instanceof CPlayerPacket.PositionRotationPacket posRotPacket) {
                this.lastPos = new Vector3d(posRotPacket.x, posRotPacket.y, posRotPacket.z);
            }
        }

        this.packets.clear();
        this.started = System.currentTimeMillis();
        this.timerUtil.reset();
    }

    @Subscribe
    private void onRender(EventRender3D event) {
        if (!delay.get() || mc.gameSettings.getPointOfView() != PointOfView.FIRST_PERSON) {
            if (this.lastPos != null && !mc.isSingleplayer()) {
                if (this.animation == null) {
                    this.animation = this.lastPos;
                }

                double width = mc.player.getWidth() / 2.0F;
                this.animation = AnimationMath.fast(this.animation, this.lastPos, 11.0F);
                GL11.glPushMatrix();
                Vector3d renderPos = mc.getRenderManager().info.getProjectedView();
                GL11.glTranslated(-renderPos.x, -renderPos.y, -renderPos.z);

                double minX = this.animation.getX() - width;
                double minY = this.animation.getY();
                double minZ = this.animation.getZ() - width;
                double maxX = this.animation.getX() + width;
                double maxY = this.animation.getY() + mc.player.getHeight();
                double maxZ = this.animation.getZ() + width;

                RenderUtils.drawBox(new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ), ColorUtils.interpolate(-1, ColorUtils.getColor(0), 1.0F));
                GL11.glPopMatrix();
            }
        }
    }

    @Subscribe
    public void onMotion(EventMotion event) {
        if (System.currentTimeMillis() - this.started >= 29900L) {
            this.toggle();
        } else {
            if (delay.get()) {
                this.freezePosition = false;
                if (this.timerUtil.hasTimeElapsed(delayS.get().longValue())) {
                    for(IPacket packet : this.packets) {
                        mc.player.connection.getNetworkManager().sendPacketWithoutEvent(packet);
                        if (packet instanceof CPlayerPacket.PositionPacket) {
                            CPlayerPacket.PositionPacket posPacket = (CPlayerPacket.PositionPacket)packet;
                            this.lastPos = new Vector3d(posPacket.x, posPacket.y, posPacket.z);
                        } else if (packet instanceof CPlayerPacket.PositionRotationPacket) {
                            CPlayerPacket.PositionRotationPacket posRotPacket = (CPlayerPacket.PositionRotationPacket)packet;
                            this.lastPos = new Vector3d(posRotPacket.x, posRotPacket.y, posRotPacket.z);
                        }
                    }

                    this.packets.clear();
                    this.started = System.currentTimeMillis();
                    this.timerUtil.reset();
                }
            } else if (this.freezePosition) {
            }
        }
    }

    public boolean onDisable() {
        super.onDisable();

        for(IPacket packet : this.packets) {
            mc.player.connection.sendPacket(packet);
        }

        this.packets.clear();
        this.freezePosition = false;
        return false;
    }
}
