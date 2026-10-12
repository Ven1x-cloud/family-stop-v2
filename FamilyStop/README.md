# Family Stop

**Dit is de map die je in Android Studio opent.** Kies in Android Studio:
`File → Open…` en selecteer deze map (`FamilyStop`). *Niet* de map eromheen.

Een zwevend paneel bovenop je telefoon, op te roepen met de
toegankelijkheidsknop. Met een eigen browser, een eigen toetsenbord en een
snelkoppeling naar de kluis.

- **Package:** `nl.ven1x.familystop`
- **minSdk 24 · targetSdk 36**
- **Taal:** Java
- **Vereist:** Android Studio (nieuw genoeg voor AGP 9.2.1 / Gradle 9.4.1)

---

## Naar je telefoon krijgen

### Manier 1 — de Run-knop (makkelijkst)

1. **Zet USB-foutopsporing aan op je A55:**
   `Instellingen → Over je telefoon → Software-informatie → tik 7× op Buildnummer`
   Dan terug naar `Instellingen → Ontwikkelaarsopties → USB-foutopsporing` aanzetten.
2. **Sluit je telefoon met een USB-kabel aan.** Op je telefoon verschijnt
   "USB-foutopsporing toestaan?" → **Toestaan**.
3. **Klik in Android Studio op de groene ▶ Run-knop** (of druk `Shift+F10`).

Android Studio bouwt de app en zet hem direct op je telefoon. Meestal opent hij
daarna meteen. Zo niet: zoek het icoon **Family Stop** in je app-lade.

### Manier 2 — met de opdrachtregel

Open de **Terminal** in Android Studio (onderaan) of een PowerShell-venster in
deze map. Het `gradlew.bat`-bestand staat hier al.

```powershell
# 1. controleren of je telefoon gezien wordt
adb devices

# 2. de app bouwen én direct installeren
.\gradlew.bat installDebug
```

Wat je wilt zien:

```
List of devices attached
R58XXXXXXXX     device          <- jouw A55, moet "device" zeggen
...
BUILD SUCCESSFUL
```

Zegt `adb` dat het commando niet bestaat? Gebruik dan het `adb.exe` dat bij
Android Studio zit:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" devices
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk
```

### Manier 3 — APK bouwen en zelf overzetten

```powershell
.\gradlew.bat assembleDebug
```

De APK staat dan in `app\build\outputs\apk\debug\app-debug.apk`. Die kun je
mailen, via Google Drive zetten, of met een kabel overzetten en op je telefoon
aantikken (dan moet je "Onbekende bronnen" toestaan).

### Manier 4 — zonder kabel (draadloos)

Op Android 11 en nieuwer:

```powershell
adb pair 192.168.x.x:PORT      # de code staat op je telefoon
adb connect 192.168.x.x:PORT
.\gradlew.bat installDebug
```

(Pairing vind je op je A55 onder `Ontwikkelaarsopties → Draadloos foutopsporing`.)

---

## Na de installatie — de app instellen

Het paneel hangt aan de toegankelijkheidsdienst van Android. Er is één stap die
je moet zetten:

1. **Toegankelijkheid aanzetten.** Open de app, tik op stap 1, zet
   *Family Stop* aan en bevestig het venster.

**Zet de schakelaar niet om?** Bij een zelf geïnstalleerde app staat die op
Android 13 en nieuwer standaard op slot. Tik op de grijze uitleg onderaan de
setup-pagina → de drie puntjes rechtsboven → **Beperkte instellingen toestaan**.
Daarna lukt stap 1 wel.

2. **Kluis** (optioneel). Opent Samsung Secure Folder als die er is. Onze eigen
   kluis komt later.

Daarna: open het paneel met de **toegankelijkheidsknop** (die rondzwevende knop
die je in `Instellingen → Toegankelijkheid` aanzet). Het paneel komt tevoorschijn
over alles heen wat je open hebt.

---

## Wat er in deze versie zit

### Staand: ronde kaart
Het paneel is een kaart met ronde hoeken, dun randje en zachte schaduw. Alle
knoppen hebben ronde hoeken en een tik-rimpel. Kleuren staan in
`app/src/main/res/values/colors.xml` (alles met `fs_` ervoor), teksten in
`strings.xml`.

### Liggend in de browser: volledig scherm + sleepbaar knopje
- Alle knoppen verdwijnen; de browser gaat van rand tot rand, ook onder de
  statusbalk door.
- Er blijft één klein knopje over dat je kunt **verslepen**; bij loslaten springt
  het naar de dichtstbijzijnde hoek.
- **Tik erop** voor: `← Terug`, `Hoofdmenu`, `Toetsenbord`, `× Sluiten`.
- Draai je terug naar staand, dan wordt het weer een kaart.

> Nooduitgang: met de toegankelijkheidsknop sluit je het paneel altijd, ook als
> je het sleepbare knopje even niet kunt vinden.

### Het logo
Het Family Link-logo met een verbodsbord erover, in `design/` in de hoofdmap van
de repo, en als vector in `app/src/main/res/drawable/`.

### Wat er gerepareerd is
- De browserhoogte was hardgecodeerd op 1200 **pixels** (geen dp) — dat liep op
  een A55 van het scherm af. Nu volgt alles het scherm.
- `shouldOverrideUrlLoading(WebView, String)` wordt op Android 7+ nooit meer
  aangeroepen, dus links die in een nieuw tabblad openen deden niets. Opgelost
  met de `WebResourceRequest`-variant plus `WebChromeClient`.
- Caching stond uit (`LOAD_NO_CACHE`) — nu `LOAD_DEFAULT`, dus sneller.
- Er werd nooit opgeruimd: `onUnbind`/`onDestroy` halen het venster nu weg.
- Zoekopdrachten worden URL-encoded.
- De theme werd nergens gebruikt (het manifest had geen `android:theme`).
- De setup-pagina telde drie verplichte stappen, waarvan er één (Secure Folder)
  op een toestel zonder die functie nooit groen kon worden. Nu is er één
  verplichte stap en is de kluis optioneel.

---

## Bestandsindeling

```
FamilyStop/
├── app/src/main/java/nl/ven1x/familystop/
│   ├── MainActivity.java          de setup-pagina
│   ├── FamilyStopService.java     het zwevende paneel + browser + toetsenbord
│   └── Ui.java                    helpers: ronde vormen, dp, tik-rimpels
├── app/src/main/res/
│   ├── drawable/                  het logo als vector + app-icoon
│   ├── layout/activity_main.xml   de setup-pagina
│   ├── values/colors.xml          het hele kleurenpalet
│   ├── values/strings.xml         alle teksten
│   └── xml/accessibility_config.xml
└── app/build.gradle.kts
```

---

## Nog te doen

- Onze eigen kluis in plaats van de Samsung Secure Folder-knop
- Melding als de dienst uitvalt, met één tik naar de instelling
- Tabs, bladwijzers en geschiedenis in de browser
- Echt systeemtoetsenbord in plaats van het namaak-toetsenbord (het venster is
  nu `FLAG_NOT_FOCUSABLE`, daarom bestaat dat toetsenbord)
