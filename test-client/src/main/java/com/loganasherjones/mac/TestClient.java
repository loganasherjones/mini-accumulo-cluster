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
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestClient {
    private static final Logger log = LoggerFactory.getLogger(TestClient.class);

    private final String table = "table1";
    private final String user1 = "user1";
    private final String user1Password = "password1";
    private final ColumnVisibility aOrB = new ColumnVisibility("A|B");
    private final ColumnVisibility aAndB = new ColumnVisibility("A&B");
    private final Connector rootConnector;
    private final Instance instance;

    public TestClient(Instance instance, Connector rootConnector) {
        this.instance = instance;
        this.rootConnector = rootConnector;
    }


    public void runTest() throws Exception {
        cleanup();
        try {
            setupTablesUsersAndPermissions(rootConnector);
            Connector userConnector = instance.getConnector(user1, new PasswordToken(user1Password));
            insertData(userConnector);
            checkScans(userConnector);
            log.info("Test successful.");
        } finally {
            cleanup();
        }
    }

    private void cleanup() throws Exception {
        try {
            rootConnector.tableOperations().delete(table);
        } catch (TableNotFoundException e) {
            // No worries.
        }

        try {
            rootConnector.securityOperations().dropLocalUser(user1);
        } catch (AccumuloSecurityException e) {
            if (e.getSecurityErrorCode() != SecurityErrorCode.USER_DOESNT_EXIST) {
                throw e;
            }
        }

    }

    private void setupTablesUsersAndPermissions(Connector conn) throws Exception {
        log.info("Creating table: {}", table);
        conn.tableOperations().create(table, new NewTableConfiguration());

        log.info("Creating user: {}", user1);
        conn.securityOperations().createLocalUser(user1, new PasswordToken(user1Password));
        conn.securityOperations().changeUserAuthorizations(user1, new Authorizations("A", "B"));
        conn.securityOperations().grantTablePermission(user1, table, TablePermission.READ);
        conn.securityOperations().grantTablePermission(user1, table, TablePermission.WRITE);

        log.info("Attaching iterator to: {}", table);
        IteratorSetting is = new IteratorSetting(10, SummingCombiner.class);
        SummingCombiner.setEncodingType(is, LongCombiner.Type.STRING);
        IteratorSetting.Column countColumn = new IteratorSetting.Column("META", "COUNT");
        SummingCombiner.setColumns(is, Collections.singletonList(countColumn));
        conn.tableOperations().attachIterator(table, is);
    }

    private void insertData(Connector conn) throws Exception {
        log.info("Inserting data");
        BatchWriter bw = conn.createBatchWriter(table, new BatchWriterConfig());
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

    private void checkScans(Connector conn) throws Exception {
        log.info("Ensuring scans work as expected.");
        int count = 0;
        Scanner scanner = conn.createScanner(table, new Authorizations("A"));
        for (Map.Entry<Key, Value> entry : scanner) {
            if (entry.getKey().getColumnQualifierData().toString().equals("COUNT")) {
                assertEquals("2", entry.getValue().toString());
            } else if (entry.getKey().getColumnQualifierData().toString().equals("SIZE")) {
                assertEquals("8", entry.getValue().toString());
            } else if (entry.getKey().getColumnQualifierData().toString().equals("CRC")) {
                assertEquals("123", entry.getValue().toString());
            } else {
                assertTrue(false);
            }
            count++;
        }
        scanner.close();

        assertEquals(3, count);

        count = 0;
        scanner = conn.createScanner(table, new Authorizations("A", "B"));
        for (Map.Entry<Key, Value> entry : scanner) {
            if (entry.getKey().getColumnQualifierData().toString().equals("IMG")) {
                assertEquals("ABCDEFGH", entry.getValue().toString());
            }
            count++;
        }

        assertEquals(4, count);

        // Iterator Testing
        log.info("Testing custom iterator scanning.");
        scanner = conn.createScanner(table, new Authorizations("A", "B"));
        IteratorSetting exampleIteratorSetting = new IteratorSetting(11, "example", "com.loganasherjones.mac.ExampleIterator");
        scanner.addScanIterator(exampleIteratorSetting);
        count = 0;
        for (Map.Entry<Key, Value> ignored: scanner) {
            count++;
        }
        assertEquals(0, count);
        scanner.close();
    }
}