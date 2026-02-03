# Settings: 3-Tab Simplified Structure

## Tab Organization

### Tab 1: BASIC (Station + Radio + Transmit)
**Purpose:** Essential settings needed to get on the air

#### Station Section
- My Callsign
- Callsign Modifier
- Park Number
- Maidenhead Grid (with Get Location button)

#### Radio Configuration Section
- Rig Make
- Rig Model
- Operation Band
- Control Mode (VOX/CAT/RTS/DTR)
- Connection Mode (USB/Bluetooth/Network) *conditional*
- CI-V Address
- Baud Rate
- Serial Port Settings (Data/Parity/Stop bits)

#### Transmit Settings Section
- Transmit Frequency
- Synchronous Frequency
- Fake It Split
- Transmit Offset (Calibration)
- Transmit Delay
- PTT Delay

**Total: ~25 settings**

---

### Tab 2: ADVANCED (Audio + Time + Operating)
**Purpose:** Fine-tuning and operating preferences

#### Audio Configuration Section
- Audio Bit Depth (16/32 bit)
- Audio Sample Rate (12/24/48 kHz)

#### Time Synchronization Section
- UTC Time Offset
- NTP Server (with custom input)
- GPS Time Sync
- Sync Time Button

#### Decode Settings Section
- Decode Mode (Fast/Deep)
- Decode Overrun Alert
- Message Display Mode

#### Operating Behavior Section
- Auto Follow CQ
- Calling Adds to Follow List
- Auto Call Follow List
- Skip My Grid When Responding
- Launch Supervision Timeout
- No Reply Limit
- Exclude Callsigns

#### Logging Section
- Save SWL Messages
- Save SWL QSOs

#### Monitoring Section
- SWR Alarm
- ALC Alarm

**Total: ~20 settings**

---

### Tab 3: CLOUD & HELP (Services + Maintenance)
**Purpose:** Online integrations and app maintenance

#### PSK Reporter Section
- Enable PSK Reporter Receive
- View Spots Button

#### Cloudlog Integration Section
- Enable Cloudlog
- Server Address
- API Key
- Station ID
- Test Connection Button

#### QRZ Integration Section
- Enable QRZ
- API Key
- Test Connection Button

#### Data Management Section
- Clear Cache/Data Button
- Application Log Button

#### Help & Information Section
- FAQ Button
- About Button

**Total: ~15 settings**

---

## Implementation Strategy

### Phase 1: Quick Tab Wrapper (Minimal Changes)
Keep existing ConfigFragment logic, just wrap it in tabs:

```
ConfigFragment (new)
  ├─ TabLayout
  └─ ViewPager2
      ├─ BasicSettingsFragment
      │   └─ Include most of current ConfigFragment UI
      ├─ AdvancedSettingsFragment
      │   └─ Include remaining settings
      └─ CloudSettingsFragment
          └─ Include cloud services + help
```

**Advantages:**
- Quick to implement
- Minimal code changes
- All existing logic preserved
- Easy to test

**Approach:**
1. Split current fragment_config.xml into 3 layout files
2. Create 3 simple fragments that include those layouts
3. Keep all the existing Java logic in ConfigFragment or move to MainViewModel
4. Use SharedViewModel pattern to share data between tabs

### Phase 2: Full Migration (Clean Separation)
Move all settings logic into individual tab fragments:

**Advantages:**
- Cleaner code organization
- Better separation of concerns
- Easier to maintain long-term
- Each tab is self-contained

**Approach:**
1. Create proper fragment classes for each tab
2. Move relevant settings logic to each fragment
3. Use shared MainViewModel for data
4. Implement proper lifecycle management

---

## Quick Implementation Plan (Phase 1)

### Step 1: Backup Current ConfigFragment
```bash
cp ConfigFragment.java ConfigFragment_backup.java
cp fragment_config.xml fragment_config_backup.xml
```

### Step 2: Split Layout XML
Extract sections from `fragment_config.xml` into:
- `fragment_basic_settings.xml` - Station + Radio + Transmit UI
- `fragment_advanced_settings.xml` - Audio + Time + Operating UI
- `fragment_cloud_settings.xml` - Cloud services + Help UI

### Step 3: Create Simple Fragment Classes

Each fragment just inflates its layout and accesses ConfigFragment's logic:

```java
public class BasicSettingsFragment extends Fragment {
    private FragmentBasicSettingsBinding binding;
    private ConfigFragment parentConfig;

    @Override
    public View onCreateView(...) {
        binding = FragmentBasicSettingsBinding.inflate(inflater, container, false);
        parentConfig = (ConfigFragment) getParentFragment();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(...) {
        super.onViewCreated(view, savedInstanceState);
        // Initialize UI components using parentConfig or MainViewModel
        initializeBasicSettings();
    }

    private void initializeBasicSettings() {
        // Set up callsign, rig, transmit settings
        // Can delegate to ConfigFragment methods or MainViewModel
    }
}
```

### Step 4: Update Main ConfigFragment

Replace ScrollView with TabLayout + ViewPager2:

```java
public class ConfigFragment extends Fragment {
    private FragmentConfigBinding binding;
    private SettingsPagerAdapter pagerAdapter;

    @Override
    public View onCreateView(...) {
        binding = FragmentConfigBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(...) {
        super.onViewCreated(view, savedInstanceState);

        pagerAdapter = new SettingsPagerAdapter(requireActivity());
        binding.settingsViewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(binding.settingsTabLayout, binding.settingsViewPager,
                (tab, position) -> tab.setText(SettingsPagerAdapter.getTabTitle(position))
        ).attach();
    }

    // Keep all existing methods for now
    // Child fragments can call these methods through parent reference
}
```

### Step 5: Test Each Tab
- Verify all settings appear in correct tabs
- Test saving/loading for each setting
- Verify VOX mode dependencies work
- Test conditional UI elements (network/bluetooth)

---

## Detailed Settings Mapping

### Basic Tab - Detailed Breakdown

```
STATION IDENTIFICATION
├─ My Callsign (EditText)
│  └─ Help button
├─ Callsign Modifier (EditText)
│  └─ Help button
├─ Park Number (EditText)
└─ Maidenhead Grid (EditText)
   └─ Get Location button

RADIO CONFIGURATION
├─ Rig Make (Spinner)
│  └─ Filters Rig Model list
├─ Rig Model (Spinner)
│  └─ Auto-fills CI-V, Baud Rate
├─ Operation Band (Spinner)
│  └─ Updates radio frequency
├─ Control Mode (RadioGroup)
│  ├─ VOX
│  ├─ CAT
│  ├─ RTS
│  └─ DTR
├─ Connection Mode (RadioGroup) *[shown if CAT mode]*
│  ├─ USB Cable
│  ├─ Bluetooth → Opens device selector
│  └─ Network → Shows radio type selector
├─ CI-V Address (EditText)
│  └─ Hex validation
├─ Baud Rate (Spinner)
└─ Serial Port Settings
   ├─ Data Bits (Spinner)
   ├─ Parity (Spinner)
   ├─ Stop Bits (Spinner)
   └─ Reset to Defaults button

TRANSMIT SETTINGS
├─ Transmit Frequency (EditText)
│  └─ Range: 100-2900 Hz
├─ Synchronous Frequency (Switch)
│  └─ Locks TX=RX
├─ Fake It Split (Switch)
│  ├─ Help button
│  └─ Disabled in VOX mode
├─ Transmit Offset (EditText + RadioGroup)
│  ├─ Value in Hz
│  ├─ +/- sign selector
│  └─ Help button
├─ Transmit Delay (EditText)
│  └─ Default: 500ms
└─ PTT Delay (Spinner)
   └─ Range: 0-240ms (10ms steps)
```

### Advanced Tab - Detailed Breakdown

```
AUDIO CONFIGURATION
├─ Audio Bit Depth (RadioGroup)
│  ├─ 16-bit INT
│  └─ 32-bit Float
└─ Audio Sample Rate (RadioGroup)
   ├─ 12 kHz
   ├─ 24 kHz
   └─ 48 kHz

TIME SYNCHRONIZATION
├─ UTC Time Offset (Spinner)
│  └─ Range: -75 to +75 (5min steps)
├─ NTP Server (Spinner + EditText)
│  ├─ Preset servers
│  └─ Custom input when "Custom" selected
├─ GPS Time Sync (Switch)
│  └─ Experimental feature
└─ Sync Time Now (Button)

DECODE SETTINGS
├─ Decode Mode (RadioGroup)
│  ├─ Fast
│  └─ Deep
├─ Decode Overrun Alert (Switch)
└─ Message Display Mode (RadioGroup)
   ├─ Standard (full info)
   └─ Simple (compact)

OPERATING BEHAVIOR
├─ Auto Follow CQ (Switch)
│  └─ Help button
├─ Calling Adds to Follow List (Switch)
│  └─ Help button
├─ Auto Call Follow List (Switch)
├─ Skip My Grid When Responding (Switch)
│  └─ Help button
├─ Launch Supervision Timeout (Spinner)
│  └─ Options: Off, 5, 10, 15, 20, 30 min
├─ No Reply Limit (Spinner)
│  └─ Range: 0-10 attempts
└─ Exclude Callsigns (EditText)
   ├─ Comma-separated list
   └─ Help button

LOGGING
├─ Save SWL Messages (Switch)
└─ Save SWL QSOs (Switch)

MONITORING ALARMS
├─ SWR Alarm (Switch)
└─ ALC Alarm (Switch)
```

### Cloud & Help Tab - Detailed Breakdown

```
PSK REPORTER
├─ Enable PSK Reporter Receive (Switch)
│  └─ MQTT spot subscriptions
└─ View PSK Spots (Button)
   └─ Opens spots dialog

CLOUDLOG INTEGRATION
├─ Enable Cloudlog (Switch)
├─ Server Address (EditText)
│  └─ e.g., http://example.com
├─ API Key (EditText)
│  └─ From Cloudlog settings
├─ Station ID (EditText)
│  └─ Cloudlog station name
└─ Test Connection (Button)
   └─ Validates credentials

QRZ INTEGRATION
├─ Enable QRZ (Switch)
├─ API Key (EditText)
│  └─ From QRZ account
└─ Test Connection (Button)
   └─ Validates API key

DATA MANAGEMENT
├─ Clear Cache/Data (Button)
│  └─ Opens ClearCacheDataDialog with options:
│     ├─ Clear Follow List
│     ├─ Clear Log Cache
│     └─ Clear SWL QSO Cache
└─ Application Log (Button)
   └─ Opens ApplicationLogFragment

HELP & INFORMATION
├─ FAQ (Button)
│  └─ Opens FAQActivity
└─ About (Button)
   └─ Shows readme.txt with version info
```

---

## Dependencies & Interactions

### Control Mode Dependencies
```
if (controlMode == VOX) {
    • Hide Connection Mode section
    • Hide Serial Port section
    • Disable Fake It Split
    • Hide CI-V Address
} else if (controlMode == CAT/RTS/DTR) {
    • Show Connection Mode section
    • Show Serial Port section
    • Enable Fake It Split
    • Show CI-V Address
}
```

### Connection Mode Dependencies
```
if (connectionMode == NETWORK) {
    • Show radio type selector (Flex/Xiegu/Icom)
    • Different connection dialogs
} else if (connectionMode == BLUETOOTH) {
    • Show Bluetooth device selector
} else if (connectionMode == USB_CABLE) {
    • Standard serial connection
}
```

### Rig Selection Dependencies
```
on Rig Model change:
    • Auto-update CI-V Address
    • Auto-update Baud Rate
    • Auto-update Instruction Set
    • Filter by Rig Make selection
```

### Synchronous Frequency Dependencies
```
if (synFrequency == ON) {
    • Disable manual Transmit Frequency input
    • TX freq = RX freq automatically
}
```

---

## Next Steps

1. ✅ Updated SettingsPagerAdapter for 3 tabs
2. ✅ Added tab title strings
3. ⏭️ Choose implementation strategy:
   - **Quick:** Just split the XML layouts, minimal Java changes
   - **Complete:** Full fragment reorganization

Which approach would you like to proceed with?

