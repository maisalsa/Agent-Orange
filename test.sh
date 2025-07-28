#!/bin/bash

# ============================================================================
# Agent-Orange Test Script
# ============================================================================
#
# This script runs comprehensive tests to validate the project setup,
# including unit tests, integration tests, and system validation.
#
# USAGE:
#   ./test.sh
#
# DEPENDENCIES:
#   - Java 17+ (OpenJDK)
#   - Compiled project (run build.sh first)
#   - JUnit 4 (optional, for unit tests)
# ============================================================================

set -e  # Exit on any error

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# Test counters
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Print colored output
print_status() {
    local status=$1
    local message=$2
    
    case $status in
        "INFO")
            echo -e "${BLUE}[INFO]${NC} $message"
            ;;
        "SUCCESS")
            echo -e "${GREEN}[SUCCESS]${NC} $message"
            ;;
        "WARNING")
            echo -e "${YELLOW}[WARNING]${NC} $message"
            ;;
        "ERROR")
            echo -e "${RED}[ERROR]${NC} $message"
            ;;
        "HEADER")
            echo -e "${PURPLE}$message${NC}"
            ;;
    esac
}

# Print test header
print_test_header() {
    echo
    # Check if terminal supports UTF-8, otherwise use ASCII fallback
    if [[ "$LANG" =~ [Uu][Tt][Ff]-?8 ]] && [[ "$TERM" != "dumb" ]]; then
        print_status "HEADER" "╔════════════════════════════════════════════════════════════════════╗"
        print_status "HEADER" "║                        Agent-Orange Test Suite                        ║"
        print_status "HEADER" "╠════════════════════════════════════════════════════════════════════╣"
        local date_str="$(date)"
        local date_content="Date: $date_str"
        local padding=$((68 - ${#date_content}))
        printf -v date_line "║  %s%*s║" "$date_content" $padding ""
        print_status "HEADER" "$date_line"
        print_status "HEADER" "╚════════════════════════════════════════════════════════════════════╝"
    else
        # ASCII fallback for terminals without UTF-8 support
        print_status "HEADER" "+====================================================================+"
        print_status "HEADER" "|                        Agent-Orange Test Suite                        |"
        print_status "HEADER" "+====================================================================+"
        local date_str="$(date)"
        local date_content="Date: $date_str"
        local padding=$((68 - ${#date_content}))
        printf -v date_line "|  %s%*s|" "$date_content" $padding ""
        print_status "HEADER" "$date_line"
        print_status "HEADER" "+====================================================================+"
    fi
    echo
}

# Print test footer
print_test_footer() {
    echo
    # Check if terminal supports UTF-8, otherwise use ASCII fallback
    if [[ "$LANG" =~ [Uu][Tt][Ff]-?8 ]] && [[ "$TERM" != "dumb" ]]; then
        print_status "HEADER" "╔════════════════════════════════════════════════════════════════════╗"
        print_status "HEADER" "║                            Test Summary                            ║"
        print_status "HEADER" "╠════════════════════════════════════════════════════════════════════╣"
        
        # Format test statistics with proper alignment
        local total_content="Total Tests: $TOTAL_TESTS"
        local passed_content="Passed: $PASSED_TESTS"
        local failed_content="Failed: $FAILED_TESTS"
        
        local total_padding=$((68 - ${#total_content}))
        local passed_padding=$((68 - ${#passed_content}))
        local failed_padding=$((68 - ${#failed_content}))
        
        printf -v total_line "║  %s%*s║" "$total_content" $total_padding ""
        printf -v passed_line "║  %s%*s║" "$passed_content" $passed_padding ""
        printf -v failed_line "║  %s%*s║" "$failed_content" $failed_padding ""
        
        print_status "HEADER" "$total_line"
        print_status "HEADER" "$passed_line"
        print_status "HEADER" "$failed_line"
        print_status "HEADER" "╚════════════════════════════════════════════════════════════════════╝"
    else
        # ASCII fallback for terminals without UTF-8 support
        print_status "HEADER" "+====================================================================+"
        print_status "HEADER" "|                            Test Summary                            |"
        print_status "HEADER" "+====================================================================+"
        
        # Format test statistics with proper alignment
        local total_content="Total Tests: $TOTAL_TESTS"
        local passed_content="Passed: $PASSED_TESTS"
        local failed_content="Failed: $FAILED_TESTS"
        
        local total_padding=$((68 - ${#total_content}))
        local passed_padding=$((68 - ${#passed_content}))
        local failed_padding=$((68 - ${#failed_content}))
        
        printf -v total_line "|  %s%*s|" "$total_content" $total_padding ""
        printf -v passed_line "|  %s%*s|" "$passed_content" $passed_padding ""
        printf -v failed_line "|  %s%*s|" "$failed_content" $failed_padding ""
        
        print_status "HEADER" "$total_line"
        print_status "HEADER" "$passed_line"
        print_status "HEADER" "$failed_line"
        print_status "HEADER" "+====================================================================+"
    fi
    echo
}

# Run a test and update counters
run_test() {
    local test_name="$1"
    local test_command="$2"
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    print_status "INFO" "Running test: $test_name"
    
    if eval "$test_command" >/dev/null 2>&1; then
        print_status "SUCCESS" "✓ $test_name passed"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        return 0
    else
        print_status "ERROR" "✗ $test_name failed"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        return 1
    fi
}

# Check if file exists
test_file_exists() {
    local file="$1"
    local description="$2"
    
    if [[ -f "$file" ]]; then
        print_status "SUCCESS" "✓ $description found: $file"
        return 0
    else
        print_status "ERROR" "✗ $description missing: $file"
        return 1
    fi
}

# Check if directory exists
test_directory_exists() {
    local dir="$1"
    local description="$2"
    
    if [[ -d "$dir" ]]; then
        print_status "SUCCESS" "✓ $description found: $dir"
        return 0
    else
        print_status "ERROR" "✗ $description missing: $dir"
        return 1
    fi
}

# Check if command is available
test_command() {
    local cmd="$1"
    local description="$2"
    
    if command -v "$cmd" >/dev/null 2>&1; then
        print_status "SUCCESS" "✓ $description found: $(command -v $cmd)"
        return 0
    else
        print_status "ERROR" "✗ $description not found: $cmd"
        return 1
    fi
}

# Test Java compilation
test_java_compilation() {
    print_status "INFO" "Testing Java compilation..."
    
    # Check if Java files exist
    local java_files=$(find main -name "*.java" -type f 2>/dev/null | wc -l)
    if [[ $java_files -eq 0 ]]; then
        print_status "ERROR" "No Java source files found"
        return 1
    fi
    
    print_status "SUCCESS" "Found $java_files Java source files"
    
    # Try to compile
    if javac -d bin -cp ".:bin" main/*.java 2>/dev/null; then
        print_status "SUCCESS" "Java compilation successful"
        return 0
    else
        print_status "ERROR" "Java compilation failed"
        return 1
    fi
}

# Test JAR creation
test_jar_creation() {
    print_status "INFO" "Testing JAR creation..."
    
    if [[ ! -d "bin" ]] || [[ -z "$(ls -A bin 2>/dev/null)" ]]; then
        print_status "ERROR" "No compiled classes found"
        return 1
    fi
    
    if jar cf bin/test.jar -C bin . 2>/dev/null; then
        print_status "SUCCESS" "JAR creation successful"
        rm -f bin/test.jar
        return 0
    else
        print_status "ERROR" "JAR creation failed"
        return 1
    fi
}

# Test basic Java execution
test_java_execution() {
    print_status "INFO" "Testing Java execution..."
    
    # Create a simple test class
    cat > tmp/TestExecution.java << 'EOF'
public class TestExecution {
    public static void main(String[] args) {
        System.out.println("Java execution test successful");
    }
}
EOF
    
    if javac -d bin tmp/TestExecution.java 2>/dev/null && \
       java -cp bin TestExecution 2>/dev/null | grep -q "successful"; then
        print_status "SUCCESS" "Java execution test passed"
        rm -f tmp/TestExecution.java bin/TestExecution.class
        return 0
    else
        print_status "ERROR" "Java execution test failed"
        rm -f tmp/TestExecution.java bin/TestExecution.class 2>/dev/null
        return 1
    fi
}

# Test project structure
test_project_structure() {
    print_status "INFO" "Testing project structure..."
    
    local structure_ok=true
    
    # Check required directories
    test_directory_exists "main" "Source directory" || structure_ok=false
    test_directory_exists "bin" "Binary directory" || structure_ok=false
    
    # Check required files
    test_file_exists "main/Main.java" "Main class" || structure_ok=false
    test_file_exists "main/LlamaJNI.java" "LLM interface" || structure_ok=false
    test_file_exists "main/MCPOrchestrator.java" "Orchestrator" || structure_ok=false
    test_file_exists "application.properties" "Configuration file" || structure_ok=false
    test_file_exists "install.sh" "Installation script" || structure_ok=false
    test_file_exists "run_chatbot.sh" "Startup script" || structure_ok=false
    test_file_exists "build.sh" "Build script" || structure_ok=false
    
    if [[ $structure_ok == true ]]; then
        print_status "SUCCESS" "Project structure validation passed"
        return 0
    else
        print_status "ERROR" "Project structure validation failed"
        return 1
    fi
}

# Test script permissions
test_script_permissions() {
    print_status "INFO" "Testing script permissions..."
    
    local permissions_ok=true
    
    # Check if scripts are executable
    if [[ ! -x "install.sh" ]]; then
        print_status "ERROR" "install.sh is not executable"
        permissions_ok=false
    fi
    
    if [[ ! -x "run_chatbot.sh" ]]; then
        print_status "ERROR" "run_chatbot.sh is not executable"
        permissions_ok=false
    fi
    
    if [[ ! -x "build.sh" ]]; then
        print_status "ERROR" "build.sh is not executable"
        permissions_ok=false
    fi
    
    if [[ $permissions_ok == true ]]; then
        print_status "SUCCESS" "Script permissions validation passed"
        return 0
    else
        print_status "ERROR" "Script permissions validation failed"
        return 1
    fi
}

# Test configuration
test_configuration() {
    print_status "INFO" "Testing configuration..."
    
    local config_ok=true
    
    # Check if application.properties has required keys
    if [[ -f "application.properties" ]]; then
        if grep -q "vectordb.endpoint" application.properties; then
            print_status "SUCCESS" "Vector DB endpoint configured"
        else
            print_status "WARNING" "Vector DB endpoint not configured"
        fi
        
        if grep -q "ghidra.headless.path" application.properties; then
            print_status "SUCCESS" "Ghidra path configured"
        else
            print_status "WARNING" "Ghidra path not configured"
        fi
    else
        print_status "ERROR" "application.properties not found"
        config_ok=false
    fi
    
    if [[ $config_ok == true ]]; then
        print_status "SUCCESS" "Configuration validation passed"
        return 0
    else
        print_status "ERROR" "Configuration validation failed"
        return 1
    fi
}

# Test system dependencies
test_system_dependencies() {
    print_status "INFO" "Testing system dependencies..."
    
    local deps_ok=true
    
    # Check Java
    if ! test_command "java" "Java runtime"; then
        deps_ok=false
    fi
    
    # Check javac
    if ! test_command "javac" "Java compiler"; then
        deps_ok=false
    fi
    
    # Check jar
    if ! test_command "jar" "JAR tool"; then
        deps_ok=false
    fi
    
    # Check Java version
    local version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
    local major_version=$(echo $version | cut -d'.' -f1)
    
    if [[ $major_version -ge 17 ]]; then
        print_status "SUCCESS" "Java version $version (meets requirement: 17+)"
    else
        print_status "ERROR" "Java version $version (requires 17+)"
        deps_ok=false
    fi
    
    if [[ $deps_ok == true ]]; then
        print_status "SUCCESS" "System dependencies validation passed"
        return 0
    else
        print_status "ERROR" "System dependencies validation failed"
        return 1
    fi
}

# Test optional dependencies
test_optional_dependencies() {
    print_status "INFO" "Testing optional dependencies..."
    
    # Check Python 3
    if test_command "python3" "Python 3"; then
        local py_version=$(python3 --version 2>&1)
        print_status "SUCCESS" "Python 3 found: $py_version"
    else
        print_status "WARNING" "Python 3 not found (optional)"
    fi
    
    # Check curl for ChromaDB testing
    if test_command "curl" "curl"; then
        print_status "SUCCESS" "curl found (for ChromaDB testing)"
    else
        print_status "WARNING" "curl not found (ChromaDB testing limited)"
    fi
    
    # Check Ghidra (optional)
    local ghidra_found=false
    for path in "/opt/ghidra/support/analyzeHeadless" "/usr/share/ghidra/support/analyzeHeadless" "/usr/local/ghidra/support/analyzeHeadless"; do
        if [[ -f "$path" && -x "$path" ]]; then
            print_status "SUCCESS" "Ghidra found: $path"
            ghidra_found=true
            break
        fi
    done
    
    if [[ $ghidra_found == false ]]; then
        print_status "WARNING" "Ghidra not found (binary analysis will not work)"
    fi
    
    print_status "SUCCESS" "Optional dependencies check completed"
}

# Test ChromaDB connectivity
test_chromadb_connectivity() {
    print_status "INFO" "Testing ChromaDB connectivity..."
    
    if command -v curl >/dev/null 2>&1; then
        if curl -s http://localhost:8000/api/v1/heartbeat >/dev/null 2>&1; then
            print_status "SUCCESS" "ChromaDB instance found at http://localhost:8000"
        else
            print_status "WARNING" "ChromaDB instance not found at http://localhost:8000"
            print_status "INFO" "ChromaDB is optional - vector database features will not work"
        fi
    else
        print_status "WARNING" "curl not available - cannot test ChromaDB connectivity"
    fi
}

# Run all tests
run_all_tests() {
    print_test_header
    
    # Create temporary directory
    mkdir -p tmp
    
    # System tests
    print_status "INFO" "=== System Tests ==="
    run_test "System Dependencies" "test_system_dependencies"
    run_test "Optional Dependencies" "test_optional_dependencies"
    
    # Project tests
    print_status "INFO" "=== Project Tests ==="
    run_test "Project Structure" "test_project_structure"
    run_test "Script Permissions" "test_script_permissions"
    run_test "Configuration" "test_configuration"
    
    # Build tests
    print_status "INFO" "=== Build Tests ==="
    run_test "Java Compilation" "test_java_compilation"
    run_test "JAR Creation" "test_jar_creation"
    run_test "Java Execution" "test_java_execution"
    
    # Integration tests
    print_status "INFO" "=== Integration Tests ==="
    run_test "ChromaDB Connectivity" "test_chromadb_connectivity"
    
    # Clean up
    rm -rf tmp
    
    print_test_footer
    
    # Return appropriate exit code
    if [[ $FAILED_TESTS -eq 0 ]]; then
        print_status "SUCCESS" "All tests passed! Project is ready for use."
        exit 0
    else
        print_status "ERROR" "$FAILED_TESTS test(s) failed. Please fix issues before proceeding."
        exit 1
    fi
}

# Main execution
main() {
    run_all_tests
}

# Run main function
main "$@" 