"""gen_step12.py - pixel art Buoc 12 (Dan Lo): block, thao duoc, dan, nen GUI."""
import os
from PIL import Image, ImageDraw

ROOT = r"D:\mc_mod\src\main\resources\assets\hukhong\textures"
OL = (24, 16, 20, 255)


def c(h):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


def save(img, *p):
    path = os.path.join(ROOT, *p)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


def new():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def outline(im):
    px = im.load()
    pts = [(x, y) for y in range(16) for x in range(16) if px[x, y][3] == 0]
    for x, y in pts:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, ny = x + dx, y + dy
            if 0 <= nx < 16 and 0 <= ny < 16 and px[nx, ny][3] and px[nx, ny] != OL:
                px[x, y] = OL
                break
    return im


# ---- block ----
def noise(base, dark, light, seed):
    im = Image.new("RGBA", (16, 16), c(base))
    p = im.load()
    for y in range(16):
        for x in range(16):
            v = (x * 7 + y * 13 + seed * 5) % 11
            if v == 0: p[x, y] = c(dark)
            elif v == 5: p[x, y] = c(light)
    return im


side = noise("#4a4452", "#38333f", "#5e5868", 1)
d = ImageDraw.Draw(side)
d.rectangle((0, 0, 15, 1), fill=c("#2b2733"))
d.rectangle((0, 14, 15, 15), fill=c("#2b2733"))
d.rectangle((4, 5, 11, 12), fill=c("#1c1820"))  # mieng lo
d.rectangle((5, 6, 10, 11), fill=c("#d84315"))
d.rectangle((6, 8, 9, 11), fill=c("#ffb300"))
d.point((7, 9), fill=c("#fff59d"))
save(side, "block", "dan_lo_side.png")
top = noise("#3e3946", "#2f2b36", "#57515f", 2)
d = ImageDraw.Draw(top)
d.ellipse((3, 3, 12, 12), fill=c("#1c1820"))
d.ellipse((4, 4, 11, 11), fill=c("#7b3a1d"))
d.ellipse((6, 6, 9, 9), fill=c("#ffb300"))
save(top, "block", "dan_lo_top.png")
save(noise("#2f2b36", "#25222b", "#3e3946", 3), "block", "dan_lo_bottom.png")


# ---- herbs ----
def herb(name, stem, leaf, leaf2, flower=None):
    im = new(); d = ImageDraw.Draw(im)
    d.line((8, 14, 8, 6), fill=c(stem))
    d.polygon([(8, 10), (3, 6), (4, 9)], fill=c(leaf))
    d.polygon([(8, 9), (13, 5), (12, 8)], fill=c(leaf2))
    d.polygon([(8, 6), (6, 2), (10, 2)], fill=c(leaf))
    if flower: d.rectangle((7, 1, 9, 3), fill=c(flower))
    save(outline(im), "item", name + ".png")


herb("linh_thao", "#2e7d32", "#66bb6a", "#a5d6a7", "#e1f5fe")
herb("thanh_diep", "#1b5e20", "#2e9e5b", "#43a047")
herb("huyet_tham", "#8d1c1c", "#c62828", "#ef5350", "#ffcdd2")
im = new(); d = ImageDraw.Draw(im)
d.polygon([(8, 3), (13, 8), (8, 13), (3, 8)], fill=c("#ff7043"))
d.polygon([(8, 5), (11, 8), (8, 11), (5, 8)], fill=c("#ffd54f"))
d.point((8, 8), fill=c("#ffffff"))
save(outline(im), "item", "hoa_lien.png")
im = new(); d = ImageDraw.Draw(im)
d.ellipse((3, 3, 12, 12), fill=c("#6a1b9a"))
d.ellipse((5, 5, 10, 10), fill=c("#ff6e40"))
d.ellipse((6, 6, 8, 8), fill=c("#fff3e0"))
save(outline(im), "item", "di_hoa_chau.png")


# ---- pills ----
def pill(name, body, hi, ring):
    im = new(); d = ImageDraw.Draw(im)
    d.ellipse((3, 3, 12, 12), fill=c(body))
    d.arc((3, 3, 12, 12), 200, 340, fill=c(ring))
    d.ellipse((5, 5, 7, 7), fill=c(hi))
    save(outline(im), "item", name + ".png")


pill("thong_huyen_dan", "#3f8ae0", "#bbdefb", "#1565c0")
pill("tu_vuong_dan", "#8e44ad", "#e1bee7", "#4a148c")
pill("hoang_cuc_dan", "#e6a817", "#fff8e1", "#8d6e00")
pill("de_hoan_dan", "#d4382c", "#ffcdd2", "#7f0000")
pill("chi_huyet_dan", "#ef5350", "#ffebee", "#b71c1c")
pill("hoi_xuan_dan", "#66bb6a", "#e8f5e9", "#1b5e20")
pill("giai_doc_dan", "#26c6da", "#e0f7fa", "#006064")

# ---- GUI 256x256, dung 176x166 ----
g = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
d = ImageDraw.Draw(g)
d.rectangle((0, 0, 175, 165), fill=c("#c6c6c6"), outline=c("#000000"))
d.rectangle((1, 1, 174, 164), outline=c("#ffffff"))
d.rectangle((2, 2, 173, 163), outline=c("#555555"))


def slot(x, y):
    d.rectangle((x - 1, y - 1, x + 16, y + 16), fill=c("#8b8b8b"), outline=c("#373737"))


for x, y in ((8, 18), (26, 18), (44, 18), (8, 44), (44, 44), (138, 20)):
    slot(x, y)
d.rectangle((136, 18, 157, 39), outline=c("#b8860b"))  # vien o thanh pham
for r in range(3):
    for k in range(9): slot(8 + k * 18, 84 + r * 18)
for k in range(9): slot(8 + k * 18, 142)
d.rectangle((85, 13, 98, 64), fill=c("#373737"))  # khung thanh nhiet
save(g, "gui", "dan_lo.png")
print("ok")
