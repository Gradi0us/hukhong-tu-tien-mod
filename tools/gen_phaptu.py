"""Sinh icon ky nang Phap Tu (6 nguyen to x Q/E/R/T + 4 icon chung) cho mod hukhong.

- 16x16 RGBA pixel-art, vien 1px toi, khong anti-alias (giong gen_skill_icons.py).
- Ten file: phap_<nguyen to>_<1|2|3|ult>.png (HUD chon theo nguyen to qua PhapClient.icon) + phap_<1|2|3|ult>.png chung.

Chay: python D:\\mc_mod\\tools\\gen_phaptu.py
"""
import math
import os

from PIL import Image

OUT_DIR = os.path.normpath(os.path.join(
    os.path.dirname(os.path.abspath(__file__)),
    os.pardir, "src", "main", "resources", "assets", "hukhong", "textures", "gui", "skill"))
SIZE = 16
OUTLINE = (20, 14, 10, 255)
N8 = [(-1, -1), (0, -1), (1, -1), (-1, 0), (1, 0), (-1, 1), (0, 1), (1, 1)]


def rgba(h):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


# (sang, chinh, toi) cho moi nguyen to
PAL = {
    "quang": (rgba("#fffbe0"), rgba("#ffd84a"), rgba("#c98a14")),
    "loi": (rgba("#f0f6ff"), rgba("#8fb8ff"), rgba("#4a62c8")),
    "hoa": (rgba("#ffe9a0"), rgba("#ff7a2a"), rgba("#b82a10")),
    "thuy": (rgba("#d8f4ff"), rgba("#3fa9ff"), rgba("#1c5cb0")),
    "tho": (rgba("#e0c89a"), rgba("#a8823f"), rgba("#5e4420")),
    "kim": (rgba("#ffffff"), rgba("#c4ced8"), rgba("#6a7886")),
    "chung": (rgba("#d8fff6"), rgba("#3fd6c4"), rgba("#1c8a7c")),
}


class Canvas:
    def __init__(self):
        self.px = [[None] * SIZE for _ in range(SIZE)]

    def set(self, x, y, c):
        if 0 <= x < SIZE and 0 <= y < SIZE:
            self.px[y][x] = c

    def filled(self, x, y):
        return 0 <= x < SIZE and 0 <= y < SIZE and self.px[y][x] is not None

    def disc(self, cx, cy, r, c):
        for y in range(SIZE):
            for x in range(SIZE):
                if (x - cx) ** 2 + (y - cy) ** 2 <= r * r:
                    self.set(x, y, c)

    def line(self, x0, y0, x1, y1, c, w=0):
        n = int(max(abs(x1 - x0), abs(y1 - y0)) * 2) + 1
        for i in range(n + 1):
            t = i / n
            x, y = round(x0 + (x1 - x0) * t), round(y0 + (y1 - y0) * t)
            self.set(x, y, c)
            if w:
                for dx, dy in ((1, 0), (0, 1)):
                    self.set(x + dx, y + dy, c)

    def poly(self, pts, c):
        for y in range(SIZE):
            for x in range(SIZE):
                inside = False
                j = len(pts) - 1
                for i in range(len(pts)):
                    xi, yi = pts[i]
                    xj, yj = pts[j]
                    if (yi > y) != (yj > y) and x < (xj - xi) * (y - yi) / (yj - yi + 1e-9) + xi:
                        inside = not inside
                    j = i
                if inside:
                    self.set(x, y, c)

    def bevel_outline(self):
        snap = [r[:] for r in self.px]
        for y in range(SIZE):
            for x in range(SIZE):
                c = snap[y][x]
                if c is None:
                    continue
                up = y == 0 or snap[y - 1][x] is None
                left = x == 0 or snap[y][x - 1] is None
                down = y == SIZE - 1 or snap[y + 1][x] is None
                right = x == SIZE - 1 or snap[y][x + 1] is None
                if up or left:
                    c = tuple(int(v + (255 - v) * 0.2) for v in c[:3]) + (255,)
                elif down or right:
                    c = tuple(int(v * 0.8) for v in c[:3]) + (255,)
                self.px[y][x] = c
        edge = [(x, y) for y in range(SIZE) for x in range(SIZE)
                if self.px[y][x] is None and any(self.filled(x + dx, y + dy) for dx, dy in N8)]
        for x, y in edge:
            self.set(x, y, OUTLINE)

    def image(self):
        img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
        for y in range(SIZE):
            for x in range(SIZE):
                if self.px[y][x] is not None:
                    img.putpixel((x, y), self.px[y][x])
        return img


def motif(cv, el, s, pal):
    """Hinh nguyen to; s = ti le (0.6 .. 1.0)."""
    lt, mid, dk = pal
    cx = cy = 7.5
    if el == "quang":  # ngoi sao + tia
        r = 5.5 * s
        for k in range(8):
            a = k * math.pi / 4
            cv.line(cx, cy, cx + math.cos(a) * r, cy + math.sin(a) * r, mid)
        cv.disc(cx, cy, 2.2 * s + 0.5, lt)
    elif el == "loi":  # tia set zig-zag
        k = 6 * s
        pts = [(cx + 1.5, cy - k), (cx - 2.5, cy - 0.5), (cx + 0.5, cy - 0.5), (cx - 1.5, cy + k)]
        for i in range(3):
            cv.line(*pts[i], *pts[i + 1], mid, w=1)
        cv.line(pts[1][0], pts[1][1], pts[2][0], pts[2][1], lt)
    elif el == "hoa":  # ngon lua
        h = 6.5 * s
        cv.poly([(cx, cy - h), (cx + 3.4 * s, cy + 0.5), (cx + 2.4 * s, cy + h * 0.8), (cx - 2.4 * s, cy + h * 0.8), (cx - 3.4 * s, cy + 0.5)], mid)
        cv.poly([(cx, cy - h * 0.2), (cx + 1.8 * s, cy + 2), (cx + 1.2 * s, cy + h * 0.75), (cx - 1.2 * s, cy + h * 0.75), (cx - 1.8 * s, cy + 2)], lt)
    elif el == "thuy":  # giot nuoc
        h = 6.5 * s
        cv.poly([(cx, cy - h), (cx + 3.6 * s, cy + 1.5 * s), (cx + 2.2 * s, cy + h * 0.85), (cx - 2.2 * s, cy + h * 0.85), (cx - 3.6 * s, cy + 1.5 * s)], mid)
        cv.disc(cx + 1.2 * s, cy + 1.0, 1.1 * s, lt)
    elif el == "tho":  # thap da
        w = 4.2 * s
        cv.poly([(cx - w, cy + 6 * s), (cx + w, cy + 6 * s), (cx + w * 0.7, cy - 2 * s), (cx - w * 0.7, cy - 2 * s)], mid)
        cv.poly([(cx - w * 0.9, cy - 2 * s), (cx + w * 0.9, cy - 2 * s), (cx, cy - 6 * s)], lt)
        cv.line(cx - w * 0.5, cy + 5 * s, cx + w * 0.5, cy + 5 * s, dk)
    elif el == "kim":  # luoi kiem
        L = 6.8 * s
        cv.line(cx - L, cy + L, cx + L, cy - L, mid, w=1)
        cv.line(cx - L + 1, cy + L - 1, cx + L - 1, cy - L + 1, lt)
        cv.line(cx - 3.5 * s, cy + 1.5 * s, cx - 1.5 * s, cy + 3.5 * s, dk, w=1)
    else:  # chung: hat nhan
        cv.disc(cx, cy, 4.5 * s, mid)
        cv.disc(cx - 1, cy - 1, 2 * s, lt)


def slot_deco(cv, slot, pal):
    lt, mid, dk = pal
    if slot == "1":  # Q: chi tiet mui ten nho goc phai
        cv.line(12, 13, 14, 13, lt)
        cv.set(13, 12, lt)
        cv.set(13, 14, lt)
    elif slot == "2":  # E: vong
        for k in range(20):
            a = k * math.pi / 10
            cv.set(round(7.5 + math.cos(a) * 7), round(7.5 + math.sin(a) * 7), dk)
    elif slot == "3":  # R: ba cham
        for x in (3, 7, 11):
            cv.set(x, 14, lt)
    else:  # T: vien sang 4 goc
        for x, y in ((0, 0), (15, 0), (0, 15), (15, 15), (1, 0), (0, 1), (14, 0), (15, 1), (0, 14), (1, 15), (14, 15), (15, 14)):
            cv.set(x, y, lt)


def make(el, slot):
    cv = Canvas()
    pal = PAL[el]
    s = {"1": 0.7, "2": 0.8, "3": 0.9, "4": 1.0}[slot]
    motif(cv, el, s, pal)
    cv.bevel_outline()
    slot_deco(cv, slot, pal)
    return cv.image()


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    n = 0
    for el in PAL:
        for slot in ("1", "2", "3", "ult"):
            name = "phap_%s.png" % slot if el == "chung" else "phap_%s_%s.png" % (el, slot)
            make(el, "4" if slot == "ult" else slot).save(os.path.join(OUT_DIR, name))
            n += 1
    print("wrote %d icons -> %s" % (n, OUT_DIR))


if __name__ == "__main__":
    main()
