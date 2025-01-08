# Mini Accumulo Cluster

Accumulo provides its own [Mini Accumulo Cluster](https://accumulo.apache.org/1.10/accumulo_user_manual.html#_mini_accumulo_cluster) (MAC).
Here are some of the problems I've experienced when working with the bundled MAC:

1. JDK 11+ Support
2. Lacking Configuration Options
3. No Container Support

This project is an attempt to address these shortcomings. The main focus of
this project is to provide an Accumulo cluster that can be used for integration
testing.

At the moment, only 1.10.X is supported with the intent of supporting 2.X in
the future.

## TODOs:

- [ ] Publish library
- [ ] Publish Docker Image
- [ ] Documentation in the README
- [ ] Auto Schema Creation
- [ ] Create 2.X Version
- [ ] Create 3.X Version

## Java Usage

To use this project, first install it:

**Gradle**

```
implementation 'com.loganasherjones:mini-accumulo-cluster:1.10.4
```

Coming soon...

## Docker Usage

Coming soon...

## Docker Compose Usage

Coming soon...

## Test Containers Usage

Coming soon...
