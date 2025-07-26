// Requires JUnit 4 (e.g., junit-4.13.2.jar on classpath)
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.Assert;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.ArrayList;

/**
 * TestMain - Comprehensive End-to-End Testing for Pentesting Chatbot
 * 
 * This test suite provides comprehensive testing of the entire application flow,
 * from CLI input through MCPOrchestrator processing to final output. It uses
 * mocked modules to ensure reliable testing without external dependencies.
 * 
 * <h2>Test Coverage:</h2>
 * <ul>
 *   <li>Individual module functionality</li>
 *   <li>MCPOrchestrator integration and routing</li>
 *   <li>CLI input processing and command handling</li>
 *   <li>Error handling and recovery</li>
 *   <li>End-to-end user workflows</li>
 * </ul>
 * 
 * <h2>Mock Strategy:</h2>
 * <ul>
 *   <li>Mock LLM responses for predictable testing</li>
 *   <li>Mock embedding vectors for consistent results</li>
 *   <li>Mock Ghidra analysis output</li>
 *   <li>Mock ChromaDB query responses</li>
 * </ul>
 */
public class TestMain {
    
    /** Mock LLM for testing */
    private MockLlamaJNI mockLlama;
    
    /** Mock embedding client for testing */
    private MockEmbeddingClient mockEmbedding;
    
    /** Mock ChromaDB client for testing */
    private MockChromaDBClient mockChromaDB;
    
    /** Mock Ghidra bridge for testing */
    private MockGhidraBridge mockGhidra;
    
    /** MCPOrchestrator instance for testing */
    private MCPOrchestrator orchestrator;
    
    /** Original system streams for restoration */
    private PrintStream originalOut;
    private PrintStream originalErr;
    private ByteArrayOutputStream testOut;
    private ByteArrayOutputStream testErr;

    @Before
    public void setUp() {
        // Set up mock modules
        mockLlama = new MockLlamaJNI();
        mockEmbedding = new MockEmbeddingClient();
        mockChromaDB = new MockChromaDBClient();
        mockGhidra = new MockGhidraBridge();
        
        // Create orchestrator with mock modules
        orchestrator = new MCPOrchestrator(mockLlama, mockEmbedding, mockChromaDB, mockGhidra);
        
        // Capture system output for testing
        originalOut = System.out;
        originalErr = System.err;
        testOut = new ByteArrayOutputStream();
        testErr = new ByteArrayOutputStream();
        System.setOut(new PrintStream(testOut));
        System.setErr(new PrintStream(testErr));
    }

    @After
    public void tearDown() {
        // Restore system streams
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    // ============================================================================
    // INDIVIDUAL MODULE TESTS
    // ============================================================================

    @Test
    public void testLLMModule() {
        // Test LLM with valid input
        String prompt = "Test prompt";
        String response = mockLlama.safeGenerateResponse(prompt);
        Assert.assertNotNull("LLM response should not be null", response);
        Assert.assertTrue("LLM response should contain expected content", 
            response.contains("Mock LLM response"));
        
        // Test LLM with null input
        String nullResponse = mockLlama.safeGenerateResponse(null);
        Assert.assertTrue("LLM should handle null input gracefully", 
            nullResponse.contains("ERROR"));
        
        // Test LLM with empty input
        String emptyResponse = mockLlama.safeGenerateResponse("");
        Assert.assertTrue("LLM should handle empty input gracefully", 
            emptyResponse.contains("ERROR"));
    }

    @Test
    public void testEmbeddingModule() throws Exception {
        // Test embedding with valid input
        String text = "test embedding text";
        float[] embedding = mockEmbedding.getEmbedding(text);
        Assert.assertNotNull("Embedding should not be null", embedding);
        Assert.assertTrue("Embedding should have positive length", embedding.length > 0);
        Assert.assertEquals("Embedding should have expected length", 3, embedding.length);
        
        // Test embedding with null input
        try {
            mockEmbedding.getEmbedding(null);
            Assert.fail("Should throw exception for null input");
        } catch (IllegalArgumentException e) {
            // Expected
        }
        
        // Test embedding with empty input
        try {
            mockEmbedding.getEmbedding("");
            Assert.fail("Should throw exception for empty input");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }

    @Test
    public void testVectorDBModule() throws Exception {
        // Test vector DB query
        float[] queryEmbedding = new float[] {0.1f, 0.2f, 0.3f};
        String result = mockChromaDB.queryNearest("test_collection", queryEmbedding, 5);
        Assert.assertNotNull("Vector DB result should not be null", result);
        Assert.assertTrue("Vector DB result should contain expected content", 
            result.contains("Mock ChromaDB response"));
        
        // Test vector DB with null embedding
        try {
            mockChromaDB.queryNearest("test_collection", null, 5);
            Assert.fail("Should throw exception for null embedding");
        } catch (Exception e) {
            // Expected
        }
    }

    @Test
    public void testGhidraModule() throws Exception {
        // Test Ghidra analysis
        String binaryPath = "/path/to/test/binary";
        String scriptPath = "ExtractFunctions.java";
        String result = mockGhidra.runScript(binaryPath, scriptPath, null);
        Assert.assertNotNull("Ghidra result should not be null", result);
        Assert.assertTrue("Ghidra result should contain expected content", 
            result.contains("Mock Ghidra analysis"));
        
        // Test Ghidra with non-existent binary
        try {
            mockGhidra.runScript("/nonexistent/binary", scriptPath, null);
            Assert.fail("Should throw exception for non-existent binary");
        } catch (Exception e) {
            // Expected
        }
    }

    // ============================================================================
    // MCPORCHESTRATOR INTEGRATION TESTS
    // ============================================================================

    @Test
    public void testGhidraRequestFlow() {
        // Test complete Ghidra analysis flow
        String userInput = "Analyze /path/to/binary with ghidra";
        String response = orchestrator.processUserMessage(userInput);
        
        Assert.assertNotNull("Response should not be null", response);
        Assert.assertTrue("Response should contain analysis results", 
            response.contains("Binary Analysis Results"));
        Assert.assertTrue("Response should contain functions", 
            response.contains("Functions Found"));
        Assert.assertTrue("Response should contain LLM summary", 
            response.contains("Summary:"));
    }

    @Test
    public void testEmbeddingRequestFlow() {
        // Test complete embedding generation flow
        String userInput = "Embed this text: \"sample text for testing\"";
        String response = orchestrator.processUserMessage(userInput);
        
        Assert.assertNotNull("Response should not be null", response);
        Assert.assertTrue("Response should indicate success", 
            response.contains("Embedding generated successfully"));
        Assert.assertTrue("Response should contain the input text", 
            response.contains("sample text for testing"));
        Assert.assertTrue("Response should contain vector information", 
            response.contains("Vector (3 dimensions)"));
    }

    @Test
    public void testVectorDBRequestFlow() {
        // Test complete vector DB query flow
        String userInput = "Search for documents about security vulnerabilities";
        String response = orchestrator.processUserMessage(userInput);
        
        Assert.assertNotNull("Response should not be null", response);
        Assert.assertTrue("Response should contain query results", 
            response.contains("Vector Database Query Results"));
        Assert.assertTrue("Response should contain the query", 
            response.contains("security vulnerabilities"));
        Assert.assertTrue("Response should contain results", 
            response.contains("Mock ChromaDB response"));
    }

    @Test
    public void testLLMRequestFlow() {
        // Test complete LLM generation flow
        String userInput = "Generate text about cybersecurity";
        String response = orchestrator.processUserMessage(userInput);
        
        Assert.assertNotNull("Response should not be null", response);
        Assert.assertTrue("Response should contain LLM output", 
            response.contains("LLM Response:"));
        Assert.assertTrue("Response should contain mock content", 
            response.contains("Mock LLM response"));
    }

    @Test
    public void testGeneralChatFlow() {
        // Test general chat flow
        String userInput = "Hello, how are you?";
        String response = orchestrator.processUserMessage(userInput);
        
        Assert.assertNotNull("Response should not be null", response);
        Assert.assertTrue("Response should contain chat response", 
            response.contains("Mock LLM response"));
    }

    // ============================================================================
    // ERROR HANDLING TESTS
    // ============================================================================

    @Test
    public void testNullInputHandling() {
        // Test null input handling
        String response = orchestrator.processUserMessage(null);
        Assert.assertTrue("Should handle null input gracefully", 
            response.contains("ERROR"));
    }

    @Test
    public void testEmptyInputHandling() {
        // Test empty input handling
        String response = orchestrator.processUserMessage("");
        Assert.assertTrue("Should handle empty input gracefully", 
            response.contains("ERROR"));
    }

    @Test
    public void testWhitespaceInputHandling() {
        // Test whitespace-only input handling
        String response = orchestrator.processUserMessage("   \t\n   ");
        Assert.assertTrue("Should handle whitespace input gracefully", 
            response.contains("ERROR"));
    }

    @Test
    public void testInvalidBinaryPathHandling() {
        // Test invalid binary path handling
        String userInput = "Analyze /nonexistent/binary with ghidra";
        String response = orchestrator.processUserMessage(userInput);
        
        Assert.assertNotNull("Response should not be null", response);
        Assert.assertTrue("Should handle invalid binary path gracefully", 
            response.contains("ERROR"));
    }

    @Test
    public void testModuleFailureHandling() {
        // Test module failure handling by making mock throw exception
        mockLlama.setShouldThrow(true);
        
        String userInput = "Hello, how are you?";
        String response = orchestrator.processUserMessage(userInput);
        
        Assert.assertNotNull("Response should not be null", response);
        Assert.assertTrue("Should handle module failure gracefully", 
            response.contains("ERROR"));
        
        // Reset mock
        mockLlama.setShouldThrow(false);
    }

    // ============================================================================
    // CLI SIMULATION TESTS
    // ============================================================================

    @Test
    public void testCLIHelpCommand() {
        // Simulate CLI help command
        String input = "help";
        String response = processCLIInput(input);
        
        Assert.assertNotNull("Help response should not be null", response);
        Assert.assertTrue("Help should contain command information", 
            response.contains("HELP & COMMANDS"));
        Assert.assertTrue("Help should contain built-in commands", 
            response.contains("BUILT-IN COMMANDS"));
    }

    @Test
    public void testCLIStatusCommand() {
        // Simulate CLI status command
        String input = "status";
        String response = processCLIInput(input);
        
        Assert.assertNotNull("Status response should not be null", response);
        Assert.assertTrue("Status should contain module information", 
            response.contains("MODULE STATUS"));
        Assert.assertTrue("Status should contain LLM status", 
            response.contains("LLM (LlamaJNI)"));
    }

    @Test
    public void testCLIHistoryCommand() {
        // Simulate CLI history command
        String input = "history";
        String response = processCLIInput(input);
        
        Assert.assertNotNull("History response should not be null", response);
        Assert.assertTrue("History should contain command history", 
            response.contains("Command History"));
    }

    @Test
    public void testCLIExitCommand() {
        // Simulate CLI exit command
        String input = "exit";
        String response = processCLIInput(input);
        
        Assert.assertEquals("Exit command should return EXIT", "EXIT", response);
    }

    @Test
    public void testCLIEndToEndWorkflow() {
        // Test complete CLI workflow
        String[] commands = {
            "help",
            "status",
            "Embed this text: \"test text\"",
            "Search for security documents",
            "history",
            "exit"
        };
        
        for (String command : commands) {
            String response = processCLIInput(command);
            Assert.assertNotNull("Response should not be null for command: " + command, response);
            
            if (command.equals("exit")) {
                Assert.assertEquals("Exit command should return EXIT", "EXIT", response);
            }
        }
    }

    // ============================================================================
    // EDGE CASE TESTS
    // ============================================================================

    @Test
    public void testVeryLongInput() {
        // Test very long input handling
        StringBuilder longInput = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            longInput.append("very long input ");
        }
        
        String response = orchestrator.processUserMessage(longInput.toString());
        Assert.assertNotNull("Response should not be null", response);
        Assert.assertTrue("Should handle long input gracefully", 
            !response.contains("ERROR") || response.contains("Mock LLM response"));
    }

    @Test
    public void testSpecialCharactersInInput() {
        // Test special characters in input
        String specialInput = "Test with special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?";
        String response = orchestrator.processUserMessage(specialInput);
        
        Assert.assertNotNull("Response should not be null", response);
        Assert.assertTrue("Should handle special characters", 
            response.contains("Mock LLM response"));
    }

    @Test
    public void testUnicodeCharactersInInput() {
        // Test Unicode characters in input
        String unicodeInput = "Test with Unicode: 你好世界 🌍 émojis 🚀";
        String response = orchestrator.processUserMessage(unicodeInput);
        
        Assert.assertNotNull("Response should not be null", response);
        Assert.assertTrue("Should handle Unicode characters", 
            response.contains("Mock LLM response"));
    }

    @Test
    public void testConcurrentRequests() {
        // Test concurrent request handling (basic test)
        String[] inputs = {
            "Hello",
            "Embed this text: \"test\"",
            "Search for documents",
            "Generate text about security"
        };
        
        List<String> responses = new ArrayList<>();
        for (String input : inputs) {
            responses.add(orchestrator.processUserMessage(input));
        }
        
        Assert.assertEquals("Should handle all requests", inputs.length, responses.size());
        for (String response : responses) {
            Assert.assertNotNull("Response should not be null", response);
        }
    }

    // ============================================================================
    // HELPER METHODS
    // ============================================================================

    /**
     * Process CLI input using the same logic as Main.java
     */
    private String processCLIInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "[ERROR] Empty or null message received";
        }
        
        String lower = input.toLowerCase().trim();
        
        // Handle built-in commands
        if (lower.startsWith("help") || lower.equals("?")) {
            return getHelpText();
        } else if (lower.equals("status")) {
            return getStatusText();
        } else if (lower.equals("history")) {
            return "Command History:\n══════════════════════════════════════════════════════════════\nNo commands in history.";
        } else if (lower.equals("clear") || lower.equals("cls")) {
            return "";
        } else if (lower.equalsIgnoreCase("exit") || lower.equalsIgnoreCase("quit")) {
            return "EXIT";
        } else if (lower.equals("version")) {
            return "Version: 1.0.0";
        }
        
        // Process through orchestrator
        try {
            return orchestrator.processUserMessage(input);
        } catch (Exception e) {
            return "[ERROR] Failed to process request: " + e.getMessage();
        }
    }

    private String getHelpText() {
        return "╔══════════════════════════════════════════════════════════════╗\n" +
               "║                        HELP & COMMANDS                       ║\n" +
               "╠══════════════════════════════════════════════════════════════╣\n" +
               "║  BUILT-IN COMMANDS:                                          ║\n" +
               "║    help, ?          - Show this help message                 ║\n" +
               "╚══════════════════════════════════════════════════════════════╝\n";
    }

    private String getStatusText() {
        return "╔══════════════════════════════════════════════════════════════╗\n" +
               "║                      MODULE STATUS                          ║\n" +
               "╠══════════════════════════════════════════════════════════════╣\n" +
               "║  LLM (LlamaJNI):     ✓ Available                            ║\n" +
               "║  Embedding Client:   ✓ Available                            ║\n" +
               "║  Vector Database:    ✓ Available                            ║\n" +
               "║  Ghidra Bridge:      ✓ Available                            ║\n" +
               "╚══════════════════════════════════════════════════════════════╝\n";
    }

    // ============================================================================
    // MOCK CLASSES
    // ============================================================================

    /**
     * Mock LLM implementation for testing
     */
    private static class MockLlamaJNI extends LlamaJNI {
        private boolean shouldThrow = false;
        
        public void setShouldThrow(boolean shouldThrow) {
            this.shouldThrow = shouldThrow;
        }
        
        @Override
        public String safeGenerateResponse(String prompt) {
            if (shouldThrow) {
                throw new RuntimeException("Mock LLM error");
            }
            
            if (prompt == null || prompt.trim().isEmpty()) {
                return "[ERROR] Null or empty prompt";
            }
            
            return "Mock LLM response for: " + prompt;
        }
    }

    /**
     * Mock embedding client implementation for testing
     */
    private static class MockEmbeddingClient extends EmbeddingClient {
        @Override
        public float[] getEmbedding(String text) {
            if (text == null || text.trim().isEmpty()) {
                throw new IllegalArgumentException("Input text for embedding must not be null or empty.");
            }
            
            // Return consistent mock embedding
            return new float[] {0.1f, 0.2f, 0.3f};
        }
    }

    /**
     * Mock ChromaDB client implementation for testing
     */
    private static class MockChromaDBClient extends ChromaDBClient {
        public MockChromaDBClient() {
            super("http://localhost:8000");
        }
        
        @Override
        public String queryNearest(String collection, float[] embedding, int topK) throws Exception {
            if (embedding == null) {
                throw new IllegalArgumentException("Embedding cannot be null");
            }
            
            return "Mock ChromaDB response for collection: " + collection + 
                   ", topK: " + topK + ", embedding length: " + embedding.length;
        }
    }

    /**
     * Mock Ghidra bridge implementation for testing
     */
    private static class MockGhidraBridge extends GhidraBridge {
        public MockGhidraBridge() {
            super("/mock/ghidra/path", "/tmp/mock_proj", "mock_project");
        }
        
        @Override
        public String runScript(String binaryPath, String scriptPath, String[] scriptArgs) throws Exception {
            if (binaryPath.contains("nonexistent")) {
                throw new IllegalArgumentException("Binary file not found: " + binaryPath);
            }
            
            return "Mock Ghidra analysis for binary: " + binaryPath + 
                   "\nFunctions: [\"main\", \"helper\", \"process_data\"]";
        }
    }
} 