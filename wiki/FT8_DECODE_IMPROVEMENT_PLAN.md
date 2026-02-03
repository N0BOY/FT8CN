# FT8 Decode Process Improvement Plan

## Executive Summary

This document outlines proposed improvements to the FT8CN decode process, focusing on processing performance and memory optimization. The current architecture is sophisticated with batch processing and deep decode capabilities, but there are opportunities for significant optimization.

**Current State:**
- 15-second decode cycles with 7-second deep decode budget
- Batch processing handles up to 120 candidates per pass (max 10 iterations)
- Linear O(n) duplicate detection per message
- Unbounded hash map growth throughout session
- Thread-per-decode model

---

## Part 1: Processing Improvements

### 1.1 Duplicate Detection Optimization

**Current State:**
The `DecodeDuplicateFilter.isDuplicate()` method performs a linear scan of all existing messages for each new candidate, resulting in O(n²) complexity during busy band conditions.

```
Current: For each of 200 decoded messages, scan 3000 existing = 600,000 comparisons
```

**Proposed Improvement:**
Implement a spatial hash index for frequency-time lookups.

**Implementation:**
1. Create a grid-based spatial index with 5 Hz x 0.5s cells
2. Only compare candidates within adjacent cells (9 cells max)
3. Reduces comparisons from O(n) to O(k) where k ≈ 10-20 per cell

**Expected Benefit:**
- 50-100x reduction in comparison operations on busy bands
- Decode time reduction: 500-1000ms on congested bands

| Aspect | Details |
|--------|---------|
| **Risk Level** | Low |
| **Risk Factors** | Edge cases at cell boundaries; hash collision handling |
| **Mitigation** | Comprehensive unit tests for boundary conditions |
| **Estimated Effort** | 2-3 days |
| **Complexity** | Medium |

---

### 1.2 Adaptive Batch Processing

**Current State:**
Fixed 120-candidate batches with hardcoded 10-iteration limit. Processes all iterations even when diminishing returns.

**Proposed Improvement:**
Implement adaptive early termination based on:
1. Candidate count drop-off (< 5 new candidates = stop)
2. SNR degradation threshold (average SNR drops > 6dB = stop)
3. Time budget remaining (< 1s = stop regardless)

**Implementation:**
```java
// Pseudocode for adaptive termination
while (iteration < MAX_ITERATIONS && timeRemaining > MIN_TIME_BUDGET) {
    int newCandidates = DecoderFt8FindSync(decoder);
    if (newCandidates < MIN_CANDIDATE_THRESHOLD) break;

    float avgSnr = calculateAverageSnr(candidates);
    if (iteration > 0 && (prevAvgSnr - avgSnr) > SNR_DROP_THRESHOLD) break;

    processBatch(candidates);
    prevAvgSnr = avgSnr;
    iteration++;
}
```

**Expected Benefit:**
- 20-40% reduction in decode time on quiet bands
- Consistent decode completion within time budget
- Battery savings on mobile devices

| Aspect | Details |
|--------|---------|
| **Risk Level** | Medium |
| **Risk Factors** | May miss weak signals on transitional SNR boundaries |
| **Mitigation** | Configurable thresholds; logging for tuning |
| **Estimated Effort** | 3-4 days |
| **Complexity** | Medium |

---

### 1.3 Signal Subtraction Caching

**Current State:**
Signal subtraction regenerates full waveforms for all decoded signals after each batch, even for unchanged signals.

**Proposed Improvement:**
Cache reconstructed waveforms (a91 data) and only regenerate for newly decoded signals.

**Implementation:**
1. Maintain `Map<Integer, float[]>` of reconstructed waveforms keyed by message hash
2. On new decode, only reconstruct new signals
3. Subtract cached + new waveforms from audio buffer
4. Clear cache at cycle boundary

**Expected Benefit:**
- 30-50% reduction in signal subtraction time
- Enables deeper decode iterations within time budget

| Aspect | Details |
|--------|---------|
| **Risk Level** | Medium-High |
| **Risk Factors** | Memory pressure from waveform cache; cache invalidation complexity |
| **Mitigation** | LRU eviction; cache size limit; thorough testing |
| **Estimated Effort** | 5-7 days |
| **Complexity** | High |

---

### 1.4 Parallel Candidate Analysis

**Current State:**
Candidate analysis (`DecoderFt8Analysis`) is sequential within each batch.

**Proposed Improvement:**
Parallelize candidate analysis using thread pool with work-stealing.

**Implementation:**
1. Create fixed thread pool (size = CPU cores - 1)
2. Submit candidates as independent tasks
3. Collect results with Future/CompletableFuture
4. Synchronize before signal subtraction

**Expected Benefit:**
- 2-4x speedup on multi-core devices
- Better utilization of modern Android hardware

| Aspect | Details |
|--------|---------|
| **Risk Level** | High |
| **Risk Factors** | Native library thread safety; synchronization overhead; race conditions |
| **Mitigation** | Verify native code is thread-safe; extensive stress testing |
| **Estimated Effort** | 7-10 days |
| **Complexity** | Very High |

---

### 1.5 Native Code LDPC Optimization

**Current State:**
LDPC decoding uses standard iterative algorithm with fixed iteration count.

**Proposed Improvement:**
Implement early termination in LDPC when parity checks pass.

**Implementation:**
1. Modify native `ft8_decode.c` LDPC loop
2. Add parity check after each iteration
3. Exit early when all constraints satisfied
4. Configurable max iterations (currently appears fixed)

**Expected Benefit:**
- 20-50% reduction in LDPC decode time for strong signals
- More time budget for weak signal processing

| Aspect | Details |
|--------|---------|
| **Risk Level** | Medium |
| **Risk Factors** | Native code modification; potential decode accuracy impact |
| **Mitigation** | A/B testing with known signal sets; gradual rollout |
| **Estimated Effort** | 5-7 days |
| **Complexity** | High (requires C/C++ expertise) |

---

## Part 2: Memory Improvements

### 2.1 MessageHashMap LRU Eviction

**Current State:**
`MessageHashMap.hashList` grows unbounded throughout the session, storing every discovered callsign-to-hash mapping.

**Problem:**
- Long sessions accumulate 10,000+ entries
- No eviction policy
- Memory grows linearly with unique callsigns heard

**Proposed Improvement:**
Implement LRU (Least Recently Used) eviction with configurable capacity.

**Implementation:**
```java
public class MessageHashMap {
    private static final int MAX_ENTRIES = 5000;
    private final LinkedHashMap<Long, String> hashToCallsign =
        new LinkedHashMap<>(MAX_ENTRIES, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry eldest) {
                return size() > MAX_ENTRIES;
            }
        };
}
```

**Expected Benefit:**
- Bounded memory usage (~200KB max for hash map)
- Predictable memory footprint
- Recent callsigns prioritized (most likely to be needed)

| Aspect | Details |
|--------|---------|
| **Risk Level** | Low |
| **Risk Factors** | May lose resolution for rarely-heard callsigns |
| **Mitigation** | Generous capacity (5000+); logging of evictions |
| **Estimated Effort** | 1-2 days |
| **Complexity** | Low |

---

### 2.2 Message List Priority Queue

**Current State:**
Message list capped at 3000 entries with FIFO eviction (oldest removed).

**Problem:**
- High-SNR important messages may be evicted
- Low-value noise floor messages retained
- No prioritization by signal quality

**Proposed Improvement:**
Implement priority-based eviction considering:
1. SNR (higher = more valuable)
2. Message type (CQ, directed call = more valuable)
3. Recency (recent = more valuable)

**Implementation:**
```java
// Composite priority score
float priority = (snr + 30) * 2.0f           // SNR: -30 to +30 → 0 to 120
               + (isCQ ? 20 : 0)              // CQ bonus
               + (isDirectedToMe ? 50 : 0)    // Directed call bonus
               + (ageMinutes < 5 ? 30 : 0);   // Recency bonus

// Evict lowest priority when at capacity
```

**Expected Benefit:**
- Important messages retained longer
- Better user experience during busy conditions
- More meaningful message history

| Aspect | Details |
|--------|---------|
| **Risk Level** | Low-Medium |
| **Risk Factors** | Priority calculation may not match user expectations |
| **Mitigation** | Configurable weights; user feedback collection |
| **Estimated Effort** | 2-3 days |
| **Complexity** | Medium |

---

### 2.3 Audio Buffer Pool

**Current State:**
New `float[]` buffer allocated for each 13-second recording cycle.

**Problem:**
- GC pressure from frequent large allocations (~200KB per cycle)
- Potential GC pauses during time-critical decode
- Memory fragmentation

**Proposed Improvement:**
Implement object pool for audio buffers.

**Implementation:**
```java
public class AudioBufferPool {
    private static final int POOL_SIZE = 3;
    private static final int BUFFER_SIZE = 12000 * 13; // 13 seconds @ 12kHz

    private final Queue<float[]> pool = new ArrayDeque<>(POOL_SIZE);

    public synchronized float[] acquire() {
        float[] buffer = pool.poll();
        return buffer != null ? buffer : new float[BUFFER_SIZE];
    }

    public synchronized void release(float[] buffer) {
        if (pool.size() < POOL_SIZE) {
            Arrays.fill(buffer, 0f); // Clear for reuse
            pool.offer(buffer);
        }
    }
}
```

**Expected Benefit:**
- Eliminates GC pressure from audio buffers
- Smoother decode performance
- Reduced memory fragmentation

| Aspect | Details |
|--------|---------|
| **Risk Level** | Low |
| **Risk Factors** | Buffer lifecycle management; potential leaks |
| **Mitigation** | Clear ownership semantics; leak detection logging |
| **Estimated Effort** | 1-2 days |
| **Complexity** | Low |

---

### 2.4 A91List Optimization

**Current State:**
`A91List` stores decoded LDPC payloads (88 bytes each) for signal subtraction, cleared between batches in some conditions.

**Problem:**
- Redundant storage when signals already subtracted
- No deduplication of identical a91 data

**Proposed Improvement:**
1. Use hash-based deduplication for identical a91 payloads
2. Immediate cleanup after successful subtraction
3. Bounded capacity with oldest eviction

**Expected Benefit:**
- 20-30% reduction in A91List memory usage
- Faster iteration over unique signals

| Aspect | Details |
|--------|---------|
| **Risk Level** | Medium |
| **Risk Factors** | Hash collisions could cause incorrect subtraction |
| **Mitigation** | Use cryptographic hash (SHA-256 truncated); collision logging |
| **Estimated Effort** | 2-3 days |
| **Complexity** | Medium |

---

### 2.5 Thread Pool for Decode Workers

**Current State:**
Each decode cycle spawns a new thread via `new Thread(() -> {...}).start()`.

**Problem:**
- Thread creation overhead (~1ms per thread)
- Unbounded thread count if decodes back up
- No thread reuse

**Proposed Improvement:**
Use `ExecutorService` with bounded thread pool.

**Implementation:**
```java
private static final ExecutorService decodeExecutor =
    Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "FT8-Decode");
        t.setPriority(Thread.MAX_PRIORITY - 1);
        return t;
    });

public void decodeFt8(long utc, float[] voiceData) {
    decodeExecutor.submit(() -> {
        // decode logic
    });
}
```

**Expected Benefit:**
- Eliminates thread creation overhead
- Bounded resource usage
- Better thread naming for debugging

| Aspect | Details |
|--------|---------|
| **Risk Level** | Low |
| **Risk Factors** | Queue backup if decode consistently slow |
| **Mitigation** | Bounded queue with rejection policy; monitoring |
| **Estimated Effort** | 1 day |
| **Complexity** | Low |

---

## Part 3: Observability Improvements

### 3.1 Decode Performance Metrics

**Proposed Metrics:**
```java
class DecodeMetrics {
    long findSyncTimeMs;      // Time in DecoderFt8FindSync
    long analysisTimeMs;      // Time in DecoderFt8Analysis (total)
    long subtractionTimeMs;   // Time in signal subtraction
    long hashResolutionTimeMs;// Time resolving hash callsigns
    int candidatesFound;      // Total candidates across batches
    int messagesDecoded;      // Successfully decoded messages
    int duplicatesFiltered;   // Messages filtered as duplicates
    int batchIterations;      // Number of batch iterations
    boolean timeoutOccurred;  // Whether deadline was hit
}
```

**Implementation:**
- Collect metrics during decode
- Expose via `ApplicationLogManager`
- Optional export to file for analysis

| Aspect | Details |
|--------|---------|
| **Risk Level** | Very Low |
| **Risk Factors** | Minor performance overhead from timing calls |
| **Mitigation** | Use `System.nanoTime()` for minimal overhead |
| **Estimated Effort** | 2-3 days |
| **Complexity** | Low |

---

## Implementation Priority Matrix

| Improvement | Impact | Effort | Risk | Priority |
|-------------|--------|--------|------|----------|
| 2.1 MessageHashMap LRU | Medium | Low | Low | **P1** |
| 2.3 Audio Buffer Pool | Medium | Low | Low | **P1** |
| 2.5 Thread Pool | Low | Low | Low | **P1** |
| 1.1 Duplicate Detection | High | Medium | Low | **P2** |
| 2.2 Message Priority Queue | Medium | Medium | Low | **P2** |
| 3.1 Decode Metrics | Medium | Low | Very Low | **P2** |
| 1.2 Adaptive Batch | High | Medium | Medium | **P3** |
| 2.4 A91List Optimization | Low | Medium | Medium | **P3** |
| 1.3 Signal Subtraction Cache | High | High | Medium-High | **P4** |
| 1.5 Native LDPC Optimization | High | High | Medium | **P4** |
| 1.4 Parallel Candidate Analysis | Very High | Very High | High | **P5** |

---

## Estimated Timeline

### Phase 1: Quick Wins (1-2 weeks)
- MessageHashMap LRU eviction (2 days)
- Audio buffer pool (2 days)
- Thread pool for decode workers (1 day)
- Basic decode metrics (2 days)

### Phase 2: Core Optimizations (2-3 weeks)
- Spatial hash duplicate detection (3 days)
- Message priority queue (3 days)
- A91List deduplication (3 days)
- Adaptive batch termination (4 days)

### Phase 3: Advanced Optimizations (3-4 weeks)
- Signal subtraction caching (7 days)
- Native LDPC early termination (7 days)
- Performance profiling and tuning (5 days)

### Phase 4: Parallel Processing (4-6 weeks)
- Native library thread safety audit (5 days)
- Parallel candidate analysis implementation (10 days)
- Stress testing and optimization (10 days)

**Total Estimated Time: 10-15 weeks for full implementation**

---

## Risk Summary

| Risk Category | Description | Mitigation Strategy |
|---------------|-------------|---------------------|
| **Decode Accuracy** | Optimizations may cause missed signals | A/B testing with known signal sets; gradual rollout |
| **Memory Leaks** | Buffer pools and caches may leak | Lifecycle management; leak detection tooling |
| **Thread Safety** | Parallel processing may cause races | Extensive concurrency testing; native code audit |
| **Performance Regression** | Overhead may exceed benefits | Benchmarking before/after; feature flags |
| **Battery Impact** | More processing may increase power usage | Profile battery impact; adaptive processing |
| **Device Compatibility** | Optimizations may not work on all devices | Test matrix across device tiers; graceful fallback |

---

## Testing Requirements

### Unit Tests Required
- Spatial hash index boundary conditions
- LRU eviction correctness
- Priority queue ordering
- Buffer pool lifecycle
- Thread pool rejection handling

### Integration Tests Required
- Full decode pipeline with optimizations
- Memory usage under sustained load
- Decode accuracy validation (known signal sets)
- Time budget adherence under stress

### Performance Tests Required
- Decode latency benchmarks (p50, p95, p99)
- Memory allocation rate
- GC pause frequency and duration
- CPU utilization during decode

---

## Success Criteria

1. **Processing Time:** 20% reduction in average decode time on busy bands
2. **Memory Usage:** Bounded to <50MB regardless of session length
3. **Decode Accuracy:** No regression in message decode rate
4. **Stability:** Zero crashes attributable to optimizations
5. **Battery:** <5% increase in power consumption

---

## Appendix: Current Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────┐
│                         FT8 Decode Pipeline                          │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌──────────┐    ┌──────────────┐    ┌─────────────────────────┐   │
│  │ UtcTimer │───▶│ HamRecorder  │───▶│   FT8SignalListener     │   │
│  │ (100ms)  │    │ (13s audio)  │    │                         │   │
│  └──────────┘    └──────────────┘    │  ┌───────────────────┐  │   │
│                                       │  │ InitDecoder (JNI) │  │   │
│                                       │  └─────────┬─────────┘  │   │
│                                       │            ▼            │   │
│  ┌──────────────────────────────┐    │  ┌───────────────────┐  │   │
│  │      MessageHashMap          │◀───┼──│ DecoderMonitor    │  │   │
│  │  (callsign ↔ hash cache)     │    │  │ (load audio)      │  │   │
│  └──────────────────────────────┘    │  └─────────┬─────────┘  │   │
│                                       │            ▼            │   │
│                                       │  ┌───────────────────┐  │   │
│                                       │  │ FindSync (JNI)    │  │   │
│  ┌──────────────────────────────┐    │  │ (max 120 cands)   │  │   │
│  │   DecodeDuplicateFilter      │    │  └─────────┬─────────┘  │   │
│  │  (freq/time/callsign match)  │◀───┤            ▼            │   │
│  └──────────────────────────────┘    │  ┌───────────────────┐  │   │
│                                       │  │ Analysis (JNI)    │──┼───┼▶ Ft8Message
│                                       │  │ (decode each)     │  │   │
│  ┌──────────────────────────────┐    │  └─────────┬─────────┘  │   │
│  │        A91List               │◀───┼────────────┤            │   │
│  │  (LDPC payloads for recon)   │    │            ▼            │   │
│  └──────────────────────────────┘    │  ┌───────────────────┐  │   │
│                                       │  │ ReBuildSignal     │  │   │
│                                       │  │ (subtract sigs)   │  │   │
│                                       │  └─────────┬─────────┘  │   │
│                                       │            │            │   │
│                                       │       (loop if         │   │
│                                       │        deep mode)      │   │
│                                       └────────────────────────┘   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

*Document Version: 1.0*
*Created: 2026-02-02*
*Author: Claude Code Analysis*
