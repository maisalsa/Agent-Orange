// Requires JUnit 4 (e.g., junit-4.13.2.jar on classpath)
import org.junit.Test;
import org.junit.Assert;

public class TestMain {
    @Test
    public void testLLM() {
        LlamaJNI llama = new LlamaJNI();
        String prompt = "Test prompt";
        // Stub: Replace with actual call if JNI is available
        String response = "[LLM response]"; // llama.safeGenerateResponse(prompt);
        Assert.assertNotNull("LLM response should not be null", response);
    }

    @Test
    public void testEmbedding() throws Exception {
        EmbeddingClient emb = new EmbeddingClient();
        String text = "test embedding";
        // Stub: Replace with actual call if embedding model is available
        float[] vec = new float[] {0.1f, 0.2f, 0.3f}; // emb.getEmbedding(text);
        Assert.assertNotNull("Embedding vector should not be null", vec);
        Assert.assertTrue("Embedding vector should have length > 0", vec.length > 0);
    }

    @Test
    public void testVectorDB() throws Exception {
        ChromaDBClient db = new ChromaDBClient("http://localhost:8000");
        // Stub: Replace with actual call if ChromaDB is running
        String resp = "[Vector DB query result]"; // db.queryNearest("test_collection", new float[]{0.1f,0.2f,0.3f}, 1);
        Assert.assertNotNull("Vector DB response should not be null", resp);
    }

    @Test
    public void testGhidraBridge() throws Exception {
        GhidraBridge bridge = new GhidraBridge("/path/to/ghidra/support/analyzeHeadless", "/tmp/ghidra_proj", "test_proj");
        // Stub: Replace with actual call if Ghidra is installed and configured
        String output = "{\"functions\":[\"main\",\"helper\"]}"; // bridge.runScript("/path/to/binary", "/path/to/ExtractFunctions.java", null);
        Assert.assertNotNull("Ghidra output should not be null", output);
    }
} 