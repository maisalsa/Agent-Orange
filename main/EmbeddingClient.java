import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class EmbeddingClient {
    // Example: If you have a Java-native embedding model, implement here
    // For demonstration, this returns a dummy embedding
    public float[] getEmbedding(String text) {
        // TODO: Replace with actual Java-native embedding model call
        // Example: return embeddingModel.embed(text);
        return new float[] {0.1f, 0.2f, 0.3f}; // Dummy embedding
    }

    // Fallback: Call a local Python script (embed.py) to generate embeddings
    // The Python script should print a space-separated list of floats to stdout
    public float[] getEmbeddingFromPython(String text) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("python3");
        command.add("embed.py");
        command.add(text.trim());

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line = reader.readLine();
        process.waitFor();
        reader.close();

        if (line == null || line.isEmpty()) {
            throw new RuntimeException("No embedding output from Python script");
        }

        String[] parts = line.trim().split("\\s+");
        float[] embedding = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                embedding[i] = Float.parseFloat(parts[i]);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Malformed embedding value: '" + parts[i] + "' at index " + i);
            }
        }
        return embedding;
    }

    // Example usage
    public static void main(String[] args) throws Exception {
        EmbeddingClient client = new EmbeddingClient();
        String text = "example sentence";
        float[] emb = client.getEmbedding(text);
        System.out.print("Embedding: ");
        for (float v : emb) {
            System.out.print(v + " ");
        }
        System.out.println();

        // Uncomment to use Python fallback (requires embed.py in working directory)
        // float[] pyEmb = client.getEmbeddingFromPython(text);
        // System.out.print("Python Embedding: ");
        // for (float v : pyEmb) {
        //     System.out.print(v + " ");
        // }
        // System.out.println();
    }
} 