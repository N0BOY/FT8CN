# WSJT-X "Fake It" vs FT8CN "Adjust Rig Freq" - Technical Comparison

## Executive Summary

This document provides a detailed technical comparison between WSJT-X's "Fake It" split mode emulation and FT8CN's "Adjust Rig Freq for Audio Freq" feature. While both features involve temporary rig frequency adjustments during transmission, they serve different purposes and use different architectural approaches.

---

## 1. Feature Overview

### WSJT-X "Fake It" (Split Mode Emulation)

**Purpose:** Emulate hardware split operation for radios that don't support dual VFO operation.

**Use Case:** Enables advanced features (Fox/Hound mode, Doppler tracking) that require split operation on radios without true VFO A/B capability.

**User Benefit:** Keeps TX audio in the optimal 1500-2000 Hz range for cleaner transmission through the sideband filter.

### FT8CN "Adjust Rig Freq for Audio Freq"

**Purpose:** Allow users to transmit at different audio frequencies without manually adjusting the rig frequency.

**Use Case:** Simplifies operation by automatically adjusting the rig's VFO to match the selected audio frequency during transmission.

**User Benefit:** Always transmits at 1500 Hz (center frequency) regardless of selected audio frequency, providing optimal audio quality.

---

## 2. Architectural Comparison

### WSJT-X Architecture: Decorator Pattern

**Design Pattern:** Software Decorator
**Implementation:** `EmulateSplitTransceiver` wraps the actual `Transceiver` object

**Key Files:**
- `/Transceiver/EmulateSplitTransceiver.hpp` - Header
- `/Transceiver/EmulateSplitTransceiver.cpp` - Implementation
- `/Transceiver/TransceiverFactory.cpp` - Factory creates decorator

**Code Structure:**
```cpp
// Factory instantiation (TransceiverFactory.cpp:188-196)
if (split_mode_emulate == params.split_mode)
{
  // Wrap the Transceiver with emulation decorator
  result.reset (new EmulateSplitTransceiver {&logger_, std::move (result)});
}
```

**Advantages:**
- Clean separation of concerns
- Transparent to application logic
- Can be added/removed without changing core transceiver code
- Intercepts all state updates automatically

**How It Works:**
1. Application requests split mode (RX on one freq, TX on another)
2. Decorator intercepts the request
3. Stores both RX and TX frequencies internally
4. When PTT activates: Tells rig to use TX frequency
5. When PTT deactivates: Tells rig to use RX frequency
6. Always reports "no split" to the actual rig

### FT8CN Architecture: Callback-Based

**Design Pattern:** Callback/Hook Pattern
**Implementation:** Direct implementation in `MainViewModel` callbacks

**Key Files:**
- `/ft8cn/app/src/main/java/com/bg7yoz/ft8cn/MainViewModel.java` - Implementation
- `/ft8cn/app/src/main/java/com/bg7yoz/ft8cn/GeneralVariables.java` - Configuration & logic
- `/ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ui/ConfigFragment.java` - UI control

**Code Structure:**
```java
// Callback implementation (MainViewModel.java:448-477)
@Override
public void onBeforeTransmit(Ft8Message message, int functionOder) {
    if (GeneralVariables.adjustRigFreqForAudioFreq && ...) {
        // Store original frequency
        originalRigFrequency = baseRig.getFreq();

        // Calculate and set new frequency
        long offset = Math.round(audioFreq - 1500.0f);
        long newRigFreq = GeneralVariables.band + offset;
        baseRig.setFreq(newRigFreq);
        baseRig.setFreqToRig();
    }
}

@Override
public void onAfterTransmit(Ft8Message message, int functionOder) {
    if (GeneralVariables.adjustRigFreqForAudioFreq && ...) {
        // Restore original frequency
        baseRig.setFreq(originalRigFrequency);
        baseRig.setFreqToRig();
    }
}
```

**Advantages:**
- Simple and direct implementation
- Easy to understand and debug
- Minimal architectural overhead
- Works with existing rig control infrastructure

**How It Works:**
1. User selects audio frequency (e.g., 1800 Hz)
2. Before transmission: Calculate offset from center (1800 - 1500 = 300 Hz)
3. Adjust rig frequency by offset (band + 300 Hz)
4. Generate audio signal at 1500 Hz (center frequency)
5. After transmission: Restore original rig frequency

---

## 3. Frequency Management

### WSJT-X Frequency Management

**RX/TX Frequency Relationship:**
| Mode | RX Frequency | TX Frequency | Audio Frequency |
|------|--------------|--------------|-----------------|
| None | Same | Same | Variable (0-3000 Hz) |
| Rig (Hardware Split) | VFO A | VFO B | 1500-2000 Hz (recommended) |
| Fake It | VFO A (stored) | VFO A (switched) | 1500-2000 Hz (recommended) |

**State Management (EmulateSplitTransceiver.cpp:24-39):**
```cpp
void EmulateSplitTransceiver::set (TransceiverState const& s, unsigned sequence_number)
{
  // Save requested frequencies
  rx_frequency_ = s.frequency();
  tx_frequency_ = s.tx_frequency();
  split_ = s.split();

  // Create emulated state for rig
  TransceiverState emulated_state {s};

  // If transmitting AND split requested, use TX frequency
  if (s.ptt() && split_)
    emulated_state.frequency(s.tx_frequency());

  // Tell rig "no split" - we handle it in software
  emulated_state.split(false);
  emulated_state.tx_frequency(0);

  // Send to actual rig
  wrapped_->set(emulated_state, sequence_number);
}
```

**Key Insight:** WSJT-X maintains two separate frequency values and switches the rig's VFO between them based on PTT state.

### FT8CN Frequency Management

**Audio vs Rig Frequency Relationship:**
| Feature Enabled | Rig Frequency (VFO) | Audio Frequency | Actual TX Freq |
|-----------------|---------------------|-----------------|----------------|
| No | Band (e.g., 14.074 MHz) | Variable (e.g., 1800 Hz) | Band + Audio (14.0758 MHz) |
| Yes | Band + Offset (14.0743 MHz) | 1500 Hz (center) | Band + Offset + 1500 (14.0758 MHz) |

**Calculation Logic (MainViewModel.java:461-468):**
```java
// Store original frequency
originalRigFrequency = baseRig.getFreq();

// Calculate offset from center (1500 Hz)
float audioFreq = GeneralVariables.getBaseFrequency(); // e.g., 1800 Hz
long offset = Math.round(audioFreq - 1500.0f);         // 1800 - 1500 = 300 Hz
long newRigFreq = GeneralVariables.band + offset;      // 14074000 + 300

// Apply to rig
baseRig.setFreq(newRigFreq);
baseRig.setFreqToRig();
```

**Audio Generation Logic (GeneralVariables.java:259-264):**
```java
public static float getAudioFrequencyForGeneration() {
    if (adjustRigFreqForAudioFreq && controlMode != ControlMode.VOX) {
        return 1500.0f; // Always center frequency
    }
    return baseFrequency; // User-selected frequency
}
```

**Key Insight:** FT8CN adjusts the rig frequency so that audio can always be generated at 1500 Hz, achieving the same final transmission frequency.

---

## 4. Audio Frequency Philosophy

### WSJT-X: Split Mode for Clean Transmission

**Rationale (from settings-radio.adoc):**
> "Either method [hardware split or Fake It] will result in a cleaner transmitted signal,
> by keeping the Tx audio always in the range 1500 to 2000 Hz so that audio harmonics
> cannot pass through the Tx sideband filter."

**Audio Range:**
- **Recommended:** 1500-2000 Hz
- **Why:** Center of SSB filter passband
- **Benefit:** Harmonics (3000-6000 Hz) are outside filter, cleaner signal

**Implementation:**
- User doesn't directly control TX audio frequency
- Application automatically manages to keep in optimal range
- Works in conjunction with frequency coordination algorithms

### FT8CN: Fixed Center Frequency

**Rationale:**
- Always transmit at 1500 Hz (exact center of FT8 bandwidth)
- Optimal position in SSB filter
- Consistent audio characteristics regardless of selected frequency

**Audio Range:**
- **Fixed:** 1500 Hz when feature is enabled
- **Variable:** 0-2900 Hz when feature is disabled
- **Why:** Maximum audio quality and consistent performance

**Implementation:**
- `getAudioFrequencyForGeneration()` returns 1500 Hz when enabled
- All rig types (Icom, XieGu, Flex, etc.) use same logic
- Combined with `transmitOffsetHz` for calibration

---

## 5. PTT and State Control

### WSJT-X PTT State Flow

**State Interception (EmulateSplitTransceiver::handle_update:41-69):**
```cpp
void EmulateSplitTransceiver::handle_update (TransceiverState const& state,
                                             unsigned sequence_number)
{
  if (state.split()) {
    // ERROR: Rig shouldn't be in split mode
    Q_EMIT failure (tr ("Emulated split mode requires rig to be in simplex mode"));
  }
  else {
    TransceiverState new_state {state};

    // If transmitting, show RX frequency to application
    if (state.ptt())
      new_state.frequency(rx_frequency_);

    // Always restore application's requested state
    new_state.tx_frequency(tx_frequency_);
    new_state.split(split_);

    // Signal emulated state back to application
    Q_EMIT update(new_state, sequence_number);
  }
}
```

**Flow:**
1. Application wants to transmit
2. Application sets `ptt(true)` and `split(true)`
3. Decorator intercepts
4. Decorator tells rig: `ptt(true)`, `frequency(tx_freq)`, `split(false)`
5. Rig transmits on TX frequency
6. Application thinks rig is in split mode (emulated state)

**Error Handling:**
- Validates rig is not in actual split mode
- Gracefully handles rigs that don't support split commands
- Fails with clear error message if rig doesn't cooperate

### FT8CN PTT Control Flow

**Before Transmission (MainViewModel.java:449-493):**
```java
public void onBeforeTransmit(Ft8Message message, int functionOder) {
    // 1. Adjust rig frequency (if enabled)
    if (GeneralVariables.adjustRigFreqForAudioFreq && ...) {
        originalRigFrequency = baseRig.getFreq();
        long newRigFreq = GeneralVariables.band + offset;
        baseRig.setFreq(newRigFreq);
        baseRig.setFreqToRig();
    }

    // 2. Activate PTT (CAT/RTS/DTR modes)
    if (GeneralVariables.controlMode == ControlMode.CAT
            || GeneralVariables.controlMode == ControlMode.RTS
            || GeneralVariables.controlMode == ControlMode.DTR) {
        if (baseRig != null) {
            baseRig.setPTT(true);
        }
    }
}
```

**After Transmission (MainViewModel.java:496-527):**
```java
public void onAfterTransmit(Ft8Message message, int functionOder) {
    // 1. Deactivate PTT
    if (GeneralVariables.controlMode == ControlMode.CAT
            || GeneralVariables.controlMode == ControlMode.RTS
            || GeneralVariables.controlMode == ControlMode.DTR) {
        if (baseRig != null) {
            baseRig.setPTT(false);
        }
    }

    // 2. Restore rig frequency (if adjusted)
    if (GeneralVariables.adjustRigFreqForAudioFreq && ...) {
        baseRig.setFreq(originalRigFrequency);
        baseRig.setFreqToRig();
        originalRigFrequency = 0; // Reset
    }
}
```

**Flow:**
1. User initiates transmission
2. `onBeforeTransmit()` called
3. Rig frequency adjusted (stored original)
4. PTT activated
5. Audio generated at 1500 Hz
6. Audio transmitted to rig
7. PTT deactivated
8. `onAfterTransmit()` called
9. Original rig frequency restored

**Mode Restrictions:**
- Only works in CAT, RTS, DTR modes
- Disabled in VOX mode (no rig control available)
- UI enforces this restriction automatically

---

## 6. Configuration and Persistence

### WSJT-X Configuration

**UI Location:** Settings → Radio → Split Operation
**File:** `Configuration.ui` (lines 743-791)

**Options:**
| Option | Value | Description |
|--------|-------|-------------|
| None | `split_mode_none` | No split operation |
| Rig | `split_mode_rig` | Use hardware split (VFO A+B) |
| Fake It | `split_mode_emulate` | Software emulation |

**Settings Persistence:**
```cpp
// Load (Configuration.cpp:1734)
rig_params_.split_mode = settings_->value("SplitMode", ...).value<TransceiverFactory::SplitMode>();

// Save (Configuration.cpp:1871)
settings_->setValue("SplitMode", QVariant::fromValue(rig_params_.split_mode));
```

**UI Binding:**
```cpp
// Configuration.cpp:1246-1248
ui_->split_mode_button_group->setId(ui_->split_none_radio_button,
                                    TransceiverFactory::split_mode_none);
ui_->split_mode_button_group->setId(ui_->split_rig_radio_button,
                                    TransceiverFactory::split_mode_rig);
ui_->split_mode_button_group->setId(ui_->split_emulate_radio_button,
                                    TransceiverFactory::split_mode_emulate);
```

### FT8CN Configuration

**UI Location:** Settings → Frequency Tab
**File:** `fragment_config.xml` (line 295)

**UI Element:**
```xml
<Switch
    android:id="@+id/adjustRigFreqForAudioFreqSwitch"
    android:text="@string/adjustRigFreqForAudioFreq"
    android:checked="false" />
```

**Settings Persistence:**
```java
// Load (DatabaseOpr.java:2410-2412)
if (name.equalsIgnoreCase("adjustRigFreqForAudioFreq")) {
    GeneralVariables.adjustRigFreqForAudioFreq =
        !(result == null || result.equals("") || result.equals("0"));
}

// Save (ConfigFragment.java:619-623)
if (binding.adjustRigFreqForAudioFreqSwitch.isChecked()) {
    mainViewModel.databaseOpr.writeConfig("adjustRigFreqForAudioFreq", "1", null);
} else {
    mainViewModel.databaseOpr.writeConfig("adjustRigFreqForAudioFreq", "0", null);
}
GeneralVariables.adjustRigFreqForAudioFreq =
    binding.adjustRigFreqForAudioFreqSwitch.isChecked();
```

**Mode-Dependent Behavior:**
```java
// ConfigFragment.java:1701-1710
if (GeneralVariables.controlMode == ControlMode.VOX) {
    binding.adjustRigFreqForAudioFreqSwitch.setEnabled(false);
    if (binding.adjustRigFreqForAudioFreqSwitch.isChecked()) {
        binding.adjustRigFreqForAudioFreqSwitch.setChecked(false);
        GeneralVariables.adjustRigFreqForAudioFreq = false;
        mainViewModel.databaseOpr.writeConfig("adjustRigFreqForAudioFreq", "0", null);
    }
} else {
    binding.adjustRigFreqForAudioFreqSwitch.setEnabled(true);
}
```

---

## 7. User Experience Comparison

### WSJT-X "Fake It"

**When to Use:**
- Radio doesn't support dual VFO (split) operation
- Want to use Fox/Hound mode
- Need Doppler tracking for EME (satellite)
- Want cleaner TX signal

**User Interaction:**
1. Select "Fake It" in Radio settings
2. Application automatically manages frequencies
3. User operates normally (can't tell it's emulated)
4. Brief frequency switch during TX/RX transitions

**Limitations:**
- Slight latency during TX/RX switching
- Cannot monitor RX frequency while transmitting
- May need experimentation with specific rigs
- Some rigs may not handle rapid frequency changes well

**Warnings:**
```cpp
// mainwindow.cpp:7227
if((SpecOp::FOX==m_specOp or SpecOp::HOUND==m_specOp)
   and !m_config.split_mode() and !m_bWarnedSplit) {
  MessageBox::critical_message (this,
     "Operation in FT8 DXpedition mode normally requires\n"
     " *Split* rig control (either *Rig* or *Fake It* on\n"
     "the *Settings | Radio* tab.)");
}
```

### FT8CN "Adjust Rig Freq"

**When to Use:**
- Want to transmit at different audio frequencies
- Want optimal audio quality (1500 Hz)
- Don't want to manually adjust rig VFO
- Using CAT/RTS/DTR control mode

**User Interaction:**
1. Enable "Adjust rig freq for audio freq" switch
2. Select any audio frequency (0-2900 Hz)
3. Rig frequency automatically adjusts during TX
4. Toast messages show frequency changes
5. Original frequency restored after TX

**Limitations:**
- Only works with CAT/RTS/DTR modes (not VOX)
- Frequency switch happens on each transmission
- User sees toast notifications (can be verbose)
- Requires stable rig control connection

**User Feedback:**
```java
// MainViewModel.java:472-473
ToastMessage.show(String.format(
    getStringFromResource(R.string.rig_freq_adjusted),
    BaseRigOperation.getFrequencyAllInfo(newRigFreq),
    audioFreq));

// MainViewModel.java:519-520
ToastMessage.show(String.format(
    getStringFromResource(R.string.rig_freq_restored),
    BaseRigOperation.getFrequencyAllInfo(originalRigFrequency)));
```

**Help Text:**
- `adjustRigFreqForAudioFreqSummary`: Explains feature and requirements
- `adjustRigFreqForAudioFreqVoxWarning`: Warns that VOX mode is incompatible

---

## 8. Technical Implementation Details

### WSJT-X: Decorator Transparency

**Transceiver Interface Abstraction:**
All transceiver operations go through a common interface. The decorator pattern allows "Fake It" to intercept these operations without the application knowing.

**Polymorphism:**
```cpp
// TransceiverFactory.cpp creates appropriate implementation
std::unique_ptr<Transceiver> result;

// Could be HamlibTransceiver, DXLabSuiteCommanderTransceiver, etc.
result = /* create base transceiver */;

// Optionally wrap with decorator
if (split_mode_emulate == params.split_mode) {
  result.reset(new EmulateSplitTransceiver{&logger_, std::move(result)});
}

// Application uses result the same way regardless
```

**State Synchronization:**
The decorator maintains its own view of the "true" state (with split) while presenting a different state to the rig (without split).

### FT8CN: Direct Callback Implementation

**Callback Chain:**
```
FT8TransmitSignal
  ↓
onDoTransmitted callback
  ↓
MainViewModel.onBeforeTransmit()
  ↓
baseRig.setFreq() / baseRig.setFreqToRig()
  ↓
BaseRig implementation (IcomRig, XieGuRig, etc.)
  ↓
Actual rig control (CAT, network, etc.)
```

**State Storage:**
```java
// MainViewModel.java - Class member
private long originalRigFrequency = 0;

// Store before TX
originalRigFrequency = baseRig.getFreq();

// Restore after TX
baseRig.setFreq(originalRigFrequency);
originalRigFrequency = 0; // Reset
```

**Integration with Audio Generation:**
All rig types (network and sound card) use the same audio frequency logic:
```java
// FT8TransmitSignal.java:438
float transmitFreq = GeneralVariables.getAudioFrequencyForGeneration()
                   + GeneralVariables.transmitOffsetHz;

// IcomRig.java:142, XieGuRig.java:277, FlexNetworkRig.java:90, etc.
float transmitFreq = GeneralVariables.getAudioFrequencyForGeneration()
                   + GeneralVariables.transmitOffsetHz;
float[] data = GenerateFT8.generateFt8(message, transmitFreq, sampleRate);
```

---

## 9. Compatibility and Error Handling

### WSJT-X Error Handling

**Rig Compatibility:**
```cpp
// HamlibTransceiver.cpp:955-958
// Fake split mode and non-split mode work without error
// on rigs that don't support split commands
m_->error_check(rc, tr("setting/unsetting split mode"));
```

**Validation:**
```cpp
// EmulateSplitTransceiver::handle_update:44-47
if (state.split()) {
  Q_EMIT failure(tr("Emulated split mode requires rig to be in simplex mode"));
}
```

**Graceful Degradation:**
- If rig enters split mode on its own, emit error
- If rig doesn't support frequency changes, try anyway
- Hamlib abstraction layer handles rig-specific quirks

### FT8CN Error Handling

**Mode Validation:**
```java
// ConfigFragment.java:1701-1710
if (GeneralVariables.controlMode == ControlMode.VOX) {
    // Disable switch
    binding.adjustRigFreqForAudioFreqSwitch.setEnabled(false);
    // Force off if was on
    if (binding.adjustRigFreqForAudioFreqSwitch.isChecked()) {
        binding.adjustRigFreqForAudioFreqSwitch.setChecked(false);
        GeneralVariables.adjustRigFreqForAudioFreq = false;
    }
}
```

**Connection Validation:**
```java
// MainViewModel.java:451-454
if (GeneralVariables.adjustRigFreqForAudioFreq
        && GeneralVariables.controlMode != ControlMode.VOX
        && baseRig != null
        && baseRig.isConnected()) {
```

**Rig Control Errors:**
- If `baseRig.setFreq()` fails, error is logged but not fatal
- User sees toast message with attempted frequency
- Original frequency restoration is attempted regardless
- If restoration fails, frequency remains at TX frequency

---

## 10. Performance Characteristics

### WSJT-X Performance

**Frequency Switching:**
- Happens during PTT state change
- Latency depends on rig's CAT response time
- Typically 50-200ms per switch

**State Updates:**
- All state changes intercepted by decorator
- Minimal overhead (pointer indirection)
- No observable performance impact

**Memory:**
- Decorator maintains 3 extra variables:
  - `rx_frequency_`
  - `tx_frequency_`
  - `split_`

### FT8CN Performance

**Frequency Switching:**
- Happens in `onBeforeTransmit()` callback
- Two CAT commands: `setFreq()` + `setFreqToRig()`
- Latency: 100-300ms depending on rig

**Toast Messages:**
- Two toast messages per transmission
- May impact UI responsiveness
- Log messages for debugging

**Memory:**
- Single `long` variable: `originalRigFrequency`
- Minimal overhead

---

## 11. Key Differences Summary

| Aspect | WSJT-X "Fake It" | FT8CN "Adjust Rig Freq" |
|--------|------------------|-------------------------|
| **Purpose** | Emulate hardware split for advanced features | Simplify frequency selection |
| **Architecture** | Decorator pattern | Callback-based |
| **Complexity** | High (design pattern, state management) | Low (direct implementation) |
| **Transparency** | Fully transparent to application | Explicit in callbacks |
| **Frequency Logic** | Maintains separate RX/TX frequencies | Calculates offset from audio freq |
| **Audio Frequency** | Recommends 1500-2000 Hz | Fixed at 1500 Hz when enabled |
| **Split Mode** | Emulates true split operation | Not related to split mode |
| **Mode Restrictions** | Works with any control mode | Requires CAT/RTS/DTR (not VOX) |
| **User Feedback** | None (transparent) | Toast messages on every TX |
| **Configuration** | 3 options (None/Rig/Fake It) | Boolean switch (On/Off) |
| **Error Handling** | Comprehensive validation | Basic mode validation |
| **State Management** | Maintains emulated split state | Stores/restores single frequency |
| **Use Cases** | Fox/Hound, Doppler, no dual VFO | Convenience, optimal audio quality |

---

## 12. Use Case Scenarios

### Scenario 1: Radio Without Dual VFO

**WSJT-X Approach:**
- Enable "Fake It" split mode
- Application manages RX and TX frequencies
- User operates as if radio has split capability
- Required for Fox/Hound mode

**FT8CN Approach:**
- Not designed for this use case
- Feature works with or without dual VFO
- Doesn't emulate split operation
- Simply adjusts single VFO temporarily

### Scenario 2: Transmitting at Different Audio Frequencies

**WSJT-X Approach:**
- User doesn't directly select audio frequency
- Application coordinates with other stations
- Split mode helps keep audio in optimal range
- Audio typically 1500-2000 Hz

**FT8CN Approach:**
- User selects any audio frequency (0-2900 Hz)
- Enable "Adjust Rig Freq" feature
- Rig frequency auto-adjusts during TX
- Audio always generated at 1500 Hz

### Scenario 3: Optimal Audio Quality

**WSJT-X Approach:**
- Split mode (hardware or Fake It) recommended
- Keeps TX audio in 1500-2000 Hz range
- Prevents harmonics from passing through filter
- Cleaner transmitted signal

**FT8CN Approach:**
- "Adjust Rig Freq" forces 1500 Hz audio
- Center of FT8 bandwidth
- Optimal filter position
- Combined with `transmitOffsetHz` for calibration

### Scenario 4: VOX Mode Operation

**WSJT-X Approach:**
- "Fake It" works in VOX mode
- No PTT control needed for frequency switching
- Audio triggers PTT, software manages frequency
- May have timing challenges

**FT8CN Approach:**
- Feature disabled in VOX mode
- No rig control available to adjust frequency
- UI enforces this restriction
- User must use standard audio frequency selection

---

## 13. Advantages and Disadvantages

### WSJT-X "Fake It"

**Advantages:**
✅ Enables advanced features (Fox/Hound, Doppler)
✅ Transparent to user and application logic
✅ Clean architectural pattern
✅ Works with most rigs
✅ Comprehensive error handling
✅ Maintains separate RX/TX frequencies

**Disadvantages:**
❌ High complexity (design pattern overhead)
❌ Cannot monitor RX while transmitting
❌ Frequency switch latency
❌ May not work well with all rig models
❌ Harder to debug (state is hidden)

### FT8CN "Adjust Rig Freq"

**Advantages:**
✅ Simple, easy to understand
✅ Always uses optimal audio frequency (1500 Hz)
✅ User has explicit control (on/off switch)
✅ Works with existing rig infrastructure
✅ Minimal code changes required
✅ Clear user feedback (toast messages)

**Disadvantages:**
❌ Only works with CAT/RTS/DTR modes
❌ Verbose toast messages
❌ Frequency restored after every TX
❌ No true split mode emulation
❌ Not suitable for advanced features
❌ Requires stable rig control

---

## 14. Code Architecture Diagrams

### WSJT-X "Fake It" Architecture

```
┌─────────────────────────────────────────────────┐
│         WSJT-X Application                      │
│  (mainwindow, modulator, etc.)                  │
└──────────────────┬──────────────────────────────┘
                   │ set(state)
                   │ get_state()
                   ▼
┌─────────────────────────────────────────────────┐
│    EmulateSplitTransceiver (Decorator)          │
│  ┌──────────────────────────────────────────┐   │
│  │ - rx_frequency_                          │   │
│  │ - tx_frequency_                          │   │
│  │ - split_                                 │   │
│  │                                          │   │
│  │ set():                                   │   │
│  │   if (ptt && split)                      │   │
│  │     rig.frequency = tx_frequency         │   │
│  │   else                                   │   │
│  │     rig.frequency = rx_frequency         │   │
│  │   rig.split = false                      │   │
│  │                                          │   │
│  │ handle_update():                         │   │
│  │   app_state.split = split_               │   │
│  │   app_state.tx_frequency = tx_frequency_ │   │
│  └──────────────────────────────────────────┘   │
└──────────────────┬──────────────────────────────┘
                   │ set(modified_state)
                   │ get_state()
                   ▼
┌─────────────────────────────────────────────────┐
│    HamlibTransceiver (Actual Rig Control)       │
│  (Communicates with rig via CAT/Hamlib)         │
└──────────────────┬──────────────────────────────┘
                   │ CAT commands
                   ▼
┌─────────────────────────────────────────────────┐
│              Radio Hardware                     │
└─────────────────────────────────────────────────┘
```

### FT8CN "Adjust Rig Freq" Architecture

```
┌─────────────────────────────────────────────────┐
│         FT8TransmitSignal                       │
│  (Manages transmission logic)                   │
└──────────────────┬──────────────────────────────┘
                   │ onDoTransmitted callbacks
                   ▼
┌─────────────────────────────────────────────────┐
│         MainViewModel                           │
│  ┌──────────────────────────────────────────┐   │
│  │ - originalRigFrequency                   │   │
│  │                                          │   │
│  │ onBeforeTransmit():                      │   │
│  │   if (adjustRigFreqForAudioFreq)         │   │
│  │     originalFreq = rig.getFreq()         │   │
│  │     offset = audioFreq - 1500            │   │
│  │     rig.setFreq(band + offset)           │   │
│  │   rig.setPTT(true)                       │   │
│  │                                          │   │
│  │ onAfterTransmit():                       │   │
│  │   rig.setPTT(false)                      │   │
│  │   if (adjustRigFreqForAudioFreq)         │   │
│  │     rig.setFreq(originalFreq)            │   │
│  └──────────────────────────────────────────┘   │
└──────────────────┬──────────────────────────────┘
                   │ setFreq(), setPTT()
                   ▼
┌─────────────────────────────────────────────────┐
│         BaseRig (IcomRig, XieGuRig, etc.)       │
│  (Rig-specific control implementation)          │
└──────────────────┬──────────────────────────────┘
                   │ CAT/Network commands
                   ▼
┌─────────────────────────────────────────────────┐
│              Radio Hardware                     │
└─────────────────────────────────────────────────┘
```

---

## 15. Frequency Calculation Examples

### WSJT-X "Fake It" Example

**Scenario:** Transmitting on 14.080 MHz while receiving on 14.074 MHz

```
Initial State:
- RX Frequency: 14.074 MHz
- TX Frequency: 14.080 MHz
- Split: true
- PTT: false

Decorator State:
- rx_frequency_ = 14.074 MHz
- tx_frequency_ = 14.080 MHz
- split_ = true

To Rig (RX):
- Frequency: 14.074 MHz
- Split: false
- PTT: false

User presses transmit:
- Application: ptt = true

Decorator intercepts:
- Because (ptt && split): use tx_frequency_

To Rig (TX):
- Frequency: 14.080 MHz  ← Changed
- Split: false
- PTT: true

Audio generated at: ~1750 Hz (application choice)
Actual TX frequency: 14.080 MHz + 0.00175 MHz = 14.08175 MHz

User releases transmit:
- Application: ptt = false

To Rig (back to RX):
- Frequency: 14.074 MHz  ← Restored
- Split: false
- PTT: false
```

### FT8CN "Adjust Rig Freq" Example

**Scenario:** User selects 1800 Hz audio, feature enabled

```
Initial State:
- Band: 14.074 MHz
- baseFrequency: 1800 Hz (user selection)
- adjustRigFreqForAudioFreq: true
- originalRigFrequency: 0 (not set)

User presses transmit:

onBeforeTransmit():
1. Store original:
   originalRigFrequency = baseRig.getFreq() = 14.074 MHz

2. Calculate offset:
   audioFreq = GeneralVariables.getBaseFrequency() = 1800 Hz
   offset = round(1800 - 1500) = 300 Hz

3. Calculate new rig frequency:
   newRigFreq = 14074000 + 300 = 14074300 Hz = 14.0743 MHz

4. Set rig frequency:
   baseRig.setFreq(14074300)
   baseRig.setFreqToRig()

5. Activate PTT:
   baseRig.setPTT(true)

Audio Generation:
- getAudioFrequencyForGeneration() returns 1500 Hz
- Audio generated at 1500 Hz (center frequency)

Actual TX frequency:
- Rig VFO: 14.0743 MHz
- Audio tone: 1500 Hz = 0.0015 MHz
- Total: 14.0743 + 0.0015 = 14.0758 MHz

Note: Same as if user had:
- Rig at 14.074 MHz
- Audio at 1800 Hz
- Feature disabled

onAfterTransmit():
1. Deactivate PTT:
   baseRig.setPTT(false)

2. Restore original frequency:
   baseRig.setFreq(14074000)
   baseRig.setFreqToRig()

3. Reset stored frequency:
   originalRigFrequency = 0
```

---

## 16. Integration with Other Features

### WSJT-X Integration

**Fox/Hound Mode:**
- Requires split operation (Hardware or Fake It)
- Warning displayed if split is not enabled
- "Fake It" allows Fox/Hound on non-split rigs

**Doppler Tracking:**
- Requires split mode for EME (Earth-Moon-Earth)
- Compensates for Doppler shift
- "Fake It" enables on radios without hardware split

**Frequency Coordination:**
- Works with frequency hopping algorithms
- Split mode helps manage TX frequency
- "Fake It" transparent to coordination logic

### FT8CN Integration

**transmitOffsetHz Calibration:**
```java
// All rig types apply both features together
float transmitFreq = GeneralVariables.getAudioFrequencyForGeneration()
                   + GeneralVariables.transmitOffsetHz;
```

**Example:**
- User selects 1800 Hz audio
- Adjust Rig Freq: enabled
- transmitOffsetHz: +50 Hz (calibration)

**Calculation:**
```
getAudioFrequencyForGeneration() = 1500 Hz (center)
transmitOffsetHz = 50 Hz
transmitFreq = 1500 + 50 = 1550 Hz

Rig frequency adjustment:
offset = 1800 - 1500 = 300 Hz
newRigFreq = 14074000 + 300 = 14074300 Hz

Audio generation:
Audio at 1550 Hz (center + calibration)

Final TX frequency:
14074300 + 1550 = 14075850 Hz
```

**Control Mode Interaction:**
- VOX: Feature disabled (no rig control)
- CAT: Full functionality
- RTS: Full functionality
- DTR: Full functionality

**Network Rig Support:**
- IcomRig (Icom network mode)
- XieGuRig (XieGu network mode)
- XieGu6100Rig (X6100 USB mode)
- XieGu6100NetRig (X6100 network mode)
- FlexNetworkRig (Flex radio network)
- TrUSDXRig (TrUSDX rig)

All network rig types use the same audio frequency logic.

---

## 17. Future Considerations

### Potential WSJT-X Enhancements

1. **Configurable Audio Range:** Allow users to set preferred audio range
2. **Per-Rig Profiles:** Save split mode preference per rig
3. **Timing Optimization:** Reduce frequency switching latency
4. **Status Indicator:** Show when "Fake It" is actively switching
5. **Fallback Modes:** Automatic fallback if rig doesn't cooperate

### Potential FT8CN Enhancements

1. **Reduce Toast Verbosity:** Option to disable frequency change toasts
2. **Frequency Smoothing:** Cache original frequency to avoid re-reading
3. **Error Recovery:** Better handling of rig control failures
4. **VOX Compatibility:** Explore alternative approaches for VOX mode
5. **Split Mode Emulation:** Add true split mode emulation like WSJT-X
6. **Timing Optimization:** Overlap frequency change with other operations
7. **User Preferences:** Allow users to choose toast notification level

---

## 18. Testing Recommendations

### WSJT-X "Fake It" Testing

**Test Cases:**
1. Verify frequency switches correctly on PTT
2. Test with rigs that don't support split
3. Verify Fox/Hound mode works with "Fake It"
4. Test rapid TX/RX transitions
5. Verify error handling when rig enters split mode
6. Test with different audio frequencies
7. Verify state synchronization

**Expected Behavior:**
- Transparent operation (user can't tell it's emulated)
- Smooth frequency transitions
- No state corruption
- Works with all Hamlib-supported rigs

### FT8CN "Adjust Rig Freq" Testing

**Test Cases:**
1. Enable feature, transmit at various audio frequencies
2. Verify 1500 Hz audio generation when enabled
3. Test frequency restoration after transmission
4. Verify VOX mode disables feature
5. Test mode switching (CAT ↔ VOX)
6. Test with different rig types (Icom, XieGu, Flex)
7. Verify integration with transmitOffsetHz
8. Test rig disconnection during transmission
9. Verify toast messages display correctly
10. Test multiple transmissions in sequence

**Expected Behavior:**
- Rig frequency adjusts before PTT
- Audio always at 1500 Hz (when enabled)
- Original frequency restored after PTT
- Toast messages show frequency changes
- Feature disabled in VOX mode
- Works with all supported rig types

---

## 19. Conclusion

WSJT-X's "Fake It" and FT8CN's "Adjust Rig Freq" serve different purposes despite both involving temporary rig frequency adjustments:

**WSJT-X "Fake It":**
- **Purpose:** Emulate hardware split operation
- **Architecture:** Sophisticated decorator pattern
- **Use Case:** Enable advanced features on non-split rigs
- **Philosophy:** Transparent, comprehensive emulation

**FT8CN "Adjust Rig Freq":**
- **Purpose:** Convenience feature for frequency selection
- **Architecture:** Simple callback-based implementation
- **Use Case:** Always transmit at optimal audio frequency
- **Philosophy:** Explicit, user-controlled operation

Both approaches have merit within their respective ecosystems. WSJT-X's approach is more architecturally elegant and enables advanced features, while FT8CN's approach is simpler, more direct, and focused on audio quality optimization.

---

## 20. References

### WSJT-X Files Referenced

| File | Purpose |
|------|---------|
| `/Transceiver/EmulateSplitTransceiver.hpp` | Header for split emulation |
| `/Transceiver/EmulateSplitTransceiver.cpp` | Implementation |
| `/Transceiver/TransceiverFactory.hpp` | Transceiver creation |
| `/Transceiver/TransceiverFactory.cpp` | Factory implementation |
| `/Configuration.ui` | UI definition |
| `/Configuration.cpp` | Configuration management |
| `/mainwindow.cpp` | Main application logic |
| `/doc/user_guide/en/settings-radio.adoc` | User documentation |

### FT8CN Files Referenced

| File | Purpose |
|------|---------|
| `MainViewModel.java` | Callback implementation |
| `GeneralVariables.java` | Configuration and logic |
| `ConfigFragment.java` | UI control |
| `DatabaseOpr.java` | Settings persistence |
| `FT8TransmitSignal.java` | Transmission management |
| `IcomRig.java`, `XieGuRig.java`, etc. | Rig-specific implementations |
| `fragment_config.xml` | UI layout |
| `strings.xml` | Localized strings |

---

## Document Information

- **Author:** Technical Analysis
- **Date:** 2026-02-02
- **Version:** 1.0
- **WSJT-X Version:** Latest from repository
- **FT8CN Branch:** offset-calibration
- **Purpose:** Technical comparison for development reference

