"""Download the pinned public Mindustry build dependencies (not the game client)."""
from pathlib import Path
from urllib.request import urlopen, Request
import hashlib
root = Path(__file__).resolve().parent / 'lib'
root.mkdir(exist_ok=True)
assets = {'dependencies-v160.4.jar': 'dependencies.jar', 'server-v160.4.jar': 'server-release.jar'}
hashes = {'dependencies-v160.4.jar': '3bf795fbfda1e861164ec731972fea4af1b586b81412f53b851818053b717951', 'server-v160.4.jar': '0dbd3275402ff6df8367620a47e0b24e4d4547332b99af7c434796520ca484c2'}
for target, asset in assets.items():
    path = root / target
    if not path.exists():
        url = 'https://github.com/Anuken/Mindustry/releases/download/v160.4/' + asset
        with urlopen(Request(url, headers={'User-Agent': 'Afterglow-build'})) as response:
            data = response.read()
        path.write_bytes(data)
    digest = hashlib.sha256(path.read_bytes()).hexdigest()
    if digest != hashes[target]:
        raise RuntimeError('Dependency checksum mismatch: ' + target)
    print(target, digest)
