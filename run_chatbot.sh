#!/bin/bash
set -e

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

# Check for at least one .so file in bin/
SO_COUNT=$(ls bin/*.so 2>/dev/null | wc -l)
if [ "$SO_COUNT" -eq 0 ]; then
    echo "[ERROR] No JNI .so libraries found in bin/. Please build or place native libraries in bin/."
    exit 3
fi

echo "[+] Starting pentesting chatbot CLI..."
echo "[+] LD_LIBRARY_PATH: $LD_LIBRARY_PATH"

java -jar bin/chatbot.jar 