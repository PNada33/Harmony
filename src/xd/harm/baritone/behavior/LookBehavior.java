package xd.harm.baritone.behavior;


import xd.harm.baritone.Baritone;
import xd.harm.baritone.api.behavior.ILookBehavior;
import xd.harm.baritone.api.event.events.PlayerUpdateEvent;
import xd.harm.baritone.api.event.events.RotationMoveEvent;
import xd.harm.baritone.api.utils.Rotation;

public final class LookBehavior extends Behavior implements ILookBehavior {

    /**
     * Target's values are as follows:
     */
    private Rotation target;

    /**
     * Whether or not rotations are currently being forced
     */
    private boolean force;

    private float lastYaw;

    public LookBehavior(Baritone baritone) {
        super(baritone);
    }

    @Override
    public void updateTarget(Rotation target, boolean force) {
        this.target = target;
        if (!force) {
            double rand = Math.random() - 0.5;
            if (Math.abs(rand) < 0.1) {
                rand *= 4;
            }
            this.target = new Rotation(this.target.getYaw() + (float) (rand * Baritone.settings().randomLooking113.value), this.target.getPitch());
        }
        this.force = force || !Baritone.settings().freeLook.value;
    }

    @Override
    public void onPlayerUpdate(PlayerUpdateEvent event) {
        if (this.target == null) {
            return;
        }

        // Whether or not we're going to silently set our angles
        boolean silent = Baritone.settings().antiCheatCompatibility.value && !this.force;

        switch (event.getState()) {
            case PRE: {
                if (this.force) {
                    ctx.player().rotationYaw = this.target.getYaw();
                    float oldPitch = ctx.player().rotationPitch;
                    float desiredPitch = this.target.getPitch();
                    ctx.player().rotationPitch = desiredPitch;
                    ctx.player().rotationYaw += (Math.random() - 0.5) * Baritone.settings().randomLooking.value;
                    ctx.player().rotationPitch += (Math.random() - 0.5) * Baritone.settings().randomLooking.value;
                    if (desiredPitch == oldPitch && !Baritone.settings().freeLook.value) {
                        nudgeToLevel();
                    }
                    this.target = null;
                }
                if (silent) {
                    this.lastYaw = ctx.player().rotationYaw;
                    ctx.player().rotationYaw = this.target.getYaw();
                }
                break;
            }
            case POST: {
                if (silent) {
                    ctx.player().rotationYaw = this.lastYaw;
                    this.target = null;
                }
                break;
            }
            default:
                break;
        }
    }

    public void pig() {
        if (this.target != null) {
            ctx.player().rotationYaw = this.target.getYaw();
        }
    }

    @Override
    public void onPlayerRotationMove(RotationMoveEvent event) {
        if (this.target != null) {

            event.setYaw(this.target.getYaw());

            // If we have antiCheatCompatibility on, we're going to use the target value later in onPlayerUpdate()
            // Also the type has to be MOTION_UPDATE because that is called after JUMP
            if (!Baritone.settings().antiCheatCompatibility.value && event.getType() == RotationMoveEvent.Type.MOTION_UPDATE && !this.force) {
                this.target = null;
            }
        }
    }

    /**
     * Nudges the player's pitch to a regular level. (Between {@code -20} and {@code 10}, increments are by {@code 1})
     */
    private void nudgeToLevel() {
        if (ctx.player().rotationPitch < -20) {
            ctx.player().rotationPitch++;
        } else if (ctx.player().rotationPitch > 10) {
            ctx.player().rotationPitch--;
        }
    }
}
