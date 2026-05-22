# FastSpider
Native high-throughput web crawler for Java.

[![Status](https://img.shields.io/badge/status-v0.1.0--alpha-orange.svg)]()
[![Java](https://img.shields.io/badge/Java-17+-blue.svg)](https://www.java.com)
[![Platform](https://img.shields.io/badge/Platform-Windows%2010+-lightgrey.svg)]()
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

## Vision
`FastSpider` is the network engine for agent-style web reading.

It is designed to fetch many pages concurrently and return raw response bytes quickly, so downstream components (like `FastScrape`) can extract usable text for LLM/RAG pipelines.

Compared to browser-based crawling stacks, FastSpider targets:

- lower memory usage
- lower startup overhead
- higher parallel request density
- direct byte-level handoff to native processing

## Core API (planned)
```java
public interface FastSpider {
    static FastSpider open() { return new FastSpiderImpl(); }

    CompletableFuture<SpiderResponse> fetchAsync(String url);
    List<SpiderResponse> fetchBatch(List<String> urls);

    String extractCleanText(byte[] htmlData);
    List<String> extractHrefs(byte[] htmlData);
}

public record SpiderResponse(int statusCode, byte[] rawBody, long fetchTimeMs) {}
```

## Native design
1. **Asynchronous networking**
   Native HTTP/HTTPS fetching optimized for large concurrency.
2. **High-speed parsing path**
   HTML bytes are passed to SIMD-friendly extraction logic (directly or via FastScrape integration).
3. **Zero-copy oriented flow**
   Keep data in byte form as long as possible before Java string materialization.

## Agent workflow
1. Agent produces candidate URLs from search/retrieval.
2. `fetchBatch(urls)` downloads pages concurrently.
3. `extractCleanText(...)` removes markup noise.
4. Clean text chunks are sent to tokenizer/context builder.

For JS-heavy pages with little server-rendered HTML, this module is expected to be combined with visual/browser automation modules.

## Installation
### Maven
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>io.github.andrestubbe</groupId>
        <artifactId>fastspider</artifactId>
        <version>0.1.0</version>
    </dependency>
    <dependency>
        <groupId>com.github.andrestubbe</groupId>
        <artifactId>fastcore</artifactId>
        <version>v1.0.0</version>
    </dependency>
</dependencies>
```

### Gradle
```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'io.github.andrestubbe:fastspider:0.1.0'
    implementation 'com.github.andrestubbe:fastcore:v1.0.0'
}
```

## Current status
This repository is in early alpha and currently contains JNI scaffolding.
Networking and extraction internals are the next implementation milestone.

## License
MIT License — see [LICENSE](LICENSE).
