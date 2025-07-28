package com.example;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;

/**
 * Unit tests for CveUtils class.
 * Tests CVE ID validation, naming conventions, and vulnerability information processing.
 */
public class CveUtilsTest {
    
    @Test
    public void testValidCveIds() {
        assertTrue("CVE-2023-1234 should be valid", CveUtils.isValidCveId("CVE-2023-1234"));
        assertTrue("CVE-2021-44228 should be valid", CveUtils.isValidCveId("CVE-2021-44228"));
        assertTrue("CVE-1999-0001 should be valid", CveUtils.isValidCveId("CVE-1999-0001"));
        assertTrue("Lowercase should be valid", CveUtils.isValidCveId("cve-2023-1234"));
        assertTrue("Mixed case should be valid", CveUtils.isValidCveId("Cve-2023-1234"));
    }
    
    @Test
    public void testInvalidCveIds() {
        assertFalse("Null should be invalid", CveUtils.isValidCveId(null));
        assertFalse("Empty should be invalid", CveUtils.isValidCveId(""));
        assertFalse("Spaces should be invalid", CveUtils.isValidCveId("   "));
        assertFalse("Wrong format should be invalid", CveUtils.isValidCveId("CVE-23-1234"));
        assertFalse("Wrong format should be invalid", CveUtils.isValidCveId("CVE-2023-123"));
        assertFalse("Wrong format should be invalid", CveUtils.isValidCveId("VULN-2023-1234"));
        assertFalse("Wrong format should be invalid", CveUtils.isValidCveId("CVE-2023"));
        assertFalse("Extra chars should be invalid", CveUtils.isValidCveId("CVE-2023-1234x"));
    }
    
    @Test
    public void testNormalizeCveId() {
        assertEquals("Should normalize to uppercase", "CVE-2023-1234", CveUtils.normalizeCveId("cve-2023-1234"));
        assertEquals("Should normalize to uppercase", "CVE-2023-1234", CveUtils.normalizeCveId("Cve-2023-1234"));
        assertEquals("Should keep uppercase", "CVE-2023-1234", CveUtils.normalizeCveId("CVE-2023-1234"));
        assertEquals("Should trim whitespace", "CVE-2023-1234", CveUtils.normalizeCveId("  CVE-2023-1234  "));
        assertNull("Invalid format should return null", CveUtils.normalizeCveId("INVALID"));
        assertNull("Null should return null", CveUtils.normalizeCveId(null));
    }
    
    @Test
    public void testExtractCveIdFromText() {
        assertEquals("Should extract CVE from text", "CVE-2023-1234", 
                    CveUtils.extractCveIdFromText("This vulnerability is tracked as CVE-2023-1234 in the database."));
        assertEquals("Should extract lowercase CVE", "CVE-2023-1234", 
                    CveUtils.extractCveIdFromText("Found cve-2023-1234 in the logs."));
        assertEquals("Should extract first CVE", "CVE-2023-1234", 
                    CveUtils.extractCveIdFromText("Multiple CVEs: CVE-2023-1234 and CVE-2023-5678"));
        assertNull("Should return null if no CVE found", 
                  CveUtils.extractCveIdFromText("No CVE in this text."));
        assertNull("Should return null for null input", 
                  CveUtils.extractCveIdFromText(null));
    }
    
    @Test
    public void testGenerateDescriptiveName() {
        assertEquals("Should generate descriptive name", "SQL Injection", 
                    CveUtils.generateDescriptiveName("SQL Injection", null));
        assertEquals("Should clean vulnerability prefix", "Cross-Site Scripting", 
                    CveUtils.generateDescriptiveName("Vulnerability: Cross-Site Scripting", null));
        assertEquals("Should add target context", "Buffer Overflow (example.com)", 
                    CveUtils.generateDescriptiveName("Buffer Overflow", "example.com"));
        assertEquals("Should not duplicate target", "SQL Injection in example.com", 
                    CveUtils.generateDescriptiveName("SQL Injection in example.com", "example.com"));
        assertEquals("Should handle empty description", "Unknown Vulnerability", 
                    CveUtils.generateDescriptiveName("", null));
        assertEquals("Should handle null description", "Unknown Vulnerability", 
                    CveUtils.generateDescriptiveName(null, null));
    }
    
    @Test
    public void testDetermineBestName() {
        // CVE ID takes priority
        assertEquals("CVE should take priority", "CVE-2023-1234", 
                    CveUtils.determineBestName("CVE-2023-1234", "SQL Injection", "Description", "target"));
        
        // Extract CVE from name
        assertEquals("Should extract CVE from name", "CVE-2023-1234", 
                    CveUtils.determineBestName(null, "CVE-2023-1234: SQL Injection", "Description", "target"));
        
        // Extract CVE from description
        assertEquals("Should extract CVE from description", "CVE-2023-1234", 
                    CveUtils.determineBestName(null, "SQL Injection", "This is CVE-2023-1234", "target"));
        
        // Use descriptive name when no CVE
        assertEquals("Should use descriptive name", "SQL Injection", 
                    CveUtils.determineBestName(null, "SQL Injection", "Description", "target"));
    }
    
    @Test
    public void testDetermineCveId() {
        assertEquals("Should return explicit CVE ID", "CVE-2023-1234", 
                    CveUtils.determineCveId("CVE-2023-1234", "name", "description"));
        assertEquals("Should extract from name", "CVE-2023-1234", 
                    CveUtils.determineCveId(null, "CVE-2023-1234: SQL Injection", "description"));
        assertEquals("Should extract from description", "CVE-2023-1234", 
                    CveUtils.determineCveId(null, "name", "This is CVE-2023-1234"));
        assertNull("Should return null if no CVE found", 
                  CveUtils.determineCveId(null, "name", "description"));
    }
    
    @Test
    public void testCreateVulnerabilityInfo() {
        // Test with explicit CVE ID
        CveUtils.VulnerabilityInfo info1 = CveUtils.createVulnerabilityInfo(
            "id1", "SQL Injection", "A SQL injection vulnerability", "target", "CVE-2023-1234");
        
        assertEquals("Should use CVE as name", "CVE-2023-1234", info1.getName());
        assertEquals("Should store CVE ID", "CVE-2023-1234", info1.getCveId());
        assertTrue("Should have CVE ID", info1.hasCveId());
        
        // Test without CVE ID
        CveUtils.VulnerabilityInfo info2 = CveUtils.createVulnerabilityInfo(
            "id2", "Cross-Site Scripting", "An XSS vulnerability", "target", null);
        
        assertEquals("Should use descriptive name", "Cross-Site Scripting", info2.getName());
        assertNull("Should not have CVE ID", info2.getCveId());
        assertFalse("Should not have CVE ID", info2.hasCveId());
        
        // Test with CVE in description
        CveUtils.VulnerabilityInfo info3 = CveUtils.createVulnerabilityInfo(
            "id3", "Buffer Overflow", "CVE-2023-5678: A buffer overflow", "target", null);
        
        assertEquals("Should extract CVE as name", "CVE-2023-5678", info3.getName());
        assertEquals("Should store extracted CVE ID", "CVE-2023-5678", info3.getCveId());
        assertTrue("Should have CVE ID", info3.hasCveId());
    }
    
    @Test
    public void testFormatDisplayName() {
        // Create test vulnerabilities
        Vulnerability vulnWithCve = new Vulnerability("id1", "SQL Injection", "Description", 
                                                     Severity.HIGH, "target");
        vulnWithCve.setCveId("CVE-2023-1234");
        
        Vulnerability vulnWithoutCve = new Vulnerability("id2", "XSS Vulnerability", "Description", 
                                                        Severity.MEDIUM, "target");
        
        assertEquals("Should display CVE ID", "CVE-2023-1234", 
                    CveUtils.formatDisplayName(vulnWithCve));
        assertEquals("Should display name when no CVE", "XSS Vulnerability", 
                    CveUtils.formatDisplayName(vulnWithoutCve));
        assertEquals("Should handle null", "Unknown Vulnerability", 
                    CveUtils.formatDisplayName(null));
    }
    
    @Test
    public void testFormatDisplayNameWithContext() {
        // Create test vulnerability with CVE
        Vulnerability vulnWithCve = new Vulnerability("id1", "SQL Injection", "Description", 
                                                     Severity.HIGH, "example.com");
        vulnWithCve.setCveId("CVE-2023-1234");
        
        // CVE name is different from descriptive name
        assertEquals("Should show CVE with context", "CVE-2023-1234 (SQL Injection) - example.com", 
                    CveUtils.formatDisplayNameWithContext(vulnWithCve, true));
        assertEquals("Should show CVE without target", "CVE-2023-1234 (SQL Injection)", 
                    CveUtils.formatDisplayNameWithContext(vulnWithCve, false));
        
        // Create vulnerability where CVE matches name
        Vulnerability vulnCveOnly = new Vulnerability("id2", "CVE-2023-5678", "Description", 
                                                     Severity.HIGH, "example.com");
        vulnCveOnly.setCveId("CVE-2023-5678");
        
        assertEquals("Should show CVE only when name matches", "CVE-2023-5678 - example.com", 
                    CveUtils.formatDisplayNameWithContext(vulnCveOnly, true));
        
        // Create vulnerability without CVE
        Vulnerability vulnNoCve = new Vulnerability("id3", "XSS Vulnerability", "Description", 
                                                   Severity.MEDIUM, "example.com");
        
        assertEquals("Should show name with target", "XSS Vulnerability - example.com", 
                    CveUtils.formatDisplayNameWithContext(vulnNoCve, true));
    }
    
    @Test
    public void testVulnerabilityCreationWithCve() {
        // Test creating vulnerability with CVE convention
        Vulnerability vuln = Vulnerability.createWithCveConvention(
            "test_id", "SQL Injection", "A SQL injection vulnerability", 
            Severity.HIGH, "example.com", "CVE-2023-1234");
        
        assertEquals("Should use CVE as name", "CVE-2023-1234", vuln.getName());
        assertEquals("Should store CVE ID", "CVE-2023-1234", vuln.getCveId());
        assertEquals("Should preserve target", "example.com", vuln.getTarget());
        assertEquals("Should preserve severity", Severity.HIGH, vuln.getSeverity());
    }
    
    @Test
    public void testVulnerabilityCreationFromBurpSuite() {
        // Test creating vulnerability from Burp Suite with CVE in name
        Vulnerability vuln = Vulnerability.createFromBurpSuite(
            "1234", "CVE-2023-1234: SQL Injection", "Detailed description", 
            Severity.HIGH, "example.com");
        
        assertEquals("Should extract CVE as name", "CVE-2023-1234", vuln.getName());
        assertEquals("Should store CVE ID", "CVE-2023-1234", vuln.getCveId());
        assertEquals("Should preserve target", "example.com", vuln.getTarget());
    }
    
    @Test
    public void testCveIdValidationOnSet() {
        Vulnerability vuln = new Vulnerability("id", "name", "description", Severity.HIGH, "target");
        
        // Valid CVE IDs should work
        vuln.setCveId("CVE-2023-1234");
        assertEquals("Should set valid CVE", "CVE-2023-1234", vuln.getCveId());
        
        vuln.setCveId("cve-2023-5678");
        assertEquals("Should normalize CVE", "CVE-2023-5678", vuln.getCveId());
        
        vuln.setCveId(null);
        assertNull("Should allow null", vuln.getCveId());
        
        vuln.setCveId("");
        assertNull("Should allow empty", vuln.getCveId());
        
        // Invalid CVE IDs should throw exception
        try {
            vuln.setCveId("INVALID-FORMAT");
            fail("Should throw exception for invalid CVE format");
        } catch (IllegalArgumentException e) {
            assertTrue("Should mention format requirement", e.getMessage().contains("Expected format"));
        }
    }
    
    @Test
    public void testEdgeCases() {
        // Test with very long descriptions
        String longDesc = "A".repeat(200);
        String result = CveUtils.generateDescriptiveName(longDesc, "target");
        assertTrue("Should truncate long descriptions", result.length() <= 100);
        assertTrue("Should end with ellipsis", result.endsWith("..."));
        
        // Test with special characters in CVE extraction
        String textWithSpecialChars = "Found vuln: CVE-2023-1234. Also see CVE-2023-5678!";
        assertEquals("Should extract first CVE", "CVE-2023-1234", 
                    CveUtils.extractCveIdFromText(textWithSpecialChars));
        
        // Test with minimal valid CVE (4 digits in number part)
        assertTrue("Should accept 4-digit number", CveUtils.isValidCveId("CVE-2023-1000"));
        
        // Test with large CVE number
        assertTrue("Should accept large numbers", CveUtils.isValidCveId("CVE-2023-123456789"));
    }
}