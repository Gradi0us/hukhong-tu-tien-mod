#!/usr/bin/env python3
"""C17: Phản Thần Dạ Khuyết dựng chi tiết theo ảnh concept (2026-11-07), độ phân giải GẤP ĐÔI
(1 khối = 32 đơn vị; renderer thu 0.5 -> cùng kích cỡ, mật độ texel gấp đôi).
Giữ tên xương cho hoạt ảnh + BossLook: right_leg left_leg body cloak tatter_1..4 right_arm left_arm crescent
crescent_tip_r hood head halo wing ph2_spikes ph3_halo.  Sinh thêm _p2/_p3 (tint theo pha) và nhân đôi
giá trị 'position' trong file hoạt ảnh cũ (chỉ lần đầu, đánh dấu bằng khoá meta).
Chạy: python tools/gen_c17_da_khuyet.py  (từ D:\\mc_mod)

Quy ước: model nhìn -z (north = mặt trước). Phía phải nhân vật = model -x.
FLIP: nếu ảnh chụp cho thấy nửa bị nhiễm ở sai bên thì đổi FLIP."""
import json, math, random, sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent))
from _hum_engine import Model, Paint, hexc, mix, shade, A, FACE_SHADE
from PIL import Image

FLIP = False

NAVY, NAVY_D, NAVY_L = hexc("232a5c"), hexc("161b3e"), hexc("323c7a")
BLK, BLK_L = hexc("0c0b16"), hexc("1d1a2e")
SIL, SIL_D, SIL_L = hexc("a9a9c4"), hexc("6c6c88"), hexc("d4d4e8")
VIO, VIO_D, VIO_L = hexc("a24cff"), hexc("55279a"), hexc("d6a8ff")
PALE, PALE_S = hexc("eee8f3"), hexc("c9bfd8")
HAIR, HAIR_S, HAIR_D = hexc("ece8f4"), hexc("c4bcd8"), hexc("8f86aa")


def neg_side(face, x, w, frac):
    """pixel nằm ở phía model -x (phía phải nhân vật) với tỉ lệ frac của bề ngang?"""
    left_is_neg = not FLIP
    if face == "north": return (x < w * frac) if left_is_neg else (x >= w * (1 - frac))
    if face == "south": return (x >= w * (1 - frac)) if left_is_neg else (x < w * frac)
    if face == ("west" if not FLIP else "east"): return True
    if face in ("up", "down"): return (x < w * frac) if left_is_neg else (x >= w * (1 - frac))
    return False


class Veins:
    """Mặt nạ vết nứt: đường gấp khúc liền mạch (random walk có nhánh), tính 1 lần cho mỗi (mặt, kích thước)."""
    def __init__(s, seed, density=110):
        s.seed, s.density, s.cache = seed, density, {}

    def mask(s, face, w, h):
        k = (face, w, h)
        if k in s.cache: return s.cache[k]
        r = random.Random(hash((s.seed, face, w, h)) & 0xffffffff)
        core, halo = set(), set()
        n = max(1, (w * h) // s.density)
        for _ in range(n):
            x, y = r.randrange(w), r.randrange(h)
            dx = r.choice((-1, 1))
            for step in range(int((w + h) * .7)):
                core.add((x, y))
                for ax, ay in ((1, 0), (-1, 0), (0, 1), (0, -1)): halo.add((x + ax, y + ay))
                t = r.random()
                if t < .45: y += 1
                elif t < .8: x += dx
                elif t < .9: y -= 1
                else: dx = -dx
                if r.random() < .06:                                             # nhánh ngắn
                    bx, by = x, y
                    for _ in range(r.randint(2, 5)):
                        bx += r.choice((-1, 1)); by += r.choice((0, 1)); core.add((bx, by))
                if not (0 <= x < w and 0 <= y < h): break
        s.cache[k] = (core, halo - core)
        return s.cache[k]


class Robe:
    """Áo: nền navy, nửa phía phải (model -x) bị Hư Không ăn (đen sần + vết nứt tím phát sáng),
    viền bạc ở mép dưới / hai mép, dải tím chuyển màu ở gấu, gấu rách (trong suốt)."""
    def __init__(s, seed, corrupt=0.0, base=NAVY, trim_bottom=0, trim_sides=False, fringe=0, purple_hem=0,
                 all_corrupt=False, veins=True, extra=None):
        s.r = random.Random(seed); s.v = Veins(seed)
        s.corrupt, s.base, s.tb, s.ts, s.fringe, s.ph, s.allc, s.veins, s.extra = corrupt, base, trim_bottom, trim_sides, fringe, purple_hem, all_corrupt, veins, extra

    def __call__(s, face, x, y, w, h):
        if s.extra:
            e = s.extra(face, x, y, w, h)
            if e is not None: return e
        side = face not in ("up", "down")
        if s.fringe and side and y >= h - s.fringe:
            cut = (x * 7 + (x // 2) * 3) % (s.fringe + 2)
            if y >= h - cut: return (0, 0, 0, 0), False
        edge_noise = (x * 13 + y * 7) % 5 / 10
        cor = s.allc or (s.corrupt > 0 and neg_side(face, x, w, s.corrupt + edge_noise * .3 - .1))
        if cor:
            lump = ((x // 2) * 31 + (y // 2) * 17) % 7
            c = BLK_L if lump == 0 else (mix(BLK, NAVY_D, .4) if lump == 1 else BLK)
        else:
            c = s.base
            if (x * 5 + y * 11) % 23 == 0: c = NAVY_D
            if (x + y * 3) % 29 == 0: c = NAVY_L
        if s.ph and side and y >= h - s.ph:
            t = (y - (h - s.ph)) / max(1, s.ph - 1)
            c = mix(c, VIO_D if t < .6 else VIO, .35 + .55 * t)
        if s.tb and side and h - s.tb - (s.ph or 0) <= y < h - (s.ph or 0): c = SIL if y % 2 == 0 else SIL_D
        if s.ts and face in ("north", "south") and x in (0, w - 1) and h > 6: c = SIL
        glow = False
        if s.veins and cor and side:
            core, halo = s.v.mask(face, w, h)
            if (x, y) in core: c, glow = (VIO if (x + y) % 3 else VIO_L), True
            elif (x, y) in halo: c = VIO_D
        f = FACE_SHADE[face] * (1 + (s.r.random() - .5) * .08)
        return shade(c, f), glow


def flat(c, glow=False, noise=.04, seed=1):
    r = random.Random(seed)
    def p(face, x, y, w, h):
        return shade(c, FACE_SHADE[face] * (1 + (r.random() - .5) * noise * 2)), glow
    return p


def silver(seed=2, gem=None, art=None, pal=None):
    """Bạc: viền sáng, giữa tối, (tuỳ chọn) ngọc tím/ảnh pixel ở mặt trước."""
    r = random.Random(seed)
    pal = {k: hexc(v) if isinstance(v, str) else v for k, v in (pal or {}).items()}
    def p(face, x, y, w, h):
        if art and face == "north":
            ah, aw = len(art), len(art[0])
            ax = x - (w - aw) // 2; ay = y - (h - ah) // 2
            if 0 <= ax < aw and 0 <= ay < ah and art[ay][ax] != ".":
                ch = art[ay][ax]
                return shade(pal[ch], 1.0), ch in "VG"
        c = SIL
        if x == 0 or y == 0: c = SIL_L
        elif x == w - 1 or y == h - 1: c = SIL_D
        elif (x * 3 + y * 5) % 11 == 0: c = mix(SIL, SIL_D, .5)
        return shade(c, FACE_SHADE[face] * (1 + (r.random() - .5) * .06)), False
    return p


def hair(seed=3, dark_side=0.0, fringe=0, dark_patch=None):
    r = random.Random(seed)
    def p(face, x, y, w, h):
        if fringe and face not in ("up", "down"):
            cut = (x * 5 + (x // 3) * 7) % (fringe + 3)
            if y >= h - cut: return (0, 0, 0, 0), False
        strand = (x + (y // 6)) % 4
        c = HAIR if strand else HAIR_S
        if (x * 7 + y) % 19 == 0: c = HAIR_D
        if dark_patch and dark_patch(face, x, y, w, h):
            c = BLK if (x + y) % 3 else VIO_D
        return shade(c, FACE_SHADE[face] * (1 + (r.random() - .5) * .05)), False
    return p


def build():
    m = Model("da_khuyet", 512, bounds=(8, 8), off=2.4)
    cube = m.cube

    # ------------------------------------------------------------------ chân (ủng)
    def boot(face, x, y, w, h):
        c = BLK
        if y in (h - 5, h - 6, h - 15, h - 16): c = SIL if x % 3 else SIL_D
        if y >= h - 2: c = SIL_D
        if face == "north" and y >= h - 4 and 2 <= x < w - 2: c = BLK_L
        return shade(c, FACE_SHADE[face]), False
    for nm, px, x0 in (("right_leg", -5, -9.5), ("left_leg", 5, 1.5)):
        m.bone(nm, None, (px, 26, 0))
        cube(nm, (x0, 0, -4.5), (8, 26, 9), boot)

    # ------------------------------------------------------------------ thân
    m.bone("body", None, (0, 26, 0))
    # thân trên: nửa phải bị nhiễm, vạt chéo bạc, áo lót tối ở giữa
    def torso_extra(face, x, y, w, h):
        if face != "north": return None
        mid = w / 2 - .5
        d = abs(x - mid)
        if d <= 1.5 and y > 6: return shade(NAVY_D, 1), False                         # áo lót
        if abs(d - (2 + y * .45)) < .8 and y < 26: return shade(SIL, 1), False         # ve áo chéo
        return None
    cube("body", (-11, 62, -6), (22, 30, 12), Robe(11, corrupt=.5, extra=torso_extra))
    cube("body", (-6, 88, -7), (12, 6, 13), Robe(12, base=BLK, trim_bottom=0, extra=lambda f, x, y, w, h: (shade(SIL, FACE_SHADE[f]), False) if y == 0 and f != "down" else None))   # cổ áo cao
    for x0, y0, sz in ((-13, 74, (8, 12, 3)), (-12, 64, (6, 9, 2)), (-9, 83, (6, 7, 2))):          # lông đen nhiễm che ngực phải
        cube("body", (x0, y0, -7.5), sz, Robe(20 + y0, all_corrupt=True))
    # đai bạc + mặt ngọc
    cube("body", (-12, 62, -7), (24, 6, 14), silver(4))
    medal = ["..SSSS..", ".SDDDDS.", "SDVVVVDS", "SDVGGVDS", "SDVGGVDS", "SDVVVVDS", ".SDDDDS.", "..SSSS.."]
    cube("body", (-4, 61, -8.6), (8, 8, 2), silver(5, art=medal, pal={"S": SIL_L, "D": SIL_D, "V": VIO, "G": VIO_L}))
    for x in (-10, -7, 6, 9):
        cube("body", (x - .5, 63, -7.6), (2, 3, 1), silver(6))
    # vạt trước dưới đai: tấm bạc viền + hình thoi + xích treo
    diamond = ["...SS...", "..SDDS..", ".SDVVDS.", "SDVGGVDS", ".SDVVDS.", "..SDDS..", "...SS..."]
    def panel(face, x, y, w, h):
        if face == "north":
            if x in (0, w - 1): return shade(SIL, 1), False
            if y in (6, 7) and 1 <= x < w - 1: return shade(SIL_D, 1), False
            ay, ax = y - 12, x - (w - 8) // 2
            if 0 <= ay < 7 and 0 <= ax < 8 and diamond[ay][ax] != ".":
                ch = diamond[ay][ax]
                return shade({"S": SIL_L, "D": SIL_D, "V": VIO, "G": VIO_L}[ch], 1), ch in "VG"
        return shade(NAVY_D, FACE_SHADE[face]), False
    cube("body", (-5, 20, -8.2), (10, 42, 1), panel)
    for x in (-8, 7):
        cube("body", (x - .5, 46, -8.4), (1, 16, 1), silver(7))
        cube("body", (x - 1.5, 40, -8.6), (3, 6, 1), silver(8, art=["SSS", "SVS", "SVS", "SSS"], pal={"S": SIL, "V": VIO}))
        cube("body", (x - .5, 32, -8.4), (1, 8, 1), silver(9))
        cube("body", (x - 1, 28, -8.6), (2, 4, 1), flat(BLK))
    # vạt áo dài + lớp gấu loe rách, chuyển tím
    cube("body", (-15, 14, -8), (30, 48, 16), Robe(13, corrupt=.45, trim_sides=True))
    cube("body", (-17, 2, -9), (34, 14, 18), Robe(14, corrupt=.4, fringe=6, purple_hem=7))

    # ------------------------------------------------------------------ áo choàng sau + tua rách
    m.bone("cloak", "body", (0, 90, 7))
    back = ["." * 16] * 58 + ["......SSSS......", ".....SDVVDS.....", "....SDVGGVDS....", ".....SDVVDS.....", "......SSSS......"] + ["." * 16] * 21
    def cloak_p(face, x, y, w, h):
        if face == "south":
            if x in (2, 3, w - 3, w - 4) and y > 4: return shade(SIL, .9), False
            if y in (6, 7, 22, 23) and 2 <= x < w - 2: return shade(SIL_D, .9), False
            ay, ax = y, x - (w - 16) // 2
            if 0 <= ay < len(back) and 0 <= ax < 16 and back[ay][ax] != ".":
                ch = back[ay][ax]
                return shade({"S": SIL_L, "D": SIL_D, "V": VIO, "G": VIO_L}[ch], .9), ch in "VG"
            if 6 < x < w - 7 and y % 9 == 0 and y < 30: return shade(SIL_D, .85), False          # xích ngang
        return Robe(15, corrupt=0, purple_hem=8, fringe=4)(face, x, y, w, h)
    m.cube("cloak", (-12, 6, 7.2), (24, 84, 1), cloak_p)
    for i, x in enumerate((-13, -5, 5, 13)):
        m.bone(f"tatter_{i + 1}", "cloak", (x, 20, 8))
        m.cube(f"tatter_{i + 1}", (x - 3, 0, 8.3), (6, 22, 1), Robe(30 + i, corrupt=.0, fringe=6, purple_hem=9))

    # ------------------------------------------------------------------ tay áo rộng + vai giáp ba lớp
    for nm, px, x0, side in (("right_arm", -15, -21, -1), ("left_arm", 15, 9, 1)):
        m.bone(nm, "body", (px, 88, 0))
        cor = side < 0
        cube(nm, (x0, 58, -6), (12, 30, 12), Robe(40 + side, all_corrupt=cor, base=NAVY))
        cube(nm, (x0 - 1, 50, -7), (14, 10, 14), Robe(42 + side, all_corrupt=cor, trim_bottom=3, base=NAVY))   # tay áo loe
        cube(nm, (x0 + 2, 43, -4), (8, 8, 8), lambda f, x, y, w, h: (shade(PALE_S if (x + y) % 9 == 0 else PALE, FACE_SHADE[f]), False))
        pad = lambda s: Robe(s, base=BLK, all_corrupt=cor, trim_bottom=2, veins=cor)
        cube(nm, (x0 - 3, 84, -9), (18, 8, 18), pad(44 + side))
        cube(nm, (x0 - 2, 78, -8.5), (16, 7, 17), Robe(46 + side, base=NAVY, all_corrupt=cor, trim_bottom=2))
        cube(nm, (x0 - 1, 73, -8), (14, 6, 16), pad(48 + side))
        cube(nm, (x0 - 3, 91.5, -9.2), (18, 1, 1), silver(50)); cube(nm, (x0 - 3, 91.5, 8.2), (18, 1, 1), silver(51))   # viền bạc mép vai

    # ------------------------------------------------------------------ lưỡi hái trăng (tay phải)
    m.bone("crescent", "right_arm", (-15, 47, -2))
    shaft = lambda f, x, y, w, h: ((shade(VIO, 1), True) if y % 10 == 4 else (shade(BLK_L if x % 2 else BLK, FACE_SHADE[f]), False))
    cube("crescent", (-16, 24, -3.5), (2, 38, 3), shaft)
    cube("crescent", (-17.5, 38, -5), (5, 6, 6), lambda f, x, y, w, h: ((shade(VIO_L if (x + y) % 3 == 0 else VIO, 1), True)))   # pha lê tím ở cán
    cube("crescent", (-17, 22, -4), (4, 3, 4), silver(60))
    for k in range(17):                                                                         # lưỡi cong rủ xuống, mũi hếch lên
        deg = -5 - k * 10.5
        a = math.radians(deg)
        R = 13
        cx, cy = -28 + math.cos(a) * R, 26 + math.sin(a) * R
        t = 5 if 3 < k < 12 else (4 if k < 15 else 3)
        edge = SIL_L if k % 2 else hexc("c9c3ec")
        cube("crescent", (cx - t / 2, cy - t / 2, -4), (t, t, 2), lambda f, x, y, w, h, e=edge: (shade(e if y < 2 else SIL_D, FACE_SHADE[f]), False))
        ia = math.radians(deg); ri = R - 3.5
        ix, iy = -28 + math.cos(ia) * ri, 26 + math.sin(ia) * ri
        cube("crescent", (ix - 1.5, iy - 1.5, -3.5), (3, 3, 1), lambda f, x, y, w, h: ((shade(VIO_D if (x + y) % 2 else VIO, 1), (x + y) % 2 == 0)))
    m.bone("crescent_tip_r", "crescent", (-40, 30, -2))
    cube("crescent_tip_r", (-42, 28, -4), (2, 6, 2), lambda f, x, y, w, h: (shade(VIO_L, 1), True))

    # ------------------------------------------------------------------ tóc trắng dài (hood)
    m.bone("hood", "body", (0, 92, 0))
    cube("hood", (-9, 46, 7.5), (18, 48, 3), hair(70, fringe=7))                                  # tóc sau tới thắt lưng
    cube("hood", (-8, 40, 9.5), (6, 20, 1), hair(71, fringe=5)); cube("hood", (2, 42, 9.5), (6, 18, 1), hair(72, fringe=5))
    cube("hood", (-12, 62, -5), (3, 30, 5), hair(73, fringe=5))                                   # lọn tóc trước ngực
    cube("hood", (9, 58, -5), (3, 34, 5), hair(74, fringe=6))

    # ------------------------------------------------------------------ đầu
    m.bone("head", "body", (0, 92, 0))
    face = ["HHHHHHHHHHHHDDDD",
            "HHHHHHHHHHHDDDDD",
            "HHhHHHHHhHHHDDDD",
            "HHHHHhHHHHHHHDDH",
            "HHHSHHHHHHSHHHHH",
            "HHSSShHHhSSSHhHH",
            "HSSSSSHHSSSSSSHH",
            "HSKKKSSSSSKKKSHH",
            "HSEEWSSSSSWEESSH",
            "HSeEESSSSSEEeSSH",
            "HSSSSSSSSSSSSSSH",
            "HSSSSSSsSSSSSSSH",
            "HHSSSSSSSSSSSSHH",
            "HHSSSSSmmSSSSSHH",
            "HHHSSSSSSSSSSHHH",
            "HHHHSSSSSSSSHHHH"]
    if FLIP: face = [row[::-1] for row in face]
    FP = {"H": HAIR, "h": HAIR_S, "D": BLK, "S": PALE, "s": PALE_S, "K": hexc("2a1a44"), "E": VIO, "e": VIO_D, "W": VIO_L, "m": hexc("b49cc4")}
    def headp(face_, x, y, w, h):
        if face_ == "north":
            ch = face[y][x]
            return shade(FP[ch], 1), ch in "EW"
        return hair(80, dark_patch=lambda f, x, y, w, h: (f == ("east" if not FLIP else "west") and y < 7 and x < 9) or (f == "up" and x > 10 and y < 6))(face_, x, y, w, h)
    cube("head", (-8, 92, -8), (16, 16, 16), headp)
    cube("head", (-9, 102, -9), (18, 6, 18), hair(81, dark_patch=lambda f, x, y, w, h: f in ("north", "up") and ((x > 12) != FLIP) and y < 5))   # mái tóc phồng
    cube("head", (-9.5, 84, -8), (2, 20, 14), hair(82, fringe=4)); cube("head", (7.5, 84, -8), (2, 20, 14), hair(83, fringe=4))   # tóc hai bên má
    bang = lambda f, x, y, w, h: ((0, 0, 0, 0), False) if f == "north" and (y >= 2 + (x * 3) % 5) else hair(84)(f, x, y, w, h)
    cube("head", (-8.5, 99, -9), (17, 7, 1), bang)                                                  # tóc mái lơ thơ trước trán

    # vầng trăng khuyết lớn sau đầu: dày ở hai bên/đáy, mảnh nhọn ở đầu mút, hở phía trên-phải
    CX, CY, CZ, R = 0, 114, 11, 22
    ring = Veins(90, density=70)
    for k in range(80):
        deg = -60 + k * 4.5                                       # -60 .. ~293 (bỏ khe 62..104)
        if 62 < (deg % 360) < 104: continue
        a = math.radians(deg)
        span = (deg + 60) / 353.0
        tip = min(span, 1 - span)                                 # 0 ở hai mũi
        th = 2 + round(6 * min(1, tip * 3.5))
        x, y = CX + math.cos(a) * R, CY + math.sin(a) * R
        def rp(f, px, py, w, h, k=k):
            core, halo = ring.mask(f, w, h)
            if (px + k // 3, py) in core: return shade(VIO, 1), True
            if k % 5 == 2 and f in ("north", "south") and abs(px - py) <= 0: return shade(VIO_L if py % 2 else VIO, 1), True   # vết nứt chéo
            if (px + k, py) in halo: return shade(VIO_D, 1), False
            return shade(BLK_L if (px + py + k) % 4 == 0 else BLK, FACE_SHADE[f]), False
        cube("head", (x - th / 2, y - th / 2, CZ), (th, th, 4), rp)
    cube("head", (-5, CY - R - 3, CZ - .5), (10, 5, 4), silver(91, art=["..........", "....VV....", "...VGGV...", "....VV....", ".........."], pal={"V": VIO, "G": VIO_L}))
    for x, ln in ((-23, 10), (23, 10), (-15, 7), (15, 7)):                                          # tua treo
        top = CY - 2
        cube("head", (x - .5, top - ln, CZ + 1), (1, ln, 1), silver(92))
        cube("head", (x - 1.5, top - ln - 6, CZ + .5), (3, 6, 2), lambda f, px, py, w, h: (shade(BLK if py > 0 else SIL, FACE_SHADE[f]), False))
        cube("head", (x - .5, top - ln - 9, CZ + 1), (1, 3, 1), silver(93))

    # ------------------------------------------------------------------ pha 2+: quầng trăng sáng
    m.bone("halo", "body", (0, 114, 14))
    for k in range(36):
        a = math.radians(k * 10)
        cube("halo", (math.cos(a) * 30 - 1.5, 114 + math.sin(a) * 30 - 1.5, 15), (3, 3, 1), lambda f, x, y, w, h: (shade(hexc("d8c8ff"), 1), True))
    # pha 3: cánh mảnh hư không
    m.bone("wing", "body", (0, 80, 9))
    for r, l, x0 in ((-35, 48, -4), (-60, 40, -4), (-80, 30, -4), (35, 48, 2), (60, 40, 2), (80, 30, 2)):
        cube("wing", (x0, 80, 9), (3, l, 2), lambda f, x, y, w, h: (shade(VIO if (x + y) % 4 else VIO_L, 1), True), rot=[-20, 0, r], pivot=[x0 + 1.5, 80, 10])
    # pha 2: gai pha lê sau lưng
    m.bone("ph2_spikes", "body", (0, 60, 9))
    for x, h, r in ((-8, 16, -24), (-3, 22, -8), (3, 20, 8), (8, 14, 24), (-12, 12, -36), (12, 10, 36)):
        cube("ph2_spikes", (x - 1.5, 60, 9), (3, h, 3), lambda f, px, py, w, hh: (shade(hexc("7a3ad8") if (px + py) % 3 else VIO_L, 1), True), rot=[-25, 0, r], pivot=[x, 60, 10])
    # pha 3: vương miện mảnh trăng
    m.bone("ph3_halo", "head", (0, 112, 0))
    for k in range(10):
        a = math.radians(k * 36)
        cube("ph3_halo", (math.cos(a) * 12 - 1, 108, math.sin(a) * 12 - 1), (2, 8 + (k % 2) * 4, 2), lambda f, x, y, w, h: (shade(hexc("e6d6ff"), 1), True))

    m.build()


def phase_textures():
    T = A / "textures/entity"
    base = Image.open(T / "da_khuyet.png").convert("RGBA")
    gm = Image.open(T / "da_khuyet_glowmask.png").convert("RGBA")
    theme = (143, 168, 255)
    for ph, amt in ((2, .12), (3, .24)):
        im = base.copy(); px = im.load()
        for j in range(im.height):
            for i in range(im.width):
                r, g, b, a = px[i, j]
                if a: px[i, j] = tuple(int(c + (t - c) * amt) for c, t in zip((r, g, b), theme)) + (a,)
        im.save(T / f"da_khuyet_p{ph}.png"); gm.save(T / f"da_khuyet_p{ph}_glowmask.png")


def scale_anim_positions():
    p = A / "animations/entity/da_khuyet.animation.json"
    a = json.loads(p.read_text())
    mark = Path(__file__).with_name(".dk_anim_scaled2")
    if mark.exists(): return
    for an in a["animations"].values():
        for b in an.get("bones", {}).values():
            pos = b.get("position")
            if isinstance(pos, dict):
                for k, v in pos.items():
                    if isinstance(v, list): pos[k] = [x * 2 if isinstance(x, (int, float)) else x for x in v]
                    elif isinstance(v, dict):
                        for kk in ("pre", "post", "vector"):
                            if isinstance(v.get(kk), list): v[kk] = [x * 2 if isinstance(x, (int, float)) else x for x in v[kk]]
            elif isinstance(pos, list):
                b["position"] = [x * 2 if isinstance(x, (int, float)) else x for x in pos]
    p.write_text(json.dumps(a, indent=1))
    mark.write_text("1")


if __name__ == "__main__":
    build()
    phase_textures()
    scale_anim_positions()
    print("ok")
