"""Build and verify the Android/desktop distribution from the tested desktop JAR.

Run build.py first. Requires Android SDK build-tools (D8), an SDK platform,
and JDK 17. Override CAMPAIGN_JDK and ANDROID_HOME for another workstation.
"""
from pathlib import Path
import hashlib
import os
import struct
import subprocess
import zipfile
import zlib

root = Path(__file__).resolve().parent.parent
sdk = Path(os.environ.get('ANDROID_HOME', os.environ.get('ANDROID_SDK_ROOT', str(Path.home() / 'Library/Android/sdk'))))
jdk = Path(os.environ.get('CAMPAIGN_JDK', '/opt/homebrew/opt/openjdk@17'))
# Pin the tools used for this release; do not silently vary the compiler.
d8 = sdk / 'build-tools/36.1.0/lib/d8.jar'
android = sdk / 'platforms/android-36/android.jar'
java = jdk / 'bin' / ('java.exe' if os.name == 'nt' else 'java')
desktop = root / 'dist/NewHorizon-Afterglow-0.8.0-Windows160.4.jar'
dexzip = root / 'campaign-tools/lib/afterglow-0.8.0-dex.zip'
output = root / 'dist/NewHorizon-Afterglow-0.8.0-Android160.4.jar'
for p in [d8, android, java, desktop]:
    if not p.is_file():
        raise FileNotFoundError(p)
subprocess.run([str(java), '-cp', str(d8), 'com.android.tools.r8.D8',
    '--release', '--min-api', '21', '--lib', str(android),
    '--classpath', str(root / 'campaign-tools/lib/dependencies-v160.4.jar'),
    '--output', str(dexzip), str(desktop)], check=True)

instructions = '''# 余烬航线 0.8.0 安卓兼容包

适用于安卓 Mindustry 160.4；内含完整 New Horizon 和 16 张续篇战役地图。
同时保留桌面字节码，电脑也能使用。无需再装一个 New Horizon。

1. 在 Mindustry 中备份重要存档，移除旧版 New Horizon / 余烬航线模组。
2. 打开“模组 → 导入模组”，选择 NewHorizon-Afterglow-0.8.0-Android160.4.jar。
3. 在系统文件选择器点“添加”或“确定”，回到游戏后按提示重启。
4. 不要解压文件，也不要改后缀。已经进入过的关卡需要重新开始才能更新地图布局。

这是模组文件，不是 APK，不要用安卓应用安装器打开。
新版地图规则、科技折扣和内容与 Windows 0.8.0 相同。

构建使用 Android SDK 36.1.0 的 D8，生成 Android DEX；min-api 为 21。
已校验 DEX 校验和、全部原始类定义、模组入口、16 张地图和其余资源一致性。
0.8.0 的 16 关引擎机制检查、新三关桌面客户端加载与资源依赖检查已通过；此安卓包尚未在安卓真机上启动验证。
构建脚本：campaign-tools/build-android.py。源码基于同版本 source.zip。
'''
with zipfile.ZipFile(desktop) as src, zipfile.ZipFile(dexzip) as dex, zipfile.ZipFile(output, 'w', zipfile.ZIP_DEFLATED) as out:
    for entry in src.infolist():
        data = src.read(entry.filename)
        if entry.filename == 'mod.hjson':
            data = data.replace(b'Target: Mindustry desktop 160.4.', b'Target: Mindustry Android and desktop 160.4.')
        out.writestr(entry.filename, data)
    for name in dex.namelist():
        if name.endswith('.dex'):
            out.writestr(name, dex.read(name))
    out.writestr('ANDROID-INSTALL.md', instructions)

# Read actual class definitions, not mere type references, from the DEX tables.
def dex_classes(data):
    assert data[:8] == b'dex\n035\x00', data[:8]
    assert struct.unpack_from('<I', data, 32)[0] == len(data)
    assert struct.unpack_from('<I', data, 8)[0] == zlib.adler32(data[12:]) & 0xffffffff
    assert data[12:32] == hashlib.sha1(data[32:]).digest()
    string_count, string_offset, type_count, type_offset = struct.unpack_from('<4I', data, 56)
    strings = []
    for i in range(string_count):
        pos = struct.unpack_from('<I', data, string_offset + 4 * i)[0]
        while data[pos] & 0x80:
            pos += 1
        pos += 1
        strings.append(data[pos:data.index(b'\x00', pos)])
    types = [strings[struct.unpack_from('<I', data, type_offset + 4 * i)[0]] for i in range(type_count)]
    count, offset = struct.unpack_from('<2I', data, 96)
    return {types[struct.unpack_from('<I', data, offset + 32 * i)[0]] for i in range(count)}

with zipfile.ZipFile(output) as out, zipfile.ZipFile(desktop) as src:
    assert out.testzip() is None
    assert len(out.namelist()) == len(set(out.namelist()))
    definitions = set()
    for name in out.namelist():
        if name.endswith('.dex'):
            definitions.update(dex_classes(out.read(name)))
    expected = {('L' + name[:-6] + ';').encode() for name in src.namelist() if name.endswith('.class')}
    assert expected <= definitions, sorted(expected - definitions)
    assert b'Lnewhorizon/NewHorizon;' in definitions
    maps = [n for n in src.namelist() if n.startswith('maps/afterglow-') and n.endswith('.msav')]
    assert len(maps) == 16
    for name in src.namelist():
        if name != 'mod.hjson':
            assert out.read(name) == src.read(name), name
report = (f'ANDROID_ARCHIVE_OK\nD8: Android SDK build-tools 36.1.0; min-api 21; DEX 035\n'
    f'Original class definitions: {len(expected)} / {len(expected)}\n'
    f'DEX definitions including desugaring helpers: {len(definitions)}\n'
    'All 16 campaign maps and all original non-metadata entries byte-identical.\n'
    'DEX header, SHA-1, Adler-32, ZIP CRC and unique entries verified.\n'
    'No Android device/runtime test performed.\n')
(root / 'campaign-tools/android-verification.txt').write_text(report)
(root / 'dist/安卓安装说明-0.8.0.md').write_text(instructions)
(root / 'dist/SHA256SUMS-0.8.0-Android.txt').write_text(hashlib.sha256(output.read_bytes()).hexdigest() + '  ' + output.name + '\n')
print(report)
print(output, output.stat().st_size)
