# FT8CN Release Notes

## Version 0.93.105 (January 25, 2026)

### 🎨 UI Improvements

#### Callsign Queue Enhancements
- **Fixed empty state display in landscape mode**: The "( Empty )" text now properly appears in the callsign queue when the queue is empty in landscape orientation
  - Fixed layout constraints to use `wrap_content` with `maxHeight` instead of fixed constraints
  - Ensures the RecyclerView properly displays items when the queue is empty
- **Improved text visibility**: Changed callsign queue entry text color to black for better readability
  - Position number, callsign text, and drag handle now use black color
  - Provides better contrast against the background
- **Enhanced delete gesture feedback**: Added visual trash can icon when swiping to delete items from the callsign queue
  - Shows trash icon (`log_item_delete_icon`) during swipe gesture
  - Gray background appears while swiping
  - Matches the visual feedback pattern used in the calling list
  - Makes it clear that swiping will delete the item

#### Landscape Mode Improvements
- **Resizable spectrum/sequence divider**: Added draggable divider to resize the split between spectrum view and call sequence/queue section in landscape mode
  - Drag the purple divider left or right to adjust the split
  - Divider turns orange while dragging for visual feedback
  - Position is saved and restored on app restart
  - Divider position is constrained between 30% and 70% to prevent views from becoming too small
  - Uses SharedPreferences for persistent storage

### 🐛 Bug Fixes
- **Fixed callsign queue empty state**: Resolved issue where "( Empty )" placeholder text was not visible in landscape mode
  - Corrected RecyclerView layout constraints in landscape layout
  - Ensured proper initialization and visibility handling

---

## Version 0.93.73 (January 25, 2026)

### 🎨 UI Improvements

#### Settings Reorganization
- **Moved sequence dropdown to settings page**: The sequence order dropdown has been moved from the calling screen to the settings page for better organization
  - Sequence dropdown now appears in the settings page after the decode overrun settings
  - Removed from the calling screen to reduce clutter
  - Functionality remains the same - users can still manually select the sequence order
  - Added help button for sequence selection in settings

---

## Version 0.93.67 (January 25, 2026)

### 🔧 Auto Sequencing Improvements

This release includes comprehensive improvements to the auto sequencing system to enhance reliability and prevent sequencing failures. See [`AUTO_SEQUENCING_IMPROVEMENTS.md`](AUTO_SEQUENCING_IMPROVEMENTS.md) for detailed documentation of all fixes and improvements.

#### Core Fixes
- **Fixed compound callsign matching**: Replaced `.equals()` with `checkCallsignIsCallTo()` to properly handle compound callsigns (e.g., "K1ABC/P", "W1ABC/MM")
- **Fixed no-reply retry calculation**: Corrected off-by-one error in retry limit logic
  - Previously: Setting retry limit to 2 required 3 no-replies before moving on
  - Now: Setting retry limit to 2 correctly moves on after 2 no-replies
  - Changed comparison from `>` to `>=` for proper limit enforcement
- **Improved unparseable message handling**: Added logging for debugging when valid messages from target callsigns cannot be parsed
- **Enhanced null safety**: Added defensive null checks to prevent potential crashes in edge cases
- **Added bounds checking**: Prevented potential IndexOutOfBoundsException when accessing message lists

#### Sequence Progression Improvements
- **Fixed sequence evaluation during TX**: Sequence now advances even when decoded messages arrive after transmission has started
  - Previously, decodes received during active transmission were ignored
  - Now evaluates all decodes (regular and deep decode) regardless of transmission state
  - Sequence can advance based on responses received mid-transmission cycle
- **Deep decode messages can advance sequence**: Removed restriction that prevented deep decode messages from triggering sequence advancement
  - Weak signals detected by deep decode can now properly advance the QSO sequence
  - Both regular and deep decode messages are evaluated equally

#### Documentation
- **Comprehensive sequencing documentation**: Added detailed documentation in `AUTO_SEQUENCING_IMPROVEMENTS.md`
  - Documents all identified and fixed issues
  - Explains message processing order (newest-first)
  - Documents transmission cycle behavior
  - Explains why messages don't switch mid-transmission (by design)

### 🎨 UI Improvements

#### Settings Labels
- **Updated auto track CQ label**: Changed to "Show CQs in calling window" for clarity
- **Updated auto call tracked label**: Changed to "Auto Reply to anyone calling CQ" for clarity
- Labels now remain static (don't change when toggles are selected/unselected)

#### Default Settings
- **Auto track CQ disabled by default**: Changed default value from `true` to `false`
- **Auto call tracked disabled by default**: Changed default value from `true` to `false`
- Users must explicitly enable these features if desired

### 🔒 Security & Compliance

#### Google Play Protect Compliance
- **Added backup rules**: Created `backup_rules.xml` to restrict what data can be backed up
  - Excludes sensitive data (databases, credentials, API keys) from backup
  - Addresses Google Play Protect warning about unrestricted backup access
- **Added network security configuration**: Created `network_security_config.xml` to restrict cleartext traffic
  - Allows cleartext only for local networks (needed for radio control)
  - Requires HTTPS for external connections
  - Addresses Google Play Protect warning about unrestricted cleartext traffic
- **Updated AndroidManifest**: Added references to security configuration files
  - `android:dataExtractionRules="@xml/backup_rules"`
  - `android:fullBackupContent="@xml/backup_rules"`
  - `android:networkSecurityConfig="@xml/network_security_config"`


### 🐛 Bug Fixes
- **Auto sequencing reliability fixes**: Multiple bugs fixed in the auto sequencing system
  - Fixed compound callsign matching bug
  - Fixed no-reply retry limit off-by-one error
  - Fixed sequence progression during active transmission
  - Fixed deep decode sequence advancement
  - Added defensive programming improvements (null checks, bounds checking)
  - See [`AUTO_SEQUENCING_IMPROVEMENTS.md`](AUTO_SEQUENCING_IMPROVEMENTS.md) for complete list of fixes

---

## Version 0.93.47 (January 24, 2026)

### 🔧 Improvements
- **Rig list sorting**: Rig list is now sorted by make then model for easier navigation
- **Bold text for transmitting messages**: Decoded messages that match currently transmitting messages are displayed in bold text
- **Highlight messages calling your callsign**: Messages where someone is calling your callsign now have a bright green background for easy identification
- **Improved decoding**: Messages were dropped in the decoding, now more reliable decoding sequence. See `DECODE_IMPROVEMENTS.md`

### 🗑️ Removed Features
- **SWR/ALC toggles removed**: Removed SWR and ALC toggle switches from settings UI
- **SWR/ALC toast messages removed**: Removed informational toast messages showing SWR/ALC values during transmission
  - Warning messages for high SWR/ALC are still active (safety feature)
  - Removed "Always show SWR/ALC" toggle option

## Version 0.93.35 (January 24, 2026)

### 🎉 New Features
- **Manual frequency entry**: Added a manual frequency input to the band selection dialog (accepts MHz or Hz).
- **Editable band list**: Long-press a band to edit or delete it (default bands are locked). Band list is now short with just major US FT8 Frequencies.

### 🔧 Improvements
- **Band dialog styling**: Aligned band selection dialog colors with the rest of the app UI.
- **Release APK naming**: Release builds output `ft8cn-release-<version>.apk`.
- **Localization cleanup**: Cleaned up some dialogs and dropdowns to be better formated in English.

### 📝 Documentation
- **User guide**: Added `USER_GUIDE.md` with setup, operation, and troubleshooting guidance.

### Bug Fixes
- **Improved decoding**: Enhanced auto sequencing reliability and message processing
  - **Fixed sequence filtering bug**: Previously, if the first decoded message had the same sequence as the current transmit sequence, all subsequent messages (even those with different sequences) would be ignored. Now the system checks all messages and only skips processing when ALL messages have the same sequence as the transmit sequence, ensuring valid messages are not dropped
  - **Prevented sequence regression**: Fixed issue where receiving delayed or out-of-order messages could cause the auto sequencing to regress backward in the message sequence. The system now only advances forward, preventing sequence state corruption
  - **Improved message processing**: Auto sequencing now correctly processes messages from different time slots even when mixed with messages from the same time slot, resulting in more reliable QSO progression
  - **Enhanced robustness**: Added safeguards to handle edge cases with mixed sequence messages, ensuring the auto sequencing system maintains proper state throughout the QSO


## Version 0.93.11 (January 22, 2026)

### 🎉 New Features

#### Settings & Configuration
- **Always Show SWR/ALC Toggle**: Added a new setting to always display SWR and ALC values when transmitting, regardless of warning status
  - Located in Settings → Alarm Switches section
  - When enabled, shows real-time SWR and ALC values during all transmissions
  - Provides continuous monitoring of radio parameters

#### Build System
- **Automatic Version Bumping**: Version code and version name now automatically increment on each build
  - Version code increments by 1 on each build
  - Version name patch version auto-increments (e.g., 0.93.10 → 0.93.11)
  - Version information stored in `app/version.properties`
  - Added `version.properties.example` for reference

#### Unit Testing
- Added unit test framework support
  - JUnit 4.13.2
  - Mockito 5.1.1
  - Hamcrest 2.2
  - Sample test: `MonotonicClockTest.java`
  - Run tests with: `.\gradlew.bat test` (Windows) or `./gradlew test` (Linux/macOS)

### 🔧 Improvements

#### QRZ.com Integration
- **Enhanced QRZ Upload Feedback**:
  - Success toast messages now display the callsign that was logged
  - Failure toast messages show the specific error response from QRZ API
  - Improved error handling with URL decoding for error messages
  - Better logging for debugging upload issues

#### SWR/ALC Display
- **Real-time Value Display**: SWR and ALC values now shown with warnings
  - Warning messages include actual SWR and ALC values
  - Values displayed when transmitting, even without warnings (if toggle enabled)
  - All rig implementations updated (Icom, Yaesu, Kenwood, Xiegu, Elecraft, TrUSDX, Wolf_sdr)

#### Distance Measurements
- **Unit Conversion**: Changed all distance measurements from kilometers to miles
  - Updated `MaidenheadGrid.java` to convert distances to miles
  - Updated all string resources across all languages (English, Japanese, Chinese variants, Spanish, Greek)
  - Updated layout files and comments

#### Build Configuration
- **Build Features**: Explicitly enabled `buildConfig` and `dataBinding` features
  - Ensures `BuildConfig.VERSION_NAME` and `BuildConfig.VERSION_CODE` are generated
  - Required for newer Android Gradle Plugin versions

#### String Resources
- **Fixed Multiple Substitution Warnings**: Converted all string resources with multiple substitutions to positional format
  - Fixed warnings in all language files (English, Japanese, Chinese Simplified/Traditional/HK/MO, Spanish, Greek)
  - Changed patterns like `%s(%s)` to `%1$s(%2$s)` for proper Android resource handling
  - Build now completes without warnings

### 🐛 Bug Fixes

#### QRZ.com Upload
- Fixed QRZ.com API upload not working
  - Corrected URL encoding of ADIF data
  - Fixed API URL format to use proper query parameters
  - Added proper `User-Agent` header with callsign, app name, and version
  - Improved error handling and response parsing

#### Build System
- Fixed Java environment variable issues
  - Added permanent JAVA_HOME and ANDROID_HOME configuration instructions
  - Updated README with proper JDK 17 requirements
  - Fixed Gradle compatibility issues

### 📝 Documentation

#### README Updates
- Added comprehensive build prerequisites section
  - JDK 17 requirement clearly stated
  - Android SDK 33 requirement
  - Environment variable setup instructions for Windows, Linux, and macOS
  - Build commands and APK locations
  - Unit testing instructions

#### Icon Instructions
- Created `ICON_INSTRUCTIONS.md` with detailed steps for replacing the application icon
  - Recommended sizes and formats
  - File locations and naming conventions

### 🔄 Technical Changes

#### Project Structure
- Project reorganization and cleanup
- Updated `.gitignore` to exclude `version.properties`
- Improved build configuration

#### Dependencies
- Updated Gradle wrapper to 8.5
- Maintained Android Gradle Plugin 7.4.1 for compatibility
- Added test dependencies

### 📊 Statistics

- **Files Changed**: 590 files
- **Lines Added**: ~18,067
- **Lines Removed**: ~4,693
- **Languages Supported**: 8 (English, Japanese, Chinese Simplified/Traditional/HK/MO, Spanish, Greek)

### 🎯 Compatibility

- **Minimum Android Version**: 6.0 (API 23)
- **Target Android Version**: 13 (API 33)
- **Compile SDK**: 33
- **Java Version**: JDK 17 required for building

### 🙏 Acknowledgments

Special thanks to all contributors and testers who helped improve FT8CN, especially:
- BG7YOZ (original developer)
- N0BOY (repository maintainer)
- All community contributors and testers

---

## Previous Versions

### Version 0.93 (January 2025)
- Cloudlog and QRZ.com support integration
- Various bug fixes and improvements

### Version 0.92 (January 2024)
- TrUSDX support with audio streaming over CAT
- Various radio rig improvements

### Version 0.91
- Yaesu rig constant updates
- Bug fixes

### Version 0.90
- Major project restructuring
- File synchronization improvements

### Version 0.89
- User manual updates
- Documentation improvements

---

**Note**: These release notes cover changes made since forking the project. For the complete project history, please refer to the git commit log.
