package fastspider;

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
 * Hero Demo demonstrating FastSpider capabilities.
 */
public class Demo {

    private Demo() {
        // Utility class
    }

    /**
     * Main entry point for FastSpider demo.
     * 
     * @param args command line arguments
     * @throws Exception if server setup fails
     */
    public static void main(String[] args) throws Exception {
        System.out.println("\u001B[35m====================================================================\u001B[0m");
        System.out.println("\u001B[36;1m⚡ FastSpider Native WinHTTP + Virtual Thread Crawler — Hero Demo ⚡\u001B[0m");
        System.out.println("\u001B[35m====================================================================\u001B[0m");

        // 1. Start a local mock HTTP server to run completely self-contained & offline!
        System.out.println("\u001B[33mStarting offline mock HTTP server on 127.0.0.1...\u001B[0m");
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        
        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String response = "<html>\n" +
                        "<head>\n" +
                        "  <title>Welcome to FastSpider Home</title>\n" +
                        "  <style>body { background: #111; color: #eee; }</style>\n" +
                        "</head>\n" +
                        "<body>\n" +
                        "  <h1>🚀 FastSpider Native Engine</h1>\n" +
                        "  <p>This is the main root page loaded from our native WinHTTP wrapper.</p>\n" +
                        "  <a href=\"/about\">About Us Page</a>\n" +
                        "  <a href=\"/features\">Features List</a>\n" +
                        "  <a href=\"https://github.com/andrestubbe/FastSpider\">GitHub Repository</a>\n" +
                        "  <script>console.log(\"Ignored scripting\");</script>\n" +
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

        server.createContext("/about", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String response = "<html><body><h1>About FastSpider</h1><p>Written in native C++ and modern Java 17+.</p></body></html>";
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        });

        // Simulate a slow network response (500ms delay) to showcase the power of non-blocking Virtual Threads
        server.createContext("/slow", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                String response = "<html><body><h1>Slow Content</h1><p>Loaded after 500 milliseconds delay.</p></body></html>";
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
        String baseUrl = "http://127.0.0.1:" + port;
        System.out.println("\u001B[32m✔ Mock server running on port " + port + "\u001B[0m\n");

        // 2. Open FastSpider
        FastSpider spider = FastSpider.open();

        // 3. Demo 1: Single Fetch Async
        System.out.println("\u001B[33m[1] Fetching home page asynchronously (JNI native call in Virtual Thread)...\u001B[0m");
        long start = System.nanoTime();
        CompletableFuture<FastSpider.SpiderResponse> future = spider.fetchAsync(baseUrl + "/");
        FastSpider.SpiderResponse response = future.join();
        long duration = System.nanoTime() - start;

        System.out.println("\u001B[32m--- FETCH SUCCESS --- (Time taken: " + (duration / 1_000_000.0) + " ms)\u001B[0m");
        System.out.println("  Status Code: " + response.statusCode());
        System.out.println("  Native Fetch time reported by WinHTTP: " + response.fetchTimeMs() + " ms");
        System.out.println("  Body size: " + response.rawBody().length + " bytes");

        // 4. Demo 2: Native AVX2 parsing integrated inside FastSpider
        System.out.println("\n\u001B[33m[2] Native AVX2 parsing of home page body...\u001B[0m");
        String cleanText = spider.extractCleanText(response.rawBody());
        List<String> links = spider.extractHrefs(response.rawBody());

        System.out.println("\u001B[32m--- Clean Text extracted natively ---\u001B[0m");
        System.out.println(cleanText);
        System.out.println("\u001B[32m--- Hrefs extracted natively ---\u001B[0m");
        for (String url : links) {
            System.out.println("  🔗 " + url);
        }

        // 5. Demo 3: Concurrent Batch Fetching of Slow URLS via Virtual Threads
        System.out.println("\n\u001B[33m[3] Crawling multiple URLs concurrently (including three 500ms delay endpoints)...\u001B[0m");
        List<String> urlsToFetch = List.of(
            baseUrl + "/",
            baseUrl + "/about",
            baseUrl + "/slow",
            baseUrl + "/slow",
            baseUrl + "/slow"
        );

        long batchStart = System.currentTimeMillis();
        List<FastSpider.SpiderResponse> responses = spider.fetchBatch(urlsToFetch);
        long batchEnd = System.currentTimeMillis();

        System.out.println("\u001B[32m--- BATCH CRAWL COMPLETE --- (Time taken: " + (batchEnd - batchStart) + " ms)\u001B[0m");
        System.out.println("Notice that the total duration is ~500ms, proving that all three slow fetches occurred concurrently in parallel virtual threads!");
        System.out.printf("| %-30s | %-12s | %-15s | %-12s |\n", "URL", "Status Code", "Body size", "Native time");
        System.out.println("|--------------------------------|--------------|-----------------|-------------|");
        for (int i = 0; i < urlsToFetch.size(); i++) {
            FastSpider.SpiderResponse res = responses.get(i);
            String urlLabel = urlsToFetch.get(i).substring(baseUrl.length());
            System.out.printf("| %-30s | %-12d | %-12d B | %10d ms |\n", urlLabel, res.statusCode(), res.rawBody().length, res.fetchTimeMs());
        }

        // 6. Stop Server and Clean up
        System.out.println("\n\u001B[33mStopping mock HTTP server...\u001B[0m");
        server.stop(0);

        System.out.println("\n\u001B[35m====================================================================\u001B[0m");
        System.out.println("\u001B[36;1m✔ Hero Demo Complete! FastSpider executed flawlessly.\u001B[0m");
        System.out.println("\u001B[35m====================================================================\u001B[0m");
    }
}
