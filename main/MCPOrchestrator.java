public class MCPOrchestrator {
    private final LlamaJNI llama;
    private final EmbeddingClient embeddingClient;
    private final ChromaDBClient chromaDBClient;
    private final GhidraBridge ghidraBridge;

    public MCPOrchestrator(LlamaJNI llama, EmbeddingClient embeddingClient, ChromaDBClient chromaDBClient, GhidraBridge ghidraBridge) {
        this.llama = llama;
        this.embeddingClient = embeddingClient;
        this.chromaDBClient = chromaDBClient;
        this.ghidraBridge = ghidraBridge;
    }

    /**
     * Process a user message, decide which modules to call, and chain calls as needed.
     * @param message User's natural language input
     * @return Chatbot response
     */
    public String processUserMessage(String message) {
        String lower = message.toLowerCase();
        try {
            if (lower.contains("ghidra")) {
                // Example: User wants binary analysis, then a summary
                // 1. Run Ghidra analysis (stubbed)
                // String ghidraOutput = ghidraBridge.runScript(binaryPath, scriptPath, null);
                String ghidraOutput = "{\"functions\":[\"main\",\"helper\"]}"; // Stub
                // 2. Summarize with LLM
                // String summary = llama.safeGenerateResponse("Summarize these functions: " + ghidraOutput);
                String summary = "[LLM summary of Ghidra output]"; // Stub
                return summary;
            } else if (lower.contains("embed") || lower.contains("embedding")) {
                // Example: User wants an embedding
                // float[] embedding = embeddingClient.getEmbedding(message);
                float[] embedding = new float[] {0.1f, 0.2f, 0.3f}; // Stub
                return "Embedding: [" + embedding[0] + ", " + embedding[1] + ", " + embedding[2] + "]";
            } else if (lower.contains("vector db") || lower.contains("vectordb") || lower.contains("database")) {
                // Example: User wants to query the vector DB
                // String resp = chromaDBClient.queryNearest(collection, embedding, topK);
                String resp = "[Vector DB query result]"; // Stub
                return resp;
            } else if (lower.contains("llm")) {
                // Direct LLM call
                // String response = llama.safeGenerateResponse(message);
                String response = "[LLM response]"; // Stub
                return response;
            } else {
                // Default: Use LLM as chatbot
                // String response = llama.safeGenerateResponse(message);
                String response = "[LLM chatbot response]"; // Stub
                return response;
            }
        } catch (Exception e) {
            return "[ERROR] " + e.getMessage();
        }
    }

    // Example usage
    public static void main(String[] args) {
        // Stubs for module instances
        LlamaJNI llama = new LlamaJNI();
        EmbeddingClient embeddingClient = new EmbeddingClient();
        ChromaDBClient chromaDBClient = new ChromaDBClient("http://localhost:8000");
        GhidraBridge ghidraBridge = new GhidraBridge("/path/to/ghidra/support/analyzeHeadless", "/tmp/ghidra_proj", "test_proj");
        MCPOrchestrator orchestrator = new MCPOrchestrator(llama, embeddingClient, chromaDBClient, ghidraBridge);

        String userMsg = "Analyze this binary with Ghidra and summarize.";
        String result = orchestrator.processUserMessage(userMsg);
        System.out.println("Orchestrator result: " + result);
    }
} 