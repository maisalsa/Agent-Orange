import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Offline Pentesting Chatbot CLI");
        System.out.println("Type 'exit' to quit.");
        while (true) {
            System.out.print("\n> ");
            String input = scanner.nextLine();
            if (input == null) {
                // End of input (e.g., Ctrl+D)
                break;
            }
            input = input.trim();
            if (input.equalsIgnoreCase("exit")) {
                System.out.println("Exiting.");
                break;
            }
            if (input.isEmpty()) {
                continue;
            }
            try {
                routeInput(input);
            } catch (Exception e) {
                System.err.println("[ERROR] " + e.getMessage());
            }
        }
        scanner.close();
    }

    private static void routeInput(String input) {
        // Simple keyword-based routing. In production, use NLP or more advanced parsing.
        String lower = input.toLowerCase();
        if (lower.contains("llm")) {
            // TODO: Integrate with LlamaJNI
            // Example:
            // LlamaJNI llama = new LlamaJNI();
            // String response = llama.generateResponse(input);
            // System.out.println("[LLM] " + response);
            System.out.println("[LLM MODULE] (stub) Processing: " + input);
        } else if (lower.contains("embed") || lower.contains("embedding")) {
            // TODO: Integrate with EmbeddingClient
            // Example:
            // EmbeddingClient emb = new EmbeddingClient();
            // float[] vec = emb.getEmbedding(input);
            System.out.println("[EMBEDDING MODULE] (stub) Processing: " + input);
        } else if (lower.contains("ghidra")) {
            // TODO: Integrate with GhidraBridge
            // Example:
            // GhidraBridge bridge = new GhidraBridge(ghidraHeadlessPath, projectDir, projectName);
            // String output = bridge.runScript(binaryPath, scriptPath, null);
            // System.out.println("[GHIDRA] " + output);
            System.out.println("[GHIDRA MODULE] (stub) Processing: " + input);
        } else if (lower.contains("vector db") || lower.contains("vectordb") || lower.contains("database")) {
            // TODO: Integrate with ChromaDBClient
            // Example:
            // ChromaDBClient db = new ChromaDBClient(baseUrl);
            // String resp = db.addDocument(collection, docId, document, embedding);
            System.out.println("[VECTOR DB MODULE] (stub) Processing: " + input);
        } else {
            // TODO: Default to LLM chatbot
            System.out.println("[CHATBOT] (stub) Processing: " + input);
        }
    }
} 