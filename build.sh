#!/bin/bash

# ============================================================================
# Agent-Orange Build Script
# ============================================================================
#
# This script compiles the Java project and creates the executable JAR file.
# It handles dependency checking, compilation, and packaging.
#
# USAGE:
#   ./build.sh
#
# DEPENDENCIES:
#   - Java 17+ (OpenJDK)
#   - javac compiler
#   - jar tool
# ============================================================================

set -e  # Exit on any error

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

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
    esac
}

# Check if Java is available
check_java() {
    if ! command -v java >/dev/null 2>&1; then
        print_status "ERROR" "Java not found. Please install Java 17+ first."
        exit 1
    fi
    
    local version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
    local major_version=$(echo $version | cut -d'.' -f1)
    
    if [[ $major_version -lt 17 ]]; then
        print_status "ERROR" "Java version $version found. Java 17+ is required."
        exit 1
    fi
    
    print_status "SUCCESS" "Java $version found"
}

# Check if javac is available
check_javac() {
    if ! command -v javac >/dev/null 2>&1; then
        print_status "ERROR" "javac not found. Please install Java Development Kit (JDK)."
        exit 1
    fi
    
    print_status "SUCCESS" "javac compiler found"
}

# Check if jar tool is available
check_jar() {
    if ! command -v jar >/dev/null 2>&1; then
        print_status "ERROR" "jar tool not found. Please install Java Development Kit (JDK)."
        exit 1
    fi
    
    print_status "SUCCESS" "jar tool found"
}

# Create necessary directories
create_directories() {
    print_status "INFO" "Creating build directories..."
    
    mkdir -p bin
    mkdir -p logs
    mkdir -p tmp
    
    print_status "SUCCESS" "Directories created"
}

# Compile Java source files
compile_java() {
    print_status "INFO" "Compiling Java source files..."
    
    # Find all Java files in main directory
    local java_files=$(find main -name "*.java" -type f)
    
    if [[ -z "$java_files" ]]; then
        print_status "ERROR" "No Java source files found in main/ directory"
        exit 1
    fi
    
    # Compile with error reporting
    if javac -d bin -cp ".:bin" $java_files 2>logs/compile_errors.log; then
        print_status "SUCCESS" "Java compilation completed"
        rm -f logs/compile_errors.log
    else
        print_status "ERROR" "Java compilation failed. Check logs/compile_errors.log"
        cat logs/compile_errors.log
        exit 1
    fi
}

# Create JAR file
create_jar() {
    print_status "INFO" "Creating JAR file..."
    
    # Check if compiled classes exist
    if [[ ! -d "bin" ]] || [[ -z "$(ls -A bin 2>/dev/null)" ]]; then
        print_status "ERROR" "No compiled classes found. Run compilation first."
        exit 1
    fi
    
    # Create JAR with manifest
    if jar cf bin/chatbot.jar -C bin .; then
        print_status "SUCCESS" "JAR file created: bin/chatbot.jar"
    else
        print_status "ERROR" "Failed to create JAR file"
        exit 1
    fi
}

# Create executable JAR with main class
create_executable_jar() {
    print_status "INFO" "Creating executable JAR..."
    
    # Create manifest file
    cat > tmp/manifest.txt << EOF
Manifest-Version: 1.0
Main-Class: Main
Class-Path: .

EOF
    
    # Create executable JAR
    if jar cfm bin/chatbot.jar tmp/manifest.txt -C bin .; then
        print_status "SUCCESS" "Executable JAR created: bin/chatbot.jar"
        chmod +x bin/chatbot.jar
    else
        print_status "ERROR" "Failed to create executable JAR"
        exit 1
    fi
    
    # Clean up
    rm -f tmp/manifest.txt
}

# Verify build
verify_build() {
    print_status "INFO" "Verifying build..."
    
    if [[ -f "bin/chatbot.jar" ]]; then
        print_status "SUCCESS" "Build verification passed"
        print_status "INFO" "JAR file size: $(du -h bin/chatbot.jar | cut -f1)"
    else
        print_status "ERROR" "Build verification failed - JAR file not found"
        exit 1
    fi
}

# Main build process
main() {
    echo
    print_status "INFO" "Starting Agent-Orange build process..."
    echo
    
    # Check dependencies
    check_java
    check_javac
    check_jar
    
    # Create directories
    create_directories
    
    # Compile Java
    compile_java
    
    # Create JAR
    create_jar
    
    # Create executable JAR
    create_executable_jar
    
    # Verify build
    verify_build
    
    echo
    print_status "SUCCESS" "Build completed successfully!"
    print_status "INFO" "You can now run: ./run_chatbot.sh"
    echo
}

# Run main function
main "$@" 