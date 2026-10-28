#!/usr/bin/env python3
"""Create an original, small glTF aircraft mesh for the cartographic flight marker."""
import json
import math
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
triangles = [[], [], []]


def triangle(a, b, c, material=0):
    u = [b[i] - a[i] for i in range(3)]
    v = [c[i] - a[i] for i in range(3)]
    n = [u[1]*v[2] - u[2]*v[1], u[2]*v[0] - u[0]*v[2], u[0]*v[1] - u[1]*v[0]]
    size = math.sqrt(sum(x*x for x in n))
    if size < 1e-10:
        return
    n = [x / size for x in n]
    triangles[material].extend((p, n) for p in [a, b, c])


def fuselage(x, y, rings, material=0, sides=20):
    for (z1, r1), (z2, r2) in zip(rings, rings[1:]):
        for i in range(sides):
            a, b = i*2*math.pi/sides, (i+1)*2*math.pi/sides
            p = [(x+math.cos(t)*r, y+math.sin(t)*r*.85, z) for t, r, z in
                 [(a,r1,z1),(b,r1,z1),(b,r2,z2),(a,r2,z2)]]
            triangle(p[0], p[2], p[1], material)
            triangle(p[0], p[3], p[2], material)


def wing(polygon, low, high, material=0):
    for i in range(1, len(polygon)-1):
        triangle((polygon[0][0], high, polygon[0][1]), (polygon[i][0], high, polygon[i][1]),
                 (polygon[i+1][0], high, polygon[i+1][1]), material)
        triangle((polygon[0][0], low, polygon[0][1]), (polygon[i+1][0], low, polygon[i+1][1]),
                 (polygon[i][0], low, polygon[i][1]), material)
    for a, b in zip(polygon, polygon[1:] + polygon[:1]):
        triangle((a[0], low, a[1]), (b[0], low, b[1]), (b[0], high, b[1]), material)
        triangle((a[0], low, a[1]), (b[0], high, b[1]), (a[0], high, a[1]), material)


fuselage(0, 0, [(-16,0),(-14,1.0),(-11,1.7),(-7,1.85),(6,1.65),(12,.7),(15,0)])
for side in [-1, 1]:
    wing([(side*1.3,-5),(side*17,3),(side*17,5),(side*1.3,3)], -.25, .05)
    wing([(side*1,9),(side*6.5,12),(side*6.5,14),(side*.8,12)], .5, .7)
    wing([(side*15,2),(side*17,3),(side*17,5),(side*15,4)], .06, .11, 2)
    fuselage(side*6, -1.1, [(-1,.8),(0,.9),(3,.9),(4,.5)])
    fuselage(side*6, -1.1, [(-1.05,.72),(-1.06,0)], 1)
    # Cockpit panes, independently authored geometry.
    triangle((side*.1,1.22,-12.2),(side*1.15,1.05,-11.1),(side*1.28,1.4,-8.9),1)
    triangle((side*.1,1.22,-12.2),(side*1.28,1.4,-8.9),(side*.1,1.57,-9.1),1)
for x in [-.13,.13]:
    triangle((x,.8,8),(x,5.4,12),(x,1.0,14.6),2)
triangle((-.13,5.4,12),(.13,5.4,12),(.13,1,14.6),2)
triangle((-.13,5.4,12),(.13,1,14.6),(-.13,1,14.6),2)

binary = bytearray()
views, accessors, primitives = [], [], []


def accessor(values, normals=False):
    while len(binary) % 4:
        binary.append(0)
    start = len(binary)
    for value in values:
        binary.extend(struct.pack('<3f', *value))
    views.append(dict(buffer=0, byteOffset=start, byteLength=len(binary)-start, target=34962))
    data = dict(bufferView=len(views)-1, componentType=5126, count=len(values), type='VEC3')
    if not normals:
        data.update(min=[min(p[i] for p in values) for i in range(3)],
                    max=[max(p[i] for p in values) for i in range(3)])
    accessors.append(data)
    return len(accessors)-1


for material, vertices in enumerate(triangles):
    if vertices:
        positions = accessor([v[0] for v in vertices])
        normals = accessor([v[1] for v in vertices], True)
        # Explicit 16-bit indices keep the mesh compatible with the native renderer.
        while len(binary) % 4:
            binary.append(0)
        start = len(binary)
        for index in range(len(vertices)):
            binary.extend(struct.pack('<H', index))
        views.append(dict(buffer=0, byteOffset=start, byteLength=len(binary)-start, target=34963))
        accessors.append(dict(bufferView=len(views)-1, componentType=5123, count=len(vertices),
                              type='SCALAR', min=[0], max=[len(vertices)-1]))
        primitives.append(dict(attributes=dict(POSITION=positions, NORMAL=normals), indices=len(accessors)-1,
                               material=material, mode=4))

colors = [[.91,.93,.9,1],[.025,.07,.09,1],[.94,.83,.32,1]]
document = dict(asset=dict(version='2.0', generator='Focus Flight original procedural aircraft'),
                scene=0, scenes=[dict(nodes=[0])], nodes=[dict(mesh=0)], meshes=[dict(primitives=primitives)],
                materials=[dict(pbrMetallicRoughness=dict(baseColorFactor=c, metallicFactor=.15,
                                roughnessFactor=.45), doubleSided=True) for c in colors],
                buffers=[dict(byteLength=len(binary))], bufferViews=views, accessors=accessors)
encoded = json.dumps(document, separators=(',',':')).encode()
encoded += b' ' * ((-len(encoded)) % 4)
binary += b'\0' * ((-len(binary)) % 4)
total = 12 + 8 + len(encoded) + 8 + len(binary)
output = struct.pack('<III',0x46546C67,2,total) + struct.pack('<II',len(encoded),0x4E4F534A) + encoded
output += struct.pack('<II',len(binary),0x004E4942) + binary
path = ROOT/'android/app/src/main/assets/aircraft.glb'
path.write_bytes(output)
assert len(output) == total and all(v['count'] % 3 == 0 for v in accessors)
print(f'Original glTF aircraft written: {total} bytes, {sum(len(t) for t in triangles)//3} triangles.')
