package de.hems.utils.server.console;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Follows a file that a server writes its console output into and feeds every finished line into a
 * {@link ConsoleBuffer}.
 * <p>
 * The servers run inside tmux, so their output never passes through this process. {@code tmux pipe-pane}
 * writes it into a file instead, and this is the other end of that pipe.
 */
public class ConsoleTailer {

    /** How long to wait before looking at the file again. */
    private static final long POLL_INTERVAL_MS = 200L;
    /** How much is read in one go, so a huge burst does not become one huge string. */
    private static final int READ_CHUNK = 64 * 1024;

    /**
     * The escape sequences a terminal program writes to colour its output and move the cursor. Paper and
     * velocity both do that, and without stripping them the browser would show the raw codes.
     */
    private static final Pattern ANSI = Pattern.compile(
            "\\x1B\\[[0-9;?]*[ -/]*[@-~]"              // CSI - colours and cursor movement
                    + "|\\x1B\\][^\\x07\\x1B]*(?:\\x07|\\x1B\\\\)"  // OSC - window titles
                    + "|\\x1B[@-Z\\\\-_]");                          // the single character escapes

    private final File file;
    private final ConsoleBuffer buffer;
    private volatile boolean running;
    private Thread thread;
    /** How far into the file we have read. */
    private long offset;
    /** The tail of the last read that did not end in a newline yet. */
    private StringBuilder partial = new StringBuilder();

    public ConsoleTailer(File file, ConsoleBuffer buffer) {
        this.file = file;
        this.buffer = buffer;
    }

    /**
     * Starts following the file. Safe to call before the file exists - the tailer waits for it to appear.
     */
    public void start() {
        if (running) return;
        running = true;
        thread = new Thread(this::run, "console-tailer-" + file.getParentFile().getName());
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        running = false;
        Thread current = thread;
        thread = null;
        if (current != null) current.interrupt();
    }

    public boolean isRunning() {
        return running;
    }

    private void run() {
        while (running) {
            try {
                readNewBytes();
            } catch (IOException e) {
                // the file can be replaced while a server restarts - just try again next round
            }
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /**
     * Reads whatever was appended since the last look and turns it into lines.
     */
    private void readNewBytes() throws IOException {
        if (!file.exists()) {
            offset = 0L;
            return;
        }
        long length = file.length();
        if (length < offset) {
            // the file was replaced or truncated, for example because the server was restarted
            offset = 0L;
            partial = new StringBuilder();
        }
        if (length == offset) return;

        try (RandomAccessFile access = new RandomAccessFile(file, "r")) {
            access.seek(offset);
            while (offset < length) {
                byte[] bytes = new byte[(int) Math.min(READ_CHUNK, length - offset)];
                int read = access.read(bytes);
                if (read <= 0) break;
                offset += read;
                consume(new String(bytes, 0, read, StandardCharsets.UTF_8));
            }
        }
    }

    /**
     * Splits what was read into lines, keeping an unfinished last line for the next round.
     *
     * @param text the newly read text
     */
    private void consume(String text) {
        partial.append(text);
        int start = 0;
        for (int i = 0; i < partial.length(); i++) {
            if (partial.charAt(i) != '\n') continue;
            emit(partial.substring(start, i));
            start = i + 1;
        }
        partial = new StringBuilder(partial.substring(start));
        // a line that never gets its newline must not grow without bound
        if (partial.length() > READ_CHUNK) {
            emit(partial.toString());
            partial = new StringBuilder();
        }
    }

    /**
     * Cleans a line up and hands it on. Lines that are nothing but terminal control codes - which the
     * prompt of a minecraft server redraws constantly - are dropped.
     *
     * @param raw the line as it was written
     */
    private void emit(String raw) {
        String line = clean(raw);
        if (line.isEmpty()) return;
        buffer.append(line);
    }

    /**
     * @param raw a line of raw terminal output
     * @return that line without escape sequences, carriage returns and other control characters
     */
    public static String clean(String raw) {
        String stripped = ANSI.matcher(raw).replaceAll("");
        StringBuilder result = new StringBuilder(stripped.length());
        for (char c : stripped.toCharArray()) {
            // keep tabs, drop the rest of the control range including carriage return and backspace
            if (c == '\t' || c >= 0x20) result.append(c);
        }
        return result.toString().stripTrailing();
    }
}
