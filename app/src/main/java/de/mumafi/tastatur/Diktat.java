package de.mumafi.tastatur;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;

/**
 * Das Diktat. Kapselt Androids Spracherkennung und hält sie am Laufen, bis
 * jemand sie ausschaltet.
 *
 * Die Erkennung meldet sich nach jedem Satz von selbst ab — auch mitten im
 * Sprechen, wenn jemand kurz Luft holt. Wer diktiert, erwartet aber, dass es
 * weitergeht. Deshalb startet sie hier nach jedem Ergebnis neu; nur echte
 * Fehler beenden das Diktat.
 */
final class Diktat implements RecognitionListener {

  interface Rueckmeldung {
    void diktatBegonnen();
    /** Zwischenstand — steht noch nicht fest. */
    void diktatTeil(String text);
    /** Fertiger Satz; kommt ins Textfeld. */
    void diktatErgebnis(String text);
    void diktatPegel(float rms);
    /** Ende. {@code hinweis} ist null, wenn nichts zu melden ist. */
    void diktatBeendet(String hinweis);
    void diktatBrauchtErlaubnis();
  }

  private final Context ctx;
  private final Rueckmeldung rueck;
  private final Handler haupt = new Handler(Looper.getMainLooper());

  private SpeechRecognizer erkenner;
  private boolean laeuft = false;
  /** Wie oft nacheinander nichts verstanden wurde — irgendwann ist Schluss. */
  private int leerlauf = 0;

  Diktat(Context ctx, Rueckmeldung rueck) {
    this.ctx = ctx;
    this.rueck = rueck;
  }

  boolean laeuft() { return laeuft; }

  boolean darfHoeren() {
    return ctx.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
        == PackageManager.PERMISSION_GRANTED;
  }

  void umschalten() {
    if (laeuft) beenden(null); else beginnen();
  }

  void beginnen() {
    if (laeuft) return;
    if (!darfHoeren()) { rueck.diktatBrauchtErlaubnis(); return; }
    if (!SpeechRecognizer.isRecognitionAvailable(ctx)) {
      rueck.diktatBeendet(ctx.getString(R.string.diktat_kein_dienst));
      return;
    }
    if (erkenner == null) {
      erkenner = SpeechRecognizer.createSpeechRecognizer(ctx);
      erkenner.setRecognitionListener(this);
    }
    laeuft = true;
    leerlauf = 0;
    rueck.diktatBegonnen();
    hoeren();
  }

  void beenden(String hinweis) {
    if (!laeuft && erkenner == null) return;
    laeuft = false;
    haupt.removeCallbacksAndMessages(null);
    if (erkenner != null) erkenner.cancel();
    rueck.diktatBeendet(hinweis);
  }

  void aufraeumen() {
    laeuft = false;
    haupt.removeCallbacksAndMessages(null);
    if (erkenner != null) {
      erkenner.destroy();
      erkenner = null;
    }
  }

  private void hoeren() {
    Intent absicht = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
    absicht.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
    absicht.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "de-DE");
    absicht.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
    absicht.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, ctx.getPackageName());
    try {
      erkenner.startListening(absicht);
    } catch (SecurityException e) {
      beenden(ctx.getString(R.string.diktat_kein_mikrofon));
    }
  }

  /** Nach einem Satz oder einer Sprechpause von vorn — aber nicht sofort. */
  private void neuStarten(long wartenMs) {
    if (!laeuft) return;
    haupt.postDelayed(() -> { if (laeuft) hoeren(); }, wartenMs);
  }

  private static String ersterTreffer(Bundle daten) {
    if (daten == null) return null;
    ArrayList<String> texte = daten.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
    if (texte == null || texte.isEmpty()) return null;
    return texte.get(0);
  }

  /* ---- Meldungen der Erkennung --------------------------------------- */

  @Override public void onReadyForSpeech(Bundle p) { }
  @Override public void onBeginningOfSpeech() { leerlauf = 0; }
  @Override public void onEndOfSpeech() { }
  @Override public void onBufferReceived(byte[] p) { }
  @Override public void onEvent(int art, Bundle p) { }

  @Override public void onRmsChanged(float rms) {
    if (laeuft) rueck.diktatPegel(rms);
  }

  @Override public void onPartialResults(Bundle daten) {
    String text = ersterTreffer(daten);
    if (laeuft && text != null && !text.isEmpty()) rueck.diktatTeil(text);
  }

  @Override public void onResults(Bundle daten) {
    if (!laeuft) return;
    String text = ersterTreffer(daten);
    if (text != null && !text.isEmpty()) {
      leerlauf = 0;
      rueck.diktatErgebnis(text);
    }
    neuStarten(120);
  }

  @Override public void onError(int fehler) {
    if (!laeuft) return;
    switch (fehler) {
      case SpeechRecognizer.ERROR_NO_MATCH:
      case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
        // Eine Sprechpause ist kein Fehler. Erst wenn dreimal nacheinander
        // nichts kam, hat sich das Diktat erledigt.
        if (++leerlauf >= 3) beenden(ctx.getString(R.string.diktat_nichts_verstanden));
        else neuStarten(180);
        break;
      case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
        neuStarten(400);
        break;
      case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
        beenden(ctx.getString(R.string.diktat_kein_mikrofon));
        break;
      case SpeechRecognizer.ERROR_NETWORK:
      case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
        beenden(ctx.getString(R.string.diktat_kein_netz));
        break;
      default:
        beenden(ctx.getString(R.string.diktat_fehler));
    }
  }
}
