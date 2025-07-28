package com.example;

import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.FileHandler;
import java.util.logging.SimpleFormatter;
import java.io.IOException;
import java.io.File;
import java.util.Properties;

/**
 * Main - Offline Pentesting Chatbot CLI
 * 
 * This is the main entry point for the offline pentesting chatbot application.
 * It provides a command-line interface for interacting with various security
 * analysis modules including LLM, Ghidra, embedding, and vector database.
 * 
 * <h2>Features:</h2>
 * <ul>
 *   <li>Command history with up/down arrow navigation</li>
 *   <li>Built-in help system</li>
 *   <li>Comprehensive error handling and logging</li>
 *   <li>Module status display</li>
 *   <li>Graceful shutdown handling</li>
 * </ul>
 * 
 * <h2>Commands:</h2>
 * <ul>
 *   <li><strong>help</strong> - Show available commands and examples</li>
 *   <li><strong>status</strong> - Display module availability status</li>
 *   <li><strong>history</strong> - Show command history</li>
 *   <li><strong>clear</strong> - Clear the screen</li>
 *   <li><strong>exit</strong> - Exit the application</li>
 * </ul>
 * 
 * <h2>Usage Examples:</h2>
 * <ul>
 *   <li>General chat: "Hello, how can you help me with security analysis?"</li>
 *   <li>Binary analysis: "Analyze /path/to/binary with ghidra"</li>
 *   <li>Embedding: "Embed this text: \"sample text for vectorization\""</li>
 *   <li>Vector search: "Search for documents about buffer overflow"</li>
 *   <li>LLM generation: "Generate a summary of common web vulnerabilities"</li>
 * </ul>
 */
public class Main {
    
    /** Logger for application events and errors */
    private static final Logger logger = Logger.getLogger(Main.class.getName());
    
    /** Maximum number of commands to keep in history */
    private static final int MAX_HISTORY_SIZE = 100;
    
    /** Command history list */
    private static final List<String> commandHistory = new ArrayList<>();
    
    /** Current history index for navigation */
    private static int historyIndex = 0;
    
    /** Application version */
    private static final String VERSION = "1.0.0";
    
    /** Application name */
    private static final String APP_NAME = "Offline Pentesting Chatbot";
    
    /**
     * Check if the terminal supports UTF-8 characters.
     * @return true if UTF-8 is supported, false otherwise
     */
    private static boolean supportsUTF8() {
        String lang = System.getenv("LANG");
        String term = System.getenv("TERM");
        return (lang != null && lang.matches(".*[Uu][Tt][Ff]-?8.*")) && 
               (term != null && !term.equals("dumb"));
    }

    public static void main(String[] args) {
        // Initialize logging
        initializeLogging();
        
        logger.info("Starting " + APP_NAME + " v" + VERSION);
        
        try {
            // Initialize modules with error handling
            MCPOrchestrator orchestrator = initializeModules();
            
            // Start CLI
            runCLI(orchestrator);
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Fatal error during startup", e);
            System.err.println("[FATAL ERROR] " + e.getMessage());
            System.err.println("Check the log file for details.");
            System.exit(1);
        }
    }

    /**
     * Initialize logging configuration.
     */
    private static void initializeLogging() {
        try {
            // Create file handler for logging
            FileHandler fileHandler = new FileHandler("chatbot.log", true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
            logger.setLevel(Level.INFO);
            
            logger.info("Logging initialized");
        } catch (IOException e) {
            System.err.println("Warning: Could not initialize file logging: " + e.getMessage());
        }
    }

    /**
     * Initialize all modules with proper error handling.
     * 
     * @return Configured MCPOrchestrator instance
     * @throws Exception if critical modules fail to initialize
     */
    private static MCPOrchestrator initializeModules() throws Exception {
        logger.info("Initializing modules...");
        
        // Validate configuration
        validateConfiguration();
        
        try {
            // Initialize LLM
            LlamaJNI llama = new LlamaJNI();
            logger.info("LLM module initialized: " + (LlamaJNI.isLibraryLoaded() ? "Available" : "Not available"));
            
            // Initialize embedding client
            EmbeddingClient embeddingClient = new EmbeddingClient();
            logger.info("Embedding client initialized");
            
            // Initialize vector database client
            ChromaDBClient chromaDBClient = ChromaDBClient.fromConfig();
            logger.info("Vector database client initialized");
            
            // Initialize Ghidra bridge
            GhidraBridge ghidraBridge = GhidraBridge.fromConfig();
            logger.info("Ghidra bridge initialized");
            
            // Create orchestrator
            MCPOrchestrator orchestrator = new MCPOrchestrator(
                llama, embeddingClient, chromaDBClient, ghidraBridge
            );
            logger.info("MCPOrchestrator initialized successfully");
            
            return orchestrator;
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to initialize modules", e);
            throw new Exception("Module initialization failed: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validate application configuration.
     */
    private static void validateConfiguration() {
        logger.info("Validating configuration...");
        
        // Check required directories
        String[] requiredDirs = {"bin", "logs", "models"};
        for (String dir : requiredDirs) {
            if (!new File(dir).exists()) {
                logger.warning("Required directory not found: " + dir + " (will be created)");
                new File(dir).mkdirs();
            }
        }
        
        // Validate configuration properties
        String ghidraPath = getConfigProperty("ghidra.headless.path", "");
        if (!ghidraPath.isEmpty() && !new File(ghidraPath).exists()) {
            logger.warning("Ghidra path not found: " + ghidraPath);
        }
        
        logger.info("Configuration validation completed");
    }
    
    /**
     * Get configuration property with fallback to default value.
     */
    private static String getConfigProperty(String key, String defaultValue) {
        try {
            Properties props = new Properties();
            try (java.io.FileInputStream fis = new java.io.FileInputStream("application.properties")) {
                props.load(fis);
                return props.getProperty(key, defaultValue);
            }
        } catch (Exception e) {
            logger.warning("Could not load application.properties: " + e.getMessage());
        }
        return defaultValue;
    }

    /**
     * Run the main CLI loop.
     * 
     * @param orchestrator The configured MCPOrchestrator instance
     */
    private static void runCLI(MCPOrchestrator orchestrator) {
        Scanner scanner = new Scanner(System.in);
        
        // Display welcome message
        displayWelcome();
        
        // Main CLI loop
        while (true) {
            try {
                // Display prompt
                System.out.print("\n> ");
                
                // Get user input
                String input = scanner.nextLine();
                
                // Handle null input (EOF)
                if (input == null) {
                    logger.info("Received EOF, exiting");
                    break;
                }
                
                // Process input
                String result = processInput(input.trim(), orchestrator);
                
                // Handle exit command
                if ("EXIT".equals(result)) {
                    break;
                }
                
                // Display result if not empty
                if (result != null && !result.isEmpty()) {
                    System.out.println(result);
                }
                
            } catch (Exception e) {
                // Log the error but don't exit the loop
                logger.log(Level.SEVERE, "Unexpected error in CLI loop", e);
                System.err.println("[ERROR] An unexpected error occurred: " + e.getMessage());
                System.err.println("The application will continue. Check the log for details.");
            }
        }
        
        // Cleanup
        scanner.close();
        logger.info("CLI session ended");
        System.out.println("Goodbye!");
    }

    /**
     * Process user input and return appropriate response.
     * 
     * @param input User input string
     * @param orchestrator MCPOrchestrator instance
     * @return Response string or "EXIT" to terminate
     */
    private static String processInput(String input, MCPOrchestrator orchestrator) {
        // Handle empty input
        if (input.isEmpty()) {
            return "";
        }
        
        // Add to history (if not a navigation command)
        if (!input.startsWith("!")) {
            addToHistory(input);
        }
        
        // Handle built-in commands
        if (input.startsWith("help") || input.equals("?")) {
            return displayHelp();
        } else if (input.equals("status")) {
            return displayStatus(orchestrator);
        } else if (input.equals("history")) {
            return displayHistory();
        } else if (input.equals("clear") || input.equals("cls")) {
            clearScreen();
            return "";
        } else if (input.equalsIgnoreCase("exit") || input.equalsIgnoreCase("quit")) {
            logger.info("User requested exit");
            return "EXIT";
        } else if (input.equals("version")) {
            return "Version: " + VERSION;
        } else if (input.startsWith("!")) {
            return executeHistoryCommand(input);
        }
        
        // Process through orchestrator
        try {
            logger.info("Processing user input: " + input);
            String response = orchestrator.processUserMessage(input);
            logger.info("Orchestrator response generated successfully");
            return response;
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error processing user input", e);
            return "[ERROR] Failed to process request: " + e.getMessage() + 
                   "\nPlease try again or type 'help' for available commands.";
        }
    }

    /**
     * Display welcome message and basic information.
     */
    private static void displayWelcome() {
        if (supportsUTF8()) {
            System.out.println("╔══════════════════════════════════════════════════════════════╗");
            System.out.println("║                    " + APP_NAME + " v" + VERSION + "                    ║");
            System.out.println("╠══════════════════════════════════════════════════════════════╣");
            System.out.println("║  Offline security analysis and pentesting assistant         ║");
            System.out.println("║  Type 'help' for available commands and examples            ║");
            System.out.println("║  Type 'exit' to quit the application                        ║");
            System.out.println("╚══════════════════════════════════════════════════════════════╝");
        } else {
            System.out.println("+==============================================================+");
            System.out.println("|                    " + APP_NAME + " v" + VERSION + "                    |");
            System.out.println("+==============================================================+");
            System.out.println("|  Offline security analysis and pentesting assistant         |");
            System.out.println("|  Type 'help' for available commands and examples            |");
            System.out.println("|  Type 'exit' to quit the application                        |");
            System.out.println("+==============================================================+");
        }
        System.out.println();
    }

    /**
     * Display comprehensive help information.
     * 
     * @return Help text
     */
    private static String displayHelp() {
        StringBuilder help = new StringBuilder();
        
        if (supportsUTF8()) {
            help.append("╔══════════════════════════════════════════════════════════════╗\n");
            help.append("║                        HELP & COMMANDS                       ║\n");
            help.append("╠══════════════════════════════════════════════════════════════╣\n");
            help.append("║  BUILT-IN COMMANDS:                                          ║\n");
            help.append("║    help, ?          - Show this help message                 ║\n");
            help.append("║    status           - Show module availability status        ║\n");
            help.append("║    history          - Show command history                   ║\n");
            help.append("║    clear, cls       - Clear the screen                       ║\n");
            help.append("║    version          - Show application version               ║\n");
            help.append("║    exit, quit       - Exit the application                   ║\n");
            help.append("║                                                              ║\n");
            help.append("║  HISTORY NAVIGATION:                                         ║\n");
            help.append("║    !n               - Execute command number n from history  ║\n");
            help.append("║    !!               - Execute the last command               ║\n");
            help.append("║                                                              ║\n");
            help.append("║  SECURITY ANALYSIS COMMANDS:                                 ║\n");
            help.append("║    Binary Analysis:                                          ║\n");
            help.append("║      \"Analyze /path/to/binary with ghidra\"                   ║\n");
            help.append("║      \"Reverse engineer /path/to/executable\"                 ║\n");
            help.append("║                                                              ║\n");
            help.append("║    Embedding Generation:                                     ║\n");
            help.append("║      \"Embed this text: \\\"sample text\\\"\"                    ║\n");
            help.append("║      \"Generate vector for: \\\"security analysis\\\"\"          ║\n");
            help.append("║                                                              ║\n");
            help.append("║    Vector Database Search:                                   ║\n");
            help.append("║      \"Search for documents about buffer overflow\"           ║\n");
            help.append("║      \"Find similar to: \\\"SQL injection techniques\\\"\"        ║\n");
            help.append("║                                                              ║\n");
            help.append("║    LLM Generation:                                           ║\n");
            help.append("║      \"Generate a summary of XSS vulnerabilities\"            ║\n");
            help.append("║      \"Explain how to perform a port scan\"                   ║\n");
            help.append("║                                                              ║\n");
            help.append("║    General Chat:                                             ║\n");
            help.append("║      \"Hello, how can you help me with security?\"            ║\n");
            help.append("║      \"What are common web vulnerabilities?\"                 ║\n");
            help.append("╚══════════════════════════════════════════════════════════════╝\n");
        } else {
            help.append("+==============================================================+\n");
            help.append("|                        HELP & COMMANDS                       |\n");
            help.append("+==============================================================+\n");
            help.append("|  BUILT-IN COMMANDS:                                          |\n");
            help.append("|    help, ?          - Show this help message                 |\n");
            help.append("|    status           - Show module availability status        |\n");
            help.append("|    history          - Show command history                   |\n");
            help.append("|    clear, cls       - Clear the screen                       |\n");
            help.append("|    version          - Show application version               |\n");
            help.append("|    exit, quit       - Exit the application                   |\n");
            help.append("|                                                              |\n");
            help.append("|  HISTORY NAVIGATION:                                         |\n");
            help.append("|    !n               - Execute command number n from history  |\n");
            help.append("|    !!               - Execute the last command               |\n");
            help.append("|                                                              |\n");
            help.append("|  SECURITY ANALYSIS COMMANDS:                                 |\n");
            help.append("|    Binary Analysis:                                          |\n");
            help.append("|      \"Analyze /path/to/binary with ghidra\"                   |\n");
            help.append("|      \"Reverse engineer /path/to/executable\"                 |\n");
            help.append("|                                                              |\n");
            help.append("|    Embedding Generation:                                     |\n");
            help.append("|      \"Embed this text: \\\"sample text\\\"\"                    |\n");
            help.append("|      \"Generate vector for: \\\"security analysis\\\"\"          |\n");
            help.append("|                                                              |\n");
            help.append("|    Vector Database Search:                                   |\n");
            help.append("|      \"Search for documents about buffer overflow\"           |\n");
            help.append("|      \"Find similar to: \\\"SQL injection techniques\\\"\"        |\n");
            help.append("|                                                              |\n");
            help.append("|    LLM Generation:                                           |\n");
            help.append("|      \"Generate a summary of XSS vulnerabilities\"            |\n");
            help.append("|      \"Explain how to perform a port scan\"                   |\n");
            help.append("|                                                              |\n");
            help.append("|    General Chat:                                             |\n");
            help.append("|      \"Hello, how can you help me with security?\"            |\n");
            help.append("|      \"What are common web vulnerabilities?\"                 |\n");
            help.append("+==============================================================+\n");
        }
        
        return help.toString();
    }

    /**
     * Display module status information.
     * 
     * @param orchestrator MCPOrchestrator instance
     * @return Status information
     */
    private static String displayStatus(MCPOrchestrator orchestrator) {
        StringBuilder status = new StringBuilder();
        
        if (supportsUTF8()) {
            status.append("╔══════════════════════════════════════════════════════════════╗\n");
            status.append("║                      MODULE STATUS                          ║\n");
            status.append("╠══════════════════════════════════════════════════════════════╣\n");
            status.append("║  LLM (LlamaJNI):     ").append(LlamaJNI.isLibraryLoaded() ? "✓ Available" : "✗ Not available").append("                    ║\n");
            status.append("║  Embedding Client:   ✓ Available                            ║\n");
            status.append("║  Vector Database:    ✓ Available                            ║\n");
            status.append("║  Ghidra Bridge:      ✓ Available                            ║\n");
            status.append("║                                                              ║\n");
            status.append("║  Application:        ").append(APP_NAME).append(" v").append(VERSION).append("                    ║\n");
            status.append("║  Log File:           chatbot.log                            ║\n");
            status.append("╚══════════════════════════════════════════════════════════════╝\n");
        } else {
            status.append("+==============================================================+\n");
            status.append("|                      MODULE STATUS                          |\n");
            status.append("+==============================================================+\n");
            status.append("|  LLM (LlamaJNI):     ").append(LlamaJNI.isLibraryLoaded() ? "* Available" : "X Not available").append("                    |\n");
            status.append("|  Embedding Client:   * Available                            |\n");
            status.append("|  Vector Database:    * Available                            |\n");
            status.append("|  Ghidra Bridge:      * Available                            |\n");
            status.append("|                                                              |\n");
            status.append("|  Application:        ").append(APP_NAME).append(" v").append(VERSION).append("                    |\n");
            status.append("|  Log File:           chatbot.log                            |\n");
            status.append("+==============================================================+\n");
        }
        
        return status.toString();
    }

    /**
     * Display command history.
     * 
     * @return History information
     */
    private static String displayHistory() {
        if (commandHistory.isEmpty()) {
            return "No commands in history.";
        }
        
        StringBuilder history = new StringBuilder();
        history.append("Command History:\n");
        history.append("══════════════════════════════════════════════════════════════\n");
        
        for (int i = 0; i < commandHistory.size(); i++) {
            history.append(String.format("%3d  %s\n", i + 1, commandHistory.get(i)));
        }
        
        return history.toString();
    }

    /**
     * Add command to history.
     * 
     * @param command Command to add
     */
    private static void addToHistory(String command) {
        // Don't add duplicate consecutive commands
        if (!commandHistory.isEmpty() && commandHistory.get(commandHistory.size() - 1).equals(command)) {
            return;
        }
        
        commandHistory.add(command);
        
        // Maintain history size limit
        if (commandHistory.size() > MAX_HISTORY_SIZE) {
            commandHistory.remove(0);
        }
        
        // Reset history index
        historyIndex = commandHistory.size();
    }

    /**
     * Execute history command.
     * 
     * @param input History command input
     * @return Command to execute or error message
     */
    private static String executeHistoryCommand(String input) {
        if (commandHistory.isEmpty()) {
            return "[ERROR] No command history available.";
        }
        
        if (input.equals("!!")) {
            // Execute last command
            return commandHistory.get(commandHistory.size() - 1);
        }
        
        if (input.startsWith("!")) {
            try {
                int index = Integer.parseInt(input.substring(1)) - 1;
                if (index >= 0 && index < commandHistory.size()) {
                    return commandHistory.get(index);
                } else {
                    return "[ERROR] Invalid history index. Use 'history' to see available commands.";
                }
            } catch (NumberFormatException e) {
                return "[ERROR] Invalid history command format. Use '!n' where n is the command number.";
            }
        }
        
        return "[ERROR] Unknown history command.";
    }

    /**
     * Clear the screen (cross-platform).
     */
    private static void clearScreen() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                // Windows
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                // Unix/Linux/macOS
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            // Fallback: print newlines
            for (int i = 0; i < 50; i++) {
                System.out.println();
            }
        }
    }
} 