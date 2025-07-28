# Agent-Orange Offline Deployment Guide
## ThinkStation Tiny P320 Optimization

### 🎯 **Overview**

This guide provides complete instructions for deploying Agent-Orange in a fully offline environment on the Lenovo ThinkStation Tiny P320 with Intel i7-7700T processor. No internet connectivity is required after initial setup.

---

## 🏗️ **Hardware Specifications**

### **Target Platform**
- **Model**: Lenovo ThinkStation Tiny P320
- **CPU**: Intel i7-7700T (4 cores, 8 threads, 2.9-3.8 GHz)
- **RAM**: 16-32 GB DDR4-2400 (recommended: 32 GB)
- **Storage**: 256+ GB NVMe SSD (recommended: 512+ GB)
- **Form Factor**: Ultra-compact (1.35L volume)

### **Performance Optimizations**
- Memory allocation tuned for 4C/8T processor
- Native library compilation with Kaby Lake optimizations
- Garbage collection optimized for low-latency
- I/O operations optimized for SSD storage

---

## 📦 **Installation Process**

### **Phase 1: Online Setup (One-Time)**

#### **1. Download Agent-Orange**
```bash
git clone https://github.com/your-org/agent-orange.git
cd agent-orange
```

#### **2. Install Prerequisites**
```bash
# Java 17+ (required)
sudo apt update
sudo apt install openjdk-17-jdk

# Build tools
sudo apt install build-essential git curl wget unzip

# Python for embedding models (optional)
sudo apt install python3 python3-pip python3-venv
```

#### **3. Run Offline Setup**
```bash
# Complete offline setup with model downloads
./setup_offline.sh --download-models

# This will:
# - Download embedding models (~90 MB)
# - Configure offline directories
# - Create offline configuration files
# - Set up ChromaDB in embedded mode
# - Configure Ghidra for offline operation
```

#### **4. Build Application**
```bash
# Build for offline operation
./gradlew buildOffline

# Or use the build script
./build.sh all
```

### **Phase 2: Offline Verification**

#### **1. Disconnect Network**
```bash
# Disable network interface (varies by system)
sudo ip link set eth0 down
sudo ip link set wlan0 down

# Or disconnect physically
```

#### **2. Verify Offline Readiness**
```bash
./verify_offline.sh

# Expected output:
# ✅ Network offline - perfect for testing
# ✅ bin/chatbot.jar exists
# ✅ offline-config.properties exists
# ✅ All directories created
# ✅ Application starts successfully
```

#### **3. Test Offline Operation**
```bash
./run_offline.sh

# Should start successfully with:
# ✅ Starting Agent-Orange in OFFLINE mode...
# ✅ Optimized for ThinkStation Tiny P320 (i7-7700T)
# ✅ No active network connections (ideal for offline)
# ✅ Launching Agent-Orange in offline mode...
```

---

## ⚙️ **Configuration Details**

### **Memory Configuration (i7-7700T Optimized)**
```properties
# JVM Heap Settings
memory.heap.max=6g              # 6GB heap (for 16-32GB system)
memory.heap.initial=1g          # Conservative initial allocation
memory.gc.type=G1GC             # Low-latency garbage collection
memory.gc.pause.target=200      # 200ms max GC pause

# Component Memory Allocation
vectordb.memory.limit=2g        # ChromaDB memory limit
ghidra.memory.max=3g            # Ghidra analysis memory
llama.context.size=2048         # Llama.cpp context window
```

### **CPU Optimization**
```properties
# Thread Configuration
cpu.cores.available=4           # Physical cores
cpu.threads.available=8         # Logical threads (hyperthreading)
cpu.worker.threads=6            # Gradle worker threads
ghidra.analysis.threads=4       # Ghidra analysis threads
llama.cpu.threads=4             # Llama.cpp CPU threads
```

### **Storage Configuration**
```properties
# Local Data Directories
vectordb.data.directory=./data/chromadb
ghidra.project.dir=./data/ghidra/projects
llama.model.directory=./models/llama
cache.root=./cache
temp.directory=./tmp

# Size Limits
cache.max.size=2g
temp.max.size=5g
```

---

## 🔧 **Component Configuration**

### **ChromaDB (Embedded Mode)**

#### **Configuration**
```yaml
# data/chromadb/config.yaml
persist_directory: "./data/chromadb/persist"
chroma_db_impl: "duckdb+parquet"
anonymized_telemetry: false

settings:
  chroma_memory_limit_bytes: 2147483648  # 2GB limit
  chroma_segment_cache_policy: "LRU"
```

#### **Features in Offline Mode**
- ✅ Document storage and retrieval
- ✅ Vector similarity search
- ✅ Metadata filtering
- ✅ Collection management
- ❌ External embedding APIs
- ❌ Remote server connections

### **Ghidra (Headless Mode)**

#### **Configuration**
```properties
# data/ghidra/config/preferences
ANALYSIS_PARALLEL_THREAD_COUNT=4
ANALYSIS_MAX_MEMORY_GB=3
ANALYSIS_TIMEOUT_MINUTES=5

# Disabled Features
UPDATES_CHECK_FOR_UPDATES=false
EXTERNAL_TOOLS_ENABLED=false
NETWORK_DISCOVERY_ENABLED=false
```

#### **Features in Offline Mode**
- ✅ Binary disassembly
- ✅ Function analysis
- ✅ String extraction
- ✅ Control flow analysis
- ✅ Local script execution
- ❌ Symbol server access
- ❌ Plugin updates
- ❌ Collaboration features

### **Llama.cpp (Native)**

#### **Configuration**
```properties
# Native Library Settings
llama.native.library.path=./bin/native-libs
llama.threads=4
llama.cpu.affinity=true
llama.instruction.set=AVX2        # i7-7700T supports AVX2
llama.memory.map=true
```

#### **Features in Offline Mode**
- ✅ Local LLM inference
- ✅ Text generation
- ✅ Context-aware responses
- ✅ Streaming output
- ❌ Model downloads
- ❌ Online fine-tuning

---

## 📊 **Performance Expectations**

### **Benchmark Results (ThinkStation P320)**

| Operation | Time | Memory Usage | Notes |
|-----------|------|--------------|-------|
| **Application Startup** | 15-30s | 2-3 GB | Including JVM warmup |
| **Ghidra Binary Analysis** | 30s-5m | 1-4 GB | Depends on binary size |
| **ChromaDB Query (1K docs)** | 50-200ms | 500 MB | Local vector search |
| **LLM Response (2K context)** | 5-30s | 1-2 GB | CPU-based inference |
| **Build Process** | 2-5 min | 4-6 GB | Full compilation |

### **Resource Utilization**
```
Typical Operation:
- CPU Usage: 40-80% (during analysis)
- Memory Usage: 8-12 GB total
- Disk I/O: Moderate (SSD recommended)
- Network: 0% (offline mode)

Peak Operation (All components active):
- CPU Usage: 90-100%
- Memory Usage: 14-18 GB
- Disk Usage: 20-50 GB (data + cache)
```

---

## 🔍 **Troubleshooting**

### **Common Issues**

#### **Insufficient Memory**
```bash
# Symptoms
OutOfMemoryError: Java heap space
Application crashes during analysis

# Solutions
# 1. Reduce heap size if system has < 16GB RAM
export JAVA_OPTS="-Xmx4g -Xms1g"

# 2. Increase swap space
sudo fallocate -l 8G /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
```

#### **Native Library Loading Issues**
```bash
# Symptoms
UnsatisfiedLinkError: no llama in java.library.path

# Solutions
# 1. Verify native library exists
ls -la bin/native-libs/

# 2. Check library path
export LD_LIBRARY_PATH=$PWD/bin:$PWD/bin/native-libs:$LD_LIBRARY_PATH

# 3. Rebuild native library
./gradlew compileJNI
```

#### **Slow Performance**
```bash
# Symptoms
Very slow startup or analysis times

# Solutions
# 1. Verify SSD is being used
df -T .

# 2. Check CPU frequency scaling
cat /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor

# 3. Monitor resource usage
htop
iotop
```

### **Performance Tuning**

#### **For 16GB RAM Systems**
```properties
# Reduced memory configuration
memory.heap.max=4g
vectordb.memory.limit=1g
ghidra.memory.max=2g
```

#### **For Maximum Performance**
```bash
# CPU Governor
echo performance | sudo tee /sys/devices/system/cpu/cpu*/cpufreq/scaling_governor

# Disable swap
sudo swapoff -a

# Use high-performance JVM
export JAVA_OPTS="$JAVA_OPTS -XX:+UnlockExperimentalVMOptions -XX:+UseJVMCICompiler"
```

---

## 🔒 **Security Considerations**

### **Network Isolation Verification**
```bash
# Verify no outbound connections
netstat -an | grep ESTABLISHED

# Monitor network activity
sudo tcpdump -i any 'host not 127.0.0.1'

# Block network access (firewall)
sudo iptables -A OUTPUT -j DROP
sudo iptables -A INPUT -j DROP
sudo iptables -A INPUT -i lo -j ACCEPT
sudo iptables -A OUTPUT -o lo -j ACCEPT
```

### **Data Isolation**
- All data stored locally in `./data/`
- No external data transmission
- Local encryption recommended for sensitive data
- Regular backups to isolated storage

---

## 📚 **File Structure**

### **Offline Deployment Layout**
```
agent-orange/
├── bin/
│   ├── chatbot.jar              # Main application (offline build)
│   └── native-libs/             # Native libraries
│       ├── libllama.so          # Llama.cpp library (Linux)
│       ├── libllama.dylib       # Llama.cpp library (macOS)
│       └── llama.dll            # Llama.cpp library (Windows)
├── data/
│   ├── chromadb/               # ChromaDB data
│   │   ├── persist/            # Persistent storage
│   │   ├── index/              # Vector indexes
│   │   └── config.yaml         # ChromaDB configuration
│   └── ghidra/                 # Ghidra data
│       ├── projects/           # Analysis projects
│       └── config/             # Offline configuration
├── models/
│   ├── sentence-transformers-all-MiniLM-L6-v2/  # Embedding model
│   └── llama/                  # LLM models (optional)
├── cache/                      # Application cache
├── logs/                       # Application logs
├── tmp/                        # Temporary files
├── offline-config.properties   # Offline configuration
├── run_offline.sh              # Offline launcher
├── setup_offline.sh            # Offline setup script
└── verify_offline.sh           # Offline verification
```

---

## 🚀 **Quick Start**

### **For First-Time Setup**
```bash
# 1. Clone and setup (online)
git clone https://github.com/your-org/agent-orange.git
cd agent-orange
./setup_offline.sh --download-models

# 2. Build application
./gradlew buildOffline

# 3. Disconnect network and verify
./verify_offline.sh

# 4. Run offline
./run_offline.sh
```

### **For Daily Operation**
```bash
# Start offline mode
./run_offline.sh

# Check status
./verify_offline.sh

# View logs
tail -f logs/agent-orange-offline.log
```

---

## ✅ **Verification Checklist**

### **Pre-Deployment**
- [ ] Java 17+ installed
- [ ] Build tools available (GCC, Gradle)
- [ ] Sufficient disk space (50+ GB)
- [ ] Sufficient RAM (16+ GB)

### **Post-Setup**
- [ ] All directories created
- [ ] Application JAR built successfully
- [ ] Native libraries compiled
- [ ] Embedding model downloaded
- [ ] Offline configuration files present

### **Offline Operation**
- [ ] Network disconnected
- [ ] Application starts without errors
- [ ] All features accessible
- [ ] No network connection attempts
- [ ] Data persisted locally

**This deployment guide ensures Agent-Orange runs completely offline on the ThinkStation Tiny P320 with optimal performance and security.**