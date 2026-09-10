package de.mumafi.tastatur;

import android.content.Context;
import android.content.res.Configuration;

/**
 * Die Farbwerte der Tastatur, hell und dunkel. Es sind dieselben Zahlen wie
 * in entwurf/tastatur-entwurf.html — wer dort etwas ändert, ändert es hier
 * mit, sonst laufen Vorlage und App auseinander.
 */
final class Farben {

  final boolean dunkel;

  /* Tastaturfeld */
  final int panelOben, panelUnten, panelLichtkante;

  /* Tasten */
  final int tasteOben, tasteUnten;
  final int nebenOben, nebenUnten;
  final int gedruecktOben, gedruecktUnten;
  final int glanz;              // Verlauf über der oberen Hälfte
  final int rimOben, rimUnten;  // Licht- und Schattenkante
  final int brechung;           // helle Linie an der unteren Innenkante
  final int fall;               // Schlagschatten

  /* Zeichen */
  final int schrift, schriftNeben, akzent, trenner, steg;

  /* Mikrofon */
  final int mikroOben, mikroUnten, hoertOben, hoertUnten, pegel;

  private Farben(boolean dunkel) {
    this.dunkel = dunkel;
    if (dunkel) {
      panelOben       = 0x851E212C;
      panelUnten      = 0x700E1016;
      panelLichtkante = 0x2EFFFFFF;

      tasteOben       = 0x33FFFFFF;
      tasteUnten      = 0x13FFFFFF;
      nebenOben       = 0x1AFFFFFF;
      nebenUnten      = 0x09FFFFFF;
      gedruecktOben   = 0x6BFFFFFF;
      gedruecktUnten  = 0x40FFFFFF;
      glanz           = 0x52FFFFFF;
      rimOben         = 0x66FFFFFF;
      rimUnten        = 0x38000000;
      brechung        = 0x61FFFFFF;
      fall            = 0x61000000;

      schrift         = 0xFFF6F7FA;
      schriftNeben    = 0xC7F6F7FA;
      akzent          = 0xFF8EA9FF;
      trenner         = 0x29FFFFFF;
      steg            = 0x57FFFFFF;

      mikroOben       = 0xF29682FF;
      mikroUnten      = 0xE64048E1;
      hoertOben       = 0xF7FF8AA8;
      hoertUnten      = 0xF0E22058;
      pegel           = 0xFFFF3D70;
    } else {
      panelOben       = 0x80FFFFFF;
      panelUnten      = 0x61EEF1FA;
      panelLichtkante = 0xD9FFFFFF;

      tasteOben       = 0xCCFFFFFF;
      tasteUnten      = 0x75FFFFFF;
      nebenOben       = 0x57FFFFFF;
      nebenUnten      = 0x26FFFFFF;
      gedruecktOben   = 0xFFFFFFFF;
      gedruecktUnten  = 0xE0FFFFFF;
      glanz           = 0xEBFFFFFF;
      rimOben         = 0xFAFFFFFF;
      rimUnten        = 0x0F0C101E;
      brechung        = 0xD9FFFFFF;
      fall            = 0x210E1222;

      schrift         = 0xFF10131A;
      schriftNeben    = 0xB8161A24;
      akzent          = 0xFF3A6BFF;
      trenner         = 0x24141828;
      steg            = 0x4D141828;

      mikroOben       = 0xF29682FF;
      mikroUnten      = 0xE64048E1;
      hoertOben       = 0xF7FF8AA8;
      hoertUnten      = 0xF0E22058;
      pegel           = 0xFFFF3D70;
    }
  }

  static Farben fuer(Context ctx) {
    int modus = ctx.getResources().getConfiguration().uiMode
        & Configuration.UI_MODE_NIGHT_MASK;
    return new Farben(modus == Configuration.UI_MODE_NIGHT_YES);
  }
}
