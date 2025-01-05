package com.loganasherjones.mac;

import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.Instance;
import org.junit.jupiter.api.Test;


public class DefaultIT {
    @Test
    public void testDefaultContainer() throws Exception {
        Instance instance = TestHelper.getInstance("default", "127.0.0.1", 21811);
        Connector rootConnector = TestHelper.getRootConnector(instance);
        TestClient client = new TestClient(instance, rootConnector);
        client.runTest();
    }
}
