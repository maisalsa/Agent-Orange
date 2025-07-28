package com.example;

import java.lang.management.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.File;

/**
 * Performance monitoring and optimization system for Agent-Orange.
 * 
 * Monitors system resources, application performance, and provides
 * optimization recommendations for resource-constrained environments.
 * 
 * Key features:
 * - Real-time memory, CPU, and disk monitoring
 * - Performance bottleneck detection
 * - Automatic optimization triggers
 * - Resource usage alerts
 * - Adaptive performance tuning
 * 
 * @author Agent-Orange Team
 * @version 1.0
 */
public class PerformanceMonitor {
    
    private static final Logger logger = Logger.getLogger(PerformanceMonitor.class.getName());
    
    // JMX beans for system monitoring
    private final MemoryMXBean memoryBean;
    private final OperatingSystemMXBean osBean;
    private final RuntimeMXBean runtimeBean;
    private final ThreadMXBean threadBean;
    private final GarbageCollectorMXBean[] gcBeans;
    
    // Monitoring configuration
    private final ScheduledExecutorService monitorExecutor;
    private volatile boolean monitoringActive = false;
    private final AtomicLong monitoringCycles = new AtomicLong(0);
    
    // Performance metrics
    private final AtomicLong totalMemoryUsed = new AtomicLong(0);
    private final AtomicLong peakMemoryUsed = new AtomicLong(0);
    private final AtomicDouble averageCpuUsage = new AtomicDouble(0.0);
    private final AtomicLong totalGcTime = new AtomicLong(0);
    private final AtomicInteger activeThreads = new AtomicInteger(0);
    
    // Optimization callbacks
    private final List<OptimizationCallback> optimizationCallbacks;
    private final Map<String, Object> performanceHistory;
    
    // Thresholds for optimization triggers
    private static final double HIGH_MEMORY_THRESHOLD = 0.85; // 85% memory usage
    private static final double HIGH_CPU_THRESHOLD = 0.80;    // 80% CPU usage
    private static final long HIGH_GC_TIME_THRESHOLD = 1000;  // 1 second GC time
    private static final int HIGH_THREAD_COUNT = 50;          // 50 active threads
    
    // Monitoring intervals
    private static final long MONITORING_INTERVAL_MS = 30000; // 30 seconds
    private static final long OPTIMIZATION_COOLDOWN_MS = 300000; // 5 minutes
    
    // Optimization state
    private volatile long lastOptimizationTime = 0;
    private final AtomicBoolean optimizationInProgress = new AtomicBoolean(false);
    
    /**
     * Callback interface for optimization actions.
     */
    public interface OptimizationCallback {
        /**
         * Called when performance optimization is triggered.
         * 
         * @param reason The reason for optimization
         * @param metrics Current performance metrics
         */
        void onOptimizationTriggered(String reason, Map<String, Object> metrics);
    }
    
    /**
     * Performance metrics snapshot.
     */
    public static class PerformanceMetrics {
        public final long timestamp;
        public final long usedMemoryMB;
        public final long maxMemoryMB;
        public final double memoryUsagePercent;
        public final double cpuUsagePercent;
        public final long gcTimeMs;
        public final int threadCount;
        public final long freeMemoryMB;
        public final long totalMemoryMB;
        public final double systemLoadAverage;
        public final long availableProcessors;
        
        public PerformanceMetrics(long timestamp, long usedMemoryMB, long maxMemoryMB,
                                double memoryUsagePercent, double cpuUsagePercent,
                                long gcTimeMs, int threadCount, long freeMemoryMB,
                                long totalMemoryMB, double systemLoadAverage,
                                long availableProcessors) {
            this.timestamp = timestamp;
            this.usedMemoryMB = usedMemoryMB;
            this.maxMemoryMB = maxMemoryMB;
            this.memoryUsagePercent = memoryUsagePercent;
            this.cpuUsagePercent = cpuUsagePercent;
            this.gcTimeMs = gcTimeMs;
            this.threadCount = threadCount;
            this.freeMemoryMB = freeMemoryMB;
            this.totalMemoryMB = totalMemoryMB;
            this.systemLoadAverage = systemLoadAverage;
            this.availableProcessors = availableProcessors;
        }
        
        @Override
        public String toString() {
            return String.format("PerformanceMetrics{memory=%.1f%% (%dMB/%dMB), cpu=%.1f%%, gc=%dms, threads=%d, load=%.2f}",
                               memoryUsagePercent, usedMemoryMB, maxMemoryMB, cpuUsagePercent, 
                               gcTimeMs, threadCount, systemLoadAverage);
        }
    }
    
    /**
     * Constructs a new PerformanceMonitor.
     */
    public PerformanceMonitor() {
        // Initialize JMX beans
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
        this.runtimeBean = ManagementFactory.getRuntimeMXBean();
        this.threadBean = ManagementFactory.getThreadMXBean();
        this.gcBeans = ManagementFactory.getGarbageCollectorMXBeans().toArray(new GarbageCollectorMXBean[0]);
        
        // Initialize executor
        this.monitorExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "PerformanceMonitor");
            t.setDaemon(true);
            return t;
        });
        
        // Initialize collections
        this.optimizationCallbacks = new CopyOnWriteArrayList<>();
        this.performanceHistory = new ConcurrentHashMap<>();
        
        logger.info("PerformanceMonitor initialized");
    }
    
    /**
     * Starts performance monitoring.
     */
    public void startMonitoring() {
        if (!monitoringActive) {
            monitoringActive = true;
            
            monitorExecutor.scheduleAtFixedRate(this::performMonitoringCycle,
                                              0, MONITORING_INTERVAL_MS, TimeUnit.MILLISECONDS);
            
            logger.info("Performance monitoring started (interval: " + MONITORING_INTERVAL_MS + "ms)");
        }
    }
    
    /**
     * Stops performance monitoring.
     */
    public void stopMonitoring() {
        if (monitoringActive) {
            monitoringActive = false;
            logger.info("Performance monitoring stopped");
        }
    }
    
    /**
     * Adds an optimization callback.
     * 
     * @param callback The callback to add
     */
    public void addOptimizationCallback(OptimizationCallback callback) {
        if (callback != null) {
            optimizationCallbacks.add(callback);
        }
    }
    
    /**
     * Removes an optimization callback.
     * 
     * @param callback The callback to remove
     */
    public void removeOptimizationCallback(OptimizationCallback callback) {
        optimizationCallbacks.remove(callback);
    }
    
    /**
     * Gets current performance metrics.
     * 
     * @return Current performance metrics
     */
    public PerformanceMetrics getCurrentMetrics() {
        long timestamp = System.currentTimeMillis();
        
        // Memory metrics
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        long usedMemoryMB = heapUsage.getUsed() / (1024 * 1024);
        long maxMemoryMB = heapUsage.getMax() / (1024 * 1024);
        long freeMemoryMB = (heapUsage.getMax() - heapUsage.getUsed()) / (1024 * 1024);
        long totalMemoryMB = heapUsage.getCommitted() / (1024 * 1024);
        double memoryUsagePercent = (double) heapUsage.getUsed() / heapUsage.getMax() * 100;
        
        // CPU metrics
        double cpuUsagePercent = 0.0;
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            com.sun.management.OperatingSystemMXBean sunOsBean = 
                (com.sun.management.OperatingSystemMXBean) osBean;
            cpuUsagePercent = sunOsBean.getProcessCpuLoad() * 100;
        }
        
        // GC metrics
        long totalGcTimeMs = 0;
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            long gcTime = gcBean.getCollectionTime();
            if (gcTime > 0) {
                totalGcTimeMs += gcTime;
            }
        }
        
        // Thread metrics
        int threadCount = threadBean.getThreadCount();
        
        // System metrics
        double systemLoadAverage = osBean.getSystemLoadAverage();
        long availableProcessors = osBean.getAvailableProcessors();
        
        return new PerformanceMetrics(timestamp, usedMemoryMB, maxMemoryMB, memoryUsagePercent,
                                    cpuUsagePercent, totalGcTimeMs, threadCount, freeMemoryMB,
                                    totalMemoryMB, systemLoadAverage, availableProcessors);
    }
    
    /**
     * Performs a monitoring cycle.
     */
    private void performMonitoringCycle() {
        try {
            long cycleNumber = monitoringCycles.incrementAndGet();
            PerformanceMetrics metrics = getCurrentMetrics();
            
            // Update running statistics
            updateStatistics(metrics);
            
            // Store in history (keep last 100 entries)
            performanceHistory.put("cycle_" + cycleNumber, metrics);
            if (performanceHistory.size() > 100) {
                // Remove oldest entry
                String oldestKey = "cycle_" + (cycleNumber - 100);
                performanceHistory.remove(oldestKey);
            }
            
            // Check for optimization triggers
            checkOptimizationTriggers(metrics);
            
            // Log performance summary periodically
            if (cycleNumber % 10 == 0) { // Every 10 cycles (5 minutes)
                logPerformanceSummary(metrics);
            }
            
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error during monitoring cycle", e);
        }
    }
    
    /**
     * Updates running performance statistics.
     */
    private void updateStatistics(PerformanceMetrics metrics) {
        // Update memory statistics
        totalMemoryUsed.addAndGet(metrics.usedMemoryMB);
        peakMemoryUsed.updateAndGet(current -> Math.max(current, metrics.usedMemoryMB));
        
        // Update CPU statistics (running average)
        long cycles = monitoringCycles.get();
        double currentAvg = averageCpuUsage.get();
        double newAvg = (currentAvg * (cycles - 1) + metrics.cpuUsagePercent) / cycles;
        averageCpuUsage.set(newAvg);
        
        // Update GC statistics
        totalGcTime.set(metrics.gcTimeMs);
        
        // Update thread statistics
        activeThreads.set(metrics.threadCount);
    }
    
    /**
     * Checks for conditions that trigger optimization.
     */
    private void checkOptimizationTriggers(PerformanceMetrics metrics) {
        long currentTime = System.currentTimeMillis();
        
        // Check cooldown period
        if (currentTime - lastOptimizationTime < OPTIMIZATION_COOLDOWN_MS) {
            return;
        }
        
        // Check if optimization is already in progress
        if (optimizationInProgress.get()) {
            return;
        }
        
        List<String> reasons = new ArrayList<>();
        
        // Check memory threshold
        if (metrics.memoryUsagePercent > HIGH_MEMORY_THRESHOLD * 100) {
            reasons.add("High memory usage: " + String.format("%.1f%%", metrics.memoryUsagePercent));
        }
        
        // Check CPU threshold
        if (metrics.cpuUsagePercent > HIGH_CPU_THRESHOLD * 100) {
            reasons.add("High CPU usage: " + String.format("%.1f%%", metrics.cpuUsagePercent));
        }
        
        // Check GC time threshold
        long recentGcTime = metrics.gcTimeMs - totalGcTime.get();
        if (recentGcTime > HIGH_GC_TIME_THRESHOLD) {
            reasons.add("High GC time: " + recentGcTime + "ms");
        }
        
        // Check thread count threshold
        if (metrics.threadCount > HIGH_THREAD_COUNT) {
            reasons.add("High thread count: " + metrics.threadCount);
        }
        
        // Trigger optimization if any thresholds exceeded
        if (!reasons.isEmpty()) {
            triggerOptimization(String.join(", ", reasons), metrics);
        }
    }
    
    /**
     * Triggers performance optimization.
     */
    private void triggerOptimization(String reason, PerformanceMetrics metrics) {
        if (optimizationInProgress.compareAndSet(false, true)) {
            try {
                logger.info("Triggering performance optimization: " + reason);
                
                // Create metrics map for callbacks
                Map<String, Object> metricsMap = createMetricsMap(metrics);
                
                // Notify callbacks
                for (OptimizationCallback callback : optimizationCallbacks) {
                    try {
                        callback.onOptimizationTriggered(reason, metricsMap);
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Error in optimization callback", e);
                    }
                }
                
                // Perform built-in optimizations
                performBuiltInOptimizations(metrics);
                
                lastOptimizationTime = System.currentTimeMillis();
                
            } finally {
                optimizationInProgress.set(false);
            }
        }
    }
    
    /**
     * Performs built-in optimization actions.
     */
    private void performBuiltInOptimizations(PerformanceMetrics metrics) {
        logger.info("Performing built-in optimizations");
        
        // Suggest garbage collection if memory usage is high
        if (metrics.memoryUsagePercent > HIGH_MEMORY_THRESHOLD * 100) {
            logger.info("Suggesting garbage collection due to high memory usage");
            System.gc();
        }
        
        // Log optimization recommendations
        generateOptimizationRecommendations(metrics);
    }
    
    /**
     * Generates optimization recommendations.
     */
    private void generateOptimizationRecommendations(PerformanceMetrics metrics) {
        List<String> recommendations = new ArrayList<>();
        
        if (metrics.memoryUsagePercent > HIGH_MEMORY_THRESHOLD * 100) {
            recommendations.add("Consider increasing JVM heap size or optimizing memory usage");
            recommendations.add("Review cached data and consider implementing weak references");
        }
        
        if (metrics.cpuUsagePercent > HIGH_CPU_THRESHOLD * 100) {
            recommendations.add("Consider reducing concurrent operations or optimizing algorithms");
            recommendations.add("Review thread pool sizes and consider async processing");
        }
        
        if (metrics.threadCount > HIGH_THREAD_COUNT) {
            recommendations.add("Consider reducing thread pool sizes or using shared executors");
        }
        
        if (metrics.systemLoadAverage > metrics.availableProcessors * 1.5) {
            recommendations.add("System load is high, consider reducing concurrent operations");
        }
        
        if (!recommendations.isEmpty()) {
            logger.info("Optimization recommendations:");
            for (String recommendation : recommendations) {
                logger.info("  - " + recommendation);
            }
        }
    }
    
    /**
     * Creates a metrics map for callbacks.
     */
    private Map<String, Object> createMetricsMap(PerformanceMetrics metrics) {
        Map<String, Object> map = new HashMap<>();
        map.put("timestamp", metrics.timestamp);
        map.put("usedMemoryMB", metrics.usedMemoryMB);
        map.put("maxMemoryMB", metrics.maxMemoryMB);
        map.put("memoryUsagePercent", metrics.memoryUsagePercent);
        map.put("cpuUsagePercent", metrics.cpuUsagePercent);
        map.put("gcTimeMs", metrics.gcTimeMs);
        map.put("threadCount", metrics.threadCount);
        map.put("systemLoadAverage", metrics.systemLoadAverage);
        map.put("availableProcessors", metrics.availableProcessors);
        return map;
    }
    
    /**
     * Logs performance summary.
     */
    private void logPerformanceSummary(PerformanceMetrics metrics) {
        logger.info(String.format("Performance Summary - Memory: %.1f%% (%dMB), CPU: %.1f%%, Threads: %d, Load: %.2f",
                                 metrics.memoryUsagePercent, metrics.usedMemoryMB, 
                                 metrics.cpuUsagePercent, metrics.threadCount, metrics.systemLoadAverage));
    }
    
    /**
     * Gets performance statistics.
     * 
     * @return Map containing performance statistics
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("monitoringActive", monitoringActive);
        stats.put("monitoringCycles", monitoringCycles.get());
        stats.put("peakMemoryUsedMB", peakMemoryUsed.get());
        stats.put("averageCpuUsage", averageCpuUsage.get());
        stats.put("totalGcTimeMs", totalGcTime.get());
        stats.put("activeThreads", activeThreads.get());
        stats.put("lastOptimizationTime", lastOptimizationTime);
        stats.put("optimizationCallbacks", optimizationCallbacks.size());
        
        // Add current metrics
        PerformanceMetrics current = getCurrentMetrics();
        stats.put("currentMetrics", createMetricsMap(current));
        
        return stats;
    }
    
    /**
     * Gets performance history.
     * 
     * @return Map containing historical performance data
     */
    public Map<String, Object> getPerformanceHistory() {
        return new HashMap<>(performanceHistory);
    }
    
    /**
     * Forces a performance optimization check.
     */
    public void forceOptimizationCheck() {
        logger.info("Forcing optimization check");
        PerformanceMetrics metrics = getCurrentMetrics();
        
        // Temporarily reset cooldown to allow immediate optimization
        long originalTime = lastOptimizationTime;
        lastOptimizationTime = 0;
        
        checkOptimizationTriggers(metrics);
        
        // Restore original time if no optimization was triggered
        if (lastOptimizationTime == 0) {
            lastOptimizationTime = originalTime;
        }
    }
    
    /**
     * Gets system information for optimization planning.
     * 
     * @return Map containing system information
     */
    public Map<String, Object> getSystemInfo() {
        Map<String, Object> info = new HashMap<>();
        
        // JVM information
        info.put("javaVersion", System.getProperty("java.version"));
        info.put("javaVendor", System.getProperty("java.vendor"));
        info.put("jvmName", runtimeBean.getVmName());
        info.put("jvmVersion", runtimeBean.getVmVersion());
        
        // Operating system information
        info.put("osName", osBean.getName());
        info.put("osArch", osBean.getArch());
        info.put("osVersion", osBean.getVersion());
        info.put("availableProcessors", osBean.getAvailableProcessors());
        
        // Memory information
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapUsage = memoryBean.getNonHeapMemoryUsage();
        info.put("heapMemoryMB", heapUsage.getMax() / (1024 * 1024));
        info.put("nonHeapMemoryMB", nonHeapUsage.getMax() / (1024 * 1024));
        
        // Disk information
        File root = new File("/");
        info.put("diskTotalGB", root.getTotalSpace() / (1024 * 1024 * 1024));
        info.put("diskFreeGB", root.getFreeSpace() / (1024 * 1024 * 1024));
        info.put("diskUsableGB", root.getUsableSpace() / (1024 * 1024 * 1024));
        
        return info;
    }
    
    /**
     * Shuts down the performance monitor.
     */
    public void shutdown() {
        logger.info("Shutting down PerformanceMonitor");
        
        stopMonitoring();
        
        monitorExecutor.shutdown();
        try {
            if (!monitorExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                monitorExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            monitorExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        logger.info("PerformanceMonitor shutdown complete");
    }
    
    /**
     * Creates a performance optimization report.
     * 
     * @return Formatted performance report
     */
    public String generateOptimizationReport() {
        StringBuilder report = new StringBuilder();
        PerformanceMetrics metrics = getCurrentMetrics();
        Map<String, Object> stats = getStatistics();
        Map<String, Object> systemInfo = getSystemInfo();
        
        report.append("=== Agent-Orange Performance Optimization Report ===\n\n");
        
        // Current metrics
        report.append("Current Performance Metrics:\n");
        report.append(String.format("  Memory Usage: %.1f%% (%d MB / %d MB)\n", 
                                   metrics.memoryUsagePercent, metrics.usedMemoryMB, metrics.maxMemoryMB));
        report.append(String.format("  CPU Usage: %.1f%%\n", metrics.cpuUsagePercent));
        report.append(String.format("  Thread Count: %d\n", metrics.threadCount));
        report.append(String.format("  GC Time: %d ms\n", metrics.gcTimeMs));
        report.append(String.format("  System Load: %.2f (processors: %d)\n", 
                                   metrics.systemLoadAverage, metrics.availableProcessors));
        report.append("\n");
        
        // System information
        report.append("System Information:\n");
        report.append(String.format("  OS: %s %s (%s)\n", 
                                   systemInfo.get("osName"), systemInfo.get("osVersion"), systemInfo.get("osArch")));
        report.append(String.format("  Java: %s (%s)\n", 
                                   systemInfo.get("javaVersion"), systemInfo.get("javaVendor")));
        report.append(String.format("  JVM: %s %s\n", 
                                   systemInfo.get("jvmName"), systemInfo.get("jvmVersion")));
        report.append(String.format("  Heap Memory: %d MB\n", systemInfo.get("heapMemoryMB")));
        report.append(String.format("  Disk Space: %d GB total, %d GB free\n", 
                                   systemInfo.get("diskTotalGB"), systemInfo.get("diskFreeGB")));
        report.append("\n");
        
        // Performance recommendations
        report.append("Optimization Recommendations:\n");
        List<String> recommendations = generateRecommendationsForReport(metrics, systemInfo);
        if (recommendations.isEmpty()) {
            report.append("  No specific optimizations needed at this time.\n");
        } else {
            for (String recommendation : recommendations) {
                report.append("  - ").append(recommendation).append("\n");
            }
        }
        
        return report.toString();
    }
    
    /**
     * Generates recommendations for the optimization report.
     */
    private List<String> generateRecommendationsForReport(PerformanceMetrics metrics, Map<String, Object> systemInfo) {
        List<String> recommendations = new ArrayList<>();
        
        // Memory recommendations
        if (metrics.memoryUsagePercent > 70) {
            recommendations.add("Memory usage is high. Consider optimizing data structures or increasing heap size.");
        }
        
        if (metrics.memoryUsagePercent > 90) {
            recommendations.add("Critical memory usage. Immediate optimization or heap increase required.");
        }
        
        // CPU recommendations
        if (metrics.cpuUsagePercent > 60) {
            recommendations.add("CPU usage is elevated. Consider optimizing algorithms or reducing concurrent operations.");
        }
        
        // Thread recommendations
        if (metrics.threadCount > 30) {
            recommendations.add("High thread count detected. Consider using thread pools or async processing.");
        }
        
        // System load recommendations
        if (metrics.systemLoadAverage > metrics.availableProcessors) {
            recommendations.add("System load is high. Consider reducing workload or optimizing I/O operations.");
        }
        
        // Disk space recommendations
        Long diskFreeGB = (Long) systemInfo.get("diskFreeGB");
        if (diskFreeGB != null && diskFreeGB < 5) {
            recommendations.add("Low disk space detected. Consider cleaning up temporary files or logs.");
        }
        
        return recommendations;
    }
}