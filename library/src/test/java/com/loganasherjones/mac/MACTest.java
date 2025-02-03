package com.loganasherjones.mac;

import org.junit.jupiter.api.Test;

public class MACTest {
    @Test
    public void testStart() throws Exception {
        MACConfig config = new MACConfig.MACConfigBuilder().build();
        MAC cluster = new MAC(config);
        try {
            cluster.start();
            TestClient client = new TestClient(
                    cluster.getInstanceName(),
                    config.getZooKeeperHost(),
                    config.getZooKeeperPort(),
                    config.getRootPassword(),
                    false
            );
            client.runTest();
        } finally {
            cluster.stop();
        }
    }
}
