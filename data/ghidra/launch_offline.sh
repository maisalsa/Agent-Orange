#!/bin/bash
# Ghidra Offline Launcher for ThinkStation P320

export GHIDRA_HOME="/opt/ghidra"
export GHIDRA_PROJECT_DIR="$DATA_DIR/ghidra/projects"

# Disable network access
export JAVA_OPTS="$JAVA_OPTS -Djava.net.useSystemProxies=false"
export JAVA_OPTS="$JAVA_OPTS -Dhttp.proxyHost="
export JAVA_OPTS="$JAVA_OPTS -Dhttps.proxyHost="

# Memory optimization for i7-7700T
export GHIDRA_MAX_MEM="3G"
export GHIDRA_INITIAL_MEM="512M"

# Use offline configuration
export GHIDRA_USER_SETTINGS_DIR="$DATA_DIR/ghidra/config"

exec "$GHIDRA_HOME/support/analyzeHeadless" "$@"
