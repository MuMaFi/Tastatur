package de.mumafi.tastatur;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;

/**
 * Fragt nach dem Mikrofon und verschwindet wieder.
 *
 * Eine Eingabemethode kann das nicht selbst: Erlaubnisse hängen an einer
 * Aktivität. Diese hier hat kein Fenster und keinen Übergang, damit von ihr
 * nichts zu sehen ist außer dem Systemdialog.
 */
public final class BerechtigungActivity extends Activity {

  private static final int MIKROFON = 1;

  @Override
  protected void onCreate(Bundle stand) {
    super.onCreate(stand);
    overridePendingTransition(0, 0);
    if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
        == PackageManager.PERMISSION_GRANTED) {
      finish();
      return;
    }
    requestPermissions(new String[] { Manifest.permission.RECORD_AUDIO }, MIKROFON);
  }

  @Override
  public void onRequestPermissionsResult(int nummer, String[] rechte, int[] ergebnis) {
    finish();
    overridePendingTransition(0, 0);
  }
}
