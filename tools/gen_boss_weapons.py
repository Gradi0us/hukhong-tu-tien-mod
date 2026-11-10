"""Add hand-held weapon bones (parented to arm bones) to boss geckolib models and paint their box-UV
footprints into a free (transparent) area of every texture variant (+ glowmask for glowing parts).
Idempotent: skips a model whose weapon bone already exists.  Run from repo root: python tools/gen_boss_weapons.py"""
import json, math, os, random
from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources', 'assets', 'hukhong')
random.seed(7)

# cube: (origin, size, dark, light, glow[, (pivot, rotation)])
WOOD, WOOD_L = (58, 38, 24), (92, 62, 38)
GOLD, GOLD_L = (130, 100, 40), (210, 175, 90)
STEEL, STEEL_L = (90, 88, 92), (175, 172, 168)
WEAPONS = {
    # Godfrey: great axe, haft through the gauntlet, head forward (-z)
    'chg_1': ('weapon_axe', 'right_arm', [-18.5, 8, -7.5], [
        ([-20, 0, -9], [3, 48, 3], WOOD, WOOD_L, False),
        ([-20.5, 48, -9.5], [4, 3, 4], GOLD, GOLD_L, False),
        ([-19.5, 30, -23], [2, 18, 14], STEEL, STEEL_L, False),
        ([-19.5, 28, -25], [2, 22, 2], GOLD, GOLD_L, False),
        ([-19.5, 36, -6], [2, 6, 6], STEEL, STEEL_L, False),
    ]),
    # Maliketh: black blade, forward + slight curve, purple-black glowing edge
    'chg_2': ('weapon_black_blade', 'right_arm', [-7, 21, 0], [
        ([-8, 20, -3], [2, 2, 6], (30, 22, 30), (55, 42, 50), False),
        ([-9.5, 19.5, -4], [5, 3, 1], (40, 30, 50), (80, 60, 100), False),
        ([-7.5, 19, -20], [1, 4, 16], (14, 10, 18), (36, 26, 44), False),
        ([-7.75, 18.5, -20], [2, 1, 16], (110, 40, 190), (190, 110, 255), True),
        ([-7.5, 19, -34], [1, 4, 14], (14, 10, 18), (36, 26, 44), False, ([-7, 21, -20], [14, 0, 0])),
        ([-7.75, 18.5, -34], [2, 1, 14], (110, 40, 190), (190, 110, 255), True, ([-7, 21, -20], [14, 0, 0])),
    ]),
    # Morgott: cane in the left hand (sword in right hand already exists as bone 'blade')
    'chg_3': ('weapon_cane', 'left_arm', [6.5, 22, 0], [
        ([5.5, 1, -1], [2, 23, 2], WOOD, WOOD_L, False),
        ([5.5, 24, -3], [2, 2, 5], GOLD, GOLD_L, False),
        ([5.5, 0, -1], [2, 1, 2], GOLD, GOLD_L, False),
    ]),
    # Mohg: blood trident, shaft in front of the arm, red glowing prongs
    'chg_4': ('weapon_trident', 'right_arm', [-10.5, 19, -3.5], [
        ([-11.5, 0, -4.5], [2, 56, 2], (60, 8, 10), (110, 20, 22), False),
        ([-15.5, 56, -4.5], [10, 2, 2], (120, 14, 14), (190, 40, 30), False),
        ([-11.5, 58, -4.5], [2, 12, 2], (170, 20, 20), (255, 70, 50), True),
        ([-15.5, 58, -4.5], [2, 9, 2], (170, 20, 20), (255, 70, 50), True),
        ([-7.5, 58, -4.5], [2, 9, 2], (170, 20, 20), (255, 70, 50), True),
    ]),
    # Malenia: katana in the free (left) hand; right hand already holds the crescent staff
    'da_khuyet': ('weapon_katana', 'left_arm', [15, 45, 0], [
        ([14, 40, -1], [2, 10, 2], (100, 24, 26), (150, 44, 40), False),
        ([12, 39, -3], [6, 1, 6], GOLD, GOLD_L, False),
        ([14.5, 3, -1.5], [1, 36, 3], (150, 155, 165), (230, 235, 245), False),
    ]),
}


def fp(size):
    w, h, d = (math.ceil(v) for v in size)
    return 2 * d + 2 * w, d + h


def find_free(occ, bw, bh, TW, TH, name):
    for y in range(TH - bh + 1):
        for x in range(TW - bw + 1):
            if all(not occ[yy][xx] for yy in range(y, y + bh) for xx in range(x, x + bw)):
                return x, y
    raise SystemExit(name + ': no free texture area for %dx%d' % (bw, bh))


def run(name, spec):
    gpath = os.path.join(ROOT, 'geo', 'entity', name + '.geo.json')
    data = json.load(open(gpath, encoding='utf-8'))
    geo = data['minecraft:geometry'][0]
    bone_name, parent, pivot, cubes = spec
    if any(b['name'] == bone_name for b in geo['bones']):
        print(name, 'already has', bone_name); return
    assert any(b['name'] == parent for b in geo['bones']), parent
    tdir = os.path.join(ROOT, 'textures', 'entity')
    variants = [name, name + '_p2', name + '_p3']
    paths = [os.path.join(tdir, v + '.png') for v in variants if os.path.exists(os.path.join(tdir, v + '.png'))]
    gpaths = [p[:-4] + '_glowmask.png' for p in paths]
    imgs = [Image.open(p).convert('RGBA') for p in paths]
    gimgs = [Image.open(p).convert('RGBA') if os.path.exists(p) else None for p in gpaths]
    TW, TH = geo['description']['texture_width'], geo['description']['texture_height']
    occ = [[False] * TW for _ in range(TH)]
    for im in imgs + [g for g in gimgs if g]:
        a = im.getchannel('A').load()
        for yy in range(TH):
            row = occ[yy]
            for xx in range(TW):
                if a[xx, yy]:
                    row[xx] = True
    out = []
    for c in cubes:
        o, s, dark, light, glow = c[:5]
        bw, bh = fp(s)
        x, y = find_free(occ, bw, bh, TW, TH, name)
        for k, im in enumerate(imgs):
            px = im.load()
            gp = gimgs[k].load() if gimgs[k] else None
            for yy in range(bh):
                for xx in range(bw):
                    t = (yy / max(1, bh - 1)) * 0.5 + random.random() * 0.5
                    col = tuple(int(dark[i] + (light[i] - dark[i]) * t) for i in range(3)) + (255,)
                    px[x + xx, y + yy] = col
                    if glow and gp is not None:
                        gp[x + xx, y + yy] = col
        cube = {'origin': o, 'size': s, 'uv': [x, y]}
        if len(c) > 5:
            cube['pivot'], cube['rotation'] = c[5]
        out.append(cube)
        for yy in range(y, y + bh):
            for xx in range(x, x + bw):
                occ[yy][xx] = True
    bones = geo['bones']
    idx = max(i for i, b in enumerate(bones) if b['name'] == parent or b.get('parent') == parent)
    bones.insert(idx + 1, {'name': bone_name, 'parent': parent, 'pivot': pivot, 'cubes': out})
    for p, im in zip(paths, imgs):
        im.save(p)
    for p, g in zip(gpaths, gimgs):
        if g:
            g.save(p)
    json.dump(data, open(gpath, 'w', encoding='utf-8'), indent=1, ensure_ascii=False)
    print(name, '+', bone_name, '->', parent, 'ok')


if __name__ == '__main__':
    for n, sp in WEAPONS.items():
        run(n, sp)
