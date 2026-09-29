"""Contact sheet of unretouched game screenshots; labels are outside each screenshot."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
root=Path(__file__).resolve().parent.parent
fontpath=next(p for p in ['/System/Library/Fonts/STHeiti Medium.ttc','C:/Windows/Fonts/msyh.ttc','/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'] if Path(p).exists())
font=lambda n:ImageFont.truetype(fontpath,n)
width,height=920,449
sheet=Image.new('RGB',(width*2+60,(height+62)*2+115),'#121822')
d=ImageDraw.Draw(sheet)
d.text((20,15),'余烬航线 0.4.0 · 战役改造实机画面',font=font(31),fill='#f4d48b')
d.text((20,60),'Mindustry 160.4 客户端截图 · 前方矿区 · 可修复遗迹 · 分层敌方阵地',font=font(19),fill='#d0d4df')
for i,(idx,title) in enumerate([(4,'熔炉盆地 · 工业开局与前方矿区'),(5,'断桥群岛 · 可修复航空信标'),(7,'猎网突袭 · 分散火力与突击出发区'),(8,'风暴穹顶 · 防空与供电准备')]):
 x=20+(i%2)*(width+20);y=110+(i//2)*(height+62)
 d.text((x,y),title,font=font(23),fill='#f4d48b')
 im=Image.open(root/f'campaign-tools/previews/client-{idx}.png').convert('RGB')
 im=im.resize((width,height),Image.Resampling.LANCZOS);sheet.paste(im,(x,y+34))
out=root/'dist/Afterglow-0.4.0-terrain-in-game.png';sheet.save(out);print(out)
