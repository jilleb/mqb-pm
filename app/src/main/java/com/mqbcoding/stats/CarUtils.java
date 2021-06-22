package com.mqbcoding.stats;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;
import android.util.DisplayMetrics;

public class CarUtils {
    public static Bitmap getCarBitmap(Context context, @DrawableRes int id, @ColorRes int tint, int size) {
        Drawable drawable = context.getResources().getDrawable(id, context.getTheme());
        return getCarBitmap(context, drawable, tint, size);
    }

    public static Bitmap getCarBitmap(Context context, Drawable drawable, @ColorRes int tint, int size) {
        drawable.setTint(ContextCompat.getColor(context, tint));
        return getCarBitmap(context, drawable, size);
    }

    public static Bitmap getCarBitmap(Context context, Drawable drawable, int size) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        Bitmap bitmap = Bitmap.createBitmap(metrics, size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public static float celsiusToFahrenheit(float celsius) {
        return (float)(celsius * 1.8 + 32);
    }

    public static final int SUCCESS_RESULT = 0;
    public static final int FAILURE_RESULT = 1;

    public static final int USE_ADDRESS_NAME = 1;
    public static final int USE_ADDRESS_LOCATION = 2;

    public static final String PACKAGE_NAME =
            "com.mqbcoding.stats";
    public static final String RECEIVER = PACKAGE_NAME + ".RECEIVER";
    public static final String RESULT_DATA_KEY = PACKAGE_NAME + ".RESULT_DATA_KEY";
    public static final String RESULT_ADDRESS = PACKAGE_NAME + ".RESULT_ADDRESS";
    public static final String LOCATION_LATITUDE_DATA_EXTRA = PACKAGE_NAME + ".LOCATION_LATITUDE_DATA_EXTRA";
    public static final String LOCATION_LONGITUDE_DATA_EXTRA = PACKAGE_NAME + ".LOCATION_LONGITUDE_DATA_EXTRA";
    public static final String LOCATION_NAME_DATA_EXTRA = PACKAGE_NAME + ".LOCATION_NAME_DATA_EXTRA";
    public static final String FETCH_TYPE_EXTRA = PACKAGE_NAME + ".FETCH_TYPE_EXTRA";
}
