package com.example;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.lang.ref.WeakReference;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Memory and performance optimized LLM service for resource-constrained systems.
 * 
 * Key optimizations:
 * - Model pre-loading and persistent caching
 * - Request batching for improved throughput
 * - Context window management and reuse
 * - Quantized model support for memory efficiency
 * - Asynchronous inference with thread pooling
 * - Automatic garbage collection of old contexts
 * - Resource monitoring and adaptive throttling
 * 
 * @author Agent-Orange Team
 * @version 2.0
 */
public class OptimizedLLMService {
    
    private static final Logger logger = Logger.getLogger(OptimizedLLMService.class.getName());
    
    // Core configuration
    private final LlamaJNI llamaJNI;
    private final ExecutorService inferenceExecutor;
    private final ScheduledExecutorService maintenanceExecutor;
    
    // Model management
    private volatile boolean modelLoaded = false;
    private volatile String currentModelPath;
    private final AtomicLong totalInferences = new AtomicLong(0);
    private final AtomicLong totalTokensProcessed = new AtomicLong(0);
    
    // Context management for efficiency
    private final Map<String, ContextInfo> contextCache;
    private final Queue<InferenceRequest> requestQueue;
    private final AtomicBoolean batchProcessingActive = new AtomicBoolean(false);
    
    // Performance optimization settings
    private static final int MAX_CONTEXT_CACHE_SIZE = 10;
    private static final int CONTEXT_TTL_MS = 900000; // 15 minutes
    private static final int BATCH_SIZE = 4;
    private static final long BATCH_TIMEOUT_MS = 100; // 100ms
    private static final int MAX_CONTEXT_LENGTH = 2048;
    private static final int INFERENCE_TIMEOUT_MS = 30000; // 30 seconds
    
    // Memory management
    private final AtomicLong peakMemoryUsage = new AtomicLong(0);
    private volatile boolean lowMemoryMode = false;
    private static final long MEMORY_THRESHOLD_MB = 2048; // 2GB threshold
    
    /**
     * Context information for reuse and caching.
     */
    private static class ContextInfo {
        final String contextId;
        final String systemPrompt;
        final List<String> conversationHistory;
        final long createdAt;
        volatile long lastUsed;
        volatile int usageCount;
        
        ContextInfo(String contextId, String systemPrompt) {
            this.contextId = contextId;
            this.systemPrompt = systemPrompt != null ? systemPrompt : "";
            this.conversationHistory = new ArrayList<>();
            this.createdAt = System.currentTimeMillis();
            this.lastUsed = this.createdAt;
            this.usageCount = 0;
        }
        
        synchronized void addToHistory(String input, String response) {
            conversationHistory.add("User: " + input);
            conversationHistory.add("Assistant: " + response);
            
            // Trim history if too long
            while (getContextLength() > MAX_CONTEXT_LENGTH && conversationHistory.size() > 2) {
                conversationHistory.remove(0);
                conversationHistory.remove(0);
            }
            
            this.lastUsed = System.currentTimeMillis();
            this.usageCount++;
        }
        
        int getContextLength() {
            return systemPrompt.length() + 
                   conversationHistory.stream().mapToInt(String::length).sum();
        }
        
        String buildFullContext() {
            StringBuilder context = new StringBuilder();
            if (!systemPrompt.isEmpty()) {
                context.append("System: ").append(systemPrompt).append("\n\n");
            }
            
            for (String message : conversationHistory) {
                context.append(message).append("\n");
            }
            
            return context.toString();
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - lastUsed > CONTEXT_TTL_MS;
        }
    }
    
    /**
     * Inference request for batching.
     */
    private static class InferenceRequest {
        final String input;
        final String contextId;
        final CompletableFuture<String> future;
        final long timestamp;
        final Map<String, Object> options;
        
        InferenceRequest(String input, String contextId, Map<String, Object> options) {
            this.input = input;
            this.contextId = contextId;
            this.future = new CompletableFuture<>();
            this.timestamp = System.currentTimeMillis();
            this.options = options != null ? new HashMap<>(options) : new HashMap<>();
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > INFERENCE_TIMEOUT_MS;
        }
    }
    
    /**
     * Constructs a new optimized LLM service.
     */
    public OptimizedLLMService() {
        this(new LlamaJNI());
    }
    
    /**
     * Constructs a new optimized LLM service with existing LlamaJNI instance.
     * 
     * @param llamaJNI The LlamaJNI instance to use
     */
    public OptimizedLLMService(LlamaJNI llamaJNI) {
        this.llamaJNI = llamaJNI;
        
        // Initialize thread pools with optimal sizing for i7-7700T (4 cores, 8 threads)
        this.inferenceExecutor = Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "LLM-Inference");
            t.setDaemon(true);
            t.setPriority(Thread.NORM_PRIORITY + 1); // Slightly higher priority
            return t;
        });
        
        this.maintenanceExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "LLM-Maintenance");
            t.setDaemon(true);
            return t;
        });
        
        // Initialize data structures
        this.contextCache = new ConcurrentHashMap<>();
        this.requestQueue = new ConcurrentLinkedQueue<>();
        
        // Schedule maintenance tasks
        scheduleMaintenance();
        
        logger.info("OptimizedLLMService initialized");
    }
    
    /**
     * Loads a model with optimizations for resource-constrained systems.
     * 
     * @param modelPath Path to the model file
     * @param options Loading options (quantization, context size, etc.)
     * @return true if model loaded successfully
     */
    public boolean loadModel(String modelPath, Map<String, Object> options) {
        if (modelPath == null || modelPath.trim().isEmpty()) {
            logger.warning("Model path cannot be null or empty");
            return false;
        }
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Loading model: " + modelPath);
                
                // Check available memory before loading
                long availableMemory = getAvailableMemoryMB();
                if (availableMemory < MEMORY_THRESHOLD_MB) {
                    logger.warning("Low memory detected (" + availableMemory + "MB), enabling low memory mode");
                    lowMemoryMode = true;
                }
                
                // Configure loading options for optimization
                Map<String, Object> loadOptions = new HashMap<>();
                if (options != null) {
                    loadOptions.putAll(options);
                }
                
                // Apply memory-optimized defaults
                loadOptions.putIfAbsent("context_length", lowMemoryMode ? 1024 : MAX_CONTEXT_LENGTH);
                loadOptions.putIfAbsent("batch_size", lowMemoryMode ? 1 : BATCH_SIZE);
                loadOptions.putIfAbsent("threads", Math.min(4, Runtime.getRuntime().availableProcessors()));
                loadOptions.putIfAbsent("mmap", true); // Memory mapping for efficiency
                loadOptions.putIfAbsent("mlock", false); // Don't lock memory on constrained systems
                
                // Load model via JNI
                boolean success = llamaJNI.loadModel(modelPath, loadOptions);
                
                if (success) {
                    this.currentModelPath = modelPath;
                    this.modelLoaded = true;
                    
                    // Clear context cache when switching models
                    contextCache.clear();
                    
                    logger.info("Model loaded successfully: " + modelPath);
                } else {
                    logger.severe("Failed to load model: " + modelPath);
                }
                
                return success;
                
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error loading model: " + modelPath, e);
                return false;
            }
        }, inferenceExecutor).join();
    }
    
    /**
     * Performs inference with context management and batching optimization.
     * 
     * @param input The input text
     * @param contextId Optional context ID for conversation continuity
     * @param options Inference options
     * @return CompletableFuture with the generated response
     */
    public CompletableFuture<String> generateAsync(String input, String contextId, Map<String, Object> options) {
        if (!modelLoaded) {
            CompletableFuture<String> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(new IllegalStateException("No model loaded"));
            return failedFuture;
        }
        
        if (input == null || input.trim().isEmpty()) {
            CompletableFuture<String> emptyFuture = new CompletableFuture<>();
            emptyFuture.complete("");
            return emptyFuture;
        }
        
        // Create inference request
        InferenceRequest request = new InferenceRequest(input.trim(), contextId, options);
        
        // Add to batch queue
        requestQueue.offer(request);
        
        // Trigger batch processing if not already active
        triggerBatchProcessing();
        
        return request.future;
    }
    
    /**
     * Synchronous inference method for backward compatibility.
     * 
     * @param input The input text
     * @param contextId Optional context ID
     * @param options Inference options
     * @return Generated response
     */
    public String generate(String input, String contextId, Map<String, Object> options) {
        try {
            return generateAsync(input, contextId, options).get(INFERENCE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            logger.warning("Inference timeout for input: " + input.substring(0, Math.min(50, input.length())));
            return "Error: Inference timeout";
        } catch (Exception e) {
            logger.log(Level.WARNING, "Inference error", e);
            return "Error: " + e.getMessage();
        }
    }
    
    /**
     * Simple inference without context management.
     * 
     * @param input The input text
     * @return Generated response
     */
    public String generate(String input) {
        return generate(input, null, null);
    }
    
    /**
     * Creates or gets a conversation context.
     * 
     * @param contextId Unique context identifier
     * @param systemPrompt Optional system prompt
     * @return The context info
     */
    public ContextInfo createContext(String contextId, String systemPrompt) {
        if (contextId == null || contextId.trim().isEmpty()) {
            contextId = "ctx_" + System.currentTimeMillis();
        }
        
        ContextInfo context = new ContextInfo(contextId, systemPrompt);
        
        // Manage cache size
        if (contextCache.size() >= MAX_CONTEXT_CACHE_SIZE) {
            evictOldestContext();
        }
        
        contextCache.put(contextId, context);
        logger.fine("Created context: " + contextId);
        
        return context;
    }
    
    /**
     * Gets an existing context.
     * 
     * @param contextId The context identifier
     * @return The context info, or null if not found
     */
    public ContextInfo getContext(String contextId) {
        if (contextId == null) return null;
        
        ContextInfo context = contextCache.get(contextId);
        if (context != null && context.isExpired()) {
            contextCache.remove(contextId);
            return null;
        }
        
        return context;
    }
    
    /**
     * Removes a context from cache.
     * 
     * @param contextId The context identifier
     */
    public void removeContext(String contextId) {
        if (contextId != null) {
            contextCache.remove(contextId);
            logger.fine("Removed context: " + contextId);
        }
    }
    
    /**
     * Triggers batch processing of inference requests.
     */
    private void triggerBatchProcessing() {
        if (batchProcessingActive.compareAndSet(false, true)) {
            CompletableFuture.runAsync(() -> {
                try {
                    processBatch();
                } finally {
                    batchProcessingActive.set(false);
                    
                    // Check if more requests arrived
                    if (!requestQueue.isEmpty()) {
                        triggerBatchProcessing();
                    }
                }
            }, inferenceExecutor);
        }
    }
    
    /**
     * Processes a batch of inference requests.
     */
    private void processBatch() {
        List<InferenceRequest> batch = new ArrayList<>();
        long batchStart = System.currentTimeMillis();
        
        // Collect requests for batch
        while (batch.size() < BATCH_SIZE && 
               (System.currentTimeMillis() - batchStart < BATCH_TIMEOUT_MS || batch.isEmpty())) {
            
            InferenceRequest request = requestQueue.poll();
            if (request != null) {
                if (request.isExpired()) {
                    request.future.completeExceptionally(new TimeoutException("Request expired"));
                } else {
                    batch.add(request);
                }
            } else if (batch.isEmpty()) {
                // Wait a bit for requests
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            } else {
                break; // Process collected batch
            }
        }
        
        if (batch.isEmpty()) {
            return;
        }
        
        logger.fine("Processing batch of " + batch.size() + " requests");
        
        // Process each request in the batch
        for (InferenceRequest request : batch) {
            try {
                String response = processInferenceRequest(request);
                request.future.complete(response);
                
                // Update statistics
                totalInferences.incrementAndGet();
                totalTokensProcessed.addAndGet(request.input.length() + response.length());
                
            } catch (Exception e) {
                request.future.completeExceptionally(e);
                logger.log(Level.WARNING, "Error processing inference request", e);
            }
        }
    }
    
    /**
     * Processes a single inference request with context management.
     */
    private String processInferenceRequest(InferenceRequest request) {
        String contextualInput = request.input;
        ContextInfo context = null;
        
        // Handle context if specified
        if (request.contextId != null) {
            context = getContext(request.contextId);
            if (context != null) {
                contextualInput = context.buildFullContext() + "\nUser: " + request.input + "\nAssistant:";
            }
        }
        
        // Perform inference
        String response = llamaJNI.generate(contextualInput, request.options);
        
        // Update context with response
        if (context != null && response != null) {
            context.addToHistory(request.input, response);
        }
        
        return response != null ? response.trim() : "";
    }
    
    /**
     * Evicts the oldest context from cache.
     */
    private void evictOldestContext() {
        String oldestContextId = null;
        long oldestTime = Long.MAX_VALUE;
        
        for (Map.Entry<String, ContextInfo> entry : contextCache.entrySet()) {
            ContextInfo context = entry.getValue();
            if (context.lastUsed < oldestTime) {
                oldestTime = context.lastUsed;
                oldestContextId = entry.getKey();
            }
        }
        
        if (oldestContextId != null) {
            contextCache.remove(oldestContextId);
            logger.fine("Evicted oldest context: " + oldestContextId);
        }
    }
    
    /**
     * Schedules maintenance tasks.
     */
    private void scheduleMaintenance() {
        // Context cleanup every 5 minutes
        maintenanceExecutor.scheduleAtFixedRate(this::cleanupExpiredContexts, 
                                              300, 300, TimeUnit.SECONDS);
        
        // Memory monitoring every minute
        maintenanceExecutor.scheduleAtFixedRate(this::monitorMemoryUsage, 
                                              60, 60, TimeUnit.SECONDS);
        
        // Performance logging every 10 minutes
        maintenanceExecutor.scheduleAtFixedRate(this::logPerformanceStats, 
                                              600, 600, TimeUnit.SECONDS);
    }
    
    /**
     * Cleans up expired contexts.
     */
    private void cleanupExpiredContexts() {
        int removedCount = 0;
        Iterator<Map.Entry<String, ContextInfo>> iterator = contextCache.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<String, ContextInfo> entry = iterator.next();
            if (entry.getValue().isExpired()) {
                iterator.remove();
                removedCount++;
            }
        }
        
        if (removedCount > 0) {
            logger.fine("Cleaned up " + removedCount + " expired contexts");
        }
    }
    
    /**
     * Monitors memory usage and adjusts operation mode.
     */
    private void monitorMemoryUsage() {
        try {
            long currentMemory = getCurrentMemoryUsageMB();
            long availableMemory = getAvailableMemoryMB();
            
            peakMemoryUsage.updateAndGet(current -> Math.max(current, currentMemory));
            
            // Adjust low memory mode based on available memory
            boolean shouldEnableLowMemory = availableMemory < MEMORY_THRESHOLD_MB;
            if (shouldEnableLowMemory != lowMemoryMode) {
                lowMemoryMode = shouldEnableLowMemory;
                logger.info("Low memory mode " + (lowMemoryMode ? "enabled" : "disabled") + 
                           " (available: " + availableMemory + "MB)");
                
                if (lowMemoryMode) {
                    // Aggressively clean up contexts in low memory mode
                    contextCache.clear();
                    System.gc(); // Suggest garbage collection
                }
            }
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error monitoring memory usage", e);
        }
    }
    
    /**
     * Logs performance statistics.
     */
    private void logPerformanceStats() {
        long inferences = totalInferences.get();
        long tokens = totalTokensProcessed.get();
        long peakMem = peakMemoryUsage.get();
        
        logger.info(String.format("LLM Performance: %d inferences, %d tokens, %dMB peak memory, %d contexts", 
                                 inferences, tokens, peakMem, contextCache.size()));
    }
    
    /**
     * Gets current memory usage in MB.
     */
    private long getCurrentMemoryUsageMB() {
        Runtime runtime = Runtime.getRuntime();
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
    }
    
    /**
     * Gets available memory in MB.
     */
    private long getAvailableMemoryMB() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.freeMemory() / (1024 * 1024);
    }
    
    /**
     * Gets service statistics.
     * 
     * @return Map containing service statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("modelLoaded", modelLoaded);
        stats.put("currentModel", currentModelPath);
        stats.put("totalInferences", totalInferences.get());
        stats.put("totalTokensProcessed", totalTokensProcessed.get());
        stats.put("activeContexts", contextCache.size());
        stats.put("queuedRequests", requestQueue.size());
        stats.put("lowMemoryMode", lowMemoryMode);
        stats.put("peakMemoryUsageMB", peakMemoryUsage.get());
        stats.put("currentMemoryUsageMB", getCurrentMemoryUsageMB());
        stats.put("availableMemoryMB", getAvailableMemoryMB());
        
        return stats;
    }
    
    /**
     * Gets memory usage statistics.
     * 
     * @return Map containing memory usage information
     */
    public Map<String, Object> getMemoryStats() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("contextCacheSize", contextCache.size());
        stats.put("requestQueueSize", requestQueue.size());
        stats.put("lowMemoryMode", lowMemoryMode);
        stats.put("peakMemoryUsageMB", peakMemoryUsage.get());
        stats.put("currentMemoryUsageMB", getCurrentMemoryUsageMB());
        
        // Calculate context memory usage
        int totalContextLength = contextCache.values().stream()
            .mapToInt(ContextInfo::getContextLength)
            .sum();
        stats.put("totalContextLength", totalContextLength);
        
        return stats;
    }
    
    /**
     * Optimizes memory usage by cleaning up caches.
     */
    public void optimizeMemory() {
        logger.info("Optimizing LLM service memory usage");
        
        // Clear expired contexts
        cleanupExpiredContexts();
        
        // Clear request queue of expired requests
        requestQueue.removeIf(InferenceRequest::isExpired);
        
        // Suggest garbage collection
        System.gc();
        
        logger.info("Memory optimization complete");
    }
    
    /**
     * Checks if the service is ready for inference.
     * 
     * @return true if model is loaded and service is ready
     */
    public boolean isReady() {
        return modelLoaded && llamaJNI != null;
    }
    
    /**
     * Unloads the current model to free memory.
     */
    public void unloadModel() {
        if (modelLoaded) {
            try {
                llamaJNI.unloadModel();
                modelLoaded = false;
                currentModelPath = null;
                contextCache.clear();
                
                logger.info("Model unloaded successfully");
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error unloading model", e);
            }
        }
    }
    
    /**
     * Shuts down the LLM service.
     */
    public void shutdown() {
        logger.info("Shutting down OptimizedLLMService");
        
        try {
            // Complete pending requests
            while (!requestQueue.isEmpty()) {
                InferenceRequest request = requestQueue.poll();
                if (request != null) {
                    request.future.completeExceptionally(new InterruptedException("Service shutting down"));
                }
            }
            
            // Unload model
            unloadModel();
            
            // Shutdown executors
            inferenceExecutor.shutdown();
            maintenanceExecutor.shutdown();
            
            if (!inferenceExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                inferenceExecutor.shutdownNow();
            }
            
            if (!maintenanceExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                maintenanceExecutor.shutdownNow();
            }
            
            logger.info("OptimizedLLMService shutdown complete");
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error during LLM service shutdown", e);
        }
    }
}