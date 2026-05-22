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
        System.out.println("\u001B[35m====================================================================\u001B[0m");
        System.out.println("\u001B[36;1m⚡ FastJava Pipeline — Native Crawler (WinHTTP) + Native Parser (SIMD) ⚡\u001B[0m");
        System.out.println("\u001B[35m====================================================================\u001B[0m");

        // 1. Start a local mock HTTP server serving a rich structured page
        System.out.println("\u001B[33mStarting local mock HTTP server...\u001B[0m");
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
                        "  <h1>⚡ High-Performance JNI Integration</h1>\n" +
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
        System.out.println("\u001B[32m✔ Mock server running on " + targetUrl + "\u001B[0m\n");

        // 2. Open both native engines
        System.out.println("\u001B[33mInitializing Native WinHTTP Crawler and SIMD Parser...\u001B[0m");
        FastSpider spider = FastSpider.open();
        FastScrape scraper = FastScrape.open();
        System.out.println("\u001B[32m✔ Both libraries successfully JNI loaded.\u001B[0m\n");

        // 3. Execution Pipeline
        System.out.println("\u001B[33m[Step 1] Fetching page asynchronously with FastSpider...\u001B[0m");
        long startFetch = System.nanoTime();
        CompletableFuture<FastSpider.SpiderResponse> future = spider.fetchAsync(targetUrl);
        FastSpider.SpiderResponse response = future.join();
        long fetchDurationMs = (System.nanoTime() - startFetch) / 1_000_000;

        if (response.isSuccess()) {
            System.out.println("\u001B[32m✔ Page successfully downloaded via WinHTTP in " + fetchDurationMs + " ms! (" + response.rawBody().length + " bytes)\u001B[0m\n");

            // Zero-Copy Passing directly to FastScrape JNI
            System.out.println("\u001B[33m[Step 2] Passing raw bytes to FastScrape for instant SIMD parsing...\u001B[0m");
            
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
            System.out.println("\u001B[32m====================================================================\u001B[0m");
            System.out.println("\u001B[36;1m📊 Pipeline Metrics & Extracted Results 📊\u001B[0m");
            System.out.println("\u001B[32m====================================================================\u001B[0m");
            
            System.out.println("\n\u001B[33m[Readable Plain Text] extracted in " + textTimeUs + " microseconds:\u001B[0m");
            System.out.println(readableText);
            
            System.out.println("\n\u001B[33m[Anchor Links] extracted in " + linksTimeUs + " microseconds:\u001B[0m");
            for (String link : links) {
                System.out.println("  🔗 " + link);
            }
            
            System.out.println("\n\u001B[33m[JSON-LD Schema Metadata] extracted in " + jsonTimeUs + " microseconds:\u001B[0m");
            System.out.println(jsonLD.trim());
        } else {
            System.err.println("❌ Fetch failed: Status " + response.statusCode());
        }

        // 4. Shutdown server
        System.out.println("\n\u001B[33mShutting down mock HTTP server...\u001B[0m");
        server.stop(0);
        System.out.println("\u001B[35m====================================================================\u001B[0m");
        System.out.println("\u001B[36;1m✔ Pipeline Execution Complete!\u001B[0m");
        System.out.println("\u001B[35m====================================================================\u001B[0m");
    }
}
