#!/usr/bin/env python3
"""Thổ Trụ 3 cấp (GeckoLib): tho_tru_1 (trụ đơn), tho_tru_2 (Hợp Thể), tho_tru_3 (Thái Sơn Thần Tháp) + tho_tru_3_ctl (lõi đỏ khi có người điều khiển).
Phong cách theo ảnh tham chiếu: gạch đá nâu/đen xếp từng viên, đá cát nhô ra, rêu, rune 土 vàng phát sáng, pha lê vàng lơ lửng, đá bay quanh.
Bone động (xoay bằng code ở PhapClient): crystal, orbit, orbit2, halo, upper (ẩn khi người lái nhìn)."""
import json, math, random, zlib
from PIL import Image
from _hum_engine import Model, hexc, shade, mix, FACE_SHADE, A

# ---------------------------------------------------------------- màu
DARK = [hexc(c) for c in ("3f3731", "4a4038", "54493f", "5e5145", "463d36")]
WARM = [hexc(c) for c in ("6e5a45", "7a6249", "86694c")]
SAND = [hexc(c) for c in ("a88c5f", "b89a6a", "c9ad7c")]
MORTAR = hexc("2a2420")
MOSS = [hexc(c) for c in ("5d6b22", "6f7f2a", "4f5a1c")]
GOLD, GOLD_D, GOLD_H = hexc("c9952c"), hexc("8a6418"), hexc("f1cf6a")
AMB, AMB_H, AMB_D = hexc("ffb72e"), hexc("ffe48a"), hexc("e08a14")
LAVA, LAVA_H = hexc("ff7a12"), hexc("ffc04a")
IRON = hexc("2e2a27")


def H(*a):
    return zlib.crc32(repr(a).encode()) / 4294967295.0


class Stone:
    """Gạch xếp: viên gạch lệch hàng, vữa tối, viền sáng/tối từng viên, nhiễu, rêu, vết nứt. overlay[face](x,y,w,h) -> (rgb, glow) | None."""
    def __init__(s, pal=DARK, seed=0, bw=6, bh=4, moss=0.0, mix_pal=None, mix_p=0.0, overlay=None, mortar=True):
        s.pal, s.seed, s.bw, s.bh, s.moss, s.mix_pal, s.mix_p = pal, seed, bw, bh, moss, mix_pal, mix_p
        s.ov = overlay or {}
        s.mortar = mortar

    def __call__(s, face, x, y, w, h):
        for k in (face, "side" if face not in ("up", "down") else None, "*"):
            f = s.ov.get(k)
            if f:
                r = f(x, y, w, h)
                if r is not None:
                    c, g = r
                    return (c if g else shade(c, FACE_SHADE[face])), g
        if face == "down":
            return shade(MORTAR, .8), False
        bw, bh = s.bw, s.bh
        row = y // bh
        off = (row * 3 + int(H(s.seed, face, row) * 3)) % bw
        col = (x + off) // bw
        if s.mortar and (y % bh == bh - 1 or (x + off) % bw == bw - 1):
            c = MORTAR
        else:
            u = H(s.seed, face, row, col)
            pal = s.mix_pal if (s.mix_pal and H(s.seed, "m", face, row, col) < s.mix_p) else s.pal
            c = pal[int(u * len(pal)) % len(pal)]
            if y % bh == 0: c = shade(c, 1.14)
            elif y % bh == bh - 2: c = shade(c, .86)
            if (x + off) % bw == 0: c = shade(c, 1.07)
            c = shade(c, 1 + (H(s.seed, x, y, face) - .5) * .12)
            if H(s.seed, "crack", face, row, col) < .08 and (x + y + row) % 3 == 0 and (x + off) % bw in (2, 3):
                c = shade(MORTAR, 1.1)
        if s.moss:
            mw = s.moss * (2.2 if face == "up" else (1.6 if y > h * .7 else .5))
            if H(s.seed, "moss", face, x // 2, y // 2) < mw and H(s.seed, "mz", face, x, y) < .75:
                c = MOSS[int(H(x, y, face) * 3)]
        if face not in ("up", "down") and (x == 0 or x == w - 1): c = shade(c, .88)
        return shade(c, FACE_SHADE[face]), False


def plain(col, glow=False, noise=.06, seed=0, edge=True):
    col = hexc(col) if isinstance(col, str) else col
    def p(face, x, y, w, h):
        c = shade(col, 1 + (H(seed, face, x, y) - .5) * 2 * noise)
        if edge and (x == 0 or y == 0 or x == w - 1 or y == h - 1): c = shade(c, .82 if not glow else .92)
        return (c if glow else shade(c, FACE_SHADE[face])), glow
    return p


def gold(seed=0):
    def p(face, x, y, w, h):
        c = GOLD
        if y == 0 or (face == "up" and (x == 0 or y == 0)): c = GOLD_H
        elif y == h - 1 or x == w - 1: c = GOLD_D
        c = shade(c, 1 + (H(seed, face, x, y) - .5) * .1)
        return shade(c, FACE_SHADE[face]), False
    return p


def crystal_paint(seed=0, hot=False):
    def p(face, x, y, w, h):
        t = (x / max(1, w - 1) + y / max(1, h - 1)) / 2
        c = mix(AMB_H, AMB_D, t)
        if face == "up": c = AMB_H
        if face == "down": c = AMB_D
        if (x + y) % 5 == 0: c = shade(c, 1.08)
        if x == 0 or y == 0: c = mix(c, (255, 250, 220), .5)
        return c, True
    return p


def lantern(seed=0):
    def p(face, x, y, w, h):
        if face in ("up", "down") or x == 0 or x == w - 1 or y == 0 or y == h - 1:
            return shade(IRON, FACE_SHADE[face] * (1.2 if y == 0 else 1)), False
        c = mix(AMB_H, AMB, abs(x - w / 2) / (w / 2) + abs(y - h / 2) / h)
        return c, True
    return p


# ---------------------------------------------------------------- overlay (hoạ tiết phát sáng)
TU = ["....A....", "....A....", ".AAAAAAA.", "....A....", "....A....", "....A....", "AAAAAAAAA"]


def rune_panel(cx_frac=.5, top=4, pw=11, ph=12, glyph=TU, channel=True, chan_w=1, frame=True):
    """Bảng đá lõm viền vàng, chữ 土 phát sáng, rãnh sáng chạy xuống đáy mặt."""
    gh, gw = len(glyph), len(glyph[0])
    def f(x, y, w, h):
        x0 = int(w * cx_frac - pw / 2)
        if x0 <= x < x0 + pw and top <= y < top + ph:
            lx, ly = x - x0, y - top
            if frame and (lx in (0, pw - 1) or ly in (0, ph - 1)):
                return (GOLD_H if ly == 0 else GOLD if lx == 0 else GOLD_D), False
            gx, gy = lx - (pw - gw) // 2, ly - (ph - gh) // 2
            if 0 <= gx < gw and 0 <= gy < gh and glyph[gy][gx] == "A":
                return (AMB_H if gy % 3 == 0 else AMB), True
            if 0 <= gx < gw and 0 <= gy < gh and any(0 <= gx + dx < gw and 0 <= gy + dy < gh and glyph[gy + dy][gx + dx] == "A" for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                return mix(AMB_D, MORTAR, .4), True
            return shade(MORTAR, 1.15 + .1 * ((lx + ly) % 2)), False
        if channel:
            mid = x0 + pw // 2
            if y >= top + ph and abs(x - mid) <= chan_w // 2 + (0 if chan_w % 2 else -1) + 0:
                if abs(x - mid) < max(1, chan_w // 2 + 1): return (AMB_H if (y // 2) % 3 == 0 else AMB), True
            if y >= top + ph and abs(x - mid) == chan_w // 2 + 1: return shade(GOLD_D, .9), False
        return None
    return f


def veins(seed, n=2, color=AMB, hi=AMB_H, y0=0.15, y1=0.95, wiggle=2):
    """Mạch sáng ngoằn ngoèo thẳng đứng (vết nứt rune / dung nham)."""
    def f(x, y, w, h):
        if not (h * y0 <= y <= h * y1): return None
        for i in range(n):
            bx = int(w * (i + 1) / (n + 1) + (H(seed, i) - .5) * w * .3)
            ox = int(round(math.sin(y * .45 + H(seed, i, 1) * 6) * wiggle * H(seed, i, 2)))
            if x == bx + ox:
                return (hi if H(seed, i, y) < .3 else color), True
            if (y + i) % 7 == 0 and x == bx + ox + (1 if H(seed, i, y, 3) < .5 else -1):
                return color, True
        return None
    return f


def key_rune(top=6):
    """Rune kiểu chìa khoá (mặt sau): vòng tròn + vạch dọc."""
    def f(x, y, w, h):
        cx = w // 2
        if top <= y < top + 5 and cx - 2 <= x <= cx + 2:
            if (y in (top, top + 4) and cx - 1 <= x <= cx + 1) or (x in (cx - 2, cx + 2) and top < y < top + 4):
                return AMB, True
            return None
        if y >= top + 5 and x == cx: return (AMB_H if y % 4 == 0 else AMB), True
        return None
    return f


def windows(seed, rows=(.35,), n=2, ww=2, wh=3, col=AMB):
    """Cửa sổ nhỏ phát sáng."""
    def f(x, y, w, h):
        for r in rows:
            wy = int(h * r)
            if wy <= y < wy + wh:
                for i in range(n):
                    wx = int(w * (i + 1) / (n + 1)) - ww // 2
                    if wx <= x < wx + ww:
                        return (AMB_H if y == wy else col), True
                    if x in (wx - 1, wx + ww) or y == wy + wh - 1 and wx - 1 <= x <= wx + ww:
                        pass
        return None
    return f


def combine(*fs):
    def f(x, y, w, h):
        for g in fs:
            if g is None: continue
            r = g(x, y, w, h)
            if r is not None: return r
        return None
    return f


# ---------------------------------------------------------------- dựng hình
class B:
    def __init__(s, m):
        s.m = m

    def box(s, bone, cx, y, cz, w, h, d, paint, rot=None, pivot=None):
        s.m.cube(bone, (cx - w / 2, y, cz - d / 2), (w, h, d), paint, rot=rot, pivot=pivot)

    def crystal(s, bone, cy, k, seed=0):
        """Pha lê hai chóp (đỉnh dài hơn), các lớp xoay 45°."""
        layers = [(2, 2), (4, 2), (6, 2), (8, 3), (7, 3), (6, 3), (4, 3), (2, 3)]
        y = cy - sum(h for _, h in layers[:3]) * k
        for i, (w, h) in enumerate(layers):
            ww, hh = max(1, round(w * k)), max(1, round(h * k))
            s.box(bone, 0, y, 0, ww, hh, ww, crystal_paint(seed + i), rot=[0, 45, 0], pivot=[0, y, 0])
            y += hh

    def rubble(s, bone, r, n, y, seed, sizes=(3, 6), pal=DARK, ring=True):
        rng = random.Random(seed)
        for i in range(n):
            a = rng.random() * math.tau
            rr = r * (0.85 + rng.random() * .3) if ring else r * rng.random()
            w, hgt, d = [rng.randint(*sizes) for _ in range(3)]
            x, z = math.cos(a) * rr, math.sin(a) * rr
            s.box(bone, x, y, z, w, hgt, d, Stone(pal if rng.random() < .6 else SAND, seed + i, bw=4, bh=3, moss=.12),
                  rot=[0, rng.uniform(-30, 30), rng.uniform(-12, 12)], pivot=[x, y, z])

    def studs(s, bone, cx, cz, half, y0, y1, n, seed, depth=1):
        """Viên đá nhô ra trên 4 mặt (tạo khối voxel lởm chởm như ảnh)."""
        rng = random.Random(seed)
        for i in range(n):
            side = i % 4
            w, hgt = rng.randint(3, 6), rng.randint(2, 4)
            t = rng.uniform(-half + w / 2 + 1, half - w / 2 - 1)
            y = rng.uniform(y0, y1 - hgt)
            pal = SAND if rng.random() < .45 else WARM
            p = Stone(pal, seed * 31 + i, bw=w + 1, bh=hgt + 1, moss=.1, mortar=False)
            if side == 0: s.box(bone, cx + t, y, cz - half - depth / 2 + .01, w, hgt, depth, p)
            elif side == 1: s.box(bone, cx + t, y, cz + half + depth / 2 - .01, w, hgt, depth, p)
            elif side == 2: s.box(bone, cx - half - depth / 2 + .01, y, cz + t, depth, hgt, w, p)
            else: s.box(bone, cx + half + depth / 2 - .01, y, cz + t, depth, hgt, w, p)

    def merlons(s, bone, half, y, size, mid=True, seed=0, hgt=None):
        hgt = hgt or size
        for sx in (-1, 1):
            for sz in (-1, 1):
                s.box(bone, sx * (half - size / 2), y, sz * (half - size / 2), size, hgt, size, Stone(DARK, seed + sx * 3 + sz, bw=4, bh=3, mix_pal=SAND, mix_p=.35, moss=.08))
        if mid:
            m = max(2, size - 2)
            for i, (x, z) in enumerate(((0, -half + m / 2), (0, half - m / 2), (-half + m / 2, 0), (half - m / 2, 0))):
                s.box(bone, x, y, z, m, hgt - 1, m, Stone(SAND, seed + 10 + i, bw=4, bh=3, mix_pal=DARK, mix_p=.4))

    def orbit(s, bone, cy, r, n, size, seed, jitter=3):
        rng = random.Random(seed)
        for i in range(n):
            a = math.tau * i / n + rng.uniform(-.15, .15)
            x, z = math.cos(a) * r, math.sin(a) * r
            y = cy + rng.uniform(-jitter, jitter)
            sz = size + rng.randint(-1, 1)
            s.box(bone, x, y, z, sz, sz, sz, Stone(DARK if i % 3 else WARM, seed + i, bw=3, bh=3, mix_pal=SAND, mix_p=.3),
                  rot=[rng.uniform(-30, 30), rng.uniform(0, 90), rng.uniform(-30, 30)], pivot=[x, y + sz / 2, z])

    def halo(s, bone, cy, r, n, seed):
        for i in range(n):
            a = math.tau * i / n
            x, z = math.cos(a) * r, math.sin(a) * r
            s.box(bone, x, cy, z, max(2, round(math.tau * r / n * .7)), 1, 1, plain("ffc85a", True, edge=False, seed=i),
                  rot=[0, -math.degrees(a) + 90, 0], pivot=[x, cy, z])


def anim_file():
    (A / "animations/entity").mkdir(parents=True, exist_ok=True)
    (A / "animations/entity/tho_tru.animation.json").write_text(json.dumps({"format_version": "1.8.0", "animations": {}}))


# ================================================================ CẤP 1: Thổ Trụ
def level1():
    m = Model("tho_tru_1", 256, 256, bounds=(3, 5), off=2)
    b = B(m)
    m.bone("root", pivot=(0, 0, 0))
    m.bone("body", "root", pivot=(0, 0, 0))
    # đế bậc thang
    b.box("body", 0, 0, 0, 26, 4, 26, Stone(DARK, 1, moss=.18, mix_pal=SAND, mix_p=.25))
    b.box("body", 0, 4, 0, 21, 4, 21, Stone(DARK, 2, moss=.15, mix_pal=SAND, mix_p=.3))
    b.box("body", 0, 8, 0, 17, 3, 17, Stone(WARM, 3, moss=.1, mix_pal=DARK, mix_p=.5))
    b.rubble("body", 12, 10, 0, 11, sizes=(3, 6))
    b.rubble("body", 9, 6, 4, 12, sizes=(2, 4))
    # thân
    front = combine(rune_panel(top=3, pw=9, ph=11, chan_w=1))
    b.box("body", 0, 11, 0, 13, 29, 13, Stone(DARK, 4, bw=5, bh=4, moss=.06, mix_pal=WARM, mix_p=.3, overlay={
        "north": front, "south": key_rune(5), "east": veins(41, 1, wiggle=1), "west": veins(42, 1, wiggle=1)}))
    b.studs("body", 0, 0, 6.5, 12, 38, 22, 5)
    # cổ vàng + vương miện răng cưa
    b.box("body", 0, 40, 0, 15, 2, 15, gold(6))
    b.box("body", 0, 42, 0, 17, 4, 17, Stone(DARK, 7, bw=4, bh=4, mix_pal=SAND, mix_p=.35, moss=.1))
    b.merlons("body", 8.5, 46, 5, seed=8)
    b.box("body", 0, 45.9, 0, 9, 1, 9, plain("ffcc55", True, edge=False))  # lòng vương miện sáng
    # pha lê + đá bay
    m.bone("crystal", "root", pivot=(0, 57, 0))
    b.crystal("crystal", 57, 1.0)
    m.bone("orbit", "root", pivot=(0, 56, 0))
    b.orbit("orbit", 54, 11, 5, 3, 20)
    m.build()


# ================================================================ CẤP 2: Hợp Thể
def level2():
    m = Model("tho_tru_2", 512, 512, bounds=(5, 6), off=2.5)
    b = B(m)
    m.bone("root", pivot=(0, 0, 0))
    m.bone("body", "root", pivot=(0, 0, 0))
    b.box("body", 0, 0, 0, 42, 4, 42, Stone(DARK, 101, moss=.18, mix_pal=SAND, mix_p=.25))
    b.box("body", 0, 4, 0, 37, 4, 37, Stone(DARK, 102, moss=.14, mix_pal=SAND, mix_p=.3))
    b.box("body", 0, 8, 0, 33, 3, 33, Stone(WARM, 103, moss=.1, mix_pal=DARK, mix_p=.5))
    # bậc thang trước
    for k in range(3):
        b.box("body", 0, 0, -21 - 3 + k * 2 + .01, 10, 3 + k * 3, 4, Stone(SAND, 110 + k, bw=5, bh=3, mix_pal=DARK, mix_p=.3))
    b.rubble("body", 20, 14, 0, 120, sizes=(3, 6))
    # 4 cột góc + đèn lồng
    for i, (sx, sz) in enumerate(((-1, -1), (1, -1), (-1, 1), (1, 1))):
        x, z = sx * 15, sz * 15
        b.box("body", x, 11, z, 7, 12, 7, Stone(DARK, 130 + i, bw=4, bh=4, mix_pal=SAND, mix_p=.3, moss=.1))
        b.box("body", x, 23, z, 8, 2, 8, gold(140 + i))
        b.box("body", x, 25, z, 5, 6, 5, lantern(150 + i))
        b.box("body", x, 31, z, 7, 2, 7, gold(160 + i))
        b.box("body", x, 33, z, 3, 2, 3, Stone(DARK, 170 + i, bw=3, bh=2))
    # thân chính + chân đế rộng
    b.box("body", 0, 11, 0, 24, 9, 24, Stone(DARK, 180, bw=6, bh=3, mix_pal=SAND, mix_p=.35, moss=.12, overlay={
        "north": combine(lambda x, y, w, h: ((AMB, True) if (w // 2 - 1 <= x <= w // 2) else None))}))
    front = rune_panel(top=6, pw=11, ph=13, chan_w=2)
    b.box("body", 0, 20, 0, 20, 26, 20, Stone(DARK, 181, bw=5, bh=4, moss=.06, mix_pal=WARM, mix_p=.3, overlay={
        "north": front, "south": key_rune(6), "east": combine(veins(182, 2), windows(183, (.15,), 1)),
        "west": combine(veins(184, 2), windows(185, (.15,), 1))}))
    # khung cổng vàng quanh bảng rune (mặt trước)
    b.box("body", 0, 39, -10.5, 14, 2, 1, gold(186))
    b.box("body", -7.5, 22, -10.5, 1, 19, 1, gold(187))
    b.box("body", 7.5, 22, -10.5, 1, 19, 1, gold(188))
    b.studs("body", 0, 0, 10, 21, 44, 26, 190)
    # tầng trên + tháp đỉnh
    b.box("body", 0, 46, 0, 24, 3, 24, gold(200))
    b.box("body", 0, 49, 0, 22, 5, 22, Stone(DARK, 201, bw=4, bh=5, mix_pal=SAND, mix_p=.4, moss=.08))
    b.merlons("body", 11, 54, 5, seed=202)
    b.box("body", 0, 49, 0, 14, 12, 14, Stone(DARK, 203, bw=4, bh=4, mix_pal=SAND, mix_p=.35, overlay={
        "side": lambda x, y, w, h: ((AMB, True) if (y == 3 and 2 < x < w - 3) else None)}))
    b.merlons("body", 7, 61, 4, mid=False, seed=204)
    b.box("body", 0, 60.9, 0, 8, 1, 8, plain("ffcc55", True, edge=False))
    m.bone("crystal", "root", pivot=(0, 72, 0))
    b.crystal("crystal", 72, 1.45)
    m.bone("orbit", "root", pivot=(0, 70, 0))
    b.orbit("orbit", 68, 17, 8, 4, 210)
    m.bone("halo", "root", pivot=(0, 70, 0))
    b.halo("halo", 70, 14, 20, 220)
    m.build()


# ================================================================ CẤP 3: Thái Sơn Thần Tháp
def level3():
    m = Model("tho_tru_3", 1024, 1024, bounds=(10, 12), off=5)
    b = B(m)
    m.bone("root", pivot=(0, 0, 0))
    m.bone("body", "root", pivot=(0, 0, 0))
    # nền 3 tầng
    for i, (w, y) in enumerate(((96, 0), (84, 8), (72, 16))):
        b.box("body", 0, y, 0, w, 8, w, Stone(DARK, 300 + i, bw=8, bh=4, moss=.16, mix_pal=SAND, mix_p=.22, overlay={
            "side": (lambda x, yy, ww, hh, i=i: ((LAVA, True) if (yy == 5 and (x // 4) % 5 == 2) else None))}))
    # cầu thang trước + lan can
    for k in range(6):
        b.box("body", 0, 0, -48 + 2 * k + 1 + .02, 20, 4 * (k + 1), 2, Stone(SAND, 310 + k, bw=5, bh=4, mix_pal=DARK, mix_p=.4))
    for sx in (-1, 1):
        for k in range(3):
            b.box("body", sx * 12, 0, -46 + 4 * k + .02, 4, 9 + 8 * k, 4, Stone(DARK, 320 + k + sx, bw=4, bh=4, mix_pal=SAND, mix_p=.3))
    b.rubble("body", 47, 26, 0, 330, sizes=(4, 9))
    # 4 tháp canh góc (mạch dung nham, cửa sổ sáng, gai đá chĩa ra)
    for i, (sx, sz) in enumerate(((-1, -1), (1, -1), (-1, 1), (1, 1))):
        x, z = sx * 37, sz * 37
        lava = combine(veins(340 + i, 2, LAVA, LAVA_H, .1, .9, 2), windows(350 + i, (.2, .55), 2, 2, 3))
        b.box("body", x, 0, z, 20, 42, 20, Stone(DARK, 360 + i, bw=6, bh=4, moss=.1, mix_pal=WARM, mix_p=.35, overlay={"side": lava}))
        b.studs("body", x, z, 10, 6, 40, 14, 370 + i)
        b.box("body", x, 42, z, 23, 3, 23, gold(380 + i))
        b.box("body", x, 45, z, 21, 4, 21, Stone(DARK, 390 + i, bw=4, bh=4, mix_pal=SAND, mix_p=.4))
        for j, (mx, mz) in enumerate(((-1, -1), (1, -1), (-1, 1), (1, 1))):
            b.box("body", x + mx * 8, 49, z + mz * 8, 5, 6, 5, Stone(DARK, 400 + i * 4 + j, bw=3, bh=3, mix_pal=SAND, mix_p=.4))
        b.box("body", x, 49, z, 9, 7, 9, lantern(410 + i))
        b.box("body", x, 56, z, 11, 2, 11, gold(415 + i))
        # gai đá: 2 gai mỗi tháp, nghiêng ra ngoài theo đường chéo
        for j, yy in enumerate((6, 26)):
            px, pz = x + sx * 11, z + sz * 11
            ang = math.degrees(math.atan2(sz, sx))
            for q, (w, hh) in enumerate(((6, 9), (4, 9), (2, 8))):
                b.box("body", px, yy + sum(h2 for _, h2 in ((6, 9), (4, 9), (2, 8))[:q]), pz, w, hh, w,
                      Stone(WARM if q else DARK, 420 + i * 8 + j * 3 + q, bw=3, bh=3, mix_pal=SAND, mix_p=.5, mortar=False),
                      rot=[sz * 38, 0, -sx * 38], pivot=[px, yy, pz])
    # cánh nối giữa tháp
    for sx in (-1, 1):
        b.box("body", sx * 31, 24, 0, 10, 26, 40, Stone(DARK, 430 + sx, bw=6, bh=4, moss=.1, mix_pal=SAND, mix_p=.3, overlay={
            "side": combine(windows(432 + sx, (.25,), 3, 2, 4), veins(434 + sx, 1, LAVA, LAVA_H, .55, .95, 1))}))
        for j, zz in enumerate((-16, -6, 4, 14)):
            b.box("body", sx * 33, 50, zz, 5, 4, 5, Stone(SAND if j % 2 else DARK, 436 + sx * 5 + j, bw=3, bh=3, mix_pal=DARK, mix_p=.4))
    # thân đài chính
    gate = combine(rune_panel(top=6, pw=19, ph=20, glyph=[r.replace("A", "AA").replace(".", "..") for r in TU for _ in (0, 1)], chan_w=3),
                   lambda x, y, w, h: ((LAVA if y % 3 else LAVA_H, True) if (h - 9 <= y and abs(x - w / 2 + .5) < 4) else None))
    b.box("body", 0, 24, 0, 54, 14, 54, Stone(DARK, 440, bw=8, bh=4, moss=.12, mix_pal=SAND, mix_p=.3, overlay={
        "north": lambda x, y, w, h: ((LAVA, True) if abs(x - w / 2 + .5) < 4 and y > 2 else None)}))
    b.box("body", 0, 38, 0, 48, 28, 48, Stone(DARK, 441, bw=6, bh=4, moss=.06, mix_pal=WARM, mix_p=.3, overlay={
        "north": gate, "south": key_rune(6), "east": combine(veins(442, 3), windows(443, (.25, .6), 2)),
        "west": combine(veins(444, 3), windows(445, (.25, .6), 2))}))
    b.box("body", 0, 59, -24.6, 22, 2, 1.2, gold(446))
    b.box("body", -10.5, 38, -24.6, 1, 21, 1.2, gold(447))
    b.box("body", 10.5, 38, -24.6, 1, 21, 1.2, gold(448))
    b.studs("body", 0, 0, 24, 39, 64, 40, 450)
    b.box("body", 0, 66, 0, 52, 3, 52, gold(460))
    b.box("body", 0, 69, 0, 50, 4, 50, Stone(DARK, 461, bw=5, bh=4, mix_pal=SAND, mix_p=.4))
    b.merlons("body", 25, 73, 6, seed=462)
    # tầng trên
    b.box("body", 0, 69, 0, 34, 22, 34, Stone(DARK, 470, bw=5, bh=4, moss=.05, mix_pal=WARM, mix_p=.35, overlay={
        "north": rune_panel(top=4, pw=11, ph=12, chan_w=1), "side": combine(veins(471, 2), windows(472, (.4,), 2))}))
    b.studs("body", 0, 0, 17, 70, 90, 20, 473)
    b.box("body", 0, 91, 0, 38, 3, 38, gold(480))
    b.box("body", 0, 94, 0, 30, 5, 30, Stone(DARK, 481, bw=4, bh=5, mix_pal=SAND, mix_p=.4, moss=.05))
    b.merlons("body", 15, 99, 5, seed=482)
    # ngai điều khiển (mặt ngồi ~ y102 = THO_TITAN_SEAT_Y)
    b.box("body", 0, 99, 0, 14, 3, 12, gold(490))
    b.box("body", 0, 99, 6.5, 14, 16, 3, Stone(DARK, 491, bw=4, bh=4, mix_pal=SAND, mix_p=.3, overlay={
        "north": rune_panel(top=3, pw=7, ph=8, channel=False)}))
    b.box("body", 0, 115, 6.5, 16, 2, 4, gold(492))
    for sx in (-1, 1):
        b.box("body", sx * 7.5, 99, 0, 2, 7, 12, gold(493 + sx))
    # phần trên (ẩn khi người lái nhìn): pha lê lõi + 2 vòng đá bay
    m.bone("upper", "root", pivot=(0, 0, 0))
    m.bone("crystal", "upper", pivot=(0, 128, 0))
    b.crystal("crystal", 128, 2.0)
    m.bone("orbit", "upper", pivot=(0, 124, 0))
    b.orbit("orbit", 122, 44, 12, 8, 500, 4)
    m.bone("orbit2", "upper", pivot=(0, 124, 0))
    b.orbit("orbit2", 126, 60, 16, 6, 520, 4)
    m.bone("halo", "upper", pivot=(0, 124, 0))
    b.halo("halo", 123, 52, 40, 540)
    m.build()
    # bản lõi đỏ khi có người điều khiển
    tex = Image.open(A / "textures/entity/tho_tru_3.png").convert("RGBA")
    gm = Image.open(A / "textures/entity/tho_tru_3_glowmask.png").convert("RGBA")
    T, G = tex.load(), gm.load()
    for yy in range(tex.height):
        for xx in range(tex.width):
            r, g, bb, a = T[xx, yy]
            if not a: continue
            if G[xx, yy][3]:
                nc = (min(255, int(r * 1.0 + 20)), int(g * .42), int(bb * .25), a)
                T[xx, yy] = nc
                G[xx, yy] = nc
            else:
                T[xx, yy] = (min(255, int(r * 1.12)), int(g * .9), int(bb * .85), a)
    tex.save(A / "textures/entity/tho_tru_3_ctl.png")
    gm.save(A / "textures/entity/tho_tru_3_ctl_glowmask.png")


if __name__ == "__main__":
    anim_file()
    level1()
    level2()
    level3()
