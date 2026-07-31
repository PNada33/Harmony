package xd.harm.ui.schedules.impl;

import xd.harm.ui.schedules.Schedule;
import xd.harm.ui.schedules.TimeType;

public class MascotSchedule
        extends Schedule {
    @Override
    public String getName() {
        return "Талисман";
    }

    @Override
    public TimeType[] getTimes() {
        return new TimeType[]{TimeType.NINETEEN_HALF};
    }
}
