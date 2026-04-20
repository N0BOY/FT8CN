# FT-710 Release Record And Diff Analysis

## Build Snapshot

- Date: 2026-04-20
- Branch: `Feature/FT710Support`
- Local commit: `0727ada`
- Commit message: `Stabilize validated FT-710 support set`
- Remote push:
  - `origin/Feature/FT710Support`
  - GitHub compare / PR entry:
    - `https://github.com/LeoLiXX/FT8CN/pull/new/Feature/FT710Support`

## APK Outputs

- Debug APK:
  - Path: `ft8cn/app/build/outputs/apk/debug/app-debug.apk`
  - Size: `22,350,536`
  - Build time: `2026-04-20 23:03:11`
- Release APK:
  - Path: `ft8cn/app/build/outputs/apk/release/app-release.apk`
  - Size: `20,368,214`
  - Build time: `2026-04-20 23:05:15`

## Release Build Note

- `assembleRelease` succeeded.
- The release build emitted many pre-existing string-format warnings in localized `strings.xml` files.
- These warnings did not block APK generation.
- This warning set is not specific to the FT-710 repair.

## Final Diff Vs Main

Relative to `main`, the final branch keeps the following code changes:

### FT-710 Identification

- `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/rigs/InstructionSet.java`
  - adds `InstructionSet.YAESU_FT710 = 23`
- `ft8cn/app/src/main/assets/rigaddress.txt`
  - adds `YAESU FT-710,00,38400,23`

### FT-710 Routing Into Existing Yaesu CAT Logic

- `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/MainViewModel.java`
  - adds:
    - `case InstructionSet.YAESU_FT710:`
    - `baseRig = new YaesuDX10Rig();`
  - meaning:
    - FT-710 reuses the existing DX10 CAT command behavior
    - FT-710-specific repair stays outside the rig command set and is handled in the serial path

### FT-710 Default Control Mode

- `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ui/ConfigFragment.java`
  - when rig selection becomes FT-710:
    - force `GeneralVariables.controlMode = ControlMode.CAT`
    - persist `ctrMode`
    - refresh `setControlMode()` and `setConnectMode()`
  - meaning:
    - reduces operator friction
    - avoids repeatedly selecting CAT by hand
    - not the root fix for `0` power, but an important usability correction

### Core Serial-Path Repair

- `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/connector/CableSerialPort.java`
  - adds `shouldUseFt710WriteOnlyCatMode()`
  - skips `usbIoManager.start()` only when:
    - instruction set is FT-710
    - connection mode is USB cable
    - control mode is CAT
  - meaning:
    - FT-710 USB CAT becomes write-only
    - background serial read loop is disabled only for the FT-710 USB CAT path

### Bluetooth Guardrail

- `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/bluetooth/BluetoothStateBroadcastReceive.java`
  - restores `shouldHandleBluetoothAudioRouting()`
  - limits Bluetooth audio state handling to Bluetooth connection mode
  - meaning:
    - avoids unrelated Bluetooth audio routing reactions when the app is not actually operating in Bluetooth mode
    - this is not part of the FT-710 USB fix, but is a correctness guardrail worth keeping

### CDC ACM Safety Wrapper

- `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/serialport/CdcAcmSerialDriver.java`
  - adds `claimInterfaceSafely(...)`
  - centralizes interface null-checks and force-claim logging
  - meaning:
    - behavior stays effectively force-claim based
    - this is more of a defensive cleanup than a proven FT-710 core fix

### Mic Recorder Hardening

- `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/wave/MicRecorder.java`
  - delays `AudioRecord` creation until needed
  - recreates `AudioRecord` when invalid
  - retries `startRecording()`
  - releases recorder in `stopRecord()`
  - meaning:
    - this is a recorder robustness patch
    - it is not strongly supported as the root fix for the FT-710 transmit issue

### Repo Hygiene

- `.gitignore`
  - adds local Android / Gradle / IDE / APK ignore rules

## Root Cause Analysis

### Most Likely Root Cause

The strongest final interpretation is:

- the original FT-710 failure was not mainly caused by FT8 waveform generation
- it was not mainly caused by `DATA-U` mode text alone
- it was not mainly caused by the recorder path alone
- it was most likely caused by FT8CN treating FT-710 USB CAT too much like the inherited DX10 serial model

That matters because FT-710 presents a composite USB scenario:

- USB CAT serial interface
- USB audio interface
- Android USB audio session

The repeated field evidence showed:

- before FT8CN connected CAT, `DATA-U + manual PTT + music player` could produce audio
- after FT8CN connected CAT, even an external music player could lose audio
- killing FT8CN alone was not always enough to restore playback
- restarting the media player app restored playback

This evidence points much more strongly to:

- FT8CN disturbing the Android USB audio session during CAT operation
- rather than simply generating the wrong FT8 baseband signal

### Why The Serial Read Loop Became The Prime Suspect

The turning point in the investigation was the A-B result where:

- FT-710 CAT remained connected
- but the serial background read loop was disabled

In that state:

- the Android music player was no longer broken by FT8CN
- FT8 playback audio and external music could coexist
- FT-710 `DATA-U` transmit behavior became normal enough to validate the path

This is the strongest practical evidence in the whole debugging chain.

So the final high-confidence hypothesis is:

- FT-710 over USB CAT is unusually sensitive to continuous serial background reads or inherited polling behavior
- that background activity interferes with the composite USB audio session on Android
- once the read loop is removed, the USB audio path becomes stable again

## Final Repair Strategy

### Strategy Summary

The final strategy was intentionally conservative:

1. Add FT-710 as a selectable rig.
2. Route FT-710 to the existing DX10 CAT command set.
3. Force FT-710 default control mode to CAT in the configuration UI.
4. Keep the real FT-710 repair isolated to the serial layer.
5. Disable the FT-710 USB CAT background read loop, leaving CAT write-only.

### Why This Strategy Is Better Than The Earlier Attempts

Earlier attempts explored many directions:

- mode forcing
- TX timing compensation
- audio padding
- alternative `AudioTrack` formats
- output binding and route reporting
- debug-heavy diagnostics

Most of those either:

- had no measurable effect
- or improved observability without being necessary for the repair itself

The current final set is smaller and more defensible because it says:

- keep command behavior as close to existing Yaesu support as possible
- only special-case the FT-710 where the field evidence clearly demanded it
- keep the special case at the USB serial coexistence boundary

## What Looks Core Vs Non-Core

### High Confidence Core

- `InstructionSet.YAESU_FT710`
- `rigaddress.txt` FT-710 entry
- `MainViewModel` FT-710 -> `YaesuDX10Rig`
- `ConfigFragment` FT-710 default CAT behavior
- `CableSerialPort` FT-710 USB CAT write-only / no read loop

### Medium Confidence / Supportive

- `BluetoothStateBroadcastReceive` Bluetooth-mode gating
  - good correctness change
  - not a direct FT-710 USB repair
- `CdcAcmSerialDriver` `claimInterfaceSafely(...)`
  - defensive cleanup
  - not strongly proven as the root fix

### Low Confidence As FT-710 Core Fix

- `MicRecorder` hardening
  - useful robustness improvement
  - likely independent from the actual FT-710 `DATA-U` repair
- `.gitignore`
  - repo hygiene only

## Why The Final Branch Is Still Reasonably Minimal

Compared with the earlier FT-710 debugging branch, the final branch no longer depends on:

- large debug UI additions
- route-report presentation
- speculative audio timing offsets
- FT-710-only audio padding
- explicit preferred-output binding
- exact USB `deviceId/productId` matching
- the earlier custom FT-710 rig subclass path
- aggressive mode-rewrite experiments

That subtraction matters because it increases confidence that the remaining serial-path change is not accidental noise.

## Remaining Uncertainty

Even after this convergence, two areas are still best described as supportive rather than proven:

- `CdcAcmSerialDriver` safety wrapper
- `MicRecorder` recorder hardening

If a future cleanup wants to get even closer to `main`, those are the two most reasonable next A-B rollback candidates after preserving the FT-710 serial-path core.

## Recommended Long-Term Direction

- Keep the FT-710 CAT write-only special case unless new evidence disproves it.
- Treat FT-710 as a DX10-compatible command target with a different USB coexistence profile.
- Avoid reintroducing background CAT polling for FT-710 USB unless it is tested as an explicit A-B change.
- Keep release notes and issue history in separate clean UTF-8 Markdown files to avoid mixing new conclusions into older garbled notes.
