package com.loganasherjones.mac;

import org.apache.accumulo.core.client.*;
import org.apache.accumulo.core.client.admin.NewTableConfiguration;
import org.apache.accumulo.core.client.security.SecurityErrorCode;
import org.apache.accumulo.core.client.security.tokens.PasswordToken;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Mutation;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.LongCombiner;
import org.apache.accumulo.core.iterators.user.SummingCombiner;
import org.apache.accumulo.core.security.Authorizations;
import org.apache.accumulo.core.security.ColumnVisibility;
import org.apache.accumulo.core.security.TablePermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class TestClient {
    private static final Logger log = LoggerFactory.getLogger(TestClient.class);

    private final String table = "table1";
    private final String user1 = "user1";
    private final String user1Password = "password1";
    private final ColumnVisibility aOrB = new ColumnVisibility("A|B");
    private final ColumnVisibility aAndB = new ColumnVisibility("A&B");
    private final String instanceName;
    private final String zookeeperHost;
    private final String rootPassword;
    private final int zookeeperPort;
    private final boolean skipIteratorTest;
    private final AccumuloClient rootClient;

    public TestClient(
            String instanceName,
            String zookeeperHost,
            int zookeeperPort,
            String rootPassword,
            boolean skipIteratorTest
    ) {
        this.instanceName = instanceName;
        this.zookeeperHost = zookeeperHost;
        this.zookeeperPort = zookeeperPort;
        this.rootPassword = rootPassword;
        this.rootClient = createRootClient();
        this.skipIteratorTest = skipIteratorTest;
    }

    public void runTest() throws Exception {
        cleanup();
        AccumuloClient userClient = null;
        try {
            setupTablesUsersAndPermissions(rootClient);
            userClient = createClientWithRetry(zookeeperHost, zookeeperPort, user1, user1Password);
            insertData(userClient);
            checkScans(userClient);
            log.info("Test successful.");
        } finally {
            if (userClient != null) {
                userClient.close();
            }
            cleanup();
        }
    }

    private void cleanup() throws Exception {
        try {
            rootClient.tableOperations().delete(table);
        } catch (TableNotFoundException e) {
            // No worries.
        }

        try {
            rootClient.securityOperations().dropLocalUser(user1);
        } catch (AccumuloSecurityException e) {
            if (e.getSecurityErrorCode() != SecurityErrorCode.USER_DOESNT_EXIST) {
                throw e;
            }
        }

    }

    private void setupTablesUsersAndPermissions(AccumuloClient client) throws Exception {
        log.info("Creating table: {}", table);
        client.tableOperations().create(table, new NewTableConfiguration());

        log.info("Creating user: {}", user1);
        client.securityOperations().createLocalUser(user1, new PasswordToken(user1Password));
        client.securityOperations().changeUserAuthorizations(user1, new Authorizations("A", "B"));
        client.securityOperations().grantTablePermission(user1, table, TablePermission.READ);
        client.securityOperations().grantTablePermission(user1, table, TablePermission.WRITE);

        log.info("Attaching iterator to: {}", table);
        IteratorSetting is = new IteratorSetting(10, SummingCombiner.class);
        SummingCombiner.setEncodingType(is, LongCombiner.Type.STRING);
        IteratorSetting.Column countColumn = new IteratorSetting.Column("META", "COUNT");
        SummingCombiner.setColumns(is, Collections.singletonList(countColumn));
        client.tableOperations().attachIterator(table, is);
    }

    private void insertData(AccumuloClient client) throws Exception {
        log.info("Inserting data");
        BatchWriter bw = client.createBatchWriter(table, new BatchWriterConfig());
        UUID uuid = UUID.randomUUID();

        Mutation m = new Mutation(uuid.toString());
        m.put("META", "SIZE", aOrB, "8");
        m.put("META", "CRC", aOrB, "456");
        m.put("META", "COUNT", aOrB, "1");
        m.put("META", "IMG", aAndB, "ABCDEFGH");

        bw.addMutation(m);
        bw.flush();

        m = new Mutation(uuid.toString());
        m.put("META", "COUNT", aOrB, "1");
        m.put("META", "CRC", aOrB, "123");
        bw.addMutation(m);
        bw.flush();

        bw.close();
    }

    private void checkScans(AccumuloClient client) throws Exception {
        log.info("Ensuring scans work as expected.");
        int count = 0;
        Scanner scanner = client.createScanner(table, new Authorizations("A"));
        for (Map.Entry<Key, Value> entry : scanner) {
            if (entry.getKey().getColumnQualifierData().toString().equals("COUNT")) {
                assertEquals("2", entry.getValue().toString());
            } else if (entry.getKey().getColumnQualifierData().toString().equals("SIZE")) {
                assertEquals("8", entry.getValue().toString());
            } else if (entry.getKey().getColumnQualifierData().toString().equals("CRC")) {
                assertEquals("123", entry.getValue().toString());
            } else {
                fail("Got an unexpected key " + entry.getKey());
            }
            count++;
        }
        scanner.close();

        assertEquals(3, count);

        count = 0;
        scanner = client.createScanner(table, new Authorizations("A", "B"));
        for (Map.Entry<Key, Value> entry : scanner) {
            if (entry.getKey().getColumnQualifierData().toString().equals("IMG")) {
                assertEquals("ABCDEFGH", entry.getValue().toString());
            }
            count++;
        }

        assertEquals(4, count);
        scanner.close();

        // Iterator Testing
        if (!skipIteratorTest) {
            try {
                customIteratorScan(client);
            } catch (Exception e) {
                fail("Error occurred during iterator-base scanning: " + e.getMessage());
            }
        }
    }

    private void customIteratorScan(AccumuloClient client) throws Exception {
        log.info("Testing custom iterator scanning.");
        Scanner scanner = client.createScanner(table, new Authorizations("A", "B"));
        IteratorSetting exampleIteratorSetting = new IteratorSetting(11, "example", "com.loganasherjones.mac.ExampleIterator");
        scanner.addScanIterator(exampleIteratorSetting);
        int count = 0;
        for (Map.Entry<Key, Value> ignored: scanner) {
            count++;
        }
        assertEquals(0, count);
        scanner.close();
    }

    private AccumuloClient createRootClient() {
        try {
            return createClientWithRetry(
                    zookeeperHost,
                    zookeeperPort,
                    "root",
                    rootPassword
                );
        } catch (InterruptedException e) {
            fail("Interrupted trying to get Accumulo client");
        } catch (RuntimeException e) {
            fail("Could not successfully connect to Accumulo");
        }
        throw new RuntimeException("Unhandled case failing to get Accumulo Client.");
    }

    private AccumuloClient createClientWithRetry(
            String zookeeperHost,
            int zookeeperPort,
            String username,
            String password
    ) throws InterruptedException {
        String zookeeperConnect = zookeeperHost + ":" + zookeeperPort;
        AccumuloClient client = null;
        long startTime = System.currentTimeMillis();

        RuntimeException lastException = new RuntimeException("You shouldn't see this.");
        while (client == null && System.currentTimeMillis() - startTime < 60_000) {
            try {
                client = Accumulo
                        .newClient()
                        .to(instanceName, zookeeperConnect)
                        .as(username, new PasswordToken(password))
                        .build();
            } catch (RuntimeException e) {
                log.debug("Could not connect to zookeeper, sleeping for one second", e);
                Thread.sleep(1000);
                lastException = e;
            }
        }
        if (client == null) {
            throw lastException;
        }
        return client;
    }
}