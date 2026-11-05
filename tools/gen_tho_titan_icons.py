"""Icon 16x16 cho kỹ năng trụ cấp 3 Thổ (Thái Sơn Thần Tháp): tho_titan_1 (Q Liệt Hoả Liên Pháo), tho_titan_2 (E tự huỷ),
tho_titan_3 (R Địa Thứ Hộ Tháp), tho_titan_ult (T Tia Diệt Thế). Chạy: python -X utf8 tools/gen_tho_titan_icons.py"""
import math
import os
from PIL import Image

OUT = os.path.normpath(os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "hukhong", "textures", "gui", "skill"))
S = 16


def hx(h, a=255):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255, a)


def canvas(bg_top, bg_bot):
    img = Image.new("RGBA", (S, S))
    for y in range(S):
        t = y / (S - 1)
        c = tuple(int(bg_top[i] * (1 - t) + bg_bot[i] * t) for i in range(3)) + (255,)
        for x in range(S):
            img.putpixel((x, y), c)
    return img


def put(img, x, y, c):
    if 0 <= x < S and 0 <= y < S:
        img.putpixel((int(x), int(y)), c)


def disc(img, cx, cy, r, c):
    for y in range(S):
        for x in range(S):
            if (x + .5 - cx) ** 2 + (y + .5 - cy) ** 2 <= r * r:
                put(img, x, y, c)


def border(img, c, corner):
    for i in range(S):
        put(img, i, 0, c); put(img, i, S - 1, c); put(img, 0, i, c); put(img, S - 1, i, c)
    for (x, y) in ((1, 1), (14, 1), (1, 14), (14, 14)):
        put(img, x, y, corner)


def q():  # 3 đạn đá nung bay chéo, đuôi lửa
    img = canvas((40, 20, 10), (90, 30, 8))
    for k, (cx, cy) in enumerate(((4.5, 11.5), (8, 8), (11.5, 4.5))):
        for t in range(1, 5):
            put(img, cx - t, cy + t, hx(0xFF8A2A, 220 - t * 40))
            put(img, cx - t + 1, cy + t, hx(0xFFC060, 160 - t * 30))
        disc(img, cx, cy, 1.9, hx(0x5A3A20))
        disc(img, cx + .3, cy - .3, 1.0, hx(0xFF9A3A))
        put(img, cx + .5, cy - .8, hx(0xFFF0C0))
    border(img, hx(0xFF8A2A), hx(0xFFE9A0))
    return img


def e():  # lõi tự huỷ: quả cầu đỏ rực + vòng cảnh báo + tia nổ
    img = canvas((30, 8, 6), (70, 12, 6))
    for a in range(12):
        ang = a * math.pi / 6
        for r in range(5, 8):
            put(img, 8 + math.cos(ang) * r, 8 + math.sin(ang) * r, hx(0xFF5A14, 200 if r < 7 else 120))
    disc(img, 8, 8, 4.2, hx(0x8A1A08))
    disc(img, 8, 8, 3.0, hx(0xFF5A14))
    disc(img, 8, 8, 1.6, hx(0xFFE9A0))
    for y in range(S):
        for x in range(S):
            if abs(math.hypot(x + .5 - 8, y + .5 - 8) - 6.6) < .5:
                put(img, x, y, hx(0xFFD27A))
    border(img, hx(0xFF2A10), hx(0xFFE9A0))
    return img


def r():  # cụm gai đá trồi từ mặt đất
    img = canvas((50, 40, 30), (30, 22, 14))
    for x in range(1, 15):
        put(img, x, 13, hx(0x6B4A26)); put(img, x, 14, hx(0x4A3018))
    for bx, h, w in ((4, 8, 2), (8, 11, 3), (12, 7, 2)):
        for y in range(h):
            half = w * (1 - y / h)
            for dx in range(-int(half + .5), int(half + .5) + 1):
                c = 0xB08A4F if dx <= 0 else 0x7A5A30
                put(img, bx + dx, 12 - y, hx(c))
        put(img, bx, 12 - h, hx(0xFFD27A))
    border(img, hx(0xC49A5C), hx(0xFFE9A0))
    return img


def t():  # tia diệt thế khổng lồ từ lõi chéo xuống
    img = canvas((20, 10, 25), (60, 20, 10))
    for i in range(-2, 18):
        x, y = i, i
        for w in range(-3, 4):
            a = 255 - abs(w) * 55
            c = 0xFFFFFF if abs(w) == 0 else 0xFFD27A if abs(w) == 1 else 0xFF5A14
            put(img, x + w, y - w, hx(c, max(60, a)))
    disc(img, 3, 3, 2.6, hx(0xFFF0C0))
    disc(img, 13, 13, 2.2, hx(0xFF8A2A))
    border(img, hx(0xFFD27A), hx(0xFFFFFF))
    return img


def main():
    os.makedirs(OUT, exist_ok=True)
    for name, fn in (("tho_titan_1", q), ("tho_titan_2", e), ("tho_titan_3", r), ("tho_titan_ult", t)):
        fn().save(os.path.join(OUT, name + ".png"))
    print("wrote 4 icons ->", OUT)


if __name__ == "__main__":
    main()
