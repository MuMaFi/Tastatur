package de.mumafi.tastatur;

import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;

import java.util.HashMap;

/**
 * Zeichnet das Glas: die Form der Tasten und ihre Lichtführung.
 *
 * Jede Taste wird einmal in ein Bild gezeichnet und danach nur noch kopiert.
 * Das hat einen handfesten Grund: der Schlagschatten braucht einen Weichzeichner,
 * und der läuft nur auf der weichen Leinwand. Würde die Ansicht selbst weich
 * zeichnen, ginge die Beschleunigung für die ganze Tastatur verloren — so
 * bleibt sie erhalten, weil zur Laufzeit nur fertige Bilder gesetzt werden.
 * Verschiedene Größen gibt es ohnehin nur eine Handvoll.
 */
final class Glas {

  enum Stil { NORMAL, NEBEN, LEER, MIKRO, MIKRO_HOERT }

  private final Farben f;
  private final float dichte;
  private final HashMap<String, Bitmap> vorrat = new HashMap<>();

  Glas(Farben farben, float dichte) {
    this.f = farben;
    this.dichte = dichte;
  }

  private float dp(float wert) { return wert * dichte; }

  /**
   * Superellipse statt Rechteck mit Kreisecken. Der Kreis setzt an der
   * Geraden mit einem sichtbaren Knick an; bei elf Tasten nebeneinander
   * sieht man nichts anderes mehr. Der Exponent 5 kommt der Form nahe, die
   * Apple „continuous corner“ nennt.
   */
  static Path superellipse(float b, float h, float r) {
    r = Math.min(r, Math.min(b, h) / 2f);
    final int schritte = 16;
    Path p = new Path();
    // Ecke für Ecke im Uhrzeigersinn; jeder Bogen läuft von einer Kante zur
    // nächsten, damit die vier Stücke ohne Sprung aneinanderpassen.
    bogen(p, r, r, -1, -1, false, r, schritte);
    bogen(p, b - r, r, 1, -1, true, r, schritte);
    bogen(p, b - r, h - r, 1, 1, false, r, schritte);
    bogen(p, r, h - r, -1, 1, true, r, schritte);
    p.close();
    return p;
  }

  private static void bogen(Path p, float mx, float my, int rx, int ry,
                            boolean zurueck, float r, int schritte) {
    for (int i = 0; i <= schritte; i++) {
      int k = zurueck ? schritte - i : i;
      double t = (k / (double) schritte) * (Math.PI / 2);
      float dx = (float) (r * Math.pow(Math.cos(t), 0.4));  // 2/n mit n = 5
      float dy = (float) (r * Math.pow(Math.sin(t), 0.4));
      float x = mx + rx * dx, y = my + ry * dy;
      if (p.isEmpty()) p.moveTo(x, y); else p.lineTo(x, y);
    }
  }

  /** Rand um das Tastenbild — Platz für Schatten und, beim Mikrofon, Schein. */
  int rand() { return (int) Math.ceil(dp(9)); }

  Bitmap taste(int breite, int hoehe, Stil stil, boolean gedrueckt) {
    String schluessel = breite + "×" + hoehe + "·" + stil + (gedrueckt ? "·g" : "");
    Bitmap fertig = vorrat.get(schluessel);
    if (fertig != null) return fertig;

    int rand = rand();
    Bitmap bild = Bitmap.createBitmap(breite + 2 * rand, hoehe + 2 * rand,
        Bitmap.Config.ARGB_8888);
    Canvas c = new Canvas(bild);
    c.translate(rand, rand);

    float radius = dp(15);
    Path form = superellipse(breite, hoehe, radius);

    int oben, unten;
    switch (stil) {
      case MIKRO:       oben = f.mikroOben; unten = f.mikroUnten; break;
      case MIKRO_HOERT: oben = f.hoertOben; unten = f.hoertUnten; break;
      case NEBEN:
      case LEER:        oben = f.nebenOben; unten = f.nebenUnten; break;
      default:          oben = f.tasteOben; unten = f.tasteUnten;
    }
    if (gedrueckt && stil != Stil.MIKRO && stil != Stil.MIKRO_HOERT) {
      oben = f.gedruecktOben;
      unten = f.gedruecktUnten;
    }

    Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    // 1. Der Fall. Zwei Lagen: eine enge dunkle an der Kante, eine weite
    //    weiche darunter — so löst sich die Taste vom Feld, ohne zu kleben.
    p.setStyle(Paint.Style.FILL);
    p.setColor(f.fall);
    p.setMaskFilter(new BlurMaskFilter(dp(3), BlurMaskFilter.Blur.NORMAL));
    c.save();
    c.translate(0, dp(1.5f));
    c.drawPath(form, p);
    c.restore();
    p.setMaskFilter(null);

    // 2. Beim Hören liegt ein farbiger Schein um die Mikrofontaste.
    if (stil == Stil.MIKRO_HOERT) {
      p.setColor((f.pegel & 0x00FFFFFF) | 0x66000000);
      p.setMaskFilter(new BlurMaskFilter(dp(7), BlurMaskFilter.Blur.NORMAL));
      c.drawPath(form, p);
      p.setMaskFilter(null);
    }

    // 3. Die Füllung.
    p.setShader(new LinearGradient(0, 0, 0, hoehe, oben, unten, Shader.TileMode.CLAMP));
    c.drawPath(form, p);
    p.setShader(null);

    // Alles Weitere liegt innerhalb der Form.
    c.save();
    c.clipPath(form);

    // 4. Der Glanz: Licht von oben, über der oberen Hälfte.
    float glanzHoehe = hoehe * 0.58f;
    p.setShader(new LinearGradient(0, 0, 0, glanzHoehe,
        f.glanz, f.glanz & 0x00FFFFFF, Shader.TileMode.CLAMP));
    c.drawRect(0, 0, breite, glanzHoehe, p);
    p.setShader(null);

    // 5. Die Kante: oben Licht, unten Schatten. Ein Verlauf im Strich
    //    erspart zwei getrennte Striche und trifft die Rundung genauer.
    p.setStyle(Paint.Style.STROKE);
    p.setStrokeWidth(dp(2.2f));   // die Hälfte liegt außerhalb und wird beschnitten
    p.setShader(new LinearGradient(0, 0, 0, hoehe,
        new int[] { f.rimOben, f.rimOben & 0x00FFFFFF, f.rimUnten },
        new float[] { 0f, 0.5f, 1f }, Shader.TileMode.CLAMP));
    c.drawPath(form, p);
    p.setShader(null);

    // 6. Die Brechung: das Licht sammelt sich an der unteren Innenkante.
    float y = hoehe - dp(2.2f);
    p.setStrokeWidth(dp(1.2f));
    p.setStrokeCap(Paint.Cap.ROUND);
    p.setShader(new LinearGradient(breite * 0.14f, 0, breite * 0.86f, 0,
        new int[] { f.brechung & 0x00FFFFFF, f.brechung, f.brechung & 0x00FFFFFF },
        new float[] { 0f, 0.5f, 1f }, Shader.TileMode.CLAMP));
    c.drawLine(breite * 0.16f, y, breite * 0.84f, y, p);
    p.setShader(null);

    c.restore();

    vorrat.put(schluessel, bild);
    return bild;
  }

  /** Der Grundton des Tastaturfelds. Den Weichzeichner dahinter setzt das Fenster. */
  void feld(Canvas c, float breite, float hoehe, float ecke, boolean weich) {
    Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    Path form = new Path();
    RectF r = new RectF(0, 0, breite, hoehe);
    form.addRoundRect(r, new float[] { ecke, ecke, ecke, ecke, 0, 0, 0, 0 },
        Path.Direction.CW);

    p.setShader(new LinearGradient(0, 0, 0, hoehe,
        deckender(f.panelOben, weich), deckender(f.panelUnten, weich),
        Shader.TileMode.CLAMP));
    c.drawPath(form, p);
    p.setShader(null);

    // Lichtkante an der Oberkante — die Linie, an der das Glas anfängt.
    c.save();
    c.clipPath(form);
    p.setStyle(Paint.Style.STROKE);
    p.setStrokeWidth(dp(2));
    p.setColor(f.panelLichtkante);
    c.drawPath(form, p);
    c.restore();
  }

  /** Ohne Weichzeichner im Rücken muss die Farbe allein für Ruhe sorgen. */
  private static int deckender(int farbe, boolean weich) {
    if (weich) return farbe;
    int alpha = (farbe >>> 24);
    alpha = Math.min(255, alpha + (255 - alpha) * 3 / 5);
    return (alpha << 24) | (farbe & 0x00FFFFFF);
  }

  void leeren() {
    for (Bitmap b : vorrat.values()) b.recycle();
    vorrat.clear();
  }
}
