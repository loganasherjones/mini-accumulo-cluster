package com.loganasherjones.mac;

import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.ZooKeeperInstance;
import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.fail;

public class DefaultIT {
    private static final Logger log = LoggerFactory.getLogger(DefaultIT.class);
    private final int zookeeperPort = 21811;
    private final String zookeeperHost = "127.0.0.1";
    private final String instanceName = "default";

    @Test
    public void testDefaultContainer() throws Exception {
        Instance instance = null;
        try {
            instance = getInstance();
        } catch (InterruptedException e) {
            fail("Interrupted trying to get zookeeper connection");
        } catch (RuntimeException e) {
            fail("Could not successfully connect to zookeeper");
        }

        Connector rootConnector = null;
        try {
            rootConnector = instance.getConnector("root", new PasswordToken("notsecure"));
        } catch (AccumuloException | AccumuloSecurityException e) {
            log.error("Could not get connector for default mac", e);
            fail("Could not get connector for default mac");
        }

        TestClient client = new TestClient(instance, rootConnector);
        client.runTest();
    }

    private Instance getInstance() throws InterruptedException {
        String zookeeperConnect = zookeeperHost + ":" + zookeeperPort;
        Instance instance = null;
        long startTime = System.currentTimeMillis();

        RuntimeException lastException = new RuntimeException("You shouldn't see this.");
        while (instance == null && System.currentTimeMillis() - startTime < 60_000) {
            try {
                instance = new ZooKeeperInstance(instanceName, zookeeperConnect);
            } catch (RuntimeException e) {
                log.debug("Could not connect to zookeeper, sleeping for one second", e);
                Thread.sleep(1000);
                lastException = e;
            }
        }
        if (instance == null) {
            throw lastException;
        }
        return instance;
    }
}
