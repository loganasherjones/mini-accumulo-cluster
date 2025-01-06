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
 * Configuration for the {@link MAC}.
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
            Map<String, String> zooCfg
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
    }

    public String getInstanceName() {
        return instanceName;
    }

    public String getRootPassword() {
        return rootPassword;
    }

    public ClasspathLoader getClasspathLoader() {
        return classpathLoader;
    }

    public String getMACId() {
        return this.id;
    }

    public boolean logToFiles() {
        return this.logToFile;
    }

    public int getZooKeeperStartupTimeout() {
        return this.zooKeeperStartupTimeout;
    }

    public File getZooCfgFile() {
        return new File(this.configDirectory, "zoo.cfg");
    }

    public File getLogDir() {
        return this.logDirectory;
    }

    public String getZooKeeperHost() {
        return this.zooKeeperHost;
    }

    public String getAccumuloBindAddress() {
        return this.accumuloBindAddress;
    }

    public Map<String, String> getSiteConfig() {
        return this.siteConfig;
    }

    public File getBaseDirectory() { return this.baseDirectory; }

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

    public Map<String, String> getZooKeeperJvmProperties() {
        return jvmProperties.get("zookeeper");
    }

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

    public boolean useExistingZookeeper() {
        return !(zooKeeperHost.equals("localhost") || zooKeeperHost.equals("127.0.0.1"));
    }

    public Map<String, String> getAccumuloManagerJvmProperties() {
        return jvmProperties.get("manager");
    }

    public Map<String, String> getAccumuloTserverJvmProperties() {
        return jvmProperties.get("tserver");
    }

    public int getNumTservers() {
        return numTservers;
    }

    public Map<String, String> getAccumuloInitJvmProperties() {
        return jvmProperties.get("init");
    }

    public static class MACConfigBuilder {

        private String macId = UUID.randomUUID().toString();
        private String instanceName = "default";
        private String rootPassword = "notsecure";
        private ClasspathLoader classpathLoader = null;
        private File baseDir = null;
        private boolean logToFile = false;
        private String zooKeeperHost = "127.0.0.1";
        private int zooKeeperPort = -1;
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

        public MACConfigBuilder withInstanceName(String s) {
            this.instanceName = s;
            return this;
        }

        public MACConfigBuilder withRootPassword(String s) {
            this.rootPassword = s;
            return this;
        }

        public MACConfigBuilder withClasspathLoader(ClasspathLoader cpl) {
            this.classpathLoader = cpl;
            return this;
        }

        public MACConfigBuilder withBaseDirectory(File file) {
            this.baseDir = file;
            return this;
        }

        public MACConfigBuilder withId(String id) {
            this.macId = id;
            return this;
        }

        public MACConfigBuilder withFileLogging() {
            this.logToFile = true;
            return this;
        }

        public MACConfigBuilder withZooKeeperStartupTimeoutMS(int timeout) {
            this.zooKeeperStartupTimeout = timeout;
            return this;
        }

        public MACConfigBuilder withZooKeeperHostname(String hostname) {
            this.zooKeeperHost = hostname;
            return this;
        }

        public MACConfigBuilder withStaticZooKeeperPort(int port) {
            this.zooKeeperPort = port;
            return this;
        }

        public MACConfigBuilder withAccumuloBindAddress(String address) {
            this.accumuloBindAddress = address;
            return this;
        }

        public MACConfigBuilder withZookeeperJavaProperty(String key, String value) {
            zookeeperJvmProperties.put(key, value);
            return this;
        }

        public MACConfigBuilder withAccumuloGCJavaProperty(String key, String value) {
            accumuloGCJvmProperties.put(key, value);
            return this;
        }

        public MACConfigBuilder withAccumuloManagerJavaProperty(String key, String value) {
            accumuloManagerJvmProperties.put(key, value);
            return this;
        }

        public MACConfigBuilder withAccumuloTServerJavaProperty(String key, String value) {
            accumuloTserverJvmProperties.put(key, value);
            return this;
        }

        public MACConfigBuilder withAccumuloInitJavaProperty(String key, String value) {
            accumuloInitJvmProperties.put(key, value);
            return this;
        }

        public MACConfigBuilder withNumTservers(int num) {
            this.numTservers = num;
            return this;
        }

        public MACConfigBuilder withAccumuloSiteProperty(Property property, String value) {
            siteXml.put(property.getKey(), value);
            return this;
        }

        public MACConfigBuilder withZookeeperProperty(String key, String value) {
            zooCfg.put(key, value);
            return this;
        }

        public MACConfigBuilder withGlobalJavaProperty(String key, String value) {
            withAccumuloGCJavaProperty(key, value);
            withAccumuloManagerJavaProperty(key, value);
            withAccumuloTServerJavaProperty(key, value);
            withAccumuloInitJavaProperty(key, value);
            return withZookeeperJavaProperty(key, value);
        }

        public MACConfig build() {
            if (this.baseDir == null) {
                this.baseDir = new File(System.getProperty("java.io.tmpdir"), "mac-" + this.macId);
            }

            File confDir = new File(this.baseDir, "conf");
            File libDir = new File(this.baseDir, "lib");
            File libExtDir = new File(libDir, "ext");

            if (this.classpathLoader == null) {
                this.classpathLoader = new DefaultClasspathLoader(Collections.singletonList(confDir.getAbsolutePath()));
            }

            File accumuloData = new File(this.baseDir, "accumulo-data");

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
                    baseDir,
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
                    zooCfg
            );
        }

        private void setPropertyIfNotSet(String key, String value) {
            if (!siteXml.containsKey(key)) {
                siteXml.put(key, value);
            }
        }
    }

}
