import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        // Instantiate modules (use config or hardcoded paths as needed)
        LlamaJNI llama = new LlamaJNI();
        EmbeddingClient embeddingClient = new EmbeddingClient();
        ChromaDBClient chromaDBClient = new ChromaDBClient("http://localhost:8000");
        GhidraBridge ghidraBridge = new GhidraBridge("/opt/ghidra/support/analyzeHeadless", "/tmp/ghidra_proj", "test_proj");
        MCPOrchestrator orchestrator = new MCPOrchestrator(llama, embeddingClient, chromaDBClient, ghidraBridge);

        Scanner scanner = new Scanner(System.in);
        System.out.println("Offline Pentesting Chatbot CLI");
        System.out.println("Type 'exit' to quit.");
        while (true) {
            System.out.print("\n> ");
            String input = scanner.nextLine();
            if (input == null) break;
            input = input.trim();
            if (input.equalsIgnoreCase("exit")) {
                System.out.println("Exiting.");
                break;
            }
            if (input.isEmpty()) continue;
            try {
                String response = orchestrator.processUserMessage(input);
                System.out.println(response);
            } catch (Exception e) {
                System.err.println("[ERROR] " + e.getMessage());
            }
        }
        scanner.close();
    }
} 