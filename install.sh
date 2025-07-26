#!/bin/bash
set -e

# Function to check/install a package using pacman or apt-get
detect_and_install() {
    PKG="$1"
    PACMAN_NAME="$2"
    APT_NAME="$3"
    if command -v pacman >/dev/null 2>&1; then
        if ! pacman -Qi "$PACMAN_NAME" >/dev/null 2>&1; then
            echo "[+] Installing $PKG with pacman..."
            sudo pacman -Sy --noconfirm "$PACMAN_NAME"
        fi
    elif command -v apt-get >/dev/null 2>&1; then
        if ! dpkg -s "$APT_NAME" >/dev/null 2>&1; then
            echo "[+] Installing $PKG with apt-get..."
            sudo apt-get update && sudo apt-get install -y "$APT_NAME"
        fi
    else
        echo "[-] No supported package manager found (pacman or apt-get required)."
        exit 1
    fi
}

echo "[+] Checking/installing OpenJDK..."
detect_and_install "OpenJDK" "jdk-openjdk" "openjdk-11-jdk"

echo "[+] Checking/installing Python3..."
detect_and_install "Python3" "python" "python3"

echo "[+] Checking/installing pip..."
detect_and_install "pip" "python-pip" "python3-pip"

# Validate presence of native .so libraries in bin/
if [ ! -d "bin" ]; then
    echo "[-] bin/ directory not found. Please ensure native .so libraries are in bin/."
    exit 1
fi
SO_COUNT=$(ls bin/*.so 2>/dev/null | wc -l)
if [ "$SO_COUNT" -eq 0 ]; then
    echo "[-] No .so libraries found in bin/. Please build or place JNI libraries in bin/."
    exit 1
fi
echo "[+] Found $SO_COUNT native .so libraries in bin/."

# Set LD_LIBRARY_PATH for JNI
export LD_LIBRARY_PATH="$(pwd)/bin:$LD_LIBRARY_PATH"
echo "[+] LD_LIBRARY_PATH set to: $LD_LIBRARY_PATH"

# Optionally check for Ghidra in PATH
if command -v analyzeHeadless >/dev/null 2>&1; then
    echo "[+] Ghidra analyzeHeadless found in PATH."
else
    echo "[!] Ghidra analyzeHeadless not found in PATH. Please add Ghidra's support/ directory to your PATH if you plan to use Ghidra integration."
fi

echo "\n[+] Install complete."
echo "[!] To run the chatbot, ensure you use this shell or export LD_LIBRARY_PATH as above."
echo "[!] Example:"
echo "    export LD_LIBRARY_PATH=\"$(pwd)/bin:$LD_LIBRARY_PATH\""
echo "    java -cp main Main" 