import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class GhidraBridge {
    private final String ghidraHeadlessPath; // Path to Ghidra's support/analyzeHeadless script
    private final String projectDir;         // Directory for Ghidra project
    private final String projectName;        // Name for Ghidra project

    public GhidraBridge(String ghidraHeadlessPath, String projectDir, String projectName) {
        this.ghidraHeadlessPath = ghidraHeadlessPath;
        this.projectDir = projectDir;
        this.projectName = projectName;
    }

    /**
     * Runs Ghidra headless with the specified binary and analysis script.
     * @param binaryPath Path to the binary to analyze
     * @param scriptPath Path to the Ghidra Java script (e.g., ExtractFunctions.java)
     * @param scriptArgs Arguments to pass to the script (may be null)
     * @return Output from the script (stdout)
     * @throws Exception on failure
     */
    public String runScript(String binaryPath, String scriptPath, String[] scriptArgs) throws Exception {
        List<String> cmd = new ArrayList<>();
        cmd.add(ghidraHeadlessPath);
        cmd.add(projectDir);
        cmd.add(projectName);
        cmd.add("-import");
        cmd.add(binaryPath);
        cmd.add("-postScript");
        cmd.add(scriptPath);
        if (scriptArgs != null) {
            for (String arg : scriptArgs) {
                cmd.add(arg);
            }
        }
        cmd.add("-deleteProject"); // Clean up after analysis

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(new File(projectDir));
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Ghidra headless exited with code " + exitCode + ":\n" + output);
        }
        return output.toString();
    }

    // Example usage
    public static void main(String[] args) throws Exception {
        String ghidraHeadless = "/path/to/ghidra/support/analyzeHeadless";
        String projectDir = "/tmp/ghidra_proj";
        String projectName = "test_proj";
        String binaryPath = "/path/to/binary";
        String scriptPath = "/path/to/ExtractFunctions.java";
        GhidraBridge bridge = new GhidraBridge(ghidraHeadless, projectDir, projectName);
        String output = bridge.runScript(binaryPath, scriptPath, null);
        System.out.println("Script output:\n" + output);
    }
} 