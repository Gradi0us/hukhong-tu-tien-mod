# DIALOG — Dạ Khuyết (boss kiểu Malenia) — khoá thoại

**Last Updated**: 2026-11-10 — tạo danh sách khoá thoại placeholder cho bản làm lại Dạ Khuyết (trăng/máu)
**Status**: Placeholder (vi_vn + en_us ngắn) — chờ viết lại lời thoại

Mọi câu nằm trong `src/main/resources/assets/hukhong/lang/{vi_vn,en_us}.json`. Hiện dưới dạng phụ đề cutscene
(`BossPhaseConfig.SubLine`), mỗi câu có khung tick [from, to) ghi bên dưới — giữ câu đủ ngắn để đọc kịp trong khung đó.

## Cutscene & khoá

| Khoá | Cảnh | Khung (tick) | Bối cảnh |
|------|------|--------------|----------|
| `hukhong.dialog.dakhuyet.intro.1` | Giới thiệu | 36–72 / 110 | Lần đầu người chơi tới gần (40 block). Camera từ sau lưng nhìn qua vai lên trăng, vòng ra trước mặt. |
| `hukhong.dialog.dakhuyet.intro.2` | Giới thiệu | 72–106 | Cận mặt. Gợi ý cơ chế hút máu (đòn trúng làm hắn hồi máu). |
| `hukhong.dialog.dakhuyet.p2.1` | Chuyển pha 2 (HP < 66%) | 8–44 / 120 | Quỳ gối, bất tử. Ngạc nhiên vì người chơi còn đứng. |
| `hukhong.dialog.dakhuyet.p2.2` | Chuyển pha 2 | 44–84 | Lộ diện (tick 60): cánh trăng khuyết bung ra sau lưng (xương `wing` + `moon_wings`, texture _p2). |
| `hukhong.dialog.dakhuyet.p2.3` | Chuyển pha 2 | 84–116 | Kéo camera ra xa; hắn tuyên bố không còn là "kẻ phiên dịch". |
| `hukhong.dialog.dakhuyet.p3.1` | Chuyển pha 3 (HP < 33%) | 8–44 / 120 | Trăng hoá máu, trời mưa máu. |
| `hukhong.dialog.dakhuyet.p3.2` | Chuyển pha 3 | 44–84 | Lộ diện (tick 70): hoa trăng máu nở quanh thân (bloom), camera từ dưới đất nhìn lên. |
| `hukhong.dialog.dakhuyet.p3.3` | Chuyển pha 3 | 84–116 | Lời mời "nở cùng ta" — mở màn giai đoạn cuối. |
| `hukhong.dialog.dakhuyet.ult.1` | Tối thượng Huyết Nguyệt Khai Hoa | 2–28 / 60 | Bay lên với cánh trăng máu; camera thấp nhìn lên. |
| `hukhong.dialog.dakhuyet.ult.2` | Tối thượng | 28–58 | Hô tên chiêu ngay trước khi lao xuống nổ hoa. |
| `hukhong.dialog.dakhuyet.death.1` | Tử trận | 6–46 / 90 | Ngã xuống, cánh tan thành cánh hoa. Cutscene tĩnh tại chỗ ngã. |
| `hukhong.dialog.dakhuyet.death.2` | Tử trận | 46–86 | Lời trăng trối; camera kéo lên cao. |

Ghi chú: phụ đề đầu cutscene giới thiệu (6–36) dùng tên thực thể `entity.hukhong.da_khuyet`.
Thoại đồng minh (trận Nguyên Thủy Tịch Diệt) vẫn dùng khoá cũ `hukhong.endgame.dakhuyet.ally.*` — không đổi.

## Tên chiêu (hiện ở action bar khi thi triển)

| Khoá | vi_vn (tạm) | Chiêu |
|------|-------------|-------|
| `hukhong.boss.move.dk_thuy_dieu` | Thủy Điểu Vũ | Waterfowl: bay lên, 3 đợt lướt chém |
| `hukhong.boss.move.dk_thuy_dieu_huyet` | Huyết Thủy Điểu Vũ | Waterfowl máu (pha 3), để lại vũng máu |
| `hukhong.boss.move.dk_dam_xuyen` | Nguyệt Thương Xuyên | Đâm lao |
| `hukhong.boss.move.dk_lien_tram` | Khuyết Nguyệt Liên Trảm | Chém liên hoàn, nhát cuối trễ |
| `hukhong.boss.move.dk_lao_khong` | Lạc Nguyệt Kích | Lao từ trên không |
| `hukhong.boss.move.dk_song_lao` | Song Huyết Lạc Nguyệt | Lao 2 lần (pha 3) |
| `hukhong.boss.move.dk_nguyet_luan` | Tam Nguyệt Luân | 3 lưỡi trăng hình quạt |
| `hukhong.boss.move.dk_dem_khuyet` | Đêm Khuyết | Mù + yếu quanh thân (pha 1) |
| `hukhong.boss.move.dk_huyet_hoa` | Huyết Nguyệt Khai Hoa | Tối thượng (Scarlet Aeonia) |

## Document Lineage

| Version | Document | Status |
|---------|----------|--------|
| v1 | docs/DIALOG-dakhuyet-keys.md (this file) | ✅ Current |
