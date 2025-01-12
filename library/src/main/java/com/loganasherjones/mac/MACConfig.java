package com.loganasherjones.mac;

import org.apache.accumulo.core.conf.Property;
import org.apache.accumulo.server.util.PortUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

/**
 * Configuration for the {@link MAC}. To construct this object use
 * the {@link MACConfigBuilder}
 *
 * @author loganasherjones
 * @since 1.10.4
 */
public class MACConfig {

    private final String instanceName;
    private final String rootPassword;
    private final String id;
    private final ClasspathLoader classpathLoader;
    private final boolean logToFile;
    private final int zooKeeperStartupTimeout;
    private final File baseDirectory;
    private final File configDirectory;
    private final File logDirectory;
    private final String zooKeeperHost;
    private int zooKeeperPort;
    private final Boolean forceExternalZookeeper;
    private final Map<String, String> siteConfig;
    private final String accumuloBindAddress;
    private final Map<String, Map<String, String>> jvmProperties = new HashMap<>();
    private final int numTservers;
    private final Map<String, String> zooCfg;

    private MACConfig(
            String instanceName,
            String rootPassword,
            ClasspathLoader classpathLoader,
            String id,
            boolean logToFile,
            int zooKeeperStartupTimeout,
            File baseDirectory,
            String zooKeeperHost,
            int zooKeeperPort,
            Map<String, String> siteConfig,
            String accumuloBindAddress,
            Map<String, String> zooKeeperJvmProperties,
            Map<String, String> accumuloGCJvmProperties,
            Map<String, String> accumuloManagerJvmProperties,
            Map<String, String> accumuloTserverJvmProperties,
            Map<String, String> accumuloInitJvmProperties,
            int numTservers,
            Map<String, String> zooCfg,
            Boolean forceExternalZookeeper
    ) {
        this.instanceName = instanceName;
        this.rootPassword = rootPassword;
        this.classpathLoader = classpathLoader;
        this.id = id;
        this.logToFile = logToFile;
        this.zooKeeperStartupTimeout = zooKeeperStartupTimeout;
        this.baseDirectory = baseDirectory;
        this.configDirectory = new File(baseDirectory, "conf");
        this.logDirectory = new File(baseDirectory, "logs");
        this.zooKeeperHost = zooKeeperHost;
        this.zooKeeperPort = zooKeeperPort;
        this.siteConfig = siteConfig;
        this.accumuloBindAddress = accumuloBindAddress;
        this.jvmProperties.put("zookeeper", zooKeeperJvmProperties);
        this.jvmProperties.put("gc", accumuloGCJvmProperties);
        this.jvmProperties.put("manager", accumuloManagerJvmProperties);
        this.jvmProperties.put("tserver", accumuloTserverJvmProperties);
        this.jvmProperties.put("init", accumuloInitJvmProperties);
        this.numTservers = numTservers;
        this.zooCfg = zooCfg;
        this.forceExternalZookeeper = forceExternalZookeeper;
    }

    /**
     * Returns the accumulo instance name.
     *
     * @return the accumulo instance name.
     * @since 1.10.4
     */
    public String getInstanceName() {
        return instanceName;
    }

    /**
     * Returns the original 'root' user's password.
     *
     * @return the original 'root' user's password.
     * @since 1.10.4
     */
    public String getRootPassword() {
        return rootPassword;
    }

    /**
     * Returns the specified {@link ClasspathLoader}
     *
     * @return the specified {@link ClasspathLoader}
     * @since 1.10.4
     */
    public ClasspathLoader getClasspathLoader() {
        return classpathLoader;
    }

    /**
     * Returns the unique ID for this MAC.
     *
     * @return the unique ID for this MAC.
     * @since 1.10.4
     */
    public String getMACId() {
        return this.id;
    }

    /**
     * Returns whether MAC will log to files.
     *
     * @return whether MAC will log to files.
     * @since 1.10.4
     */
    public boolean shouldLogToFile() {
        return this.logToFile;
    }

    /**
     * Returns time to wait for zookeeper to start in milliseconds.
     *
     * @return time to wait for zookeeper to start in milliseconds.
     * @since 1.10.4
     */
    public int getZooKeeperStartupTimeout() {
        return this.zooKeeperStartupTimeout;
    }

    /**
     * Returns the location of the zoo.cfg file.
     *
     * @return the location of the zoo.cfg file.
     * @since 1.10.4
     */
    public File getZooCfgFile() {
        return new File(this.configDirectory, "zoo.cfg");
    }

    /**
     * Returns the log directory where log files should be stored.
     *
     * @return the log directory where log files should be stored.
     * @since 1.10.4
     */
    public File getLogDir() {
        return this.logDirectory;
    }

    /**
     * Returns the zookeeper host accumulo should connect to.
     *
     * @return the zookeeper host accumulo should connect to.
     * @since 1.10.4
     */
    public String getZooKeeperHost() {
        return this.zooKeeperHost;
    }

    /**
     * Returns the bind address for accumulo.
     *
     * @return the bind address for accumulo.
     * @since 1.10.4
     */
    public String getAccumuloBindAddress() {
        return this.accumuloBindAddress;
    }

    /**
     * Returns the key/value pairs to use for accumulo-site.xml
     *
     * @return the key/value pairs to use for accumulo-site.xml
     * @since 1.10.4
     */
    public Map<String, String> getSiteConfig() {
        return this.siteConfig;
    }

    /**
     * Returns the base directory where configuration/logging goes.
     *
     * @return the base directory where configuration/logging goes.
     * @since 1.10.4
     */
    public File getBaseDirectory() { return this.baseDirectory; }

    /**
     * Returns the actual bound zookeeper port. If -1 was provided, then a
     * random port will be selected. So once this function has been called once
     * the zookeeperPort will be updated to the selected port.
     *
     * @return the actual bound zookeeper port.
     * @since 1.10.4
     */
    public int getZooKeeperPort() {
        if (this.zooKeeperPort != -1) {
            return this.zooKeeperPort;
        }

        synchronized (this) {
            if (this.zooKeeperPort == -1) {
                this.zooKeeperPort = PortUtils.getRandomFreePort();
                this.siteConfig.put("instance.zookeeper.host", zooKeeperHost + ":" + zooKeeperPort);
            }
        }

        return this.zooKeeperPort;
    }

    /**
     * Returns the JVM system properties to use for the zookeeper process.
     *
     * @return the JVM system properties to use for the zookeeper process.
     * @since 1.10.4
     */
    public Map<String, String> getZooKeeperJvmProperties() {
        return jvmProperties.get("zookeeper");
    }

    /**
     * Returns the JVM system properties to use for the accumulo GC process.
     *
     * @return the JVM system properties to use for the accumulo GC process.
     * @since 1.10.4
     */
    public Map<String, String> getAccumuloGCJvmProperties() {
        return jvmProperties.get("gc");
    }

    /**
     * Creates directory structure of the configuration.
     * This gets called automatically by {@link MAC#start()}.
     *
     * @throws IOException - If there is a problem creating a directory.
     */
    public void createDirectoryStructure() throws IOException {
        if (!this.baseDirectory.exists()) {
            this.baseDirectory.mkdirs();
        }

        if (!this.baseDirectory.isDirectory()) {
            throw new IllegalArgumentException("Base Directory " + this.baseDirectory + " was not a directory.");
        }

        configDirectory.mkdirs();
        logDirectory.mkdirs();

        File zooCfgFile = new File(configDirectory, "zoo.cfg");
        if (!zooCfgFile.exists()) {
            FileWriter fw = new FileWriter(zooCfgFile);
            if (!zooCfg.containsKey("clientPort")) {
                zooCfg.put("clientPort", Integer.toString(getZooKeeperPort()));
            }
            Properties zooCfgProps = new Properties();
            for (Map.Entry<String, String> entry : zooCfg.entrySet()) {
                zooCfgProps.setProperty(entry.getKey(), entry.getValue());
            }
            zooCfgProps.store(fw, null);
            fw.close();
        }

        File siteXml = new File(configDirectory, "accumulo-site.xml");
        if (!siteXml.exists()) {
            FileWriter fileWriter = new FileWriter(siteXml);
            fileWriter.append("<configuration>").append(System.lineSeparator());
            for (Map.Entry<String, String> item : siteConfig.entrySet()) {
                String escapedValue = escapeXmlString(item.getValue());
                fileWriter.append("  <property>").append(System.lineSeparator());
                fileWriter.append("    <name>").append(item.getKey()).append("</name>").append(System.lineSeparator());
                fileWriter.append("    <value>").append(escapedValue).append("</value>").append(System.lineSeparator());
                fileWriter.append("  </property>").append(System.lineSeparator());
            }
            fileWriter.append("</configuration>").append(System.lineSeparator());
            fileWriter.close();
        }
    }

    private String escapeXmlString(String s) {
        return s
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    /**
     * Determine if an external zookeeper should be used by MAC or not.
     *
     * @return true if an external zookeeper should be used, false otherwise.
     * @since 1.10.4
     */
    public boolean useExistingZookeeper() {
        if (forceExternalZookeeper == null) {
            return !(zooKeeperHost.equals("localhost") || zooKeeperHost.equals("127.0.0.1"));
        } else {
            return forceExternalZookeeper;
        }
    }

    /**
     * Returns the JVM system properties for the accumulo manager process.
     *
     * @return the JVM system properties for the accumulo manager process.
     * @since 1.10.4
     */
    public Map<String, String> getAccumuloManagerJvmProperties() {
        return jvmProperties.get("manager");
    }

    /**
     * Returns the JVM system properties for the accumulo T-server process.
     *
     * @return the JVM system properties for the accumulo T-server process.
     * @since 1.10.4
     */
    public Map<String, String> getAccumuloTserverJvmProperties() {
        return jvmProperties.get("tserver");
    }

    /**
     * Returns the number of T-servers to spawn.
     *
     * @return the number of T-servers to spawn.
     * @since 1.10.4
     */
    public int getNumTservers() {
        return numTservers;
    }

    /**
     * Returns the JVM system properties for the accumulo init process.
     *
     * @return the JVM system properties for the accumulo init process.
     * @since 1.10.4
     */
    public Map<String, String> getAccumuloInitJvmProperties() {
        return jvmProperties.get("init");
    }

    /**
     * Builds the {@link MACConfig} object for initializing {@link MAC}
     *
     * The default should work out of the box for integration testing, but
     * see the various methods for possible configuration options.
     *
     * @author loganasherjones
     * @since 1.10.4
     */
    public static class MACConfigBuilder {

        /**
         * Create an instance with "sensible" defaults for integration testing.
         */
        public MACConfigBuilder() {}

        private String macId = UUID.randomUUID().toString();
        private String instanceName = "default";
        private String rootPassword = "notsecure";
        private ClasspathLoader classpathLoader = null;
        private File baseDirectory = null;
        private boolean logToFile = false;
        private String zooKeeperHost = "127.0.0.1";
        private int zooKeeperPort = -1;
        private Boolean useExternalZookeeper = null;
        private int zooKeeperStartupTimeout = 10000;
        private String accumuloBindAddress = null;
        private final Map<String, String> accumuloGCJvmProperties = new HashMap<>();
        private final Map<String, String> accumuloManagerJvmProperties = new HashMap<>();
        private final Map<String, String> accumuloTserverJvmProperties = new HashMap<>();
        private final Map<String, String> accumuloInitJvmProperties = new HashMap<>();

        private final Map<String, String> zookeeperJvmProperties = initialZookeeperJvmProperties();
        private static Map<String, String> initialZookeeperJvmProperties() {
            Map<String, String> result = new HashMap<>();
            result.put("zookeeper.jmx.log4j.disable", "true");
            return result;
        }

        private final Map<String, String> siteXml = initialSiteXml();
        private static Map<String, String> initialSiteXml() {
            Map<String, String> result = new HashMap<>();
            result.put("tserver.memory.maps.native.enabled", "false");
            result.put("instance.secret", "alsonotsecure");
            return result;

        }
        private final Map<String, String> zooCfg = initialZooCfg();
        private static Map<String, String> initialZooCfg() {
            Map<String, String> result = new HashMap<>();
            result.put("tickTime", "2000");
            result.put("maxClientCnxns", "1000");
            result.put("4lw.commands.whitelist", "srvr,ruok");
            return result;
        };

        private int numTservers = 2;

        /**
         * Sets the instance name the accumulo cluster will use.
         *
         * @param instanceName - The accumulo instance name
         * @return this
         * @since 1.10.4
         */
        public MACConfigBuilder withInstanceName(String instanceName) {
            this.instanceName = instanceName;
            return this;
        }

        /**
         * Set the initial root password for the accumulo 'root' user.
         *
         * @param rootPassword - The 'root' accumulo password.
         * @return this
         * @since 1.10.4
         */
        public MACConfigBuilder withRootPassword(String rootPassword) {
            this.rootPassword = rootPassword;
            return this;
        }

        /**
         * Sets a custom {@link ClasspathLoader} to change the classpath of
         * the subprocesses spawned by {@link MAC}.
         *
         * @param loader - The {@link ClasspathLoader} to use
         * @return this
         * @since 1.10.4
         */
        public MACConfigBuilder withClasspathLoader(ClasspathLoader loader) {
            this.classpathLoader = loader;
            return this;
        }

        /**
         * Sets the base directory where configuration, logging and data goes.
         * If the directory you specify has:
         * <ul>
         * <li><pre>conf/zoo.cfg</pre></li>
         * <li><pre>conf/accumulo-site.xml</pre></li>
         * </ul>
         * The files in these directories will take precedent over anything
         * set in {@link #withAccumuloSiteProperty(Property, String)} and
         * {@link #withZookeeperProperty(String, String)}
         *
         * @param file - The base directory to use.
         * @return this
         * @since 1.10.4
         */
        public MACConfigBuilder withBaseDirectory(File file) {
            this.baseDirectory = file;
            return this;
        }

        /**
         * Each MAC gets its own ID for uniqueness’s sake. If you don't want
         * that, you can specify an ID here. This will affect process IDs and
         * the log file name (if you're using {@link #withFileLogging()}
         *
         * @param id - The MAC ID to use.
         * @return this
         * @since 1.10.4
         */
        public MACConfigBuilder withId(String id) {
            this.macId = id;
            return this;
        }

        /**
         * By default, the logging for all subprocesses will be streamed to
         * stdout/stderr. If you prefer to redirect these logs to files
         * use this method. The logs will end up in {@link #baseDirectory}/logs
         * and be named prefixed mac-{id}.
         *
         * @return this
         * @since  1.10.4
         */
        public MACConfigBuilder withFileLogging() {
            this.logToFile = true;
            return this;
        }

        /**
         * Sets the total time to wait for zookeeper up before giving up.
         *
         * @param msTimeout - Milliseconds to wait for zookeeper to respond imok
         * @return this
         * @since  1.10.4
         */
        public MACConfigBuilder withZooKeeperStartupTimeoutMS(int msTimeout) {
            this.zooKeeperStartupTimeout = msTimeout;
            return this;
        }

        /**
         * Sets the zookeeper hostname accumulo should use. This is typically
         * used when you have an external zookeeper you want to manage separate
         * from MAC itself.
         *
         * @param hostname - The zookeeper hostname to use.
         * @return this
         * @since 1.10.4
         */
        public MACConfigBuilder withZooKeeperHostname(String hostname) {
            this.zooKeeperHost = hostname;
            return this;
        }

        /**
         * By default, the zookeeper port will pick a random open port. If you
         * want to use a static port, set it here.
         *
         * @param port - The port zookeeper should bind on.
         * @return this
         * @since 1.10.4
         */
        public MACConfigBuilder withStaticZooKeeperPort(int port) {
            this.zooKeeperPort = port;
            return this;
        }

        /**
         * Indicates if an external zookeeper is true or false.
         * <p>
         * Typically, you won't need to use this call. This is really only
         * useful in the following scenarios:
         * </p>
         * <ul>
         *     <li>You have an external zookeeper running on localhost or
         *     127.0.0.1</li>
         *     <li>You want the zookeeper spawned by MAC to run on a specific
         *     IP that is not 127.0.0.1</li>
         * </ul>
         * <p>
         *     By default, MAC will assume if the {@link #zooKeeperHost} is
         *     either localhost or 127.0.0.1, then you want MAC to spawn a
         *     zookeeper.
         * </p>
         *
         * @return this
         * @param useExternal - indicates if an external zookeeper is used.
         * @since 1.10.4
         */
        public MACConfigBuilder withUseExternalZookeeper(boolean useExternal) {
            this.useExternalZookeeper = useExternal;
            return this;
        }

        /**
         * Sets the accumulo bind address. This is typically used when you
         * want to run inside a container. In these cases, you'll likely want
         * to set this to something like:
         * <pre>
         *     builder.withAccumuloBindAddress(InetAddress.getLocalHost().getHostAddress());
         * </pre>
         * This will use the current IP instead of the docker hostname.
         *
         * @param address - The hostname you would like the T-servers to
         *                advertise themselves as
         * @return this
         * @since 1.10.4
         */
        public MACConfigBuilder withAccumuloBindAddress(String address) {
            this.accumuloBindAddress = address;
            return this;
        }

        /**
         * Sets Java properties for the spawned zookeeper process.
         * <p>
         * If you want to change zookeeper configuration, you should likely be
         * using {@link #withZookeeperProperty(String, String)} which affects
         * the zoo.cfg file. This is useful for doing things like setting the
         * max memory or other java settings.
         * </p>
         *
         * @param key - A system property (without the -D)
         * @param value - The value of the system property
         * @return this
         * @since 1.10.4
         */
        public MACConfigBuilder withZookeeperJavaProperty(String key, String value) {
            zookeeperJvmProperties.put(key, value);
            return this;
        }

        /**
         * Sets Java properties for spawned Accumulo GC process.
         * <p>
         * If you want to change accumulo configuration, you should likely be
         * using {@link #withAccumuloSiteProperty(Property, String)} which
         * affects the accumulo-site.xml. This is useful for doing things like
         * setting the max memory, or GC settings.
         * </p>
         * @param key - A system property (without the -D)
         * @param value - The value of the system property
         * @return this
         * @since 1.10.4
         */
        public MACConfigBuilder withAccumuloGCJavaProperty(String key, String value) {
            accumuloGCJvmProperties.put(key, value);
            return this;
        }

        /**
         * Sets Java properties for spawned Accumulo Manager process.
         * <p>
         * If you want to change accumulo configuration, you should likely be
         * using {@link #withAccumuloSiteProperty(Property, String)} which
         * affects the accumulo-site.xml. This is useful for doing things like
         * setting the max memory, or GC settings.
         * </p>
         * @param key - A system property (without the -D)
         * @param value - The value of the system property
         * @return this
         * @since 1.10.4
         */
        public MACConfigBuilder withAccumuloManagerJavaProperty(String key, String value) {
            accumuloManagerJvmProperties.put(key, value);
            return this;
        }

        /**
         * Sets Java properties for spawned Accumulo T-Server process.
         * <p>
         * If you want to change accumulo configuration, you should likely be
         * using {@link #withAccumuloSiteProperty(Property, String)} which
         * affects the accumulo-site.xml. This is useful for doing things like
         * setting the max memory, or GC settings.
         * </p>
         * @param key - A system property (without the -D)
         * @param value - The value of the system property
         * @return this
         * @since 1.10.4
         */
        public MACConfigBuilder withAccumuloTServerJavaProperty(String key, String value) {
            accumuloTserverJvmProperties.put(key, value);
            return this;
        }

        /**
         * Sets Java properties for spawned Accumulo Init process.
         * <p>
         * If you want to change accumulo configuration, you should likely be
         * using {@link #withAccumuloSiteProperty(Property, String)} which
         * affects the accumulo-site.xml. This is useful for doing things like
         * setting the max memory, or GC settings.
         * </p>
         * @param key - A system property (without the -D)
         * @param value - The value of the system property
         * @return this
         * @since 1.10.4
         */
        public MACConfigBuilder withAccumuloInitJavaProperty(String key, String value) {
            accumuloInitJvmProperties.put(key, value);
            return this;
        }

        /**
         * Sets the number of tablet servers to spawn.
         *
         * @param num - The number of tablet servers to spawn.
         * @return this
         * @since 1.10.4
         */
        public MACConfigBuilder withNumTservers(int num) {
            this.numTservers = num;
            return this;
        }

        /**
         * Modify the accumulo-site.xml with the associated properties.
         * Note that if {@link #withBaseDirectory(File)} is used and a
         * conf/accumulo-site.xml exists at that base directory, settings set
         * here will be ignored.
         *
         * @param property - The property to set
         * @param value - The value that property should be set to
         * @return this
         * @since 1.10.4
         */
        public MACConfigBuilder withAccumuloSiteProperty(Property property, String value) {
            siteXml.put(property.getKey(), value);
            return this;
        }

        /**
         * Modify the zoo.cfg with the property passed in.
         * Note that if {@link #withBaseDirectory(File)} is used and a
         * conf/zoo.cfg exists at that base directory, settings set here will
         * be ignored.
         *
         * @param key - The zoo.cfg property to set
         * @param value - The value of the above property.
         * @return this
         * @since 1.10.4
         */
        public MACConfigBuilder withZookeeperProperty(String key, String value) {
            zooCfg.put(key, value);
            return this;
        }

        /**
         * Sets Java properties for spawned all spawned processes.
         *
         * @param key - A system property (without the -D)
         * @param value - The value of the system property
         * @return this
         * @since 1.10.4
         */
        public MACConfigBuilder withGlobalJavaProperty(String key, String value) {
            withAccumuloGCJavaProperty(key, value);
            withAccumuloManagerJavaProperty(key, value);
            withAccumuloTServerJavaProperty(key, value);
            withAccumuloInitJavaProperty(key, value);
            return withZookeeperJavaProperty(key, value);
        }

        /**
         * Build the specified config.
         *
         * @return a {@link MACConfig} used to initialize {@link MAC}
         * @since 1.10.4
         */
        public MACConfig build() {
            if (this.baseDirectory == null) {
                this.baseDirectory = new File(System.getProperty("java.io.tmpdir"), "mac-" + this.macId);
            }

            File confDir = new File(this.baseDirectory, "conf");
            File libDir = new File(this.baseDirectory, "lib");
            File libExtDir = new File(libDir, "ext");

            if (this.classpathLoader == null) {
                this.classpathLoader = new DefaultClasspathLoader(Collections.singletonList(confDir.getAbsolutePath()));
            }

            File accumuloData = new File(this.baseDirectory, "accumulo-data");

            setPropertyIfNotSet("instance.volumes", accumuloData.toURI().toString());
            setPropertyIfNotSet("general.classpaths", libDir.getAbsolutePath() + "/[^.].*[.]jar");
            setPropertyIfNotSet("general.dynamic.classpaths", libExtDir.getAbsolutePath() + "/[^.].*[.]jar");
            setPropertyIfNotSet("instance.zookeeper.host", zooKeeperHost + ":" + zooKeeperPort);

            if (numTservers <= 0) {
                throw new IllegalArgumentException("numTservers must be greater than 0");
            }

            zooCfg.put("dataDir", confDir.getAbsolutePath());

            return new MACConfig(
                    this.instanceName,
                    this.rootPassword,
                    this.classpathLoader,
                    this.macId,
                    this.logToFile,
                    this.zooKeeperStartupTimeout,
                    baseDirectory,
                    this.zooKeeperHost,
                    this.zooKeeperPort,
                    siteXml,
                    accumuloBindAddress,
                    zookeeperJvmProperties,
                    accumuloGCJvmProperties,
                    accumuloManagerJvmProperties,
                    accumuloTserverJvmProperties,
                    accumuloInitJvmProperties,
                    numTservers,
                    zooCfg,
                    useExternalZookeeper
            );
        }

        private void setPropertyIfNotSet(String key, String value) {
            if (!siteXml.containsKey(key)) {
                siteXml.put(key, value);
            }
        }
    }

}
