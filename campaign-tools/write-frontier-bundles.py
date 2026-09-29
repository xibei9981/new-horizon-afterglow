from pathlib import Path
import re
root=Path(__file__).resolve().parent.parent
text=(root/'src/newhorizon/content/campaign/FrontierMissions.java').read_text()
missions=re.findall(r'new Mission\("([^"]+)","([^"]+)","([^"]+)"',text)
messages={
 'industry-warning':'下一波开始重装阶段。准备升级供电与跃迁门，工业空投即将抵达。',
 'industry':'工业补给已送达。扩充生产，并通过科技树研究更强的武器和部队！',
 'lane-warning':'下一波将开放新的进攻路线！检查两侧防区与调兵通道。',
 'new-front':'战线扩大，增援与物资已经下发。',
 'hunt-warning':'快速突袭窗口即将关闭。尚存的补给枢纽将在第 20 波开始召来追猎队！',
 'hunt-bonus':'补给网已提前摧毁！重装突击队与额外物资抵达。',
 'storm-warning':'下一波开始电离风暴：太阳能将降至 25%，持续 10 波。切换燃料发电！',
 'storm':'电离风暴已经抵达。太阳能降低，保持防空与备用供电。',
 'clear':'风暴消散，太阳能恢复。空军增援与补给抵达。',
 'twin-warning':'第 61 波起两线同时来袭！调兵支援较弱的一侧。',
 'twin-lost':'指定核心失守，双塔防线崩溃。任务失败。',
 'depot-down':'敌军补给枢纽已摧毁，附近炮塔失去持续供弹。',
 'forward-base':'前进核心已建立，续战物资到达。可将生产与防御向前部署。',
 'resupply':'战区补给抵达核心；超过容量的物资无法储存。',
 'counterattack':'百波防线守住了！敌方停止出波，重装纵队已投入。摧毁剩余三座据点核心！',
 'ending':'长夜结束。十三关航线已贯通，弥丹莎前线建立了稳定的补给通道。',
 'defend':'坚守并清场 · {0}/{1} 波',
 'depots':'敌方补给枢纽 · 剩余 {0}/{1}',
 'rush-open':'第 20 波前有突袭奖励',
 'rush-closed':'突袭奖励窗口已关闭',
 'solar':'太阳能效率 · {0}%',
 'protect-both':'东西两座指定核心必须同时存活',
}
for name in ['bundle.properties','bundle_zh_CN.properties']:
 p=root/'assets/bundles'/name;s=p.read_text().split('\n# Afterglow Frontier 0.2.0')[0]
 s+='\n# Afterglow Frontier 0.2.0\n'
 for i,(ident,title,brief) in enumerate(missions,3):
  s+=f'sector.new-horizon-afterglow-{ident}.name = {title}\nsector.new-horizon-afterglow-{ident}.description = {brief}\n'
  s+=f'afterglow.chapter.{i} = 余烬航线 {i+1} · {title}\nafterglow.intro.{i} = {brief}\n'
 for k,v in messages.items():s+=f'afterglow.frontier.{k} = {v}\n'
 s=s.replace('新增弥丹莎后续三关「余烬航线」','新增弥丹莎十三关「余烬航线」').replace('余烬航线 · 三关战役测试版','余烬航线 · 十三关战役测试版').replace('three connected Midantha missions','thirteen connected Midantha missions').replace('Three-mission campaign alpha','Thirteen-mission campaign alpha')
 p.write_text(s)
