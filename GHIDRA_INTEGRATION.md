# Ghidra Integration Guide for Agent-Orange

## 🔍 **Overview**

Agent-Orange integrates with Ghidra's headless analysis capabilities to perform automated binary analysis. This document explains how to configure and use this integration.

## 📋 **Configuration Priority System**

The application uses a flexible configuration system with the following priority (highest to lowest):

### **1. Environment Variables (Highest Priority)**
```bash
export GHIDRA_HOME=/opt/ghidra                      # Ghidra installation directory
export GHIDRA_ANALYZE_HEADLESS=/opt/ghidra/support/analyzeHeadless  # Direct path to script
export GHIDRA_PROJECT_DIR=/tmp/ghidra_projects      # Project directory
export GHIDRA_PROJECT_NAME=agent_orange_analysis    # Project name
export GHIDRA_TIMEOUT_MS=600000                     # Timeout (10 minutes)
```

### **2. System Properties (JVM Arguments)**
```bash
java -Dghidra.home=/opt/ghidra \
     -Dghidra.headless.path=/opt/ghidra/support/analyzeHeadless \
     -Dghidra.project.dir=/tmp/ghidra_projects \
     -Dghidra.project.name=agent_orange_analysis \
     -Dghidra.timeout.ms=600000 \
     -jar bin/chatbot.jar
```

### **3. Application Properties File (Medium Priority)**
```properties
# In application.properties
ghidra.headless.path=/opt/ghidra/support/analyzeHeadless
ghidra.home=/opt/ghidra
ghidra.project.dir=/tmp/ghidra_proj
ghidra.project.name=agent_orange_analysis
ghidra.timeout.ms=300000
```

### **4. Auto-Detection (Lowest Priority)**
The application automatically searches common installation paths:
- `/opt/ghidra/support/analyzeHeadless` (Linux standard)
- `/usr/local/ghidra/support/analyzeHeadless` (Linux alternative)
- `/Applications/ghidra/support/analyzeHeadless` (macOS)
- `C:\ghidra\support\analyzeHeadless.bat` (Windows)
- `C:\Program Files\ghidra\support\analyzeHeadless.bat` (Windows Program Files)

## 🚀 **Quick Setup**

### **Step 1: Install Ghidra**

#### **Option A: Download from NSA**
```bash
# Download Ghidra from official source
wget https://github.com/NationalSecurityAgency/ghidra/releases/download/Ghidra_10.4_build/ghidra_10.4_PUBLIC_20230928.zip

# Extract to /opt/ghidra
sudo unzip ghidra_10.4_PUBLIC_20230928.zip -d /opt/
sudo mv /opt/ghidra_10.4_PUBLIC /opt/ghidra
sudo chown -R $USER:$USER /opt/ghidra
```

#### **Option B: Using Package Manager**
```bash
# Ubuntu/Debian (if available in repos)
sudo apt-get install ghidra

# Arch Linux
yay -S ghidra

# macOS with Homebrew
brew install ghidra
```

#### **Option C: Build from Source**
```bash
git clone https://github.com/NationalSecurityAgency/ghidra.git
cd ghidra
gradle -I gradle/support/fetchDependencies.gradle init
gradle buildGhidra
```

### **Step 2: Configure Agent-Orange**

#### **Method A: Environment Variables (Recommended)**
```bash
# Set Ghidra home directory
export GHIDRA_HOME=/opt/ghidra

# Verify the path exists
ls -la $GHIDRA_HOME/support/analyzeHeadless

# Set project directory (must be writable)
export GHIDRA_PROJECT_DIR=/tmp/ghidra_projects
mkdir -p $GHIDRA_PROJECT_DIR

# Optional: Set project name and timeout
export GHIDRA_PROJECT_NAME=agent_orange_analysis
export GHIDRA_TIMEOUT_MS=600000  # 10 minutes
```

#### **Method B: Update application.properties**
```bash
# Edit the configuration file
nano application.properties

# Update these settings:
ghidra.headless.path=/opt/ghidra/support/analyzeHeadless
ghidra.project.dir=/tmp/ghidra_projects
ghidra.project.name=agent_orange_analysis
ghidra.timeout.ms=300000
```

#### **Method C: Gradle Properties**
```bash
# Pass configuration via Gradle
./gradlew run -Pghidra.home=/opt/ghidra \
              -Pghidra.project.dir=/tmp/ghidra_projects
```

### **Step 3: Verify Installation**
```bash
# Test Ghidra headless directly
/opt/ghidra/support/analyzeHeadless /tmp/test_project test_proj -help

# Test with Agent-Orange
java -jar bin/chatbot.jar
```

## 🧪 **Testing Ghidra Integration**

### **Method 1: Direct GhidraBridge Test**
```bash
# Compile and test GhidraBridge directly
./gradlew compileJava
java -cp build/classes/java/main com.example.GhidraBridge
```

### **Method 2: Full Application Test**
```bash
# Start Agent-Orange and test Ghidra analysis
java -jar bin/chatbot.jar

# In the chat interface, try:
analyze /path/to/binary.exe with ghidra
ghidra analyze /path/to/sample.bin
run ghidra analysis on /usr/bin/ls
```

### **Method 3: Configuration Verification**
```java
// Create a test to verify configuration
public class GhidraConfigTest {
    public static void main(String[] args) {
        try {
            GhidraBridge bridge = GhidraBridge.fromConfig();
            System.out.println("✅ Ghidra configuration loaded successfully");
            System.out.println(bridge.getConfiguration());
        } catch (Exception e) {
            System.err.println("❌ Ghidra configuration failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
```

## 🔧 **Expected Behavior When Working Correctly**

### **Successful Analysis Output**
When Ghidra integration is working correctly, you should see:

1. **Initialization Messages**
   ```
   [INFO] Ghidra bridge initialized
   [INFO] All modules loaded successfully
   ```

2. **Analysis Process**
   ```
   [INFO] Starting Ghidra analysis for: /path/to/binary.exe
   [INFO] Creating Ghidra project: agent_orange_analysis
   [INFO] Running analysis script: ExtractFunctions.java
   ```

3. **Analysis Results** (JSON format)
   ```json
   {
     "functions": [
       {"name": "main", "address": "0x401000", "size": 156},
       {"name": "printf", "address": "0x401100", "size": 45},
       {"name": "malloc", "address": "0x401150", "size": 32}
     ],
     "imports": [
       {"name": "kernel32.dll", "functions": ["GetStdHandle", "WriteFile"]},
       {"name": "msvcrt.dll", "functions": ["printf", "malloc", "free"]}
     ],
     "strings": [
       {"address": "0x402000", "value": "Hello World"},
       {"address": "0x402010", "value": "Error: Invalid input"}
     ]
   }
   ```

### **Analysis Types Performed**
- **Function Extraction**: Identifies all functions in the binary
- **Import Analysis**: Lists external library dependencies
- **String Analysis**: Extracts readable strings from the binary
- **Control Flow**: Maps program execution flow
- **Data Structure**: Identifies data types and structures

### **File Types Supported**
- **PE/COFF**: Windows executables (.exe, .dll)
- **ELF**: Linux executables and shared libraries
- **Mach-O**: macOS executables
- **Raw Binary**: Firmware and embedded binaries
- **Archive Files**: Static libraries (.a, .lib)

## 🔍 **Troubleshooting Common Issues**

### **Issue 1: "Ghidra analyzeHeadless script not found"**
```
Error: Ghidra analyzeHeadless script not found: /opt/ghidra/support/analyzeHeadless
```

**Solutions:**
```bash
# Find where Ghidra is actually installed
find /usr /opt /home -name "analyzeHeadless" 2>/dev/null

# Common alternative locations
ls -la /usr/local/ghidra/support/analyzeHeadless
ls -la /Applications/ghidra/support/analyzeHeadless

# Set correct path
export GHIDRA_HOME=/path/to/actual/ghidra
```

### **Issue 2: "Permission denied"**
```
Error: Ghidra analyzeHeadless script is not executable
```

**Solutions:**
```bash
# Make analyzeHeadless executable
chmod +x /opt/ghidra/support/analyzeHeadless

# Check ownership
ls -la /opt/ghidra/support/analyzeHeadless

# Fix ownership if needed
sudo chown $USER:$USER /opt/ghidra/support/analyzeHeadless
```

### **Issue 3: "Project directory not writable"**
```
Error: Cannot create Ghidra project directory: /tmp/ghidra_projects
```

**Solutions:**
```bash
# Create project directory with correct permissions
mkdir -p /tmp/ghidra_projects
chmod 755 /tmp/ghidra_projects

# Use alternative location
export GHIDRA_PROJECT_DIR=$HOME/ghidra_projects
mkdir -p $GHIDRA_PROJECT_DIR
```

### **Issue 4: "Analysis timeout"**
```
Error: Ghidra analysis timed out after 300000ms
```

**Solutions:**
```bash
# Increase timeout (in milliseconds)
export GHIDRA_TIMEOUT_MS=1200000  # 20 minutes

# Or in application.properties
ghidra.timeout.ms=1200000
```

### **Issue 5: "Java dependencies missing"**
```
Error: Ghidra requires Java 17 or higher
```

**Solutions:**
```bash
# Check Java version
java -version

# Install OpenJDK 17
sudo apt-get install openjdk-17-jdk

# Set JAVA_HOME
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
```

## ⚙️ **Advanced Configuration**

### **Custom Ghidra Scripts**
```bash
# Create custom analysis script
cat > custom_analysis.java << 'EOF'
// Custom Ghidra analysis script
import ghidra.app.script.GhidraScript;
import ghidra.program.model.listing.Function;

public class CustomAnalysis extends GhidraScript {
    @Override
    public void run() throws Exception {
        // Custom analysis logic
        for (Function func : currentProgram.getFunctionManager().getFunctions(true)) {
            println("Function: " + func.getName() + " at " + func.getEntryPoint());
        }
    }
}
EOF

# Use custom script with GhidraBridge
bridge.runScript("/path/to/binary", "custom_analysis.java", null);
```

### **Performance Tuning**
```bash
# Increase Java heap for large binaries
export JAVA_OPTS="-Xmx8g -Xms4g"

# Use parallel analysis (if supported)
export GHIDRA_PARALLEL_ANALYSIS=true

# Adjust timeout based on binary size
# Small binaries: 5 minutes
export GHIDRA_TIMEOUT_MS=300000
# Large binaries: 30 minutes
export GHIDRA_TIMEOUT_MS=1800000
```

### **Security Considerations**
```bash
# Run Ghidra analysis in sandbox
systemd-run --user --scope --property="PrivateTmp=yes" \
    java -jar bin/chatbot.jar

# Limit file system access
export GHIDRA_PROJECT_DIR=/isolated/analysis/directory
mkdir -p $GHIDRA_PROJECT_DIR
chmod 700 $GHIDRA_PROJECT_DIR
```

## 📊 **Environment Variables Reference**

| Variable | Purpose | Example | Required |
|----------|---------|---------|----------|
| `GHIDRA_HOME` | Ghidra installation directory | `/opt/ghidra` | No* |
| `GHIDRA_ANALYZE_HEADLESS` | Direct path to analyzeHeadless | `/opt/ghidra/support/analyzeHeadless` | No* |
| `GHIDRA_PROJECT_DIR` | Project directory | `/tmp/ghidra_projects` | No |
| `GHIDRA_PROJECT_NAME` | Project name | `agent_orange_analysis` | No |
| `GHIDRA_TIMEOUT_MS` | Analysis timeout | `600000` | No |

*Either `GHIDRA_HOME` or `GHIDRA_ANALYZE_HEADLESS` should be set, or use application.properties.

## 🎯 **Quick Command Reference**

### **Setup Commands**
```bash
# Install and configure Ghidra
wget https://github.com/NationalSecurityAgency/ghidra/releases/latest
sudo unzip ghidra_*.zip -d /opt/
sudo mv /opt/ghidra_* /opt/ghidra

# Configure Agent-Orange
export GHIDRA_HOME=/opt/ghidra
export GHIDRA_PROJECT_DIR=/tmp/ghidra_projects
mkdir -p $GHIDRA_PROJECT_DIR

# Test configuration
/opt/ghidra/support/analyzeHeadless -help
```

### **Runtime Commands**
```bash
# Start with Ghidra configuration
GHIDRA_HOME=/opt/ghidra java -jar bin/chatbot.jar

# Test specific binary
echo "analyze /usr/bin/ls with ghidra" | java -jar bin/chatbot.jar

# Debug configuration
java -Dghidra.debug=true -jar bin/chatbot.jar
```

### **Troubleshooting Commands**
```bash
# Check Ghidra installation
ls -la /opt/ghidra/support/analyzeHeadless
/opt/ghidra/support/analyzeHeadless -help

# Verify Java version
java -version
$GHIDRA_HOME/support/analyzeHeadless -help | head -5

# Test project directory
mkdir -p $GHIDRA_PROJECT_DIR && echo "✅ Project directory OK"

# Check configuration
java -cp bin/chatbot.jar com.example.GhidraBridge
```

## ✅ **Success Indicators**

When Ghidra integration is working correctly:

1. **No error messages** during application startup
2. **Configuration validation passes** without warnings
3. **Binary analysis commands** return structured JSON data
4. **Log messages** show successful Ghidra initialization
5. **Analysis results** include functions, imports, and strings

**The key is ensuring the `analyzeHeadless` script is executable and the project directory is writable!**