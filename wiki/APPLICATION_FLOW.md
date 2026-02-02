# FT8CN Application Flow Documentation

## Table of Contents

1. [Overview](#overview)
2. [Application Architecture](#application-architecture)
3. [UTC Timer Synchronization](#utc-timer-synchronization)
4. [Audio Capture and Recording](#audio-capture-and-recording)
5. [Decoding Process](#decoding-process)
6. [Message Deduplication](#message-deduplication)
7. [Auto-Sequencing System](#auto-sequencing-system)
8. [Callsign Queue Management](#callsign-queue-management)
9. [Transmission Cycle](#transmission-cycle)
10. [QSO Completion and Logging](#qso-completion-and-logging)
11. [Complete Flow Diagram](#complete-flow-diagram)

---

## Overview

FT8CN is an Android application for operating FT8 (Franke-Taylor 8) digital mode on amateur radio. The application operates in synchronized 15-second cycles aligned with UTC time, alternating between receiving (RX) and transmitting (TX) periods.

### Key Components

- **MainViewModel**: Central coordinator managing all subsystems
- **FT8SignalListener**: Handles audio capture, decoding, and message processing
- **FT8TransmitSignal**: Manages transmission, auto-sequencing, and QSO state
- **UtcTimer**: Synchronizes all operations to UTC time
- **HamRecorder**: Captures audio from microphone or network sources
- **CallsignQueue**: Manages multiple responding callsigns

---

## Application Architecture

### Component Relationships

```
MainViewModel (Singleton)
├── UtcTimer (UTC synchronization)
├── HamRecorder (Audio capture)
├── FT8SignalListener (Decoding)
│   └── UtcTimer (RX cycle trigger)
├── FT8TransmitSignal (Transmission & Auto-sequencing)
│   ├── UtcTimer (TX cycle trigger)
│   └── CallsignQueue (Multiple callsign management)
└── DatabaseOpr (Persistence)
```

### Lifecycle

1. **Initialization**: `MainViewModel` constructor creates all subsystems
2. **Startup**: Timers start, audio recording begins
3. **Operation**: Continuous RX/TX cycles synchronized to UTC
4. **Shutdown**: Timers stop, resources released

---

## UTC Timer Synchronization

### Purpose

FT8 requires precise time synchronization. All operations must align with UTC time in 15-second cycles (FT8_SLOT_TIME = 15 seconds).

### Implementation

**Location**: `UtcTimer.java`

**Key Features**:
- 100ms heartbeat interval checks for cycle boundaries
- Uses `(utc / 100) % 600) % sec == 0` to detect cycle start
- Prevents duplicate triggers with 1-second wait after trigger
- All callbacks execute in thread pool for thread safety

### Cycle Structure

```
UTC Time:  ... 00:00:00  ... 00:00:15  ... 00:00:30  ...
           └─ Cycle 0 ─┘ └─ Cycle 1 ─┘ └─ Cycle 2 ─┘

Sequential:    0 (Odd)       1 (Even)       0 (Odd)
```

**Sequential Values**:
- `0` = Odd timeslot (00, 30, 60 seconds past minute)
- `1` = Even timeslot (15, 45 seconds past minute)

### Timer Usage

1. **RX Timer** (`FT8SignalListener`):
   - Triggers at start of each 15-second cycle
   - Initiates audio capture for 15 seconds
   - Calls `runRecorde()` → `decodeFt8()`

2. **TX Timer** (`FT8TransmitSignal`):
   - Triggers when `sequential` matches current timeslot
   - Only if `activated == true`
   - Calls `doTransmit()` to generate and transmit message

### Time Synchronization

FT8 requires precise time synchronization because all stations worldwide must operate in synchronized 15-second cycles. Even small time errors (hundreds of milliseconds) can prevent successful decoding and transmission.

#### Overview

**Location**: `UtcTimer.java`

The application uses a two-tier time synchronization strategy: **GPS time is tried first** (most accurate), and if GPS is unavailable or fails, it **falls back to Network Time Protocol (NTP)**. The time offset is calculated and stored as a static delay value that adjusts all time calculations throughout the application.

#### Time Synchronization Strategy

**Method**: `UtcTimer.syncTime(AfterSyncTime afterSyncTime)`

**Priority Order**:
1. **GPS Time** (Primary) - Most accurate, works without internet
2. **NTP Time** (Fallback) - Requires internet connection

#### GPS Synchronization Process

**Method**: `syncTimeFromGPS(AfterSyncTime afterSyncTime)`

**Process**:
1. Checks if GPS provider is enabled
2. Verifies location permissions are granted
3. Attempts to get last known GPS location (fast, synchronous)
   - If location is recent (< 5 minutes old), uses GPS time immediately
4. If last known location is stale, requests fresh GPS location update
   - Waits up to 10 seconds for GPS fix
   - Uses `Location.getTime()` which provides UTC time from GPS satellites
5. Calculates time difference: `trueDelay = gpsTime - System.currentTimeMillis()`
6. Sets global delay: `UtcTimer.delay = trueDelay % 15000`
   - Modulo 15000ms ensures delay stays within one FT8 cycle (15 seconds)
7. Calls success callback: `afterSyncTime.doAfterSyncTimer(trueDelay)`
8. On failure or timeout: Returns `false` to trigger NTP fallback

**GPS Advantages**:
- More accurate than NTP (direct satellite time)
- Works without internet connection
- No network latency
- Typically accurate to within milliseconds

**GPS Limitations**:
- Requires GPS signal (may not work indoors)
- May take time to get fix (up to 10 seconds timeout)
- Requires location permissions

#### NTP Synchronization Process (Fallback)

**Method**: `syncTimeFromNTP(AfterSyncTime afterSyncTime)`

**Process**:
1. Uses configured NTP server (defaults to `time.windows.com` if not set)
2. **Retry Logic**: Attempts up to 3 times with exponential backoff
   - **Attempt 1**: Immediate
   - **Attempt 2**: Wait 2 seconds after first failure
   - **Attempt 3**: Wait 4 seconds after second failure
   - **Total maximum time**: ~26 seconds (10s timeout × 3 attempts + 2s + 4s backoff)
3. Each attempt:
   - Creates new NTP client with 10-second timeout
   - Connects to configured NTP server
   - Retrieves server time via `NTPUDPClient`
   - On success: Calculates delay and exits retry loop
   - On failure: Waits with exponential backoff before next attempt
4. After all retries:
   - Calculates time difference: `trueDelay = serverTime - System.currentTimeMillis()`
   - Sets global delay: `UtcTimer.delay = trueDelay % 15000`
     - Modulo 15000ms ensures delay stays within one FT8 cycle (15 seconds)
5. Calls success callback: `afterSyncTime.doAfterSyncTimer(trueDelay, "NTP", server)`
6. On failure after all retries: Calls `afterSyncTime.syncFailed("NTP", server, reason)`
   - Error message includes retry count: "Failed after 3 attempts" or "NTP timeout after 3 attempts"

**NTP Advantages**:
- Works indoors (no GPS signal needed)
- Usually fast (< 1 second)
- Works when GPS is disabled
- **Retry logic handles transient network issues**

**NTP Limitations**:
- Requires internet connection
- Network latency can affect accuracy
- Server availability dependency
- **Timeout**: 10 seconds per attempt (prevents indefinite hangs)

**Retry Strategy**:
- **Maximum retries**: 3 attempts
- **Timeout per attempt**: 10 seconds
- **Backoff delays**: 2s, 4s, 8s (exponential)
- **Total maximum duration**: ~26 seconds (worst case)
- **Resource cleanup**: NTP client closed after each attempt

**Sync Points**:
- **Startup**: Called automatically in `MainViewModel` constructor (line 289)
  - Uses `null` callback (no user feedback needed)
  - Tries GPS first, falls back to NTP if GPS fails
  - **Frequency**: Only once when the application starts
- **Manual Sync**: Called from `ConfigFragment` when user clicks sync button
  - Tries GPS first, falls back to NTP if GPS fails
  - Provides user feedback via toast messages:
    - Shows delay amount if >100ms difference
    - Shows "clock is accurate" if within ±100ms
  - **Frequency**: Only when user explicitly requests it

**Important**: There is **no automatic periodic time synchronization**. The time offset (`UtcTimer.delay`) is set once at startup (or when manually synced) and persists for the entire application session. The timers in `UtcTimer` are used for cycle detection and heartbeat callbacks, not for time synchronization.

**Time Sync Frequency Summary**:
- **At startup**: Once automatically
- **Manual sync**: On-demand via user action
- **Periodic sync**: None (not implemented)
- **Delay persistence**: The calculated delay persists until the next sync or manual adjustment

#### Time Offset Mechanism

**Global Delay**: `UtcTimer.delay` (static int, milliseconds)
- Applied to all time calculations via `getSystemTime()`
- Formula: `delay + System.currentTimeMillis()`
- Adjusted during NTP sync
- Can be manually adjusted via ConfigFragment spinner (-7500ms to +7500ms in 500ms steps)

**Per-Timer Offset**: `time_sec` (instance variable, milliseconds)
- Used for fine-tuning individual timer instances
- Applied in cycle detection: `(utc - time_sec) / 100) % 600) % sec == 0`
- Used for transmission timing adjustments

#### Time Calculation Flow

```
System.currentTimeMillis()
    +
UtcTimer.delay (from NTP sync or manual adjustment)
    =
UtcTimer.getSystemTime() → Used throughout application
```

**Usage Examples**:
- `UtcTimer.getSystemTime()` - Returns adjusted UTC time
- `UtcTimer.sequential(utc)` - Calculates odd/even timeslot (0 or 1)
- `UtcTimer.getTimeStr(time)` - Formats UTC time for display
- All cycle triggers use adjusted time to ensure synchronization

#### Manual Time Offset

Users can manually adjust time offset when:
- NTP sync fails (no internet connection)
- Fine-tuning is needed for specific conditions
- Testing or calibration is required

**Location**: `ConfigFragment.java` → UTC Time Offset spinner

**Range**: -7500ms to +7500ms (in 500ms increments)
- Covers one full FT8 cycle (15 seconds)
- Applied directly to `UtcTimer.delay`
- Persisted in database as "utcDelay" config parameter

#### Time Accuracy Requirements

FT8 requires time accuracy within approximately ±100ms for reliable operation:
- **Optimal**: Within ±100ms (toast shows "clock is accurate")
- **Acceptable**: ±100ms to ±500ms (may work but less reliable)
- **Problematic**: >±500ms (likely to cause decode/transmit failures)

The application calculates and displays the time offset after each sync to help users understand their clock accuracy.

#### Error Handling

**GPS Sync Failures** (falls back to NTP):
- GPS provider disabled → Falls back to NTP
- Location permissions denied → Falls back to NTP
- No GPS signal (indoors) → Falls back to NTP
- GPS timeout (> 10 seconds) → Falls back to NTP
- Stale GPS data → Falls back to NTP

**NTP Sync Failures** (final fallback):
- Network unavailable → `syncFailed()` callback invoked, uses system time
- Server unreachable → `syncFailed()` callback invoked, uses system time
- Timeout → Exception caught, uses system time
- Manual offset remains available as backup

**Fallback Behavior**:
- GPS → NTP → System Time (with manual offset option)
- If both GPS and NTP fail, application continues with system time
- User can manually adjust offset if needed
- No blocking or crashes on sync failure
- Each sync attempt is independent and non-blocking

---

## Audio Capture and Recording

### Audio Sources

The application supports multiple audio input sources:

1. **Microphone** (`MicRecorder`): Direct audio input
2. **Network Audio** (`IComWifiConnector`, `FlexConnector`, `X6100Connector`): Audio over network
3. **CAT Audio** (`CableConnector`): Audio over CAT interface (e.g., (tr)uSDX)

### Recording Process

**Location**: `HamRecorder.java` → `VoiceDataMonitor`

**Flow**:
1. **Cycle Start**: `resetCycle()` initializes new 15-second capture
2. **Data Collection**: Audio samples accumulated in `voiceData` buffer
3. **Dynamic Buffer Management**:
   - Buffer grows as needed
   - Padding with zeros if insufficient samples
   - Trimming excess if too many samples
4. **Cycle End**: `finalizeCycle()` called after `durationMs` (15 seconds)
   - Timer-based finalization ensures timely delivery
   - Data delivered to decoder via callback

**Key Improvements** (from `DECODE_IMPROVEMENTS.md`):
- Time-based capture with dynamic buffer sizing
- Guaranteed 15-second duration with padding/trimming
- Carry-over support for continuous mode
- Timer-based finalization prevents timing drift

### Audio Format

- **Sample Rate**: 12,000 Hz (configurable via `GeneralVariables.audioSampleRate`)
- **Format**: PCM Float32 or PCM 16-bit (configurable)
- **Channels**: Mono
- **Duration**: Exactly 15 seconds per cycle

---

## Decoding Process

### Overview

The decoding process extracts FT8 messages from captured audio using native C libraries via JNI.

**Location**: `FT8SignalListener.java` → `decodeFt8()` → `runDecode()`

### Decode Flow

```
1. Audio Data Received
   ↓
2. Initialize Decoder (InitDecoder)
   ↓
3. Load Audio Data (DecoderMonitorPressFloat)
   ↓
4. Initial Decode Pass (runDecode, isDeep=false)
   ├── Batch Processing Loop (up to 10 iterations):
   │   ├── Find candidates (DecoderFt8FindSync, up to 120 per batch)
   │   ├── Process each candidate (DecoderFt8Analysis)
   │   ├── Deduplicate messages
   │   ├── Subtract decoded signals (ReBuildSignal.subtractSignal)
   │   └── If 120 found, repeat to find more (up to 1,200 total)
   └── Return decoded messages
   ↓
5. Deep Decode (if enabled)
   ├── First deep decode pass (clear a91List)
   ├── Iterative subtraction loop:
   │   ├── Subtract decoded signals (ReBuildSignal.subtractSignal)
   │   ├── Find new candidates
   │   ├── Process candidates
   │   └── Repeat until no new messages found
   └── Post-decode hash callsign resolution
   ↓
6. Callback (afterDecode)
   ├── **Early Processing**: parseMessageToFunction() called FIRST
   │   └── Allows sequence advancement even during TX cycles
   │   └── Deep decode messages can also advance sequence
   ├── Add to message list
   ├── Update UI
   └── Query callsign locations
```

### Batch Processing (Busy Band Handling)

**Note**: The native library function `DecoderFt8FindSync()` returns a maximum of 120 candidates per call. This is a limitation of the underlying C library, not the Java application.

**Solution**: Batch processing with iterative signal subtraction overcomes this limitation

**Implementation**:
```java
MAX_CANDIDATES_PER_BATCH = 120
MAX_BATCH_ITERATIONS = 10

while (batchIteration < MAX_BATCH_ITERATIONS):
    1. Find up to 120 candidates
    2. Process and decode all candidates
    3. Subtract decoded signals from audio buffer
    4. If 120 candidates found, search again
    5. If <120 candidates, done
```

**Result**: Can decode hundreds of signals on busy bands (theoretically up to 1,200 with 10 iterations). The 120 limit applies per batch, not total - batch processing effectively removes this limitation for practical purposes.

### Time Budget Management

**Problem**: Decoding could run longer than 15-second slot, causing overlap with next capture.

**Solution**: Time-based deadline checking

**Implementation**:
- Calculate `nextRxDeadline = utc + (FT8_SLOT_TIME * 2) - 1000ms`
- Check deadline before each deep decode iteration
- Check deadline during candidate processing
- Exit early if deadline approaching

**Result**: Decode respects 15s RX / 15s TX cycle boundaries, prevents overlap.

### Early Message Processing for Auto-Sequencing

**Critical Feature**: Messages are processed for auto-sequencing **immediately** upon decode, even during active transmission cycles.

**Location**: `MainViewModel.java` → `afterDecode()` callback

**Implementation**:
```java
// Process decodes immediately for TX targeting - call this FIRST before other processing
// Allow evaluation even during transmission so sequence can advance based on decodes
// Deep decode messages can also advance the sequence
// Process all decodes regardless of timing
ft8TransmitSignal.parseMessageToFunction(messages); // Called FIRST
```

**Why This Matters**:

1. **Sequence Advancement During TX**: 
   - Messages decoded during a transmission cycle can still advance the sequence
   - Allows the system to prepare the next message even while transmitting
   - Ensures timely response at the start of the next cycle

2. **Deep Decode Integration**:
   - Weak signals found in deep decode can also advance the sequence
   - Prevents missing sequence advancement from weak or late responses

3. **Timing Independence**:
   - Processing happens regardless of whether we're in RX or TX cycle
   - Messages are evaluated as soon as they're decoded
   - No delay waiting for cycle boundaries

4. **Processing Order**:
   - `parseMessageToFunction()` is called **before** adding messages to the list
   - Called **before** UI updates
   - Called **before** other message processing
   - Ensures sequence state is always current

**Flow**:
```
Decode Complete
    ↓
parseMessageToFunction() ← Called FIRST (early processing)
    ├── Check for responses
    ├── Advance sequence if needed
    ├── Update queue if needed
    └── Prepare next transmission
    ↓
Add to message list
    ↓
Update UI
    ↓
Other processing (QTH queries, etc.)
```

**Result**: The auto-sequencing system can respond to messages immediately upon decode, ensuring the sequence is always up-to-date and ready for the next transmission cycle.

### Hash Callsign Resolution

**Problem**: Hash callsigns (represented as `<...>`) were not properly resolved.

**Solution**: 
1. Proper hash list management
2. Post-decode resolution pass

**Implementation**:
- Messages decoded later in pass can help resolve earlier messages
- `resolveHashCallsigns()` called after all batch processing
- Checks hash list for each `<...>` callsign
- Updates callsign in-place if hash found

---

## Message Deduplication

### Purpose

Prevent duplicate messages from appearing in the message list when the same signal is decoded multiple times (e.g., in initial decode and deep decode passes).

### Implementation

**Location**: `DecodeDuplicateFilter.java` → `isDuplicate()`

**Multi-Factor Detection**:

A message is considered a duplicate if **ALL** of the following match:

1. **Message Text**: `msg.getMessageText().equals(candidate.getMessageText())`
2. **Callsigns**: Both `callsignFrom` and `callsignTo` must match (null-safe)
3. **Frequency**: Within 3 Hz (`Math.abs(freq_hz - candidate.freq_hz) <= 3.0f`)
4. **Time Offset**: Within 0.2 seconds (`Math.abs(time_sec - candidate.time_sec) <= 0.2f`)

**SNR Update**: If duplicate found with higher SNR, update existing message's SNR:
```java
if (msg.snr < candidate.snr) {
    msg.snr = candidate.snr;
}
```

### Usage

Called in two places:
1. **During Decode** (`FT8SignalListener.runDecode()`): Filters duplicates within decode pass
2. **Message List Management** (`MainViewModel.isMessageInTransmitList()`): Prevents duplicates in transmit message list

### Rationale

Previous text-only deduplication caused valid messages from different stations to be dropped if they had the same message text. Multi-factor detection ensures only true duplicates (same station, frequency, time) are filtered.

---

## Auto-Sequencing System

### Overview

The auto-sequencing system automatically manages FT8 QSO (contact) sequences by:
1. Detecting responses from target callsign
2. Advancing message sequence (1→2→3→4→5)
3. Handling no-response scenarios
4. Managing QSO completion

**Location**: `FT8TransmitSignal.java` → `parseMessageToFunction()`

### Message Sequence (Function Order)

FT8 QSO follows a standard 6-message sequence:

| Order | Message Type | Example | Description |
|-------|-------------|---------|-------------|
| 1 | Call + Grid | `BG7YOZ W9XYZ OL50` | Initial call with grid square |
| 2 | Report | `BG7YOZ W9XYZ -10` | Signal report |
| 3 | R + Report | `BG7YOZ W9XYZ R-10` | Report received |
| 4 | RR73 | `BG7YOZ W9XYZ RR73` | Ready for 73 |
| 5 | 73 | `BG7YOZ W9XYZ 73` | End of QSO |
| 6 | CQ | `CQ BG7YOZ OL50` | Calling anyone |

### Sequence Advancement Logic

**Key Method**: `parseMessageToFunction(ArrayList<Ft8Message> msgList)`

**Flow**:

```
1. Validate Input
   ├── Check callsign configured
   ├── Check message list not empty
   └── Filter messages by sequence (skip same sequence as TX)

2. Check for Response from Target
   └── checkFunctionOrdFromMessages() → returns order (1-5) or -1

3. If Response Received (newOrder != -1):
   ├── Reset noReplyCount
   ├── Update QSL record
   ├── If order 1 or 2: Reset target report, regenerate function list
   ├── Advance sequence: functionOrder = newOrder + 1
   │   └── Only advance forward (never backward)
   └── Update UI

4. If No Response (newOrder == -1):
   ├── Check if someone calling us → checkCQMeOrFollowCQMessage()
   ├── If in CQ mode (order 6): Check for new calls
   └── Increment noReplyCount (if not weak signal)

5. Handle No-Reply Timeout:
   └── If noReplyCount >= noReplyLimit:
       ├── Remove from queue
       ├── Process next queued callsign
       └── Or reset to CQ mode
```

### Message Processing Order

**Critical**: All message processing methods iterate in **reverse order (newest first)**.

**Rationale**:
- FT8 messages are added chronologically (newest at end)
- Processing newest-first ensures we respond to current QSO state
- Prevents responding to stale messages
- Returns immediately on first valid match

**Methods Using This Pattern**:
1. `checkFunctionOrdFromMessages()`: Finds most recent valid response
2. `checkTargetCallMe()`: Checks if target is calling us
3. `checkCQMeOrFollowCQMessage()`: Two-phase search (target first, then any)

**Example**:
```java
// Process messages in reverse order (newest first)
for (int i = messages.size() - 1; i >= 0; i--) {
    Ft8Message msg = messages.get(i);
    if (valid) return result; // Return immediately on first valid match
}
```

### Response Detection

**Method**: `checkFunctionOrdFromMessages(ArrayList<Ft8Message> messages)`

**Process**:
1. Iterate messages newest-first
2. Skip messages from same sequence as TX
3. Check if message is from target callsign to us
4. Extract function order from message
5. Update signal strength with latest received value
6. Extract signal report if present
7. Return order (1-5) or -1 if not found

**Compound Callsign Handling**: Uses `checkCallsignIsCallTo()` instead of `.equals()` to handle compound callsigns (e.g., "K1ABC/P", "W1ABC/MM").

### No-Reply Handling

**Configuration**: `GeneralVariables.noReplyLimit`
- `0` = Ignore (retry indefinitely)
- `N` = Retry N times before moving on

**Behavior**:
1. Increment `noReplyCount` when no response received (not weak signals)
2. When `noReplyCount >= noReplyLimit`:
   - Remove current callsign from queue
   - Process next queued callsign (if any)
   - Or reset to CQ mode
3. Each new QSO starts with `noReplyCount = 0` (fresh retry count)

**Fixed Off-by-One Error**: Changed `>` to `>=` so `noReplyLimit=2` triggers after 2 no-replies, not 3.

### QSO Completion Detection

QSO is considered complete when:

1. **Target replies with 73** (`newOrder == 5`)
2. **We sent 73 and no response** (`functionOrder == 5 && newOrder == -1`)
3. **We sent RR73 and timeout** (`functionOrder == 4 && noReplyCount > noReplyLimit * 2`)
4. **We sent RR73 and target calling others** (`functionOrder == 4 && checkTargetCallMe() > 1`)
5. **We sent RR73 and ignore mode timeout** (`functionOrder == 4 && noReplyCount > 20 && noReplyLimit == 0`)

**On Completion**:
1. Remove callsign from queue
2. Reset to CQ mode (`functionOrder = 6`)
3. Process next queued callsign (if any)
4. Or check for new calls (`checkCQMeOrFollowCQMessage()`)

---

## Callsign Queue Management

### Purpose

When multiple callsigns respond to your CQ or call, the queue system automatically manages them, ensuring you don't miss contacts and can efficiently process multiple QSOs.

**Location**: `CallsignQueue.java`

### Queue Structure

```java
public static class QueuedCallsign {
    public String callsign;
    public long timestamp;        // When added to queue
    public Ft8Message initialMessage; // Message that triggered queue entry
}
```

### Adding to Queue

**Trigger**: `checkCQMeOrFollowCQMessage()` detects someone calling us

**Conditions**:
1. Message is addressed to us (`callsignTo == myCallsign`)
2. Not a 73 message
3. Not already in active QSO with that callsign
4. Not already in queue

**Process**:
```java
if (toCallsign == null || !checkCallsignIsCallTo(msg.getCallsignFrom(), toCallsign.callsign)) {
    callsignQueue.addCallsign(msg.getCallsignFrom(), msg);
}
```

### Queue Processing Order

**Default**: Oldest first (FIFO based on timestamp)

**Implementation**:
```java
queue.sort((a, b) -> Long.compare(a.timestamp, b.timestamp));
return queue.get(0); // Return oldest
```

### Queue Integration with Auto-Sequencing

**When QSO Completes or Times Out**:
1. Remove completed/timed-out callsign from queue
2. Get next callsign from queue (`getNext()`)
3. Start QSO with next callsign:
   ```java
   setTransmit(new TransmitCallsign(...), 
               checkFunOrder(next.initialMessage) + 1, 
               next.initialMessage.extraInfo);
   ```
4. Remove from queue
5. Update UI

**Active QSO Priority**: If already in QSO with a callsign and they call again, system responds immediately without adding to queue.

### Manual Queue Control

**UI Integration**: `MyCallingFragment.java`

**Operations**:
1. **Reorder**: Long-press and drag items up/down
   - Updates queue order via `moveItem(fromPosition, toPosition)`
2. **Delete**: Swipe left/right on item
   - Removes from queue via `removeByIndex(index)`

**Thread Safety**: All queue operations are `synchronized` for thread safety.

---

## Transmission Cycle

### Overview

Transmission occurs in synchronized 15-second cycles, alternating with receive cycles.

**Location**: `FT8TransmitSignal.java` → `doTransmit()` → `DoTransmitRunnable`

### Transmission Trigger

**Condition**: 
- `UtcTimer.getNowSequential() == sequential` (matching timeslot)
- `activated == true` (transmission enabled)
- Valid callsign configured

**Timing**: Triggered by UTC timer at start of appropriate timeslot.

### Message Generation

**Critical**: Message is **generated at the start of each transmission cycle**, not during transmission.

**Location**: `DoTransmitRunnable.run()` → `getFunctionCommand(functionOrder)`

**Process**:
```java
msg = transmitSignal.getFunctionCommand(transmitSignal.functionOrder);
```

Message created using current values:
- `toCallsign`: Target callsign
- `functionOrder`: Message sequence (1-6)
- `toMaidenheadGrid`: Target's grid square (if applicable)

### Transmission Modes

1. **Audio Output** (Default):
   - Generate FT8 audio signal
   - Play via `AudioTrack`
   - Control PTT via CAT/RTS/DTR

2. **Network Audio** (`ConnectMode.NETWORK`):
   - Send audio data over network
   - Radio generates signal
   - No local audio playback

3. **CAT Audio** (`ControlMode.CAT` with `supportWaveOverCAT()`):
   - Send audio data over CAT interface
   - Radio generates signal (e.g., (tr)uSDX)
   - No local audio playback

### Transmission During Active QSO

**Question**: When someone calls us during the middle of a TX cycle, does the message switch immediately?

**Answer**: **No, the current TX message does NOT switch immediately.**

**What Happens**:
1. **During Active Transmission**:
   - `parseMessageToFunction()` detects someone calling us
   - `setTransmit()` updates `toCallsign` and `functionOrder`
   - **Current transmission continues with original message** (already generated)

2. **Next Transmission Cycle**:
   - New `DoTransmitRunnable` generates message using **updated** values
   - New target/message used for this cycle

**Rationale**:
- FT8 transmissions are time-synchronized 15-second cycles
- Interrupting mid-cycle would break timing synchronization
- Message integrity requires complete transmission
- System correctly responds at start of next cycle

### Sequential Calculation

**Automatic** (default):
```java
sequential = (toCallsign.sequential + 1) % 2;
```
Transmit on opposite timeslot from target.

**Manual Override**:
```java
if (GeneralVariables.manualTimeslot >= 0) {
    sequential = GeneralVariables.manualTimeslot; // 0 = Odd, 1 = Even
}
```

---

## QSO Completion and Logging

### QSO Record Management

**Location**: `FT8TransmitSignal.java` → `updateQSlRecordList()`

**Process**:
1. Create or retrieve QSL record for callsign
2. Update record based on function order:
   - **Order 1**: Update grid, send report
   - **Order 2-3**: Update send/received reports
   - **Order 4-5**: Mark as complete, save to database

### QSO Completion

**Method**: `doComplete()`

**Actions**:
1. Record end time
2. Lookup grid if missing
3. Create `QSLRecord` with all QSO details
4. Save to database via `databaseOpr.addQSL_Callsign()`
5. Upload to third-party services (Cloudlog, QRZ) if enabled
6. Add DXCC/ITU/CQ zones to lists

### Third-Party Integration

**Services**:
- **Cloudlog**: Automatic upload if `GeneralVariables.enableCloudlog == true`
- **QRZ**: Automatic upload if `GeneralVariables.enableQRZ == true`

**Location**: `ThirdPartyService.java`

**Process**: Runs in background thread to avoid blocking UI.

---

## Complete Flow Diagram

### Full Application Cycle

```
┌─────────────────────────────────────────────────────────────┐
│                    Application Startup                       │
│  MainViewModel Constructor                                   │
│  ├── Create UtcTimer (heartbeat)                            │
│  ├── Create HamRecorder                                     │
│  ├── Create FT8SignalListener (RX timer)                    │
│  ├── Create FT8TransmitSignal (TX timer)                    │
│  └── Sync UTC time (NTP)                                     │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                    UTC Timer Heartbeat                       │
│  Every 100ms: Check for cycle boundary                      │
│  ├── RX Cycle: (utc % 15000) == 0 → Trigger RX             │
│  └── TX Cycle: (utc % 15000) == 0 && sequential match       │
└─────────────────────────────────────────────────────────────┘
                            ↓
        ┌───────────────────┴───────────────────┐
        │                                         │
        ↓                                         ↓
┌───────────────┐                      ┌───────────────┐
│   RX Cycle    │                      │   TX Cycle   │
│  (15 seconds) │                      │  (15 seconds) │
└───────────────┘                      └───────────────┘
        │                                         │
        ↓                                         ↓
┌──────────────────┐                  ┌──────────────────┐
│ Audio Capture    │                  │ Message Gen      │
│ HamRecorder      │                  │ getFunctionCmd()  │
│ 15 seconds       │                  │                  │
└──────────────────┘                  └──────────────────┘
        │                                         │
        ↓                                         ↓
┌──────────────────┐                  ┌──────────────────┐
│ Decode Process   │                  │ Generate Audio   │
│ runDecode()      │                  │ GenerateFT8      │
│ ├── Initial      │                  │                  │
│ ├── Deep (opt)   │                  └──────────────────┘
│ └── Deduplicate  │                            │
└──────────────────┘                            ↓
        │                            ┌──────────────────┐
        ↓                            │ Transmit Audio   │
┌──────────────────┐                 │ AudioTrack/      │
│ Process Messages │                 │ Network/CAT      │
│ afterDecode()    │                 └──────────────────┘
│ ├── Deduplicate │
│ ├── Auto-seq     │
│ └── Queue mgmt   │
└──────────────────┘
        │
        ↓
┌──────────────────┐
│ Update UI        │
│ Message List     │
│ Queue Display    │
│ Spectrum         │
└──────────────────┘
```

### Auto-Sequencing Flow

```
┌─────────────────────────────────────────────────────────────┐
│         parseMessageToFunction(messages)                      │
└─────────────────────────────────────────────────────────────┘
                            ↓
        ┌───────────────────┴───────────────────┐
        │                                         │
        ↓                                         ↓
┌──────────────────┐                  ┌──────────────────┐
│ Response Found   │                  │ No Response      │
│ newOrder != -1   │                  │ newOrder == -1    │
└──────────────────┘                  └──────────────────┘
        │                                         │
        ↓                                         ↓
┌──────────────────┐                  ┌──────────────────┐
│ Reset noReply    │                  │ Check New Calls  │
│ Update QSL       │                  │ checkCQMe...()   │
│ Advance Sequence │                  │                   │
│ functionOrder++  │                  └──────────────────┘
└──────────────────┘                            │
        │                                        ↓
        │                            ┌──────────────────┐
        │                            │ Increment        │
        │                            │ noReplyCount     │
        │                            └──────────────────┘
        │                                        │
        │                                        ↓
        │                            ┌──────────────────┐
        │                            │ Timeout?         │
        │                            │ noReplyCount >=  │
        │                            │ noReplyLimit?    │
        │                            └──────────────────┘
        │                                        │
        │                                        ↓ Yes
        │                            ┌──────────────────┐
        │                            │ Process Queue   │
        │                            │ or Reset to CQ  │
        │                            └──────────────────┘
        │
        ↓
┌──────────────────┐
│ Update UI        │
│ functionOrder    │
│ Queue Display    │
└──────────────────┘
```

### Queue Processing Flow

```
┌─────────────────────────────────────────────────────────────┐
│         Someone Calls Us                                     │
│         checkCQMeOrFollowCQMessage()                        │
└─────────────────────────────────────────────────────────────┘
                            ↓
        ┌───────────────────┴───────────────────┐
        │                                         │
        ↓                                         ↓
┌──────────────────┐                  ┌──────────────────┐
│ Active QSO?      │                  │ No Active QSO    │
│ Same Callsign?   │                  │ or Different      │
└──────────────────┘                  └──────────────────┘
        │                                         │
        │ Yes                                     ↓
        │                            ┌──────────────────┐
        │                            │ Add to Queue    │
        │                            │ callsignQueue   │
        │                            │ .addCallsign()  │
        │                            └──────────────────┘
        │                                        │
        │                                        ↓
        │                            ┌──────────────────┐
        │                            │ Update UI        │
        │                            │ Queue Display    │
        │                            └──────────────────┘
        │
        ↓
┌──────────────────┐
│ Respond          │
│ Immediately      │
│ setTransmit()    │
└──────────────────┘
```

```
┌─────────────────────────────────────────────────────────────┐
│         QSO Completes or Times Out                          │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│         Remove from Queue                                    │
│         callsignQueue.removeCallsign(toCallsign.callsign)   │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│         Get Next from Queue                                  │
│         next = callsignQueue.getNext()                      │
│         (oldest first)                                       │
└─────────────────────────────────────────────────────────────┘
                            ↓
        ┌───────────────────┴───────────────────┐
        │                                         │
        ↓                                         ↓
┌──────────────────┐                  ┌──────────────────┐
│ Queue Not Empty  │                  │ Queue Empty      │
└──────────────────┘                  └──────────────────┘
        │                                         │
        ↓                                         ↓
┌──────────────────┐                  ┌──────────────────┐
│ Start QSO with   │                  │ Check for New     │
│ Next Callsign    │                  │ Calls or Reset    │
│ setTransmit(...)  │                  │ to CQ             │
└──────────────────┘                  └──────────────────┘
```

---

## Key Design Decisions

### 1. Message Processing Order (Newest First)

**Decision**: Process messages in reverse order (newest first)

**Rationale**:
- FT8 messages added chronologically
- Newest message reflects current QSO state
- Prevents responding to stale messages
- Efficient (returns on first match)

### 2. Message Generation Timing

**Decision**: Generate transmission message at start of cycle, not during transmission

**Rationale**:
- FT8 requires time-synchronized 15-second cycles
- Changing mid-cycle would break timing
- Ensures complete message transmission
- Responds to new calls at proper time (next cycle)

### 3. Multi-Factor Deduplication

**Decision**: Use message text, callsigns, frequency, and time offset for duplicate detection

**Rationale**:
- Text-only caused false positives
- Different stations can have same message text
- Multi-factor ensures only true duplicates filtered

### 4. Batch Processing for Busy Bands

**Decision**: Process candidates in batches with signal subtraction

**Rationale**:
- Native library limits to 120 candidates
- Busy bands have >120 signals
- Batch processing allows decoding hundreds of signals
- Signal subtraction prevents re-processing

### 5. Queue Processing (Oldest First)

**Decision**: Process queued callsigns oldest first (FIFO)

**Rationale**:
- Fair processing order
- First-come-first-served
- Prevents missing early callers
- User can manually reorder if needed

---

## Thread Safety

### Synchronized Operations

1. **CallsignQueue**: All operations are `synchronized`
2. **Message Lists**: `synchronized (ft8Messages)` blocks
3. **Timer Callbacks**: Execute in thread pool

### Thread Pools

1. **Decode Thread**: New thread per decode cycle
2. **Transmit Thread Pool**: `ExecutorService` for transmission
3. **QTH Query Thread Pool**: Background callsign location queries
4. **Wave Data Thread Pool**: Network audio transmission

---

## Performance Considerations

### Decode Time Budget

- **Target**: Complete decode within 15-second slot
- **Deadline**: `utc + (FT8_SLOT_TIME * 2) - 1000ms`
- **Early Exit**: Stop processing if deadline approaching
- **Monitoring**: Optional toast for decode overruns

### Memory Management

- **Message Lists**: Limited size via `deleteArrayListMore()`
- **Audio Buffers**: Dynamic sizing with padding/trimming
- **Queue**: Unbounded (typically small, <10 items)

### CPU Usage

- **Deep Decode**: Optional, can be CPU-intensive
- **Batch Processing**: May increase CPU on very busy bands
- **Live Updates**: Optional UI updates during decode

---

## Configuration Options

### Auto-Sequencing

- **Auto Reply**: `GeneralVariables.autoCallFollow`
- **Auto Follow CQ**: `GeneralVariables.autoFollowCQ`
- **No Reply Limit**: `GeneralVariables.noReplyLimit` (0 = ignore, N = retry N times)

### Decoding

- **Deep Decode Mode**: `GeneralVariables.deepDecodeMode`
- **Decode Overrun Toast**: `GeneralVariables.decode_overrun_toast`

### Transmission

- **Manual Timeslot**: `GeneralVariables.manualTimeslot` (-1 = auto, 0 = odd, 1 = even)
- **Sync Frequency**: `GeneralVariables.synFrequency`

---

## Related Documentation

- [Auto Sequencing Improvements](AUTO_SEQUENCING_IMPROVEMENTS.md) - Detailed auto sequencing fixes and improvements
- [Callsign Queue Feature](CALLSIGN_QUEUE.md) - Complete queue feature documentation
- [Decode Improvements](DECODE_IMPROVEMENTS.md) - Decoding system enhancements
- [Release Notes](RELEASE_NOTES.md) - Version history and changes
- [User Guide](USER_GUIDE.md) - End-user documentation

---

**Last Updated**: 2026-01-26
**Version**: Based on FT8CN codebase analysis
