# "Fake It" Split Mode Implementation - Change Summary

## Overview

Updated FT8CN to match WSJT-X's "Fake It" split operation terminology and behavior. The feature now emulates WSJT-X's approach to split operation for radios that don't have dual VFO capability.

**Date:** 2026-02-02
**Branch:** offset-calibration

---

## Changes Made

### 1. Variable Renaming

**Old:** `adjustRigFreqForAudioFreq`
**New:** `fakeItSplit`

| File | Changes |
|------|---------|
| `GeneralVariables.java` | Renamed variable, updated comments to reference WSJT-X |
| `MainViewModel.java` | Updated all references, improved comments with WSJT-X calculation explanation |
| `DatabaseOpr.java` | Changed config key to "fakeItSplit", added legacy key support |
| `ConfigFragment.java` | Updated all widget IDs and references |
| `fragment_config.xml` | Changed Switch ID from `adjustRigFreqForAudioFreqSwitch` to `fakeItSplitSwitch` |
| `fragment_config.xml` | Changed ImageButton ID from `adjustRigFreqImageButton` to `fakeItSplitImageButton` |
| `strings.xml` | Added new strings, kept legacy strings for compatibility |

### 2. UI String Changes

#### New Strings

```xml
<string name="fake_it_split">Split operation: Fake it</string>
<string name="fake_it_split_summary">
  Emulate split operation by adjusting rig frequency during transmission.
  Audio is always generated at 1500 Hz for optimal quality.
  Matches WSJT-X "Fake It" behavior. Only works in CAT/RTS/DTR modes.
</string>
<string name="fake_it_split_vox_warning">
  This feature is not available in VOX mode (requires rig control)
</string>
<string name="fake_it_rig_adjusted">Fake It: Rig adjusted to %s (offset: %.0f Hz)</string>
<string name="fake_it_rig_restored">Fake It: Rig restored to %s</string>
```

#### Legacy String Compatibility

Old string names (`adjustRigFreqForAudioFreq`, etc.) are kept and point to new strings for backwards compatibility.

### 3. Calculation (Unchanged)

The calculation remains the same and matches WSJT-X's approach:

```java
// WSJT-X "Fake It" calculation:
// Target TX freq = dial + audio offset, but we transmit at 1500 Hz
// So adjust rig to: (dial + audio offset) - 1500 = dial + (audio - 1500)
float audioFreq = GeneralVariables.getBaseFrequency();
long offset = Math.round(audioFreq - 1500.0f);
long newRigFreq = GeneralVariables.band + offset;
```

**Example:**
- Dial frequency: 14.074 MHz
- Selected audio: 200 Hz
- Rig adjustment: 14.074 + (0.0002 - 0.0015) = 14.0727 MHz
- Audio generated: 1500 Hz
- Final TX frequency: 14.0727 + 0.0015 = 14.0742 MHz

This matches WSJT-X within rounding differences.

### 4. Code Comments Updated

All code comments now reference WSJT-X and "Fake It" terminology:

**Before:**
```java
// Adjust rig frequency if feature is enabled (before PTT activation)
```

**After:**
```java
// Fake It split mode: Emulate split operation like WSJT-X (before PTT activation)
```

### 5. Log Messages Updated

**Before:**
```java
Log.d(TAG, String.format("Adjusted rig frequency: %d Hz -> %d Hz (audio freq: %.0f Hz, offset: %d Hz)", ...));
```

**After:**
```java
Log.d(TAG, String.format("Fake It: Adjusted rig frequency: %d Hz -> %d Hz (audio offset: %.0f Hz, rig offset: %d Hz)", ...));
```

---

## Backwards Compatibility

### Database Configuration

The feature supports both old and new configuration keys:

```java
// New key (preferred)
if (name.equalsIgnoreCase("fakeItSplit")) {
    GeneralVariables.fakeItSplit = !(result == null || result.equals("") || result.equals("0"));
}

// Legacy key support
if (name.equalsIgnoreCase("adjustRigFreqForAudioFreq")) {
    GeneralVariables.fakeItSplit = !(result == null || result.equals("") || result.equals("0"));
}
```

Users with existing configurations will have their settings automatically migrated when the app reads the old key.

### String Resources

Legacy string names are maintained and point to new strings, ensuring any hardcoded references continue to work.

---

## Testing Checklist

- [ ] Verify switch appears in settings UI with "Split operation: Fake it" label
- [ ] Confirm switch is disabled in VOX mode
- [ ] Test frequency adjustment with various audio frequencies (e.g., 200 Hz, 800 Hz, 1500 Hz, 2500 Hz)
- [ ] Verify audio is always generated at 1500 Hz when enabled
- [ ] Confirm toast messages show "Fake It:" prefix
- [ ] Test with different rig types (Icom, XieGu, Flex)
- [ ] Verify original frequency is restored after transmission
- [ ] Check that transmitOffsetHz calibration still applies correctly
- [ ] Test mode switching from CAT to VOX and back
- [ ] Verify legacy configuration keys are read correctly

---

## Files Modified

1. `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/GeneralVariables.java`
2. `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/MainViewModel.java`
3. `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/database/DatabaseOpr.java`
4. `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ui/ConfigFragment.java`
5. `ft8cn/app/src/main/res/values/strings.xml`
6. `ft8cn/app/src/main/res/layout/fragment_config.xml`

---

## Comparison with WSJT-X

| Aspect | WSJT-X | FT8CN (Updated) |
|--------|--------|-----------------|
| **Feature Name** | "Fake It" | "Split operation: Fake it" |
| **Purpose** | Emulate split operation | Emulate split operation |
| **Calculation** | `targetTX - 1500` | `band + (audio - 1500)` (equivalent) |
| **Audio Frequency** | 1500 Hz (center) | 1500 Hz (center) |
| **Mode Restriction** | None (works with VOX) | Requires CAT/RTS/DTR |
| **User Feedback** | None (transparent) | Toast messages |
| **Terminology** | "Fake It" button | "Split operation: Fake it" switch |

---

## Migration Guide for Users

### If you had "Adjust rig freq for audio freq" enabled:

1. **No action required** - The setting automatically migrates to "Split operation: Fake it"
2. The feature works exactly the same way
3. UI label now matches WSJT-X terminology
4. Toast messages now say "Fake It:" instead of "Rig frequency adjusted:"

### New Users:

1. Go to Settings → Frequency Tab
2. Enable "Split operation: Fake it" switch
3. Ensure you're in CAT, RTS, or DTR mode (not VOX)
4. Select any audio frequency (0-2900 Hz)
5. When transmitting, rig frequency automatically adjusts
6. Audio is always generated at 1500 Hz for optimal quality

---

## Technical Notes

### Why "Fake It"?

The term "Fake It" comes from WSJT-X and refers to emulating (faking) a hardware split operation in software. The radio thinks it's in simplex mode, but the application rapidly switches frequencies during TX/RX transitions to achieve the same result as true split operation.

### Audio Frequency Generation

When Fake It is enabled:
- User selection (e.g., 200 Hz) represents the **offset** from the dial frequency
- Audio is always generated at **1500 Hz** (center of FT8 bandwidth)
- Rig frequency is adjusted to compensate: `dial + (offset - 1500)`
- Final TX frequency = adjusted rig + 1500 Hz = dial + offset ✓

### Integration with transmitOffsetHz

The transmit offset calibration still applies on top of Fake It:

```java
float transmitFreq = GeneralVariables.getAudioFrequencyForGeneration() // 1500 Hz
                   + GeneralVariables.transmitOffsetHz;                 // e.g., +50 Hz
// Result: 1550 Hz audio generation
```

This allows users to use both features together for maximum flexibility and accuracy.

---

## Related Documentation

- [WSJT-X vs FT8CN Comparison](WSJTX_FAKE_IT_VS_FT8CN_CENTER_FREQ.md)
- [Transmit Frequency Adjustment Plan](TRANSMIT_FREQUENCY_ADJUSTMENT_PLAN.md)

---

## Commit Message Template

```
Rename "Adjust rig freq" to "Fake It" split mode

- Matches WSJT-X terminology and behavior
- Renamed variable: adjustRigFreqForAudioFreq → fakeItSplit
- Updated all UI strings and comments
- Added legacy key support for backwards compatibility
- Improved code comments with WSJT-X calculation explanation
- Updated toast messages to show "Fake It:" prefix

The calculation remains unchanged and is mathematically equivalent
to WSJT-X's "Fake It" implementation:
  newRigFreq = band + (audioFreq - 1500)

Users with existing settings will have them automatically migrated.
```

---

## Future Enhancements

1. **Option to disable toast messages** - Some users may find them too verbose
2. **VOX mode support** - Explore alternative approaches (timing-based?)
3. **True split mode detection** - Detect if rig actually supports split
4. **Per-band settings** - Remember Fake It preference per band
5. **Split status indicator** - Visual indicator when Fake It is actively adjusting

