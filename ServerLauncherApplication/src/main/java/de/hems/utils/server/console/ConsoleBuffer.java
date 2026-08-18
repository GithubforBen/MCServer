package de.hems.utils.server.console;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

/**
 * The recent console output of one server, plus whoever is watching it live.
 * <p>
 * Holds the last {@code capacity} lines so a browser that connects gets the context it needs right away,
 * and forwards every new line to the open websockets.
 */
public class ConsoleBuffer {

    /** How many lines are kept for someone who connects later. */
    public static final int DEFAULT_CAPACITY = 300;

    private final int capacity;
    private final Deque<String> lines = new ArrayDeque<>();
    private final List<Consumer<String>> listeners = new ArrayList<>();

    public ConsoleBuffer() {
        this(DEFAULT_CAPACITY);
    }

    public ConsoleBuffer(int capacity) {
        this.capacity = Math.max(1, capacity);
    }

    /**
     * Adds a line and hands it to everyone watching.
     * <p>
     * The listeners are called while the lock is held. That is deliberate: it makes subscribing and
     * appending atomic against each other, so a viewer can not miss a line that arrives between reading the
     * history and being registered, nor see it twice. It is only safe because sending on a websocket is
     * asynchronous - a listener must never block.
     *
     * @param line the line, already free of terminal escape codes
     */
    public void append(String line) {
        synchronized (lines) {
            lines.addLast(line);
            while (lines.size() > capacity) lines.removeFirst();
            for (Consumer<String> listener : listeners) {
                try {
                    listener.accept(line);
                } catch (Exception e) {
                    // a viewer that went away must not stop the others from getting their output
                }
            }
        }
    }

    /**
     * @return the lines that are currently kept, oldest first
     */
    public List<String> history() {
        synchronized (lines) {
            return List.copyOf(lines);
        }
    }

    /**
     * Starts watching. The returned subscription carries the history as it was at that exact moment, so the
     * caller can send it before the first live line arrives without risking a gap or a duplicate.
     *
     * @param listener what to call for every new line
     * @return the subscription, which has to be closed when the viewer goes away
     */
    public Subscription subscribe(Consumer<String> listener) {
        synchronized (lines) {
            List<String> history = List.copyOf(lines);
            listeners.add(listener);
            return new Subscription(history, listener);
        }
    }

    /**
     * @return how many websockets are watching this console
     */
    public int getListenerCount() {
        synchronized (lines) {
            return listeners.size();
        }
    }

    public void clear() {
        synchronized (lines) {
            lines.clear();
        }
    }

    /**
     * One viewer of a console.
     */
    public class Subscription implements AutoCloseable {

        private final List<String> history;
        private final Consumer<String> listener;

        private Subscription(List<String> history, Consumer<String> listener) {
            this.history = history;
            this.listener = listener;
        }

        /**
         * @return the lines that were already there when the viewer connected
         */
        public List<String> getHistory() {
            return history;
        }

        @Override
        public void close() {
            synchronized (lines) {
                listeners.remove(listener);
            }
        }
    }
}
