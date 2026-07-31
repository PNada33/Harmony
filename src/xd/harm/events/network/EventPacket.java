package xd.harm.events.network;


import xd.harm.events.CancelEvent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.IPacket;

@Getter
@Setter
@AllArgsConstructor
public class EventPacket extends CancelEvent {

    private IPacket<?> packet;
    private Type type;

    public boolean isSend() {
        return type == Type.SEND;
    }

    public boolean isReceive() {
        return type == Type.RECEIVE;
    }

    public boolean isSendPacket() {
        return type == Type.SEND;
    }

    public boolean isReceivePacket() {
        return type == Type.RECEIVE;
    }

    public void setPacket(IPacket<?> packet) {
        this.packet = packet;
    }

    public void setCancel(boolean cancel) {
        this.cancel();
    }

    public enum Type {
        RECEIVE, SEND
    }
}


