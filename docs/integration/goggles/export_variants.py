"""Export supplied Box-UV variants using the established Mojmaps conversion."""
from pathlib import Path
import json, math, shutil

ROOT = Path(__file__).resolve().parents[3]
JAVA = ROOT / 'src/main/java/com/yourname/thermalvision/client/model'
def f(value):
    return str(round(float(value), 6)) + 'F'

for variant in ('white_hot', 'night_vision'):
    name = 'thermal_goggles_' + variant
    source = ROOT / 'art/thermal_variants' / variant / 'modded_entity'
    model = json.loads((source / (name + '.bbmodel')).read_text(encoding='utf8'))
    assert model['meta']['model_format'] == 'modded_entity' and model['modded_entity_flip_y']
    cubes = {c['uuid']: c for c in model['elements']}
    used, lines = [], []
    def bone(g, parent=None):
        origin = g['origin']; p = parent['origin'] if parent else [0,24,0]
        offset = [p[0]-origin[0], p[1]-origin[1], origin[2]-p[2]]
        rotation = [-math.radians(g['rotation'][0]), -math.radians(g['rotation'][1]), math.radians(g['rotation'][2])]
        parent_name = parent['name'] if parent else 'root'
        lines.append(f'        PartDefinition {g["name"]} = {parent_name}.addOrReplaceChild("{g["name"]}", CubeListBuilder.create()')
        for child in g['children']:
            if not isinstance(child, str): continue
            c = cubes[child]; used.append(child)
            assert c['export'] and c['box_uv'] and c['rotation'] == [0,0,0] and not c['mirror_uv']
            a,b = c['from'],c['to']
            pos = [origin[0]-b[0], origin[1]-b[1], a[2]-origin[2]]
            size = [b[i]-a[i] for i in range(3)]
            u,v = c['uv_offset']
            lines.append(f'            // {c["name"]}')
            lines.append(f'            .texOffs({int(u)}, {int(v)}).addBox({", ".join(f(t) for t in pos+size)}, new CubeDeformation({f(c["inflate"])}))')
        pose = 'PartPose.offsetAndRotation' if any(rotation) else 'PartPose.offset'
        lines.append(f'            , {pose}({", ".join(f(t) for t in (offset+rotation if any(rotation) else offset))}));')
        for child in g['children']:
            if isinstance(child,dict): bone(child,g)
    for group in model['outliner']: bone(group)
    assert sorted(used) == sorted(cubes)
    cls = 'WhiteHotGeometry' if variant == 'white_hot' else 'NightVisionGeometry'
    text = '''package com.yourname.thermalvision.client.model;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
/** Generated from the supplied modded_entity bbmodel by export_variants.py. */
public final class CLASS {
    private CLASS() {}
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
'''.replace('CLASS',cls) + '\n'.join(lines) + '''
        for (String name : new String[]{"hat", "body", "left_arm", "right_arm", "left_leg", "right_leg"})
            root.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.ZERO);
        return LayerDefinition.create(mesh, WIDTH, HEIGHT);
    }
}
'''.replace('WIDTH',str(model['resolution']['width'])).replace('HEIGHT',str(model['resolution']['height']))
    (JAVA / (cls + '.java')).write_text(text,encoding='utf8')
    for state in ('on','off'):
        shutil.copyfile(source / f'{name}_{state}.png', ROOT / f'src/main/resources/assets/thermal_vision/textures/models/goggles/{name}_{state}.png')
    print(name, len(used), 'cubes exported')
