---
hide:
  - navigation
---
# Docker Mini Accumulo Cluster

This section will go over how to use the Mini Accumulo Cluster with Docker.

## Usage

The fastest way to get started is:

```bash
docker run \
  -it \
  --rm \
  --name mac \
  -p 21811:21811 \
  loganasherjones/mini-accumulo-cluster:{{ gradle.project_version }}
```

You should see logs similar to:

```text
com.loganasherjones.mac.MAC	[main]	Starting Mini Accumulo Cluster
com.loganasherjones.mac.MACProcessSpawner	[main]	Starting mac-a9fac1eb-b684-4cc9-8325-a90c57971dbb-zookeeper Process
com.loganasherjones.mac.MAC	[main]	Waiting for zookeeper to report ok.
com.loganasherjones.mac.MAC	[main]	Zookeeper reported ok.
com.loganasherjones.mac.MACProcessSpawner	[main]	Starting mac-a9fac1eb-b684-4cc9-8325-a90c57971dbb-accumulo-init Process
com.loganasherjones.mac.MACProcessSpawner	[main]	Starting mac-a9fac1eb-b684-4cc9-8325-a90c57971dbb-tserver-0 Process
com.loganasherjones.mac.MACProcessSpawner	[main]	Starting mac-a9fac1eb-b684-4cc9-8325-a90c57971dbb-tserver-1 Process
com.loganasherjones.mac.MACProcessSpawner	[main]	Starting mac-a9fac1eb-b684-4cc9-8325-a90c57971dbb-accumulo-manager-state Process
com.loganasherjones.mac.MACProcessSpawner	[main]	Starting mac-a9fac1eb-b684-4cc9-8325-a90c57971dbb-manager Process
com.loganasherjones.mac.MACProcessSpawner	[main]	Starting mac-a9fac1eb-b684-4cc9-8325-a90c57971dbb-gc Process
com.loganasherjones.mac.MAC	[main]	Mini Accumulo Cluster Started successfully
```

Congratulations, you have an accumulo cluster!

You can connect to it with the Accumulo shell by opening another terminal and
typing:

```bash
docker exec -it mac /app/bin/mac-app shell
```

This should drop you into a shell as root:

```bash
Shell - Apache Accumulo Interactive Shell
- 
- version: 1.10.4
- instance name: default
- instance id: bd3cd6c9-a3b7-4094-a08e-74c03657019a
- 
- type 'help' for a list of available commands
- 
root@default>
```

Alternatively, you can connect with the Java API:

```java
public void myTest() {
    ZooKeeperInstance instance = new ZooKeeperInstance("default", "127.0.0.1:21811");
    Connector conn = instance.getConnector("root", new PasswordToken("notsecure"));
}
```

Happy integration testing!

## Configuration

While the [Java API](../java/index.md) provides the most customization options
the docker image is typically configured with environment variables. Here they
are listed out:

| Env Name                           | Default     | Description                                                             |
|------------------------------------|-------------|-------------------------------------------------------------------------|
| `MAC_ACCUMULO_BIND_ADDRESS `       | N/A         | This will automatically be set to the container IP.                     |
| `MAC_BASE_DIR`                     | `/app`      | Where MAC will store config / logs / data                               |
| `MAC_FILE_LOGGING`                 | N/A         | Set to `"true"` if you want MAC to log to file instead of STDOUT/STDERR |
| `MAC_INSTANCE_NAME`                | `default`   | This will set Accumulo's instance name                                  |
| `MAC_LOG_LEVEL`                    | `INFO`      | The log level for MAC itself                                            |
| `MAC_NUM_TSERVERS`                 | `2`         | This will set the number of Tablet Servers Spawned                      |
| `MAC_ROOT_PASSWORD`                | `notsecure` | This will set the password for the `root` user                          |
| `MAC_USE_EXTERNAL_ZOOKEEPER`       | `false`     | This will force MAC not to spawn its own Zookeeper Instance             |
| `MAC_ZOOKEEPER_HOST`               | `127.0.0.1` | The hostname to use for zookeeper                                       |
| `MAC_ZOOKEEPER_PORT`               | `21811`     | The port MAC will use for Zookeeper                                     |
| `MAC_ZOOKEEPER_STARTUP_TIMEOUT_MS` | 10,000      | Sets how long MAC will wait for Zookeeper to respond to `ruok`          |
| `ROOT_LOG_LEVEL`                   | `ERROR`     | The root log level for all loggers                                      |
