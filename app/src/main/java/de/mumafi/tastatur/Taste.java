package de.mumafi.tastatur;

/** Eine Taste: was auf ihr steht, was sie auslöst, und wo sie liegt. */
final class Taste {

  enum Art { BUCHSTABE, UMSCHALT, LOESCHEN, EBENE, WELT, MIKRO, LEER, EINGABE }

  final Art art;
  /** Der Text, der eingefügt wird. Bei Sondertasten null. */
  final String zeichen;
  /** Was draufsteht. Null heißt: es wird ein Zeichen gemalt, kein Text. */
  final String aufschrift;
  /** Bei EBENE: wohin gewechselt wird. */
  final int ebene;
  /** Anteil an der Zeilenbreite. */
  final float gewicht;

  /* Lage, beim Vermessen gesetzt. */
  int x, y, breite, hoehe;

  private Taste(Art art, String zeichen, String aufschrift, int ebene, float gewicht) {
    this.art = art;
    this.zeichen = zeichen;
    this.aufschrift = aufschrift;
    this.ebene = ebene;
    this.gewicht = gewicht;
  }

  static Taste buchstabe(String z) {
    return new Taste(Art.BUCHSTABE, z, z, -1, 1f);
  }

  static Taste buchstabe(String z, float gewicht) {
    return new Taste(Art.BUCHSTABE, z, z, -1, gewicht);
  }

  static Taste sonder(Art art, float gewicht) {
    return new Taste(art, null, null, -1, gewicht);
  }

  static Taste ebene(String aufschrift, int ziel, float gewicht) {
    return new Taste(Art.EBENE, null, aufschrift, ziel, gewicht);
  }

  boolean trifft(int px, int py) {
    return px >= x && px < x + breite && py >= y && py < y + hoehe;
  }

  /** Waagerechter Abstand zum Mittelpunkt — für das Zuordnen beim Wischen. */
  int abstand(int px, int py) {
    int mx = x + breite / 2, my = y + hoehe / 2;
    int dx = px - mx, dy = py - my;
    return dx * dx + dy * dy;
  }
}
