# family-stop-v2

Werkmap voor de **Family Stop**-app.

## Wat zit waar

| Map / bestand | Wat het is |
|---|---|
| **`FamilyStop/`** | **De app. Deze map open je in Android Studio.** |
| `design/` | Het logo en de iconen (SVG + PNG) plus de scripts waarmee ze gemaakt zijn |
| `UPGRADE-PLAN.md` | Analyse van de oorspronkelijke app en het plan om hem te moderniseren |
| `STUUR-JE-PROJECTEN.md` | Hoe je je Android Studio-projecten naar GitHub krijgt |
| `MyApplication/`, `noodmelding/` | Je twee andere projecten, nog ongewijzigd |
| `main` (branch) | De originele zip-bestanden van je eerste upload |

## Snelle start

```
File → Open… → kies de map FamilyStop
```

Daarna: USB-foutopsporing aanzetten op je telefoon, kabel erin, groene ▶ Run-knop.
Zonder Android Studio:

```powershell
cd FamilyStop
.\gradlew.bat installDebug
```

Volledige uitleg staat in `FamilyStop/README.md`.

## Branches

| Branch | Waar het voor is |
|---|---|
| `main` | De originele uploads (de vier zip-bestanden) |
| `android-projects` | De uitgepakte broncode zoals je hem instuurde |
| `arena/b6a68250-family-stop-v2` | Waar ik aan werk; hier staan alle wijzigingen |
