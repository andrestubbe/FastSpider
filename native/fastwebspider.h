#ifndef fastwebspider_H
#define fastwebspider_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

// Export JNI declarations for fastwebspiderImpl

JNIEXPORT jobject JNICALL Java_fastwebspider_FastWebSpiderImpl_nativeFetch(
    JNIEnv* env, jobject obj, jstring urlStr);

JNIEXPORT jstring JNICALL Java_fastwebspider_FastWebSpiderImpl_nativeExtractCleanText(
    JNIEnv* env, jobject obj, jbyteArray htmlData);

JNIEXPORT jobjectArray JNICALL Java_fastwebspider_FastWebSpiderImpl_nativeExtractHrefs(
    JNIEnv* env, jobject obj, jbyteArray htmlData);

#ifdef __cplusplus
}
#endif

#endif // fastwebspider_H
