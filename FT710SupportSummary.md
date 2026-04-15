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
- 新增 `FT710Rig`
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
- `ft8cn/app/src/main/java/com/bg7yoz/ft8cn/rigs/FT710Rig.java`
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
