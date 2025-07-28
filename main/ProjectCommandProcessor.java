package com.example;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Logger;

/**
 * Processes natural language commands for project management.
 * 
 * This class provides natural language understanding for project-related
 * commands, parsing user input and translating it into appropriate
 * ProjectManager operations.
 * 
 * @author Agent-Orange Team
 * @version 1.0
 */
public class ProjectCommandProcessor {
    private static final Logger logger = Logger.getLogger(ProjectCommandProcessor.class.getName());
    
    private final ProjectManager projectManager;
    private final Map<String, CommandHandler> commandHandlers;
    
    // Command patterns for natural language processing
    private static final Map<String, Pattern> COMMAND_PATTERNS = new HashMap<>();
    
    static {
        // Project creation patterns
        COMMAND_PATTERNS.put("CREATE_PROJECT", Pattern.compile(
            "(?:create|new|start)\\s+(?:project|engagement)\\s+(?:named?|called?)\\s*['\"]?([^'\"]+?)['\"]?(?:\\s|$)",
            Pattern.CASE_INSENSITIVE));
        
        // Project opening patterns
        COMMAND_PATTERNS.put("OPEN_PROJECT", Pattern.compile(
            "(?:open|load|switch\\s+to)\\s+(?:project|engagement)?\\s*['\"]?([^'\"]+?)['\"]?(?:\\s|$)",
            Pattern.CASE_INSENSITIVE));
        
        // Project listing patterns
        COMMAND_PATTERNS.put("LIST_PROJECTS", Pattern.compile(
            "(?:list|show|display)\\s+(?:all\\s+)?(?:projects|engagements)(?:\\s|$)",
            Pattern.CASE_INSENSITIVE));
        
        // Target addition patterns
        COMMAND_PATTERNS.put("ADD_TARGET", Pattern.compile(
            "(?:add|include)\\s+(?:target|host)\\s+([\\w\\.-]+)(?:\\s+to\\s+(?:project\\s+)?['\"]?([^'\"]+?)['\"]?)?(?:\\s|$)",
            Pattern.CASE_INSENSITIVE));
        
        // Vulnerability addition patterns (complex)
        COMMAND_PATTERNS.put("ADD_VULNERABILITY", Pattern.compile(
            "(?:add|found|create)\\s+(?:vulnerability|vuln|finding)\\s+([^\\s]+(?:\\s+[^\\s]+)*)(?:\\s+(?:to|on)\\s+(?:target\\s+)?([\\w\\.-]+))?(?:\\s+(?:at|in)\\s+(?:path\\s+)?['\"]?([^'\"]+?)['\"]?)?(?:\\s+(?:with\\s+)?(?:severity\\s+)?([\\w]+))?(?:\\s|$)",
            Pattern.CASE_INSENSITIVE));
        
        // Vulnerability listing patterns
        COMMAND_PATTERNS.put("LIST_VULNERABILITIES", Pattern.compile(
            "(?:list|show|display)\\s+(?:vulnerabilities|vulns|findings)(?:\\s+(?:for|on|in)\\s+(?:target\\s+)?([\\w\\.-]+))?(?:\\s+(?:in\\s+)?(?:project\\s+)?['\"]?([^'\"]+?)['\"]?)?(?:\\s|$)",
            Pattern.CASE_INSENSITIVE));
        
        // CVE-specific query patterns
        COMMAND_PATTERNS.put("SHOW_CVE", Pattern.compile(
            "(?:show|display|get)\\s+(?:details\\s+(?:for|of)\\s+)?(CVE-\\d{4}-\\d{4,})(?:\\s+(?:in\\s+)?(?:project\\s+)?['\"]?([^'\"]+?)['\"]?)?(?:\\s|$)",
            Pattern.CASE_INSENSITIVE));
        
        // Search vulnerabilities by name/CVE patterns
        COMMAND_PATTERNS.put("SEARCH_VULNERABILITY", Pattern.compile(
            "(?:search|find|lookup)\\s+(?:vulnerability|vuln|finding|cve)\\s+['\"]?([^'\"]+?)['\"]?(?:\\s+(?:in\\s+)?(?:project\\s+)?['\"]?([^'\"]+?)['\"]?)?(?:\\s|$)",
            Pattern.CASE_INSENSITIVE));
        
        // Report generation patterns
        COMMAND_PATTERNS.put("GENERATE_REPORT", Pattern.compile(
            "(?:generate|create|show)\\s+(?:vulnerability\\s+)?(?:report|summary)(?:\\s+for\\s+(?:project\\s+)?['\"]?([^'\"]+?)['\"]?)?(?:\\s|$)",
            Pattern.CASE_INSENSITIVE));
        
        // Project closing patterns
        COMMAND_PATTERNS.put("CLOSE_PROJECT", Pattern.compile(
            "(?:close|finish|end)\\s+(?:project|engagement)(?:\\s+['\"]?([^'\"]+?)['\"]?)?(?:\\s|$)",
            Pattern.CASE_INSENSITIVE));
        
        // Project saving patterns
        COMMAND_PATTERNS.put("SAVE_PROJECT", Pattern.compile(
            "(?:save|persist)\\s+(?:current\\s+)?(?:project|changes)(?:\\s|$)",
            Pattern.CASE_INSENSITIVE));
        
        // Project status patterns
        COMMAND_PATTERNS.put("PROJECT_STATUS", Pattern.compile(
            "(?:show|display|get)\\s+(?:project\\s+)?(?:status|info|summary)(?:\\s+(?:for\\s+)?['\"]?([^'\"]+?)['\"]?)?(?:\\s|$)",
            Pattern.CASE_INSENSITIVE));
        
        // Help patterns
        COMMAND_PATTERNS.put("HELP", Pattern.compile(
            "(?:help|\\?|commands?)(?:\\s+(?:with\\s+)?(?:projects?|management))?(?:\\s|$)",
            Pattern.CASE_INSENSITIVE));
    }
    
    /**
     * Functional interface for command handlers.
     */
    @FunctionalInterface
    private interface CommandHandler {
        String execute(String input, Matcher matcher);
    }
    
    /**
     * Constructs a ProjectCommandProcessor with the specified ProjectManager.
     * 
     * @param projectManager The ProjectManager to use for operations
     * @throws IllegalArgumentException if projectManager is null
     */
    public ProjectCommandProcessor(ProjectManager projectManager) {
        if (projectManager == null) {
            throw new IllegalArgumentException("ProjectManager cannot be null");
        }
        
        this.projectManager = projectManager;
        this.commandHandlers = new HashMap<>();
        
        initializeCommandHandlers();
        
        logger.info("ProjectCommandProcessor initialized");
    }
    
    /**
     * Initializes command handlers for each supported command type.
     */
    private void initializeCommandHandlers() {
        commandHandlers.put("CREATE_PROJECT", this::handleCreateProject);
        commandHandlers.put("OPEN_PROJECT", this::handleOpenProject);
        commandHandlers.put("LIST_PROJECTS", this::handleListProjects);
        commandHandlers.put("ADD_TARGET", this::handleAddTarget);
        commandHandlers.put("ADD_VULNERABILITY", this::handleAddVulnerability);
        commandHandlers.put("LIST_VULNERABILITIES", this::handleListVulnerabilities);
        commandHandlers.put("SHOW_CVE", this::handleShowCve);
        commandHandlers.put("SEARCH_VULNERABILITY", this::handleSearchVulnerability);
        commandHandlers.put("GENERATE_REPORT", this::handleGenerateReport);
        commandHandlers.put("CLOSE_PROJECT", this::handleCloseProject);
        commandHandlers.put("SAVE_PROJECT", this::handleSaveProject);
        commandHandlers.put("PROJECT_STATUS", this::handleProjectStatus);
        commandHandlers.put("HELP", this::handleHelp);
    }
    
    /**
     * Processes a natural language command and returns the result.
     * 
     * @param input The natural language input from the user
     * @return A response string describing the result of the command
     */
    public String processCommand(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "❓ Please provide a command. Type 'help' for available project commands.";
        }
        
        String cleanInput = input.trim();
        logger.fine("Processing command: " + cleanInput);
        
        // Try to match against each command pattern
        for (Map.Entry<String, Pattern> entry : COMMAND_PATTERNS.entrySet()) {
            String commandType = entry.getKey();
            Pattern pattern = entry.getValue();
            Matcher matcher = pattern.matcher(cleanInput);
            
            if (matcher.find()) {
                logger.fine("Matched command type: " + commandType);
                try {
                    CommandHandler handler = commandHandlers.get(commandType);
                    if (handler != null) {
                        return handler.execute(cleanInput, matcher);
                    }
                } catch (Exception e) {
                    logger.warning("Error executing command: " + e.getMessage());
                    return "❌ Error executing command: " + e.getMessage();
                }
            }
        }
        
        // No pattern matched - try to provide helpful suggestions
        return generateSuggestions(cleanInput);
    }
    
    /**
     * Handles project creation commands.
     */
    private String handleCreateProject(String input, Matcher matcher) {
        String projectName = matcher.group(1);
        if (projectName == null || projectName.trim().isEmpty()) {
            return "❌ Please specify a project name. Example: 'create project named \"Web App Audit\"'";
        }
        
        try {
            Project project = projectManager.createProject(projectName.trim());
            return String.format("✅ Created new project: '%s'\n📋 Project is now active and ready for targets and vulnerabilities.", 
                                project.getName());
        } catch (IllegalArgumentException e) {
            return "❌ " + e.getMessage();
        }
    }
    
    /**
     * Handles project opening commands.
     */
    private String handleOpenProject(String input, Matcher matcher) {
        String projectName = matcher.group(1);
        if (projectName == null || projectName.trim().isEmpty()) {
            List<String> projectNames = projectManager.getProjectNames();
            if (projectNames.isEmpty()) {
                return "❌ No projects available. Create a new project first.";
            }
            return "❌ Please specify which project to open:\n📁 Available projects: " + 
                   String.join(", ", projectNames);
        }
        
        try {
            Project project = projectManager.openProject(projectName.trim());
            return String.format("✅ Opened project: '%s'\n📊 Status: %s | Targets: %d | Vulnerabilities: %d", 
                                project.getName(), project.getStatus(), 
                                project.getTargets().size(), project.getTotalVulnerabilityCount());
        } catch (IllegalArgumentException e) {
            return "❌ " + e.getMessage();
        }
    }
    
    /**
     * Handles project listing commands.
     */
    private String handleListProjects(String input, Matcher matcher) {
        List<String> projectNames = projectManager.getProjectNames();
        
        if (projectNames.isEmpty()) {
            return "📁 No projects found. Create your first project with: 'create project named \"My Project\"'";
        }
        
        StringBuilder response = new StringBuilder();
        response.append("📁 Available Projects (").append(projectNames.size()).append("):\n\n");
        
        Project currentProject = projectManager.getCurrentProject();
        String currentProjectName = (currentProject != null) ? currentProject.getName() : null;
        
        for (String name : projectNames) {
            Project project = projectManager.getProject(name);
            if (project != null) {
                boolean isCurrent = name.equals(currentProjectName);
                response.append(isCurrent ? "▶️ " : "📄 ")
                        .append(name)
                        .append(" (").append(project.getStatus()).append(")")
                        .append(" - ").append(project.getTargets().size()).append(" targets, ")
                        .append(project.getTotalVulnerabilityCount()).append(" vulnerabilities");
                
                if (isCurrent) {
                    response.append(" [CURRENT]");
                }
                response.append("\n");
            }
        }
        
        return response.toString();
    }
    
    /**
     * Handles target addition commands.
     */
    private String handleAddTarget(String input, Matcher matcher) {
        String target = matcher.group(1);
        String projectName = matcher.groupCount() > 1 ? matcher.group(2) : null;
        
        if (target == null || target.trim().isEmpty()) {
            return "❌ Please specify a target. Example: 'add target 192.168.1.100'";
        }
        
        try {
            // If project name specified, open that project first
            if (projectName != null && !projectName.trim().isEmpty()) {
                projectManager.openProject(projectName.trim());
            }
            
            projectManager.addTargetToCurrentProject(target.trim());
            Project current = projectManager.getCurrentProject();
            
            return String.format("✅ Added target '%s' to project '%s'\n📊 Total targets: %d", 
                                target.trim(), current.getName(), current.getTargets().size());
        } catch (IllegalStateException | IllegalArgumentException e) {
            return "❌ " + e.getMessage();
        }
    }
    
    /**
     * Handles vulnerability addition commands.
     */
    private String handleAddVulnerability(String input, Matcher matcher) {
        String vulnName = matcher.group(1);
        String target = matcher.groupCount() > 1 ? matcher.group(2) : null;
        String path = matcher.groupCount() > 2 ? matcher.group(3) : null;
        String severityStr = matcher.groupCount() > 3 ? matcher.group(4) : null;
        
        if (vulnName == null || vulnName.trim().isEmpty()) {
            return "❌ Please specify a vulnerability name. Example: 'add vulnerability SQL Injection to 192.168.1.100'";
        }
        
        Project currentProject = projectManager.getCurrentProject();
        if (currentProject == null) {
            return "❌ No project is currently open. Open a project first or create a new one.";
        }
        
        // Determine target
        if (target == null || target.trim().isEmpty()) {
            Set<String> targets = currentProject.getTargets();
            if (targets.isEmpty()) {
                return "❌ No targets specified and no targets in current project. Add a target first.";
            } else if (targets.size() == 1) {
                target = targets.iterator().next();
            } else {
                return "❌ Multiple targets in project. Please specify which target: " + 
                       String.join(", ", targets);
            }
        }
        
        // Parse severity
        Severity severity = Severity.MEDIUM; // Default
        if (severityStr != null && !severityStr.trim().isEmpty()) {
            try {
                severity = Severity.fromString(severityStr);
            } catch (IllegalArgumentException e) {
                return "❌ Invalid severity '" + severityStr + "'. Valid options: CRITICAL, HIGH, MEDIUM, LOW, INFO";
            }
        }
        
        try {
            // Generate a simple ID
            String id = "VULN-" + System.currentTimeMillis() % 100000;
            
            Vulnerability vulnerability = projectManager.createVulnerabilityForCurrentProject(
                id, vulnName.trim(), "Vulnerability found during penetration test", 
                severity, target.trim(), null, path);
            
            return String.format("✅ Added %s vulnerability '%s' to target '%s'%s\n📊 Total vulnerabilities: %d", 
                                severity.getFormattedName(), vulnName.trim(), target.trim(),
                                path != null ? " at path '" + path + "'" : "",
                                currentProject.getTotalVulnerabilityCount());
        } catch (Exception e) {
            return "❌ Error adding vulnerability: " + e.getMessage();
        }
    }
    
    /**
     * Handles vulnerability listing commands.
     */
    private String handleListVulnerabilities(String input, Matcher matcher) {
        String target = matcher.group(1);
        String projectName = matcher.groupCount() > 1 ? matcher.group(2) : null;
        
        Project project = projectManager.getCurrentProject();
        
        // If project name specified, use that project
        if (projectName != null && !projectName.trim().isEmpty()) {
            project = projectManager.getProject(projectName.trim());
            if (project == null) {
                return "❌ Project '" + projectName + "' not found.";
            }
        }
        
        if (project == null) {
            return "❌ No project specified and no current project. Open a project first.";
        }
        
        List<Vulnerability> vulnerabilities;
        String targetInfo = "";
        
        if (target != null && !target.trim().isEmpty()) {
            vulnerabilities = project.getVulnerabilitiesForTarget(target.trim());
            targetInfo = " for target '" + target.trim() + "'";
        } else {
            vulnerabilities = project.getAllVulnerabilities();
        }
        
        if (vulnerabilities.isEmpty()) {
            return String.format("🔍 No vulnerabilities found%s in project '%s'", targetInfo, project.getName());
        }
        
        StringBuilder response = new StringBuilder();
        response.append(String.format("🔍 Vulnerabilities%s in project '%s' (%d found):\n\n", 
                                     targetInfo, project.getName(), vulnerabilities.size()));
        
        // Group by severity
        Map<Severity, List<Vulnerability>> bySeverity = new HashMap<>();
        for (Vulnerability vuln : vulnerabilities) {
            bySeverity.computeIfAbsent(vuln.getSeverity(), k -> new ArrayList<>()).add(vuln);
        }
        
        // Display by severity (critical first)
        for (Severity severity : Arrays.asList(Severity.CRITICAL, Severity.HIGH, Severity.MEDIUM, Severity.LOW, Severity.INFO)) {
            List<Vulnerability> vulnList = bySeverity.get(severity);
            if (vulnList != null && !vulnList.isEmpty()) {
                response.append(severity.getFormattedName()).append(" (").append(vulnList.size()).append("):\n");
                for (Vulnerability vuln : vulnList) {
                    response.append("  • ");
                    
                    // Use CVE-aware display formatting
                    String displayName = CveUtils.formatDisplayNameWithContext(vuln, false);
                    response.append(displayName);
                    
                    response.append(" (").append(vuln.getFullPath()).append(")\n");
                }
                response.append("\n");
            }
        }
        
        return response.toString();
    }
    
    /**
     * Handles CVE-specific query commands.
     * Searches for a specific CVE ID in the current or specified project.
     */
    private String handleShowCve(String input, Matcher matcher) {
        String cveId = matcher.group(1);
        String projectName = matcher.groupCount() > 1 ? matcher.group(2) : null;
        
        // Normalize the CVE ID
        String normalizedCveId = CveUtils.normalizeCveId(cveId);
        if (normalizedCveId == null) {
            return "❌ Invalid CVE ID format: " + cveId + ". Expected format: CVE-YYYY-NNNN";
        }
        
        Project project = projectManager.getCurrentProject();
        
        // If project name specified, use that project
        if (projectName != null && !projectName.trim().isEmpty()) {
            project = projectManager.getProject(projectName.trim());
            if (project == null) {
                return "❌ Project '" + projectName + "' not found.";
            }
        }
        
        if (project == null) {
            return "❌ No project specified and no current project. Open a project first.";
        }
        
        // Search for the CVE ID
        List<Vulnerability> allVulns = project.getAllVulnerabilities();
        Vulnerability foundVuln = null;
        
        for (Vulnerability vuln : allVulns) {
            if (normalizedCveId.equals(vuln.getCveId())) {
                foundVuln = vuln;
                break;
            }
        }
        
        if (foundVuln == null) {
            return String.format("🔍 CVE %s not found in project '%s'.\n\n" +
                                "💡 Use 'list vulnerabilities' to see all vulnerabilities in the project.", 
                                normalizedCveId, project.getName());
        }
        
        // Display detailed vulnerability information
        StringBuilder response = new StringBuilder();
        response.append("🔍 ").append(normalizedCveId).append(" Details\n");
        response.append("════════════════════════════════════\n");
        response.append("📁 Project: ").append(project.getName()).append("\n");
        response.append("🎯 Target: ").append(foundVuln.getTarget()).append("\n");
        response.append("🚨 Severity: ").append(foundVuln.getSeverity().getFormattedName()).append("\n");
        response.append("📍 Location: ").append(foundVuln.getFullPath()).append("\n");
        
        if (foundVuln.getService() != null) {
            response.append("⚙️ Service: ").append(foundVuln.getService()).append("\n");
        }
        
        response.append("📅 Discovered: ").append(foundVuln.getDiscoveredAt()).append("\n");
        
        if (foundVuln.getDiscoveredBy() != null) {
            response.append("👤 Discovered by: ").append(foundVuln.getDiscoveredBy()).append("\n");
        }
        
        response.append("✅ Verified: ").append(foundVuln.isVerified() ? "Yes" : "No").append("\n\n");
        
        response.append("📋 Description:\n").append(foundVuln.getDescription()).append("\n\n");
        
        if (foundVuln.getEvidence() != null && !foundVuln.getEvidence().trim().isEmpty()) {
            response.append("🔍 Evidence:\n").append(foundVuln.getEvidence()).append("\n\n");
        }
        
        List<String> remediation = foundVuln.getRemediation();
        if (!remediation.isEmpty()) {
            response.append("🛠️ Remediation:\n");
            for (String step : remediation) {
                response.append("  • ").append(step).append("\n");
            }
            response.append("\n");
        }
        
        if (foundVuln.getNotes() != null && !foundVuln.getNotes().trim().isEmpty()) {
            response.append("📝 Notes:\n").append(foundVuln.getNotes()).append("\n");
        }
        
        return response.toString();
    }
    
    /**
     * Handles vulnerability search commands.
     * Searches for vulnerabilities by name, CVE ID, or description.
     */
    private String handleSearchVulnerability(String input, Matcher matcher) {
        String searchTerm = matcher.group(1);
        String projectName = matcher.groupCount() > 1 ? matcher.group(2) : null;
        
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return "❌ Search term cannot be empty.";
        }
        
        Project project = projectManager.getCurrentProject();
        
        // If project name specified, use that project
        if (projectName != null && !projectName.trim().isEmpty()) {
            project = projectManager.getProject(projectName.trim());
            if (project == null) {
                return "❌ Project '" + projectName + "' not found.";
            }
        }
        
        if (project == null) {
            return "❌ No project specified and no current project. Open a project first.";
        }
        
        String normalizedSearchTerm = searchTerm.trim().toLowerCase();
        List<Vulnerability> allVulns = project.getAllVulnerabilities();
        List<Vulnerability> matches = new ArrayList<>();
        
        // Search in CVE IDs, names, and descriptions
        for (Vulnerability vuln : allVulns) {
            boolean isMatch = false;
            
            // Check CVE ID (exact match, case-insensitive)
            if (vuln.getCveId() != null && 
                vuln.getCveId().toLowerCase().contains(normalizedSearchTerm)) {
                isMatch = true;
            }
            
            // Check name (partial match, case-insensitive)
            if (!isMatch && vuln.getName() != null && 
                vuln.getName().toLowerCase().contains(normalizedSearchTerm)) {
                isMatch = true;
            }
            
            // Check description (partial match, case-insensitive)
            if (!isMatch && vuln.getDescription() != null && 
                vuln.getDescription().toLowerCase().contains(normalizedSearchTerm)) {
                isMatch = true;
            }
            
            if (isMatch) {
                matches.add(vuln);
            }
        }
        
        if (matches.isEmpty()) {
            return String.format("🔍 No vulnerabilities found matching '%s' in project '%s'.\n\n" +
                                "💡 Use 'list vulnerabilities' to see all vulnerabilities in the project.", 
                                searchTerm, project.getName());
        }
        
        StringBuilder response = new StringBuilder();
        response.append(String.format("🔍 Search Results for '%s' in project '%s' (%d found):\n\n", 
                                     searchTerm, project.getName(), matches.size()));
        
        // Group by severity
        Map<Severity, List<Vulnerability>> bySeverity = new HashMap<>();
        for (Vulnerability vuln : matches) {
            bySeverity.computeIfAbsent(vuln.getSeverity(), k -> new ArrayList<>()).add(vuln);
        }
        
        // Display by severity (critical first)
        for (Severity severity : Arrays.asList(Severity.CRITICAL, Severity.HIGH, Severity.MEDIUM, Severity.LOW, Severity.INFO)) {
            List<Vulnerability> vulnList = bySeverity.get(severity);
            if (vulnList != null && !vulnList.isEmpty()) {
                response.append(severity.getFormattedName()).append(" (").append(vulnList.size()).append("):\n");
                for (Vulnerability vuln : vulnList) {
                    response.append("  • ");
                    
                    // Use CVE-aware display formatting
                    String displayName = CveUtils.formatDisplayNameWithContext(vuln, true);
                    response.append(displayName).append("\n");
                    
                    // Show matched context if it's in description
                    if (vuln.getDescription() != null && 
                        vuln.getDescription().toLowerCase().contains(normalizedSearchTerm) &&
                        !vuln.getName().toLowerCase().contains(normalizedSearchTerm)) {
                        
                        String desc = vuln.getDescription();
                        int index = desc.toLowerCase().indexOf(normalizedSearchTerm);
                        if (index >= 0) {
                            int start = Math.max(0, index - 30);
                            int end = Math.min(desc.length(), index + normalizedSearchTerm.length() + 30);
                            String context = desc.substring(start, end);
                            if (start > 0) context = "..." + context;
                            if (end < desc.length()) context = context + "...";
                            response.append("    ↳ ").append(context).append("\n");
                        }
                    }
                }
                response.append("\n");
            }
        }
        
        return response.toString();
    }
    
    /**
     * Handles report generation commands.
     */
    private String handleGenerateReport(String input, Matcher matcher) {
        String projectName = matcher.group(1);
        
        Project project = projectManager.getCurrentProject();
        
        // If project name specified, use that project
        if (projectName != null && !projectName.trim().isEmpty()) {
            project = projectManager.getProject(projectName.trim());
            if (project == null) {
                return "❌ Project '" + projectName + "' not found.";
            }
        }
        
        if (project == null) {
            return "❌ No project specified and no current project. Open a project first.";
        }
        
        String report = project.generateVulnerabilityReport();
        return "📄 Vulnerability Report Generated:\n\n" + report;
    }
    
    /**
     * Handles project closing commands.
     */
    private String handleCloseProject(String input, Matcher matcher) {
        Project current = projectManager.getCurrentProject();
        if (current == null) {
            return "ℹ️ No project is currently open.";
        }
        
        String projectName = current.getName();
        boolean closed = projectManager.closeCurrentProject();
        
        if (closed) {
            return String.format("✅ Closed project '%s'\n💾 Project has been saved automatically.", projectName);
        } else {
            return "❌ Failed to close project.";
        }
    }
    
    /**
     * Handles project saving commands.
     */
    private String handleSaveProject(String input, Matcher matcher) {
        try {
            projectManager.saveProjects();
            Project current = projectManager.getCurrentProject();
            String projectInfo = (current != null) ? " (Current: " + current.getName() + ")" : "";
            return "✅ All projects saved successfully" + projectInfo;
        } catch (Exception e) {
            return "❌ Error saving projects: " + e.getMessage();
        }
    }
    
    /**
     * Handles project status commands.
     */
    private String handleProjectStatus(String input, Matcher matcher) {
        String projectName = matcher.group(1);
        
        Project project = projectManager.getCurrentProject();
        
        // If project name specified, use that project
        if (projectName != null && !projectName.trim().isEmpty()) {
            project = projectManager.getProject(projectName.trim());
            if (project == null) {
                return "❌ Project '" + projectName + "' not found.";
            }
        }
        
        if (project == null) {
            return "❌ No project specified and no current project. Open a project first.";
        }
        
        return project.generateProjectSummary();
    }
    
    /**
     * Handles help commands.
     */
    private String handleHelp(String input, Matcher matcher) {
        StringBuilder help = new StringBuilder();
        help.append("🛠️ Project Management Commands\n");
        help.append("=" .repeat(40)).append("\n\n");
        
        help.append("📋 **Project Operations:**\n");
        help.append("  • create project named \"Project Name\"\n");
        help.append("  • open project \"Project Name\"\n");
        help.append("  • list projects\n");
        help.append("  • close project\n");
        help.append("  • save project\n");
        help.append("  • show project status\n\n");
        
        help.append("🎯 **Target Management:**\n");
        help.append("  • add target 192.168.1.100\n");
        help.append("  • add target example.com to project \"My Project\"\n\n");
        
        help.append("🔍 **Vulnerability Management:**\n");
        help.append("  • add vulnerability \"SQL Injection\" to 192.168.1.100\n");
        help.append("  • add vulnerability \"CVE-2023-1234\" to 192.168.1.100 at path \"/login\" with severity high\n");
        help.append("  • list vulnerabilities\n");
        help.append("  • list vulnerabilities for 192.168.1.100\n");
        help.append("  • show CVE-2023-1234\n");
        help.append("  • search vulnerability \"injection\"\n");
        help.append("  • search cve \"2023\"\n\n");
        
        help.append("📄 **Reporting:**\n");
        help.append("  • generate report\n");
        help.append("  • generate report for project \"My Project\"\n\n");
        
        help.append("🔧 **Severity Levels:** CRITICAL, HIGH, MEDIUM, LOW, INFO\n");
        help.append("💡 **Tip:** Commands are flexible - try natural language variations!");
        
        return help.toString();
    }
    
    /**
     * Generates helpful suggestions when no command pattern matches.
     */
    private String generateSuggestions(String input) {
        String lowerInput = input.toLowerCase();
        
        StringBuilder suggestions = new StringBuilder();
        suggestions.append("❓ I didn't understand that command. Here are some suggestions:\n\n");
        
        // Analyze input for keywords and provide contextual help
        if (lowerInput.contains("project")) {
            suggestions.append("📋 **Project commands:**\n");
            suggestions.append("  • create project named \"My Project\"\n");
            suggestions.append("  • open project \"My Project\"\n");
            suggestions.append("  • list projects\n\n");
        }
        
        if (lowerInput.contains("target") || lowerInput.contains("host") || 
            lowerInput.matches(".*\\d+\\.\\d+\\.\\d+\\.\\d+.*")) {
            suggestions.append("🎯 **Target commands:**\n");
            suggestions.append("  • add target 192.168.1.100\n");
            suggestions.append("  • add target example.com\n\n");
        }
        
        if (lowerInput.contains("vulner") || lowerInput.contains("finding") || 
            lowerInput.contains("exploit") || lowerInput.contains("security")) {
            suggestions.append("🔍 **Vulnerability commands:**\n");
            suggestions.append("  • add vulnerability \"SQL Injection\" to 192.168.1.100\n");
            suggestions.append("  • list vulnerabilities\n\n");
        }
        
        if (lowerInput.contains("report") || lowerInput.contains("summary")) {
            suggestions.append("📄 **Report commands:**\n");
            suggestions.append("  • generate report\n");
            suggestions.append("  • show project status\n\n");
        }
        
        suggestions.append("💡 Type 'help' for complete command reference.");
        
        return suggestions.toString();
    }
    
    /**
     * Checks if the input appears to be a project management command.
     * 
     * @param input The user input to check
     * @return true if it appears to be a project command, false otherwise
     */
    public boolean isProjectCommand(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }
        
        String lowerInput = input.toLowerCase();
        
        // Check for project-related keywords
        String[] projectKeywords = {
            "project", "engagement", "target", "vulnerabilit", "finding", 
            "create", "open", "close", "save", "list", "add", "show", 
            "generate", "report", "summary", "status"
        };
        
        for (String keyword : projectKeywords) {
            if (lowerInput.contains(keyword)) {
                return true;
            }
        }
        
        // Check if any command pattern matches
        for (Pattern pattern : COMMAND_PATTERNS.values()) {
            if (pattern.matcher(input).find()) {
                return true;
            }
        }
        
        return false;
    }
}