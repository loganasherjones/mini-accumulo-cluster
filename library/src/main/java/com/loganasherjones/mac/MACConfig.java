package com.loganasherjones.mac;

import org.apache.accumulo.server.util.PortUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;

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

    private MACConfig(
            String instanceName,
            String rootPassword,
            ClasspathLoader classpathLoader,
            String id,
            boolean logToFile,
            int zooKeeperStartupTimeout,
            File baseDirectory,
            String zooKeeperHost,
            int zooKeeperPort
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

    public int getZooKeeperPort() {
        if (this.zooKeeperPort != -1) {
            return this.zooKeeperPort;
        }

        synchronized (this) {
            if (this.zooKeeperPort == -1) {
                this.zooKeeperPort = PortUtils.getRandomFreePort();
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

            if (this.classpathLoader == null) {
                this.classpathLoader = new DefaultClasspathLoader(Collections.emptyList());
            }

            return new MACConfig(
                    this.instanceName,
                    this.rootPassword,
                    this.classpathLoader,
                    this.macId,
                    this.logToFile,
                    this.zooKeeperStartupTimeout,
                    baseDir,
                    this.zooKeeperHost,
                    this.zooKeeperPort
            );
        }
    }
}
