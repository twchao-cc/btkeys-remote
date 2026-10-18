package com.dsh.btkeys;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Shared chrome: dark status bar, header with back arrow + live connection pill. */
public abstract class BaseActivity extends Activity implements Hid.Listener {

    protected Hid hid;
    protected LinearLayout root;
    protected LinearLayout body;
    protected TextView backView;
    private TextView pill;
    private TextView titleView;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Ui.BG);
        getWindow().setNavigationBarColor(Ui.BG);
        hid = Hid.get(this);
    }

    /** Build the page skeleton. */
    protected void build(String title, boolean scrollable) {
        root = Ui.col(this);
        root.setBackgroundColor(Ui.BG);
        root.setFitsSystemWindows(true);

        LinearLayout bar = Ui.row(this);
        bar.setPadding(Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 16), Ui.dp(this, 8));

        TextView back = Ui.text(this, "‹", 30, Ui.TEXT);
        backView = back;
        back.setGravity(Gravity.CENTER);
        back.setPadding(Ui.dp(this, 8), 0, Ui.dp(this, 10), Ui.dp(this, 2));
        back.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        bar.addView(back);

        titleView = Ui.text(this, title, 19, Ui.TEXT);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        bar.addView(titleView, tlp);

        pill = Ui.text(this, "未连接", 12, Ui.SUB);
        pill.setGravity(Gravity.CENTER);
        pill.setPadding(Ui.dp(this, 10), Ui.dp(this, 5), Ui.dp(this, 10), Ui.dp(this, 5));
        pill.setBackground(Ui.bg(Ui.CARD, 20, this));
        pill.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (hid.getTarget() == null) {
                    toast("设备未连接，请返回首页选择设备");
                } else {
                    toast("已连接：" + Hid.safeName(hid.getTarget()));
                }
            }
        });
        bar.addView(pill);

        root.addView(bar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        body = Ui.col(this);
        body.setPadding(Ui.dp(this, 16), Ui.dp(this, 4), Ui.dp(this, 16), Ui.dp(this, 16));

        if (scrollable) {
            ScrollView sv = new ScrollView(this);
            sv.setFillViewport(true);
            sv.addView(body, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            root.addView(sv, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        } else {
            root.addView(body, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        }

        setContentView(root);
        refreshChrome();
    }

    protected void setTitle(String t) {
        if (titleView != null) titleView.setText(t);
    }

    @Override
    protected void onResume() {
        super.onResume();
        hid.addListener(this);
        if (Prefs.get(this).keepScreenOn()) {
            getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        hid.start();
        refreshChrome();
    }

    @Override
    protected void onPause() {
        super.onPause();
        hid.releaseKeys();
        hid.removeListener(this);
    }

    @Override
    public void onHidChanged() {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                refreshChrome();
            }
        });
    }

    protected void refreshChrome() {
        if (pill == null) return;
        if (hid.isConnected()) {
            pill.setText("● 已连接");
            pill.setTextColor(Ui.OK);
            pill.setBackground(Ui.bg(0xFF17351F, 20, this));
        } else {
            pill.setText("○ 未连接");
            pill.setTextColor(Ui.SUB);
            pill.setBackground(Ui.bg(Ui.CARD, 20, this));
        }
    }

    protected void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    protected void haptic() {
        Haptics.tap(this, Prefs.get(this).vibration());
    }

    /** Fire a configured button: keyboard chord, mouse button or media key. */
    protected void fire(Prefs.KeyDef k) {
        if (k == null) return;
        if (!hid.isConnected()) {
            toast("未连接设备，请先返回首页连接");
            return;
        }
        haptic();
        if (k.type == Prefs.TYPE_MOUSE) {
            hid.clickMouse(k.button);
        } else if (k.type == Prefs.TYPE_MEDIA) {
            int u = k.usages.length > 0 ? k.usages[0] : Usages.C_PLAY;
            hid.media(u);
        } else {
            hid.tapKey(k.mods, k.usages);
        }
    }

    protected LinearLayout cardBlock() {
        LinearLayout l = Ui.col(this);
        l.setBackground(Ui.bg(Ui.CARD, 14, this));
        l.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
        return l;
    }

    protected static TextView dot(Context c, int color) {
        TextView t = new TextView(c);
        t.setText("●");
        t.setTextColor(color);
        t.setTextSize(11);
        return t;
    }

    protected static int statusBarColor() {
        return Color.TRANSPARENT;
    }
}
