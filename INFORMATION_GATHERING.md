# 🔍 Information Gathering & Burp Suite Integration

## Overview

Agent-Orange now includes powerful information gathering capabilities with automatic file analysis and Burp Suite integration. This enhancement enables comprehensive reconnaissance and vulnerability import from Burp Suite XML exports.

## 🎯 Features Implemented

### **1. Automatic File Analysis**

#### **Supported File Types**
- **Configuration files**: `.env`, `.properties`, `.xml`, `.yml`, `.yaml`, `.json`, `.ini`, `.conf`
- **Source code**: `.java`, `.py`, `.js`, `.php`, `.rb`, `.cs`, `.cpp`, `.c`, `.h`  
- **Web applications**: `.jsp`, `.asp`, `.aspx`, `.php`, `.html`, `.htm`, `.js`, `.css`, `.htaccess`
- **Database files**: `.sql`, `.db`, `.sqlite`, `.sqlite3`, `.mdb`, `.accdb`
- **Deployment configs**: `Dockerfile`, `docker-compose.yml`, Kubernetes manifests
- **Log files**: `.log`, `.out`, `.err`, access logs, error logs
- **Documentation**: `.md`, `.txt`, `.rst`, README files
- **Backup files**: `.bak`, `.backup`, `.old`, `.orig`, archives
- **Certificates**: `.pem`, `.key`, `.crt`, `.cer`, `.p12`, `.pfx`, `.jks`

#### **Data Extraction Capabilities**
- **🔑 Credentials**: Passwords, secrets, authentication tokens
- **🗝️ API Keys**: Service keys, access tokens, bearer tokens
- **🗄️ Database Connections**: JDBC URLs, MongoDB connections, Redis URLs
- **🌐 Endpoints**: URLs, API endpoints, form actions, AJAX endpoints
- **⚙️ Configuration**: Key-value pairs, environment variables
- **📋 Version Information**: Software versions, Docker images, dependencies
- **🚨 Vulnerabilities**: Error patterns, security misconfigurations
- **👤 User Information**: Usernames, email addresses, user accounts
- **📁 File Paths**: Script sources, imports, includes
- **🌐 Network Information**: IP addresses, hostnames, ports

#### **Security Features**
- **Sanitization**: Sensitive data is automatically sanitized for display
- **Scope Control**: Configurable allowed/blocked paths for security
- **Size Limits**: Configurable maximum file size for analysis
- **Access Control**: Security restrictions prevent unauthorized file access
- **Logging**: Comprehensive security logging of all analysis activities

### **2. Burp Suite Integration**

#### **XML Import Features**
- **Complete parsing** of Burp Suite XML export files
- **Automatic validation** to ensure file is a valid Burp export
- **Vulnerability extraction** with full details and metadata
- **Request/response data** preservation (truncated for display)
- **Target host discovery** from scan results

#### **Severity Mapping**
Agent-Orange automatically maps Burp Suite severity levels:
- Burp **"High"** → Agent-Orange **"Critical"**
- Burp **"Medium"** → Agent-Orange **"High"**  
- Burp **"Low"** → Agent-Orange **"Medium"**
- Burp **"Information"** → Agent-Orange **"Low"**
- Burp **"False positive"** → Agent-Orange **"Low"**

#### **Data Integration**
- **Project Integration**: Vulnerabilities are automatically added to active projects
- **Target Management**: Discovered hosts are added as project targets
- **Vulnerability Tree**: Findings are organized in the project's vulnerability structure
- **Rich Descriptions**: Full Burp details including background, remediation, and technical details

### **3. Natural Language Interface**

#### **Information Gathering Commands**
```
# Start information gathering
gather info on "Project Name"

# Analyze individual files
analyze file "/path/to/config.properties"
analyze file "/var/log/access.log"

# Analyze directories
analyze directory "/var/www/html"
scan files in "/opt/application/config"

# Query extracted data
list passwords in "Project Name"
show api keys in "Project Name"  
show endpoints found in "Project Name"
```

#### **Burp Suite Commands**
```
# Import Burp Suite data
import burp data from "/path/to/burp_export.xml"
import burp data from "scan_results.xml" into project "WebApp Audit"

# Query Burp findings
show burp vulnerabilities in "Project Name"
show burp vulnerabilities for example.com in "Project Name"
list burp findings in "Project Name"
```

#### **Session Management Commands**
```
# Manage gathering sessions
list gathering sessions
show gathering session "session_id"
close gathering session "session_id"

# Get help
help gathering
help burp suite integration
```

## 📁 Core Classes

### **FileAnalyzer**
- Main analysis engine coordinating different parsers
- Concurrent file processing for performance
- Security pattern detection using regex
- File type-specific extraction logic
- Caching system for repeated analyses

### **ExtractedData**  
- Container for analysis results with security classifications
- Data type categorization (credentials, endpoints, etc.)
- Automatic sanitization for sensitive information
- Comprehensive metadata and warning tracking
- Formatted summary generation

### **BurpSuiteParser**
- XML parsing with security-hardened configuration
- Complete vulnerability data extraction
- HTML content cleaning and normalization
- Severity mapping and target discovery
- Robust error handling for malformed XML

### **InformationGatherer**
- Session-based operation management
- Multi-file concurrent analysis
- Directory traversal with security controls
- Burp Suite integration orchestration
- Scope validation and security enforcement

### **InformationGatheringCommandProcessor**
- Natural language command interpretation
- Pattern-based command matching
- Context-aware response generation  
- Project integration and cross-referencing
- Help system and user guidance

## 🔧 Integration Points

### **MCPOrchestrator Integration**
- Seamless integration with existing project management
- Command priority system (projects → info gathering → other modules)
- Cross-module suggestions and tips
- Unified shutdown and cleanup

### **Project Manager Integration**
- Automatic project association for gathering sessions
- Target synchronization between Burp findings and projects
- Vulnerability integration with existing vulnerability tree
- Session-project lifecycle management

## 🛡️ Security Considerations

### **Data Protection**
- **Sanitization**: All sensitive data is sanitized before display
- **Logging**: Security events are logged but sensitive values are not
- **Scope Control**: File access is restricted to defined scopes
- **Path Validation**: Protection against directory traversal attacks

### **Access Control**
- **Allowed/Blocked Paths**: Configurable security boundaries
- **System Protection**: Built-in blocks for sensitive system paths
- **File Size Limits**: Prevention of resource exhaustion
- **Validation**: All inputs are validated before processing

### **Error Handling**
- **Graceful Degradation**: System continues operating on individual failures
- **Security Logging**: All security events are logged appropriately
- **Safe Defaults**: Conservative security settings by default
- **Exception Safety**: No sensitive data leakage in error messages

## 🧪 Testing

### **Unit Tests**
- **FileAnalyzerTest**: Comprehensive testing of file analysis capabilities
- **BurpSuiteParserTest**: Complete Burp Suite XML parsing validation
- **ExtractedDataTest**: Data container and sanitization testing
- **Security Tests**: Access control and validation testing

### **Test Coverage**
- File type detection and analysis
- Security pattern recognition  
- Burp Suite XML parsing edge cases
- Error handling and validation
- Sanitization and security features

## 📊 Example Usage

### **Complete Workflow Example**

```bash
# 1. Create a project
create project "Web Application Security Assessment"

# 2. Add targets
add target "example.com"
add target "api.example.com"

# 3. Start information gathering
gather info on "Web Application Security Assessment"

# 4. Analyze application files
analyze directory "/var/www/html"
analyze file "/etc/nginx/nginx.conf"

# 5. Import Burp Suite findings
import burp data from "/home/user/burp_scan.xml"

# 6. Review findings
list passwords in "Web Application Security Assessment"
show burp vulnerabilities in "Web Application Security Assessment"
show endpoints found in "Web Application Security Assessment"

# 7. Generate project report
generate report for "Web Application Security Assessment"
```

### **Output Examples**

#### File Analysis Output:
```
📁 File Analysis Complete
═══════════════════════════
📄 File: /var/www/config/database.php
📊 Items extracted: 8
🔒 Sensitive data: Yes

🔑 Credential (2):
  • db_password: [REDACTED]
  • admin_pass: [REDACTED]

🗄️ Database Connection (1):
  • mysql://localhost:3306/webapp

🌐 Network Info (2):
  • localhost
  • 3306

⚠️  Warnings:
  ⚠️  Sensitive credential detected in /var/www/config/database.php
```

#### Burp Suite Import Output:
```
🔍 Burp Suite Import Complete
═══════════════════════════════
📁 File: /home/user/burp_scan.xml
📊 Project: Web Application Security Assessment
🔍 Findings: 23
🎯 Targets: 3

📈 Severity Breakdown:
  🔴 Critical: 4
  🟠 High: 7  
  🟡 Medium: 8
  🟢 Low: 4

✅ Added 23 vulnerabilities to project

💡 Use 'list burp vulnerabilities in Web Application Security Assessment' to see findings
```

## 🚀 Performance Features

### **Optimizations**
- **Concurrent Processing**: Multi-threaded file analysis
- **Caching System**: Avoids re-analyzing unchanged files
- **Size Limits**: Prevents analysis of overly large files
- **Scope Filtering**: Early filtering reduces processing overhead

### **Resource Management**
- **Thread Pool**: Fixed-size thread pool prevents resource exhaustion
- **Memory Management**: Streaming and buffered file reading
- **Cache Management**: Configurable cache size and cleanup
- **Graceful Shutdown**: Proper cleanup of all resources

## 📈 Future Enhancements

### **Planned Features**
- **Additional File Types**: Support for more specialized file formats
- **Machine Learning**: Enhanced pattern recognition using ML
- **Custom Patterns**: User-defined security patterns
- **Export Formats**: Multiple output formats for integration
- **Real-time Monitoring**: File system monitoring for changes

### **Integration Opportunities**
- **SIEM Integration**: Export findings to security platforms
- **CI/CD Integration**: Automated security scanning in pipelines
- **Report Generation**: Enhanced reporting with visualizations
- **API Endpoints**: REST API for external tool integration

---

This comprehensive information gathering system transforms Agent-Orange into a powerful reconnaissance and vulnerability management platform, providing security professionals with the tools they need for effective penetration testing and security assessments.