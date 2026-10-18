package com.dsh.btkeys;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainActivity extends BaseActivity {

    private TextView statusTitle;
    private TextView statusMsg;
    private TextView statusAction;
    private LinearLayout devList;
    private LinearLayout foundList;
    private TextView scanBtn;
    private LinearLayout scanBox;
    private final List<BluetoothDevice> found = new ArrayList<>();
    private boolean scanning;

    private final BroadcastReceiver btReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            String a = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(a)) {
                refreshChrome();
                refreshStatus();
                refreshDevices();
            } else if (BluetoothDevice.ACTION_FOUND.equals(a)) {
                BluetoothDevice d = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (d != null && !contains(found, d)) {
                    found.add(d);
                    renderFound();
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(a)) {
                scanning = false;
                updateScanButton();
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(a)) {
                BluetoothDevice d = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int st = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
                if (d != null && st == BluetoothDevice.BOND_BONDED) {
                    toast("配对成功，正在连接…");
                    hid.connect(d);
                }
                refreshDevices();
                renderFound();
            }
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        build("蓝牙键鼠遥控", true);
        if (backView != null) backView.setVisibility(View.GONE);

        // ---- header block ----
        TextView sub = Ui.text(this, "把手机变成电脑的无线键盘和鼠标", 13, Ui.SUB);
        body.addView(sub);

        // ---- status card ----
        LinearLayout card = cardBlock();
        LinearLayout topRow = Ui.row(this);
        statusTitle = Ui.text(this, "键鼠服务", 15, Ui.TEXT);
        topRow.addView(statusTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        statusAction = Ui.text(this, "启动", 13, Ui.ACCENT);
        statusAction.setPadding(Ui.dp(this, 12), Ui.dp(this, 6), Ui.dp(this, 12), Ui.dp(this, 6));
        statusAction.setBackground(Ui.bg(Ui.BG2, 8, this));
        statusAction.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { onStatusAction(); }
        });
        topRow.addView(statusAction);
        card.addView(topRow);
        statusMsg = Ui.text(this, "正在检查…", 13, Ui.SUB);
        card.addView(statusMsg, top(8));
        body.addView(card, top(16));

        // ---- action row ----
        LinearLayout actions = Ui.row(this);
        TextView btOn = Ui.button(this, "开启蓝牙", Ui.TEXT);
        btOn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                try {
                    startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
                } catch (Throwable t) {
                    toast("请在系统设置中开启蓝牙");
                }
            }
        });
        actions.addView(btOn, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView settings = Ui.button(this, "设置", Ui.TEXT);
        settings.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { startActivity(new Intent(MainActivity.this, SettingsActivity.class)); }
        });
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        slp.leftMargin = Ui.dp(this, 10);
        actions.addView(settings, slp);
        body.addView(actions, top(12));

        // ---- paired devices ----
        body.addView(section("已配对设备", "刷新"), top(22));
        devList = Ui.col(this);
        body.addView(devList, top(8));

        // ---- scanning ----
        scanBox = Ui.col(this);
        scanBtn = Ui.button(this, "搜索新设备", Ui.ACCENT);
        scanBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { toggleScan(); }
        });
        scanBox.addView(scanBtn);
        foundList = Ui.col(this);
        scanBox.addView(foundList, top(8));
        body.addView(scanBox, top(12));

        // ---- features ----
        body.addView(section("功能", null), top(24));
        LinearLayout grid = Ui.col(this);
        LinearLayout r1 = Ui.row(this);
        r1.addView(tile("触控板", "单击 · 拖动 · 滚动", new Intent(this, TouchpadActivity.class)), weight(1));
        r1.addView(tile("键盘", "全键盘 + 快捷键", new Intent(this, KeyboardActivity.class)), weight(1));
        grid.addView(r1, top(12));
        LinearLayout r2 = Ui.row(this);
        r2.addView(tile("数字键盘", "快速录入数字", new Intent(this, NumpadActivity.class)), weight(1, 10));
        r2.addView(tile("翻页笔", "PPT 演示遥控", new Intent(this, PptActivity.class)), weight(1));
        grid.addView(r2, top(10));
        LinearLayout r3 = Ui.row(this);
        r3.addView(tile("媒体遥控", "播放 · 音量 · 全屏", new Intent(this, MediaActivity.class)), weight(1));
        r3.addView(tile("自定义按键", "按键可拖动布局", new Intent(this, CustomKeysActivity.class)), weight(1, 10));
        grid.addView(r3, top(10));
        body.addView(grid);

        // ---- help ----
        LinearLayout help = cardBlock();
        help.addView(Ui.text(this, "如何连接", 14, Ui.TEXT));
        help.addView(Ui.text(this,
                "1. 在电脑上打开蓝牙并搜索新设备\n"
                        + "2. 找到本机名称并配对（电脑会显示为键盘）\n"
                        + "3. 回到此页面，在「已配对设备」中点连接\n"
                        + "4. 连接成功后即可使用各项遥控功能",
                12, Ui.SUB), top(8));
        body.addView(help, top(24));

        TextView footer = Ui.text(this, "v1.0.0 · 无广告 · 无需登录 · 全部功能免费", 11, Ui.FAINT);
        footer.setGravity(Gravity.LEFT);
        body.addView(footer, top(22));

        Ui.space(body, this, 10);
    }

    // ------------------------------------------------------------------ helpers

    private LinearLayout.LayoutParams top(int dp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(this, dp);
        return lp;
    }

    private LinearLayout.LayoutParams weight(float w) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.dp(this, 92), w);
        return lp;
    }

    private LinearLayout.LayoutParams weight(float w, int leftDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.dp(this, 92), w);
        lp.leftMargin = Ui.dp(this, leftDp);
        return lp;
    }

    private LinearLayout.LayoutParams tileLp(float w, int leftDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.dp(this, 92), w);
        lp.leftMargin = Ui.dp(this, leftDp);
        return lp;
    }

    private View section(String title, String action) {
        LinearLayout row = Ui.row(this);
        TextView t = Ui.text(this, title, 14, Ui.SUB);
        row.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (action != null) {
            TextView a = Ui.text(this, action, 13, Ui.ACCENT);
            a.setPadding(Ui.dp(this, 10), Ui.dp(this, 4), Ui.dp(this, 2), Ui.dp(this, 4));
            a.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { refreshDevices(); refreshStatus(); }
            });
            row.addView(a);
        }
        return row;
    }

    private View tile(String name, String sub, Intent target) {
        LinearLayout l = Ui.col(this);
        l.setBackground(Ui.ripple(Ui.CARD, 14, this));
        l.setGravity(Gravity.CENTER);
        l.setPadding(Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 8));

        TextView t = Ui.text(this, name, 16, Ui.TEXT);
        t.setGravity(Gravity.CENTER);
        l.addView(t);
        TextView s = Ui.text(this, sub, 11, Ui.SUB);
        s.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        slp.topMargin = Ui.dp(this, 6);
        l.addView(s, slp);

        l.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (!hid.isConnected()) toast("提示：请先连接电脑设备");
                startActivity(target);
            }
        });
        return l;
    }

    private static boolean contains(List<BluetoothDevice> list, BluetoothDevice d) {
        for (BluetoothDevice x : list) if (x.getAddress().equals(d.getAddress())) return true;
        return false;
    }

    // ------------------------------------------------------------------ state

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter f = new IntentFilter();
        f.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        f.addAction(BluetoothDevice.ACTION_FOUND);
        f.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        f.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(btReceiver, f, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(btReceiver, f);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !Hid.hasBtPermission(this)) {
            requestPermissions(Hid.permissions(), 7);
        }
        hid.start();
        refreshStatus();
        refreshDevices();
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(btReceiver);
        } catch (Throwable ignored) {
        }
        if (scanning) {
            try {
                hid.getAdapter().cancelDiscovery();
            } catch (Throwable ignored) {
            }
            scanning = false;
            updateScanButton();
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        super.onRequestPermissionsResult(code, perms, res);
        if (code == 7) {
            if (Hid.hasBtPermission(this)) {
                hid.start();
            } else {
                toast("未授予蓝牙权限，无法连接设备");
            }
            refreshStatus();
            refreshDevices();
        }
    }

    @Override
    public void onHidChanged() {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                refreshChrome();
                refreshStatus();
                refreshDevices();
            }
        });
    }

    private void onStatusAction() {
        if (!Hid.hasBtPermission(this)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) requestPermissions(Hid.permissions(), 7);
            return;
        }
        if (hid.getAdapter() == null) {
            toast("此设备不支持蓝牙");
            return;
        }
        if (!hid.getAdapter().isEnabled()) {
            try {
                startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
            } catch (Throwable t) {
                toast("请在系统设置中开启蓝牙");
            }
            return;
        }
        if (hid.isConnected()) {
            hid.disconnect();
        } else {
            hid.start();
        }
    }

    private void refreshStatus() {
        if (statusMsg == null) return;
        BluetoothAdapter a = hid.getAdapter();
        String msg;
        int color;
        if (a == null) {
            msg = "此设备不支持蓝牙";
            color = Ui.DANGER;
        } else if (!Hid.hasBtPermission(this)) {
            msg = "需要蓝牙权限才能连接设备，点击右侧授权";
            color = Ui.WARN;
        } else if (!a.isEnabled()) {
            msg = "蓝牙未开启，请先开启蓝牙";
            color = Ui.WARN;
        } else if (hid.isConnected()) {
            msg = "已连接 " + Hid.safeName(hid.getTarget()) + "，可以使用全部功能";
            color = Ui.OK;
        } else {
            msg = hid.getMessage();
            color = Ui.SUB;
            if (msg == null || msg.length() == 0) msg = "选择下方已配对设备进行连接";
        }
        statusMsg.setText(msg);
        statusMsg.setTextColor(color);
        statusTitle.setText("键鼠服务 · " + Hid.btStateText(hid.getState()));
        statusTitle.setTextColor(Hid.btStateColor(hid.getState()));
        statusAction.setText(hid.isConnected() ? "断开" : (a != null && !a.isEnabled() ? "开启" : "启动"));
    }

    private void refreshDevices() {
        if (devList == null) return;
        devList.removeAllViews();
        BluetoothAdapter a = hid.getAdapter();
        if (a == null || !Hid.hasBtPermission(this)) {
            devList.addView(empty("需要蓝牙权限"));
            return;
        }
        Set<BluetoothDevice> bonded;
        try {
            bonded = a.getBondedDevices();
        } catch (Throwable t) {
            bonded = null;
        }
        if (bonded == null || bonded.isEmpty()) {
            devList.addView(empty("还没有已配对设备，点击下方搜索"));
            return;
        }
        for (BluetoothDevice d : bonded) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = Ui.dp(this, 8);
            devList.addView(deviceRow(d, true), lp);
        }
    }

    private View empty(String s) {
        TextView t = Ui.text(this, s, 13, Ui.SUB);
        t.setPadding(Ui.dp(this, 14), Ui.dp(this, 16), Ui.dp(this, 14), Ui.dp(this, 16));
        t.setBackground(Ui.bg(Ui.CARD, 12, this));
        return t;
    }

    private View deviceRow(final BluetoothDevice d, boolean bonded) {
        LinearLayout row = Ui.row(this);
        row.setBackground(Ui.bg(Ui.CARD, 12, this));
        row.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12));

        LinearLayout info = Ui.col(this);
        String name = Hid.safeName(d);
        TextView n = Ui.text(this, name, 15, Ui.TEXT);
        info.addView(n);
        boolean cur = hid.getTarget() != null && d.getAddress().equals(hid.getTarget().getAddress());
        TextView s = Ui.text(this, cur ? "当前连接设备" : (bonded ? "已配对" : "未配对"), 11, cur ? Ui.OK : Ui.SUB);
        info.addView(s, topMargin(4));
        row.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView act = Ui.text(this, cur ? "断开" : (bonded ? "连接" : "配对"), 13, cur ? Ui.DANGER : Ui.ACCENT);
        act.setPadding(Ui.dp(this, 14), Ui.dp(this, 8), Ui.dp(this, 14), Ui.dp(this, 8));
        act.setBackground(Ui.bg(Ui.BG2, 9, this));
        act.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (hid.getTarget() != null && d.getAddress().equals(hid.getTarget().getAddress())) {
                    hid.disconnect();
                } else {
                    hid.start();
                    hid.connect(d);
                }
            }
        });
        row.addView(act);
        return row;
    }

    private LinearLayout.LayoutParams topMargin(int dp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(this, dp);
        return lp;
    }

    private void toggleScan() {
        BluetoothAdapter a = hid.getAdapter();
        if (a == null || !Hid.hasBtPermission(this)) {
            toast("缺少蓝牙权限");
            return;
        }
        if (!a.isEnabled()) {
            toast("请先开启蓝牙");
            return;
        }
        if (scanning) {
            try {
                a.cancelDiscovery();
            } catch (Throwable ignored) {
            }
            scanning = false;
            updateScanButton();
            return;
        }
        found.clear();
        renderFound();
        try {
            scanning = a.startDiscovery();
        } catch (Throwable t) {
            scanning = false;
        }
        updateScanButton();
        if (!scanning) toast("搜索启动失败，请稍后重试");
    }

    private void updateScanButton() {
        if (scanBtn == null) return;
        scanBtn.setText(scanning ? "停止搜索" : "搜索新设备");
        scanBtn.setTextColor(scanning ? Ui.DANGER : Ui.ACCENT);
    }

    private void renderFound() {
        if (foundList == null) return;
        foundList.removeAllViews();
        if (found.isEmpty()) {
            TextView t = Ui.text(this, scanning ? "正在搜索附近的设备…" : "未发现新设备", 12, Ui.SUB);
            t.setPadding(Ui.dp(this, 14), Ui.dp(this, 10), Ui.dp(this, 14), Ui.dp(this, 10));
            foundList.addView(t);
            return;
        }
        for (BluetoothDevice d : new ArrayList<>(found)) {
            View r = deviceRow(d, d.getBondState() == BluetoothDevice.BOND_BONDED);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = Ui.dp(this, 8);
            foundList.addView(r, lp);
        }
    }
}
