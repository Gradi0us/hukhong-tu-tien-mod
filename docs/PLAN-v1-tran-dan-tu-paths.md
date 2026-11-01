# PLAN-v1: Thêm 2 đạo Trận Tu (TRAN) + Đan Tu (DAN)

**Created**: 2026-11-01
**Last Updated**: 2026-11-01 — Bản đầu: research xong, chia 3 wave cho coder Haiku
**Status**: ⬜ Chưa làm
**Scope**: `core/Path.java` + mọi chỗ switch/if theo Path. Template chính = PHAP (có đủ Q/E/R/T vì không có Bản Mệnh).

---

## Key Findings

| # | Phát hiện | Bằng chứng | Độ tin cậy |
|---|-----------|------------|------------|
| 1 | Save an toàn **nếu thêm vào CUỐI enum**: CultivationData lưu Path bằng `id` string (`Path.CODEC` = `StringRepresentable.fromEnum`), không phải ordinal | core/Path.java:14, core/CultivationData.java:29 | CAO |
| 2 | Có code dùng **ordinal**: mask chọn đạo (bit = ordinal, REROLL_BIT = 1<<8 → 7 đạo dùng bit 0–6, OK), `Path.values()[r[3]]` ở profile, mảng `stage[Path.values().length]` | charcreate/CharCreateService.java:31-33,67; client/screen/profile/ProfileScreen.java:140,457 | CAO |
| 3 | Switch-expression **không có default** → compile lỗi khi thêm enum (tốt: compiler chỉ chỗ). Switch-statement / `if contains` thì im lặng | stats/StatService.java:50,92; CultivationHud.java:105; SkillPages.java:26 | CAO |
| 4 | Phím R (slot 2): đạo có `BanMenhSkill.of(page,lv)` thì R nhường Bản Mệnh; TRAN/DAN không có Bản Mệnh → cần `Skill` slot 2 riêng như PHAP_3 | skill/SkillService.java:76-86; skillbook/SkillBook.java:45; skill/BanMenhSkill.java:10-13 | CAO |
| 5 | Kỹ năng PHAP dispatch qua 1 dòng trong SkillEffects → cùng pattern cho TranSkills/DanSkills | skill/SkillEffects.java:41-55 | CAO |
| 6 | Màn tạo nhân vật: 7 ô × 76 + 6 × 6 = 568px > GUI scale 3 (~427px) → tràn ngang | client/screen/charcreate/CharCreateScreen.java:24,67 | CAO |

---

## Bản đồ chỗ cần thêm case (file:line, src/main/java/com/hukhong/)

**Compile sẽ báo (switch-expression không default)** — thêm case TRAN/DAN:
- `stats/StatService.java:50` table() và `:92` vitals()
- `client/hud/CultivationHud.java:105` màu
- `skill/SkillPages.java:26` lang key (+ mảng `ORDER` :17)
- `client/screen/charcreate/CharCreateScreen.java:195`, `client/screen/profile/ProfileScreen.java:575,598` (lang key / tên pháp khí)
- `client/screen/profile/SkillEntries.java:72,187,218` (kiểm tra từng cái; cái nào là statement thì thêm thủ công)
- `skill/SkillEffects.java:41` (switch theo Skill — sẽ báo khi thêm Skill mới)
- `skill/StanceHands.java:25` (isPathWeapon) — trả false

**Im lặng (phải tự thêm)**:
- `charcreate/CharCreateService.java:29` `UNLOCKED` — thêm TRAN, DAN
- `charcreate/CharRoll.java:40` lặp `Path.values()` → tự có, kiểm tra roll aptitude không giả định 5
- `flight/FlightMode.java:7-11` thêm `TRAN(Path.TRAN, 40, 2.5,1.0,2.5,0.065f,3)`, `DAN(Path.DAN, 40, 2.5,1.0,2.5,0.065f,3)` (copy dòng DAO, minLevel=40)
- `passive/PassiveService.java:118-120` (onHit), `:313-315` (dọn stack khi mất đạo)
- `client/screen/profile/PassiveEntries.java:40` (thêm case mô tả nội tại)
- `artifact/ArtifactData.java:44,54` có `default` → TRAN/DAN = 0, không cần đụng (pháp khí để sau)
- `world/HealRules.java:84`, `gates/DamageRules.java:78,92`, `pathrules/PathRules.java:59-116`: KHÔNG thêm (luật riêng của Kiếm/Đạo/Thể/Ma)
- `skillbook/BookSkill.java`, `society/SectDef.java`, `linhbao/LinhBaoType.java`: ngoài scope (TRAN/DAN chưa có sách/tông môn/linh bảo)
- `client/combatui/DevAutoTest.java`, `gametest/HuKhongRuntimeGameTests.java` (30 ref): nếu có vòng `for Path.values()` giả định 5 đạo → sửa assert
- Test JVM: `src/test/.../BalanceSimTest.java` (24), `SkillEntriesTest`, `SkillPagesTest`, `StatServiceTest` — chạy `./gradlew test` xem cái nào đếm cứng 5

Lệnh tự kiểm: `grep -rn "Path\.PHAP\|case PHAP" src/main/java` — mọi switch có `case PHAP` đều phải được xem.

---

## WAVE 1 — Enum + plumbing + UI + lang (chưa có skill thật)

**Mục tiêu**: chọn được TRAN/DAN lúc tạo nhân vật, HUD/K/bay chạy, Q/E/R/T no-op không crash.

1. `core/Path.java:12` — thêm **SAU PHAP** (giữ ordinal cũ):
   ```java
   PHAP("phap", 0xFF3FD6C4, "hukhong.resource.phapluc"),
   TRAN("tran", 0xFFB8A1FF, "hukhong.resource.tranluc"),
   DAN("dan", 0xFFFF7A3D, "hukhong.resource.danhoa");
   ```
2. `skill/Skill.java:24` — copy pattern PHAP_1..ULT (4 slot, slot 2 có skill riêng):
   ```java
   TRAN_1(Path.TRAN,0,"tran_1",4,14),  TRAN_2(Path.TRAN,1,"tran_2",25,400),
   TRAN_3(Path.TRAN,2,"tran_3",35,400), TRAN_ULT(Path.TRAN,3,"tran_ult",90,1600),
   DAN_1(Path.DAN,0,"dan_1",4,14),    DAN_2(Path.DAN,1,"dan_2",30,500),
   DAN_3(Path.DAN,2,"dan_3",35,360),  DAN_ULT(Path.DAN,3,"dan_ult",90,1800);
   ```
   (cost/cd ~ PHAP_1/2/3/ULT = 3/14, 25/160, 35/400, 90/1500.) Slot 0 ở đây là Q (PHAP_1 cũng slot 0). Basic attack KHÔNG phải Skill — làm trong PassiveService (wave 2/3).
3. `skill/SkillEffects.java:54` — thêm 2 dòng: `case TRAN_1, TRAN_2, TRAN_3, TRAN_ULT -> com.hukhong.tran.TranSkills.run(s.slot, p);` và tương tự `DanSkills`. Tạo `tran/TranSkills.java`, `dan/DanSkills.java` với `public static void run(int slot, ServerPlayer p) {}` (no-op).
4. `stats/StatService.java:55` / `:97`:
   - TRAN: `{2.5, 0.9, 6.5, 1.8, 1, 0.40, 0.04, 0.002, 0, 0, 110, 5}`, vitals `{21, 4.2, 0.0005}`
   - DAN: `{2.2, 0.8, 7, 1.9, 0, 0.30, 0.04, 0.002, 0, 0, 110, 5}`, vitals `{20, 3.8, 0.0006}`
5. `skill/SkillPages.java:17,26`, `CultivationHud.java:105` (TRAN `0xFF8E7CC3`, DAN `0xFFD55E00`), `CharCreateScreen.java:195`, `ProfileScreen.java:575` (TRAN→"Trận Bàn", DAN→"Đan Lô"), `:598`, `StanceHands.java:25` (false).
6. `charcreate/CharCreateService.java:29` thêm TRAN, DAN vào `UNLOCKED`.
7. `flight/FlightMode.java:11` thêm 2 hằng minLevel 40 (xem mục bản đồ).
8. **CharCreateScreen layout** (:24,67): khi `total > width - 20` thì xếp 2 hàng (4+3) hoặc tính `cellW = min(76, (width-20-GAP*(n-1))/n)`. Chọn cách 2 (1 dòng sửa).
9. `SkillEntries.java:72,187,218` + `PassiveEntries.java:40`: thêm case trả entry tối thiểu (tên + `meta(x, path, s.cost, s.cooldownTicks)`, copy :388).
10. Lang `assets/hukhong/lang/vi_vn.json` + `en_us.json` (cạnh dòng 1213/1256):
    `hukhong.path.tran` "Trận Tu"/"Formation Cultivator", `hukhong.path.dan` "Đan Tu"/"Alchemy Cultivator", `hukhong.resource.tranluc` "Trận Lực"/"Formation Power", `hukhong.resource.danhoa` "Đan Hỏa"/"Pill Flame", 8 key `hukhong.skill.tran_1..dan_ult` (tên ở wave 2/3), `hukhong.passive.tran.*`, `hukhong.passive.dan.*`.

**Acceptance W1**
- `./gradlew build -q` xanh; `./gradlew test` xanh (sửa test đếm cứng 5 đạo nếu có).
- GameTest mới trong `gametest/HuKhongRuntimeGameTests.java` (copy pattern test PHAP có sẵn ở file này): `tranDanSelectable` — mock player, `CharCreateService.confirm(p, mask)` với bit TRAN/DAN → `paths()` chứa; `tranDanSkillsNoCrash` — cast Q/E/R/T qua `SkillService` không exception; `flightTranDan40` trong `FlightTienThienGameTests.java`: `FlightMode.TRAN.minLevelFor(null) == 40`.
- Save cũ: load thế giới có nhân vật PHAP → vẫn PHAP (ordinal không đổi).

---

## WAVE 2 — Trận Tu kit + VFX + test

Package mới `com.hukhong.tran`: `TranSkills` (cast), `TranMath` (hằng số, thuần, test JVM được), `TranRuntime` (danh sách trận đang sống, tick).

**Template**: dispatch/hit/around/aim/ground = `phaptu/PhapSkills.java:51-156` (copy helper `around`:124, `aim`:141, `ground`:150, `hit`:89). Sát thương: `DamageRules.hurtBySkill` (gates/DamageRules.java:48). Choáng/root: `PhapRuntime.stun(t, sec)` (phaptu/PhapRuntime.java:295 — setNoAi, boss thế giới ×0.5). Chậm: AttributeModifier như `PhapBeamService.slow` (phaptu/beam/PhapBeamService.java:79,349-356) hoặc `MobEffects.MOVEMENT_SLOWDOWN` amp 2 (~60%). Hẹn giờ: `SkillScheduler.later(ticks, r)` (skill/SkillScheduler.java:24). VFX: `lv.sendParticles(...)` server-side (PhapSkills.java:60,164) + `FxSfx.cast` (PhapSkills.java:80).

**Cấu trúc trận** (`TranRuntime`): `record Zone(UUID owner, ResourceKey<Level> dim, Vec3 c, double r, long end, Kind kind)`; `List<Zone>` static, tick ở `ServerTickEvent.Post` (copy đăng ký của SkillScheduler.onTick :27), xoá khi hết hạn / ServerStopped. Không lưu save (tạm thời, như STUN map của PhapRuntime).

| Phím | Tên | Số liệu (sp = Phép lực) | Thực thi |
|------|-----|--------------------------|----------|
| basic | Phù Văn Đạn | 0.6×sp, đánh dấu 5s (+10% dmg skill lên mục tiêu dấu) | PassiveService onHit: thêm nhánh `if (d.paths().contains(Path.TRAN))` cạnh :118-120; dấu = tag/Map UUID→endTick |
| Q | Khốn Trận | r=4 tại `aim(14)`, 0.8×sp; slow 60% 2s rồi `stun(1.5)` | Zone 3.5s; `SkillScheduler.later(40, ...)` gọi stun cho ai còn trong vòng |
| E | Hộ Thân Trận | r=5 quanh mình, 8s; đồng minh trong vòng −30% dmg | Zone kind=WARD; hook `LivingIncomingDamageEvent` trong TranRuntime: nếu target là Player trong WARD → `e.setAmount(amount*0.7)` |
| R | Sát Trận | 6 cột sáng r=6 quanh `aim(16)`, mỗi 10 tick 1 cột ngẫu nhiên đánh 0.55×sp r=1.5, 4s (8 nhịp ≈ 4.4×sp tổng/đơn mục tiêu) | `SkillScheduler.later(10*i, ...)` lặp 8 lần; VFX END_ROD cột dọc |
| T | Đại Trận | r=12, 6s: mỗi 5 tick kéo mob về tâm (`setDeltaMovement` hướng tâm 0.25); cuối nổ 3.0×sp; trong trận CD skill khác giảm 2× | Hệ số CD: thêm vào chỗ trừ CD — xem `PhapRuntime.cooldownMult(p,page)` (PhapRuntime.java:109) được SkillService dùng; thêm `TranRuntime.cooldownMult(p)` = 0.5 nếu đứng trong Đại Trận của mình (áp khi đặt CD mới) |
| Passive | Trận Tâm | đứng trong trận của mình: +15% dmg, hồi +2 Trận Lực/s | dmg: trong hurt hook nhân 1.15 khi caster trong Zone; regen trong tick TranRuntime |

Hệ số đặt trong `TranMath` dạng `public static final double Q_COEF=0.8, R_TICK_COEF=0.55, ULT_COEF=3.0, WARD_DR=0.30, Q_SLOW=0.60, Q_ROOT_SEC=1.5;` để balance_sim đọc bằng regex (như `const("phaptu/PhapMath.java", ...)` ở tools/balance_sim.py:200).

UI: `SkillEntries` case TRAN mô tả đọc từ `TranMath`; lang tên: Khốn Trận / Hộ Thân Trận / Sát Trận / Đại Trận.

**Acceptance W2**
- `./gradlew build -q`, `./gradlew test` (+ `src/test/java/com/hukhong/tran/TranMathTest.java`: R tổng 8 nhịp, cdMult=0.5).
- GameTest mới `tran/TranGameTests.java` (copy `phaptu/PhapThoGameTests.java`): `khonTranRoots` (zombie trong vòng → sau 45 tick `isNoAi()` true), `hoThanReduces` (player trong WARD nhận ≤ 71% dmg), `daiTranPulls` (zombie ở r=10 sau 40 tick gần tâm hơn), `daiTranCooldownHalf`.

---

## WAVE 3 — Đan Tu kit + VFX + test + balance_sim

Package `com.hukhong.dan`: `DanSkills`, `DanMath`, `DanRuntime` (độc theo UUID mục tiêu: `stacks`, `endTick`, `owner`; hồ độc Zone như TranRuntime; trạng thái biến hình T).

**Độc**: tối đa 10 tầng, mỗi tầng 0.05×sp/giây, kéo dài 6s (refresh khi cộng). Tick mỗi 20 tick ở `ServerTickEvent.Post`, sát thương qua `DamageRules.hurtBySkill` (KHÔNG dùng MobEffects.POISON vì không scale sp và không giết được).

| Phím | Tên | Số liệu | Thực thi |
|------|-----|---------|----------|
| basic | Bạo Đan | 0.5×sp r=1.5 + 1 tầng độc | PassiveService onHit nhánh `Path.DAN` (:118-120); VFX `ParticleTypes.ITEM_SLIME`/`POOF` |
| Q | Độc Đan Vũ | 5 viên rơi quanh `aim(14)` r=3, mỗi viên 0.4×sp + 2 tầng; để hồ độc r=2 4s (1 tầng/giây) | `SkillScheduler.later(4*i, ...)`; hồ = Zone |
| E | Hồi Xuân Đan | hồi 0.8×sp + 8% maxHP cho mình + người chơi r=6; xoá hiệu ứng xấu | copy cách tìm đồng minh và điều kiện Ma ở `SkillEffects.java:88,116` (Ma Tu không hỗ trợ người khác — giữ quy tắc); cleanse: `removeAllEffects` lọc `!effect.isBeneficial()` |
| R | Đan Hỏa Phần Thiên | nón 8 block, cos 0.6; 1.2×sp × (1 + 0.15×tầng độc), tiêu hao độc | cone helper `PhapSkills.java:129`; FLAME + SOUL_FIRE_FLAME |
| T | Cửu Chuyển Kim Đan | 10s: skill không tốn tài nguyên, basic → bom r=3 1.0×sp + 2 tầng | cờ trong DanRuntime; miễn phí: hook tương tự `PhapRuntime.consumeFree(p,page,slot)` (PhapRuntime.java:100) trong SkillService |
| Passive | Đan Tâm | Đan Lô +1 bậc chất lượng; Đan Độc tích −50% | grep `alchemy/DanLoBlockEntity.java` chỗ tính quality & `danDoc`/đan độc; nhân 0.5 nếu người dùng có `Path.DAN` |

`DanMath`: `MAX_STACKS=10, STACK_DPS_COEF=0.05, BASIC_COEF=0.5, Q_COEF=0.4, HEAL_COEF=0.8, R_COEF=1.2, R_PER_STACK=0.15, ULT_BOMB_COEF=1.0, DOC_GAIN_MULT=0.5`.

**balance_sim** (`tools/balance_sim.py`): sau nhánh PHAP (:478-485) thêm `elif path == "TRAN"` / `"DAN"` dùng `mk(name,"sp",coef,hits,S[key]["cd"],S[key]["cost"])`; đọc hằng qua `const("tran/TranMath.java", ...)` như :200. TRAN R: hits=8, coef=R_TICK_COEF; DAN thêm DoT xấp xỉ `10 tầng × 0.05 × 6s` vào sustained DPS. Điều kiện `if path != "PHAP"` ở :486 đổi thành `not in ("PHAP","TRAN","DAN")` (không có Bản Mệnh). Thêm 2 path vào danh sách path chạy + `BalanceSimTest.java` nếu nó liệt kê path. Mục tiêu: DPS TRAN/DAN trong ±15% PHAP cùng cấp/tư chất B.

**Acceptance W3**
- `./gradlew build -q`, `./gradlew test` (+ `DanMathTest`: R ở 10 tầng = 1.2×2.5×sp).
- `python tools/balance_sim.py` chạy không lỗi, in hàng TRAN/DAN.
- GameTest `dan/DanGameTests.java`: `poisonStacks` (3 basic → 3 tầng), `poisonCaps10`, `hoiXuanHealsAndCleanses` (player có Poison → hết, HP tăng), `phanThienScales` (dmg 5 tầng > 0 tầng × 1.7), `kimDanFree` (trong T cast Q không giảm resource).

---

## Rủi ro

| Rủi ro | Giảm thiểu |
|--------|-----------|
| Save compat | Chỉ thêm cuối enum; Path lưu bằng id string. KHÔNG đổi tên/thứ tự cũ. Map/Set codec tự chịu được key mới |
| Ordinal mask | REROLL_BIT=1<<8 ≥ 7 đạo OK; nếu sau này >8 đạo phải dời bit |
| Switch thiếu case | Switch-expression → compiler báo; switch-statement/`contains` → dùng grep `case PHAP` checklist ở trên |
| Client/server tách lớp | `TranRuntime`/`DanRuntime` chỉ server (ServerLevel, sendParticles). Màn K chỉ đọc `TranMath`/`DanMath` (thuần, không import ServerPlayer). Không import `client.*` từ package `tran`/`dan` (lưu ý SkillService đang gọi `client.anim.CastAnimHook` — đừng copy kiểu đó) |
| Blur 1.21 | Screen mới/sửa: không gọi `renderBackground` 2 lần/frame (1.21 crash "Can only blur once per frame"); CharCreateScreen chỉ sửa layout, giữ nguyên cách vẽ nền |
| CharCreate tràn ngang | Co ô theo `width` (wave 1 bước 8) |
| Phím R | Không có Bản Mệnh → R luôn là TRAN_3/DAN_3; kiểm `SkillBook.resolve` :45 trả skill gốc |
| Zone/độc rò bộ nhớ | Xoá khi hết hạn, khi entity chết, và ở `ServerStoppedEvent` |

## Document Lineage

| Version | Document | Status |
|---------|----------|--------|
| v1 | docs/PLAN-v1-tran-dan-tu-paths.md | ✅ Current |
