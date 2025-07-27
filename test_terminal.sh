#!/bin/bash

# ============================================================================
# Terminal Detection Test Script
# ============================================================================
#
# This script tests the terminal detection functionality in install.sh
# to verify that ASCII art displays correctly based on terminal capabilities.
#
# USAGE:
#   ./test_terminal.sh
# ============================================================================

set -e

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
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
        "HEADER")
            echo -e "${PURPLE}$message${NC}"
            ;;
    esac
}

# Terminal detection function (copied from install.sh)
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

# Test header function (simplified version)
test_header() {
    local title="Test Header"
    
    # Use simple header for narrow terminals or non-UTF-8 terminals
    if [[ $TERMINAL_WIDTH -lt 70 || "$TERMINAL_SUPPORTS_UTF8" != true ]]; then
        echo
        print_status "HEADER" "================================================================================"
        print_status "HEADER" "                        $title"
        print_status "HEADER" "================================================================================"
        print_status "HEADER" "  Terminal Width: $TERMINAL_WIDTH"
        print_status "HEADER" "  UTF-8 Support: $TERMINAL_SUPPORTS_UTF8"
        print_status "HEADER" "  Terminal Type: $TERM"
        print_status "HEADER" "  Language: $LANG"
        print_status "HEADER" "================================================================================"
        echo
        return
    fi
    
    # Calculate dynamic width for the box
    local box_width=$((TERMINAL_WIDTH - 4))
    
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
    
    # Format info lines
    local width_line="║  Terminal Width: $TERMINAL_WIDTH$(printf ' %.0s' $(seq 1 $((box_width - 20 - ${#TERMINAL_WIDTH}))))║"
    local utf8_line="║  UTF-8 Support: $TERMINAL_SUPPORTS_UTF8$(printf ' %.0s' $(seq 1 $((box_width - 18 - ${#TERMINAL_SUPPORTS_UTF8}))))║"
    local term_line="║  Terminal Type: $TERM$(printf ' %.0s' $(seq 1 $((box_width - 18 - ${#TERM}))))║"
    local lang_line="║  Language: $LANG$(printf ' %.0s' $(seq 1 $((box_width - 12 - ${#LANG}))))║"
    
    echo
    print_status "HEADER" "$top_border"
    print_status "HEADER" "$title_line"
    print_status "HEADER" "$separator"
    print_status "HEADER" "$width_line"
    print_status "HEADER" "$utf8_line"
    print_status "HEADER" "$term_line"
    print_status "HEADER" "$lang_line"
    print_status "HEADER" "$bottom_border"
    echo
}

# Test different terminal scenarios
test_scenarios() {
    print_status "INFO" "Testing terminal detection scenarios..."
    
    # Test 1: Current terminal
    print_status "INFO" "=== Test 1: Current Terminal ==="
    detect_terminal_capabilities
    test_header
    
    # Test 2: Simulate narrow terminal
    print_status "INFO" "=== Test 2: Narrow Terminal (50 columns) ==="
    TERMINAL_WIDTH=50
    test_header
    
    # Test 3: Simulate non-UTF-8 terminal
    print_status "INFO" "=== Test 3: Non-UTF-8 Terminal ==="
    TERMINAL_SUPPORTS_UTF8=false
    test_header
    
    # Test 4: Simulate very narrow terminal
    print_status "INFO" "=== Test 4: Very Narrow Terminal (40 columns) ==="
    TERMINAL_WIDTH=40
    TERMINAL_SUPPORTS_UTF8=true
    test_header
    
    # Test 5: Simulate wide terminal
    print_status "INFO" "=== Test 5: Wide Terminal (120 columns) ==="
    TERMINAL_WIDTH=120
    TERMINAL_SUPPORTS_UTF8=true
    test_header
}

# Test UTF-8 character display
test_utf8_display() {
    print_status "INFO" "Testing UTF-8 character display..."
    
    if [[ "$TERMINAL_SUPPORTS_UTF8" == true ]]; then
        print_status "SUCCESS" "UTF-8 characters should display correctly:"
        echo -e "  ╔══════════════════════════════════════════════════════════════╗"
        echo -e "  ║                    Test UTF-8 Display                        ║"
        echo -e "  ╠══════════════════════════════════════════════════════════════╣"
        echo -e "  ║  ✓ UTF-8 box drawing characters                              ║"
        echo -e "  ║  ✓ Unicode symbols: ★ ☆ ♠ ♥ ♦ ♣                              ║"
        echo -e "  ║  ✓ Special characters: → ← ↑ ↓ ↔ ↕                           ║"
        echo -e "  ╚══════════════════════════════════════════════════════════════╝"
    else
        print_status "WARNING" "UTF-8 not supported, using ASCII fallback:"
        echo "  +==============================================================+"
        echo "  |                    Test ASCII Display                        |"
        echo "  +==============================================================+"
        echo "  |  * ASCII box drawing characters                              |"
        echo "  |  * Basic symbols: * # @ % &                                   |"
        echo "  |  * Simple characters: -> <- ^ v                               |"
        echo "  +==============================================================+"
    fi
}

# Main test function
main() {
    print_status "INFO" "Starting terminal detection tests..."
    
    # Detect current terminal capabilities
    detect_terminal_capabilities
    
    # Display current terminal info
    print_status "INFO" "Current terminal information:"
    print_status "INFO" "  Width: $TERMINAL_WIDTH columns"
    print_status "INFO" "  UTF-8 Support: $TERMINAL_SUPPORTS_UTF8"
    print_status "INFO" "  Terminal Type: $TERM"
    print_status "INFO" "  Language: $LANG"
    print_status "INFO" "  LC_ALL: $LC_ALL"
    print_status "INFO" "  LC_CTYPE: $LC_CTYPE"
    
    echo
    
    # Test UTF-8 display
    test_utf8_display
    
    echo
    
    # Test different scenarios
    test_scenarios
    
    print_status "SUCCESS" "Terminal detection tests completed!"
}

# Run main function
main "$@" 