package xd.harm.ui.display;

import xd.harm.events.render.EventDisplay;
import xd.harm.utils.client.IMinecraft;

public interface ElementRenderer extends IMinecraft {
    void render(EventDisplay eventDisplay);
}

