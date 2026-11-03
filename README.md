# Hochwassermonitoring – GK7.1

Spring-Boot-Anwendung mit drei simulierten Messstationen, lesender REST-API, JSON/XML, OpenAPI und jQuery-Webclient.

Ausgangspunkt: [Lehrervorlage von Thomas Micheler](https://github.com/ThomasMicheler/DEZSYS_GK71_FLOODMONITOR_REST_INCOMPLETE). Die ursprüngliche Git-Historie bleibt erhalten. Maßgeblich ist die [Moodle-Aufgabe GK7.1](https://elearning.tgm.ac.at/mod/assign/view.php?id=23103); die zusätzlichen Verwaltungs-/Authentifizierungserweiterungen aus `TASK.md` gehören nicht zum implementierten GK-Leseumfang.

## Start

Voraussetzungen: **JDK 17 oder 21**, beim ersten Build Internet für Gradle und Maven Central. Kein separater Datenbankserver und keine Zugangsdaten nötig. Java 24 nicht mit diesem Gradle-8.7-Wrapper verwenden.

```sh
git clone https://github.com/Feddis08/dezsys-gk71-floodmonitor-rest.git
cd dezsys-gk71-floodmonitor-rest
./gradlew test
./gradlew bootRun
```

Unter Windows: `gradlew.bat test` und `gradlew.bat bootRun`.

- Webclient: <http://localhost:8080/>
- Swagger UI: <http://localhost:8080/swagger-ui/index.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>
- Statischer OpenAPI-Snapshot: [`docs/openapi.json`](docs/openapi.json)
- Cheatsheet mit allen acht Fragen und Quellen: [`docs/CHEATSHEET.pdf`](docs/CHEATSHEET.pdf) / [Markdown](docs/CHEATSHEET.md)
- KI-Unterstützung: [`AI_PROMPTS.md`](AI_PROMPTS.md)

Ein ausführbares JAR erzeugen:

```sh
./gradlew bootJar
java -jar build/libs/floodmonitor-0.0.1-SNAPSHOT.jar
```

Der tatsächliche Archivname folgt `settings.gradle` und der Version in `build.gradle`; alternativ `java -jar build/libs/*-SNAPSHOT.jar` verwenden, sofern dort genau ein ausführbares JAR vorhanden ist.

## Konfiguration

Standardwerte stehen in `src/main/resources/application.properties`. Eine kopierbare Konfiguration ohne Secrets liegt in [`application-example.properties`](application-example.properties).

```sh
cp application-example.properties application-local.properties
./gradlew bootRun --args='--spring.profiles.active=local'
```

| Eigenschaft | Standard | Bedeutung |
|---|---:|---|
| `server.port` | 8080 | HTTP-Port |
| `simulation.enabled` | true | Periodische Simulation ein-/ausschalten; anfängliche Demo-Historie bleibt vorhanden |
| `simulation.interval` | 10000 | Abstand in ms zwischen Simulationsläufen |
| `measurement.max-age-ms` | 60000 | Maximales Alter aktueller Werte für eine bekannte Warnstufe |
| `history.capacity` | 1000 | Gespeicherte Werte je Station, zulässig 1–10000 |

Ist das Simulationsintervall größer als das zulässige Messwertalter, sind Werte zwischen den Läufen zeitweise `UNKNOWN`. Bei deaktivierter Simulation gibt es weiterhin Startdaten, aber keine neuen periodischen Werte.

## Architektur und Datenmodell

```text
HTTP / jQuery-Webclient
        │
        ▼
StationController ──► FloodService ──► FloodRepository
                           │                 ▲
                      WarningService         │
                                      SimulationService
                                             ▲
                                      SimulationScheduler

ApiExceptionHandler: einheitliche Fehlerantworten
WebConfig / XmlListAdvice: Formatauswahl und XML-Listen
OpenApiConfig: API-Vertrag und Fehler-Schemas
```

- **Model**: unveränderliche Java-Datenmodelle. `Station` enthält `id`, `name`, `river`, `Location(latitude, longitude)`, Normal-/Warn-/kritischen Wasserstand und `isActive`. `Measurement` enthält Stations-ID, `Instant timestamp`, Wasserstand, Durchfluss, Regen, Temperatur, Akku, Stationsstatus und Warnstufe. Die Enums heißen `WarningLevel` (`NORMAL`, `WARNING`, `CRITICAL`, `UNKNOWN`) und `StationStatus` (`ONLINE`, `MAINTENANCE`, `OFFLINE`).
- **Repository**: unveränderliche Stationsliste und synchronisierte, nach `Instant` sortierte Messhistorien. Älteste Werte werden beim Kapazitätslimit entfernt. Abrufe liefern Snapshots, keine veränderbaren internen Collections.
- **Service**: Filter, Zeitraumvalidierung, Statistik und Warnstufen. Der Simulator schreibt neue Werte; ein GET erzeugt keine Messung.
- **Controller**: routet Anfragen an den Service. Keine umfangreiche Fachlogik.
- **Exception Handling**: Fehlerobjekt mit `timestamp`, `status`, `error`, `message`, `path`; keine Stacktraces im Response.
- **Client**: lokales jQuery 3.7.1, keine CDN-Abhängigkeit. JSON und XML werden wirklich separat geparst. Ausgabe über Textknoten, nicht als ungeprüftes HTML. Veraltete Antworten nach einer neuen Auswahl werden verworfen.

Einheiten: Wasserstand **cm**, Durchfluss **m³/s**, Niederschlagswert **mm/h**, Temperatur **°C**, Akku **%**. Koordinaten und Stationsnamen dienen der Demonstration; Schwellen und Werte sind keine amtlichen Pegeldaten.

## Simulations- und Warnregeln

Beim Start erhält jede aktive Station zwölf ausdrücklich simulierte Demo-Messungen im Abstand von zehn Sekunden. Die Startstände 260/340/370 cm zeigen unterschiedliche Warnsituationen. Danach läuft die Simulation standardmäßig alle zehn Sekunden.

Für jeden Schritt wird der vorherige Messwert berücksichtigt:

```text
Regen = begrenzen(0,94 × vorheriger Regen + Zufall[0,2), 0, 80)
Änderung = begrenzen(0,12 × Regen − 0,4 + Zufall[-0,3;0,3), −3, 5)
Wasserstand = max(0, vorheriger Wasserstand + Änderung)
Durchfluss = 2,2 × Wasserstand
Temperatur = vorherige Temperatur + kleine Änderung, begrenzt auf −30 bis 45 °C
Akku = max(0, vorheriger Akku − 0,02 Prozentpunkte)
```

Wasserstand, Durchfluss, Regen und Temperatur werden auf eine Nachkommastelle gerundet. Bei Akku unter 10 % wird `MAINTENANCE`, bei 0 % `OFFLINE` gesetzt. Für deaktivierte oder bereits ausgefallene Stationen entstehen keine neuen Werte. Ungültige bzw. zukünftig datierte Vorwerte werden nicht fortgeschrieben. Die einfache Formel bildet einen zusammenhängenden Verlauf, kein hydrologisches Vorhersagemodell.

Die Warnstufe wird ausschließlich serverseitig berechnet:

| Bedingung | Warnstufe |
|---|---|
| Wasserstand unter Warnschwelle | NORMAL |
| Wasserstand ab Warnschwelle, unter kritischer Schwelle | WARNING |
| Wasserstand ab kritischer Schwelle | CRITICAL |
| Fehlend/ungültig, Station inaktiv/offline, Zukunft oder aktueller Wert zu alt | UNKNOWN |

Bei historischen Messungen wird deren Alter relativ zur jetzigen Uhrzeit nicht als Fehler gewertet; sonst würde jede ältere Messung `UNKNOWN`. Für `latest`, Stationswarnfilter und Alerts wird die Aktualität dagegen geprüft. Fehlende letzte Messungen liefern 404; die Stationswarnstufe ist intern trotzdem `UNKNOWN`. Alerts enthalten nur aktuelle `WARNING`-/`CRITICAL`-Werte.

## API

Alle fachlichen Endpunkte sind GET und bieten `application/json` sowie `application/xml` an. Standard ohne Accept ist JSON.

| Pfad unter `/api/v1` | Query-Parameter |
|---|---|
| `/stations` | `river`, `isActive=true/false`, `warningLevel` |
| `/stations/{stationId}` | – |
| `/stations/{stationId}/measurements/latest` | – |
| `/stations/{stationId}/measurements` | `from`, `to`, `limit`, `warningLevel` |
| `/alerts` | `warningLevel`, `river` |
| `/stations/{stationId}/statistics` | `from`, `to` |

`from` und `to` sind inklusive ISO-8601-Zeitpunkte mit Offset/UTC, etwa `2026-11-03T10:00:00Z`. `from` darf nicht nach `to` liegen. `limit` ist 1–1000, Standard 100. Erst wird gefiltert, danach begrenzt. Historien kommen absteigend nach Zeitpunkt. Flussnamen werden exakt, ohne Unterscheidung der Groß-/Kleinschreibung verglichen. Enums sind großgeschrieben.

Statistik: `minWaterLevel`, `maxWaterLevel`, `averageWaterLevel`, `averageFlowRate`, `totalRainfall`, `measurementCount`, `warningCount`, `criticalCount`. `warningCount` zählt WARNING, `criticalCount` separat CRITICAL. `totalRainfall` ist entsprechend der verlangten Summe die Summe der gespeicherten Niederschlagswerte, **keine integrierte Niederschlagsmenge in mm**: Die Messwerte sind Raten und teilweise überlappende Stundenwerte. Leere Zeitfenster liefern Nullwerte für Extrema/Mittelwerte, sonst Nullen für Summen und Anzahlen.

```sh
curl -H 'Accept: application/json' http://localhost:8080/api/v1/stations
curl -H 'Accept: application/xml' http://localhost:8080/api/v1/stations
curl -H 'Accept: application/xml' http://localhost:8080/api/v1/stations/ST-001/measurements/latest
curl 'http://localhost:8080/api/v1/stations?river=Donau&isActive=true'
curl 'http://localhost:8080/api/v1/stations/ST-001/measurements?limit=5&warningLevel=NORMAL'
curl 'http://localhost:8080/api/v1/alerts?river=Salzach&warningLevel=CRITICAL'
curl 'http://localhost:8080/api/v1/stations/ST-001/statistics'
curl -i 'http://localhost:8080/api/v1/stations/ST-999'
curl -i 'http://localhost:8080/api/v1/stations/ST-001/measurements?limit=1001'
curl -i -H 'Accept: text/csv' http://localhost:8080/api/v1/stations
```

JSON-Listen sind Arrays. XML-Listen haben `<stations><station>…</station></stations>` bzw. `<measurements><measurement>…</measurement></measurements>`. Fachliche Felder sind identisch. Bei nicht unterstütztem Accept wird 406 mit einem strukturierten JSON-Fehler als Fallback ausgegeben, weil das angeforderte Format nicht angeboten werden kann. Fehler bei gültigem XML-Accept werden als XML ausgegeben.

Statuscodes: 200 Erfolg; 400 ungültige Parameter; 404 unbekannte Station/fehlende Messung; 405 nicht erlaubte Methode; 406 nicht unterstütztes Antwortformat; 500 allgemeiner interner Fehler. 201/204/401/403/409/415 werden nicht künstlich erzeugt: Die GK-API hat keine schreibenden Operationen, Authentifizierung oder Request-Bodies.

### Sicherheit und Grenzen

Die Demo hat ausdrücklich **keine Authentifizierung**; das OpenAPI-Dokument erklärt den öffentlichen, nur lesenden Zugriff. Es gibt keine administrativen Mutationen und keine freizügige CORS-Freigabe. Parameter werden geprüft, Historie und Antwortlimit begrenzt, Serverfehler verraten keine Implementierungsdetails. Das ist kein vollständiges Produktions-Sicherheitskonzept: Internetbetrieb würde TLS, Authentifizierung nach Bedarf, Rate-Limits und Monitoring benötigen. Alle Daten sind flüchtig und werden beim Neustart ersetzt. Nicht als echtes Hochwasserwarnsystem verwenden.

## Webclient

Station auswählen; Zeitraum in der lokalen Browserzeitzone, Warnstufe und Zeilenlimit einstellen. Der Client konvertiert Zeiten nach UTC. JSON/XML wird über den Accept-Header gewechselt. Auto-Refresh kann deaktiviert und das Intervall zwischen 1 und 3600 Sekunden eingestellt werden. Die Warnfarben sind grün/orange/rot/grau; der Text bleibt zusätzlich sichtbar. Vollständige ISO-Zeitpunkte sind als Tooltip hinterlegt. Auf schmalen Bildschirmen ist die Tabelle horizontal scrollbar.

Eigene Screenshots aus der laufenden Anwendung:

- [JSON](docs/screenshots/webclient-json.png)
- [XML](docs/screenshots/webclient-xml.png)
- [Validierungsfehler](docs/screenshots/webclient-validation.png)

## Tests

```sh
./gradlew test
./gradlew bootJar
```

`FloodMonitorApiTest` prüft JSON/XML, Standardformat, einzelne/unbekannte Stationen, letzte Messung, Filter, Zeiträume, Limits, alle Warnschwellen, Fehlerantworten und den OpenAPI-Vertrag. `FloodLogicTest` prüft Grenzwerte, fehlende/veraltete/ungültige Werte, Statistik, begrenzte Historie, Korrelation sowie inaktive/offline Stationen und niedrigen Akku. Die periodische Simulation ist in API-Tests deaktiviert; feste Zeitpunkte und kontrollierte Simulationsschritte verhindern zufällige Fehlschläge. Testergebnisse stehen unter `build/reports/tests/test/index.html`.

Optionaler Browser-Smoke mit Node.js (nicht zum normalen Java-Start nötig):

```sh
npm install
npx playwright install chromium
# Anwendung parallel starten
npm run test:browser
# Bei anderem Port:
BASE_URL=http://localhost:8873 npm run test:browser
```

Der Browser-Smoke prüft beide echten Formate, Messwerte, Filter, Zeilenlimit, Zeitraumfehler, XML-Fehlerverarbeitung und periodische Abrufe. Der Serverfehlerpfad wird dabei gezielt im Browser abgefangen, nicht als echter Backendfehler ausgegeben. Screenshots werden erneuert. Maschinenlesbares Ergebnis: `build/browser-smoke.json`.

## Kurze technische Reflexion

- Die Vorlage erzeugte bei jedem Abruf neue zufällige Stationsdaten. Das verhindert eine sinnvolle Historie. Die Lösung trennt deshalb Stationsstammdaten, gespeicherte Messungen und zeitgesteuerte Simulation.
- XML-Listen benötigen eindeutige Wrapper, während JSON eine Array-Antwort behält. Ein Response-Advice löst diese Darstellungsfrage ohne doppelte Fachlogik. Beide Formate wurden wirklich vom Browser abgerufen.
- Bei der Integration waren Einheiten im Client zunächst falsch beschriftet. Sie wurden gegen die Aufgabenstellung auf cm und mm/h korrigiert. Lange Zeitstempel und unformatierte Fließkommazahlen machten die Tabelle unnötig breit; lokale Datumsanzeige und gerundete Anzeige verbessern die Lesbarkeit, ohne die API-Werte zu verändern.
- Aktuelle Warnungen und historische Warnungen dürfen nicht dieselbe Altersprüfung verwenden. Deshalb wird die Aktualitätsprüfung nur bei aktuellen Abfragen angewendet.
- Die erste Ausführung der Vorlage hatte keine Tests. Die ergänzten Tests prüfen nicht nur erfolgreiche Antworten, sondern insbesondere fehlerhafte Parameter und Grenzwerte.
- Die Umsetzung wurde KI-unterstützt erstellt; Prompts und Umfang stehen in `AI_PROMPTS.md`. Diese Reflexion beschreibt beobachtete technische Probleme und behauptet keine persönliche manuelle Erfahrung oder ein bereits geführtes Abgabegespräch.
