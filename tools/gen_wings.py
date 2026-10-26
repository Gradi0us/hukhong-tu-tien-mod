# Cánh thiên sứ 1 bên (32x32), mirror ở render: python tools/gen_wings.py
from PIL import Image, ImageDraw
import math
import os

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources',
                   'assets', 'hukhong', 'textures', 'entity')
os.makedirs(OUT, exist_ok=True)
S = 32
im = Image.new('RGBA', (S, S), (0, 0, 0, 0))
d = ImageDraw.Draw(im)
# 5 chiếc lông tỏa từ gốc (0, ~8) ra phía phải, cong xuống
for i in range(5):
    ang = math.radians(-10 + i * 24)
    length = 31 - i * 3
    pts = []
    for t in range(0, 11):
        f = t / 10.0
        x = f * length * math.cos(ang)
        y = 6 + f * length * math.sin(ang) + 6 * f * f
        pts.append((x, y))
    for t, (x, y) in enumerate(pts):
        f = t / 10.0
        r = 2.6 * (1 - f * 0.6)
        k = f
        col = (int(255), int(255 - 40 * k), int(235 - 120 * k), 255)
        d.ellipse([x - r, y - r * 1.2, x + r, y + r * 1.2], fill=col)
# viền xám nhạt
px = im.load()
edge = []
for y in range(S):
    for x in range(S):
        if px[x, y][3] == 0:
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < S and 0 <= ny < S and px[nx, ny][3] == 255:
                    edge.append((x, y)); break
for x, y in edge:
    px[x, y] = (170, 160, 150, 255)
im.save(os.path.join(OUT, 'wings.png'))
print('ok')
