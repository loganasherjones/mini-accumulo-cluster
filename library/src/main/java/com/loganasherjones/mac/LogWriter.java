package com.loganasherjones.mac;

import org.apache.accumulo.server.util.time.SimpleTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class LogWriter extends Thread {

    private static final Logger log = LoggerFactory.getLogger(LogWriter.class);

    private final BufferedReader in;
    private BufferedWriter out;
    private final boolean safeToClose;

    public LogWriter(InputStream in, OutputStream out) {
        this.setDaemon(true);
        safeToClose = out != System.err && out != System.out;
        this.in = new BufferedReader(new InputStreamReader(in));
        this.out = new BufferedWriter(new OutputStreamWriter(out));
    }

    private void flushEverySecond() {
        SimpleTimer.getInstance(null).schedule(
                () -> {
                    try {
                        flush();
                    } catch (IOException e) {
                        log.error("Unexpected IOException during flush", e);
                    }
                },
                1000, 1000);
    }

    public synchronized void flush() throws IOException {
        if (out != null) {
            out.flush();
        }
    }

    @Override
    public void run() {
        flushEverySecond();
        String line;

        try {
            while ((line = in.readLine()) != null) {
                out.append(line);
                out.append("\n");
            }

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
