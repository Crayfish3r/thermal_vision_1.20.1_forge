"""Complete task Java delta against the saved checkout at task start.
The existing user Java changes are already in that baseline, not reverted.
"""
from pathlib import Path
import difflib, os
root=Path(__file__).resolve().parents[3]
baseline=Path(os.environ['TEMP'])/'thermal-goggles-integration-baseline/java'
assert baseline.is_dir(), 'The initial checkout snapshot is required'
actual=root/'src/main/java'
relative=sorted(set(p.relative_to(actual) for p in actual.rglob('*.java'))
                |set(p.relative_to(baseline) for p in baseline.rglob('*.java')))
patch=[];changed=[]
for rel in relative:
    before=(baseline/rel).read_text(encoding='utf8').splitlines(True) if (baseline/rel).exists() else []
    after=(actual/rel).read_text(encoding='utf8').splitlines(True) if (actual/rel).exists() else []
    if before==after: continue
    path='src/main/java/'+rel.as_posix()
    patch.extend(difflib.unified_diff(before,after,'a/'+path if before else '/dev/null','b/'+path))
    changed.append(path)
for path in sorted((root/'docs/integration/goggles').rglob('*.java')):
    rel=path.relative_to(root).as_posix()
    patch.extend(difflib.unified_diff([],path.read_text(encoding='utf8').splitlines(True),'/dev/null','b/'+rel))
    changed.append(rel)
(root/'docs/integration/goggles/JAVA_DIFF.patch').write_text(''.join(patch),encoding='utf8')
print('Full task Java diff:',len(changed),'files;',len(patch),'lines')
print('\n'.join(changed))
