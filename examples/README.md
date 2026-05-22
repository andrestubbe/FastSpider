# FastSpider Examples

This folder contains standalone example projects to demonstrate and test the library.

## Demo

The `Demo` project provides a simple "Hello World" implementation.

To run it locally using the JAR you just built:
```bash
cd Demo
mvn compile exec:java
```

## Benchmark

The `Benchmark` project compares the performance of the native FastSpider library against standard Java equivalents.

To run it:
```bash
cd Benchmark
mvn compile exec:java
```

> **Note:** By default, the `pom.xml` files in these examples are configured to use `<scope>system</scope>` pointing to the `target/` directory of the parent project. This allows you to test changes immediately after building the main library without publishing to a repository. To see how an external user would use it via JitPack, check the comments inside the `pom.xml`.

