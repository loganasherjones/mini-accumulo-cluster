package com.loganasherjones.mac;

import org.junit.jupiter.api.Test;

public class ComposeMacExternalZkIT {
    @Test
    public void testDefaultMacInDockerCompose() throws Exception {
        TestClient client = new TestClient(
                "default",
                "127.0.0.1",
                2181,
                "notsecure",
                false
        );
        client.runTest();
    }

}
