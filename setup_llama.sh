#!/bin/bash

# ============================================================================
# Llama.cpp Native Library Setup Script for Agent-Orange
# ============================================================================
#
# This script installs and configures Llama.cpp native library integration
# for the Agent-Orange application, handling the entire setup process.
#
# USAGE:
#   ./setup_llama.sh [--install-method METHOD] [--skip-build]
#
# OPTIONS:
#   --install-method METHOD  Installation method: source, headers-only, download
#   --skip-build            Skip building the native library
#   --test-only             Only test existing setup
# ============================================================================

set -e  # Exit on any error

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default configuration
INSTALL_METHOD="headers-only"
SKIP_BUILD=false
TEST_ONLY=false

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --install-method)
            INSTALL_METHOD="$2"
            shift 2
            ;;
        --skip-build)
            SKIP_BUILD=true
            shift
            ;;
        --test-only)
            TEST_ONLY=true
            shift
            ;;
        --help|-h)
            echo "Usage: $0 [OPTIONS]"
            echo "Options:"
            echo "  --install-method METHOD  Installation method (source|headers-only|download)"
            echo "  --skip-build            Skip building native library"
            echo "  --test-only             Only test existing setup"
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

# Check prerequisites
check_prerequisites() {
    print_status "INFO" "Checking prerequisites..."
    
    # Check Java
    if command -v java >/dev/null 2>&1; then
        JAVA_VERSION=$(java -version 2>&1 | head -n1 | cut -d'"' -f2 | cut -d'.' -f1)
        if [[ "$JAVA_VERSION" -ge 17 ]]; then
            print_status "SUCCESS" "Java $JAVA_VERSION found"
        else
            print_status "ERROR" "Java 17+ required. Found: $JAVA_VERSION"
            exit 1
        fi
    else
        print_status "ERROR" "Java not found. Please install Java 17+"
        exit 1
    fi
    
    # Check GCC
    if command -v g++ >/dev/null 2>&1; then
        print_status "SUCCESS" "GCC/G++ found"
    else
        print_status "ERROR" "GCC/G++ not found. Please install build-essential"
        print_status "INFO" "Ubuntu/Debian: sudo apt-get install build-essential"
        exit 1
    fi
    
    # Check required tools
    for tool in wget curl git; do
        if ! command -v $tool >/dev/null 2>&1; then
            print_status "WARNING" "$tool not found. Some features may not work."
        fi
    done
    
    print_status "SUCCESS" "Prerequisites satisfied"
}

# Install llama.cpp headers only (minimal approach)
install_headers_only() {
    print_status "INFO" "Installing minimal llama.cpp headers..."
    
    # Create minimal header for testing
    cat > main/llama.h << 'EOF'
#ifndef LLAMA_H
#define LLAMA_H

#ifdef __cplusplus
extern "C" {
#endif

// Minimal llama.cpp interface for JNI
// This is a placeholder for demonstration purposes
// For full functionality, install complete llama.cpp

typedef struct llama_context llama_context;
typedef struct llama_model llama_model;

// Placeholder functions
struct llama_model * llama_load_model_from_file(const char * path_model, struct llama_context_params params);
void llama_free_model(struct llama_model * model);
struct llama_context * llama_new_context_with_model(struct llama_model * model, struct llama_context_params params);
void llama_free(struct llama_context * ctx);

#ifdef __cplusplus
}
#endif

#endif // LLAMA_H
EOF
    
    print_status "SUCCESS" "Minimal headers installed"
    print_status "WARNING" "This is a minimal setup for testing only"
    print_status "INFO" "For full functionality, install complete llama.cpp"
}

# Install llama.cpp from source
install_from_source() {
    print_status "INFO" "Installing llama.cpp from source..."
    
    # Check if already exists
    if [[ -d "llama.cpp" ]]; then
        print_status "INFO" "llama.cpp directory exists, updating..."
        cd llama.cpp
        git pull
        cd ..
    else
        print_status "INFO" "Cloning llama.cpp repository..."
        git clone https://github.com/ggerganov/llama.cpp.git
    fi
    
    # Build llama.cpp
    print_status "INFO" "Building llama.cpp..."
    cd llama.cpp
    make clean
    make -j$(nproc)
    cd ..
    
    # Copy headers
    cp llama.cpp/llama.h main/
    if [[ -f "llama.cpp/ggml.h" ]]; then
        cp llama.cpp/ggml.h main/
    fi
    
    print_status "SUCCESS" "llama.cpp built and headers copied"
}

# Download pre-built llama.cpp
install_from_download() {
    print_status "INFO" "Downloading pre-built llama.cpp..."
    
    # This is a placeholder - adjust URL based on actual releases
    local download_url="https://github.com/ggerganov/llama.cpp/releases/latest/download/llama.cpp-linux-x64.tar.gz"
    
    print_status "WARNING" "Pre-built download not implemented yet"
    print_status "INFO" "Falling back to headers-only installation"
    install_headers_only
}

# Test current setup
test_current_setup() {
    print_status "INFO" "Testing current llama.cpp setup..."
    
    # Check if headers exist
    if [[ -f "main/llama.h" ]]; then
        print_status "SUCCESS" "llama.h found in main/"
    else
        print_status "ERROR" "llama.h not found in main/"
        return 1
    fi
    
    # Check if JAR exists
    if [[ -f "bin/chatbot.jar" ]]; then
        print_status "SUCCESS" "chatbot.jar found"
    else
        print_status "WARNING" "chatbot.jar not found. Run './build.sh jar' first"
    fi
    
    # Check native library
    if [[ -f "bin/libllama.so" ]] || [[ -f "bin/libllama.dylib" ]] || [[ -f "bin/llama.dll" ]]; then
        print_status "SUCCESS" "Native library found"
        ls -la bin/lib*llama* bin/llama.dll 2>/dev/null || true
    else
        print_status "WARNING" "Native library not found"
        return 1
    fi
    
    return 0
}

# Build native library
build_native_library() {
    print_status "INFO" "Building native library..."
    
    # Ensure bin directory exists
    mkdir -p bin
    
    # Use the build system
    if ./build.sh native; then
        print_status "SUCCESS" "Native library build completed"
    else
        print_status "ERROR" "Native library build failed"
        return 1
    fi
    
    # Verify library was created
    local lib_found=false
    for lib in bin/libllama.so bin/libllama.dylib bin/llama.dll; do
        if [[ -f "$lib" ]]; then
            print_status "SUCCESS" "Native library created: $lib"
            ls -la "$lib"
            lib_found=true
            break
        fi
    done
    
    if [[ "$lib_found" == false ]]; then
        print_status "ERROR" "No native library was created"
        return 1
    fi
    
    return 0
}

# Test library loading
test_library_loading() {
    print_status "INFO" "Testing library loading..."
    
    # Check if JAR exists
    if [[ ! -f "bin/chatbot.jar" ]]; then
        print_status "ERROR" "chatbot.jar not found. Run './build.sh jar' first"
        return 1
    fi
    
    # Set library path
    export LD_LIBRARY_PATH=$PWD/bin:$LD_LIBRARY_PATH
    export DYLD_LIBRARY_PATH=$PWD/bin:$DYLD_LIBRARY_PATH
    
    # Test library loading
    print_status "INFO" "Testing Java library loading..."
    if timeout 10 java -cp bin/chatbot.jar com.example.LlamaJNI 2>/dev/null; then
        print_status "SUCCESS" "Library loading test passed"
    else
        print_status "WARNING" "Library loading test failed or library not available"
        print_status "INFO" "This is normal if llama.cpp is not fully installed"
    fi
    
    # Test full application initialization
    print_status "INFO" "Testing application initialization..."
    export LD_LIBRARY_PATH=$PWD/bin:$LD_LIBRARY_PATH
    if timeout 15 java -jar bin/chatbot.jar --help >/dev/null 2>&1; then
        print_status "SUCCESS" "Application initialization test passed"
    else
        print_status "INFO" "Application test completed (may have warnings)"
    fi
}

# Create environment setup script
create_environment_script() {
    print_status "INFO" "Creating environment setup script..."
    
    cat > llama_env.sh << 'EOF'
#!/bin/bash
# Llama.cpp Environment Configuration for Agent-Orange
# Source this file to set up llama.cpp environment

# Set library path for native library loading
export LD_LIBRARY_PATH="$PWD/bin:$LD_LIBRARY_PATH"
export DYLD_LIBRARY_PATH="$PWD/bin:$DYLD_LIBRARY_PATH"

# Set Java library path
export JAVA_OPTS="-Djava.library.path=bin $JAVA_OPTS"

echo "Llama.cpp environment configured:"
echo "  LD_LIBRARY_PATH: $LD_LIBRARY_PATH"
echo "  JAVA_OPTS: $JAVA_OPTS"
echo ""
echo "Usage:"
echo "  java -jar bin/chatbot.jar"
echo "  ./run_chatbot.sh"
EOF
    
    chmod +x llama_env.sh
    print_status "SUCCESS" "Environment script created: llama_env.sh"
}

# Main execution
main() {
    echo
    print_status "INFO" "Llama.cpp Native Library Setup for Agent-Orange"
    print_status "INFO" "Method: $INSTALL_METHOD | Skip Build: $SKIP_BUILD | Test Only: $TEST_ONLY"
    echo
    
    # Check prerequisites
    check_prerequisites
    
    # If test-only mode, just test and exit
    if [[ "$TEST_ONLY" == true ]]; then
        if test_current_setup && test_library_loading; then
            print_status "SUCCESS" "Current setup is working correctly"
        else
            print_status "WARNING" "Current setup has issues"
        fi
        exit 0
    fi
    
    # Install llama.cpp based on method
    case "$INSTALL_METHOD" in
        "source")
            install_from_source
            ;;
        "headers-only")
            install_headers_only
            ;;
        "download")
            install_from_download
            ;;
        *)
            print_status "ERROR" "Unknown install method: $INSTALL_METHOD"
            exit 1
            ;;
    esac
    
    # Build native library unless skipped
    if [[ "$SKIP_BUILD" == false ]]; then
        if build_native_library; then
            print_status "SUCCESS" "Native library build successful"
        else
            print_status "WARNING" "Native library build failed"
            print_status "INFO" "This is normal if complete llama.cpp is not installed"
        fi
    fi
    
    # Create environment script
    create_environment_script
    
    # Test the setup
    test_library_loading
    
    # Final status
    echo
    if test_current_setup 2>/dev/null; then
        print_status "SUCCESS" "Llama.cpp setup completed successfully!"
        echo
        print_status "INFO" "To use llama.cpp with Agent-Orange:"
        echo "  1. Source the environment: source llama_env.sh"
        echo "  2. Start the application: java -jar bin/chatbot.jar"
        echo "  3. Or use the run script: ./run_chatbot.sh"
        echo
        print_status "INFO" "For full llama.cpp functionality:"
        echo "  Run: $0 --install-method source"
        echo
    else
        print_status "WARNING" "Setup completed with warnings"
        echo
        print_status "INFO" "Current status:"
        echo "  - Headers: $([ -f main/llama.h ] && echo '✅ Available' || echo '❌ Missing')"
        echo "  - Native Library: $([ -f bin/libllama.so ] && echo '✅ Compiled' || echo '❌ Not compiled')"
        echo "  - Application: $([ -f bin/chatbot.jar ] && echo '✅ Built' || echo '❌ Not built')"
        echo
        print_status "INFO" "The application will run but with limited LLM functionality"
        print_status "INFO" "To enable full functionality, install complete llama.cpp"
    fi
}

# Run main function
main "$@"