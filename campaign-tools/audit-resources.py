"""Audit sustainable material closure from engine-exported recipes; starting crates never count as ore.
Run check.py --resources first. --propose writes additional conservative research gates for review.
This is a dependency audit, not a throughput, footprint or combat-balance proof.
"""
from pathlib import Path
from collections import defaultdict
import sys,json
root=Path(__file__).resolve().parent.parent
rows=[r.split('\t') for r in (root/'campaign-tools/resource-inputs.tsv').read_text().splitlines()]
parts=lambda s:set(filter(None,s.split(',')))
blocks={x[1]:dict(stage=int(x[2]),req=parts(x[3]),inputs=parts(x[4]),kind=x[5],parent=x[6],tech=x[7]=='true') for x in rows if x[0]=='B'}
recipes=[(x[1],parts(x[2]),parts(x[3])) for x in rows if x[0]=='R']
drills={x[1]:(int(x[2]),x[3]) for x in rows if x[0]=='D'}
hardness={x[1]:int(x[2]) for x in rows if x[0]=='I'}
units={x[1]:dict(stage=int(x[2]),req=parts(x[3]),gate=x[4]) for x in rows if x[0]=='U' and x[1].startswith('new-horizon-') and len(x)>5 and x[5]=='true'}
ammo={x[1]:parts(x[2]) for x in rows if x[0]=='A'}
maps={int(x[1]):dict(resources={a.split(':')[0]:int(a.split(':')[1]) for a in x[2].split(',') if a},seed=parts(x[3])) for x in rows if x[0]=='M'}
# These ordinary vanilla tools are explicitly provided by the campaign field kit.
vanilla={'graphite-press','silicon-smelter'}
for key in vanilla:
 if key in blocks:blocks[key]['tech']=True

def closure(stage,mp):
    ores={i for i in mp['resources'] if i in hardness};have={i for i in mp['resources'] if i not in hardness};built=set()
    # Uncounted input crates may bootstrap building construction, but never a sustainable recipe input.
    while True:
        before=len(have),len(built)
        for b,v in blocks.items():
            if v['tech'] and v['stage']<=stage and v['req'] <= have|mp['seed']:built.add(b)
        for b,(tier,blocked) in drills.items():
            if b in built:have|={i for i in ores if hardness[i]<=tier and i!=blocked}
        # Actual photon-panel update emits hard light to the core (not a GenericCrafter output).
        if 'new-horizon-photon-panel' in built:have.add('new-horizon-hard-light')
        for b,ins,outs in recipes:
            if b in built and ins<=have:have|=outs
        if before==(len(have),len(built)):return have,built

def gaps(stage,mp):
    have,built=closure(stage,mp);problems={}
    for b,v in blocks.items():
        if not v['tech'] or v['stage']>stage:continue
        missing=v['req']-have
        # For special filters, the explicit alternative ammo check below models the supported inputs.
        if b not in ammo and b not in {'new-horizon-sand-cracker','new-horizon-liquid-radiator'}:
            missing|={r for r in v['inputs'] if not r.startswith('?')}-have
        if b in ammo and not ammo[b]&have:missing|=ammo[b]
        if missing:problems[b]=sorted(missing)
    for u,v in units.items():
        if v['stage']<=stage and blocks[v['gate']]['stage']<=stage and v['req']-have:problems[u]=sorted(v['req']-have)
    return problems

if '--propose' in sys.argv:
    original={b:v['stage'] for b,v in blocks.items()}|{u:v['stage'] for u,v in units.items()}
    changes={}
    for loop in range(20):
        any_change=False
        for stage in range(13):
            for b,missing in gaps(stage,maps[min(stage+1,16)]).items():
                future=next((s for s in range(stage+1,13) if all(r in closure(s,maps[min(s+1,16)])[0] for r in missing)),None)
                if future is None:continue
                entry=blocks.get(b,units.get(b))
                if future>entry['stage']:entry['stage']=future;changes[b]=future;any_change=True
        for b,v in blocks.items():
            p=blocks.get(v['parent']);required=p['stage'] if p else 0
            if v['stage']<required:v['stage']=required;changes[b]=required;any_change=True
        for u,v in units.items():
            if v['stage']<blocks[v['gate']]['stage']:v['stage']=blocks[v['gate']]['stage'];changes[u]=v['stage'];any_change=True
        if not any_change:break
    (root/'campaign-tools/proposed-resource-gates.json').write_text(json.dumps(changes,ensure_ascii=False,indent=2))
    print('Proposed',len(changes),'additional gates')
report=[];failures=0
for number,mp in maps.items():
    # Check in-map research after this victory too: the same map must sustain its newly opened tier.
    stage=min(number,12);missing=gaps(stage,mp);missing.update(gaps(min(number-1,12),mp));have,built=closure(stage,mp)
    report.append(f'MAP {number:02d}: stage {stage}, {len(have)} sustainable resources, {len(built)} buildable facilities, {len(missing)} gaps')
    for b,resources in missing.items():report.append('  '+b+': '+', '.join(resources))
    failures+=len(missing)
text='\n'.join(report)+'\n';(root/'campaign-tools/resource-verification.txt').write_text(text)
print(text)
if failures and '--propose' not in sys.argv:raise SystemExit('Unresolved recipe/resource gaps: '+str(failures))
