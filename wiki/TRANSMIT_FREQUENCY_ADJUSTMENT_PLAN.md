# Transmit Frequency Adjustment Plan

## Overview

This document outlines the plan to implement a feature that adjusts the rig's operating frequency based on the selected audio frequency during transmission. This allows the user to transmit at the exact frequency they've selected in the audio spectrum, rather than using a fixed carrier frequency with an audio offset.

## Problem Statement

Currently, the application:
- Sets the rig to a base carrier frequency (`GeneralVariables.band`, e.g., 14.074 MHz)
- Generates FT8 audio at an audio frequency (`GeneralVariables.baseFrequency`, e.g., 1000 Hz)
- The FT8 audio signal is 3000 Hz wide, with 1500 Hz being the center frequency
- The actual transmit frequency is `band + (baseFrequency - 1500)` (e.g., if baseFrequency is 1000 Hz, transmit at band - 500 Hz)

When a user selects a different audio frequency in the spectrum (e.g., 2000 Hz), they may want the rig to actually transmit at `band + (2000 - 1500) = band + 500 Hz` instead of the current offset.

## Solution

Add a user-selectable option that:
1. **Before transmission**: Adjusts the rig frequency to `band + (baseFrequency - 1500)` (the actual transmit frequency, accounting for 1500 Hz center)
2. **After transmission**: Restores the rig to the original `band` frequency
3. **Only works in CAT/RTS/DTR modes**: Not available in VOX mode (since VOX doesn't allow programmatic frequency control)

**Note**: The FT8 audio signal is 3000 Hz wide, with 1500 Hz as the center frequency. Audio frequencies below 1500 Hz subtract from the base frequency, and frequencies above 1500 Hz add to it.

## Implementation Plan

### 1. Add Configuration Setting

**Location**: `GeneralVariables.java`

Add a new boolean flag:
```java
public static boolean adjustRigFreqForAudioFreq = false; // Adjust rig frequency based on audio frequency
```

**Location**: `DatabaseOpr.java`

Add database configuration key:
- Key: `"adjustRigFreqForAudioFreq"`
- Type: Boolean (stored as "0" or "1")
- Default: `false` (disabled by default)

**Location**: `ConfigFragment.java`

Add UI control:
- Add a Switch or CheckBox in the settings UI
- Label: "Adjust rig frequency for audio frequency" (or similar)
- Help text: "When enabled, the rig frequency will be adjusted to match the selected audio frequency during transmission. Only works in CAT/RTS/DTR modes."
- Save/load the setting from database

### 2. Store Original Frequency

**Location**: `FT8TransmitSignal.java` or `MainViewModel.java`

Add a field to store the original rig frequency before transmission:
```java
private long originalRigFrequency = 0; // Store original rig frequency before adjustment
```

### 3. Calculate New Rig Frequency

**Formula**:
```
newRigFreq = GeneralVariables.band + (GeneralVariables.getBaseFrequency() - 1500)
```

Where:
- `GeneralVariables.band` = Base carrier frequency (e.g., 14074000 Hz for 14.074 MHz)
- `GeneralVariables.getBaseFrequency()` = Audio frequency (0-3000 Hz range)
- `1500` = Center frequency of the 3000 Hz wide FT8 audio signal

**Examples**:
- Band: 14.074 MHz (14074000 Hz), Audio frequency: 1500 Hz (center)
  - New rig frequency: 14074000 + (1500 - 1500) = 14074000 Hz (no change)
  
- Band: 14.074 MHz (14074000 Hz), Audio frequency: 1000 Hz (below center)
  - New rig frequency: 14074000 + (1000 - 1500) = 14073500 Hz (subtract 500 Hz)
  
- Band: 14.074 MHz (14074000 Hz), Audio frequency: 2000 Hz (above center)
  - New rig frequency: 14074000 + (2000 - 1500) = 14074500 Hz (add 500 Hz)

### 4. Implement Frequency Adjustment Before Transmission

**Location**: `MainViewModel.java` → `onBeforeTransmit()` callback

**Logic**:
```java
@Override
public void onBeforeTransmit(Ft8Message message, int functionOder) {
    // Existing PTT control code...
    
    // NEW: Adjust rig frequency if feature is enabled
    if (GeneralVariables.adjustRigFreqForAudioFreq 
            && GeneralVariables.controlMode != ControlMode.VOX
            && baseRig != null 
            && baseRig.isConnected()) {
        
        // Store original frequency
        originalRigFrequency = baseRig.getFreq();
        
        // Calculate new frequency: band + (audioFrequency - 1500)
        // 1500 Hz is the center of the 3000 Hz wide FT8 audio signal
        float audioFreq = GeneralVariables.getBaseFrequency();
        long offset = Math.round(audioFreq - 1500.0f);
        long newRigFreq = GeneralVariables.band + offset;
        
        // Set new frequency
        baseRig.setFreq(newRigFreq);
        baseRig.setFreqToRig();
        
        Log.d(TAG, String.format("Adjusted rig frequency: %d Hz -> %d Hz (audio freq: %.0f Hz, offset: %d Hz)", 
                originalRigFrequency, newRigFreq, audioFreq, offset));
    }
    
    // Existing PTT control code continues...
}
```

**Timing Considerations**:
- Frequency adjustment should happen **before** PTT is activated
- May need a small delay after setting frequency before activating PTT (similar to existing `pttDelay`)
- Consider the existing 800ms delay in `setOperationBand()` for X6100 compatibility

### 5. Implement Frequency Restoration After Transmission

**Location**: `MainViewModel.java` → `onAfterTransmit()` callback

**Logic**:
```java
@Override
public void onAfterTransmit(Ft8Message message, int functionOder) {
    // Existing PTT control code...
    
    // NEW: Restore original rig frequency if it was adjusted
    if (GeneralVariables.adjustRigFreqForAudioFreq 
            && GeneralVariables.controlMode != ControlMode.VOX
            && baseRig != null 
            && baseRig.isConnected()
            && originalRigFrequency > 0) {
        
        // Restore original frequency
        baseRig.setFreq(originalRigFrequency);
        baseRig.setFreqToRig();
        
        Log.d(TAG, String.format("Restored rig frequency: %d Hz", originalRigFrequency));
        
        // Reset stored frequency
        originalRigFrequency = 0;
    }
    
    // Existing PTT control code continues...
}
```

**Timing Considerations**:
- Frequency restoration should happen **after** PTT is deactivated
- May need a small delay after PTT off before restoring frequency (to allow rig to finish transmitting)

### 6. Handle Edge Cases

#### 6.1 Transmission Cancelled/Interrupted

**Scenario**: User cancels transmission or transmission is interrupted

**Solution**: 
- Store `originalRigFrequency` in `FT8TransmitSignal` or `MainViewModel` instance
- Check in `onAfterTransmit()` if frequency was adjusted, restore if needed
- Also check in `setTransmitting(false)` if transmission is manually stopped

**Location**: `FT8TransmitSignal.java` → `setTransmitting(boolean transmitting)`

Add restoration logic when `transmitting == false`:
```java
if (!transmitting) {
    // Existing stop transmission code...
    
    // If frequency was adjusted, restore it
    if (GeneralVariables.adjustRigFreqForAudioFreq 
            && GeneralVariables.controlMode != ControlMode.VOX) {
        // Trigger frequency restoration via callback
        if (onDoTransmitted != null) {
            onDoTransmitted.onAfterTransmit(getFunctionCommand(functionOrder), functionOrder);
        }
    }
}
```

#### 6.2 Frequency Already at Target

**Scenario**: Rig frequency is already at the target frequency

**Solution**: 
- Check if `newRigFreq == originalRigFrequency` before adjusting
- Skip adjustment if already correct
- Still store original frequency to ensure proper restoration

#### 6.3 VOX Mode

**Scenario**: User enables feature but is in VOX mode

**Solution**: 
- Feature is automatically disabled (checked in code)
- UI should show a warning or disable the option when VOX is selected
- Consider graying out the option in VOX mode

**Location**: `ConfigFragment.java`

Add listener to control mode spinner:
```java
binding.controlModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        GeneralVariables.controlMode = controlModeAdapter.getValue(i);
        // ... existing code ...
        
        // Disable frequency adjustment option if VOX is selected
        if (GeneralVariables.controlMode == ControlMode.VOX) {
            binding.adjustRigFreqSwitch.setEnabled(false);
            binding.adjustRigFreqSwitch.setChecked(false);
            GeneralVariables.adjustRigFreqForAudioFreq = false;
        } else {
            binding.adjustRigFreqSwitch.setEnabled(true);
        }
    }
});
```

#### 6.4 Rig Not Connected

**Scenario**: Feature is enabled but rig is not connected

**Solution**: 
- Check `baseRig != null && baseRig.isConnected()` before adjusting
- Log warning if adjustment attempted without connection
- Feature silently fails (no error to user, just doesn't adjust)

#### 6.5 Multiple Transmissions in Sequence

**Scenario**: Multiple transmissions occur before frequency is restored

**Solution**: 
- Store original frequency only once (check if `originalRigFrequency == 0` before storing)
- Restore only once (reset `originalRigFrequency = 0` after restoration)
- Each transmission cycle recalculates the target frequency based on current `baseFrequency`

### 7. Database Migration

**Location**: `DatabaseOpr.java`

Add to configuration loading:
```java
if (name.equalsIgnoreCase("adjustRigFreqForAudioFreq")) {
    GeneralVariables.adjustRigFreqForAudioFreq = !(result == null || result.equals("") || result.equals("0"));
}
```

### 8. String Resources

**Location**: `ft8cn/app/src/main/res/values/strings.xml`

Add strings:
```xml
<string name="adjust_rig_freq_for_audio_freq">Adjust rig frequency for audio frequency</string>
<string name="adjust_rig_freq_for_audio_freq_summary">Adjust rig frequency to match selected audio frequency during transmission. Only works in CAT/RTS/DTR modes.</string>
<string name="adjust_rig_freq_for_audio_freq_vox_warning">This feature is not available in VOX mode</string>
```

### 9. Testing Considerations

#### 9.1 Unit Tests

**Location**: Create new test file or add to existing tests

Test cases:
1. Frequency adjustment enabled, CAT mode → frequency adjusted before TX, restored after TX
2. Frequency adjustment enabled, VOX mode → no adjustment occurs
3. Frequency adjustment disabled → no adjustment occurs
4. Transmission cancelled → frequency restored
5. Multiple transmissions → frequency adjusted each time, restored after last TX
6. Rig not connected → no adjustment, no errors

#### 9.2 Manual Testing

Test scenarios:
1. Enable feature, transmit → verify rig frequency changes
2. Enable feature, transmit, check frequency → verify correct frequency
3. Enable feature, VOX mode → verify feature disabled
4. Enable feature, disconnect rig → verify no errors
5. Enable feature, cancel transmission → verify frequency restored
6. Change audio frequency between transmissions → verify rig adjusts to new frequency

### 10. Documentation Updates

**Location**: `RELEASE_NOTES.md`

Add entry:
```markdown
## New Features

### Transmit Frequency Adjustment
- Added option to adjust rig frequency based on selected audio frequency
- When enabled, rig frequency is set to `band + (audioFrequency - 1500)` during transmission
- 1500 Hz is the center of the 3000 Hz wide FT8 audio signal
- Audio frequencies below 1500 Hz subtract from base frequency, above 1500 Hz add to it
- Original frequency is restored after transmission completes
- Only available in CAT/RTS/DTR control modes (not VOX)
```

**Location**: `USER_GUIDE.md`

Add section explaining:
- What the feature does
- When to use it
- How to enable it
- Limitations (VOX mode)

## Implementation Order

1. **Phase 1: Core Functionality**
   - Add `adjustRigFreqForAudioFreq` flag to `GeneralVariables`
   - Add database configuration
   - Add UI control in `ConfigFragment`
   - Implement frequency adjustment in `onBeforeTransmit()`
   - Implement frequency restoration in `onAfterTransmit()`

2. **Phase 2: Edge Cases**
   - Handle transmission cancellation
   - Handle VOX mode (disable option)
   - Handle rig not connected
   - Add logging

3. **Phase 3: Testing & Documentation**
   - Write unit tests
   - Manual testing
   - Update documentation
   - Update release notes

## Code Locations Summary

| Component | File | Method/Class |
|-----------|------|--------------|
| Configuration Flag | `GeneralVariables.java` | `adjustRigFreqForAudioFreq` |
| Database Config | `DatabaseOpr.java` | `readConfig()` → `"adjustRigFreqForAudioFreq"` |
| UI Control | `ConfigFragment.java` | Switch/CheckBox for setting |
| Frequency Storage | `MainViewModel.java` | `originalRigFrequency` field |
| Before TX | `MainViewModel.java` | `onBeforeTransmit()` callback |
| After TX | `MainViewModel.java` | `onAfterTransmit()` callback |
| Cancel TX | `FT8TransmitSignal.java` | `setTransmitting(false)` |
| String Resources | `strings.xml` | New string entries |

## Potential Issues & Solutions

### Issue 1: Timing Problems
**Problem**: Rig may not have time to change frequency before PTT activates
**Solution**: Add delay between frequency change and PTT activation (similar to existing `pttDelay`)

### Issue 2: Frequency Drift
**Problem**: Rig frequency may drift or not be set accurately
**Solution**: 
- Verify frequency was set correctly (read back if supported)
- Add retry logic if needed
- Log actual vs. expected frequency

### Issue 3: User Confusion
**Problem**: User may not understand what the feature does
**Solution**: 
- Clear UI labels and help text
- Documentation in USER_GUIDE
- Tooltip or info icon with explanation

### Issue 4: Compatibility
**Problem**: Some rigs may not support rapid frequency changes
**Solution**: 
- Add rig-specific delays if needed
- Test with common rig models
- Document known issues

## Success Criteria

1. ✅ Feature can be enabled/disabled in settings
2. ✅ Feature only works in CAT/RTS/DTR modes (disabled in VOX)
3. ✅ Rig frequency adjusts to `band + (audioFrequency - 1500)` before transmission
4. ✅ Original frequency is restored after transmission
5. ✅ Frequency is restored even if transmission is cancelled
6. ✅ No errors when rig is not connected
7. ✅ Feature works correctly with multiple sequential transmissions
8. ✅ Unit tests pass
9. ✅ Documentation updated

## Notes

- The actual transmit frequency is always `band + (baseFrequency - 1500)`, regardless of this feature
- This feature changes where the rig's VFO is set to match the actual transmit frequency
- The 1500 Hz offset accounts for the center frequency of the 3000 Hz wide FT8 audio signal
- The feature is optional and disabled by default to maintain backward compatibility
- Users who want to manually control their rig frequency can leave this disabled
