# Agent-Orange Enhanced Build System

## Overview

The Agent-Orange project now uses a modern Gradle-based build system that provides automatic dependency management, security scanning, native library compilation, and comprehensive testing support.

## Key Features

### 🔧 **Automatic Dependency Management**
- **Smart Resolution**: Automatically resolves and downloads all required dependencies
- **Version Management**: Centralized version control with latest compatible versions
- **Transitive Dependencies**: Handles complex dependency chains automatically
- **Conflict Resolution**: Intelligent resolution of version conflicts

### 🔒 **Security-First Approach**
- **Vulnerability Scanning**: Automatic CVE database checking via OWASP Dependency Check
- **Secure Repositories**: HTTPS-only repository access
- **Dependency Verification**: Checksum validation of downloaded artifacts
- **Security Reporting**: Detailed security reports with remediation guidance

### 🖥️ **Native Library Support**
- **Cross-Platform JNI**: Automatic compilation for Linux, macOS, and Windows
- **Platform Detection**: Smart detection of compilation requirements
- **Incremental Builds**: Only recompiles when source changes
- **Library Management**: Automatic placement and configuration

### 📊 **Advanced Testing**
- **Test Retry**: Automatic retry of flaky tests
- **Coverage Reports**: Code coverage analysis
- **Parallel Execution**: Faster test execution
- **Test Classification**: Unit, integration, and security tests

## Quick Start

### Prerequisites
- Java 17+ (OpenJDK recommended)
- GCC/G++ (for native libraries)
- Internet connection (for initial dependency download)

### Basic Usage
```bash
# Complete build (default)
./build.sh

# Specific targets
./build.sh clean          # Clean artifacts
./build.sh dependencies   # Download dependencies
./build.sh compile        # Compile only
./build.sh test           # Run tests
./build.sh jar            # Create JAR
./build.sh security       # Security scan
./build.sh native         # Compile native libs
```

## Dependency Management

### Core Dependencies
The build system automatically manages these critical dependencies:

| Dependency | Version | Purpose |
|------------|---------|---------|
| Gson | 2.10.1 | JSON processing for ChromaDB |
| JUnit | 4.13.2 | Testing framework |
| Commons Lang3 | 3.14.0 | Utility functions |
| SLF4J + Logback | 2.0.12 | Logging framework |
| Jackson | 2.16.1 | Additional JSON security |

### Dependency Updates
```bash
# Check for available updates
./gradlew dependencyUpdates

# List current dependencies
./build.sh list-deps

# Download latest compatible versions
./build.sh dependencies
```

### Version Management Strategy
- **Stable Versions**: Only stable releases, no alphas/betas/RCs
- **Security Patches**: Automatic security patch adoption
- **Compatibility**: Backwards compatibility verification
- **Lock Files**: Reproducible builds across environments

## Native Library Handling

### JNI Compilation Process
1. **Platform Detection**: Automatically detects OS and architecture
2. **Header Generation**: Uses Java headers for JNI compilation
3. **Compilation**: Cross-platform G++ compilation
4. **Library Placement**: Places libraries in correct locations
5. **Path Configuration**: Automatic java.library.path setup

### Supported Platforms
- **Linux**: `.so` libraries (primary target)
- **macOS**: `.dylib` libraries
- **Windows**: `.dll` libraries

### Compilation Requirements
```bash
# Ubuntu/Debian
sudo apt-get install build-essential

# CentOS/RHEL/Fedora
sudo yum groupinstall "Development Tools"
# or
sudo dnf groupinstall "Development Tools"

# macOS
xcode-select --install

# Windows
# Install MinGW-w64 or Visual Studio Build Tools
```

## Security Considerations

### Dependency Security
- **CVE Scanning**: Continuous vulnerability monitoring
- **HTTPS Only**: All repository access via secure connections
- **Checksum Verification**: Integrity validation of downloads
- **False Positive Management**: Suppression file for known safe cases

### Update Strategy
```bash
# Run security scan
./build.sh security

# Check specific CVE
./gradlew dependencyCheckAnalyze

# Update dependencies
./gradlew dependencyUpdates
```

### Security Reporting
- **HTML Reports**: Detailed vulnerability reports in `build/reports/`
- **JSON/XML**: Machine-readable security data
- **CI/CD Integration**: Fail builds on high-severity vulnerabilities
- **Suppression**: Manage false positives via `dependency-check-suppressions.xml`

### Best Practices
1. **Regular Scans**: Run security checks in CI/CD pipeline
2. **Prompt Updates**: Apply security patches quickly
3. **Version Pinning**: Use specific versions in production
4. **Review Updates**: Manual review of major version changes

## Advanced Configuration

### Gradle Properties
Edit `gradle.properties` for customization:
```properties
# Memory allocation
org.gradle.jvmargs=-Xmx4g -Xms1g

# Build performance
org.gradle.parallel=true
org.gradle.caching=true

# Security settings
systemProp.https.protocols=TLSv1.2,TLSv1.3
```

### Build Customization
Edit `build.gradle` for advanced needs:
- Custom repositories
- Additional dependencies
- Build script modifications
- Plugin configuration

## Troubleshooting

### Common Issues

#### "Gradle not found"
```bash
# Wrapper will auto-download Gradle
./gradlew --version
```

#### "GCC not found"
```bash
# Install build tools
sudo apt-get install build-essential  # Ubuntu/Debian
sudo yum groupinstall "Development Tools"  # CentOS/RHEL
```

#### "Permission denied on gradlew"
```bash
chmod +x gradlew
```

#### "Dependencies not downloading"
```bash
# Check network and clear cache
rm -rf ~/.gradle/caches
./build.sh dependencies
```

#### "Native compilation fails"
```bash
# Check Java headers
ls $JAVA_HOME/include/
# Should contain jni.h and platform directory

# Manual compilation test
g++ -I$JAVA_HOME/include -I$JAVA_HOME/include/linux \
    -shared -fPIC -o test.so main/llama_jni.cpp
```

### Debug Mode
```bash
# Verbose output
./gradlew build --debug

# Dependency resolution
./gradlew dependencies --configuration runtimeClasspath

# Build scan
./gradlew build --scan
```

### Performance Issues
```bash
# Build performance
./gradlew build --profile

# Dependency cache
./gradlew build --refresh-dependencies

# Parallel builds
./gradlew build --parallel
```

## Integration with IDEs

### IntelliJ IDEA
1. Open project as Gradle project
2. Auto-import will configure dependencies
3. Native library paths configured automatically

### Eclipse
1. Install Gradle plugin
2. Import as Gradle project
3. Refresh Gradle project for updates

### VS Code
1. Install Java Extension Pack
2. Install Gradle Extension
3. Open project folder

## Continuous Integration

### GitHub Actions Example
```yaml
name: Build and Test
on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v4
    - uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
    - name: Build
      run: ./build.sh all
    - name: Security Check
      run: ./build.sh security
```

### Jenkins Pipeline
```groovy
pipeline {
    agent any
    tools {
        jdk 'Java17'
    }
    stages {
        stage('Build') {
            steps {
                sh './build.sh all'
            }
        }
        stage('Security') {
            steps {
                sh './build.sh security'
                publishHTML([
                    allowMissing: false,
                    alwaysLinkToLastBuild: true,
                    keepAll: true,
                    reportDir: 'build/reports/dependency-check',
                    reportFiles: 'dependency-check-report.html',
                    reportName: 'Security Report'
                ])
            }
        }
    }
}
```

## Migration from Old Build System

### For Existing Users
1. **Backup**: Save your current `lib/` directory if you have custom JARs
2. **Clean**: Remove old build artifacts: `rm -rf bin/ lib/ logs/`
3. **Build**: Run `./build.sh` - dependencies will be auto-downloaded
4. **Verify**: Test the application: `./run_chatbot.sh`

### Custom Dependencies
If you have custom JARs:
1. Place them in `libs/` directory (will be created)
2. Add to `build.gradle` dependencies block:
   ```gradle
   implementation files('libs/custom.jar')
   ```

## Support and Resources

- **Documentation**: This file and inline code comments
- **Gradle Docs**: https://docs.gradle.org/
- **Security Plugin**: https://jeremylong.github.io/DependencyCheck/
- **Issues**: Project GitHub issues for build-related problems

---

**The new build system provides a robust, secure, and maintainable foundation for the Agent-Orange project while maintaining compatibility with existing workflows.**