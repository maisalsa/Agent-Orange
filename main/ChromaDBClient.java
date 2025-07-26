import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ChromaDBClient {
    private final String baseUrl;

    public ChromaDBClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    // Add a document with embedding to a collection
    public String addDocument(String collection, String docId, String document, float[] embedding) throws IOException {
        String endpoint = baseUrl + "/collections/" + collection + "/add";
        StringBuilder embStr = new StringBuilder();
        for (int i = 0; i < embedding.length; i++) {
            embStr.append(embedding[i]);
            if (i < embedding.length - 1) embStr.append(",");
        }
        String json = String.format("{\"id\":\"%s\",\"document\":\"%s\",\"embedding\":[%s]}",
                docId, escapeJson(document), embStr.toString());
        return postJson(endpoint, json);
    }

    // Query nearest neighbors by embedding
    public String queryNearest(String collection, float[] embedding, int topK) throws IOException {
        String endpoint = baseUrl + "/collections/" + collection + "/query";
        StringBuilder embStr = new StringBuilder();
        for (int i = 0; i < embedding.length; i++) {
            embStr.append(embedding[i]);
            if (i < embedding.length - 1) embStr.append(",");
        }
        String json = String.format("{\"embedding\":[%s],\"top_k\":%d}", embStr.toString(), topK);
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
        conn.setRequestMethod(method);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoInput(true);
        if (body != null) {
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
        }
        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        conn.disconnect();
        return response.toString();
    }

    // Helper: Escape JSON string
    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
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