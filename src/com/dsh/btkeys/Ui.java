package com.dsh.btkeys;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Small programmatic-UI toolkit (no AndroidX, keeps the APK tiny). */
public final class Ui {
    public static final int BG = 0xFF0E1014;
    public static final int BG2 = 0xFF15181F;
    public static final int CARD = 0xFF1A1E27;
    public static final int KEY = 0xFF232833;
    public static final int KEY_HI = 0xFF2E3644;
    public static final int TEXT = 0xFFEDF0F5;
    public static final int SUB = 0xFF98A0B2;
    public static final int ACCENT = 0xFF4C8DFF;
    public static final int OK = 0xFF2FBF71;
    public static final int WARN = 0xFFF0A93B;
    public static final int DANGER = 0xFFF0655C;
    public static final int FAINT = 0xFF8A93A6;

    private Ui() {}

    public static int dp(Context c, float v) {
        return Math.round(c.getResources().getDisplayMetrics().density * v);
    }

    public static Drawable bg(int color, float radius, Context c) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(c, radius));
        return g;
    }

    public static Drawable bgStroke(int color, float radius, int stroke, int strokeColor, Context c) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(c, radius));
        g.setStroke(dp(c, stroke), strokeColor);
        return g;
    }

    public static Drawable ripple(int color, float radius, Context c) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(c, radius));
        return new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), g, null);
    }

    public static LinearLayout col(Context c) {
        VBox l = new VBox(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    /** Vertical container whose children default to full width (avoids narrow cards). */
    public static class VBox extends LinearLayout {
        public VBox(Context c) {
            super(c);
        }

        @Override protected LayoutParams generateDefaultLayoutParams() {
            return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        }

        @Override public LayoutParams generateLayoutParams(android.util.AttributeSet attrs) {
            return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        }

        @Override protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
            return p instanceof LayoutParams;
        }
    }

    public static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    public static TextView text(Context c, CharSequence s, float sp, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        return t;
    }

    public static TextView card(Context c) {
        TextView t = new TextView(c);
        return t;
    }

    public static LinearLayout panel(Context c, int color, float radius) {
        LinearLayout l = col(c);
        l.setBackground(bg(color, radius, c));
        return l;
    }

    public static void pad(View v, int l, int t, int r, int b) {
        v.setPadding(dp(v.getContext(), l), dp(v.getContext(), t), dp(v.getContext(), r), dp(v.getContext(), b));
    }

    public static void margin(View v, float l, float t, float r, float b) {
        Context c = v.getContext();
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        if (lp instanceof ViewGroup.MarginLayoutParams) {
            ((ViewGroup.MarginLayoutParams) lp).setMargins(dp(c, l), dp(c, t), dp(c, r), dp(c, b));
            v.setLayoutParams(lp);
        }
    }

    public static LinearLayout.LayoutParams lp(float w, float h) {
        return new LinearLayout.LayoutParams((int) w, (int) h);
    }

    public static LinearLayout.LayoutParams wlp(float weight, float h) {
        return new LinearLayout.LayoutParams(0, (int) h, weight);
    }

    /** Big rounded key button used by all the remote screens. */
    public static TextView key(Context c, String label, String sub, int bgColor) {
        TextView t = new TextView(c);
        t.setText(label);
        t.setTextSize(18);
        t.setTextColor(TEXT);
        t.setGravity(Gravity.CENTER);
        t.setBackground(ripple(bgColor, 12, c));
        t.setClickable(true);
        t.setFocusable(true);
        if (sub != null) t.setContentDescription(sub);
        return t;
    }

    public static TextView button(Context c, String label, int color) {
        TextView t = text(c, label, 14, color);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(c, 18), dp(c, 10), dp(c, 18), dp(c, 10));
        t.setBackground(ripple(KEY, 10, c));
        t.setClickable(true);
        t.setFocusable(true);
        return t;
    }

    public static TextView primaryButton(Context c, String label) {
        TextView t = button(c, label, BG);
        t.setBackground(ripple(ACCENT, 10, c));
        return t;
    }

    public static void space(LinearLayout parent, Context c, float h) {
        View v = new View(c);
        parent.addView(v, new LinearLayout.LayoutParams(1, dp(c, h)));
    }

    public static View divider(Context c) {
        View v = new View(c);
        v.setBackgroundColor(0xFF242A35);
        return v;
    }

    public static TextView header(Context c, String title, String right) {
        LinearLayout bar = row(c);
        bar.setPadding(dp(c, 18), dp(c, 14), dp(c, 18), dp(c, 10));
        TextView t = text(c, title, 20, TEXT);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        bar.addView(t, lp);
        if (right != null) {
            TextView r = button(c, right, SUB);
            bar.addView(r);
            bar.setTag(r);
        }
        return t;
    }
}
