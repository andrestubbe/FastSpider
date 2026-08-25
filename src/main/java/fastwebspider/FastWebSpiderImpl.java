package fastwebspider;

import fastcore.FastCore;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Implementation of FastWebSpider interface using native WinHTTP and AVX2 logic.
 */
class FastWebSpiderImpl implements FastWebSpider {

    static {
        // Load the JNI library using FastCore Unified Loader
        FastCore.loadLibrary("fastwebspider");
    }

    // Virtual Thread Executor for async WinHTTP tasks
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    // Native JNI methods
    private native SpiderResponse nativeFetch(String url);
    private native String nativeExtractCleanText(byte[] htmlData);
    private native String[] nativeExtractHrefs(byte[] htmlData);

    @Override
    public CompletableFuture<SpiderResponse> fetchAsync(String url) {
        Objects.requireNonNull(url, "url cannot be null");
        return CompletableFuture.supplyAsync(() -> nativeFetch(url), executor);
    }

    @Override
    public List<SpiderResponse> fetchBatch(List<String> urls) {
        Objects.requireNonNull(urls, "urls list cannot be null");
        List<CompletableFuture<SpiderResponse>> futures = urls.stream()
                .map(this::fetchAsync)
                .toList();

        // Wait for all to complete and collect results
        return futures.stream()
                .map(CompletableFuture::join)
                .toList();
    }

    @Override
    public String extractCleanText(byte[] htmlData) {
        if (htmlData == null || htmlData.length == 0) {
            return "";
        }
        return nativeExtractCleanText(htmlData);
    }

    @Override
    public List<String> extractHrefs(byte[] htmlData) {
        if (htmlData == null || htmlData.length == 0) {
            return new ArrayList<>();
        }
        String[] hrefs = nativeExtractHrefs(htmlData);
        return hrefs != null ? List.of(hrefs) : new ArrayList<>();
    }
}
