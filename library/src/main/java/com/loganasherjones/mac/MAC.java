package com.loganasherjones.mac;

import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.ClientConfiguration;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.ZooKeeperInstance;
import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.master.thrift.MasterGoalState;
import org.apache.accumulo.gc.SimpleGarbageCollector;
import org.apache.accumulo.master.Master;
import org.apache.accumulo.master.state.SetGoalState;
import org.apache.accumulo.minicluster.impl.ZooKeeperBindException;
import org.apache.accumulo.server.init.Initialize;
import org.apache.accumulo.shell.Shell;
import org.apache.accumulo.tserver.TabletServer;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MAC {

    private static final Logger log = LoggerFactory.getLogger(MAC.class);

    private final MACConfig config;
    private final MACProcessSpawner spawner;
    private boolean initialized = false;
    private final List<MACProcess> macProcesses = new ArrayList<>();
    private final Map<String, Process> processes = new HashMap<>();
    private final List<LogWriter> logWriters = new ArrayList<>();

    public MAC() {
        this(new MACConfig.MACConfigBuilder().build());
    }

    public MAC(MACConfig config) {
        this.config = config;
        this.spawner = new MACProcessSpawner(config.getClasspathLoader(), config.logToFiles(), config.getLogDir());
    }

    public String getInstanceName() {
        return this.config.getInstanceName();
    }

    public String getZooKeepers() {
        return config.getZooKeeperHost() + ":" + config.getZooKeeperPort();
    }

    public Connector getConnector(String user, AuthenticationToken token) throws AccumuloException, AccumuloSecurityException {
        Instance instance = new ZooKeeperInstance(getClientConfig());
        return instance.getConnector(user, token);
    }

    private ClientConfiguration getClientConfig() {
        return ClientConfiguration
                .fromMap(config.getSiteConfig())
                .withInstance(this.getInstanceName())
                .withZkHosts(this.getZooKeepers());
    }

    public void start() throws Exception {
        if (initialized) {
            log.warn("start called on an already started MAC.");
            return;
        }
        // TODO: Add thread safeness here

        log.info("Starting Mini Accumulo Cluster");
        config.createDirectoryStructure();

        ensureStopIsCalled();
        ensureZookeeperIsRunning();
        initializeAccumulo();
        startTabletServers();
        setManagerGoalState();
        startManager();
        startGarbageCollector();

        initialized = true;
        log.info("Mini Accumulo Cluster Started successfully");
    }

    public void stop() throws IOException, InterruptedException {
        // TODO: Add thread-safeness here.
        log.debug("Flushing log writers...");
        for (LogWriter lw : logWriters) {
            lw.flush();
        }

        log.info("Stopping Mini Accumulo Cluster");
        log.debug("Stopping {} MAC processes...", processes.size());
        for (Map.Entry<String, Process> entry : processes.entrySet()) {
            log.debug("Stopping '{}' process", entry.getKey());
            Process p = entry.getValue();
            p.destroy();
            log.debug("Waiting for '{}' process to stop.", entry.getKey());
            p.waitFor();
            log.debug("Process '{}' successfully stopped.", entry.getKey());
        }

        for (MACProcess process : macProcesses) {
            process.stop();
        }
        log.info("Mini Accumulo Cluster stopped.");
    }

    public void runShell() throws IOException {
        String[] shellArgs = new String[]{"-u", "root", "-p", config.getRootPassword(), "-zi", config.getInstanceName(), "-zh", config.getZooKeeperHost() + ":" + config.getZooKeeperPort()};
        Shell shell = new Shell();
        shell.config(shellArgs);
        shell.start();
        shell.shutdown();
    }

    private void ensureZookeeperIsRunning() throws IOException, InterruptedException {
        if (!config.useExistingZookeeper()) {
            startZookeeperProcess();
        }
        waitForZookeeperToBeOk();
    }

    private void startGarbageCollector() throws IOException {
        String processName = "mac-" + config.getMACId() + "-gc";
        MACProcess process = spawner.spawnProcess(
                processName,
                SimpleGarbageCollector.class.getName(),
                getAccumuloAddressArgs(),
                config.getAccumuloGCJvmProperties()
        );
        macProcesses.add(process);
    }


    private void startManager() throws IOException {
        // TODO: Add support for multiple tablet servers.
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
        String classpath = config.getClasspathLoader().getClasspath();
        String className = Master.class.getName();

        String processName = "mac-" + config.getMACId() + "-manager";
        List<String> argList = new ArrayList<>(Arrays.asList(javaBin, "-Dproc=" + processName, "-cp", classpath));

//        List<String> jvmOpts = new ArrayList<>();

        // TODO: Allow the user to specify JVM Opts from the config.
//        argList.addAll(jvmOpts);

        argList.add(className);
        addAddressArg(argList);
//        argList.add(config.getZooCfgFile().getAbsolutePath());

        ProcessBuilder builder = new ProcessBuilder(argList);

        log.info("Starting Accumulo Manager");
        log.debug(String.join(" ", argList));
        Process process = builder.start();
        processes.put(processName, process);
        captureOutput(processName, process);
    }

    private void setManagerGoalState() throws IOException, InterruptedException {
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
        String classpath = config.getClasspathLoader().getClasspath();
        String className = SetGoalState.class.getName();

        String processName = "mac-" + config.getMACId() + "-accumulo-manager-state";
        List<String> argList = new ArrayList<>(Arrays.asList(javaBin, "-Dproc=" + processName, "-cp", classpath));

        // TODO: Allow the user to specify JVM Opts from the config.
        argList.add(className);
        argList.add(MasterGoalState.NORMAL.toString());

        ProcessBuilder builder = new ProcessBuilder(argList);
        log.info("Setting accumulo manager goal state.");
        log.debug(String.join(" ", argList));
        Process process = builder.start();
        captureOutput(processName, process);
        int retCode = process.waitFor();
        if (retCode != 0) {
            log.error("Error setting manager goal state.");
            throw new RuntimeException("Error setting manager goal state.");
        }

    }

    private void startTabletServers() throws IOException {
        log.info("Starting tablet servers...");
        // TODO: Add support for multiple tablet servers.
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
        String classpath = config.getClasspathLoader().getClasspath();
        String className = TabletServer.class.getName();

        String processName = "mac-" + config.getMACId() + "-tserver";
        List<String> argList = new ArrayList<>(Arrays.asList(javaBin, "-Dproc=" + processName, "-cp", classpath));

//        List<String> jvmOpts = new ArrayList<>();

        // TODO: Allow the user to specify JVM Opts from the config.
//        argList.addAll(jvmOpts);

        argList.add(className);
        addAddressArg(argList);
//        argList.add(config.getZooCfgFile().getAbsolutePath());

        log.debug("Tablet server command: {}", String.join(" ", argList));
        ProcessBuilder builder = new ProcessBuilder(argList);
        // This is required to make lib ext loading work correctly.
        builder.environment().put("ACCUMULO_HOME", config.getBaseDirectory().getAbsolutePath());

        Process process = builder.start();
        processes.put(processName, process);
        captureOutput(processName, process);
    }

    private void startZookeeperProcess() throws IOException {
        String processName = "mac-" + config.getMACId() + "-zookeeper";
        String className = ZooKeeperServerMain.class.getName();

        List<String> additionalArgs = new ArrayList<>();
        additionalArgs.add(config.getZooCfgFile().getAbsolutePath());

        MACProcess process = spawner.spawnProcess(processName, className, additionalArgs, config.getZooKeeperJvmProperties());
        macProcesses.add(process);
    }


    private void initializeAccumulo() throws IOException, InterruptedException {
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
        String classpath = config.getClasspathLoader().getClasspath();
        String className = Initialize.class.getName();

        String processName = "mac-" + config.getMACId() + "-accumulo-init";
        List<String> argList = new ArrayList<>(Arrays.asList(javaBin, "-Dproc=" + processName, "-cp", classpath));

        // TODO: Allow the user to specify JVM Opts from the config.
        argList.add(className);
        argList.add("--instance-name");
        argList.add(config.getInstanceName());
        argList.add("--user");
        argList.add("root");
        argList.add("--clear-instance-name");
        argList.add("--password");
        argList.add(config.getRootPassword());

        ProcessBuilder builder = new ProcessBuilder(argList);
        log.info("Running accumulo init.");
        log.debug(String.join(" ", argList));
        Process process = builder.start();
        captureOutput(processName, process);
        int retCode = process.waitFor();
        if (retCode != 0) {
            log.error("Error initializing accumulo.");
            throw new RuntimeException("Error initializing accumulo.");
        }
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
                    log.info("Zookeeper reported ok.");
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

    private void addAddressArg(List<String> args) {
        String address = config.getAccumuloBindAddress();
        if (address == null || address.isEmpty()) {
            return;
        }
        args.add("-a");
        args.add(address);
    }

    private List<String> getAccumuloAddressArgs() {
        String address = config.getAccumuloBindAddress();
        if (address == null || address.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        result.add("-a");
        result.add(address);
        return result;
    }

}
