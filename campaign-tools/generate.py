from pathlib import Path
import os,subprocess,zipfile,shutil
os.chdir(Path(__file__).resolve().parent.parent)
jdk=Path(os.environ.get('CAMPAIGN_JDK','/opt/homebrew/opt/openjdk@17'))/'bin'
def java_tool(name):
 executable = name + ('.exe' if os.name == 'nt' else '')
 return str(jdk/executable) if (jdk/executable).exists() else executable
subprocess.run([java_tool('javac'),'-encoding','UTF-8','--release','17','-sourcepath','campaign-tools/src','-cp',os.pathsep.join(['campaign-tools/lib/server-v160.4.jar','campaign-tools/classes']),'-d','campaign-tools/test-classes','campaign-tools/src/CampaignMaps.java','campaign-tools/src/FrontierMaps.java','campaign-tools/src/CampaignHarness.java'],check=True)
Path('campaign-tools/launcher').mkdir(exist_ok=True)
for p in Path('campaign-tools/test-classes').glob('CampaignHarness*.class'): shutil.copy2(p,'campaign-tools/launcher')
shutil.copy2('dist/NewHorizon-Afterglow-0.4.0-Windows160.4.jar','campaign-tools/run/mods/afterglow.jar')
with zipfile.ZipFile('campaign-tools/run/mods/afterglow.jar','a') as z:
 for pattern in ['CampaignMaps*.class','FrontierMaps*.class']:
  for p in Path('campaign-tools/test-classes').glob(pattern):z.write(p,p.name)
subprocess.run([java_tool('java'),'-Dcampaign.generate=true','-Dcampaign.frontier=true','-cp',os.pathsep.join(['campaign-tools/launcher','campaign-tools/lib/server-v160.4.jar']),'CampaignHarness'],check=True)
