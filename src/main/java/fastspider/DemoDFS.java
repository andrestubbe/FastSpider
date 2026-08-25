package fastspider;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Task-Driven DFS Pathfinder Demo (Hacker-Style Deep Descent Stream).
 * Recursively follows live hyperlinks, streaming all candidate links
 * on every hop until reaching target concept domain.
 */
public class DemoDFS {

    private static final String START_URL = "https://en.wikipedia.org/wiki/Computer_science";
    private static final String TARGET_TERM = "Quantum";
    private static final Pattern HREF_PATTERN = Pattern.compile("href=\"/wiki/([^\"#:]+)\"");

    public static void main(String[] args) throws Exception {
        System.out.println("========================================================================================================================");
        System.out.println(" FastSpider — DFS Deep Dive Pathfinder Demo (High-Speed Recursive Navigation Stream)");
        System.out.println(" MISSION: Auto-navigate Wikipedia hyperlink graph from '" + START_URL + "' -> Domain containing '" + TARGET_TERM + "'");
        System.out.println("========================================================================================================================");
        System.out.println();

        FastSpider spider = FastSpider.open();
        Set<String> visited = ConcurrentHashMap.newKeySet();
        String currentUrl = START_URL;
        int maxDepth = 15;
        long t0 = System.currentTimeMillis();
        int totalHops = 0;
        int totalExtracted = 0;

        System.out.printf("[Start 00] %s\n", currentUrl);

        boolean targetFound = false;

        for (int depth = 1; depth <= maxDepth; depth++) {
            visited.add(currentUrl);
            totalHops++;

            long stepT0 = System.currentTimeMillis();
            FastSpider.SpiderResponse resp = spider.fetchAsync(currentUrl).join();
            long stepMs = System.currentTimeMillis() - stepT0;

            String htmlText = new String(resp.rawBody(), StandardCharsets.UTF_8);
            boolean containsTarget = htmlText.contains(TARGET_TERM);
            List<String> links = extractAllWikiLinks(htmlText);
            totalExtracted += links.size();

            StringBuilder indent = new StringBuilder();
            for (int d = 0; d < depth; d++) {
                indent.append("  ");
            }

            String matchNotice = containsTarget ? " *** TARGET CONCEPT DISCOVERED ('" + TARGET_TERM + "') ***" : "";
            System.out.printf("%s└── [Hop %02d] %-66s | HTTP %d | %,7d B | %3d ms | (%,d links)%s\n",
                    indent, depth, currentUrl, resp.statusCode(), resp.rawBody().length, stepMs, links.size(), matchNotice);

            // Stream candidate links out in real time
            int previewCount = Math.min(links.size(), 6);
            for (int p = 0; p < previewCount; p++) {
                String lk = links.get(p);
                boolean isLast = (p == previewCount - 1);
                String tag = lk.contains(TARGET_TERM) ? " [★ TARGET CANDIDATE]" : "";
                System.out.printf("%s    %s [CANDIDATE %02d] %s%s\n", indent, isLast ? "└──" : "├──", p + 1, lk, tag);
            }

            if (containsTarget && depth > 2) {
                targetFound = true;
                break;
            }

            // Find best next branch (prioritize links mentioning target or next unexplored link)
            String nextUrl = links.stream()
                    .filter(u -> !visited.contains(u) && u.contains(TARGET_TERM))
                    .findFirst()
                    .orElseGet(() -> links.stream().filter(u -> !visited.contains(u)).findFirst().orElse(null));

            if (nextUrl == null) {
                System.out.printf("%s    (Terminal branch reached - no further unexplored links)\n", indent);
                break;
            }

            currentUrl = nextUrl;
        }

        long duration = System.currentTimeMillis() - t0;
        System.out.println();
        System.out.println("========================================================================================================================");
        if (targetFound) {
            System.out.printf(" SUCCESS: Path connected to '%s' in %,d ms across %d recursive branch hops!\n", TARGET_TERM, duration, totalHops);
        } else {
            System.out.printf(" COMPLETE: Navigated %,d hops in %,d ms total.\n", totalHops, duration);
        }
        System.out.printf(" Scanned %,d total candidate hyperlinks in real-time.\n", totalExtracted);
        System.out.println("========================================================================================================================");
    }

    private static List<String> extractAllWikiLinks(String html) {
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
}
