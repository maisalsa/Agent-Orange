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
export JAVA_OPTS="$JAVA_OPTS -XX:+TieredCompilation"
export JAVA_OPTS="$JAVA_OPTS -XX:+UseCompressedOops"

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
