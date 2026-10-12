package com.example.overlayrunnergame;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

/**
 * De setup-pagina. Hier zet je de drie dingen aan die het paneel nodig heeft,
 * met per stap een duidelijke kleur: grijs = nog doen, groen = klaar.
 */
public class MainActivity extends Activity {

    private static final String KLUIS_PAKKET = "com.samsung.knox.securefolder";

    private TextView stapOverlay;
    private TextView stapToegankelijkheid;
    private TextView stapKluis;
    private TextView samenvatting;
    private TextView restrictedHint;

    private GradientDrawable vormOverlay;
    private GradientDrawable vormToegankelijkheid;
    private GradientDrawable vormKluis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        stapOverlay = findViewById(R.id.stap_overlay);
        stapToegankelijkheid = findViewById(R.id.stap_toegankelijkheid);
        stapKluis = findViewById(R.id.stap_kluis);
        samenvatting = findViewById(R.id.samenvatting);
        restrictedHint = findViewById(R.id.restricted_hint);

        vormOverlay = maakStapKnop(stapOverlay, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!Settings.canDrawOverlays(MainActivity.this)) {
                    try {
                        startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + getPackageName())));
                    } catch (Exception e) {
                        startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION));
                    }
                }
            }
        });

        vormToegankelijkheid = maakStapKnop(stapToegankelijkheid, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            }
        });

        vormKluis = maakStapKnop(stapKluis, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openKluis();
            }
        });

        restrictedHint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                } catch (Exception e) {
                    startActivity(new Intent(Settings.ACTION_SETTINGS));
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkMachtigingen();
    }

    /**
     * Geeft de knop een ronde vorm met tik-rimpel en onthoudt de vorm, zodat we
     * de kleur later kunnen omzetten zonder de ronde hoeken kwijt te raken.
     */
    private GradientDrawable maakStapKnop(TextView knop, View.OnClickListener actie) {
        GradientDrawable vorm = Ui.vorm(this, Ui.kleur(this, R.color.fs_uit), 20f);
        knop.setBackground(Ui.tikbaar(this, vorm, 20f));
        knop.setPadding(Ui.px(this, 20), 0, Ui.px(this, 20), 0);
        knop.setFocusable(true);
        knop.setOnClickListener(actie);
        return vorm;
    }

    private void checkMachtigingen() {
        boolean overlay = Settings.canDrawOverlays(this);
        boolean toegankelijkheid = isToegankelijkheidAan(this);
        boolean kluis = isKluisAanwezig();

        zetStap(stapOverlay, vormOverlay, overlay, "1", getString(R.string.stap_overlay));
        zetStap(stapToegankelijkheid, vormToegankelijkheid, toegankelijkheid, "2",
                getString(R.string.stap_toegankelijkheid));
        zetStap(stapKluis, vormKluis, kluis, "3", getString(R.string.stap_kluis));

        int klaar = (overlay ? 1 : 0) + (toegankelijkheid ? 1 : 0) + (kluis ? 1 : 0);
        samenvatting.setText(getString(R.string.samenvatting, klaar));
    }

    private void zetStap(TextView knop, GradientDrawable vorm, boolean aan,
                         String nummer, String tekst) {
        vorm.setColor(Ui.kleur(this, aan ? R.color.fs_succes : R.color.fs_uit));
        knop.setText(aan ? "✓  " + tekst : nummer + ".  " + tekst);
    }

    private void openKluis() {
        try {
            Intent intent = getPackageManager().getLaunchIntentForPackage(KLUIS_PAKKET);
            if (intent != null) {
                startActivity(intent);
            } else {
                startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS));
            }
        } catch (Exception e) {
            try {
                startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS));
            } catch (Exception ignored) {
                // niets meer aan te doen
            }
        }
    }

    private boolean isKluisAanwezig() {
        try {
            getPackageManager().getPackageInfo(KLUIS_PAKKET, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private boolean isToegankelijkheidAan(Context context) {
        String service = getPackageName() + "/" + MijnService.class.getCanonicalName();
        int toegankelijkheidAan;
        try {
            toegankelijkheidAan = Settings.Secure.getInt(context.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            return false;
        }
        if (toegankelijkheidAan != 1) {
            return false;
        }
        String lijst = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (lijst == null) {
            return false;
        }
        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
        splitter.setString(lijst);
        while (splitter.hasNext()) {
            if (splitter.next().equalsIgnoreCase(service)) {
                return true;
            }
        }
        return false;
    }
}
