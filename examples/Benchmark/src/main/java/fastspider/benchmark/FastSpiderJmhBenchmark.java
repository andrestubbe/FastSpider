package fastspider.benchmark;

import fastspider.FastSpider;
import org.openjdk.jmh.annotations.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Standard JMH Benchmark Suite for FastSpider Native AVX2 Link Extraction vs JDK Pattern.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
public class FastSpiderJmhBenchmark {

    private static final Pattern HREF_PATTERN = Pattern.compile("href=\"([^\"]+)\"");

    private FastSpider spider;
    private byte[] sampleHtmlBytes;
    private String sampleHtmlText;

    @Setup
    public void setup() {
        spider = FastSpider.open();
        try {
            FastSpider.SpiderResponse response = spider.fetchAsync("https://en.wikipedia.org/wiki/Java_(programming_language)").join();
            if (response.isSuccess() && response.rawBody().length > 0) {
                sampleHtmlBytes = response.rawBody();
            } else {
                sampleHtmlBytes = buildSyntheticHtml();
            }
        } catch (Exception e) {
            sampleHtmlBytes = buildSyntheticHtml();
        }
        sampleHtmlText = new String(sampleHtmlBytes, StandardCharsets.UTF_8);
    }

    @TearDown
    public void tearDown() {
        if (spider != null) {
            spider.close();
        }
    }

    @Benchmark
    public int benchmarkJdkPatternExtraction() {
        Matcher matcher = HREF_PATTERN.matcher(sampleHtmlText);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    @Benchmark
    public int benchmarkFastSpiderAvx2Extraction() {
        List<String> links = spider.extractHrefs(sampleHtmlBytes);
        return links.size();
    }

    private static byte[] buildSyntheticHtml() {
        StringBuilder sb = new StringBuilder(500_000);
        sb.append("<!DOCTYPE html><html><body><h1>Benchmark Document</h1>");
        for (int i = 0; i < 2000; i++) {
            sb.append("<p>Paragraph containing references to <a href=\"/wiki/Article_")
              .append(i)
              .append("\">Link ")
              .append(i)
              .append("</a> and some text describing vector hardware acceleration.</p>");
        }
        sb.append("</body></html>");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
