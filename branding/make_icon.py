from PIL import Image, ImageDraw, ImageFilter, ImageFont, ImageChops
S = 2048                      # work size, downscaled to 512 at the end
img = Image.new("RGBA", (S, S), (0, 0, 0, 0))

def lerp(a, b, t): return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(len(a)))

# --- background: diagonal gradient in a rounded square
bg = Image.new("RGBA", (S, S))
top, bottom = (58, 28, 112), (14, 10, 32)
px = bg.load()
for y in range(S):
    for x in range(0, S, 1):
        t = min(1, max(0, (x * 0.35 + y) / (S * 1.35)))
        px[x, y] = lerp(top, bottom, t) + (255,)
mask = Image.new("L", (S, S), 0)
ImageDraw.Draw(mask).rounded_rectangle([0, 0, S - 1, S - 1], radius=int(S * 0.2), fill=255)
img.paste(bg, (0, 0), mask)

# soft radial glow behind the content
glow = Image.new("RGBA", (S, S), (0, 0, 0, 0))
ImageDraw.Draw(glow).ellipse([S*0.18, S*0.12, S*0.82, S*0.76], fill=(150, 90, 255, 120))
glow = glow.filter(ImageFilter.GaussianBlur(S * 0.09))
img = Image.alpha_composite(img, Image.composite(glow, Image.new("RGBA", (S, S)), mask))
d = ImageDraw.Draw(img)

# --- disguise mask (the "spoof")
def eye_mask(draw, cx, cy, w, h, fill):
    # two lens lobes joined by a bridge
    lw = w * 0.46
    draw.rounded_rectangle([cx - w/2, cy - h/2, cx - w/2 + lw, cy + h/2], radius=h*0.5, fill=fill)
    draw.rounded_rectangle([cx + w/2 - lw, cy - h/2, cx + w/2, cy + h/2], radius=h*0.5, fill=fill)
    draw.rectangle([cx - w*0.1, cy - h*0.28, cx + w*0.1, cy + h*0.12], fill=fill)
    # eye holes
    ew, eh = lw * 0.52, h * 0.34
    for ex in (cx - w/2 + lw*0.52, cx + w/2 - lw*0.52):
        draw.ellipse([ex - ew/2, cy - eh/2 - h*0.02, ex + ew/2, cy + eh/2 - h*0.02], fill=(0, 0, 0, 0))

mcx, mcy, mw, mh = S/2, S*0.35, S*0.62, S*0.24
# shadow
sh = Image.new("RGBA", (S, S), (0, 0, 0, 0)); eye_mask(ImageDraw.Draw(sh), mcx, mcy + S*0.025, mw, mh, (0, 0, 0, 150))
img = Image.alpha_composite(img, sh.filter(ImageFilter.GaussianBlur(S*0.015)))
# body with a white->lavender gradient
layer = Image.new("RGBA", (S, S), (0, 0, 0, 0)); eye_mask(ImageDraw.Draw(layer), mcx, mcy, mw, mh, (255, 255, 255, 255))
grad = Image.new("RGBA", (S, S)); gp = ImageDraw.Draw(grad)
for y in range(int(mcy - mh/2), int(mcy + mh/2) + 1):
    t = (y - (mcy - mh/2)) / mh
    gp.line([(0, y), (S, y)], fill=lerp((255, 255, 255), (196, 170, 255), t) + (255,))
body = Image.composite(grad, Image.new("RGBA", (S, S), (0, 0, 0, 0)), layer.split()[3])
# punch the eye holes (they were drawn transparent in `layer`)
body.putalpha(ImageChops.multiply(body.split()[3], layer.split()[3]))
img = Image.alpha_composite(img, body)

# --- nametag plate with "HT1"
d = ImageDraw.Draw(img)
tx0, ty0, tx1, ty1 = S*0.14, S*0.56, S*0.86, S*0.84
plate = Image.new("RGBA", (S, S), (0, 0, 0, 0)); pd = ImageDraw.Draw(plate)
pd.rounded_rectangle([tx0, ty0 + S*0.02, tx1, ty1 + S*0.02], radius=S*0.05, fill=(0, 0, 0, 140))
plate = plate.filter(ImageFilter.GaussianBlur(S*0.012))
img = Image.alpha_composite(img, plate)
d = ImageDraw.Draw(img)
d.rounded_rectangle([tx0, ty0, tx1, ty1], radius=S*0.05, fill=(12, 8, 26, 235), outline=(245, 206, 77, 255), width=int(S*0.018))

font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", int(S*0.2))
text = "HT1"
bb = d.textbbox((0, 0), text, font=font)
tw, th = bb[2] - bb[0], bb[3] - bb[1]
x = (S - tw) / 2 - bb[0]; y = (ty0 + ty1) / 2 - th / 2 - bb[1]
# text shadow
d.text((x + S*0.012, y + S*0.012), text, font=font, fill=(80, 52, 0, 255))
# gold gradient text
tl = Image.new("L", (S, S), 0); ImageDraw.Draw(tl).text((x, y), text, font=font, fill=255)
g2 = Image.new("RGBA", (S, S)); g2d = ImageDraw.Draw(g2)
for yy in range(int(y + bb[1]), int(y + bb[3]) + 1):
    t = (yy - (y + bb[1])) / max(1, th)
    g2d.line([(0, yy), (S, yy)], fill=lerp((255, 236, 140), (232, 160, 30), t) + (255,))
img.paste(g2, (0, 0), tl)

# --- sparkle
def sparkle(draw, cx, cy, r, fill):
    draw.polygon([(cx, cy - r), (cx + r*0.22, cy - r*0.22), (cx + r, cy), (cx + r*0.22, cy + r*0.22),
                  (cx, cy + r), (cx - r*0.22, cy + r*0.22), (cx - r, cy), (cx - r*0.22, cy - r*0.22)], fill=fill)
d = ImageDraw.Draw(img)
sparkle(d, S*0.82, S*0.17, S*0.07, (255, 240, 170, 255))
sparkle(d, S*0.72, S*0.1, S*0.03, (255, 255, 255, 220))

out = img.resize((512, 512), Image.LANCZOS)
out.save("icon_512.png")
img.resize((128, 128), Image.LANCZOS).save("icon_128.png")
print("ok")
