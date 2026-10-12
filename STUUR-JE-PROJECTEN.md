# Je Android-projecten naar GitHub sturen (zodat ik ze kan lezen)

**Belangrijk om te weten:** je echte broncode is klein. Van die 4 projecten in
`AndroidStudioProjects` is alles bij elkaar **~700 KB code**. De 31 MB die in je oude
uploads zat was `build/`, `.gradle/` en `.idea/` — rommel die je nooit hoeft te sturen.
De scripts hieronder filteren dat automatisch weg.

Open **PowerShell** (Win-toets → "PowerShell" → Enter) en plak een van de blokken hieronder.
Alles is één keer plakken en klaar.

---

## Route A — Geen git, alleen een zip (snelst)

Plak dit. Het maakt een zip met **alleen je broncode** op je bureaublad.

```powershell
$bron  = "C:\Users\126881\AndroidStudioProjects"
$tmp   = "$env:TEMP\projexport"
$zip   = "$env:USERPROFILE\Desktop\android-projecten.zip"
$skipD = "build",".gradle",".idea",".kotlin",".git","captures",".cxx",".externalNativeBuild"
$skipF = "local.properties","*.jks","*.keystore","*.apk","*.aab","*.hprof"

if (Test-Path $tmp) { Remove-Item $tmp -Recurse -Force }
New-Item -ItemType Directory $tmp | Out-Null
Get-ChildItem $bron -Directory | ForEach-Object {
    robocopy $_.FullName "$tmp\$($_.Name)" /E /XD $skipD /XF $skipF /NFL /NDL /NJH /NJS /NP | Out-Null
    Write-Host "toegevoegd: $($_.Name)"
}
Compress-Archive "$tmp\*" $zip -Force
Write-Host "`nKlaar! Hang dit bestand in de chat: $zip" -ForegroundColor Green
```

Daarna: sleep `android-projecten.zip` van je bureaublad in de chat. Klaar.

> Wil je maar 4 mappen in plaats van alles? Vervang dan de `Get-ChildItem`-regel door:
> ```powershell
> @("MyApplication","noodmelding","OverlayRunnerGame","OverlayRunnerGame2") | ForEach-Object {
>     robocopy "$bron\$_" "$tmp\$_" /E /XD $skipD /XF $skipF /NFL /NDL /NJH /NJS /NP | Out-Null
> }
> ```
> (zet je eigen mapnamen erin)

---

## Route B — Via git naar een **nieuwe** repo

### Stap 1 (30 seconden, op de website)
Ga naar <https://github.com/new> → naam: `android-projects` → **Create repository**
(vink "Add a README" uit). Laat de pagina daarna staan, die heb je niet meer nodig.

### Stap 2 — dit plakken in PowerShell

```powershell
$bron  = "C:\Users\126881\AndroidStudioProjects"
$stage = "$env:USERPROFILE\android-projects-export"
$repo  = "https://github.com/Ven1x-cloud/android-projects.git"
$skipD = "build",".gradle",".idea",".kotlin",".git","captures",".cxx",".externalNativeBuild"
$skipF = "local.properties","*.jks","*.keystore","*.apk","*.aab","*.hprof"

# schone kopie van al je projecten (zonder build-rommel)
if (Test-Path $stage) { Remove-Item $stage -Recurse -Force }
New-Item -ItemType Directory $stage | Out-Null
Get-ChildItem $bron -Directory | ForEach-Object {
    robocopy $_.FullName "$stage\$($_.Name)" /E /XD $skipD /XF $skipF /NFL /NDL /NJH /NJS /NP | Out-Null
    Write-Host "toegevoegd: $($_.Name)"
}

# naar GitHub pushen
cd $stage
git init
git add .
git commit -m "Alle Android Studio projecten (zonder build-bestanden)"
git branch -M main
git remote add origin $repo
git push -u origin main
```

De eerste keer vraagt hij om in te loggen: er opent een browservenster
("Sign in with your browser") → inloggen met je GitHub-account → klaar.

**Daarna:** zeg het tegen mij, dan haal ik de repo op en kan ik alles lezen.

> Doe je later nog een keer een push (nieuw project erbij)? Dan is het genoeg om:
> ```powershell
> cd "$env:USERPROFILE\android-projects-export"
> git add . ; git commit -m "update" ; git push
> ```
> (wel eerst stap "schone kopie" opnieuw draaien, anders mist het nieuwe project)

---

## Route C — Via git naar de repo die we al gebruiken (geen website nodig)

Dit zet je projecten op een **aparte branch** `android-projects` in
`Ven1x-cloud/family-stop-v2`. Die branch raakt de app-code niet aan, ik lees er alleen uit.

```powershell
$bron  = "C:\Users\126881\AndroidStudioProjects"
$stage = "$env:USERPROFILE\android-projects-export"
$repo  = "https://github.com/Ven1x-cloud/family-stop-v2.git"
$skipD = "build",".gradle",".idea",".kotlin",".git","captures",".cxx",".externalNativeBuild"
$skipF = "local.properties","*.jks","*.keystore","*.apk","*.aab","*.hprof"

if (Test-Path $stage) { Remove-Item $stage -Recurse -Force }
New-Item -ItemType Directory $stage | Out-Null
Get-ChildItem $bron -Directory | ForEach-Object {
    robocopy $_.FullName "$stage\$($_.Name)" /E /XD $skipD /XF $skipF /NFL /NDL /NJH /NJS /NP | Out-Null
    Write-Host "toegevoegd: $($_.Name)"
}

cd $stage
git init
git add .
git commit -m "Alle Android Studio projecten (zonder build-bestanden)"
git branch -M main
git remote add origin $repo
git push origin main:android-projects
```

---

## Als `git` niet gevonden wordt

Android Studio heeft git ingebouwd. Vind het pad via:
**Android Studio → Settings → Version Control → Git** (daar staat "Path to Git executable").
Gebruik dat pad in plaats van `git`, bijvoorbeeld:

```powershell
& "C:\Program Files\Android\Android Studio\bin\git.exe" --version
```

Of installeer Git voor Windows: <https://git-scm.com/download/win> (standaardopties aanhouden).

---

## Wat er bewust **niet** meegaat

| Weggefilterd | Waarom |
|---|---|
| `build/`, `.gradle/`, `.kotlin/`, `.cxx/`, `captures/` | honderden MB aan gegenereerde troep |
| `.idea/` | editor-instellingen, andere pc = onbruikbaar |
| `local.properties` | bevat jouw pc-specifieke SDK-pad |
| `*.jks`, `*.keystore` | **je app-ondertekeningssleutels** — die horen nooit in een publieke repo |
| `*.apk`, `*.aab` | kant-en-klare builds, geen broncode |
| `.git` van elk project | anders wordt het een "nested repo"; je eigen historie blijft gewoon staan in `AndroidStudioProjects` |

Je originele mappen in `AndroidStudioProjects` blijven **volledig onaangeroerd** —
dit werkt op een kopie in `%USERPROFILE%\android-projects-export`.
