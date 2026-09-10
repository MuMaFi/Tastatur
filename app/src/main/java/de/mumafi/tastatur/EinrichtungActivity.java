package de.mumafi.tastatur;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;

/**
 * Die Seite, die beim Antippen des Symbols erscheint: drei Schritte, bis die
 * Tastatur benutzbar ist, und ein Feld zum Ausprobieren.
 */
public final class EinrichtungActivity extends Activity {

  private static final int MIKROFON = 1;

  private Button knopfMikrofon;

  @Override
  protected void onCreate(Bundle stand) {
    super.onCreate(stand);
    setContentView(R.layout.einrichtung);

    findViewById(R.id.knopf_einstellungen).setOnClickListener(v ->
        startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)));

    findViewById(R.id.knopf_auswahl).setOnClickListener(v -> {
      InputMethodManager imm = getSystemService(InputMethodManager.class);
      if (imm != null) imm.showInputMethodPicker();
    });

    knopfMikrofon = findViewById(R.id.knopf_mikrofon);
    knopfMikrofon.setOnClickListener(v ->
        requestPermissions(new String[] { Manifest.permission.RECORD_AUDIO }, MIKROFON));
  }

  @Override
  protected void onResume() {
    super.onResume();
    zeigeMikrofonstand();
  }

  @Override
  public void onRequestPermissionsResult(int nummer, String[] rechte, int[] ergebnis) {
    zeigeMikrofonstand();
  }

  private void zeigeMikrofonstand() {
    boolean erlaubt = checkSelfPermission(Manifest.permission.RECORD_AUDIO)
        == PackageManager.PERMISSION_GRANTED;
    knopfMikrofon.setText(erlaubt ? R.string.schritt_3_erledigt : R.string.schritt_3_knopf);
    knopfMikrofon.setEnabled(!erlaubt);
    knopfMikrofon.setAlpha(erlaubt ? 0.55f : 1f);
  }
}
