"""gen_trung_sinh_dan.py - texture 16x16 cho Trung Sinh Dan (item/trung_sinh_dan.png).

Chay: python tools/gen_trung_sinh_dan.py  (tu thu muc goc repo, can PIL + tools/pixel_shade.py)
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from pixel_shade import mask, render  # noqa: E402

OUT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "hukhong", "textures", "item", "trung_sinh_dan.png")


def c(h):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16))


def main():
    # Hinh thoi (kim cuong tai sinh) mau than do-tim, diem nhan vang, vong sang tim.
    m = mask(lambda d: d.polygon([(7, 1), (14, 7), (7, 14), (1, 7)], fill=255))
    hi, ring = c("#fff3e0"), c("#ffd54f")
    acc = [(5, 5, hi), (6, 5, hi), (5, 6, hi)]  # diem sang
    acc +=[(7, 4, ring), (7, 5, ring), (7, 9, ring), (7, 10, ring), (4, 7, ring), (5, 7, ring), (9, 7, ring), (10, 7, ring)]
    img = render(m, c("#b71c5c"), sum(map(ord, "trung_sinh_dan")), speckle=0.0, accents=acc)
    img.save(OUT)
    print("wrote", os.path.normpath(OUT), img.size)


if __name__ == "__main__":
    main()
