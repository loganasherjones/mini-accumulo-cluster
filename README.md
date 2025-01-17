# Mini Accumulo Cluster

Accumulo provides its own [Mini Accumulo Cluster](https://accumulo.apache.org/1.10/accumulo_user_manual.html#_mini_accumulo_cluster) (MAC).
Here are some of the problems I've experienced when working with the bundled MAC:

1. JDK 17+ Support
2. Lacking Configuration Options
3. No Container Support

This project is an attempt to address these shortcomings. The main focus of
this project is to provide an Accumulo cluster that can be used for integration
testing.

At the moment, only 1.10.X is supported with the intent of supporting 2.X in
the future.

## TODOs:

- [x] Publish library
- [ ] Publish Docker Image
- [ ] Publish Versioned Documentation
- [ ] Auto Schema Creation
- [ ] Create 2.X Version
- [ ] Create 3.X Version

## Java Usage

See [Java Documentation](./docs/java/index.md)

## Docker Usage

See [Docker Documentation](./docs/docker/index.md)

## Releasing

Setting up gradle to push thing to maven central was a major pain. I'm
documenting what I learned here briefly.

1. [Generate GPG Keys](https://docs.github.com/en/authentication/managing-commit-signature-verification/adding-a-gpg-key-to-your-github-account)
2. Have the following `~/.gradle/gradle.properties`:
   ```
   mavenCentralUsername=<username>
   mavenCentralPassword=<password>

   signing.keyId=<keyId>
   signing.password=<password>
   signing.secretKeyRingFile=/Users/yourusername/.gnupg/secring.gpg
   ```
   * Where `<username>`/`<password>` are created via the [client portal](https://central.sonatype.com/)
     by logging in, going to the top right, selecting "View account", then
     "Generate User Token"
3. Run `./gradlew library:publishToMavenCentral`
4. Verify the release in the Client Portal -> `Publish`
5. Find your deployment and click "Publish"
