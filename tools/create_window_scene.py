#!/usr/bin/env python3
"""Arrange licensed Kenney city models and original airport/cloud geometry.
Source asset hashes and distribution revision: docs/scenery-provenance.json.
The source folder is a local build cache, not a runtime network dependency.
"""
import copy
import hashlib
import json
import math
import random
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'android/build/local-validation/asset-research/city'
binary = bytearray()
g = dict(asset=dict(version='2.0', generator='Focus Flight window scenery'), scene=0,
         scenes=[dict(nodes=[])], nodes=[], meshes=[], materials=[], accessors=[], bufferViews=[],
         textures=[], images=[], samplers=[])
roots = g['scenes'][0]['nodes']


def align():
    binary.extend(b'\0' * ((-len(binary)) % 4))


def import_model(path):
    b = path.read_bytes()
    count = struct.unpack_from('<I', b, 12)[0]
    src = json.loads(b[20:20+count])
    chunk_start = 20 + count
    bin_size = struct.unpack_from('<I', b, chunk_start)[0]
    align()
    start = len(binary)
    binary.extend(b[chunk_start+8:chunk_start+8+bin_size])
    offsets = {key: len(g[key]) for key in g if isinstance(g[key], list)}
    for view in src['bufferViews']:
        v = copy.deepcopy(view)
        v['buffer'] = 0
        v['byteOffset'] = start + v.get('byteOffset', 0)
        g['bufferViews'].append(v)
    for acc in src['accessors']:
        a = copy.deepcopy(acc)
        a['bufferView'] += offsets['bufferViews']
        g['accessors'].append(a)
    for image in src.get('images', []):
        image = copy.deepcopy(image)
        image['bufferView'] += offsets['bufferViews']
        g['images'].append(image)
    g['samplers'].extend(src.get('samplers', []))
    for texture in src.get('textures', []):
        t = copy.deepcopy(texture)
        t['source'] += offsets['images']
        if 'sampler' in t:
            t['sampler'] += offsets['samplers']
        g['textures'].append(t)
    def texture_refs(value):
        if isinstance(value, dict):
            for key, child in value.items():
                if key.endswith('Texture') and isinstance(child, dict) and 'index' in child:
                    child['index'] += offsets['textures']
                else:
                    texture_refs(child)
    for material in src.get('materials', []):
        m = copy.deepcopy(material)
        texture_refs(m)
        g['materials'].append(m)
    for mesh in src['meshes']:
        mesh = copy.deepcopy(mesh)
        for primitive in mesh['primitives']:
            primitive['attributes'] = {key: value+offsets['accessors'] for key, value in primitive['attributes'].items()}
            if 'indices' in primitive:
                primitive['indices'] += offsets['accessors']
            if 'material' in primitive:
                primitive['material'] += offsets['materials']
        g['meshes'].append(mesh)
    assert len(src['nodes']) == 1 and 'mesh' in src['nodes'][0]
    return src['nodes'][0]['mesh'] + offsets['meshes']


def material(name, color, unlit=False):
    m = dict(name=name, pbrMetallicRoughness=dict(baseColorFactor=[*color, 1], roughnessFactor=.85, metallicFactor=0))
    if unlit:
        m['extensions'] = dict(KHR_materials_unlit={})
        g['extensionsUsed'] = ['KHR_materials_unlit']
    g['materials'].append(m)
    return len(g['materials'])-1


def accessor(values, kind='VEC3'):
    align()
    start = len(binary)
    for item in values:
        binary.extend(struct.pack('<3f', *item) if kind == 'VEC3' else struct.pack('<H', item))
    g['bufferViews'].append(dict(buffer=0, byteOffset=start, byteLength=len(binary)-start))
    a = dict(bufferView=len(g['bufferViews'])-1, componentType=5126 if kind == 'VEC3' else 5123,
             count=len(values), type=kind)
    if kind == 'VEC3':
        a.update(min=[min(p[i] for p in values) for i in range(3)], max=[max(p[i] for p in values) for i in range(3)])
    g['accessors'].append(a)
    return len(g['accessors'])-1


def mesh(vertices, normals, indices, mat):
    g['meshes'].append(dict(primitives=[dict(attributes=dict(POSITION=accessor(vertices), NORMAL=accessor(normals)),
                                           indices=accessor(indices, 'SCALAR'), material=mat)]))
    return len(g['meshes'])-1


def cube(mat):
    vertices, normals, indices = [], [], []
    for axis in range(3):
        for side in [-1, 1]:
            start = len(vertices)
            normal = [0, 0, 0]
            normal[axis] = side
            for a, b in [(-1,-1), (1,-1), (1,1), (-1,1)]:
                p = [0, 0, 0]
                p[axis] = side/2
                p[(axis+1) % 3] = a/2
                p[(axis+2) % 3] = b/2
                vertices.append(p)
                normals.append(normal)
            order = [0,1,2,0,2,3] if side == 1 else [0,2,1,0,3,2]
            indices.extend(start+i for i in order)
    return mesh(vertices, normals, indices, mat)


def sphere(mat):
    vertices, normals, indices = [], [], []
    for lat in range(19):
        phi = math.pi*lat/18
        for lon in range(33):
            theta = 2*math.pi*lon/32
            p = [math.sin(phi)*math.cos(theta), math.cos(phi), math.sin(phi)*math.sin(theta)]
            vertices.append(p)
            normals.append(p)
    for lat in range(18):
        for lon in range(32):
            a = lat*33+lon
            indices.extend([a,a+1,a+33,a+1,a+34,a+33])
    return mesh(vertices, normals, indices, mat)


def instance(model, pos, scale, name):
    g['nodes'].append(dict(mesh=model, translation=pos, scale=scale, name=name))
    roots.append(len(g['nodes'])-1)


models = [import_model(p) for p in sorted(SOURCE.glob('*.glb'))]
assert len(models) == 6, 'The six pinned city assets must be present.'
ground = cube(material('grass', [.36,.49,.31]))
asphalt = cube(material('asphalt', [.19,.22,.24]))
concrete = cube(material('apron', [.64,.65,.61]))
white = cube(material('runway markings', [.94,.94,.86]))
glass = cube(material('terminal glass', [.21,.41,.48]))
terminal = cube(material('terminal walls', [.78,.79,.70]))
cloud = sphere(material('sunlit cloud', [.94,.96,.98]))
instance(ground, [500,-2,0], [3500,3,7000], 'airport-and-city-ground')
instance(asphalt, [0,0,0], [48,.12,2500], 'runway')
instance(concrete, [110,-.02,0], [160,.12,1800], 'airport-apron')
for z in range(-1200,1201,75):
    instance(white, [0,.1,z], [1.2,.04,27], 'runway-center-mark')
for x in [-22,22]:
    instance(white, [x,.1,0], [.5,.04,2500], 'runway-edge')
for z in [-220,80,380]:
    instance(terminal, [170,13,z], [46,26,180], 'airport-terminal')
    instance(glass, [146.8,15,z], [.5,13,174], 'terminal-windows')
    for gate in range(-2,3):
        instance(terminal, [128,6,z+gate*30], [38,5,7], 'boarding-bridge')
instance(terminal, [220,43,390], [11,86,11], 'control-tower')
instance(glass, [220,87,390], [23,10,23], 'control-tower-glass')
random.seed(664877)
for x in range(320,1500,170):
    instance(asphalt, [x,-.1,0], [15,.1,4000], 'city-road')
    for z in range(-1800,1801,200):
        if abs(z) % 360 < 30:
            instance(asphalt, [800,-.08,z], [1600,.1,13], 'cross-street')
        model = random.choice(models)
        scale = random.uniform(22,38)
        instance(model, [x+random.uniform(26,48),0,z+random.uniform(-16,16)], [scale]*3, 'city-building')
# Broad banks of actual 3D puffs form a cloud layer above the city.
for x in range(-300,1801,400):
    for z in range(-2400,2401,400):
        cx=x+random.uniform(-80,80);cz=z+random.uniform(-70,70)
        for _ in range(3):
            instance(cloud,[cx+random.uniform(-55,55),220+random.uniform(-8,18),cz+random.uniform(-45,45)],
                     [random.uniform(85,125),random.uniform(35,65),random.uniform(75,120)], 'cloud-bank')
align()
g['buffers'] = [dict(byteLength=len(binary))]
encoded = json.dumps(g,separators=(',',':')).encode()
encoded += b' ' * ((-len(encoded)) % 4)
total = 12+8+len(encoded)+8+len(binary)
output = struct.pack('<III',0x46546c67,2,total)+struct.pack('<II',len(encoded),0x4e4f534a)+encoded
output += struct.pack('<II',len(binary),0x004e4942)+binary
path = ROOT/'android/app/src/main/assets/window_scene.glb'
path.write_bytes(output)
print(f'Bundled window scene: {len(output)} bytes, {len(roots)} scene objects.')
