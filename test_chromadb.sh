#!/bin/bash

# ============================================================================
# ChromaDB Test Script
# ============================================================================
#
# This script tests ChromaDB installation, server startup, and API functionality
# to verify that the installation script works correctly.
#
# USAGE:
#   ./test_chromadb.sh
# ============================================================================

set -e

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

# Test ChromaDB installation
test_chromadb_installation() {
    print_status "INFO" "Testing ChromaDB installation..."
    
    if command -v chroma >/dev/null 2>&1; then
        print_status "SUCCESS" "ChromaDB CLI found: $(command -v chroma)"
        return 0
    elif python3 -c "import chromadb" 2>/dev/null; then
        print_status "SUCCESS" "ChromaDB Python package found"
        return 0
    else
        print_status "ERROR" "ChromaDB not found"
        return 1
    fi
}

# Test ChromaDB server startup
test_chromadb_server() {
    print_status "INFO" "Testing ChromaDB server startup..."
    
    # Check if server is already running
    if curl -s --connect-timeout 2 http://localhost:8000/api/v1/heartbeat >/dev/null 2>&1; then
        print_status "SUCCESS" "ChromaDB server is already running"
        return 0
    fi
    
    # Try to start server
    if command -v chroma >/dev/null 2>&1; then
        print_status "INFO" "Starting ChromaDB server..."
        
        # Check if port is available
        if lsof -i :8000 >/dev/null 2>&1; then
            print_status "ERROR" "Port 8000 is already in use"
            return 1
        fi
        
        # Start server in background
        nohup chroma run --host localhost --port 8000 > /tmp/chromadb_test.log 2>&1 &
        local pid=$!
        
        # Wait for server to start
        local attempts=0
        while [[ $attempts -lt 30 ]]; do
            if curl -s --connect-timeout 2 http://localhost:8000/api/v1/heartbeat >/dev/null 2>&1; then
                print_status "SUCCESS" "ChromaDB server started successfully (PID: $pid)"
                return 0
            fi
            sleep 1
            ((attempts++))
        done
        
        # Cleanup if failed
        kill $pid 2>/dev/null || true
        print_status "ERROR" "Failed to start ChromaDB server"
        return 1
    else
        print_status "ERROR" "ChromaDB CLI not found"
        return 1
    fi
}

# Test API version detection
test_api_version() {
    print_status "INFO" "Testing API version detection..."
    
    local response=$(curl -s --connect-timeout 5 http://localhost:8000/api/v1/heartbeat 2>/dev/null)
    
    if [[ -n "$response" ]]; then
        local version=$(echo "$response" | grep -o '"version":"[^"]*"' | cut -d'"' -f4)
        
        if [[ -n "$version" ]]; then
            print_status "SUCCESS" "API version detected: $version"
            
            # Check if version is v2+
            local major_version=$(echo "$version" | cut -d'.' -f1 | sed 's/v//')
            if [[ "$major_version" -ge 2 ]]; then
                print_status "SUCCESS" "Version $version is compatible (v2+)"
                print_status "INFO" "Full API URL: http://localhost:8000/api/v1"
                return 0
            else
                print_status "ERROR" "Version $version is not supported (requires v2+)"
                return 1
            fi
        else
            print_status "WARNING" "Could not detect API version"
            return 0
        fi
    else
        print_status "ERROR" "Could not connect to API"
        return 1
    fi
}

# Test basic API functionality
test_api_functionality() {
    print_status "INFO" "Testing basic API functionality..."
    
    # Test heartbeat endpoint
    if curl -s http://localhost:8000/api/v1/heartbeat >/dev/null 2>&1; then
        print_status "SUCCESS" "Heartbeat endpoint working"
    else
        print_status "ERROR" "Heartbeat endpoint failed"
        return 1
    fi
    
    # Test collections endpoint
    if curl -s http://localhost:8000/api/v1/collections >/dev/null 2>&1; then
        print_status "SUCCESS" "Collections endpoint working"
    else
        print_status "ERROR" "Collections endpoint failed"
        return 1
    fi
    
    return 0
}

# Cleanup function
cleanup() {
    print_status "INFO" "Cleaning up..."
    
    # Stop ChromaDB server
    local pids=$(pgrep -f "chroma run" 2>/dev/null || true)
    if [[ -n "$pids" ]]; then
        for pid in $pids; do
            kill $pid 2>/dev/null || true
        done
        print_status "INFO" "Stopped ChromaDB server"
    fi
    
    # Remove test log
    rm -f /tmp/chromadb_test.log
}

# Main test function
main() {
    print_status "INFO" "Starting ChromaDB tests..."
    
    local tests_passed=0
    local tests_failed=0
    
    # Test 1: Installation
    if test_chromadb_installation; then
        ((tests_passed++))
    else
        ((tests_failed++))
    fi
    
    # Test 2: Server startup
    if test_chromadb_server; then
        ((tests_passed++))
    else
        ((tests_failed++))
    fi
    
    # Test 3: API version
    if test_api_version; then
        ((tests_passed++))
    else
        ((tests_failed++))
    fi
    
    # Test 4: API functionality
    if test_api_functionality; then
        ((tests_passed++))
    else
        ((tests_failed++))
    fi
    
    # Print results
    echo
    print_status "INFO" "Test Results:"
    print_status "INFO" "  Passed: $tests_passed"
    print_status "INFO" "  Failed: $tests_failed"
    
    if [[ $tests_failed -eq 0 ]]; then
        print_status "SUCCESS" "All ChromaDB tests passed!"
        exit 0
    else
        print_status "ERROR" "Some tests failed"
        exit 1
    fi
}

# Set up cleanup on exit
trap cleanup EXIT

# Run main function
main "$@" 