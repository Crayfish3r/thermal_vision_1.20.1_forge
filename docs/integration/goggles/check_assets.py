"""Validate source-to-Java geometry, PNG identity, motion and release packaging."""
from pathlib import Path
import base64, hashlib, json, re, struct, zipfile
from PIL import Image
root=Path(__file__).resolve().parents[3]
folder=root/'art/thermal_goggles'
bb=json.loads((folder/'thermal_goggles.bbmodel').read_text(encoding='utf8'))
java=(root/'src/main/java/com/yourname/thermalvision/client/model/ThermalGogglesModel.java').read_text()
cubes={c['uuid']:c for c in bb['elements']}
exported=re.findall(r'// ([^\n]+)\n\s*\.texOffs\((\d+), (\d+)\)\.addBox\(([^\n]+)',java)
assert len(exported)==len(cubes)==88
actual={name:(int(u),int(v),[float(n) for n in re.findall(r'(-?[\d.]+)F',numbers)]) for name,u,v,numbers in exported}
groups=[]
def walk(group):
    groups.append(group['name'])
    for child in group['children']:
        if isinstance(child,dict): walk(child);continue
        cube=cubes[child];a,b=cube['from'],cube['to'];o=group['origin']
        expected=[o[0]-b[0],o[1]-b[1],a[2]-o[2]]+[b[i]-a[i] for i in range(3)]+[cube['inflate']]
        u,v,coords=actual[cube['name']]
        assert [u,v]==cube['uv_offset'] and len(coords)==7
        assert all(abs(a-b)<1e-6 for a,b in zip(coords,expected)),cube['name']
        assert cube['export'] and cube['rotation']==[0,0,0] and cube['box_uv']
for group in bb['outliner']:walk(group)
assert len(groups)==8 and len(set(groups))==8
for group in groups: assert f'addOrReplaceChild("{group}"' in java
motion=json.loads((folder/'thermal_goggles.motion.json').read_text())
assert motion['provenance']['sha256']==hashlib.sha256((folder/'thermal_goggles.bbmodel').read_bytes()).hexdigest()
assert motion['pivot_java_head_local']==[0,-7.25,-5.6]
assert motion['raised_degrees_java']==[-100,0,0] and motion['lowered_degrees_java']==[0,0,0]
for clip in bb['animations']:
    assert motion['clips'][clip['name']]['duration_seconds']==clip['length']
    keys=next(iter(clip['animators'].values()))['keyframes']
    for key in keys:
        assert key['interpolation']=='bezier'
        assert key['bezier_left_value']==key['bezier_right_value']==[0,0,0]
        assert all(abs(t+clip['length']/3)<1e-8 for t in key['bezier_left_time'])
        assert all(abs(t-clip['length']/3)<1e-8 for t in key['bezier_right_time'])
images=[]
for state in ('on','off'):
    name=f'thermal_goggles_{state}.png'
    path=folder/name
    texture=next(t for t in bb['textures'] if t['name']==name)
    assert path.read_bytes()==base64.b64decode(texture['source'].split(',',1)[1])
    image=Image.open(path)
    assert image.mode=='RGBA' and image.size==(128,128)
    assert set(image.getchannel('A').getdata())=={0,255}
    images.append(image)
    resource=root/f'src/main/resources/assets/thermal_vision/textures/models/goggles/{name}'
    assert path.read_bytes()==resource.read_bytes()
assert images[0].getchannel('A').tobytes()==images[1].getchannel('A').tobytes()
assert sum(sum(p[:3]) for p in images[0].getdata())>sum(sum(p[:3]) for p in images[1].getdata())
jar=root/'build/libs/thermal_vision_v4.jar'
with zipfile.ZipFile(jar) as z:
    names=z.namelist()
    assert not any(n.startswith('top/theillusivec4/') or 'jarjar' in n or '/gogglesaudit/' in n or '/audit/' in n for n in names)
    for state in ('on','off'):
        name=f'thermal_goggles_{state}.png'
        assert z.read(f'assets/thermal_vision/textures/models/goggles/{name}')==(folder/name).read_bytes()
    for resource in ('data/curios/tags/items/head.json','data/thermal_vision/curios/slots/head.json',
                     'data/thermal_vision/curios/entities/players.json'):
        assert resource in names
    model=z.read('com/yourname/thermalvision/client/model/ThermalGogglesModel.class')
    assert struct.unpack('>H',model[6:8])[0]==61
report={'result':'PASS','source_cubes':88,'groups':groups,'geometry_tolerance_model_pixels':1e-6,
        'png_size':[128,128],'alpha':'identical binary transparency; embedded/source/resource/JAR bytes identical',
        'motion':'source Bezier clips exactly smoothstep, durations/pivot/endpoints verified',
        'release_java_major':61,'release_embedded_curios':False,'release_audit_classes':False,
        'jar_sha256':hashlib.sha256(jar.read_bytes()).hexdigest()}
(root/'docs/integration/goggles/asset_checks.json').write_text(json.dumps(report,indent=2))
print('ASSET CHECKS PASS: 88 source cubes/UV, 8 groups, PNG transparency/identity, motion, clean release JAR.')
