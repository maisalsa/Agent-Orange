# Agent-Orange Hardware Requirements

## 🎯 **Overview**

This document outlines the hardware requirements for building, deploying, and running Agent-Orange, a pentesting AI assistant that integrates multiple compute-intensive components including Java applications, vector databases, reverse engineering tools, and LLM inference engines.

---

## 📊 **Summary Requirements Table**

| Component | Minimum | Recommended | Optimal |
|-----------|---------|-------------|---------|
| **CPU** | Quad-core 2.4GHz | 8-core 3.0GHz | 16+ core 3.5GHz+ |
| **RAM** | 8 GB | 16 GB | 32+ GB |
| **Storage** | 50 GB SSD | 100 GB NVMe SSD | 500+ GB NVMe SSD |
| **Network** | 10 Mbps | 100 Mbps | 1 Gbps |
| **OS** | Linux/macOS/Windows | Ubuntu 22.04+ LTS | Ubuntu 22.04+ LTS |

---

## 💻 **Detailed Hardware Specifications**

### **🔥 CPU Requirements**

#### **Minimum Configuration**
- **Cores**: 4 cores (8 threads recommended)
- **Clock Speed**: 2.4 GHz base frequency
- **Architecture**: x64 (AMD64/Intel 64-bit)
- **Examples**: Intel Core i5-8265U, AMD Ryzen 5 3500U

#### **Recommended Configuration**
- **Cores**: 8 cores (16 threads)
- **Clock Speed**: 3.0 GHz base frequency
- **Architecture**: x64 with AVX2 support
- **Examples**: Intel Core i7-10700K, AMD Ryzen 7 3700X

#### **Optimal Configuration**
- **Cores**: 16+ cores (32+ threads)
- **Clock Speed**: 3.5+ GHz base frequency
- **Architecture**: x64 with AVX-512 support (Intel) or AVX2+ (AMD)
- **Examples**: Intel Core i9-12900K, AMD Ryzen 9 5900X, Intel Xeon W-2295

#### **CPU Usage Breakdown**
- **Java Application**: 2-4 cores for main application and JVM
- **Ghidra Analysis**: 2-8 cores for headless reverse engineering
- **Native Compilation**: 4+ cores for GCC/G++ builds
- **ChromaDB**: 1-2 cores for vector operations
- **Llama.cpp**: All available cores for inference (CPU-based)

### **🧠 Memory (RAM) Requirements**

#### **Java Application Memory**
```
Application Heap:     4 GB (configured via -Xmx4g)
Application Initial:  2 GB (configured via -Xms2g)
```

#### **Build System Memory**
```
Gradle Daemon:       4 GB (configured via org.gradle.jvmargs=-Xmx4g)
Parallel Builds:     Additional 1-2 GB per worker thread
Test Execution:      2 GB (configured via jvmArgs '-Xmx2g')
```

#### **Ghidra Memory**
```
Ghidra GUI:          2-4 GB for interactive use
Headless Analysis:   1-8 GB depending on binary size
Large Binaries:      Up to 16 GB for complex executables
```

#### **ChromaDB Memory**
ChromaDB stores vector indexes in memory. Calculate using:
```
RAM = number_of_vectors × vector_dimensions × 4_bytes

Example calculations:
- 100K documents (384-dim embeddings): 100,000 × 384 × 4 = ~1.5 GB
- 1M documents (384-dim embeddings):   1,000,000 × 384 × 4 = ~1.5 GB
- 1M documents (1536-dim embeddings):  1,000,000 × 1536 × 4 = ~6 GB
```

#### **Total Memory Requirements**

| Configuration | Java App | Ghidra | ChromaDB | OS/Other | **Total** |
|---------------|----------|--------|----------|----------|-----------|
| **Minimum** | 6 GB | 1 GB | 0.5 GB | 2 GB | **8 GB** |
| **Recommended** | 8 GB | 4 GB | 2 GB | 4 GB | **16 GB** |
| **Optimal** | 12 GB | 8 GB | 8 GB | 8 GB | **32+ GB** |

### **💾 Storage Requirements**

#### **Application Components**
```
Agent-Orange Source:       ~6 MB
Compiled Application:      ~5 MB (JAR file)
Build Artifacts:          ~50 MB
Dependencies Cache:        ~200 MB
```

#### **External Dependencies**
```
Java 17+ JDK:             ~300 MB
Ghidra 11.3:              ~500 MB (extracted)
ChromaDB (Python):        ~100 MB
Llama.cpp (compiled):     ~50 MB
Build Tools (GCC/Gradle): ~500 MB
```

#### **Runtime Data**
```
Ghidra Projects:          1-10 GB (varies by analysis)
ChromaDB Data:            0.5-50 GB (varies by document corpus)
Log Files:                100 MB - 1 GB
Temporary Files:          1-5 GB during builds/analysis
```

#### **Storage Performance Requirements**
- **Minimum**: SATA SSD (500 MB/s read/write)
- **Recommended**: NVMe SSD (1+ GB/s read/write)
- **Optimal**: High-speed NVMe SSD (3+ GB/s read/write)

#### **Total Storage Requirements**

| Configuration | System | Apps | Data | Temp | **Total** |
|---------------|--------|------|------|------|-----------|
| **Minimum** | 20 GB | 2 GB | 5 GB | 5 GB | **50 GB** |
| **Recommended** | 40 GB | 5 GB | 20 GB | 10 GB | **100 GB** |
| **Optimal** | 100 GB | 10 GB | 200 GB | 20 GB | **500+ GB** |

### **🌐 Network Requirements**

#### **Bandwidth Requirements**
- **Initial Setup**: 2-5 GB downloads (Java, Ghidra, dependencies)
- **ChromaDB Updates**: Varies by document corpus size
- **LLM Models**: 3-15 GB per model (if downloading)
- **Git Operations**: 10-50 MB for source updates

#### **Latency Requirements**
- **ChromaDB**: <10ms for local deployment, <100ms for remote
- **External APIs**: <500ms for acceptable user experience

#### **Network Configuration**
| Use Case | Minimum | Recommended |
|----------|---------|-------------|
| **Development** | 10 Mbps | 100 Mbps |
| **Production** | 50 Mbps | 1 Gbps |
| **Offline Use** | N/A | Local deployment |

---

## 🏗️ **Platform-Specific Requirements**

### **🐧 Linux (Recommended)**

#### **Distribution Requirements**
- **Supported**: Ubuntu 20.04+, Debian 11+, RHEL 8+, CentOS 8+
- **Recommended**: Ubuntu 22.04 LTS or Ubuntu 24.04 LTS
- **Kernel**: Linux 5.4+ (for modern container support)

#### **System Dependencies**
```bash
# Essential packages
build-essential      # GCC/G++ compilers
openjdk-17-jdk      # Java development kit
git                 # Version control
curl wget           # Network utilities
unzip               # Archive extraction

# Optional but recommended
docker.io           # Container runtime
python3-venv        # Python virtual environments
cmake               # Alternative build system
```

#### **Linux-Specific Advantages**
- **Best Performance**: Native JNI compilation
- **Container Support**: Docker/Podman for ChromaDB
- **Package Management**: Easy dependency installation
- **Memory Management**: Better large heap handling

### **🍎 macOS**

#### **Version Requirements**
- **Minimum**: macOS 11 (Big Sur)
- **Recommended**: macOS 12+ (Monterey or newer)
- **Architecture**: Intel x64 or Apple Silicon (M1/M2/M3)

#### **Apple Silicon Considerations**
```bash
# Some Java applications may require Rosetta 2
arch -x86_64 java -jar application.jar

# Native ARM builds preferred when available
brew install --cask temurin17    # ARM-native Java
```

#### **macOS-Specific Requirements**
- **Xcode Command Line Tools**: For native compilation
- **Homebrew**: For dependency management
- **Increased Disk Space**: macOS requires more overhead

### **🪟 Windows**

#### **Version Requirements**
- **Minimum**: Windows 10 (1909 or later)
- **Recommended**: Windows 11 or Windows Server 2019+
- **Architecture**: x64 (64-bit)

#### **Windows-Specific Dependencies**
```powershell
# Required components
Windows Subsystem for Linux (WSL 2)  # For Linux tools
Visual Studio Build Tools             # For native compilation
Git for Windows                       # Version control
Java 17+ (AdoptOpenJDK/Eclipse Temurin)
```

#### **Windows Considerations**
- **Path Length**: Enable long path support for Ghidra
- **Execution Policy**: May need to adjust for PowerShell scripts
- **Antivirus**: Exclude build directories from real-time scanning
- **Memory Allocation**: May require adjusted virtual memory settings

---

## ⚡ **Performance Scaling Guidelines**

### **Single-User Development**
```
CPU:     4-8 cores
RAM:     8-16 GB
Storage: 50-100 GB SSD
Network: 50+ Mbps
```

### **Multi-User Team Environment**
```
CPU:     8-16 cores
RAM:     32-64 GB
Storage: 200-500 GB NVMe
Network: 1 Gbps
```

### **Production/Enterprise Deployment**
```
CPU:     16+ cores (or multiple nodes)
RAM:     64+ GB (or distributed)
Storage: 1+ TB NVMe (with backup)
Network: 10 Gbps (with redundancy)
```

### **Performance Optimization Tips**

#### **Memory Optimization**
```bash
# Increase Java heap for large datasets
export JAVA_OPTS="-Xmx8g -Xms4g"

# Optimize Gradle memory usage
echo "org.gradle.jvmargs=-Xmx8g -Xms2g" >> gradle.properties

# Enable parallel garbage collection
export JAVA_OPTS="$JAVA_OPTS -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
```

#### **CPU Optimization**
```bash
# Use all CPU cores for builds
./gradlew build --parallel --max-workers=$(nproc)

# Enable CPU-specific optimizations for native code
export CFLAGS="-march=native -O3"
export CXXFLAGS="-march=native -O3"
```

#### **Storage Optimization**
```bash
# Use SSD for temporary directories
export TMPDIR=/path/to/fast/ssd
export GRADLE_USER_HOME=/path/to/fast/ssd/.gradle

# Enable file system optimizations
mount -o noatime,nodiratime /dev/nvme0n1 /build
```

---

## 🔍 **Component-Specific Details**

### **Java Application Requirements**
- **JVM Version**: OpenJDK 17 or later (17, 21 recommended)
- **Heap Space**: 4-8 GB for normal operation
- **Garbage Collection**: G1GC recommended for large heaps
- **JIT Compilation**: Tiered compilation enabled by default

### **Ghidra Requirements**
- **Java Version**: Same as main application (17+)
- **Memory per Analysis**: 1-8 GB depending on binary complexity
- **Temporary Storage**: 2-10 GB per analysis session
- **CPU Usage**: Scales with available cores (embarrassingly parallel)

### **ChromaDB Requirements**
- **Python Version**: 3.9-3.12 (3.11+ recommended)
- **Memory Formula**: `vectors × dimensions × 4 bytes`
- **Disk Formula**: ~2-4x memory requirements for persistence
- **Concurrent Connections**: Scales with CPU cores

### **Llama.cpp Requirements**
- **CPU Instructions**: AVX2 minimum, AVX-512 preferred
- **Memory**: 4-16 GB depending on model size
- **Model Storage**: 3-15 GB per model file
- **Thread Scaling**: Excellent multi-core utilization

---

## 📈 **Capacity Planning Examples**

### **Small Team (1-5 developers)**
```
Hardware:
- CPU: Intel i7-12700K or AMD Ryzen 7 5700X
- RAM: 32 GB DDR4-3200
- Storage: 1 TB NVMe SSD
- Network: 1 Gbps fiber

Estimated Capacity:
- Document corpus: 100K-1M documents
- Concurrent users: 1-3
- Analysis sessions: 5-10 per day
- Build frequency: Multiple per day
```

### **Medium Organization (5-20 developers)**
```
Hardware:
- CPU: Intel Xeon W-2295 or AMD Threadripper 3970X
- RAM: 64 GB DDR4-3200 ECC
- Storage: 2 TB NVMe SSD + 10 TB HDD backup
- Network: 10 Gbps enterprise

Estimated Capacity:
- Document corpus: 1M-10M documents
- Concurrent users: 5-15
- Analysis sessions: 20-50 per day
- Build frequency: Continuous integration
```

### **Large Enterprise (20+ developers)**
```
Hardware (Distributed):
- Multiple servers or cloud instances
- Load balancing for ChromaDB
- Shared storage with replication
- Dedicated build servers

Estimated Capacity:
- Document corpus: 10M+ documents
- Concurrent users: 20+
- Analysis sessions: 100+ per day
- Build frequency: Continuous deployment
```

---

## ⚠️ **Hardware Warnings and Limitations**

### **Insufficient Memory Symptoms**
```
OutOfMemoryError: Java heap space
OutOfMemoryError: GC overhead limit exceeded
ChromaDB: "Cannot allocate memory for vector index"
Ghidra: Analysis timeout or crash
```

### **Insufficient Storage Symptoms**
```
"No space left on device"
Build failures during compilation
ChromaDB index corruption
Ghidra project corruption
```

### **Insufficient CPU Symptoms**
```
Very slow builds (>30 minutes)
Ghidra analysis timeouts
ChromaDB query timeouts
High CPU wait times
```

### **Network Issues**
```
Dependency download failures
ChromaDB connection timeouts
Git clone/pull failures
Docker image pull errors
```

---

## 🎯 **Quick Hardware Recommendations**

### **Budget Development Setup ($800-1500)**
```
Laptop: Business-grade with 16GB RAM, 512GB SSD
Desktop: Ryzen 5 5600X, 32GB RAM, 1TB NVMe
Performance: Suitable for small projects and learning
```

### **Professional Development Setup ($1500-3000)**
```
Laptop: High-end mobile workstation, 32GB RAM, 1TB SSD
Desktop: Intel i7-12700K/Ryzen 7 5700X, 32GB RAM, 2TB NVMe
Performance: Handles medium projects and team development
```

### **Enterprise/Research Setup ($3000+)**
```
Workstation: Xeon/Threadripper, 64+ GB ECC RAM, 4+ TB storage
Server: Multi-node setup with shared storage and load balancing
Performance: Supports large-scale deployments and research
```

---

## 🔧 **Hardware Optimization Checklist**

### **Pre-Installation**
- [ ] Verify CPU architecture (x64) and instruction sets (AVX2+)
- [ ] Confirm minimum RAM (8GB) and recommend 16+ GB
- [ ] Ensure SSD storage (50+ GB free space)
- [ ] Test network connectivity and bandwidth
- [ ] Check OS compatibility and version

### **Post-Installation**
- [ ] Monitor Java heap usage and adjust if needed
- [ ] Check Ghidra memory allocation for large binaries
- [ ] Verify ChromaDB vector index fits in memory
- [ ] Test native library compilation and execution
- [ ] Benchmark build times and optimize if slow

### **Ongoing Monitoring**
- [ ] Track memory usage trends over time
- [ ] Monitor storage growth (logs, data, projects)
- [ ] Check CPU utilization during peak usage
- [ ] Verify network performance for remote operations
- [ ] Update hardware as requirements grow

---

## 📚 **Additional Resources**

### **Hardware Vendors and Models**
- **Intel**: Core i5/i7/i9, Xeon W series
- **AMD**: Ryzen 5/7/9, Threadripper, EPYC
- **Memory**: Crucial, Kingston, Corsair DDR4/DDR5
- **Storage**: Samsung 980/990 Pro, WD Black SN850X

### **Cloud Alternatives**
- **AWS**: EC2 c5.xlarge+ instances
- **Google Cloud**: Compute Engine n2-standard-8+
- **Azure**: Standard_D8s_v3+ virtual machines
- **Digital Ocean**: Premium CPU-optimized droplets

### **Monitoring Tools**
```bash
# System monitoring
htop, iotop, nethogs    # Real-time resource usage
vmstat, iostat          # Performance statistics
df, du                  # Disk usage
free, /proc/meminfo     # Memory information

# Java monitoring
jconsole, jvisualvm     # JVM monitoring
gc logs                 # Garbage collection analysis
```

**This hardware specification ensures optimal performance for Agent-Orange across development, testing, and production environments.**