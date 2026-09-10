package de.mumafi.tastatur;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.GradientDrawable;
import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;

/**
 * Die Eingabemethode: verbindet die Ansicht mit dem Textfeld und dem Diktat.
 */
public final class TastaturDienst extends InputMethodService
    implements TastaturAnsicht.Hoerer, Diktat.Rueckmeldung {

  private TastaturAnsicht ansicht;
  private Diktat diktat;
  private long letztesLeerzeichen = 0;

  @Override
  public void onCreate() {
    super.onCreate();
    diktat = new Diktat(this, this);
  }

  @Override
  public View onCreateInputView() {
    if (ansicht != null) ansicht.aufraeumen();
    ansicht = new TastaturAnsicht(this);
    ansicht.setHoerer(this);
    ansicht.setzeWeichzeichner(weichzeichnerMoeglich());
    return ansicht;
  }

  @Override
  public void onWindowShown() {
    super.onWindowShown();
    fensterAlsGlas();
  }

  @Override
  public void onStartInputView(EditorInfo info, boolean neustart) {
    super.onStartInputView(info, neustart);
    if (ansicht == null) return;
    ansicht.zurueckAufBuchstaben();
    ansicht.setzeEingabewort(eingabewort(info));
    ansicht.setzeHinweis(getString(R.string.diktat_bereit));
    pruefeGrossschreibung();
  }

  @Override
  public void onFinishInputView(boolean fertig) {
    super.onFinishInputView(fertig);
    diktat.beenden(null);
  }

  @Override
  public void onDestroy() {
    diktat.aufraeumen();
    if (ansicht != null) ansicht.aufraeumen();
    super.onDestroy();
  }

  @Override
  public void onConfigurationChanged(Configuration neu) {
    super.onConfigurationChanged(neu);
    // Hell und dunkel wechseln über die Konfiguration; die Farbwerte stecken
    // in der Ansicht, also muss sie neu gebaut werden.
    setInputView(onCreateInputView());
  }

  @Override
  public void onUpdateSelection(int altAnfang, int altEnde, int neuAnfang, int neuEnde,
                                int kandidatAnfang, int kandidatEnde) {
    super.onUpdateSelection(altAnfang, altEnde, neuAnfang, neuEnde,
        kandidatAnfang, kandidatEnde);
    pruefeGrossschreibung();
  }

  /** Im Querformat blendet Android sonst ein eigenes Textfeld ein — das passt
      weder zur Glasfläche noch zum Diktat. */
  @Override
  public boolean onEvaluateFullscreenMode() {
    return false;
  }

  /* ---- Das Fenster --------------------------------------------------- */

  private boolean weichzeichnerMoeglich() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false;
    WindowManager wm = getSystemService(WindowManager.class);
    return wm != null && wm.isCrossWindowBlurEnabled();
  }

  /**
   * Macht das Fenster durchsichtig und lässt Android den Untergrund
   * weichzeichnen. Der Weichzeichner richtet sich nach der Deckkraft des
   * Fensterhintergrunds — deshalb ist der eine Form mit runden Schultern und
   * nicht einfach durchsichtig: sonst läge die Unschärfe auch in den Ecken.
   */
  private void fensterAlsGlas() {
    Window fenster = getWindow() == null ? null : getWindow().getWindow();
    if (fenster == null) return;

    float ecke = 28 * getResources().getDisplayMetrics().density;
    GradientDrawable grund = new GradientDrawable();
    grund.setShape(GradientDrawable.RECTANGLE);
    grund.setColor(0x01000000);
    grund.setCornerRadii(new float[] { ecke, ecke, ecke, ecke, 0, 0, 0, 0 });
    fenster.setBackgroundDrawable(grund);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      fenster.setBackgroundBlurRadius(
          (int) (36 * getResources().getDisplayMetrics().density));
    }
  }

  /* ---- Was die Tasten auslösen --------------------------------------- */

  @Override
  public void schreibe(String text) {
    InputConnection ic = getCurrentInputConnection();
    if (ic == null) return;

    if (" ".equals(text)) {
      long jetzt = System.currentTimeMillis();
      // Zweimal Leertaste hintereinander macht einen Punkt — wie überall.
      CharSequence davor = ic.getTextBeforeCursor(2, 0);
      if (davor != null && davor.length() == 2 && davor.charAt(1) == ' '
          && Character.isLetterOrDigit(davor.charAt(0))
          && jetzt - letztesLeerzeichen < 700) {
        ic.deleteSurroundingText(1, 0);
        ic.commitText(". ", 1);
        letztesLeerzeichen = 0;
        pruefeGrossschreibung();
        return;
      }
      letztesLeerzeichen = jetzt;
    }

    ic.commitText(text, 1);
    pruefeGrossschreibung();
  }

  @Override
  public void loesche() {
    InputConnection ic = getCurrentInputConnection();
    if (ic == null) return;
    CharSequence markiert = ic.getSelectedText(0);
    if (!TextUtils.isEmpty(markiert)) {
      ic.commitText("", 1);
    } else {
      // Ein Emoji besteht aus zwei Zeichen — halbe Emoji sind keine.
      CharSequence davor = ic.getTextBeforeCursor(2, 0);
      int anzahl = (davor != null && davor.length() == 2
          && Character.isSurrogatePair(davor.charAt(0), davor.charAt(1))) ? 2 : 1;
      ic.deleteSurroundingText(anzahl, 0);
    }
    pruefeGrossschreibung();
  }

  @Override
  public void eingabe() {
    InputConnection ic = getCurrentInputConnection();
    if (ic == null) return;
    EditorInfo info = getCurrentInputEditorInfo();
    int tat = info == null ? EditorInfo.IME_ACTION_NONE
        : info.imeOptions & EditorInfo.IME_MASK_ACTION;
    boolean eigeneZeile = info != null
        && (info.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0;

    if (!eigeneZeile && tat != EditorInfo.IME_ACTION_NONE
        && tat != EditorInfo.IME_ACTION_UNSPECIFIED) {
      ic.performEditorAction(tat);
    } else {
      ic.commitText("\n", 1);
      pruefeGrossschreibung();
    }
  }

  @Override
  public void naechsteTastatur() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      switchToNextInputMethod(false);
    } else {
      tastaturAuswahl();
    }
  }

  @Override
  public void tastaturAuswahl() {
    InputMethodManager imm = getSystemService(InputMethodManager.class);
    if (imm != null) imm.showInputMethodPicker();
  }

  @Override
  public void mikrofon() {
    diktat.umschalten();
  }

  /** Am Satzanfang schreibt die Tastatur von selbst groß. */
  private void pruefeGrossschreibung() {
    InputConnection ic = getCurrentInputConnection();
    EditorInfo info = getCurrentInputEditorInfo();
    if (ic == null || info == null || ansicht == null) return;
    ansicht.setzeGrossschreibung(ic.getCursorCapsMode(info.inputType) != 0);
  }

  /** „Senden“, „Suchen“, „Weiter“ — was die App von der Eingabetaste erwartet. */
  private String eingabewort(EditorInfo info) {
    if (info == null) return null;
    if ((info.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0) return null;
    if (info.actionLabel != null) return info.actionLabel.toString();
    switch (info.imeOptions & EditorInfo.IME_MASK_ACTION) {
      case EditorInfo.IME_ACTION_SEND:   return "Senden";
      case EditorInfo.IME_ACTION_SEARCH: return "Suchen";
      case EditorInfo.IME_ACTION_GO:     return "Los";
      case EditorInfo.IME_ACTION_NEXT:   return "Weiter";
      case EditorInfo.IME_ACTION_DONE:   return "Fertig";
      default: return null;
    }
  }

  /* ---- Was das Diktat meldet ------------------------------------------ */

  @Override
  public void diktatBegonnen() {
    if (ansicht != null) ansicht.diktatBegonnen();
  }

  @Override
  public void diktatTeil(String text) {
    InputConnection ic = getCurrentInputConnection();
    if (ic != null) ic.setComposingText(text, 1);
    if (ansicht != null) ansicht.diktatText("", text);
  }

  @Override
  public void diktatErgebnis(String text) {
    InputConnection ic = getCurrentInputConnection();
    if (ic != null) {
      ic.setComposingText("", 1);
      ic.finishComposingText();
      ic.commitText(text + " ", 1);
    }
    if (ansicht != null) ansicht.diktatText(text, "");
    pruefeGrossschreibung();
  }

  @Override
  public void diktatPegel(float rms) {
    if (ansicht != null) ansicht.setzePegel(rms);
  }

  @Override
  public void diktatBeendet(String hinweis) {
    InputConnection ic = getCurrentInputConnection();
    if (ic != null) ic.finishComposingText();
    if (ansicht != null) {
      ansicht.diktatBeendet(hinweis != null ? hinweis : getString(R.string.diktat_bereit));
    }
  }

  @Override
  public void diktatBrauchtErlaubnis() {
    if (ansicht != null) ansicht.setzeHinweis(getString(R.string.diktat_kein_mikrofon));
    // Eine Eingabemethode darf selbst keinen Erlaubnis-Dialog öffnen; dafür
    // gibt es die durchsichtige Aktivität.
    Intent absicht = new Intent(this, BerechtigungActivity.class);
    absicht.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
    startActivity(absicht);
  }
}
