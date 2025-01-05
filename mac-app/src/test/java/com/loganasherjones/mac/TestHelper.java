package com.loganasherjones.mac;

import org.apache.accumulo.core.client.AccumuloException;
import org.apache.accumulo.core.client.AccumuloSecurityException;
import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.ZooKeeperInstance;
import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.fail;

public class TestHelper {

    private static final Logger log = LoggerFactory.getLogger(TestHelper.class);

    public static Instance getInstance(String instanceName, String zookeeperHost, int zookeeperPort) {
        try {
            return getInstanceWithRetry(instanceName, zookeeperHost, zookeeperPort);
        } catch (InterruptedException e) {
            fail("Interrupted trying to get zookeeper connection");
        } catch (RuntimeException e) {
            fail("Could not successfully connect to zookeeper");
        }
        throw new RuntimeException("Unhandled case failing to get instance.");
    }

    public static Connector getRootConnector(Instance instance) {
        try {
            return instance.getConnector("root", new PasswordToken("notsecure"));
        } catch (AccumuloException | AccumuloSecurityException e) {
            log.error("Could not get connector for default mac", e);
            fail("Could not get connector for default mac");
        }
        throw new RuntimeException("Unhandled case failing to get rootConnector");
    }

    private static Instance getInstanceWithRetry(String instanceName, String zookeeperHost, int zookeeperPort) throws InterruptedException {
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
