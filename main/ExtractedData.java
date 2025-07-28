package com.example;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Represents information extracted from a file during reconnaissance.
 * 
 * This class encapsulates all data found in a target file, including
 * sensitive information like credentials, with appropriate security
 * classifications and sanitization methods.
 * 
 * @author Agent-Orange Team
 * @version 1.0
 */
public class ExtractedData {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Enum representing the sensitivity level of extracted data.
     */
    public enum DataType {
        CREDENTIAL("Credential", "🔑", true),
        API_KEY("API Key", "🗝️", true),
        DATABASE_CONNECTION("Database Connection", "🗄️", true),
        ENDPOINT("Endpoint", "🌐", false),
        CONFIGURATION("Configuration", "⚙️", false),
        VERSION_INFO("Version Info", "📋", false),
        VULNERABILITY("Vulnerability", "🚨", true),
        SECRET("Secret", "🔒", true),
        USER_INFO("User Info", "👤", true),
        FILE_PATH("File Path", "📁", false),
        NETWORK_INFO("Network Info", "🌐", false),
        OTHER("Other", "📄", false);
        
        private final String displayName;
        private final String icon;
        private final boolean sensitive;
        
        DataType(String displayName, String icon, boolean sensitive) {
            this.displayName = displayName;
            this.icon = icon;
            this.sensitive = sensitive;
        }
        
        public String getDisplayName() { return displayName; }
        public String getIcon() { return icon; }
        public boolean isSensitive() { return sensitive; }
        public String getFormattedName() { return icon + " " + displayName; }
    }
    
    private final String filePath;
    private final FileType fileType;
    private final LocalDateTime extractedAt;
    private final String extractedBy;
    private final Map<DataType, List<String>> extractedInfo;
    private final Map<String, String> metadata;
    private final Set<String> warnings;
    
    /**
     * Constructs a new ExtractedData instance.
     * 
     * @param filePath The path of the file from which data was extracted
     * @param fileType The type of the file
     * @param extractedBy The component that performed the extraction
     */
    public ExtractedData(String filePath, FileType fileType, String extractedBy) {
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path cannot be null or empty");
        }
        if (fileType == null) {
            throw new IllegalArgumentException("File type cannot be null");
        }
        
        this.filePath = filePath.trim();
        this.fileType = fileType;
        this.extractedAt = LocalDateTime.now();
        this.extractedBy = extractedBy != null ? extractedBy.trim() : "Unknown";
        this.extractedInfo = new HashMap<>();
        this.metadata = new HashMap<>();
        this.warnings = new HashSet<>();
        
        // Initialize all data types with empty lists
        for (DataType type : DataType.values()) {
            this.extractedInfo.put(type, new ArrayList<>());
        }
    }
    
    /**
     * Gets the file path.
     * 
     * @return The file path
     */
    public String getFilePath() {
        return filePath;
    }
    
    /**
     * Gets the file type.
     * 
     * @return The file type
     */
    public FileType getFileType() {
        return fileType;
    }
    
    /**
     * Gets the extraction timestamp.
     * 
     * @return The extraction timestamp
     */
    public LocalDateTime getExtractedAt() {
        return extractedAt;
    }
    
    /**
     * Gets the component that performed the extraction.
     * 
     * @return The extractor identifier
     */
    public String getExtractedBy() {
        return extractedBy;
    }
    
    /**
     * Adds extracted information of a specific type.
     * 
     * @param dataType The type of data
     * @param value The extracted value
     */
    public void addExtractedInfo(DataType dataType, String value) {
        if (dataType == null || value == null || value.trim().isEmpty()) {
            return;
        }
        
        String normalizedValue = value.trim();
        List<String> values = extractedInfo.get(dataType);
        
        // Avoid duplicates
        if (!values.contains(normalizedValue)) {
            values.add(normalizedValue);
            
            // Add security warning for sensitive data
            if (dataType.isSensitive()) {
                warnings.add("Sensitive " + dataType.getDisplayName().toLowerCase() + " detected in " + filePath);
            }
        }
    }
    
    /**
     * Gets all extracted information of a specific type.
     * 
     * @param dataType The type of data to retrieve
     * @return List of extracted values for the specified type
     */
    public List<String> getExtractedInfo(DataType dataType) {
        if (dataType == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(extractedInfo.getOrDefault(dataType, new ArrayList<>()));
    }
    
    /**
     * Gets all extracted information with sanitization for display.
     * 
     * @param sanitizeSensitive Whether to sanitize sensitive information
     * @return Map of data types to extracted values
     */
    public Map<DataType, List<String>> getAllExtractedInfo(boolean sanitizeSensitive) {
        Map<DataType, List<String>> result = new HashMap<>();
        
        for (Map.Entry<DataType, List<String>> entry : extractedInfo.entrySet()) {
            DataType type = entry.getKey();
            List<String> values = entry.getValue();
            
            if (values.isEmpty()) {
                continue; // Skip empty categories
            }
            
            if (sanitizeSensitive && type.isSensitive()) {
                List<String> sanitized = new ArrayList<>();
                for (String value : values) {
                    sanitized.add(sanitizeValue(value));
                }
                result.put(type, sanitized);
            } else {
                result.put(type, new ArrayList<>(values));
            }
        }
        
        return result;
    }
    
    /**
     * Sanitizes a sensitive value for display.
     * 
     * @param value The value to sanitize
     * @return Sanitized value
     */
    private String sanitizeValue(String value) {
        if (value == null || value.length() <= 4) {
            return "[REDACTED]";
        }
        
        // Show first 2 and last 2 characters with asterisks in between
        int len = value.length();
        if (len <= 8) {
            return value.substring(0, 2) + "*".repeat(len - 4) + value.substring(len - 2);
        } else {
            return value.substring(0, 2) + "*".repeat(6) + value.substring(len - 2);
        }
    }
    
    /**
     * Adds metadata about the extraction process.
     * 
     * @param key The metadata key
     * @param value The metadata value
     */
    public void addMetadata(String key, String value) {
        if (key != null && value != null) {
            metadata.put(key.trim(), value.trim());
        }
    }
    
    /**
     * Gets metadata value by key.
     * 
     * @param key The metadata key
     * @return The metadata value or null if not found
     */
    public String getMetadata(String key) {
        return key != null ? metadata.get(key.trim()) : null;
    }
    
    /**
     * Gets all metadata.
     * 
     * @return Map of all metadata
     */
    public Map<String, String> getAllMetadata() {
        return new HashMap<>(metadata);
    }
    
    /**
     * Adds a warning message.
     * 
     * @param warning The warning message
     */
    public void addWarning(String warning) {
        if (warning != null && !warning.trim().isEmpty()) {
            warnings.add(warning.trim());
        }
    }
    
    /**
     * Gets all warnings.
     * 
     * @return Set of warning messages
     */
    public Set<String> getWarnings() {
        return new HashSet<>(warnings);
    }
    
    /**
     * Checks if any sensitive data was extracted.
     * 
     * @return true if sensitive data was found, false otherwise
     */
    public boolean hasSensitiveData() {
        for (Map.Entry<DataType, List<String>> entry : extractedInfo.entrySet()) {
            if (entry.getKey().isSensitive() && !entry.getValue().isEmpty()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Gets the total count of extracted items.
     * 
     * @return Total number of extracted data items
     */
    public int getTotalItemCount() {
        return extractedInfo.values().stream()
                .mapToInt(List::size)
                .sum();
    }
    
    /**
     * Gets count of extracted items by type.
     * 
     * @param dataType The data type to count
     * @return Number of items of the specified type
     */
    public int getItemCount(DataType dataType) {
        if (dataType == null) {
            return 0;
        }
        return extractedInfo.getOrDefault(dataType, new ArrayList<>()).size();
    }
    
    /**
     * Checks if the extraction found any data.
     * 
     * @return true if data was extracted, false otherwise
     */
    public boolean hasData() {
        return getTotalItemCount() > 0;
    }
    
    /**
     * Creates a formatted summary of the extracted data.
     * 
     * @param sanitizeSensitive Whether to sanitize sensitive information
     * @return Formatted summary string
     */
    public String getFormattedSummary(boolean sanitizeSensitive) {
        StringBuilder summary = new StringBuilder();
        
        summary.append("📁 File Analysis: ").append(filePath).append("\n");
        summary.append("📄 Type: ").append(fileType.getFormattedName()).append("\n");
        summary.append("⏰ Extracted: ").append(extractedAt.format(DATE_FORMAT)).append("\n");
        summary.append("🔧 By: ").append(extractedBy).append("\n");
        summary.append("📊 Total Items: ").append(getTotalItemCount()).append("\n");
        
        if (hasSensitiveData()) {
            summary.append("⚠️  Contains sensitive data\n");
        }
        
        summary.append("\n");
        
        // Show extracted data by category
        Map<DataType, List<String>> allData = getAllExtractedInfo(sanitizeSensitive);
        for (Map.Entry<DataType, List<String>> entry : allData.entrySet()) {
            DataType type = entry.getKey();
            List<String> values = entry.getValue();
            
            if (!values.isEmpty()) {
                summary.append(type.getFormattedName()).append(" (").append(values.size()).append("):\n");
                for (String value : values) {
                    summary.append("  • ").append(value).append("\n");
                }
                summary.append("\n");
            }
        }
        
        // Show warnings
        if (!warnings.isEmpty()) {
            summary.append("⚠️  Warnings:\n");
            for (String warning : warnings) {
                summary.append("  ⚠️  ").append(warning).append("\n");
            }
            summary.append("\n");
        }
        
        // Show metadata
        if (!metadata.isEmpty()) {
            summary.append("ℹ️  Metadata:\n");
            for (Map.Entry<String, String> entry : metadata.entrySet()) {
                summary.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
            }
        }
        
        return summary.toString();
    }
    
    /**
     * Serializes the extracted data to a string format.
     * Note: Sensitive data is not included in serialization for security.
     * 
     * @return Serialized data
     */
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("EXTRACTED_DATA_START\n");
        sb.append("filePath=").append(filePath).append("\n");
        sb.append("fileType=").append(fileType.name()).append("\n");
        sb.append("extractedAt=").append(extractedAt.format(DATE_FORMAT)).append("\n");
        sb.append("extractedBy=").append(extractedBy).append("\n");
        
        // Serialize non-sensitive data only
        for (Map.Entry<DataType, List<String>> entry : extractedInfo.entrySet()) {
            DataType type = entry.getKey();
            List<String> values = entry.getValue();
            
            if (!values.isEmpty() && !type.isSensitive()) {
                sb.append("data.").append(type.name()).append("=");
                for (int i = 0; i < values.size(); i++) {
                    if (i > 0) sb.append("||");
                    sb.append(values.get(i).replace("\n", "\\n"));
                }
                sb.append("\n");
            }
        }
        
        // Serialize metadata
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            sb.append("metadata.").append(entry.getKey()).append("=")
              .append(entry.getValue().replace("\n", "\\n")).append("\n");
        }
        
        // Serialize warnings
        if (!warnings.isEmpty()) {
            sb.append("warnings=");
            String[] warningArray = warnings.toArray(new String[0]);
            for (int i = 0; i < warningArray.length; i++) {
                if (i > 0) sb.append("||");
                sb.append(warningArray[i].replace("\n", "\\n"));
            }
            sb.append("\n");
        }
        
        sb.append("EXTRACTED_DATA_END\n");
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return String.format("ExtractedData{file='%s', type=%s, items=%d, sensitive=%s}", 
                           filePath, fileType, getTotalItemCount(), hasSensitiveData());
    }
}