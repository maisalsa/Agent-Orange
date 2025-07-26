#include <jni.h>
#include "LlamaJNI.h"
#include <string.h>
#include <stdlib.h>
#include <stdio.h>

/**
 * LlamaJNI.c - JNI Implementation for llama.cpp Integration
 * 
 * This file implements the JNI bridge between Java and the llama.cpp library.
 * It handles string conversions, memory management, and error handling.
 * 
 * LINKING REQUIREMENTS:
 * - Link against llama.cpp library: -lllama
 * - Include llama.cpp headers for model management
 * - Compile with: g++ -fPIC -shared -o libllama.so llama.cpp LlamaJNI.c
 * 
 * MEMORY MANAGEMENT:
 * - JNI strings are managed by the JVM (automatic cleanup)
 * - Native strings from llama_infer() must be freed by the caller
 * - All GetStringUTFChars calls are paired with ReleaseStringUTFChars
 * - Error conditions are handled with proper cleanup
 * 
 * ERROR HANDLING:
 * - Validates all input parameters
 * - Handles memory allocation failures
 * - Provides meaningful error messages
 * - Ensures cleanup on all error paths
 */

// External function declaration - this should be implemented in llama.cpp
// The function should return a dynamically allocated string that the caller must free
extern const char* llama_infer(const char* prompt);

// External function to free memory allocated by llama_infer
// This should be implemented in llama.cpp to match the allocation method used
extern void llama_free_response(const char* response);

/**
 * JNI implementation for LLM inference.
 * 
 * This function:
 * 1. Validates the input prompt
 * 2. Converts Java string to C string
 * 3. Calls the native llama_infer function
 * 4. Converts the response back to Java string
 * 5. Ensures proper cleanup of all resources
 * 
 * @param env JNI environment pointer
 * @param obj Java object reference (unused)
 * @param prompt Java string containing the input prompt
 * @return Java string containing the LLM response or error message
 */
JNIEXPORT jstring JNICALL Java_LlamaJNI_generateResponse(JNIEnv *env, jobject obj, jstring prompt) {
    const char *c_prompt = NULL;
    const char *c_response = NULL;
    jstring result = NULL;
    
    // Step 1: Validate input parameter
    if (prompt == NULL) {
        return (*env)->NewStringUTF(env, "[ERROR] Null prompt parameter");
    }
    
    // Step 2: Convert Java string to C string
    // GetStringUTFChars returns NULL if conversion fails
    c_prompt = (*env)->GetStringUTFChars(env, prompt, NULL);
    if (c_prompt == NULL) {
        return (*env)->NewStringUTF(env, "[ERROR] Failed to convert prompt string");
    }
    
    // Step 3: Validate C string content
    if (strlen(c_prompt) == 0) {
        // Release the string before returning error
        (*env)->ReleaseStringUTFChars(env, prompt, c_prompt);
        return (*env)->NewStringUTF(env, "[ERROR] Empty prompt string");
    }
    
    // Step 4: Call the native LLM inference function
    c_response = llama_infer(c_prompt);
    
    // Step 5: Release the input string (always do this, regardless of success/failure)
    (*env)->ReleaseStringUTFChars(env, prompt, c_prompt);
    c_prompt = NULL; // Prevent accidental reuse
    
    // Step 6: Handle the response
    if (c_response == NULL) {
        return (*env)->NewStringUTF(env, "[ERROR] LLM inference returned null response");
    }
    
    // Step 7: Convert response to Java string
    result = (*env)->NewStringUTF(env, c_response);
    if (result == NULL) {
        // Failed to create Java string - this is a serious error
        // Free the native response before returning error
        if (llama_free_response != NULL) {
            llama_free_response(c_response);
        }
        return (*env)->NewStringUTF(env, "[ERROR] Failed to convert response to Java string");
    }
    
    // Step 8: Free the native response string
    // This is crucial to prevent memory leaks
    if (llama_free_response != NULL) {
        llama_free_response(c_response);
    } else {
        // Fallback: if llama_free_response is not available, use free()
        // WARNING: This assumes llama_infer used malloc() - adjust if different
        free((void*)c_response);
    }
    
    return result;
}

/**
 * Alternative implementation if llama_free_response is not available.
 * 
 * This version assumes that llama_infer() returns a string that should be freed
 * with the standard C free() function. If your llama.cpp implementation uses
 * a different memory management scheme, you'll need to adjust this.
 * 
 * UNCOMMENT AND USE THIS VERSION IF llama_free_response IS NOT AVAILABLE:
 */
/*
JNIEXPORT jstring JNICALL Java_LlamaJNI_generateResponse(JNIEnv *env, jobject obj, jstring prompt) {
    const char *c_prompt = NULL;
    const char *c_response = NULL;
    jstring result = NULL;
    
    // Validate input
    if (prompt == NULL) {
        return (*env)->NewStringUTF(env, "[ERROR] Null prompt parameter");
    }
    
    // Convert Java string to C string
    c_prompt = (*env)->GetStringUTFChars(env, prompt, NULL);
    if (c_prompt == NULL) {
        return (*env)->NewStringUTF(env, "[ERROR] Failed to convert prompt string");
    }
    
    // Validate content
    if (strlen(c_prompt) == 0) {
        (*env)->ReleaseStringUTFChars(env, prompt, c_prompt);
        return (*env)->NewStringUTF(env, "[ERROR] Empty prompt string");
    }
    
    // Call LLM inference
    c_response = llama_infer(c_prompt);
    
    // Always release the input string
    (*env)->ReleaseStringUTFChars(env, prompt, c_prompt);
    c_prompt = NULL;
    
    // Handle response
    if (c_response == NULL) {
        return (*env)->NewStringUTF(env, "[ERROR] LLM inference returned null response");
    }
    
    // Convert to Java string
    result = (*env)->NewStringUTF(env, c_response);
    if (result == NULL) {
        // Free native response before returning error
        free((void*)c_response);
        return (*env)->NewStringUTF(env, "[ERROR] Failed to convert response to Java string");
    }
    
    // Free the native response (assuming malloc was used)
    free((void*)c_response);
    
    return result;
}
*/ 