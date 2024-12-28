package com.loganasherjones.mac;

import org.apache.accumulo.cluster.AccumuloCluster;
import org.junit.jupiter.api.Test;

public class MACTest {
    @Test
    public void testStart() throws Exception {
        AccumuloCluster cluster = new MAC();
        try {
            cluster.start();
        } finally {
            cluster.stop();
        }
    }
}
