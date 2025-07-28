# CVE Naming Convention for Agent-Orange

## Overview

Agent-Orange now implements a comprehensive CVE (Common Vulnerabilities and Exposures) naming convention system to ensure consistent vulnerability identification and management. This system prioritizes CVE IDs when available and provides intelligent fallbacks for vulnerabilities without assigned CVE numbers.

## Key Features

### 🔍 **CVE ID Validation and Normalization**
- Validates CVE IDs against the official format: `CVE-YYYY-NNNN`
- Normalizes case (converts to uppercase)
- Extracts CVE IDs from text descriptions
- Supports case-insensitive input

### 🏷️ **Intelligent Naming Priority**
1. **CVE ID** (highest priority) - when available and valid
2. **Extracted CVE ID** - found in name or description text
3. **Descriptive Name** - cleaned and formatted when no CVE available

### 🤖 **Automatic Processing**
- Burp Suite imports automatically detect CVE IDs
- Information gathering extracts CVE IDs from file contents
- Project management commands understand CVE queries
- All vulnerability displays prioritize CVE IDs

## CVE ID Format Requirements

### Valid CVE Format
```
CVE-YYYY-NNNN
```
Where:
- `CVE` is the literal prefix
- `YYYY` is a 4-digit year (1999-present)
- `NNNN` is a 4+ digit number
- Case-insensitive input accepted, normalized to uppercase

### Examples of Valid CVE IDs
```
CVE-2023-1234
CVE-2021-44228  (Log4Shell)
CVE-1999-0001  (Historic vulnerability)
cve-2023-5678   (lowercase, will be normalized)
```

### Examples of Invalid CVE IDs
```
CVE-23-1234     (Year too short)
CVE-2023-123    (Number too short)
VULN-2023-1234  (Wrong prefix)
CVE-2023        (Missing number)
```

## Usage Examples

### Manual Vulnerability Creation
```bash
# Creating with explicit CVE ID
add vulnerability "CVE-2023-1234" to 192.168.1.100

# Creating with descriptive name (no CVE)
add vulnerability "SQL Injection" to 192.168.1.100

# Mixed format (CVE will be extracted and prioritized)
add vulnerability "CVE-2023-1234: SQL Injection" to 192.168.1.100
```

### CVE-Specific Queries
```bash
# Show details for a specific CVE
show CVE-2023-1234

# Search for CVE IDs containing specific terms
search cve "2023"

# Search vulnerabilities by description
search vulnerability "injection"

# List all vulnerabilities (CVE IDs displayed prominently)
list vulnerabilities
```

### Burp Suite Integration
When importing Burp Suite XML files, the system automatically:
1. Scans issue names for CVE IDs
2. Extracts CVE IDs from descriptions
3. Uses CVE IDs as primary names when found
4. Falls back to descriptive names for non-CVE issues

```bash
# Import Burp data (automatic CVE detection)
import burp data from burp_export.xml into project "WebApp Audit"
```

## Display Formatting

### Primary Display (List View)
- **With CVE**: `CVE-2023-1234 (SQL Injection)`
- **Without CVE**: `Cross-Site Scripting`

### Detailed Display (Show Command)
- CVE ID prominently displayed in header
- Descriptive name shown as additional context
- Target and location information preserved

### Search Results
- CVE IDs highlighted in results
- Context snippets show matched terms
- Grouped by severity with CVE priority

## Implementation Components

### Core Classes

#### `CveUtils`
Central utility class providing:
- `isValidCveId()` - CVE format validation
- `normalizeCveId()` - Case normalization
- `extractCveIdFromText()` - Text extraction
- `determineBestName()` - Name priority logic
- `formatDisplayName()` - Display formatting

#### `Vulnerability` (Enhanced)
- `createWithCveConvention()` - Factory method with CVE logic
- `createFromBurpSuite()` - Burp Suite specific factory
- `setCveId()` - Validated CVE ID setter

#### `ProjectCommandProcessor` (Enhanced)
- CVE-specific command patterns
- `handleShowCve()` - CVE detail lookup
- `handleSearchVulnerability()` - Enhanced search with CVE support

### Updated Components

#### `BurpSuiteParser`
- Automatic CVE extraction from Burp findings
- Intelligent name prioritization

#### `ProjectManager`
- Uses CVE conventions for all vulnerability creation
- Preserves CVE metadata in project persistence

#### `InformationGatheringCommandProcessor`
- CVE-aware vulnerability processing
- Automatic CVE detection in gathered data

## Configuration

### Application Properties
No additional configuration required. The CVE system works automatically with existing vulnerability management.

### CVE Database Integration (Future)
```properties
# Future enhancement - CVE database integration
cve.database.enabled=true
cve.database.url=https://services.nvd.nist.gov/rest/json/cves/
cve.auto.enrich=true
```

## API Examples

### Java Code Examples

#### Creating Vulnerability with CVE Convention
```java
// Using the factory method (recommended)
Vulnerability vuln = Vulnerability.createWithCveConvention(
    "vuln_001", 
    "SQL Injection", 
    "CVE-2023-1234: SQL injection in login form", 
    Severity.HIGH, 
    "example.com",
    null  // CVE will be extracted from description
);

// Result: vuln.getName() = "CVE-2023-1234"
//         vuln.getCveId() = "CVE-2023-1234"
```

#### CVE Validation
```java
// Validate CVE format
boolean isValid = CveUtils.isValidCveId("CVE-2023-1234");  // true
String normalized = CveUtils.normalizeCveId("cve-2023-1234");  // "CVE-2023-1234"

// Extract from text
String extracted = CveUtils.extractCveIdFromText(
    "This vulnerability is tracked as CVE-2023-1234 in the database."
);  // "CVE-2023-1234"
```

#### Display Formatting
```java
// Format for display
String display = CveUtils.formatDisplayName(vulnerability);
// With CVE: "CVE-2023-1234"
// Without CVE: "SQL Injection"

String contextDisplay = CveUtils.formatDisplayNameWithContext(vulnerability, true);
// "CVE-2023-1234 (SQL Injection) - example.com"
```

## Natural Language Processing

### Supported Command Patterns

#### CVE-Specific Commands
```
show CVE-2023-1234
display CVE-2021-44228
get details for CVE-2023-1234
show CVE-2023-1234 in project "WebApp Audit"
```

#### Search Commands
```
search vulnerability "injection"
search cve "2023"
find vulnerability "log4j"
lookup cve "44228"
search vulnerability "CVE-2023" in project "My Audit"
```

#### Regular Vulnerability Commands (CVE-Enhanced)
```
list vulnerabilities                    # CVE IDs displayed prominently
add vulnerability "CVE-2023-1234"      # CVE used as name
add vulnerability "SQL Injection"      # Descriptive name when no CVE
```

## Testing

### Unit Tests
Comprehensive test suite in `CveUtilsTest.java`:
- CVE format validation
- Text extraction
- Name determination logic
- Display formatting
- Edge cases and error handling

### Integration Tests
- End-to-end vulnerability creation
- Burp Suite import with CVE detection
- Project management with CVE queries
- Information gathering with CVE extraction

## Performance Considerations

### CVE Processing
- Regex patterns compiled once and cached
- Text extraction limited to first match for efficiency
- Validation performed only when setting CVE IDs

### Memory Usage
- CVE IDs stored as normalized strings
- No additional metadata caching by default
- Minimal impact on existing vulnerability objects

## Security Considerations

### CVE ID Validation
- Strict format validation prevents injection
- Case normalization ensures consistency
- Invalid formats rejected with clear error messages

### Data Sanitization
- CVE IDs extracted safely from untrusted text
- No execution of embedded code or scripts
- Validation occurs before storage

## Migration and Compatibility

### Existing Projects
- Existing vulnerabilities remain unchanged
- New CVE IDs can be added to existing vulnerabilities
- Display automatically prioritizes CVE IDs when present

### Backward Compatibility
- All existing commands continue to work
- Non-CVE vulnerabilities display unchanged
- Optional CVE enhancement for existing data

## Future Enhancements

### Planned Features
1. **CVE Database Integration** - Automatic enrichment from NVD
2. **CVE Scoring** - CVSS score integration
3. **CVE Timeline** - Vulnerability lifecycle tracking
4. **CVE Reporting** - Enhanced reports with CVE statistics

### API Extensions
```java
// Future CVE database integration
CveDetails details = CveDatabase.lookupCve("CVE-2023-1234");
Vulnerability enriched = vulnerability.enrichWithCveData(details);

// Future CVSS integration
CvssScore score = vulnerability.getCvssScore();
```

## Troubleshooting

### Common Issues

#### CVE Format Errors
```
Error: Invalid CVE ID format: CVE-23-1234. Expected format: CVE-YYYY-NNNN
```
**Solution**: Use 4-digit year and at least 4-digit number

#### CVE Not Found
```
🔍 CVE CVE-2023-1234 not found in project 'WebApp Audit'.
```
**Solution**: Verify CVE ID spelling, check project name, or list all vulnerabilities

#### Search No Results
```
🔍 No vulnerabilities found matching 'term' in project 'Project'.
```
**Solution**: Try broader search terms, check CVE format, or list all vulnerabilities

### Debug Commands
```bash
# List all vulnerabilities to see current naming
list vulnerabilities

# Search broadly to find variations
search vulnerability "CVE"

# Check project status for data verification
show project status
```

## Support and Documentation

- **Source Code**: `main/CveUtils.java`
- **Tests**: `main/CveUtilsTest.java` 
- **Integration**: All vulnerability management components
- **Help**: Use `help` command for available CVE operations

---

*This CVE naming convention system ensures Agent-Orange provides industry-standard vulnerability identification while maintaining flexibility for non-CVE findings and legacy data.*