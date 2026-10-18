package com.dsh.btkeys;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Settings + saved button-layout profiles. Everything is local, no account needed. */
public final class Prefs {

    public static final int TYPE_KEY = 0;
    public static final int TYPE_MOUSE = 1;
    public static final int TYPE_MEDIA = 2;

    public static final class KeyDef {
        public String label = "按键";
        public int type = TYPE_KEY;
        public int mods = 0;
        public int[] usages = new int[0];
        public int button = Usages.B_LEFT;
        public int x = 24, y = 24, w = 84, h = 62;

        public KeyDef copy() {
            KeyDef k = new KeyDef();
            k.label = label;
            k.type = type;
            k.mods = mods;
            k.usages = usages.clone();
            k.button = button;
            k.x = x;
            k.y = y;
            k.w = w;
            k.h = h;
            return k;
        }

        public JSONObject toJson() {
            try {
                JSONObject o = new JSONObject();
                o.put("l", label);
                o.put("t", type);
                o.put("m", mods);
                JSONArray a = new JSONArray();
                for (int u : usages) a.put(u);
                o.put("u", a);
                o.put("b", button);
                o.put("x", x);
                o.put("y", y);
                o.put("w", w);
                o.put("h", h);
                return o;
            } catch (Exception e) {
                return new JSONObject();
            }
        }

        public static KeyDef fromJson(JSONObject o) {
            KeyDef k = new KeyDef();
            k.label = o.optString("l", "按键");
            k.type = o.optInt("t", TYPE_KEY);
            k.mods = o.optInt("m", 0);
            JSONArray a = o.optJSONArray("u");
            if (a != null) {
                k.usages = new int[a.length()];
                for (int i = 0; i < a.length(); i++) k.usages[i] = a.optInt(i);
            }
            k.button = o.optInt("b", Usages.B_LEFT);
            k.x = o.optInt("x", 24);
            k.y = o.optInt("y", 24);
            k.w = o.optInt("w", 84);
            k.h = o.optInt("h", 62);
            return k;
        }

        public static KeyDef key(String label, int mods, int... usages) {
            KeyDef k = new KeyDef();
            k.label = label;
            k.type = TYPE_KEY;
            k.mods = mods;
            k.usages = usages;
            return k;
        }

        public static KeyDef mouse(String label, int button) {
            KeyDef k = new KeyDef();
            k.label = label;
            k.type = TYPE_MOUSE;
            k.button = button;
            return k;
        }

        public static KeyDef media(String label, int usage) {
            KeyDef k = new KeyDef();
            k.label = label;
            k.type = TYPE_MEDIA;
            k.usages = new int[]{usage};
            return k;
        }
    }

    public static final class Profile {
        public String name = "默认档案";
        public final List<KeyDef> keys = new ArrayList<>();

        public JSONObject toJson() {
            try {
                JSONObject o = new JSONObject();
                o.put("n", name);
                JSONArray a = new JSONArray();
                for (KeyDef k : keys) a.put(k.toJson());
                o.put("k", a);
                return o;
            } catch (Exception e) {
                return new JSONObject();
            }
        }

        public static Profile fromJson(JSONObject o) {
            Profile p = new Profile();
            p.name = o.optString("n", "档案");
            JSONArray a = o.optJSONArray("k");
            if (a != null) {
                for (int i = 0; i < a.length(); i++) {
                    JSONObject ko = a.optJSONObject(i);
                    if (ko != null) p.keys.add(KeyDef.fromJson(ko));
                }
            }
            return p;
        }
    }

    private static Prefs s;
    private final SharedPreferences sp;

    private Prefs(Context c) {
        sp = c.getApplicationContext().getSharedPreferences("btkeys", Context.MODE_PRIVATE);
    }

    public static Prefs get(Context c) {
        if (s == null) s = new Prefs(c);
        return s;
    }

    // ---- simple settings ----

    public int vibration() { return sp.getInt("vib", 2); }

    public void setVibration(int v) { sp.edit().putInt("vib", v).apply(); }

    public int cursorColor() { return sp.getInt("cursor", 0xFF4C8DFF); }

    public void setCursorColor(int c) { sp.edit().putInt("cursor", c).apply(); }

    public int padSpeed() { return sp.getInt("padspeed", 50); }

    public void setPadSpeed(int v) { sp.edit().putInt("padspeed", v).apply(); }

    public boolean keepScreenOn() { return sp.getBoolean("keepon", true); }

    public void setKeepScreenOn(boolean v) { sp.edit().putBoolean("keepon", v).apply(); }

    public boolean soundFeedback() { return sp.getBoolean("sound", false); }

    public void setSoundFeedback(boolean v) { sp.edit().putBoolean("sound", v).apply(); }

    public boolean firstRun() { return sp.getBoolean("firstrun", true); }

    public void setFirstRun(boolean v) { sp.edit().putBoolean("firstrun", v).apply(); }

    // ---- profiles ----

    public List<Profile> profiles() {
        String raw = sp.getString("profiles", null);
        List<Profile> out = new ArrayList<>();
        if (raw != null) {
            try {
                JSONArray a = new JSONArray(raw);
                for (int i = 0; i < a.length(); i++) {
                    JSONObject o = a.optJSONObject(i);
                    if (o != null) out.add(Profile.fromJson(o));
                }
            } catch (Exception ignored) {
            }
        }
        if (out.isEmpty()) {
            out.addAll(defaultProfiles());
            saveProfiles(out);
        }
        return out;
    }

    public void saveProfiles(List<Profile> ps) {
        JSONArray a = new JSONArray();
        for (Profile p : ps) a.put(p.toJson());
        sp.edit().putString("profiles", a.toString()).apply();
    }

    public String currentProfileName() {
        List<Profile> ps = profiles();
        String n = sp.getString("current", null);
        for (Profile p : ps) if (p.name.equals(n)) return n;
        return ps.get(0).name;
    }

    public void setCurrentProfile(String name) { sp.edit().putString("current", name).apply(); }

    public Profile currentProfile() {
        List<Profile> ps = profiles();
        String n = currentProfileName();
        for (Profile p : ps) if (p.name.equals(n)) return p;
        return ps.get(0);
    }

    public static List<Profile> defaultProfiles() {
        List<Profile> out = new ArrayList<>();

        Profile office = new Profile();
        office.name = "办公";
        KeyDef c = KeyDef.key("复制", Usages.M_CTRL, Usages.C);
        c.x = 24; c.y = 24;
        KeyDef v = KeyDef.key("粘贴", Usages.M_CTRL, Usages.V);
        v.x = 120; v.y = 24;
        KeyDef x = KeyDef.key("剪切", Usages.M_CTRL, Usages.X);
        x.x = 216; x.y = 24;
        KeyDef z = KeyDef.key("撤销", Usages.M_CTRL, Usages.Z);
        z.x = 24; z.y = 98;
        KeyDef win = KeyDef.key("开始", Usages.M_GUI, Usages.ESC);
        win.x = 120; win.y = 98;
        KeyDef tab = KeyDef.key("切换", Usages.M_ALT, Usages.TAB);
        tab.x = 216; tab.y = 98;
        KeyDef prt = KeyDef.key("截图", Usages.M_GUI, Usages.PRINT);
        prt.x = 24; prt.y = 172;
        KeyDef lock = KeyDef.key("锁屏", Usages.M_GUI, Usages.L);
        lock.x = 120; lock.y = 172;
        office.keys.add(c); office.keys.add(v); office.keys.add(x); office.keys.add(z);
        office.keys.add(win); office.keys.add(tab); office.keys.add(prt); office.keys.add(lock);
        out.add(office);

        Profile game = new Profile();
        game.name = "游戏";
        String none = "";
        KeyDef w = KeyDef.key("W", 0, Usages.W); w.x = 120; w.y = 24;
        KeyDef a = KeyDef.key("A", 0, Usages.A); a.x = 24; a.y = 98;
        KeyDef s = KeyDef.key("S", 0, Usages.S); s.x = 120; s.y = 98;
        KeyDef d = KeyDef.key("D", 0, Usages.D); d.x = 216; d.y = 98;
        KeyDef sp = KeyDef.key("跳", 0, Usages.SPACE); sp.x = 24; sp.y = 172; sp.w = 180;
        KeyDef sh = KeyDef.key("Shift", Usages.M_SHIFT); sh.x = 216; sh.y = 172;
        KeyDef e = KeyDef.key("E", 0, Usages.E); e.x = 24; e.y = 246;
        KeyDef r = KeyDef.key("R", 0, Usages.R); r.x = 120; r.y = 246;
        KeyDef esc = KeyDef.key("Esc", 0, Usages.ESC); esc.x = 216; esc.y = 246;
        game.keys.add(w); game.keys.add(a); game.keys.add(s); game.keys.add(d);
        game.keys.add(sp); game.keys.add(sh); game.keys.add(e); game.keys.add(r); game.keys.add(esc);
        out.add(game);

        Profile media = new Profile();
        media.name = "影音";
        KeyDef play = KeyDef.media("播放", Usages.C_PLAY); play.x = 24; play.y = 24;
        KeyDef prev = KeyDef.media("上一首", Usages.C_PREV); prev.x = 120; prev.y = 24;
        KeyDef next = KeyDef.media("下一首", Usages.C_NEXT); next.x = 216; next.y = 24;
        KeyDef vu = KeyDef.media("音量+", Usages.C_VOLUP); vu.x = 24; vu.y = 98;
        KeyDef vd = KeyDef.media("音量-", Usages.C_VOLDOWN); vd.x = 120; vd.y = 98;
        KeyDef mu = KeyDef.media("静音", Usages.C_MUTE); mu.x = 216; mu.y = 98;
        KeyDef full = KeyDef.key("全屏", 0, Usages.F11); full.x = 24; full.y = 172;
        KeyDef esc2 = KeyDef.key("退出", 0, Usages.ESC); esc2.x = 120; esc2.y = 172;
        KeyDef hom = KeyDef.media("主页", Usages.C_HOME); hom.x = 216; hom.y = 172;
        media.keys.add(play); media.keys.add(prev); media.keys.add(next);
        media.keys.add(vu); media.keys.add(vd); media.keys.add(mu);
        media.keys.add(full); media.keys.add(esc2); media.keys.add(hom);
        out.add(media);

        return out;
    }
}
