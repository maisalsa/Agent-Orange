/**
 * llama_jni.cpp - Robust JNI Implementation for llama.cpp Integration
 *
 * This file provides a thread-safe, production-ready JNI bridge between Java
 * and the llama.cpp library. It implements proper resource management, error
 * handling, and model lifecycle management.
 *
 * FEATURES:
 * - Thread-safe model loading and inference
 * - Proper resource cleanup and memory management
 * - Comprehensive error handling and logging
 * - Model caching to prevent reloading on every call
 * - Configurable model parameters and inference settings
 * - Graceful degradation on errors
 *
 * DEPENDENCIES:
 * - llama.cpp library (libllama)
 * - JNI headers (jni.h)
 * - C++ standard library
 *
 * BUILD COMMANDS:
 * Linux:   g++ -fPIC -shared -I$JAVA_HOME/include -I$JAVA_HOME/include/linux -lllama -o libllama.so llama_jni.cpp
 * macOS:   g++ -fPIC -shared -I$JAVA_HOME/include -I$JAVA_HOME/include/darwin -lllama -o libllama.dylib llama_jni.cpp
 * Windows: g++ -shared -I"%JAVA_HOME%\include" -I"%JAVA_HOME%\include\win32" -lllama -o llama.dll llama_jni.cpp
 *
 * USAGE:
 * The Java code should call generateResponse() with a prompt string.
 * The function returns the generated response or an error message.
 */

#include <iostream>
#include <string>
#include <mutex>
#include <memory>
#include <vector>
#include <stdexcept>
#include <cstring>
#include <cstdlib>
#include <chrono>
#include <thread>

// JNI headers
#include "com_example_LlamaJNI.h"

// llama.cpp headers
#include "llama.h"

// ============================================================================
// GLOBAL STATE AND CONFIGURATION
// ============================================================================

/**
 * Global model state - shared across all threads
 * This structure holds the loaded model and context for efficient reuse
 */
struct ModelState {
    llama_model* model = nullptr;
    llama_context* ctx = nullptr;
    std::string model_path;
    bool is_initialized = false;
    std::chrono::steady_clock::time_point last_used;
    
    // Model configuration
    int max_tokens = 2048;
    float temperature = 0.7f;
    float top_p = 0.9f;
    int top_k = 40;
    float repeat_penalty = 1.1f;
    
    // Thread safety
    std::mutex model_mutex;
    std::mutex inference_mutex;
};

// Global model state instance
static std::unique_ptr<ModelState> g_model_state = nullptr;
static std::mutex g_init_mutex;

// Configuration constants
static constexpr const char* DEFAULT_MODEL_PATH = "models/llama-2-7b-chat.gguf";
static constexpr const char* DEFAULT_SYSTEM_PROMPT = "You are a helpful assistant. Respond concisely and accurately.";
static constexpr int MAX_PROMPT_LENGTH = 8192;
static constexpr int MAX_RESPONSE_LENGTH = 4096;
static constexpr int MODEL_TIMEOUT_MS = 30000; // 30 seconds

// ============================================================================
// ERROR HANDLING AND LOGGING
// ============================================================================

/**
 * Error codes for different failure scenarios
 */
enum class ErrorCode {
    SUCCESS = 0,
    MODEL_LOAD_FAILED,
    CONTEXT_CREATION_FAILED,
    INVALID_PROMPT,
    TOKENIZATION_FAILED,
    INFERENCE_FAILED,
    MEMORY_ALLOCATION_FAILED,
    TIMEOUT_ERROR,
    THREAD_ERROR,
    UNKNOWN_ERROR
};

/**
 * Convert error code to human-readable message
 */
std::string getErrorMessage(ErrorCode code) {
    switch (code) {
        case ErrorCode::SUCCESS:
            return "Success";
        case ErrorCode::MODEL_LOAD_FAILED:
            return "Failed to load LLM model - check model path and file permissions";
        case ErrorCode::CONTEXT_CREATION_FAILED:
            return "Failed to create inference context - insufficient memory or invalid model";
        case ErrorCode::INVALID_PROMPT:
            return "Invalid or empty prompt provided";
        case ErrorCode::TOKENIZATION_FAILED:
            return "Failed to tokenize input prompt";
        case ErrorCode::INFERENCE_FAILED:
            return "Inference failed during text generation";
        case ErrorCode::MEMORY_ALLOCATION_FAILED:
            return "Memory allocation failed during processing";
        case ErrorCode::TIMEOUT_ERROR:
            return "Inference timed out - model may be too large or system overloaded";
        case ErrorCode::THREAD_ERROR:
            return "Thread safety error - concurrent access detected";
        case ErrorCode::UNKNOWN_ERROR:
        default:
            return "Unknown error occurred during processing";
    }
}

/**
 * Log error message with timestamp and context
 */
void logError(const std::string& message, ErrorCode code = ErrorCode::UNKNOWN_ERROR) {
    auto now = std::chrono::system_clock::now();
    auto time_t = std::chrono::system_clock::to_time_t(now);
    std::cerr << "[llama_jni] ERROR [" << std::ctime(&time_t) << "] " 
              << message << " (Code: " << static_cast<int>(code) << ")" << std::endl;
}

/**
 * Log info message with timestamp
 */
void logInfo(const std::string& message) {
    auto now = std::chrono::system_clock::now();
    auto time_t = std::chrono::system_clock::to_time_t(now);
    std::cout << "[llama_jni] INFO [" << std::ctime(&time_t) << "] " << message << std::endl;
}

// ============================================================================
// MODEL MANAGEMENT
// ============================================================================

/**
 * Initialize llama.cpp library and set up global parameters
 * This should be called once at startup
 */
bool initializeLlamaLibrary() {
    try {
        // Initialize llama.cpp library
        llama_backend_init(false);
        logInfo("llama.cpp library initialized successfully");
        return true;
    } catch (const std::exception& e) {
        logError("Failed to initialize llama.cpp library: " + std::string(e.what()));
        return false;
    }
}

/**
 * Load the LLM model from file
 * This function handles model loading with proper error checking
 */
ErrorCode loadModel(const std::string& model_path) {
    if (g_model_state == nullptr) {
        logError("Model state not initialized", ErrorCode::MODEL_LOAD_FAILED);
        return ErrorCode::MODEL_LOAD_FAILED;
    }
    
    std::lock_guard<std::mutex> lock(g_model_state->model_mutex);
    
    try {
        logInfo("Loading model from: " + model_path);
        
        // Check if model file exists and is readable
        FILE* test_file = fopen(model_path.c_str(), "rb");
        if (!test_file) {
            logError("Model file not found or not readable: " + model_path, ErrorCode::MODEL_LOAD_FAILED);
            return ErrorCode::MODEL_LOAD_FAILED;
        }
        fclose(test_file);
        
        // Configure model parameters
        llama_model_params model_params = llama_model_default_params();
        model_params.n_gpu_layers = 0; // CPU-only for now, can be made configurable
        
        // Load the model
        g_model_state->model = llama_load_model_from_file(model_path.c_str(), model_params);
        if (!g_model_state->model) {
            logError("llama_load_model_from_file failed for: " + model_path, ErrorCode::MODEL_LOAD_FAILED);
            return ErrorCode::MODEL_LOAD_FAILED;
        }
        
        // Configure context parameters
        llama_context_params ctx_params = llama_context_default_params();
        ctx_params.seed = 42; // Fixed seed for reproducible results
        ctx_params.n_ctx = 4096; // Context window size
        ctx_params.n_batch = 512; // Batch size for processing
        ctx_params.n_threads = std::thread::hardware_concurrency(); // Use all available CPU cores
        ctx_params.n_threads_batch = std::thread::hardware_concurrency();
        
        // Create inference context
        g_model_state->ctx = llama_new_context_with_model(g_model_state->model, ctx_params);
        if (!g_model_state->ctx) {
            logError("llama_new_context_with_model failed", ErrorCode::CONTEXT_CREATION_FAILED);
            llama_free_model(g_model_state->model);
            g_model_state->model = nullptr;
            return ErrorCode::CONTEXT_CREATION_FAILED;
        }
        
        g_model_state->model_path = model_path;
        g_model_state->is_initialized = true;
        g_model_state->last_used = std::chrono::steady_clock::now();
        
        logInfo("Model loaded successfully. Context size: " + std::to_string(ctx_params.n_ctx) + 
                ", Threads: " + std::to_string(ctx_params.n_threads));
        
        return ErrorCode::SUCCESS;
        
    } catch (const std::exception& e) {
        logError("Exception during model loading: " + std::string(e.what()), ErrorCode::MODEL_LOAD_FAILED);
        return ErrorCode::MODEL_LOAD_FAILED;
    }
}

/**
 * Clean up model resources
 * This function ensures proper cleanup of llama.cpp resources
 */
void cleanupModel() {
    if (g_model_state == nullptr) {
        return;
    }
    
    std::lock_guard<std::mutex> lock(g_model_state->model_mutex);
    
    try {
        if (g_model_state->ctx) {
            llama_free_context(g_model_state->ctx);
            g_model_state->ctx = nullptr;
            logInfo("Context freed");
        }
        
        if (g_model_state->model) {
            llama_free_model(g_model_state->model);
            g_model_state->model = nullptr;
            logInfo("Model freed");
        }
        
        g_model_state->is_initialized = false;
        
    } catch (const std::exception& e) {
        logError("Exception during model cleanup: " + std::string(e.what()));
    }
}

/**
 * Initialize global model state
 * This function sets up the global model state and loads the model
 */
ErrorCode initializeModelState(const std::string& model_path = DEFAULT_MODEL_PATH) {
    std::lock_guard<std::mutex> lock(g_init_mutex);
    
    // Check if already initialized
    if (g_model_state && g_model_state->is_initialized) {
        logInfo("Model already initialized, skipping");
        return ErrorCode::SUCCESS;
    }
    
    // Initialize llama.cpp library
    if (!initializeLlamaLibrary()) {
        return ErrorCode::MODEL_LOAD_FAILED;
    }
    
    // Create model state
    g_model_state = std::make_unique<ModelState>();
    
    // Load the model
    ErrorCode result = loadModel(model_path);
    if (result != ErrorCode::SUCCESS) {
        g_model_state.reset();
        return result;
    }
    
    logInfo("Model state initialized successfully");
    return ErrorCode::SUCCESS;
}

// ============================================================================
// INFERENCE ENGINE
// ============================================================================

/**
 * Tokenize input text using the loaded model
 * This function handles tokenization with proper error checking
 */
ErrorCode tokenizeInput(const std::string& input, std::vector<llama_token>& tokens) {
    if (!g_model_state || !g_model_state->is_initialized || !g_model_state->ctx) {
        return ErrorCode::MODEL_LOAD_FAILED;
    }
    
    try {
        // Tokenize the input text
        tokens = llama_tokenize(g_model_state->ctx, input, true, true);
        
        if (tokens.empty()) {
            logError("Tokenization resulted in empty token list", ErrorCode::TOKENIZATION_FAILED);
            return ErrorCode::TOKENIZATION_FAILED;
        }
        
        // Check if input is too long
        if (tokens.size() > g_model_state->max_tokens) {
            logError("Input too long: " + std::to_string(tokens.size()) + " tokens (max: " + 
                    std::to_string(g_model_state->max_tokens) + ")", ErrorCode::INVALID_PROMPT);
            return ErrorCode::INVALID_PROMPT;
        }
        
        return ErrorCode::SUCCESS;
        
    } catch (const std::exception& e) {
        logError("Exception during tokenization: " + std::string(e.what()), ErrorCode::TOKENIZATION_FAILED);
        return ErrorCode::TOKENIZATION_FAILED;
    }
}

/**
 * Perform inference to generate response
 * This function handles the core text generation with proper error checking
 */
ErrorCode performInference(const std::vector<llama_token>& input_tokens, std::string& output_text) {
    if (!g_model_state || !g_model_state->is_initialized || !g_model_state->ctx) {
        return ErrorCode::MODEL_LOAD_FAILED;
    }
    
    std::lock_guard<std::mutex> lock(g_model_state->inference_mutex);
    
    try {
        auto start_time = std::chrono::steady_clock::now();
        
        // Evaluate the input tokens
        int eval_result = llama_eval(g_model_state->ctx, input_tokens.data(), input_tokens.size(), 0, 1);
        if (eval_result != 0) {
            logError("llama_eval failed with code: " + std::to_string(eval_result), ErrorCode::INFERENCE_FAILED);
            return ErrorCode::INFERENCE_FAILED;
        }
        
        // Configure sampling parameters
        llama_sampling_params sampling_params = {};
        sampling_params.temp = g_model_state->temperature;
        sampling_params.top_p = g_model_state->top_p;
        sampling_params.top_k = g_model_state->top_k;
        sampling_params.repeat_penalty = g_model_state->repeat_penalty;
        sampling_params.mirostat = 0; // Disable mirostat for simplicity
        
        // Generate response tokens
        output_text.clear();
        int generated_tokens = 0;
        const int max_generated_tokens = g_model_state->max_tokens - input_tokens.size();
        
        while (generated_tokens < max_generated_tokens) {
            // Check for timeout
            auto current_time = std::chrono::steady_clock::now();
            auto elapsed = std::chrono::duration_cast<std::chrono::milliseconds>(current_time - start_time);
            if (elapsed.count() > MODEL_TIMEOUT_MS) {
                logError("Inference timed out after " + std::to_string(MODEL_TIMEOUT_MS) + "ms", ErrorCode::TIMEOUT_ERROR);
                return ErrorCode::TIMEOUT_ERROR;
            }
            
            // Sample next token
            llama_token new_token = llama_sample_token(g_model_state->ctx, &sampling_params);
            
            // Check for end of sequence
            if (new_token == llama_token_eos(g_model_state->ctx)) {
                break;
            }
            
            // Convert token to string and append
            const char* token_str = llama_token_to_str(g_model_state->ctx, new_token);
            if (token_str) {
                output_text += token_str;
            }
            
            // Evaluate the new token
            eval_result = llama_eval(g_model_state->ctx, &new_token, 1, llama_n_tokens(g_model_state->ctx), 1);
            if (eval_result != 0) {
                logError("llama_eval failed during generation", ErrorCode::INFERENCE_FAILED);
                return ErrorCode::INFERENCE_FAILED;
            }
            
            generated_tokens++;
        }
        
        // Update last used timestamp
        g_model_state->last_used = std::chrono::steady_clock::now();
        
        auto end_time = std::chrono::steady_clock::now();
        auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(end_time - start_time);
        logInfo("Inference completed in " + std::to_string(duration.count()) + "ms, generated " + 
                std::to_string(generated_tokens) + " tokens");
        
        return ErrorCode::SUCCESS;
        
    } catch (const std::exception& e) {
        logError("Exception during inference: " + std::string(e.what()), ErrorCode::INFERENCE_FAILED);
        return ErrorCode::INFERENCE_FAILED;
    }
}

// ============================================================================
// JNI IMPLEMENTATION
// ============================================================================

/**
 * JNI function to generate LLM response
 * This is the main entry point called from Java
 */
extern "C" JNIEXPORT jstring JNICALL Java_com_example_LlamaJNI_generateResponse(JNIEnv *env, jobject obj, jstring prompt) {
    // Validate input
    if (prompt == nullptr) {
        logError("Null prompt received from Java", ErrorCode::INVALID_PROMPT);
        return env->NewStringUTF("[ERROR] Null prompt parameter");
    }
    
    // Convert Java string to C++ string
    const char* prompt_cstr = env->GetStringUTFChars(prompt, nullptr);
    if (prompt_cstr == nullptr) {
        logError("Failed to convert Java string to C string", ErrorCode::INVALID_PROMPT);
        return env->NewStringUTF("[ERROR] Failed to convert prompt string");
    }
    
    std::string cpp_prompt(prompt_cstr);
    env->ReleaseStringUTFChars(prompt, prompt_cstr);
    
    // Validate prompt content
    if (cpp_prompt.empty() || cpp_prompt.length() > MAX_PROMPT_LENGTH) {
        logError("Invalid prompt: empty or too long (" + std::to_string(cpp_prompt.length()) + " chars)", 
                ErrorCode::INVALID_PROMPT);
        return env->NewStringUTF("[ERROR] Invalid prompt: empty or too long");
    }
    
    try {
        // Initialize model state if not already done
        ErrorCode init_result = initializeModelState();
        if (init_result != ErrorCode::SUCCESS) {
            std::string error_msg = "[ERROR] " + getErrorMessage(init_result);
            logError("Model initialization failed: " + error_msg, init_result);
            return env->NewStringUTF(error_msg.c_str());
        }
        
        // Prepare the full prompt with system message
        std::string full_prompt = std::string(DEFAULT_SYSTEM_PROMPT) + "\n\nUser: " + cpp_prompt + "\nAssistant: ";
        
        // Tokenize the input
        std::vector<llama_token> tokens;
        ErrorCode tokenize_result = tokenizeInput(full_prompt, tokens);
        if (tokenize_result != ErrorCode::SUCCESS) {
            std::string error_msg = "[ERROR] " + getErrorMessage(tokenize_result);
            logError("Tokenization failed: " + error_msg, tokenize_result);
            return env->NewStringUTF(error_msg.c_str());
        }
        
        // Perform inference
        std::string generated_text;
        ErrorCode inference_result = performInference(tokens, generated_text);
        if (inference_result != ErrorCode::SUCCESS) {
            std::string error_msg = "[ERROR] " + getErrorMessage(inference_result);
            logError("Inference failed: " + error_msg, inference_result);
            return env->NewStringUTF(error_msg.c_str());
        }
        
        // Validate output
        if (generated_text.empty()) {
            logError("Generated text is empty", ErrorCode::INFERENCE_FAILED);
            return env->NewStringUTF("[ERROR] Generated response is empty");
        }
        
        if (generated_text.length() > MAX_RESPONSE_LENGTH) {
            generated_text = generated_text.substr(0, MAX_RESPONSE_LENGTH) + "...";
            logInfo("Response truncated to " + std::to_string(MAX_RESPONSE_LENGTH) + " characters");
        }
        
        // Convert result to Java string
        jstring result = env->NewStringUTF(generated_text.c_str());
        if (result == nullptr) {
            logError("Failed to create Java string from generated text", ErrorCode::MEMORY_ALLOCATION_FAILED);
            return env->NewStringUTF("[ERROR] Failed to create response string");
        }
        
        logInfo("Successfully generated response (" + std::to_string(generated_text.length()) + " characters)");
        return result;
        
    } catch (const std::exception& e) {
        std::string error_msg = "[ERROR] Unexpected exception: " + std::string(e.what());
        logError(error_msg, ErrorCode::UNKNOWN_ERROR);
        return env->NewStringUTF(error_msg.c_str());
    }
}

// ============================================================================
// LIBRARY INITIALIZATION AND CLEANUP
// ============================================================================

/**
 * Library initialization function (called automatically by JVM)
 * This ensures proper setup when the library is loaded
 */
extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    try {
        logInfo("llama_jni library loaded");
        
        // Initialize llama.cpp library
        if (!initializeLlamaLibrary()) {
            logError("Failed to initialize llama.cpp library during JNI_OnLoad");
            return JNI_ERR;
        }
        
        return JNI_VERSION_1_8;
        
    } catch (const std::exception& e) {
        logError("Exception during JNI_OnLoad: " + std::string(e.what()));
        return JNI_ERR;
    }
}

/**
 * Library cleanup function (called automatically by JVM)
 * This ensures proper cleanup when the library is unloaded
 */
extern "C" JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* vm, void* reserved) {
    try {
        logInfo("llama_jni library unloading");
        
        // Clean up model resources
        cleanupModel();
        
        // Clean up llama.cpp library
        llama_backend_free();
        
        logInfo("llama_jni library unloaded successfully");
        
    } catch (const std::exception& e) {
        logError("Exception during JNI_OnUnload: " + std::string(e.what()));
    }
}
