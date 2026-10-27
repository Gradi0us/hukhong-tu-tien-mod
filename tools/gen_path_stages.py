# Pháp khí các đạo theo giai đoạn 1..6 (texture layer người chơi): python tools/gen_path_stages.py [--sheet out.png]
#   Đạo  - Pháp Bảo: wings_s<N>.png (+ _glow từ s4)          cánh MỘT bên 32x32 (mirror ở render)
#   Thể  - Bá Thể : the_skin_s<N>.png (+ _glow từ s4)         lớp da vàng kim 64x64 phủ UV người chơi (độ phủ tăng theo N)
#   Ma   - Ma Hồn : ma_horn_s<N>.png, ma_cloud_s<N>.png        sừng 16x16 (v=0 mũi -> v=1 gốc), mây tối 32x32
import math
import os
import random
import sys
import zlib

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')
OUT = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'hukhong', 'textures', 'entity', 'artifact')
os.makedirs(OUT, exist_ok=True)
OL = (20, 14, 22, 255)
N4 = ((1, 0), (-1, 0), (0, 1), (0, -1))


def tone(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (255,)


def mixc(a, b, t):
    return tuple(int(a[i] * (1 - t) + b[i] * t) for i in range(3)) + (255,)


def rng(*k):
    return random.Random(zlib.crc32(repr(k).encode()))


def outline(im, col):
    w, h = im.size
    src = im.copy().load()
    px = im.load()
    for y in range(h):
        for x in range(w):
            if src[x, y][3] == 0:
                for dx, dy in N4:
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < w and 0 <= ny < h and src[nx, ny][3] > 200:
                        px[x, y] = col
                        break


# ------------------------------------------------------------------ Đạo: cánh
WING = {  # lông, màu gốc, màu ngọn, viền, glow
    1: dict(n=3, root=(214, 214, 222), tip=(168, 170, 184), edge=(110, 112, 128), glow=None),
    2: dict(n=4, root=(250, 250, 252), tip=(206, 212, 226), edge=(140, 146, 164), glow=None),
    3: dict(n=5, root=(255, 252, 238), tip=(255, 228, 160), edge=(176, 140, 70), glow=None),
    4: dict(n=5, root=(255, 250, 224), tip=(255, 214, 96), edge=(190, 138, 40), glow=(255, 230, 120)),
    5: dict(n=6, root=(255, 255, 240), tip=(255, 200, 70), edge=(206, 140, 30), glow=(255, 240, 160)),
    6: dict(n=7, root=(255, 255, 250), tip=(255, 186, 52), edge=(220, 150, 30), glow=(255, 250, 200)),
}


def wing(stage):
    c = WING[stage]
    S = 32
    im = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    gl = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    px, gp = im.load(), gl.load()
    n = c['n']
    for i in range(n):
        ang = math.radians(-8 + i * (82.0 / max(1, n - 1)))
        length = 30 - i * (11.0 / n)
        for t in range(0, 41):
            f = t / 40.0
            x = f * length * math.cos(ang)
            y = 7 + f * length * math.sin(ang) + 5.0 * f * f
            r = (2.9 + 0.12 * stage) * (1 - f * 0.72)
            col = mixc(c['root'], c['tip'], f ** 1.2)
            for dy in range(-4, 5):
                for dx in range(-4, 5):
                    px_x, px_y = int(round(x)) + dx, int(round(y)) + dy
                    if not (0 <= px_x < S and 0 <= px_y < S):
                        continue
                    ex = (dx * 0.9) ** 2 + (dy * 1.15) ** 2
                    if ex <= r * r:
                        k = 1.0
                        if dy < -r * 0.25:
                            k = 1.12
                        elif dy > r * 0.35:
                            k = 0.8
                        px[px_x, px_y] = tone(col, k)
                        if c['glow'] and ((abs(dy) <= 0 and f > 0.35) or (f > 0.86 and ex <= (r * 0.7) ** 2)):
                            gp[px_x, px_y] = tuple(c['glow']) + (255,)
    outline(im, tone(c['edge'], 0.8))
    return im, (gl if c['glow'] else None)


# ------------------------------------------------------------------ Thể: da vàng kim
GOLD = (236, 184, 52)


def noise(x, y, s):
    return rng(x, y, s).random()


def the_skin(stage):
    S = 64
    im = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    gl = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    px, gp = im.load(), gl.load()
    cover = (0.0, 0.30, 0.45, 0.62, 0.78, 0.92, 1.0)[stage]
    for y in range(S):
        for x in range(S):
            n = noise(x // 2, y // 2, stage)
            # mảng vàng liền khối (giai đoạn thấp: loang lổ; cao: phủ kín)
            blob = 0.5 * noise(x // 4, y // 4, 99) + 0.5 * n
            if blob > cover + (0.0 if stage < 6 else -1):
                continue
            band = 0.5 + 0.5 * math.sin((x + y * 0.6) * 0.55 + stage)
            f = 0.78 + 0.34 * band + (noise(x, y, 7) - 0.5) * 0.18
            col = tone(GOLD, f)
            if stage >= 3 and noise(x, y, 31) > 0.93:   # vảy sáng
                col = tone((255, 244, 190), 1.0)
            px[x, y] = col
            vein = (stage >= 4) and ((x * 7 + y * 3) % 23 == 0 or (x * 3 - y * 5) % 29 == 0) and noise(x, y, 5) > 0.25
            if vein:
                px[x, y] = (255, 246, 200, 255)
                gp[x, y] = (255, 236, 140, 255)
    return im, (gl if stage >= 4 else None)


# ------------------------------------------------------------------ Ma: sừng + mây
def horn(stage):
    S = 16
    im = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    px = im.load()
    base = (36, 22, 46)
    mid = (96, 28, 52)
    tip = (255, 70, 60) if stage >= 3 else (170, 50, 70)
    for y in range(S):
        f = y / (S - 1.0)  # 0 mũi -> 1 gốc
        for x in range(S):
            ridge = 0.85 + 0.15 * math.sin(x * 1.3 + stage)
            if f < 0.35:
                col = mixc(tip, mid, f / 0.35)
            else:
                col = mixc(mid, base, (f - 0.35) / 0.65)
            col = tone(col, ridge * (0.9 + 0.2 * (1 - abs(x - 7.5) / 8)))
            if stage >= 4 and (x + y * 2) % 7 == 0 and f < 0.7:
                col = (255, 120, 80, 255)
            px[x, y] = col
    return im


def cloud(stage):
    S = 32
    im = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    px = im.load()
    cx = cy = (S - 1) / 2.0
    r = rng('cloud', stage)
    blobs = [(cx + r.uniform(-6, 6), cy + r.uniform(-6, 6), r.uniform(6, 11)) for _ in range(6)]
    for y in range(S):
        for x in range(S):
            d = 0.0
            for bx, by, br in blobs:
                dd = 1 - math.hypot(x - bx, y - by) / br
                d = max(d, dd)
            if d <= 0:
                continue
            a = 255 if d > 0.55 else 190 if d > 0.3 else 110 if d > 0.12 else 50
            col = mixc((22, 8, 34), (58, 20, 86), min(1.0, d + noise(x, y, 3) * 0.3))
            if stage >= 3 and noise(x, y, 9) > 0.965 - 0.006 * stage:
                col = (200, 40, 70, 255)
                a = 255
            px[x, y] = col[:3] + (a,)
    return im


def main():
    cols = 6
    sheet = Image.new('RGBA', (cols * 96, 4 * 96), (70, 70, 84, 255))
    for st in range(1, 7):
        w, wg = wing(st)
        w.save(os.path.join(OUT, 'wings_s%d.png' % st))
        wsz = w.resize((96, 96), Image.NEAREST)
        sheet.alpha_composite(wsz, ((st - 1) * 96, 0))
        if wg:
            wg.save(os.path.join(OUT, 'wings_s%d_glow.png' % st))
        sk, sg = the_skin(st)
        sk.save(os.path.join(OUT, 'the_skin_s%d.png' % st))
        if sg:
            sg.save(os.path.join(OUT, 'the_skin_s%d_glow.png' % st))
        sheet.alpha_composite(sk.resize((96, 96), Image.NEAREST), ((st - 1) * 96, 96))
        h = horn(st)
        h.save(os.path.join(OUT, 'ma_horn_s%d.png' % st))
        sheet.alpha_composite(h.resize((96, 96), Image.NEAREST), ((st - 1) * 96, 192))
        cl = cloud(st)
        cl.save(os.path.join(OUT, 'ma_cloud_s%d.png' % st))
        sheet.alpha_composite(cl.resize((96, 96), Image.NEAREST), ((st - 1) * 96, 288))
    if '--sheet' in sys.argv:
        sheet.save(sys.argv[sys.argv.index('--sheet') + 1])
    print('ok')


if __name__ == '__main__':
    main()
