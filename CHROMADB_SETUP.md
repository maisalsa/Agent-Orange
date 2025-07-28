# ChromaDB Configuration & Troubleshooting Guide

## 🔍 **Current Issue Diagnosis**

Based on the analysis, **ChromaDB is not currently running** on your system. Here's how to fix it:

## 📋 **ChromaDB Connection Configuration**

### **Configuration Priority (Highest to Lowest)**

1. **Environment Variable** (Highest Priority)
   ```bash
   export VECTORDB_ENDPOINT=http://localhost:8000
   ```

2. **Application Properties File** (Medium Priority)
   ```properties
   # In application.properties
   vectordb.endpoint=http://localhost:8000
   ```

3. **Default Fallback** (Lowest Priority)
   ```
   http://localhost:8000
   ```

### **Current Configuration**
```properties
# Your current application.properties setting:
vectordb.endpoint=http://localhost:8000
```

## 🚀 **Quick Setup: Get ChromaDB Running**

### **Option 1: Docker (Recommended - Fastest)**
```bash
# Install and run ChromaDB via Docker
docker run -p 8000:8000 chromadb/chroma:latest

# Verify it's running
curl http://localhost:8000/api/v1/heartbeat
```

### **Option 2: Python Installation**
```bash
# Install ChromaDB
pip install chromadb

# Start the server
python3 -m chromadb.cli run --host localhost --port 8000
```

### **Option 3: Using the Project Installer**
```bash
# The install.sh script should handle ChromaDB setup
./install.sh
```

## 🔧 **Step-by-Step ChromaDB Setup**

### **Step 1: Install ChromaDB**

#### **Method A: Via pip (Python)**
```bash
# Install ChromaDB and dependencies
pip install chromadb

# Verify installation
python3 -c "import chromadb; print('ChromaDB version:', chromadb.__version__)"
```

#### **Method B: Via Docker**
```bash
# Pull the ChromaDB Docker image
docker pull chromadb/chroma:latest

# Verify the image
docker images | grep chroma
```

### **Step 2: Start ChromaDB Server**

#### **Method A: Python Server**
```bash
# Start ChromaDB server
python3 -m chromadb.cli run --host 0.0.0.0 --port 8000

# Alternative with specific settings
chromadb run --host localhost --port 8000 --log-level INFO
```

#### **Method B: Docker Server**
```bash
# Run ChromaDB in Docker
docker run -p 8000:8000 chromadb/chroma:latest

# Run with persistent storage
docker run -p 8000:8000 -v $(pwd)/chroma_data:/chroma chromadb/chroma:latest
```

#### **Method C: Background Service**
```bash
# Run ChromaDB as background service
nohup python3 -m chromadb.cli run --host localhost --port 8000 > chromadb.log 2>&1 &

# Check if it's running
ps aux | grep chromadb
```

### **Step 3: Verify ChromaDB is Running**

#### **Check Process**
```bash
# Check if ChromaDB process is running
ps aux | grep -i chroma

# Check port 8000 is open
lsof -i :8000  # or: netstat -tulpn | grep :8000
```

#### **Test HTTP Endpoint**
```bash
# Test ChromaDB health endpoint
curl http://localhost:8000/api/v1/heartbeat

# Expected response: {"nanosecond heartbeat": timestamp}
```

#### **Test API Functionality**
```bash
# Test creating a collection
curl -X POST http://localhost:8000/api/v1/collections \
  -H "Content-Type: application/json" \
  -d '{"name": "test_collection"}'

# Test listing collections
curl http://localhost:8000/api/v1/collections
```

## 🧪 **Testing ChromaDB Connection from Agent-Orange**

### **Method 1: Using the Built-in Test**
```bash
# Test ChromaDB client functionality
cd /workspace
java -cp bin/chatbot.jar com.example.ChromaDBClient
```

### **Method 2: Custom Configuration Test**
```java
// Create a test class to verify connection
public class ChromaDBConnectionTest {
    public static void main(String[] args) {
        try {
            ChromaDBClient client = ChromaDBClient.fromConfig();
            System.out.println("✅ ChromaDB client created successfully");
            
            // Test basic connectivity
            String result = client.addDocument("test", "doc1", "test document", new float[]{1.0f, 2.0f});
            System.out.println("✅ ChromaDB connection working: " + result);
            
        } catch (Exception e) {
            System.err.println("❌ ChromaDB connection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
```

### **Method 3: Environment Variable Test**
```bash
# Test with custom endpoint
export VECTORDB_ENDPOINT=http://localhost:8000
java -jar bin/chatbot.jar

# Test with remote endpoint
export VECTORDB_ENDPOINT=http://remote-server:8000
java -jar bin/chatbot.jar
```

## 🔍 **Troubleshooting Common Issues**

### **Issue 1: "Connection Refused"**
```
Error: java.net.ConnectException: Connection refused
```

**Solutions:**
```bash
# Check if ChromaDB is running
ps aux | grep chroma

# Check if port 8000 is open
lsof -i :8000

# Start ChromaDB if not running
python3 -m chromadb.cli run --host localhost --port 8000
```

### **Issue 2: "No route to host"**
```
Error: java.net.NoRouteToHostException
```

**Solutions:**
```bash
# Check network connectivity
ping localhost

# Test port accessibility
telnet localhost 8000

# Check firewall settings
sudo ufw status
```

### **Issue 3: "Port Already in Use"**
```
Error: OSError: [Errno 98] Address already in use
```

**Solutions:**
```bash
# Find what's using port 8000
lsof -i :8000

# Kill the process if needed
sudo kill -9 <PID>

# Use a different port
python3 -m chromadb.cli run --port 8001
```

### **Issue 4: "ChromaDB Module Not Found"**
```
Error: ModuleNotFoundError: No module named 'chromadb'
```

**Solutions:**
```bash
# Install ChromaDB
pip install chromadb

# If using virtual environment
source venv/bin/activate && pip install chromadb

# Check Python path
python3 -c "import sys; print(sys.path)"
```

## ⚙️ **Advanced Configuration**

### **Custom ChromaDB Configuration**
```bash
# Create ChromaDB config file
cat > chromadb_config.yaml << EOF
server:
  host: "0.0.0.0"
  port: 8000
  cors_allow_origins: ["*"]
  
storage:
  path: "./chroma_data"
  
logging:
  level: "INFO"
EOF

# Start with custom config
chromadb run --config chromadb_config.yaml
```

### **Environment-Specific Configurations**

#### **Development Environment**
```properties
# application.properties for development
vectordb.endpoint=http://localhost:8000
vectordb.top_k=5
```

#### **Production Environment**
```properties
# application.properties for production
vectordb.endpoint=http://chromadb-server:8000
vectordb.top_k=10
```

#### **Docker Environment**
```bash
# Docker Compose setup
cat > docker-compose.yml << EOF
version: '3.8'
services:
  chromadb:
    image: chromadb/chroma:latest
    ports:
      - "8000:8000"
    volumes:
      - ./chroma_data:/chroma
    environment:
      - CHROMA_SERVER_HOST=0.0.0.0
      - CHROMA_SERVER_PORT=8000
EOF

# Start with Docker Compose
docker-compose up -d
```

## 🎯 **Verification Checklist**

### **Pre-Flight Checklist**
- [ ] Python 3.7+ installed
- [ ] ChromaDB package installed (`pip list | grep chromadb`)
- [ ] ChromaDB server running (`ps aux | grep chroma`)
- [ ] Port 8000 accessible (`curl http://localhost:8000/api/v1/heartbeat`)
- [ ] Application configuration correct (`application.properties`)

### **Connection Test Commands**
```bash
# 1. Health check
curl http://localhost:8000/api/v1/heartbeat

# 2. Version check
curl http://localhost:8000/api/v1/version

# 3. Collections test
curl http://localhost:8000/api/v1/collections

# 4. Java client test
java -cp bin/chatbot.jar com.example.ChromaDBClient
```

## 📊 **Monitoring ChromaDB**

### **Check ChromaDB Status**
```bash
# Check server logs
tail -f chromadb.log

# Monitor ChromaDB process
watch 'ps aux | grep chroma'

# Check memory usage
ps -o pid,ppid,cmd,%mem,%cpu -C python3 | grep chroma
```

### **Performance Monitoring**
```bash
# Check ChromaDB API response time
time curl http://localhost:8000/api/v1/heartbeat

# Monitor database size
du -sh chroma_data/

# Check active connections
netstat -an | grep :8000
```

## 🚨 **Emergency Troubleshooting**

### **Quick Reset**
```bash
# Stop ChromaDB
pkill -f chromadb

# Clear data (if needed)
rm -rf chroma_data/

# Restart ChromaDB
python3 -m chromadb.cli run --host localhost --port 8000
```

### **Alternative Ports**
If port 8000 is unavailable:
```bash
# Start ChromaDB on different port
python3 -m chromadb.cli run --port 8001

# Update application configuration
export VECTORDB_ENDPOINT=http://localhost:8001
```

### **Remote ChromaDB Setup**
```bash
# Connect to remote ChromaDB instance
export VECTORDB_ENDPOINT=http://remote-chromadb-server:8000

# Test remote connection
curl http://remote-chromadb-server:8000/api/v1/heartbeat
```

## ✅ **Success Indicators**

When everything is working correctly, you should see:

1. **ChromaDB Server Running**
   ```bash
   $ ps aux | grep chroma
   user  12345  0.1  1.2  python3 -m chromadb.cli run
   ```

2. **Port 8000 Open**
   ```bash
   $ curl http://localhost:8000/api/v1/heartbeat
   {"nanosecond heartbeat": 1234567890}
   ```

3. **Agent-Orange Connection Success**
   ```bash
   $ java -jar bin/chatbot.jar
   [INFO] Vector database client initialized
   [INFO] All modules loaded successfully
   ```

## 📞 **Need Help?**

If you're still having issues:

1. **Check the logs**: `tail -f chromadb.log`
2. **Verify network**: `ping localhost && curl http://localhost:8000`
3. **Test manually**: Run the ChromaDB test commands above
4. **Check firewall**: Ensure port 8000 is not blocked

**The most common issue is simply that ChromaDB is not running. Start with the Quick Setup section above!**