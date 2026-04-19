# FT-710 支持改动总结

## 目标

本次改动的目标是为 `Yaesu FT-710` 在 Android 工程中建立独立支持分支，并围绕以下问题持续修正与排障：

- 机型识别与配置独立化
- `CAT` 控制默认化
- 发射前后音频/录音切换稳定化
- `DATA-U` 下无功率输出问题定位
- 应用内调试能力建设

## 已保留的改动

### 1. FT-710 独立机型支持

- 新增 `InstructionSet.YAESU_FT710`
- 新增 `YaesuFT710Rig`
- 在 `rigaddress.txt` 中加入 `YAESU FT-710`
- 配置列表中单独显示 FT-710，而不是继续复用 FTDX10 入口

### 2. 配置与控制模式

- 选择 FT-710 时，默认将控制方式切到 `CAT`
- 将 FT-710 选项排在 FTDX10 前面，降低误选成本

### 3. 发射前后稳定性修正

- 发射前暂停录音，发射完成后恢复
- 改善发射后录音器未初始化导致的恢复失败
- 蓝牙连接消息与音频路由切换只在蓝牙模式下介入

### 4. 调试体系

- 配置页新增 `Debug` 开关
- 新增调试状态持久化
- 应用内 Debug 面板可显示 FT-710 关键日志
- 新增音频路由调试输出到 HTTP Debug 页面

### 5. FT-710 模式切换验证逻辑

- 当前保留 `RTTY-U -> DATA-U` 两步切换逻辑
- 这是针对用户观察到的关键线索保留的验证性方案，不属于最终结论

## 已证伪并回收的尝试

### 1. FT-710 专属音频格式兼容链

以下方向均未解决 `DATA-U` 下 `0` 功率问题：

- `48kHz / stereo / stream`
- `44.1kHz / mono / 16bit / stream`
- `44.1kHz / mono / 16bit / static preload`
- 满音量强推

结论：

- 这条路线只改变了脉冲/起播表现，没有解决持续功率输出问题
- 已从 FT-710 专属音频播放分支中移除

### 2. 缺乏依据的 EX 菜单命令猜测

- 曾尝试过基于猜测的 FT-710 菜单命令
- 无有效改善，且手册依据不足
- 已回收

## 当前工作判断

### 最可信线索

- 误切到 `RTTY-U` 后，用户手动切回 `DATA-U` 曾出现输出

### 当前倾向

相比“Android 音频格式错误”，当前更倾向于：

- FT-710 的模式切换序列问题
- `DATA-U` 进入后的内部数据音频入口状态问题

## 涉及文件

### 机型与指令

- `ft8cn/app/src/main/assets/rigaddress.txt`
- `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/rigs/InstructionSet.java`
- `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/rigs/YaesuFT710Rig.java`
- `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/rigs/Yaesu3RigConstant.java`

### 配置与调试

- `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/GeneralVariables.java`
- `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/database/DatabaseOpr.java`
- `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ui/ConfigFragment.java`
- `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ui/ToastMessage.java`
- `ft8cn/app/src/main/res/layout/fragment_config.xml`
- `ft8cn/app/src/main/res/values/strings.xml`
- `ft8cn/app/src/main/res/values-zh-rCN/strings.xml`

### 发射与音频链

- `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/MainViewModel.java`
- `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ft8transmit/FT8TransmitSignal.java`
- `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/audio/AudioRouteHelper.java`
- `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/wave/MicRecorder.java`
- `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/bluetooth/BluetoothStateBroadcastReceive.java`
- `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/html/LogHttpServer.java`

## 当前未解决问题

- `DATA-U` 下仍然可能出现 `ALC=0 / PO=0`
- 根因尚未完全锁定
- 下一阶段应优先利用新调试开关和 Debug 面板继续定位
## Latest Findings 2026-04-15

### Verified But Ineffective

- Disabling FT-710 auto `RTTY-U -> DATA-U` switching did not change the symptom.
- Fully releasing `AudioRecord` before TX did not change the symptom.
- Switching FT-710 TX audio to a media-player-like path:
  - `48kHz`
  - `stereo`
  - `16bit`
  - `MODE_STREAM`
  still did not produce RF output.

### New Critical Log Evidence

- Captured log:
  - `FT8TransmitSignal partial write expected=1213456, actual=0, trackMode=1`
- Conclusion:
  - The failure has narrowed to the `AudioTrack` stream write stage
  - Audio is not successfully entering the playback stream

### Important Field Observation

- Before FT8CN connects, `DATA-U + manual PTT + music player` works on FT-710.
- After FT8CN connects to the serial interface, music playback disappears.
- Killing FT8CN does not immediately restore playback.
- Restarting the music player app restores playback.

### Working Hypotheses

- FT8CN serial connection may disturb the Android USB audio playback session on the FT-710 composite USB device.
- `AudioTrack.write(...) == 0` is now the most direct technical failure point and should remain the top investigation target.

## Latest Change 2026-04-15

- Refocused debugging priority onto the observed symptom:
  - after FT8CN connects CAT, even an external music player loses FT-710 audio
- Implemented serial-side mitigation and instrumentation:
  - `CableSerialPort` now matches the selected USB device by `deviceId/productId` before falling back
  - `CableSerialPort` now logs full USB interface layout for the chosen device
  - `CableSerialPort` now emits audio-route snapshots before and after serial open
  - `CdcAcmSerialDriver` now prefers non-forced interface claims and only retries with forced claim when necessary
- Reason:
  - this symptom suggests the serial open path may be disturbing the FT-710 composite USB audio session at the Android USB layer

## Added Observation 2026-04-16

- The debug screenshots also show at least one TX sequence that starts and then cuts off roughly 1 second later.
- Representative sequence:
  - `15:46:15` TX start / `playFT8Signal`
  - `15:46:16` `finishPlaybackOnce`
  - `15:46:16` `afterPlayAudio release track`
  - `15:46:16` `PTT OFF`
- This is now considered a primary clue because it suggests early playback termination is part of the FT-710 failure path.

## Breakthrough 2026-04-16

### Confirmed Effective

- Suspending the FT-710 CAT serial background read loop stops FT8CN from breaking the Android music player.
- In the same diagnostic build, FT8 playback audio and external music playback can be heard together.

### Confirmed Ineffective

- Suspending only the FT-710 USB mic recorder did not solve the player breakage.

### Current Best Interpretation

- The dominant conflict is now believed to be on the USB serial read / polling side, not on the USB audio playback side alone.
- For FT-710, the inherited DX10 CAT model is too aggressive for this CP2105 + USB audio coexistence scenario.

### Formalized Code Direction

- FT-710 should use a dedicated CAT behavior branch rather than only debug-time diagnostics.
- FT-710 USB CAT now moves toward:
  - write-only CAT behavior
  - no `SerialInputOutputManager` background read loop
  - no inherited DX10 background polling for read-frequency / read-meter traffic

## End-of-Day Notes 2026-04-16

### What We Believe Now

- The FT-710 work has crossed an important boundary:
  - FT8CN no longer appears to be destroying the external music player session in the latest write-only CAT direction
  - FT8 audio can coexist with external music audio
- Because of that, the remaining FT-710 transmit problem should now be narrowed toward radio-side `DATA-U` behavior instead of general Android playback failure.

### Product Behavior Boundary

- Merely opening FT8CN should not pause or damage existing media playback.
- If FT8CN ever manages media focus, that should only happen during real TX and should ideally be optional.
- Therefore, historical behavior where CAT connect broke the player was treated as a bug, not an acceptable UX tradeoff.

### Planned Next Optimization Work

- strengthen FT-710-only TX debug traces
- reduce FT-710 TX flow to a minimal sequence
- keep FT-710 isolated from FTDX10 behavior
- postpone restoration of CAT readback / polling until RF output is stable
- recover source files whose Chinese comments were corrupted by a past commit, using git history as the source of truth

## Today Implementation 2026-04-16

### Code Changes Added

- stronger FT-710-only TX boundary logs in `MainViewModel`
- stronger FT-710 TX lifecycle logs in `FT8TransmitSignal`
- extra audio-route reports during FT-710 TX playback and cleanup
- FT-710-only `250ms` PTT tail hold after playback completion and before `PTT OFF`

### Intent Of This Round

- make the FT-710 transmit path easier to reason about from a single log capture
- keep using the FT-710 minimal USB TX direction:
  - CAT write-only
  - no read loop
  - no DX10 polling
  - local audio playback path
- reduce the chance that the final part of USB audio is being cut off too early by an early `PTT OFF`

## Code Hygiene Follow-up

- Some files contain Chinese comment corruption introduced by an earlier commit.
- Example already observed:
  - `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ft8transmit/FT8TransmitSignal.java`
  - corrupted fragments such as `?????????`
- This should be treated as a later cleanup task, not mixed into the current FT-710 TX debugging.
- Expected cleanup approach:
  - locate the last good version from git history
  - restore only comments / human-readable text
  - avoid changing active logic while doing the encoding cleanup

## Latest Test Reading 2026-04-16

### What The Log Now Confirms

- FT-710 CAT write-only mode stays active without breaking USB audio routing.
- During TX:
  - preferred output remains `USB_HEADSET`
  - `bind=true`
  - audio stream writes through essentially completely
- This is the strongest evidence so far that the Android-side playback chain is now working correctly.

### New Follow-up Change

- Added FT-710-only USB audio padding in `FT8TransmitSignal`:
  - `250ms` silent pre-roll
  - `250ms` silent post-roll
- Rationale:
  - if FT-710 `DATA-U` needs a stable USB stream before it starts accepting modulation, this is a better-targeted fix than only adjusting PTT timing

## Confirmed Working State 2026-04-16

### User Verification

- FT-710 now produces RF power in `DATA-U`.
- User verified that:
  - the previous build produced RF power in `DATA-U`
  - the newer build with USB audio padding also produces RF power in `DATA-U`

### Interpretation

- The decisive working change is currently believed to be the FT-710 USB/CAT architecture shift:
  - CAT write-only mode
  - no serial read loop
  - no DX10 polling inheritance during CAT USB operation
  - stable USB audio playback path
- The added USB padding may still be beneficial, but it is not yet isolated as the root fix.

### Practical Meaning

- The project has now crossed from “unable to modulate in `DATA-U`” into “`DATA-U` TX works”.
- Future work should prioritize:
  - preserving this working baseline
  - validating repeatability
  - cautiously trimming non-essential debug or workaround layers only after A/B confirmation

## Checkpoint 2026-04-17

### Safe Baseline Snapshot

- Current safe checkpoint commit:
  - `73ae96f Stabilize FT-710 USB TX path and debug cleanup`
- Branch:
  - `Feature/FT710Support`
- This commit is intended as the new rollback-safe baseline before touching CQ state-machine behavior.

### End-to-End Repair Chain

- The final effective repair direction was not a single parameter tweak.
- The working chain was built in this order:
  - split FT-710 out from the inherited FTDX10 behavior
  - default FT-710 control mode to `CAT`
  - stop FT-710 from inheriting DX10-style background CAT polling
  - disable FT-710 USB CAT background read loop and move to write-only CAT behavior
  - stabilize local USB audio playback toward the FT-710 composite USB audio device
  - pause recorder before USB TX and restore it after TX
  - add TX-side tracing and route snapshots so the whole boundary could be observed in one log
  - keep a small FT-710-only PTT tail hold so `PTT OFF` does not cut the final part of FT8 audio too early

### What Is Now Considered Core To The Fix

- The following items are now treated as core to the currently successful FT-710 path:
  - FT-710 dedicated rig branch
  - FT-710 CAT write-only behavior
  - no serial background read loop for FT-710 USB CAT
  - no inherited DX10 CAT polling during FT-710 USB use
  - stable local USB `AudioTrack` playback path
  - recorder pause/resume around FT-710 USB TX

### What Is Not Yet Proven As Root Cause

- The following items may still be helpful, but are not yet isolated as the decisive root fix:
  - FT-710 USB audio pre-roll / post-roll padding
  - FT-710-only PTT tail-hold duration tuning
  - detailed debug traces and route snapshots

### Timing Clarification

- There are now three separate timing concepts in the code and they should not be confused:
  - `transmitDelay`
    - launch offset inside each 15-second FT8 cycle
    - used to leave time for decode and scheduling
  - `pttDelay`
    - wait time after `PTT ON` and before sending FT8 audio
    - configured from the settings page
  - `FT710_TX_TAIL_HOLD_MS`
    - FT-710-only wait time after audio playback finishes and before `PTT OFF`
    - this is not the same as `pttDelay`

### CQ Logic Status

- A new behavioral concern was observed after the TX path became stable:
  - under some conditions the app may remain in activated state and continue calling `CQ`
  - this appears as:
    - target callsign stays `CQ`
    - function order stays `6`
    - TX continues with `CQ <MYCALL> <GRID>`
- Current judgment:
  - this is more likely a shared FT8 transmit state-machine behavior than an FT-710-only bug
  - the logic lives primarily in the common `FT8TransmitSignal` state machine
- Decision at this checkpoint:
  - do not change CQ auto-return behavior yet
  - first compare with the intended behavior on non-FT710 models

### Next Action Plan

- Freeze `73ae96f` as the current known-good baseline.
- Review shared CQ behavior against non-FT710 rigs before making any logic change.
- Focus the next round on:
  - whether “return to CQ after QSO” should also clear activation
  - whether manual `resetToCQ()` and automatic state-machine `resetToCQ()` should behave differently
  - whether current UI button state matches the real transmit activation state
- Until that comparison is complete:
  - avoid changing the FT-710 USB TX/audio chain
  - avoid removing the FT-710 write-only CAT architecture

## Repair Thinking Snapshot 2026-04-17

### Core Judgment

- At this stage, the FT-710 repair should be understood as a layered fix, not a single magic parameter.
- The strongest evidence points to USB coexistence as the real breakthrough:
  - once FT8CN stopped disturbing the FT-710 composite USB serial/audio coexistence, the TX path became stable enough for `DATA-U` to produce RF
- Because of that, sample rate, channel count, or one-off audio format tweaks should no longer be treated as the primary root cause.

### What Currently Looks Like The Real Fix Path

- FT-710 was split into its own rig branch instead of continuing to behave like a lightly modified FTDX10.
- FT-710 stopped inheriting the DX10 background CAT polling model.
- FT-710 USB CAT moved to a write-heavy / no background read-loop path.
- FT-710 TX preparation stopped aggressively forcing a mode rewrite every time.
- These items align best with the observed field behavior and should be treated as the protected baseline.

### What Looks More Like Supportive Improvement

- Recorder pause / resume around TX
- safer `AudioRecord` reinitialization
- route snapshots and richer debug traces
- media-focus handling during TX
- UI state/button refresh
- FT-710 default `CAT` selection
- These are still valuable, but they currently look more like stability and usability improvements than the root fix itself.

### What Is Still In The Gray Zone

- FT-710 pre-roll / post-roll padding
- FT-710 tail-hold duration
- `AudioRouteHelper.bindTrackToPreferredOutput(...)`
- exact USB `deviceId/productId` match strategy
- safer CDC ACM `claimInterface` fallback logic
- These may help, but they are not yet proven to be indispensable. If we optimize further, they should be tested one by one with A/B comparison.

### Practical Rule For Follow-up Work

- Keep the proven FT-710 minimal path frozen.
- Prefer subtraction over addition.
- Touch only one uncertain variable per round.
- Do not mix CQ behavior changes into FT-710 USB/TX work unless evidence shows they are directly linked.
- Keep all verbose debug output strictly gated by the debug switch.

## Subtraction Validation Checklist 2026-04-17

### Verification Snapshot 2026-04-19

- Verified normal after rollback:
  - `TX_AUDIO_FOCUS_SETTLE_MS = 0`
  - `FT710_USB_AUDIO_PREROLL_MS = 0`
  - `FT710_USB_AUDIO_POSTROLL_MS = 0`
  - `FT710_TX_TAIL_HOLD_MS = 0`
  - FT-710 path skips `bindTrackToPreferredOutput(...)`
  - rollback of exact `deviceId/productId` USB matching in `CableSerialPort`
  - rollback of the non-forced-first `claimInterfaceSafely(...)` strategy in `CdcAcmSerialDriver`
  - FT-710 local playback parameters rolled back to near-`main` behavior:
    - sample rate follows `GeneralVariables.audioSampleRate`
    - output bit depth follows `GeneralVariables.audioOutput32Bit`
    - `trackMode = MODE_STATIC`
    - mono `float2Short(...)` path
- Current interpretation:
  - the FT-710 working path does not currently appear to depend on the added timing compensations
  - the FT-710 working path also does not currently appear to depend on explicit `AudioTrack.setPreferredDevice(...)` binding
  - the FT-710 working path does not currently appear to depend on exact `deviceId/productId` matching
  - the FT-710 working path does not currently appear to depend on the safer non-forced-first CDC ACM claim strategy
  - the FT-710 working path also does not currently appear to depend on the previously added FT-710-specific local playback parameter bundle
- Remaining high-confidence core path after subtraction:
  - dedicated FT-710 rig branch
  - no inherited DX10 background CAT polling
  - FT-710 CAT write-only / no serial read loop
  - preserve current rig mode instead of forcing a mode rewrite
- Remaining work direction:
  - summarize confirmed non-core changes that can likely be cleaned up
  - re-check whether any FT-710-specific logic is still left in `FT8TransmitSignal` that is now effectively dead or redundant

### Live Handoff Snapshot

- Last user-confirmed normal build:
  - rollback of FT-710 local playback parameter bundle to near-`main` behavior
- Current build already field-verified:
  - `app-debug.apk`
  - build time: `2026-04-19 22:19:46`
- Current code state under test:
  - `FT710_TX_TAIL_HOLD_MS = 0`
  - `TX_AUDIO_FOCUS_SETTLE_MS = 0`
  - `FT710_USB_AUDIO_PREROLL_MS = 0`
  - `FT710_USB_AUDIO_POSTROLL_MS = 0`
  - sample rate follows `GeneralVariables.audioSampleRate`
  - output bit depth follows `GeneralVariables.audioOutput32Bit`
  - `trackMode = MODE_STATIC`
  - FT-710 local PCM path changed from stereo duplication back to mono `float2Short(...)`
  - FT-710 path still skips `bindTrackToPreferredOutput(...)`
  - `CableSerialPort` currently uses relaxed vendor-only matching
  - `CdcAcmSerialDriver` currently uses forced `claimInterface(..., true)`
- Immediate next expected action:
  - decide whether to stop subtraction and start cleanup / consolidation

### Priority 1

- `TX_AUDIO_FOCUS_SETTLE_MS`
  - Why test first:
    - compared with `main`, this is a newly introduced timing delay
    - although it is not FT-710-only, it is large enough to materially affect TX start timing
    - it should be validated before the FT-710-only timing compensations
  - Code:
    - [FT8TransmitSignal.java](/d:/Workshop/FT8CN/ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ft8transmit/FT8TransmitSignal.java#L47)
    - [FT8TransmitSignal.java](/d:/Workshop/FT8CN/ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ft8transmit/FT8TransmitSignal.java#L605)
  - A/B method:
    - keep everything else fixed and reduce the settle delay to `0`
  - Main observation:
    - whether media pause behavior remains acceptable
    - whether FT8 start timing becomes cleaner
    - whether FT-710 RF output remains stable

- FT-710 USB pre-roll
  - Why test second:
    - among the FT-710-only timing additions, this is the one most likely to shift the effective FT8 symbol start later inside the 15-second slot
  - Code:
    - [FT8TransmitSignal.java](/d:/Workshop/FT8CN/ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ft8transmit/FT8TransmitSignal.java#L48)
    - [FT8TransmitSignal.java](/d:/Workshop/FT8CN/ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ft8transmit/FT8TransmitSignal.java#L471)
  - A/B method:
    - remove pre-roll only, leave post-roll and the rest unchanged
  - Main observation:
    - whether `DATA-U` still produces stable RF
    - whether start-of-TX regresses to brief pulse / no power
    - whether slot timing discipline improves

- FT-710 USB post-roll
  - Why test third:
    - it does not move the FT8 symbol start, but it does extend occupied TX time and compress the return-to-RX margin
  - Code:
    - [FT8TransmitSignal.java](/d:/Workshop/FT8CN/ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ft8transmit/FT8TransmitSignal.java#L49)
    - [FT8TransmitSignal.java](/d:/Workshop/FT8CN/ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ft8transmit/FT8TransmitSignal.java#L472)
  - A/B method:
    - remove post-roll only, leave pre-roll and the rest unchanged
  - Main observation:
    - whether RX recovery margin improves
    - whether FT-710 still accepts modulation reliably

- FT-710 TX tail-hold
  - Why test fourth:
    - it is still a local timing compensation, but it occurs after audio playback and is less likely than pre-roll to disturb FT8 slot discipline
  - Code:
    - [FT8TransmitSignal.java](/d:/Workshop/FT8CN/ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ft8transmit/FT8TransmitSignal.java#L46)
    - [FT8TransmitSignal.java](/d:/Workshop/FT8CN/ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ft8transmit/FT8TransmitSignal.java#L133)
  - A/B method:
    - keep everything else fixed and reduce the FT-710 tail-hold to `0` or a much smaller value
  - Main observation:
    - whether the FT8 tail is cut off
    - whether RF output changes
    - whether TX ends too early

- Preferred output binding
  - Why test after timing items:
    - this reinforces audio routing, but the main breakthrough currently looks serial-side rather than route-binding-side
  - Code:
    - [AudioRouteHelper.java](/d:/Workshop/FT8CN/ft8cn/app/src/main/java/com/bg7yoz/ft8cn/audio/AudioRouteHelper.java#L41)
    - [FT8TransmitSignal.java](/d:/Workshop/FT8CN/ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ft8transmit/FT8TransmitSignal.java#L769)
  - A/B method:
    - disable only the FT-710 `bindTrackToPreferredOutput(...)` call
  - Main observation:
    - whether USB headset routing remains stable
    - whether RF output remains stable
    - whether media coexistence regresses

### Priority 2

- Exact `deviceId/productId` USB matching
  - Why test next:
    - this is strong defensive matching logic, but it may be more robustness than root cause
  - Code:
    - [CableSerialPort.java](/d:/Workshop/FT8CN/ft8cn/app/src/main/java/com/bg7yoz/ft8cn/connector/CableSerialPort.java#L56)
    - [CableSerialPort.java](/d:/Workshop/FT8CN/ft8cn/app/src/main/java/com/bg7yoz/ft8cn/connector/CableSerialPort.java#L107)
    - [CableSerialPort.java](/d:/Workshop/FT8CN/ft8cn/app/src/main/java/com/bg7yoz/ft8cn/connector/CableSerialPort.java#L110)
  - A/B method:
    - relax the extra exact-match preference while preserving FT-710 write-only CAT behavior
  - Main observation:
    - whether device selection becomes unstable
    - whether player breakage or CAT misbinding returns

- Safe CDC ACM `claimInterface` fallback
  - Why test after that:
    - this could still matter for composite USB coexistence, but it is more invasive than tail-hold or padding
  - Code:
    - [CdcAcmSerialDriver.java](/d:/Workshop/FT8CN/ft8cn/app/src/main/java/com/bg7yoz/ft8cn/serialport/CdcAcmSerialDriver.java#L108)
    - [CdcAcmSerialDriver.java](/d:/Workshop/FT8CN/ft8cn/app/src/main/java/com/bg7yoz/ft8cn/serialport/CdcAcmSerialDriver.java#L112)
    - [CdcAcmSerialDriver.java](/d:/Workshop/FT8CN/ft8cn/app/src/main/java/com/bg7yoz/ft8cn/serialport/CdcAcmSerialDriver.java#L117)
  - A/B method:
    - revert only the claim strategy, keep the FT-710 no-read-loop architecture in place
  - Main observation:
    - whether CAT connect again disturbs USB audio
    - whether headset icon or player behavior regresses

### Priority 3

- FT-710 local playback parameter tweaks
  - Why test later:
    - history already suggests isolated sample-format experimentation was not the decisive fix
  - Code:
    - [FT8TransmitSignal.java](/d:/Workshop/FT8CN/ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ft8transmit/FT8TransmitSignal.java)
  - A/B method:
    - only after the higher-priority items are settled, compare the FT-710 playback path with the common path
  - Main observation:
    - whether `AudioTrack.write(...)` regressions or `0` RF output return

### Do Not A-B First

- FT-710 dedicated rig branch
  - [YaesuFT710Rig.java](/d:/Workshop/FT8CN/ft8cn/app/src/main/java/com/bg7yoz/ft8cn/rigs/YaesuFT710Rig.java)
- no inherited DX10 background polling
  - [YaesuDX10Rig.java](/d:/Workshop/FT8CN/ft8cn/app/src/main/java/com/bg7yoz/ft8cn/rigs/YaesuDX10Rig.java#L29)
  - [YaesuFT710Rig.java](/d:/Workshop/FT8CN/ft8cn/app/src/main/java/com/bg7yoz/ft8cn/rigs/YaesuFT710Rig.java#L14)
- FT-710 CAT write-only / no serial read loop
  - [CableSerialPort.java](/d:/Workshop/FT8CN/ft8cn/app/src/main/java/com/bg7yoz/ft8cn/connector/CableSerialPort.java#L161)
  - [CableSerialPort.java](/d:/Workshop/FT8CN/ft8cn/app/src/main/java/com/bg7yoz/ft8cn/connector/CableSerialPort.java#L286)
- preserve mode instead of forcing mode rewrite
  - [YaesuFT710Rig.java](/d:/Workshop/FT8CN/ft8cn/app/src/main/java/com/bg7yoz/ft8cn/rigs/YaesuFT710Rig.java#L19)
- These are the current best candidates for the real repair chain and should stay frozen while we test the secondary items.

### Deferred Observation

- Low-priority follow-up after current A-B convergence:
  - review the user's subjective "timing still feels a bit off" observation against `main`
  - separate two questions clearly:
    - clock-sync / UTC offset behavior
    - TX/RX micro-timing feel during the FT-710 transmit path
  - current judgment:
    - the core `UtcTimer.syncTime(...)` path is unchanged versus `main`
    - the transmit execution path is not strictly identical to `main`
  - therefore this should be revisited only after the higher-priority A-B rollback items are settled
