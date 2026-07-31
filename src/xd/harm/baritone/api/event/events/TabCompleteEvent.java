

package xd.harm.baritone.api.event.events;

import xd.harm.baritone.api.event.events.type.Cancellable;

/**
 * @author LoganDark
 */
public final class TabCompleteEvent extends Cancellable {

    public final String prefix;
    public String[] completions;

    public TabCompleteEvent(String prefix) {
        this.prefix = prefix;
        this.completions = null;
    }
}
