# GUIDE-v1: Nhập công trình (thành, tông môn, cảng...) từ file

**Last Updated**: 2026-10-27 — hệ thống import structures (package `com.hukhong.structures`)

## 1. Tổng quan

Mỗi thành (`CityDef`) và tông (`SectDef`) có một **thể loại**. Khi sinh, mod tìm file theo thể loại:

1. Thư mục người dùng: `<thư mục game>/config/hukhong/structures/<thể loại>/` (ưu tiên, mod tự tạo thư mục rỗng ở lần sinh đầu).
2. Trong mod: `data/hukhong/structure/<thể loại>/` (hiện không đóng gói file nào).
3. Không có file hợp lệ: dùng builder thủ tục (tường + tháp + cổng, nhà mái dốc, cảng có tàu, tầng núi, hoang phế).

Nếu có nhiều file, chọn tất định theo seed thế giới + id thành. File lỗi sẽ bị bỏ qua (xem log) và thử file kế.

| Thể loại | Dùng cho |
|----------|----------|
| `port` | Lưu Ly Cảng, Đan Hà Thành |
| `city_mountain` | Hắc Thạch Quan, Thanh Vân Thành, Bắc Hàn Thành |
| `city_ruined` | Huyết Nguyệt Phường, Thiên Khai Hoang Đô |
| `city_large` | các thành còn lại |
| `sect` | cả 7 tông môn |
| `village`, `ruin`, `tower` | đã hỗ trợ nạp file, chưa gán cho thực thể nào |

## 2. Lấy map (bản Java)

- Tải map Java từ nguồn bạn có quyền dùng (Planet Minecraft, v.v.), mở trong Minecraft Java cùng phiên bản (1.21.1), hoặc dùng world mới.
- Map **Bedrock** (.mcworld/.mcpack) không đọc trực tiếp: chuyển sang Java bằng Chunker hoặc Amulet trước.
- Bản quyền: mod **không** đóng gói map của bên thứ ba. File bạn tự đặt vào `config/` chỉ dùng cá nhân; không phát hành lại nếu tác giả không cho phép.

## 3. Xuất vùng thành file

**WorldEdit**: `//pos1`, `//pos2` (hoặc `//wand`), đứng ở vị trí gốc, `//copy`, `//schem save ten_file`. Định dạng Sponge `.schem` (v2/v3) nằm ở `config/worldedit/schematics/`.

**Amulet**: chọn vùng, Export, chọn Sponge Schematic (.schem) hoặc Structure (.nbt).

**Structure block** (vanilla): chế độ Save, đặt kích thước tối đa 48 mỗi chiều (vanilla giới hạn) rồi lưu; file `.nbt` nằm ở `saves/<world>/generated/minecraft/structures/`. Với công trình lớn nên dùng WorldEdit/Amulet.

Chép file vào `config/hukhong/structures/<thể loại>/`. Đuôi hỗ trợ: `.nbt`, `.schem`. (`.litematic` chưa hỗ trợ, hãy chuyển sang `.schem`.)

## 4. Quy ước

- **Tầng y=0 của file** được đặt đúng lớp mặt đất (thay khối mặt). Hãy bao gồm đúng lớp nền/sàn ở đáy vùng chọn.
- Công trình được **căn tâm** theo (x,z) của vị trí sinh. Địa hình trong vùng được dọn sạch (cao hơn công trình 10 block), phía dưới được đắp nền (đất/đá, tối đa 48 block) nếu đáy công trình chạm đất.
- Giới hạn kích thước: tối đa **256 x 192 x 256** (X x Y x Z). Lớn hơn sẽ bị từ chối.
- Đặt bất đồng bộ theo từng ô chunk (ngân sách ~6 ms mỗi tick), nên công trình lớn hiện dần trong vài giây; không lag tức thì. Nếu tắt server giữa chừng, công trình dang dở (không tự tiếp tục).
- Block entity (rương, biển...) được khôi phục từ file.
- Bán kính "cư dân thành" vẫn là 21 block quanh Ấn Hồn Đài, công trình lớn hơn vẫn hoạt động nhưng vùng đăng ký cư dân chỉ ở trung tâm.

## 5. Marker gameplay

Đặt **structure block chế độ DATA** trong vùng trước khi xuất, nhập chuỗi vào ô metadata (khối này không được đặt ra, chỉ đánh dấu vị trí). Với `.schem`, dùng structure block DATA và đảm bảo block entity xuất kèm metadata (nếu công cụ không giữ được, dùng `.nbt`).

| Metadata | Ý nghĩa | Không có marker |
|----------|---------|-----------------|
| `hukhong:altar` | vị trí Ấn Hồn Đài (thành) | tâm công trình, mặt nền + 1, tự lát bệ 3x3 |
| `hukhong:board` | Bảng Thành | cạnh Ấn Hồn Đài |
| `hukhong:npc` | nơi sinh dân làng (đặt nhiều marker) | 3 điểm quanh đài |
| `hukhong:linhkhi` | Linh Khí Đài (tông) | tâm công trình |
| `hukhong:sect_board` | Bảng Tông Môn | cạnh Linh Khí Đài |

Cũng chấp nhận viết ngắn (`altar`, `board`...). Vị trí marker là vị trí khối được đặt; tâm thành đăng ký = khối ngay dưới Ấn Hồn Đài.

## 6. Kiểm tra

Log server ghi `Structure '<tên>' (...) chosen for <thể loại>/<id>` và `Structure ... placed at ...`. Nếu không thấy, kiểm tra tên thư mục, đuôi file và cảnh báo `Skipping structure`.

## 7. Công cụ `tools/world_to_schem.py` (world Java -> .schem)

**Last Updated**: 2026-10-27 — thêm tool chuyển thế giới Java 1.18+ thành `.schem` + marker sidecar.

Cài một lần: `python -m venv tools/.venv` rồi `tools/.venv/Scripts/pip install nbtlib` (kéo theo numpy).

```
# một world
tools/.venv/Scripts/python tools/world_to_schem.py <worldDir> <thể loại> [--name TÊN] [--bounds x1 y1 z1 x2 y2 z2]
# hàng loạt: import/<thể loại>/<world>/ (world có thư mục region/)
tools/.venv/Scripts/python tools/world_to_schem.py --all D:/mc_mod/import
# kiểm tra file .schem (đọc lại)
tools/.venv/Scripts/python tools/world_to_schem.py --verify <file.schem>
```

- Ra file: `run/config/hukhong/structures/<thể loại>/<tên>.schem` (Sponge v2, DataVersion lấy từ level.dat) và `<tên>.schem.markers.json`.
- Vùng cắt: `--bounds`, hoặc file `toado.txt` trong thư mục world (`x1 y1 z1 x2 y2 z2`, toạ độ block tuyệt đối), hoặc tự dò: đếm block "nhân tạo" (ván, gạch, kính, len, bê tông, terracotta, stairs/slab/wall/fence, đèn lồng, cửa...) theo chunk (>= 40 block/chunk), flood fill các chunk dày, lấy cụm lớn nhất, đệm 4 block. Cụm > 256x256 thì chọn cửa sổ dày nhất và cảnh báo; Y tối đa 192 (giới hạn của loader).
- Chỉ đọc chunk DataVersion >= 2860 (palette + long array đóng gói); chunk cũ bị bỏ qua và báo warning. Block entity được giữ lại.
- Marker gợi ý (`altar`, `board`, `npc` x3, `linhkhi`, `sect_board`) tính từ quảng trường phẳng, thoáng, lớn nhất gần tâm. Loader tự đọc `<file>.markers.json` cạnh file `.schem` trong `config/hukhong/structures/` (toạ độ cục bộ trong file; marker trong chính file ưu tiên trước). Hãy mở thử và sửa tay JSON nếu vị trí gợi ý chưa hợp.
- Thư mục nhập: `import/{city_large,city_mountain,city_ruined,port,sect}/` (dữ liệu world bị gitignore, chỉ giữ `.gitkeep`).
