#!/bin/bash

# ============================================================================
# Agent-Orange Dependency Checker
# ============================================================================
#
# This script automatically verifies all runtime dependencies for Agent-Orange
# and provides detailed feedback on any issues found.
#
# USAGE:
#   ./check_dependencies.sh [--quick] [--verbose] [--fix]
#
# OPTIONS:
#   --quick      Quick check (skip detailed tests)
#   --verbose    Show detailed output for all checks
#   --fix        Attempt to automatically fix issues
# ============================================================================

set -e

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
QUICK_MODE=false
VERBOSE_MODE=false
FIX_MODE=false

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --quick)
            QUICK_MODE=true
            shift
            ;;
        --verbose)
            VERBOSE_MODE=true
            shift
            ;;
        --fix)
            FIX_MODE=true
            shift
            ;;
        --help|-h)
            echo "Usage: $0 [--quick] [--verbose] [--fix]"
            echo "  --quick      Quick check (skip detailed tests)"
            echo "  --verbose    Show detailed output for all checks"
            echo "  --fix        Attempt to automatically fix issues"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

# Print status with colors
print_status() {
    local status=$1
    local message=$2
    case $status in
        "PASS") echo -e "${GREEN}✅ PASS${NC} $message" ;;
        "FAIL") echo -e "${RED}❌ FAIL${NC} $message" ;;
        "WARN") echo -e "${YELLOW}⚠️  WARN${NC} $message" ;;
        "INFO") echo -e "${BLUE}ℹ️  INFO${NC} $message" ;;
        "DEBUG") [[ "$VERBOSE_MODE" == true ]] && echo -e "${CYAN}🔍 DEBUG${NC} $message" ;;
        "FIX") echo -e "${PURPLE}🔧 FIX${NC} $message" ;;
    esac
}

# Check Java Runtime Environment
check_java() {
    print_status "INFO" "Checking Java Runtime Environment..."
    
    if command -v java >/dev/null 2>&1; then
        local java_version_output=$(java -version 2>&1 | head -n1)
        print_status "DEBUG" "Java version output: $java_version_output"
        
        # Extract version number (handles both Oracle and OpenJDK format)
        local java_version=$(echo "$java_version_output" | sed -n 's/.*version "\([0-9]*\)\..*/\1/p')
        if [[ -z "$java_version" ]]; then
            # Try alternative format for newer Java versions
            java_version=$(echo "$java_version_output" | sed -n 's/.*version "\([0-9]*\)".*/\1/p')
        fi
        
        if [[ "$java_version" -ge 17 ]]; then
            print_status "PASS" "Java $java_version found"
            
            # Check JAVA_HOME
            if [[ -n "$JAVA_HOME" ]]; then
                print_status "DEBUG" "JAVA_HOME: $JAVA_HOME"
            else
                print_status "WARN" "JAVA_HOME not set (optional)"
            fi
            
            return 0
        else
            print_status "FAIL" "Java 17+ required, found version $java_version"
            if [[ "$FIX_MODE" == true ]]; then
                print_status "FIX" "Attempting to install Java 17..."
                if command -v apt >/dev/null 2>&1; then
                    sudo apt update && sudo apt install -y openjdk-17-jdk
                elif command -v dnf >/dev/null 2>&1; then
                    sudo dnf install -y java-17-openjdk-devel
                elif command -v brew >/dev/null 2>&1; then
                    brew install openjdk@17
                fi
            fi
            return 1
        fi
    else
        print_status "FAIL" "Java not found"
        if [[ "$FIX_MODE" == true ]]; then
            print_status "FIX" "Installing Java 17..."
            if command -v apt >/dev/null 2>&1; then
                sudo apt update && sudo apt install -y openjdk-17-jdk
            elif command -v dnf >/dev/null 2>&1; then
                sudo dnf install -y java-17-openjdk-devel
            elif command -v brew >/dev/null 2>&1; then
                brew install openjdk@17
            fi
        fi
        return 1
    fi
}

# Check ChromaDB Vector Database
check_chromadb() {
    print_status "INFO" "Checking ChromaDB Vector Database..."
    
    local chromadb_installed=false
    
    # Check Python package
    if python3 -c "import chromadb; print('ChromaDB version:', chromadb.__version__)" 2>/dev/null; then
        print_status "PASS" "ChromaDB Python package installed"
        chromadb_installed=true
    # Check Docker image
    elif docker images 2>/dev/null | grep -q chroma; then
        print_status "PASS" "ChromaDB Docker image available"
        chromadb_installed=true
    # Check system installation
    elif command -v chromadb >/dev/null 2>&1; then
        print_status "PASS" "ChromaDB system installation found"
        chromadb_installed=true
    else
        print_status "FAIL" "ChromaDB not installed"
        if [[ "$FIX_MODE" == true ]]; then
            print_status "FIX" "Installing ChromaDB..."
            if [[ -x "./setup_chromadb.sh" ]]; then
                ./setup_chromadb.sh --background
            else
                python3 -m venv chromadb_env
                source chromadb_env/bin/activate
                pip install chromadb
            fi
        fi
        chromadb_installed=false
    fi
    
    # Check service status
    if curl -s http://localhost:8000/api/v1/heartbeat >/dev/null 2>&1; then
        local heartbeat=$(curl -s http://localhost:8000/api/v1/heartbeat)
        print_status "PASS" "ChromaDB service running on port 8000"
        print_status "DEBUG" "Heartbeat response: $heartbeat"
        
        # Test basic functionality if not in quick mode
        if [[ "$QUICK_MODE" == false ]]; then
            local test_collection="agent_orange_test_$(date +%s)"
            if curl -s -X POST http://localhost:8000/api/v1/collections \
                -H "Content-Type: application/json" \
                -d "{\"name\": \"$test_collection\"}" >/dev/null 2>&1; then
                print_status "PASS" "ChromaDB API functional"
                # Cleanup test collection
                curl -s -X DELETE "http://localhost:8000/api/v1/collections/$test_collection" >/dev/null 2>&1
            else
                print_status "WARN" "ChromaDB API may have issues"
            fi
        fi
        
        return 0
    else
        print_status "FAIL" "ChromaDB service not running on port 8000"
        if [[ "$FIX_MODE" == true ]]; then
            print_status "FIX" "Starting ChromaDB service..."
            if command -v docker >/dev/null 2>&1; then
                docker run -d --name chromadb-agent-orange -p 8000:8000 chromadb/chroma:latest >/dev/null 2>&1
                sleep 3
            elif python3 -c "import chromadb" 2>/dev/null; then
                python3 -m chromadb.cli run --host localhost --port 8000 &
                sleep 3
            fi
        fi
        return 1
    fi
}

# Check Ghidra Reverse Engineering Platform
check_ghidra() {
    print_status "INFO" "Checking Ghidra Reverse Engineering Platform..."
    
    # Check for Ghidra installation in common locations
    local ghidra_paths=(
        "/opt/ghidra/support/analyzeHeadless"
        "/usr/local/ghidra/support/analyzeHeadless"
        "/Applications/ghidra/support/analyzeHeadless"
        "$HOME/.local/ghidra/support/analyzeHeadless"
    )
    
    # Also check environment variables
    if [[ -n "$GHIDRA_HOME" ]]; then
        ghidra_paths+=("$GHIDRA_HOME/support/analyzeHeadless")
    fi
    if [[ -n "$GHIDRA_ANALYZE_HEADLESS" ]]; then
        ghidra_paths+=("$GHIDRA_ANALYZE_HEADLESS")
    fi
    
    local found=false
    local ghidra_path=""
    
    for path in "${ghidra_paths[@]}"; do
        if [[ -x "$path" ]]; then
            print_status "PASS" "Ghidra found at $path"
            print_status "DEBUG" "analyzeHeadless path: $path"
            found=true
            ghidra_path="$path"
            break
        fi
    done
    
    if [[ "$found" == false ]]; then
        # Try to find analyzeHeadless anywhere
        local found_paths=$(find /usr /opt /Applications /home 2>/dev/null -name "analyzeHeadless" -executable 2>/dev/null | head -1)
        if [[ -n "$found_paths" ]]; then
            print_status "WARN" "Ghidra found at non-standard location: $found_paths"
            ghidra_path="$found_paths"
            found=true
        else
            print_status "FAIL" "Ghidra analyzeHeadless not found"
            if [[ "$FIX_MODE" == true ]]; then
                print_status "FIX" "Installing Ghidra..."
                if [[ -x "./setup_ghidra.sh" ]]; then
                    ./setup_ghidra.sh --user-install
                fi
            fi
            return 1
        fi
    fi
    
    # Test Ghidra functionality
    if [[ "$QUICK_MODE" == false && -n "$ghidra_path" ]]; then
        if timeout 10 "$ghidra_path" -help >/dev/null 2>&1; then
            print_status "PASS" "Ghidra executable responds to help command"
        else
            print_status "WARN" "Ghidra executable may have issues"
        fi
        
        # Test project directory creation
        local test_dir="/tmp/ghidra_test_$$"
        if mkdir -p "$test_dir" && touch "$test_dir/test" && rm -rf "$test_dir"; then
            print_status "PASS" "Ghidra project directory writable"
        else
            print_status "WARN" "Cannot create Ghidra project directories"
        fi
    fi
    
    # Test Agent-Orange integration
    if [[ -f "bin/chatbot.jar" ]]; then
        if timeout 10 java -cp bin/chatbot.jar com.example.GhidraBridge >/dev/null 2>&1; then
            print_status "PASS" "Ghidra integration working"
        else
            print_status "WARN" "Ghidra integration issues (check configuration)"
        fi
    fi
    
    return 0
}

# Check Llama.cpp LLM Engine
check_llama() {
    print_status "INFO" "Checking Llama.cpp LLM Engine..."
    
    # Check for headers
    if [[ -f "main/llama.h" ]]; then
        print_status "PASS" "Llama.cpp headers found in main/"
        print_status "DEBUG" "Header file: main/llama.h ($(stat -c%s main/llama.h 2>/dev/null || stat -f%z main/llama.h) bytes)"
        
        # Check if it's a real header or placeholder
        if grep -q "placeholder\|minimal\|demonstration" main/llama.h 2>/dev/null; then
            print_status "WARN" "Llama.cpp headers are minimal/placeholder only"
            print_status "INFO" "For full functionality, install complete llama.cpp"
        else
            print_status "PASS" "Llama.cpp headers appear complete"
        fi
    else
        print_status "FAIL" "Llama.cpp headers missing from main/"
        if [[ "$FIX_MODE" == true ]]; then
            print_status "FIX" "Installing Llama.cpp headers..."
            if [[ -x "./setup_llama.sh" ]]; then
                ./setup_llama.sh --install-method headers-only
            fi
        fi
        return 1
    fi
    
    # Check for native library
    local native_libs=(bin/libllama.so bin/libllama.dylib bin/llama.dll)
    local lib_found=false
    
    for lib in "${native_libs[@]}"; do
        if [[ -f "$lib" ]]; then
            print_status "PASS" "Native library found: $lib"
            print_status "DEBUG" "Library size: $(stat -c%s "$lib" 2>/dev/null || stat -f%z "$lib") bytes"
            lib_found=true
            
            # Check library dependencies on Linux
            if [[ "$lib" == *".so" ]] && command -v ldd >/dev/null 2>&1; then
                local missing_deps=$(ldd "$lib" 2>/dev/null | grep "not found" | wc -l)
                if [[ "$missing_deps" -eq 0 ]]; then
                    print_status "PASS" "Native library dependencies satisfied"
                else
                    print_status "WARN" "Native library has missing dependencies"
                fi
            fi
            break
        fi
    done
    
    if [[ "$lib_found" == false ]]; then
        print_status "WARN" "Llama.cpp native library not compiled"
        if [[ "$FIX_MODE" == true ]]; then
            print_status "FIX" "Attempting to compile native library..."
            if [[ -x "./build.sh" ]]; then
                ./build.sh native
            fi
        fi
    fi
    
    # Test library loading
    if [[ "$lib_found" == true && "$QUICK_MODE" == false ]]; then
        export LD_LIBRARY_PATH=$PWD/bin:$LD_LIBRARY_PATH
        export DYLD_LIBRARY_PATH=$PWD/bin:$DYLD_LIBRARY_PATH
        
        if timeout 10 java -cp bin/chatbot.jar com.example.LlamaJNI >/dev/null 2>&1; then
            print_status "PASS" "Llama.cpp library loads successfully"
        else
            print_status "WARN" "Llama.cpp library found but fails to load"
        fi
    fi
    
    return 0
}

# Check Build System Dependencies
check_build_system() {
    print_status "INFO" "Checking Build System..."
    
    # Check GCC/G++
    if command -v g++ >/dev/null 2>&1; then
        local gcc_version=$(g++ --version | head -n1)
        print_status "PASS" "GCC/G++ compiler available"
        print_status "DEBUG" "Compiler: $gcc_version"
    else
        print_status "FAIL" "GCC/G++ compiler missing"
        if [[ "$FIX_MODE" == true ]]; then
            print_status "FIX" "Installing build tools..."
            if command -v apt >/dev/null 2>&1; then
                sudo apt install -y build-essential
            elif command -v dnf >/dev/null 2>&1; then
                sudo dnf groupinstall -y "Development Tools"
            elif command -v brew >/dev/null 2>&1; then
                xcode-select --install
            fi
        fi
        return 1
    fi
    
    # Check Gradle
    if [[ -x "./gradlew" ]] && ./gradlew --version >/dev/null 2>&1; then
        local gradle_version=$(./gradlew --version | grep "Gradle" | head -n1)
        print_status "PASS" "Gradle build system working"
        print_status "DEBUG" "Build system: $gradle_version"
    else
        print_status "FAIL" "Gradle build system issues"
        return 1
    fi
    
    # Check JAR
    if [[ -f "bin/chatbot.jar" ]]; then
        local jar_size=$(stat -c%s bin/chatbot.jar 2>/dev/null || stat -f%z bin/chatbot.jar)
        print_status "PASS" "Application JAR built ($(( jar_size / 1024 / 1024 ))MB)"
        
        # Quick JAR integrity check
        if [[ "$QUICK_MODE" == false ]]; then
            if timeout 10 java -jar bin/chatbot.jar --help >/dev/null 2>&1; then
                print_status "PASS" "Application JAR runs successfully"
            else
                print_status "WARN" "Application JAR may have issues"
            fi
        fi
    else
        print_status "FAIL" "Application JAR missing"
        if [[ "$FIX_MODE" == true ]]; then
            print_status "FIX" "Building application JAR..."
            if [[ -x "./build.sh" ]]; then
                ./build.sh jar
            fi
        fi
        return 1
    fi
    
    return 0
}

# Generate dependency report
generate_report() {
    local all_passed=$1
    echo
    echo "📊 Dependency Status Report"
    echo "=========================="
    
    # Check each dependency and show status
    echo "| Component | Status | Notes |"
    echo "|-----------|--------|-------|"
    
    # Java
    if command -v java >/dev/null 2>&1; then
        local java_version=$(java -version 2>&1 | head -n1 | sed -n 's/.*version "\([^"]*\)".*/\1/p')
        echo "| Java | ✅ Available | Version: $java_version |"
    else
        echo "| Java | ❌ Missing | Required for application |"
    fi
    
    # ChromaDB
    if curl -s http://localhost:8000/api/v1/heartbeat >/dev/null 2>&1; then
        echo "| ChromaDB | ✅ Running | Port 8000 accessible |"
    elif python3 -c "import chromadb" 2>/dev/null || docker images 2>/dev/null | grep -q chroma; then
        echo "| ChromaDB | ⚠️ Installed | Service not running |"
    else
        echo "| ChromaDB | ❌ Missing | Vector database features unavailable |"
    fi
    
    # Ghidra
    local ghidra_found=false
    for path in "/opt/ghidra/support/analyzeHeadless" "/usr/local/ghidra/support/analyzeHeadless" "/Applications/ghidra/support/analyzeHeadless"; do
        if [[ -x "$path" ]]; then
            echo "| Ghidra | ✅ Available | Found at $path |"
            ghidra_found=true
            break
        fi
    done
    if [[ "$ghidra_found" == false ]]; then
        echo "| Ghidra | ❌ Missing | Binary analysis features unavailable |"
    fi
    
    # Llama.cpp
    if [[ -f "main/llama.h" ]]; then
        if ls bin/libllama.* >/dev/null 2>&1; then
            echo "| Llama.cpp | ✅ Complete | Headers and library available |"
        else
            echo "| Llama.cpp | ⚠️ Partial | Headers only, library not compiled |"
        fi
    else
        echo "| Llama.cpp | ❌ Missing | LLM features unavailable |"
    fi
    
    # Build System
    if command -v g++ >/dev/null 2>&1 && [[ -x "./gradlew" ]] && [[ -f "bin/chatbot.jar" ]]; then
        echo "| Build System | ✅ Complete | All tools available |"
    else
        echo "| Build System | ❌ Issues | Some components missing |"
    fi
    
    echo
}

# Main execution function
main() {
    echo "🔍 Agent-Orange Dependency Checker"
    echo "=================================="
    echo
    
    if [[ "$QUICK_MODE" == true ]]; then
        print_status "INFO" "Running in quick mode (limited tests)"
    fi
    
    if [[ "$FIX_MODE" == true ]]; then
        print_status "INFO" "Auto-fix mode enabled"
    fi
    
    echo
    
    local all_passed=true
    local checks_run=0
    local checks_passed=0
    
    # Run all checks
    if check_java; then
        ((checks_passed++))
    else
        all_passed=false
    fi
    ((checks_run++))
    echo
    
    if check_chromadb; then
        ((checks_passed++))
    else
        all_passed=false
    fi
    ((checks_run++))
    echo
    
    if check_ghidra; then
        ((checks_passed++))
    else
        all_passed=false
    fi
    ((checks_run++))
    echo
    
    if check_llama; then
        ((checks_passed++))
    else
        all_passed=false
    fi
    ((checks_run++))
    echo
    
    if check_build_system; then
        ((checks_passed++))
    else
        all_passed=false
    fi
    ((checks_run++))
    echo
    
    # Generate report
    generate_report "$all_passed"
    
    # Summary
    echo "📈 Summary: $checks_passed/$checks_run checks passed"
    echo
    
    if [[ "$all_passed" == true ]]; then
        print_status "PASS" "All dependencies verified successfully!"
        echo
        print_status "INFO" "Agent-Orange is ready for full functionality"
        echo
        print_status "INFO" "You can now run:"
        echo "  java -jar bin/chatbot.jar"
        echo "  ./run_chatbot.sh"
    else
        print_status "WARN" "Some dependencies have issues"
        echo
        print_status "INFO" "To fix issues automatically, run:"
        echo "  $0 --fix"
        echo
        print_status "INFO" "Or fix manually using specific setup scripts:"
        echo "  ./setup_chromadb.sh    - Fix ChromaDB"
        echo "  ./setup_ghidra.sh      - Fix Ghidra"  
        echo "  ./setup_llama.sh       - Fix Llama.cpp"
        echo "  ./build.sh clean jar   - Rebuild application"
    fi
    
    # Return appropriate exit code
    if [[ "$all_passed" == true ]]; then
        exit 0
    else
        exit 1
    fi
}

# Run main function
main "$@"