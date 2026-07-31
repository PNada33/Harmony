package xd.harm.events.movement;


import xd.harm.events.CancelEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.util.math.vector.Vector3d;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class StrafeEvent extends CancelEvent {

    private float friction;
    private Vector3d relative;
    private float yaw;
}



