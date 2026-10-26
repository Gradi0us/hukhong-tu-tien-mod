"""gen_step14.py - Buoc 14: texture + blockstate/model/item/loot cho Linh Khi Dai, Bang Tong Mon, Bang Thanh."""
import json
import os
from PIL import Image, ImageDraw

RES = r"D:\mc_mod\src\main\resources"
TEX = os.path.join(RES, "assets", "hukhong", "textures", "block")
OL = (24, 16, 20, 255)


def c(h):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


def noise(base, dark, light, seed):
    im = Image.new("RGBA", (16, 16), c(base))
    p = im.load()
    for y in range(16):
        for x in range(16):
            v = (x * 7 + y * 13 + seed * 5) % 11
            if v == 0:
                p[x, y] = c(dark)
            elif v == 5:
                p[x, y] = c(light)
    return im


def frame(im, col):
    d = ImageDraw.Draw(im)
    d.rectangle((0, 0, 15, 15), outline=col)
    return im


def save(im, name):
    os.makedirs(TEX, exist_ok=True)
    im.save(os.path.join(TEX, name + ".png"))


def wjson(path, obj):
    full = os.path.join(RES, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, "w", encoding="utf-8") as f:
        json.dump(obj, f, ensure_ascii=False)


# ---- Linh Khi Dai: da xanh nhat, tran phap tran sang ----
side = frame(noise("#3b4f66", "#2c3e52", "#4f6a86", 2), c("#22303f"))
bottom = frame(noise("#2c3e52", "#22303f", "#3b4f66", 3), c("#1a2430"))
top = noise("#5b86b0", "#4a7099", "#7fb0d8", 4)
d = ImageDraw.Draw(top)
d.ellipse((2, 2, 13, 13), outline=c("#d8f4ff"))
d.ellipse((5, 5, 10, 10), outline=c("#a9e6ff"))
d.line((8, 1, 8, 14), fill=c("#d8f4ff"))
d.line((1, 8, 14, 8), fill=c("#d8f4ff"))
d.rectangle((7, 7, 8, 8), fill=c("#ffffff"))
frame(top, c("#2c3e52"))
save(side, "linh_khi_dai_side")
save(bottom, "linh_khi_dai_bottom")
save(top, "linh_khi_dai_top")

# ---- Bang Tong Mon: go toi + giay do ----
bs = noise("#5a3d28", "#46301f", "#6e4c33", 6)
d = ImageDraw.Draw(bs)
d.rectangle((2, 2, 13, 11), fill=c("#d9c9a0"), outline=c("#46301f"))
for y in (4, 6, 8):
    d.line((4, y, 11, y), fill=c("#9a3a2a"))
save(frame(bs, c("#2e1f14")), "tong_mon_bang_side")
save(frame(noise("#6e4c33", "#5a3d28", "#84603f", 7), c("#2e1f14")), "tong_mon_bang_top")

# ---- Bang Thanh: da xam + giay xanh ----
cs = noise("#6a6a72", "#55555d", "#80808a", 8)
d = ImageDraw.Draw(cs)
d.rectangle((2, 2, 13, 11), fill=c("#cfd8e0"), outline=c("#3a3a42"))
for y in (4, 6, 8):
    d.line((4, y, 11, y), fill=c("#2a5a9a"))
save(frame(cs, c("#2a2a30")), "thanh_bang_side")
save(frame(noise("#80808a", "#6a6a72", "#9a9aa4", 9), c("#2a2a30")), "thanh_bang_top")

# ---- JSON ----
for name in ("linh_khi_dai", "tong_mon_bang", "thanh_bang"):
    wjson(f"assets/hukhong/blockstates/{name}.json", {"variants": {"": {"model": f"hukhong:block/{name}"}}})
    wjson(f"assets/hukhong/models/item/{name}.json", {"parent": f"hukhong:block/{name}"})
    wjson(f"data/hukhong/loot_table/blocks/{name}.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "bonus_rolls": 0,
                   "entries": [{"type": "minecraft:item", "name": f"hukhong:{name}"}],
                   "conditions": [{"condition": "minecraft:survives_explosion"}]}]})

wjson("assets/hukhong/models/block/linh_khi_dai.json", {
    "parent": "minecraft:block/slab",
    "textures": {"bottom": "hukhong:block/linh_khi_dai_bottom", "top": "hukhong:block/linh_khi_dai_top",
                 "side": "hukhong:block/linh_khi_dai_side"}})
for name in ("tong_mon_bang", "thanh_bang"):
    wjson(f"assets/hukhong/models/block/{name}.json", {
        "parent": "minecraft:block/cube_bottom_top",
        "textures": {"top": f"hukhong:block/{name}_top", "side": f"hukhong:block/{name}_side",
                     "bottom": f"hukhong:block/{name}_top"}})
print("gen_step14 done")
