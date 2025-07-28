package com.example;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Main file analysis engine for extracting information from target files.
 * 
 * This class coordinates different parsers and extractors to gather
 * intelligence from various file types while maintaining security
 * boundaries and providing comprehensive logging.
 * 
 * @author Agent-Orange Team
 * @version 1.0
 */
public class FileAnalyzer {
    private static final Logger logger = Logger.getLogger(FileAnalyzer.class.getName());
    
    // Security patterns for detecting sensitive information
    private static final Map<ExtractedData.DataType, List<Pattern>> SECURITY_PATTERNS = new HashMap<>();
    
    static {
        initializeSecurityPatterns();
    }
    
    private final ExecutorService executorService;
    private final Set<String> allowedPaths;
    private final Set<String> blockedPaths;
    private final long maxFileSize;
    private final boolean strictScopeMode;
    private final Map<String, ExtractedData> analysisCache;
    
    /**
     * Constructs a new FileAnalyzer with default settings.
     */
    public FileAnalyzer() {
        this(Collections.emptySet(), Collections.emptySet(), 10 * 1024 * 1024, false); // 10MB default
    }
    
    /**
     * Constructs a new FileAnalyzer with custom settings.
     * 
     * @param allowedPaths Set of allowed file paths or patterns
     * @param blockedPaths Set of blocked file paths or patterns
     * @param maxFileSize Maximum file size to analyze (in bytes)
     * @param strictScopeMode Whether to enforce strict scope checking
     */
    public FileAnalyzer(Set<String> allowedPaths, Set<String> blockedPaths, 
                       long maxFileSize, boolean strictScopeMode) {
        this.allowedPaths = new HashSet<>(allowedPaths);
        this.blockedPaths = new HashSet<>(blockedPaths);
        this.maxFileSize = maxFileSize;
        this.strictScopeMode = strictScopeMode;
        this.executorService = Executors.newFixedThreadPool(4); // Resource-conscious for ThinkStation
        this.analysisCache = new ConcurrentHashMap<>();
        
        logger.info("FileAnalyzer initialized - Max file size: " + maxFileSize + 
                   ", Strict mode: " + strictScopeMode);
    }
    
    /**
     * Initializes security patterns for detecting sensitive information.
     */
    private static void initializeSecurityPatterns() {
        // Password patterns
        List<Pattern> passwordPatterns = Arrays.asList(
            Pattern.compile("(?i)password\\s*[=:]\\s*['\"]?([^'\"\\s]+)['\"]?"),
            Pattern.compile("(?i)passwd\\s*[=:]\\s*['\"]?([^'\"\\s]+)['\"]?"),
            Pattern.compile("(?i)pwd\\s*[=:]\\s*['\"]?([^'\"\\s]+)['\"]?"),
            Pattern.compile("(?i)secret\\s*[=:]\\s*['\"]?([^'\"\\s]+)['\"]?")
        );
        SECURITY_PATTERNS.put(ExtractedData.DataType.CREDENTIAL, passwordPatterns);
        
        // API Key patterns
        List<Pattern> apiKeyPatterns = Arrays.asList(
            Pattern.compile("(?i)api[_-]?key\\s*[=:]\\s*['\"]?([^'\"\\s]+)['\"]?"),
            Pattern.compile("(?i)apikey\\s*[=:]\\s*['\"]?([^'\"\\s]+)['\"]?"),
            Pattern.compile("(?i)access[_-]?token\\s*[=:]\\s*['\"]?([^'\"\\s]+)['\"]?"),
            Pattern.compile("(?i)bearer\\s+([A-Za-z0-9\\-_\\.]+)"),
            Pattern.compile("(?i)authorization\\s*[=:]\\s*['\"]?bearer\\s+([^'\"\\s]+)['\"]?")
        );
        SECURITY_PATTERNS.put(ExtractedData.DataType.API_KEY, apiKeyPatterns);
        
        // Database connection patterns
        List<Pattern> dbPatterns = Arrays.asList(
            Pattern.compile("(?i)jdbc:([^\\s'\"]+)"),
            Pattern.compile("(?i)mongodb://([^\\s'\"]+)"),
            Pattern.compile("(?i)mysql://([^\\s'\"]+)"),
            Pattern.compile("(?i)postgresql://([^\\s'\"]+)"),
            Pattern.compile("(?i)redis://([^\\s'\"]+)"),
            Pattern.compile("(?i)database\\s*[=:]\\s*['\"]?([^'\"\\s]+)['\"]?"),
            Pattern.compile("(?i)db[_-]?host\\s*[=:]\\s*['\"]?([^'\"\\s]+)['\"]?")
        );
        SECURITY_PATTERNS.put(ExtractedData.DataType.DATABASE_CONNECTION, dbPatterns);
        
        // Endpoint patterns
        List<Pattern> endpointPatterns = Arrays.asList(
            Pattern.compile("https?://([^\\s'\"<>]+)"),
            Pattern.compile("(?i)endpoint\\s*[=:]\\s*['\"]?([^'\"\\s]+)['\"]?"),
            Pattern.compile("(?i)url\\s*[=:]\\s*['\"]?([^'\"\\s]+)['\"]?"),
            Pattern.compile("(?i)base[_-]?url\\s*[=:]\\s*['\"]?([^'\"\\s]+)['\"]?")
        );
        SECURITY_PATTERNS.put(ExtractedData.DataType.ENDPOINT, endpointPatterns);
        
        // Version info patterns
        List<Pattern> versionPatterns = Arrays.asList(
            Pattern.compile("(?i)version\\s*[=:]\\s*['\"]?([^'\"\\s]+)['\"]?"),
            Pattern.compile("(?i)ver\\s*[=:]\\s*['\"]?([^'\"\\s]+)['\"]?"),
            Pattern.compile("(?i)<version>([^<]+)</version>"),
            Pattern.compile("(?i)Server:\\s*([^\\r\\n]+)")
        );
        SECURITY_PATTERNS.put(ExtractedData.DataType.VERSION_INFO, versionPatterns);
        
        // User info patterns
        List<Pattern> userPatterns = Arrays.asList(
            Pattern.compile("(?i)username\\s*[=:]\\s*['\"]?([^'\"\\s]+)['\"]?"),
            Pattern.compile("(?i)user\\s*[=:]\\s*['\"]?([^'\"\\s]+)['\"]?"),
            Pattern.compile("(?i)login\\s*[=:]\\s*['\"]?([^'\"\\s]+)['\"]?"),
            Pattern.compile("(?i)email\\s*[=:]\\s*['\"]?([\\w\\._%+-]+@[\\w\\.-]+\\.[A-Za-z]{2,})['\"]?")
        );
        SECURITY_PATTERNS.put(ExtractedData.DataType.USER_INFO, userPatterns);
        
        // Network info patterns
        List<Pattern> networkPatterns = Arrays.asList(
            Pattern.compile("(?i)host\\s*[=:]\\s*['\"]?([^'\"\\s]+)['\"]?"),
            Pattern.compile("(?i)hostname\\s*[=:]\\s*['\"]?([^'\"\\s]+)['\"]?"),
            Pattern.compile("(?i)port\\s*[=:]\\s*['\"]?(\\d+)['\"]?"),
            Pattern.compile("(?i)ip\\s*[=:]\\s*['\"]?(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})['\"]?")
        );
        SECURITY_PATTERNS.put(ExtractedData.DataType.NETWORK_INFO, networkPatterns);
        
        // Secret patterns (generic)
        List<Pattern> secretPatterns = Arrays.asList(
            Pattern.compile("(?i)secret\\s*[=:]\\s*['\"]?([^'\"\\s]+)['\"]?"),
            Pattern.compile("(?i)private[_-]?key\\s*[=:]\\s*['\"]?([^'\"\\s]+)['\"]?"),
            Pattern.compile("(?i)encryption[_-]?key\\s*[=:]\\s*['\"]?([^'\"\\s]+)['\"]?"),
            Pattern.compile("(?i)token\\s*[=:]\\s*['\"]?([^'\"\\s]+)['\"]?")
        );
        SECURITY_PATTERNS.put(ExtractedData.DataType.SECRET, secretPatterns);
    }
    
    /**
     * Analyzes a single file and extracts information.
     * 
     * @param filePath Path to the file to analyze
     * @return ExtractedData containing the analysis results
     * @throws IOException if file access fails
     * @throws SecurityException if file access is not allowed
     */
    public ExtractedData analyzeFile(String filePath) throws IOException, SecurityException {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }
        
        String normalizedPath = Paths.get(filePath).toAbsolutePath().normalize().toString();
        
        // Check cache first
        if (analysisCache.containsKey(normalizedPath)) {
            logger.fine("Returning cached analysis for: " + normalizedPath);
            return analysisCache.get(normalizedPath);
        }
        
        // Security checks
        validateFileAccess(normalizedPath);
        
        Path path = Paths.get(normalizedPath);
        if (!Files.exists(path)) {
            throw new IOException("File not found: " + normalizedPath);
        }
        
        if (!Files.isReadable(path)) {
            throw new IOException("File not readable: " + normalizedPath);
        }
        
        long fileSize = Files.size(path);
        if (fileSize > maxFileSize) {
            throw new IOException("File too large: " + fileSize + " bytes (max: " + maxFileSize + ")");
        }
        
        if (Files.isDirectory(path)) {
            throw new IllegalArgumentException("Path is a directory, not a file: " + normalizedPath);
        }
        
        logger.info("Analyzing file: " + normalizedPath + " (" + fileSize + " bytes)");
        
        // Determine file type
        FileType fileType = FileType.fromFilename(normalizedPath);
        
        // Create extracted data container
        ExtractedData extractedData = new ExtractedData(normalizedPath, fileType, "FileAnalyzer");
        extractedData.addMetadata("file_size", String.valueOf(fileSize));
        extractedData.addMetadata("last_modified", Files.getLastModifiedTime(path).toString());
        
        try {
            // Read file content
            String content = readFileContent(path);
            extractedData.addMetadata("content_length", String.valueOf(content.length()));
            
            // Extract information based on file type
            if (fileType.isHighSensitivity()) {
                extractedData.addWarning("High sensitivity file type detected: " + fileType.getDisplayName());
            }
            
            // Apply security patterns
            extractSecurityInformation(content, extractedData);
            
            // Apply file-type specific extraction
            switch (fileType) {
                case CONFIGURATION:
                    extractConfigurationData(content, extractedData);
                    break;
                case LOG:
                    extractLogData(content, extractedData);
                    break;
                case SOURCE_CODE:
                    extractSourceCodeData(content, extractedData);
                    break;
                case WEB_APPLICATION:
                    extractWebApplicationData(content, extractedData);
                    break;
                case DATABASE:
                    extractDatabaseData(content, extractedData);
                    break;
                case DEPLOYMENT:
                    extractDeploymentData(content, extractedData);
                    break;
                default:
                    // Generic extraction for other types
                    break;
            }
            
            // Cache the result
            analysisCache.put(normalizedPath, extractedData);
            
            logger.info("Analysis complete for " + normalizedPath + ": " + 
                       extractedData.getTotalItemCount() + " items extracted");
            
            return extractedData;
            
        } catch (Exception e) {
            logger.warning("Failed to analyze file " + normalizedPath + ": " + e.getMessage());
            extractedData.addWarning("Analysis failed: " + e.getMessage());
            throw new IOException("Analysis failed for " + normalizedPath, e);
        }
    }
    
    /**
     * Analyzes multiple files concurrently.
     * 
     * @param filePaths List of file paths to analyze
     * @return Map of file paths to extraction results
     */
    public Map<String, ExtractedData> analyzeFiles(List<String> filePaths) {
        if (filePaths == null || filePaths.isEmpty()) {
            return new HashMap<>();
        }
        
        Map<String, ExtractedData> results = new ConcurrentHashMap<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        for (String filePath : filePaths) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    ExtractedData data = analyzeFile(filePath);
                    results.put(filePath, data);
                } catch (Exception e) {
                    logger.warning("Failed to analyze " + filePath + ": " + e.getMessage());
                    // Create error result
                    try {
                        FileType fileType = FileType.fromFilename(filePath);
                        ExtractedData errorData = new ExtractedData(filePath, fileType, "FileAnalyzer");
                        errorData.addWarning("Analysis failed: " + e.getMessage());
                        results.put(filePath, errorData);
                    } catch (Exception ex) {
                        logger.severe("Failed to create error result for " + filePath);
                    }
                }
            }, executorService);
            
            futures.add(future);
        }
        
        // Wait for all analyses to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).join();
        
        return results;
    }
    
    /**
     * Discovers and analyzes files in a directory.
     * 
     * @param directoryPath Path to the directory to analyze
     * @param recursive Whether to search recursively
     * @param maxFiles Maximum number of files to analyze
     * @return Map of file paths to extraction results
     * @throws IOException if directory access fails
     */
    public Map<String, ExtractedData> analyzeDirectory(String directoryPath, boolean recursive, int maxFiles) 
            throws IOException {
        
        Path dirPath = Paths.get(directoryPath);
        if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
            throw new IOException("Directory not found or not a directory: " + directoryPath);
        }
        
        validateFileAccess(dirPath.toAbsolutePath().normalize().toString());
        
        List<String> filesToAnalyze = new ArrayList<>();
        
        try (Stream<Path> paths = recursive ? Files.walk(dirPath) : Files.list(dirPath)) {
            paths.filter(Files::isRegularFile)
                 .filter(this::isFileInScope)
                 .limit(maxFiles)
                 .forEach(path -> filesToAnalyze.add(path.toString()));
        }
        
        logger.info("Discovered " + filesToAnalyze.size() + " files in " + directoryPath);
        
        return analyzeFiles(filesToAnalyze);
    }
    
    /**
     * Validates if file access is allowed based on security settings.
     * 
     * @param filePath The file path to validate
     * @throws SecurityException if access is not allowed
     */
    private void validateFileAccess(String filePath) throws SecurityException {
        // Check blocked paths first
        for (String blockedPath : blockedPaths) {
            if (filePath.contains(blockedPath)) {
                throw new SecurityException("Access to blocked path: " + filePath);
            }
        }
        
        // In strict mode, only allow explicitly allowed paths
        if (strictScopeMode && !allowedPaths.isEmpty()) {
            boolean allowed = false;
            for (String allowedPath : allowedPaths) {
                if (filePath.startsWith(allowedPath)) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed) {
                throw new SecurityException("Path not in allowed scope: " + filePath);
            }
        }
        
        // Additional security checks
        if (filePath.contains("..")) {
            throw new SecurityException("Path traversal attempt detected: " + filePath);
        }
        
        // Check for system-sensitive paths
        String[] systemPaths = {"/etc/shadow", "/etc/passwd", "/proc/", "/sys/", 
                               "C:\\Windows\\System32", "C:\\Users\\", "/home/"};
        for (String systemPath : systemPaths) {
            if (filePath.startsWith(systemPath)) {
                throw new SecurityException("Access to system path denied: " + filePath);
            }
        }
    }
    
    /**
     * Checks if a file is within the analysis scope.
     * 
     * @param path The file path to check
     * @return true if the file should be analyzed
     */
    private boolean isFileInScope(Path path) {
        try {
            String pathStr = path.toAbsolutePath().normalize().toString();
            validateFileAccess(pathStr);
            
            // Skip very large files
            if (Files.size(path) > maxFileSize) {
                return false;
            }
            
            // Skip binary files that are unlikely to contain useful text
            String filename = path.getFileName().toString().toLowerCase();
            String[] skipExtensions = {".exe", ".bin", ".dll", ".so", ".dylib", ".class", 
                                     ".jar", ".war", ".ear", ".zip", ".tar", ".gz", 
                                     ".jpg", ".jpeg", ".png", ".gif", ".pdf", ".mp4", ".avi"};
            for (String ext : skipExtensions) {
                if (filename.endsWith(ext)) {
                    return false;
                }
            }
            
            return true;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Reads file content safely with encoding detection.
     * 
     * @param path The file path to read
     * @return File content as string
     * @throws IOException if reading fails
     */
    private String readFileContent(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        
        // Try UTF-8 first, fall back to system default
        try {
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return new String(bytes, StandardCharsets.ISO_8859_1);
        }
    }
    
    /**
     * Extracts security-related information using pattern matching.
     * 
     * @param content File content to analyze
     * @param extractedData Container for extracted data
     */
    private void extractSecurityInformation(String content, ExtractedData extractedData) {
        for (Map.Entry<ExtractedData.DataType, List<Pattern>> entry : SECURITY_PATTERNS.entrySet()) {
            ExtractedData.DataType dataType = entry.getKey();
            List<Pattern> patterns = entry.getValue();
            
            for (Pattern pattern : patterns) {
                Matcher matcher = pattern.matcher(content);
                while (matcher.find()) {
                    String value = matcher.group(1);
                    if (value != null && !value.trim().isEmpty()) {
                        extractedData.addExtractedInfo(dataType, value.trim());
                    }
                }
            }
        }
    }
    
    /**
     * Extracts configuration-specific data.
     * 
     * @param content File content to analyze
     * @param extractedData Container for extracted data
     */
    private void extractConfigurationData(String content, ExtractedData extractedData) {
        // Extract key-value pairs
        Pattern kvPattern = Pattern.compile("^\\s*([^#\\s=]+)\\s*[=:]\\s*(.+)$", Pattern.MULTILINE);
        Matcher matcher = kvPattern.matcher(content);
        
        while (matcher.find()) {
            String key = matcher.group(1).trim();
            String value = matcher.group(2).trim();
            
            if (!key.isEmpty() && !value.isEmpty()) {
                // Remove quotes if present
                if ((value.startsWith("\"") && value.endsWith("\"")) ||
                    (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                
                extractedData.addExtractedInfo(ExtractedData.DataType.CONFIGURATION, 
                                             key + " = " + value);
            }
        }
    }
    
    /**
     * Extracts log-specific data.
     * 
     * @param content File content to analyze
     * @param extractedData Container for extracted data
     */
    private void extractLogData(String content, ExtractedData extractedData) {
        // Extract IP addresses
        Pattern ipPattern = Pattern.compile("\\b(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})\\b");
        Matcher ipMatcher = ipPattern.matcher(content);
        while (ipMatcher.find()) {
            extractedData.addExtractedInfo(ExtractedData.DataType.NETWORK_INFO, ipMatcher.group(1));
        }
        
        // Extract URLs
        Pattern urlPattern = Pattern.compile("https?://[^\\s<>\"']+");
        Matcher urlMatcher = urlPattern.matcher(content);
        while (urlMatcher.find()) {
            extractedData.addExtractedInfo(ExtractedData.DataType.ENDPOINT, urlMatcher.group());
        }
        
        // Extract error patterns that might indicate vulnerabilities
        Pattern errorPattern = Pattern.compile("(?i)(error|exception|fail|denied|unauthorized|forbidden|sql.*error|stack.*trace).*", Pattern.MULTILINE);
        Matcher errorMatcher = errorPattern.matcher(content);
        while (errorMatcher.find()) {
            extractedData.addExtractedInfo(ExtractedData.DataType.VULNERABILITY, 
                                         "Error pattern: " + errorMatcher.group().substring(0, Math.min(100, errorMatcher.group().length())));
        }
    }
    
    /**
     * Extracts source code specific data.
     * 
     * @param content File content to analyze
     * @param extractedData Container for extracted data
     */
    private void extractSourceCodeData(String content, ExtractedData extractedData) {
        // Extract hardcoded strings that might be sensitive
        Pattern stringPattern = Pattern.compile("['\"]([^'\"]{6,})['\"]");
        Matcher stringMatcher = stringPattern.matcher(content);
        
        while (stringMatcher.find()) {
            String value = stringMatcher.group(1);
            if (looksLikeSensitiveData(value)) {
                extractedData.addExtractedInfo(ExtractedData.DataType.SECRET, value);
            }
        }
        
        // Extract function/method names that might indicate endpoints
        Pattern functionPattern = Pattern.compile("(?i)(function|def|public|private)\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\(");
        Matcher functionMatcher = functionPattern.matcher(content);
        while (functionMatcher.find()) {
            String functionName = functionMatcher.group(2);
            if (functionName.toLowerCase().contains("api") || 
                functionName.toLowerCase().contains("endpoint") ||
                functionName.toLowerCase().contains("route")) {
                extractedData.addExtractedInfo(ExtractedData.DataType.ENDPOINT, 
                                             "Function: " + functionName);
            }
        }
    }
    
    /**
     * Extracts web application specific data.
     * 
     * @param content File content to analyze
     * @param extractedData Container for extracted data
     */
    private void extractWebApplicationData(String content, ExtractedData extractedData) {
        // Extract form actions
        Pattern formPattern = Pattern.compile("(?i)<form[^>]*action\\s*=\\s*['\"]([^'\"]+)['\"]");
        Matcher formMatcher = formPattern.matcher(content);
        while (formMatcher.find()) {
            extractedData.addExtractedInfo(ExtractedData.DataType.ENDPOINT, 
                                         "Form action: " + formMatcher.group(1));
        }
        
        // Extract script sources
        Pattern scriptPattern = Pattern.compile("(?i)<script[^>]*src\\s*=\\s*['\"]([^'\"]+)['\"]");
        Matcher scriptMatcher = scriptPattern.matcher(content);
        while (scriptMatcher.find()) {
            extractedData.addExtractedInfo(ExtractedData.DataType.FILE_PATH, 
                                         "Script: " + scriptMatcher.group(1));
        }
        
        // Extract AJAX endpoints
        Pattern ajaxPattern = Pattern.compile("(?i)(ajax|fetch|xmlhttprequest)[^'\"]*['\"]([^'\"]+)['\"]");
        Matcher ajaxMatcher = ajaxPattern.matcher(content);
        while (ajaxMatcher.find()) {
            extractedData.addExtractedInfo(ExtractedData.DataType.ENDPOINT, 
                                         "AJAX endpoint: " + ajaxMatcher.group(2));
        }
    }
    
    /**
     * Extracts database-specific data.
     * 
     * @param content File content to analyze
     * @param extractedData Container for extracted data
     */
    private void extractDatabaseData(String content, ExtractedData extractedData) {
        // Extract table names
        Pattern tablePattern = Pattern.compile("(?i)CREATE\\s+TABLE\\s+([a-zA-Z_][a-zA-Z0-9_]*)");
        Matcher tableMatcher = tablePattern.matcher(content);
        while (tableMatcher.find()) {
            extractedData.addExtractedInfo(ExtractedData.DataType.CONFIGURATION, 
                                         "Table: " + tableMatcher.group(1));
        }
        
        // Extract column names that might indicate sensitive data
        Pattern columnPattern = Pattern.compile("(?i)\\b(password|secret|key|token|ssn|credit_card|email)\\b");
        Matcher columnMatcher = columnPattern.matcher(content);
        while (columnMatcher.find()) {
            extractedData.addExtractedInfo(ExtractedData.DataType.VULNERABILITY, 
                                         "Sensitive column: " + columnMatcher.group());
        }
    }
    
    /**
     * Extracts deployment-specific data.
     * 
     * @param content File content to analyze
     * @param extractedData Container for extracted data
     */
    private void extractDeploymentData(String content, ExtractedData extractedData) {
        // Extract Docker image names
        Pattern imagePattern = Pattern.compile("(?i)FROM\\s+([^\\s]+)");
        Matcher imageMatcher = imagePattern.matcher(content);
        while (imageMatcher.find()) {
            extractedData.addExtractedInfo(ExtractedData.DataType.VERSION_INFO, 
                                         "Docker image: " + imageMatcher.group(1));
        }
        
        // Extract exposed ports
        Pattern portPattern = Pattern.compile("(?i)EXPOSE\\s+(\\d+)");
        Matcher portMatcher = portPattern.matcher(content);
        while (portMatcher.find()) {
            extractedData.addExtractedInfo(ExtractedData.DataType.NETWORK_INFO, 
                                         "Exposed port: " + portMatcher.group(1));
        }
        
        // Extract environment variables
        Pattern envPattern = Pattern.compile("(?i)ENV\\s+([^\\s=]+)\\s*=?\\s*(.*)");
        Matcher envMatcher = envPattern.matcher(content);
        while (envMatcher.find()) {
            String key = envMatcher.group(1);
            String value = envMatcher.group(2);
            if (looksLikeSensitiveData(key) || looksLikeSensitiveData(value)) {
                extractedData.addExtractedInfo(ExtractedData.DataType.SECRET, 
                                             "ENV: " + key + "=" + value);
            } else {
                extractedData.addExtractedInfo(ExtractedData.DataType.CONFIGURATION, 
                                             "ENV: " + key + "=" + value);
            }
        }
    }
    
    /**
     * Heuristic to determine if a string looks like sensitive data.
     * 
     * @param value The string to evaluate
     * @return true if it looks sensitive
     */
    private boolean looksLikeSensitiveData(String value) {
        if (value == null || value.length() < 6) {
            return false;
        }
        
        String lower = value.toLowerCase();
        String[] sensitiveKeywords = {"password", "secret", "key", "token", "auth", 
                                    "credential", "private", "api", "jwt", "bearer"};
        
        for (String keyword : sensitiveKeywords) {
            if (lower.contains(keyword)) {
                return true;
            }
        }
        
        // Check for base64-like strings
        if (value.matches("^[A-Za-z0-9+/]+=*$") && value.length() > 16) {
            return true;
        }
        
        // Check for hex strings that might be keys
        if (value.matches("^[a-fA-F0-9]+$") && value.length() >= 32) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Clears the analysis cache.
     */
    public void clearCache() {
        analysisCache.clear();
        logger.info("Analysis cache cleared");
    }
    
    /**
     * Gets the current cache size.
     * 
     * @return Number of cached analyses
     */
    public int getCacheSize() {
        return analysisCache.size();
    }
    
    /**
     * Shuts down the file analyzer and releases resources.
     */
    public void shutdown() {
        executorService.shutdown();
        clearCache();
        logger.info("FileAnalyzer shutdown complete");
    }
    
    @Override
    public String toString() {
        return String.format("FileAnalyzer{maxFileSize=%d, strictMode=%s, cache=%d}", 
                           maxFileSize, strictScopeMode, getCacheSize());
    }
}