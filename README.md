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

### Instellingen overzetten naar een ander apparaat

Onderaan het "E-mail instellingen"-scherm staat een sectie om deze Mailjet-configuratie tussen je
eigen apparaten over te zetten zonder de API Key en Secret Key opnieuw over te typen:

1. Tik op het ene apparaat op **"Genereer code (kopieert naar klembord)"**. Dit zet een code met
   het voorvoegsel `UMJMAIL1:` op het klembord en toont 'm ook in een tekstveld om handmatig te
   kopiëren.
2. Plak deze code op het andere apparaat in het veld **"Plak hier de code"** en tik op
   **"Importeren"**. De velden hierboven (API Key, Secret Key, afzender, afzendernaam, ontvanger)
   worden dan gevuld met de geïmporteerde waarden.
3. Tik daarna nog op **"Instellingen opslaan"** om de import te bevestigen — importeren vult alleen
   de velden, het slaat nog niet automatisch op.

**Klembord werkt niet (bijv. bij beheer op afstand via scrcpy)?** De code wordt bij het genereren
ook altijd weggeschreven naar de eigen (private) opslag van de app, als `mailjet-export.txt`. Haal
'm op vanaf je computer met:

```bash
adb shell run-as eu.ulonetwork.monitorapp cat files/mailjet-export.txt
```

Dit werkt zonder opslagpermissies omdat het de interne, app-eigen opslag is (`run-as` kan hierbij
omdat dit een debug-build is). Kopieer de output (inclusief het `UMJMAIL1:`-voorvoegsel) en plak
'm op het andere apparaat.

**Let op:** deze code bevat je API Key en Secret Key in decodeerbare vorm (base64), dus behandel
'm als een wachtwoord — deel 'm niet via chat-apps of notities die naar derden gesynchroniseerd
worden.

## 6. Trefwoordregels overzetten naar een ander apparaat

Onderaan het "Trefwoorden"-scherm staat dezelfde export/import-functionaliteit als bij de
Mailjet-instellingen, maar dan voor de hele lijst met trefwoordregels:

1. Tik op **"Genereer code (kopieert naar klembord)"**. Dit zet een code met het voorvoegsel
   `UMRULES1:` op het klembord, toont 'm ook in een tekstveld, en schrijft 'm weg naar de
   app-eigen opslag als `keyword-rules-export.txt` (zelfde klembord-fallback als bij de
   Mailjet-code, op te halen met
   `adb shell run-as eu.ulonetwork.monitorapp cat files/keyword-rules-export.txt`).
2. Plak de code op het andere apparaat in het importveld onderaan het "Trefwoorden"-scherm en tik
   op **"Importeren"**.

**Belangrijk verschil met de e-mailinstellingen:** importeren van regels **voegt ze toe** als
nieuwe regels — het overschrijft of verwijdert nooit bestaande regels op het ontvangende apparaat.
Dezelfde code twee keer importeren levert dus duplicaten op; verwijder die in dat geval handmatig
in de lijst.

## 7. Taal van de app

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
  aan/uit-schakelaar, meldingskanalen (lokaal/e-mail), optioneel app-pakketfilter, optionele
  scherm-herkenningstekst en een afkoelperiode in minuten. "Bevat NIET"-regels vereisen een
  specifiek app-pakket. Een trefwoord mag eenvoudige jokertekens bevatten: `*` voor een
  willekeurige reeks tekens (ook geen) en `?` voor precies één teken — bijvoorbeeld `koop*bitcoin`
  vindt ook "koop nu snel bitcoin". Een trefwoord zonder jokertekens werkt zoals voorheen als
  gewone tekstovereenkomst. Wildcards matchen ook over regeleinden heen, zodat een patroon als
  `Attestation*Hardware-verified` ook matcht als de twee delen als aparte tekstelementen op het
  scherm staan. De hele lijst met regels kan via een kopieerbare code worden overgezet naar andere
  apparaten (regels worden bij import toegevoegd, niet overschreven).
- **Scherm-herkenningstekst**: een tweede, optioneel trefwoord per regel (zelfde jokertekens) dat
  eerst op het scherm aanwezig moet zijn voordat de eigenlijke Bevat/Bevat-NIET-controle van die
  regel wordt uitgevoerd. Het app-pakketfilter scopet een regel tot één app, maar niet tot een
  specifiek scherm daarbinnen — zonder deze herkenningstekst zou wisselen naar een ander scherm in
  dezelfde app (waar het trefwoord toevallig ook niet voorkomt) ten onrechte als een ISSUE- of
  RESOLVED-overgang gezien worden. Staat de herkenningstekst niet op het huidige scherm, dan wordt
  de regel dat moment overgeslagen en blijft zijn status ongewijzigd.
- **ISSUE/RESOLVED-meldingen**: een regel meldt zich niet meer bij elke afzonderlijke controle,
  maar alleen bij een statusovergang. Elke regel definieert met zijn overeenkomstmodus wat de
  normale/verwachte toestand is (bij "Bevat": trefwoord aanwezig; bij "Bevat NIET": trefwoord
  afwezig). Zodra die verwachte toestand niet meer klopt, komt er één ISSUE-melding (lokaal en/of
  e-mail, afhankelijk van de kanalen die voor die regel zijn ingeschakeld). Zolang de afwijking
  blijft bestaan, wordt er stil doorgecontroleerd zonder nieuwe meldingen. Pas zodra de verwachte
  toestand weer klopt komt er één RESOLVED-melding — en nooit een RESOLVED-melding zonder dat er
  eerst een ISSUE geregistreerd was. De afkoelperiode van de regel geldt als minimale tijd tussen
  zulke statusovergangen, in beide richtingen. Deze status (`issueActive`) wordt per regel
  onthouden; bij het bewerken van een bestaande regel wordt de status gereset zodra de
  trefwoord-voorwaarde zelf verandert (trefwoord, overeenkomstmodus, hoofdlettergevoeligheid,
  app-pakketfilter of scherm-herkenningstekst), maar blijft behouden als alleen
  kanalen/afkoelperiode/aan-uit worden aangepast.
- **Betrouwbaardere e-maillevering**: het versturen van de alert-e-mail en het wegschrijven van de
  logregel kunnen niet langer stilletjes worden afgebroken doordat het scherm tijdens het versturen
  van de e-mail (tot 15 seconden) opnieuw wijzigt. Als het versturen van een e-mail toch mislukt
  (bijv. door een fout van Mailjet), toont het meldingenlog voortaan de exacte foutmelding bij die
  logregel, in plaats van alleen "geen e-mail" te tonen zonder reden.
- **Meldingenlog**: chronologisch overzicht van ISSUE/RESOLVED-gebeurtenissen, met tijdstip, app,
  tekstfragment en (bij een mislukte e-mail) de foutmelding.
- **E-mail instellingen**: Mailjet-configuratie (API Key/Secret Key, afzender, ontvanger) met
  testknop. E-mail wordt verstuurd via HTTPS naar de Mailjet Send API, niet via SMTP. Instellingen
  kunnen via een kopieerbare code worden overgezet naar andere apparaten.
- **Apparaatgegevens in de e-mail**: elke alert-e-mail begint met apparaatnaam (merk/model),
  een apparaat-ID en het lokale IP-adres van het toestel. Android staat reguliere apps sinds
  Android 10 niet meer toe om het echte hardware-serienummer op te vragen (dat is voorbehouden aan
  systeem-/device-ownerapps), dus als apparaat-ID wordt de `ANDROID_ID` gebruikt: een stabiele,
  per-toestel-en-per-app identifier die niet verandert zolang de app geïnstalleerd blijft.

## Herbouwen na wijzigingen

Elke keer dat je de broncode wijzigt, bouw je opnieuw met hetzelfde commando als in stap 1, en
installeer je de nieuwe APK opnieuw met `adb install -r ...` (de `-r` zorgt dat een bestaande
installatie wordt vervangen zonder gegevensverlies).
