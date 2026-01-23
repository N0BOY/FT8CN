# Icon Replacement Instructions

## Current Icon Setup

The app currently uses a drawable icon:
- **Main icon**: `ft8cn/app/src/main/res/drawable/ft8cn_icon.png`
- Referenced in AndroidManifest.xml as `@drawable/ft8cn_icon`

The app also has adaptive icon resources in:
- `ft8cn/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- `ft8cn/app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`

## Required Icon Sizes

You need to create icons in the following sizes and place them in the corresponding directories:

### Adaptive Icon (Android 8.0+)
- **Foreground**: 108x108 dp (safe zone: 72x72 dp)
- **Background**: 108x108 dp
- Formats: PNG or Vector Drawable

### Legacy Icons (Android 7.1 and below)
- **mipmap-mdpi**: 48x48 px
- **mipmap-hdpi**: 72x72 px
- **mipmap-xhdpi**: 96x96 px
- **mipmap-xxhdpi**: 144x144 px
- **mipmap-xxxhdpi**: 192x192 px

## Icon Design Requirements

Create an icon showing:
- An Android robot/mascot holding the letters "FT8"
- The design should be clear and recognizable at small sizes
- Use high contrast colors for visibility
- Ensure the "FT8" text is readable

## Steps to Replace Icons

1. **Create your icon image** (recommended: 1024x1024 px source)
   - Design: Android robot holding "FT8" letters
   - Export as PNG with transparency

2. **Generate adaptive icon layers**:
   - **Foreground**: Android robot with FT8 letters (108x108 dp safe zone)
   - **Background**: Solid color or gradient background (108x108 dp)

3. **Generate legacy icon sizes**:
   - Use Android Asset Studio or similar tool
   - Or manually resize to each mipmap size

4. **Replace files**:
   - **Main icon**: Replace `ft8cn/app/src/main/res/drawable/ft8cn_icon.png` with your new icon
   - **Adaptive icon foreground**: Place in `ft8cn/app/src/main/res/drawable/ic_launcher_foreground.xml` (or PNG)
   - **Adaptive icon background**: Place in `ft8cn/app/src/main/res/drawable/ic_launcher_background.xml` (or PNG)
   - Replace legacy icons in each mipmap folder (if using adaptive icons)

5. **Test the icon**:
   - Build the app and verify it appears correctly
   - Check on different Android versions if possible

## Tools for Icon Generation

- **Android Asset Studio**: https://romannurik.github.io/AndroidAssetStudio/icons-launcher.html
- **Icon Kitchen**: https://icon.kitchen/
- **Figma/Photoshop**: Design and export manually

## Note

The current icon files are in WebP format. You can replace them with PNG files of the same name, or convert your PNGs to WebP for better compression.
