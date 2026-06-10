# Release Signing

This project reads Android release signing credentials from either:

- `FT8CN_RELEASE_*` environment variables
- a local `keystore.properties` file in the `ft8cn/` project root

Environment variables take priority, which makes CI/CD setup straightforward.

## Local files

- `keystore.properties`
- `release-keystore.jks` or your preferred local keystore filename

Neither file should be committed.

## Expected format

Copy `keystore.properties.example` to `keystore.properties` and fill in your local values:

```properties
storeFile=release-keystore.jks
storePassword=your-store-password
keyAlias=ft8cn-release
keyPassword=your-key-password
```

`storeFile` can be a relative path under `ft8cn/` or an absolute path on your machine.

## Environment variables

These variables override `keystore.properties` when present:

```text
FT8CN_RELEASE_STORE_FILE
FT8CN_RELEASE_STORE_PASSWORD
FT8CN_RELEASE_KEY_ALIAS
FT8CN_RELEASE_KEY_PASSWORD
```

Example for PowerShell:

```powershell
$env:FT8CN_RELEASE_STORE_FILE = "release-keystore.jks"
$env:FT8CN_RELEASE_STORE_PASSWORD = "your-store-password"
$env:FT8CN_RELEASE_KEY_ALIAS = "ft8cn-release"
$env:FT8CN_RELEASE_KEY_PASSWORD = "your-key-password"
.\gradlew assembleRelease
```

If you use a PKCS12 keystore, `FT8CN_RELEASE_KEY_PASSWORD` is usually the same as `FT8CN_RELEASE_STORE_PASSWORD`.

## Build

Run the release build from `ft8cn/`:

```powershell
.\gradlew assembleRelease
```

If neither the environment variables nor `keystore.properties` provide a complete signing configuration, the release build fails early with a clear error.
