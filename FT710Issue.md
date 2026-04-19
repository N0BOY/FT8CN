# FT-710 支持问题跟踪

## 背景

- 目标机型：Yaesu FT-710
- 连接方式：Android 手机 Type-C OTG -> USB 转接 -> FT-710 USB
- 目标工作模式：`DATA-U`
- CAT 波特率：`38400`
- 电台侧关键菜单：`Data Source=USB`

## 当前结论

- `PTT` 控制链路已经基本可用，应用能够把电台拉入发射。
- 蓝牙路由抖动、录音恢复失败、发射后解码失效等外围问题已有针对性修正。
- 目前核心未解问题仍然是：
  - 在 `DATA-U` 下多数情况下 `ALC=0`、`PO=0`
  - 电台有时会出现短脉冲或噪声脉冲，但没有持续功率输出

## 关键观察

### 1. 最重要线索

- 曾经出现过一次“程序错误切到 `RTTY-U`”的情况。
- 用户手动把模式从 `RTTY-U` 改回 `DATA-U` 后，电台出现过可听输出。
- 这说明问题很可能不只是 Android 音频格式，而与 **FT-710 的模式切换状态** 或 **`DATA-U` 进入方式** 强相关。

### 2. 现象变化

- 早期现象：起发瞬间出现一次类似噪声的脉冲，然后完全无功率。
- 中间改动后：变成两个脉冲，但仍然没有持续功率。
- 这类变化说明 Android 侧播放链路并非完全不工作，但并没有解决根因。

### 3. 当前判断

- `DATA-U` 下无持续功率，更像是：
  - 模式切换命令与 FT-710 的实际状态不完全匹配
  - 电台进入 `DATA-U` 后，数据音频入口状态没有被正确初始化
  - 或需要特定的过渡切换流程，不能只直接发送 `MD0C`

## 已落地改动

### FT-710 独立分支化

- 新增独立指令集：`InstructionSet.YAESU_FT710 = 23`
- 新增独立机型类：`YaesuFT710Rig`
- 在 `rigaddress.txt` 中加入 `YAESU FT-710`
- 配置页中将 FT-710 排在 FTDX10 前面
- 选择 FT-710 时默认控制方式切为 `CAT`

### 发射前后稳定性

- 发射期间暂停录音，发射结束后延时恢复
- 避免发射后 `AudioRecord startRecording called on an uninitialized AudioRecord`
- 限制蓝牙状态广播只在蓝牙连接模式下干预音频路由
- 降低蓝牙连接/断开反复弹消息的问题

### 调试能力

- 在配置页新增 `Debug` 开关
- 调试信息持久化到配置项 `debugMode`
- FT-710 相关关键日志接入应用内 Debug 面板：
  - 模式切换
  - PTT 开关
  - 录音暂停/恢复
  - 音频路由
  - 发射音频生成与播放完成

### 当前仍在保留验证的逻辑

- `YaesuFT710Rig.setUsbModeToRig()` 目前采用：
  - 先切 `RTTY-U`
  - 短延时后再切回 `DATA-U`
- 这是依据“手动从 `RTTY-U` 切回 `DATA-U` 后曾出现输出”这条关键线索保留的待验证逻辑。

## 已验证无效的尝试

以下尝试已经验证“不解决 `DATA-U` 下 0 功率”问题，现已标记为无效；其中纯音频兼容路径已从代码中回收。

### [无效] 猜测型 FT-710 菜单 EX 命令

- 曾尝试加入类似 `EX0104141;` 的 FT-710 菜单设置命令。
- 用户验证无效，且命令本身缺乏可靠手册依据。
- 结论：不保留。

### [无效] FT-710 专属音频格式强制兼容

- 曾尝试过：
  - `48kHz / stereo / stream`
  - `44.1kHz / mono / 16bit / stream`
  - `44.1kHz / mono / 16bit / static preload`
  - 满音量输出
- 用户验证后结论：`0` 功率问题没有本质变化。
- 结论：这部分不能作为主方向，已从 `FT8TransmitSignal` 的 FT-710 专属分支中回收。

### [无效] 仅直接 CAT 切到 `DATA-U`

- 仅使用 `MD0C` 直接切 `DATA-U`，长期表现为：
  - PTT 能拉起
  - 但 `ALC=0`、`PO=0`
- 结论：直接切 `DATA-U` 不足以解决问题。

## 当前保留的工作假设

### 假设 A：模式切换序列问题

- FT-710 对 `DATA-U` 的进入流程可能有额外状态要求。
- “`RTTY-U -> DATA-U` 后手动恢复有输出”的线索，优先支持这个方向。

### 假设 B：电台 `DATA-U` 音频入口状态问题

- 即使前面板显示为 `DATA-U`，也可能还有内部数据调制入口没有被 CAT 正确带起。
- 需要结合更多调试信息继续确认。

## 后续建议

### 短期

- 使用最新带 Debug 开关的 APK 继续复现。
- 抓取应用内 Debug 面板内容，重点看：
  - `mode switch request / settled`
  - `PTT ON / PTT OFF`
  - `playFT8Signal`
  - `generated samples`
  - `route Before TX`
  - `finishPlaybackOnce`

### 中期

- 若 `RTTY-U -> DATA-U` 仍不能恢复功率，需要继续核对：
  - FT-710 CAT 手册中模式查询/返回值
  - `DATA-U` 相关菜单项是否存在可读写 CAT 命令
  - 电台端 `DATA MOD SELECT` / `USB AUDIO` / `DATA PTT SELECT` 是否有额外状态

## 当前状态

- 状态：`进行中`
- 优先级：`高`
- 根问题：`FT-710 在 DATA-U 下仍无持续发射功率`
## Latest Findings 2026-04-15

- Disabled FT-710 auto mode switch: no change.
- Released `AudioRecord` before TX: no change.
- FT-710 TX path already tested with `48kHz / stereo / 16bit / MODE_STREAM`: still no RF output.

### Hard Evidence

- Debug log now shows:
  - `partial write expected=1213456, actual=0, trackMode=1`
- This means the current direct failure point is:
  - `AudioTrack.write(...)` in stream mode returns `0`
  - TX audio is not actually entering the playback pipeline

### Player Comparison

- User verified:
  - Before FT8CN connects, `DATA-U + manual PTT + music player` can produce audio on FT-710
  - After FT8CN connects to the rig, music player output disappears
  - Killing FT8CN is not enough to restore playback immediately
  - Restarting the music player app restores audio
- This strongly suggests FT8CN is disturbing the Android USB audio playback session after connecting to the FT-710 composite USB device.

### Current Priorities

- Investigate why `AudioTrack.write(...)` returns `0` in the FT-710 stream path
- Investigate whether serial connection to the FT-710 composite USB device disrupts the existing Android USB audio session
- Keep the above as the top working hypotheses for the next debugging round

## Serial-Link Hypothesis Update 2026-04-15

- The field symptom "music player loses audio immediately after FT8CN connects CAT" is now treated as a first-class clue, not a side effect.
- This points more strongly to FT8CN disturbing the FT-710 composite USB device session, instead of only generating bad FT8 audio.
- New code changes in this round:
  - `CableSerialPort` now prefers exact `deviceId/productId` matching instead of only `vendorId`
  - `CableSerialPort` now logs the selected USB device and all interface classes when connecting
  - `CableSerialPort` now records audio route snapshots before and after serial open
  - `CdcAcmSerialDriver` now tries `claimInterface(..., false)` first, and only falls back to forced claim if needed
- Purpose of this round:
  - reduce the chance that FT8CN forcibly disturbs FT-710 USB audio while opening CAT
  - collect evidence on whether serial open itself changes Android USB audio routing

## Additional Log Clue 2026-04-16

- In the previously captured debug log, there is at least one TX attempt that starts normally and then drops about 1 second later.
- Example timing from the screenshots:
  - `15:46:15` TX start / `playFT8Signal`
  - `15:46:16` `finishPlaybackOnce` / `afterPlayAudio release track` / `PTT OFF`
- This is much shorter than a normal FT8 transmit window and should be treated as a key clue.
- Interpretation:
  - at least one failure mode is "TX playback chain ends early", not merely "audio keeps playing but RF stays at 0"
  - this strongly supports investigating premature playback completion, early `PTT OFF`, or early USB audio session teardown

## USB Coexistence Breakthrough 2026-04-16

- User verified a diagnostic build where FT-710 CAT still opens, but the serial background read loop is not started.
- In that build:
  - the Android music player is no longer broken after FT8CN connects
  - FT8 playback audio can be heard together with the music player audio
  - this is the first change that clearly improves the USB audio coexistence symptom
- Interpretation:
  - the main interference source is very likely the FT-710 CAT serial background read loop
  - `AudioTrack` alone is not sufficient to explain the playback breakage
  - `AudioRecord` alone is also not sufficient to explain the playback breakage
- Formalized direction for the next build:
  - FT-710 should use a dedicated CAT branch
  - disable serial background read loop for FT-710 over USB CAT
  - disable inherited DX10 background CAT polling for FT-710

## Current Thinking Snapshot 2026-04-16

- The USB coexistence issue and the `0` RF output issue should now be treated as two separate layers:
  - layer 1: FT8CN must not break the Android music player or USB audio session after CAT connect
  - layer 2: after layer 1 is stabilized, investigate why FT-710 still may not produce RF in `DATA-U`
- Current evidence says layer 1 has improved substantially after disabling the FT-710 CAT read loop.
- If a new build still has `0` RF while external audio playback remains healthy, the next suspect is no longer Android playback destruction, but FT-710 `DATA-U` transmit conditions on the radio side.

## Product Boundary Note 2026-04-16

- Opening FT8CN should not proactively pause or break an existing external music player.
- Connecting FT8CN to FT-710 CAT should also not leave the player in a broken state that requires restarting the player app.
- A future optional behavior may be acceptable:
  - request audio focus only during actual TX
  - optionally pause or duck external media only during TX
- That future media-focus behavior is a product choice, not the root cause of the current FT-710 bug.

## Next Work Items

- Add a cleaner FT-710 TX trace focused on:
  - `PTT ON/OFF`
  - TX playback start/end timing
  - route snapshots around TX
  - FT-710 CAT write-only mode state
- Create a minimal FT-710 TX path:
  - no mode switching
  - no CAT readback
  - no meter polling
  - only `PTT ON -> play audio -> PTT OFF`
- Once the minimal TX path is verified stable, continue investigating FT-710 `DATA-U` side conditions:
  - `DATA MOD SELECT`
  - `USB AUDIO`
  - `DATA PTT SELECT`
  - any radio-side prerequisite that differs from `RTTY-U`

## Implementation Update 2026-04-16 (Today)

- Added clearer FT-710 TX lifecycle tracing in the actual transmit path:
  - TX lifecycle begin
  - playback start
  - playback finished
  - cleanup complete
- Added FT-710 boundary logs around:
  - `before-transmit-entry`
  - `After PTT ON`
  - `after-transmit-entry`
  - `After PTT OFF`
- Added extra FT-710 route snapshots around TX playback:
  - `FT710 TX stream started`
  - `FT710 before PTT tail hold`
  - `FT710 after TX cleanup`
- Added a small FT-710-only PTT tail hold after playback completes and before `PTT OFF`:
  - current value: `250ms`
  - purpose: reduce the chance that the last part of TX audio is cut off too early

## What To Look For In The Next Field Test

- Whether FT-710 still keeps the external player healthy after CAT connect
- Whether `DATA-U` still has `0` RF output even when FT8 playback clearly starts
- Whether the new logs show:
  - TX playback lasting normally
  - `After PTT ON` route still on USB headset
  - `After PTT OFF` route remaining stable
  - any clear transition point where `DATA-U` stops accepting modulation

## Source Cleanup Note

- Some source files were found to contain garbled Chinese comments introduced by an earlier commit.
- Known example:
  - `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ft8transmit/FT8TransmitSignal.java`
  - visible corrupted text like `?????????`
- This is not considered the current FT-710 root cause, but it should be repaired later from git history so the original Chinese comments can be restored accurately.

## Log Interpretation 2026-04-16 (Latest Test)

- Current screenshots show a much healthier Android-side TX path:
  - USB route remains `USB_HEADSET` before serial open, after serial open, and at TX start
  - track binding reports `bind=true`
  - FT8 stream write completes almost fully:
    - `writtenFrames=606728`
    - `actualDurationMs=12640`
    - `elapsedMs=12002`
    - `remainingMs=638`
- Interpretation:
  - Android audio routing is no longer the primary failure point
  - FT8CN is now successfully pushing a full TX audio stream toward the USB audio path
  - the remaining `0` RF problem is increasingly likely to be on the FT-710 `DATA-U` acceptance side

## Targeted Change After Latest Test

- Added FT-710-only USB audio padding around the actual FT8 tones:
  - `250ms` silent pre-roll
  - `250ms` silent post-roll
- Purpose:
  - let FT-710 see a stable USB audio stream before the FT8 tones begin
  - reduce the chance that `DATA-U` ignores modulation because the stream starts too abruptly

## Confirmed Success 2026-04-16

- User confirmed that `DATA-U` now produces RF power output.
- Important nuance:
  - the previous build already produced RF output in `DATA-U`
  - this newer build with USB audio padding also produces RF output
- Therefore the currently proven effective baseline is more likely:
  - FT-710 CAT write-only mode
  - no serial background read loop
  - no inherited DX10 CAT polling
  - stable local USB audio playback path
- The newly added `250ms` pre-roll / `250ms` post-roll padding is not yet proven to be the decisive fix by itself.

## Latest Log Reading (Successful TX)

- The successful log still shows:
  - `bind=true`
  - USB route stays on `USB_HEADSET`
  - stream write completes almost fully
  - full TX duration is now slightly longer because padding is included
- This supports the conclusion that the Android-side TX chain is now healthy enough for FT-710 `DATA-U` to accept modulation.

## Next Recommended Work

- Freeze the current FT-710 working branch as the new known-good baseline.
- Do one careful A/B cleanup pass later:
  - keep the FT-710 minimal USB TX architecture
  - evaluate whether USB padding is actually needed
  - remove only changes that are confirmed unnecessary
- After that, shift focus from “can it transmit” to:
  - stability across repeated TX/RX cycles
  - receive recovery after TX
  - whether debug-only traces should become optional or be reduced

## Latest Checkpoint 2026-04-17

### Safe Commit Baseline

- Safe checkpoint commit created:
  - `73ae96f Stabilize FT-710 USB TX path and debug cleanup`
- Branch:
  - `Feature/FT710Support`
- Purpose:
  - preserve the currently working FT-710 USB TX path before touching CQ state-machine behavior

### Updated Current Conclusion

- FT-710 `DATA-U` transmit is now considered basically repaired at the USB/CAT/audio boundary level.
- The most credible working repair chain is:
  - FT-710 dedicated rig branch
  - FT-710 CAT write-only mode
  - no USB serial background read loop
  - no inherited DX10 polling behavior
  - stable local USB audio playback path
  - recorder pause/resume around TX
- Remaining concern has shifted from “no RF output” to “post-QSO or CQ activation behavior may not match expectation”.

### Important Timing Distinction

- The FT-710 TX tail-hold is not the same thing as the user-configurable `PTT delay`.
- Current timing concepts:
  - `transmitDelay`
    - cycle scheduling offset
  - `pttDelay`
    - wait after `PTT ON` before audio starts
  - FT-710 TX tail-hold
    - wait after audio ends before `PTT OFF`
- Therefore:
  - adjusting settings-page `PTT delay` does not change the FT-710 TX tail-hold behavior

### New Behavior Risk To Evaluate

- A new observation needs careful comparison with non-FT710 behavior:
  - after the TX path became stable, the app may stay in activated `CQ` mode and continue transmitting `CQ`
  - user screenshot characteristics:
    - target callsign is `CQ`
    - function order is `6`
    - TX message is `CQ <MYCALL> <GRID>`
- Current interpretation:
  - this does not yet prove an FT-710-only bug
  - this may be inherited shared behavior from the common transmit state machine
  - the main logic is in `FT8TransmitSignal.parseMessageToFunction(...)` and `resetToCQ()`

### Decision For The Next Round

- Do not modify CQ auto-return logic blindly in the FT-710 branch.
- First compare the current CQ/state-machine behavior with non-FT710 rigs.
- Only after that comparison decide whether to:
  - keep current behavior
  - clear activation when the state machine automatically returns to `CQ`
  - separate manual `resetToCQ()` behavior from automatic state-machine `resetToCQ()` behavior

### Immediate Next Actions

- Treat `73ae96f` as the new rollback-safe baseline.
- Review shared CQ behavior in common transmit logic before making any functional change.
- Keep the FT-710 USB TX path frozen while evaluating CQ/state behavior.

## Current Repair Thinking 2026-04-17

### Reclassification Of Existing Changes

- After comparing the working branch against `main`, the FT-710 changes should now be split into three buckets:
  - likely true fixes
  - usability / stability improvements
  - uncertain or optional changes
- This matters because the branch already contains many experiments, and future cleanup should protect the real fix path first.

### Likely True Fixes

- Add a dedicated FT-710 rig branch:
  - `InstructionSet.YAESU_FT710`
  - `YaesuFT710Rig`
  - FT-710 entry in `rigaddress.txt`
- Stop FT-710 from inheriting DX10 background CAT polling behavior.
- Stop the FT-710 USB CAT background read loop and keep CAT on a write-heavy path.
- Stop aggressively forcing rig mode rewrites during FT-710 USB TX preparation.
- These are the changes that best match the observed turning point:
  - FT8CN no longer breaks the external music player after CAT connect
  - `DATA-U` TX becomes able to produce RF

### Usability / Stability Improvements

- Pause recorder around TX and resume after TX
- `MicRecorder` reinitialization hardening
- richer route / TX lifecycle debug logging
- debug switch gating
- FT-710 default `CAT` selection in config
- TX/RX button state refresh work
- These are useful and should likely remain, but they do not currently look like the decisive root fix.

### Uncertain / Optional Changes

- FT-710 USB pre-roll / post-roll padding
- FT-710 tail-hold timing
- `AudioRouteHelper.bindTrackToPreferredOutput(...)`
- exact USB `deviceId/productId` matching
- safer `claimInterface` fallback in `CdcAcmSerialDriver`
- FT-710 local playback parameter tweaks in `FT8TransmitSignal`
- These changes should be treated as A/B candidates rather than assumed necessities.

### Working Principle For The Next Round

- Freeze the currently working FT-710 minimal path.
- Prefer subtraction over new additions.
- Change only one uncertain variable at a time.
- Keep CQ behavior review separate unless a direct FT-710 coupling is proven.

## Subtraction Validation Checklist 2026-04-17

### Verification Snapshot 2026-04-19

- Already verified as normal after rollback:
  - `TX_AUDIO_FOCUS_SETTLE_MS = 0`
  - `FT710_USB_AUDIO_PREROLL_MS = 0`
  - `FT710_USB_AUDIO_POSTROLL_MS = 0`
  - `FT710_TX_TAIL_HOLD_MS = 0`
  - FT-710 transmit path without `bindTrackToPreferredOutput(...)`
  - rollback of exact `deviceId/productId` matching in `CableSerialPort`
  - rollback of the non-forced-first `claimInterfaceSafely(...)` strategy in `CdcAcmSerialDriver`
  - FT-710 local playback parameters rolled back to near-`main` behavior:
    - sample rate follows `GeneralVariables.audioSampleRate`
    - output bit depth follows `GeneralVariables.audioOutput32Bit`
    - `trackMode = MODE_STATIC`
    - mono `float2Short(...)` path
- Meaning of the current results:
  - the FT-710 repair does not currently look dependent on the added timing compensations
  - the FT-710 repair also does not currently look dependent on explicit preferred-output binding
  - the FT-710 repair also does not currently look dependent on exact `deviceId/productId` matching
  - the FT-710 repair also does not currently look dependent on the safer non-forced-first CDC ACM claim strategy
  - the FT-710 repair also does not currently look dependent on the previously added FT-710-specific local playback parameter bundle
- Remaining likely core path after subtraction:
  - dedicated FT-710 rig branch
  - no inherited DX10 background polling
  - FT-710 CAT write-only / no serial read loop
  - preserve radio mode instead of forcing rewrite
- Next queued A-B item if subtraction continues:
  - only the remaining high-confidence core path is left, so any further subtraction is now high-risk

### Live Handoff Snapshot

- Last confirmed normal result:
  - rollback of FT-710 local playback parameter bundle to near-`main` behavior
- Current APK already verified in field test:
  - `app-debug.apk`
  - build timestamp: `2026-04-19 22:19:46`
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
  - `CableSerialPort` is currently on relaxed vendor-only matching
  - `CdcAcmSerialDriver` is currently on forced `claimInterface(..., true)`
- Next expected step:
  - decide whether to stop subtraction and start cleanup / consolidation

### Highest Priority A-B Rollback Items

- `TX_AUDIO_FOCUS_SETTLE_MS`
  - code:
    - [FT8TransmitSignal.java](/d:/Workshop/FT8CN/ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ft8transmit/FT8TransmitSignal.java#L47)
    - [FT8TransmitSignal.java](/d:/Workshop/FT8CN/ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ft8transmit/FT8TransmitSignal.java#L605)
  - reason:
    - compared with `main`, this is a newly introduced timing delay and should be validated before FT-710-specific timing compensations

- FT-710 USB pre-roll
  - code:
    - [FT8TransmitSignal.java](/d:/Workshop/FT8CN/ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ft8transmit/FT8TransmitSignal.java#L48)
    - [FT8TransmitSignal.java](/d:/Workshop/FT8CN/ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ft8transmit/FT8TransmitSignal.java#L471)
  - reason:
    - among FT-710-only timing additions, this is the one most likely to push the effective FT8 symbol start later inside the 15-second slot

- FT-710 USB post-roll
  - code:
    - [FT8TransmitSignal.java](/d:/Workshop/FT8CN/ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ft8transmit/FT8TransmitSignal.java#L49)
    - [FT8TransmitSignal.java](/d:/Workshop/FT8CN/ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ft8transmit/FT8TransmitSignal.java#L472)
  - reason:
    - it extends occupied TX time and compresses the return-to-RX margin, but is less risky to symbol start timing than pre-roll

- FT-710 TX tail-hold
  - code:
    - [FT8TransmitSignal.java](/d:/Workshop/FT8CN/ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ft8transmit/FT8TransmitSignal.java#L46)
    - [FT8TransmitSignal.java](/d:/Workshop/FT8CN/ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ft8transmit/FT8TransmitSignal.java#L133)
  - reason:
    - still worth A-B rollback, but it happens after audio playback and is less likely than pre-roll to disturb FT8 slot discipline

- `AudioRouteHelper.bindTrackToPreferredOutput(...)`
  - code:
    - [AudioRouteHelper.java](/d:/Workshop/FT8CN/ft8cn/app/src/main/java/com/bg7yoz/ft8cn/audio/AudioRouteHelper.java#L41)
    - [FT8TransmitSignal.java](/d:/Workshop/FT8CN/ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ft8transmit/FT8TransmitSignal.java#L769)
  - reason:
    - useful routing reinforcement, but the main repair turning point looks serial-side

### Second-Tier A-B Rollback Items

- exact `deviceId/productId` matching
  - code:
    - [CableSerialPort.java](/d:/Workshop/FT8CN/ft8cn/app/src/main/java/com/bg7yoz/ft8cn/connector/CableSerialPort.java#L56)
    - [CableSerialPort.java](/d:/Workshop/FT8CN/ft8cn/app/src/main/java/com/bg7yoz/ft8cn/connector/CableSerialPort.java#L107)
    - [CableSerialPort.java](/d:/Workshop/FT8CN/ft8cn/app/src/main/java/com/bg7yoz/ft8cn/connector/CableSerialPort.java#L110)
  - reason:
    - strong robustness logic, but not yet proven as a decisive repair

- safer `claimInterface` fallback
  - code:
    - [CdcAcmSerialDriver.java](/d:/Workshop/FT8CN/ft8cn/app/src/main/java/com/bg7yoz/ft8cn/serialport/CdcAcmSerialDriver.java#L108)
  - reason:
    - may matter for composite USB coexistence, but should be tested after the lower-risk items

### Later Candidate

- FT-710 local playback parameter tweaks
  - code:
    - [FT8TransmitSignal.java](/d:/Workshop/FT8CN/ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ft8transmit/FT8TransmitSignal.java)
  - reason:
    - historical evidence says format-only changes were mostly ineffective

### Do Not Roll Back First

- FT-710 dedicated rig branch
  - [YaesuFT710Rig.java](/d:/Workshop/FT8CN/ft8cn/app/src/main/java/com/bg7yoz/ft8cn/rigs/YaesuFT710Rig.java)
- disable inherited DX10 polling
  - [YaesuDX10Rig.java](/d:/Workshop/FT8CN/ft8cn/app/src/main/java/com/bg7yoz/ft8cn/rigs/YaesuDX10Rig.java#L29)
  - [YaesuFT710Rig.java](/d:/Workshop/FT8CN/ft8cn/app/src/main/java/com/bg7yoz/ft8cn/rigs/YaesuFT710Rig.java#L14)
- FT-710 CAT write-only / no serial read loop
  - [CableSerialPort.java](/d:/Workshop/FT8CN/ft8cn/app/src/main/java/com/bg7yoz/ft8cn/connector/CableSerialPort.java#L161)
  - [CableSerialPort.java](/d:/Workshop/FT8CN/ft8cn/app/src/main/java/com/bg7yoz/ft8cn/connector/CableSerialPort.java#L286)
- preserve radio mode instead of forcing rewrite
  - [YaesuFT710Rig.java](/d:/Workshop/FT8CN/ft8cn/app/src/main/java/com/bg7yoz/ft8cn/rigs/YaesuFT710Rig.java#L19)
- These are currently the strongest candidates for the real FT-710 repair chain.

### Deferred At The End Of Current A-B Work

- Revisit the user's observation that recent builds still "feel a bit off" in timing.
- Scope of that later review:
  - compare subjective TX/RX timing feel against `main`
  - separately verify whether clock-sync / UTC offset behavior is actually different from `main`
- Current interim judgment:
  - `UtcTimer.syncTime(...)` itself is unchanged versus `main`
  - the transmit execution path is not strictly identical to `main`
- Priority:
  - keep this item at the tail of the current to-do list
  - do not interrupt the ongoing A-B rollback convergence for it
