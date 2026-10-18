package org.lineageos.spencursorfix;

import android.content.Context;
import android.os.IBinder;
import android.view.PointerIcon;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class HookEntry implements IXposedHookLoadPackage {
    private static final String TAG = "[SPenCursorFix]";
    private static final String TARGET_PKG = "android";
    private static final String IMS_CLASS = "com.android.server.input.InputManagerService";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!TARGET_PKG.equals(lpparam.packageName) || !TARGET_PKG.equals(lpparam.processName)) {
            return;
        }

        XposedBridge.log(TAG + " Initializing HookEntry in system_server (PID: " + android.os.Process.myPid() + ")");
        Prefs.initXsp();

        try {
            Class<?> imsClass = XposedHelpers.findClass(IMS_CLASS, lpparam.classLoader);

            try {
                XposedHelpers.findAndHookConstructor(imsClass, Context.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        PointerHook.ensureReceiverRegistered(param.thisObject);
                    }
                });
            } catch (Throwable ignored) {
                // Constructor hook fallback: lazy init inside PointerHook.beforeHookedMethod
            }

            XposedHelpers.findAndHookMethod(
                    imsClass,
                    "setPointerIcon",
                    PointerIcon.class,
                    int.class,     // displayId
                    int.class,     // deviceId
                    int.class,     // pointerId
                    IBinder.class, // inputToken
                    new PointerHook()
            );
            XposedBridge.log(TAG + " Successfully hooked InputManagerService.setPointerIcon");
        } catch (Throwable t) {
            XposedBridge.log(TAG + " Failed to hook InputManagerService: " + t.getMessage());
        }
    }
}
