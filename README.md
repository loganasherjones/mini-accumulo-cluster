# Mini Accumulo Cluster

Accumulo provides its own [Mini Accumulo Cluster](https://accumulo.apache.org/1.10/accumulo_user_manual.html#_mini_accumulo_cluster) (MAC).
Here are some of the problems I've experienced when working with the bundled MAC:

1. JDK 17 Support
2. Lacking Configuration Options
3. No Container Support

This project is an attempt to address these shortcomings. The main focus of
this project is to provide an Accumulo cluster that can be used for integration
testing.

At the moment, only 1.10.X is supported with the intent of supporting 2.X in
the future.

## Library TODOs

- [x] Make the basics work
- [ ] Make logging good
- [ ] Figure out publishing
- [ ] Play with GitHub Actions/Workflows
- [ ] Figure out the license
- [ ] Add documentation
- [ ] Support For:
    - [ ] Existing Zookeeper
    - [ ] Mini HDFS(?)
    - [ ] JVM Settings
      - [ ] Zookeeper
      - [ ] Manager
      - [ ] Tablet Servers
      - [ ] Garbage Collector
    - [ ] Configuration File support
      - [ ] Zoo.cfg Configuration Overrides
      - [ ] accumulo-site.xml overrides
- [ ] Automated tests for
  - [x] Custom Iterators (Native)
  - [x] Custom Iterators (Docker)
  - [x] Native MAC
  - [x] Docker MAC
  - [x] Docker Compose MAC
  - [ ] Docker Compose Existing Zookeeper MAC
  - [ ] Test Containers

## Application TODOs

- [x] Create a configurable application that runs the library
- [x] Create a docker image
- [x] Auto figure-out version number in docker build
- [ ] Confirm testing works with docker image.
- [ ] Confirm testing works with native app.
- [ ] Publish the docker image
- [ ] Add documentation
- [ ] Add support for:
  - [ ] Log Level Support
      - [ ] Root / ZK / Accumulo / MAC
  - [ ] Auto Schema Creation

## Java Usage

Coming soon...

## Docker Usage

Coming soon...

## Docker Compose Usage

Coming soon...

## Test Containers Usage

Coming soon...
