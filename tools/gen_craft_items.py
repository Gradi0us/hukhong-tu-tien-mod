"""Sinh texture 16x16 cho nguyen lieu che tao (PLAN-v1-profession-crafting §0.3) va 3 quyen pho (§0.2).

Chay: python tools/gen_craft_items.py  (can Pillow). Ghi vao src/main/resources/assets/hukhong/textures/item/.
"""
import os
from PIL import Image, ImageDraw

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources",
                   "assets", "hukhong", "textures", "item")


def canvas():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def chu_sa():
    im = canvas(); d = ImageDraw.Draw(im)
    d.ellipse((3, 6, 13, 14), fill=(150, 20, 20, 255))
    d.ellipse((5, 4, 11, 10), fill=(190, 40, 30, 255))
    for x, y in [(6, 7), (9, 9), (7, 11), (11, 12), (4, 10)]:
        d.point((x, y), fill=(255, 110, 90, 255))
    d.line((2, 14, 14, 14), fill=(90, 10, 10, 255))
    return im


def phu_chi(tint):
    """Giay phu chi: dai giay vang nhat, dau mau (tint = mau dau theo phẩm)."""
    im = canvas(); d = ImageDraw.Draw(im)
    d.rectangle((3, 2, 12, 13), fill=(236, 222, 180, 255), outline=(150, 120, 70, 255))
    for y in (5, 8, 11):
        d.line((5, y, 10, y), fill=(120, 90, 50, 255))
    d.rectangle((9, 9, 11, 11), fill=tint + (255,))
    return im


def linh_mac():
    im = canvas(); d = ImageDraw.Draw(im)
    d.rounded_rectangle((4, 3, 11, 13), 2, fill=(30, 30, 40, 255), outline=(10, 10, 15, 255))
    d.rectangle((6, 1, 9, 3), fill=(90, 70, 50, 255))
    d.point((6, 6), fill=(120, 160, 255, 255)); d.point((7, 7), fill=(120, 160, 255, 255))
    d.point((8, 10), fill=(180, 60, 60, 255))
    return im


def toi_dich():
    im = canvas(); d = ImageDraw.Draw(im)
    d.rectangle((6, 1, 9, 3), fill=(150, 150, 160, 255))
    d.rounded_rectangle((4, 4, 11, 14), 3, fill=(90, 170, 230, 230), outline=(40, 90, 150, 255))
    d.rectangle((5, 8, 10, 13), fill=(120, 200, 255, 230))
    d.point((6, 6), fill=(230, 250, 255, 255))
    return im


def linh_than():
    im = canvas(); d = ImageDraw.Draw(im)
    d.polygon([(3, 12), (5, 5), (9, 4), (13, 9), (11, 13)], fill=(40, 40, 42, 255))
    d.polygon([(5, 11), (7, 7), (10, 8), (10, 11)], fill=(70, 70, 75, 255))
    d.point((8, 9), fill=(255, 140, 40, 255)); d.point((9, 10), fill=(255, 190, 80, 255))
    return im


def scroll(band):
    """Quyen pho: giay cuon, dai mau theo loai (band)."""
    im = canvas(); d = ImageDraw.Draw(im)
    d.rectangle((3, 3, 12, 12), fill=(232, 214, 165, 255), outline=(140, 105, 60, 255))
    d.ellipse((1, 3, 4, 12), fill=(200, 175, 120, 255), outline=(140, 105, 60, 255))
    d.ellipse((11, 3, 14, 12), fill=(200, 175, 120, 255), outline=(140, 105, 60, 255))
    d.rectangle((5, 6, 10, 9), fill=band + (255,))
    d.line((5, 11, 10, 11), fill=(120, 90, 50, 255))
    return im


def dau_thu():
    """Dau Thu: binh nut mau nau, nap go (Doan Tao Dai - buoc toi)."""
    im = canvas(); d = ImageDraw.Draw(im)
    d.rectangle((6, 1, 9, 3), fill=(110, 80, 40, 255))
    d.rounded_rectangle((4, 4, 11, 14), 3, fill=(170, 110, 40, 235), outline=(90, 55, 20, 255))
    d.rectangle((5, 8, 10, 13), fill=(210, 150, 70, 235))
    d.point((6, 6), fill=(250, 220, 160, 255))
    return im


TEX = {
    "chu_sa": chu_sa(),
    "phu_chi_ha": phu_chi((150, 170, 120)),
    "phu_chi_trung": phu_chi((200, 60, 50)),
    "phu_chi_thuong": phu_chi((70, 130, 220)),
    "linh_mac": linh_mac(),
    "toi_dich": toi_dich(),
    "dau_thu": dau_thu(),
    "linh_than": linh_than(),
    "dan_phuong": scroll((70, 170, 90)),
    "phu_pho": scroll((230, 200, 60)),
    "khi_pho": scroll((90, 120, 170)),
}

if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    for name, im in TEX.items():
        im.save(os.path.join(OUT, name + ".png"))
        print("wrote", name)
