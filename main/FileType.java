package com.example;

import java.util.Arrays;
import java.util.List;

/**
 * Enum representing different file types that can be analyzed for information gathering.
 * 
 * This classification helps determine the appropriate parsing strategy and
 * security considerations for each file type during reconnaissance.
 * 
 * @author Agent-Orange Team
 * @version 1.0
 */
public enum FileType {
    /**
     * Configuration files containing application settings and credentials.
     * Examples: .env, config.ini, application.properties, web.xml
     */
    CONFIGURATION("Configuration", Arrays.asList(".env", ".ini", ".conf", ".config", ".properties", ".xml", ".yml", ".yaml", ".json"), true),
    
    /**
     * Log files containing runtime information and potential security data.
     * Examples: access.log, error.log, application.log, syslog
     */
    LOG("Log", Arrays.asList(".log", ".out", ".err", ".txt"), false),
    
    /**
     * Database-related files containing schemas and connection information.
     * Examples: .sql, .db, .sqlite, database.yml
     */
    DATABASE("Database", Arrays.asList(".sql", ".db", ".sqlite", ".sqlite3", ".mdb", ".accdb"), true),
    
    /**
     * Source code files that may contain hardcoded secrets or configuration.
     * Examples: .java, .py, .js, .php, .rb, .cs
     */
    SOURCE_CODE("Source Code", Arrays.asList(".java", ".py", ".js", ".php", ".rb", ".cs", ".cpp", ".c", ".h"), true),
    
    /**
     * Web application files containing endpoints and configurations.
     * Examples: web.xml, .htaccess, index.php, app.js
     */
    WEB_APPLICATION("Web Application", Arrays.asList(".jsp", ".asp", ".aspx", ".php", ".html", ".htm", ".js", ".css", ".htaccess"), true),
    
    /**
     * Documentation files that may contain security information.
     * Examples: README.md, SECURITY.md, docs/
     */
    DOCUMENTATION("Documentation", Arrays.asList(".md", ".txt", ".rst", ".doc", ".docx", ".pdf"), false),
    
    /**
     * Container and deployment files.
     * Examples: Dockerfile, docker-compose.yml, kubernetes manifests
     */
    DEPLOYMENT("Deployment", Arrays.asList("dockerfile", ".dockerfile", "docker-compose.yml", ".k8s", ".yaml", ".yml"), true),
    
    /**
     * Backup and archive files that may contain sensitive data.
     * Examples: .bak, .backup, .tar, .zip
     */
    BACKUP("Backup", Arrays.asList(".bak", ".backup", ".old", ".orig", ".tar", ".zip", ".gz", ".7z"), true),
    
    /**
     * Certificate and key files.
     * Examples: .pem, .key, .crt, .p12
     */
    CERTIFICATE("Certificate", Arrays.asList(".pem", ".key", ".crt", ".cer", ".p12", ".pfx", ".jks"), true),
    
    /**
     * Unknown or unclassified file types.
     */
    UNKNOWN("Unknown", Arrays.asList(), false);
    
    private final String displayName;
    private final List<String> extensions;
    private final boolean highSensitivity;
    
    /**
     * Constructs a FileType enum value.
     * 
     * @param displayName Human-readable name for this file type
     * @param extensions List of file extensions associated with this type
     * @param highSensitivity Whether this file type typically contains sensitive information
     */
    FileType(String displayName, List<String> extensions, boolean highSensitivity) {
        this.displayName = displayName;
        this.extensions = extensions;
        this.highSensitivity = highSensitivity;
    }
    
    /**
     * Gets the human-readable display name.
     * 
     * @return The display name for this file type
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Gets the list of file extensions associated with this type.
     * 
     * @return List of file extensions (including the dot)
     */
    public List<String> getExtensions() {
        return extensions;
    }
    
    /**
     * Checks if this file type typically contains sensitive information.
     * 
     * @return true if high sensitivity, false otherwise
     */
    public boolean isHighSensitivity() {
        return highSensitivity;
    }
    
    /**
     * Determines the file type based on the filename or path.
     * 
     * @param filename The filename or path to analyze
     * @return The determined FileType
     */
    public static FileType fromFilename(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return UNKNOWN;
        }
        
        String lowerFilename = filename.toLowerCase();
        
        // Check for exact filename matches first
        if (lowerFilename.endsWith("dockerfile") || lowerFilename.contains("docker-compose")) {
            return DEPLOYMENT;
        }
        
        if (lowerFilename.endsWith(".env") || lowerFilename.contains("config")) {
            return CONFIGURATION;
        }
        
        if (lowerFilename.contains("readme") || lowerFilename.contains("security") || 
            lowerFilename.contains("doc")) {
            return DOCUMENTATION;
        }
        
        // Check by file extension
        for (FileType type : values()) {
            if (type == UNKNOWN) continue;
            
            for (String extension : type.getExtensions()) {
                if (lowerFilename.endsWith(extension)) {
                    return type;
                }
            }
        }
        
        return UNKNOWN;
    }
    
    /**
     * Gets the security icon for this file type.
     * 
     * @return Unicode icon representing security level
     */
    public String getSecurityIcon() {
        return highSensitivity ? "🔒" : "📄";
    }
    
    /**
     * Gets a formatted display string with icon and name.
     * 
     * @return Formatted string like "🔒 Configuration"
     */
    public String getFormattedName() {
        return getSecurityIcon() + " " + displayName;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}