package fastspider;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Compact Base Demo — Validates native WinHTTP fetch, Virtual Thread concurrency,
 * and AVX2 link extraction with clean neutral output.
 */
public class Demo {

    private Demo() {}

    private static final List<String> URLS = List.of(
        "https://en.wikipedia.org/wiki/Java_(programming_language)",
        "https://en.wikipedia.org/wiki/C%2B%2B",
        "https://en.wikipedia.org/wiki/SIMD",
        "https://en.wikipedia.org/wiki/Web_crawler",
        "https://en.wikipedia.org/wiki/Just-in-time_compilation"
    );

    public static void main(String[] args) throws Exception {
        System.out.println("------------------------------------------------------------------");
        System.out.println(" FastSpider — Native WinHTTP + Virtual Thread Crawler Benchmark");
        System.out.println("------------------------------------------------------------------");
        System.out.println();

        FastSpider spider = FastSpider.open();

        // ── Phase 1: Sequential Fetch ───────────────────────────────────────
        System.out.println("[Phase 1] Sequential Crawl (" + URLS.size() + " pages):");
        System.out.println();

        List<FastSpider.SpiderResponse> sequential = new ArrayList<>();
        long seqTotal = 0;

        for (int i = 0; i < URLS.size(); i++) {
            String url  = URLS.get(i);
            String label = shortLabel(url);

            long t0 = System.nanoTime();
            FastSpider.SpiderResponse resp = spider.fetchAsync(url).join();
            long ms = (System.nanoTime() - t0) / 1_000_000;
            seqTotal += ms;
            sequential.add(resp);

            String status = resp.isSuccess() ? "OK" : "ERR";
            System.out.printf("  [%d/%d] [%s] %-30s | HTTP %3d | %,5d ms | %,7d Bytes\n",
                    i + 1, URLS.size(), status, label, resp.statusCode(), ms, resp.rawBody().length);
        }
        System.out.printf("  Total sequential time: %,d ms\n", seqTotal);
        System.out.println();

        // ── Phase 2: Concurrent Virtual Thread Batch ─────────────────────────
        System.out.println("[Phase 2] Concurrent Batch (Virtual Threads):");
        System.out.println();

        AtomicInteger doneCount = new AtomicInteger(0);
        long batchStart = System.currentTimeMillis();

        List<CompletableFuture<FastSpider.SpiderResponse>> futures = new ArrayList<>();
        for (String url : URLS) {
            futures.add(spider.fetchAsync(url).whenComplete((r, t) -> doneCount.incrementAndGet()));
        }

        for (var f : futures) {
            f.join();
        }

        long batchMs = System.currentTimeMillis() - batchStart;
        double speedup = (double) seqTotal / Math.max(1, batchMs);
        System.out.printf("  Completed all %d pages in %,d ms (%.2fx speedup vs sequential)\n",
                URLS.size(), batchMs, speedup);
        System.out.println();

        // ── Phase 3: AVX2 Link Extraction ────────────────────────────────────
        System.out.println("[Phase 3] AVX2 Link Extraction (\"Java (programming language)\"):");
        System.out.println();

        FastSpider.SpiderResponse javaPage = sequential.get(0);
        long parseT0 = System.nanoTime();
        List<String> hrefs = spider.extractHrefs(javaPage.rawBody());
        long parseUs = (System.nanoTime() - parseT0) / 1_000;

        int shown = Math.min(hrefs.size(), 8);
        for (int i = 0; i < shown; i++) {
            System.out.println("  -> " + hrefs.get(i));
        }
        if (hrefs.size() > shown) {
            System.out.println("     ... and " + (hrefs.size() - shown) + " more links extracted in " + parseUs + " µs");
        }
        System.out.println();
        System.out.println("------------------------------------------------------------------");
        System.out.printf(" Summary: Crawled %d pages, extracted %,d links in %,d ms total.\n",
                URLS.size(), hrefs.size(), (seqTotal + batchMs));
        System.out.println("------------------------------------------------------------------");
    }

    private static String shortLabel(String url) {
        return url.substring(url.lastIndexOf('/') + 1)
                  .replace("%2B", "+")
                  .replace("_", " ");
    }
}
