"""B5 (PLAN-v1 §5): add BossBrain animations (strafe / backstep / dash / combo_*) to Hu Vuong, Sa Hat Vuong, Bang Nhan,
Doc Mau and the 3 Su Do models. Only bones that exist in each geo.json are animated (checked at run time).
Idempotent: re-running overwrites the same animation names. Run from repo root: python tools/gen_b5_brain_anims.py"""
import json, os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources', 'assets', 'hukhong')


def bones_of(model):
    g = json.load(open(os.path.join(ROOT, 'geo', 'entity', model + '.geo.json'), encoding='utf-8'))
    return {b['name'] for b in g['minecraft:geometry'][0]['bones']}


def t(x):
    return ('%.3f' % x).rstrip('0').rstrip('.')


def anim(length, tracks, loop=False):
    """tracks: {bone: {'rotation'|'position': [(time_s, [x,y,z]), ...]}}"""
    out = {'loop': loop, 'animation_length': round(length, 3), 'bones': {}}
    for bone, ch in tracks.items():
        out['bones'][bone] = {k: {t(tm): v for tm, v in keys} for k, keys in ch.items()}
    return out


def swing(L, w, a, pose_up, pose_hit, extra=None):
    """Generic attack: neutral -> wind-up pose at w -> strike pose at w+a -> neutral at L (seconds).
    pose_up / pose_hit: {bone: [rx,ry,rz]}; extra: {bone: {'position': [...]}} added as is."""
    tr = {}
    for b in set(pose_up) | set(pose_hit):
        up, hit = pose_up.get(b, [0, 0, 0]), pose_hit.get(b, [0, 0, 0])
        tr[b] = {'rotation': [(0, [0, 0, 0]), (w, up), (w + a, hit), (min(L, w + a + (L - w - a) * 0.5), [h * 0.6 for h in hit]), (L, [0, 0, 0])]}
    for b, ch in (extra or {}).items():
        tr.setdefault(b, {}).update(ch)
    return anim(L, tr)


def sec(windup, active, recovery):
    return windup / 20.0, active / 20.0, (windup + active + recovery) / 20.0


def strafe(side, legs, body='body', lean=8):
    s = 1 if side == 'r' else -1
    tr = {body: {'rotation': [(0, [0, 0, s * lean]), (0.5, [0, 0, s * lean * 0.6]), (1.0, [0, 0, s * lean])]}}
    for i, lg in enumerate(legs):
        ph = 1 if i % 2 == 0 else -1
        tr[lg] = {'rotation': [(0, [0, 0, ph * 18 * s]), (0.5, [0, 0, -ph * 18 * s]), (1.0, [0, 0, ph * 18 * s])]}
    return anim(1.0, tr, loop=True)


def backstep(legs, body='body', back=-2.5, length=0.4):
    tr = {body: {'rotation': [(0, [0, 0, 0]), (0.12, [-14, 0, 0]), (length, [0, 0, 0])],
                 'position': [(0, [0, 0, 0]), (0.12, [0, -1, back]), (length, [0, 0, 0])]}}
    for lg in legs:
        tr[lg] = {'rotation': [(0, [0, 0, 0]), (0.12, [30, 0, 0]), (length, [0, 0, 0])]}
    return anim(length, tr)


def sidestep(side, legs, body='body'):
    s = 1 if side == 'r' else -1
    tr = {body: {'rotation': [(0, [0, 0, 0]), (0.1, [0, 0, s * 16]), (0.32, [0, 0, 0])]}}
    for lg in legs:
        tr[lg] = {'rotation': [(0, [0, 0, 0]), (0.1, [0, 0, s * 26]), (0.32, [0, 0, 0])]}
    return anim(0.32, tr)


def backflip(legs, body='body'):
    tr = {body: {'rotation': [(0, [0, 0, 0]), (0.1, [-20, 0, 0]), (0.3, [-200, 0, 0]), (0.45, [-340, 0, 0]), (0.5, [-360, 0, 0])],
                 'position': [(0, [0, 0, 0]), (0.25, [0, 8, 2]), (0.5, [0, 0, 0])]}}
    for lg in legs:
        tr[lg] = {'rotation': [(0, [0, 0, 0]), (0.2, [-60, 0, 0]), (0.5, [0, 0, 0])]}
    return anim(0.5, tr)


def dash(arms, legs, body='body', lean=24, length=0.7):
    tr = {body: {'rotation': [(0, [0, 0, 0]), (0.15, [lean, 0, 0]), (length - 0.15, [lean, 0, 0]), (length, [0, 0, 0])]}}
    for a in arms:
        tr[a] = {'rotation': [(0, [0, 0, 0]), (0.15, [50, 0, 0]), (length, [0, 0, 0])]}
    for i, lg in enumerate(legs):
        sgn = 1 if i % 2 == 0 else -1
        tr[lg] = {'rotation': [(0, [0, 0, 0]), (0.15, [-40 * sgn, 0, 0]), (0.35, [40 * sgn, 0, 0]), (length, [0, 0, 0])]}
    return anim(length, tr)


def keep(model, anims):
    have = bones_of(model)

    def clean(a):
        a['bones'] = {b: v for b, v in a['bones'].items() if b in have}
        return a
    path = os.path.join(ROOT, 'animations', 'entity', model + '.animation.json')
    data = json.load(open(path, encoding='utf-8'))
    for name, a in anims.items():
        data['animations']['animation.%s.%s' % (model, name)] = clean(a)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=1)
    print(model, sorted(anims))


# ---------------------------------------------------------------- Hu Vuong: no strafe, pull / leap, axe sweeps, alternating arms, tail spin
def hu_vuong():
    A = {}
    A['dash'] = swing(1.0, 0.3, 0.4, {'left_arm': [-100, 0, 20], 'right_arm': [-100, 0, -20], 'body': [-10, 0, 0]},
                      {'left_arm': [-40, 0, 10], 'right_arm': [-40, 0, -10], 'body': [12, 0, 0]})  # pull: reach out, drag back
    A['leap'] = anim(1.0, {'body': {'rotation': [(0, [0, 0, 0]), (0.2, [20, 0, 0]), (0.5, [-10, 0, 0]), (1.0, [0, 0, 0])],
                                    'position': [(0, [0, 0, 0]), (0.2, [0, -3, 0]), (0.5, [0, 6, 0]), (1.0, [0, 0, 0])]},
                           'right_arm': {'rotation': [(0, [0, 0, 0]), (0.5, [-160, 0, 0]), (1.0, [0, 0, 0])]},
                           'left_arm': {'rotation': [(0, [0, 0, 0]), (0.5, [-160, 0, 0]), (1.0, [0, 0, 0])]}})
    for i, (w, a, r, side) in enumerate([(16, 5, 12, 1), (12, 5, 12, -1)], 1):
        W, Ac, L = sec(w, a, r)
        A['combo_riu_%d' % i] = swing(L, W, Ac, {'right_arm': [-90, 50 * side, 0], 'body': [0, 35 * side, 0]},
                                      {'right_arm': [-80, -70 * side, 0], 'body': [0, -40 * side, 0]})
    W, Ac, L = sec(18, 5, 18)
    A['combo_riu_3'] = swing(L, W, Ac, {'right_leg': [-50, 0, 0], 'body': [-8, 0, 0], 'right_arm': [-150, 0, 0]},
                             {'right_leg': [0, 0, 0], 'body': [10, 0, 0], 'right_arm': [-30, 0, 0]},
                             {'body': {'position': [(0, [0, 0, 0]), (W, [0, 2, 0]), (W + Ac, [0, -2, 0]), (L, [0, 0, 0])]}})
    for i in range(1, 5):
        arm = 'right_arm' if i % 2 else 'left_arm'
        W, Ac, L = sec(10, 4, 8 if i < 4 else 16)
        A['combo_tay_%d' % i] = swing(L, W, Ac, {arm: [-150, 0, 0], 'body': [-6, 20 if i % 2 else -20, 0]},
                                      {arm: [-20, 0, 0], 'body': [12, -10 if i % 2 else 10, 0]})
    W, Ac, L = sec(14, 8, 16)
    A['combo_duoi_1'] = anim(L, {'body': {'rotation': [(0, [0, 0, 0]), (W, [0, 40, 0]), (W + Ac, [0, -320, 0]), (L, [0, -360, 0])]},
                                 'mantle': {'rotation': [(0, [0, 0, 0]), (W + Ac * 0.5, [40, 0, 0]), (L, [0, 0, 0])]}})
    keep('hu_vuong', A)


# ---------------------------------------------------------------- Sa Hat Vuong: scorpion; strafe / slide back / gravity dash / twin swords
def sa_hat():
    legs = ['leg%d' % i for i in range(8)]
    A = {'strafe_l': strafe('l', legs), 'strafe_r': strafe('r', legs),
         'backstep': backstep(legs, back=-4, length=0.5),
         'sidestep_l': sidestep('l', legs), 'sidestep_r': sidestep('r', legs),
         'backflip': backstep(legs, back=-5, length=0.6),
         'dash': dash(['claw_r', 'claw_l'], legs, lean=10, length=1.1)}
    A['dash']['bones']['tail_1'] = {'rotation': {'0': [0, 0, 0], '0.3': [-40, 0, 0], '1.1': [0, 0, 0]}}
    seq = [('claw_l', 1), ('claw_l', -1), ('claw_r', 1), ('claw_r', -1)]
    for i, (claw, s) in enumerate(seq, 1):
        W, Ac, L = sec(9 if i > 1 else 12, 3, 5 if i < 4 else 12)
        A['combo_song_kiem_%d' % i] = swing(L, W, Ac, {claw: [-110, 40 * s, 0], 'body': [0, 20 * s, 0]},
                                            {claw: [-30, -50 * s, 0], 'body': [0, -20 * s, 0]})
    W, Ac, L = sec(16, 4, 18)
    A['combo_dap_cheo_1'] = swing(L, W, Ac, {'claw_r': [-160, 0, -30], 'claw_l': [-160, 0, 30], 'tail_1': [-30, 0, 0], 'body': [-10, 0, 0]},
                                  {'claw_r': [-10, 0, 20], 'claw_l': [-10, 0, -20], 'tail_1': [20, 0, 0], 'body': [14, 0, 0]})
    keep('sa_hat_vuong', A)


# ---------------------------------------------------------------- Bang Nhan: hover drift, blink, staff 2 hits + ice burst
def bang_nhan():
    def drift(side):
        s = 1 if side == 'r' else -1
        return anim(1.6, {'body': {'rotation': [(0, [4, 0, 6 * s]), (0.8, [-2, 0, 10 * s]), (1.6, [4, 0, 6 * s])],
                                   'position': [(0, [0, 1, 0]), (0.8, [0, 2, 0]), (1.6, [0, 1, 0])]},
                          'right_leg': {'rotation': [(0, [10, 0, 0]), (1.6, [10, 0, 0])]},
                          'left_leg': {'rotation': [(0, [14, 0, 0]), (1.6, [14, 0, 0])]},
                          'shard_1': {'rotation': [(0, [0, 0, 0]), (1.6, [0, 360, 0])]}}, loop=True)
    A = {'strafe_l': drift('l'), 'strafe_r': drift('r'),
         'dash': anim(0.5, {'body': {'rotation': [(0, [0, 0, 0]), (0.2, [0, 180, 0]), (0.4, [0, 360, 0]), (0.5, [0, 360, 0])],
                                     'position': [(0, [0, 0, 0]), (0.2, [0, 3, 0]), (0.5, [0, 0, 0])]},
                            'staff': {'rotation': [(0, [0, 0, 0]), (0.25, [-90, 0, 0]), (0.5, [0, 0, 0])]}})}
    A['backstep'] = A['dash']
    for i, s in [(1, 1), (2, -1)]:
        W, Ac, L = sec(10 if i == 1 else 8, 3, 6)
        A['combo_gay_%d' % i] = swing(L, W, Ac, {'right_arm': [-130, 30 * s, 0], 'body': [0, 25 * s, 0]},
                                      {'right_arm': [-40, -40 * s, 0], 'body': [0, -25 * s, 0]})
    W, Ac, L = sec(16, 4, 18)
    A['combo_gay_3'] = swing(L, W, Ac, {'right_arm': [-170, 0, 0], 'left_arm': [-170, 0, 0], 'body': [-10, 0, 0]},
                             {'right_arm': [-20, 0, 0], 'left_arm': [-20, 0, 0], 'body': [15, 0, 0]})
    A['combo_no_1'] = A['combo_gay_3']
    keep('bang_nhan', A)


# ---------------------------------------------------------------- Doc Mau: strafe, spit-retreat, aggressive lunge, claw x2 + close spit
def doc_mau():
    legs = ['right_leg', 'left_leg']
    A = {'strafe_l': strafe('l', legs), 'strafe_r': strafe('r', legs),
         'backstep': swing(0.45, 0.12, 0.1, {'head': [-30, 0, 0], 'body': [-10, 0, 0]}, {'head': [20, 0, 0], 'body': [-16, 0, 0]},
                           {'body': {'position': [(0, [0, 0, 0]), (0.2, [0, 0, -2]), (0.45, [0, 0, 0])]}}),
         'sidestep_l': sidestep('l', legs), 'sidestep_r': sidestep('r', legs),
         'dash': dash(['right_arm', 'left_arm'], legs, lean=28, length=0.6)}
    for i, arm, s in [(1, 'right_arm', 1), (2, 'left_arm', -1)]:
        W, Ac, L = sec(9, 3, 6)
        A['combo_cao_%d' % i] = swing(L, W, Ac, {arm: [-140, 20 * s, 30 * s], 'body': [0, 20 * s, 0], 'vine_%d' % i: [-30, 0, 0]},
                                      {arm: [-20, -30 * s, -10 * s], 'body': [8, -20 * s, 0], 'vine_%d' % i: [30, 0, 0]})
    W, Ac, L = sec(12, 4, 14)
    A['combo_cao_3'] = swing(L, W, Ac, {'head': [-35, 0, 0], 'body': [-12, 0, 0]}, {'head': [25, 0, 0], 'body': [14, 0, 0]})
    keep('doc_mau', A)


# ---------------------------------------------------------------- Su Do (3 models): Margit cane tempo, backflip, taunt
def su_do(model):
    cane_arm = 'right_arm' if model == 'su_do_chu' else 'left_arm'
    off_arm = 'left_arm' if cane_arm == 'right_arm' else 'right_arm'
    legs = ['right_leg', 'left_leg']
    A = {'strafe_l': strafe('l', legs, lean=5), 'strafe_r': strafe('r', legs, lean=5),
         'backstep': backstep(legs), 'sidestep_l': sidestep('l', legs), 'sidestep_r': sidestep('r', legs),
         'backflip': backflip(legs), 'dash': dash([cane_arm], legs, lean=20, length=0.6),
         'taunt': anim(1.6, {'body': {'rotation': [(0, [0, 0, 0]), (0.3, [-8, 0, 0]), (1.3, [-8, 0, 0]), (1.6, [0, 0, 0])]},
                             'head': {'rotation': [(0, [0, 0, 0]), (0.3, [-20, 0, 8]), (0.8, [-20, 0, -8]), (1.3, [-20, 0, 8]), (1.6, [0, 0, 0])]},
                             cane_arm: {'rotation': [(0, [0, 0, 0]), (0.3, [-30, 0, 0]), (1.3, [-30, 0, 0]), (1.6, [0, 0, 0])]},
                             off_arm: {'rotation': [(0, [0, 0, 0]), (0.4, [-90, 0, 0]), (0.7, [-70, 30, 0]), (1.0, [-90, 0, 0]), (1.6, [0, 0, 0])]}})}
    # truong: fast, DELAYED, fast, DELAYED + hammer (windups mirror SuDo.BRAIN; delayExtra 12)
    hits = [(9, 3, 5, 1, False), (10 + 12, 4, 6, -1, True), (7, 3, 5, 1, False), (12 + 12, 5, 16, 0, True)]
    for i, (w, a, r, s, delayed) in enumerate(hits, 1):
        W, Ac, L = sec(w, a, r)
        up = {cane_arm: [-150 if s == 0 else -120, 40 * s, 0], 'body': [-6 if delayed else 0, 25 * s, 0]}
        hit = {cane_arm: [-10, -40 * s, 0], 'body': [12 if s == 0 else 4, -25 * s, 0]}
        if s == 0:
            up[off_arm] = [-150, 0, 0]; hit[off_arm] = [-10, 0, 0]
        A['combo_truong_%d' % i] = swing(L, W, Ac, up, hit)
    for i, (w, a, r) in enumerate([(12, 4, 8), (8, 4, 16)], 1):
        W, Ac, L = sec(w, a, r)
        A['combo_dam_%d' % i] = swing(L, W, Ac, {cane_arm: [-60, 0, 0], 'body': [-8, 0, 0]}, {cane_arm: [-95, 0, 0], 'body': [16, 0, 0]})
    W, Ac, L = sec(9, 4, 14)
    A['combo_quet_1'] = anim(L, {'body': {'rotation': [(0, [0, 0, 0]), (W, [0, 50, 0]), (W + Ac, [0, -230, 0]), (L, [0, -360, 0])]},
                                 cane_arm: {'rotation': [(0, [0, 0, 0]), (W, [-90, 0, 0]), (L, [0, 0, 0])]}})
    keep(model, A)


if __name__ == '__main__':
    hu_vuong(); sa_hat(); bang_nhan(); doc_mau()
    for m in ('su_do_kiem', 'su_do_chu', 'su_do_te'):
        su_do(m)
