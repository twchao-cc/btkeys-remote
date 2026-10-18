package com.dsh.btkeys;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class PptActivity extends BaseActivity {

    private TextView page;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        build("翻页笔", false);

        // ---- big prev / next ----
        LinearLayout big = Ui.row(this);
        LinearLayout prev = bigKey("◀", "上一页", "Page Up / ←", new Runnable() {
            @Override public void run() { send(Usages.PAGEUP); }
        });
        LinearLayout next = bigKey("▶", "下一页", "Page Down / →", new Runnable() {
            @Override public void run() { send(Usages.PAGEDOWN); }
        });
        big.addView(prev, weight(1, 0));
        big.addView(next, weight(1, 10));
        body.addView(big, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        page = Ui.text(this, "演示中：左右两个大按钮即可翻页", 12, Ui.SUB);
        page.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.topMargin = Ui.dp(this, 12);
        body.addView(page, tlp);

        // ---- secondary keys ----
        LinearLayout r1 = keyRow();
        r1.addView(small("F5 放映", Usages.F5));
        r1.addView(small("Esc 退出", Usages.ESC));
        r1.addView(small("B 黑屏", Usages.B));
        r1.addView(small("Enter", Usages.ENTER));

        LinearLayout r2 = keyRow();
        r2.addView(small("◀", Usages.LEFT));
        r2.addView(small("▶", Usages.RIGHT));
        r2.addView(small("▲", Usages.UP));
        r2.addView(small("▼", Usages.DOWN));

        LinearLayout r3 = keyRow();
        r3.addView(small("W 白屏", Usages.W));
        r3.addView(small("Home", Usages.HOME));
        r3.addView(small("End", Usages.END));
        r3.addView(small("Tab", Usages.TAB));
    }

    private LinearLayout.LayoutParams weight(float w, int leftDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, w);
        lp.leftMargin = Ui.dp(this, leftDp);
        return lp;
    }

    private LinearLayout bigKey(String glyph, String label, String sub, final Runnable action) {
        LinearLayout l = Ui.col(this);
        l.setBackground(Ui.ripple(Ui.KEY, 18, this));
        l.setGravity(Gravity.CENTER);
        l.setClickable(true);
        TextView g = Ui.text(this, glyph, 34, Ui.ACCENT);
        g.setGravity(Gravity.CENTER);
        l.addView(g);
        TextView t = Ui.text(this, label, 16, Ui.TEXT);
        t.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(this, 6);
        l.addView(t, lp);
        TextView s = Ui.text(this, sub, 10, Ui.SUB);
        s.setGravity(Gravity.CENTER);
        l.addView(s, lp);
        l.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { action.run(); }
        });
        return l;
    }

    private LinearLayout keyRow() {
        LinearLayout row = Ui.row(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 48));
        lp.topMargin = Ui.dp(this, 8);
        body.addView(row, lp);
        return row;
    }

    private TextView small(String label, final int usage) {
        TextView t = Ui.key(this, label, null, Ui.KEY_HI);
        t.setTextSize(12);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        lp.leftMargin = Ui.dp(this, 3);
        lp.rightMargin = Ui.dp(this, 3);
        t.setLayoutParams(lp);
        t.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { send(usage); }
        });
        return t;
    }

    private void send(int usage) {
        if (!hid.isConnected()) {
            toast("未连接设备");
            return;
        }
        haptic();
        hid.tapKey(0, usage);
        if (usage == Usages.PAGEUP && page != null) page.setText("已发送：上一页");
        if (usage == Usages.PAGEDOWN && page != null) page.setText("已发送：下一页");
    }
}
