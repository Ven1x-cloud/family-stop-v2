package com.example.overlayrunnergame;

import android.accessibilityservice.AccessibilityButtonController;
import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Het zwevende paneel van Family Stop.
 *
 * Twee standen:
 *  - Staand: een ronde kaart in het midden van het scherm.
 *  - Liggend (in de browser): het hele scherm, alle knoppen weg, met alleen
 *    een klein sleepbaar knopje in een hoek om er weer uit te komen.
 */
public class MijnService extends AccessibilityService {

    // ---------------------------------------------------------------- schermen
    private static final int SCHERM_MENU = 0;
    private static final int SCHERM_BROWSER = 1;
    private static final int SCHERM_EDITOR = 2;

    private static final String KLUIS_PAKKET = "com.samsung.knox.securefolder";

    // ------------------------------------------------------------- vormgeving
    private static final float R_KAART = 28f;
    private static final float R_KNOP = 18f;
    private static final float R_TOETS = 11f;
    private static final float R_ZWEVER = 18f;

    // Afmetingen in dp
    private static final float ZWEVER_MAAT = 52f;
    private static final float ZWEVER_MARGE = 12f;
    // Extra ruimte onderaan, zodat het knopje niet onder de navigatiebalk
    // of de veegbalk van One UI verdwijnt.
    private static final float ZWEVER_ONDER = 22f;
    private static final float MINI_BREEDTE = 172f;
    private static final float MINI_HOOGTE = 196f;
    private static final float TOETS_HOOGTE = 42f;

    // ----------------------------------------------------------------- venster
    private WindowManager windowManager;
    private DisplayManager displayManager;
    private DisplayManager.DisplayListener displayListener;
    private WindowManager.LayoutParams params;

    private FrameLayout wortel;   // vult het venster
    private LinearLayout paneel;  // de ronde kaart, of het hele scherm

    // ------------------------------------------------------------- de schermen
    private TextView titel;
    private LinearLayout schermMenu;
    private FrameLayout schermBrowser;
    private LinearLayout schermEditor;

    // ---------------------------------------------------------------- browser
    private FrameLayout webHouder;
    private WebView webView;
    private View browserKop;
    private TextView adresBalk;
    private LinearLayout toetsenbordHouder;
    private LinearLayout layoutLetters;
    private LinearLayout layoutSymbolen;

    // ----------------------------------------------------------------- zwever
    private TextView zwever;
    private LinearLayout miniMenu;
    private boolean miniMenuZichtbaar = false;
    private boolean zweverLinks = false;
    private boolean zweverBoven = true;

    // ------------------------------------------------------------------ status
    private boolean zichtbaar = false;
    private boolean landschap = false;
    private boolean gesleept = false;
    private boolean typenInAdresbalk = true;
    private boolean shiftAan = false;
    private boolean toetsenbordZichtbaar = false;
    private int actiefScherm = SCHERM_MENU;
    private int schermBreedte = 0;
    private int schermHoogte = 0;
    private int vorigeBreedte = -1;
    private int vorigeHoogte = -1;

    // ==========================================================================
    //  Lifecycle van de service
    // ==========================================================================

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        meetScherm();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AccessibilityButtonController controller = getAccessibilityButtonController();
            if (controller != null) {
                controller.registerAccessibilityButtonCallback(
                        new AccessibilityButtonController.AccessibilityButtonCallback() {
                            @Override
                            public void onClicked(AccessibilityButtonController controller) {
                                if (zichtbaar) {
                                    sluitOverlay();
                                } else {
                                    toonOverlay();
                                }
                            }
                        });
            }
        }

        // Tweede bron van waarheid voor het draaien van het scherm. Sommige
        // toestellen melden de draaiing niet netjes via onConfigurationChanged.
        displayManager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        if (displayManager != null) {
            displayListener = new DisplayManager.DisplayListener() {
                @Override
                public void onDisplayAdded(int displayId) {
                }

                @Override
                public void onDisplayRemoved(int displayId) {
                }

                @Override
                public void onDisplayChanged(int displayId) {
                    herlayout();
                }
            };
            displayManager.registerDisplayListener(displayListener, null);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration nieuweConfig) {
        super.onConfigurationChanged(nieuweConfig);
        herlayout();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        stopDisplayListener();
        sluitOverlay();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        stopDisplayListener();
        sluitOverlay();
        super.onDestroy();
    }

    private void stopDisplayListener() {
        if (displayManager != null && displayListener != null) {
            try {
                displayManager.unregisterDisplayListener(displayListener);
            } catch (Exception ignored) {
                // service gaat toch weg
            }
            displayListener = null;
        }
    }

    /** Bewust leeg: we lezen geen gebeurtenissen uit andere apps mee. */
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }

    // ==========================================================================
    //  Meten en hertekenen
    // ==========================================================================

    @SuppressWarnings("deprecation")
    private void meetScherm() {
        if (windowManager != null) {
            try {
                DisplayMetrics echt = new DisplayMetrics();
                Display display = windowManager.getDefaultDisplay();
                if (display != null) {
                    display.getRealMetrics(echt);
                    schermBreedte = echt.widthPixels;
                    schermHoogte = echt.heightPixels;
                }
            } catch (Exception ignored) {
                // valt hieronder terug op de resource-maten
            }
        }
        if (schermBreedte <= 0 || schermHoogte <= 0) {
            DisplayMetrics val = getResources().getDisplayMetrics();
            schermBreedte = val.widthPixels;
            schermHoogte = val.heightPixels;
        }
    }

    private void herlayout() {
        if (windowManager == null) {
            return;
        }
        meetScherm();
        if (schermBreedte == vorigeBreedte && schermHoogte == vorigeHoogte) {
            return;
        }
        vorigeBreedte = schermBreedte;
        vorigeHoogte = schermHoogte;
        landschap = schermBreedte > schermHoogte;
        if (!zichtbaar || wortel == null) {
            return;
        }
        pasMaatAan();
        zetZweverHoek();
    }

    // ==========================================================================
    //  Paneel openen en sluiten
    // ==========================================================================

    private void toonOverlay() {
        if (zichtbaar || windowManager == null) {
            return;
        }
        meetScherm();
        vorigeBreedte = schermBreedte;
        vorigeHoogte = schermHoogte;
        landschap = schermBreedte > schermHoogte;

        if (wortel == null) {
            bouwOverlay();
        }

        zetScherm(actiefScherm);
        pasMaatAan();
        zetZweverHoek();

        params = maakParams();
        try {
            windowManager.addView(wortel, params);
        } catch (Exception e) {
            vernietigBoom();
            Toast.makeText(this, "Kon het paneel niet openen", Toast.LENGTH_SHORT).show();
            return;
        }
        zichtbaar = true;

        // zacht inzoomen bij openen
        wortel.setAlpha(0f);
        wortel.setScaleX(0.95f);
        wortel.setScaleY(0.95f);
        wortel.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(170L).start();
    }

    private void sluitOverlay() {
        zichtbaar = false;
        miniMenuZichtbaar = false;
        if (wortel == null) {
            return;
        }
        try {
            if (windowManager != null) {
                windowManager.removeView(wortel);
            }
        } catch (Exception ignored) {
            // was al losgekoppeld
        }
        vernietigBoom();
    }

    private void vernietigBoom() {
        if (webView != null) {
            try {
                webView.destroy();
            } catch (Exception ignored) {
                // al opgeruimd
            }
            webView = null;
        }
        wortel = null;
        paneel = null;
        schermBrowser = null;
        schermMenu = null;
        schermEditor = null;
        webHouder = null;
        browserKop = null;
        toetsenbordHouder = null;
        zwever = null;
        miniMenu = null;
        toetsenbordZichtbaar = false;
    }

    // ==========================================================================
    //  Venster-parameters
    // ==========================================================================

    private boolean isVolledigScherm() {
        return landschap && actiefScherm == SCHERM_BROWSER;
    }

    private WindowManager.LayoutParams maakParams() {
        boolean volledig = isVolledigScherm();
        int breedte = volledig
                ? WindowManager.LayoutParams.MATCH_PARENT
                : WindowManager.LayoutParams.WRAP_CONTENT;
        int hoogte = volledig
                ? WindowManager.LayoutParams.MATCH_PARENT
                : WindowManager.LayoutParams.WRAP_CONTENT;

        int vlaggen = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        if (volledig) {
            // echt van rand tot rand, ook onder de statusbalk door
            vlaggen |= WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        }

        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                breedte, hoogte,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                vlaggen,
                PixelFormat.TRANSLUCENT);
        p.gravity = volledig ? (Gravity.TOP | Gravity.START) : Gravity.CENTER;
        p.setTitle("Family Stop");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            p.setFitInsetsTypes(0);
        }
        return p;
    }

    // ==========================================================================
    //  Alles opbouwen
    // ==========================================================================

    private GradientDrawable kaartAchtergrond() {
        return Ui.vorm(this, Ui.kleur(this, R.color.fs_surface), R_KAART,
                Ui.kleur(this, R.color.fs_rand), 1f);
    }

    private void bouwOverlay() {
        wortel = new FrameLayout(this);
        wortel.setClipChildren(false);

        paneel = new LinearLayout(this);
        paneel.setOrientation(LinearLayout.VERTICAL);
        paneel.setBackground(kaartAchtergrond());
        wortel.addView(paneel, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER));

        titel = new TextView(this);
        titel.setText(R.string.fs_titel);
        titel.setTextColor(Ui.kleur(this, R.color.fs_tekst));
        titel.setTextSize(24f);
        titel.setTypeface(Typeface.DEFAULT_BOLD);
        titel.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        titelParams.bottomMargin = Ui.px(this, 2);
        paneel.addView(titel, titelParams);

        bouwMenu();
        bouwBrowser();
        bouwEditor();

        // Het paneel is een verticale stapel: menu en editor op hun eigen
        // hoogte, de browser vult wat er overblijft.
        paneel.addView(schermMenu, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        paneel.addView(schermBrowser, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        paneel.addView(schermEditor, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        bouwZwever();
        bouwMiniMenu();

        zetScherm(actiefScherm);
        zetToetsenbordZichtbaar(false);
    }

    private void bouwMenu() {
        schermMenu = new LinearLayout(this);
        schermMenu.setOrientation(LinearLayout.VERTICAL);

        TextView onder = new TextView(this);
        onder.setText(R.string.fs_ondertitel);
        onder.setTextColor(Ui.kleur(this, R.color.fs_tekst_dim));
        onder.setTextSize(13f);
        onder.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams onderParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        onderParams.topMargin = Ui.px(this, 4);
        onderParams.bottomMargin = Ui.px(this, 20);
        schermMenu.addView(onder, onderParams);

        schermMenu.addView(maakMenuKnop(R.string.fs_games, R.color.fs_primair,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        zetScherm(SCHERM_BROWSER);
                        if (webView != null && webView.getUrl() == null) {
                            webView.loadUrl("https://www.google.com");
                        }
                    }
                }));

        schermMenu.addView(maakMenuKnop(R.string.fs_editor, R.color.fs_waarschuwing,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        zetScherm(SCHERM_EDITOR);
                    }
                }));

        schermMenu.addView(maakMenuKnop(R.string.fs_exit, R.color.fs_gevaar,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        sluitOverlay();
                    }
                }));
    }

    private TextView maakMenuKnop(int tekstRes, int kleurRes, View.OnClickListener actie) {
        TextView knop = new TextView(this);
        knop.setText(tekstRes);
        knop.setGravity(Gravity.CENTER);
        knop.setTextColor(Color.WHITE);
        knop.setTextSize(16f);
        knop.setTypeface(Typeface.DEFAULT_BOLD);
        knop.setSingleLine(true);
        knop.setFocusable(true);
        knop.setPadding(Ui.px(this, 12), 0, Ui.px(this, 12), 0);

        GradientDrawable vorm = Ui.vorm(this, Ui.kleur(this, kleurRes), R_KNOP);
        knop.setBackground(Ui.tikbaar(this, vorm, R_KNOP));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.px(this, 54));
        lp.bottomMargin = Ui.px(this, 10);
        knop.setLayoutParams(lp);
        knop.setOnClickListener(actie);
        return knop;
    }

    private void bouwEditor() {
        schermEditor = new LinearLayout(this);
        schermEditor.setOrientation(LinearLayout.VERTICAL);

        TextView uitleg = new TextView(this);
        uitleg.setText(R.string.fs_editor_uitleg);
        uitleg.setTextColor(Ui.kleur(this, R.color.fs_tekst_dim));
        uitleg.setTextSize(13f);
        LinearLayout.LayoutParams uitlegParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        uitlegParams.bottomMargin = Ui.px(this, 18);
        schermEditor.addView(uitleg, uitlegParams);

        schermEditor.addView(maakMenuKnop(R.string.fs_open_kluis, R.color.fs_waarschuwing,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openKluis();
                    }
                }));

        schermEditor.addView(maakMenuKnop(R.string.fs_terug, R.color.fs_uit,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        zetScherm(SCHERM_MENU);
                    }
                }));
    }

    private void bouwBrowser() {
        schermBrowser = new FrameLayout(this);

        // De WebView zit in een eigen houder, zodat we hem met ronde hoeken
        // kunnen afknippen zonder aan de pagina zelf te zitten.
        webHouder = new FrameLayout(this);
        webHouder.setClipToOutline(true);
        webHouder.setBackground(Ui.vorm(this, Ui.kleur(this, R.color.fs_surface_hoog), R_KNOP));
        schermBrowser.addView(webHouder, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        final WebView wv = new WebView(this);
        webView = wv;

        WebSettings s = wv.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportMultipleWindows(true);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setMediaPlaybackRequiresUserGesture(false);

        wv.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest verzoek) {
                view.loadUrl(verzoek.getUrl().toString());
                return true;
            }

            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (adresBalk != null && !toetsenbordZichtbaar && url != null
                        && !url.startsWith("about:")) {
                    adresBalk.setText(url);
                }
            }
        });

        // Zonder dit doen links die in een nieuw tabblad openen helemaal niets.
        wv.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog,
                                          boolean isUserGesture, android.os.Message resultMsg) {
                WebView.HitTestResult resultaat = view.getHitTestResult();
                String url = resultaat == null ? null : resultaat.getExtra();
                if (url != null) {
                    view.loadUrl(url);
                    return true;
                }
                return false;
            }
        });

        // Tikken in de pagina: staat er een invoerveld open, dan tonen we het
        // toetsenbord, anders klappen we het weg.
        wv.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    wv.evaluateJavascript(
                            "(function(){var e=document.activeElement;return e?e.tagName:'';})()",
                            new ValueCallback<String>() {
                                @Override
                                public void onReceiveValue(String waarde) {
                                    boolean invoerVeld = waarde != null
                                            && (waarde.contains("INPUT") || waarde.contains("TEXTAREA"));
                                    if (invoerVeld) {
                                        typenInAdresbalk = false;
                                        zetToetsenbordZichtbaar(true);
                                    } else {
                                        zetToetsenbordZichtbaar(false);
                                    }
                                }
                            });
                }
                return false;
            }
        });

        webHouder.addView(wv, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        bouwAdresbalk();
        bouwToetsenbord(wv);
    }

    private void bouwAdresbalk() {
        LinearLayout kop = new LinearLayout(this);
        kop.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams kopParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP);
        kopParams.setMargins(Ui.px(this, 12), Ui.px(this, 12), Ui.px(this, 12), 0);
        schermBrowser.addView(kop, kopParams);
        browserKop = kop;

        adresBalk = new TextView(this);
        // Een hint en geen tekst: anders zou het adres aan de hint vastgeplakt
        // worden zodra je begint te typen.
        adresBalk.setHint(R.string.fs_adres_hint);
        adresBalk.setHintTextColor(Ui.kleur(this, R.color.fs_tekst_dim));
        adresBalk.setTextColor(Ui.kleur(this, R.color.fs_tekst));
        adresBalk.setTextSize(14f);
        adresBalk.setSingleLine(true);
        adresBalk.setEllipsize(android.text.TextUtils.TruncateAt.END);
        adresBalk.setPadding(Ui.px(this, 16), Ui.px(this, 13), Ui.px(this, 16), Ui.px(this, 13));
        adresBalk.setFocusable(false);

        GradientDrawable balkVorm = Ui.vorm(this, Ui.kleur(this, R.color.fs_surface_hoog), R_KNOP);
        balkVorm.setStroke(Ui.px(this, 1), Ui.kleur(this, R.color.fs_rand));
        adresBalk.setBackground(Ui.tikbaar(this, balkVorm, R_KNOP));

        kop.addView(adresBalk, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        adresBalk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                typenInAdresbalk = true;
                zetToetsenbordZichtbaar(true);
            }
        });
    }

    private void bouwToetsenbord(final WebView wv) {
        toetsenbordHouder = new LinearLayout(this);
        toetsenbordHouder.setOrientation(LinearLayout.VERTICAL);
        toetsenbordHouder.setVisibility(View.GONE);
        int rand = Ui.px(this, 8);
        toetsenbordHouder.setPadding(rand, rand, rand, rand);
        toetsenbordHouder.setBackground(Ui.vorm(this,
                Ui.kleur(this, R.color.fs_toetsenbord), 20f,
                Ui.kleur(this, R.color.fs_rand), 1f));

        FrameLayout.LayoutParams houderParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM);
        houderParams.setMargins(Ui.px(this, 8), 0, Ui.px(this, 8), Ui.px(this, 8));
        schermBrowser.addView(toetsenbordHouder, houderParams);

        layoutLetters = new LinearLayout(this);
        layoutLetters.setOrientation(LinearLayout.VERTICAL);

        layoutSymbolen = new LinearLayout(this);
        layoutSymbolen.setOrientation(LinearLayout.VERTICAL);
        layoutSymbolen.setVisibility(View.GONE);

        final TextView invoer = adresBalk;

        layoutLetters.addView(maakToetsenbordRij(
                new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"},
                invoer, wv, toetsenbordHouder, layoutLetters, layoutSymbolen));
        layoutLetters.addView(maakToetsenbordRij(
                new String[]{"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"},
                invoer, wv, toetsenbordHouder, layoutLetters, layoutSymbolen));
        layoutLetters.addView(maakToetsenbordRij(
                new String[]{"a", "s", "d", "f", "g", "h", "j", "k", "l"},
                invoer, wv, toetsenbordHouder, layoutLetters, layoutSymbolen));
        layoutLetters.addView(maakToetsenbordRij(
                new String[]{"SHIFT", "z", "x", "c", "v", "b", "n", "m", "DEL"},
                invoer, wv, toetsenbordHouder, layoutLetters, layoutSymbolen));
        layoutLetters.addView(maakToetsenbordRij(
                new String[]{"SYM", "/", ".", "SPATIE", "ZOEK", "KLAAR"},
                invoer, wv, toetsenbordHouder, layoutLetters, layoutSymbolen));

        layoutSymbolen.addView(maakToetsenbordRij(
                new String[]{"!", "@", "#", "$", "%", "^", "&", "*", "(", ")"},
                invoer, wv, toetsenbordHouder, layoutLetters, layoutSymbolen));
        layoutSymbolen.addView(maakToetsenbordRij(
                new String[]{"-", "_", "=", "+", "[", "]", "{", "}", "\\", "|"},
                invoer, wv, toetsenbordHouder, layoutLetters, layoutSymbolen));
        layoutSymbolen.addView(maakToetsenbordRij(
                new String[]{"<", ">", ";", ":", "'", "\"", "`", "~", "?", "/"},
                invoer, wv, toetsenbordHouder, layoutLetters, layoutSymbolen));
        layoutSymbolen.addView(maakToetsenbordRij(
                new String[]{"ABC", ",", ".", "SPATIE", "DEL", "ZOEK", "KLAAR"},
                invoer, wv, toetsenbordHouder, layoutLetters, layoutSymbolen));

        toetsenbordHouder.addView(layoutLetters);
        toetsenbordHouder.addView(layoutSymbolen);
    }

    private LinearLayout maakToetsenbordRij(String[] knoppen, final TextView invoer,
                                            final WebView wv, final LinearLayout houder,
                                            final LinearLayout letters,
                                            final LinearLayout symbolen) {
        LinearLayout rij = new LinearLayout(this);
        rij.setOrientation(LinearLayout.HORIZONTAL);
        rij.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        for (final String tekst : knoppen) {
            TextView toets = new TextView(this);
            toets.setText(tekst);
            toets.setGravity(Gravity.CENTER);
            toets.setTextColor(Color.WHITE);
            toets.setTextSize(13f);
            toets.setSingleLine(true);
            toets.setFocusable(true);

            int kleurRes;
            if ("DEL".equals(tekst)) {
                kleurRes = R.color.fs_gevaar;
            } else if ("SHIFT".equals(tekst) || "SYM".equals(tekst) || "ABC".equals(tekst)) {
                kleurRes = R.color.fs_primair;
            } else if ("ZOEK".equals(tekst)) {
                kleurRes = R.color.fs_succes;
            } else if ("KLAAR".equals(tekst)) {
                kleurRes = R.color.fs_surface_hoog2;
            } else {
                kleurRes = R.color.fs_surface_hoog;
            }

            GradientDrawable vorm = Ui.vorm(this, Ui.kleur(this, kleurRes), R_TOETS);
            toets.setBackground(Ui.tikbaar(this, vorm, R_TOETS));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0, Ui.px(this, TOETS_HOOGTE), "SPATIE".equals(tekst) ? 3f : 1f);
            lp.setMargins(Ui.px(this, 2), Ui.px(this, 3), Ui.px(this, 2), Ui.px(this, 3));
            toets.setLayoutParams(lp);

            toets.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    v.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    drukToets(tekst, invoer, wv, houder, letters, symbolen);
                }
            });
            rij.addView(toets);
        }
        return rij;
    }

    // ==========================================================================
    //  Wat doet een toets?
    // ==========================================================================

    private void drukToets(String tekst, TextView invoer, WebView wv, LinearLayout houder,
                           LinearLayout letters, LinearLayout symbolen) {
        if ("KLAAR".equals(tekst)) {
            zetToetsenbordZichtbaar(false);
            return;
        }
        if ("SHIFT".equals(tekst)) {
            shiftAan = !shiftAan;
            Toast.makeText(this, shiftAan ? "Hoofdletters aan" : "Hoofdletters uit",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if ("SYM".equals(tekst)) {
            letters.setVisibility(View.GONE);
            symbolen.setVisibility(View.VISIBLE);
            return;
        }
        if ("ABC".equals(tekst)) {
            symbolen.setVisibility(View.GONE);
            letters.setVisibility(View.VISIBLE);
            return;
        }

        if (typenInAdresbalk && invoer != null) {
            String huidig = invoer.getText().toString();
            if ("DEL".equals(tekst)) {
                if (huidig.length() > 0) {
                    invoer.setText(huidig.substring(0, huidig.length() - 1));
                }
            } else if ("ZOEK".equals(tekst)) {
                zoek(huidig.trim(), wv);
                invoer.setText("");
                zetToetsenbordZichtbaar(false);
            } else {
                invoer.setText(huidig + letterVan(tekst));
            }
            return;
        }

        // Typen midden in de website zelf, via JavaScript.
        if ("DEL".equals(tekst)) {
            wv.evaluateJavascript(
                    "if(document.activeElement && (document.activeElement.tagName==='INPUT' "
                            + "|| document.activeElement.tagName==='TEXTAREA')){"
                            + "document.activeElement.value=document.activeElement.value.slice(0,-1);"
                            + "document.activeElement.dispatchEvent(new Event('input',{bubbles:true}));}",
                    null);
        } else if ("ZOEK".equals(tekst)) {
            wv.evaluateJavascript(
                    "if(document.activeElement && document.activeElement.form){"
                            + "document.activeElement.form.submit();}",
                    null);
            zetToetsenbordZichtbaar(false);
        } else {
            String veilig = letterVan(tekst).replace("\\", "\\\\").replace("\"", "\\\"");
            wv.evaluateJavascript(
                    "if(document.activeElement && (document.activeElement.tagName==='INPUT' "
                            + "|| document.activeElement.tagName==='TEXTAREA')){"
                            + "document.activeElement.value += \"" + veilig + "\";"
                            + "document.activeElement.dispatchEvent(new Event('input',{bubbles:true}));"
                            + "document.activeElement.scrollIntoView({behavior:'smooth',block:'center'});}",
                    null);
        }
    }

    private String letterVan(String tekst) {
        String letter = "SPATIE".equals(tekst) ? " " : tekst;
        if (shiftAan && letter.length() == 1 && !" ".equals(letter)) {
            letter = letter.toUpperCase();
        }
        return letter;
    }

    private void zoek(String query, WebView wv) {
        if (query.isEmpty() || wv == null) {
            return;
        }
        boolean lijktOpAdres = query.contains(".") && !query.contains(" ");
        if (lijktOpAdres) {
            String url = query;
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }
            wv.loadUrl(url);
        } else {
            wv.loadUrl("https://www.google.com/search?q=" + Uri.encode(query));
        }
    }

    private void openKluis() {
        try {
            Intent intent = getPackageManager().getLaunchIntentForPackage(KLUIS_PAKKET);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                sluitOverlay();
            } else {
                Toast.makeText(this, R.string.fs_kluis_niet_gevonden, Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, R.string.fs_kluis_niet_gevonden, Toast.LENGTH_LONG).show();
        }
    }

    // ==========================================================================
    //  Schermen wisselen en de maat van het paneel
    // ==========================================================================

    private void zetScherm(int welk) {
        actiefScherm = welk;
        if (schermMenu == null || schermBrowser == null || schermEditor == null) {
            return;
        }
        schermMenu.setVisibility(welk == SCHERM_MENU ? View.VISIBLE : View.GONE);
        schermBrowser.setVisibility(welk == SCHERM_BROWSER ? View.VISIBLE : View.GONE);
        schermEditor.setVisibility(welk == SCHERM_EDITOR ? View.VISIBLE : View.GONE);
        if (titel != null) {
            titel.setVisibility(welk == SCHERM_MENU ? View.VISIBLE : View.GONE);
        }
        zetToetsenbordZichtbaar(false);
        verbergMiniMenu();
        pasMaatAan();
    }

    private void zetToetsenbordZichtbaar(boolean aan) {
        toetsenbordZichtbaar = aan;
        if (toetsenbordHouder != null) {
            toetsenbordHouder.setVisibility(aan ? View.VISIBLE : View.GONE);
        }
        if (aan && layoutLetters != null && layoutSymbolen != null) {
            layoutLetters.setVisibility(View.VISIBLE);
            layoutSymbolen.setVisibility(View.GONE);
        }
        if (browserKop != null) {
            browserKop.setVisibility((!isVolledigScherm() || aan) ? View.VISIBLE : View.GONE);
        }
    }

    private void pasMaatAan() {
        if (wortel == null || paneel == null) {
            return;
        }
        boolean volledig = isVolledigScherm();

        FrameLayout.LayoutParams pp = (FrameLayout.LayoutParams) paneel.getLayoutParams();
        if (pp == null) {
            pp = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER);
        }

        if (volledig) {
            // Liggend in de browser: alles weg, alleen het scherm.
            pp.width = FrameLayout.LayoutParams.MATCH_PARENT;
            pp.height = FrameLayout.LayoutParams.MATCH_PARENT;
            pp.gravity = Gravity.TOP | Gravity.START;
            pp.leftMargin = 0;
            pp.topMargin = 0;
            paneel.setPadding(0, 0, 0, 0);
            paneel.setBackgroundColor(Ui.kleur(this, R.color.fs_scrim));
        } else {
            pp.width = Math.min(schermBreedte - Ui.px(this, 40), Ui.px(this, 440));
            pp.height = (actiefScherm == SCHERM_BROWSER)
                    ? Math.min(Math.round(schermHoogte * 0.68f), Ui.px(this, 640))
                    : FrameLayout.LayoutParams.WRAP_CONTENT;
            pp.gravity = Gravity.CENTER;
            pp.leftMargin = 0;
            pp.topMargin = 0;
            int pad = Ui.px(this, 18);
            paneel.setPadding(pad, Ui.px(this, 16), pad, pad);
            paneel.setBackground(kaartAchtergrond());
        }
        paneel.setLayoutParams(pp);

        if (webHouder != null) {
            if (volledig) {
                webHouder.setClipToOutline(false);
                webHouder.setBackgroundColor(Color.BLACK);
            } else {
                webHouder.setBackground(Ui.vorm(this,
                        Ui.kleur(this, R.color.fs_surface_hoog), R_KNOP));
                webHouder.setClipToOutline(true);
            }
        }

        if (browserKop != null) {
            browserKop.setVisibility((!volledig || toetsenbordZichtbaar) ? View.VISIBLE : View.GONE);
        }

        if (zwever != null) {
            zwever.setVisibility(volledig ? View.VISIBLE : View.GONE);
        }
        if (!volledig) {
            miniMenuZichtbaar = false;
        }
        if (miniMenu != null) {
            miniMenu.setVisibility((miniMenuZichtbaar && volledig) ? View.VISIBLE : View.GONE);
        }

        WindowManager.LayoutParams np = maakParams();
        if (zichtbaar && params != null) {
            params.width = np.width;
            params.height = np.height;
            params.gravity = np.gravity;
            params.flags = np.flags;
            params.x = 0;
            params.y = 0;
            try {
                windowManager.updateViewLayout(wortel, params);
            } catch (Exception ignored) {
                // het venster is er even niet
            }
        }
    }

    // ==========================================================================
    //  Het sleepbare knopje
    // ==========================================================================

    private void bouwZwever() {
        zwever = new TextView(this);
        zwever.setText(R.string.fs_zwever_icoon);
        zwever.setGravity(Gravity.CENTER);
        zwever.setTextColor(Color.WHITE);
        zwever.setTextSize(20f);
        zwever.setTypeface(Typeface.DEFAULT_BOLD);
        zwever.setVisibility(View.GONE);

        GradientDrawable vorm = Ui.vorm(this, Ui.kleur(this, R.color.fs_primair), R_ZWEVER);
        vorm.setStroke(Ui.px(this, 1), 0x33FFFFFF);
        zwever.setBackground(Ui.tikbaar(this, vorm, R_ZWEVER));
        zwever.setElevation(Ui.dp(this, 8f));

        int maat = Ui.px(this, ZWEVER_MAAT);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                maat, maat, Gravity.TOP | Gravity.START);
        lp.leftMargin = Ui.px(this, ZWEVER_MARGE);
        lp.topMargin = Ui.px(this, ZWEVER_MARGE);
        wortel.addView(zwever, lp);

        zwever.setOnTouchListener(new View.OnTouchListener() {
            private float startX;
            private float startY;
            private float basisX;
            private float basisY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = event.getRawX();
                        startY = event.getRawY();
                        basisX = v.getTranslationX();
                        basisY = v.getTranslationY();
                        gesleept = false;
                        v.animate().scaleX(1.10f).scaleY(1.10f).setDuration(110L).start();
                        return true;

                    case MotionEvent.ACTION_MOVE: {
                        float dx = event.getRawX() - startX;
                        float dy = event.getRawY() - startY;
                        float grens = Ui.dp(MijnService.this, 8f);
                        if (!gesleept && (Math.abs(dx) > grens || Math.abs(dy) > grens)) {
                            gesleept = true;
                            verbergMiniMenu();
                        }
                        if (gesleept) {
                            v.setTranslationX(beperk(basisX + dx, maxZweverX()));
                            v.setTranslationY(beperk(basisY + dy, maxZweverY()));
                        }
                        return true;
                    }

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL: {
                        if (gesleept) {
                            float middenX = v.getTranslationX() + Ui.dp(MijnService.this, ZWEVER_MAAT) / 2f;
                            float middenY = v.getTranslationY() + Ui.dp(MijnService.this, ZWEVER_MAAT) / 2f;
                            zweverLinks = middenX < schermBreedte / 2f;
                            zweverBoven = middenY < schermHoogte / 2f;
                        }
                        final float doelX = zweverLinks ? 0f : maxZweverX();
                        final float doelY = zweverBoven ? 0f : maxZweverY();
                        v.animate()
                                .scaleX(1f).scaleY(1f)
                                .translationX(doelX).translationY(doelY)
                                .setDuration(gesleept ? 170L : 110L)
                                .withEndAction(new Runnable() {
                                    @Override
                                    public void run() {
                                        zetMiniMenuPositie();
                                    }
                                })
                                .start();
                        if (!gesleept) {
                            toggleMiniMenu();
                        }
                        return true;
                    }

                    default:
                        return false;
                }
            }
        });
    }

    private float beperk(float waarde, float max) {
        return Math.max(0f, Math.min(max, waarde));
    }

    private float maxZweverX() {
        return Math.max(0f, schermBreedte
                - Ui.dp(this, ZWEVER_MAAT) - Ui.dp(this, ZWEVER_MARGE) * 2f);
    }

    private float maxZweverY() {
        return Math.max(0f, schermHoogte
                - Ui.dp(this, ZWEVER_MAAT) - Ui.dp(this, ZWEVER_MARGE) * 2f
                - Ui.dp(this, ZWEVER_ONDER));
    }

    private void zetZweverHoek() {
        if (zwever == null) {
            return;
        }
        zwever.setTranslationX(zweverLinks ? 0f : maxZweverX());
        zwever.setTranslationY(zweverBoven ? 0f : maxZweverY());
        zetMiniMenuPositie();
    }

    // ==========================================================================
    //  Het kleine uitklapmenu naast het knopje
    // ==========================================================================

    private void bouwMiniMenu() {
        miniMenu = new LinearLayout(this);
        miniMenu.setOrientation(LinearLayout.VERTICAL);
        int pad = Ui.px(this, 6);
        miniMenu.setPadding(pad, pad, pad, pad);
        miniMenu.setBackground(Ui.vorm(this, Ui.kleur(this, R.color.fs_scrim), 20f,
                Ui.kleur(this, R.color.fs_rand), 1f));
        miniMenu.setElevation(Ui.dp(this, 12f));
        miniMenu.setVisibility(View.GONE);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                Ui.px(this, MINI_BREEDTE),
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.START);
        wortel.addView(miniMenu, lp);

        miniMenu.addView(miniRij(R.string.fs_mini_terug, R.color.fs_surface_hoog,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        verbergMiniMenu();
                        if (webView != null && webView.canGoBack()) {
                            webView.goBack();
                        }
                    }
                }));

        miniMenu.addView(miniRij(R.string.fs_mini_menu, R.color.fs_surface_hoog,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        verbergMiniMenu();
                        zetScherm(SCHERM_MENU);
                    }
                }));

        miniMenu.addView(miniRij(R.string.fs_mini_toetsenbord, R.color.fs_surface_hoog,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        verbergMiniMenu();
                        typenInAdresbalk = true;
                        zetToetsenbordZichtbaar(!toetsenbordZichtbaar);
                    }
                }));

        miniMenu.addView(miniRij(R.string.fs_mini_sluit, R.color.fs_gevaar,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        verbergMiniMenu();
                        sluitOverlay();
                    }
                }));
    }

    private TextView miniRij(int tekstRes, int kleurRes, View.OnClickListener actie) {
        TextView rij = new TextView(this);
        rij.setText(tekstRes);
        rij.setGravity(Gravity.CENTER_VERTICAL);
        rij.setTextColor(Ui.kleur(this, R.color.fs_tekst));
        rij.setTextSize(14f);
        rij.setSingleLine(true);
        rij.setFocusable(true);
        rij.setPadding(Ui.px(this, 14), 0, Ui.px(this, 14), 0);

        GradientDrawable vorm = Ui.vorm(this, Ui.kleur(this, kleurRes), 13f);
        rij.setBackground(Ui.tikbaar(this, vorm, 13f));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Ui.px(this, 40));
        lp.bottomMargin = Ui.px(this, 6);
        rij.setLayoutParams(lp);
        rij.setOnClickListener(actie);
        return rij;
    }

    private void toggleMiniMenu() {
        if (miniMenu == null) {
            return;
        }
        if (miniMenuZichtbaar) {
            verbergMiniMenu();
            return;
        }
        miniMenuZichtbaar = true;
        zetMiniMenuPositie();
        miniMenu.setVisibility(View.VISIBLE);
        miniMenu.setAlpha(0f);
        miniMenu.setScaleX(0.9f);
        miniMenu.setScaleY(0.9f);
        miniMenu.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(140L).start();
    }

    private void verbergMiniMenu() {
        miniMenuZichtbaar = false;
        if (miniMenu != null) {
            miniMenu.setVisibility(View.GONE);
        }
    }

    private void zetMiniMenuPositie() {
        if (miniMenu == null || zwever == null || miniMenu.getLayoutParams() == null) {
            return;
        }
        float marge = Ui.dp(this, ZWEVER_MARGE);
        float maat = Ui.dp(this, ZWEVER_MAAT);
        float breedte = Ui.px(this, MINI_BREEDTE);
        float hoogte = miniMenu.getMeasuredHeight() > 0
                ? miniMenu.getMeasuredHeight()
                : Ui.dp(this, MINI_HOOGTE);

        float fabX = marge + zwever.getTranslationX();
        float fabY = marge + zwever.getTranslationY();

        float x = zweverLinks
                ? fabX + maat + Ui.dp(this, 8f)
                : fabX - breedte - Ui.dp(this, 8f);
        float y = zweverBoven ? fabY : fabY + maat - hoogte;

        x = Math.max(marge, Math.min(x, Math.max(marge, schermBreedte - breedte - marge)));
        y = Math.max(marge, Math.min(y, Math.max(marge, schermHoogte - hoogte - marge)));

        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) miniMenu.getLayoutParams();
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.leftMargin = Math.round(x);
        lp.topMargin = Math.round(y);
        miniMenu.setLayoutParams(lp);
    }
}
