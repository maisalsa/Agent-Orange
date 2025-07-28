package com.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for BurpSuiteParser class.
 * 
 * Tests Burp Suite XML parsing, vulnerability extraction,
 * and severity mapping functionality.
 * 
 * @author Agent-Orange Team
 * @version 1.0
 */
public class BurpSuiteParserTest {
    
    @TempDir
    Path tempDir;
    
    private String sampleBurpXml;
    
    @BeforeEach
    void setUp() {
        // Sample Burp Suite XML export format
        sampleBurpXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <issues burpVersion="2023.10.3.4" exportTime="2024-01-15T10:30:00">
              <issue>
                <serialNumber>1</serialNumber>
                <type>5243392</type>
                <name>Cross-site scripting (reflected)</name>
                <host ip="192.168.1.100">https://example.com</host>
                <path>/search</path>
                <location>https://example.com/search?q=[XSS]</location>
                <severity>High</severity>
                <confidence>Certain</confidence>
                <issueBackground><![CDATA[Reflected cross-site scripting vulnerabilities arise when data is copied from a request and echoed into the application's immediate response in an unsafe way.]]></issueBackground>
                <remediationBackground><![CDATA[In most situations where user-controllable data is copied into application responses, cross-site scripting attacks can be prevented using two layers of defenses.]]></remediationBackground>
                <issueDetail><![CDATA[The application appears to be vulnerable to reflected cross-site scripting attacks.]]></issueDetail>
                <remediationDetail><![CDATA[Ensure that user-controllable data is properly encoded before being inserted into application responses.]]></remediationDetail>
                <request base64="false"><![CDATA[GET /search?q=<script>alert(1)</script> HTTP/1.1
            Host: example.com
            User-Agent: Mozilla/5.0]]></request>
                <response base64="false"><![CDATA[HTTP/1.1 200 OK
            Content-Type: text/html
            
            <html><body>Search results for: <script>alert(1)</script></body></html>]]></response>
              </issue>
              
              <issue>
                <serialNumber>2</serialNumber>
                <type>1048832</type>
                <name>SQL injection</name>
                <host ip="192.168.1.100">https://example.com</host>
                <path>/login</path>
                <location>https://example.com/login</location>
                <severity>High</severity>
                <confidence>Firm</confidence>
                <issueBackground><![CDATA[SQL injection vulnerabilities arise when user-controllable data is incorporated into database SQL queries in an unsafe manner.]]></issueBackground>
                <remediationBackground><![CDATA[SQL injection vulnerabilities can be prevented by using parameterized queries (prepared statements).]]></remediationBackground>
                <issueDetail><![CDATA[The application appears to be vulnerable to SQL injection attacks.]]></issueDetail>
                <remediationDetail><![CDATA[Use parameterized queries and proper input validation.]]></remediationDetail>
              </issue>
              
              <issue>
                <serialNumber>3</serialNumber>
                <type>2097408</type>
                <name>Password field with autocomplete enabled</name>
                <host ip="10.0.0.1">https://app.test.com</host>
                <path>/account</path>
                <location>https://app.test.com/account</location>
                <severity>Low</severity>
                <confidence>Certain</confidence>
                <issueBackground><![CDATA[The autocomplete attribute is not disabled for a password input field.]]></issueBackground>
                <remediationBackground><![CDATA[Password fields should have the autocomplete attribute set to off.]]></remediationBackground>
                <issueDetail><![CDATA[Password field allows autocomplete.]]></issueDetail>
                <remediationDetail><![CDATA[Add autocomplete='off' to password input fields.]]></remediationDetail>
              </issue>
              
              <issue>
                <serialNumber>4</serialNumber>
                <type>134217728</type>
                <name>Frameable response (potential Clickjacking)</name>
                <host ip="192.168.1.100">https://example.com</host>
                <path>/admin</path>
                <location>https://example.com/admin</location>
                <severity>Information</severity>
                <confidence>Firm</confidence>
                <issueBackground><![CDATA[The application response does not include either X-Frame-Options or Content-Security-Policy headers.]]></issueBackground>
                <remediationBackground><![CDATA[To prevent framing attacks, the application should return a response header with an appropriate directive.]]></remediationBackground>
                <issueDetail><![CDATA[The response can be framed.]]></issueDetail>
                <remediationDetail><![CDATA[Use X-Frame-Options: DENY or appropriate CSP frame-ancestors directive.]]></remediationDetail>
              </issue>
            </issues>
            """;
    }
    
    @Test
    void testParseBurpExport() throws IOException {
        // Create test XML file
        Path xmlFile = tempDir.resolve("burp_export.xml");
        Files.writeString(xmlFile, sampleBurpXml);
        
        BurpSuiteParser.BurpParseResult result = BurpSuiteParser.parseBurpExport(xmlFile.toString());
        
        assertNotNull(result);
        assertEquals(xmlFile.toString(), result.getSourceFile());
        assertEquals(4, result.getFindings().size());
        assertEquals(2, result.getTargets().size()); // example.com and app.test.com
        
        // Check that all targets are found
        assertTrue(result.getTargets().contains("https://example.com"));
        assertTrue(result.getTargets().contains("https://app.test.com"));
    }
    
    @Test
    void testSeverityMapping() throws IOException {
        Path xmlFile = tempDir.resolve("burp_export.xml");
        Files.writeString(xmlFile, sampleBurpXml);
        
        BurpSuiteParser.BurpParseResult result = BurpSuiteParser.parseBurpExport(xmlFile.toString());
        List<BurpSuiteParser.BurpFinding> findings = result.getFindings();
        
        // Check severity mapping
        BurpSuiteParser.BurpFinding xssVuln = findings.stream()
            .filter(f -> f.getIssueName().contains("Cross-site scripting"))
            .findFirst().orElse(null);
        assertNotNull(xssVuln);
        assertEquals(Severity.CRITICAL, xssVuln.getSeverity()); // High -> Critical
        
        BurpSuiteParser.BurpFinding sqlVuln = findings.stream()
            .filter(f -> f.getIssueName().contains("SQL injection"))
            .findFirst().orElse(null);
        assertNotNull(sqlVuln);
        assertEquals(Severity.CRITICAL, sqlVuln.getSeverity()); // High -> Critical
        
        BurpSuiteParser.BurpFinding lowVuln = findings.stream()
            .filter(f -> f.getIssueName().contains("Password field"))
            .findFirst().orElse(null);
        assertNotNull(lowVuln);
        assertEquals(Severity.MEDIUM, lowVuln.getSeverity()); // Low -> Medium
        
        BurpSuiteParser.BurpFinding infoVuln = findings.stream()
            .filter(f -> f.getIssueName().contains("Frameable response"))
            .findFirst().orElse(null);
        assertNotNull(infoVuln);
        assertEquals(Severity.LOW, infoVuln.getSeverity()); // Information -> Low
    }
    
    @Test
    void testFindingDetails() throws IOException {
        Path xmlFile = tempDir.resolve("burp_export.xml");
        Files.writeString(xmlFile, sampleBurpXml);
        
        BurpSuiteParser.BurpParseResult result = BurpSuiteParser.parseBurpExport(xmlFile.toString());
        List<BurpSuiteParser.BurpFinding> findings = result.getFindings();
        
        BurpSuiteParser.BurpFinding xssVuln = findings.stream()
            .filter(f -> f.getIssueName().contains("Cross-site scripting"))
            .findFirst().orElse(null);
        
        assertNotNull(xssVuln);
        assertEquals("Cross-site scripting (reflected)", xssVuln.getIssueName());
        assertEquals("5243392", xssVuln.getIssueType());
        assertEquals("https://example.com", xssVuln.getHost());
        assertEquals("/search", xssVuln.getPath());
        assertEquals("https://example.com/search?q=[XSS]", xssVuln.getLocation());
        assertEquals("Certain", xssVuln.getConfidence());
        
        assertNotNull(xssVuln.getIssueBackground());
        assertTrue(xssVuln.getIssueBackground().contains("Reflected cross-site scripting"));
        
        assertNotNull(xssVuln.getRemediationBackground());
        assertTrue(xssVuln.getRemediationBackground().contains("two layers of defenses"));
        
        assertNotNull(xssVuln.getRequestResponse());
        assertTrue(xssVuln.getRequestResponse().contains("GET /search"));
        assertTrue(xssVuln.getRequestResponse().contains("200 OK"));
    }
    
    @Test
    void testVulnerabilityConversion() throws IOException {
        Path xmlFile = tempDir.resolve("burp_export.xml");
        Files.writeString(xmlFile, sampleBurpXml);
        
        BurpSuiteParser.BurpParseResult result = BurpSuiteParser.parseBurpExport(xmlFile.toString());
        List<BurpSuiteParser.BurpFinding> findings = result.getFindings();
        
        BurpSuiteParser.BurpFinding finding = findings.get(0);
        Vulnerability vuln = finding.toVulnerability();
        
        assertNotNull(vuln);
        assertEquals(finding.getIssueName(), vuln.getName());
        assertEquals(finding.getSeverity(), vuln.getSeverity());
        
        String description = vuln.getDescription();
        assertTrue(description.contains("Burp Suite Finding"));
        assertTrue(description.contains("Type: " + finding.getIssueType()));
        assertTrue(description.contains("Location: " + finding.getLocation()));
        assertTrue(description.contains("Confidence: " + finding.getConfidence()));
    }
    
    @Test
    void testFindingsByHost() throws IOException {
        Path xmlFile = tempDir.resolve("burp_export.xml");
        Files.writeString(xmlFile, sampleBurpXml);
        
        BurpSuiteParser.BurpParseResult result = BurpSuiteParser.parseBurpExport(xmlFile.toString());
        
        List<BurpSuiteParser.BurpFinding> exampleComFindings = result.getFindingsByHost("https://example.com");
        assertEquals(3, exampleComFindings.size());
        
        List<BurpSuiteParser.BurpFinding> testComFindings = result.getFindingsByHost("https://app.test.com");
        assertEquals(1, testComFindings.size());
        
        List<BurpSuiteParser.BurpFinding> noFindings = result.getFindingsByHost("https://nonexistent.com");
        assertEquals(0, noFindings.size());
    }
    
    @Test
    void testFormattedSummary() throws IOException {
        Path xmlFile = tempDir.resolve("burp_export.xml");
        Files.writeString(xmlFile, sampleBurpXml);
        
        BurpSuiteParser.BurpParseResult result = BurpSuiteParser.parseBurpExport(xmlFile.toString());
        String summary = result.getFormattedSummary();
        
        assertNotNull(summary);
        assertTrue(summary.contains("Burp Suite Import Summary"));
        assertTrue(summary.contains("Total Findings: 4"));
        assertTrue(summary.contains("Targets: 2"));
        assertTrue(summary.contains("Severity Breakdown"));
        assertTrue(summary.contains("Critical: 2"));
        assertTrue(summary.contains("Medium: 1"));
        assertTrue(summary.contains("Low: 1"));
        assertTrue(summary.contains("Target Hosts"));
        assertTrue(summary.contains("example.com"));
        assertTrue(summary.contains("app.test.com"));
    }
    
    @Test
    void testBurpFileValidation() throws IOException {
        // Test with valid Burp file
        Path validBurpFile = tempDir.resolve("valid_burp.xml");
        Files.writeString(validBurpFile, sampleBurpXml);
        assertTrue(BurpSuiteParser.isBurpExportFile(validBurpFile.toString()));
        
        // Test with non-Burp XML
        Path nonBurpXml = tempDir.resolve("not_burp.xml");
        Files.writeString(nonBurpXml, """
            <?xml version="1.0"?>
            <project>
                <name>Test Project</name>
                <version>1.0</version>
            </project>
            """);
        assertFalse(BurpSuiteParser.isBurpExportFile(nonBurpXml.toString()));
        
        // Test with non-XML file
        Path textFile = tempDir.resolve("test.txt");
        Files.writeString(textFile, "This is not XML");
        assertFalse(BurpSuiteParser.isBurpExportFile(textFile.toString()));
        
        // Test with non-existent file
        assertFalse(BurpSuiteParser.isBurpExportFile("/nonexistent/file.xml"));
        
        // Test with null path
        assertFalse(BurpSuiteParser.isBurpExportFile(null));
        
        // Test with non-XML extension
        assertFalse(BurpSuiteParser.isBurpExportFile("file.txt"));
    }
}