package com.dsh.btkeys;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHidDevice;
import android.bluetooth.BluetoothHidDeviceAppSdpSettings;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import java.util.ArrayList;
import java.util.List;

/**
 * Bluetooth HID-device engine: turns the phone into a standard Bluetooth
 * keyboard + relative mouse + consumer-control remote, so any PC / TV box
 * sees it as a normal wireless keyboard and needs no driver at all.
 */
public final class Hid {

    public static final int RID_KEY = 1;
    public static final int RID_MOUSE = 2;
    public static final int RID_MEDIA = 3;

    public static final int ST_OFF = 0;       // bluetooth off / not started
    public static final int ST_INIT = 1;      // connecting to the HID profile service
    public static final int ST_READY = 2;     // HID app registered, waiting for a host
    public static final int ST_CONNECTED = 3;
    public static final int ST_FAIL = 4;

    public interface Listener {
        void onHidChanged();
    }

    private static Hid sInstance;

    public static Hid get(Context c) {
        if (sInstance == null) sInstance = new Hid(c.getApplicationContext());
        return sInstance;
    }

    private final Context app;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final List<Listener> listeners = new ArrayList<>();

    private BluetoothAdapter adapter;
    private BluetoothHidDevice hid;
    private BluetoothDevice target;
    private BluetoothDevice pending;
    private boolean registered;
    private int state = ST_OFF;
    private String message = "";
    private boolean registering;

    private Hid(Context c) {
        app = c;
        adapter = BluetoothAdapter.getDefaultAdapter();
    }

    // ------------------------------------------------------------------ observer

    public void addListener(Listener l) {
        if (l != null && !listeners.contains(l)) listeners.add(l);
    }

    public void removeListener(Listener l) {
        listeners.remove(l);
    }

    private void notifyChanged() {
        ui.post(new Runnable() {
            @Override public void run() {
                for (Listener l : new ArrayList<>(listeners)) l.onHidChanged();
            }
        });
    }

    // ------------------------------------------------------------------ state

    public int getState() { return state; }

    public String getMessage() { return message; }

    public BluetoothAdapter getAdapter() { return adapter; }

    public BluetoothDevice getTarget() { return target; }

    public boolean isConnected() { return state == ST_CONNECTED && target != null; }

    public static boolean hasBtPermission(Context c) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return c.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    public static String[] permissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN};
        }
        return new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
    }

    private void set(int st, String msg) {
        state = st;
        message = msg == null ? "" : msg;
        notifyChanged();
    }

    /** Called from an Activity (or any context) once permission is granted. */
    public void start() {
        if (!hasBtPermission(app)) {
            set(ST_OFF, "未授予蓝牙权限");
            return;
        }
        if (adapter == null) {
            set(ST_FAIL, "此设备不支持蓝牙");
            return;
        }
        if (!adapter.isEnabled()) {
            set(ST_OFF, "请先开启蓝牙");
            return;
        }
        if (hid != null) {
            if (!registered) {
                // registration previously failed: retry instead of pretending to be ready
                if (!registering) register();
            } else if (state != ST_CONNECTED && state != ST_READY) {
                set(ST_READY, "键鼠服务已就绪");
            }
            return;
        }
        if (registering) return;
        registering = true;
        set(ST_INIT, "正在启动键鼠服务…");
        ui.postDelayed(new Runnable() {
            @Override public void run() {
                if (hid == null && state == ST_INIT) {
                    registering = false;
                    set(ST_FAIL, "此设备不支持蓝牙键鼠模式");
                }
            }
        }, 6000);
        boolean ok = adapter.getProfileProxy(app, proxyListener, BluetoothProfile.HID_DEVICE);
        if (!ok) {
            registering = false;
            set(ST_FAIL, "无法启动键鼠服务");
        }
    }

    private final BluetoothProfile.ServiceListener proxyListener = new BluetoothProfile.ServiceListener() {
        @Override public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (profile != BluetoothProfile.HID_DEVICE) return;
            hid = (BluetoothHidDevice) proxy;
            register();
        }

        @Override public void onServiceDisconnected(int profile) {
            hid = null;
            target = null;
            registered = false;
            registering = false;
            set(ST_OFF, "键鼠服务已断开");
        }
    };

    private void register() {
        if (hid == null) return;
        try {
            BluetoothHidDeviceAppSdpSettings sdp = new BluetoothHidDeviceAppSdpSettings(
                    "KbMouse", "Phone keyboard & mouse", "dsh",
                    (byte) 0xC0, descriptor());
            boolean ok = hid.registerApp(sdp, null, null, app.getMainExecutor(), callback);
            if (!ok) {
                registering = false;
                set(ST_FAIL, "键鼠服务注册失败");
            }
        } catch (Throwable t) {
            registering = false;
            set(ST_FAIL, "键鼠服务注册失败：" + t.getMessage());
        }
    }

    private final BluetoothHidDevice.Callback callback = new BluetoothHidDevice.Callback() {
        @Override public void onAppStatusChanged(BluetoothDevice plugged, boolean reg) {
            registering = false;
            registered = reg;
            if (reg) {
                if (pending != null) {
                    BluetoothDevice p = pending;
                    pending = null;
                    set(ST_READY, "键鼠服务已就绪，正在连接 " + safeName(p) + "…");
                    doConnect(p);
                } else if (state != ST_CONNECTED) {
                    set(ST_READY, "键鼠服务已就绪，请选择已配对设备连接");
                }
            } else {
                set(ST_FAIL, "键鼠服务已停止");
            }
        }

        @Override public void onConnectionStateChanged(BluetoothDevice device, int st) {
            if (st == BluetoothProfile.STATE_CONNECTED) {
                target = device;
                String n = safeName(device);
                set(ST_CONNECTED, "已连接 " + n);
            } else if (st == BluetoothProfile.STATE_DISCONNECTED) {
                if (device != null && device.equals(target)) target = null;
                set(ST_READY, "连接已断开");
            }
        }

        @Override public void onVirtualCableUnplug(BluetoothDevice device) {
            target = null;
            set(ST_READY, "连接已断开");
        }
    };

    // ------------------------------------------------------------------ connect

    public void connect(BluetoothDevice d) {
        if (d == null) return;
        if (!hasBtPermission(app)) {
            message = "未授予蓝牙权限";
            notifyChanged();
            return;
        }
        if (hid == null || !registered) {
            // service is still starting: remember the device and connect once registered
            pending = d;
            message = "正在准备键鼠服务…";
            notifyChanged();
            start();
            return;
        }
        doConnect(d);
    }

    private void doConnect(BluetoothDevice d) {
        try {
            if (d.getBondState() != BluetoothDevice.BOND_BONDED) {
                message = "正在配对…请在两台设备上确认";
                notifyChanged();
                d.createBond();
                return;
            }
            boolean ok = hid.connect(d);
            if (!ok) {
                message = "对方设备未接受键鼠连接";
            } else {
                message = "正在连接 " + safeName(d) + "…";
            }
            notifyChanged();
        } catch (Throwable t) {
            message = "连接失败：" + t.getMessage();
            notifyChanged();
        }
    }

    public void disconnect() {
        if (hid == null || target == null) return;
        try {
            hid.disconnect(target);
        } catch (Throwable ignored) {
        }
        target = null;
        set(ST_READY, "连接已断开");
    }

    public void stop() {
        try {
            if (hid != null && target != null) hid.disconnect(target);
            if (hid != null) hid.unregisterApp();
        } catch (Throwable ignored) {
        }
        hid = null;
        target = null;
        registered = false;
        pending = null;
        registering = false;
        set(ST_OFF, "键鼠服务已停止");
    }

    // ------------------------------------------------------------------ reports

    private boolean send(int id, byte[] data) {
        BluetoothDevice d = target;
        BluetoothHidDevice h = hid;
        if (d == null || h == null) return false;
        try {
            return h.sendReport(d, id, data);
        } catch (Throwable t) {
            return false;
        }
    }

    /** Press (and keep pressing) the given keys with the given modifier byte. */
    public boolean sendKeys(int mods, int... usages) {
        byte[] r = new byte[8];
        r[0] = (byte) mods;
        int n = Math.min(6, usages == null ? 0 : usages.length);
        for (int i = 0; i < n; i++) r[2 + i] = (byte) usages[i];
        return send(RID_KEY, r);
    }

    public void releaseKeys() {
        sendKeys(0);
    }

    /** Press then release: one complete keystroke. */
    public void tapKey(final int mods, final int... usages) {
        if (!isConnected()) return;
        sendKeys(mods, usages);
        ui.postDelayed(new Runnable() {
            @Override public void run() {
                sendKeys(0);
            }
        }, 18);
    }

    /** Type a full chord such as Ctrl+C where every key goes down together. */
    public void chord(final int mods, final int... usages) {
        tapKey(mods, usages);
    }

    public boolean sendMouse(int buttons, int dx, int dy, int wheel) {
        byte[] r = new byte[4];
        r[0] = (byte) buttons;
        r[1] = (byte) clamp(dx);
        r[2] = (byte) clamp(dy);
        r[3] = (byte) clamp(wheel);
        return send(RID_MOUSE, r);
    }

    private static int clamp(int v) {
        if (v > 127) return 127;
        if (v < -127) return -127;
        return v;
    }

    public void clickMouse(final int button) {
        if (!isConnected()) return;
        sendMouse(button, 0, 0, 0);
        ui.postDelayed(new Runnable() {
            @Override public void run() {
                sendMouse(0, 0, 0, 0);
            }
        }, 26);
    }

    public void doubleClick() {
        if (!isConnected()) return;
        sendMouse(Usages.B_LEFT, 0, 0, 0);
        ui.postDelayed(new Runnable() {
            @Override public void run() {
                sendMouse(0, 0, 0, 0);
                ui.postDelayed(new Runnable() {
                    @Override public void run() {
                        sendMouse(Usages.B_LEFT, 0, 0, 0);
                        ui.postDelayed(new Runnable() {
                            @Override public void run() {
                                sendMouse(0, 0, 0, 0);
                            }
                        }, 26);
                    }
                }, 60);
            }
        }, 26);
    }

    public void media(final int usage) {
        if (!isConnected()) return;
        sendMediaReport(0);
        ui.postDelayed(new Runnable() {
            @Override public void run() {
                sendMediaReport(usage);
                ui.postDelayed(new Runnable() {
                    @Override public void run() {
                        sendMediaReport(0);
                    }
                }, 18);
            }
        }, 12);
    }

    private void sendMediaReport(int usage) {
        byte[] r = new byte[]{(byte) (usage & 0xFF), (byte) ((usage >> 8) & 0xFF)};
        send(RID_MEDIA, r);
    }

    // ------------------------------------------------------------------ misc

    public static String safeName(BluetoothDevice d) {
        if (d == null) return "设备";
        try {
            String n = d.getName();
            if (n != null && n.length() > 0) return n;
            String a = d.getAddress();
            return a == null ? "设备" : a;
        } catch (Throwable t) {
            return "设备";
        }
    }

    public static String btStateText(int st) {
        switch (st) {
            case ST_OFF: return "未启动";
            case ST_INIT: return "启动中";
            case ST_READY: return "就绪";
            case ST_CONNECTED: return "已连接";
            case ST_FAIL: return "异常";
            default: return "未知";
        }
    }

    public static int btStateColor(int st) {
        switch (st) {
            case ST_CONNECTED: return Ui.OK;
            case ST_READY: return Ui.ACCENT;
            case ST_INIT: return Ui.WARN;
            case ST_FAIL: return Ui.DANGER;
            default: return Ui.SUB;
        }
    }

    // ------------------------------------------------------------------ descriptor

    /** Combined keyboard + mouse + consumer-control report descriptor. */
    private static byte[] descriptor() {
        int[] d = {
                // ---- keyboard (report id 1) ----
                0x05, 0x01,             // usage page (generic desktop)
                0x09, 0x06,             // usage (keyboard)
                0xA1, 0x01,             // collection (application)
                0x85, 0x01,             //   report id 1
                0x05, 0x07,             //   usage page (keyboard)
                0x19, 0xE0,             //   usage min (left ctrl)
                0x29, 0xE7,             //   usage max (right gui)
                0x15, 0x00,             //   logical min 0
                0x25, 0x01,             //   logical max 1
                0x75, 0x01,             //   report size 1
                0x95, 0x08,             //   report count 8
                0x81, 0x02,             //   input (data, variable, absolute) modifiers
                0x95, 0x01,             //   report count 1
                0x75, 0x08,             //   report size 8
                0x81, 0x01,             //   input (constant) reserved byte
                0x95, 0x06,             //   report count 6
                0x75, 0x08,             //   report size 8
                0x15, 0x00,             //   logical min 0
                0x25, 0x65,             //   logical max 101
                0x05, 0x07,             //   usage page (keyboard)
                0x19, 0x00,             //   usage min 0
                0x29, 0x65,             //   usage max 101
                0x81, 0x00,             //   input (data, array) six keys
                0xC0,                   // end collection

                // ---- mouse (report id 2) ----
                0x05, 0x01,             // usage page (generic desktop)
                0x09, 0x02,             // usage (mouse)
                0xA1, 0x01,             // collection (application)
                0x85, 0x02,             //   report id 2
                0x09, 0x01,             //   usage (pointer)
                0xA1, 0x00,             //   collection (physical)
                0x05, 0x09,             //     usage page (button)
                0x19, 0x01,             //     usage min 1
                0x29, 0x05,             //     usage max 5
                0x15, 0x00,             //     logical min 0
                0x25, 0x01,             //     logical max 1
                0x95, 0x05,             //     report count 5
                0x75, 0x01,             //     report size 1
                0x81, 0x02,             //     input (data, variable, absolute) buttons
                0x95, 0x01,             //     report count 1
                0x75, 0x03,             //     report size 3
                0x81, 0x03,             //     input (constant) padding
                0x05, 0x01,             //     usage page (generic desktop)
                0x09, 0x30,             //     usage (x)
                0x09, 0x31,             //     usage (y)
                0x09, 0x38,             //     usage (wheel)
                0x15, 0x81,             //     logical min -127
                0x25, 0x7F,             //     logical max 127
                0x75, 0x08,             //     report size 8
                0x95, 0x03,             //     report count 3
                0x81, 0x06,             //     input (data, variable, relative)
                0xC0,                   //   end collection
                0xC0,                   // end collection

                // ---- consumer control (report id 3) ----
                0x05, 0x0C,             // usage page (consumer)
                0x09, 0x01,             // usage (consumer control)
                0xA1, 0x01,             // collection (application)
                0x85, 0x03,             //   report id 3
                0x15, 0x00,             //   logical min 0
                0x26, 0xFF, 0x03,       //   logical max 0x03FF
                0x19, 0x00,             //   usage min 0
                0x2A, 0xFF, 0x03,       //   usage max 0x03FF
                0x75, 0x10,             //   report size 16
                0x95, 0x01,             //   report count 1
                0x81, 0x00,             //   input (data, array)
                0xC0                    // end collection
        };
        byte[] out = new byte[d.length];
        for (int i = 0; i < d.length; i++) out[i] = (byte) d[i];
        return out;
    }
}
