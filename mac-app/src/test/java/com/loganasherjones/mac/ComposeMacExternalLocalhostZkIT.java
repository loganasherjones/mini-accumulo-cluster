package com.loganasherjones.mac;

import org.junit.jupiter.api.Test;

public class ComposeMacExternalLocalhostZkIT {
    @Test
    public void testExternalZookeeperOnLocalhost() throws Exception {
        MACConfig.MACConfigBuilder builder = new MACConfig.MACConfigBuilder();
        builder
                .withStaticZooKeeperPort(2183)
                .withZooKeeperHostname("localhost")
                .withUseExternalZookeeper(true)
                .build();

        MACConfig config = builder.build();
        MAC cluster = new MAC(config);
        try {
            cluster.start();
            TestClient client = new TestClient(
                    config.getInstanceName(),
                    config.getZooKeeperHost(),
                    config.getZooKeeperPort(),
                    config.getRootPassword(),
                    true
            );
            client.runTest();
        } finally {
            cluster.stop();
        }
    }

}
