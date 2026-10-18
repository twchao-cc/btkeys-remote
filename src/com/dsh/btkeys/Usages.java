package com.dsh.btkeys;

/** HID usage codes: keyboard/keypad page 0x07 and consumer page 0x0C. */
public final class Usages {
    private Usages() {}

    // ---- modifiers (keyboard report byte 0) ----
    public static final int M_CTRL = 0x01;
    public static final int M_SHIFT = 0x02;
    public static final int M_ALT = 0x04;
    public static final int M_GUI = 0x08;

    // ---- letters ----
    public static final int A = 0x04, B = 0x05, C = 0x06, D = 0x07, E = 0x08, F = 0x09, G = 0x0A,
            H = 0x0B, I = 0x0C, J = 0x0D, K = 0x0E, L = 0x0F, M = 0x10, N = 0x11, O = 0x12,
            P = 0x13, Q = 0x14, R = 0x15, S = 0x16, T = 0x17, U = 0x18, V = 0x19, W = 0x1A,
            X = 0x1B, Y = 0x1C, Z = 0x1D;

    // ---- digits ----
    public static final int N1 = 0x1E, N2 = 0x1F, N3 = 0x20, N4 = 0x21, N5 = 0x22, N6 = 0x23,
            N7 = 0x24, N8 = 0x25, N9 = 0x26, N0 = 0x27;

    // ---- control / punctuation ----
    public static final int ENTER = 0x28, ESC = 0x29, BACKSPACE = 0x2A, TAB = 0x2B, SPACE = 0x2C,
            MINUS = 0x2D, EQUAL = 0x2E, LBRACKET = 0x2F, RBRACKET = 0x30, BACKSLASH = 0x31,
            SEMICOLON = 0x33, QUOTE = 0x34, GRAVE = 0x35, COMMA = 0x36, DOT = 0x37, SLASH = 0x38,
            CAPS = 0x39;

    // ---- function keys ----
    public static final int F1 = 0x3A, F2 = 0x3B, F3 = 0x3C, F4 = 0x3D, F5 = 0x3E, F6 = 0x3F,
            F7 = 0x40, F8 = 0x41, F9 = 0x42, F10 = 0x43, F11 = 0x44, F12 = 0x45;

    // ---- navigation ----
    public static final int PRINT = 0x46, SCROLL = 0x47, PAUSE = 0x48, INSERT = 0x49, HOME = 0x4A,
            PAGEUP = 0x4B, DELETE = 0x4C, END = 0x4D, PAGEDOWN = 0x4E,
            RIGHT = 0x4F, LEFT = 0x50, DOWN = 0x51, UP = 0x52;

    // ---- keypad ----
    public static final int NUMLOCK = 0x53, KP_SLASH = 0x54, KP_STAR = 0x55, KP_MINUS = 0x56,
            KP_PLUS = 0x57, KP_ENTER = 0x58;
    public static final int KP1 = 0x59, KP2 = 0x5A, KP3 = 0x5B, KP4 = 0x5C, KP5 = 0x5D,
            KP6 = 0x5E, KP7 = 0x5F, KP8 = 0x60, KP9 = 0x61, KP0 = 0x62, KP_DOT = 0x63;

    // ---- consumer page 0x0C ----
    public static final int C_PLAY = 0x00CD, C_NEXT = 0x00B5, C_PREV = 0x00B6, C_STOP = 0x00B7,
            C_VOLUP = 0x00E9, C_VOLDOWN = 0x00EA, C_MUTE = 0x00E2,
            C_HOME = 0x0223, C_BACK = 0x0224, C_BRIGHT_UP = 0x006F, C_BRIGHT_DOWN = 0x0070,
            C_CALC = 0x0192, C_MAIL = 0x018A, C_SEARCH = 0x0221, C_WWW = 0x0196;

    // ---- mouse buttons ----
    public static final int B_LEFT = 1, B_RIGHT = 2, B_MIDDLE = 4;
}
