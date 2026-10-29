"""Sinh texture + model item cho Tâm Tinh (tam_tinh) và Linh Tinh (linh_tinh). Cần: pip install pillow."""
import json, os
from PIL import Image

ROOT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), os.pardir, "src", "main", "resources", "assets", "hukhong"))
TEX = os.path.join(ROOT, "textures", "item")
MOD = os.path.join(ROOT, "models", "item")

# 16x16 gem silhouette (rows of x-ranges), outline + 3 tone shading
SHAPE = {2: (6, 9), 3: (4, 11), 4: (3, 12), 5: (2, 13), 6: (2, 13), 7: (2, 13), 8: (3, 12), 9: (3, 12),
         10: (4, 11), 11: (5, 10), 12: (6, 9), 13: (7, 8)}


def gem(light, mid, dark, outline, glow):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    for y, (x0, x1) in SHAPE.items():
        for x in range(x0, x1 + 1):
            edge = x in (x0, x1) or y in (min(SHAPE), max(SHAPE))
            if edge:
                c = outline
            elif y <= 5 and x <= 8:
                c = light
            elif y >= 9 or x >= 11:
                c = dark
            else:
                c = mid
            px[x, y] = c + (255,)
    px[5, 4] = (255, 255, 255, 255)
    px[6, 5] = (255, 255, 255, 255)
    for (x, y) in ((1, 1), (14, 2), (13, 13), (2, 12)):  # sparkle
        px[x, y] = glow + (200,)
    return im


def main():
    os.makedirs(TEX, exist_ok=True)
    os.makedirs(MOD, exist_ok=True)
    gem((255, 140, 150), (214, 48, 72), (128, 20, 44), (60, 8, 24), (255, 190, 200)).save(os.path.join(TEX, "tam_tinh.png"))
    gem((170, 230, 255), (70, 150, 235), (30, 70, 150), (12, 24, 70), (200, 240, 255)).save(os.path.join(TEX, "linh_tinh.png"))
    for n in ("tam_tinh", "linh_tinh"):
        with open(os.path.join(MOD, n + ".json"), "w", encoding="utf-8") as f:
            json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "hukhong:item/" + n}}, f)
    print("ok")


if __name__ == "__main__":
    main()
