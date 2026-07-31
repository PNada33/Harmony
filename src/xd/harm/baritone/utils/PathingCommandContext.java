

package xd.harm.baritone.utils;

import xd.harm.baritone.api.pathing.goals.Goal;
import xd.harm.baritone.api.process.PathingCommand;
import xd.harm.baritone.api.process.PathingCommandType;
import xd.harm.baritone.pathing.movement.CalculationContext;

public class PathingCommandContext extends PathingCommand {

    public final CalculationContext desiredCalcContext;

    public PathingCommandContext(Goal goal, PathingCommandType commandType, CalculationContext context) {
        super(goal, commandType);
        this.desiredCalcContext = context;
    }
}
