# PLAN-v1-tu-tien-mod-canon

**Created**: 2026-10-26
**Last Updated**: 2026-10-26 — Nhận 7 đề xuất đổi canon sau đối chiếu S1–S5 (xem §9)
**Status**: ⛔ Superseded by PLAN-v2-tu-tien-mod-canon.md (2026-10-26)
**Mục đích**: "Luật chung" (single source of truth) cho mọi agent viết các hệ thống. Agent viết KHÔNG được mâu thuẫn file này. Nếu cần thay đổi → ghi vào mục "Đề xuất đổi canon" cuối section, KHÔNG tự sửa.

Tên mod tạm: **Hư Không Tu Tiên Lục** (modid: `hukhong`) — quyết định cuối thuộc user.

---

## 0. Nguyên tắc thiết kế (Design Pillars)

1. **Đi lên có cảm giác "phá cảnh"**: mỗi 10 cấp là một cột mốc có nghi thức (về thành/tông môn chọn Lõi) chứ không chỉ +số.
2. **Thế giới bị đe dọa thật**: Hư Không xâm thực có hậu quả vĩnh viễn (thành mất → phế tích) → người chơi có lý do phòng thủ.
3. **Mỗi đạo chơi khác nhau về cơ chế, không chỉ khác số**: luật cấm (Đạo tu chỉ giết bằng phép, Thể tu không vũ khí) là bản sắc.
4. **Vanilla vẫn là nền**: quái vanilla giữ nguyên, chỉ thêm biến thể "Bị Hư Hóa"; kinh nghiệm từ quái thường rất ít.
5. **Data-driven**: Lõi, kỹ năng, quái, sự kiện định nghĩa bằng JSON (datapack) để cân bằng không cần build lại.

## 1. Cảnh giới (Realm) — cố định

| Cấp | Cảnh giới | Ghi chú |
|-----|-----------|---------|
| 1–9 | Thường Dân | Chưa có Lõi |
| 10–19 | Tu Hành Nhất Giả | Lõi #1 |
| 20–29 | Tu Hành Tông Sư | Lõi #2 |
| 30–39 | Tu Hành Đại Tông Sư | Lõi #3 |
| 40–49 | Tu Hành Vương Giả | Lõi #4 |
| 50–59 | **Tu Hành Hoàng Giả** *(user bỏ trống — tên tạm, cần user chốt)* | Lõi #5 |
| 60–69 | Tu Hành Đế Giả | Lõi #6 — Thể/Đạo tu mở bay |
| 70–79 | Tu Hành Thánh Giả | Lõi #7 |
| 80–89 | Tu Hành Đấu Giả | Lõi #8 |
| 90–95 | Tu Hành Đấu Kình I→V (90–91=I, 92=II, 93=III, 94=IV, 95=V) | Lõi #9 ở cấp 90 |
| 96–98 | Luyện Thần Giả | |
| 99 | Bán Thần | |
| 100 | Thần | Lõi #10 = **Thần Lõi**, chỉ sau Khảo Hạch Thăng Thần |

- Đạt cấp 10/20/…/90: thanh EXP **khóa** ở cấp đó cho đến khi tới **Ấn Hồn Đài** (trong thành/tông môn/thánh địa) chọn Lõi. Chưa chọn = không nhận EXP thêm (tích tối đa 1 cấp "dư").
- Lên 100 cần hoàn thành chuỗi **Khảo Hạch Thăng Thần** (3 thử thách: Tâm ma, Lôi kiếp, Hư Không Bản Nguyên).

## 2. Tài nguyên & chỉ số (thống nhất tên)

| Tên | Dùng cho | Ghi chú |
|-----|---------|---------|
| Linh Lực (Mana) | Kiếm tu, chung | thanh xanh lam |
| Thánh Lực | Đạo tu | thay Linh Lực, hồi nhanh khi đứng yên/thiền |
| Bá Khí | Thể tu | tích khi đánh/chịu đòn, giảm dần khi ngoài combat |
| Ô Uế Lực | Ma tu | tích khi giết, hồi máu = Hấp Huyết |
| Tâm Ma | Ma tu (và mọi đạo khi dùng đồ ma) | thanh rủi ro, đầy → mất kiểm soát tạm thời |

Chỉ số lõi (dùng attribute NeoForge): Sinh Lực (HP), Công (Attack), Phòng (Armor/Toughness), Thần Thức (Spell Power), Tốc, Bạo Kích (crit chance/dmg), Hấp Huyết (lifesteal %), Hồi Linh (regen resource).

## 3. Bốn đạo (Path) — luật cứng

| Đạo | Bản sắc | Luật cứng | Bay |
|-----|---------|-----------|-----|
| **Kiếm Tu** | Sát thương cao nhất, glass-cannon | Nhận **Bản Mệnh Kiếm** ngẫu nhiên khi nhập đạo (lớn lên theo chủ) | Ngự kiếm từ **cấp 20**, tốn Linh Lực/giây |
| **Đạo Tu** | Phép thuật thần thánh, AoE, hỗ trợ | Không cần ăn (hunger không giảm); **sát thương cận chiến/cung lên quái = 0**, chỉ phép mới giết | Từ **cấp 60** (Đế Giả), có **cánh thiên sứ** |
| **Thể Tu** | Công + thủ, đấu tay không | **Không dùng vũ khí** (cầm vũ khí → sát thương ×0, không dùng cung/khiên tấn công); hệ **Bá Khí** riêng | Từ **cấp 60** |
| **Ma Tu** | Cân bằng, sống bằng giết chóc | **Hấp Huyết nội tại**; giết mọi thứ (cả dân làng/người chơi) đều tăng trưởng; tích Tâm Ma; 4 nhánh: Ma Thuần / Ma Kiếm / Ma Thể / Ma Đạo (nhánh lai nhận luật nới lỏng của đạo gốc) | Ma Kiếm: cấp 20; còn lại cấp 60 |

## 4. Tư chất (Aptitude) & xuất thân

- Bậc: D, C, B, A, S, SS, SSS. Tỉ lệ đề xuất (mỗi đạo roll riêng): D 8% · C 24% · B 28% · A 22% · S 13% · SS 4% · SSS 1%.
- Số đạo được phép tu: Đơn năng 75% · Song năng 20% · Tam năng 5%.
- Phạt tu nhiều đạo (EXP cần ×): song ×1.6, tam ×2.5 (cân bằng: mỗi đạo vẫn có Lõi riêng nhưng chỉ chọn 1 Lõi/mốc chung).
- Tư chất ảnh hưởng: hệ số EXP (D ×0.6 … SSS ×1.6), độ hiếm Lõi khi roll, chỉ số khởi đầu.
- Từ **A trở lên** → 1 **Kỹ Năng Bản Mệnh** độc nhất; S/SS/SSS là phiên bản mạnh hơn (cùng skill, nâng tier).
- Màn hình tạo nhân vật: hiện bảng 4 đạo, mỗi đạo ghi tư chất tương ứng; khóa các đạo vượt số năng.
- Xuất thân (roll, ảnh hưởng điểm spawn + quà khởi đầu + quan hệ phe): Thành Thị, Thôn Làng, Đệ Tử Tông Môn, Hậu Duệ Thế Gia, Con Hoang (hoang dã, buff sinh tồn, không có quan hệ phe), Di Dân Phế Tích (từng sống trong thành bị Hư Không nuốt — kháng Hư Hóa nhẹ).

## 5. Lõi (Core) — tổng hợp Đấu La + ARAM Hỗn Loạn

- Mỗi Lõi có **Niên Hạn/Phẩm** theo màu Hồn Hoàn: Bạch (10 năm) → Hoàng (100) → Tử (1.000) → Hắc (10.000) → Hồng (100.000) → Kim (Thần Lõi, chỉ cấp 100).
- Ở Ấn Hồn Đài: hiện **3 Lõi**, cùng phẩm (như ARAM: cả màn chọn cùng tier). Phẩm được roll dựa trên: tư chất + **Hồn Tinh** mang theo (rơi từ quái/boss Hư Hóa, phẩm Hồn Tinh nâng tỉ lệ) + cảnh giới.
- Được **reroll** bằng vật phẩm "Thiên Cơ Phù" (giới hạn).
- **Hệ (Set)**: mỗi Lõi thuộc 1 hệ (vd: Liệt Diễm, Huyết Ma, Tinh Thần, Cương Thể, Phong Lôi, Thánh Quang, Hư Không Phản Phệ...). Giữ ≥2/≥4 Lõi cùng hệ → bonus set (như ARAM Mayhem).
- Mỗi Lõi cho: 1 bị động + (tùy) 1 kỹ năng chủ động gắn phím; Lõi **nâng cấp** (Tinh Luyện +1→+5) bằng Hồn Tinh + Đá Tẩy Luyện.
- **Hồn Cốt** (Spirit Bone, slot phụ 6 vị trí: đầu, thân, tay trái, tay phải, chân trái, chân phải): rơi hiếm từ boss, cho kỹ năng phụ — tương ứng "slot đặc biệt" của ARAM.

## 6. Thế giới & phe

- Thế giới: **Cửu Châu Linh Giới** (tên tạm). Kẻ thù chung: **Hư Không Xâm Thực** — thực thể từ ngoài thiên đạo, đồng hóa sinh linh & tu sĩ.
- **4 Chấp Hành Giả Hư Không** (mỗi người giữ 1 **Mảnh Thiên Môn**) → gom đủ 4 mảnh mở **Hư Không Bản Nguyên** (thay The End). Boss cuối thay Ender Dragon.
- Phe nhân loại: các Tông Môn (≥1 tông chính cho mỗi đạo + 1 Ma Môn), Thành Thị (Ấn Hồn Đài, chợ, nhiệm vụ), Thánh Địa (tu luyện tăng tốc).
- Quái: vanilla giữ nguyên + biến thể **Hư Hóa** (vanilla bị nhiễm) + **Hư Tu** (thân xác tu sĩ bị đồng hóa) + quái gốc Hư Không. EXP quái vanilla thường: ~1–2% EXP của quái Hư Hóa cùng cấp.
- Sự kiện: Bí Cảnh mở (có cảnh báo mức nguy hiểm ↔ phần thưởng), Boss thế giới/khu vực/tập kích, **Quân Đoàn Hư Không tập kích** thành/tông môn → thua = thành thành **Phế Tích Hư Hóa** (vĩnh viễn trừ khi tái chiếm — user chốt).

## 7. Nền tảng kỹ thuật (đề xuất mặc định — user chốt)

- **NeoForge 1.21.1** (hệ sinh thái mod lớn nhất, GeckoLib/Curios/các mod tham khảo đều có) — phương án B: NeoForge 26.1 (mới hơn, Java 25, ít thư viện hơn).
- Thư viện: GeckoLib (model/animation quái & cánh), Curios (slot Hồn Cốt/pháp bảo) — optional dependency.
- Lưu trạng thái người chơi: NeoForge Data Attachments; thế giới: SavedData (trạng thái thành, xâm thực); network: payload codecs.

## 8. Phân công multi-agent

| Vai | Model | Nhiệm vụ | Output |
|-----|-------|---------|--------|
| Planner | Opus | Canon (file này), ghép tài liệu tổng, câu hỏi cho user | PLAN-v1 + DESIGN-v1 |
| Researcher R1 | Haiku | Đấu La (Võ Hồn, Hồn Hoàn, Hồn Cốt, Phong Hào) + ARAM Mayhem (augment, set) + tropes tu tiên | research/R1-*.md |
| Researcher R2 | Haiku | Kỹ thuật NeoForge + mod tham khảo (Iron's Spells, Epic Fight, mod tu tiên hiện có) | research/R2-*.md |
| Writer W1 | Sonnet | Cốt truyện, phe, 4 Chấp Hành Giả, chuỗi nhiệm vụ chính | sections/S1-lore.md |
| Writer W2 | Sonnet | 4 đạo + chỉ số + tư chất + xuất thân + Bản Mệnh skill + pháp bảo | sections/S2-paths.md |
| Writer W3 | Sonnet | Cảnh giới, EXP curve, Lõi, Hồn Cốt, kỹ năng | sections/S3-cores.md |
| Writer W4 | Sonnet | Quái/Boss, Hư Không xâm thực, Map/worldgen, Bí Cảnh, sự kiện, cơ duyên | sections/S4-world.md |
| Writer W5 | Sonnet | Kiến trúc kỹ thuật + roadmap MVP | sections/S5-tech.md |

## 9. Thay đổi canon đã nhận (2026-10-26, sau đối chiếu S1–S5)

- §5: Bonus Hệ là **2/4/6 món**; phẩm Lõi roll theo **Điểm Vận** (tư chất + Hồn Tinh + cảnh giới); Thần Lõi tính +1 cho bất kỳ Hệ.
- §4: Hệ số EXP tư chất: D 0.6 · C 0.8 · B 1.0 · A 1.15 · S 1.3 · SS 1.45 · SSS 1.6.
- §1: Thử thách 3 của Khảo Hạch = trận Chủ Hư Không (boss cuối).
- §6: Hư Không Bản Nguyên khuyến nghị cấp ≥80 (không cưỡng chế). Đường cong EXP chuẩn = S3 (`E(L)=40·L^2.15`).
- §7: Ấn Hồn Đài dùng `MenuType` (server xác thực). Cờ cốt truyện lưu trong attachment: `manh_thien_mon`, `ket_thuc_da_mo`.
- Chờ user: đạo chủ/phụ, chi phí bay Thể tu, mất thành, Curios — xem DESIGN-v1 §4.

## Document Lineage

| Version | Document | Status |
|---------|----------|--------|
| v1 | PLAN-v1-tu-tien-mod-canon.md | ✅ Current |
