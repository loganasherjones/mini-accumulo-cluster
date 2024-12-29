package com.loganasherjones.mac;

import org.apache.accumulo.server.util.PortUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

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
            Map<String, String> siteConfig
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

    public Map<String, String> getSiteConfig() {
        return this.siteConfig;
    }

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

    public void createDirectoryStructure() throws IOException {
        if (!this.baseDirectory.exists()) {
            this.baseDirectory.mkdirs();
        }

        if (!this.baseDirectory.isDirectory()) {
            throw new IllegalArgumentException("Base Directory " + this.baseDirectory + " was not a directory.");
        }

        if (Objects.requireNonNull(this.baseDirectory.list()).length != 0) {
            throw new IllegalArgumentException("Base Directory " + this.baseDirectory + " was not empty.");
        }

        configDirectory.mkdirs();
        logDirectory.mkdirs();

        File zooCfgFile = new File(configDirectory, "zoo.cfg");
        FileWriter fw = new FileWriter(zooCfgFile);
        Properties zooCfg = new Properties();
        zooCfg.setProperty("clientPort", Integer.toString(getZooKeeperPort()));
        zooCfg.setProperty("dataDir", configDirectory.getAbsolutePath());
        zooCfg.setProperty("tickTime", "2000");
        zooCfg.setProperty("maxClientCnxns", "1000");
        zooCfg.store(fw, null);

        File siteXml = new File(configDirectory, "accumulo-site.xml");
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

    private String escapeXmlString(String s) {
        return s
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
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

        public MACConfig build() {
            if (this.baseDir == null) {
                this.baseDir = new File(System.getProperty("java.io.tmpdir"), "mac-" + this.macId);
            }

            File confDir = new File(this.baseDir, "conf");

            if (this.classpathLoader == null) {
                this.classpathLoader = new DefaultClasspathLoader(Collections.singletonList(confDir.getAbsolutePath()));
            }

            File accumuloData = new File(this.baseDir, "accumulo-data");
            // TODO: Add better support for ext dir.
            File extDir = new File(this.baseDir, "ext");

            Map<String, String> siteConfig = new HashMap<>();
            // TODO: Unclear if this is required.
//          siteConfig.put("trace.token.property.password", rootPassword);
            siteConfig.put("instance.dfs.dir", accumuloData.getAbsolutePath());

            // TODO: Add better
            siteConfig.put("general.dynamic.classpaths", extDir.getAbsolutePath());
//          siteConfig.put("tserver.memory.maps.max</name><value>50M</value></property>
//          siteConfig.put("gc.cycle.start</name><value>0s</value></property>
//          siteConfig.put("master.port.client</name><value>0</value></property>
//          siteConfig.put("tserver.cache.data.size</name><value>10M</value></property>
//          siteConfig.put("monitor.port.client</name><value>0</value></property>
//          siteConfig.put("tserver.port.client</name><value>0</value></property>
//          siteConfig.put("tserver.cache.index.size</name><value>10M</value></property>
//          siteConfig.put("gc.cycle.delay</name><value>4s</value></property>
//          siteConfig.put("monitor.port.log4j</name><value>0</value></property>
//          siteConfig.put("tserver.port.search</name><value>true</value></property>
//          siteConfig.put("replication.receipt.service.port</name><value>0</value></property>
            siteConfig.put("instance.dfs.uri", "file:///");
//          siteConfig.put("tserver.memory.maps.native.enabled</name><value>false</value></property>
//          siteConfig.put("trace.port.client</name><value>0</value></property>
//          siteConfig.put("tserver.compaction.major.delay</name><value>3</value></property>
//          siteConfig.put("tserver.walog.max.size</name><value>100M</value></property>
            siteConfig.put("instance.secret", "DONTTELL");
//          siteConfig.put("gc.port.client</name><value>0</value></property>
            siteConfig.put("instance.zookeeper.host", zooKeeperHost + ":" + zooKeeperPort);
//          siteConfig.put("master.replication.coordinator.port</name><value>0</value></property>
//          siteConfig.put("general.classpaths</name><value>/tmp/foo/lib/[^.].*[.]jar</value></property>

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
                    siteConfig
            );
        }
    }
}
