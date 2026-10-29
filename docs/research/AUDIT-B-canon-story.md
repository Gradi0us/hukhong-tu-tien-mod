# AUDIT-B — Canon lore + Story flow + Quest chains (đối chiếu doc <-> code)

**Created**: 2026-10-29
**Last Updated**: 2026-10-29 — Bản đầu: audit READ-ONLY, đối chiếu PLAN-v2-canon (+§14 R1–R29), DESIGN-v1-story-flow, DESIGN-v2-quest-chains với code/data thật
**Status**: Kết quả audit (không sửa code). Không chạy game; kết luận dựa trên đọc code + grep callsite + file test.
**Quy ước đường dẫn**: `J/` = `src/main/java/com/hukhong/`, `R/` = `src/main/resources/`, `T/` = `src/test/java/com/hukhong/`.

---

## Key Findings (Pyramid — kết luận trước)

| # | Phát hiện | Mức |
|---|-----------|-----|
| 1 | Tuyến truyện 0–10 **đi hết được trên giấy** (FlowStepsWalkTest đi 3 nhóm xuất thân tới `c10_end`; mọi TALK/DUEL có thoại; mọi lang key tồn tại, vi+en). Không có bước nào tự khóa cứng; chương luôn nhảy theo cấp (`StoryFlow.onProgress`), nên bước bị kẹt chỉ làm *mục tiêu HUD sai*, không chặn level. | Tốt |
| 2 | **Soft-lock thật duy nhất (cứng): Tâm Kiếp "Mắc Kẹt"** — chọn ô úp Phó Mặc, 1% roll -> kẹt vĩnh viễn ở mốc; nhiệm vụ "Phá Chấp" mà lang hứa **không tồn tại**; chỉ có lệnh dev `core_unstuck`. | CAO |
| 3 | Cốt lõi tu luyện thiếu: **Phá Cảnh Đan phẩm 7–9, Thiên Địa Linh Vật, Thần Đan, Tẩy Tủy/Tăng Tu Vi/Thanh Tâm/Tịnh Hư, Tẩy Hồn, hệ số EXP tư chất + phạt EXP đa đạo** đều chưa có trong đường EXP thật. | CAO |
| 4 | Hệ thống "thế giới bị đe dọa" mới chỉ áp cho **1 thành (Lưu Ly Cảng)**; phế tích **không có đường tái chiếm** (không code nào đặt lại `State.NORMAL`). | CAO |
| 5 | Nhiều lựa chọn truyện **không có hệ quả** (cờ được set nhưng không ai đọc): `rival_saved/lost`, `mo_dao_left/died`, `trust_mo_dao`, `qc.merchant.c.favor/ignore`, `qc.noi_gian.c.proof`. | TB |
| 6 | Multiplayer scaling có nhưng **lệch spec** (tối đa 8 người thay vì 10; bán kính 64 thay vì 48; không có ngưỡng đóng góp ≥5%; loot không chia lượt; Tầng II toàn server). | TB |

Đếm trạng thái xem cuối file (§ Tổng kết).

---

## 1. Bảng đối chiếu

Ký hiệu: **DONE** = code + callsite + test; **PARTIAL** = thiếu callsite/test/một phần spec/số khác doc; **MISSING**; **CHANGED** = bị feedback/code sau thay thế.

### A. Cảnh giới, Bình Cảnh, Lõi, vật liệu đột phá (PLAN-v2 §1, §5, R2, R6, R12, R25, R29)

| # | Hạng mục (doc ref) | Status | Evidence | Ghi chú |
|---|--------------------|--------|----------|---------|
| A1 | 13 cảnh giới, dải cấp 1–100 (§1) | PARTIAL | Code `J/core/Realm.java:7-19`; tên `R/assets/hukhong/lang/vi_vn.json:9-21`; callsite `J/core/CultivationData.java:63`, `J/client/hud/CultivationHud.java:70` | Khớp bảng canon (Đấu Kình I–V = cấp 90–95). Không có test riêng cho `Realm` (chỉ gián tiếp). |
| A2 | Bình Cảnh cấp 9/19…89: EXP x0,25 nếu không có vật liệu (§1.1.1) | DONE | `J/core/CultivationService.java:18,36-37` (BOTTLENECK_EXP_MULT), `J/core/CultivationData.java:85` (`inBottleneck`: level<90 && %10==9); callsite `CultivationService.applyExp`; test `T/core/CultivationServiceTest.java` (`bottleneckQuartersExp`) | Số "x0,25" là lựa chọn code; doc chỉ nói "cần thêm rất nhiều". Mục tiêu 500h vs 110h chưa kiểm bằng `BalanceSimTest` (chưa đọc). |
| A3 | Phá Cảnh Đan phẩm 1–6 (mốc 10–60) | DONE | `J/item/PhaCanhDanItem.java:57` -> `CultivationService.java:78`; đăng ký `J/item/ModItems.java:39-44`, `J/alchemy/AlchemyRegistry.java:82-85`; nguồn: `J/item/PillDrops.java:14` (2%/1% zombie/skeleton), `J/bicanh/BiCanhLoot.java:39`, `J/currency/ShopCatalog.java:65`, `J/alchemy/AlchemyRecipes.java:20-30` (luyện), `J/story/freequest/FreeQuest.java:41-53`; test `CultivationServiceTest.milestoneLockAndBreakthrough`, `T/alchemy/AlchemyMathTest.java` | Bind người nhặt: `PhaCanhDanItem.java:40-52`. |
| A4 | Phá Cảnh Đan phẩm 7, 8, 9 (mốc 70/80/90 — Bình Cảnh cấp 69/79/89) | MISSING | Chỉ có tier 1–6 (`AlchemyRegistry.java:82-85`); `FreeQuest.pillFor` trả `null` cho tier ≥7 (`FreeQuest.java:52`); `PhaCanhDanItem` tier 9 duy nhất là Tiểu Bán Thần Đan (light 98) | Cấp 69/79/89 chỉ vượt được bằng EXP x0,25. Lang `hukhong.pill.tier.9` tồn tại nhưng không phải Phá Cảnh Đan 90. |
| A5 | Thiên Địa Linh Vật (thay thế Phá Cảnh Đan, §1.1.1) | MISSING | `grep -i linh_vat/ThienDia` chỉ trúng `CultivationService.java` (comment) | Pillar 2 ("cơ duyên quyết định tốc độ") chỉ có nhánh đan. |
| A6 | Mốc nhẹ 94->95, 98->99 (Bình Cảnh nhẹ x3 + Tiểu Phá Đan, R6) + mốc Lõi 95/99/100 (R29) | DONE | `CultivationService.java:20-27,59-75`; `CultivationData.java:74` (isMilestoneLevel 95,99); đan `ModItems.java:45-48`; callsite `PhaCanhDanItem.java:56`; nguồn `BiCanhLoot.java:36-37`; test `CultivationServiceTest.lightBottleneckBar3xAndPill` | Doc §1.1 ghi "91–95/96 chỉ x3"; code chỉ x3 ở cấp 94 & 98 (CHANGED nhẹ theo S3/R29). |
| A7 | Ấn Hồn Đài: EXP khóa ở mốc, chọn Lõi 4 ô (3 thường + Phó Mặc), roll lưu trước UI (R8) | DONE | `J/block/AnHonDaiBlock.java:25-46`; `J/cores/CoreService.java:81-125`; `J/cores/CoreRoller.java` (Δ-table `:48-52`); `CultivationService.unlockMilestone:88-97`; test `T/cores/CoreRollerTest.java` | Ấn Hồn Đài dựng trong thành: `J/city/CityBuilder.java:145,345`. |
| A8 | Ấn Hồn Đài ở **mọi** thành/tông/Thánh Địa; cấp thành giới hạn trần phẩm Lõi (R12) | PARTIAL | Altar thành: `CityBuilder.java:145,345`. Tông: `grep AN_HON_DAI society/` = 0 trúng; trần phẩm chỉ theo mốc `CoreRoller.ceilTier:56`, không theo cấp thành | Altar tông/Thánh Địa chưa xác minh trong NBT cấu trúc (ngoài code) — NHÃN: chưa xác minh. Trần theo thành: MISSING. |
| A9 | Ô thứ 4 "Phó Mặc Số Phận" 1% Mắc Kẹt -> Tâm Kiếp, kẹt đến khi xong "Phá Chấp" (§5, R27) | PARTIAL | Stuck set: `CoreRoller.java:149`, `CoreService.java:110-116`; chặn altar: `AnHonDaiBlock.java:30-33`; gỡ duy nhất: lệnh dev `HuKhongCommand.java:78-82` (`core_unstuck`). **Không có** code/NBT/lang nhiệm vụ Phá Chấp (grep toàn `src` chỉ trúng lang `altar.stuck`) | **SOFT-LOCK CỨNG** (xem §2 SL1). |
| A10 | Tẩy Hồn (Luân Hồi Hoa / Tẩy Hồn Thạch, R9) | MISSING | grep `TayHon/tay_hon/Luân Hồi` = 0 trong `J/`; chỉ có câu cảnh báo lang `vi_vn.json:116` | Lõi chọn xong **không bao giờ đổi được** (kể cả chọn nhầm). |
| A11 | Bù Tư Chất / pity Bình Cảnh cho D & đa đạo (R2) | MISSING | `grep -i pity` chỉ trúng `J/honcot/HonCotEvents.java:38` (xương, không liên quan) | |
| A12 | Hệ số EXP theo tư chất D0,6…SSS1,6 (§4) + phạt EXP giữ song x1,6 / tam x2,5 (§3.1) | MISSING | Đường EXP thật: `J/entity/HuHoa.java:127-133,185` và `J/gates/GateEvents.java:39` — không nhân hệ số tư chất / số đạo. `aptitude()` chỉ dùng cho chỉ số/skill (`stats/StatService`, `passive/*`) | Tư chất & song/tam năng **không ảnh hưởng tốc độ lên cấp**. Chi phí tài nguyên x2/x3 thì có (A13). |
| A13 | Đa đạo: chi phí tài nguyên x2 / x3 (§3.1) | PARTIAL | `J/gates/Resources.java:13-15,21-31` | Không thấy test riêng cho `Resources`. |
| A14 | Spawner vô hiệu (0 EXP/Hồn Tinh/vật liệu) + KillRateGate >30 con/64 block/10 phút -> x0,1 (§1.1, R3) | PARTIAL | `J/gates/KillRateGate.java:11-17`, `J/gates/SpawnerMarks.java`, `J/gates/RewardGate.java`; callsite `GateEvents.java:36-42`, `HuHoa.java:183`, `PillDrops.java:21`, `SectEvents.java:121-124` | Không tìm thấy unit test cho gates (grep `T/` = 0). |
| A15 | Khảo Hạch Thăng Thần: 3 thử thách (Tâm Ma, Lôi Kiếp, Hư Không Bản Nguyên = boss cuối) (§9, PLAN-v1 §1) | PARTIAL | `J/trial/TrialService.java:47` (`Type{TAM_MA,LOI_KIEP}`), kích bởi `CultivationService.unlockMilestone:90-93` (cấp ≥90); boss `J/endgame/NguyenThuyTichDiet` độc lập, không phải bước của Trial | Thử thách 3 = boss: chưa nối. Trial bắt buộc ở 90/95/99/100. Test: `HuKhongRuntimeGameTests.java:376-420` (GameTest Tâm Ma/Lôi Kiếp), `T/trial/TrialMathTest.java`. |
| A16 | Ma Tu: Ác Tính, Mất Kiểm Soát -> Tẩu Hỏa Nhập Ma = phế tu, làm lại từ đầu, 3 tầng cảnh báo (R23) | DONE | `J/pathrules/MaRules.java:243-246` (trigger), `:363-384` (`tauHoa`: reset cultivation/cores/roll lại), `warn(...)` `:252`; test `T/pathrules/MaRulesTest.java` | **Không reset `QuestData`/`StoryData`** -> story step còn ở chương cũ (xem SL2). |
| A17 | Thể Tu "không dùng vũ khí", Đạo Tu đòn thường lên quái = 0, Kiếm Tu Bản Mệnh Kiếm | PARTIAL | `J/gates/DamageRules.java:65-79`; `J/banmenh/*` (test `T/banmenh/BanMenhMathTest.java`) | DamageRules chưa thấy test riêng. Không thuộc phạm vi lore chính, chỉ ghi nhận. |
| A18 | Lõi: Hệ (set) bonus 2/4/6, Thần Lõi tính +1 mọi Hệ (§5) | PARTIAL | `J/cores/CoreService.java:156-165` — chỉ 6 hệ, `hasSetBonus` chỉ cần `count>=2` (`:172`), **không có mốc 4/6** | 36 Lõi JSON: `R/data/hukhong/hukhong/core/*.json`. |

### B. Hệ Tuổi / Thọ Nguyên (PLAN-v2 §6, R1, R22)

| # | Hạng mục | Status | Evidence | Ghi chú |
|---|----------|--------|----------|---------|
| B1 | 1 ngày MC = 1 tháng (24.000 tick), đồng hồ chỉ khi online, chặn AFK (R1) | DONE | `J/age/AgeData.java:13-14,32`; `J/age/AgeService.java:37-47`; `J/age/AfkTracker.java:21`; test `T/age/AgeMathTest.java` | AFK-check chưa có test (AfkTracker). |
| B2 | Thọ Nguyên tăng theo cảnh giới | DONE | `J/age/AgeMath.java:8-17` (50…1000 theo realmIndex); `AgeService.java:50` | Số "BASE" chưa đối chiếu S6 (file sections không đọc). |
| B3 | Thiên Nhân Ngũ Suy I–V, **không chết vì tuổi** (dừng GĐ V) | DONE | `AgeMath.java:24-27,42-55`; debuff `AgeService.java:72-80`; cấm bay `J/flight/FlightBans.java:26`; thiền x0,5 `J/pathrules/PathRules.java:114`, `J/phaptu/PhapRuntime.java:446`, `SectEvents.java:177`; test `AgeMathTest` | |
| B4 | Cảnh báo 3 mức trước hạn + title khi Ngũ Suy | DONE | `AgeMath.warnLevel/warnKey:29-40`; `AgeService.warn:61-70`, `:53-58` | Lang `hukhong.age.warn.*` có. |
| B5 | Thọ Nguyên Đan gia hạn | DONE | `J/age/ThoNguyenDanItem.java:24-32`; đăng ký `AlchemyRegistry.java:95`; luyện `AlchemyRecipes.java:38`; test `AgeMathTest` | Đan này (R5) chưa thấy cơ chế bind. |
| B6 | Mỗi xuất thân có tuổi khởi đầu (6–16) (§4) | PARTIAL | `J/charcreate/CharRoll.java:18,45` — tuổi random 6/8/…/16, **không gắn xuất thân** | |
| B7 | Tuổi ảnh hưởng điều kiện vào tông / sự kiện / NPC đối xử | PARTIAL | Tông: `J/society/SectMath.java:105-109`. Sự kiện / NPC: không tìm thấy | Chỉ 1/3 phần. |
| B8 | Bế Quan (Linh Khí Đài) làm tuổi trôi (R22 ngoại lệ) | DONE | `AgeService.java:43` + `SectEvents.isBeQuan:106` | |

### C. Tông môn (PLAN-v2 §7, R13, R14, R22)

| # | Hạng mục | Status | Evidence | Ghi chú |
|---|----------|--------|----------|---------|
| C1 | 7 tông, 3 cấp (Thiên 1 / Địa 2 / Nhân 4), tên + đạo + tuổi tối đa Thiên≤40/Địa≤45/Nhân≤55 (R14) | DONE | `J/society/SectDef.java:15-22`; lang `vi_vn.json:427-433`; dựng `J/society/SectEvents.java:59-80`, `SectBuilder`; test `T/society/SectMathTest.java` | |
| C2 | Gia nhập: (a) mời ngẫu nhiên, (b) tự ứng tuyển (tư chất+cảnh giới+tuổi) | DONE | Mời: `SectEvents.java:87` -> `rollInvite:192-215`; ứng tuyển: `J/society/SectService.java:36-43,70`; check `SectMath.java:105-120`; test `SectMathTest` | |
| C3 | Xuất thân Đệ Tử Tông Môn nhận **Thư Tiến Cử** (bỏ giới hạn tuổi, R13/R14) | MISSING | `grep tien_cu` = 0. Xuất thân SECT chỉ đổi điểm spawn (`J/world/OriginSpawn.java:31-34`). Mời thường chỉ nới `+5` tuổi / `-10` cấp (`SectMath.java:106-108`) | Số khác doc: R14 "bỏ qua" vs code "+5". |
| C4 | Bậc đệ tử Ngoại->Nội->Chân Truyền->Trưởng Lão, Cống Hiến, nhiệm vụ riêng (kill/herb/chain) | DONE | `SectMath.java:22-26,124-151`; `SectService.onHuKill:166`; bảng `J/society/SectBoardBlock`; test `SectMathTest` | Nhiệm vụ tông tách khỏi tuyến chính (đúng doc). |
| C5 | Buff tông môn | DONE | `J/society/SectBuffs.java:29-40`; callsite `SectEvents.java:86,92,97`, `J/alchemy/DanLoBlockEntity.java:134` (buff Đan Đỉnh) | |
| C6 | Huyết Ảnh Ma Môn ẩn (chỉ mời, Ma tu Ác Danh) | DONE | `SectDef.java:22` (hidden), `SectMath.checkHidden:73-77`, `SectEvents.java:214-` (tin đồn + lệnh) | |
| C7 | Linh Khí Đài + Bế Quan AFK ~10% tốc độ, không qua Bình Cảnh, phí/giờ, số chỗ (R22) | DONE | `SectEvents.meditate:134-190`, `SectMath.seats/altarFeePerHour:59-62`, `J/society/LinhKhiDaiBlock`; flow bước `pro_sect_meditate` | |
| C8 | Chỉ thuộc 1 tông tại một thời điểm | DONE | `SectMath.ALREADY` (`apply_fail.5` lang); rời tông mất Cống Hiến (`vi_vn.json:446`) | |

### D. Thành, chức vị, tập kích, phế tích (PLAN-v2 §8, §14 R12)

| # | Hạng mục | Status | Evidence | Ghi chú |
|---|----------|--------|----------|---------|
| D1 | 10 thành, 3 cấp, tên cố định (Thiên Khải Hoàng Đô; Lạc Nhật/Thanh Vân/Bắc Hàn; 6 Nhân) | DONE | `J/city/CityDef.java:8-17`; lang `vi_vn.json:401…`; dựng `J/city/CityService.java:60-94`, `CityBuilder`; test `T/city/PlacementTest.java` | |
| D2 | Chức vị: Cư Dân -> Hộ Vệ -> Đội Trưởng -> Thống Lĩnh -> Phó Thành Chủ -> Thành Chủ | PARTIAL | `J/city/CityRank.java:7-48`; callsite `CityService.java:98-121`, `CityService.onKill:139-146`, quyên góp `:167-190`; test `T/city/CityRankTest.java` | Chức vị chỉ là **danh hiệu + thông báo** (`hukhong.city.promoted`); không tìm thấy quyền lợi/quyền hạn nào gắn rank (grep `CityRank.Rank` ngoài city/ = 0). |
| D3 | Mỗi thành chỉ 1 Thành Chủ (NPC giữ nếu chưa có người) | DONE | `J/city/CityRegistryData.java:63-70`; `CityService.refreshRank:117-122`; test `CityRankTest.lordSeatTakenCapsAtDeputy` | "NPC giữ chức" chưa thấy trong code (chỉ `lordName` hiển thị). |
| D4 | Mất thành -> Phế Tích | PARTIAL | `J/events/InvasionManager.java:238-243` + `J/city/CitySavedData.java:14-27` (singleton **một** thành Lưu Ly Cảng) + `J/city/RuinGuard.java` | 9 thành còn lại **không thể bị mất**. |
| D5 | Phế Tích tái chiếm được; kho riêng người chơi được giữ | MISSING | Chỉ 1 nơi đặt state: `InvasionManager.java:240` (`RUIN`). Không có `State.NORMAL` setter. `J/liberation/*` là tẩy Tâm Hư Hóa (HeartArena), không phải tái chiếm thành | Sau khi mất thành: tập kích dừng vĩnh viễn (`CorruptionManager.schedule:185`), altar Hub vẫn còn nhưng bao quanh bởi mob 55–85 (`RuinGuard.java:34`). |
| D6 | Tập kích tần suất theo Xâm Thực, có trần; chỉ khi có người chơi online liên quan / hoãn khi không ai online | PARTIAL | `J/events/CorruptionManager.java:38-39,73,183-192` (khoảng cố định 72.000–120.000 tick, chỉ cần có ≥1 người online ở Overworld); win/lose `InvasionManager.java:225-243` | Không tính "mức Xâm Thực" hay "người chơi liên quan thành". Hậu quả: có thể tập kích khi người chơi ở xa (sinh quái bị bỏ qua vì chunk chưa nạp `:197` -> thắng ngầm). |
| D7 | Thế giới 15.000 block từ spawn (R24), worldborder | PARTIAL | `CorruptionManager.java:35,45-53` (`setSize(2*15000)` tâm = spawn); hang ổ CHG đặt theo gốc (0,0) `J/chg/ChgKit.java:103-115` | Không test. Nếu spawn lệch xa gốc, hang IV (≈12.000) có thể ra ngoài border — chưa kiểm. |

### E. Đan dược (PLAN-v2 §11, R5, R7)

| # | Hạng mục | Status | Evidence | Ghi chú |
|---|----------|--------|----------|---------|
| E1 | Nhóm Phá Cảnh | PARTIAL | Xem A3/A4/A6 | 1–6 + 2 Tiểu Phá Đan; thiếu 7–9 + Thần Đan. |
| E2 | Nhóm Hồi Phục | DONE | `J/alchemy/UtilityPillItem.java:15-47`; `AlchemyRegistry.java:88-92` (Chỉ Huyết, Hồi Xuân); luyện `AlchemyRecipes.java:32-36`; test `AlchemyMathTest` | |
| E3 | Tăng Tu Vi (EXP pill) | MISSING | Không item nào cộng EXP trực tiếp | |
| E4 | Thọ Nguyên | DONE | Xem B5 | |
| E5 | Tẩy Tủy (nâng tư chất, cực hiếm) | MISSING | grep `tay_tuy` = 0 | Tư chất không thể nâng. |
| E6 | Thanh Tâm (giảm Tâm Ma/Ác Tính) | MISSING | grep = 0 | Ma tu không có thuốc hạ Ác Tính ngoài thiền/thời gian. |
| E7 | Tịnh Hư (giải Hư Hóa) | MISSING | grep = 0 | |
| E8 | Đan Độc (cơ chế) + Giải Độc Đan; Độc Đan (item độc) | PARTIAL | `J/alchemy/DanDoc.java:22-45`, `AlchemyRegistry.java:92`; `PhaCanhDanItem.java:56-57` gọi `DanDoc.consume`; test `AlchemyMathTest` | Cơ chế Đan Độc OK; "Độc Đan" (đan độc hại) không có. |
| E9 | Luyện đan: Đan Lô + Đan Hỏa + công thức; Đan Đỉnh Tông là nguồn học chính | PARTIAL | `J/alchemy/DanLoBlock/DanLoBlockEntity/DanLoMenu`, `AlchemyRecipes.java:20-40` (10 phương trong code) | Không có "Đan Hỏa", không có học công thức (tất cả phương mặc định); buff Đan Đỉnh chỉ +tỉ lệ (`DanLoBlockEntity.java:134`). |
| E10 | Bind (R5): Phá Cảnh Đan, Linh Vật, Tẩy Tủy, Thọ Nguyên, Thần Đan ràng buộc; Cống Hiến đổi đan bind có giới hạn tuần | PARTIAL | Chỉ Phá Cảnh Đan: `PhaCanhDanItem.java:38-52`; Thọ Nguyên Đan không bind | Đổi Cống Hiến -> đan: không thấy. |
| E11 | Phẩm Nhất->Cửu + Thần Đan; Thông Thiên Thần Đan cho Thăng Thần (R7) | MISSING | grep `thong_thien` = 0 | |

### F. Phản Thần / Sứ Đồ / Chấp Hành Giả (PLAN-v2 §9, R15, R19)

| # | Hạng mục | Status | Evidence | Ghi chú |
|---|----------|--------|----------|---------|
| F1 | Phản Thần Dạ Khuyết (ngoại lai Hư Không, tín đồ Sứ Đồ) | PARTIAL | `J/endgame/DaKhuyet.java:43` (xuất hiện pha 3 boss cuối `EndgameKit.DA_KHUYET_PHASE`); banner chương 10 `StoryFlow.beat:280`; lang `hukhong.flow.ch.10.*` | Doc story-flow tự nhận: "cảnh cắt Dạ Khuyết chỉ banner". |
| F2 | Sứ Đồ nhiều loại + bậc, cấp ≤65 | PARTIAL | `J/story/SuDo.java`, `StoryRegistry.java:42` (3 variant), `StoryService.java:24` (MAX_LEVEL=65); spawn `StoryService.onServerTick:108-125` | Không có test unit cho SuDo (chỉ GameTest `HuKhongRuntimeGameTests`). "Bậc" Sứ Đồ chỉ qua tierOf(level) cho drop. |
| F3 | Sứ Đồ xuất hiện tự nhiên | PARTIAL | `StoryService.java:108-125`: chỉ trong chunk Hư Hóa **và** người chơi cấp ≥30; tim Hư Hóa cách spawn 4.000–11.000 (`CorruptionManager.java:55-61`) | Người chơi quanh Hub gần như không gặp Sứ Đồ tự nhiên; nguồn thay thế: phục kích trong chuỗi phụ (`QuestChains.spawnAmbush:206`). |
| F4 | Văn Kiện (12 loại, rơi 6/14/28/55%) | PARTIAL | `J/story/StoryDocs.java:18-46`; rơi `SuDo.java:219-228`; dùng `VanKienItem.java:24`; ghi `StoryService.collect:49-72` | Không có unit test cho StoryDocs/collect. Hệ số loại (x loại) trong R15: không có (chỉ theo bậc). |
| F5 | Gom đủ manh mối -> lộ hang ổ / kích sớm sự kiện CHG | DONE | `StoryService.java:61-64` (>=3 cùng chuỗi -> `ChgService.revealLair`), `:69` (6 -> `ChgService.earlyEvent` `J/chg/ChgService.java:63`); flag `lair` `StoryData.java:45`; test `T/chg/ChgKitTest.java` (`story_flags_do_not_collide_with_lair_hints`) | Doc R19 "kích sớm cần party>=2 hoặc cảnh giới>=CHG-5": không có (`earlyEvent` không kiểm). |
| F6 | Manh mối chỉ về 2 CHG đầu; III/IV: manh mối từ CHG I/II + sự kiện cao | PARTIAL | `ChgKit.revealed:92-93` chỉ idx 1–2; **không có** Văn Kiện/manh mối cho III/IV | Thay vào đó objective HUD chỉ **thẳng tọa độ hang** `StoryFlow.java:229` (`case CHG -> lair(ow,s.n())`) cho cả 4 CHG, kể cả khi chưa có Văn Kiện -> bypass cơ chế manh mối. |
| F7 | 4 Chấp Hành Giả cấp 67/78/86/98, đạo mô phỏng Thể/Đạo/Kiếm/Ma | DONE | `J/chg/ChgKit.java:36-53`; entity `J/chg/ChapHanhGia.java`; cờ hạ `ChapHanhGia.die:462-471`; test `T/chg/ChgKitTest.java` | |
| F8 | Xuất hiện: sự kiện ngẫu nhiên / hang ổ bất ngờ | DONE | `ChgService.lairTick:86-108`, `eventTick:110-125` | |
| F9 | 4 Mảnh Thiên Môn (giữ bởi 4 CHG), ráp mở cổng | CHANGED | Thay bằng bit "đã hạ" trong `StoryData.lair` (`ChgKit.defeatedBit:89`); cổng mở khi cả 4 bit + cấp ≥99 (`EndgameKit.java:13-18`, `EndgameService.gatewayTick:53-66`); item "Hạch Địa/Quang/Phong/Huyết Môn" (`ChgRegistry.java:37`, lang `vi_vn.json:643-646`) chỉ rơi, **không dùng** | Doc story-flow vẫn ghi "ráp 4 Mảnh". |
| F10 | Văn Kiện/manh mối CHG **chung toàn server** (R19) | CHANGED | `StoryData` là attachment từng người (`story/StoryData.java:59`) | Mỗi người chơi tự thu thập. |
| F11 | Bắt sống Ám Ngữ lấy Văn Kiện (≤60%, 1 lần/ngày) (R15) | MISSING | grep `Ám Ngữ/am_ngu` = 0 | |

### G. Multiplayer (PLAN-v2 §10, R16)

| # | Hạng mục | Status | Evidence | Ghi chú |
|---|----------|--------|----------|---------|
| G1 | HP boss = gốc x (1+0,75(n-1)), n<=10, cập nhật động | PARTIAL | `J/events/BossScaling.java:6-10` (**MAX_PLAYERS = 8**, HP +75%/người); dùng ở CHG (`ChapHanhGia.java:421-427`, quét bán kính **64**), `J/entity/HuVuong`, `WorldBoss`, `NguyenThuyTichDiet`, `InvasionManager`; test `T/events/BossScalingTest.java` | Số khác doc: trần 8 vs 10; bán kính 64 vs 48. |
| G2 | n = người gây >=5% sát thương / hồi máu đáng kể (R16) | MISSING | CHG dùng "mọi Player không spectator trong 64 block" (`ChapHanhGia.java:427`); grep `contribut` = 0 | Người đứng xem vẫn tăng HP boss. |
| G3 | Loot chia theo người tham gia (mỗi người 1 lượt roll) | MISSING | `ChapHanhGia.dropCustomDeathLoot:476-481` rơi 1 bộ chung | |
| G4 | Boss thế giới luôn cấm bay | DONE | `J/flight/FlightBans.java:13-24`; GameTest `HuKhongRuntimeGameTests.java:232` | |
| G5 | Mỗi người có cờ "đã hạ CHG" riêng khi cùng đánh | DONE | `ChapHanhGia.die:462-471` gán bit cho mọi người trong 128 block | Khác R19 (chung server) nhưng chấp nhận được. |

### H. Thế giới, kết thúc (PLAN-v2 §9, R18)

| # | Hạng mục | Status | Evidence | Ghi chú |
|---|----------|--------|----------|---------|
| H1 | Hai cách chơi: Theo cốt truyện / Tự do | DONE | `StoryService.chooseMode:31-46`; `StoryFlow.active:81`; GameTest `StoryFlowGameTests.freeModeHasNoMainObjective` | |
| H2 | Boss cuối Nguyên Thủy Tịch Diệt | DONE | `J/endgame/NguyenThuyTichDiet.java`, `EndgameKit.java:29-41`, `EndgameService.spawnBoss`; test `T/endgame/EndgameKitTest.java` | |
| H3 | Cổng Hư Không + vào/ra | DONE | `EndgameService.gatewayTick:53`, `enter`, `leave`; callsite `J/endgame/EndgamePortalBlock.java:24` | |
| H4 | Kết thúc Phong Ấn / Hấp Thụ (chọn cá nhân) | DONE | `EndgameService.choose:140-151`, `onBossDefeated:123-138`; callsite `EndgameNet.java:51`; flag đọc bởi `mo_dao.json` (`ch6_seal/absorb`); `StoryFlow.satisfied ENDING` -> `EndgameService.hasEnding` | |
| H5 | Ghi kết thúc vào Bia Công Trạng (R18) | MISSING | Chỉ có lang `vi_vn.json:755`; grep `Bia/bia_cong` = 0 trong `J/` | |
| H6 | Hấp Thụ để lại Hư Hóa chỉ ngoại hình (R18) | MISSING | `F_ABSORB` chỉ dùng trong EndgameService/cutscene/dialogue | |
| H7 | Tầng Thế Giới II = **dimension mới**, khác hương vị theo kết thúc | CHANGED | `J/endgame/WorldTier.java:12-34` — chỉ **hệ số toàn server** HP x1,5 / dmg x1,3 / reward x1,5 (`EndgameKit.java:22`); `raiseTo` gọi bởi `EndgameService.choose:145` | Một người chọn là nâng cho cả server (trái R18 "cá nhân"). |
| H8 | Hư Hóa lan theo chunk từ tim | PARTIAL | `CorruptionManager.spread:77-95`; `J/events/CorruptionData` | Không test spread. |

### I. Luồng truyện 0–10 (DESIGN-v1-story-flow) — đi từng chương, kiểm bước có hoàn thành được không

Hạ tầng dùng chung: `J/story/flow/StoryFlow.java` (517 dòng), `FlowSteps.java`, `WaveCountdown.java`, `RequiredNpcs.java`; hoàn thành bước qua `StoryFlow.complete:114`, `satisfied:161`, poll 1s `onPlayerTick`, nhảy chương `onProgress:193` (gọi từ `CultivationService.update:125`).

| # | Hạng mục (chương/bước) | Status | Evidence | Ghi chú |
|---|------------------------|--------|----------|---------|
| I0 | Ánh xạ chương <-> dải cấp, `chapterFor` | DONE | `FlowSteps.java:116-120`; test `T/story/flow/FlowStepsWalkTest.java` (`everyGroupReachesEndingWithMonotonicChapters`) | |
| I1 | Mục tiêu (`Objectives`) cho mọi bước + target | DONE | `StoryFlow.setObjective/target`; GameTest `StoryFlowGameTests.everyStepSetsObjectiveWithTarget`; JUnit `FlowStepsWalkTest.everyStepHasObjectiveText` | |
| I2 | Di trú save cũ | DONE | `StoryFlow.migrate:100`; GameTest `oldSaveMigratesByLevel` | |
| I3 | Ch0 city: `pro_city_raid` (KILL 5 Hư Thi) -> `pro_city_road` (REACH 60 block Lưu Ly Cảng) | DONE | `FlowSteps.java:34-35`; `WaveCountdown.start` (`StoryFlow.java:146`), `spawnHuThi`, `maintain KILL`, `onKill`; GameTest `prologueSetsObjectiveCity` | Doc §8: **chưa kiểm real-run (AP4)**; không tìm thấy kiểm thử mới hơn. Hoàn thành khả thi: quái được bổ sung tới đủ (`maintain:450-456`). |
| I4 | Ch0 sect: talk Sư Phụ -> bấm Linh Khí Đài -> kill 5 -> REACH | DONE | `FlowSteps.java:36-39`; Sư Phụ sinh `StoryFlow.enter:147`; thoại `R/data/.../dialogue/su_phu.json` (`step_done pro_sect_talk`); click `StoryFlow.onUseBlock:416-423`; GameTest `prologueSetsObjectiveVillageAndSect` | |
| I5 | Ch0 village: kill 5 -> REACH | DONE | `FlowSteps.java:40-41` | |
| I6 | Ch0 hội tụ: `pro_meet` (TALK Mộ Dao) -> `pro_altar` (bấm Ấn Hồn Đài) | DONE | `FlowSteps.java:42-43`; `mo_dao.json` node `pro_meet` (`step_done pro_meet`); NPC sinh bởi `DialogueService.onPlayerTick:226-247` khi đứng <=120 block thành đã dựng | Nếu Hub bị RUIN trước bước này, Mộ Dao vẫn sinh nhưng giữa mob 55–85 (xem SL4). |
| I7 | Ch1 Hạt Bụi: `c1_talk` -> grind(10) -> ms | DONE | `FlowSteps.java:46-47`; `mo_dao.json` node `ch1` (`step_done c1_talk`, exp 150, skill PHAM) | |
| I8 | Ch2 Nhập Đạo: `c2_sect` (vào tông) -> `c2_rival` (thoại Hạng Chiêu) | DONE | `FlowSteps.java:49-50`; `satisfied SECT:175` (member **hoặc** Hiệp Hội đã làm — lối thoát); `StoryFlow.sectPos:252-266` chỉ chỉ tông đủ điều kiện; thoại `hang_chieu.json` (`step_done c2_rival`) | Lối thoát Hiệp Hội tránh kẹt khi không hợp tông nào. |
| I9 | Ch3 Bí Cảnh đầu: `c3_bicanh` -> `c3_docs`(>=1) | PARTIAL | `FlowSteps.java:52-53`; `StoryFlow.maintain BICANH:473-492` (đặt cổng, fallback mở thẳng sau 60s), `storyTier:497`; `c3_docs` `satisfied DOCS:176` | `c3_docs` cần 1 Văn Kiện nhưng người chơi cấp 20–29 chưa gặp Sứ Đồ tự nhiên (F3: cần cấp>=30). Nguồn khả dĩ: Bí Cảnh loot / chuỗi phụ. Không kẹt cấp (chương nhảy theo cấp) nhưng mục tiêu HUD sai tới cấp 30. |
| I10 | Ch4 Bóng Dạ: `c4_docs`(>=2) -> `c4_hua_co` | PARTIAL | `FlowSteps.java:55-56`; thoại `hua_co.json` entry `ch3` đòi `docs_min:2`, `chapter_min:4`, `min_level:30` | Như I9: phụ thuộc Văn Kiện. Chuỗi phụ cho tối đa a2,a5 / b2|b3,b4. |
| I11 | Ch5 Cơ Duyên Bị Cướp: `c5_invasion` -> `c5_rival` (đấu) | DONE | `FlowSteps.java:58-59`; `StoryFlow.maintain INVASION:461-471` (gọi `InvasionManager.warn` khi người chơi <80 block thành; RUIN -> bỏ qua `complete`), `satisfied INVASION:180-184`; duel `startDuel:347`, `onRivalHurt:367-381` (rút lui ở 20%); thoại `hang_chieu_r.json` | Thua tập kích => RUIN => bước bị bỏ qua tự động (không kẹt). GameTest riêng cho duel: không có (doc §8 thừa nhận). |
| I12 | Ch6 Địa Môn: `c6_lair` -> `c6_modao` (tin/nghi) | PARTIAL | `FlowSteps.java:61-62`; `satisfied LAIR_SEEN:177` = `(lair & 3)!=0` (cần >=3 Văn Kiện cùng chuỗi `StoryService.java:61`); thoại `mo_dao.json` node `ch4` đòi `lair:3`, `chapter_min:6`, `min_level:50` | Nếu chưa đủ 3 Văn Kiện: Mộ Dao chỉ nói `ch4_wait`; chương vẫn nhảy theo cấp; cờ `trust/doubt_mo_dao` không đặt -> chương 9 mặc định "rời đi". |
| I13 | Ch7 Thạch Hống: `c7_chg1` | DONE | `FlowSteps.java:64`; `satisfied CHG:178` = `ChgKit.defeated`; bit đặt ở `ChapHanhGia.die:465-469`; hang `lairTick` kích hoạt khi cấp>=lv-15 (`ChgKit.lairEligible:100`) | Hang I cách ~4.500 block; HUD trỏ thẳng (F6). |
| I14 | Ch8 Từ Bi: `c8_rival` (đấu, lựa chọn `rival_saved/lost`) -> `c8_chg2` | PARTIAL | `FlowSteps.java:66-67`; `hang_chieu_r.json` node `c8` đặt cờ rồi `duel` | Hai cờ `rival_saved`/`rival_lost` **không ai đọc** (grep `J/` + JSON = 0) -> doc nói "quyết định phe ở chương 10" là chưa làm. |
| I15 | Ch9 Kiếm Vong: `c9_chg3` -> `c9_modao` (leave/sacrifice theo trust/doubt) | PARTIAL | `FlowSteps.java:69-70`; `mo_dao.json` entry `c9_modao` +`doubt_mo_dao` -> `ch9_sacrifice` | `mo_dao_left/mo_dao_died` không ai đọc; Mộ Dao "hy sinh" vẫn sinh ra nói chuyện tiếp (idle/ch5/ch6) — mâu thuẫn tự sự. |
| I16 | Ch10 Khảo Hạch: `c10_grind`(98) -> `c10_chg4` -> `c10_gate` -> `c10_end` | PARTIAL | `FlowSteps.java:73-76`; GATE `satisfied:184-187` (cổng đã dựng + đủ 4 bit + cấp>=99); ENDING `:188`; `finish:154` | (a) Mốc Lõi 95/99/100 khóa EXP nhưng **không bước nào chỉ Ấn Hồn Đài** (SL3). (b) `c10_gate` thỏa khi cổng *tồn tại*, không cần bước vào. (c) Doc "ráp 4 Mảnh" đã CHANGED (F9). |
| I17 | Mộ Dao đi cùng thật; tố cáo nội gián (S1 §11-3); cảnh cắt Dạ Khuyết | MISSING | Doc §8 tự liệt kê là "chưa làm"; xác nhận: NPC Mộ Dao là NPC đứng thành (`DialogueService`), nội gián đã chuyển sang chuỗi `noi_gian` (CHANGED một phần) | |
| I18 | Mộ Dao nhịp "Dặn dò" ch2–5 (banner chat) | DONE | `StoryFlow.beat:280-286`, lang `hukhong.flow.ch.N.l1..l3` (script kiểm đủ 1–10) | |

### J. Thoại NPC (JSON <-> code)

Kiểm tự động (script Python): 12 file `R/data/hukhong/hukhong/dialogue/*.json`, 140 text-key — **0 key thiếu** ở `vi_vn.json` và `en_us.json`; mọi `step_done` trỏ tới step có thật; 9 loại action (`set_flag, give_item, give_skill, exp, chapter, step_done, duel, qc, doc, message`) đều được xử lý ở `J/story/npc/Dialogue.java:64-83` (`chapter` là no-op có chủ đích).

| # | Hạng mục | Status | Evidence | Ghi chú |
|---|----------|--------|----------|---------|
| J1 | Mộ Dao (12 node + 7 gossip chuỗi phụ) | PARTIAL | `mo_dao.json`; callsite `StoryNpc.mobInteract:94-100` -> `DialogueService.start`; test `FlowStepsWalkTest.talkAndDuelStepsHaveDialogueSource`, `QuestChainsTest.everyOfferNpcHasDialogueAndBranchesAreReferenced` | `trust_mo_dao`, `mo_dao_left/died` set mà không dùng (I15). Thoại `ch5` (cấp 99) thưởng 5000 EXP + kỹ năng THIEN: đạt được. |
| J2 | Hạng Chiêu thành (`hang_chieu`) + đấu (`hang_chieu_r`) | PARTIAL | `hang_chieu.json`, `hang_chieu_r.json` (city:"" -> chỉ do flow sinh); `StoryFlow.spawnNpc:325` | Hai lựa chọn ch2 `c1/c2` cho kết quả **giống hệt** nhau (cùng action). |
| J3 | Hứa Cổ, Sư Phụ | DONE | `hua_co.json`, `su_phu.json`; flow `step_done` | |
| J4 | Cờ được kiểm trong JSON phải được set ở đâu đó | DONE | `ending_seal/absorb` <- `EndgameService.F_SEAL/F_ABSORB`; `qc.<chain>.<n>` <- `QuestChains.setStage:89-98`; `ch1/2/3/4/5/6_done` <- thoại | Không cờ "mồ côi" phía đọc. |
| J5 | Cờ được set phải được dùng | PARTIAL | Set mà không ai đọc: `rival_saved`, `rival_lost`, `trust_mo_dao`, `mo_dao_left`, `mo_dao_died`, `qc.merchant.c.favor`, `qc.merchant.c.ignore`, `qc.noi_gian.c.proof` | Grep `J/` + 12 JSON. 8 cờ không hệ quả. |
| J6 | Cờ story trong code (`flow2`, `flow_done`, `d:ID`, `step:ID`, `prog:N`, `ending_pending`) | DONE | `StoryFlow.F_V2/F_DONE`, `QuestData.step()/prog()`; đều có điểm set | |

### K. Chuỗi nhiệm vụ phụ (DESIGN-v2-quest-chains)

Hạ tầng: `J/story/chain/QuestChains.java`; test `T/story/chain/QuestChainsTest.java` (stage/offer/compass/countdown/dialogue-branch), `T/guild/EscortMoverTest.java`.

| # | Hạng mục | Status | Evidence | Ghi chú |
|---|----------|--------|----------|---------|
| K1 | Giao việc: NPC ngẫu nhiên cạnh người chơi (50%/20s, cooldown 5', TTL 5') | DONE | `QuestChains.offer:371-391`, `sweep:394`, gọi từ `onPlayerTick:336`; `OFFERS:54-59` | Chỉ ở Tuyến truyện (`storyMode`). |
| K2 | A. `merchant`: hộ tống -> phục kích Sứ Đồ cải trang -> hỏi (truth/ignore) -> thưởng fee/free | DONE | `startEscort:242-255`, `onServerTick:258-290`, thoại `thuong_nhan*.json`; set `c.fee/free/truth/ignore/favor`, `doc a2` | Lỗi nhỏ: `startEscort:252` `withFlag("qc.merchant.at:..")` không xóa `at:` cũ -> sau khi reset, gợi ý khoảng cách bước 5 trỏ đích cũ (`valueOf` lấy cờ đầu). `c.ignore` & `c.favor` không hệ quả. |
| K3 | B. `disciple`: tìm sư đệ -> dọn Hư Thi -> chọn cứu/bỏ -> đợt 2 -> Văn Kiện | DONE | `onStage:100-112`, `onPlayerTick:303-307`, `pickTarget:124`, `dryLand` (tránh nước), thoại `lam_thu/de_tu_lac.json` | Đăng xuất giữa bước 2: wave bị dọn (`onLogout:415`) -> tự lên bước 3 nhưng gợi ý la bàn bước 3 không trỏ đích (`hint:361`). |
| K4 | C. `noi_gian`: Văn Kiện b4 -> (chương>=5) trưởng lão: vạch trần / có bằng chứng / tống tiền / hoãn | DONE | `OFFERS:57-58`, `truong_lao_gian.json`, `REVENGE` `onStage:108`, `onPlayerTick:328-334`; nhánh `proof` mở khi `qc.merchant.c.truth` (đúng "twist liên chuỗi") | `c.proof` set nhưng không đọc. |
| K5 | D. `dying`: bản đồ hang ẩn -> cổng Bí Cảnh ẩn -> (keep: phục kích) | DONE | `onPlayerTick:309-326`, `reward:346-352`; sửa lỗi lặp vô hạn trong Bí Cảnh `:298` | |
| K6 | Lựa chọn rẽ nhánh có tham chiếu về sau (Mộ Dao bình luận, mở lựa chọn) | PARTIAL | 7 gossip ở `mo_dao.json` entry đầu (qc.told.*); `truong_lao_gian.json` cond `qc.merchant.c.truth` | Hạng Chiêu / Hứa Cổ chưa tham chiếu (doc §5 "chưa làm"). |
| K7 | Giao chuỗi qua Bảng thông báo (SectBoard) | MISSING | Doc §5 tự nhận; `SectBoardBlock` không có quest chain | |
| K8 | Độ bền: escort mất khi tắt server; mọi nhánh kết thúc được | PARTIAL | `onLogout:402-417` reset chuỗi merchant; | Nếu server **crash** (không có LoggedOut) khi merchant ở bước 3–6: cờ bước còn nhưng không có Escort -> không lệnh nào gỡ (offer bị chặn vì `stageOf != 0`). Rủi ro thấp (SL6). |

---

## 2. Soft-lock risks

| ID | Mức | Mô tả | Evidence | Gỡ / đề xuất |
|----|-----|-------|----------|-------------|
| SL1 | **CAO (cứng)** | **Tâm Kiếp Mắc Kẹt**: chọn ô Phó Mặc (ô 4) khi roll có `stuckRoll` (~1% mỗi mốc, `CoreRoller.DELTA` cột cuối) => `stuckAt = m`; Ấn Hồn Đài từ chối vĩnh viễn ("hoàn thành nhiệm vụ Phá Chấp") nhưng nhiệm vụ **không tồn tại** => EXP đóng băng ở mốc, không lên cấp, không qua Khảo Hạch. | `J/cores/CoreService.java:110-116`, `J/block/AnHonDaiBlock.java:30-33`, lang `vi_vn.json:118-119`, gỡ duy nhất `HuKhongCommand.java:78-82` | Làm nhiệm vụ "Phá Chấp" (hoặc bỏ cơ chế / cho Tẩy Hồn / gỡ sau N giờ). Hiện chỉ admin gỡ được. |
| SL2 | TB | **Sau Tẩu Hỏa Nhập Ma, truyện không reset**: `tauHoa` đặt `CultivationData.DEFAULT` + roll lại, nhưng `QuestData` (bước `step:cX_...`, chương) & `StoryData` (mode, Văn Kiện, lair) giữ nguyên; `QuestData.withChapter` chỉ tăng. Người chơi cấp 1 bị mục tiêu kiểu "hạ Chấp Hành Giả" ở bước cũ; thoại `ch5/ch6` vẫn dùng cờ cũ. | `J/pathrules/MaRules.java:363-384`; `J/story/npc/QuestData.java:323` ; `StoryFlow.onProgress:193-205` (chỉ tiến) | Reset hoặc re-seat bước theo cấp trong `tauHoa`/`CharCreateService.confirm`. |
| SL3 | TB | **Mốc 95 / 99 không có chỉ dẫn**: EXP khóa ở 95/99 (`CultivationData.java:74`) và Trial bắt buộc (`TrialMath.requiresTrial` cấp>=90), nhưng chương 10 chỉ có `c10_grind` (LEVEL 98, target null) — HUD không trỏ Ấn Hồn Đài như `cN_ms`. Người chơi đứng ở 95 thấy "đạt cấp 98" mà không thể lên. | `FlowSteps.java:73-76`; `StoryFlow.target:215-232` (LEVEL -> null) | Thêm bước `c10_ms95`/`c10_ms99` hoặc target Altar khi `milestoneLocked`. |
| SL4 | TB | **Phế tích Hub vĩnh viễn + mob 55–85 quanh Mộ Dao**: mất Lưu Ly Cảng (đặc biệt trước chương 1–2) => mọi bước TALK Mộ Dao (`pro_meet`, `c1_talk`, `c6_modao`, `c9_modao`, thoại `ch5`/`ch6`) nằm trong `RuinGuard` (24 mob cấp 55–85 + boss). Không tái chiếm được (D5). Chương vẫn nhảy theo cấp nên không kẹt cứng, nhưng mất toàn bộ thoại/thưởng Mộ Dao với người chơi yếu. | `InvasionManager.java:238-243`; `RuinGuard.java:26,34`; `CorruptionManager.schedule:183-192` | Tái chiếm hoặc dời Mộ Dao khỏi Hub khi RUIN. |
| SL5 | THẤP-TB | **Văn Kiện khan hiếm cho `c3_docs`/`c4_docs`/`c6_lair`**: nguồn tự nhiên chỉ Sứ Đồ trong chunk Hư Hóa & cấp>=30; tim Hư Hóa cách spawn 4.000–11.000. Chuỗi phụ cho tối đa 2 Văn Kiện/chuỗi (a2,a5 | b2/b3,b4) < 3 cần để lộ hang. Không chặn cấp (chương theo cấp), nhưng Hứa Cổ (`docs_min 2`) và Mộ Dao `ch4` (`lair 3`) đóng thoại. | `StoryService.java:108-125`, `CorruptionManager.java:55-61`, `StoryDocs.CLUES_FOR_LAIR=3` | Thêm nguồn Văn Kiện sớm (Bí Cảnh, Hiệp Hội) hoặc hạ ngưỡng. |
| SL6 | THẤP | Merchant escort sau **crash server** (cờ `qc.merchant.3..6` còn, `ESCORTS` rỗng) => chuỗi treo, offer bị chặn. Chỉ logout bình thường được xử lý. | `QuestChains.java:402-417`, `offerApplies:74-78` | Thêm dọn cờ khi đăng nhập nếu không có escort. |
| SL7 | THẤP | Tiền đề hang CHG IV (≈12.000 block từ **gốc (0,0)**) vs worldborder tâm = **spawn** bán kính 15.000: nếu spawn lệch gốc >~2.600 theo hướng ngược, hang có thể ngoài border. Chưa kiểm số liệu thế giới thật. | `ChgKit.java:103-115`, `CorruptionManager.java:45-53` | Tính `lairXZ` theo `ow.getSharedSpawnPos()`. |
| SL8 | THẤP (UX) | Bình Cảnh 69/79/89 không có đan (A4): không kẹt (EXP x0,25) nhưng quest "Đột Phá" (Tự do) và mọi đường vật liệu im lặng. | `FreeQuest.java:52` | Thêm phẩm 7–9. |

Đã kiểm và **KHÔNG** thấy soft-lock: bước `KILL` (quái bổ sung tới đủ, `StoryFlow.maintain:450-456`), `DUEL` (NPC sinh lại nếu mất `:457-459`; rival persist), `BICANH` (fallback cổng gần người chơi -> mở thẳng, `:473-492`), `INVASION` (RUIN -> bỏ qua `:466-469`), `SECT` (Hiệp Hội thay thế `:175,268`), `GATE`/`ENDING` (cổng dựng tự động khi đủ điều kiện `EndgameService.gatewayTick:53`), chuỗi phụ `dying` (sửa vòng lặp `:298`).

---

## 3. Top 10 gaps theo tác động người chơi

1. **SL1 Tâm Kiếp Mắc Kẹt không có Phá Chấp** — người chơi kẹt vĩnh viễn một mốc (A9).
2. **Hệ số EXP tư chất & phạt EXP song/tam năng không áp** — tư chất/đa đạo không đổi tốc độ lên cấp, phá Pillar 2/R2 (A12, A11).
3. **Thiếu Phá Cảnh Đan phẩm 7–9, Thiên Địa Linh Vật, Thần Đan** — mốc 70/80/90 chỉ còn đường EXP x0,25 (A4, A5, E11).
4. **Phế tích Hub vĩnh viễn, chỉ 1/10 thành có thể mất, không tái chiếm** — "thế giới bị đe dọa thật" chưa trọn (D4, D5, SL4).
5. **Lựa chọn truyện không hệ quả** — `rival_saved/lost`, `mo_dao_left/died`, `favor/ignore/proof` (J5, I14, I15).
6. **Mốc 95/99 & Tẩu Hỏa làm HUD truyện sai/kẹt hướng dẫn** (SL2, SL3).
7. **Multiplayer lệch spec** — trần 8 vs 10, bán kính 64 vs 48, không ngưỡng đóng góp 5%, loot không chia, Tầng II áp toàn server (G1–G3, H7).
8. **Thiếu thuốc nhóm Tẩy Tủy / Tăng Tu Vi / Thanh Tâm / Tịnh Hư và Tẩy Hồn** — không đổi được tư chất/Lõi, Ma tu không hạ Ác Tính (E3, E5–E7, A10).
9. **Xuất thân chỉ đổi điểm spawn** — không Thư Tiến Cử, không debuff Con Hoang, tuổi khởi đầu random (C3, B6) ; chức vị thành chỉ là danh hiệu không quyền lợi (D2).
10. **Manh mối/Văn Kiện: nguồn khan, HUD trỏ thẳng hang bỏ qua manh mối, III/IV không có manh mối, Bia Công Trạng / Hư Hóa ngoại hình / dimension Tầng II / Ám Ngữ chưa làm** (F3, F6, F11, H5–H7).

---

## 4. Tổng kết đếm trạng thái

(Đếm theo các dòng bảng có cột Status ở §1; dòng nhóm I17/K7 tính MISSING.)

**Tổng 109 hạng mục**: DONE **51** · PARTIAL **36** · MISSING **19** · CHANGED **3**.

| Nhóm | DONE | PARTIAL | MISSING | CHANGED |
|------|------|---------|---------|---------|
| A Cảnh giới/Lõi/vật liệu | 5 | 8 | 5 | 0 |
| B Tuổi | 6 | 2 | 0 | 0 |
| C Tông môn | 7 | 0 | 1 | 0 |
| D Thành/chức vị | 2 | 4 | 1 | 0 |
| E Đan dược | 2 | 4 | 5 | 0 |
| F Phản Thần/Sứ Đồ/CHG | 3 | 5 | 1 | 2 |
| G Multiplayer | 2 | 1 | 2 | 0 |
| H Thế giới/Kết thúc | 4 | 1 | 2 | 1 |
| I Luồng truyện 0–10 | 12 | 6 | 1 | 0 |
| J Thoại NPC | 3 | 3 | 0 | 0 |
| K Chuỗi phụ | 5 | 2 | 1 | 0 |


---

## Phương pháp & giới hạn (AP4/AP15)

- Đọc đủ 3 doc chính (+PLAN-v1 để loại phần v2 đã thay). Grep callsite cho mọi mục đánh DONE.
- **Chưa chạy game / GameTest / JUnit**; "có test" nghĩa là file test tồn tại và đúng chủ đề, không phải đã chạy xanh.
- Không đọc `docs/sections/S1–S8` nên số liệu Thọ Nguyên, tỉ lệ roll, giá đan chưa đối chiếu với S6/S7; các chỗ này ghi "chưa đối chiếu".
- Altar ở tông/Thánh Địa (A8) và Altar Sư Phụ trong cấu trúc NBT: **chưa xác minh** (không đọc `.nbt`).

## Document Lineage

| Version | Document | Status |
|---------|----------|--------|
| v1 | AUDIT-B-canon-story.md (file này) | Current |
