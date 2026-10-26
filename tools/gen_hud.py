"""Generate pixel-art HUD textures for mod hukhong (Minecraft).

Output: src/main/resources/assets/hukhong/textures/gui/hud/
Sizes are fixed (code depends on them):
  exp_bar_bg / exp_bar_fill / resource_bar_bg / resource_bar_fill : 100x6
  frame.png : 100x24
Pure pixel placement, no resampling / anti-aliasing.
Requires: pip install pillow
"""
import os
from PIL import Image

OUT_DIR = os.path.normpath(os.path.join(
    os.path.dirname(os.path.abspath(__file__)),
    os.pardir, "src", "main", "resources",
    "assets", "hukhong", "textures", "gui", "hud",
))

BAR_W, BAR_H = 100, 6
FRAME_W, FRAME_H = 100, 24


def hexc(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def lerp(c1, c2, t):
    return tuple(int(round(c1[i] + (c2[i] - c1[i]) * t)) for i in range(4))


# ---------------------------------------------------------------- bars
def make_bar_bg(w=BAR_W, h=BAR_H):
    """Dark recessed track: black border, 3 inner tones, 1px lighter top lip."""
    border = hexc("0A0A10")
    top_lip = hexc("4A4A58")   # 1px highlight at top inside edge
    inner_a = hexc("2A2A34")
    inner_b = hexc("22222B")
    bottom = hexc("17171E")    # 1px shade at bottom inside edge
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    px = img.load()
    for y in range(h):
        for x in range(w):
            if x == 0 or x == w - 1 or y == 0 or y == h - 1:
                px[x, y] = border
            elif y == 1:
                px[x, y] = top_lip
            elif y == h - 2:
                px[x, y] = bottom
            else:
                px[x, y] = inner_a if (x // 4) % 2 == 0 else inner_b
    return img


def make_exp_fill(w=BAR_W, h=BAR_H):
    """Teal-jade fill: 1px bright highlight on top row, 3-tone body, gold shimmer."""
    teal_dark = hexc("0B5A4A")
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
            if y == 0:                       # 1px highlight above the bar
                c = lerp(body, hexc("E6FFF6"), 0.7)
            elif y == 1:
                c = lerp(body, hexc("FFFFFF"), 0.25)
            elif y == h - 2:
                c = lerp(body, teal_dark, 0.45)  # bottom shade
            elif y == h - 1:
                c = teal_dark
            else:
                c = body
            px[x, y] = c
        # gold shimmer streak sweeping along the bar
        s = (x + 3) % 14
        if s in (0, 1):
            px[x, 2] = gold
            px[x, 3] = lerp(body, gold, 0.35)
        elif s == 2:
            px[x, 2] = gold_hi
    return img


def make_resource_fill(w=BAR_W, h=BAR_H):
    """Neutral silver bar (tinted per path in game): 1px white top highlight."""
    hi = hexc("FFFFFF")
    light = hexc("EDEDED")
    body_a = hexc("D2D2D2")
    body_b = hexc("A8A8A8")
    shade = hexc("8A8A8A")
    lo = hexc("5E5E5E")
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    px = img.load()
    for x in range(w):
        t = x / max(1, w - 1)
        body = lerp(body_a, body_b, t * 0.5)
        for y in range(h):
            if y == 0:
                c = hi
            elif y == 1:
                c = light
            elif y == h - 2:
                c = shade
            elif y == h - 1:
                c = lo
            else:
                c = body
            px[x, y] = c
        if x % 10 == 4:  # sparse glint
            px[x, 2] = hi
    return img


# ---------------------------------------------------------------- frame
def make_frame(w=FRAME_W, h=FRAME_H):
    """Jade-slip frame: bronze/gold border, notched corners, ornament on
    top/bottom centre, translucent violet-black fill."""
    bronze_dark = hexc("5A3D0E")
    bronze = hexc("B8862B")
    bronze_lo = hexc("8A6220")
    gold_hi = hexc("F2D27A")
    gem = hexc("E0475B")
    fill = hexc("1A0D2B", 190)
    fill_hi = hexc("2E1A4A", 200)
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    px = img.load()
    cx = w // 2
    for y in range(h):
        for x in range(w):
            on_edge = x in (0, w - 1) or y in (0, h - 1)
            on_inner = x in (1, w - 2) or y in (1, h - 2)
            if on_edge:
                c = bronze_dark
            elif on_inner:
                # light from top-left: lit top/left, shaded bottom/right
                c = gold_hi if (y == 1 or x == 1) else bronze_lo if (y == h - 2 or x == w - 2) else bronze
            else:
                c = fill_hi if y == 2 else fill
            px[x, y] = c
    # dotted trim on the inner line (every 4th pixel) for a crafted look
    for x in range(4, w - 4, 4):
        px[x, h - 2] = gold_hi if x % 8 == 0 else bronze_lo
    # clipped outer corners (notch)
    for (x, y) in [(0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1)]:
        px[x, y] = (0, 0, 0, 0)
    # corner rivets
    for (x, y) in [(3, 3), (w - 4, 3), (3, h - 4), (w - 4, h - 4)]:
        px[x, y] = gold_hi
    # centre-top and centre-bottom diamond gems
    for yy, sign in ((0, 1), (h - 1, -1)):
        for (x, y, c) in [(cx, yy, bronze_dark), (cx - 1, yy + sign, gold_hi),
                          (cx, yy + sign, gem), (cx + 1, yy + sign, gold_hi)]:
            px[x, y] = c
    return img


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    outputs = {
        "exp_bar_bg.png": make_bar_bg(),
        "exp_bar_fill.png": make_exp_fill(),
        "resource_bar_bg.png": make_bar_bg(),
        "resource_bar_fill.png": make_resource_fill(),
        "frame.png": make_frame(),
    }
    for name, img in outputs.items():
        path = os.path.join(OUT_DIR, name)
        img.save(path, "PNG")
        print(f"wrote {path} {img.size}")


if __name__ == "__main__":
    main()
