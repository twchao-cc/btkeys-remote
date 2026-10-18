package com.dsh.btkeys;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class TouchpadActivity extends BaseActivity {

    private PadView pad;
    private int speed;
    private TextView speedLabel;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        build("触控板", false);
        speed = Prefs.get(this).padSpeed();

        LinearLayout wrap = Ui.col(this);
        wrap.setBackground(Ui.bg(0xFF12151C, 18, this));
        wrap.setClipToOutline(true);

        pad = new PadView();
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        wrap.addView(pad, plp);
        body.addView(wrap, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // ---- mouse buttons ----
        LinearLayout btns = Ui.row(this);
        LinearLayout l = bigButton("左键", "单击");
        l.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { click(Usages.B_LEFT); }
        });
        LinearLayout m = bigButton("中键", "滚轮键");
        m.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { click(Usages.B_MIDDLE); }
        });
        LinearLayout r = bigButton("右键", "菜单");
        r.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { click(Usages.B_RIGHT); }
        });
        btns.addView(l, weight(1, 0));
        btns.addView(m, weight(1, 8));
        btns.addView(r, weight(1, 8));
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.topMargin = Ui.dp(this, 12);
        body.addView(btns, blp);

        // ---- sensitivity ----
        LinearLayout sp = Ui.row(this);
        TextView lab = Ui.text(this, "灵敏度", 12, Ui.SUB);
        sp.addView(lab);
        speedLabel = Ui.text(this, String.valueOf(speed), 12, Ui.TEXT);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        llp.leftMargin = Ui.dp(this, 8);
        sp.addView(speedLabel, llp);

        SeekBar bar = new SeekBar(this);
        bar.setMax(100);
        bar.setProgress(speed);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean fromUser) {
                speed = p;
                speedLabel.setText(String.valueOf(p));
                Prefs.get(TouchpadActivity.this).setPadSpeed(p);
            }
            @Override public void onStartTrackingTouch(SeekBar sb) { }
            @Override public void onStopTrackingTouch(SeekBar sb) { }
        });
        LinearLayout.LayoutParams balp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        balp.leftMargin = Ui.dp(this, 8);
        sp.addView(bar, balp);
        LinearLayout.LayoutParams splp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        splp.topMargin = Ui.dp(this, 8);
        body.addView(sp, splp);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (pad != null) pad.cancel();
    }

    private LinearLayout.LayoutParams weight(float w, int leftDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, Ui.dp(this, 62), w);
        lp.leftMargin = Ui.dp(this, leftDp);
        return lp;
    }

    private LinearLayout bigButton(String label, String sub) {
        LinearLayout l = Ui.col(this);
        l.setBackground(Ui.ripple(Ui.KEY, 12, this));
        l.setGravity(Gravity.CENTER);
        l.setClickable(true);
        TextView t = Ui.text(this, label, 15, Ui.TEXT);
        t.setGravity(Gravity.CENTER);
        l.addView(t);
        TextView s = Ui.text(this, sub, 10, Ui.SUB);
        s.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = Ui.dp(this, 3);
        l.addView(s, lp);
        return l;
    }

    private void click(int button) {
        if (!hid.isConnected()) {
            toast("未连接设备");
            return;
        }
        haptic();
        hid.clickMouse(button);
    }

    private float speedFactor() {
        return 0.55f + (speed / 100f) * 1.95f;
    }

    // ------------------------------------------------------------------ pad

    private class PadView extends View {
        private final Paint bgPaint = new Paint();
        private final Paint dotPaint = new Paint();
        private final Paint ringPaint = new Paint();
        private final Paint hintPaint = new Paint();
        private final Handler h = new Handler(Looper.getMainLooper());

        private float lastX, lastY, downX, downY;
        private float lastAvgY;
        private float accX, accY, accScroll;
        private long downTime;
        private boolean moved, dragging, touching;
        private int maxPointers = 1;

        private final Runnable longPressRunnable = new Runnable() {
            @Override public void run() {
                startDrag();
            }
        };

        PadView() {
            super(TouchpadActivity.this);
            setBackgroundColor(Color.TRANSPARENT);
            bgPaint.setColor(0xFF12151C);
            dotPaint.setColor(0xFF222834);
            ringPaint.setStyle(Paint.Style.STROKE);
            ringPaint.setStrokeWidth(Ui.dp(TouchpadActivity.this, 2));
            ringPaint.setAntiAlias(true);
            hintPaint.setColor(0xFF7A8496);
            hintPaint.setTextSize(Ui.dp(TouchpadActivity.this, 11));
            hintPaint.setAntiAlias(true);
            hintPaint.setTypeface(Typeface.DEFAULT);
        }

        @Override protected void onDraw(Canvas c) {
            int w = getWidth(), hgt = getHeight();
            c.drawRect(0, 0, w, hgt, bgPaint);

            int step = Ui.dp(TouchpadActivity.this, 22);
            for (int x = step; x < w; x += step) {
                for (int y = step; y < hgt; y += step) {
                    c.drawCircle(x, y, Ui.dp(TouchpadActivity.this, 1), dotPaint);
                }
            }

            c.drawText("单指滑动移动 · 单击左键 · 双指滚动 · 长按拖动", Ui.dp(TouchpadActivity.this, 16),
                    Ui.dp(TouchpadActivity.this, 22), hintPaint);

            if (touching) {
                int color = Prefs.get(TouchpadActivity.this).cursorColor();
                ringPaint.setColor(dragging ? Ui.OK : color);
                float r = Ui.dp(TouchpadActivity.this, 20);
                c.drawCircle(lastX, lastY, r, ringPaint);
                ringPaint.setStyle(Paint.Style.FILL);
                c.drawCircle(lastX, lastY, Ui.dp(TouchpadActivity.this, 3), ringPaint);
                ringPaint.setStyle(Paint.Style.STROKE);
            }
        }

        private float avgY(MotionEvent e) {
            float s = 0;
            int n = e.getPointerCount();
            for (int i = 0; i < n; i++) s += e.getY(i);
            return n == 0 ? 0 : s / n;
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX = e.getX();
                    downY = e.getY();
                    lastX = downX;
                    lastY = downY;
                    downTime = SystemClock.uptimeMillis();
                    moved = false;
                    dragging = false;
                    touching = true;
                    maxPointers = 1;
                    accX = 0;
                    accY = 0;
                    h.removeCallbacks(longPressRunnable);
                    h.postDelayed(longPressRunnable, 460);
                    invalidate();
                    return true;

                case MotionEvent.ACTION_POINTER_DOWN:
                    maxPointers = Math.max(maxPointers, e.getPointerCount());
                    h.removeCallbacks(longPressRunnable);
                    lastAvgY = avgY(e);
                    invalidate();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    if (dragging) {
                        moveBy(e.getX() - lastX, e.getY() - lastY);
                        lastX = e.getX();
                        lastY = e.getY();
                    } else if (e.getPointerCount() >= 2) {
                        float y = avgY(e);
                        if (lastAvgY == 0) lastAvgY = y;
                        scrollBy(lastAvgY - y);
                        lastAvgY = y;
                    } else {
                        float dx = e.getX() - lastX;
                        float dy = e.getY() - lastY;
                        if (Math.abs(e.getX() - downX) > Ui.dp(TouchpadActivity.this, 8)
                                || Math.abs(e.getY() - downY) > Ui.dp(TouchpadActivity.this, 8)) {
                            moved = true;
                            h.removeCallbacks(longPressRunnable);
                        }
                        lastX = e.getX();
                        lastY = e.getY();
                        moveBy(dx, dy);
                    }
                    invalidate();
                    return true;

                case MotionEvent.ACTION_POINTER_UP:
                    lastAvgY = 0;
                    for (int i = 0; i < e.getPointerCount(); i++) {
                        if (i != e.getActionIndex()) {
                            lastX = e.getX(i);
                            lastY = e.getY(i);
                            break;
                        }
                    }
                    return true;

                case MotionEvent.ACTION_CANCEL:
                    h.removeCallbacks(longPressRunnable);
                    if (dragging) endDrag();
                    touching = false;
                    invalidate();
                    return true;

                case MotionEvent.ACTION_UP:
                    h.removeCallbacks(longPressRunnable);
                    touching = false;
                    if (dragging) {
                        endDrag();
                    } else if (!moved && SystemClock.uptimeMillis() - downTime < 420) {
                        tap(maxPointers);
                    }
                    invalidate();
                    return true;
            }
            return super.onTouchEvent(e);
        }

        private void moveBy(float dx, float dy) {
            if (!hid.isConnected()) return;
            float f = speedFactor();
            accX += dx * f;
            accY += dy * f;
            int ix = (int) accX;
            int iy = (int) accY;
            accX -= ix;
            accY -= iy;
            if (ix != 0 || iy != 0) {
                hid.sendMouse(dragging ? Usages.B_LEFT : 0, ix, iy, 0);
            }
        }

        private void scrollBy(float dy) {
            if (!hid.isConnected()) return;
            accScroll += dy;
            int step = Ui.dp(TouchpadActivity.this, 4);
            int n = (int) (accScroll / step);
            if (n != 0) {
                accScroll -= n * step;
                hid.sendMouse(0, 0, 0, n);
            }
        }

        private void startDrag() {
            if (!hid.isConnected()) return;
            dragging = true;
            hid.sendMouse(Usages.B_LEFT, 0, 0, 0);
            haptic();
            invalidate();
        }

        private void endDrag() {
            dragging = false;
            hid.sendMouse(0, 0, 0, 0);
            invalidate();
        }

        /** Drop any pending long-press and release a held left button. */
        void cancel() {
            h.removeCallbacks(longPressRunnable);
            if (dragging) {
                dragging = false;
                hid.sendMouse(0, 0, 0, 0);
            }
            touching = false;
            invalidate();
        }

        @Override protected void onDetachedFromWindow() {
            cancel();
            super.onDetachedFromWindow();
        }

        private void tap(int fingers) {
            if (!hid.isConnected()) return;
            haptic();
            if (fingers >= 3) {
                hid.clickMouse(Usages.B_MIDDLE);
            } else if (fingers == 2) {
                hid.clickMouse(Usages.B_RIGHT);
            } else {
                // Two quick taps are seen by the host as a double click, exactly
                // like a hardware touchpad, so one click per tap is correct here.
                hid.clickMouse(Usages.B_LEFT);
            }
        }
    }
}
