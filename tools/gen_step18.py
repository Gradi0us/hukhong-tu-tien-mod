"""gen_step18.py - Buoc 18: texture + model cho Huyet Anh Lenh (item)."""
import json
import os
from PIL import Image, ImageDraw

RES = r"D:\mc_mod\src\main\resources"
im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
d = ImageDraw.Draw(im)
d.rounded_rectangle((3, 1, 12, 14), radius=2, fill=(58, 14, 20, 255), outline=(20, 6, 8, 255))
d.ellipse((5, 4, 10, 9), fill=(190, 20, 40, 255), outline=(255, 90, 90, 255))
d.rectangle((7, 9, 8, 12), fill=(190, 20, 40, 255))
d.point((6, 5), fill=(255, 200, 200, 255))
tex = os.path.join(RES, "assets", "hukhong", "textures", "item")
os.makedirs(tex, exist_ok=True)
im.save(os.path.join(tex, "huyet_anh_lenh.png"))
mp = os.path.join(RES, "assets", "hukhong", "models", "item", "huyet_anh_lenh.json")
with open(mp, "w", encoding="utf-8") as f:
    json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "hukhong:item/huyet_anh_lenh"}}, f)
