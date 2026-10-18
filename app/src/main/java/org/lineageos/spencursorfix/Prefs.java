package org.lineageos.spencursorfix;

import android.content.Intent;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;

public class Prefs {
    public static final String PREF_FILE = "settings";
    public static final String PREF_ENABLED = "pref_enabled";
    public static final String PREF_DOT_COLOR = "pref_dot_color";
    public static final String PREF_DOT_RADIUS = "pref_dot_radius";
    public static final String PREF_TARGET_NAME = "pref_target_name";
    public static final String PREF_DEBUG_LOG = "pref_debug_log";

    public static final String DEFAULT_COLOR = "#B6E5EA";
    public static final float DEFAULT_RADIUS = 4.0f;
    public static final String DEFAULT_TARGET_NAME = "sec_e-pen";

    // Live in-memory cache directly updated via broadcast from app
    private static volatile boolean sHasLiveOverride = false;
    private static volatile boolean sLiveEnabled = true;
    private static volatile String sLiveColor = DEFAULT_COLOR;
    private static volatile float sLiveRadius = DEFAULT_RADIUS;
    private static volatile String sLiveTargetName = DEFAULT_TARGET_NAME;
    private static volatile boolean sLiveDebugLog = false;

    private static XSharedPreferences sXsp = null;
    private static long sLastReload = 0;

    public static synchronized void initXsp() {
        if (sXsp == null) {
            try {
                sXsp = new XSharedPreferences("org.lineageos.spencursorfix", PREF_FILE);
                sXsp.makeWorldReadable();
            } catch (Throwable t) {
                XposedBridge.log("[SPenCursorFix] initXsp error: " + t.getMessage());
            }
        }
    }

    public static void onBroadcastReceived(Intent intent) {
        if (intent == null) return;
        try {
            if (intent.hasExtra(PREF_ENABLED)) {
                sLiveEnabled = intent.getBooleanExtra(PREF_ENABLED, true);
            }
            if (intent.hasExtra(PREF_DOT_COLOR)) {
                String c = intent.getStringExtra(PREF_DOT_COLOR);
                if (c != null && !c.isEmpty()) {
                    sLiveColor = c;
                }
            }
            if (intent.hasExtra(PREF_DOT_RADIUS)) {
                sLiveRadius = intent.getFloatExtra(PREF_DOT_RADIUS, DEFAULT_RADIUS);
            }
            if (intent.hasExtra(PREF_TARGET_NAME)) {
                String n = intent.getStringExtra(PREF_TARGET_NAME);
                if (n != null && !n.isEmpty()) {
                    sLiveTargetName = n;
                }
            }
            if (intent.hasExtra(PREF_DEBUG_LOG)) {
                sLiveDebugLog = intent.getBooleanExtra(PREF_DEBUG_LOG, false);
            }
            sHasLiveOverride = true;
            if (sLiveDebugLog) {
                XposedBridge.log("[SPenCursorFix] Live update received: enabled=" + sLiveEnabled
                        + ", color=" + sLiveColor + ", radius=" + sLiveRadius);
            }
        } catch (Throwable t) {
            XposedBridge.log("[SPenCursorFix] onBroadcastReceived error: " + t.getMessage());
        }
    }

    private static void checkReload() {
        long now = System.currentTimeMillis();
        if (now - sLastReload < 1500) {
            return;
        }
        sLastReload = now;

        if (sXsp == null) {
            initXsp();
        }

        if (sXsp != null) {
            try {
                if (sXsp.hasFileChanged()) {
                    sXsp.reload();
                }
            } catch (Throwable ignored) {
            }
        }
    }

    public static boolean isEnabled() {
        if (sHasLiveOverride) {
            return sLiveEnabled;
        }
        checkReload();
        if (sXsp != null) {
            return sXsp.getBoolean(PREF_ENABLED, true);
        }
        return true;
    }

    public static String getDotColor() {
        if (sHasLiveOverride) {
            return sLiveColor;
        }
        checkReload();
        if (sXsp != null) {
            return sXsp.getString(PREF_DOT_COLOR, DEFAULT_COLOR);
        }
        return DEFAULT_COLOR;
    }

    public static float getDotRadius() {
        if (sHasLiveOverride) {
            return sLiveRadius;
        }
        checkReload();
        if (sXsp != null) {
            String val = sXsp.getString(PREF_DOT_RADIUS, String.valueOf(DEFAULT_RADIUS));
            try {
                return Float.parseFloat(val);
            } catch (NumberFormatException ignored) {
            }
        }
        return DEFAULT_RADIUS;
    }

    public static String getTargetName() {
        if (sHasLiveOverride) {
            return sLiveTargetName;
        }
        checkReload();
        if (sXsp != null) {
            return sXsp.getString(PREF_TARGET_NAME, DEFAULT_TARGET_NAME);
        }
        return DEFAULT_TARGET_NAME;
    }

    public static boolean isDebugLog() {
        if (sHasLiveOverride) {
            return sLiveDebugLog;
        }
        checkReload();
        if (sXsp != null) {
            return sXsp.getBoolean(PREF_DEBUG_LOG, false);
        }
        return false;
    }
}
