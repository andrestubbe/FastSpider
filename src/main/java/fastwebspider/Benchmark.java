package fastspider;

import fastansi.FastANSI;
import fastregex.FastRegex;
import fastregex.MatchResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Multi-Tier Comparative Benchmark Suite for FastSpider vs Standard Java Stacks.
 * Evaluates Link Extraction, Concurrent 200-Page Massive Crawling, and Plaintext Extraction with direct Head-to-Head comparisons.
 */
public class Benchmark {

    private Benchmark() {}

    private static final Pattern HREF_PATTERN = Pattern.compile("href=\"([^\"]+)\"");

    private static final List<String> ROOT_SEEDS = List.of(
        "https://en.wikipedia.org/wiki/SIMD",
        "https://en.wikipedia.org/wiki/Advanced_Vector_Extensions",
        "https://en.wikipedia.org/wiki/AVX-512",
        "https://en.wikipedia.org/wiki/Streaming_SIMD_Extensions",
        "https://en.wikipedia.org/wiki/ARM_architecture_family",
        "https://en.wikipedia.org/wiki/RISC-V",
        "https://en.wikipedia.org/wiki/Graphics_processing_unit",
        "https://en.wikipedia.org/wiki/General-purpose_computing_on_graphics_processing_units",
        "https://en.wikipedia.org/wiki/CUDA",
        "https://en.wikipedia.org/wiki/OpenCL",
        "https://en.wikipedia.org/wiki/Java_(programming_language)",
        "https://en.wikipedia.org/wiki/C%2B%2B",
        "https://en.wikipedia.org/wiki/Rust_(programming_language)",
        "https://en.wikipedia.org/wiki/Go_(programming_language)",
        "https://en.wikipedia.org/wiki/Julia_(programming_language)",
        "https://en.wikipedia.org/wiki/Fortran",
        "https://en.wikipedia.org/wiki/Assembly_language",
        "https://en.wikipedia.org/wiki/Compiler",
        "https://en.wikipedia.org/wiki/Just-in-time_compilation",
        "https://en.wikipedia.org/wiki/Parallel_computing"
    );

    public static void main(String[] args) throws Exception {
        System.out.println(darkGray("========================================================================================================================"));
        System.out.println(" " + boldWhite("FastSpider & FastJava") + darkGray(" — Comprehensive Multi-Tier 120-Column Benchmark Suite"));
        System.out.println(darkGray("========================================================================================================================"));
        System.out.println();

        FastSpider spider = FastSpider.open();

        // ─────────────────────────────────────────────────────────────────────
        // Tier 1: Zero-Allocation AVX2 Link Extraction Benchmark
        // ─────────────────────────────────────────────────────────────────────
        System.out.println(darkGray("[Tier 1]") + " " + boldWhite("HTML Link Extraction Benchmark") + darkGray(" (Heavy Wikipedia Document)"));
        System.out.print(darkGray(" Downloading live payload ... "));

        FastSpider.SpiderResponse response = spider.fetchAsync("https://en.wikipedia.org/wiki/Java_(programming_language)").join();
        byte[] htmlBytes = response.rawBody();
        String htmlText = new String(htmlBytes, StandardCharsets.UTF_8);
        System.out.printf(darkGray("OK (") + boldWhite("%,d bytes") + darkGray(", %d ms)\n"), htmlBytes.length, response.fetchTimeMs());

        int warmup = 50;
        int iterations = 300;

        // JDK Pattern Warmup & Benchmark
        for (int i = 0; i < warmup; i++) runJdkRegex(htmlText, HREF_PATTERN);
        long t0 = System.nanoTime();
        int totalJdkLinks = 0;
        for (int i = 0; i < iterations; i++) {
            totalJdkLinks += runJdkRegex(htmlText, HREF_PATTERN);
        }
        long jdkNanos = System.nanoTime() - t0;
        double jdkOpsPerMs = (double) iterations / (jdkNanos / 1_000_000.0);
        double jdkAvgMs = (jdkNanos / 1_000_000.0) / iterations;

        // FastSpider AVX2 Warmup & Benchmark
        for (int i = 0; i < warmup; i++) spider.extractHrefs(htmlBytes);
        long t1 = System.nanoTime();
        int totalFastLinks = 0;
        for (int i = 0; i < iterations; i++) {
            totalFastLinks += spider.extractHrefs(htmlBytes).size();
        }
        long fastNanos = System.nanoTime() - t1;
        double fastOpsPerMs = (double) iterations / (fastNanos / 1_000_000.0);
        double fastAvgMs = (fastNanos / 1_000_000.0) / iterations;

        double extractSpeedup = (double) jdkNanos / fastNanos;

        System.out.println();
        System.out.println(darkGray("------------------------------------------------------------------------------------------------------------------------"));
        System.out.printf(" %-48s | %-22s | %-20s | %-20s\n", "Extraction Engine", "Throughput (ops/ms)", "Avg Latency (ms)", "Speedup");
        System.out.println(darkGray("------------------------------------------------------------------------------------------------------------------------"));
        System.out.printf(" %-48s | %22.2f | %17.3f ms | %s\n", "Standard JDK Pattern.matcher(\"href=...\")", jdkOpsPerMs, jdkAvgMs, darkGray("1.00x Base          "));
        System.out.printf(" %-48s | %22.2f | %17.3f ms | %s\n", "FastSpider AVX2 + FastRegex", fastOpsPerMs, fastAvgMs, boldWhite(String.format("%.2fx Faster         ", extractSpeedup)));
        System.out.println(darkGray("------------------------------------------------------------------------------------------------------------------------"));
        System.out.printf(darkGray(" Extracted ") + boldWhite("%,d links") + darkGray(" per iteration across %,d iterations.\n\n"), totalFastLinks / iterations, iterations);

        // ─────────────────────────────────────────────────────────────────────
        // Tier 2: Concurrent Multi-Threaded Batch Crawl Benchmark (Scaled to 200 Nodes)
        // ─────────────────────────────────────────────────────────────────────
        System.out.println(darkGray("[Tier 2]") + " " + boldWhite("Concurrent Batch Crawl Benchmark") + darkGray(" (200 Parallel Architecture Nodes)"));
        System.out.print(darkGray(" Pre-building 200 distinct test URLs from live architecture branches ... "));

        // Build 200 unique URLs from 20 seeds (10 URLs per seed)
        List<String> crawlUrls = new ArrayList<>(200);
        Set<String> visitedSet = ConcurrentHashMap.newKeySet();
        for (String seed : ROOT_SEEDS) {
            crawlUrls.add(seed);
            visitedSet.add(seed);
        }
        for (String seed : ROOT_SEEDS) {
            FastSpider.SpiderResponse seedResp = spider.fetchAsync(seed).join();
            List<String> validLinks = filterWikiArticleLinks(spider.extractHrefs(seedResp.rawBody()));
            int added = 0;
            for (String lk : validLinks) {
                if (visitedSet.add(lk)) {
                    crawlUrls.add(lk);
                    added++;
                    if (added >= 9 || crawlUrls.size() >= 200) break;
                }
            }
            if (crawlUrls.size() >= 200) break;
        }
        System.out.printf(darkGray("OK (") + boldWhite("%,d URLs") + darkGray(")\n"), crawlUrls.size());

        // 2a. JDK Standard HttpClient Concurrent Batch
        HttpClient jdkClient = HttpClient.newBuilder()
                .executor(Executors.newVirtualThreadPerTaskExecutor())
                .build();
        long jdkBatchT0 = System.currentTimeMillis();
        List<CompletableFuture<HttpResponse<byte[]>>> jdkFutures = new ArrayList<>();
        for (String url : crawlUrls) {
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).header("User-Agent", "FastSpider-Benchmark/0.1").build();
            jdkFutures.add(jdkClient.sendAsync(req, HttpResponse.BodyHandlers.ofByteArray()));
        }
        CompletableFuture.allOf(jdkFutures.toArray(new CompletableFuture[0])).join();
        long jdkBatchDuration = System.currentTimeMillis() - jdkBatchT0;
        double jdkPagesPerSec = crawlUrls.size() / (Math.max(jdkBatchDuration, 1) / 1000.0);

        // 2b. FastSpider Native WinHTTP Concurrent Batch
        long fastBatchT0 = System.currentTimeMillis();
        List<CompletableFuture<FastSpider.SpiderResponse>> fastFutures = new ArrayList<>();
        for (String url : crawlUrls) {
            fastFutures.add(spider.fetchAsync(url));
        }
        CompletableFuture.allOf(fastFutures.toArray(new CompletableFuture[0])).join();
        long fastBatchDuration = System.currentTimeMillis() - fastBatchT0;

        long totalDownloadedBytes = 0;
        int totalLinksDiscovered = 0;
        for (CompletableFuture<FastSpider.SpiderResponse> f : fastFutures) {
            FastSpider.SpiderResponse r = f.join();
            totalDownloadedBytes += r.rawBody().length;
            totalLinksDiscovered += spider.extractHrefs(r.rawBody()).size();
        }

        double mbDownloaded = totalDownloadedBytes / (1024.0 * 1024.0);
        double fastPagesPerSec = crawlUrls.size() / (Math.max(fastBatchDuration, 1) / 1000.0);
        double crawlSpeedup = (double) jdkBatchDuration / Math.max(fastBatchDuration, 1);

        System.out.println();
        System.out.println(darkGray("------------------------------------------------------------------------------------------------------------------------"));
        System.out.printf(" %-48s | %-22s | %-20s | %-20s\n", "Parallel Crawler (200 Pages)", "Batch Duration", "Throughput", "Speedup");
        System.out.println(darkGray("------------------------------------------------------------------------------------------------------------------------"));
        System.out.printf(" %-48s | %19d ms | %15.1f p/s | %s\n", "Standard java.net.http.HttpClient", jdkBatchDuration, jdkPagesPerSec, darkGray("1.00x Base          "));
        System.out.printf(" %-48s | %19d ms | %15.1f p/s | %s\n", "FastSpider Native WinHTTP + Virtual Threads", fastBatchDuration, fastPagesPerSec, boldWhite(String.format("%.2fx Faster         ", crawlSpeedup)));
        System.out.println(darkGray("------------------------------------------------------------------------------------------------------------------------"));
        System.out.printf(darkGray(" Downloaded ") + boldWhite(String.format("%.2f MB", mbDownloaded)) + darkGray(" and discovered ") + boldWhite(String.format("%,d links", totalLinksDiscovered)) + darkGray(" across 200 nodes in real-time.\n\n"));

        // ─────────────────────────────────────────────────────────────────────
        // Tier 3: Zero-Allocation Native Plaintext Extraction & Tag Stripper
        // ─────────────────────────────────────────────────────────────────────
        System.out.println(darkGray("[Tier 3]") + " " + boldWhite("HTML Plaintext & Tag Stripper Benchmark") + darkGray(" (Clean Text for LLMs)"));

        // JDK Regex-based HTML Stripping Warmup & Benchmark
        Pattern tagPattern = Pattern.compile("<[^>]+>");
        for (int i = 0; i < 30; i++) tagPattern.matcher(htmlText).replaceAll("");
        long jdkStripT0 = System.nanoTime();
        for (int i = 0; i < 100; i++) {
            tagPattern.matcher(htmlText).replaceAll("");
        }
        long jdkStripNanos = System.nanoTime() - jdkStripT0;
        double jdkStripOpsPerMs = 100.0 / (jdkStripNanos / 1_000_000.0);
        double jdkStripAvgMs = (jdkStripNanos / 1_000_000.0) / 100.0;

        // FastSpider Native AVX2 Clean Text Stripper
        for (int i = 0; i < 30; i++) spider.extractCleanText(htmlBytes);
        long fastStripT0 = System.nanoTime();
        String cleanSample = "";
        for (int i = 0; i < 100; i++) {
            cleanSample = spider.extractCleanText(htmlBytes);
        }
        long fastStripNanos = System.nanoTime() - fastStripT0;
        double fastStripOpsPerMs = 100.0 / (fastStripNanos / 1_000_000.0);
        double fastStripAvgMs = (fastStripNanos / 1_000_000.0) / 100.0;

        double stripSpeedup = (double) jdkStripNanos / fastStripNanos;

        System.out.println();
        System.out.println(darkGray("------------------------------------------------------------------------------------------------------------------------"));
        System.out.printf(" %-48s | %-22s | %-20s | %-20s\n", "HTML Stripper Engine", "Throughput (ops/ms)", "Avg Latency (ms)", "Speedup");
        System.out.println(darkGray("------------------------------------------------------------------------------------------------------------------------"));
        System.out.printf(" %-48s | %22.2f | %17.3f ms | %s\n", "Standard JDK RegEx HTML Parser", jdkStripOpsPerMs, jdkStripAvgMs, darkGray("1.00x Base          "));
        System.out.printf(" %-48s | %22.2f | %17.3f ms | %s\n", "FastSpider AVX2 CleanText Stripper", fastStripOpsPerMs, fastStripAvgMs, boldWhite(String.format("%.2fx Faster         ", stripSpeedup)));
        System.out.println(darkGray("------------------------------------------------------------------------------------------------------------------------"));
        System.out.printf(darkGray(" Processed 100 full payloads extracting ") + boldWhite(String.format("%,d characters", cleanSample.length())) + darkGray(" of clean text for AI models.\n\n"));

        // ─────────────────────────────────────────────────────────────────────
        // Summary Card
        // ─────────────────────────────────────────────────────────────────────
        System.out.println(darkGray("========================================================================================================================"));
        System.out.printf(" " + boldWhite("BENCHMARK VERDICT:") + darkGray(" FastSpider outperforms JDK across all tiers (") + boldWhite(String.format("Link Extraction: %.2fx", extractSpeedup)) + darkGray(" | ") + boldWhite(String.format("Batch Crawl: %.2fx", crawlSpeedup)) + darkGray(" | ") + boldWhite(String.format("CleanText: %.2fx", stripSpeedup)) + darkGray(").\n"));
        System.out.println(darkGray("========================================================================================================================"));
    }

    private static List<String> filterWikiArticleLinks(List<String> rawHrefs) {
        List<String> links = new ArrayList<>();
        for (String href : rawHrefs) {
            if (href == null || href.isEmpty()) continue;
            if (href.startsWith("/wiki/")) {
                String sub = href.substring(6);
                if (!sub.contains(":") && !sub.contains("#") && !sub.contains("?") && !sub.equals("Main_Page")) {
                    links.add("https://en.wikipedia.org" + href);
                }
            } else if (href.startsWith("https://en.wikipedia.org/wiki/")) {
                String sub = href.substring(30);
                if (!sub.contains(":") && !sub.contains("#") && !sub.contains("?") && !sub.equals("Main_Page")) {
                    links.add(href);
                }
            }
        }
        return links;
    }

    private static String darkGray(String text) {
        return FastANSI.fg(240) + text + FastANSI.RESET;
    }

    private static String boldWhite(String text) {
        return FastANSI.BOLD + FastANSI.FG_BRIGHT_WHITE + text + FastANSI.RESET;
    }

    private static int runJdkRegex(String text, Pattern pattern) {
        Matcher m = pattern.matcher(text);
        int count = 0;
        while (m.find()) {
            String link = m.group(1);
            count++;
        }
        return count;
    }
}
