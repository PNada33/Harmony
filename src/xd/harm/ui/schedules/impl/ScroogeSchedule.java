package xd.harm.ui.schedules.impl;

import xd.harm.ui.schedules.Schedule;
import xd.harm.ui.schedules.TimeType;

public class ScroogeSchedule
        extends Schedule {
    @Override
    public String getName() {
        return "Скрудж";
    }

    @Override
    public TimeType[] getTimes() {
        return new TimeType[]{TimeType.FIFTEEN_HALF};
    }
}
