"""Animation BossBrain cho 4 Chấp Hành Giả (PLAN-v1 §5, bước B4).

Thêm vào chg_N.animation.json: strafe_l/r, backstep, sidestep_l/r, dash, leap, (backflip cho CHG II) và combo_<id>_<n>
theo bảng combo trong ChgBrain.java (thời lượng = windup [+delayExtra] + active + recovery). Chạy lại an toàn (ghi đè).
Xương dùng chung: body, head, right_arm, left_arm, right_leg, left_leg (vũ khí là xương con của tay).
"""
import json
import os

ROOT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'hukhong', 'animations', 'entity')
DELAY = {1: 12, 2: 8, 3: 14, 4: 10}
# (kiểu, windup, active, recovery, delayed) — khớp ChgBrain.java
COMBOS = {
    1: {'riu': [('sweep_r', 14, 4, 8, 0), ('sweep_l', 10, 4, 8, 0), ('overhead', 12, 4, 18, 1)],
        'quet': [('sweep_wide', 16, 5, 8, 0), ('shoulder', 10, 3, 16, 0)],
        'vo': [('grab', 16, 5, 6, 0), ('punch', 8, 3, 16, 0)],
        'dam': [('stomp', 10, 3, 16, 0)]},
    2: {'cao': [('claw_r', 6, 2, 3, 0), ('claw_l', 4, 2, 8, 0), ('claw_r', 9, 2, 2, 0), ('claw_both', 5, 3, 14, 1)],
        'chom': [('lunge', 10, 4, 6, 0), ('claw_both', 6, 3, 14, 0)],
        'hac': [('sweep_r', 12, 4, 6, 0), ('sweep_l', 8, 4, 8, 0), ('spin', 14, 6, 18, 1)],
        'dam': [('thrust', 10, 4, 14, 0)]},
    3: {'gay': [('cane_r', 12, 4, 6, 1), ('cane_l', 8, 4, 6, 0), ('cane_over', 14, 5, 18, 1)],
        'dam': [('cane_thrust', 10, 4, 8, 0), ('cane_up', 12, 5, 16, 0)],
        'quet': [('cane_low', 9, 4, 6, 0), ('cane_over', 12, 4, 16, 0)]},
    4: {'dinh_ba': [('thrust', 12, 4, 6, 0), ('sweep_wide', 10, 5, 6, 0), ('spin_thrust', 14, 5, 18, 1)],
        'dam_xa': [('thrust', 12, 4, 8, 0), ('thrust', 10, 4, 16, 0)],
        'xoay': [('spin', 8, 6, 16, 0)]},
}

# kiểu đòn -> (tư thế lấy đà, tư thế vung); tư thế = {xương: [rx, ry, rz]}; 'W' = tay cầm vũ khí, 'O' = tay kia
POSES = {
    'sweep_r': ({'W': [-70, 0, 75], 'body': [0, 35, 0], 'O': [-20, 0, -10]}, {'W': [-80, 0, -55], 'body': [5, -40, 0], 'O': [20, 0, 0]}),
    'sweep_l': ({'W': [-75, 0, -60], 'body': [0, -35, 0]}, {'W': [-70, 0, 70], 'body': [5, 40, 0]}),
    'sweep_wide': ({'W': [-60, 0, 90], 'body': [0, 50, 0], 'O': [-30, 0, -30]}, {'W': [-70, 0, -80], 'body': [8, -55, 0], 'O': [10, 0, 20]}),
    'overhead': ({'W': [-175, 0, 0], 'O': [-160, 0, 0], 'body': [-14, 0, 0], 'head': [-10, 0, 0]},
                 {'W': [-25, 0, 0], 'O': [-30, 0, 0], 'body': [25, 0, 0], 'head': [10, 0, 0]}),
    'shoulder': ({'body': [0, -40, 0], 'W': [20, 0, 0]}, {'body': [20, 30, 0], 'O': [-60, 0, -40]}),
    'grab': ({'W': [-40, 0, 40], 'O': [-40, 0, -40], 'body': [-10, 0, 0]}, {'W': [-95, 0, -10], 'O': [-95, 0, 10], 'body': [25, 0, 0]}),
    'punch': ({'O': [30, 0, -20], 'body': [0, 30, 0]}, {'O': [-95, 0, 0], 'body': [10, -25, 0]}),
    'stomp': ({'right_leg': [-70, 0, 0], 'body': [-10, 0, 0], 'W': [-40, 0, 20], 'O': [-40, 0, -20]},
              {'right_leg': [5, 0, 0], 'body': [18, 0, 0], 'W': [10, 0, 30], 'O': [10, 0, -30]}),
    'claw_r': ({'W': [-130, 0, 30], 'body': [0, 25, 0]}, {'W': [-30, 0, -30], 'body': [15, -20, 0]}),
    'claw_l': ({'O': [-130, 0, -30], 'body': [0, -25, 0]}, {'O': [-30, 0, 30], 'body': [15, 20, 0]}),
    'claw_both': ({'W': [-150, 0, 20], 'O': [-150, 0, -20], 'body': [-15, 0, 0]}, {'W': [-20, 0, -20], 'O': [-20, 0, 20], 'body': [30, 0, 0]}),
    'lunge': ({'body': [-20, 0, 0], 'right_leg': [30, 0, 0], 'left_leg': [30, 0, 0], 'W': [40, 0, 0], 'O': [40, 0, 0]},
              {'body': [40, 0, 0], 'W': [-110, 0, 0], 'O': [-110, 0, 0], 'right_leg': [-30, 0, 0], 'left_leg': [40, 0, 0]}),
    'thrust': ({'W': [-50, 0, 10], 'body': [0, 30, 0], 'right_leg': [20, 0, 0]}, {'W': [-92, 0, 0], 'body': [12, -15, 0], 'left_leg': [-30, 0, 0]}),
    'spin_thrust': ({'W': [-60, 0, 60], 'body': [0, 70, 0]}, {'W': [-92, 0, 0], 'body': [15, -20, 0]}),
    'spin': ({'W': [-80, 0, 70], 'body': [0, 60, 0]}, {'W': [-85, 0, -60], 'body': [0, -300, 0]}),
    'cane_r': ({'W': [-80, 0, -60], 'body': [0, -30, 0]}, {'W': [-70, 0, 60], 'body': [5, 35, 0]}),
    'cane_l': ({'W': [-70, 0, 60], 'body': [0, 30, 0]}, {'W': [-80, 0, -60], 'body': [5, -35, 0]}),
    'cane_over': ({'W': [-170, 0, 0], 'O': [-150, 0, 0], 'body': [-12, 0, 0]}, {'W': [-20, 0, 0], 'O': [-25, 0, 0], 'body': [22, 0, 0]}),
    'cane_thrust': ({'W': [-45, 0, 0], 'body': [0, -25, 0]}, {'W': [-92, 0, 0], 'body': [10, 10, 0]}),
    'cane_up': ({'W': [10, 0, -20], 'body': [10, 0, 0]}, {'W': [-160, 0, 0], 'body': [-15, 0, 0]}),
    'cane_low': ({'W': [-40, 0, -70], 'body': [15, -30, 0], 'right_leg': [-20, 0, 0]}, {'W': [-40, 0, 70], 'body': [15, 35, 0]}),
}


def bone_of(key, idx):
    weapon = 'left_arm' if idx == 3 else 'right_arm'   # CHG III: gậy ở tay trái
    other = 'right_arm' if idx == 3 else 'left_arm'
    return weapon if key == 'W' else other if key == 'O' else key


def r(x):
    return round(x, 3)


def combo_anim(idx, kind, w, a, rec, delayed):
    wind = w + (DELAY[idx] if delayed else 0)
    t_set = r(min(wind, 8) / 20)            # vào tư thế lấy đà
    t_wind = r(wind / 20)
    t_act = r((wind + a) / 20)
    t_end = r((wind + a + rec) / 20)
    pa, pb = POSES[kind]
    bones = {}
    for key in set(pa) | set(pb):
        b = bone_of(key, idx)
        A = pa.get(key, [0, 0, 0])
        B = pb.get(key, [0, 0, 0])
        kf = {'0': [0, 0, 0], str(t_set): A}
        if delayed and t_wind - t_set > 0.15:   # nhát trễ: giữ tư thế, hơi run
            mid = r((t_set + t_wind) / 2)
            kf[str(mid)] = [r(A[0] - 6), A[1], r(A[2] + 3)]
        kf[str(t_wind)] = A
        kf[str(t_act)] = B
        kf[str(r(t_act + (t_end - t_act) * 0.5))] = [r(v * 0.6) for v in B]
        kf[str(t_end)] = [0, 0, 0]
        bones[b] = {'rotation': dict(sorted(kf.items(), key=lambda e: float(e[0])))}
    if kind in ('lunge', 'grab', 'thrust', 'cane_thrust'):
        bones.setdefault('body', {})['position'] = {'0': [0, 0, 0], str(t_wind): [0, -1, 2], str(t_act): [0, -0.5, -3], str(t_end): [0, 0, 0]}
    return {'loop': False, 'animation_length': t_end, 'bones': bones}


def simple(length, frames):
    """frames: {bone: {t: [rx,ry,rz]}} (+ 'body_pos')"""
    bones = {}
    for b, kf in frames.items():
        if b == 'body_pos':
            bones.setdefault('body', {})['position'] = {str(k): v for k, v in kf.items()}
        else:
            bones.setdefault(b, {})['rotation'] = {str(k): v for k, v in kf.items()}
    return {'loop': False, 'animation_length': length, 'bones': bones}


def strafe(sign, heavy):
    amp = 10 if heavy else 18
    L = 1.2
    f = {'body': {0: [0, 0, 0], 0.3: [0, 0, 4 * sign], 0.6: [0, 0, 0], 0.9: [0, 0, 4 * sign], L: [0, 0, 0]},
         'right_leg': {0: [0, 0, 0], 0.3: [0, 0, amp * sign], 0.6: [0, 0, 0], 0.9: [0, 0, -amp * sign * 0.5], L: [0, 0, 0]},
         'left_leg': {0: [0, 0, 0], 0.3: [0, 0, -amp * sign * 0.5], 0.6: [0, 0, 0], 0.9: [0, 0, amp * sign], L: [0, 0, 0]},
         'right_arm': {0: [0, 0, 0], 0.6: [-15, 0, 8], L: [0, 0, 0]},
         'left_arm': {0: [0, 0, 0], 0.6: [-10, 0, -8], L: [0, 0, 0]},
         'body_pos': {0: [0, 0, 0], 0.3: [0, -0.6, 0], 0.6: [0, 0, 0], 0.9: [0, -0.6, 0], L: [0, 0, 0]}}
    return simple(L, f)


def build(idx):
    heavy = idx in (1, 4)
    a = {}
    a['strafe_l'] = strafe(-1, heavy)
    a['strafe_r'] = strafe(1, heavy)
    a['backstep'] = simple(0.4, {'body': {0: [0, 0, 0], 0.1: [-15, 0, 0], 0.4: [0, 0, 0]},
                                 'right_leg': {0: [0, 0, 0], 0.1: [30, 0, 0], 0.4: [0, 0, 0]},
                                 'left_leg': {0: [0, 0, 0], 0.15: [-20, 0, 0], 0.4: [0, 0, 0]},
                                 'right_arm': {0: [0, 0, 0], 0.1: [-30, 0, 15], 0.4: [0, 0, 0]},
                                 'left_arm': {0: [0, 0, 0], 0.1: [-30, 0, -15], 0.4: [0, 0, 0]}})
    for name, s in (('sidestep_l', -1), ('sidestep_r', 1)):
        a[name] = simple(0.35, {'body': {0: [0, 0, 0], 0.1: [0, 0, 14 * s], 0.35: [0, 0, 0]},
                                'right_leg': {0: [0, 0, 0], 0.1: [0, 0, 25 * s], 0.35: [0, 0, 0]},
                                'left_leg': {0: [0, 0, 0], 0.1: [0, 0, 25 * s], 0.35: [0, 0, 0]},
                                'body_pos': {0: [0, 0, 0], 0.1: [0, -1, 0], 0.35: [0, 0, 0]}})
    if idx == 2:
        a['backflip'] = simple(0.55, {'body': {0: [0, 0, 0], 0.1: [-20, 0, 0], 0.3: [-200, 0, 0], 0.45: [-340, 0, 0], 0.55: [-360, 0, 0]},
                                      'right_leg': {0: [0, 0, 0], 0.2: [-60, 0, 0], 0.45: [0, 0, 0]},
                                      'left_leg': {0: [0, 0, 0], 0.2: [-60, 0, 0], 0.45: [0, 0, 0]},
                                      'body_pos': {0: [0, 0, 0], 0.25: [0, 6, 0], 0.55: [0, 0, 0]}})
    a['dash'] = simple(0.7, {'body': {0: [0, 0, 0], 0.1: [28, 0, 0], 0.55: [28, 0, 0], 0.7: [0, 0, 0]},
                             'right_arm': {0: [0, 0, 0], 0.1: [40, 0, 10], 0.55: [40, 0, 10], 0.7: [0, 0, 0]},
                             'left_arm': {0: [0, 0, 0], 0.1: [40, 0, -10], 0.55: [40, 0, -10], 0.7: [0, 0, 0]},
                             'right_leg': {0: [0, 0, 0], 0.2: [-35, 0, 0], 0.4: [30, 0, 0], 0.7: [0, 0, 0]},
                             'left_leg': {0: [0, 0, 0], 0.2: [30, 0, 0], 0.4: [-35, 0, 0], 0.7: [0, 0, 0]}})
    a['leap'] = simple(1.0, {'body': {0: [0, 0, 0], 0.15: [20, 0, 0], 0.4: [-15, 0, 0], 0.8: [30, 0, 0], 1.0: [0, 0, 0]},
                             'right_arm': {0: [0, 0, 0], 0.15: [30, 0, 0], 0.4: [-170, 0, 0], 0.8: [-20, 0, 0], 1.0: [0, 0, 0]},
                             'left_arm': {0: [0, 0, 0], 0.15: [30, 0, 0], 0.4: [-160, 0, 0], 0.8: [-30, 0, 0], 1.0: [0, 0, 0]},
                             'right_leg': {0: [0, 0, 0], 0.15: [-40, 0, 0], 0.4: [-30, 0, 0], 0.8: [0, 0, 0]},
                             'left_leg': {0: [0, 0, 0], 0.15: [-40, 0, 0], 0.4: [20, 0, 0], 0.8: [0, 0, 0]},
                             'body_pos': {0: [0, 0, 0], 0.15: [0, -3, 0], 0.4: [0, 2, 0], 0.8: [0, -2, 0], 1.0: [0, 0, 0]}})
    for cid, hits in COMBOS[idx].items():
        for n, (kind, w, ac, rec, dl) in enumerate(hits, 1):
            a[f'combo_{cid}_{n}'] = combo_anim(idx, kind, w, ac, rec, dl)
    return a


def main():
    for idx in range(1, 5):
        p = os.path.join(ROOT, f'chg_{idx}.animation.json')
        with open(p, encoding='utf-8') as f:
            data = json.load(f)
        geo = os.path.join(ROOT, '..', '..', 'geo', 'entity', f'chg_{idx}.geo.json')
        with open(geo, encoding='utf-8') as f:
            bones = {b['name'] for b in json.load(f)['minecraft:geometry'][0]['bones']}
        new = build(idx)
        for name, anim in new.items():
            missing = set(anim['bones']) - bones
            assert not missing, (idx, name, missing)
            data['animations'][f'animation.chg_{idx}.{name}'] = anim
        with open(p, 'w', encoding='utf-8') as f:
            json.dump(data, f, indent=1, ensure_ascii=False)
        print(f'chg_{idx}: +{len(new)} animation')


if __name__ == '__main__':
    main()
