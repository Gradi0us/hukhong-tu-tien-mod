"""Thêm rìu lớn (bone 'great_axe', con của right_arm) vào hu_vuong.geo.json + vẽ vùng texture trống (x128-255, y96-140)
trên hu_vuong(.png/_p2/_p3) và glowmask tương ứng. Chạy lại an toàn (ghi đè bone + vùng)."""
import json, random
from PIL import Image

A = 'src/main/resources/assets/hukhong/'
GEO = A + 'geo/entity/hu_vuong.geo.json'
TEX = A + 'textures/entity/'

# (origin, size, uv) - mặt trước = -z; tay phải trục ở [-13,36,0], bàn tay ~y22
CUBES = [
    ([-14, 21, -26], [2, 2, 26], [128, 96]),   # cán rìu
    ([-14.5, 14, -29], [3, 14, 6], [190, 96]),  # lưỡi chính (đứng)
    ([-14, 12, -31], [2, 18, 2], [210, 96]),    # mép lưỡi magma
    ([-14.5, 20.5, 0], [3, 3, 3], [220, 96]),   # chuôi sọ
    ([-14, 23, -26], [2, 6, 4], [128, 130]),    # gai hư không sau lưỡi
]

def box_rects(uv, s):
    w, h, d = s; u, v = uv
    return (u, v, u + 2 * (d + w), v + d + h)

def geo():
    d = json.load(open(GEO, encoding='utf-8'))
    g = d['minecraft:geometry'][0]
    g['bones'] = [b for b in g['bones'] if b['name'] != 'great_axe']
    g['bones'].append({"name": "great_axe", "parent": "right_arm", "pivot": [-13, 22, 0],
                       "cubes": [{"origin": o, "size": s, "uv": uv} for o, s, uv in CUBES]})
    open(GEO, 'w', encoding='utf-8', newline='\n').write(json.dumps(d, indent=1))

PAL = {  # (cán, lưỡi, mép, sọ, gai) theo pha
    '': ((58, 36, 40), (52, 30, 70), (255, 110, 40), (225, 215, 190), (150, 70, 230)),
    '_p2': ((50, 28, 40), (60, 26, 84), (255, 90, 30), (215, 200, 175), (170, 80, 255)),
    '_p3': ((40, 18, 24), (70, 20, 30), (255, 150, 50), (240, 200, 160), (255, 90, 40)),
}

def paint(suffix):
    rnd = random.Random(7)
    im = Image.open(TEX + 'hu_vuong' + suffix + '.png').convert('RGBA')
    gm = Image.open(TEX + 'hu_vuong' + suffix + '_glowmask.png').convert('RGBA')
    for i, (o, s, uv) in enumerate(CUBES):
        x0, y0, x1, y1 = box_rects(uv, s)
        base = PAL[suffix][i]
        for x in range(x0, x1):
            for y in range(y0, y1):
                j = rnd.randint(-14, 14)
                im.putpixel((x, y), tuple(max(0, min(255, c + j)) for c in base) + (255,))
                glow = i == 2 or (i == 4 and (x + y) % 3 == 0) or (i == 1 and rnd.random() < 0.08)
                gm.putpixel((x, y), (PAL[suffix][2] if i != 4 else base) + (255,) if glow else (0, 0, 0, 0))
    im.save(TEX + 'hu_vuong' + suffix + '.png')
    gm.save(TEX + 'hu_vuong' + suffix + '_glowmask.png')

if __name__ == '__main__':
    geo()
    for sfx in PAL: paint(sfx)
