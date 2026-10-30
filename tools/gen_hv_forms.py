#!/usr/bin/env python3
"""Dot 4: 8 dang Hu Vuong tren 4 khung xuong (idempotent; chay truoc gen_boss_phase.py, script nay tu goi no o cuoi).

Sinh:
  geo/entity/hvf_<khung>.geo.json            khung: giant | quad | worm | spirit  (xuong body/head/... + xuong phu kien acc_<dang>_<n>)
  animations/entity/hvf_<khung>.animation.json   animation.hvf.{idle,walk,attack,cast,phase}  (ten chung, moi khung 1 file)
  textures/entity/hvf_<dang>.png + _glowmask.png (256x256, mau/hoa van rieng moi dang)
Sau do gen_boss_phase.py them xuong ph2_spikes/ph3_halo + ban tint _p2/_p3.

Atlas: 8 vat lieu 32x32 o hang tren cung: skin dark accent light glow face face2 face3 (x = 32*i).
Moi cube dung UV theo mat (uv_size <= 32) nen khong can box-UV rieng.
"""
import json
import math
import os
import random
import subprocess
import sys

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "hukhong")
GEO = os.path.join(ROOT, "geo", "entity")
ANI = os.path.join(ROOT, "animations", "entity")
TEX = os.path.join(ROOT, "textures", "entity")
for d in (GEO, ANI, TEX):
    os.makedirs(d, exist_ok=True)

MATS = ["skin", "dark", "accent", "light", "glow", "face", "face2", "face3"]
MI = {m: i for i, m in enumerate(MATS)}


# ------------------------------------------------------------------ hinh hoc
def cube(mat, o, s, rot=None, pivot=None):
    """Cube voi UV theo mat. o=origin, s=size (px)."""
    mx = MI[mat] * 32
    w, h, d = s

    def face(a, b, k):
        # lay tu goc vung vat lieu, dich nhe theo k de bot lap lai
        return {"uv": [mx + (k * 3) % 8, (k * 5) % 8], "uv_size": [min(a, 24), min(b, 24)]}

    c = {"origin": list(o), "size": list(s), "uv": {
        "north": face(w, h, 0), "south": face(w, h, 1), "east": face(d, h, 2),
        "west": face(d, h, 3), "up": face(w, d, 4), "down": face(w, d, 5)}}
    if mat in ("face", "face2", "face3"):  # mat nguoi: nguyen vung 32x32 ep len mat truoc (north), cac mat khac lay da
        base = MI["skin"] * 32
        for k in ("south", "east", "west", "up", "down"):
            c["uv"][k]["uv"][0] += base - mx
        c["uv"]["north"] = {"uv": [mx, 0], "uv_size": [32, 32]}
    if rot:
        c["rotation"] = list(rot)
        c["pivot"] = list(pivot if pivot else [o[0] + w / 2, o[1] + h / 2, o[2] + d / 2])
    return c


def bone(name, pivot, cubes, parent=None):
    b = {"name": name, "pivot": list(pivot), "cubes": cubes}
    if parent:
        b["parent"] = parent
    return b


def geo(ident, bones, w=5, h=5, off=2.0):
    return {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry." + ident, "texture_width": 256, "texture_height": 256,
                        "visible_bounds_width": w, "visible_bounds_height": h, "visible_bounds_offset": [0, off, 0]},
        "bones": bones}]}


def mirror(c):
    """Doi xung qua x=0 (cho chi trai/phai)."""
    d = json.loads(json.dumps(c))
    o, s = d["origin"], d["size"]
    d["origin"] = [-o[0] - s[0], o[1], o[2]]
    if "pivot" in d:
        d["pivot"][0] = -d["pivot"][0]
    if "rotation" in d:
        d["rotation"] = [d["rotation"][0], -d["rotation"][1], -d["rotation"][2]]
    return d


def pair(bone_r, bone_l_name, bone_l_pivot):
    l = json.loads(json.dumps(bone_r))
    l["name"] = bone_l_name
    l["pivot"] = bone_l_pivot
    l["cubes"] = [mirror(c) for c in bone_r["cubes"]]
    return l


# ------------------------------------------------------------------ khung xuong
def giant():
    body = bone("body", [0, 26, 0], [
        cube("skin", [-13, 26, -8], [26, 26, 16]), cube("dark", [-10, 22, -7], [20, 4, 14]),
        cube("dark", [-20, 46, -6], [9, 6, 12]), cube("dark", [11, 46, -6], [9, 6, 12]),
        cube("dark", [-9, 30, 8], [18, 18, 3]), cube("accent", [-4, 30, -9], [8, 12, 1])])
    head = bone("head", [0, 52, 0], [
        cube("skin", [-7, 52, -7], [14, 14, 14]), cube("face", [-7, 52, -7.1], [14, 14, 0.1]),
        cube("dark", [-4, 50, -8], [8, 3, 2])], "body")
    rarm = bone("right_arm", [-18, 48, 0], [
        cube("skin", [-22, 22, -4], [8, 26, 8]), cube("dark", [-23, 16, -5], [10, 8, 10])], "body")
    larm = pair(rarm, "left_arm", [18, 48, 0])
    rleg = bone("right_leg", [-6, 26, 0], [
        cube("skin", [-10, 4, -4], [9, 22, 8]), cube("dark", [-11, 0, -6], [11, 4, 12])])
    lleg = pair(rleg, "left_leg", [6, 26, 0])
    acc = [
        # Huyet Ma: sung, vuot, suon gai
        bone("acc_huyet_ma_1", [0, 64, 0], [
            cube("dark", [-9, 64, -2], [3, 8, 3], [0, 0, 20], [-8, 64, -1]), cube("dark", [6, 64, -2], [3, 8, 3], [0, 0, -20], [8, 64, -1]),
            cube("accent", [-13, 70, -2], [2, 6, 2], [0, 0, 35], [-12, 70, -1]), cube("accent", [11, 70, -2], [2, 6, 2], [0, 0, -35], [12, 70, -1])], "head"),
        bone("acc_huyet_ma_2", [-18, 48, 0], [
            cube("light", [-22, 6, -6], [2, 10, 2]), cube("light", [-19, 5, -7], [2, 11, 2]), cube("light", [-16, 6, -6], [2, 10, 2])], "right_arm"),
        bone("acc_huyet_ma_3", [18, 48, 0], [
            cube("light", [20, 6, -6], [2, 10, 2]), cube("light", [17, 5, -7], [2, 11, 2]), cube("light", [14, 6, -6], [2, 10, 2])], "left_arm"),
        bone("acc_huyet_ma_4", [0, 26, 0], [
            cube("light", [-10, 30, -10], [2, 10, 2], [-20, 0, 0]), cube("light", [-5, 33, -10], [2, 12, 2], [-20, 0, 0]),
            cube("light", [1, 33, -10], [2, 12, 2], [-20, 0, 0]), cube("light", [6, 30, -10], [2, 10, 2], [-20, 0, 0])], "body"),
        # Moc Yeu: nhanh gac, re, day leo, la
        bone("acc_moc_yeu_1", [0, 64, 0], [
            cube("dark", [-9, 64, -1], [2, 10, 2]), cube("dark", [-12, 70, -1], [2, 6, 2], [0, 0, 40], [-11, 70, 0]), cube("dark", [-7, 72, -1], [2, 5, 2], [0, 0, -30], [-6, 72, 0]),
            cube("dark", [7, 64, -1], [2, 10, 2]), cube("dark", [10, 70, -1], [2, 6, 2], [0, 0, -40], [11, 70, 0]), cube("dark", [5, 72, -1], [2, 5, 2], [0, 0, 30], [6, 72, 0]),
            cube("accent", [-6, 65, -6], [12, 2, 12])], "head"),
        bone("acc_moc_yeu_2", [0, 26, 0], [
            cube("dark", [-14, 40, 6], [3, 22, 3], [-25, 0, 10], [-13, 40, 7]), cube("dark", [-4, 44, 8], [3, 24, 3], [-30, 0, 0], [-3, 44, 9]),
            cube("dark", [8, 40, 6], [3, 22, 3], [-25, 0, -10], [9, 40, 7]), cube("accent", [-8, 48, 9], [6, 6, 2])], "body"),
        bone("acc_moc_yeu_3", [0, 0, 0], [
            cube("dark", [-14, 0, -10], [3, 3, 16], [0, 20, 0]), cube("dark", [10, 0, -10], [3, 3, 16], [0, -20, 0]),
            cube("dark", [-4, 0, 6], [3, 3, 14], [0, 8, 0]), cube("accent", [-9, 3, -4], [5, 2, 5]), cube("accent", [6, 3, 2], [5, 2, 5])]),
        bone("acc_moc_yeu_4", [18, 48, 0], [
            cube("accent", [13, 26, -5], [10, 2, 10]), cube("accent", [13, 34, -5], [10, 2, 10]), cube("accent", [13, 42, -5], [10, 2, 10])], "left_arm"),
    ]
    return geo("hvf_giant", [body, head, rarm, larm, rleg, lleg] + acc, 6, 6, 3.0)


def quad():
    body = bone("body", [0, 22, 0], [
        cube("skin", [-11, 16, -14], [22, 20, 28]), cube("dark", [-10, 34, -12], [20, 6, 12]),
        cube("dark", [-12, 16, 6], [24, 4, 8]), cube("accent", [-5, 20, -15], [10, 10, 1])])
    head = bone("head", [0, 28, -14], [
        cube("skin", [-7, 22, -26], [14, 12, 14]), cube("face", [-7, 22, -26.1], [14, 12, 0.1]),
        cube("skin", [-4, 22, -34], [8, 6, 8]), cube("dark", [-4, 18, -32], [8, 4, 8]), cube("skin", [-5, 26, -16], [10, 10, 6])], "body")
    fr = bone("front_right", [-9, 18, -10], [cube("skin", [-13, 4, -13], [8, 14, 8]), cube("dark", [-14, 0, -16], [10, 4, 12])])
    fl = pair(fr, "front_left", [9, 18, -10])
    br = bone("back_right", [-9, 18, 12], [cube("skin", [-13, 4, 8], [9, 14, 9]), cube("dark", [-14, 0, 6], [11, 4, 13])])
    bl = pair(br, "back_left", [9, 18, 12])
    t1 = bone("tail1", [0, 26, 14], [cube("skin", [-4, 22, 14], [8, 8, 14])], "body")
    t2 = bone("tail2", [0, 26, 28], [cube("skin", [-3, 22, 28], [6, 6, 12])], "tail1")
    t3 = bone("tail3", [0, 26, 40], [cube("dark", [-2, 22, 40], [4, 4, 10])], "tail2")
    acc = [
        # Cot Long: song lung xuong, sung dai, suon
        bone("acc_cot_long_1", [0, 22, 0], [cube("light", [-1.5, 40 - (i % 2), -10 + i * 5], [3, 6 + (i % 3), 3]) for i in range(6)], "body"),
        bone("acc_cot_long_2", [0, 28, -14], [
            cube("light", [-8, 34, -18], [3, 3, 14], [-20, 0, 15], [-6, 34, -12]), cube("light", [5, 34, -18], [3, 3, 14], [-20, 0, -15], [6, 34, -12]),
            cube("light", [-11, 40, -10], [2, 2, 8], [-20, 0, 25], [-10, 40, -6]), cube("light", [9, 40, -10], [2, 2, 8], [-20, 0, -25], [10, 40, -6])], "head"),
        bone("acc_cot_long_3", [0, 22, 0], [
            cube("light", [-12, 18, -8], [1, 14, 2]), cube("light", [-12, 18, -2], [1, 14, 2]), cube("light", [-12, 18, 4], [1, 14, 2]),
            cube("light", [11, 18, -8], [1, 14, 2]), cube("light", [11, 18, -2], [1, 14, 2]), cube("light", [11, 18, 4], [1, 14, 2])], "body"),
        bone("acc_cot_long_4", [0, 26, 40], [cube("light", [-1, 18, 48], [2, 16, 6]), cube("accent", [-1, 24, 52], [2, 6, 4])], "tail3"),
        # Doc Chuong: tui doc, nam, nanh nho
        bone("acc_doc_chuong_1", [0, 22, 0], [
            cube("accent", [-7, 36, -6], [9, 8, 9]), cube("glow", [-5, 38, -4], [5, 5, 5]), cube("accent", [3, 36, 4], [8, 7, 8]),
            cube("glow", [5, 38, 6], [4, 4, 4]), cube("accent", [-9, 34, 8], [6, 6, 6])], "body"),
        bone("acc_doc_chuong_2", [0, 22, 0], [
            cube("dark", [-10, 36, 0], [2, 4, 2]), cube("accent", [-12, 40, -1], [6, 2, 6]), cube("dark", [8, 36, -6], [2, 5, 2]), cube("accent", [6, 41, -7], [7, 2, 7]),
            cube("dark", [0, 40, 12], [2, 4, 2]), cube("accent", [-2, 44, 11], [6, 2, 6])], "body"),
        bone("acc_doc_chuong_3", [0, 28, -14], [
            cube("light", [-3, 14, -33], [1.5, 6, 1.5]), cube("light", [1.5, 14, -33], [1.5, 6, 1.5]), cube("glow", [-1, 10, -32], [2, 5, 2])], "head"),
        bone("acc_doc_chuong_4", [0, 26, 14], [cube("accent", [-4, 30, 14], [8, 6, 8]), cube("glow", [-2, 31, 16], [4, 4, 4]), cube("accent", [-2, 28, 30], [5, 5, 5])], "tail1"),
    ]
    return geo("hvf_quad", [body, head, fr, fl, br, bl, t1, t2, t3] + acc, 7, 5, 2.0)


def worm():
    body = bone("body", [0, 0, 6], [cube("skin", [-11, 0, -6], [22, 18, 26]), cube("dark", [-12, 12, -2], [24, 3, 6]), cube("dark", [-12, 12, 10], [24, 3, 6])])
    n1 = bone("neck1", [0, 14, -6], [cube("skin", [-8, 14, -12], [16, 18, 16]), cube("dark", [-9, 22, -13], [18, 3, 18])], "body")
    n2 = bone("neck2", [0, 30, -6], [cube("skin", [-7, 30, -12], [14, 16, 14]), cube("dark", [-8, 38, -13], [16, 3, 16])], "neck1")
    head = bone("head", [0, 44, -6], [
        cube("skin", [-8, 44, -16], [16, 14, 18]), cube("face", [-8, 44, -16.1], [16, 14, 0.1]),
        cube("dark", [-6, 42, -18], [12, 3, 10]), cube("light", [-5, 45, -17], [2, 3, 1]), cube("light", [3, 45, -17], [2, 3, 1])], "neck2")
    t1 = bone("tail1", [0, 8, 20], [cube("skin", [-9, 0, 20], [18, 14, 18])], "body")
    t2 = bone("tail2", [0, 6, 38], [cube("skin", [-7, 0, 38], [14, 11, 18]), cube("dark", [-8, 8, 40], [16, 3, 6])], "tail1")
    t3 = bone("tail3", [0, 5, 56], [cube("skin", [-5, 0, 56], [10, 8, 16])], "tail2")
    t4 = bone("tail4", [0, 4, 70], [cube("dark", [-3, 0, 70], [6, 5, 12])], "tail3")
    acc = [
        # Sa Tran: ao cat, nanh ham, san cat
        bone("acc_sa_tran_1", [0, 44, -6], [
            cube("accent", [-20, 46, -8], [12, 14, 1], [0, -25, 0]), cube("accent", [8, 46, -8], [12, 14, 1], [0, 25, 0]),
            cube("light", [-16, 54, -8], [4, 8, 1], [0, -25, 0]), cube("light", [12, 54, -8], [4, 8, 1], [0, 25, 0])], "head"),
        bone("acc_sa_tran_2", [0, 44, -6], [cube("light", [-9, 44, -18], [2, 2, 9]), cube("light", [7, 44, -18], [2, 2, 9]), cube("light", [-3, 41, -18], [1.5, 4, 1.5]), cube("light", [2, 41, -18], [1.5, 4, 1.5])], "head"),
        bone("acc_sa_tran_3", [0, 30, -6], [cube("accent", [-14, 22, -6], [6, 12, 1], [0, 20, 0]), cube("accent", [8, 22, -6], [6, 12, 1], [0, -20, 0]), cube("accent", [-14, 36, -4], [6, 10, 1], [0, 20, 0]), cube("accent", [8, 36, -4], [6, 10, 1], [0, -20, 0])], "neck1"),
        bone("acc_sa_tran_4", [0, 0, 6], [cube("dark", [-2, 18, -2], [4, 6, 4]), cube("dark", [-2, 18, 8], [4, 8, 4]), cube("dark", [-2, 18, 18], [4, 6, 4]), cube("light", [-1, 24, 8], [2, 4, 2])], "body"),
        # Hai Uyen: san ho, den cau, vay lung
        bone("acc_hai_uyen_1", [0, 44, -6], [
            cube("accent", [-3, 58, -8], [2, 10, 2]), cube("accent", [-6, 62, -8], [2, 6, 2], [0, 0, 30], [-5, 62, -7]), cube("accent", [3, 58, -4], [2, 12, 2]),
            cube("accent", [5, 64, -4], [2, 6, 2], [0, 0, -30], [6, 64, -3]), cube("light", [-1, 66, -2], [2, 4, 2])], "head"),
        bone("acc_hai_uyen_2", [0, 44, -6], [cube("light", [-0.5, 58, -16], [1, 10, 1]), cube("light", [-0.5, 67, -17], [1, 1, 8]), cube("glow", [-2, 65, -26], [4, 4, 4])], "head"),
        bone("acc_hai_uyen_3", [0, 30, -6], [cube("accent", [-18, 46, -6], [10, 10, 1], [0, -20, 0]), cube("accent", [8, 46, -6], [10, 10, 1], [0, 20, 0]), cube("accent", [-17, 30, -4], [8, 8, 1], [0, -20, 0]), cube("accent", [9, 30, -4], [8, 8, 1], [0, 20, 0])], "neck2"),
        bone("acc_hai_uyen_4", [0, 0, 6], [cube("accent", [-0.5, 18, -2], [1, 10, 14]), cube("accent", [-0.5, 12, 18], [1, 8, 14]), cube("accent", [-0.5, 10, 34], [1, 7, 14]), cube("light", [-0.5, 17, 56], [1, 6, 10])], "body"),
    ]
    return geo("hvf_worm", [body, n1, n2, head, t1, t2, t3, t4] + acc, 6, 6, 2.5)


def spirit():
    H = 14  # noi len: thuc the troi cach dat 14px (~0.9 block)
    body = bone("body", [0, 36 + H, 0], [
        cube("skin", [-9, 24 + H, -6], [18, 24, 12]), cube("accent", [-10, 38 + H, -7], [20, 3, 14]), cube("dark", [-6, 26 + H, -7], [12, 8, 1])])
    head = bone("head", [0, 50 + H, 0], [
        cube("skin", [-7, 50 + H, -7], [14, 14, 14]), cube("face", [-7, 50 + H, -7.1], [14, 14, 0.1]), cube("dark", [-8, 62 + H, -8], [16, 2, 16])], "body")
    w1 = bone("wisp1", [0, 26 + H, 0], [cube("skin", [-7, 14 + H, -5], [14, 10, 10])], "body")
    w2 = bone("wisp2", [0, 16 + H, 0], [cube("skin", [-5, 6 + H, -4], [10, 9, 8])], "wisp1")
    w3 = bone("wisp3", [0, 8 + H, 0], [cube("light", [-3, 0 + H, -3], [6, 7, 6])], "wisp2")
    rh = bone("right_hand", [-14, 44 + H, 0], [cube("skin", [-18, 36 + H, -3], [5, 10, 5]), cube("light", [-20, 28 + H, -4], [8, 8, 8])], "body")
    lh = pair(rh, "left_hand", [14, 44 + H, 0])
    acc = [
        # Bang Phach: man bang tren dau, manh bang xoay, bang tren tay
        bone("acc_bang_phach_1", [0, 64 + H, 0], [
            cube("light", [-6, 64 + H, -5], [3, 10, 3], [0, 0, 12], [-5, 64 + H, -4]), cube("light", [-1.5, 66 + H, -1.5], [3, 14, 3]),
            cube("light", [3, 64 + H, 2], [3, 9, 3], [0, 0, -12], [4, 64 + H, 3]), cube("light", [-9, 62 + H, 1], [3, 7, 3], [0, 0, 28], [-8, 62 + H, 2]), cube("light", [6, 62 + H, -4], [3, 7, 3], [0, 0, -28], [7, 62 + H, -3])], "head"),
        bone("acc_bang_phach_2", [0, 40 + H, 0], [cube("light", [14 * math.cos(i * math.pi / 3) - 1.5, 30 + H + (i % 2) * 14, 14 * math.sin(i * math.pi / 3) - 1.5], [3, 9, 3]) for i in range(6)], "body"),
        bone("acc_bang_phach_3", [-14, 44 + H, 0], [cube("light", [-23, 30 + H, -2], [3, 8, 3], [0, 0, 20], [-22, 32 + H, -1]), cube("light", [-18, 24 + H, -2], [3, 7, 3], [0, 0, -15], [-17, 26 + H, -1])], "right_hand"),
        bone("acc_bang_phach_4", [14, 44 + H, 0], [cube("light", [20, 30 + H, -2], [3, 8, 3], [0, 0, -20], [21, 32 + H, -1]), cube("light", [15, 24 + H, -2], [3, 7, 3], [0, 0, 15], [16, 26 + H, -1])], "left_hand"),
        # Thien Dien: mat 2 (sau gay), mat 3 (dinh dau), mu mien vang, vong chu
        bone("acc_thien_dien_1", [0, 50 + H, 0], [cube("skin", [-7, 50 + H, 7], [14, 14, 1]), cube("face2", [-7, 50 + H, 8.1], [14, 14, 0.1]), cube("dark", [-8, 49 + H, 6.5], [16, 16, 1])], "head"),
        bone("acc_thien_dien_2", [0, 64 + H, 0], [cube("skin", [-6, 65 + H, -2], [12, 12, 2]), cube("face3", [-6, 65 + H, -2.1], [12, 12, 0.1]), cube("dark", [-7, 64 + H, -1], [14, 14, 1])], "head"),
        bone("acc_thien_dien_3", [0, 64 + H, 0], [cube("accent", [-9, 64 + H, -9], [18, 2, 18]), cube("accent", [-3, 66 + H, -3], [6, 8, 6]), cube("glow", [-1, 74 + H, -1], [2, 3, 2])], "head"),
        bone("acc_thien_dien_4", [0, 40 + H, 0], [cube("glow" if i % 2 else "accent", [20 * math.cos(i * math.pi / 4) - 2, 36 + H, 20 * math.sin(i * math.pi / 4) - 2], [4, 10, 4]) for i in range(8)], "body"),
    ]
    return geo("hvf_spirit", [body, head, w1, w2, w3, rh, lh] + acc, 5, 6, 2.5)


# ------------------------------------------------------------------ animation
def fmt(t):
    return ("%g" % round(t, 3))


def wave(L, amp, phase=0.0, cycles=1, base=(0, 0, 0), steps=8):
    out = {}
    for i in range(steps + 1):
        t = L * i / steps
        k = math.sin(math.tau * (cycles * i / steps + phase))
        out[fmt(t)] = [round(base[j] + amp[j] * k, 3) for j in range(3)]
    return out


def kf(pts):
    return {fmt(t): list(v) for t, v in pts}


def anim(L, loop, bones):
    a = {"loop": bool(loop), "animation_length": L, "bones": {}}
    for name, ch in bones.items():
        a["bones"][name] = {k: v for k, v in ch.items()}
    return a


def anims(skel):
    A = {}
    if skel == "giant":
        A["idle"] = anim(3, True, {
            "body": {"position": wave(3, (0, 0.6, 0))}, "head": {"rotation": wave(3, (3, 0, 0), 0.2)},
            "right_arm": {"rotation": wave(3, (0, 0, -2))}, "left_arm": {"rotation": wave(3, (0, 0, 2))}})
        A["walk"] = anim(1, True, {
            "right_leg": {"rotation": wave(1, (30, 0, 0))}, "left_leg": {"rotation": wave(1, (-30, 0, 0))},
            "right_arm": {"rotation": wave(1, (-25, 0, 0))}, "left_arm": {"rotation": wave(1, (25, 0, 0))},
            "body": {"position": wave(1, (0, 1.2, 0), 0.25, 2), "rotation": wave(1, (0, 3, 0))}})
        A["attack"] = anim(0.8, False, {
            "right_arm": {"rotation": kf([(0, (0, 0, 0)), (0.2, (-110, 0, 12)), (0.4, (45, 0, -6)), (0.8, (0, 0, 0))])},
            "body": {"rotation": kf([(0, (0, 0, 0)), (0.2, (-6, 10, 0)), (0.4, (14, -8, 0)), (0.8, (0, 0, 0))])}})
        A["cast"] = anim(1.6, False, {
            "right_arm": {"rotation": kf([(0, (0, 0, 0)), (0.5, (-150, 0, -20)), (1.1, (-150, 0, -20)), (1.6, (0, 0, 0))])},
            "left_arm": {"rotation": kf([(0, (0, 0, 0)), (0.5, (-150, 0, 20)), (1.1, (-150, 0, 20)), (1.6, (0, 0, 0))])},
            "head": {"rotation": kf([(0, (0, 0, 0)), (0.5, (-22, 0, 0)), (1.1, (-22, 0, 0)), (1.6, (0, 0, 0))])}})
        A["phase"] = anim(2, False, {
            "right_arm": {"rotation": kf([(0, (0, 0, 0)), (0.6, (0, 0, -75)), (1.6, (0, 0, -75)), (2, (0, 0, 0))])},
            "left_arm": {"rotation": kf([(0, (0, 0, 0)), (0.6, (0, 0, 75)), (1.6, (0, 0, 75)), (2, (0, 0, 0))])},
            "head": {"rotation": kf([(0, (0, 0, 0)), (0.6, (-32, 0, 0)), (1.6, (-32, 0, 0)), (2, (0, 0, 0))])},
            "body": {"position": wave(2, (0.6, 0.6, 0), 0, 10)}})
    elif skel == "quad":
        tail = lambda L, k: {"tail1": {"rotation": wave(L, (0, 8 * k, 0))}, "tail2": {"rotation": wave(L, (0, 12 * k, 0), 0.15)}, "tail3": {"rotation": wave(L, (0, 16 * k, 0), 0.3)}}
        d = {"body": {"position": wave(3, (0, 0.5, 0))}, "head": {"rotation": wave(3, (4, 6, 0), 0.2)}}
        d.update(tail(3, 1))
        A["idle"] = anim(3, True, d)
        w = {"front_right": {"rotation": wave(0.9, (30, 0, 0))}, "back_left": {"rotation": wave(0.9, (30, 0, 0))},
             "front_left": {"rotation": wave(0.9, (-30, 0, 0))}, "back_right": {"rotation": wave(0.9, (-30, 0, 0))},
             "body": {"position": wave(0.9, (0, 1, 0), 0.25, 2)}}
        w.update(tail(0.9, 2))
        A["walk"] = anim(0.9, True, w)
        A["attack"] = anim(0.7, False, {
            "head": {"rotation": kf([(0, (0, 0, 0)), (0.2, (-25, 0, 0)), (0.35, (32, 0, 0)), (0.7, (0, 0, 0))])},
            "body": {"position": kf([(0, (0, 0, 0)), (0.3, (0, 0, -5)), (0.7, (0, 0, 0))])}})
        A["cast"] = anim(1.6, False, {
            "body": {"rotation": kf([(0, (0, 0, 0)), (0.5, (-22, 0, 0)), (1.1, (-22, 0, 0)), (1.6, (0, 0, 0))])},
            "head": {"rotation": kf([(0, (0, 0, 0)), (0.5, (-28, 0, 0)), (1.1, (-28, 0, 0)), (1.6, (0, 0, 0))])},
            "front_right": {"rotation": kf([(0, (0, 0, 0)), (0.5, (-55, 0, 0)), (1.1, (-55, 0, 0)), (1.6, (0, 0, 0))])},
            "front_left": {"rotation": kf([(0, (0, 0, 0)), (0.5, (-55, 0, 0)), (1.1, (-55, 0, 0)), (1.6, (0, 0, 0))])}})
        A["phase"] = anim(2, False, {
            "body": {"rotation": kf([(0, (0, 0, 0)), (0.6, (-36, 0, 0)), (1.6, (-36, 0, 0)), (2, (0, 0, 0))]), "position": wave(2, (0.5, 0, 0), 0, 10)},
            "head": {"rotation": kf([(0, (0, 0, 0)), (0.6, (-30, 0, 0)), (1.6, (-30, 0, 0)), (2, (0, 0, 0))])},
            "front_right": {"rotation": kf([(0, (0, 0, 0)), (0.6, (-70, 0, 0)), (1.6, (-70, 0, 0)), (2, (0, 0, 0))])},
            "front_left": {"rotation": kf([(0, (0, 0, 0)), (0.6, (-70, 0, 0)), (1.6, (-70, 0, 0)), (2, (0, 0, 0))])}})
    elif skel == "worm":
        A["idle"] = anim(3, True, {
            "neck1": {"rotation": wave(3, (4, 0, 0))}, "neck2": {"rotation": wave(3, (6, 0, 0), 0.15)}, "head": {"rotation": wave(3, (8, 4, 0), 0.3)},
            "tail1": {"rotation": wave(3, (0, 6, 0))}, "tail2": {"rotation": wave(3, (0, 9, 0), 0.15)}, "tail3": {"rotation": wave(3, (0, 12, 0), 0.3)}, "tail4": {"rotation": wave(3, (0, 16, 0), 0.45)}})
        A["walk"] = anim(1, True, {
            "tail1": {"rotation": wave(1, (0, 15, 0))}, "tail2": {"rotation": wave(1, (0, 25, 0), 0.15)}, "tail3": {"rotation": wave(1, (0, 35, 0), 0.3)}, "tail4": {"rotation": wave(1, (0, 40, 0), 0.45)},
            "body": {"position": wave(1, (0, 0.8, 0), 0, 2), "rotation": wave(1, (0, 6, 0))},
            "neck1": {"rotation": wave(1, (5, 0, 4), 0.1)}, "neck2": {"rotation": wave(1, (6, 0, 6), 0.25)}, "head": {"rotation": wave(1, (6, 0, 0), 0.4)}})
        A["attack"] = anim(0.8, False, {
            "neck1": {"rotation": kf([(0, (0, 0, 0)), (0.25, (-18, 0, 0)), (0.4, (34, 0, 0)), (0.8, (0, 0, 0))])},
            "neck2": {"rotation": kf([(0, (0, 0, 0)), (0.25, (-22, 0, 0)), (0.4, (38, 0, 0)), (0.8, (0, 0, 0))])},
            "head": {"rotation": kf([(0, (0, 0, 0)), (0.25, (-25, 0, 0)), (0.4, (45, 0, 0)), (0.8, (0, 0, 0))])}})
        A["cast"] = anim(1.6, False, {
            "neck1": {"rotation": kf([(0, (0, 0, 0)), (0.5, (-22, 0, 0)), (1.1, (-22, 0, 0)), (1.6, (0, 0, 0))])},
            "neck2": {"rotation": kf([(0, (0, 0, 0)), (0.5, (-30, 0, 0)), (1.1, (-30, 0, 0)), (1.6, (0, 0, 0))])},
            "head": {"rotation": kf([(0, (0, 0, 0)), (0.5, (-24, 0, 0)), (1.1, (-24, 0, 0)), (1.6, (0, 0, 0))])},
            "tail2": {"rotation": wave(1.6, (0, 30, 0), 0, 3)}})
        A["phase"] = anim(2, False, {
            "neck1": {"rotation": wave(2, (0, 0, 10), 0, 6)}, "neck2": {"rotation": wave(2, (-25, 0, 12), 0, 6)}, "head": {"rotation": kf([(0, (0, 0, 0)), (0.6, (-35, 0, 0)), (1.6, (-35, 0, 0)), (2, (0, 0, 0))])},
            "tail3": {"rotation": wave(2, (0, 40, 0), 0, 5)}})
    else:  # spirit
        orbit = {"rotation": kf([(0, (0, 0, 0)), (4, (0, 360, 0))])}
        ring = {"rotation": kf([(0, (0, 0, 0)), (4, (0, -360, 0))])}
        A["idle"] = anim(4, True, {
            "body": {"position": wave(4, (0, 2, 0))}, "head": {"rotation": wave(4, (0, 5, 0), 0.1)},
            "wisp1": {"rotation": wave(4, (5, 0, 6))}, "wisp2": {"rotation": wave(4, (6, 0, 9), 0.15)}, "wisp3": {"rotation": wave(4, (7, 0, 12), 0.3)},
            "right_hand": {"position": wave(4, (0, 1.5, 0), 0.2)}, "left_hand": {"position": wave(4, (0, 1.5, 0), 0.7)},
            "acc_bang_phach_2": orbit, "acc_thien_dien_4": ring})
        A["walk"] = anim(1.6, True, {
            "body": {"position": wave(1.6, (0, 2.5, 0)), "rotation": wave(1.6, (0, 0, 0), 0, 1, (12, 0, 0))},
            "wisp1": {"rotation": wave(1.6, (14, 0, 8))}, "wisp2": {"rotation": wave(1.6, (18, 0, 12), 0.15)}, "wisp3": {"rotation": wave(1.6, (22, 0, 16), 0.3)},
            "right_hand": {"position": wave(1.6, (0, 2, 3), 0.2)}, "left_hand": {"position": wave(1.6, (0, 2, 3), 0.7)},
            "acc_bang_phach_2": {"rotation": kf([(0, (0, 0, 0)), (1.6, (0, 360, 0))])}, "acc_thien_dien_4": {"rotation": kf([(0, (0, 0, 0)), (1.6, (0, -360, 0))])}})
        A["attack"] = anim(0.8, False, {
            "right_hand": {"position": kf([(0, (0, 0, 0)), (0.2, (0, 6, 6)), (0.4, (0, 0, -14)), (0.8, (0, 0, 0))])},
            "body": {"rotation": kf([(0, (0, 0, 0)), (0.2, (-8, 0, 0)), (0.4, (18, 0, 0)), (0.8, (0, 0, 0))])}})
        A["cast"] = anim(1.6, False, {
            "right_hand": {"position": kf([(0, (0, 0, 0)), (0.5, (0, 14, 0)), (1.1, (0, 14, 0)), (1.6, (0, 0, 0))])},
            "left_hand": {"position": kf([(0, (0, 0, 0)), (0.5, (0, 14, 0)), (1.1, (0, 14, 0)), (1.6, (0, 0, 0))])},
            "head": {"rotation": kf([(0, (0, 0, 0)), (0.5, (-20, 0, 0)), (1.1, (-20, 0, 0)), (1.6, (0, 0, 0))])},
            "body": {"position": kf([(0, (0, 0, 0)), (0.5, (0, 6, 0)), (1.1, (0, 6, 0)), (1.6, (0, 0, 0))])}})
        A["phase"] = anim(2, False, {
            "body": {"rotation": kf([(0, (0, 0, 0)), (2, (0, 360, 0))]), "position": kf([(0, (0, 0, 0)), (0.8, (0, 10, 0)), (1.4, (0, 10, 0)), (2, (0, 0, 0))])},
            "right_hand": {"position": kf([(0, (0, 0, 0)), (0.8, (0, 12, 0)), (1.4, (0, 12, 0)), (2, (0, 0, 0))])},
            "left_hand": {"position": kf([(0, (0, 0, 0)), (0.8, (0, 12, 0)), (1.4, (0, 12, 0)), (2, (0, 0, 0))])}})
    return {"format_version": "1.8.0", "animations": {"animation.hvf." + k: v for k, v in A.items()}}


# ------------------------------------------------------------------ texture
# moi dang: (skin, dark, accent, light, glow, pattern)
FORMS = {
    "huyet_ma": ((150, 22, 38), (46, 10, 22), (190, 60, 60), (230, 200, 190), (255, 40, 60), "veins"),
    "moc_yeu": ((96, 66, 38), (48, 34, 22), (66, 150, 52), (150, 190, 90), (120, 255, 90), "bark"),
    "cot_long": ((214, 206, 184), (110, 100, 86), (80, 60, 52), (246, 240, 224), (255, 220, 120), "cracks"),
    "doc_chuong": ((88, 106, 52), (40, 48, 30), (122, 70, 140), (180, 210, 120), (150, 255, 70), "blotch"),
    "sa_tran": ((196, 158, 96), (110, 84, 52), (222, 190, 120), (246, 226, 170), (255, 190, 80), "ripple"),
    "hai_uyen": ((30, 96, 120), (14, 44, 66), (232, 110, 90), (150, 230, 230), (80, 230, 255), "scales"),
    "bang_phach": ((150, 210, 236), (70, 110, 150), (200, 240, 255), (240, 252, 255), (150, 240, 255), "facets"),
    "thien_dien": ((236, 232, 224), (40, 36, 52), (214, 170, 70), (255, 250, 236), (255, 216, 102), "glyph"),
}


def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c)


def paint_mat(px, mi, base, pattern, rnd, alt):
    ox = mi * 32
    for y in range(32):
        for x in range(32):
            k = 0.88 + 0.24 * rnd.random()
            col = shade(base, k)
            if pattern == "veins" and (math.sin(x * 0.7 + y * 0.31) + math.sin(y * 0.9 - x * 0.2)) > 1.45:
                col = shade(alt, 0.8 + 0.2 * rnd.random())
            elif pattern == "bark" and (x % 6 in (0, 1) or rnd.random() < 0.04):
                col = shade(base, 0.55)
            elif pattern == "bark" and rnd.random() < 0.05:
                col = alt
            elif pattern == "cracks" and ((x * 3 + y * 2) % 23 == 0 or (x - y * 2) % 29 == 0):
                col = shade(alt, 0.9)
            elif pattern == "blotch" and math.sin(x * 0.45) * math.cos(y * 0.5) > 0.55:
                col = shade(alt, 0.7 + 0.3 * rnd.random())
            elif pattern == "ripple" and int(math.sin(y * 0.55 + math.sin(x * 0.2) * 2) * 2) == 1:
                col = shade(alt, 0.95)
            elif pattern == "scales" and ((x % 8) - 4) ** 2 + (((y + (4 if (x // 8) % 2 else 0)) % 8) - 4) ** 2 > 14:
                col = shade(base, 0.62)
            elif pattern == "facets" and ((x + y) % 11 == 0 or (x - y) % 13 == 0):
                col = shade(alt, 1.0)
            elif pattern == "glyph" and ((x % 8 == 3 and 4 < y % 16 < 12) or (y % 16 == 8 and 2 < x % 8 < 6)):
                col = shade(alt, 0.95)
            px[ox + x, y] = col + (255,)


def draw_face(px, mi, form, kind, glow, dark, light):
    """Mat nguoi 16x16 ve vao goc vung 32x32 (cube 'north' lay 32x32, nen ve ca 32 cho sat)."""
    ox = mi * 32
    for y in range(32):
        for x in range(32):
            px[ox + x, y] = px[ox + x, y]
    def box(x0, y0, x1, y1, c):
        for y in range(y0, y1):
            for x in range(x0, x1):
                px[ox + x, y] = c + (255,)
    box(0, 0, 32, 32, shade(dark, 0.9) if kind != "face" else px[ox + 2, 2][:3])
    if form == "thien_dien":
        skin = (236, 232, 224)
        box(2, 2, 30, 30, skin)
        if kind == "face":      # Tu Bi: mat nham, mim cuoi
            box(6, 12, 13, 14, dark); box(19, 12, 26, 14, dark); box(11, 22, 21, 24, (200, 90, 90)); box(14, 5, 18, 8, glow)
        elif kind == "face2":   # Phan No: mat do lua
            box(5, 10, 14, 15, glow); box(18, 10, 27, 15, glow); box(7, 8, 15, 10, dark); box(17, 8, 25, 10, dark); box(8, 22, 24, 26, dark); box(10, 22, 12, 25, light); box(20, 22, 22, 25, light)
        else:                   # Hu Vo: khong mat, mot khe sang
            box(14, 4, 18, 28, glow); box(8, 14, 24, 17, shade(glow, 0.6))
        return
    # dang thuong: ho mat toi, mat sang, mieng rang
    base = tuple(px[ox + 2, 2][:3])
    box(2, 2, 30, 30, shade(base, 0.8))
    box(5, 10, 13, 15, glow); box(19, 10, 27, 15, glow)
    box(7, 11, 11, 14, (255, 255, 255)); box(21, 11, 25, 14, (255, 255, 255))
    box(7, 21, 25, 25, dark)
    for i in range(8, 25, 4):
        box(i, 21, i + 2, 24, light)
    if form in ("cot_long", "bang_phach"):
        box(14, 14, 18, 20, shade(dark, 1.0))


def make_textures():
    rnd0 = random.Random(7)
    for fid, (skin, dark, accent, light, glow, pat) in FORMS.items():
        rnd = random.Random(hash(fid) & 0xFFFF or 1)
        rnd = random.Random(sum(map(ord, fid)))
        im = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
        px = im.load()
        gm = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
        gp = gm.load()
        mats = {"skin": skin, "dark": dark, "accent": accent, "light": light, "glow": glow}
        for name, col in mats.items():
            paint_mat(px, MI[name], col, pat if name in ("skin", "accent") else "none", rnd, accent if name == "skin" else skin)
        for name in ("face", "face2", "face3"):
            paint_mat(px, MI[name], skin, "none", rnd, accent)
            draw_face(px, MI[name], fid, name, glow, dark, light)
        # glow mat: sang that, co van
        for y in range(32):
            for x in range(32):
                k = 0.8 + 0.2 * math.sin((x + y) * 0.5)
                c = shade(glow, k) + (255,)
                px[MI["glow"] * 32 + x, y] = c
                gp[MI["glow"] * 32 + x, y] = c
        # mat sang tren mat: lay lai tu texture
        for name in ("face", "face2", "face3"):
            ox = MI[name] * 32
            for y in range(32):
                for x in range(32):
                    r, g, b, _ = px[ox + x, y]
                    if (r, g, b) in (glow, shade(glow, 0.6)):
                        gp[ox + x, y] = (r, g, b, 255)
        # vung dac / sang cho gai & vanh pha (gen_boss_phase ve de len, o day ve ban dau)
        for y in range(200, 256):
            for x in range(0, 64):
                px[x, y] = shade(dark, 0.9) + (255,)
            for x in range(64, 128):
                px[x, y] = glow + (255,)
                gp[x, y] = glow + (255,)
        # vien sang nhe tren accent (hoa van)
        for y in range(32):
            for x in range(32):
                if (x * 7 + y * 13) % 41 == 0:
                    gp[MI["accent"] * 32 + x, y] = glow + (255,)
        im.save(os.path.join(TEX, f"hvf_{fid}.png"))
        gm.save(os.path.join(TEX, f"hvf_{fid}_glowmask.png"))
        print("tex", fid)


def main():
    for name, fn in (("giant", giant), ("quad", quad), ("worm", worm), ("spirit", spirit)):
        with open(os.path.join(GEO, f"hvf_{name}.geo.json"), "w", encoding="utf-8") as f:
            json.dump(fn(), f, indent=1)
            f.write("\n")
        with open(os.path.join(ANI, f"hvf_{name}.animation.json"), "w", encoding="utf-8") as f:
            json.dump(anims(name), f, indent=1)
            f.write("\n")
        print("geo+anim", name)
    make_textures()
    return subprocess.call([sys.executable, os.path.join(HERE, "gen_boss_phase.py")])


if __name__ == "__main__":
    sys.exit(main())
