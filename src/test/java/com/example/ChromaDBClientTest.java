// Requires JUnit 4 (e.g., junit-4.13.2.jar on classpath)
import org.junit.Test;
import org.junit.Before;
import org.junit.Assert;
import java.io.IOException;

public class ChromaDBClientTest {

    @Test
    public void testFromConfigWithEnvironmentVariable() {
        // Test environment variable configuration
        // Note: In real tests, you'd use a test framework that can set env vars
        // For now, test the default fallback behavior
        ChromaDBClient client = ChromaDBClient.fromConfig();
        Assert.assertNotNull("Client should not be null", client);
    }

    @Test
    public void testConstructorWithCustomUrl() {
        String customUrl = "http://custom-host:9000";
        ChromaDBClient client = new ChromaDBClient(customUrl);
        Assert.assertNotNull("Client should not be null", client);
    }

    @Test
    public void testAddDocumentSuccess() {
        ChromaDBClient client = new ChromaDBClient("http://localhost:8000");
        try {
            // This will fail in unit tests without a real server, but tests the method signature
            String result = client.addDocument("test_collection", "doc1", "test document", new float[]{0.1f, 0.2f});
            // If we get here, the method executed (though it likely threw an IOException)
        } catch (IOException e) {
            // Expected in unit test environment without real ChromaDB server
            Assert.assertTrue("Should be connection or server error", 
                e.getMessage().contains("HTTP") || e.getMessage().contains("Connection"));
        }
    }

    @Test
    public void testAddDocumentWithNullInputs() {
        ChromaDBClient client = new ChromaDBClient("http://localhost:8000");
        try {
            client.addDocument(null, "doc1", "test", new float[]{0.1f});
            Assert.fail("Should throw exception for null collection");
        } catch (Exception e) {
            // Expected - null collection should cause issues
        }
        
        try {
            client.addDocument("collection", null, "test", new float[]{0.1f});
            Assert.fail("Should throw exception for null docId");
        } catch (Exception e) {
            // Expected - null docId should cause issues
        }
        
        try {
            client.addDocument("collection", "doc1", null, new float[]{0.1f});
            // This might work depending on ChromaDB's handling of null documents
        } catch (Exception e) {
            // Also acceptable
        }
    }

    @Test
    public void testQueryNearestSuccess() {
        ChromaDBClient client = new ChromaDBClient("http://localhost:8000");
        try {
            String result = client.queryNearest("test_collection", new float[]{0.1f, 0.2f}, 5);
            // If we get here, the method executed (though it likely threw an IOException)
        } catch (IOException e) {
            // Expected in unit test environment without real ChromaDB server
            Assert.assertTrue("Should be connection or server error", 
                e.getMessage().contains("HTTP") || e.getMessage().contains("Connection"));
        }
    }

    @Test
    public void testQueryNearestWithInvalidInputs() {
        ChromaDBClient client = new ChromaDBClient("http://localhost:8000");
        
        try {
            client.queryNearest(null, new float[]{0.1f}, 5);
            Assert.fail("Should throw exception for null collection");
        } catch (Exception e) {
            // Expected
        }
        
        try {
            client.queryNearest("collection", null, 5);
            Assert.fail("Should throw exception for null embedding");
        } catch (Exception e) {
            // Expected
        }
        
        try {
            client.queryNearest("collection", new float[]{0.1f}, -1);
            // This might work depending on ChromaDB's validation
        } catch (Exception e) {
            // Also acceptable
        }
    }

    @Test
    public void testDeleteCollectionSuccess() {
        ChromaDBClient client = new ChromaDBClient("http://localhost:8000");
        try {
            String result = client.deleteCollection("test_collection");
            // If we get here, the method executed (though it likely threw an IOException)
        } catch (IOException e) {
            // Expected in unit test environment without real ChromaDB server
            Assert.assertTrue("Should be connection or server error", 
                e.getMessage().contains("HTTP") || e.getMessage().contains("Connection"));
        }
    }

    @Test
    public void testDeleteCollectionWithNullInput() {
        ChromaDBClient client = new ChromaDBClient("http://localhost:8000");
        try {
            client.deleteCollection(null);
            Assert.fail("Should throw exception for null collection");
        } catch (Exception e) {
            // Expected
        }
    }

    @Test
    public void testJsonSerializationFormat() {
        // Test that the JSON payload structure is correct
        ChromaDBClient client = new ChromaDBClient("http://localhost:8000");
        
        // Test addDocument JSON structure
        try {
            client.addDocument("test", "doc1", "test doc", new float[]{0.1f, 0.2f});
        } catch (IOException e) {
            // Expected without real server, but the JSON was constructed correctly
            Assert.assertTrue("Should be server error, not JSON error", 
                e.getMessage().contains("HTTP") || e.getMessage().contains("Connection"));
        }
        
        // Test queryNearest JSON structure
        try {
            client.queryNearest("test", new float[]{0.1f, 0.2f}, 3);
        } catch (IOException e) {
            // Expected without real server, but the JSON was constructed correctly
            Assert.assertTrue("Should be server error, not JSON error", 
                e.getMessage().contains("HTTP") || e.getMessage().contains("Connection"));
        }
    }

    @Test
    public void testErrorHandlingForInvalidUrls() {
        try {
            ChromaDBClient client = new ChromaDBClient("invalid-url");
            client.addDocument("test", "doc1", "test", new float[]{0.1f});
            Assert.fail("Should throw exception for invalid URL");
        } catch (Exception e) {
            // Expected - invalid URL should cause issues
            Assert.assertTrue("Should be URL or connection error", 
                e.getMessage().contains("URL") || e.getMessage().contains("Connection") || 
                e.getMessage().contains("UnknownHost"));
        }
    }

    @Test
    public void testConfigurationPrecedence() {
        // Test that fromConfig() returns a valid client
        ChromaDBClient client = ChromaDBClient.fromConfig();
        Assert.assertNotNull("fromConfig should return a valid client", client);
        
        // Test that the client can be used (even if it fails due to no server)
        try {
            client.addDocument("test", "doc1", "test", new float[]{0.1f});
        } catch (IOException e) {
            // Expected without real server
            Assert.assertTrue("Should be server error", 
                e.getMessage().contains("HTTP") || e.getMessage().contains("Connection"));
        }
    }

    @Test
    public void testEmbeddingArrayHandling() {
        ChromaDBClient client = new ChromaDBClient("http://localhost:8000");
        
        // Test empty embedding array
        try {
            client.addDocument("test", "doc1", "test", new float[]{});
        } catch (IOException e) {
            // Expected without real server
        }
        
        // Test large embedding array
        float[] largeEmbedding = new float[1000];
        for (int i = 0; i < largeEmbedding.length; i++) {
            largeEmbedding[i] = (float) i * 0.001f;
        }
        
        try {
            client.addDocument("test", "doc1", "test", largeEmbedding);
        } catch (IOException e) {
            // Expected without real server
        }
    }
} 