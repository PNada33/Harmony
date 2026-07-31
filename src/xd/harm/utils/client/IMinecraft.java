package xd.harm.utils.client;

import xd.harm.utils.math.MathUtil;
import xd.harm.utils.text.GradientUtil;
import net.minecraft.client.MainWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import org.joml.Vector2d;

import java.util.ArrayList;
import java.util.List;

public interface IMinecraft {
    Minecraft mc = Minecraft.getInstance();

    MainWindow window = mc.getMainWindow();
    BufferBuilder buffer = Tessellator.getInstance().getBuffer();
    Tessellator tessellator = Tessellator.getInstance();
    List<ITextComponent> clientMessages = new ArrayList<>();

    default void print(String input) {
        if (mc.player == null) return;
        IFormattableTextComponent text = GradientUtil.gradient("Harmony")
                .append(new StringTextComponent(" "))
                .append(GradientUtil.gradient("»"))
                .append(new StringTextComponent(TextFormatting.DARK_GRAY + " " + TextFormatting.RESET + input));
        clientMessages.add(text);
        mc.ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(text, 0);
    }

    default void print(ITextComponent input) {
        if (mc.player == null) return;
        IFormattableTextComponent text = GradientUtil.gradient("Harmony")
                .append(new StringTextComponent(" "))
                .append(GradientUtil.gradient("»"))
                .append(new StringTextComponent(TextFormatting.DARK_GRAY + " " + TextFormatting.RESET))
                .append(input.deepCopy());
        clientMessages.add(text);
        mc.ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(text, 0);
    }

    default Vector2d scaled() {
        return MathUtil.getMouse(mc.getMainWindow().getScaledWidth(), mc.getMainWindow().getScaledHeight());
    }
}
