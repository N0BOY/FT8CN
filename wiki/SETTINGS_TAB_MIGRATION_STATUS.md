# Settings Tab Migration - Status Tracker

## Overview
Full reorganization of ConfigFragment settings into 3 logical tabs.

## Progress

### Phase 1: Preparation ✅
- [x] Backup ConfigFragment.java → ConfigFragment_backup.java
- [x] Backup fragment_config.xml → fragment_config_backup.xml
- [x] Create task tracking

### Phase 2: Create Tab Infrastructure
- [ ] Create BaseSettingsFragment.java (shared base class)
- [ ] Create fragment_basic_settings.xml (Station + Radio + Transmit UI)
- [ ] Create fragment_advanced_settings.xml (Audio + Time + Operating UI)
- [ ] Create fragment_cloud_settings.xml (Cloud + Help UI)
- [ ] Update BasicSettingsFragment.java implementation
- [ ] Update AdvancedSettingsFragment.java implementation
- [ ] Update CloudSettingsFragment.java implementation

### Phase 3: Update Main ConfigFragment
- [ ] Create new fragment_config.xml with TabLayout
- [ ] Simplify ConfigFragment.java to manage tabs only
- [ ] Ensure proper initialization

### Phase 4: Testing & Verification
- [ ] Test Basic tab settings
- [ ] Test Advanced tab settings
- [ ] Test Cloud tab settings
- [ ] Verify VOX mode dependencies
- [ ] Verify conditional UI elements
- [ ] Test save/load for all settings

## Settings Distribution

### Basic Tab (25 settings)
**Station (4):** Callsign, Modifier, Park Number, Grid
**Radio (12):** Make, Model, Band, Control Mode, Connection Mode, CI-V, Baud, Serial Port
**Transmit (9):** Frequency, Sync Freq, Fake It, Offset, TX Delay, PTT Delay

### Advanced Tab (20 settings)
**Audio (2):** Bit Depth, Sample Rate
**Time (4):** UTC Offset, NTP Server, GPS Sync, Sync Button
**Decode (3):** Mode, Overrun Alert, Message Display
**Operating (7):** Auto Follow, Calling Options, Supervision, Reply Limit, Exclude
**Logging (2):** SWL Messages, SWL QSOs
**Monitoring (2):** SWR Alarm, ALC Alarm

### Cloud Tab (15 settings)
**PSK Reporter (2):** Enable, View Spots
**Cloudlog (5):** Enable, Server, API Key, Station ID, Test
**QRZ (3):** Enable, API Key, Test
**Maintenance (2):** Clear Cache, App Log
**Help (3):** FAQ, About

## Files Being Modified
1. `ui/ConfigFragment.java` - Simplified to manage tabs
2. `ui/BaseSettingsFragment.java` - NEW: Base class
3. `ui/BasicSettingsFragment.java` - Fully implemented
4. `ui/AdvancedSettingsFragment.java` - Fully implemented
5. `ui/CloudSettingsFragment.java` - Fully implemented
6. `layout/fragment_config.xml` - Replaced with tab layout
7. `layout/fragment_basic_settings.xml` - NEW
8. `layout/fragment_advanced_settings.xml` - NEW
9. `layout/fragment_cloud_settings.xml` - NEW

## Backup Files Created
- `ui/ConfigFragment_backup.java` - Original implementation
- `layout/fragment_config_backup.xml` - Original layout

## Rollback Plan
If issues occur:
```bash
mv ConfigFragment_backup.java ConfigFragment.java
mv fragment_config_backup.xml fragment_config.xml
```

## Notes
- All existing GeneralVariables references preserved
- All DatabaseOpr write operations maintained
- All validation logic kept intact
- Dependencies (VOX mode, etc.) preserved

---
**Last Updated:** 2026-02-02
**Status:** In Progress
