package fastpipeline;

import fastspider.FastSpider;
import fastscrape.FastScrape;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * PipelineDemo — Orchestration demonstrating FastSpider and FastScrape working in unison.
 */
public class PipelineDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("====================================================================");
        System.out.println(" FastJava Pipeline — Native Crawler (WinHTTP) + Native Parser (SIMD)");
        System.out.println("====================================================================");
        System.out.println();

        // 1. Start a local mock HTTP server serving a rich structured page
        System.out.println("Starting local mock HTTP server...");
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        
        server.createContext("/target", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String response = "<html>\n" +
                        "<head>\n" +
                        "  <title>FastJava Pipeline Article</title>\n" +
                        "  <style>body { font-family: monospace; background: #000; }</style>\n" +
                        "  <script type=\"application/ld+json\">\n" +
                        "  {\n" +
                        "    \"@context\": \"https://schema.org\",\n" +
                        "    \"@type\": \"NewsArticle\",\n" +
                        "    \"headline\": \"SIMD Acceleration in JVM Ecosystems\"\n" +
                        "  }\n" +
                        "  </script>\n" +
                        "</head>\n" +
                        "<body>\n" +
                        "  <h1>High-Performance JNI Integration</h1>\n" +
                        "  <p>This page was fetched asynchronously via C++ WinHTTP sockets.</p>\n" +
                        "  <p>Now, it will be parsed natively via C++ AVX2 vector registers!</p>\n" +
                        "  \n" +
                        "  <div class=\"links\">\n" +
                        "    <a href=\"https://github.com/andrestubbe/FastSpider\">FastSpider Github</a>\n" +
                        "    <a href=\"https://github.com/andrestubbe/FastScrape\">FastScrape Github</a>\n" +
                        "  </div>\n" +
                        "  <script>console.log(\"should be stripped\");</script>\n" +
                        "</body>\n" +
                        "</html>";
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        });

        server.setExecutor(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());
        server.start();
        int port = server.getAddress().getPort();
        String targetUrl = "http://127.0.0.1:" + port + "/target";
        System.out.println("OK: Mock server running on " + targetUrl + "\n");

        // 2. Open both native engines
        System.out.println("Initializing Native WinHTTP Crawler and SIMD Parser...");
        FastSpider spider = FastSpider.open();
        FastScrape scraper = FastScrape.open();
        System.out.println("OK: Both native engines loaded via FastCore.\n");

        // 3. Execution Pipeline
        System.out.println("[Step 1] Fetching page asynchronously with FastSpider...");
        long startFetch = System.nanoTime();
        CompletableFuture<FastSpider.SpiderResponse> future = spider.fetchAsync(targetUrl);
        FastSpider.SpiderResponse response = future.join();
        long fetchDurationMs = (System.nanoTime() - startFetch) / 1_000_000;

        if (response.isSuccess()) {
            System.out.println("OK: Downloaded " + response.rawBody().length + " bytes in " + fetchDurationMs + " ms.\n");

            // Zero-Copy Passing directly to FastScrape JNI
            System.out.println("[Step 2] Passing raw bytes to FastScrape for instant SIMD parsing...");
            
            // Extract clean visible text
            long textStart = System.nanoTime();
            String readableText = scraper.extractReadableText(response.rawBody());
            double textTimeUs = (System.nanoTime() - textStart) / 1000.0;

            // Extract links
            long linksStart = System.nanoTime();
            List<String> links = scraper.extractLinks(response.rawBody());
            double linksTimeUs = (System.nanoTime() - linksStart) / 1000.0;

            // Extract JSON-LD schema
            long jsonStart = System.nanoTime();
            String jsonLD = scraper.extractJsonLD(response.rawBody());
            double jsonTimeUs = (System.nanoTime() - jsonStart) / 1000.0;

            // Render Output Matrix
            System.out.println("====================================================================");
            System.out.println(" Pipeline Metrics & Extracted Results");
            System.out.println("====================================================================");
            
            System.out.printf("\n[Readable Plain Text] extracted in %.2f µs:\n", textTimeUs);
            System.out.println(readableText);
            
            System.out.printf("\n[Anchor Links] (%,d links) extracted in %.2f µs:\n", links.size(), linksTimeUs);
            for (String link : links) {
                System.out.println("  -> " + link);
            }
            
            System.out.printf("\n[JSON-LD Schema Metadata] extracted in %.2f µs:\n", jsonTimeUs);
            System.out.println(jsonLD.trim());
        } else {
            System.err.println("ERR: Fetch failed with status " + response.statusCode());
        }

        // 4. Shutdown server
        System.out.println("\nShutting down mock HTTP server...");
        server.stop(0);
        System.out.println("====================================================================");
        System.out.println(" Pipeline Execution Complete!");
        System.out.println("====================================================================");
    }
}
