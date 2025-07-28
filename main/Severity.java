package com.example;

/**
 * Enum representing vulnerability severity levels for pentesting projects.
 * 
 * This classification follows industry standards like CVSS and OWASP guidelines
 * to provide consistent vulnerability prioritization across projects.
 * 
 * @author Agent-Orange Team
 * @version 1.0
 */
public enum Severity {
    /**
     * Critical vulnerabilities that allow immediate system compromise.
     * CVSS Score: 9.0-10.0
     * Examples: RCE, SQLi with data access, authentication bypass
     */
    CRITICAL("Critical", 5, "🔴"),
    
    /**
     * High-severity vulnerabilities that pose significant security risks.
     * CVSS Score: 7.0-8.9
     * Examples: XSS, privilege escalation, data exposure
     */
    HIGH("High", 4, "🟠"),
    
    /**
     * Medium-severity vulnerabilities with moderate impact.
     * CVSS Score: 4.0-6.9
     * Examples: Information disclosure, weak encryption
     */
    MEDIUM("Medium", 3, "🟡"),
    
    /**
     * Low-severity vulnerabilities with limited impact.
     * CVSS Score: 0.1-3.9
     * Examples: Version disclosure, minor configuration issues
     */
    LOW("Low", 2, "🟢"),
    
    /**
     * Informational findings that don't pose direct security risks.
     * CVSS Score: 0.0
     * Examples: Security recommendations, best practices
     */
    INFO("Info", 1, "🔵");
    
    private final String displayName;
    private final int priority;
    private final String icon;
    
    /**
     * Constructs a Severity enum value.
     * 
     * @param displayName Human-readable name for the severity level
     * @param priority Numeric priority for sorting (higher = more severe)
     * @param icon Unicode icon for visual representation
     */
    Severity(String displayName, int priority, String icon) {
        this.displayName = displayName;
        this.priority = priority;
        this.icon = icon;
    }
    
    /**
     * Gets the human-readable display name.
     * 
     * @return The display name for this severity level
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Gets the numeric priority for sorting and comparison.
     * 
     * @return Priority value (higher = more severe)
     */
    public int getPriority() {
        return priority;
    }
    
    /**
     * Gets the Unicode icon for visual representation.
     * 
     * @return Unicode icon string
     */
    public String getIcon() {
        return icon;
    }
    
    /**
     * Parses a string to a Severity enum value.
     * Case-insensitive and supports both display names and enum names.
     * 
     * @param severityString The string to parse
     * @return The corresponding Severity enum value
     * @throws IllegalArgumentException if the string doesn't match any severity level
     */
    public static Severity fromString(String severityString) {
        if (severityString == null || severityString.trim().isEmpty()) {
            return INFO; // Default to INFO for empty/null input
        }
        
        String normalized = severityString.trim().toUpperCase();
        
        // Try exact enum name match first
        try {
            return Severity.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            // Fall through to display name matching
        }
        
        // Try display name matching
        for (Severity severity : Severity.values()) {
            if (severity.getDisplayName().equalsIgnoreCase(severityString.trim())) {
                return severity;
            }
        }
        
        throw new IllegalArgumentException("Unknown severity level: " + severityString);
    }
    
    /**
     * Gets a formatted string representation with icon and name.
     * 
     * @return Formatted string like "🔴 Critical"
     */
    public String getFormattedName() {
        return icon + " " + displayName;
    }
    
    /**
     * Returns the display name for string representation.
     * 
     * @return The display name
     */
    @Override
    public String toString() {
        return displayName;
    }
}