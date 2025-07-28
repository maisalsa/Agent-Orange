package com.example;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * GhidraBridge - Java interface for Ghidra headless analysis
 * 
 * This class provides a Java wrapper around Ghidra's headless analysis capabilities.
 * It allows Java applications to programmatically analyze binaries using Ghidra
 * without requiring the GUI interface.
 * 
 * CONFIGURATION REQUIREMENTS:
 * 1. Ghidra Installation: Must have Ghidra installed and accessible
 * 2. analyzeHeadless Script: Path to Ghidra's support/analyzeHeadless script
 * 3. Project Directory: Directory where Ghidra projects will be created
 * 4. Permissions: Read access to binaries, write access to project directory
 * 5. Java Scripts: Ghidra Java analysis scripts (e.g., ExtractFunctions.java)
 * 
 * TYPICAL PATHS:
 * - Linux: /opt/ghidra/support/analyzeHeadless
 * - Windows: C:\ghidra\support\analyzeHeadless.bat
 * - macOS: /Applications/ghidra/support/analyzeHeadless
 * 
 * USAGE EXAMPLE:
 * <pre>
 * GhidraBridge bridge = new GhidraBridge(
 *     "/opt/ghidra/support/analyzeHeadless",  // analyzeHeadless script path
 *     "/tmp/ghidra_projects",                 // Project directory
 *     "analysis_project"                      // Project name
 * );
 * 
 * String output = bridge.runScript(
 *     "/path/to/binary.exe",                  // Binary to analyze
 *     "/path/to/ExtractFunctions.java",       // Analysis script
 *     new String[]{"arg1", "arg2"}            // Script arguments (optional)
 * );
 * </pre>
 * 
 * OUTPUT FORMAT:
 * The runScript method returns the stdout from the Ghidra analysis script.
 * For ExtractFunctions.java, this will be a JSON array of function names.
 * 
 * ERROR HANDLING:
 * - Validates all file paths before execution
 * - Checks for required permissions
 * - Provides detailed error messages for troubleshooting
 * - Handles process timeouts and resource cleanup
 */
public class GhidraBridge {
    
    /** Path to Ghidra's analyzeHeadless script (e.g., /opt/ghidra/support/analyzeHeadless) */
    private final String ghidraHeadlessPath;
    
    /** Directory where Ghidra projects will be created (e.g., /tmp/ghidra_projects) */
    private final String projectDir;
    
    /** Name for the Ghidra project (e.g., "binary_analysis") */
    private final String projectName;
    
    /** Maximum time to wait for Ghidra analysis in milliseconds (default: 5 minutes) */
    private final long timeoutMs;
    
    /** Default timeout: 5 minutes */
    private static final long DEFAULT_TIMEOUT_MS = 300000;

    /**
     * Creates a new GhidraBridge with default timeout.
     * 
     * @param ghidraHeadlessPath Path to Ghidra's analyzeHeadless script
     * @param projectDir Directory for Ghidra projects (must be writable)
     * @param projectName Name for Ghidra projects
     * @throws IllegalArgumentException if paths are invalid or inaccessible
     */
    public GhidraBridge(String ghidraHeadlessPath, String projectDir, String projectName) {
        this(ghidraHeadlessPath, projectDir, projectName, DEFAULT_TIMEOUT_MS);
    }

    /**
     * Creates a new GhidraBridge with custom timeout.
     * 
     * @param ghidraHeadlessPath Path to Ghidra's analyzeHeadless script
     * @param projectDir Directory for Ghidra projects (must be writable)
     * @param projectName Name for Ghidra projects
     * @param timeoutMs Maximum time to wait for analysis in milliseconds
     * @throws IllegalArgumentException if paths are invalid or inaccessible
     */
    public GhidraBridge(String ghidraHeadlessPath, String projectDir, String projectName, long timeoutMs) {
        this.ghidraHeadlessPath = ghidraHeadlessPath;
        this.projectDir = projectDir;
        this.projectName = projectName;
        this.timeoutMs = timeoutMs;
        
        // Validate configuration
        validateConfiguration();
    }

    /**
     * Create a GhidraBridge using configuration from application.properties or environment variables.
     * Configuration precedence (highest to lowest):
     * 1. Environment variables: GHIDRA_HOME, GHIDRA_ANALYZE_HEADLESS
     * 2. System properties: ghidra.home, ghidra.headless.path
     * 3. application.properties file
     * 4. Default paths based on common installation locations
     *
     * Environment variables:
     * - GHIDRA_HOME: Path to Ghidra installation directory (e.g., /opt/ghidra)
     * - GHIDRA_ANALYZE_HEADLESS: Full path to analyzeHeadless script
     * - GHIDRA_PROJECT_DIR: Directory for Ghidra projects (optional, defaults to /tmp/ghidra_proj)
     * - GHIDRA_PROJECT_NAME: Project name (optional, defaults to agent_orange_analysis)
     *
     * System properties:
     * - ghidra.home: Path to Ghidra installation directory
     * - ghidra.headless.path: Full path to analyzeHeadless script
     * - ghidra.project.dir: Directory for Ghidra projects
     * - ghidra.project.name: Project name
     */
    public static GhidraBridge fromConfig() {
        String headlessPath = null;
        String projectDir = null;
        String projectName = null;
        
        // Priority 1: Environment variables (highest precedence)
        String ghidraHome = System.getenv("GHIDRA_HOME");
        String ghidraHeadless = System.getenv("GHIDRA_ANALYZE_HEADLESS");
        String ghidraProjectDir = System.getenv("GHIDRA_PROJECT_DIR");
        String ghidraProjectName = System.getenv("GHIDRA_PROJECT_NAME");
        
        if (ghidraHeadless != null && !ghidraHeadless.isEmpty()) {
            headlessPath = ghidraHeadless;
        } else if (ghidraHome != null && !ghidraHome.isEmpty()) {
            // Construct path from GHIDRA_HOME
            headlessPath = ghidraHome + "/support/analyzeHeadless";
        }
        
        if (ghidraProjectDir != null && !ghidraProjectDir.isEmpty()) {
            projectDir = ghidraProjectDir;
        }
        
        if (ghidraProjectName != null && !ghidraProjectName.isEmpty()) {
            projectName = ghidraProjectName;
        }
        
        // Priority 2: System properties
        if (headlessPath == null) {
            String systemGhidraHome = System.getProperty("ghidra.home");
            String systemHeadlessPath = System.getProperty("ghidra.headless.path");
            
            if (systemHeadlessPath != null && !systemHeadlessPath.isEmpty()) {
                headlessPath = systemHeadlessPath;
            } else if (systemGhidraHome != null && !systemGhidraHome.isEmpty()) {
                headlessPath = systemGhidraHome + "/support/analyzeHeadless";
            }
        }
        
        if (projectDir == null) {
            projectDir = System.getProperty("ghidra.project.dir");
        }
        
        if (projectName == null) {
            projectName = System.getProperty("ghidra.project.name");
        }
        
        // Priority 3: application.properties file
        if (headlessPath == null || projectDir == null || projectName == null) {
            try (java.io.InputStream in = new java.io.FileInputStream("application.properties")) {
                java.util.Properties props = new java.util.Properties();
                props.load(in);
                
                if (headlessPath == null) {
                    String propHeadlessPath = props.getProperty("ghidra.headless.path");
                    if (propHeadlessPath != null && !propHeadlessPath.isEmpty()) {
                        headlessPath = propHeadlessPath;
                    }
                }
                
                if (projectDir == null) {
                    String propProjectDir = props.getProperty("ghidra.project.dir");
                    if (propProjectDir != null && !propProjectDir.isEmpty()) {
                        projectDir = propProjectDir;
                    }
                }
                
                if (projectName == null) {
                    String propProjectName = props.getProperty("ghidra.project.name");
                    if (propProjectName != null && !propProjectName.isEmpty()) {
                        projectName = propProjectName;
                    }
                }
                
            } catch (java.io.IOException ignored) {
                // Properties file not found or unreadable - continue to defaults
            }
        }
        
        // Priority 4: Default fallback paths
        if (headlessPath == null) {
            // Try common installation locations
            String[] commonPaths = {
                "/opt/ghidra/support/analyzeHeadless",           // Linux standard
                "/usr/local/ghidra/support/analyzeHeadless",    // Linux alternative
                "/Applications/ghidra/support/analyzeHeadless", // macOS
                "C:\\ghidra\\support\\analyzeHeadless.bat",     // Windows
                "C:\\Program Files\\ghidra\\support\\analyzeHeadless.bat" // Windows Program Files
            };
            
            for (String path : commonPaths) {
                if (new java.io.File(path).exists()) {
                    headlessPath = path;
                    break;
                }
            }
            
            // If still not found, use the configured path (will fail validation if not exists)
            if (headlessPath == null) {
                headlessPath = "/opt/ghidra/support/analyzeHeadless";
            }
        }
        
        if (projectDir == null) {
            projectDir = "/tmp/ghidra_proj";
        }
        
        if (projectName == null) {
            projectName = "agent_orange_analysis";
        }
        
        // Get timeout configuration
        long timeoutMs = DEFAULT_TIMEOUT_MS;
        String timeoutStr = System.getProperty("ghidra.timeout.ms");
        if (timeoutStr == null || timeoutStr.isEmpty()) {
            timeoutStr = System.getenv("GHIDRA_TIMEOUT_MS");
        }
        if (timeoutStr == null || timeoutStr.isEmpty()) {
            try (java.io.InputStream in = new java.io.FileInputStream("application.properties")) {
                java.util.Properties props = new java.util.Properties();
                props.load(in);
                timeoutStr = props.getProperty("ghidra.timeout.ms");
            } catch (java.io.IOException ignored) {
                // Properties file not found - use default
            }
        }
        
        if (timeoutStr != null && !timeoutStr.isEmpty()) {
            try {
                timeoutMs = Long.parseLong(timeoutStr);
            } catch (NumberFormatException e) {
                System.err.println("Warning: Invalid timeout value '" + timeoutStr + "', using default " + DEFAULT_TIMEOUT_MS);
            }
        }
        
        return new GhidraBridge(headlessPath, projectDir, projectName, timeoutMs);
    }

    /**
     * Validates that all required paths and permissions are available.
     * 
     * @throws IllegalArgumentException if configuration is invalid
     */
    private void validateConfiguration() {
        // Check analyzeHeadless script
        File headlessScript = new File(ghidraHeadlessPath);
        if (!headlessScript.exists()) {
            throw new IllegalArgumentException(
                "Ghidra analyzeHeadless script not found: " + ghidraHeadlessPath + 
                "\nPlease verify your Ghidra installation and path."
            );
        }
        if (!headlessScript.canExecute()) {
            throw new IllegalArgumentException(
                "Ghidra analyzeHeadless script is not executable: " + ghidraHeadlessPath +
                "\nPlease check file permissions: chmod +x " + ghidraHeadlessPath
            );
        }

        // Check project directory
        File projectDirectory = new File(projectDir);
        if (!projectDirectory.exists()) {
            if (!projectDirectory.mkdirs()) {
                throw new IllegalArgumentException(
                    "Cannot create project directory: " + projectDir +
                    "\nPlease check parent directory permissions."
                );
            }
        }
        if (!projectDirectory.canWrite()) {
            throw new IllegalArgumentException(
                "Project directory is not writable: " + projectDir +
                "\nPlease check directory permissions."
            );
        }

        // Validate project name
        if (projectName == null || projectName.trim().isEmpty()) {
            throw new IllegalArgumentException("Project name cannot be null or empty");
        }
        if (projectName.contains(File.separator) || projectName.contains("/") || projectName.contains("\\")) {
            throw new IllegalArgumentException("Project name cannot contain path separators: " + projectName);
        }
    }

    /**
     * Runs Ghidra headless analysis with the specified binary and script.
     * 
     * This method:
     * 1. Imports the binary into a Ghidra project
     * 2. Runs the specified analysis script
     * 3. Captures the script output (stdout)
     * 4. Cleans up the project after analysis
     * 
     * @param binaryPath Path to the binary file to analyze (must be readable)
     * @param scriptPath Path to the Ghidra Java script (e.g., ExtractFunctions.java)
     * @param scriptArgs Optional arguments to pass to the script (may be null)
     * @return The stdout output from the analysis script (e.g., JSON from ExtractFunctions)
     * @throws IllegalArgumentException if input paths are invalid
     * @throws IOException if file I/O operations fail
     * @throws RuntimeException if Ghidra analysis fails or times out
     */
    public String runScript(String binaryPath, String scriptPath, String[] scriptArgs) 
            throws IllegalArgumentException, IOException, RuntimeException {
        
        // Validate input parameters
        validateInputs(binaryPath, scriptPath);
        
        // Build Ghidra command
        List<String> cmd = buildGhidraCommand(binaryPath, scriptPath, scriptArgs);
        
        // Execute Ghidra analysis
        return executeGhidraAnalysis(cmd);
    }

    /**
     * Validates input parameters for the analysis.
     * 
     * @param binaryPath Path to binary file
     * @param scriptPath Path to analysis script
     * @throws IllegalArgumentException if inputs are invalid
     */
    private void validateInputs(String binaryPath, String scriptPath) {
        // Check binary file
        if (binaryPath == null || binaryPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Binary path cannot be null or empty");
        }
        File binaryFile = new File(binaryPath);
        if (!binaryFile.exists()) {
            throw new IllegalArgumentException("Binary file not found: " + binaryPath);
        }
        if (!binaryFile.canRead()) {
            throw new IllegalArgumentException("Binary file is not readable: " + binaryPath);
        }

        // Check script file
        if (scriptPath == null || scriptPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Script path cannot be null or empty");
        }
        File scriptFile = new File(scriptPath);
        if (!scriptFile.exists()) {
            throw new IllegalArgumentException("Analysis script not found: " + scriptPath);
        }
        if (!scriptFile.canRead()) {
            throw new IllegalArgumentException("Analysis script is not readable: " + scriptPath);
        }
    }

    /**
     * Builds the Ghidra analyzeHeadless command.
     * 
     * @param binaryPath Path to binary file
     * @param scriptPath Path to analysis script
     * @param scriptArgs Optional script arguments
     * @return List of command arguments
     */
    private List<String> buildGhidraCommand(String binaryPath, String scriptPath, String[] scriptArgs) {
        List<String> cmd = new ArrayList<>();
        
        // Basic command structure
        cmd.add(ghidraHeadlessPath);  // analyzeHeadless script
        cmd.add(projectDir);          // Project directory
        cmd.add(projectName);         // Project name
        cmd.add("-import");           // Import the binary
        cmd.add(binaryPath);          // Binary file path
        cmd.add("-postScript");       // Run script after import
        cmd.add(scriptPath);          // Script file path
        
        // Add script arguments if provided
        if (scriptArgs != null) {
            for (String arg : scriptArgs) {
                if (arg != null) {
                    cmd.add(arg);
                }
            }
        }
        
        // Clean up project after analysis
        cmd.add("-deleteProject");
        
        return cmd;
    }

    /**
     * Executes the Ghidra analysis command and captures output.
     * 
     * @param cmd Command to execute
     * @return Script output (stdout)
     * @throws IOException if process execution fails
     * @throws RuntimeException if analysis fails or times out
     */
    private String executeGhidraAnalysis(List<String> cmd) throws IOException, RuntimeException {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(new File(projectDir));
        pb.redirectErrorStream(true);  // Combine stdout and stderr
        
        Process process = null;
        try {
            // Start the process
            process = pb.start();
            
            // Capture output with timeout
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                
                String line;
                long startTime = System.currentTimeMillis();
                
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    
                    // Check for timeout
                    if (System.currentTimeMillis() - startTime > timeoutMs) {
                        process.destroyForcibly();
                        throw new RuntimeException(
                            "Ghidra analysis timed out after " + (timeoutMs / 1000) + " seconds. " +
                            "Consider increasing timeout or checking binary size."
                        );
                    }
                }
            }
            
            // Wait for process completion
            int exitCode = process.waitFor();
            
            if (exitCode != 0) {
                throw new RuntimeException(
                    "Ghidra analysis failed with exit code " + exitCode + ":\n" +
                    "Command: " + String.join(" ", cmd) + "\n" +
                    "Output: " + output.toString()
                );
            }
            
            return output.toString();
            
        } catch (InterruptedException e) {
            if (process != null) {
                process.destroyForcibly();
            }
            Thread.currentThread().interrupt();
            throw new RuntimeException("Ghidra analysis was interrupted", e);
        } finally {
            // Ensure process is cleaned up
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    /**
     * Gets the current configuration for debugging purposes.
     * 
     * @return Configuration summary string
     */
    public String getConfiguration() {
        return String.format(
            "GhidraBridge Configuration:\n" +
            "  analyzeHeadless: %s\n" +
            "  projectDir: %s\n" +
            "  projectName: %s\n" +
            "  timeout: %d ms",
            ghidraHeadlessPath, projectDir, projectName, timeoutMs
        );
    }

    /**
     * Example usage demonstrating how to use GhidraBridge.
     * 
     * PREREQUISITES:
     * 1. Install Ghidra (https://ghidra-sre.org/)
     * 2. Locate analyzeHeadless script in your Ghidra installation
     * 3. Ensure you have write permissions to a project directory
     * 4. Have a Ghidra Java script ready (e.g., ExtractFunctions.java)
     */
    public static void main(String[] args) {
        try {
            // Use the new configuration system
            GhidraBridge bridge = GhidraBridge.fromConfig();
            
            // Print configuration for verification
            System.out.println("✅ GhidraBridge configuration loaded successfully!");
            System.out.println(bridge.getConfiguration());
            
            // Note: Skipping actual analysis in test mode
            System.out.println("\n✅ Configuration test completed successfully!");
            System.out.println("To run actual analysis, provide a binary path as argument.");
            
        } catch (IllegalArgumentException e) {
            System.err.println("❌ Configuration error: " + e.getMessage());
            System.err.println("\nTroubleshooting steps:");
            System.err.println("1. Set environment variable: export GHIDRA_HOME=/path/to/ghidra");
            System.err.println("2. Or set direct path: export GHIDRA_ANALYZE_HEADLESS=/path/to/analyzeHeadless");
            System.err.println("3. Or update application.properties");
            System.exit(1);
        } catch (Exception e) {
            System.err.println("❌ Unexpected error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
} 