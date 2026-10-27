"""Hu Khong void art set: block textures (+animated mcmeta), sky textures, boss bar frames, Hu Hoa icon.
Palette: indigo/black base, violet+cyan veins, desaturated. Original procedural art (value noise + seeded RNG).
Writes to assets/hukhong and mirrors to tools/resourcepack/HuKhong-Void/. Run: python tools/gen_void_pack.py"""
import json, math, os, random

ROOT = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(ROOT, "..", "src", "main", "resources", "assets", "hukhong")
PACK = os.path.join(ROOT, "resourcepack", "HuKhong-Void")
from PIL import Image

BASE = [(10, 8, 20), (17, 13, 34), (26, 20, 50), (38, 30, 70)]
VIOLET = (150, 100, 220)
CYAN = (90, 210, 230)
WHITE = (225, 215, 250)
OUTS = []


def save(im, rel, mcmeta=None):
    for base in (ASSETS, os.path.join(PACK, "assets", "hukhong")):
        p = os.path.join(base, rel)
        os.makedirs(os.path.dirname(p), exist_ok=True)
        im.save(p)
        if mcmeta is not None:
            with open(p + ".mcmeta", "w") as f:
                json.dump(mcmeta, f)
    OUTS.append(rel)


def vnoise(w, h, seed, cell):
    r = random.Random(seed)
    gw, gh = w // cell + 2, h // cell + 2
    g = [[r.random() for _ in range(gw)] for _ in range(gh)]
    for i in range(gh):
        g[i][gw - 1] = g[i][0]
    g[gh - 1] = g[0][:]

    def at(x, y):
        fx, fy = x / cell, y / cell
        x0, y0 = int(fx) % (gw - 1), int(fy) % (gh - 1)
        tx, ty = fx - int(fx), fy - int(fy)
        tx = tx * tx * (3 - 2 * tx)
        ty = ty * ty * (3 - 2 * ty)
        a = g[y0][x0] * (1 - tx) + g[y0][x0 + 1] * tx
        b = g[y0 + 1][x0] * (1 - tx) + g[y0 + 1][x0 + 1] * tx
        return a * (1 - ty) + b * ty
    return at


def mix(a, b, t):
    t = max(0, min(1, t))
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def base_tile(seed, size=16, bright=1.0):
    n1, n2 = vnoise(size, size, seed, 8), vnoise(size, size, seed + 7, 4)
    im = Image.new("RGBA", (size, size))
    px = im.load()
    for y in range(size):
        for x in range(size):
            v = (n1(x, y) * 0.6 + n2(x, y) * 0.4) * bright
            px[x, y] = BASE[max(0, min(3, int(v * 4)))] + (255,)
    return im


def veins(im, seed, n=3, col=VIOLET, glow=CYAN):
    r = random.Random(seed)
    px = im.load()
    s = im.size[0]
    for _ in range(n):
        x, y = r.randrange(s), r.randrange(s)
        dx, dy = r.choice([(1, 0), (0, 1), (1, 1), (1, -1)])
        for _ in range(r.randint(5, 10)):
            px[x % s, y % s] = col + (255,)
            if r.random() < .35:
                px[(x + 1) % s, y % s] = mix(col, glow, .6) + (255,)
            x += dx + (r.choice([0, 0, 1, -1]) if dy == 0 else 0)
            y += dy + (r.choice([0, 0, 1, -1]) if dx == 0 else 0)
    return im


def speck(im, seed, n, col):
    r = random.Random(seed)
    px = im.load()
    for _ in range(n):
        px[r.randrange(im.size[0]), r.randrange(im.size[1])] = col + (255,)


def anim(frames, time, interp=False):
    w = frames[0].size[0]
    im = Image.new("RGBA", (w, w * len(frames)))
    for i, f in enumerate(frames):
        im.paste(f, (0, i * w))
    return im, {"animation": {"frametime": time, "interpolate": interp}}


# ---- blocks ----
dat = veins(base_tile(1), 11, 1)
speck(dat, 12, 6, (60, 48, 100))
save(dat, "textures/block/hu_khong_dat.png")
grass_top = base_tile(2, bright=1.15)
speck(grass_top, 21, 22, (74, 90, 150))
speck(grass_top, 22, 4, CYAN)
save(grass_top, "textures/block/hu_khong_co_top.png")
save(grass_top, "textures/block/hu_khong_co.png")
side = dat.copy()
sp = side.load()
r = random.Random(31)
for x in range(16):
    h = r.randint(2, 4)
    for y in range(h):
        sp[x, y] = grass_top.getpixel((x, y))
    if r.random() < .5:
        sp[x, h] = (46, 58, 110, 255)
save(side, "textures/block/hu_khong_co_side.png")
save(dat, "textures/block/hu_khong_co_bottom.png")
da = base_tile(3, bright=.85)
for y in range(0, 16, 5):
    for x in range(16):
        if (x * 7 + y * 3) % 5 < 3:
            da.putpixel((x, (y + x // 6) % 16), (8, 6, 16, 255))
veins(da, 41, 1, (80, 70, 150), (120, 130, 200))
save(da, "textures/block/hu_khong_da.png")


def crystal(f, n=8):
    im = Image.new("RGBA", (16, 16), (12, 9, 28, 255))
    px = im.load()
    ph = f / n * 2 * math.pi
    nz = vnoise(16, 16, 51, 5)
    for y in range(16):
        for x in range(16):
            facet = ((x + y) // 3 + (x - y) // 4) % 3
            v = .35 + .2 * facet + .35 * nz(x, y) + .25 * math.sin(ph + (x + y) * .5)
            px[x, y] = mix((40, 28, 100), mix(VIOLET, CYAN, nz(x + 3, y)), v) + (255,)
    k = .5 + .5 * math.sin(ph)
    for (x, y) in [(7, 7), (8, 7), (7, 8), (8, 8)]:
        px[x, y] = mix(CYAN, WHITE, k) + (255,)
    for i in range(16):
        px[i, 0] = px[i, 15] = px[0, i] = px[15, i] = (14, 10, 34, 255)
    return im


im, meta = anim([crystal(i) for i in range(8)], 3, True)
save(im, "textures/block/hu_khong_tinh.png", meta)


def wood(top):
    im = Image.new("RGBA", (16, 16))
    px = im.load()
    nz = vnoise(16, 16, 61, 4)
    for y in range(16):
        for x in range(16):
            if top:
                d = math.hypot(x - 7.5, y - 7.5)
                v = (math.sin(d * 1.6) + 1) / 2 * .6 + nz(x, y) * .4
                if d > 7:
                    v *= .5
            else:
                v = ((x % 4 in (0, 1)) * .3 + nz(x * 3, y) * .5) * (1 if x % 4 else .6)
            px[x, y] = BASE[max(0, min(3, int(v * 4)))] + (255,)
    if not top:
        r = random.Random(62)
        for _ in range(3):
            x, y = r.randrange(16), r.randrange(10)
            for k in range(5):
                px[x, y + k] = VIOLET + (255,) if k % 2 == 0 else (70, 52, 130, 255)
    else:
        px[7, 7] = px[8, 8] = (70, 52, 130, 255)
    return im


save(wood(True), "textures/block/hu_khong_go_top.png")
save(wood(False), "textures/block/hu_khong_go_side.png")
save(wood(False), "textures/block/hu_khong_go.png")

leaf = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
lp = leaf.load()
r = random.Random(71)
nz = vnoise(16, 16, 72, 3)
for y in range(16):
    for x in range(16):
        if r.random() < .82:
            lp[x, y] = mix((24, 18, 52), (66, 50, 120), nz(x, y)) + (255,)
            if r.random() < .06:
                lp[x, y] = CYAN + (255,)
save(leaf, "textures/block/hu_khong_la.png")


def rift(f, n=8):
    im = Image.new("RGBA", (16, 16), (6, 4, 14, 255))
    px = im.load()
    nz = vnoise(16, 16, 81, 6)
    for y in range(16):
        for x in range(16):
            d = abs(nz((x + f * 2) % 16, y) - .5)
            if d < .07:
                px[x, y] = mix(WHITE, CYAN, d / .07) + (255,)
            elif d < .14:
                px[x, y] = mix(VIOLET, (30, 22, 70), (d - .07) / .07) + (255,)
            elif d < .2:
                px[x, y] = (18, 14, 40, 255)
    return im


im, meta = anim([rift(i) for i in range(8)], 4)
save(im, "textures/block/hu_khong_vet_nut.png", meta)


def portal(f, n=16):
    im = Image.new("RGBA", (16, 16))
    px = im.load()
    ph = f / n * 2 * math.pi
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            d = math.hypot(dx, dy)
            a = math.atan2(dy, dx)
            v = (math.sin(a * 2 + d * .9 - ph) + 1) / 2 * (1 - d / 11) + .15 * math.sin(ph * 2 + x)
            v = max(0, min(1, v))
            px[x, y] = mix((20, 12, 60), mix(VIOLET, CYAN, v), v) + (int(120 + 110 * v),)
    return im


im, meta = anim([portal(i) for i in range(16)], 2, True)
save(im, "textures/block/bi_canh_cong.png", meta)

# ---- sky (256x256 tileable nebula + cracks) ----
S = 256
neb = Image.new("RGBA", (S, S))
npx = neb.load()
a, b, c = vnoise(S, S, 91, 64), vnoise(S, S, 92, 32), vnoise(S, S, 93, 16)
r = random.Random(94)
for y in range(S):
    for x in range(S):
        v = a(x, y) * .55 + b(x, y) * .3 + c(x, y) * .15
        v = max(0, (v - .4) * 2.2)
        npx[x, y] = mix((4, 3, 12), mix((70, 40, 140), (60, 150, 190), b(x + 40, y)), v) + (255,)
for _ in range(140):
    npx[r.randrange(S), r.randrange(S)] = (200, 200, 240, 255)
save(neb, "textures/environment/void_nebula.png")
cr = Image.new("RGBA", (S, S), (0, 0, 0, 0))
cp = cr.load()
r = random.Random(95)
for _ in range(9):
    x, y = r.randrange(S), r.randrange(S)
    ang = r.uniform(0, 6.28)
    for _ in range(r.randint(40, 90)):
        ang += r.uniform(-.5, .5)
        x += math.cos(ang) * 1.5
        y += math.sin(ang) * 1.5
        for ox, oy, col in ((0, 0, WHITE + (255,)), (1, 0, VIOLET + (150,)), (-1, 0, VIOLET + (150,)), (0, 1, VIOLET + (90,))):
            cp[int(x + ox) % S, int(y + oy) % S] = col
save(cr, "textures/environment/void_cracks.png")

# ---- GUI: boss bar sheet 182x10 (row0 = frame bg, row1 = fill) + Hu Hoa icon ----
bar = Image.new("RGBA", (182, 10), (0, 0, 0, 0))
bp = bar.load()
for x in range(182):
    t = x / 181
    for y in range(5):
        edge = x in (0, 181) or y in (0, 4)
        bp[x, y] = (60, 46, 110, 255) if edge else (10, 8, 22, 235)
        if edge and (x + y) % 9 == 0:
            bp[x, y] = CYAN + (255,)
        bp[x, 5 + y] = (mix((130, 100, 230), WHITE, t) if y == 0 else mix((70, 40, 150), (150, 90, 230), t)) + (255,)
for x in range(0, 182, 13):
    bp[x, 7] = CYAN + (255,)
save(bar, "textures/gui/void_bossbar.png")
ic = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
ip = ic.load()
nz = vnoise(18, 18, 111, 6)
for y in range(18):
    for x in range(18):
        d = math.hypot(x - 8.5, y - 8.5)
        if d < 7.5:
            ip[x, y] = (110, 80, 190, 255) if d > 6 else mix((16, 12, 36), (60, 44, 120), nz(x, y)) + (255,)
for i in range(3, 15):
    ip[i, i] = (WHITE if i % 3 == 0 else CYAN) + (255,)
for x, y in [(9, 8), (8, 9), (10, 9)]:
    ip[x, y] = (4, 3, 10, 255)
save(ic, "textures/mob_effect/hu_hoa.png")

os.makedirs(PACK, exist_ok=True)
with open(os.path.join(PACK, "pack.mcmeta"), "w") as f:
    json.dump({"pack": {"pack_format": 34, "description": "Hu Khong - Void art set (optional, overrides mod textures)"}}, f, indent=1)
print(len(OUTS), "textures written")
