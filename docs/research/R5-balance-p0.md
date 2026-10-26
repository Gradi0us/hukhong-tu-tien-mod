# R5 - Cân bằng nhanh P0 (TTK Đạo/Thể vs Hư Hóa)

**Last Updated**: 2026-10-26 - Bước 9: mô phỏng TTK + chỉnh hằng số

## Phương pháp
`src/test/java/com/hukhong/entity/HuHoaBalanceTest.java`: dùng `StatService.compute/maxHealth/mitigate` + `HuHoa.hpScale/dmgScale/skillDamage`.
Sát thương mỗi đòn = hệ số kỹ năng 1 (Đạo `DAO_1_COEF` x Phép, Thể `THE_1_COEF` x Công) x (1 + crit x (critDmg-1)); kỹ năng đi qua `indirectMagic` nên bỏ qua giáp quái.
Số đòn = ceil(HP quái / sát thương). Quái đánh mạnh nhất = max(cận chiến, kỹ năng x1.5), trừ Phòng người chơi; yêu cầu < 50% HP người chơi (không one-shot).

## Hằng số đã chỉnh
| Hằng số | Cũ | Mới | Lý do |
|---|---|---|---|
| `HuHoa.HP_PER_LEVEL` (hpScale) | 0.12 | 0.38 | Cũ: Đạo L30 hạ quái cùng cấp trong 3 đòn. |
| `Skill.DAO_1_COEF` | 1.10 | 0.90 | Đạo_1 là đòn spam (CD 0.8s, tốn 8) nên không nên mạnh hơn Thể_1 quá 1.7x. |
| HP mul HuThi / HuCot | 1.6 / 1.2 | 1.45 / 1.5 | Đưa cả 3 quái vào 6-10 đòn. |

## Kết quả (số đòn kỹ năng 1 / sát thương quái tối đa nhận sau Phòng / HP người)
| Đạo | Lv | HuThi | HuCot | HuChu | Quái đánh tối đa | HP người |
|---|---|---|---|---|---|---|
| Đạo | 1 | 6 | 6 | 6 | 4.5 | 20 |
| Đạo | 10 | 6 | 7 | 6 | 8.3 | 51.5 |
| Đạo | 20 | 6 | 7 | 6 | 12.3 | 86.5 |
| Đạo | 30 | 6 | 7 | 6 | 16.1 | 121.5 |
| Thể | 1 | 10 | 10 | 10 | 4.4 | 24 |
| Thể | 10 | 9 | 9 | 9 | 7.8 | 87 |
| Thể | 20 | 9 | 9 | 9 | 11.0 | 157 |
| Thể | 30 | 9 | 9 | 9 | 13.9 | 227 |

Tất cả trong 6-10; quái không one-shot (đòn mạnh nhất <= ~37% HP ở Lv1 Đạo, ~14% ở Lv30 Đạo).

## Cảnh báo / theo dõi
- HP quái tăng ~3x ở cấp cao => EXP/giờ giảm tương ứng (`HuHoa.exp` chưa đổi). Cần playtest, có thể nhân `exp` ~x1.5-2 nếu chậm.
- Đạo Lv30 đánh 6-7 đòn (~5s) trong khi HuThi cận chiến ~11.7/s: Đạo phải giữ khoảng cách; chưa mô phỏng dps quái theo thời gian.
- Chỉ mô phỏng kỹ năng 1, tư chất B, 1 đạo; chưa tính Lõi/bộ Thần/Phân Tâm.
