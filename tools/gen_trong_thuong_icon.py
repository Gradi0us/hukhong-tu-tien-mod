from PIL import Image, ImageDraw
im = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
d = ImageDraw.Draw(im)
d.ellipse((1, 1, 16, 16), fill=(40, 6, 10, 255), outline=(120, 10, 20, 255))
# vết nứt đỏ sẫm
for pts in ([(9, 2), (7, 6), (10, 9), (8, 12), (9, 16)], [(7, 6), (4, 7)], [(10, 9), (14, 8)], [(8, 12), (5, 14)]):
    d.line(pts, fill=(205, 20, 35, 255), width=1)
d.point((9, 2), fill=(255, 90, 90, 255))
im.save("src/main/resources/assets/hukhong/textures/mob_effect/trong_thuong.png")
