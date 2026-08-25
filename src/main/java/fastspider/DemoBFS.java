package fastspider;

import fastansi.FastANSI;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * High-Speed Visual BFS Tree & Stream Crawler Demo.
 * Uses FastANSI dark-gray (index 240) framing and ultra-crisp bold bright-white highlights for maximum contrast.
 */
public class DemoBFS {

    private static final String SEARCH_KEYWORD = "vector";

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
        System.out.println(darkGray("========================================================================================================================"));
        System.out.println(" " + boldWhite("FastSpider") + darkGray(" — Massive BFS Real-Time Tree & Stream Crawler (WinHTTP Native + Virtual Threads)"));
        System.out.println(darkGray(" MISSION: Live high-throughput network scan discovering 10,000+ branch links and hardware vector references"));
        System.out.println(darkGray("========================================================================================================================"));
        System.out.println();

        System.out.println(darkGray("[Initial Crawl Queue] Loaded ") + boldWhite(String.valueOf(SEED_PAGES.size())) + darkGray(" root architecture & language nodes:"));
        for (int i = 0; i < SEED_PAGES.size(); i++) {
            boolean isLast = (i == SEED_PAGES.size() - 1);
            String branch = isLast ? "└──" : "├──";
            System.out.printf("  %s %s %s\n", darkGray(branch), darkGray(String.format("[%02d]", i + 1)), white(SEED_PAGES.get(i)));
        }
        System.out.println();
        System.out.println(darkGray("[Root Stream] Spawning ") + boldWhite(String.valueOf(SEED_PAGES.size())) + darkGray(" concurrent crawler workers on Java Virtual Threads...\n"));

        FastSpider spider = FastSpider.open();
        Set<String> visited = ConcurrentHashMap.newKeySet();
        AtomicInteger totalCrawled = new AtomicInteger(0);
        AtomicLong totalBytes = new AtomicLong(0);
        AtomicInteger keywordMatches = new AtomicInteger(0);
        AtomicInteger totalDiscoveredLinks = new AtomicInteger(0);

        long t0 = System.currentTimeMillis();

        // Level 1: Progressive Live Stream
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
                
                // Native AVX2 Link Extraction
                List<String> rawHrefs = spider.extractHrefs(res.rawBody());
                List<String> childs = filterWikiArticleLinks(rawHrefs);
                totalDiscoveredLinks.addAndGet(childs.size());

                String tag = kwHits > 0 ? " " + boldWhite("[MATCH: " + kwHits + "x '" + SEARCH_KEYWORD + "']") : "";

                synchronized (System.out) {
                    System.out.printf("  %s %s %s\n",
                            darkGray(branch), darkGray(String.format("[%02d]", index)), white(url));
                    System.out.printf("  %s    %s HTTP %s %s %s B %s %s ms %s %s links%s\n",
                            darkGray(subIndent), darkGray("STATUS:"), boldWhite(String.valueOf(res.statusCode())),
                            darkGray("|"), boldWhite(String.format("%,d", res.rawBody().length)),
                            darkGray("|"), boldWhite(String.format("%3d", res.fetchTimeMs())),
                            darkGray("|"), boldWhite(String.format("%,d", childs.size())), tag);

                    // Stream 8 distinct live candidate links for intense visual flow
                    int streamPreview = Math.min(childs.size(), 8);
                    for (int s = 0; s < streamPreview; s++) {
                        String lk = childs.get(s);
                        boolean isStreamLast = (s == streamPreview - 1);
                        String leaf = isStreamLast ? "└──" : "├──";
                        System.out.printf("  %s  %s %s -> %s\n",
                                darkGray(subIndent), darkGray(leaf), darkGray(String.format("[LIVE LINK %02d]", s + 1)), darkGray(lk));
                    }
                }

                // Sub-Page parallel fetch (pick 3 distinct sub-links to crawl)
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
                        String subTag = kwSubHits > 0 ? " " + boldWhite("[MATCH: " + kwSubHits + "x '" + SEARCH_KEYWORD + "']") : "";
                        if (kwSubHits > 0) keywordMatches.incrementAndGet();

                        List<String> subRaw = spider.extractHrefs(subRes.rawBody());
                        List<String> subChilds = filterWikiArticleLinks(subRaw);
                        totalDiscoveredLinks.addAndGet(subChilds.size());

                        synchronized (System.out) {
                            System.out.printf("  %s  ├── %s %s\n",
                                    darkGray(subIndent), darkGray(String.format("[SUB-PAGE %03d]", subCrawledCount)), white(subUrl));
                            System.out.printf("  %s  │    %s HTTP %s %s %s B %s %s ms %s %s links%s\n",
                                    darkGray(subIndent), darkGray("STATUS:"), boldWhite(String.valueOf(subRes.statusCode())),
                                    darkGray("|"), boldWhite(String.format("%,d", subRes.rawBody().length)),
                                    darkGray("|"), boldWhite(String.format("%3d", subRes.fetchTimeMs())),
                                    darkGray("|"), boldWhite(String.format("%,d", subChilds.size())), subTag);
                        }
                    }));
                }
            }));
        }

        CompletableFuture.allOf(level1Futures.toArray(new CompletableFuture[0])).join();
        CompletableFuture.allOf(level2Futures.toArray(new CompletableFuture[0])).join();

        long duration = System.currentTimeMillis() - t0;

        System.out.println();
        System.out.println(darkGray("========================================================================================================================"));
        System.out.printf(" " + boldWhite("MISSION COMPLETE:") + darkGray(" Crawled ") + boldWhite(String.format("%,d", totalCrawled.get())) + darkGray(" live pages (") + boldWhite(String.format("%,.2f MB", (totalBytes.get() / (1024.0 * 1024.0)))) + darkGray(") in ") + boldWhite(String.format("%,d ms", duration)) + darkGray(" (") + boldWhite(String.format("%,.1f pages/sec", (totalCrawled.get() / (duration / 1000.0)))) + darkGray(")\n"));
        System.out.printf(" " + darkGray("Discovered ") + boldWhite(String.format("%,d", totalDiscoveredLinks.get())) + darkGray(" total hyperlinks across Wikipedia graph in real time!\n"));
        System.out.printf(" " + darkGray("Found ") + boldWhite(String.format("%,d", keywordMatches.get())) + darkGray(" pages with explicit '") + boldWhite(SEARCH_KEYWORD) + darkGray("' hardware acceleration references.\n"));
        System.out.println(darkGray("========================================================================================================================"));
    }

    private static String darkGray(String text) {
        return FastANSI.fg(240) + text + FastANSI.RESET;
    }

    private static String white(String text) {
        return FastANSI.FG_WHITE + text + FastANSI.RESET;
    }

    private static String boldWhite(String text) {
        return FastANSI.BOLD + FastANSI.FG_BRIGHT_WHITE + text + FastANSI.RESET;
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
        return links.stream().distinct().toList();
    }
}
