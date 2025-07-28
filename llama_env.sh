#!/bin/bash
# Llama.cpp Environment Configuration for Agent-Orange
# Source this file to set up llama.cpp environment

# Set library path for native library loading
export LD_LIBRARY_PATH="$PWD/bin:$LD_LIBRARY_PATH"
export DYLD_LIBRARY_PATH="$PWD/bin:$DYLD_LIBRARY_PATH"

# Set Java library path
export JAVA_OPTS="-Djava.library.path=bin $JAVA_OPTS"

echo "Llama.cpp environment configured:"
echo "  LD_LIBRARY_PATH: $LD_LIBRARY_PATH"
echo "  JAVA_OPTS: $JAVA_OPTS"
echo ""
echo "Usage:"
echo "  java -jar bin/chatbot.jar"
echo "  ./run_chatbot.sh"
