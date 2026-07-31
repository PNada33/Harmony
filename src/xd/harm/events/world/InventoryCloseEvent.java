package xd.harm.events.world;


import xd.harm.events.CancelEvent;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class InventoryCloseEvent extends CancelEvent {

    public int windowId;

}



