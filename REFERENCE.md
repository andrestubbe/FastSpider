# FastSpider Reference

## 1. CPU Feature Model
*   **AVX2** â€” detected via CPUID. Enables 32-byte vector ops.
*   **SSE4.2** â€” detected via CPUID. 16-byte fallback.
*   **Fallback rule**: AVX2 â†’ SSE4.2 â†’ scalar.

## 2. Guarantees
*   **Zero-Copy**: All operations use `GetPrimitiveArrayCritical` for direct memory access.
*   **Unaligned Access**: Safe on all byte boundaries.
*   **Thread-Safety**: All static native methods are thread-safe.

## 3. JNI & Memory Contracts
*   **Direct Memory Pinning**: No implicit copies are made by the JNI bridge.
*   **No Allocation**: All operations work on pre-allocated Java arrays or buffers.
*   **Critical Sections**: Native calls minimize blocking to prevent GC impact.

## 4. Platform Support
| Platform | Status |
|----------|--------|
| Windows 10/11 (x64) | âœ… Fully Supported |

---
**Part of the FastJava Ecosystem** â€” *Making the JVM faster.*

Made with âš¡ by Andre Stubbe

