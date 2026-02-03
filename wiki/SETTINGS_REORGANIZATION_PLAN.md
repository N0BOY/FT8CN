# Settings Reorganization Plan

## Proposed Tab Structure (6 Tabs)

### Tab 1: Station
**Purpose:** Station identification and location
- My Callsign
- Callsign Modifier (e.g., /P, /M)
- Park Number
- Maidenhead Grid (with Get Location button)

### Tab 2: Radio
**Purpose:** Radio hardware configuration and connection
- Rig Make (filter)
- Rig Model
- Control Mode (VOX/CAT/RTS/DTR)
- Connection Mode (USB/Bluetooth/Network) *[conditional on CAT mode]*
- Operation Band
- CI-V Address
- Baud Rate
- Serial Port Settings (Data bits, Parity, Stop bits, Reset button)

### Tab 3: Transmit
**Purpose:** Transmission settings and timing
- Transmit Frequency (with help)
- Synchronous Frequency (lock TX=RX)
- Fake It Split (with help)
- Transmit Offset Calibration (+/- Hz)
- Transmit Delay
- PTT Delay
- Launch Supervision Timeout
- No Reply Limit

### Tab 4: Audio & Time
**Purpose:** Audio configuration and time synchronization
- **Audio Section:**
  - Audio Bit Depth (16/32 bit)
  - Audio Sample Rate (12/24/48 kHz)
- **Time Section:**
  - UTC Time Offset
  - NTP Server (with custom input)
  - GPS Time Sync
  - Sync Time Button

### Tab 5: Operating
**Purpose:** Operating modes, decoding, and behavior
- **Decode Section:**
  - Decode Mode (Fast/Deep)
  - Decode Overrun Alert
  - Message Display Mode (Standard/Simple)
- **Calling & Follow:**
  - Auto Follow CQ
  - Calling Adds to Follow List
  - Auto Call Follow List
  - Skip My Grid When Responding
  - Exclude Callsigns
- **Monitoring:**
  - Save SWL Messages
  - Save SWL QSOs
  - SWR Alarm
  - ALC Alarm

### Tab 6: Cloud & Advanced
**Purpose:** Online integrations, maintenance, and help
- **Cloudlog Integration:**
  - Enable Cloudlog
  - Server Address
  - API Key
  - Station ID
  - Test Connection Button
- **QRZ Integration:**
  - Enable QRZ
  - API Key
  - Test Connection Button
- **PSK Reporter:**
  - Enable PSK Reporter Receive
  - View Spots Button
- **Maintenance:**
  - Clear Cache/Data
  - Application Log
  - FAQ
  - About

---

## Implementation Approach

### 1. Use TabLayout with ViewPager2

Create a tabbed interface that allows users to swipe or click between setting categories.

### 2. Fragment Architecture

```
ConfigFragment (host)
  ├─ TabLayout (6 tabs)
  └─ ViewPager2
      ├─ StationSettingsFragment
      ├─ RadioSettingsFragment
      ├─ TransmitSettingsFragment
      ├─ AudioTimeSettingsFragment
      ├─ OperatingSettingsFragment
      └─ CloudAdvancedSettingsFragment
```

### 3. Preserve Existing Logic

- All setting values remain in GeneralVariables
- All database operations remain in DatabaseOpr
- All validation logic stays in place
- Dependencies (e.g., VOX disables Fake It) preserved

### 4. Visual Consistency

- Keep current styling and colors
- Use same switches, spinners, and inputs
- Maintain help buttons (info icons)
- Consistent section headers within tabs

---

## Benefits

1. **Reduced Scrolling** - Settings organized by purpose
2. **Easier Discovery** - Clear category labels
3. **Better Context** - Related settings grouped together
4. **Scalability** - Easy to add new settings to appropriate tab
5. **Mobile-Friendly** - Less overwhelming on small screens
6. **Logical Flow** - Follows setup order (Station → Radio → TX → etc.)

---

## Tab Icons (Optional Enhancement)

- **Station:** Person/ID icon
- **Radio:** Radio/antenna icon
- **Transmit:** Transmission/signal icon
- **Audio & Time:** Waveform/clock icon
- **Operating:** Settings/gear icon
- **Cloud & Advanced:** Cloud/wrench icon

---

## Migration Notes

- Settings will be split across multiple fragments
- Each fragment will have its own layout XML
- ViewBinding will be used for each fragment
- ConfigFragment becomes the container/host
- No changes to database schema or keys
- All existing settings remain accessible

