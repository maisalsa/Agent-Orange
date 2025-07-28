# Llama.cpp Native Library Setup Guide for Agent-Orange

## 🔍 **Current Issue Diagnosis**

Based on analysis, the Llama.cpp native library is failing to load because:

1. **❌ Native library not compiled** - llama.cpp headers are missing
2. **❌ Library path not configured** - `java.library.path` doesn't include `bin/`
3. **❌ Dependencies not available** - llama.cpp needs to be installed first

## 📋 **Expected Library Locations**

The compiled native libraries should be located at:

| Platform | Library Name | Expected Location | Runtime Path |
|----------|-------------|-------------------|--------------|
| **Linux** | `libllama.so` | `bin/libllama.so` | `LD_LIBRARY_PATH` |
| **macOS** | `libllama.dylib` | `bin/libllama.dylib` | `DYLD_LIBRARY_PATH` |
| **Windows** | `llama.dll` | `bin/llama.dll` | `PATH` |

## 🚀 **Complete Setup Process**

### **Step 1: Install llama.cpp**

#### **Option A: Download Pre-compiled Release**
```bash
# Download latest llama.cpp release
wget https://github.com/ggerganov/llama.cpp/releases/latest/download/llama.cpp-linux-x64.tar.gz
tar -xzf llama.cpp-linux-x64.tar.gz
sudo cp -r llama.cpp-linux-x64/* /usr/local/
```

#### **Option B: Build from Source (Recommended)**
```bash
# Clone llama.cpp repository
git clone https://github.com/ggerganov/llama.cpp.git
cd llama.cpp

# Build llama.cpp
make clean
make -j$(nproc)

# Install headers and libraries
sudo make install

# Copy headers to Agent-Orange
cp llama.h /workspace/main/
cp ggml.h /workspace/main/
```

#### **Option C: Package Manager**
```bash
# Ubuntu/Debian (if available)
sudo apt-get update
sudo apt-get install llama.cpp-dev

# macOS with Homebrew
brew install llama.cpp

# Fedora/RHEL
sudo dnf install llama.cpp-devel
```

### **Step 2: Copy Required Headers**

After installing llama.cpp, copy the required headers:

```bash
# Find llama.cpp installation
find /usr -name "llama.h" 2>/dev/null
find /usr/local -name "llama.h" 2>/dev/null

# Copy headers to Agent-Orange main directory
cp /path/to/llama.h main/
cp /path/to/ggml.h main/  # Also needed
cp /path/to/llama.cpp main/  # Source file
```

### **Step 3: Compile Native Library**

```bash
# Use the build system to compile
./build.sh native

# Or manually compile
./gradlew compileJNI

# Verify compilation
ls -la bin/libllama.so  # Linux
ls -la bin/libllama.dylib  # macOS
ls -la bin/llama.dll  # Windows
```

### **Step 4: Configure Runtime Library Path**

#### **Method A: Environment Variables (Recommended)**
```bash
# Linux
export LD_LIBRARY_PATH=$PWD/bin:$LD_LIBRARY_PATH

# macOS
export DYLD_LIBRARY_PATH=$PWD/bin:$DYLD_LIBRARY_PATH

# Windows
set PATH=%CD%\bin;%PATH%
```

#### **Method B: System Property**
```bash
java -Djava.library.path=bin -jar bin/chatbot.jar
```

#### **Method C: Update build.gradle**
The `applicationDefaultJvmArgs` should already include `-Djava.library.path=bin`.

### **Step 5: Verify Installation**

```bash
# Check library exists
ls -la bin/libllama.*

# Test library loading
java -cp bin/chatbot.jar com.example.LlamaJNI

# Check Java library path
java -XshowSettings:properties 2>&1 | grep java.library.path

# Run full application
java -jar bin/chatbot.jar
```

## 🔧 **Fixing Current Configuration**

### **Issue 1: Missing llama.cpp Headers**

```bash
# Quick fix - create dummy headers for testing
mkdir -p main
touch main/llama.h
echo '#ifndef LLAMA_H' > main/llama.h
echo '#define LLAMA_H' >> main/llama.h
echo 'extern "C" {' >> main/llama.h
echo '// Placeholder for llama.cpp integration' >> main/llama.h
echo '}' >> main/llama.h
echo '#endif' >> main/llama.h

# Then install real llama.cpp and replace
```

### **Issue 2: Library Path Configuration**

The `build.gradle` already configures `java.library.path=bin`, but we need to ensure it's properly passed to the runtime:

```gradle
application {
    applicationDefaultJvmArgs = [
        '-Xmx4g',
        '-Xms2g',
        '-Djava.library.path=bin'  // ✅ Already configured
    ]
}
```

### **Issue 3: Runtime Library Loading**

The `LlamaJNI.java` uses `System.loadLibrary("llama")` which is correct, but we need to ensure the library exists.

## 🧪 **Testing Native Library Integration**

### **Test 1: Verify Compilation**
```bash
# Check if native compilation works
./build.sh clean
./build.sh native

# Should see:
# > Task :compileJNI
# Successfully compiled native library
```

### **Test 2: Library Existence**
```bash
# Check library file exists
ls -la bin/
# Should show: libllama.so (Linux), libllama.dylib (macOS), or llama.dll (Windows)

# Check library dependencies
ldd bin/libllama.so  # Linux
otool -L bin/libllama.dylib  # macOS
```

### **Test 3: Java Loading**
```bash
# Test with correct library path
LD_LIBRARY_PATH=bin java -cp bin/chatbot.jar com.example.LlamaJNI

# Should see:
# [LlamaJNI] Native library loaded successfully
```

### **Test 4: Full Application**
```bash
# Run with library path
LD_LIBRARY_PATH=bin java -jar bin/chatbot.jar

# Should see in logs:
# [LlamaJNI] Native library loaded successfully
# INFO: LLM module initialized: Available
```

## 🔍 **Troubleshooting Common Issues**

### **Issue 1: "llama.cpp headers not found"**
```
llama.cpp headers not found. JNI compilation skipped.
```

**Solutions:**
```bash
# Option A: Install llama.cpp properly
git clone https://github.com/ggerganov/llama.cpp.git
cd llama.cpp && make
cp llama.h /workspace/main/

# Option B: Find existing installation
find /usr -name "llama.h" 2>/dev/null
cp /usr/local/include/llama.h main/

# Option C: Download headers only
wget https://raw.githubusercontent.com/ggerganov/llama.cpp/master/llama.h -O main/llama.h
```

### **Issue 2: "no llama in java.library.path"**
```
UnsatisfiedLinkError: no llama in java.library.path
```

**Solutions:**
```bash
# Check library exists
ls -la bin/libllama.so

# Set library path
export LD_LIBRARY_PATH=$PWD/bin:$LD_LIBRARY_PATH

# Use system property
java -Djava.library.path=bin -jar bin/chatbot.jar

# Check current path
java -XshowSettings:properties 2>&1 | grep java.library.path
```

### **Issue 3: "symbol lookup error"**
```
symbol lookup error: bin/libllama.so: undefined symbol
```

**Solutions:**
```bash
# Check library dependencies
ldd bin/libllama.so

# Install missing dependencies
sudo apt-get install libstdc++6

# Recompile with proper linking
g++ -shared -fPIC ... -lstdc++ -lm
```

### **Issue 4: "Permission denied"**
```
Cannot load library: Permission denied
```

**Solutions:**
```bash
# Fix permissions
chmod +x bin/libllama.so

# Check file ownership
ls -la bin/libllama.so

# Fix ownership
chown $USER:$USER bin/libllama.so
```

### **Issue 5: "Architecture mismatch"**
```
wrong ELF class: ELFCLASS64
```

**Solutions:**
```bash
# Check architecture
file bin/libllama.so
java -version

# Recompile for correct architecture
g++ -m64 ...  # For 64-bit
g++ -m32 ...  # For 32-bit
```

## ⚙️ **Advanced Configuration**

### **Gradle Enhanced Native Build**

Add this to `build.gradle` for better native library handling:

```gradle
// Enhanced native library management
task copyNativeLibs(type: Copy) {
    description = 'Copy native libraries to bin directory'
    group = 'build'
    
    from 'build/libs/native'
    into 'bin'
    include '*.so', '*.dylib', '*.dll'
    
    dependsOn compileJNI
}

// Enhanced JNI header generation
task generateJNIHeader(type: Exec) {
    description = 'Generate JNI header files'
    group = 'build'
    
    commandLine 'javac', '-h', 'main', 
                '-cp', 'build/classes/java/main', 
                'main/LlamaJNI.java'
    
    dependsOn compileJava
    
    inputs.files 'main/LlamaJNI.java'
    outputs.dir 'main'
    outputs.files 'main/com_example_LlamaJNI.h'
}

// Enhanced JNI compilation with better error handling
task compileJNI(type: Exec) {
    description = 'Compile JNI libraries with enhanced configuration'
    group = 'build'
    
    dependsOn generateJNIHeader
    
    def osName = System.getProperty('os.name').toLowerCase()
    def isWindows = osName.contains('windows')
    def isMac = osName.contains('mac')
    def isLinux = osName.contains('linux')
    
    def javaHome = System.getProperty('java.home')
    def outputLib = isWindows ? 'bin/llama.dll' : 
                   isMac ? 'bin/libllama.dylib' : 
                   'bin/libllama.so'
    
    // Enhanced compilation with better linking
    if (isLinux) {
        commandLine 'g++', '-shared', '-fPIC', '-O3',
                   "-I${javaHome}/include",
                   "-I${javaHome}/include/linux",
                   '-Imain',
                   '-o', outputLib,
                   'main/llama_jni.cpp',
                   '-lstdc++', '-lm', '-lpthread'
    }
    
    onlyIf {
        def hasHeaders = file('main/llama.h').exists()
        def hasSource = file('main/llama_jni.cpp').exists()
        def needsRebuild = !file(outputLib).exists() || 
                          file('main/llama_jni.cpp').lastModified() > file(outputLib).lastModified()
        
        if (!hasHeaders) {
            logger.error("llama.h not found in main/")
            logger.error("Install llama.cpp and copy headers to main/")
        }
        
        return hasHeaders && hasSource && needsRebuild
    }
}

// JAR packaging with native libraries
jar {
    from('bin') {
        include '*.so', '*.dylib', '*.dll'
        into 'native'
    }
}
```

### **Runtime Library Loading Enhancement**

Update `LlamaJNI.java` for better library loading:

```java
static {
    try {
        // Try loading from system library path first
        System.loadLibrary("llama");
        libraryLoaded = true;
    } catch (UnsatisfiedLinkError e1) {
        try {
            // Try loading from JAR embedded native libraries
            String osName = System.getProperty("os.name").toLowerCase();
            String libName = osName.contains("windows") ? "llama.dll" :
                           osName.contains("mac") ? "libllama.dylib" :
                           "libllama.so";
            
            // Extract from JAR to temp directory and load
            File tempLib = extractNativeLibrary("/native/" + libName);
            System.load(tempLib.getAbsolutePath());
            libraryLoaded = true;
        } catch (Exception e2) {
            libraryLoaded = false;
            loadError = e1.getMessage() + "; " + e2.getMessage();
        }
    }
}
```

## 📊 **Configuration Verification Checklist**

### **Prerequisites**
- [ ] Java 17+ installed (`java -version`)
- [ ] GCC/G++ available (`g++ --version`)
- [ ] llama.cpp installed or built from source
- [ ] Headers available in `main/` directory

### **Build Verification**
- [ ] JNI headers generated (`main/com_example_LlamaJNI.h` exists)
- [ ] Native library compiled (`bin/libllama.so` exists)
- [ ] Library has correct permissions (`chmod +x`)
- [ ] Dependencies satisfied (`ldd bin/libllama.so`)

### **Runtime Verification**
- [ ] Library path configured (`java.library.path` includes `bin`)
- [ ] Environment variables set (`LD_LIBRARY_PATH` on Linux)
- [ ] Java can find library (`java -cp ... LlamaJNI` works)
- [ ] Application loads successfully

## 🎯 **Quick Setup Script**

```bash
#!/bin/bash
# quick_llama_setup.sh

set -e

echo "Setting up Llama.cpp native library for Agent-Orange..."

# Step 1: Install llama.cpp
if [ ! -d "llama.cpp" ]; then
    git clone https://github.com/ggerganov/llama.cpp.git
    cd llama.cpp
    make clean && make -j$(nproc)
    cd ..
fi

# Step 2: Copy headers
cp llama.cpp/llama.h main/
cp llama.cpp/ggml.h main/

# Step 3: Compile native library
./build.sh native

# Step 4: Verify setup
if [ -f "bin/libllama.so" ]; then
    echo "✅ Native library compiled successfully"
    ls -la bin/libllama.*
else
    echo "❌ Native library compilation failed"
    exit 1
fi

# Step 5: Test loading
export LD_LIBRARY_PATH=$PWD/bin:$LD_LIBRARY_PATH
echo "Testing library loading..."
java -cp bin/chatbot.jar com.example.LlamaJNI

echo "✅ Llama.cpp setup completed successfully!"
echo "Run with: LD_LIBRARY_PATH=bin java -jar bin/chatbot.jar"
```

## ✅ **Success Indicators**

When everything is working correctly:

1. **Compilation Success**
   ```
   > Task :compileJNI
   BUILD SUCCESSFUL
   ```

2. **Library Exists**
   ```bash
   $ ls -la bin/
   -rwxr-xr-x libllama.so
   ```

3. **Loading Success**
   ```
   [LlamaJNI] Native library loaded successfully
   INFO: LLM module initialized: Available
   ```

4. **Application Ready**
   ```
   Agent-Orange> generate text about cybersecurity
   [Generated text using Llama.cpp...]
   ```

**The key is ensuring llama.cpp is properly installed and headers are available before attempting to compile the native library!**