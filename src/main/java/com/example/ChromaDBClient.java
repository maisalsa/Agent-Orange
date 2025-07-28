package com.example;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import com.google.gson.Gson;

public class ChromaDBClient {
    private final String baseUrl;
    private final Gson gson = new Gson();

    /**
     * Create a ChromaDBClient using configuration from application.properties or environment variable.
     * Configuration precedence (highest to lowest):
     * 1. Environment variable VECTORDb_ENDPOINT
     * 2. application.properties file (vectordb.endpoint key)
     * 3. Default: http://localhost:8000
     *
     * To set via environment: export VECTORDb_ENDPOINT=http://host:port
     * To set via properties: vectordb.endpoint=http://host:port in application.properties
     */
    public static ChromaDBClient fromConfig() {
        // Priority 1: Environment variable (highest precedence)
        String env = System.getenv("VECTORDb_ENDPOINT");
        if (env != null && !env.isEmpty()) {
            return new ChromaDBClient(env);
        }
        
        // Priority 2: application.properties file
        try (InputStream in = new FileInputStream("application.properties")) {
            Properties props = new Properties();
            props.load(in);
            String prop = props.getProperty("vectordb.endpoint");
            if (prop != null && !prop.isEmpty()) {
                return new ChromaDBClient(prop);
            }
        } catch (IOException ignored) {
            // Properties file not found or unreadable - continue to default
        }
        
        // Priority 3: Default fallback
        return new ChromaDBClient("http://localhost:8000");
    }

    public ChromaDBClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    // Add a document with embedding to a collection
    public String addDocument(String collection, String docId, String document, float[] embedding) throws IOException {
        String endpoint = baseUrl + "/collections/" + collection + "/add";
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", docId);
        payload.put("document", document);
        payload.put("embedding", embedding);
        String json = gson.toJson(payload);
        return postJson(endpoint, json);
    }

    // Query nearest neighbors by embedding
    public String queryNearest(String collection, float[] embedding, int topK) throws IOException {
        String endpoint = baseUrl + "/collections/" + collection + "/query";
        Map<String, Object> payload = new HashMap<>();
        payload.put("embedding", embedding);
        payload.put("top_k", topK);
        String json = gson.toJson(payload);
        return postJson(endpoint, json);
    }

    // Delete/reset a collection
    public String deleteCollection(String collection) throws IOException {
        String endpoint = baseUrl + "/collections/" + collection;
        return sendRequest(endpoint, "DELETE", null);
    }

    // Helper: POST JSON
    private String postJson(String endpoint, String json) throws IOException {
        return sendRequest(endpoint, "POST", json);
    }

    // Helper: Send HTTP request
    private String sendRequest(String endpoint, String method, String body) throws IOException {
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        // Set connection timeouts to prevent hanging
        conn.setConnectTimeout(10000); // 10 seconds
        conn.setReadTimeout(30000);    // 30 seconds
        
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("User-Agent", "Agent-Orange/1.0");
        conn.setDoInput(true);
        
        if (body != null) {
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
        }
        
        int code = conn.getResponseCode();
        StringBuilder response = new StringBuilder();
        
        // Handle different response codes
        if (code < 200 || code >= 300) {
            throw new IOException("HTTP " + code + " error from ChromaDB: " + conn.getResponseMessage());
        }
        
        try (InputStream is = conn.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }
        conn.disconnect();
        if (code < 200 || code >= 300) {
            String msg = String.format("HTTP %s %s error %d: %s", method, endpoint, code, response.toString());
            System.err.println("[ChromaDBClient] " + msg);
            throw new IOException(msg);
        }
        return response.toString();
    }

    // Example usage
    public static void main(String[] args) throws Exception {
        ChromaDBClient client = new ChromaDBClient("http://localhost:8000");
        String collection = "test_collection";
        String docId = "doc1";
        String document = "This is a test document.";
        float[] embedding = {0.1f, 0.2f, 0.3f};

        // Add document
        String addResp = client.addDocument(collection, docId, document, embedding);
        System.out.println("Add response: " + addResp);

        // Query nearest
        String queryResp = client.queryNearest(collection, embedding, 3);
        System.out.println("Query response: " + queryResp);

        // Delete collection
        String delResp = client.deleteCollection(collection);
        System.out.println("Delete response: " + delResp);
    }
} 