"""Render an evidence figure from engine-exported map previews, not concept art."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
root=Path(__file__).resolve().parent.parent
font_path='/System/Library/Fonts/STHeiti Medium.ttc'
font=lambda size: ImageFont.truetype(font_path,size)
im=Image.new('RGB',(1112,1434),'#131a25');d=ImageDraw.Draw(im)
d.text((28,22),'余烬航线 · 敌占区实际布局对比',font=font(32),fill='#f2f5fc')
d.text((28,67),'黄色：玩家建筑   红色：敌方建筑   灰蓝：原有地形／水域',font=font(19),fill='#becadb')
for row,(chapter,height,old,new,oa,na) in enumerate([(13,544,118,367,'7.43%','20.08%'),(16,576,80,401,'5.07%','20.80%')]):
 y=110+row*644
 for col,(version,guns,area) in enumerate([('0.6.0',old,oa),('0.7.0',new,na)]):
  x=28+col*556
  d.text((x,y),f'第 {chapter} 关 · {version}',font=font(25),fill='#f2f5fc')
  d.text((x,y+34),f'{guns} 座敌炮 · 建筑占地 {area}',font=font(19),fill='#ff8f9b' if col else '#becadb')
  p=Image.open(root/f'campaign-tools/territory-{version}/map-{chapter}.png').convert('RGB')
  assert p.size==(512,height)
  im.paste(p,(x,y+66))
d.text((28,1394),'统计为真实地块占用；占地不等于射程覆盖。新区块布局，非旧存档自动替换。',font=font(17),fill='#becadb')
out=root/'dist/Afterglow-0.7.0-territory-comparison.png';im.save(out);print(out)
