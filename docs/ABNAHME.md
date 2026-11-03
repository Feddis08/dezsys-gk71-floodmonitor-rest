# Abnahmeübersicht GK7.1

Maßgeblich ist die Aufgabenstellung in [Moodle, Aktivität 23103](https://elearning.tgm.ac.at/mod/assign/view.php?id=23103). `TASK.md` wurde unverändert aus dem Lehrer-Repository übernommen und enthält zusätzlich optionale Erweiterungen; Stationsverwaltung gehört nicht zum hier geforderten GK-Leseumfang.

| Anforderung | Nachweis im Projekt |
|---|---|
| Lehrervorlage, Java/Spring Boot, Gradle | Paket `com.example.floodmonitor`, `build.gradle`, Gradle Wrapper; ursprüngliche Git-Historie erhalten |
| Mindestens drei Stationen | Repository-Initialisierung und API-Test |
| Station, Location, Measurement und beide Enums | Paket `model` |
| Java-Zeitdatentyp und ISO-8601 | Messzeitpunkt als `Instant`, JSON/XML-Antworten |
| Regelmäßige korrelierte Simulation, konfigurierbar | `SimulationService`, Eigenschaften `simulation.*` und Tests |
| Keine Messungen für deaktivierte/ausgefallene Stationen | Simulationsprüfung und Tests |
| Serverseitige Warnstufen inklusive Grenzwerte und UNKNOWN | Warnstufen-Service und Tests |
| Stationen, Einzelstation, letzter Messwert, Historie, Alerts | Controller und API-Tests |
| Stationsfilter river/isActive/warningLevel | API-Tests und dokumentierte Query-Parameter |
| Historie from/to/limit/warningLevel | API-Tests und Webclient |
| Alerts warningLevel/river | API-Tests |
| Statistik mit Min/Max/Mittelwerten, Niederschlag und Anzahlen | Statistik-Endpunkt und Tests; Semantik in README |
| JSON-Standard und XML per Accept, gleiche Fachinformationen | HTTP-Integration und UI in beiden Formaten |
| Zentrale Fehler, 400/404/406, keine Stacktraces | Exception Handling und negative Tests |
| Historienlimit und ungültige Werte | API-Tests für Grenzen/Formatfehler |
| HTML/CSS/jQuery-Client | `src/main/resources/static` mit lokalem jQuery |
| Aktuelle Messung, Tabelle und alle Pflichtspalten | Browserprüfung und Screenshots |
| Zeitraum/Warnstufe, JSON/XML, Auto-Refresh | Browserprüfung in beiden Formaten |
| Ladezustände, lesbare Fehler, Warnfarben | Webclient und Browserprüfung |
| Schichtentrennung | model/repository/service/controller/exception und statischer Client |
| Kontrollierbare Simulation, deterministische Tests | Testkonfiguration und Simulations-Tests |
| OpenAPI, Parameter/Antworten/Formate/Fehler/Auth | `/v3/api-docs`, `/swagger-ui/index.html` |
| Alle acht Theoriefragen, jeweils mit Quellen | `docs/CHEATSHEET.md` und PDF |
| KI-Prompts dokumentiert | `AI_PROMPTS.md` |
| Architektur, Modell, Regeln, Start, Beispiele, Reflexion | `README.md` |
| Eigene Screenshots | `docs/screenshots` aus der laufenden Anwendung |
| Nachvollziehbare Git-Commits | Historie der Vorlage plus getrennte Bearbeitungs-Commits |

Das persönliche Abgabegespräch ist durch die technische Repository-Abgabe nicht ersetzt.
