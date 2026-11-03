# Webclient-Prüfung

Der Client wird von Spring Boot unter `/` ausgeliefert. Er benötigt keine CDN-Verbindung; jQuery 3.7.1 und die MIT-Lizenz liegen unter `vendor/`.

Automatischer DOM-Test mit echten jQuery-Callbacks und kontrollierten HTTP-Antworten (kein Backend nötig), aus diesem `static`-Verzeichnis:

```sh
npm install --prefix .verify --no-audit --no-fund --no-package-lock jsdom@26
node tests/client.test.cjs
```

`.verify` ist nur eine temporäre Testabhängigkeit und darf nicht ausgeliefert werden. Danach dieses Verzeichnis entfernen. Alternativ kann `JSDOM_PATH` auf eine bereits vorhandene jsdom-Installation zeigen.

Geprüft werden JSON und tatsächlich geparstes XML, übliche Jackson-Wrapper, alle neun Messwertfelder, Stationsdaten, leere Antworten, sichere Textausgabe, JSON/XML-Fehlermeldungen, lokale Zeit nach UTC, Filtervalidierung, Warnstufen, konfigurierbare 10-Sekunden-Aktualisierung sowie abgebrochene und verspätete Antworten. Die Tests verwenden Beispieldaten als explizite Test-Fixtures; sie belegen keine Backend-Integration.

Zusätzlich bei laufendem Backend im Browser prüfen: Stationsauswahl, JSON/XML-Umschaltung im Netzwerk-Tab (`Accept`), lokale Datumsfilter, leere Anfangsdaten, Messhistorie, automatische Aktualisierung und schmale Bildschirmbreite.
