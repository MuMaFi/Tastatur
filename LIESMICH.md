# Tastatur

Eine Android-Systemtastatur auf Deutsch, mit Diktierfunktion — und einer
Glasoptik, die den Untergrund durchscheinen lässt.

Sie ist eine richtige Tastatur, keine App mit Textfeld: einmal eingerichtet,
erscheint sie überall, wo getippt wird — in Nachrichten, im Browser, in
Formularen.

## APK bekommen

**Ohne eigenen Rechner:** Auf GitHub unter *Actions → APK bauen → Run
workflow*. Nach ein paar Minuten hängt das APK unter dem Lauf als Artefakt
`tastatur-apk`.

**Selbst bauen:** Android SDK 34 und JDK 17 vorausgesetzt.

    gradle assembleDebug

Das fertige APK liegt in `app/build/outputs/apk/debug/`.

## Einrichten

Nach der Installation die App einmal öffnen; sie führt durch drei Schritte:

1. **Tastatur zulassen** — in den Systemeinstellungen einschalten. Android
   warnt dabei, die Tastatur könne alles mitlesen, was getippt wird. Der
   Hinweis erscheint bei jeder Tastatur; diese hier schickt nichts weg.
2. **Tastatur auswählen** — bestimmt, welche beim Tippen erscheint.
3. **Mikrofon erlauben** — nur fürs Diktieren.

## Diktieren

Die Mikrofontaste in der untersten Zeile startet das Diktat. Was gesprochen
wird, erscheint blass im Textfeld, solange es noch nicht feststeht, und wird
danach übernommen. Ein zweiter Tipper auf die Taste beendet das Diktat.

Sprechpausen beenden es nicht: Androids Spracherkennung meldet sich nach
jedem Satz von selbst ab, die Tastatur startet sie dann neu. Erst wenn
dreimal nacheinander nichts ankommt, hört sie auf.

Die Erkennung ist die des Geräts. Ob sie ohne Netz arbeitet, hängt davon ab,
ob die deutsche Sprache dafür heruntergeladen wurde — unter
*Einstellungen → System → Sprachen → Spracherkennung*.

## Was die Tastatur kann

* QWERTZ mit ü, ö, ä und ß auf den Hauptzeilen — deshalb elf Tasten pro
  Zeile statt zehn
* Umschalttaste: einmal tippen für einen großen Buchstaben, zweimal für
  Feststellung
* Großschreibung am Satzanfang von selbst
* Zwei Ebenen für Zahlen und Sonderzeichen
* Löschen beschleunigt beim Halten
* Zweimal Leertaste macht einen Punkt
* Die Eingabetaste trägt das Wort, das die App vorgibt — „Senden",
  „Suchen", „Weiter" —, sonst den Rücklaufpfeil
* Weltkugel: kurz für die nächste Tastatur, lang für die ganze Auswahl

Noch nicht dabei: Wortvorschläge und Autokorrektur. Die Leiste über den
Tasten zeigt deshalb den Stand des Diktats, keine erfundenen Vorschläge.

## Die Optik

Das Glas ist keine Nachahmung mit halbdurchsichtigem Grau. Android zeichnet
den Untergrund hinter dem Fenster wirklich weich (`setBackgroundBlurRadius`,
ab Android 12), und die Tasten liegen als eigene Schicht darauf: Lichtkante
oben, Schattenkante unten, eine helle Linie an der unteren Innenkante, wo
sich das Licht im Glas sammelt.

Kann das Gerät nicht weichzeichnen — älter als Android 12, Stromsparmodus,
oder abgeschaltet —, deckt das Feld von sich aus stärker, damit die Schrift
lesbar bleibt.

Die Tasten sind Superellipsen, keine Rechtecke mit Kreisecken: ein
Kreisradius setzt an der Geraden mit einem sichtbaren Knick an, und bei elf
Tasten nebeneinander sieht man nichts anderes mehr.

## Die Vorlage

`entwurf/tastatur-entwurf.html` zeichnet dieselbe Tastatur im Browser —
gleiche Maße, gleiche Farbwerte, gleiche Superellipse. Damit lässt sich am
Aussehen arbeiten, ohne für jede Änderung ein APK zu bauen. Wer dort etwas
ändert, ändert es in `Farben.java` und `Glas.java` mit.

Aufrufen mit `?thema=hell|dunkel` und
`?zustand=ruhe|gedrueckt|diktat|zeichen`.

## Aufbau

    app/src/main/java/de/mumafi/tastatur/
      TastaturDienst.java     Eingabemethode: Textfeld, Diktat, Fenster
      TastaturAnsicht.java    Was man sieht und anfasst
      Glas.java               Die Form der Tasten und ihre Lichtführung
      Zeichen.java            Umschalt, Löschen, Eingabe, Weltkugel, Mikrofon
      Belegung.java           Die drei Ebenen
      Farben.java             Farbwerte, hell und dunkel
      Diktat.java             Androids Spracherkennung, am Laufen gehalten
      EinrichtungActivity.java   Die drei Schritte
      BerechtigungActivity.java  Fragt nach dem Mikrofon, ohne sich zu zeigen
