// Requires JUnit 4 (e.g., junit-4.13.2.jar on classpath)
import org.junit.Test;
import org.junit.Before;
import org.junit.Assert;

public class EmbeddingClientTest {
    private EmbeddingClient client;

    @Before
    public void setUp() {
        client = new EmbeddingClient();
    }

    @Test
    public void testGetEmbeddingWithBackend() {
        // Test with a mock backend
        client.setBackend(new EmbeddingClient.EmbeddingBackend() {
            public float[] embed(String text) {
                return new float[] {1.0f, 2.0f, 3.0f};
            }
        });
        float[] result = client.getEmbedding("test input");
        Assert.assertNotNull("Embedding should not be null", result);
        Assert.assertEquals("Embedding should have expected length", 3, result.length);
        Assert.assertEquals("First element should match", 1.0f, result[0], 0.001f);
    }

    @Test
    public void testGetEmbeddingWithoutBackendThrows() {
        // Should throw when no backend is set and throwIfNoBackend is true (default)
        try {
            client.getEmbedding("test input");
            Assert.fail("Should have thrown IllegalStateException");
        } catch (IllegalStateException e) {
            Assert.assertTrue("Error message should mention backend", 
                e.getMessage().contains("No embedding backend set"));
        }
    }

    @Test
    public void testGetEmbeddingWithoutBackendReturnsDummy() {
        // Should return dummy when no backend and throwIfNoBackend is false
        client.setThrowIfNoBackend(false);
        float[] result = client.getEmbedding("test input");
        Assert.assertNotNull("Dummy embedding should not be null", result);
        Assert.assertEquals("Dummy embedding should have length 3", 3, result.length);
        Assert.assertEquals("Dummy first element", 0.1f, result[0], 0.001f);
    }

    @Test
    public void testNullInput() {
        client.setBackend(new EmbeddingClient.EmbeddingBackend() {
            public float[] embed(String text) {
                return new float[] {1.0f};
            }
        });
        try {
            client.getEmbedding(null);
            Assert.fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue("Error message should mention null", 
                e.getMessage().contains("null"));
        }
    }

    @Test
    public void testEmptyInput() {
        client.setBackend(new EmbeddingClient.EmbeddingBackend() {
            public float[] embed(String text) {
                return new float[] {1.0f};
            }
        });
        try {
            client.getEmbedding("");
            Assert.fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue("Error message should mention empty", 
                e.getMessage().contains("empty"));
        }
    }

    @Test
    public void testWhitespaceOnlyInput() {
        client.setBackend(new EmbeddingClient.EmbeddingBackend() {
            public float[] embed(String text) {
                return new float[] {1.0f};
            }
        });
        try {
            client.getEmbedding("   \t\n   ");
            Assert.fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue("Error message should mention empty", 
                e.getMessage().contains("empty"));
        }
    }

    @Test
    public void testVeryLongInput() {
        // Test with a very long input string
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            longText.append("very long text ");
        }
        client.setBackend(new EmbeddingClient.EmbeddingBackend() {
            public float[] embed(String text) {
                // Should handle long input gracefully
                return new float[] {text.length() * 0.1f, 2.0f, 3.0f};
            }
        });
        float[] result = client.getEmbedding(longText.toString());
        Assert.assertNotNull("Long input embedding should not be null", result);
        Assert.assertEquals("Long input embedding should have length 3", 3, result.length);
    }

    @Test
    public void testBackendSwapping() {
        // Test that backends can be swapped
        client.setBackend(new EmbeddingClient.EmbeddingBackend() {
            public float[] embed(String text) {
                return new float[] {1.0f};
            }
        });
        float[] result1 = client.getEmbedding("test");
        Assert.assertEquals("First backend result", 1.0f, result1[0], 0.001f);

        // Swap to different backend
        client.setBackend(new EmbeddingClient.EmbeddingBackend() {
            public float[] embed(String text) {
                return new float[] {5.0f};
            }
        });
        float[] result2 = client.getEmbedding("test");
        Assert.assertEquals("Second backend result", 5.0f, result2[0], 0.001f);
    }
} 