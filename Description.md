# FastSpider (inkl. FastScrape)

## 1. Vision & Kernidee
**FastSpider** ist die native Hochgeschwindigkeits-Engine für Web-Crawling und Text-Extraktion. 

Wenn ein KI-Agent eine Aufgabe bekommt ("Recherchiere die neuesten News zu X" oder "Lies die Dokumentation von Y"), muss er das Internet lesen. Klassische Lösungen wie *Selenium* oder *Playwright* starten einen kompletten Headless-Chrome, der Gigabytes an RAM und wertvolle Sekunden frisst. Javas `HttpURLConnection` und `JSoup` sind solide, generieren aber extrem viel Garbage (Strings) beim Parsen von HTML-DOMs.

**Die FastJava Lösung:**
FastSpider umgeht den kompletten Java-Overhead. Es lädt Webseiten über asynchrone native Sockets (Win32 IOCP) herunter und jagt **SIMD-AVX2 Befehle** über das rohe HTML-Byte-Array, um in Mikrosekunden alle HTML-Tags wegzustrippen und nur den reinen, lesbaren Text (für das LLM) herauszufiltern.

## 2. Java High-Level API

```java
public interface FastSpider {
    static FastSpider open() { return new FastSpiderImpl(); }

    // Asynchroner Massen-Download von URLs
    CompletableFuture<SpiderResponse> fetchAsync(String url);
    List<SpiderResponse> fetchBatch(List<String> urls);

    // FastScrape-Komponente: SIMD-HTML Parsing
    // Extrahiert nur den sichtbaren Text (ohne <script>, <style> oder HTML-Tags)
    String extractCleanText(byte[] htmlData);
    
    // Findet spezifische Elemente (wie Links) rasend schnell im Byte-Array
    List<String> extractHrefs(byte[] htmlData);
}

public record SpiderResponse(int statusCode, byte[] rawBody, long fetchTimeMs) {}
```

## 3. C++ JNI Backend (IOCP & SIMD)
Das Backend kombiniert asynchrone Netzwerk-I/O mit roher CPU-Power.

1. **Netzwerk (IOCP):** Unter Windows nutzt das Backend I/O Completion Ports für den HTTP/HTTPS-Download. Das bedeutet, dass Tausende Verbindungen gleichzeitig offen sein können, ohne dass C++ für jede Verbindung einen Thread spawnen muss (Zero-Blocking).
2. **SIMD-Scraping:** Sobald das HTML im Speicher liegt (als Byte-Array), durchkämmt eine AVX2-Schleife die Daten. Anstatt einen komplexen DOM-Tree zu bauen (wie JSoup/Chrome), sucht AVX2 in 32-Byte-Blöcken nach `<` und `>` und verwirft den Inhalt dazwischen (besonders optimiert für `<script>` und `<style>`).
3. **Zero-Copy Returns:** Der resultierende saubere Text wird direkt in einen `FastString` oder als UTF-8 Byte-Array nach Java gereicht, um LLM-Tokenizer direkt zu füttern.

## 4. Agent-Kit (KI-Integration)
Für das LLM ist FastSpider das "Lesegerät" für externe Links.

**Workflow für Agenten (RAG & Web Search):**
1. Der Agent führt eine Websuche durch (z.B. Google API) und bekommt 5 URLs.
2. Der Agent ruft `FastSpider.fetchBatch(urls)` auf.
3. Innerhalb von Millisekunden lädt FastSpider alle 5 Seiten herunter.
4. `extractCleanText` entfernt den ganzen HTML-Müll.
5. Der Agent bekommt 5 saubere Text-Blöcke, füttert sie in seinen Kontext (RAG) und beantwortet deine Frage.

*(Hinweis: Für extrem moderne Single-Page-Apps (React/Vue), die ohne JavaScript komplett leer sind, kann der Agent immer noch auf FastRobot/FastScreen zurückgreifen und die echte visuelle Seite "lesen".)*
