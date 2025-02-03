package com.loganasherjones.mac;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A class for writing logs from a subprocess.
 * <p>
 * This code was taken mostly from Accumulo, but then modified to allow
 * it to work safely with both log files and to stdout/stderr.
 * </p>
 *
 * @author loganasherjones
 * @since 1.10.4
 */
public class LogWriter extends Thread {

    private static final Logger log = LoggerFactory.getLogger(LogWriter.class);

    private final BufferedReader in;
    private final Timer timer;
    private BufferedWriter out;
    private final boolean safeToClose;

    /**
     * Create a LogWriter Thread.
     * <p>
     *     Generally, you'll want a LogWriter for both stdout and stderr
     *     for a process.
     * </p>
     *
     * <p>
     *     Note that when this thread stops being able to read from the
     *     input it will try to close the output if it is not stdout/stderr.
     * </p>
     *
     * @param in - An input stream from a process. (stdout/stderr)
     * @param out - An output stream to send the input stream to.
     * @since 1.10.4
     */
    public LogWriter(InputStream in, OutputStream out) {
        this.setDaemon(true);
        safeToClose = out != System.err && out != System.out;
        this.in = new BufferedReader(new InputStreamReader(in));
        this.out = new BufferedWriter(new OutputStreamWriter(out));
        this.timer = new Timer();
    }

    private void flushEverySecond() {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                try {
                    flush();
                } catch (IOException e) {
                    log.error("Unexpected IOException during flush", e);
                }
            }
        };
        timer.schedule(task, 1000, 1000);
    }

    /**
     * Forces a flush of any buffer contents to {@link #out}.
     *
     * @throws IOException if something goes wrong with the flush.
     * @since 1.10.4
     */
    public synchronized void flush() throws IOException {
        if (out != null) {
            out.flush();
        }
    }

    /**
     * Begin forwarding input to output.
     *
     * @since 1.10.4
     */
    @Override
    public void run() {
        flushEverySecond();
        String line;

        try {
            while ((line = in.readLine()) != null) {
                out.append(line);
                out.append("\n");
            }

            timer.cancel();
            timer.purge();
            synchronized (this) {
                if (safeToClose) {
                    out.close();
                }
                out = null;
                in.close();
            }
        } catch (IOException e) {
            // Left empty on purpose.
        }
    }
}
