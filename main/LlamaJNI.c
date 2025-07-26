#include <jni.h>
#include "LlamaJNI.h"
#include <string.h>

// Assume this function is implemented in llama.cpp and linked with libllama.so
extern const char* llama_infer(const char* prompt);

JNIEXPORT jstring JNICALL Java_LlamaJNI_generateResponse(JNIEnv *env, jobject obj, jstring prompt) {
    if (prompt == NULL) {
        return (*env)->NewStringUTF(env, "[ERROR] Null prompt");
    }
    const char *c_prompt = (*env)->GetStringUTFChars(env, prompt, 0);
    if (c_prompt == NULL) {
        return (*env)->NewStringUTF(env, "[ERROR] Failed to get prompt string");
    }

    // Call the native LLM inference function
    const char *c_response = llama_infer(c_prompt);

    // Release the Java string
    (*env)->ReleaseStringUTFChars(env, prompt, c_prompt);

    if (c_response == NULL) {
        return (*env)->NewStringUTF(env, "[ERROR] LLM returned null response");
    }

    // Return the response as a Java string
    return (*env)->NewStringUTF(env, c_response);
} 