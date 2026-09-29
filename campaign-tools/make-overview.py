"""Draw a geographic contact sheet from the engine-verified tile previews."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import re
root=Path(__file__).resolve().parent.parent
missions=re.findall(r'new Mission\("([^"]+)","([^"]+)","([^"]+)"',(root/'src/newhorizon/content/campaign/FrontierMissions.java').read_text())
fonts=['/System/Library/Fonts/STHeiti Medium.ttc','/System/Library/Fonts/PingFang.ttc','C:/Windows/Fonts/msyh.ttc','/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf']
fontpath=next((p for p in fonts if Path(p).exists()),None)
font=lambda size:ImageFont.truetype(fontpath,size) if fontpath else ImageFont.load_default()
cellw,cellh=310,358
sheet=Image.new('RGB',(5*cellw+48,2*cellh+146),'#121822');d=ImageDraw.Draw(sheet)
d.text((24,18),'余烬航线 · 后续十关地图总览',font=font(30),fill='#f4d48b')
d.text((24,62),'0.4.0 经营与战斗重做 · 由 Mindustry 160.4 客户端生成的原生全图预览',font=font(17),fill='#d0d4df')
types=['60 波 · 先发育后爆发','夺取 3 岛 · 空地双线','72 波 · 逐步开启三线','双据点 · 抢先切断补给','80 波 · 空袭与供电风暴','四据点 · 接管前进核心','90 波 · 同时保护双核心','五据点 · 装甲纵队推进','三据点 · 摧毁出兵枢纽','100 波后 · 重装反攻']
for i,(_,name,_) in enumerate(missions):
 x=24+(i%5)*cellw;y=108+(i//5)*cellh
 d.rounded_rectangle((x,y,x+cellw-12,y+cellh-12),radius=10,fill='#202937')
 im=Image.open(root/f'campaign-tools/previews/native-map-{i+4}.png').convert('RGB');im.thumbnail((278,264),Image.Resampling.NEAREST)
 sheet.paste(im,(x+(cellw-12-im.width)//2,y+12+(264-im.height)//2))
 d.text((x+14,y+284),f'{i+4:02d}  {name}',font=font(22),fill='#f4d48b')
 d.text((x+14,y+319),types[i],font=font(15),fill='#d0d4df')
out=root/'dist/Afterglow-0.4.0-map-overview.png';sheet.save(out);print(out)
