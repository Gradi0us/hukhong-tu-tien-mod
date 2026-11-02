"""Sinh icon ky nang pixel-art cho mod hukhong.

- 16x16 RGBA, khong anti-alias (moi pixel hoac opaque hoan toan, hoac trong suot).
- Moi icon co vien 1px toi (outline 8-huong) de noi tren HUD.
- slot_bg.png 20x20 la khung o ky nang, vien dong.

Chay: python D:\\mc_mod\\tools\\gen_skill_icons.py
"""
import math
import os

from PIL import Image

OUT_DIR = os.path.normpath(os.path.join(
    os.path.dirname(os.path.abspath(__file__)),
    os.pardir, "src", "main", "resources",
    "assets", "hukhong", "textures", "gui", "skill",
))
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

    def bevel(self, amt=0.22):
        """Shading 3 tong: pixel co o trong suot phia tren/trai duoc lam sang,
        phia duoi/phai duoc lam toi. Chay truoc khi ve outline."""
        snap = [row[:] for row in self.px]
        for y in range(self.h):
            for x in range(self.w):
                col = snap[y][x]
                if col is None or col == OUTLINE:
                    continue
                up_open = y == 0 or snap[y - 1][x] is None
                left_open = x == 0 or snap[y][x - 1] is None
                down_open = y == self.h - 1 or snap[y + 1][x] is None
                right_open = x == self.w - 1 or snap[y][x + 1] is None
                if up_open or left_open:
                    col = tuple(int(round(c + (255 - c) * amt)) for c in col[:3]) + (255,)
                elif down_open or right_open:
                    col = tuple(int(round(c * (1 - amt))) for c in col[:3]) + (255,)
                self.px[y][x] = col

    def add_outline(self, color=OUTLINE):
        """Ve vien 1px cho cac pixel trong suot co pixel opaque ke ben (8-huong)."""
        self.bevel()
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
                col = BRONZE if (x + y) % 4 else BRONZE_L  # rivet-dot trim
            elif ring == 3:
                col = BRONZE_D
            elif ring == 4:
                col = SLOT_SHADE
            else:
                col = SLOT_IN_A if (x + y) % 2 == 0 else SLOT_IN_B
            c.set(x, y, col)
    # corner rivets (inside the bronze ring, lit top-left)
    for x, y in ((3, 3), (16, 3), (3, 16), (16, 16)):
        c.set(x, y, BRONZE_L if x < 10 else BRONZE_D)
    # inner top-left lip so the slot reads as recessed
    for i in range(5, 15):
        c.set(i, 5, SLOT_SHADE)
        c.set(5, i, SLOT_SHADE)
    return c


# ---------------------------------------------------------------- Tran / Dan (mau bo sung)
CYAN_L = rgba("#d4f8ff")
CYAN = rgba("#4fd6ff")
CYAN_D = rgba("#1b7fa6")
PINK_L = rgba("#ffe0f0")
PINK = rgba("#ff7fb8")
PINK_D = rgba("#c2407f")
POISON_L = rgba("#d8ff9a")
POISON = rgba("#6fd13e")
POISON_D = rgba("#2e6b1c")
FIRE_Y = rgba("#ffe066")
FLAME_RED = rgba("#ff4a1c")


def _tri_edges(cx, cy, r, base_deg):
    pts = [(cx + r * math.cos(math.radians(a)), cy + r * math.sin(math.radians(a)))
           for a in (base_deg, base_deg + 120, base_deg + 240)]
    return [(pts[k], pts[(k + 1) % 3]) for k in range(3)]


def _capsule(c, a, b, r, col_a, col_b):
    """Vien thuoc (capsule) tu a toi b, ban kinh r; nua dau col_a, nua cuoi col_b."""
    ax, ay = a
    bx, by = b
    dx, dy = bx - ax, by - ay
    l2 = dx * dx + dy * dy
    for y in range(SIZE):
        for x in range(SIZE):
            if seg_dist(x, y, ax, ay, bx, by) <= r:
                t = ((x - ax) * dx + (y - ay) * dy) / l2
                c.set(x, y, col_a if t < 0.5 else col_b)


# ---------------------------------------------------------------- tran_1
def icon_tran_1():
    """Khon Tran: vong phu van vang-lam xen ke, tam tam giac sang."""
    c = Canvas()
    cx = cy = 7.5
    edges = _tri_edges(cx, cy, 4.2, -90)
    for y in range(SIZE):
        for x in range(SIZE):
            d = math.hypot(x - cx, y - cy)
            ang = math.degrees(math.atan2(y - cy, x - cx)) % 360
            if 5.2 <= d <= 6.6:
                c.set(x, y, GOLD if int(ang // 45) % 2 == 0 else CYAN)
            elif any(seg_dist(x, y, *p, *q) <= 0.5 for p, q in edges):
                c.set(x, y, CYAN_L)
            elif d <= 0.9:
                c.set(x, y, WHITE)
    c.add_outline()
    return c


# ---------------------------------------------------------------- tran_2
def icon_tran_2():
    """Ho Than Tran: khien tron lam lam, vong sang lam va tam vang."""
    c = Canvas()
    cx = cy = 7.5
    for y in range(SIZE):
        for x in range(SIZE):
            d = math.hypot(x - cx, y - cy)
            if d <= 6.2:
                if 5.2 <= d:
                    c.set(x, y, CYAN_L)
                elif 3.6 <= d <= 4.3:
                    c.set(x, y, CYAN)
                elif d <= 1.4:
                    c.set(x, y, GOLD)
                else:
                    c.set(x, y, CYAN_D)
    for x, y in [(7, 2), (8, 2), (2, 7), (2, 8), (13, 7), (13, 8), (7, 13), (8, 13)]:
        c.set(x, y, WHITE)  # 4 diem sang tren vong
    c.add_outline()
    return c


# ---------------------------------------------------------------- tran_3
def icon_tran_3():
    """Sat Tran: ba cot sang trang-vang doc, goc de tren nen rune."""
    c = Canvas()
    for col_x in (4, 8, 12):
        for y in range(2, 14):
            for x in (col_x - 1, col_x, col_x + 1):
                if x == col_x:
                    c.set(x, y, WHITE)
                elif y < 4:
                    c.set(x, y, YEL_L)
                else:
                    c.set(x, y, GOLD)
        c.set(col_x, 1, WHITE)  # dau cot
    for x in range(2, 14):
        c.set(x, 14, GOLD_D)  # vach nen
    for x, y in [(2, 6), (14, 9), (6, 2), (10, 2)]:
        c.set(x, y, WHITE)  # tia sang nho
    c.add_outline()
    return c


# ---------------------------------------------------------------- tran_ult
def icon_tran_ult():
    """Dai Tran: dai phu van lon, vong ngoai vang-lam, sao sau cung, tam sang."""
    c = Canvas()
    cx = cy = 7.5
    up = _tri_edges(cx, cy, 4.8, -90)
    down = _tri_edges(cx, cy, 4.8, 90)
    for y in range(SIZE):
        for x in range(SIZE):
            d = math.hypot(x - cx, y - cy)
            ang = math.degrees(math.atan2(y - cy, x - cx)) % 360
            if 5.6 <= d <= 6.4:
                c.set(x, y, GOLD if int(ang // 30) % 2 == 0 else CYAN)
            elif any(seg_dist(x, y, *p, *q) <= 0.45 for p, q in up + down):
                c.set(x, y, CYAN_L)
            elif d <= 1.6:
                c.set(x, y, WHITE)
            elif d <= 2.6 and (x + y) % 2 == 0:
                c.set(x, y, GOLD_D)
    c.add_outline()
    return c


# ---------------------------------------------------------------- dan_1
def icon_dan_1():
    """Doc Dan Vu: ba vien thuoc doc xanh roi cheo, vet loang duoi dat."""
    c = Canvas()
    _capsule(c, (3, 2), (6, 5), 1.2, POISON_L, POISON)
    _capsule(c, (9, 3), (12, 6), 1.2, POISON_L, POISON)
    _capsule(c, (5, 8), (8, 11), 1.2, POISON_L, POISON)
    for x in range(2, 14, 2):
        c.set(x, 14, POISON_D)  # vet doc loang
    c.set(11, 12, POISON_D)
    c.add_outline()
    return c


# ---------------------------------------------------------------- dan_2
def icon_dan_2():
    """Hoi Xuan Dan: vien hong nhat duong cheo, dau thap trang goc phai duoi."""
    c = Canvas()
    _capsule(c, (2, 12), (12, 2), 2.4, PINK_L, PINK)
    for x, y in [(11, 11), (12, 11), (13, 11), (12, 10), (12, 12), (11, 12), (13, 12), (12, 13), (13, 13)]:
        c.set(x, y, WHITE)
    c.set(12, 11, PINK_D)
    c.add_outline()
    return c


# ---------------------------------------------------------------- dan_3
def icon_dan_3():
    """Dan Hoa Phan Thien: non lua tu goc duoi trai toa len, loi trang-vang-do."""
    c = Canvas()
    origin = (1.0, 14.0)
    poly = [(1, 12), (15, 2), (15, 14), (1, 15)]
    for y in range(SIZE):
        for x in range(SIZE):
            if not in_poly(x, y, poly):
                continue
            d = math.hypot(x - origin[0], y - origin[1])
            if d <= 3.2:
                c.set(x, y, WHITE if d <= 1.6 else FIRE_Y)
            elif d <= 6.5:
                c.set(x, y, ORANGE if (x + y) % 3 else ORANGE_L)
            else:
                c.set(x, y, FLAME_RED if (x * y) % 4 else RED_D)
    for x, y in [(13, 1), (14, 1), (11, 13)]:
        c.set(x, y, ORANGE_L)  # tan lua bay len
    c.add_outline()
    return c


# ---------------------------------------------------------------- dan_ult
def icon_dan_ult():
    """Cuu Chuyen Kim Dan: vien dan vang lon, vanh sang quanh, tia tru trang."""
    c = Canvas()
    cx = cy = 7.5
    for y in range(SIZE):
        for x in range(SIZE):
            d = math.hypot(x - cx, y - cy)
            if 6.0 <= d <= 6.6 and (x + y) % 2 == 0:
                c.set(x, y, YEL_L)  # vanh hao quang rải điểm
            elif d <= 4.9:
                if d >= 4.0:
                    c.set(x, y, GOLD_D)
                elif (x - y) % 3 == 0:
                    c.set(x, y, YEL_L)
                else:
                    c.set(x, y, GOLD)
    for x in range(5, 11):
        c.set(x, 7, WHITE if x in (6, 7, 8) else GOLD_D)  # tia ngang trang
    for y in range(5, 11):
        c.set(7, y, WHITE if y in (6, 7, 8) else GOLD_D)  # tia doc trang
    c.add_outline()
    return c


# ---------------------------------------------------------------- tran_* (bo moi Trận Tu)
PURP = rgba("#a060ff")
PURP_D = rgba("#5b2e9e")
RED_BR = rgba("#ff3b2e")


def _hex_edges(cx, cy, r):
    pts = [(cx + r * math.cos(math.radians(a)), cy + r * math.sin(math.radians(a)))
           for a in range(0, 360, 60)]
    return [(pts[k], pts[(k + 1) % 6]) for k in range(6)]


def icon_tran_khon():
    """Khon Tran: vong vang chia cung khuc, 8 nan lam tu tam ra, hat trang o tam."""
    c = Canvas()
    cx = cy = 7.5
    for y in range(SIZE):
        for x in range(SIZE):
            d = math.hypot(x - cx, y - cy)
            ang = math.degrees(math.atan2(y - cy, x - cx)) % 360
            if 5.4 <= d <= 6.4:
                if int(ang // 30) % 2 == 0:
                    c.set(x, y, GOLD)
            elif 1.6 <= d <= 4.2 and min(ang % 45, 45 - ang % 45) <= 4:
                c.set(x, y, CYAN_L)
            elif d <= 1.0:
                c.set(x, y, WHITE)
    c.add_outline()
    return c


def icon_tran_sat():
    """Sat Tran: kiem phu van cheo, chuoi vang, tia do dau mui."""
    c = Canvas()
    _seg_fill(c, (2, 2), (5, 5), 0.8, GOLD_D)
    _seg_fill(c, (3, 7), (7, 3), 0.7, GOLD)
    _seg_fill(c, (5, 5), (11, 11), 0.7, PALE)
    c.set(12, 12, RED_BR)
    c.set(11, 12, RED_D)
    c.add_outline()
    return c


def _seg_fill(c, a, b, r, col):
    for y in range(SIZE):
        for x in range(SIZE):
            if seg_dist(x, y, a[0], a[1], b[0], b[1]) <= r:
                c.set(x, y, col)


def icon_tran_ho():
    """Ho Tran: vom che hinh vom cung, vong lam ngoai, vong vang trong, tam trang."""
    c = Canvas()
    cx, cy = 7.5, 11.5
    for y in range(SIZE):
        for x in range(SIZE):
            d = math.hypot(x - cx, y - cy)
            if y > 12:
                continue
            if 4.6 <= d <= 5.8:
                c.set(x, y, CYAN_L)
            elif 2.6 <= d <= 3.4:
                c.set(x, y, GOLD)
            elif d <= 1.0:
                c.set(x, y, WHITE)
    _seg_fill(c, (1, 12), (14, 12), 0.5, GOLD_D)
    c.add_outline()
    return c


def icon_tran_stance():
    """Tu the: bon hinh thoi goc, moi goc mot mau: trang, lam, do, vang."""
    c = Canvas()
    quads = [((4, 4), WHITE), ((11, 4), CYAN), ((4, 11), RED_BR), ((11, 11), GOLD)]
    for (qx, qy), col in quads:
        for y in range(SIZE):
            for x in range(SIZE):
                if abs(x - qx) + abs(y - qy) <= 2:
                    c.set(x, y, col)
    c.add_outline()
    return c


def icon_tran_cage():
    """Lo Tran: khung song sat vang, noc tren, day duoi, mot vet nut do."""
    c = Canvas()
    for bx in (3, 6, 9, 12):
        for y in range(3, 14):
            c.set(bx, y, GOLD)
    _seg_fill(c, (2, 2), (13, 2), 0.5, GOLD_D)
    _seg_fill(c, (2, 13), (13, 13), 0.5, GOLD_D)
    _seg_fill(c, (7, 6), (9, 9), 0.5, RED_BR)
    _seg_fill(c, (9, 9), (8, 11), 0.5, RED_BR)
    c.add_outline()
    return c


def icon_tran_slow():
    """Lam Tran: xoay xoan lam, hai nhanh xoan ngoc, tam trang."""
    c = Canvas()
    cx = cy = 7.5
    for y in range(SIZE):
        for x in range(SIZE):
            d = math.hypot(x - cx, y - cy)
            ang = math.degrees(math.atan2(y - cy, x - cx)) % 360
            if 1.2 < d <= 6.2:
                off = (ang - d * 70) % 360
                if off < 80:
                    c.set(x, y, CYAN)
                elif off < 110:
                    c.set(x, y, CYAN_L)
            elif d <= 1.0:
                c.set(x, y, WHITE)
    c.add_outline()
    return c


def icon_tran_suppress():
    """Trấn Ap: con dau tim-vang nang ep xuong, mui ten chi xuong."""
    c = Canvas()
    for y in range(2, 8):
        for x in range(3, 13):
            c.set(x, y, PURP_D if 3 < y < 7 and 4 <= x <= 11 else GOLD)
    for y in range(8, 11):
        for x in (7, 8):
            c.set(x, y, PURP)
    for y in range(10, 14):
        for x in range(2, 14):
            if abs(x - 7.5) <= (13.5 - y) * 1.1:
                c.set(x, y, GOLD)
    c.add_outline()
    return c


def icon_tran_pillar():
    """Tru Sang Tran: cot anh sang trang-vang dung thang, de tron phat sang."""
    c = Canvas()
    for y in range(1, 13):
        for x in range(6, 10):
            c.set(x, y, WHITE if x in (7, 8) else YEL_L)
    for x in range(3, 13):
        c.set(x, 13, GOLD)
    for x, y in [(3, 5), (12, 7), (4, 10), (11, 2)]:
        c.set(x, y, WHITE)
    c.add_outline()
    return c


def icon_tran_blades():
    """Kiem Tran: ba thanh kiem lam roi xuong, chuoi vang, mui trang."""
    c = Canvas()
    for x0 in (3, 7, 11):
        _seg_fill(c, (x0, 2), (x0 + 1, 11), 0.6, CYAN_L)
        c.set(x0 + 1, 12, WHITE)
        _seg_fill(c, (x0 - 2, 4), (x0 + 2, 4), 0.5, GOLD)
    c.add_outline()
    return c


def icon_tran_doom():
    """Dai Diet Tran: loi tim toi, hai vong co lai, vong vang ngoai, tam trang."""
    c = Canvas()
    cx = cy = 7.5
    for y in range(SIZE):
        for x in range(SIZE):
            d = math.hypot(x - cx, y - cy)
            ang = math.degrees(math.atan2(y - cy, x - cx)) % 360
            if 5.6 <= d <= 6.5:
                c.set(x, y, GOLD if int(ang // 30) % 2 == 0 else GOLD_D)
            elif 3.8 <= d <= 4.5:
                c.set(x, y, PURP)
            elif d <= 2.4:
                c.set(x, y, PURP_D)
            elif d <= 0.8:
                c.set(x, y, WHITE)
    c.add_outline()
    return c


def icon_tran_ward():
    """Ho Vuc Tran: luc giac lam ngoai, luc giac nho ben trong, tam sang."""
    c = Canvas()
    cx = cy = 7.5
    outer = _hex_edges(cx, cy, 6.0)
    inner = _hex_edges(cx, cy, 3.4)
    for y in range(SIZE):
        for x in range(SIZE):
            if any(seg_dist(x, y, *p, *q) <= 0.5 for p, q in outer):
                c.set(x, y, CYAN_L)
            elif any(seg_dist(x, y, *p, *q) <= 0.5 for p, q in inner):
                c.set(x, y, CYAN)
            elif math.hypot(x - cx, y - cy) <= 1.0:
                c.set(x, y, WHITE)
    c.add_outline()
    return c


def icon_tran_heal():
    """Hoi Phuc Tran: chu thap xanh la, loi sang vang, vai hat bay quanh."""
    c = Canvas()
    for y in range(SIZE):
        for x in range(SIZE):
            dx, dy = abs(x - 7.5), abs(y - 7.5)
            if (dx <= 1.5 and dy <= 5) or (dy <= 1.5 and dx <= 5):
                c.set(x, y, GREEN_L if dx <= 0.5 or dy <= 0.5 else GREEN)
    for x, y in [(2, 3), (13, 2), (12, 13), (2, 12)]:
        c.set(x, y, GOLD)
    c.add_outline()
    return c


def icon_tran_shield():
    """Ho Than Tran (Tran): khien tron vang, vanh sang, vet sang chu thap."""
    c = Canvas()
    cx = cy = 7.5
    for y in range(SIZE):
        for x in range(SIZE):
            d = math.hypot(x - cx, y - cy)
            if d <= 6.2:
                if d >= 5.0:
                    c.set(x, y, YEL_L)
                elif abs(x - cx) < 0.7 or abs(y - cy) < 0.7:
                    c.set(x, y, PALE)
                else:
                    c.set(x, y, GOLD)
    c.add_outline()
    return c


ICONS = {
    "dao_1.png": icon_dao_1,
    "dao_2.png": icon_dao_2,
    "dao_ult.png": icon_dao_ult,
    "the_1.png": icon_the_1,
    "the_2.png": icon_the_2,
    "the_ult.png": icon_the_ult,
    "tran_1.png": icon_tran_1,
    "tran_2.png": icon_tran_2,
    "tran_3.png": icon_tran_3,
    "tran_ult.png": icon_tran_ult,
    "tran_khon.png": icon_tran_khon,
    "tran_sat.png": icon_tran_sat,
    "tran_ho.png": icon_tran_ho,
    "tran_stance.png": icon_tran_stance,
    "tran_cage.png": icon_tran_cage,
    "tran_slow.png": icon_tran_slow,
    "tran_suppress.png": icon_tran_suppress,
    "tran_pillar.png": icon_tran_pillar,
    "tran_blades.png": icon_tran_blades,
    "tran_doom.png": icon_tran_doom,
    "tran_ward.png": icon_tran_ward,
    "tran_heal.png": icon_tran_heal,
    "tran_shield.png": icon_tran_shield,
    "dan_1.png": icon_dan_1,
    "dan_2.png": icon_dan_2,
    "dan_3.png": icon_dan_3,
    "dan_ult.png": icon_dan_ult,
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
    import gen_book_skill_icons
    gen_book_skill_icons.main()


if __name__ == "__main__":
    main()
