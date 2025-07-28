# Agent-Orange Runtime Dependencies Checklist

## 🎯 **Overview**

This checklist systematically verifies that all runtime dependencies for Agent-Orange are correctly installed, configured, and accessible. Use this guide to troubleshoot issues and ensure complete functionality.

## 📋 **Quick Status Check**

Run this command first to get an overall status:
```bash
./check_dependencies.sh --quick
```

---

## 🔍 **Dependency Status Matrix**

| Dependency | Status | Required For | Impact if Missing |
|------------|--------|--------------|-------------------|
| **Java 17+** | ❓ | Core Application | Application won't start |
| **ChromaDB** | ❓ | Vector Database | No document search/embedding |
| **Ghidra** | ❓ | Binary Analysis | No reverse engineering features |
| **Llama.cpp** | ❓ | LLM Inference | No AI text generation |
| **GCC/G++** | ❓ | Native Compilation | Can't build native libraries |

---

## ☕ **1. Java Runtime Environment**

### **Installation Check**
```bash
# Check Java version
java -version

# Expected output:
# openjdk version "17.0.x" or higher
# OpenJDK Runtime Environment
```

**✅ Success Criteria**: Java 17 or higher installed

**❌ If Failed**:
```bash
# Ubuntu/Debian
sudo apt update && sudo apt install openjdk-17-jdk

# RHEL/CentOS/Fedora
sudo dnf install java-17-openjdk-devel

# macOS
brew install openjdk@17

# Verify installation
java -version
```

### **Configuration Check**
```bash
# Check JAVA_HOME
echo $JAVA_HOME

# Check Java classpath and library path
java -XshowSettings:properties 2>&1 | grep -E "(java.home|java.library.path)"
```

**✅ Success Criteria**: JAVA_HOME set, java.library.path includes project bin/

---

## 🗄️ **2. ChromaDB Vector Database**

### **Installation Check**
```bash
# Method 1: Check Python package
python3 -c "import chromadb; print('ChromaDB version:', chromadb.__version__)" 2>/dev/null

# Method 2: Check Docker installation
docker images | grep chroma

# Method 3: Check system installation
which chromadb
```

**✅ Success Criteria**: At least one method shows ChromaDB available

**❌ If Not Installed**:
```bash
# Option A: Python installation
python3 -m venv chromadb_env
source chromadb_env/bin/activate
pip install chromadb

# Option B: Docker installation
docker pull chromadb/chroma:latest

# Option C: Use setup script
./setup_chromadb.sh --background
```

### **Service Status Check**
```bash
# Check if ChromaDB server is running
curl -s http://localhost:8000/api/v1/heartbeat

# Expected response:
# {"nanosecond heartbeat": [timestamp]}

# Check process
ps aux | grep -i chroma
```

**✅ Success Criteria**: Heartbeat responds with JSON containing timestamp

**❌ If Not Running**:
```bash
# Option A: Start Python server
python3 -m chromadb.cli run --host localhost --port 8000 &

# Option B: Start Docker container
docker run -d --name chromadb-agent-orange -p 8000:8000 chromadb/chroma:latest

# Option C: Use setup script
./setup_chromadb.sh --background

# Verify startup
sleep 5 && curl http://localhost:8000/api/v1/heartbeat
```

### **Application Integration Check**
```bash
# Test Agent-Orange ChromaDB client
java -cp bin/chatbot.jar com.example.ChromaDBClient

# Expected output:
# ✅ ChromaDB client created successfully
# ✅ ChromaDB connection working

# Check configuration
grep vectordb.endpoint application.properties
```

**✅ Success Criteria**: Client connects successfully, no connection errors

**❌ If Connection Fails**:
```bash
# Check endpoint configuration
export VECTORDB_ENDPOINT=http://localhost:8000

# Update application.properties
sed -i 's|vectordb.endpoint=.*|vectordb.endpoint=http://localhost:8000|' application.properties

# Test with custom endpoint
VECTORDB_ENDPOINT=http://localhost:8000 java -cp bin/chatbot.jar com.example.ChromaDBClient
```

### **Functional Test**
```bash
# Test basic ChromaDB operations
curl -X POST http://localhost:8000/api/v1/collections \
  -H "Content-Type: application/json" \
  -d '{"name": "test_collection"}'

# List collections
curl http://localhost:8000/api/v1/collections

# Delete test collection
curl -X DELETE http://localhost:8000/api/v1/collections/test_collection
```

**✅ Success Criteria**: All operations return successful HTTP responses

---

## 🔬 **3. Ghidra Reverse Engineering Platform**

### **Installation Check**
```bash
# Check for Ghidra installation
ls -la /opt/ghidra/support/analyzeHeadless
ls -la /usr/local/ghidra/support/analyzeHeadless
ls -la /Applications/ghidra/support/analyzeHeadless

# Check environment variables
echo $GHIDRA_HOME
echo $GHIDRA_ANALYZE_HEADLESS

# Search for analyzeHeadless
find /usr /opt /Applications -name "analyzeHeadless" 2>/dev/null
```

**✅ Success Criteria**: analyzeHeadless script found and executable

**❌ If Not Installed**:
```bash
# Option A: Use setup script
./setup_ghidra.sh --user-install

# Option B: Manual download
wget https://github.com/NationalSecurityAgency/ghidra/releases/latest
# Follow GHIDRA_INTEGRATION.md for complete instructions

# Option C: Package manager (if available)
# Ubuntu: sudo apt install ghidra
# macOS: brew install ghidra
```

### **Permissions and Dependencies Check**
```bash
# Check if analyzeHeadless is executable
/opt/ghidra/support/analyzeHeadless -help

# Check Java dependency for Ghidra
/opt/ghidra/support/analyzeHeadless | head -5

# Check write permissions for project directory
mkdir -p /tmp/ghidra_test_proj
touch /tmp/ghidra_test_proj/test_file
rm -rf /tmp/ghidra_test_proj
```

**✅ Success Criteria**: analyzeHeadless runs and shows help, project directory writable

**❌ If Permission Issues**:
```bash
# Fix executable permissions
chmod +x /opt/ghidra/support/analyzeHeadless

# Fix ownership
sudo chown -R $USER:$USER /opt/ghidra

# Create writable project directory
mkdir -p $HOME/ghidra_projects
export GHIDRA_PROJECT_DIR=$HOME/ghidra_projects
```

### **Application Integration Check**
```bash
# Test Agent-Orange Ghidra integration
java -cp bin/chatbot.jar com.example.GhidraBridge

# Expected output:
# ✅ GhidraBridge configuration loaded successfully!
# GhidraBridge Configuration:
#   analyzeHeadless: /opt/ghidra/support/analyzeHeadless
#   projectDir: /tmp/ghidra_proj
#   projectName: agent_orange_analysis
#   timeout: 300000 ms

# Check configuration
grep ghidra application.properties
```

**✅ Success Criteria**: GhidraBridge loads without errors, shows valid configuration

**❌ If Configuration Fails**:
```bash
# Set environment variables
export GHIDRA_HOME=/opt/ghidra
export GHIDRA_PROJECT_DIR=/tmp/ghidra_projects
mkdir -p $GHIDRA_PROJECT_DIR

# Update application.properties
echo "ghidra.headless.path=/opt/ghidra/support/analyzeHeadless" >> application.properties
echo "ghidra.project.dir=/tmp/ghidra_projects" >> application.properties

# Test with environment override
GHIDRA_HOME=/opt/ghidra java -cp bin/chatbot.jar com.example.GhidraBridge
```

### **Functional Test**
```bash
# Test Ghidra headless analysis (using system binary)
/opt/ghidra/support/analyzeHeadless /tmp/test_ghidra_proj test_proj -import /bin/ls -postScript ExtractFunctions.java

# Create simple test binary
echo 'int main(){return 0;}' > test.c
gcc -o test test.c

# Test with Agent-Orange (if binary analysis is working)
echo "analyze $(pwd)/test with ghidra" | timeout 60 java -jar bin/chatbot.jar
```

**✅ Success Criteria**: Ghidra successfully analyzes a test binary

---

## 🦙 **4. Llama.cpp LLM Engine**

### **Installation Check**
```bash
# Check for llama.cpp headers
ls -la main/llama.h main/ggml.h

# Check for llama.cpp installation
find /usr /opt /usr/local -name "llama.h" 2>/dev/null

# Check for built llama.cpp
ls -la llama.cpp/llama.h llama.cpp/main 2>/dev/null
```

**✅ Success Criteria**: llama.h found in main/ directory or system installation

**❌ If Not Installed**:
```bash
# Option A: Use setup script
./setup_llama.sh --install-method source

# Option B: Manual installation
git clone https://github.com/ggerganov/llama.cpp.git
cd llama.cpp
make -j$(nproc)
cp llama.h ../main/
cp ggml.h ../main/
cd ..

# Option C: Headers-only (minimal functionality)
./setup_llama.sh --install-method headers-only
```

### **Native Library Check**
```bash
# Check for compiled native library
ls -la bin/libllama.so bin/libllama.dylib bin/llama.dll 2>/dev/null

# Check library dependencies (Linux)
ldd bin/libllama.so 2>/dev/null

# Check library architecture
file bin/libllama.so 2>/dev/null
```

**✅ Success Criteria**: Native library exists and has correct architecture

**❌ If Library Missing**:
```bash
# Compile native library
./build.sh native

# Check compilation requirements
./setup_llama.sh --test-only

# Manual compilation check
./gradlew compileJNI --info
```

### **Library Path Check**
```bash
# Check Java library path
java -XshowSettings:properties 2>&1 | grep java.library.path

# Check environment variables
echo "LD_LIBRARY_PATH: $LD_LIBRARY_PATH"
echo "DYLD_LIBRARY_PATH: $DYLD_LIBRARY_PATH"
echo "PATH: $PATH" | grep bin

# Test library loading
export LD_LIBRARY_PATH=$PWD/bin:$LD_LIBRARY_PATH
java -Djava.library.path=bin -cp bin/chatbot.jar com.example.LlamaJNI
```

**✅ Success Criteria**: Library path includes bin/, no "library not found" errors

**❌ If Path Issues**:
```bash
# Set library paths
export LD_LIBRARY_PATH=$PWD/bin:$LD_LIBRARY_PATH
export DYLD_LIBRARY_PATH=$PWD/bin:$DYLD_LIBRARY_PATH

# Create environment script
source llama_env.sh

# Test with explicit path
java -Djava.library.path=bin -jar bin/chatbot.jar
```

### **Application Integration Check**
```bash
# Test LlamaJNI loading
java -cp bin/chatbot.jar com.example.LlamaJNI

# Expected output:
# ✅ LlamaJNI configuration loaded successfully!
# OR: [LlamaJNI] Failed to load native library: [specific error]

# Check in full application
timeout 30 java -jar bin/chatbot.jar --help 2>&1 | grep -i llama
```

**✅ Success Criteria**: Either library loads successfully OR clear error message about missing library

---

## 🏗️ **5. Build System Dependencies**

### **GCC/G++ Compiler Check**
```bash
# Check compiler availability
gcc --version
g++ --version

# Check build tools
make --version
```

**✅ Success Criteria**: GCC/G++ version 7.0 or higher

**❌ If Missing**:
```bash
# Ubuntu/Debian
sudo apt install build-essential

# RHEL/CentOS/Fedora
sudo dnf groupinstall "Development Tools"

# macOS
xcode-select --install
```

### **Gradle Build System Check**
```bash
# Check Gradle wrapper
./gradlew --version

# Test basic compilation
./gradlew compileJava

# Check all tasks
./gradlew tasks
```

**✅ Success Criteria**: Gradle builds successfully, shows available tasks

---

## 🔄 **Automated Dependency Checker Script**

Let me create an automated script that runs all these checks:

```bash
#!/bin/bash
# check_dependencies.sh - Automated dependency verification

set -e

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_status() {
    local status=$1
    local message=$2
    case $status in
        "PASS") echo -e "${GREEN}✅ PASS${NC} $message" ;;
        "FAIL") echo -e "${RED}❌ FAIL${NC} $message" ;;
        "WARN") echo -e "${YELLOW}⚠️  WARN${NC} $message" ;;
        "INFO") echo -e "${BLUE}ℹ️  INFO${NC} $message" ;;
    esac
}

check_java() {
    print_status "INFO" "Checking Java Runtime Environment..."
    
    if command -v java >/dev/null 2>&1; then
        JAVA_VERSION=$(java -version 2>&1 | head -n1 | cut -d'"' -f2 | cut -d'.' -f1)
        if [[ "$JAVA_VERSION" -ge 17 ]]; then
            print_status "PASS" "Java $JAVA_VERSION found"
            return 0
        else
            print_status "FAIL" "Java 17+ required, found $JAVA_VERSION"
            return 1
        fi
    else
        print_status "FAIL" "Java not found"
        return 1
    fi
}

check_chromadb() {
    print_status "INFO" "Checking ChromaDB..."
    
    # Check installation
    if python3 -c "import chromadb" 2>/dev/null; then
        print_status "PASS" "ChromaDB Python package installed"
    elif docker images | grep -q chroma; then
        print_status "PASS" "ChromaDB Docker image available"
    else
        print_status "FAIL" "ChromaDB not installed"
        return 1
    fi
    
    # Check service
    if curl -s http://localhost:8000/api/v1/heartbeat >/dev/null 2>&1; then
        print_status "PASS" "ChromaDB service running on port 8000"
    else
        print_status "FAIL" "ChromaDB service not running"
        return 1
    fi
    
    return 0
}

check_ghidra() {
    print_status "INFO" "Checking Ghidra..."
    
    # Check installation
    local ghidra_paths=(
        "/opt/ghidra/support/analyzeHeadless"
        "/usr/local/ghidra/support/analyzeHeadless"
        "/Applications/ghidra/support/analyzeHeadless"
    )
    
    local found=false
    for path in "${ghidra_paths[@]}"; do
        if [[ -x "$path" ]]; then
            print_status "PASS" "Ghidra found at $path"
            found=true
            break
        fi
    done
    
    if [[ "$found" == false ]]; then
        print_status "FAIL" "Ghidra analyzeHeadless not found"
        return 1
    fi
    
    # Check Java integration
    if java -cp bin/chatbot.jar com.example.GhidraBridge >/dev/null 2>&1; then
        print_status "PASS" "Ghidra integration working"
    else
        print_status "WARN" "Ghidra integration issues (check configuration)"
    fi
    
    return 0
}

check_llama() {
    print_status "INFO" "Checking Llama.cpp..."
    
    # Check headers
    if [[ -f "main/llama.h" ]]; then
        print_status "PASS" "Llama.cpp headers found"
    else
        print_status "FAIL" "Llama.cpp headers missing"
        return 1
    fi
    
    # Check native library
    if ls bin/libllama.* >/dev/null 2>&1; then
        print_status "PASS" "Llama.cpp native library compiled"
        
        # Test loading
        export LD_LIBRARY_PATH=$PWD/bin:$LD_LIBRARY_PATH
        if java -cp bin/chatbot.jar com.example.LlamaJNI >/dev/null 2>&1; then
            print_status "PASS" "Llama.cpp library loads successfully"
        else
            print_status "WARN" "Llama.cpp library found but fails to load"
        fi
    else
        print_status "WARN" "Llama.cpp native library not compiled"
    fi
    
    return 0
}

check_build_system() {
    print_status "INFO" "Checking Build System..."
    
    # Check GCC
    if command -v g++ >/dev/null 2>&1; then
        print_status "PASS" "GCC/G++ compiler available"
    else
        print_status "FAIL" "GCC/G++ compiler missing"
        return 1
    fi
    
    # Check Gradle
    if ./gradlew --version >/dev/null 2>&1; then
        print_status "PASS" "Gradle build system working"
    else
        print_status "FAIL" "Gradle build system issues"
        return 1
    fi
    
    # Check JAR
    if [[ -f "bin/chatbot.jar" ]]; then
        print_status "PASS" "Application JAR built"
    else
        print_status "FAIL" "Application JAR missing"
        return 1
    fi
    
    return 0
}

main() {
    echo "🔍 Agent-Orange Dependency Checker"
    echo "=================================="
    echo
    
    local all_passed=true
    
    check_java || all_passed=false
    echo
    check_chromadb || all_passed=false
    echo
    check_ghidra || all_passed=false
    echo
    check_llama || all_passed=false
    echo
    check_build_system || all_passed=false
    echo
    
    if [[ "$all_passed" == true ]]; then
        print_status "PASS" "All dependencies verified successfully!"
        echo
        print_status "INFO" "Agent-Orange is ready for full functionality"
    else
        print_status "WARN" "Some dependencies have issues"
        echo
        print_status "INFO" "Review the failed checks above"
        print_status "INFO" "Run specific setup scripts to fix issues:"
        echo "  ./setup_chromadb.sh    - Fix ChromaDB"
        echo "  ./setup_ghidra.sh      - Fix Ghidra"
        echo "  ./setup_llama.sh       - Fix Llama.cpp"
    fi
}

main "$@"
```

---

## 🛠️ **Troubleshooting Decision Tree**

### **Application Won't Start**
1. ✅ Check Java installation (`java -version`)
2. ✅ Check JAR exists (`ls bin/chatbot.jar`)
3. ✅ Check permissions (`chmod +x run_chatbot.sh`)

### **"Module Not Available" Messages**
1. ✅ Run dependency checker (`./check_dependencies.sh`)
2. ✅ Check specific service status (ChromaDB heartbeat, Ghidra path)
3. ✅ Review configuration files (`application.properties`)

### **Native Library Errors**
1. ✅ Check library exists (`ls bin/lib*.*)`)
2. ✅ Check library path (`echo $LD_LIBRARY_PATH`)
3. ✅ Test library loading (`java -cp bin/chatbot.jar com.example.LlamaJNI`)

### **Performance Issues**
1. ✅ Check available memory (`free -h`)
2. ✅ Check CPU usage (`top`)
3. ✅ Check Java heap size (JVM args in build.gradle)

---

## 📊 **Quick Reference Commands**

| Check | Command | Expected Result |
|-------|---------|-----------------|
| **Java** | `java -version` | Version 17+ |
| **ChromaDB** | `curl http://localhost:8000/api/v1/heartbeat` | JSON heartbeat |
| **Ghidra** | `ls /opt/ghidra/support/analyzeHeadless` | File exists |
| **Llama.cpp** | `ls main/llama.h bin/libllama.*` | Files exist |
| **Application** | `java -jar bin/chatbot.jar --help` | Help text |

---

## 🚀 **Quick Fix Commands**

```bash
# Fix all dependencies at once
./setup_chromadb.sh --background
./setup_ghidra.sh --user-install  
./setup_llama.sh --install-method source

# Rebuild application
./build.sh clean && ./build.sh all

# Test everything
./check_dependencies.sh
```

---

## ✅ **Success Verification**

When everything is working, you should see:

```bash
$ ./check_dependencies.sh
🔍 Agent-Orange Dependency Checker
==================================

✅ PASS Java 21 found
✅ PASS ChromaDB Python package installed
✅ PASS ChromaDB service running on port 8000
✅ PASS Ghidra found at /opt/ghidra/support/analyzeHeadless
✅ PASS Ghidra integration working
✅ PASS Llama.cpp headers found
✅ PASS Llama.cpp native library compiled
✅ PASS Llama.cpp library loads successfully
✅ PASS GCC/G++ compiler available
✅ PASS Gradle build system working
✅ PASS Application JAR built

✅ PASS All dependencies verified successfully!
ℹ️  INFO Agent-Orange is ready for full functionality
```

**This checklist provides a systematic approach to verify and troubleshoot all Agent-Orange dependencies, ensuring reliable operation across different environments.**