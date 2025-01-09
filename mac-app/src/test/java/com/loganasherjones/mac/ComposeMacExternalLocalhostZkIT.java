package com.loganasherjones.mac;

import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.Instance;
import org.apache.accumulo.core.client.ZooKeeperInstance;
import org.apache.accumulo.core.client.security.tokens.PasswordToken;
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
            Instance instance = TestHelper.getInstance(config.getInstanceName(), config.getZooKeeperHost(), config.getZooKeeperPort());
            Connector connector = TestHelper.getRootConnector(instance);
            TestClient client = new TestClient(instance, connector, true);
            client.runTest();
        } finally {
            cluster.stop();
        }
    }

}
