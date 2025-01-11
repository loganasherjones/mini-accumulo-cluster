package com.loganasherjones.mac;

import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.ZooKeeperInstance;
import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.junit.jupiter.api.Test;

public class MACTest {
    @Test
    public void testStart() throws Exception {
        MACConfig config = new MACConfig.MACConfigBuilder().build();
        MAC cluster = new MAC(config);
        try {
            cluster.start();
            Instance instance = new ZooKeeperInstance(cluster.getInstanceName(), cluster.getZooKeepers());
            Connector connector = cluster.getConnector("root", new PasswordToken(config.getRootPassword()));
            TestClient client = new TestClient(instance, connector);
            client.runTest();
        } finally {
            cluster.stop();
        }
    }
}
