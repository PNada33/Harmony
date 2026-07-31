package xd.harm.command.feature;

import xd.harm.command.interfaces.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.client.CPlayerPacket;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TextFormatting;
import org.apache.commons.lang3.math.NumberUtils;
import xd.harm.command.api.CommandException;
import java.util.List;

@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HClipCommand implements Command, MultiNamedCommand {
    final Prefix prefix;
    final Logger logger;
    final Minecraft mc;

    @Override
    public void execute(Parameters parameters) {
        String direction = parameters.asString(0).orElseThrow(() ->
                new CommandException(TextFormatting.RED + "× " + TextFormatting.RED + "Укажите дистанцию"));

        if (!NumberUtils.isNumber(direction)) {
            logger.log(TextFormatting.DARK_GRAY + "" + TextFormatting.RED + "× " + TextFormatting.WHITE + "Введите число");
            return;
        }

        double blocks = Double.parseDouble(direction);
        Vector3d lookVector = mc.player.getLook(1F).mul(blocks, 0, blocks);

        double newX = mc.player.getPosX() + lookVector.getX();
        double newZ = mc.player.getPosZ() + lookVector.getZ();

        for (int i = 0; i < 5; i++) {
            mc.player.connection.sendPacket(new CPlayerPacket.PositionPacket(newX, mc.player.getPosY(), newZ, false));
        }
        mc.player.setPositionAndUpdate(newX, mc.player.getPosY(), newZ);
        for (int i = 0; i < 5; i++) {
            mc.player.connection.sendPacket(new CPlayerPacket.PositionPacket(newX, mc.player.getPosY(), newZ, false));
        }

        logger.log(TextFormatting.DARK_GRAY + "" + TextFormatting.GREEN + "⟶ " + TextFormatting.WHITE + "Телепорт: " + String.format("%.1f", blocks) + TextFormatting.GRAY + " блоков");
    }

    @Override
    public String name() {
        return "hclip";
    }

    @Override
    public String description() {
        return "Горизонтальный телепорт";
    }

    @Override
    public List<String> aliases() {
        return List.of("hc");
    }
}