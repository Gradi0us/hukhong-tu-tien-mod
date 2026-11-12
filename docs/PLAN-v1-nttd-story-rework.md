# PLAN-v1 — Làm lại cutscene & đồng minh tuyến Nguyên Thủy Tịch Diệt (NTTD)

**Created**: 2026-11-12
**Last Updated**: 2026-11-12 — v1 khảo sát code + kế hoạch (chưa sửa code)
**Status**: ⬜ Chưa làm
**Phạm vi**: Lv10Cutscene, Dạ Khuyết (DK) đồng minh pha 3, Mộ Dao đồng minh pha 4, cutscene cải tà DK, cutscene hy sinh (grandSacrifice), cutscene kết (NttdEnding).

---

## 1. Key Findings

| # | Phát hiện | Bằng chứng (file:line) | Khoảng trống |
|---|-----------|------------------------|--------------|
| F1 | Lv10: bóng NTTD **đứng yên** (`setNoAi(true)`), spawn sẵn cách 6 block, chỉ gọi `triggerAnim("skill","f1_attack")` 2 lần | `core/Lv10Cutscene.java:107-113, 177-191` | Không bay vào, không lướt, không chém thật; "va chạm" chỉ là particle vanilla tại 1 điểm cố định |
| F2 | Lv10: Mộ Dao **hiện ra tại chỗ** (`spawnMo` t=26, `setNoAi`), không anim nào ngoài idle | `core/Lv10Cutscene.java:193-204` | Không lao vào, không rút kiếm, không đỡ |
| F3 | Lv10 camera chỉ 4 key, neo vào bóng, không có cảnh cận/slow-mo | `core/Lv10Cutscene.java:121-127` | Thiếu shot: over-shoulder, cận tay rút kiếm, cận va chạm |
| F4 | Model Mộ Dao chỉ có 6 bone: `right_leg left_leg body head right_arm left_arm`; anim chỉ `idle walk talk` | `assets/hukhong/geo/entity/npc_mo_dao.geo.json`, `animations/entity/npc_mo_dao.animation.json` | **Không có kiếm, không có vỏ kiếm, không có anim chiến đấu** |
| F5 | `StoryNpc` chỉ có controller `move` (walk/idle), không có controller trigger | `story/npc/StoryNpc.java:47-52` | Không thể `triggerAnim` cho Mộ Dao |
| F6 | DK có model + anim rất đầy đủ (≈40 anim: `thrust flurry rise dive dash blink backflip combo_*`; bone `weapon_katana crescent moon_wings`) | `animations/entity/da_khuyet.animation.json` | Đủ dùng, không cần vẽ thêm cho DK |
| F7 | DK đồng minh **tắt brain**: `brainProfile()` trả `null` khi `ally`; `tick()` bỏ `BossBrain.tickFor` khi `ally`; chiêu riêng bị `endMove()` khi `ally` | `endgame/DaKhuyet.java:545, 489-494` | Đây là lý do DK chỉ đánh 1-2 kiểu |
| F8 | DK đồng minh chỉ có `allyAttack` 3 nhánh (Nguyệt Luân / lướt chém teleport / tia) + hồi máu, cd 45-70 tick, đứng xa | `endgame/DaKhuyet.java:337-411` | Không dùng combo brain, không dùng `DaKhuyetMoves` |
| F9 | `BossBrain.strike` đã cho phép đánh mục tiêu không phải Player (`e != mob.getTarget()` mới bỏ qua) **nhưng vẫn đánh mọi Player** | `boss/ai/BossBrain.java:333` | Cần bộ lọc: đồng minh không được đánh người chơi |
| F10 | `BossBrain.tick` chỉ chạy khi `mob.getTarget()` sống và trong 48 block | `boss/ai/BossBrain.java:167-171` | OK cho NTTD nếu setTarget(NTTD) |
| F11 | `DaKhuyetMoves` gây sát thương qua `players(sl,c,r)` / `hitAround` → **chỉ trúng Player** | `endgame/DaKhuyetMoves.java:237, 269, 296, 319, 374, 385-397` | Cần "đổi phe": khi `ally`, quét `mTarget` (NTTD) thay vì Player |
| F12 | `DaKhuyet.strike` có lifesteal; nhánh ally đã bỏ lifesteal | `endgame/DaKhuyet.java:463-470` | Chỗ móc scale sát thương |
| F13 | Mộ Dao pha 4: `moTick` **teleport** (`place`) và bắn ribbon + `boss.hurt(0.4%)`, hoặc hồi máu mỗi beat thứ 4 | `endgame/NttdAllies.java:66-100`, gọi từ `NguyenThuyTichDiet.java:1177` | Đứng yên "ping", không di chuyển, không anim |
| F14 | Cải tà DK: chỉ `triggerAnim("phase")` + 4 key camera xoay quanh + particle bụi trăng | `endgame/DaKhuyet.java:142-170, 181-205` | Không có hành động: không quỳ (không có anim quỳ), không buông kiếm, không có khoảnh khắc trăng đổi màu |
| F15 | Hy sinh (grandSacrifice): đã có beat theo tick (`later(...)`) khá tốt, nhưng DK chỉ `teleportTo` + `rise`, rồi `discard()`; Mộ Dao spawn t=212 không có màn vào | `endgame/NguyenThuyTichDiet.java:1109-1168` | Thiếu anim NTTD tụ chiêu, DK quay đầu nhìn người chơi, Mộ Dao lao vào |
| F16 | Ending: camera 14 key tốt, nhưng Mộ Dao chỉ spawn đứng (t=320), NTTD không có anim sụp đổ (dùng particle) | `endgame/NttdEnding.java` (`step`, t=0..320) | Thiếu anim `death` cho NTTD trong ending, Mộ Dao tra kiếm vào vỏ |
| F17 | NTTD form1 có `leap teleport attack slam combo throw`, bone `hammer void_core cape`; form2 có `charge crush dive_under emerge star_beam` | `animations/entity/nttd_form1.animation.json`, `nguyen_thuy_tich_diet.animation.json` | Thiếu `fly_in` (lơ lửng lao ngang) cho bóng Lv10 — vẽ mới hoặc ghép `leap`+`teleport` |
| F18 | Hạ tầng sẵn: `CamPath.Key(t,dx,dy,dz,yaw,pitch,fov,lookH)` nội suy cubic; `sendCutscene(..., at, yaw, ...)` neo theo điểm cố định; Fx: `slash qi ribbon ribbonHelix swordFly giantSword shake telegraphLine/Cone groundCrack elem(E_ARC/E_HALOS/E_NUKE) shell rune` | `boss/phase/CamPath.java:13`, `BossPhaseDirector.java:139-148`, `vfx/Fx.java:30-185` | Không cần hệ thống mới; thiếu "hit-stop"/slow-mo phía client (tùy chọn) |

**Kết luận**: gốc vấn đề không phải thiếu hạ tầng mà là (a) Mộ Dao không có model/anim chiến đấu, (b) DK đồng minh bị tách khỏi BossBrain + DaKhuyetMoves, (c) các cutscene viết bằng "spawn đứng yên + particle" thay vì di chuyển thực thể theo beat.

---

## 2. Quyết định kiến trúc

1. **Thực thể chiến đấu riêng cho Mộ Dao**: `endgame/MoDaoCombat.java` (extends `PathfinderMob`, `GeoEntity`, `BossBrain.Host`). Không nhét vào `StoryNpc` (StoryNpc dùng chung cho mọi NPC, có hội thoại/quest). Model mới `npc_mo_dao_combat.geo.json` = copy `npc_mo_dao.geo.json` + bone kiếm/vỏ. Dùng chung texture `npc_mo_dao.png` (mở rộng canvas nếu thiếu chỗ UV cho kiếm — kiểm tra kích thước khi làm bước A1).
   - Dùng cho cả 3 nơi: Lv10, pha 4, ending. `NttdAllies.spawnMo/findMo/dismissMo` đổi sang `MoDaoCombat` (giữ `MO_TAG`, `DaKhuyet.isStoryAlly` đã dựa vào tag nên không đổi).
2. **"Chế độ phe đồng minh" chung cho BossBrain**: thêm 2 hook mặc định vào `BossBrain.Host`:
   - `default boolean brainCanHit(LivingEntity e) { return true; }` — `BossBrain.strike` (line 333) gọi thêm: `if (!pb.brainCanHit(e)) continue;`. Đồng minh trả `e == target` (chỉ NTTD + Hư mob, không bao giờ Player).
   - Sát thương: đồng minh override `brainStrike(victim, src, dmg)` → bỏ `dmg` gốc, dùng `victim.getMaxHealth() * frac` (xem §4.3).
   - `BossBrain.scanThreats/onHeal/punishHeal/counter` đều so với `mob.getTarget()` → tự vô hiệu khi target là NTTD (Player không bao giờ là target). Cần kiểm thêm `BossBrain.java:463` (quét Player trong 5.5 block — đọc kỹ khi làm, nếu là "đẩy lùi người chơi áp sát" thì chặn bằng `brainCanHit`).
3. **DaKhuyetMoves đổi phe**: thay `players(sl,c,r)` bằng `victims(sl, d, c, r)`: nếu `d.ally()` trả `[d.mTarget]` (+ Hư mob trong r) khi nằm trong vùng, ngược lại trả Player như cũ. Mọi chỗ `d.strike(p, ...)` giữ nguyên, `strike` tự scale khi ally. Vũng máu (`tickPools`) cũng đi qua `victims`.

---

## 3. Mục 1 — Lv10 Cutscene làm lại ("Hóa thân Tịch Diệt bay tới chém, Mộ Dao rút kiếm đỡ")

Tổng: **TICKS = 340** (~17s). Người chơi vẫn khóa tại `origin` như cũ. Gốc toạ độ cutscene: `sendCutscene(sl, null, origin, yawPlayer, ...)` (overload neo điểm, `BossPhaseDirector.java:148`) — camera **không** neo vào bóng nữa vì bóng di chuyển. `u` = hướng nhìn người chơi; `r` = vector phải.

### 3.1 Beat-by-beat

| Tick | Sự kiện | Thực thể / anim | VFX / âm |
|------|---------|-----------------|----------|
| 0 | Trời tối, gió Hư Không | — | `Fx.rune` trên trời tại origin+u*20+y12, tím; tiếng `WARDEN_EMERGE` xa |
| 10 | Vết nứt mở ở xa | — | `Fx.rift(origin+u*28+y6, dọc, 4, VOIDC, 60)` |
| 24 | **Bóng NTTD (form1) chui ra khỏi vết nứt**, lơ lửng | spawn tại origin+u*28+y6, `setNoGravity(true)`, anim `f1_fly_in` (mới) / tạm `f1_teleport` | `REVERSE_PORTAL` 60 hạt, `Fx.shake` 0.4 |
| 24-64 | **Bay vào** theo đường cong (Bezier: start → origin+u*14+r*4+y5 → origin+u*6+y2.5), tốc độ tăng dần (ease-in) | server `moveTo` mỗi tick theo đường, yaw hướng người chơi | vệt `Fx.ribbon` tím mỗi 4 tick sau lưng, `PORTAL` |
| 64 | Lấy đà: giơ búa trên đầu, khựng 8 tick | anim `f1_attack` (đoạn giơ) hoặc mới `f1_overhead_windup` | `Fx.telegraphLine(origin+u*6 → origin, rộng 2, 16t, đỏ)`, âm `WARDEN_SONIC_CHARGE` |
| 50 | (song song) **Mộ Dao lao vào từ bên phải người chơi** | spawn `MoDaoCombat` tại origin−u*4+r*8, anim `dash` | vệt `Fx.ribbon` xanh `MO_COLOR` |
| 50-66 | Mộ Dao chạy/lướt tới điểm chắn origin+u*2.5 | `moveTo` nội suy 16 tick | bụi `CLOUD` dưới chân |
| 66 | **Rút kiếm**: tay phải nắm chuôi bên hông trái, kéo ra | anim `draw_sword` (mới, 10 tick): bone `sword` đổi parent visibility từ `sheath` sang `right_arm` (xem §6) | `Fx.slash` nhỏ trắng theo đường rút, âm `TRIDENT_RETURN` + `ANVIL_LAND` pitch 2.0 |
| 72 | **Nhát 1: bóng bổ búa xuống** | `f1_attack` (đoạn vung) + bóng lao 3 block tới (moveTo nhanh) | — |
| 74 | **Va chạm 1 — Mộ Dao đỡ ngang kiếm** | Mộ Dao anim `parry_high`, lùi trượt 1.5 block về phía người chơi trong 6 tick | `clash()` mới: `Fx.elem(E_ARC)`, `SWEEP_ATTACK`, `CRIT` 40, `Fx.shake 1.0`, `groundCrack` dưới chân Mộ Dao, âm `ANVIL_LAND` + `GENERIC_EXPLODE`; **hit-stop 4 tick** (đóng băng cả 2 thực thể: không moveTo, anim tốc 0 nếu làm phần client tùy chọn) |
| 84 | Bóng lùi, xoay người quét ngang | `f1_combo` (đoạn 1) | `Fx.telegraphCone` |
| 92 | **Va chạm 2 — Mộ Dao đỡ thấp + phản đòn** | Mộ Dao `parry_low` → `counter_slash` | `Fx.slash` xanh lớn qua thân bóng, `END_ROD` 30, shake 0.8 |
| 100 | Bóng bị đẩy lùi 4 block, lơ lửng lại | moveTo lùi, `f1_backstep` (BRAIN alias) | `Fx.ribbon` tím đứt quãng |
| 100-200 | Đối thoại (phụ đề giữ nguyên key cũ, dời mốc) — hai bên **đối đầu**: Mộ Dao thế thủ (`guard_idle` loop), bóng lơ lửng lắc lư (`f1_idle`), sương | — | `PORTAL` quanh bóng, ánh kiếm `END_ROD` dọc lưỡi kiếm mỗi 6 tick |
| 200 | Bóng giơ búa lần 3, tụ năng lượng | `f1_ult` (đoạn tụ) | `Fx.rune` dưới bóng, `void_core` sáng |
| 214 | **Mộ Dao chém trước: Thanh Minh Nhất Tuyến** — lướt xuyên qua bóng ra sau lưng nó | Mộ Dao `dash_slash`, moveTo từ trước bóng ra sau bóng 6 block trong 4 tick | `Fx.ribbon` thẳng xanh dài, `Fx.giantSword`? (không, quá lớn) → `Fx.slash` cuộn 3 lớp, shake 1.2 |
| 222 | Vết chém hiện trên bóng, bóng nứt | bóng `f1_struggle` | `Fx.rift` ngang thân bóng, `SOUL` |
| 236 | Bóng tan (giữ `dissolveShade`) + phụ đề NTTD.3 | — | `REVERSE_PORTAL` 60, `Fx.elem(E_NUKE nhỏ 2f, tím)` |
| 250 | Bóng discard | — | — |
| 256 | **Mộ Dao vẩy kiếm, tra kiếm vào vỏ** | `sheathe_sword` (mới, 14 tick) | âm `ARMOR_EQUIP_IRON`, tia sáng nhỏ ở vỏ lúc chạm |
| 270-310 | Mộ Dao quay về người chơi, nói câu cuối | `talk` (có sẵn) | — |
| 316 | Mộ Dao biến mất | — | `END_ROD` 40 + `Fx.groundRing` xanh |
| 340 | Kết thúc, gỡ hiệu ứng | — | — |

### 3.2 Camera keys (toạ độ tương đối origin, trục: dz = về phía trước người chơi, dx = phải)

| Tick | Shot | dx | dy | dz | yaw | pitch | fov | lookH |
|------|------|----|----|----|-----|-------|-----|-------|
| 0 | Over-shoulder sau lưng người chơi nhìn vết nứt xa | 0.8 | 2.0 | −2.5 | 0 | −4 | 60 | 4 |
| 40 | Theo bóng bay tới, hạ thấp | 1.5 | 1.6 | −1.5 | 0 | 0 | 66 | 3 |
| 58 | Cận Mộ Dao lao vào (bên phải) | 5.0 | 1.4 | 1.0 | −60 | 0 | 55 | 1.5 |
| 66 | **Cận tay rút kiếm** (thấp, hông) | 2.5 | 1.0 | 3.2 | −100 | 4 | 40 | 1.0 |
| 74 | Va chạm 1, ngang 2 bên | 4.5 | 1.8 | 4.0 | −90 | 0 | 50 | 1.8 |
| 92 | Va chạm 2, góc thấp đối diện | −4.0 | 0.6 | 4.5 | 90 | −8 | 52 | 1.6 |
| 110 | Hai bên đối đầu, rộng | 0 | 2.6 | −3.5 | 0 | 0 | 70 | 2.0 |
| 160 | Xoay chậm sang trái | −3.5 | 2.0 | 1.0 | 40 | 0 | 62 | 2.0 |
| 210 | Cận Mộ Dao trước cú lướt | 1.2 | 1.6 | 1.0 | 180 | 0 | 45 | 1.6 |
| 218 | Theo cú lướt, góc cao | 0 | 5.0 | 5.0 | 0 | 30 | 70 | 1.5 |
| 236 | Bóng tan, rộng | 0 | 2.2 | 1.0 | 0 | 0 | 68 | 3 |
| 260 | Cận tra kiếm | 1.8 | 1.3 | 3.0 | −120 | 0 | 42 | 1.2 |
| 300 | Mộ Dao + người chơi | 2.5 | 1.8 | 5.5 | −160 | 0 | 58 | 1.6 |
| 340 | Trả về sau lưng người chơi | 0 | 1.7 | −0.2 | 0 | 0 | 70 | 1.6 |

> Kiểm tra ý nghĩa `yaw/pitch` trong `CamPath.Key` khi neo điểm (là offset hay tuyệt đối) bằng cách đọc `CutsceneClient` trước khi chốt số — bảng trên giả định yaw tương đối hướng `u`.

### 3.3 Anim cần

| Thực thể | Anim | Trạng thái | Bone |
|----------|------|-----------|------|
| NTTD form1 | `fly_in` (lơ lửng, người ngả về trước, búa kéo sau, cape bay) | **Mới** | root, body, cape, right_arm, hammer, left_arm, legs (co) |
| NTTD form1 | `attack`, `combo`, `backstep`, `struggle`, `ult`, `idle` | Có sẵn | — |
| Mộ Dao | `dash`, `draw_sword`, `parry_high`, `parry_low`, `counter_slash`, `guard_idle`, `dash_slash`, `sheathe_sword` | **Mới** | xem §6 |
| Mộ Dao | `idle walk talk` | Có sẵn | — |

### 3.4 Code
- `core/Lv10Cutscene.java`: viết lại `start/step`; thay `shade.setNoAi + đứng` bằng điều khiển vị trí mỗi tick (`moveTo` theo `Path` nội suy); thay `StoryNpc` bằng `MoDaoCombat`; `clash()` nhận vị trí động; thêm helper `beatMove(Entity, from, to, t0, t1, ease)`.
- Bóng NTTD cần đăng ký `f1_fly_in` trong `NguyenThuyTichDiet.registerControllers` (line 1744: thêm vào mảng f1_).
- Giữ nguyên hàng rào an toàn: logout/die/orphan cleanup (thêm TAG cho MoDaoCombat).

---

## 4. Mục 2 — Dạ Khuyết đồng minh dùng ĐỦ moveset qua BossBrain

### 4.1 Cách làm
1. `DaKhuyet.brainProfile()` (line 545): `return DaKhuyetBrain.PROFILE;` (bỏ điều kiện `ally`), nhưng khi ally dùng `brainPhaseVals` riêng: `DaKhuyetBrain.ALLY` = tốc độ như phase 3, `rest` dài hơn ×1.3 (để người chơi là "nhân vật chính").
2. `DaKhuyet.tick()` (line 489-494): khi `ally && allyIntro == 0 && guarding == 0` → `BossBrain.tickFor(sl, this, this)` + `brainFollowUps(sl)`; bỏ dòng `if (ally ...) endMove()` (line 493) — chỉ `endMove()` khi `guarding > 0` hoặc `standDown`.
3. `customServerAiStep` (line 502): `allyStep` chỉ còn lo: chọn target (`allyTarget` giữ nguyên, set mỗi 20 tick), intro, guard, **Nguyệt Quang hồi máu** mỗi ~12s (giữ, chuyển thành timer riêng thay vì beat). Xoá `allyAttack` (hoặc giữ làm fallback khi target ở >24 block: Nguyệt Luân tầm xa).
4. `frozen()` của DK khi ally: trả `allyIntro > 0 || guarding > 0 || redeeming`.
5. Host hook: `brainCanHit(e)` → `!ally || (e == getTarget() || e instanceof HuMob)`; tuyệt đối không Player, không `MoDaoCombat`.
6. `brainBigMove` (line 551): khi ally không lọc `aeoniaCd`/khoảng cách 24 theo bbox NTTD — dùng `distanceTo(t) - t.getBbWidth()/2`. `DaKhuyetMoves.pick` giữ nguyên ngẫu nhiên theo `phase()` (đặt phase = 3 khi ally để mở hết chiêu, gồm `dk_huyet_hoa`).
7. `DaKhuyetMoves`: `players()` → `victims()` (§2.3). `tpos(d)` (line 99) đã dựa `mTarget` → nhắm NTTD tự động. Kiểm `begin()` line 92 (thông báo Player — đổi sang thông báo trung lập/ẩn khi ally). `ultScene`/`deathScene` không gọi khi ally.
8. NTTD to (bbox lớn): `reach` trong brain cộng `mob.getBbWidth()*0.5` nhưng trừ `e.getBbWidth()*0.5` → đã ổn. `GapClose` của DK (dash/blink) dừng ở khoảng cách tính theo tâm → thêm offset `t.getBbWidth()*0.5` trong `DaKhuyetBrain.gapClose` khi target không phải Player.

### 4.2 Né đòn NTTD
`BossBrain.scanThreats` (line 373) chỉ né khi có threat gần target; NTTD đánh người chơi nên DK ít né. Thêm: khi NTTD bắt đầu chiêu lớn (`NguyenThuyTichDiet` đã gọi `BossBrain.notifySkillCast(sl, at, r)` line 481?) → DK có brain sẽ nhận và `EVADE` (backflip/sidestep có anim sẵn) → combo nối Nguyệt Luân như boss thật.

### 4.3 Sát thương lên NTTD
`DaKhuyet.strike(victim, dmg)` khi `ally && victim instanceof NguyenThuyTichDiet`: `dmg = victim.getMaxHealth() * ALLY_FRAC * (mult/1.0)` với `ALLY_FRAC = 0.0025` cho đòn thường, ×`DaKhuyetMoves.mult(mv)` cho chiêu, **trần 0.8%/đòn**, **trần tổng 12% máu NTTD trong cả pha 3** (đếm trong DK, hết trần thì damage = 0 nhưng vẫn VFX). Hư mob: 8% máu tối đa như cũ. Không lifesteal; thay bằng hồi cho người chơi 1% máu khi trúng (nhẹ, giữ chất "trăng máu").

---

## 5. Mục 3 — Mộ Dao pha 4: moveset thật (MoDaoCombat + BossBrain)

### 5.1 Khung
- `MoDaoCombat implements BossBrain.Host`, profile `MoDaoBrain.PROFILE` (file mới `endgame/MoDaoBrain.java`, mô theo `DaKhuyetBrain.java` 105 dòng): nhanh, nhẹ (weight thấp), `rest` 20-35 tick, combo 2-4 nhát.
- Target = NTTD gần nhất (như `allyTarget`). `brainCanHit`: chỉ NTTD/Hư mob. Sát thương như §4.3 (`ALLY_FRAC 0.002`, trần 10%/pha 4).
- Bất tử: `hurt()` trả false; NTTD không target (đã có `isStoryAlly`).
- Hỗ trợ (giữ ý cũ, đổi thành chiêu có anim): mỗi ~15s `Thanh Tâm Hộ Thể` khi có người chơi < 50% máu.
- `NttdAllies.moTick` → chỉ còn giám sát (khoảng cách > 40 thì lướt về), brain lo phần còn lại. Gọi `BossBrain.tickFor(sl, mo, mo)` trong `MoDaoCombat.tick()`.

### 5.2 Moveset (8 chiêu)

| # | Tên | Di chuyển | Telegraph | Gây sát thương | Anim |
|---|-----|-----------|-----------|----------------|------|
| M1 | **Lưu Vân Bộ** (gap-close) | lướt thẳng 8-12 block trong 5 tick, dừng cách mép NTTD 2 block | vệt ribbon xanh mờ 3 tick trước | không | `dash` |
| M2 | **Tam Kiếm Liên Hoàn** (combo thường) | mỗi nhát tiến 1 block | không (đòn nhanh) | 3 nhát, cung 120°, tầm 3.5, ×0.6/×0.6/×1.0 | `combo_1/2/3` |
| M3 | **Thanh Minh Nhất Tuyến** | lướt xuyên qua NTTD ra sau 6 block (đổi bên) | `telegraphLine` xanh 8 tick | 1 đòn ×1.6 lúc đi qua | `dash_slash` |
| M4 | **Phi Yến Hồi Toàn** | nhảy lên 3 block, xoay 2 vòng, đáp xuống | vòng `groundRing` xanh nơi đáp, 10 tick | AoE r=3.5 lúc đáp ×1.3 | `spin_leap` |
| M5 | **Kiếm Khí Tam Điệp** (tầm xa) | lùi 3 block (`backstep`) rồi vung | cone ngắn 6 tick | 3 `Fx.qi` bay, mỗi cái ×0.5 | `backstep` + `qi_cast` |
| M6 | **Thiên Ảnh Bách Kiếm** (chiêu lớn, cd 25s) | đứng tại chỗ, lơ lửng 1 block | `rune` dưới chân 20 tick + `swordFly` hiện quanh | 12 kiếm `Fx.swordStab` rơi vào NTTD, mỗi ×0.25 | `sword_rain_cast` |
| M7 | **Né bên / Lăn né** (evade) | sidestep 3 block / backflip khi NTTD tung chiêu (`notifySkillCast`) | — | không | `sidestep_l/r`, `backflip` |
| M8 | **Thanh Tâm Hộ Thể** (hỗ trợ) | lướt tới cạnh người chơi máu thấp nhất | `rune` 10 tick | hồi 12% + Absorption II + `Fx.shell` | `sheathe_sword`→`heal_cast` |

Strafe khi rảnh: `strafe_l/r` vòng quanh NTTD bán kính 6-9 (BossBrain STRAFE, có sẵn trong `Locomotion`).

---

## 6. Model & anim Mộ Dao (GeckoLib)

`geo/entity/npc_mo_dao_combat.geo.json` = copy `npc_mo_dao.geo.json` + bone mới:

| Bone | Parent | Ghi chú |
|------|--------|---------|
| `sheath` | `body` | vỏ kiếm đeo hông trái, dài ~12px, nghiêng 30° |
| `sword_sheathed` | `sheath` | lưỡi + chuôi nằm trong vỏ (chỉ chuôi lộ) |
| `sword` | `right_arm` | kiếm cầm tay, pivot ở cổ tay; ẩn (scale 0) khi tra vỏ |
| `sword_blade` | `sword` | để gắn glow/trail |
| `hair_back` (tùy chọn) | `head` | tóc bay khi lướt |
| `sash` (tùy chọn) | `body` | đai áo bay |

Rút kiếm = keyframe: t0 `sword` scale 0 / `sword_sheathed` scale 1 → t4 (tay chạm chuôi) đổi: `sword` scale 1, `sword_sheathed` scale 0 → t10 tay kéo ra trước. Tra kiếm ngược lại.

Anim mới (`animations/entity/npc_mo_dao_combat.animation.json`, tên `animation.npc_mo_dao_combat.*`): `idle walk talk` (copy) + `guard_idle dash draw_sword sheathe_sword parry_high parry_low counter_slash dash_slash combo_1 combo_2 combo_3 spin_leap backstep qi_cast sword_rain_cast sidestep_l sidestep_r backflip strafe_l strafe_r heal_cast` (21 mới). Đăng ký qua `BrainAnims.controller(...)` như `DaKhuyet.java:533` + controller `skill` triggerable cho cutscene.

Client: renderer `GeoEntityRenderer<MoDaoCombat>` dùng texture `npc_mo_dao.png` (mở rộng nếu thiếu UV) + glowmask cho `sword_blade` (tùy chọn). Đăng ký entity type trong `EndgameRegistry`.

---

## 7. Các cutscene khác

### 7.1 Cải tà Dạ Khuyết (`DaKhuyet.startRedeem`, REDEEM_TICKS hiện tại ≈ 6×48)
| Tick | Beat | Anim | VFX |
|------|------|------|-----|
| 0 | Đòn cuối của người chơi: DK khựng, lảo đảo lùi 2 block | `stagger` (**mới** cho DK) | hit-stop 6 tick, `CRIT`, shake |
| 14 | **Quỳ một gối, cắm katana xuống đất** | `kneel` (**mới**: legs, body, right_arm, weapon_katana) | `groundCrack` nhỏ đỏ |
| 40-120 | Đối thoại 1-2, trăng máu trên đầu nhỏ dần | `kneel_idle` (mới, loop) | `elem(E_HALOS)` đỏ, `moonDust` |
| 130 | **Ngẩng lên, trăng đổi bạc** (ánh sáng quét từ đỉnh đầu xuống) | `look_up` (mới, head/hood) | `Fx.ribbon` từ trời xuống, halo đổi `MOON_RED→MOON_WHITE` |
| 180 | **Buông kiếm** — rút katana khỏi đất, đặt ngang trước mặt người chơi | `offer_blade` (mới) | `END_ROD` dọc lưỡi |
| 240 | Đứng dậy, cánh trăng mở | `rise` (có) | `wingsFx` MOON |
| T | tan (giữ fade cũ) | — | — |
Camera: thêm shot cận tay cắm kiếm (t14), cận mặt khi ngẩng (t130, fov 38), góc thấp nhìn lên lúc đứng dậy (t240).

### 7.2 Hy sinh pha 3→4 (`grandSacrifice`)
Giữ khung `later()`. Bổ sung:
- t0-60: NTTD (form2) `skill_c`/`star_beam` tụ — anim thật, `ring_*` bone xoay (đã có trong model).
- t70: DK **không teleport** — lướt `dash` 8 tick từ chỗ đang đứng tới `guard` (moveTo nội suy) rồi `rise` + kết giới.
- t150: DK quỳ dần dưới áp lực (`kneel` từ 7.1), kết giới nứt (`groundCrack` + `rift`).
- t170: DK quay đầu nhìn người chơi (`look_up`/head yaw), phụ đề sacrifice.
- t175: tan thành cánh hoa (giữ).
- t200-212: **Mộ Dao lao xuống từ trên cao** (spawn ở +12y, `spin_leap` đáp), `draw_sword` khi chạm đất, đứng `guard_idle` hướng NTTD. Camera key t200 nhìn lên trời, t214 cận Mộ Dao đáp.

### 7.3 Ending (`NttdEnding`)
- t0-130: thay chỉ particle bằng thân NTTD thật đứng tại `o` chạy `death` (có sẵn: `animation.nguyen_thuy_tich_diet.death`) — giữ boss entity sống `noAi + invulnerable` đến t130 rồi discard (cần `NguyenThuyTichDiet.die` không discard ngay khi ending chạy).
- t320: Mộ Dao (`MoDaoCombat`, noAi) bước vào (walk 20 tick), t340 `sheathe_sword` (tra kiếm — kết thúc chiến tranh), t360-500 `talk`.
- Ký ức DK (t500-700): thêm ảnh ảo DK (spawn DaKhuyet `noAi`, invisible-ish/`Glowing` + particle) chạy `idle` rồi `rise` và tan — thay vì chỉ particle. Cờ `dkMemory` giữ.

---

## 8. Work breakdown

Ký hiệu: **[Opus]** = biên đạo/brain khó; **[Haiku]** = cơ học. Cột "File" để xếp chạy song song (không trùng file).

| ID | Việc | Model | File chạm | Phụ thuộc |
|----|------|-------|-----------|-----------|
| A1 | Tạo `npc_mo_dao_combat.geo.json` (copy + bone §6), kiểm tra/mở rộng UV texture | [Haiku] | geo/entity/npc_mo_dao_combat.geo.json, textures/entity/npc_mo_dao.png | — |
| A2 | Nháp 21 anim Mộ Dao (JSON keyframe, theo bảng §3.3/§5.2/§6) | [Haiku] (nháp) → [Opus] duyệt `draw_sword/sheathe_sword/dash_slash` | animations/entity/npc_mo_dao_combat.animation.json | A1 (tên bone) |
| A3 | Anim mới DK: `stagger kneel kneel_idle look_up offer_blade` | [Haiku] | animations/entity/da_khuyet.animation.json | — |
| A4 | Anim mới NTTD form1: `fly_in` | [Haiku] | animations/entity/nttd_form1.animation.json | — |
| B1 | Hook `brainCanHit` trong `BossBrain.Host` + lọc ở `strike` (line 333) và chỗ quét Player line 463 | [Opus] | boss/ai/BossBrain.java | — |
| B2 | DK đồng minh dùng brain + `DaKhuyetMoves.victims()` + scale sát thương/trần + gapClose theo bbox | [Opus] | endgame/DaKhuyet.java, DaKhuyetMoves.java, DaKhuyetBrain.java | B1 |
| B3 | Entity `MoDaoCombat` + `MoDaoBrain` (profile, 8 chiêu, follow-up) + đăng ký entity/renderer | [Opus] | endgame/MoDaoCombat.java (mới), endgame/MoDaoBrain.java (mới), EndgameRegistry.java, client renderer (mới) | B1, A1, A2 |
| B4 | `NttdAllies` chuyển sang `MoDaoCombat`, `moTick` thành giám sát | [Haiku] | endgame/NttdAllies.java | B3 |
| C1 | Viết lại Lv10Cutscene (§3) | [Opus] | core/Lv10Cutscene.java | B3 (entity), A4 |
| C2 | Đăng ký trigger `f1_fly_in` + anim DK mới trong controllers | [Haiku] | NguyenThuyTichDiet.java (line 1744-1747), DaKhuyet.java (line 536) | A3, A4 — **chạy sau B2** vì cùng DaKhuyet.java |
| C3 | Cải tà DK (§7.1) | [Opus] | DaKhuyet.java (`startRedeem/tickRedeem`) | B2, A3 |
| C4 | Hy sinh (§7.2) | [Opus] | NguyenThuyTichDiet.java (`grandSacrifice/arriveMo`) | B3, A3, C2 |
| C5 | Ending (§7.3) | [Opus] | endgame/NttdEnding.java (+ chỗ `die` NTTD) | B3 |
| D1 | Lang: phụ đề mới/dời mốc, tên chiêu Mộ Dao (vi_vn + en_us) | [Haiku] | assets/hukhong/lang/*.json | — |
| D2 | Cập nhật `DevCutsceneDemo` thêm nút: Lv10, redeem, sacrifice, ending, "ally sandbox" (NTTD + DK + Mộ Dao bất tử đánh nhau 60s) | [Haiku] | client/combatui/DevCutsceneDemo.java | C1-C5 |
| E1 | GameTest: DK ally không bao giờ gây sát thương Player; Mộ Dao dùng ≥5 chiêu khác nhau trong 60s; trần sát thương | [Opus] | src/test hoặc gametest hiện có | B2, B3 |

**Song song được (đợt 1)**: A1, A3, A4, B1, D1 — không trùng file.
**Đợt 2**: A2 ∥ B2 (khác file).
**Đợt 3**: B3 → rồi B4 ∥ C1 ∥ C5.
**Đợt 4 (tuần tự vì chung file)**: C2 → C3 (DaKhuyet.java); C4 (NguyenThuyTichDiet.java) sau C2.
**Đợt 5**: D2, E1, rồi chơi thử thật (unit test qua ≠ đẹp — phải xem trong game).

---

## 8b. Mục 4 — 3 cutscene kết (Phong Ấn / Hấp Thụ / Siêu Độ) diễn thật trong thế giới

### 8b.1 Hiện trạng
| Phát hiện | Bằng chứng | Khoảng trống |
|-----------|-----------|--------------|
| Chọn kết → `playEnding` chỉ gửi `EndgameNet.PlayCutscene(ending)`, người chơi bất tử + chậm | `endgame/EndgameService.java:200-224` | Không có thực thể/camera trong thế giới |
| Client mở `CutsceneScreen` (màn hình 2D xoáy tím) hiện 3 dòng `hukhong.endgame.cutscene.<seal|absorb|truth>.1..3` | `client/endgame/CutsceneScreen.java:22-69` | Chỉ chữ trên nền |
| Thời lượng `CUTSCENE_TICKS=600`, `FADE_IN=40`, `FADE_OUT=80`, 3 dòng → mỗi dòng 160 tick (40-200, 200-360, 360-520) | `endgame/EndgameKit.java:109-131` | Giữ nhịp này để khớp phụ đề |
| Sau cutscene chỉ có tin nhắn chat `hukhong.endgame.return.*`; Hấp Thụ không đổi gì trên ngoại hình | `EndgameService.java:258`; lang `vi_vn.json:762-763` | Thiếu hậu quả nhìn thấy được |

Dòng thoại (vi_vn.json):
- Phong Ấn (766-768): 1 "Bốn Mảnh sáng lên, ráp thành Thiên Môn." · 2 "Cửa Dạ khép lại. Những gì còn kẹt bên trong sẽ ở lại đó." · 3 "Bình minh đầu tiên sau ba trăm năm." · return: "Cánh cổng khép lại sau lưng ngươi... cảm giác chưa xong."
- Hấp Thụ (770-772): 1 "Hư Không chảy ngược vào trong ngươi, lặng lẽ như nước vào cát." · 2 "Cửa Dạ mở ra trong lồng ngực. Không còn ai nói chuyện với ngươi nữa." · 3 "Ngươi là tất cả. Và chỉ có một mình." · return: "...Người đời nhìn ngươi bằng ánh mắt sợ hãi."
- Siêu Độ (2197-2199): 1 "Hư Không tan thành mưa linh quang." · 2 "Dạ Khuyết mỉm cười, lần đầu sau ngàn năm." · 3 "Thế gian thở ra. Và phương xa, có thứ đang thức giấc."

### 8b.2 Khung chung
- Lớp mới `endgame/EndingOutro.java` (server, mô theo `NttdEnding`: `Run` + `step(r,t)` + `onTick`): `start(p, ending)` gọi `BossPhaseDirector.sendCutscene(sl, null, o, yaw, 600, cam, subs, MIN_SKIP=300)` với `subs` = 3 key cũ ở mốc 40-200 / 200-360 / 360-520 + tiêu đề 4-40. `o` = vị trí người chơi trong Hư Không Bản Nguyên.
- `EndgameService.playEnding`: thay `PlayCutscene` bằng `EndingOutro.start`; khi outro xong (t=600) mới gửi `PlayCutscene` ở chế độ **chỉ credits** (thêm cờ `creditsOnly` vào payload, `CutsceneScreen` bỏ phần xoáy → `toCredits()` ngay). Giữ `CUTSCENE` map để "skip" vẫn trả người chơi về.
- Người chơi là diễn viên: giữ tại `o` (như Lv10), camera ngoài thân thấy được model người chơi. Thực thể phụ (Thiên Môn, Dạ Khuyết, Mộ Dao) spawn `noAi` với TAG để dọn khi logout (giống `Lv10Cutscene.onLogin`).

### 8b.3 Phong Ấn — beat
| Tick | Dòng | Dàn dựng | VFX | Camera |
|------|------|----------|-----|--------|
| 0-40 | tiêu đề | Người chơi đứng trước Cửa Dạ (vết nứt lớn dọc, `Fx.rift` cao 12, tím, sống 400t) | gió `PORTAL` | rộng sau lưng người chơi (0,3,−8, fov 70) |
| 40-120 | 1 | 4 Mảnh bay ra từ người chơi (4 `ItemDisplay`/ArmorStand nhỏ hoặc `Fx.swordFly` thay thế), xoay quanh rồi bay lên 4 góc của vết nứt | mỗi mảnh 1 vệt `ribbon` màu riêng (vàng/xanh/đỏ/trắng) | vòng quanh người chơi 90° |
| 120-200 | 1 | 4 Mảnh nối nhau bằng tia → khung **Thiên Môn** vàng | `Fx.rune` vàng dựng đứng 10f, `ribbon` nối 4 góc, `BEACON_ACTIVATE` | cận khung từ dưới lên (pitch −20) |
| 200-280 | 2 | Cửa Dạ co hẹp lại từ hai mép (rift scale giảm mỗi 10t), cánh cổng vàng ép vào | `groundCrack` vàng, shake 2 | ngang, rộng |
| 280-360 | 2 | "Những gì còn kẹt bên trong": bóng mờ (2-3 hình nhân Hư mob/ bóng người, `SOUL` hạt) với tay ra khe nứt rồi bị khép lại — **khoảnh khắc buồn** | `SOUL`, `REVERSE_PORTAL` hút vào | cận khe nứt, fov 40 |
| 360 | — | khe khép hẳn: chớp trắng | `elem(E_NUKE, vàng)`, shake 3 | — |
| 360-520 | 3 | Bầu trời Hư Không chuyển sáng; mặt trời mọc (client: tạm ép `dayTime` cục bộ hoặc vùng sáng `END_ROD` ngang chân trời) — người chơi quay lưng nhìn bình minh | hạt vàng nhẹ rơi | cẩu lên cao lùi xa (0,20,20 → fov 82) |
| 520-600 | fade | — | — | giữ |

Hậu quả nhìn thấy (theo "cảm giác chưa xong"): người chơi giữ **vết ấn vàng** nhỏ trên mu bàn tay (render layer nhẹ, cờ `F_SEAL`); ban đêm thỉnh thoảng nghe tiếng gõ cửa xa (âm thanh cục bộ, hiếm) — tùy chọn.

### 8b.4 Hấp Thụ — beat
| Tick | Dòng | Dàn dựng | VFX | Camera |
|------|------|----------|-----|--------|
| 0-40 | tiêu đề | Người chơi giữa không gian Hư Không, Cửa Dạ phía trước | rift tím | sau lưng |
| 40-200 | 1 | **Hư Không chảy ngược vào người chơi**: hàng trăm dòng `ribbonWave` tím từ mọi hướng xoắn vào ngực người chơi, chậm, êm ("như nước vào cát") — không tiếng nổ, chỉ âm trầm | `ribbonWave` mỗi 3t, `REVERSE_PORTAL` hút vào tâm, rift co lại dần | vòng 180° chậm quanh người chơi, fov 60 |
| 200-280 | 2 | **Cửa Dạ mở trong lồng ngực**: rift nhỏ dọc (h=0.6) ngay trước ngực người chơi, phát sáng tím; overlay nửa thân bắt đầu hiện (lớp §8b.6 bật ở đây, alpha tăng dần) | `Fx.rift` nhỏ gắn theo người chơi, `Fx.shell` tím | **cận ngực**, fov 35 |
| 280-360 | 2 | "Không còn ai nói chuyện với ngươi": các bóng ma quen thuộc (Mộ Dao, Dạ Khuyết, Hứa Cô — spawn `StoryNpc` noAi mờ/`Invisible`+particle hình người) đứng vòng quanh, lần lượt quay lưng và tan khi camera lướt qua | mỗi bóng tan `END_ROD`→`SMOKE` | lướt ngang qua từng bóng |
| 360-520 | 3 | "Ngươi là tất cả": camera kéo vút lên rất xa, người chơi thành chấm nhỏ giữa vùng tối mênh mông; toàn bộ hạt tắt, chỉ còn mắt/vết nứt tím của người chơi phát sáng. "Một mình": im lặng hoàn toàn (tắt nhạc) | dừng mọi particle, chỉ `glow` overlay | cẩu thẳng lên (0,60,0, pitch 89, fov 30→80) |
| 520-600 | fade | — | — | — |

### 8b.5 Siêu Độ — beat
| Tick | Dòng | Dàn dựng | VFX | Camera |
|------|------|----------|-----|--------|
| 40-200 | 1 | Hư Không vỡ thành mưa sáng: vòm trời tím nứt, rơi xuống thành giọt vàng-trắng | `rift`s tan; `END_ROD`+`FALLING_HONEY`? → dùng `END_ROD` rơi chậm + `Fx.elem(E_NEEDLES, trắng-vàng)` hướng xuống mỗi 5t | ngửa nhìn trời từ dưới chân người chơi |
| 200-360 | 2 | **Dạ Khuyết** (spawn DaKhuyet noAi, model thật, texture thường) đứng trong mưa, ngẩng lên, quay về người chơi, đầu hơi nghiêng — anim `look_up` + `idle` (A3); không có anim cười → dùng cận mặt + ánh trăng bạc + cánh trăng mở nhẹ (`wingsFx` MOON) | halo bạc, cánh hoa `CHERRY_LEAVES` | **cận mặt DK** fov 35, rồi hai người cùng khung |
| 360-460 | 3 | "Thế gian thở ra": làn sóng sáng lan từ chỗ đứng ra xa (vòng `groundRing` bán kính 5→60) | `groundRing` vàng mở rộng | cẩu cao |
| 460-520 | 3 | "Phương xa có thứ thức giấc": camera quay ngoắt về chân trời, một con mắt/đốm đỏ lóe trong mây (Yêu Tộc) | `elem(E_HALOS, đỏ)` nhỏ xa 80 block, âm `ENDER_DRAGON_GROWL` rất nhỏ | xoay 180° nhanh, fov 30 (zoom xa) |

Hậu quả nhìn thấy: đã có cơ chế (vùng linh khí, Thủ Hộ). Thêm: vòng sáng mờ (halo vàng nhạt) sau lưng người chơi khi đứng yên — layer nhẹ cờ `SieuDoKit.TITLE_FLAG`.

### 8b.6 Ngoại hình "Nửa Hư Không" (Hấp Thụ) — vĩnh viễn, ai cũng thấy
- **Dữ liệu**: cờ `F_ABSORB` hiện nằm trong `QuestData` (server, chỉ đồng bộ cho chính người đó?). Cần một attachment đồng bộ cho **mọi client**: `ModAttachments.VOID_MARK` (int 0/1/2 = none/half-void/sealed-hand/halo) đặt trên `Player`, `.sync(...)` của NeoForge attachment (1.21.1 hỗ trợ `AttachmentType.builder(...).serialize(...)` + sync qua payload riêng nếu bản NeoForge chưa có sync tự động — kiểm phiên bản). Đặt ở `EndgameService.choose` (absorb) và khôi phục lúc login từ cờ quest (`onLogin` line 114) để người chơi cũ cũng có.
- **Render**: `client/endgame/VoidHalfLayer.java` = `RenderLayer<AbstractClientPlayer, PlayerModel<...>>` đăng ký trong `EntityRenderersEvent.AddLayers` cho cả skin `default` và `slim`:
  - vẽ lại model người chơi với texture `textures/entity/void_half.png` (64×64, nửa phải đen-tím có vân sao, nửa trái trong suốt) + RenderType `entityTranslucentEmissive` cho đường nứt tím phát sáng (texture `void_half_glow.png`).
  - đường ranh giới hai nửa có vết nứt răng cưa; mắt bên phải tím phát sáng.
  - client particle: mỗi 6 tick, 1-2 hạt `ModParticles.HU_KHONG` / `REVERSE_PORTAL` từ nửa thân phải (`ClientTickEvent`, chỉ cho người chơi trong 32 block, tắt khi tàng hình).
  - tay người thứ nhất: `RenderHandEvent` vẽ tay phải phủ lớp hư không.
- Cutscene §8b.4 t200: alpha layer tăng 0→1 (client đọc `cutsceneProgress`; đơn giản hơn: server đặt attachment ở t200, layer tự fade-in 40 tick lần đầu thấy).
- Hệ quả "người đời sợ hãi" (tùy chọn, nhỏ): dân làng/StoryNpc phụ lùi lại khi người chơi lại gần (`villager` goal avoid) — để sau.

### 8b.7 Work breakdown bổ sung
| ID | Việc | Model | File | Phụ thuộc |
|----|------|-------|------|-----------|
| G1 | `EndingOutro` khung chung + nối `playEnding` + cờ `creditsOnly` cho `CutsceneScreen` + `EndgameNet` | [Opus] | endgame/EndingOutro.java (mới), EndgameService.java, EndgameNet.java, client/endgame/CutsceneScreen.java | — |
| G2 | Beat + camera Phong Ấn (§8b.3) | [Opus] | EndingOutro.java (phần `seal`) | G1 |
| G3 | Beat + camera Hấp Thụ (§8b.4) | [Opus] | EndingOutro.java (phần `absorb`) | G1, G6 |
| G4 | Beat + camera Siêu Độ (§8b.5) | [Opus] | EndingOutro.java (phần `truth`) | G1, A3 |
| G5 | Attachment `VOID_MARK` đồng bộ mọi client + đặt khi chọn kết/khôi phục lúc login | [Opus] | registry attachment (mới hoặc ModAttachments hiện có), EndgameService.java | — (chạy cùng đợt G1 nhưng **sau G1** vì chung EndgameService.java) |
| G6 | `VoidHalfLayer` + tay người thứ nhất + particle client | [Opus] | client/endgame/VoidHalfLayer.java (mới), client event đăng ký layer | G5 |
| G7 | Texture `void_half.png` + `void_half_glow.png` (64×64 theo UV skin người chơi), ấn tay vàng (seal), halo (truth) | [Haiku] (nháp) | textures/entity/ | — |
| G8 | Layer nhẹ cho seal (ấn tay) và truth (halo) dùng chung `VoidHalfLayer` theo giá trị attachment | [Haiku] | VoidHalfLayer.java | G6 |
| G9 | Lang: tiêu đề phụ nếu tách dòng; mô tả trạng thái ngoại hình | [Haiku] | lang/*.json (gộp với D1) | — |
| G10 | Dev demo: nút chạy 3 outro + bật/tắt `VOID_MARK` | [Haiku] | client/combatui/DevCutsceneDemo.java (gộp với D2) | G1-G6 |
| G11 | Kiểm thử multiplayer (2 client): người khác thấy nửa hư không sau relog | [Opus] | — | G6 |

Song song: G7 ∥ G1 (khác file); G2/G3/G4 chung `EndingOutro.java` → tuần tự hoặc tách thành `EndingOutroSeal/Absorb/Truth.java` để chạy song song (khuyên tách). G5→G6→G8 tuần tự. Toàn bộ nhóm G độc lập với nhóm A-E trừ A3 (anim DK) và D1/D2 (lang, demo).

## 9. Rủi ro / câu hỏi mở
1. `CamPath.Key.yaw` khi neo điểm là tương đối hay tuyệt đối — đọc `CutsceneClient` trước C1 (chưa kiểm).
2. Texture Mộ Dao có đủ UV trống cho kiếm/vỏ không — kiểm ở A1; nếu không, mở rộng canvas (phải giữ UV cũ).
3. BossBrain có thể có chỗ khác giả định target là Player (vd. `hitStop`, `punishHeal`) — B1 phải đọc hết `BossBrain.java` (487 dòng) chứ không chỉ line 333.
4. Hit-stop/slow-mo client cho cutscene là tùy chọn; nếu cần thì thêm payload riêng (không có sẵn).
5. Cân bằng: tổng sát thương đồng minh ≤ ~20% máu NTTD cả trận — điều chỉnh sau khi chơi thử.

## Document Lineage
| Version | Document | Status |
|---------|----------|--------|
| v1 | docs/PLAN-v1-nttd-story-rework.md | ✅ Current |
