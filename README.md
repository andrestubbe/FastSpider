# FastSpider 0.1.1 [ALPHA] — High-Performance Native WinHTTP Web Crawler for Java

[![Status](https://img.shields.io/badge/status-0.1.1-brightgreen.svg)](https://github.com/andrestubbe/FastSpider/releases/tag/0.1.1)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://www.java.com)
[![Platform](https://img.shields.io/badge/Platform-Windows%2010+-lightgrey.svg)]()
[![JitPack](https://img.shields.io/badge/JitPack-ready-green.svg)](https://jitpack.io/#andrestubbe/FastSpider)

---

**⚡ High-performance native Windows WinHTTP web crawler powered by Java 17+ Virtual Threads, zero-allocation link filtering, and FastRegex integration.**

**FastSpider** is the high-concurrency network crawling engine of the **FastJava** stack. It integrates Microsoft Windows HTTP Services (**WinHTTP** API) and Windows **Schannel** at the C++/JNI layer with modern Java **Virtual Thread executors** to achieve hyper-scalable, secure (TLS 1.2/1.3), non-blocking web crawling with zero HTTP client allocation overhead on the JVM heap.

---

## Quick Start

```java
import fastspider.FastSpider;

public class Demo {
    public static void main(String[] args) {
        // 1. Open high-speed native WinHTTP crawler
        FastSpider spider = FastSpider.open();

        // 2. Asynchronous non-blocking web fetch via Virtual Threads
        spider.fetchAsync("https://en.wikipedia.org/wiki/SIMD")
              .thenAccept(response -> {
                  if (response.isSuccess()) {
                      System.out.printf("Fetched %,d bytes in %d ms (Status %d)\n",
                              response.rawBody().length,
                              response.fetchTimeMs(),
                              response.statusCode());
                  }
              }).join();
    }
}
```

---

## 📑 Table of Contents

- [Why FastSpider?](#why-fastspider)
- [Key Features](#key-features)
- [Real-World Examples](#real-world-examples)
- [Performance Benchmarks](#performance-benchmarks)
- [API Quick Reference](#api-quick-reference)
- [Technical Examples & Hero Demos](#technical-examples--hero-demos)
- [Installation](#installation)
- [Documentation](#documentation)
- [License](#license)

---

## Why FastSpider?

> [!IMPORTANT]
> **"Native OS WinHTTP Sockets Over Heavyweight Java HTTP Descriptors. Zero JVM Connection Churn."**

Standard Java HTTP clients (`java.net.http.HttpClient` or Apache HttpComponents) create substantial JVM heap overhead when maintaining hundreds of concurrent sockets, SSL contexts, and buffer objects.

`FastSpider` offloads the entire network transport to the **Windows WinHTTP & Schannel subsystem**:

1. **Kernel-Level Connection Pooling**: Reuses native OS connection pools and DNS cache across threads without Java object overhead.
2. **Zero-Allocation Network Streaming**: Receives raw HTTP bytes directly into off-heap memory buffers.
3. **Seamless Virtual Thread Concurrency**: Scales to thousands of simultaneous requests without blocking OS threads or inflating heap memory.

---

## Key Features

- **🌐 WinHTTP Enterprise Core**: Native Microsoft HTTP client handling DNS, connection pooling, and TLS 1.3 handshakes automatically.
- **🧵 Virtual Thread Scheduler**: Delegates blocking JNI network tasks to lightweight Java Virtual Threads for scalable asynchronous execution.
- **⚡ FastRegex & AVX2 Integration**: Uses zero-allocation regex pattern matching and vector instructions for link extraction.
- **📦 Zero-Heap Networking**: Eliminates JVM socket descriptors, request state wrappers, and GC cycles for high request throughput.

---

## Real-World Examples

### 1. Autonomous AI Research Agent Pipeline
Background document and HTML fetching for `FastAIAgent` and `FastAIReasoner` without inflating JVM memory:
```java
FastSpider spider = FastSpider.open();
spider.fetchAsync("https://en.wikipedia.org/wiki/Artificial_intelligence")
      .thenAccept(res -> {
          if (res.isSuccess()) {
              List<String> links = spider.extractHrefs(res.rawBody());
              System.out.printf("Discovered %,d reference links for agent context.\n", links.size());
          }
      });
```

### 2. High-Throughput Documentation Ingestion
Batch fetching hundreds of documentation pages for instant full-text indexing in `FastFileContentIndex`:
```java
List<String> docPages = List.of(
    "https://docs.oracle.com/en/java/javase/17/docs/api/index.html",
    "https://docs.oracle.com/en/java/javase/21/docs/api/index.html"
);
List<CompletableFuture<FastSpider.SpiderResponse>> batch = docPages.stream()
    .map(spider::fetchAsync)
    .toList();
CompletableFuture.allOf(batch.toArray(new CompletableFuture[0])).join();
```

### 3. Real-Time Price & Feed Monitoring
Periodic, low-latency API and RSS endpoint polling with WinHTTP connection pooling and zero GC churn:
```java
ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
scheduler.scheduleAtFixedRate(() -> {
    FastSpider.SpiderResponse res = spider.fetchAsync("https://api.example.com/feed").join();
    if (res.isSuccess()) {
        processFeed(res.rawBody());
    }
}, 0, 1, TimeUnit.SECONDS);
```

---

## Performance Benchmarks

Measured on **Intel/AMD x64 Hardware** with Windows 11:

| Operation | Requests | Java HttpClient (Async) | **FastSpider Native (0.1.1)** | Measured Speedup |
|---|---|---|---|---|
| **Concurrent Fetch (100 Pages)** | 100 Req | ~220 ms | **~120 ms** | **1.8× Faster** |
| **Max Memory Overhead** | 100 Req | ~84 MB | **~4 MB** | **21× Less Memory** |

*Run the benchmarks locally:* `.\run-demo.bat`

---

## API Quick Reference

| Method / Class | Description |
|---|---|
| `FastSpider.open()` | Initializes a thread-safe native WinHTTP crawler instance. |
| `spider.fetchAsync(url)` | Asynchronously fetches URL content returning `CompletableFuture<SpiderResponse>`. |
| `response.isSuccess()` | Checks whether HTTP response status is 200-299. |
| `response.rawBody()` | Returns downloaded raw byte array payload. |
| `response.fetchTimeMs()` | Time taken to resolve DNS, establish TLS, and download payload in milliseconds. |

---

## Technical Examples & Hero Demos

| Case | Java Example | Launcher | Description |
|---|---|---|---|
| **Hero Demo** | [Demo.java](src/main/java/fastspider/Demo.java) | `run-demo.bat` | Live Wikipedia sequential crawl, parallel Virtual Thread batch, and AVX2 link extraction. |
| **Joint Pipeline Demo** | [PipelineDemo.java](examples/PipelineDemo/src/main/java/fastpipeline/PipelineDemo.java) | `run-pipeline.bat` | Orchestrates FastSpider and FastScrape in unison. |

---

## Installation

### Option 1: Maven (JitPack)

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
        <artifactId>FastSpider</artifactId>
        <version>0.1.1</version>
    </dependency>
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>FastRegex</artifactId>
        <version>0.1.0</version>
    </dependency>
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>FastCore</artifactId>
        <version>0.1.0</version>
    </dependency>
</dependencies>
```

### Option 2: Gradle (via JitPack)

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.andrestubbe:FastSpider:0.1.1'
    implementation 'com.github.andrestubbe:FastRegex:0.1.0'
    implementation 'com.github.andrestubbe:FastCore:0.1.0'
}
```

### Option 3: Direct Download (No Build Tool)

Download the latest JARs directly:

1. 📦 **[FastSpider-0.1.1.jar](https://github.com/andrestubbe/FastSpider/releases/download/0.1.1/FastSpider-0.1.1.jar)** (The Core Library)
2. ⚡ **[FastRegex-0.1.0.jar](https://github.com/andrestubbe/FastRegex/releases/download/0.1.0/FastRegex-0.1.0.jar)** (Zero-Allocation Regex Scanner)
3. ⚙️ **[fastcore-0.1.0.jar](https://github.com/andrestubbe/FastCore/releases/download/0.1.0/fastcore-0.1.0.jar)** (Native JNI Loader)

---

## Documentation

* **[REFERENCE.md](docs/REFERENCE.md)**: Full API reference and method signatures.
* **[PHILOSOPHY.md](docs/PHILOSOPHY.md)**: Architectural design principles and network model.
* **[CHANGELOG.md](docs/CHANGELOG.md)**: Release history and version notes.
* **[ROADMAP.md](docs/ROADMAP.md)**: Future milestones and planned features.
* **[COMPILE.md](docs/COMPILE.md)**: Instructions for compiling from source.

---

## License

MIT License. See [LICENSE](LICENSE) file for details.

---

**Part of the FastJava Ecosystem** — *Making the JVM faster. Small package. Maximum speed. Zero bloat. 🚀🕷️*
