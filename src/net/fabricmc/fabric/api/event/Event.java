package net.fabricmc.fabric.api.event;

import java.lang.reflect.Array;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

public final class Event<T> {
    private final Class<T> type;
    private final Function<T[], T> invokerFactory;
    private final List<T> listeners = new CopyOnWriteArrayList<>();
    private volatile T invoker;

    Event(Class<T> type, Function<T[], T> invokerFactory) {
        this.type = type;
        this.invokerFactory = invokerFactory;
        this.invoker = this.createInvoker();
    }

    public void register(T listener) {
        if (listener == null) {
            return;
        }

        this.listeners.add(listener);
        this.invoker = this.createInvoker();
    }

    public T invoker() {
        return this.invoker;
    }

    @SuppressWarnings("unchecked")
    private T createInvoker() {
        T[] array = this.listeners.toArray((T[]) Array.newInstance(this.type, this.listeners.size()));
        return this.invokerFactory.apply(array);
    }
}
