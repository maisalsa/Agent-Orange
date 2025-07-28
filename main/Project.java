package com.example;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a complete penetration testing project.
 * 
 * This class encapsulates all aspects of a pentesting engagement including
 * scope, targets, vulnerabilities, and project metadata. It provides the
 * foundation for organizing and managing security assessments.
 * 
 * @author Agent-Orange Team
 * @version 1.0
 */
public class Project {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private String name;
    private String description;
    private String scope;
    private Set<String> targets;
    private VulnerabilityTree vulnerabilities;
    private LocalDateTime createdAt;
    private LocalDateTime lastModified;
    private String createdBy;
    private String status; // "ACTIVE", "COMPLETED", "ARCHIVED", "ON_HOLD"
    private List<String> tags;
    private String notes;
    
    /**
     * Constructs a new Project with the specified name.
     * 
     * @param name The name of the project
     * @throws IllegalArgumentException if name is null or empty
     */
    public Project(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Project name cannot be null or empty");
        }
        
        this.name = name.trim();
        this.targets = new HashSet<>();
        this.vulnerabilities = new VulnerabilityTree(this.name);
        this.createdAt = LocalDateTime.now();
        this.lastModified = LocalDateTime.now();
        this.status = "ACTIVE";
        this.tags = new ArrayList<>();
    }
    
    /**
     * Gets the project name.
     * 
     * @return The project name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Sets the project name.
     * 
     * @param name The new project name
     * @throws IllegalArgumentException if name is null or empty
     */
    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Project name cannot be null or empty");
        }
        this.name = name.trim();
        updateLastModified();
    }
    
    /**
     * Gets the project description.
     * 
     * @return The project description or null if not set
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Sets the project description.
     * 
     * @param description The project description
     */
    public void setDescription(String description) {
        this.description = (description != null) ? description.trim() : null;
        updateLastModified();
    }
    
    /**
     * Gets the project scope.
     * 
     * @return The project scope or null if not set
     */
    public String getScope() {
        return scope;
    }
    
    /**
     * Sets the project scope.
     * 
     * @param scope The project scope definition
     */
    public void setScope(String scope) {
        this.scope = (scope != null) ? scope.trim() : null;
        updateLastModified();
    }
    
    /**
     * Gets the set of targets for this project.
     * 
     * @return An unmodifiable view of the targets set
     */
    public Set<String> getTargets() {
        return new HashSet<>(targets);
    }
    
    /**
     * Adds a target to the project.
     * 
     * @param target The target to add (e.g., IP address, domain name)
     * @throws IllegalArgumentException if target is null or empty
     */
    public void addTarget(String target) {
        if (target == null || target.trim().isEmpty()) {
            throw new IllegalArgumentException("Target cannot be null or empty");
        }
        
        String normalizedTarget = target.trim();
        if (targets.add(normalizedTarget)) {
            updateLastModified();
        }
    }
    
    /**
     * Removes a target from the project.
     * 
     * @param target The target to remove
     * @return true if the target was removed, false if it wasn't found
     */
    public boolean removeTarget(String target) {
        if (target == null || target.trim().isEmpty()) {
            return false;
        }
        
        boolean removed = targets.remove(target.trim());
        if (removed) {
            updateLastModified();
        }
        return removed;
    }
    
    /**
     * Checks if a target is in the project scope.
     * 
     * @param target The target to check
     * @return true if the target is in scope, false otherwise
     */
    public boolean isTargetInScope(String target) {
        if (target == null || target.trim().isEmpty()) {
            return false;
        }
        return targets.contains(target.trim());
    }
    
    /**
     * Gets the vulnerability tree for this project.
     * 
     * @return The vulnerability tree
     */
    public VulnerabilityTree getVulnerabilities() {
        return vulnerabilities;
    }
    
    /**
     * Adds a vulnerability to the project.
     * 
     * @param vulnerability The vulnerability to add
     * @throws IllegalArgumentException if vulnerability is null
     */
    public void addVulnerability(Vulnerability vulnerability) {
        if (vulnerability == null) {
            throw new IllegalArgumentException("Vulnerability cannot be null");
        }
        
        vulnerabilities.addVulnerability(vulnerability);
        
        // Automatically add the vulnerability's target to the project targets
        addTarget(vulnerability.getTarget());
        
        updateLastModified();
    }
    
    /**
     * Removes a vulnerability from the project.
     * 
     * @param vulnerabilityId The ID of the vulnerability to remove
     * @return true if the vulnerability was removed, false if not found
     */
    public boolean removeVulnerability(String vulnerabilityId) {
        boolean removed = vulnerabilities.removeVulnerability(vulnerabilityId);
        if (removed) {
            updateLastModified();
        }
        return removed;
    }
    
    /**
     * Finds a vulnerability by ID.
     * 
     * @param vulnerabilityId The ID of the vulnerability to find
     * @return The vulnerability if found, null otherwise
     */
    public Vulnerability findVulnerability(String vulnerabilityId) {
        return vulnerabilities.findVulnerability(vulnerabilityId);
    }
    
    /**
     * Gets vulnerabilities for a specific target.
     * 
     * @param target The target to get vulnerabilities for
     * @return A list of vulnerabilities for the target
     */
    public List<Vulnerability> getVulnerabilitiesForTarget(String target) {
        return vulnerabilities.getVulnerabilitiesForTarget(target);
    }
    
    /**
     * Gets the creation timestamp.
     * 
     * @return The creation timestamp
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    /**
     * Sets the creation timestamp.
     * 
     * @param createdAt The creation timestamp
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    /**
     * Gets the last modification timestamp.
     * 
     * @return The last modification timestamp
     */
    public LocalDateTime getLastModified() {
        return lastModified;
    }
    
    /**
     * Sets the last modification timestamp.
     * 
     * @param lastModified The last modification timestamp
     */
    public void setLastModified(LocalDateTime lastModified) {
        this.lastModified = lastModified;
    }
    
    /**
     * Updates the last modification timestamp to now.
     */
    private void updateLastModified() {
        this.lastModified = LocalDateTime.now();
    }
    
    /**
     * Gets the creator of this project.
     * 
     * @return The creator name or null if not set
     */
    public String getCreatedBy() {
        return createdBy;
    }
    
    /**
     * Sets the creator of this project.
     * 
     * @param createdBy The creator name
     */
    public void setCreatedBy(String createdBy) {
        this.createdBy = (createdBy != null) ? createdBy.trim() : null;
        updateLastModified();
    }
    
    /**
     * Gets the project status.
     * 
     * @return The project status
     */
    public String getStatus() {
        return status;
    }
    
    /**
     * Sets the project status.
     * 
     * @param status The new project status (ACTIVE, COMPLETED, ARCHIVED, ON_HOLD)
     * @throws IllegalArgumentException if status is invalid
     */
    public void setStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("Status cannot be null or empty");
        }
        
        String normalizedStatus = status.trim().toUpperCase();
        if (!isValidStatus(normalizedStatus)) {
            throw new IllegalArgumentException("Invalid status: " + status + 
                ". Valid statuses are: ACTIVE, COMPLETED, ARCHIVED, ON_HOLD");
        }
        
        this.status = normalizedStatus;
        updateLastModified();
    }
    
    /**
     * Checks if a status is valid.
     * 
     * @param status The status to check
     * @return true if valid, false otherwise
     */
    private boolean isValidStatus(String status) {
        return "ACTIVE".equals(status) || "COMPLETED".equals(status) || 
               "ARCHIVED".equals(status) || "ON_HOLD".equals(status);
    }
    
    /**
     * Gets the project tags.
     * 
     * @return A copy of the tags list
     */
    public List<String> getTags() {
        return new ArrayList<>(tags);
    }
    
    /**
     * Adds a tag to the project.
     * 
     * @param tag The tag to add
     * @throws IllegalArgumentException if tag is null or empty
     */
    public void addTag(String tag) {
        if (tag == null || tag.trim().isEmpty()) {
            throw new IllegalArgumentException("Tag cannot be null or empty");
        }
        
        String normalizedTag = tag.trim().toLowerCase();
        if (!tags.contains(normalizedTag)) {
            tags.add(normalizedTag);
            updateLastModified();
        }
    }
    
    /**
     * Removes a tag from the project.
     * 
     * @param tag The tag to remove
     * @return true if the tag was removed, false if not found
     */
    public boolean removeTag(String tag) {
        if (tag == null || tag.trim().isEmpty()) {
            return false;
        }
        
        boolean removed = tags.remove(tag.trim().toLowerCase());
        if (removed) {
            updateLastModified();
        }
        return removed;
    }
    
    /**
     * Checks if the project has a specific tag.
     * 
     * @param tag The tag to check for
     * @return true if the project has the tag, false otherwise
     */
    public boolean hasTag(String tag) {
        if (tag == null || tag.trim().isEmpty()) {
            return false;
        }
        return tags.contains(tag.trim().toLowerCase());
    }
    
    /**
     * Gets additional project notes.
     * 
     * @return The project notes or null if not set
     */
    public String getNotes() {
        return notes;
    }
    
    /**
     * Sets additional project notes.
     * 
     * @param notes The project notes
     */
    public void setNotes(String notes) {
        this.notes = (notes != null) ? notes.trim() : null;
        updateLastModified();
    }
    
    /**
     * Gets the total count of vulnerabilities in this project.
     * 
     * @return The total vulnerability count
     */
    public int getTotalVulnerabilityCount() {
        return vulnerabilities.getTotalVulnerabilityCount();
    }
    
    /**
     * Gets vulnerability counts by severity.
     * 
     * @return A list of severity counts
     */
    public List<String> getSeverityCounts() {
        return vulnerabilities.getSeverityCounts();
    }
    
    /**
     * Gets all vulnerabilities in the project.
     * 
     * @return A list of all vulnerabilities
     */
    public List<Vulnerability> getAllVulnerabilities() {
        return vulnerabilities.getAllVulnerabilities();
    }
    
    /**
     * Gets vulnerabilities by severity.
     * 
     * @param severity The severity to filter by
     * @return A list of vulnerabilities with the specified severity
     */
    public List<Vulnerability> getVulnerabilitiesBySeverity(Severity severity) {
        return vulnerabilities.getVulnerabilitiesBySeverity(severity);
    }
    
    /**
     * Generates a comprehensive project summary.
     * 
     * @return A formatted project summary
     */
    public String generateProjectSummary() {
        StringBuilder summary = new StringBuilder();
        
        summary.append("📋 Project Summary: ").append(name).append("\n");
        summary.append("=" .repeat(50)).append("\n");
        
        // Basic project info
        summary.append("📅 Created: ").append(createdAt.format(DATE_FORMAT)).append("\n");
        summary.append("📝 Last Modified: ").append(lastModified.format(DATE_FORMAT)).append("\n");
        summary.append("🔄 Status: ").append(status).append("\n");
        
        if (createdBy != null) {
            summary.append("👤 Created By: ").append(createdBy).append("\n");
        }
        
        if (description != null) {
            summary.append("📖 Description: ").append(description).append("\n");
        }
        
        if (scope != null) {
            summary.append("🎯 Scope: ").append(scope).append("\n");
        }
        
        summary.append("\n");
        
        // Targets
        if (!targets.isEmpty()) {
            summary.append("🎯 Targets (").append(targets.size()).append("):\n");
            for (String target : targets) {
                int targetVulnCount = getVulnerabilitiesForTarget(target).size();
                summary.append("  • ").append(target);
                if (targetVulnCount > 0) {
                    summary.append(" (").append(targetVulnCount).append(" vulnerabilities)");
                }
                summary.append("\n");
            }
            summary.append("\n");
        }
        
        // Vulnerability summary
        int totalVulns = getTotalVulnerabilityCount();
        if (totalVulns > 0) {
            summary.append("🔍 Vulnerabilities (").append(totalVulns).append(" total):\n");
            List<String> severityCounts = getSeverityCounts();
            for (String count : severityCounts) {
                summary.append("  ").append(count).append("\n");
            }
            summary.append("\n");
        } else {
            summary.append("🔍 No vulnerabilities found yet.\n\n");
        }
        
        // Tags
        if (!tags.isEmpty()) {
            summary.append("🏷️  Tags: ").append(String.join(", ", tags)).append("\n\n");
        }
        
        // Notes
        if (notes != null && !notes.isEmpty()) {
            summary.append("📋 Notes:\n").append(notes).append("\n\n");
        }
        
        return summary.toString();
    }
    
    /**
     * Generates a detailed vulnerability report for this project.
     * 
     * @return A formatted vulnerability report
     */
    public String generateVulnerabilityReport() {
        return vulnerabilities.generateSummaryReport();
    }
    
    /**
     * Serializes the project to a string format for file persistence.
     * 
     * @return The serialized project data
     */
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("PROJECT_START\n");
        sb.append("name=").append(name).append("\n");
        sb.append("status=").append(status).append("\n");
        sb.append("createdAt=").append(createdAt.format(DATE_FORMAT)).append("\n");
        sb.append("lastModified=").append(lastModified.format(DATE_FORMAT)).append("\n");
        
        if (description != null) {
            sb.append("description=").append(description.replace("\n", "\\n")).append("\n");
        }
        if (scope != null) {
            sb.append("scope=").append(scope.replace("\n", "\\n")).append("\n");
        }
        if (createdBy != null) {
            sb.append("createdBy=").append(createdBy).append("\n");
        }
        if (notes != null) {
            sb.append("notes=").append(notes.replace("\n", "\\n")).append("\n");
        }
        
        // Serialize targets
        if (!targets.isEmpty()) {
            sb.append("targets=").append(String.join(",", targets)).append("\n");
        }
        
        // Serialize tags
        if (!tags.isEmpty()) {
            sb.append("tags=").append(String.join(",", tags)).append("\n");
        }
        
        // Serialize vulnerability tree
        sb.append(vulnerabilities.serialize());
        
        sb.append("PROJECT_END\n");
        
        return sb.toString();
    }
    
    /**
     * Deserializes a project from string data.
     * 
     * @param data The serialized project data
     * @return The deserialized Project object
     * @throws IllegalArgumentException if the data format is invalid
     */
    public static Project deserialize(String data) {
        if (data == null || !data.contains("PROJECT_START") || !data.contains("PROJECT_END")) {
            throw new IllegalArgumentException("Invalid project data format");
        }
        
        String[] lines = data.split("\n");
        String name = null;
        String description = null, scope = null, createdBy = null, notes = null;
        String status = "ACTIVE";
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime lastModified = LocalDateTime.now();
        Set<String> targets = new HashSet<>();
        List<String> tags = new ArrayList<>();
        
        // Extract tree data
        int treeStart = data.indexOf("TREE_START");
        int treeEnd = data.indexOf("TREE_END") + "TREE_END".length();
        VulnerabilityTree tree = null;
        
        if (treeStart != -1 && treeEnd > treeStart) {
            String treeData = data.substring(treeStart, treeEnd);
            try {
                tree = VulnerabilityTree.deserialize(treeData);
            } catch (Exception e) {
                System.err.println("Warning: Failed to deserialize vulnerability tree: " + e.getMessage());
            }
        }
        
        // Parse project metadata
        for (String line : lines) {
            if (line.trim().isEmpty() || line.startsWith("TREE_") || 
                line.startsWith("VULN_") || line.equals("PROJECT_START") || 
                line.equals("PROJECT_END")) {
                continue;
            }
            
            int equalPos = line.indexOf('=');
            if (equalPos == -1) continue;
            
            String key = line.substring(0, equalPos).trim();
            String value = line.substring(equalPos + 1).trim();
            
            switch (key) {
                case "name": name = value; break;
                case "description": description = value.replace("\\n", "\n"); break;
                case "scope": scope = value.replace("\\n", "\n"); break;
                case "status": status = value; break;
                case "createdBy": createdBy = value; break;
                case "notes": notes = value.replace("\\n", "\n"); break;
                case "createdAt":
                    try {
                        createdAt = LocalDateTime.parse(value, DATE_FORMAT);
                    } catch (Exception e) {
                        createdAt = LocalDateTime.now();
                    }
                    break;
                case "lastModified":
                    try {
                        lastModified = LocalDateTime.parse(value, DATE_FORMAT);
                    } catch (Exception e) {
                        lastModified = LocalDateTime.now();
                    }
                    break;
                case "targets":
                    if (!value.isEmpty()) {
                        String[] targetArray = value.split(",");
                        for (String target : targetArray) {
                            targets.add(target.trim());
                        }
                    }
                    break;
                case "tags":
                    if (!value.isEmpty()) {
                        String[] tagArray = value.split(",");
                        for (String tag : tagArray) {
                            tags.add(tag.trim());
                        }
                    }
                    break;
            }
        }
        
        if (name == null) {
            throw new IllegalArgumentException("Missing project name in data");
        }
        
        // Create project
        Project project = new Project(name);
        project.setDescription(description);
        project.setScope(scope);
        project.setStatus(status);
        project.setCreatedBy(createdBy);
        project.setNotes(notes);
        project.setCreatedAt(createdAt);
        project.setLastModified(lastModified);
        
        // Add targets and tags
        for (String target : targets) {
            project.addTarget(target);
        }
        for (String tag : tags) {
            project.addTag(tag);
        }
        
        // Replace vulnerability tree if successfully deserialized
        if (tree != null) {
            project.vulnerabilities = tree;
        }
        
        return project;
    }
    
    @Override
    public String toString() {
        return String.format("Project{name='%s', status='%s', targets=%d, vulnerabilities=%d}", 
                           name, status, targets.size(), getTotalVulnerabilityCount());
    }
}