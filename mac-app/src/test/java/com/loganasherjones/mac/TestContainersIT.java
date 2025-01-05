package com.loganasherjones.mac;

import org.apache.accumulo.core.client.Connector;
import org.apache.accumulo.core.client.Instance;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.util.HashMap;
import java.util.Map;

@Testcontainers
public class TestContainersIT {
    private static final String projectVersion = System.getenv("PROJECT_VERSION");
    private static final DockerImageName dockerImage = DockerImageName.parse("loganasherjones/mini-accumulo-cluster:" + projectVersion);

    @Container
    @SuppressWarnings("unchecked")
    public GenericContainer mac = new GenericContainer(dockerImage)
            .withCopyToContainer(
                    MountableFile.forHostPath("build/iterators"),
                    "/app/lib/ext"
            )
            .withExposedPorts(21813)
            .withEnv(createEnv());

    @Test
    public void testTestContainers() throws Exception {
        Instance instance = TestHelper.getInstance("default", mac.getHost(), mac.getFirstMappedPort());
        Connector rootConnector = TestHelper.getRootConnector(instance);
        TestClient client = new TestClient(instance, rootConnector);
        client.runTest();
    }

    private static Map<String, String> createEnv() {
        Map<String, String> env = new HashMap<>();
        env.put("MAC_ZOOKEEPER_PORT", "21813");
        return env;
    }
}
