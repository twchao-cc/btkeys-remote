package com.dsh.btkeys;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class KeyboardActivity extends BaseActivity {

    private final Handler repeat = new Handler(Looper.getMainLooper());
    private Runnable repeatTask;

    private boolean shift, ctrl, alt, win;
    private boolean extended;
    private TextView shiftKey, ctrlKey, altKey, winKey;
    private LinearLayout extendedBox;
    private TextView echo;

    private static final String[] NUM_LABEL = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"};
    private static final int[] NUM_USAGE = {
            Usages.N1, Usages.N2, Usages.N3, Usages.N4, Usages.N5,
            Usages.N6, Usages.N7, Usages.N8, Usages.N9, Usages.N0};
    private static final String[] NUM_SHIFT = {"!", "@", "#", "$", "%", "^", "&", "*", "(", ")"};

    private static final String[] Q_LABEL = {"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"};
    private static final int[] Q_USAGE = {
            Usages.Q, Usages.W, Usages.E, Usages.R, Usages.T, Usages.Y, Usages.U, Usages.I, Usages.O, Usages.P};

    private static final String[] A_LABEL = {"a", "s", "d", "f", "g", "h", "j", "k", "l", "'"};
    private static final int[] A_USAGE = {
            Usages.A, Usages.S, Usages.D, Usages.F, Usages.G, Usages.H, Usages.J, Usages.K, Usages.L, Usages.QUOTE};

    private static final String[] Z_LABEL = {"z", "x", "c", "v", "b", "n", "m", ",", ".", "/"};
    private static final int[] Z_USAGE = {
            Usages.Z, Usages.X, Usages.C, Usages.V, Usages.B, Usages.N, Usages.M,
            Usages.COMMA, Usages.DOT, Usages.SLASH};

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        build("键盘", true);

        // ---- echo / modifier status ----
        LinearLayout bar = Ui.row(this);
        bar.setBackground(Ui.bg(Ui.CARD, 10, this));
        bar.setPadding(Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 8));
        LinearLayout status = Ui.col(this);
        echo = Ui.text(this, "", 12, Ui.SUB);
        echo.setText("在此按下按键即可在电脑上输入文字");
        status.addView(echo);
        TextView hint = Ui.text(this, "提示：中文须在电脑输入法里切换", 10, Ui.FAINT);
        status.addView(hint);
        bar.addView(status, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView clear = Ui.text(this, "清空", 12, Ui.ACCENT);
        clear.setPadding(Ui.dp(this, 10), Ui.dp(this, 6), Ui.dp(this, 10), Ui.dp(this, 6));
        clear.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                echo.setText("");
                echo.setText("在此按下按键即可在电脑上输入文字");
            }
        });
        bar.addView(clear);
        body.addView(bar);

        // ---- keyboard ----
        LinearLayout kb = Ui.col(this);
        kb.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 4));

        // number row
        LinearLayout r0 = rowOfKeys(kb);
        for (int i = 0; i < NUM_LABEL.length; i++) {
            addKey(r0, NUM_LABEL[i], NUM_USAGE[i], 1f);
        }
        addKey(r0, "⌫", Usages.BACKSPACE, 1.35f, true);

        // qwerty
        LinearLayout r1 = rowOfKeys(kb);
        for (int i = 0; i < Q_LABEL.length; i++) addKey(r1, Q_LABEL[i], Q_USAGE[i], 1f);

        LinearLayout r2 = rowOfKeys(kb);
        for (int i = 0; i < A_LABEL.length; i++) addKey(r2, A_LABEL[i], A_USAGE[i], 1f);

        LinearLayout r3 = rowOfKeys(kb);
        shiftKey = addKey(r3, "⇧", -1, 1.3f, false);
        shiftKey.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                shift = !shift;
                paintMods();
            }
        });
        for (int i = 0; i < Z_LABEL.length; i++) addKey(r3, Z_LABEL[i], Z_USAGE[i], 1f);
        addKey(r3, "⏎", Usages.ENTER, 1.4f);

        // bottom row
        LinearLayout r4 = rowOfKeys(kb);
        ctrlKey = addKey(r4, "Ctrl", -1, 1.1f, false);
        ctrlKey.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { ctrl = !ctrl; paintMods(); }
        });
        altKey = addKey(r4, "Alt", -1, 1.1f, false);
        altKey.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { alt = !alt; paintMods(); }
        });
        winKey = addKey(r4, "⊞", -1, 1.1f, false);
        winKey.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { win = !win; paintMods(); }
        });
        TextView space = addKey(r4, "空格", Usages.SPACE, 4.6f);
        space.setTextSize(13);

        // row: punctuation + tab + esc
        LinearLayout r5 = rowOfKeys(kb);
        addKey(r5, "Tab", Usages.TAB, 1.3f, true);
        addKey(r5, "-", Usages.MINUS, 1f);
        addKey(r5, "=", Usages.EQUAL, 1f);
        addKey(r5, "[", Usages.LBRACKET, 1f);
        addKey(r5, "]", Usages.RBRACKET, 1f);
        addKey(r5, "\\", Usages.BACKSLASH, 1f);
        addKey(r5, ";", Usages.SEMICOLON, 1f);
        addKey(r5, "`", Usages.GRAVE, 1f);
        addKey(r5, "Esc", Usages.ESC, 1.4f, true);

        body.addView(kb);

        // ---- shortcuts ----
        body.addView(sectionTitle("常用快捷键"), topMargin(14));
        LinearLayout sc = Ui.col(this);
        sc.addView(shortcutRow(new String[]{"复制", "粘贴", "剪切", "撤销"},
                new int[][]{{Usages.C}, {Usages.V}, {Usages.X}, {Usages.Z}},
                new int[]{Usages.M_CTRL, Usages.M_CTRL, Usages.M_CTRL, Usages.M_CTRL}));
        LinearLayout sc2 = Ui.col(this);
        sc2.addView(shortcutRow(new String[]{"全选", "保存", "查杀", "关闭"},
                new int[][]{{Usages.A}, {Usages.S}, {Usages.F}, {Usages.F4}},
                new int[]{Usages.M_CTRL, Usages.M_CTRL, Usages.M_CTRL, Usages.M_ALT}));
        LinearLayout sc3 = Ui.col(this);
        sc3.addView(shortcutRow(new String[]{"切换窗口", "桌面", "锁屏", "截图"},
                new int[][]{{Usages.TAB}, {Usages.D}, {Usages.L}, {Usages.PRINT}},
                new int[]{Usages.M_ALT, Usages.M_GUI, Usages.M_GUI, Usages.M_GUI}));
        body.addView(sc);
        body.addView(sc2);
        body.addView(sc3);

        // ---- extended keys ----
        body.addView(sectionTitle("扩展按键"), topMargin(16));
        extendedBox = Ui.col(this);
        body.addView(extendedBox, topMargin(8));
        renderExtended(!extended);
    }

    // ------------------------------------------------------------------ views

    private LinearLayout.LayoutParams topMargin(int dp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(this, dp);
        return lp;
    }

    private View sectionTitle(String s) {
        TextView t = Ui.text(this, s, 13, Ui.SUB);
        return t;
    }

    private LinearLayout rowOfKeys(LinearLayout parent) {
        LinearLayout row = Ui.row(this);
        row.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 50));
        lp.topMargin = Ui.dp(this, 6);
        parent.addView(row, lp);
        return row;
    }

    private TextView addKey(LinearLayout row, String label, final int usage, float weight) {
        return addKey(row, label, usage, weight, false);
    }

    private TextView addKey(LinearLayout row, String label, final int usage, float weight, boolean small) {
        final TextView t = Ui.key(this, label, null, Ui.KEY);
        t.setTextSize(small ? 12 : 16);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight);
        lp.leftMargin = Ui.dp(this, 3);
        lp.rightMargin = Ui.dp(this, 3);
        row.addView(t, lp);

        if (usage >= 0) {
            t.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    press(usage, label);
                }
            });
            if (usage == Usages.BACKSPACE) {
                attachRepeat(t, usage, label);
            }
        }
        return t;
    }

    private void attachRepeat(final TextView t, final int usage, final String label) {
        t.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View v) {
                stopRepeat();
                repeatTask = new Runnable() {
                    @Override public void run() {
                        press(usage, label);
                        repeat.postDelayed(this, 55);
                    }
                };
                repeat.postDelayed(repeatTask, 60);
                return true;
            }
        });
        t.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_UP || e.getAction() == MotionEvent.ACTION_CANCEL) {
                    stopRepeat();
                }
                return false;
            }
        });
    }

    private void stopRepeat() {
        if (repeatTask != null) {
            repeat.removeCallbacks(repeatTask);
            repeatTask = null;
        }
        repeat.removeCallbacksAndMessages(null);
    }

    private int mods() {
        int m = 0;
        if (ctrl) m |= Usages.M_CTRL;
        if (shift) m |= Usages.M_SHIFT;
        if (alt) m |= Usages.M_ALT;
        if (win) m |= Usages.M_GUI;
        return m;
    }

    private void press(int usage, String label) {
        if (!hid.isConnected()) {
            toast("未连接设备");
            return;
        }
        haptic();
        hid.tapKey(mods(), usage);
        if (!label.equals("⌫") && !label.equals("⏎") && label.length() == 1) {
            appendEcho(label);
        } else if (label.equals("⏎")) {
            appendEcho("\n");
        } else if (label.equals("⌫")) {
            String s = echo.getText().toString();
            if (s.length() > 0) echo.setText(s.substring(0, s.length() - 1));
            return;
        }
        if (ctrl || alt || win) {
            ctrl = false;
            alt = false;
            win = false;
            paintMods();
        }
    }

    private void appendEcho(String s) {
        String cur = echo.getText().toString();
        cur = cur + s;
        if (cur.length() > 40) cur = cur.substring(cur.length() - 40);
        echo.setText(cur);
        echo.setTextColor(Ui.TEXT);
    }

    private void paintMods() {
        styleMod(shiftKey, shift);
        styleMod(ctrlKey, ctrl);
        styleMod(altKey, alt);
        styleMod(winKey, win);
    }

    private void styleMod(TextView v, boolean on) {
        if (v == null) return;
        v.setTextColor(on ? 0xFF0E1014 : Ui.TEXT);
        v.setBackground(Ui.ripple(on ? Ui.ACCENT : Ui.KEY, 12, this));
    }

    private View shortcutRow(String[] labels, final int[][] usages, final int[] mods) {
        LinearLayout row = Ui.row(this);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 48));
        rlp.topMargin = Ui.dp(this, 6);
        row.setLayoutParams(rlp);
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            TextView t = Ui.key(this, labels[i], null, Ui.KEY_HI);
            t.setTextSize(12);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            lp.leftMargin = Ui.dp(this, 3);
            lp.rightMargin = Ui.dp(this, 3);
            row.addView(t, lp);
            t.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (!hid.isConnected()) {
                        toast("未连接设备");
                        return;
                    }
                    haptic();
                    hid.tapKey(mods[idx], usages[idx]);
                }
            });
        }
        return row;
    }

    private void renderExtended(boolean collapsed) {
        extended = !collapsed;
        extendedBox.removeAllViews();

        TextView toggle = Ui.button(this, extended ? "收起扩展按键" : "展开扩展按键（F1-F12 / 方向键）", Ui.ACCENT);
        toggle.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { renderExtended(extended); }
        });
        extendedBox.addView(toggle);

        if (!extended) return;

        LinearLayout f1 = extendedRow();
        addExt(f1, "F1", Usages.F1);
        addExt(f1, "F2", Usages.F2);
        addExt(f1, "F3", Usages.F3);
        addExt(f1, "F4", Usages.F4);
        addExt(f1, "F5", Usages.F5);
        addExt(f1, "F6", Usages.F6);

        LinearLayout f2 = extendedRow();
        addExt(f2, "F7", Usages.F7);
        addExt(f2, "F8", Usages.F8);
        addExt(f2, "F9", Usages.F9);
        addExt(f2, "F10", Usages.F10);
        addExt(f2, "F11", Usages.F11);
        addExt(f2, "F12", Usages.F12);

        LinearLayout nav = extendedRow();
        addExt(nav, "Ins", Usages.INSERT);
        addExt(nav, "Home", Usages.HOME);
        addExt(nav, "PgUp", Usages.PAGEUP);
        addExt(nav, "Del", Usages.DELETE);
        addExt(nav, "End", Usages.END);
        addExt(nav, "PgDn", Usages.PAGEDOWN);

        LinearLayout arrows = extendedRow();
        addExt(arrows, "←", Usages.LEFT);
        addExt(arrows, "↑", Usages.UP);
        addExt(arrows, "↓", Usages.DOWN);
        addExt(arrows, "→", Usages.RIGHT);
        addExt(arrows, "Caps", Usages.CAPS);
        addExt(arrows, "PrtSc", Usages.PRINT);
    }

    private LinearLayout extendedRow() {
        LinearLayout row = Ui.row(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 48));
        lp.topMargin = Ui.dp(this, 6);
        extendedBox.addView(row, lp);
        return row;
    }

    private void addExt(LinearLayout row, String label, final int usage) {
        TextView t = Ui.key(this, label, null, Ui.KEY_HI);
        t.setTextSize(12);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        lp.leftMargin = Ui.dp(this, 3);
        lp.rightMargin = Ui.dp(this, 3);
        row.addView(t, lp);
        t.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (!hid.isConnected()) {
                    toast("未连接设备");
                    return;
                }
                haptic();
                hid.tapKey(mods(), usage);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopRepeat();
        shift = ctrl = alt = win = false;
        paintMods();
    }
}
