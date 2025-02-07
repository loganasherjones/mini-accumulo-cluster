package com.loganasherjones.mac;

import org.apache.accumulo.core.client.Accumulo;
import org.apache.accumulo.core.client.AccumuloClient;
import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.ClientConfiguration;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.ZooKeeperInstance;
import org.apache.accumulo.core.client.security.tokens.AuthenticationToken;
import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.core.manager.thrift.ManagerGoalState;
import org.apache.accumulo.gc.SimpleGarbageCollector;
import org.apache.accumulo.manager.Manager;
import org.apache.accumulo.manager.state.SetGoalState;
import org.apache.accumulo.server.init.Initialize;
import org.apache.accumulo.shell.Shell;
import org.apache.accumulo.tserver.TabletServer;
import org.apache.zookeeper.server.ZooKeeperServerMain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A Mini Accumulo Cluster suitable for integration tests.
 *
 * <p>
 *     This class spawns all the necessary processes to run several Accumulo
 *     processes. The main use-case for this class is integration tests.
 * </p>
 *
 * <p>
 *     The simplest possible usage is:
 * </p>
 *
 * <pre>{@code
 * @Test
 * public void myTest() {
 *     // Create mac with default configuration.
 *     MAC mac = new MAC();
 *     // Spawn the server.
 *     mac.start();
 *     Connector rootConnector = mac.getRootConnector();
 *     // Do whatever with the rootConnector
 *     mac.stop();
 * }
 * }</pre>
 *
 * There are many configuration options. See the {@link MACConfig.MACConfigBuilder}
 *
 * @author loganasherjones
 */
public class MAC {

    private static final Logger log = LoggerFactory.getLogger(MAC.class);

    private final MACConfig config;
    private final MACProcessSpawner spawner;
    private boolean initialized = false;
    private boolean stopped = false;
    private final List<MACProcess> macProcesses = new ArrayList<>();

    /**
     * Create a new Mini Accumulo Cluster with the default configuration.
     * @since 1.10.4
     */
    public MAC() {
        this(new MACConfig.MACConfigBuilder().build());
    }

    /**
     * Create a Mini Accumulo Cluster with the specified configuration.
     *
     * @param config - configuration to use.
     * @since 1.10.4
     */
    public MAC(MACConfig config) {
        this.config = config;
        this.spawner = new MACProcessSpawner(config.getClasspathLoader(), config.shouldLogToFile(), config.getLogDir());
    }

    /**
     * Get the zookeeper instance name.
     * @return the zookeeper instance name.
     * @since 1.10.4
     */
    public String getInstanceName() {
        return this.config.getInstanceName();
    }

    /**
     * Get the zookeeper connection string.
     * @return the zookeeper connection string.
     * @since 1.10.4
     */
    public String getZooKeepers() {
        return config.getZooKeeperHost() + ":" + config.getZooKeeperPort();
    }

    /**
     * Get an {@link AccumuloClient} for the 'root' user.
     * </p>
     * Note that if you change root's password, after starting
     * this function will not work correctly.
     * </p>
     * @return An AccumuloClient for root
     */
    public AccumuloClient getRootClient() {
        return getClient("root", new PasswordToken(config.getRootPassword()));
    }

    /**
     * Get an {@link AccumuloClient} for the specified user/password.
     * </p>
     * @see Accumulo#newClient()
     *
     * @param user - Accumulo username
     * @param token - Accumulo username's password
     * @return an Accumulo Client
     */
    public AccumuloClient getClient(String user, AuthenticationToken token) {
        return Accumulo
                .newClient()
                .to(getInstanceName(), getZooKeepers())
                .as(user, token)
                .build();
    }

    /**
     * Deprecated. Use {@link #getClient(String, AuthenticationToken)} )} instead.
     * <p>
     * Get an {@link Connector} for the specified user/password.
     * </p>
     * @see ZooKeeperInstance#getConnector(String, AuthenticationToken)
     * @param user - Accumulo username
     * @param token - Accumulo username's password
     * @return an Accumulo Connector
     * @throws AccumuloException if something unexpected goes wrong.
     * @throws AccumuloSecurityException If there is an auth problem.
     */
    @Deprecated(since = "2.1.3")
    public Connector getConnector(String user, AuthenticationToken token) throws AccumuloException, AccumuloSecurityException {
        Instance instance = new ZooKeeperInstance(getClientConfig());
        return instance.getConnector(user, token);
    }

    /**
     * Deprecated. Use {@link #getRootClient()} instead.
     * <p>
     * Get an {@link Connector} for the 'root' user.
     * </p>
     * Note that if you change root's password, after starting
     * this function will not work correctly.
     * </p>
     * @return A root connector
     * @throws AccumuloException if something unexpected goes wrong.
     * @throws AccumuloSecurityException If there is an auth problem.
     */
    @Deprecated(since = "2.1.3")
    public Connector getRootConnector() throws AccumuloException, AccumuloSecurityException {
        return getConnector("root", new PasswordToken(config.getRootPassword()));
    }

    @Deprecated
    private ClientConfiguration getClientConfig() {
        return ClientConfiguration
                .fromMap(config.getSiteConfig())
                .withInstance(this.getInstanceName())
                .withZkHosts(this.getZooKeepers());
    }

    /**
     * Spawn a mini-accumulo cluster.
     * @throws Exception if something goes wrong.
     * @since 1.10.4
     */
    public void start() throws Exception {
        if (initialized) {
            log.warn("start called on an already started MAC.");
            return;
        }

        synchronized (this) {
            if (!initialized) {
                log.info("Starting Mini Accumulo Cluster");
                config.createDirectoryStructure();
                if (config.shouldLogToFile()) {
                    log.info("You can find logs at: {}", config.getLogDir());
                }

                ensureStopIsCalled();
                ensureZookeeperIsRunning();
                initializeAccumulo();
                startTabletServers();
                setManagerGoalState();
                startManager();
                startGarbageCollector();

                initialized = true;
            }
        }
        log.info("Mini Accumulo Cluster Started successfully");
    }

    /**
     * Stops the mini-accumulo-cluster.
     *
     * @throws IOException - if something goes wrong.
     * @throws InterruptedException - if interrupted while stopping.
     * @since 1.10.4
     */
    public void stop() throws IOException, InterruptedException {
        if (stopped) {
            log.warn("Stop called multiple times. Ignoring.");
            return;
        }

        synchronized (this) {
            if (!stopped) {
                log.info("Stopping Mini Accumulo Cluster");
                for (MACProcess process : macProcesses) {
                    process.stop();
                }
                log.info("Mini Accumulo Cluster stopped.");
            }
            stopped = true;
        }
    }

    /**
     * Spawns an accumulo shell to the current mini-accumulo-cluster.
     * This is mostly intended for debugging purposes.
     *
     * @throws IOException - if something goes wrong.
     * @since 1.10.4
     */
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
        String processName = "mac-" + config.getMACId() + "-manager";
        MACProcess process = spawner.spawnProcess(
                processName,
                Manager.class.getName(),
                getAccumuloAddressArgs(),
                config.getAccumuloManagerJvmProperties()
        );
        macProcesses.add(process);
    }

    private void setManagerGoalState() throws IOException, InterruptedException {
        String processName = "mac-" + config.getMACId() + "-accumulo-manager-state";
        MACProcess process = spawner.spawnProcess(
                processName,
                SetGoalState.class.getName(),
                Collections.singletonList(ManagerGoalState.NORMAL.toString()),
                config.getAccumuloInitJvmProperties()
        );
        int retCode = process.waitFor();
        if (retCode != 0) {
            log.error("Error setting manager goal state.");
            throw new RuntimeException("Error setting manager goal state.");
        }

    }

    private void startTabletServers() throws IOException {
        for (int i = 0; i < config.getNumTservers(); i++) {
            startTabletServer(i);
        }
    }

    private void startTabletServer(int num) throws IOException {
        String processName = "mac-" + config.getMACId() + "-tserver-" + num;
        Map<String, String> env = new HashMap<>();
        env.put("ACCUMULO_HOME", config.getBaseDirectory().getAbsolutePath());
        MACProcess process = spawner.spawnProcess(
                processName,
                TabletServer.class.getName(),
                getAccumuloAddressArgs(),
                config.getAccumuloTserverJvmProperties(),
                env
        );
        macProcesses.add(process);
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
        String processName = "mac-" + config.getMACId() + "-accumulo-init";
        List<String> additionalArgs = new ArrayList<>();
        additionalArgs.add("--instance-name");
        additionalArgs.add(config.getInstanceName());
        additionalArgs.add("--user");
        additionalArgs.add("root");
        additionalArgs.add("--clear-instance-name");
        additionalArgs.add("--password");
        additionalArgs.add(config.getRootPassword());
        MACProcess process = spawner.spawnProcess(
                processName,
                Initialize.class.getName(),
                additionalArgs,
                config.getAccumuloInitJvmProperties()
        );
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
                    throw new RuntimeException("Zookeeper did not start within "
                            + (config.getZooKeeperStartupTimeout() / 1000) + " seconds. Check the logs in "
                            + config.getLogDir() + " for errors.  Last exception: " + e);
                }
                // Don't spin absurdly fast
                Thread.sleep(250);
            }
        }
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
