# Family Stop

Een zwevend paneel bovenop je telefoon, op te roepen met de toegankelijkheidsknop.
Met een eigen browser, een eigen toetsenbord en een snelkoppeling naar de kluis.

## Wat er nieuw is in deze versie

### Staand: ronde kaart
Het paneel is geen blokkerige rechthoek meer, maar een kaart met ronde hoeken,
dun randje en zachte schaduw. Alle knoppen hebben ronde hoeken en tik-rimpel.
Kleuren staan in `res/values/colors.xml` (alles met `fs_` ervoor), dus je kunt
het hele uiterlijk vanuit één bestand aanpassen.

### Liggend in de browser: volledig scherm + sleepbaar knopje
Dit is de grote verandering:

- **Alle knoppen verdwijnen.** Geen menu, geen adresbalk, geen toetsenbord.
- **De browser gaat van rand tot rand.** Ook onder de statusbalk door.
- **Er blijft één klein knopje over** (standaard rechtsboven). Dat kun je
  verslepen; bij loslaten springt het naar de dichtstbijzijnde hoek.
- **Tik op dat knopje** → klein uitklapmenu met:
  - **← Terug** — een pagina terug in de browser
  - **Hoofdmenu** — terug naar het menu (en dus uit volledig scherm)
  - **Toetsenbord** — adresbalk + toetsenbord tevoorschijn halen om te typen
  - **× Sluiten** — het hele paneel dicht

Draai je de telefoon terug naar staand, dan wordt het weer een kaart.

> **Nooduitgang:** je kunt het paneel altijd sluiten met de toegankelijkheidsknop,
> ook als je het sleepbare knopje even niet kunt vinden.

### Overige verbeteringen
- De browserhoogte is niet meer hardgecodeerd op 1200 pixels (dat liep op een
  A55 van het scherm af). Alles is nu dp en volgt het scherm.
- Links die in een nieuw tabblad openen werken nu wel (via `WebChromeClient`).
- Caching staat aan in plaats van uit — pagina's laden sneller.
- `onUnbind` / `onDestroy` ruimen het venster netjes op.
- Zoekopdrachten worden netjes URL-encoded.
- Alle teksten staan in `res/values/strings.xml`.
- De setup-pagina laat per stap zien of hij klaar is, en legt de
  "Beperkte instellingen"-stap van Android 13+ uit.

## Draaien

Open de map `OverlayRunnerGame2` in Android Studio en druk op Run.

Op het toestel:
1. **Zwevend paneel toestaan** — tik op stap 1
2. **Toegankelijkheid aanzetten** — tik op stap 2. Staat de schakelaar op slot?
   Tik dan op de grijze uitleg onderaan de setup-pagina, kies de drie puntjes
   rechtsboven en dan "Beperkte instellingen toestaan".
3. **Secure Folder** — alleen als je die op je toestel hebt.

Daarna: open het paneel met de toegankelijkheidsknop, kies GAMES & INTERNET en
draai je telefoon liggend.

## Nog te doen

- Het eigen toetsenbord is nog steeds nodig omdat het venster
  `FLAG_NOT_FOCUSABLE` is. Een volgende stap is om het venster tijdelijk
  focusbaar te maken, zodat het echte systeemtoetsenbord werkt.
- Projectnaam en package staan nog op `com.example.overlayrunnergame`.
