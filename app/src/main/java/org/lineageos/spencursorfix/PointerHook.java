package org.lineageos.spencursorfix;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.view.InputDevice;
import android.view.PointerIcon;

import java.util.concurrent.ConcurrentHashMap;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public class PointerHook extends XC_MethodHook {
    private static final String TAG = "[SPenCursorFix]";
    private static final String TARGET_DESC = "8d3a4578bc04c65b36fda3518c1104ce811ed68e";

    // Fast thread-safe cache: deviceId -> isPen
    private static final ConcurrentHashMap<Integer, Boolean> sIsPenCache = new ConcurrentHashMap<>();
    private static volatile boolean sReceiverRegistered = false;

    public static synchronized void ensureReceiverRegistered(Object imsObject) {
        if (sReceiverRegistered || imsObject == null) return;
        try {
            Context context = (Context) XposedHelpers.getObjectField(imsObject, "mContext");
            if (context != null) {
                IntentFilter filter = new IntentFilter("org.lineageos.spencursorfix.UPDATE_PREFS");
                BroadcastReceiver receiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context ctx, Intent intent) {
                        Prefs.onBroadcastReceived(intent);
                    }
                };
                if (Build.VERSION.SDK_INT >= 33) {
                    context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
                } else {
                    context.registerReceiver(receiver, filter);
                }
                sReceiverRegistered = true;
                XposedBridge.log(TAG + " Live preference update receiver registered in system_server");
            }
        } catch (Throwable t) {
            XposedBridge.log(TAG + " Failed to register settings receiver: " + t.getMessage());
        }
    }

    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
        try {
            ensureReceiverRegistered(param.thisObject);

            // Master toggle check
            if (!Prefs.isEnabled()) {
                return;
            }

            int deviceId = (Integer) param.args[2];

            Boolean isPen = sIsPenCache.get(deviceId);
            if (isPen == null) {
                InputDevice dev = null;
                try {
                    dev = (InputDevice) XposedHelpers.callMethod(param.thisObject, "getInputDevice", deviceId);
                } catch (Throwable t) {
                    if (Prefs.isDebugLog()) {
                        XposedBridge.log(TAG + " Failed to resolve InputDevice: " + t.getMessage());
                    }
                }

                if (dev == null) {
                    return; // Do not cache yet; device info not ready
                }

                String name = dev.getName();
                String desc = dev.getDescriptor();
                String targetName = Prefs.getTargetName();
                isPen = (targetName != null && targetName.equalsIgnoreCase(name))
                        || TARGET_DESC.equals(desc);
                sIsPenCache.put(deviceId, isPen);

                if (Prefs.isDebugLog()) {
                    XposedBridge.log(TAG + " Cached deviceId " + deviceId + " -> isPen=" + isPen
                            + " (name=" + name + ", desc=" + desc + ")");
                }
            }

            if (!isPen) {
                return; // Non-pen device (mouse, touch, trackpad) -> 100% stock pass-through!
            }

            PointerIcon originalIcon = (PointerIcon) param.args[0];
            int iconType = getIconType(originalIcon);

            // CRITICAL: Respect explicit hide requests (e.g. drawing app canvas or video playback)
            // PointerIconType.TYPE_NULL == 0
            if (iconType == 0) {
                return;
            }

            // Replace unassigned / arrow / link / text hover icons with customizable OneUI hover dot
            String color = Prefs.getDotColor();
            float radius = Prefs.getDotRadius();
            PointerIcon dotIcon = DotIconGenerator.getOrCreateDot(color, radius);
            param.args[0] = dotIcon;

        } catch (Throwable t) {
            XposedBridge.log(TAG + " Error in setPointerIcon hook: " + t.getMessage());
        }
    }

    private static int getIconType(PointerIcon icon) {
        if (icon == null) return 1; // TYPE_NOT_SPECIFIED default
        try {
            return (Integer) XposedHelpers.callMethod(icon, "getType");
        } catch (Throwable t1) {
            try {
                return XposedHelpers.getIntField(icon, "mType");
            } catch (Throwable t2) {
                return 1;
            }
        }
    }

    public static void clearDeviceCache() {
        sIsPenCache.clear();
    }
}
