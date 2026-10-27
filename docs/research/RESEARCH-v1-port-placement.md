# RESEARCH-v1: Đặt Cảng trên biển
**Last Updated**: 2026-10-27 — Cảng tìm bờ biển bằng xoắn ốc 2000 block + dự phòng bioma đại dương. Code: `city/PortSites.java`, `city/Prebuild.java`.

## Vanilla kiểm tra biển thế nào
(WebSearch chỉ ra trang wiki gameplay, không có chi tiết kỹ thuật; phần dưới dựa trên mã vanilla 1.21.1 đã biết, chưa đối chiếu nguồn web.)
- Structure biển (shipwreck, ocean_ruin, monument) giới hạn bằng **tag bioma** `#minecraft:has_structure/*` (gồm `#minecraft:is_ocean`) trong `structure.biomes`, kiểm trên bioma ở điểm mẫu của chunk.
- Độ cao đáy biển: `Heightmap.Types.OCEAN_FLOOR_WG` (bỏ qua nước, chỉ khối rắn). Mặt nước/mặt đất: `WORLD_SURFACE_WG` (tính cả nước, ở biển = mực biển). Khác biệt hai giá trị = độ sâu nước.
- Các giá trị `*_WG` lấy từ `ChunkGenerator.getBaseHeight(x, z, type, level, randomState)`: **không cần sinh chunk**, nên dò được vùng chưa nạp.
- Bioma tại điểm chưa sinh: `Level.getBiome` tự rơi về `getUncachedNoiseBiome` (nguồn bioma + sampler), cũng không sinh chunk.

## Áp dụng cho Cảng
1. Schematic cảng (`seaport.schem`) tự chứa lớp nước ở tầng 0 (mặt nước = tầng 0). Chọn **cạnh nước** = cạnh (dải ngoài 16 ô) có nhiều nước nhất ở 3 tầng thấp (ở file hiện tại: Tây). Đặt gốc cấu trúc ở `seaLevel - 1` để mặt nước khớp mặt biển (không dùng `surfaceY` như thành).
2. Neo tất định (như thành) -> **xoắn ốc** bước 48 ô, bán kính tối đa 2000: mỗi ứng viên đạt khi
   - điểm cách tâm `sx/2 + 70` ngoài cạnh nước có bioma `#minecraft:is_ocean`;
   - lưới 9x9 (bước 16 = 144x144) quanh điểm đó >= 80% `OCEAN_FLOOR_WG < seaLevel - 1` (biển lớn, không phải ao/sông);
   - 9 mẫu `WORLD_SURFACE_WG` trong khung cấu trúc: >= 3 mẫu là đất (> mực biển) và không mẫu nào cao hơn mực biển + 24 (tránh chôn vào núi).
3. Dò chia nhỏ, 8 ms/tick (`PortSites.step`), không chặn tick. Kết quả lưu vào `WorldBuildData` (không dò lại sau khi khởi động).
4. Không thấy bờ biển trong 2000: `level.findClosestBiome3d(is_ocean, neo, 6400, 32, 64)`; dịch cấu trúc để cạnh nước nằm ngay điểm bioma tìm được (nền sẽ được đổ đá xuống đáy).

## Hạn chế
- Chưa xoay cấu trúc; chỉ tìm bờ biển hợp với hướng cạnh nước sẵn có của schematic.
- Cần chạy trên world thật để xác nhận cảng thực sự nằm trên biển (smoke server chỉ chứng minh không lỗi).
