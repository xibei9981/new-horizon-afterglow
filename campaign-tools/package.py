"""Package the current checkout's full source without dependencies, test saves or local settings."""
from pathlib import Path
import hashlib, shutil, subprocess, zipfile
root=Path(__file__).resolve().parent.parent
out=root/'dist';out.mkdir(exist_ok=True)
tracked=subprocess.check_output(['git','ls-files','-z'],cwd=root).decode().split('\0')
files={Path(p) for p in tracked if p and (root/p).is_file()}
for pattern in ['assets/maps/afterglow-*.msav','src/newhorizon/content/campaign/*.java','campaign-tools/src/*.java','campaign-tools/*.py']:
    files.update(p.relative_to(root) for p in root.glob(pattern))
files.update(map(Path,['CAMPAIGN.md','campaign-tools/VALIDATION.md','campaign-tools/verification.txt','campaign-tools/frontier-verification.txt','campaign-tools/client-verification.txt','campaign-tools/power-verification.txt','campaign-tools/signature-verification.txt','campaign-tools/resource-verification.txt','campaign-tools/resource-inputs.tsv','campaign-tools/android-verification.txt']))
source=out/'NewHorizon-Afterglow-0.5.0-source.zip'
with zipfile.ZipFile(source,'w',zipfile.ZIP_DEFLATED) as z:
    for p in sorted(files):z.write(root/p,Path('NewHorizon-Afterglow-0.5.0')/p)
shutil.copy2(root/'CAMPAIGN.md',out/'安装与玩法说明-0.5.0.md')
jar=out/'NewHorizon-Afterglow-0.5.0-Windows160.4.jar'
with zipfile.ZipFile(jar) as z:
    assert z.testzip() is None
    maps=list((root/'assets/maps').glob('afterglow-*.msav'))
    assert len(maps)==16
    for p in maps:assert z.read(str(p.relative_to(root/'assets')))==p.read_bytes()
    for n in ['mod.hjson','LICENSE','CAMPAIGN.md']:assert z.read(n)==(root/n).read_bytes()
    for n in ['AfterglowCampaign','FrontierCampaign','FrontierMissions','FrontierSites','SignatureCampaign','AfterglowTech']:
        assert 'newhorizon/content/campaign/'+n+'.class' in z.namelist()
    assert not any(n.startswith(('CampaignChecks','CampaignMaps','FrontierChecks','FrontierMaps','DesktopCheck','CampaignHarness','SignatureChecks','SignatureMaps','ResourceChecks')) for n in z.namelist())
with zipfile.ZipFile(source) as z:
    assert z.testzip() is None
    assert not any(any(part.startswith('client-') and part.endswith('data') for part in Path(n).parts) or '/lib/dependencies' in n or '/run/' in n for n in z.namelist())
(out/'SHA256SUMS-0.5.0.txt').write_text('\n'.join(hashlib.sha256(p.read_bytes()).hexdigest()+'  '+p.name for p in [jar,source])+'\n')
for p in [jar,source]:print(p.name,p.stat().st_size)
print('ARCHIVE_CHECKS_OK',len(files),'source files, 16 campaign maps')
