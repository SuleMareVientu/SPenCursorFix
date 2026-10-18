package org.lineageos.spencursorfix;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.PointerIcon;

public class DotIconGenerator {
    private static volatile PointerIcon sCachedIcon = null;
    private static int sCachedColor = 0;
    private static float sCachedRadiusDp = 0;
    private static int sCachedDensityDpi = 0;

    /**
     * Creates or returns a cached Samsung OneUI S Pen hover ring PointerIcon with exact center hotspot,
     * derived from the official Samsung pointer_spot_hover_vector asset.
     *
     * @param colorHex Hex color string, e.g. "#B6E5EA"
     * @param radiusDp Radius in dp (e.g. 4.0f for official 8dp diameter ring)
     * @return PointerIcon ready for setPointerIcon
     */
    public static synchronized PointerIcon getOrCreateDot(String colorHex, float radiusDp) {
        int color;
        try {
            if ("dynamic".equalsIgnoreCase(colorHex) || "system".equalsIgnoreCase(colorHex)) {
                color = getSystemAccentColor();
            } else {
                color = Color.parseColor(colorHex);
            }
        } catch (Throwable t) {
            color = Color.parseColor("#B6E5EA");
        }

        int densityDpi = Resources.getSystem().getDisplayMetrics().densityDpi;
        if (densityDpi <= 0) {
            densityDpi = 340; // Default verified density for LineageOS for Tablets
        }

        if (sCachedIcon != null
                && sCachedColor == color
                && sCachedRadiusDp == radiusDp
                && sCachedDensityDpi == densityDpi) {
            return sCachedIcon;
        }

        float scale = densityDpi / 160.0f;
        float ratio = radiusDp / 4.0f; // 4.0dp is baseline official OneUI radius

        // Geometry from pointer_spot_hover_vector.xml:
        // Viewport 24dp, scaled 0.5x, center at (12, 12)
        // Outer border path: outer r = 4.001dp, inner r = 3.250dp, color #9ea0a0
        // Inner ring path:   outer r = 3.611dp, inner r = 3.100dp, color #b6e5ea
        float outerBorderOuterR = 4.001f * ratio * scale;
        float outerBorderInnerR = 3.250f * ratio * scale;
        float innerRingOuterR   = 3.611f * ratio * scale;
        float innerRingInnerR   = 3.100f * ratio * scale;

        // Allocate bitmap with safe padding for anti-aliasing boundary
        float maxRadius = outerBorderOuterR + (scale * 1.0f);
        int sizePx = (int) Math.ceil(maxRadius * 2.0f);
        if (sizePx % 2 != 0) {
            sizePx += 1;
        }

        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        float centerX = sizePx / 2.0f;
        float centerY = sizePx / 2.0f;

        // 1. Subtle dark drop halo for contrast on bright/white backgrounds
        Paint haloPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        haloPaint.setStyle(Paint.Style.STROKE);
        haloPaint.setStrokeWidth(Math.max(1.0f, scale * 0.75f));
        haloPaint.setColor(Color.argb(40, 0, 0, 0));
        canvas.drawCircle(centerX, centerY, outerBorderOuterR, haloPaint);

        // 2. Outer grey border (#9ea0a0) - exact path 1 from vector
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        float borderStrokeWidth = Math.max(1.0f, outerBorderOuterR - outerBorderInnerR);
        borderPaint.setStrokeWidth(borderStrokeWidth);
        borderPaint.setColor(Color.parseColor("#9EA0A0"));
        float borderCenterR = (outerBorderOuterR + outerBorderInnerR) / 2.0f;
        canvas.drawCircle(centerX, centerY, borderCenterR, borderPaint);

        // 3. Inner accent ring (official OneUI cyan #b6e5ea or user custom color) - exact path 2 from vector
        Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ringPaint.setStyle(Paint.Style.STROKE);
        float ringStrokeWidth = Math.max(1.0f, innerRingOuterR - innerRingInnerR);
        ringPaint.setStrokeWidth(ringStrokeWidth);
        ringPaint.setColor(color);
        float ringCenterR = (innerRingOuterR + innerRingInnerR) / 2.0f;
        canvas.drawCircle(centerX, centerY, ringCenterR, ringPaint);

        // 4. Hotspot is exactly at the geometric center
        PointerIcon icon = PointerIcon.create(bitmap, centerX, centerY);

        sCachedIcon = icon;
        sCachedColor = color;
        sCachedRadiusDp = radiusDp;
        sCachedDensityDpi = densityDpi;

        return icon;
    }

    public static int getSystemAccentColor() {
        try {
            Resources res = Resources.getSystem();
            int resId = res.getIdentifier("system_accent1_600", "color", "android");
            if (resId != 0) {
                return res.getColor(resId, null);
            }
            resId = res.getIdentifier("system_accent1_500", "color", "android");
            if (resId != 0) {
                return res.getColor(resId, null);
            }
        } catch (Throwable ignored) {
        }
        return Color.parseColor("#006495");
    }
}
