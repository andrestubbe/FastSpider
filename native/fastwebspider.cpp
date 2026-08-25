#include "fastwebspider.h"
#include <windows.h>
#include <winhttp.h>
#include <stdio.h>
#include <string>
#include <vector>
#include <chrono>
#include <algorithm>
#include <immintrin.h>
#include <cctype>

#ifdef _MSC_VER
#include <intrin.h>
#endif

// ============================================================================
// DLL Entry Point & Global Session
// ============================================================================
static HINTERNET g_hSession = nullptr;

BOOL APIENTRY DllMain(HMODULE hModule, DWORD ul_reason_for_call, LPVOID lpReserved) {
    switch (ul_reason_for_call) {
        case DLL_PROCESS_ATTACH:
            DisableThreadLibraryCalls(hModule);
            g_hSession = WinHttpOpen(L"fastwebspider/1.0 (Windows; JNI)",
                                     WINHTTP_ACCESS_TYPE_DEFAULT_PROXY,
                                     WINHTTP_NO_PROXY_NAME,
                                     WINHTTP_NO_PROXY_BYPASS, 0);
            break;
        case DLL_PROCESS_DETACH:
            if (g_hSession) {
                WinHttpCloseHandle(g_hSession);
                g_hSession = nullptr;
            }
            break;
    }
    return TRUE;
}


// ============================================================================
// CPU Feature Detection
// ============================================================================
static bool g_avx2 = false;
static bool g_initialized = false;

static void initCpuFeatures() {
    if (g_initialized) return;
    int cpuInfo[4] = {0};
#ifdef _MSC_VER
    __cpuid(cpuInfo, 1);
#else
    __cpuid(1, cpuInfo[0], cpuInfo[1], cpuInfo[2], cpuInfo[3]);
#endif
    bool hasAVX = (cpuInfo[2] & (1 << 28)) != 0;
    if (hasAVX) {
#ifdef _MSC_VER
        __cpuidex(cpuInfo, 7, 0);
#else
        __cpuid_count(7, 0, cpuInfo[0], cpuInfo[1], cpuInfo[2], cpuInfo[3]);
#endif
        g_avx2 = (cpuInfo[1] & (1 << 5)) != 0;
    }
    g_initialized = true;
}

bool hasAVX2() {
    if (!g_initialized) initCpuFeatures();
    return g_avx2;
}

// ============================================================================
// Helpers and Utilities
// ============================================================================

// Fast ASCII case insensitivity that is safe from UB on non-ASCII characters
inline uint8_t toLowerByte(uint8_t c) {
    return (c >= 'A' && c <= 'Z') ? (c + ('a' - 'A')) : c;
}

// ============================================================================
// SIMD AVX2 and Fallback Parser Implementations
// ============================================================================

std::string extractCleanTextCpp(const uint8_t* data, size_t length) {
    std::string result;
    result.reserve(length);

    size_t i = 0;
    bool in_tag = false;
    bool in_script = false;
    bool in_style = false;
    bool in_comment = false;
    
    const bool useAvx2 = hasAVX2();
    
    while (i < length) {
        if (!in_tag && !in_script && !in_style && !in_comment) {
            // AVX2 accelerated search for '<' or whitespace
            if (useAvx2 && i + 31 < length) {
                __m256i chunk = _mm256_loadu_si256(reinterpret_cast<const __m256i*>(data + i));
                __m256i tagChar = _mm256_set1_epi8('<');
                __m256i spaceChar = _mm256_set1_epi8(' ');
                __m256i tabChar = _mm256_set1_epi8('\t');
                __m256i nlChar = _mm256_set1_epi8('\n');
                __m256i crChar = _mm256_set1_epi8('\r');
                
                __m256i cmpTag = _mm256_cmpeq_epi8(chunk, tagChar);
                __m256i cmpSpace = _mm256_cmpeq_epi8(chunk, spaceChar);
                __m256i cmpTab = _mm256_cmpeq_epi8(chunk, tabChar);
                __m256i cmpNl = _mm256_cmpeq_epi8(chunk, nlChar);
                __m256i cmpCr = _mm256_cmpeq_epi8(chunk, crChar);
                
                __m256i cmpAny = _mm256_or_si256(cmpTag, _mm256_or_si256(cmpSpace, _mm256_or_si256(cmpTab, _mm256_or_si256(cmpNl, cmpCr))));
                int mask = _mm256_movemask_epi8(cmpAny);
                
                if (mask == 0) {
                    result.append(reinterpret_cast<const char*>(data + i), 32);
                    i += 32;
                    continue;
                } else {
                    unsigned long index = 32;
#ifdef _MSC_VER
                    _BitScanForward(&index, mask);
#else
                    index = __builtin_ctz(mask);
#endif
                    if (index > 0) {
                        result.append(reinterpret_cast<const char*>(data + i), index);
                        i += index;
                        continue;
                    }
                }
            }
        }
        
        uint8_t c = data[i];
        if (in_comment) {
            if (c == '-' && i + 2 < length && data[i + 1] == '-' && data[i + 2] == '>') {
                in_comment = false;
                i += 3;
            } else {
                i++;
            }
            continue;
        }
        
        if (in_script) {
            // Tolerant script closing tag match: </script  > or </script type="...">
            if (c == '<' && i + 7 < length &&
                data[i + 1] == '/' &&
                toLowerByte(data[i + 2]) == 's' &&
                toLowerByte(data[i + 3]) == 'c' &&
                toLowerByte(data[i + 4]) == 'r' &&
                toLowerByte(data[i + 5]) == 'i' &&
                toLowerByte(data[i + 6]) == 'p' &&
                toLowerByte(data[i + 7]) == 't') {
                
                char next = (i + 8 < length) ? data[i + 8] : '>';
                if (next == '>' || next == ' ' || next == '\t' || next == '\r' || next == '\n' || next == '/') {
                    size_t closeIdx = i + 8;
                    while (closeIdx < length && data[closeIdx] != '>') {
                        closeIdx++;
                    }
                    if (closeIdx < length) {
                        in_script = false;
                        i = closeIdx + 1;
                        continue;
                    }
                }
            }
            i++;
            continue;
        }
        
        if (in_style) {
            // Tolerant style closing tag match: </style  >
            if (c == '<' && i + 6 < length &&
                data[i + 1] == '/' &&
                toLowerByte(data[i + 2]) == 's' &&
                toLowerByte(data[i + 3]) == 't' &&
                toLowerByte(data[i + 4]) == 'y' &&
                toLowerByte(data[i + 5]) == 'l' &&
                toLowerByte(data[i + 6]) == 'e') {
                
                char next = (i + 7 < length) ? data[i + 7] : '>';
                if (next == '>' || next == ' ' || next == '\t' || next == '\r' || next == '\n' || next == '/') {
                    size_t closeIdx = i + 7;
                    while (closeIdx < length && data[closeIdx] != '>') {
                        closeIdx++;
                    }
                    if (closeIdx < length) {
                        in_style = false;
                        i = closeIdx + 1;
                        continue;
                    }
                }
            }
            i++;
            continue;
        }
        
        if (c == '<') {
            in_tag = true;
            if (i + 3 < length && data[i + 1] == '!' && data[i + 2] == '-' && data[i + 3] == '-') {
                in_comment = true;
                in_tag = false;
                i += 4;
                continue;
            }
            if (i + 7 < length &&
                toLowerByte(data[i + 1]) == 's' &&
                toLowerByte(data[i + 2]) == 'c' &&
                toLowerByte(data[i + 3]) == 'r' &&
                toLowerByte(data[i + 4]) == 'i' &&
                toLowerByte(data[i + 5]) == 'p' &&
                toLowerByte(data[i + 6]) == 't' &&
                (data[i + 7] == '>' || data[i + 7] == ' ' || data[i + 7] == '\t' || data[i + 7] == '\r' || data[i + 7] == '\n')) {
                in_script = true;
                in_tag = false;
                i += 7;
                continue;
            }
            if (i + 6 < length &&
                toLowerByte(data[i + 1]) == 's' &&
                toLowerByte(data[i + 2]) == 't' &&
                toLowerByte(data[i + 3]) == 'y' &&
                toLowerByte(data[i + 4]) == 'l' &&
                toLowerByte(data[i + 5]) == 'e' &&
                (data[i + 6] == '>' || data[i + 6] == ' ' || data[i + 6] == '\t' || data[i + 6] == '\r' || data[i + 6] == '\n')) {
                in_style = true;
                in_tag = false;
                i += 6;
                continue;
            }
            
            // Check for HTML5 Block Boundaries and self-closing tags
            bool is_block = false;
            if (i + 2 < length && data[i + 1] == '/') {
                size_t nameStart = i + 2;
                size_t nameLen = 0;
                while (nameStart + nameLen < length && data[nameStart + nameLen] != '>' && data[nameStart + nameLen] != ' ' && data[nameStart + nameLen] != '\t' && data[nameStart + nameLen] != '\r' && data[nameStart + nameLen] != '\n') {
                    nameLen++;
                }
                if (nameLen > 0) {
                    std::string tagNameStr(reinterpret_cast<const char*>(data + nameStart), nameLen);
                    for (char &ch : tagNameStr) ch = toLowerByte(ch);
                    if (tagNameStr == "p" || tagNameStr == "div" || tagNameStr == "li" || tagNameStr == "br" ||
                        (tagNameStr[0] == 'h' && tagNameStr.length() == 2 && tagNameStr[1] >= '1' && tagNameStr[1] <= '6') ||
                        tagNameStr == "section" || tagNameStr == "article" || tagNameStr == "header" || tagNameStr == "footer" ||
                        tagNameStr == "aside" || tagNameStr == "nav" || tagNameStr == "main" || tagNameStr == "tr" ||
                        tagNameStr == "td" || tagNameStr == "ul" || tagNameStr == "ol" || tagNameStr == "blockquote") {
                        is_block = true;
                    }
                }
            } else if (i + 2 < length) {
                size_t nameStart = i + 1;
                size_t nameLen = 0;
                while (nameStart + nameLen < length && data[nameStart + nameLen] != '>' && data[nameStart + nameLen] != '/' && data[nameStart + nameLen] != ' ' && data[nameStart + nameLen] != '\t' && data[nameStart + nameLen] != '\r' && data[nameStart + nameLen] != '\n') {
                    nameLen++;
                }
                if (nameLen > 0) {
                    std::string tagNameStr(reinterpret_cast<const char*>(data + nameStart), nameLen);
                    for (char &ch : tagNameStr) ch = toLowerByte(ch);
                    if (tagNameStr == "br" || tagNameStr == "hr") {
                        is_block = true;
                    }
                }
            }
            
            if (is_block) {
                if (!result.empty() && result.back() != '\n') {
                    while (!result.empty() && result.back() == ' ') {
                        result.pop_back();
                    }
                    result += '\n';
                }
            }
            
            i++;
            continue;
        }
        
        if (in_tag) {
            if (c == '>') {
                in_tag = false;
            }
            i++;
            continue;
        }
        
        if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
            if (!result.empty() && result.back() != ' ' && result.back() != '\n') {
                result += ' ';
            }
            i++;
        } else {
            result += static_cast<char>(c);
            i++;
        }
    }
    
    while (!result.empty() && (result.back() == ' ' || result.back() == '\n' || result.back() == '\r')) {
        result.pop_back();
    }
    return result;
}

std::vector<std::string> extractHrefsCpp(const uint8_t* data, size_t length) {
    std::vector<std::string> links;
    size_t i = 0;
    const bool useAvx2 = hasAVX2();
    
    while (i < length) {
        if (useAvx2 && i + 31 < length) {
            __m256i chunk = _mm256_loadu_si256(reinterpret_cast<const __m256i*>(data + i));
            __m256i target = _mm256_set1_epi8('<');
            int mask = _mm256_movemask_epi8(_mm256_cmpeq_epi8(chunk, target));
            if (mask == 0) {
                i += 32;
                continue;
            } else {
                unsigned long index = 32;
#ifdef _MSC_VER
                _BitScanForward(&index, mask);
#else
                index = __builtin_ctz(mask);
#endif
                if (index > 0) {
                    i += index;
                    continue;
                }
            }
        }
        
        if (data[i] == '<') {
            // Support <a> tags with no space, or with standard newlines, tabs, and self-closing slashes
            if (i + 2 < length && (toLowerByte(data[i + 1]) == 'a') && 
                (data[i + 2] == '>' || data[i + 2] == '/' || data[i + 2] == ' ' || data[i + 2] == '\t' || data[i + 2] == '\r' || data[i + 2] == '\n')) {
                size_t tag_start = i;
                size_t tag_end = tag_start;
                for (size_t j = tag_start; j < length; j++) {
                    if (data[j] == '>') {
                        tag_end = j;
                        break;
                    }
                }
                
                if (tag_end > tag_start) {
                    for (size_t k = tag_start + 2; k + 5 < tag_end; k++) {
                        // Ensure href attribute name is preceded by standard boundaries, preventing class-name matching
                        if (toLowerByte(data[k]) == 'h' &&
                            toLowerByte(data[k + 1]) == 'r' &&
                            toLowerByte(data[k + 2]) == 'e' &&
                            toLowerByte(data[k + 3]) == 'f' &&
                            data[k + 4] == '=' &&
                            (data[k - 1] == ' ' || data[k - 1] == '\t' || data[k - 1] == '\r' || data[k - 1] == '\n' || data[k - 1] == 'a' || data[k - 1] == 'A')) {
                            
                            size_t val_start = k + 5;
                            while (val_start < tag_end && (data[val_start] == ' ' || data[val_start] == '\t')) {
                                val_start++;
                            }
                            
                            if (val_start < tag_end) {
                                char quote = data[val_start];
                                if (quote == '"' || quote == '\'') {
                                    val_start++;
                                    size_t val_end = val_start;
                                    while (val_end < tag_end && data[val_end] != quote) {
                                        val_end++;
                                    }
                                    if (val_end < tag_end) {
                                        links.push_back(std::string(reinterpret_cast<const char*>(data + val_start), val_end - val_start));
                                    }
                                    k = val_end;
                                } else {
                                    size_t val_end = val_start;
                                    while (val_end < tag_end && data[val_end] != ' ' && data[val_end] != '\t') {
                                        val_end++;
                                    }
                                    links.push_back(std::string(reinterpret_cast<const char*>(data + val_start), val_end - val_start));
                                    k = val_end;
                                }
                            }
                            break;
                        }
                    }
                    i = tag_end + 1;
                    continue;
                }
            }
        }
        i++;
    }
    return links;
}

// ============================================================================
// JNI Implementations (GC-Locked and Thread-Safe)
// ============================================================================

extern "C" {

JNIEXPORT jobject JNICALL Java_fastwebspider_FastWebSpiderImpl_nativeFetch(
    JNIEnv* env, jobject obj, jstring urlStr) {

    if (urlStr == nullptr) return nullptr;

    // 1. Convert URL to wide string for WinHTTP
    const jchar* jUrl = env->GetStringChars(urlStr, nullptr);
    jsize urlLen = env->GetStringLength(urlStr);
    std::wstring wUrl(reinterpret_cast<const wchar_t*>(jUrl), urlLen);
    env->ReleaseStringChars(urlStr, jUrl);

    // 2. Parse URL components using WinHttpCrackUrl
    URL_COMPONENTS urlComp;
    ZeroMemory(&urlComp, sizeof(urlComp));
    urlComp.dwStructSize = sizeof(urlComp);
    urlComp.dwSchemeLength = (DWORD)-1;
    urlComp.dwHostNameLength = (DWORD)-1;
    urlComp.dwUrlPathLength = (DWORD)-1;
    urlComp.dwExtraInfoLength = (DWORD)-1;

    DWORD dwStatusCode = 0;
    std::vector<char> responseBuffer;
    long long fetchTimeMs = 0;

    if (WinHttpCrackUrl(wUrl.c_str(), (DWORD)wUrl.length(), 0, &urlComp)) {
        std::wstring host(urlComp.lpszHostName, urlComp.dwHostNameLength);
        std::wstring path(urlComp.lpszUrlPath, urlComp.dwUrlPathLength + urlComp.dwExtraInfoLength);
        if (path.empty()) {
            path = L"/";
        }

        // Measure performance duration
        auto startTime = std::chrono::high_resolution_clock::now();

        HINTERNET hConnect = nullptr;
        HINTERNET hRequest = nullptr;

        if (g_hSession) {
            hConnect = WinHttpConnect(g_hSession, host.c_str(), urlComp.nPort, 0);
        }

        if (hConnect) {
            DWORD dwFlags = 0;
            if (urlComp.nScheme == INTERNET_SCHEME_HTTPS) {
                dwFlags |= WINHTTP_FLAG_SECURE;
            }

            hRequest = WinHttpOpenRequest(hConnect, L"GET", path.c_str(),
                                          NULL, WINHTTP_NO_REFERER,
                                          WINHTTP_DEFAULT_ACCEPT_TYPES,
                                          dwFlags);
        }

        if (hRequest) {
            // Set reasonable short crawler timeouts: 5 seconds
            WinHttpSetTimeouts(hRequest, 5000, 5000, 5000, 5000);

            BOOL bResults = WinHttpSendRequest(hRequest,
                                               WINHTTP_NO_ADDITIONAL_HEADERS, 0,
                                               WINHTTP_NO_REQUEST_DATA, 0,
                                               0, 0);

            if (bResults) {
                bResults = WinHttpReceiveResponse(hRequest, NULL);
            }

            if (bResults) {
                DWORD dwSize = sizeof(dwStatusCode);
                WinHttpQueryHeaders(hRequest, 
                                    WINHTTP_QUERY_STATUS_CODE | WINHTTP_QUERY_FLAG_NUMBER,
                                    WINHTTP_HEADER_NAME_BY_INDEX, 
                                    &dwStatusCode, &dwSize, 
                                    WINHTTP_NO_HEADER_INDEX);

                // Read body chunks
                DWORD dwSizeAvailable = 0;
                do {
                    if (!WinHttpQueryDataAvailable(hRequest, &dwSizeAvailable)) {
                        break;
                    }
                    if (dwSizeAvailable == 0) {
                        break;
                    }

                    size_t currentOffset = responseBuffer.size();
                    responseBuffer.resize(currentOffset + dwSizeAvailable);

                    DWORD dwBytesRead = 0;
                    if (!WinHttpReadData(hRequest, &responseBuffer[currentOffset], dwSizeAvailable, &dwBytesRead)) {
                        responseBuffer.resize(currentOffset); // rollback
                        break;
                    }
                    if (dwBytesRead < dwSizeAvailable) {
                        responseBuffer.resize(currentOffset + dwBytesRead);
                    }
                } while (dwSizeAvailable > 0);
            } else {
                // If connection failed (e.g. DNS or Timeout), status is 503
                dwStatusCode = 503; 
            }
        } else {
            dwStatusCode = 500;
        }

        // Close Handles
        if (hRequest) WinHttpCloseHandle(hRequest);
        if (hConnect) WinHttpCloseHandle(hConnect);

        auto endTime = std::chrono::high_resolution_clock::now();
        fetchTimeMs = std::chrono::duration_cast<std::chrono::milliseconds>(endTime - startTime).count();
    } else {
        // Bad URL format
        dwStatusCode = 400;
    }

    // 3. Map values to Java record: fastwebspider.fastwebspider.SpiderResponse
    jbyteArray bodyArray = env->NewByteArray((jsize)responseBuffer.size());
    if (!responseBuffer.empty()) {
        env->SetByteArrayRegion(bodyArray, 0, (jsize)responseBuffer.size(), reinterpret_cast<const jbyte*>(responseBuffer.data()));
    }

    jclass respClass = env->FindClass("fastwebspider/FastWebSpider$SpiderResponse");
    if (!respClass) {
        return nullptr;
    }
    jmethodID constructor = env->GetMethodID(respClass, "<init>", "(I[BJ)V");
    if (!constructor) {
        return nullptr;
    }

    jobject respObj = env->NewObject(respClass, constructor, (jint)dwStatusCode, bodyArray, (jlong)fetchTimeMs);
    return respObj;
}

JNIEXPORT jstring JNICALL Java_fastwebspider_FastWebSpiderImpl_nativeExtractCleanText(
    JNIEnv* env, jobject obj, jbyteArray htmlData) {

    if (htmlData == nullptr) return env->NewStringUTF("");
    jsize len = env->GetArrayLength(htmlData);
    if (len == 0) return env->NewStringUTF("");

    void* bytes = env->GetPrimitiveArrayCritical(htmlData, nullptr);
    if (!bytes) return env->NewStringUTF("");

    std::string text = extractCleanTextCpp(reinterpret_cast<const uint8_t*>(bytes), len);
    
    env->ReleasePrimitiveArrayCritical(htmlData, bytes, JNI_ABORT);
    return env->NewStringUTF(text.c_str());
}

JNIEXPORT jobjectArray JNICALL Java_fastwebspider_FastWebSpiderImpl_nativeExtractHrefs(
    JNIEnv* env, jobject obj, jbyteArray htmlData) {

    jclass stringClazz = env->FindClass("java/lang/String");
    if (htmlData == nullptr) return env->NewObjectArray(0, stringClazz, nullptr);
    jsize len = env->GetArrayLength(htmlData);
    if (len == 0) return env->NewObjectArray(0, stringClazz, nullptr);

    void* bytes = env->GetPrimitiveArrayCritical(htmlData, nullptr);
    if (!bytes) {
        return env->NewObjectArray(0, stringClazz, nullptr);
    }

    std::vector<std::string> links = extractHrefsCpp(reinterpret_cast<const uint8_t*>(bytes), len);
    env->ReleasePrimitiveArrayCritical(htmlData, bytes, JNI_ABORT);

    jobjectArray array = env->NewObjectArray(static_cast<jsize>(links.size()), stringClazz, nullptr);
    for (size_t i = 0; i < links.size(); i++) {
        jstring val = env->NewStringUTF(links[i].c_str());
        env->SetObjectArrayElement(array, static_cast<jsize>(i), val);
        env->DeleteLocalRef(val);
    }
    return array;
}

}
