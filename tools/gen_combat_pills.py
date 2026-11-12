"""Sinh texture 16x16 cho 4 đan chiến đấu (PLAN-v1-profession-crafting §1.4) và model item.

Chạy: python tools/gen_combat_pills.py  (cần Pillow). Ghi texture vào textures/item/, model vào models/item/.
"""
import json
import os
from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "hukhong")
TEX = os.path.join(ROOT, "textures", "item")
MODEL = os.path.join(ROOT, "models", "item")


def canvas():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def pill(body, hi, line):
    """Viên đan tròn 10x8 với vạch màu riêng cho từng loại."""
    im = canvas(); d = ImageDraw.Draw(im)
    d.ellipse((3, 4, 12, 13), fill=body + (255,), outline=line + (255,))
    d.ellipse((5, 5, 8, 8), fill=hi + (255,))
    d.line((4, 9, 11, 9), fill=line + (255,))
    return im


def hoi_khi():  # xanh lục nhạt
    return pill((90, 190, 140), (200, 255, 225), (30, 90, 70))


def cuong_huyet():  # đỏ thẫm
    return pill((170, 30, 40), (255, 120, 110), (80, 10, 20))


def kim_cuong():  # vàng nhạt, ánh kim
    return pill((220, 200, 110), (255, 250, 220), (120, 100, 40))


def phong_hanh():  # xanh dương nhạt
    return pill((110, 170, 235), (220, 240, 255), (40, 70, 130))


PILLS = {
    "hoi_khi_dan": hoi_khi,
    "cuong_huyet_dan": cuong_huyet,
    "kim_cuong_chien_dan": kim_cuong,
    "phong_hanh_chien_dan": phong_hanh,
}

if __name__ == "__main__":
    os.makedirs(TEX, exist_ok=True)
    os.makedirs(MODEL, exist_ok=True)
    for name, fn in PILLS.items():
        fn().save(os.path.join(TEX, name + ".png"))
        with open(os.path.join(MODEL, name + ".json"), "w", encoding="utf-8") as f:
            json.dump({"parent": "minecraft:item/generated",
                       "textures": {"layer0": "hukhong:item/" + name}}, f)
    print("ok", len(PILLS))
