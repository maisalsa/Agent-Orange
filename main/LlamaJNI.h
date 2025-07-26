#ifndef _Included_LlamaJNI
#define _Included_LlamaJNI
#ifdef __cplusplus
extern "C" {
#endif
#include <jni.h>

JNIEXPORT jstring JNICALL Java_LlamaJNI_generateResponse(JNIEnv *, jobject, jstring);

#ifdef __cplusplus
}
#endif
#endif 