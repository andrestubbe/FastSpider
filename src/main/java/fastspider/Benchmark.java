package fastspider;

import fastregex.FastRegex;
import fastregex.MatchResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Benchmark {

    public static void main(String[] args) throws Exception {
        System.out.println("==================================================================");
        System.out.println(" FastSpider & FastRegex Link Extraction Benchmark Suite");
        System.out.println("==================================================================");
        System.out.println();

        // 1. Fetch real HTML page
        System.out.print("Downloading Wikipedia target page for benchmark ... ");
        FastSpider spider = FastSpider.open();
        FastSpider.SpiderResponse response = spider.fetchAsync("https://en.wikipedia.org/wiki/Java_(programming_language)").join();
        byte[] htmlBytes = response.rawBody();
        String htmlText = new String(htmlBytes, StandardCharsets.UTF_8);
        System.out.printf("OK (%,d bytes, %d ms)\n\n", htmlBytes.length, response.fetchTimeMs());

        int warmup = 50;
        int iterations = 200;

        // 2. JDK Pattern Matcher link extraction
        Pattern hrefPattern = Pattern.compile("href=\"([^\"]+)\"");
        System.out.println("Running JDK java.util.regex.Pattern link extraction benchmark...");
        for (int i = 0; i < warmup; i++) {
            runJdkRegex(htmlText, hrefPattern);
        }
        long t0 = System.nanoTime();
        int totalJdkLinks = 0;
        for (int i = 0; i < iterations; i++) {
            totalJdkLinks += runJdkRegex(htmlText, hrefPattern);
        }
        long jdkNanos = System.nanoTime() - t0;
        double jdkOpsPerMs = (double) iterations / (jdkNanos / 1_000_000.0);
        double jdkAvgMs = (jdkNanos / 1_000_000.0) / iterations;

        // 3. FastSpider + FastRegex Zero-Allocation Scanner
        FastRegex fastRegex = FastRegex.compile("href=\"[^\"]+\"");
        MatchResult matchResult = new MatchResult();
        System.out.println("Running FastSpider native AVX2 + FastRegex zero-allocation extraction...");
        for (int i = 0; i < warmup; i++) {
            spider.extractHrefs(htmlBytes);
        }
        long t1 = System.nanoTime();
        int totalFastLinks = 0;
        for (int i = 0; i < iterations; i++) {
            totalFastLinks += spider.extractHrefs(htmlBytes).size();
        }
        long fastNanos = System.nanoTime() - t1;
        double fastOpsPerMs = (double) iterations / (fastNanos / 1_000_000.0);
        double fastAvgMs = (fastNanos / 1_000_000.0) / iterations;

        double speedup = (double) jdkNanos / fastNanos;

        System.out.println();
        System.out.println("-----------------------------------------------------------------------------------------");
        System.out.printf(" %-40s | %-16s | %-16s | %-10s\n", "Engine / Operation", "Throughput (ops/ms)", "Avg Latency (ms)", "Speedup");
        System.out.println("-----------------------------------------------------------------------------------------");
        System.out.printf(" %-40s | %16.2f | %14.3f ms | 1.00x\n", "JDK Pattern.matcher(\"href=...\")", jdkOpsPerMs, jdkAvgMs);
        System.out.printf(" %-40s | %16.2f | %14.3f ms | %.2fx Faster\n", "FastSpider AVX2 + FastRegex", fastOpsPerMs, fastAvgMs, speedup);
        System.out.println("-----------------------------------------------------------------------------------------");
        System.out.printf(" Extracted %,d links per iteration across %,d iterations.\n", totalFastLinks / iterations, iterations);
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
