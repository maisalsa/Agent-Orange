# ============================================================================
# Agent-Orange Makefile
# ============================================================================
#
# Common tasks for building, testing, and managing the project
#
# USAGE:
#   make install    # Run installation script
#   make build      # Build the project
#   make test       # Run tests
#   make clean      # Clean build artifacts
#   make run        # Run the chatbot
#   make help       # Show this help
# ============================================================================

.PHONY: help install build test clean run all

# Default target
all: install build test

# Show help
help:
	@echo "Agent-Orange Makefile"
	@echo "===================="
	@echo ""
	@echo "Available targets:"
	@echo "  install    - Run installation script"
	@echo "  build      - Build the project"
	@echo "  test       - Run tests"
	@echo "  clean      - Clean build artifacts"
	@echo "  run        - Run the chatbot"
	@echo "  all        - Install, build, and test"
	@echo "  help       - Show this help"
	@echo ""

# Run installation script
install:
	@echo "Running installation script..."
	@chmod +x install.sh
	@./install.sh

# Build the project
build:
	@echo "Building project..."
	@chmod +x build.sh
	@./build.sh

# Run tests
test:
	@echo "Running tests..."
	@chmod +x test.sh
	@./test.sh

# Clean build artifacts
clean:
	@echo "Cleaning build artifacts..."
	@rm -rf bin/*.class bin/*.jar
	@rm -rf tmp/
	@rm -rf logs/
	@echo "Clean complete"

# Run the chatbot
run:
	@echo "Starting chatbot..."
	@chmod +x run_chatbot.sh
	@./run_chatbot.sh

# Quick development cycle
dev: clean build test

# Full setup
setup: install build test
	@echo "Setup complete! Run 'make run' to start the chatbot" 