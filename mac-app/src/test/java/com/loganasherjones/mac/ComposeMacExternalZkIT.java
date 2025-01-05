package com.loganasherjones.mac;

import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.Instance;
import org.junit.jupiter.api.Test;

public class ComposeMacExternalZkIT {
    @Test
    public void testDefaultMacInDockerCompose() throws Exception {
        Instance instance = TestHelper.getInstance("default", "127.0.0.1", 2181);
        Connector rootConnector = TestHelper.getRootConnector(instance);
        TestClient client = new TestClient(instance, rootConnector);
        client.runTest();
    }

}
