package com.example.overlayrunnergame;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

/**
 * Kleine helpers voor ronde vormen, tik-rimpels en afmetingen.
 *
 * Alles wordt in code gemaakt in plaats van in XML, omdat het paneel in een
 * service leeft en daar geen thema van de app beschikbaar is. Zo kunnen we
 * altijd ronde hoeken tekenen zonder afhankelijk te zijn van Material-thema's.
 */
final class Ui {

    private static final int RIMPEL_KLEUR = 0x33FFFFFF;

    private Ui() {
    }

    /** Zet een dp-waarde om naar pixels. */
    static float dp(Context c, float waarde) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, waarde,
                c.getResources().getDisplayMetrics());
    }

    /** Zet een dp-waarde om naar hele pixels. */
    static int px(Context c, float waarde) {
        return Math.round(dp(c, waarde));
    }

    /** Kleur uit colors.xml. */
    static int kleur(Context c, int resId) {
        return c.getColor(resId);
    }

    /** Ronde vorm met een effen kleur. */
    static GradientDrawable vorm(Context c, int kleur, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.RECTANGLE);
        g.setColor(kleur);
        g.setCornerRadius(dp(c, radiusDp));
        return g;
    }

    /** Ronde vorm met een effen kleur en een dun randje. */
    static GradientDrawable vorm(Context c, int kleur, float radiusDp, int randKleur, float randDp) {
        GradientDrawable g = vorm(c, kleur, radiusDp);
        g.setStroke(px(c, randDp), randKleur);
        return g;
    }

    /**
     * Maakt van een ronde vorm een klikbare achtergrond met een tik-rimpel.
     * Dezelfde vorm-instantie wordt hergebruikt, dus je kunt later de kleur
     * aanpassen met {@code vorm.setColor(...)}.
     */
    static Drawable tikbaar(Context c, GradientDrawable inhoud, float radiusDp) {
        GradientDrawable masker = vorm(c, Color.WHITE, radiusDp);
        return new RippleDrawable(ColorStateList.valueOf(RIMPEL_KLEUR), inhoud, masker);
    }

    /** Zet de marges van een view in dp. */
    static void marge(View v, float links, float boven, float rechts, float onder) {
        ViewGroup.LayoutParams p = v.getLayoutParams();
        if (p instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams m = (ViewGroup.MarginLayoutParams) p;
            Context c = v.getContext();
            m.setMargins(px(c, links), px(c, boven), px(c, rechts), px(c, onder));
            v.setLayoutParams(m);
        }
    }
}
