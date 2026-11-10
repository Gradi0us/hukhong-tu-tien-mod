"""Them song dai kiem cong (Radahn) vao geo Sa Hat Vuong: bone sword_r/sword_l con cua claw_r/claw_l.
Texture: vung trong 196..255 x 196..255 tren 3 texture pha + glowmask (luoi phat sang mo)."""
import json
from PIL import Image
R = 'src/main/resources/assets/hukhong/'
GEO = R + 'geo/entity/sa_hat_vuong.geo.json'
TEX = R + 'textures/entity/'

def sword(name, parent, x):
    # hilt + 3 doan luoi xoay dan quanh truc x -> luoi cong
    cubes = [{"origin": [x - 1, 15, -40], "size": [2, 2, 6], "uv": [230, 200]},          # chuoi
             {"origin": [x - 3, 14.5, -41], "size": [6, 3, 1], "uv": [230, 210]}]        # chan kiem
    z, y = -41, 14
    for i, rot in enumerate([0, -10, -22]):
        cubes.append({"origin": [x - 0.5, y, z - 11], "size": [1, 5, 11], "pivot": [x, y + 2.5, z],
                      "rotation": [rot, 0, 0], "uv": [196, 198 + i * 18]})
        z -= 10.5; y += [1.8, 4.0, 0][i]
    return {"name": name, "parent": parent, "pivot": [x, 16, -38], "cubes": cubes}

g = json.load(open(GEO, encoding='utf-8'))
bones = [b for b in g['minecraft:geometry'][0]['bones'] if b['name'] not in ('sword_r', 'sword_l')]
bones += [sword('sword_r', 'claw_r', -12), sword('sword_l', 'claw_l', 12)]
g['minecraft:geometry'][0]['bones'] = bones
json.dump(g, open(GEO, 'w', encoding='utf-8'), indent=1)

def paint(im, blade, edge, hilt):
    for i in range(3):
        for x in range(196, 196 + 24):
            for y in range(198 + i * 18, 198 + i * 18 + 16):
                im.putpixel((x, y), edge if (y - 198 - i * 18) in (11, 12, 15) else blade)
    for x in range(230, 230 + 16):
        for y in range(200, 200 + 14): im.putpixel((x, y), hilt)

for f, blade, edge in [('sa_hat_vuong.png', (150, 120, 80, 255), (230, 200, 140, 255)),
                       ('sa_hat_vuong_p2.png', (110, 80, 150, 255), (200, 160, 255, 255)),
                       ('sa_hat_vuong_p3.png', (160, 70, 30, 255), (255, 170, 60, 255))]:
    im = Image.open(TEX + f).convert('RGBA'); paint(im, blade, edge, (60, 40, 25, 255)); im.save(TEX + f)
for f, c in [('sa_hat_vuong_glowmask.png', (230, 190, 120, 160)), ('sa_hat_vuong_p2_glowmask.png', (190, 140, 255, 200)),
             ('sa_hat_vuong_p3_glowmask.png', (255, 150, 50, 220))]:
    im = Image.open(TEX + f).convert('RGBA'); paint(im, (0, 0, 0, 0), c, (0, 0, 0, 0)); im.save(TEX + f)
print('ok')
