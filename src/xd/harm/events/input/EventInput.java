package xd.harm.events.input;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EventInput {
    private float forward, strafe;
    private boolean jump, sneak, sprint;
    private double sneakSlowDownMultiplier;
    private float yaw;

    public EventInput(float forward, float strafe, boolean jump, boolean sneak, double sneakSlowDownMultiplier) {
        this.forward = forward;
        this.strafe = strafe;
        this.jump = jump;
        this.sneak = sneak;
        this.sprint = false;
        this.sneakSlowDownMultiplier = sneakSlowDownMultiplier;
        this.yaw = 0;
    }

    public EventInput(float forward, float strafe, boolean jump, boolean sneak, boolean sprint, double sneakSlowDownMultiplier) {
        this.forward = forward;
        this.strafe = strafe;
        this.jump = jump;
        this.sneak = sneak;
        this.sprint = sprint;
        this.sneakSlowDownMultiplier = sneakSlowDownMultiplier;
        this.yaw = 0;
    }

    public void setYaw(float currentYaw, float targetYaw) {
        this.yaw = targetYaw;
    }

    public void setForward(float forward) {
        this.forward = forward;
    }

    public void setSprintState(boolean sprint) {
        this.sprint = sprint;
    }

    public boolean getSprintState() {
        return this.sprint;
    }
}
