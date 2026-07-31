package xd.harm.events.world;


import xd.harm.events.Event;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class TickEvent extends Event {

    public TickEvent() {
    }

}


