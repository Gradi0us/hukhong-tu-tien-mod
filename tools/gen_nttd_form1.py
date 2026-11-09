"""Generate form-1 model (geo.json), animations and 128x128 textures for Nguyen Thuy Tich Diet (possessed cultivator).
Run from repo root: python tools/gen_nttd_form1.py"""
import json, random, os
from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources', 'assets', 'hukhong')
NAME = 'nttd_form1'
TW = TH = 128
random.seed(25)

# (bone, parent, pivot, [(origin, size, kind)])  - front of the model is -z (north), right hand is -x
BONES = [
    ('root', None, [0, 0, 0], []),
    ('right_leg', 'root', [-3.5, 18, 0], [([-6.5, 0, -3], [6, 18, 6], 'robe')]),
    ('left_leg', 'root', [3.5, 18, 0], [([0.5, 0, -3], [6, 18, 6], 'robe')]),
    ('body', 'root', [0, 18, 0], [([-8, 18, -4], [16, 16, 8], 'chest'),
                                  ([-8, 6, -5], [16, 12, 10], 'robe'),
                                  ([-11, 30, -5], [5, 6, 10], 'armor'),
                                  ([6, 30, -5], [5, 6, 10], 'armor')]),
    ('cape', 'body', [0, 34, 4], [([-7, 8, 4], [14, 26, 1], 'cape')]),
    ('head', 'body', [0, 34, 0], [([-4, 34, -4], [8, 9, 8], 'face'),
                                  ([-5, 43, -5], [10, 2, 10], 'hood'),
                                  ([-5, 34, 4], [10, 9, 1], 'hood'),
                                  ([-5, 34, -5], [1, 9, 9], 'hood'),
                                  ([4, 34, -5], [1, 9, 9], 'hood'),
                                  ([-3, 45, -1], [2, 5, 2], 'horn'),
                                  ([1, 45, -1], [2, 5, 2], 'horn')]),
    ('right_arm', 'body', [-10, 32, 0], [([-14, 18, -2], [5, 14, 5], 'armor')]),
    ('hammer', 'right_arm', [-11.5, 19, 0], [([-12.5, 2, -1], [2, 28, 2], 'handle'),
                                             ([-15.5, 28, -6], [8, 8, 12], 'hammer'),
                                             ([-12.5, 30, -9], [2, 4, 3], 'horn'),
                                             ([-12.5, 30, 6], [2, 4, 3], 'horn')]),
    ('left_arm', 'body', [10, 32, 0], [([9, 18, -2], [5, 14, 5], 'armor')]),
    ('void_core', 'body', [0, 26, -4], [([-2, 24, -5], [4, 4, 1], 'core')]),
]

cubes = [(b[0], o, s, k) for b in BONES for (o, s, k) in b[3]]
order = sorted(range(len(cubes)), key=lambda i: -(cubes[i][2][2] + cubes[i][2][1]))
uv = {}
x = y = shelf = 0
for i in order:
    w, h, d = cubes[i][2]
    bw, bh = 2 * d + 2 * w, d + h
    if x + bw > TW:
        x, y, shelf = 0, y + shelf, 0
    uv[i] = (x, y)
    x += bw
    shelf = max(shelf, bh)
assert y + shelf <= TH, 'texture too small'

PAL = {
    'robe': ((22, 14, 32), (40, 24, 56)), 'chest': ((30, 18, 44), (52, 30, 70)), 'armor': ((34, 32, 44), (70, 64, 86)),
    'cape': ((14, 8, 22), (30, 16, 44)), 'face': ((48, 40, 52), (70, 60, 74)), 'hood': ((18, 10, 28), (36, 20, 50)),
    'horn': ((60, 20, 110), (150, 80, 240)), 'handle': ((40, 30, 30), (60, 46, 44)), 'hammer': ((28, 26, 34), (60, 56, 72)),
    'core': ((200, 120, 255), (255, 230, 255)),
}
VOID = (176, 90, 255)
img = Image.new('RGBA', (TW, TH), (0, 0, 0, 0))
glow = Image.new('RGBA', (TW, TH), (0, 0, 0, 0))
px, gx = img.load(), glow.load()


def crack(rx, ry, rw, rh, n):
    for _ in range(n):
        cx, cy = rx + random.randrange(rw), ry + random.randrange(rh)
        for _ in range(random.randint(3, 8)):
            if not (rx <= cx < rx + rw and ry <= cy < ry + rh) or px[cx, cy][3] == 0:
                break
            c = VOID if random.random() < 0.7 else (230, 190, 255)
            px[cx, cy] = c + (255,)
            gx[cx, cy] = c + (255,)
            cx += random.choice((-1, 0, 1))
            cy += random.choice((0, 1, 1))


for i, (bone, o, s, kind) in enumerate(cubes):
    w, h, d = s
    u, v = uv[i]
    a, b = PAL[kind]
    for yy in range(v, v + d + h):
        for xx in range(u, u + 2 * d + 2 * w):
            if yy < v + d and not (u + d <= xx < u + d + 2 * w):
                continue
            t = random.random() * 0.35 + (yy - v) / max(1, d + h) * 0.4
            px[xx, yy] = tuple(int(a[k] + (b[k] - a[k]) * t) for k in range(3)) + (255,)
            if kind == 'core':
                gx[xx, yy] = (255, 210, 255, 255)
    area = (2 * d + 2 * w) * (d + h)
    crack(u, v, 2 * d + 2 * w, d + h, max(1, area // (40 if kind in ('chest', 'robe', 'face', 'hammer') else 90)))
    if kind == 'face':  # north face = front: glowing eyes + mouth slit
        fx, fy = u + d, v + d
        for ex in (fx + 1, fx + 5):
            for c in ((ex, fy + 3), (ex + 1, fy + 3)):
                px[c] = (235, 170, 255, 255)
                gx[c] = (235, 170, 255, 255)
        for xx in range(fx + 2, fx + 6):
            px[xx, fy + 6] = (120, 50, 200, 255)
            gx[xx, fy + 6] = (120, 50, 200, 255)
    if kind == 'hammer':  # rune bands
        for yy in range(v + d, v + d + h, 3):
            for xx in range(u, u + 2 * d + 2 * w, 2):
                px[xx, yy] = VOID + (255,)
                gx[xx, yy] = VOID + (255,)

tex_dir = os.path.join(ROOT, 'textures', 'entity')
img.save(os.path.join(tex_dir, NAME + '.png'))
glow.save(os.path.join(tex_dir, NAME + '_glowmask.png'))

bones_json = []
for b in BONES:
    bj = {'name': b[0], 'pivot': b[2]}
    if b[1]:
        bj['parent'] = b[1]
    cj = [{'origin': c[1], 'size': c[2], 'uv': list(uv[i])} for i, c in enumerate(cubes) if c[0] == b[0]]
    if cj:
        bj['cubes'] = cj
    bones_json.append(bj)
geo = {'format_version': '1.12.0', 'minecraft:geometry': [{
    'description': {'identifier': 'geometry.' + NAME, 'texture_width': TW, 'texture_height': TH,
                    'visible_bounds_width': 5, 'visible_bounds_height': 5, 'visible_bounds_offset': [0, 1.5, 0]},
    'bones': bones_json}]}
with open(os.path.join(ROOT, 'geo', 'entity', NAME + '.geo.json'), 'w') as f:
    json.dump(geo, f, indent=1)


def kf(d):
    return {('%.2f' % t): v for t, v in d}


def anim(length, bones, loop=False):
    a = {'animation_length': length, 'bones': bones}
    if loop:
        a['loop'] = True
    return a


P = 'animation.' + NAME + '.'
A = {}
A[P + 'idle'] = anim(3.0, {
    'body': {'rotation': kf([(0, [2, 0, 0]), (1.5, [4, 0, 0]), (3.0, [2, 0, 0])])},
    'head': {'rotation': kf([(0, [0, 0, 0]), (1.5, [-4, 6, 0]), (3.0, [0, 0, 0])])},
    'right_arm': {'rotation': kf([(0, [-10, 0, 8]), (1.5, [-14, 0, 10]), (3.0, [-10, 0, 8])])},
    'cape': {'rotation': kf([(0, [6, 0, 0]), (1.5, [10, 0, 0]), (3.0, [6, 0, 0])])},
    'void_core': {'scale': kf([(0, [1, 1, 1]), (1.5, [1.4, 1.4, 1.4]), (3.0, [1, 1, 1])])}}, True)
A[P + 'walk'] = anim(1.0, {
    'right_leg': {'rotation': kf([(0, [30, 0, 0]), (0.5, [-30, 0, 0]), (1.0, [30, 0, 0])])},
    'left_leg': {'rotation': kf([(0, [-30, 0, 0]), (0.5, [30, 0, 0]), (1.0, [-30, 0, 0])])},
    'left_arm': {'rotation': kf([(0, [25, 0, 0]), (0.5, [-25, 0, 0]), (1.0, [25, 0, 0])])},
    'right_arm': {'rotation': kf([(0, [-20, 0, 8]), (0.5, [-10, 0, 8]), (1.0, [-20, 0, 8])])},
    'body': {'rotation': kf([(0, [8, 0, 0]), (1.0, [8, 0, 0])])},
    'cape': {'rotation': kf([(0, [20, 0, 0]), (0.5, [30, 0, 0]), (1.0, [20, 0, 0])])}}, True)
A[P + 'attack'] = anim(0.8, {
    'right_arm': {'rotation': kf([(0, [-10, 0, 8]), (0.25, [-150, 0, 0]), (0.45, [30, 0, 0]), (0.8, [-10, 0, 8])])},
    'body': {'rotation': kf([(0, [0, 0, 0]), (0.25, [-10, 15, 0]), (0.45, [20, -10, 0]), (0.8, [0, 0, 0])])}})
# delayed slam: raise 0-0.4, hold (Radagon delay) until 1.3, slam at 1.4 (= tick 28)
A[P + 'slam'] = anim(2.2, {
    'right_arm': {'rotation': kf([(0, [-10, 0, 8]), (0.4, [-175, 0, 0]), (1.3, [-185, 0, 0]), (1.4, [40, 0, 0]), (2.2, [-10, 0, 8])])},
    'left_arm': {'rotation': kf([(0, [0, 0, 0]), (0.4, [-160, 0, 0]), (1.3, [-170, 0, 0]), (1.4, [30, 0, 0]), (2.2, [0, 0, 0])])},
    'body': {'rotation': kf([(0, [0, 0, 0]), (0.4, [-15, 0, 0]), (1.3, [-18, 0, 0]), (1.4, [30, 0, 0]), (2.2, [0, 0, 0])])},
    'root': {'position': kf([(0, [0, 0, 0]), (1.3, [0, 2, 0]), (1.4, [0, -2, -4]), (2.2, [0, 0, 0])])}})
# 3-hit combo: swings land at 0.6 / 1.1 / 1.8 s (ticks 12 / 22 / 36)
A[P + 'combo'] = anim(2.4, {
    'right_arm': {'rotation': kf([(0, [-10, 0, 8]), (0.45, [-100, -60, 60]), (0.6, [-60, 60, -40]), (0.95, [-100, 60, -60]),
                                  (1.1, [-60, -60, 50]), (1.6, [-180, 0, 0]), (1.8, [40, 0, 0]), (2.4, [-10, 0, 8])])},
    'body': {'rotation': kf([(0, [0, 0, 0]), (0.45, [0, 40, 0]), (0.6, [10, -40, 0]), (0.95, [0, -40, 0]),
                             (1.1, [10, 40, 0]), (1.6, [-15, 0, 0]), (1.8, [30, 0, 0]), (2.4, [0, 0, 0])])}})
A[P + 'teleport'] = anim(1.8, {
    'body': {'rotation': kf([(0, [0, 0, 0]), (0.5, [30, 0, 0]), (1.1, [-20, 0, 0]), (1.3, [40, 0, 0]), (1.8, [0, 0, 0])])},
    'right_arm': {'rotation': kf([(0, [-10, 0, 8]), (0.5, [20, 0, 30]), (1.1, [-170, 0, 0]), (1.3, [50, 0, 0]), (1.8, [-10, 0, 8])])},
    'root': {'scale': kf([(0, [1, 1, 1]), (0.6, [0.2, 1.4, 0.2]), (0.7, [1, 1, 1])])}})
A[P + 'pillars'] = anim(2.0, {
    'right_arm': {'rotation': kf([(0, [-10, 0, 8]), (0.8, [-170, 0, 0]), (1.5, [10, 0, 0]), (2.0, [-10, 0, 8])])},
    'left_arm': {'rotation': kf([(0, [0, 0, 0]), (0.8, [-170, 0, 0]), (1.5, [10, 0, 0]), (2.0, [0, 0, 0])])},
    'body': {'rotation': kf([(0, [0, 0, 0]), (0.8, [-20, 0, 0]), (1.5, [35, 0, 0]), (2.0, [0, 0, 0])])},
    'right_leg': {'rotation': kf([(0, [0, 0, 0]), (1.5, [-30, 0, 0]), (2.0, [0, 0, 0])])},
    'left_leg': {'rotation': kf([(0, [0, 0, 0]), (1.5, [-30, 0, 0]), (2.0, [0, 0, 0])])}})
A[P + 'ult'] = anim(5.5, {
    'right_arm': {'rotation': kf([(0, [-10, 0, 8]), (1.0, [-180, 0, 0]), (3.8, [-185, 0, 0]), (4.0, [40, 0, 0]), (5.5, [-10, 0, 8])])},
    'left_arm': {'rotation': kf([(0, [0, 0, 0]), (1.0, [-120, 0, -40]), (3.8, [-120, 0, -40]), (4.0, [30, 0, 0]), (5.5, [0, 0, 0])])},
    'root': {'position': kf([(0, [0, 0, 0]), (2.0, [0, 24, 0]), (3.8, [0, 30, 0]), (4.0, [0, 0, 0]), (5.5, [0, 0, 0])])},
    'void_core': {'scale': kf([(0, [1, 1, 1]), (3.8, [3, 3, 3]), (4.0, [1, 1, 1])])}})
# body cracking / convulsing before form 2 replaces it (5.5 s = 110 ticks)
A[P + 'transform'] = anim(5.5, {
    'body': {'rotation': kf([(0, [0, 0, 0]), (1.0, [25, 0, 0]), (2.0, [-30, 10, 0]), (3.0, [-35, -10, 5]), (4.5, [-45, 0, 0]), (5.5, [-50, 0, 0])])},
    'head': {'rotation': kf([(0, [0, 0, 0]), (1.0, [30, 0, 0]), (2.0, [-40, 20, 0]), (3.0, [-50, -20, 0]), (5.5, [-60, 0, 0])])},
    'right_arm': {'rotation': kf([(0, [-10, 0, 8]), (1.0, [10, 0, 20]), (2.5, [-60, 0, 80]), (5.5, [-90, 0, 90])])},
    'left_arm': {'rotation': kf([(0, [0, 0, 0]), (1.0, [10, 0, -20]), (2.5, [-60, 0, -80]), (5.5, [-90, 0, -90])])},
    'hammer': {'rotation': kf([(0, [0, 0, 0]), (1.0, [0, 0, 0]), (1.4, [90, 0, 0])]),
               'position': kf([(0, [0, 0, 0]), (1.0, [0, 0, 0]), (1.4, [0, -14, 6])])},
    'root': {'position': kf([(0, [0, 0, 0]), (1.0, [0, -4, 0]), (2.5, [0, 6, 0]), (5.5, [0, 14, 0])])},
    'void_core': {'scale': kf([(0, [1, 1, 1]), (2.5, [2.5, 2.5, 2.5]), (5.0, [5, 5, 5]), (5.5, [7, 7, 7])])}})
with open(os.path.join(ROOT, 'animations', 'entity', NAME + '.animation.json'), 'w') as f:
    json.dump({'format_version': '1.8.0', 'animations': A}, f, indent=1)
print('ok', len(cubes), 'cubes, uv height', y + shelf)
