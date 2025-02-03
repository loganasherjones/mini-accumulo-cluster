---
hide:
- navigation
---
# Java Mini Accumulo Cluster

This section will go over how to use the Mini Accumulo Cluster from Java code.

## Installation

Add the following to your build tool:

=== "Gradle (kts)"

    ```kotlin
    testImplementation("com.loganasherjones:mini-accumulo-cluster:{{ gradle.project_version }}")
    ```

=== "Gradle (groovy)"

    ```groovy
    testImplementation "com.loganasherjones:mini-accumulo-cluster:{{ gradle.project_version }}"
    ```

=== "Maven"

    ```xml
    <dependency>
        <groupId>org.webjars.bower</groupId>
        <artifactId>protobuf</artifactId>
        <version>5.0.0</version>
    </dependency>
    ```

## Usage

The fastest way to get started is to initialize the object and call `start()`:

```java
import com.loganasherjones.mac.MAC;
import org.apache.accumulo.core.client.Connector;

public class MyTest {
    
    public void testAccumulo() throws Exception {
        // Create mac with the default configuration.
        MAC cluster = new MAC();
        
        // Start the server.
        cluster.start();
        
        // Get a connector for the 'root' user.
        AccumuloClient rootClient = cluster.getRootClient();
        
        // TODO: Insert your test code here.
        
        // When you're done, stop the server.
        cluster.stop();
    }
}
```

## Configuration

There are many options you can use to configure the Mini Accumulo Cluster.
To set them in java, you'll need to use the `MACConfig.MACConfigBuilder` class:

```java
import com.loganasherjones.mac.MAC;
import com.loganasherjones.mac.MACConfig;

public class MyTest {
    
    public void testAccumulo() {
        MACConfig.MACConfigBuilder builder = new MACConfig.MACConfigBuilder();
        
        // Build a config. These are just some of the options.
        MACConfig config = builder
                .withFileLogging() // Log to files instead of STDOUT/STDERR
                .withRootPassword("supersecret") // Change root password
                .withBaseDirectory(new File("/some/path")) // Set configuration / logging files destination
                .withStaticZooKeeperPort(21811) // Set specific Zookeeper port
                .build();
        
        MAC cluster = new MAC(config);
    }
}
```

One of the main goals of this project is to provide a way to programmatically
configure just about everything pertaining to the MAC. The `MACConfigBuilder`
allows you to set any JVM property, for any/all processes, along with 
controlling the `zoo.cfg` and `accumulo.properties` file. If you need more
customization, don't hesitate to open an issue.
