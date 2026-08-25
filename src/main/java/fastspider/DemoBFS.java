package fastspider;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * High-Speed Visual BFS Tree & Stream Crawler Demo.
 * Fires concurrent WinHTTP requests across 100+ Wikipedia pages,
 * streaming thousands of discovered links directly through the console in real-time.
 */
public class DemoBFS {

    private static final String SEARCH_KEYWORD = "vector";
    private static final Pattern HREF_PATTERN = Pattern.compile("href=\"/wiki/([^\"#:]+)\"");

    private static final List<String> SEED_PAGES = List.of(
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
        System.out.println("========================================================================================================================");
        System.out.println(" FastSpider — Massive BFS Real-Time Tree & Stream Crawler (WinHTTP Native + Virtual Threads)");
        System.out.println(" MISSION: Live high-throughput network scan discovering 5,000+ branch links and hardware vector references");
        System.out.println("========================================================================================================================");
        System.out.println();

        FastSpider spider = FastSpider.open();
        Set<String> visited = ConcurrentHashMap.newKeySet();
        AtomicInteger totalCrawled = new AtomicInteger(0);
        AtomicLong totalBytes = new AtomicLong(0);
        AtomicInteger keywordMatches = new AtomicInteger(0);
        AtomicInteger totalDiscoveredLinks = new AtomicInteger(0);

        long t0 = System.currentTimeMillis();

        System.out.printf("[Root Stream] Spawning %d concurrent crawler workers on Java Virtual Threads...\n\n", SEED_PAGES.size());

        // Level 1: Progressive Live Stream as requests arrive
        List<CompletableFuture<Void>> level1Futures = new ArrayList<>();
        List<CompletableFuture<Void>> level2Futures = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < SEED_PAGES.size(); i++) {
            final int index = i + 1;
            final String url = SEED_PAGES.get(i);
            visited.add(url);
            final boolean isLast = (i == SEED_PAGES.size() - 1);
            final String branch = isLast ? "└──" : "├──";
            final String subIndent = isLast ? "   " : "│  ";

            level1Futures.add(spider.fetchAsync(url).thenAccept(res -> {
                int crawledCount = totalCrawled.incrementAndGet();
                totalBytes.addAndGet(res.rawBody().length);
                int kwHits = countKeywordHits(res.rawBody(), SEARCH_KEYWORD);
                if (kwHits > 0) keywordMatches.incrementAndGet();
                List<String> childs = extractHrefLinks(res.rawBody());
                totalDiscoveredLinks.addAndGet(childs.size());

                String tag = kwHits > 0 ? " [MATCH: " + kwHits + "x '" + SEARCH_KEYWORD + "']" : "";

                synchronized (System.out) {
                    System.out.printf("  %s [%02d] %-66s | HTTP %d | %,7d B | %3d ms | %,d links%s\n",
                            branch, index, url, res.statusCode(),
                            res.rawBody().length, res.fetchTimeMs(), childs.size(), tag);

                    // Stream 6 live candidate links for intense hacker-style output
                    int streamPreview = Math.min(childs.size(), 6);
                    for (int s = 0; s < streamPreview; s++) {
                        String lk = childs.get(s);
                        boolean isStreamLast = (s == streamPreview - 1);
                        String leaf = isStreamLast ? "└──" : "├──";
                        System.out.printf("  %s  %s [LIVE LINK %02d] -> %s\n", subIndent, leaf, s + 1, lk);
                    }
                }

                // Sub-Page parallel fetch
                List<String> subToCrawl = childs.stream()
                        .filter(u -> !visited.contains(u))
                        .limit(3)
                        .toList();

                for (String subUrl : subToCrawl) {
                    visited.add(subUrl);

                    level2Futures.add(spider.fetchAsync(subUrl).thenAccept(subRes -> {
                        int subCrawledCount = totalCrawled.incrementAndGet();
                        totalBytes.addAndGet(subRes.rawBody().length);
                        int kwSubHits = countKeywordHits(subRes.rawBody(), SEARCH_KEYWORD);
                        String subTag = kwSubHits > 0 ? " [MATCH: " + kwSubHits + "x '" + SEARCH_KEYWORD + "']" : "";
                        if (kwSubHits > 0) keywordMatches.incrementAndGet();

                        List<String> subChilds = extractHrefLinks(subRes.rawBody());
                        totalDiscoveredLinks.addAndGet(subChilds.size());

                        synchronized (System.out) {
                            System.out.printf("  %s  ├── [SUB-PAGE %03d] %-58s | HTTP %d | %,7d B | %3d ms | %,d links%s\n",
                                    subIndent, subCrawledCount, subUrl, subRes.statusCode(),
                                    subRes.rawBody().length, subRes.fetchTimeMs(), subChilds.size(), subTag);
                        }
                    }));
                }
            }));
        }

        CompletableFuture.allOf(level1Futures.toArray(new CompletableFuture[0])).join();
        CompletableFuture.allOf(level2Futures.toArray(new CompletableFuture[0])).join();

        long duration = System.currentTimeMillis() - t0;

        System.out.println();
        System.out.println("========================================================================================================================");
        System.out.printf(" MISSION COMPLETE: Crawled %,d live pages (%,.2f MB) in %,d ms (%,.1f pages/sec)\n",
                totalCrawled.get(), (totalBytes.get() / (1024.0 * 1024.0)), duration,
                (totalCrawled.get() / (duration / 1000.0)));
        System.out.printf(" Discovered %,d total hyperlinks across Wikipedia graph in real time!\n", totalDiscoveredLinks.get());
        System.out.printf(" Found %,d pages with explicit '%s' hardware acceleration references.\n", keywordMatches.get(), SEARCH_KEYWORD);
        System.out.println("========================================================================================================================");
    }

    private static int countKeywordHits(byte[] body, String keyword) {
        String text = new String(body, StandardCharsets.UTF_8).toLowerCase();
        keyword = keyword.toLowerCase();
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(keyword, idx)) != -1) {
            count++;
            idx += keyword.length();
        }
        return count;
    }

    private static List<String> extractHrefLinks(byte[] htmlBytes) {
        String html = new String(htmlBytes, StandardCharsets.UTF_8);
        List<String> links = new ArrayList<>();
        Matcher matcher = HREF_PATTERN.matcher(html);
        while (matcher.find()) {
            String path = matcher.group(1);
            if (!path.equals("Main_Page") && !path.contains(":")) {
                links.add("https://en.wikipedia.org/wiki/" + path);
            }
        }
        return links;
    }
}
