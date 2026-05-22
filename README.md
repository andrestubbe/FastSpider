# FastSpider — High-performance native WinHTTP web crawler for Java

**High-performance native Windows WinHTTP web crawler powered by Java 17+ Virtual Threads.**

[![Build](https://img.shields.io/github/actions/workflow/status/andrestubbe/FastSpider/maven.yml?branch=main)](https://github.com/andrestubbe/FastSpider/actions)
[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://www.java.com)
[![Platform](https://img.shields.io/badge/Platform-Windows%2010+-lightgrey.svg)]()
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![JitPack](https://jitpack.io/v/andrestubbe/FastSpider.svg)](https://jitpack.io/#andrestubbe/FastSpider)

FastSpider is the high-concurrency network crawling engine of the **FastJava** stack. It integrates Microsoft Windows HTTP Services (**WinHTTP** API) and Windows **Schannel** at the C++/JNI layer with modern Java **Virtual Thread executors** to achieve hyper-scalable, secure (TLS 1.2/1.3), non-blocking web crawling with zero HTTP client allocation overhead on the JVM heap.

```java
// Quick Start — Asynchronous Fetch
FastSpider spider = FastSpider.open();

spider.fetchAsync("https://example.com")
      .thenAccept(response -> {
          if (response.isSuccess()) {
              System.out.println("Fetched " + response.rawBody().length + " bytes in " + response.fetchTimeMs() + "ms");
          }
      });
```

## Table of Contents
- [Key Features](#key-features)
- [Performance](#performance)
- [API Quick Reference](#api-quick-reference)
- [Installation](#installation)
- [Technical Examples & Hero Demos](#technical-examples--hero-demos)
- [Platform Support](#platform-support)
- [Modular Ecosystem](#modular-ecosystem)
- [License](#license)

---

## Key Features
- **🌐 WinHTTP Enterprise Core**: Native Microsoft HTTP client that handles DNS, connection pooling, and secure TLS 1.3 handshakes automatically.
- **🧵 Virtual Thread Scheduler**: Delegates blocking JNI network tasks to lightweight Java Virtual Threads for scalable asynchronous execution.
- **⚡ Built-in AVX2 Extractor**: Shares FastJava's AVX2 vectors to clean formatting and find links directly on the downloaded bytes.
- **📦 Zero-Heap Networking**: Avoids JVM connection descriptors, request buffers, and GC cycles for extreme request densities.

---

## 📊 Performance (v0.1.0)

Measured on **Intel/AMD x64 Hardware** with Windows 11.

| Operation | Requests | Java HttpClient (Async) | FastSpider Native (v0.1.0) | Speedup |
|-----------|----------|-------------------------|---------------------------|---------|
| **Concurrent Fetch** | 100 Req  | ~220 ms                 | **~120 ms**               | **1.8x** |
| **Max Memory Overhead**| 100 Req  | ~84 MB                  | **~4 MB**                 | **21x** |

> [!NOTE]
> FastSpider drastically reduces GC pause frequency and native thread handle count compared to traditional JVM client engines.

---

## API Quick Reference

| Method | Description | Target Path |
|--------|-------------|-------------|
| `fetchAsync(...)` | Schedules a non-blocking asynchronous fetch inside the Virtual Thread Executor. | [Reference →](REFERENCE.md#fetchasync) |
| `fetchBatch(...)` | Performs parallel concurrent page crawls and blocks until all complete. | [Reference →](REFERENCE.md#fetchbatch) |
| `extractCleanText(...)` | Cleans document tags natively to yield readable text for LLMs. | [Reference →](REFERENCE.md#extractcleantext) |
| `extractHrefs(...)` | Rapidly extracts all hyperlink targets from HTML page bytes natively. | [Reference →](REFERENCE.md#extracthrefs) |

> [!TIP]
> Use `FastSpider.open()` to obtain a thread-safe, reusable native crawler instance.

---

## Installation

FastJava modules require **two** dependencies: the module itself, and `FastCore` (which automatically unpacks and loads the JNI library).

### Maven (JitPack)
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>fastspider</artifactId>
        <version>v0.1.0</version>
    </dependency>
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>fastcore</artifactId>
        <version>v0.1.0</version>
    </dependency>
</dependencies>
```

### Gradle (JitPack)
```gradle
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.andrestubbe:fastspider:v0.1.0'
    implementation 'com.github.andrestubbe:fastcore:v0.1.0'
}
```

### Option 3: Direct Download (No Build Tool)
Download the latest pre-compiled JARs directly to add them to your project's classpath:

1. 📦 [**fastspider-v0.1.0.jar**](https://github.com/andrestubbe/FastSpider/releases/download/v0.1.0/fastspider-0.1.0.jar) (The Core Library & native JNI resources)
2. ⚙️ [**fastcore-v0.1.0.jar**](https://github.com/andrestubbe/FastCore/releases/download/v0.1.0/fastcore-0.1.0.jar) (The Mandatory Native Loader)

> [!IMPORTANT]
> Both JARs must be present in your classpath for FastSpider's native functions to operate correctly.

---

## Technical Examples & Hero Demos
Explore the complete source configurations and benchmarks:

* **⚡ Interactive Demo**: [Demo.java](src/main/java/fastspider/Demo.java) (sets up an offline mock server, performs parallel fetches of delayed endpoints, and extracts content).
* **📈 Performance Benchmark**: [Benchmark.java](src/main/java/fastspider/Benchmark.java) (races concurrent fetches against standard Java HttpClient).
* **🧪 Test Suite**: [FastSpiderTest.java](src/test/java/fastspider/FastSpiderTest.java) (fully automated JUnit 5 crawler test suite).

Run the hero demo locally from the command line:
```bash
mvn exec:java "-Dexec.mainClass=fastspider.Demo"
```

---

## Platform Support
| Platform | Status |
|----------|--------|
| Windows 10/11 (x64) | ✅ Fully Supported (WinHTTP + AVX2 Native) |
| Linux | 🚧 Planned |
| macOS | 🚧 Planned |

---

## Modular Ecosystem
Combine FastSpider with other accelerators for maximum efficiency:
* [**FastScrape**](https://github.com/andrestubbe/FastScrape) — Native SIMD HTML parser.
* [**FastCore**](https://github.com/andrestubbe/FastCore) — Native loading substrate.
* [**FastBytes**](https://github.com/andrestubbe/FastBytes) — Hardware-aligned byte arrays.
* [**FastJSON**](https://github.com/andrestubbe/FastJSON) — SIMD-powered JSON parser.

---

**Part of the FastJava Ecosystem** — *Making the JVM faster.*

Made with ⚡ by Andre Stubbe
