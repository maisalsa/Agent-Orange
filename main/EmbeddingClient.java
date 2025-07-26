import java.util.*;

/**
 * EmbeddingClient supports pluggable Java-native embedding backends.
 *
 * To add a new embedding model, implement the EmbeddingBackend interface and register it:
 *   EmbeddingClient client = new EmbeddingClient();
 *   client.setBackend(new MyEmbeddingModel());
 *
 * If no backend is set, getEmbedding() will throw an error or return a dummy vector.
 */
public class EmbeddingClient {
    public interface EmbeddingBackend {
        float[] embed(String text);
    }

    private EmbeddingBackend backend = null;
    private boolean throwIfNoBackend = true;

    /**
     * Register or set the embedding backend implementation.
     */
    public void setBackend(EmbeddingBackend backend) {
        this.backend = backend;
    }

    /**
     * If true (default), getEmbedding() throws if no backend is set. If false, returns a dummy vector.
     */
    public void setThrowIfNoBackend(boolean throwIfNoBackend) {
        this.throwIfNoBackend = throwIfNoBackend;
    }

    /**
     * Get the embedding for the given text using the registered backend.
     * Throws IllegalStateException if no backend is set and throwIfNoBackend is true.
     * Throws IllegalArgumentException if text is null or empty.
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

    // Example usage
    public static void main(String[] args) {
        EmbeddingClient client = new EmbeddingClient();
        // Example: Register a dummy backend
        client.setBackend(new EmbeddingBackend() {
            public float[] embed(String text) {
                // Replace with real model logic
                return new float[] {1.0f, 2.0f, 3.0f};
            }
        });
        String text = "example sentence";
        float[] emb = client.getEmbedding(text);
        System.out.print("Embedding: ");
        for (float v : emb) {
            System.out.print(v + " ");
        }
        System.out.println();
    }
} 