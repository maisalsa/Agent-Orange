package com.example;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Main orchestrator for information gathering operations.
 * 
 * This class coordinates file analysis, Burp Suite integration, and
 * data aggregation to provide comprehensive reconnaissance capabilities
 * for pentesting projects.
 * 
 * @author Agent-Orange Team
 * @version 1.0
 */
public class InformationGatherer {
    private static final Logger logger = Logger.getLogger(InformationGatherer.class.getName());
    
    private final FileAnalyzer fileAnalyzer;
    private final Map<String, GatheringSession> sessions;
    private final Set<String> allowedScopes;
    private final boolean securityMode;
    
    /**
     * Represents a single information gathering session.
     */
    public static class GatheringSession {
        private final String sessionId;
        private final String projectName;
        private final LocalDateTime startTime;
        private final Map<String, ExtractedData> fileResults;
        private final List<BurpSuiteParser.BurpParseResult> burpResults;
        private final Map<String, Object> sessionMetadata;
        private final Set<String> warnings;
        
        public GatheringSession(String sessionId, String projectName) {
            this.sessionId = sessionId;
            this.projectName = projectName;
            this.startTime = LocalDateTime.now();
            this.fileResults = new ConcurrentHashMap<>();
            this.burpResults = new ArrayList<>();
            this.sessionMetadata = new ConcurrentHashMap<>();
            this.warnings = ConcurrentHashMap.newKeySet();
        }
        
        // Getters
        public String getSessionId() { return sessionId; }
        public String getProjectName() { return projectName; }
        public LocalDateTime getStartTime() { return startTime; }
        public Map<String, ExtractedData> getFileResults() { return new HashMap<>(fileResults); }
        public List<BurpSuiteParser.BurpParseResult> getBurpResults() { return new ArrayList<>(burpResults); }
        public Map<String, Object> getSessionMetadata() { return new HashMap<>(sessionMetadata); }
        public Set<String> getWarnings() { return new HashSet<>(warnings); }
        
        // Internal methods for adding results
        void addFileResult(String filePath, ExtractedData data) {
            fileResults.put(filePath, data);
        }
        
        void addBurpResult(BurpSuiteParser.BurpParseResult result) {
            burpResults.add(result);
        }
        
        void addMetadata(String key, Object value) {
            sessionMetadata.put(key, value);
        }
        
        void addWarning(String warning) {
            warnings.add(warning);
        }
        
        /**
         * Gets all extracted credentials across all sources.
         * 
         * @return List of credentials (sanitized)
         */
        public List<String> getAllCredentials() {
            List<String> credentials = new ArrayList<>();
            
            // From file analysis
            for (ExtractedData data : fileResults.values()) {
                credentials.addAll(data.getExtractedInfo(ExtractedData.DataType.CREDENTIAL));
            }
            
            return credentials.stream()
                             .map(this::sanitizeCredential)
                             .distinct()
                             .collect(Collectors.toList());
        }
        
        /**
         * Gets all API keys across all sources.
         * 
         * @return List of API keys (sanitized)
         */
        public List<String> getAllApiKeys() {
            List<String> apiKeys = new ArrayList<>();
            
            // From file analysis
            for (ExtractedData data : fileResults.values()) {
                apiKeys.addAll(data.getExtractedInfo(ExtractedData.DataType.API_KEY));
            }
            
            return apiKeys.stream()
                         .map(this::sanitizeCredential)
                         .distinct()
                         .collect(Collectors.toList());
        }
        
        /**
         * Gets all endpoints across all sources.
         * 
         * @return List of endpoints
         */
        public List<String> getAllEndpoints() {
            Set<String> endpoints = new HashSet<>();
            
            // From file analysis
            for (ExtractedData data : fileResults.values()) {
                endpoints.addAll(data.getExtractedInfo(ExtractedData.DataType.ENDPOINT));
            }
            
            // From Burp Suite results
            for (BurpSuiteParser.BurpParseResult burpResult : burpResults) {
                for (BurpSuiteParser.BurpFinding finding : burpResult.getFindings()) {
                    if (finding.getLocation() != null) {
                        endpoints.add(finding.getLocation());
                    }
                }
            }
            
            return new ArrayList<>(endpoints);
        }
        
        /**
         * Gets all vulnerabilities from Burp Suite.
         * 
         * @return List of Burp findings
         */
        public List<BurpSuiteParser.BurpFinding> getAllBurpVulnerabilities() {
            List<BurpSuiteParser.BurpFinding> vulns = new ArrayList<>();
            for (BurpSuiteParser.BurpParseResult result : burpResults) {
                vulns.addAll(result.getFindings());
            }
            return vulns;
        }
        
        /**
         * Gets vulnerabilities by host from Burp Suite.
         * 
         * @param host The host to filter by
         * @return List of Burp findings for the specified host
         */
        public List<BurpSuiteParser.BurpFinding> getBurpVulnerabilitiesByHost(String host) {
            return getAllBurpVulnerabilities().stream()
                                            .filter(f -> host.equals(f.getHost()))
                                            .collect(Collectors.toList());
        }
        
        /**
         * Gets a summary of the gathering session.
         * 
         * @return Formatted summary string
         */
        public String getFormattedSummary() {
            StringBuilder summary = new StringBuilder();
            
            summary.append("📊 Information Gathering Summary\n");
            summary.append("═══════════════════════════════════\n");
            summary.append("🆔 Session: ").append(sessionId).append("\n");
            summary.append("📁 Project: ").append(projectName).append("\n");
            summary.append("⏰ Started: ").append(startTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
            summary.append("📄 Files Analyzed: ").append(fileResults.size()).append("\n");
            summary.append("🔍 Burp Imports: ").append(burpResults.size()).append("\n\n");
            
            // File analysis summary
            if (!fileResults.isEmpty()) {
                summary.append("📂 File Analysis Results:\n");
                int totalItems = fileResults.values().stream()
                                           .mapToInt(ExtractedData::getTotalItemCount)
                                           .sum();
                summary.append("  Total items extracted: ").append(totalItems).append("\n");
                
                // Count by data type
                Map<ExtractedData.DataType, Integer> typeCounts = new HashMap<>();
                for (ExtractedData.DataType type : ExtractedData.DataType.values()) {
                    typeCounts.put(type, 0);
                }
                
                for (ExtractedData data : fileResults.values()) {
                    for (ExtractedData.DataType type : ExtractedData.DataType.values()) {
                        typeCounts.merge(type, data.getItemCount(type), Integer::sum);
                    }
                }
                
                for (Map.Entry<ExtractedData.DataType, Integer> entry : typeCounts.entrySet()) {
                    int count = entry.getValue();
                    if (count > 0) {
                        summary.append("  ").append(entry.getKey().getFormattedName())
                               .append(": ").append(count).append("\n");
                    }
                }
                summary.append("\n");
            }
            
            // Burp Suite summary
            if (!burpResults.isEmpty()) {
                summary.append("🔍 Burp Suite Results:\n");
                int totalFindings = burpResults.stream()
                                              .mapToInt(r -> r.getFindings().size())
                                              .sum();
                summary.append("  Total findings: ").append(totalFindings).append("\n");
                
                // Count by severity
                Map<Severity, Integer> severityCounts = new HashMap<>();
                for (Severity sev : Severity.values()) {
                    severityCounts.put(sev, 0);
                }
                
                for (BurpSuiteParser.BurpParseResult result : burpResults) {
                    Map<Severity, Integer> resultCounts = result.getSeveritySummary();
                    for (Map.Entry<Severity, Integer> entry : resultCounts.entrySet()) {
                        severityCounts.merge(entry.getKey(), entry.getValue(), Integer::sum);
                    }
                }
                
                for (Map.Entry<Severity, Integer> entry : severityCounts.entrySet()) {
                    int count = entry.getValue();
                    if (count > 0) {
                        summary.append("  ").append(entry.getKey().getIcon()).append(" ")
                               .append(entry.getKey().getDisplayName()).append(": ").append(count).append("\n");
                    }
                }
                summary.append("\n");
            }
            
            // Warnings
            if (!warnings.isEmpty()) {
                summary.append("⚠️  Warnings:\n");
                for (String warning : warnings) {
                    summary.append("  ⚠️  ").append(warning).append("\n");
                }
                summary.append("\n");
            }
            
            // Session metadata
            if (!sessionMetadata.isEmpty()) {
                summary.append("ℹ️  Session Metadata:\n");
                for (Map.Entry<String, Object> entry : sessionMetadata.entrySet()) {
                    summary.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                }
            }
            
            return summary.toString();
        }
        
        /**
         * Sanitizes sensitive credentials for display.
         * 
         * @param credential The credential to sanitize
         * @return Sanitized credential
         */
        private String sanitizeCredential(String credential) {
            if (credential == null || credential.length() <= 4) {
                return "[REDACTED]";
            }
            
            int len = credential.length();
            if (len <= 8) {
                return credential.substring(0, 2) + "*".repeat(len - 4) + credential.substring(len - 2);
            } else {
                return credential.substring(0, 2) + "*".repeat(6) + credential.substring(len - 2);
            }
        }
        
        @Override
        public String toString() {
            return String.format("GatheringSession{id='%s', project='%s', files=%d, burp=%d}", 
                               sessionId, projectName, fileResults.size(), burpResults.size());
        }
    }
    
    /**
     * Constructs a new InformationGatherer with default settings.
     */
    public InformationGatherer() {
        this(new FileAnalyzer(), Collections.emptySet(), true);
    }
    
    /**
     * Constructs a new InformationGatherer with custom settings.
     * 
     * @param fileAnalyzer The file analyzer to use
     * @param allowedScopes Set of allowed scope patterns
     * @param securityMode Whether to enforce security restrictions
     */
    public InformationGatherer(FileAnalyzer fileAnalyzer, Set<String> allowedScopes, boolean securityMode) {
        this.fileAnalyzer = fileAnalyzer;
        this.sessions = new ConcurrentHashMap<>();
        this.allowedScopes = new HashSet<>(allowedScopes);
        this.securityMode = securityMode;
        
        logger.info("InformationGatherer initialized - Security mode: " + securityMode);
    }
    
    /**
     * Starts a new information gathering session.
     * 
     * @param projectName The name of the project
     * @return The session ID
     */
    public String startGatheringSession(String projectName) {
        if (projectName == null || projectName.trim().isEmpty()) {
            throw new IllegalArgumentException("Project name cannot be null or empty");
        }
        
        String sessionId = generateSessionId();
        GatheringSession session = new GatheringSession(sessionId, projectName.trim());
        sessions.put(sessionId, session);
        
        logger.info("Started gathering session: " + sessionId + " for project: " + projectName);
        return sessionId;
    }
    
    /**
     * Analyzes a single file within a session.
     * 
     * @param sessionId The session ID
     * @param filePath Path to the file to analyze
     * @return ExtractedData containing the analysis results
     * @throws IOException if file analysis fails
     * @throws IllegalArgumentException if session not found
     */
    public ExtractedData analyzeFile(String sessionId, String filePath) throws IOException {
        GatheringSession session = getSession(sessionId);
        
        validateScope(filePath);
        
        try {
            ExtractedData data = fileAnalyzer.analyzeFile(filePath);
            session.addFileResult(filePath, data);
            
            // Add session metadata
            session.addMetadata("last_file_analyzed", filePath);
            session.addMetadata("last_analysis_time", LocalDateTime.now().toString());
            
            // Check for security warnings
            if (data.hasSensitiveData()) {
                session.addWarning("Sensitive data found in: " + filePath);
            }
            
            logger.info("Analyzed file in session " + sessionId + ": " + filePath + 
                       " (" + data.getTotalItemCount() + " items)");
            
            return data;
            
        } catch (Exception e) {
            session.addWarning("Failed to analyze file: " + filePath + " - " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Analyzes multiple files within a session.
     * 
     * @param sessionId The session ID
     * @param filePaths List of file paths to analyze
     * @return Map of file paths to analysis results
     * @throws IllegalArgumentException if session not found
     */
    public Map<String, ExtractedData> analyzeFiles(String sessionId, List<String> filePaths) {
        GatheringSession session = getSession(sessionId);
        
        if (filePaths == null || filePaths.isEmpty()) {
            return new HashMap<>();
        }
        
        // Validate scope for all files
        for (String filePath : filePaths) {
            try {
                validateScope(filePath);
            } catch (SecurityException e) {
                session.addWarning("Skipping file outside scope: " + filePath);
            }
        }
        
        Map<String, ExtractedData> results = fileAnalyzer.analyzeFiles(filePaths);
        
        // Add results to session
        for (Map.Entry<String, ExtractedData> entry : results.entrySet()) {
            session.addFileResult(entry.getKey(), entry.getValue());
            
            if (entry.getValue().hasSensitiveData()) {
                session.addWarning("Sensitive data found in: " + entry.getKey());
            }
        }
        
        session.addMetadata("bulk_analysis_count", results.size());
        session.addMetadata("last_bulk_analysis_time", LocalDateTime.now().toString());
        
        logger.info("Analyzed " + results.size() + " files in session " + sessionId);
        
        return results;
    }
    
    /**
     * Analyzes a directory within a session.
     * 
     * @param sessionId The session ID
     * @param directoryPath Path to the directory to analyze
     * @param recursive Whether to search recursively
     * @param maxFiles Maximum number of files to analyze
     * @return Map of file paths to analysis results
     * @throws IOException if directory analysis fails
     * @throws IllegalArgumentException if session not found
     */
    public Map<String, ExtractedData> analyzeDirectory(String sessionId, String directoryPath, 
                                                      boolean recursive, int maxFiles) throws IOException {
        GatheringSession session = getSession(sessionId);
        
        validateScope(directoryPath);
        
        try {
            Map<String, ExtractedData> results = fileAnalyzer.analyzeDirectory(directoryPath, recursive, maxFiles);
            
            // Add results to session
            for (Map.Entry<String, ExtractedData> entry : results.entrySet()) {
                session.addFileResult(entry.getKey(), entry.getValue());
                
                if (entry.getValue().hasSensitiveData()) {
                    session.addWarning("Sensitive data found in: " + entry.getKey());
                }
            }
            
            session.addMetadata("directory_analyzed", directoryPath);
            session.addMetadata("directory_recursive", recursive);
            session.addMetadata("directory_file_count", results.size());
            session.addMetadata("last_directory_analysis_time", LocalDateTime.now().toString());
            
            logger.info("Analyzed directory in session " + sessionId + ": " + directoryPath + 
                       " (" + results.size() + " files)");
            
            return results;
            
        } catch (Exception e) {
            session.addWarning("Failed to analyze directory: " + directoryPath + " - " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Imports Burp Suite XML export into a session.
     * 
     * @param sessionId The session ID
     * @param burpXmlPath Path to the Burp Suite XML export file
     * @return BurpParseResult containing the imported data
     * @throws IOException if import fails
     * @throws IllegalArgumentException if session not found or invalid XML
     */
    public BurpSuiteParser.BurpParseResult importBurpSuite(String sessionId, String burpXmlPath) throws IOException {
        GatheringSession session = getSession(sessionId);
        
        validateScope(burpXmlPath);
        
        // Validate that it's a Burp export file
        if (!BurpSuiteParser.isBurpExportFile(burpXmlPath)) {
            throw new IllegalArgumentException("File does not appear to be a valid Burp Suite export: " + burpXmlPath);
        }
        
        try {
            BurpSuiteParser.BurpParseResult result = BurpSuiteParser.parseBurpExport(burpXmlPath);
            session.addBurpResult(result);
            
            // Add session metadata
            session.addMetadata("burp_import_file", burpXmlPath);
            session.addMetadata("burp_import_time", LocalDateTime.now().toString());
            session.addMetadata("burp_findings_count", result.getFindings().size());
            session.addMetadata("burp_targets_count", result.getTargets().size());
            
            // Check for high-severity findings
            int criticalCount = result.getFindingsBySeverity(Severity.CRITICAL).size();
            int highCount = result.getFindingsBySeverity(Severity.HIGH).size();
            
            if (criticalCount > 0) {
                session.addWarning("Found " + criticalCount + " critical vulnerabilities in Burp import");
            }
            if (highCount > 0) {
                session.addWarning("Found " + highCount + " high-severity vulnerabilities in Burp import");
            }
            
            logger.info("Imported Burp Suite data in session " + sessionId + ": " + burpXmlPath + 
                       " (" + result.getFindings().size() + " findings)");
            
            return result;
            
        } catch (Exception e) {
            session.addWarning("Failed to import Burp Suite data: " + burpXmlPath + " - " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Gets a gathering session by ID.
     * 
     * @param sessionId The session ID
     * @return The gathering session
     * @throws IllegalArgumentException if session not found
     */
    public GatheringSession getSession(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Session ID cannot be null or empty");
        }
        
        GatheringSession session = sessions.get(sessionId.trim());
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }
        
        return session;
    }
    
    /**
     * Gets all active sessions.
     * 
     * @return Map of session IDs to sessions
     */
    public Map<String, GatheringSession> getAllSessions() {
        return new HashMap<>(sessions);
    }
    
    /**
     * Closes a gathering session.
     * 
     * @param sessionId The session ID to close
     * @return The closed session
     * @throws IllegalArgumentException if session not found
     */
    public GatheringSession closeSession(String sessionId) {
        GatheringSession session = getSession(sessionId);
        sessions.remove(sessionId);
        
        logger.info("Closed gathering session: " + sessionId);
        return session;
    }
    
    /**
     * Clears all sessions.
     */
    public void clearAllSessions() {
        int count = sessions.size();
        sessions.clear();
        logger.info("Cleared " + count + " gathering sessions");
    }
    
    /**
     * Validates if a path is within the allowed scope.
     * 
     * @param path The path to validate
     * @throws SecurityException if path is not allowed
     */
    private void validateScope(String path) throws SecurityException {
        if (!securityMode) {
            return; // Skip validation in non-security mode
        }
        
        if (path == null || path.trim().isEmpty()) {
            throw new SecurityException("Path cannot be null or empty");
        }
        
        String normalizedPath = path.trim();
        
        // If no allowed scopes are defined, allow all paths
        if (allowedScopes.isEmpty()) {
            return;
        }
        
        // Check if path is within any allowed scope
        for (String allowedScope : allowedScopes) {
            if (normalizedPath.startsWith(allowedScope)) {
                return;
            }
        }
        
        throw new SecurityException("Path not within allowed scope: " + normalizedPath);
    }
    
    /**
     * Generates a unique session ID.
     * 
     * @return A unique session ID
     */
    private String generateSessionId() {
        return "gather_" + System.currentTimeMillis() + "_" + 
               Integer.toHexString(new Random().nextInt(0xFFFF));
    }
    
    /**
     * Gets the file analyzer instance.
     * 
     * @return The file analyzer
     */
    public FileAnalyzer getFileAnalyzer() {
        return fileAnalyzer;
    }
    
    /**
     * Gets the number of active sessions.
     * 
     * @return Number of active sessions
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }
    
    /**
     * Checks if security mode is enabled.
     * 
     * @return true if security mode is enabled
     */
    public boolean isSecurityMode() {
        return securityMode;
    }
    
    /**
     * Gets the allowed scopes.
     * 
     * @return Set of allowed scope patterns
     */
    public Set<String> getAllowedScopes() {
        return new HashSet<>(allowedScopes);
    }
    
    /**
     * Shuts down the information gatherer and releases resources.
     */
    public void shutdown() {
        clearAllSessions();
        fileAnalyzer.shutdown();
        logger.info("InformationGatherer shutdown complete");
    }
    
    @Override
    public String toString() {
        return String.format("InformationGatherer{sessions=%d, securityMode=%s, scopes=%d}", 
                           sessions.size(), securityMode, allowedScopes.size());
    }
}