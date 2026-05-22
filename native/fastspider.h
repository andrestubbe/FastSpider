#ifndef FASTSPIDER_H
#define FASTSPIDER_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

// Export JNI declarations for FastSpiderImpl

JNIEXPORT jobject JNICALL Java_fastspider_FastSpiderImpl_nativeFetch(
    JNIEnv* env, jobject obj, jstring urlStr);

JNIEXPORT jstring JNICALL Java_fastspider_FastSpiderImpl_nativeExtractCleanText(
    JNIEnv* env, jobject obj, jbyteArray htmlData);

JNIEXPORT jobjectArray JNICALL Java_fastspider_FastSpiderImpl_nativeExtractHrefs(
    JNIEnv* env, jobject obj, jbyteArray htmlData);

#ifdef __cplusplus
}
#endif

#endif // FASTSPIDER_H
