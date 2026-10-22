"""Remove baked-in backgrounds from AI images and export small transparent WebP files.

Usage: python clean_images.py <icons_dir> <preview_out.jpg> [source files to process, default all]
"""
import sys
from pathlib import Path

from PIL import Image
from rembg import new_session, remove

ICONS = Path(sys.argv[1])
PREVIEW = Path(sys.argv[2])

# source file -> (output name, max size in px, remove background?)
JOBS = {
    "otp-ghost.jpeg": ("otp-ghost.webp", 640, True),
    "ghost-male.jpeg": ("ghost-male.webp", 384, True),
    "ghost-female.jpeg": ("ghost-female.webp", 384, True),
    "ghost-other.jpeg": ("ghost-other.webp", 384, True),
    "ghost-camera.png": ("ghost-camera.webp", 384, True),
    "app-icon.jpeg": ("app-icon.webp", 512, False),
    "home.png": ("home.webp", 384, False),
    "chat.png": ("chat.webp", 384, False),
    "profile.png": ("profile.webp", 384, False),
    "ghost-search.png": ("ghost-search.webp", 512, True),
    "ghost-empty.png": ("ghost-empty.webp", 512, True),
    "coin.png": ("coin.webp", 192, True),
}
ONLY = set(sys.argv[3:])
if ONLY:
    # files not listed above (e.g. new ic-*.png icons) default to: cut background, 192px webp
    JOBS = {k: JOBS.get(k, (Path(k).stem + ".webp", 192, True)) for k in ONLY}

# REMBG_MODEL=birefnet-general-lite gives cleaner edges on light/translucent subjects
import os
session = new_session(os.environ.get("REMBG_MODEL", "isnet-general-use"))
done = []
for src, (out, size, cut) in JOBS.items():
    path = ICONS / src
    if not path.exists():
        print(f"skip (missing): {src}")
        continue
    img = Image.open(path).convert("RGBA")
    img.thumbnail((1280, 1280), Image.LANCZOS)  # big AI images run the model out of memory
    if cut:
        img = remove(img, session=session, post_process_mask=True)
    # trim empty borders, keep a little breathing room
    bbox = img.getchannel("A").point(lambda a: 255 if a > 8 else 0).getbbox()
    if bbox:
        pad = int(max(bbox[2] - bbox[0], bbox[3] - bbox[1]) * 0.04)
        bbox = (max(bbox[0] - pad, 0), max(bbox[1] - pad, 0),
                min(bbox[2] + pad, img.width), min(bbox[3] + pad, img.height))
        img = img.crop(bbox)
    # square canvas so every icon lines up the same in the UI
    side = max(img.size)
    canvas = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    canvas.paste(img, ((side - img.width) // 2, (side - img.height) // 2))
    canvas.thumbnail((size, size), Image.LANCZOS)
    canvas.save(ICONS / out, "WEBP", quality=88, method=6)
    print(f"{src} -> {out} {canvas.size} {(ICONS / out).stat().st_size // 1024} KB")
    done.append(canvas)

# preview sheet on the app's dark background
if done:
    cell = 200
    sheet = Image.new("RGB", (cell * len(done), cell), (7, 7, 12))
    for i, im in enumerate(done):
        t = im.copy()
        t.thumbnail((cell, cell))
        sheet.paste(t, (i * cell + (cell - t.width) // 2, (cell - t.height) // 2), t)
    sheet.save(PREVIEW, quality=80)
