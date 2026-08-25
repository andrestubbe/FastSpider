package fastwebspider;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * FastWebSpider — High-performance native WinHTTP web crawler for Java.
 * 
 * Leverages native Windows HTTP Services (WinHTTP) and Schannel at the JNI layer
 * to execute non-blocking, zero-copy asynchronous page fetches inside modern
 * Java Virtual Threads, avoiding garbage-collection and HTTP client overhead.
 */
public interface FastWebSpider {

    /**
     * Opens a new FastWebSpider instance.
     * 
     * @return a thread-safe FastWebSpider implementation
     */
    static FastWebSpider open() {
        return new FastWebSpiderImpl();
    }

    /**
     * Represents the result of a crawled page request.
     * 
     * @param statusCode the HTTP status code returned by the server (e.g. 200)
     * @param rawBody the raw response body bytes
     * @param fetchTimeMs the native execution duration in milliseconds
     */
    public record SpiderResponse(int statusCode, byte[] rawBody, long fetchTimeMs) {
        /**
         * Checks if the response code indicates success (2xx).
         * 
         * @return true if status is between 200 and 299 inclusive
         */
        public boolean isSuccess() {
            return statusCode >= 200 && statusCode < 300;
        }
    }

    /**
     * Asynchronously fetches the content of a single URL.
     * Uses Java Virtual Threads to handle the native blocking calls.
     * 
     * @param url the complete HTTP or HTTPS URL to fetch
     * @return a CompletableFuture resolving to the SpiderResponse
     */
    CompletableFuture<SpiderResponse> fetchAsync(String url);

    /**
     * Synchronously/concurrently fetches a batch of URLs using virtual threads.
     * Blocks until all responses are completed.
     * 
     * @param urls the list of URLs to crawl
     * @return the list of fetched SpiderResponses in corresponding order
     */
    List<SpiderResponse> fetchBatch(List<String> urls);

    /**
     * Extracts clean, readable text from HTML data using native AVX2 logic.
     * Strips elements like {@code <script>}, {@code <style>}, and comments.
     * 
     * @param htmlData raw HTML page bytes
     * @return clean plain text formatted for LLM parsing
     */
    String extractCleanText(byte[] htmlData);

    /**
     * Rapidly extracts all link URLs (hrefs) from HTML data using native AVX2 logic.
     * 
     * @param htmlData raw HTML page bytes
     * @return a list of link URLs
     */
    List<String> extractHrefs(byte[] htmlData);
}
