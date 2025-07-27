#ifndef _Included_LlamaJNI
#define _Included_LlamaJNI

/**
 * LlamaJNI.h - JNI Header for llama.cpp Integration
 * 
 * This header file declares the JNI functions for integrating with llama.cpp.
 * It should be generated using: javah -jni LlamaJNI
 * 
 * LINKING REQUIREMENTS:
 * - Include this header in your C/C++ implementation
 * - Link against the llama.cpp library
 * - Ensure JNI headers are in your include path
 * 
 * BUILD COMMANDS:
 * 1. Generate header: javah -jni LlamaJNI
 * 2. Compile: g++ -fPIC -shared -I$JAVA_HOME/include -I$JAVA_HOME/include/linux -o libllama.so llama.cpp LlamaJNI.c
 * 3. For macOS: g++ -fPIC -shared -I$JAVA_HOME/include -I$JAVA_HOME/include/darwin -o libllama.dylib llama.cpp LlamaJNI.c
 * 4. For Windows: g++ -shared -I"%JAVA_HOME%\include" -I"%JAVA_HOME%\include\win32" -o llama.dll llama.cpp LlamaJNI.c
 */

#ifdef __cplusplus
extern "C" {
#endif

#include <jni.h>

/**
 * JNI function to generate LLM response using llama.cpp
 * 
 * This function is called from Java to perform LLM inference.
 * It handles string conversion between Java and C, calls the native
 * llama_infer function, and manages memory properly.
 * 
 * @param env JNI environment pointer
 * @param obj Java object reference (unused)
 * @param prompt Java string containing the input prompt
 * @return Java string containing the LLM response or error message
 */
JNIEXPORT jstring JNICALL Java_com_example_LlamaJNI_generateResponse(JNIEnv *, jobject, jstring);

#ifdef __cplusplus
}
#endif

#endif 