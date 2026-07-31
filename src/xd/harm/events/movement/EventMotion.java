package xd.harm.events.movement;


import xd.harm.events.CancelEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.minecraft.util.math.vector.Vector3d;

@Data
@AllArgsConstructor
public class EventMotion extends CancelEvent{
    private double x, y, z;
    private float yaw, pitch;
    private boolean onGround;
    private Vector3d motion;

    Runnable postMotion;

    public Vector3d getMotion() {
        return motion;
    }

    public void setMotion(Vector3d motion) {
        this.motion = motion;
    }
}


