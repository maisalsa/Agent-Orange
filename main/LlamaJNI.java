/**
 * LlamaJNI - Java Native Interface for llama.cpp
 * 
 * This class provides a Java wrapper around the llama.cpp library for LLM inference.
 * It uses JNI to call native C/C++ functions from Java with proper resource management
 * and error handling.
 * 
 * <h2>Native Library Requirements</h2>
 * 
 * <h3>Library Naming Convention:</h3>
 * <ul>
 *   <li><strong>Linux:</strong> libllama.so</li>
 *   <li><strong>macOS:</strong> libllama.dylib</li>
 *   <li><strong>Windows:</strong> llama.dll</li>
 * </ul>
 * 
 * <h3>Library Path Configuration:</h3>
 * <ul>
 *   <li><strong>Linux:</strong> Set LD_LIBRARY_PATH environment variable</li>
 *   <li><strong>macOS:</strong> Set DYLD_LIBRARY_PATH environment variable</li>
 *   <li><strong>Windows:</strong> Add library directory to PATH environment variable</li>
 * </ul>
 * 
 * <h2>Compilation Instructions</h2>
 * 
 * <h3>Step 1: Generate JNI Header</h3>
 * <pre>{@code
 * javac LlamaJNI.java
 * javah -jni LlamaJNI
 * }</pre>
 * 
 * <h3>Step 2: Compile Native Library</h3>
 * 
 * <strong>Linux:</strong>
 * <pre>{@code
 * g++ -fPIC -shared \
 *     -I$JAVA_HOME/include \
 *     -I$JAVA_HOME/include/linux \
 *     -o libllama.so \
 *     llama.cpp LlamaJNI.c
 * }</pre>
 * 
 * <strong>macOS:</strong>
 * <pre>{@code
 * g++ -fPIC -shared \
 *     -I$JAVA_HOME/include \
 *     -I$JAVA_HOME/include/darwin \
 *     -o libllama.dylib \
 *     llama.cpp LlamaJNI.c
 * }</pre>
 * 
 * <strong>Windows:</strong>
 * <pre>{@code
 * g++ -shared \
 *     -I"%JAVA_HOME%\include" \
 *     -I"%JAVA_HOME%\include\win32" \
 *     -o llama.dll \
 *     llama.cpp LlamaJNI.c
 * }</pre>
 * 
 * <h3>Step 3: Set Library Path</h3>
 * <pre>{@code
 * # Linux
 * export LD_LIBRARY_PATH=/path/to/lib:$LD_LIBRARY_PATH
 * 
 * # macOS
 * export DYLD_LIBRARY_PATH=/path/to/lib:$DYLD_LIBRARY_PATH
 * 
 * # Windows
 * set PATH=C:\path\to\lib;%PATH%
 * }</pre>
 * 
 * <h2>Memory Management</h2>
 * <ul>
 *   <li>JNI strings are automatically managed by the JVM</li>
 *   <li>Native strings returned by llama_infer() must be freed by the native code</li>
 *   <li>This class handles proper cleanup of JNI string conversions</li>
 *   <li>All GetStringUTFChars calls are paired with ReleaseStringUTFChars</li>
 * </ul>
 * 
 * <h2>Error Handling</h2>
 * <ul>
 *   <li>Validates input parameters before native calls</li>
 *   <li>Handles null responses from native code</li>
 *   <li>Provides meaningful error messages for debugging</li>
 *   <li>Graceful degradation when native library is unavailable</li>
 *   <li>Thread-safe library loading and status checking</li>
 * </ul>
 * 
 * <h2>Thread Safety</h2>
 * <ul>
 *   <li>Library loading is thread-safe (static initialization)</li>
 *   <li>Native method calls are thread-safe if llama.cpp is thread-safe</li>
 *   <li>Status checking methods are thread-safe</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Check if library is available
 * if (!LlamaJNI.isLibraryLoaded()) {
 *     System.err.println("Library not loaded: " + LlamaJNI.getLoadError());
 *     return;
 * }
 * 
 * // Create instance and use
 * LlamaJNI llama = new LlamaJNI();
 * String response = llama.safeGenerateResponse("Hello, world!");
 * System.out.println(response);
 * }</pre>
 * 
 * @see <a href="https://github.com/ggerganov/llama.cpp">llama.cpp GitHub Repository</a>
 * @see <a href="https://docs.oracle.com/javase/8/docs/technotes/guides/jni/">JNI Documentation</a>
 */
public class LlamaJNI {
    
    /** Flag indicating if the native library was loaded successfully */
    private static volatile boolean libraryLoaded = false;
    
    /** Error message if library loading failed */
    private static volatile String loadError = null;
    
    /**
     * Static initializer block for loading the native library.
     * 
     * This block attempts to load the native library using System.loadLibrary().
     * The library name "llama" will be automatically prefixed with "lib" and
     * suffixed with the appropriate platform extension (.so, .dylib, .dll).
     * 
     * <p>The loading process is thread-safe and handles the following scenarios:</p>
     * <ul>
     *   <li>Library found and loaded successfully</li>
     *   <li>Library not found in library path</li>
     *   <li>Library found but cannot be loaded (permissions, dependencies)</li>
     *   <li>Security restrictions preventing library loading</li>
     * </ul>
     * 
     * <p>If loading fails, the error is captured and the application can continue
     * with graceful degradation.</p>
     */
    static {
        try {
            // Load the native library (libllama.so, libllama.dylib, or llama.dll)
            // System.loadLibrary() automatically handles platform-specific naming
            System.loadLibrary("llama");
            libraryLoaded = true;
            System.out.println("[LlamaJNI] Native library loaded successfully");
        } catch (UnsatisfiedLinkError e) {
            libraryLoaded = false;
            loadError = e.getMessage();
            System.err.println("[LlamaJNI] Failed to load native library: " + e.getMessage());
            System.err.println("[LlamaJNI] Please ensure libllama is in your library path");
            System.err.println("[LlamaJNI] Set LD_LIBRARY_PATH (Linux), DYLD_LIBRARY_PATH (macOS), or PATH (Windows)");
        } catch (SecurityException e) {
            libraryLoaded = false;
            loadError = e.getMessage();
            System.err.println("[LlamaJNI] Security exception loading library: " + e.getMessage());
            System.err.println("[LlamaJNI] Check security manager settings and file permissions");
        } catch (Exception e) {
            libraryLoaded = false;
            loadError = e.getMessage();
            System.err.println("[LlamaJNI] Unexpected error loading library: " + e.getMessage());
        }
    }

    /**
     * Native method declaration for LLM inference.
     * 
     * <p>This method calls the native llama_infer() function in llama.cpp.
     * The native code is responsible for:</p>
     * <ul>
     *   <li>Loading and managing the LLM model</li>
     *   <li>Running inference on the provided prompt</li>
     *   <li>Returning the generated response as a UTF-8 string</li>
     *   <li>Managing model memory and computational resources</li>
     * </ul>
     * 
     * <p><strong>Important Notes:</strong></p>
     * <ul>
     *   <li>This method is not thread-safe unless llama.cpp is thread-safe</li>
     *   <li>The native code must properly free any allocated memory</li>
     *   <li>Input validation should be done before calling this method</li>
     *   <li>Use safeGenerateResponse() for safe, validated calls</li>
     * </ul>
     * 
     * <p><strong>JNI Implementation Details:</strong></p>
     * <ul>
     *   <li>Java string is converted to C string using GetStringUTFChars</li>
     *   <li>C string is passed to llama_infer() function</li>
     *   <li>Response is converted back to Java string using NewStringUTF</li>
     *   <li>All JNI string operations are properly paired for cleanup</li>
     * </ul>
     * 
     * @param prompt The input text prompt for the LLM (must not be null)
     * @return The generated response from the LLM, or error message
     * @throws UnsatisfiedLinkError if native library is not available
     * @throws NullPointerException if prompt is null (handled by native code)
     * @throws OutOfMemoryError if native memory allocation fails
     */
    public native String generateResponse(String prompt);

    /**
     * Safe wrapper for native LLM inference with comprehensive input validation.
     * 
     * <p>This method provides a safe interface to the native generateResponse method
     * by validating inputs and handling potential errors gracefully. It should be
     * used instead of calling generateResponse() directly in most cases.</p>
     * 
     * <p><strong>Validation Performed:</strong></p>
     * <ul>
     *   <li>Library availability check</li>
     *   <li>Null input validation</li>
     *   <li>Empty/whitespace-only input validation</li>
     *   <li>Response validation (null check)</li>
     * </ul>
     * 
     * <p><strong>Error Handling:</strong></p>
     * <ul>
     *   <li>Returns descriptive error messages instead of throwing exceptions</li>
     *   <li>Handles UnsatisfiedLinkError gracefully</li>
     *   <li>Catches and reports unexpected exceptions</li>
     *   <li>Provides debugging information for troubleshooting</li>
     * </ul>
     * 
     * <p><strong>Thread Safety:</strong></p>
     * <ul>
     *   <li>This method is thread-safe for checking library status</li>
     *   <li>Native method calls are thread-safe if llama.cpp is thread-safe</li>
     *   <li>Input validation is thread-safe</li>
     * </ul>
     * 
     * @param prompt The input text prompt (must not be null or empty)
     * @return The generated response or a descriptive error message
     * @throws IllegalArgumentException if prompt is null or empty (optional, currently returns error message)
     */
    public String safeGenerateResponse(String prompt) {
        // Check if native library is available
        if (!libraryLoaded) {
            return "[ERROR] Native library not loaded: " + loadError;
        }
        
        // Validate input
        if (prompt == null) {
            return "[ERROR] Null prompt";
        }
        
        if (prompt.trim().isEmpty()) {
            return "[ERROR] Empty prompt";
        }
        
        try {
            // Call native method
            String response = generateResponse(prompt);
            
            // Validate response
            if (response == null) {
                return "[ERROR] Native method returned null response";
            }
            
            return response;
            
        } catch (UnsatisfiedLinkError e) {
            return "[ERROR] Native library not available: " + e.getMessage();
        } catch (OutOfMemoryError e) {
            return "[ERROR] Out of memory during inference: " + e.getMessage();
        } catch (Exception e) {
            return "[ERROR] Unexpected error: " + e.getMessage();
        }
    }

    /**
     * Check if the native library is loaded and available.
     * 
     * <p>This method provides a thread-safe way to check if the native library
     * was successfully loaded during class initialization.</p>
     * 
     * <p><strong>Usage:</strong></p>
     * <pre>{@code
     * if (!LlamaJNI.isLibraryLoaded()) {
     *     System.err.println("Cannot use LLM functionality - library not loaded");
     *     return;
     * }
     * }</pre>
     * 
     * @return true if the library is loaded and available, false otherwise
     */
    public static boolean isLibraryLoaded() {
        return libraryLoaded;
    }

    /**
     * Get the error message from library loading failure.
     * 
     * <p>This method provides detailed information about why the native library
     * failed to load, which is useful for debugging and user feedback.</p>
     * 
     * <p><strong>Common Error Messages:</strong></p>
     * <ul>
     *   <li>"no llama in java.library.path" - Library not found in path</li>
     *   <li>"libllama.so: cannot open shared object file" - Library file not found</li>
     *   <li>"libllama.so: wrong ELF class" - Architecture mismatch</li>
     *   <li>"Permission denied" - File permissions issue</li>
     * </ul>
     * 
     * @return Error message if library failed to load, null if loaded successfully
     */
    public static String getLoadError() {
        return loadError;
    }

    /**
     * Example usage demonstrating proper error handling and resource management.
     * 
     * <p>This method shows how to properly use the LlamaJNI class with comprehensive
     * error handling and testing of various input scenarios.</p>
     * 
     * <h3>Build and Run Instructions:</h3>
     * <ol>
     *   <li>Compile Java: <code>javac LlamaJNI.java</code></li>
     *   <li>Generate header: <code>javah -jni LlamaJNI</code></li>
     *   <li>Compile native: <code>g++ -fPIC -shared -I$JAVA_HOME/include -I$JAVA_HOME/include/linux -o libllama.so llama.cpp LlamaJNI.c</code></li>
     *   <li>Set library path: <code>export LD_LIBRARY_PATH=/path/to/lib:$LD_LIBRARY_PATH</code></li>
     *   <li>Run: <code>java LlamaJNI</code></li>
     * </ol>
     * 
     * <h3>Expected Output:</h3>
     * <pre>{@code
     * === LlamaJNI Test ===
     * [LlamaJNI] Native library loaded successfully
     * 
     * --- Testing prompt: 'Hello, Llama!' ---
     * Response: [LLM response or error message]
     * 
     * --- Testing prompt: '' ---
     * Response: [ERROR] Empty prompt
     * 
     * --- Testing prompt: 'null' ---
     * Response: [ERROR] Null prompt
     * 
     * === Test Complete ===
     * }</pre>
     * 
     * @param args Command line arguments (unused)
     */
    public static void main(String[] args) {
        System.out.println("=== LlamaJNI Test ===");
        
        // Check library status
        if (!isLibraryLoaded()) {
            System.err.println("Library not loaded: " + getLoadError());
            System.err.println("Please ensure libllama is properly compiled and linked");
            System.err.println("Check library path and file permissions");
            return;
        }
        
        // Create instance
        LlamaJNI llama = new LlamaJNI();
        
        // Test cases covering various scenarios
        String[] testPrompts = {
            "Hello, Llama!",           // Normal input
            "",                        // Empty string
            null,                      // Null string
            "What is the capital of France?",  // Normal question
            "   \t\n   ",              // Whitespace only
            new String(new char[10000]).replace("\0", "A")  // Very long input (Java 8 compatible)
        };
        
        for (String prompt : testPrompts) {
            System.out.println("\n--- Testing prompt: " + 
                (prompt == null ? "null" : "'" + prompt + "'") + " ---");
            
            try {
                String response = llama.safeGenerateResponse(prompt);
                System.out.println("Response: " + response);
            } catch (Exception e) {
                System.err.println("Exception: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        System.out.println("\n=== Test Complete ===");
    }
} 