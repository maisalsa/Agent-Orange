#!/bin/bash

# ============================================================================
# Agent-Orange Enhanced Build Script with Gradle Integration
# ============================================================================
#
# This script provides a modern build system using Gradle for dependency 
# management, compilation, testing, and packaging. It includes automatic
# dependency management, security scanning, and native library handling.
#
# USAGE:
#   ./build.sh [target]
#
# TARGETS:
#   clean          - Clean build artifacts
#   dependencies   - Download and verify dependencies
#   compile        - Compile Java sources
#   test           - Run test suite
#   jar            - Create executable JAR
#   security       - Run security scans
#   all (default)  - Complete build with all steps
#
# DEPENDENCIES:
#   - Java 17+ (OpenJDK)
#   - Gradle (auto-downloaded via wrapper)
#   - GCC/G++ (for native libraries)
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

# Check if GCC is available for native compilation
check_gcc() {
    if ! command -v gcc >/dev/null 2>&1; then
        print_status "WARNING" "GCC not found. Native library compilation may fail."
        print_status "INFO" "Install GCC: sudo apt-get install build-essential (Ubuntu/Debian)"
        return 1
    fi
    
    if ! command -v g++ >/dev/null 2>&1; then
        print_status "WARNING" "G++ not found. Native library compilation may fail."
        return 1
    fi
    
    print_status "SUCCESS" "GCC/G++ compiler found"
    return 0
}

# Initialize Gradle wrapper if needed
init_gradle() {
    print_status "INFO" "Initializing Gradle build system..."
    
    # Download Gradle wrapper if not present
    if [[ ! -f "gradlew" ]]; then
        print_status "INFO" "Downloading Gradle wrapper..."
        if command -v gradle >/dev/null 2>&1; then
            gradle wrapper --gradle-version 8.6
        else
            # Download wrapper manually
            curl -s -L https://services.gradle.org/distributions/gradle-8.6-bin.zip -o gradle.zip
            unzip -q gradle.zip
            mv gradle-8.6 gradle
            rm gradle.zip
            chmod +x gradle/bin/gradle
            gradle/bin/gradle wrapper
            rm -rf gradle
        fi
        chmod +x gradlew
    fi
    
    print_status "SUCCESS" "Gradle wrapper ready"
}

# Gradle build functions
gradle_clean() {
    print_status "INFO" "Cleaning build artifacts..."
    ./gradlew clean
    print_status "SUCCESS" "Clean completed"
}

gradle_download_dependencies() {
    print_status "INFO" "Downloading and verifying dependencies..."
    ./gradlew downloadDependencies
    print_status "SUCCESS" "Dependencies downloaded"
}

gradle_compile() {
    print_status "INFO" "Compiling Java sources with dependency management..."
    ./gradlew compileJava
    print_status "SUCCESS" "Compilation completed"
}

gradle_test() {
    print_status "INFO" "Running test suite..."
    ./gradlew test
    print_status "SUCCESS" "Tests completed"
}

gradle_jar() {
    print_status "INFO" "Creating executable JAR with all dependencies..."
    ./gradlew jar
    print_status "SUCCESS" "JAR created: bin/chatbot.jar"
}

gradle_security_check() {
    print_status "INFO" "Running security scans on dependencies..."
    ./gradlew securityCheck
    print_status "SUCCESS" "Security check completed"
}

gradle_list_dependencies() {
    print_status "INFO" "Listing all project dependencies..."
    ./gradlew listDependencies
}

# Native library compilation
compile_native_libs() {
    print_status "INFO" "Compiling native libraries..."
    
    if check_gcc; then
        if ./gradlew compileJNI 2>/dev/null; then
            print_status "SUCCESS" "Native libraries compiled"
        else
            print_status "WARNING" "Native library compilation skipped"
            print_status "INFO" "This is normal if llama.cpp is not installed locally"
            print_status "INFO" "The application can still run with external native libraries"
            print_status "INFO" "To enable JNI compilation, install llama.cpp and copy headers to main/"
        fi
    else
        print_status "WARNING" "Skipping native library compilation (GCC not available)"
        print_status "INFO" "The application may still work if native libraries are provided separately"
    fi
}

# Complete build process
gradle_build_all() {
    print_status "INFO" "Running complete build process..."
    
    gradle_clean
    gradle_download_dependencies
    gradle_compile
    compile_native_libs
    gradle_test
    gradle_jar
    
    print_status "SUCCESS" "Complete build finished successfully!"
}

# Display help information
show_help() {
    echo
    echo "Agent-Orange Enhanced Build System"
    echo "================================="
    echo
    echo "USAGE: $0 [target]"
    echo
    echo "TARGETS:"
    echo "  clean          - Clean all build artifacts"
    echo "  dependencies   - Download and verify all dependencies"
    echo "  compile        - Compile Java sources"
    echo "  test           - Run the complete test suite"
    echo "  jar            - Create executable JAR file"
    echo "  security       - Run security scans on dependencies"
    echo "  native         - Compile native libraries (JNI)"
    echo "  list-deps      - List all project dependencies"
    echo "  all            - Complete build (default)"
    echo "  help           - Show this help message"
    echo
    echo "EXAMPLES:"
    echo "  $0              # Run complete build"
    echo "  $0 clean        # Clean build artifacts"
    echo "  $0 security     # Check for vulnerable dependencies"
    echo "  $0 test         # Run tests only"
    echo
    echo "FEATURES:"
    echo "  - Automatic dependency management with Gradle"
    echo "  - Security vulnerability scanning"
    echo "  - Native library compilation (JNI)"
    echo "  - Cross-platform support"
    echo "  - Incremental builds"
    echo
}

# Main build process
main() {
    local target="${1:-all}"
    
    echo
    print_status "INFO" "Starting Agent-Orange enhanced build system..."
    print_status "INFO" "Target: $target"
    echo
    
    # Check system dependencies
    check_java
    check_gcc
    
    # Initialize Gradle
    init_gradle
    
    # Execute based on target
    case "$target" in
        "clean")
            gradle_clean
            ;;
        "dependencies")
            gradle_download_dependencies
            ;;
        "compile")
            gradle_download_dependencies
            gradle_compile
            ;;
        "test")
            gradle_download_dependencies
            gradle_compile
            gradle_test
            ;;
        "jar")
            gradle_download_dependencies
            gradle_compile
            compile_native_libs
            gradle_jar
            ;;
        "security")
            gradle_download_dependencies
            gradle_security_check
            ;;
        "native")
            compile_native_libs
            ;;
        "list-deps")
            gradle_list_dependencies
            ;;
        "all")
            gradle_build_all
            ;;
        "help"|"-h"|"--help")
            show_help
            exit 0
            ;;
        *)
            print_status "ERROR" "Unknown target: $target"
            show_help
            exit 1
            ;;
    esac
    
    echo
    print_status "SUCCESS" "Build target '$target' completed successfully!"
    if [[ "$target" == "all" || "$target" == "jar" ]]; then
        print_status "INFO" "You can now run: ./run_chatbot.sh"
        if [[ -f "bin/chatbot.jar" ]]; then
            print_status "INFO" "JAR file size: $(du -h bin/chatbot.jar | cut -f1)"
        fi
    fi
    echo
}

# Run main function
main "$@" 