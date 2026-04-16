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
