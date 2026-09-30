"""Build a Windows/desktop JAR against Mindustry 160.4. No source rewriting."""
from pathlib import Path
import os, subprocess, zipfile, shutil
root = Path(__file__).resolve().parent.parent
os.chdir(root)
jdk = os.environ.get('CAMPAIGN_JDK', '/opt/homebrew/opt/openjdk@17')
javac = str(Path(jdk) / 'bin/javac') if Path(jdk).exists() else 'javac'
classes = root / 'campaign-tools/classes'
if classes.exists(): shutil.rmtree(classes)
classes.mkdir(parents=True, exist_ok=True)
sources = sorted(str(p) for p in Path('src').rglob('*.java'))
Path('campaign-tools/sources.txt').write_text('\n'.join(sources))
subprocess.run([javac, '-encoding','UTF-8','--release','17','-sourcepath','src','-cp','campaign-tools/lib/dependencies-v160.4.jar','-d',str(classes),'@campaign-tools/sources.txt'], check=True)
Path('dist').mkdir(exist_ok=True)
out = Path('dist/NewHorizon-Afterglow-0.6.0-Windows160.4.jar')
with zipfile.ZipFile(out, 'w', zipfile.ZIP_DEFLATED) as z:
 for base in [classes, root / 'assets']:
  for p in sorted(base.rglob('*')):
   if p.is_file() and '/sounds/raw/' not in p.as_posix(): z.write(p, p.relative_to(base))
 for name in ['mod.hjson', 'LICENSE']:
  z.write(name, name)
 if Path('CAMPAIGN.md').exists(): z.write('CAMPAIGN.md','CAMPAIGN.md')
Path('campaign-tools/run/mods').mkdir(parents=True, exist_ok=True)
shutil.copy2(out, 'campaign-tools/run/mods/afterglow.jar')
print(out, out.stat().st_size)
