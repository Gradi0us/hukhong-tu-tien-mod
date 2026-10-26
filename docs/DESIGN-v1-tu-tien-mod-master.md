# DESIGN-v1-tu-tien-mod-master

**Created**: 2026-10-26
**Last Updated**: 2026-10-26 — Bản thiết kế v1 hoàn chỉnh 5 hệ thống (multi-agent: Haiku search → Sonnet viết → Opus lập kế hoạch + đối chiếu)
**Research Target**: Mod Minecraft tu tiên theo đạo (Kiếm/Đạo/Thể/Ma), cảnh giới 1→100, Lõi (Đấu La + ARAM Hỗn Loạn), Hư Không Xâm Thực
**Mode**: Planning · **Level**: L3-Analyze · **Type**: DESIGN · **Language**: vi
**Context**: Yêu cầu của user (chat) + R1/R2 research
**Status**: ⛔ Superseded by DESIGN-v2-tu-tien-mod-master.md (2026-10-26)
**Researcher**: Claude Opus (planner) + Sonnet ×5 (writer) + Haiku ×2 (search)

---

## Key Findings

| # | Finding | Chi tiết | Bằng chứng | Độ tin cậy |
|---|---------|----------|------------|------------|
| 1 | **Nền tảng: NeoForge 1.21.1** | Hệ sinh thái thư viện lớn nhất (GeckoLib, Curios, mod tham khảo). NeoForge 26.x đã có nhưng đòi Java 25, ít addon | [R2](research/R2-tech-neoforge.md), neoforged.net | TRUNG BÌNH (chưa xác minh trạng thái stable 26.x) |
| 2 | **Lõi = Hồn Hoàn × Augment** | Phẩm theo màu Hồn Hoàn (Bạch→Hồng→Kim); mỗi lần chọn 3 Lõi cùng phẩm như ARAM; Hệ (set) bonus 2/4/6 | [S3](sections/S3-cores.md), wiki LoL ARAM Mayhem, fables.gg Soul Land | CAO (cơ chế gốc đã xác minh) |
| 3 | **Tổng thời gian lên Thần ≈ 110 giờ** (tư chất B) | `E(L)=40·L^2.15`; D ≈ 183 giờ, SSS ≈ 69 giờ | [S3 §1](sections/S3-cores.md) — ước lượng Fermi | THẤP (chưa playtest) |
| 4 | **Luật cứng của từng đạo đều làm được bằng event NeoForge** | Đạo tu không đói + đánh thường 0 dmg; Thể tu cầm vũ khí 0 dmg; Ma tu hấp huyết; bay tốn tài nguyên | [S5 §3](sections/S5-tech.md) có code mẫu | TRUNG BÌNH (tên API có đánh dấu [KIỂM TRA]) |
| 5 | **Cốt truyện có twist + 4 Chấp Hành Giả = 4 đạo sa ngã** | Thạch Hống (Thể), Lăng Uyên (Kiếm), Bạch Từ Âm (Đạo), Cơ Vô Tận (Ma); boss cuối Nguyên Thủy Tịch Diệt; 2–3 kết thúc | [S1](sections/S1-lore.md) | — (sáng tác) |
| 6 | **Mất thành nên "phế tích tái chiếm được"** thay vì mất vĩnh viễn tuyệt đối | Giữ được hậu quả mà không khóa tiến trình; hardcore để thành gamerule | [S4 §5.3–5.5](sections/S4-world.md) | — (đề xuất thiết kế) |
| 7 | **MVP phải nhỏ** | 2 đạo (Đạo + Thể), cấp 1–30, 1 thành, Ấn Hồn Đài, 10 Lõi, 3 quái Hư Hóa ≈ 8–10 người-tuần (có kinh nghiệm) / 16–24 (mới) | [S5 §6](sections/S5-tech.md) | THẤP (ước lượng thô) |

---

## Table of Contents
1. [Bản đồ tài liệu](#1-bản-đồ-tài-liệu)
2. [Tổng quan kiến trúc gameplay](#2-tổng-quan-kiến-trúc-gameplay)
3. [Đối chiếu nhất quán & thay đổi canon](#3-đối-chiếu-nhất-quán--thay-đổi-canon)
4. [12 quyết định cần user chốt](#4-12-quyết-định-cần-user-chốt)
5. [Lộ trình tiếp theo](#5-lộ-trình-tiếp-theo)
6. [Kết luận + sơ đồ tổng hợp](#6-kết-luận--sơ-đồ-tổng-hợp)
7. [Sources, Lineage, Token Summary](#7-sources--references)

---

## 1. Bản đồ tài liệu

| File | Nội dung | Số từ |
|------|---------|------|
| [PLAN-v1-tu-tien-mod-canon.md](PLAN-v1-tu-tien-mod-canon.md) | **Luật chung** — cảnh giới, tài nguyên, luật cứng 4 đạo, tư chất, Lõi, phe | ~1.500 |
| [sections/S1-lore.md](sections/S1-lore.md) | Thế giới Cửu Châu Linh Giới, Hư Không, 4 Chấp Hành Giả, 5 tông, 5 thành, cốt truyện theo Hồi, kết thúc, mở đầu theo xuất thân | ~3.200 |
| [sections/S2-paths.md](sections/S2-paths.md) | Chỉ số 4 đạo + radar, tài nguyên, kỹ năng nền, bay, Bản Mệnh Kiếm, Bá Khí, cánh thiên sứ, 4 nhánh Ma tu, tư chất D→SSS, UI tạo nhân vật, Kỹ Năng Bản Mệnh, xuất thân, pháp bảo | ~5.300 |
| [sections/S3-cores.md](sections/S3-cores.md) | Đường cong EXP, Ấn Hồn Đài, xác suất phẩm (Điểm Vận), 8 Hệ, 32 Lõi, Tinh Luyện, 13 Hồn Cốt, phím kỹ năng, Khảo Hạch Thăng Thần, phạt chết | ~5.800 |
| [sections/S4-world.md](sections/S4-world.md) | Worldgen 5 vòng, Vùng Xâm Thực lan, Hư Không Bản Nguyên, Bí Cảnh, 25 quái, 5 tầng boss, tập kích thành, nhiệm vụ, cơ duyên | ~6.100 |
| [sections/S5-tech.md](sections/S5-tech.md) | Chọn nền tảng, kiến trúc module, code mẫu, schema JSON, cấu trúc project, roadmap P0→P3, rủi ro | ~3.300 |
| [research/R1-lore-mechanics.md](research/R1-lore-mechanics.md) · [research/R2-tech-neoforge.md](research/R2-tech-neoforge.md) | Ghi chú search (Haiku) | ~1.800 |

**Đọc theo thứ tự**: file này → canon → S2 (đạo) → S3 (Lõi) → S1 (truyện) → S4 (thế giới) → S5 (kỹ thuật).

---

## 2. Tổng quan kiến trúc gameplay

```
VÒNG LẶP CHÍNH (core loop)
═══════════════════════════════════════════════════════════════════════

 [Tạo nhân vật]                                         
  roll Xuất thân + Tư chất D→SSS từng đạo + Song/Tam năng
        │
        ▼
 ┌─────────────┐  giết quái Hư Hóa / nhiệm vụ / bí cảnh / thiền   ┌──────────────┐
 │  TÍCH EXP   │ ───────────────────────────────────────────────► │ Mốc 10 cấp:  │
 │ (vanilla ≈0)│                                                  │ EXP bị KHÓA  │
 └─────▲───────┘                                                  └──────┬───────┘
       │                                                                 │ về Thành/Tông/Thánh Địa
       │                                                                 ▼
       │                                                   ┌───────────────────────────┐
       │        sức mạnh mới → vào vùng nguy hiểm hơn       │ ẤN HỒN ĐÀI: chọn 1/3 Lõi   │
       └─────────────────────────────────────────────────── │ cùng phẩm (Bạch…Hồng)      │
                                                           │ Hồn Tinh ↑ phẩm · Reroll   │
                                                           └───────────────────────────┘
ÁP LỰC THẾ GIỚI (chạy song song)
  Vùng Xâm Thực lan ──► Quân Đoàn tập kích thành ──► thua = Phế Tích (tái chiếm được)
        ▲                                                   │
        └──── người chơi diệt boss / thanh tẩy ◄────────────┘

TUYẾN CHÍNH
  Thường Dân → … → hạ 4 Chấp Hành Giả (4 Mảnh Thiên Môn) → Hư Không Bản Nguyên
  → Khảo Hạch Thăng Thần (Tâm Ma · Lôi Kiếp · trận Chủ Hư Không) → Thần (cấp 100)
```

**Bốn đạo trong 1 bảng** (chi tiết: [S2](sections/S2-paths.md)):

| Đạo | Vai trò | Tài nguyên | Luật cứng | Bay |
|-----|--------|-----------|-----------|-----|
| Kiếm Tu | Sát thương cao nhất, máu giấy | Linh Lực | Bản Mệnh Kiếm random (≥8 loại), lớn theo chủ | Cấp 20, 4 LL/s |
| Đạo Tu | Phép AoE, hồi máu, hỗ trợ | Thánh Lực | Không đói; đánh thường = 0 dmg lên quái | Cấp 60, cánh 1→2→3 đôi → 6 cánh |
| Thể Tu | Tank + đấm | Bá Khí | Không dùng vũ khí; Bá Khí áp chế quái yếu | Cấp 60, tốn Bá Khí + đói |
| Ma Tu | Cân bằng, hút máu | Ô Uế Lực + Tâm Ma | Hấp Huyết; giết càng nhiều càng mạnh (Huyết Tế); 4 nhánh Thuần/Kiếm/Thể/Đạo; bị truy nã | Ma Kiếm cấp 20, còn lại cấp 60 |

---

## 3. Đối chiếu nhất quán & thay đổi canon

### 3.1 Chỗ lệch giữa các phần (đã xử lý)

| # | Lệch | Giữa | Xử lý (Opus) |
|---|------|------|--------------|
| C1 | Mốc EXP/cấp: S4 giả định cấp 10 ≈ 1.000 EXP; công thức S3 cho ≈ 5.650 | S3 ↔ S4 | **Lấy S3 làm chuẩn.** EXP quái S4 tính lại bằng công thức S4 đã ghi sẵn: `EXP_quái = 0.08 × EXP_cần_cấp_vùng ÷ 12` (cần làm khi viết datapack) |
| C2 | EXP quái vanilla cấp 1–9: S3 nhân ×20 để khởi động; S4 ghi cố định 1–2 EXP | S3 ↔ S4 | **Lấy S3**: ×20 ở cấp 1–9, giảm dần còn 1,5% ở cấp 10 |
| C3 | Thử thách 3 của Khảo Hạch trùng boss cuối | S1 ↔ S3 | **Gộp**: thử thách 3 = trận Chủ Hư Không (bớt 1 arena) |
| C4 | Ma Kiếm bay dùng Linh Lực trong khi Ma tu dùng Ô Uế Lực | S2 nội bộ | Giữ (Ma Kiếm có 2 tài nguyên song song) — ghi chú để test |

### 3.2 Đề xuất đổi canon — trạng thái

| Đề xuất | Từ | Trạng thái |
|---------|----|-----------|
| Bonus Hệ 2/4/6 món | S3 | ✅ Đã nhận vào canon |
| "Điểm Vận" (tư chất + Hồn Tinh) là cơ chế roll phẩm | S3 | ✅ Đã nhận |
| Hệ số EXP tư chất D0.6 · C0.8 · B1.0 · A1.15 · S1.3 · SS1.45 · SSS1.6 | S3, S2 | ✅ Đã nhận |
| Cờ cốt truyện `manh_thien_mon` / `ket_thuc_da_mo` | S1 | ✅ Đã nhận (kỹ thuật) |
| Ấn Hồn Đài dùng `MenuType` (server xác thực) | S5 | ✅ Đã nhận (kỹ thuật) |
| Thần Lõi tính +1 cho bất kỳ Hệ | S3 | ✅ Đã nhận |
| Hư Không Bản Nguyên: khuyến nghị cấp ≥80, không cưỡng chế | S4 | ✅ Đã nhận |
| Đạo chủ/đạo phụ 40%, đổi đạo hồi 5 phút | S2 | ⏳ User chốt (Q4) |
| Thể tu bay tốn Bá Khí + đói | S2 | ⏳ User chốt (Q6) |
| Mất thành → phế tích tái chiếm, giữ kho riêng | S4 | ⏳ User chốt (Q7) |
| Curios chỉ từ P2 | S5 | ⏳ User chốt (Q3) |

---

## 4. 12 quyết định cần user chốt

Gộp từ 25 câu hỏi của 5 phần, chỉ giữ câu **thay đổi thiết kế**. Có ★ = khuyến nghị. Không trả lời = dùng khuyến nghị.

| # | Quyết định | Lựa chọn | ★ Khuyến nghị & lý do |
|---|-----------|----------|----------------------|
| **Q1** | Phiên bản Minecraft | A) NeoForge 1.21.1 · B) NeoForge 26.x | **A** — thư viện đủ, nhiều mod tham khảo; có thể port sau |
| **Q2** | Chơi chủ yếu | A) Singleplayer · B) Server nhiều người | **A** cho MVP; B kéo theo luật PvP, sync, hiệu năng |
| **Q3** | Ai viết code | A) Bạn tự code · B) Agent viết, bạn chạy thử & báo lỗi · C) Kết hợp | **C** — agent viết khung + JSON, bạn chỉnh cân bằng/UI |
| **Q4** | Song/Tam năng chơi thế nào | A) 1 đạo hoạt động/lúc, đạo phụ 40%, đổi hồi 5 phút · B) Dùng cả 2 cùng lúc | **A** — B gần như không cân bằng được |
| **Q5** | Tên cảnh giới 50–59 (bạn để trống) | A) "Tu Hành Hoàng Giả" · B) Bạn đặt tên | Bạn chốt |
| **Q6** | Thể tu bay tốn gì | A) Bá Khí + đói · B) Chỉ Bá Khí | **A** — Thể tu cần điểm yếu |
| **Q7** | Thành bị Hư Không chiếm | A) Mất vĩnh viễn · B) Phế tích, tái chiếm có chi phí (kho riêng giữ lại) | **B**, còn A là tùy chọn hardcore |
| **Q8** | Tần suất tập kích | A) Cố định 7 ngày game · B) Theo mức Xâm Thực, có trần | **B** |
| **Q9** | Phạt khi chết | A) Mất ít EXP · B) "Căn Cơ Tổn Thương" (debuff tạm, không rớt cấp) · C) Hardcore | **B** |
| **Q10** | Lõi đã chọn có đổi được không | A) Vĩnh viễn, chỉ "Tẩy Hồn" tốn kém · B) Đổi tự do | **A** — mỗi Lõi là quyết định quan trọng |
| **Q11** | Ô "Hỗn Loạn" kiểu ARAM trong 3 lựa chọn Lõi | A) Giữ (vui, bất ngờ) · B) Bỏ, 3 Lõi đều hợp đạo | **A** |
| **Q12** | Hư Không có twist (Chủ Hư Không từng là Thần) | A) Có twist, 2–3 kết thúc · B) Phản diện ngoại lai đơn giản | **A** — tốn ít công mà truyện hay hơn nhiều |

Câu hỏi phụ (giữ nguyên khuyến nghị nếu bạn không nói gì): thứ tự 4 Chấp Hành Giả cố định cho MVP; Đạo tu đánh người chơi ×0.3; Bản Mệnh Kiếm không rơi khi chết; Ma tu giết người chơi chỉ khi bật PvP; ~110 giờ chơi; 4 + 1 phím kỹ năng; Bí Cảnh là dimension tạm; boss có chiêu khóa bay; thế giới không giới hạn (singleplayer).

---

## 5. Lộ trình tiếp theo

| Phase | Phạm vi | Xong khi (test được trong game) |
|-------|---------|-------------------------------|
| **P0 — MVP** | Đạo tu + Thể tu; cấp 1–30; tạo nhân vật (tư chất); 1 thành có Ấn Hồn Đài; 10 Lõi; 3 quái Hư Hóa; HUD tài nguyên | Tạo nhân vật → lên cấp 10 → khóa EXP → chọn Lõi ở thành → dùng được kỹ năng; Đạo tu đánh thường = 0 dmg; Thể tu cầm kiếm = 0 dmg |
| P1 | + Kiếm tu, Ma tu; cấp 1–60; bay; 3 tông môn; Bí Cảnh; tập kích thành | Thành có thể thất thủ và thành phế tích |
| P2 | Cấp 1–90; Hồn Cốt; 4 Chấp Hành Giả; Curios | Gom 4 Mảnh Thiên Môn |
| P3 | Hư Không Bản Nguyên, Khảo Hạch Thăng Thần, kết thúc | Lên Thần, xem được kết thúc |

Chi tiết từng mốc: [S5 §6](sections/S5-tech.md).

---

## 6. Kết luận + sơ đồ tổng hợp

Thiết kế đã đủ để bắt đầu code P0. Ba quyết định chặn việc code là **Q1 (phiên bản), Q2 (singleplayer/server), Q3 (ai code)**. Các câu còn lại có thể chốt dần trong lúc làm P0.

```
┌──────────────────────── HƯ KHÔNG TU TIÊN LỤC — MỐI LIÊN KẾT HỆ THỐNG ────────────────────────┐
│                                                                                              │
│  XUẤT THÂN ─► spawn + quà     TƯ CHẤT D→SSS ─► hệ số EXP · Điểm Vận · Bản Mệnh (A+)            │
│        │                              │                                                      │
│        ▼                              ▼                                                      │
│  ┌──────────── 4 ĐẠO ────────────┐   CẢNH GIỚI 1→100 (khóa EXP mỗi 10 cấp)                    │
│  │ Kiếm  Đạo  Thể  Ma(4 nhánh)   │ ──────────────┬────────────────────────────┐               │
│  │ LL    TL   BK   ÔUL+Tâm Ma    │               ▼                            ▼               │
│  └───────────────┬───────────────┘   ẤN HỒN ĐÀI (thành/tông/thánh địa)   KHẢO HẠCH THĂNG THẦN │
│                  │                   chọn 1/3 LÕI ◄─ Hồn Tinh            Tâm Ma · Lôi Kiếp ·   │
│                  │                   Hệ 2/4/6 · Tinh Luyện +5            Chủ Hư Không ─► THẦN  │
│                  ▼                          ▲                                    ▲           │
│        KỸ NĂNG Z/X/C/V + R  ◄── Lõi + Hồn Cốt (6 slot, rơi từ boss)              │           │
│                  │                          ▲                                    │           │
│                  ▼                          │                                    │           │
│  THẾ GIỚI: 5 vòng độ khó ─► quái Hư Hóa / Hư Tu / gốc Hư Không ─► BOSS 5 tầng ───┤           │
│        │                                                     │                   │           │
│        ▼                                                     ▼                   │           │
│  VÙNG XÂM THỰC lan ─► TẬP KÍCH THÀNH ─► Phế Tích ⇄ tái chiếm   4 CHẤP HÀNH GIẢ ─► 4 MẢNH ─┘    │
│                                                     (Thể·Kiếm·Đạo·Ma sa ngã)                  │
│                                                                                              │
│  KỸ THUẬT: NeoForge 1.21.1 · Data Attachments · datapack JSON · GeckoLib · SavedData          │
│  LỘ TRÌNH: P0 MVP (Đạo+Thể, cấp 1–30) → P1 → P2 → P3 (Thần + kết thúc)                        │
└──────────────────────────────────────────────────────────────────────────────────────────────┘
```

---

## 7. Sources & References

### Source 1: Local
| File | Relevance |
|------|-----------|
| docs/PLAN-v1-tu-tien-mod-canon.md | Canon (Opus) |
| docs/sections/S1…S5 | 5 phần thiết kế (Sonnet) |
| docs/research/R1, R2 | Ghi chú search (Haiku) |

### Source 3: External
| # | Source | URL | Relevance |
|---|--------|-----|-----------|
| 1 | LoL Wiki — ARAM: Mayhem Augments | https://wiki.leagueoflegends.com/en-us/ARAM_Mayhem/Augments | Phẩm Silver/Gold/Prismatic, 3 lựa chọn/màn |
| 2 | LoL Wiki — ARAM: Mayhem | https://wiki.leagueoflegends.com/en-us/ARAM:_Mayhem | Mốc chọn augment, slot đặc biệt |
| 3 | LoLTheory — ARAM Mayhem guide | https://blog.loltheory.gg/aram-mayhem/ | Set bonus |
| 4 | Fables.gg — Soul Land soul rings | https://fables.gg/worlds/soul-land-fpclts81ic9f/lore/soul-rings | Màu/niên hạn Hồn Hoàn |
| 5 | Fables.gg — Spirit bone | https://fables.gg/worlds/soul-land-fpclts81ic9f/lore/spirit-bone | Hồn Cốt (chưa xác minh hết) |
| 6 | NeoForge — 26.1 release | https://neoforged.net/news/26.1release/ | Trạng thái 26.x |
| 7 | NeoForge docs — Data Attachments | https://docs.neoforged.net/docs/datastorage/attachments | Lưu dữ liệu người chơi |
| 8 | NeoForge docs — Payloads | https://docs.neoforged.net/docs/networking/payload | Đồng bộ mạng |
| 9 | Immortal Cultivation (NeoForge 1.21.1) | https://www.mado-hosting.com/minecraft-mods/immortal-cultivation-mod | Mod tu tiên tham khảo |

Danh sách đầy đủ ở cuối R1/R2.

## Document Lineage

| Version | Document | Focus | Status |
|---------|----------|-------|--------|
| v1 | PLAN-v1-tu-tien-mod-canon.md | Luật chung + phân công | ✅ Current |
| v1 | DESIGN-v1-tu-tien-mod-master.md (file này) | Tổng hợp + quyết định | ✅ Current |

## Related Documents
**Suggested next**: `SPEC-v1-tu-tien-mod-p0-mvp.md` (đặc tả P0 sau khi chốt Q1–Q3) → `TASK-v1-tu-tien-mod-journey.md` (theo dõi tiến độ code).

## Token Summary
```
Agents:   Haiku ×2 (~226K tokens) · Sonnet ×5 (~657K tokens) · Opus (~60K)
Output:   ~27K từ thiết kế (7 file) + file tổng này
Web:      ~15 lượt search/fetch
```
