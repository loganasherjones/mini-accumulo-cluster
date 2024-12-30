package com.loganasherjones.mac;

import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.ZooKeeperInstance;
import org.apache.accumulo.core.client.security.tokens.PasswordToken;

public class Main {

    public static void main(String[] args) throws Exception {
        ZooKeeperInstance instance = new ZooKeeperInstance("default", "127.0.0.1:21811");
        Connector conn = instance.getConnector("root", new PasswordToken("notsecure"));
        TestClient test = new TestClient(instance, conn);
        test.runTest();
    }
}