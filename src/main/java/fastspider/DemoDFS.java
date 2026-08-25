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
 * Uses FastANSI dark-gray (index 240) framing and bold bright-white highlights.
 */
public class DemoDFS {

    private static final String START_URL = "https://en.wikipedia.org/wiki/Computer_science";
    private static final String TARGET_TERM = "Quantum";
    private static final Pattern HREF_PATTERN = Pattern.compile("href=\"/wiki/([^\"#:]+)\"");

    public static void main(String[] args) throws Exception {
        System.out.println(darkGray("========================================================================================================================"));
        System.out.println(" " + boldWhite("FastSpider") + darkGray(" — DFS Deep Dive Pathfinder Demo (High-Speed Recursive Navigation Stream)"));
        System.out.println(darkGray(" MISSION: Auto-navigate Wikipedia hyperlink graph from '") + boldWhite(START_URL) + darkGray("' -> Domain containing '") + boldWhite(TARGET_TERM) + darkGray("'"));
        System.out.println(darkGray("========================================================================================================================"));
        System.out.println();

        FastSpider spider = FastSpider.open();
        Set<String> visited = ConcurrentHashMap.newKeySet();
        String currentUrl = START_URL;
        int maxDepth = 15;
        long t0 = System.currentTimeMillis();
        int totalHops = 0;
        int totalExtracted = 0;

        System.out.println(darkGray("[Start 00] ") + boldWhite(currentUrl));

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

            String matchNotice = containsTarget ? " " + boldWhite("*** TARGET CONCEPT DISCOVERED ('" + TARGET_TERM + "') ***") : "";
            System.out.printf("%s%s %s %s\n",
                    darkGray(indent.toString()), darkGray("└──"), darkGray(String.format("[Hop %02d]", depth)), white(currentUrl));
            System.out.printf("%s    %s HTTP %s %s %s B %s %s ms %s (%s links)%s\n",
                    darkGray(indent.toString()), darkGray("STATUS:"), boldWhite(String.valueOf(resp.statusCode())),
                    darkGray("|"), boldWhite(String.format("%,d", resp.rawBody().length)),
                    darkGray("|"), boldWhite(String.format("%3d", stepMs)),
                    darkGray("|"), boldWhite(String.format("%,d", links.size())), matchNotice);

            // Stream candidate links out in real time
            int previewCount = Math.min(links.size(), 6);
            for (int p = 0; p < previewCount; p++) {
                String lk = links.get(p);
                boolean isLast = (p == previewCount - 1);
                String tag = lk.contains(TARGET_TERM) ? " " + boldWhite("[★ TARGET CANDIDATE]") : "";
                System.out.printf("%s    %s %s %s%s\n",
                        darkGray(indent.toString()), darkGray(isLast ? "└──" : "├──"),
                        darkGray(String.format("[CANDIDATE %02d]", p + 1)), darkGray(lk), tag);
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
                System.out.printf("%s    %s\n", darkGray(indent.toString()), darkGray("(Terminal branch reached - no further unexplored links)"));
                break;
            }

            currentUrl = nextUrl;
        }

        long duration = System.currentTimeMillis() - t0;
        System.out.println();
        System.out.println(darkGray("========================================================================================================================"));
        if (targetFound) {
            System.out.printf(" " + boldWhite("SUCCESS:") + darkGray(" Path connected to '") + boldWhite(TARGET_TERM) + darkGray("' in ") + boldWhite(String.format("%,d ms", duration)) + darkGray(" across ") + boldWhite(String.valueOf(totalHops)) + darkGray(" recursive branch hops!\n"));
        } else {
            System.out.printf(" " + boldWhite("COMPLETE:") + darkGray(" Navigated ") + boldWhite(String.valueOf(totalHops)) + darkGray(" hops in ") + boldWhite(String.format("%,d ms", duration)) + darkGray(" total.\n"));
        }
        System.out.printf(" " + darkGray("Scanned ") + boldWhite(String.format("%,d", totalExtracted)) + darkGray(" total candidate hyperlinks in real-time.\n"));
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
