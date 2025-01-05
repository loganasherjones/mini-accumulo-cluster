package com.loganasherjones.mac;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A helper class for spawning processes for the mini-accumulo-cluster.
 */
public class MACProcessSpawner {
    private static final Logger log = LoggerFactory.getLogger(MACProcessSpawner.class);
    private static final String javaHome = System.getProperty("java.home");
    private static final String javaBin = javaHome + File.separator + "bin" + File.separator + "java";

    private final ClasspathLoader classpathLoader;
    private final boolean logToFile;
    private final File logDirectory;

    public MACProcessSpawner(ClasspathLoader classpathLoader, boolean logToFile, File logDirectory) {
        this.classpathLoader = classpathLoader;
        this.logToFile = logToFile;
        this.logDirectory = logDirectory;
    }

    public MACProcess spawnProcess(
            String processName,
            String className,
            List<String> cliArgs
    ) throws IOException {
        String classpath = classpathLoader.getClasspath();
        List<String> argList = new ArrayList<>(Arrays.asList(javaBin, "-Dproc=" + processName, "-cp", classpath, className));
        argList.addAll(cliArgs);

        log.info("Starting {} Process", processName);
        log.debug(String.join(" ", argList));
        ProcessBuilder builder = new ProcessBuilder(argList);
        Process process = builder.start();
        LogWriter stderrWriter = spawnStdErrWriter(processName, process);
        LogWriter stdoutWriter = spawnStdOutWriter(processName, process);
        return new MACProcess(processName, process, stderrWriter, stdoutWriter);
    }

    private LogWriter spawnStdErrWriter(String processName, Process process) throws IOException {
        OutputStream stderrStream;
        if (logToFile) {
            File errLogFile = new File(logDirectory, processName + ".err");
            stderrStream = Files.newOutputStream(errLogFile.toPath());
        } else {
            stderrStream = System.err;
        }
        LogWriter writer = new LogWriter(process.getErrorStream(), stderrStream);
        writer.start();
        return writer;
    }

    private LogWriter spawnStdOutWriter(String processName, Process process) throws IOException {
        OutputStream stdoutStream;
        if (logToFile) {
            File outLogFile = new File(logDirectory, processName + ".out");
            stdoutStream = Files.newOutputStream(outLogFile.toPath());
        } else {
            stdoutStream = System.out;
        }
        LogWriter writer = new LogWriter(process.getInputStream(), stdoutStream);
        writer.start();
        return writer;
    }


}
