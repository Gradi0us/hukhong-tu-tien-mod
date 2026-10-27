"""Generate assets for the Tu Tinh currency + Adventurer Guild (mod hukhong).

Outputs under src/main/resources:
  assets/hukhong/textures/item/{ha,trung,thuong}_pham_tu_tinh.png   16x16 coins
  assets/hukhong/textures/block/mao_hiem_bang_{side,top}.png        16x16 quest board
  assets/hukhong/textures/gui/guild/badge_{0..5}.png                40x40 rank badges (Dong, Bac, Bach Ngan, Vang, Tu Kim, Dau Vuong)
  models / blockstate / loot table JSON for the coins and the board.
Pure pixel placement, no resampling. Requires: pip install pillow
"""
import json
import os
from PIL import Image

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), os.pardir, "src", "main", "resources"))
A = os.path.join(ROOT, "assets", "hukhong")
D = os.path.join(ROOT, "data", "hukhong")


def rgb(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def shade(c, f):
    return (max(0, min(255, int(c[0] * f))), max(0, min(255, int(c[1] * f))), max(0, min(255, int(c[2] * f))), c[3])


def save(img, *path):
    p = os.path.join(*path)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f)


# ---------------------------------------------------------------- coins
def coin(base, stack):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    hi, lo, ink = shade(base, 1.35), shade(base, 0.7), shade(base, 0.35)
    for n in range(stack):  # 1..3 coins stacked: more value = bigger pile
        ox, oy = 3 + n * 2 - (stack - 1), 4 + (stack - 1 - n) * 2
        for y in range(8):
            for x in range(8):
                dx, dy = x - 3.5, y - 3.5
                d = dx * dx + dy * dy
                if d <= 14:
                    c = base
                    if dx + dy < -1.5:
                        c = hi
                    elif dx + dy > 2:
                        c = lo
                    px[ox + x, oy + y] = c
                if 9 <= d <= 14:
                    px[ox + x, oy + y] = ink if dx + dy > 0 else lo
        px[ox + 3, oy + 3] = ink  # square hole
        px[ox + 4, oy + 3] = ink
        px[ox + 3, oy + 4] = ink
        px[ox + 4, oy + 4] = ink
    return img


def gen_coins():
    for name, col, stack in (("ha_pham_tu_tinh", rgb("#7fd4c8"), 1), ("trung_pham_tu_tinh", rgb("#9a7bff"), 2), ("thuong_pham_tu_tinh", rgb("#ff7bd0"), 3)):
        save(coin(col, stack), A, "textures", "item", name + ".png")
        write_json(os.path.join(A, "models", "item", name + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": "hukhong:item/" + name}})


# ---------------------------------------------------------------- board
def gen_board():
    wood, wood_d, wood_l = rgb("#7a5230"), rgb("#4d3320"), rgb("#9a6c42")
    side = Image.new("RGBA", (16, 16), wood)
    px = side.load()
    for y in range(16):
        for x in range(16):
            if (x + y * 3) % 7 == 0:
                px[x, y] = wood_d
            if x in (0, 15) or y in (0, 15):
                px[x, y] = wood_d
    # pinned paper notes
    for (x0, y0, c) in ((2, 2, rgb("#efe6c8")), (9, 3, rgb("#e8d9a0")), (4, 9, rgb("#d9e8e0")), (10, 9, rgb("#efe6c8"))):
        for y in range(y0, y0 + 5):
            for x in range(x0, x0 + 5):
                px[x, y] = c
        px[x + 0 - 2 + 2, y0] = rgb("#c04030")  # red pin
    for y in (4, 11):
        for x in range(1, 15):
            if px[x, y] == wood:
                px[x, y] = wood_l
    save(side, A, "textures", "block", "mao_hiem_bang_side.png")
    top = Image.new("RGBA", (16, 16), wood_l)
    tp = top.load()
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                tp[x, y] = wood_d
            elif (x + y) % 5 == 0:
                tp[x, y] = wood
    save(top, A, "textures", "block", "mao_hiem_bang_top.png")
    write_json(os.path.join(A, "models", "block", "mao_hiem_bang.json"),
               {"parent": "minecraft:block/cube_bottom_top", "textures": {"top": "hukhong:block/mao_hiem_bang_top", "side": "hukhong:block/mao_hiem_bang_side", "bottom": "hukhong:block/mao_hiem_bang_top"}})
    write_json(os.path.join(A, "blockstates", "mao_hiem_bang.json"), {"variants": {"": {"model": "hukhong:block/mao_hiem_bang"}}})
    write_json(os.path.join(A, "models", "item", "mao_hiem_bang.json"), {"parent": "hukhong:block/mao_hiem_bang"})
    write_json(os.path.join(D, "loot_table", "blocks", "mao_hiem_bang.json"),
               {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": "hukhong:mao_hiem_bang"}],
                                                      "conditions": [{"condition": "minecraft:survives_explosion"}]}]})


# ---------------------------------------------------------------- badges
RANK_COLORS = [rgb("#b0703a"), rgb("#c8ccd4"), rgb("#e8f0ff"), rgb("#f2c230"), rgb("#b070ff"), rgb("#ff4a3a")]


def badge(rank):
    S = 40
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    px = img.load()
    base = RANK_COLORS[rank]
    hi, lo, ink = shade(base, 1.3), shade(base, 0.65), rgb("#1a1018")
    cx = 19.5
    for y in range(S):
        for x in range(S):
            # shield: rounded top, pointed bottom
            hw = 15 - max(0, (y - 22)) * 0.85
            if y < 4 or hw <= 0 or y > 37:
                continue
            dx = abs(x - cx)
            edge = dx - hw
            if edge > 1.5:
                continue
            if edge > 0.0:
                px[x, y] = ink
            elif edge > -2.0:
                px[x, y] = hi if x < cx else lo
            else:
                px[x, y] = base if (x + y) % 9 else shade(base, 1.08)
    # emblem: rank pips (rank+1 diamonds, up to 3 per row) over a crossed-sword hint
    pips = rank + 1
    rows = [pips] if pips <= 3 else [3, pips - 3]
    y0 = 11 if len(rows) == 2 else 15
    for r, n in enumerate(rows):
        for i in range(n):
            ox = int(cx - (n - 1) * 5 + i * 10)
            oy = y0 + r * 9
            for d in range(-3, 4):
                w = 3 - abs(d)
                for e in range(-w, w + 1):
                    px[ox + e, oy + d] = ink if (abs(e) == w or abs(d) == 3) else hi
    for y in range(26, 33):  # chevron at the bottom
        w = 7 - (y - 26)
        for x in range(int(cx) - w, int(cx) + w + 1):
            if abs(x - cx) > w - 1:
                px[x, y] = ink
    return img


def gen_badges():
    for r in range(6):
        save(badge(r), A, "textures", "gui", "guild", "badge_%d.png" % r)


if __name__ == "__main__":
    gen_coins()
    gen_board()
    gen_badges()
    print("guild assets written to", ROOT)
