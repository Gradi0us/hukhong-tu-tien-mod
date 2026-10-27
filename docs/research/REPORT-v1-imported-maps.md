# REPORT-v1: Bản đồ nhập (import maps)
**Last Updated**: 2026-10-27 — nhập 6 map cá nhân, thêm thể loại `trade` (Thương Hội). Dữ liệu map ngoài git, chỉ dùng cá nhân.

| Map | Thể loại | File | Kích thước | Dùng cho |
|---|---|---|---|---|
| Castillo | city_large | castillo.schem | 256×109×215 | thành city_large |
| French Paradise | city_large | french_paradise.schem | 256×121×256 | thành city_large |
| Dalia | city_mountain | dalia.schem | 227×52×227 | Hắc Thạch Quan, Thanh Vân, Bắc Hàn |
| Apocalypse City | city_ruined | apocalypse.schem | 256×192×256 | Huyết Nguyệt Phường, Thiên Khai Hoang Đô |
| Seaport | port | seaport.schem | 256×44×186 | Lưu Ly Cảng, Đan Hà Thành |
| Spawn Lobby V2 | trade | lobby_market.schem | 191×170×251 | chợ cạnh Lưu Ly Cảng, Cổ Tiên, Đan Hà |

File nằm ở `run/config/hukhong/structures/` (client) và `run/server/config/...` (server).
Vấn đề còn lại: NPE bí khắc ở biên chunk (đã bắt), dựng 256×256 mất 12–25s gây lag 3–9s (chưa tiền nạp chunk), marker tự đề xuất cần xem lại.
Lệnh thử: `/hukhong structure place <category> <name> [x z]`, `/hukhong structure city <id> <x> <z>`.
