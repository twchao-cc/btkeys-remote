package com.dsh.btkeys;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

/** Draggable button board with switchable profiles — the "自定义按键" screen. */
public class CustomKeysActivity extends BaseActivity {

    private Prefs prefs;
    private Prefs.Profile profile;
    private FrameLayout canvas;
    private boolean edit;
    private TextView modeBtn;
    private TextView profBtn;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = Prefs.get(this);
        profile = prefs.currentProfile();
        build("自定义按键", false);

        // ---- toolbar ----
        LinearLayout bar = Ui.row(this);
        profBtn = Ui.button(this, "档案：" + profile.name, Ui.TEXT);
        profBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showProfileDialog(); }
        });
        bar.addView(profBtn, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        modeBtn = Ui.button(this, "编辑", Ui.ACCENT);
        modeBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                edit = !edit;
                modeBtn.setText(edit ? "完成" : "编辑");
                modeBtn.setTextColor(edit ? Ui.DANGER : Ui.ACCENT);
                render();
            }
        });
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.leftMargin = Ui.dp(this, 8);
        bar.addView(modeBtn, blp);
        body.addView(bar);

        TextView hint = Ui.text(this, edit ? "编辑模式：拖动可移动按键，点击按键可修改或删除"
                : "点击按键即可发送到电脑", 11, Ui.SUB);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hlp.topMargin = Ui.dp(this, 8);
        body.addView(hint, hlp);
        hint.setTag("hint");

        canvas = new FrameLayout(this);
        canvas.setBackground(Ui.bgStroke(0xFF12151C, 16, 1, 0xFF232A36, this));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        clp.topMargin = Ui.dp(this, 10);
        body.addView(canvas, clp);

        // ---- add button ----
        TextView add = Ui.primaryButton(this, "＋ 添加按键");
        add.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showActionPicker(); }
        });
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        alp.topMargin = Ui.dp(this, 10);
        body.addView(add, alp);

        canvas.post(new Runnable() {
            @Override public void run() { render(); }
        });
        render();
    }

    // ------------------------------------------------------------------ rendering

    private void render() {
        if (canvas == null) return;
        canvas.removeAllViews();
        TextView hint = (TextView) body.findViewWithTag("hint");
        if (hint != null) {
            hint.setText(edit ? "编辑模式：拖动可移动按键，点击按键可修改或删除"
                    : "点击按键即可发送到电脑");
        }
        // drop out-of-bounds keys back inside
        int cw = canvas.getWidth(), ch = canvas.getHeight();
        for (Prefs.KeyDef k : profile.keys) {
            if (cw > 0) {
                k.x = (int) Math.max(0f, Math.min((float) k.x, cw / getDensity() - k.w));
                k.y = (int) Math.max(0f, Math.min((float) k.y, ch / getDensity() - k.h));
            }
            canvas.addView(makeKey(k));
        }
        if (profile.keys.isEmpty()) {
            TextView empty = Ui.text(this, "还没有按键\n点击下方「添加按键」开始设置", 13, Ui.SUB);
            empty.setGravity(Gravity.CENTER);
            empty.setLayoutParams(new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            canvas.addView(empty);
        }
        save();
    }

    private float getDensity() {
        return getResources().getDisplayMetrics().density;
    }

    private View makeKey(final Prefs.KeyDef k) {
        TextView t = new TextView(this);
        t.setText(k.label);
        t.setTextColor(Ui.TEXT);
        t.setTextSize(13);
        t.setGravity(Gravity.CENTER);
        t.setBackground(Ui.ripple(edit ? 0xFF2B3342 : Ui.KEY, 12, this));
        t.setSingleLine(true);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                Ui.dp(this, k.w), Ui.dp(this, k.h));
        lp.leftMargin = Ui.dp(this, k.x);
        lp.topMargin = Ui.dp(this, k.y);
        t.setLayoutParams(lp);

        if (edit) {
            t.setOnTouchListener(new DragListener(k, t));
        } else {
            t.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { fire(k); }
            });
        }
        return t;
    }

    private void save() {
        List<Prefs.Profile> all = prefs.profiles();
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).name.equals(profile.name)) {
                all.set(i, profile);
                break;
            }
        }
        prefs.saveProfiles(all);
        prefs.setCurrentProfile(profile.name);
    }

    // ------------------------------------------------------------------ drag

    private class DragListener implements View.OnTouchListener {
        private final Prefs.KeyDef k;
        private float startX, startY;
        private int baseX, baseY;
        private boolean moved;
        private long downAt;

        DragListener(Prefs.KeyDef k, View v) {
            this.k = k;
        }

        @Override public boolean onTouch(View v, MotionEvent e) {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) v.getLayoutParams();
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    startX = e.getRawX();
                    startY = e.getRawY();
                    baseX = lp.leftMargin;
                    baseY = lp.topMargin;
                    moved = false;
                    downAt = android.os.SystemClock.uptimeMillis();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float dx = e.getRawX() - startX;
                    float dy = e.getRawY() - startY;
                    if (!moved && (Math.abs(dx) > Ui.dp(CustomKeysActivity.this, 4)
                            || Math.abs(dy) > Ui.dp(CustomKeysActivity.this, 4))) {
                        moved = true;
                    }
                    if (!moved) return true;
                    int nx = baseX + (int) dx;
                    int ny = baseY + (int) dy;
                    int maxX = canvas.getWidth() - v.getWidth();
                    int maxY = canvas.getHeight() - v.getHeight();
                    nx = Math.max(0, Math.min(nx, Math.max(0, maxX)));
                    ny = Math.max(0, Math.min(ny, Math.max(0, maxY)));
                    lp.leftMargin = nx;
                    lp.topMargin = ny;
                    v.setLayoutParams(lp);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (moved) {
                        k.x = Math.round(lp.leftMargin / getDensity());
                        k.y = Math.round(lp.topMargin / getDensity());
                        save();
                        haptic();
                    } else if (android.os.SystemClock.uptimeMillis() - downAt < 700) {
                        keyMenu(k);
                    }
                    return true;
            }
            return false;
        }
    }

    // ------------------------------------------------------------------ dialogs

    private void keyMenu(final Prefs.KeyDef k) {
        final String[] items = {"修改名称", "修改动作", "调整大小", "复制按键", "删除按键"};
        new AlertDialog.Builder(this)
                .setTitle(k.label)
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int which) {
                        if (which == 0) rename(k);
                        else if (which == 1) showActionPickerFor(k);
                        else if (which == 2) resize(k);
                        else if (which == 3) {
                            Prefs.KeyDef c = k.copy();
                            c.x = k.x + 12;
                            c.y = k.y + 12;
                            profile.keys.add(c);
                            render();
                        } else if (which == 4) {
                            profile.keys.remove(k);
                            render();
                        }
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void rename(final Prefs.KeyDef k) {
        final EditText et = new EditText(this);
        et.setInputType(InputType.TYPE_CLASS_TEXT);
        et.setText(k.label);
        et.setSelectAllOnFocus(true);
        new AlertDialog.Builder(this)
                .setTitle("按键名称")
                .setView(et)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        String s = et.getText().toString().trim();
                        if (s.length() > 0) {
                            k.label = s.length() > 8 ? s.substring(0, 8) : s;
                            render();
                        }
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void resize(final Prefs.KeyDef k) {
        final String[] items = {"小", "中", "大", "超宽"};
        new AlertDialog.Builder(this)
                .setTitle("按键大小")
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        if (w == 0) { k.w = 60; k.h = 48; }
                        else if (w == 1) { k.w = 84; k.h = 62; }
                        else if (w == 2) { k.w = 110; k.h = 76; }
                        else { k.w = 160; k.h = 62; }
                        render();
                    }
                })
                .show();
    }

    private void showProfileDialog() {
        final List<Prefs.Profile> all = prefs.profiles();
        String[] items = new String[all.size() + 3];
        for (int i = 0; i < all.size(); i++) {
            items[i] = (all.get(i).name.equals(profile.name) ? "✓ " : "") + all.get(i).name;
        }
        items[all.size()] = "新建档案…";
        items[all.size() + 1] = "重命名当前档案…";
        items[all.size() + 2] = "删除当前档案";
        new AlertDialog.Builder(this)
                .setTitle("按钮档案")
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int which) {
                        if (which < all.size()) {
                            profile = all.get(which);
                            prefs.setCurrentProfile(profile.name);
                            profBtn.setText("档案：" + profile.name);
                            render();
                        } else if (which == all.size()) {
                            newProfile();
                        } else if (which == all.size() + 1) {
                            renameProfile();
                        } else {
                            deleteProfile();
                        }
                    }
                })
                .show();
    }

    private void newProfile() {
        final EditText et = new EditText(this);
        et.setHint("例如：办公、游戏、直播");
        new AlertDialog.Builder(this)
                .setTitle("新建档案")
                .setView(et)
                .setPositiveButton("创建", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        String s = et.getText().toString().trim();
                        if (s.length() == 0) return;
                        List<Prefs.Profile> all = prefs.profiles();
                        for (Prefs.Profile p : all) {
                            if (p.name.equals(s)) {
                                toast("已存在同名档案");
                                return;
                            }
                        }
                        Prefs.Profile p = new Prefs.Profile();
                        p.name = s;
                        all.add(p);
                        prefs.saveProfiles(all);
                        profile = p;
                        prefs.setCurrentProfile(s);
                        profBtn.setText("档案：" + s);
                        render();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void renameProfile() {
        final EditText et = new EditText(this);
        et.setText(profile.name);
        new AlertDialog.Builder(this)
                .setTitle("重命名档案")
                .setView(et)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        String s = et.getText().toString().trim();
                        if (s.length() == 0) return;
                        List<Prefs.Profile> all = prefs.profiles();
                        for (int i = 0; i < all.size(); i++) {
                            if (all.get(i).name.equals(profile.name)) {
                                all.get(i).name = s;
                                break;
                            }
                        }
                        profile.name = s;
                        prefs.saveProfiles(all);
                        prefs.setCurrentProfile(s);
                        profBtn.setText("档案：" + s);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void deleteProfile() {
        List<Prefs.Profile> all = prefs.profiles();
        if (all.size() <= 1) {
            toast("至少保留一个档案");
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("删除档案")
                .setMessage("确定删除「" + profile.name + "」及其全部按键？删除后无法恢复。")
                .setPositiveButton("删除", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        List<Prefs.Profile> list = prefs.profiles();
                        for (int i = 0; i < list.size(); i++) {
                            if (list.get(i).name.equals(profile.name)) {
                                list.remove(i);
                                break;
                            }
                        }
                        prefs.saveProfiles(list);
                        profile = list.get(0);
                        prefs.setCurrentProfile(profile.name);
                        profBtn.setText("档案：" + profile.name);
                        render();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // ------------------------------------------------------------------ action picker

    private void showActionPicker() {
        showActionPickerFor(null);
    }

    /** target == null means "create a new key", otherwise replace that key's action. */
    private void showActionPickerFor(final Prefs.KeyDef target) {
        ScrollView sv = new ScrollView(this);
        LinearLayout root = Ui.col(this);
        root.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16), Ui.dp(this, 8));
        sv.addView(root);

        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(target == null ? "添加按键" : "修改动作")
                .setView(sv)
                .setNegativeButton("取消", null)
                .create();

        Pick pick = new Pick() {
            @Override public void onPick(Prefs.KeyDef def) {                if (target == null) {
                    def.x = 20 + (profile.keys.size() % 3) * 24;
                    def.y = 20 + (profile.keys.size() % 5) * 70;
                    profile.keys.add(def);
                } else {
                    target.label = def.label;
                    target.type = def.type;
                    target.mods = def.mods;
                    target.usages = def.usages;
                    target.button = def.button;
                }
                render();
                dialog.dismiss();
            }
        };

        group(root, "组合键");
        pickCallback = pick;
        chips(root, 4, new String[]{"Ctrl+C 复制", "Ctrl+V 粘贴", "Ctrl+X 剪切", "Ctrl+Z 撤销",
                "Ctrl+A 全选", "Ctrl+S 保存", "Alt+Tab 切换", "Alt+F4 关闭",
                "Win+D 桌面", "Win+L 锁屏", "Win+E 资源", "Ctrl+Shift+Esc"});
        group(root, "功能键");
        chips(root, 5, new String[]{"Esc", "Tab", "Enter", "Backspace", "Space",
                "Delete", "Home", "End", "PgUp", "PgDn",
                "↑", "↓", "←", "→", "PrintScreen"});
        group(root, "F1 - F12");
        chips(root, 6, new String[]{"F1", "F2", "F3", "F4", "F5", "F6",
                "F7", "F8", "F9", "F10", "F11", "F12"});
        group(root, "字母");
        chips(root, 7, new String[]{"A", "B", "C", "D", "E", "F", "G",
                "H", "I", "J", "K", "L", "M", "N",
                "O", "P", "Q", "R", "S", "T", "U",
                "V", "W", "X", "Y", "Z"});
        group(root, "数字与符号");
        chips(root, 7, new String[]{"0", "1", "2", "3", "4", "5", "6",
                "7", "8", "9", "-", "=", ",", ".",
                "/", ";", "'", "[", "]", "\\", "`"});
        group(root, "鼠标");
        chips(root, 4, new String[]{"左键", "右键", "中键"});
        group(root, "媒体");
        chips(root, 4, new String[]{"播放/暂停", "上一首", "下一首", "停止",
                "音量+", "音量-", "静音", "主页",
                "返回", "亮度+", "亮度-"});

        dialog.show();
    }

    private AlertDialog pickDialog;

    private interface Pick {
        void onPick(Prefs.KeyDef def);
    }

    private void group(LinearLayout root, String title) {
        TextView t = Ui.text(this, title, 12, Ui.SUB);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(this, 14);
        root.addView(t, lp);
    }

    private void chips(LinearLayout root, int columns, String[] labels) {
        LinearLayout row = null;
        for (int i = 0; i < labels.length; i++) {
            if (i % columns == 0) {
                row = Ui.row(this);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.topMargin = Ui.dp(this, 6);
                root.addView(row, lp);
            }
            final Prefs.KeyDef def = buildDef(labels[i]);
            TextView t = Ui.key(this, labels[i], null, Ui.KEY);
            t.setTextSize(11);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.dp(this, 48), 1f);
            lp.leftMargin = Ui.dp(this, 3);
            lp.rightMargin = Ui.dp(this, 3);
            row.addView(t, lp);
            t.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    if (pickCallback != null) pickCallback.onPick(def);
                }
            });
        }
    }

    private Pick pickCallback;

    private Prefs.KeyDef buildDef(String label) {
        // keyboard combos
        if (label.startsWith("Ctrl+Shift+Esc")) {
            return Prefs.KeyDef.key("任务", Usages.M_CTRL | Usages.M_SHIFT, Usages.ESC);
        }
        if (label.startsWith("Ctrl+")) {
            String key = label.substring(5, 6);
            int u = letterUsage(key);
            return Prefs.KeyDef.key(label.substring(0, 6), Usages.M_CTRL, u);
        }
        if (label.startsWith("Alt+Tab")) return Prefs.KeyDef.key("切换", Usages.M_ALT, Usages.TAB);
        if (label.startsWith("Alt+F4")) return Prefs.KeyDef.key("关闭", Usages.M_ALT, Usages.F4);
        if (label.startsWith("Win+D")) return Prefs.KeyDef.key("桌面", Usages.M_GUI, Usages.D);
        if (label.startsWith("Win+L")) return Prefs.KeyDef.key("锁屏", Usages.M_GUI, Usages.L);
        if (label.startsWith("Win+E")) return Prefs.KeyDef.key("资源", Usages.M_GUI, Usages.E);

        String n = label.split(" ")[0];

        if (n.equals("左键")) return Prefs.KeyDef.mouse("左键", Usages.B_LEFT);
        if (n.equals("右键")) return Prefs.KeyDef.mouse("右键", Usages.B_RIGHT);
        if (n.equals("中键")) return Prefs.KeyDef.mouse("中键", Usages.B_MIDDLE);
        if (n.equals("播放/暂停")) return Prefs.KeyDef.media("播放", Usages.C_PLAY);
        if (n.equals("上一首")) return Prefs.KeyDef.media("上首", Usages.C_PREV);
        if (n.equals("下一首")) return Prefs.KeyDef.media("下首", Usages.C_NEXT);
        if (n.equals("停止")) return Prefs.KeyDef.media("停止", Usages.C_STOP);
        if (n.equals("音量+")) return Prefs.KeyDef.media("音+", Usages.C_VOLUP);
        if (n.equals("音量-")) return Prefs.KeyDef.media("音-", Usages.C_VOLDOWN);
        if (n.equals("静音")) return Prefs.KeyDef.media("静音", Usages.C_MUTE);
        if (n.equals("主页")) return Prefs.KeyDef.media("主页", Usages.C_HOME);
        if (n.equals("返回")) return Prefs.KeyDef.media("返回", Usages.C_BACK);
        if (n.equals("亮度+")) return Prefs.KeyDef.media("亮+", Usages.C_BRIGHT_UP);
        if (n.equals("亮度-")) return Prefs.KeyDef.media("亮-", Usages.C_BRIGHT_DOWN);

        int u = keyUsage(n);
        if (u < 0) u = Usages.SPACE;
        return Prefs.KeyDef.key(n.length() > 6 ? n.substring(0, 6) : n, 0, u);
    }

    private static int letterUsage(String s) {
        int c = s.toUpperCase().charAt(0);
        if (c >= 'A' && c <= 'Z') return Usages.A + (c - 'A');
        return Usages.SPACE;
    }

    private static int keyUsage(String n) {
        if (n.equals("Esc")) return Usages.ESC;
        if (n.equals("Tab")) return Usages.TAB;
        if (n.equals("Enter")) return Usages.ENTER;
        if (n.equals("Backspace")) return Usages.BACKSPACE;
        if (n.equals("Space")) return Usages.SPACE;
        if (n.equals("Delete")) return Usages.DELETE;
        if (n.equals("Home")) return Usages.HOME;
        if (n.equals("End")) return Usages.END;
        if (n.equals("PgUp")) return Usages.PAGEUP;
        if (n.equals("PgDn")) return Usages.PAGEDOWN;
        if (n.equals("↑")) return Usages.UP;
        if (n.equals("↓")) return Usages.DOWN;
        if (n.equals("←")) return Usages.LEFT;
        if (n.equals("→")) return Usages.RIGHT;
        if (n.equals("PrintScreen")) return Usages.PRINT;
        if (n.length() == 1) {
            char c = n.charAt(0);
            if (c >= '0' && c <= '9') return c == '0' ? Usages.N0 : Usages.N1 + (c - '1');
            if (c >= 'A' && c <= 'Z') return Usages.A + (c - 'A');
            if (c == '-') return Usages.MINUS;
            if (c == '=') return Usages.EQUAL;
            if (c == ',') return Usages.COMMA;
            if (c == '.') return Usages.DOT;
            if (c == '/') return Usages.SLASH;
            if (c == ';') return Usages.SEMICOLON;
            if (c == '\'') return Usages.QUOTE;
            if (c == '[') return Usages.LBRACKET;
            if (c == ']') return Usages.RBRACKET;
            if (c == '\\') return Usages.BACKSLASH;
            if (c == '`') return Usages.GRAVE;
        }
        if (n.startsWith("F") && n.length() <= 3) {
            try {
                int idx = Integer.parseInt(n.substring(1));
                if (idx >= 1 && idx <= 12) return Usages.F1 + (idx - 1);
            } catch (Throwable ignored) {
            }
        }
        return -1;
    }
}
