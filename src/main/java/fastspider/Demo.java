package fastspider;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Hero Demo — live terminal crawler using real Wikipedia pages.
 * Showcases native WinHTTP fetching, Virtual Thread concurrency,
 * and AVX2 link extraction with animated ANSI output.
 */
public class Demo {

    private Demo() {}

    // ── ANSI helpers ────────────────────────────────────────────────────────
    private static final String R  = "\033[0m";
    private static final String B  = "\033[1m";
    private static final String CY = "\033[36m";
    private static final String GR = "\033[32m";
    private static final String YL = "\033[33m";
    private static final String MG = "\033[35m";
    private static final String RD = "\033[31m";
    private static final String DM = "\033[90m";   // dim grey
    private static final String ER = "\033[K";     // erase to end of line
    private static final String[] SPIN = {"⠋","⠙","⠹","⠸","⠼","⠴","⠦","⠧","⠇","⠏"};

    // ── Target URLs ─────────────────────────────────────────────────────────
    private static final List<String> URLS = List.of(
        "https://en.wikipedia.org/wiki/Java_(programming_language)",
        "https://en.wikipedia.org/wiki/C%2B%2B",
        "https://en.wikipedia.org/wiki/SIMD",
        "https://en.wikipedia.org/wiki/Web_crawler",
        "https://en.wikipedia.org/wiki/Just-in-time_compilation"
    );

    public static void main(String[] args) throws Exception {
        banner();

        FastSpider spider = FastSpider.open();

        // ── Phase 1: live sequential ticker ─────────────────────────────────
        header("Phase 1", "Sequential crawl — " + URLS.size() + " Wikipedia pages");
        System.out.println();

        List<FastSpider.SpiderResponse> sequential = new ArrayList<>();
        long seqTotal = 0;

        for (int i = 0; i < URLS.size(); i++) {
            String url  = URLS.get(i);
            String label = shortLabel(url);

            // pending line
            System.out.printf("\r  " + YL + "[%d/%d]" + R + " Fetching %-34s ..." + ER,
                    i + 1, URLS.size(), label);
            System.out.flush();

            long t0 = System.nanoTime();
            FastSpider.SpiderResponse resp = spider.fetchAsync(url).join();
            long ms = (System.nanoTime() - t0) / 1_000_000;
            seqTotal += ms;
            sequential.add(resp);

            String tick = resp.isSuccess() ? GR + "✔" + R : RD + "✘" + R;
            System.out.printf(
                "\r  " + YL + "[%d/%d]" + R + " %s %-34s  " + GR + "%3d" + R + "  " + CY + "%,5d ms" + R + "  " + DM + "%,6d B" + R + ER + "\n",
                i + 1, URLS.size(), tick, label, resp.statusCode(), ms, resp.rawBody().length);
        }
        System.out.println();

        // ── Phase 2: concurrent batch with spinner ───────────────────────────
        header("Phase 2", "Concurrent batch — same pages via Virtual Threads");
        System.out.println();

        AtomicInteger doneCount = new AtomicInteger(0);
        long batchStart = System.currentTimeMillis();

        List<CompletableFuture<FastSpider.SpiderResponse>> futures = new ArrayList<>();
        for (String url : URLS) {
            futures.add(spider.fetchAsync(url).whenComplete((r, t) -> doneCount.incrementAndGet()));
        }

        int s = 0;
        while (doneCount.get() < URLS.size()) {
            double elapsed = (System.currentTimeMillis() - batchStart) / 1000.0;
            System.out.printf("\r  " + CY + SPIN[s % SPIN.length] + R
                    + " Crawling...  " + GR + "%d" + R + "/" + URLS.size() + " pages done  " + DM + "%.1fs" + R + ER,
                    doneCount.get(), elapsed);
            System.out.flush();
            Thread.sleep(80);
            s++;
        }
        long batchMs = System.currentTimeMillis() - batchStart;
        double speedup = (double) seqTotal / Math.max(1, batchMs);
        System.out.printf("\r  " + GR + "✔ Batch complete!" + R
                + "  All %d pages in " + B + GR + "%d ms" + R
                + "  " + DM + "(%.1fx faster than sequential)" + R + ER + "\n",
                URLS.size(), batchMs, speedup);
        System.out.println();

        // ── Phase 3: link extraction ─────────────────────────────────────────
        header("Phase 3", "AVX2 link extraction — \"Java (programming language)\"");
        System.out.println();

        FastSpider.SpiderResponse javaPage = sequential.get(0);
        List<String> hrefs = spider.extractHrefs(javaPage.rawBody());
        int shown = Math.min(hrefs.size(), 10);
        for (int i = 0; i < shown; i++) {
            System.out.println("  " + CY + "→" + R + " " + hrefs.get(i));
        }
        if (hrefs.size() > shown) {
            System.out.println("  " + DM + "  ... and " + (hrefs.size() - shown) + " more links" + R);
        }
        System.out.println();

        // ── Footer ───────────────────────────────────────────────────────────
        footer("FastSpider crawled " + URLS.size() + " real Wikipedia pages via native WinHTTP + Virtual Threads.");
    }

    // ── Helpers ─────────────────────────────────────────────────────────────
    private static String shortLabel(String url) {
        return url.substring(url.lastIndexOf('/') + 1)
                  .replace("%2B", "+")
                  .replace("_", " ");
    }

    private static void banner() {
        System.out.println(MG + "═══════════════════════════════════════════════════════════════");
        System.out.println(B + CY + "  ⚡ FastSpider  —  Native WinHTTP + Virtual Thread Crawler" + R);
        System.out.println(MG + "═══════════════════════════════════════════════════════════════" + R);
        System.out.println();
    }

    private static void header(String phase, String msg) {
        System.out.println(YL + B + "[" + phase + "]" + R + " " + msg);
    }

    private static void footer(String msg) {
        System.out.println(MG + "═══════════════════════════════════════════════════════════════");
        System.out.println(B + GR + "  ✔ " + msg + R);
        System.out.println(MG + "═══════════════════════════════════════════════════════════════" + R);
    }
}
