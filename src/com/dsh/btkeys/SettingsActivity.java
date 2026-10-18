package com.dsh.btkeys;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SettingsActivity extends BaseActivity {

    private Prefs prefs;
    private LinearLayout vibBox;
    private LinearLayout colorBox;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = Prefs.get(this);
        build("设置", true);

        // ---- vibration ----
        body.addView(section("按键振动"));
        vibBox = Ui.row(this);
        body.addView(vibBox, top(10));
        renderVibration();

        // ---- cursor colour ----
        body.addView(section("触控板光标颜色"), top(22));
        colorBox = Ui.row(this);
        body.addView(colorBox, top(10));
        renderColors();

        // ---- screen ----
        body.addView(section("屏幕"), top(22));
        LinearLayout keep = rowItem("保持屏幕常亮", "操作时不让屏幕自动熄灭");
        TextSwitch sw = new TextSwitch(prefs.keepScreenOn(), new TextSwitch.OnChange() {
            @Override public void onChange(boolean on) {
                prefs.setKeepScreenOn(on);
                if (on) {
                    getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                } else {
                    getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
            }
        });
        keep.addView(sw.asView(this));
        body.addView(keep, top(8));

        // ---- data ----
        body.addView(section("按钮档案"), top(22));
        TextView reset = Ui.button(this, "恢复默认档案（办公 / 游戏 / 影音）", Ui.ACCENT);
        reset.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                prefs.saveProfiles(Prefs.defaultProfiles());
                prefs.setCurrentProfile("办公");
                toast("已恢复默认档案");
            }
        });
        body.addView(reset, top(8));

        // ---- help ----
        body.addView(section("使用说明"), top(22));
        LinearLayout help = cardBlock();
        help.addView(Ui.text(this,
                "• 手机通过标准蓝牙 HID 协议连接电脑，无需安装任何电脑端软件\n"
                        + "• 电脑上会显示为一个蓝牙键盘 / 鼠标设备\n"
                        + "• 部分电脑需要先在系统蓝牙设置里配对一次\n"
                        + "• 中文输入请在电脑的输入法里切换，本应用只负责发送按键\n"
                        + "• 触控板：单指滑动移动光标，单击左键，双指上下滑动滚动，长按后拖动",
                12, Ui.SUB));
        body.addView(help, top(8));

        // ---- about ----
        body.addView(section("关于"), top(22));
        LinearLayout about = cardBlock();
        about.addView(Ui.text(this, "蓝牙键鼠遥控", 15, Ui.TEXT));
        about.addView(Ui.text(this, "版本 1.0.0", 12, Ui.SUB), top(6));
        about.addView(Ui.text(this, "无广告 · 无会员 · 免登录 · 全部功能免费开放", 12, Ui.OK), top(6));
        body.addView(about, top(8));

        Ui.space(body, this, 20);
    }

    private LinearLayout.LayoutParams top(int dp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(this, dp);
        return lp;
    }

    private View section(String s) {
        return Ui.text(this, s, 13, Ui.SUB);
    }

    private void renderVibration() {
        vibBox.removeAllViews();
        String[] labels = {"关闭", "轻柔", "适中", "明显"};
        for (int i = 0; i < labels.length; i++) {
            final int v = i;
            TextView t = Ui.key(this, labels[i], null, prefs.vibration() == i ? Ui.ACCENT : Ui.KEY);
            t.setTextColor(prefs.vibration() == i ? 0xFF0E1014 : Ui.TEXT);
            t.setTextSize(13);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.dp(this, 48), 1f);
            lp.leftMargin = Ui.dp(this, 3);
            lp.rightMargin = Ui.dp(this, 3);
            t.setLayoutParams(lp);
            t.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    prefs.setVibration(v);
                    if (v > 0) Haptics.tap(SettingsActivity.this, v);
                    renderVibration();
                }
            });
            vibBox.addView(t);
        }
    }

    private void renderColors() {
        colorBox.removeAllViews();
        int[] colors = {0xFF4C8DFF, 0xFF2FBF71, 0xFFF0A93B, 0xFFE5544B, 0xFFB07CFF, 0xFFEDF0F5};
        for (int i = 0; i < colors.length; i++) {
            final int c = colors[i];
            View v = new View(this);
            v.setBackground(Ui.bgStroke(c, 12, prefs.cursorColor() == c ? 3 : 0,
                    prefs.cursorColor() == c ? 0xFFFFFFFF : 0x00000000, this));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.dp(this, 48), 1f);
            lp.leftMargin = Ui.dp(this, 3);
            lp.rightMargin = Ui.dp(this, 3);
            v.setLayoutParams(lp);
            v.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View view) {
                    prefs.setCursorColor(c);
                    renderColors();
                }
            });
            colorBox.addView(v);
        }
    }

    private LinearLayout rowItem(String title, String sub) {
        LinearLayout row = Ui.row(this);
        row.setBackground(Ui.bg(Ui.CARD, 12, this));
        row.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
        LinearLayout info = Ui.col(this);
        info.addView(Ui.text(this, title, 14, Ui.TEXT));
        info.addView(Ui.text(this, sub, 11, Ui.SUB), top(4));
        row.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return row;
    }

    /** Minimal text-based toggle so we do not depend on AppCompat SwitchCompat. */
    private static class TextSwitch {
        interface OnChange {
            void onChange(boolean on);
        }

        private boolean on;
        private final OnChange change;
        private TextView view;

        TextSwitch(boolean initial, OnChange c) {
            on = initial;
            change = c;
        }

        View asView(final android.content.Context c) {
            view = Ui.text(c, on ? "已开启" : "已关闭", 13, on ? 0xFF0E1014 : Ui.SUB);
            view.setGravity(Gravity.CENTER);
            view.setPadding(Ui.dp(c, 14), Ui.dp(c, 7), Ui.dp(c, 14), Ui.dp(c, 7));
            view.setBackground(Ui.ripple(on ? Ui.ACCENT : Ui.KEY, 20, c));
            view.setClickable(true);
            view.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    on = !on;
                    view.setText(on ? "已开启" : "已关闭");
                    view.setTextColor(on ? 0xFF0E1014 : Ui.SUB);
                    view.setBackground(Ui.ripple(on ? Ui.ACCENT : Ui.KEY, 20, c));
                    change.onChange(on);
                }
            });
            return view;
        }
    }
}
