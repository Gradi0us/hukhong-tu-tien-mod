"""Sinh icon ky nang pixel-art cho mod hukhong.

- 16x16 RGBA, khong anti-alias (moi pixel hoac opaque hoan toan, hoac trong suot).
- Moi icon co vien 1px toi (outline 8-huong) de noi tren HUD.
- slot_bg.png 20x20 la khung o ky nang, vien dong.

Chay: python D:\\mc_mod\\tools\\gen_skill_icons.py
"""
import math
import os

from PIL import Image

OUT_DIR = r"D:\mc_mod\src\main\resources\assets\hukhong\textures\gui\skill"
SIZE = 16
NEIGH8 = [(-1, -1), (0, -1), (1, -1), (-1, 0), (1, 0), (-1, 1), (0, 1), (1, 1)]


def rgba(hex_str):
    h = hex_str.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


OUTLINE = rgba("#140e06")
WHITE = rgba("#ffffff")
PALE = rgba("#fff9d6")
YEL_L = rgba("#fff3a0")
GOLD = rgba("#f5c542")
GOLD_D = rgba("#c8921a")
BROWN = rgba("#6b4423")
GREEN_L = rgba("#c4ffa8")
GREEN = rgba("#4cc23a")
GREEN_D = rgba("#23802a")
HALO_G = rgba("#7fe06a")
ORANGE = rgba("#ff8c1a")
ORANGE_L = rgba("#ffc56e")
RED_D = rgba("#d9461f")
SEP = rgba("#7a1a0c")
CUFF = rgba("#5a2a12")
STEEL_L = rgba("#eef2f6")
STEEL = rgba("#a7b0ba")
STEEL_D = rgba("#5f6873")
STEEL_RIM = rgba("#3e4650")
LOCK_L = rgba("#c9ced6")
LOCK = rgba("#8d939b")
LOCK_D = rgba("#50555d")
SHACKLE = rgba("#6f757e")
SHACKLE_L = rgba("#aab0b8")
KEYHOLE = rgba("#1c1c20")
BRONZE_OUT = rgba("#0c0806")
BRONZE_L = rgba("#d08c4a")
BRONZE = rgba("#a8662c")
BRONZE_D = rgba("#5e3a1a")
SLOT_IN_A = rgba("#221c18")
SLOT_IN_B = rgba("#1c1715")
SLOT_SHADE = rgba("#120e0b")


class Canvas:
    def __init__(self, w=SIZE, h=SIZE):
        self.w = w
        self.h = h
        self.px = [[None] * w for _ in range(h)]

    def set(self, x, y, color):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.px[y][x] = color

    def filled(self, x, y):
        return 0 <= x < self.w and 0 <= y < self.h and self.px[y][x] is not None

    def add_outline(self, color=OUTLINE):
        """Ve vien 1px cho cac pixel trong suot co pixel opaque ke ben (8-huong)."""
        targets = []
        for y in range(self.h):
            for x in range(self.w):
                if self.px[y][x] is None and any(
                    self.filled(x + dx, y + dy) for dx, dy in NEIGH8
                ):
                    targets.append((x, y))
        for x, y in targets:
            self.set(x, y, color)

    def to_image(self):
        img = Image.new("RGBA", (self.w, self.h), (0, 0, 0, 0))
        for y in range(self.h):
            for x in range(self.w):
                col = self.px[y][x]
                img.putpixel((x, y), col if col is not None else (0, 0, 0, 0))
        return img


def seg_dist(px, py, ax, ay, bx, by):
    dx, dy = bx - ax, by - ay
    t = ((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy)
    t = max(0.0, min(1.0, t))
    return math.hypot(px - (ax + t * dx), py - (ay + t * dy))


def in_poly(x, y, poly):
    inside = False
    j = len(poly) - 1
    for i in range(len(poly)):
        xi, yi = poly[i]
        xj, yj = poly[j]
        if (yi > y) != (yj > y) and x < (xj - xi) * (y - yi) / (yj - yi) + xi:
            inside = not inside
        j = i
    return inside


# ---------------------------------------------------------------- dao_1
def icon_dao_1():
    """Tia thanh quang vang trang: luoi kiem anh sang cheo + glow dither."""
    c = Canvas()
    blade = (4, 11, 12, 3)
    guard = (2, 9, 6, 13)
    grip = (4, 11, 1.5, 13.5)
    for y in range(SIZE):
        for x in range(SIZE):
            d_blade = seg_dist(x, y, *blade)
            if d_blade <= 0.6:
                c.set(x, y, WHITE)
            elif d_blade <= 1.4:
                c.set(x, y, YEL_L)
            elif seg_dist(x, y, *guard) <= 0.7:
                c.set(x, y, GOLD)
            elif seg_dist(x, y, *grip) <= 0.7:
                c.set(x, y, BROWN)
            elif d_blade <= 2.2 and (x + y) % 2 == 0:
                c.set(x, y, PALE)  # glow dither (khong anti-alias)
    for x, y in [(12, 12), (11, 13), (12, 13), (13, 13), (12, 14)]:
        c.set(x, y, WHITE)  # tia lap lanh
    c.add_outline()
    return c


# ---------------------------------------------------------------- dao_2
def icon_dao_2():
    """Buff: dau cong xanh la + vong hao quang."""
    c = Canvas()
    cx = cy = 7.5
    for y in range(SIZE):
        for x in range(SIZE):
            r = math.hypot(x - cx, y - cy)
            arm_v = abs(x - cx) <= 1.6 and abs(y - cy) <= 4.5
            arm_h = abs(y - cy) <= 1.6 and abs(x - cx) <= 4.5
            if arm_v or arm_h:
                core = abs(x - cx) <= 0.6 or abs(y - cy) <= 0.6
                c.set(x, y, GREEN_L if core else GREEN)
            elif 5.0 <= r <= 6.0:
                c.set(x, y, HALO_G)
    # bong toi duoi cac nhanh de tao khoi
    for y in range(SIZE):
        for x in range(SIZE):
            if c.px[y][x] == GREEN and (x in (6, 9) or y in (6, 9)):
                c.set(x, y, GREEN_D)
    c.add_outline()
    return c


# ---------------------------------------------------------------- dao_ult
def icon_dao_ult():
    """Sam thanh / thien phat vang: tia set zigzag vang."""
    c = Canvas()
    bolt = [(11, 1), (4, 9), (7.5, 9), (5, 14), (12, 6), (8.5, 6)]

    def inside_all(x, y):
        return in_poly(x, y, bolt)

    for y in range(SIZE):
        for x in range(SIZE):
            if not inside_all(x, y):
                continue
            # pixel "loi" = co it nhat 1 neighbour 4-huong nam ngoai
            edge = any(not inside_all(x + dx, y + dy) for dx, dy in
                       [(0, -1), (-1, 0), (1, 0), (0, 1)])
            if edge:
                c.set(x, y, GOLD_D)
            elif (x + y) % 3 == 0:
                c.set(x, y, YEL_L)
            else:
                c.set(x, y, GOLD)
    # loi sang trang o tam
    for x, y in [(7, 4), (6, 5), (6, 7), (7, 8)]:
        if c.px[y][x] is not None:
            c.set(x, y, WHITE)
    # tia sang nho
    for x, y in [(2, 4), (13, 8), (2, 12), (13, 2)]:
        c.set(x, y, WHITE)
    c.add_outline()
    return c


# ---------------------------------------------------------------- the_1
def icon_the_1():
    """Nam dam cam do."""
    c = Canvas()

    def body(x, y):
        if 4 <= x <= 11 and 3 <= y <= 13:
            return not ((x in (4, 11)) and (y in (3, 13)))
        return False

    def thumb(x, y):
        return 2 <= x <= 3 and 7 <= y <= 10 and not (x == 2 and y in (7, 10))

    def cuff(x, y):
        return 5 <= x <= 10 and y == 14

    for y in range(SIZE):
        for x in range(SIZE):
            if cuff(x, y):
                c.set(x, y, CUFF)
            elif body(x, y) or thumb(x, y):
                if y in (6, 9, 12) and 5 <= x <= 10 and body(x, y):
                    c.set(x, y, SEP)  # vach ngan cac ngon
                elif x >= 10 or y >= 12:
                    c.set(x, y, RED_D)
                elif x <= 5 and y <= 8:
                    c.set(x, y, ORANGE_L)
                else:
                    c.set(x, y, ORANGE)
    c.add_outline()
    return c


# ---------------------------------------------------------------- the_2
def icon_the_2():
    """Khien thep xam (thiet bo sam)."""
    c = Canvas()

    def half_w(y):
        if y <= 9:
            return 4.5
        return 4.5 - (y - 9) * 0.75

    def inside(x, y):
        return 2 <= y <= 14 and abs(x - 7.5) <= half_w(y)

    for y in range(SIZE):
        for x in range(SIZE):
            if not inside(x, y):
                continue
            edge = any(not inside(x + dx, y + dy) for dx, dy in
                       [(0, -1), (-1, 0), (1, 0), (0, 1)])
            if edge:
                c.set(x, y, STEEL_RIM)
            elif x in (7, 8) and 4 <= y <= 11:
                c.set(x, y, STEEL_L)  # song noi giua
            elif y >= 10 or x >= 10:
                c.set(x, y, STEEL_D)
            elif x <= 5 and y <= 7:
                c.set(x, y, STEEL_L)
            else:
                c.set(x, y, STEEL)
    c.add_outline()
    return c


# ---------------------------------------------------------------- the_ult
def icon_the_ult():
    """Kim than vang dong: hinh nguoi vang co hao quang."""
    c = Canvas()

    def head(x, y):
        return math.hypot(x - 7.5, y - 4.5) <= 2.6

    def torso(x, y):
        return (4 <= x <= 11 and 7 <= y <= 13) or (3 <= x <= 12 and 8 <= y <= 9)

    def arms(x, y):
        return (x in (2, 3, 12, 13)) and 9 <= y <= 12

    for y in range(SIZE):
        for x in range(SIZE):
            r = math.hypot(x - 7.5, y - 7.5)
            if 6.0 <= r <= 6.8 and not (head(x, y) or torso(x, y)):
                c.set(x, y, PALE)  # hao quang
            elif head(x, y) or torso(x, y) or arms(x, y):
                if x <= 5 and y <= 10:
                    c.set(x, y, YEL_L)
                elif x >= 10 or y >= 12:
                    c.set(x, y, GOLD_D)
                else:
                    c.set(x, y, GOLD)
    c.add_outline()
    return c


# ---------------------------------------------------------------- locked
def icon_locked():
    """O khoa xam."""
    c = Canvas()

    def shackle(x, y):
        r = math.hypot(x - 7.5, y - 6.0)
        return y <= 6 and 2.6 <= r <= 4.2

    def body(x, y):
        if not (3 <= x <= 12 and 7 <= y <= 14):
            return False
        return not ((x, y) in [(3, 7), (12, 7), (3, 14), (12, 14)])

    def keyhole(x, y):
        return (x, y) in [(7, 9), (8, 9), (7, 10), (8, 10), (7, 11), (8, 11)]

    for y in range(SIZE):
        for x in range(SIZE):
            if keyhole(x, y):
                c.set(x, y, KEYHOLE)
            elif body(x, y):
                if x <= 5 and y <= 10:
                    c.set(x, y, LOCK_L)
                elif x >= 10 or y >= 12:
                    c.set(x, y, LOCK_D)
                else:
                    c.set(x, y, LOCK)
            elif shackle(x, y):
                c.set(x, y, SHACKLE_L if x <= 6 else SHACKLE)
    c.add_outline()
    return c


# ---------------------------------------------------------------- slot_bg
def icon_slot_bg():
    """Khung o ky nang 20x20, vien dong, long toi."""
    size = 20
    c = Canvas(size, size)
    for y in range(size):
        for x in range(size):
            ring = min(x, y, size - 1 - x, size - 1 - y)
            if ring == 0:
                col = BRONZE_OUT
            elif ring == 1:
                col = BRONZE_L if (x < 10 and y < 10) else BRONZE
            elif ring == 2:
                col = BRONZE
            elif ring == 3:
                col = BRONZE_D
            elif ring == 4:
                col = SLOT_SHADE
            else:
                col = SLOT_IN_A if (x + y) % 2 == 0 else SLOT_IN_B
            c.set(x, y, col)
    return c


ICONS = {
    "dao_1.png": icon_dao_1,
    "dao_2.png": icon_dao_2,
    "dao_ult.png": icon_dao_ult,
    "the_1.png": icon_the_1,
    "the_2.png": icon_the_2,
    "the_ult.png": icon_the_ult,
    "locked.png": icon_locked,
    "slot_bg.png": icon_slot_bg,
}


def main():
    os.makedirs(OUT_DIR, exist_ok=True)
    for name, fn in ICONS.items():
        img = fn().to_image()
        path = os.path.join(OUT_DIR, name)
        img.save(path, "PNG")
        print(f"wrote {path} {img.size} {img.mode}")


if __name__ == "__main__":
    main()
