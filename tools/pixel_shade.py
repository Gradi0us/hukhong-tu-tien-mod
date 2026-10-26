"""Helper chung: to bong pixel-art 16x16 tu mat na hinh (khong anti-alias) + vien toi 1px."""
import random

from PIL import Image, ImageDraw

OUT = (20, 14, 22, 255)
N4 = ((1, 0), (-1, 0), (0, 1), (0, -1))


def tone(col, f):
    return tuple(max(0, min(255, int(v * f))) for v in col[:3]) + (255,)


def mask(fn):
    """fn(ImageDraw) ve len anh L (fill=255 = co, 0 = khoet). Tra ve set (x, y)."""
    im = Image.new("L", (16, 16), 0)
    fn(ImageDraw.Draw(im))
    return {(x, y) for y in range(16) for x in range(16) if im.getpixel((x, y)) > 127}


def render(m, col, seed=0, speckle=0.12, accents=(), outline=True):
    """Bong 4 tong: sang canh tren-trai, toi canh duoi-phai; accents = [(x, y, rgb), ...]."""
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    rnd = random.Random(seed)
    for (x, y) in m:
        up = (x, y - 1) not in m
        left = (x - 1, y) not in m
        down = (x, y + 1) not in m
        right = (x + 1, y) not in m
        f = 1.0
        if up or left:
            f = 1.28
        if down or right:
            f = 0.66
        if (up or left) and (down or right):
            f = 0.95
        elif f == 1.0 and (x + y) <= 9:
            f = 1.12
        elif f == 1.0 and (x + y) >= 20:
            f = 0.85
        if f == 1.0 and rnd.random() < speckle:
            f = 0.8
        px[x, y] = tone(col, f)
    for x, y, c in accents:
        if 0 <= x < 16 and 0 <= y < 16 and (x, y) in m:
            px[x, y] = tuple(c[:3]) + (255,)
    if outline:
        src = im.copy().load()
        for y in range(16):
            for x in range(16):
                if src[x, y][3] == 0:
                    for dx, dy in N4:
                        nx, ny = x + dx, y + dy
                        if 0 <= nx < 16 and 0 <= ny < 16 and src[nx, ny][3]:
                            px[x, y] = OUT
                            break
    return im
