package com.dsh.btkeys;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MediaActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        build("媒体遥控", true);

        // ---- transport ----
        LinearLayout card = Ui.col(this);
        card.setBackground(Ui.bg(Ui.CARD, 16, this));
        card.setPadding(Ui.dp(this, 14), Ui.dp(this, 16), Ui.dp(this, 14), Ui.dp(this, 16));

        LinearLayout t = Ui.row(this);
        t.addView(transport("⏮", "上一首", Usages.C_PREV));
        t.addView(transport("⏯", "播放/暂停", Usages.C_PLAY));
        t.addView(transport("⏭", "下一首", Usages.C_NEXT));
        card.addView(t, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 110)));

        LinearLayout v = Ui.row(this);
        v.addView(small("音量+", Usages.C_VOLUP, 2f));
        v.addView(small("音量−", Usages.C_VOLDOWN, 2f));
        v.addView(small("静音", Usages.C_MUTE, 1.2f));
        LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 52));
        vp.topMargin = Ui.dp(this, 14);
        card.addView(v, vp);
        body.addView(card);

        // ---- navigation ----
        body.addView(title("画面与系统"), top(18));
        LinearLayout n1 = row();
        n1.addView(small("全屏 F11", Usages.F11, 1f));
        n1.addView(small("退出 Esc", Usages.ESC, 1f));
        n1.addView(small("空格", Usages.SPACE, 1f));

        LinearLayout n2 = row();
        n2.addView(small("主页", Usages.C_HOME, 1f));
        n2.addView(small("返回", Usages.C_BACK, 1f));
        n2.addView(small("停止", Usages.C_STOP, 1f));

        LinearLayout n3 = row();
        n3.addView(small("⏪ 倒退", Usages.LEFT, 1f));
        n3.addView(small("⏩ 快进", Usages.RIGHT, 1f));
        n3.addView(small("亮度−", Usages.C_BRIGHT_DOWN, 1f));
        n3.addView(small("亮度+", Usages.C_BRIGHT_UP, 1f));

        body.addView(Ui.text(this, "适用于视频播放器、电视盒子与各类直播软件。", 11, Ui.SUB), top(14));
    }

    private LinearLayout.LayoutParams top(int dp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(this, dp);
        return lp;
    }

    private View title(String s) {
        return Ui.text(this, s, 13, Ui.SUB);
    }

    private LinearLayout row() {
        LinearLayout r = Ui.row(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 48));
        lp.topMargin = Ui.dp(this, 8);
        body.addView(r, lp);
        return r;
    }

    private LinearLayout transport(String glyph, String label, final int usage) {
        LinearLayout l = Ui.col(this);
        l.setBackground(Ui.ripple(Ui.KEY, 14, this));
        l.setGravity(Gravity.CENTER);
        l.setClickable(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        lp.leftMargin = Ui.dp(this, 5);
        lp.rightMargin = Ui.dp(this, 5);
        l.setLayoutParams(lp);

        TextView g = Ui.text(this, glyph, 30, Ui.TEXT);
        g.setGravity(Gravity.CENTER);
        l.addView(g);
        TextView t = Ui.text(this, label, 11, Ui.SUB);
        t.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tlp.topMargin = Ui.dp(this, 6);
        l.addView(t, tlp);
        l.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { send(usage); }
        });
        return l;
    }

    private TextView small(String label, final int usage, float weight) {
        TextView t = Ui.key(this, label, null, Ui.KEY);
        t.setTextSize(12);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight);
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
        if (usage > 0x65) {
            hid.media(usage);
        } else {
            hid.tapKey(0, usage);
        }
    }
}
