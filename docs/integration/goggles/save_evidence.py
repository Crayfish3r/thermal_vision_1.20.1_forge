"""Archive game evidence outside build/ before clean, and encode captured game frames.
No generated or interpolated poses: GIF frames are actual Minecraft screenshots.
"""
from pathlib import Path
import csv, shutil
from PIL import Image

root=Path(__file__).resolve().parents[3]
dest=root/'docs/integration/goggles/evidence'
for mode in ('vanilla','curios'):
    for side in ('client','server'):
        source=root/f'build/goggles-audit/{mode}/{side}'
        target=dest/mode/side
        target.mkdir(parents=True,exist_ok=True)
        log=source/'logs/latest.log'
        if log.exists():
            text=log.read_text(encoding='utf8',errors='replace')
            assert f'GOGGLES {side.upper()} AUDIT COMPLETE' in text
            assert f'GOGGLES {side.upper()} AUDIT FAILED' not in text
            shutil.copy2(log,target/'latest.log')
        frames=source/'goggles-evidence'
        if not frames.exists(): continue
        for path in frames.glob('*.png'):
            if not path.name.startswith('motion-'): shutil.copy2(path,target/path.name)
        rows=list(csv.DictReader((frames/'motion.csv').open()))
        shutil.copy2(frames/'motion.csv',target/'motion.csv')
        images=[];durations=[]
        for i,row in enumerate(rows):
            path=frames/f'motion-{int(row["frame"]):03d}.png'
            shutil.copy2(path,target/path.name)
            images.append(Image.open(path).convert('RGB'))
            seconds=(float(rows[i+1]['game_seconds'])-float(row['game_seconds'])) if i+1<len(rows) else .4
            durations.append(max(10,round(seconds*100)*10))
        # Preserve captured game-clock durations; no smoothing or synthesized frames.
        images[0].save(target/'animation.gif',save_all=True,append_images=images[1:],
                       duration=durations,loop=0,optimize=False,disposal=2)
        print(mode,side,len(rows),'actual captured animation frames')
old=root/'build/audit-run/logs/latest.log'
if old.exists():
    dest.mkdir(parents=True,exist_ok=True)
    shutil.copy2(old,dest/'thermal-regression.log')
