"""Offline bbmodel authoring. No Blockbench UI used. Python + Pillow + numpy."""
from pathlib import Path
import base64,json,math,uuid
import numpy as np
from PIL import Image
OUT=Path(__file__).resolve().parents[1]
OUT.mkdir(parents=True,exist_ok=True)
NS=uuid.UUID('b924a44f-0740-4a47-b271-76f2c1406f2d')
def uid(s): return str(uuid.uuid5(NS,s))
P=np.array([0,-7.25,-5.6])
groups={}; cubes=[]
def group(n,parent=None,origin=(0,0,0),color=0):
    groups[n]=dict(name=n,uuid=uid(n),origin=list(origin),color=color,children=[])
    if parent: groups[parent]['children'].append(n)
def box(n,g,a,b,mat='carbon'):
    assert all(y>x for x,y in zip(a,b)),n
    c=dict(name=n,uuid=uid(n),group=g,a=a,b=b,material=mat)
    cubes.append(c); groups[g]['children'].append(c)
group('head')
group('fixed_mount','head',color=2)
group('strap','fixed_mount',color=2)
group('mount_hardware','fixed_mount',color=7)
group('goggles_hinge','head',P.tolist(),4)
for n,color in [('bridge',4),('left_optic',1),('right_optic',1)]:
    group(n,'goggles_hinge',P.tolist(),color)
box('strap_left','strap',[-5.65,-6.2,-5.65],[-5.15,-4.7,5.65],'webbing')
box('strap_right','strap',[5.15,-6.2,-5.65],[5.65,-4.7,5.65],'webbing')
box('strap_rear','strap',[-5.15,-6.2,5.15],[5.15,-4.7,5.65],'webbing')
box('brow_band','strap',[-5.15,-7.05,-5.9],[5.15,-6.1,-5.15],'olive')
for sign,side in [(-1,'left'),(1,'right')]:
    a,b=sorted([sign*5.65,sign*5.9])
    for n,z in enumerate([-4.6,-1.8,1.5,4.4]):
        box(f'{side}_band_keeper_{n}','strap',[a,-6.35,z-.28],[b,-4.55,z+.28],'rubber')
    a,b=sorted([sign*5.65,sign*6.0])
    box(f'{side}_mount_plate','mount_hardware',[a,-7.05,-3.6],[b,-4.6,-1.8],'olive')
    a,b=sorted([sign*6.0,sign*6.13])
    for n,y in enumerate([-6.5,-5.1]):
        box(f'{side}_mount_bolt_{n}','mount_hardware',[a,y-.2,-2.95],[b,y+.2,-2.55],'steel')
    a,b=sorted([sign*5.15,sign*5.82])
    box(f'{side}_brow_anchor','mount_hardware',[a,-7.1,-6.02],[b,-5.45,-5.15],'carbon')
box('rear_buckle_frame','strap',[-.85,-6.5,5.65],[.85,-4.4,6.05],'carbon')
box('rear_buckle_inset','strap',[-.48,-6.1,6.05],[.48,-4.8,6.15],'rubber')
box('rear_buckle_tab','strap',[-.26,-5.7,6.15],[.26,-5.2,6.24],'steel')
box('brow_rail','mount_hardware',[-3.9,-7.35,-6.0],[3.9,-6.85,-5.75],'rubber')
box('fixed_hinge_base','mount_hardware',[-1.22,-7.95,-6.15],[1.22,-6.5,-5.05],'carbon')
box('fixed_hinge_top_cap','mount_hardware',[-1,-8.15,-6.12],[1,-7.95,-5.2],'steel')
for sign,side in [(-1,'left'),(1,'right')]:
    a,b=sorted([sign*1.22,sign*1.48])
    box(f'{side}_hinge_axis_cap','mount_hardware',[a,-7.62,-5.97],[b,-6.88,-5.23],'steel')
box('fixed_hinge_front_plate','mount_hardware',[-.75,-7.65,-6.3],[.75,-6.95,-6.15],'olive')
box('hinge_moving_link','bridge',[-.63,-6.95,-6.52],[.63,-5.8,-6.18],'steel')
box('armored_bridge_core','bridge',[-.72,-6.35,-7.55],[.72,-3.6,-6.08],'carbon')
box('bridge_front_inset','bridge',[-.45,-5.6,-7.75],[.45,-4.55,-7.55],'rubber')
box('bridge_lower_fastener','bridge',[-.25,-4.3,-7.76],[.25,-3.88,-7.56],'steel')
box('controller_housing','bridge',[-1,-6.6,-8.13],[1,-5.6,-6.12],'carbon')
box('controller_armored_lid','bridge',[-.9,-6.75,-7.95],[.9,-6.6,-6.3],'olive')
box('controller_front_rim','bridge',[-.85,-6.48,-8.3],[.85,-5.77,-8.13],'steel')
box('controller_face','bridge',[-.61,-6.35,-8.43],[.61,-5.86,-8.3],'rubber')
box('controller_status_led','bridge',[-.19,-6.24,-8.52],[.19,-5.97,-8.43],'status')
CY=-4.4
for cx,side in [(-2.5,'left'),(2.5,'right')]:
    def optic(n,x0,x1,y0,y1,z0,z1,mat='carbon'):
        box(f'{side}_{n}',f'{side}_optic',[cx+x0,y0,z0],[cx+x1,y1,z1],mat)
    optic('barrel_vertical',-1.5,1.5,CY-1.9,CY+1.9,-8.94,-6.35)
    optic('barrel_horizontal',-1.95,1.95,CY-1.35,CY+1.35,-8.94,-6.35)
    optic('rear_eyecup',-1.55,1.55,CY-1.5,CY+1.5,-6.4,-5.98,'rubber')
    optic('top_armor',-1.3,1.3,CY-2,CY-1.55,-8.55,-6.22,'olive')
    optic('bottom_armor',-1.25,1.25,CY+1.55,CY+1.95,-8.45,-6.38,'olive')
    optic('left_armor',-2.02,-1.6,CY-1.05,CY+1.05,-8.42,-6.55,'olive')
    optic('right_armor',1.6,2.02,CY-1.05,CY+1.05,-8.42,-6.55,'olive')
    optic('left_seam',-2.1,-1.96,CY-.83,CY+.83,-7.86,-7.53,'rubber')
    optic('right_seam',1.96,2.1,CY-.83,CY+.83,-7.86,-7.53,'rubber')
    for sn,(x0,x1) in enumerate([(-2.11,-2.02),(2.02,2.11)]):
        for bn,y in enumerate([CY-.72,CY+.72]):
            optic(f'side_bolt_{sn}_{bn}',x0,x1,y-.18,y+.18,-7.32,-6.94,'steel')
    optic('rim_top',-1.1,1.1,CY-1.98,CY-1.53,-9.43,-8.8,'steel_dark')
    optic('rim_bottom',-1.1,1.1,CY+1.53,CY+1.98,-9.43,-8.8,'steel_dark')
    optic('rim_left',-1.98,-1.53,CY-1.1,CY+1.1,-9.43,-8.8,'steel_dark')
    optic('rim_right',1.53,1.98,CY-1.1,CY+1.1,-9.43,-8.8,'steel_dark')
    for tag,x0,x1 in [('left',-1.64,-1.1),('right',1.1,1.64)]:
        optic(f'rim_{tag}_upper',x0,x1,CY-1.64,CY-1.1,-9.43,-8.8,'steel_dark')
        optic(f'rim_{tag}_lower',x0,x1,CY+1.1,CY+1.64,-9.43,-8.8,'steel_dark')
    optic('rim_top_edge',-1.05,1.05,CY-2.03,CY-1.96,-9.35,-8.85,'steel')
    optic('lens_center',-1.08,1.08,CY-1.51,CY+1.51,-9.26,-8.94,'lens')
    optic('lens_left_shoulder',-1.51,-1.08,CY-1.08,CY+1.08,-9.26,-8.94,'lens_edge')
    optic('lens_right_shoulder',1.08,1.51,CY-1.08,CY+1.08,-9.26,-8.94,'lens_edge')
x=2;y=2;rh=0
for c in sorted(cubes,key=lambda c:-(c['b'][1]-c['a'][1]+c['b'][2]-c['a'][2])):
    w,h,d=np.array(c['b'])-c['a']; iw=math.ceil(2*(w+d))+2; ih=math.ceil(h+d)+2
    if x+iw>126: x=2;y+=rh+2;rh=0
    assert y+ih<=126,'atlas overflow'
    c['uv']=[x+1,y+1];c['island']=[x,y,x+iw,y+ih];x+=iw+2;rh=max(rh,ih)
palette={'carbon':(39,42,43),'rubber':(24,27,27),'olive':(83,87,61),'webbing':(64,69,47),
         'steel':(99,102,99),'steel_dark':(57,61,61),'lens':(201,61,8),'lens_edge':(165,40,6),'status':(175,79,15)}
textures={}
for state in ['on','off']:
    im=Image.new('RGBA',(128,128),(0,0,0,0))
    for n,c in enumerate(cubes):
        x0,y0,x1,y1=c['island']; col=palette[c['material']]
        if state=='off' and c['material'] in ['lens','lens_edge','status']:
            col={'lens':(91,37,13),'lens_edge':(63,27,15),'status':(62,39,22)}[c['material']]
        for yy in range(y0,y1):
            for xx in range(x0,x1):
                k=(xx*17+yy*29+n*11)%23; delta=5 if k==0 else (-4 if k==1 else 0)
                if c['material']=='webbing': delta=4 if (xx+yy)%2==0 else -3
                im.putpixel((xx,yy),tuple(max(0,min(255,t+delta)) for t in col)+(255,))
        if c['material']=='lens':
            w,h,d=np.array(c['b'])-c['a'];u,v=c['uv'];xx0,yy0=u+d,v+d
            for yy in range(math.floor(yy0),math.ceil(yy0+h)):
                for xx in range(math.floor(xx0),math.ceil(xx0+w)):
                    r=max(abs((xx+.5-xx0)/w-.5)*1.3,abs((yy+.5-yy0)/h-.5))
                    if state=='on': col=(255,190,41) if r<.22 else ((244,104,10) if r<.46 else (198,51,7))
                    else: col=(143,70,22) if r<.22 else ((105,43,14) if r<.46 else (73,28,13))
                    im.putpixel((xx,yy),col+(255,))
    im.save(OUT/f'thermal_goggles_{state}.png'); textures[state]=im
def bb(p): return [-p[0],24-p[1],p[2]]
def faces(c):
    w,h,d=np.array(c['b'])-c['a'];u,v=c['uv']
    rs={'east':[u,v+d,u+d,v+d+h],'west':[u+d+w,v+d,u+2*d+w,v+d+h],
        'north':[u+d,v+d,u+d+w,v+d+h],'south':[u+2*d+w,v+d,u+2*d+2*w,v+d+h],
        'up':[u+d+w,v+d,u+d,v],'down':[u+d+2*w,v,u+d+w,v+d]}
    return {f:dict(uv=[round(float(t),5) for t in r],texture=0) for f,r in rs.items()}
elements=[]
for c in cubes:
    a,b=c['a'],c['b']
    elements.append({'name':c['name'],'type':'cube','uuid':c['uuid'],'box_uv':True,'uv_offset':c['uv'],
        'from':[-b[0],24-b[1],a[2]],'to':[-a[0],24-a[1],b[2]],
        'origin':bb(groups[c['group']]['origin']),'rotation':[0,0,0],'autouv':0,
        'color':groups[c['group']]['color'],'inflate':0,'mirror_uv':False,'shade':True,
        'visibility':True,'export':True,'faces':faces(c)})
def tree(n):
    g=groups[n]
    return dict(name=n,uuid=g['uuid'],origin=bb(g['origin']),rotation=[0,0,0],
                color=g['color'],export=True,visibility=True,isOpen=True,
                children=[tree(ch) if isinstance(ch,str) else ch['uuid'] for ch in g['children']])
def anim(n,duration,start,end):
    keys=[]
    for t,a in [(0,start),(duration,end)]:
        keys.append(dict(uuid=uid(f'{n}:{t}'),channel='rotation',time=t,
            data_points=[dict(x=str(a),y='0',z='0')],interpolation='bezier',
            bezier_linked=False,bezier_left_time=[-duration/3]*3,bezier_right_time=[duration/3]*3,
            bezier_left_value=[0,0,0],bezier_right_value=[0,0,0]))
    return dict(uuid=uid(n),name=n,loop='hold',override=False,length=duration,snapping=100,
                animators={uid('goggles_hinge'):dict(name='goggles_hinge',type='bone',keyframes=keys)})
tex=[]
for i,state in enumerate(['on','off']):
    raw=(OUT/f'thermal_goggles_{state}.png').read_bytes()
    tex.append(dict(uuid=uid('texture_'+state),name=f'thermal_goggles_{state}.png',id=str(i),
        relative_path=f'thermal_goggles_{state}.png',mode='bitmap',saved=True,
        width=128,height=128,uv_width=128,uv_height=128,visible=True,render_mode='default',
        render_sides='auto',pbr_channel='color',particle=False,
        source='data:image/png;base64,'+base64.b64encode(raw).decode()))
model=dict(meta=dict(format_version='5.0',model_format='modded_entity',box_uv=True),
    name='thermal_goggles',model_identifier='ThermalGogglesModel',modded_entity_version='1.17',
    modded_entity_entity_class='LivingEntity',modded_entity_flip_y=True,
    resolution=dict(width=128,height=128),elements=elements,outliner=[tree('head')],
    textures=tex,animations=[anim('equip_lower',.25,100,0),anim('unequip_raise',.30,0,100)],
    animation_variable_placeholders='',credit='Thermal Vision / armored concept 03; authored offline')
(OUT/'thermal_goggles.bbmodel').write_text(json.dumps(model,indent=2),encoding='utf8')
def verts(c):
    a,b=c['a'],c['b']
    return np.array([[a[0],a[1],a[2]],[b[0],a[1],a[2]],[b[0],b[1],a[2]],[a[0],b[1],a[2]],
                     [a[0],a[1],b[2]],[b[0],a[1],b[2]],[b[0],b[1],b[2]],[a[0],b[1],b[2]]])
def rotation(angle):
    a=math.radians(-angle);co,si=math.cos(a),math.sin(a)
    return np.array([[1,0,0],[0,co,-si],[0,si,co]])
def moving(c): return c['group'] in ['bridge','left_optic','right_optic']
def points(c,angle):
    v=verts(c)
    return (v-P)@rotation(angle).T+P if moving(c) else v
F=[('north',[0,1,2,3],[0,0,-1]),('south',[5,4,7,6],[0,0,1]),
   ('west',[4,0,3,7],[-1,0,0]),('east',[1,5,6,2],[1,0,0]),
   ('up',[4,5,1,0],[0,-1,0]),('down',[3,2,6,7],[0,1,0])]
def render(angle=0,state='on',az=30,elev=18,size=720):
    az,el=math.radians(az),math.radians(elev)
    view=np.array([math.sin(az)*math.cos(el),math.sin(el),math.cos(az)*math.cos(el)])
    right=np.array([math.cos(az),0,-math.sin(az)]);up=np.cross(view,right)
    scale=size/23;center=np.array([0,5,1]);atlas=np.asarray(textures[state])
    yy,xx=np.mgrid[:size,:size];bg=(24+8*(1-yy/size)).astype(np.uint8)
    rgb=np.stack([bg,bg+3,bg+4],axis=-1).copy();depth=np.full((size,size),-np.inf)
    light=np.array([-.5,.8,.55]);light/=np.linalg.norm(light);rot=rotation(angle)
    for c in cubes:
        pp=points(c,angle)*np.array([1,-1,-1]);p0=pp-center
        pr=np.stack([size/2+p0@right*scale,size/2-p0@up*scale,p0@view],axis=1)
        w,h,d=np.array(c['b'])-c['a'];u,v=c['uv']
        rs={'north':[u+d,v+d,u+d+w,v+d+h],'south':[u+2*d+w,v+d,u+2*d+2*w,v+d+h],
            'west':[u,v+d,u+d,v+d+h],'east':[u+d+w,v+d,u+2*d+w,v+d+h],
            'up':[u+d,v,u+d+w,v+d],'down':[u+d+w,v,u+d+2*w,v+d]}
        for face,inds,n in F:
            normal=np.array(n,dtype=float)
            if moving(c): normal=rot@normal
            normal*=np.array([1,-1,-1])
            if normal@view<=0: continue
            shade=.64+.36*max(0,normal@light)
            if c['material'].startswith('lens') and state=='on':shade=.95
            u0,v0,u1,v1=rs[face];uv=np.array([[u0,v0],[u1,v0],[u1,v1],[u0,v1]]);p=pr[inds]
            for tri in [[0,1,2],[0,2,3]]:
                pt=p[tri];ut=uv[tri]
                x0=max(0,int(math.floor(pt[:,0].min())));x1=min(size,int(math.ceil(pt[:,0].max()))+1)
                y0=max(0,int(math.floor(pt[:,1].min())));y1=min(size,int(math.ceil(pt[:,1].max()))+1)
                if x0>=x1 or y0>=y1:continue
                gy,gx=np.mgrid[y0:y1,x0:x1];gx=gx+.5;gy=gy+.5
                den=(pt[1,1]-pt[2,1])*(pt[0,0]-pt[2,0])+(pt[2,0]-pt[1,0])*(pt[0,1]-pt[2,1])
                if abs(den)<1e-9:continue
                aa=((pt[1,1]-pt[2,1])*(gx-pt[2,0])+(pt[2,0]-pt[1,0])*(gy-pt[2,1]))/den
                b0=((pt[2,1]-pt[0,1])*(gx-pt[2,0])+(pt[0,0]-pt[2,0])*(gy-pt[2,1]))/den
                cc=1-aa-b0;zz=aa*pt[0,2]+b0*pt[1,2]+cc*pt[2,2]
                mask=(aa>=-1e-7)&(b0>=-1e-7)&(cc>=-1e-7)&(zz>depth[y0:y1,x0:x1])
                tx=np.clip(np.floor(aa*ut[0,0]+b0*ut[1,0]+cc*ut[2,0]).astype(int),0,127)
                ty=np.clip(np.floor(aa*ut[0,1]+b0*ut[1,1]+cc*ut[2,1]).astype(int),0,127)
                sample=atlas[ty,tx];mask&=sample[:,:,3]>0
                rgb[y0:y1,x0:x1][mask]=(sample[:,:,:3][mask]*shade).astype(np.uint8)
                depth[y0:y1,x0:x1][mask]=zz[mask]
    return Image.fromarray(rgb)
render().save(OUT/'preview.png')
render(100,'off').save(OUT/'preview_raised.png')
minimum=1000
for angle in np.linspace(0,100,1001):
    for c in cubes:
        pp=points(c,float(angle))
        if moving(c):
            assert pp[:,2].max()<-5,(c['name'],angle,pp[:,2].max())
            minimum=min(minimum,-5-pp[:,2].max())
        else:
            lo,hi=pp.min(0),pp.max(0)
            assert np.any(hi<=np.array([-5,-9,-5])) or np.any(lo>=np.array([5,1,5])),c['name']
for c,e in zip(cubes,elements):
    for f in e['faces'].values():
        assert all(0<=q<=128 for q in f['uv'])
        u0,v0,u1,v1=f['uv']
        for im in textures.values():
            assert im.getpixel((math.floor((u0+u1)/2),math.floor((v0+v1)/2)))[3]==255
for i,c in enumerate(cubes):
    a=c['island']
    for o in cubes[i+1:]:
        b=o['island']
        assert not(max(a[0],b[0])<min(a[2],b[2]) and max(a[1],b[1])<min(a[3],b[3]))
assert len(set(c['uuid'] for c in cubes))==len(cubes)
report=dict(authoring='offline; Blockbench not opened or UI-tested',cubes=len(cubes),groups=len(groups),
    texture_size=[128,128],embedded_textures=2,checks=['unique UUIDs','UV in atlas','opaque face samples',
    'no UV island overlaps','no reference geometry','1001 hinge positions clear vanilla helmet envelope'],
    minimum_moving_front_clearance_pixels=round(minimum,5),
    pivot_java=P.tolist(),pivot_blockbench=bb(P),raised_degrees_java=-100,raised_degrees_blockbench=100,
    limitation='Opening, animation playback in Blockbench and in-game rendering still need manual verification.')
(OUT/'validation.json').write_text(json.dumps(report,indent=2),encoding='utf8')
print(json.dumps(report,indent=2))

