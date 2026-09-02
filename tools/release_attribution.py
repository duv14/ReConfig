#!/usr/bin/env python3
"""Emit an apply_patch patch for distribution credits; do not rewrite file bytes directly.
ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.
Existing authorship and licensing remain unchanged; see LICENSE-RECONFIG.txt.
"""
import pathlib
import subprocess

ROOT = pathlib.Path(__file__).resolve().parents[1]
CREDIT = 'ReConfig by duv14 incorporates OneConfig by Polyfrost and contributors.'
NOTICE = 'See ATTRIBUTIONS.md and LICENSE-RECONFIG.txt. Original copyright notices are retained.'
EXCLUDED = {'.git','.gradle','build','node_modules','__pycache__','.wrangler'}

def paths():
    names = subprocess.check_output(['git','ls-files','--cached','--others','--exclude-standard','-z'],cwd=ROOT).decode().split('\0')
    return sorted({n for n in names if n and (ROOT/n).is_file() and not EXCLUDED.intersection(pathlib.Path(n).parts)})

def main():
    names=paths()
    print('*** Begin Patch')
    for name in names:
        p=ROOT/name
        if p.suffix in {'.java','.kt','.kts','.js','.ts','.css','.frag','.vert'}:
            header='/* '+CREDIT+'\n * '+NOTICE+'\n */\n'
        elif p.suffix in {'.py','.sh','.toml','.properties','.yml','.yaml'} or p.name=='gradlew':
            header='# '+CREDIT+'\n# '+NOTICE+'\n'
        elif p.suffix in {'.svg','.xml','.html'}:
            header='<!-- '+CREDIT+' '+NOTICE+' -->\n'
        elif p.suffix=='.bat':
            header='@rem '+CREDIT+'\n@rem '+NOTICE+'\n'
        else: continue
        old=p.read_text()
        if 'Polyfrost' in old: continue
        lines=old.splitlines(keepends=True)
        at=1 if lines and (lines[0].startswith('#!') or lines[0].startswith('<?xml')) else 0
        # Preserve shebangs, XML declarations and every original license notice.
        print('*** Update File: '+str(p))
        print('@@')
        if at: print(' '+lines[0].rstrip('\r\n'))
        for line in header.splitlines(): print('+'+line)
        if len(lines)>at: print(' '+lines[at].rstrip('\r\n'))
    manifest='FILE_ATTRIBUTIONS.tsv'
    rows=['path\tdistribution_credit\tlicense_note']
    for name in sorted(set(names+[manifest])):
        rows.append(name+'\t'+CREDIT+'\tOriginal file notices and third-party rights preserved; see LICENSE-RECONFIG.txt / THIRD_PARTY_NOTICES.md')
    content='\n'.join(rows)+'\n'
    p=ROOT/manifest
    if not p.exists():
        print('*** Add File: '+str(p))
        for line in content.splitlines():print('+'+line)
    elif p.read_text()!=content:
        print('*** Update File: '+str(p));print('@@')
        for line in p.read_text().splitlines():print('-'+line)
        for line in content.splitlines():print('+'+line)
    print('*** End Patch')

if __name__=='__main__':main()
