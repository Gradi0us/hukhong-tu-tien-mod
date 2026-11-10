"""Animation BossBrain cho Dạ Khuyết (PLAN-v1 §2): đi vòng, né, áp sát, bay lượn, lướt máu, 9 chuỗi đòn thường.
Ghi thêm vào da_khuyet.animation.json (giữ nguyên animation cũ). Độ dài combo_* khớp windup+active+recovery trong DaKhuyetBrain.
Xương: body, head, left_arm (katana), right_arm (lưỡi trăng), right_leg, left_leg, cloak, wing, halo.
Chạy: python tools/gen_da_khuyet_brain_anims.py
"""
import json
import math
import os

PATH = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "hukhong",
                    "animations", "entity", "da_khuyet.animation.json")
T = 1 / 20.0  # 1 tick


def r(v):
    return [round(x, 3) for x in v]


def track(*keys):
    """keys: (tick, [x, y, z]) -> {"s": [x,y,z]}"""
    return {str(round(t * T, 4)): r(v) for t, v in keys}


def anim(length_ticks, bones, loop=False):
    return {"loop": loop, "animation_length": round(length_ticks * T, 4), "bones": bones}


def cloak_flutter(n, amp=12, base=8):
    return {"rotation": track(*[(t, [base + amp * math.sin(t * 0.7), 0, 0]) for t in range(0, n + 1, 2)])}


# ---------------------------------------------------------------- di chuyển
def strafe(side):  # side = +1 phải, -1 trái; đi ngang, kiếm chĩa xuống, thân nghiêng về hướng đi
    n = 16
    s = side
    legs = lambda ph: track(*[(t, [0, 0, s * (10 + 14 * math.sin(t / n * 2 * math.pi + ph))]) for t in range(0, n + 1, 2)])
    return anim(n, {
        "body": {"rotation": track((0, [4, -s * 15, s * 4]), (n, [4, -s * 15, s * 4])),
                 "position": track(*[(t, [0, abs(math.sin(t / n * 2 * math.pi)) * 1.2, 0]) for t in range(0, n + 1, 2)])},
        "left_arm": {"rotation": track((0, [20, 0, -12]), (n // 2, [16, 0, -14]), (n, [20, 0, -12]))},  # kiếm chĩa xuống
        "right_arm": {"rotation": track((0, [-10, 0, 18]), (n, [-10, 0, 18]))},
        "right_leg": {"rotation": legs(0)},
        "left_leg": {"rotation": legs(math.pi)},
        "head": {"rotation": track((0, [0, s * 15, 0]), (n, [0, s * 15, 0]))},  # mắt dõi theo mục tiêu
        "cloak": cloak_flutter(n, 8, 10 + 6),
    }, loop=True)


def backstep():
    return anim(8, {
        "body": {"rotation": track((0, [0, 0, 0]), (3, [-14, 0, 0]), (8, [0, 0, 0])),
                 "position": track((0, [0, 0, 0]), (3, [0, 1.5, 0]), (8, [0, 0, 0]))},
        "left_arm": {"rotation": track((0, [0, 0, 0]), (3, [-30, 0, -20]), (8, [0, 0, 0]))},
        "right_arm": {"rotation": track((0, [0, 0, 0]), (3, [-20, 0, 30]), (8, [0, 0, 0]))},
        "right_leg": {"rotation": track((0, [0, 0, 0]), (3, [30, 0, 0]), (8, [0, 0, 0]))},
        "left_leg": {"rotation": track((0, [0, 0, 0]), (3, [-20, 0, 0]), (8, [0, 0, 0]))},
        "cloak": {"rotation": track((0, [8, 0, 0]), (4, [-25, 0, 0]), (8, [8, 0, 0]))},
    })


def sidestep(side):
    s = side
    return anim(8, {
        "body": {"rotation": track((0, [0, 0, 0]), (2, [0, -s * 20, s * 22]), (6, [0, -s * 10, s * 8]), (8, [0, 0, 0])),
                 "position": track((0, [0, 0, 0]), (2, [s * 1.5, -2, 0]), (8, [0, 0, 0]))},
        "left_arm": {"rotation": track((0, [0, 0, 0]), (2, [-20, 0, -s * 40]), (8, [0, 0, 0]))},
        "right_arm": {"rotation": track((0, [0, 0, 0]), (2, [-20, 0, -s * 40]), (8, [0, 0, 0]))},
        "right_leg": {"rotation": track((0, [0, 0, 0]), (2, [0, 0, s * 28]), (8, [0, 0, 0]))},
        "left_leg": {"rotation": track((0, [0, 0, 0]), (2, [0, 0, s * 28]), (8, [0, 0, 0]))},
        "cloak": {"rotation": track((0, [8, 0, 0]), (3, [-10, 0, s * 25]), (8, [8, 0, 0]))},
    })


def backflip():  # 10 tick nhào lộn lùi, kiếm dang
    return anim(12, {
        "body": {"rotation": track((0, [0, 0, 0]), (2, [-20, 0, 0]), (5, [-180, 0, 0]), (8, [-340, 0, 0]), (10, [-360, 0, 0]), (12, [-360, 0, 0])),
                 "position": track((0, [0, 0, 0]), (2, [0, -3, 0]), (5, [0, 10, 0]), (9, [0, 4, 0]), (10, [0, -2, 0]), (12, [0, 0, 0]))},
        "left_arm": {"rotation": track((0, [0, 0, 0]), (3, [-150, 0, -30]), (8, [-150, 0, -30]), (11, [10, 0, -10]), (12, [0, 0, 0]))},
        "right_arm": {"rotation": track((0, [0, 0, 0]), (3, [-150, 0, 30]), (8, [-150, 0, 30]), (11, [10, 0, 10]), (12, [0, 0, 0]))},
        "right_leg": {"rotation": track((0, [0, 0, 0]), (4, [-80, 0, 0]), (8, [-80, 0, 0]), (11, [20, 0, 0]), (12, [0, 0, 0]))},
        "left_leg": {"rotation": track((0, [0, 0, 0]), (4, [-80, 0, 0]), (8, [-80, 0, 0]), (11, [-10, 0, 0]), (12, [0, 0, 0]))},
        "cloak": {"rotation": track((0, [8, 0, 0]), (5, [60, 0, 0]), (10, [-20, 0, 0]), (12, [8, 0, 0]))},
    })


def dash():  # lướt thấp, kiếm kéo sau lưng, kết thúc vươn tay đâm
    n = 16
    return anim(n, {
        "body": {"rotation": track((0, [0, 0, 0]), (3, [35, 0, 0]), (n - 3, [35, 0, 0]), (n, [10, 0, 0])),
                 "position": track((0, [0, 0, 0]), (3, [0, -5, 0]), (n - 3, [0, -5, 0]), (n, [0, 0, 0]))},
        "left_arm": {"rotation": track((0, [0, 0, 0]), (3, [50, 0, -25]), (n - 3, [55, 0, -25]), (n, [-90, 0, 0]))},
        "right_arm": {"rotation": track((0, [0, 0, 0]), (3, [45, 0, 20]), (n, [20, 0, 15]))},
        "right_leg": {"rotation": track((0, [0, 0, 0]), (3, [-50, 0, 0]), (n, [-10, 0, 0]))},
        "left_leg": {"rotation": track((0, [0, 0, 0]), (3, [40, 0, 0]), (n, [10, 0, 0]))},
        "cloak": {"rotation": track((0, [8, 0, 0]), (3, [70, 0, 0]), (n - 3, [75, 0, 0]), (n, [20, 0, 0]))},
        "wing": {"rotation": track((0, [0, 0, 0]), (3, [-25, 0, 0]), (n, [0, 0, 0]))},
    })


def hover():  # bốc lên 8, lượn 16 (vỗ cánh), lao xuống 8
    n = 34
    wing = track(*[(t, [-25 + 25 * math.sin(t * 0.8), 0, 0]) for t in range(0, n + 1, 2)])
    return anim(n, {
        "body": {"rotation": track((0, [0, 0, 0]), (6, [-10, 0, 0]), (24, [-6, 0, 0]), (27, [50, 0, 0]), (n, [10, 0, 0])),
                 "position": track(*[(t, [0, 1.5 * math.sin(t * 0.4) if 8 <= t <= 24 else 0, 0]) for t in range(0, n + 1, 2)])},
        "left_arm": {"rotation": track((0, [0, 0, 0]), (6, [-40, 0, -50]), (24, [-40, 0, -50]), (27, [-170, 0, 0]), (31, [-60, 0, 0]), (n, [0, 0, 0]))},
        "right_arm": {"rotation": track((0, [0, 0, 0]), (6, [-40, 0, 50]), (24, [-40, 0, 50]), (27, [20, 0, 30]), (n, [0, 0, 0]))},
        "right_leg": {"rotation": track((0, [0, 0, 0]), (6, [20, 0, 5]), (24, [25, 0, 5]), (n, [0, 0, 0]))},
        "left_leg": {"rotation": track((0, [0, 0, 0]), (6, [35, 0, -5]), (24, [30, 0, -5]), (n, [0, 0, 0]))},
        "wing": {"rotation": wing},
        "cloak": {"rotation": track((0, [8, 0, 0]), (8, [30, 0, 0]), (24, [25, 0, 0]), (27, [80, 0, 0]), (n, [8, 0, 0]))},
        "halo": {"rotation": track((0, [0, 0, 0]), (n, [0, 360, 0]))},
    })


def blink():  # co người lại (tan thành máu) 6 tick rồi bung ra ở chỗ mới
    return anim(12, {
        "body": {"rotation": track((0, [0, 0, 0]), (4, [30, 0, 0]), (6, [30, 0, 0]), (8, [-10, 0, 0]), (12, [0, 0, 0])),
                 "position": track((0, [0, 0, 0]), (4, [0, -6, 0]), (6, [0, -6, 0]), (8, [0, 1, 0]), (12, [0, 0, 0])),
                 "scale": track((0, [1, 1, 1]), (5, [0.4, 0.2, 0.4]), (6, [0.4, 0.2, 0.4]), (8, [1.1, 1.1, 1.1]), (12, [1, 1, 1]))},
        "left_arm": {"rotation": track((0, [0, 0, 0]), (4, [-30, 0, 60]), (7, [-100, 0, -20]), (12, [0, 0, 0]))},
        "right_arm": {"rotation": track((0, [0, 0, 0]), (4, [-30, 0, -60]), (12, [0, 0, 0]))},
        "cloak": {"rotation": track((0, [8, 0, 0]), (4, [-30, 0, 0]), (8, [60, 0, 0]), (12, [8, 0, 0]))},
    })


# ---------------------------------------------------------------- đòn
def slash(windup, active, recovery, start, end, body_twist, delay=0, heavy=False):
    """Chém katana (tay trái): start/end = góc [x,y,z] đầu/cuối vung; body_twist = xoay thân y."""
    w = windup + delay
    n = w + active + recovery
    hold = [] if delay == 0 else [(windup, start), (w - 1, [start[0] - 8, start[1], start[2]])]  # giữ thế (nhát trễ)
    return anim(n, {
        "left_arm": {"rotation": track((0, [0, 0, 0]), (max(1, windup - 2), start), *hold, (w + active, end), (n, [0, 0, 0]))},
        "right_arm": {"rotation": track((0, [0, 0, 0]), (w, [-20, 0, 25]), (w + active, [10, 0, 35]), (n, [0, 0, 0]))},
        "body": {"rotation": track((0, [0, 0, 0]), (w, [0, -body_twist, 0]), (w + active, [12 if heavy else 6, body_twist, 0]), (n, [0, 0, 0])),
                 "position": track((0, [0, 0, 0]), (w, [0, -1.5 if heavy else -0.5, 0]), (w + active, [0, -2.5 if heavy else -1, 0]), (n, [0, 0, 0]))},
        "right_leg": {"rotation": track((0, [0, 0, 0]), (w + active, [-25, 0, 0]), (n, [0, 0, 0]))},
        "left_leg": {"rotation": track((0, [0, 0, 0]), (w + active, [20, 0, 0]), (n, [0, 0, 0]))},
        "cloak": {"rotation": track((0, [8, 0, 0]), (w, [0, 0, 0]), (w + active, [35, body_twist * 0.3, 0]), (n, [8, 0, 0]))},
    })


def spin_slash(windup, active, recovery, delay=0):  # xoay người 360 chém
    w = windup + delay
    n = w + active + recovery
    return anim(n, {
        "body": {"rotation": track((0, [0, 0, 0]), (windup, [5, 50, 0]), (w, [8, 60, 0]), (w + active, [8, -300, 0]), (w + active + 2, [6, -360, 0]), (n, [0, -360, 0])),
                 "position": track((0, [0, 0, 0]), (w, [0, -2, 0]), (w + active, [0, -3, 0]), (n, [0, 0, 0]))},
        "left_arm": {"rotation": track((0, [0, 0, 0]), (windup, [-60, 0, -80]), (w, [-70, 0, -85]), (w + active, [-80, 0, -95]), (n, [0, 0, 0]))},
        "right_arm": {"rotation": track((0, [0, 0, 0]), (w, [-40, 0, 70]), (w + active, [-40, 0, 80]), (n, [0, 0, 0]))},
        "right_leg": {"rotation": track((0, [0, 0, 0]), (w, [-20, 0, 10]), (n, [0, 0, 0]))},
        "left_leg": {"rotation": track((0, [0, 0, 0]), (w, [25, 0, -10]), (n, [0, 0, 0]))},
        "cloak": {"rotation": track((0, [8, 0, 0]), (w + active, [70, 0, 30]), (n, [8, 0, 0]))},
    })


def stab(windup, active, recovery, pull_back=False):
    n = windup + active + recovery
    end_body = [-8, 0, 0] if pull_back else [0, 0, 0]
    return anim(n, {
        "left_arm": {"rotation": track((0, [0, 0, 0]), (windup, [30, 15, -15]), (windup + 1, [-95, 0, 0]), (windup + active, [-92, 0, 0]),
                                       (n, [20, 0, -10] if pull_back else [0, 0, 0]))},
        "right_arm": {"rotation": track((0, [0, 0, 0]), (windup, [-30, 0, 30]), (windup + active, [30, 0, 20]), (n, [0, 0, 0]))},
        "body": {"rotation": track((0, [0, 0, 0]), (windup, [-5, 30, 0]), (windup + 1, [18, -15, 0]), (windup + active, [18, -15, 0]), (n, end_body))},
        "right_leg": {"rotation": track((0, [0, 0, 0]), (windup + 1, [-35, 0, 0]), (n, [0, 0, 0]))},
        "left_leg": {"rotation": track((0, [0, 0, 0]), (windup + 1, [30, 0, 0]), (n, [0, 0, 0]))},
        "cloak": {"rotation": track((0, [8, 0, 0]), (windup + active, [45, 0, 0]), (n, [8, 0, 0]))},
    })


def flurry(windup, active, recovery, k):
    side = 1 if k % 2 else -1
    n = windup + active + recovery
    return anim(n, {
        "left_arm": {"rotation": track((0, [-60, 0, side * 30]), (windup, [-80, 0, side * 60]), (windup + active, [-70, 0, -side * 50]), (n, [-60, 0, -side * 30]))},
        "right_arm": {"rotation": track((0, [-30, 0, 30]), (n, [-30, 0, 30]))},
        "body": {"rotation": track((0, [6, side * 10, 0]), (windup + active, [8, -side * 20, 0]), (n, [6, -side * 10, 0]))},
        "cloak": {"rotation": track((0, [20, 0, 0]), (n, [30, 0, 0]))},
    })


def leap_slam(windup, active, recovery):
    n = windup + active + recovery
    return anim(n, {
        "body": {"rotation": track((0, [0, 0, 0]), (3, [-20, 0, 0]), (windup, [-25, 0, 0]), (windup + active, [40, 0, 0]), (n, [0, 0, 0])),
                 "position": track((0, [0, 0, 0]), (2, [0, -3, 0]), (6, [0, 4, 0]), (windup + active, [0, -4, 0]), (n, [0, 0, 0]))},
        "left_arm": {"rotation": track((0, [0, 0, 0]), (windup, [-190, 0, -10]), (windup + active, [-30, 0, 0]), (n, [0, 0, 0]))},
        "right_arm": {"rotation": track((0, [0, 0, 0]), (windup, [-190, 0, 10]), (windup + active, [-30, 0, 0]), (n, [0, 0, 0]))},
        "right_leg": {"rotation": track((0, [0, 0, 0]), (4, [-70, 0, 0]), (windup, [-40, 0, 0]), (windup + active, [-30, 0, 0]), (n, [0, 0, 0]))},
        "left_leg": {"rotation": track((0, [0, 0, 0]), (4, [-70, 0, 0]), (windup, [-40, 0, 0]), (windup + active, [40, 0, 0]), (n, [0, 0, 0]))},
        "cloak": {"rotation": track((0, [8, 0, 0]), (windup, [-40, 0, 0]), (windup + active, [80, 0, 0]), (n, [8, 0, 0]))},
        "wing": {"rotation": track((0, [0, 0, 0]), (windup, [-40, 0, 0]), (n, [0, 0, 0]))},
    })


def parry(windup, active, recovery):  # gạt kiếm chéo trước ngực, ánh trăng
    n = windup + active + recovery
    return anim(n, {
        "left_arm": {"rotation": track((0, [0, 0, 0]), (windup, [-80, -30, 40]), (windup + active, [-100, 30, -40]), (n, [-40, 0, -20]))},
        "right_arm": {"rotation": track((0, [0, 0, 0]), (windup, [-60, 0, -20]), (n, [-20, 0, 20]))},
        "body": {"rotation": track((0, [0, 0, 0]), (windup, [-10, 25, 0]), (windup + active, [5, -20, 0]), (n, [0, -10, 0]))},
        "halo": {"rotation": track((0, [0, 0, 0]), (n, [0, 180, 0]))},
    })


def build():
    a = {}
    p = "animation.da_khuyet."
    a[p + "strafe_l"] = strafe(-1)
    a[p + "strafe_r"] = strafe(1)
    a[p + "backstep"] = backstep()
    a[p + "sidestep_l"] = sidestep(-1)
    a[p + "sidestep_r"] = sidestep(1)
    a[p + "backflip"] = backflip()
    a[p + "dash"] = dash()
    a[p + "hover"] = hover()
    a[p + "blink"] = blink()
    # Tam Nguyệt Trảm: ngang, chéo, xoay người trễ (delayExtra 10)
    a[p + "combo_tam_nguyet_1"] = slash(8, 3, 5, [-80, 0, 70], [-80, 0, -80], 35)
    a[p + "combo_tam_nguyet_2"] = slash(6, 3, 6, [-150, 0, -40], [-20, 0, 40], -30)
    a[p + "combo_tam_nguyet_3"] = spin_slash(8, 4, 12, delay=10)
    # Đâm – Rút – Đâm
    a[p + "combo_dam_rut_dam_1"] = stab(7, 3, 6, pull_back=True)
    a[p + "combo_dam_rut_dam_2"] = stab(3, 3, 10)
    # Kiếm Vũ Liên Hoàn (6 nhát)
    fl = [(5, 2, 1), (2, 2, 1), (2, 2, 1), (2, 2, 1), (2, 2, 1), (3, 3, 16)]
    for i, (w, ac, rc) in enumerate(fl, 1):
        a[p + "combo_kiem_vu_%d" % i] = flurry(w, ac, rc, i)
    # Nhảy Chém: bổ xuống -> quét
    a[p + "combo_nhay_chem_1"] = leap_slam(12, 4, 6)
    a[p + "combo_nhay_chem_2"] = spin_slash(5, 4, 14)
    a[p + "combo_dam_xuyen_1"] = stab(6, 4, 10)
    a[p + "combo_quet_1"] = spin_slash(6, 4, 14)
    a[p + "combo_phan_nguyet_1"] = parry(3, 3, 3)
    a[p + "combo_phan_nguyet_2"] = slash(4, 4, 12, [-120, 0, 90], [-60, 0, -100], 50, heavy=True)
    a[p + "combo_dam_dash_1"] = stab(3, 3, 10)
    return a


def main():
    with open(PATH, encoding="utf-8") as f:
        data = json.load(f)
    data["animations"].update(build())
    with open(PATH, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=1)
    print("da_khuyet animations:", len(data["animations"]))


if __name__ == "__main__":
    main()
