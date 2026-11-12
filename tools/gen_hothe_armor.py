"""Sinh asset cho 4 Pháp Khí hộ thể: 20 icon 16x16, 10 texture lớp giáp (5 phẩm x layer_1/layer_2), 20 model item.
Chạy: python tools/gen_hothe_armor.py  (cần Pillow). Ghi vào src/main/resources/assets/hukhong.
Màu phẩm khớp PhapKhiTier.rgb: Phàm, Linh, Huyền, Địa, Thiên."""
import json
import os
from PIL import Image, ImageDraw

ASSETS = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "hukhong")

TIERS = [("pham", (0xAA, 0xAA, 0xAA)), ("linh", (0x55, 0xFF, 0x77)), ("huyen", (0x55, 0x99, 0xFF)),
         ("dia", (0xB0, 0x60, 0xFF)), ("thien", (0xFF, 0xAA, 0x00))]
KINDS = ["tinh_tam_quan", "ho_tam_giap", "kim_cang_ho_thoi", "phi_van_ngoa"]


def dark(c, k=0.55):
    return tuple(int(v * k) for v in c)


def icon(kind, rgb):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    base, edge = rgb + (255,), dark(rgb) + (255,)
    if kind == "tinh_tam_quan":      # mũ: vành + đỉnh
        d.rectangle([3, 6, 12, 10], fill=base, outline=edge)
        d.rectangle([5, 3, 10, 5], fill=base, outline=edge)
    elif kind == "ho_tam_giap":      # áo giáp: thân + vai
        d.polygon([(3, 3), (6, 2), (10, 2), (13, 3), (13, 13), (3, 13)], fill=base, outline=edge)
        d.line([(8, 4), (8, 12)], fill=edge)
    elif kind == "kim_cang_ho_thoi":  # quần: hai ống
        d.rectangle([3, 2, 12, 5], fill=base, outline=edge)
        d.polygon([(3, 5), (7, 5), (6, 14), (3, 14)], fill=base, outline=edge)
        d.polygon([(9, 5), (12, 5), (12, 14), (9, 14)], fill=base, outline=edge)
    else:                            # giày: ngoa
        d.polygon([(2, 9), (6, 7), (7, 4), (10, 4), (9, 10), (14, 11), (14, 13), (2, 13)], fill=base, outline=edge)
    d.point([(5, 4)], fill=(255, 255, 255, 255))
    return im


def armor_layer(rgb, layer):
    # Lớp giáp 64x32 (vanilla): tông phẩm, viền tối; layer_2 là ống quần.
    im = Image.new("RGBA", (64, 32), rgb + (255,))
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, 63, 1], fill=dark(rgb) + (255,))
    d.rectangle([0, 30, 63, 31], fill=dark(rgb) + (255,))
    for x in range(0, 64, 16):
        d.line([(x, 0), (x, 31)], fill=dark(rgb, 0.75) + (255,))
    if layer == 2:
        d.rectangle([16, 16, 47, 20], fill=dark(rgb, 0.8) + (255,))
    return im


def write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(obj, f)


if __name__ == "__main__":
    for tier, rgb in TIERS:
        for layer in (1, 2):
            p = os.path.join(ASSETS, "textures", "models", "armor", f"hothe_{tier}_layer_{layer}.png")
            os.makedirs(os.path.dirname(p), exist_ok=True)
            armor_layer(rgb, layer).save(p)
        for kind in KINDS:
            name = f"hothe_{kind}_{tier}"
            p = os.path.join(ASSETS, "textures", "item", f"{name}.png")
            os.makedirs(os.path.dirname(p), exist_ok=True)
            icon(kind, rgb).save(p)
            write_json(os.path.join(ASSETS, "models", "item", f"{name}.json"),
                       {"parent": "minecraft:item/generated", "textures": {"layer0": f"hukhong:item/{name}"}})
    print("ok")
