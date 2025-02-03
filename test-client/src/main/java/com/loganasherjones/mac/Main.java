package com.loganasherjones.mac;

public class Main {

    public static void main(String[] args) throws Exception {
        TestClient test = new TestClient(
                "default",
                "127.0.0.1",
                21811,
                "notsecure",
                false
        );
        test.runTest();
    }
}