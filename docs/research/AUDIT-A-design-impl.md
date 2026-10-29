# AUDIT-A — Thiết kế (DESIGN-v2 / PLAN-v2 / TASK-v1) so với code thật

**Created**: 2026-10-29
**Last Updated**: 2026-10-29 — audit read-only lần 1; đối chiếu 3 tài liệu gốc + canon v2.1 (R1–R29) + S1–S8 với `src/main/java/com/hukhong` (559 file) và `src/test`
**Mode**: UC4/UC3 evidence audit · **Type**: AUDIT · **Language**: vi (thuật ngữ kỹ thuật giữ tiếng Anh)
**Phạm vi**: P0 (Bước 1–9), P1 (10–16), P2 (17–23), P3 (24–27) + mọi hệ thống/con số được hứa trong DESIGN-v2 master, canon, S2/S3/S6/S7 (khi cần số liệu).
**Không sửa file nào khác. Không commit.**

---

## Key Findings (Pyramid — kết luận trước)

| # | Finding | Độ tin cậy |
|---|---------|-----------|
| 1 | Phần lớn **P0–P3 có code + wiring thật** (gate, EXP, Lõi, Đan Lô, Tuổi, Tông, Thành, CHG, Khảo Hạch, Endgame). Không thấy event subscriber/payload nào bị treo (mọi `@SubscribeEvent` đều có `@EventBusSubscriber` hoặc `EVENT_BUS.register`). | CAO |
| 2 | **AP1 nặng nhất: Mắc Kẹt (1% ở ô Phó Mặc Số Phận) không có đường thoát** — nhiệm vụ "Phá Chấp" không tồn tại trong code; chỉ lệnh debug `core_unstuck` gỡ được. Người chơi dính là kẹt vĩnh viễn ở mốc đó. | CAO |
| 3 | **Đường Phá Cảnh Đan chỉ có phẩm 1–6**; mốc 70/80/90 (phẩm 7/8/9) và Thần Đan **không có item** → `FreeQuest.pillFor` trả `null`. Cả hành trình từ cấp 69 trở đi chỉ còn "EXP ×0,25". Linh Vật, Tẩy Tủy, Tăng Tu Vi, Thanh Tâm, Tịnh Hư, Độc Đan = MISSING (S7 hứa 45 loại, code ≈13). | CAO |
| 4 | Công thức Bình Cảnh khác doc: code = EXP ×0,25 trên **thanh của cấp 9 duy nhất**; doc = thanh Chướng bằng EXP **cả chặng 10 cấp** (≈ ×4 giờ chặng), có chuyển tiếp 50%, phẩm thấp −40%, **Bù Tư Chất (pity)**. Hệ số EXP theo tư chất (D0,6…SSS1,6) và phạt đa đạo (×1,6/×2,5) **không áp vào EXP**. Mô hình "110 h / 500 h" của S3 không còn hiệu lực. | CAO |
| 5 | Một số thay đổi **có chủ đích sau phản hồi chơi thật** (CHANGED): thêm đạo Pháp (5 đạo), Tử Tinh thay Linh Thạch, EXP quái theo % thanh cấp, Hiệp Hội Mạo Hiểm Giả, đói chậm ×5 cho mọi đạo, minimap/travel/skillbook/Linh Bảo. | CAO |
| 6 | Xã hội (S6) làm **xương sống đúng, thiếu tầng sâu**: chức vị thành chỉ có 1 trục điểm (không DV + ĐPT), Thành Chủ chiếm ghế "ai đủ điểm trước" và **không có quyền lực nào** (thuế/Quỹ/trục xuất/bầu/bất tín = MISSING); tập kích chỉ nhắm **thành #0 Lưu Ly Cảng**. | CAO |
| 7 | Tracker `TASK-v1` header **trôi nghiêm trọng (AP11)**: ghi "Code ⬜ P0 0/7 mốc" trong khi PLAN-v2 ghi "code xong Bước 1–27" và `git log` có 53 commit. | CAO |

**Cách đọc status**: DONE = code + callsite thật + ≥1 test (unit/GameTest). PARTIAL = thiếu wiring / thiếu test / thiếu một phần spec / số khác doc. MISSING = không có code. CHANGED = bị thay có chủ đích (bằng chứng trong code/commit).
**Quy ước thiếu-test**: UI/HUD thuần client không có test tự động ⇒ PARTIAL với ghi chú "chỉ thiếu test" (DoD mục 5 vẫn chờ user chạy thật theo PLAN §header).

---

## 1. Bảng audit

Viết tắt đường dẫn: `…/` = `src/main/java/com/hukhong/`; `T:` = `src/test/java/com/hukhong/`; `GT:` = GameTest.

### 1A. P0 — Bước 1–9

| # | Hạng mục (doc ref) | Status | Evidence (code / callsite / test) | Ghi chú |
|---|-------------------|--------|-----------------------------------|---------|
| 1 | B1 Khởi tạo NeoForge 1.21.1 + GeckoLib, mod hiện trong danh sách (PLAN §3) | DONE | `HuKhong.java:15-36` (đăng ký 14 registry); `src/main/resources/logo.png`; lang vi=en=1.564 key; `dist/mods/hukhong-1.0.0.jar` | Build/test xanh là claim của PLAN, chưa tự chạy lại (audit read-only). |
| 2 | B2 Attachment cấp/EXP/tài nguyên + sync + lệnh debug (PLAN §3) | DONE | `core/CultivationData.java`, `core/ModAttachments.java`; `network/SyncPayload.java:61`; `command/HuKhongCommand.java:40` (addExp), `:78` (core_unstuck); test `T:core/CultivationServiceTest.codecRoundTripDefault` | |
| 3 | B2 HUD cảnh giới / EXP / tài nguyên màu đạo / tuổi | PARTIAL | `client/hud/CultivationHud.java:45-47` (`registerAboveAll`), `:99` ("Bình Cảnh") | Chỉ thiếu test (client render). |
| 4 | B2 Hiệu ứng lên cấp (particle + âm) | PARTIAL | `core/CultivationService.java:121-128` (`levelUpEffect`) ← gọi từ `update()` `:98`; `breakthrough/BreakthroughEffects.java` ← `:99` | Chỉ thiếu test. |
| 5 | B2 Tuổi online (24.000 tick = 1 tháng, chặn AFK) (R1) | DONE | `age/AgeData.java:12-13`; `age/AgeService.java:39-46`; `age/AfkTracker.java` (AFK 5 phút, gọi `AgeService:41`); test `T:age/AgeMathTest` (5) | |
| 6 | B3 Màn tạo nhân vật + roll tư chất D→SSS (8/24/28/22/13/4/1) + đơn/song/tam 75/20/5 (canon §4) | PARTIAL | `charcreate/CharRoll.java:19-22,36-37` (đúng số doc); `CharCreateService.java:58` → `OpenCharCreatePayload` → `client/screen/charcreate/CharCreateScreen`; confirm `ConfirmCharCreatePayload` | Test chỉ phủ `TienThien` (`T:world/W2MathTest.tienThienRanges`); `CharRoll.roll` + weights chưa có test. |
| 7 | B3 Xuất thân 6 loại + tuổi khởi đầu 6–16 (canon §4, R13) | PARTIAL | `charcreate/Origin.java:8-9`; `CharRoll.java:19,42` (sàn C cho Hậu Duệ); `world/OriginSpawn.java:29-32` (spawn theo xuất thân); `T:world/W2MathTest.originKinds` | **Thiếu**: Thư Tiến Cử cho Đệ Tử Tông Môn (R13), debuff thật của Con Hoang, Di Dân Phế Tích không khác biệt (`Kind.NONE`). Các xuất thân chỉ đổi chỗ spawn + câu chuyện mở đầu. |
| 8 | B3 Hiệu ứng roll tư chất "sáng theo bậc" | MISSING | grep `roll.*particle/reveal` ở `client/` không có; lật thẻ chỉ có ở Ấn Hồn Đài (`CoreRevealPayload`) | Chưa tìm thấy VFX riêng cho màn tạo nhân vật. |
| 9 | B3/canon §4 **Hệ số EXP tư chất** D0,6 · C0,8 · B1,0 · A1,15 · S1,3 · SS1,45 · SSS1,6 | MISSING | `core/CultivationService.addExp:106` không nhân hệ số; `entity/HuHoa.exp:127-134` không đọc tư chất; grep `0.6/1.15/1.45` trong `core/` `stats/` không có | Tư chất chỉ ảnh hưởng chỉ số/Lõi/tiên thiên (`TienThien.java`), **không** ảnh hưởng tốc độ lên cấp. `society/SectMath.java:28 APT_MULT` là hệ số khác (mời tông). |
| 10 | B4 Đạo tu: đòn thường lên quái = 0, lên người = ×1 (canon §3) | DONE | `gates/DamageRules.java:79`; GT `gametest/HuKhongGameTests.daoDanhZombieBangKhong:35` | |
| 11 | B4 Đạo tu "không cần ăn" (canon §3) | PARTIAL | `pathrules/PathRules.java:110` (`dao → setFoodLevel(20)`); thêm `world/HungerRules.java:16` (đói chậm ×5 cho mọi đạo) | Có code, chưa có test. Đói chậm ×5 là thêm sau (commit `3d223b9`). |
| 12 | B4 Thể tu: cấm vũ khí, đòn tay = Công×(1+1,8×Bá Khí%) (canon §3) | DONE | `gates/DamageRules.java:23,59-76`; `pathrules/TheBody.java`; GT `gametest/TrongThuongGameTests`; `T:passive/HuyetKhiTest` | Hệ số 1,8 (canon doc từng ghi 0,3 trong comment `:57` cũ; code = 1,80). |
| 13 | B4 Kỹ năng 3+1, phím Q·E·R·T, G bật/tắt Thế Tu Luyện (R25) | DONE | `client/keybind/SkillKeys.java:18-26`; `SkillKeyHandler.java:27-36`; `skill/SkillService`; GT `HuKhongRuntimeGameTests.skillCase:163-202` (mỗi đạo); `T:skill/SkillPagesTest` | |
| 14 | B4 Mỗi đạo 2 kỹ năng + 1 tối thượng có hiệu ứng (PLAN §3) | DONE | `skill/SkillEffects.java`; `client/fx/FxEffects.java`; GT `skillCase` (HuKhongRuntimeGameTests:187-202) | Hơn kế hoạch: 19 kỹ năng sách (`skillbook/*`), Thiểm Bộ, Tinh Hà Quang Trụ. |
| 15 | B4 Thiền (tư thế + hồi tài nguyên) | PARTIAL | `pathrules/PathRules.java:114` (`meditationMult`); `phaptu/PhapRuntime.java:446` | Chỉ thiếu test thiền riêng. |
| 16 | B4 Spawner = 0 EXP/Hồn Tinh/vật liệu (canon §1.1, R3) | DONE | `gates/GateEvents.java:23-28` (mark `SpawnerMarks.MARK`); `gates/RewardGate.java:11-14`; GT `HuKhongGameTests.spawnerMobKhongChoExp:56` | |
| 17 | B4 `KillRateGate` >30 con/64 block/10 phút → ×0,1 (R3) | PARTIAL | `gates/KillRateGate.java:15-34` (COUNT 30, WINDOW 12.000 tick, RADIUS 64, FLOOR 0,1 — **đúng số doc**); callsite `RewardGate.reward:14` ← `GateEvents.java:34`, `entity/HuHoa.java:180` | **Không có test nào** (grep `KillRateGate` trong `src/test` và GameTest = 0). Hàng đợi `KILLS` toàn cục theo loài, không theo người chơi. |
| 18 | B5 Đường cong EXP `E(L)=40·L^2.15`, khóa mốc 10/20…+95/99/100 | DONE | `core/CultivationService.java:15-17,39-58`; `core/CultivationData.java:67,78`; `T:core/CultivationServiceTest` (expCurve, milestoneLockAndBreakthrough) | Khớp S3 §1.1. |
| 19 | B5 Bình Cảnh cấp 9/19…/89: EXP ×0,25 (canon §1.1; S3 §1.3) | PARTIAL | `CultivationService.java:18,44`; `CultivationData.inBottleneck:81`; `T:CultivationServiceTest.bottleneckQuartersExp` | **Số khác doc**: doc = thanh Chướng = EXP cả chặng 10 cấp (≈4× giờ chặng, S3:50,104-110); code = ×0,25 trên thanh cấp 9 (≈4× của 1 cấp). Thiếu chuyển tiếp 50%, "phẩm thấp 1 bậc −40% / ≥2 bậc vô hiệu / cao hơn dùng được" (code bắt **đúng phẩm**: `CultivationService.tryBreakthrough:82-86`). |
| 20 | B5 **Bù Tư Chất** pity Bình Cảnh (R2) | MISSING | grep `pity|Bù Tư Chất` chỉ trúng `honcot/` (pity Hồn Cốt) | D không vật liệu "≈633 h" của doc không được bảo đảm. |
| 21 | B5 Phạt EXP đa đạo: song ×1,6, tam ×2,5 (canon §3.1, S3:66) | MISSING | grep `1.6|2.5` ở `core/ stats/ pathrules/` = không có; `addExp` không đọc `paths().size()` | Chi phí tài nguyên ×2/×3 thì có (hàng 40). |
| 22 | B5 Khai Mạch Đan + Ngưng Nguyên Đan (Phá Cảnh 1–2), bind | DONE | `item/ModItems.java:34-39`; `item/PhaCanhDanItem.java:39-45` (bind người nhặt đầu tiên), `:47-60`; nguồn: `alchemy/AlchemyRecipes.java:21-24`, `bicanh/BiCanhLoot.java:34-37`, `story/freequest/FreeQuest.java:41-52`; GT `danLoBrewProducesPill` | `item/PillDrops.java` (zombie/skeleton rơi 2%/1%) **gần như chết**: `world/SpawnRules.java:44-48` chặn quái vanilla thù địch tự nhiên ⇒ không có zombie/skeleton hợp lệ để rơi (xem Top gaps #9). |
| 23 | B5 Hiệu ứng đột phá (cột sáng, rung, âm) | PARTIAL | `breakthrough/BreakthroughEffects.java` ← `CultivationService.java:99` (mỗi mốc ×10) | Chỉ thiếu test. |
| 24 | Nguồn EXP quái: `E(L)/200` mỗi con (≈52 con/cấp, S3:245) | CHANGED | `entity/HuHoa.java:124-141` (3% → 1,6% của `expToNext`, ≈30 con/cấp; ×0,1 nếu thấp ≥10 cấp); commit `3d223b9` "EXP quái nhân theo cấp"; `T:entity/HuHoaExpTest` (5) | Có chủ đích (phản hồi chơi thật). Tổng giờ lên 100 thay đổi so với mô hình Fermi. |
| 25 | B6 Ấn Hồn Đài 4 ô: 3 Lõi cùng phẩm + Phó Mặc Số Phận, seed lưu trước (R8) | DONE | `cores/CoreRoller.java:142-165`; `cores/CoreService.java:80-97` (`withPending` lưu **trước** khi mở UI), `:99-125`; `block/AnHonDaiBlock`; `client/screen/core/CoreAltarScreen`; `T:cores/CoreRollerTest` (8) | |
| 26 | B6 32 Lõi + phẩm Bạch→Kim + Điểm Vận | DONE | 36 file `src/main/resources/data/hukhong/hukhong/core/*.json` (32 Lõi + Thần Lõi theo đạo); `cores/CoreTier`; `cores/CoreRegistry`; `T:CoreRollerTest.jsonParsesAndRollIsDeterministic` | Doc: 32; thư mục có 36 (gồm 4 Thần Lõi + Lõi mới). |
| 27 | S3 §3.1 Hệ (set) bonus 2/4/6 món, 8 Hệ | PARTIAL | `cores/CoreService.java:155-164,193-213` (`SET_BONUS`: chỉ **6** hệ: liet_diem, huyet_ma, tinh_than, cuong_the, phong_loi, …; ngưỡng **2 món**) | Thiếu ngưỡng 4 và 6 món; 6/8 Hệ có bonus. Không có test cho set bonus. |
| 28 | B6 Thần Lõi cấp 100 + bộ chỉ số Thần (R29) | DONE | `cores/CoreRoller.java:104`; `CoreRoller.THAN_MILESTONE`; `T:CoreRollerTest.thanCoreOnlyAtMilestone12AndOnlyOwnPath` | |
| 29 | B6 **Mắc Kẹt 1% → nhiệm vụ Phá Chấp** (G2, canon §5) | PARTIAL | Roll: `CoreRoller.java:47,149`; trạng thái: `CoreService.java:110-116`, `ensurePending:86` (`stuckAt==m → return null` ⇒ không bao giờ mở lại UI); lang `hukhong.altar.stuck(_blocked)` ; **chỉ** `command/HuKhongCommand.java:78` (`core_unstuck`) gỡ được | **AP1 HIGH**: không có quest Phá Chấp, không có callsite nào gọi `withStuckCleared`/`withStuck(0)` từ gameplay. Dính = kẹt vĩnh viễn ở mốc (EXP khóa). |
| 30 | B6 Tẩy Hồn + Luân Hồi Hoa + Tẩy Hồn Thạch (canon §5, R9, S7 §6) | MISSING | grep `Tẩy Hồn|TayHon|luan_hoi` trong java = 0 (chỉ chuỗi cảnh báo lang `hukhong.altar.warning`) | Lõi "vĩnh viễn" nhưng không có đường đổi. |
| 31 | B6 Bảng tu sĩ (K): chỉ số, Lõi, kỹ năng, nội tại | DONE | `client/keybind/SkillKeyHandler.java:36` → `client/screen/profile/ProfileScreen`; `T:client/SkillEntriesTest` (6), `T:phaptu/PhapPassiveEntriesTest` | |
| 32 | B7 3 quái Hư Hóa (Thi/Cốt/Chủ) + loot Hồn Tinh + EXP, GeckoLib | DONE | `entity/HuThi|HuCot|HuChu`; `entity/ModEntities.java:33-43,77-81` (spawn placement); `HuHoa.onDeath:177-190`; GT `entityHuThi/Cot/Chu`, `killHuThiGivesExp:132` | Nhiều hơn kế hoạch: HuAnh, HuGiap, HuTrung, HuTrieuSu, SaHatVuong, BangNhan, DocMau (GT `MobBuffGameTests`). |
| 33 | B7 Thanh máu/tên quái có cấp | PARTIAL | `client/combatui/CombatUiHud.java`, `CombatTextEvents` | Chỉ thiếu test. |
| 34 | B8 Thành đầu tiên: Ấn Hồn Đài + SavedData + tập kích thử | DONE | `city/CitySavedData.java:18-36`; `city/CityBuilder.java`; `events/InvasionManager.java:111-119` (`start`); GT `siege/SiegeGameTests.guardsTargetVoidMobWhenSiegeStarts`; `T:city/PlacementTest` | |
| 35 | B8 Thông báo sự kiện (title/bossbar) | DONE | `InvasionManager.java:87,96` (title + `WARN_BAR`); GT `MobBuffGameTests.raidStartsAndCleansUp` | |
| 36 | B8 Onboarding 5 bước hướng dẫn | PARTIAL | `onboarding/OnboardingService.java:31-42` (5 case) → `network/OnboardingHintPayload` → `client/onboarding/OnboardingClient` | Không có test (grep `OnboardingService` trong src/test = 0). |
| 37 | **R21 Mở khóa dần theo cảnh giới** (Thường Dân chỉ thấy EXP/đạo/tư chất; Lõi từ 10; Sứ Đồ từ 30…) | MISSING | grep `R21|mở khóa dần` = 0; `ProfileScreen` hiển thị mọi tab ngay | Onboarding hint ≠ ẩn hệ thống. |
| 38 | B9 Tùy chọn client: tắt hiệu ứng toàn màn hình (R26), particle, mù màu, HUD | PARTIAL | `client/ClientConfig.java:18-35` (disableScreenEffects, colorblind, particles, showHud) ; callsite `ClientShake`, `FxClient` | Chỉ thiếu test. |
| 39 | B9 **Pixel font tiếng Việt** (R26) | MISSING | `src/main/resources/assets/hukhong/font/` chỉ có `icons.json` | |
| 40 | B9 Multiplayer 2 client nghiệm thu | MISSING | Không GameTest/cấu hình 2 client; PLAN header "CHỜ user chạy thật" | Mọi ghi chú multiplayer trong bảng này là suy từ code, chưa kiểm thực tế. |
| 41 | B9 Lang vi/en đầy đủ, không key thiếu (PLAN B9) | PARTIAL | vi=en=1.564 key; nhưng **44 key được code tham chiếu mà không có trong `vi_vn.json`**: 11 key `hukhong.map.*` (`client/minimap/FullMapScreen.java:38,129,133,157,169`, `MapEvents.java:92,107,115`, `MinimapState.java:23`), `hukhong.qc.merchant.s2` (`story/chain/QuestChains.java:254`), `hukhong.banmenh.msg.full`… (một số có `translatableWithFallback`) | Bản đồ lớn/minimap sẽ hiện **key thô**. Một phần còn lại là key động (`hukhong.passive.*`) — cần soát riêng. |

### 1B. P1 — Bước 10–16

| # | Hạng mục (doc ref) | Status | Evidence | Ghi chú |
|---|-------------------|--------|----------|---------|
| 42 | B10 Kiếm tu + Bản Mệnh Kiếm (random, không rơi khi chết) | DONE | `banmenh/*` (13 file); `item/ModItems.java:25-33`; GT `BanMenhGameTests` (`kiemKhongDoBen:45`, `mat_kiem_thi_tai_tao:90`, `khongVutDuocQvaCtrlQ:59`); `T:banmenh/BanMenhMathTest` | Còn có tiến hóa bằng Hồn Tinh (commit `1879a27`). |
| 43 | B10 Ma tu: Ác Tính, Huyết Tế (chỉ NPC, quota), 2 lộ, Mất Kiểm Soát, Ác Danh (R17) | DONE | `pathrules/MaRules.java:58-62,124,152-191`; `MaHud`, `client/screen/ma/MaLoScreen` (`SetMaLoPayload`); `T:pathrules/MaRulesTest` (3) | Dân làng nhân giống ×0,1: `MaRules.BRED_MULT:58`, tag gán ở `gates/GateEvents.java:21`. |
| 44 | B10 Tẩu Hỏa Nhập Ma = phế tu + reset + làm lại từ đầu (R23) | PARTIAL | `pathrules/MaRules.java:363-385` (reset CULTIVATION/CORE_SLOTS, `CharCreateService.reroll`) ← `:243,246` | `MaRulesTest` **không** test `tauHoa`; không GT. Comment `:359` nói "Hồn Cốt chưa có hệ thống" — stale (Hồn Cốt có từ Bước 17), Hồn Cốt đang đeo có bị gỡ khi reset không: chưa thấy `HonCotService.unequip` trong `tauHoa`. |
| 45 | B10 Ma tu chỉ PvP khi hai bên "chiến" (Q6); khiêu chiến Thành Chủ PvP tắt (Q7) | MISSING | grep không có cờ "chiến" | 8 câu hỏi nhỏ §4.3 DESIGN vẫn "chờ user"; không chặn code. |
| 46 | B11 Đa đạo: chi phí ×2/×3 | PARTIAL | `gates/Resources.java:14-19` (`pathMultiplier`); callsite `Resources.tryConsume`/`costFor` ×14 | Không test trực tiếp. |
| 47 | B11 Trần **Phân Tâm** (song ×0,8 công, tam ×0,7) (Q4) | MISSING | grep `Phân Tâm|0.8f|0.7f` không trúng logic | Commit `ea1f999` nói "Phân Tâm", nhưng không có hằng số. |
| 48 | B11 Bay 4 đạo: Kiếm 20 / Đạo·Thể·Ma 60, chi phí tài nguyên | DONE | `flight/FlightMode.java:7-11`; `FlightService`, `FlightKeys` (Z); GT `flightBannedNearWorldBoss:232`, `flightCase:214` (mỗi đạo); `FlightBans.java:26` | Thêm PHAP (cấp 60). |
| 49 | B11 Ngự kiếm / cánh thiên sứ 1→2→3 đôi / đạp không | DONE | `flight/client/FlightLayer`, `client/render/artifact/WingRenderer.java:62`, `artifact/ArtifactMath.java:24` (`wingPairs`); `T:artifact/ArtifactMathTest` | |
| 50 | B12 Đan Lô + minigame nhiệt + công thức | DONE | `alchemy/DanLoBlock`, `DanLoBlockEntity`, `DanLoMenu`, `client/screen/alchemy/DanLoScreen.java:89` (đăng ký màn); recipe `data/hukhong/recipe/dan_lo.json`; GT `danLoBrewProducesPill:251`; `T:alchemy/AlchemyMathTest` | |
| 51 | B12 Đan Độc (cộng/giải/hiệu quả/bùng phát) | DONE | `alchemy/DanDoc.java`, `AlchemyMath.java:34-45`; `UtilityPillItem.java:35`; GT `pillAddsDanDocAndSurvivesDeath:279` | `effectMult` chỉ áp cho đan tiện ích, Phá Cảnh Đan chỉ bị chặn khi ≥100. |
| 52 | B12 Dị Hỏa / Đan Hỏa / Hồng Mông Lô / lô phẩm 1–9 (S7 §4.1-4.2) | PARTIAL | Chỉ `AlchemyRegistry.DI_HOA_CHAU:70` + recipe `di_hoa_chau.json`; `AlchemyMath.bandHalf:25` | Chỉ 1 loại lô, 1 vật Dị Hỏa Châu; không có tầng Đan Hỏa/lô phẩm. |
| 53 | B12 / S7 §2.A Phá Cảnh Đan **9 phẩm + Thần Đan** | PARTIAL | Chỉ phẩm 1–6: `ModItems:34-39`, `AlchemyRegistry.java:68-71`; `FreeQuest.pillFor:43-52` trả `null` cho phẩm 7/8/9 | **Chặn đường Bình Cảnh mốc 70/80/90** (xem Top gaps #2). |
| 54 | S7 §2.A2 **Tiểu Phá Đan** (7 loại, mốc 91–95/96/99; R6) | PARTIAL | Chỉ 2: `ModItems.TIEU_KINH_DAN_V:43`, `TIEU_BAN_THAN_DAN:46`; Bình Cảnh nhẹ chỉ ở cấp 94 & 98 (`CultivationService:21-23`) | Nguồn: BiCanhLoot + FreeQuest. Test `CultivationServiceTest.lightBottleneckBar3xAndPill`. |
| 55 | S7 §2 nhóm B/C/D/E/F/G/H: Hồi Phục (5), Tăng Tu Vi (6), Thọ Nguyên (2 họ), Tẩy Tủy (2), Thanh Tâm (4), Tịnh Hư/Giải Độc (3+2), Độc Đan (4) | PARTIAL | Có: Chỉ Huyết/Hồi Xuân/Giải Độc/Thọ Nguyên (`AlchemyRegistry.java:73-84`, `UtilityPillItem.Kind {HEAL, DETOX}`). **Không có**: Tăng Tu Vi, Tẩy Tủy, Thanh Tâm, Tịnh Hư, Độc Đan (grep `TayTuy|TinhHu|Thanh Tâm Đan` = 0) | Code ≈13 loại / doc 45. |
| 56 | S7 §3 Thiên Địa Linh Vật (cấp 1–9) + Linh dược niên | MISSING | grep `Linh Vật|LinhVat|Thiên Địa Linh` = 0 (chỉ 4 thảo dược `linh_thao/huyet_tham/hoa_lien/thanh_diep`) | Canon §1.1 "Phá Cảnh Đan **HOẶC** Linh Vật" chỉ còn đường đan. |
| 57 | R5 Bind: Phá Cảnh Đan, **Thọ Nguyên Đan**, Tẩy Tủy, Thần Đan | PARTIAL | Phá Cảnh Đan bind ✓ (`PhaCanhDanItem:39-45`); `age/ThoNguyenDanItem.java` **không** dùng `BOUND_TO`; `currency/ShopCatalog` bán THO_NGUYEN_DAN | Thọ Nguyên Đan giao dịch được. |
| 58 | B13 Thọ Nguyên theo cảnh giới (50…1000) + Thiên Nhân Ngũ Suy I–V, R1 | DONE | `age/AgeMath.java:8,20-24,28-48` (khớp bảng S6 §1.4 từng số); `AgeService.java:73-95`; `FlightBans:26`; `SectEvents:177`; `PathRules:114`; `T:age/AgeMathTest`; GT `pillAddsDanDocAndSurvivesDeath` (qua chết) | |
| 59 | B13 Ngũ Suy III "giá chợ +20%", IV "NPC tránh nói chuyện" (S6 §1.5) | MISSING | `AgeService.stageOf` chỉ được gọi ở flight/thiền; không trong `currency/` `city/` `story/` | |
| 60 | B13 Cảnh báo tuổi + hiệu ứng già yếu | PARTIAL | `AgeMath.warnKey:42` ← `AgeService:53`; particle già `AgeService:51` | Thiếu test warn dịch lên. |
| 61 | B14 10 thành cố định (tên + cấp Nhân/Địa/Thiên) | DONE | `city/CityDef.java:9-18` (10 thành, 3 Địa + 1 Thiên đúng canon §8); `CityBuilder`, `Prebuild`; `T:city/PlacementTest`, GT `siege/*` | Tọa độ giãn ×2 theo R24: `Placement.WORLD_RADIUS=15000` (#75). |
| 62 | B14 Chức vị Cư Dân→…→Thành Chủ + yêu cầu cảnh giới 10/30/50… | PARTIAL | `city/CityRank.java:7-18` (6 chức, `LEVELS` 20/40/60 cho Đội Trưởng ✓); `CityService.java:105-122`; `T:city/CityRankTest` (6) | **Một** trục điểm (`BASE_POINTS {0,100,300,700,1200,2000}`), không có **Danh Vọng + Điểm Phòng Thủ** riêng (S6 §3.2: DV 500…8.000, ĐPT 100…2.000); không "dẫn ≥2 raid" v.v. |
| 63 | B14 Quyền lợi Thành Chủ: thuế chợ 0–10%, Quỹ Thành, Kho Thành, ban danh hiệu, trục xuất, bất tín nhiệm, bầu (S6 §3.3) | MISSING | grep `thue|tax|treasury|quy_thanh|bat_tin` trong `city/` = 0; `CityRegistryData.claimLord:63-68` ai đủ điểm trước giữ ghế | Thành Chủ chỉ có danh vị. Q7 "dùng bầu/đóng góp": chưa có. |
| 64 | B14 7 tông, 3 cấp, ứng tuyển (tư chất+cảnh giới+tuổi), tuổi tối đa Thiên≤40/Địa≤45/Nhân≤55 (R14) | DONE | `society/SectDef.java:15-22`; `SectService.checkApply:36-40`; `SectMath.check` ; `SectMenu`; `T:society/SectMathTest` (15) | Số tuổi tối đa khớp R14. |
| 65 | B14 Bậc đệ tử Ngoại→Nội→Chân Truyền→Trưởng Lão, Cống Hiến, Đại Bỉ | DONE | `SectMath.rankFor:34`, `RANK_CH:23`; `SectData.daiBi`; `SectTrial.java`; `SectMath.daiBiNeeded:31` (không có caller — helper thừa, không ảnh hưởng) | |
| 66 | B14 Buff tông môn | DONE | `society/SectBuffs.java` (10 callsite) | |
| 67 | B14/R22 **Linh Khí Đài** (Ngoại×1/Nội×1,5/Chân Truyền×2, Bế Quan 0,1 E(L)/giờ) | DONE | `society/LinhKhiDaiBlock`, `SocietyRegistry.java:34`; `SectMath.java:24,92-93`; `SectEvents.java:177` ; `AltarSeats` ; `T:SectMathTest` | |
| 68 | B14 Thư Tiến Cử / sứ giả mời tông (sự kiện qua mốc, Nhân 12%/Địa 6%/Thiên 2%) | PARTIAL | `SectMath.INVITE_BASE:27`, `SectData.invite`, `SectMenu.B_ACCEPT` | Invite theo mốc có; **Thư Tiến Cử vật phẩm** (xuất thân Đệ Tử, bỏ giới hạn tuổi, R13/R14) không tồn tại. |
| 69 | B14 Màn Tông môn / Thành / bảng nhiệm vụ | PARTIAL | `client/screen/society/SocietyScreens.java:29,50`; `SectBoardBlock`, `CityBoardBlock` ; `client/screen/guild/GuildScreens.java:31` | Chỉ thiếu test UI. |
| 70 | B15 Tập kích thành theo Xâm Thực (trần), hoãn khi không online | PARTIAL | `events/InvasionManager.java:81-120`; `CorruptionManager` (7 callsite); GT `MobBuffGameTests.raidStartsAndCleansUp`, `raidWavesSpawnWithMiniBoss`, `LiberationGameTests.tapKichXacSuatVaCap` | **Chỉ nhắm thành #0** (`CitySavedData.get(ow)`, `:82-84,113-115`); 9 thành còn lại không bao giờ bị tập kích/mất. Hoãn khi vắng người: `:137` (kết thúc nếu `players().isEmpty()`) — không phải "hoãn theo cư dân/chức vị". |
| 71 | B15 Phế Tích ⇄ tái chiếm; kho riêng giữ (canon §8) | PARTIAL | Mất: `InvasionManager.lose:238-244`; `CityBuilder.ruin:375`; tái chiếm: `LiberationManager` (4 callsite), `story/flow/StoryFlow.java:462-470` | Kho riêng người chơi giữ khi mất thành: không có code. |
| 72 | B15 Hư Hóa lan chunk + buff quái | DONE | `events/CorruptionManager.java`, `CorruptBuff`; GT `MobBuffGameTests.corruptedChunkBuffsMobs:206` | |
| 73 | B15 Boss scaling `HP×(1+0,75(n−1))`, n tối đa 10, n = người gây ≥5% sát thương/hồi máu trong 48 block (R16) | PARTIAL | `events/BossScaling.java:7-11` (**MAX_PLAYERS = 8**, hệ số 0,75 ✓); đếm n bằng `getEntitiesOfClass(Player.class, bb.inflate(64/80))`: `entity/WorldBoss.java:320`, `HuVuong.java:269`, `chg/ChapHanhGia.java:427`, `endgame/NguyenThuyTichDiet.java:405`; `T:events/BossScalingTest` (4) | 3 sai khác: trần 8≠10, bán kính 64/80≠48, đếm mọi người trong vùng (không lọc đóng góp ≥5%) ⇒ exploit "đứng gần" tăng HP không có lợi, nhưng ngược lại 1 người đóng góp + 7 người đứng nhìn làm boss ×6. Loot "mỗi người 1 lượt roll": không thấy code. |
| 74 | B15 Boss thế giới cấm bay (canon §10) | DONE | `entity/WorldBoss.java`, `events/WorldBossManager.java` (9 callsite); `flight/FlightBans`; GT `flightBannedNearWorldBoss:232` | |
| 75 | B15 Worldborder 15.000 từ điểm spawn (R24) | PARTIAL | `events/CorruptionManager.java:35,48-52` (`BORDER_RADIUS=15000`, `setSize(2*R)`); `city/Placement.java:9 WORLD_RADIUS` | Không test (grep). |
| 76 | B15 Boss Hư Vương + animation pha | DONE | `entity/HuVuong`; `BossBuff`; GT `BossDamageGameTests.playerDamagesHuVuong:93`, `entityHuVuong` | |
| 77 | B16 Rà UI/đánh bóng P1 | PARTIAL | commit `5cde212` ("rà lỗi P1–P3"); `docs/research/REVIEW-v2-p1-bugs.md` | Không kiểm chứng độc lập. |

### 1C. P2 — Bước 17–23

| # | Hạng mục (doc ref) | Status | Evidence | Ghi chú |
|---|-------------------|--------|----------|---------|
| 78 | B17 Hồn Cốt 6 slot (đầu/thân/2 tay/2 chân) | DONE | `honcot/BoneSlot.java:5-7`; `HonCotService` (18 callsite), `HonCotItem.java:57` (dùng để mang), `BoneUnequipPayload.java:24`; nguồn: `honcot/HonCotEvents.java:152-156` (boss 25% + pity), `bicanh/BiCanhLoot.java`; `T:honcot/HonCotMathTest` (8), `HonCotBonusTest` | |
| 79 | B17 Kỹ năng Bản Mệnh (A+) dùng ô Tối thượng | DONE | `skill/BanMenhSkill`, `BanMenhSkillEffects`; `T:skill/BanMenhSkillTest` | Q5 "chọn 1/2": chưa xác minh. |
| 80 | B18 7 tông đủ + Huyết Ảnh ẩn | DONE | `SectDef.java:22` (`hidden=true`), `SectBuilder` ; `T:SectMathTest` | |
| 81 | B18 Đại Bỉ NPC khảo quan | DONE | `SectData.daiBi`, `SocietyScreens.java:132,140`, `SectTrial` | Có thể thiếu test riêng cho Đại Bỉ ngoài `rankFor`. |
| 82 | B19 Sứ Đồ ≤65 (3 biến thể), Văn Kiện 6/14/28/55% (R15) | DONE | `story/SuDo.java:45,74`; `story/StoryService.java:24` (MAX_LEVEL 65); `story/StoryDocs.java:20` (`CHANCE {0.06,0.14,0.28,0.55}` ✓); `StoryRegistry.java:44-47` (item mỗi Văn Kiện); `VanKienItem`; GT `entitySuDoKiem/Chu/Te`; `T:story/chain/QuestChainsTest` | |
| 83 | B19 Màn Nhật ký truyện (J) | DONE | `client/story/StoryKeys.java:29` → `JournalScreen`; `story/StoryNet` | Chỉ thiếu test UI (PARTIAL-ish); tính DONE vì logic truyện có GT `StoryFlowGameTests`. |
| 84 | B19 Tố cáo nội gián trong tông (Q3) | DONE | `story/chain/QuestChains.java:333` (chain `NOI_GIAN`); `T:QuestChainsTest` | |
| 85 | B20 4 Chấp Hành Giả 67/78/86/98 + giáng lâm + hang ổ | DONE | `chg/ChgKit.java:37-49`; `ChgService.descend:29`, `eventTick:110`, `lairTick:86`; `ChgRegistry`; `T:chg/ChgKitTest` (7); GT `entityChg1-4`, `BossDamageGameTests.playerDamagesChg:99`, `chgSkillCompletes:136` | |
| 86 | B20/R19 Kích hoạt sớm sự kiện CHG cần party ≥2 **hoặc** cảnh giới ≥ cấp CHG−5; manh mối chung server | PARTIAL | `chg/ChgService.earlyEvent:63-68` + gọi từ `story/StoryService.java:69` — chỉ kiểm "đủ 6 Văn Kiện cùng chuỗi"; `ChgKit.lairEligible:102` (cấp −15) | Không kiểm party/cảnh giới tại `earlyEvent`. "Chung server" (R19): `StoryService.get(p)` là **theo người chơi** (`lair` mask per-player). |
| 87 | B21 Bí Cảnh dimension + hang bản đồ | DONE | `bicanh/*` (13 file, 36 callsite `BiCanhManager`), `data/hukhong/dimension/bicanh.json`; `T:bicanh/BiCanhTest`; GT `biCanhEnterLeaveReturnsPosition:302` | |
| 88 | B22 Tuyến truyện chính + hội thoại NPC | DONE | `story/flow/StoryFlow.java` (34 callsite), `story/npc/*`, 12 file `data/hukhong/hukhong/dialogue/*.json`; GT `StoryFlowGameTests` (6), `T:story/flow/FlowStepsWalkTest` | |
| 89 | B22 Dạ Khuyết boss ẩn Hồi 8 (Q1), Mộ Dao hy sinh (Q2) | PARTIAL | `endgame/DaKhuyet.java` + GT `entityDaKhuyet`; `dialogue/mo_dao.json` | Dạ Khuyết xuất hiện trong arena cuối; "ẩn tùy chọn ở Hồi 8" & "hy sinh tùy lựa chọn" chưa thấy code. |
| 90 | B23 Đánh bóng P2 (cánh 2–3 đôi, kỹ năng cấp cao, màn Lộ Ma) | DONE | `ArtifactMath.wingPairs`; `skillbook/*` (18 file); `MaLoScreen`; commit `1879a27` | |

### 1D. P3 — Bước 24–27

| # | Hạng mục (doc ref) | Status | Evidence | Ghi chú |
|---|-------------------|--------|----------|---------|
| 91 | B24 Khảo Hạch: Tâm Ma + Lôi Kiếp (cá nhân, R18) | DONE | `trial/TrialService.java:72` (`begin`) ← `core/CultivationService.unlockMilestone:92`; `TrialMath`; `client/TamThucClient`; GT `trialLoiKiepRunsToCompletion:365`, `trialTamMaMovesToTamThucAndBack:391`; `T:trial/TrialMathTest` (6) | Áp từ mốc **90+** (90/95/99/100), rộng hơn "Khảo Hạch Thăng Thần" 1 lần. "Tối đa 3 hộ pháp" chưa thấy. |
| 92 | **Thăng Thần khóa sau cốt truyện** (cần hạ đủ 4 CHG, cả chế độ tự do — DESIGN §4.2, Q8) | PARTIAL | `endgame/EndgameKit.java:10,18` (`allChgDefeated` chỉ gate **cổng Hư Không Bản Nguyên**) ; `CultivationService.applyExp` không kiểm `lair` | Người chơi lên **cấp 100 không cần hạ CHG nào**; gate chỉ chặn việc vào arena boss cuối. |
| 93 | 4 Mảnh Thiên Môn (vật phẩm) | MISSING | Không có item/component; trạng thái chỉ là bitmask `StoryData.lair` | |
| 94 | R7 Thông Thiên Thần Đan để Thăng Thần | MISSING | grep `ThongThien|Thần Đan` = 0 | |
| 95 | B25 Nguyên Thủy Tịch Diệt + dimension Hư Không Bản Nguyên | DONE | `endgame/NguyenThuyTichDiet`, `EndgameArena`, `EndgameService.openGateway:47`; `data/hukhong/dimension/hu_khong_ban_nguyen.json`; GT `entityNguyenThuyTichDiet`, `BossDamageGameTests.playerDamagesNguyenThuyTichDiet:86`; `T:endgame/EndgameKitTest` (6) | |
| 96 | B26 Kết thúc Phong Ấn / Hấp Thụ + cutscene + credits | PARTIAL | `EndgameService.choose:135-145`, `finish:150-160`; `client/endgame/EndingChoiceScreen|CutsceneScreen|CreditsScreen` | Hai kết thúc **chỉ khác text/cutscene** (`F_SEAL`/`F_ABSORB` chỉ dùng cho message `:164`); không khác biệt thế giới; thiếu **Bia Công Trạng** (chỉ có chuỗi lang) và "Hấp Thụ để lại Hư Hóa ngoại hình" (R18). |
| 97 | B26/R18 **Tầng Thế Giới II = dimension mới**, kết thúc là lựa chọn cá nhân | PARTIAL | `endgame/WorldTier.java`, `WorldTierData`, `EndgameKit.java:22-24` (HP×1,5, DMG×1,3, reward×1,5); `EndgameService.choose:136` (`WorldTier.raiseTo(server,2)`); GT `worldTierScalesHuHoaStats:342` | Là **hệ số toàn thế giới**, không phải dimension; lựa chọn là cá nhân nhưng nâng tier cho cả server. |
| 98 | B27 Cân bằng 1–100, tối ưu, đóng gói | PARTIAL | `T:balance/BalanceSimTest`, `tools/balance_sim.py`, `docs/research/REPORT-v1-balance.md`; commit `d16e4e4` ("~506h tới 100"); `dist/mods/hukhong-1.0.0.jar` + `dist/shaderpacks` | Mô phỏng, chưa playtest thật; kết quả ~506h giả định có đủ đan (phẩm 7–9 không tồn tại). |
| 99 | B27 Giấy phép nhạc/ambient | DONE | `docs/research/CREDITS-music.md`, `LICENSES-music.md`, `LICENSES-ambient.md`; `audio/BossMusic` + `client/audio/ClientBossMusic` | |

### 1E. Luật/số liệu xuyên suốt (DESIGN §2–§3, canon §0–§14) và thay đổi có chủ đích

| # | Hạng mục (doc ref) | Status | Evidence | Ghi chú |
|---|-------------------|--------|----------|---------|
| 100 | DESIGN §3 / canon §3: **4 đạo** (Kiếm/Đạo/Thể/Ma) | CHANGED | `core/Path.java:8-22` (**5** đạo, thêm `PHAP`); `phaptu/*` (11 file), commit `f4321e8` "Pháp tu 6 hệ + màn chọn hệ" | Mọi tài liệu thiết kế vẫn nói 4 đạo ⇒ cần cập nhật docs. |
| 101 | R10 Tiền tệ duy nhất = **Linh Thạch** 4 bậc | CHANGED | `currency/TuTinh.java:7-27` (Tử Tinh **3** bậc: Hạ=1, Trung=100, Thượng=10.000); `currency/ShopService|Wallet`; commit `aad5997` | grep `Linh Thạch|linh_thach` = 0 trong code/resource. Emerald vanilla vẫn cho dân làng. |
| 102 | Hiệp Hội Mạo Hiểm Giả (6 hạng, nhiệm vụ ngày) — **không có trong thiết kế** | CHANGED | `guild/*` (14 file, 20 callsite `GuildService`), `client/screen/guild/GuildScreens`; GT `guild/GuildGameTests.killQuestPaysPointsAndCoins`; `T:guild/GuildMathTest` | Thêm sau phản hồi; ngoài S6. |
| 103 | Thêm ngoài kế hoạch: Minimap/Map, Travel (dịch chuyển), Linh Bảo/Pháp bảo, Kiếp Số (scaling), Skillbook, Liberation | CHANGED | `client/minimap/*`, `travel/*` (`TravelGameTests`), `linhbao/*` (`LinhBaoGameTests`), `scaling/*` (`ScalingGameTests`), `skillbook/*`, `liberation/*` | Phình scope so với PLAN-v2 (rủi ro AP2/AP14). |
| 104 | Phạt chết: **Căn Cơ Tổn Thương** (≤3 tầng, −15%/tầng, 10 phút/tầng) (R11, S3 §8) | MISSING | grep `Căn Cơ|CanCo` = 0; không handler chết cho người chơi trừ Bản Mệnh Kiếm/Linh Bảo giữ đồ | Chết không có hậu quả tu vi. Có thể chủ đích nhưng không ghi nhận. |
| 105 | Thiền / Bế Quan chế độ chủ động (S3 §1.7) | PARTIAL | `pathrules/PathRules.java:110-114`; `society/SectEvents.java:177`; `phaptu/PhapRuntime.java:446` | Không thấy chặn "không qua Bình Cảnh" và không test. |
| 106 | Boss multiplayer: loot mỗi người 1 lượt roll, cần đóng góp tối thiểu (canon §10, R16) | MISSING | `HonCotEvents.java:152` roll theo sát thủ; grep per-participant loot = 0 | |
| 107 | `Q6`/`Q7`/`Q4`/`Q5` các câu hỏi mở §4.3 | PARTIAL | Q3 ✓, Q4 ✗ (hàng 47), Q5 ?, Q6 ✗, Q7 ✗ | Chưa user xác nhận ⇒ code không bắt buộc. |

---

## 2. Thống kê

(Đếm bằng `grep -c` trên cột Status của bảng §1.)

| Status | Số hạng mục |
|--------|-------------|
| DONE | 46 |
| PARTIAL | 39 |
| MISSING | 17 |
| CHANGED | 5 |
| **Tổng** | **107** |

Trong 39 PARTIAL: **13 chỉ thiếu test** (#3, 4, 15, 17, 23, 33, 36, 38, 46, 60, 69, 75, 77), 26 thiếu wiring/spec/số khác doc.

---

## 3. AP1 hunt — kết quả tìm "hàm treo / không có đường obtain"

| Phát hiện | Bằng chứng | Mức |
|-----------|-----------|-----|
| **Mắc Kẹt không có lối ra** | Hàng #29 | HIGH |
| `PillDrops` hầu như vô dụng vì `SpawnRules` chặn zombie/skeleton tự nhiên | `item/PillDrops.java:19-21` + `world/SpawnRules.java:44-48` | MED |
| Phá Cảnh Đan phẩm 7–9 + Thần Đan: không item | `FreeQuest.pillFor:51` `default -> null` | HIGH |
| 11 key `hukhong.map.*` + `hukhong.qc.merchant.s2` thiếu trong `vi_vn.json`/`en_us.json` | script đối chiếu `Component.translatable("hukhong.…")` ↔ lang | MED |
| `SectMath.daiBiNeeded` không caller | `society/SectMath.java:31` | LOW (helper) |
| Helper chỉ test dùng (production không gọi): `PhapMath.damageMult/passiveDps`, `PhapBeamMath.oldUltDamage/totalCost`, `SkillBookMath.totalHonTinh/typicalLevel`, `ChgKit.revealed` | grep số lần xuất hiện trong `src/main` = 1 | LOW (AP1-lite / dead code) |
| Hàm không caller khác: `CoreDef.fitsPath`, `BanMenhMath.orbitCount`, `CitySavedData.residentCount`, `Fx.telegraphCone`, `SkillEffects.flyTicks/shoulderOf`, `BookSkill.learnableBy`, `SkillBook.hasAssignment`, `AmbientNpc.archer/post/setPost`, `StoryNpc.home`, `PhapRuntime.isEnemyLike`, `LiberationData.liberated`, `Rift.closing`, `StructurePlacer.drainAll`, `AltarSeats.used`, `MinimapState.sizeMode`, `PhapClient.aura/renderers` | script grep đếm tham chiếu | LOW |
| `Payload` / `@SubscribeEvent` / `KeyMapping` / `MenuScreens` chưa đăng ký | quét toàn bộ: 0 trường hợp treo (5 lớp có `@SubscribeEvent` không `@EventBusSubscriber` đều được `EVENT_BUS.register` ở `HuKhong.java:32-35`, `ScalingMod.java:37`) | — |
| Item đăng ký nhưng không đường obtain | Linh Khí Đài / Ấn Hồn Đài / bảng chỉ đặt bởi builder (đúng thiết kế); `di_hoa_chau` có recipe; `tho_nguyen_dan` có recipe + shop | — (không phát hiện) |

---

## 4. Top 10 gaps (xếp theo tác động lên người chơi)

1. **Mắc Kẹt (1%) = kẹt vĩnh viễn** — Phá Chấp quest không tồn tại; chỉ `/hukhong core_unstuck` (debug) gỡ. (#29)
2. **Không có Phá Cảnh Đan phẩm 7–9 và Thông Thiên Thần Đan** — từ cấp 69/79/89 chỉ còn EXP ×0,25; Thăng Thần không có vật liệu. (#53, #94)
3. **EXP không nhân hệ số tư chất, không phạt đa đạo, không Bù Tư Chất, Bình Cảnh khác doc** — D/SSS lên cấp cùng tốc độ; mô hình 110h/500h vô hiệu; D/đa đạo không có pity. (#9, #19, #20, #21)
4. **Thành Chủ không có quyền lực** (thuế/Quỹ/Kho/trục xuất/bầu/bất tín) và chức vị chỉ 1 trục điểm (không DV + ĐPT) — hệ xã hội S6 cụt ở đỉnh. (#62, #63)
5. **Tập kích chỉ nhắm thành #0**; 9 thành còn lại không bao giờ mất/phế tích — áp lực "mất thành" (Pillar 3) gần như không có ở phần lớn bản đồ. (#70, #71)
6. **Thăng Thần không bị khóa bởi cốt truyện** — cấp 100 đạt được mà không hạ CHG nào; chỉ cổng boss cuối bị gate. Không có Mảnh Thiên Môn. (#92, #93)
7. **Kết thúc Phong Ấn/Hấp Thụ không khác biệt, Tầng II là hệ số toàn server (không dimension), thiếu Bia Công Trạng** — endgame hứa nhiều hơn mang lại. (#96, #97)
8. **Thiếu cả nhóm đan (Tăng Tu Vi, Tẩy Tủy, Thanh Tâm, Tịnh Hư, Độc Đan) + Thiên Địa Linh Vật + Tẩy Hồn/Luân Hồi Hoa** — Lõi "vĩnh viễn" không có đường đổi; Tâm Ma/Ác Tính/Hư Hóa không có thuốc giải. (#30, #55, #56)
9. **Boss multiplayer sai khác** (trần 8 thay vì 10, bán kính 64/80 thay vì 48, đếm mọi người trong vùng chứ không theo đóng góp ≥5%, không có loot mỗi người 1 lượt) + `PillDrops` chết. (#73, #106, #22)
10. **R21 mở khóa dần, pixel font, multiplayer 2 client chưa nghiệm thu, 44 key lang thiếu (minimap hiện key thô), Set bonus Lõi chỉ 6/8 Hệ và chỉ ngưỡng 2 món, TASK-v1 header lỗi thời** (AP11). (#37, #39, #40, #41, #27)

---

## 5. Hành động đề xuất (không thực hiện trong audit này)

1. Sửa AP1 trước: (a) cho `ensurePending`/`stuckAt` một lối ra (quest Phá Chấp hoặc tự hết hạn N giờ online); (b) thêm 7–9 + Thần Đan (hoặc ghi chú "đã cắt phạm vi" vào canon); (c) thêm key lang `hukhong.map.*`.
2. Quyết định có khôi phục `k_tư_chất` + phạt đa đạo + Bù Tư Chất hay chính thức bỏ (cập nhật S3 + canon §4) — đây là **Type 1 decision** vì đổi toàn bộ tốc độ tiến trình.
3. Cập nhật `TASK-v1` header (AP11) + ghi 5 thay đổi CHANGED vào `DESIGN-v2` (5 đạo, Tử Tinh, Hiệp Hội, EXP quái theo %).
4. Ghi pivot criteria cho phần còn lại của S6/S7 (xã hội, đan) để tránh AP10: hoặc cắt, hoặc làm tiếp.

---

## Document Lineage

| Version | Document | Focus | Status |
|---------|----------|-------|--------|
| v1 | AUDIT-A-design-impl.md | Audit thiết kế vs code (read-only) | ✅ Current |

**Nguồn**: DESIGN-v2-tu-tien-mod-master.md, PLAN-v2-tu-tien-mod-implementation.md, TASK-v1-tu-tien-mod-journey.md, PLAN-v2-tu-tien-mod-canon.md (R1–R29), sections/S3, S6, S7 (số liệu); `src/main/java/com/hukhong` (559 file), `src/test`.
