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
