package nl.ven1x.familystop;

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
 * De setup-pagina. Hier zet je aan wat het paneel nodig heeft.
 *
 * Er is één stap die echt moet (toegankelijkheid) en één optionele stap
 * (de kluis). De optionele stap telt niet mee voor "klaar", want anders blijft
 * de pagina voor altijd onaf uit zien als je toestel die functie niet heeft.
 */
public class MainActivity extends Activity {

    /** Samsung Secure Folder. Wordt later vervangen door onze eigen kluis. */
    private static final String KLUIS_PAKKET = "com.samsung.knox.securefolder";

    private TextView stapToegankelijkheid;
    private TextView stapKluis;
    private TextView samenvatting;
    private TextView restrictedHint;

    private GradientDrawable vormToegankelijkheid;
    private GradientDrawable vormKluis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        stapToegankelijkheid = findViewById(R.id.stap_toegankelijkheid);
        stapKluis = findViewById(R.id.stap_kluis);
        samenvatting = findViewById(R.id.samenvatting);
        restrictedHint = findViewById(R.id.restricted_hint);

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
                    startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:" + getPackageName())));
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
     * Geeft de knop een ronde vorm met tik-rimpel. De vorm wordt onthouden,
     * zodat we de kleur later kunnen omzetten zonder de ronde hoeken kwijt
     * te raken.
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
        boolean toegankelijkheid = isToegankelijkheidAan(this);
        boolean kluis = isKluisAanwezig();

        zetStap(stapToegankelijkheid, vormToegankelijkheid, toegankelijkheid, "1",
                getString(R.string.stap_toegankelijkheid));
        zetStap(stapKluis, vormKluis, kluis, "2", getString(R.string.stap_kluis));

        samenvatting.setText(getString(toegankelijkheid
                ? R.string.samenvatting_klaar
                : R.string.samenvatting_nog));
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
        String service = getPackageName() + "/" + FamilyStopService.class.getCanonicalName();
        int aan;
        try {
            aan = Settings.Secure.getInt(context.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            return false;
        }
        if (aan != 1) {
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
