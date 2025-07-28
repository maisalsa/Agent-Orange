package com.example;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Natural language command processor for information gathering operations.
 * 
 * This class processes user commands related to file analysis, Burp Suite
 * integration, and reconnaissance activities, providing a conversational
 * interface for security testing workflows.
 * 
 * @author Agent-Orange Team
 * @version 1.0
 */
public class InformationGatheringCommandProcessor {
    private static final Logger logger = Logger.getLogger(InformationGatheringCommandProcessor.class.getName());
    
    private final InformationGatherer informationGatherer;
    private final ProjectManager projectManager;
    
    // Command patterns for natural language processing
    private static final Map<Pattern, String> COMMAND_PATTERNS = new HashMap<>();
    
    static {
        // Information gathering commands
        COMMAND_PATTERNS.put(Pattern.compile("(?i)gather\\s+info(?:rmation)?\\s+(?:on\\s+)?(?:project\\s+)?['\"]?([^'\"]+)['\"]?"), "gather_info");
        COMMAND_PATTERNS.put(Pattern.compile("(?i)analyze\\s+file\\s+['\"]?([^'\"]+)['\"]?"), "analyze_file");
        COMMAND_PATTERNS.put(Pattern.compile("(?i)analyze\\s+directory\\s+['\"]?([^'\"]+)['\"]?"), "analyze_directory");
        COMMAND_PATTERNS.put(Pattern.compile("(?i)scan\\s+files\\s+in\\s+['\"]?([^'\"]+)['\"]?"), "analyze_directory");
        
        // Burp Suite commands
        COMMAND_PATTERNS.put(Pattern.compile("(?i)import\\s+burp\\s+(?:data\\s+)?(?:from\\s+)?['\"]?([^'\"]+)['\"]?(?:\\s+into\\s+(?:project\\s+)?['\"]?([^'\"]+)['\"]?)?"), "import_burp");
        COMMAND_PATTERNS.put(Pattern.compile("(?i)load\\s+burp\\s+(?:export\\s+)?['\"]?([^'\"]+)['\"]?"), "import_burp");
        
        // Query commands
        COMMAND_PATTERNS.put(Pattern.compile("(?i)(?:list\\s+|show\\s+)?passwords?\\s+(?:found\\s+)?(?:in\\s+)?(?:project\\s+)?['\"]?([^'\"]+)['\"]?"), "list_passwords");
        COMMAND_PATTERNS.put(Pattern.compile("(?i)(?:list\\s+|show\\s+)?(?:api\\s+)?keys?\\s+(?:found\\s+)?(?:in\\s+)?(?:project\\s+)?['\"]?([^'\"]+)['\"]?"), "list_api_keys");
        COMMAND_PATTERNS.put(Pattern.compile("(?i)(?:show\\s+|list\\s+)?(?:api\\s+)?endpoints?\\s+(?:found\\s+)?(?:in\\s+)?['\"]?([^'\"]+)['\"]?"), "list_endpoints");
        COMMAND_PATTERNS.put(Pattern.compile("(?i)(?:show\\s+|list\\s+)?burp\\s+(?:vulnerabilities|vulns|findings)\\s+(?:for\\s+)?([\\d\\.]+|[a-zA-Z0-9\\.-]+)\\s+(?:in\\s+)?(?:project\\s+)?['\"]?([^'\"]+)['\"]?"), "list_burp_vulns_host");
        COMMAND_PATTERNS.put(Pattern.compile("(?i)(?:show\\s+|list\\s+)?burp\\s+(?:vulnerabilities|vulns|findings)\\s+(?:in\\s+)?(?:project\\s+)?['\"]?([^'\"]+)['\"]?"), "list_burp_vulns");
        
        // Session management
        COMMAND_PATTERNS.put(Pattern.compile("(?i)(?:show\\s+|list\\s+)?gathering\\s+(?:sessions?|status)"), "list_sessions");
        COMMAND_PATTERNS.put(Pattern.compile("(?i)(?:show\\s+)?(?:gathering\\s+)?session\\s+['\"]?([^'\"]+)['\"]?"), "show_session");
        COMMAND_PATTERNS.put(Pattern.compile("(?i)close\\s+(?:gathering\\s+)?session\\s+['\"]?([^'\"]+)['\"]?"), "close_session");
        
        // Help commands
        COMMAND_PATTERNS.put(Pattern.compile("(?i)(?:help\\s+)?(?:info\\s+)?gather(?:ing)?"), "help_gathering");
        COMMAND_PATTERNS.put(Pattern.compile("(?i)(?:help\\s+)?burp\\s+(?:suite\\s+)?(?:integration)?"), "help_burp");
    }
    
    /**
     * Constructs a new InformationGatheringCommandProcessor.
     * 
     * @param informationGatherer The information gatherer instance
     * @param projectManager The project manager instance
     */
    public InformationGatheringCommandProcessor(InformationGatherer informationGatherer, ProjectManager projectManager) {
        this.informationGatherer = informationGatherer;
        this.projectManager = projectManager;
        
        logger.info("InformationGatheringCommandProcessor initialized");
    }
    
    /**
     * Checks if a message is an information gathering command.
     * 
     * @param message The user message to check
     * @return true if it's an information gathering command
     */
    public boolean isInformationGatheringCommand(String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }
        
        String normalized = message.trim().toLowerCase();
        
        // Quick keyword checks
        String[] keywords = {"gather", "analyze", "import burp", "load burp", "show passwords", 
                           "list passwords", "show keys", "list keys", "show endpoints", 
                           "list endpoints", "burp vulnerabilities", "burp vulns", "burp findings",
                           "gathering session"};
        
        for (String keyword : keywords) {
            if (normalized.contains(keyword)) {
                return true;
            }
        }
        
        // Pattern-based checks
        for (Pattern pattern : COMMAND_PATTERNS.keySet()) {
            if (pattern.matcher(message).find()) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Processes an information gathering command.
     * 
     * @param message The user's command message
     * @return Response string
     */
    public String processCommand(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "❌ Empty command. Type 'help gathering' for available commands.";
        }
        
        try {
            // Match command patterns
            for (Map.Entry<Pattern, String> entry : COMMAND_PATTERNS.entrySet()) {
                Pattern pattern = entry.getKey();
                String commandType = entry.getValue();
                Matcher matcher = pattern.matcher(message);
                
                if (matcher.find()) {
                    logger.info("Processing information gathering command: " + commandType);
                    return executeCommand(commandType, matcher, message);
                }
            }
            
            // If no pattern matches, provide help
            return "❓ Command not recognized. Available information gathering commands:\n\n" +
                   getHelpText();
            
        } catch (Exception e) {
            logger.severe("Error processing information gathering command: " + e.getMessage());
            return "❌ Error processing command: " + e.getMessage();
        }
    }
    
    /**
     * Executes a specific command based on the matched pattern.
     * 
     * @param commandType The type of command to execute
     * @param matcher The regex matcher containing captured groups
     * @param originalMessage The original user message
     * @return Response string
     */
    private String executeCommand(String commandType, Matcher matcher, String originalMessage) {
        switch (commandType) {
            case "gather_info":
                return handleGatherInfo(matcher.group(1));
                
            case "analyze_file":
                return handleAnalyzeFile(matcher.group(1));
                
            case "analyze_directory":
                return handleAnalyzeDirectory(matcher.group(1));
                
            case "import_burp":
                String burpFile = matcher.group(1);
                String targetProject = matcher.groupCount() > 1 ? matcher.group(2) : null;
                return handleImportBurp(burpFile, targetProject);
                
            case "list_passwords":
                return handleListPasswords(matcher.group(1));
                
            case "list_api_keys":
                return handleListApiKeys(matcher.group(1));
                
            case "list_endpoints":
                return handleListEndpoints(matcher.group(1));
                
            case "list_burp_vulns":
                return handleListBurpVulnerabilities(matcher.group(1), null);
                
            case "list_burp_vulns_host":
                return handleListBurpVulnerabilities(matcher.group(2), matcher.group(1));
                
            case "list_sessions":
                return handleListSessions();
                
            case "show_session":
                return handleShowSession(matcher.group(1));
                
            case "close_session":
                return handleCloseSession(matcher.group(1));
                
            case "help_gathering":
                return getHelpText();
                
            case "help_burp":
                return getBurpHelpText();
                
            default:
                return "❓ Unknown command type: " + commandType;
        }
    }
    
    /**
     * Handles the "gather info" command.
     * 
     * @param projectName The project name
     * @return Response string
     */
    private String handleGatherInfo(String projectName) {
        try {
            // Check if project exists
            if (!projectManager.getProjectNames().contains(projectName)) {
                return "❌ Project not found: " + projectName + "\n" +
                       "Available projects: " + String.join(", ", projectManager.getProjectNames());
            }
            
            // Start a gathering session
            String sessionId = informationGatherer.startGatheringSession(projectName);
            
            // Get project details for scoping
            Project project = projectManager.getProject(projectName);
            Set<String> targetSet = project.getTargets();
            List<String> targets = new ArrayList<>(targetSet);
            
            if (targets.isEmpty()) {
                return "⚠️  Project '" + projectName + "' has no targets defined.\n" +
                       "Add targets first using: 'add target <target>'\n\n" +
                       "Gathering session started: " + sessionId;
            }
            
            return "🔍 Information gathering started for project: " + projectName + "\n" +
                   "📋 Session ID: " + sessionId + "\n" +
                   "🎯 Targets: " + String.join(", ", targets) + "\n\n" +
                   "💡 Next steps:\n" +
                   "• analyze file '<file_path>'\n" +
                   "• analyze directory '<directory_path>'\n" +
                   "• import burp data from '<burp_export.xml>'\n" +
                   "• show gathering session '" + sessionId + "'";
            
        } catch (Exception e) {
            logger.warning("Failed to start gathering for project " + projectName + ": " + e.getMessage());
            return "❌ Failed to start information gathering: " + e.getMessage();
        }
    }
    
    /**
     * Handles file analysis commands.
     * 
     * @param filePath The file path to analyze
     * @return Response string
     */
    private String handleAnalyzeFile(String filePath) {
        try {
            // Get current project and session
            Project currentProject = projectManager.getCurrentProject();
            if (currentProject == null) {
                return "❌ No active project. Create or open a project first.";
            }
            
            // Find or create a gathering session for this project
            String sessionId = getOrCreateSessionForProject(currentProject.getName());
            
            // Analyze the file
            ExtractedData data = informationGatherer.analyzeFile(sessionId, filePath);
            
            StringBuilder response = new StringBuilder();
            response.append("📁 File Analysis Complete\n");
            response.append("═══════════════════════════\n");
            response.append("📄 File: ").append(filePath).append("\n");
            response.append("📊 Items extracted: ").append(data.getTotalItemCount()).append("\n");
            response.append("🔒 Sensitive data: ").append(data.hasSensitiveData() ? "Yes" : "No").append("\n\n");
            
            // Show summary by category
            Map<ExtractedData.DataType, List<String>> allData = data.getAllExtractedInfo(true);
            for (Map.Entry<ExtractedData.DataType, List<String>> entry : allData.entrySet()) {
                List<String> values = entry.getValue();
                if (!values.isEmpty()) {
                    response.append(entry.getKey().getFormattedName()).append(" (").append(values.size()).append("):\n");
                    for (String value : values.subList(0, Math.min(values.size(), 5))) {
                        response.append("  • ").append(value).append("\n");
                    }
                    if (values.size() > 5) {
                        response.append("  ... and ").append(values.size() - 5).append(" more\n");
                    }
                    response.append("\n");
                }
            }
            
            // Show warnings
            Set<String> warnings = data.getWarnings();
            if (!warnings.isEmpty()) {
                response.append("⚠️  Warnings:\n");
                for (String warning : warnings) {
                    response.append("  ⚠️  ").append(warning).append("\n");
                }
                response.append("\n");
            }
            
            response.append("💡 Use 'show gathering session ").append(sessionId).append("' for full session details");
            
            return response.toString();
            
        } catch (Exception e) {
            logger.warning("Failed to analyze file " + filePath + ": " + e.getMessage());
            return "❌ Failed to analyze file: " + e.getMessage();
        }
    }
    
    /**
     * Handles directory analysis commands.
     * 
     * @param directoryPath The directory path to analyze
     * @return Response string
     */
    private String handleAnalyzeDirectory(String directoryPath) {
        try {
            // Get current project and session
            Project currentProject = projectManager.getCurrentProject();
            if (currentProject == null) {
                return "❌ No active project. Create or open a project first.";
            }
            
            String sessionId = getOrCreateSessionForProject(currentProject.getName());
            
            // Analyze the directory (recursive, max 50 files for performance)
            Map<String, ExtractedData> results = informationGatherer.analyzeDirectory(sessionId, directoryPath, true, 50);
            
            StringBuilder response = new StringBuilder();
            response.append("📁 Directory Analysis Complete\n");
            response.append("════════════════════════════════\n");
            response.append("📂 Directory: ").append(directoryPath).append("\n");
            response.append("📄 Files analyzed: ").append(results.size()).append("\n");
            
            // Count total items and sensitive files
            int totalItems = results.values().stream().mapToInt(ExtractedData::getTotalItemCount).sum();
            long sensitiveFiles = results.values().stream().filter(ExtractedData::hasSensitiveData).count();
            
            response.append("📊 Total items: ").append(totalItems).append("\n");
            response.append("🔒 Files with sensitive data: ").append(sensitiveFiles).append("\n\n");
            
            // Summary by data type across all files
            Map<ExtractedData.DataType, Integer> typeCounts = new HashMap<>();
            for (ExtractedData.DataType type : ExtractedData.DataType.values()) {
                typeCounts.put(type, 0);
            }
            
            for (ExtractedData data : results.values()) {
                for (ExtractedData.DataType type : ExtractedData.DataType.values()) {
                    typeCounts.merge(type, data.getItemCount(type), Integer::sum);
                }
            }
            
            response.append("📈 Data Summary:\n");
            for (Map.Entry<ExtractedData.DataType, Integer> entry : typeCounts.entrySet()) {
                int count = entry.getValue();
                if (count > 0) {
                    response.append("  ").append(entry.getKey().getFormattedName())
                           .append(": ").append(count).append("\n");
                }
            }
            
            // Show files with most sensitive data
            List<ExtractedData> sensitiveFilesList = results.values().stream()
                                                           .filter(ExtractedData::hasSensitiveData)
                                                           .sorted((a, b) -> Integer.compare(b.getTotalItemCount(), a.getTotalItemCount()))
                                                           .limit(5)
                                                           .collect(Collectors.toList());
            
            if (!sensitiveFilesList.isEmpty()) {
                response.append("\n🔒 Files with most sensitive data:\n");
                for (ExtractedData data : sensitiveFilesList) {
                    response.append("  📄 ").append(data.getFilePath())
                           .append(" (").append(data.getTotalItemCount()).append(" items)\n");
                }
            }
            
            response.append("\n💡 Use 'show gathering session ").append(sessionId).append("' for full session details");
            
            return response.toString();
            
        } catch (Exception e) {
            logger.warning("Failed to analyze directory " + directoryPath + ": " + e.getMessage());
            return "❌ Failed to analyze directory: " + e.getMessage();
        }
    }
    
    /**
     * Handles Burp Suite import commands.
     * 
     * @param burpFile The Burp Suite XML export file path
     * @param targetProject The target project name (optional)
     * @return Response string
     */
    private String handleImportBurp(String burpFile, String targetProject) {
        try {
            // Determine target project
            Project project;
            if (targetProject != null && !targetProject.trim().isEmpty()) {
                if (!projectManager.getProjectNames().contains(targetProject)) {
                    return "❌ Project not found: " + targetProject + "\n" +
                           "Available projects: " + String.join(", ", projectManager.getProjectNames());
                }
                project = projectManager.getProject(targetProject);
            } else {
                project = projectManager.getCurrentProject();
                if (project == null) {
                    return "❌ No active project. Create or open a project first, or specify target project.";
                }
            }
            
            String sessionId = getOrCreateSessionForProject(project.getName());
            
            // Import Burp Suite data
            BurpSuiteParser.BurpParseResult result = informationGatherer.importBurpSuite(sessionId, burpFile);
            
            StringBuilder response = new StringBuilder();
            response.append("🔍 Burp Suite Import Complete\n");
            response.append("═══════════════════════════════\n");
            response.append("📁 File: ").append(burpFile).append("\n");
            response.append("📊 Project: ").append(project.getName()).append("\n");
            response.append("🔍 Findings: ").append(result.getFindings().size()).append("\n");
            response.append("🎯 Targets: ").append(result.getTargets().size()).append("\n\n");
            
            // Severity breakdown
            Map<Severity, Integer> severityCounts = result.getSeveritySummary();
            response.append("📈 Severity Breakdown:\n");
            for (Severity severity : Severity.values()) {
                int count = severityCounts.get(severity);
                if (count > 0) {
                    response.append("  ").append(severity.getIcon()).append(" ")
                           .append(severity.getDisplayName()).append(": ").append(count).append("\n");
                }
            }
            
            // Add vulnerabilities to project
            int addedVulns = 0;
            for (BurpSuiteParser.BurpFinding finding : result.getFindings()) {
                try {
                    Vulnerability vuln = finding.toVulnerability();
                    
                                         // Set the target on the vulnerability and add it to the project
                     if (finding.getHost() != null) {
                         // Create a new vulnerability with the host as target
                         Vulnerability vulnWithTarget = new Vulnerability(
                             "BURP_" + finding.getIssueType(),
                             vuln.getName(), 
                             vuln.getDescription(), 
                             vuln.getSeverity(), 
                             finding.getHost()
                         );
                         
                         // Add the host as a target if it's not already present
                         project.addTarget(finding.getHost());
                         
                         // Add the vulnerability to the project
                         project.addVulnerability(vulnWithTarget);
                         addedVulns++;
                     }
                    
                } catch (Exception e) {
                    logger.warning("Failed to add Burp finding to project: " + e.getMessage());
                }
            }
            
            if (addedVulns > 0) {
                response.append("\n✅ Added ").append(addedVulns).append(" vulnerabilities to project\n");
            }
            
            response.append("\n💡 Use 'list burp vulnerabilities in ").append(project.getName()).append("' to see findings");
            
            return response.toString();
            
        } catch (Exception e) {
            logger.warning("Failed to import Burp Suite data from " + burpFile + ": " + e.getMessage());
            return "❌ Failed to import Burp Suite data: " + e.getMessage();
        }
    }
    
    /**
     * Handles list passwords commands.
     * 
     * @param projectName The project name
     * @return Response string
     */
    private String handleListPasswords(String projectName) {
        try {
            String sessionId = findSessionForProject(projectName);
            if (sessionId == null) {
                return "❌ No gathering session found for project: " + projectName + "\n" +
                       "Start information gathering first with: 'gather info on " + projectName + "'";
            }
            
            InformationGatherer.GatheringSession session = informationGatherer.getSession(sessionId);
            List<String> credentials = session.getAllCredentials();
            
            if (credentials.isEmpty()) {
                return "ℹ️  No passwords found in project: " + projectName;
            }
            
            StringBuilder response = new StringBuilder();
            response.append("🔑 Passwords Found in Project: ").append(projectName).append("\n");
            response.append("═══════════════════════════════════════════\n");
            response.append("⚠️  Showing sanitized versions for security\n\n");
            
            for (int i = 0; i < credentials.size(); i++) {
                response.append("  ").append(i + 1).append(". ").append(credentials.get(i)).append("\n");
            }
            
            response.append("\n🔒 Total: ").append(credentials.size()).append(" credentials found");
            response.append("\n⚠️  Raw credentials are logged securely and not displayed");
            
            return response.toString();
            
        } catch (Exception e) {
            logger.warning("Failed to list passwords for project " + projectName + ": " + e.getMessage());
            return "❌ Failed to list passwords: " + e.getMessage();
        }
    }
    
    /**
     * Handles list API keys commands.
     * 
     * @param projectName The project name
     * @return Response string
     */
    private String handleListApiKeys(String projectName) {
        try {
            String sessionId = findSessionForProject(projectName);
            if (sessionId == null) {
                return "❌ No gathering session found for project: " + projectName;
            }
            
            InformationGatherer.GatheringSession session = informationGatherer.getSession(sessionId);
            List<String> apiKeys = session.getAllApiKeys();
            
            if (apiKeys.isEmpty()) {
                return "ℹ️  No API keys found in project: " + projectName;
            }
            
            StringBuilder response = new StringBuilder();
            response.append("🗝️ API Keys Found in Project: ").append(projectName).append("\n");
            response.append("═══════════════════════════════════════\n");
            response.append("⚠️  Showing sanitized versions for security\n\n");
            
            for (int i = 0; i < apiKeys.size(); i++) {
                response.append("  ").append(i + 1).append(". ").append(apiKeys.get(i)).append("\n");
            }
            
            response.append("\n🔒 Total: ").append(apiKeys.size()).append(" API keys found");
            
            return response.toString();
            
        } catch (Exception e) {
            logger.warning("Failed to list API keys for project " + projectName + ": " + e.getMessage());
            return "❌ Failed to list API keys: " + e.getMessage();
        }
    }
    
    /**
     * Handles list endpoints commands.
     * 
     * @param projectName The project name
     * @return Response string
     */
    private String handleListEndpoints(String projectName) {
        try {
            String sessionId = findSessionForProject(projectName);
            if (sessionId == null) {
                return "❌ No gathering session found for project: " + projectName;
            }
            
            InformationGatherer.GatheringSession session = informationGatherer.getSession(sessionId);
            List<String> endpoints = session.getAllEndpoints();
            
            if (endpoints.isEmpty()) {
                return "ℹ️  No endpoints found in project: " + projectName;
            }
            
            StringBuilder response = new StringBuilder();
            response.append("🌐 Endpoints Found in Project: ").append(projectName).append("\n");
            response.append("══════════════════════════════════════════\n\n");
            
            // Group endpoints by type
            List<String> urls = endpoints.stream().filter(e -> e.startsWith("http")).collect(Collectors.toList());
            List<String> paths = endpoints.stream().filter(e -> e.startsWith("/")).collect(Collectors.toList());
            List<String> other = endpoints.stream().filter(e -> !e.startsWith("http") && !e.startsWith("/")).collect(Collectors.toList());
            
            if (!urls.isEmpty()) {
                response.append("🔗 URLs:\n");
                for (String url : urls.subList(0, Math.min(urls.size(), 10))) {
                    response.append("  • ").append(url).append("\n");
                }
                if (urls.size() > 10) {
                    response.append("  ... and ").append(urls.size() - 10).append(" more\n");
                }
                response.append("\n");
            }
            
            if (!paths.isEmpty()) {
                response.append("📁 Paths:\n");
                for (String path : paths.subList(0, Math.min(paths.size(), 10))) {
                    response.append("  • ").append(path).append("\n");
                }
                if (paths.size() > 10) {
                    response.append("  ... and ").append(paths.size() - 10).append(" more\n");
                }
                response.append("\n");
            }
            
            if (!other.isEmpty()) {
                response.append("🔧 Other Endpoints:\n");
                for (String endpoint : other.subList(0, Math.min(other.size(), 10))) {
                    response.append("  • ").append(endpoint).append("\n");
                }
                if (other.size() > 10) {
                    response.append("  ... and ").append(other.size() - 10).append(" more\n");
                }
            }
            
            response.append("\n📊 Total: ").append(endpoints.size()).append(" endpoints found");
            
            return response.toString();
            
        } catch (Exception e) {
            logger.warning("Failed to list endpoints for project " + projectName + ": " + e.getMessage());
            return "❌ Failed to list endpoints: " + e.getMessage();
        }
    }
    
    /**
     * Handles list Burp vulnerabilities commands.
     * 
     * @param projectName The project name
     * @param hostFilter Optional host filter
     * @return Response string
     */
    private String handleListBurpVulnerabilities(String projectName, String hostFilter) {
        try {
            String sessionId = findSessionForProject(projectName);
            if (sessionId == null) {
                return "❌ No gathering session found for project: " + projectName;
            }
            
            InformationGatherer.GatheringSession session = informationGatherer.getSession(sessionId);
            List<BurpSuiteParser.BurpFinding> vulnerabilities;
            
            if (hostFilter != null) {
                vulnerabilities = session.getBurpVulnerabilitiesByHost(hostFilter);
            } else {
                vulnerabilities = session.getAllBurpVulnerabilities();
            }
            
            if (vulnerabilities.isEmpty()) {
                String filterText = hostFilter != null ? " for host " + hostFilter : "";
                return "ℹ️  No Burp Suite vulnerabilities found" + filterText + " in project: " + projectName;
            }
            
            StringBuilder response = new StringBuilder();
            response.append("🔍 Burp Suite Vulnerabilities");
            if (hostFilter != null) {
                response.append(" for ").append(hostFilter);
            }
            response.append(" in Project: ").append(projectName).append("\n");
            response.append("══════════════════════════════════════════════════\n\n");
            
            // Group by severity
            Map<Severity, List<BurpSuiteParser.BurpFinding>> bySeverity = vulnerabilities.stream()
                .collect(Collectors.groupingBy(BurpSuiteParser.BurpFinding::getSeverity));
            
            for (Severity severity : Severity.values()) {
                List<BurpSuiteParser.BurpFinding> findings = bySeverity.get(severity);
                if (findings != null && !findings.isEmpty()) {
                    response.append(severity.getIcon()).append(" ").append(severity.getDisplayName())
                           .append(" (").append(findings.size()).append("):\n");
                    
                    for (BurpSuiteParser.BurpFinding finding : findings.subList(0, Math.min(findings.size(), 5))) {
                        response.append("  • ").append(finding.getIssueName());
                        if (finding.getLocation() != null) {
                            response.append(" at ").append(finding.getLocation());
                        }
                        response.append(" [").append(finding.getConfidence()).append("]\n");
                    }
                    
                    if (findings.size() > 5) {
                        response.append("  ... and ").append(findings.size() - 5).append(" more\n");
                    }
                    response.append("\n");
                }
            }
            
            response.append("📊 Total: ").append(vulnerabilities.size()).append(" vulnerabilities found");
            
            return response.toString();
            
        } catch (Exception e) {
            logger.warning("Failed to list Burp vulnerabilities for project " + projectName + ": " + e.getMessage());
            return "❌ Failed to list Burp vulnerabilities: " + e.getMessage();
        }
    }
    
    /**
     * Handles list sessions commands.
     * 
     * @return Response string
     */
    private String handleListSessions() {
        Map<String, InformationGatherer.GatheringSession> sessions = informationGatherer.getAllSessions();
        
        if (sessions.isEmpty()) {
            return "ℹ️  No active gathering sessions.\n" +
                   "Start a session with: 'gather info on <project_name>'";
        }
        
        StringBuilder response = new StringBuilder();
        response.append("📋 Active Gathering Sessions\n");
        response.append("════════════════════════════\n\n");
        
        for (InformationGatherer.GatheringSession session : sessions.values()) {
            response.append("🆔 Session: ").append(session.getSessionId()).append("\n");
            response.append("📁 Project: ").append(session.getProjectName()).append("\n");
            response.append("⏰ Started: ").append(session.getStartTime()).append("\n");
            response.append("📄 Files: ").append(session.getFileResults().size()).append("\n");
            response.append("🔍 Burp Imports: ").append(session.getBurpResults().size()).append("\n");
            response.append("⚠️  Warnings: ").append(session.getWarnings().size()).append("\n\n");
        }
        
        response.append("💡 Use 'show gathering session <session_id>' for details");
        
        return response.toString();
    }
    
    /**
     * Handles show session commands.
     * 
     * @param sessionId The session ID
     * @return Response string
     */
    private String handleShowSession(String sessionId) {
        try {
            InformationGatherer.GatheringSession session = informationGatherer.getSession(sessionId);
            return session.getFormattedSummary();
            
        } catch (Exception e) {
            return "❌ Session not found: " + sessionId + "\n" +
                   "Use 'list gathering sessions' to see active sessions.";
        }
    }
    
    /**
     * Handles close session commands.
     * 
     * @param sessionId The session ID
     * @return Response string
     */
    private String handleCloseSession(String sessionId) {
        try {
            InformationGatherer.GatheringSession session = informationGatherer.closeSession(sessionId);
            
            return "✅ Gathering session closed: " + sessionId + "\n" +
                   "📁 Project: " + session.getProjectName() + "\n" +
                   "📄 Files analyzed: " + session.getFileResults().size() + "\n" +
                   "🔍 Burp imports: " + session.getBurpResults().size();
            
        } catch (Exception e) {
            return "❌ Session not found: " + sessionId;
        }
    }
    
    /**
     * Finds or creates a gathering session for a project.
     * 
     * @param projectName The project name
     * @return The session ID
     */
    private String getOrCreateSessionForProject(String projectName) {
        // Look for existing session
        String existingSession = findSessionForProject(projectName);
        if (existingSession != null) {
            return existingSession;
        }
        
        // Create new session
        return informationGatherer.startGatheringSession(projectName);
    }
    
    /**
     * Finds an existing gathering session for a project.
     * 
     * @param projectName The project name
     * @return The session ID or null if not found
     */
    private String findSessionForProject(String projectName) {
        Map<String, InformationGatherer.GatheringSession> sessions = informationGatherer.getAllSessions();
        
        for (InformationGatherer.GatheringSession session : sessions.values()) {
            if (projectName.equals(session.getProjectName())) {
                return session.getSessionId();
            }
        }
        
        return null;
    }
    
    /**
     * Gets the help text for information gathering commands.
     * 
     * @return Help text string
     */
    private String getHelpText() {
        return "🔍 Information Gathering Commands\n" +
               "════════════════════════════════\n\n" +
               "📊 **Starting Analysis:**\n" +
               "• gather info on \"<project_name>\"\n" +
               "• analyze file \"<file_path>\"\n" +
               "• analyze directory \"<directory_path>\"\n\n" +
               "🔍 **Burp Suite Integration:**\n" +
               "• import burp data from \"<export.xml>\"\n" +
               "• import burp data from \"<export.xml>\" into project \"<project>\"\n\n" +
               "📋 **Querying Results:**\n" +
               "• list passwords in \"<project>\"\n" +
               "• show api keys in \"<project>\"\n" +
               "• show endpoints found in \"<project>\"\n" +
               "• show burp vulnerabilities in \"<project>\"\n" +
               "• show burp vulnerabilities for <host> in \"<project>\"\n\n" +
               "🗂️ **Session Management:**\n" +
               "• list gathering sessions\n" +
               "• show gathering session \"<session_id>\"\n" +
               "• close gathering session \"<session_id>\"\n\n" +
               "❓ **Help:**\n" +
               "• help gathering\n" +
               "• help burp suite integration";
    }
    
    /**
     * Gets the help text specifically for Burp Suite integration.
     * 
     * @return Burp Suite help text string
     */
    private String getBurpHelpText() {
        return "🔍 Burp Suite Integration Help\n" +
               "══════════════════════════════\n\n" +
               "📤 **Exporting from Burp Suite:**\n" +
               "1. In Burp Suite, go to Target > Site map\n" +
               "2. Right-click on the target domain\n" +
               "3. Select \"Save selected items\"\n" +
               "4. Choose XML format and save\n\n" +
               "📥 **Importing into Agent-Orange:**\n" +
               "• import burp data from \"/path/to/export.xml\"\n" +
               "• import burp data from \"export.xml\" into project \"MyPentest\"\n\n" +
               "📊 **What Gets Imported:**\n" +
               "• Vulnerability findings with severity mapping\n" +
               "• Target hosts and endpoints\n" +
               "• Request/response data (truncated for display)\n" +
               "• Issue descriptions and remediation advice\n\n" +
               "🔍 **Viewing Imported Data:**\n" +
               "• show burp vulnerabilities in \"<project>\"\n" +
               "• show burp vulnerabilities for <host> in \"<project>\"\n" +
               "• list endpoints found in \"<project>\"\n\n" +
               "📈 **Severity Mapping:**\n" +
               "• Burp \"High\" → Agent-Orange \"Critical\"\n" +
               "• Burp \"Medium\" → Agent-Orange \"High\"\n" +
               "• Burp \"Low\" → Agent-Orange \"Medium\"\n" +
               "• Burp \"Information\" → Agent-Orange \"Low\"\n\n" +
               "⚠️  **Security Notes:**\n" +
               "• Imported vulnerabilities are added to your project\n" +
               "• Sensitive request/response data is handled securely\n" +
               "• File paths must be within project scope (if configured)";
    }
}