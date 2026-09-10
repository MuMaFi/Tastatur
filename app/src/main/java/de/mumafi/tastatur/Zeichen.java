package de.mumafi.tastatur;

import android.graphics.Path;
import android.graphics.RectF;

/**
 * Die gezeichneten Zeichen — Umschalt, Löschen, Eingabe, Weltkugel, Mikrofon.
 *
 * Sie sind Striche, keine Schrift: Emoji sehen auf jedem Gerät anders aus und
 * bringen ihre eigene Farbe mit. Alle Pfade liegen in einem Feld von 24 × 24
 * und werden beim Zeichnen auf die Tastengröße gerechnet.
 */
final class Zeichen {

  static Path umschalt() {
    Path p = new Path();
    p.moveTo(12f, 3.4f);
    p.lineTo(4.4f, 11.3f);
    p.lineTo(8.2f, 11.3f);
    p.lineTo(8.2f, 17.4f);
    p.lineTo(15.8f, 17.4f);
    p.lineTo(15.8f, 11.3f);
    p.lineTo(19.6f, 11.3f);
    p.close();
    return p;
  }

  static Path loeschen() {
    Path p = new Path();
    p.moveTo(21.4f, 5.2f);
    p.lineTo(9.3f, 5.2f);
    p.lineTo(2.9f, 12f);
    p.lineTo(9.3f, 18.8f);
    p.lineTo(21.4f, 18.8f);
    p.close();
    // Das Kreuz darin
    p.moveTo(12.9f, 9.7f);
    p.lineTo(17.5f, 14.3f);
    p.moveTo(17.5f, 9.7f);
    p.lineTo(12.9f, 14.3f);
    return p;
  }

  static Path eingabe() {
    Path p = new Path();
    p.moveTo(17.8f, 6.9f);
    p.lineTo(17.8f, 11.9f);
    p.quadTo(17.8f, 13.9f, 15.8f, 13.9f);
    p.lineTo(7.3f, 13.9f);
    // Spitze genau am Schaftende
    p.moveTo(10.7f, 10.4f);
    p.lineTo(7.1f, 13.9f);
    p.lineTo(10.7f, 17.4f);
    return p;
  }

  static Path welt() {
    Path p = new Path();
    p.addCircle(12f, 12f, 8.4f, Path.Direction.CW);
    p.moveTo(3.6f, 12f);
    p.lineTo(20.4f, 12f);
    p.addOval(new RectF(8.7f, 3.6f, 15.3f, 20.4f), Path.Direction.CW);
    return p;
  }

  static Path mikro() {
    Path p = new Path();
    p.addRoundRect(new RectF(9f, 2.6f, 15f, 13.8f), 3f, 3f, Path.Direction.CW);
    p.addArc(new RectF(5.4f, 4.8f, 18.6f, 18f), 0f, 180f);
    p.moveTo(12f, 18f);
    p.lineTo(12f, 21.2f);
    return p;
  }

  private Zeichen() { }
}
