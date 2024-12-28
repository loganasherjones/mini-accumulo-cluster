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

- [ ] Make the basics work
- [ ] Figure out publishing
- [ ] Play with GitHub Actions/Workflows
- [ ] Figure out the license
- [ ] Add documentation
- [ ] Add Support For:
    - [ ] Existing Zookeeper
    - [ ] Mini HDFS

## Application TODOs

- [ ] Create a configurable application that runs the library
- [ ] Create a docker image
- [ ] Publish the docker image
- [ ] Add documentation
- [ ] Add support for:
  - [ ] Auto Schema Creation

## Java Usage

Coming soon...

## Docker Usage

Coming soon...

## Docker Compose Usage

Coming soon...

## Test Containers Usage

Coming soon...
