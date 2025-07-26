// Requires JUnit 4 (e.g., junit-4.13.2.jar on classpath)
import org.junit.Test;
import org.junit.Before;
import org.junit.Assert;

/**
 * Comprehensive test suite for EmbeddingClient.
 * 
 * Tests cover:
 * - Backend registration and configuration
 * - Input validation and error handling
 * - Backend swapping and state management
 * - Edge cases and boundary conditions
 * - Configuration options (throwIfNoBackend)
 */
public class EmbeddingClientTest {
    private EmbeddingClient client;

    @Before
    public void setUp() {
        // Create a fresh client instance for each test
        client = new EmbeddingClient();
    }

    // ============================================================================
    // BACKEND REGISTRATION AND CONFIGURATION TESTS
    // ============================================================================

    @Test
    public void testBackendRegistrationWithValidBackend() {
        // Test: Registering a valid backend should allow successful embedding generation
        client.setBackend(new EmbeddingClient.EmbeddingBackend() {
            public float[] embed(String text) {
                return new float[] {1.0f, 2.0f, 3.0f};
            }
        });
        
        float[] result = client.getEmbedding("test input");
        Assert.assertNotNull("Embedding should not be null", result);
        Assert.assertEquals("Embedding should have expected length", 3, result.length);
        Assert.assertEquals("First element should match", 1.0f, result[0], 0.001f);
        Assert.assertEquals("Second element should match", 2.0f, result[1], 0.001f);
        Assert.assertEquals("Third element should match", 3.0f, result[2], 0.001f);
    }

    @Test
    public void testBackendRegistrationWithNullBackend() {
        // Test: Setting backend to null should clear the current backend
        // First set a backend
        client.setBackend(new EmbeddingClient.EmbeddingBackend() {
            public float[] embed(String text) {
                return new float[] {1.0f};
            }
        });
        
        // Then clear it
        client.setBackend(null);
        
        // Should throw exception when no backend is set
        try {
            client.getEmbedding("test");
            Assert.fail("Should have thrown IllegalStateException after clearing backend");
        } catch (IllegalStateException e) {
            Assert.assertTrue("Error message should mention backend", 
                e.getMessage().contains("No embedding backend set"));
        }
    }

    @Test
    public void testConfigurationThrowIfNoBackendDefault() {
        // Test: Default configuration should throw exception when no backend is set
        try {
            client.getEmbedding("test input");
            Assert.fail("Should have thrown IllegalStateException by default");
        } catch (IllegalStateException e) {
            Assert.assertTrue("Error message should mention backend", 
                e.getMessage().contains("No embedding backend set"));
        }
    }

    @Test
    public void testConfigurationThrowIfNoBackendFalse() {
        // Test: When throwIfNoBackend is false, should return dummy vector instead of throwing
        client.setThrowIfNoBackend(false);
        float[] result = client.getEmbedding("test input");
        
        Assert.assertNotNull("Dummy embedding should not be null", result);
        Assert.assertEquals("Dummy embedding should have length 3", 3, result.length);
        Assert.assertEquals("Dummy first element should be 0.1", 0.1f, result[0], 0.001f);
        Assert.assertEquals("Dummy second element should be 0.2", 0.2f, result[1], 0.001f);
        Assert.assertEquals("Dummy third element should be 0.3", 0.3f, result[2], 0.001f);
    }

    @Test
    public void testConfigurationThrowIfNoBackendToggle() {
        // Test: Configuration can be toggled between throw and dummy modes
        // Start with throw mode (default)
        try {
            client.getEmbedding("test");
            Assert.fail("Should throw by default");
        } catch (IllegalStateException e) {
            // Expected
        }
        
        // Switch to dummy mode
        client.setThrowIfNoBackend(false);
        float[] result = client.getEmbedding("test");
        Assert.assertEquals("Should return dummy vector", 0.1f, result[0], 0.001f);
        
        // Switch back to throw mode
        client.setThrowIfNoBackend(true);
        try {
            client.getEmbedding("test");
            Assert.fail("Should throw again after switching back");
        } catch (IllegalStateException e) {
            // Expected
        }
    }

    // ============================================================================
    // INPUT VALIDATION TESTS
    // ============================================================================

    @Test
    public void testInputValidationNullText() {
        // Test: Null input should throw IllegalArgumentException regardless of backend
        client.setBackend(new EmbeddingClient.EmbeddingBackend() {
            public float[] embed(String text) {
                return new float[] {1.0f};
            }
        });
        
        try {
            client.getEmbedding(null);
            Assert.fail("Should have thrown IllegalArgumentException for null input");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue("Error message should mention null", 
                e.getMessage().contains("null"));
        }
    }

    @Test
    public void testInputValidationEmptyText() {
        // Test: Empty string should throw IllegalArgumentException
        client.setBackend(new EmbeddingClient.EmbeddingBackend() {
            public float[] embed(String text) {
                return new float[] {1.0f};
            }
        });
        
        try {
            client.getEmbedding("");
            Assert.fail("Should have thrown IllegalArgumentException for empty input");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue("Error message should mention empty", 
                e.getMessage().contains("empty"));
        }
    }

    @Test
    public void testInputValidationWhitespaceOnlyText() {
        // Test: Whitespace-only string should throw IllegalArgumentException
        client.setBackend(new EmbeddingClient.EmbeddingBackend() {
            public float[] embed(String text) {
                return new float[] {1.0f};
            }
        });
        
        try {
            client.getEmbedding("   \t\n   ");
            Assert.fail("Should have thrown IllegalArgumentException for whitespace-only input");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue("Error message should mention empty", 
                e.getMessage().contains("empty"));
        }
    }

    @Test
    public void testInputValidationWithValidText() {
        // Test: Valid text should pass validation and reach the backend
        final boolean[] backendCalled = {false};
        client.setBackend(new EmbeddingClient.EmbeddingBackend() {
            public float[] embed(String text) {
                backendCalled[0] = true;
                Assert.assertEquals("Backend should receive the original text", "valid text", text);
                return new float[] {1.0f};
            }
        });
        
        client.getEmbedding("valid text");
        Assert.assertTrue("Backend should have been called", backendCalled[0]);
    }

    // ============================================================================
    // BACKEND SWAPPING AND STATE MANAGEMENT TESTS
    // ============================================================================

    @Test
    public void testBackendSwappingBetweenDifferentBackends() {
        // Test: Backends can be swapped and each should produce different results
        // First backend
        client.setBackend(new EmbeddingClient.EmbeddingBackend() {
            public float[] embed(String text) {
                return new float[] {1.0f, 2.0f};
            }
        });
        float[] result1 = client.getEmbedding("test");
        Assert.assertEquals("First backend first element", 1.0f, result1[0], 0.001f);
        Assert.assertEquals("First backend second element", 2.0f, result1[1], 0.001f);

        // Second backend with different dimension
        client.setBackend(new EmbeddingClient.EmbeddingBackend() {
            public float[] embed(String text) {
                return new float[] {5.0f, 6.0f, 7.0f};
            }
        });
        float[] result2 = client.getEmbedding("test");
        Assert.assertEquals("Second backend first element", 5.0f, result2[0], 0.001f);
        Assert.assertEquals("Second backend second element", 6.0f, result2[1], 0.001f);
        Assert.assertEquals("Second backend third element", 7.0f, result2[2], 0.001f);
        Assert.assertEquals("Second backend should have different length", 3, result2.length);
    }

    @Test
    public void testBackendSwappingWithSameInput() {
        // Test: Same input should produce different results with different backends
        final String testInput = "same input text";
        
        // First backend
        client.setBackend(new EmbeddingClient.EmbeddingBackend() {
            public float[] embed(String text) {
                Assert.assertEquals("Backend should receive correct input", testInput, text);
                return new float[] {text.length() * 0.1f};
            }
        });
        float[] result1 = client.getEmbedding(testInput);
        
        // Second backend
        client.setBackend(new EmbeddingClient.EmbeddingBackend() {
            public float[] embed(String text) {
                Assert.assertEquals("Backend should receive correct input", testInput, text);
                return new float[] {text.hashCode() * 0.001f};
            }
        });
        float[] result2 = client.getEmbedding(testInput);
        
        // Results should be different
        Assert.assertNotEquals("Different backends should produce different results", 
            result1[0], result2[0], 0.001f);
    }

    // ============================================================================
    // EDGE CASES AND BOUNDARY CONDITIONS
    // ============================================================================

    @Test
    public void testVeryLongInputText() {
        // Test: Very long input should be handled gracefully by the backend
        StringBuilder longText = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            longText.append("very long text ");
        }
        String input = longText.toString();
        
        client.setBackend(new EmbeddingClient.EmbeddingBackend() {
            public float[] embed(String text) {
                Assert.assertEquals("Backend should receive the full long text", input, text);
                return new float[] {text.length() * 0.1f, 2.0f, 3.0f};
            }
        });
        
        float[] result = client.getEmbedding(input);
        Assert.assertNotNull("Long input embedding should not be null", result);
        Assert.assertEquals("Long input embedding should have length 3", 3, result.length);
        Assert.assertEquals("First element should reflect text length", input.length() * 0.1f, result[0], 0.001f);
    }

    @Test
    public void testSpecialCharactersInInput() {
        // Test: Input with special characters should be handled correctly
        String specialText = "Text with special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?";
        
        client.setBackend(new EmbeddingClient.EmbeddingBackend() {
            public float[] embed(String text) {
                Assert.assertEquals("Backend should receive the special text unchanged", specialText, text);
                return new float[] {text.length() * 0.01f};
            }
        });
        
        float[] result = client.getEmbedding(specialText);
        Assert.assertNotNull("Special character embedding should not be null", result);
        Assert.assertEquals("Result should reflect text length", specialText.length() * 0.01f, result[0], 0.001f);
    }

    @Test
    public void testUnicodeCharactersInInput() {
        // Test: Unicode characters should be handled correctly
        String unicodeText = "Unicode: 你好世界 🌍 émojis 🚀";
        
        client.setBackend(new EmbeddingClient.EmbeddingBackend() {
            public float[] embed(String text) {
                Assert.assertEquals("Backend should receive the unicode text unchanged", unicodeText, text);
                return new float[] {text.codePointCount(0, text.length()) * 0.1f};
            }
        });
        
        float[] result = client.getEmbedding(unicodeText);
        Assert.assertNotNull("Unicode embedding should not be null", result);
        Assert.assertEquals("Result should reflect code point count", 
            unicodeText.codePointCount(0, unicodeText.length()) * 0.1f, result[0], 0.001f);
    }

    @Test
    public void testBackendExceptionHandling() {
        // Test: Exceptions from backend should be propagated correctly
        client.setBackend(new EmbeddingClient.EmbeddingBackend() {
            public float[] embed(String text) {
                throw new RuntimeException("Backend error");
            }
        });
        
        try {
            client.getEmbedding("test");
            Assert.fail("Should propagate backend exception");
        } catch (RuntimeException e) {
            Assert.assertEquals("Should preserve backend error message", "Backend error", e.getMessage());
        }
    }

    @Test
    public void testBackendReturnsNull() {
        // Test: Backend returning null should be handled gracefully
        client.setBackend(new EmbeddingClient.EmbeddingBackend() {
            public float[] embed(String text) {
                return null;
            }
        });
        
        try {
            client.getEmbedding("test");
            Assert.fail("Should handle null return from backend");
        } catch (Exception e) {
            // Expected - null return should cause issues
        }
    }
} 