package com.example;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.lang.ref.WeakReference;

/**
 * Memory and performance optimized Project class for resource-constrained systems.
 * 
 * Key optimizations:
 * - Uses memory-efficient data structures
 * - Implements lazy loading for expensive operations
 * - Provides object pooling for frequent allocations
 * - Uses weak references for optional caching
 * - Minimizes string allocations through interning
 * - Thread-safe for concurrent access
 * 
 * @author Agent-Orange Team
 * @version 2.0
 */
public class OptimizedProject {
    
    // Static shared resources for memory efficiency
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Map<String, String> STATUS_INTERN_POOL = new ConcurrentHashMap<>(8);
    private static final Set<String> COMMON_TAGS = Set.of("web", "network", "mobile", "api", "internal", "external");
    
    // Core immutable fields
    private final String name;
    private final String id; // UUID for fast equality/hashing
    private final LocalDateTime createdAt;
    
    // Mutable fields with optimized storage
    private volatile String description; // volatile for thread safety
    private volatile String scope;
    private volatile String createdBy;
    private volatile String notes;
    
    // Thread-safe collections for concurrent access
    private final Set<String> targets;
    private final Set<String> tags;
    
    // Lazy-loaded expensive objects
    private volatile OptimizedVulnerabilityTree vulnerabilities;
    private volatile LocalDateTime lastModified;
    private volatile String status;
    
    // Weak reference cache for computed values
    private transient WeakReference<List<Vulnerability>> vulnerabilityCache;
    private transient WeakReference<Map<String, Object>> statsCache;
    
    // Performance tracking
    private transient volatile long lastCacheUpdate = 0;
    private static final long CACHE_TTL_MS = 30000; // 30 seconds
    
    /**
     * Constructs a new optimized Project with the specified name.
     * 
     * @param name The name of the project
     * @throws IllegalArgumentException if name is null or empty
     */
    public OptimizedProject(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Project name cannot be null or empty");
        }
        
        this.name = name.trim().intern(); // Intern for memory efficiency
        this.id = UUID.randomUUID().toString().intern();
        this.createdAt = LocalDateTime.now();
        this.lastModified = this.createdAt;
        
        // Use concurrent collections for thread safety
        this.targets = ConcurrentHashMap.newKeySet(16); // Initial capacity
        this.tags = ConcurrentHashMap.newKeySet(8);
        
        // Set default status using intern pool
        this.status = internStatus("ACTIVE");
    }
    
    /**
     * Copy constructor for creating optimized project from legacy project.
     * 
     * @param legacyProject The original project to optimize
     */
    public OptimizedProject(Project legacyProject) {
        this.name = legacyProject.getName().intern();
        this.id = UUID.randomUUID().toString().intern();
        this.createdAt = legacyProject.getCreatedAt();
        this.lastModified = legacyProject.getLastModified();
        this.description = internIfCommon(legacyProject.getDescription());
        this.scope = internIfCommon(legacyProject.getScope());
        this.createdBy = internIfCommon(legacyProject.getCreatedBy());
        this.notes = legacyProject.getNotes();
        this.status = internStatus(legacyProject.getStatus());
        
        // Copy collections efficiently
        this.targets = new ConcurrentHashMap<String, Boolean>(legacyProject.getTargets().size()).keySet();
        legacyProject.getTargets().forEach(target -> this.targets.add(target.intern()));
        
        this.tags = new ConcurrentHashMap<String, Boolean>(legacyProject.getTags().size()).keySet();
        legacyProject.getTags().forEach(tag -> this.tags.add(internTag(tag)));
        
        // Convert vulnerability tree lazily
        if (legacyProject.getVulnerabilities() != null) {
            this.vulnerabilities = new OptimizedVulnerabilityTree(legacyProject.getVulnerabilities());
        }
    }
    
    /**
     * Interns status strings for memory efficiency.
     */
    private static String internStatus(String status) {
        if (status == null) return null;
        return STATUS_INTERN_POOL.computeIfAbsent(status.toUpperCase(), String::intern);
    }
    
    /**
     * Interns common tags for memory efficiency.
     */
    private static String internTag(String tag) {
        if (tag == null) return null;
        String lowerTag = tag.toLowerCase();
        return COMMON_TAGS.contains(lowerTag) ? lowerTag : tag.intern();
    }
    
    /**
     * Interns common strings if they're likely to be repeated.
     */
    private static String internIfCommon(String value) {
        if (value == null || value.length() > 100) return value;
        return value.intern();
    }
    
    /**
     * Gets the vulnerability tree, creating it lazily if needed.
     * 
     * @return The vulnerability tree
     */
    public OptimizedVulnerabilityTree getVulnerabilities() {
        if (vulnerabilities == null) {
            synchronized (this) {
                if (vulnerabilities == null) {
                    vulnerabilities = new OptimizedVulnerabilityTree(this.name);
                }
            }
        }
        return vulnerabilities;
    }
    
    /**
     * Adds a target to the project with memory optimization.
     * 
     * @param target The target to add
     * @return true if the target was added, false if it already existed
     */
    public boolean addTarget(String target) {
        if (target == null || target.trim().isEmpty()) {
            return false;
        }
        
        String optimizedTarget = target.trim().intern();
        boolean added = targets.add(optimizedTarget);
        if (added) {
            updateLastModified();
            invalidateCache();
        }
        return added;
    }
    
    /**
     * Adds a vulnerability to the project with performance optimization.
     * 
     * @param vulnerability The vulnerability to add
     */
    public void addVulnerability(Vulnerability vulnerability) {
        if (vulnerability != null) {
            getVulnerabilities().addVulnerability(vulnerability);
            updateLastModified();
            invalidateCache();
        }
    }
    
    /**
     * Gets all vulnerabilities with caching for performance.
     * 
     * @return List of vulnerabilities (cached result when possible)
     */
    public List<Vulnerability> getAllVulnerabilities() {
        long currentTime = System.currentTimeMillis();
        
        // Check cache validity
        if (vulnerabilityCache != null && currentTime - lastCacheUpdate < CACHE_TTL_MS) {
            List<Vulnerability> cached = vulnerabilityCache.get();
            if (cached != null) {
                return new ArrayList<>(cached); // Return defensive copy
            }
        }
        
        // Rebuild cache
        List<Vulnerability> vulnerabilities = getVulnerabilities().getAllVulnerabilities();
        this.vulnerabilityCache = new WeakReference<>(new ArrayList<>(vulnerabilities));
        this.lastCacheUpdate = currentTime;
        
        return vulnerabilities;
    }
    
    /**
     * Gets vulnerabilities for a specific target with optimized lookup.
     * 
     * @param target The target to filter by
     * @return List of vulnerabilities for the target
     */
    public List<Vulnerability> getVulnerabilitiesForTarget(String target) {
        if (target == null) return Collections.emptyList();
        
        return getVulnerabilities().getVulnerabilitiesForTarget(target.intern());
    }
    
    /**
     * Gets project statistics with caching.
     * 
     * @return Map containing project statistics
     */
    public Map<String, Object> getStatistics() {
        long currentTime = System.currentTimeMillis();
        
        // Check cache validity
        if (statsCache != null && currentTime - lastCacheUpdate < CACHE_TTL_MS) {
            Map<String, Object> cached = statsCache.get();
            if (cached != null) {
                return new HashMap<>(cached); // Return defensive copy
            }
        }
        
        // Calculate statistics
        Map<String, Object> stats = calculateStatistics();
        this.statsCache = new WeakReference<>(new HashMap<>(stats));
        
        return stats;
    }
    
    /**
     * Calculates project statistics efficiently.
     */
    private Map<String, Object> calculateStatistics() {
        Map<String, Object> stats = new HashMap<>(8);
        
        stats.put("targetCount", targets.size());
        stats.put("tagCount", tags.size());
        
        if (vulnerabilities != null) {
            List<Vulnerability> vulns = vulnerabilities.getAllVulnerabilities();
            stats.put("vulnerabilityCount", vulns.size());
            
            // Count by severity efficiently
            Map<Severity, Integer> severityCounts = new EnumMap<>(Severity.class);
            vulns.forEach(v -> severityCounts.merge(v.getSeverity(), 1, Integer::sum));
            stats.put("severityCounts", severityCounts);
            
            // CVE statistics
            long cveCount = vulns.stream().mapToLong(v -> v.getCveId() != null ? 1 : 0).sum();
            stats.put("cveCount", cveCount);
        } else {
            stats.put("vulnerabilityCount", 0);
            stats.put("severityCounts", Collections.emptyMap());
            stats.put("cveCount", 0);
        }
        
        return stats;
    }
    
    /**
     * Adds a tag with memory optimization.
     * 
     * @param tag The tag to add
     * @return true if the tag was added, false if it already existed
     */
    public boolean addTag(String tag) {
        if (tag == null || tag.trim().isEmpty()) {
            return false;
        }
        
        String optimizedTag = internTag(tag.trim());
        boolean added = tags.add(optimizedTag);
        if (added) {
            updateLastModified();
        }
        return added;
    }
    
    /**
     * Removes a target efficiently.
     * 
     * @param target The target to remove
     * @return true if the target was removed
     */
    public boolean removeTarget(String target) {
        if (target == null) return false;
        
        boolean removed = targets.remove(target.intern());
        if (removed) {
            updateLastModified();
            invalidateCache();
        }
        return removed;
    }
    
    /**
     * Removes a tag efficiently.
     * 
     * @param tag The tag to remove
     * @return true if the tag was removed
     */
    public boolean removeTag(String tag) {
        if (tag == null) return false;
        
        boolean removed = tags.remove(internTag(tag));
        if (removed) {
            updateLastModified();
        }
        return removed;
    }
    
    /**
     * Updates the last modified timestamp.
     */
    private void updateLastModified() {
        this.lastModified = LocalDateTime.now();
    }
    
    /**
     * Invalidates cached data.
     */
    private void invalidateCache() {
        this.vulnerabilityCache = null;
        this.statsCache = null;
        this.lastCacheUpdate = 0;
    }
    
    /**
     * Optimizes memory usage by cleaning up caches and compacting collections.
     * Call this method during low-activity periods.
     */
    public void optimizeMemory() {
        invalidateCache();
        
        // Force garbage collection of weak references
        System.gc();
        
        // Compact vulnerability tree if present
        if (vulnerabilities != null) {
            vulnerabilities.optimizeMemory();
        }
    }
    
    /**
     * Gets memory usage statistics for monitoring.
     * 
     * @return Map containing memory usage information
     */
    public Map<String, Object> getMemoryStats() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("targetCount", targets.size());
        stats.put("tagCount", tags.size());
        stats.put("hasVulnerabilityCache", vulnerabilityCache != null && vulnerabilityCache.get() != null);
        stats.put("hasStatsCache", statsCache != null && statsCache.get() != null);
        
        if (vulnerabilities != null) {
            stats.put("vulnerabilityTreeMemory", vulnerabilities.getMemoryStats());
        }
        
        return stats;
    }
    
    // Getter methods with optimizations
    public String getName() { return name; }
    public String getId() { return id; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastModified() { return lastModified; }
    public String getDescription() { return description; }
    public String getScope() { return scope; }
    public String getCreatedBy() { return createdBy; }
    public String getNotes() { return notes; }
    public String getStatus() { return status; }
    
    public Set<String> getTargets() {
        return Collections.unmodifiableSet(targets);
    }
    
    public Set<String> getTags() {
        return Collections.unmodifiableSet(tags);
    }
    
    // Setter methods with optimization
    public void setDescription(String description) {
        this.description = internIfCommon(description);
        updateLastModified();
    }
    
    public void setScope(String scope) {
        this.scope = internIfCommon(scope);
        updateLastModified();
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = internIfCommon(createdBy);
        updateLastModified();
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
        updateLastModified();
    }
    
    public void setStatus(String status) {
        this.status = internStatus(status);
        updateLastModified();
    }
    
    /**
     * Efficiently formats the project for display.
     */
    public String getFormattedInfo() {
        StringBuilder info = new StringBuilder(256);
        info.append("📁 Project: ").append(name);
        
        if (description != null && !description.isEmpty()) {
            info.append("\n📝 Description: ").append(description);
        }
        
        info.append("\n📅 Created: ").append(createdAt.format(DATE_FORMAT));
        info.append("\n📊 Status: ").append(status != null ? status : "UNKNOWN");
        info.append("\n🎯 Targets: ").append(targets.size());
        
        if (vulnerabilities != null) {
            info.append("\n🔍 Vulnerabilities: ").append(vulnerabilities.getAllVulnerabilities().size());
        }
        
        if (!tags.isEmpty()) {
            info.append("\n🏷️ Tags: ").append(String.join(", ", tags));
        }
        
        return info.toString();
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof OptimizedProject)) return false;
        OptimizedProject other = (OptimizedProject) obj;
        return Objects.equals(id, other.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return String.format("OptimizedProject{name='%s', id='%s', targets=%d, vulnerabilities=%d}", 
                           name, id, targets.size(), 
                           vulnerabilities != null ? vulnerabilities.getAllVulnerabilities().size() : 0);
    }
}