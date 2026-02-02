# PSK Reporter Sending Code Restoration Guide

This document describes how to restore the PSK Reporter spot sending functionality that was removed from the build.

## Overview

The PSK Reporter sending code has been moved to the `psk-reporting-broken` directory to remove it from the build while preserving it for potential future restoration. The receiving code (MQTT-based spot consumption) remains in the build.

## Files Moved

### Main Code File
- **`psk-reporting-broken/java/com/bg7yoz/ft8cn/psk/PSKReporter.java`**
  - This is the main PSK Reporter sending implementation
  - Implements IPFIX protocol for sending spots to `report.pskreporter.info:4739`
  - Handles spot batching, deduplication, and UDP packet construction

## Code Changes Made (To Restore)

### 1. GeneralVariables.java

**Add back these variables:**
```java
public static boolean enablePskReporter = false;//是否向PSK Reporter上报解码结果
public static String pskReporterAntenna = "";//PSK Reporter天线描述（可选）
public static boolean pskReporterDebugMode = false;//PSK Reporter debug mode (uses port 14739 instead of 4739)
```

**Location:** After line 180 (before `enablePskReporterReceive`)

### 2. MainViewModel.java

**Add import:**
```java
import com.bg7yoz.ft8cn.psk.PSKReporter;
```

**Add field:**
```java
public PSKReporter pskReporter;//用于向PSK Reporter上报解码结果
```

**Add spot reporting code in `afterDecode` callback (around line 410):**
```java
// Add spots to PSK Reporter if enabled
if (GeneralVariables.enablePskReporter && pskReporter != null) {
    int spotsAdded = 0;
    for (Ft8Message msg : messages) {
        // Use existing method that handles all the filtering logic
        String callsign = getSenderCallsignForPsk(msg);
        if (callsign != null && !callsign.isEmpty()) {
            // Calculate absolute frequency: carrier frequency + offset
            long absoluteFreqHz = msg.band + (long)msg.freq_hz;
            // Get grid locator (use maidenGrid if available, otherwise empty string)
            String gridLocator = (msg.maidenGrid != null && !msg.maidenGrid.isEmpty()) 
                    ? msg.maidenGrid : "";
            // Convert UTC time to Unix seconds
            long flowStartSeconds = msg.utcTime / 1000;
            // Add spot to PSK Reporter
            pskReporter.addSpot(callsign, absoluteFreqHz, msg.snr, gridLocator, flowStartSeconds);
            spotsAdded++;
        }
    }
    if (spotsAdded > 0) {
        Log.d(TAG, "PSK Reporter: Added " + spotsAdded + " spots from " + messages.size() + " messages");
    } else if (messages.size() > 0) {
        Log.d(TAG, "PSK Reporter: No valid callsigns found in " + messages.size() + " messages");
    }
} else {
    if (!GeneralVariables.enablePskReporter) {
        Log.d(TAG, "PSK Reporter: Disabled in settings");
    }
    if (pskReporter == null) {
        Log.w(TAG, "PSK Reporter: pskReporter is null!");
    }
}
```

**Add initialization (around line 457):**
```java
pskReporter = new PSKReporter();
ft8SignalListener.startListen();
if (GeneralVariables.enablePskReporter) {
    pskReporter.start();
}
```

**Add helper method (around line 687):**
```java
/**
 * Get the transmitting station callsign for PSK Reporter (the station we heard).
 * Returns empty if the message is from us or not a valid reportable callsign.
 */
private String getSenderCallsignForPsk(Ft8Message msg) {
    String from = msg.getCallsignFrom();
    String to = msg.getCallsignTo();
    if (GeneralVariables.myCallsign != null && !GeneralVariables.myCallsign.isEmpty()
            && GeneralVariables.checkIsMyCallsign(from)) {
        return "";  // don't report our own transmission
    }
    if (from != null && !from.isEmpty() && !from.startsWith("<") && !from.equals("CQ") && !from.startsWith("DE") && !from.startsWith("QRZ")) {
        return from;
    }
    if (to != null && !to.isEmpty() && !to.startsWith("<") && !GeneralVariables.checkIsMyCallsign(to)) {
        return to;
    }
    return "";
}
```

### 3. MainActivity.java

**Add stop call in `onDestroy` (around line 745):**
```java
if (mainViewModel.pskReporter != null) {
    mainViewModel.pskReporter.stop();
}
```

### 4. ConfigFragment.java

**Add PSK Reporter switch handling (around line 599):**
```java
// PSK Reporter
if (binding.pskReporterSwitch != null) {
    binding.pskReporterSwitch.setOnCheckedChangeListener(null);
    binding.pskReporterSwitch.setChecked(GeneralVariables.enablePskReporter);
    setPskReporterSwitchText();
    binding.pskReporterSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            GeneralVariables.enablePskReporter = binding.pskReporterSwitch.isChecked();
            mainViewModel.databaseOpr.writeConfig("enablePskReporter", binding.pskReporterSwitch.isChecked() ? "1" : "0", null);
            setPskReporterSwitchText();
            if (mainViewModel.pskReporter != null) {
                if (binding.pskReporterSwitch.isChecked()) {
                    mainViewModel.pskReporter.start();
                } else {
                    mainViewModel.pskReporter.stop();
                }
            }
        }
    });
}
```

**Add antenna edit handling (around line 619):**
```java
if (binding.pskReporterAntennaEdit != null) {
    binding.pskReporterAntennaEdit.setText(GeneralVariables.pskReporterAntenna != null ? GeneralVariables.pskReporterAntenna : "");
    binding.pskReporterAntennaEdit.addTextChangedListener(new android.text.TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override
        public void afterTextChanged(android.text.Editable s) {
            String v = s != null ? s.toString().trim() : "";
            GeneralVariables.pskReporterAntenna = v;
            mainViewModel.databaseOpr.writeConfig("pskReporterAntenna", v, null);
        }
    });
}
```

**Add debug mode switch handling (around line 634):**
```java
if (binding.pskReporterDebugModeSwitch != null) {
    binding.pskReporterDebugModeSwitch.setOnCheckedChangeListener(null);
    binding.pskReporterDebugModeSwitch.setChecked(GeneralVariables.pskReporterDebugMode);
    setPskReporterDebugModeSwitchText();
    android.util.Log.d("ConfigFragment", "PSK Reporter Debug Mode loaded: " + GeneralVariables.pskReporterDebugMode);
    binding.pskReporterDebugModeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
            GeneralVariables.pskReporterDebugMode = binding.pskReporterDebugModeSwitch.isChecked();
            android.util.Log.d("ConfigFragment", "PSK Reporter Debug Mode changed to: " + GeneralVariables.pskReporterDebugMode);
            mainViewModel.databaseOpr.writeConfig("pskReporterDebugMode", binding.pskReporterDebugModeSwitch.isChecked() ? "1" : "0", null);
            setPskReporterDebugModeSwitchText();
        }
    });
}
```

**Add helper methods (around line 1098):**
```java
private void setPskReporterSwitchText() {
    if (binding.pskReporterSwitch != null) {
        binding.pskReporterSwitch.setText(binding.pskReporterSwitch.isChecked() ? R.string.psk_reporter_on : R.string.psk_reporter_off);
    }
}

private void setPskReporterDebugModeSwitchText() {
    if (binding.pskReporterDebugModeSwitch != null) {
        binding.pskReporterDebugModeSwitch.setText(binding.pskReporterDebugModeSwitch.isChecked() ? R.string.psk_reporter_debug_mode_on : R.string.psk_reporter_debug_mode_off);
    }
}
```

### 5. fragment_config.xml

**Add PSK Reporter sending UI elements in `pskReporterLayout` (before `pskReporterReceiveText`):**
```xml
<TextView
    android:id="@+id/pskReporterText"
    android:layout_width="0dp"
    android:layout_height="wrap_content"
    android:layout_marginStart="8dp"
    android:layout_marginEnd="8dp"
    android:layout_marginTop="4dp"
    android:text="@string/psk_reporter"
    android:textColor="@color/text_view_color"
    android:textSize="14sp"
    app:layout_constraintEnd_toStartOf="@+id/pskReporterSwitch"
    app:layout_constraintStart_toStartOf="parent"
    app:layout_constraintTop_toTopOf="parent" />

<Switch
    android:id="@+id/pskReporterSwitch"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_marginStart="8dp"
    android:layout_marginEnd="8dp"
    android:layout_marginTop="4dp"
    android:layout_marginBottom="4dp"
    android:checked="false"
    android:text="@string/psk_reporter_on"
    android:textColor="@color/text_view_color"
    android:textSize="14sp"
    android:thumbTint="?attr/colorPrimary"
    android:thumbTintMode="multiply"
    android:trackTint="@color/button_start_color"
    app:layout_constraintEnd_toEndOf="parent"
    app:layout_constraintStart_toEndOf="@+id/pskReporterText"
    app:layout_constraintTop_toTopOf="parent"
    tools:ignore="TouchTargetSizeCheck,UseSwitchCompatOrMaterialXml" />

<TextView
    android:id="@+id/pskReporterAntennaText"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_marginStart="8dp"
    android:layout_marginTop="4dp"
    android:layout_marginBottom="4dp"
    android:text="@string/psk_reporter_antenna"
    android:textColor="@color/text_view_color"
    android:textSize="14sp"
    app:layout_constraintBottom_toTopOf="@+id/pskReporterDebugModeSwitch"
    app:layout_constraintStart_toStartOf="parent"
    app:layout_constraintTop_toBottomOf="@+id/pskReporterText" />

<EditText
    android:id="@+id/pskReporterAntennaEdit"
    android:layout_width="0dp"
    android:layout_height="wrap_content"
    android:layout_marginStart="8dp"
    android:layout_marginEnd="8dp"
    android:layout_marginBottom="4dp"
    android:background="@drawable/editor_style"
    android:hint="@string/psk_reporter_antenna_hint"
    android:inputType="text"
    android:maxLines="1"
    android:textColor="@color/text_view_color"
    android:textColorHint="@color/text_view_color"
    android:textSize="14sp"
    app:layout_constraintBottom_toTopOf="@+id/pskReporterDebugModeSwitch"
    app:layout_constraintEnd_toEndOf="parent"
    app:layout_constraintStart_toEndOf="@+id/pskReporterAntennaText"
    app:layout_constraintTop_toTopOf="@+id/pskReporterAntennaText" />

<TextView
    android:id="@+id/pskReporterDebugModeText"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_marginStart="8dp"
    android:layout_marginTop="4dp"
    android:text="@string/psk_reporter_debug_mode"
    android:textColor="@color/text_view_color"
    android:textSize="14sp"
    app:layout_constraintBottom_toTopOf="@+id/pskReporterReceiveSwitch"
    app:layout_constraintStart_toStartOf="parent"
    app:layout_constraintTop_toBottomOf="@+id/pskReporterAntennaEdit" />

<Switch
    android:id="@+id/pskReporterDebugModeSwitch"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:layout_marginEnd="8dp"
    android:text="@string/psk_reporter_debug_mode_off"
    android:textColor="@color/text_view_color"
    android:textSize="14sp"
    app:layout_constraintBottom_toTopOf="@+id/pskReporterReceiveSwitch"
    app:layout_constraintEnd_toEndOf="parent"
    app:layout_constraintStart_toEndOf="@+id/pskReporterDebugModeText"
    app:layout_constraintTop_toTopOf="@+id/pskReporterDebugModeText" />
```

**Update constraint on `pskReporterReceiveText`:**
```xml
app:layout_constraintTop_toBottomOf="@+id/pskReporterDebugModeSwitch"
```

### 6. strings.xml

**Add back these strings:**
```xml
<string name="psk_reporter">PSK Reporter</string>
<string name="psk_reporter_on" translatable="false">On</string>
<string name="psk_reporter_antenna">Antenna</string>
<string name="psk_reporter_antenna_hint">Optional antenna description</string>
<string name="psk_reporter_debug_mode">Debug mode</string>
<string name="psk_reporter_debug_mode_on">On (port 14739)</string>
<string name="psk_reporter_debug_mode_off">Off (port 4739)</string>
<string name="psk_reporter_view_spots">View spots</string>
```

### 7. DatabaseOpr.java

**Add config loading (around line 2670):**
```java
if (name.equalsIgnoreCase("enablePskReporter")) {
    GeneralVariables.enablePskReporter = result != null && result.equals("1");
}
if (name.equalsIgnoreCase("pskReporterAntenna")) {
    GeneralVariables.pskReporterAntenna = (result != null) ? result : "";
}
if (name.equalsIgnoreCase("pskReporterDebugMode")) {
    GeneralVariables.pskReporterDebugMode = result != null && result.equals("1");
    android.util.Log.d("DatabaseOpr", "Loaded pskReporterDebugMode: " + GeneralVariables.pskReporterDebugMode);
}
```

## File to Restore

1. **Copy PSKReporter.java back:**
   ```bash
   cp psk-reporting-broken/java/com/bg7yoz/ft8cn/psk/PSKReporter.java ft8cn/app/src/main/java/com/bg7yoz/ft8cn/psk/
   ```

## Testing After Restoration

1. Build the project: `./gradlew.bat assembleDebug`
2. Enable PSK Reporter in settings
3. Set callsign and grid square
4. Decode some FT8 messages
5. Check logs for PSK Reporter activity
6. Verify spots are being sent (check external calls log)

## Notes

- The PSK Reporter receiving code (MQTT) remains in the build and is unaffected
- The PSK Reporter log tab in Application Logs will work once sending is restored
- All sending-related UI elements were removed and need to be restored as described above
- The code follows the IPFIX protocol specification from https://pskreporter.info/pskdev.html
