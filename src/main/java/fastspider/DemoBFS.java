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
 * High-Speed Visual BFS Tree Crawler Demo (Hacker-Style Stream).
 * Rushes through thousands of discovered links in real-time, displaying
 * live tree branches, SIMD keyword matches, and full telemetry.
 */
public class DemoBFS {

    private static final String ROOT_URL = "https://en.wikipedia.org/wiki/Computer_science";
    private static final String SEARCH_KEYWORD = "SIMD";
    private static final Pattern HREF_PATTERN = Pattern.compile("href=\"/wiki/([^\"#:]+)\"");

    public static void main(String[] args) throws Exception {
        System.out.println("========================================================================================================================");
        System.out.println(" FastSpider — Massive BFS Real-Time Tree & Stream Crawler (WinHTTP + Virtual Threads)");
        System.out.println(" MISSION: Live scan of Wikipedia network graph discovering 5,000+ branch links in seconds");
        System.out.println("========================================================================================================================");
        System.out.println();

        FastSpider spider = FastSpider.open();
        Set<String> visited = ConcurrentHashMap.newKeySet();
        AtomicInteger totalCrawled = new AtomicInteger(0);
        AtomicLong totalBytes = new AtomicLong(0);
        AtomicInteger simdMatches = new AtomicInteger(0);
        AtomicInteger totalDiscoveredLinks = new AtomicInteger(0);

        long t0 = System.currentTimeMillis();

        // 1. Root Fetch
        visited.add(ROOT_URL);
        FastSpider.SpiderResponse rootResp = spider.fetchAsync(ROOT_URL).join();
        totalCrawled.incrementAndGet();
        totalBytes.addAndGet(rootResp.rawBody().length);

        System.out.printf("[Root 00] %s (HTTP %d | %,d Bytes in %d ms)\n",
                ROOT_URL, rootResp.statusCode(), rootResp.rawBody().length, rootResp.fetchTimeMs());

        List<String> rootLinks = extractAllWikiLinks(rootResp.rawBody());
        totalDiscoveredLinks.addAndGet(rootLinks.size());
        List<String> level1Links = rootLinks.stream().distinct().filter(u -> !visited.contains(u)).limit(40).toList();

        System.out.printf("  │\n  ├── Discovered %,d article links on Root. Launching Level 1 Parallel Crawl (%d articles)...\n  │\n",
                rootLinks.size(), level1Links.size());

        // 2. Fetch Level 1 concurrently
        List<CompletableFuture<BFSNode>> level1Futures = new ArrayList<>();
        for (int i = 0; i < level1Links.size(); i++) {
            String url = level1Links.get(i);
            visited.add(url);
            boolean isLast = (i == level1Links.size() - 1);

            level1Futures.add(spider.fetchAsync(url).thenApply(res -> {
                totalCrawled.incrementAndGet();
                totalBytes.addAndGet(res.rawBody().length);
                int kwHits = countKeywordHits(res.rawBody(), SEARCH_KEYWORD);
                if (kwHits > 0) simdMatches.incrementAndGet();
                List<String> childs = extractAllWikiLinks(res.rawBody());
                totalDiscoveredLinks.addAndGet(childs.size());
                return new BFSNode(url, res, childs, kwHits, isLast);
            }));
        }

        List<BFSNode> level1Nodes = level1Futures.stream().map(CompletableFuture::join).toList();

        // 3. Render Level 1 & Stream Out Level 2 Sub-Branches with Hacker-Style Link Torrent
        List<CompletableFuture<Void>> level2Futures = new ArrayList<>();
        for (int i = 0; i < level1Nodes.size(); i++) {
            BFSNode node = level1Nodes.get(i);
            String branch = node.isLast ? "└──" : "├──";
            String subIndent = node.isLast ? "   " : "│  ";
            String tag = node.keywordHits > 0 ? " [MATCH: " + node.keywordHits + "x SIMD]" : "";

            System.out.printf("  %s [%02d] %-68s | HTTP %d | %,7d B | %3d ms | %,d links%s\n",
                    branch, i + 1, node.url, node.response.statusCode(),
                    node.response.rawBody().length, node.response.fetchTimeMs(), node.childLinks.size(), tag);

            // Stream 5 live discovered sub-links directly to terminal for intense visual flow
            int streamPreview = Math.min(node.childLinks.size(), 4);
            for (int s = 0; s < streamPreview; s++) {
                String lk = node.childLinks.get(s);
                boolean isStreamLast = (s == streamPreview - 1);
                String leaf = isStreamLast ? "└──" : "├──";
                System.out.printf("  %s  %s [LIVE LINK] -> %s\n", subIndent, leaf, lk);
            }

            // Pick 3 sub-links per level 1 node for actual parallel background download
            List<String> subLinks = node.childLinks.stream()
                    .filter(u -> !visited.contains(u))
                    .limit(3)
                    .toList();

            for (int k = 0; k < subLinks.size(); k++) {
                String subUrl = subLinks.get(k);
                visited.add(subUrl);

                level2Futures.add(spider.fetchAsync(subUrl).thenAccept(subRes -> {
                    totalCrawled.incrementAndGet();
                    totalBytes.addAndGet(subRes.rawBody().length);
                    int kwSubHits = countKeywordHits(subRes.rawBody(), SEARCH_KEYWORD);
                    String subTag = kwSubHits > 0 ? " [MATCH: " + kwSubHits + "x SIMD]" : "";
                    if (kwSubHits > 0) simdMatches.incrementAndGet();

                    List<String> subChilds = extractAllWikiLinks(subRes.rawBody());
                    totalDiscoveredLinks.addAndGet(subChilds.size());

                    System.out.printf("  %s  ├── [SUB-PAGE %03d] %-60s | HTTP %d | %,7d B | %3d ms | %,d links%s\n",
                            subIndent, totalCrawled.get(), subUrl, subRes.statusCode(),
                            subRes.rawBody().length, subRes.fetchTimeMs(), subChilds.size(), subTag);
                }));
            }
        }

        CompletableFuture.allOf(level2Futures.toArray(new CompletableFuture[0])).join();
        long duration = System.currentTimeMillis() - t0;

        System.out.println();
        System.out.println("========================================================================================================================");
        System.out.printf(" MISSION COMPLETE: Crawled %,d live pages (%,.2f MB) in %,d ms (%,.1f pages/sec)\n",
                totalCrawled.get(), (totalBytes.get() / (1024.0 * 1024.0)), duration,
                (totalCrawled.get() / (duration / 1000.0)));
        System.out.printf(" Discovered %,d total hyperlinks across Wikipedia graph in real time!\n", totalDiscoveredLinks.get());
        System.out.printf(" Found %,d pages with explicit 'SIMD' hardware vectorization references.\n", simdMatches.get());
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

    private static List<String> extractAllWikiLinks(byte[] htmlBytes) {
        String html = new String(htmlBytes, StandardCharsets.UTF_8);
        List<String> links = new ArrayList<>();
        Matcher matcher = HREF_PATTERN.matcher(html);
        while (matcher.find()) {
            String path = matcher.group(1);
            if (!path.equals("Main_Page")) {
                links.add("https://en.wikipedia.org/wiki/" + path);
            }
        }
        return links;
    }

    private static class BFSNode {
        final String url;
        final FastSpider.SpiderResponse response;
        final List<String> childLinks;
        final int keywordHits;
        final boolean isLast;

        BFSNode(String url, FastSpider.SpiderResponse response, List<String> childLinks, int keywordHits, boolean isLast) {
            this.url = url;
            this.response = response;
            this.childLinks = childLinks;
            this.keywordHits = keywordHits;
            this.isLast = isLast;
        }
    }
}
