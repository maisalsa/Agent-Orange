import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * MCPOrchestrator - Central Controller for Pentesting Chatbot
 * 
 * This class orchestrates the interaction between different modules (LLM, Ghidra, 
 * Embedding, Vector DB) based on user input. It analyzes natural language commands
 * and chains appropriate module calls to provide comprehensive responses.
 * 
 * <h2>Module Integration:</h2>
 * <ul>
 *   <li><strong>LLM (LlamaJNI):</strong> Natural language processing and response generation</li>
 *   <li><strong>Ghidra:</strong> Binary analysis and function extraction</li>
 *   <li><strong>Embedding:</strong> Text vectorization for similarity search</li>
 *   <li><strong>Vector DB (ChromaDB):</strong> Document storage and retrieval</li>
 * </ul>
 * 
 * <h2>Command Patterns:</h2>
 * <ul>
 *   <li><strong>Ghidra Analysis:</strong> "analyze [binary] with ghidra", "ghidra [binary]"</li>
 *   <li><strong>Embedding Generation:</strong> "embed [text]", "get embedding for [text]"</li>
 *   <li><strong>Vector DB Query:</strong> "search [query]", "find similar to [text]"</li>
 *   <li><strong>LLM Chat:</strong> General conversation and analysis requests</li>
 * </ul>
 * 
 * <h2>Error Handling:</h2>
 * <ul>
 *   <li>Module availability checking</li>
 *   <li>Graceful degradation when modules fail</li>
 *   <li>Detailed error messages for debugging</li>
 *   <li>Fallback responses for unavailable functionality</li>
 * </ul>
 */
public class MCPOrchestrator {
    
    /** LLM interface for natural language processing */
    private final LlamaJNI llama;
    
    /** Embedding client for text vectorization */
    private final EmbeddingClient embeddingClient;
    
    /** Vector database client for document storage and retrieval */
    private final ChromaDBClient chromaDBClient;
    
    /** Ghidra bridge for binary analysis */
    private final GhidraBridge ghidraBridge;
    
    /** Default collection name for vector database operations */
    private static final String DEFAULT_COLLECTION = "pentesting_docs";
    
    /** Default number of results for vector database queries */
    private static final int DEFAULT_TOP_K = 5;

    /**
     * Creates a new MCPOrchestrator with the specified modules.
     * 
     * @param llama LLM interface for natural language processing
     * @param embeddingClient Embedding client for text vectorization
     * @param chromaDBClient Vector database client for document storage
     * @param ghidraBridge Ghidra bridge for binary analysis
     */
    public MCPOrchestrator(LlamaJNI llama, EmbeddingClient embeddingClient, ChromaDBClient chromaDBClient, GhidraBridge ghidraBridge) {
        this.llama = llama;
        this.embeddingClient = embeddingClient;
        this.chromaDBClient = chromaDBClient;
        this.ghidraBridge = ghidraBridge;
        
        // Log module availability
        logModuleStatus();
    }

    /**
     * Process a user message and route to appropriate modules.
     * 
     * This method analyzes the user input to determine which modules to invoke
     * and chains them together as needed. It provides comprehensive error handling
     * and fallback responses when modules are unavailable.
     * 
     * @param message User's natural language input
     * @return Chatbot response or error message
     */
    public String processUserMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "[ERROR] Empty or null message received";
        }
        
        String lower = message.toLowerCase().trim();
        System.out.println("[MCPOrchestrator] Processing message: " + message);
        
        try {
            // Check for Ghidra binary analysis requests
            if (isGhidraRequest(lower)) {
                return handleGhidraRequest(message, lower);
            }
            
            // Check for embedding generation requests
            if (isEmbeddingRequest(lower)) {
                return handleEmbeddingRequest(message, lower);
            }
            
            // Check for vector database queries
            if (isVectorDBRequest(lower)) {
                return handleVectorDBRequest(message, lower);
            }
            
            // Check for explicit LLM requests
            if (isLLMRequest(lower)) {
                return handleLLMRequest(message);
            }
            
            // Default: Use LLM as general chatbot
            return handleGeneralChat(message);
            
        } catch (Exception e) {
            System.err.println("[MCPOrchestrator] Error processing message: " + e.getMessage());
            e.printStackTrace();
            return "[ERROR] Failed to process request: " + e.getMessage();
        }
    }

    /**
     * Determines if the message is a Ghidra analysis request.
     * 
     * @param lowerMessage Lowercase message for pattern matching
     * @return true if this is a Ghidra request
     */
    private boolean isGhidraRequest(String lowerMessage) {
        return lowerMessage.contains("ghidra") || 
               lowerMessage.contains("analyze") && lowerMessage.contains("binary") ||
               lowerMessage.contains("reverse engineer") ||
               lowerMessage.contains("function") && lowerMessage.contains("extract");
    }

    /**
     * Handles Ghidra binary analysis requests.
     * 
     * @param originalMessage Original user message
     * @param lowerMessage Lowercase message for parsing
     * @return Analysis results or error message
     */
    private String handleGhidraRequest(String originalMessage, String lowerMessage) {
        System.out.println("[MCPOrchestrator] Handling Ghidra request");
        
        // Check if Ghidra is available
        if (ghidraBridge == null) {
            return "[ERROR] Ghidra analysis not available - GhidraBridge not configured";
        }
        
        try {
            // Extract binary path from message
            String binaryPath = extractBinaryPath(originalMessage);
            if (binaryPath == null) {
                return "[ERROR] No binary path specified. Please provide a path to the binary file.";
            }
            
            // Check if binary file exists
            if (!new java.io.File(binaryPath).exists()) {
                return "[ERROR] Binary file not found: " + binaryPath;
            }
            
            System.out.println("[MCPOrchestrator] Analyzing binary: " + binaryPath);
            
            // Run Ghidra analysis
            String ghidraOutput = ghidraBridge.runScript(
                binaryPath, 
                "ExtractFunctions.java", 
                null
            );
            
            System.out.println("[MCPOrchestrator] Ghidra analysis completed");
            
            // If LLM is available, summarize the results
            if (llama != null && LlamaJNI.isLibraryLoaded()) {
                String summaryPrompt = String.format(
                    "Analyze and summarize the following function list from a binary analysis:\n%s\n\n" +
                    "Provide a brief overview of the binary's functionality based on these functions.",
                    ghidraOutput
                );
                
                String summary = llama.safeGenerateResponse(summaryPrompt);
                return "Binary Analysis Results:\n\n" + 
                       "Functions Found:\n" + ghidraOutput + "\n\n" +
                       "Summary:\n" + summary;
            } else {
                return "Binary Analysis Results:\n\n" + 
                       "Functions Found:\n" + ghidraOutput + "\n\n" +
                       "Note: LLM summarization not available";
            }
            
        } catch (Exception e) {
            System.err.println("[MCPOrchestrator] Ghidra analysis failed: " + e.getMessage());
            return "[ERROR] Ghidra analysis failed: " + e.getMessage();
        }
    }

    /**
     * Determines if the message is an embedding generation request.
     * 
     * @param lowerMessage Lowercase message for pattern matching
     * @return true if this is an embedding request
     */
    private boolean isEmbeddingRequest(String lowerMessage) {
        return lowerMessage.contains("embed") || 
               lowerMessage.contains("vector") && lowerMessage.contains("generate") ||
               lowerMessage.contains("convert") && lowerMessage.contains("vector");
    }

    /**
     * Handles embedding generation requests.
     * 
     * @param originalMessage Original user message
     * @param lowerMessage Lowercase message for parsing
     * @return Embedding vector or error message
     */
    private String handleEmbeddingRequest(String originalMessage, String lowerMessage) {
        System.out.println("[MCPOrchestrator] Handling embedding request");
        
        // Check if embedding client is available
        if (embeddingClient == null) {
            return "[ERROR] Embedding generation not available - EmbeddingClient not configured";
        }
        
        try {
            // Extract text to embed from message
            String textToEmbed = extractTextToEmbed(originalMessage);
            if (textToEmbed == null || textToEmbed.trim().isEmpty()) {
                return "[ERROR] No text specified for embedding. Please provide text to vectorize.";
            }
            
            System.out.println("[MCPOrchestrator] Generating embedding for: " + textToEmbed);
            
            // Generate embedding
            float[] embedding = embeddingClient.getEmbedding(textToEmbed);
            
            // Format embedding as readable string
            StringBuilder embeddingStr = new StringBuilder();
            embeddingStr.append("[");
            for (int i = 0; i < embedding.length; i++) {
                if (i > 0) embeddingStr.append(", ");
                embeddingStr.append(String.format("%.6f", embedding[i]));
            }
            embeddingStr.append("]");
            
            return "Embedding generated successfully:\n" +
                   "Text: " + textToEmbed + "\n" +
                   "Vector (" + embedding.length + " dimensions): " + embeddingStr.toString();
            
        } catch (Exception e) {
            System.err.println("[MCPOrchestrator] Embedding generation failed: " + e.getMessage());
            return "[ERROR] Embedding generation failed: " + e.getMessage();
        }
    }

    /**
     * Determines if the message is a vector database query request.
     * 
     * @param lowerMessage Lowercase message for pattern matching
     * @return true if this is a vector DB request
     */
    private boolean isVectorDBRequest(String lowerMessage) {
        return lowerMessage.contains("search") || 
               lowerMessage.contains("find") && lowerMessage.contains("similar") ||
               lowerMessage.contains("query") && lowerMessage.contains("database") ||
               lowerMessage.contains("vector") && lowerMessage.contains("db") ||
               lowerMessage.contains("vectordb") ||
               lowerMessage.contains("chroma");
    }

    /**
     * Handles vector database query requests.
     * 
     * @param originalMessage Original user message
     * @param lowerMessage Lowercase message for parsing
     * @return Query results or error message
     */
    private String handleVectorDBRequest(String originalMessage, String lowerMessage) {
        System.out.println("[MCPOrchestrator] Handling vector DB request");
        
        // Check if vector DB client is available
        if (chromaDBClient == null) {
            return "[ERROR] Vector database not available - ChromaDBClient not configured";
        }
        
        try {
            // Extract query text from message
            String queryText = extractQueryText(originalMessage);
            if (queryText == null || queryText.trim().isEmpty()) {
                return "[ERROR] No query specified. Please provide text to search for.";
            }
            
            System.out.println("[MCPOrchestrator] Querying vector DB for: " + queryText);
            
            // Generate embedding for query
            if (embeddingClient == null) {
                return "[ERROR] Embedding client not available for query vectorization";
            }
            
            float[] queryEmbedding = embeddingClient.getEmbedding(queryText);
            
            // Query vector database
            String queryResult = chromaDBClient.queryNearest(DEFAULT_COLLECTION, queryEmbedding, DEFAULT_TOP_K);
            
            return "Vector Database Query Results:\n" +
                   "Query: " + queryText + "\n" +
                   "Results: " + queryResult;
            
        } catch (Exception e) {
            System.err.println("[MCPOrchestrator] Vector DB query failed: " + e.getMessage());
            return "[ERROR] Vector database query failed: " + e.getMessage();
        }
    }

    /**
     * Determines if the message is an explicit LLM request.
     * 
     * @param lowerMessage Lowercase message for pattern matching
     * @return true if this is an LLM request
     */
    private boolean isLLMRequest(String lowerMessage) {
        return lowerMessage.contains("llm") || 
               lowerMessage.contains("generate") && lowerMessage.contains("text") ||
               lowerMessage.contains("ai") && lowerMessage.contains("response");
    }

    /**
     * Handles explicit LLM requests.
     * 
     * @param message User message
     * @return LLM response or error message
     */
    private String handleLLMRequest(String message) {
        System.out.println("[MCPOrchestrator] Handling LLM request");
        
        // Check if LLM is available
        if (llama == null || !LlamaJNI.isLibraryLoaded()) {
            return "[ERROR] LLM not available - LlamaJNI not configured or library not loaded";
        }
        
        try {
            // Remove LLM-specific keywords for cleaner prompt
            String cleanPrompt = message.replaceAll("(?i)\\b(llm|generate|ai)\\b", "").trim();
            if (cleanPrompt.isEmpty()) {
                cleanPrompt = "Please provide a helpful response.";
            }
            
            System.out.println("[MCPOrchestrator] Sending to LLM: " + cleanPrompt);
            
            String response = llama.safeGenerateResponse(cleanPrompt);
            return "LLM Response:\n" + response;
            
        } catch (Exception e) {
            System.err.println("[MCPOrchestrator] LLM request failed: " + e.getMessage());
            return "[ERROR] LLM request failed: " + e.getMessage();
        }
    }

    /**
     * Handles general chat requests using LLM.
     * 
     * @param message User message
     * @return LLM response or error message
     */
    private String handleGeneralChat(String message) {
        System.out.println("[MCPOrchestrator] Handling general chat request");
        
        // Check if LLM is available
        if (llama == null || !LlamaJNI.isLibraryLoaded()) {
            return "[ERROR] Chat functionality not available - LLM not configured";
        }
        
        try {
            System.out.println("[MCPOrchestrator] Sending to LLM: " + message);
            
            String response = llama.safeGenerateResponse(message);
            return response;
            
        } catch (Exception e) {
            System.err.println("[MCPOrchestrator] General chat failed: " + e.getMessage());
            return "[ERROR] Chat failed: " + e.getMessage();
        }
    }

    /**
     * Extracts binary path from user message.
     * 
     * @param message User message
     * @return Binary path or null if not found
     */
    private String extractBinaryPath(String message) {
        // Look for file paths in the message
        Pattern pathPattern = Pattern.compile("(/[^\\s]+|\\b[A-Za-z]:\\\\[^\\s]+)");
        Matcher matcher = pathPattern.matcher(message);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // Look for "binary" or "file" followed by a path
        Pattern binaryPattern = Pattern.compile("(?i)\\b(binary|file)\\s+([^\\s]+)");
        matcher = binaryPattern.matcher(message);
        
        if (matcher.find()) {
            return matcher.group(2);
        }
        
        return null;
    }

    /**
     * Extracts text to embed from user message.
     * 
     * @param message User message
     * @return Text to embed or null if not found
     */
    private String extractTextToEmbed(String message) {
        // Remove embedding-related keywords
        String cleaned = message.replaceAll("(?i)\\b(embed|vector|generate|convert)\\b", "").trim();
        
        // Look for quoted text
        Pattern quotePattern = Pattern.compile("\"([^\"]+)\"");
        Matcher matcher = quotePattern.matcher(cleaned);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // Return the cleaned message if no quotes found
        return cleaned.isEmpty() ? null : cleaned;
    }

    /**
     * Extracts query text from user message.
     * 
     * @param message User message
     * @return Query text or null if not found
     */
    private String extractQueryText(String message) {
        // Remove search-related keywords
        String cleaned = message.replaceAll("(?i)\\b(search|find|query|database|vectordb|chroma)\\b", "").trim();
        
        // Look for quoted text
        Pattern quotePattern = Pattern.compile("\"([^\"]+)\"");
        Matcher matcher = quotePattern.matcher(cleaned);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // Return the cleaned message if no quotes found
        return cleaned.isEmpty() ? null : cleaned;
    }

    /**
     * Logs the status of all modules for debugging.
     */
    private void logModuleStatus() {
        System.out.println("[MCPOrchestrator] Module Status:");
        System.out.println("  LLM (LlamaJNI): " + (llama != null && LlamaJNI.isLibraryLoaded() ? "Available" : "Not available"));
        System.out.println("  EmbeddingClient: " + (embeddingClient != null ? "Available" : "Not available"));
        System.out.println("  ChromaDBClient: " + (chromaDBClient != null ? "Available" : "Not available"));
        System.out.println("  GhidraBridge: " + (ghidraBridge != null ? "Available" : "Not available"));
    }

    /**
     * Example usage demonstrating the orchestrator with real module integration.
     * 
     * This example shows how to set up and use the MCPOrchestrator with actual
     * module instances. It includes error handling and demonstrates various
     * types of requests.
     */
    public static void main(String[] args) {
        System.out.println("=== MCPOrchestrator Test ===");
        
        try {
            // Initialize modules with proper error handling
            LlamaJNI llama = new LlamaJNI();
            EmbeddingClient embeddingClient = new EmbeddingClient();
            ChromaDBClient chromaDBClient = ChromaDBClient.fromConfig();
            GhidraBridge ghidraBridge = new GhidraBridge(
                "/opt/ghidra/support/analyzeHeadless", 
                "/tmp/ghidra_projects", 
                "test_project"
            );
            
            // Create orchestrator
            MCPOrchestrator orchestrator = new MCPOrchestrator(
                llama, embeddingClient, chromaDBClient, ghidraBridge
            );
            
            // Test various types of requests
            String[] testMessages = {
                "Hello, how are you?",  // General chat
                "Embed this text: \"sample text for embedding\"",  // Embedding
                "Search for documents about security",  // Vector DB
                "Analyze /path/to/binary with ghidra",  // Ghidra
                "Generate text about cybersecurity"  // LLM
            };
            
            for (String message : testMessages) {
                System.out.println("\n--- Testing: " + message + " ---");
                String result = orchestrator.processUserMessage(message);
                System.out.println("Result: " + result);
            }
            
        } catch (Exception e) {
            System.err.println("Error in test: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n=== Test Complete ===");
    }
} 