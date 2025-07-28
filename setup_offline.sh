#!/bin/bash

# ============================================================================
# Agent-Orange Offline Setup for ThinkStation Tiny P320
# ============================================================================
#
# This script prepares Agent-Orange for completely offline operation on the
# Lenovo ThinkStation Tiny P320 with Intel i7-7700T processor.
#
# Run this script ONCE while online to download and configure all necessary
# components for offline operation.
#
# USAGE:
#   ./setup_offline.sh [--download-models] [--verify-only]
#
# OPTIONS:
#   --download-models    Download embedding and LLM models (requires ~2GB)
#   --verify-only        Only verify offline readiness, don't download
#   --help              Show this help message
# ============================================================================

set -e

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m'

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
MODELS_DIR="$SCRIPT_DIR/models"
DATA_DIR="$SCRIPT_DIR/data"
CACHE_DIR="$SCRIPT_DIR/cache"
LOGS_DIR="$SCRIPT_DIR/logs"
TMP_DIR="$SCRIPT_DIR/tmp"

DOWNLOAD_MODELS=false
VERIFY_ONLY=false

# Print status with colors
print_status() {
    local status=$1
    local message=$2
    case $status in
        "INFO") echo -e "${BLUE}ℹ️  INFO${NC} $message" ;;
        "SUCCESS") echo -e "${GREEN}✅ SUCCESS${NC} $message" ;;
        "WARNING") echo -e "${YELLOW}⚠️  WARNING${NC} $message" ;;
        "ERROR") echo -e "${RED}❌ ERROR${NC} $message" ;;
        "STEP") echo -e "${PURPLE}🔧 STEP${NC} $message" ;;
    esac
}

# Parse command line arguments
parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            --download-models)
                DOWNLOAD_MODELS=true
                shift
                ;;
            --verify-only)
                VERIFY_ONLY=true
                shift
                ;;
            --help|-h)
                cat << EOF
Agent-Orange Offline Setup for ThinkStation Tiny P320

USAGE:
    $0 [OPTIONS]

OPTIONS:
    --download-models    Download embedding and LLM models (~2GB)
    --verify-only        Only verify offline readiness
    --help              Show this help message

DESCRIPTION:
    Prepares Agent-Orange for completely offline operation on the
    Lenovo ThinkStation Tiny P320 with Intel i7-7700T processor.

HARDWARE REQUIREMENTS:
    - Intel i7-7700T (4 cores, 8 threads)
    - 16-32 GB RAM
    - 50+ GB SSD storage
    - No network required after setup

EOF
                exit 0
                ;;
            *)
                print_status "ERROR" "Unknown option: $1"
                exit 1
                ;;
        esac
    done
}

# Check system requirements
check_system_requirements() {
    print_status "STEP" "Checking system requirements for ThinkStation P320..."

    # Check CPU
    local cpu_model=$(grep "model name" /proc/cpuinfo | head -1 | cut -d: -f2 | xargs)
    print_status "INFO" "CPU: $cpu_model"
    
    if [[ "$cpu_model" == *"i7-7700T"* ]]; then
        print_status "SUCCESS" "Intel i7-7700T detected (optimal)"
    elif [[ "$cpu_model" == *"i7"* ]]; then
        print_status "WARNING" "Different i7 processor detected, should work but not optimal"
    else
        print_status "WARNING" "Non-i7 processor detected, performance may vary"
    fi

    # Check memory
    local total_mem=$(free -g | grep "Mem:" | awk '{print $2}')
    print_status "INFO" "Total RAM: ${total_mem}GB"
    
    if [[ $total_mem -ge 16 ]]; then
        print_status "SUCCESS" "Sufficient RAM for offline operation"
    else
        print_status "WARNING" "Less than 16GB RAM, may affect performance"
    fi

    # Check disk space
    local available_space=$(df -BG "$SCRIPT_DIR" | tail -1 | awk '{print $4}' | tr -d 'G')
    print_status "INFO" "Available disk space: ${available_space}GB"
    
    if [[ $available_space -ge 50 ]]; then
        print_status "SUCCESS" "Sufficient disk space for offline operation"
    else
        print_status "ERROR" "Insufficient disk space. Need at least 50GB free."
        exit 1
    fi

    # Check Java
    if command -v java >/dev/null 2>&1; then
        local java_version=$(java -version 2>&1 | head -n1 | cut -d'"' -f2)
        print_status "SUCCESS" "Java $java_version found"
    else
        print_status "ERROR" "Java not found. Please install Java 17 or later."
        exit 1
    fi
}

# Create directory structure
create_directory_structure() {
    print_status "STEP" "Creating offline directory structure..."

    local dirs=(
        "$MODELS_DIR/sentence-transformers-all-MiniLM-L6-v2"
        "$MODELS_DIR/llama"
        "$DATA_DIR/chromadb/persist"
        "$DATA_DIR/chromadb/index"
        "$DATA_DIR/ghidra/projects"
        "$CACHE_DIR/embeddings"
        "$CACHE_DIR/llama"
        "$LOGS_DIR"
        "$TMP_DIR"
        "bin/native-libs"
    )

    for dir in "${dirs[@]}"; do
        mkdir -p "$dir"
        print_status "INFO" "Created: $dir"
    done

    print_status "SUCCESS" "Directory structure created"
}

# Download embedding model for offline use
download_embedding_model() {
    if [[ "$VERIFY_ONLY" == true ]]; then
        return 0
    fi

    print_status "STEP" "Downloading lightweight embedding model for offline use..."

    local model_dir="$MODELS_DIR/sentence-transformers-all-MiniLM-L6-v2"
    
    if [[ ! -f "$model_dir/pytorch_model.bin" ]]; then
        print_status "INFO" "Downloading sentence-transformers/all-MiniLM-L6-v2 model..."
        
        # Create a simple Python script to download the model
        cat > "$TMP_DIR/download_model.py" << 'EOF'
import os
import sys

try:
    from sentence_transformers import SentenceTransformer
    
    model_path = sys.argv[1]
    print(f"Downloading model to: {model_path}")
    
    # Download and save the model
    model = SentenceTransformer('all-MiniLM-L6-v2')
    model.save(model_path)
    
    print("Model downloaded successfully")
    print(f"Model dimensions: {model.get_sentence_embedding_dimension()}")
    
except ImportError:
    print("ERROR: sentence-transformers not installed")
    print("Install with: pip install sentence-transformers")
    sys.exit(1)
except Exception as e:
    print(f"ERROR: {e}")
    sys.exit(1)
EOF

        if python3 "$TMP_DIR/download_model.py" "$model_dir"; then
            print_status "SUCCESS" "Embedding model downloaded"
        else
            print_status "WARNING" "Failed to download embedding model"
            print_status "INFO" "You can download it manually later"
        fi
    else
        print_status "SUCCESS" "Embedding model already exists"
    fi
}

# Setup ChromaDB for offline operation
setup_chromadb_offline() {
    print_status "STEP" "Configuring ChromaDB for offline operation..."

    # Create ChromaDB configuration
    cat > "$DATA_DIR/chromadb/config.yaml" << EOF
# ChromaDB Offline Configuration for ThinkStation P320
persist_directory: "$DATA_DIR/chromadb/persist"
chroma_db_impl: "duckdb+parquet"
anonymized_telemetry: false
allow_reset: true

# Memory optimizations for i7-7700T
settings:
  chroma_memory_limit_bytes: 2147483648  # 2GB
  chroma_segment_cache_policy: "LRU"
  chroma_collection_cache_size: 100
  
# Disable network features
chroma_server_host: null
chroma_server_http_port: null
chroma_server_ssl_enabled: false
chroma_server_grpc_port: null
EOF

    # Create embedded ChromaDB initialization script
    cat > "$DATA_DIR/chromadb/init_embedded.py" << 'EOF'
#!/usr/bin/env python3
"""
Initialize embedded ChromaDB for offline operation
"""
import chromadb
from chromadb.config import Settings
import os

def initialize_chromadb():
    """Initialize ChromaDB in embedded mode"""
    persist_dir = os.path.dirname(os.path.abspath(__file__)) + "/persist"
    
    client = chromadb.Client(Settings(
        chroma_db_impl="duckdb+parquet",
        persist_directory=persist_dir,
        anonymized_telemetry=False
    ))
    
    # Create a test collection to verify functionality
    try:
        collection = client.create_collection(
            name="test_offline_collection",
            metadata={"description": "Test collection for offline verification"}
        )
        
        # Add a test document
        collection.add(
            documents=["This is a test document for offline ChromaDB"],
            metadatas=[{"source": "offline_test"}],
            ids=["test_doc_1"]
        )
        
        # Query to verify
        results = collection.query(
            query_texts=["test document"],
            n_results=1
        )
        
        print("✅ ChromaDB offline initialization successful")
        print(f"✅ Test collection created with {len(results['documents'][0])} documents")
        
        return True
        
    except Exception as e:
        print(f"❌ ChromaDB initialization failed: {e}")
        return False

if __name__ == "__main__":
    initialize_chromadb()
EOF

    chmod +x "$DATA_DIR/chromadb/init_embedded.py"
    print_status "SUCCESS" "ChromaDB offline configuration created"
}

# Setup Ghidra for offline operation
setup_ghidra_offline() {
    print_status "STEP" "Configuring Ghidra for offline operation..."

    local ghidra_config_dir="$DATA_DIR/ghidra/config"
    mkdir -p "$ghidra_config_dir"

    # Create Ghidra offline configuration
    cat > "$ghidra_config_dir/preferences" << EOF
# Ghidra Offline Configuration for ThinkStation P320
# Disable all network features

# Update and repository settings
UPDATES_CHECK_FOR_UPDATES=false
UPDATES_DOWNLOAD_UPDATES=false
UPDATES_REPOSITORY_URLS=

# Analysis settings optimized for i7-7700T
ANALYSIS_PARALLEL_THREAD_COUNT=4
ANALYSIS_MAX_MEMORY_GB=3
ANALYSIS_TIMEOUT_MINUTES=5

# Disable external tools and network dependencies
EXTERNAL_TOOLS_ENABLED=false
COLLABORATION_ENABLED=false
NETWORK_DISCOVERY_ENABLED=false
SYMBOL_SERVER_ENABLED=false

# Local script directories
SCRIPT_DIRECTORIES=$SCRIPT_DIR/main:$SCRIPT_DIR/ghidra-scripts
EOF

    # Create offline Ghidra launcher script
    cat > "$DATA_DIR/ghidra/launch_offline.sh" << 'EOF'
#!/bin/bash
# Ghidra Offline Launcher for ThinkStation P320

export GHIDRA_HOME="/opt/ghidra"
export GHIDRA_PROJECT_DIR="$DATA_DIR/ghidra/projects"

# Disable network access
export JAVA_OPTS="$JAVA_OPTS -Djava.net.useSystemProxies=false"
export JAVA_OPTS="$JAVA_OPTS -Dhttp.proxyHost="
export JAVA_OPTS="$JAVA_OPTS -Dhttps.proxyHost="

# Memory optimization for i7-7700T
export GHIDRA_MAX_MEM="3G"
export GHIDRA_INITIAL_MEM="512M"

# Use offline configuration
export GHIDRA_USER_SETTINGS_DIR="$DATA_DIR/ghidra/config"

exec "$GHIDRA_HOME/support/analyzeHeadless" "$@"
EOF

    chmod +x "$DATA_DIR/ghidra/launch_offline.sh"
    print_status "SUCCESS" "Ghidra offline configuration created"
}

# Optimize runtime startup script
create_offline_launcher() {
    print_status "STEP" "Creating optimized offline launcher..."

    cat > "run_offline.sh" << 'EOF'
#!/bin/bash

# ============================================================================
# Agent-Orange Offline Launcher for ThinkStation Tiny P320
# ============================================================================
#
# Optimized startup script for completely offline operation on the
# Lenovo ThinkStation Tiny P320 with Intel i7-7700T processor.
# ============================================================================

set -e

# Detect script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

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
        "INFO") echo -e "${BLUE}ℹ️${NC} $message" ;;
        "SUCCESS") echo -e "${GREEN}✅${NC} $message" ;;
        "WARNING") echo -e "${YELLOW}⚠️${NC} $message" ;;
        "ERROR") echo -e "${RED}❌${NC} $message" ;;
    esac
}

print_status "INFO" "Starting Agent-Orange in OFFLINE mode..."
print_status "INFO" "Optimized for ThinkStation Tiny P320 (i7-7700T)"

# Verify offline mode requirements
if [[ ! -f "offline-config.properties" ]]; then
    print_status "ERROR" "offline-config.properties not found"
    print_status "INFO" "Run ./setup_offline.sh first"
    exit 1
fi

if [[ ! -f "bin/chatbot.jar" ]]; then
    print_status "ERROR" "Application JAR not found"
    print_status "INFO" "Run ./build.sh first"
    exit 1
fi

# Create required directories
mkdir -p data/chromadb data/ghidra/projects logs tmp cache

# Set up environment for offline operation
export JAVA_OPTS="-server"
export JAVA_OPTS="$JAVA_OPTS -Xmx6g -Xms1g"  # Optimized for 16-32GB RAM
export JAVA_OPTS="$JAVA_OPTS -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
export JAVA_OPTS="$JAVA_OPTS -XX:+UseStringDeduplication"
export JAVA_OPTS="$JAVA_OPTS -XX:G1HeapRegionSize=16m"
export JAVA_OPTS="$JAVA_OPTS -XX:ConcGCThreads=2"  # Use 2 threads for GC
export JAVA_OPTS="$JAVA_OPTS -XX:ParallelGCThreads=4"  # Use all 4 cores

# Offline-specific JVM options
export JAVA_OPTS="$JAVA_OPTS -Djava.net.useSystemProxies=false"
export JAVA_OPTS="$JAVA_OPTS -Dhttp.proxyHost="
export JAVA_OPTS="$JAVA_OPTS -Dhttps.proxyHost="
export JAVA_OPTS="$JAVA_OPTS -Djava.awt.headless=true"

# Native library path for embedded libraries
export JAVA_OPTS="$JAVA_OPTS -Djava.library.path=bin:bin/native-libs"

# Use offline configuration
export JAVA_OPTS="$JAVA_OPTS -Dconfig.file=offline-config.properties"
export JAVA_OPTS="$JAVA_OPTS -Doffline.mode=true"

# Performance tuning for i7-7700T
export JAVA_OPTS="$JAVA_OPTS -XX:+UseFastAccessorMethods"
export JAVA_OPTS="$JAVA_OPTS -XX:+OptimizeStringConcat"
export JAVA_OPTS="$JAVA_OPTS -XX:+TieredCompilation"

# Native library paths for macOS/Linux
if [[ "$OSTYPE" == "darwin"* ]]; then
    export DYLD_LIBRARY_PATH="$PWD/bin:$PWD/bin/native-libs:$DYLD_LIBRARY_PATH"
else
    export LD_LIBRARY_PATH="$PWD/bin:$PWD/bin/native-libs:$LD_LIBRARY_PATH"
fi

print_status "INFO" "JVM Options: $JAVA_OPTS"
print_status "INFO" "Library Path: $LD_LIBRARY_PATH"

# Verify network isolation (optional)
if command -v netstat >/dev/null 2>&1; then
    CONNECTIONS=$(netstat -tn 2>/dev/null | grep ESTABLISHED | wc -l || echo 0)
    if [[ $CONNECTIONS -gt 0 ]]; then
        print_status "WARNING" "Active network connections detected"
        print_status "INFO" "Agent-Orange will run in offline mode anyway"
    else
        print_status "SUCCESS" "No active network connections (ideal for offline)"
    fi
fi

# Start the application
print_status "SUCCESS" "Launching Agent-Orange in offline mode..."
java $JAVA_OPTS -jar bin/chatbot.jar "$@"
EOF

    chmod +x "run_offline.sh"
    print_status "SUCCESS" "Offline launcher created"
}

# Create verification script
create_verification_script() {
    print_status "STEP" "Creating offline verification script..."

    cat > "verify_offline.sh" << 'EOF'
#!/bin/bash

# ============================================================================
# Agent-Orange Offline Verification for ThinkStation Tiny P320
# ============================================================================

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_status() {
    local status=$1
    local message=$2
    case $status in
        "INFO") echo -e "${BLUE}ℹ️${NC} $message" ;;
        "SUCCESS") echo -e "${GREEN}✅${NC} $message" ;;
        "WARNING") echo -e "${YELLOW}⚠️${NC} $message" ;;
        "ERROR") echo -e "${RED}❌${NC} $message" ;;
    esac
}

echo "🔍 Agent-Orange Offline Verification"
echo "===================================="
echo

# Check if running offline
if ping -c 1 8.8.8.8 >/dev/null 2>&1; then
    print_status "WARNING" "Network connection detected"
    print_status "INFO" "For true offline verification, disconnect network"
else
    print_status "SUCCESS" "Network offline - perfect for testing"
fi

# Check required files
print_status "INFO" "Checking required files..."

required_files=(
    "bin/chatbot.jar"
    "offline-config.properties"
    "run_offline.sh"
)

for file in "${required_files[@]}"; do
    if [[ -f "$file" ]]; then
        print_status "SUCCESS" "$file exists"
    else
        print_status "ERROR" "$file missing"
    fi
done

# Check directories
print_status "INFO" "Checking directory structure..."

required_dirs=(
    "data/chromadb"
    "data/ghidra/projects"
    "models"
    "cache"
    "logs"
    "tmp"
)

for dir in "${required_dirs[@]}"; do
    if [[ -d "$dir" ]]; then
        print_status "SUCCESS" "$dir exists"
    else
        print_status "ERROR" "$dir missing"
    fi
done

# Check Java
if command -v java >/dev/null 2>&1; then
    java_version=$(java -version 2>&1 | head -n1)
    print_status "SUCCESS" "Java found: $java_version"
else
    print_status "ERROR" "Java not found"
fi

# Quick application test
print_status "INFO" "Testing application startup..."
if timeout 30 java -Dconfig.file=offline-config.properties -jar bin/chatbot.jar --help >/dev/null 2>&1; then
    print_status "SUCCESS" "Application starts successfully"
else
    print_status "WARNING" "Application startup issues detected"
fi

echo
print_status "INFO" "Offline verification complete"
print_status "INFO" "Run ./run_offline.sh to start Agent-Orange in offline mode"
EOF

    chmod +x "verify_offline.sh"
    print_status "SUCCESS" "Verification script created"
}

# Main execution
main() {
    echo "🔒 Agent-Orange Offline Setup for ThinkStation Tiny P320"
    echo "========================================================"
    echo
    
    parse_args "$@"
    
    if [[ "$VERIFY_ONLY" == true ]]; then
        print_status "INFO" "Running verification only..."
        ./verify_offline.sh 2>/dev/null || create_verification_script && ./verify_offline.sh
        exit 0
    fi
    
    check_system_requirements
    create_directory_structure
    
    if [[ "$DOWNLOAD_MODELS" == true ]]; then
        download_embedding_model
    else
        print_status "INFO" "Skipping model download (use --download-models to download)"
    fi
    
    setup_chromadb_offline
    setup_ghidra_offline
    create_offline_launcher
    create_verification_script
    
    echo
    print_status "SUCCESS" "Agent-Orange offline setup complete!"
    echo
    print_status "INFO" "Next steps:"
    echo "  1. Build the application: ./build.sh"
    echo "  2. Verify offline mode: ./verify_offline.sh"
    echo "  3. Run offline: ./run_offline.sh"
    echo
    print_status "INFO" "For complete offline operation:"
    echo "  - Disconnect network"
    echo "  - Use ./run_offline.sh instead of ./run_chatbot.sh"
    echo "  - All data will be stored locally"
}

main "$@"