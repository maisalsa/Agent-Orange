package com.example;

import java.util.regex.Pattern;

/**
 * Utility class for CVE (Common Vulnerabilities and Exposures) ID validation and management.
 * 
 * This class provides methods to validate CVE ID format, generate descriptive names
 * when CVE IDs are not available, and ensure consistent vulnerability naming
 * conventions throughout the Agent-Orange system.
 * 
 * @author Agent-Orange Team
 * @version 1.0
 */
public class CveUtils {
    
    /**
     * Pattern for valid CVE ID format: CVE-YYYY-Number
     * Where YYYY is a 4-digit year and Number is at least 4 digits
     */
    private static final Pattern CVE_PATTERN = Pattern.compile(
        "^CVE-\\d{4}-\\d{4,}$", 
        Pattern.CASE_INSENSITIVE
    );
    
    /**
     * Pattern for relaxed CVE format matching (for parsing from various sources)
     */
    private static final Pattern CVE_LOOSE_PATTERN = Pattern.compile(
        "\\b(CVE-\\d{4}-\\d{4,})\\b", 
        Pattern.CASE_INSENSITIVE
    );
    
    /**
     * Validates if a string is a properly formatted CVE ID.
     * 
     * @param cveId The string to validate
     * @return true if the string is a valid CVE ID format
     */
    public static boolean isValidCveId(String cveId) {
        if (cveId == null || cveId.trim().isEmpty()) {
            return false;
        }
        
        return CVE_PATTERN.matcher(cveId.trim()).matches();
    }
    
    /**
     * Normalizes a CVE ID to the standard format (uppercase).
     * 
     * @param cveId The CVE ID to normalize
     * @return The normalized CVE ID or null if invalid
     */
    public static String normalizeCveId(String cveId) {
        if (!isValidCveId(cveId)) {
            return null;
        }
        
        return cveId.trim().toUpperCase();
    }
    
    /**
     * Extracts a CVE ID from a longer text string.
     * 
     * @param text The text to search for CVE IDs
     * @return The first valid CVE ID found, or null if none found
     */
    public static String extractCveIdFromText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        
        java.util.regex.Matcher matcher = CVE_LOOSE_PATTERN.matcher(text);
        if (matcher.find()) {
            String found = matcher.group(1);
            return normalizeCveId(found);
        }
        
        return null;
    }
    
    /**
     * Generates a descriptive vulnerability name from a description or title.
     * This is used when no CVE ID is available.
     * 
     * @param description The vulnerability description
     * @param target The target system (optional)
     * @return A descriptive vulnerability name
     */
    public static String generateDescriptiveName(String description, String target) {
        if (description == null || description.trim().isEmpty()) {
            return "Unknown Vulnerability";
        }
        
        String cleaned = description.trim();
        
        // Remove common prefixes and suffixes
        cleaned = cleaned.replaceAll("(?i)^(vulnerability|vuln|issue|finding|security|exploit)\\s*:?\\s*", "");
        cleaned = cleaned.replaceAll("(?i)\\s*(vulnerability|vuln|issue|finding)\\s*$", "");
        
        // Truncate if too long
        if (cleaned.length() > 80) {
            cleaned = cleaned.substring(0, 77) + "...";
        }
        
        // Capitalize first letter
        if (!cleaned.isEmpty()) {
            cleaned = Character.toUpperCase(cleaned.charAt(0)) + cleaned.substring(1);
        }
        
        // Add target context if provided and not already mentioned
        if (target != null && !target.trim().isEmpty() && 
            !cleaned.toLowerCase().contains(target.toLowerCase())) {
            cleaned += " (" + target.trim() + ")";
        }
        
        return cleaned.isEmpty() ? "Unknown Vulnerability" : cleaned;
    }
    
    /**
     * Determines the best name for a vulnerability based on available information.
     * Prioritizes CVE ID if valid, otherwise uses descriptive name.
     * 
     * @param cveId The potential CVE ID
     * @param name The current name/title
     * @param description The vulnerability description
     * @param target The target system
     * @return The best name to use for the vulnerability
     */
    public static String determineBestName(String cveId, String name, String description, String target) {
        // First, check if we have a valid CVE ID
        String normalizedCveId = normalizeCveId(cveId);
        if (normalizedCveId != null) {
            return normalizedCveId;
        }
        
        // Check if the name itself contains a valid CVE ID
        String extractedCveId = extractCveIdFromText(name);
        if (extractedCveId != null) {
            return extractedCveId;
        }
        
        // Check if the description contains a valid CVE ID
        extractedCveId = extractCveIdFromText(description);
        if (extractedCveId != null) {
            return extractedCveId;
        }
        
        // If we have a good descriptive name, use it
        if (name != null && !name.trim().isEmpty() && 
            !name.trim().matches("^[A-Z0-9_-]+$")) { // Not just an ID
            return generateDescriptiveName(name, target);
        }
        
        // Fall back to description-based name
        return generateDescriptiveName(description, target);
    }
    
    /**
     * Determines the CVE ID to store based on available information.
     * 
     * @param cveId The explicit CVE ID
     * @param name The vulnerability name
     * @param description The vulnerability description
     * @return The CVE ID to store, or null if none found
     */
    public static String determineCveId(String cveId, String name, String description) {
        // First, check explicit CVE ID
        String normalizedCveId = normalizeCveId(cveId);
        if (normalizedCveId != null) {
            return normalizedCveId;
        }
        
        // Check name for CVE ID
        String extractedCveId = extractCveIdFromText(name);
        if (extractedCveId != null) {
            return extractedCveId;
        }
        
        // Check description for CVE ID
        extractedCveId = extractCveIdFromText(description);
        if (extractedCveId != null) {
            return extractedCveId;
        }
        
        return null;
    }
    
    /**
     * Creates a vulnerability information bundle with proper CVE naming conventions.
     * 
     * @param id The vulnerability ID
     * @param rawName The raw name/title
     * @param description The description
     * @param target The target system
     * @param cveId The explicit CVE ID (can be null)
     * @return VulnerabilityInfo with properly set name and CVE ID
     */
    public static VulnerabilityInfo createVulnerabilityInfo(String id, String rawName, 
                                                           String description, String target, String cveId) {
        String determinedCveId = determineCveId(cveId, rawName, description);
        String bestName = determineBestName(cveId, rawName, description, target);
        
        // If we found a CVE ID, make sure the description doesn't duplicate it
        String finalDescription = description;
        if (determinedCveId != null && description != null && 
            description.toLowerCase().contains(determinedCveId.toLowerCase())) {
            finalDescription = description.replaceAll("(?i)" + Pattern.quote(determinedCveId) + "\\s*:?\\s*", "").trim();
        }
        
        return new VulnerabilityInfo(id, bestName, finalDescription, determinedCveId);
    }
    
    /**
     * Data class to hold vulnerability information with proper naming.
     */
    public static class VulnerabilityInfo {
        private final String id;
        private final String name;
        private final String description;
        private final String cveId;
        
        public VulnerabilityInfo(String id, String name, String description, String cveId) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.cveId = cveId;
        }
        
        public String getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public String getCveId() { return cveId; }
        public boolean hasCveId() { return cveId != null && !cveId.trim().isEmpty(); }
        
        @Override
        public String toString() {
            return String.format("VulnerabilityInfo{id='%s', name='%s', cveId='%s'}", 
                               id, name, cveId);
        }
    }
    
    /**
     * Formats a vulnerability name for display, prioritizing CVE ID.
     * 
     * @param vulnerability The vulnerability to format
     * @return Formatted display name
     */
    public static String formatDisplayName(Vulnerability vulnerability) {
        if (vulnerability == null) {
            return "Unknown Vulnerability";
        }
        
        String cveId = vulnerability.getCveId();
        if (cveId != null && !cveId.trim().isEmpty()) {
            return cveId;
        }
        
        return vulnerability.getName();
    }
    
    /**
     * Formats a vulnerability name with additional context for display.
     * 
     * @param vulnerability The vulnerability to format
     * @param includeTarget Whether to include target information
     * @return Formatted display name with context
     */
    public static String formatDisplayNameWithContext(Vulnerability vulnerability, boolean includeTarget) {
        if (vulnerability == null) {
            return "Unknown Vulnerability";
        }
        
        StringBuilder display = new StringBuilder();
        
        String cveId = vulnerability.getCveId();
        if (cveId != null && !cveId.trim().isEmpty()) {
            display.append(cveId);
            
            // If the name is different from CVE ID, add it as context
            String name = vulnerability.getName();
            if (name != null && !name.equals(cveId)) {
                display.append(" (").append(name).append(")");
            }
        } else {
            display.append(vulnerability.getName());
        }
        
        if (includeTarget && vulnerability.getTarget() != null) {
            display.append(" - ").append(vulnerability.getTarget());
        }
        
        return display.toString();
    }
}