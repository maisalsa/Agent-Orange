import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ChromaDBClient {
    private final String baseUrl;

    public ChromaDBClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    // Helper: Convert float array to JSON array string
    private String floatArrayToJson(float[] arr) {
        if (arr == null || arr.length == 0) return "[]";
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < arr.length; i++) {
            // Use String.format with Locale.US to ensure period decimal separator
            sb.append(String.format(java.util.Locale.US, "%f", arr[i]));
            if (i < arr.length - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    // Add a document with embedding to a collection
    public String addDocument(String collection, String docId, String document, float[] embedding) throws IOException {
        String endpoint = baseUrl + "/collections/" + collection + "/add";
        String json = String.format("{\"id\":\"%s\",\"document\":\"%s\",\"embedding\":%s}",
                docId, escapeJson(document), floatArrayToJson(embedding));
        return postJson(endpoint, json);
    }

    // Query nearest neighbors by embedding
    public String queryNearest(String collection, float[] embedding, int topK) throws IOException {
        String endpoint = baseUrl + "/collections/" + collection + "/query";
        String json = String.format("{\"embedding\":%s,\"top_k\":%d}", floatArrayToJson(embedding), topK);
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
        if (code < 200 || code >= 300) {
            String msg = String.format("HTTP error %d: %s", code, response.toString());
            System.err.println("[ChromaDBClient] " + msg);
            throw new IOException(msg);
        }
        return response.toString();
    }

    // Helper: Escape JSON string
    private String escapeJson(String s) {
        if (s == null) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20 || c > 0x7E) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
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