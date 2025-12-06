# Codesize

Codesize is a tool for calculating the bytecode size of a Java class file, ZIP/JAR archive.
It is being used by Robocode to calculate the code size of robots after having compiled these.

Read more about the [Code Size](https://robowiki.net/wiki/Code_Size) on the RoboWiki.

## Build

To build Codesize, use Gradle:

```sh
./gradlew build
```

This will compile the project and create the JAR file in the `build/libs` directory.

## Publishing

Codesize uses in-memory PGP signing for publishing to OSSRH (Maven Central). You must provide the following properties
in your `gradle.properties`:

```
signing.keyId=YOUR_KEY_ID
signingKey=YOUR_ASCII_ARMOR_PRIVATE_KEY
signingPassword=YOUR_KEY_PASSWORD
ossrhUsername=YOUR_OSSRH_USERNAME
ossrhPassword=YOUR_OSSRH_PASSWORD
```

To publish the artifact:

```sh
 ./gradlew publishToSonatype closeAndReleaseSonatypeStagingRepository
```

This will sign and upload the artifacts to the configured OSSRH repository.
