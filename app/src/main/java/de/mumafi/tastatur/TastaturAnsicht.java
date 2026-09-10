package de.mumafi.tastatur;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Looper;
import android.util.SparseArray;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

/**
 * Die Tastatur, wie man sie sieht und anfasst. Sie kennt weder Textfeld noch
 * Spracherkennung — was gedrückt wurde, meldet sie an den {@link Hoerer}.
 */
final class TastaturAnsicht extends View {

  interface Hoerer {
    void schreibe(String text);
    void loesche();
    void eingabe();
    void naechsteTastatur();
    void tastaturAuswahl();
    void mikrofon();
  }

  /* Maße in dp — dieselben wie im Entwurf. */
  private static final float LEISTE = 42f;
  private static final float TASTE = 47f;
  private static final float LUFT = 6f;
  private static final float RAND = 5f;
  private static final float STEG = 22f;
  private static final float ECKE = 28f;

  private final float dichte;
  private final Farben f;
  private final Glas glas;
  private Hoerer hoerer;

  private final Paint schrift = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint strich = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint flaeche = new Paint(Paint.ANTI_ALIAS_FLAG);

  private int ebene = Belegung.BUCHSTABEN;
  private Taste[][] zeilen = Belegung.ebene(Belegung.BUCHSTABEN);

  /** 0 = klein, 1 = einmal groß, 2 = feststehend groß. */
  private int umschalt = 0;
  private long letzteUmschaltung = 0;

  private final SparseArray<Taste> zeiger = new SparseArray<>();
  private Taste vorschau;
  /** Wort auf der Eingabetaste, wenn die App eines vorgibt; sonst der Pfeil. */
  private String eingabewort;
  /** Ob Android den Untergrund weichzeichnet. Wenn nicht, deckt das Feld mehr. */
  private boolean weich = true;
  /** Wer lange gehalten wurde, hat beim Loslassen nichts mehr auszulösen. */
  private Taste erledigt;
  private Runnable langerDruck;

  /* Diktat */
  private boolean hoert = false;
  private String bestaetigt = "";
  private String vorlaeufig = "";
  private String hinweis;
  private final float[] pegel = new float[9];

  private final Handler takt = new Handler(Looper.getMainLooper());
  private Runnable loeschTakt;

  TastaturAnsicht(Context ctx) {
    super(ctx);
    dichte = ctx.getResources().getDisplayMetrics().density;
    f = Farben.fuer(ctx);
    glas = new Glas(f, dichte);
    hinweis = ctx.getString(R.string.diktat_bereit);

    setBackgroundColor(Color.TRANSPARENT);
    schrift.setTextAlign(Paint.Align.CENTER);
    strich.setStyle(Paint.Style.STROKE);
    strich.setStrokeCap(Paint.Cap.ROUND);
    strich.setStrokeJoin(Paint.Join.ROUND);
    setHapticFeedbackEnabled(true);
  }

  void setHoerer(Hoerer h) { this.hoerer = h; }

  private float dp(float wert) { return wert * dichte; }

  /* ---- Aufbau ------------------------------------------------------- */

  @Override
  protected void onMeasure(int breiteSpec, int hoeheSpec) {
    int breite = MeasureSpec.getSize(breiteSpec);
    int hoehe = Math.round(dp(LEISTE + 4 * (TASTE + LUFT) + STEG));
    setMeasuredDimension(breite, hoehe);
  }

  @Override
  protected void onSizeChanged(int b, int h, int altB, int altH) {
    vermessen(b);
  }

  private void vermessen(int gesamtbreite) {
    float y = dp(LEISTE);
    float tasteH = dp(TASTE), luft = dp(LUFT), rand = dp(RAND);
    for (Taste[] zeile : zeilen) {
      float summe = 0;
      for (Taste t : zeile) summe += t.gewicht;
      float verfuegbar = gesamtbreite - 2 * rand - luft * (zeile.length - 1);
      float x = rand;
      for (Taste t : zeile) {
        float breite = verfuegbar * (t.gewicht / summe);
        t.x = Math.round(x);
        t.y = Math.round(y);
        t.breite = Math.round(x + breite) - t.x;
        t.hoehe = Math.round(tasteH);
        x += breite + luft;
      }
      y += tasteH + luft;
    }
  }

  private void wechsleEbene(int neu) {
    ebene = neu;
    zeilen = Belegung.ebene(neu);
    if (neu != Belegung.BUCHSTABEN) umschalt = 0;
    vermessen(getWidth());
    invalidate();
  }

  /* ---- Zeichnen ------------------------------------------------------ */

  @Override
  protected void onDraw(Canvas c) {
    glas.feld(c, getWidth(), getHeight(), dp(ECKE), weich);
    zeichneLeiste(c);
    for (Taste[] zeile : zeilen) for (Taste t : zeile) zeichneTaste(c, t);
    zeichneSteg(c);
    if (vorschau != null) zeichneVorschau(c, vorschau);
  }

  private void zeichneTaste(Canvas c, Taste t) {
    boolean gedrueckt = zeiger.indexOfValue(t) >= 0;
    Glas.Stil stil;
    switch (t.art) {
      case MIKRO: stil = hoert ? Glas.Stil.MIKRO_HOERT : Glas.Stil.MIKRO; break;
      case LEER:  stil = Glas.Stil.LEER; break;
      case BUCHSTABE: stil = Glas.Stil.NORMAL; break;
      default:    stil = Glas.Stil.NEBEN;
    }

    Bitmap bild = glas.taste(t.breite, t.hoehe, stil, gedrueckt);
    int rand = glas.rand();
    c.drawBitmap(bild, t.x - rand, t.y - rand, null);

    float mx = t.x + t.breite / 2f, my = t.y + t.hoehe / 2f;
    switch (t.art) {
      case BUCHSTABE:
        text(c, beschriftung(t), mx, my, dp(21), f.schrift, false);
        break;
      case EBENE:
        text(c, t.aufschrift, mx, my, dp(15), f.schriftNeben, true);
        break;
      case LEER:
        text(c, getContext().getString(R.string.leertaste), mx, my,
            dp(11.5f), (f.schriftNeben & 0x00FFFFFF) | 0x99000000, true);
        break;
      case UMSCHALT:
        symbol(c, Zeichen.umschalt(), mx, my, dp(22),
            umschalt == 0 ? f.schriftNeben : f.akzent, umschalt == 2);
        break;
      case LOESCHEN:
        symbol(c, Zeichen.loeschen(), mx, my, dp(23), f.schriftNeben, false);
        break;
      case WELT:
        symbol(c, Zeichen.welt(), mx, my, dp(20), f.schriftNeben, false);
        break;
      case EINGABE:
        if (eingabewort != null) text(c, eingabewort, mx, my, dp(13.5f), f.akzent, true);
        else symbol(c, Zeichen.eingabe(), mx, my, dp(21), f.akzent, false);
        break;
      case MIKRO:
        symbol(c, Zeichen.mikro(), mx, my, dp(21), 0xFFFFFFFF, false);
        break;
    }
  }

  /** Was auf einer Buchstabentaste steht — mit Umschalt in Großschrift. */
  private String beschriftung(Taste t) {
    if (ebene != Belegung.BUCHSTABEN || umschalt == 0) return t.aufschrift;
    String gross = t.aufschrift.toUpperCase(java.util.Locale.GERMAN);
    // ß wird groß zu SS — aus einem Anschlag würden zwei Zeichen. Solche
    // Tasten bleiben, wie sie sind.
    return gross.length() == t.aufschrift.length() ? gross : t.aufschrift;
  }

  private void text(Canvas c, String s, float mx, float my, float groesse,
                    int farbe, boolean halbfett) {
    schrift.setTextSize(groesse);
    schrift.setColor(farbe);
    schrift.setFakeBoldText(halbfett);
    Paint.FontMetrics m = schrift.getFontMetrics();
    c.drawText(s, mx, my - (m.ascent + m.descent) / 2f, schrift);
  }

  /** Zeichnet einen Pfad aus dem 24 × 24-Feld mittig auf die Taste. */
  private void symbol(Canvas c, Path p, float mx, float my, float groesse,
                      int farbe, boolean gefuellt) {
    Matrix m = new Matrix();
    float faktor = groesse / 24f;
    m.setScale(faktor, faktor);
    m.postTranslate(mx - groesse / 2f, my - groesse / 2f);
    Path gerechnet = new Path();
    p.transform(m, gerechnet);

    if (gefuellt) {
      flaeche.setColor((farbe & 0x00FFFFFF) | 0x33000000);
      flaeche.setStyle(Paint.Style.FILL);
      c.drawPath(gerechnet, flaeche);
    }
    strich.setColor(farbe);
    strich.setStrokeWidth(dp(1.7f));
    c.drawPath(gerechnet, strich);
  }

  private void zeichneLeiste(Canvas c) {
    float mitte = dp(LEISTE) / 2f;
    boolean amDiktieren = hoert || !bestaetigt.isEmpty() || !vorlaeufig.isEmpty();

    c.save();
    c.clipRect(0, 0, getWidth(), dp(LEISTE));

    float links = dp(12);
    if (amDiktieren) {
      links = zeichnePegel(c, dp(10), mitte) + dp(9);
    }

    schrift.setTextAlign(Paint.Align.LEFT);
    schrift.setTextSize(dp(14.5f));
    schrift.setFakeBoldText(false);
    Paint.FontMetrics m = schrift.getFontMetrics();
    float grundlinie = mitte - (m.ascent + m.descent) / 2f;

    if (amDiktieren) {
      float breiteA = schrift.measureText(bestaetigt);
      float breiteB = schrift.measureText(vorlaeufig);
      float platz = getWidth() - links - dp(12);
      // Beim Diktieren wächst der Text nach rechts; sichtbar bleibt das Ende.
      float x = links + Math.min(0, platz - (breiteA + breiteB));
      schrift.setColor(f.schrift);
      c.drawText(bestaetigt, x, grundlinie, schrift);
      schrift.setColor((f.schrift & 0x00FFFFFF) | 0x73000000);
      c.drawText(vorlaeufig, x + breiteA, grundlinie, schrift);
    } else {
      schrift.setColor(f.schriftNeben);
      c.drawText(hinweis, links, grundlinie, schrift);
    }

    schrift.setTextAlign(Paint.Align.CENTER);
    c.restore();
  }

  /** Die Pegelbalken links in der Leiste. Gibt zurück, wo sie enden. */
  private float zeichnePegel(Canvas c, float links, float mitte) {
    float breite = dp(3), luft = dp(2.5f);
    strich.setColor(f.pegel);
    strich.setStrokeWidth(breite);
    float x = links;
    for (float wert : pegel) {
      float h = dp(3) + dp(15) * Math.max(0f, Math.min(1f, wert));
      c.drawLine(x, mitte - h / 2f, x, mitte + h / 2f, strich);
      x += breite + luft;
    }
    return x - luft;
  }

  private void zeichneSteg(Canvas c) {
    float b = dp(118), h = dp(4.5f);
    float x = (getWidth() - b) / 2f;
    float y = getHeight() - dp(STEG) / 2f - h / 2f;
    strich.setColor(f.steg);
    strich.setStrokeWidth(h);
    c.drawLine(x + h / 2, y + h / 2, x + b - h / 2, y + h / 2, strich);
  }

  private void zeichneVorschau(Canvas c, Taste t) {
    float zusatz = dp(7);
    float breite = t.breite + 2 * zusatz, hoehe = dp(52);
    float x = Math.max(dp(2), Math.min(getWidth() - breite - dp(2), t.x - zusatz));
    float y = Math.max(dp(LEISTE) - dp(8), t.y - hoehe - dp(6));

    c.save();
    c.translate(x, y);
    Path form = Glas.superellipse(breite, hoehe, dp(18));

    flaeche.setStyle(Paint.Style.FILL);
    flaeche.setShader(new LinearGradient(0, 0, 0, hoehe,
        f.gedruecktOben, f.gedruecktUnten, Shader.TileMode.CLAMP));
    c.drawPath(form, flaeche);
    flaeche.setShader(null);

    c.save();
    c.clipPath(form);
    strich.setColor(f.rimOben);
    strich.setStrokeWidth(dp(2.2f));
    c.drawPath(form, strich);
    c.restore();

    text(c, beschriftung(t), breite / 2f, hoehe / 2f, dp(27), f.schrift, false);
    c.restore();
  }

  /* ---- Finger -------------------------------------------------------- */

  @Override
  public boolean onTouchEvent(MotionEvent e) {
    int was = e.getActionMasked();
    switch (was) {
      case MotionEvent.ACTION_DOWN:
      case MotionEvent.ACTION_POINTER_DOWN: {
        int i = e.getActionIndex();
        Taste t = finde((int) e.getX(i), (int) e.getY(i));
        if (t != null) {
          zeiger.put(e.getPointerId(i), t);
          druecken(t);
        }
        return true;
      }
      case MotionEvent.ACTION_MOVE: {
        for (int i = 0; i < e.getPointerCount(); i++) {
          int id = e.getPointerId(i);
          Taste alt = zeiger.get(id);
          if (alt == null) continue;
          Taste neu = finde((int) e.getX(i), (int) e.getY(i));
          // Nur zwischen gewöhnlichen Tasten darf der Finger wandern; wer
          // von Löschen wegrutscht, wollte meist nicht schreiben.
          if (neu != null && neu != alt
              && alt.art == Taste.Art.BUCHSTABE && neu.art == Taste.Art.BUCHSTABE) {
            zeiger.put(id, neu);
            vorschau = neu;
            invalidate();
          }
        }
        return true;
      }
      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_POINTER_UP: {
        int id = e.getPointerId(e.getActionIndex());
        Taste t = zeiger.get(id);
        zeiger.remove(id);
        if (t != null) {
          loslassen(t);
          ausloesen(t);
        }
        invalidate();
        return true;
      }
      case MotionEvent.ACTION_CANCEL:
        for (int i = 0; i < zeiger.size(); i++) loslassen(zeiger.valueAt(i));
        zeiger.clear();
        vorschau = null;
        invalidate();
        return true;
    }
    return super.onTouchEvent(e);
  }

  private Taste finde(int x, int y) {
    Taste naechste = null;
    int besterAbstand = Integer.MAX_VALUE;
    for (Taste[] zeile : zeilen) {
      for (Taste t : zeile) {
        if (t.trifft(x, y)) return t;
        // Die Luft zwischen den Tasten gehört der nächstgelegenen — sonst
        // fällt jeder dritte Anschlag am Rand ins Leere.
        if (y >= t.y - dp(LUFT) && y < t.y + t.hoehe + dp(LUFT)) {
          int a = t.abstand(x, y);
          if (a < besterAbstand) { besterAbstand = a; naechste = t; }
        }
      }
    }
    return naechste;
  }

  private void druecken(Taste t) {
    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
    if (t.art == Taste.Art.BUCHSTABE && t.breite < dp(60)) vorschau = t;
    if (t.art == Taste.Art.LOESCHEN) starteLoeschTakt();
    if (t.art == Taste.Art.WELT) starteLangenDruck(t);
    invalidate();
  }

  private void loslassen(Taste t) {
    if (vorschau == t) vorschau = null;
    if (t.art == Taste.Art.LOESCHEN) haltLoeschTakt();
    if (langerDruck != null) { takt.removeCallbacks(langerDruck); langerDruck = null; }
  }

  /** Weltkugel kurz: nächste Tastatur. Lang: die ganze Auswahl. */
  private void starteLangenDruck(final Taste t) {
    langerDruck = () -> {
      erledigt = t;
      performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
      if (hoerer != null) hoerer.tastaturAuswahl();
    };
    takt.postDelayed(langerDruck, 450);
  }

  private void ausloesen(Taste t) {
    if (hoerer == null) return;
    if (erledigt == t) { erledigt = null; return; }
    switch (t.art) {
      case BUCHSTABE:
        String s = beschriftung(t);
        hoerer.schreibe(s);
        if (umschalt == 1) { umschalt = 0; invalidate(); }
        break;
      case LEER:
        hoerer.schreibe(" ");
        if (umschalt == 1) { umschalt = 0; invalidate(); }
        break;
      case LOESCHEN:
        // Der erste Anschlag löscht sofort; der Takt übernimmt beim Halten.
        break;
      case UMSCHALT: {
        long jetzt = System.currentTimeMillis();
        if (umschalt == 2) umschalt = 0;
        else if (umschalt == 1 && jetzt - letzteUmschaltung < 400) umschalt = 2;
        else umschalt = 1;
        letzteUmschaltung = jetzt;
        invalidate();
        break;
      }
      case EBENE:
        wechsleEbene(t.ebene);
        break;
      case WELT:
        hoerer.naechsteTastatur();
        break;
      case MIKRO:
        hoerer.mikrofon();
        break;
      case EINGABE:
        hoerer.eingabe();
        break;
    }
  }

  /* Löschen: erster Anschlag sofort, dann nach kurzem Halten Schlag auf Schlag. */
  private void starteLoeschTakt() {
    if (hoerer != null) hoerer.loesche();
    haltLoeschTakt();
    loeschTakt = new Runnable() {
      @Override public void run() {
        if (hoerer != null) hoerer.loesche();
        takt.postDelayed(this, 55);
      }
    };
    takt.postDelayed(loeschTakt, 380);
  }

  private void haltLoeschTakt() {
    if (loeschTakt != null) takt.removeCallbacks(loeschTakt);
    loeschTakt = null;
  }

  /* ---- Zustände von außen -------------------------------------------- */

  void setzeGrossschreibung(boolean gross) {
    if (umschalt == 2) return;         // Feststellung schlägt die Automatik
    int neu = gross ? 1 : 0;
    if (neu != umschalt) { umschalt = neu; invalidate(); }
  }

  void zurueckAufBuchstaben() {
    if (ebene != Belegung.BUCHSTABEN) wechsleEbene(Belegung.BUCHSTABEN);
  }

  void diktatBegonnen() {
    hoert = true;
    bestaetigt = "";
    vorlaeufig = "";
    java.util.Arrays.fill(pegel, 0f);
    invalidate();
  }

  void diktatText(String fest, String offen) {
    bestaetigt = fest == null ? "" : fest;
    vorlaeufig = offen == null ? "" : offen;
    invalidate();
  }

  void diktatBeendet(String schlusshinweis) {
    hoert = false;
    bestaetigt = "";
    vorlaeufig = "";
    if (schlusshinweis != null) hinweis = schlusshinweis;
    invalidate();
  }

  void setzeHinweis(String text) {
    hinweis = text;
    invalidate();
  }

  void setzeEingabewort(String wort) {
    if (wort != null && wort.length() > 8) wort = wort.substring(0, 8);
    eingabewort = wort;
    invalidate();
  }

  /**
   * Kann Android den Untergrund weichzeichnen? Wenn nicht — altes Gerät,
   * Stromsparen, abgeschaltet —, muss das Feld selbst mehr decken, sonst
   * steht die Schrift auf dem blanken Inhalt der App darunter.
   */
  void setzeWeichzeichner(boolean moeglich) {
    weich = moeglich;
    invalidate();
  }

  /** Lautstärke der Spracherkennung, −2 … 10 laut Android. */
  void setzePegel(float rms) {
    float wert = Math.max(0f, Math.min(1f, (rms + 2f) / 12f));
    System.arraycopy(pegel, 1, pegel, 0, pegel.length - 1);
    pegel[pegel.length - 1] = wert;
    invalidate(0, 0, getWidth(), (int) dp(LEISTE));
  }

  void aufraeumen() {
    haltLoeschTakt();
    glas.leeren();
  }
}
