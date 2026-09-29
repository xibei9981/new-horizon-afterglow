# 余烬航线 · New Horizon Afterglow

New Horizon 的非官方社区续篇，包含原模组和 13 张续篇战役地图。目标版本为 **Mindustry 160.4**，Release 提供安卓与桌面通用 JAR。

## 游戏内安装

进入 **模组 → 导入模组 → 从 GitHub 导入**，填写：

```text
xibei9981/new-horizon-afterglow
```

下载后按游戏提示重启。安装前备份重要存档，移除旧版 New Horizon / 余烬航线，避免重复安装。已进入过的关卡需要重新开始才能使用新地图布局。

## 下载与源码

- [最新发布](https://github.com/xibei9981/new-horizon-afterglow/releases/latest)：选择 `dexed-NewHorizon-Afterglow-0.4.0-Android160.4.jar`，不要解压。
- **完整可编译源码**是同一 Release 下的 `NewHorizon-Afterglow-0.4.0-Android-source.zip`，包含上游源码、修改、13 张地图和构建脚本。
- 本仓库也包含完整源码；战役逻辑位于 `src/newhorizon/content/campaign/`，构建与检查脚本位于 `campaign-tools/`。
- [玩法说明](CAMPAIGN.md)

## 0.4.0 内容

十张新增关卡各有地貌、工业布局与任务节奏；可修复仓库、航空信标、变电站、防空哨所；敌方有限补给与可摧毁供电；长波防守穿插冲锋、重装和恢复波。研究费用为原来的 20%。

## 验证范围

13 关引擎检查、20 次供电复测、十张新地图桌面客户端加载已通过。安卓 D8 编译、全部类定义和包完整性校验已通过；尚未完成安卓真机启动及完整通关验证。GitHub 导入用于绕过本地文件选择器，并不代表已排除所有安卓运行问题。

## 上游与许可

基于 [Yuria-Shikibe/NewHorizonMod](https://github.com/Yuria-Shikibe/NewHorizonMod)，上游提交 `e29819bde94eaecf5b760c37e7af1c51c5484b57`。原作者 Yuria & Lao。修改版本为 `2.2.2-afterglow-0.4.0`。

依原项目 [GPL-3.0 许可证](LICENSE) 分发，保留原有署名。完整对应源码随二进制发布。
