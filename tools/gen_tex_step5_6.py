"""Generate pixel-art textures for hukhong mod (step 5-6).

Output: 16x16 item/block textures and 48x64 GUI card textures, PNG RGBA.
No anti-aliasing: every pixel is set explicitly, alpha is 0 or 255 except the
glow halo on the crystal (hard-edged per pixel, still no smoothing).
Run: python gen_tex_step5_6.py
"""

import math
import os
import random

from PIL import Image

BASE = os.path.normpath(os.path.join(
    os.path.dirname(os.path.abspath(__file__)),
    os.pardir, "src", "main", "resources", "assets", "hukhong", "textures"))
ITEM_DIR = os.path.join(BASE, "item")
BLOCK_DIR = os.path.join(BASE, "block")
CORE_DIR = os.path.join(BASE, "gui", "core")

TRANS = (0, 0, 0, 0)
N4 = ((1, 0), (-1, 0), (0, 1), (0, -1))


def hx(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def new_img(w, h):
    return Image.new("RGBA", (w, h), TRANS)


def put(img, x, y, c):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), c)


def save(img, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path, format="PNG")
    print("wrote", path, img.size)


def disc_dist(x, y, cx=8.0, cy=8.0):
    return math.hypot(x + 0.5 - cx, y + 0.5 - cy)


# ---------------------------------------------------------------- items

def make_pill(base, shade, hi, outline, rings):
    """Round pill 16x16. rings = [(r0, r1, hex), ...] annular bands."""
    img = new_img(16, 16)
    for y in range(16):
        for x in range(16):
            d = disc_dist(x, y)
            if d >= 7.0:
                continue
            if d >= 6.2:
                c = hx(outline)
            else:
                c = hx(base)
                if d > 4.6 and (x + y) >= 19:
                    c = hx(shade)
                elif d < 5.5 and (x + y) <= 9:
                    c = hx(hi)
                for r0, r1, col in rings:
                    if r0 <= d < r1:
                        c = hx(col)
            put(img, x, y, c)
    return img


def point_in_poly(px, py, poly):
    inside = False
    j = len(poly) - 1
    for i in range(len(poly)):
        xi, yi = poly[i]
        xj, yj = poly[j]
        if (yi > py) != (yj > py) and px < (xj - xi) * (py - yi) / (yj - yi) + xi:
            inside = not inside
        j = i
    return inside


def make_crystal():
    poly = [(8, 1), (13, 4), (13, 11), (8, 15), (3, 11), (3, 4)]
    img = new_img(16, 16)
    inside = {}
    for y in range(16):
        for x in range(16):
            inside[(x, y)] = point_in_poly(x + 0.5, y + 0.5, poly)

    for y in range(16):
        for x in range(16):
            if inside[(x, y)]:
                on_edge = any(not inside.get((x + dx, y + dy), False) for dx, dy in N4)
                if on_edge:
                    c = hx("#2A1050")
                elif x < 8:
                    c = hx("#5A2A9A") if y < 9 else hx("#4A2188")
                else:
                    c = hx("#9B5FE0") if y < 9 else hx("#7A3CC0")
                if not on_edge and 2 <= y <= 4 and 5 <= x <= 7:
                    c = hx("#D9B8FF")
                if not on_edge and x in (7, 8) and 4 <= y <= 11:
                    c = hx("#B57BFF")
                put(img, x, y, c)
            else:
                # glow halo: hard-edged, semi-transparent, only next to the gem
                if any(inside.get((x + dx, y + dy), False) for dx, dy in N4):
                    put(img, x, y, hx("#B88CFF", 170))

    put(img, 6, 6, hx("#FFFFFF"))
    put(img, 5, 6, hx("#FFFFFF"))
    put(img, 10, 9, hx("#F5EBFF"))
    return img


def make_hon_tinh():
    return make_crystal()


# ---------------------------------------------------------------- block

STONE = ["#4B5761", "#56626C", "#5E6B76", "#414C55"]
STONE_W = [5, 4, 2, 1]
SEAM = "#343D45"


def make_block_top():
    rng = random.Random(1001)
    img = new_img(16, 16)
    for y in range(16):
        for x in range(16):
            c = hx(rng.choices(STONE, STONE_W)[0])
            if x in (0, 15) or y in (0, 15):
                c = hx(SEAM)
            put(img, x, y, c)

    for y in range(16):
        for x in range(16):
            d = disc_dist(x, y)
            if 6.0 <= d < 6.9:
                put(img, x, y, hx("#B57BFF"))
            elif 4.6 <= d < 5.3:
                put(img, x, y, hx("#8E4FE0"))
            elif d < 1.6:
                put(img, x, y, hx("#E6CCFF"))

    # rune ticks between the two rings
    for k in range(8):
        a = k * math.pi / 4
        tx = math.floor(8 + 3.0 * math.cos(a))
        ty = math.floor(8 + 3.0 * math.sin(a))
        put(img, tx, ty, hx("#F0DCFF"))
    put(img, 7, 7, hx("#FFFFFF"))
    put(img, 8, 8, hx("#FFFFFF"))
    return img


def make_block_side():
    rng = random.Random(2002)
    img = new_img(16, 16)
    mortar = "#2F373E"
    for y in range(16):
        for x in range(16):
            if y in (4, 8, 12):
                c = hx(mortar)
            else:
                if 0 <= y <= 3 or 9 <= y <= 11:
                    joint = 7
                    band_joints = (joint,)
                else:
                    band_joints = (3, 11)
                if x in band_joints:
                    c = hx(mortar)
                else:
                    c = hx(rng.choices(STONE, STONE_W)[0])
            put(img, x, y, c)
    return img


def make_block_bottom():
    rng = random.Random(3003)
    img = new_img(16, 16)
    dark = ["#353E46", "#3E4850", "#2F373E"]
    for y in range(16):
        for x in range(16):
            c = hx(rng.choices(dark, [5, 4, 2])[0])
            if x in (0, 15) or y in (0, 15):
                c = hx("#262D33")
            put(img, x, y, c)
    return img


# ---------------------------------------------------------------- GUI cards

W, H = 48, 64
OUTLINE = "#0B0A10"
BODY = "#14121C"


def edge_dist(x, y):
    return min(x, y, W - 1 - x, H - 1 - y)


def make_card(main, shade, hi, body=BODY, decorate=None, body_fill=None):
    img = new_img(W, H)
    for y in range(H):
        for x in range(W):
            d = edge_dist(x, y)
            if d == 0:
                c = hx(OUTLINE)
            elif d == 1:
                c = hx(hi) if (x + y) < (W + H) // 2 else hx(shade)
            elif d == 2:
                c = hx(main)
            elif d == 3:
                c = hx(shade)
            else:
                c = hx(body)
                if body_fill is not None:
                    c = body_fill(x, y, c)
            put(img, x, y, c)
    if decorate is not None:
        decorate(img)
    return img


def _corner_zone(x, y, k=8):
    return (x < k or x >= W - k) and (y < k or y >= H - k)


def _rivets(img, light, dark):
    for x, y in ((5, 5), (42, 5), (5, 58), (42, 58)):
        put(img, x, y, hx(light))
        put(img, x + 1, y + 1, hx(dark))


def deco_bach(img):
    """Bach (white, basic): plain silver border, nothing else."""
    pass


def deco_hoang(img):
    """Hoang (yellow): dotted gold trim on the middle ring + corner dots."""
    for y in range(H):
        for x in range(W):
            if edge_dist(x, y) == 2 and (x + y) % 4 == 0:
                put(img, x, y, hx("#FFF6B8"))
    _rivets(img, "#FFF6B8", "#8C6A10")


def deco_tu(img):
    """Tu (purple): bright L-brackets in each corner of the ring."""
    for y in range(H):
        for x in range(W):
            d = edge_dist(x, y)
            if d in (1, 2) and _corner_zone(x, y, 9):
                put(img, x, y, hx("#E2C8FF"))
    for x, y in ((6, 6), (41, 6), (6, 57), (41, 57)):
        put(img, x, y, hx("#FFFFFF"))


def deco_hac(img):
    """Hac (black): steel chevron pattern + silver sheen + rivets."""
    for y in range(H):
        for x in range(W):
            d = edge_dist(x, y)
            if d == 2 and (x + y) % 8 in (0, 1):
                put(img, x, y, hx("#6E7A88"))
            if d in (1, 2) and (x + y) % 24 in (5, 6):
                put(img, x, y, hx("#E8EEF6"))
    _rivets(img, "#B9C3CF", "#0A0A0E")


def deco_hong(img):
    """Hong (red): zig-zag scallop trim + gem studs in each corner."""
    for y in range(H):
        for x in range(W):
            if edge_dist(x, y) == 2 and ((x + y) // 3) % 2 == 0:
                put(img, x, y, hx("#F0606A"))
    for x, y in ((6, 6), (41, 6), (6, 57), (41, 57)):
        for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
            put(img, x + dx, y + dy, hx("#FFD0D0"))
        put(img, x, y, hx("#FF4A55"))


def deco_kim(img):
    """Kim (gold, top tier): chevron gold ring, gold sheen streaks,
    floret corners, jewel studs at top/bottom centre."""
    tones = ["#F2D230", "#FFF3A0", "#D9A41E", "#FFF8D0"]
    for y in range(H):
        for x in range(W):
            d = edge_dist(x, y)
            if d == 2:
                put(img, x, y, hx(tones[((x + y) // 2) % 3]))
            elif d == 3 and (x + y) % 6 == 0:
                put(img, x, y, hx("#FFF3A0"))
            elif d in (1, 2, 3) and (x - y) % 30 in (10, 11):
                put(img, x, y, hx("#FFFBE0"))  # gold sheen streak
    # floret (plus + centre) in each corner
    for cx, cy in ((7, 7), (40, 7), (7, 56), (40, 56)):
        for dx, dy in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
            put(img, cx + dx, cy + dy, hx("#FFF8D0"))
        put(img, cx, cy, hx("#FFFFFF"))
    # jewel studs top / bottom centre
    for cx, cy in ((24, 2), (24, 61)):
        put(img, cx, cy, hx("#B3122E"))
        put(img, cx - 1, cy, hx("#FF8FA0"))
        put(img, cx + 1, cy, hx("#7A0A1E"))


def body_lattice(color):
    """Faint diagonal lattice on the card body (used by hoang/kim)."""
    def fn(x, y, c):
        if (x + y) % 10 == 0 or (x - y) % 10 == 0:
            return hx(color)
        return c
    return fn


GLYPH_Q = [
    "01110",
    "10001",
    "00001",
    "00010",
    "00100",
    "00000",
    "00100",
]


def back_body(x, y, c):
    if (x + y) % 12 == 0 or (x - y) % 12 == 0:
        c = hx("#2E1A4D")
    if (x * 7 + y * 13) % 97 == 0:
        c = hx("#E0C8FF")
    dd = abs(x + 0.5 - 24) + abs(y + 0.5 - 32)
    if 15 <= dd < 16.2:
        c = hx("#6A3AB0")
    elif 18 <= dd < 19:
        c = hx("#4B2A80")
    return c


def make_card_back():
    img = make_card("#5B2C8F", "#2A124A", "#9D6BD9", body="#1A0F2E",
                    body_fill=back_body)
    # corner diamonds
    for cx, cy in ((6, 6), (41, 6), (6, 57), (41, 57)):
        for dx, dy in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
            put(img, cx + dx, cy + dy, hx("#8E5FD6"))
    # "?" glyph, scale 2, shadow offset (+1,+1)
    gx, gy, s = 19, 25, 2
    for pass_shadow in (True, False):
        col = "#2A0F55" if pass_shadow else "#E6CCFF"
        off = 1 if pass_shadow else 0
        for ry, row in enumerate(GLYPH_Q):
            for rx, bit in enumerate(row):
                if bit != "1":
                    continue
                for sy in range(s):
                    for sx in range(s):
                        put(img, gx + rx * s + sx + off, gy + ry * s + sy + off, hx(col))
    return img


def main():
    # Items
    save(make_pill("#F3EDD8", "#D9D0B4", "#FFFFFF", "#7A6E55",
                   [(3.9, 4.9, "#9ED3F5")]),
         os.path.join(ITEM_DIR, "khai_mach_dan.png"))
    save(make_pill("#3FA34D", "#2C7A3A", "#7FD08A", "#1E5A2E",
                   [(2.6, 3.5, "#CFF7C4"), (4.6, 5.5, "#CFF7C4")]),
         os.path.join(ITEM_DIR, "ngung_nguyen_dan.png"))
    save(make_hon_tinh(), os.path.join(ITEM_DIR, "hon_tinh.png"))

    # Block
    save(make_block_top(), os.path.join(BLOCK_DIR, "an_hon_dai_top.png"))
    save(make_block_side(), os.path.join(BLOCK_DIR, "an_hon_dai_side.png"))
    save(make_block_bottom(), os.path.join(BLOCK_DIR, "an_hon_dai_bottom.png"))

    # GUI cards
    save(make_card_back(), os.path.join(CORE_DIR, "card_back.png"))
    # tier -> (main, shade, hi, decorator, body fill). Ornament grows with tier:
    # bach (plain) -> hoang -> tu -> hac -> hong -> kim (most ornate, gold sheen).
    frames = {
        "bach": ("#EDEFF3", "#9AA1AD", "#FFFFFF", deco_bach, None),
        "hoang": ("#F2D230", "#A87F12", "#FFF3A0", deco_hoang,
                  body_lattice("#2A220E")),
        "tu": ("#8E4FD6", "#4C237F", "#CDA8FF", deco_tu, None),
        "hac": ("#33333C", "#121217", "#B9C3CF", deco_hac, None),
        "hong": ("#C8323A", "#6E141B", "#FF8080", deco_hong, None),
        "kim": ("#D9A41E", "#7C5A08", "#FFE68A", deco_kim,
                body_lattice("#2B2210")),
    }
    for name, (main_c, shade_c, hi_c, deco, bfill) in frames.items():
        save(make_card(main_c, shade_c, hi_c, decorate=deco, body_fill=bfill),
             os.path.join(CORE_DIR, f"card_frame_{name}.png"))


if __name__ == "__main__":
    main()
