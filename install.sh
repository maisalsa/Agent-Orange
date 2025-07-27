#!/bin/bash

# ============================================================================
# Agent-Orange Automated Installation Script
# ============================================================================
#
# This script provides a comprehensive, hands-off installation for the
# Agent-Orange pentesting chatbot across multiple operating systems.
#
# SUPPORTED SYSTEMS:
# - Debian/Ubuntu/Kali (apt)
# - Arch/BlackArch (pacman)
# - Fedora/RHEL/CentOS (dnf/yum)
# - macOS (Homebrew)
# - Alpine Linux (apk)
#
# FEATURES:
# - Automatic OS detection and package manager selection
# - Comprehensive dependency checking and installation
# - Automatic Ghidra download and setup
# - ChromaDB installation and server startup
# - Global CLI command installation
# - Minimal user interaction required
# - Self-healing and error recovery
#
# USAGE:
#   ./install.sh
#
# DEPENDENCIES INSTALLED:
# - Java 17+ (OpenJDK)
# - Build tools (gcc, g++, cmake, make)
# - Network tools (wget, curl)
# - Python 3 and pip
# - Ghidra (latest version)
# - ChromaDB (with server startup)
#
# EXIT CODES:
#   0 - Success (all dependencies satisfied)
#   1 - Missing dependencies that cannot be auto-installed
#   2 - Unsupported operating system
#   3 - Insufficient privileges for required operations
#   4 - Critical error during installation
#
# AUTHOR: Agent-Orange Team
# VERSION: 2.0.0
# ============================================================================

set -e  # Exit on any error

# ============================================================================
# CONFIGURATION AND CONSTANTS
# ============================================================================

# Script configuration
SCRIPT_NAME="install.sh"
SCRIPT_VERSION="2.0.0"
PROJECT_NAME="Agent-Orange"

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

# Required system commands
REQUIRED_COMMANDS=(
    "java"
    "javac"
    "gcc"
    "g++"
    "cmake"
    "make"
    "wget"
    "curl"
)

# Required project files
REQUIRED_FILES=(
    "main/Main.java"
    "main/LlamaJNI.java"
    "main/MCPOrchestrator.java"
    "main/EmbeddingClient.java"
    "main/ChromaDBClient.java"
    "main/GhidraBridge.java"
    "main/llama_jni.cpp"
    "agnt-orange"
    "application.properties"
)

# Ghidra installation paths to check
GHIDRA_PATHS=(
    "/opt/ghidra/support/analyzeHeadless"
    "/usr/local/ghidra/support/analyzeHeadless"
    "/usr/share/ghidra/support/analyzeHeadless"
    "$HOME/ghidra/support/analyzeHeadless"
    "$HOME/ghidra_*/support/analyzeHeadless"
)

# ============================================================================
# UTILITY FUNCTIONS
# ============================================================================

# Terminal detection and compatibility functions
detect_terminal_capabilities() {
    # Check if we're in a terminal
    if [[ ! -t 1 ]]; then
        # Not a terminal, disable fancy output
        TERMINAL_SUPPORTS_UTF8=false
        TERMINAL_WIDTH=80
        return
    fi
    
    # Get terminal width
    if command -v tput >/dev/null 2>&1; then
        TERMINAL_WIDTH=$(tput cols 2>/dev/null || echo 80)
    else
        TERMINAL_WIDTH=80
    fi
    
    # Ensure minimum width
    if [[ $TERMINAL_WIDTH -lt 60 ]]; then
        TERMINAL_WIDTH=60
    fi
    
    # Check UTF-8 support
    TERMINAL_SUPPORTS_UTF8=false
    if [[ -n "$LANG" && "$LANG" =~ UTF-8 ]]; then
        TERMINAL_SUPPORTS_UTF8=true
    elif [[ -n "$LC_ALL" && "$LC_ALL" =~ UTF-8 ]]; then
        TERMINAL_SUPPORTS_UTF8=true
    elif [[ -n "$LC_CTYPE" && "$LC_CTYPE" =~ UTF-8 ]]; then
        TERMINAL_SUPPORTS_UTF8=true
    fi
    
    # Additional check for terminal type
    if [[ -n "$TERM" ]]; then
        case "$TERM" in
            *"xterm"*|*"screen"*|*"tmux"*|*"linux"*)
                TERMINAL_SUPPORTS_UTF8=true
                ;;
        esac
    fi
    
    # Test UTF-8 output capability
    if [[ "$TERMINAL_SUPPORTS_UTF8" == true ]]; then
        # Test if terminal can display UTF-8 characters
        if ! echo -e "\xe2\x95\x94" >/dev/null 2>&1; then
            TERMINAL_SUPPORTS_UTF8=false
        fi
    fi
}

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
        "STEP")
            echo -e "${CYAN}$message${NC}"
            ;;
    esac
}

# Print simple header for narrow terminals or non-UTF-8 terminals
print_simple_header() {
    echo
    print_status "HEADER" "================================================================================"
    print_status "HEADER" "                    $PROJECT_NAME Installation"
    print_status "HEADER" "================================================================================"
    print_status "HEADER" "  Script: $SCRIPT_NAME v$SCRIPT_VERSION"
    print_status "HEADER" "  Date: $(date)"
    print_status "HEADER" "================================================================================"
    echo
}

# Print header with project information
print_header() {
    # Check terminal capabilities
    detect_terminal_capabilities
    
    # Use simple header for narrow terminals or non-UTF-8 terminals
    if [[ $TERMINAL_WIDTH -lt 70 || "$TERMINAL_SUPPORTS_UTF8" != true ]]; then
        print_simple_header
        return
    fi
    
    # Calculate dynamic width for the box
    local box_width=$((TERMINAL_WIDTH - 4))
    local title="$PROJECT_NAME Installation"
    local script_info="Script: $SCRIPT_NAME v$SCRIPT_VERSION"
    local date_info="Date: $(date)"
    
    # Ensure minimum width for content
    if [[ $box_width -lt 60 ]]; then
        box_width=60
    fi
    
    # Create dynamic box borders
    local top_border="╔$(printf '═%.0s' $(seq 1 $((box_width-2))))╗"
    local separator="╠$(printf '═%.0s' $(seq 1 $((box_width-2))))╣"
    local bottom_border="╚$(printf '═%.0s' $(seq 1 $((box_width-2))))╝"
    
    # Center the title
    local title_padding=$(( (box_width - ${#title}) / 2 ))
    local title_line="║$(printf ' %.0s' $(seq 1 $title_padding))$title$(printf ' %.0s' $(seq 1 $((box_width - title_padding - ${#title}))))║"
    
    # Format script info and date
    local script_line="║  $script_info$(printf ' %.0s' $(seq 1 $((box_width - ${#script_info} - 3))))║"
    local date_line="║  $date_info$(printf ' %.0s' $(seq 1 $((box_width - ${#date_info} - 3))))║"
    
    echo
    print_status "HEADER" "$top_border"
    print_status "HEADER" "$title_line"
    print_status "HEADER" "$separator"
    print_status "HEADER" "$script_line"
    print_status "HEADER" "$date_line"
    print_status "HEADER" "$bottom_border"
    echo
}

# Print footer with summary
print_footer() {
    # Use simple footer for narrow terminals or non-UTF-8 terminals
    if [[ $TERMINAL_WIDTH -lt 70 || "$TERMINAL_SUPPORTS_UTF8" != true ]]; then
        echo
        print_status "HEADER" "================================================================================"
        print_status "HEADER" "                        Installation Summary"
        print_status "HEADER" "================================================================================"
        print_status "HEADER" "  Total Dependencies: $TOTAL_DEPENDENCIES"
        print_status "HEADER" "  Installed/Found: $INSTALLED_DEPENDENCIES"
        print_status "HEADER" "  Missing: $MISSING_DEPENDENCIES"
        print_status "HEADER" "  Failed Installations: $FAILED_INSTALLATIONS"
        print_status "HEADER" "================================================================================"
        echo
        return
    fi
    
    # Calculate dynamic width for the box
    local box_width=$((TERMINAL_WIDTH - 4))
    local title="Installation Summary"
    
    # Ensure minimum width for content
    if [[ $box_width -lt 60 ]]; then
        box_width=60
    fi
    
    # Create dynamic box borders
    local top_border="╔$(printf '═%.0s' $(seq 1 $((box_width-2))))╗"
    local separator="╠$(printf '═%.0s' $(seq 1 $((box_width-2))))╣"
    local bottom_border="╚$(printf '═%.0s' $(seq 1 $((box_width-2))))╝"
    
    # Center the title
    local title_padding=$(( (box_width - ${#title}) / 2 ))
    local title_line="║$(printf ' %.0s' $(seq 1 $title_padding))$title$(printf ' %.0s' $(seq 1 $((box_width - title_padding - ${#title}))))║"
    
    # Format summary lines
    local total_line="║  Total Dependencies: $TOTAL_DEPENDENCIES$(printf ' %.0s' $(seq 1 $((box_width - 25 - ${#TOTAL_DEPENDENCIES}))))║"
    local installed_line="║  Installed/Found: $INSTALLED_DEPENDENCIES$(printf ' %.0s' $(seq 1 $((box_width - 20 - ${#INSTALLED_DEPENDENCIES}))))║"
    local missing_line="║  Missing: $MISSING_DEPENDENCIES$(printf ' %.0s' $(seq 1 $((box_width - 12 - ${#MISSING_DEPENDENCIES}))))║"
    local failed_line="║  Failed Installations: $FAILED_INSTALLATIONS$(printf ' %.0s' $(seq 1 $((box_width - 24 - ${#FAILED_INSTALLATIONS}))))║"
    
    echo
    print_status "HEADER" "$top_border"
    print_status "HEADER" "$title_line"
    print_status "HEADER" "$separator"
    print_status "HEADER" "$total_line"
    print_status "HEADER" "$installed_line"
    print_status "HEADER" "$missing_line"
    print_status "HEADER" "$failed_line"
    print_status "HEADER" "$bottom_border"
    echo
}

# Check if running as root
check_privileges() {
    if [[ $EUID -eq 0 ]]; then
        print_status "INFO" "Running as root - will install system-wide"
        IS_ROOT=true
    else
        print_status "INFO" "Running as regular user - will install to user directories"
        IS_ROOT=false
    fi
}

# Detect operating system and package manager
detect_system() {
    print_status "STEP" "Detecting operating system and package manager..."
    
    # Initialize variables
    OS=""
    OS_VERSION=""
    PKG_MANAGER=""
    PKG_INSTALL=""
    PKG_UPDATE=""
    PKG_SEARCH=""
    
    # Detect OS
    if [[ -f /etc/os-release ]]; then
        . /etc/os-release
        OS=$NAME
        OS_VERSION=$VERSION_ID
    elif [[ "$OSTYPE" == "darwin"* ]]; then
        OS="macOS"
        OS_VERSION=$(sw_vers -productVersion)
    else
        print_status "ERROR" "Could not detect operating system"
        exit 2
    fi
    
    # Detect package manager based on OS
    case "$OS" in
        *"Ubuntu"*|*"Debian"*|*"Kali"*|*"Linux Mint"*)
            PKG_MANAGER="apt"
            PKG_INSTALL="apt-get install -y"
            PKG_UPDATE="apt-get update"
            PKG_SEARCH="apt-cache search"
            print_status "INFO" "Detected Debian-based system with apt"
            ;;
        *"Arch"*|*"BlackArch"*|*"Manjaro"*)
            PKG_MANAGER="pacman"
            PKG_INSTALL="pacman -S --noconfirm"
            PKG_UPDATE="pacman -Sy"
            PKG_SEARCH="pacman -Ss"
            print_status "INFO" "Detected Arch-based system with pacman"
            ;;
        *"Fedora"*|*"Red Hat"*|*"CentOS"*|*"Rocky"*|*"AlmaLinux"*)
            if command -v dnf >/dev/null 2>&1; then
                PKG_MANAGER="dnf"
                PKG_INSTALL="dnf install -y"
                PKG_UPDATE="dnf update -y"
                PKG_SEARCH="dnf search"
            else
                PKG_MANAGER="yum"
                PKG_INSTALL="yum install -y"
                PKG_UPDATE="yum update -y"
                PKG_SEARCH="yum search"
            fi
            print_status "INFO" "Detected RHEL-based system with $PKG_MANAGER"
            ;;
        *"Alpine"*)
            PKG_MANAGER="apk"
            PKG_INSTALL="apk add"
            PKG_UPDATE="apk update"
            PKG_SEARCH="apk search"
            print_status "INFO" "Detected Alpine Linux with apk"
            ;;
        *"macOS"*)
            if command -v brew >/dev/null 2>&1; then
                PKG_MANAGER="brew"
                PKG_INSTALL="brew install"
                PKG_UPDATE="brew update"
                PKG_SEARCH="brew search"
                print_status "INFO" "Detected macOS with Homebrew"
            else
                print_status "ERROR" "Homebrew not found on macOS. Please install Homebrew first:"
                print_status "INFO" "  /bin/bash -c \"\$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)\""
                exit 2
            fi
            ;;
        *)
            print_status "ERROR" "Unsupported operating system: $OS"
            print_status "INFO" "Supported systems: Ubuntu/Debian/Kali, Arch/BlackArch, Fedora/RHEL, Alpine, macOS"
            exit 2
            ;;
    esac
    
    print_status "SUCCESS" "System detected: $OS $OS_VERSION with $PKG_MANAGER"
}

# Update package manager
update_package_manager() {
    print_status "STEP" "Updating package manager..."
    
    if [[ $PKG_MANAGER == "apt" ]]; then
        sudo $PKG_UPDATE
    elif [[ $PKG_MANAGER == "pacman" ]]; then
        sudo $PKG_UPDATE
    elif [[ $PKG_MANAGER == "dnf" || $PKG_MANAGER == "yum" ]]; then
        sudo $PKG_UPDATE
    elif [[ $PKG_MANAGER == "apk" ]]; then
        sudo $PKG_UPDATE
    elif [[ $PKG_MANAGER == "brew" ]]; then
        $PKG_UPDATE
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
    
    if [[ $IS_ROOT == true ]]; then
        # Root user - install directly
        if $PKG_INSTALL "$package"; then
            print_status "SUCCESS" "$name installed successfully"
            ((INSTALLED_DEPENDENCIES++))
            ((MISSING_DEPENDENCIES--))
            return 0
        else
            print_status "ERROR" "Failed to install $name"
            ((FAILED_INSTALLATIONS++))
            return 1
        fi
    else
        # Non-root user - provide installation command
        print_status "INFO" "Please run the following command to install $name:"
        if [[ $PKG_MANAGER == "apt" ]]; then
            print_status "INFO" "  sudo apt-get install $package"
        elif [[ $PKG_MANAGER == "pacman" ]]; then
            print_status "INFO" "  sudo pacman -S $package"
        elif [[ $PKG_MANAGER == "dnf" ]]; then
            print_status "INFO" "  sudo dnf install $package"
        elif [[ $PKG_MANAGER == "yum" ]]; then
            print_status "INFO" "  sudo yum install $package"
        elif [[ $PKG_MANAGER == "apk" ]]; then
            print_status "INFO" "  sudo apk add $package"
        elif [[ $PKG_MANAGER == "brew" ]]; then
            print_status "INFO" "  brew install $package"
        fi
        return 1
    fi
}

# Check and install system commands
check_system_commands() {
    print_status "STEP" "Checking required system commands..."
    
    local missing_commands=()
    
    for cmd in "${REQUIRED_COMMANDS[@]}"; do
        if ! check_command "$cmd"; then
            missing_commands+=("$cmd")
        fi
    done
    
    if [[ ${#missing_commands[@]} -gt 0 ]]; then
        print_status "STEP" "Installing missing system commands..."
        
        if [[ $PKG_MANAGER == "apt" ]]; then
            # Install build tools and development packages
            local packages=()
            for cmd in "${missing_commands[@]}"; do
                case $cmd in
                    "java"|"javac")
                        packages+=("openjdk-17-jdk")
                        ;;
                    "gcc"|"g++")
                        packages+=("build-essential")
                        ;;
                    "cmake")
                        packages+=("cmake")
                        ;;
                    "make")
                        packages+=("build-essential")
                        ;;
                    "wget")
                        packages+=("wget")
                        ;;
                    "curl")
                        packages+=("curl")
                        ;;
                esac
            done
            
            # Remove duplicates and install
            packages=($(printf "%s\n" "${packages[@]}" | sort -u))
            for pkg in "${packages[@]}"; do
                install_package "$pkg" "$pkg"
            done
            
        elif [[ $PKG_MANAGER == "pacman" ]]; then
            # Install build tools and development packages
            local packages=()
            for cmd in "${missing_commands[@]}"; do
                case $cmd in
                    "java"|"javac")
                        packages+=("jdk-openjdk")
                        ;;
                    "gcc"|"g++"|"make")
                        packages+=("base-devel")
                        ;;
                    "cmake")
                        packages+=("cmake")
                        ;;
                    "wget")
                        packages+=("wget")
                        ;;
                    "curl")
                        packages+=("curl")
                        ;;
                esac
            done
            
            # Remove duplicates and install
            packages=($(printf "%s\n" "${packages[@]}" | sort -u))
            for pkg in "${packages[@]}"; do
                install_package "$pkg" "$pkg"
            done
            
        elif [[ $PKG_MANAGER == "dnf" || $PKG_MANAGER == "yum" ]]; then
            # Install build tools and development packages
            local packages=()
            for cmd in "${missing_commands[@]}"; do
                case $cmd in
                    "java"|"javac")
                        packages+=("java-17-openjdk-devel")
                        ;;
                    "gcc"|"g++"|"make")
                        packages+=("gcc-c++ make")
                        ;;
                    "cmake")
                        packages+=("cmake")
                        ;;
                    "wget")
                        packages+=("wget")
                        ;;
                    "curl")
                        packages+=("curl")
                        ;;
                esac
            done
            
            # Remove duplicates and install
            packages=($(printf "%s\n" "${packages[@]}" | sort -u))
            for pkg in "${packages[@]}"; do
                install_package "$pkg" "$pkg"
            done
            
        elif [[ $PKG_MANAGER == "apk" ]]; then
            # Install build tools and development packages
            local packages=()
            for cmd in "${missing_commands[@]}"; do
                case $cmd in
                    "java"|"javac")
                        packages+=("openjdk17")
                        ;;
                    "gcc"|"g++"|"make")
                        packages+=("build-base")
                        ;;
                    "cmake")
                        packages+=("cmake")
                        ;;
                    "wget")
                        packages+=("wget")
                        ;;
                    "curl")
                        packages+=("curl")
                        ;;
                esac
            done
            
            # Remove duplicates and install
            packages=($(printf "%s\n" "${packages[@]}" | sort -u))
            for pkg in "${packages[@]}"; do
                install_package "$pkg" "$pkg"
            done
            
        elif [[ $PKG_MANAGER == "brew" ]]; then
            # Install build tools and development packages
            local packages=()
            for cmd in "${missing_commands[@]}"; do
                case $cmd in
                    "java"|"javac")
                        packages+=("openjdk@17")
                        ;;
                    "gcc"|"g++"|"make")
                        packages+=("gcc make")
                        ;;
                    "cmake")
                        packages+=("cmake")
                        ;;
                    "wget")
                        packages+=("wget")
                        ;;
                    "curl")
                        packages+=("curl")
                        ;;
                esac
            done
            
            # Remove duplicates and install
            packages=($(printf "%s\n" "${packages[@]}" | sort -u))
            for pkg in "${packages[@]}"; do
                install_package "$pkg" "$pkg"
            done
        fi
        
        # Re-check commands after installation
        print_status "STEP" "Re-checking system commands after installation..."
        for cmd in "${missing_commands[@]}"; do
            if check_command "$cmd"; then
                print_status "SUCCESS" "$cmd successfully installed"
            else
                print_status "ERROR" "Failed to install $cmd"
                ((FAILED_INSTALLATIONS++))
            fi
        done
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

# Download and install Ghidra
install_ghidra() {
    print_status "STEP" "Installing Ghidra..."
    
    # Check if Ghidra is already installed
    local ghidra_found=false
    local found_path=""
    
    for path in "${GHIDRA_PATHS[@]}"; do
        # Handle wildcards in paths
        if [[ "$path" == *"*"* ]]; then
            for expanded_path in $path; do
                if [[ -f "$expanded_path" && -x "$expanded_path" ]]; then
                    print_status "SUCCESS" "Ghidra found: $expanded_path"
                    ghidra_found=true
                    found_path="$expanded_path"
                    ((INSTALLED_DEPENDENCIES++))
                    break 2
                fi
            done
        else
            if [[ -f "$path" && -x "$path" ]]; then
                print_status "SUCCESS" "Ghidra found: $path"
                ghidra_found=true
                found_path="$path"
                ((INSTALLED_DEPENDENCIES++))
                break
            fi
        fi
    done
    
    if [[ $ghidra_found == false ]]; then
        print_status "STEP" "Ghidra not found, downloading and installing..."
        
        # Determine installation directory
        local install_dir=""
        if [[ $IS_ROOT == true ]]; then
            install_dir="/opt/ghidra"
        else
            install_dir="$HOME/ghidra"
        fi
        
        # Create installation directory
        mkdir -p "$install_dir"
        
        # Get latest Ghidra version
        local ghidra_url="https://github.com/NationalSecurityAgency/ghidra/releases/latest/download/ghidra_10.4.2_PUBLIC_20231228.zip"
        
        # Download Ghidra
        print_status "INFO" "Downloading Ghidra from GitHub..."
        if wget -q --show-progress "$ghidra_url" -O /tmp/ghidra.zip; then
            print_status "SUCCESS" "Ghidra downloaded successfully"
        else
            print_status "ERROR" "Failed to download Ghidra"
            print_status "INFO" "Please download manually from: https://ghidra-sre.org/"
            ((FAILED_INSTALLATIONS++))
            return 1
        fi
        
        # Extract Ghidra
        print_status "INFO" "Extracting Ghidra..."
        if unzip -q /tmp/ghidra.zip -d /tmp/; then
            print_status "SUCCESS" "Ghidra extracted successfully"
        else
            print_status "ERROR" "Failed to extract Ghidra"
            ((FAILED_INSTALLATIONS++))
            return 1
        fi
        
        # Move to installation directory
        local extracted_dir=$(find /tmp -maxdepth 1 -name "ghidra_*" -type d | head -n 1)
        if [[ -n "$extracted_dir" ]]; then
            if mv "$extracted_dir" "$install_dir"; then
                print_status "SUCCESS" "Ghidra moved to: $install_dir"
            else
                print_status "ERROR" "Failed to move Ghidra to installation directory"
                ((FAILED_INSTALLATIONS++))
                return 1
            fi
        else
            print_status "ERROR" "Could not find extracted Ghidra directory"
            ((FAILED_INSTALLATIONS++))
            return 1
        fi
        
        # Set executable permissions
        local analyze_headless="$install_dir/ghidra_*/support/analyzeHeadless"
        if [[ -f "$analyze_headless" ]]; then
            chmod +x "$analyze_headless"
            print_status "SUCCESS" "Set executable permissions on analyzeHeadless"
            ((INSTALLED_DEPENDENCIES++))
        else
            print_status "ERROR" "Could not find analyzeHeadless script"
            ((FAILED_INSTALLATIONS++))
            return 1
        fi
        
        # Clean up
        rm -f /tmp/ghidra.zip
        print_status "SUCCESS" "Ghidra installation completed"
    fi
}

# Install and setup ChromaDB
install_chromadb() {
    print_status "STEP" "Installing and setting up ChromaDB..."
    
    # Check if ChromaDB is installed
    local chromadb_installed=false
    if command -v chroma >/dev/null 2>&1; then
        print_status "SUCCESS" "ChromaDB CLI found: $(command -v chroma)"
        chromadb_installed=true
    elif python3 -c "import chromadb" 2>/dev/null; then
        print_status "SUCCESS" "ChromaDB Python package found"
        chromadb_installed=true
    fi
    
    if [[ $chromadb_installed == false ]]; then
        print_status "STEP" "Installing ChromaDB..."
        
        if command -v pip3 >/dev/null 2>&1; then
            if pip3 install chromadb --user; then
                print_status "SUCCESS" "ChromaDB installed successfully"
                chromadb_installed=true
                ((INSTALLED_DEPENDENCIES++))
            else
                print_status "ERROR" "Failed to install ChromaDB via pip3"
                ((FAILED_INSTALLATIONS++))
                return 1
            fi
        else
            print_status "ERROR" "pip3 not found, cannot install ChromaDB"
            print_status "INFO" "Please install pip3 first"
            ((FAILED_INSTALLATIONS++))
            return 1
        fi
    fi
    
    # Start ChromaDB server
    print_status "STEP" "Starting ChromaDB server..."
    
    # Check if server is already running
    if curl -s --connect-timeout 2 http://localhost:8000/api/v1/heartbeat >/dev/null 2>&1; then
        print_status "SUCCESS" "ChromaDB server is already running"
        ((INSTALLED_DEPENDENCIES++))
        return 0
    fi
    
    # Start server in background
    if command -v chroma >/dev/null 2>&1; then
        # Check if port is available
        if lsof -i :8000 >/dev/null 2>&1; then
            print_status "WARNING" "Port 8000 is already in use by another process"
            print_status "INFO" "ChromaDB server may not start properly"
        fi
        
        # Start ChromaDB server in background
        nohup chroma run --host localhost --port 8000 > logs/chromadb.log 2>&1 &
        local chromadb_pid=$!
        
        # Wait for server to start
        print_status "STEP" "Waiting for ChromaDB server to start..."
        local attempts=0
        local max_attempts=30
        
        while [[ $attempts -lt $max_attempts ]]; do
            if curl -s --connect-timeout 2 http://localhost:8000/api/v1/heartbeat >/dev/null 2>&1; then
                print_status "SUCCESS" "ChromaDB server started successfully (PID: $chromadb_pid)"
                ((INSTALLED_DEPENDENCIES++))
                return 0
            fi
            
            sleep 1
            ((attempts++))
            
            if [[ $((attempts % 5)) -eq 0 ]]; then
                print_status "INFO" "Still waiting for ChromaDB server... (attempt $attempts/$max_attempts)"
            fi
        done
        
        print_status "ERROR" "Failed to start ChromaDB server after $max_attempts attempts"
        print_status "INFO" "Check logs/chromadb.log for details"
        kill $chromadb_pid 2>/dev/null || true
        ((FAILED_INSTALLATIONS++))
        return 1
    else
        print_status "ERROR" "ChromaDB CLI not found, cannot start server"
        ((FAILED_INSTALLATIONS++))
        return 1
    fi
}

# Create necessary directories
create_directories() {
    print_status "STEP" "Creating necessary directories..."
    
    local directories=("bin" "logs" "models" "tmp")
    
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
# Agent-Orange Environment Variables
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
    chmod +x install.sh run_chatbot.sh agnt-orange 2>/dev/null || true
    print_status "SUCCESS" "Made scripts executable"
}

# Set up global CLI command
setup_global_cli() {
    print_status "STEP" "Setting up global CLI command..."
    
    # Check if agnt-orange wrapper script exists
    if [[ ! -f "agnt-orange" ]]; then
        print_status "ERROR" "agnt-orange wrapper script not found"
        print_status "INFO" "Please ensure the agnt-orange script is in the project root"
        return 1
    fi
    
    # Determine installation path based on user privileges
    local install_path=""
    local install_dir=""
    
    if [[ $IS_ROOT == true ]]; then
        # Root user - install to system-wide location
        install_path="/usr/local/bin/agnt-orange"
        install_dir="/usr/local/bin"
    else
        # Regular user - install to user's local bin
        install_path="$HOME/.local/bin/agnt-orange"
        install_dir="$HOME/.local/bin"
    fi
    
    # Create installation directory if it doesn't exist
    if [[ ! -d "$install_dir" ]]; then
        print_status "INFO" "Creating installation directory: $install_dir"
        mkdir -p "$install_dir"
    fi
    
    # Copy the wrapper script to the installation path
    if cp "agnt-orange" "$install_path"; then
        print_status "SUCCESS" "Copied agnt-orange to: $install_path"
    else
        print_status "ERROR" "Failed to copy agnt-orange to: $install_path"
        return 1
    fi
    
    # Set executable permissions
    if chmod +x "$install_path"; then
        print_status "SUCCESS" "Set executable permissions on: $install_path"
    else
        print_status "ERROR" "Failed to set executable permissions on: $install_path"
        return 1
    fi
    
    # Check if installation path is in PATH
    local path_in_path=false
    local current_path="$PATH"
    
    # Check if the installation directory is in PATH
    if [[ ":$current_path:" == *":$install_dir:"* ]]; then
        path_in_path=true
    fi
    
    if [[ $path_in_path == false ]]; then
        print_status "WARNING" "Installation path is not in your PATH"
        print_status "INFO" "To use 'agnt-orange' from any directory, add this to your shell profile:"
        if [[ $IS_ROOT == true ]]; then
            print_status "INFO" "  export PATH=\"/usr/local/bin:\$PATH\""
        else
            print_status "INFO" "  export PATH=\"\$HOME/.local/bin:\$PATH\""
        fi
        print_status "INFO" "Or restart your terminal session."
    else
        print_status "SUCCESS" "Installation path is in your PATH"
    fi
    
    # Test the installation
    if command -v agnt-orange >/dev/null 2>&1; then
        print_status "SUCCESS" "Global CLI command 'agnt-orange' is now available"
        print_status "INFO" "You can now use: agnt-orange <command> or agnt-orange from any directory"
    else
        print_status "WARNING" "Global CLI command may not be immediately available"
        print_status "INFO" "Try restarting your terminal or adding the installation path to your PATH"
    fi
}

# Print final success message
print_success_message() {
    echo
    print_status "SUCCESS" "🎉 Agent-Orange installation completed successfully!"
    echo
    print_status "INFO" "🚀 You can now start the chatbot using:"
    print_status "INFO" "   agnt-orange                    # Launch interactive mode"
    print_status "INFO" "   agnt-orange \"Hello, how are you?\"  # Send a message"
    print_status "INFO" "   agnt-orange --help             # Show help"
    echo
    print_status "INFO" "📚 Available commands:"
    print_status "INFO" "   • General chat and conversation"
    print_status "INFO" "   • Binary analysis with Ghidra"
    print_status "INFO" "   • Text embedding and vector search"
    print_status "INFO" "   • LLM-powered security analysis"
    echo
    print_status "INFO" "🔧 Configuration files:"
    print_status "INFO" "   • application.properties - Main configuration"
    print_status "INFO" "   • .env - Environment variables"
    print_status "INFO" "   • logs/ - Log files"
    echo
    print_status "INFO" "📖 For more information, see README.md and QUICKSTART.md"
    echo
    print_status "SUCCESS" "Happy pentesting! 🛡️"
    echo
}

# ============================================================================
# MAIN INSTALLATION PROCESS
# ============================================================================

main() {
    # Ensure logs directory exists before any logging operations
    # This prevents errors when ChromaDB or other components try to write logs
    # The -p flag ensures no error if the directory already exists
    mkdir -p logs
    
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
    
    # System commands (java, javac, gcc, g++, cmake, make, wget, curl)
    TOTAL_DEPENDENCIES=$((TOTAL_DEPENDENCIES + 8))
    check_system_commands
    
    # Java version check (after installation)
    if ! check_java_version; then
        print_status "ERROR" "Java 17+ is required but not available after installation."
        ((FAILED_INSTALLATIONS++))
    fi
    
    # Ghidra
    TOTAL_DEPENDENCIES=$((TOTAL_DEPENDENCIES + 1))
    install_ghidra
    
    # ChromaDB
    TOTAL_DEPENDENCIES=$((TOTAL_DEPENDENCIES + 1))
    install_chromadb
    
    # Create directories
    create_directories
    
    # Setup environment
    setup_environment
    
    # Setup global CLI command
    setup_global_cli
    
    # Print summary
    print_footer
    
    # Print success message
    print_success_message
    
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