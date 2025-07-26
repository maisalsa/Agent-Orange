import java.util.*;

/**
 * EmbeddingClient supports pluggable Java-native embedding backends.
 * 
 * <p>This class provides a flexible interface for integrating different embedding models.
 * You can register any Java-native embedding implementation that implements the 
 * {@link EmbeddingBackend} interface.</p>
 *
 * <h3>Quick Start:</h3>
 * <pre>{@code
 * EmbeddingClient client = new EmbeddingClient();
 * client.setBackend(new MyEmbeddingModel());
 * float[] embedding = client.getEmbedding("Hello, world!");
 * }</pre>
 *
 * <h3>Custom Backend Implementation:</h3>
 * <pre>{@code
 * public class MyEmbeddingModel implements EmbeddingClient.EmbeddingBackend {
 *     public float[] embed(String text) {
 *         // Your embedding logic here
 *         // Example: call a native library, use a Java ML framework, etc.
 *         return new float[] {0.1f, 0.2f, 0.3f, 0.4f};
 *     }
 * }
 * }</pre>
 *
 * <p>If no backend is set, getEmbedding() will throw an error or return a dummy vector
 * depending on the configuration.</p>
 */
public class EmbeddingClient {
    
    /**
     * Interface for embedding model implementations.
     * 
     * <p>Implement this interface to provide a custom embedding backend.
     * The embed method should return a fixed-length vector representation of the input text.</p>
     * 
     * <h3>Implementation Requirements:</h3>
     * <ul>
     *   <li>The method should return a non-null float array</li>
     *   <li>The array length should be consistent for all inputs (fixed embedding dimension)</li>
     *   <li>The method should handle any valid text input</li>
     *   <li>Consider thread safety if the backend will be used in multi-threaded environments</li>
     * </ul>
     * 
     * <h3>Example Implementation:</h3>
     * <pre>{@code
     * public class SentenceTransformerBackend implements EmbeddingBackend {
     *     private final int embeddingDimension = 768;
     *     
     *     public float[] embed(String text) {
     *         // Initialize your model (do this once in constructor in real implementations)
     *         // Call your embedding library
     *         float[] result = new float[embeddingDimension];
     *         // ... embedding logic ...
     *         return result;
     *     }
     * }
     * }</pre>
     */
    public interface EmbeddingBackend {
        /**
         * Generate an embedding vector for the given text.
         * 
         * @param text The input text to embed. Will not be null or empty.
         * @return A float array representing the text embedding. Must not be null.
         * @throws RuntimeException if embedding generation fails
         */
        float[] embed(String text);
    }

    private EmbeddingBackend backend = null;
    private boolean throwIfNoBackend = true;

    /**
     * Register or set the embedding backend implementation.
     * 
     * <p>This method allows you to set the embedding model that will be used
     * for all subsequent calls to {@link #getEmbedding(String)}.</p>
     * 
     * <h3>Usage:</h3>
     * <pre>{@code
     * EmbeddingClient client = new EmbeddingClient();
     * client.setBackend(new MyCustomEmbeddingModel());
     * }</pre>
     * 
     * @param backend The embedding backend implementation. If null, clears the current backend.
     */
    public void setBackend(EmbeddingBackend backend) {
        this.backend = backend;
    }

    /**
     * Configure whether to throw an exception when no backend is set.
     * 
     * <p>By default (true), {@link #getEmbedding(String)} will throw an 
     * {@link IllegalStateException} if no backend is registered. If set to false,
     * it will return a dummy embedding vector instead.</p>
     * 
     * <h3>Usage:</h3>
     * <pre>{@code
     * EmbeddingClient client = new EmbeddingClient();
     * client.setThrowIfNoBackend(false); // Return dummy vectors instead of throwing
     * float[] dummy = client.getEmbedding("test"); // Returns [0.1, 0.2, 0.3]
     * }</pre>
     * 
     * @param throwIfNoBackend If true, throw exception when no backend is set. If false, return dummy vector.
     */
    public void setThrowIfNoBackend(boolean throwIfNoBackend) {
        this.throwIfNoBackend = throwIfNoBackend;
    }

    /**
     * Get the embedding for the given text using the registered backend.
     * 
     * <p>This method will use the currently registered backend to generate an embedding
     * for the input text. The behavior depends on the backend configuration:</p>
     * 
     * <ul>
     *   <li>If a backend is registered: calls the backend's embed method</li>
     *   <li>If no backend and throwIfNoBackend is true: throws IllegalStateException</li>
     *   <li>If no backend and throwIfNoBackend is false: returns dummy vector [0.1, 0.2, 0.3]</li>
     * </ul>
     * 
     * <h3>Usage:</h3>
     * <pre>{@code
     * EmbeddingClient client = new EmbeddingClient();
     * client.setBackend(new MyEmbeddingModel());
     * 
     * try {
     *     float[] embedding = client.getEmbedding("Hello, world!");
     *     // Use the embedding...
     * } catch (IllegalArgumentException e) {
     *     // Handle invalid input
     * } catch (IllegalStateException e) {
     *     // Handle missing backend
     * }
     * }</pre>
     * 
     * @param text The text to embed. Must not be null or empty.
     * @return A float array representing the text embedding
     * @throws IllegalArgumentException if text is null or empty
     * @throws IllegalStateException if no backend is set and throwIfNoBackend is true
     * @throws RuntimeException if the backend's embed method throws an exception
     */
    public float[] getEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Input text for embedding must not be null or empty.");
        }
        if (backend != null) {
            return backend.embed(text);
        } else if (throwIfNoBackend) {
            throw new IllegalStateException("No embedding backend set. Please register a Java-native embedding model.");
        } else {
            // Return a fixed dummy embedding
            return new float[] {0.1f, 0.2f, 0.3f};
        }
    }

    /**
     * Example usage demonstrating how to use the EmbeddingClient with a custom backend.
     */
    public static void main(String[] args) {
        // Create the client
        EmbeddingClient client = new EmbeddingClient();
        
        // Example 1: Register a simple dummy backend
        client.setBackend(new EmbeddingBackend() {
            public float[] embed(String text) {
                // Simple hash-based dummy embedding for demonstration
                int hash = text.hashCode();
                return new float[] {
                    (hash & 0xFF) / 255.0f,
                    ((hash >> 8) & 0xFF) / 255.0f,
                    ((hash >> 16) & 0xFF) / 255.0f
                };
            }
        });
        
        // Example 2: Use the client
        String text = "example sentence";
        try {
            float[] emb = client.getEmbedding(text);
            System.out.print("Embedding: ");
            for (float v : emb) {
                System.out.print(v + " ");
            }
            System.out.println();
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
        
        // Example 3: Demonstrate error handling
        try {
            client.getEmbedding(""); // Should throw IllegalArgumentException
        } catch (IllegalArgumentException e) {
            System.out.println("Correctly caught empty input: " + e.getMessage());
        }
    }
} 