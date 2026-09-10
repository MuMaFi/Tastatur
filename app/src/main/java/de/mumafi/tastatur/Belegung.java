package de.mumafi.tastatur;

import java.util.ArrayList;
import java.util.List;

/**
 * Die drei Ebenen der Tastatur. QWERTZ mit ü, ö, ä und ß auf den
 * Hauptzeilen — deshalb elf Tasten pro Zeile statt zehn.
 */
final class Belegung {

  static final int BUCHSTABEN = 0;
  static final int ZAHLEN = 1;
  static final int SONDER = 2;

  private static final float BREIT = 1.52f;   // Umschalt, Löschen, Ebene
  private static final float LEER = 4.6f;     // Leertaste

  static Taste[][] ebene(int nummer) {
    switch (nummer) {
      case ZAHLEN:  return zahlen();
      case SONDER:  return sonder();
      default:      return buchstaben();
    }
  }

  private static Taste[][] buchstaben() {
    return new Taste[][] {
        zeile("q w e r t z u i o p ü"),
        zeile("a s d f g h j k l ö ä"),
        mitRand(Taste.sonder(Taste.Art.UMSCHALT, BREIT),
                zeile("y x c v b n m ß"),
                Taste.sonder(Taste.Art.LOESCHEN, BREIT)),
        unten("?123", ZAHLEN),
    };
  }

  private static Taste[][] zahlen() {
    return new Taste[][] {
        zeile("1 2 3 4 5 6 7 8 9 0"),
        zeile("@ # € % & - + ( ) /"),
        mitRand(Taste.ebene("=\\<", SONDER, BREIT),
                zeile("* \" ' : ; ! ?"),
                Taste.sonder(Taste.Art.LOESCHEN, BREIT)),
        unten("ABC", BUCHSTABEN),
    };
  }

  private static Taste[][] sonder() {
    return new Taste[][] {
        zeile("~ ` | • √ π ÷ × ¶ ∆"),
        zeile("£ ¢ $ ¥ ^ ° = { }"),
        mitRand(Taste.ebene("?123", ZAHLEN, BREIT),
                zeile("\\ © ® ™ ℅ [ ]"),
                Taste.sonder(Taste.Art.LOESCHEN, BREIT)),
        unten("ABC", BUCHSTABEN),
    };
  }

  /** Die unterste Zeile ist auf allen Ebenen gleich, nur die Ebenentaste wechselt. */
  private static Taste[] unten(String aufschrift, int ziel) {
    return new Taste[] {
        Taste.ebene(aufschrift, ziel, BREIT),
        Taste.sonder(Taste.Art.WELT, 1f),
        Taste.sonder(Taste.Art.MIKRO, 1f),
        Taste.sonder(Taste.Art.LEER, LEER),
        Taste.buchstabe("."),
        Taste.sonder(Taste.Art.EINGABE, BREIT),
    };
  }

  private static Taste[] zeile(String zeichen) {
    String[] teile = zeichen.split(" ");
    Taste[] tasten = new Taste[teile.length];
    for (int i = 0; i < teile.length; i++) tasten[i] = Taste.buchstabe(teile[i]);
    return tasten;
  }

  private static Taste[] mitRand(Taste links, Taste[] mitte, Taste rechts) {
    List<Taste> alle = new ArrayList<>(mitte.length + 2);
    alle.add(links);
    for (Taste t : mitte) alle.add(t);
    alle.add(rechts);
    return alle.toArray(new Taste[0]);
  }

  private Belegung() { }
}
