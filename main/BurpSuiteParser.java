package com.example;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;

/**
 * Parser for Burp Suite XML export files to extract vulnerability information.
 * 
 * This class processes Burp Suite XML exports and converts them into
 * Agent-Orange compatible vulnerability data structures, including
 * mapping Burp's severity levels to Agent-Orange's Severity enum.
 * 
 * @author Agent-Orange Team
 * @version 1.0
 */
public class BurpSuiteParser {
    private static final Logger logger = Logger.getLogger(BurpSuiteParser.class.getName());
    
    // Mapping from Burp Suite severity levels to Agent-Orange Severity
    private static final Map<String, Severity> SEVERITY_MAPPING = new HashMap<>();
    
    static {
        SEVERITY_MAPPING.put("High", Severity.CRITICAL);
        SEVERITY_MAPPING.put("Medium", Severity.HIGH);
        SEVERITY_MAPPING.put("Low", Severity.MEDIUM);
        SEVERITY_MAPPING.put("Information", Severity.LOW);
        SEVERITY_MAPPING.put("False positive", Severity.LOW);
    }
    
    /**
     * Represents a parsed Burp Suite finding.
     */
    public static class BurpFinding {
        private final String issueType;
        private final String issueName;
        private final String host;
        private final String path;
        private final String location;
        private final Severity severity;
        private final String confidence;
        private final String issueBackground;
        private final String remediationBackground;
        private final String issueDetail;
        private final String remediationDetail;
        private final String requestResponse;
        private final LocalDateTime discoveredAt;
        
        public BurpFinding(String issueType, String issueName, String host, String path,
                          String location, Severity severity, String confidence,
                          String issueBackground, String remediationBackground,
                          String issueDetail, String remediationDetail,
                          String requestResponse) {
            this.issueType = issueType;
            this.issueName = issueName;
            this.host = host;
            this.path = path;
            this.location = location;
            this.severity = severity;
            this.confidence = confidence;
            this.issueBackground = issueBackground;
            this.remediationBackground = remediationBackground;
            this.issueDetail = issueDetail;
            this.remediationDetail = remediationDetail;
            this.requestResponse = requestResponse;
            this.discoveredAt = LocalDateTime.now();
        }
        
        // Getters
        public String getIssueType() { return issueType; }
        public String getIssueName() { return issueName; }
        public String getHost() { return host; }
        public String getPath() { return path; }
        public String getLocation() { return location; }
        public Severity getSeverity() { return severity; }
        public String getConfidence() { return confidence; }
        public String getIssueBackground() { return issueBackground; }
        public String getRemediationBackground() { return remediationBackground; }
        public String getIssueDetail() { return issueDetail; }
        public String getRemediationDetail() { return remediationDetail; }
        public String getRequestResponse() { return requestResponse; }
        public LocalDateTime getDiscoveredAt() { return discoveredAt; }
        
        /**
         * Converts this Burp finding to an Agent-Orange Vulnerability.
         * 
         * @return Vulnerability object
         */
        public Vulnerability toVulnerability() {
            String description = buildDescription();
            return new Vulnerability(
                issueType, 
                issueName, 
                description, 
                severity, 
                host != null ? host : "Unknown"
            );
        }
        
        /**
         * Builds a comprehensive description from all available Burp data.
         * 
         * @return Formatted description
         */
        private String buildDescription() {
            StringBuilder desc = new StringBuilder();
            
            desc.append("🔍 Burp Suite Finding\n");
            desc.append("Type: ").append(issueType).append("\n");
            desc.append("Location: ").append(location != null ? location : (host + path)).append("\n");
            desc.append("Confidence: ").append(confidence).append("\n\n");
            
            if (issueBackground != null && !issueBackground.trim().isEmpty()) {
                desc.append("📋 Background:\n").append(cleanHtml(issueBackground)).append("\n\n");
            }
            
            if (issueDetail != null && !issueDetail.trim().isEmpty()) {
                desc.append("🔎 Details:\n").append(cleanHtml(issueDetail)).append("\n\n");
            }
            
            if (remediationBackground != null && !remediationBackground.trim().isEmpty()) {
                desc.append("🛠️ Remediation:\n").append(cleanHtml(remediationBackground)).append("\n\n");
            }
            
            if (remediationDetail != null && !remediationDetail.trim().isEmpty()) {
                desc.append("🔧 Remediation Steps:\n").append(cleanHtml(remediationDetail)).append("\n\n");
            }
            
            desc.append("📅 Discovered: ").append(discoveredAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            return desc.toString();
        }
        
        /**
         * Removes HTML tags and entities from text.
         * 
         * @param html HTML text to clean
         * @return Plain text
         */
        private String cleanHtml(String html) {
            if (html == null) return "";
            
            return html
                .replaceAll("<[^>]+>", "")  // Remove HTML tags
                .replaceAll("&lt;", "<")    // HTML entities
                .replaceAll("&gt;", ">")
                .replaceAll("&amp;", "&")
                .replaceAll("&quot;", "\"")
                .replaceAll("&apos;", "'")
                .replaceAll("\\s+", " ")    // Normalize whitespace
                .trim();
        }
        
        @Override
        public String toString() {
            return String.format("BurpFinding{name='%s', host='%s', severity=%s, confidence='%s'}", 
                               issueName, host, severity, confidence);
        }
    }
    
    /**
     * Represents the complete results of parsing a Burp Suite export.
     */
    public static class BurpParseResult {
        private final List<BurpFinding> findings;
        private final Map<String, String> scanInfo;
        private final Set<String> targets;
        private final LocalDateTime parsedAt;
        private final String sourceFile;
        
        public BurpParseResult(List<BurpFinding> findings, Map<String, String> scanInfo, 
                              Set<String> targets, String sourceFile) {
            this.findings = new ArrayList<>(findings);
            this.scanInfo = new HashMap<>(scanInfo);
            this.targets = new HashSet<>(targets);
            this.parsedAt = LocalDateTime.now();
            this.sourceFile = sourceFile;
        }
        
        public List<BurpFinding> getFindings() { return new ArrayList<>(findings); }
        public Map<String, String> getScanInfo() { return new HashMap<>(scanInfo); }
        public Set<String> getTargets() { return new HashSet<>(targets); }
        public LocalDateTime getParsedAt() { return parsedAt; }
        public String getSourceFile() { return sourceFile; }
        
        /**
         * Gets findings by severity level.
         * 
         * @param severity The severity to filter by
         * @return List of findings with the specified severity
         */
        public List<BurpFinding> getFindingsBySeverity(Severity severity) {
            return findings.stream()
                          .filter(f -> f.getSeverity() == severity)
                          .collect(java.util.stream.Collectors.toList());
        }
        
        /**
         * Gets findings by host.
         * 
         * @param host The host to filter by
         * @return List of findings for the specified host
         */
        public List<BurpFinding> getFindingsByHost(String host) {
            return findings.stream()
                          .filter(f -> f.getHost().equals(host))
                          .collect(java.util.stream.Collectors.toList());
        }
        
        /**
         * Gets a summary of findings by severity.
         * 
         * @return Map of severity to count
         */
        public Map<Severity, Integer> getSeveritySummary() {
            Map<Severity, Integer> summary = new HashMap<>();
            for (Severity sev : Severity.values()) {
                summary.put(sev, 0);
            }
            
            for (BurpFinding finding : findings) {
                summary.merge(finding.getSeverity(), 1, Integer::sum);
            }
            
            return summary;
        }
        
        /**
         * Creates a formatted summary of the parse results.
         * 
         * @return Formatted summary string
         */
        public String getFormattedSummary() {
            StringBuilder summary = new StringBuilder();
            
            summary.append("📊 Burp Suite Import Summary\n");
            summary.append("════════════════════════════════\n");
            summary.append("📁 Source File: ").append(sourceFile).append("\n");
            summary.append("⏰ Parsed: ").append(parsedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
            summary.append("🎯 Targets: ").append(targets.size()).append("\n");
            summary.append("🔍 Total Findings: ").append(findings.size()).append("\n\n");
            
            // Severity breakdown
            Map<Severity, Integer> severityCount = getSeveritySummary();
            summary.append("📈 Severity Breakdown:\n");
            for (Severity severity : Severity.values()) {
                int count = severityCount.get(severity);
                if (count > 0) {
                    summary.append("  ").append(severity.getIcon()).append(" ")
                           .append(severity.getDisplayName()).append(": ").append(count).append("\n");
                }
            }
            
            summary.append("\n🎯 Target Hosts:\n");
            for (String target : targets) {
                summary.append("  • ").append(target).append("\n");
            }
            
            if (!scanInfo.isEmpty()) {
                summary.append("\nℹ️ Scan Information:\n");
                for (Map.Entry<String, String> entry : scanInfo.entrySet()) {
                    summary.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                }
            }
            
            return summary.toString();
        }
        
        @Override
        public String toString() {
            return String.format("BurpParseResult{findings=%d, targets=%d, file='%s'}", 
                               findings.size(), targets.size(), sourceFile);
        }
    }
    
    /**
     * Parses a Burp Suite XML export file.
     * 
     * @param xmlFilePath Path to the Burp Suite XML export file
     * @return BurpParseResult containing all parsed data
     * @throws IOException if file reading fails
     * @throws IllegalArgumentException if XML parsing fails
     */
    public static BurpParseResult parseBurpExport(String xmlFilePath) throws IOException {
        if (xmlFilePath == null || xmlFilePath.trim().isEmpty()) {
            throw new IllegalArgumentException("XML file path cannot be null or empty");
        }
        
        File xmlFile = new File(xmlFilePath);
        if (!xmlFile.exists()) {
            throw new IOException("Burp export file not found: " + xmlFilePath);
        }
        
        if (!xmlFile.canRead()) {
            throw new IOException("Cannot read Burp export file: " + xmlFilePath);
        }
        
        logger.info("Parsing Burp Suite export: " + xmlFilePath + " (" + xmlFile.length() + " bytes)");
        
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(xmlFile);
            document.getDocumentElement().normalize();
            
            List<BurpFinding> findings = new ArrayList<>();
            Map<String, String> scanInfo = new HashMap<>();
            Set<String> targets = new HashSet<>();
            
            // Parse scan information
            parseScanInfo(document, scanInfo);
            
            // Parse issues (vulnerabilities)
            NodeList issueNodes = document.getElementsByTagName("issue");
            logger.info("Found " + issueNodes.getLength() + " issues in Burp export");
            
            for (int i = 0; i < issueNodes.getLength(); i++) {
                Node issueNode = issueNodes.item(i);
                if (issueNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element issueElement = (Element) issueNode;
                    BurpFinding finding = parseIssue(issueElement);
                    if (finding != null) {
                        findings.add(finding);
                        targets.add(finding.getHost());
                    }
                }
            }
            
            logger.info("Successfully parsed " + findings.size() + " findings from Burp export");
            
            return new BurpParseResult(findings, scanInfo, targets, xmlFilePath);
            
        } catch (Exception e) {
            logger.severe("Failed to parse Burp export: " + e.getMessage());
            throw new IllegalArgumentException("Failed to parse Burp XML export: " + e.getMessage(), e);
        }
    }
    
    /**
     * Parses scan information from the XML document.
     * 
     * @param document The XML document
     * @param scanInfo Map to store scan information
     */
    private static void parseScanInfo(Document document, Map<String, String> scanInfo) {
        try {
            // Look for scan metadata
            NodeList scanNodes = document.getElementsByTagName("scan");
            if (scanNodes.getLength() > 0) {
                Element scanElement = (Element) scanNodes.item(0);
                
                // Extract scan attributes
                NamedNodeMap attributes = scanElement.getAttributes();
                for (int i = 0; i < attributes.getLength(); i++) {
                    Node attr = attributes.item(i);
                    scanInfo.put("scan_" + attr.getNodeName(), attr.getNodeValue());
                }
            }
            
            // Look for other metadata elements
            String[] metadataElements = {"burpVersion", "exportTime", "scanMetrics"};
            for (String elementName : metadataElements) {
                NodeList nodes = document.getElementsByTagName(elementName);
                if (nodes.getLength() > 0) {
                    scanInfo.put(elementName, nodes.item(0).getTextContent());
                }
            }
            
        } catch (Exception e) {
            logger.warning("Failed to parse scan info: " + e.getMessage());
        }
    }
    
    /**
     * Parses a single issue element from the XML.
     * 
     * @param issueElement The issue XML element
     * @return BurpFinding object or null if parsing fails
     */
    private static BurpFinding parseIssue(Element issueElement) {
        try {
            String issueType = getElementText(issueElement, "type");
            String issueName = getElementText(issueElement, "name");
            String host = getElementText(issueElement, "host");
            String path = getElementText(issueElement, "path");
            String location = getElementText(issueElement, "location");
            String severityText = getElementText(issueElement, "severity");
            String confidence = getElementText(issueElement, "confidence");
            String issueBackground = getElementText(issueElement, "issueBackground");
            String remediationBackground = getElementText(issueElement, "remediationBackground");
            String issueDetail = getElementText(issueElement, "issueDetail");
            String remediationDetail = getElementText(issueElement, "remediationDetail");
            
            // Parse request/response data
            String requestResponse = parseRequestResponse(issueElement);
            
            // Map Burp severity to Agent-Orange severity
            Severity severity = mapSeverity(severityText);
            
            // Create location string if not provided
            if (location == null || location.trim().isEmpty()) {
                location = (host != null ? host : "") + (path != null ? path : "");
            }
            
            // Validate required fields
            if (issueName == null || issueName.trim().isEmpty()) {
                logger.warning("Skipping issue with missing name");
                return null;
            }
            
            return new BurpFinding(
                issueType, issueName, host, path, location, severity, confidence,
                issueBackground, remediationBackground, issueDetail, remediationDetail,
                requestResponse
            );
            
        } catch (Exception e) {
            logger.warning("Failed to parse issue: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Extracts text content from a child element.
     * 
     * @param parent Parent element
     * @param childName Name of child element
     * @return Text content or null if not found
     */
    private static String getElementText(Element parent, String childName) {
        NodeList nodes = parent.getElementsByTagName(childName);
        if (nodes.getLength() > 0) {
            String text = nodes.item(0).getTextContent();
            return text != null ? text.trim() : null;
        }
        return null;
    }
    
    /**
     * Parses request/response data from the issue element.
     * 
     * @param issueElement The issue XML element
     * @return Formatted request/response string
     */
    private static String parseRequestResponse(Element issueElement) {
        StringBuilder reqResp = new StringBuilder();
        
        try {
            // Parse request data
            NodeList requestNodes = issueElement.getElementsByTagName("request");
            if (requestNodes.getLength() > 0) {
                Element requestElement = (Element) requestNodes.item(0);
                String method = requestElement.getAttribute("method");
                String url = requestElement.getAttribute("url");
                String requestData = requestElement.getTextContent();
                
                if (method != null && !method.isEmpty()) {
                    reqResp.append("Method: ").append(method).append("\n");
                }
                if (url != null && !url.isEmpty()) {
                    reqResp.append("URL: ").append(url).append("\n");
                }
                if (requestData != null && !requestData.trim().isEmpty()) {
                    reqResp.append("Request:\n").append(requestData.trim()).append("\n\n");
                }
            }
            
            // Parse response data
            NodeList responseNodes = issueElement.getElementsByTagName("response");
            if (responseNodes.getLength() > 0) {
                Element responseElement = (Element) responseNodes.item(0);
                String statusCode = responseElement.getAttribute("statusCode");
                String responseData = responseElement.getTextContent();
                
                if (statusCode != null && !statusCode.isEmpty()) {
                    reqResp.append("Status: ").append(statusCode).append("\n");
                }
                if (responseData != null && !responseData.trim().isEmpty()) {
                    String truncatedResponse = responseData.trim();
                    if (truncatedResponse.length() > 1000) {
                        truncatedResponse = truncatedResponse.substring(0, 1000) + "... [truncated]";
                    }
                    reqResp.append("Response:\n").append(truncatedResponse).append("\n");
                }
            }
            
        } catch (Exception e) {
            logger.warning("Failed to parse request/response: " + e.getMessage());
        }
        
        return reqResp.toString();
    }
    
    /**
     * Maps Burp Suite severity to Agent-Orange Severity enum.
     * 
     * @param burpSeverity Burp Suite severity string
     * @return Mapped Severity enum value
     */
    private static Severity mapSeverity(String burpSeverity) {
        if (burpSeverity == null || burpSeverity.trim().isEmpty()) {
            return Severity.LOW;
        }
        
        String normalized = burpSeverity.trim();
        Severity mapped = SEVERITY_MAPPING.get(normalized);
        
        if (mapped != null) {
            return mapped;
        }
        
        // Fallback mapping based on common patterns
        String lower = normalized.toLowerCase();
        if (lower.contains("critical") || lower.contains("high")) {
            return Severity.CRITICAL;
        } else if (lower.contains("medium") || lower.contains("moderate")) {
            return Severity.HIGH;
        } else if (lower.contains("low") || lower.contains("minor")) {
            return Severity.MEDIUM;
        } else {
            return Severity.LOW;
        }
    }
    
    /**
     * Validates a Burp Suite XML file format.
     * 
     * @param xmlFilePath Path to the XML file
     * @return true if the file appears to be a valid Burp export
     * @throws IOException if file reading fails
     */
    public static boolean isBurpExportFile(String xmlFilePath) throws IOException {
        if (xmlFilePath == null || !xmlFilePath.toLowerCase().endsWith(".xml")) {
            return false;
        }
        
        File file = new File(xmlFilePath);
        if (!file.exists() || !file.canRead()) {
            return false;
        }
        
        // Quick validation by checking for Burp-specific elements
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineCount = 0;
            boolean foundBurpIndicator = false;
            
            while ((line = reader.readLine()) != null && lineCount < 50) {
                lineCount++;
                String lowerLine = line.toLowerCase();
                
                if (lowerLine.contains("<issues>") || 
                    lowerLine.contains("<issue>") ||
                    lowerLine.contains("burp") ||
                    lowerLine.contains("portswigger")) {
                    foundBurpIndicator = true;
                    break;
                }
            }
            
            return foundBurpIndicator;
            
        } catch (Exception e) {
            logger.warning("Failed to validate Burp export file: " + e.getMessage());
            return false;
        }
    }
}