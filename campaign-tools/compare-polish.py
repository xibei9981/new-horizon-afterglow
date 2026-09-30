"""Render saved map-tile evidence, not concept art. Requires Pillow."""
from pathlib import Path
import re
from PIL import Image, ImageDraw, ImageFont
root=Path(__file__).resolve().parent.parent
font_path='/System/Library/Fonts/STHeiti Medium.ttc'
def font(size): return ImageFont.truetype(font_path,size)
def metrics(version):
    rows={}
    for line in (root/f'campaign-tools/polish-{version}/verification.txt').read_text().splitlines():
        if line.startswith('ORES '):
            d=dict(re.findall(r'(\w+)=(\d+)',line));rows[int(d['map'])]={k:int(v) for k,v in d.items()}
    return rows
old,new=metrics('0.7.0'),metrics('0.8.0')
names=['断流峡谷','双脊封锁','寂光中枢','熔炉盆地','断桥群岛','三叉前线','猎网突袭','风暴穹顶','逆流远征','孤城双塔','破晓兵工厂','静默物流','长夜终焉','赤峡闸口','白海矿驿','三相遗城']
canvas=Image.new('RGB',(1500,2850),'#141923');draw=ImageDraw.Draw(canvas)
draw.text((50,30),'余烬航线 0.8.0 · 分散矿脉与野外工业',font=font(36),fill='#faf1d6')
draw.text((50,85),'实际 .msav 地块俯视图｜左：0.7.0　右：0.8.0',font=font(23),fill='#c0cbd6')
draw.text((50,124),'金色：玩家设施　红色：敌军　灰色：中立/遗迹　彩色斑块：矿物',font=font(21),fill='#a8b8c7')
for row,c in enumerate([1,14,15,16]):
    top=190+row*650
    draw.text((50,top),f'{c:02d}  {names[c-1]}',font=font(29),fill='#faf1d6')
    for col,version,data in [(0,'0.7.0',old),(1,'0.8.0',new)]:
        x=50+col*740
        im=Image.open(root/f'campaign-tools/polish-{version}/map-{c}.png').convert('RGB');scale=min(680/im.width,530/im.height);im=im.resize((round(im.width*scale),round(im.height*scale)),Image.Resampling.NEAREST)
        canvas.paste(im,(x+(680-im.width)//2,top+47+(530-im.height)//2))
        d=data[c]
        draw.text((x,top+586),f"{version} · 小矿脉 {d['small4to100']} 片 · 资源分布 {d['occupied8x8Bins']}/64 区",font=font(21),fill='#bdd2db')
draw.text((50,2800),'小矿脉 = 同种矿四向相连 4～100 格；64 区 = 全图等分 8×8。非战力评分。',font=font(19),fill='#9daebc')
out=root/'dist/Afterglow-0.8.0-map-comparison.png';canvas.save(out);print(out)
canvas=Image.new('RGB',(1600,2040),'#141923');draw=ImageDraw.Draw(canvas)
draw.text((35,22),'余烬航线 0.8.0 · 全部 16 关',font=font(32),fill='#faf1d6')
for i in range(16):
    x=20+(i%4)*395;y=88+(i//4)*480
    draw.text((x+10,y),f'{i+1:02d} {names[i]}',font=font(23),fill='#faf1d6')
    im=Image.open(root/f'campaign-tools/polish-0.8.0/map-{i+1}.png').convert('RGB');im.thumbnail((375,400),Image.Resampling.NEAREST)
    canvas.paste(im,(x+(375-im.width)//2,y+36+(400-im.height)//2))
    draw.text((x+10,y+442),f"小矿脉 {new[i+1]['small4to100']} 片",font=font(19),fill='#bdd2db')
canvas.save(root/'dist/Afterglow-0.8.0-all-maps.png')
print('small patches',sum(d['small4to100'] for d in old.values()),sum(d['small4to100'] for d in new.values()))
