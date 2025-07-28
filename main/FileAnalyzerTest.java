package com.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Unit tests for FileAnalyzer class.
 * 
 * Tests file analysis capabilities, security pattern detection,
 * and proper handling of different file types.
 * 
 * @author Agent-Orange Team
 * @version 1.0
 */
public class FileAnalyzerTest {
    
    private FileAnalyzer fileAnalyzer;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        // Use permissive settings for testing
        Set<String> allowedPaths = Set.of(tempDir.toString());
        fileAnalyzer = new FileAnalyzer(allowedPaths, Collections.emptySet(), 1024 * 1024, false);
    }
    
    @Test
    void testAnalyzeConfigurationFile() throws IOException {
        // Create a test configuration file
        Path configFile = tempDir.resolve("test.properties");
        String content = """
            # Test configuration
            database.password=secret123
            api.key=sk-abcd1234567890
            server.host=192.168.1.100
            server.port=8080
            debug.enabled=true
            """;
        Files.writeString(configFile, content);
        
        ExtractedData result = fileAnalyzer.analyzeFile(configFile.toString());
        
        assertNotNull(result);
        assertEquals(configFile.toString(), result.getFilePath());
        assertEquals(FileType.CONFIGURATION, result.getFileType());
        assertTrue(result.hasSensitiveData());
        assertTrue(result.getTotalItemCount() > 0);
        
        // Check that credentials were detected
        List<String> credentials = result.getExtractedInfo(ExtractedData.DataType.CREDENTIAL);
        assertFalse(credentials.isEmpty());
        assertTrue(credentials.stream().anyMatch(c -> c.contains("secret123")));
        
        // Check that API keys were detected
        List<String> apiKeys = result.getExtractedInfo(ExtractedData.DataType.API_KEY);
        assertFalse(apiKeys.isEmpty());
        assertTrue(apiKeys.stream().anyMatch(k -> k.contains("sk-abcd1234567890")));
        
        // Check network info
        List<String> networkInfo = result.getExtractedInfo(ExtractedData.DataType.NETWORK_INFO);
        assertFalse(networkInfo.isEmpty());
        assertTrue(networkInfo.stream().anyMatch(n -> n.contains("192.168.1.100")));
    }
    
    @Test
    void testAnalyzeSourceCodeFile() throws IOException {
        // Create a test source code file
        Path sourceFile = tempDir.resolve("test.java");
        String content = """
            public class TestApp {
                private static final String SECRET_KEY = "abcd1234567890123456";
                private static final String API_ENDPOINT = "https://api.example.com/v1";
                
                public void apiCall() {
                    String token = "bearer_token_12345";
                    // Make API call
                }
                
                public void databaseConnect() {
                    String dbPassword = "mypassword123";
                    // Connect to database
                }
            }
            """;
        Files.writeString(sourceFile, content);
        
        ExtractedData result = fileAnalyzer.analyzeFile(sourceFile.toString());
        
        assertNotNull(result);
        assertEquals(FileType.SOURCE_CODE, result.getFileType());
        assertTrue(result.hasSensitiveData());
        
        // Check for extracted secrets
        List<String> secrets = result.getExtractedInfo(ExtractedData.DataType.SECRET);
        assertFalse(secrets.isEmpty());
        
        // Check for endpoints
        List<String> endpoints = result.getExtractedInfo(ExtractedData.DataType.ENDPOINT);
        assertTrue(endpoints.stream().anyMatch(e -> e.contains("api.example.com")));
    }
    
    @Test
    void testAnalyzeLogFile() throws IOException {
        // Create a test log file
        Path logFile = tempDir.resolve("access.log");
        String content = """
            192.168.1.50 - - [01/Jan/2024:12:00:00 +0000] "GET /api/users HTTP/1.1" 200 1234
            10.0.0.1 - - [01/Jan/2024:12:01:00 +0000] "POST /login HTTP/1.1" 200 567
            ERROR: SQL error in query execution
            403.168.1.100 - - [01/Jan/2024:12:02:00 +0000] "GET https://example.com/admin HTTP/1.1" 403 0
            EXCEPTION: java.lang.NullPointerException at line 123
            """;
        Files.writeString(logFile, content);
        
        ExtractedData result = fileAnalyzer.analyzeFile(logFile.toString());
        
        assertNotNull(result);
        assertEquals(FileType.LOG, result.getFileType());
        
        // Check for extracted network info (IP addresses)
        List<String> networkInfo = result.getExtractedInfo(ExtractedData.DataType.NETWORK_INFO);
        assertTrue(networkInfo.stream().anyMatch(n -> n.contains("192.168.1.50")));
        assertTrue(networkInfo.stream().anyMatch(n -> n.contains("10.0.0.1")));
        
        // Check for endpoints
        List<String> endpoints = result.getExtractedInfo(ExtractedData.DataType.ENDPOINT);
        assertTrue(endpoints.stream().anyMatch(e -> e.contains("example.com")));
        
        // Check for vulnerabilities (error patterns)
        List<String> vulnerabilities = result.getExtractedInfo(ExtractedData.DataType.VULNERABILITY);
        assertTrue(vulnerabilities.stream().anyMatch(v -> v.toLowerCase().contains("sql")));
        assertTrue(vulnerabilities.stream().anyMatch(v -> v.toLowerCase().contains("exception")));
    }
    
    @Test
    void testAnalyzeDockerFile() throws IOException {
        // Create a test Dockerfile
        Path dockerFile = tempDir.resolve("Dockerfile");
        String content = """
            FROM node:16-alpine
            WORKDIR /app
            
            ENV NODE_ENV=production
            ENV SECRET_KEY=my-secret-key-123
            ENV DATABASE_URL=postgresql://user:pass@db:5432/myapp
            
            EXPOSE 3000
            EXPOSE 8080
            
            COPY package*.json ./
            RUN npm install --production
            
            COPY . .
            
            CMD ["node", "server.js"]
            """;
        Files.writeString(dockerFile, content);
        
        ExtractedData result = fileAnalyzer.analyzeFile(dockerFile.toString());
        
        assertNotNull(result);
        assertEquals(FileType.DEPLOYMENT, result.getFileType());
        assertTrue(result.hasSensitiveData());
        
        // Check for version info (Docker image)
        List<String> versionInfo = result.getExtractedInfo(ExtractedData.DataType.VERSION_INFO);
        assertTrue(versionInfo.stream().anyMatch(v -> v.contains("node:16-alpine")));
        
        // Check for network info (exposed ports)
        List<String> networkInfo = result.getExtractedInfo(ExtractedData.DataType.NETWORK_INFO);
        assertTrue(networkInfo.stream().anyMatch(n -> n.contains("3000")));
        assertTrue(networkInfo.stream().anyMatch(n -> n.contains("8080")));
        
        // Check for secrets in environment variables
        List<String> secrets = result.getExtractedInfo(ExtractedData.DataType.SECRET);
        assertTrue(secrets.stream().anyMatch(s -> s.contains("SECRET_KEY")));
        
        // Check for database connections
        List<String> dbConnections = result.getExtractedInfo(ExtractedData.DataType.DATABASE_CONNECTION);
        assertTrue(dbConnections.stream().anyMatch(d -> d.contains("postgresql")));
    }
    
    @Test
    void testAnalyzeMultipleFiles() throws IOException {
        // Create multiple test files
        Path file1 = tempDir.resolve("config.properties");
        Files.writeString(file1, "password=secret1\napi.key=key1");
        
        Path file2 = tempDir.resolve("app.py");
        Files.writeString(file2, "SECRET = 'secret2'\nAPI_URL = 'https://api.test.com'");
        
        Path file3 = tempDir.resolve("readme.txt");
        Files.writeString(file3, "This is a readme file with no sensitive data.");
        
        List<String> filePaths = List.of(
            file1.toString(),
            file2.toString(),
            file3.toString()
        );
        
        Map<String, ExtractedData> results = fileAnalyzer.analyzeFiles(filePaths);
        
        assertEquals(3, results.size());
        assertTrue(results.containsKey(file1.toString()));
        assertTrue(results.containsKey(file2.toString()));
        assertTrue(results.containsKey(file3.toString()));
        
        // Check that sensitive files are properly identified
        ExtractedData configResult = results.get(file1.toString());
        assertTrue(configResult.hasSensitiveData());
        
        ExtractedData sourceResult = results.get(file2.toString());
        assertTrue(sourceResult.hasSensitiveData());
        
        ExtractedData readmeResult = results.get(file3.toString());
        assertFalse(readmeResult.hasSensitiveData());
    }
    
    @Test
    void testAnalyzeDirectory() throws IOException {
        // Create a directory structure
        Path subDir = tempDir.resolve("subdir");
        Files.createDirectories(subDir);
        
        // Create files in main directory
        Files.writeString(tempDir.resolve("config.ini"), "password=test123");
        Files.writeString(tempDir.resolve("readme.md"), "# Project README");
        
        // Create files in subdirectory
        Files.writeString(subDir.resolve("secret.txt"), "api_key=secret456");
        Files.writeString(subDir.resolve("data.json"), "{\"key\": \"value\"}");
        
        Map<String, ExtractedData> results = fileAnalyzer.analyzeDirectory(tempDir.toString(), true, 10);
        
        assertTrue(results.size() >= 3); // At least config.ini, secret.txt, and some others
        
        // Verify that files from both main and subdirectory are included
        boolean foundMainDirFile = results.keySet().stream().anyMatch(k -> k.contains("config.ini"));
        boolean foundSubDirFile = results.keySet().stream().anyMatch(k -> k.contains("secret.txt"));
        
        assertTrue(foundMainDirFile);
        assertTrue(foundSubDirFile);
    }
    
    @Test
    void testSecurityRestrictions() {
        // Create analyzer with strict security mode
        Set<String> allowedPaths = Set.of("/tmp/allowed");
        Set<String> blockedPaths = Set.of("/etc", "/root");
        FileAnalyzer restrictedAnalyzer = new FileAnalyzer(allowedPaths, blockedPaths, 1024, true);
        
        // Test blocked path
        assertThrows(SecurityException.class, () -> {
            restrictedAnalyzer.analyzeFile("/etc/passwd");
        });
        
        // Test path traversal attempt
        assertThrows(SecurityException.class, () -> {
            restrictedAnalyzer.analyzeFile("/tmp/allowed/../../../etc/passwd");
        });
    }
    
    @Test
    void testFileSizeLimit() throws IOException {
        // Create a file that exceeds the size limit
        FileAnalyzer limitedAnalyzer = new FileAnalyzer(
            Set.of(tempDir.toString()), Collections.emptySet(), 10, false); // 10 bytes limit
        
        Path largeFile = tempDir.resolve("large.txt");
        Files.writeString(largeFile, "This file is definitely larger than 10 bytes");
        
        assertThrows(IOException.class, () -> {
            limitedAnalyzer.analyzeFile(largeFile.toString());
        });
    }
    
    @Test
    void testFileTypeDetection() {
        assertEquals(FileType.CONFIGURATION, FileType.fromFilename("config.properties"));
        assertEquals(FileType.CONFIGURATION, FileType.fromFilename("app.yml"));
        assertEquals(FileType.CONFIGURATION, FileType.fromFilename(".env"));
        
        assertEquals(FileType.SOURCE_CODE, FileType.fromFilename("App.java"));
        assertEquals(FileType.SOURCE_CODE, FileType.fromFilename("script.py"));
        assertEquals(FileType.SOURCE_CODE, FileType.fromFilename("index.js"));
        
        assertEquals(FileType.LOG, FileType.fromFilename("access.log"));
        assertEquals(FileType.LOG, FileType.fromFilename("error.out"));
        
        assertEquals(FileType.DATABASE, FileType.fromFilename("data.sql"));
        assertEquals(FileType.DATABASE, FileType.fromFilename("app.db"));
        
        assertEquals(FileType.DEPLOYMENT, FileType.fromFilename("Dockerfile"));
        assertEquals(FileType.DEPLOYMENT, FileType.fromFilename("docker-compose.yml"));
        
        assertEquals(FileType.DOCUMENTATION, FileType.fromFilename("README.md"));
        assertEquals(FileType.DOCUMENTATION, FileType.fromFilename("docs.txt"));
        
        assertEquals(FileType.UNKNOWN, FileType.fromFilename("unknown.xyz"));
    }
    
    @Test
    void testExtractedDataSanitization() throws IOException {
        Path testFile = tempDir.resolve("secrets.conf");
        String content = """
            password=verylongpassword123456789
            api_key=short
            token=abcdefghijklmnopqrstuvwxyz123456
            """;
        Files.writeString(testFile, content);
        
        ExtractedData result = fileAnalyzer.analyzeFile(testFile.toString());
        
        // Get sanitized data
        Map<ExtractedData.DataType, List<String>> sanitizedData = result.getAllExtractedInfo(true);
        
        for (List<String> values : sanitizedData.values()) {
            for (String value : values) {
                // Verify that long values are sanitized (contain asterisks)
                if (value.length() > 8) {
                    assertTrue(value.contains("*"), "Long sensitive value should be sanitized: " + value);
                }
            }
        }
    }
    
    @Test
    void testCacheOperations() throws IOException {
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "test content");
        
        // First analysis
        ExtractedData result1 = fileAnalyzer.analyzeFile(testFile.toString());
        assertEquals(1, fileAnalyzer.getCacheSize());
        
        // Second analysis should use cache
        ExtractedData result2 = fileAnalyzer.analyzeFile(testFile.toString());
        assertEquals(result1, result2); // Should be same object from cache
        
        // Clear cache
        fileAnalyzer.clearCache();
        assertEquals(0, fileAnalyzer.getCacheSize());
        
        // Analysis after cache clear should create new object
        ExtractedData result3 = fileAnalyzer.analyzeFile(testFile.toString());
        assertNotEquals(result1, result3); // Different objects
    }
    
    @Test
    void testFormattedSummary() throws IOException {
        Path testFile = tempDir.resolve("summary.properties");
        String content = """
            # Test configuration
            password=test123
            api.endpoint=https://api.example.com
            debug=true
            """;
        Files.writeString(testFile, content);
        
        ExtractedData result = fileAnalyzer.analyzeFile(testFile.toString());
        String summary = result.getFormattedSummary(true);
        
        assertNotNull(summary);
        assertTrue(summary.contains("File Analysis"));
        assertTrue(summary.contains(testFile.toString()));
        assertTrue(summary.contains("Contains sensitive data"));
        assertTrue(summary.contains("FileAnalyzer"));
    }
    
    @Test
    void testErrorHandling() {
        // Test with non-existent file
        assertThrows(IOException.class, () -> {
            fileAnalyzer.analyzeFile("/nonexistent/file.txt");
        });
        
        // Test with null file path
        assertThrows(IllegalArgumentException.class, () -> {
            fileAnalyzer.analyzeFile(null);
        });
        
        // Test with empty file path
        assertThrows(IllegalArgumentException.class, () -> {
            fileAnalyzer.analyzeFile("");
        });
    }
    
    @Test
    void testShutdown() {
        fileAnalyzer.shutdown();
        // After shutdown, cache should be cleared
        assertEquals(0, fileAnalyzer.getCacheSize());
    }
}