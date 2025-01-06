package com.loganasherjones.mac;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * A wrapper for a process which includes capture of stdout/stderr.
 * <p>
 * Expected to be used internally. Not a safe API to depend on.
 * </p>
 *
 * @author loganasherjones
 * @since 1.10.4
 */
public class MACProcess {
    private static final Logger log = LoggerFactory.getLogger(MACProcess.class);

    private final String processName;
    private final Process process;
    private final LogWriter errLogWriter;
    private final LogWriter outLogWriter;

    MACProcess(String processName, Process process, LogWriter errLogWriter, LogWriter outLogWriter) {
        this.processName = processName;
        this.process = process;
        this.errLogWriter = errLogWriter;
        this.outLogWriter = outLogWriter;
    }

    /**
     * Stop the wrapped process and flush stdout/stderr to their outputs.
     *
     * @throws IOException - If something goes wrong writing output.
     * @throws InterruptedException - If interrupted while waiting for process to die.
     * @since 1.10.4
     */
    public void stop() throws IOException, InterruptedException {
        log.debug("Flushing stderr for {}", processName);
        this.errLogWriter.flush();
        log.debug("Flushing stdout for {}", processName);
        this.outLogWriter.flush();

        log.debug("Stopping {} process", processName);
        process.destroy();
        log.debug("Waiting for {} process to stop", processName);
        int retCode = process.waitFor();
        log.debug("Process {} stopped (return={})", processName, retCode);
    }

    /**
     * @see Process#waitFor()
     */
    public int waitFor() throws InterruptedException {
        return process.waitFor();
    }
}
