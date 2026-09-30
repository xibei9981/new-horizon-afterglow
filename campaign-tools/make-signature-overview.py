"""Compose original engine map previews without altering the map artwork."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
root=Path(__file__).resolve().parent.parent
fontpaths=['/System/Library/Fonts/STHeiti Medium.ttc','C:/Windows/Fonts/msyh.ttc','/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf']
fpath=next((p for p in fontpaths if Path(p).exists()),None)
font=lambda s:ImageFont.truetype(fpath,s) if fpath else ImageFont.load_default()
out=Image.new('RGB',(1560,690),'#151b24');d=ImageDraw.Draw(out)
d.text((28,20),'余烬航线 0.5.0 · 三张特色行动',font=font(32),fill='#efd19b')
d.text((28,66),'Mindustry 160.4 实际地图预览 · 第 12 关后全科技研究门槛开放',font=font(20),fill='#bac5d3')
for k,(title,desc) in enumerate([('14 赤峡闸口','72 波 / 切换地面进攻路线'),('15 白海矿驿','盐湖运输 / 12 批货物与 48 波'),('16 三相遗城','接管三处能源 / 同步后攻城')]):
 x=24+k*512;d.rounded_rectangle((x,112,x+496,662),radius=12,fill='#242d3a')
 im=Image.open(root/f'campaign-tools/previews/native-map-{k+14}.png').convert('RGB');im.thumbnail((470,432),Image.Resampling.NEAREST)
 out.paste(im,(x+(496-im.width)//2,126+(432-im.height)//2))
 d.text((x+18,578),title,font=font(27),fill='#efd19b');d.text((x+18,621),desc,font=font(20),fill='#dae0e9')
path=root/'dist/Afterglow-0.5.0-signature-overview.png';out.save(path);print(path)
