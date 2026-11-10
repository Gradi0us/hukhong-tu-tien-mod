"""Generate model (geo.json), animations and 256x256 texture + glowmask for Bi Canh Thu Ve
(armored realm guardian: Tree Sentinel / Crucible Knight - halberd + tower shield, armor tail, horned helm, ember wings).
Run from repo root: python tools/gen_bicanh_thu_ve.py"""
import json, random, os
from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources', 'assets', 'hukhong')
NAME = 'bicanh_thu_ve'
TW = TH = 256
random.seed(77)

# (bone, parent, pivot, [(origin, size, kind)]) - front of the model is -z (north), right hand is -x
BONES = [
    ('root', None, [0, 0, 0], []),
    ('right_leg', 'root', [-4, 20, 0], [([-7, 0, -3.5], [6, 20, 7], 'plate'),
                                         ([-7.5, 9, -4.5], [7, 4, 2], 'gold'),
                                         ([-7.5, 0, -4.5], [7, 3, 8], 'plate2')]),
    ('left_leg', 'root', [4, 20, 0], [([1, 0, -3.5], [6, 20, 7], 'plate'),
                                       ([0.5, 9, -4.5], [7, 4, 2], 'gold'),
                                       ([0.5, 0, -4.5], [7, 3, 8], 'plate2')]),
    ('body', 'root', [0, 20, 0], [([-9, 20, -5], [18, 16, 10], 'plate'),
                                  ([-9.5, 13, -5.5], [19, 8, 11], 'plate2'),
                                  ([-3, 26, -6], [6, 6, 1], 'emblem'),
                                  ([-14, 31, -6], [6, 6, 12], 'gold'),
                                  ([8, 31, -6], [6, 6, 12], 'gold')]),
    ('tail', 'body', [0, 18, 5], [([-2.5, 15, 5], [5, 5, 10], 'plate2')]),
    ('tail_tip', 'tail', [0, 17, 15], [([-1.5, 15.5, 15], [3, 3, 10], 'gold'),
                                       ([-0.5, 12, 22], [1, 8, 5], 'blade')]),
    ('head', 'body', [0, 36, 0], [([-4.5, 36, -4.5], [9, 10, 9], 'helm'),
                                  ([-3.5, 40, -5], [7, 1, 1], 'eye'),
                                  ([-1, 46, -3], [2, 4, 9], 'plume'),
                                  ([-1, 43, -8], [2, 2, 4], 'horn')]),
    ('horn_r', 'head', [-4.5, 43, 0], [([-7, 43, -1], [3, 2, 2], 'horn'), ([-8, 45, -1], [2, 5, 2], 'horn')]),
    ('horn_l', 'head', [4.5, 43, 0], [([4, 43, -1], [3, 2, 2], 'horn'), ([6, 45, -1], [2, 5, 2], 'horn')]),
    ('ph3_halo', 'head', [0, 43, 7], [([-7, 50, 7], [14, 1, 1], 'halo'), ([-7, 37, 7], [1, 13, 1], 'halo'),
                                      ([6, 37, 7], [1, 13, 1], 'halo')]),
    ('wings', 'body', [0, 32, 5], []),
    ('wing_r', 'wings', [-3, 32, 5], [([-21, 17, 5], [18, 17, 1], 'wing')]),
    ('wing_l', 'wings', [3, 32, 5], [([3, 17, 5], [18, 17, 1], 'wing')]),
    ('right_arm', 'body', [-11, 33, 0], [([-14, 17, -2.5], [5, 16, 5], 'plate'),
                                         ([-14.5, 15, -3], [6, 5, 6], 'gold')]),
    ('halberd', 'right_arm', [-11.5, 17, 0], [([-12.5, 2, -1], [2, 42, 2], 'shaft'),
                                              ([-12, 34, -8], [1, 9, 7], 'blade'),
                                              ([-12.5, 44, -1], [2, 6, 2], 'blade'),
                                              ([-12, 37, 1], [1, 4, 4], 'blade')]),
    ('left_arm', 'body', [11, 33, 0], [([9, 17, -2.5], [5, 16, 5], 'plate'),
                                       ([8.5, 15, -3], [6, 5, 6], 'gold')]),
    ('shield', 'left_arm', [14, 22, 0], [([14, 6, -8], [2, 26, 16], 'shield'),
                                         ([16, 15, -3], [1, 8, 6], 'emblem')]),
]

cubes = [(b[0], o, s, k) for b in BONES for (o, s, k) in b[3]]


def isz(s):
    return [int(round(v)) for v in s]


order = sorted(range(len(cubes)), key=lambda i: -(isz(cubes[i][2])[2] + isz(cubes[i][2])[1]))
uv = {}
x = y = shelf = 0
for i in order:
    w, h, d = isz(cubes[i][2])
    bw, bh = 2 * d + 2 * w, d + h
    if x + bw > TW:
        x, y, shelf = 0, y + shelf, 0
    uv[i] = (x, y)
    x += bw
    shelf = max(shelf, bh)
assert y + shelf <= TH, 'texture too small'

PAL = {
    'plate': ((96, 72, 34), (176, 138, 70)), 'plate2': ((70, 52, 26), (130, 100, 52)), 'gold': ((170, 130, 50), (240, 200, 110)),
    'emblem': ((200, 90, 30), (255, 190, 90)), 'helm': ((110, 84, 40), (190, 150, 80)), 'eye': ((255, 170, 60), (255, 240, 170)),
    'plume': ((120, 20, 24), (190, 40, 40)), 'horn': ((190, 176, 150), (240, 230, 210)), 'halo': ((255, 190, 80), (255, 245, 200)),
    'wing': ((120, 30, 10), (255, 120, 30)), 'shaft': ((50, 34, 24), (80, 56, 40)), 'blade': ((150, 150, 160), (225, 225, 235)),
    'shield': ((88, 66, 32), (160, 124, 64)),
}
GLOW_KINDS = {'eye', 'halo'}
EMBER = (255, 150, 50)
img = Image.new('RGBA', (TW, TH), (0, 0, 0, 0))
glow = Image.new('RGBA', (TW, TH), (0, 0, 0, 0))
px, gx = img.load(), glow.load()

for i, (bone, o, s, kind) in enumerate(cubes):
    w, h, d = isz(s)
    u, v = uv[i]
    a, b = PAL[kind]
    for yy in range(v, v + d + h):
        for xx in range(u, u + 2 * d + 2 * w):
            if yy < v + d and not (u + d <= xx < u + d + 2 * w):
                continue
            t = random.random() * 0.3 + (1 - (yy - v) / max(1, d + h)) * 0.45
            c = tuple(int(a[k] + (b[k] - a[k]) * t) for k in range(3))
            if kind in ('plate', 'plate2', 'helm', 'shield') and (yy - v) % 6 == 0:  # plate seams
                c = tuple(int(q * 0.6) for q in c)
            px[xx, yy] = c + (255,)
            if kind in GLOW_KINDS:
                gx[xx, yy] = c + (255,)
            if kind == 'wing' and random.random() < 0.35:  # ember veins
                px[xx, yy] = EMBER + (255,)
                gx[xx, yy] = EMBER + (255,)
            if kind == 'emblem' and (xx + yy) % 3 == 0:
                gx[xx, yy] = (255, 200, 110, 255)
            if kind == 'blade' and yy % 4 == 0:
                gx[xx, yy] = (255, 230, 170, 160)
    if kind == 'shield':  # gold rim + sunburst rune
        for yy in range(v + d, v + d + h):
            for xx in (u, u + 2 * d + 2 * w - 1):
                px[xx, yy] = (230, 190, 100, 255)
        cx, cy = u + d + w // 2, v + d + h // 2
        for r in range(1, 6):
            for dx, dy in ((r, 0), (-r, 0), (0, r), (0, -r), (r, r), (-r, -r)):
                if 0 <= cx + dx < TW and 0 <= cy + dy < TH and px[cx + dx, cy + dy][3]:
                    px[cx + dx, cy + dy] = (255, 200, 100, 255)
                    gx[cx + dx, cy + dy] = (255, 190, 90, 255)

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
                    'visible_bounds_width': 6, 'visible_bounds_height': 6, 'visible_bounds_offset': [0, 2, 0]},
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


R0 = [-15, 0, 6]   # right arm rest (halberd forward)
L0 = [-10, 0, -4]
P = 'animation.' + NAME + '.'
A = {}
A[P + 'idle'] = anim(3.0, {
    'body': {'rotation': kf([(0, [2, 0, 0]), (1.5, [4, 0, 0]), (3.0, [2, 0, 0])])},
    'head': {'rotation': kf([(0, [0, 0, 0]), (1.5, [-3, 5, 0]), (3.0, [0, 0, 0])])},
    'right_arm': {'rotation': kf([(0, R0), (1.5, [-18, 0, 8]), (3.0, R0)])},
    'left_arm': {'rotation': kf([(0, L0), (3.0, L0)])},
    'tail': {'rotation': kf([(0, [-10, -8, 0]), (1.5, [-14, 8, 0]), (3.0, [-10, -8, 0])])},
    'tail_tip': {'rotation': kf([(0, [10, 0, 0]), (1.5, [16, 0, 0]), (3.0, [10, 0, 0])])}}, True)
A[P + 'walk'] = anim(1.2, {
    'right_leg': {'rotation': kf([(0, [25, 0, 0]), (0.6, [-25, 0, 0]), (1.2, [25, 0, 0])])},
    'left_leg': {'rotation': kf([(0, [-25, 0, 0]), (0.6, [25, 0, 0]), (1.2, [-25, 0, 0])])},
    'right_arm': {'rotation': kf([(0, [-25, 0, 6]), (0.6, [-10, 0, 6]), (1.2, [-25, 0, 6])])},
    'left_arm': {'rotation': kf([(0, [-15, 0, -4]), (0.6, [5, 0, -4]), (1.2, [-15, 0, -4])])},
    'body': {'rotation': kf([(0, [6, -4, 0]), (0.6, [6, 4, 0]), (1.2, [6, -4, 0])])},
    'tail': {'rotation': kf([(0, [-6, 12, 0]), (0.6, [-6, -12, 0]), (1.2, [-6, 12, 0])])}}, True)
# idle guard: shield turned forward in front of the body
A[P + 'guard'] = anim(2.0, {
    'left_arm': {'rotation': kf([(0, [-60, -70, 0]), (1.0, [-62, -72, 0]), (2.0, [-60, -70, 0])])},
    'right_arm': {'rotation': kf([(0, [-35, 0, 15]), (2.0, [-35, 0, 15])])},
    'body': {'rotation': kf([(0, [10, 0, 0]), (2.0, [10, 0, 0])])},
    'right_leg': {'rotation': kf([(0, [-15, 0, 0]), (2.0, [-15, 0, 0])])},
    'left_leg': {'rotation': kf([(0, [15, 0, 0]), (2.0, [15, 0, 0])])}}, True)
A[P + 'attack'] = anim(0.8, {
    'right_arm': {'rotation': kf([(0, R0), (0.25, [-160, 0, 0]), (0.45, [20, 0, 0]), (0.8, R0)])},
    'body': {'rotation': kf([(0, [0, 0, 0]), (0.25, [-12, 15, 0]), (0.45, [22, -10, 0]), (0.8, [0, 0, 0])])}})
# combo: sweeps land at 0.5 / 1.0 / 1.75 s (ticks 10 / 20 / 35)
A[P + 'combo'] = anim(2.3, {
    'right_arm': {'rotation': kf([(0, R0), (0.35, [-90, -70, 60]), (0.5, [-60, 70, -40]), (0.85, [-90, 70, -60]),
                                  (1.0, [-60, -70, 50]), (1.5, [-185, 0, 0]), (1.75, [35, 0, 0]), (2.3, R0)])},
    'body': {'rotation': kf([(0, [0, 0, 0]), (0.35, [0, 45, 0]), (0.5, [10, -45, 0]), (0.85, [0, -45, 0]),
                             (1.0, [10, 45, 0]), (1.5, [-18, 0, 0]), (1.75, [32, 0, 0]), (2.3, [0, 0, 0])])}})
# shield bash: wind back, slam shield at 0.5 (tick 10), halberd thrust at 1.1 (tick 22)
A[P + 'shield_bash'] = anim(1.5, {
    'left_arm': {'rotation': kf([(0, L0), (0.3, [-40, -80, 0]), (0.5, [-85, -60, 0]), (0.9, [-60, -70, 0]), (1.5, L0)])},
    'right_arm': {'rotation': kf([(0, R0), (0.9, [-40, 0, 0]), (1.1, [-95, 0, 0]), (1.5, R0)])},
    'body': {'rotation': kf([(0, [0, 0, 0]), (0.3, [-5, 25, 0]), (0.5, [18, -20, 0]), (1.1, [20, 10, 0]), (1.5, [0, 0, 0])])},
    'root': {'position': kf([(0, [0, 0, 0]), (0.5, [0, 0, -6]), (1.1, [0, 0, -10]), (1.5, [0, 0, 0])])}})
# tail swipe: full turn, tail whips at 0.6-0.9 (ticks 12-18)
A[P + 'tail'] = anim(1.4, {
    'root': {'rotation': kf([(0, [0, 0, 0]), (0.4, [0, -40, 0]), (0.9, [0, 320, 0]), (1.0, [0, 360, 0])])},
    'tail': {'rotation': kf([(0, [-10, 0, 0]), (0.4, [0, -40, 0]), (0.9, [10, 60, 0]), (1.4, [-10, 0, 0])])},
    'tail_tip': {'rotation': kf([(0, [10, 0, 0]), (0.6, [-10, 40, 0]), (1.4, [10, 0, 0])])},
    'body': {'rotation': kf([(0, [0, 0, 0]), (0.4, [15, 0, 0]), (1.4, [0, 0, 0])])}})
# horn charge: lower head 0-0.5, run head-first until 1.6
A[P + 'charge'] = anim(1.8, {
    'body': {'rotation': kf([(0, [0, 0, 0]), (0.5, [45, 0, 0]), (1.6, [45, 0, 0]), (1.8, [0, 0, 0])])},
    'head': {'rotation': kf([(0, [0, 0, 0]), (0.5, [20, 0, 0]), (1.6, [20, 0, 0]), (1.8, [0, 0, 0])])},
    'left_arm': {'rotation': kf([(0, L0), (0.5, [-70, -70, 0]), (1.6, [-70, -70, 0]), (1.8, L0)])},
    'right_arm': {'rotation': kf([(0, R0), (0.5, [30, 0, 10]), (1.6, [30, 0, 10]), (1.8, R0)])},
    'right_leg': {'rotation': kf([(0.5, [-40, 0, 0]), (0.7, [40, 0, 0]), (0.9, [-40, 0, 0]), (1.1, [40, 0, 0]), (1.3, [-40, 0, 0]), (1.6, [0, 0, 0])])},
    'left_leg': {'rotation': kf([(0.5, [40, 0, 0]), (0.7, [-40, 0, 0]), (0.9, [40, 0, 0]), (1.1, [-40, 0, 0]), (1.3, [40, 0, 0]), (1.6, [0, 0, 0])])}})
# giant stomp: hop up 0-0.9, slam at 1.2 (tick 24)
A[P + 'stomp'] = anim(2.0, {
    'root': {'position': kf([(0, [0, 0, 0]), (0.3, [0, -3, 0]), (0.9, [0, 20, 0]), (1.2, [0, 0, 0]), (1.4, [0, -3, 0]), (2.0, [0, 0, 0])])},
    'right_leg': {'rotation': kf([(0, [0, 0, 0]), (0.9, [-60, 0, 0]), (1.2, [10, 0, 0]), (2.0, [0, 0, 0])])},
    'left_leg': {'rotation': kf([(0, [0, 0, 0]), (0.9, [-20, 0, 0]), (1.2, [10, 0, 0]), (2.0, [0, 0, 0])])},
    'body': {'rotation': kf([(0, [0, 0, 0]), (0.3, [20, 0, 0]), (0.9, [-15, 0, 0]), (1.2, [30, 0, 0]), (2.0, [0, 0, 0])])},
    'right_arm': {'rotation': kf([(0, R0), (0.9, [-170, 0, 20]), (1.2, [-20, 0, 40]), (2.0, R0)])}})
# wings burst: spread 0-0.9, burst at 1.0 (tick 20)
A[P + 'wings'] = anim(1.8, {
    'wing_r': {'rotation': kf([(0, [0, -60, 0]), (0.9, [0, 30, -30]), (1.0, [0, 45, -40]), (1.8, [0, -60, 0])])},
    'wing_l': {'rotation': kf([(0, [0, 60, 0]), (0.9, [0, -30, 30]), (1.0, [0, -45, 40]), (1.8, [0, 60, 0])])},
    'body': {'rotation': kf([(0, [0, 0, 0]), (0.9, [-20, 0, 0]), (1.0, [15, 0, 0]), (1.8, [0, 0, 0])])},
    'right_arm': {'rotation': kf([(0, R0), (0.9, [-30, 0, 60]), (1.8, R0)])},
    'left_arm': {'rotation': kf([(0, L0), (0.9, [-30, 0, -60]), (1.8, L0)])}})
# leaping halberd slam: crouch 0-0.3, airborne, slam at 1.0 (tick 20)
A[P + 'leap'] = anim(1.5, {
    'right_arm': {'rotation': kf([(0, R0), (0.4, [-180, 0, 0]), (0.9, [-190, 0, 0]), (1.0, [40, 0, 0]), (1.5, R0)])},
    'body': {'rotation': kf([(0, [0, 0, 0]), (0.3, [25, 0, 0]), (0.9, [-20, 0, 0]), (1.0, [35, 0, 0]), (1.5, [0, 0, 0])])},
    'right_leg': {'rotation': kf([(0, [0, 0, 0]), (0.5, [-50, 0, 0]), (1.0, [0, 0, 0])])},
    'left_leg': {'rotation': kf([(0, [0, 0, 0]), (0.5, [30, 0, 0]), (1.0, [0, 0, 0])])}})
# ultimate: wings out, rise, hold halberd up (sky judgement) and drive it down at 4.5 s (tick 90)
A[P + 'ult'] = anim(5.0, {
    'wing_r': {'rotation': kf([(0, [0, -60, 0]), (1.0, [0, 40, -40]), (4.5, [0, 40, -40]), (5.0, [0, -60, 0])])},
    'wing_l': {'rotation': kf([(0, [0, 60, 0]), (1.0, [0, -40, 40]), (4.5, [0, -40, 40]), (5.0, [0, 60, 0])])},
    'right_arm': {'rotation': kf([(0, R0), (1.5, [-180, 0, 0]), (4.3, [-185, 0, 0]), (4.5, [40, 0, 0]), (5.0, R0)])},
    'left_arm': {'rotation': kf([(0, L0), (1.5, [-90, 0, -60]), (4.3, [-90, 0, -60]), (4.5, [-60, -70, 0]), (5.0, L0)])},
    'head': {'rotation': kf([(0, [0, 0, 0]), (1.5, [-25, 0, 0]), (4.3, [-25, 0, 0]), (4.5, [15, 0, 0]), (5.0, [0, 0, 0])])},
    'ph3_halo': {'scale': kf([(0, [1, 1, 1]), (4.3, [2, 2, 2]), (4.5, [1, 1, 1])])}})
# phase shift roar
A[P + 'phase'] = anim(2.5, {
    'head': {'rotation': kf([(0, [0, 0, 0]), (0.6, [-35, 0, 0]), (2.0, [-35, 0, 0]), (2.5, [0, 0, 0])])},
    'body': {'rotation': kf([(0, [0, 0, 0]), (0.6, [-15, 0, 0]), (2.0, [-15, 0, 0]), (2.5, [0, 0, 0])])},
    'right_arm': {'rotation': kf([(0, R0), (0.6, [-20, 0, 70]), (2.0, [-20, 0, 70]), (2.5, R0)])},
    'left_arm': {'rotation': kf([(0, L0), (0.6, [-20, 0, -70]), (2.0, [-20, 0, -70]), (2.5, L0)])},
    'wing_r': {'rotation': kf([(0, [0, -60, 0]), (0.8, [0, 45, -40]), (2.0, [0, 45, -40]), (2.5, [0, -60, 0])])},
    'wing_l': {'rotation': kf([(0, [0, 60, 0]), (0.8, [0, -45, 40]), (2.0, [0, -45, 40]), (2.5, [0, 60, 0])])}})
# death: drops to one knee leaning on the halberd, then sinks
A[P + 'death'] = anim(3.0, {
    'root': {'position': kf([(0, [0, 0, 0]), (0.8, [0, -8, 0]), (3.0, [0, -10, 0])])},
    'right_leg': {'rotation': kf([(0, [0, 0, 0]), (0.8, [-80, 0, 0]), (3.0, [-80, 0, 0])])},
    'left_leg': {'rotation': kf([(0, [0, 0, 0]), (0.8, [80, 0, 0]), (3.0, [80, 0, 0])])},
    'body': {'rotation': kf([(0, [0, 0, 0]), (0.8, [20, 0, 0]), (3.0, [40, 0, 0])])},
    'head': {'rotation': kf([(0, [0, 0, 0]), (1.5, [35, 0, 0]), (3.0, [45, 0, 0])])},
    'right_arm': {'rotation': kf([(0, R0), (0.8, [-60, 0, 0]), (3.0, [-60, 0, 0])])},
    'left_arm': {'rotation': kf([(0, L0), (0.8, [10, 0, -20]), (3.0, [20, 0, -30])])}}, False)
A[P + 'death']['loop'] = 'hold_on_last_frame'
with open(os.path.join(ROOT, 'animations', 'entity', NAME + '.animation.json'), 'w') as f:
    json.dump({'format_version': '1.8.0', 'animations': A}, f, indent=1)
print('ok', len(cubes), 'cubes, uv height', y + shelf)
