# UnetworkMonitor

UnetworkMonitor is een native Android-app (Kotlin, Jetpack Compose) die via een
Toegankelijkheidsservice (Accessibility Service) de tekst leest die zichtbaar is op het scherm,
deze vergelijkt met een lijst zelf ingestelde trefwoorden, en bij een match een lokale melding
en/of e-mail stuurt.

Deze app wordt **niet** gepubliceerd op de Play Store. Hij is uitsluitend bedoeld om via `adb
install` te sideloaden op een apparaat.

## Belangrijk: gebruik alleen op eigen of geautoriseerde apparatuur

UnetworkMonitor kan de tekst lezen van andere apps op het toestel waarop hij draait. Installeer en
gebruik deze app **uitsluitend** op een apparaat dat je zelf bezit, of waarvoor je expliciete
toestemming hebt van de eigenaar/gebruiker om het te monitoren. Het zonder toestemming lezen van
schermtekst van een apparaat van iemand anders kan in strijd zijn met de privacywetgeving (o.a.
AVG/GDPR) en mogelijk strafbaar zijn. De app toont daarom bij de eerste start altijd een
toestemmingsscherm, heeft een normaal, zichtbaar app-icoon en verbergt zichzelf nooit.

## 1. De APK bouwen

Vereisten (al aanwezig op deze machine):
- Android SDK in `/Users/tvr/Library/Android/sdk` (platform android-34, build-tools 34.0.0+)
- Gradle 8.7 wrapper-distributie (al gecachet), Android Gradle Plugin 8.4.2, Kotlin 1.9.24
- JDK: de JBR die met Android Studio is meegeleverd (er is geen los JDK op dit systeem)

Bouw de debug-APK met het volgende commando vanuit de projectroot
(`/Users/tvr/Dev/UnetworkCheck`):

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug
```

Na een succesvolle build staat de APK hier:

```
app/build/outputs/apk/debug/app-debug.apk
```

## 2. Installeren op het toestel

Sluit het Android-toestel aan via USB, schakel USB-debugging in (Instellingen > Over telefoon > 7x
op buildnummer tikken om ontwikkelaarsopties te activeren, daarna Instellingen >
Ontwikkelaarsopties > USB-foutopsporing), en installeer met:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Open daarna de app "UnetworkMonitor" vanuit de app-lijst. Bij de eerste start verschijnt het
toestemmingsscherm; lees dit en tik op "Ik ga akkoord" om verder te gaan.

## 3. Toegankelijkheidspermissie handmatig verlenen

Android staat apps niet toe om de Toegankelijkheidsservice zelf te activeren; dit moet je
handmatig doen:

1. Open de app UnetworkMonitor en ga naar het tabblad "Status".
2. Tik op de knop "Open toegankelijkheidsinstellingen" (dit opent
   Instellingen > Toegankelijkheid van Android).
3. Zoek in de lijst met services naar "UnetworkMonitor" (soms staat dit onder een kopje als
   "Gedownloade apps" of "Extra services").
4. Tik op "UnetworkMonitor" en schakel de service in.
5. Android toont een waarschuwing over de rechten die de service krijgt (o.a. het bekijken van de
   inhoud van het scherm) — dit is verwacht gedrag voor deze app. Bevestig om door te gaan.
6. Ga terug naar UnetworkMonitor; het statusscherm toont nu "Toegankelijkheidsservice is
   ingeschakeld".

## 4. Notificatiepermissie verlenen (Android 13 en hoger)

1. Ga in de app naar het tabblad "Status".
2. Tik op de knop "Meldingen toestaan".
3. Bevestig de systeemdialoog die om toestemming vraagt voor meldingen.

Op Android 12 en lager is deze aparte toestemming niet nodig; meldingen werken dan direct.

## 5. SMTP-instellingen invullen (voor e-mailmeldingen)

Ga naar het tabblad "E-mail instellingen" en vul de volgende velden in:

| Veld | Omschrijving |
|---|---|
| SMTP-server (host) | Bijvoorbeeld `smtp.gmail.com` |
| Poort | Bijvoorbeeld `587` (TLS) |
| Gebruikersnaam | Je volledige e-mailadres |
| App-wachtwoord | Zie hieronder — gebruik nooit je normale wachtwoord |
| Afzenderadres | Meestal hetzelfde als de gebruikersnaam |
| Ontvangeradres | Het e-mailadres waar de meldingen naartoe moeten |
| Gebruik TLS | Aan laten staan voor de meeste providers (incl. Gmail op poort 587) |

Tik op "Instellingen opslaan" en gebruik daarna "Test e-mail versturen" om te controleren of alles
correct is ingesteld. Het resultaat (gelukt/mislukt) verschijnt onderaan het scherm.

### Voorbeeld: Gmail-App-wachtwoord aanmaken

Gmail staat vanwege beveiliging niet toe om je gewone accountwachtwoord te gebruiken voor apps als
deze. In plaats daarvan maak je een "App-wachtwoord" aan:

1. Zorg dat tweestapsverificatie is ingeschakeld op je Google-account
   (myaccount.google.com > Beveiliging > Tweestapsverificatie).
2. Ga naar myaccount.google.com > Beveiliging > App-wachtwoorden
   (rechtstreekse link: https://myaccount.google.com/apppasswords).
3. Kies een naam voor de app-wachtwoord, bijvoorbeeld "UnetworkMonitor", en klik op "Aanmaken".
4. Google toont een wachtwoord van 16 tekens (zonder spaties bij het invullen). Kopieer dit.
5. Vul in UnetworkMonitor bij "SMTP-server" `smtp.gmail.com` in, poort `587`, TLS aan,
   "Gebruikersnaam" is je volledige Gmail-adres, en "App-wachtwoord" is het gekopieerde
   wachtwoord van stap 4.
6. Afzenderadres en ontvangeradres kun je beide op je eigen Gmail-adres zetten (of een ander
   ontvangeradres naar keuze).

De SMTP-gegevens worden op het toestel opgeslagen met `EncryptedSharedPreferences`, dus
versleuteld en nooit in platte tekst.

## 6. Taal van de app

De app toont voorlopig altijd **Engels**, ongeacht de systeemtaal van het toestel. Er is voor nu
geen taalkeuze zichtbaar in de app. Onder de motorkap zijn de Nederlandse vertalingen en de
infrastructuur voor het wisselen van taal (`AppCompatDelegate`, `values-nl/strings.xml`,
`locales_config.xml`) wel aanwezig, zodat een taalkeuzemenu later eenvoudig weer ingeschakeld kan
worden (in `SettingsScreen.kt`, via de `LANGUAGE_PICKER_ENABLED`-vlag) zonder verdere
herbouwwerkzaamheden.

## Overzicht van de functionaliteit

- **Toestemmingsscherm**: verschijnt eenmalig bij de eerste start.
- **Status**: toont of de Toegankelijkheidsservice en meldingspermissie zijn ingeschakeld, met
  knoppen om deze te activeren.
- **Trefwoorden**: lijst van regels (`KeywordRule`) met toevoegen/bewerken/verwijderen. Elke regel
  heeft een trefwoord, overeenkomstmodus (Bevat / Bevat NIET), hoofdlettergevoeligheid,
  aan/uit-schakelaar, meldingskanalen (lokaal/e-mail), optioneel app-pakketfilter en een
  afkoelperiode in minuten. "Bevat NIET"-regels vereisen een specifiek app-pakket. Een trefwoord
  mag eenvoudige jokertekens bevatten: `*` voor een willekeurige reeks tekens (ook geen) en `?`
  voor precies één teken — bijvoorbeeld `koop*bitcoin` vindt ook "koop nu snel bitcoin". Een
  trefwoord zonder jokertekens werkt zoals voorheen als gewone tekstovereenkomst.
- **Meldingenlog**: chronologisch overzicht van alle gevonden trefwoorden, met tijdstip, app en
  tekstfragment.
- **E-mail instellingen**: SMTP-configuratie met testknop.

## Herbouwen na wijzigingen

Elke keer dat je de broncode wijzigt, bouw je opnieuw met hetzelfde commando als in stap 1, en
installeer je de nieuwe APK opnieuw met `adb install -r ...` (de `-r` zorgt dat een bestaande
installatie wordt vervangen zonder gegevensverlies).
