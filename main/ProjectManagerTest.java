package com.example;

import static org.junit.Assert.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Unit tests for the project management system.
 * 
 * Tests the core functionality of ProjectManager, Project, VulnerabilityTree,
 * and related classes to ensure proper project lifecycle management and
 * data persistence.
 * 
 * @author Agent-Orange Team
 * @version 1.0
 */
public class ProjectManagerTest {
    
    private ProjectManager projectManager;
    private String testDataFile = "test_projects.txt";
    
    @Before
    public void setUp() throws Exception {
        // Clean up any existing test data
        deleteTestFile();
        
        // Create a new project manager with test file
        projectManager = new ProjectManager(testDataFile);
        projectManager.setAutoSave(false); // Manual saving for tests
    }
    
    @After
    public void tearDown() throws Exception {
        if (projectManager != null) {
            projectManager.shutdown();
        }
        deleteTestFile();
    }
    
    private void deleteTestFile() {
        try {
            Files.deleteIfExists(Paths.get(testDataFile));
        } catch (Exception e) {
            // Ignore
        }
    }
    
    @Test
    public void testCreateProject() {
        // Test creating a new project
        Project project = projectManager.createProject("Test Project");
        
        assertNotNull("Created project should not be null", project);
        assertEquals("Project name should match", "Test Project", project.getName());
        assertEquals("Project should be active", "ACTIVE", project.getStatus());
        assertNotNull("Project should have created timestamp", project.getCreatedAt());
        
        // Test that project is current
        assertEquals("Created project should be current", project, projectManager.getCurrentProject());
        
        // Test duplicate project creation
        try {
            projectManager.createProject("Test Project");
            fail("Should not allow duplicate project names");
        } catch (IllegalArgumentException e) {
            assertTrue("Error message should mention duplicate", e.getMessage().contains("already exists"));
        }
    }
    
    @Test
    public void testOpenProject() {
        // Create and close a project
        Project original = projectManager.createProject("Test Project");
        projectManager.closeCurrentProject();
        
        // Open the project
        Project opened = projectManager.openProject("Test Project");
        
        assertNotNull("Opened project should not be null", opened);
        assertEquals("Project names should match", original.getName(), opened.getName());
        assertEquals("Opened project should be current", opened, projectManager.getCurrentProject());
        
        // Test opening non-existent project
        try {
            projectManager.openProject("Non-existent Project");
            fail("Should not allow opening non-existent project");
        } catch (IllegalArgumentException e) {
            assertTrue("Error message should mention not found", e.getMessage().contains("not found"));
        }
    }
    
    @Test
    public void testProjectPersistence() throws Exception {
        // Create a project with data
        Project project = projectManager.createProject("Persistence Test");
        project.setDescription("Test project for persistence");
        project.addTarget("192.168.1.100");
        project.addTarget("example.com");
        
        // Add a vulnerability
        Vulnerability vuln = new Vulnerability("VULN-001", "SQL Injection", 
                "SQL injection vulnerability", Severity.HIGH, "192.168.1.100");
        vuln.setService("HTTP");
        vuln.setPath("/login");
        project.addVulnerability(vuln);
        
        // Save projects
        projectManager.saveProjects();
        
        // Verify file exists
        assertTrue("Data file should exist", Files.exists(Paths.get(testDataFile)));
        
        // Create new project manager and load data
        ProjectManager newManager = new ProjectManager(testDataFile);
        
        // Verify project was loaded
        List<String> projectNames = newManager.getProjectNames();
        assertEquals("Should have one project", 1, projectNames.size());
        assertTrue("Project name should be loaded", projectNames.contains("Persistence Test"));
        
        // Verify project data
        Project loadedProject = newManager.getProject("Persistence Test");
        assertNotNull("Loaded project should not be null", loadedProject);
        assertEquals("Description should match", "Test project for persistence", loadedProject.getDescription());
        assertEquals("Should have 2 targets", 2, loadedProject.getTargets().size());
        assertTrue("Should contain target", loadedProject.getTargets().contains("192.168.1.100"));
        assertTrue("Should contain target", loadedProject.getTargets().contains("example.com"));
        
        // Verify vulnerability
        assertEquals("Should have 1 vulnerability", 1, loadedProject.getTotalVulnerabilityCount());
        Vulnerability loadedVuln = loadedProject.findVulnerability("VULN-001");
        assertNotNull("Vulnerability should be loaded", loadedVuln);
        assertEquals("Vulnerability name should match", "SQL Injection", loadedVuln.getName());
        assertEquals("Severity should match", Severity.HIGH, loadedVuln.getSeverity());
        assertEquals("Service should match", "HTTP", loadedVuln.getService());
        assertEquals("Path should match", "/login", loadedVuln.getPath());
        
        newManager.shutdown();
    }
    
    @Test
    public void testAddTargetToCurrentProject() {
        // Test without current project
        try {
            projectManager.addTargetToCurrentProject("192.168.1.100");
            fail("Should require current project");
        } catch (IllegalStateException e) {
            assertTrue("Error should mention no project", e.getMessage().contains("No project"));
        }
        
        // Test with current project
        projectManager.createProject("Target Test");
        projectManager.addTargetToCurrentProject("192.168.1.100");
        
        Project current = projectManager.getCurrentProject();
        assertTrue("Target should be added", current.getTargets().contains("192.168.1.100"));
    }
    
    @Test
    public void testAddVulnerabilityToCurrentProject() {
        // Test without current project
        Vulnerability vuln = new Vulnerability("VULN-001", "XSS", 
                "Cross-site scripting", Severity.MEDIUM, "192.168.1.100");
        
        try {
            projectManager.addVulnerabilityToCurrentProject(vuln);
            fail("Should require current project");
        } catch (IllegalStateException e) {
            assertTrue("Error should mention no project", e.getMessage().contains("No project"));
        }
        
        // Test with current project
        projectManager.createProject("Vulnerability Test");
        projectManager.addVulnerabilityToCurrentProject(vuln);
        
        Project current = projectManager.getCurrentProject();
        assertEquals("Should have 1 vulnerability", 1, current.getTotalVulnerabilityCount());
        assertTrue("Target should be auto-added", current.getTargets().contains("192.168.1.100"));
        
        Vulnerability found = current.findVulnerability("VULN-001");
        assertNotNull("Vulnerability should be found", found);
        assertEquals("Name should match", "XSS", found.getName());
    }
    
    @Test
    public void testProjectManagement() {
        // Create multiple projects
        projectManager.createProject("Project 1");
        projectManager.createProject("Project 2");
        projectManager.createProject("Project 3");
        
        // Test project listing
        List<String> names = projectManager.getProjectNames();
        assertEquals("Should have 3 projects", 3, names.size());
        assertTrue("Should contain all projects", 
                  names.contains("Project 1") && names.contains("Project 2") && names.contains("Project 3"));
        
        // Test project deletion
        assertTrue("Should delete existing project", projectManager.deleteProject("Project 2"));
        assertFalse("Should not delete non-existent project", projectManager.deleteProject("Non-existent"));
        
        names = projectManager.getProjectNames();
        assertEquals("Should have 2 projects", 2, names.size());
        assertFalse("Should not contain deleted project", names.contains("Project 2"));
        
        // Test closing current project
        assertTrue("Should close current project", projectManager.closeCurrentProject());
        assertNull("Should have no current project", projectManager.getCurrentProject());
        assertFalse("Should not close when no current project", projectManager.closeCurrentProject());
    }
    
    @Test
    public void testVulnerabilityTree() {
        Project project = projectManager.createProject("Tree Test");
        
        // Add vulnerabilities with different paths
        Vulnerability vuln1 = new Vulnerability("VULN-001", "SQL Injection", 
                "SQL injection in login", Severity.HIGH, "192.168.1.100");
        vuln1.setService("HTTP");
        vuln1.setPath("/login");
        
        Vulnerability vuln2 = new Vulnerability("VULN-002", "XSS", 
                "Cross-site scripting in search", Severity.MEDIUM, "192.168.1.100");
        vuln2.setService("HTTP");
        vuln2.setPath("/search");
        
        Vulnerability vuln3 = new Vulnerability("VULN-003", "Weak Password", 
                "Weak SSH password", Severity.LOW, "192.168.1.100");
        vuln3.setService("SSH");
        
        project.addVulnerability(vuln1);
        project.addVulnerability(vuln2);
        project.addVulnerability(vuln3);
        
        // Test vulnerability tree structure
        VulnerabilityTree tree = project.getVulnerabilities();
        assertEquals("Should have 3 vulnerabilities total", 3, tree.getTotalVulnerabilityCount());
        
        // Test target filtering
        List<Vulnerability> targetVulns = tree.getVulnerabilitiesForTarget("192.168.1.100");
        assertEquals("Should have all 3 vulnerabilities for target", 3, targetVulns.size());
        
        // Test service filtering
        List<Vulnerability> httpVulns = tree.getVulnerabilitiesForService("192.168.1.100", "HTTP");
        assertEquals("Should have 2 HTTP vulnerabilities", 2, httpVulns.size());
        
        // Test severity filtering
        List<Vulnerability> highVulns = tree.getVulnerabilitiesBySeverity(Severity.HIGH);
        assertEquals("Should have 1 high severity vulnerability", 1, highVulns.size());
        assertEquals("High vulnerability should be SQL injection", "SQL Injection", highVulns.get(0).getName());
        
        // Test tree formatting
        String treeString = tree.formatTree(false);
        assertNotNull("Tree string should not be null", treeString);
        assertTrue("Tree should contain project name", treeString.contains("Tree Test"));
        assertTrue("Tree should contain target", treeString.contains("192.168.1.100"));
    }
    
    @Test
    public void testSeverityEnum() {
        // Test severity parsing
        assertEquals("Should parse CRITICAL", Severity.CRITICAL, Severity.fromString("critical"));
        assertEquals("Should parse HIGH", Severity.HIGH, Severity.fromString("High"));
        assertEquals("Should parse MEDIUM", Severity.MEDIUM, Severity.fromString("MEDIUM"));
        assertEquals("Should parse LOW", Severity.LOW, Severity.fromString("low"));
        assertEquals("Should parse INFO", Severity.INFO, Severity.fromString("Info"));
        
        // Test default for empty input
        assertEquals("Should default to INFO", Severity.INFO, Severity.fromString(""));
        assertEquals("Should default to INFO", Severity.INFO, Severity.fromString(null));
        
        // Test invalid severity
        try {
            Severity.fromString("invalid");
            fail("Should throw exception for invalid severity");
        } catch (IllegalArgumentException e) {
            assertTrue("Error should mention unknown severity", e.getMessage().contains("Unknown severity"));
        }
        
        // Test priority ordering
        assertTrue("Critical should have highest priority", 
                  Severity.CRITICAL.getPriority() > Severity.HIGH.getPriority());
        assertTrue("High should be higher than medium", 
                  Severity.HIGH.getPriority() > Severity.MEDIUM.getPriority());
        assertTrue("Medium should be higher than low", 
                  Severity.MEDIUM.getPriority() > Severity.LOW.getPriority());
        assertTrue("Low should be higher than info", 
                  Severity.LOW.getPriority() > Severity.INFO.getPriority());
    }
    
    @Test
    public void testVulnerabilitySerialization() {
        // Create vulnerability with all fields
        Vulnerability original = new Vulnerability("VULN-001", "Test Vulnerability", 
                "A test vulnerability", Severity.HIGH, "192.168.1.100");
        original.setCveId("CVE-2023-1234");
        original.setService("HTTP");
        original.setPath("/test");
        original.setEvidence("Test evidence");
        original.setDiscoveredBy("Test User");
        original.setNotes("Test notes");
        original.setVerified(true);
        original.addRemediationStep("Step 1: Fix this");
        original.addRemediationStep("Step 2: Test that");
        
        // Serialize and deserialize
        String serialized = original.serialize();
        Vulnerability deserialized = Vulnerability.deserialize(serialized);
        
        // Verify all fields
        assertEquals("ID should match", original.getId(), deserialized.getId());
        assertEquals("Name should match", original.getName(), deserialized.getName());
        assertEquals("Description should match", original.getDescription(), deserialized.getDescription());
        assertEquals("Severity should match", original.getSeverity(), deserialized.getSeverity());
        assertEquals("Target should match", original.getTarget(), deserialized.getTarget());
        assertEquals("CVE ID should match", original.getCveId(), deserialized.getCveId());
        assertEquals("Service should match", original.getService(), deserialized.getService());
        assertEquals("Path should match", original.getPath(), deserialized.getPath());
        assertEquals("Evidence should match", original.getEvidence(), deserialized.getEvidence());
        assertEquals("Discovered by should match", original.getDiscoveredBy(), deserialized.getDiscoveredBy());
        assertEquals("Notes should match", original.getNotes(), deserialized.getNotes());
        assertEquals("Verified status should match", original.isVerified(), deserialized.isVerified());
        assertEquals("Remediation steps should match", original.getRemediation(), deserialized.getRemediation());
    }
    
    @Test
    public void testProjectStatistics() {
        // Create projects with different statuses and vulnerabilities
        Project project1 = projectManager.createProject("Active Project");
        project1.setStatus("ACTIVE");
        project1.addVulnerability(new Vulnerability("V1", "Critical Issue", "desc", Severity.CRITICAL, "target1"));
        project1.addVulnerability(new Vulnerability("V2", "High Issue", "desc", Severity.HIGH, "target1"));
        
        Project project2 = projectManager.createProject("Completed Project");
        project2.setStatus("COMPLETED");
        project2.addVulnerability(new Vulnerability("V3", "Medium Issue", "desc", Severity.MEDIUM, "target2"));
        
        // Test statistics
        String stats = projectManager.getProjectStatistics();
        assertNotNull("Statistics should not be null", stats);
        assertTrue("Should contain total projects", stats.contains("Total Projects: 2"));
        assertTrue("Should contain total vulnerabilities", stats.contains("Total Vulnerabilities: 3"));
        assertTrue("Should contain status breakdown", stats.contains("ACTIVE: 1"));
        assertTrue("Should contain status breakdown", stats.contains("COMPLETED: 1"));
        assertTrue("Should contain severity breakdown", stats.contains("Critical: 1"));
        assertTrue("Should contain severity breakdown", stats.contains("High: 1"));
        assertTrue("Should contain severity breakdown", stats.contains("Medium: 1"));
    }
    
    @Test
    public void testProjectCommandProcessor() {
        ProjectCommandProcessor processor = new ProjectCommandProcessor(projectManager);
        
        // Test project creation
        String response = processor.processCommand("create project named \"Test Project\"");
        assertTrue("Should indicate success", response.contains("Created new project"));
        assertTrue("Should mention project name", response.contains("Test Project"));
        
        // Test project listing
        response = processor.processCommand("list projects");
        assertTrue("Should list projects", response.contains("Test Project"));
        
        // Test adding target
        response = processor.processCommand("add target 192.168.1.100");
        assertTrue("Should add target", response.contains("Added target"));
        assertTrue("Should mention target", response.contains("192.168.1.100"));
        
        // Test adding vulnerability
        response = processor.processCommand("add vulnerability \"SQL Injection\" to 192.168.1.100 with severity high");
        assertTrue("Should add vulnerability", response.contains("Added"));
        assertTrue("Should mention vulnerability", response.contains("SQL Injection"));
        
        // Test listing vulnerabilities
        response = processor.processCommand("list vulnerabilities");
        assertTrue("Should list vulnerabilities", response.contains("SQL Injection"));
        
        // Test generating report
        response = processor.processCommand("generate report");
        assertTrue("Should generate report", response.contains("Vulnerability Report"));
        
        // Test help command
        response = processor.processCommand("help");
        assertTrue("Should show help", response.contains("Project Management Commands"));
        
        // Test invalid command
        response = processor.processCommand("invalid command here");
        assertTrue("Should provide suggestions", response.contains("didn't understand"));
    }
    
    @Test
    public void testCommandPatternMatching() {
        ProjectCommandProcessor processor = new ProjectCommandProcessor(projectManager);
        
        // Test various command patterns
        assertTrue("Should recognize project creation", 
                  processor.isProjectCommand("create project named Test"));
        assertTrue("Should recognize project opening", 
                  processor.isProjectCommand("open project Test"));
        assertTrue("Should recognize target addition", 
                  processor.isProjectCommand("add target 192.168.1.100"));
        assertTrue("Should recognize vulnerability commands", 
                  processor.isProjectCommand("list vulnerabilities"));
        assertTrue("Should recognize report generation", 
                  processor.isProjectCommand("generate report"));
        
        // Test non-project commands
        assertFalse("Should not recognize non-project commands", 
                   processor.isProjectCommand("hello world"));
        assertFalse("Should not recognize empty commands", 
                   processor.isProjectCommand(""));
        assertFalse("Should not recognize null commands", 
                   processor.isProjectCommand(null));
    }
}