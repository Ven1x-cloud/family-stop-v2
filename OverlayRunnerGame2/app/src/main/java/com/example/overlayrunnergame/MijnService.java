package com.example.overlayrunnergame;

import android.accessibilityservice.AccessibilityButtonController;
import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MijnService extends AccessibilityService {

    private WindowManager windowManager;
    private View overlayView;
    private boolean isOverlayZichtbaar = false;
    
    private boolean typenInUrlBalk = true; 
    private boolean isShiftAan = false; // Onthoudt of Hoofdletters aan staan!

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AccessibilityButtonController buttonController = getAccessibilityButtonController();
            buttonController.registerAccessibilityButtonCallback(new AccessibilityButtonController.AccessibilityButtonCallback() {
                @Override
                public void onClicked(AccessibilityButtonController controller) {
                    if (isOverlayZichtbaar) {
                        sluitOverlay();
                    } else {
                        toonOverlay();
                    }
                }
            });
        }
    }

    private void toonOverlay() {
        if (overlayView == null) {
            LinearLayout layoutBasis = new LinearLayout(this);
            layoutBasis.setOrientation(LinearLayout.VERTICAL);
            layoutBasis.setBackgroundColor(Color.parseColor("#EE111111"));
            layoutBasis.setPadding(40, 60, 40, 60);
            layoutBasis.setGravity(Gravity.CENTER);

            final TextView textTitel = new TextView(this);
            textTitel.setText("FAMILY STOP");
            textTitel.setTextColor(Color.WHITE);
            textTitel.setTextSize(30f);
            textTitel.setGravity(Gravity.CENTER);
            textTitel.setPadding(0, 0, 0, 40);
            layoutBasis.addView(textTitel);

            final LinearLayout menuHoofd = new LinearLayout(this);
            menuHoofd.setOrientation(LinearLayout.VERTICAL);
            layoutBasis.addView(menuHoofd);

            final LinearLayout menuEditor = new LinearLayout(this);
            menuEditor.setOrientation(LinearLayout.VERTICAL);
            menuEditor.setVisibility(View.GONE);
            layoutBasis.addView(menuEditor);

            final LinearLayout menuBrowser = new LinearLayout(this);
            menuBrowser.setOrientation(LinearLayout.VERTICAL);
            menuBrowser.setVisibility(View.GONE);
            layoutBasis.addView(menuBrowser);

            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            btnParams.setMargins(0, 0, 0, 30);

            // ==========================================
            // 1. HOOFDMENU 
            // ==========================================
            Button btnGames = new Button(this);
            btnGames.setText("GAMES & INTERNET");
            btnGames.setBackgroundColor(Color.parseColor("#4CAF50"));
            btnGames.setTextColor(Color.WHITE);
            btnGames.setTextSize(18f);
            btnGames.setPadding(0, 30, 0, 30);
            btnGames.setLayoutParams(btnParams);
            menuHoofd.addView(btnGames);

            Button btnOpenEditor = new Button(this);
            btnOpenEditor.setText("FAMILY LINK EDITOR");
            btnOpenEditor.setBackgroundColor(Color.parseColor("#FF9800"));
            btnOpenEditor.setTextColor(Color.WHITE);
            btnOpenEditor.setTextSize(18f);
            btnOpenEditor.setPadding(0, 30, 0, 30);
            btnOpenEditor.setLayoutParams(btnParams);
            menuHoofd.addView(btnOpenEditor);

            Button btnExit = new Button(this);
            btnExit.setText("EXIT OVERLAY");
            btnExit.setBackgroundColor(Color.parseColor("#F44336"));
            btnExit.setTextColor(Color.WHITE);
            btnExit.setTextSize(18f);
            btnExit.setPadding(0, 30, 0, 30);
            btnExit.setLayoutParams(btnParams);
            btnExit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sluitOverlay();
                }
            });
            menuHoofd.addView(btnExit);

            // ==========================================
            // 2. EDITOR MENU
            // ==========================================
            Button btnEscape = new Button(this);
            btnEscape.setText("OPEN DE KLUIS (Veilige Map)");
            btnEscape.setBackgroundColor(Color.parseColor("#9C27B0"));
            btnEscape.setTextColor(Color.WHITE);
            btnEscape.setTextSize(18f);
            btnEscape.setPadding(0, 30, 0, 30);
            btnEscape.setLayoutParams(btnParams);
            btnEscape.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        Intent intent = getPackageManager().getLaunchIntentForPackage("com.samsung.knox.securefolder");
                        if (intent != null) {
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            sluitOverlay(); 
                        } else {
                            textTitel.setText("FOUT: Veilige Map Onzichtbaar!");
                            textTitel.setTextColor(Color.RED);
                            textTitel.setTextSize(18f);
                        }
                    } catch (Exception e) {
                        textTitel.setText("GEBLOKKEERD!");
                        textTitel.setTextColor(Color.RED);
                        textTitel.setTextSize(18f);
                    }
                }
            });
            menuEditor.addView(btnEscape);

            Button btnTerugEditor = new Button(this);
            btnTerugEditor.setText("TERUG NAAR HOOFDMENU");
            btnTerugEditor.setBackgroundColor(Color.parseColor("#607D8B"));
            btnTerugEditor.setTextColor(Color.WHITE);
            btnTerugEditor.setTextSize(18f);
            btnTerugEditor.setPadding(0, 30, 0, 30);
            btnTerugEditor.setLayoutParams(btnParams);
            btnTerugEditor.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    textTitel.setText("FAMILY STOP");
                    textTitel.setTextColor(Color.WHITE);
                    textTitel.setTextSize(30f);
                    menuEditor.setVisibility(View.GONE);
                    menuHoofd.setVisibility(View.VISIBLE);
                }
            });
            menuEditor.addView(btnTerugEditor);

            btnOpenEditor.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    textTitel.setText("EDITOR MODUS");
                    menuHoofd.setVisibility(View.GONE);
                    menuEditor.setVisibility(View.VISIBLE);
                }
            });

            // ==========================================
            // 3. BROWSER MENU & GEAVANCEERD TOETSENBORD
            // ==========================================
            
            final TextView inputUrl = new TextView(this);
            inputUrl.setText("");
            inputUrl.setHint("Tik hier om te zoeken of typ URL...");
            inputUrl.setHintTextColor(Color.GRAY);
            inputUrl.setTextColor(Color.BLACK);
            inputUrl.setBackgroundColor(Color.WHITE);
            inputUrl.setPadding(30, 30, 30, 30);
            inputUrl.setTextSize(16f);
            LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            inputParams.setMargins(0, 0, 0, 20);
            inputUrl.setLayoutParams(inputParams);
            menuBrowser.addView(inputUrl);

            // Container voor het gehele toetsenbord
            final LinearLayout toetsenbordContainer = new LinearLayout(this);
            toetsenbordContainer.setOrientation(LinearLayout.VERTICAL);
            toetsenbordContainer.setVisibility(View.GONE); 

            // De TWEE verschillende toetsenborden (Letters en Symbolen)
            final LinearLayout layoutLetters = new LinearLayout(this);
            layoutLetters.setOrientation(LinearLayout.VERTICAL);
            
            final LinearLayout layoutSymbolen = new LinearLayout(this);
            layoutSymbolen.setOrientation(LinearLayout.VERTICAL);
            layoutSymbolen.setVisibility(View.GONE); // Symbolen zijn standaard verborgen

            // De WebView (Browser)
            final WebView webView = new WebView(this);
            WebSettings webSettings = webView.getSettings();
            webSettings.setJavaScriptEnabled(true);
            webSettings.setDomStorageEnabled(true);
            webSettings.setDatabaseEnabled(true); 
            webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE); 
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public boolean shouldOverrideUrlLoading(WebView view, String url) {
                    view.loadUrl(url);
                    return true;
                }
            });

            webView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_UP) {
                        webView.evaluateJavascript("document.activeElement.tagName", new ValueCallback<String>() {
                            @Override
                            public void onReceiveValue(String value) {
                                if (value != null && (value.contains("INPUT") || value.contains("TEXTAREA"))) {
                                    typenInUrlBalk = false; 
                                    toetsenbordContainer.setVisibility(View.VISIBLE);
                                } else {
                                    toetsenbordContainer.setVisibility(View.GONE);
                                }
                            }
                        });
                    }
                    return false; 
                }
            });

            // Bouw de LETTERS rijen (met SHIFT en SYM)
            layoutLetters.addView(maakToetsenbordRij(new String[]{"1","2","3","4","5","6","7","8","9","0"}, inputUrl, webView, toetsenbordContainer, layoutLetters, layoutSymbolen));
            layoutLetters.addView(maakToetsenbordRij(new String[]{"q","w","e","r","t","y","u","i","o","p"}, inputUrl, webView, toetsenbordContainer, layoutLetters, layoutSymbolen));
            layoutLetters.addView(maakToetsenbordRij(new String[]{"a","s","d","f","g","h","j","k","l"}, inputUrl, webView, toetsenbordContainer, layoutLetters, layoutSymbolen));
            layoutLetters.addView(maakToetsenbordRij(new String[]{"SHIFT","z","x","c","v","b","n","m","DEL"}, inputUrl, webView, toetsenbordContainer, layoutLetters, layoutSymbolen));
            layoutLetters.addView(maakToetsenbordRij(new String[]{"SYM", "/", ".", "SPATIE", "ZOEK", "KLAAR ⬇"}, inputUrl, webView, toetsenbordContainer, layoutLetters, layoutSymbolen));
            
            // Bouw de SYMBOLEN rijen (met leestekens en ABC knop)
            layoutSymbolen.addView(maakToetsenbordRij(new String[]{"!","@","#","$","%","^","&","*","(",")"}, inputUrl, webView, toetsenbordContainer, layoutLetters, layoutSymbolen));
            layoutSymbolen.addView(maakToetsenbordRij(new String[]{"-","_","=","+","[","]","{","}","\\","|"}, inputUrl, webView, toetsenbordContainer, layoutLetters, layoutSymbolen));
            layoutSymbolen.addView(maakToetsenbordRij(new String[]{"<",">",";",":","'","\"","`","~","?","/"}, inputUrl, webView, toetsenbordContainer, layoutLetters, layoutSymbolen));
            layoutSymbolen.addView(maakToetsenbordRij(new String[]{"ABC", ",", ".", "SPATIE", "DEL", "ZOEK", "KLAAR ⬇"}, inputUrl, webView, toetsenbordContainer, layoutLetters, layoutSymbolen));

            // Voeg beide borden toe aan de container
            toetsenbordContainer.addView(layoutLetters);
            toetsenbordContainer.addView(layoutSymbolen);
            
            menuBrowser.addView(toetsenbordContainer);

            inputUrl.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    typenInUrlBalk = true;
                    toetsenbordContainer.setVisibility(View.VISIBLE);
                }
            });

            LinearLayout.LayoutParams webParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1200); 
            webParams.setMargins(0, 0, 0, 30);
            webView.setLayoutParams(webParams);
            menuBrowser.addView(webView);

            Button btnSluitBrowser = new Button(this);
            btnSluitBrowser.setText("SLUIT BROWSER");
            btnSluitBrowser.setBackgroundColor(Color.RED);
            btnSluitBrowser.setTextColor(Color.WHITE);
            btnSluitBrowser.setTextSize(18f);
            btnSluitBrowser.setPadding(0, 30, 0, 30);
            btnSluitBrowser.setLayoutParams(btnParams);
            btnSluitBrowser.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    webView.loadUrl("about:blank");
                    inputUrl.setText(""); 
                    textTitel.setText("FAMILY STOP");
                    toetsenbordContainer.setVisibility(View.GONE);
                    menuBrowser.setVisibility(View.GONE);
                    menuHoofd.setVisibility(View.VISIBLE);
                }
            });
            menuBrowser.addView(btnSluitBrowser);

            btnGames.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    textTitel.setText("INTERNET MODE");
                    menuHoofd.setVisibility(View.GONE);
                    menuBrowser.setVisibility(View.VISIBLE);
                    webView.loadUrl("https://www.google.com"); 
                }
            });

            overlayView = layoutBasis;
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, 
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, 
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, 
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.CENTER;

        windowManager.addView(overlayView, params);
        isOverlayZichtbaar = true;
    }

    private void sluitOverlay() {
        if (overlayView != null && isOverlayZichtbaar) {
            windowManager.removeView(overlayView);
            isOverlayZichtbaar = false;
        }
    }

    // ==========================================
    // DE CUSTOM KNOPPEN-MACHINE (Met SHIFT en SYM logica!)
    // ==========================================
    private LinearLayout maakToetsenbordRij(String[] knoppen, final TextView inputVeld, final WebView webView, final LinearLayout toetsenbordContainer, final LinearLayout layoutLetters, final LinearLayout layoutSymbolen) {
        LinearLayout rij = new LinearLayout(this);
        rij.setOrientation(LinearLayout.HORIZONTAL);
        rij.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        for (final String tekst : knoppen) {
            final Button btn = new Button(this);
            btn.setText(tekst);
            btn.setTextColor(Color.WHITE);
            
            // Speciale kleuren voor commandoknoppen
            if(tekst.equals("ZOEK") || tekst.equals("KLAAR ⬇")) btn.setBackgroundColor(Color.parseColor("#2196F3")); 
            else if(tekst.equals("DEL")) btn.setBackgroundColor(Color.parseColor("#F44336")); 
            else if(tekst.equals("SHIFT") || tekst.equals("SYM") || tekst.equals("ABC")) btn.setBackgroundColor(Color.parseColor("#9C27B0")); // Paars voor wissel-knoppen
            else btn.setBackgroundColor(Color.parseColor("#333333")); 

            btn.setPadding(0, 20, 0, 20);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
            params.setMargins(2, 2, 2, 2);
            btn.setLayoutParams(params);

            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    
                    // KLAAR KNOP
                    if (tekst.equals("KLAAR ⬇")) {
                        toetsenbordContainer.setVisibility(View.GONE);
                        return; 
                    }

                    // SHIFT KNOP (Hoofdletters aan/uit)
                    if (tekst.equals("SHIFT")) {
                        isShiftAan = !isShiftAan; // Draai om
                        Toast.makeText(MijnService.this, isShiftAan ? "Hoofdletters AAN" : "Hoofdletters UIT", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // SYM KNOP (Naar symbolen scherm)
                    if (tekst.equals("SYM")) {
                        layoutLetters.setVisibility(View.GONE);
                        layoutSymbolen.setVisibility(View.VISIBLE);
                        return;
                    }

                    // ABC KNOP (Terug naar letters scherm)
                    if (tekst.equals("ABC")) {
                        layoutSymbolen.setVisibility(View.GONE);
                        layoutLetters.setVisibility(View.VISIBLE);
                        return;
                    }

                    // HET TYPEN ZELF
                    if (typenInUrlBalk) {
                        String huidigeTekst = inputVeld.getText().toString();
                        
                        if (tekst.equals("DEL")) {
                            if (huidigeTekst.length() > 0) {
                                inputVeld.setText(huidigeTekst.substring(0, huidigeTekst.length() - 1));
                            }
                        } else if (tekst.equals("ZOEK")) {
                            String query = huidigeTekst.trim();
                            if (!query.isEmpty()) {
                                if (query.contains(" ") || (!query.contains(".") && !query.contains(":"))) {
                                    webView.loadUrl("https://www.google.com/search?q=" + query);
                                } else {
                                    if (!query.startsWith("http://") && !query.startsWith("https://")) {
                                        query = "https://" + query;
                                    }
                                    webView.loadUrl(query);
                                }
                            }
                            inputVeld.setText("");
                            toetsenbordContainer.setVisibility(View.GONE); 
                        } else {
                            String letter = tekst.equals("SPATIE") ? " " : tekst;
                            // HOOFDLETTERS CHECK!
                            if (isShiftAan && letter.length() == 1) {
                                letter = letter.toUpperCase();
                            }
                            inputVeld.setText(huidigeTekst + letter);
                        }

                    } else {
                        // LIVE TYPEN IN DE WEBSITE
                        if (tekst.equals("DEL")) {
                            String jsDel = "if(document.activeElement && (document.activeElement.tagName === 'INPUT' || document.activeElement.tagName === 'TEXTAREA')) { document.activeElement.value = document.activeElement.value.slice(0, -1); document.activeElement.dispatchEvent(new Event('input', {bubbles:true})); }";
                            webView.evaluateJavascript(jsDel, null);
                        } else if (tekst.equals("ZOEK")) {
                            String jsEnter = "if(document.activeElement && document.activeElement.form) { document.activeElement.form.submit(); }";
                            webView.evaluateJavascript(jsEnter, null);
                            toetsenbordContainer.setVisibility(View.GONE);
                        } else {
                            String letter = tekst.equals("SPATIE") ? " " : tekst;
                            // HOOFDLETTERS CHECK!
                            if (isShiftAan && letter.length() == 1) {
                                letter = letter.toUpperCase();
                            }
                            
                            // Beveiliging: zorg dat rare symbolen de Javascript code niet kapot maken
                            String safeLetter = letter.replace("\\", "\\\\").replace("\"", "\\\"");

                            String jsTyp = "if(document.activeElement && (document.activeElement.tagName === 'INPUT' || document.activeElement.tagName === 'TEXTAREA')) { " +
                                    "document.activeElement.value += \"" + safeLetter + "\"; " +
                                    "document.activeElement.dispatchEvent(new Event('input', {bubbles:true})); " +
                                    "document.activeElement.scrollIntoView({behavior: 'smooth', block: 'center'}); " +
                                    "}";
                            webView.evaluateJavascript(jsTyp, null);
                        }
                    }
                }
            });
            rij.addView(btn);
        }
        return rij;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}
}