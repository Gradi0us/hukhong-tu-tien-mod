"""Thêm khoá thoại/tên chiêu Hư Vương (hukhong.dialog.huvuong.*, hukhong.boss.move.hvg_*) vào cuối vi_vn/en_us."""
import json, os
L = 'src/main/resources/assets/hukhong/lang/'
K = [
 ("hukhong.boss.move.hvg_xa_ngoan", "Xà Thần Nuốt Hồn", "Serpent Soul-Devour"),
 ("hukhong.boss.move.hvg_phong_phu", "Phong Phủ Đạp", "Gale Axe Stomp"),
 ("hukhong.boss.move.hvg_cot_tra", "Ghép Thủ Loạn Vũ", "Grafted Arms Frenzy"),
 ("hukhong.boss.move.hvg_dung_nham", "Dung Nham Cuồn Cuộn", "Surging Magma"),
 ("hukhong.boss.move.hvg_trieu_hoi", "Triệu Hồn Bị Nuốt", "Call of the Devoured"),
 ("hukhong.boss.move.hvg_long_tuc", "Long Tức", "Dragon-Arm Breath"),
 ("hukhong.boss.move.hvg_lau_cot", "Lâu Cốt Phún Phát", "Skull Eruption"),
 ("hukhong.boss.move.hvg_dia_chan", "Địa Chấn Tam Đạp", "Threefold Quake"),
 ("hukhong.boss.move.hvg_thon_thien", "Hư Không Thôn Thiên", "Void Devours Heaven"),
 ("hukhong.dialog.huvuong.intro.1", "Lại một kẻ tu tiên tự dâng mình vào miệng Hư Không.", "Another cultivator offering himself to the Void's maw."),
 ("hukhong.dialog.huvuong.intro.2", "Linh căn, đạo tâm, hồn phách... ta nuốt tất cả.", "Spirit root, dao heart, soul... I devour them all."),
 ("hukhong.dialog.huvuong.p2.1", "Đói... vẫn còn đói.", "Hungry... still so hungry."),
 ("hukhong.dialog.huvuong.p2.2", "Cánh tay của những kẻ bại trận, nay là của ta!", "The arms of the fallen are mine now!"),
 ("hukhong.dialog.huvuong.p2.3", "Hãy trở thành một phần của bổn vương.", "Become part of this king."),
 ("hukhong.dialog.huvuong.p3.1", "Ngươi tưởng đã chạm tới đáy Hư Không sao?", "You think you have touched the Void's bottom?"),
 ("hukhong.dialog.huvuong.p3.2", "Thân này ghép từ rồng, lòng này sôi dung nham!", "This body grafted from dragons, this heart boils with magma!"),
 ("hukhong.dialog.huvuong.p3.3", "Hư Không vô đáy — và ta là cái miệng của nó!", "The Void is bottomless — and I am its mouth!"),
 ("hukhong.dialog.huvuong.ult.1", "Trời cao kia... ta cũng nuốt được!", "Even the heavens... I can swallow!"),
 ("hukhong.dialog.huvuong.ult.2", "Hư Không Thôn Thiên!", "Void Devours Heaven!"),
 ("hukhong.dialog.huvuong.death.1", "Sao... vẫn chưa no...", "How... am I still not sated..."),
 ("hukhong.dialog.huvuong.death.2", "Vạn hồn bị nuốt... đang xé ta từ bên trong.", "Ten thousand devoured souls... tear me from within."),
 ("hukhong.dialog.huvuong.death.3", "Hư Không... cuối cùng cũng nuốt luôn cả ta.", "The Void... at last devours me too."),
]
for f, col in (("vi_vn.json", 1), ("en_us.json", 2)):
    path = os.path.join(L, f)
    txt = open(path, encoding='utf-8').read().rstrip()
    d = json.loads(txt)
    add = [k for k in K if k[0] not in d]
    if not add: continue
    body = txt[:-1].rstrip()
    body += ',\n' + ',\n'.join('  ' + json.dumps(k[0]) + ': ' + json.dumps(k[col], ensure_ascii=False) for k in add) + '\n}\n'
    open(path, 'w', encoding='utf-8', newline='\n').write(body)
    json.loads(body)
