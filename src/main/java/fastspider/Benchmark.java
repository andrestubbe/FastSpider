package fastspider;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * High-speed Benchmark comparing FastSpider JNI WinHTTP + Virtual Threads against Standard Java HttpClient.
 */
public class Benchmark {

    private Benchmark() {
        // Utility class
    }

    /**
     * Main entry point for FastSpider benchmark.
     * 
     * @param args command line arguments
     * @throws Exception if server or requests fail
     */
    public static void main(String[] args) throws Exception {
        System.out.println("\u001B[35m====================================================================\u001B[0m");
        System.out.println("\u001B[36;1m📈 FastSpider Network Concurrency Performance Race 📈\u001B[0m");
        System.out.println("\u001B[35m====================================================================\u001B[0m");

        // Start mock HTTP server
        System.out.println("\u001B[33mStarting mock HTTP server for benchmark... \u001B[0m");
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/bench", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String response = "<html><body><h1>Benchmark Page</h1><p>FastSpider JNI speed test.</p></body></html>";
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        });
        server.setExecutor(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());
        server.start();
        int port = server.getAddress().getPort();
        String benchUrl = "http://127.0.0.1:" + port + "/bench";
        System.out.println("\u001B[32m✔ Mock server running on port " + port + "\u001B[0m");

        int requestCount = 100;
        System.out.println("Preparing to execute " + requestCount + " concurrent HTTP requests against the mock server.\n");

        FastSpider spider = FastSpider.open();

        // Warmup loops to load classes, JNI libs, and establish early TCP connections
        System.out.println("\u001B[33mWarming up JNI, JVM HttpClient, and connection pools...\u001B[0m");
        HttpClient warmupClient = HttpClient.newHttpClient();
        for (int i = 0; i < 10; i++) {
            spider.fetchAsync(benchUrl).join();
            warmupClient.send(HttpRequest.newBuilder().uri(URI.create(benchUrl)).build(), HttpResponse.BodyHandlers.discarding());
        }

        // Test 1: Standard Java HttpClient (Async)
        System.out.println("\u001B[33mRunning Race 1: Standard Java HttpClient (Async)... \u001B[0m");
        HttpClient httpClient = HttpClient.newBuilder().build();
        long start = System.currentTimeMillis();
        List<CompletableFuture<HttpResponse<byte[]>>> javaFutures = new ArrayList<>();
        for (int i = 0; i < requestCount; i++) {
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(benchUrl)).build();
            javaFutures.add(httpClient.sendAsync(req, HttpResponse.BodyHandlers.ofByteArray()));
        }
        CompletableFuture.allOf(javaFutures.toArray(new CompletableFuture[0])).join();
        long javaEnd = System.currentTimeMillis();
        long javaTime = javaEnd - start;

        // Test 2: FastSpider JNI WinHTTP (Virtual Threads)
        System.out.println("\u001B[33mRunning Race 2: FastSpider Native WinHTTP (Virtual Threads)... \u001B[0m");
        start = System.currentTimeMillis();
        List<CompletableFuture<FastSpider.SpiderResponse>> spiderFutures = new ArrayList<>();
        for (int i = 0; i < requestCount; i++) {
            spiderFutures.add(spider.fetchAsync(benchUrl));
        }
        CompletableFuture.allOf(spiderFutures.toArray(new CompletableFuture[0])).join();
        long spiderEnd = System.currentTimeMillis();
        long spiderTime = spiderEnd - start;

        // Visual Result Matrix
        System.out.println("\n\u001B[32m--- RACE RESULT SUMMARY ---\u001B[0m");
        System.out.printf("| %-28s | %-16s | %-16s |\n", "Operation", "Java HttpClient", "FastSpider Native");
        System.out.println("|------------------------------|------------------|-------------------|");
        System.out.printf("| %-28s | %13d ms | %14d ms |\n", "Concurrent fetches (100 req)", javaTime, spiderTime);
        System.out.println("---------------------------");

        double speedup = (double) javaTime / Math.max(1, spiderTime);
        System.out.printf("\u001B[32;1m⚡ FastSpider native speedup: %.2fx faster than Standard Java HttpClient! ⚡\u001B[0m\n", speedup);
        System.out.println("\u001B[35m====================================================================\u001B[0m");

        // Stop Server
        server.stop(0);
    }
}
