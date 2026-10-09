"""Verify the toast area stays clear in every recorded PNG and encoded GIF frame."""
from pathlib import Path
import csv, json
from PIL import Image, ImageSequence

root=Path(__file__).resolve().parents[3]
results={}
for mode in ('vanilla','curios'):
    folder=root/f'docs/integration/goggles/evidence/{mode}/client'
    rows=list(csv.DictReader((folder/'motion.csv').open()))
    def check(image):
        rgb=image.convert('RGB')
        # The right edge above the character contains only the preview background.
        # Movement/tutorial toasts used to cover this exact part of the image.
        box=(int(rgb.width*.88),int(rgb.height*.08),rgb.width,int(rgb.height*.19))
        pixels=rgb.crop(box).getcolors(rgb.width*rgb.height)
        assert pixels==[( (box[2]-box[0])*(box[3]-box[1]),(39,49,61))], 'popup covers recorded preview'
    for row in rows:
        with Image.open(folder/f'motion-{int(row["frame"]):03d}.png') as image: check(image)
    with Image.open(folder/'animation.gif') as gif:
        for image in ImageSequence.Iterator(gif): check(image)
        count=gif.n_frames
    results[mode]={'recorded_frames':len(rows),'gif_frames':count,'toast_region_clear_in_all_frames':True}
(root/'docs/integration/goggles/evidence/gif-capture-checks.json').write_text(json.dumps(results,indent=2))
print('GIF CAPTURE PASS:',json.dumps(results))
