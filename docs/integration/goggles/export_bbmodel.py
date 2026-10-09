"""Export the supplied bbmodel into the 1.20.1 Mojmaps ModelPart API.
No replacement geometry. Also reconstruct the previously missing movement manifest.
"""
from pathlib import Path
import hashlib, json, math, shutil

ROOT = Path(__file__).resolve().parents[3]
SOURCE = ROOT / 'art/thermal_goggles/thermal_goggles.bbmodel'
model = json.loads(SOURCE.read_text(encoding='utf8'))
assert model['meta']['model_format'] == 'modded_entity'
assert model['modded_entity_flip_y']
assert model['resolution'] == {'width': 128, 'height': 128}
cubes = {c['uuid']: c for c in model['elements']}
assert len(cubes) == 88
used = []
lines = []
def f(n):
    s = f'{float(n):.6f}'.rstrip('0').rstrip('.')
    if s in ('-0', ''): s = '0'
    if '.' not in s: s += '.0'
    return s + 'F'

def bone(g, parent=None):
    name = g['name']; origin = g['origin']
    p = parent['origin'] if parent else [0,24,0]
    offset = [p[0]-origin[0], p[1]-origin[1], origin[2]-p[2]]
    rotation = [-math.radians(g['rotation'][0]), -math.radians(g['rotation'][1]), math.radians(g['rotation'][2])]
    parent_name = parent['name'] if parent else 'root'
    lines.append(f'        PartDefinition {name} = {parent_name}.addOrReplaceChild("{name}", CubeListBuilder.create()')
    for child in g['children']:
        if not isinstance(child, str): continue
        c = cubes[child]; used.append(child)
        assert c['export'] and c['box_uv'] and c['rotation'] == [0,0,0]
        assert not c['mirror_uv']
        a,b = c['from'],c['to']
        pos = [origin[0]-b[0], origin[1]-b[1], a[2]-origin[2]]
        size = [b[i]-a[i] for i in range(3)]
        u,v = c['uv_offset']
        lines.append(f'            // {c["name"]}')
        lines.append(f'            .texOffs({u}, {v}).addBox({", ".join(f(t) for t in pos+size)}, new CubeDeformation({f(c["inflate"])}))')
    pose = 'PartPose.offsetAndRotation' if any(rotation) else 'PartPose.offset'
    args = offset+rotation if any(rotation) else offset
    lines.append(f'            , {pose}({", ".join(f(t) for t in args)}));')
    for child in g['children']:
        if isinstance(child,dict): bone(child,g)

for g in model['outliner']: bone(g)
assert sorted(used)==sorted(cubes), 'Dropped or duplicate geometry'
java = '''package com.yourname.thermalvision.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.yourname.thermalvision.ThermalVision;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/** Geometry exported directly from art/thermal_goggles/thermal_goggles.bbmodel.
 * Empty humanoid parts adapt the same head geometry to Forge armor and Curios.
 * Regenerate with docs/integration/goggles/export_bbmodel.py.
 */
public final class ThermalGogglesModel extends HumanoidModel<LivingEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
        new ResourceLocation(ThermalVision.MOD_ID, "thermal_goggles"), "main");
    private final ModelPart gogglesHinge;
    private boolean renderEnabled;

    public ThermalGogglesModel(ModelPart root) {
        super(root);
        gogglesHinge = head.getChild("goggles_hinge");
    }

    public void applyPose(HumanoidModel<?> wearer, float hingeAngle, boolean visible) {
        head.copyFrom(wearer.head);
        head.visible = true;
        young = wearer.young;
        crouching = wearer.crouching;
        riding = wearer.riding;
        gogglesHinge.xRot = hingeAngle;
        renderEnabled = visible;
    }

    public float hingeAngle() { return gogglesHinge.xRot; }

    @Override
    public void renderToBuffer(PoseStack poses, VertexConsumer vertices, int light, int overlay,
                               float red, float green, float blue, float alpha) {
        if (renderEnabled) super.renderToBuffer(poses, vertices, light, overlay, red, green, blue, alpha);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
'''
java += '\n'.join(lines)
java += '''
        // HumanoidModel's constructor requires these nodes; they contain no geometry.
        for (String name : new String[]{"hat", "body", "left_arm", "right_arm", "left_leg", "right_leg"}) {
            root.addOrReplaceChild(name, CubeListBuilder.create(), PartPose.ZERO);
        }
        return LayerDefinition.create(mesh, 128, 128);
    }
}
'''
target = ROOT/'src/main/java/com/yourname/thermalvision/client/model/ThermalGogglesModel.java'
target.parent.mkdir(parents=True,exist_ok=True)
target.write_text(java,encoding='utf8')

clips = {}
for a in model['animations']:
    bone_anim = next(iter(a['animators'].values()))
    assert bone_anim['name']=='goggles_hinge'
    keys=[]
    for k in bone_anim['keyframes']:
        assert k['interpolation']=='bezier'
        assert k['bezier_left_value']==k['bezier_right_value']==[0,0,0]
        keys.append({'time_seconds':k['time'], 'rotation_degrees_java':[-float(k['data_points'][0]['x']),0,0]})
    clips[a['name']]={'duration_seconds':a['length'],'keys':keys,'easing':'smoothstep(t)=3*t*t-2*t*t*t'}
assert clips['equip_lower']['duration_seconds']==.25
assert clips['unequip_raise']['duration_seconds']==.30
motion = {
    'schema':'thermal_vision:goggles_motion','schema_version':1,
    'provenance':{'reconstructed_from':'thermal_goggles.bbmodel','sha256':hashlib.sha256(SOURCE.read_bytes()).hexdigest(),
        'note':'Input Java export and motion.json were absent. Geometry and original clips extracted from the supplied bbmodel; reversal policy added for integration.'},
    'units':{'translation':'model pixels; 16 per block','angles':'degrees in this document; radians in ModelPart','time':'seconds'},
    'coordinates':{'java':'+Y down, front -Z; head local origin [0,0,0]',
        'blockbench':'Y up; head origin [0,24,0]', 'point_conversion':'Java=[-BB.x,24-BB.y,BB.z]',
        'rotation_conversion':'Java degrees=[-BB.x,-BB.y,BB.z]'},
    'animated_path':'head/goggles_hinge','pivot_java_head_local':[0,-7.25,-5.6],
    'base_transform':{'translation':[0,-7.25,-5.6],'rotation_degrees':[0,0,0],'scale':[1,1,1]},
    'lowered_degrees_java':[0,0,0],'raised_degrees_java':[-100,0,0], 'clips':clips,
    'runtime_blend':{'type':'single scalar absolute local X rotation; never add two clip rotations',
        'clock':'client level gameTime plus partial tick, divided by 20; paused world freezes',
        'initial_tracking':'snap to confirmed endpoint; subsequent toggles animate',
        'reverse':'sample current angle and angular velocity; never reset to clip start',
        'brake':'if moving away from new target, linearly reduce angular velocity to zero for at most 0.025s; shorten to avoid endpoint overshoot',
        'continue':'cubic Hermite to target with preserved initial velocity and zero final velocity',
        'duration':'nominal clip duration * max(0.2, abs(remaining angle)/100 degrees); cap to 3*abs(remaining angle)/abs(initial velocity) when moving toward target',
        'position_continuity':'C1 at reversal, no overshoot; at rest Hermite equals exact source smoothstep'}
}
manifest = ROOT/'art/thermal_goggles/thermal_goggles.motion.json'
manifest.write_text(json.dumps(motion,indent=2),encoding='utf8')
resources=ROOT/'src/main/resources/assets/thermal_vision'
(resources/'models/entity').mkdir(parents=True,exist_ok=True)
shutil.copy2(manifest,resources/'models/entity/thermal_goggles.motion.json')
(resources/'textures/models/goggles').mkdir(parents=True,exist_ok=True)
for state in ['on','off']:
    shutil.copy2(ROOT/f'art/thermal_goggles/thermal_goggles_{state}.png',resources/f'textures/models/goggles/thermal_goggles_{state}.png')
print('Exported 88 exact cubes, reconstructed motion manifest and copied original PNGs.')
