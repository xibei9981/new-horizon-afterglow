# 余烬航线 0.4.0 安卓兼容包

适用于安卓 Mindustry 160.4；内含完整 New Horizon 和 13 张续篇战役地图。
同时保留桌面字节码，电脑也能使用。无需再装一个 New Horizon。

1. 在 Mindustry 中备份重要存档，移除旧版 New Horizon / 余烬航线模组。
2. 打开“模组 → 导入模组”，选择 NewHorizon-Afterglow-0.4.0-Android160.4.jar。
3. 在系统文件选择器点“添加”或“确定”，回到游戏后按提示重启。
4. 不要解压文件，也不要改后缀。已经进入过的关卡需要重新开始才能更新地图布局。

这是模组文件，不是 APK，不要用安卓应用安装器打开。
新版地图规则、科技折扣和内容与 Windows 0.4.0 相同。

构建使用 Android SDK 36.1.0 的 D8，生成 Android DEX；min-api 为 21。
已校验 DEX 校验和、全部原始类定义、模组入口、13 张地图和其余资源一致性。
原有 0.4.0 的引擎与桌面检查已通过；此安卓包尚未在安卓真机上启动验证。
构建脚本：campaign-tools/build-android.py。源码基于同版本 source.zip。
