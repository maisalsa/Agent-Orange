import java.util.*;

public class EmbeddingClient {
    // Example: If you have a Java-native embedding model, implement here
    // For demonstration, this returns a dummy embedding
    public float[] getEmbedding(String text) {
        // TODO: Replace with actual Java-native embedding model call
        // Example: return embeddingModel.embed(text);
        return new float[] {0.1f, 0.2f, 0.3f}; // Dummy embedding
    }

    // Example usage
    public static void main(String[] args) {
        EmbeddingClient client = new EmbeddingClient();
        String text = "example sentence";
        float[] emb = client.getEmbedding(text);
        System.out.print("Embedding: ");
        for (float v : emb) {
            System.out.print(v + " ");
        }
        System.out.println();
    }
} 