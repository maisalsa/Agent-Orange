package com.example;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.After;
import java.util.Set;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for OptimizedProject class.
 * Tests memory efficiency, performance optimizations, and functionality.
 */
public class OptimizedProjectTest {
    
    private OptimizedProject project;
    private static final String TEST_PROJECT_NAME = "Test Project";
    
    @Before
    public void setUp() {
        project = new OptimizedProject(TEST_PROJECT_NAME);
    }
    
    @After
    public void tearDown() {
        if (project != null) {
            project.optimizeMemory();
        }
    }
    
    @Test
    public void testBasicProjectCreation() {
        assertNotNull("Project should not be null", project);
        assertEquals("Project name should match", TEST_PROJECT_NAME, project.getName());
        assertNotNull("Project ID should not be null", project.getId());
        assertNotNull("Created date should not be null", project.getCreatedAt());
        assertNotNull("Last modified should not be null", project.getLastModified());
        assertEquals("Default status should be ACTIVE", "ACTIVE", project.getStatus());
    }
    
    @Test
    public void testStringInterning() {
        // Test that common strings are interned for memory efficiency
        String projectName1 = new String("Common Project"); // Force new string
        String projectName2 = new String("Common Project"); // Force new string
        
        OptimizedProject project1 = new OptimizedProject(projectName1);
        OptimizedProject project2 = new OptimizedProject(projectName2);
        
        // Names should be the same reference due to interning
        assertSame("Project names should be interned", project1.getName(), project2.getName());
    }
    
    @Test
    public void testTargetManagement() {
        assertTrue("Target should be added", project.addTarget("192.168.1.1"));
        assertTrue("Target should be added", project.addTarget("example.com"));
        assertFalse("Duplicate target should not be added", project.addTarget("192.168.1.1"));
        
        Set<String> targets = project.getTargets();
        assertEquals("Should have 2 targets", 2, targets.size());
        assertTrue("Should contain first target", targets.contains("192.168.1.1"));
        assertTrue("Should contain second target", targets.contains("example.com"));
        
        assertTrue("Target should be removed", project.removeTarget("192.168.1.1"));
        assertFalse("Target should not be removed twice", project.removeTarget("192.168.1.1"));
        assertEquals("Should have 1 target after removal", 1, project.getTargets().size());
    }
    
    @Test
    public void testTagManagement() {
        assertTrue("Tag should be added", project.addTag("web"));
        assertTrue("Tag should be added", project.addTag("network"));
        assertFalse("Duplicate tag should not be added", project.addTag("web"));
        
        Set<String> tags = project.getTags();
        assertEquals("Should have 2 tags", 2, tags.size());
        assertTrue("Should contain first tag", tags.contains("web"));
        assertTrue("Should contain second tag", tags.contains("network"));
        
        assertTrue("Tag should be removed", project.removeTag("web"));
        assertFalse("Tag should not be removed twice", project.removeTag("web"));
        assertEquals("Should have 1 tag after removal", 1, project.getTags().size());
    }
    
    @Test
    public void testVulnerabilityManagement() {
        // Create test vulnerability
        Vulnerability vuln = new Vulnerability("test_id", "Test Vulnerability", 
                                             "Test description", Severity.HIGH, "test.com");
        
        project.addVulnerability(vuln);
        
        List<Vulnerability> vulnerabilities = project.getAllVulnerabilities();
        assertEquals("Should have 1 vulnerability", 1, vulnerabilities.size());
        assertEquals("Vulnerability should match", vuln, vulnerabilities.get(0));
        
        // Test target-specific vulnerability retrieval
        List<Vulnerability> targetVulns = project.getVulnerabilitiesForTarget("test.com");
        assertEquals("Should have 1 vulnerability for target", 1, targetVulns.size());
        
        List<Vulnerability> noTargetVulns = project.getVulnerabilitiesForTarget("nonexistent.com");
        assertTrue("Should have no vulnerabilities for non-existent target", noTargetVulns.isEmpty());
    }
    
    @Test
    public void testCacheInvalidation() {
        // Add some data
        project.addTarget("test.com");
        
        // Get cached data
        List<Vulnerability> vulns1 = project.getAllVulnerabilities();
        Map<String, Object> stats1 = project.getStatistics();
        
        // Add more data to trigger cache invalidation
        Vulnerability vuln = new Vulnerability("test_id", "Test", "Description", Severity.HIGH, "test.com");
        project.addVulnerability(vuln);
        
        // Get data again (should be recalculated)
        List<Vulnerability> vulns2 = project.getAllVulnerabilities();
        Map<String, Object> stats2 = project.getStatistics();
        
        assertNotEquals("Vulnerability lists should be different", vulns1.size(), vulns2.size());
        assertNotEquals("Statistics should be different", 
                       stats1.get("vulnerabilityCount"), stats2.get("vulnerabilityCount"));
    }
    
    @Test
    public void testStatisticsCalculation() {
        // Add test data
        project.addTarget("example.com");
        project.addTarget("test.com");
        project.addTag("web");
        project.addTag("network");
        
        Vulnerability vuln1 = new Vulnerability("id1", "Test 1", "Description", Severity.HIGH, "example.com");
        Vulnerability vuln2 = new Vulnerability("id2", "Test 2", "Description", Severity.MEDIUM, "test.com");
        Vulnerability vuln3 = new Vulnerability("id3", "CVE-2023-1234", "CVE Description", Severity.CRITICAL, "example.com");
        vuln3.setCveId("CVE-2023-1234");
        
        project.addVulnerability(vuln1);
        project.addVulnerability(vuln2);
        project.addVulnerability(vuln3);
        
        Map<String, Object> stats = project.getStatistics();
        
        assertEquals("Should have 2 targets", 2, stats.get("targetCount"));
        assertEquals("Should have 2 tags", 2, stats.get("tagCount"));
        assertEquals("Should have 3 vulnerabilities", 3, stats.get("vulnerabilityCount"));
        assertEquals("Should have 1 CVE", 1L, stats.get("cveCount"));
        
        @SuppressWarnings("unchecked")
        Map<Severity, Integer> severityCounts = (Map<Severity, Integer>) stats.get("severityCounts");
        assertEquals("Should have 1 critical vulnerability", Integer.valueOf(1), severityCounts.get(Severity.CRITICAL));
        assertEquals("Should have 1 high vulnerability", Integer.valueOf(1), severityCounts.get(Severity.HIGH));
        assertEquals("Should have 1 medium vulnerability", Integer.valueOf(1), severityCounts.get(Severity.MEDIUM));
    }
    
    @Test
    public void testMemoryOptimization() {
        // Add a lot of data
        for (int i = 0; i < 100; i++) {
            project.addTarget("target" + i + ".com");
            project.addTag("tag" + i);
            
            Vulnerability vuln = new Vulnerability("id" + i, "Vulnerability " + i, 
                                                 "Description " + i, Severity.MEDIUM, "target" + i + ".com");
            project.addVulnerability(vuln);
        }
        
        Map<String, Object> memoryStatsBefore = project.getMemoryStats();
        
        // Perform memory optimization
        project.optimizeMemory();
        
        Map<String, Object> memoryStatsAfter = project.getMemoryStats();
        
        // Verify optimization occurred (caches should be cleared)
        assertFalse("Vulnerability cache should be cleared", 
                   (Boolean) memoryStatsAfter.get("hasVulnerabilityCache"));
        assertFalse("Stats cache should be cleared", 
                   (Boolean) memoryStatsAfter.get("hasStatsCache"));
    }
    
    @Test
    public void testConcurrentAccess() throws InterruptedException {
        final int NUM_THREADS = 10;
        final int OPERATIONS_PER_THREAD = 50;
        Thread[] threads = new Thread[NUM_THREADS];
        
        // Create threads that concurrently modify the project
        for (int i = 0; i < NUM_THREADS; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
                    project.addTarget("target" + threadId + "_" + j + ".com");
                    project.addTag("tag" + threadId + "_" + j);
                    
                    Vulnerability vuln = new Vulnerability(
                        "id" + threadId + "_" + j, 
                        "Vulnerability " + threadId + "_" + j,
                        "Description", 
                        Severity.MEDIUM, 
                        "target" + threadId + "_" + j + ".com"
                    );
                    project.addVulnerability(vuln);
                }
            });
        }
        
        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Verify final state
        int expectedTargets = NUM_THREADS * OPERATIONS_PER_THREAD;
        int expectedTags = NUM_THREADS * OPERATIONS_PER_THREAD;
        int expectedVulnerabilities = NUM_THREADS * OPERATIONS_PER_THREAD;
        
        assertEquals("Should have expected number of targets", expectedTargets, project.getTargets().size());
        assertEquals("Should have expected number of tags", expectedTags, project.getTags().size());
        assertEquals("Should have expected number of vulnerabilities", 
                    expectedVulnerabilities, project.getAllVulnerabilities().size());
    }
    
    @Test
    public void testFormattedInfo() {
        project.setDescription("Test project description");
        project.addTarget("example.com");
        project.addTag("web");
        
        Vulnerability vuln = new Vulnerability("id1", "Test Vulnerability", 
                                             "Description", Severity.HIGH, "example.com");
        project.addVulnerability(vuln);
        
        String formattedInfo = project.getFormattedInfo();
        
        assertNotNull("Formatted info should not be null", formattedInfo);
        assertTrue("Should contain project name", formattedInfo.contains(TEST_PROJECT_NAME));
        assertTrue("Should contain description", formattedInfo.contains("Test project description"));
        assertTrue("Should contain target count", formattedInfo.contains("1"));
        assertTrue("Should contain vulnerability count", formattedInfo.contains("1"));
        assertTrue("Should contain tag", formattedInfo.contains("web"));
    }
    
    @Test
    public void testEqualsAndHashCode() {
        OptimizedProject project1 = new OptimizedProject("Test Project 1");
        OptimizedProject project2 = new OptimizedProject("Test Project 2");
        OptimizedProject project3 = new OptimizedProject("Test Project 1");
        
        // Projects with different IDs should not be equal
        assertNotEquals("Projects with different IDs should not be equal", project1, project2);
        assertNotEquals("Projects with different IDs should not be equal", project1, project3);
        
        // Same project instance should be equal to itself
        assertEquals("Project should equal itself", project1, project1);
        
        // Hash codes should be consistent
        assertEquals("Hash code should be consistent", project1.hashCode(), project1.hashCode());
    }
    
    @Test
    public void testToString() {
        project.addTarget("example.com");
        Vulnerability vuln = new Vulnerability("id1", "Test", "Description", Severity.HIGH, "example.com");
        project.addVulnerability(vuln);
        
        String toString = project.toString();
        
        assertNotNull("toString should not be null", toString);
        assertTrue("Should contain class name", toString.contains("OptimizedProject"));
        assertTrue("Should contain project name", toString.contains(TEST_PROJECT_NAME));
        assertTrue("Should contain target count", toString.contains("1"));
        assertTrue("Should contain vulnerability count", toString.contains("1"));
    }
    
    @Test
    public void testNullSafety() {
        // Test null target handling
        assertFalse("Should not add null target", project.addTarget(null));
        assertFalse("Should not add empty target", project.addTarget(""));
        assertFalse("Should not add whitespace-only target", project.addTarget("   "));
        
        // Test null tag handling
        assertFalse("Should not add null tag", project.addTag(null));
        assertFalse("Should not add empty tag", project.addTag(""));
        assertFalse("Should not add whitespace-only tag", project.addTag("   "));
        
        // Test null vulnerability handling
        project.addVulnerability(null); // Should not throw exception
        assertEquals("Should have no vulnerabilities", 0, project.getAllVulnerabilities().size());
        
        // Test null removal
        assertFalse("Should not remove null target", project.removeTarget(null));
        assertFalse("Should not remove null tag", project.removeTag(null));
    }
    
    @Test
    public void testPerformanceWithLargeDataset() {
        final int LARGE_SIZE = 1000;
        
        long startTime = System.currentTimeMillis();
        
        // Add large amount of data
        for (int i = 0; i < LARGE_SIZE; i++) {
            project.addTarget("target" + i + ".example.com");
            project.addTag("tag" + i);
            
            Vulnerability vuln = new Vulnerability("vuln" + i, "Vulnerability " + i,
                                                 "Description for vulnerability " + i,
                                                 Severity.values()[i % Severity.values().length],
                                                 "target" + i + ".example.com");
            project.addVulnerability(vuln);
        }
        
        long addTime = System.currentTimeMillis() - startTime;
        
        // Test retrieval performance
        startTime = System.currentTimeMillis();
        List<Vulnerability> allVulns = project.getAllVulnerabilities();
        long retrievalTime = System.currentTimeMillis() - startTime;
        
        // Test statistics calculation performance
        startTime = System.currentTimeMillis();
        Map<String, Object> stats = project.getStatistics();
        long statsTime = System.currentTimeMillis() - startTime;
        
        // Verify correctness
        assertEquals("Should have correct number of targets", LARGE_SIZE, project.getTargets().size());
        assertEquals("Should have correct number of tags", LARGE_SIZE, project.getTags().size());
        assertEquals("Should have correct number of vulnerabilities", LARGE_SIZE, allVulns.size());
        assertEquals("Statistics should be correct", LARGE_SIZE, stats.get("vulnerabilityCount"));
        
        // Performance assertions (should complete within reasonable time)
        assertTrue("Adding data should be fast", addTime < 5000); // 5 seconds
        assertTrue("Retrieval should be fast", retrievalTime < 1000); // 1 second
        assertTrue("Statistics calculation should be fast", statsTime < 1000); // 1 second
        
        System.out.println(String.format("Performance test completed - Add: %dms, Retrieve: %dms, Stats: %dms",
                                        addTime, retrievalTime, statsTime));
    }
}