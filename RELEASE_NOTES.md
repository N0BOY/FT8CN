# FT8CN Release Notes

## Version 0.93.176 (January 28, 2026)

### 🎉 New Features

#### Settings & Configuration
- **GPS Time Sync Setting (Experimental)**: Added toggle to enable/disable GPS time synchronization
  - Located in Settings → UTC Time Offset section
  - Disabled by default (experimental feature)
  - When enabled, time sync attempts GPS first, then falls back to NTP if GPS fails
  - When disabled, time sync uses NTP only (previous behavior)
  - Setting is stored in database and persists across app restarts
  - Default value is automatically written to database on first startup if missing
  - Provides users with control over GPS time sync behavior

### 🔧 Technical Improvements

#### Time Synchronization
- **Configurable GPS sync**: GPS time sync is now optional and controlled by user setting
  - `UtcTimer.syncTime()` checks `GeneralVariables.enableGpsTimeSync` before attempting GPS
  - If GPS sync is disabled, goes directly to NTP synchronization
  - Maintains backward compatibility (default behavior unchanged)
  - GPS sync advantages: More accurate, works without internet, typically accurate to milliseconds
  - NTP fallback: Still available if GPS fails or is disabled

---

## Version 0.93.171 (January 28, 2026)

### 🎨 UI Improvements

#### QSO Log Filtering Enhancements
- **QRZ Upload Status Filter**: Added filter option to show all QSOs, only uploaded QSOs, or only missing QSOs
  - Filter options: "Show all", "QRZ uploaded", "QRZ missing"
  - Helps identify which QSOs still need to be uploaded to QRZ.com
  - Filter is applied to both log display and ADIF export/share
- **Date Range Filter**: Added start date and end date filtering for QSO logs
  - Format: YYYYMMDD (e.g., "20240101" for January 1, 2024)
  - Filter QSOs by QSO end date (`qso_date_off`)
  - Empty fields mean no date filtering applied
  - Enables time-based log analysis and export
- **Comment Text Filter**: Added ability to search QSOs by text content in comment field
  - Case-insensitive partial matching (LIKE search)
  - Useful for searching by location, distance, or other information stored in comments
  - Works together with other filters
- **Filter Visual Indicator**: Added visual indicator showing which filters are currently active
  - Displays below the action bar when filters are active
  - Shows comma-separated list of active filters
  - Updates automatically when filters change
  - Makes it clear when the log display is filtered
- **Removed QSL Status Filter**: Removed confirmed/unconfirmed QSL status filter to simplify UI
  - Focus is now on QRZ upload status instead

#### Share Logs Filename Customization
- **Custom filename dialog**: Added dialog to specify custom filename when sharing/exporting logs
  - Default filename is "FT8CN" but can be customized
  - Filename sanitization removes invalid characters (/ \ : * ? " < > |)
  - Makes it easier to organize exported log files

### 🔧 Technical Improvements

#### ADIF Export Enhancements
- **POTA Support**: Added POTA (Parks on the Air) fields to ADIF export
  - Automatically includes `MY_SIG: POTA` and `MY_SIG_INFO: [park_number]` when park number is present
  - Band name converted to uppercase for POTA compliance (e.g., "20m" → "20M")
  - Enables proper POTA log submission

#### Filter Integration
- **Unified Filter System**: All filters (callsign, comment, QRZ status, date range) work together seamlessly
  - Filters are combined with SQL AND clauses
  - Share ADIF respects all filter settings
  - Filter state persists during app session via MainViewModel
  - Database queries optimized with dynamic parameter building

### 📚 Documentation
- **Filter Improvements Documentation**: Added comprehensive documentation in `wiki/FILTER_IMPROVEMENTS.md`
  - Documents all filter types and their implementation
  - Includes usage examples and technical details
  - Explains filter combination logic

---

## Version 0.93.154 (January 27, 2026)

### 🐛 Bug Fixes

#### Calling Screen Message Display
- **Fixed messages not appearing on calling screen**: Resolved issue where decoded messages appeared on the decode screen but not on the calling screen
  - Removed early return condition in `findIncludedCallsigns()` that was preventing messages from being added to the calling screen during TX cycles
  - Messages are now always added to the calling screen regardless of transmission state
  - Users can now see who's calling them even during active transmission cycles
  - Fixes issue where stations calling the user would only appear on decode screen but not calling screen

### 🔧 Configuration Changes

#### Audio Capture Duration
- **Changed audio capture duration**: Modified audio capture from 15 seconds to 13 seconds
  - Decode cycle still triggers every 15 seconds (FT8 protocol requirement)
  - Audio capture now captures 13 seconds of audio per cycle
  - Decode starts 13 seconds after each timer trigger
  - **Note**: This change may affect decode reliability as FT8 messages are 12.64 seconds long and require full cycle context

---

## Version 0.93.153 (January 27, 2026)

### 🎤 Audio Input Monitoring & Control

#### Microphone Input Level Meter
- **Real-time microphone input level display**: Added visual microphone input level meter next to the microphone icon in the calling list
  - Displays input level as percentage (0% to 100%)
  - Updates in real-time (every 100ms) when microphone is active
  - Automatically hides when microphone is not recording
  - Level calculation based on RMS (Root Mean Square) of audio samples
  - Maps dBFS values (-60 dBFS to 0 dBFS) to percentage scale for intuitive display
- **Visual warning for high input levels**: Text color changes to red when input level exceeds 80%
  - Provides immediate visual feedback for potential clipping or distortion
  - Helps users adjust microphone gain or distance to prevent audio issues
  - Default text color (white/gray) used for normal levels

#### Microphone Input Gain Control
- **Adjustable microphone input gain**: Added microphone input gain slider in the volume settings dialog
  - Gain range: 25% (0.25x) to 200% (2.0x)
  - Default: 100% (1.0x, no gain change)
  - Gain is applied to audio samples in real-time before processing
  - Prevents clipping by clamping samples to valid range (-1.0 to 1.0)
  - Gain setting persists across app restarts (saved to database)
- **Unified audio control dialog**: Both output volume and microphone input gain controls are now in the same settings dialog
  - Signal output strength slider (top section)
  - Microphone input gain slider (bottom section)
  - Both controls include visual progress indicators
  - Consistent UI/UX for audio adjustments

### 🔧 Technical Improvements

#### Audio Processing
- **Real-time dB level calculation**: Added RMS-based dB level calculation in MicRecorder
  - Calculates dBFS (decibels relative to full scale) from audio samples
  - Updates LiveData for UI observation
  - Handles edge cases (silence, clipping) gracefully
- **Gain application**: Microphone input gain is applied to audio samples before passing to listeners
  - Gain multiplication with clipping protection
  - Affects both audio processing and dB level display
  - Real-time application without buffering delays

#### Database & Configuration
- **Mic input gain persistence**: Added database storage for microphone input gain setting
  - Config key: `micInputGain` (stored as percentage, 25-200)
  - Automatic loading on app startup
  - Value clamping to valid range [0.25, 2.0]
  - Default value: 1.0 (100%) if not set

### 🌐 Localization
- **Added microphone input gain string**: Added "Microphone input gain %.0f %%" string resource
  - Available in all supported languages

---

## Version 0.93.148 (January 27, 2026)

### 🧪 Testing & Quality Assurance

#### Comprehensive Unit Test Coverage
- **Added unit tests for high-touch areas**: Created comprehensive test suites for critical components
  - **CallsignQueue tests**: 20+ test cases covering queue operations, FIFO ordering, duplicate prevention, item reordering, and thread safety
  - **GeneralVariables tests**: 30+ test cases covering function order detection, message type classification, callsign matching, and exclusion list management
  - **DecodeDuplicateFilter tests**: 15+ test cases covering multi-factor duplicate detection, tolerance boundaries, SNR updates, and real-world scenarios
  - **QSLRecord tests**: 13 test cases covering record creation, validation, error handling, data import, and date/time formatting
  - **Database operations tests**: Tests for configuration operations, SQL construction, and data validation
  - **Settings validation tests**: 10 test cases covering input validation for all major settings fields

#### Test Infrastructure
- All 93+ unit tests passing
- Tests follow existing patterns and use JUnit 4
- Test results available in HTML format for easy review
- Tests help prevent regressions in critical areas

### 🔄 CI/CD Improvements

#### GitHub Actions Workflows
- **Unit tests workflow**: Automatically runs unit tests on pull requests and pushes to the `release` branch
  - Sets up JDK 17 and Android SDK API 33
  - Runs all unit tests with `./gradlew test`
  - Uploads test results as artifacts (retained for 7 days)
- **Release build workflow**: Manual workflow for creating release builds
  - Can be triggered manually from GitHub Actions UI
  - Optional version name input for custom versioning
  - Builds release APK with `./gradlew assembleRelease`
  - Uploads APK as artifact (retained for 30 days)
  - Creates build summary with version information

### 📚 Documentation Organization

#### Wiki Structure
- **Moved documentation to wiki folder**: Reorganized markdown documentation files
  - Moved `APPLICATION_FLOW.md`, `AUTO_SEQUENCING_IMPROVEMENTS.md`, `CALLSIGN_QUEUE.md`, `DECODE_IMPROVEMENTS.md`, `SETTINGS_DIALOG_UPDATES.md`, `SKIP_MY_GRID_WHEN_RESPONDING.md`, and `USER_GUIDE.md` to `wiki/` folder
  - Kept `README.md` and `RELEASE_NOTES.md` in root for GitHub visibility
  - Improved project organization and documentation discoverability

### 🔧 Technical Improvements

#### Code Quality
- Enhanced test coverage for critical business logic
- Improved validation and error handling in settings dialogs
- Better documentation of database operations and configuration management

---

## Version 0.93.113 (January 26, 2026)

### 🎨 UI Improvements

#### Calling Page Enhancements
- **Horizontal resize divider for spectrum/sequence split**: Added a draggable horizontal divider between the spectrum view and the queue manager/sequencing section in landscape mode
  - Drag the purple divider up or down to adjust the vertical split
  - Divider turns orange while dragging for visual feedback
  - Position is saved and restored on app restart (default: 65%)
  - Divider position is constrained between 30% and 85% to prevent views from becoming too small
  - Uses SharedPreferences for persistent storage
  - Works alongside the existing vertical divider for left/right resizing

#### QSO Logs Page Improvements
- **Simplified QRZ upload status display**: QSO logs now focus exclusively on QRZ.com upload status
  - Removed "Unconfirmed", "LoTW confirmed", and "Manually confirmed" status displays
  - Shows "QRZ.com uploaded" in green text when entry has been uploaded to QRZ
  - Shows "Not uploaded to QRZ.com" in default text color when not uploaded
  - Green text color (`holo_green_dark`) provides clear visual indication of uploaded status
- **Removed confirmation features**: Removed manual confirmation and cancel confirmation functionality
  - Removed long-press context menu items for manual confirmation and cancel confirmation
  - Removed swipe-left gesture that toggled QSL confirmation
  - Context menu still available for QRZ lookup and location features
  - Streamlined interface focuses on QRZ upload tracking

### 🌐 Localization
- **Added QRZ upload status strings**: Added "Not uploaded to QRZ.com" string resource to all supported languages
  - English: "Not uploaded to QRZ.com"
  - Chinese (Simplified): "未上传到QRZ.com"
  - Chinese (Traditional): "未上傳到QRZ.com"
  - Spanish: "No subido a QRZ.com"
  - Japanese: "QRZ.comにアップロードされていません"
  - Greek: "Δεν ανέβηκε στο QRZ.com"

---

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
