"""Genereer de Android-iconen en het in-app logo uit dezelfde vorm."""
from PIL import Image, ImageDraw, ImageChops
import os, sys

sys.path.insert(0, "/home/user/logo-work")
import icoon as I

RES = "/home/user/family-stop-v2/OverlayRunnerGame2/app/src/main/res"
ONTWERP = "/home/user/family-stop-v2/design"

ROOD = "#D93025"
GRIJS_L = "#DADCE0"
GRIJS_D = "#BDC1C6"

# ---------------------------------------------------------------- bbox
alle = I.kite_curven + I.lint_curven + [c for v in I.vouw_curven for c in v]
punten = [p for b in alle for p in b]
xs = [p[0] for p in punten] + [I.RING_MIDDEN[0] - I.RING_BUITEN, I.RING_MIDDEN[0] + I.RING_BUITEN]
ys = [p[1] for p in punten] + [I.RING_MIDDEN[1] - I.RING_BUITEN, I.RING_MIDDEN[1] + I.RING_BUITEN]
X0, X1, Y0, Y1 = min(xs), max(xs), min(ys), max(ys)
print("inhoud-bbox: %.1f,%.1f .. %.1f,%.1f" % (X0, Y0, X1, Y1))


# ---------------------------------------------------------------- paden
def cirkel(cx, cy, r):
    return ("M %.2f %.2f a %.2f %.2f 0 1 0 %.2f 0 a %.2f %.2f 0 1 0 -%.2f 0 Z"
            % (cx - r, cy, r, r, 2 * r, r, r, -2 * r))


CX, CY = I.RING_MIDDEN
RO, RI = I.RING_BUITEN, I.RING_BUITEN - I.RING_DIK
RING = cirkel(CX, CY, RO) + " " + cirkel(CX, CY, RI)
BUITEN = cirkel(CX, CY, RO)

L = RO * 1.5
H = I.STREEP_BREED / 2.0 * 0.7071
BALK = ("M %.2f %.2f L %.2f %.2f L %.2f %.2f L %.2f %.2f Z"
        % (CX - L * 0.7071 - H, CY - L * 0.7071 + H,
           CX + L * 0.7071 - H, CY + L * 0.7071 + H,
           CX + L * 0.7071 + H, CY + L * 0.7071 - H,
           CX - L * 0.7071 + H, CY - L * 0.7071 - H))

BANDEN = [
    ("M -600 -600 H %0.2f V %0.2f H -600 Z" % (CX, I.y_g1), "#4285F4"),
    ("M %0.2f -600 H 1200 V %0.2f H %0.2f Z" % (CX, I.y_g1, CX), "#669DF6"),
    ("M -600 %0.2f H %0.2f V %0.2f H -600 Z" % (I.y_g1, CX, I.y_g2), "#34A853"),
    ("M %0.2f %0.2f H 1200 V %0.2f H %0.2f Z" % (CX, I.y_g1, I.y_g2, CX), "#5BB974"),
    ("M -600 %0.2f H %0.2f V 1200 H -600 Z" % (I.y_g2, CX), "#FBBC04"),
    ("M %0.2f %0.2f H 1200 V 1200 H %0.2f Z" % (CX, I.y_g2, CX), "#FCC934"),
]


def vector_xml(viewport_breedte_dp, transform=None, monochroom=False):
    """Een VectorDrawable met het volledige logo. viewport is altijd 512."""
    if monochroom:
        binnen = [
            '<path android:pathData="%s" android:fillColor="#000000"/>' % I.kite_pad,
            '<path android:pathData="%s" android:fillColor="#000000" '
            'android:fillType="evenOdd"/>' % RING,
            '<group>',
            '  <clip-path android:pathData="%s"/>' % BUITEN,
            '  <path android:pathData="%s" android:fillColor="#000000"/>' % BALK,
            '</group>',
        ]
    else:
        binnen = ['<group>', '  <clip-path android:pathData="%s"/>' % I.kite_pad]
        for d, kleur in BANDEN:
            binnen.append('  <path android:pathData="%s" android:fillColor="%s"/>' % (d, kleur))
        binnen.append('</group>')
        binnen.append('<path android:pathData="%s Z" android:fillColor="%s"/>'
                      % (I.lint_pad, GRIJS_L))
        for v in I.vouw_paden:
            binnen.append('<path android:pathData="%s Z" android:fillColor="%s"/>' % (v, GRIJS_D))
        binnen.append('<path android:pathData="%s" android:fillColor="%s" '
                      'android:fillType="evenOdd"/>' % (RING, ROOD))
        binnen.append('<group>')
        binnen.append('  <clip-path android:pathData="%s"/>' % BUITEN)
        binnen.append('  <path android:pathData="%s" android:fillColor="%s"/>' % (BALK, ROOD))
        binnen.append('</group>')

    uit = ['<?xml version="1.0" encoding="utf-8"?>',
           '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
           '    android:width="%ddp"' % viewport_breedte_dp,
           '    android:height="%ddp"' % viewport_breedte_dp,
           '    android:viewportWidth="%d"' % int(I.DOEK),
           '    android:viewportHeight="%d">' % int(I.DOEK)]
    if transform:
        uit += ['    <group',
                '        android:scaleX="%.6f"' % transform["s"],
                '        android:scaleY="%.6f"' % transform["s"],
                '        android:translateX="%.3f"' % transform["tx"],
                '        android:translateY="%.3f"' % transform["ty"],
                '        android:pivotX="0" android:pivotY="0">']
        uit += ["      " + r for r in binnen]
        uit.append('    </group>')
    else:
        uit += ["    " + r for r in binnen]
    uit.append('</vector>')
    return "\n".join(uit) + "\n"


# ---------------------------------------------------------------- PNG-iconen
def logo_plaat(maat, buiten_beeld=True):
    """Het volledige logo (met ring) als PIL-beeld, precies passend."""
    basis = I.teken_png(True, None, maat=1024)
    bbox = basis.getbbox()
    kern = basis.crop(bbox)
    return kern.resize((maat, maat), Image.LANCZOS)


def png_met_plaat(maat, vorm):
    ss = 3
    W = maat * ss
    doek = Image.new("RGBA", (W, W), (0, 0, 0, 0))

    plaat = Image.new("L", (W, W), 0)
    pd = ImageDraw.Draw(plaat)
    if vorm == "rond":
        pd.ellipse([0, 0, W - 1, W - 1], fill=255)
    else:
        pd.rounded_rectangle([0, 0, W - 1, W - 1], radius=int(W * 0.2), fill=255)

    wit = Image.new("RGBA", (W, W), (255, 255, 255, 255))
    tussen = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    tussen.paste(wit, (0, 0), plaat)
    doek = Image.alpha_composite(doek, tussen)

    kern = logo_plaat(W)
    marge = int(W * 0.07)
    kern = kern.resize((W - 2 * marge, W - 2 * marge), Image.LANCZOS)
    doek.alpha_composite(kern, (marge, marge))

    doek.putalpha(ImageChops.multiply(doek.getchannel("A"), plaat))
    return doek.resize((maat, maat), Image.LANCZOS)


if __name__ == "__main__":
    # ---------------- het logo in de app
    open(RES + "/drawable/familystop_logo.xml", "w").write(vector_xml(512))

    # ---------------- adaptief icoon
    span = max(X1 - X0, Y1 - Y0)
    # het veilige gebied van een adaptief icoon is 66dp van de 108dp;
    # de viewport is 512, dus reken dat om naar viewport-eenheden
    veilig = 66.0 / 108.0 * I.DOEK
    s = veilig / span
    tx = I.DOEK / 2.0 - (X0 + X1) / 2.0 * s
    ty = I.DOEK / 2.0 - (Y0 + Y1) / 2.0 * s
    print("adaptief: schaal %.5f  translate (%.2f, %.2f)" % (s, tx, ty))
    open(RES + "/drawable/ic_launcher_foreground.xml", "w").write(
        vector_xml(108, {"s": s, "tx": tx, "ty": ty}))
    open(RES + "/drawable/ic_launcher_monochrome.xml", "w").write(
        vector_xml(108, {"s": s, "tx": tx, "ty": ty}, monochroom=True))
    open(RES + "/drawable/ic_launcher_background.xml", "w").write(
        '<?xml version="1.0" encoding="utf-8"?>\n'
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
        '    android:width="108dp"\n    android:height="108dp"\n'
        '    android:viewportWidth="108"\n    android:viewportHeight="108">\n'
        '    <path android:pathData="M0,0h108v108h-108z" android:fillColor="#FFFFFF"/>\n'
        '</vector>\n')

    # ---------------- legacy mipmaps
    for naam, maat in {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}.items():
        map_ = "%s/mipmap-%s" % (RES, naam)
        os.makedirs(map_, exist_ok=True)
        png_met_plaat(maat, "vierkant").save(map_ + "/ic_launcher.png", optimize=True)
        png_met_plaat(maat, "rond").save(map_ + "/ic_launcher_round.png", optimize=True)
    print("mipmaps geschreven")

    # ---------------- losse bestanden voor het ontwerpmapje
    os.makedirs(ONTWERP, exist_ok=True)
    for maat in (256, 512, 1024):
        I.teken_png(True, (255, 255, 255), maat=maat).save(
            "%s/familystop-logo-%d.png" % (ONTWERP, maat))
    logo_plaat(512).save(ONTWERP + "/familystop-logo-vrij-512.png")
    print("klaar")
