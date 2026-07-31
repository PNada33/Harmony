package xd.harm.events.render;

import com.mojang.blaze3d.matrix.MatrixStack;

import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
public class WorldEvent2 {
    private MatrixStack stack;
    private float partialTicks;

    public WorldEvent2(MatrixStack stack, float partialTicks)
    {
        this.stack = stack;
        this.partialTicks = partialTicks;
    }


}

