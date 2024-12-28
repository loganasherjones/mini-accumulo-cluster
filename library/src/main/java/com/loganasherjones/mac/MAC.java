package com.loganasherjones.mac;

import org.apache.accumulo.cluster.AccumuloCluster;
import org.apache.accumulo.cluster.ClusterControl;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.ClientConfiguration;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.ZooKeeperInstance;
import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.conf.AccumuloConfiguration;
import org.apache.accumulo.minicluster.impl.ZooKeeperBindException;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.zookeeper.server.ZooKeeperServerMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MAC implements AccumuloCluster {

    private static final Logger log = LoggerFactory.getLogger(MAC.class);

    private final MACConfig config;
    private boolean initialized = false;
    private final Map<String, Process> processes = new HashMap<>();
    private final List<LogWriter> logWriters = new ArrayList<>();

    public MAC() {
        this(new MACConfig.MACConfigBuilder().build());
    }

    public MAC(MACConfig config) {
        this.config = config;
    }

    @Override
    public String getInstanceName() {
        return this.config.getInstanceName();
    }

    @Override
    public String getZooKeepers() {
        return "";
    }

    @Override
    public Connector getConnector(String user, AuthenticationToken token) throws AccumuloException, AccumuloSecurityException {
        Instance instance = new ZooKeeperInstance(getClientConfig());
        return instance.getConnector(user, token);
    }

    @Override
    public ClientConfiguration getClientConfig() {
        return ClientConfiguration
                .fromMap(new HashMap<>())
                .withInstance(this.getInstanceName())
                .withZkHosts(this.getZooKeepers());
    }

    @Override
    public AccumuloConfiguration getSiteConfiguration() {
        return null;
    }

    @Override
    public ClusterControl getClusterControl() {
        throw new RuntimeException("ClusterControl is not necessary for MAC.");
    }

    @Override
    public void start() throws Exception {
        if (initialized) {
            log.warn("start called on an already started MAC.");
            return;
        }

        config.createDirectoryStructure();

        ensureStopIsCalled();
        ensureZookeeperIsRunning();
//        initializeAccumulo();
//        startTabletServers();
//        setManagerGoalState();
//        startManager();
//        startGarbageCollector();

        initialized = true;
    }

    @Override
    public void stop() throws IOException, InterruptedException {
        log.debug("Flushing log writers...");
        for (LogWriter lw : logWriters) {
            lw.flush();
        }

        log.info("Stopping MAC...");
        for (Map.Entry<String, Process> entry: processes.entrySet()) {
            log.info("Stopping '{}' process", entry.getKey());
            Process p = entry.getValue();
            p.destroy();
            log.debug("Waiting for '{}' process to stop.", entry.getKey());
            p.waitFor();
            log.debug("Process '{}' successfully stopped.", entry.getKey());
        }
        log.info("Done stopping MAC.");
    }

    @Override
    public FileSystem getFileSystem() throws IOException {
        return null;
    }

    @Override
    public Path getTemporaryPath() {
        return null;
    }

    private void ensureZookeeperIsRunning() throws IOException, InterruptedException {
        startZookeeperProcess();
        waitForZookeeperToBeOk();
    }

    private void startZookeeperProcess() throws IOException {
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
        String classpath = config.getClasspathLoader().getClasspath();
        String className = ZooKeeperServerMain.class.getName();

        String processName = "mac-" + config.getMACId() + "-zookeeper";
        List<String> argList = new ArrayList<>(Arrays.asList(javaBin, "-Dproc=" + processName, "-cp", classpath));

        List<String> jvmOpts = new ArrayList<>();
        jvmOpts.add("-Dzookeeper.jmx.log4j.disable=true");

        // TODO: Allow the user to specify JVM Opts from the config.
        argList.addAll(jvmOpts);

        argList.add(className);
        argList.add(config.getZooCfgFile().getAbsolutePath());

        String foo = String.join(" ", argList);
        log.info(foo);
        ProcessBuilder builder = new ProcessBuilder(argList);

        Process process = builder.start();
        processes.put(processName, process);
        captureOutput(processName, process);
    }

    private void ensureStopIsCalled() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                log.info("MAC shutdown hook called. Attempting to stop...");
                MAC.this.stop();
                log.info("MAC shutdown hook complete successfully.");
            } catch (IOException e) {
                log.error("IOException while trying to stop MAC.", e);
            } catch (InterruptedException e) {
                log.error("InterruptedException while trying to stop MAC.", e);
            }
        }));
    }

    private void waitForZookeeperToBeOk() throws InterruptedException {
        log.info("Waiting for zookeeper to report ok.");

        long startTime = System.currentTimeMillis();
        while (true) {
            try (Socket s = new Socket(config.getZooKeeperHost(), config.getZooKeeperPort())) {
                log.debug("Checking if zookeeper is ok");
                s.setReuseAddress(true);
                s.getOutputStream().write("ruok\n".getBytes());
                s.getOutputStream().flush();
                byte[] buffer = new byte[100];
                int n = s.getInputStream().read(buffer);
                if (n >= 4 && new String(buffer, 0, 4).equals("imok")) {
                    log.info("Zookeeper is up.");
                    return;
                }
            } catch (Exception e) {
                log.debug("Zookeeper is not ok.");
                log.trace("Error message was: ", e);
                if (System.currentTimeMillis() - startTime >= config.getZooKeeperStartupTimeout()) {
                    throw new ZooKeeperBindException("Zookeeper did not start within "
                            + (config.getZooKeeperStartupTimeout() / 1000) + " seconds. Check the logs in "
                            + config.getLogDir() + " for errors.  Last exception: " + e);
                }
                // Don't spin absurdly fast
                Thread.sleep(250);
            }
        }
    }

    private void captureOutput(String processName, Process process) throws IOException {
        OutputStream stderrStream;
        OutputStream stdoutStream;
        if (config.logToFiles()) {
            File errLogFile = new File(config.getLogDir(), processName + ".err");
            stderrStream = Files.newOutputStream(errLogFile.toPath());

            File outLogFile = new File(config.getLogDir(), processName + ".out");
            stdoutStream = Files.newOutputStream(outLogFile.toPath());
        } else {
            stderrStream = System.err;
            stdoutStream = System.out;
        }
        LogWriter errLogWriter = new LogWriter(process.getErrorStream(), stderrStream);
        LogWriter outLogWriter = new LogWriter(process.getInputStream(), stdoutStream);
        errLogWriter.start();
        outLogWriter.start();
        logWriters.add(errLogWriter);
        logWriters.add(outLogWriter);
    }

}
