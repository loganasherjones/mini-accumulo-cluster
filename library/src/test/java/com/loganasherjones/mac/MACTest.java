package com.loganasherjones.mac;

import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.Scanner;
import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.security.Authorizations;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Map;

public class MACTest {
    @Test
    public void testStart() throws Exception {
//        MiniAccumuloCluster cluster = new MiniAccumuloCluster(new File("/tmp/foo"), "supersecret");
        MACConfig config = new MACConfig.MACConfigBuilder().build();
        MAC cluster = new MAC(config);
        try {
            cluster.start();
            System.out.println("Creating connector");
            Connector connector = cluster.getConnector("root", new PasswordToken(config.getRootPassword()));
            System.out.println("Scanning metadata...");
            try (Scanner scanner = connector.createScanner("accumulo.metadata", Authorizations.EMPTY)) {
                for (Map.Entry<Key, Value> entry : scanner) {
                    System.out.println(entry.getKey().toString() + " = " + entry.getValue().toString());
                }
            }

        } finally {
            cluster.stop();
        }
    }
}
