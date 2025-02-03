package com.loganasherjones.mac;

import org.junit.jupiter.api.Test;


public class DefaultIT {
    @Test
    public void testDefaultContainer() throws Exception {
        TestClient client = new TestClient(
                "default",
                "127.0.0.1",
                21811,
                "notsecure",
                false
        );

        client.runTest();
    }
}
