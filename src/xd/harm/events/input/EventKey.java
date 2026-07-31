package xd.harm.events.input;

import lombok.*;

@Data
@AllArgsConstructor
public class EventKey {
    int key;

    public boolean isKeyDown(int key) {
        return this.key == key;
    }

    public boolean isKey(int key) {
        return this.key == key;
    }
}
