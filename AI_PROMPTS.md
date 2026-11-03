# Dokumentation der KI-Unterstützung

Für diese Lösung wurde Hermes Agent mit einem OpenAI-Codex-Chatmodell zur Implementierung, Dokumentation und Prüfung eingesetzt. Das ist keine Erklärung eigenständiger, ausschließlich manueller Bearbeitung. KI-generierte Änderungen wurden mit automatisierten Tests und realen HTTP-/Browseraufrufen geprüft. Das persönliche Abgabegespräch muss der Schüler selbst führen.

## Benutzerauftrag

> https://elearning.tgm.ac.at/mod/assign/view.php?id=23103
>
> bitte abarbeiten und abgeben

## Verwendete technische Arbeitsaufträge

Die folgenden Arbeitsaufträge geben die verwendeten technischen Prompts in gekürzter Form wieder; Zugangsdaten, private Umgebungsangaben und nicht fachliche Agentensteuerung sind nicht enthalten.

### Backend und Tests

> Vervollständige die Lehrervorlage für GK7.1 Floodmonitor in Java 17 und Spring Boot. Behalte Paketstruktur und Gradle bei. Implementiere die Modelle Station, Location und Measurement mit den geforderten Enums, ein speicherbegrenztes In-Memory-Repository, eine konfigurierbare zusammenhängende Simulation und serverseitige Warnstufen. Setze alle GET-Endpunkte, Filter, Zeitraum-/Limitvalidierung, Statistik, JSON/XML-Content-Negotiation und zentrale strukturierte Fehler um. Dokumentiere die öffentliche lesende API mit OpenAPI. Schreibe deterministische Tests für die geforderten Fälle sowie Randfälle und führe test und bootJar aus. Nutze einfache, lesbare Schulprojekt-Strukturen, keine unnötigen Entwurfsmuster.

### Webclient

> Erstelle einen einfachen deutschsprachigen HTML-/CSS-/jQuery-Client. Lade Stationen, aktuelle und historische Werte. Unterstütze Zeitraum-/Warnstufenfilter, JSON/XML per Accept-Header, echte XML-Auswertung, periodische Aktualisierung, Lade-/Fehlerzustände und die geforderten Warnfarben. Verwende lokale jQuery-Dateien, sichere Textausgabe und verhindere, dass veraltete parallele Antworten die aktuelle Auswahl überschreiben.

### Dokumentation und Integration

> Erstelle ein schlichtes Cheatsheet zu allen acht Fragen mit Quellenverweisen. Beschreibe Architektur, Datenmodell, Simulation, Start, API-Beispiele und tatsächliche Probleme. Prüfe die zusammengesetzte Anwendung im Browser für JSON und XML und verwende eigene Screenshots aus dem laufenden Client statt Bilder der Vorlage. Dokumentiere Grenzen offen und erfinde keine Testausgaben oder persönlichen Erfahrungen.

## Umgang mit Ergebnissen

Die Vorlage stammt von `ThomasMicheler/DEZSYS_GK71_FLOODMONITOR_REST_INCOMPLETE`; ihre Git-Historie bleibt erhalten. Fachliche Quellen stehen im Cheatsheet. Messwerte sind ausdrücklich simuliert. Screenshots und Prüfergebnisse beziehen sich auf echte Ausführung der erzeugten Anwendung, nicht auf reale Pegelstationen. Probleme und Grenzen werden in der Projektdokumentation festgehalten.
