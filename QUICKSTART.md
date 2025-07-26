# Agent-Orange Quick Start Guide

Get up and running with Agent-Orange in minutes!

## 🚀 Quick Installation

### 1. Clone and Install
```bash
git clone https://github.com/maisalsa/Agent-Orange.git
cd Agent-Orange
./install.sh
```

### 2. Build the Project
```bash
./build.sh
```

### 3. Start the Chatbot
```bash
./run_chatbot.sh
```

## 🎯 First Steps

### Basic Usage
Once the chatbot is running, try these commands:

```
help                    # Show available commands
status                  # Check module status
Hello, how are you?     # General conversation
embed "test text"       # Generate text embeddings
search for security     # Search vector database
exit                    # Exit the chatbot
```

### Example Session
```
Agent-Orange> help
[Shows help information]

Agent-Orange> status
[Shows module availability]

Agent-Orange> Hello, I'm a pentester
[LLM responds with pentesting assistance]

Agent-Orange> embed "buffer overflow vulnerability"
[Generates embedding vector]

Agent-Orange> search for buffer overflow techniques
[Searches vector database for relevant documents]
```

## ⚙️ Configuration

### Essential Setup
1. **Edit `application.properties`**:
   ```properties
   vectordb.endpoint=http://localhost:8000
   ghidra.headless.path=/opt/ghidra/support/analyzeHeadless
   ```

2. **Set environment variables**:
   ```bash
   export LD_LIBRARY_PATH="$(pwd)/bin:$LD_LIBRARY_PATH"
   export JAVA_OPTS="-Xmx4g -Xms2g"
   ```

### Optional Dependencies
- **Ghidra**: For binary analysis (`analyze /path/to/binary with ghidra`)
- **ChromaDB**: For vector database operations (`search for documents`)
- **llama.cpp**: For LLM inference (automatic via JNI)

## 🧪 Testing

### Run Tests
```bash
./test.sh
```

### Manual Testing
```bash
# Test Java compilation
javac -d bin main/*.java

# Test JAR creation
jar cf bin/chatbot.jar -C bin .

# Test execution
java -jar bin/chatbot.jar
```

## 🐛 Troubleshooting

### Common Issues

**"Java not found"**
```bash
# Install Java 17+
sudo apt-get install openjdk-17-jdk  # Ubuntu/Debian
sudo pacman -S jdk-openjdk           # Arch
```

**"JNI library not found"**
```bash
# Set library path
export LD_LIBRARY_PATH="$(pwd)/bin:$LD_LIBRARY_PATH"
```

**"Ghidra not found"**
```bash
# Download and install Ghidra
# https://ghidra-sre.org/
# Extract to /opt/ghidra
chmod +x /opt/ghidra/support/analyzeHeadless
```

**"ChromaDB connection failed"**
```bash
# Install and start ChromaDB
pip install chromadb
chroma run --host localhost --port 8000
```

### Debug Mode
```bash
# Enable verbose logging
export JAVA_OPTS="$JAVA_OPTS -Djava.util.logging.config.file=logging.properties"
./run_chatbot.sh
```

## 📚 Next Steps

### Advanced Usage
- **Binary Analysis**: Use Ghidra integration for reverse engineering
- **Vector Search**: Store and query security documents
- **Custom Embeddings**: Implement your own embedding backends
- **Module Extension**: Add new capabilities to the orchestrator

### Development
- **Add Tests**: Extend `TestMain.java` with new test cases
- **New Modules**: Create modules implementing the appropriate interfaces
- **Configuration**: Add new settings to `application.properties`

## 📞 Support

- **Issues**: Create an issue on GitHub
- **Documentation**: Check the main README.md
- **Testing**: Run `./test.sh` for diagnostics

---

**Ready to start?** Run `./install.sh` and begin your pentesting journey with AI assistance! 