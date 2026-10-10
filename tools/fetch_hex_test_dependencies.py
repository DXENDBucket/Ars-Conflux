"""Prepare the optional Hex-Ars Linker test runtime; never bundle its mods."""
from pathlib import Path
import argparse
import hashlib
import json
import shutil
import urllib.parse
import urllib.request


def digest(path):
    with path.open('rb') as source:
        return hashlib.file_digest(source, 'sha256').hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--platform', choices=['both', 'neoforge', 'forge'], default='both')
    parser.add_argument('--replace', action='store_true')
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    for entry in json.loads((root / 'hex-test-dependencies.lock.json').read_text(encoding='utf-8')):
        if args.platform not in ('both', entry['platform']):
            continue
        relative = Path(entry['path'])
        expected = ('libs', 'hex-link') if entry['platform'] == 'neoforge' else ('forge', 'libs', 'hex-link')
        if relative.parts[:-1] != expected or relative.suffix != '.jar':
            raise RuntimeError('Invalid dependency destination')
        target = (root / relative).resolve()
        if not target.is_relative_to(root):
            raise RuntimeError('Dependency destination escapes the checkout')
        url = urllib.parse.urlparse(entry['url'])
        if url.scheme != 'https' or url.hostname not in ('cdn.modrinth.com', 'ci.blamejared.com', 'maven.blamejared.com'):
            raise RuntimeError('Unexpected dependency host')
        if target.exists() and digest(target) == entry['sha256']:
            print('Verified:', entry['path'])
            continue
        if target.exists() and not args.replace:
            raise RuntimeError(f'{target.name} differs from the pinned file; use --replace to replace it')
        target.parent.mkdir(parents=True, exist_ok=True)
        partial = target.with_suffix('.download')
        request = urllib.request.Request(entry['url'], headers={'User-Agent': 'EndXiom/Ars-Conflux-tests'})
        with urllib.request.urlopen(request, timeout=90) as response, partial.open('wb') as output:
            shutil.copyfileobj(response, output)
        if digest(partial) != entry['sha256']:
            partial.unlink()
            raise RuntimeError('Checksum mismatch for ' + target.name)
        partial.replace(target)
        print('Verified:', entry['path'])


if __name__ == '__main__':
    main()
