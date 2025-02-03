package com.loganasherjones.mac;

import org.junit.jupiter.api.Test;

public class ComposeMac1IT {
    @Test
    public void testDefaultMacInDockerCompose() throws Exception {
        TestClient client = new TestClient(
                "default",
                "127.0.0.1",
                21812,
                "notsecure",
                false
        );
        client.runTest();
    }

}
