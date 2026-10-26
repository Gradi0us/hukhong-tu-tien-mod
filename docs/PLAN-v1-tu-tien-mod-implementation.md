# PLAN-v1-tu-tien-mod-implementation

**Created**: 2026-10-26
**Last Updated**: 2026-10-26 — Kế hoạch triển khai code P0 (MVP) chi tiết theo mốc + quy trình multi-agent
**Mode**: Planning · **Type**: PLAN · **Language**: vi
**Context**: PLAN-v2 canon, S1–S7 v2, S5-tech §5–§7, REVIEW-v1 (top fix trước khi code)
**Status**: ⛔ Superseded by PLAN-v2-tu-tien-mod-implementation.md

---

## Key Findings

| # | Finding | Chi tiết | Bằng chứng | Độ tin cậy |
|---|---------|----------|------------|------------|
| 1 | **Máy user đủ điều kiện build** | JDK 21.0.10, Git 2.52, VS Code 1.142 đã có | `java -version`, `git --version` (2026-10-26) | CAO |
| 2 | **Agent tự compile + chạy GameTest được** | Chỉ phần nhìn/cảm giác (client) cần user chạy tay → giảm số vòng hỏi đáp | JDK có sẵn; S5 §5.3 | CAO |
| 3 | **P0 = 7 mốc (M0–M6), ~25–40 vòng build-test** | Đạo + Thể, cấp 1–30, 1 thành, Ấn Hồn Đài 4 ô, 10 Lõi, 2 Phá Cảnh Đan, 3 quái Hư Hóa | S5 §6.1 | TRUNG BÌNH (ước lượng) |
| 4 | **Rủi ro lớn nhất là API nhớ nhầm** | Mọi chỗ [KIỂM TRA] phải có "spike" compile trước khi xây lên | S5 R1 | CAO |

---

## Table of Contents
1. [Chuẩn bị](#1-chuẩn-bị)
2. [Quy trình multi-agent khi code](#2-quy-trình-multi-agent-khi-code)
3. [P0 theo mốc M0–M6](#3-p0-theo-mốc-m0m6)
4. [P1–P3 tóm tắt](#4-p1p3-tóm-tắt)
5. [Definition of Done](#5-definition-of-done)
6. [Điểm dừng & tiêu chí đổi hướng](#6-điểm-dừng--tiêu-chí-đổi-hướng)
7. [Việc cần user](#7-việc-cần-user)

---

## 1. Chuẩn bị

| Hạng mục | Trạng thái | Ghi chú |
|----------|-----------|---------|
| JDK 21 | ✅ có | `C:\Program Files\Java\jdk-21.0.10` |
| Git | ✅ có | Cần `git init` trong `D:\mc_mod\` (M0) |
| Editor | ✅ VS Code | Khuyến nghị thêm extension "Extension Pack for Java" |
| Internet | cần | Gradle tải NeoForge + GeckoLib lần đầu (~1–2 GB cache) |
| RAM | ≥ 8 GB | Chạy server + 2 client cùng máy tốn ~5–6 GB |
| Minecraft account | cần cho client thật | Dev run dùng tài khoản offline `Dev`, `Dev2` |

**Cấu trúc repo**: `D:\mc_mod\` = gốc repo (chuyển từ `C:\Users\Minhhoangg\tu-tien-mod` ngày 2026-10-26); `docs/` giữ nguyên; mã mod ở gốc (`build.gradle`, `src/`) theo S5 §5.1.

---

## 2. Quy trình multi-agent khi code

```
             ┌──────────────── 1 MỐC (milestone) ────────────────┐
 Opus        │ ① Viết SPEC mốc (≤1 trang): file được sửa, API,  │
 (plan)      │   tiêu chí test, các [KIỂM TRA] cần spike         │
             └───────────────┬───────────────────────────────────┘
                             ▼
 Haiku       ② Tra docs.neoforged.net / source cho từng [KIỂM TRA] → 5–10 dòng kết luận
 (lookup)                    │
                             ▼
 Sonnet      ③ Viết code đúng danh sách file → ./gradlew compileJava + test + runGameTestServer
 (code)        (tự sửa lỗi compile ≤3 vòng; vượt → dừng báo Opus)
                             │
                             ▼
 Opus        ④ Review diff theo checklist: luật canon, cổng `gates`, AP1 (đã nối callsite/UI?),
 (review)      AP3 (không vòng lặp chờ vô hạn), sync server→client
                             │
                             ▼
 User        ⑤ runServer + runClient (+client2) → làm checklist PASS/FAIL → gửi log nếu lỗi
                             │
                             ▼
             ⑥ PASS hết → cập nhật TASK-v1, gợi ý commit (chỉ commit khi user cho phép)
```

**Tối ưu token**
- Mỗi agent nhận **SPEC mốc + đường dẫn canon/section liên quan**, không nhận cả 7 file.
- Agent trả lời ≤5 dòng; chi tiết nằm trong code/diff.
- Sonnet chỉ được sửa các file liệt kê trong SPEC (chống lan phạm vi).
- Các mốc độc lập (vd M4 Lõi và M5 quái) có thể chạy 2 agent Sonnet song song.

**Git policy (áp dụng cho mọi agent)**: agent **không commit/push**. Opus đề xuất commit `feat(scope): ...` khi mốc PASS; chỉ commit khi user nói rõ.

---

## 3. P0 theo mốc M0–M6

| Mốc | Nội dung | File/Package chính | User kiểm tra (PASS khi) | Vòng ước lượng |
|-----|---------|-------------------|--------------------------|---------------|
| **M0 Khởi tạo** | MDK ModDevGradle 1.21.1, GeckoLib, `git init`, `.gitignore`, run configs `client/client2/server/gameTestServer`; **spike** 5 API: Attachment+Codec, Payload+StreamCodec, DataPackRegistry, `LivingIncomingDamageEvent`, sự kiện finalize spawn | `build.gradle`, `gradle.properties`, `HuKhong.java`, `spike/*` (xóa sau) | `runClient` → danh sách Mods có "Hư Không Tu Tiên Lục"; `runServer` lên không lỗi | 2–4 |
| **M1 Dữ liệu tu sĩ** | `CultivationData` + `AgeData` attachment (copyOnDeath), sync payload, HUD (cấp, cảnh giới, EXP, tài nguyên, tuổi), lệnh `/hukhong dump|setlevel|addexp|age` | `core/`, `age/`, `network/`, `client/ui/hud` | Vào game thấy HUD; `/hukhong setlevel 15` → HUD đổi; chết/thoát vào lại dữ liệu còn; 2 client thấy dữ liệu riêng | 4–6 |
| **M2 Tạo nhân vật + luật đạo** | Màn tạo nhân vật (roll tư chất D→SSS mỗi đạo, Đơn/Song/Tam; P0 chỉ mở Đạo/Thể), `gates` (`Resources.tryConsume`, `RewardGate`, `DamageRules`), luật Đạo tu (không đói, đòn thường lên quái = 0, lên người ×1), Thể tu (vũ khí = 0, Bá Khí), 3+1 phím, mỗi đạo 2 kỹ năng + 1 tối thượng, spawner = 0 thưởng | `pathrules/`, `gates/`, `combat/`, `client/ui/charcreate`, keybinds | Checklist S5 §6.1 mục 1–4 | 6–9 |
| **M3 EXP & Bình Cảnh** | Đường cong `E(L)=40·L^2.15`, hệ số tư chất, khóa EXP ở mốc 10/20/30, Bình Cảnh ở cấp 9/19 (×0,25 EXP, thanh Chướng), **Khai Mạch Đan** + **Ngưng Nguyên Đan** (item + loot tạm), HUD báo Bình Cảnh | `breakthrough/`, `core/CultivationService`, `alchemy/PillItem` (bản tối giản) | Mục 5–6 S5 §6.1: không đan thì chậm, dùng đúng đan qua ngay, sai phẩm vô tác dụng | 4–6 |
| **M4 Ấn Hồn Đài** | Datapack registry Lõi + 10 Lõi JSON (bị động), block Ấn Hồn Đài + `MenuType` 4 ô (3 cùng phẩm + Phó Mặc Số Phận, chưa có Mắc Kẹt 1%), Điểm Vận, set bonus 2 món | `cores/`, `data/hukhong/hukhong/core/*.json`, `client/ui/soulseal` | Lên 10 → đến đài → 4 ô → chọn → bị động có hiệu lực, mở cấp 11; Lõi lưu sau relog; server từ chối chọn sai | 5–7 |
| **M5 Quái Hư Hóa** | 3 quái (Hư Thi/zombie, Hư Cốt/skeleton, Hư Chu/spider) GeckoLib, spawn tự nhiên vòng 1–2, loot Hồn Tinh + EXP theo S3/S4; quái vanilla ×20 EXP ở cấp 1–9 | `entity/`, `assets/hukhong/geo|animations|textures`, `loot_table/` | Gặp quái Hư Hóa ngoài tự nhiên, giết ra Hồn Tinh + EXP; spawner zombie vẫn 0 | 4–6 |
| **M6 Thành & nghiệm thu P0** | 1 thành (jigsaw) chứa Ấn Hồn Đài, `CitySavedData` (Cư Dân, NORMAL/RUIN), lệnh `/hukhong invade` (cắt nếu trễ); chạy **trọn checklist 8 mục P0** trên dedicated server + 2 client | `city/`, `worldgen/`, `structure/` | 8/8 tiêu chí S5 §6.1 PASS | 3–6 |

**Song song được**: M4 ‖ M5 (khác package). Còn lại tuần tự.

**Tổng P0**: 28–44 vòng build-test ≈ 5–8 tuần với 5–8 giờ thử/tuần của user.

---

## 4. P1–P3 tóm tắt

| Phase | Phạm vi chính (xem S5 §6) |
|-------|---------------------------|
| P1 | Cấp 31–60, đủ 4 đạo + đa đạo (chi phí ×2/×3), bay, ~40 Lõi, Đan Lô + luyện đan + Đan Độc, Phá Cảnh Đan 3–5, Tuổi/Thọ Nguyên đầy đủ, 10 thành + chức vị, 3–4 tông, tập kích + Hư Hóa lan, boss scaling, worldborder |
| P2 | Cấp 61–99, Hồn Cốt, Bản Mệnh skill, 7 tông, Sứ Đồ + Văn Kiện, 4 Chấp Hành Giả, Bí Cảnh, tuyến truyện |
| P3 | Cấp 100 + Khảo Hạch, Thần Đan, Nguyên Thủy Tịch Diệt, kết thúc → Tầng Thế Giới II, cân bằng, đóng gói |

Mỗi phase bắt đầu bằng 1 SPEC phase (Opus) + cập nhật TASK-v1.

---

## 5. Definition of Done

Một mốc chỉ ✅ khi đủ 5 mục; thiếu → 🔄 PARTIAL + ghi carry-over.

| # | Mục | Kiểm bằng |
|---|-----|-----------|
| 1 | Spec mốc có | file SPEC hoặc mục trong PLAN này |
| 2 | Code có | danh sách file |
| 3 | JUnit/GameTest xanh | output `./gradlew test runGameTestServer` |
| 4 | **Nối vào game** (UI/phím/lệnh/worldgen) — không còn hàm "treo" | grep callsite + user thấy trong game |
| 5 | **User chạy thật PASS** checklist | báo cáo PASS/FAIL của user |

Tự hỏi trước khi báo xong: *"Xóa unit test đi, người chơi có thấy tính năng này không?"* — Không → chưa nối.

---

## 6. Điểm dừng & tiêu chí đổi hướng

| Tình huống | Hành động |
|-----------|-----------|
| Cùng một lỗi 3 vòng không qua | Dừng, RCA (tìm nguyên nhân gốc) trước khi sửa tiếp |
| GeckoLib không chạy sau 2 vòng ở M5 | Đổi sang model vanilla đổi texture (giữ GeckoLib cho P1) |
| Datapack registry Lõi lỗi sync sau 2 vòng | Tạm nạp Lõi bằng `SimpleJsonResourceReloadListener` + payload tự sync |
| M2 vượt 9 vòng | Cắt kỹ năng xuống 1 + 1 tối thượng/đạo cho P0 |
| P0 vượt 10 tuần | Cắt M6 invade, chỉ giữ thành tĩnh |

---

## 7. Việc cần user

1. **Đồng ý bắt đầu M0** (agent sẽ tạo project trong `D:\mc_mod\`, `git init`, tải NeoForge qua Gradle).
2. **Git**: cho phép agent commit sau mỗi mốc PASS không? Nếu có, dùng tên/email tác giả nào?
3. Trả lời các câu hỏi còn mở trong DESIGN-v2 §4 (không chặn M0–M2).

---

## Document Lineage
| Version | Document | Status |
|---------|----------|--------|
| v1 | PLAN-v1-tu-tien-mod-implementation.md | ✅ Current |

## Related Documents
| Document | Relationship |
|----------|-------------|
| PLAN-v2-tu-tien-mod-canon.md | Luật chung |
| sections/S5-tech.md | Kiến trúc, schema, rủi ro |
| TASK-v1-tu-tien-mod-journey.md | Theo dõi tiến độ |
| REVIEW-v1-tu-tien-mod-design.md | Việc sửa trước khi code |
