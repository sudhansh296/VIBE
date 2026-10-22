"""Draws flat-illustration portraits for the 8 test hosts (3 photos each) with PIL. No external images needed.
Output: C:\\VIBE\\call me\\app\\app\\src\\main\\res\\drawable-nodpi\\host_<id>_<n>.webp (640x800)."""
import math
import os
import random
from PIL import Image, ImageDraw, ImageFilter

OUT = r'C:\VIBE\call me\app\app\src\main\res\drawable-nodpi'
W, H = 640, 800
S = 2  # supersample

HOSTS = {
    1: dict(g='F', skin=(236, 190, 160), hair=(40, 24, 22), hair_style='long', top=(255, 255, 255), earr=(255, 215, 120), bg=((255, 196, 220), (255, 120, 170))),
    2: dict(g='M', skin=(205, 152, 118), hair=(25, 20, 20), hair_style='short', top=(60, 70, 110), beard='stubble', bg=((200, 220, 255), (120, 160, 240))),
    3: dict(g='F', skin=(226, 176, 140), hair=(70, 36, 28), hair_style='wavy', top=(255, 120, 170), earr=(255, 255, 255), bg=((255, 210, 190), (255, 130, 140))),
    4: dict(g='M', skin=(214, 164, 128), hair=(35, 28, 26), hair_style='short', top=(40, 40, 48), glasses=True, bg=((215, 205, 255), (150, 120, 230))),
    5: dict(g='F', skin=(190, 138, 104), hair=(30, 20, 18), hair_style='bun', top=(255, 196, 80), earr=(255, 100, 140), bg=((190, 240, 225), (90, 200, 170))),
    6: dict(g='M', skin=(222, 172, 136), hair=(45, 30, 24), hair_style='messy', top=(230, 120, 90), beard='stubble', bg=((255, 230, 190), (255, 170, 100))),
    7: dict(g='F', skin=(232, 184, 150), hair=(28, 22, 24), hair_style='long', top=(150, 60, 140), earr=(255, 215, 120), bg=((255, 200, 230), (210, 100, 190))),
    8: dict(g='M', skin=(196, 144, 110), hair=(30, 24, 22), hair_style='short', top=(240, 240, 245), beard='full', bg=((200, 240, 255), (90, 190, 230))),
}


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def gradient(w, h, c1, c2, angle_deg=70):
    img = Image.new('RGB', (w, h), c1)
    px = img.load()
    a = math.radians(angle_deg)
    dx, dy = math.cos(a), math.sin(a)
    mx = abs(dx) * w + abs(dy) * h
    for y in range(h):
        for x in range(w):
            t = ((x - w / 2) * dx + (y - h / 2) * dy) / mx + 0.5
            px[x, y] = lerp(c1, c2, max(0, min(1, t)))
    return img


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c)


def draw_person(p, variant):
    w, h = W * S, H * S
    img = gradient(w // 4, h // 4, p['bg'][0], p['bg'][1]).resize((w, h), Image.BILINEAR)
    d = ImageDraw.Draw(img, 'RGBA')
    rnd = random.Random(p['top'][0] * 7 + variant)

    # soft background shapes
    for _ in range(4):
        r = rnd.randint(120, 260) * S
        cx, cy = rnd.randint(0, w), rnd.randint(0, int(h * .6))
        d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(255, 255, 255, 38))

    # variant tweaks: 0 = centred, 1 = shifted + lighter bg tint, 2 = close up
    scale = [1.0, 0.92, 1.38][variant]
    ox = [0, -50, 0][variant] * S
    oy = [0, 30, 150][variant] * S
    cx = w // 2 + ox
    head_cy = int(h * 0.40) + oy
    hr_x = int(150 * S * scale)
    hr_y = int(185 * S * scale)
    skin, hair = p['skin'], p['hair']
    skin_d = shade(skin, 0.88)
    top = p['top'] if variant != 1 else shade(p['top'], 0.85)

    def ell(x, y, rx, ry, fill):
        d.ellipse([x - rx, y - ry, x + rx, y + ry], fill=fill)

    # back hair
    style = p['hair_style']
    if style in ('long', 'wavy'):
        ell(cx, head_cy + int(30 * S * scale), hr_x + int(34 * S * scale), hr_y + int(70 * S * scale), hair)
        d.rectangle([cx - hr_x - int(34 * S * scale), head_cy + int(40 * S * scale), cx + hr_x + int(34 * S * scale), head_cy + int(330 * S * scale)], fill=hair)
        if style == 'wavy':
            for k in range(5):
                ell(cx - hr_x - int(10 * S * scale) + k * 0, head_cy + int((200 + k * 40) * S * scale), int(46 * S * scale), int(40 * S * scale), hair)
                ell(cx + hr_x + int(10 * S * scale), head_cy + int((200 + k * 40) * S * scale), int(46 * S * scale), int(40 * S * scale), hair)
    if style == 'bun':
        ell(cx, head_cy - hr_y - int(40 * S * scale), int(62 * S * scale), int(62 * S * scale), hair)

    # shoulders / clothes
    sh_top = head_cy + int(hr_y * 0.95)
    d.rounded_rectangle([cx - int(330 * S * scale), sh_top + int(70 * S * scale), cx + int(330 * S * scale), h + 400 * S], radius=int(170 * S * scale), fill=top)
    # neck
    d.rounded_rectangle([cx - int(58 * S * scale), head_cy + int(hr_y * 0.55), cx + int(58 * S * scale), sh_top + int(95 * S * scale)], radius=int(30 * S), fill=skin_d)
    # collar / neckline
    d.polygon([(cx - int(70 * S * scale), sh_top + int(75 * S * scale)), (cx + int(70 * S * scale), sh_top + int(75 * S * scale)), (cx, sh_top + int(190 * S * scale))], fill=skin_d)
    d.ellipse([cx - int(70 * S * scale), sh_top + int(55 * S * scale), cx + int(70 * S * scale), sh_top + int(120 * S * scale)], fill=skin_d)

    # ears
    ell(cx - hr_x + int(6 * S * scale), head_cy + int(10 * S * scale), int(24 * S * scale), int(36 * S * scale), skin_d)
    ell(cx + hr_x - int(6 * S * scale), head_cy + int(10 * S * scale), int(24 * S * scale), int(36 * S * scale), skin_d)
    if 'earr' in p:
        for sx in (-1, 1):
            ell(cx + sx * (hr_x - int(6 * S * scale)), head_cy + int(58 * S * scale), int(11 * S * scale), int(16 * S * scale), p['earr'])
    # head
    ell(cx, head_cy, hr_x, hr_y, skin)
    # cheek shading
    d.ellipse([cx - hr_x, head_cy - hr_y, cx + hr_x, head_cy + hr_y], outline=skin_d + (0,), width=0)

    # front hair
    if style == 'long':
        d.pieslice([cx - hr_x - 12 * S, head_cy - hr_y - 20 * S, cx + hr_x + 12 * S, head_cy + hr_y * 0.35], 180, 360, fill=hair)
        d.polygon([(cx - hr_x, head_cy - int(20 * S * scale)), (cx, head_cy - int(hr_y * 0.80)), (cx + int(hr_x * 0.15), head_cy - int(hr_y * 0.55)), (cx - int(hr_x * 0.2), head_cy + int(10 * S * scale))], fill=hair)
        d.polygon([(cx - hr_x - 10 * S, head_cy - 30 * S), (cx - hr_x + int(32 * S * scale), head_cy + int(hr_y * 0.55)), (cx - hr_x - 20 * S, head_cy + int(hr_y * 0.7))], fill=hair)
        d.polygon([(cx + hr_x + 10 * S, head_cy - 30 * S), (cx + hr_x - int(32 * S * scale), head_cy + int(hr_y * 0.55)), (cx + hr_x + 20 * S, head_cy + int(hr_y * 0.7))], fill=hair)
    elif style in ('wavy', 'bun'):
        d.pieslice([cx - hr_x - 10 * S, head_cy - hr_y - 14 * S, cx + hr_x + 10 * S, head_cy + hr_y * 0.45], 180, 360, fill=hair)
        d.polygon([(cx - hr_x + 10 * S, head_cy - int(10 * S * scale)), (cx - int(30 * S * scale), head_cy - int(hr_y * 0.78)), (cx + int(hr_x * 0.9), head_cy - int(hr_y * 0.25)), (cx + hr_x - 10 * S, head_cy - int(40 * S * scale))], fill=hair)
    elif style in ('short', 'messy'):
        d.pieslice([cx - hr_x - 8 * S, head_cy - hr_y - 30 * S, cx + hr_x + 8 * S, head_cy + hr_y * 0.18], 180, 360, fill=hair)
        d.polygon([(cx - hr_x + 4 * S, head_cy - int(60 * S * scale)), (cx - int(30 * S * scale), head_cy - int(hr_y * 0.62)), (cx + hr_x - 4 * S, head_cy - int(60 * S * scale)), (cx + hr_x - 14 * S, head_cy - int(hr_y * 0.35)), (cx - hr_x + 14 * S, head_cy - int(hr_y * 0.35))], fill=hair)
        if style == 'messy':
            for k in range(6):
                ell(cx - hr_x + int(k * hr_x * 0.4), head_cy - hr_y + int(10 * S) - (k % 2) * 14 * S, int(34 * S * scale), int(28 * S * scale), hair)

    # beard
    if p.get('beard'):
        a = 70 if p['beard'] == 'stubble' else 255
        bc = hair + (a,)
        d.pieslice([cx - hr_x + 6 * S, head_cy - int(hr_y * 0.05), cx + hr_x - 6 * S, head_cy + hr_y + 8 * S], 0, 180, fill=bc)
        if p['beard'] == 'full':
            d.rectangle([cx - int(46 * S * scale), head_cy + int(hr_y * 0.50), cx + int(46 * S * scale), head_cy + int(hr_y * 0.62)], fill=skin)

    # face
    ey = head_cy + int(8 * S * scale)
    ex = int(62 * S * scale)
    for sx in (-1, 1):
        ell(cx + sx * ex, ey, int(23 * S * scale), int(15 * S * scale), (255, 255, 255))
        ell(cx + sx * ex + sx * 2 * S, ey, int(11 * S * scale), int(12 * S * scale), (46, 30, 28))
        ell(cx + sx * ex + sx * 5 * S, ey - int(4 * S * scale), int(4 * S * scale), int(4 * S * scale), (255, 255, 255))
        # brows
        d.line([(cx + sx * (ex - int(26 * S * scale)), ey - int(38 * S * scale)), (cx + sx * (ex + int(24 * S * scale)), ey - int(44 * S * scale))], fill=hair, width=int(8 * S * scale))
        # lashes for F
        if p['g'] == 'F':
            d.arc([cx + sx * ex - int(25 * S * scale), ey - int(18 * S * scale), cx + sx * ex + int(25 * S * scale), ey + int(14 * S * scale)], 200, 340, fill=(30, 20, 20), width=int(5 * S * scale))
    if p.get('glasses'):
        for sx in (-1, 1):
            d.rounded_rectangle([cx + sx * ex - int(42 * S * scale), ey - int(30 * S * scale), cx + sx * ex + int(42 * S * scale), ey + int(30 * S * scale)], radius=int(14 * S), outline=(30, 30, 36), width=int(6 * S * scale))
        d.line([(cx - int(20 * S * scale), ey - 4 * S), (cx + int(20 * S * scale), ey - 4 * S)], fill=(30, 30, 36), width=int(6 * S * scale))
    # nose
    d.arc([cx - int(18 * S * scale), ey + int(30 * S * scale), cx + int(18 * S * scale), ey + int(78 * S * scale)], 20, 160, fill=skin_d, width=int(6 * S * scale))
    # blush
    for sx in (-1, 1):
        ell(cx + sx * int(98 * S * scale), ey + int(62 * S * scale), int(34 * S * scale), int(20 * S * scale), (255, 110, 140, 70))
    # mouth
    my = ey + int(112 * S * scale)
    lip = (196, 70, 90) if p['g'] == 'F' else (160, 80, 78)
    d.pieslice([cx - int(48 * S * scale), my - int(26 * S * scale), cx + int(48 * S * scale), my + int(30 * S * scale)], 0, 180, fill=lip)
    d.chord([cx - int(30 * S * scale), my - int(2 * S * scale), cx + int(30 * S * scale), my + int(16 * S * scale)], 0, 180, fill=(255, 255, 255))

    img = img.filter(ImageFilter.GaussianBlur(0.6 * S / 2))
    return img.resize((W, H), Image.LANCZOS)


def main():
    os.makedirs(OUT, exist_ok=True)
    for hid, p in HOSTS.items():
        for v in range(3):
            im = draw_person(p, v)
            im.save(os.path.join(OUT, f'host_{hid}_{v}.webp'), 'WEBP', quality=82)
    print('done')


if __name__ == '__main__':
    main()
