"""Build editable Blockbench variants from the supplied thermal goggles model.

Uses Pillow/NumPy for deterministic UV textures and an orthographic geometry preview.
No game resources or Java code are changed.
"""
import base64
import copy
import io
import json
import math
import sys
import uuid
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent
SOURCE = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(r'C:/Users/makar/OneDrive/Рабочий стол/thermal_goggles.bbmodel')
BASE = json.loads(SOURCE.read_text(encoding='utf-8'))
SIDES = ('east', 'west', 'north', 'south', 'up', 'down')


def uid(name):
    return str(uuid.uuid5(uuid.NAMESPACE_URL, 'thermal-vision/variants/' + name))


def group(name, children, origin=(0, 24, 0)):
    return dict(name=name, uuid=uid(name), origin=list(origin), rotation=[0, 0, 0],
                color=0, export=True, visibility=True, isOpen=True, children=children)


def recolor(texture):
    src = Image.open(io.BytesIO(base64.b64decode(texture['source'].split(',')[1]))).convert('RGBA')
    out = Image.new('RGBA', (256, 256))
    for y in range(src.height):
        for x in range(src.width):
            r, g, b, a = src.getpixel((x, y))
            if not a:
                continue
            # Preserve original edge wear and metal/plastic contrast, remove olive tint.
            lum = .2126*r + .7152*g + .0722*b
            value = round(10 + lum * .48)
            out.putpixel((x, y), (value, value+1, value+2, a))
    return out


class Builder:
    def __init__(self, variant):
        self.variant = variant
        self.model = copy.deepcopy(BASE)
        # Free format supports mixed box/per-face UVs. Modded Entity forces box UVs
        # and would discard the dense, continuous grain mapping on the glass.
        self.model['meta']['model_format'] = 'free'
        self.model['name'] = 'thermal_goggles_' + variant
        self.model['model_identifier'] = 'WhiteHotGogglesModel' if variant == 'white_hot' else 'NightVisionHelmetModel'
        self.model['resolution'] = dict(width=256, height=256)
        self.images = [recolor(t) for t in BASE['textures']]
        self.x, self.y, self.row = 2, 64, 0

    def cube(self, name, lo, hi, material='body', origin=(0, 31.25, -5.6)):
        w, h, d = [round(hi[i]-lo[i], 5) for i in range(3)]
        tw, th = math.ceil(2*(w+d))+2, math.ceil(h+d)+2
        if self.x+tw > 254:
            self.x, self.y, self.row = 2, self.y+self.row+2, 0
        assert self.y+th < 190, 'UV packing overflow'
        u, v = self.x+1, self.y+1
        self.x += tw+2
        self.row = max(self.row, th)
        uv = dict(east=[u,v+d,u+d,v+d+h], west=[u+d+w,v+d,u+2*d+w,v+d+h],
                  north=[u+d,v+d,u+d+w,v+d+h], south=[u+2*d+w,v+d,u+2*d+2*w,v+d+h],
                  up=[u+d+w,v+d,u+d,v], down=[u+d+2*w,v,u+d+w,v+d])
        e = dict(name=name, type='cube', uuid=uid(self.variant+'/'+name), box_uv=True,
                 uv_offset=[u,v], **{'from':lo, 'to':hi}, origin=list(origin), rotation=[0,0,0],
                 autouv=0, color=0, inflate=0, mirror_uv=False, shade=True, visibility=True,
                 export=True, faces={s:dict(uv=uv[s], texture=0) for s in SIDES})
        shade = {'body':27, 'edge':40, 'rubber':16, 'metal':63}[material]
        for im in self.images:
            dr = ImageDraw.Draw(im)
            dr.rectangle((u-1,v-1,u+tw-2,v+th-2), fill=(shade,shade+1,shade+2,255))
            dr.line((u-1,v-1,u+tw-2,v-1), fill=(shade+9,shade+10,shade+11,255))
        self.model['elements'].append(e)
        return e

    def lens(self, e, bounds, patch):
        # Continuous world-space UVs also join the stepped outside corners seamlessly.
        e['box_uv'] = False
        x0,y0,x1,y1 = bounds
        u0,v0,u1,v1 = patch
        for s,f in e['faces'].items():
            f['uv'] = [u0+2,v0+2,u1-2,v1-2]
        lo,hi = e['from'],e['to']
        e['faces']['north']['uv'] = [u0+(lo[0]-x0)/(x1-x0)*(u1-u0),
            v0+(y1-hi[1])/(y1-y0)*(v1-v0),u0+(hi[0]-x0)/(x1-x0)*(u1-u0),
            v0+(y1-lo[1])/(y1-y0)*(v1-v0)]

    def paint_glass(self, patch, green=False):
        u0,v0,u1,v1 = patch
        rng = np.random.default_rng(148 if green else 92+u0)
        noise = rng.normal(0, 1, (v1-v0,u1-u0))
        for state,im in enumerate(self.images):
            for y in range(v0,v1):
                for x in range(u0,u1):
                    fx,fy = (x-u0)/(u1-u0),(y-v0)/(v1-v0)
                    n = noise[y-v0,x-u0]
                    shine = max(0,1-abs(fy-(.22+.17*fx))/.055)
                    edge = min(1, min(fx,1-fx,fy,1-fy)*12)
                    if green:
                        val = 77+67*edge+7*n+35*shine-(y%3==0)*10 if state==0 else 29+29*edge+3*n+18*shine
                        rgb = (int(val*.24),int(val),int(val*.39))
                    else:
                        val = 153+51*edge+18*n+28*shine if state==0 else 72+34*edge+5*n+20*shine
                        rgb = (int(val),int(val),int(val))
                    im.putpixel((x,y),tuple(max(0,min(255,c)) for c in rgb)+(255,))

    def save(self):
        folder = ROOT / self.variant
        folder.mkdir(exist_ok=True)
        for i,(t,im) in enumerate(zip(self.model['textures'],self.images)):
            name = self.model['name']+('_on.png' if i==0 else '_off.png')
            im.save(folder/name)
            buf = io.BytesIO(); im.save(buf,format='PNG')
            t.update(name=name,relative_path=name,width=256,height=256,uv_width=256,uv_height=256,
                     source='data:image/png;base64,'+base64.b64encode(buf.getvalue()).decode())
            for stale in ('path','absolute_path'):
                t.pop(stale,None)
        path = folder/(self.model['name']+'.bbmodel')
        path.write_text(json.dumps(self.model,ensure_ascii=False,indent=2),encoding='utf-8')
        return path

    def save_box_uv(self):
        """Companion project using the source's Java/Modded Entity UV restrictions."""
        m = copy.deepcopy(self.model)
        m['meta']['model_format'] = 'modded_entity'
        m['meta']['box_uv'] = True
        images = [im.copy() for im in self.images]
        rng = np.random.default_rng(507)
        for e in m['elements']:
            if e['box_uv']:
                continue
            w,h,d = [b-a for a,b in zip(e['from'],e['to'])]
            u,v = e['uv_offset']
            uv = dict(east=[u,v+d,u+d,v+d+h],west=[u+d+w,v+d,u+2*d+w,v+d+h],
                      north=[u+d,v+d,u+d+w,v+d+h],south=[u+2*d+w,v+d,u+2*d+2*w,v+d+h],
                      up=[u+d+w,v+d,u+d,v],down=[u+d+2*w,v,u+d+w,v+d])
            e['box_uv'] = True
            for s in SIDES:
                e['faces'][s]['uv'] = uv[s]
            for y in range(math.floor(v),math.ceil(v+d+h)):
                for x in range(math.floor(u),math.ceil(u+2*d+2*w)):
                    n = rng.normal()
                    for state,im in enumerate(images):
                        if self.variant=='white_hot':
                            value = int((207+26*n) if state==0 else (95+10*n))
                            rgb=(value,value,value)
                        else:
                            value = int((148+18*n) if state==0 else (51+7*n))
                            rgb=(int(value*.24),value,int(value*.39))
                        im.putpixel((x,y),tuple(max(0,min(255,c)) for c in rgb)+(255,))
        folder = ROOT / self.variant / 'modded_entity'
        folder.mkdir(exist_ok=True)
        for i,(t,im) in enumerate(zip(m['textures'],images)):
            im.save(folder/t['name'])
            buf=io.BytesIO();im.save(buf,format='PNG')
            t['source']='data:image/png;base64,'+base64.b64encode(buf.getvalue()).decode()
        (folder/(m['name']+'.bbmodel')).write_text(json.dumps(m,ensure_ascii=False,indent=2),encoding='utf-8')
        assert all(e['box_uv'] for e in m['elements'])


white = Builder('white_hot')
for side,cx,patch in [('left',2.5,(8,200,88,248)),('right',-2.5,(104,200,184,248))]:
    white.paint_glass(patch)
    for e in white.model['elements']:
        if e['name'].startswith(side+'_lens_'):
            white.lens(e,(cx-1.51,26.89,cx+1.51,29.91),patch)
white.save()

night = Builder('night_vision')
head = night.model['outliner'][0]
fixed,hinge = head['children']
# Keep original mount geometry and the animated pivot, replace the binocular assembly.
keep = set()
def collect(g):
    for c in g['children']:
        collect(c) if isinstance(c,dict) else keep.add(c)
collect(fixed)
link = next(e for e in night.model['elements'] if e['name']=='hinge_moving_link')
keep.add(link['uuid'])
night.model['elements'] = [e for e in night.model['elements'] if e['uuid'] in keep]
shell = []
for name,lo,hi,mat in [
    ('helmet_crown',[-5.55,32.15,-5.35],[5.55,33.0,5.5],'body'),
    ('helmet_left_shell',[5.12,29.5,-5.12],[5.8,32.15,5.4],'body'),
    ('helmet_right_shell',[-5.8,29.5,-5.12],[-5.12,32.15,5.4],'body'),
    ('helmet_rear_shell',[-5.12,28.8,5.12],[5.12,32.15,5.8],'body'),
    ('helmet_front_brow',[-5.12,30.85,-5.7],[5.12,32.15,-5.12],'edge'),
    ('helmet_crown_rail_left',[3.4,33,-3.8],[4.15,33.18,3.5],'edge'),
    ('helmet_crown_rail_right',[-4.15,33,-3.8],[-3.4,33.18,3.5],'edge'),
]:
    shell.append(night.cube(name,lo,hi,mat,origin=(0,24,0))['uuid'])
fixed['children'].append(group('helmet_shell',shell))
housing = [link['uuid']]
for name,lo,hi,mat in [
    ('visor_brow_frame',[-4.65,30.10,-8.45],[4.65,30.65,-6.1],'edge'),
    ('visor_lower_frame',[-4.65,26.35,-8.45],[4.65,26.90,-6.2],'body'),
    ('visor_left_frame',[4.65,27.35,-8.45],[5.2,29.65,-6.05],'body'),
    ('visor_right_frame',[-5.2,27.35,-8.45],[-4.65,29.65,-6.05],'body'),
    ('visor_top_left_corner',[4.1,29.65,-8.45],[4.98,30.3,-6.2],'edge'),
    ('visor_top_right_corner',[-4.98,29.65,-8.45],[-4.1,30.3,-6.2],'edge'),
    ('visor_bottom_left_corner',[4.1,26.7,-8.45],[4.98,27.35,-6.2],'body'),
    ('visor_bottom_right_corner',[-4.98,26.7,-8.45],[-4.1,27.35,-6.2],'body'),
    ('visor_left_hinge',[5.2,28.2,-7.25],[5.65,29.25,-6.2],'metal'),
    ('visor_right_hinge',[-5.65,28.2,-7.25],[-5.2,29.25,-6.2],'metal'),
    ('visor_control_cap',[-.8,30.65,-7.1],[.8,31.1,-6.1],'body'),
]:
    housing.append(night.cube(name,lo,hi,mat)['uuid'])
glass = []
patch = (8,200,248,248)
night.paint_glass(patch,True)
for name,lo,hi in [
    ('visor_single_pane',[-4.12,26.9,-8.30],[4.12,30.1,-7.92]),
    ('visor_left_glass_corner',[4.12,27.35,-8.30],[4.65,29.65,-7.92]),
    ('visor_right_glass_corner',[-4.65,27.35,-8.30],[-4.12,29.65,-7.92]),
]:
    e = night.cube(name,lo,hi,'body')
    night.lens(e,(-4.65,26.9,4.65,30.1),patch)
    glass.append(e['uuid'])
hinge['children'] = [group('visor_frame',housing,(0,31.25,-5.6)),group('continuous_green_visor',glass,(0,31.25,-5.6))]
night.save()


def validate(builder):
    m = builder.model
    ids = {e['uuid'] for e in m['elements']}
    assert len(ids)==len(m['elements'])
    refs,groups = [],set()
    def walk(g):
        groups.add(g['uuid'])
        for c in g['children']:
            walk(c) if isinstance(c,dict) else refs.append(c)
    for g in m['outliner']: walk(g)
    assert len(refs)==len(set(refs)) and set(refs)==ids
    for a in m['animations']: assert set(a['animators']) <= groups
    for e in m['elements']:
        assert all(a<b for a,b in zip(e['from'],e['to']))
        for f in e['faces'].values():
            assert f['texture']==0 and all(0<=v<=256 for v in f['uv'])
    assert m['animations']==BASE['animations']
    print(m['name'],len(ids),'cubes; hierarchy, UV bounds, embedded textures and animation targets OK')


def render(builder, yaw, pitch, size=(740,470)):
    """Rasterize actual model faces, with nearest texture sampling and a depth buffer."""
    width,height = size
    view = np.array([math.sin(yaw)*math.cos(pitch),math.sin(pitch),-math.cos(yaw)*math.cos(pitch)])
    right = np.array([math.cos(yaw),0,math.sin(yaw)])
    up = np.cross(right,view)
    transform = np.array([right,-up,view])
    center = np.array([0,29.3,-1.6])
    scale = min(width/19,height/12)
    pixels = np.zeros((height,width,4),dtype=np.uint8)
    depth = np.full((height,width),-1e10)
    texture = np.asarray(builder.images[0])
    for e in builder.model['elements']:
        x,y,z = e['from']; X,Y,Z = e['to']
        faces = {
            'north':([(x,Y,z),(X,Y,z),(X,y,z),(x,y,z)],1.0),
            'south':([(X,Y,Z),(x,Y,Z),(x,y,Z),(X,y,Z)],.74),
            'east':([(X,Y,z),(X,Y,Z),(X,y,Z),(X,y,z)],.80),
            'west':([(x,Y,Z),(x,Y,z),(x,y,z),(x,y,Z)],.72),
            'up':([(x,Y,Z),(X,Y,Z),(X,Y,z),(x,Y,z)],1.18),
            'down':([(x,y,z),(X,y,z),(X,y,Z),(x,y,Z)],.62)}
        for s,(vertices,shade) in faces.items():
            p = (np.array(vertices)-center)@transform.T
            p[:,:2] *= scale; p[:,:2] += [width/2,height/2]
            a,b,c,d = e['faces'][s]['uv']
            uv = np.array([[a,b],[c,b],[c,d],[a,d]])
            for indexes in ((0,1,2),(0,2,3)):
                q=p[list(indexes)]; t=uv[list(indexes)]
                lo=np.maximum(np.floor(q[:,:2].min(0)).astype(int),0)
                hi=np.minimum(np.ceil(q[:,:2].max(0)).astype(int),[width-1,height-1])
                if np.any(lo>hi): continue
                xx,yy=np.meshgrid(np.arange(lo[0],hi[0]+1)+.5,np.arange(lo[1],hi[1]+1)+.5)
                det=(q[1,1]-q[2,1])*(q[0,0]-q[2,0])+(q[2,0]-q[1,0])*(q[0,1]-q[2,1])
                if abs(det)<1e-8: continue
                w0=((q[1,1]-q[2,1])*(xx-q[2,0])+(q[2,0]-q[1,0])*(yy-q[2,1]))/det
                w1=((q[2,1]-q[0,1])*(xx-q[2,0])+(q[0,0]-q[2,0])*(yy-q[2,1]))/det
                w2=1-w0-w1
                zz=w0*q[0,2]+w1*q[1,2]+w2*q[2,2]
                u=np.clip((w0*t[0,0]+w1*t[1,0]+w2*t[2,0]).astype(int),0,255)
                v=np.clip((w0*t[0,1]+w1*t[1,1]+w2*t[2,1]).astype(int),0,255)
                col=texture[v,u].copy();col[:,:,:3]=np.clip(col[:,:,:3]*shade,0,255).astype(np.uint8)
                region=np.s_[lo[1]:hi[1]+1,lo[0]:hi[0]+1]
                mask=(w0>=-1e-6)&(w1>=-1e-6)&(w2>=-1e-6)&(zz>depth[region])&(col[:,:,3]>0)
                depth[region][mask]=zz[mask];pixels[region][mask]=col[mask]
    return Image.fromarray(pixels)


for builder in (white,night):
    validate(builder)
    builder.save_box_uv()
canvas=Image.new('RGB',(1560,1100),(28,33,41))
draw=ImageDraw.Draw(canvas)
font=ImageFont.truetype('C:/Windows/Fonts/arial.ttf',30)
small=ImageFont.truetype('C:/Windows/Fonts/arial.ttf',20)
for i,builder in enumerate((white,night)):
    ox=20+i*780
    draw.rounded_rectangle((ox,20,ox+760,1080),radius=20,fill=(42,49,59))
    draw.text((ox+30,45),'WHITE-HOT' if i==0 else 'NIGHT VISION',font=font,fill=(230,237,244))
    draw.text((ox+30,90),'Textured model / front + perspective',font=small,fill=(161,174,188))
    canvas.paste(render(builder,0,0),(ox+10,120),render(builder,0,0))
    r=render(builder,.62,.34)
    canvas.paste(r,(ox+10,580),r)
canvas.save(ROOT/'thermal_variants_preview.png')
print('Saved models, ON/OFF texture atlases and geometry preview in',ROOT)
