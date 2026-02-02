# FT8 Decode Improvements

This document details all improvements made to the FT8 decoding system to ensure no decodes are missed and improve overall reliability.

## Overview

The FT8 decoding system has been enhanced to handle busy bands, weak signals, and edge cases that previously caused missed decodes. These improvements address issues at multiple levels: audio capture, candidate processing, signal subtraction, and message filtering.

---

## 1. Candidate Cap Fix (120 Signal Limit)

### Problem
`DecoderFt8FindSync()` limits candidates to 120 per call. On busy bands with more than 120 signals, later signals were dropped and never decoded.

### Solution
Implemented batch processing with iterative signal subtraction:

- **Batch Processing**: Process candidates in batches of up to 120
- **Signal Subtraction**: After each batch, subtract decoded signals from the audio buffer
- **Iterative Search**: Call `DecoderFt8FindSync()` again to find additional candidates that were beyond the initial 120 limit
- **Safety Limits**: Maximum of 10 batch iterations to prevent infinite loops on extremely busy bands

### Implementation Details
- Location: `FT8SignalListener.runDecode()`
- Constants:
  - `MAX_CANDIDATES_PER_BATCH = 120`
  - `MAX_BATCH_ITERATIONS = 10`
- Process:
  1. Find up to 120 candidates
  2. Process and decode all candidates
  3. Subtract decoded signals
  4. Repeat until fewer than 120 candidates found or iteration limit reached

### Impact
- **Before**: Maximum 120 signals decoded per 15-second slot
- **After**: Can decode hundreds of signals on busy bands (theoretically up to 1,200 with 10 iterations)

---

## 2. Hash Callsign Resolution Fix

### Problem
Hash callsigns (represented as `<...>`) were not being properly resolved or added to the hash list, causing:
- Resolved callsigns wrapped in angle brackets (e.g., `<W9XYZ>` instead of `W9XYZ`)
- Hash lists not being populated with resolved callsigns
- Messages with hash callsigns appearing as `<...>` even when the hash was known

### Solution
Fixed multiple issues in hash callsign handling:

1. **MessageHashMap.getCallsign()**: Removed angle bracket wrapping - now returns callsign directly
2. **Ft8Message Copy Constructor**: Only adds hashes when callsigns are successfully resolved (not `<...>`)
3. **i3=4 Non-Standard Callsigns**: Uses resolved callsign (not original) when adding hashes
4. **Post-Decode Resolution**: Added pass to resolve remaining hash callsigns after all messages in a decode pass are collected

### Implementation Details
- **MessageHashMap.java**:
  - `getCallsign()`: Returns callsign without angle brackets
  - `addHash()`: Added null/empty checks
  
- **Ft8Message.java**:
  - Copy constructor: Validates callsigns before adding to hash list
  - i3=4 handling: Uses resolved callsign for hash addition
  
- **FT8SignalListener.java**:
  - `resolveHashCallsigns()`: Post-decode pass to resolve remaining `<...>` callsigns

### Impact
- Hash callsigns properly resolved and displayed
- Hash lists correctly populated for future resolution
- Messages decoded later in a pass can help resolve earlier messages with the same hash

---

## 3. Decode Time Budget Management

### Problem
Decoding could run longer than the 15-second slot, causing:
- Overlap with the next capture cycle
- Missed messages in the next slot
- No visibility into decode overruns

### Solution
Implemented time-based deadline checking:

1. **Deep Decode Deadline**: Calculate `nextRxDeadline = utc + (FT8_SLOT_TIME * 2) - 1000ms`
   - Leaves 1 second margin before next RX cycle starts
2. **Early Exit**: Deep decode exits early if deadline is approaching
3. **Batch Processing Time Checks**: Added deadline checks in batch processing loop
4. **Decode Overrun Toast**: Optional debug toast showing decode overrun time

### Implementation Details
- **Deadline Calculation**: 
  ```java
  long nextRxDeadline = utc + (long) FT8Common.FT8_SLOT_TIME_MILLISECOND * 2 - 1000L;
  ```
- **Time Checks**: 
  - Before each deep decode iteration
  - Before and during batch candidate processing
- **Toast Message**: Shows overrun time when `GeneralVariables.decode_overrun_toast` is enabled

### Impact
- Decode respects 15s RX / 15s TX cycle boundaries
- Prevents overlap with next capture
- Provides visibility into decode performance

---

## 4. Audio Capture Truncation Fix

### Problem
Audio recorder buffer sizing assumed exact 15-second duration. Any stall, overflow, or timing drift could:
- Shorten the buffer
- Reduce decode success
- Cause missed samples at the end of the capture

### Solution
Refactored audio recording to be time-based with dynamic buffer management:

1. **Time-Based Capture**: Uses `durationMs`, `expectedSamples`, `startTimeMs` for precise timing
2. **Dynamic Buffer**: Resizable `voiceData` buffer that grows as needed
3. **Padding**: If insufficient samples collected, pad with zeros to reach expected duration
4. **Trimming**: If too many samples collected, trim excess
5. **Carry-Over**: In continuous mode, carry excess samples as `pendingData` for next cycle
6. **Timer-Based Finalization**: `Timer` scheduled to call `finalizeCycle()` after `durationMs` to ensure timely delivery

### Implementation Details
- Location: `HamRecorder.VoiceDataMonitor`
- Key Methods:
  - `resetCycle()`: Initialize new capture cycle
  - `finalizeCycle()`: Handle padding/trimming and deliver data
  - `scheduleTimeout()`: Ensure data delivered on time
  - `cancelTimer()` / `stopTimer()`: Resource cleanup

### Impact
- **Before**: Fixed-size buffer could miss samples or deliver incomplete data
- **After**: Guaranteed 15-second capture with proper padding/trimming

---

## 5. Duplicate Message Filtering Enhancement

### Problem
Duplicate filtering was text-only. Two signals with the same message text at different frequencies/times would cause the later one to be dropped, even if they were different stations.

### Solution
Enhanced duplicate detection to consider multiple factors:

1. **Message Text**: Still primary check
2. **Callsigns**: Both `callsignFrom` and `callsignTo` must match
3. **Frequency**: Must be within 3 Hz (`Math.abs(freq_hz - candidate.freq_hz) <= 3.0f`)
4. **Time Offset**: Must be within 0.2 seconds (`Math.abs(time_sec - candidate.time_sec) <= 0.2f`)
5. **SNR Update**: If duplicate found with higher SNR, update the existing message's SNR

### Implementation Details
- Location: `DecodeDuplicateFilter.isDuplicate()`
- Extracted from `FT8SignalListener` to avoid native library loading in unit tests
- Unit test: `FT8SignalListenerTest.java`

### Impact
- **Before**: Valid messages dropped if text matched
- **After**: Only true duplicates (same station, frequency, time) are filtered

---

## 7. Exception Handling and Robustness

### Problem
Unhandled exceptions in signal subtraction or decode processing could crash the decode thread or cause infinite loops.

### Solution
Added comprehensive exception handling:

1. **Signal Subtraction**: Wrapped `ReBuildSignal.subtractSignal()` in try-catch
2. **Deep Decode Safety**: If subtraction fails, stop deep decode loop to prevent infinite loops
3. **Candidate Processing**: Existing try-catch around `DecoderFt8Analysis()` preserved
4. **Logging**: All exceptions logged with context

### Implementation Details
- Signal subtraction failures logged and handled gracefully
- Deep decode loop exits on subtraction failure
- Batch processing continues even if individual candidates fail

### Impact
- More robust decode process
- Prevents crashes from unexpected errors
- Better error visibility through logging

---

## 8. a91List Management in Deep Decode

### Problem
`a91List` was being cleared at the start of each `runDecode()` call, but deep decode iterations needed to accumulate signals for subtraction. This caused:
- Signals from previous iterations not being subtracted
- Potential infinite loops in deep decode
- Incorrect signal accumulation

### Solution
Fixed `a91List` lifecycle management:

1. **Initial Decode**: Clear `a91List` at start
2. **First Deep Decode**: Clear `a91List` to start fresh
3. **Subsequent Deep Decode Iterations**: Accumulate signals (don't clear)
4. **Parameter Control**: Added `clearA91List` parameter to `runDecode()` for explicit control

### Implementation Details
- **runDecode() Overloads**:
  - `runDecode(ft8Decoder, utc, isDeep)` - Default: clears a91List
  - `runDecode(ft8Decoder, utc, isDeep, deadlineMs, clearA91List)` - Explicit control
- **Deep Decode Flow**:
  - First call: `clearA91List = true`
  - Loop iterations: `clearA91List = false`

### Impact
- Proper signal accumulation for subtraction
- Correct deep decode iteration behavior
- No signal loss between iterations

---

## 9. Post-Decode Hash Callsign Resolution

### Problem
Hash callsigns might not resolve during initial decode if the hash wasn't in the list yet. Later messages in the same decode pass could add the hash, but earlier messages with `<...>` wouldn't be updated.

### Solution
Added post-decode resolution pass:

1. **After All Messages Collected**: `resolveHashCallsigns()` called at end of `runDecode()`
2. **Hash List Check**: For each message with `<...>`, check hash list again
3. **Update Messages**: If hash found, update callsign in-place

### Implementation Details
- Location: `FT8SignalListener.resolveHashCallsigns()`
- Called after all batch processing completes
- Uses hashes added during the current decode pass

### Impact
- Messages decoded later help resolve earlier messages
- Better hash callsign resolution within a single decode pass

---

## Configuration Options

### New Settings

1. **Decode Overrun Toast** (`decode_overrun_toast`)
   - Location: Configuration tab, same line as SWR/ALC switches
   - Default: `false`
   - Shows toast when decode exceeds 15-second slot time

---

## Testing Recommendations

1. **Busy Band Testing**: Test on bands with >120 signals to verify batch processing
2. **Weak Signal Testing**: Verify deep decode finds weak signals
3. **Hash Callsign Testing**: Test with stations using hash callsigns
4. **Time Budget Monitoring**: Enable decode overrun toast to monitor timing
5. **Duplicate Detection**: Verify different stations with same message text aren't filtered

---

## Performance Impact

### Positive Impacts
- **More Decodes**: Can now decode hundreds of signals on busy bands
- **Better Resolution**: Hash callsigns properly resolved
- **No Overruns**: Time budget prevents slot overlap

### Potential Concerns
- **CPU Usage**: Batch processing may increase CPU usage on very busy bands
- **Memory**: Dynamic audio buffers may use more memory
### Mitigation
- Batch iteration limit (10) prevents excessive processing
- Time budget checks prevent excessive processing

---

## Files Modified

### Core Decode Logic
- `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ft8listener/FT8SignalListener.java`
- `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ft8listener/DecodeDuplicateFilter.java`
- `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ft8listener/A91List.java`

### Message Handling
- `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/Ft8Message.java`
- `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/MessageHashMap.java`

### Audio Capture
- `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/wave/HamRecorder.java`

### Configuration
- `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/GeneralVariables.java`
- `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/database/DatabaseOpr.java`
- `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ui/ConfigFragment.java`
- `ft8cn/app/src/main/res/layout/fragment_config.xml`
- `ft8cn/app/src/main/res/values/strings.xml`

### Testing
- `ft8cn/app/src/test/java/com/bg7yoz/ft8cn/ft8listener/FT8SignalListenerTest.java`

---

## Summary

These improvements address the major causes of missed decodes:

1. ✅ **Candidate Cap**: Batch processing handles >120 signals
2. ✅ **Hash Callsigns**: Proper resolution and hash list management
3. ✅ **Time Budget**: Prevents decode overruns
4. ✅ **Audio Capture**: Guaranteed 15-second capture with padding/trimming
5. ✅ **Duplicate Filtering**: Multi-factor detection prevents false positives
6. ✅ **Exception Handling**: Robust error handling prevents crashes
7. ✅ **Signal Accumulation**: Proper a91List management in deep decode
8. ✅ **Post-Decode Resolution**: Hash callsigns resolved after full decode pass

The decode system is now significantly more robust and should capture significantly more signals, especially on busy bands.
