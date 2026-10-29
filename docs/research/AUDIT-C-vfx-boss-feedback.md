# AUDIT-C — Rà soát PLAN-v1 (VFX/boss/scaling), PLAN-v2 (shader/resource pack) và 36 yêu cầu phản hồi người chơi

**Ngày tạo**: 2026-10-29
**Cập nhật**: 2026-10-29 — Bản đầu, rà bằng grep + đọc code (read-only), HEAD = 248a6e2
**Phạm vi**: `src/main/java/com/hukhong`, `src/test`, GameTest (`*GameTests.java`)
**Quy ước**: DONE = code + callsite wired + test (unit hoặc GameTest). PARTIAL = thiếu wire (AP1), thiếu test, hoặc làm một phần. MISSING = không có code.
**Giới hạn**: chưa chạy build/test trong lượt rà này; "test" nghĩa là test tồn tại trong repo, chưa xác nhận pass. Phần client-visual (HUD, shader Iris) không có GameTest nên tối đa PARTIAL (AP4).

---

## Key Findings

| Kết quả | Số lượng | Mục |
|---|---|---|
| DONE | 18 | 1, 5, 6, 8, 9, 10, 13, 15, 17, 18, 19, 22, 23, 25, 32, 33, 34, 36 |
| PARTIAL | 16 | 2, 3, 4, 7, 11, 12, 14, 16, 20, 21, 24, 26, 27, 28, 31, 35 |
| MISSING | 2 | 29 (boss phase kiểu Elden Ring), 30 (boss biển) |

Phần lớn PARTIAL là "code đã wire nhưng chưa có test chạy đường đầy đủ" (AP4). Thiếu thật sự (feature gap): 20 (Tụ Linh Đan), 11 (nhạc riêng), 31 (click marker không dịch chuyển), 29, 30.

---

## Bảng chi tiết

Đường dẫn tính từ `src/main/java/com/hukhong/` (code) và `src/test/java/com/hukhong/` (test).

| # | Hạng mục | Status | Evidence | Ghi chú |
|---|---|---|---|---|
| 1 | Bản Mệnh Kiếm không độ bền, không vứt được | DONE | Code: `banmenh/BanMenhKiemItem.java:24-31` (UNBREAKABLE + `getMaxDamage=0`), `:40` `stripDamage`. Callsite: `banmenh/BanMenhEvents.java:185` `onToss` hủy vứt, `:140` `onDrops`, `:217/228` strip save cũ. Test: `banmenh/BanMenhGameTests.java:45` kiemKhongDoBen, `:59` khongVutDuocQvaCtrlQ, `:79` itemEntityBiHuyVaTraChu, `:90` mat_kiem_thi_tai_tao | Đủ 5 mục DoD |
| 2 | Công trình không ở biển trừ cảng | PARTIAL | Code: `city/DrySites.java:12,71-87` (loại ocean/river, tỉ lệ nước <5%). Callsite: `city/CityBuilder.java:52,57`, `story/chain/QuestChains.java:141`. Cảng riêng: `city/PortSites.java:64-71` | Không có test cho DrySites/PortSites (PlacementTest chỉ test jitter/bán kính). Chưa grep hết mọi loại công trình phụ (ruin, tower, village) có đi qua DrySites không |
| 3 | Dựng sẵn khi tạo thế giới ổn định | PARTIAL | Code: `city/Prebuild.java` (342 dòng, ngân sách 8 ms/tick, ticket chunk, watchdog `:115`). Callsite: `city/CityEvents.java:44`, `city/CityService.java:65`, `society/SectEvents.java:63`, overlay `client/PrebuildOverlay.java:20` | Không có test/GameTest cho Prebuild (AP4). "Ổn định" chỉ chứng minh bằng log chạy thật, chưa có bằng chứng trong repo |
| 4 | Thanh máu / số sát thương quái (cả khi Iris) | PARTIAL | Code: `client/combatui/CombatUiHud.java:22-47` (vẽ 2D GuiGraphics), nhánh shader `:30`, `client/combatui/CombatUiClient.java:105`, `client/compat/ShaderCompat.java:15`. Commit 7ee68a8 | Không test (client-only), chưa xác nhận dưới Iris thật. PLAN-v2 A2/B chưa làm: `AmbientPost` không gọi `ShaderCompat` (grep `shadersActive` chỉ ra CombatUi + PhapBeamClient) |
| 5 | Độ khó kiểu Scaling Health (Kiếp Số) | DONE | Code: `scaling/ScalingEvents.java:106` `onSpawn`, `:98` modifier, `scaling/ScalingConfig`. Callsite: `scaling/ScalingMod.java:33` đăng ký event + payload; HUD `client/hud/KiepHud.java`. Test: `scaling/ScalingGameTests.java:22`, `test/.../scaling/DifficultyTest.java:8-19` | Cân bằng thực tế chưa kiểm (PLAN-v1 S1-S7) |
| 6 | Quái Hư không biến dân làng thành zombie | DONE | Code+callsite: `events/VillagerGuard.java:17-35` (@EventBusSubscriber, `blocksConversion`). Test: `liberation/LiberationGameTests.java:50` danLangKhongThanhZombieVillager | Đủ |
| 7 | NPC cốt truyện đứng trên đất | PARTIAL | Code: `story/npc/NpcPlacement.java:65-79` (`needsReground`, tắt NoGravity). Callsite: `story/npc/StoryNpc.java:57-72` | Chỉ có `travel/TravelGameTests.java:30` (respawn NPC), không có test riêng cho reground |
| 8 | Đếm ngược 30s trước đợt đầu | DONE | Code: `story/flow/WaveCountdown.java:23` `SECONDS=30`. Callsite: `story/flow/StoryFlow.java:146,442,451-454`. Test: `test/.../story/chain/QuestChainsTest.java:39` countdownTiming (+ `FlowStepsWalkTest`) | Áp cho mọi bước KILL, không riêng "làng đợt đầu" |
| 9 | Hư Không đột kích: xác suất + cấp = TB người trong 100 block | DONE | Code: `events/VoidRaid.java:64,161` (`chance`), `events/RaidLevel.java:15-27` (RADIUS=100, `fromAverage`, +30% vùng nhiễm). Test: `liberation/LiberationGameTests.java:24,33` | Đủ |
| 10 | NPC hộ tống thông minh hơn | DONE | Code: `guild/EscortMover.java:17-93` (waypoint, chờ/tiếp tục hysteresis, groundY). Callsite: `guild/GuildCourier.java:37,71,115`, `story/chain/QuestChains.java:235` (có phục kích giữa đường `:276-280`). Test: `test/.../guild/EscortMoverTest.java:11,24` | Đủ |
| 11 | Vùng nhiễm: hiệu ứng, boss trung tâm, nhạc riêng, quest "Giải phóng vùng đất" | PARTIAL | Code: `liberation/LiberationManager.java:61-156` (HeartArena, boss gác, objective "Giải phóng vùng đất" `:148-149`), hiệu ứng `events/CorruptBuff.java`, không khí commit d88ce85. Test: `liberation/LiberationGameTests.java:67` damTamHuHoaTatDinhVaCapBoss | **Nhạc riêng MISSING**: `LiberationManager.java:58` ghi "chưa có track riêng", boss dùng `BossMusic.watch("hu_vuong")` (`entity/HuVuong.java:120`). `sounds/music/` không có track liberation |
| 12 | Icon kỹ năng đã học | PARTIAL | Code: `client/hud/SkillHud.java:33`, `skillbook/client/CongPhapScreen.java:210-213`. Assets: `textures/gui/skill/` có đủ 19/19 png kỹ năng sách + 24 Pháp + 5 Bản Mệnh (đối chiếu `skillbook/BookSkill.java`) | Không có test kiểm tra "mỗi skill id có texture" (dễ vỡ khi thêm skill) |
| 13 | Chuỗi nhiệm vụ MMORPG có twist | DONE | Code: `story/chain/QuestChains.java:47-117` (4 chuỗi MERCHANT/DISCIPLE/NOI_GIAN/DYING; twist nội gián `:58,108,117`, phục kích `:280,317`). Callsite: `story/npc/Dialogue.java:70` (`qc`). Test: `test/.../story/chain/QuestChainsTest.java:15,57` | Đủ |
| 14 | Minimap + ping quái | PARTIAL | Code: `client/minimap/MinimapHud.java:97-107,231-243` (ping, drawMobs ≤64), `ClientConfig.minimapMobs()`. Commit 3a7e6d4, 484e837 | Không có test (client-only) |
| 15 | Dịch chuyển nhanh tới khu nhiệm vụ | DONE | Code: `travel/TravelService.java:66,159` (kênh, hồi chiêu, hazard), id `obj` `:47`. Callsite: `client/story/JournalScreen.java:58-62` (`TravelNet.Go`), `HuKhong.java:19`. Test: `travel/TravelGameTests.java:30,50` (chặn khi đang chiến đấu) | Đủ; chỉ mở từ Nhật ký (xem #31) |
| 16 | Hiệp Hội đặt bên phải bàn thờ (thành/cảng) | PARTIAL | Code: `guild/GuildBuilder.java:46` (`AltarShrine.procT`), log "east of the Altar Shrine" `:70`. Callsite: `city/CityBuilder.java:128,165`, `guild/GuildEvents.java:72-78` | `GuildGameTests` chỉ test nhiệm vụ kill/tiền, không test vị trí. Chưa xác nhận hướng "phải/đông" trong cảng |
| 17 | Hiệp Hội 6 hạng | DONE | Code: `guild/GuildMath.java:14,27-36` (RANKS=6, điểm+cấp, giảm giá 5%/hạng). Callsite: `guild/GuildService`, `GuildMenu`. Test: `test/.../guild/GuildMathTest.java:11,24,42,58`, `guild/GuildGameTests.java:26` | Đủ |
| 18 | Tử Tinh chỉ trong ví, hiện ở bảng K, popup có icon | DONE | Code: `currency/CurrencyEvents.java:28-47` (rơi thẳng vào ví), `:57-70` onPickup hủy nhặt + `Wallet.give`, `:74` tick quy đổi, `currency/Wallet.java:37-48,51`. Hiển thị K: `client/screen/profile/ProfileScreen.java:220`. Popup icon: `currency/TuTinhText.java:14-21`. Test: `test/.../currency/TuTinhTest.java`, `WalletMathTest.java` | Ngoại lệ: quái không có người giết / rương Bí Cảnh (`bicanh/BiCanhManager.java:392`) vẫn tạo item, rồi bị `onTick` quy đổi vào ví sau ≤1s |
| 19 | Đói chậm 5×, ngoài chiến đấu hồi 3%/s | DONE | Code: `world/HungerRules.java:14` (giữ 20% exhaustion), `world/HealRules.java:30-34` (100 tick, 0.03/s). Test: `test/.../world/W2MathTest.java` | Đủ (xác nhận tên test qua grep, chưa đọc từng case) |
| 20 | Đường cong EXP + Tụ Linh Đan | PARTIAL | Đường cong: `core/CultivationService.java:14-31` (`expToNext`), test `test/.../core/CultivationServiceTest.java:9-44`; EXP quái `entity/HuHoa.java:186`, commit 3d223b9, f4321e8 | **Tụ Linh Đan / viên EXP không tồn tại**: danh sách `ITEMS.register` chỉ có chi_huyet/hoi_xuan/giai_doc/tho_nguyen/khai_mach/ngung_nguyen/tieu_kinh/tieu_ban_than (đan phá cảnh). `CultivationService.addExp` chỉ gọi từ quái, raid, gate, lệnh debug. MISSING phần đan |
| 21 | Buff Thể: đi trên nước, nín thở ×5, tốc/nhảy, Kiên Giáp ×2, Trọng Thương, Huyết Khí | PARTIAL | Code: `pathrules/TheBody.java:27-91` (@EventBusSubscriber; BREATH_MULT=5, jump, water), Kiên Giáp `passive/PassiveService.java:166`, `passive/TrongThuong.java:72-79`. UI: `client/screen/profile/PassiveEntries.java:42-59`. Test: `gametest/TrongThuongGameTests.java`, `test/.../passive/HuyetKhiTest.java` | Đi trên nước / nín thở / tốc-nhảy / Kiên Giáp ×2 không có test. Chưa kiểm hệ số "×2" của Kiên Giáp |
| 22 | Mỗi đạo có bản mệnh nâng cấp được | DONE | Code: Kiếm `banmenh/BanMenhKiemItem`; Đạo/Thể/Ma `linhbao/LinhBaoType.java:7-9` (Đạo Linh, Linh Thể, Ma Công); Pháp `phaptu/linhtruong/LinhTruongService.java:56-57`. Nâng cấp: `artifact/ArtifactService` → `client/screen/profile/ProfileScreen.java:499` (`ArtifactUpgradePayload`), `linhbao/LinhBao.java:23`. Test: `linhbao/LinhBaoGameTests.java`, `phaptu/linhtruong/LinhTruongGameTests.java`, `test/.../artifact/ArtifactMathTest.java`, `LinhBaoMathTest`, `LinhTruongMathTest` | Đủ |
| 23 | Bảng K: Nội Tại đủ mọi đạo (Pháp 2 nội tại) + tab Kỹ Năng có sát thương | DONE | Code: `client/screen/profile/PassiveEntries.java`, `SkillEntries.java:61-71` (sát thương thật từ hằng số). Callsite: `ProfileScreen`. Test: `test/.../phaptu/PhapPassiveEntriesTest.java:12,24`, `test/.../client/SkillEntriesTest.java:41-109` | Đủ |
| 24 | Phím G (thế đứng) tự cầm vũ khí / tay không | PARTIAL | Code: `skill/StanceHands.java`. Callsite: `skill/SkillService.java:41,62`, `network/SkillNet.java:38`, `client/keybind/SkillKeyHandler.java:39` | Không có test nào gọi `StanceHands` (grep toàn repo) |
| 25 | Pháp T tia (Quang/Lôi/Hỏa/Thủy) +35% bạo kích; Quang E Thiểm Bộ; Lôi E Lôi Trận | DONE | Code: `phaptu/beam/PhapBeamService.java`, `PhapBeamMath.java:38` (`FIRE_CRIT_BONUS=0.35`), `phaptu/ThiemBoService.java:64-181`, `ThiemBoMath.java:39-40`. Callsite: `network/SkillNet.java:20-21`, `skill/SkillService.java:40,57`. Test: `phaptu/beam/PhapBeamGameTests.java:62-128`, `phaptu/ThiemBoGameTests.java:49-96`, `PhapBeamMathTest`, `ThiemBoMathTest` | Đủ. Thứ tự đổi đạo (G/stance) hủy channel đã wire |
| 26 | Bí Cảnh: portal fallback, EXP ×2, cấp quái = cấp bậc, boss ×1.8 không BossBuff, map quest không lặp, ra không kẹt hang | PARTIAL | EXP ×2: `bicanh/BiCanhRewards.java:12-16`. Cấp: `bicanh/BiCanhManager.java:349`. Boss ×1.8: `:340`, dùng `:309`; chặn BossBuff `events/BossBuff.java:50`. Thoát mặt đất: `BiCanhManager.java:179,210`. Chuỗi map không lặp: commit cbfdf18 (`QuestChains`), fallback cổng: commit 0b1493c (`story/flow/StoryFlow.java`) | `test/.../bicanh/BiCanhTest.java` chỉ test loot/layout/tier. Không có GameTest cho EXP ×2, cấp quái, ×1.8, thoát hang, chuỗi map. Comment `:339` còn ghi "truoc 1.8 - qua manh" gây khó hiểu |
| 27 | Quái chỉ rơi vật phẩm mod | PARTIAL | Code+callsite: `world/ModDrops.java:28` (`LivingDropsEvent`), commit 1c32f1e | Không có test (grep `ModDrops` trong test và gametest = 0) |
| 28 | Lính thành đánh quái Hư ngoài lúc công thành | PARTIAL | Code: `city/ambient/AmbientNpc.java` (goal mới trong commit 248a6e2: chọn `getTarget`, tick, stop) | Không có test (grep `AmbientNpc` trong test/gametest = 0). Commit 248a6e2 chưa có GameTest |
| 29 | Chuyển phase boss kiểu Elden Ring (cutscene, phụ đề, bất tử, đổi đấu trường) | MISSING | Hiện chỉ: `entity/WorldBoss.java:297-308` `enterPhase` = animation + hạt + tiếng rồng + actionbar "Phase N". Không có `BossPhaseDirector`, không camera cutscene boss, không phụ đề/thoại (grep `BossPhaseDirector`=0; `CutsceneScreen` chỉ dùng cho kết thúc game `client/endgame/EndgameClient.java:13`). Phụ đề chỉ ở Chấp Hành Giả/Bí Cảnh (`chg/ChapHanhGia.java:233`) | PLAN-v1 B1-B7 chưa làm. Boss không bất tử khi đổi phase |
| 30 | Boss biển | MISSING | grep `Hải Long`/`hai_long`/ocean boss = 0 kết quả. PLAN-v1 §5.4 ghi "Boss biển (chưa có)" | Cần Hải Quỷ/Thủy Xà + vùng biển (PLAN-v1 đợt 10) |
| 31 | Click marker bản đồ để dịch chuyển | PARTIAL | `client/minimap/FullMapScreen.java:176-209`: click marker/danh sách chỉ `toggleWaypoint`, chuột phải đặt waypoint. Dịch chuyển nhanh chỉ gọi ở `client/story/JournalScreen.java:58` | Chưa có đường từ FullMapScreen sang `TravelNet.Go` (AP1: 2 hệ rời nhau) |
| 32 | Thế giới nội tâm Tâm Ma | DONE | Code: dimension `data/hukhong/dimension/tam_thuc.json`, `trial/TrialService` (`TAM_THUC`), `entity/TamMa.java`, client `client/TamThucClient.java`, payload `network/SyncPayload.java:41`. Test: `gametest/HuKhongRuntimeGameTests.java:391` trialTamMaMovesToTamThucAndBack, `:147` entityTamMa, `test/.../trial/TrialMathTest` | GameTestServer chỉ nạp overworld nên test có nhánh dự phòng (`:408`): vào chiều thật chưa chứng minh trong test |
| 33 | Cổng hang vào Bí Cảnh | DONE | Code: `bicanh/BiCanhCave.java` (plan/build/collapse). Callsite: `bicanh/BiCanhEvents.java:80-84`, `BiCanhManager.java:122`. Test: `voidblock/VoidGameTests.java:24` (BiCanhCave.plan), `test/.../bicanh/BiCanhTest.java:60` layoutChain | Hang chỉ đặt trên đất liền: commit 37e8052 |
| 34 | Pháp tu 6 hệ + màn chọn hệ | DONE | Code: `phaptu/PhapElement.java:8-10` (6 hệ). Callsite: `charcreate/CharCreateService.java:76` (`roll`), `client/screen/charcreate/CharCreateScreen.java:47` → `PhapElementScreen`. Test: `test/.../phaptu/PhapMathTest.java`, `PhapPassiveEntriesTest.java` | Đủ |
| 35 | Thành/cảng từ map nhập | PARTIAL | Code: `structures/StructureLibrary.java:29,44,61,110` (.nbt/.schem + markers.json), `StructurePlacer`, `city/CityBuilder.java:165`. Commit 39a06bc, 3a7e6d4; báo cáo `docs/research/REPORT-v1-imported-maps.md` | Không có unit test cho `StructureParser`/`LoadedStructure`. Tồn tại fallback dựng procedural nên không chứng minh mọi thành dùng map nhập |
| 36 | Animation tung chiêu mọi kỹ năng (playerAnimator) + combo đấm Thể | DONE | Code: `client/anim/CastAnimLibrary`, `CastAnimMap`, `CastAnimHook.java:15-27`. Callsite: `skill/SkillService.java:83,100,110`, `phaptu/beam/PhapBeamService.java:122-199`, `client/anim/FistComboEvents.java:36`; `build.gradle:144` playerAnimator bắt buộc. Test: `test/.../client/anim/CastAnimMapTest.java:13` everySkillHasAnimation, `FistComboTest.java:11` | Kết quả hiển thị thực tế chưa có ảnh kiểm trong `docs/vfx-review/` (PLAN-v1 §3 yêu cầu) |

---

## Đối chiếu PLAN-v1 / PLAN-v2 (ngoài 36 mục)

| Khối kế hoạch | Trạng thái quan sát | Evidence |
|---|---|---|
| PLAN-v1 S (cân chỉ số S1-S7) | PARTIAL: có `tools/balance_sim.py` (commit 4e3f699), `test/.../balance/BalanceSimTest.java`, `HuHoaBalanceTest`; chưa xác nhận S3 chỉnh hệ số, bỏ nhảy ×2 cấp 100 | `git log` |
| PLAN-v1 F (nền VFX F1-F8) | PARTIAL-DONE: vệt, decal, cự kiếm, phản hồi đòn, chất lượng (commit 6cfe49d); `client/fx/FxTrail, FxDecal, FxGiantSword, FxHit` | grep file list |
| PLAN-v1 K (19 kỹ năng sách VFX) | Có commit 6cfe49d; không có ảnh `docs/vfx-review/` | commit log |
| PLAN-v1 B (khung boss B1-B7) | MISSING (xem #29) | — |
| PLAN-v1 M (`MobMove`, quái mới, tiền tố tinh anh) | MISSING: không có class `MobMove`; kỹ năng quái vẫn trong `entity/HuSkill.java` | file list |
| PLAN-v1 V (lồng tiếng) | Đã chốt bỏ (chỉ phụ đề) | PLAN-v1 header |
| PLAN-v2 A1 (`ShaderCompat`) | DONE một phần: `client/compat/ShaderCompat.java` tồn tại, dùng ở CombatUi + PhapBeam | grep |
| PLAN-v2 A2/B (tắt PostChain + fog/sky khi Iris) | MISSING: `client/ambient/AmbientPost.java` không tham chiếu `ShaderCompat` | grep `shadersActive` |
| PLAN-v2 C (ma trận chụp), D (resource pack), E (README) | MISSING / cần người chơi | PLAN-v2 trạng thái ⬜ |

---

## Top 10 gaps (theo ảnh hưởng người chơi)

| # | Gap | Mục | Vì sao quan trọng |
|---|---|---|---|
| 1 | Boss không có chuyển phase điện ảnh (cutscene, phụ đề, bất tử 3-6s, đổi đấu trường); chỉ có chữ "Phase N" | 29 | Tính năng nổi bật mà người chơi yêu cầu, hiện hoàn toàn thiếu (PLAN-v1 B1-B7) |
| 2 | Không có Tụ Linh Đan / viên EXP | 20 | Người chơi hỏi rõ; không có nguồn EXP chủ động ngoài đánh quái |
| 3 | Chưa có boss biển và quái biển | 30 | Vùng biển/cảng không có endgame content |
| 4 | Vùng nhiễm dùng nhạc Hư Vương, chưa có nhạc riêng | 11 | Yêu cầu nêu rõ; code chú thích "chưa có track riêng" |
| 5 | Click marker bản đồ không dịch chuyển (chỉ đặt waypoint); dịch chuyển chỉ mở từ Nhật ký | 31 | AP1: hai hệ rời nhau, người chơi kỳ vọng click là đi |
| 6 | Post shader/fog dưới Iris chưa xử lý (`AmbientPost` không kiểm `ShaderCompat`); PLAN-v2 A2/B/C chưa làm | 4 | Người dùng shader có thể thấy trời tím/hậu kỳ sai hoặc mất; HUD thanh máu đã xử lý nhưng chưa kiểm thật |
| 7 | Bí Cảnh (EXP ×2, cấp quái, boss ×1.8, thoát hang, chuỗi map không lặp) không có GameTest; từng nhiều lần hồi quy (commit 0b1493c, cbfdf18, 0f83312, b47fa32) | 26 | Hồi quy lặp lại, không có lưới an toàn |
| 8 | Prebuild khi tạo thế giới và đặt công trình tránh biển không có test; chỉ tin log chạy thật | 2, 3 | Lỗi ở đây làm hỏng cả thế giới mới, khó phát hiện sớm |
| 9 | Buff Thể (đi nước, nín thở, tốc/nhảy, Kiên Giáp ×2) và G-stance không có test; lính thành tự đánh quái (248a6e2) và `ModDrops` không có test | 21, 24, 27, 28 | AP4: chỉ unit test toán học, đường người dùng chưa được kiểm |
| 10 | Chưa có ảnh kiểm VFX/animation (`docs/vfx-review/`, `docs/shader-review/`) và chưa chạy ma trận Iris | 36, 4 | PLAN-v1 §3 và PLAN-v2 §3 yêu cầu bằng chứng hình ảnh trước khi đóng |

---

## Document Lineage

| Version | Document | Focus | Status |
|---|---|---|---|
| v1 | docs/research/AUDIT-C-vfx-boss-feedback.md (file này) | Rà PLAN-v1 vfx-boss + PLAN-v2 shader + 36 phản hồi | ✅ Current |

Liên quan: `docs/PLAN-v1-vfx-boss-mob-scaling.md`, `docs/PLAN-v2-shader-resourcepack.md`, `docs/DESIGN-v2-quest-chains.md`.
