package fastspider;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import static org.junit.jupiter.api.Assertions.*;

public class FastSpiderTest {

    private static FastSpider spider;
    private static HttpServer server;
    private static String baseUrl;

    @BeforeAll
    public static void setup() throws Exception {
        spider = FastSpider.open();
        
        // Dynamic port selection
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/test", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String response = "<html><body><h1>Hello Test</h1><a href=\"/link1\">Link 1</a></body></html>";
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        });
        server.createContext("/404", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                exchange.sendResponseHeaders(404, 0);
                exchange.close();
            }
        });
        server.setExecutor(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());
        server.start();
        
        int port = server.getAddress().getPort();
        baseUrl = "http://127.0.0.1:" + port;
    }

    @AfterAll
    public static void teardown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    public void testFetchAsyncSuccess() throws Exception {
        CompletableFuture<FastSpider.SpiderResponse> future = spider.fetchAsync(baseUrl + "/test");
        FastSpider.SpiderResponse response = future.get();
        
        assertNotNull(response);
        assertEquals(200, response.statusCode());
        assertTrue(response.isSuccess());
        assertTrue(response.rawBody().length > 0);
        assertTrue(response.fetchTimeMs() >= 0);
        
        String html = new String(response.rawBody(), StandardCharsets.UTF_8);
        assertTrue(html.contains("Hello Test"));
    }

    @Test
    public void testFetchAsync404() throws Exception {
        CompletableFuture<FastSpider.SpiderResponse> future = spider.fetchAsync(baseUrl + "/404");
        FastSpider.SpiderResponse response = future.get();
        
        assertNotNull(response);
        assertEquals(404, response.statusCode());
        assertFalse(response.isSuccess());
    }

    @Test
    public void testBatchFetch() {
        List<String> urls = List.of(baseUrl + "/test", baseUrl + "/404", baseUrl + "/test");
        List<FastSpider.SpiderResponse> responses = spider.fetchBatch(urls);
        
        assertEquals(3, responses.size());
        assertEquals(200, responses.get(0).statusCode());
        assertEquals(404, responses.get(1).statusCode());
        assertEquals(200, responses.get(2).statusCode());
    }

    @Test
    public void testExtractCleanText() {
        String html = "<html><head><style>body{margin:0;}</style></head><body><h1>Title</h1><p>Para <script>alert(1);</script></p></body></html>";
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        
        String cleaned = spider.extractCleanText(bytes);
        assertNotNull(cleaned);
        assertTrue(cleaned.contains("Title"));
        assertTrue(cleaned.contains("Para"));
        assertFalse(cleaned.contains("margin"));
        assertFalse(cleaned.contains("alert"));
    }

    @Test
    public void testExtractHrefs() {
        String html = "<html><body><a href=\"/url1\">URL 1</a><a href='/url2'>URL 2</a></body></html>";
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        
        List<String> links = spider.extractHrefs(bytes);
        assertEquals(2, links.size());
        assertTrue(links.contains("/url1"));
        assertTrue(links.contains("/url2"));
    }

    @Test
    public void testEmptyInput() {
        String text = spider.extractCleanText(new byte[0]);
        assertEquals("", text);

        List<String> links = spider.extractHrefs(new byte[0]);
        assertTrue(links.isEmpty());
    }
}
