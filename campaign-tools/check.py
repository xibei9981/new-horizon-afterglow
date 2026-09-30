from pathlib import Path
import os,subprocess,zipfile,shutil,sys
os.chdir(Path(__file__).resolve().parent.parent)
jdk=Path(os.environ.get('CAMPAIGN_JDK','/opt/homebrew/opt/openjdk@17'))/'bin'
def java_tool(name):
 executable = name + ('.exe' if os.name == 'nt' else '')
 return str(jdk/executable) if (jdk/executable).exists() else executable
subprocess.run([java_tool('javac'),'-encoding','UTF-8','--release','17','-sourcepath','campaign-tools/src','-cp',os.pathsep.join(['campaign-tools/lib/server-v160.4.jar','campaign-tools/classes']),'-d','campaign-tools/test-classes',*map(str,[Path('campaign-tools/src')/n for n in ['CampaignHarness.java','CampaignMaps.java','CampaignChecks.java','FrontierChecks.java','SignatureChecks.java','ResourceChecks.java']])],check=True)
Path('campaign-tools/launcher').mkdir(exist_ok=True)
shutil.copy2('campaign-tools/test-classes/CampaignHarness.class','campaign-tools/launcher')
shutil.copy2('campaign-tools/test-classes/CampaignHarness$1.class','campaign-tools/launcher')
shutil.copy2('dist/NewHorizon-Afterglow-0.5.0-Windows160.4.jar','campaign-tools/run/mods/afterglow.jar')
with zipfile.ZipFile('campaign-tools/run/mods/afterglow.jar','a') as z:
 for p in Path('campaign-tools/test-classes').glob('*.class'):
  if not p.name.startswith('CampaignHarness'):z.write(p,p.name)
subprocess.run([java_tool('java'),*(['-Dcampaign.resources=true'] if '--resources' in sys.argv else ['-Dcampaign.signatureVerify=true'] if '--signature-only' in sys.argv else ['-Dcampaign.lateVerify=true'] if '--late-only' in sys.argv else ['-Dcampaign.verify=true','-Dcampaign.frontierVerify=true','-Dcampaign.signatureVerify=true']+(['-Dcampaign.powerProbe=true'] if '--power-probe' in sys.argv else [])),'-cp',os.pathsep.join(['campaign-tools/launcher','campaign-tools/lib/server-v160.4.jar']),'CampaignHarness'],check=True)
