package com.example;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

/**
 * Manages pentesting projects with file-based persistence.
 * 
 * This class provides comprehensive project management capabilities including
 * creation, loading, saving, and querying of pentesting projects. All project
 * data is persisted to a configurable text file for durability.
 * 
 * @author Agent-Orange Team
 * @version 1.0
 */
public class ProjectManager {
    private static final Logger logger = Logger.getLogger(ProjectManager.class.getName());
    private static final String DEFAULT_DATA_FILE = "pentest_projects.txt";
    private static final String CONFIG_PROPERTY = "project.data.file";
    private static final String ENV_VAR = "AGENT_ORANGE_PROJECT_FILE";
    
    private final String dataFilePath;
    private final Map<String, Project> projects;
    private final ReentrantReadWriteLock lock;
    private Project currentProject;
    private boolean autoSave;
    
    /**
     * Constructs a ProjectManager with default settings.
     * 
     * The data file location is determined by:
     * 1. System property "project.data.file"
     * 2. Environment variable "AGENT_ORANGE_PROJECT_FILE"
     * 3. Default: "pentest_projects.txt" in current directory
     */
    public ProjectManager() {
        this(determineDataFilePath());
    }
    
    /**
     * Constructs a ProjectManager with a specific data file path.
     * 
     * @param dataFilePath The path to the data file for project persistence
     * @throws IllegalArgumentException if dataFilePath is null or empty
     */
    public ProjectManager(String dataFilePath) {
        if (dataFilePath == null || dataFilePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Data file path cannot be null or empty");
        }
        
        this.dataFilePath = dataFilePath.trim();
        this.projects = new ConcurrentHashMap<>();
        this.lock = new ReentrantReadWriteLock();
        this.autoSave = true;
        
        // Create directory if it doesn't exist
        try {
            Path path = Paths.get(this.dataFilePath);
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            logger.warning("Failed to create parent directories for data file: " + e.getMessage());
        }
        
        // Load existing projects
        loadProjects();
        
        logger.info("ProjectManager initialized with data file: " + this.dataFilePath);
    }
    
    /**
     * Determines the data file path based on configuration hierarchy.
     * 
     * @return The determined data file path
     */
    private static String determineDataFilePath() {
        // Priority 1: System property
        String path = System.getProperty(CONFIG_PROPERTY);
        if (path != null && !path.trim().isEmpty()) {
            return path.trim();
        }
        
        // Priority 2: Environment variable
        path = System.getenv(ENV_VAR);
        if (path != null && !path.trim().isEmpty()) {
            return path.trim();
        }
        
        // Priority 3: Configuration file
        try (InputStream input = new FileInputStream("application.properties")) {
            Properties props = new Properties();
            props.load(input);
            path = props.getProperty("project.data.file");
            if (path != null && !path.trim().isEmpty()) {
                return path.trim();
            }
        } catch (IOException e) {
            // File not found or unreadable - continue to default
        }
        
        // Priority 4: Default
        return DEFAULT_DATA_FILE;
    }
    
    /**
     * Gets the data file path being used for persistence.
     * 
     * @return The data file path
     */
    public String getDataFilePath() {
        return dataFilePath;
    }
    
    /**
     * Checks if auto-save is enabled.
     * 
     * @return true if auto-save is enabled, false otherwise
     */
    public boolean isAutoSaveEnabled() {
        return autoSave;
    }
    
    /**
     * Enables or disables automatic saving after project modifications.
     * 
     * @param autoSave true to enable auto-save, false to disable
     */
    public void setAutoSave(boolean autoSave) {
        this.autoSave = autoSave;
        logger.info("Auto-save " + (autoSave ? "enabled" : "disabled"));
    }
    
    /**
     * Creates a new project.
     * 
     * @param projectName The name of the new project
     * @return The created project
     * @throws IllegalArgumentException if project name is invalid or already exists
     */
    public Project createProject(String projectName) {
        if (projectName == null || projectName.trim().isEmpty()) {
            throw new IllegalArgumentException("Project name cannot be null or empty");
        }
        
        String normalizedName = projectName.trim();
        
        lock.writeLock().lock();
        try {
            if (projects.containsKey(normalizedName)) {
                throw new IllegalArgumentException("Project '" + normalizedName + "' already exists");
            }
            
            Project project = new Project(normalizedName);
            project.setCreatedBy(System.getProperty("user.name", "unknown"));
            
            projects.put(normalizedName, project);
            currentProject = project;
            
            if (autoSave) {
                saveProjects();
            }
            
            logger.info("Created new project: " + normalizedName);
            return project;
            
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Opens an existing project and sets it as the current project.
     * 
     * @param projectName The name of the project to open
     * @return The opened project
     * @throws IllegalArgumentException if project doesn't exist
     */
    public Project openProject(String projectName) {
        if (projectName == null || projectName.trim().isEmpty()) {
            throw new IllegalArgumentException("Project name cannot be null or empty");
        }
        
        String normalizedName = projectName.trim();
        
        lock.readLock().lock();
        try {
            Project project = projects.get(normalizedName);
            if (project == null) {
                throw new IllegalArgumentException("Project '" + normalizedName + "' not found");
            }
            
            currentProject = project;
            logger.info("Opened project: " + normalizedName);
            return project;
            
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Gets the currently active project.
     * 
     * @return The current project or null if none is active
     */
    public Project getCurrentProject() {
        lock.readLock().lock();
        try {
            return currentProject;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Closes the current project.
     * 
     * @return true if a project was closed, false if no project was active
     */
    public boolean closeCurrentProject() {
        lock.writeLock().lock();
        try {
            if (currentProject != null) {
                String projectName = currentProject.getName();
                
                if (autoSave) {
                    saveProjects();
                }
                
                currentProject = null;
                logger.info("Closed project: " + projectName);
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Gets a project by name without opening it.
     * 
     * @param projectName The name of the project to get
     * @return The project or null if not found
     */
    public Project getProject(String projectName) {
        if (projectName == null || projectName.trim().isEmpty()) {
            return null;
        }
        
        lock.readLock().lock();
        try {
            return projects.get(projectName.trim());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Gets all project names.
     * 
     * @return A sorted list of project names
     */
    public List<String> getProjectNames() {
        lock.readLock().lock();
        try {
            List<String> names = new ArrayList<>(projects.keySet());
            Collections.sort(names);
            return names;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Gets all projects.
     * 
     * @return A list of all projects
     */
    public List<Project> getAllProjects() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(projects.values());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Deletes a project.
     * 
     * @param projectName The name of the project to delete
     * @return true if the project was deleted, false if not found
     */
    public boolean deleteProject(String projectName) {
        if (projectName == null || projectName.trim().isEmpty()) {
            return false;
        }
        
        String normalizedName = projectName.trim();
        
        lock.writeLock().lock();
        try {
            Project removed = projects.remove(normalizedName);
            if (removed != null) {
                // Close project if it was current
                if (currentProject != null && currentProject.getName().equals(normalizedName)) {
                    currentProject = null;
                }
                
                if (autoSave) {
                    saveProjects();
                }
                
                logger.info("Deleted project: " + normalizedName);
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Adds a target to the current project.
     * 
     * @param target The target to add
     * @throws IllegalStateException if no project is currently open
     * @throws IllegalArgumentException if target is invalid
     */
    public void addTargetToCurrentProject(String target) {
        lock.writeLock().lock();
        try {
            if (currentProject == null) {
                throw new IllegalStateException("No project is currently open");
            }
            
            currentProject.addTarget(target);
            
            if (autoSave) {
                saveProjects();
            }
            
            logger.info("Added target '" + target + "' to project '" + currentProject.getName() + "'");
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Adds a vulnerability to the current project.
     * 
     * @param vulnerability The vulnerability to add
     * @throws IllegalStateException if no project is currently open
     * @throws IllegalArgumentException if vulnerability is invalid
     */
    public void addVulnerabilityToCurrentProject(Vulnerability vulnerability) {
        lock.writeLock().lock();
        try {
            if (currentProject == null) {
                throw new IllegalStateException("No project is currently open");
            }
            
            currentProject.addVulnerability(vulnerability);
            
            if (autoSave) {
                saveProjects();
            }
            
            logger.info("Added vulnerability '" + vulnerability.getName() + 
                       "' to project '" + currentProject.getName() + "'");
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Creates and adds a vulnerability to the current project.
     * 
     * @param id Unique vulnerability ID
     * @param name Vulnerability name
     * @param description Vulnerability description
     * @param severity Severity level
     * @param target Target system
     * @param service Service (optional)
     * @param path Path (optional)
     * @return The created vulnerability
     * @throws IllegalStateException if no project is currently open
     */
    public Vulnerability createVulnerabilityForCurrentProject(String id, String name, 
            String description, Severity severity, String target, String service, String path) {
        
        // Use CVE naming convention when creating vulnerabilities
        Vulnerability vulnerability = Vulnerability.createWithCveConvention(id, name, description, severity, target, null);
        
        if (service != null && !service.trim().isEmpty()) {
            vulnerability.setService(service);
        }
        if (path != null && !path.trim().isEmpty()) {
            vulnerability.setPath(path);
        }
        
        addVulnerabilityToCurrentProject(vulnerability);
        return vulnerability;
    }
    
    /**
     * Finds projects by tag.
     * 
     * @param tag The tag to search for
     * @return A list of projects with the specified tag
     */
    public List<Project> findProjectsByTag(String tag) {
        if (tag == null || tag.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        lock.readLock().lock();
        try {
            return projects.values().stream()
                    .filter(project -> project.hasTag(tag.trim()))
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Finds projects by status.
     * 
     * @param status The status to search for
     * @return A list of projects with the specified status
     */
    public List<Project> findProjectsByStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        String normalizedStatus = status.trim().toUpperCase();
        
        lock.readLock().lock();
        try {
            return projects.values().stream()
                    .filter(project -> normalizedStatus.equals(project.getStatus()))
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Gets summary statistics for all projects.
     * 
     * @return A formatted summary of project statistics
     */
    public String getProjectStatistics() {
        lock.readLock().lock();
        try {
            int totalProjects = projects.size();
            int totalVulnerabilities = 0;
            int totalTargets = 0;
            Map<String, Integer> statusCounts = new HashMap<>();
            Map<Severity, Integer> severityCounts = new HashMap<>();
            
            for (Project project : projects.values()) {
                totalVulnerabilities += project.getTotalVulnerabilityCount();
                totalTargets += project.getTargets().size();
                
                String status = project.getStatus();
                statusCounts.put(status, statusCounts.getOrDefault(status, 0) + 1);
                
                for (Vulnerability vuln : project.getAllVulnerabilities()) {
                    Severity severity = vuln.getSeverity();
                    severityCounts.put(severity, severityCounts.getOrDefault(severity, 0) + 1);
                }
            }
            
            StringBuilder stats = new StringBuilder();
            stats.append("📊 Project Statistics\n");
            stats.append("=".repeat(30)).append("\n");
            stats.append("Total Projects: ").append(totalProjects).append("\n");
            stats.append("Total Targets: ").append(totalTargets).append("\n");
            stats.append("Total Vulnerabilities: ").append(totalVulnerabilities).append("\n\n");
            
            if (!statusCounts.isEmpty()) {
                stats.append("📈 Projects by Status:\n");
                statusCounts.entrySet().stream()
                        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                        .forEach(entry -> stats.append("  ").append(entry.getKey())
                                .append(": ").append(entry.getValue()).append("\n"));
                stats.append("\n");
            }
            
            if (!severityCounts.isEmpty()) {
                stats.append("🔍 Vulnerabilities by Severity:\n");
                for (Severity severity : Severity.values()) {
                    int count = severityCounts.getOrDefault(severity, 0);
                    if (count > 0) {
                        stats.append("  ").append(severity.getFormattedName())
                              .append(": ").append(count).append("\n");
                    }
                }
            }
            
            return stats.toString();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Saves all projects to the data file.
     * 
     * @throws RuntimeException if saving fails
     */
    public void saveProjects() {
        lock.readLock().lock();
        try {
            StringBuilder data = new StringBuilder();
            
            // File header
            data.append("# Agent-Orange Pentest Projects Data File\n");
            data.append("# Generated: ").append(LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
            data.append("# Total Projects: ").append(projects.size()).append("\n");
            data.append("\n");
            
            // Serialize each project
            for (Project project : projects.values()) {
                data.append(project.serialize()).append("\n");
            }
            
            // Write to file
            Files.write(Paths.get(dataFilePath), data.toString().getBytes(StandardCharsets.UTF_8),
                       StandardOpenOption.CREATE, StandardOpenOption.WRITE, 
                       StandardOpenOption.TRUNCATE_EXISTING);
            
            logger.info("Saved " + projects.size() + " projects to " + dataFilePath);
            
        } catch (IOException e) {
            String error = "Failed to save projects to " + dataFilePath + ": " + e.getMessage();
            logger.severe(error);
            throw new RuntimeException(error, e);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * Loads projects from the data file.
     * 
     * This method is called automatically during initialization.
     */
    public void loadProjects() {
        lock.writeLock().lock();
        try {
            Path path = Paths.get(dataFilePath);
            if (!Files.exists(path)) {
                logger.info("Data file does not exist, starting with empty project list: " + dataFilePath);
                return;
            }
            
            String content = Files.readString(path, StandardCharsets.UTF_8);
            if (content.trim().isEmpty()) {
                logger.info("Data file is empty: " + dataFilePath);
                return;
            }
            
            // Split content into individual project blocks
            String[] projectBlocks = content.split("(?=PROJECT_START)");
            int loadedCount = 0;
            int errorCount = 0;
            
            for (String block : projectBlocks) {
                if (block.trim().isEmpty() || !block.contains("PROJECT_START")) {
                    continue;
                }
                
                try {
                    Project project = Project.deserialize(block);
                    projects.put(project.getName(), project);
                    loadedCount++;
                } catch (Exception e) {
                    errorCount++;
                    logger.warning("Failed to load project from block: " + e.getMessage());
                }
            }
            
            logger.info("Loaded " + loadedCount + " projects from " + dataFilePath + 
                       (errorCount > 0 ? " (with " + errorCount + " errors)" : ""));
            
        } catch (IOException e) {
            String error = "Failed to load projects from " + dataFilePath + ": " + e.getMessage();
            logger.warning(error);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Reloads projects from the data file, discarding any unsaved changes.
     * 
     * @return The number of projects loaded
     */
    public int reloadProjects() {
        lock.writeLock().lock();
        try {
            projects.clear();
            currentProject = null;
            loadProjects();
            return projects.size();
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * Creates a backup of the current data file.
     * 
     * @return The path to the backup file
     * @throws IOException if backup creation fails
     */
    public String createBackup() throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String backupPath = dataFilePath + ".backup." + timestamp;
        
        Path sourcePath = Paths.get(dataFilePath);
        Path backupFile = Paths.get(backupPath);
        
        if (Files.exists(sourcePath)) {
            Files.copy(sourcePath, backupFile, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Created backup: " + backupPath);
        } else {
            logger.warning("No data file to backup: " + dataFilePath);
        }
        
        return backupPath;
    }
    
    /**
     * Validates the integrity of the data file.
     * 
     * @return A validation report
     */
    public String validateDataFile() {
        try {
            Path path = Paths.get(dataFilePath);
            if (!Files.exists(path)) {
                return "❌ Data file does not exist: " + dataFilePath;
            }
            
            String content = Files.readString(path, StandardCharsets.UTF_8);
            String[] projectBlocks = content.split("(?=PROJECT_START)");
            
            int validProjects = 0;
            int invalidProjects = 0;
            List<String> errors = new ArrayList<>();
            
            for (String block : projectBlocks) {
                if (block.trim().isEmpty() || !block.contains("PROJECT_START")) {
                    continue;
                }
                
                try {
                    Project.deserialize(block);
                    validProjects++;
                } catch (Exception e) {
                    invalidProjects++;
                    errors.add("Invalid project block: " + e.getMessage());
                }
            }
            
            StringBuilder report = new StringBuilder();
            report.append("📋 Data File Validation Report\n");
            report.append("File: ").append(dataFilePath).append("\n");
            report.append("Valid Projects: ").append(validProjects).append("\n");
            report.append("Invalid Projects: ").append(invalidProjects).append("\n");
            
            if (!errors.isEmpty()) {
                report.append("\n❌ Errors:\n");
                for (String error : errors) {
                    report.append("  - ").append(error).append("\n");
                }
            }
            
            if (invalidProjects == 0) {
                report.append("\n✅ Data file is valid");
            }
            
            return report.toString();
            
        } catch (IOException e) {
            return "❌ Failed to validate data file: " + e.getMessage();
        }
    }
    
    /**
     * Shuts down the ProjectManager, saving any unsaved changes.
     */
    public void shutdown() {
        lock.writeLock().lock();
        try {
            if (autoSave && !projects.isEmpty()) {
                saveProjects();
            }
            logger.info("ProjectManager shutdown complete");
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public String toString() {
        lock.readLock().lock();
        try {
            return String.format("ProjectManager{dataFile='%s', projects=%d, current='%s'}", 
                               dataFilePath, projects.size(), 
                               currentProject != null ? currentProject.getName() : "none");
        } finally {
            lock.readLock().unlock();
        }
    }
}