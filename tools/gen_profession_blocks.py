"""Sinh texture 16x16 cho Phu An, Doan Tao Dai va Linh Thiet Phoi (nghe phu).
Chay: python tools/gen_profession_blocks.py  (can Pillow). Ghi vao src/main/resources/assets/hukhong/textures."""
import os
from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "hukhong", "textures")


def save(img, rel):
    path = os.path.join(ROOT, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


def phu_an(face):
    # ban ve phu: go nau, mat ban co vien, goc ghi muc
    im = Image.new("RGBA", (16, 16), (110, 74, 40, 255))
    d = ImageDraw.Draw(im)
    if face == "top":
        d.rectangle([1, 1, 14, 14], fill=(214, 196, 150, 255), outline=(90, 60, 30, 255))
        d.rectangle([3, 3, 12, 12], outline=(190, 60, 50, 255))
        d.point([(7, 7), (8, 8)], fill=(20, 20, 60, 255))
    elif face == "side":
        for x in range(0, 16, 4):
            d.line([(x, 0), (x, 15)], fill=(80, 50, 25, 255))
        d.rectangle([5, 6, 10, 10], fill=(214, 196, 150, 255))
        d.point([(7, 8)], fill=(190, 60, 50, 255))
    else:
        d.rectangle([0, 0, 15, 15], fill=(70, 45, 25, 255))
    return im


def doan_tao_dai(face):
    # dai ran: sat xam, than lo lua o mat tren
    im = Image.new("RGBA", (16, 16), (80, 84, 92, 255))
    d = ImageDraw.Draw(im)
    if face == "top":
        d.rectangle([1, 1, 14, 14], fill=(60, 62, 70, 255), outline=(150, 155, 165, 255))
        d.ellipse([5, 5, 10, 10], fill=(255, 130, 40, 255))
    elif face == "side":
        d.rectangle([0, 0, 15, 4], fill=(120, 124, 132, 255))
        d.rectangle([3, 8, 12, 11], fill=(255, 110, 30, 255))
    else:
        d.rectangle([0, 0, 15, 15], fill=(55, 57, 64, 255))
    return im


def linh_thiet_phoi():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.polygon([(3, 10), (6, 5), (13, 6), (12, 12), (5, 13)], fill=(170, 200, 230, 255), outline=(90, 110, 140, 255))
    d.point([(8, 8)], fill=(255, 255, 255, 255))
    return im


if __name__ == "__main__":
    for name, fn in (("phu_an", phu_an), ("doan_tao_dai", doan_tao_dai)):
        for face in ("top", "side", "bottom"):
            save(fn(face), f"block/{name}_{face}.png")
    save(linh_thiet_phoi(), "item/linh_thiet_phoi.png")
    print("ok")
