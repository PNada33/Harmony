package xd.harm.modules.impl.player;

import com.google.common.eventbus.Subscribe;
import xd.harm.events.network.EventPacket;
import xd.harm.modules.api.Category;
import xd.harm.modules.api.Module;
import xd.harm.modules.api.ModuleRegister;
import xd.harm.modules.settings.impl.BooleanSetting;
import xd.harm.modules.settings.impl.ModeListSetting;
import net.minecraft.block.Blocks;
import net.minecraft.network.play.server.SExplosionPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import lombok.Getter;

@Getter
@ModuleRegister(name = "AntiPush", category = Category.Player, desc = "Блокирует отталкивание и замедление")
public class AntiPush extends Module {

    public static ModeListSetting modes = new ModeListSetting("Тип",
            new BooleanSetting("Игроки", true),
            new BooleanSetting("Вода", false),
            new BooleanSetting("Взрывы", false),
            new BooleanSetting("Блоки", true),
            new BooleanSetting("Паутина", true),
            new BooleanSetting("Сладкие ягоды", true));

    private boolean inWeb = false;
    private boolean inSweetBerries = false;
    private long lastToggleTime = 0;
    private boolean isSpeedingUp = false;

    public AntiPush() {
        addSettings(modes);
    }

    @Subscribe
    public void onUpdate(xd.harm.events.world.EventUpdate event) {
        if (mc.player == null || mc.world == null) return;

        BlockPos playerPos = new BlockPos(mc.player.getPosX(), mc.player.getPosY(), mc.player.getPosZ());
        inWeb = mc.world.getBlockState(playerPos).getBlock() == Blocks.COBWEB ||
                mc.world.getBlockState(playerPos.up()).getBlock() == Blocks.COBWEB;

        inSweetBerries = mc.world.getBlockState(playerPos).getBlock() == Blocks.SWEET_BERRY_BUSH ||
                mc.world.getBlockState(playerPos.up()).getBlock() == Blocks.SWEET_BERRY_BUSH;

        long currentTime = System.currentTimeMillis();
        if (inSweetBerries && modes.getValueByName("Сладкие ягоды").get() && currentTime - lastToggleTime >= 1000) {
            isSpeedingUp = !isSpeedingUp;
            lastToggleTime = currentTime;
        }

        if (inWeb && modes.getValueByName("Паутина").get()) {
            preventWebSlowdown();
        }
        if (inSweetBerries && modes.getValueByName("Сладкие ягоды").get()) {
            preventSweetBerrySlowdown();
        }
    }

    @Subscribe
    public void onPacket(EventPacket e) {
        if (e.isReceive()) {
            if (modes.getValueByName("Взрывы").get()) {
                if (e.getPacket() instanceof SExplosionPacket) {
                    e.cancel();
                }
            }
        }
    }

    private void preventWebSlowdown() {
        if (mc.player == null) return;
        Vector3d zeroVector = new Vector3d(0.0D, 0.0D, 0.0D);
        mc.player.setMotionMultiplier(Blocks.COBWEB.getDefaultState(), zeroVector);
    }

    private void preventSweetBerrySlowdown() {
        if (mc.player == null) return;
        Vector3d motionMultiplier;
        if (isSpeedingUp) {

            motionMultiplier = new Vector3d(1.5D, 1.0D, 1.5D);
        } else {

            motionMultiplier = new Vector3d(0.5D, 1.0D, 0.5D);
        }
        mc.player.setMotionMultiplier(Blocks.SWEET_BERRY_BUSH.getDefaultState(), motionMultiplier);
    }

    @Override
    public boolean onDisable() {
        super.onDisable();
        inWeb = false;
        inSweetBerries = false;
        isSpeedingUp = false;
        lastToggleTime = 0;
        return false;
    }
}
