package com.jagrosh.discordipc;

import com.jagrosh.discordipc.entities.*;
import com.jagrosh.discordipc.entities.Packet.OpCode;
import com.jagrosh.discordipc.entities.pipe.Pipe;
import com.jagrosh.discordipc.entities.pipe.PipeStatus;
import com.jagrosh.discordipc.exceptions.NoDiscordClientException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Closeable;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.HashMap;

public final class IPCClient implements Closeable {
    private final long clientId;
    private final HashMap<String, Callback> callbacks = new HashMap<>();
    private volatile Pipe pipe;
    private IPCListener listener = null;
    private Thread readThread = null;
    private User currentUser = null;

    public IPCClient(long clientId) {
        this.clientId = clientId;
    }

    public void setListener(IPCListener listener) {
        this.listener = listener;
        if (pipe != null)
            pipe.setListener(listener);
    }

    public void connect(DiscordBuild... preferredOrder) throws NoDiscordClientException {
        checkConnected(false);
        callbacks.clear();
        pipe = null;

        pipe = Pipe.openPipe(this, clientId, callbacks, preferredOrder);

        if (listener != null)
            listener.onReady(this);
        startReading();
    }

    public void sendRichPresence(RichPresence presence) {
        sendRichPresence(presence, null);
    }

    public void sendRichPresence(RichPresence presence, Callback callback) {
        checkConnected(true);
        pipe.send(OpCode.FRAME, new JSONObject()
                .put("cmd", "SET_ACTIVITY")
                .put("args", new JSONObject()
                        .put("pid", getPID())
                        .put("activity", presence == null ? null : presence.toJson())), callback);
    }

    public void subscribe(Event sub) {
        subscribe(sub, null);
    }

    public void subscribe(Event sub, Callback callback) {
        checkConnected(true);
        if (!sub.isSubscribable())
            throw new IllegalStateException("Cannot subscribe to " + sub + " event!");
        pipe.send(OpCode.FRAME, new JSONObject()
                .put("cmd", "SUBSCRIBE")
                .put("evt", sub.name()), callback);
    }

    public PipeStatus getStatus() {
        if (pipe == null) return PipeStatus.UNINITIALIZED;
        return pipe.getStatus();
    }

    @Override
    public void close() {
        checkConnected(true);
        try {
            pipe.close();
            currentUser = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public DiscordBuild getDiscordBuild() {
        if (pipe == null) return null;
        return pipe.getDiscordBuild();
    }

    public User getCurrentUser() {
        return currentUser;
    }

    private void checkConnected(boolean connected) {
        if (connected && getStatus() != PipeStatus.CONNECTED)
            throw new IllegalStateException(String.format("IPCClient (ID: %d) is not connected!", clientId));
        if (!connected && getStatus() == PipeStatus.CONNECTED)
            throw new IllegalStateException(String.format("IPCClient (ID: %d) is already connected!", clientId));
    }

    private void startReading() {
        readThread = new Thread(() -> {
            try {
                Packet p;
                while ((p = pipe.read()).getOp() != OpCode.CLOSE) {
                    JSONObject json = p.getJson();

                    Event event = Event.of(json.optString("evt", null));

                    if (event == Event.READY && json.has("data")) {
                        try {

                            JSONObject data = json.getJSONObject("data");

                            JSONObject userData = null;

                            if (data.has("user")) {
                                userData = data.getJSONObject("user");
                            } else if (data.has("users")) {
                                userData = data.getJSONArray("users").getJSONObject(0);
                            }

                            if (userData != null) {

                                currentUser = new User(
                                        userData.optString("username", "Unknown"),
                                        userData.optString("discriminator", "0000"),
                                        userData.has("id") ? Long.parseLong(userData.getString("id")) : 0L,
                                        userData.optString("avatar", null)
                                );
                            }
                        } catch (Exception e) {
                            System.err.println("Detailed error parsing user data: ");
                            e.printStackTrace();
                        }
                    }

                    if (listener != null) {
                        listener.onPacketReceived(this, p);
                    }
                }

                pipe.setStatus(PipeStatus.DISCONNECTED);
                if (listener != null)
                    listener.onClose(this, p.getJson());
            } catch (IOException | JSONException ex) {

                pipe.setStatus(PipeStatus.DISCONNECTED);
                currentUser = null;
                if (listener != null)
                    listener.onDisconnect(this, ex);
            }
        });

        readThread.start();
    }

    private static int getPID() {
        String pr = ManagementFactory.getRuntimeMXBean().getName();
        return Integer.parseInt(pr.substring(0, pr.indexOf('@')));
    }

    public enum Event {
        NULL(false),
        READY(false),
        ERROR(false),
        ACTIVITY_JOIN(true),
        ACTIVITY_SPECTATE(true),
        ACTIVITY_JOIN_REQUEST(true),
        UNKNOWN(false);

        private final boolean subscribable;

        Event(boolean subscribable) {
            this.subscribable = subscribable;
        }

        public boolean isSubscribable() {
            return subscribable;
        }

        static Event of(String str) {
            if (str == null)
                return NULL;
            for (Event s : Event.values()) {
                if (s != UNKNOWN && s.name().equalsIgnoreCase(str))
                    return s;
            }
            return UNKNOWN;
        }
    }
}