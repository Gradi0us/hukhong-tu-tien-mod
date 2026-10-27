# Bản Mệnh Kiếm: texture từng giai đoạn (Linh..Thần) cho 8 loại + model item theo giai đoạn.
#   python tools/gen_banmenh_stages.py [--sheet out.png]
# Giai đoạn 0 (Phàm) giữ texture 16x16 sẵn có (item/ban_menh/<id>.png); 1..6 sinh 32x32 pixel-art (đổ bóng 4 tông + viền 1px),
# từ giai đoạn 4 (Thiên) có thêm lớp phát sáng <id>_s<N>_glow.png (emissive qua neoforge_data layer 1 trong model).
# Model: item/ban_menh/<id>[_s<N>].json — display tay thu nhỏ ~0 (kiếm KHÔNG hiện trong tay, vẽ quỹ đạo ở client),
# display fixed = không xoay/không co (OrbitRenderer tự đặt xoay/scale). ban_menh_kiem.json chọn theo custom_model_data (loại)
# + property hukhong:stage (giai đoạn) — dòng đứng SAU thắng, nên sắp xếp stage tăng dần, loại tăng dần.
import json
import math
import os
import random
import sys
import zlib

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
ASSETS = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'hukhong')
TEX = os.path.join(ASSETS, 'textures', 'item', 'ban_menh')
MOD = os.path.join(ASSETS, 'models', 'item', 'ban_menh')
TOP = os.path.join(ASSETS, 'models', 'item', 'ban_menh_kiem.json')
S = 32
OUT = (20, 14, 22, 255)
STEEL = (178, 182, 194)

# thứ tự = BanMenhType.modelIndex() 1..8
TYPES = ['thanh_phong', 'liet_diem', 'han_bang', 'loi_dinh', 'huyet_anh', 'trong_nhac', 'ngoc_tam', 'tinh_than']
# blade: màu thân kiếm; accent: khóa/chuôi (giai đoạn >= 3); glow: màu phát sáng; grip: chuôi
PAL = {
    'thanh_phong': dict(blade=(96, 226, 204), accent=(236, 196, 84), glow=(190, 255, 235), grip=(40, 110, 98)),
    'liet_diem': dict(blade=(255, 132, 34), accent=(255, 214, 90), glow=(255, 235, 140), grip=(110, 40, 24)),
    'han_bang': dict(blade=(148, 214, 255), accent=(226, 240, 255), glow=(230, 250, 255), grip=(52, 84, 128)),
    'loi_dinh': dict(blade=(150, 102, 242), accent=(255, 226, 72), glow=(255, 248, 150), grip=(60, 40, 110)),
    'huyet_anh': dict(blade=(206, 30, 46), accent=(150, 24, 34), glow=(255, 110, 110), grip=(70, 14, 20)),
    'trong_nhac': dict(blade=(208, 208, 220), accent=(196, 146, 72), glow=(255, 232, 170), grip=(104, 72, 38)),
    'ngoc_tam': dict(blade=(72, 202, 112), accent=(244, 214, 120), glow=(200, 255, 200), grip=(34, 96, 56)),
    'tinh_than': dict(blade=(132, 122, 232), accent=(224, 214, 255), glow=(235, 225, 255), grip=(56, 48, 112)),
}

# giai đoạn 1..6: L = u mũi kiếm, w = nửa bề rộng thân, guard = nửa bề rộng khóa, steel = pha thép (càng thấp càng rực)
ST = {
    1: dict(L=31.0, w=1.2, guard=4.2, steel=0.50, fuller=False, bulge=0.0, wing=0, gem=False, glow=False, rune=False, aura=0, pommel=1),
    2: dict(L=33.0, w=1.45, guard=4.8, steel=0.28, fuller=True, bulge=0.0, wing=0, gem=False, glow=False, rune=False, aura=0, pommel=1),
    3: dict(L=34.5, w=1.7, guard=5.8, steel=0.10, fuller=True, bulge=0.10, wing=1, gem=False, glow=False, rune=False, aura=0, pommel=2),
    4: dict(L=35.5, w=1.95, guard=6.6, steel=0.0, fuller=True, bulge=0.16, wing=1, gem=True, glow=True, rune=False, aura=0, pommel=2),
    5: dict(L=37.0, w=2.2, guard=7.4, steel=0.0, fuller=True, bulge=0.24, wing=2, gem=True, glow=True, rune=True, aura=0, pommel=3),
    6: dict(L=38.0, w=2.5, guard=8.2, steel=0.0, fuller=True, bulge=0.30, wing=3, gem=True, glow=True, rune=True, aura=14, pommel=3),
}
OX, OY = 2.5, 29.5
SQ = math.sqrt(2.0)
UB = 9.4  # u bắt đầu thân kiếm (sau khóa)


def tone(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (255,)


def mix(a, b, t):
    return tuple(int(a[i] * (1 - t) + b[i] * t) for i in range(3))


def lit(c, k):  # k>0 sáng dần về trắng, k<0 tối
    return tuple(int(v + (255 - v) * k) for v in c[:3]) if k >= 0 else tone(c, 1 + k)


def uv(x, y):
    qx, qy = x + 0.5 - OX, y + 0.5 - OY
    return (qx - qy) / SQ, (qx + qy) / SQ


def build(tp, stg):
    """Trả (base: dict[(x,y)] -> rgba, glow: dict[(x,y)] -> rgba)."""
    P, A = PAL[tp], ST[stg]
    rnd = random.Random(zlib.crc32(('%s%d' % (tp, stg)).encode()))
    blade = mix(P['blade'], STEEL, A['steel'])
    guard_c = mix((120, 120, 136), P['accent'], 0.0 if stg >= 3 else 0.0) if stg < 3 else P['accent']
    glow_c = P['glow']
    base, glow = {}, {}
    L, w0 = A['L'], A['w']
    tip_len = 2.6 + w0 * 1.3
    for y in range(S):
        for x in range(S):
            u, v = uv(x, y)
            col = None
            gl = None
            # --- đầu chuôi
            pr = (2.2, 2.9, 3.4)[A['pommel'] - 1]
            if -0.2 <= u < 2.2 + (0.8 if A['pommel'] == 3 else 0) and abs(v) <= pr * (0.6 if u < 0.6 else 1.0):
                col = tone(P['accent'] if stg >= 3 else (130, 130, 146), 1.0 - 0.18 * (v > 0.3) + 0.16 * (v < -0.3))
                if A['pommel'] >= 2 and u > 0.9 and abs(v) < 0.8:
                    col = lit(glow_c, -0.15) if stg >= 4 else tone(P['blade'], 1.15)
                    if stg >= 4:
                        gl = glow_c
            # --- chuôi
            elif 2.2 <= u < 7.2 and abs(v) <= 1.15:
                t = v / 1.15
                f = 1.2 if t < -0.4 else 0.55 if t > 0.45 else 0.85
                if int(u * 1.3) % 2 == 0:
                    f *= 0.82  # quấn dây
                col = tone(P['grip'], f)
            # --- khóa
            else:
                gx = 7.2 <= u < 9.4
                gh = A['guard']
                if A['wing']:  # cánh khóa cong lên theo v
                    shift = 0.035 * (abs(v) ** 1.5) * A['wing']
                    gx = 7.2 - shift <= u < 9.4 - shift * 0.6
                    if A['wing'] >= 2 and abs(v) > gh * 0.7:
                        gx = 6.0 - shift * 0.4 <= u < 9.6 - shift * 1.1
                if gx and abs(v) <= gh:
                    t = v / gh
                    f = 1.28 if t < -0.5 else 0.6 if t > 0.55 else 0.95
                    col = tone(guard_c, f)
                    if A['gem'] and abs(v) < 0.9 and 7.6 <= u < 9.0:
                        col = lit(glow_c, 0.2)
                        gl = glow_c
                # --- thân kiếm
                if col is None and UB <= u <= L:
                    rem = L - u
                    body = (u - UB) / max(1.0, L - UB - tip_len)
                    hw = w0 * (1.0 + 0.9 * A['bulge'] * math.sin(math.pi * min(1.0, max(0.0, (u - UB) / (L - UB))) ** 0.8))
                    hw *= 1.0 - 0.18 * min(1.0, body)
                    if rem < tip_len:
                        hw *= max(0.0, rem / tip_len)
                    # răng cưa nhẹ ở giai đoạn 5-6 (mép dưới v>0)
                    if stg >= 5 and v > 0 and int(u * 0.9) % 3 == 0 and 14 < u < L - 5:
                        hw -= 0.7
                    if abs(v) <= hw and hw > 0.35:
                        t = v / max(hw, 0.4)
                        if t < -0.62:
                            col = lit(blade, 0.55)
                        elif t < -0.12:
                            col = lit(blade, 0.18)
                        elif t <= 0.42:
                            col = blade
                        else:
                            col = tone(blade, 0.62)
                        if A['fuller'] and abs(v) < 0.5 and UB + 2.0 < u < L - tip_len - 1.0:
                            if A['glow']:
                                col = lit(glow_c, -0.08)
                                gl = glow_c
                            else:
                                col = tone(blade, 0.55)
                        if rem < 1.6 and stg >= 4:  # mũi kiếm rực
                            col = lit(glow_c, 0.4)
                            gl = glow_c
            if col is not None:
                base[(x, y)] = col
                if gl is not None:
                    glow[(x, y)] = tuple(gl[:3]) + (255,)
    # runes: chấm sáng hai bên rãnh
    if A['rune']:
        for (x, y) in list(base):
            u, v = uv(x, y)
            if UB + 4 < u < L - 8 and 0.55 < abs(v) < 1.6 and int(u * 0.75) % 4 == 2 and abs(v) < A['w'] * 0.7:
                base[(x, y)] = lit(glow_c, -0.2)
                glow[(x, y)] = tuple(glow_c[:3]) + (255,)
    # viền 1px
    solid = set(base)
    for y in range(S):
        for x in range(S):
            if (x, y) in solid:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                if (x + dx, y + dy) in solid:
                    base[(x, y)] = OUT
                    break
    # hào quang tinh thể quanh thân (Thần)
    for _ in range(A['aura']):
        for _try in range(40):
            u = rnd.uniform(UB, L - 2)
            v = rnd.choice((-1, 1)) * rnd.uniform(A['w'] + 1.6, A['w'] + 4.2)
            # nghịch của uv(): (x+.5-OX, y+.5-OY) = ((u+v)/SQ, (v-u)/SQ)
            x = int(round((u + v) / SQ + OX - 0.5))
            y = int(round((v - u) / SQ + OY - 0.5))
            if 0 <= x < S and 0 <= y < S and (x, y) not in base:
                c = lit(glow_c, 0.1)
                base[(x, y)] = c
                glow[(x, y)] = tuple(glow_c[:3]) + (255,)
                break
    return base, glow


def to_img(d):
    im = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    px = im.load()
    for (x, y), c in d.items():
        px[x, y] = tuple(c[:3]) + (255,)
    return im


def model(tex_base, tex_glow=None):
    m = {'parent': 'minecraft:item/generated'}
    if tex_glow:
        m['loader'] = 'neoforge:item_layers'
        m['textures'] = {'layer0': tex_base, 'layer1': tex_glow}
        m['neoforge_data'] = {'layers': {'1': {'block_light': 15, 'sky_light': 15}}}
    else:
        m['textures'] = {'layer0': tex_base}
    hide = {'scale': [0.0001, 0.0001, 0.0001]}
    m['display'] = {
        'thirdperson_righthand': hide, 'thirdperson_lefthand': hide,
        'firstperson_righthand': hide, 'firstperson_lefthand': hide,
        'fixed': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [1, 1, 1]},
    }
    return m


def main():
    os.makedirs(TEX, exist_ok=True)
    os.makedirs(MOD, exist_ok=True)
    sheet = Image.new('RGBA', (S * 8 * 4, S * 6 * 4), (58, 58, 70, 255))
    for ti, tp in enumerate(TYPES):
        # giai đoạn 0: texture 16x16 cũ
        with open(os.path.join(MOD, tp + '.json'), 'w', encoding='utf-8') as f:
            json.dump(model('hukhong:item/ban_menh/' + tp), f, indent=1)
        for stg in range(1, 7):
            base, glow = build(tp, stg)
            name = '%s_s%d' % (tp, stg)
            to_img(base).save(os.path.join(TEX, name + '.png'))
            has_glow = ST[stg]['glow']
            if has_glow:
                to_img(glow).save(os.path.join(TEX, name + '_glow.png'))
            with open(os.path.join(MOD, name + '.json'), 'w', encoding='utf-8') as f:
                json.dump(model('hukhong:item/ban_menh/' + name,
                                'hukhong:item/ban_menh/' + name + '_glow' if has_glow else None), f, indent=1)
            im = to_img(base).resize((S * 4, S * 4), Image.NEAREST)
            if has_glow:
                g = to_img(glow).resize((S * 4, S * 4), Image.NEAREST)
                im.alpha_composite(g)
            sheet.alpha_composite(im, (ti * S * 4, (stg - 1) * S * 4))
    # model gốc: chọn theo loại (custom_model_data) + giai đoạn (hukhong:stage)
    ov = []
    for stg in range(0, 7):
        for ti, tp in enumerate(TYPES):
            mdl = 'hukhong:item/ban_menh/' + (tp if stg == 0 else '%s_s%d' % (tp, stg))
            ov.append({'predicate': {'custom_model_data': ti + 1, 'hukhong:stage': stg}, 'model': mdl})
    top = {'parent': 'minecraft:item/handheld', 'textures': {'layer0': 'hukhong:item/ban_menh/thanh_phong'}, 'overrides': ov}
    with open(TOP, 'w', encoding='utf-8') as f:
        json.dump(top, f, indent=1)
    if '--sheet' in sys.argv:
        sheet.save(sys.argv[sys.argv.index('--sheet') + 1])
    print('ok')


if __name__ == '__main__':
    main()
