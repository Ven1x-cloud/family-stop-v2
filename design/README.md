# Family Stop — logo en iconen

Het idee: het **echte Family Link-logo** (de vlieger met het lint) met een
**verbodsbord** erover. Zodat je in één oogopslag ziet waar de app over gaat.

## Wat waar zit

| Bestand | Waarvoor |
|---|---|
| `familystop-icoon.svg` | Het icoon, 512×512, **transparante achtergrond** — dit is het hoofdbestand |
| `familystop-icoon-vierkant.svg` | Zelfde, met witte achtergrond |
| `familystop-icoon-512.png` / `-1024.png` | Zelfde als PNG, transparant |
| `familystop-icoon-vierkant-512.png` / `-1024.png` | Zelfde als PNG, wit |
| `familystop-icoon-streep-andersom-512.png` | De schuine streep de andere kant op (zie hieronder) |
| `familystop-logo-256/512/1024.png` | Het logo op wit, zonder transparantie — voor documentatie |
| `familystop-logo-vrij-512.png` | Alleen het logo, strak bijgesneden, transparant |

De app gebruikt zelf vectorvarianten; die staan in
`OverlayRunnerGame2/app/src/main/res/drawable/`:

- `familystop_logo.xml` — het volledige icoon, gebruikt in het zwevende paneel en op de setup-pagina
- `ic_launcher_foreground.xml` — voorgrond van het app-icoon
- `ic_launcher_background.xml` — witte achtergrond
- `ic_launcher_monochrome.xml` — de vorm voor thematische iconen (Android 13+)

## Hoe het gemaakt is

Het Family Link-logo is **niet nagetekend met de hand**, maar **opgemeten uit een
referentieafbeelding** en omgezet in vectoren:

1. De contouren van de vlieger en het lint zijn sub-pixel nauwkeurig uitgelezen.
2. De vlieger bleek een ruit met vier rechte randen en vier **identieke ronde
   hoeken met straal 17,9** (gemeten: de afwijking van het model is gemiddeld
   0,066 pixel). Die is dus exact als vorm te beschrijven.
3. De banden zijn exact: verticale tweedeling op de middenas, horizontale
   scheidingen op y=113 en y=188.
4. Het lint is met een Bézier-fit gladgestreken, inclusief de twee donkere vouwen.

Controle: het nagemeten logo naast het origineel geeft een gemiddeld
kleurverschil van **1,5 per kanaal** — dat is alleen de randpixels.

Het verbodsbord volgt de ISO 3864-verhoudingen: de rode ring is 19% van de
straal dik, en de schuine balk is even dik als de ring.

## Over de richting van de streep

De internationale norm (ISO 3864) zegt: **van linksboven naar rechtsonder** —
dat is de versie die standaard gebruikt wordt. In jouw voorbeeld liep de streep
de andere kant op (van rechtsboven naar linksonder). Ik heb de norm-versie als
hoofdbestand genomen, en jouw variant zit er als `-streep-andersom` bij.

Wil je toch die andere richting? Dan is het één regel in de generator.

## Zelf aanpassen

De vorm staat in `/home/user/logo-work/` (het script `icoon.py` bepaalt de
maten, `assets.py` maakt de Android-bestanden). De belangrijkste knoppen:

```python
RING_MIDDEN = (256.0, 250.0)   # waar de ring zit
RING_BUITEN = 232.0            # buitenstraal
RING_DIK = 44.0                # dikte van de ring
ROOD = (217, 48, 37)           # #D93025
```
