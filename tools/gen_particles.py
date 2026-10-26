# Sprite 8x8 particle: python tools/gen_particles.py
from PIL import Image
import os
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources', 'assets', 'hukhong', 'textures', 'particle')
os.makedirs(OUT, exist_ok=True)
SHAPES = {
    'thanh_quang': ((255, 214, 80), ["...##...", "...##...", "..####..", "########", "########", "..####..", "...##...", "...##..."]),
    'linh_khi': ((70, 230, 190), ["........", "..####..", ".######.", ".######.", ".######.", ".######.", "..####..", "........"]),
    'ba_khi': ((255, 140, 40), ["...#....", "..##....", "..###.#.", ".#####.#", ".#######", "########", ".######.", "..####.."]),
    'hu_khong': ((160, 80, 230), ["..####..", ".#....#.", "#..##..#", "#.#..#.#", "#.#..#.#", "#..###.#", ".#.....#", "..####.."]),
}
for name, (rgb, rows) in SHAPES.items():
    im = Image.new('RGBA', (8, 8), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, c in enumerate(row):
            if c == '#':
                d = min(abs(x - 3.5), abs(y - 3.5))
                k = 1.0 - 0.12 * d
                im.putpixel((x, y), (min(255, int(rgb[0] * k + 40 * (1 - d / 4))), min(255, int(rgb[1] * k + 30)),
                                     min(255, int(rgb[2] * k + 30)), 255))
    im.save(os.path.join(OUT, name + '.png'))
print('ok')
