package xd.harm.ui.display;

import xd.harm.events.world.EventUpdate;
import xd.harm.utils.client.IMinecraft;

public interface ElementUpdater extends IMinecraft {

    void update(EventUpdate e);
}

