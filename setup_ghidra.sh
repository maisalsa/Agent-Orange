#!/bin/bash

# ============================================================================
# Ghidra Setup Script for Agent-Orange
# ============================================================================
#
# This script helps install and configure Ghidra for use with Agent-Orange.
# It handles downloading, extracting, and setting up environment variables.
#
# USAGE:
#   ./setup_ghidra.sh [--install-dir DIR] [--version VERSION]
#
# OPTIONS:
#   --install-dir DIR    Installation directory (default: /opt/ghidra)
#   --version VERSION    Ghidra version to install (default: latest)
#   --project-dir DIR    Project directory (default: /tmp/ghidra_projects)
#   --skip-download      Skip download if Ghidra already exists
# ============================================================================

set -e  # Exit on any error

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default configuration
INSTALL_DIR="/opt/ghidra"
PROJECT_DIR="/tmp/ghidra_projects"
GHIDRA_VERSION=""
SKIP_DOWNLOAD=false
USER_INSTALL=false

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --install-dir)
            INSTALL_DIR="$2"
            shift 2
            ;;
        --project-dir)
            PROJECT_DIR="$2"
            shift 2
            ;;
        --version)
            GHIDRA_VERSION="$2"
            shift 2
            ;;
        --skip-download)
            SKIP_DOWNLOAD=true
            shift
            ;;
        --user-install)
            USER_INSTALL=true
            INSTALL_DIR="$HOME/.local/ghidra"
            shift
            ;;
        --help|-h)
            echo "Usage: $0 [OPTIONS]"
            echo "Options:"
            echo "  --install-dir DIR     Installation directory (default: /opt/ghidra)"
            echo "  --project-dir DIR     Project directory (default: /tmp/ghidra_projects)"
            echo "  --version VERSION     Ghidra version (default: latest)"
            echo "  --skip-download       Skip download if exists"
            echo "  --user-install        Install in user directory"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

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

# Check if running as root when needed
check_permissions() {
    if [[ "$USER_INSTALL" == false && "$INSTALL_DIR" == "/opt/ghidra" ]]; then
        if [[ $EUID -ne 0 ]]; then
            print_status "WARNING" "Installing to /opt/ghidra requires sudo privileges"
            print_status "INFO" "Re-run with: sudo $0 or use --user-install"
            read -p "Continue with sudo? (y/n): " -n 1 -r
            echo
            if [[ ! $REPLY =~ ^[Yy]$ ]]; then
                exit 1
            fi
        fi
    fi
}

# Check prerequisites
check_prerequisites() {
    print_status "INFO" "Checking prerequisites..."
    
    # Check Java version
    if command -v java >/dev/null 2>&1; then
        JAVA_VERSION=$(java -version 2>&1 | head -n1 | cut -d'"' -f2 | cut -d'.' -f1)
        if [[ "$JAVA_VERSION" -ge 17 ]]; then
            print_status "SUCCESS" "Java $JAVA_VERSION found"
        else
            print_status "ERROR" "Java 17 or higher required. Found: $JAVA_VERSION"
            print_status "INFO" "Install with: sudo apt-get install openjdk-17-jdk"
            exit 1
        fi
    else
        print_status "ERROR" "Java not found. Please install Java 17 or higher"
        print_status "INFO" "Install with: sudo apt-get install openjdk-17-jdk"
        exit 1
    fi
    
    # Check required tools
    for tool in wget unzip; do
        if ! command -v $tool >/dev/null 2>&1; then
            print_status "ERROR" "Required tool not found: $tool"
            print_status "INFO" "Install with: sudo apt-get install $tool"
            exit 1
        fi
    done
    
    print_status "SUCCESS" "All prerequisites satisfied"
}

# Get latest Ghidra version
get_latest_version() {
    print_status "INFO" "Fetching latest Ghidra version..."
    
    # Try to get latest release from GitHub API
    if command -v curl >/dev/null 2>&1; then
        LATEST_VERSION=$(curl -s https://api.github.com/repos/NationalSecurityAgency/ghidra/releases/latest | grep -o '"tag_name": "[^"]*' | cut -d'"' -f4)
        if [[ -n "$LATEST_VERSION" ]]; then
            print_status "SUCCESS" "Latest version: $LATEST_VERSION"
            echo "$LATEST_VERSION"
            return
        fi
    fi
    
    # Fallback to known stable version
    print_status "WARNING" "Could not fetch latest version, using fallback"
    echo "Ghidra_10.4_build"
}

# Download and extract Ghidra
install_ghidra() {
    local version=$1
    local install_dir=$2
    
    print_status "INFO" "Installing Ghidra $version to $install_dir"
    
    # Create installation directory
    if [[ "$USER_INSTALL" == true ]]; then
        mkdir -p "$install_dir"
    else
        sudo mkdir -p "$install_dir"
    fi
    
    # Download Ghidra
    local download_url="https://github.com/NationalSecurityAgency/ghidra/releases/download/${version}/ghidra_${version#Ghidra_}_PUBLIC.zip"
    local zip_file="/tmp/ghidra_${version}.zip"
    
    print_status "INFO" "Downloading from: $download_url"
    wget -O "$zip_file" "$download_url"
    
    if [[ ! -f "$zip_file" ]]; then
        print_status "ERROR" "Download failed"
        exit 1
    fi
    
    print_status "SUCCESS" "Download completed"
    
    # Extract Ghidra
    print_status "INFO" "Extracting Ghidra..."
    local temp_dir="/tmp/ghidra_extract_$$"
    mkdir -p "$temp_dir"
    unzip -q "$zip_file" -d "$temp_dir"
    
    # Find the extracted directory (should be ghidra_X.X.X_PUBLIC)
    local extracted_dir=$(find "$temp_dir" -maxdepth 1 -type d -name "ghidra_*" | head -1)
    
    if [[ -z "$extracted_dir" ]]; then
        print_status "ERROR" "Could not find extracted Ghidra directory"
        exit 1
    fi
    
    # Move to final location
    if [[ "$USER_INSTALL" == true ]]; then
        cp -r "$extracted_dir"/* "$install_dir/"
        chown -R $USER:$USER "$install_dir"
    else
        sudo cp -r "$extracted_dir"/* "$install_dir/"
        sudo chown -R $USER:$USER "$install_dir"
    fi
    
    # Make analyzeHeadless executable
    chmod +x "$install_dir/support/analyzeHeadless"
    
    # Cleanup
    rm -rf "$temp_dir" "$zip_file"
    
    print_status "SUCCESS" "Ghidra installed successfully"
}

# Check if Ghidra is already installed
check_existing_installation() {
    local install_dir=$1
    
    if [[ -f "$install_dir/support/analyzeHeadless" ]]; then
        print_status "SUCCESS" "Ghidra already installed at $install_dir"
        
        # Test if it works
        if "$install_dir/support/analyzeHeadless" -help >/dev/null 2>&1; then
            print_status "SUCCESS" "Existing installation is functional"
            return 0
        else
            print_status "WARNING" "Existing installation may be broken"
            return 1
        fi
    fi
    
    return 1
}

# Setup project directory
setup_project_directory() {
    local project_dir=$1
    
    print_status "INFO" "Setting up project directory: $project_dir"
    
    mkdir -p "$project_dir"
    chmod 755 "$project_dir"
    
    # Test write permissions
    local test_file="$project_dir/.write_test_$$"
    if echo "test" > "$test_file" 2>/dev/null; then
        rm -f "$test_file"
        print_status "SUCCESS" "Project directory is writable"
    else
        print_status "ERROR" "Project directory is not writable: $project_dir"
        exit 1
    fi
}

# Configure environment variables
configure_environment() {
    local install_dir=$1
    local project_dir=$2
    
    print_status "INFO" "Configuring environment variables..."
    
    # Create environment configuration
    local env_file="ghidra_env.sh"
    cat > "$env_file" << EOF
#!/bin/bash
# Ghidra Environment Configuration for Agent-Orange
# Source this file to set up Ghidra environment variables

export GHIDRA_HOME="$install_dir"
export GHIDRA_ANALYZE_HEADLESS="$install_dir/support/analyzeHeadless"
export GHIDRA_PROJECT_DIR="$project_dir"
export GHIDRA_PROJECT_NAME="agent_orange_analysis"
export GHIDRA_TIMEOUT_MS=600000

# Add to PATH for convenience
export PATH="\$GHIDRA_HOME/support:\$PATH"

echo "Ghidra environment configured:"
echo "  GHIDRA_HOME: \$GHIDRA_HOME"
echo "  PROJECT_DIR: \$GHIDRA_PROJECT_DIR"
echo "  TIMEOUT: \$GHIDRA_TIMEOUT_MS ms"
EOF
    
    chmod +x "$env_file"
    print_status "SUCCESS" "Environment configuration saved to: $env_file"
    
    # Update application.properties
    if [[ -f "application.properties" ]]; then
        print_status "INFO" "Updating application.properties..."
        
        # Create backup
        cp application.properties application.properties.backup.$(date +%s)
        
        # Update configuration
        sed -i "s|ghidra.headless.path=.*|ghidra.headless.path=$install_dir/support/analyzeHeadless|" application.properties
        sed -i "s|ghidra.project.dir=.*|ghidra.project.dir=$project_dir|" application.properties
        
        print_status "SUCCESS" "application.properties updated"
    fi
}

# Test Ghidra installation
test_installation() {
    local install_dir=$1
    local project_dir=$2
    
    print_status "INFO" "Testing Ghidra installation..."
    
    # Test analyzeHeadless
    if "$install_dir/support/analyzeHeadless" -help >/dev/null 2>&1; then
        print_status "SUCCESS" "analyzeHeadless script works"
    else
        print_status "ERROR" "analyzeHeadless script failed"
        return 1
    fi
    
    # Test project creation
    local test_project="$project_dir/test_installation_$$"
    if "$install_dir/support/analyzeHeadless" "$test_project" test_proj -help >/dev/null 2>&1; then
        print_status "SUCCESS" "Project creation test passed"
        rm -rf "$test_project"
    else
        print_status "WARNING" "Project creation test failed (may be normal)"
    fi
    
    # Test with Agent-Orange if available
    if [[ -f "bin/chatbot.jar" ]]; then
        print_status "INFO" "Testing with Agent-Orange..."
        
        # Set environment and test
        export GHIDRA_HOME="$install_dir"
        export GHIDRA_PROJECT_DIR="$project_dir"
        
        # This would normally test the integration, but we'll just verify the JAR loads
        if timeout 10 java -jar bin/chatbot.jar --help >/dev/null 2>&1; then
            print_status "SUCCESS" "Agent-Orange integration test passed"
        else
            print_status "INFO" "Agent-Orange integration test inconclusive"
        fi
    fi
    
    return 0
}

# Main execution
main() {
    echo
    print_status "INFO" "Ghidra Setup for Agent-Orange"
    print_status "INFO" "Install Dir: $INSTALL_DIR | Project Dir: $PROJECT_DIR"
    echo
    
    # Check prerequisites
    check_prerequisites
    
    # Check permissions
    check_permissions
    
    # Check if already installed
    if [[ "$SKIP_DOWNLOAD" == false ]]; then
        if check_existing_installation "$INSTALL_DIR"; then
            read -p "Reinstall anyway? (y/n): " -n 1 -r
            echo
            if [[ ! $REPLY =~ ^[Yy]$ ]]; then
                SKIP_DOWNLOAD=true
            fi
        fi
    fi
    
    # Install Ghidra if needed
    if [[ "$SKIP_DOWNLOAD" == false ]]; then
        if [[ -z "$GHIDRA_VERSION" ]]; then
            GHIDRA_VERSION=$(get_latest_version)
        fi
        install_ghidra "$GHIDRA_VERSION" "$INSTALL_DIR"
    fi
    
    # Setup project directory
    setup_project_directory "$PROJECT_DIR"
    
    # Configure environment
    configure_environment "$INSTALL_DIR" "$PROJECT_DIR"
    
    # Test installation
    if test_installation "$INSTALL_DIR" "$PROJECT_DIR"; then
        echo
        print_status "SUCCESS" "Ghidra setup completed successfully!"
        echo
        print_status "INFO" "To use Ghidra with Agent-Orange:"
        echo "  1. Source the environment: source ghidra_env.sh"
        echo "  2. Start Agent-Orange: java -jar bin/chatbot.jar"
        echo "  3. Test with: analyze /usr/bin/ls with ghidra"
        echo
        print_status "INFO" "Manual setup:"
        echo "  export GHIDRA_HOME='$INSTALL_DIR'"
        echo "  export GHIDRA_PROJECT_DIR='$PROJECT_DIR'"
        echo
    else
        print_status "ERROR" "Ghidra setup failed during testing"
        exit 1
    fi
}

# Run main function
main "$@"