#!/usr/bin/env python3
"""Collect notices from the actual Cargo package sources, without adding APK code."""
import hashlib
import json
from pathlib import Path
import subprocess

repo = Path(__file__).resolve().parent.parent
metadata = json.loads(subprocess.check_output([
    'cargo', 'metadata', '--format-version', '1', '--locked', '--offline',
    '--filter-platform', 'aarch64-linux-android',
], cwd=repo / 'mobile/src/main/rust/vpnhotspotd'))
output = ['Rust daemon dependency notices',
          'Collected from the pinned local Cargo source packages.\n']
licenses = {}
for package in sorted(metadata['packages'], key=lambda p: (p['name'], p['version'])):
    if not package['source']:
        continue
    root = Path(package['manifest_path']).parent
    texts = list(root.glob('LICENSE*')) + list(root.glob('COPYING*')) + list(root.glob('NOTICE*'))
    texts += list(root.glob('license*'))
    if package.get('license_file'):
        texts.append(root / package['license_file'])
    refs = []
    for path in sorted(set(texts)):
        if not path.is_file():
            continue
        data = path.read_text(errors='replace').strip()
        digest = hashlib.sha256(data.encode()).hexdigest()
        if digest not in licenses:
            licenses[digest] = (len(licenses) + 1, data)
        refs.append(str(licenses[digest][0]))
    output += [f"\n{package['name']} {package['version']}",
               f"License: {package.get('license') or 'see license file'}",
               f"Source: {package.get('repository') or package['source']}",
               'Notice texts: ' + ', '.join(refs)]
for number, data in licenses.values():
    output += [f'\n----- Notice {number} -----\n', data]
(repo / 'mobile/src/main/assets/rust-notices.txt').write_text('\n'.join(output) + '\n')
print(f"Collected {len(licenses)} distinct texts for {len(metadata['packages']) - 1} packages.")
