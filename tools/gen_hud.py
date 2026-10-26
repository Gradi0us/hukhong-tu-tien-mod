"""Generate pixel-art HUD textures for mod hukhong (Minecraft).

Output: src/main/resources/assets/hukhong/textures/gui/hud/
Pure pixel placement, no resampling / anti-aliasing.
Requires: pip install pillow
"""
import os
from PIL import Image

OUT_DIR = os.path.join(
    os.path.dirname(os.path.abspath(__file__)),
    os.pardir, "src", "main", "resources",
    "assets", "hukhong", "textures", "gui", "hud",
)
OUT_DIR = os.path.normpath(OUT_DIR)


def hexc(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def lerp(c1, c2, t):
    return tuple(int(round(c1[i] + (c2[i] - c1[i]) * t)) for i in range(4))


# ---------------------------------------------------------------- bars
def make_bar_bg(w, h, border, inner, top_hi):
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    px = img.load()
    for y in range(h):
        for x in range(w):
            if x == 0 or x == w - 1 or y == 0 or y == h - 1:
                px[x, y] = border
            elif y == 1:
                px[x, y] = top_hi
            else:
                px[x, y] = inner
    return img


def make_exp_fill(w, h):
    """Teal-jade bar with gold shimmer, pixel gradient."""
    teal_dark = hexc("0E6B5A")
    teal_mid = hexc("17A58A")
    teal_light = hexc("3FD9B0")
    gold = hexc("E8C35A")
    gold_hi = hexc("FFF3B0")
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    px = img.load()
    for x in range(w):
        t = x / max(1, w - 1)
        body = lerp(teal_mid, teal_light, t)
        for y in range(h):
            if y == 0:
                c = lerp(body, hexc("D8FFF0"), 0.55)
            elif y == h - 1:
                c = teal_dark
            else:
                c = body
            px[x, y] = c
        # gold shimmer: diagonal pixel streak sweeping along the bar
        s = (x + 3) % 14
        if s in (0, 1):
            px[x, 2] = gold
            if h > 3:
                px[x, 1] = lerp(px[x, 1], gold_hi, 0.6)
        elif s == 2:
            px[x, 2] = gold_hi
    return img


def make_resource_fill(w, h):
    """Neutral white-grey bar; game code tints it per path."""
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    px = img.load()
    hi = hexc("FFFFFF")
    body_a = hexc("E6E6E6")
    body_b = hexc("B4B4B4")
    lo = hexc("7E7E7E")
    for x in range(w):
        t = x / max(1, w - 1)
        body = lerp(body_a, body_b, t * 0.5)
        for y in range(h):
            if y == 0:
                c = hi
            elif y == h - 1:
                c = lo
            else:
                c = body
            px[x, y] = c
        if x % 10 == 4:  # sparse glint so it reads as a gem-like bar
            px[x, 2] = hi
    return img


# ---------------------------------------------------------------- frame
def make_frame(w=100, h=24):
    """Jade-slip frame: bronze-gold border, translucent violet-black fill."""
    bronze_dark = hexc("6B4A12")
    bronze = hexc("B8862B")
    gold_hi = hexc("F2D27A")
    fill = hexc("1A0D2B", 190)
    fill_hi = hexc("2E1A4A", 200)
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    px = img.load()
    for y in range(h):
        for x in range(w):
            on_edge = x in (0, w - 1) or y in (0, h - 1)
            on_inner = x in (1, w - 2) or y in (1, h - 2)
            if on_edge:
                c = bronze_dark
            elif on_inner:
                c = gold_hi if (y == 1 or x == 1) else bronze
            else:
                c = fill_hi if y == 2 else fill
            px[x, y] = c
    # clipped corners (jade-slip notch): remove outer corner pixels
    for (x, y) in [(0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1)]:
        px[x, y] = (0, 0, 0, 0)
    # small gold rivets at the four corners of the inner border
    for (x, y) in [(3, 3), (w - 4, 3), (3, h - 4), (w - 4, h - 4)]:
        px[x, y] = gold_hi
    return img


def main():
    os.makedirs(OUT_DIR, exist_ok=True)

    bar_border = hexc("0A0A10")
    bar_inner = hexc("24242D")
    bar_top = hexc("3A3A46")

    outputs = {
        "exp_bar_bg.png": make_bar_bg(100, 6, bar_border, bar_inner, bar_top),
        "exp_bar_fill.png": make_exp_fill100, 6,
        "resource_bar_bg.png": make_bar_bg(100, 6, bar_border, bar_inner, bar_top),
        "resource_bar_fill.png": make_resource_fill100, 6,
        "frame.png": make_frame(100, 24),
    }
    for name, img in outputs.items():
        path = os.path.join(OUT_DIR, name)
        img.save(path, "PNG")
        print(f"wrote {path} {img.size}")


if __name__ == "__main__":
    main()
