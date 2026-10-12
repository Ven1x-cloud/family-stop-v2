"""Bouw het definitieve Family Stop-icoon: logo + verbodsbord."""
from PIL import Image, ImageDraw
import json, math, sys

sys.path.insert(0, "/home/user/logo-work")
from lib import bezier_punt

# ---------------------------------------------------------------- kleuren
BLUW_D, BLUW_L = (66, 133, 244), (102, 157, 246)
GROEN_D, GROEN_L = (52, 168, 83), (91, 185, 116)
GEEL_D, GEEL_L = (251, 188, 4), (252, 201, 52)
GRIJS_L, GRIJS_D = (218, 220, 224), (189, 193, 198)
ROOD = (217, 48, 37)          # #D93025

DOEK = 512
RING_MIDDEN = (256.0, 250.0)
RING_BUITEN = 232.0          # buitenrand van de rode ring
RING_DIK = 44.0              # dikte van de ring (ca. 19% van de straal, ISO-verhouding)
STREEP_BREED = RING_DIK      # de schuine balk is even dik als de ring


def hex_kleur(rgb):
    return "#%02X%02X%02X" % rgb


# ---------------------------------------------------------------- inladen
kite = json.load(open("/home/user/logo-work/kite_klaar.json"))
lint = json.load(open("/home/user/logo-work/lint_klaar.json"))
AS = kite["as"]

# de kite staat gecentreerd op x=0 -> naar absolute coordinaten
kite_curven = [[(p[0] + AS, p[1]) for p in b] for b in kite["curven"]]
lint_curven = lint["lint"]["curven"]
vouw_curven = [v["curven"] for v in lint["vouwen"]]

# ---------------------------------------------------------------- transformatie
# de kite zo groot maken dat zijn uiterste punten precies de buitenrand van de
# ring raken; het lint steekt daardoor vanzelf buiten de ring uit
xs = [p[0] for b in kite_curven for p in b]
ys = [p[1] for b in kite_curven for p in b]
kx0, kx1 = min(xs), max(xs)
ky0, ky1 = min(ys), max(ys)
kite_midden = ((kx0 + kx1) / 2.0, (ky0 + ky1) / 2.0)
halve_diagonaal = math.hypot((kx1 - kx0) / 2.0, (ky1 - ky0) / 2.0)
S = RING_BUITEN / halve_diagonaal
TX = RING_MIDDEN[0] - kite_midden[0] * S
TY = RING_MIDDEN[1] - kite_midden[1] * S
print("schaal %.5f  translate (%.2f, %.2f)  halve diagonaal %.2f" % (S, TX, TY, halve_diagonaal))


def T(p):
    return (p[0] * S + TX, p[1] * S + TY)


def pad_van(curven, decimalen=2):
    fmt = "%." + str(decimalen) + "f"
    eerste = T(curven[0][0])
    delen = ["M %s %s" % (fmt % eerste[0], fmt % eerste[1])]
    vorige = eerste
    for b in curven:
        c1, c2, eind = T(b[1]), T(b[2]), T(b[3])
        if abs(c1[0] - vorige[0]) < 0.005 and abs(c1[1] - vorige[1]) < 0.005 and \
           abs(c2[0] - eind[0]) < 0.005 and abs(c2[1] - eind[1]) < 0.005:
            delen.append("L %s %s" % (fmt % eind[0], fmt % eind[1]))
        else:
            delen.append("C %s %s %s %s %s %s" % (fmt % c1[0], fmt % c1[1], fmt % c2[0],
                                                   fmt % c2[1], fmt % eind[0], fmt % eind[1]))
        vorige = eind
    return " ".join(delen)


kite_pad = pad_van(kite_curven)
lint_pad = pad_van(lint_curven)
vouw_paden = [pad_van(c) for c in vouw_curven]

# bandgrenzen omgerekend
g1, g2 = 113.0, 188.0
centrum_x = AS * S + TX
y_g1 = T((0, g1))[1]
y_g2 = T((0, g2))[1]
print("banden: y=%.2f en y=%.2f, midden x=%.2f" % (y_g1, y_g2, centrum_x))


def svg_voor(streep_links_boven=True, achtergrond=None, rand_achtergrond=None):
    cx, cy = RING_MIDDEN
    r_midden = RING_BUITEN - RING_DIK / 2.0

    delen = []
    delen.append('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 %d %d" width="%d" height="%d">'
                 % (DOEK, DOEK, DOEK, DOEK))
    delen.append('  <title>Family Stop</title>')
    if achtergrond:
        if rand_achtergrond:
            delen.append('  <rect width="%d" height="%d" rx="112" fill="%s"/>'
                         % (DOEK, DOEK, hex_kleur(achtergrond)))
        else:
            delen.append('  <rect width="%d" height="%d" fill="%s"/>'
                         % (DOEK, DOEK, hex_kleur(achtergrond)))

    delen.append('  <defs>')
    delen.append('    <clipPath id="kiteclip"><path d="%s Z"/></clipPath>' % kite_pad)
    delen.append('    <clipPath id="ringclip"><circle cx="%.2f" cy="%.2f" r="%.2f"/></clipPath>'
                 % (cx, cy, RING_BUITEN))
    delen.append('  </defs>')

    # ---- het logo
    delen.append('  <g id="logo">')
    delen.append('    <g clip-path="url(#kiteclip)">')
    delen.append('      <rect x="0" y="%0.2f" width="%0.2f" height="%0.2f" fill="%s"/>'
                 % (y_g1 - 260, centrum_x, 260.0, hex_kleur(BLUW_D)))
    delen.append('      <rect x="%.2f" y="%0.2f" width="%.2f" height="%.2f" fill="%s"/>'
                 % (centrum_x, y_g1 - 260, DOEK, 260.0, hex_kleur(BLUW_L)))
    delen.append('      <rect x="0" y="%0.2f" width="%.2f" height="%.2f" fill="%s"/>'
                 % (y_g1, centrum_x, y_g2 - y_g1, hex_kleur(GROEN_D)))
    delen.append('      <rect x="%.2f" y="%.2f" width="%.2f" height="%.2f" fill="%s"/>'
                 % (centrum_x, y_g1, DOEK, y_g2 - y_g1, hex_kleur(GROEN_L)))
    delen.append('      <rect x="0" y="%.2f" width="%.2f" height="%.2f" fill="%s"/>'
                 % (y_g2, centrum_x, DOEK, hex_kleur(GEEL_D)))
    delen.append('      <rect x="%.2f" y="%.2f" width="%.2f" height="%.2f" fill="%s"/>'
                 % (centrum_x, y_g2, DOEK, DOEK, hex_kleur(GEEL_L)))
    delen.append('    </g>')
    delen.append('    <path d="%s Z" fill="%s"/>' % (lint_pad, hex_kleur(GRIJS_L)))
    for p in vouw_paden:
        delen.append('    <path d="%s Z" fill="%s"/>' % (p, hex_kleur(GRIJS_D)))
    delen.append('  </g>')

    # ---- het verbodsbord
    dx = 1.0 if streep_links_boven else -1.0
    lengte = RING_BUITEN * 1.5
    bx1 = cx - dx * lengte * 0.7071
    by1 = cy - lengte * 0.7071
    bx2 = cx + dx * lengte * 0.7071
    by2 = cy + lengte * 0.7071
    delen.append('  <g id="verbod" clip-path="url(#ringclip)" fill="%s">' % hex_kleur(ROOD))
    delen.append('    <circle cx="%.2f" cy="%.2f" r="%.2f" fill="none" stroke="%s" '
                 'stroke-width="%.2f"/>' % (cx, cy, r_midden, hex_kleur(ROOD), RING_DIK))
    delen.append('    <line x1="%.2f" y1="%.2f" x2="%.2f" y2="%.2f" stroke="%s" '
                 'stroke-width="%.2f"/>' % (bx1, by1, bx2, by2, hex_kleur(ROOD), STREEP_BREED))
    delen.append('  </g>')
    delen.append('</svg>')
    return "\n".join(delen)


def teken_png(streep_links_boven=True, achtergrond=None, maat=1024):
    ss = 2
    W = maat * ss
    f = W / float(DOEK)
    doek = Image.new("RGBA", (W, W), achtergrond + (255,) if achtergrond else (0, 0, 0, 0))

    def poly(curven, per=20):
        uit = []
        for b in curven:
            for i in range(per):
                p = T(bezier_punt(b, i / float(per)))
                uit.append((p[0] * f, p[1] * f))
        return uit

    # banden + kite
    banden = Image.new("RGBA", (W, W), BLUW_D + (255,))
    db = ImageDraw.Draw(banden)
    cx = centrum_x * f
    db.rectangle([cx, 0, W, y_g1 * f], fill=BLUW_L + (255,))
    db.rectangle([0, y_g1 * f, cx, y_g2 * f], fill=GROEN_D + (255,))
    db.rectangle([cx, y_g1 * f, W, y_g2 * f], fill=GROEN_L + (255,))
    db.rectangle([0, y_g2 * f, cx, W], fill=GEEL_D + (255,))
    db.rectangle([cx, y_g2 * f, W, W], fill=GEEL_L + (255,))

    m = Image.new("L", (W, W), 0)
    ImageDraw.Draw(m).polygon(poly(kite_curven), fill=255)
    laag = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    laag.paste(banden, (0, 0), m)
    doek = Image.alpha_composite(doek, laag)

    # lint + vouwen
    for curven, kleur in [(lint_curven, GRIJS_L)] + [(c, GRIJS_D) for c in vouw_curven]:
        m = Image.new("L", (W, W), 0)
        ImageDraw.Draw(m).polygon(poly(curven), fill=255)
        laag = Image.new("RGBA", (W, W), kleur + (255,))
        tussen = Image.new("RGBA", (W, W), (0, 0, 0, 0))
        tussen.paste(laag, (0, 0), m)
        doek = Image.alpha_composite(doek, tussen)

    # verbodsbord
    rcx, rcy = RING_MIDDEN[0] * f, RING_MIDDEN[1] * f
    ro, ri = RING_BUITEN * f, (RING_BUITEN - RING_DIK) * f
    bord = Image.new("L", (W, W), 0)
    bd = ImageDraw.Draw(bord)
    bd.ellipse([rcx - ro, rcy - ro, rcx + ro, rcy + ro], fill=255)
    bd.ellipse([rcx - ri, rcy - ri, rcx + ri, rcy + ri], fill=0)
    half = STREEP_BREED * f / 2.0
    dx = 1.0 if streep_links_boven else -1.0
    lengte = RING_BUITEN * 1.5 * f
    bx1, by1 = rcx - dx * lengte * 0.7071, rcy - lengte * 0.7071
    bx2, by2 = rcx + dx * lengte * 0.7071, rcy + lengte * 0.7071
    # rechthoek langs de lijn, met de hand opgebouwd
    ux, uy = (bx2 - bx1), (by2 - by1)
    n = math.hypot(ux, uy)
    ux, uy = ux / n, uy / n
    px_, py_ = -uy, ux
    bd.polygon([(bx1 + px_ * half, by1 + py_ * half),
                (bx2 + px_ * half, by2 + py_ * half),
                (bx2 - px_ * half, by2 - py_ * half),
                (bx1 - px_ * half, by1 - py_ * half)], fill=255)
    masker_buiten = Image.new("L", (W, W), 0)
    ImageDraw.Draw(masker_buiten).ellipse([rcx - ro, rcy - ro, rcx + ro, rcy + ro], fill=255)
    bord = ImageChops.multiply(bord, masker_buiten)
    laag = Image.new("RGBA", (W, W), ROOD + (255,))
    tussen = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    tussen.paste(laag, (0, 0), bord)
    doek = Image.alpha_composite(doek, tussen)

    return doek.resize((maat, maat), Image.LANCZOS)


from PIL import ImageChops  # noqa: E402  (nodig voor teken_png)

if __name__ == "__main__":
    import os
    uit = "/home/user/family-stop-v2/design"
    os.makedirs(uit, exist_ok=True)

    open(uit + "/familystop-icoon.svg", "w").write(svg_voor(True))
    open(uit + "/familystop-icoon-streep-andersom.svg", "w").write(svg_voor(False))
    open(uit + "/familystop-icoon-vierkant.svg", "w").write(svg_voor(True, (255, 255, 255)))

    teken_png(True).save(uit + "/familystop-icoon-1024.png")
    teken_png(True, (255, 255, 255)).save(uit + "/familystop-icoon-vierkant-1024.png")
    teken_png(True).resize((512, 512), Image.LANCZOS).save(uit + "/familystop-icoon-512.png")
    teken_png(True, (255, 255, 255)).resize((512, 512), Image.LANCZOS).save(
        uit + "/familystop-icoon-vierkant-512.png")
    teken_png(False, (255, 255, 255)).resize((512, 512), Image.LANCZOS).save(
        uit + "/familystop-icoon-streep-andersom-512.png")
    print("bestanden geschreven naar", uit)
