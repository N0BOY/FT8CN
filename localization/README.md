# Language Localization Files

This directory contains language-specific string resources that have been moved outside the build directory to prevent them from being bundled into the APK and to protect them from automated modifications.

## Current Languages

The following language directories are stored here:

- `values-el/` - Greek (Ελληνικά)
- `values-es/` - Spanish (Español)
- `values-ja/` - Japanese (日本語)
- `values-zh-rCN/` - Chinese Simplified (简体中文)
- `values-zh-rHK/` - Chinese Hong Kong (繁體中文 - 香港)
- `values-zh-rMO/` - Chinese Macau (繁體中文 - 澳門)
- `values-zh-rTW/` - Chinese Traditional (繁體中文 - 台灣)

## How to Add Languages Back to the Build

To include these languages in your APK build, copy the desired language directories back to the Android resources folder:

### Windows (PowerShell):
```powershell
# Copy all languages back
Copy-Item -Path "localization\values-*" -Destination "ft8cn\app\src\main\res\" -Recurse

# Or copy specific languages
Copy-Item -Path "localization\values-el" -Destination "ft8cn\app\src\main\res\values-el" -Recurse
Copy-Item -Path "localization\values-es" -Destination "ft8cn\app\src\main\res\values-es" -Recurse
```

### Linux/macOS:
```bash
# Copy all languages back
cp -r localization/values-* ft8cn/app/src/main/res/

# Or copy specific languages
cp -r localization/values-el ft8cn/app/src/main/res/
cp -r localization/values-es ft8cn/app/src/main/res/
```

### Manual Method:
1. Navigate to the `localization/` directory
2. Copy the desired `values-*` directory (e.g., `values-el`)
3. Paste it into `ft8cn/app/src/main/res/`
4. The language will be included in the next build

## Adding New Languages

To add a new language:

1. Create a new directory in `localization/` following Android naming conventions:
   - `values-<language code>/` (e.g., `values-fr` for French)
   - `values-<language code>-r<region code>/` (e.g., `values-fr-rCA` for French Canada)

2. Copy `ft8cn/app/src/main/res/values/strings.xml` to your new directory

3. Translate all string values in the new `strings.xml` file

4. Follow the instructions above to add it to the build when ready

## Removing Languages from Build

To remove languages from the build (to reduce APK size):

1. Copy the language directory from `ft8cn/app/src/main/res/` to `localization/` (if not already there)
2. Delete the language directory from `ft8cn/app/src/main/res/`
3. Rebuild the APK

## Notes

- The base English strings are always included in `ft8cn/app/src/main/res/values/strings.xml`
- Language directories in `localization/` are **not** included in APK builds
- Language directories must be in `ft8cn/app/src/main/res/` to be bundled
- This separation prevents automated tools and AI agents from modifying language files during builds
- Language files are preserved here for future use or manual inclusion

## Size Impact

Removing all languages saves approximately **150-200 KB** from the APK size. For better size optimization, consider using Android App Bundle (AAB) format which automatically splits languages.
