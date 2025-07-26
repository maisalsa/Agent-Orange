public class LlamaJNI {
    static {
        System.loadLibrary("llama"); // Loads libllama.so
    }

    // Native method declaration
    public native String generateResponse(String prompt);

    // Safe wrapper for native call
    public String safeGenerateResponse(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            return "[ERROR] Null or empty prompt";
        }
        return generateResponse(prompt);
    }

    // Example usage
    public static void main(String[] args) {
        LlamaJNI llama = new LlamaJNI();
        String prompt = "Hello, Llama!";
        String response = llama.generateResponse(prompt);
        System.out.println("Model response: " + response);
    }
} 