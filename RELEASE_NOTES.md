# FT8CN Release Notes

## Unreleased

### 🎉 New Features
- **Manual frequency entry**: Added a manual frequency input to the band selection dialog (accepts MHz or Hz).
- **Editable band list**: Long-press a band to edit or delete it (default bands are locked).

### 🔧 Improvements
- **Band dialog styling**: Aligned band selection dialog colors with the rest of the app UI.
- **Release APK naming**: Release builds output `ft8cn-release-<version>.apk`.

### 📝 Documentation
- **User guide**: Added `USER_GUIDE.md` with setup, operation, and troubleshooting guidance.

### 🐛 Bug Fixes
- **Japanese string formatting**: Fixed non-positional placeholder in `values-ja/strings.xml`.

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
