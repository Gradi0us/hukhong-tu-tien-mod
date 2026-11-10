# GUIDE-v1: Boss Combat Brain — cách gắn cho một boss

**Created**: 2026-11-10
**Last Updated**: 2026-11-10 — B1: khung chung + boss mẫu Sứ Đồ Dạ Kiếm
**Status**: ✅ Current
**Code**: `src/main/java/com/hukhong/boss/ai/` · Plan: `PLAN-v1-boss-movement-combat-ai.md` §1, §7

---

## 1. Tổng quan

```
BossPhaseDirector.tick()  ──►  BossBrain.tickFor()      (tự gọi cho mọi PhasedBoss)
                                  │ brainProfile() == null → không làm gì (hành vi cũ)
                                  │ director.frozen() / ownMoveActive() → nhường quyền
                                  ▼
  OBSERVE → STRAFE ─┬─► BASIC_COMBO (windup / active / recovery, rẽ nhánh)
                    ├─► GAP_CLOSE (DASH / LEAP / TELEPORT / tự viết) → combo
                    ├─► EVADE (backstep / sidestep / backflip) ◄─ vung tay, đạn, phép
                    ├─► REACT (trừng phạt ăn/uống, phản đòn sau N nhát)
                    └─► brainBigMove() — chiêu lớn riêng của boss, mỗi bigMoveEvery chuỗi
```

| File | Vai trò |
|------|---------|
| `BossProfile` | Thông số (theo pha), combo, nhánh, gap-close, phản ứng, độ khó (`difficulty`, mặc định 1.0) |
| `BossBrain` | Máy trạng thái, glint (M9), hit-stop (M6), dò mối đe dọa phía server |
| `BossMovement` | Tăng/phanh (M1), giới hạn xoay (M2), root-motion (M4), bước chân (M8), giữ trong `BossArena`, lách tường |
| `MoveSelector` | Chọn đòn theo tầm + trọng số + chống lặp (không quá 2 lần liên tiếp) |
| `GapClose` | Kiểu áp sát pluggable |
| `BrainEvents` | AttackEntityEvent → né · LivingDamageEvent.Post → phản đòn · UseItem.Finish (thuốc/đồ ăn) → trừng phạt |
| `BrainAnims` | Controller GeckoLib "brain" (blend M5), tên thiếu rơi về attack/walk |

Brain ghi đè vận tốc + hướng mặt mỗi tick, nên boss **phải tắt goal di chuyển/đánh vanilla** khi có profile.

## 2. Gắn brain cho boss (checklist)

1. Khai báo profile (static, dùng chung mọi instance):
```java
static final BossProfile BRAIN = BossProfile.builder()
    // PhaseVals(prefMin, prefMax, strafeSpeed, turnRate°/tick, evadeChance, evadeCd, restMin, restMax)
    .phase(1, new BossProfile.PhaseVals(3, 6, 0.18, 16f, 0.25, 50, 26, 40))
    .phase(2, new BossProfile.PhaseVals(3, 6, 0.20, 18f, 0.35, 40, 20, 32))   // pha thiếu = dùng pha trước
    .weight(1.0)            // 0.5 nhẹ .. 3 nặng: hit-stop dài hơn, bụi/âm bước to hơn
    .accel(5, 4).moveSpeed(0.26)
    .gapClose(GapClose.DASH, 9)   // xa hơn 9 ô (hoặc ngoài tầm mọi combo) thì áp sát
    .combo(...)                   // xem §3
    .punishHeal("dam")            // tùy chọn
    .counter(3, 40, "quet")       // tùy chọn: bị đánh 3 lần trong 40 tick -> lướt ngang + combo
    .bigMoveEvery(2)
    .difficulty(1.0)
    .build();
```
2. `@Override public BossProfile brainProfile() { return BRAIN; }` (có thể trả null theo biến thể).
3. Chặn goal vanilla khi có profile (xem `SuDo.registerGoals`): `MeleeAttackGoal.canUse/canContinueToUse` và goal đi dạo khi có mục tiêu.
4. Logic chọn kỹ năng cũ trong `customServerAiStep`: bỏ qua khi `brainProfile() != null`; đưa vào `brainBigMove(sl, target)` (trả true nếu đã bắt đầu chiêu). Trong lúc chiêu chạy `idleForMove()` phải false (hoặc override `ownMoveActive()`), brain tự nhường.
5. Animation: thêm controller trong `registerControllers`:
```java
c.add(BrainAnims.controller(this, BRAIN, "animation.<id>.", Set.of("strafe_l", "dash", "combo_truong_1"), "attack", "walk", 4));
```
   Tên trong `Set` = animation **đã có** trong file `.animation.json`; tên còn lại tự rơi về `attack` (combo_*) / `walk`.
   Danh sách tên: `strafe_l strafe_r backstep sidestep_l sidestep_r backflip dash leap combo_<id>_<n>` (n từ 1).
6. Viết gametest theo `gametest/BossBrainGameTests.java` (nhớ ép chunk + gỡ barrier của khuôn `empty`).

## 3. Combo + nhánh rẽ

```java
new BossProfile.Combo("truong", List.of(
        BossProfile.hit(10, 3, 6, 2.8, 110, 0.9, 0.8),          // windup, active, recovery, reach, arc°, dmgMult, lunge
        // Hit(windup, active, recovery, reach, arc, dmg, lunge, recoil, delayed, heavy, unblockable)
        new BossProfile.Hit(10, 4, 14, 3.2, 140, 1.4, 1.2, 0.5, true, true, false)),
    0, 3.8,          // tầm chọn [min, max] (block)
    3, 1,            // trọng số, pha mở khóa
    List.of(new BossProfile.Branch(0, BossProfile.Cond.RETREAT, 2.5, "dam"),   // sau nhát 1: lùi > 2.5 ô
            new BossProfile.Branch(0, BossProfile.Cond.STRAFE, 50, "quet")),   // đi ngang > 50°
    false)           // branchOnly = true: chỉ dùng làm nhánh, không tự được chọn
```
- **windup**: boss bám mục tiêu (xoay ≤ turnRate). Glint trắng 6 tick trước khi vung; `unblockable` → glint đỏ + phá khiên.
- **delayed**: windup cộng thêm `delayExtra` (mặc định 10 tick), glint bật ngay đầu → nhát "trễ cố ý".
- **active**: khóa hướng, lao `lunge` block theo mặt; trúng đòn `heavy` → hit-stop 2-4 tick + rung camera (`Fx.shake`).
- **recovery**: đứng yên (lùi `recoil` block) — cửa sổ phản công; boss có thể né trong lúc này.
- Sát thương = `moveDamage(dmgMult) × difficulty`.

## 4. Kiểu áp sát riêng

```java
GapClose BLINK = new GapClose() {
    public String anim() { return "blink"; }
    public boolean step(ServerLevel sl, Mob b, BossMovement mv, LivingEntity t, int tick) {
        ... dùng mv.face / mv.drive(v, true) / mv.freeze ...
        return tick < 8;   // false = xong -> brain chọn combo
    }
};
```
Brain tự cắt sau 40 tick nếu `step` không kết thúc.

## 5. Phản ứng & né

| Nguồn | Dò ở đâu | Kết quả |
|-------|----------|---------|
| Người chơi vung tay trong 5.5 ô | `BossBrain.scanThreats` (cạnh lên `Player.swinging`) | roll `evadeChance` |
| Người chơi đánh trúng boss | `BrainEvents.onAttack` | roll né |
| Đạn bay tới | `scanThreats` (dự đoán đường bay) | sidestep |
| Kỹ năng người chơi | `SkillService.cast` → `BossBrain.notifySkillCast` | roll né |
| Ăn/uống thuốc | `BrainEvents.onUseFinish` | REACT → `punishCombo` (áp sát nếu xa) |
| Bị đánh N lần / T tick | `BrainEvents.onDamaged` | lướt ngang + glint đỏ → `counterCombo` |

Né chỉ xảy ra khi đang OBSERVE/STRAFE hoặc thu đòn, hết `evadeCd`. Pha ≥ 2: 30% thành backflip.

## 6. Còn chưa có / lưu ý

- Animation riêng (strafe, dash, backflip, combo_*) cho từng model: làm ở B2–B5. Hiện Sứ Đồ rơi về attack/walk.
- Brain không chạy khi director đang intro / chuyển pha / chiêu phase, và khi `ownMoveActive()`.
- Số đo debug công khai trên `BossBrain` (`evades`, `combosCompleted`, `branchesTaken`, `hitStops`, `allowAttacks`...) dành cho gametest.

## Document Lineage

| Version | Document | Status |
|---------|----------|--------|
| v1 | GUIDE-v1-boss-brain.md (this file) | ✅ Current |
