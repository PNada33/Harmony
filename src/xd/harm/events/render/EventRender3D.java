package xd.harm.events.render;


import xd.harm.events.Event;
import com.mojang.blaze3d.matrix.MatrixStack;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.renderer.IRenderTypeBuffer;

@Getter
@Setter
public class EventRender3D extends Event {
    private MatrixStack stack;
    private float partialTicks;
    private IRenderTypeBuffer vertex;

    public EventRender3D(MatrixStack stack, float partialTicks, IRenderTypeBuffer vertex) {
        this.stack = stack;
        this.partialTicks = partialTicks;
        this.vertex = vertex;
    }

    public MatrixStack getMatrixStack() {
        return stack;
    }
}


