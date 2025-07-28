#!/bin/bash
set -e

# Create necessary directories
mkdir -p logs
mkdir -p bin

# Set LD_LIBRARY_PATH for JNI
export LD_LIBRARY_PATH="$(pwd)/bin:$LD_LIBRARY_PATH"

# Check for Java
if ! command -v java >/dev/null 2>&1; then
    echo "[ERROR] Java runtime not found. Please install OpenJDK (e.g., sudo pacman -S jdk-openjdk or sudo apt-get install openjdk-11-jre)."
    exit 1
fi

# Check for chatbot.jar
if [ ! -f "bin/chatbot.jar" ]; then
    echo "[ERROR] bin/chatbot.jar not found. Please build or place the chatbot JAR in bin/."
    exit 2
fi

# Check for native libraries (optional)
NATIVE_LIBS=$(ls bin/*.so bin/*.dylib bin/*.dll 2>/dev/null | wc -l || echo 0)
if [ "$NATIVE_LIBS" -eq 0 ]; then
    echo "[WARNING] No native libraries found in bin/."
    echo "[INFO] The application will run with limited functionality."
    echo "[INFO] To enable native features, run: ./setup_llama.sh"
else
    echo "[+] Found $NATIVE_LIBS native library(ies) in bin/"
fi

echo "[+] Starting pentesting chatbot CLI..."
echo "[+] LD_LIBRARY_PATH: $LD_LIBRARY_PATH"
echo "[+] DYLD_LIBRARY_PATH: $DYLD_LIBRARY_PATH"

# Set additional library paths for macOS
export DYLD_LIBRARY_PATH="$(pwd)/bin:$DYLD_LIBRARY_PATH"

java -Djava.library.path=bin -jar bin/chatbot.jar 