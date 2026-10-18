package com.dsh.btkeys;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

/** Tiny haptic helper (0 = off, 1..3 = light..strong). */
public final class Haptics {
    private Haptics() {}

    public static void tap(Context c, int strength) {
        if (strength <= 0) return;
        Vibrator v = get(c);
        if (v == null || !v.hasVibrator()) return;
        int ms = strength == 1 ? 8 : strength == 2 ? 14 : 22;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(ms);
            }
        } catch (Throwable ignored) {
        }
    }

    private static Vibrator get(Context c) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vm = (VibratorManager) c.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            return vm == null ? null : vm.getDefaultVibrator();
        }
        return (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);
    }
}
