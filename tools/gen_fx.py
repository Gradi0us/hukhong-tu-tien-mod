# Sprite VFX trang (tint bang code): python tools/gen_fx.py
from PIL import Image
import math, os
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources', 'assets', 'hukhong', 'textures', 'particle')
os.makedirs(OUT, exist_ok=True)
def make(name, n, fn):
    im = Image.new('RGBA', (n, n), (0, 0, 0, 0))
    c = (n - 1) / 2
    for y in range(n):
        for x in range(n):
            a = max(0.0, min(1.0, fn((x - c) / (n / 2), (y - c) / (n / 2))))
            if a > 0.02:
                im.putpixel((x, y), (255, 255, 255, int(255 * a)))
    im.save(os.path.join(OUT, name + '.png'))
make('fx_glow', 16, lambda u, v: max(0, 1 - math.hypot(u, v)) ** 1.6 * 1.3)
make('fx_star', 16, lambda u, v: max((1 - abs(u) * 6) * (1 - abs(v)), (1 - abs(v) * 6) * (1 - abs(u)), (1 - math.hypot(u, v) * 1.6)))
make('fx_shard', 16, lambda u, v: (1 - abs(u + v) * 5) * (1 - math.hypot(u, v)) * 1.4)
print('ok')
