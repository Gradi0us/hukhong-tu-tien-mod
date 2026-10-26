# DESIGN-v2-tu-tien-mod-master

**Created**: 2026-10-26
**Last Updated**: 2026-10-26 — v2.1: áp 25 câu trả lời của user, thêm S6 (tuổi/tông/thành) + S7 (đan dược), rà soát chéo (19 mâu thuẫn, 9 exploit) và sửa theo 21 phán quyết; lập kế hoạch triển khai code
**Mode**: Planning · **Level**: L3-Analyze · **Type**: DESIGN · **Language**: vi
**Status**: ✅ Thiết kế đủ để code P0 — còn 8 câu hỏi nhỏ + 4 giả định chờ user xác nhận (§4)
**Researcher**: Opus (planner/phán quyết) · Sonnet ×10 lượt (viết/sửa/rà soát) · Haiku ×3 (search)

---

## Key Findings

| # | Finding | Chi tiết | Bằng chứng | Độ tin cậy |
|---|---------|----------|------------|------------|
| 1 | **7 phần thiết kế đã nhất quán** | 19 mâu thuẫn chéo được rà ra và sửa theo 21 phán quyết (canon §14) | REVIEW-v1 + 2 lượt sửa, grep số cũ sạch | CAO |
| 2 | **Cơ duyên quyết định tốc độ** | Tư chất B: đủ vật liệu ≈110 giờ, nửa ≈321 giờ, không vật liệu ≈496 giờ; D ≈633 giờ, tam năng ≈892 giờ (nhờ "Bù Tư Chất") | S3 v2.1 — mô hình Fermi | THẤP (chưa playtest) |
| 3 | **Chống lách luật bằng một cổng duy nhất** | `KillRateGate` (farm quái), spawner = 0, đan đột phá ràng buộc người nhặt, roll Lõi lưu seed trước | canon §14 R3/R5/R8, S5 | TRUNG BÌNH |
| 4 | **Không ai chết vì già** | Tuổi chỉ chạy khi online; Ngũ Suy dừng ở giai đoạn V, gỡ được | canon §14 R1, S6 | — (quyết định thiết kế) |
| 5 | **Multiplayer**: boss scale theo người **thật sự đóng góp** | `HP × (1 + 0,75·(n−1))`, n = người gây ≥5% sát thương/hồi máu, tối đa 10 | canon §10, R16 | TRUNG BÌNH |
| 6 | **P0 chia 7 mốc, agent tự build/test** | Máy đã có JDK 21 + Git + VS Code | PLAN-v1-implementation | CAO |

---

## 1. Bản đồ tài liệu

| File | Nội dung |
|------|---------|
| [PLAN-v2-tu-tien-mod-canon.md](PLAN-v2-tu-tien-mod-canon.md) | **Luật chung** (§0–13) + **phán quyết R1–R21** (§14) |
| [sections/S1-lore.md](sections/S1-lore.md) | Thế giới, Phản Thần Dạ Khuyết, Sứ Đồ, 4 Chấp Hành Giả (67/78/86/98), 7 tông, 10 thành, cốt truyện theo Hồi (tuyến truyện + tự do), 2 kết thúc → Tầng II, mảnh truyện thu thập |
| [sections/S2-paths.md](sections/S2-paths.md) | 4 đạo, đa đạo dùng đồng thời (chi phí ×2/×3), Đạo tu hỗ trợ, Ma tu 2 lộ + Ác Tính, 3+1 phím, tư chất, xuất thân, pháp bảo, mở khóa dần |
| [sections/S3-cores.md](sections/S3-cores.md) | EXP + Bình Cảnh, Bù Tư Chất, Ấn Hồn Đài 4 ô (Phó Mặc Số Phận, 1% Mắc Kẹt), 32 Lõi, 8 Hệ, Hồn Cốt, Tẩy Hồn, Khảo Hạch, phạt chết |
| [sections/S4-world.md](sections/S4-world.md) | Bản đồ ±7.500, vị trí 10 thành + 7 tông, Sứ Đồ + Văn Kiện, Chấp Hành Giả giáng lâm/hang ổ, boss multiplayer, tập kích, Tầng II |
| [sections/S5-tech.md](sections/S5-tech.md) | NeoForge 1.21.1, 20 module, code mẫu, schema JSON, roadmap P0→P3, 18 rủi ro |
| [sections/S6-society.md](sections/S6-society.md) | Tuổi & Thọ Nguyên, 7 tông (ứng tuyển, bậc, nhiệm vụ, buff), 10 thành + chức vị đến Thành Chủ |
| [sections/S7-alchemy.md](sections/S7-alchemy.md) | 39+ đan dược, 9 Phá Cảnh Đan + 7 Tiểu Phá Đan + Thông Thiên Thần Đan, Linh Vật, Dị Hỏa, luyện đan, kinh tế Linh Thạch |
| [REVIEW-v1-tu-tien-mod-design.md](REVIEW-v1-tu-tien-mod-design.md) | Rà soát: mâu thuẫn, exploit, inversion, góc nhìn đa chiều |
| [PLAN-v1-tu-tien-mod-implementation.md](PLAN-v1-tu-tien-mod-implementation.md) | **Kế hoạch triển khai code** |
| [TASK-v1-tu-tien-mod-journey.md](TASK-v1-tu-tien-mod-journey.md) | Theo dõi tiến độ |

Tổng ≈ 58.000 từ (7 phần).

---

## 2. Vòng lặp chơi v2

```
 TẠO NHÂN VẬT: xuất thân (tuổi 6–16) + tư chất từng đạo + đơn/song/tam năng
        │
        ▼
 TU LUYỆN ── quái Hư Hóa · nhiệm vụ thành/tông · sự kiện · bí cảnh · thiền
        │      (quái vanilla gần 0 · spawner = 0 · KillRateGate chống farm)
        ▼
 BÌNH CẢNH (cấp 9/19/…/89) ── có Phá Cảnh Đan / Linh Vật? ── Có → qua ngay
        │                                                    Không → EXP ×0,25 (rất lâu)
        ▼
 ẤN HỒN ĐÀI: 3 Lõi cùng phẩm + "Phó Mặc Số Phận" (cao/thấp/1% Mắc Kẹt → Phá Chấp)
        │
        ├─► XÃ HỘI: ứng tuyển tông (tư chất+cảnh giới+tuổi) · thăng chức thành → Thành Chủ
        ├─► ĐAN ĐẠO: hái linh dược · Đan Lô + Dị Hỏa · Đan Độc
        └─► CỐT TRUYỆN: Sứ Đồ (≤65) → Văn Kiện → hang ổ CHG I (67) / II (78)
                         → manh mối CHG III (86) / IV (98) → 4 Mảnh Thiên Môn
                         → Khảo Hạch (Tâm Ma · Lôi Kiếp · Nguyên Thủy Tịch Diệt) → THẦN
                         → Phong Ấn / Hấp Thụ → TẦNG THẾ GIỚI II
 ÁP LỰC: Xâm Thực lan → tập kích thành (khi có cư dân online) → Phế Tích ⇄ tái chiếm
```

---

## 3. Thay đổi chính so với v1

| Chủ đề | v1 | v2.1 |
|--------|----|------|
| Phản diện | Hư Không có twist "từng là Thần" | Hư Không ngoại lai + **Phản Thần Dạ Khuyết** phản bội nhân loại + **Sứ Đồ** |
| Chấp Hành Giả | Thứ tự cố định theo Hồi | Sự kiện ngẫu nhiên / hang ổ; cấp 67-78-86-98; manh mối từ Sứ Đồ |
| Kết thúc | 2–3 kết, twist | Phong Ấn hoặc Hấp Thụ → **Tầng Thế Giới II** (dimension mới) |
| Tiến trình | ~110 giờ cố định | 110–500 giờ tùy **vật liệu đột phá** (Bình Cảnh) |
| Đa đạo | 1 đạo hoạt động, đạo phụ 40% | **Dùng đồng thời**, chi phí ×2/×3, trần "Phân Tâm" |
| Đạo tu | Chỉ phép | + **hỗ trợ** (buff Máu/Công/Tốc/Linh Lực), đánh người ×1 |
| Ma tu | Giết mọi thứ | Chỉ hút **NPC** (quota), 2 lộ, Ác Tính → Tẩu Hỏa Nhập Ma, không hỗ trợ |
| Ấn Hồn Đài | 3 ô (+ô Hỗn Loạn) | **4 ô** với Phó Mặc Số Phận; Lõi vĩnh viễn, Tẩy Hồn đắt |
| Phím | 4 + 1 | **3 + 1** |
| Mới | — | Tuổi/Thọ Nguyên, 7 tông 3 cấp, 10 thành 3 cấp + chức vị, đan dược + luyện đan, border 15.000, boss scale theo người |

---

## 4. Còn chờ user

### 4.1 Giả định Opus đã tự diễn giải (xác nhận hoặc sửa)

| # | Câu trả lời gốc | Mình hiểu là |
|---|----------------|-------------|
| G1 | Đạo tu buff "máu, **tc**, mana" | tc = **cả Tấn Công lẫn Tốc Độ** |
| G2 | Phó Mặc Số Phận "1% **mắc kẹt tại đây**" | Kẹt ở cấp hiện tại cho đến khi làm nhiệm vụ "Phá Chấp" (không kẹt vĩnh viễn) |
| G3 | Thế giới "giới hạn tại **15k block**" | Đường kính 15.000 (±7.500 từ tâm) |
| G4 | Ma tu tẩu hỏa nhập ma "**chết luôn**" | Chết ngay + suy yếu nặng + mất Huyết Tế, hồi sinh tu lại được (không xóa nhân vật) |

### 4.2 Phán quyết lớn Opus đã chọn (nói nếu muốn khác)
- Không ai chết vì già (R1). · Thăng Thần khóa sau cốt truyện (cần 4 Mảnh) ở cả chế độ tự do (S3 Q1). · Kết thúc là lựa chọn **cá nhân** từng người chơi (R18). · Manh mối Chấp Hành Giả **chung server** (R19).

### 4.3 8 câu hỏi nhỏ còn mở (không chặn code P0)

| # | Câu hỏi | ★ Khuyến nghị |
|---|---------|--------------|
| 1 | Dạ Khuyết là boss ẩn tùy chọn ở Hồi 8? | Có |
| 2 | Nhân vật đồng hành Mộ Dao có thể hy sinh trong tuyến truyện? | Có (tùy lựa chọn) |
| 3 | Người chơi được tố cáo nội gián Sứ Đồ trong tông? | Có |
| 4 | Trần đa đạo "Phân Tâm" (song ×0,8 công, tam ×0,7) | Chấp nhận |
| 5 | Kỹ Năng Bản Mệnh dùng chung ô Tối Thượng (chọn 1/2) | Chấp nhận |
| 6 | Ma tu giết người chơi | Chỉ khi hai bên bật "chiến" |
| 7 | Khiêu chiến Thành Chủ bằng PvP | Tắt, dùng bầu/đóng góp |
| 8 | Thăng Thần cần hạ đủ 4 Chấp Hành Giả kể cả chế độ tự do | Có |

---

## 5. Kế hoạch triển khai code (tóm tắt — chi tiết: [PLAN-v1-implementation](PLAN-v1-tu-tien-mod-implementation.md))

| Mốc | Nội dung | Vòng build-test |
|-----|---------|-----------------|
| M0 | Project NeoForge 1.21.1 + GeckoLib, git init, spike 5 API | 2–4 |
| M1 | Dữ liệu tu sĩ + HUD + tuổi cơ bản + lệnh debug | 4–6 |
| M2 | Tạo nhân vật (tư chất) + luật Đạo/Thể + `gates` + spawner = 0 | 6–9 |
| M3 | EXP + Bình Cảnh + Khai Mạch Đan / Ngưng Nguyên Đan | 4–6 |
| M4 | Ấn Hồn Đài 4 ô + 10 Lõi (song song M5) | 5–7 |
| M5 | 3 quái Hư Hóa (song song M4) | 4–6 |
| M6 | 1 thành + nghiệm thu P0 trên dedicated server | 3–6 |

Quy trình mỗi mốc: **Opus** viết spec → **Haiku** tra API chưa chắc → **Sonnet** viết code + tự compile/test → **Opus** review → **bạn** chạy game thử theo checklist → PASS thì sang mốc sau. P0 ≈ 5–8 tuần.

---

## 6. Sơ đồ tổng hợp

```
┌──────────────────────── HƯ KHÔNG TU TIÊN LỤC v2.1 ────────────────────────────┐
│ XUẤT THÂN(tuổi) ─► TƯ CHẤT D→SSS ─► 4 ĐẠO (đồng thời, ×2/×3 chi phí)          │
│                                        │                                      │
│  NGUỒN EXP ◄─ KillRateGate/spawner=0   ▼                                      │
│  quái Hư Hóa · Sứ Đồ · nhiệm vụ ─► CẢNH GIỚI 1→100 ─► BÌNH CẢNH ◄─ S7 ĐAN/LINH VẬT │
│                                        │                    ▲  (bind, Đan Độc) │
│                                        ▼                    │                 │
│                          ẤN HỒN ĐÀI 4 ô ─► LÕI/HỆ/HỒN CỐT    │  ĐAN ĐỈNH TÔNG   │
│                                        │                    │       │         │
│  S6 XÃ HỘI: TUỔI(online) ─► ỨNG TUYỂN 7 TÔNG ─► buff/nhiệm vụ ┘       │         │
│             10 THÀNH ─► chức vị ─► THÀNH CHỦ ─► phòng thủ tập kích ◄─ XÂM THỰC │
│                                                                    │          │
│  S1/S4 TRUYỆN: Dạ Khuyết ─► SỨ ĐỒ ≤65 ─► VĂN KIỆN ─► CHG 67·78·86·98 ─► 4 MẢNH │
│               ─► KHẢO HẠCH ─► THẦN ─► Phong Ấn | Hấp Thụ ─► TẦNG THẾ GIỚI II    │
│                                                                               │
│  MULTIPLAYER: boss HP × (1+0,75(n−1)) · boss thế giới cấm bay · 1 Thành Chủ   │
│  KỸ THUẬT: NeoForge 1.21.1 · GeckoLib · Attachments · SavedData · datapack    │
└───────────────────────────────────────────────────────────────────────────────┘
```

---

## 7. Sources & References
- Local: canon v2, S1–S7 v2.1, REVIEW-v1, research R1–R3.
- External (chính): [LoL Wiki – ARAM Mayhem Augments](https://wiki.leagueoflegends.com/en-us/ARAM_Mayhem/Augments) · [Fables.gg – Soul Rings](https://fables.gg/worlds/soul-land-fpclts81ic9f/lore/soul-rings) · [NeoForge docs – Attachments](https://docs.neoforged.net/docs/datastorage/attachments) · [NeoForge 26.1](https://neoforged.net/news/26.1release/) · nguồn đan dược/tuổi/tông môn ở cuối research/R3.

## Document Lineage
| Version | Document | Status |
|---------|----------|--------|
| v1 | DESIGN-v1-tu-tien-mod-master.md | Superseded |
| v2 | DESIGN-v2-tu-tien-mod-master.md (v2.1) | ✅ Current |

## Token Summary
```
Lượt 2: Haiku ×1 (~119K) · Sonnet ×10 lượt (~2,0M gồm resume sau giới hạn) · Opus (~90K)
Output: 7 phần v2.1 (~58K từ) + review + kế hoạch triển khai + tracker
```
