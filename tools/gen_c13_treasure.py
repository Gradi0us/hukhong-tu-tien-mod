"""Bảo Rương (2026-11-06): rương báu 4 cấp thay rương thường ở Bí Cảnh / địa điểm / Kỳ Ngộ.
Model GeckoLib (geo + texture + glowmask) + animation idle/open/opened. Thiết kế gốc."""
import json, sys
sys.path.insert(0, str(__import__("pathlib").Path(__file__).parent))
import _gm2_engine as E
from _gm2_engine import Model, Mat, Anim, A, mix, mul

_px = E.px
def px(m, face, x, y, w, h, rng):
    if m.kind == "wood":  # ván gỗ: thớ dọc + rãnh ngang mỗi 4 px
        c = mul(m.base, E.SHADE[face] * (1 + (rng.random() - .5) * .14))
        if (x + (y // 4) * 3) % 5 == 0: c = mul(c, .82)
        if y % 4 == 3: c = mul(c, .7)
        if x == 0 or x == w - 1: c = mul(c, .8)
        return c + (255,), None
    return _px(m, face, x, y, w, h, rng)
E.px = px

def wood(b): return Mat("wood", b, None, None, .05)

TIERS = [  # (thân, viền kim loại, ngọc phát sáng)
    ((112, 72, 40), (186, 128, 66), (255, 196, 90)),     # 0 Gỗ Đồng
    ((36, 78, 74), (206, 214, 224), (110, 255, 190)),    # 1 Bích Ngọc
    ((128, 26, 30), (244, 204, 84), (255, 226, 130)),    # 2 Hoàng Kim
    ((34, 22, 50), (160, 120, 214), (196, 96, 255)),     # 3 Hư Không
]

def build(t):
    body, band, gem = TIERS[t]
    W, P, G = wood(body), Mat("plate", band, mul(band, 1.25), None), E.crystal(mix(gem, (255, 255, 255), .2), gem)
    m = Model(f"bao_ruong_{t}", 128, bounds=(3, 3), off=0.6)
    root = m.bone("root", pivot=(0, 0, 0))
    lid = m.bone("lid", root, pivot=(0, 9, 6))
    aura = m.bone("aura", root, pivot=(0, 0, 0))
    gemb = m.bone("gem", root, pivot=(0, 6, -7))
    # thân
    m.cube(root, (-7, 0, -6), (14, 9, 12), W)
    m.cube(root, (-7.5, 0, -6.5), (15, 1.5, 13), P)                 # đế viền
    for x in (-4.5, 3.5): m.cube(root, (x, 0, -6.5), (1, 9, 13), P)  # đai dọc
    for x in (-8, 6):
        for z in (-7, 5): m.cube(root, (x, 0, z), (2, 9.5, 2), P)   # trụ góc
    m.cube(root, (-2, 3.5, -7.2), (4, 5, 1), P)                     # tấm khóa
    m.cube(gemb, (-1, 5, -7.8), (2, 2, 1), G)                       # ngọc khóa
    for x in (-6, 4):  # chân
        for z in (-5, 3): m.cube(root, (x, -1, z), (2, 1, 2), P)
    # nắp
    m.cube(lid, (-7, 9, -6), (14, 4, 12), W)
    m.cube(lid, (-6, 13, -5), (12, 1.5, 10), W)
    m.cube(lid, (-7.5, 9, -6.5), (15, 1, 13), P)
    m.cube(lid, (-7.5, 12.5, -6.5), (15, 1, 13), P)
    for x in (-4.5, 3.5): m.cube(lid, (x, 9, -6.5), (1, 5.5, 13), P)
    m.cube(lid, (-1, 7.5, -7.3), (2, 3, 1), P)                      # móc khóa
    m.cube(lid, (-1.5, 14.5, -1.5), (3, 1, 3), G)                   # ngọc đỉnh nắp
    if t >= 1:
        for x in (-6.5, 5.5):
            for z in (-6, 4.5): m.cube(lid, (x, 13.5, z), (1, 1.5, 1.5), P)  # đinh tán góc nắp
    if t >= 2:  # vương miện nhỏ trên nắp
        for i, x in enumerate((-3, -1, 1)):
            m.cube(lid, (x + .5, 15.5, -.5), (1, 2 + (i == 1), 1), P)
        m.cube(lid, (-2.5, 15.5, -1), (5, 1, 2), P)
    if t >= 2:  # vòng ngọc lơ lửng quanh rương
        m.ring(aura, (0, 2, 0), 11 if t == 2 else 12, G, P, bead=2, thick=1, every=3)
    if t == 3:  # mảnh tinh thể Hư Không lơ lửng
        for x, z, h in ((-10, -8, 6), (9, 7, 8), (10, -6, 5), (-9, 8, 7)):
            m.cube(aura, (x, 4, z), (2, h, 2), G, rot=[0, 0, 18])
    m.build()

for t in range(4): build(t)

# animation dùng chung (bone giống nhau)
idle = Anim("bao_ruong", "idle", 4, True)
idle.kf("aura", "rotation", [(0, [0, 0, 0]), (4, [0, 360, 0])])
idle.kf("aura", "position", [(0, [0, 0, 0]), (2, [0, 1.2, 0]), (4, [0, 0, 0])])
idle.kf("gem", "scale", [(0, [1, 1, 1]), (1, [1.15, 1.15, 1.15]), (2, [1, 1, 1]), (4, [1, 1, 1])])
idle.kf("lid", "rotation", [(0, [0, 0, 0]), (2.6, [0, 0, 0]), (2.75, [-4, 0, 0]), (2.9, [0, 0, 0]), (3.05, [-2.5, 0, 0]), (3.2, [0, 0, 0]), (4, [0, 0, 0])])
opn = Anim("bao_ruong", "open", 0.7, False)
opn.kf("lid", "rotation", [(0, [0, 0, 0]), (0.12, [8, 0, 0]), (0.45, [-118, 0, 0]), (0.7, [-104, 0, 0])])
opn.kf("aura", "rotation", [(0, [0, 0, 0]), (0.7, [0, 240, 0])])
opn.kf("aura", "position", [(0, [0, 0, 0]), (0.45, [0, 4, 0]), (0.7, [0, 3, 0])])
opn.kf("gem", "scale", [(0, [1, 1, 1]), (0.2, [1.6, 1.6, 1.6]), (0.7, [1.2, 1.2, 1.2])])
held = Anim("bao_ruong", "opened", 4, True)
held.kf("lid", "rotation", [(0, [-104, 0, 0]), (4, [-104, 0, 0])])
held.kf("aura", "rotation", [(0, [0, 240, 0]), (4, [0, 600, 0])])
held.kf("aura", "position", [(0, [0, 3, 0]), (2, [0, 3.8, 0]), (4, [0, 3, 0])])
held.kf("gem", "scale", [(0, [1.2, 1.2, 1.2]), (4, [1.2, 1.2, 1.2])])
anims = dict(a.json() for a in (idle, opn, held))
(A / "animations/entity/bao_ruong.animation.json").write_text(json.dumps({"format_version": "1.8.0", "animations": anims}, indent=1))
print("anim ok")
