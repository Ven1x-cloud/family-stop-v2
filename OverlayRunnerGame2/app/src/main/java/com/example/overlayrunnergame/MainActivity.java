package com.example.overlayrunnergame;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {

    private Button btnOverlay;
    private Button btnToegankelijkheid;
    private Button btnKluis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnOverlay = findViewById(R.id.btn_overlay);
        btnToegankelijkheid = findViewById(R.id.btn_toegankelijkheid);
        btnKluis = findViewById(R.id.btn_kluis);

        btnOverlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!Settings.canDrawOverlays(MainActivity.this)) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                }
            }
        });

        btnToegankelijkheid.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isAccessibilityServiceEnabled(MainActivity.this)) {
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivity(intent);
                }
            }
        });

        btnKluis.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isVeiligeMapAanwezig()) {
                    try {
                        Intent intent = getPackageManager().getLaunchIntentForPackage("com.samsung.knox.securefolder");
                        if (intent != null) {
                            startActivity(intent);
                        } else {
                            startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS));
                        }
                    } catch (Exception e) {
                        startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS));
                    }
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkMachtigingen();
    }

    private void checkMachtigingen() {
        if (Settings.canDrawOverlays(this)) {
            btnOverlay.setBackgroundColor(Color.parseColor("#4CAF50")); 
            btnOverlay.setText("1. OVERLAY (AAN)");
        } else {
            btnOverlay.setBackgroundColor(Color.parseColor("#F44336")); 
            btnOverlay.setText("1. ACTIVEER OVERLAY");
        }

        if (isAccessibilityServiceEnabled(this)) {
            btnToegankelijkheid.setBackgroundColor(Color.parseColor("#4CAF50")); 
            btnToegankelijkheid.setText("2. SNELTOETS (AAN)");
        } else {
            btnToegankelijkheid.setBackgroundColor(Color.parseColor("#F44336")); 
            btnToegankelijkheid.setText("2. ACTIVEER SNELTOETS");
        }

        if (isVeiligeMapAanwezig()) {
            btnKluis.setBackgroundColor(Color.parseColor("#4CAF50")); 
            btnKluis.setText("3. KLUIS GEBOUWD (AAN)");
        } else {
            btnKluis.setBackgroundColor(Color.parseColor("#F44336")); 
            btnKluis.setText("3. BOUW DE KLUIS (Veilige Map)");
        }
    }

    private boolean isVeiligeMapAanwezig() {
        try {
            getPackageManager().getPackageInfo("com.samsung.knox.securefolder", 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private boolean isAccessibilityServiceEnabled(Context context) {
        String service = getPackageName() + "/" + MijnService.class.getCanonicalName();
        int accessibilityEnabled = 0;
        try {
            accessibilityEnabled = Settings.Secure.getInt(context.getContentResolver(), android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            return false;
        }
        TextUtils.SimpleStringSplitter colonSplitter = new TextUtils.SimpleStringSplitter(':');
        if (accessibilityEnabled == 1) {
            String settingValue = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                colonSplitter.setString(settingValue);
                while (colonSplitter.hasNext()) {
                    String accessibilityService = colonSplitter.next();
                    if (accessibilityService.equalsIgnoreCase(service)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}