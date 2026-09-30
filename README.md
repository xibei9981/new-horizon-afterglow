# 余烬航线 · New Horizon Afterglow

新视界非官方社区续篇，包含完整本体与 **16 张连续战役地图**，适用于 **Mindustry 160.4**。

## 安装

游戏内「模组 → 导入模组 → 从 GitHub 导入」填写：

```text
xibei9981/new-horizon-afterglow
```

完整地址：[https://github.com/xibei9981/new-horizon-afterglow](https://github.com/xibei9981/new-horizon-afterglow)

或在[最新发布](https://github.com/xibei9981/new-horizon-afterglow/releases/latest)下载 `dexed-NewHorizon-Afterglow-0.8.0-Android160.4.jar`，安卓和电脑通用，无需解压。此包包含新视界本体，移除原版/旧版后导入并重启，安装前请备份游戏数据。

**已有区块存档不会自动换建筑，需重新开始对应关卡查看新版基地；已研究科技保留。**

## 0.8.0：分散资源与野外工业

- 全部 16 关加入不规则零散矿脉，保留开局资源与矿机脚下的矿，外部矿区沿可建设陆地展开。
- 共 35 处可接管采矿/加工设施，自带电力、运输、仓储和维修。在中心建修理投影器并供电 20 秒接管；生产走真实建筑，原料储备有限，产品需要自行运走。
- 赤峡闸口补齐重炮桥头和对应装甲进攻；白海矿驿改成两个分离矿场、跨湖货运、港口工业与六处敌方沿岸炮阵；三相遗城补入可接管工业和散矿，保留三点供能目标与纵深攻城。
- 科研仍约 20% 费用，第 12 关后开放全部战役研究门槛。新设施不占额外远程核心仓库名额，不改变武器伤害倍率。

[矿脉与地图对比](https://github.com/xibei9981/new-horizon-afterglow/releases/download/v0.8.0/Afterglow-0.8.0-map-comparison.png) · [完整玩法](CAMPAIGN.md) · [验证记录](campaign-tools/VALIDATION.md)

## 0.7.0：展开敌占区，连接前线与纵深（历史版本）

- 修正敌军仅围绕核心占据小片区域的问题。前沿、两翼、核心之间和后方新增有工业与后勤的驻防区，保留原有山脊、河流、岛屿、矿湖和任务目标。
- 第 13 关由 118 座敌方炮台增至 367 座，新增 37 片驻防区；第 16 关由 80 座增至 401 座，新增 49 片驻防区。实际建筑占地与射程覆盖分别测量，不把两者混作密度。
- 外围装备按阶段采用同步、怖烬、凝聚、光棱、终焉和裁决；每片阵地配真实电站、装甲储能、维修、物料或液体生产与输送。没有新增核心、改伤害倍率或用脚本补弹。
- 九炮激光阵地由六座氙液工厂供给：四座使用矿物生产线，两座消耗可破坏仓库中的有限石墨/钍储备。重炮的高级弹药同样有储量限制。
- 保留玩家出发基地、准备期、原有波次、16 关的不同目标和科技推进。防守关的外围驻军可以主动打掉，但不额外要求清除全部建筑才通关；群岛与矿湖不为追求数量强行填海。
- 研究费用仍约为原需求的 20%；第 12 关通关后满足全部战役研究门槛，仍须研究前置和材料。

[全图前后对比](https://github.com/xibei9981/new-horizon-afterglow/releases/download/v0.7.0/Afterglow-0.7.0-territory-comparison.png) · [安装与玩法](CAMPAIGN.md) · [实测结果与范围](campaign-tools/VALIDATION.md) · [参考设计与许可](campaign-tools/BASE-DESIGN.md)

预置工业仍需玩家扩产，部分高级中间材料与敌方弹药为有限储备。生产检查、固定目标压力测试、移动部队攻城和波次实战分别记录；没有把它们宣称为人类完整通关。未做安卓真机、Windows 原生启动或联机兼容验证。

## 源码与许可

基于 [Yuria-Shikibe/NewHorizonMod](https://github.com/Yuria-Shikibe/NewHorizonMod)，上游提交 `e29819bde94eaecf5b760c37e7af1c51c5484b57`，原作者 Yuria & Lao。修改版本为 `2.2.2-afterglow-0.7.0`，按 [GPL-3.0](LICENSE) 分发完整对应源码。

早期电站复用 Mindustry 160.4 的 GPL-3.0 基地蓝图；饱和火力 3.3.0、神恒之心 1.10 与 CT-origin 为设计参考，未搬运其地图或美术。
