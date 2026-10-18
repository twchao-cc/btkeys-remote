package com.dsh.btkeys;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class NumpadActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        build("数字键盘", true);

        LinearLayout box = Ui.col(this);
        box.setBackground(Ui.bg(Ui.CARD, 16, this));
        box.setPadding(Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12));

        addRow(box).addView(k("Num", Usages.NUMLOCK, 1f));
        row2(box);
        body.addView(box);

        body.addView(Ui.text(this, "说明：数字键使用主键盘数字，+-*/ 使用小键盘符号，不受 NumLock 影响。",
                11, Ui.SUB), top(12));
    }

    private LinearLayout.LayoutParams top(int dp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(this, dp);
        return lp;
    }

    private LinearLayout addRow(LinearLayout parent) {
        LinearLayout row = Ui.row(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 62));
        lp.topMargin = Ui.dp(this, 8);
        parent.addView(row, lp);
        return row;
    }

    private void row2(LinearLayout parent) {
        LinearLayout row = addRow(parent);
        row.addView(k("7", Usages.N7, 1f));
        row.addView(k("8", Usages.N8, 1f));
        row.addView(k("9", Usages.N9, 1f));
        row.addView(k("÷", Usages.KP_SLASH, 1f));
        row.addView(k("⌫", Usages.BACKSPACE, 1f));

        row = addRow(parent);
        row.addView(k("4", Usages.N4, 1f));
        row.addView(k("5", Usages.N5, 1f));
        row.addView(k("6", Usages.N6, 1f));
        row.addView(k("×", Usages.KP_STAR, 1f));
        row.addView(k("Tab", Usages.TAB, 1f));

        row = addRow(parent);
        row.addView(k("1", Usages.N1, 1f));
        row.addView(k("2", Usages.N2, 1f));
        row.addView(k("3", Usages.N3, 1f));
        row.addView(k("−", Usages.KP_MINUS, 1f));
        row.addView(k("Esc", Usages.ESC, 1f));

        row = addRow(parent);
        row.addView(k("0", Usages.N0, 2f));
        row.addView(k(".", Usages.DOT, 1f));
        row.addView(k("+", Usages.KP_PLUS, 1f));
        TextView enter = k("⏎", Usages.ENTER, 1f);
        row.addView(enter);
    }

    private TextView k(String label, final int usage, float weight) {
        TextView t = Ui.key(this, label, null, Ui.KEY);
        t.setTextSize(label.length() > 1 ? 14 : 20);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight);
        lp.leftMargin = Ui.dp(this, 4);
        lp.rightMargin = Ui.dp(this, 4);
        t.setLayoutParams(lp);
        t.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (!hid.isConnected()) {
                    toast("未连接设备");
                    return;
                }
                haptic();
                hid.tapKey(0, usage);
            }
        });
        return t;
    }
}
