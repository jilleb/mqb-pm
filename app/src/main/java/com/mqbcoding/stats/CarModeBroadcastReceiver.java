package com.mqbcoding.stats;

import android.app.UiModeManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.util.Log;

public class CarModeBroadcastReceiver extends BroadcastReceiver {
    public static final String TAG = "CarModeBR";

    private static final Class[] SERVICES = {
            CarStatsService.class,
            //RemoteProjectionService.class
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "intent: " + intent);
        String action = intent.getAction();
        if (UiModeManager.ACTION_ENTER_CAR_MODE.equals(action)) {
            startServices(context);
        } else if (UiModeManager.ACTION_EXIT_CAR_MODE.equals(action)) {
            stopServices(context);
        } else if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            UiModeManager uiModeManager = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
            if (uiModeManager != null && uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_CAR) {
                startServices(context);
            }
        } else {
            throw new AssertionError();
        }
    }

    private void startServices(Context context) {
        Log.d(TAG, "starting services");
        for (Class cls: SERVICES) {
            context.startService(new Intent(context, cls));
        }
    }

    private void stopServices(Context context) {
        Log.d(TAG, "stopping services");
        for (Class cls: SERVICES) {
            context.stopService(new Intent(context, cls));
        }
    }
}
