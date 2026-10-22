import sys
from PIL import Image
d, prefix, n, out = sys.argv[1], sys.argv[2], int(sys.argv[3]), sys.argv[4]
W, H = 270, 602
sh = Image.new('RGB', (n * W, H))
for k in range(n):
    sh.paste(Image.open(f'{d}\\{prefix}{k}.png').convert('RGB').resize((W, H)), (k * W, 0))
sh.save(f'{d}\\{out}.jpg', quality=85)
