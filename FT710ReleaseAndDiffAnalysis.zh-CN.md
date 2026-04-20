# FT-710 发布记录与最终差异分析

## 一、版本快照

- 日期：2026-04-20
- 分支：`Feature/FT710Support`
- 本地提交：`0727ada`
- 提交说明：`Stabilize validated FT-710 support set`
- 远端分支：
  - `origin/Feature/FT710Support`
  - PR 入口：
    - `https://github.com/LeoLiXX/FT8CN/pull/new/Feature/FT710Support`

## 二、APK 构建产物

- Debug APK
  - 路径：`ft8cn/app/build/outputs/apk/debug/app-debug.apk`
  - 大小：`22,350,536`
  - 构建时间：`2026-04-20 23:03:11`

- Release APK
  - 路径：`ft8cn/app/build/outputs/apk/release/app-release.apk`
  - 大小：`20,368,214`
  - 构建时间：`2026-04-20 23:05:15`

## 三、Release 构建说明

- `assembleRelease` 已成功通过。
- 构建过程中出现了大量多语言 `strings.xml` 的格式化警告。
- 这些警告是仓库原有问题，不是本次 FT-710 修复引入的。
- 这些警告没有阻止 release APK 生成。

## 四、相对 main 分支的最终代码差异

最终相对 `main` 保留下来的代码改动，主要只有下面几类。

### 1. FT-710 机型识别

- `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/rigs/InstructionSet.java`
  - 增加 `InstructionSet.YAESU_FT710 = 23`

- `ft8cn/app/src/main/assets/rigaddress.txt`
  - 增加 `YAESU FT-710,00,38400,23`

这部分属于“机型入口定义”，是基础必需项，没有争议。

### 2. FT-710 复用 DX10 CAT 指令集

- `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/MainViewModel.java`
  - 增加：
    - `case InstructionSet.YAESU_FT710:`
    - `baseRig = new YaesuDX10Rig();`

它的含义很明确：

- FT-710 当前复用 DX10 的 CAT 命令行为
- FT-710 的特殊修复不放在 rig 指令层
- 而是放在更贴近 USB 串口共存问题的 serial path

这也是比较合理的设计，因为我们最后确认的问题并不像是“CAT 命令语义错了”，而更像是“USB CAT 与 USB 音频共存出了问题”。

### 3. FT-710 默认控制方式强制为 CAT

- `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/ui/ConfigFragment.java`

当用户把机型切到 FT-710 时，如果当前控制方式不是 `CAT`，就自动：

- 设置 `GeneralVariables.controlMode = ControlMode.CAT`
- 持久化 `ctrMode`
- 调用 `mainViewModel.setControlMode()`
- 刷新 `setControlMode()` 和 `setConnectMode()`

这部分不是“根因修复”，但它是一个应当保留的易用性修正：

- 避免每次切到 FT-710 都要手动重新选 CAT
- 减少错误配置带来的误判

### 4. FT-710 核心串口修复

- `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/connector/CableSerialPort.java`

新增：

- `shouldUseFt710WriteOnlyCatMode()`

并在打开串口后：

- 对 FT-710 + USB 线 + CAT 控制方式的组合
- 不再启动 `usbIoManager.start()`

也就是：

- FT-710 的 USB CAT 变成只写不读
- 后台串口读循环被禁用

这部分是整个修复链条里，证据最强、最接近根因的一项。

### 5. 蓝牙广播接收器收敛

- `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/bluetooth/BluetoothStateBroadcastReceive.java`

恢复了：

- `shouldHandleBluetoothAudioRouting()`

把蓝牙音频路由相关处理限制在：

- 当前连接模式确实是 `BLUE_TOOTH`

这部分不是 FT-710 USB 问题的主修复，但它是一个合理的防护：

- 避免 App 在并非蓝牙连接模式下，仍然响应蓝牙音频广播并误操作状态

### 6. CDC ACM 接口 claim 安全包装

- `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/serialport/CdcAcmSerialDriver.java`

增加了：

- `claimInterfaceSafely(...)`

它的作用主要是：

- 做空指针保护
- 统一 force claim 失败路径
- 增加一些串口接口 claim 的日志

从最终证据来看，这部分更像“防御性增强”，不是 FT-710 根因修复本体。

### 7. MicRecorder 健壮性增强

- `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/wave/MicRecorder.java`

主要变化包括：

- 延迟创建 `AudioRecord`
- 如果 recorder 非法，则重建
- `startRecording()` 失败时重试一次
- `stopRecord()` 时主动释放 recorder

这部分更像：

- 录音链路健壮性修复
- 防止 `AudioRecord startRecording called on an uninitialized AudioRecord`

但它和 FT-710 `DATA-U` 无功率这个核心问题的关联度不高。

### 8. 仓库卫生

- `.gitignore`
  - 补充了 Gradle、Android、本地构建目录、IDE 配置和 APK 文件忽略规则

这部分只属于仓库整理，不属于 bug 修复本体。

## 五、对整个 bug 原因的最终判断

### 1. 最像根因的，不是 FT8 音频波形本身

从整个排查过程看，问题并不像一开始直觉认为的那样，是：

- FT8 音频采样率错了
- 声道格式错了
- `DATA-U` 模式字符串不对
- 或者简单的 PTT 时序问题

因为这些方向大多都测过，而且没有形成稳定有效的改善。

### 2. 最关键的现场线索，是“CAT 一连上，连音乐播放器都坏了”

这个现象非常关键：

- 在 FT8CN 没连接串口前
  - `DATA-U + 手动 PTT + 音乐播放器`
  - 可以正常从 FT-710 出声

- 但 FT8CN 一旦连上 CAT
  - 音乐播放器本身的声音就可能消失
  - 杀掉 FT8CN 也不一定马上恢复
  - 往往要重启播放器 App 才恢复

这说明问题很可能不是：

- “FT8CN 生成的 FT8 音频不对”

而是：

- “FT8CN 在连接 FT-710 的复合 USB 设备后，干扰了 Android 的 USB 音频会话”

### 3. 为什么最后怀疑点集中到串口后台读循环

真正的突破来自 A-B 实验：

- 保留 FT-710 CAT 连接
- 但禁用 FT-710 的串口后台读循环

结果是：

- 外部音乐播放器不再被 FT8CN 破坏
- FT8 音频和音乐甚至可以同时被听到
- FT-710 在 `DATA-U` 下的发射行为恢复到可验证正常

这是整个排查过程中证据最强的一组现象。

因此最终最合理的解释是：

- FT-710 的 USB 复合设备场景下
- Android 对 USB CAT 串口和 USB 音频的并行占用比较敏感
- 继承自 DX10 的后台串口读循环/轮询行为过于激进
- 这个行为破坏了 USB 音频会话的稳定性

也就是说，真正的问题更像：

- “USB 串口读循环干扰了 USB 音频”

而不是：

- “FT8 发射音频本身生成错误”

## 六、最终修复策略为什么成立

最终保留下来的修复策略可以概括成：

1. 增加 FT-710 机型入口
2. 继续复用 DX10 的 CAT 指令集
3. 在配置上默认切到 CAT
4. 把 FT-710 的真正特殊处理放到 serial path
5. 对 FT-710 USB CAT 禁用后台读循环，只保留写路径

这套策略的优点在于：

- 对原有代码侵入小
- 没有为了 FT-710 去重做整套 Yaesu CAT 指令
- 把特殊性限制在真正出问题的层面，也就是 USB 串口和 USB 音频共存层

它比之前那些尝试更可靠，因为它不是“拍脑袋调参数”，而是由现场 A-B 结果支撑出来的。

## 七、哪些看起来是核心修复，哪些不是

### 高置信核心项

- `InstructionSet.YAESU_FT710`
- `rigaddress.txt` 的 FT-710 条目
- `MainViewModel` 中 FT-710 -> `YaesuDX10Rig`
- `ConfigFragment` 中 FT-710 默认 CAT
- `CableSerialPort` 中 FT-710 USB CAT write-only / no read loop

### 中等置信、偏辅助

- `BluetoothStateBroadcastReceive`
  - 这是合理的逻辑护栏
  - 但不是 FT-710 USB 问题的核心修复

- `CdcAcmSerialDriver`
  - 这是串口层的防御性增强
  - 但不是目前证据最强的根因修复点

### 低置信、不是 FT-710 核心修复

- `MicRecorder`
  - 更像录音稳定性补丁
  - 和 FT-710 `DATA-U` 发射恢复正常之间，没有形成强证据链

- `.gitignore`
  - 纯仓库卫生

## 八、为什么现在这版已经比较“收敛”

相比前面调试期的大分支，现在最终保留下来的版本已经去掉了很多东西：

- 大量 debug UI
- 路由展示与调试开关
- 各种音频 padding / pre-roll / post-roll
- 明显偏实验性质的时序补偿
- 绑定首选输出设备的尝试
- 精确 USB `deviceId/productId` 匹配
- 较重的 FT-710 专属 rig 子类分叉
- 各类模式强制改写实验

这很重要，因为它意味着：

- 最后留下来的东西，不是“大量试错后的噪音”
- 而是真正经过多轮减法之后，还无法轻易删掉的那部分

## 九、仍然存在的不确定性

目前仍然有两部分，虽然保留了，但它们和主问题之间的关联度没有那么高：

### 1. `CdcAcmSerialDriver`

它更像：

- 串口 claim 过程的防御性包装

不是：

- FT-710 主修复本体

### 2. `MicRecorder`

它更像：

- 录音异常时的健壮性增强

不是：

- FT-710 `DATA-U` 无功率的直接修复

如果未来还要继续向 `main` 靠拢，这两项是比较合理的下一轮 A-B 候选。

## 十、长期建议

- 保留 FT-710 在 `CableSerialPort` 上的 write-only CAT 特判，除非以后有新的反证。
- 把 FT-710 视为“DX10 CAT 指令兼容，但 USB 共存特性不同”的机型。
- 不要轻易给 FT-710 重新加回后台 CAT 读循环，除非做明确的 A-B 验证。
- 后续记录尽量继续使用新的 UTF-8 Markdown 文件，不要把新结论继续混入已有乱码文档。
