# Agent-Orange: Pentesting Chatbot

A comprehensive Java-based CLI pentesting chatbot that integrates LLM inference, binary analysis, vector database operations, and text embedding capabilities.

## 🚀 Features

### Core Capabilities
- **Natural Language Processing**: LLM-powered conversation and analysis
- **Binary Analysis**: Ghidra integration for reverse engineering
- **Vector Database**: ChromaDB integration for document storage and retrieval
- **Text Embedding**: Pluggable embedding backends for semantic search
- **JNI Integration**: Native llama.cpp integration for efficient LLM inference

### Technical Features
- **Thread-safe Operations**: Robust multi-threading support
- **Comprehensive Error Handling**: Graceful degradation and detailed error reporting
- **Modular Architecture**: Pluggable components for easy extension
- **Cross-platform Support**: Linux, macOS, and Windows compatibility
- **Production-ready**: Comprehensive testing and logging

## 📋 Prerequisites

### Required Dependencies
- **Java 17+** (OpenJDK recommended)
- **GCC/G++** compiler toolchain
- **CMake** build system
- **Make** build tool
- **Ghidra** (for binary analysis)
- **ChromaDB** (for vector database operations)
- **llama.cpp** (for LLM inference)

### Optional Dependencies
- **Python 3** (for additional embedding backends)
- **Docker** (for ChromaDB containerized deployment)

## 🛠️ Installation

### Quick Start
```bash
# Clone the repository
git clone https://github.com/maisalsa/Agent-Orange.git
cd Agent-Orange

# Run the installation script
./install.sh
```

### Manual Installation
1. **Install System Dependencies**:
   ```bash
   # Debian/Ubuntu/Kali
   sudo apt-get update
   sudo apt-get install openjdk-17-jdk build-essential cmake python3
   
   # Arch/BlackArch
   sudo pacman -S jdk-openjdk base-devel cmake python
   ```

2. **Install Ghidra**:
   - Download from [https://ghidra-sre.org/](https://ghidra-sre.org/)
   - Extract to `/opt/ghidra` or `/usr/local/ghidra`
   - Ensure `analyzeHeadless` script is executable

3. **Install ChromaDB**:
   ```bash
   pip install chromadb
   chroma run --host localhost --port 8000
   ```

4. **Build JNI Libraries**:
   ```bash
   # Clone and build llama.cpp
   git clone https://github.com/ggerganov/llama.cpp.git
   cd llama.cpp && make
   
   # Build JNI wrapper
   g++ -fPIC -shared \
     -I$JAVA_HOME/include \
     -I$JAVA_HOME/include/linux \
     -I./llama.cpp \
     -L./llama.cpp -lllama \
     -o bin/libllama.so \
     main/llama_jni.cpp
   ```

5. **Build Java Project**:
   ```bash
   # Create bin directory
   mkdir -p bin
   
   # Compile Java classes
   javac -d bin main/*.java
   
   # Create JAR file
   jar cf bin/chatbot.jar -C bin .
   ```

## 🚀 Usage

### Starting the Chatbot
```bash
# Quick start
./run_chatbot.sh

# Manual start
java -jar bin/chatbot.jar
```

### Available Commands
- **General Chat**: Natural language conversation
- **Binary Analysis**: `analyze /path/to/binary with ghidra`
- **Text Embedding**: `embed "text to vectorize"`
- **Vector Search**: `search for documents about security`
- **LLM Generation**: `generate text about cybersecurity`

### Built-in Commands
- `help` - Show help information
- `status` - Display module status
- `history` - Show command history
- `clear` - Clear screen
- `exit` - Exit the chatbot

## 📁 Project Structure

```
Agent-Orange/
├── main/                          # Source code
│   ├── Main.java                  # CLI entry point
│   ├── MCPOrchestrator.java       # Central controller
│   ├── LlamaJNI.java              # LLM interface
│   ├── llama_jni.cpp              # JNI implementation
│   ├── GhidraBridge.java          # Binary analysis
│   ├── EmbeddingClient.java       # Text embedding
│   ├── ChromaDBClient.java        # Vector database
│   └── *Test.java                 # Unit tests
├── bin/                           # Compiled binaries
│   ├── chatbot.jar                # Main application
│   └── libllama.so                # JNI library
├── install.sh                     # Installation script
├── run_chatbot.sh                 # Startup script
├── application.properties         # Configuration
└── README.md                      # This file
```

## ⚙️ Configuration

### Environment Variables
```bash
export LD_LIBRARY_PATH="$(pwd)/bin:$LD_LIBRARY_PATH"
export JAVA_OPTS="-Xmx4g -Xms2g"
export GHIDRA_PATH="/opt/ghidra/support/analyzeHeadless"
export CHROMADB_ENDPOINT="http://localhost:8000"
```

### Application Properties
Edit `application.properties` to configure:
- Vector database endpoint
- Ghidra installation path
- JNI library settings
- Model parameters

## 🧪 Testing

### Run Unit Tests
```bash
# Compile and run tests
javac -cp ".:junit-4.13.2.jar" main/*Test.java
java -cp ".:junit-4.13.2.jar:hamcrest-core-1.3.jar" org.junit.runner.JUnitCore TestMain
```

### Integration Testing
```bash
# Run comprehensive end-to-end tests
java -cp bin TestMain
```

## 🔧 Development

### Building from Source
```bash
# Compile Java source
javac -d bin main/*.java

# Build JNI library
g++ -fPIC -shared -lllama -o bin/libllama.so main/llama_jni.cpp

# Create JAR
jar cf bin/chatbot.jar -C bin .
```

### Adding New Modules
1. Create your module class in `main/`
2. Implement the appropriate interface
3. Register with `MCPOrchestrator`
4. Add unit tests
5. Update documentation

## 🐛 Troubleshooting

### Common Issues
- **JNI Library Not Found**: Ensure `LD_LIBRARY_PATH` is set correctly
- **Ghidra Not Found**: Verify installation path in `application.properties`
- **ChromaDB Connection Failed**: Check if server is running on port 8000
- **Java Version Issues**: Ensure Java 17+ is installed and `JAVA_HOME` is set

### Debug Mode
```bash
# Enable debug logging
export JAVA_OPTS="$JAVA_OPTS -Djava.util.logging.config.file=logging.properties"
./run_chatbot.sh
```

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests for new functionality
5. Submit a pull request

## 📞 Support

For issues and questions:
- Create an issue on GitHub
- Check the troubleshooting section
- Review the test files for usage examples

---

**Agent-Orange** - Empowering pentesters with AI-driven analysis tools.
