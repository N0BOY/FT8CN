# FT8CN User Guide

FT8CN is an Android application that decodes and transmits FT8/FT4 signals natively on a phone or tablet. It is intended for research and amateur radio use and includes tools for decoding, calling, logging, and optional rig control.

This guide focuses on how to operate the app. Build instructions are in `README.md`.

## Safety, compliance, and responsibility

- Operate only under the rules and regulations that apply in your country.
- Use a valid amateur license and callsign where required.
- FT8CN is provided as-is; you are responsible for how you use it.
- Mobile hardware has limited performance and power; decode depth and processing are optimized for mobile use.

## App overview

FT8CN is organized into main screens reachable from the bottom navigation bar:

- **Decode**: Live decode list and spectrum.
- **Calling**: Create and control outbound messages.
- **QSO Logs**: Review and manage logs.
- **Settings**: Configure audio, rig control, filters, and other options.

Many screens are built around:

- A **waterfall/spectrum** view.
- A **decode list** showing recent messages.
- A **toolbar** with controls and status indicators.

## Getting started

1. **Install the APK** (download from releases or build from source).
2. **Grant permissions** when prompted (microphone, storage, Bluetooth/USB as needed).
3. **Set your callsign and grid** in Settings.
4. **Pick an audio path**:
   - Internal audio (device mic/speaker).
   - External audio (USB/Bluetooth audio interface).
5. **Set the time** accurately (FT8 depends on accurate time).

## Time synchronization

FT8 requires accurate time. If the app shows time offset warnings:

- Sync your device time using NTP or your OS time sync.
- Reduce background battery optimization that may block time updates.
- Use the time offset setting if available to compensate for small offsets.

## Decode screen

The Decode screen typically includes:

- **Spectrum/Waterfall**: Visualizes signals in the audio passband.
- **Decode list**: Shows decoded messages for each cycle.
- **Filters**: Limit list to CQ or specific callsigns (if enabled).

Typical workflow:

1. Press **Decode** to start/stop decoding.
2. Verify the **audio frequency** range and signal visibility.
3. Tap a decoded message to populate calling fields (if enabled).

## Calling screen

Use this screen to craft and transmit sequences.

Common controls:

- **Transmit sequence**: Step through standard FT8 messages.
- **Free text**: Send custom text when allowed.
- **TX/RX split**: Choose whether TX and RX are locked or separated.
- **Frequency**: Choose operating band or audio frequency.

Typical workflow:

1. Select a decoded message or manually enter target callsign.
2. Choose a **message mode** (standard or free text).
3. Confirm **TX frequency** and audio output.
4. Start transmission with **TX**.

## Settings

Settings include several major areas. Exact names may vary by version.

### Callsign & grid

- Set **My Callsign** and **Maidenhead grid**.
- These values are required for proper message composition and logging.

### Audio

- **Audio output format**: 16‑bit or 32‑bit float.
- **Sample rate**: 12 kHz / 24 kHz / 48 kHz.
- Choose a configuration compatible with your device or interface.

### Rig control (CAT / VOX / CI‑V / USB / Bluetooth)

- **Control mode**: VOX or CAT.
- **Serial port / Bluetooth device**: Select and connect.
- **PTT delay** and **TX delay**: Adjust timing for your rig.
- **Rig model / CI‑V address**: Required for some radios.

### Filters and decode options

- Filter decoded messages (all, confirmed, unconfirmed, or by callsign).
- Toggle visual markers and spectrum options.

### Transmission supervision

- **TX watchdog** (launch supervision) and **no‑response limit** can stop auto‑transmit loops.
- **SWR/ALC warning** and **always show SWR/ALC** (if enabled) for safety.

### Logging and export

- Configure log storage and export settings.
- Optional auto‑sync to Cloudlog or QRZ.com.

## Logging (QSO Logs)

QSO logs record contacts and confirmations.

- **Confirmed / unconfirmed** statuses.
- **Manual confirmation** (toggle per contact).
- **Distance and grid** fields shown in list views.
- Export tools to share logs for backup or third‑party services.

## Cloudlog and QRZ.com integration

If enabled in Settings:

- **Cloudlog**: Provide server address and API key.
- **QRZ.com**: Provide API key and station info.

Tips:

- Confirm network connectivity before upload.
- Watch for toast messages indicating success or failure.

## Common issues and fixes

### No decodes

- Check that your **audio input** is active.
- Ensure **sample rate** and **bit depth** match your interface.
- Verify **time sync** (FT8 is time‑critical).

### PTT or rig does not key

- Confirm **control mode** (VOX/CAT).
- Verify **serial/Bluetooth** connection. Typically use the second adverised USB connection. Be sure to use an OTG cable.
- Check **PTT delay** and **rig model/CI‑V address**.

### Very poor decode quality

- Reduce device load (close other apps).
- Ensure clean audio levels (avoid clipping).
- Use external audio interfaces if possible.

### Upload or sync fails

- Confirm API credentials.
- Check network connectivity.
- Retry and check logs or toast messages.

## Tips for reliable operation

- Use **stable power**; phones can throttle under low battery.
- Keep **screen on** while operating to avoid audio suspension.
- Use **airplane mode + Wi‑Fi** to reduce RF noise from cellular radios.
- Keep **TX duty cycle** reasonable to avoid overheating your rig or device.

## Where to get help

- See **FAQ** in the app (if available).
- Check the repository issues and release notes.
- Provide device model, Android version, and rig details when reporting issues.

