"""Draws the R-logo .ico for the EXE (Pillow, CI only)."""
import sys
from PIL import Image, ImageDraw

def main(out):
    img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle((8, 8, 248, 248), 56, fill=(14, 9, 22, 255), outline=(139, 92, 246, 255), width=8)
    d.rectangle((78, 64, 106, 192), fill=(167, 139, 250, 255))
    d.pieslice((78, 64, 178, 144), -90, 90, fill=(139, 92, 246, 255))
    d.pieslice((106, 88, 150, 120), -90, 90, fill=(14, 9, 22, 255))
    d.polygon([(118, 140), (150, 140), (186, 192), (152, 192)], fill=(139, 92, 246, 255))
    img.save(out, sizes=[(16, 16), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)])

main(sys.argv[1])
