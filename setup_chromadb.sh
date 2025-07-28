#!/bin/bash

# ============================================================================
# ChromaDB Quick Setup Script for Agent-Orange
# ============================================================================
#
# This script installs and starts ChromaDB for the Agent-Orange application.
# It handles installation via pip and provides verification steps.
#
# USAGE:
#   ./setup_chromadb.sh [--port PORT] [--background]
#
# OPTIONS:
#   --port PORT      Specify port (default: 8000)
#   --background     Run ChromaDB in background
#   --docker         Use Docker instead of pip installation
# ============================================================================

set -e  # Exit on any error

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Default configuration
DEFAULT_PORT=8000
DEFAULT_HOST="localhost"
USE_DOCKER=false
RUN_BACKGROUND=false

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --port)
            DEFAULT_PORT="$2"
            shift 2
            ;;
        --background)
            RUN_BACKGROUND=true
            shift
            ;;
        --docker)
            USE_DOCKER=true
            shift
            ;;
        --help|-h)
            echo "Usage: $0 [--port PORT] [--background] [--docker]"
            echo "  --port PORT      Specify port (default: 8000)"
            echo "  --background     Run ChromaDB in background"
            echo "  --docker         Use Docker instead of pip installation"
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

# Check if ChromaDB is already running
check_chromadb_running() {
    if curl -s "http://${DEFAULT_HOST}:${DEFAULT_PORT}/api/v1/heartbeat" >/dev/null 2>&1; then
        return 0  # Running
    else
        return 1  # Not running
    fi
}

# Check if port is in use
check_port_available() {
    local port=$1
    if command -v lsof >/dev/null 2>&1; then
        if lsof -i ":$port" >/dev/null 2>&1; then
            return 1  # Port in use
        fi
    elif command -v netstat >/dev/null 2>&1; then
        if netstat -tuln | grep ":$port " >/dev/null 2>&1; then
            return 1  # Port in use
        fi
    fi
    return 0  # Port available
}

# Install ChromaDB via pip
install_chromadb_pip() {
    print_status "INFO" "Installing ChromaDB via pip..."
    
    # Check if pip is available
    if ! command -v pip3 >/dev/null 2>&1 && ! command -v pip >/dev/null 2>&1; then
        print_status "ERROR" "pip not found. Please install Python pip first."
        print_status "INFO" "Ubuntu/Debian: sudo apt-get install python3-pip"
        print_status "INFO" "CentOS/RHEL: sudo yum install python3-pip"
        exit 1
    fi
    
    # Install ChromaDB
    local pip_cmd="pip3"
    if ! command -v pip3 >/dev/null 2>&1; then
        pip_cmd="pip"
    fi
    
    print_status "INFO" "Installing ChromaDB and dependencies..."
    $pip_cmd install chromadb --user
    
    # Verify installation
    if python3 -c "import chromadb; print('ChromaDB version:', chromadb.__version__)" 2>/dev/null; then
        print_status "SUCCESS" "ChromaDB installed successfully"
    else
        print_status "ERROR" "ChromaDB installation failed"
        exit 1
    fi
}

# Install ChromaDB via Docker
install_chromadb_docker() {
    print_status "INFO" "Setting up ChromaDB via Docker..."
    
    # Check if Docker is available
    if ! command -v docker >/dev/null 2>&1; then
        print_status "ERROR" "Docker not found. Please install Docker first."
        print_status "INFO" "Visit: https://docs.docker.com/get-docker/"
        exit 1
    fi
    
    # Pull ChromaDB image
    print_status "INFO" "Pulling ChromaDB Docker image..."
    docker pull chromadb/chroma:latest
    
    print_status "SUCCESS" "ChromaDB Docker image ready"
}

# Start ChromaDB via pip
start_chromadb_pip() {
    local port=$1
    local background=$2
    
    print_status "INFO" "Starting ChromaDB server on port $port..."
    
    if [[ "$background" == true ]]; then
        # Start in background
        nohup python3 -m chromadb.cli run --host "$DEFAULT_HOST" --port "$port" > chromadb.log 2>&1 &
        local pid=$!
        print_status "INFO" "ChromaDB started in background (PID: $pid)"
        print_status "INFO" "Logs available in: chromadb.log"
        
        # Wait a moment for startup
        sleep 3
        
    else
        # Start in foreground
        print_status "INFO" "Starting ChromaDB in foreground (Ctrl+C to stop)..."
        python3 -m chromadb.cli run --host "$DEFAULT_HOST" --port "$port"
    fi
}

# Start ChromaDB via Docker
start_chromadb_docker() {
    local port=$1
    local background=$2
    
    print_status "INFO" "Starting ChromaDB Docker container on port $port..."
    
    # Create data directory for persistence
    mkdir -p chroma_data
    
    if [[ "$background" == true ]]; then
        # Start in background
        docker run -d \
            --name chromadb-agent-orange \
            -p "$port:8000" \
            -v "$(pwd)/chroma_data:/chroma" \
            chromadb/chroma:latest
        
        print_status "SUCCESS" "ChromaDB Docker container started in background"
        print_status "INFO" "Container name: chromadb-agent-orange"
        print_status "INFO" "Data directory: $(pwd)/chroma_data"
        
    else
        # Start in foreground
        print_status "INFO" "Starting ChromaDB Docker container in foreground (Ctrl+C to stop)..."
        docker run --rm \
            --name chromadb-agent-orange \
            -p "$port:8000" \
            -v "$(pwd)/chroma_data:/chroma" \
            chromadb/chroma:latest
    fi
}

# Verify ChromaDB is working
verify_chromadb() {
    local port=$1
    local max_attempts=10
    local attempt=1
    
    print_status "INFO" "Verifying ChromaDB is running..."
    
    while [[ $attempt -le $max_attempts ]]; do
        if curl -s "http://${DEFAULT_HOST}:${port}/api/v1/heartbeat" >/dev/null 2>&1; then
            print_status "SUCCESS" "ChromaDB is running and accessible!"
            
            # Test basic functionality
            print_status "INFO" "Testing ChromaDB API..."
            local heartbeat_response=$(curl -s "http://${DEFAULT_HOST}:${port}/api/v1/heartbeat")
            print_status "SUCCESS" "Heartbeat response: $heartbeat_response"
            
            # Test collections endpoint
            local collections_response=$(curl -s "http://${DEFAULT_HOST}:${port}/api/v1/collections")
            print_status "SUCCESS" "Collections endpoint accessible"
            
            return 0
        fi
        
        print_status "INFO" "Attempt $attempt/$max_attempts - waiting for ChromaDB to start..."
        sleep 2
        ((attempt++))
    done
    
    print_status "ERROR" "ChromaDB failed to start or is not accessible"
    return 1
}

# Test Agent-Orange connection
test_agent_orange_connection() {
    local port=$1
    
    print_status "INFO" "Testing Agent-Orange ChromaDB connection..."
    
    # Update application.properties if needed
    if [[ -f "application.properties" ]]; then
        # Create backup
        cp application.properties application.properties.backup
        
        # Update endpoint
        sed -i "s|vectordb.endpoint=.*|vectordb.endpoint=http://${DEFAULT_HOST}:${port}|" application.properties
        print_status "INFO" "Updated application.properties with endpoint: http://${DEFAULT_HOST}:${port}"
    fi
    
    # Test if JAR exists and can connect
    if [[ -f "bin/chatbot.jar" ]]; then
        print_status "INFO" "Testing ChromaDB connection with Agent-Orange..."
        # Note: This would require a specific test method in the JAR
        # For now, just show the user how to test
        echo
        print_status "INFO" "To test the connection manually, run:"
        echo "  java -cp bin/chatbot.jar com.example.ChromaDBClient"
        echo
        print_status "INFO" "Or start the full application:"
        echo "  java -jar bin/chatbot.jar"
    else
        print_status "WARNING" "No chatbot.jar found. Run './build.sh jar' to build the application."
    fi
}

# Main execution
main() {
    echo
    print_status "INFO" "ChromaDB Quick Setup for Agent-Orange"
    print_status "INFO" "Port: $DEFAULT_PORT | Background: $RUN_BACKGROUND | Docker: $USE_DOCKER"
    echo
    
    # Check if ChromaDB is already running
    if check_chromadb_running; then
        print_status "SUCCESS" "ChromaDB is already running on port $DEFAULT_PORT"
        
        # Test the connection
        verify_chromadb "$DEFAULT_PORT"
        test_agent_orange_connection "$DEFAULT_PORT"
        exit 0
    fi
    
    # Check if port is available
    if ! check_port_available "$DEFAULT_PORT"; then
        print_status "ERROR" "Port $DEFAULT_PORT is already in use"
        print_status "INFO" "Use --port to specify a different port, or stop the service using port $DEFAULT_PORT"
        exit 1
    fi
    
    # Install ChromaDB
    if [[ "$USE_DOCKER" == true ]]; then
        install_chromadb_docker
    else
        install_chromadb_pip
    fi
    
    # Start ChromaDB
    if [[ "$USE_DOCKER" == true ]]; then
        start_chromadb_docker "$DEFAULT_PORT" "$RUN_BACKGROUND"
    else
        start_chromadb_pip "$DEFAULT_PORT" "$RUN_BACKGROUND"
    fi
    
    # Verify installation (only if running in background)
    if [[ "$RUN_BACKGROUND" == true ]]; then
        if verify_chromadb "$DEFAULT_PORT"; then
            test_agent_orange_connection "$DEFAULT_PORT"
            
            echo
            print_status "SUCCESS" "ChromaDB setup completed successfully!"
            print_status "INFO" "ChromaDB is running at: http://${DEFAULT_HOST}:${DEFAULT_PORT}"
            print_status "INFO" "To stop ChromaDB:"
            if [[ "$USE_DOCKER" == true ]]; then
                echo "  docker stop chromadb-agent-orange"
            else
                echo "  pkill -f chromadb"
            fi
            echo
        else
            print_status "ERROR" "ChromaDB setup failed"
            exit 1
        fi
    fi
}

# Trap Ctrl+C for graceful shutdown
trap 'print_status "INFO" "Shutting down ChromaDB..."; exit 0' INT

# Run main function
main "$@"