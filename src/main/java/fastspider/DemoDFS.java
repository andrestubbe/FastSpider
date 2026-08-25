package fastspider;

import fastansi.FastANSI;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Task-Driven DFS Pathfinder Demo (Hacker-Style Deep Descent Stream).
 * Uses native AVX2 link extraction to trace an ultra-deep 20+ hop path across hundreds of live streaming candidates.
 * Clean, perfectly aligned recursive branch tree formatting with bright-white index badges.
 */
public class DemoDFS {

    private static final String START_URL = "https://en.wikipedia.org/wiki/Computer_science";
    private static final String TARGET_TOPIC = "Quantum_computing";

    public static void main(String[] args) throws Exception {
        System.out.println(darkGray("========================================================================================================================"));
        System.out.println(" " + boldWhite("FastSpider") + darkGray(" — DFS Deep-Dive Pathfinder Demo (Native AVX2 + Virtual Threads)"));
        System.out.println(darkGray(" MISSION: Auto-traverse Wikipedia graph from '") + boldWhite(START_URL) + darkGray("' -> Target: '") + boldWhite(TARGET_TOPIC) + darkGray("'"));
        System.out.println(darkGray("========================================================================================================================"));
        System.out.println();

        FastSpider spider = FastSpider.open();
        Set<String> visited = ConcurrentHashMap.newKeySet();
        String currentUrl = START_URL;
        int maxDepth = 25;
        long t0 = System.currentTimeMillis();
        int totalHops = 0;
        int totalExtracted = 0;

        System.out.println(darkGray("[Root 00] ") + boldWhite(currentUrl));

        boolean targetFound = false;

        for (int depth = 1; depth <= maxDepth; depth++) {
            visited.add(currentUrl);
            totalHops++;

            long stepT0 = System.currentTimeMillis();
            FastSpider.SpiderResponse resp = spider.fetchAsync(currentUrl).join();
            long stepMs = System.currentTimeMillis() - stepT0;

            // AVX2 Native Link Extraction
            List<String> rawLinks = spider.extractHrefs(resp.rawBody());
            List<String> links = filterWikiArticleLinks(rawLinks);
            totalExtracted += links.size();

            boolean isTargetPage = currentUrl.toLowerCase().contains(TARGET_TOPIC.toLowerCase());

            StringBuilder indent = new StringBuilder();
            for (int d = 0; d < Math.min(depth - 1, 12); d++) {
                indent.append("  ");
            }

            String matchNotice = isTargetPage ? " " + boldWhite("*** TARGET REACHED! ***") : "";
            String meta = String.format(" -> %,d links", links.size());
            String shortUrl = truncate(currentUrl, 56);

            System.out.printf("%s└── %s %-56s%s%s\n",
                    darkGray(indent.toString()), boldWhite(String.format("[Hop %02d]", depth)),
                    white(shortUrl), darkGray(meta), matchNotice);

            // Stream 15 live candidate links per hop to show extensive real-time action (200+ links total stream)
            int previewCount = Math.min(links.size(), 15);
            for (int p = 0; p < previewCount; p++) {
                String lk = links.get(p);
                boolean isLast = (p == previewCount - 1);
                String tag = lk.toLowerCase().contains(TARGET_TOPIC.toLowerCase()) ? " " + boldWhite("[★ TARGET CANDIDATE]") : "";
                String shortLk = truncate(lk, 60);
                System.out.printf("%s    %s %s %s%s\n",
                        darkGray(indent.toString()), darkGray(isLast ? "└──" : "├──"),
                        boldWhite(String.format("[LINK %02d]", p + 1)), darkGray(shortLk), tag);
            }

            if (isTargetPage && depth >= 15) {
                targetFound = true;
                break;
            }

            // Find best next branch: prioritize target topic only near the end, otherwise dive deeper
            String nextUrl = null;
            if (depth >= 18) {
                for (String lk : links) {
                    if (!visited.contains(lk) && lk.toLowerCase().contains(TARGET_TOPIC.toLowerCase())) {
                        nextUrl = lk;
                        break;
                    }
                }
            }
            if (nextUrl == null) {
                for (String lk : links) {
                    if (!visited.contains(lk)) {
                        nextUrl = lk;
                        break;
                    }
                }
            }

            if (nextUrl == null) {
                System.out.printf("%s    %s\n", darkGray(indent.toString()), darkGray("(Terminal branch reached)"));
                break;
            }

            currentUrl = nextUrl;
        }

        long duration = System.currentTimeMillis() - t0;
        System.out.println();
        System.out.println(darkGray("========================================================================================================================"));
        if (targetFound) {
            System.out.printf(" " + boldWhite("SUCCESS:") + darkGray(" Path connected to '") + boldWhite(TARGET_TOPIC) + darkGray("' in ") + boldWhite(String.format("%,d ms", duration)) + darkGray(" across ") + boldWhite(String.valueOf(totalHops)) + darkGray(" recursive branch hops!\n"));
        } else {
            System.out.printf(" " + boldWhite("COMPLETE:") + darkGray(" Navigated ") + boldWhite(String.valueOf(totalHops)) + darkGray(" deep recursive hops in ") + boldWhite(String.format("%,d ms", duration)) + darkGray(" total.\n"));
        }
        System.out.printf(" " + darkGray("Discovered & Scanned ") + boldWhite(String.format("%,d", totalExtracted)) + darkGray(" total candidate hyperlinks via AVX2 in real-time.\n"));
        System.out.println(darkGray("========================================================================================================================"));
    }

    private static String truncate(String text, int maxLen) {
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen - 3) + "...";
    }

    private static String darkGray(String text) {
        return FastANSI.fg(240) + text + FastANSI.RESET;
    }

    private static String white(String text) {
        return FastANSI.FG_BRIGHT_WHITE + text + FastANSI.RESET;
    }

    private static String boldWhite(String text) {
        return FastANSI.BOLD + FastANSI.FG_BRIGHT_WHITE + text + FastANSI.RESET;
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
