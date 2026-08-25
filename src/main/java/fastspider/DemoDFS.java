package fastspider;

import fastansi.FastANSI;

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
 * Uses FastANSI subtle Gray & White highlighting with dedicated telemetry lines to prevent word-wrap.
 */
public class DemoDFS {

    private static final String START_URL = "https://en.wikipedia.org/wiki/Computer_science";
    private static final String TARGET_TERM = "Quantum";
    private static final Pattern HREF_PATTERN = Pattern.compile("href=\"/wiki/([^\"#:]+)\"");

    public static void main(String[] args) throws Exception {
        System.out.println(gray("========================================================================================================================"));
        System.out.println(" " + white("FastSpider") + gray(" — DFS Deep Dive Pathfinder Demo (High-Speed Recursive Navigation Stream)"));
        System.out.println(gray(" MISSION: Auto-navigate Wikipedia hyperlink graph from '") + white(START_URL) + gray("' -> Domain containing '") + white(TARGET_TERM) + gray("'"));
        System.out.println(gray("========================================================================================================================"));
        System.out.println();

        FastSpider spider = FastSpider.open();
        Set<String> visited = ConcurrentHashMap.newKeySet();
        String currentUrl = START_URL;
        int maxDepth = 15;
        long t0 = System.currentTimeMillis();
        int totalHops = 0;
        int totalExtracted = 0;

        System.out.println(gray("[Start 00] ") + white(currentUrl));

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

            String matchNotice = containsTarget ? " " + white("*** TARGET CONCEPT DISCOVERED ('" + TARGET_TERM + "') ***") : "";
            System.out.printf("%s%s %s %s\n",
                    gray(indent.toString()), gray("└──"), gray(String.format("[Hop %02d]", depth)), white(currentUrl));
            System.out.printf("%s    %s HTTP %d %s %,7d B %s %3d ms %s (%,d links)%s\n",
                    gray(indent.toString()), gray("STATUS:"), resp.statusCode(), gray("|"),
                    resp.rawBody().length, gray("|"), stepMs, gray("|"), links.size(), matchNotice);

            // Stream candidate links out in real time
            int previewCount = Math.min(links.size(), 6);
            for (int p = 0; p < previewCount; p++) {
                String lk = links.get(p);
                boolean isLast = (p == previewCount - 1);
                String tag = lk.contains(TARGET_TERM) ? " " + white("[★ TARGET CANDIDATE]") : "";
                System.out.printf("%s    %s %s %s%s\n",
                        gray(indent.toString()), gray(isLast ? "└──" : "├──"),
                        gray(String.format("[CANDIDATE %02d]", p + 1)), gray(lk), tag);
            }

            if (containsTarget && depth > 2) {
                targetFound = true;
                break;
            }

            // Find best next branch
            String nextUrl = links.stream()
                    .filter(u -> !visited.contains(u) && u.contains(TARGET_TERM))
                    .findFirst()
                    .orElseGet(() -> links.stream().filter(u -> !visited.contains(u)).findFirst().orElse(null));

            if (nextUrl == null) {
                System.out.printf("%s    %s\n", gray(indent.toString()), gray("(Terminal branch reached - no further unexplored links)"));
                break;
            }

            currentUrl = nextUrl;
        }

        long duration = System.currentTimeMillis() - t0;
        System.out.println();
        System.out.println(gray("========================================================================================================================"));
        if (targetFound) {
            System.out.printf(" " + white("SUCCESS:") + " Path connected to '" + white(TARGET_TERM) + "' in " + white("%,d ms") + " across " + white("%d") + " recursive branch hops!\n", duration, totalHops);
        } else {
            System.out.printf(" " + white("COMPLETE:") + " Navigated " + white("%d") + " hops in " + white("%,d ms") + " total.\n", totalHops, duration);
        }
        System.out.printf(" Scanned " + white("%,d") + " total candidate hyperlinks in real-time.\n", totalExtracted);
        System.out.println(gray("========================================================================================================================"));
    }

    private static String gray(String text) {
        return FastANSI.FG_BRIGHT_BLACK + text + FastANSI.RESET;
    }

    private static String white(String text) {
        return FastANSI.FG_BRIGHT_WHITE + text + FastANSI.RESET;
    }

    private static List<String> extractAllWikiLinks(String html) {
        List<String> links = new ArrayList<>();
        Matcher matcher = HREF_PATTERN.matcher(html);
        while (matcher.find()) {
            String path = matcher.group(1);
            if (!path.equals("Main_Page") && !path.contains(":") && !path.contains("?")) {
                links.add("https://en.wikipedia.org/wiki/" + path);
            }
        }
        return links;
    }
}
