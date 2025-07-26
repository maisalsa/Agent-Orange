#!/bin/bash

# ============================================================================
# Pentesting Chatbot Installation Script
# ============================================================================
#
# This script checks for and installs all required dependencies for the
# pentesting chatbot project. It supports Debian/Kali/Ubuntu and Arch/BlackArch
# systems and provides clear status reporting for each dependency.
#
# FEATURES:
# - Comprehensive dependency checking
# - Automatic installation via package managers
# - Clear status reporting for each component
# - Idempotent operation (safe to run multiple times)
# - Platform detection and appropriate package manager selection
# - Helpful error messages and installation guidance
#
# USAGE:
#   ./install.sh
#
# DEPENDENCIES CHECKED:
# - Java 17+ (OpenJDK)
# - GCC/G++ compiler toolchain
# - CMake build system
# - Python 3 (optional)
# - Ghidra headless installation
# - ChromaDB (local instance check)
# - Project files (.so, .jar, config files)
#
# EXIT CODES:
#   0 - Success (all dependencies satisfied)
#   1 - Missing dependencies that cannot be auto-installed
#   2 - Package manager not supported
#   3 - Insufficient privileges
#   4 - Critical error during installation
#
# AUTHOR: Agent-Orange Team
# VERSION: 1.0.0
# ============================================================================

set -e  # Exit on any error

# ============================================================================
# CONFIGURATION AND CONSTANTS
# ============================================================================

# Script configuration
SCRIPT_NAME="install.sh"
SCRIPT_VERSION="1.0.0"
PROJECT_NAME="Pentesting Chatbot"

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Status tracking
TOTAL_DEPENDENCIES=0
INSTALLED_DEPENDENCIES=0
MISSING_DEPENDENCIES=0
FAILED_INSTALLATIONS=0

# Required project files
REQUIRED_FILES=(
    "main/LlamaJNI.java"
    "main/EmbeddingClient.java"
    "main/ChromaDBClient.java"
    "main/GhidraBridge.java"
    "main/MCPOrchestrator.java"
    "main/Main.java"
    "main/llama_jni.cpp"
    "main/ExtractFunctions.java"
    "application.properties"
    "install.sh"
    "run_chatbot.sh"
)

# Expected binary/library files (may not exist yet)
EXPECTED_BINARIES=(
    "bin/chatbot.jar"
    "bin/libllama.so"
    "bin/libllama.dylib"
    "bin/llama.dll"
)

# ============================================================================
# UTILITY FUNCTIONS
# ============================================================================

# Print colored output
print_status() {
    local status=$1
    local message=$2
    local color=$3
    
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
        "STEP")
            echo -e "${CYAN}$message${NC}"
            ;;
    esac
}

# Print header with project information
print_header() {
    echo
    print_status "HEADER" "╔══════════════════════════════════════════════════════════════╗"
    print_status "HEADER" "║                    $PROJECT_NAME Installation                    ║"
    print_status "HEADER" "╠══════════════════════════════════════════════════════════════╣"
    print_status "HEADER" "║  Script: $SCRIPT_NAME v$SCRIPT_VERSION                              ║"
    print_status "HEADER" "║  Date: $(date)                                    ║"
    print_status "HEADER" "╚══════════════════════════════════════════════════════════════╝"
    echo
}

# Print footer with summary
print_footer() {
    echo
    print_status "HEADER" "╔══════════════════════════════════════════════════════════════╗"
    print_status "HEADER" "║                        Installation Summary                    ║"
    print_status "HEADER" "╠══════════════════════════════════════════════════════════════╣"
    print_status "HEADER" "║  Total Dependencies: $TOTAL_DEPENDENCIES                                    ║"
    print_status "HEADER" "║  Installed/Found: $INSTALLED_DEPENDENCIES                                    ║"
    print_status "HEADER" "║  Missing: $MISSING_DEPENDENCIES                                    ║"
    print_status "HEADER" "║  Failed Installations: $FAILED_INSTALLATIONS                                    ║"
    print_status "HEADER" "╚══════════════════════════════════════════════════════════════╝"
    echo
}

# Check if running as root
check_privileges() {
    if [[ $EUID -eq 0 ]]; then
        print_status "WARNING" "Running as root. This is not recommended for security reasons."
        read -p "Continue anyway? (y/N): " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            print_status "ERROR" "Installation aborted by user."
            exit 1
        fi
    fi
}

# Detect operating system and package manager
detect_system() {
    print_status "STEP" "Detecting system and package manager..."
    
    if [[ -f /etc/os-release ]]; then
        . /etc/os-release
        OS=$NAME
        VER=$VERSION_ID
    else
        print_status "ERROR" "Could not detect operating system"
        exit 2
    fi
    
    # Detect package manager
    if command -v apt-get >/dev/null 2>&1; then
        PKG_MANAGER="apt-get"
        PKG_INSTALL="apt-get install -y"
        PKG_UPDATE="apt-get update"
        print_status "INFO" "Detected Debian/Ubuntu system with apt-get"
    elif command -v pacman >/dev/null 2>&1; then
        PKG_MANAGER="pacman"
        PKG_INSTALL="pacman -S --noconfirm"
        PKG_UPDATE="pacman -Sy"
        print_status "INFO" "Detected Arch/BlackArch system with pacman"
    else
        print_status "ERROR" "Unsupported package manager. Only apt-get and pacman are supported."
        exit 2
    fi
    
    print_status "SUCCESS" "System detected: $OS $VER with $PKG_MANAGER"
}

# Update package manager
update_package_manager() {
    print_status "STEP" "Updating package manager..."
    
    if [[ $PKG_MANAGER == "apt-get" ]]; then
        sudo $PKG_UPDATE
    elif [[ $PKG_MANAGER == "pacman" ]]; then
        sudo $PKG_UPDATE
    fi
    
    print_status "SUCCESS" "Package manager updated"
}

# Check if command exists and is executable
check_command() {
    local cmd=$1
    local name=${2:-$1}
    
    if command -v "$cmd" >/dev/null 2>&1; then
        print_status "SUCCESS" "$name found: $(command -v $cmd)"
        ((INSTALLED_DEPENDENCIES++))
        return 0
    else
        print_status "WARNING" "$name not found"
        ((MISSING_DEPENDENCIES++))
        return 1
    fi
}

# Install package using detected package manager
install_package() {
    local package=$1
    local name=${2:-$1}
    
    print_status "STEP" "Installing $name..."
    
    if sudo $PKG_INSTALL "$package"; then
        print_status "SUCCESS" "$name installed successfully"
        ((INSTALLED_DEPENDENCIES++))
        ((MISSING_DEPENDENCIES--))
        return 0
    else
        print_status "ERROR" "Failed to install $name"
        ((FAILED_INSTALLATIONS++))
        return 1
    fi
}

# Check Java version
check_java_version() {
    if check_command "java" "Java"; then
        local version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
        local major_version=$(echo $version | cut -d'.' -f1)
        
        if [[ $major_version -ge 17 ]]; then
            print_status "SUCCESS" "Java version $version (meets requirement: 17+)"
        else
            print_status "WARNING" "Java version $version (requires 17+)"
            ((MISSING_DEPENDENCIES++))
            ((INSTALLED_DEPENDENCIES--))
            return 1
        fi
    else
        return 1
    fi
}

# Install Java
install_java() {
    print_status "STEP" "Installing Java 17+..."
    
    if [[ $PKG_MANAGER == "apt-get" ]]; then
        # Try to install OpenJDK 17 or higher
        if sudo $PKG_INSTALL "openjdk-17-jdk"; then
            print_status "SUCCESS" "OpenJDK 17 installed"
            return 0
        elif sudo $PKG_INSTALL "openjdk-11-jdk"; then
            print_status "WARNING" "OpenJDK 11 installed (minimum recommended: 17)"
            return 0
        else
            print_status "ERROR" "Failed to install Java"
            return 1
        fi
    elif [[ $PKG_MANAGER == "pacman" ]]; then
        if sudo $PKG_INSTALL "jdk-openjdk"; then
            print_status "SUCCESS" "OpenJDK installed"
            return 0
        else
            print_status "ERROR" "Failed to install Java"
            return 1
        fi
    fi
}

# Check and install compiler toolchain
check_compiler() {
    print_status "STEP" "Checking compiler toolchain..."
    
    local compiler_found=false
    
    if check_command "gcc" "GCC"; then
        compiler_found=true
    fi
    
    if check_command "g++" "G++"; then
        compiler_found=true
    fi
    
    if [[ $compiler_found == false ]]; then
        print_status "STEP" "Installing compiler toolchain..."
        
        if [[ $PKG_MANAGER == "apt-get" ]]; then
            install_package "build-essential" "Build Tools"
        elif [[ $PKG_MANAGER == "pacman" ]]; then
            install_package "base-devel" "Build Tools"
        fi
    fi
}

# Check and install CMake
check_cmake() {
    print_status "STEP" "Checking CMake..."
    
    if ! check_command "cmake" "CMake"; then
        print_status "STEP" "Installing CMake..."
        
        if [[ $PKG_MANAGER == "apt-get" ]]; then
            install_package "cmake" "CMake"
        elif [[ $PKG_MANAGER == "pacman" ]]; then
            install_package "cmake" "CMake"
        fi
    fi
}

# Check Python 3 (optional)
check_python() {
    print_status "STEP" "Checking Python 3 (optional)..."
    
    if check_command "python3" "Python 3"; then
        local version=$(python3 --version 2>&1)
        print_status "SUCCESS" "Python 3 found: $version"
    else
        print_status "WARNING" "Python 3 not found (optional dependency)"
        print_status "INFO" "Python 3 is optional and not required for core functionality"
    fi
}

# Check Ghidra installation
check_ghidra() {
    print_status "STEP" "Checking Ghidra installation..."
    
    # Common Ghidra installation paths
    local ghidra_paths=(
        "/opt/ghidra/support/analyzeHeadless"
        "/usr/local/ghidra/support/analyzeHeadless"
        "/Applications/ghidra/support/analyzeHeadless"
        "$HOME/ghidra/support/analyzeHeadless"
        "$HOME/ghidra_*/support/analyzeHeadless"
    )
    
    local ghidra_found=false
    
    for path in "${ghidra_paths[@]}"; do
        if [[ -f "$path" && -x "$path" ]]; then
            print_status "SUCCESS" "Ghidra found: $path"
            ghidra_found=true
            ((INSTALLED_DEPENDENCIES++))
            break
        fi
    done
    
    if [[ $ghidra_found == false ]]; then
        print_status "WARNING" "Ghidra not found"
        print_status "INFO" "Ghidra is required for binary analysis functionality"
        print_status "INFO" "Download from: https://ghidra-sre.org/"
        print_status "INFO" "Installation instructions:"
        print_status "INFO" "  1. Download Ghidra from https://ghidra-sre.org/"
        print_status "INFO" "  2. Extract to /opt/ghidra or /usr/local/ghidra"
        print_status "INFO" "  3. Ensure analyzeHeadless script is executable"
        print_status "INFO" "  4. Run this script again to verify installation"
        ((MISSING_DEPENDENCIES++))
    fi
}

# Check ChromaDB (local instance)
check_chromadb() {
    print_status "STEP" "Checking ChromaDB (local instance)..."
    
    # Try to connect to local ChromaDB instance
    if command -v curl >/dev/null 2>&1; then
        if curl -s http://localhost:8000/api/v1/heartbeat >/dev/null 2>&1; then
            print_status "SUCCESS" "ChromaDB instance found at http://localhost:8000"
            ((INSTALLED_DEPENDENCIES++))
        else
            print_status "WARNING" "ChromaDB instance not found at http://localhost:8000"
            print_status "INFO" "ChromaDB is required for vector database functionality"
            print_status "INFO" "To install ChromaDB:"
            print_status "INFO" "  pip install chromadb"
            print_status "INFO" "  chroma run --host localhost --port 8000"
            ((MISSING_DEPENDENCIES++))
        fi
    else
        print_status "WARNING" "curl not available, cannot check ChromaDB"
        print_status "INFO" "Install curl to enable ChromaDB checking"
        ((MISSING_DEPENDENCIES++))
    fi
}

# Check project files
check_project_files() {
    print_status "STEP" "Checking project files..."
    
    local missing_files=()
    
    for file in "${REQUIRED_FILES[@]}"; do
        if [[ -f "$file" ]]; then
            print_status "SUCCESS" "Found: $file"
            ((INSTALLED_DEPENDENCIES++))
        else
            print_status "WARNING" "Missing: $file"
            missing_files+=("$file")
            ((MISSING_DEPENDENCIES++))
        fi
    done
    
    if [[ ${#missing_files[@]} -gt 0 ]]; then
        print_status "ERROR" "Missing required project files:"
        for file in "${missing_files[@]}"; do
            print_status "ERROR" "  - $file"
        done
        print_status "ERROR" "Please ensure all project files are present before running this script"
        return 1
    fi
    
    return 0
}

# Check binary/library files (may not exist yet)
check_binary_files() {
    print_status "STEP" "Checking binary/library files..."
    
    local found_binaries=0
    
    for binary in "${EXPECTED_BINARIES[@]}"; do
        if [[ -f "$binary" ]]; then
            print_status "SUCCESS" "Found: $binary"
            ((found_binaries++))
        else
            print_status "INFO" "Not found (will be built): $binary"
        fi
    done
    
    if [[ $found_binaries -eq 0 ]]; then
        print_status "WARNING" "No binary files found. These will need to be built:"
        print_status "INFO" "  - Compile llama_jni.cpp to create libllama.so"
        print_status "INFO" "  - Build Java project to create chatbot.jar"
        print_status "INFO" "  - Place files in bin/ directory"
    else
        print_status "SUCCESS" "Found $found_binaries binary file(s)"
    fi
}

# Create necessary directories
create_directories() {
    print_status "STEP" "Creating necessary directories..."
    
    local directories=("bin" "models" "logs" "tmp")
    
    for dir in "${directories[@]}"; do
        if [[ ! -d "$dir" ]]; then
            mkdir -p "$dir"
            print_status "SUCCESS" "Created directory: $dir"
        else
            print_status "INFO" "Directory exists: $dir"
        fi
    done
}

# Set up environment variables
setup_environment() {
    print_status "STEP" "Setting up environment variables..."
    
    # Create .env file if it doesn't exist
    if [[ ! -f ".env" ]]; then
        cat > .env << EOF
# Pentesting Chatbot Environment Variables
export LD_LIBRARY_PATH="\$(pwd)/bin:\$LD_LIBRARY_PATH"
export DYLD_LIBRARY_PATH="\$(pwd)/bin:\$DYLD_LIBRARY_PATH"
export JAVA_OPTS="-Xmx4g -Xms2g"
export GHIDRA_PATH="/opt/ghidra/support/analyzeHeadless"
export CHROMADB_ENDPOINT="http://localhost:8000"
EOF
        print_status "SUCCESS" "Created .env file with environment variables"
    else
        print_status "INFO" ".env file already exists"
    fi
    
    # Make scripts executable
    chmod +x install.sh run_chatbot.sh 2>/dev/null || true
    print_status "SUCCESS" "Made scripts executable"
}

# Print usage instructions
print_usage_instructions() {
    echo
    print_status "HEADER" "╔══════════════════════════════════════════════════════════════╗"
    print_status "HEADER" "║                        Usage Instructions                       ║"
    print_status "HEADER" "╠══════════════════════════════════════════════════════════════╣"
    print_status "HEADER" "║  To start the chatbot:                                        ║"
    print_status "HEADER" "║    ./run_chatbot.sh                                           ║"
    print_status "HEADER" "║                                                               ║"
    print_status "HEADER" "║  To build the project:                                        ║"
    print_status "HEADER" "║    1. Compile llama_jni.cpp: g++ -fPIC -shared -lllama -o bin/libllama.so main/llama_jni.cpp ║"
    print_status "HEADER" "║    2. Build Java project: javac -d bin main/*.java            ║"
    print_status "HEADER" "║    3. Create JAR: jar cf bin/chatbot.jar -C bin .            ║"
    print_status "HEADER" "║                                                               ║"
    print_status "HEADER" "║  To configure the chatbot:                                    ║"
    print_status "HEADER" "║    Edit application.properties for settings                  ║"
    print_status "HEADER" "║    Edit .env for environment variables                       ║"
    print_status "HEADER" "╚══════════════════════════════════════════════════════════════╝"
    echo
}

# ============================================================================
# MAIN INSTALLATION PROCESS
# ============================================================================

main() {
    print_header
    
    # Check privileges
    check_privileges
    
    # Detect system
    detect_system
    
    # Update package manager
    update_package_manager
    
    # Initialize counters
    TOTAL_DEPENDENCIES=0
    INSTALLED_DEPENDENCIES=0
    MISSING_DEPENDENCIES=0
    FAILED_INSTALLATIONS=0
    
    # Check and install dependencies
    print_status "STEP" "Checking and installing dependencies..."
    
    # Java
    TOTAL_DEPENDENCIES=$((TOTAL_DEPENDENCIES + 1))
    if ! check_java_version; then
        if ! install_java; then
            print_status "ERROR" "Java installation failed. Please install Java 17+ manually."
            ((FAILED_INSTALLATIONS++))
        fi
    fi
    
    # Compiler toolchain
    TOTAL_DEPENDENCIES=$((TOTAL_DEPENDENCIES + 1))
    check_compiler
    
    # CMake
    TOTAL_DEPENDENCIES=$((TOTAL_DEPENDENCIES + 1))
    check_cmake
    
    # Python 3 (optional)
    check_python
    
    # Ghidra
    TOTAL_DEPENDENCIES=$((TOTAL_DEPENDENCIES + 1))
    check_ghidra
    
    # ChromaDB
    TOTAL_DEPENDENCIES=$((TOTAL_DEPENDENCIES + 1))
    check_chromadb
    
    # Project files
    TOTAL_DEPENDENCIES=$((TOTAL_DEPENDENCIES + 1))
    if ! check_project_files; then
        print_status "ERROR" "Critical project files are missing. Please ensure all files are present."
        exit 1
    fi
    
    # Binary files
    check_binary_files
    
    # Create directories
    create_directories
    
    # Setup environment
    setup_environment
    
    # Print summary
    print_footer
    
    # Print usage instructions
    print_usage_instructions
    
    # Final status
    if [[ $MISSING_DEPENDENCIES -eq 0 && $FAILED_INSTALLATIONS -eq 0 ]]; then
        print_status "SUCCESS" "Installation completed successfully!"
        print_status "SUCCESS" "All dependencies are satisfied."
        exit 0
    elif [[ $FAILED_INSTALLATIONS -gt 0 ]]; then
        print_status "ERROR" "Installation completed with errors."
        print_status "ERROR" "Please resolve the failed installations before proceeding."
        exit 1
    else
        print_status "WARNING" "Installation completed with warnings."
        print_status "WARNING" "Some optional dependencies are missing but core functionality should work."
        exit 0
    fi
}

# ============================================================================
# SCRIPT EXECUTION
# ============================================================================

# Check if script is being sourced
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    # Script is being executed directly
    main "$@"
else
    # Script is being sourced
    print_status "WARNING" "This script should be executed directly, not sourced."
    print_status "INFO" "Run: ./install.sh"
fi 