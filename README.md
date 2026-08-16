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

## 5. Mailjet-instellingen invullen (voor e-mailmeldingen)

### Waarom Mailjet in plaats van SMTP?

Deze app verstuurde e-mailmeldingen aanvankelijk via SMTP, maar op het doeltoestel blokkeert de
netwerkfirewall uitgaand verkeer op de gangbare SMTP-poorten (25, 465, 587) — een veelvoorkomend
firewallbeleid. Daardoor werkte SMTP nooit, ongeacht welke host/poort/TLS-instelling je invulde
("Exception reading response" en vergelijkbare foutmeldingen). HTTPS (poort 443) is op dit
toestel wél toegestaan (anders zou niets anders in de app werken), dus verstuurt UnetworkMonitor
e-mail nu via de Mailjet Send API v3.1, een REST-API over gewoon HTTPS, in plaats van rechtstreeks
via SMTP.

### Mailjet-account voorbereiden

1. Maak een gratis account aan op [mailjet.com](https://www.mailjet.com). Controleer op de
   website zelf wat de actuele limieten van het gratis abonnement zijn (aantal e-mails per dag/
   maand) — dat kan in de tussentijd gewijzigd zijn.
2. **Verifieer het afzenderadres of -domein** dat je wilt gebruiken: ga naar
   Account Settings > Sender domains & addresses en volg de verificatiestappen (meestal een
   bevestigingsmail of een DNS-record, afhankelijk van of je een los adres of een heel domein
   verifieert). **Belangrijk:** Mailjet weigert e-mail te versturen vanaf een niet-geverifieerd
   afzenderadres, dus doe dit vóórdat je de testknop in de app gebruikt, anders krijg je een
   foutmelding die niets met de app zelf te maken heeft.
3. Ga naar Account Settings > REST API > API Key Management om je **API Key** en **Secret Key**
   te vinden (of een nieuwe sleutel aan te maken).

### Instellingen invullen in de app

Ga naar het tabblad "E-mail instellingen" en vul de volgende velden in:

| Veld | Omschrijving |
|---|---|
| API Key | Uit Mailjet: Account Settings > REST API > API Key Management |
| Secret Key | Idem — behandel dit als een wachtwoord |
| Afzenderadres | Het geverifieerde adres uit stap 2 hierboven |
| Afzendernaam (optioneel) | Bijvoorbeeld "UnetworkMonitor" — mag leeg blijven |
| Ontvangeradres | Het e-mailadres waar de meldingen naartoe moeten |

Tik op "Instellingen opslaan" en gebruik daarna "Test e-mail versturen" om te controleren of alles
correct is ingesteld. Het resultaat (gelukt/mislukt, inclusief de foutmelding van Mailjet zelf bij
een fout) verschijnt onderaan het scherm.

De Mailjet API Key en Secret Key worden op het toestel opgeslagen met `EncryptedSharedPreferences`,
dus versleuteld en nooit in platte tekst.

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
- **E-mail instellingen**: Mailjet-configuratie (API Key/Secret Key, afzender, ontvanger) met
  testknop. E-mail wordt verstuurd via HTTPS naar de Mailjet Send API, niet via SMTP.

## Herbouwen na wijzigingen

Elke keer dat je de broncode wijzigt, bouw je opnieuw met hetzelfde commando als in stap 1, en
installeer je de nieuwe APK opnieuw met `adb install -r ...` (de `-r` zorgt dat een bestaande
installatie wordt vervangen zonder gegevensverlies).
