package com.loganasherjones.mac;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws IOException, InterruptedException {
        setupLogging();
        MACConfig config = generateConfig();
        MAC mac = new MAC(config);
        if (args.length > 0 && args[0].equals("shell")) {
            mac.runShell();
        } else {
            try {
                mac.start();
                // Wait forever.
                Thread.currentThread().join();
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    mac.stop();
                } catch (Exception e) {
                    System.out.println("Error stopping");
                }
            }
        }
    }

    private static void setupLogging() {
        System.setProperty("root_log_level", getRootLogLevel());

        String macLogLevel = System.getenv("MAC_LOG_LEVEL");
        if (macLogLevel == null || macLogLevel.isEmpty()) {
            macLogLevel = "INFO";
        }
        System.setProperty("mac_log_level", macLogLevel);
    }

    private static String getRootLogLevel() {
        String rootLogLevel = System.getenv("ROOT_LOG_LEVEL");
        if (rootLogLevel == null || rootLogLevel.isEmpty()) {
            rootLogLevel = "ERROR";
        }
        return rootLogLevel;
    }

    private static MACConfig generateConfig() throws UnknownHostException {
        MACConfig.MACConfigBuilder builder = new MACConfig.MACConfigBuilder();
        if (isInContainer()) {
            builder.withAccumuloBindAddress(InetAddress.getLocalHost().getHostAddress());
        }

        builder.withGlobalJavaProperty("root_log_level", getRootLogLevel());

        String baseDir = System.getenv("MAC_BASE_DIR");
        if (baseDir != null && !baseDir.isEmpty()) {
            builder.withBaseDirectory(new File(baseDir));
        }

        String zookeeperPortStr = System.getenv("MAC_ZOOKEEPER_PORT");
        int zookeeperPort = 21811;
        if (zookeeperPortStr != null && !zookeeperPortStr.isEmpty()) {
            zookeeperPort = Integer.parseInt(zookeeperPortStr);

        }

        String zookeeperHost = System.getenv("MAC_ZOOKEEPER_HOST");
        if (zookeeperHost != null && !zookeeperHost.isEmpty()) {
            builder.withZooKeeperHostname(zookeeperHost);
        }

        String fileLogging = System.getenv("MAC_FILE_LOGGING");
        if (fileLogging != null && fileLogging.equalsIgnoreCase("true")) {
            builder.withFileLogging();
        }

        return builder
                .withStaticZooKeeperPort(zookeeperPort)
                .build();
    }

    private static boolean isInContainer() {
        String inContainer = System.getenv("MAC_IN_DOCKER_CONTAINER");
        return inContainer != null && inContainer.equals("true");
    }

}